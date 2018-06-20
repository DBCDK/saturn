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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HarvesterConfigApiTest {
    private final static HarvesterConfigApi harvesterConfigApi =
        new HarvesterConfigApi();
    private final static JSONBContext jsonbContext = new JSONBContext();
    private static UriInfo mockedUriInfo;

    final static CollectionType httpHarvesterConfigType =
        jsonbContext.getTypeFactory().constructCollectionType(
            List.class, HttpHarvesterConfig.class);
    final static CollectionType ftpHarvesterConfigType =
        jsonbContext.getTypeFactory().constructCollectionType(
            List.class, FtpHarvesterConfig.class);

    @BeforeAll
    static void setup() throws URISyntaxException {
        harvesterConfigApi.harvesterConfigRepository =
            mock(HarvesterConfigRepository.class);
        mockedUriInfo = mock(UriInfo.class);
        UriBuilder mockedUriBuilder = mock(UriBuilder.class);
        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));
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

    @Test
    void test_add_httpHarvester() {
        final String harvesterConfig = "{\"url\": \"spongebob\", " +
            "\"schedule\": \"!!\", \"transfile\": \"squarepants\"}";
        Response response = harvesterConfigApi.addHttpHarvesterConfig(
            mockedUriInfo, harvesterConfig);
        assertThat("status", response.getStatus(), is(201));
    }

    @Test
    void test_add_httpHarvesterBadRequest() {
        final String harvesterConfig = "{\"url\": \"patrick\", " +
            "\"schedule\": \"uuuuuuhh\", \"TRANSFILE\": \"barnacles!\"}";
        Response response = harvesterConfigApi.addHttpHarvesterConfig(
            mockedUriInfo, harvesterConfig);
        assertThat("status", response.getStatus(), is(400));
    }

    @Test
    void test_add_ftpHarvester() {
        final String harvesterConfig = "{\"host\": \"bikini_bottom\", " +
            "\"port\": 5432, \"username\": \"patrick\", \"password\": " +
            "\"*\", \"schedule\": \"uuuh nothing..\", \"transfile\": " +
            "\"tartar sauce\", \"dir\": \"season_1\", \"filesPattern\": " +
            "\"hawaii*.mp3\"}";
        Response response = harvesterConfigApi.addFtpHarvesterConfig(
            mockedUriInfo, harvesterConfig);
        assertThat("status", response.getStatus(), is(201));
    }

    @Test
    void test_add_ftpHarvesterBadRequest() {
        final String harvesterConfig = "{\"url\": \"patrick\", " +
            "\"schedule\": \"uuuuuuhh\", \"TRANSFILE\": \"barnacles!\"}";
        Response response = harvesterConfigApi.addFtpHarvesterConfig(
            mockedUriInfo, harvesterConfig);
        assertThat("status", response.getStatus(), is(400));
    }

    @Test
    void test_getHttpHarvesterConfig() throws JSONBException {
        HttpHarvesterConfig config = new HttpHarvesterConfig();
        config.setUrl("BubbleBuddy!");
        when(harvesterConfigApi.harvesterConfigRepository
            .getHarvesterConfig(eq(HttpHarvesterConfig.class),
                anyInt()))
            .thenReturn(Optional.of(config));
        Response response = harvesterConfigApi.getHttpHarvesterConfig(1);
        assertThat("status", response.getStatus(), is(200));
        assertThat("has entity", response.hasEntity(), is(true));

        final String configsJson = (String) response.getEntity();
        final HttpHarvesterConfig configResult = jsonbContext.unmarshall(
            configsJson, HttpHarvesterConfig.class);
        assertThat("result entity is not null", configResult, notNullValue());
        assertThat("name", configResult.getUrl(), is("BubbleBuddy!"));
    }

    @Test
    void test_getFtpHarvesterConfig() throws JSONBException {
        FtpHarvesterConfig config = new FtpHarvesterConfig();
        config.setHost("Tartar sauce!");
        when(harvesterConfigApi.harvesterConfigRepository
            .getHarvesterConfig(eq(FtpHarvesterConfig.class),
                anyInt()))
            .thenReturn(Optional.of(config));
        Response response = harvesterConfigApi.getFtpHarvesterConfig(1);
        assertThat("status", response.getStatus(), is(200));
        assertThat("has entity", response.hasEntity(), is(true));

        final String configsJson = (String) response.getEntity();
        final FtpHarvesterConfig configResult = jsonbContext.unmarshall(
            configsJson, FtpHarvesterConfig.class);
        assertThat("result entity is not null", configResult, notNullValue());
        assertThat("name", configResult.getHost(), is("Tartar sauce!"));
    }

    @Test
    void test_getHttpHarvesterConfig_notFound() throws JSONBException {
        when(harvesterConfigApi.harvesterConfigRepository
            .getHarvesterConfig(eq(HttpHarvesterConfig.class),
                anyInt()))
            .thenReturn(Optional.empty());
        Response response = harvesterConfigApi.getHttpHarvesterConfig(1);
        assertThat("status", response.getStatus(), is(404));
    }

    @Test
    void test_getFtpHarvesterConfig_notFound() throws JSONBException {
        when(harvesterConfigApi.harvesterConfigRepository
            .getHarvesterConfig(eq(FtpHarvesterConfig.class),
                anyInt()))
            .thenReturn(Optional.empty());
        Response response = harvesterConfigApi.getFtpHarvesterConfig(1);
        assertThat("status", response.getStatus(), is(404));
    }
}