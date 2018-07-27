/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class HTTPHarvesterBeanTest {

    private static WireMockServer wireMockServer;
    private static String wireMockHost;

    private final FileHarvest squarepantsFileHarvest =
            new FileHarvest("squarepants.jpg", null, null);
    private final FileHarvest squarepantsNoHeaderFileHarvest =
            new FileHarvest("squarepants", null, null);

    @BeforeAll
    public static void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort()
            .dynamicHttpsPort());
        wireMockServer.start();
        wireMockHost = "http://localhost:" + wireMockServer.port();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    public static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void test_harvest() throws HarvestException, IOException,
            ExecutionException, InterruptedException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Disposition",
                    "attachment; filename=\"squarepants.jpg\"")
                .withBody("barnacles!")
            ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        Set<FileHarvest> result = httpHarvesterBean.harvest(wireMockHost +
            "/spongebob/squarepants").get();
        assertThat("has squarepants harvest", result.contains(squarepantsFileHarvest),
            is(true));
        final FileHarvest fileHarvest = result.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("barnacles!"));
    }

    @Test
    public void test_harvest_noFilenameHeader() throws HarvestException,
            IOException, ExecutionException, InterruptedException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("barnacles!")
            ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        Set<FileHarvest> result = httpHarvesterBean.harvest(wireMockHost +
            "/spongebob/squarepants").get();
        assertThat("has squarepants harvest", result.contains(squarepantsNoHeaderFileHarvest),
            is(true));
        final FileHarvest fileHarvest = result.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("barnacles!"));
    }

    @Test
    public void test_harvest_noFilenameHeaderUrlEndingInSlash() throws
            HarvestException, IOException, ExecutionException, InterruptedException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("barnacles!")
            ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        Set<FileHarvest> result = httpHarvesterBean.harvest(wireMockHost +
            "/spongebob/squarepants/").get();
        assertThat("has squarepants harvest", result.contains(squarepantsNoHeaderFileHarvest),
            is(true));
        final FileHarvest fileHarvest = result.iterator().next();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            fileHarvest.getContent()));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = in.readLine()) != null) {
            sb.append(line);
        }
        assertThat(sb.toString(), is("barnacles!"));
    }

    @Test
    public void test_harvest_nullPointer() throws HarvestException {
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            httpHarvesterBean.harvest(null);
            fail("expected nullpointer exception");
        } catch(NullPointerException e) {}
    }

    @Test
    public void test_harvest_emptyUrl() throws HarvestException {
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            httpHarvesterBean.harvest("");
            fail("expected illegalargument exception");
        } catch(IllegalArgumentException e) {}
    }

    @Test
    public void test_harvest_noEntity() {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants/"))
            .willReturn(aResponse().withStatus(200)));
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            httpHarvesterBean.harvest(wireMockHost + "/spongebob/squarepants");
            fail("expected harvestexception");
        } catch(HarvestException e) {}
    }

    @Test
    public void test_harvest_errorCode() {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants/"))
            .willReturn(aResponse().withStatus(404)));
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            httpHarvesterBean.harvest(wireMockHost + "/spongebob/squarepants");
            fail("expected harvestexception");
        } catch(HarvestException e) {}
    }

    @Test
    void test_findInContent() throws HarvestException {
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
        wireMockServer.stubFor(get(urlEqualTo("/viaf/")).willReturn(
            aResponse().withStatus(200).withBody(html)));
        final HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        final String result = httpHarvesterBean.findInContent(
            wireMockHost + "/viaf", "http://viaf.org*iso.gz");
        assertThat(result, is("http://viaf.org/viaf/data/viaf-" +
            "20180701-clusters-marc21.iso.gz"));
    }

    @Test
    void test_findInContent_noMatches() {
        final String html = "<html><body><blah/></body></html>";
        wireMockServer.stubFor(get(urlEqualTo("/nothing/")).willReturn(
            aResponse().withStatus(200).withBody(html)));
        final HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            final String result = httpHarvesterBean.findInContent(
                wireMockHost + "/nothing", "no-match");
            fail(String.format("expected harvestexception. instead result " +
                "\"%s\" was returned", result));
        } catch (HarvestException e) {}
    }

    @Test
    void test_findInContent_emptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/empty/")).willReturn(
            aResponse().withStatus(200)));
        final HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        try {
            final String result = httpHarvesterBean.findInContent(
                wireMockHost + "/empty", "PatternPants");
            fail(String.format("expected harvestexception. instead result " +
                "\"%s\" was returned", result));
        } catch (HarvestException e) {}
    }

    private static HTTPHarvesterBean getHTTPHarvesterBean() {
        HTTPHarvesterBean httpHarvesterBean = new HTTPHarvesterBean();
        httpHarvesterBean.RETRY_POLICY = new RetryPolicy();
        return httpHarvesterBean;
    }
}
