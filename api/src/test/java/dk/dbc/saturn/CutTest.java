/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CutTest {
    @Test
    void singleton() {
        final Cut cut = new Cut("2");
        assertThat(cut.of("abcdefghij"), is("b"));
    }

    @Test
    void range() {
        final Cut cut = new Cut("3-6");
        assertThat(cut.of("abcdefghij"), is("cdef"));
    }

    @Test
    void rangeOpenEndedFrom() {
        final Cut cut = new Cut("-6");
        assertThat(cut.of("abcdefghij"), is("abcdef"));
    }

    @Test
    void rangeOpenEndedTo() {
        final Cut cut = new Cut("6-");
        assertThat(cut.of("abcdefghij"), is("fghij"));
    }

    @Test
    void multipleRanges() {
        final Cut cut = new Cut("2,4,6-7,9-");
        assertThat(cut.of("abcdefghij"), is("bdfgij"));
    }

    @Test
    void nullExpression() {
        assertThrows(IllegalArgumentException.class, () -> new Cut(null));
    }

    @Test
    void emptyExpression() {
        assertThrows(IllegalArgumentException.class, () -> new Cut(" "));
    }

    @Test
    void nonNumeric() {
        assertThrows(NumberFormatException.class, () -> new Cut("x-y"));
    }
}