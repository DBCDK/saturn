/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileNameMatcherTest {
    @Test
    public void test_matches() {
        FileNameMatcher fileNameMatcher = new FileNameMatcher(
            "spongebob*-1.j?g");

        assertThat("matches 1", fileNameMatcher.matches(
            "spongebob-squarepants-1.jpg"), is(true));
        assertThat("matches 2", fileNameMatcher.matches(
            "spongebob-squarepants-1.jog"), is(true));
        assertThat("matches 3", fileNameMatcher.matches(
            "spongebob-1.jpg"), is(true));
        assertThat("doesn't match 1", fileNameMatcher.matches(
            "spongebob-squarepants-2.jpg"), is(false));
        assertThat("doesn't match 2", fileNameMatcher.matches(
            "spongebob-squarepants-1.jg"), is(false));
    }

    @Test
    public void test_matches_dot() {
        FileNameMatcher fileNameMatcher = new FileNameMatcher(
            "spongebob.jpg");
        assertThat("literal dot", fileNameMatcher.matches("spongebob-jpg"),
            is(false));
    }
}
