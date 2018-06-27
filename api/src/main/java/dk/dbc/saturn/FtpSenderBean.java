/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.Set;

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
    public void send(Set<FileHarvest> files) {
        FtpClient ftpClient = new FtpClient()
            .withHost(host)
            .withPort(Integer.parseInt(port))
            .withUsername(username)
            .withPassword(password)
            .cd(dir);
        try {
            for(FileHarvest fileHarvest : files) {
                ftpClient.put(fileHarvest.getFilename(), fileHarvest.getContent());
            }
        } finally {
            ftpClient.close();
        }
    }
}
