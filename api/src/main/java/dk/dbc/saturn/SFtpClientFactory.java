package dk.dbc.saturn;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SFtpClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            SFtpClientFactory.class);

        final private static String knownHostsFile = "/tmp/known_hosts";

    private SFtpClientFactory() {}

    private static void makeAUTHORIZED_FILES(SFtpHarvesterConfig config) throws IOException, InterruptedException {
        final Process p = Runtime.getRuntime()
                .exec(String
                        .format("ssh-keyscan -H -t rsa %s > %s", config.getHost(), knownHostsFile));
        p.waitFor();
    }

    private static ChannelSftp createSFtpClient(SFtpHarvesterConfig config, ProxyHandlerBean proxyHandlerBean) throws JSchException, IOException, InterruptedException, SftpException {
        makeAUTHORIZED_FILES(config);
        Properties jschConfig = new Properties();
        LOGGER.info("Trying to connect to '{}' at port '{}' with user '{}' at path '{}'",
                config.getHost(),
                config.getPort(),
                config.getUsername(),
                config.getDir());
        jschConfig.setProperty("StrictHostKeyChecking", "no");
        JSch jsch = new JSch();
        Session session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
        if( proxyHandlerBean != null && proxyHandlerBean.getProxyHostname() != null &&
                proxyHandlerBean.getProxyPort() != 0 ) {
            final InetSocketAddress address = new InetSocketAddress(
                    proxyHandlerBean.getProxyHostname(),
                    proxyHandlerBean.getProxyPort());
            final ProxySOCKS5 proxy = new ProxySOCKS5(config.getHost(), config.getPort());
            session.setProxy(proxy);
        }
        session.setPassword(config.getPassword());
        session.setConfig(jschConfig);
        session.connect();
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        channelSftp.cd(config.getDir());
        return channelSftp ;
    }



    public static ChannelSftp createSFtpClient(String host, int port,
                                             String username, String password, String dir,
                                             ProxyHandlerBean proxyHandlerBean)
            throws InterruptedException, JSchException, IOException, SftpException {
        SFtpHarvesterConfig config = new SFtpHarvesterConfig();
        config.setHost(host);
        config.setPort(port);
        config.setUsername(username);
        config.setPassword(password);
        config.setDir(dir);
        return createSFtpClient(config, proxyHandlerBean);
    }



}