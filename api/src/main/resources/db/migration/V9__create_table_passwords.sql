CREATE SEQUENCE passwords_id_seq START WITH 1 INCREMENT BY 1 MINVALUE 1 CACHE 1;

CREATE TABLE passwords (
    id integer PRIMARY KEY NOT NULL,
    host text NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    activefrom timestamp with time zone
);
ALTER TABLE ONLY passwords
    ADD CONSTRAINT passwords_host_username_password_activefrom_key UNIQUE (host, username, password, activefrom);