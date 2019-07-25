/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.Objects;
import java.util.Comparator;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@LocalBean
@Stateless
public class FtpHarvesterBean {
    @EJB ProxyHandlerBean proxyHandlerBean;
    @EJB FtpSenderBean ftpSenderBean;
    @EJB RunningTasks runningTasks;
    @EJB HarvesterConfigRepository harvesterConfigRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(
        FtpHarvesterBean.class);
    
    @Asynchronous
    public Future<Void> harvest( FtpHarvesterConfig config, Set<FileHarvest> fileHarvests ) throws HarvestException {
        try (HarvesterMDC mdc = new HarvesterMDC(config)) {
            LOGGER.info( "Starting harvest of {}", config.getName());
            ftpSenderBean.send(fileHarvests, config.getAgency(), config.getTransfile());
            config.setLastHarvested(Date.from(Instant.now()));
            config.setSeqno(fileHarvests.stream()
                    .map(FileHarvest::getSeqno)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Integer::valueOf))
                    .orElse(0));
            fileHarvests.stream().forEach(FileHarvest::close);

            harvesterConfigRepository.save(FtpHarvesterConfig.class, config);
            LOGGER.info( "Ended harvest of {}", config.getName() );
            runningTasks.remove( config );
            return new AsyncResult<Void>(null);
        }
    }



    public Set<FileHarvest> listFiles( FtpHarvesterConfig ftpHarvesterConfig ) {
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher( ftpHarvesterConfig );
        final FileNameMatcher fileNameMatcher = new FileNameMatcher(ftpHarvesterConfig.getFilesPattern());
        final Stopwatch stopwatch = new Stopwatch();
        final String username = ftpHarvesterConfig.getUsername();
        final String host = ftpHarvesterConfig.getHost();
        final int port = ftpHarvesterConfig.getPort();
        final String password = ftpHarvesterConfig.getPassword();
        final String dir = ftpHarvesterConfig.getDir();

        LOGGER.info("Listing from {}@{}:{}/{} with pattern \"{}\"", username,
                host, port, dir, fileNameMatcher.getPattern());
        Set<FileHarvest> fileHarvests = new HashSet<>();
        FtpClient ftpClient = new FtpClient()
                .withHost(host)
                .withPort(port)
                .withUsername(username)
                .withPassword(password);
        if(proxyHandlerBean.getProxyHostname() != null &&
                proxyHandlerBean.getProxyPort() != 0) {
            // mockftpserver doesn't seem to be accessible through a mock
            // socks proxy so this part is untested for now
            final InetSocketAddress address = new InetSocketAddress(
                    proxyHandlerBean.getProxyHostname(),
                    proxyHandlerBean.getProxyPort());
            final Proxy proxy = new Proxy(Proxy.Type.SOCKS, address);
            ftpClient.withProxy(proxy);
            LOGGER.debug("using proxy: host = {} port = {}",
                    proxyHandlerBean.getProxyHostname(),
                    proxyHandlerBean.getProxyPort());
        }
        if(!dir.isEmpty()) {
            ftpClient.cd(dir);
        }
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
                            dir,
                            file,
                            seqnoMatcher.getSeqno(),
                            ftpClient);
                    fileHarvests.add(fileHarvest);
                }
            }
        }
        ftpClient.close();
        LOGGER.info("Listing from {}@{}:{}/{} took {} ms", username,
                host, port, dir, stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        return fileHarvests;
    }

}
