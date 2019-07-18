/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FtpHarvesterBeanTest extends AbstractFtpBeanTest {
    @Test
    public void test_harvest() throws IOException {
        final String putFile1 = "bb.txt";
        final String putFile2 = "mm.txt";
        final FtpClient ftpClient = new FtpClient()
            .withHost("localhost")
            .withPort(fakeFtpServer.getServerControlPort())
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .cd(PUT_DIR);
        ftpClient.put(putFile1, "Barnacle Boy!");
        ftpClient.put(putFile2, "Mermaid Man!");
        ftpClient.close();

        FtpHarvesterBean ftpHarvesterBean = getFtpHarvesterBean();
        Set<FileHarvest> fileHarvests = ftpHarvesterBean.harvest(
            "localhost", fakeFtpServer.getServerControlPort(), USERNAME,
            PASSWORD, String.join("/", HOME_DIR, PUT_DIR), new FileNameMatcher(),
                new SeqnoMatcher(new FtpHarvesterConfig()));

        assertThat("result size", fileHarvests.size(), is(2));
        final Map<String, String> contentMap = new HashMap<>(2);
        contentMap.put("bb.txt", "Barnacle Boy!");
        contentMap.put("mm.txt", "Mermaid Man!");
        for (FileHarvest fileHarvest : fileHarvests) {
            assertThat(fileHarvest.getFilename(), readInputStream(fileHarvest.getContent()),
                is(contentMap.get(fileHarvest.getFilename())));
        }
    }

    @Test
    public void test_harvest_dirArgumentIsEmpty() throws IOException {
        final String putFile1 = "bb.txt";
        final String putFile2 = "mm.txt";
        final FtpClient ftpClient = new FtpClient()
            .withHost("localhost")
            .withPort(fakeFtpServer.getServerControlPort())
            .withUsername(USERNAME)
            .withPassword(PASSWORD);
        ftpClient.put(putFile1, "Barnacle Boy!");
        ftpClient.put(putFile2, "Mermaid Man!");
        ftpClient.close();
        FtpHarvesterBean ftpHarvesterBean = getFtpHarvesterBean();
        Set<FileHarvest> fileHarvests = ftpHarvesterBean.harvest(
            "localhost", fakeFtpServer.getServerControlPort(), USERNAME,
            PASSWORD, "", new FileNameMatcher("*.txt"),
            new SeqnoMatcher(new FtpHarvesterConfig()));
        assertThat("result size", fileHarvests.size(), is(2));
        final Map<String, String> contentMap = new HashMap<>(2);
        contentMap.put("bb.txt", "Barnacle Boy!");
        contentMap.put("mm.txt", "Mermaid Man!");
        for (FileHarvest fileHarvest : fileHarvests) {
            assertThat(fileHarvest.getFilename(), readInputStream(fileHarvest.getContent()),
                is(contentMap.get(fileHarvest.getFilename())));
        }
    }

    @Test
    void test_harvest_seqnoFilenameLeadingSpace() throws IOException {
        final String putFile1 = " 12v24.txt";
        final FtpClient ftpClient = new FtpClient()
            .withHost("localhost")
            .withPort(fakeFtpServer.getServerControlPort())
            .withUsername(USERNAME)
            .withPassword(PASSWORD);
        ftpClient.put(putFile1, "Barnacle Boy!");
        ftpClient.close();

        FtpHarvesterBean ftpHarvesterBean = getFtpHarvesterBean();
        FtpHarvesterConfig config = new FtpHarvesterConfig();
        config.setSeqnoExtract("1-2,4-5");
        Set<FileHarvest> fileHarvests = ftpHarvesterBean.harvest(
            "localhost", fakeFtpServer.getServerControlPort(), USERNAME,
            PASSWORD, "", new FileNameMatcher("*.txt"),
            new SeqnoMatcher(config));

        assertThat("result size", fileHarvests.size(), is(1));
        final Map<String, String> contentMap = new HashMap<>(1);
        contentMap.put(" 12v24.txt", "Barnacle Boy!");
        for (FileHarvest fileHarvest : fileHarvests) {
            assertThat(fileHarvest.getFilename(), readInputStream(fileHarvest.getContent()),
                is(contentMap.get(fileHarvest.getFilename())));
        }
    }

    private static FtpHarvesterBean getFtpHarvesterBean() {
        FtpHarvesterBean ftpHarvesterBean = new FtpHarvesterBean();
        ftpHarvesterBean.proxyHandlerBean = new ProxyHandlerBean();
        return ftpHarvesterBean;
    }
}
