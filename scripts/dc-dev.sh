#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_DIR="${ROOT_DIR}/infra/docker"

docker compose \
  -f "${COMPOSE_DIR}/compose.yml" \
  -f "${COMPOSE_DIR}/compose.dev.yml" \
  --env-file "${COMPOSE_DIR}/.env.dev" \
  "$@"