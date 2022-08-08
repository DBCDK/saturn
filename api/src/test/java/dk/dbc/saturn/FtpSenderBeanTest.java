/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class FtpSenderBeanTest extends AbstractFtpBeanTest {
    @Test
    void send() throws IOException, HarvestException {
        FtpSenderBean ftpSenderBean = getFtpSenderBean();
        Set<FileHarvest> inputStreams = getFileHarvests("krusty", "sponge", "bob");
        final String transfile = "b=transfile";
        final String transfileName = "krusty.saturn.trans";
        ftpSenderBean.send(inputStreams, "krusty", transfile, false, new ProgressTrackerBean.Key(FtpHarvesterConfig.class, 0));

        FtpClient ftpClient = getFtpClient();

        assertThat("file 1", readInputStream(ftpClient.get("krusty.sponge")), is("sponge"));
        assertThat("file 2", readInputStream(ftpClient.get("krusty.bob")), is("bob"));
        assertThat("transfile", readInputStream(ftpClient.get(transfileName)), is("b=transfile,f=krusty.bob\nb=transfile,f=krusty.sponge\nslut"));

        ftpClient.close();
    }

    @Test
    void send_zipped() throws IOException, HarvestException {
        FtpSenderBean ftpSenderBean = getFtpSenderBean();
        Set<FileHarvest> inputStreams = getFileHarvests("krusty", "sponge", "bob");
        final String transfile = "b=transfile";
        final String transfileName = "krusty.saturn.trans";
        ftpSenderBean.send(inputStreams, "krusty", transfile, true, new ProgressTrackerBean.Key(FtpHarvesterConfig.class, 0));

        FtpClient ftpClient = getFtpClient();

        assertThat("file 1", decompressToString(ftpClient.get("krusty.sponge.gz", FtpClient.FileType.BINARY)), is("sponge"));
        assertThat("file 2", decompressToString(ftpClient.get("krusty.bob.gz", FtpClient.FileType.BINARY)), is("bob"));
        assertThat("transfile", readInputStream(ftpClient.get(transfileName)), is("b=transfile,f=krusty.bob.gz\nb=transfile,f=krusty.sponge.gz\nslut"));

        ftpClient.close();
    }

    private FtpSenderBean getFtpSenderBean() {
        FtpSenderBean ftpSenderBean = new FtpSenderBean();
        ftpSenderBean.host = "localhost";
        ftpSenderBean.port = String.valueOf(fakeFtpServer.getServerControlPort());
        ftpSenderBean.username = USERNAME;
        ftpSenderBean.password = PASSWORD;
        ftpSenderBean.dir = PUT_DIR;
        return ftpSenderBean;
    }

    private FtpClient getFtpClient() {
        return FtpClientFactory.createFtpClient("localhost", fakeFtpServer.getServerControlPort(), USERNAME, PASSWORD, PUT_DIR, null);
    }

    private Set<FileHarvest> getFileHarvests(String filenamePrefix, String... contentList) {
        final Set<FileHarvest> fileHarvests = new HashSet<>(contentList.length);
        for (String content : contentList) {
            fileHarvests.add(new MockFileHarvest(content, content, 0));
        }
        return fileHarvests;
    }

}
