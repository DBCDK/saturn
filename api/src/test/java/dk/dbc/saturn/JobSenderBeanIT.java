package dk.dbc.saturn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Body;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.commons.testcontainers.service.DBCServiceContainer;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.ftp.ByteCountingFailInputStream;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.saturn.entity.CustomHttpHeader;
import dk.dbc.saturn.job.JobSenderBean;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.Mockito.mock;

public class JobSenderBeanIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSenderBeanIT.class);
    private static final int LARGE_NUMBER = 50000;
    private final byte[] LARGE_BLOB = createLargeBlob();
    private static final String URL_PATH = "/largeslowdownload/data/some-data-here.bin";

    private final WireMockServer wiremock = makeWiremock();
    private final DBCPostgreSQLContainer db = makeDBContainer();
    private final DBCServiceContainer fileStore = makeFilestore();
    private final FileStoreServiceConnector fsConnector = makeFilestoreConnector(fileStore);

    @Test
    public void sendToFileStoreTest() throws FileStoreServiceConnectorException, IOException {
        JobSenderBean jobSenderBean = makeJobSenderBean(fsConnector, 2);
        String fsId = jobSenderBean.sendToFileStore(makeHarvest("largeslowdownload", Long.MAX_VALUE));
        byte[] result = fsConnector.getFile(fsId).readAllBytes();
        Assert.assertArrayEquals(LARGE_BLOB, result);
    }

    @Test(expected = ResponseProcessingException.class)
    public void sendToFileStoreFailTest() throws FileStoreServiceConnectorException, IOException {
        JobSenderBean jobSenderBean = makeJobSenderBean(fsConnector, 2);
        jobSenderBean.sendToFileStore(makeHarvest("largeslowdownload", LARGE_BLOB.length / 2));
    }

    @Test
    public void resumeOfLargeDownloadTest() throws FileStoreServiceConnectorException, IOException {
        JobSenderBean jobSenderBean = makeJobSenderBean(fsConnector, 2);
        String fsId = jobSenderBean.sendToFileStoreResume(makeResumingHarvest("largeslowdownload", LARGE_BLOB.length / 2));
        byte[] result = fsConnector.getFile(fsId).readAllBytes();
        Assert.assertArrayEquals(LARGE_BLOB, result);
    }

    @Test(expected = ResponseProcessingException.class)
    public void resumeTooManyRetriesTest() throws FileStoreServiceConnectorException, IOException {
        JobSenderBean jobSenderBean = makeJobSenderBean(fsConnector, 2);
        jobSenderBean.sendToFileStoreResume(makeResumingHarvest("largeslowdownload", LARGE_BLOB.length / 5));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void noResumeAvailable() throws HarvestException, FileStoreServiceConnectorException {
        JobSenderBean jobSenderBean = makeJobSenderBean(fsConnector, 2);
        jobSenderBean.sendToFileStoreResume(makeHarvest("test-upload-with-errors", Long.MAX_VALUE));
    }


    private JobSenderBean makeJobSenderBean(FileStoreServiceConnector fsConnector, int retries) {
        return new JobSenderBean(new ProgressTrackerBean(), fsConnector, mock(JobStoreServiceConnectorBean.class), retries);
    }

    private FileHarvest makeResumingHarvest(String name, long failAt) {
        return new HttpFileHarvest(name, ClientBuilder.newClient(), wiremock.baseUrl() + URL_PATH, 0,
                FileHarvest.Status.AWAITING_DOWNLOAD, List.of(new CustomHttpHeader().withKey("Range").withValue("bytes=0-"))) {
            @Override
            public InputStream getContent() throws HarvestException {
                return new ByteCountingFailInputStream(super.getContent(), failAt);
            }
        };
    }

    private FileHarvest makeHarvest(String name, long failAt) {
        return new HttpFileHarvest(name, ClientBuilder.newClient(), wiremock.baseUrl() + URL_PATH, 0,
                FileHarvest.Status.AWAITING_DOWNLOAD, List.of()) {
            @Override
            public InputStream getContent() throws HarvestException {
                return new ByteCountingFailInputStream(super.getContent(), failAt);
            }
        };
    }

    private boolean captureRange(AtomicInteger range, Request request) {
        String header = request.getHeader("Range");
        if(header == null) return false;
        Matcher matcher = Pattern.compile("bytes=(\\d+)-").matcher(header);
        if(!matcher.matches()) return false;
        range.set(Integer.parseInt(matcher.group(1)));
        return true;
    }

    private void makeStubs(WireMockServer wireMockServer) {
        AtomicInteger progress = new AtomicInteger();
        StubMapping stubMapping = wireMockServer.stubFor(requestMatching(request ->
                MatchResult.of(request.getUrl().contains(URL_PATH) && captureRange(progress, request)))
                .willReturn(aResponse()
                        .withStatus(Response.Status.PARTIAL_CONTENT.getStatusCode())
                        .withHeader("Content-Disposition", "filename=\"some-data-here.bin\"")
                        .withResponseBody(new Body(new byte[0]) {
                            @Override
                            public byte[] asBytes() {
                                return getPartOfBlob(progress.get());
                            }
                        })
                ));
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

    private WireMockServer makeWiremock() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        configureFor("localhost", server.port());
        makeStubs(server);
        return server;
    }

    private FileStoreServiceConnector makeFilestoreConnector(DBCServiceContainer fs) {
        RetryPolicy<Response> policy = new RetryPolicy<Response>().withMaxRetries(1).withDelay(Duration.ofMillis(1)).handle(Exception.class);
        FailSafeHttpClient client = FailSafeHttpClient.create(fs.getHttpClient().getClient(), policy);
        return new FileStoreServiceConnector(client, fs.getServiceBaseUrl() + "/dataio/file-store-service");
    }

    private DBCServiceContainer makeFilestore() {
        String image = "docker-metascrum.artifacts.dbccloud.dk/dataio-file-store-service:devel";
        DBCServiceContainer container = new DBCServiceContainer(image)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "1G")
                .withEnv("FILESTORE_DB_URL", db.getPayaraDockerJdbcUrl())
                .withEnv("BFS_ROOT", "/tmp/filestore")
                .withEnv("HZ_CLUSTER_NAME", "dataio-filestore-cluster")
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/dataio/file-store-service/status"))
                .withStartupTimeout(Duration.ofMinutes(2));
        container.start();
        return container;
    }

    private static DBCPostgreSQLContainer makeDBContainer() {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withReuse(false);
        container.start();
        container.exposeHostPort();
        LOGGER.info("Postgres url is:{}", container.getDockerJdbcUrl());
        return container;
    }

    protected byte[] createLargeBlob() {
        byte[] bytes = new byte[LARGE_NUMBER];
        new Random().nextBytes(bytes);
        return bytes;
    }

    protected byte[] getPartOfBlob(int start) {
        return Arrays.copyOfRange(LARGE_BLOB, start, LARGE_BLOB.length);
    }
}
