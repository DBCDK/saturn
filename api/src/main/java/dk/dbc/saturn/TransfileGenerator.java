/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import java.util.List;

/**
 * Handles transfile templates
 */
public class TransfileGenerator {
    /**
     * validates a transfile template
     * @param transfileTemplate transfile template
     * @return true if the transfile template validates
     */
    public static boolean validateTransfileTemplate(String transfileTemplate) {
        return !transfileTemplate.isEmpty() && !transfileTemplate.contains(
            "f=");
    }

    /**
     * generates actual transfile based on template and filenames
     * @param transfileTemplate transfile template
     * @param filenames names of data files
     * @return generated transfile
     */
    public static String generateTransfile(String transfileTemplate,
            List<String> filenames) {
        StringBuilder sb = new StringBuilder();
        for(String file : filenames) {
            sb.append(transfileTemplate).append(",f=").append(file)
                .append("\n");
        }
        sb.append("slut");
        return sb.toString();
    }
}
