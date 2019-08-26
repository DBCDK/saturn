package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Lock(LockType.WRITE)
public class RunningTasks {
    private final Set<Integer> runningHarvestTasks = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RunningTasks.class);

    public boolean isRunning( AbstractHarvesterConfigEntity config ) {

        return runningHarvestTasks.contains( config.getId() );
    }

    public  void remove( AbstractHarvesterConfigEntity config ) {
        if (isRunning(config) ) {
            runningHarvestTasks.remove( config.getId() );
        }
        LOGGER.info( "Removed task {}. NOW the list of running tasks is:{}",
                config.getId(),
                String.join(",",runningHarvestTasks.stream()
                        .map(String::valueOf).collect(Collectors.toList())));
    }

    public void add( AbstractHarvesterConfigEntity config ) throws HarvestException {
        if ( isRunning( config) ) {
            throw new HarvestException( String.format("%s already running.", config.getName()));
        }
        runningHarvestTasks.add( config.getId() );
    }

    public int size() { return runningHarvestTasks.size(); }


}