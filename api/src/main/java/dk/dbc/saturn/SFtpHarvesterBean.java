/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import dk.dbc.commons.sftpclient.SFTPConfig;
import dk.dbc.commons.sftpclient.SFtpClient;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import dk.dbc.util.Stopwatch;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@LocalBean
@Stateless
public class SFtpHarvesterBean extends Harvester<SFtpHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SFtpHarvesterBean.class);
    @EJB
    ProxyBean proxyBean;
    @Inject
    @ConfigProperty(name = "NON_PROXY_HOSTS", defaultValue = "")
    Set<String> nonProxiedHosts;
    @Inject
    @ConfigProperty(name = "JSCH_SERVER_HOST_KEY", defaultValue = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256,ssh-rsa")
    private String jschServerHostKey;

    @PostConstruct
    public void init() {
        JSch.setConfig("server_host_key", jschServerHostKey);
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
        SFTPConfig config = makeConfig(sFtpHarvesterConfig);
        try (SFtpClient sftpClient = new SFtpClient(config, proxyBean.getProxy(), nonProxiedHosts != null ? nonProxiedHosts : Set.of())) {
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
                            FileHarvest.Status.AWAITING_DOWNLOAD, lsEntry.getAttrs().getSize());
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
        try (SFtpClient sftpClient = new SFtpClient(makeConfig(sFtpHarvesterConfig), proxyBean.getProxy(), nonProxiedHosts != null ? nonProxiedHosts : Set.of())) {
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
                                FileHarvest.Status.SKIPPED_BY_FILENAME, lsEntry.getAttrs().getSize()));
                        continue;
                    }
                    final FileHarvest.Status status;
                    if (seqnoMatcher.shouldFetch(filename.trim())) {
                        status = FileHarvest.Status.AWAITING_DOWNLOAD;
                    } else {
                        status = FileHarvest.Status.SKIPPED_BY_SEQNO;
                    }
                    fileHarvests.add(new SFtpFileHarvest(
                            sFtpHarvesterConfig.getDir(), filename, seqnoMatcher.getSeqno(), sftpClient, status, lsEntry.getAttrs().getSize()));
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

    private SFTPConfig makeConfig(SFtpHarvesterConfig sFtpHarvesterConfig) {
        SFTPConfig config = new SFTPConfig()
                .withHost(sFtpHarvesterConfig.getHost())
                .withUsername(sFtpHarvesterConfig.getUsername())
                .withPort(sFtpHarvesterConfig.getPort())
                .withDir(sFtpHarvesterConfig.getDir())
                .withFilesPattern(sFtpHarvesterConfig.getFilesPattern());
        if (sFtpHarvesterConfig.getPrivateKey() == null) config.withPassword(sFtpHarvesterConfig.getPassword());
        else config.withPrivateKey(sFtpHarvesterConfig.getPrivateKey()).withPublicKey(sFtpHarvesterConfig.getPublicKey());
        return config;
    }
}
