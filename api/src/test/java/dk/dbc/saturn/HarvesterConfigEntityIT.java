/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterConfigEntityIT {
    private static HarvesterConfigRepository harvesterConfigRepository =
        new HarvesterConfigRepository();
    private final static UriBuilder mockedUriBuilder = mock(UriBuilder.class);

    @BeforeAll
    public static void setUp() throws URISyntaxException {
        final PGSimpleDataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        EntityManager entityManager = createEntityManager(dataSource,
            "saturnIT_PU");
        harvesterConfigRepository.entityManager = entityManager;
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));
    }

    @BeforeEach
    void beginTransaction() {
        harvesterConfigRepository.entityManager.getTransaction().begin();
    }

    @AfterEach
    void resetDatabase() {
        harvesterConfigRepository.entityManager.getTransaction().begin();
        harvesterConfigRepository.entityManager.createNativeQuery(
            "DELETE FROM httpharvester").executeUpdate();
        harvesterConfigRepository.entityManager.createNativeQuery(
            "DELETE FROM ftpharvester").executeUpdate();
        harvesterConfigRepository.entityManager.getTransaction().commit();
    }

    @Test
    public void test_httpHarvesterEntities() throws ParseException {
        HttpHarvesterConfig config1 = new HttpHarvesterConfig();
        config1.setName("Patar");
        config1.setSchedule("0 13 20 * *");
        config1.setUrl("http://skerdernogetiaarhusiaften.dk/");
        config1.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk");

        HttpHarvesterConfig config2 = new HttpHarvesterConfig();
        config2.setName("MyName'sNotRick!");
        config2.setSchedule("1 * * * *");
        config2.setUrl("http://nick.com");
        config2.setLastHarvested(getDate("2018-06-06T20:20:20",
            "Europe/Copenhagen"));
        config2.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "c=latin-1,o=littsiden,m=kildepost@dbc.dk");

        harvesterConfigRepository.entityManager.persist(config1);
        harvesterConfigRepository.entityManager.flush();
        harvesterConfigRepository.entityManager.persist(config2);

        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<HttpHarvesterConfig> entities = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);

        assertThat("results", entities.size(), is(2));
        HttpHarvesterConfig result1 = entities.get(0);
        assertThat("result 1 url", result1.getUrl(), is("http://nick.com"));
        assertThat("result 1 schedule", result1.getSchedule(),
            is("1 * * * *"));
        assertThat("result 1 last harvested", result1.getLastHarvested(),
            is(getDate("2018-06-06T20:20:20", "Europe/Copenhagen")));
        assertThat("result 1 last harvested wrong time zone",
            result1.getLastHarvested(), not(getDate("2018-06-06T20:20:20",
            "Europe/London")));

        HttpHarvesterConfig result2 = entities.get(1);
        assertThat("result 2 url", result2.getUrl(),
            is("http://skerdernogetiaarhusiaften.dk/"));
        assertThat("result 2 schedule", result2.getSchedule(),
            is("0 13 20 * *"));
        assertThat("result 2 last harvested", result2.getLastHarvested(),
            is(nullValue()));
    }

    @Test
    public void test_ftpHarvesterEntities() {
        FtpHarvesterConfig config1 = new FtpHarvesterConfig();
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

        FtpHarvesterConfig config2 = new FtpHarvesterConfig();
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

        harvesterConfigRepository.entityManager.persist(config1);
        harvesterConfigRepository.entityManager.flush();
        harvesterConfigRepository.entityManager.persist(config2);

        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<FtpHarvesterConfig> entities = harvesterConfigRepository
            .list(FtpHarvesterConfig.class, 0, 0);
        assertThat("results", entities.size(), is(2));

        FtpHarvesterConfig result1 = entities.get(0);
        assertThat("result 1 host", result1.getHost(), is("nick.com"));
        assertThat("result 1 port", result1.getPort(), is(1234));
        assertThat("result 1 username", result1.getUsername(), is("PatrickStar"));
        assertThat("result 1 password", result1.getPassword(), is("uuuuh"));
        assertThat("result 1 dir", result1.getDir(), is("rock/bottom"));
        assertThat("result 1 files pattern", result1.getFilesPattern(),
            is("glove-candy.jpg"));
        assertThat("result 1 schedule", result1.getSchedule(), is("1 * * * *"));
        assertThat("result 1 last harvested", result1.getLastHarvested(),
            is(Timestamp.from(Instant.ofEpochSecond(1234567))));

        FtpHarvesterConfig result2 = entities.get(1);
        assertThat("result 2 last harvested", result2.getLastHarvested(),
            is(nullValue()));
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
            harvesterConfigRepository.add(HttpHarvesterConfig.class,
                httpHarvesterConfig, mockedUriBuilder);
        }

        harvesterConfigRepository.entityManager.getTransaction().commit();

        List<HttpHarvesterConfig> configs = harvesterConfigRepository
            .list(HttpHarvesterConfig.class, 0, 0);

        assertThat("list size", configs.size(), is(5));
        assertThat("entity 1 url", configs.get(0).getUrl(), is("gary"));
        assertThat("entity 2 schedule", configs.get(1).getSchedule(),
            is("larry"));
        assertThat("entity 3 transfile", configs.get(2).getTransfile(),
            is("squidward"));
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

    private static PGSimpleDataSource getDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("saturn");
        datasource.setServerName("localhost");
        datasource.setPortNumber(Integer.parseInt(System.getProperty(
            "postgresql.port", "5432")));
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
        return datasource;
    }

    private static void migrateDatabase(PGSimpleDataSource datasource) {
        final DatabaseMigrator migrator = new DatabaseMigrator(datasource);
        migrator.migrate();
    }

    private static EntityManager createEntityManager(
        PGSimpleDataSource dataSource, String persistenceUnitName) {
        Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put(JDBC_USER, dataSource.getUser());
        entityManagerProperties.put(JDBC_PASSWORD, dataSource.getPassword());
        entityManagerProperties.put(JDBC_URL, dataSource.getUrl());
        entityManagerProperties.put(JDBC_DRIVER, "org.postgresql.Driver");
        entityManagerProperties.put("eclipselink.logging.level", "FINE");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName,
            entityManagerProperties);
        return factory.createEntityManager(entityManagerProperties);
    }

    private Date getDate(String date, String timezone) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf.parse(date);
    }
}