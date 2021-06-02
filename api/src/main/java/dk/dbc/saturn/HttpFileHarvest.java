/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Objects;

public class HttpFileHarvest implements Comparable<FileHarvest>, FileHarvest {
    private String filename;
    private final Client client;
    private final String url;
    private final Integer seqno;
    private final FileHarvest.Status status;

    private static final Logger LOGGER = LoggerFactory.getLogger(
            HttpFileHarvest.class);

    public HttpFileHarvest(String filename, Client client, String url, Integer seqno, FileHarvest.Status status) {
        this.filename = filename;
        this.client = client;
        this.url = url;
        this.seqno = seqno;
        this.status = status;
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
        /*
        todo: stopwatch timing
         */
        final Response response = HTTPHarvesterBean.getResponse(client, url);
        if (response.hasEntity()) {
            InputStream is = response.readEntity(InputStream.class);
            return is;
        }
        else {
            throw new HarvestException(String.format("Unable to read from url %s", url));
        }
    }

    public Integer getSeqno() {
        return seqno;
    }

    @Override
    public Status getStatus() {
        return status;
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
                ", seqno=" + seqno +
                ", status=" + status +
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