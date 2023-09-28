/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@LocalBean
@Stateless
public class FtpHarvesterBean {
    @EJB
    ProxyBean proxyBean;
    @EJB FtpSenderBean ftpSenderBean;
    @EJB RunningTasks runningTasks;
    @EJB HarvesterConfigRepository harvesterConfigRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(
        FtpHarvesterBean.class);
    
    @Asynchronous
    public Future<Void> harvest(FtpHarvesterConfig config, ProgressTrackerBean.Key progressKey) throws HarvestException {
        try (HarvesterMDC mdc = new HarvesterMDC(config)) {
            LOGGER.info("Starting harvest of {}", config.getName());
            Set<FileHarvest> fileHarvests = listFiles( config );
            ftpSenderBean.send(fileHarvests, config.getAgency(), config.getTransfile(), config.getGzip(), progressKey, false);
            config.setLastHarvested(Date.from(Instant.now()));
            config.setSeqno(fileHarvests.stream()
                    .map(FileHarvest::getSeqno)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Integer::valueOf))
                    .orElse(0));
            fileHarvests.forEach(FileHarvest::close);

            harvesterConfigRepository.save(FtpHarvesterConfig.class, config);
            LOGGER.info("Ended harvest of {}", config.getName());
            return new AsyncResult<Void>(null);
        } finally {
            runningTasks.remove(config);
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
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
                            ftpHarvesterConfig.getDir(),
                            file,
                            seqnoMatcher.getSeqno(),
                            ftpClient,
                            FileHarvest.Status.AWAITING_DOWNLOAD);
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
        for (String file : ftpClient.list(allFilesMatcher)) {
            if (file != null && !file.isEmpty()) {
                if (!fileNameMatcher.matches(file)) {
                    fileHarvests.add(new FtpFileHarvest(
                            ftpHarvesterConfig.getDir(), file, null, ftpClient,
                            FileHarvest.Status.SKIPPED_BY_FILENAME));
                    continue;
                }
                final String filename = Paths.get(file).getFileName().toString().trim();
                final FileHarvest.Status status;
                if (seqnoMatcher.shouldFetch(filename)) {
                    status = FileHarvest.Status.AWAITING_DOWNLOAD;
                } else {
                   status = FileHarvest.Status.SKIPPED_BY_SEQNO;
                }
                fileHarvests.add(new FtpFileHarvest(
                        ftpHarvesterConfig.getDir(), file, seqnoMatcher.getSeqno(), ftpClient, status));
            }
        }
        ftpClient.close();
        LOGGER.info("Listing all files from {}@{}:{}/{} took {} ms", ftpHarvesterConfig.getUsername(),
                ftpHarvesterConfig.getHost(),
                ftpHarvesterConfig.getPort(),
                ftpHarvesterConfig.getDir(),
                stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        return fileHarvests;
    }
}
