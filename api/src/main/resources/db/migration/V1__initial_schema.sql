CREATE TABLE httpharvester (
    id              INTEGER PRIMARY KEY,
    url             TEXT NOT NULL,
    -- schedule is a string in cron syntax
    schedule        TEXT NOT NULL,
    lastharvested   TIMESTAMP WITH TIME ZONE
);

CREATE TABLE ftpharvester (
    id              INTEGER PRIMARY KEY,
    host            TEXT NOT NULL,
    port            INTEGER NOT NULL,
    username        TEXT NOT NULL,
    password        TEXT NOT NULL,
    dir             TEXT NOT NULL,
    filespattern    TEXT NOT NULL,
    -- schedule is a string in cron syntax
    schedule        TEXT NOT NULL,
    lastharvested   TIMESTAMP WITH TIME ZONE
);

CREATE SEQUENCE harvesterconfig_id_seq START WITH 1 INCREMENT BY 1 MINVALUE 1 CACHE 1;
