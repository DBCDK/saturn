/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import org.slf4j.MDC;

public class HarvesterMDC implements AutoCloseable {
    private static final String HARVESTER = "HARVESTER";

    public HarvesterMDC(AbstractHarvesterConfigEntity config) {
        MDC.put(HARVESTER, config.getName());
    }

    @Override
    public void close() {
        MDC.remove(HARVESTER);
    }
}
