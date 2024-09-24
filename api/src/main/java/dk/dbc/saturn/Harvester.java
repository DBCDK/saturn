package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.job.JobSenderBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Stateless
public abstract class Harvester<T extends AbstractHarvesterConfigEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Harvester.class);
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
        runningTasks.run(configId, c -> {
            ProgressTrackerBean.Progress progress = null;
            try {
                T config = harvesterConfigRepository.getHarvesterConfig(clazz, configId);
                if (runScheduleBean.shouldSkip(config)) return;
                progress = trackerBean.add(config.getId());
                LOGGER.info("Starting harvesting task: {}", config);
                Set<FileHarvest> fileHarvests = listFiles(config);
                if (!fileHarvests.isEmpty()) {
                    progress.init(fileHarvests);
                    harvest(config, fileHarvests);
                    LOGGER.info("Done harvesting {}", config.getName());
                    progress.done(configId, metricRegistry);
                    metricRegistry.counter("harvests", TAG_OK, new Tag("id", Integer.toString(configId))).inc();
                } else {
                    LOGGER.info("No files to harvest for {}", config.getName());
                    progress.setMessage("no files");
                }
                config.setLastHarvested(Date.from(Instant.now()));
            } catch (HarvestException | RuntimeException e) {
                metricRegistry.counter("harvests", TAG_FAIL, new Tag("id", Integer.toString(configId))).inc();
                if(progress != null)  progress.setMessage("failed");
                LOGGER.error("Error while harvesting: {}", configId, e);
            }
        });
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

