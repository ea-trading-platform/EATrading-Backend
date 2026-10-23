#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

container="${POSTGRES_TEST_CONTAINER:-postgres-ea-trading}"
export ORDER_TEST_POSTGRES_USER
export ORDER_TEST_POSTGRES_PASSWORD
ORDER_TEST_POSTGRES_USER="$(docker exec "$container" printenv POSTGRES_USER)"
ORDER_TEST_POSTGRES_PASSWORD="$(docker exec "$container" printenv POSTGRES_PASSWORD)"
database="$(docker exec "$container" printenv POSTGRES_DB)"
export ORDER_TEST_POSTGRES_URL="${ORDER_TEST_POSTGRES_URL:-jdbc:postgresql://localhost:8088/$database}"

./mvnw -Dtest=OrderStatusLogPostgresTest test
