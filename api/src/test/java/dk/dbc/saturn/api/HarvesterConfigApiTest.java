/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.api;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.saturn.HarvesterConfigRepository;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HarvesterConfigApiTest {
    private final static HarvesterConfigApi harvesterConfigApi =
        new HarvesterConfigApi();
    private final static JSONBContext jsonbContext = new JSONBContext();

    final static CollectionType httpHarvesterConfigType =
        jsonbContext.getTypeFactory().constructCollectionType(
            List.class, HttpHarvesterConfig.class);
    final static CollectionType ftpHarvesterConfigType =
        jsonbContext.getTypeFactory().constructCollectionType(
            List.class, FtpHarvesterConfig.class);

    @BeforeAll
    static void setup() {
        harvesterConfigApi.harvesterConfigRepository =
            mock(HarvesterConfigRepository.class);
    }

    @Test
    void test_list_httpEmpty() throws JSONBException {
        when(harvesterConfigApi.harvesterConfigRepository
            .list(eq(HttpHarvesterConfig.class), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        Response response = harvesterConfigApi.listHttpHarvesterConfigs(
            0, 0);

        assertThat("response status", response.getStatus(), is(200));
        assertThat("response has entity", response.hasEntity(), is(true));

        final String configsJson = (String) response.getEntity();
        final List<HttpHarvesterConfig> configs = jsonbContext.unmarshall(
            configsJson, httpHarvesterConfigType);
        assertThat("configs size", configs.size(), is(0));
    }

    @Test
    void test_list_ftpEmpty() throws JSONBException {
        when(harvesterConfigApi.harvesterConfigRepository
            .list(eq(FtpHarvesterConfig.class), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        Response response = harvesterConfigApi.listFtpHarvesterConfigs(
            0, 0);

        assertThat("response status", response.getStatus(), is(200));
        assertThat("response has entity", response.hasEntity(), is(true));

        final String configsJson = (String) response.getEntity();
        final List<FtpHarvesterConfig> configs = jsonbContext.unmarshall(
            configsJson, ftpHarvesterConfigType);
        assertThat("configs size", configs.size(), is(0));
    }

    @Test
    void test_list_httpHarvester() throws JSONBException {
        List<HttpHarvesterConfig> configs = Stream.of("jack_sparrow", "elizabeth_swan",
            "hector_barbossa").map(item -> {
                HttpHarvesterConfig entity = new HttpHarvesterConfig();
                entity.setUrl(item);
                return entity;
        }).collect(Collectors.toList());
        when(harvesterConfigApi.harvesterConfigRepository
            .list(eq(HttpHarvesterConfig.class), anyInt(), anyInt()))
            .thenReturn(configs);

        Response response = harvesterConfigApi.listHttpHarvesterConfigs(
            0, 0);

        assertThat("response status", response.getStatus(), is(200));
        assertThat("response has entity", response.hasEntity(), is(true));

        final String configsJson = (String) response.getEntity();
        final List<HttpHarvesterConfig> resultsConfigs = jsonbContext
            .unmarshall(configsJson, httpHarvesterConfigType);

        assertThat("configs size", resultsConfigs.size(), is(3));
        assertThat("result 1 url", resultsConfigs.get(0).getUrl(),
            is("jack_sparrow"));
    }

    @Test
    void test_list_ftpHarvester() throws JSONBException {
        List<FtpHarvesterConfig> configs = Stream.of("jack_sparrow", "elizabeth_swan",
            "hector_barbossa").map(item -> {
                FtpHarvesterConfig entity = new FtpHarvesterConfig();
                entity.setHost(item);
                return entity;
        }).collect(Collectors.toList());
        when(harvesterConfigApi.harvesterConfigRepository
            .list(eq(FtpHarvesterConfig.class), anyInt(), anyInt()))
            .thenReturn(configs);

        Response response = harvesterConfigApi.listFtpHarvesterConfigs(
            0, 0);

        assertThat("response status", response.getStatus(), is(200));
        assertThat("response has entity", response.hasEntity(), is(true));

        final String configsJson = (String) response.getEntity();
        final List<FtpHarvesterConfig> resultsConfigs = jsonbContext
            .unmarshall(configsJson, ftpHarvesterConfigType);

        assertThat("configs size", resultsConfigs.size(), is(3));
        assertThat("result 1 url", resultsConfigs.get(0).getHost(),
            is("jack_sparrow"));
    }
}
