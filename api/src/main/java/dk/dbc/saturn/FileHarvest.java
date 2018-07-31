/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import java.io.InputStream;
import java.util.Objects;

public class FileHarvest {
    private final String filename;
    private final InputStream content;
    private final Integer seqno;

    public FileHarvest(String filename, InputStream content, Integer seqno) {
        this.filename = filename;
        this.content = content;
        this.seqno = seqno;
    }

    public String getFilename() {
        return filename;
    }

    public InputStream getContent() {
        return content;
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
        FileHarvest that = (FileHarvest) o;
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
}