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

const getHttpHeadersAsText = list => {
    try {
        if (list === undefined || list === null) {
            return "";
        }
        if (typeof list === "string") {
            return list;
        }
        const newList = list.map((header) => {
            return `{"${header.key}": "${header.value}"}`
        }).join();

        return list !== null ? "["+newList+"]" : "";
    } catch (e) {
        return "";
    }
}

const getHttpHeadersTable = text => {
    try {
        const newList = JSON.parse(text).map((header) => {
            return {"key": Object.keys(header)[0] , "value": Object.values(header)[0]};
        });

        return text !== null ? newList : [];
    } catch (e) {
        console.log(e);
        return [];
    }
}

function formatDate(date) {
    var hours = date.getHours();
    var minutes = date.getMinutes();
    var days = date.getDate();
    var months = date.getMonth()+1;
    var secs = date.getSeconds();
    secs = secs < 10 ? '0'+secs : secs;
    days = days < 10 ? '0'+days : days;
    hours = hours < 10 ? '0'+hours : hours;
    months = months < 10 ? '0'+months : months;
    minutes = minutes < 10 ? '0'+minutes : minutes;
    var strTime = hours + ':' + minutes + ':' + secs;

    return date.getFullYear() + "-" + months + "-" + days + ' ' + strTime;
}

export {getArgValue, getStringValue, getHttpHeadersAsText, getHttpHeadersTable, formatDate};
