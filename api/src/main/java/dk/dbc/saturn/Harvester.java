package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.job.JobSenderBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Stateless
public abstract class Harvester<T extends AbstractHarvesterConfigEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Harvester.class);
    private static final RetryPolicy<?> RETRY_FULL_JOB = new RetryPolicy<>().withMaxRetries(12).withDelay(Duration.ofMinutes(15));
    private Tag TAG_OK = new Tag("status", "success");
    private Tag TAG_FAIL = new Tag("status", "failed");
    @EJB
    private HarvesterConfigRepository harvesterConfigRepository;
    @EJB
    private JobSenderBean jobSenderBean;
    @EJB
    private ProgressTrackerBean trackerBean;
    @Inject
    private RunScheduleBean runScheduleBean;
    @Inject
    private MetricRegistry metricRegistry;
    @Inject
    private RunningTasks runningTasks;


    protected Harvester() {
    }

    protected Harvester(HarvesterConfigRepository harvesterConfigRepository, JobSenderBean jobSenderBean, ProgressTrackerBean trackerBean, RunningTasks runningTasks) {
        this.harvesterConfigRepository = harvesterConfigRepository;
        this.jobSenderBean = jobSenderBean;
        this.trackerBean = trackerBean;
        this.runningTasks = runningTasks;
    }

    public void runHarvest(Class<T> clazz, int configId) {
        runHarvest(clazz, configId, false);
    }

    public void runHarvest(Class<T> clazz, int configId, boolean runNow) {
        runningTasks.run(configId, c -> {
            T config = harvesterConfigRepository.getHarvesterConfig(clazz, configId);
            if (!runNow && runScheduleBean.shouldSkip(config)) return;
            ProgressTrackerBean.Progress progress = trackerBean.add(config.getId());
            try {
                Failsafe.with(RETRY_FULL_JOB.copy()
                                .abortIf(e -> progress.isAbort())
                                .onFailedAttempt(e -> failedHarvestAttempt(e, config, progress)))
                        .run(() -> runHarvest(config, progress));
            } catch (FailsafeException e) {
                if(e.getCause() instanceof InterruptedException) {
                    LOGGER.info("Harvester {} was stopped by user", config.getId());
                } else {
                    metricRegistry.counter("harvests", TAG_FAIL, new Tag("id", Integer.toString(config.getId()))).inc();
                    if (progress != null) progress.failed("Failed");
                    LOGGER.error("Error while harvesting: {}", config.getId(), e);
                }
            }
        });
    }

    private void failedHarvestAttempt(ExecutionAttemptedEvent<?> e, T config, ProgressTrackerBean.Progress progress) {
        progress.setMessage("Failed, waiting for retry");
        LOGGER.warn("Harvesting {} failed, waiting for retry", config.getId(), e.getLastFailure());
    }

    private void runHarvest (T config, ProgressTrackerBean.Progress progress) throws HarvestException {
        LOGGER.info("Starting harvesting task: {}", config);
        progress.init(Set.of());
        Set<FileHarvest> fileHarvests = listFiles(config);
        if (!fileHarvests.isEmpty()) {
            progress.init(fileHarvests);
            harvest(config, fileHarvests);
            LOGGER.info("Done harvesting {}", config.getName());
            progress.done(config.getId(), metricRegistry);
            metricRegistry.counter("harvests", TAG_OK, new Tag("id", Integer.toString(config.getId()))).inc();
        } else {
            LOGGER.info("No files to harvest for {}", config.getName());
            progress.noFiles();
        }
        config.setLastHarvested(Date.from(Instant.now()));
    }

    public void harvest(T config, Set<FileHarvest> fileHarvests) throws HarvestException {
        try (HarvesterMDC mdc = new HarvesterMDC(config)) {
            LOGGER.info("Starting harvest of {}", config.getName());
            jobSenderBean.send(fileHarvests, config.getAgency(), config.getTransfile(), config.getId());
            config.setSeqno(fileHarvests.stream()
                    .map(FileHarvest::getSeqno)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Integer::valueOf))
                    .orElse(0));
            fileHarvests.forEach(FileHarvest::close);
        }
    }

    public abstract Set<FileHarvest> listFiles(T config) throws HarvestException;
}

