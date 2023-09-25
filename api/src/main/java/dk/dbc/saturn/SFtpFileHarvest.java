package dk.dbc.saturn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.InputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dk.dbc.commons.sftpclient.SFtpClient;

public class SFtpFileHarvest implements Comparable<FileHarvest>, FileHarvest {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            SFtpFileHarvest.class);
    private final String filename;
    private final Integer seqno;
    private final SFtpClient sftpClient;
    private final String dir;
    private final Status status;

    public SFtpFileHarvest(String dir, String filename, Integer seqno, SFtpClient sftpClient, Status status) {
        this.filename = filename;
        this.seqno = seqno;
        this.sftpClient = sftpClient;
        this.dir = dir;
        this.status = status;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getUploadFilename( String prefix ){
        return String.format("%s.%s", prefix, filename);
    }

    @Override
    @JsonIgnore
    public InputStream getContent() {
        LOGGER.info("Trying to get: {}", filename);
        return sftpClient.getContent(filename);
    }

    @Override
    public Integer getSeqno() {
        return seqno;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SFtpFileHarvest that = (SFtpFileHarvest) o;

        if (!Objects.equals(filename, that.filename)) {
            return false;
        }
        if (!Objects.equals(dir, that.dir)) {
            return false;
        }
        return status == that.status;
    }

    @Override
    public int hashCode() {
        int result = filename != null ? filename.hashCode() : 0;
        result = 31 * result + (dir != null ? dir.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SFtpFileHarvest{" +
                "filename='" + filename + '\'' +
                ", dir='" + dir + '\'' +
                ", status=" + status +
                '}';
    }
    @Override
    public void close(){
        sftpClient.close();
    }

    @Override
    public int compareTo(FileHarvest other) {
        return filename.compareTo(other.getFilename());
    }
}
