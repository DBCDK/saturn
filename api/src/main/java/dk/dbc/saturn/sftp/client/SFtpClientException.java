/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.sftp.client;

public class SFtpClientException extends RuntimeException {
    public SFtpClientException(Exception e) {
        super(e);
    }
}
