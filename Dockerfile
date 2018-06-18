FROM docker.dbc.dk/payara-micro

USER gfish

LABEL DB_URL="database url"
LABEL FTP_HOST="host of ftp server to send files to"
LABEL FTP_PORT="port of ftp server to send files to"
LABEL FTP_USERNAME="username for ftp server to send files to"
LABEL FTP_PASSWORD="password for ftp server to send files to"
LABEL FTP_DIR="directory on ftp server to send files to"

COPY api/target/saturn.war wars
COPY api/target/config /payara-micro/config.d

EXPOSE 8080
