package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RunningTasks {
    private final Set<Integer> runningHarvestTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Logger LOGGER = LoggerFactory.getLogger(RunningTasks.class);


    public void run(AbstractHarvesterConfigEntity config, HarvestTask block) throws HarvestException {
        if(runningHarvestTasks.contains(config)) return;
        runningHarvestTasks.add(config.getId());
        try {
            block.accept();
        } finally {
            runningHarvestTasks.remove(config.getId());
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int size() { return runningHarvestTasks.size(); }

    public interface HarvestTask {
        void accept() throws HarvestException;
    }
}
