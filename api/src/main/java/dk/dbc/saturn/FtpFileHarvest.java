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

    public String getFilename() {
        return filename;
    }

    public void setFilenamePrefix(String prefix) {
        filename = String.format("%s.%s", prefix, filename);
    }

    @JsonIgnore
    public InputStream getContent() {
        System.out.println("===============================");
        for ( String f : ftpClient.list()) {
            System.out.println("F:");
            System.out.println(f);
        }
        if (!dir.isEmpty()){
            ftpClient.cd(dir);
        }

        System.out.printf("Trying to get:%s in %s",filename, dir);
        System.out.println();
        return ftpClient.get(filename, FtpClient.FileType.BINARY);
    }

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

    public int compareTo(FileHarvest other) {
        return filename.compareTo(other.getFilename());
    }
}