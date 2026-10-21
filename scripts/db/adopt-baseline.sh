#!/bin/sh
set -eu
: "${DB_URL:?Set DB_URL to the reviewed database}"
: "${DB_USERNAME:?Set DB_USERNAME}"
: "${DB_PASSWORD:?Set DB_PASSWORD securely}"
: "${FITNUTRI_DATABASE_IDENTITY:?Set the reviewed database name}"
[ "${FITNUTRI_BASELINE_APPROVED:-}" = 1 ] || { echo 'BLOCKED: reviewed baseline approval required' >&2; exit 2; }
[ "${FITNUTRI_BACKUP_VERIFIED:-}" = 1 ] || { echo 'BLOCKED: verified backup required' >&2; exit 2; }
task_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cd "$task_root/backend"
exec ./mvnw spring-boot:run -Dspring-boot.run.main-class=com.fitnutri.backend.common.LegacyBaselineAdoption
