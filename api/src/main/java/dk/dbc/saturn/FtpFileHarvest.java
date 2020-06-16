package dk.dbc.saturn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.dbc.ftp.FtpClient;

import java.io.InputStream;
import java.util.Objects;

public class FtpFileHarvest implements Comparable<FileHarvest>, FileHarvest {
    private String filename;
    private final Integer seqno;
    private FtpClient ftpClient;
    private String dir;
    private final FileHarvest.Status status;

    public FtpFileHarvest(String dir, String filename, Integer seqno, FtpClient ftpClient, FileHarvest.Status status) {
        this.filename = filename;
        this.seqno = seqno;
        this.ftpClient = ftpClient;
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
        if (!dir.isEmpty()){
            ftpClient.cd(dir);
        }
        return ftpClient.get(filename, FtpClient.FileType.BINARY);
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
        if (!Objects.equals(seqno, that.seqno)) {
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
        result = 31 * result + (seqno != null ? seqno.hashCode() : 0);
        result = 31 * result + (dir != null ? dir.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FtpFileHarvest{" +
                "filename='" + filename + '\'' +
                ", seqno=" + seqno +
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