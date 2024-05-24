/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class CutTest {

    @Test
    public void singleton() {
        final Cut cut = new Cut("2");
        assertThat(cut.of("abcdefghij"), is("b"));
    }

    @Test
    public void range() {
        final Cut cut = new Cut("3-6");
        assertThat(cut.of("abcdefghij"), is("cdef"));
    }

    @Test
    public void rangeOpenEndedFrom() {
        final Cut cut = new Cut("-6");
        assertThat(cut.of("abcdefghij"), is("abcdef"));
    }

    @Test
    public void rangeOpenEndedTo() {
        final Cut cut = new Cut("6-");
        assertThat(cut.of("abcdefghij"), is("fghij"));
    }

    @Test
    public void multipleRanges() {
        final Cut cut = new Cut("2,4,6-7,9-");
        assertThat(cut.of("abcdefghij"), is("bdfgij"));
    }

    @Test
    public void nullExpression() {
        assertThrows(IllegalArgumentException.class, () -> new Cut(null));
    }

    @Test
    public void emptyExpression() {
        assertThrows(IllegalArgumentException.class, () -> new Cut(" "));
    }

    @Test
    public void nonNumeric() {
        assertThrows(NumberFormatException.class, () -> new Cut("x-y"));
    }
}