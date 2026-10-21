#!/bin/sh
# Sourced by maintenance commands. Never use application datasource defaults.
: "${PGDATABASE:?Set PGDATABASE explicitly}"
: "${PGUSER:?Set PGUSER explicitly}"
: "${PGHOST:?Set PGHOST explicitly}"
: "${PGPORT:?Set PGPORT explicitly}"
: "${FITNUTRI_DATABASE_IDENTITY:?Set the expected database name}"
task_actual_database=$(psql -X --no-password --set ON_ERROR_STOP=1 --tuples-only --no-align --command 'SELECT current_database()')
[ "$task_actual_database" = "$FITNUTRI_DATABASE_IDENTITY" ] || { echo 'BLOCKED: database identity mismatch' >&2; exit 2; }
