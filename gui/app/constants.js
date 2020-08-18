/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

const endpoints = {
    addHttpHarvesterConfig: "/api/configs/http/add",
    addFtpHarvesterConfig: "/api/configs/ftp/add",
    addSFtpHarvesterConfig: "/api/configs/sftp/add",
    cronValidate: "/api/fields/cron/validate",
    deleteHttpHarvesterConfig: "/api/configs/http/delete/:id",
    deleteFtpHarvesterConfig: "/api/configs/ftp/delete/:id",
    deleteSFtpHarvesterConfig: "/api/configs/sftp/delete/:id",
    getHttpHarvesterById: "/api/configs/http/get/:id",
    getFtpHarvesterById: "/api/configs/ftp/get/:id",
    getSFtpHarvesterById: "/api/configs/sftp/get/:id",
    testHttpHarvesterById: "/api/configs/http/test/:id",
    testFtpHarvesterById: "/api/configs/ftp/test/:id",
    testSFtpHarvesterById: "/api/configs/sftp/test/:id",
    listHttpHarvesterConfigs: "/api/configs/http/list",
    listFtpHarvesterConfigs: "/api/configs/ftp/list",
    listSFtpHarvesterConfigs: "/api/configs/sftp/list",
};
Object.freeze(endpoints);

const paths = {
    httpConfigList: "/http/",
    ftpConfigList: "/ftp/",
    sftpConfigList: "/sftp/",
    editHttpHarvesterConfig: "/configs/http/:id/edit/",
    editFtpHarvesterConfig: "/configs/ftp/:id/edit/",
    editSFtpHarvesterConfig: "/configs/sftp/:id/edit/",
    newHttpHarvesterConfig: "/http/new",
    newFtpHarvesterConfig: "/ftp/new",
    newSFtpHarvesterConfig: "/sftp/new",
};
Object.freeze(paths);

const constants = {
    endpoints,
    paths,
};
Object.freeze(constants);

export default constants;
