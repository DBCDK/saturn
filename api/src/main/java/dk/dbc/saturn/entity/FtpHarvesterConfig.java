/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "ftpharvester")
@NamedQueries({
    @NamedQuery(
        name = FtpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME,
        query = FtpHarvesterConfig.GET_HARVESTER_CONFIGS_QUERY
    ),
    @NamedQuery(
        name = FtpHarvesterConfig.GET_HARVESTER_CONFIG_BY_ID_NAME,
        query = FtpHarvesterConfig.GET_HARVESTER_CONFIG_BY_ID_QUERY
    ),
    @NamedQuery(
        name = FtpHarvesterConfig.GET_HARVESTER_CONFIG_COUNT_BY_ID_NAME,
        query = FtpHarvesterConfig.GET_HARVESTER_CONFIG_COUNT_BY_ID_QUERY
    )
})
public class FtpHarvesterConfig extends AbstractHarvesterConfigEntity {
    public static final String GET_HARVESTER_CONFIGS_NAME =
        "FtpHarvesterConfig.getHarvesterConfigs";
    public static final String GET_HARVESTER_CONFIGS_QUERY =
        "SELECT config FROM FtpHarvesterConfig config WHERE config.id >= :start " +
        "ORDER BY config.id DESC";
    public static final String GET_HARVESTER_CONFIG_BY_ID_NAME =
        "FtpHarvesterConfig.getHarvesterConfigById";
    public static final String GET_HARVESTER_CONFIG_BY_ID_QUERY =
        "SELECT config FROM FtpHarvesterConfig config WHERE config.id = :id";
    public static final String GET_HARVESTER_CONFIG_COUNT_BY_ID_NAME =
        "FtpHarvesterConfig.getHarvesterConfigCountById";
    static final String GET_HARVESTER_CONFIG_COUNT_BY_ID_QUERY =
        "SELECT COUNT(config.id) FROM FtpHarvesterConfig config WHERE config.id = :id";

    private String host;
    private int port;
    private String username;
    private String password;
    private String dir;
    @Column(name = "filespattern")
    private String filesPattern;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getFilesPattern() {
        return filesPattern;
    }

    public void setFilesPattern(String filesPattern) {
        this.filesPattern = filesPattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FtpHarvesterConfig)) return false;

        FtpHarvesterConfig that = (FtpHarvesterConfig) o;

        return super.equals(o)
            && host.equals(that.host)
            && port == that.port
            && username.equals(that.username)
            && password.equals(that.username)
            && dir.equals(that.dir)
            && filesPattern.equals(that.filesPattern);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        result = 31 * result + username.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + dir.hashCode();
        result = 31 * result + filesPattern.hashCode();
        return result;
    }
}
