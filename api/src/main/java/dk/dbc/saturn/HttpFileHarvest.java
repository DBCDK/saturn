/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.dbc.saturn.entity.CustomHttpHeader;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class HttpFileHarvest implements Comparable<FileHarvest>, FileHarvest {
    private final String filename;
    private final Client client;
    private final String url;
    private final Integer seqno;
    private final FileHarvest.Status status;
    private final List<CustomHttpHeader> headers;
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HttpFileHarvest.class);
    public static final String RANGE_HEADER = "Range";
    private boolean resumable = false;

    public HttpFileHarvest(String filename, Client client, String url,
                           Integer seqno, FileHarvest.Status status,
                           List<CustomHttpHeader> headers) {
        this.filename = filename;
        this.client = client;
        this.url = url;
        this.seqno = seqno;
        this.status = status;
        this.headers = headers;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String getUploadFilename(String prefix) {
        return String.format("%s.%s", prefix, filename);
    }

    @JsonIgnore
    public InputStream getContent() throws HarvestException {
        LOGGER.info("Headers:{}", headers);
        final Response response = HTTPHarvesterBean.getResponse(client, url, headers);
        resumable = response.getStatus() == Response.Status.PARTIAL_CONTENT.getStatusCode();
        if (response.hasEntity()) {
            InputStream is = response.readEntity(InputStream.class);
            return is;
        }
        else {
            throw new HarvestException(String.format("Unable to read from url %s", url));
        }
    }

    public void setResumePoint(long resumePoint) {
        String range = String.format("bytes=%d-", resumePoint);
        LOGGER.info("Setting resumepoint at:{}", range);
        CustomHttpHeader customHttpHeader = headers.stream().filter(header -> RANGE_HEADER.equals(header.getKey()))
                .findFirst()
                .orElse(null);
        if (customHttpHeader == null) {
            headers.add(new CustomHttpHeader().withKey(RANGE_HEADER).withValue(range));
        } else {
            customHttpHeader.setValue(range);
        }
    }

    public Integer getSeqno() {
        return seqno;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public boolean isResumable() {
        return resumable;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HttpFileHarvest that = (HttpFileHarvest) o;

        if (!Objects.equals(filename, that.filename)) {
            return false;
        }
        if (!Objects.equals(seqno, that.seqno)) {
            return false;
        }
        return status == that.status;
    }

    @Override
    public int hashCode() {
        int result = filename != null ? filename.hashCode() : 0;
        result = 31 * result + (seqno != null ? seqno.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HttpFileHarvest{" +
                "filename='" + filename + '\'' +
                ", url='" + url + '\'' +
                ", seqno=" + seqno +
                ", headers=" + headers +
                '}';
    }

    public int compareTo(FileHarvest other) {
        return filename.compareTo(other.getFilename());
    }

    @Override
    public void close() {
        LOGGER.info("Closing http connection to {}", url);
        client.close();
    }

}
