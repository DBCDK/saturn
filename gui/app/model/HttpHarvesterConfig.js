/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import {BaseHarvesterConfig} from "./BaseHarvesterConfig";
import constants from "../constants";
import {HttpClient} from "../HttpClient";

import {getArgValue} from "../utils";

class HttpHarvesterConfig extends BaseHarvesterConfig {
    static listHttpHarvesterConfigs(start, limit) {
        start = getArgValue(start);
        limit = getArgValue(limit);
        return new HttpClient()
            .get(constants.endpoints.listHttpHarvesterConfigs);
    }
    static fromJson(json) {
        const config = super.fromJson(json);
        config.url = json.url;
        return config;
    }
}

export default HttpHarvesterConfig;
