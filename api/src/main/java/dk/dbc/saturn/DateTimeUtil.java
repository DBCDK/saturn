package dk.dbc.saturn;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    public static final ZoneId DK_ZONE = ZoneId.of("Europe/Copenhagen");
    public static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static final OffsetDateTime parseLocalDateTime(String date) {
        return LocalDateTime.parse(date, LOCAL_DATE_TIME_FORMATTER).atZone(DK_ZONE).toOffsetDateTime();
    }
}
