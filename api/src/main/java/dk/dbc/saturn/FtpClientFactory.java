package dk.dbc.saturn;

import dk.dbc.ftp.FtpClient;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;


public class FtpClientFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            FtpClientFactory.class);

    private FtpClientFactory() {}

    public static FtpClient createFtpClient( FtpHarvesterConfig config, ProxyHandlerBean proxyHandlerBean ){
        final String username = config.getUsername();
        final String host = config.getHost();
        final int port = config.getPort();
        final String password = config.getPassword();
        final String dir = config.getDir();


        FtpClient ftpClient = new FtpClient()
                .withHost(host)
                .withPort(port)
                .withUsername(username)
                .withPassword(password);
        if( proxyHandlerBean != null && proxyHandlerBean.getProxyHostname() != null &&
                proxyHandlerBean.getProxyPort() != 0 ) {
            final InetSocketAddress address = new InetSocketAddress(
                    proxyHandlerBean.getProxyHostname(),
                    proxyHandlerBean.getProxyPort());
            final Proxy proxy = new Proxy(Proxy.Type.SOCKS, address);
            ftpClient.withProxy(proxy);
            LOGGER.debug("using proxy for {}: proxyhost = {} proxyport = {}",
                    host,
                    proxyHandlerBean.getProxyHostname(),
                    proxyHandlerBean.getProxyPort());
        }
        if(!dir.isEmpty()) {
            ftpClient.cd(dir);
        }
        return ftpClient;
    }

    public static FtpClient createFtpClient( String host, int port,
                                             String username, String password, String dir,
                                             ProxyHandlerBean proxyHandlerBean ) {
        FtpHarvesterConfig config = new FtpHarvesterConfig();
        config.setHost( host );
        config.setPort( port );
        config.setUsername( username );
        config.setPassword( password );
        config.setDir( dir );
        return createFtpClient( config, proxyHandlerBean );
    }

}