package dk.dbc.saturn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import dk.dbc.ftp.FTPTestClient;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.CustomHttpHeader;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndProxy;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FtpSenderFailSafeTest extends AbstractFtpBeanTest{
    private static ClientAndProxy mockProxy;
    private static final String URL_PATH = "/largeslowdownload/data/some-data-here.bin";

    @BeforeAll
    public static void setup() throws IOException {
        final ServerSocket socket = new ServerSocket(0);
        final int proxyPort = socket.getLocalPort();
        socket.close();
        mockProxy = ClientAndProxy.startClientAndProxy(proxyPort);
        makeStubs(wireMockServer);
    }


    @Test
    public void resumeOfLargeDownloadTest() throws HarvestException, IOException {
        HttpHarvesterConfig config = getHttpHarvesterConfig();
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();

        FTPTestClient ftpTestClient = (FTPTestClient) new FTPTestClient()
                .withHost("localhost")
                .withPort(fakeFtpServer.getServerControlPort())
                .withPassword(PASSWORD)
                .withUsername(USERNAME);
        ftpTestClient.setFakeFtpUploadError(true);
        Set<FileHarvest> fileHarvests = httpHarvesterBean.listFiles(config);
        httpHarvesterBean.ftpSenderBean.upload(false, ftpTestClient, fileHarvests.stream().findFirst().orElse(null),
                "test-upload-with-errors", true);
        assertThat("Same bytes", readInputStream(ftpTestClient.get("test-upload-with-errors")), is(new String(LARGE_BLOB)));
    }

    @Test
    public void noResumeAvailable() throws HarvestException {
        HttpHarvesterConfig config = getHttpHarvesterConfig();
        config.setHttpHeaders(List.of());
        HTTPHarvesterBean httpHarvesterBean = getHTTPHarvesterBean();

        FTPTestClient ftpTestClient = (FTPTestClient) new FTPTestClient()
                .withHost("localhost")
                .withPort(fakeFtpServer.getServerControlPort())
                .withPassword(PASSWORD)
                .withUsername(USERNAME);
        ftpTestClient.setFakeFtpUploadError(true);
        Set<FileHarvest> fileHarvests = httpHarvesterBean.listFiles(config);
        assertThrows(HarvestException.class, () ->
                httpHarvesterBean.ftpSenderBean.upload(false, ftpTestClient, fileHarvests.stream().findFirst().orElse(null),
                        "test-upload-with-errors", true));
    }

    private static HTTPHarvesterBean getHTTPHarvesterBean() {
        HTTPHarvesterBean httpHarvesterBean = new HTTPHarvesterBean();
        httpHarvesterBean.ftpSenderBean = new FtpSenderBean();
        httpHarvesterBean.ftpSenderBean.dir = "put";
        httpHarvesterBean.proxyBean = new ProxyBean("localhost", mockProxy.getPort())
                .withNonProxyHosts(Set.of("localhost"));
        httpHarvesterBean.harvesterConfigRepository = mock(HarvesterConfigRepository.class);
        when(httpHarvesterBean.harvesterConfigRepository.list(eq(HttpHarvesterConfig.class), anyInt(), anyInt())).thenReturn(List.of(getHttpHarvesterConfig()));

        HTTPHarvesterBean.RETRY_POLICY = new RetryPolicy<>();
        httpHarvesterBean.runningTasks = new RunningTasks();
        return httpHarvesterBean;
    }

    private static HttpHarvesterConfig getHttpHarvesterConfig() {
        HttpHarvesterConfig httpHarvesterConfig =  new HttpHarvesterConfig();
        httpHarvesterConfig.setHttpHeaders(List.of(new CustomHttpHeader().withKey("Range").withValue("bytes=0-")));
        httpHarvesterConfig.setUrl(wireMockHost+ URL_PATH);
        httpHarvesterConfig.setName("largeslowdownload");
        return httpHarvesterConfig;
    }

    private static void makeStubs(WireMockServer wireMockServer) {
        List.of(0, 16384, 32768, 49152, 65536, 81920, 98304).forEach(progress -> wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains(URL_PATH) &&
                                request.getHeader("Range") != null &&
                                String.format("bytes=%d-", progress).equals(request.getHeader("Range"))
                ))
                .willReturn(aResponse()
                        .withStatus(Response.Status.PARTIAL_CONTENT.getStatusCode())
                        .withHeader("Content-Disposition", "filename=\"some-data-here.bin\"")
                        .withBody(getPartOfBlob(progress))
                )));
        wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(
                        request.getUrl().contains(URL_PATH) &&
                                request.getHeader("Range") == null
                ))
                .willReturn(aResponse()
                        .withStatus(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Disposition", "filename=\"some-data-here.bin\"")
                        .withBody(getPartOfBlob(0))
                ));
    }
}
