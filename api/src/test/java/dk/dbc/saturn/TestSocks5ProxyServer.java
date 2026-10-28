package dk.dbc.saturn;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test-only SOCKS5 server. Do not use outside of tests: it has no
 * authentication, no timeouts and no limits on connections or threads.
 * <p>
 * Minimal SOCKS5 proxy (RFC 1928), listening on an ephemeral loopback port. Each requested destination is recorded as "host:port" and
 * available through {@link #getDestinations()}.
 * <p>
 * Per client connection the server performs these steps:
 * <ol>
 *     <li>Method negotiation: reads {@code VER NMETHODS METHODS...}, and
 *     answers {@code 05 00} if "no authentication" is offered, otherwise
 *     {@code 05 FF} and closes the connection.</li>
 *     <li>Connect request: reads {@code VER CMD RSV ATYP DST.ADDR DST.PORT}.
 *     Only CONNECT is accepted, with IPv4, IPv6 or domain name addresses.
 *     Anything else is answered with status "command not supported".</li>
 *     <li>Reply: connects to the destination and answers "succeeded", or
 *     "general failure" if the connection could not be established. No
 *     reply is sent after "succeeded", so relayed data is never mixed with
 *     protocol bytes.</li>
 *     <li>Relay: copies bytes in both directions until each side closes,
 *     without interpreting them, so any TCP protocol can pass through.</li>
 * </ol>
 * <p>
 * Not supported: username/password authentication, BIND and UDP ASSOCIATE.
 * The version and reserved bytes of the connect request are not validated.
 * <p>
 * The JDK uses SOCKS5 for a {@link java.net.Proxy.Type#SOCKS} proxy, which is
 * what {@link dk.dbc.proxy.ProxyBean} creates.
 */
final class TestSocks5ProxyServer implements AutoCloseable {
    private static final int VERSION = 5;
    private static final int NO_AUTHENTICATION = 0;
    private static final int NO_ACCEPTABLE_METHODS = 0xFF;
    private static final int CONNECT = 1;
    private static final int ATYP_IPV4 = 1;
    private static final int ATYP_DOMAIN = 3;
    private static final int ATYP_IPV6 = 4;
    private static final int REPLY_SUCCEEDED = 0;
    private static final int REPLY_GENERAL_FAILURE = 1;
    private static final int REPLY_COMMAND_NOT_SUPPORTED = 7;

    private final ServerSocket serverSocket;
    private final List<String> destinations = new CopyOnWriteArrayList<>();

    TestSocks5ProxyServer() throws IOException {
        serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        final Thread acceptor = new Thread(this::acceptLoop, "socks5-proxy-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    List<String> getDestinations() {
        return List.copyOf(destinations);
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                final Socket client = serverSocket.accept();
                startDaemon(() -> handle(client), "socks5-proxy-client");
            } catch (IOException e) {
                return;
            }
        }
    }

    private void handle(Socket client) {
        try (client) {
            final DataInputStream in = new DataInputStream(client.getInputStream());
            final OutputStream out = client.getOutputStream();
            if (!negotiateMethod(in, out)) {
                return;
            }
            final String host = readConnectRequest(in, out);
            if (host == null) {
                return;
            }
            final int port = in.readUnsignedShort();
            destinations.add(host + ":" + port);
            final Socket target;
            try {
                target = new Socket(host, port);
            } catch (IOException e) {
                reply(out, REPLY_GENERAL_FAILURE);
                return;
            }
            try (target) {
                reply(out, REPLY_SUCCEEDED);
                final Thread upstream = startDaemon(() -> relay(in, target), "socks5-proxy-upstream");
                relay(target.getInputStream(), client);
                upstream.join();
            }
        } catch (IOException | InterruptedException e) {
            // connection dropped, nothing to clean up beyond closing sockets
        }
    }

    private static boolean negotiateMethod(DataInputStream in, OutputStream out) throws IOException {
        if (in.readUnsignedByte() != VERSION) {
            return false;
        }
        final byte[] methods = in.readNBytes(in.readUnsignedByte());
        for (byte method : methods) {
            if (method == NO_AUTHENTICATION) {
                out.write(new byte[]{VERSION, NO_AUTHENTICATION});
                out.flush();
                return true;
            }
        }
        out.write(new byte[]{VERSION, (byte) NO_ACCEPTABLE_METHODS});
        out.flush();
        return false;
    }

    private static String readConnectRequest(DataInputStream in, OutputStream out) throws IOException {
        in.readUnsignedByte();
        final int command = in.readUnsignedByte();
        in.readUnsignedByte();
        final int addressType = in.readUnsignedByte();
        final String host = switch (addressType) {
            case ATYP_IPV4 -> InetAddress.getByAddress(in.readNBytes(4)).getHostAddress();
            case ATYP_IPV6 -> InetAddress.getByAddress(in.readNBytes(16)).getHostAddress();
            case ATYP_DOMAIN -> new String(in.readNBytes(in.readUnsignedByte()), StandardCharsets.US_ASCII);
            default -> null;
        };
        if (command != CONNECT || host == null) {
            reply(out, REPLY_COMMAND_NOT_SUPPORTED);
            return null;
        }
        return host;
    }

    private static void reply(OutputStream out, int status) throws IOException {
        out.write(new byte[]{VERSION, (byte) status, 0, ATYP_IPV4, 0, 0, 0, 0, 0, 0});
        out.flush();
    }

    private static void relay(InputStream from, Socket to) {
        try {
            from.transferTo(to.getOutputStream());
            to.shutdownOutput();
        } catch (IOException e) {
            // peer closed the connection
        }
    }

    private static Thread startDaemon(Runnable runnable, String name) {
        final Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
