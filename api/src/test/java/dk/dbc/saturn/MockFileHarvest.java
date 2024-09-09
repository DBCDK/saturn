package dk.dbc.saturn;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.ByteArrayInputStream;
import java.util.Objects;

public class MockFileHarvest implements FileHarvest, Comparable<FileHarvest> {
    private String filename;
    private final Integer seqno;
    private String content;

    public MockFileHarvest(String filename, String content, int seqno) {
        this.filename=filename;
        this.content=content;
        this.seqno=seqno;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public Number getSize() {
        return 1;
    }

    @Override
    public Number getBytesTransferred() {
        return 1;
    }

    @Override
    public String getUploadFilename(String prefix) {
        return String.format("%s.%s", prefix, filename);
    }

    @Override
    public Integer getSeqno() {
        return seqno;
    }

    @Override
    public Status getStatus() {
        return null;
    }

    @Override
    @JsonIgnore
    public ByteCountingInputStream getContent() {
        return new ByteCountingInputStream(new ByteArrayInputStream(content.toString().getBytes()));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileHarvest that = (FileHarvest) o;
        return Objects.equals(filename, that.getFilename());
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

    }

    public int compareTo(FileHarvest other) {
        return filename.compareTo(other.getFilename());
    }
}
