/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.entity;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "httpharvester")
@NamedQueries({
    @NamedQuery(
        name = HttpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME,
        query = HttpHarvesterConfig.GET_HARVESTER_CONFIGS_QUERY
    )
})
public class HttpHarvesterConfig extends AbstractHarvesterConfigEntity {
    public static final String GET_HARVESTER_CONFIGS_NAME =
        "HttpHarvesterConfig.getHarvesterConfigs";
    public static final String GET_HARVESTER_CONFIGS_QUERY =
        "SELECT config FROM HttpHarvesterConfig config ORDER BY config.id DESC";

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
