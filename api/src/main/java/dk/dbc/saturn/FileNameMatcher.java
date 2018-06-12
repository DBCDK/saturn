/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses a simple glob pattern with * and ?
 * and matches it against strings
 */
public class FileNameMatcher {
    private final Pattern pattern;

    public FileNameMatcher(String globPattern) {
        final String regex = globPatternToRegex(globPattern);
        pattern = Pattern.compile(regex);
    }

    private String globPatternToRegex(String globPattern) {
        return globPattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
    }

    /**
     * matches the glob pattern against a filename
     * @param filename filename to check
     * @return true if the glob pattern matches the filename
     */
    public boolean matches(String filename) {
        Matcher matcher = pattern.matcher(filename);
        return matcher.matches();
    }
}
