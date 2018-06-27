/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TimeZone;

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

    // this test simulates the situation where there is no last harvested
    // timestamp in the database
    @Test
    void shouldExecute_noLastExecution() throws HarvestException, ParseException {
        Date last = null;
        Date now = getDate("2018-06-06T20:30:00.00Z");
        CronParserBean parserBean = new CronParserBean();
        assertThat(parserBean.shouldExecute("30 * * * *", last, now),
            is(true));
    }

    @Test
    void shouldExecute_noLastExecutionNoTrigger() throws HarvestException, ParseException {
        Date last = null;
        Date now = getDate("2018-06-06T20:20:00.00Z");
        CronParserBean parserBean = new CronParserBean();
        assertThat(parserBean.shouldExecute("30 * * * *", last, now),
            is(false));
    }

    @Test
    void shouldExecute_triggerDaily() throws HarvestException, ParseException {
        Date last = getDate("2018-06-05T20:20:00.00Z");
        Date now = getDate("2018-06-06T20:20:00.00Z");
        CronParserBean parserBean = new CronParserBean();
        assertThat(parserBean.shouldExecute("0 0 * * *", last, now),
            is(true));
    }

    @Test
    void describe() {
        CronParserBean parserBean = new CronParserBean();
        assertThat(parserBean.describe("3 * 22 3 *"),
            is("every hour at minute 3 at 22 day at March month"));
    }

    @Test
    void validate() {
        CronParserBean parserBean = new CronParserBean();
        assertThat("invalid expression", parserBean.validate("* 12 X 13 *"),
            is(false));
        assertThat("valid expression", parserBean.validate("* 23 31 1 3"),
            is(true));
    }

    private Date getDate(String date) throws ParseException {
        return getDate(date, ZoneId.systemDefault().getId());
    }

    private Date getDate(String date, String timezone) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf.parse(date);
    }
}
