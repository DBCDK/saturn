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
import java.util.Map;

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
     * @param files map of filenames and corresponding input streams
     */
    public void send(Map<String, InputStream> files) {
        FtpClient ftpClient = new FtpClient()
            .withHost(host)
            .withPort(Integer.parseInt(port))
            .withUsername(username)
            .withPassword(password)
            .cd(dir);
        try {
            for(Map.Entry<String, InputStream> entry : files.entrySet()) {
                ftpClient.put(entry.getKey(), entry.getValue());
            }
        } finally {
            ftpClient.close();
        }
    }
}
