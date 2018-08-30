/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class SocksConnectionFactory implements
        HttpUrlConnectorProvider.ConnectionFactory {
    final Proxy proxy;
    public SocksConnectionFactory(String url, int port) {
        InetSocketAddress address = new InetSocketAddress(url, port);
        proxy = new Proxy(Proxy.Type.SOCKS, address);
    }
    @Override
    public HttpURLConnection getConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection(proxy);
    }
}
