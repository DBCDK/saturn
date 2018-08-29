/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

@Startup
@Singleton
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

    public String getProxyHostname() {
        return proxyHostname;
    }

    public int getProxyPort() {
        return Integer.valueOf(proxyPort);
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
}
