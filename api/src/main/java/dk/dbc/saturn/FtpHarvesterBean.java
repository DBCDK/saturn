/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.util.Stopwatch;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@LocalBean
@Stateless
public class FtpHarvesterBean extends Harvester<FtpHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpHarvesterBean.class);
    @EJB
    ProxyBean proxyBean;

    public Set<FileHarvest> listFiles( FtpHarvesterConfig ftpHarvesterConfig ) {
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher( ftpHarvesterConfig );
        final FileNameMatcher fileNameMatcher = new FileNameMatcher(ftpHarvesterConfig.getFilesPattern());
        final Stopwatch stopwatch = new Stopwatch();
        LOGGER.info("Listing from {}@{}:{}/{} with pattern \"{}\"",
                ftpHarvesterConfig.getUsername(),
                ftpHarvesterConfig.getHost(),
                ftpHarvesterConfig.getPort(),
                ftpHarvesterConfig.getDir(),
                fileNameMatcher.getPattern());
        Set<FileHarvest> fileHarvests = new HashSet<>();
        FtpClient ftpClient = FtpClientFactory.createFtpClient( ftpHarvesterConfig, proxyBean);
        String workingDirectory = ftpClient.pwd();
        for (String file : ftpClient.list(fileNameMatcher)) {
            if (file != null && !file.isEmpty()) {
                /*
                 * The sequence number comparison is done using a trimmed
                 * version of the filename because sometimes the filenames
                 * have leading spaces. The filename is carried unaltered
                 * to the receiver though to maintain some faithfulness to
                 * the original file.
                 */
                final String filename = Paths.get(file).getFileName().toString().trim();
                if (seqnoMatcher.shouldFetch(filename)) {
                    final FileHarvest fileHarvest = new FtpFileHarvest(
                            workingDirectory + '/' + ftpHarvesterConfig.getDir(),
                            file,
                            seqnoMatcher.getSeqno(),
                            ftpClient,
                            FileHarvest.Status.AWAITING_DOWNLOAD, fileNameMatcher.getFileSize(filename));
                    fileHarvests.add(fileHarvest);
                }
            }
        }
        ftpClient.close();
        LOGGER.info("Listing from {}@{}:{}/{} took {} ms", ftpHarvesterConfig.getUsername(),
                ftpHarvesterConfig.getHost(),
                ftpHarvesterConfig.getPort(),
                ftpHarvesterConfig.getDir(),
                stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        return fileHarvests;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<FileHarvest> listAllFiles(FtpHarvesterConfig ftpHarvesterConfig) {
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher(ftpHarvesterConfig);
        final FileNameMatcher fileNameMatcher = new FileNameMatcher(ftpHarvesterConfig.getFilesPattern());
        final FileNameMatcher allFilesMatcher = new FileNameMatcher("*");
        final Stopwatch stopwatch = new Stopwatch();
        Set<FileHarvest> fileHarvests = new HashSet<>();
        FtpClient ftpClient = FtpClientFactory.createFtpClient(ftpHarvesterConfig, proxyBean);
        try {
            for (String file : ftpClient.list(allFilesMatcher)) {
                if (file != null && !file.isEmpty()) {
                    if (!fileNameMatcher.matches(file)) {
                        fileHarvests.add(new FtpFileHarvest(
                                ftpHarvesterConfig.getDir(), file, null, ftpClient,
                                FileHarvest.Status.SKIPPED_BY_FILENAME, allFilesMatcher.getFileSize(file)));
                        continue;
                    }
                    final String filename = Paths.get(file).getFileName().toString().trim();
                    final FileHarvest.Status status;
                    if (seqnoMatcher.shouldFetch(filename)) {
                        status = FileHarvest.Status.AWAITING_DOWNLOAD;
                    } else {
                        status = FileHarvest.Status.SKIPPED_BY_SEQNO;
                    }
                    fileHarvests.add(new FtpFileHarvest(ftpHarvesterConfig.getDir(), file, seqnoMatcher.getSeqno(), ftpClient, status, allFilesMatcher.getFileSize(file)));
                }
            }
            LOGGER.info("Listing all files from {}@{}:{}/{} took {} ms", ftpHarvesterConfig.getUsername(),
                    ftpHarvesterConfig.getHost(),
                    ftpHarvesterConfig.getPort(),
                    ftpHarvesterConfig.getDir(),
                    stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
            return fileHarvests;
        } finally {
            ftpClient.close();
        }
    }
}
