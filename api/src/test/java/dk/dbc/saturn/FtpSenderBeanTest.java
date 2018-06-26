/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class FtpSenderBeanTest extends AbstractFtpBeanTest {
    @Test
    void send() throws IOException  {
        FtpSenderBean ftpSenderBean = getFtpSenderBean();
        Map<String, InputStream> inputStreams = getInputStreams("sponge",
            "bob");
        ftpSenderBean.send(inputStreams);

        FtpClient ftpClient = getFtpClient();

        assertThat("file 1", readInputStream(ftpClient.get("sponge")),
            is("sponge"));
        assertThat("file 2", readInputStream(ftpClient.get("bob")),
            is("bob"));

        ftpClient.close();
    }

    private FtpSenderBean getFtpSenderBean() {
        FtpSenderBean ftpSenderBean = new FtpSenderBean();
        ftpSenderBean.host = "localhost";
        ftpSenderBean.port = String.valueOf(
            fakeFtpServer.getServerControlPort());
        ftpSenderBean.username = USERNAME;
        ftpSenderBean.password = PASSWORD;
        ftpSenderBean.dir = PUT_DIR;
        return ftpSenderBean;
    }

    private FtpClient getFtpClient() {
        return new FtpClient()
            .withHost("localhost")
            .withPort(fakeFtpServer.getServerControlPort())
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .cd(PUT_DIR);
    }

    private Map<String, InputStream> getInputStreams(String ...contentList) {
        return Arrays.stream(contentList).collect(Collectors.toMap(
                Function.identity(),
            name -> {
                try {
                    return new ByteArrayInputStream(name.getBytes("utf8"));
                } catch(UnsupportedEncodingException e) {
                    return new ByteArrayInputStream(e.toString().getBytes());
                }
            }
        ));
    }
}
