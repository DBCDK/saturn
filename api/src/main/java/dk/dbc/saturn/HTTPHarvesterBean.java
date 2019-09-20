/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.util.Stopwatch;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionAttribute;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;


import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Date;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.Objects;
import java.util.Comparator;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@LocalBean
@Stateless
public class HTTPHarvesterBean {
    @EJB ProxyHandlerBean proxyHandlerBean;
    @EJB FtpSenderBean ftpSenderBean;
    @EJB RunningTasks runningTasks;
    @EJB HarvesterConfigRepository harvesterConfigRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(
        HTTPHarvesterBean.class);

    protected static RetryPolicy RETRY_POLICY = new RetryPolicy()
        .retryOn(Collections.singletonList(ProcessingException.class))
        .retryIf((Response response) -> response.getStatus() == 404 ||
            response.getStatus() == 500 || response.getStatus() == 502)
        .withDelay(10, TimeUnit.SECONDS)
        .withMaxRetries(6);

    private static Pattern filenamePattern =
        Pattern.compile(".*filename=[\"\']([^\"\']+)[\"\']");

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Set<FileHarvest> listFiles(HttpHarvesterConfig config) throws HarvestException {
        String url = config.getUrl();
        InvariantUtil.checkNotNullNotEmptyOrThrow(url, "url");
        LOGGER.info("Listing files from {}", url);
        if (config.getUrlPattern() ==null || config.getUrlPattern().isEmpty() ){
            return listFiles(url);
        }
        else {
            return listFiles(url, config.getUrlPattern());
        }
    }

    private Set<FileHarvest> listFiles(String url, String urlPattern) throws HarvestException {
        LOGGER.info("looking for pattern {} in {}", urlPattern, url);
        final String datafileUrl = findInContent(url, urlPattern);
        LOGGER.info("found url {} for pattern {}", datafileUrl, urlPattern);
        return listFiles(datafileUrl);
    }

    private Set<FileHarvest> listFiles(String url) throws HarvestException {
        final Client client = getHttpClient();
        final Stopwatch stopwatch = new Stopwatch();
        try {
            final Response response = getResponse(client, url);
            if (response.hasEntity()) {
                final Optional<String> filename = getFilenameFromResponse(response);
                final Set<FileHarvest> fileHarvests = new HashSet<>();
                if (filename.isPresent()) {
                    final FileHarvest fileHarvest = new HttpFileHarvest(
                            filename.get(), client, url, null);
                    fileHarvests.add(fileHarvest);
                } else {
                    final FileHarvest fileHarvest = new HttpFileHarvest(
                            getFilename(url), client, url, null);
                    fileHarvests.add(fileHarvest);
                }
                return fileHarvests;
            } else {
                throw new HarvestException(String.format(
                        "no entity found on response for url \"%s\"", url));
            }
        } finally {
            LOGGER.info("Listing of {} took {} ms", url,
                    stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        }
    }

    @Asynchronous
    public Future<Void> harvest( HttpHarvesterConfig config ) throws HarvestException {
        doHarvest(config);
        return new AsyncResult<Void>(null);
    }

    private void doHarvest(HttpHarvesterConfig config) throws HarvestException {
        LOGGER.info("Do harvestingf of url {}", config.getUrl());
        try (HarvesterMDC mdc = new HarvesterMDC(config)) {
            LOGGER.info( "Starting harvest of {}", config.getName());
            Set<FileHarvest> fileHarvests = listFiles( config );
            ftpSenderBean.send(fileHarvests, config.getAgency(), config.getTransfile());
            config.setLastHarvested(Date.from(Instant.now()));
            config.setSeqno(fileHarvests.stream()
                    .map(FileHarvest::getSeqno)
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(Integer::valueOf))
                    .orElse(0));
            fileHarvests.stream().forEach(FileHarvest::close);

            harvesterConfigRepository.save( HttpHarvesterConfig.class, config );
            LOGGER.info( "Ended harvest of {}", config.getName() );
            runningTasks.remove( config );
        }
    }

    private  Client getHttpClient(){
        final ClientConfig clientConfig = new ClientConfig();
        final Optional<HttpUrlConnectorProvider> connectorProvider =
                getConnectorProvider();
        connectorProvider.ifPresent(clientConfig::connectorProvider);
        clientConfig.register(new JacksonFeature());
        // for logging requests and responses (LoggingFilter is deprecated
        // in jersey 2.23 though)
        //clientConfig.register(LoggingFilter.class);
        return HttpClient.newClient(clientConfig);
    }



    protected static Response getResponse(Client client, String url) throws HarvestException {
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
        return response;
    }

    // finds a string matching a pattern in the content fetched from the given url
    protected String findInContent(String url, String pattern) throws HarvestException {
        final FileNameMatcher fileNameMatcher = new FileNameMatcher(pattern);
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JacksonFeature());
        final Optional<HttpUrlConnectorProvider> connectorProvider =
            getConnectorProvider();
        connectorProvider.ifPresent(clientConfig::connectorProvider);
        final Client client = HttpClient.newClient(clientConfig);
        try {
            final Response response = getResponse(client, url);
            if(response.hasEntity()) {
                final String result = response.readEntity(String.class);
                final Matcher matcher = fileNameMatcher.getPattern()
                    .matcher(result);
                final Set<String> groups = new HashSet<>();
                while(matcher.find()) {
                    groups.add(matcher.group());
                }
                /* a search for a url where the pattern needs to be contained
                 * in globable characters (i.e. not having access to lookaround
                 * and other more advanced regex features) may return several
                 * matches. we assume the shortest match is the most relevant.
                 */
                Optional<String> smallestMatch = groups.stream().min(
                    Comparator.comparingInt(String::length));
                if(smallestMatch.isPresent()) {
                    return smallestMatch.get();
                }
                throw new HarvestException(String.format("not matches found " +
                    "for pattern %s", pattern));
            } else {
                throw new HarvestException(String.format("response for url " +
                    "%s return empty body", url));
            }
        } finally {
            client.close();
        }
    }

    private Optional<String> getFilenameFromResponse(Response response) {
        final String contentDispositionHeader = response.getHeaderString(
            "Content-Disposition");
        if(contentDispositionHeader != null) {
            Matcher matcher = filenamePattern.matcher(contentDispositionHeader);
            if(matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private String getFilename(String url) {
        if(url.charAt(url.length() - 1) == '/') {
            url = url.substring(0, url.length() - 1);
        }
        return url.substring(url.lastIndexOf("/") + 1, url.length());
    }

    private Optional<HttpUrlConnectorProvider> getConnectorProvider() {
        if (proxyHandlerBean.getProxyHostname() != null
                && proxyHandlerBean.getProxyPort() != 0) {
            LOGGER.debug("using proxy: host = {} port = {}",
                    proxyHandlerBean.getProxyHostname(),
                    proxyHandlerBean.getProxyPort());
            final SocksConnectionFactory connectionFactory = new SocksConnectionFactory(
                    proxyHandlerBean.getProxyHostname(), proxyHandlerBean.getProxyPort());
            final HttpUrlConnectorProvider connectorProvider =
                new HttpUrlConnectorProvider();
            connectorProvider.connectionFactory(connectionFactory);

            return Optional.of(connectorProvider);
        }
        return Optional.empty();
    }
}
