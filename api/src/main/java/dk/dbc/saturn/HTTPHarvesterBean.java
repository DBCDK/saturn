/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.CustomHttpHeader;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.job.JobSenderBean;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@LocalBean
@Stateless
public class HTTPHarvesterBean extends Harvester<HttpHarvesterConfig> {
    @EJB
    ProxyBean proxyBean;

    static RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                       response.getStatus() == 404
                    || response.getStatus() == 500
                    || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    public HTTPHarvesterBean() {
    }

    public HTTPHarvesterBean(HarvesterConfigRepository harvesterConfigRepository, JobSenderBean jobSenderBean, RunningTasks runningTasks, SessionContext context, ProxyBean proxyBean) {
        super(harvesterConfigRepository, jobSenderBean, runningTasks, context);
        this.proxyBean = proxyBean;
    }

    static Response getResponse(Client client, String url) throws HarvestException {
        return getResponse(client, url, null);
    }

    static Response getResponse(Client client, String url, List<CustomHttpHeader> headers) throws HarvestException {
        try {
            // Jersey client breaks if '{' or '}' are included in URLs in their decoded form
            url = url.replaceAll("\\{", "%7B");
            url = url.replaceAll("\\}", "%7D");
            HttpGet httpGet = new HttpGet(HttpClient.create(ClientBuilder.newClient()))
                    .withBaseUrl(url);
            if (headers != null) {
                headers.forEach(header -> httpGet.withHeader(header.getKey(), header.getValue()));
            }
            final Response response = httpGet.execute();
            if (!List.of(Response.Status.OK.getStatusCode(), Response.Status.PARTIAL_CONTENT.getStatusCode()).contains(response.getStatus())) {
                throw new HarvestException(String.format(
                        "got status \"%s\" when trying url \"%s\"",
                        response.getStatus(), url));
            }
            return response;
        } catch (Exception e) {
            if (client != null) {
                client.close();
            }
            if (e instanceof HarvestException) {
                throw e;
            }
            throw new HarvestException("error getting response: " + e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<FileHarvest> listFiles(HttpHarvesterConfig config) throws HarvestException {
        return getHttpListFilesHandler(config).listFiles(config);
    }

    HttpListFilesHandler getHttpListFilesHandler(HttpHarvesterConfig config) {
        if (config.getListFilesHandler() == HttpHarvesterConfig.ListFilesHandler.LITTERATURSIDEN) {
            return new LitteratursidenHttpListFilesHandler(proxyBean, RETRY_POLICY);
        }
        return new HttpListFilesHandler(proxyBean, RETRY_POLICY, config.getHttpHeaders());
    }
}
