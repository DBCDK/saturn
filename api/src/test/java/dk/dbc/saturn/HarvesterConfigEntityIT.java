/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.CustomHttpHeader;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HarvesterConfigEntityIT extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterConfigEntityIT.class);

    @Test
    void test_httpHarvesterEntities() throws ParseException {
        final HttpHarvesterConfig config1 = new HttpHarvesterConfig();
        config1.setName("Patar");
        config1.setSchedule("0 13 20 * *");
        config1.setUrl("http://skerdernogetiaarhusiaften.dk/");
        config1.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk");
        config1.setAgency("010100");
        config1.setEnabled(true);

        final HttpHarvesterConfig config2 = new HttpHarvesterConfig();
        config2.setName("MyName'sNotRick!");
        config2.setSchedule("1 * * * *");
        config2.setUrl("http://nick.com");
        config2.setLastHarvested(getDate("2018-06-06T20:20:20",
            "Europe/Copenhagen"));
        config2.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk");
        config2.setAgency("010100");
        config2.setEnabled(true);

        harvesterConfigRepository.entityManager.persist(config1);
        harvesterConfigRepository.entityManager.flush();
        harvesterConfigRepository.entityManager.persist(config2);

        harvesterConfigRepository.entityManager.getTransaction().commit();

        final List<HttpHarvesterConfig> entities = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);

        assertThat("results", entities.size(), is(2));
        assertThat("1st result", entities.get(0), is(config2));
        assertThat("2nd result", entities.get(1), is(config1));
    }

    @Test
    void test_ftpHarvesterEntities() {
        final FtpHarvesterConfig config1 = new FtpHarvesterConfig();
        config1.setName("Patar");
        config1.setSchedule("0 13 20 * *");
        config1.setHost("skerdernogetiaarhusiaften.dk");
        config1.setPort(1234);
        config1.setUsername("JackSparrow");
        config1.setPassword("blackpearl");
        config1.setDir("tortuga");
        config1.setFilesPattern("treasure-map*.jpg");
        config1.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk");
        config1.setAgency("010100");
        config1.setEnabled(true);

        final FtpHarvesterConfig config2 = new FtpHarvesterConfig();
        config2.setName("MyName'sNotRick!");
        config2.setSchedule("1 * * * *");
        config2.setHost("nick.com");
        config2.setPort(1234);
        config2.setUsername("PatrickStar");
        config2.setPassword("uuuuh");
        config2.setDir("rock/bottom");
        config2.setFilesPattern("glove-candy.jpg");
        config2.setLastHarvested(Timestamp.from(
            Instant.ofEpochSecond(1234567)));
        config2.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk");
        config2.setAgency("010100");
        config2.setEnabled(true);

        harvesterConfigRepository.entityManager.persist(config1);
        harvesterConfigRepository.entityManager.flush();
        harvesterConfigRepository.entityManager.persist(config2);

        harvesterConfigRepository.entityManager.getTransaction().commit();

        final List<FtpHarvesterConfig> entities = harvesterConfigRepository
            .list(FtpHarvesterConfig.class, 0, 0);

        assertThat("results", entities.size(), is(2));
        assertThat("1st result", entities.get(0), is(config2));
        assertThat("2nd result", entities.get(1), is(config1));
    }

    @Test
    void test_harvesterConfigStartLimitHttp() {
        String[] names = {"spongebob", "patrick", "squidward", "larry",
            "gary"};
        for(String name : names) {
            HttpHarvesterConfig config = new HttpHarvesterConfig();
            config.setName(name);
            config.setUrl(name);
            config.setSchedule(name);
            config.setTransfile(name);
            config.setAgency("010100");
            config.setEnabled(true);
            harvesterConfigRepository.entityManager.persist(config);
            harvesterConfigRepository.entityManager.flush();
        }
        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<HttpHarvesterConfig> configs = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 4, 2);

        assertThat("limit results", configs.size(), is(2));
        assertThat("first result id", configs.get(0).getUrl(), is("gary"));
    }

    @Test
    void test_harvesterConfigStartLimitFtp() {
        String[] names = {"spongebob", "patrick", "squidward", "larry",
            "gary"};
        for(String name : names) {
            FtpHarvesterConfig config = new FtpHarvesterConfig();
            config.setName(name);
            config.setHost(name);
            config.setSchedule(name);
            config.setTransfile(name);
            config.setPort(5432);
            config.setUsername(name);
            config.setPassword(name);
            config.setDir(name);
            config.setFilesPattern(name);
            config.setAgency("010100");
            config.setEnabled(true);
            harvesterConfigRepository.entityManager.persist(config);
            harvesterConfigRepository.entityManager.flush();
        }
        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<FtpHarvesterConfig> configs = harvesterConfigRepository
            .list(FtpHarvesterConfig.class, 0, 3);

        assertThat("limit results", configs.size(), is(3));
        assertThat("first result id", configs.get(2).getHost(), is("squidward"));
    }

    @Test
    void test_add_httpHarvesterConfig() {
        String[] names = {"spongebob", "patrick", "squidward", "larry",
            "gary"};
        for(String name : names) {
            HttpHarvesterConfig httpHarvesterConfig = new HttpHarvesterConfig();
            httpHarvesterConfig.setName(name);
            httpHarvesterConfig.setUrl(name);
            httpHarvesterConfig.setSchedule(name);
            httpHarvesterConfig.setTransfile(name);
            httpHarvesterConfig.setAgency("010100");
            harvesterConfigRepository.add(HttpHarvesterConfig.class,
                httpHarvesterConfig, mockedUriBuilder);
            httpHarvesterConfig.setUrlPattern("http://" + name);
            httpHarvesterConfig.setEnabled(true);
            httpHarvesterConfig.setGzip(name.startsWith("s"));
            LOGGER.info("Gzip {} {}", name, httpHarvesterConfig.getGzip());
            httpHarvesterConfig.setHttpHeaders(List.of(
                    new CustomHttpHeader().withKey("myKey1").withValue("myValue1"),
                    new CustomHttpHeader().withKey("myKey1").withValue("myValue2"),
                    new CustomHttpHeader().withKey("myKey2").withValue("myValue1.2")
            ));
        }

        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<HttpHarvesterConfig> configs = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);

        assertThat("list size", configs.size(), is(5));
        assertThat("entity 1 url", configs.get(0).getUrl(), is("gary"));
        assertThat("entity 1 urlpattern", configs.get(0).getUrlPattern(),
            is("http://gary"));
        assertThat("entity 2 schedule", configs.get(1).getSchedule(),
            is("larry"));
        assertThat("entity 3 transfile", configs.get(2).getTransfile(),
            is("squidward"));

        // Check only one of the configs for headers, since all are equipped with same three
        // pair of headers.
        assertThat("first header pair", configs.get(0).getHttpHeaders(),
                hasItem(new CustomHttpHeader().withKey("myKey1").withValue("myValue1")));
        assertThat("second header pair", configs.get(0).getHttpHeaders(),
                hasItem(new CustomHttpHeader().withKey("myKey1").withValue("myValue2")));
        for (HttpHarvesterConfig harvesterConfig : configs) {
            if (harvesterConfig.getName().startsWith("s")) {
                Assertions.assertTrue(harvesterConfig.getGzip(), harvesterConfig.getName()+" is gzipped");
            } else {
                Assertions.assertFalse(harvesterConfig.getGzip(), harvesterConfig.getName() + " is NOT gzipped");
            }
      }
    }

    @Test
    void test_add_ftpHarvesterConfig() {
        String[] names = {"spongebob", "patrick", "squidward", "larry",
            "gary"};
        for(String name : names) {
            FtpHarvesterConfig ftpHarvesterConfig = new FtpHarvesterConfig();
            ftpHarvesterConfig.setName(name);
            ftpHarvesterConfig.setHost(name);
            ftpHarvesterConfig.setPort(5432);
            ftpHarvesterConfig.setUsername(name);
            ftpHarvesterConfig.setPassword(name);
            ftpHarvesterConfig.setFilesPattern(name);
            ftpHarvesterConfig.setDir(name);
            ftpHarvesterConfig.setSchedule(name);
            ftpHarvesterConfig.setTransfile(name);
            ftpHarvesterConfig.setAgency("010100");
            ftpHarvesterConfig.setEnabled(true);
            harvesterConfigRepository.add(FtpHarvesterConfig.class,
                ftpHarvesterConfig, mockedUriBuilder);
        }

        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<FtpHarvesterConfig> configs = harvesterConfigRepository
            .list(FtpHarvesterConfig.class, 0, 0);

        assertThat("list size", configs.size(), is(5));
        assertThat("entity 1 host", configs.get(0).getHost(), is("gary"));
        assertThat("entity 2 schedule", configs.get(1).getSchedule(),
            is("larry"));
        assertThat("entity 3 transfile", configs.get(2).getTransfile(),
            is("squidward"));
    }

    @Test
    void test_add_updateHttpHarvesterConfig() {
        HttpHarvesterConfig config = new HttpHarvesterConfig();
        config.setName("plankton");
        config.setUrl("chumbucket.ru");
        config.setSchedule("* * * * *");
        config.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk");
        config.setAgency("010100");
        config.setEnabled(true);
        harvesterConfigRepository.add(HttpHarvesterConfig.class, config,
            mockedUriBuilder);
        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<HttpHarvesterConfig> preliminaryList = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);
        assertThat(preliminaryList.size(), is(1));
        final int entityId = preliminaryList.get(0).getId();

        harvesterConfigRepository.entityManager.getTransaction().begin();
        HttpHarvesterConfig config2 = new HttpHarvesterConfig();
        // same name and id:
        config2.setId(entityId);
        config2.setName("plankton");
        config2.setUrl("chumbucket.com");
        config2.setSchedule("1 * 12 * 31");
        config2.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk");
        config2.setAgency("010100");
        config2.setEnabled(true);
        harvesterConfigRepository.add(HttpHarvesterConfig.class, config2,
            mockedUriBuilder);
        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<HttpHarvesterConfig> configs = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);
        assertThat("results size", configs.size(), is(1));

        HttpHarvesterConfig resultConfig = configs.get(0);
        assertThat("result id", resultConfig.getId(), is(entityId));
        assertThat("result name", resultConfig.getName(), is("plankton"));
        assertThat("result url", resultConfig.getUrl(), is("chumbucket.com"));
        assertThat("result schedule", resultConfig.getSchedule(),
            is("1 * 12 * 31"));
        assertThat("result transfile", resultConfig.getTransfile(),
            is("b=databroendpr3,f=$DATAFIL,t=abmxml,c=latin-1,o=littsiden," +
            "m=kildepost@dbc.dk"));
    }

    @Test
    void test_delete_httpHarvesterConfig() throws ParseException {
        HttpHarvesterConfig config = getHttpHarvesterConfig();
        harvesterConfigRepository.entityManager.persist(config);
        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<HttpHarvesterConfig> listBeforeDelete = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);
        assertThat("list size before delete", listBeforeDelete.size(), is(1));
        int id = listBeforeDelete.get(0).getId();

        harvesterConfigRepository.entityManager.getTransaction().begin();
        harvesterConfigRepository.delete(HttpHarvesterConfig.class, id);
        harvesterConfigRepository.entityManager.getTransaction().commit();
        List<HttpHarvesterConfig> listAfterDelete = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);
        assertThat("list size after delete", listAfterDelete.size(), is(0));
    }

    @Test
    void test_delete_ftpHarvesterConfig() throws ParseException {
        FtpHarvesterConfig config = getFtpHarvesterConfig();
        harvesterConfigRepository.entityManager.persist(config);
        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<FtpHarvesterConfig> listBeforeDelete = harvesterConfigRepository
            .list(FtpHarvesterConfig.class, 0, 0);
        assertThat("list size before delete", listBeforeDelete.size(), is(1));
        int id = listBeforeDelete.get(0).getId();

        harvesterConfigRepository.entityManager.getTransaction().begin();
        harvesterConfigRepository.delete(FtpHarvesterConfig.class, id);
        harvesterConfigRepository.entityManager.getTransaction().commit();
        List<HttpHarvesterConfig> listAfterDelete = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);
        assertThat("list size after delete", listAfterDelete.size(), is(0));
    }

    @Test
    void test_getHarvesterConfigType() throws ParseException {
        FtpHarvesterConfig ftpHarvesterConfig = getFtpHarvesterConfig();
        HttpHarvesterConfig httpHarvesterConfig = getHttpHarvesterConfig();
        SFtpHarvesterConfig sFtpHarvesterConfig = getSFtpHarvesterConfig();

        harvesterConfigRepository.entityManager.persist(ftpHarvesterConfig);
        harvesterConfigRepository.entityManager.persist(httpHarvesterConfig);
        harvesterConfigRepository.entityManager.persist(sFtpHarvesterConfig);
        harvesterConfigRepository.entityManager.flush();

        Optional ftpConfigOptional = harvesterConfigRepository
            .getHarvesterConfigType(ftpHarvesterConfig.getId());
        Optional httpConfigOptional = harvesterConfigRepository
            .getHarvesterConfigType(httpHarvesterConfig.getId());
        Optional sFtpConfigOptional = harvesterConfigRepository
                .getHarvesterConfigType(sFtpHarvesterConfig.getId());

        assertThat("ftpharvesterconfig is present",
            ftpConfigOptional.isPresent(), is(true));
        assertThat("sftpharvesterconfig is present",
            sFtpConfigOptional.isPresent(), is(true));
        assertThat("httpharvesterconfig is present",
                httpConfigOptional.isPresent(), is(true));
        assertThat("ftpharvesterconfig type", ftpConfigOptional.get(),
            is(equalTo(FtpHarvesterConfig.class)));
        assertThat("httpharvesterconfig type", httpConfigOptional.get(),
            is(equalTo(HttpHarvesterConfig.class)));
        assertThat("sftpharvesterconfig type", sFtpConfigOptional.get(),
                is(equalTo(SFtpHarvesterConfig.class)));

        assertThat("some large id value not present",
            harvesterConfigRepository.getHarvesterConfigType(
            Integer.MAX_VALUE).isPresent(), is(false));

    }
}
