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
}
