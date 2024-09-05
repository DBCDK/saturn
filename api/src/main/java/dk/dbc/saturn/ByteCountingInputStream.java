package dk.dbc.saturn;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * count the number of bytes read through the stream
 */
public class ByteCountingInputStream extends InputStream {
    private final AtomicLong count = new AtomicLong();
    private long marked = -1;
    private InputStream is;

    public ByteCountingInputStream setCount(long count) {
        this.count.set(count);
        return this;
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int read() throws IOException {
        int r = is.read();
        if (r > 0) {
            count.incrementAndGet();
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int r = is.read(b, off, len);
        if (r > 0) {
            count.addAndGet(r);
        }
        return r;
    }

    @Override
    public long skip(long skipped) throws IOException {
        long l = is.skip(skipped);
        if (l > 0) {
            count.addAndGet(l);
        }
        return l;
    }

    @Override
    public synchronized void mark(int readlimit) {
        is.mark(readlimit);
        marked = count.get();
    }

    @Override
    public synchronized void reset() throws IOException {
        is.reset();
        count.set(marked);
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    /**
     * get the actual number of bytes read
     *
     * @return a long, the number of bytes read
     */
    public long getBytesRead() {
        return count.get();
    }

    public ByteCountingInputStream(InputStream is) {
        this.is = is;
    }
}
