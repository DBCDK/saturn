/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "httpharvester")
@NamedQueries({
    @NamedQuery(
        name = HttpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME,
        query = HttpHarvesterConfig.GET_HARVESTER_CONFIGS_QUERY
    ),
    @NamedQuery(
        name = HttpHarvesterConfig.GET_HARVESTER_CONFIG_BY_ID_NAME,
        query = HttpHarvesterConfig.GET_HARVESTER_CONFIG_BY_ID_QUERY
    ),
    @NamedQuery(
        name = HttpHarvesterConfig.GET_HARVESTER_CONFIG_COUNT_BY_ID_NAME,
        query = HttpHarvesterConfig.GET_HARVESTER_CONFIG_COUNT_BY_ID_QUERY
    )
})
public class HttpHarvesterConfig extends AbstractHarvesterConfigEntity {
    public static final String GET_HARVESTER_CONFIGS_NAME =
        "HttpHarvesterConfig.getHarvesterConfigs";
    public static final String GET_HARVESTER_CONFIGS_QUERY =
        "SELECT config FROM HttpHarvesterConfig config WHERE config.id >= :start " +
        "ORDER BY config.id DESC";
    public static final String GET_HARVESTER_CONFIG_BY_ID_NAME =
        "HttpHarvesterConfig.getHarvesterConfigById";
    public static final String GET_HARVESTER_CONFIG_BY_ID_QUERY =
        "SELECT config FROM HttpHarvesterConfig config WHERE config.id = :id";
    public static final String GET_HARVESTER_CONFIG_COUNT_BY_ID_NAME =
        "HttpHarvesterConfig.getHarvesterConfigCountById";
    static final String GET_HARVESTER_CONFIG_COUNT_BY_ID_QUERY =
        "SELECT COUNT(config.id) FROM HttpHarvesterConfig config WHERE config.id = :id";

    public enum ListFilesHandler {
        STANDARD
    }

    private String url;
    private String urlPattern;

    @Enumerated(EnumType.STRING)
    private ListFilesHandler listFilesHandler = ListFilesHandler.STANDARD;

    public String getUrl() {
        return url;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public ListFilesHandler getListFilesHandler() {
        return listFilesHandler;
    }

    public void setListFilesHandler(ListFilesHandler listFilesHandler) {
        this.listFilesHandler = listFilesHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        HttpHarvesterConfig that = (HttpHarvesterConfig) o;

        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }
        if (urlPattern != null ? !urlPattern.equals(that.urlPattern) : that.urlPattern != null) {
            return false;
        }
        return listFilesHandler == that.listFilesHandler;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (urlPattern != null ? urlPattern.hashCode() : 0);
        result = 31 * result + (listFilesHandler != null ? listFilesHandler.hashCode() : 0);
        return result;
    }
}
