/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

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
     * list http harvester configs
     * @return list of http harvester configs
     */
    public List<HttpHarvesterConfig> listHttpHarvesterConfigs() {
        TypedQuery<HttpHarvesterConfig> query = entityManager
            .createNamedQuery(HttpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME,
                HttpHarvesterConfig.class);
        return query.getResultList();
    }

    /**
     * list ftp harvester configs
     * @return list of ftp harvester configs
     */
    public List<FtpHarvesterConfig> listFtpHarvesterConfigs() {
        TypedQuery<FtpHarvesterConfig> query = entityManager
            .createNamedQuery(FtpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME,
                FtpHarvesterConfig.class);
        return query.getResultList();
    }
}
