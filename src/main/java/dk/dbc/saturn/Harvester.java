/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn;

import javax.validation.constraints.NotNull;
import java.io.InputStream;

public interface Harvester {
    InputStream harvest(@NotNull String url) throws HarvestException;
}
