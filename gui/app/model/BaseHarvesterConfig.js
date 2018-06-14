/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

const mapResponseToConfigList = (type, response) => {
    const list = JSON.parse(response);
    return list.map(item => type.fromJson(item));
};

class BaseHarvesterConfig {
    static fromJson(json) {
        /*
         * to enable inheriting this method properly, use `new this()`
         * otherwise, if the object is instantiated as new BaseHarvesterConfig()
         * the object returned by an inheriting class will be an instance
         * of the parent class rather than an instance of the inherting class
         */
        const config = new this();
        config.id = json.id;
        config.schedule = json.schedule;
        config.transfile = json.transfile;
        return config;
    }
}

export {BaseHarvesterConfig, mapResponseToConfigList};
