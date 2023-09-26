/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.saturn;

import java.io.InputStream;

public interface FileHarvest {
     enum Status {
          SKIPPED_BY_FILENAME,
          SKIPPED_BY_SEQNO,
          AWAITING_DOWNLOAD
     }

     String getFilename();
     String getUploadFilename(String prefix);
     Integer getSeqno();
     Status getStatus();
     InputStream getContent() throws HarvestException;
     default void setResumePoint(long resumePoint) throws HarvestException {
          throw new HarvestException("Resume point not available for this harvester");
     }
     default boolean isResumable() {
          return false;
     }
     void close();
}
