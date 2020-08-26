package dk.dbc.saturn;

import dk.dbc.saturn.entity.PasswordEntry;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.MatcherAssert.assertThat;

public class PasswordRepositoryIT extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            PasswordRepositoryIT.class);

    final Date tomorrow = getDatePlusDays(1);
    final Date yesterday = getDateMinusDays(1);
    final Date dayBeforeYesterday = getDateMinusDays(2);
    final Date earlier = getDateMinusDays(62);
    final Date muchEarlier = getDateMinusDays(90);
    final Date later = getDatePlusDays(41);
    final Date today = new Date();

    @BeforeEach
    void populate_db() {
        List<PasswordEntry> entries = Arrays.asList(
                getPasswordEntry(1, 1, 1, tomorrow),
                getPasswordEntry(1, 1, 2, yesterday),
                getPasswordEntry(1, 1, 3, dayBeforeYesterday),
                getPasswordEntry(1, 1, 4, earlier),
                getPasswordEntry(1, 1, 5, later),
                getPasswordEntry(1, 2, 1, later),
                getPasswordEntry(1, 2, 1, muchEarlier),
                getPasswordEntry(2, 1, 1, dayBeforeYesterday));
        for (PasswordEntry entry : entries) {
            passwordRepository.save(entry);
        }
        passwordRepository.entityManager.flush();
        passwordRepository.entityManager.getTransaction().commit();
    }

    @Test
    void test_list_passwords() {
        List<PasswordEntry> actualList = passwordRepository.list("host-1", "user-1", 5);
        for (PasswordEntry entry : actualList) {
            LOGGER.info("  => {} {} {}", entry.getHost(), entry.getUsername(), entry.getActiveFrom());
        }
        assertThat("Length of returned list", actualList.size(), is(5));
    }

    @Test
    void test_that_returned_current_password_is_the_one_valid_from_yesterday() {
        PasswordEntry expected = getPasswordEntry(1, 1, 2, yesterday);
        PasswordEntry actual = passwordRepository.getPasswordForDate("host-1", "user-1", today);
        assertThat("Current password-entry is the one with date yesterday", actual, is(expected));
    }

    @Test
    void test_that_a_password_entry_can_be_removed() {
        PasswordEntry oldEntry = passwordRepository.getPasswordForDate("host-1", "user-2", earlier);
        passwordRepository.delete(oldEntry.getId());
        assertThat("No password entry can be found for dates more than 62 days old",
                passwordRepository.getPasswordForDate("host-1", "user-1", earlier), nullValue());
    }

    private PasswordEntry getPasswordEntry(int hostnr, int usrnr, int passwdnr, Date date) {
        PasswordEntry entry = new PasswordEntry();
        entry.setHost(String.format("host-%d", hostnr));
        entry.setUsername(String.format("user-%d", usrnr));
        entry.setPassword(String.format("password-%d", passwdnr));
        entry.setActiveFrom(date);
        return entry;
    }
}
