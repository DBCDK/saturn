/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractIntegrationTest {
    final static HarvesterConfigRepository harvesterConfigRepository =
        new HarvesterConfigRepository();

    final static UriBuilder mockedUriBuilder = mock(UriBuilder.class);

    @BeforeAll
    public static void setUp() throws URISyntaxException {
        final PGSimpleDataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        EntityManager entityManager = createEntityManager(dataSource,
            "saturnIT_PU");
        harvesterConfigRepository.entityManager = entityManager;
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));
    }

    @BeforeEach
    void beginTransaction() {
        harvesterConfigRepository.entityManager.getTransaction().begin();
    }

    @AfterEach
    void resetDatabase() {
        if(harvesterConfigRepository.entityManager.getTransaction().isActive()) {
            harvesterConfigRepository.entityManager.getTransaction().commit();
        }
        harvesterConfigRepository.entityManager.getTransaction().begin();
        harvesterConfigRepository.entityManager.createNativeQuery(
            "DELETE FROM httpharvester").executeUpdate();
        harvesterConfigRepository.entityManager.createNativeQuery(
            "DELETE FROM ftpharvester").executeUpdate();
        harvesterConfigRepository.entityManager.getTransaction().commit();
    }

    private static PGSimpleDataSource getDataSource() {
        final PGSimpleDataSource datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("saturn");
        datasource.setServerName("localhost");
        datasource.setPortNumber(Integer.parseInt(System.getProperty(
            "postgresql.port", "5432")));
        datasource.setUser(System.getProperty("user.name"));
        datasource.setPassword(System.getProperty("user.name"));
        return datasource;
    }


    private static EntityManager createEntityManager(
        PGSimpleDataSource dataSource, String persistenceUnitName) {
        Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put(JDBC_USER, dataSource.getUser());
        entityManagerProperties.put(JDBC_PASSWORD, dataSource.getPassword());
        entityManagerProperties.put(JDBC_URL, dataSource.getUrl());
        entityManagerProperties.put(JDBC_DRIVER, "org.postgresql.Driver");
        entityManagerProperties.put("eclipselink.logging.level", "FINE");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName,
            entityManagerProperties);
        return factory.createEntityManager(entityManagerProperties);
    }

    private static void migrateDatabase(PGSimpleDataSource datasource) {
        final DatabaseMigrator migrator = new DatabaseMigrator(datasource);
        migrator.migrate();
    }
}
