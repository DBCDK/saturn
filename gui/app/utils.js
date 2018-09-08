/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

function getArgValue(arg) {
    if(arg === undefined || arg === null) {
        return 0;
    }
    return arg;
}

const getStringValue = value => {
    return value !== null ? value : "";
};

export {getArgValue, getStringValue};
