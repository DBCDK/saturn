/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.api;

import dk.dbc.saturn.CronParserBean;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigFieldApiTest {
    private static final CronParserBean cronParserBean = mock(
        CronParserBean.class);

    @Test
    void test_validateCron() {
        when(cronParserBean.validate(anyString())).thenReturn(true);
        ConfigFieldApi configFieldApi = getConfigFieldApi();
        Response response = configFieldApi.validateCron("* * * * *");
        assertThat("status", response.getStatus(), is(200));
    }

    @Test
    void test_validateCron_badRequest() {
        when(cronParserBean.validate(anyString())).thenReturn(false);
        ConfigFieldApi configFieldApi = getConfigFieldApi();
        Response response = configFieldApi.validateCron("* * * * *");
        assertThat("status", response.getStatus(), is(400));
    }

    @Test
    void test_describeCron() {
        when(cronParserBean.validate(anyString())).thenReturn(true);
        when(cronParserBean.describe(anyString())).thenReturn("description");
        ConfigFieldApi configFieldApi = getConfigFieldApi();
        final Response response = configFieldApi.describeCron("* * * * *");
        assertThat("status", response.getStatus(), is(200));
        assertThat("has entity", response.hasEntity(), is(true));
        assertThat("response entity", response.getEntity(), is("description"));
    }

    @Test
    void test_describeCron_invalidCronExpression() {
        when(cronParserBean.validate(anyString())).thenReturn(false);
        when(cronParserBean.describe(anyString())).thenReturn("description");
        ConfigFieldApi configFieldApi = getConfigFieldApi();
        final Response response = configFieldApi.describeCron("* * * * *");
        assertThat("status", response.getStatus(), is(400));
        assertThat("has entity", response.hasEntity(), is(false));
    }

    private ConfigFieldApi getConfigFieldApi() {
        final ConfigFieldApi configFieldApi = new ConfigFieldApi();
        configFieldApi.cronParserBean = cronParserBean;
        return configFieldApi;
    }
}
