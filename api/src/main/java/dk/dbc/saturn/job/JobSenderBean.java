/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.job;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.transfile.JobSpecificationFactory;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.saturn.FileHarvest;
import dk.dbc.saturn.HarvestException;
import dk.dbc.saturn.ProgressTrackerBean;
import dk.dbc.util.Stopwatch;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@LocalBean
@Stateless
public class JobSenderBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSenderBean.class);
    private final RetryPolicy<?> retryPolicy;
    @Inject
    private ProgressTrackerBean progressTrackerBean;
    private FileStoreServiceConnector fileStore;
    @Inject
    private JobStoreServiceConnectorBean jobStore;

    private static final String APPLICATION_ID = "saturn";


    public JobSenderBean() {
        retryPolicy = new RetryPolicy<>().withMaxRetries(5).withDelay(Duration.ofMinutes(1)).handle(Exception.class);
        ClientConfig config = new ClientConfig().register(new JacksonFeature());
        FailSafeHttpClient client = FailSafeHttpClient.create(HttpClient.newClient(config), new RetryPolicy<Response>().withMaxRetries(0));
        fileStore = new FileStoreServiceConnector(client, System.getenv("FILESTORE_URL"));
    }

    public JobSenderBean(ProgressTrackerBean progressTrackerBean, FileStoreServiceConnector fileStore, JobStoreServiceConnectorBean jobStore, int retries) {
        this.progressTrackerBean = progressTrackerBean;
        this.fileStore = fileStore;
        this.jobStore = jobStore;
        this.retryPolicy = new RetryPolicy<>().withMaxRetries(retries).withDelay(Duration.ofMillis(1)).handle(Exception.class);
    }

    /**
     * send files to filestore and create the job in jobstore
     * @param files map of filenames and corresponding input streams
     * @param filenamePrefix prefix for data files and transfile
     * @param transfileTemplate transfile content template
     * @param progressKey file x out of y
     */
    public void send(Set<FileHarvest> files, String filenamePrefix, String transfileTemplate,
                     ProgressTrackerBean.Key progressKey) throws HarvestException {
        final Stopwatch stopwatch = new Stopwatch();
        try {
            progressTrackerBean.init(progressKey, files.size());

            String transfileName = String.format("%s.%s.trans", filenamePrefix, APPLICATION_ID);
            for (FileHarvest fileHarvest : files) {
                createJob(transfileName, fileHarvest, transfileTemplate);
                progressTrackerBean.get(progressKey).inc();
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
        } catch (FileStoreServiceConnectorException | JobStoreServiceConnectorException | RuntimeException e) {
            throw new HarvestException("Failed to create job for harvest " + fileHarvest, e);
        }
    }

    public String sendToFileStore(FileHarvest fileHarvest) {
        AtomicReference<String> ref = new AtomicReference<>();
        Failsafe.with(retryPolicy).run(() -> {
            ref.set(fileStore.addFile(fileHarvest.getContent()));
        });
        return ref.get();
    }

    public String sendToFileStoreResume(FileHarvest fileHarvest) throws FileStoreServiceConnectorException {
        String fileStoreId = fileStore.addFile(new ByteArrayInputStream(new byte[0]));
        Failsafe.with(retryPolicy).run(() -> {
            long size = fileStore.getByteSize(fileStoreId);
            fileHarvest.setResumePoint(size);
            fileStore.appendStream(fileStoreId, fileHarvest.getContent());
        });
        return fileStoreId;
    }
}
