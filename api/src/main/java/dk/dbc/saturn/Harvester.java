package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.job.JobSenderBean;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
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
    @EJB
    private HarvesterConfigRepository harvesterConfigRepository;
    @EJB
    private JobSenderBean jobSenderBean;
    @EJB
    private RunningTasks runningTasks;
    @Resource
    private SessionContext context;

    protected Harvester() {
    }

    public Harvester(HarvesterConfigRepository harvesterConfigRepository, JobSenderBean jobSenderBean, RunningTasks runningTasks, SessionContext context) {
        this.harvesterConfigRepository = harvesterConfigRepository;
        this.jobSenderBean = jobSenderBean;
        this.runningTasks = runningTasks;
        this.context = context;
    }

    @Asynchronous
    public void doHarvest(T config) {
        runHarvest(config);
    }

    private void runHarvest(T config) {
        try {
            runningTasks.run(config, () -> {
                Set<FileHarvest> fileHarvests = self().listFiles(config);
                if (!fileHarvests.isEmpty()) {
                    ProgressTrackerBean.Key progressKey = new ProgressTrackerBean.Key(FtpHarvesterConfig.class, config.getId());
                    harvest(config, progressKey);
                    LOGGER.info("Done scheduling {}", config.getName());
                }
            });
        } catch (HarvestException e) {
            LOGGER.error("Error while harvesting: {}", config, e);
        }
    }

    public void harvest(T config, ProgressTrackerBean.Key progressKey) throws HarvestException {
        try (HarvesterMDC mdc = new HarvesterMDC(config)) {
            LOGGER.info("Starting harvest of {}", config.getName());
            Set<FileHarvest> fileHarvests = listFiles( config );
            jobSenderBean.send(fileHarvests, config.getAgency(), config.getTransfile(), progressKey);
            config.setLastHarvested(Date.from(Instant.now()));
            config.setSeqno(fileHarvests.stream()
                    .map(FileHarvest::getSeqno)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Integer::valueOf))
                    .orElse(0));
            fileHarvests.forEach(FileHarvest::close);
            harvesterConfigRepository.save((Class<T>)config.getClass(), config);
            LOGGER.info("Ended harvest of {}", config.getName());
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public abstract Set<FileHarvest> listFiles(T config) throws HarvestException;

    protected Harvester<T> self() {
        //noinspection unchecked
        return context.getBusinessObject(getClass());
    }
}

