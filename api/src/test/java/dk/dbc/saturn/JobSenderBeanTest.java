/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.saturn.job.JobSenderBean;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobSenderBeanTest {
    ProgressTrackerBean progressTracker = new ProgressTrackerBean();
    FileStoreServiceConnector fileStore = mock(FileStoreServiceConnector.class);
    JobStoreServiceConnector jobstore = mock(JobStoreServiceConnector.class);

    @Test
    public void send() throws HarvestException, FileStoreServiceConnectorException, JobStoreServiceConnectorException {
        JobSenderBean jobSenderBean = makeJobSenderBean();
        JobInfoSnapshot jobInfoSnapshot = mock(JobInfoSnapshot.class);
        when(jobInfoSnapshot.getJobId()).thenReturn(42);
        when(fileStore.addFile(any(InputStream.class))).thenReturn("abc");
        ArgumentCaptor<JobInputStream> captor = ArgumentCaptor.forClass(JobInputStream.class);
        when(jobstore.addJob(any(JobInputStream.class))).thenReturn(jobInfoSnapshot);
        Set<FileHarvest> inputStreams = getFileHarvests("sponge", "bob");
        final String transfile = "transfile";
        progressTracker.add(0);
        jobSenderBean.send(inputStreams, transfile, "b=ticklerepo,c=utf8,t=iso,o=viaf,m=any@dbc.dk",  0);
        verify(jobstore, times(2)).addJob(captor.capture());

        int current = progressTracker.get(0).getCurrentFiles();
        Set<String> dataFiles = captor.getAllValues().stream().map(JobInputStream::getJobSpecification).map(JobSpecification::getAncestry).map(JobSpecification.Ancestry::getDatafile).collect(Collectors.toSet());
        captor.getAllValues().stream().map(JobInputStream::getJobSpecification).forEach(js -> {
            assertThat("Correct format", js.getFormat(), is("viaf"));
            assertThat("Correct packaging", js.getPackaging(), is("iso"));
            assertThat("Correct charset", js.getCharset(), is("utf8"));
            assertThat("Correct destination", js.getDestination(), is("ticklerepo"));
            assertThat("Correct mail", js.getMailForNotificationAboutVerification(), is("any@dbc.dk"));
        });
        assertThat("Ancestor data files are correct", dataFiles, is(Set.of("sponge", "bob")));
        assertThat("Two files was transferred", current, is(2));
    }

    private JobSenderBean makeJobSenderBean() {
        JobStoreServiceConnectorBean bean = new JobStoreServiceConnectorBean(jobstore);
        return new JobSenderBean(progressTracker, fileStore, bean, 1);
    }

    private Set<FileHarvest> getFileHarvests(String... contentList) {
        Set<FileHarvest> fileHarvests = new HashSet<>(contentList.length);
        for (String content : contentList) {
            fileHarvests.add(new MockFileHarvest(content, content, 0));
        }
        return fileHarvests;
    }

}
