Saturn
======

Service and administrative graphical user interface for setting up and running periodic harvests of files from
FTP, HTTP and SFTP based sources.

Harvested files will be uploaded alongside generated .trans files to a preconfigured FTP server.

*"saturn is a roman god of periodic renewal."*

### Configuration

**Environment variables**

* DB_URL database URL (USER:PASSWORD@HOST:PORT/DBNAME) of the underlying saturn database.
* FTP_DIR destination dir for harvested files and .trans files
* FTP_HOST hostname (or IP) of FTP server used for uploading of harvested files and .trans files
* FTP_PORT FTP server port
* FTP_USERNAME username used for FTP server login
* FTP_PASSWORD password used for FTP server login
* PROXY_HOSTNAME hostname (or IP) of SOCKS proxy to allow harvesting through an inner proxy
* PROXY_PORT SOCKS proxy port
* PROXY_USERNAME username used for SOCKS proxy login
* PROXY_PASSWORD password used for SOCKS proxy login
* NON_PROXY_HOSTS comma seperated list of hostnames or domains to be excluded from SOCKS proxying (OPTIONAL)
* TZ timezone used by the Java environment (OPTIONAL)
* JAVA_MAX_HEAP_SIZE maximum size of the Java heap

### Development

To build this project JDK 11, Docker and Apache Maven are required:

```bash
mvn clean
mvn verify
```

To start a local instance:

```bash
docker run -it --name saturn --rm --env-file $(pwd)/env.devel -p 8080:8080 docker-io.dbc.dk/saturn-service:devel
```
(Assuming that you have placed your ```env.devel``` in your working dir.)

Example ``env.devel`` file:
```bash
DB_URL=<saturn_db_user>:<password>@<my_local_ip>:5432/<saturn_db>
FTP_DIR=<ftp_destination_path>
FTP_HOST=<my_local_ftp_test_server>
FTP_PORT=21
FTP_USERNAME=<ftpuser>
FTP_PASSWORD=<ftppassword>
PROXY_HOSTNAME=<proxy_hostname>
PROXY_PORT=1080
PROXY_USERNAME=<proxy_user>
PROXY_PASSWORD=<proxy_password>
NON_PROXY_HOSTS=dbc.dk,<my_local_ip>
TZ=Europe/Copenhagen
JAVA_MAX_HEAP_SIZE=2G
LOG_FORMAT=text
```

The administrative user interface can now be accessed on http://localhost:8080

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt
