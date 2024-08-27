/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.commons.testcontainers.service.DBCServiceContainer;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import static dk.dbc.saturn.TestUtils.TIME_ZONE;
import static dk.dbc.saturn.TestUtils.getDate;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SuppressWarnings("SqlResolve")
public abstract class AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);
    private static final String SFTP_IMAGE = "docker-metascrum.artifacts.dbccloud.dk/simplesftpserver:latest";
    protected static final String PASSWORDSTORE_IMAGE = "docker-metascrum.artifacts.dbccloud.dk/saturn-passwordstoresync:devel";
    public static final String SATURN_IMAGE = "docker-metascrum.artifacts.dbccloud.dk/saturn-service:devel";
    public static final String SFTP_LIST_ENDPOINT = "api/configs/sftp/list";

    public final static Network network = Network.newNetwork();
    public static final DBCPostgreSQLContainer SATURN_DB_CONTAINER = makeDBContainer();
    public static EntityManager entityManager;
    public final static HarvesterConfigRepository HARVESTER_CONFIG_REPOSITORY = new HarvesterConfigRepository();
    public final static PasswordRepository PASSWORD_REPOSITORY = new PasswordRepository();
    public final static UriBuilder MOCKED_URI_BUILDER = mock(UriBuilder.class);
    public static final SFtpContainer SFTP_CONTAINER = new SFtpContainer(SFTP_IMAGE, "sftp", "sftp", "upload").withNetwork(network).go();
    public static final DBCServiceContainer SATURN_CONTAINER = makeSaturnContainer(network, "http://localhost");
    public static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @BeforeClass
    public static void setUp() throws URISyntaxException {
        final DataSource dataSource = SATURN_DB_CONTAINER.datasource();
        migrateDatabase(dataSource);
        entityManager = createEntityManager(SATURN_DB_CONTAINER, "saturnIT_PU");
        HARVESTER_CONFIG_REPOSITORY.entityManager = entityManager;
        PASSWORD_REPOSITORY.entityManager = entityManager;
        when(MOCKED_URI_BUILDER.path(anyString())).thenReturn(MOCKED_URI_BUILDER);
        when(MOCKED_URI_BUILDER.build()).thenReturn(new URI("location"));
    }

    @Before
    public void beginTransaction() {
        HARVESTER_CONFIG_REPOSITORY.entityManager.getTransaction().begin();
    }

    @After
    public void resetDatabase() {
        if(HARVESTER_CONFIG_REPOSITORY.entityManager.getTransaction().isActive()) {
            HARVESTER_CONFIG_REPOSITORY.entityManager.getTransaction().commit();
        }
        HARVESTER_CONFIG_REPOSITORY.entityManager.getTransaction().begin();
        HARVESTER_CONFIG_REPOSITORY.entityManager.createNativeQuery("DELETE FROM httpharvester").executeUpdate();
        HARVESTER_CONFIG_REPOSITORY.entityManager.createNativeQuery("DELETE FROM ftpharvester").executeUpdate();
        HARVESTER_CONFIG_REPOSITORY.entityManager.createNativeQuery("DELETE FROM sftpharvester").executeUpdate();
        HARVESTER_CONFIG_REPOSITORY.entityManager.createNativeQuery("DELETE FROM passwords").executeUpdate();
        HARVESTER_CONFIG_REPOSITORY.entityManager.getTransaction().commit();
    }

    private static EntityManager createEntityManager(DBCPostgreSQLContainer dbContainer, String persistenceUnitName) {
        Map<String, String> entityManagerProperties = dbContainer.entityManagerProperties();
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName, entityManagerProperties);
        return factory.createEntityManager(entityManagerProperties);
    }

    private static void migrateDatabase(DataSource datasource) {
        final DatabaseMigrator migrator = new DatabaseMigrator(datasource);
        migrator.migrate();
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
        config.setUsername(SFTP_CONTAINER.user);
        config.setPassword(SFTP_CONTAINER.password);
        config.setDir(SFTP_CONTAINER.dir);
        config.setFilesPattern("*");
        config.setLastHarvested(getDate("2018-06-06T20:20:20"));
        config.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
                "clatin-1,o=littsiden,m=kildepost@dbc.dk");
        config.setAgency("010100");
        config.setEnabled(true);
        return config;
    }

    public static String  getOclcDate(Date date)  {
        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");
        sdf.setTimeZone(TIME_ZONE);
        return sdf.format(date);
    }

    public static Date getDatePlusDays(int days) {
        return Date.from(Instant.now().plus(days, ChronoUnit.DAYS));
    }

    public static Date getDateMinusDays(int days) {
        return Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
    }

    public static DBCServiceContainer makeSaturnContainer(Network network, String filestore) {
        DBCServiceContainer container = new DBCServiceContainer(SATURN_IMAGE)
                .withNetworkAliases("saturn")
                .withNetwork(network)
                .withExposedPorts(8080)
                .withEnv("JAVA_MAX_HEAP_SIZE",  "1G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("TZ", "Europe/Copenhagen")
                .withEnv("DB_URL", SATURN_DB_CONTAINER.getPayaraDockerJdbcUrl())
                .withEnv("FILESTORE_URL", filestore)
                .withEnv("PROXY_HOSTNAME","<none>")
                .withEnv("PROXY_USERNAME", "<none>")
                .withEnv("PROXY_PASSWORD", "<none>")
                .waitingFor(Wait.forHttp("/health/ready"))
                .withStartupTimeout(Duration.ofSeconds(30));
        container.start();
        return container;
    }

    private static DBCPostgreSQLContainer makeDBContainer() {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withReuse(false);
        container.start();
        container.exposeHostPort();
        LOGGER.info("Postgres url is:{}", container.getDockerJdbcUrl());
        return container;
    }

}
