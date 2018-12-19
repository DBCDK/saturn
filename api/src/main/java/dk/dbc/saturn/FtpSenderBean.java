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
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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

    private static final String APPLICATION_ID = "saturn";

    /**
     * send files to an ftp server
     * @param files map of filenames and corresponding input streams
     * @param filenamePrefix prefix for data files and transfile
     * @param transfileTemplate transfile content template
     */
    public void send(Set<FileHarvest> files, String filenamePrefix,
            String transfileTemplate) {
        files.forEach(f -> f.setFilenamePrefix(filenamePrefix));
        final String filenames = files.stream().map(FileHarvest::getFilename)
            .collect(Collectors.joining(", "));
        LOGGER.info("downloading to ftp: {}", filenames);
        final String transfile = TransfileGenerator
            .generateTransfile(transfileTemplate,
                files.stream().map(FileHarvest::getFilename).collect(Collectors.toList()));
        final String transfileName = String.format("%s.%s.trans",
            filenamePrefix, APPLICATION_ID);
        LOGGER.info("creating transfile {} with content: {}", transfileName, transfile);
        FtpClient ftpClient = new FtpClient()
            .withHost(host)
            .withPort(Integer.parseInt(port))
            .withUsername(username)
            .withPassword(password)
            .cd(dir);
        try {
            for(FileHarvest fileHarvest : files) {
                ftpClient.put(fileHarvest.getFilename(),
                    fileHarvest.getContent(), FtpClient.FileType.BINARY);
            }
            ftpClient.put(transfileName, new ByteArrayInputStream(
                transfile.getBytes(StandardCharsets.UTF_8)),
                FtpClient.FileType.BINARY);
        } finally {
            ftpClient.close();
        }
    }
}
