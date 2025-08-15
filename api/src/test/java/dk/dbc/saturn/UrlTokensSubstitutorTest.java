package dk.dbc.saturn;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class UrlTokensSubstitutorTest {

    @Test
    public void substituteRelativeUTCTest() {
        Instant now = Instant.now();
        String time = now.minus(Duration.ofDays(7)).truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", "");
        String result = new UrlTokensSubstitutor() {
            @Override
            public Instant now() {
                return now;
            }
        }.substituteRelativeUTC("http://onix.pubhub.dk/v4/products?fromUtc=${utc(P7D)}");
        Assert.assertEquals("http://onix.pubhub.dk/v4/products?fromUtc=" + time, result);
    }
}
