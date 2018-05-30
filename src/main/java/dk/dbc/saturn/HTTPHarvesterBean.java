/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.invariant.InvariantUtil;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@LocalBean
@Stateless
public class HTTPHarvesterBean implements Harvester {
    protected static RetryPolicy RETRY_POLICY = new RetryPolicy()
        .retryOn(Collections.singletonList(ProcessingException.class))
        .retryIf((Response response) -> response.getStatus() == 404 ||
            response.getStatus() == 500 || response.getStatus() == 502)
        .withDelay(10, TimeUnit.SECONDS)
        .withMaxRetries(6);

    public InputStream harvest(String url) throws HarvestException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(url, "url");

        final Client client = HttpClient.newClient(new ClientConfig()
            .register(new JacksonFeature()));
        try {
            final FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(
                client, RETRY_POLICY);
            final Response response = new HttpGet(failSafeHttpClient)
                .withBaseUrl(url)
                .execute();

            if(response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new HarvestException(String.format(
                    "got status \"%s\" when trying url \"%s\"",
                    response.getStatus(), url));
            }
            if (response.hasEntity()) {
                return response.readEntity(InputStream.class);
            } else {
                throw new HarvestException(String.format(
                    "no entity found on response for url \"%s\"", url));
            }
        } finally {
            client.close();
        }
    }
}
