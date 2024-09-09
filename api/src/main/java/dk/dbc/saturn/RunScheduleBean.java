/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.util.RunSchedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.ZoneId;
import java.util.Date;

@ApplicationScoped
public class RunScheduleBean {
    @Inject
    @ConfigProperty(name = "TIMEZONE", defaultValue = "Europe/Copenhagen")
    String timezone;

    public RunScheduleBean() {}

    public RunScheduleBean(String timezone) {
        this.timezone = timezone;
    }

    public RunSchedule newRunScheduleFrom(String expression) throws IllegalArgumentException {
        return new RunSchedule(expression)
                .withTimezone(ZoneId.of(timezone));
    }

    public <T extends AbstractHarvesterConfigEntity> boolean shouldRun(T config) {
        return newRunScheduleFrom(config.getSchedule()).isSatisfiedBy(new Date(), config.getLastHarvested());
    }

    public <T extends AbstractHarvesterConfigEntity> boolean shouldSkip(T config) {
        return !shouldRun(config);
    }
}
