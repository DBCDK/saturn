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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@LocalBean
@Stateless
public class FtpHarvesterBean {
    @EJB ProxyHandlerBean proxyHandlerBean;

    private static final Logger LOGGER = LoggerFactory.getLogger(
        FtpHarvesterBean.class);

    @Asynchronous
    public Future<Set<FileHarvest>> harvest(FtpHarvesterConfig config) {
        try (HarvesterMDC mdc = new HarvesterMDC(config)) {
            final FileNameMatcher fileNameMatcher =
                    new FileNameMatcher(config.getFilesPattern());
            return new AsyncResult<>(harvest(config.getHost(),
                    config.getPort(), config.getUsername(),
                    config.getPassword(), config.getDir(),
                    fileNameMatcher, new SeqnoMatcher(config)));
        }
    }

    public Set<FileHarvest> harvest(String host, int port,
                                            String username, String password, String dir,
                                            FileNameMatcher fileNameMatcher,
                                            SeqnoMatcher seqnoMatcher) {
        final Stopwatch stopwatch = new Stopwatch();
        LOGGER.info("harvesting {}@{}:{}/{} with pattern \"{}\"", username,
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
        LOGGER.info("harvesting for {}@{}:{}/{} took {} ms", username,
            host, port, dir, stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        return fileHarvests;
    }
}
