# Keycloak Image

This  project aims to build a Keycloak image corresponding to the Veolia use case.


### How it works

The Keycloak image is a multi-stage built Dockerfile offered by the Quarkus version of Keycloak. This multi-stage allows faster startup times and smaller image size.

The [Keycloak documentation](https://www.keycloak.org/server/all-config) describes which parameters can be configured at build time and which can be configured at runtime (deployment).

### 1st stage 

It is meant to configure these environment variables : 

| Name                       | Value                     | Description                          |
|----------------------------|---------------------------|--------------------------------------|
| KC_HEALTH_ENABLED          | true                      | Enable the heatlh endpoint           |
| KC_METRICS_ENABLED         | true                      | Enable the metrics endpoint          |
| KC_FEATURES                | token-exchange            | Enable the preview features          |
| KC_DB                      | postgres                  | Type of the database                 |
| KC_CACHE                   | ispn                      | Enable the Infinispan cache          |
| KC_CACHE_CONFIG_FILE       | custom-caches-jgroups.xml | Name of the Infinispan configuration |

It also add the theme and the custom extensions.

### 2nd stage

It creates the image with only the keycloak built at the previous step. 

## How to upgrade extensions

One the extension has been built, you have to follow this procedure :

- Update the extension's version in [Jenkinsfile](Jenkinsfile)
- Update the extension's version in [Dockerfile](Dockerfile)

## Develop a theme locally
 docker run -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -v $(pwd)/themes:/opt/keycloak/themes/ -p 8443:8443 -p 8080:8080 -it quay.io/keycloak/keycloak:19.0.1 start-dev --spi-theme-static-max-age=-1 --spi-theme-cache-themes=false --spi-theme-cache-templates=false
