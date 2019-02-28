/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import {BaseHarvesterConfig} from "./BaseHarvesterConfig";
import constants from "../constants";

class FtpHarvesterConfig extends BaseHarvesterConfig {
    static addFtpHarvesterConfig(config) {
        return BaseHarvesterConfig.addHarvesterConfig(
            constants.endpoints.addFtpHarvesterConfig, config);
    }
    static fetchConfig(id) {
        return BaseHarvesterConfig.fetchConfig(
            constants.endpoints.getFtpHarvesterById, id);
    }
    static listFtpHarvesterConfigs(start, limit) {
        return BaseHarvesterConfig.listHarvesterConfigs(
            constants.endpoints.listFtpHarvesterConfigs, start, limit);
    }
    static testConfig(id) {
        return BaseHarvesterConfig.testConfig(
            constants.endpoints.testFtpHarvesterById, id);
    }
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
