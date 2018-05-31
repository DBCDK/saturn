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
    )
})
public class FtpHarvesterConfig extends AbstractHarvesterConfigEntity {
    public static final String GET_HARVESTER_CONFIGS_NAME =
        "FtpHarvesterConfig.getHarvesterConfigs";
    public static final String GET_HARVESTER_CONFIGS_QUERY =
        "SELECT config FROM FtpHarvesterConfig config ORDER BY config.id DESC";
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
}
