/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.util.Stopwatch;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles files listing for site litteratursiden.dk:
 *
 * https://litteratursiden.dk/rest-output-dbc/24h?page=${PAGE_NO}
 *
 * where PAGE_NO can take on values from 0 to n as long as the service responds with a non-empty JSON array
 * for increasing page numbers.
 */
public class LitteratursidenHttpListFilesHandler extends HttpListFilesHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LitteratursidenHttpListFilesHandler.class);

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");
    private final Pattern pagePattern = Pattern.compile("(?:\\?|&)page=(\\d+)", Pattern.MULTILINE);

    public LitteratursidenHttpListFilesHandler(ProxyBean proxyHandler, RetryPolicy<Response> retryPolicy) {
        super(proxyHandler, retryPolicy, null);
    }

    @Override
    public Set<FileHarvest> listFiles(HttpHarvesterConfig config) throws HarvestException {
        final String url = config.getUrl();
        InvariantUtil.checkNotNullNotEmptyOrThrow(url, "url");
        LOGGER.info("Listing files from {}", url);
        return listFiles(url);
    }

    @Override
    Set<FileHarvest> listFiles(String templateUrl) throws HarvestException {
        final Set<FileHarvest> fileHarvests = new HashSet<>();
        final Map<String, String> valuesMap = new HashMap<>();
        final String formattedDateTime = LocalDateTime.now().format(dateTimeFormatter);
        int nextPageNo = 0;
        do {
            valuesMap.put("PAGE_NO", String.valueOf(nextPageNo));
            final String url = StringSubstitutor.replace(templateUrl, valuesMap);
            if (!hasPageNo(url, nextPageNo)) {
                LOGGER.error("Looks like PAGE_NO variable is not used correctly in {}", templateUrl);
                break;
            }
            final Stopwatch stopwatch = new Stopwatch();
            try {
                final Client client = getHttpClient(new URL(url));
                final Response response = HTTPHarvesterBean.getResponse(client, url);
                if (getNumberOfRecordsInResponse(response, url) > 0) {
                    fileHarvests.add(new HttpFileHarvest(String.format("%s.page%d", formattedDateTime, nextPageNo),
                            client, url, null, FileHarvest.Status.AWAITING_DOWNLOAD, null));
                    nextPageNo++;       // continue testing next page number
                } else {
                    nextPageNo = -1;    // no more pages need to be tested
                }
            } catch (MalformedURLException e) {
                throw new HarvestException(String.format("invalid URL: %s - %s", url, e));
            } finally {
                LOGGER.info("Listing of {} took {} ms", url, stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
            }

        } while (nextPageNo >= 0);

        return fileHarvests;
    }

    private int getNumberOfRecordsInResponse(Response response, String url) throws HarvestException {
        if (!response.hasEntity()) {
             throw new HarvestException(String.format("no entity found on response for url \"%s\"", url));
        }
        final String json = response.readEntity(String.class);
        try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            final JsonArray records = jsonReader.readArray();
            final int recordCount = records.size();
            LOGGER.info("Listing of {} found {} records", url, recordCount);
            return recordCount;
        }
    }

    private boolean hasPageNo(String url, int pageNo) {
        final Matcher pageMatcher = pagePattern.matcher(url);
        if (pageMatcher.find()) {
            return Integer.parseInt(pageMatcher.group(1)) == pageNo;
        }
        return false;
    }
}
