#!/bin/sh
# Explicit PostgreSQL connection environment only; never falls back to app defaults.
set -eu
: "${PGDATABASE:?Set PGDATABASE explicitly to the database to inspect}"
: "${PGUSER:?Set PGUSER explicitly}"
: "${PGHOST:?Set PGHOST explicitly (or use a local socket directory)}"
task_script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec psql -X --no-password --set ON_ERROR_STOP=1 --tuples-only --no-align --file "$task_script_dir/preflight.sql"
