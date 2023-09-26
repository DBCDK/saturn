package dk.dbc.saturn;

import dk.dbc.saturn.entity.HttpHarvesterConfig;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TestUtils {
    public static TimeZone TIME_ZONE = TimeZone.getTimeZone("Europe/Copenhagen");
    public static HttpHarvesterConfig getHttpHarvesterConfig() throws ParseException {
        HttpHarvesterConfig config = new HttpHarvesterConfig();
        config.setName("MyName'sNotRick!");
        config.setSchedule("* * * * *");
        config.setUrl("http://nick.com");
        config.setLastHarvested(getDate("2018-06-06T20:20:20"));
        config.setTransfile("b=databroendpr3,f=$DATAFIL,t=abmxml," +
                "clatin-1,o=littsiden,m=kildepost@dbc.dk");
        config.setAgency("010100");
        config.setId(1);
        config.setEnabled(true);
        return config;
    }

    public static Date getDate(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TIME_ZONE);
        return sdf.parse(date);
    }
}
