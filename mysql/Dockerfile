FROM mysql:8.2
# Add Maintainer Info
LABEL maintainer="shamual.rahaman@gmail.com"

ENV MYSQL_ROOT_PASSWORD basswood
ENV MYSQL_DATABASE webauthn_basswood
ENV MYSQL_USER basswood
ENV MYSQL_PASSWORD basswood

VOLUME /opt

COPY sql-01-setup-users.sql /docker-entrypoint-initdb.d
COPY sql-02-setup-schema.sql /docker-entrypoint-initdb.d
COPY health-check.sh /opt