package dk.dbc.saturn;

import dk.dbc.commons.sftpclient.SFTPConfig;
import dk.dbc.commons.sftpclient.SFtpClient;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class SFtpClientFactoryIT extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SFtpClientFactoryIT.class);

    @Test
    public void test_simpleTest() throws IOException {
        try (SFtpClient sFtpClient = getSftpClient()) {
            LOGGER.info("host: {}, user: {}, dir:{}", SFTP_ADDRESS, SFTP_USER, SFTP_DIR);
            sFtpClient.putContent("hej.txt", new ByteArrayInputStream("Hej".getBytes()));
            LOGGER.info("pwd:{}", sFtpClient.pwd());
            String actual = new String(readInputStream(sFtpClient.getContent("hej.txt")));
            assertThat("Filecontent", actual, is("Hej"));
        }
    }
    protected static String readInputStream(InputStream is) throws IOException {
        try (final BufferedReader in = new BufferedReader(
                new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

}
