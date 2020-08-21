CREATE TABLE sftpharvester (
    id integer PRIMARY KEY NOT NULL,
    host text NOT NULL,
    port integer NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    dir text NOT NULL,
    filespattern text NOT NULL,
    schedule text NOT NULL,
    lastharvested timestamp with time zone,
    transfile text NOT NULL,
    name text NOT NULL,
    seqno integer,
    seqnoextract text,
    agency text DEFAULT '000000'::text NOT NULL,
    enabled boolean DEFAULT true NOT NULL
);

ALTER TABLE ONLY sftpharvester
    ADD CONSTRAINT sftpharvester_name_key UNIQUE (name);