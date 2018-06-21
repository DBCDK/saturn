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

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.InputStream;
import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Startup
@Singleton
public class ScheduledHarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ScheduledHarvesterBean.class);

    final private HashMap<? super AbstractHarvesterConfigEntity,
        Future<Map<String, InputStream>>> harvestTasks = new HashMap<>();

    @EJB CronParserBean cronParserBean;
    @EJB HTTPHarvesterBean httpHarvesterBean;
    @EJB FtpHarvesterBean ftpHarvesterBean;
    @EJB HarvesterConfigRepository harvesterConfigRepository;
    @EJB FtpSenderBean ftpSenderBean;

    @Schedule(minute = "*", hour = "*")
    public void harvest() {
        List<FtpHarvesterConfig> ftpResults = harvesterConfigRepository
            .list(FtpHarvesterConfig.class, 0, 0);
        List<HttpHarvesterConfig> httpResults = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);
        LOGGER.info("got {} ftp configs, {} http configs", ftpResults.size(),
            httpResults.size());
        for(FtpHarvesterConfig ftpConfig : ftpResults) {
                try {
                    FileNameMatcher fileNameMatcher = new FileNameMatcher(
                        ftpConfig.getFilesPattern());
                    if (cronParserBean.shouldExecute(ftpConfig.getSchedule(),
                        ftpConfig.getLastHarvested())) {
                        Future<Map<String, InputStream>> result =
                            ftpHarvesterBean.harvest(ftpConfig.getHost(),
                            ftpConfig.getPort(), ftpConfig.getUsername(),
                            ftpConfig.getPassword(), ftpConfig.getDir(),
                            fileNameMatcher);
                        harvestTasks.put(ftpConfig, result);
                    }
                } catch (HarvestException e) {
                    LOGGER.error("error while harvesting for ftp {}",
                        ftpConfig.getName(), e);
                }
        }
        for(HttpHarvesterConfig httpConfig : httpResults) {
            try {
                if(cronParserBean.shouldExecute(httpConfig.getSchedule(),
                        httpConfig.getLastHarvested())) {
                    Future<Map<String, InputStream>> result =
                        httpHarvesterBean.harvest(httpConfig.getUrl());
                    harvestTasks.put(httpConfig, result);
                }
            } catch (HarvestException e) {
                LOGGER.error("error while harvesting for http {}",
                    httpConfig.getName(), e);
            }
        }
        sendResults();
    }

    private void sendResults() {
        try {
            Iterator<? extends Map.Entry<? super AbstractHarvesterConfigEntity,
                Future<Map<String, InputStream>>>> iterator = harvestTasks
                .entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<? super AbstractHarvesterConfigEntity,
                    Future<Map<String, InputStream>>> configEntry =
                    iterator.next();
                final AbstractHarvesterConfigEntity config =
                    (AbstractHarvesterConfigEntity) configEntry.getKey();
                try {
                    Future<Map<String, InputStream>> result = configEntry.getValue();
                    ftpSenderBean.send(result.get());
                    config.setLastHarvested(Date.from(Instant.now()));
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.warn("harvest task for {} interrupted", config.getName(), e);
                }
            }
        } finally {
            harvestTasks.clear();
        }
    }
}
