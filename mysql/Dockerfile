FROM mysql:8.2
# Add Maintainer Info
LABEL maintainer="shamual.rahaman@gmail.com"

ENV MYSQL_ROOT_PASSWORD basswood
ENV MYSQL_DATABASE webauthn_basswood
ENV MYSQL_USER basswood
ENV MYSQL_PASSWORD basswood

COPY database.sql /docker-entrypoint-initdb.d