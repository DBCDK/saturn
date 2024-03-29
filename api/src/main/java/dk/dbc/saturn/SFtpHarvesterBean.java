/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import com.jcraft.jsch.ChannelSftp;
import dk.dbc.commons.sftpclient.SFTPConfig;
import dk.dbc.commons.sftpclient.SFtpClient;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import dk.dbc.util.Stopwatch;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@LocalBean
@Stateless
public class SFtpHarvesterBean {
    @EJB
    ProxyBean proxyBean;
    @EJB FtpSenderBean ftpSenderBean;
    @EJB RunningTasks runningTasks;
    @EJB HarvesterConfigRepository harvesterConfigRepository;

    @Inject
    @ConfigProperty(name = "NON_PROXY_HOSTS", defaultValue = "")
    Set<String> nonProxiedHosts;

    private static final Logger LOGGER = LoggerFactory.getLogger(
        SFtpHarvesterBean.class);

    @Asynchronous
    public void harvest(SFtpHarvesterConfig config, ProgressTrackerBean.Key progressKey) throws HarvestException {
        try (HarvesterMDC mdc = new HarvesterMDC(config)) {
            LOGGER.info("Starting harvest of {}", config.getName());
            Set<FileHarvest> fileHarvests = listFiles(config);
            ftpSenderBean.send(fileHarvests, config.getAgency(), config.getTransfile(), config.getGzip(), progressKey, false);
            config.setLastHarvested(Date.from(Instant.now()));
            config.setSeqno(fileHarvests.stream()
                    .map(FileHarvest::getSeqno)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Integer::valueOf))
                    .orElse(0));
            fileHarvests.forEach(FileHarvest::close);

            harvesterConfigRepository.save(SFtpHarvesterConfig.class, config);
            LOGGER.info("Ended harvest of {}", config.getName() );
        } finally {
            runningTasks.remove(config);
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<FileHarvest> listFiles(SFtpHarvesterConfig sFtpHarvesterConfig) {
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher(sFtpHarvesterConfig);
        final Stopwatch stopwatch = new Stopwatch();
        LOGGER.info("Listing from {}@{}:{}/{} with pattern \"{}\"",
                sFtpHarvesterConfig.getUsername(),
                sFtpHarvesterConfig.getHost(),
                sFtpHarvesterConfig.getPort(),
                sFtpHarvesterConfig.getDir(),
                sFtpHarvesterConfig.getFilesPattern());
        LOGGER.info("None proxied hosts {}", String.join(", ", nonProxiedHosts != null ? nonProxiedHosts : Set.of()));
        Set<FileHarvest> fileHarvests = new HashSet<>();

        try (SFtpClient sftpClient = new SFtpClient(
                new SFTPConfig()
                .withHost(sFtpHarvesterConfig.getHost())
                .withUsername(sFtpHarvesterConfig.getUsername())
                .withPassword(sFtpHarvesterConfig.getPassword())
                .withPort(sFtpHarvesterConfig.getPort())
                .withDir(sFtpHarvesterConfig.getDir())
                .withFilesPattern(sFtpHarvesterConfig.getFilesPattern()),
                proxyBean.getProxy(), nonProxiedHosts != null ? nonProxiedHosts : Set.of())) {
            for (ChannelSftp.LsEntry lsEntry : sftpClient.ls(sFtpHarvesterConfig.getFilesPattern())) {
                String filename = lsEntry.getFilename();
                if (filename != null && !filename.isEmpty() && seqnoMatcher.shouldFetch(filename.trim())) {
                    /*
                     * The sequence number comparison is done using a trimmed
                     * version of the filename because sometimes the filenames
                     * have leading spaces. The filename is carried unaltered
                     * to the receiver though to maintain some faithfulness to
                     * the original file.
                     */

                    final FileHarvest fileHarvest = new SFtpFileHarvest(
                            sFtpHarvesterConfig.getDir(),
                            filename,
                            seqnoMatcher.getSeqno(),
                            sftpClient,
                            FileHarvest.Status.AWAITING_DOWNLOAD);
                    fileHarvests.add(fileHarvest);

                }
            }
            LOGGER.info("Listing from {}@{}:{}/{} took {} ms", sFtpHarvesterConfig.getUsername(),
                    sFtpHarvesterConfig.getHost(),
                    sFtpHarvesterConfig.getPort(),
                    sFtpHarvesterConfig.getDir(),
                    stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
            return fileHarvests;

        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<FileHarvest> listAllFiles(SFtpHarvesterConfig sFtpHarvesterConfig) {
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher(sFtpHarvesterConfig);
        final FileNameMatcher fileNameMatcher = new FileNameMatcher(sFtpHarvesterConfig.getFilesPattern());
        final Stopwatch stopwatch = new Stopwatch();
        Set<FileHarvest> fileHarvests = new HashSet<>();
        LOGGER.info("None proxied hosts {}",proxyBean.getNonProxyHosts() != null ? String.join(", ", proxyBean.getNonProxyHosts()) : "NONE");
        try (SFtpClient sftpClient = new SFtpClient(new SFTPConfig()
                .withHost(sFtpHarvesterConfig.getHost())
                .withUsername(sFtpHarvesterConfig.getUsername())
                .withPassword(sFtpHarvesterConfig.getPassword())
                .withPort(sFtpHarvesterConfig.getPort())
                .withDir(sFtpHarvesterConfig.getDir())
                .withFilesPattern(sFtpHarvesterConfig.getFilesPattern()),
                proxyBean.getProxy(), nonProxiedHosts != null ? nonProxiedHosts : Set.of())) {
            for (ChannelSftp.LsEntry lsEntry : sftpClient.ls("*")) {
                String filename = lsEntry.getFilename();
                LOGGER.info("Checking filename:{}", filename);
                if (filename != null && !filename.isEmpty()) {
                    if (!fileNameMatcher.matches(filename)) {
                        fileHarvests.add(new SFtpFileHarvest(
                                sFtpHarvesterConfig.getDir(),
                                filename,
                                null,
                                sftpClient,
                                FileHarvest.Status.SKIPPED_BY_FILENAME));
                        continue;
                    }
                    final FileHarvest.Status status;
                    if (seqnoMatcher.shouldFetch(filename.trim())) {
                        status = FileHarvest.Status.AWAITING_DOWNLOAD;
                    } else {
                        status = FileHarvest.Status.SKIPPED_BY_SEQNO;
                    }
                    fileHarvests.add(new SFtpFileHarvest(
                            sFtpHarvesterConfig.getDir(), filename, seqnoMatcher.getSeqno(), sftpClient, status));
                }
            }
            LOGGER.info("Listing all files from {}@{}:{}/{} took {} ms", sFtpHarvesterConfig.getUsername(),
                    sFtpHarvesterConfig.getHost(),
                    sFtpHarvesterConfig.getPort(),
                    sFtpHarvesterConfig.getDir(),
                    stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
            return fileHarvests;
        }
    }
}
