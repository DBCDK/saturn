/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;

@LocalBean
@Stateless
public class HTTPHarvesterBean {
    @EJB ProxyHandlerBean proxyHandlerBean;
    @EJB FtpSenderBean ftpSenderBean;
    @EJB RunningTasks runningTasks;
    @EJB HarvesterConfigRepository harvesterConfigRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPHarvesterBean.class);

    static RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                       response.getStatus() == 404
                    || response.getStatus() == 500
                    || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    static Response getResponse(Client client, String url) throws HarvestException {
        try {
            // Jersey client breaks if '{' or '}' are included in URLs in their decoded form
            url = url.replaceAll("\\{", "%7B");
            url = url.replaceAll("\\}", "%7D");
            final FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client, RETRY_POLICY);
            final Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(url)
                    .execute();

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
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

    @Asynchronous
    public Future<Void> harvest( HttpHarvesterConfig config ) throws HarvestException {
        doHarvest(config);
        return new AsyncResult<Void>(null);
    }

    HttpListFilesHandler getHttpListFilesHandler(HttpHarvesterConfig config) {
        if (config.getListFilesHandler() == HttpHarvesterConfig.ListFilesHandler.LITTERATURSIDEN) {
            return new LitteratursidenHttpListFilesHandler(proxyHandlerBean, RETRY_POLICY);
        }
        return new HttpListFilesHandler(proxyHandlerBean, RETRY_POLICY);
    }

    private void doHarvest(HttpHarvesterConfig config) throws HarvestException {
        LOGGER.info("Harvesting url {}", config.getUrl());
        try (HarvesterMDC mdc = new HarvesterMDC(config)) {
            LOGGER.info("Starting harvest of {}", config.getName());
            Set<FileHarvest> fileHarvests = listFiles( config );
            ftpSenderBean.send(fileHarvests, config.getAgency(), config.getTransfile());
            config.setLastHarvested(Date.from(Instant.now()));
            config.setSeqno(fileHarvests.stream()
                    .map(FileHarvest::getSeqno)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Integer::valueOf))
                    .orElse(0));
            fileHarvests.stream().forEach(FileHarvest::close);

            harvesterConfigRepository.save(HttpHarvesterConfig.class, config);
            LOGGER.info("Ended harvest of {}", config.getName());
        } finally {
            runningTasks.remove(config);
        }
    }
}
