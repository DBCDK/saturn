/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@LocalBean
@Stateless
public class FtpHarvesterBean {
    public Map<String, InputStream> harvest(String host, int port, String username,
            String password, String dir, FileNameMatcher fileNameMatcher) {
        Map<String, InputStream> inputStreams = new HashMap<>();
        FtpClient ftpClient = new FtpClient()
            .withHost(host)
            .withPort(port)
            .withUsername(username)
            .withPassword(password)
            .cd(dir);
        for(String file : ftpClient.list(fileNameMatcher)) {
            inputStreams.put(file, ftpClient.get(file));
        }
        ftpClient.close();
        return inputStreams;
    }
}
