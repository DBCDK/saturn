/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Stateless
public class HarvesterConfigRepository {
    @PersistenceContext(unitName = "saturn_PU")
    EntityManager entityManager;

    /**
     * list harvester configs
     * @param type type of harvester config to list
     * @param start harvester config id to start list from
     * @param limit limit of results
     * @param <T> type parameter
     * @return list of harvester configs
     */
    public <T extends AbstractHarvesterConfigEntity> List<T> list(
            Class<T> type, int start, int limit) {
        InvariantUtil.checkNotNullOrThrow(type, "type");
        String queryName;
        if (type == FtpHarvesterConfig.class) {
            queryName = FtpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME;
        } else if (type == HttpHarvesterConfig.class) {
            queryName = HttpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME;
        } else {
            throw new IllegalArgumentException(String.format(
                "unknown type %s", type));
        }
        TypedQuery<T> query = entityManager.createNamedQuery(queryName, type);
        query.setParameter("start", start);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    /**
     * get harvester config by id
     * @param type type of harvester config
     * @param id id of harvester config
     * @param <T> type parameter
     * @return optional harvester config, none if no config with the given
     * id is found
     */
    public <T extends AbstractHarvesterConfigEntity> Optional<T> getHarvesterConfig(
            Class<T> type, int id) {
        InvariantUtil.checkNotNullOrThrow(type, "type");
        String queryName;
        if(type == FtpHarvesterConfig.class) {
            queryName = FtpHarvesterConfig.GET_HARVESTER_CONFIG_BY_ID_NAME;
        } else if(type == HttpHarvesterConfig.class) {
            queryName = HttpHarvesterConfig.GET_HARVESTER_CONFIG_BY_ID_NAME;
        } else {
            throw new IllegalArgumentException(String.format(
                "unknown type: %s", type));
        }
        TypedQuery<T> query = entityManager.createNamedQuery(queryName, type);
        query.setParameter("id", id);
        query.setMaxResults(1);
        if(query.getResultList().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(query.getSingleResult());
    }

    /**
     * persist harvester config entity in database
     * @param entity harvester config entity
     * @param <T> entity type parameter
     */
    public <T extends AbstractHarvesterConfigEntity> void add(T entity) {
        entityManager.persist(entity);
    }
}
