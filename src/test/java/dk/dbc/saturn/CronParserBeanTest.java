/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CronParserBeanTest {
    @Test
    void shouldExecute_false() throws HarvestException {
        Timestamp last = Timestamp.from(Instant.parse(
            "2018-06-06T20:20:20.00Z"));
        Timestamp now = Timestamp.from(last.toInstant().plus(2,
            ChronoUnit.MINUTES));
        CronParserBean parserBean = new CronParserBean();
        assertThat(parserBean.shouldExecute("0 1 * * *", last, now),
            is(false));
    }

    @Test
    void shouldExecute_true() throws HarvestException {
        Timestamp last = Timestamp.from(Instant.parse(
            "2018-06-06T20:20:20.00Z"));
        Timestamp now = Timestamp.from(last.toInstant().plus(1,
            ChronoUnit.HOURS));
        CronParserBean parserBean = new CronParserBean();
        assertThat(parserBean.shouldExecute("30 * * * *", last, now),
            is(true));
    }

    @Test
    void describe() {
        CronParserBean parserBean = new CronParserBean();
        assertThat(parserBean.describe("3 * 22 3 *"),
            is("every hour at minute 3 at 22 day at March month"));
    }
}
