/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.HttpHarvesterConfig;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static dk.dbc.saturn.TestUtils.getHttpHarvesterConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledHarvesterBeanTest {
    private static final FtpSenderBean ftpSenderBean = mock(FtpSenderBean.class);
    private static final RunningTasks runningTasks = new RunningTasks();
    private static final HTTPHarvesterBean httpHarvesterBean = mock(HTTPHarvesterBean.class);

    @Test
    void test_harvest() throws HarvestException, ParseException {
        final HttpHarvesterConfig config = getHttpHarvesterConfig();

        final Set<FileHarvest> fileHarvests = Collections.singleton(new MockFileHarvest("spongebob", "spongebob", 3));
        final List<HttpHarvesterConfig> httpConfigs = Collections.singletonList(getHttpHarvesterConfig());
        final Future future = mock(Future.class);
        final ScheduledHarvesterBean scheduledHarvesterBean = getScheduledHarvesterBean();
        when(scheduledHarvesterBean.harvesterConfigRepository.list(HttpHarvesterConfig.class, 0, 0)).thenReturn(httpConfigs);
        when(scheduledHarvesterBean.httpHarvesterBean.harvest(any(HttpHarvesterConfig.class), any(ProgressTrackerBean.Key.class))).thenReturn(future);
        when(future.isDone()).thenReturn(true);

        when(scheduledHarvesterBean.httpHarvesterBean.listFiles(any(HttpHarvesterConfig.class))).thenReturn(fileHarvests);
        config.setId(1);
        runningTasks.add(config);
        scheduledHarvesterBean.harvest();
        ProgressTrackerBean.Key progressKey = new ProgressTrackerBean.Key(HttpHarvesterConfig.class, config.getId());

        // Test that no new harvest is launched if an earlier version is still running..
        MatcherAssert.assertThat("task list after first run", runningTasks.size(), is(1));
        verify(scheduledHarvesterBean.httpHarvesterBean, times(0)).harvest(config, progressKey);
        runningTasks.remove(config);
        MatcherAssert.assertThat("task list after removing this config", runningTasks.size(), is(0));

        scheduledHarvesterBean.harvest();
        MatcherAssert.assertThat("empty task list after second run", runningTasks.size(), is(1));

        // verify that harvest was called once
        verify(scheduledHarvesterBean.httpHarvesterBean, times(1)).harvest(config, progressKey);
    }

    private ScheduledHarvesterBean getScheduledHarvesterBean() {
        final RunScheduleFactory runScheduleFactory = new RunScheduleFactory("Europe/Copenhagen");
        final ScheduledHarvesterBean scheduledHarvesterBean = new ScheduledHarvesterBean();
        scheduledHarvesterBean.runScheduleFactory = runScheduleFactory;
        scheduledHarvesterBean.ftpSenderBean = ftpSenderBean;
        scheduledHarvesterBean.harvesterConfigRepository = mock(HarvesterConfigRepository.class);
        scheduledHarvesterBean.httpHarvesterBean = httpHarvesterBean;
        scheduledHarvesterBean.runningTasks = runningTasks;
        return scheduledHarvesterBean;
    }
}
