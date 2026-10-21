#!/bin/sh
# Local equivalent of mandatory CI gates. Never installs packages or calls paid AI.
set -eu
check_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$check_root"
check_stage=all
if [ "$#" -ne 0 ]; then
  [ "$#" -eq 2 ] && [ "$1" = --stage ] || { echo 'Usage: scripts/check.sh [--stage static|frontend|data|backend|browser]' >&2; exit 2; }
  check_stage=$2
fi
case "$check_stage" in all|static|frontend|data|backend|browser) ;; *) echo 'Unknown check stage' >&2; exit 2;; esac
check_python=${FITNUTRI_PYTHON:-python3}
static_checks() {
  git diff --check
  "$check_python" scripts/validate_contract.py
  "$check_python" scripts/generate_api_types.py
  npm --prefix frontend run typecheck:test
  npm --prefix frontend run lint
}
frontend_checks() {
  npm --prefix frontend run test:legacy
  npm --prefix frontend run typecheck:legacy
  npm --prefix frontend test -- --maxWorkers=2
  npm --prefix frontend run test:boundaries
  npm --prefix frontend run test:fault-proxy
  node --test scripts/tests/check-pipeline.test.mjs scripts/tests/deployment-server.test.mjs scripts/tests/backend-artifact.test.mjs
  npm --prefix frontend run build
}
data_checks() {
  "$check_python" -m unittest discover -s scripts/tests -p 'test_*.py'
}
backend_checks() {
  : "${TEST_DB_ADMIN_URL:?Set explicit isolated TEST_DB_ADMIN_URL}"
  : "${TEST_DB_USERNAME:?Set explicit isolated TEST_DB_USERNAME}"
  : "${TEST_DB_PASSWORD:?Set explicit isolated TEST_DB_PASSWORD}"
  : "${TEST_DB_DOCKER_CONTAINER:?Set dedicated TEST_DB_DOCKER_CONTAINER for restore tests}"
  # TestDatabase independently rejects unsafe identities; no default or H2 fallback.
  ./backend/mvnw -f backend/pom.xml verify
}
browser_checks() {
  # Opt in only after starting the real isolated stack and installing approved browser binaries.
  : "${FITNUTRI_E2E_DB_NAME:?Set owned E2E database}"
  : "${TEST_DB_DOCKER_CONTAINER:?Set dedicated test container}"
  : "${TEST_DB_USERNAME:?Set explicit isolated database role}"
  : "${FITNUTRI_E2E_ADMIN_EMAIL:?Set synthetic E2E administrator email privately}"
  : "${FITNUTRI_E2E_ADMIN_PASSWORD:?Set synthetic E2E administrator password privately}"
  npm --prefix frontend run test:e2e
}
case "$check_stage" in
  all) static_checks; frontend_checks; data_checks; backend_checks;;
  static) static_checks;; frontend) frontend_checks;; data) data_checks;; backend) backend_checks;; browser) browser_checks;;
esac
printf 'Automated gates passed for stage: %s. Browser/live-provider/peer/UAT acceptance are separate.\n' "$check_stage"
