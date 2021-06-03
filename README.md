saturn
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
* PROXY_HOSTNAME hostname (or IP) of SOCKS proxy used for harvesting files
* PROXY_PORT SOCKS proxy port
* PROXY_USERNAME username used for SOCKS proxy login
* PROXY_PASSWORD password used for SOCKS proxy login
* NON_PROXY_HOSTS comma seperated list hostnames or domains to be excluded from SOCKS proxying (OPTIONAL)
* TZ timezone used by the Java environment (OPTIONAL)
* JAVA_MAX_HEAP_SIZE maximum size of the Java heap

### Development

To build this project JDK 8 and Apache Maven is required.

```bash
mvn clean
mvn verify
```

To start a local instance, docker is required.

```bash
docker build . -t saturn:devel
docker run --name saturn -e DB_URL=... -e FTP_DIR=... -e FTP_HOST=... -e FTP_PORT=... -e FTP_USERNAME=... -e FTP_PASSWORD=... -e PROXY_HOSTNAME=... -e PROXY_PORT=... -e PROXY_USERNAME=... -e PROXY_PASSWORD=... -e NON_PROXY_HOSTS=dbc.dk,litteratursiden.dk -e TZ=Europe/Copenhagen -e JAVA_MAX_HEAP_SIZE=2G -p 8080:8080 saturn:devel
```

The administrative user interface can now be accessed on http://localhost:8080

### License

Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3.
See license text in LICENSE.txt
