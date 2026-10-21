#!/bin/sh
set -eu
task_script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$task_script_dir/connection-gate.sh"
exec psql -X --no-password --set ON_ERROR_STOP=1 --tuples-only --no-align --file "$task_script_dir/verify.sql"
