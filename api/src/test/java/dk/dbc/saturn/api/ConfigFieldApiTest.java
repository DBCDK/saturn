/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.api;

import dk.dbc.saturn.RunScheduleFactory;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ConfigFieldApiTest {
    private final ConfigFieldApi configFieldApi = getConfigFieldApi();

    @Test
    void test_validateCron() {
        final Response response = configFieldApi.validateCron("* * * * *");
        assertThat("status", response.getStatus(), is(200));
    }

    @Test
    void test_validateCron_badRequest() {
        final Response response = configFieldApi.validateCron("invalid");
        assertThat("status", response.getStatus(), is(400));
    }

    @Test
    void test_describeCron() {
        final Response response = configFieldApi.describeCron("* * * * *");
        assertThat("status", response.getStatus(), is(200));
        assertThat("has entity", response.hasEntity(), is(true));
        assertThat("response entity", response.getEntity(), is("every minute"));
    }

    @Test
    void test_describeCron_invalidCronExpression() {
        final Response response = configFieldApi.describeCron("invalid");
        assertThat("status", response.getStatus(), is(400));
        assertThat("has entity", response.hasEntity(), is(false));
    }

    private ConfigFieldApi getConfigFieldApi() {
        final RunScheduleFactory runScheduleFactory = new RunScheduleFactory("Europe/Copenhagen");
        final ConfigFieldApi configFieldApi = new ConfigFieldApi();
        configFieldApi.runScheduleFactory = runScheduleFactory;
        return configFieldApi;
    }
}
