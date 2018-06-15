/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import {BaseHarvesterConfig} from "./BaseHarvesterConfig";

class FtpHarvesterConfig extends BaseHarvesterConfig {
    static fromJson(json) {
        const config = super.fromJson(json);
        config.host = json.host;
        config.port = json.port;
        config.username = json.username;
        config.password = json.password;
        config.dir = json.dir;
        config.filesPattern = json.filesPattern;
        return config;
    }
}

export default FtpHarvesterConfig;
