package dk.dbc.saturn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.dbc.ftp.FtpClient;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class FtpFileHarvest implements Comparable<FileHarvest>, FileHarvest {
    private String filename;
    private final Integer seqno;
    private FtpClient ftpClient;
    private String dir;
    private final FileHarvest.Status status;
    private final Number size;
    private final AtomicReference<ByteCountingInputStream> countingInputStream = new AtomicReference<>();

    public FtpFileHarvest(String dir, String filename, Integer seqno, FtpClient ftpClient, FileHarvest.Status status, Number size) {
        this.filename = filename;
        this.seqno = seqno;
        this.ftpClient = ftpClient;
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
        ByteCountingInputStream stream = countingInputStream.get();
        return stream == null ? null : stream.getBytesRead();
    }

    @Override
    public String getUploadFilename( String prefix ){
        return String.format("%s.%s", prefix, filename);
    }

    @Override
    @JsonIgnore
    public ByteCountingInputStream getContent() {
        if (!dir.isEmpty()){
            ftpClient.cd(dir);
        }
        ByteCountingInputStream stream = new ByteCountingInputStream(ftpClient.get(filename, FtpClient.FileType.BINARY));
        countingInputStream.set(stream);
        return stream;
    }

    @Override
    public Integer getSeqno() {
        return seqno;
    }

    @Override
    public FileHarvest.Status getStatus() {
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

        FtpFileHarvest that = (FtpFileHarvest) o;

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
        return "FtpFileHarvest{" +
                "filename='" + filename + '\'' +
                ", dir='" + dir + '\'' +
                ", status=" + status +
                '}';
    }

    @Override
    public void close(){
        ftpClient.close();
    }

    @Override
    public int compareTo(FileHarvest other) {
        return filename.compareTo(other.getFilename());
    }
}
