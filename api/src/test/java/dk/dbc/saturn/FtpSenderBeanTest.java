/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class FtpSenderBeanTest extends AbstractFtpBeanTest {
    @Test
    void send() throws IOException  {
        FtpSenderBean ftpSenderBean = getFtpSenderBean();
        Set<FileHarvest> inputStreams = getFileHarvests("sponge", "bob");
        final String transfile = "b=transfile";
        final String transfileName = "krusty.saturn.trans";
        ftpSenderBean.send(inputStreams, "krusty", transfile);

        FtpClient ftpClient = getFtpClient();

        assertThat("file 1", readInputStream(ftpClient.get("krusty.sponge")),
            is("sponge"));
        assertThat("file 2", readInputStream(ftpClient.get("krusty.bob")),
            is("bob"));
        assertThat("transfile", readInputStream(ftpClient.get(transfileName)),
            is("b=transfile,f=krusty.bob\nb=transfile,f=krusty.sponge\nslut"));

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

    private Set<FileHarvest> getFileHarvests(String ...contentList) {
        final Set<FileHarvest> fileHarvests = new HashSet<>(contentList.length);
        for (String content : contentList) {
            try {
                fileHarvests.add(new FileHarvest(content,
                        new ByteArrayInputStream(content.getBytes("utf8")),
                        null));
            } catch (UnsupportedEncodingException e) {
                fileHarvests.add(new FileHarvest(content,
                        new ByteArrayInputStream(e.toString().getBytes()), null));
                    }
        }
        return fileHarvests;
    }
}
