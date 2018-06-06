/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
        List<String> files = new ArrayList<>();
        files.add(putFile1);
        files.add(putFile2);
        List<InputStream> inputStreams = ftpHarvesterBean.harvest(
            "localhost", fakeFtpServer.getServerControlPort(), USERNAME,
            PASSWORD, PUT_DIR, files);

        assertThat(readInputStream(inputStreams.get(0)),
            is("Barnacle Boy!"));
        assertThat(readInputStream(inputStreams.get(1)),
            is("Mermaid Man!"));
    }

    private static FtpHarvesterBean getFtpHarvesterBean() {
        return new FtpHarvesterBean();
    }
}
