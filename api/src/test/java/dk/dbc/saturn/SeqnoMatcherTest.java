/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.HttpHarvesterConfig;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SeqnoMatcherTest {
    @Test
    public void noSeqnoExpression() {
        final HttpHarvesterConfig config = newHarvesterConfig();
        config.setSeqnoExtract(null);
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher(config);
        assertThat(seqnoMatcher.shouldFetch("filename"), is(true));
        assertThat(seqnoMatcher.getSeqno(), is(nullValue()));
    }

    @Test
    public void invalidSeqnoExpression() {
        final HttpHarvesterConfig config = newHarvesterConfig();
        config.setSeqnoExtract("a-b");
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher(config);
        assertThat(seqnoMatcher.shouldFetch("filename"), is(false));
        assertThat(seqnoMatcher.getSeqno(), is(nullValue()));
    }

    @Test
    public void seqnoExpressionOutOfBounds() {
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher(newHarvesterConfig());
        assertThat(seqnoMatcher.shouldFetch("file"), is(false));
        assertThat(seqnoMatcher.getSeqno(), is(nullValue()));
    }

    @Test
    public void seqnoExpressionExtractsNonNumeric() {
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher(newHarvesterConfig());
        assertThat(seqnoMatcher.shouldFetch("filename"), is(false));
        assertThat(seqnoMatcher.getSeqno(), is(nullValue()));
    }

    @Test
    public void seqnoMatcher() {
        final HttpHarvesterConfig config = newHarvesterConfig();
        final SeqnoMatcher seqnoMatcher = new SeqnoMatcher(config);
        assertThat(seqnoMatcher.shouldFetch("file1234"), is(true));
        assertThat(seqnoMatcher.getSeqno(), is(1234));

        config.setSeqno(4242);
        assertThat(seqnoMatcher.shouldFetch("file1234"), is(false));
        assertThat(seqnoMatcher.getSeqno(), is(1234));
    }

    private HttpHarvesterConfig newHarvesterConfig() {
        final HttpHarvesterConfig config = new HttpHarvesterConfig();
        config.setName(this.getClass().getName());
        config.setSeqnoExtract("5-8");
        return config;
    }

}