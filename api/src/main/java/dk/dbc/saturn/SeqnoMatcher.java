/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import dk.dbc.saturn.entity.AbstractHarvesterConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeqnoMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeqnoMatcher.class);
    private final AbstractHarvesterConfigEntity config;

    private Integer seqno;

    public SeqnoMatcher(AbstractHarvesterConfigEntity config) {
        this.config = config;
    }

    public boolean shouldFetch(String filename) {
        seqno = null;
        final Cut cut;
        try {
            cut = new Cut(config.getSeqnoExtract());
        } catch (NumberFormatException e) {
            LOGGER.error("harvest task for {} has invalid seqno expression {}",
                    config.getName(), config.getSeqnoExtract());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }

        final String nextSeqnoString;
        try {
            nextSeqnoString = cut.of(filename);
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.error("harvest task for {} has out of bounds seqno expression {}",
                    config.getName(), config.getSeqnoExtract());
            return false;
        }

        try {
            seqno = Integer.parseInt(nextSeqnoString);
            final Integer currentSeqno = config.getSeqno();
            return currentSeqno == null || seqno > currentSeqno;
        } catch (NumberFormatException e) {
            LOGGER.error("harvest task for {} extracted illegal seqno {}",
                    config.getName(), nextSeqnoString);
            return false;
        }
    }

    public Integer getSeqno() {
        return seqno;
    }
}
