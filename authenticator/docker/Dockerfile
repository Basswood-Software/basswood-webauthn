FROM eclipse-temurin:21
# Add Maintainer Info
LABEL maintainer="shamual.rahaman@gmail.com"

ARG TAR_BALL
ARG STARTUP_SCRIPT
ARG LOGGING_CONFIG_ARG
ENV LAUNCH_SCRIPT $STARTUP_SCRIPT
ENV LOGGING_CONFIG $LOGGING_CONFIG_ARG
ENV SPRING_APPLICATION_NAME authenticator

RUN apt-get update &&  apt-get install -y netcat

# Add a volume pointing to /opt
VOLUME /opt

WORKDIR /opt/basswood

COPY ../../target/${TAR_BALL} /opt/basswood

RUN tar -xvf *.tar.gz

ENTRYPOINT $LAUNCH_SCRIPT