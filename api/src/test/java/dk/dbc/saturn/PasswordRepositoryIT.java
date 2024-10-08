package dk.dbc.saturn;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.saturn.api.PasswordEntryFrontEnd;
import dk.dbc.saturn.entity.PasswordEntry;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.Transferable;

import java.text.ParseException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dk.dbc.saturn.DateTimeUtil.LOCAL_DATE_TIME_FORMATTER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PasswordRepositoryIT extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordRepositoryIT.class);

    final OffsetDateTime tomorrow = getDatePlusDays(1);
    final OffsetDateTime yesterday = getDateMinusDays(1);
    final OffsetDateTime dayBeforeYesterday = getDateMinusDays(2);
    final OffsetDateTime earlier = getDateMinusDays(62);
    final OffsetDateTime muchEarlier = getDateMinusDays(90);
    final OffsetDateTime later = getDatePlusDays(41);
    final OffsetDateTime today = OffsetDateTime.now();

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
            PASSWORD_REPOSITORY.save(entry);
        }
        PASSWORD_REPOSITORY.entityManager.getTransaction().commit();
    }

    @Test
    public void test_list_passwords() {
        List<PasswordEntry> actualList = PASSWORD_REPOSITORY.list("host-1", "user-1", 5);
        assertThat("Length of returned list", actualList.size(), is(5));
    }

    @Test
    public void test_that_returned_current_password_is_the_one_valid_from_yesterday() {
        PasswordEntry expected = getPasswordEntry(1, 1, 2, yesterday);
        PasswordEntry actual = PASSWORD_REPOSITORY.getPasswordForDate("host-1", "user-1", today);
        expected.setId(actual.getId());
        assertThat("Current password-entry is the one with date yesterday", actual, is(expected));
    }

    @Test
    public void test_that_a_password_entry_can_be_removed() {
        PasswordEntry oldEntry = PASSWORD_REPOSITORY.getPasswordForDate("host-1", "user-2", earlier);
        PASSWORD_REPOSITORY.delete(oldEntry.getId());
        assertThat("No password entry can be found for dates more than 62 days old",
                PASSWORD_REPOSITORY.getPasswordForDate("host-1", "user-1", earlier), nullValue());
    }

    @Test @Ignore("Can't connect to saturn service for some reason")
    public void test_that_a_new_password_with_valid_from_date_can_be_added() throws ParseException, JSONBException {
        persistASFtpConfig();
        makeAPasswordListFileAndUpload();

        // Renew registered passwords by fetching list from the sftp host.
        runPasswordFetchBatchJob();

        PasswordEntryFrontEnd passwordEntryFrontEnd = getPasswordFromApi("sftp", "sftp", ZonedDateTime.now().format(LOCAL_DATE_TIME_FORMATTER));
        assertThat("Found an updated password", passwordEntryFrontEnd.getPassword(),
                                is("c29tZS1wYXNzd29yZDp3aXRoQHZhcmlvdXMsc3ltYm9scw=="));

        // Now new password should be in effect at config: host=sftp, user=sftp
        List<SFtpHarvesterConfig> sFtpHarvesterConfigs = getSftpHarvesterConfigs();
        SFtpHarvesterConfig sfc = sFtpHarvesterConfigs.stream().filter(c ->
                "sftp".equals(c.getHost()) &&
                "sftp".equals(c.getUsername())).findFirst().orElseThrow();
        assertThat("Found updated password", sfc.getPassword(), is("some-password:with@various,symbols"));
        SATURN_CONTAINER.stop();
    }

    private PasswordEntryFrontEnd getPasswordFromApi(String host, String user, String date) throws JSONBException {
        Response response = SATURN_CONTAINER.httpGet().withPathElements("api/passwordrepository", host, user, date).execute();
        return JSONB_CONTEXT.unmarshall(response.readEntity(String.class), PasswordEntryFrontEnd.class);
    }

    private List<SFtpHarvesterConfig> getSftpHarvesterConfigs() throws JSONBException {
        final CollectionType collectionType = JSONB_CONTEXT.getTypeFactory().constructCollectionType(List.class, SFtpHarvesterConfig.class);
        Response response = SATURN_CONTAINER.httpGet().withPathElements(SFTP_LIST_ENDPOINT).execute();
        return JSONB_CONTEXT.unmarshall(response.readEntity(String.class), collectionType);
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
        String user = SFTP_CONTAINER.user;
        SFTP_CONTAINER.copyFileToContainer(Transferable.of(makeAPasswordList()), String.format("/home/%s/%s", user, user));
    }

    void persistASFtpConfig() throws ParseException {
        SFtpHarvesterConfig sFtpHarvesterConfig = getSFtpHarvesterConfig();
        PASSWORD_REPOSITORY.entityManager.getTransaction().begin();
        PASSWORD_REPOSITORY.entityManager.persist(sFtpHarvesterConfig);
        PASSWORD_REPOSITORY.entityManager.flush();
        PASSWORD_REPOSITORY.entityManager.getTransaction().commit();
    }

    void runPasswordFetchBatchJob() {
        LOGGER.info("Running passwordJob");

        try (GenericContainer<?> passwordBatchContainer = new GenericContainer<>(PASSWORDSTORE_IMAGE)) {
            passwordBatchContainer.withCommand("sh", "-c", "sleep 2 && src/python/main/main.py")
                    .withNetwork(network)
                    .withNetworkAliases("passwordChanger")
                    .withEnv("PASSWORD_CHANGE_ENABLED_SFTP_HOSTS", String.format("[\"%s\"]", "sftp"))
                    .withEnv("SATURN_REST_ENDPOINT", SATURN_CONTAINER.getServiceBaseUrl() + "/api")
                    .withEnv("PROXY_PORT", "-1")
                    .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(20)));
            passwordBatchContainer.start();
            String logs = passwordBatchContainer.getLogs();
            LOGGER.info("LOGS:" + logs);
            LOGGER.info("DONE Running passwordJob");
            passwordBatchContainer.stop();
        }
    }

    private static PasswordEntry getPasswordEntry(int hostnr, int usrnr, int passwdnr, OffsetDateTime date) {
        PasswordEntry entry = new PasswordEntry();
        entry.setHost(hostnr > 0?String.format("host-%d", hostnr): "sftp");
        entry.setUsername(usrnr>0 ?String.format("user-%d", usrnr): SFTP_CONTAINER.user);
        entry.setPassword(passwdnr>0?String.format("password-%d", passwdnr): SFTP_CONTAINER.password);
        entry.setActiveFrom(date);
        return entry;
    }

}
