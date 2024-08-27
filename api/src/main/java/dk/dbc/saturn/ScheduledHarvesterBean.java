/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Startup
@Singleton
@DependsOn("ProxyBean")
public class ScheduledHarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledHarvesterBean.class);
    @EJB
    private HTTPHarvesterBean httpHarvesterBean;
    @EJB
    private FtpHarvesterBean ftpHarvesterBean;
    @EJB
    private SFtpHarvesterBean sftpHarvesterBean;
    @EJB
    private HarvesterConfigRepository harvesterConfigRepository;

    public ScheduledHarvesterBean() {
    }

    public ScheduledHarvesterBean(HTTPHarvesterBean httpHarvesterBean, FtpHarvesterBean ftpHarvesterBean, SFtpHarvesterBean sftpHarvesterBean, HarvesterConfigRepository harvesterConfigRepository) {
        this.httpHarvesterBean = httpHarvesterBean;
        this.ftpHarvesterBean = ftpHarvesterBean;
        this.sftpHarvesterBean = sftpHarvesterBean;
        this.harvesterConfigRepository = harvesterConfigRepository;
    }

    @PostConstruct
    public void init() {
        // For some reason we need to touch the MDC context initially
        // to get any MDC logging at all???
        MDC.clear();
    }

    @Schedule(minute = "*", hour = "*", second = "*/20")
    public void harvest() {
        try {
            harvesterConfigRepository.list(FtpHarvesterConfig.class, 0, 0).forEach(c -> ftpHarvesterBean.doHarvest(c));
            harvesterConfigRepository.list(SFtpHarvesterConfig.class, 0, 0).forEach(c -> sftpHarvesterBean.doHarvest(c));
            harvesterConfigRepository.list(HttpHarvesterConfig.class, 0, 0).forEach(c -> httpHarvesterBean.doHarvest(c));
        } catch (Exception e) {
            LOGGER.error("caught unexpected exception while harvesting", e);
        }
    }
}
