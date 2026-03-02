FROM quay.io/keycloak/keycloak:26.4.7 AS builder

#### Env build options #####
#DB
ENV KC_DB=postgres
#Health
ENV KC_HEALTH_ENABLED=true
#Features
ENV KC_FEATURES=token-exchange,recovery-codes,workflows,log-mdc,preview
#Log MDC: additionnal context info in logs (realm, client)
ENV KC_LOG_MDC_ENABLED=true
#Metrics
ENV KC_METRICS_ENABLED=true
ENV KC_EVENT_METRICS_USER_ENABLED=true

# COPY conf
COPY keycloak-image/conf /opt/keycloak/conf
# Install custom providers
COPY keycloak-image/providers /opt/keycloak/providers

HEALTHCHECK NONE

WORKDIR /opt/keycloak
# Install keystore
RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore

RUN /opt/keycloak/bin/kc.sh build --verbose

FROM quay.io/keycloak/keycloak:26.4.7

#### Env config #####
#HTTP
ENV KC_PROXY_HEADERS=xforwarded
ENV KC_HTTP_ENABLED=true
ENV KC_HTTP_MANAGEMENT_PORT=9000
#Truststore
##todo: switch volume to /opt/keycoak/conf/truststores and remove this env var
ENV KC_TRUSTSTORE_PATHS=/data
ENV KC_TLS_HOSTNAME_VERIFIER=DEFAULT
#Quarkus
ENV QUARKUS_HTTP_ACCESS_LOG_ENABLED=true
ENV QUARKUS_HTTP_RECORD_REQUEST_START_TIME=true
ENV QUARKUS_PATTERN_JSON='{ "client":"%{REMOTE_HOST}", "method":"%{METHOD}", "request_url":"%{REQUEST_URL}", "status":"%{RESPONSE_CODE}", "duration":"%{RESPONSE_TIME}", "bytes":"%{BYTES_SENT}" }'
ENV QUARKUS_HTTP_ACCESS_LOG_PATTERN='${quarkus.pattern.json}'
ENV QUARKUS_HTTP_ACCESS_LOG_EXCLUDE_PATTERN='(/resources/|/health|.*/.well-known).*'
ENV QUARKUS_HTTP_LIMITS_MAX_INITIAL_LINE_LENGTH=8192
ENV QUARKUS_HTTP_LIMITS_MAX_FORM_ATTRIBUTE_SIZE=1M
#CACHE
ENV KC_CACHE=ispn
ENV KC_CACHE_STACK=jdbc-ping-vef
ENV KC_CACHE_CONFIG_FILE=custom-caches-jgroups.xml

#Lastlogin SPI
KC_SPI_EVENTS_LISTENER_LAST_LOGIN_ATTRIBUTE=last_login
KC_SPI_EVENTS_LISTENER_LAST_LOGIN_ZONE_ID=Europe/Paris
KC_SPI_EVENTS_LISTENER_LAST_LOGIN_PATTERN=dd.MM.yyyy HH:mm:ss

#COPY keycloak folder from image builder
COPY --from=builder /opt/keycloak/ /opt/keycloak/

#Network conf
USER root
RUN chown -Rf keycloak:keycloak /opt/keycloak
RUN echo "net.core.rmem_max=25600000" > /etc/sysctl.conf
RUN echo "net.core.wmem_max=1024000" >> /etc/sysctl.conf

USER keycloak
