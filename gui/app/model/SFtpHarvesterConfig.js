/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import {BaseHarvesterConfig} from "./BaseHarvesterConfig";
import constants from "../constants";

class SFtpHarvesterConfig extends BaseHarvesterConfig {
    static addSFtpHarvesterConfig(config) {
        return BaseHarvesterConfig.addHarvesterConfig(
            constants.endpoints.addSFtpHarvesterConfig, config);
    }
    static fetchConfig(id) {
        return BaseHarvesterConfig.fetchConfig(
            constants.endpoints.getSFtpHarvesterById, id);
    }
    static listSFtpHarvesterConfigs(start, limit) {
        return BaseHarvesterConfig.listHarvesterConfigs(
            constants.endpoints.listSFtpHarvesterConfigs, start, limit);
    }
    static testConfig(id) {
        return BaseHarvesterConfig.testConfig(
            constants.endpoints.testSFtpHarvesterById, id);
    }
    static fromJson(json) {
        const config = super.fromJson(json);
        config.host = json.host;
        config.port = json.port;
        config.username = json.username;
        config.password = json.password;
        config.dir = json.dir;
        config.filesPattern = json.filesPattern;
        config.lastHarvested = json.lastHarvested;
        return config;
    }
}

export default SFtpHarvesterConfig;
