FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:latest

USER gfish

LABEL DB_URL="database url"
LABEL PROXY_HOSTNAME="hostname of proxy to pass through"
LABEL PROXY_PORT="port number of proxy"
LABEL PROXY_USERNAME="username for proxy authentication"
LABEL PROXY_PASSWORD="password for proxy authentication"
LABEL JOBSTORE_URL="url for the DataIO Jobstore service"
LABEL FILESTORE_URL="url for the DataIO Fileservice service"

COPY target/saturn.war target/docker/saturn.json deployments/

EXPOSE 8080
