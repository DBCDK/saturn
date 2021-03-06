/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public abstract class AbstractIntegrationTest {
    final static HarvesterConfigRepository harvesterConfigRepository =
        new HarvesterConfigRepository();
    final static PasswordRepository passwordRepository =
         new PasswordRepository();
    final static UriBuilder mockedUriBuilder = mock(UriBuilder.class);
    private static final GenericContainer sftpServerContainer;
    private static final String SFTPSERVER_IMAGE = "docker.dbc.dk/simplesftpserver:latest";
    static final String SFTP_USER = "sftp";
    static final String SFTP_PASSWORD = "sftp";
    static final String SFTP_DIR = "upload";
    static final String SFTP_ADDRESS;
    static final int SFTP_PORT;

    static {
        Network network = Network.newNetwork();
        sftpServerContainer = new GenericContainer(SFTPSERVER_IMAGE)
                .withNetwork(network)
                .withExposedPorts(22)
                .withCommand(String.format("%s:%s:::%s", SFTP_USER, SFTP_PASSWORD, SFTP_DIR))
                .withStartupTimeout(Duration.ofMinutes(1));
        sftpServerContainer.start();
        SFTP_ADDRESS = sftpServerContainer.getContainerIpAddress();
        SFTP_PORT = sftpServerContainer.getMappedPort(22);
    }


    @BeforeAll
    public static void setUp() throws URISyntaxException {
        final PGSimpleDataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        EntityManager entityManager = createEntityManager(dataSource,
            "saturnIT_PU");
        harvesterConfigRepository.entityManager = entityManager;
        passwordRepository.entityManager = entityManager;
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));
    }

    @BeforeEach
    void beginTransaction() {
        harvesterConfigRepository.entityManager.getTransaction().begin();
    }

    @AfterEach
    void resetDatabase() {
        if(harvesterConfigRepository.entityManager.getTransaction().isActive()) {
            harvesterConfigRepository.entityManager.getTransaction().commit();
        }
        harvesterConfigRepository.entityManager.getTransaction().begin();
        harvesterConfigRepository.entityManager.createNativeQuery(
            "DELETE FROM httpharvester").executeUpdate();
        harvesterConfigRepository.entityManager.createNativeQuery(
            "DELETE FROM ftpharvester").executeUpdate();
        harvesterConfigRepository.entityManager.createNativeQuery(
                "DELETE FROM passwords").executeUpdate();
        harvesterConfigRepository.entityManager.getTransaction().commit();
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

    private static void migrateDatabase(PGSimpleDataSource datasource) {
        final DatabaseMigrator migrator = new DatabaseMigrator(datasource);
        migrator.migrate();
    }

    public static HttpHarvesterConfig getHttpHarvesterConfig() throws ParseException {
        HttpHarvesterConfig config = new HttpHarvesterConfig();
        config.setName("MyName'sNotRick!");
        config.setSchedule("* * * * *");
        config.setUrl("http://nick.com");
        config.setLastHarvested(getDate("2018-06-06T20:20:20",
            "Europe/Copenhagen"));
        config.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "clatin-1,o=littsiden,m=kildepost@dbc.dk");
        config.setAgency("010100");
        config.setId(1);
        config.setEnabled(true);
        return config;
    }

    FtpHarvesterConfig getFtpHarvesterConfig() throws ParseException {
        FtpHarvesterConfig config = new FtpHarvesterConfig();
        config.setName("MyName'sNotRick!");
        config.setSchedule("1 * * * *");
        config.setHost("http://nick.com");
        config.setPort(5432);
        config.setUsername("patrick-squarepants");
        config.setPassword("secretpants");
        config.setDir("rock-bottom");
        config.setFilesPattern("glove-candy.png");
        config.setLastHarvested(getDate("2018-06-06T20:20:20",
            "Europe/Copenhagen"));
        config.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
            "clatin-1,o=littsiden,m=kildepost@dbc.dk");
        config.setAgency("010100");
        config.setEnabled(true);
        return config;
    }

    SFtpHarvesterConfig getSFtpHarvesterConfig() throws ParseException {
        SFtpHarvesterConfig config = new SFtpHarvesterConfig();
        config.setName("MyName'sNotRick!");
        config.setSchedule("1 * * * *");
        config.setHost("http://nick.com");
        config.setPort(5432);
        config.setUsername("patrick-squarepants");
        config.setPassword("secretpants");
        config.setDir("rock-bottom");
        config.setFilesPattern("glove-candy.png");
        config.setLastHarvested(getDate("2018-06-06T20:20:20",
                "Europe/Copenhagen"));
        config.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
                "clatin-1,o=littsiden,m=kildepost@dbc.dk");
        config.setAgency("010100");
        config.setEnabled(true);
        return config;
    }

    public static Date getDate(String date, String timezone) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf.parse(date);
    }

    public static Date getDatePlusDays(int days) {
        return Date.from(Instant.now().plus(days, ChronoUnit.DAYS));
    }

    public static Date getDateMinusDays(int days) {
        return Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
    }
}
