package dk.dbc.saturn;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.testcontainers.service.DBCServiceContainer;
import dk.dbc.saturn.api.PasswordEntryFrontEnd;
import dk.dbc.saturn.entity.PasswordEntry;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dk.dbc.saturn.api.PasswordRepositoryApi.sdf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PasswordRepositoryIT extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordRepositoryIT.class);
    private final DBCServiceContainer saturnService = makeSaturnContainer();

    final Date tomorrow = getDatePlusDays(1);
    final Date yesterday = getDateMinusDays(1);
    final Date dayBeforeYesterday = getDateMinusDays(2);
    final Date earlier = getDateMinusDays(62);
    final Date muchEarlier = getDateMinusDays(90);
    final Date later = getDatePlusDays(41);
    final Date today = new Date();

    @Before
    public void populate_db() {
        List<PasswordEntry> entries = Arrays.asList(
                getPasswordEntry(1, 1, 1, tomorrow),
                getPasswordEntry(1, 1, 2, yesterday),
                getPasswordEntry(1, 1, 3, dayBeforeYesterday),
                getPasswordEntry(1, 1, 4, earlier),
                getPasswordEntry(1, 1, 5, later),
                getPasswordEntry(1, 2, 1, later),
                getPasswordEntry(1, 2, 1, muchEarlier),
                getPasswordEntry(2, 1, 1, dayBeforeYesterday),
                getPasswordEntry(0, 0, 0, dayBeforeYesterday)
                );
        for (PasswordEntry entry : entries) {
            passwordRepository.save(entry);
        }
//        passwordRepository.entityManager.flush();
        passwordRepository.entityManager.getTransaction().commit();
    }

    @Test
    public void test_list_passwords() {
        List<PasswordEntry> actualList = passwordRepository.list("host-1", "user-1", 5);
        assertThat("Length of returned list", actualList.size(), is(5));
    }

    @Test
    public void test_that_returned_current_password_is_the_one_valid_from_yesterday() {
        PasswordEntry expected = getPasswordEntry(1, 1, 2, yesterday);
        PasswordEntry actual = passwordRepository.getPasswordForDate("host-1", "user-1", today);
        expected.setId(actual.getId());
        assertThat("Current password-entry is the one with date yesterday", actual, is(expected));
    }

    @Test
    public void test_that_a_password_entry_can_be_removed() {
        PasswordEntry oldEntry = passwordRepository.getPasswordForDate("host-1", "user-2", earlier);
        passwordRepository.delete(oldEntry.getId());
        assertThat("No password entry can be found for dates more than 62 days old",
                passwordRepository.getPasswordForDate("host-1", "user-1", earlier), nullValue());
    }

    @Test
    public void test_that_a_new_password_with_valid_from_date_can_be_added() throws ParseException, InterruptedException, JSONBException {
        persistASFtpConfig();
        makeAPasswordListFileAndUpload();

        // Renew registered passwords by fetching list from the sftp host.
        runPasswordFetchBatchJob();

        PasswordEntryFrontEnd passwordEntryFrontEnd = getPasswordFromApi("sftp", "sftp", sdf.format(Date.from(Instant.now())));
        assertThat("Found an updated password", passwordEntryFrontEnd.getPassword(),
                                is("c29tZS1wYXNzd29yZDp3aXRoQHZhcmlvdXMsc3ltYm9scw=="));

        // Now new password should be in effect at config: host=sftp, user=sftp
        List<SFtpHarvesterConfig> sFtpHarvesterConfigs = getSftpHarvesterConfigs();
        SFtpHarvesterConfig sfc = sFtpHarvesterConfigs.stream().filter(c ->
                "sftp".equals(c.getHost()) &&
                "sftp".equals(c.getUsername())).findFirst().orElseThrow();
        assertThat("Found updated password", sfc.getPassword(), is("some-password:with@various,symbols"));
    }

    private PasswordEntryFrontEnd getPasswordFromApi(String host, String user, String date) throws JSONBException {
        try(Response response = getHttp(String.format(PASSWORDREPO_GET_PASSWORD, host, user, date))) {
            return jsonbContext.unmarshall(response.readEntity(String.class), PasswordEntryFrontEnd.class);
        }
    }

    private List<SFtpHarvesterConfig> getSftpHarvesterConfigs() throws JSONBException {
        final CollectionType collectionType = jsonbContext.getTypeFactory()
                .constructCollectionType(List.class, SFtpHarvesterConfig.class);
        Response response = getHttp(String.format(SFTP_LIST_ENDPOINT));
        return jsonbContext.unmarshall(response.readEntity(String.class), collectionType);
    }
    String makeAPasswordList() {
        return Map.of(
                        getOclcDate(yesterday), "some-password:with@various,symbols",
                        getOclcDate(tomorrow), "some-other-ÆEH-password:with@various,symbols",

                        // Silly small fix, to allow the project to be tested on the second of each month.
                        getOclcDate(getDateMinusDays(3)), "password-for-sftp-user:Å"
                )
                .entrySet()
                .stream()
                .map(theDate -> String.format("%-13s", theDate.getKey()) + "-   " + theDate.getValue())
                .collect(Collectors.joining("\n"));
    }

    void makeAPasswordListFileAndUpload() {
        sftpServerContainer.copyFileToContainer(Transferable.of(makeAPasswordList()), String.format("/home/%s/%s", SFTP_USER, SFTP_USER));
    }

    void persistASFtpConfig() throws ParseException {
        SFtpHarvesterConfig sFtpHarvesterConfig = getSFtpHarvesterConfig();
        passwordRepository.entityManager.getTransaction().begin();
        passwordRepository.entityManager.persist(sFtpHarvesterConfig);
        passwordRepository.entityManager.flush();
        passwordRepository.entityManager.getTransaction().commit();
    }

    void runPasswordFetchBatchJob() {
        LOGGER.info("Running passwordJob");

        try (GenericContainer passwordBatchContainer = new GenericContainer<>(PASSWORDSTORE_IMAGE)) {
            passwordBatchContainer.withCommand("sh", "-c", "sleep 2 && src/python/main/main.py")
                    .withNetwork(network)
                    .withNetworkAliases("passwordChanger")
                    .withEnv("PASSWORD_CHANGE_ENABLED_SFTP_HOSTS", String.format("[\"%s\"]", "sftp"))
                    .withEnv("SATURN_REST_ENDPOINT", "http://saturn:8080/api")
                    .withEnv("PROXY_PORT", "-1")
                    .withStartupCheckStrategy(
                            new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(20)));
            passwordBatchContainer.start();
            String logs = passwordBatchContainer.getLogs();
            LOGGER.info("LOGS:" + logs);
            LOGGER.info("DONE Running passwordJob");
            passwordBatchContainer.stop();
        }
    }


    private static PasswordEntry getPasswordEntry(int hostnr, int usrnr, int passwdnr, Date date) {
        PasswordEntry entry = new PasswordEntry();
        entry.setHost(hostnr > 0?String.format("host-%d", hostnr): "sftp");
        entry.setUsername(usrnr>0 ?String.format("user-%d", usrnr): SFTP_USER);
        entry.setPassword(passwdnr>0?String.format("password-%d", passwdnr):SFTP_PASSWORD);
        entry.setActiveFrom(date);
        return entry;
    }

    public Response getHttp(String path) {
        LOGGER.info("Request: {}/{}", saturnService.getServiceBaseUrl(), path);
        return saturnService.httpGet().withPathElements(path).execute();
    }

    private DBCServiceContainer makeSaturnContainer() {
        DBCServiceContainer container = new DBCServiceContainer(SATURN_IMAGE)
                .withNetworkAliases("saturn")
                .withNetwork(network)
                .withExposedPorts(8080)
                .withEnv("JAVA_MAX_HEAP_SIZE",  "1G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("TZ", "Europe/Copenhagen")
                .withEnv("DB_URL", saturnDBContainer.getPayaraDockerJdbcUrl())
                .withEnv("PROXY_HOSTNAME","<none>")
                .withEnv("PROXY_USERNAME", "<none>")
                .withEnv("PROXY_PASSWORD", "<none>")
                .withEnv("FILESTORE_URL", "http://localhost:8080/no")
                .withEnv("JOBSTORE_URL", "http://localhost:8080/no")
                .waitingFor(Wait.forHttp("/health/ready"))
                .withStartupTimeout(Duration.ofSeconds(30));
        container.start();
        return container;
    }
}
