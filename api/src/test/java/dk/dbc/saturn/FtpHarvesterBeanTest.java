/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
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
        Map<String, InputStream> inputStreams = ftpHarvesterBean.harvest(
            "localhost", fakeFtpServer.getServerControlPort(), USERNAME,
            PASSWORD, PUT_DIR, new FileNameMatcher()).get();

        assertThat("result size", inputStreams.size(), is(2));
        assertThat(readInputStream(inputStreams.get("bb.txt")),
            is("Barnacle Boy!"));
        assertThat(readInputStream(inputStreams.get("mm.txt")),
            is("Mermaid Man!"));
    }

    private static FtpHarvesterBean getFtpHarvesterBean() {
        return new FtpHarvesterBean();
    }
}
