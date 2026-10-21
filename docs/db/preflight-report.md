# D00 migration preflight evidence

Branch develop/yuankang-wei. BC-14; no schema migration applied. Scripts db/preflight.sh + preflight.sql require explicit PGDATABASE/PGUSER/PGHOST and begin READ ONLY, inventory schema/constraints/indexes/counts/duplicates/nonfinite values/reference FKs, then ROLLBACK. No business values/passwords/health answers emitted. check_preflight.py fails closed on missing sections and returns3 for reviewed conflicts. This is a legacy-schema preflight; it is not a general post-migration health checker.

Executed shell script inside dedicated PG16 container with explicit synthetic identity: exit0; complete6-section report PASS, all checked conflicts0; existing unknown application database untouched. Metadata artifact schema-before.json contains structure and aggregate counts only.

`python3 scripts/tests/test_db_preflight.py`: 5 PASS/0 FAIL/0 ERROR/0 SKIP,1.753s. Each case clones schema into a new UUID database, creates synthetic fixtures, removes only that new UUID database. Verified clean/read-only/column names; case-insensitive email and dated-weight collision detection without exposing values; NaN and unknown food-reference FK blocks; incomplete reports and missing connection identity fail closed. Initial3 JSON parsing errors resolved by using compact JSONB output; no business assertion removed. Original test database remains available.

No real-data collision resolution authorised or attempted. Legacy timezone and shared backup remain separate gates. Independent review/UAT PENDING.
