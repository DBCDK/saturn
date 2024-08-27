/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.job.JobSenderBean;
import org.junit.Test;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static dk.dbc.saturn.TestUtils.getHttpHarvesterConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledHarvesterBeanTest {
    private static final JobSenderBean JOB_SENDER_BEAN = mock(JobSenderBean.class);
    private static final RunningTasks runningTasks = new RunningTasks();
    HarvesterConfigRepository harvesterConfigRepository = mock(HarvesterConfigRepository.class);

    @Test
    public void test_harvest() throws HarvestException, ParseException {
        Set<FileHarvest> fileHarvests = Set.of(new MockFileHarvest("spongebob", "spongebob", 3));
        List<HttpHarvesterConfig> httpConfigs = Collections.singletonList(getHttpHarvesterConfig());
        ScheduledHarvesterBean scheduledHarvesterBean = makeScheduledHarvesterBean(fileHarvests);
        when(harvesterConfigRepository.list(HttpHarvesterConfig.class, 0, 0)).thenReturn(httpConfigs);

        scheduledHarvesterBean.harvest();
    }

    private ScheduledHarvesterBean makeScheduledHarvesterBean(Set<FileHarvest> fileHarvests) throws HarvestException {
        HTTPHarvesterBean httpHarvesterBean = new HTTPHarvesterBean(harvesterConfigRepository, JOB_SENDER_BEAN, runningTasks, null, mock(ProxyBean.class)) {
            @Override
            protected Harvester<HttpHarvesterConfig> self() {
                return this;
            }

            @Override
            public Set<FileHarvest> listFiles(HttpHarvesterConfig config) throws HarvestException {
                return fileHarvests;
            }
        };
        SFtpHarvesterBean sFtpHarvesterBean = mock(SFtpHarvesterBean.class);
        FtpHarvesterBean ftpHarvesterBean = mock(FtpHarvesterBean.class);
        final ScheduledHarvesterBean scheduledHarvesterBean = new ScheduledHarvesterBean(httpHarvesterBean, ftpHarvesterBean, sFtpHarvesterBean, harvesterConfigRepository);
        for (Harvester<? extends AbstractHarvesterConfigEntity> c : List.of(sFtpHarvesterBean, ftpHarvesterBean)) {
            when(c.listFiles(any())).thenReturn(Set.of());
        }
        return scheduledHarvesterBean;
    }
}
