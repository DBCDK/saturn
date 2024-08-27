package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RunningTasks {
    private final Set<Integer> runningHarvestTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void run(AbstractHarvesterConfigEntity config, HarvestTask block) throws HarvestException {
        Integer id = config.getId();
        if(runningHarvestTasks.contains(id)) return;
        runningHarvestTasks.add(id);
        try {
            block.accept();
        } finally {
            runningHarvestTasks.remove(id);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int size() { return runningHarvestTasks.size(); }

    public interface HarvestTask {
        void accept() throws HarvestException;
    }
}
