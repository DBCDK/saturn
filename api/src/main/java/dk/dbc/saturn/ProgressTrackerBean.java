package dk.dbc.saturn;

import jakarta.ejb.Singleton;
import org.apache.commons.io.FileUtils;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class ProgressTrackerBean {
    private static final Map<Integer, Progress> progressMap = new ConcurrentHashMap<>();

    public Progress add(Integer configId) {
        Progress progress = new Progress();
        progressMap.put(configId, progress);
        return progress;
    }

    public Progress get(Integer configId) {
        return progressMap.get(configId);
    }

    public static class Progress {
        private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("0.0");
        private final AtomicInteger currentFiles = new AtomicInteger(0);
        private final AtomicLong totalBytes = new AtomicLong(0);
        private Set<FileHarvest> harvests;
        private String message = null;

        public Progress() {
        }

        public void init(Set<FileHarvest> harvests) {
            this.harvests = harvests;
        }

        public Progress setMessage(String message) {
            this.message = message;
            return this;
        }

        public int inc() {
            return currentFiles.incrementAndGet();
        }

        public int getCurrentFiles() {
            return currentFiles.get();
        }

        public int getTotalFiles() {
            return harvests == null ? 0 : harvests.size();
        }

        public void setTotalBytes(long totalBytes) {
            this.totalBytes.set(totalBytes);
        }

        public Long getBytesTransferred() {
            if(harvests == null) return null;
            return harvests.stream().map(FileHarvest::getBytesTransferred).filter(Objects::nonNull).mapToLong(Number::longValue).sum();
        }

        public String getMessage() {
            if(message != null) return message;
            if(getTotalFiles() == 0) return "listing";
            Long bytesTransferred = getBytesTransferred();
            String transferred = FileUtils.byteCountToDisplaySize(bytesTransferred);
            if(totalBytes.get() != 0) return transferred + " " + PERCENTAGE_FORMAT.format(100d * bytesTransferred / totalBytes.get()) + "%";
            return transferred + " " + PERCENTAGE_FORMAT.format(100d * getCurrentFiles() / getTotalFiles()) + "%";
        }

        public boolean isRunning() {
            return harvests != null && currentFiles.get() < harvests.size();
        }
    }
}
