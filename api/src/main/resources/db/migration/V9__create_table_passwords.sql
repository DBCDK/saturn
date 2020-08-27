CREATE TABLE passwords (
    id INT GENERATED ALWAYS AS IDENTITY,
    host text NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    activefrom timestamp with time zone
);
ALTER TABLE ONLY passwords
    ADD CONSTRAINT passwords_host_username_password_activefrom_key UNIQUE (host, username, password, activefrom);