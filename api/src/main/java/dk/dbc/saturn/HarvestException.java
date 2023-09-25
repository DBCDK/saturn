/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

public class HarvestException extends Exception {
    public HarvestException(String msg) {
        super(msg);
    }

    public HarvestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
