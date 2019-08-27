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

    public FtpFileHarvest(String dir, String filename, Integer seqno, FtpClient ftpClient) {
        this.filename = filename;
        this.seqno = seqno;
        this.ftpClient = ftpClient;
        this.dir = dir;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FtpFileHarvest that = (FtpFileHarvest) o;
        return Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename);
    }

    @Override
    public String toString() {
        return "FileHarvest{" +
                "filename='" + filename + '\'' +
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