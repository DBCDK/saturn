package dk.dbc.saturn;

import dk.dbc.commons.sftpclient.SFTPConfig;
import dk.dbc.commons.sftpclient.SFtpClient;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

public class SFtpContainer extends GenericContainer<SFtpContainer> {
    public final String user;
    public final String password;
    public final String dir;
    private final int port = 22;

    public SFtpContainer(String dockerImageName, String user, String password, String dir) {
        super(dockerImageName);
        this.user = user;
        this.password = password;
        this.dir = dir;
    }

    @SuppressWarnings("resource")
    public SFtpContainer go() {
        withCommand(user + ":" + password + ":::" + dir);
        withExposedPorts(port);
        withStartupTimeout(Duration.ofMinutes(1));
        start();
        return this;
    }

    public int getPort() {
        return getMappedPort(port);
    }

    public SFtpClient createClient() {
        return new SFtpClient(
                new SFTPConfig()
                        .withHost(AbstractIntegrationTest.SFTP_CONTAINER.getHost())
                        .withUsername(user)
                        .withPassword(password)
                        .withPort(AbstractIntegrationTest.SFTP_CONTAINER.getMappedPort(22))
                        .withDir(dir)
                        .withFilesPattern("*"), null);
    }
}
