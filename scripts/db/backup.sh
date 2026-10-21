#!/bin/sh
set -eu
umask 077
task_script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$task_script_dir/connection-gate.sh"
: "${BACKUP_FILE:?Set a new protected backup file path outside Git}"
[ ! -e "$BACKUP_FILE" ] && [ ! -L "$BACKUP_FILE" ] || { echo 'BLOCKED: backup destination already exists' >&2; exit 2; }
pg_dump --no-password --format=custom --no-owner --no-privileges --file "$BACKUP_FILE"
pg_restore --list "$BACKUP_FILE" >/dev/null
printf '%s\n' 'Backup created and archive inspected. Restore rehearsal is still required.'
