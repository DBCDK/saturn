/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.job;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.transfile.JobSpecificationFactory;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.saturn.ByteCountingInputStream;
import dk.dbc.saturn.FileHarvest;
import dk.dbc.saturn.HarvestException;
import dk.dbc.saturn.ProgressTrackerBean;
import dk.dbc.util.Stopwatch;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@LocalBean
@Stateless
public class JobSenderBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSenderBean.class);
    private static final int MAX_HTTP_CONNECTIONS = 100;
    private final RetryPolicy<?> retryPolicy;
    @Inject
    private ProgressTrackerBean progressTrackerBean;
    private FileStoreServiceConnector fileStore;
    @Inject
    private JobStoreServiceConnectorBean jobStore;

    private static final String APPLICATION_ID = "saturn";


    public JobSenderBean() {
        retryPolicy = new RetryPolicy<>().withMaxRetries(5).withDelay(Duration.ofMinutes(1));
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(MAX_HTTP_CONNECTIONS);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(MAX_HTTP_CONNECTIONS);
        ClientConfig config = new ClientConfig()
                .register(new JacksonFeature())
                .property(ClientProperties.READ_TIMEOUT, Duration.ofHours(1).toMillis())
                .property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024)
                .property(ApacheClientProperties.CONNECTION_MANAGER, poolingHttpClientConnectionManager)
                .connectorProvider(new ApacheConnectorProvider());
        Client client = HttpClient.newClient(config);
        fileStore = new FileStoreServiceConnector(client, System.getenv("FILESTORE_URL"));
    }

    public JobSenderBean(ProgressTrackerBean progressTrackerBean, FileStoreServiceConnector fileStore, JobStoreServiceConnectorBean jobStore, int retries) {
        this.progressTrackerBean = progressTrackerBean;
        this.fileStore = fileStore;
        this.jobStore = jobStore;
        retryPolicy = new RetryPolicy<>().withMaxRetries(retries).withDelay(Duration.ofMillis(1));
    }

    /**
     * send files to filestore and create the job in jobstore
     * @param files map of filenames and corresponding input streams
     * @param filenamePrefix prefix for data files and transfile
     * @param transfileTemplate transfile content template
     */
    public void send(Set<FileHarvest> files, String filenamePrefix, String transfileTemplate, Integer configId) throws HarvestException {
        final Stopwatch stopwatch = new Stopwatch();
        try {
            String transfileName = String.format("%s.%s.trans", filenamePrefix, APPLICATION_ID);
            ProgressTrackerBean.Progress progress = progressTrackerBean.get(configId);
            long totalBytes = files.stream().map(FileHarvest::getSize).filter(Objects::nonNull).mapToLong(Number::longValue).sum();
            progress.setTotalBytes(totalBytes);
            for (FileHarvest fileHarvest : files) {
                createJob(transfileName, fileHarvest, transfileTemplate);
                progress.inc();
            }
        } finally {
            LOGGER.info("send took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        }
    }

    private int createJob(String transfileName, FileHarvest fileHarvest, String template) throws HarvestException {
        try {
            Map<Character, String> templateMap = JobSpecificationFactory.transfileLineToMap(template);
            templateMap.put('f', fileHarvest.getFilename());
            String fileStoreId = fileHarvest.isResumable() ? sendToFileStoreResume(fileHarvest) : sendToFileStore(fileHarvest);
            LOGGER.info("Added file {} to file store with id {}", fileHarvest.getFilename(), fileStoreId);
            JobSpecification specification = JobSpecificationFactory.createJobSpecification(templateMap, transfileName, fileStoreId, template.getBytes(StandardCharsets.UTF_8));
            JobInfoSnapshot job = jobStore.getConnector().addJob(new JobInputStream(specification, true, 0));
            LOGGER.info("Added job {} to job store", job.getJobId());
            return job.getJobId();
        } catch (Exception e) {
            throw new HarvestException("Failed to create job for harvest " + fileHarvest, e);
        }
    }

    public String sendToFileStore(FileHarvest fileHarvest) throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        Failsafe.with(retryPolicy).run(() -> {
            try(InputStream is = fileHarvest.getContent()) {
                LOGGER.info("Sending file {} to filestore with size {}", fileHarvest.getFilename(), FileUtils.byteCountToDisplaySize(fileHarvest.getSize()));
                ref.set(fileStore.addFile(is));
            }
        });
        return ref.get();
    }

    public String sendToFileStoreResume(FileHarvest fileHarvest) throws Exception {
        String fileStoreId = fileStore.addFile(new ByteArrayInputStream(new byte[0]));
        Failsafe.with(retryPolicy).run(() -> {
            long size = fileStore.getByteSize(fileStoreId);
            LOGGER.info("Sending resumable file {} to filestore resume at {}", size, fileHarvest.getFilename());
            fileHarvest.setResumePoint(size);
            try(ByteCountingInputStream content = fileHarvest.getContent().setCount(size)) {
                fileStore.appendStream(fileStoreId, content);
            } catch (Exception e) {
                LOGGER.warn("Failed to send file {}", fileHarvest.getFilename(), e);
                throw e;
            }
        });
        return fileStoreId;
    }
}
