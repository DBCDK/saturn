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
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FtpHarvesterBeanTest extends AbstractFtpBeanTest {
    @Test
    public void test_harvest() throws IOException, ExecutionException, InterruptedException {
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
            PASSWORD, PUT_DIR, new FileNameMatcher(),
                new SeqnoMatcher(new FtpHarvesterConfig())).get();

        assertThat("result size", fileHarvests.size(), is(2));
        final Map<String, String> contentMap = new HashMap<>(2);
        contentMap.put("bb.txt", "Barnacle Boy!");
        contentMap.put("mm.txt", "Mermaid Man!");
        for (FileHarvest fileHarvest : fileHarvests) {
            assertThat(fileHarvest.getFilename(), readInputStream(fileHarvest.getContent()),
                is(contentMap.get(fileHarvest.getFilename())));
        }
    }

    private static FtpHarvesterBean getFtpHarvesterBean() {
        return new FtpHarvesterBean();
    }
}
