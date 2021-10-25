/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import dk.dbc.saturn.gzip.GzipCompressingInputStream;
import dk.dbc.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
     * @param gzip zip output contents (not tramsfile)?
     */
    public void send(Set<FileHarvest> files, String filenamePrefix,
            String transfileTemplate, Boolean gzip) throws HarvestException    {
        final Stopwatch stopwatch = new Stopwatch();
        try {
            final List<String> filenames = files.stream()
                    .map( fileHarvest-> fileHarvest.getUploadFilename(filenamePrefix) )
                    .sorted()
                    .collect( Collectors.toList() );
            LOGGER.info("Files to upload to ftp: {}", String.join(",", filenames));
            final String transfile = TransfileGenerator
                    .generateTransfile(transfileTemplate, filenames, gzip);
            final String transfileName = String.format("%s.%s.trans",
                    filenamePrefix, APPLICATION_ID);
            FtpClient ftpClient = FtpClientFactory.createFtpClient( host, Integer.parseInt(port),
                    username, password, dir, null );
            String filename = "";
            try {
                for (FileHarvest fileHarvest : files) {
                    filename = fileHarvest.getUploadFilename(filenamePrefix);
                    ftpClient.put(
                            gzip ? filename + ".gz": filename,
                            gzip ? new GzipCompressingInputStream(fileHarvest.getContent()) : fileHarvest.getContent(),
                            FtpClient.FileType.BINARY );
                    fileHarvest.close();
                }
                LOGGER.info("Uploading transfile {} with content: {}", transfileName, transfile);
                ftpClient.put(transfileName, new ByteArrayInputStream(
                                transfile.getBytes(StandardCharsets.UTF_8)),
                        FtpClient.FileType.BINARY);
            } catch (IOException e) {
                throw new HarvestException(String.format("GZip of file '%s' failed.",filename));
            } finally {
                ftpClient.close();
            }
        } finally {
            LOGGER.info("send took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        }
    }
}
