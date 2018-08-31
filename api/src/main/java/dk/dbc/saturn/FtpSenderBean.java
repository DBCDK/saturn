/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.util.Set;
import java.util.stream.Collectors;

@LocalBean
@Stateless
public class FtpSenderBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        FtpSenderBean.class);
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
    public void send(Set<FileHarvest> files, String transfileName, String transfile) {
        String filenames = files.stream().map(FileHarvest::getFilename)
            .collect(Collectors.joining(", "));
        LOGGER.info(String.format("sending to ftp, files = %s, " +
            "transfile = %s, transfilename = %s", filenames, transfile,
            transfileName));
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
            ftpClient.put(transfileName, transfile);
        } finally {
            ftpClient.close();
        }
    }
}
