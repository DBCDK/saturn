/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.io.InputStream;
import java.util.List;

@LocalBean
@Stateless
public class FtpSenderBean {
    @Resource(lookup = "java:app/env/ftp/host")
    protected String host;

    @Resource(lookup = "java:app/env/ftp/port")
    protected String port;

    @Resource(lookup = "java:app/env/ftp/username")
    protected String username;

    @Resource(lookup = "java:app/env/ftp/password")
    protected String password;

    @Resource(lookup = "java:app/env/ftp/dir")
    protected String dir;

    /**
     * send files to an ftp server
     * @param inputStreams list of inputstream to send
     * @param remoteNames list of filenames corresponding to the inputstreams
     */
    public void send(List<InputStream> inputStreams, List<String> remoteNames) {
        if(inputStreams.size() != remoteNames.size()) {
            throw new IllegalArgumentException(String.format("number of filenames " +
                "(%s) does not match number of files (%s)",
                remoteNames.size(), inputStreams.size()));
        }
        FtpClient ftpClient = new FtpClient()
            .withHost(host)
            .withPort(Integer.parseInt(port))
            .withUsername(username)
            .withPassword(password)
            .cd(dir);
        for(int i = 0; i < inputStreams.size(); i++) {
            ftpClient.put(remoteNames.get(i), inputStreams.get(i));
        }
        ftpClient.close();
    }
}
