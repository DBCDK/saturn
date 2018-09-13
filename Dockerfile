FROM docker.dbc.dk/dbc-payara-micro-logback:4.1.2.181.temporary

USER gfish

LABEL DB_URL="database url"
LABEL FTP_HOST="host of ftp server to send files to"
LABEL FTP_PORT="port of ftp server to send files to"
LABEL FTP_USERNAME="username for ftp server to send files to"
LABEL FTP_PASSWORD="password for ftp server to send files to"
LABEL FTP_DIR="directory on ftp server to send files to"
LABEL PROXY_HOSTNAME="hostname of proxy to pass through"
LABEL PROXY_PORT="port number of proxy"
LABEL PROXY_USERNAME="username for proxy authentication"
LABEL PROXY_PASSWORD="password for proxy authentication"

COPY api/target/saturn.war wars
COPY api/target/config /payara-micro/config.d

EXPOSE 8080
