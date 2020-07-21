package dk.dbc.saturn;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class SFtpClientFactoryIT extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SFtpClientFactoryIT.class);

    @Test
    public void test_simpleTest() throws InterruptedException, SftpException, JSchException, IOException {
        LOGGER.info("host: {}, user: {}, dir:{}", SFTP_ADDRESS, SFTP_USER, SFTP_DIR);
        ChannelSftp channelSftp = SFtpClientFactory.createSFtpClient(SFTP_ADDRESS, SFTP_PORT, SFTP_USER, SFTP_PASSWORD, SFTP_DIR, null);
        LOGGER.info("LS:{}",channelSftp.ls("*"));
    }

}