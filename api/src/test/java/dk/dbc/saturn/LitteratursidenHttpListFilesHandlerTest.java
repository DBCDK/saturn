/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class LitteratursidenHttpListFilesHandlerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitteratursidenHttpListFilesHandlerTest.class);
    private static WireMockServer wireMockServer;
    private static ClientAndProxy mockProxy;
    private static String wireMockHost;

    @BeforeAll
    public static void setUp() throws IOException {
        wireMockServer = new WireMockServer(options().dynamicPort().dynamicHttpsPort());
        wireMockServer.start();
        wireMockHost = "http://localhost:" + wireMockServer.port();
        configureFor("localhost", wireMockServer.port());

        // mockserver doesn't seem to be able to dynamically allocate an
        // available port when started with the maven plugin so therefore we
        // start it manually.
        final ServerSocket socket = new ServerSocket(0);
        final int proxyPort = socket.getLocalPort();
        socket.close();
        mockProxy = ClientAndProxy.startClientAndProxy(proxyPort);
    }

    @AfterAll
    public static void tearDown() {
        wireMockServer.stop();
        mockProxy.stop();
    }

    @Test
    void listFiles() throws HarvestException {
        wireMockServer.stubFor(get(urlEqualTo("/rest-output-dbc/24h?page=0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[{\"id\":\"rec1\"}]")));
        wireMockServer.stubFor(get(urlEqualTo("/rest-output-dbc/24h?page=1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[{\"id\":\"rec2\"},{\"id\":\"rec3\"}]")));
        wireMockServer.stubFor(get(urlEqualTo("/rest-output-dbc/24h?page=2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[]")));

        final LitteratursidenHttpListFilesHandler litteratursidenHttpListFilesHandler =
                getLitteratursidenHttpListFilesHandler();
        final List<FileHarvest> fileHarvests = litteratursidenHttpListFilesHandler.listFiles(getHttpHarvesterConfig())
                .stream()
                .sorted(Comparator.comparing(FileHarvest::getFilename))
                .collect(Collectors.toList());

        assertThat("number of files to harvest", fileHarvests.size(),
                is(2));
        assertThat("1st file harvest has page suffix", fileHarvests.get(0).getFilename(),
                endsWith(".page0"));
        assertThat("1st file harvest has url", ((HttpFileHarvest)fileHarvests.get(0)).getUrl(),
                containsString("/rest-output-dbc/24h?page=0"));
        assertThat("2nd file harvest has page suffix", fileHarvests.get(1).getFilename(),
                endsWith(".page1"));
        assertThat("2nd file harvest has url", ((HttpFileHarvest)fileHarvests.get(1)).getUrl(),
                containsString("/rest-output-dbc/24h?page=1"));
    }

    private static LitteratursidenHttpListFilesHandler getLitteratursidenHttpListFilesHandler() {
        final ProxyHandlerBean proxyHandlerBean = new ProxyHandlerBean();
        proxyHandlerBean.proxyHostname = "localhost";
        proxyHandlerBean.proxyPort = String.valueOf(mockProxy.getPort());
        proxyHandlerBean.nonProxyHosts = new HashSet<>();
        proxyHandlerBean.nonProxyHosts.add("localhost");
        return new LitteratursidenHttpListFilesHandler(proxyHandlerBean, new RetryPolicy());
    }

    private static HttpHarvesterConfig getHttpHarvesterConfig() {
        final HttpHarvesterConfig config = new HttpHarvesterConfig();
        config.setListFilesHandler(HttpHarvesterConfig.ListFilesHandler.LITTERATURSIDEN);
        config.setUrl(wireMockHost + "/rest-output-dbc/24h?page=${PAGE_NO}");
        return config;
    }
}
