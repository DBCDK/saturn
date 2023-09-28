/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.rest;

import dk.dbc.saturn.api.ConfigFieldApi;
import dk.dbc.saturn.api.HarvesterConfigApi;
import dk.dbc.saturn.api.PasswordRepositoryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
// application path must not be "/" if webapp/index.html is to be loaded
@ApplicationPath("/api")
public class Saturn extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Saturn.class);

    private static final Set<Class<?>> classes = new HashSet<>();
    static {
        classes.add(HarvesterConfigApi.class);
        classes.add(ConfigFieldApi.class);
        classes.add(PasswordRepositoryApi.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        for (Class<?> clazz : classes) {
            LOGGER.info("Registered {} resource", clazz.getName());
        }
        return classes;
    }
}
