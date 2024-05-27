package dk.dbc.saturn;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.CustomHttpHeader;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import net.jodah.failsafe.RetryPolicy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndProxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class HTTPHarvesterBeanTest {

    private static WireMockServer wireMockServer;
    private static String wireMockHost;
    private static ClientAndProxy mockProxy;

    private final FileHarvest squarepantsFileHarvest =
            new HttpFileHarvest("squarepants.jpg", null, null, null,
                    FileHarvest.Status.AWAITING_DOWNLOAD, null);
    private final FileHarvest productsFileHarvest =
            new HttpFileHarvest("products-http.xml", null, null, null,
                    FileHarvest.Status.AWAITING_DOWNLOAD, null);
    private final FileHarvest squarepantsNoHeaderFileHarvest =
            new HttpFileHarvest("squarepants", null, null, null,
                    FileHarvest.Status.AWAITING_DOWNLOAD, null);
    private final FileHarvest squarepantsWithQueryStringFileHarvest =
            new HttpFileHarvest("squarepants%3Fpage%3D0", null, null, null,
                    FileHarvest.Status.AWAITING_DOWNLOAD, null);

    @BeforeClass
    public static void setUp() throws IOException {
        wireMockServer = new WireMockServer(options().dynamicPort()
                .dynamicHttpsPort());
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

    @AfterClass
    public static void tearDown() {
        wireMockServer.stop();
        mockProxy.stop();
    }

    @Test
    public void test_harvest() throws HarvestException, IOException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Disposition",
                                "attachment; filename=\"squarepants.jpg\"")
                        .withBody("barnacles!")
                ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        Set<FileHarvest> result = httpHarvesterBean.listFiles(
                getHttpHarvesterConfig(wireMockHost + "/spongebob/squarepants", null));
        assertThat("has squarepants harvest", result.contains(squarepantsFileHarvest),
                is(true));
        final FileHarvest fileHarvest = result.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("barnacles!"));
    }

    @Test
    public void test_harvest_with_authorization() throws HarvestException, IOException {
        final String testUrl = "/someprotected/resource";

        wireMockServer.stubFor(get(urlEqualTo(testUrl))
                .withHeader("x-service-key", matching("retailer-secret"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Disposition",
                                "attachment; filename=\"products-http.xml\"")
                        .withBody("<Products/>")
                ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        Set<FileHarvest> result = httpHarvesterBean.listFiles(
                getHttpHarvesterConfigWithHeaders(wireMockHost + testUrl,
                        null,
                        List.of(new CustomHttpHeader().withKey("x-service-key").withValue("retailer-secret"))));
        assertThat("has products harvest", result.contains(productsFileHarvest),
                is(true));
        final FileHarvest fileHarvest = result.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("<Products/>"));
    }


    @Test
    public void test_harvest_noFilenameHeader() throws HarvestException, IOException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("barnacles!")
                ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        Set<FileHarvest> result = httpHarvesterBean.listFiles(
                getHttpHarvesterConfig(wireMockHost + "/spongebob/squarepants", null));
        assertThat("has squarepants harvest", result.contains(squarepantsNoHeaderFileHarvest),
                is(true));
        final FileHarvest fileHarvest = result.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("barnacles!"));
    }

    @Test
    public void test_harvest_noFilenameHeaderWithQueryString() throws HarvestException, IOException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants?page=0"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("barnacles!")));

        final HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        final Set<FileHarvest> result = httpHarvesterBean.listFiles(
                getHttpHarvesterConfig(wireMockHost + "/spongebob/squarepants?page=0", null));

        assertThat("has squarepants harvest", result.contains(squarepantsWithQueryStringFileHarvest),
                is(true));

        final FileHarvest fileHarvest = result.iterator().next();
        final BufferedReader in = new BufferedReader(new InputStreamReader(
                fileHarvest.getContent()));
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("barnacles!"));
    }

    @Test
    public void test_harvest_noFilenameHeaderUrlEndingInSlash()
            throws HarvestException, IOException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("barnacles!")
                ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        Set<FileHarvest> result = httpHarvesterBean.listFiles(
                getHttpHarvesterConfig(wireMockHost + "/spongebob/squarepants/", null));
        assertThat("has squarepants harvest", result.contains(squarepantsNoHeaderFileHarvest),
                is(true));
        final FileHarvest fileHarvest = result.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("barnacles!"));
    }

    @Test
    public void test_harvest_nullPointer() throws HarvestException {
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            httpHarvesterBean.listFiles(
                    getHttpHarvesterConfig(null, null));
            fail("expected nullpointer exception");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void test_harvest_emptyUrl() throws HarvestException {
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            httpHarvesterBean.listFiles(
                    getHttpHarvesterConfig("", null));
            fail("expected illegalargument exception");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void test_harvest_noEntity() {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants"))
                .willReturn(aResponse().withStatus(200)));
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            httpHarvesterBean.listFiles(
                    getHttpHarvesterConfig(wireMockHost + "/spongebob/squarepants", null));
            fail("expected harvestexception");
        } catch (HarvestException ignored) {
        }
    }

    @Test
    public void test_harvest_errorCode() {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants"))
                .willReturn(aResponse().withStatus(404)));
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            httpHarvesterBean.listFiles(
                    getHttpHarvesterConfig(wireMockHost + "/spongebob/squarepants", null));
            fail("expected harvestexception");
        } catch (HarvestException e) {
        }
    }

    @Test
    public void test_harvest_urlFromPattern() throws HarvestException, IOException {
        final String targetUrl = String.format(
                "%s/viaf/data/viaf-20180701-clusters-marc21.iso.gz", wireMockHost);
        final String html = "<html><body>" +
                "<a href=\"http://viaf.org/viaf/data/viaf-20180701-clusters-" +
                "marc21.xml.gz\" resource=\"http://viaf.org/viaf/data/viaf-" +
                "20180701-clusters-marc21.xml.gz\">http://viaf.org/viaf/data" +
                "/viaf-20180701-clusters-marc21.xml.gz</a>" +
                "<a href=\"http://viaf.org/viaf/data/viaf-" +
                "20180701-clusters-marc21.iso.gz\" resource=\"http://viaf.org" +
                "/viaf/data/viaf-20180701-clusters-marc21.iso.gz\" " +
                String.format("rel=\"nofollow\">%s</a>", targetUrl) +
                "</body></html>";
        wireMockServer.stubFor(get(urlEqualTo("/patternpants")).willReturn(
                aResponse().withStatus(200).withBody(html)));
        wireMockServer.stubFor(get(urlEqualTo(
                "/viaf/data/viaf-20180701-clusters-marc21.iso.gz")).willReturn(
                aResponse().withStatus(200).withBody("viaf-data")));
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        final Set<FileHarvest> results = httpHarvesterBean.listFiles(
                getHttpHarvesterConfig(wireMockHost + "/patternpants",
                        String.format("%s/viaf*iso.gz", wireMockHost)));
        assertThat("results size", results.size(), is(1));
        final FileHarvest viafHarvest = new HttpFileHarvest(
                "viaf-20180701-clusters-marc21.iso.gz", null, targetUrl, null,
                FileHarvest.Status.AWAITING_DOWNLOAD, null);
        assertThat("contains viaf harvest", results.contains(viafHarvest),
                is(true));

        final FileHarvest fileHarvest = results.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("viaf-data"));
    }

    @Test
    public void test_harvest_proxy() throws HarvestException, IOException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Disposition",
                                "attachment; filename=\"squarepants.jpg\"")
                        .withBody("barnacles!")
                ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        httpHarvesterBean.proxyBean.withNonProxyHosts(Set.of());
        Set<FileHarvest> result = httpHarvesterBean.listFiles(
                getHttpHarvesterConfig(wireMockHost + "/spongebob/squarepants", null));
        assertThat("has squarepants harvest", result.contains(squarepantsFileHarvest),
                is(true));
        final FileHarvest fileHarvest = result.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("barnacles!"));
    }

    @Test
    public void test_findInContent() throws HarvestException {
        final String html = "<html><body>" +
                "<a href=\"http://viaf.org/viaf/data/viaf-20180701-clusters-" +
                "marc21.xml.gz\" resource=\"http://viaf.org/viaf/data/viaf-" +
                "20180701-clusters-marc21.xml.gz\">http://viaf.org/viaf/data" +
                "/viaf-20180701-clusters-marc21.xml.gz</a>" +
                "<a href=\"http://viaf.org/viaf/data/viaf-" +
                "20180701-clusters-marc21.iso.gz\" resource=\"http://viaf.org" +
                "/viaf/data/viaf-20180701-clusters-marc21.iso.gz\" " +
                "rel=\"nofollow\">http://viaf.org/viaf/data/viaf-20180701-" +
                "clusters-marc21.iso.gz</a>" +
                "</body></html>";
        wireMockServer.stubFor(get(urlEqualTo("/viaf")).willReturn(
                aResponse().withStatus(200).withBody(html)));
        final HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        final HttpListFilesHandler httpListFilesHandler =
                httpHarvesterBean.getHttpListFilesHandler(new HttpHarvesterConfig());
        final String result = httpListFilesHandler.findInContent(
                wireMockHost + "/viaf", "http://viaf.org*iso.gz");
        assertThat(result, is("http://viaf.org/viaf/data/viaf-" +
                "20180701-clusters-marc21.iso.gz"));
    }

    @Test
    public void test_findInContent_noMatches() {
        final String html = "<html><body><blah/></body></html>";
        wireMockServer.stubFor(get(urlEqualTo("/nothing")).willReturn(
                aResponse().withStatus(200).withBody(html)));
        final HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        final HttpListFilesHandler httpListFilesHandler =
                httpHarvesterBean.getHttpListFilesHandler(new HttpHarvesterConfig());
        try {
            final String result = httpListFilesHandler.findInContent(
                    wireMockHost + "/nothing", "no-match");
            fail(String.format("expected harvestexception. instead result " +
                    "\"%s\" was returned", result));
        } catch (HarvestException e) {
        }
    }

    @Test
    public void test_findInContent_emptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/empty")).willReturn(
                aResponse().withStatus(200)));
        final HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        final HttpListFilesHandler httpListFilesHandler =
                httpHarvesterBean.getHttpListFilesHandler(new HttpHarvesterConfig());
        try {
            final String result = httpListFilesHandler.findInContent(
                    wireMockHost + "/empty", "PatternPants");
            fail(String.format("expected harvestexception. instead result " +
                    "\"%s\" was returned", result));
        } catch (HarvestException e) {
        }
    }

    private static HTTPHarvesterBean getHTTPHarvesterBean() {
        HTTPHarvesterBean httpHarvesterBean = new HTTPHarvesterBean();
        httpHarvesterBean.proxyBean = new ProxyBean("localhost", mockProxy.getPort())
                .withNonProxyHosts(Set.of("localhost"));
        httpHarvesterBean.RETRY_POLICY = new RetryPolicy();
        return httpHarvesterBean;
    }

    private static HttpHarvesterConfig getHttpHarvesterConfig(String url, String urlPattern) {
        HttpHarvesterConfig config = new HttpHarvesterConfig();
        config.setUrl(url);
        config.setUrlPattern(urlPattern);
        return config;
    }

    private static HttpHarvesterConfig getHttpHarvesterConfigWithHeaders(String url, String urlPattern,
                                                                         List<CustomHttpHeader> headers) {
        HttpHarvesterConfig config = new HttpHarvesterConfig();
        config.setUrl(url);
        config.setUrlPattern(urlPattern);
        config.setHttpHeaders(headers);
        return config;
    }
}
