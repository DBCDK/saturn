/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import java.io.InputStream;

public interface FileHarvest {
     String getFilename();
     String getUploadFilename( String prefix );
     Integer getSeqno();
     InputStream getContent() throws HarvestException;
     void close();
}
