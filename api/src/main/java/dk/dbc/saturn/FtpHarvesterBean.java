/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@LocalBean
@Stateless
public class FtpHarvesterBean {
    @Asynchronous
    public Future<Map<String, InputStream>> harvest(String host, int port,
            String username, String password, String dir,
            FileNameMatcher fileNameMatcher) {
        Map<String, InputStream> inputStreams = new HashMap<>();
        FtpClient ftpClient = new FtpClient()
            .withHost(host)
            .withPort(port)
            .withUsername(username)
            .withPassword(password)
            .cd(dir);
        for(String file : ftpClient.list(fileNameMatcher)) {
            if(file != null && !file.isEmpty()) {
                inputStreams.put(file, ftpClient.get(file));
            }
        }
        ftpClient.close();
        return new AsyncResult<>(inputStreams);
    }
}
