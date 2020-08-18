package dk.dbc.saturn.sftp.client;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import dk.dbc.saturn.ProxyHandlerBean;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFtpClient implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            SFtpClient.class);
    private Session session = null;
    private ChannelSftp channelSftp = null;
    SFtpHarvesterConfig config = null;
    ProxyHandlerBean proxyHandlerBean = null;
    private static final JSch jsch = new JSch();
    private static final Properties jschConfig = new Properties();

    static {
        jschConfig.setProperty("StrictHostKeyChecking", "no");
    }

    public SFtpClient(SFtpHarvesterConfig config, ProxyHandlerBean proxyHandlerBean)  {

        LOGGER.info("Trying to connect to '{}' at port '{}' with user '{}' at path '{}'",
                config.getHost(),
                config.getPort(),
                config.getUsername(),
                config.getDir());
        this.proxyHandlerBean = proxyHandlerBean;
        this.config = config;
        connect();
    }

    public static SFtpClient getClient(String host, String user, String password, int port, String dir, ProxyHandlerBean proxyHandlerBean) {
        SFtpHarvesterConfig sFtpHarvesterConfig = new SFtpHarvesterConfig();
        sFtpHarvesterConfig.setHost(host);
        sFtpHarvesterConfig.setUsername(user);
        sFtpHarvesterConfig.setPassword(password);
        sFtpHarvesterConfig.setPort(port);
        sFtpHarvesterConfig.setDir(dir);
        return new SFtpClient(sFtpHarvesterConfig, proxyHandlerBean);
    }

    private void connect() {
        try {
            session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
            if (proxyHandlerBean != null && proxyHandlerBean.getProxyHostname() != null &&
                    !proxyHandlerBean.getProxyHostname().isEmpty() &&
                    proxyHandlerBean.getProxyPort() != 0) {
                session.setProxy(proxyHandlerBean.getProxySOCKS5());
            }
            session.setPassword(config.getPassword());
            session.setConfig(jschConfig);
            session.connect();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.cd(config.getDir());
            LOGGER.info("Connection to '{}' was succesful.", config.getHost());
        } catch (JSchException | SftpException e) {
            throw new SFtpClientException(e);
        }
    }

    @Override
    public void close() throws SFtpClientException {

        if (channelSftp != null) {
            channelSftp.exit();
            channelSftp.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        LOGGER.info("SFtpClient for '{}' port '{}' was succesfully closed", config.getHost(), config.getPort());
    }

    private void verifyConnection() {
        if (session == null || !session.isConnected() ||
                channelSftp == null || channelSftp.isClosed()) {
            close();
            connect();
        }
    }

    public Vector<ChannelSftp.LsEntry> ls(String pattern) {
        verifyConnection();
        try {
            Vector files = channelSftp.ls(pattern);
            return files;
        } catch (SftpException e) {
            throw new SFtpClientException(e);
        }
    }

    public InputStream getContent(String filename) {
        LOGGER.info("Getting content of file:{}", filename);
        verifyConnection();
        try {
            return channelSftp.get(filename);
        } catch (SftpException e) {
            throw new SFtpClientException(e);
        }
    }

    public void putContent(String filename, InputStream content) {
        verifyConnection();
        try {
            channelSftp.put(content, filename);
        } catch (SftpException e) {
            throw new SFtpClientException(e);
        }
    }

    public String pwd() {
        verifyConnection();
        try {
            return channelSftp.pwd();
        } catch (SftpException e) {
            throw  new SFtpClientException(e);
        }
    }

    public void mkdir(String dirname) {
        verifyConnection();
        try {
            channelSftp.mkdir(dirname);
        } catch (SftpException e) {
            throw new SFtpClientException(e);
        }
    }

    public void cd(String dirname) {
        verifyConnection();
        try {
            channelSftp.cd(dirname);
        } catch (SftpException e) {
            throw new SFtpClientException(e);
        }
    }

    public void rm(String filename) {
        verifyConnection();
        try {
            channelSftp.rm(filename);
        } catch (SftpException e) {
            throw new SFtpClientException(e);
        }
    }

    public void rmdir(String dirname) {
        verifyConnection();
        try {
            channelSftp.rmdir(dirname);
        } catch (SftpException e) {
            throw new SFtpClientException(e);
        }
    }
}
