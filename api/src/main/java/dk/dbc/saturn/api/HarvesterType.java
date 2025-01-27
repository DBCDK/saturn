package dk.dbc.saturn.api;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import dk.dbc.saturn.entity.FtpHarvesterConfig;
import dk.dbc.saturn.entity.HttpHarvesterConfig;
import dk.dbc.saturn.entity.SFtpHarvesterConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum HarvesterType {
    HTTP(HttpHarvesterConfig.class), FTP(FtpHarvesterConfig.class), SFTP(SFtpHarvesterConfig.class);

    public final Class<? extends AbstractHarvesterConfigEntity> configClass;
    private static final Map<String, HarvesterType> MAP = Stream.of(values()).collect(Collectors.toMap(Enum::name, e -> e));

    HarvesterType(Class<? extends AbstractHarvesterConfigEntity> configClass) {
        this.configClass = configClass;
    }

    public static HarvesterType of(String name) {
        if (name == null) return null;
        return MAP.get(name.toUpperCase());
    }
}
