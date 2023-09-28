/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "sftpharvester")
@NamedQueries({
    @NamedQuery(
        name = SFtpHarvesterConfig.GET_HARVESTER_CONFIGS_NAME,
        query = SFtpHarvesterConfig.GET_HARVESTER_CONFIGS_QUERY
    ),
    @NamedQuery(
        name = SFtpHarvesterConfig.GET_HARVESTER_CONFIG_BY_ID_NAME,
        query = SFtpHarvesterConfig.GET_HARVESTER_CONFIG_BY_ID_QUERY
    ),
    @NamedQuery(
        name = SFtpHarvesterConfig.GET_HARVESTER_CONFIG_COUNT_BY_ID_NAME,
        query = SFtpHarvesterConfig.GET_HARVESTER_CONFIG_COUNT_BY_ID_QUERY
    )
})
public class SFtpHarvesterConfig extends AbstractHarvesterConfigEntity {
    public static final String GET_HARVESTER_CONFIGS_NAME =
        "SFtpHarvesterConfig.getHarvesterConfigs";
    public static final String GET_HARVESTER_CONFIGS_QUERY =
        "SELECT config FROM SFtpHarvesterConfig config WHERE config.id >= :start " +
        "ORDER BY config.id DESC";
    public static final String GET_HARVESTER_CONFIG_BY_ID_NAME =
        "SFtpHarvesterConfig.getHarvesterConfigById";
    public static final String GET_HARVESTER_CONFIG_BY_ID_QUERY =
        "SELECT config FROM SFtpHarvesterConfig config WHERE config.id = :id";
    public static final String GET_HARVESTER_CONFIG_COUNT_BY_ID_NAME =
        "SFtpHarvesterConfig.getHarvesterConfigCountById";
    static final String GET_HARVESTER_CONFIG_COUNT_BY_ID_QUERY =
        "SELECT COUNT(config.id) FROM SFtpHarvesterConfig config WHERE config.id = :id";

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
        if (!(o instanceof SFtpHarvesterConfig)) return false;

        SFtpHarvesterConfig that = (SFtpHarvesterConfig) o;

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

    @Override
    public String toString() {
        return "SFtpHarvesterConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", dir='" + dir + '\'' +
                ", filesPattern='" + filesPattern + '\'' +
                '}';
    }
}
