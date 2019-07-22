/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Startup
@Singleton
@DependsOn("ProxyHandlerBean")
public class ScheduledHarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ScheduledHarvesterBean.class);

    final HashMap<Integer,
        Future<Set<FileHarvest>>> harvestTasks = new HashMap<>();

    @Inject RunScheduleFactory runScheduleFactory;
    @EJB HTTPHarvesterBean httpHarvesterBean;
    @EJB FtpHarvesterBean ftpHarvesterBean;
    @EJB HarvesterConfigRepository harvesterConfigRepository;
    @EJB FtpSenderBean ftpSenderBean;

    @PostConstruct
    public void init() {
        // For some reason we need to touch the MDC context initially
        // to get any MDC logging at all???
        MDC.clear();
    }

    @Schedule(minute = "*", hour = "*", second = "*/20")
    public void harvest() {
        try {
            scheduleFtpHarvests();
            scheduleHttpHarvests();
            sendResults();
        } catch (Exception e) {
            LOGGER.error("caught unexpected exception while harvesting", e);
        }
    }

    private void scheduleFtpHarvests() {
        final List<FtpHarvesterConfig> ftpConfigs = harvesterConfigRepository
                .list(FtpHarvesterConfig.class, 0, 0);

        LOGGER.info("got {} FTP configs", ftpConfigs.size());
        for (FtpHarvesterConfig ftpConfig : ftpConfigs) {
            scheduleFtpHarvest(ftpConfig);
        }
    }

    private void scheduleFtpHarvest(FtpHarvesterConfig ftpConfig) {
        try (HarvesterMDC mdc = new HarvesterMDC(ftpConfig)) {
            if (harvestTasks.containsKey(ftpConfig.getId())) {
                LOGGER.debug("still harvesting, not rescheduled");
                return;
            }
            try {
                if (ftpConfig.isEnabled()
                        && runScheduleFactory.newRunScheduleFrom(ftpConfig.getSchedule())
                                .isSatisfiedBy(new Date(), ftpConfig.getLastHarvested())) {
                    harvestTasks.put(ftpConfig.getId(),
                            ftpHarvesterBean.harvest(ftpConfig));
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("error while scheduling harvest", e);
            }
        }
    }

    private void scheduleHttpHarvests() {
        final List<HttpHarvesterConfig> httpConfigs = harvesterConfigRepository
                .list(HttpHarvesterConfig.class, 0, 0);

        LOGGER.info("got {} HTTP configs", httpConfigs.size());
        for (HttpHarvesterConfig httpConfig : httpConfigs) {
            scheduleHttpHarvest(httpConfig);
        }
    }

    private void scheduleHttpHarvest(HttpHarvesterConfig httpConfig) {
        try (HarvesterMDC mdc = new HarvesterMDC(httpConfig)) {
            if (harvestTasks.keySet().contains(httpConfig.getId())) {
                LOGGER.debug("still harvesting, not rescheduled");
                return;
            }
            try {
                if (httpConfig.isEnabled()
                        && runScheduleFactory.newRunScheduleFrom(httpConfig.getSchedule())
                                .isSatisfiedBy(new Date(), httpConfig.getLastHarvested())) {
                       harvestTasks.put(httpConfig.getId(),
                                httpHarvesterBean.harvest(httpConfig));
                }
            } catch (HarvestException e) {
                LOGGER.error("error while scheduling harvest", e);
            }
        }
    }

    private void sendResults() {
        final Iterator<Map.Entry<Integer, Future<Set<FileHarvest>>>>
                iterator = harvestTasks.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Integer, Future<Set<FileHarvest>>> configEntry =
                    iterator.next();
            final int id = configEntry.getKey();
            final Optional<Class> type = harvesterConfigRepository.getHarvesterConfigType(id);
            if (!type.isPresent()) {
                // this should never happen
                LOGGER.error("unable to find type for config with id {}", id);
                iterator.remove();
                continue;
            }
            final AbstractHarvesterConfigEntity config = harvesterConfigRepository
                .find(type.get(), id);
            try (HarvesterMDC mdc = new HarvesterMDC(config)) {
                try {
                    final Future<Set<FileHarvest>> result = configEntry.getValue();
                    if (result.isDone()) {
                        iterator.remove();
                        final Set<FileHarvest> fileHarvests = result.get();
                        if (fileHarvests.isEmpty()) {
                            LOGGER.warn("no files harvested");
                            continue;
                        }
                        ftpSenderBean.send(fileHarvests, config.getAgency(), config.getTransfile());
                        config.setLastHarvested(Date.from(Instant.now()));
                        config.setSeqno(fileHarvests.stream()
                                .map(FileHarvest::getSeqno)
                                .filter(Objects::nonNull)
                                .max(Comparator.comparing(Integer::valueOf))
                                .orElse(0));

                        if (config instanceof HttpHarvesterConfig) {
                            harvesterConfigRepository.save(HttpHarvesterConfig.class,
                                    (HttpHarvesterConfig) config);
                        } else {
                            harvesterConfigRepository.save(FtpHarvesterConfig.class,
                                    (FtpHarvesterConfig) config);
                        }
                    }
                } catch (InterruptedException | ExecutionException | HarvestException e) {
                    LOGGER.error("harvest task not completed: {}", e.getMessage(), e);
                }
            }
        }
    }
}
