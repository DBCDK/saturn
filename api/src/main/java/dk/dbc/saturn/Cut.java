/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts parts of string
 */
public class Cut {
    private final List<CharacterRange> characterRanges;

    /**
     * Instantiate new cut expression
     * @param expression comma separated string of ranges
     *        where each range is on the form:
     *          N      N'th character counted from 1
     *          N-     from N'th character to end of line
     *          N-M    from N'th to M'th (included) character
     *          -M     from first to M'th (included) character
     * @throws IllegalArgumentException on null valued or empty expression
     */
    public Cut(String expression) throws IllegalArgumentException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid cut expression: '" + expression + "'");
        }
        characterRanges = parseCharacterRangesExpression(expression);
    }

    /**
     * Cuts sections from given string
     * @param str string to be cut
     * @return string consisting of cut sections
     */
    public String of(String str) {
        return characterRanges.stream()
                .map(characterRange -> characterRange.of(str))
                .collect(Collectors.joining(""));
    }

    private List<CharacterRange> parseCharacterRangesExpression(String characterRangesExpression) {
        final ArrayList<CharacterRange> characterRanges = new ArrayList<>();
        if (characterRangesExpression != null && !characterRangesExpression.isEmpty()) {
            characterRangesExpression = characterRangesExpression.replaceAll("\\s+", "");
            for (String range : characterRangesExpression.split(",")) {
                characterRanges.add(parseCharacterRangeExpression(range));
            }
        }
        return characterRanges;
    }

    private CharacterRange parseCharacterRangeExpression(String characterRangeExpression) {
        final String[] parts = characterRangeExpression.split("-", 2);
        if (parts.length == 2) {
            return new CharacterRange(parts[0], parts[1]);
        }
        return new CharacterRange(parts[0], parts[0]);
    }

    private static class CharacterRange {
        final int from;
        final int to;

        CharacterRange(String from, String to) {
            if (from == null || from.isEmpty()) {
                this.from = 0;
            } else {
                this.from = Integer.parseInt(from) - 1;
            }
            if (to == null || to.isEmpty()) {
                this.to = -1;
            } else {
                this.to = Integer.parseInt(to);
            }
        }

        String of(String str) {
            return str.substring(from, to == -1 ? str.length() : to);
        }
    }
}
