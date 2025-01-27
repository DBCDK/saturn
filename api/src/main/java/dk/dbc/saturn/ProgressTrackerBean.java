package dk.dbc.saturn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class ProgressTrackerBean {
    private static final Map<Integer, Progress> progressMap = new ConcurrentHashMap<>();
    @Inject
    private MetricRegistry metricRegistry;

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
        private static final Duration SLOW_JOB_THRESHOLD = Duration.ofHours(1);
        private final AtomicInteger currentFiles = new AtomicInteger(0);
        private final AtomicLong totalBytes = new AtomicLong(0);
        private Set<FileHarvest> harvests;
        private String message = null;
        private final Instant startTime = Instant.now();
        private boolean abort = false;
        private boolean done = false;
        private final Thread thread;

        public Progress() {
            thread = Thread.currentThread();
        }

        public void init(Set<FileHarvest> harvests) {
            this.harvests = harvests;
        }

        public Progress setMessage(String message) {
            this.message = message;
            return this;
        }

        public void done(int id, MetricRegistry metricRegistry) {
            done = true;
            Duration age = getAge();
            setMessage("Done in " + age.toSeconds() + "s");
            if(age.compareTo(SLOW_JOB_THRESHOLD) > 0) metricRegistry.timer("slow_jobs", new Tag("id", Integer.toString(id))).update(age);
        }

        public void abort() {
            abort = true;
            done = true;
            setMessage("Aborted");
            thread.interrupt();
        }

        public boolean isAbort() {
            return abort;
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

        @JsonIgnore
        public Duration getAge() {
            return Duration.between(startTime, Instant.now());
        }

        public String getMessage() {
            if(message != null) return message;
            if(getTotalFiles() == 0) return "Listing";
            Long bytesTransferred = getBytesTransferred();
            String transferred = FileUtils.byteCountToDisplaySize(bytesTransferred);
            if(totalBytes.get() != 0) return transferred + " " + PERCENTAGE_FORMAT.format(100d * bytesTransferred / totalBytes.get()) + "%";
            return transferred + " " + PERCENTAGE_FORMAT.format(100d * getCurrentFiles() / getTotalFiles()) + "%";
        }

        public boolean isRunning() {
            return !done;
        }
    }
}
