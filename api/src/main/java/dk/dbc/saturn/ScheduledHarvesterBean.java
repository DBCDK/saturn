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

import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.sql.Date;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Startup
@Singleton
@DependsOn("ProxyHandlerBean")
public class ScheduledHarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ScheduledHarvesterBean.class);

    final private HashMap<? super AbstractHarvesterConfigEntity,
        Future<Set<FileHarvest>>> harvestTasks = new HashMap<>();

    @EJB CronParserBean cronParserBean;
    @EJB HTTPHarvesterBean httpHarvesterBean;
    @EJB FtpHarvesterBean ftpHarvesterBean;
    @EJB HarvesterConfigRepository harvesterConfigRepository;
    @EJB FtpSenderBean ftpSenderBean;

    @Schedule(minute = "*", hour = "*", second = "*/5")
    public void harvest() {
        try {
            List<FtpHarvesterConfig> ftpResults = harvesterConfigRepository
                .list(FtpHarvesterConfig.class, 0, 0);
            List<HttpHarvesterConfig> httpResults = harvesterConfigRepository
                .list(HttpHarvesterConfig.class, 0, 0);
            LOGGER.info("got {} ftp configs, {} http configs", ftpResults.size(),
                httpResults.size());
            for(FtpHarvesterConfig ftpConfig : ftpResults) {
                if(harvestTasks.containsKey(ftpConfig)) {
                    LOGGER.info("still harvesting {}, not scheduling new " +
                        "harvest", ftpConfig.getName());
                    continue;
                }
                try {
                    FileNameMatcher fileNameMatcher = new FileNameMatcher(
                        ftpConfig.getFilesPattern());
                    if (cronParserBean.shouldExecute(ftpConfig.getSchedule(),
                        ftpConfig.getLastHarvested())) {
                        Future<Set<FileHarvest>> result =
                            ftpHarvesterBean.harvest(ftpConfig.getHost(),
                                ftpConfig.getPort(), ftpConfig.getUsername(),
                                ftpConfig.getPassword(), ftpConfig.getDir(),
                                fileNameMatcher, new SeqnoMatcher(ftpConfig));
                        harvestTasks.put(ftpConfig, result);
                    }
                } catch (HarvestException e) {
                    LOGGER.error("error while harvesting for ftp {}",
                        ftpConfig.getName(), e);
                }
            }
            for(HttpHarvesterConfig httpConfig : httpResults) {
                if(harvestTasks.containsKey(httpConfig)) {
                    LOGGER.info("still harvesting {}, not scheduling new " +
                        "harvest", httpConfig.getName());
                    continue;
                }
                try {
                    if(cronParserBean.shouldExecute(httpConfig.getSchedule(),
                        httpConfig.getLastHarvested())) {
                        if(httpConfig.getUrlPattern() == null ||
                                httpConfig.getUrlPattern().isEmpty()) {
                            Future<Set<FileHarvest>> result =
                                httpHarvesterBean.harvest(httpConfig.getUrl());
                            harvestTasks.put(httpConfig, result);
                        } else {
                            // look in response from url to get the real
                            // url for data harvesting
                            Future<Set<FileHarvest>> result =
                                httpHarvesterBean.harvest(httpConfig.getUrl(),
                                httpConfig.getUrlPattern());
                            harvestTasks.put(httpConfig, result);
                        }
                    }
                } catch (HarvestException e) {
                    LOGGER.error("error while harvesting for http {}",
                        httpConfig.getName(), e);
                }
            }
            sendResults();
        } catch (Exception e) {
            LOGGER.error("caught unexpected exception while harvesting", e);
        }
    }

    private void sendResults() {
        Iterator<? extends Map.Entry<? super AbstractHarvesterConfigEntity,
            Future<Set<FileHarvest>>>> iterator = harvestTasks
            .entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<? super AbstractHarvesterConfigEntity,
                Future<Set<FileHarvest>>> configEntry =
                iterator.next();
            final AbstractHarvesterConfigEntity config =
                (AbstractHarvesterConfigEntity) configEntry.getKey();
            try {
                Future<Set<FileHarvest>> result = configEntry.getValue();
                if(result.isDone()) {
                    iterator.remove();
                    final Set<FileHarvest> fileHarvests = result.get();
                    if(fileHarvests.isEmpty()) {
                        LOGGER.warn("no files harvested by {}", config.getName());
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
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.warn("harvest task for {} interrupted: {}",
                    config.getName(), e.getMessage());
            }
        }
    }
}
