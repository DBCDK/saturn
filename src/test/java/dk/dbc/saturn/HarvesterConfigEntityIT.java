/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
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

public class HarvesterConfigEntityIT {
    private static EntityManager entityManager;

    @BeforeAll
    public static void setUp() {
        final PGSimpleDataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        entityManager = createEntityManager(dataSource,
            "saturnIT_PU");
    }

    @BeforeEach
    public void resetDatabase() {
        entityManager.getTransaction().begin();
        entityManager.createNativeQuery("DELETE FROM httpharvester");
        entityManager.createNativeQuery("DELETE FROM ftpharvester");
    }

    @Test
    public void test_httpHarvesterEntities() throws ParseException {
        HttpHarvesterConfig config1 = new HttpHarvesterConfig();
        config1.setSchedule("0 13 20 * *");
        config1.setUrl("http://skerdernogetiaarhusiaften.dk/");

        HttpHarvesterConfig config2 = new HttpHarvesterConfig();
        config2.setSchedule("1 * * * *");
        config2.setUrl("http://nick.com");
        config2.setLastHarvested(getDate("2018-06-06T20:20:20",
            "Europe/Copenhagen"));

        entityManager.persist(config1);
        entityManager.flush();
        entityManager.persist(config2);

        entityManager.getTransaction().commit();

        TypedQuery<HttpHarvesterConfig> query = entityManager
            .createNamedQuery(HttpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME,
            HttpHarvesterConfig.class);

        List<HttpHarvesterConfig> entities = query.getResultList();
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
        config1.setSchedule("0 13 20 * *");
        config1.setHost("skerdernogetiaarhusiaften.dk");
        config1.setPort(1234);
        config1.setUsername("JackSparrow");
        config1.setPassword("blackpearl");
        config1.setDir("tortuga");
        config1.setFilesPattern("treasure-map*.jpg");

        FtpHarvesterConfig config2 = new FtpHarvesterConfig();
        config2.setSchedule("1 * * * *");
        config2.setHost("nick.com");
        config2.setPort(1234);
        config2.setUsername("PatrickStar");
        config2.setPassword("uuuuh");
        config2.setDir("rock/bottom");
        config2.setFilesPattern("glove-candy.jpg");
        config2.setLastHarvested(Timestamp.from(
            Instant.ofEpochSecond(1234567)));

        entityManager.persist(config1);
        entityManager.flush();
        entityManager.persist(config2);

        entityManager.getTransaction().commit();

        TypedQuery<FtpHarvesterConfig> query = entityManager
            .createNamedQuery(FtpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME,
            FtpHarvesterConfig.class);

        List<FtpHarvesterConfig> entities = query.getResultList();
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
