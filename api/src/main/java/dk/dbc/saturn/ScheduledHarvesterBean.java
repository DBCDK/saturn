/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
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
import java.util.Date;
import java.util.List;
import java.util.Set;

@Startup
@Singleton
@DependsOn("ProxyHandlerBean")
public class ScheduledHarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ScheduledHarvesterBean.class);

    @Inject RunScheduleFactory runScheduleFactory;
    @EJB HTTPHarvesterBean httpHarvesterBean;
    @EJB FtpHarvesterBean ftpHarvesterBean;
    @EJB SFtpHarvesterBean sftpHarvesterBean;
    @EJB HarvesterConfigRepository harvesterConfigRepository;
    @EJB FtpSenderBean ftpSenderBean;
    @EJB RunningTasks runningTasks;

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
            scheduleSFtpHarvests();
            scheduleHttpHarvests();
        } catch (Exception e) {
            LOGGER.error("caught unexpected exception while harvesting", e);
        }
    }

    private void scheduleFtpHarvests() {
        final List<FtpHarvesterConfig> ftpConfigs = harvesterConfigRepository
                .list(FtpHarvesterConfig.class, 0, 0);
        LOGGER.info("got {} FTP configs", ftpConfigs.size());
        for (FtpHarvesterConfig ftpConfig : ftpConfigs) {
            try {
                if ( runningTasks.isRunning(ftpConfig) ) {
                    LOGGER.debug("still harvesting, not rescheduled");
                    continue;
                }
                if (ftpConfig.isEnabled()
                        && runScheduleFactory.newRunScheduleFrom(ftpConfig.getSchedule())
                        .isSatisfiedBy(new Date(), ftpConfig.getLastHarvested())) {

                    Set<FileHarvest> fileHarvests = ftpHarvesterBean.listFiles(ftpConfig);

                    if (! fileHarvests.isEmpty()) {
                        runningTasks.add( ftpConfig );
                        ftpHarvesterBean.harvest( ftpConfig );
                        LOGGER.info( "Done scheduling {}", ftpConfig.getName());
                    }
                }
            } catch (HarvestException e) {
                LOGGER.error("Error while harvesting", e);
            }
        }
        LOGGER.info( "Number of tasks unfinshed:{}", runningTasks.size());
    }

    private void scheduleSFtpHarvests() {
        final List<SFtpHarvesterConfig> sftpConfigs = harvesterConfigRepository
                .list(SFtpHarvesterConfig.class, 0, 0);
        LOGGER.info("got {} FTP configs", sftpConfigs.size());
        for (SFtpHarvesterConfig sftpConfig : sftpConfigs) {
            try {
                if ( runningTasks.isRunning(sftpConfig) ) {
                    LOGGER.debug("still harvesting, not rescheduled");
                    continue;
                }
                if (sftpConfig.isEnabled()
                        && runScheduleFactory.newRunScheduleFrom(sftpConfig.getSchedule())
                        .isSatisfiedBy(new Date(), sftpConfig.getLastHarvested())) {

                    Set<FileHarvest> fileHarvests = sftpHarvesterBean.listFiles(sftpConfig);

                    if (! fileHarvests.isEmpty()) {
                        runningTasks.add( sftpConfig );
                        sftpHarvesterBean.harvest( sftpConfig );
                        LOGGER.info( "Done scheduling {}", sftpConfig.getName());
                    }
                }
            } catch (HarvestException e) {
                LOGGER.error("Error while harvesting", e);
            }
        }
        LOGGER.info( "Number of tasks unfinshed:{}", runningTasks.size());
    }

    private void scheduleHttpHarvests() {
        final List<HttpHarvesterConfig> httpConfigs = harvesterConfigRepository
                .list(HttpHarvesterConfig.class, 0, 0);

        LOGGER.info("got {} HTTP configs", httpConfigs.size());
        for (HttpHarvesterConfig httpConfig : httpConfigs) {
            if (runningTasks.isRunning( httpConfig )) {
                LOGGER.info("still harvesting, not rescheduled");
                continue;
            }
            try {
                if (httpConfig.isEnabled()
                        && runScheduleFactory.newRunScheduleFrom(httpConfig.getSchedule())
                        .isSatisfiedBy(new Date(), httpConfig.getLastHarvested())) {
                    Set<FileHarvest> fileHarvests = httpHarvesterBean.listFiles(httpConfig);
                    if ( ! fileHarvests.isEmpty() ){
                        runningTasks.add( httpConfig );
                        httpHarvesterBean.harvest(httpConfig);
                    }
                }
            } catch (HarvestException e) {
                LOGGER.error("error while scheduling harvest", e);
            }
        }
    }
}
