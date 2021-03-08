/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import com.jcraft.jsch.ProxySOCKS5;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Objects;
import java.util.Set;

@Startup
@Singleton
@Lock(LockType.READ)
public class ProxyHandlerBean {
    @Inject
    @ConfigProperty(name = "PROXY_HOSTNAME")
    String proxyHostname;

    @Inject
    @ConfigProperty(name = "PROXY_PORT", defaultValue = "0")
    String proxyPort;

    @Inject
    @ConfigProperty(name = "PROXY_USERNAME")
    String proxyUsername;

    @Inject
    @ConfigProperty(name = "PROXY_PASSWORD")
    String proxyPassword;

    @Inject
    @ConfigProperty(name = "NON_PROXY_HOSTS")
    Set<String> nonProxyHosts;

    public String getProxyHostname() {
        return proxyHostname;
    }

    public int getProxyPort() {
        return Integer.valueOf(proxyPort);
    }

    public Set<String> getNonProxyHosts() {
        return nonProxyHosts;
    }

    public boolean useProxy(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return false;
        }
        return nonProxyHosts.stream()
                .filter(Objects::nonNull)
                .filter(domain -> !domain.isEmpty())
                .noneMatch(hostname::endsWith);
    }

    @PostConstruct
    public void init() {
        setAuthentication();
    }

    public void setAuthentication() {
        Authenticator.setDefault(
            new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    if(proxyUsername == null || proxyPassword == null) {
                        return null;
                    }
                    if(getRequestingHost().equalsIgnoreCase(proxyHostname)) {
                        return new PasswordAuthentication(
                            proxyUsername, proxyPassword.toCharArray());
                    }
                    return null;
                }
            }
        );
    }

    public ProxySOCKS5 getProxySOCKS5() {
        if (proxyHostname != null && !proxyHostname.isEmpty() &&
            proxyPort != null && !proxyPort.isEmpty()) {
            final ProxySOCKS5 proxy = new ProxySOCKS5(proxyHostname, Integer.parseInt(proxyPort));
            proxy.setUserPasswd(proxyUsername, proxyPassword);
            return proxy;
        }
        else return null;
    }

    public HttpUrlConnectorProvider getHttpUrlConnectorProvider() throws HarvestException {
        if (proxyHostname == null || proxyHostname.isEmpty()) {
            throw new HarvestException("proxy host must be configured");
        }
        final int proxyPort = getProxyPort();
        if (proxyPort <= 0 || proxyPort > 65535) {
            throw new HarvestException("illegal proxy port: " + proxyPort);
        }

        final SocksConnectionFactory connectionFactory = new SocksConnectionFactory(proxyHostname, proxyPort);
        final HttpUrlConnectorProvider connectorProvider = new HttpUrlConnectorProvider();
        connectorProvider.connectionFactory(connectionFactory);
        return connectorProvider;
    }
}
