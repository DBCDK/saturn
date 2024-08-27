package dk.dbc.saturn;

import dk.dbc.commons.sftpclient.SFtpClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class SFtpClientFactoryIT extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SFtpClientFactoryIT.class);

    @Test
    public void test_simpleTest() throws IOException {
        try(SFtpClient sFtpClient = SFTP_CONTAINER.createClient()) {
            LOGGER.info("host: {}, user: {}, dir:{}", SFTP_CONTAINER.getHost(), SFTP_CONTAINER.user, SFTP_CONTAINER.dir);
            sFtpClient.putContent("hej.txt", new ByteArrayInputStream("Hej".getBytes()));
            LOGGER.info("pwd:{}", sFtpClient.pwd());
            String actual = new String(sFtpClient.getContent("hej.txt").readAllBytes());
            assertThat("Filecontent", actual, is("Hej"));
        }
    }
}
