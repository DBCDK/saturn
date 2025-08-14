package dk.dbc.saturn;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface UrlTokensSubstitutor {
    default String substituteRelativeUTC(String url) {
        if(url == null || url.isEmpty()) return url;
        Pattern pattern = Pattern.compile("\\$\\{utc\\(([^}]+)\\)}");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            OffsetDateTime time = now().atOffset(ZoneOffset.UTC).minus(Duration.parse(matcher.group(1)));
            return matcher.replaceAll(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(time));
        }
        return url;
    }

    default Instant now() {
        return Instant.now();
    }
}
