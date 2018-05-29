FROM docker.dbc.dk/payara-micro

USER gfish

COPY target/saturn.war wars

EXPOSE 8080
