/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import {BaseHarvesterConfig} from "./BaseHarvesterConfig";
import constants from "../constants";

class HttpHarvesterConfig extends BaseHarvesterConfig {
    static addHttpHarvesterConfig(config) {
        return BaseHarvesterConfig.addHarvesterConfig(
            constants.endpoints.addHttpHarvesterConfig, config);
    }
    static fetchConfig(id) {
        return BaseHarvesterConfig.fetchConfig(
            constants.endpoints.getHttpHarvesterById, id);
    }
    static listHttpHarvesterConfigs(start, limit) {
        return BaseHarvesterConfig.listHarvesterConfigs(
            constants.endpoints.listHttpHarvesterConfigs, start, limit);
    }
    static testConfig(id) {
        return BaseHarvesterConfig.testConfig(
            constants.endpoints.testHttpHarvesterById, id);
    }
    static fromJson(json) {
        const config = super.fromJson(json);
        config.url = json.url;
        config.urlPattern = json.urlPattern;
        config.lastHarvested = json.lastHarvested;
        config.listFilesHandler = json.listFilesHandler;
        return config;
    }
}

export default HttpHarvesterConfig;
