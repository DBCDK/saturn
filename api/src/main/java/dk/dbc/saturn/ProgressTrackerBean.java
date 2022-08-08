package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;

import javax.ejb.Singleton;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class ProgressTrackerBean {
    private final Map<Key, Progress> progressMap = new ConcurrentHashMap<>();

    public Progress get(Key key, int total) {
        return progressMap.computeIfAbsent(key, k -> new Progress(total));
    }

    public Progress get(Key key) {
        return progressMap.get(key);
    }

    public static class Progress {
        private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.0");
        private final AtomicInteger current = new AtomicInteger();
        private final AtomicInteger total;
        public Progress(int total) {
            this.total = new AtomicInteger(total);
        }

        public int inc() {
            return current.incrementAndGet();
        }

        public int getCurrent() {
            return current.get();
        }

        public int getTotal() {
            return total.get();
        }

        public String getPercentage() {
            return PERCENTAGE_FORMAT.format(100d * current.get() / total.get()) + "%";
        }

        public boolean isRunning() {
            return current.get() < total.get();
        }
    }

    public static class Key {
        private final Class<? extends AbstractHarvesterConfigEntity> harvesterType;
        private final int id;

        public Key(Class<? extends AbstractHarvesterConfigEntity> harvesterType, int id) {
            this.harvesterType = harvesterType;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key that = (Key) o;
            return id == that.id && harvesterType.equals(that.harvesterType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(harvesterType, id);
        }
    }
}
