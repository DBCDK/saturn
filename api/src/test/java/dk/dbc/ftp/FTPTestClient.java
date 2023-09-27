package dk.dbc.ftp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class FTPTestClient extends FtpClient{
    private boolean fakeFtpUploadError;
    private final int FAILAFTERBYTES = 25000;
    private static final Logger LOGGER = LoggerFactory.getLogger(FTPTestClient.class);

    public void setFakeFtpUploadError(boolean fakeFtpUploadError) {
        this.fakeFtpUploadError = fakeFtpUploadError;
    }

    @Override
    public FtpClient put(String remote, InputStream inputStream, FileType fileType) {
        LOGGER.info("Putting: {}", remote);
        try {
            super.put(remote, fakeFtpUploadError ? new ByteCountingFailInputStream(inputStream, FAILAFTERBYTES) : inputStream, fileType);
        } catch (FtpClientException e) {
            LOGGER.error("Ftp client exception, closing connection.", e);
            throw e;
        }
        return this;
    }

    @Override
    public FtpClient append(String remote, InputStream inputStream, FileType fileType) {
        LOGGER.info("Appending remote: {}", remote);
        try {
            super.append(remote, fakeFtpUploadError ? new ByteCountingFailInputStream(inputStream, FAILAFTERBYTES) : inputStream, fileType);
        } catch (FtpClientException e) {
            LOGGER.error("Ftp client exception, closing connection.", e);
            throw e;
        }
        return this;
    }
}
