package dk.dbc.saturn;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public abstract class AbstractFtpBeanTest {
    static final String USERNAME = "FtpClientTest";
    static final String PASSWORD = "FtpClientTestPass";
    static final String HOME_DIR = "/home/ftp";
    static final String PUT_DIR = "put";
    static FakeFtpServer fakeFtpServer;
    protected static WireMockServer wireMockServer;
    protected static String wireMockHost;

    static {
        wireMockServer = new WireMockServer(options().dynamicPort()
                .dynamicHttpsPort());
        wireMockServer.start();
        wireMockHost = "http://localhost:" + wireMockServer.port();
        configureFor("localhost", wireMockServer.port());
    }

    @BeforeClass
    public static void setUp() {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.setServerControlPort(0);  // use any free port
        fakeFtpServer.addUserAccount(new UserAccount(USERNAME,
            PASSWORD, HOME_DIR));

        final FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(HOME_DIR));
        fileSystem.add(new DirectoryEntry(String.join("/", HOME_DIR, PUT_DIR)));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.start();
    }


    @AfterClass
    public static void tearDown() {
        fakeFtpServer.stop();
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

    public static String decompressToString(InputStream is) throws IOException {
        Path tmpFile = Files.createTempFile("my-gzipped", ".gz");
        try (GZIPInputStream gis = new GZIPInputStream(is);
             FileOutputStream fos = new FileOutputStream(tmpFile.toFile())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
        String returnedData = Files.readString(tmpFile);
        Files.deleteIfExists(tmpFile);
        return returnedData;
    }

    static String createFtpDir(String dirname) {
        final String dirpath = String.join("/", HOME_DIR, PUT_DIR, dirname);
        final FileSystem fileSystem = fakeFtpServer.getFileSystem();
        fileSystem.add(new DirectoryEntry(dirpath));
        return dirpath;
    }
}
