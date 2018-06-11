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
import java.io.InputStream;
import java.io.InputStreamReader;

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
    public void test_harvest() throws HarvestException, IOException {
        wireMockServer.stubFor(get(urlEqualTo("/spongebob/squarepants/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("barnacles!")
            ));

        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();
        InputStream is = httpHarvesterBean.harvest(wireMockHost +
            "/spongebob/squarepants");
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
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

    private static HTTPHarvesterBean getHTTPHarvesterBean() {
        HTTPHarvesterBean httpHarvesterBean = new HTTPHarvesterBean();
        httpHarvesterBean.RETRY_POLICY = new RetryPolicy();
        return httpHarvesterBean;
    }
}
