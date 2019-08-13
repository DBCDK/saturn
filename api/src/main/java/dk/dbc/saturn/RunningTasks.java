package dk.dbc.saturn;

import dk.dbc.saturn.entity.FtpHarvesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class RunningTasks {
    private final static HashMap<Integer, Set<FileHarvest>> runningHarvestTasks = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RunningTasks.class);

    public boolean isRunning( FtpHarvesterConfig config ) {

        return runningHarvestTasks.containsKey( config.getId() );
    }

    public void remove( FtpHarvesterConfig config ) {
        if (isRunning(config) ) {
            runningHarvestTasks.remove( config.getId() );
        }
        LOGGER.info( "Removed task {}. NOW the list of running tasks is:{}",
                config.getId(),
                String.join(",",runningHarvestTasks.keySet().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList())));
    }

    public void add( FtpHarvesterConfig config, Set<FileHarvest> fileHarvests ) throws HarvestException {
        if ( isRunning( config) ) {
            throw new HarvestException( String.format("%s already running.", config.getName()));
        }
        runningHarvestTasks.put( config.getId(), fileHarvests );
    }

    public int size() { return runningHarvestTasks.size(); }


}