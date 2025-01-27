/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    @Resource(lookup = "java:comp/DefaultManagedThreadFactory")
    private ManagedThreadFactory threadFactory;
    private ThreadPoolExecutor executorService;
    @Inject
    private MetricRegistry metricRegistry;
    @Inject
    private RunScheduleBean runScheduleBean;

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
        executorService = new ThreadPoolExecutor(1, 16, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
        metricRegistry.gauge("harvest_scheduler_threads", executorService::getActiveCount, new Tag("status", "active"));
        metricRegistry.gauge("harvest_scheduler_threads", executorService::getCompletedTaskCount, new Tag("status", "completedTasks"));
        metricRegistry.gauge("harvest_scheduler_threads", executorService::getPoolSize, new Tag("status", "poolSize"));
        metricRegistry.gauge("harvest_scheduler_threads", executorService::getMaximumPoolSize, new Tag("status", "maxPoolSize"));
    }

    @Schedule(minute = "*", hour = "*", second = "*/20")
    public void harvest() {
        try {
            harvesterConfigRepository.list(FtpHarvesterConfig.class, 0, 0).forEach(c -> doHarvest(ftpHarvesterBean, c));
            harvesterConfigRepository.list(SFtpHarvesterConfig.class, 0, 0).forEach(c -> doHarvest(sftpHarvesterBean, c));
            harvesterConfigRepository.list(HttpHarvesterConfig.class, 0, 0).forEach(c -> doHarvest(httpHarvesterBean, c));
        } catch (Exception e) {
            LOGGER.error("caught unexpected exception while harvesting", e);
        }
    }

    public <T extends AbstractHarvesterConfigEntity> void runNow(Class<T> clazz, int configId) {
        if(clazz == FtpHarvesterConfig.class) executorService.submit(() -> ftpHarvesterBean.runHarvest((Class<FtpHarvesterConfig>) clazz, configId, true));
        if(clazz == SFtpHarvesterConfig.class) executorService.submit(() -> sftpHarvesterBean.runHarvest((Class<SFtpHarvesterConfig>) clazz, configId, true));
        if(clazz == HttpHarvesterConfig.class) executorService.submit(() -> httpHarvesterBean.runHarvest((Class<HttpHarvesterConfig>) clazz, configId, true));
    }

    public <T extends AbstractHarvesterConfigEntity> void doHarvest(Harvester<T> harvester, T config) {
        if(!config.isEnabled() || runScheduleBean.shouldSkip(config)) return;
            //noinspection unchecked
        executorService.submit(() -> harvester.runHarvest((Class<T>) config.getClass(), config.getId()));
    }
}
