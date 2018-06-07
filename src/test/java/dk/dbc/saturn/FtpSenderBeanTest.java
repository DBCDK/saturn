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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class FtpSenderBeanTest extends AbstractFtpBeanTest {
    @Test
    void send() throws IOException  {
        FtpSenderBean ftpSenderBean = getFtpSenderBean();
        List<String> names = Arrays.asList("sponge", "bob");
        List<InputStream> inputStreams = getInputStreams(names);
        ftpSenderBean.send(inputStreams, names);

        FtpClient ftpClient = getFtpClient();

        assertThat("file 1", readInputStream(ftpClient.get("sponge")),
            is("sponge"));
        assertThat("file 2", readInputStream(ftpClient.get("bob")),
            is("bob"));

        ftpClient.close();
    }

    @Test
    void send_fileNumberMismatch() {
        FtpSenderBean ftpSenderBean = getFtpSenderBean();
        List<InputStream> inputStreams = Collections.emptyList();
        List<String> names = Collections.singletonList("Larry");
        try {
            ftpSenderBean.send(inputStreams, names);
            fail("expected illegal argument exception");
        } catch(IllegalArgumentException e) {}
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

    private List<InputStream> getInputStreams(List<String> contentList) {
        return contentList.stream().map(name -> {
            try {
                return new ByteArrayInputStream(name.getBytes("utf8"));
            } catch(UnsupportedEncodingException e) {
                return new ByteArrayInputStream(e.toString().getBytes());
            }
        }).collect(Collectors.toList());
    }
}
