/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

@LocalBean
@Stateless
public class CronParserBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ScheduledHarvesterBean.class);
    private static CronDefinition definition =
        CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    private static CronParser parser = new CronParser(definition);

    public boolean shouldExecute(String expression, Date lastExecution)
            throws HarvestException {
        return shouldExecute(expression, lastExecution,
            Timestamp.from(Instant.now()));
    }

    /**
     * determine whether a given cron expression should trigger an
     * execution when used to compare a timestamp of the last execution and
     * the current time
     * @param expression cron expression
     * @param lastExecution timestamp of last execution
     * @param now current time (provided to fixate unit tests)
     * @return true if the expression should trigger an execution
     */
    public boolean shouldExecute(String expression, Date lastExecution,
            Date now) throws HarvestException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(expression, "expression");
        Cron cron = parser.parse(expression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        /* use the system time zone because trying to correlate with the
         * client time zone would require us to fetch and store the client
         * time zone when storing the cron expression and then hand-converting
         * the stored timestamp to a representation with a time zone.
         * that would be an unnecessary complication when we assume that
         * client and backend has the same time zone. if that turns out not
         * to be the case in the future, this scenario will have to be accounted
         * for.
         */
        ZonedDateTime nowDateTime = now.toInstant().atZone(ZoneId.systemDefault());
        /* there is a possibility of losing an execution here if the
         * harvester for some reason skips checking at precisely the moment
         * where the cron expression is supposed to trigger an execution and
         * and there isn't any last harvested timestamp.
         * since a harvest will eventually be triggered the next time the
         * expression matches, we would rather not handle this corner case
         * at the benefit of having simpler code.
         */
        if(executionTime.isMatch(nowDateTime)) {
            return true;
        } else if(lastExecution == null) {
            return false;
        }
        ZonedDateTime lastExecutionDateTime = lastExecution.toInstant().atZone(ZoneId.systemDefault());
        Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(lastExecutionDateTime);
        if(nextExecution.isPresent()) {
            Comparator<ZonedDateTime> comparator = Comparator.comparing(
                time -> time.truncatedTo(ChronoUnit.MINUTES));
            return comparator.compare(nextExecution.get(), nowDateTime) < 0;
        }
        throw new HarvestException(String.format("failed to get next" +
            "execution time based on cron expression \"%s\" and last " +
            "harvest time \"%s\"", expression, lastExecutionDateTime));
    }

    /**
     * provide a human-readable description of a cron expression
     * @param expression cron expression
     * @return human-readable description
     */
    public String describe(String expression) {
        InvariantUtil.checkNotNullNotEmptyOrThrow(expression, "expression");
        // the cron descriptor doesn't have a danish translation, so we use
        // it without localization
        CronDescriptor descriptor = CronDescriptor.instance();
        return descriptor.describe(parser.parse(expression));
    }

    /**
     * validate a cron expression
     * @param expression cron expression
     * @return true if the expression is a valid cron expression
     */
    public boolean validate(String expression) {
        try {
            parser.parse(expression);
            return true;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("validation failed for expression \"{}\"", expression);
        }
        return false;
    }
}
