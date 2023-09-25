/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import dk.dbc.ftp.FtpClientException;
import dk.dbc.saturn.gzip.GzipCompressingInputStream;
import dk.dbc.util.Stopwatch;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpSenderBean.class);
    @Inject
    ProgressTrackerBean progressTrackerBean;

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
    private final int MAX_RETRIES = 8;

    public FtpSenderBean() {
    }

    public FtpSenderBean(ProgressTrackerBean progressTrackerBean) {
        this.progressTrackerBean = progressTrackerBean;
    }

    /**
     * send files to an ftp server
     * @param files map of filenames and corresponding input streams
     * @param filenamePrefix prefix for data files and transfile
     * @param transfileTemplate transfile content template
     * @param gzip zip output contents (not transfile)?
     * @param progressKey file x out of y
     * @param allowResume allow a previous failed transfer to be resumed from where it was left.
     */
    public void send(Set<FileHarvest> files, String filenamePrefix, String transfileTemplate, Boolean gzip,
                     ProgressTrackerBean.Key progressKey, boolean allowResume) throws HarvestException    {
        final Stopwatch stopwatch = new Stopwatch();
        try {
            final List<String> filenames = files.stream()
                    .map( fileHarvest-> fileHarvest.getUploadFilename(filenamePrefix) )
                    .sorted()
                    .collect( Collectors.toList() );
            progressTrackerBean.init(progressKey, files.size());
            LOGGER.info("Files to upload to ftp: {}", String.join(",", filenames));
            final String transfileContent = TransfileGenerator
                    .generateTransfile(transfileTemplate, filenames, gzip);
            final String transfileName = String.format("%s.%s.trans",
                    filenamePrefix, APPLICATION_ID);
            FtpClient ftpClient = FtpClientFactory.createFtpClient( host, Integer.parseInt(port),
                    username, password, dir, null );
            String filename = "";
            try {
                for (FileHarvest fileHarvest : files) {
                    filename = fileHarvest.getUploadFilename(filenamePrefix);
                    upload(gzip, ftpClient, fileHarvest, filename, allowResume);
                    fileHarvest.close();
                    progressTrackerBean.get(progressKey).inc();
                }
                LOGGER.info("Uploading transfile {} with content: {}", transfileName, transfileContent);
                ftpClient.put(transfileName, new ByteArrayInputStream(
                                transfileContent.getBytes(StandardCharsets.UTF_8)),
                        FtpClient.FileType.BINARY);
            } catch (IOException e) {
                throw new HarvestException(String.format("GZip of file '%s' failed.",filename), e);
            } finally {
                ftpClient.close();
            }
        } finally {
            LOGGER.info("send took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        }
    }

    protected void upload(boolean gzip, FtpClient ftpClient, FileHarvest fileHarvest, String filename, boolean allowResume) throws HarvestException, IOException {
        int retries = 0;
        boolean finished = false;
        String targetFilename = gzip ? filename + ".gz" : filename;
        while (!finished && retries < MAX_RETRIES) {
            try {
                if (retries > 0 && allowResume) {
                    fileHarvest.setResumePoint(getExistingFileSize(ftpClient, targetFilename));
                    ftpClient.append(
                            targetFilename,
                            gzip ? new GzipCompressingInputStream(fileHarvest.getContent()) : fileHarvest.getContent(),
                            FtpClient.FileType.BINARY);
                } else {
                    ftpClient.put(
                            targetFilename,
                            gzip ? new GzipCompressingInputStream(fileHarvest.getContent()) : fileHarvest.getContent(),
                            FtpClient.FileType.BINARY);
                }
                finished = true;
            } catch (FtpClientException clientException) {
                LOGGER.error("FTP upload to {} of file {} failed. Retry: {}", host, targetFilename, retries, clientException);
                ftpClient.close();
                ftpClient.connect().cd(dir);
                retries = retries + 1;
            }
        }
    }


    private long getExistingFileSize(FtpClient ftpClient, String targetFilename) {
        return ftpClient.ls().stream().filter(f -> targetFilename.trim().equals(f.getName().trim())).map(FTPFile::getSize).findFirst().orElse(0L);
    }
}
