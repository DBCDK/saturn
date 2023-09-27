/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import dk.dbc.httpclient.HttpClient;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.CustomHttpHeader;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.util.Stopwatch;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpListFilesHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpListFilesHandler.class);

    private static final Pattern FILENAME_PATTERN = Pattern.compile(".*filename=[\"\']([^\"\']+)[\"\']");

    final ProxyBean proxyHandler;
    final RetryPolicy<Response> retryPolicy;
    final List<CustomHttpHeader> headers;

    public HttpListFilesHandler(ProxyBean proxyHandler, RetryPolicy<Response> retryPolicy, List<CustomHttpHeader> headers) {
        this.proxyHandler = proxyHandler;
        this.retryPolicy = retryPolicy;
        this.headers = headers;
    }

    public Set<FileHarvest> listFiles(HttpHarvesterConfig config) throws HarvestException {
        final String url = config.getUrl();
        InvariantUtil.checkNotNullNotEmptyOrThrow(url, "url");
        LOGGER.info("Listing files from {}", url);
        if (config.getUrlPattern() == null || config.getUrlPattern().isEmpty() ){
            return listFiles(url);
        }
        return listFiles(url, config.getUrlPattern());
    }

    Set<FileHarvest> listFiles(String url, String urlPattern) throws HarvestException {
        LOGGER.info("looking for pattern {} in {}", urlPattern, url);
        final String datafileUrl = findInContent(url, urlPattern);
        LOGGER.info("found url {} for pattern {}", datafileUrl, urlPattern);
        return listFiles(datafileUrl);
    }

    Set<FileHarvest> listFiles(String url) throws HarvestException {
        final Stopwatch stopwatch = new Stopwatch();
        try {
            final Client client = getHttpClient(new URL(url));
            final Response response = HTTPHarvesterBean.getResponse(client, url, headers);
            if (response.hasEntity()) {
                final Optional<String> filename = getFilenameFromResponse(response);
                final Set<FileHarvest> fileHarvests = new HashSet<>();
                final FileHarvest fileHarvest;
                if (filename.isPresent()) {
                    fileHarvest = new HttpFileHarvest(
                            filename.get(), client, url, null, FileHarvest.Status.AWAITING_DOWNLOAD, headers);
                } else {
                    fileHarvest = new HttpFileHarvest(
                            getFilenameFromURL(url), client, url, null, FileHarvest.Status.AWAITING_DOWNLOAD, headers);
                }
                fileHarvests.add(fileHarvest);
                return fileHarvests;
            } else {
                throw new HarvestException(String.format(
                        "no entity found on response for url \"%s\"", url));
            }
        } catch (MalformedURLException e) {
            throw new HarvestException(String.format("invalid URL: %s - %s", url, e));
        } finally {
            LOGGER.info("Listing of {} took {} ms", url,
                    stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        }
    }

    // finds a string matching a pattern in the content fetched from the given url
    String findInContent(String url, String pattern) throws HarvestException {
        final FileNameMatcher fileNameMatcher = new FileNameMatcher(pattern);
        try {
            final Client client = getHttpClient(new URL(url));
            final Response response = HTTPHarvesterBean.getResponse(client, url, headers);
            if (response.hasEntity()) {
                final String result = response.readEntity(String.class);
                final Matcher matcher = fileNameMatcher.getPattern().matcher(result);
                final Set<String> urlCandidates = new HashSet<>();
                while (matcher.find()) {
                    urlCandidates.add(matcher.group());
                }
                /* a search for a url where the pattern needs to be contained
                 * in globable characters (i.e. not having access to lookaround
                 * and other more advanced regex features) may return several
                 * matches. we assume the shortest match is the most relevant.
                 */
                Optional<String> smallestMatch = urlCandidates.stream().min(Comparator.comparingInt(String::length));
                if (smallestMatch.isPresent()) {
                    return smallestMatch.get();
                }
                throw new HarvestException(String.format("not matches found for pattern %s", pattern));
            } else {
                throw new HarvestException(String.format("response for url %s return empty body", url));
            }
        } catch (MalformedURLException e) {
            throw new HarvestException(String.format("invalid URL: %s - %s", url, e));
        }
    }

    Client getHttpClient(URL url) throws HarvestException {
        final ClientConfig clientConfig = new ClientConfig();
        if (proxyHandler.useProxy(url.getHost())) {
            clientConfig.connectorProvider(proxyHandler.getHttpUrlConnectorProvider());
            LOGGER.info("Using proxy: {}:{}", proxyHandler.getProxyHostname(), proxyHandler.getProxyPort());
        }
        return HttpClient.newClient(clientConfig);
    }

    Optional<String> getFilenameFromResponse(Response response) {
        final String contentDispositionHeader = response.getHeaderString("Content-Disposition");
        if (contentDispositionHeader != null) {
            Matcher matcher = FILENAME_PATTERN.matcher(contentDispositionHeader);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    String getFilenameFromURL(String url) {
        if (url.charAt(url.length() - 1) == '/') {
            url = url.substring(0, url.length() - 1);
        }
        try {
            // URL encode filename to avoid illegal characters when creating data+transfiles
            return URLEncoder.encode(url.substring(url.lastIndexOf("/") + 1), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
