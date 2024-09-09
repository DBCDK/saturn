package dk.dbc.saturn;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.MetricRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
public class RunningTasks {
    private final Map<Integer, Instant> runningHarvestTasks = new ConcurrentHashMap<>();
    @Inject
    private MetricRegistry metricRegistry;

    @PostConstruct
    public void init() {
        metricRegistry.gauge("running_tasks", this::size);
        metricRegistry.gauge("longest_running_task", () -> getLongestRunningTask().toSeconds());
    }

    public void run(Integer id, Consumer<Void> block) {
        if(runningHarvestTasks.containsKey(id)) return;
        runningHarvestTasks.put(id, Instant.now());
        try {
            block.accept(null);
        } finally {
            runningHarvestTasks.remove(id);
        }
    }

    public int size() { return runningHarvestTasks.size(); }

    public Duration getLongestRunningTask() {
        Map<Integer, Instant> snapshot = new HashMap<>(runningHarvestTasks);
        Instant now = Instant.now();
        return snapshot.values().stream()
                .reduce((a, b) -> a.isAfter(b) ? a : b)
                .map(i -> Duration.between(i, now))
                .orElse(Duration.ZERO);
    }
}
