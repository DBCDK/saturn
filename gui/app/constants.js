/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

const endpoints = {
    addHttpHarvesterConfig: "/api/configs/http/add",
    addFtpHarvesterConfig: "/api/configs/ftp/add",
    getHttpHarvesterById: "/api/configs/http/get/:id",
    getFtpHarvesterById: "/api/configs/ftp/get/:id",
    listHttpHarvesterConfigs: "/api/configs/http/list",
};
Object.freeze(endpoints);

const constants = {
    endpoints
};
Object.freeze(constants);

export default constants;
