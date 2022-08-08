/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

import constants from "../constants";
import {HttpClient} from "../HttpClient";
import {getArgValue} from "../utils";

const mapResponseToConfigList = (type, response) => {
    const list = JSON.parse(response);
    return list.map(item => type.fromJson(item));
};

class BaseHarvesterConfig {
    static addHarvesterConfig(endpoint, config) {
        return new HttpClient()
            .addHeaders({"Content-type": "application/json"})
            .post(endpoint, null, null, config);
    }
    static fetchConfig(endpoint, id) {
        const params = new Map();
        params.set("id", id);
        return new HttpClient()
            .get(endpoint, params);
    }
    static validateScheduleExpression(expression) {
        return new HttpClient().post(constants.endpoints.cronValidate, null,
            null, expression);
    }
    static listHarvesterConfigs(endpoint, start, limit) {
        start = getArgValue(start);
        limit = getArgValue(limit);
        const query = {start, limit};
        return new HttpClient()
            .get(endpoint, null, query);
    }
    static deleteConfig(endpoint, id) {
        const params = new Map();
        params.set("id", id);
        return new HttpClient().delete(endpoint, params);
    }
    static testConfig(endpoint, id) {
        const params = new Map();
        params.set("id", id);
        return new HttpClient().get(endpoint, params);
    }
    static fromJson(json) {
        /*
         * to enable inheriting this method properly, use `new this()`
         * otherwise, if the object is instantiated as new BaseHarvesterConfig()
         * the object returned by an inheriting class will be an instance
         * of the parent class rather than an instance of the inherting class
         */
        const config = new this();
        config.id = json.id;
        config.name = json.name;
        config.schedule = json.schedule;
        config.transfile = json.transfile;
        config.seqno = json.seqno;
        config.seqnoExtract = json.seqnoExtract;
        config.agency = json.agency;
        config.enabled = json.enabled;
        config.lastHarvested = json.lastHarvested;
        config.progress = json.progress ? json.progress.percentage : "";
        config.running = json.progress ? json.progress.running : false;
        return config;
    }
}

export {BaseHarvesterConfig, mapResponseToConfigList};
