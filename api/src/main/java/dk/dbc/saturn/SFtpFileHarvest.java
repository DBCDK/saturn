package dk.dbc.saturn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.dbc.commons.sftpclient.SFtpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SFtpFileHarvest implements Comparable<FileHarvest>, FileHarvest {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            SFtpFileHarvest.class);
    private final String filename;
    private final Integer seqno;
    private final SFtpClient sftpClient;
    private final String dir;
    private final Status status;
    private final Number size;
    private final AtomicReference<ByteCountingInputStream> countingInputStream = new AtomicReference<>();

    public SFtpFileHarvest(String dir, String filename, Integer seqno, SFtpClient sftpClient, Status status, Number size) {
        this.filename = filename;
        this.seqno = seqno;
        this.sftpClient = sftpClient;
        this.dir = dir;
        this.status = status;
        this.size = size;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public Number getSize() {
        return size;
    }

    @Override
    public Number getBytesTransferred() {
        return countingInputStream.get() == null ? null : countingInputStream.get().getBytesRead();
    }

    @Override
    public String getUploadFilename( String prefix ){
        return String.format("%s.%s", prefix, filename);
    }

    @Override
    @JsonIgnore
    public ByteCountingInputStream getContent() {
        LOGGER.info("Trying to get: {}", filename);
        ByteCountingInputStream stream = new ByteCountingInputStream(sftpClient.getContent(filename));
        countingInputStream.set(stream);
        return stream;
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
