package dk.dbc.saturn;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class AbstractFtpBeanTest {
    static final String USERNAME = "FtpClientTest";
    static final String PASSWORD = "FtpClientTestPass";
    static final String HOME_DIR = "/home/ftp";
    static final String PUT_DIR = "put";

    static FakeFtpServer fakeFtpServer;

    @BeforeAll
    static void setUp() {
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


    @AfterAll
    static void tearDown() {
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

    static String createFtpDir(String dirname) {
        final String dirpath = String.join("/", HOME_DIR, PUT_DIR, dirname);
        final FileSystem fileSystem = fakeFtpServer.getFileSystem();
        fileSystem.add(new DirectoryEntry(dirpath));
        return dirpath;
    }
}
