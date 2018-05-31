FROM docker.dbc.dk/payara-micro

USER gfish

LABEL DB_URL="database url"

COPY target/saturn.war wars
COPY target/config /payara-micro/config.d

EXPOSE 8080
