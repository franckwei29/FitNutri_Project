#!/bin/sh
set -eu
umask 077
task_script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$task_script_dir/connection-gate.sh"
: "${TARGET_DATABASE:?Set a NEW database name}"
: "${BACKUP_FILE:?Set the protected backup file path}"
[ "${FITNUTRI_RESTORE_APPROVED:-}" = 1 ] || { echo 'BLOCKED: explicit restore approval required' >&2; exit 2; }
case "$TARGET_DATABASE" in ''|*[!a-zA-Z0-9_]*|[0-9]*) echo 'BLOCKED: invalid new database name' >&2; exit 2;; esac
[ "$TARGET_DATABASE" != "$PGDATABASE" ] || { echo 'BLOCKED: restore must target a new database' >&2; exit 2; }
[ -f "$BACKUP_FILE" ] && [ ! -L "$BACKUP_FILE" ] || { echo 'BLOCKED: readable regular backup file required' >&2; exit 2; }
pg_restore --list "$BACKUP_FILE" >/dev/null
# createdb fails atomically if the destination exists. No --clean, DROP or overwrite fallback.
createdb --no-password --maintenance-db="$PGDATABASE" "$TARGET_DATABASE"
pg_restore --no-password --dbname="$TARGET_DATABASE" --single-transaction --exit-on-error --no-owner --no-privileges "$BACKUP_FILE"
printf '%s\n' 'Restored into a new database. Compare verification reports before any application switch.'
