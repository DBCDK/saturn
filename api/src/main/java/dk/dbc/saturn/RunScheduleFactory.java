/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.util.RunSchedule;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.ZoneId;

@ApplicationScoped
public class RunScheduleFactory {
    @Inject
    @ConfigProperty(name = "TIMEZONE", defaultValue = "Europe/Copenhagen")
    String timezone;

    public RunScheduleFactory() {}

    public RunScheduleFactory(String timezone) {
        this.timezone = timezone;
    }

    public RunSchedule newRunScheduleFrom(String expression) throws IllegalArgumentException {
        return new RunSchedule(expression)
                .withTimezone(ZoneId.of(timezone));
    }
}
