#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET_DIR="$ROOT_DIR/target"
RUNTIME_DIR="$TARGET_DIR/keycloak-local-runtime"

if [[ ! -d "$TARGET_DIR" ]]; then
  echo "Missing target directory: $TARGET_DIR"
  echo "Run: mvn -P local-distrib package"
  exit 1
fi

DIST_ZIP=""
if [[ $# -gt 0 && -f "$1" ]]; then
  DIST_ZIP="$1"
  shift
else
  CANDIDATE_PARTENAIRES=$(find "$TARGET_DIR" -maxdepth 1 -type f -name 'keycloak-local-*-partenaires-spi.zip' | sort | tail -n 1 || true)
  CANDIDATE_DEFAULT=$(find "$TARGET_DIR" -maxdepth 1 -type f -name 'keycloak-local-*-spi.zip' | sort | tail -n 1 || true)
  CANDIDATE_GENERIC=$(find "$TARGET_DIR" -maxdepth 1 -type f -name 'keycloak-local-*.zip' | sort | tail -n 1 || true)

  if [[ -n "$CANDIDATE_PARTENAIRES" ]]; then
    DIST_ZIP="$CANDIDATE_PARTENAIRES"
  elif [[ -n "$CANDIDATE_DEFAULT" ]]; then
    DIST_ZIP="$CANDIDATE_DEFAULT"
  elif [[ -n "$CANDIDATE_GENERIC" ]]; then
    DIST_ZIP="$CANDIDATE_GENERIC"
  fi
fi

if [[ -z "$DIST_ZIP" ]]; then
  echo "No local Keycloak package zip found in $TARGET_DIR"
  echo "Run: mvn -P local-distrib package"
  exit 1
fi

echo "Using package: $DIST_ZIP"

rm -rf "$RUNTIME_DIR"
mkdir -p "$RUNTIME_DIR"
unzip -oq "$DIST_ZIP" -d "$RUNTIME_DIR"

# Support both zip layouts:
# 1) keycloak-local/keycloak-<version>/...
# 2) keycloak-<version>/...
KC_HOME_CANDIDATE=$(find "$RUNTIME_DIR" -maxdepth 4 -type f -path '*/bin/kc.sh' | sort | head -n 1 || true)
if [[ -n "$KC_HOME_CANDIDATE" ]]; then
  KC_ROOT="$(cd "$(dirname "$KC_HOME_CANDIDATE")/.." && pwd)"
else
  KC_ROOT=""
fi

if [[ -z "$KC_ROOT" ]]; then
  echo "Could not locate unpacked Keycloak directory under $RUNTIME_DIR"
  exit 1
fi

if [[ ! -f "$KC_ROOT/bin/kc.sh" ]]; then
  echo "Missing Keycloak launcher: $KC_ROOT/bin/kc.sh"
  exit 1
fi

# Zip extraction can drop executable bits on shell scripts.
if [[ ! -x "$KC_ROOT/bin/kc.sh" ]]; then
  chmod +x "$KC_ROOT/bin/kc.sh" || true
fi

# Keep only the newest jar per provider artifact key in providers/.
# This avoids build failures when multiple versions of the same SPI jar are present.
PROVIDERS_DIR="$KC_ROOT/providers"
if [[ -d "$PROVIDERS_DIR" ]] && compgen -G "$PROVIDERS_DIR/*.jar" > /dev/null; then
  SEEN_KEYS_FILE="$(mktemp)"
  # Newest files first (mtime desc), so the first encountered version is kept.
  while IFS= read -r jar_path; do
    jar_name="$(basename "$jar_path")"
    artifact_key="$(echo "$jar_name" | sed -E 's/-[0-9][0-9A-Za-z.+-]*\.jar$//')"

    if grep -Fxq "$artifact_key" "$SEEN_KEYS_FILE"; then
      echo "Removing older duplicate provider jar: $jar_name"
      rm -f "$jar_path"
    else
      echo "$artifact_key" >> "$SEEN_KEYS_FILE"
    fi
  done < <(ls -1t "$PROVIDERS_DIR"/*.jar)
  rm -f "$SEEN_KEYS_FILE"
fi

export KC_BOOTSTRAP_ADMIN_USERNAME="${KC_BOOTSTRAP_ADMIN_USERNAME:-admin}"
export KC_BOOTSTRAP_ADMIN_PASSWORD="${KC_BOOTSTRAP_ADMIN_PASSWORD:-admin}"

KC_ENV_FILE="$KC_ROOT/keycloak-common.env"
if [[ -f "$KC_ENV_FILE" ]]; then
  echo "Loading Keycloak env defaults from $KC_ENV_FILE"
  set -a
  # shellcheck disable=SC1090
  source "$KC_ENV_FILE"
  set +a
fi

cd "$KC_ROOT"
echo "Building Keycloak distribution in $KC_ROOT"
bash ./bin/kc.sh build --verbose

echo "Starting Keycloak with realm import"
bash ./bin/kc.sh start-dev --import-realm "$@"
