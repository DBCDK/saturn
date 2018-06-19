/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import {BaseHarvesterConfig} from "./BaseHarvesterConfig";
import constants from "../constants";
import {HttpClient} from "../HttpClient";

import {getArgValue} from "../utils";

class HttpHarvesterConfig extends BaseHarvesterConfig {
    static addHttpHarvesterConfig(config) {
        return new HttpClient()
            .addHeaders({"Content-type": "application/json"})
            .post(constants.endpoints.addHttpHarvesterConfig, null, null, config);
    }
    static fetchConfig(id) {
        const params = new Map();
        params.set("id", id);
        return new HttpClient()
            .get(constants.endpoints.getHttpHarvesterById, params);
    }
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
