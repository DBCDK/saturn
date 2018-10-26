/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.HttpHarvesterConfig;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledHarvesterBeanIT extends AbstractIntegrationTest {
    private static final CronParserBean cronParserBean = mock(CronParserBean.class);
    private static final FtpSenderBean ftpSenderBean = mock(FtpSenderBean.class);
    private static final HTTPHarvesterBean httpHarvesterBean = mock(HTTPHarvesterBean.class);

    @Test
    void test_harvest() throws HarvestException,
            InterruptedException, ExecutionException, ParseException {
        final HttpHarvesterConfig config = getHttpHarvesterConfig();

        harvesterConfigRepository.entityManager.persist(config);
        harvesterConfigRepository.entityManager.flush();

        when(cronParserBean.shouldExecute(anyString(), any(Date.class)))
            .thenReturn(true);

        final Set<FileHarvest> fileHarvests = Collections.singleton(
            new FileHarvest("spongebob", null, 3));
        final Future future = mock(Future.class);
        when(future.get()).thenReturn(fileHarvests);
        when(future.isDone()).thenReturn(false);
        when(httpHarvesterBean.harvest(anyString())).thenReturn(future);

        final ScheduledHarvesterBean scheduledHarvesterBean =
            getScheduledHarvesterBean();
        scheduledHarvesterBean.harvest();

        assertThat("task list after first run", scheduledHarvesterBean
            .harvestTasks.size(), is(1));
        final FileHarvest result = scheduledHarvesterBean.harvestTasks.get(
            config.getId()).get().iterator().next();
        assertThat("file harvest filename", result.getFilename(),
            is("spongebob"));

        // simulate a second passing where the harvest is done
        when(future.isDone()).thenReturn(true);
        scheduledHarvesterBean.harvest();

        assertThat("empty task list after second run", scheduledHarvesterBean
            .harvestTasks.size(), is(0));
        // verify that two passes happened
        verify(future, times(2)).isDone();
    }

    private ScheduledHarvesterBean getScheduledHarvesterBean() {
        ScheduledHarvesterBean scheduledHarvesterBean = new ScheduledHarvesterBean();
        scheduledHarvesterBean.cronParserBean = cronParserBean;
        scheduledHarvesterBean.ftpSenderBean = ftpSenderBean;
        scheduledHarvesterBean.harvesterConfigRepository = harvesterConfigRepository;
        scheduledHarvesterBean.httpHarvesterBean = httpHarvesterBean;
        return scheduledHarvesterBean;
    }
}
