/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.sftpclient.SFTPConfig;
import dk.dbc.commons.sftpclient.SFtpClient;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public abstract class AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            AbstractIntegrationTest.class);
    protected static final HttpClient httpClient;
    static final EmbeddedPostgres pg = pgStart();
    static  {
        Testcontainers.exposeHostPorts(pg.getPort());
        httpClient = HttpClient.create(HttpClient.newClient());
    }
    protected static EntityManager entityManager;
    final static HarvesterConfigRepository harvesterConfigRepository =
        new HarvesterConfigRepository();
    final static PasswordRepository passwordRepository =
         new PasswordRepository();
    final static UriBuilder mockedUriBuilder = mock(UriBuilder.class);
    final static Network network;
    static final GenericContainer sftpServerContainer;
    protected static final String PASSWORDSTORE_IMAGE = "docker-metascrum.artifacts.dbccloud.dk/saturn-passwordstoresync:devel";
    protected static final String PASSWORDSTORE_IMAGE1 = "docker-metascrum.artifacts.dbccloud.dk/saturn-passwordstoresync:devel1";
    private static final String SFTPSERVER_IMAGE = "docker-metascrum.artifacts.dbccloud.dk/simplesftpserver:latest";
    static final String SATURN_IMAGE = "docker-metascrum.artifacts.dbccloud.dk/saturn-service:devel";
    static GenericContainer saturnContainer;
    static final String SFTP_USER = "sftp";
    static final String SFTP_PASSWORD = "sftp";
    static final String SFTP_DIR = "upload";
    static final String SFTP_ADDRESS;
    static final int SFTP_PORT;
    static String TIME_ZONE = "Europe/Copenhagen";
    static final String SATURN_BASE_URL;
    static final String PASSWORDREPO_GET_PASSWORD = "api/passwordrepository/%s/%s/%s";
    protected static final String SFTP_LIST_ENDPOINT = "api/configs/sftp/list";

    private static EmbeddedPostgres pgStart() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static {
        network = Network.newNetwork();
        sftpServerContainer = new GenericContainer(SFTPSERVER_IMAGE)
                .withNetworkAliases("sftp")
                .withNetwork(network)
                .withExposedPorts(22)
                .withCommand(String.format("%s:%s:::%s", SFTP_USER, SFTP_PASSWORD, SFTP_DIR))
                .withStartupTimeout(Duration.ofMinutes(1));
        sftpServerContainer.start();
        SFTP_ADDRESS = sftpServerContainer.getHost();
        SFTP_PORT = sftpServerContainer.getMappedPort(22);
        startSaturnContainer();
         SATURN_BASE_URL = "http://" + saturnContainer.getHost() + ":" + saturnContainer.getMappedPort(8080);
    }
    protected JSONBContext jsonbContext = new JSONBContext();

    @BeforeAll
    public static void setUp() throws URISyntaxException {
        final PGSimpleDataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        entityManager = createEntityManager(dataSource,
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
                "DELETE FROM sftpharvester").executeUpdate();
        harvesterConfigRepository.entityManager.createNativeQuery(
                "DELETE FROM passwords").executeUpdate();
        harvesterConfigRepository.entityManager.getTransaction().commit();
    }

    static PGSimpleDataSource getDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setURL( pg.getJdbcUrl("postgres", "postgres"));
        datasource.setUser("postgres");
        datasource.setPassword("");
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
        config.setLastHarvested(getDate("2018-06-06T20:20:20"));
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
        config.setLastHarvested(getDate("2018-06-06T20:20:20"));
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
        config.setHost("sftp");
        config.setPort(22);
        config.setUsername(SFTP_USER);
        config.setPassword(SFTP_PASSWORD);
        config.setDir("upload");
        config.setFilesPattern("*");
        config.setLastHarvested(getDate("2018-06-06T20:20:20"));
        config.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
                "clatin-1,o=littsiden,m=kildepost@dbc.dk");
        config.setAgency("010100");
        config.setEnabled(true);
        return config;
    }

    public static Date getDate(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
        return sdf.parse(date);
    }
    public static String  getOclcDate(Date date)  {
        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
        return sdf.format(date);
    }
    public static Date getDatePlusDays(int days) {
        return Date.from(Instant.now().plus(days, ChronoUnit.DAYS));
    }
    public static Date getDateMinusDays(int days) {
        return Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
    }
    public static Date getDateFirstOfThisMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Date.from(Instant.now()));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }
    SFtpClient getSftpClient() {
        return new SFtpClient(
                new SFTPConfig()
                        .withHost(SFTP_ADDRESS)
                        .withUsername(SFTP_USER)
                        .withPassword(SFTP_PASSWORD)
                        .withPort(SFTP_PORT)
                        .withDir("upload")
                        .withFilesPattern("*"), null);
    }

    public Response getHttp(String path) {
        LOGGER.info("Request: {}/{}", SATURN_BASE_URL, path);
        return new HttpGet(httpClient).withBaseUrl(SATURN_BASE_URL).withPathElements(path).execute();
    }

    private static void startSaturnContainer() {
        saturnContainer = new GenericContainer(SATURN_IMAGE)
                .withNetworkAliases("saturn")
                .withNetwork(network)
                .withExposedPorts(8080)
                .withEnv("JAVA_MAX_HEAP_SIZE",  "8G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("TZ", "Europe/Copenhagen")
                .withEnv("DB_URL", String.format("postgres:@host.testcontainers.internal:%d/postgres", getDataSource().getPortNumbers()[0]))
                .withEnv("PROXY_HOSTNAME","<none>")
                .withEnv("PROXY_USERNAME", "<none>")
                .withEnv("PROXY_PASSWORD", "<none>")
                .waitingFor(Wait.forHttp("/health/ready"))
                .withStartupTimeout(Duration.ofSeconds(30));
        saturnContainer.start();
    }
}
