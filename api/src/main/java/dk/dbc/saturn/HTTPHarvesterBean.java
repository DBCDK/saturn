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
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@LocalBean
@Stateless
public class HTTPHarvesterBean {
    @EJB ProxyHandlerBean proxyHandlerBean;

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

    @Asynchronous
    public Future<Set<FileHarvest>> harvest(String url) throws HarvestException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(url, "url");
        long start = Instant.now().toEpochMilli();
        LOGGER.info("harvesting {}", url);

        final ClientConfig clientConfig = new ClientConfig();
        if(proxyHandlerBean.getProxyHostname() != null &&
                proxyHandlerBean.getProxyPort() != 0) {
            final String proxyHost = proxyHandlerBean.getProxyHostname();
            final int proxyPort = proxyHandlerBean.getProxyPort();
            LOGGER.info(String.format(
                "running through proxy: host = %s port = %s", proxyHost,
                proxyPort));
            SocksConnectionFactory connectionFactory =
                new SocksConnectionFactory(proxyHost, proxyPort);
            HttpUrlConnectorProvider connectorProvider =
                new HttpUrlConnectorProvider();
            connectorProvider.connectionFactory(connectionFactory);

            clientConfig.connectorProvider(connectorProvider);
        }
        clientConfig.register(new JacksonFeature());
        final Client client = HttpClient.newClient(clientConfig);
        try {
            final Response response = getResponse(client, url);
            if (response.hasEntity()) {
                InputStream is = response.readEntity(InputStream.class);
                final Optional<String> filename = getFilenameFromResponse(response);
                final Set<FileHarvest> fileHarvests = new HashSet<>();
                if (filename.isPresent()) {
                    final FileHarvest fileHarvest = new FileHarvest(
                        filename.get(), is, null);
                    fileHarvests.add(fileHarvest);
                } else {
                    final FileHarvest fileHarvest = new FileHarvest(
                        getFilename(url), is, null);
                    fileHarvests.add(fileHarvest);
                }
                return new AsyncResult<>(fileHarvests);
            } else {
                throw new HarvestException(String.format(
                    "no entity found on response for url \"%s\"", url));
            }
        } finally {
            LOGGER.info("harvesting of {} took {} ms", url,
                (Instant.now().toEpochMilli() - start));
            client.close();
        }
    }

    /**
     * harvest data from a url found in the response from the first url
     *
     * @param url url pointing to a page containing a url pointing to a datafile
     * @param pattern pattern of the url to the datafile
     * @return a FileHarvest object containing the fetched resource
     * @throws HarvestException on empty response from the first url or
     * failure to match the pattern against the response
     */
    @Asynchronous
    public Future<Set<FileHarvest>> harvest(String url, String pattern)
            throws HarvestException {
        final String datafileUrl = findInContent(url, pattern);
        return harvest(datafileUrl);
    }

    private Response getResponse(Client client, String url) throws HarvestException {
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
        final Client client = HttpClient.newClient(new ClientConfig()
            .register(new JacksonFeature()));
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
}
