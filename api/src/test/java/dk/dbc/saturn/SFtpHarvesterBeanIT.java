/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.commons.sftpclient.SFTPConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import dk.dbc.commons.sftpclient.SFtpClient;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class SFtpHarvesterBeanIT extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SFtpHarvesterBeanIT.class);
    private static final SFTPConfig sftpConfig = new SFTPConfig()
            .withHost(SFTP_ADDRESS)
            .withUsername(SFTP_USER)
            .withPassword(SFTP_PASSWORD)
            .withPort(SFTP_PORT)
            .withDir("upload")
            .withFilesPattern("*");


    @Test
    public void test_harvest() throws IOException, HarvestException {
        final String putFile1 = "bb.txt";
        final String putFile2 = "mm.txt";

        try (final SFtpClient sftpClient = new SFtpClient(sftpConfig, null)) {
            sftpClient.putContent(putFile1, toInputStream("Barnacle Boy!"));
            sftpClient.putContent(putFile2, toInputStream("Mermaid Man!"));
        }

        SFtpHarvesterBean sFtpHarvesterBean = getSFtpHarvesterBean();

        SFtpHarvesterConfig config = getSFtpHarvesterConfig(
                SFTP_ADDRESS, SFTP_USER, SFTP_PASSWORD, SFTP_DIR, SFTP_PORT, "*");
        Set<FileHarvest> fileHarvests = sFtpHarvesterBean.listFiles( config );
        assertThat("result size", fileHarvests.size(), greaterThanOrEqualTo(2));
        final Map<String, String> contentMap = new HashMap<>(2);
        contentMap.put("bb.txt", "Barnacle Boy!");
        contentMap.put("mm.txt", "Mermaid Man!");
        for (FileHarvest fileHarvest : fileHarvests) {
            if (contentMap.containsKey(fileHarvest.getFilename())) {
                assertThat(fileHarvest.getFilename(), readInputStream(fileHarvest.getContent()),
                        is(contentMap.get(fileHarvest.getFilename())));
                fileHarvest.close();
            }
        }
        removeFilesAndDir("", Arrays.asList(putFile1, putFile2));
    }

    @Test
    void test_harvest_seqnoFilenameLeadingSpace() throws IOException, HarvestException {
        final String putFile1 = " 12v24.txt";
        try (final SFtpClient sftpClient = new SFtpClient(sftpConfig, null)) {
            sftpClient.putContent(putFile1, toInputStream("Barnacle Boy!"));
        }

        SFtpHarvesterBean sFtpHarvesterBean = getSFtpHarvesterBean();
        final SFtpHarvesterConfig config = getSFtpHarvesterConfig(
                SFTP_ADDRESS, SFTP_USER, SFTP_PASSWORD, SFTP_DIR, SFTP_PORT, "*.txt");
        config.setSeqnoExtract("1-2,4-5");
        Set<FileHarvest> fileHarvests = sFtpHarvesterBean.listFiles( config );

        assertThat("result size", fileHarvests.size(), is(1));
        final Map<String, String> contentMap = new HashMap<>(1);
        contentMap.put(" 12v24.txt", "Barnacle Boy!");
        for (FileHarvest fileHarvest : fileHarvests) {
            assertThat(fileHarvest.getFilename(), readInputStream(fileHarvest.getContent()),
                is(contentMap.get(fileHarvest.getFilename())));
        }
    }

    @Test
    public void test_listAllFiles() {
        final String putFile1 = "file1.xml";
        final String putFile2 = "file2.txt";
        final String putFile3 = "file3.txt";
        final String listAllFiles = "listAllFiles";

        try (SFtpClient sFtpClient = new SFtpClient(sftpConfig, null)) {
            sFtpClient.mkdir(listAllFiles);
            sFtpClient.cd(listAllFiles);
            sFtpClient.putContent(putFile1, toInputStream("file1"));
            sFtpClient.putContent(putFile2, toInputStream("file2"));
            sFtpClient.putContent(putFile3, toInputStream("file3"));
        }

        final SFtpHarvesterBean sFtpHarvesterBean = getSFtpHarvesterBean();
        final String ftpDir = String.join("/", SFTP_DIR, listAllFiles);
        final SFtpHarvesterConfig config = getSFtpHarvesterConfig(
                SFTP_ADDRESS, SFTP_USER, SFTP_PASSWORD, ftpDir, SFTP_PORT, "*.txt");
        config.setSeqnoExtract("5");
        config.setSeqno(2);

        final Set<FileHarvest> fileHarvests = sFtpHarvesterBean.listAllFiles(config);

        assertThat("result size", fileHarvests.size(), is(3));
        final Set<FileHarvest> expectedFileHarvests = new HashSet<>();
        expectedFileHarvests.add(new SFtpFileHarvest(
                ftpDir, putFile1, null, null, FileHarvest.Status.SKIPPED_BY_FILENAME));
        expectedFileHarvests.add(new SFtpFileHarvest(
                ftpDir, putFile2, null, null, FileHarvest.Status.SKIPPED_BY_SEQNO));
        expectedFileHarvests.add(new SFtpFileHarvest(
                ftpDir, putFile3, null, null, FileHarvest.Status.AWAITING_DOWNLOAD));


        assertThat(fileHarvests, is(expectedFileHarvests));
        removeFilesAndDir(listAllFiles, Arrays.asList(putFile1, putFile2, putFile3));
    }

    private static SFtpHarvesterBean getSFtpHarvesterBean() {
        SFtpHarvesterBean sFtpHarvesterBean = new SFtpHarvesterBean();
        sFtpHarvesterBean.proxyHandlerBean = new ProxyHandlerBean();
        return sFtpHarvesterBean;
    }

    private static SFtpHarvesterConfig getSFtpHarvesterConfig( String host, String username,
                                                             String password, String dir,
                                                             int port, String filesPattern ){
        SFtpHarvesterConfig config = new SFtpHarvesterConfig();
        config.setHost(host);
        config.setUsername(username);
        config.setPassword(password);
        config.setDir(dir);
        config.setPort(port);
        config.setFilesPattern(filesPattern);
        return config;
    }

    private static void removeFilesAndDir(String subdir, List<String> files) {
        try (SFtpClient sFtpClient = new SFtpClient(sftpConfig, null)) {
            if (!subdir.isEmpty()) {
                sFtpClient.cd(subdir);
            }
            for(String file: files) {
                sFtpClient.rm(file);
            }
            sFtpClient.cd("..");
            if (!subdir.isEmpty()) {
                sFtpClient.rmdir(subdir);
            }
        }
    }

    private static ByteArrayInputStream toInputStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
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
