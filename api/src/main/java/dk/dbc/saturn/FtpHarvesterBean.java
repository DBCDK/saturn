/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

@LocalBean
@Stateless
public class FtpHarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        FtpHarvesterBean.class);

    @Asynchronous
    public Future<Set<FileHarvest>> harvest(String host, int port,
                                                    String username, String password, String dir,
                                                    FileNameMatcher fileNameMatcher) {
        long start = Instant.now().toEpochMilli();
        LOGGER.info("harvesting {}@{}:{}/{} with pattern \"{}\"", username,
            host, port, dir, fileNameMatcher.getPattern());
        Set<FileHarvest> fileHarvests = new HashSet<>();
        FtpClient ftpClient = new FtpClient()
            .withHost(host)
            .withPort(port)
            .withUsername(username)
            .withPassword(password)
            .cd(dir);
        for(String file : ftpClient.list(fileNameMatcher)) {
            if (file != null && !file.isEmpty()) {
                final FileHarvest fileHarvest = new FileHarvest(
                        file, ftpClient.get(file), null);
                fileHarvests.add(fileHarvest);
            }
        }
        ftpClient.close();
        LOGGER.info("harvesting for {}@{}:{}/{} took {} ms", username,
            host, port, dir, Instant.now().toEpochMilli() - start);
        return new AsyncResult<>(fileHarvests);
    }
}
