# poc-keycloak

This repository contains the parent Maven project and modules used to build and run Keycloak locally.


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

## Run locally

### Prerequisites

Before running the project locally, make sure you have:

- OpenJDK 21 installed (required to build with Maven)
- Docker installed
- Docker Compose available (Docker Desktop includes it as `docker compose`)

You can run this project locally in two ways:

- with Docker Compose (full stack: Keycloak + Postgres + Mailcatcher)
- with a locally generated Keycloak package (script based)

## Build Keycloak first

Before running Keycloak (Compose, script, or Maven exec), build the parent project from the repository root:

```bash
mvn -P local-distrib -DskipTests clean package
```

This prepares:

- provider jars under `keycloak-distrib/providers`
- local runtime package assets under `target/keycloak-local/...`

### Option 1: Docker Compose

Start the stack:

```bash
docker-compose up --build -d
```

Follow Keycloak logs:

```bash
docker compose logs -f keycloak-x-1
```

Stop and remove the stack:

```bash
docker compose down
```

Access:

- Keycloak: http://localhost:8080
- Mailcatcher UI: http://localhost:1080

Notes:

- The `partenaires` realm import file is mounted via [docker-compose.yml](docker-compose.yml).
- Keycloak is started with `--import-realm` in [docker-compose.yml](docker-compose.yml).

### Option 2: Local package + script (or Maven exec)

Run these commands from the repository root (`poc-keycloak`) because the `local-distrib` Maven profile is defined in the parent `pom.xml`.

Build the local distribution package (includes SPI jars and realm import assets):

```bash
mvn -P local-distrib clean package
```

Run Keycloak through Maven (parent project only):

```bash
mvn -N -P local-distrib exec:exec@run-local-keycloak
```

Important:

- The `run-local-keycloak` execution is defined in the parent [pom.xml](pom.xml) under `local-distrib`.
- Use `-N` (`--non-recursive`) so Maven does not try to execute `exec:exec@run-local-keycloak` in child modules.
- Running `mvn -P local-distrib ... exec:exec@run-local-keycloak` without `-N` executes the goal reactor-wide and fails in modules that do not define this execution.

Equivalent one-liner:

```bash
mvn -P local-distrib -DskipTests package && mvn -N -P local-distrib exec:exec@run-local-keycloak
```

Run Keycloak locally from the generated package:

```bash
./scripts/run-local-keycloak.sh
```

Optional examples:

```bash
# Use a specific generated zip
./scripts/run-local-keycloak.sh ./target/keycloak-local-26.7.3-partenaires-spi.zip

# Pass extra Keycloak args
./scripts/run-local-keycloak.sh --http-port=8180
```

Access:

- Keycloak: http://localhost:8080 (or the custom port if overridden)

Defaults used by the script:

- admin username: `admin`
- admin password: `admin`

You can override them:

```bash
KC_BOOTSTRAP_ADMIN_USERNAME=admin KC_BOOTSTRAP_ADMIN_PASSWORD=admin ./scripts/run-local-keycloak.sh
```

## How to upgrade extensions

One the extension has been built, you have to follow this procedure :

- Update the extension's version in [Jenkinsfile](Jenkinsfile)
- Update the extension's version in [Dockerfile](Dockerfile)

## Develop a theme locally

```bash
docker run -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -v $(pwd)/themes:/opt/keycloak/themes/ -p 8443:8443 -p 8080:8080 -it quay.io/keycloak/keycloak:19.0.1 start-dev --spi-theme-static-max-age=-1 --spi-theme-cache-themes=false --spi-theme-cache-templates=false
```
