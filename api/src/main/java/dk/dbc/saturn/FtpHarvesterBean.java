/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@LocalBean
@Stateless
public class FtpHarvesterBean {
    public List<InputStream> harvest(String host, int port, String username,
            String password, String dir, FileNameMatcher fileNameMatcher) {
        List<InputStream> inputStreams = new ArrayList<>();
        FtpClient ftpClient = new FtpClient()
            .withHost(host)
            .withPort(port)
            .withUsername(username)
            .withPassword(password)
            .cd(dir);
        for(String file : ftpClient.list(fileNameMatcher)) {
            inputStreams.add(ftpClient.get(file));
        }
        ftpClient.close();
        return inputStreams;
    }
}
