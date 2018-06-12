FROM docker.dbc.dk/payara-micro

USER gfish

LABEL DB_URL="database url"

COPY api/target/saturn.war wars
COPY api/target/config /payara-micro/config.d

EXPOSE 8080
