# R02 backup, restore and application compatibility evidence

Executed2026-10-17 on `develop/yuankang-wei`; PostgreSQL16.15, Java21, current executable JAR. Only new `fitnutri_it_<32hex>` source/restore databases owned by the test were used. Existing review runtime, provided V1 baseline and other containers/databases were not migrated, restored over, deleted or cleaned.

## Actual exercise

`BackupRestoreRehearsalIT.dumpRestoreAndApprovedLegacyUpgradeRetainRowsChecksumsAndConstraints` creates a new V4 database with one synthetic BCrypt account, legacy PHOTO food/raw fractional inputs and legacy activity. It verifies a proposed restore name is absent, runs the actual `backup.sh` inside the dedicated container, inspects the custom archive and0600 permissions. Reusing a backup path fails. Restore without approval fails; approved restore creates only the new destination; restoring over that destination fails. No --clean/overwrite fallback exists.

The actual current executable JAR is first launched against the V4 restore with Flyway disabled. Initial RED showed that Hibernate column validation alone allowed startup: V4 already had expanded nullable columns but backfill/constraints were not finished. BC-14 now adds a read-only `SchemaReadinessGate`, after optional approved database initialization and before application readiness, requiring successful reviewed V1–V8 history (V1 may be adopted BASELINE). Missing/older/unknown future/failed/duplicate migration histories fail safely; no automatic adoption/migration or raw SQLException cause. Unit4 covers normal/older/unknown/failed/missing history, no updates/commits and resource closure. The actual V4 binary startup now exits nonzero with this gate; history staysV4.

The explicitly approved Sydney legacy conversion runs on the two test-owned databases through V8. Read-only verification reports match exactly excluding database identity: row counts, retained raw-column checksums, known nutrient/energy totals, dates, null provenance and validated constraints/indexes. Known food energy418.917kJ, activity516.724kJ, date2020-01-02. Source rawPHOTO and original float/timestamps remain; DTO maps IMAGE and LEGACY_UNVERIFIED without invented reference/weight/MET/formula.

Then a separate **real executable application process** starts successfully on the restored V8 database. Real HTTP CSRF/login with the restored BCrypt password succeeds; owned food/activity GETs return the expected migrated date/energy/legacy markers and missing facts remainNULL. The process is stopped in finally. Post-login/read verification equals both pre-smoke restored report and the source report: the clone application changes no business rows in either database. Test-generated source/destination databases are removed only by their ownership-checked harness; protected synthetic dump remains in its private generated container directory.

Focused command:

```sh
TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline \
TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=<private-local-test-value> \
TEST_DB_DOCKER_CONTAINER=fitnutri-integration-20261016 \
./backend/mvnw -o -f backend/pom.xml \
-Dtest=SchemaReadinessGateTest,LegacyBackfillMigrationTest \
-Dit.test=BackupRestoreRehearsalIT verify
```

Final:12Surefire(4gate+8legacy)+1actual restoration/application IT PASS/0fail/error/skip. The first expanded restore probe failed because the missing gate was exposed; assertions were retained and the startup boundary implemented. Unit RED compilation before the new gate class was expected. Full global regression follows in Q07/delivery evidence.

## Operator recovery procedure

Set explicit `PGHOST`, `PGPORT`, `PGUSER`, `PGDATABASE`, private `PGPASSWORD`/pgpass and exact `FITNUTRI_DATABASE_IDENTITY`. Take a protected consistent `pg_dump` snapshot with a **new** `BACKUP_FILE` outside Git. For live systems either stop writes for a stated cutover window or document the dump's transaction snapshot and later-write reconciliation; this exercise had no concurrent writers. Never interpret a live source and later clone comparison as a consistent shared cutover without that policy.

Run `scripts/db/restore-to-new-db.sh` with a genuinely absent `TARGET_DATABASE`, the protected backup and explicit `FITNUTRI_RESTORE_APPROVED=1`. `createdb` atomically refuses an existing name; `pg_restore --single-transaction --exit-on-error` fails on errors. Run read-only `verify.sh` on source/snapshot and restore; review any differences before switching applications. Rehearse the matching application version's login/reads against the clone. Scripts require host PG16 client tools, or copy/run the reviewed scripts inside the explicitly selected dedicated PG16 container as this test does.

Current application requires V8. A V4 backup alone cannot be served by the current binary before reviewed forward migration. V8→V4 downgrade SQL is neither provided nor safe; keep forward columns/data and use a newly restored compatible database/application pair for recovery. The retained old raw columns and their values are verified, but **running an older application binary against V8 is not certified**. No automatic rollback/public/shared cutover was performed; deployment selection requires the team owner.

Database migration: no new SQL/version; BC-14 adds only the read-only application startup guard. Peer review, shared database rollout and UAT remain PENDING.

## Final actual review-database snapshot and restored-login follow-up

After the real Chrome/IAB journeys completed and with no concurrent business writers, the existing owned V8 synthetic review DB was dumped through the same gated scripts into a new0600 host file under private0700 `.local/db-backups/final-v8-8d4ce37e91504264901911dfcffbab25/`. Source-before, source-after and restored verification JSON matched excluding database identity. Physical totals (soft-deleted rows retained):6users/5profiles/6weights/10foods/3activities/8receipts;5329food references(1588legacy+3741verified official),29activity types(14legacy+15verified official),2datasets per reference kind. No source table/history was modified.

Archive actually restored into new `fitnutri_it_8d4ce37e91504264901911dfcffbab25`, retained for review. A separate current immutable-JAR process on loopback8083 passed V8 startup, real restored synthetic USER6 CSRF/login, owned food9(date2020-01-02/150g/988.5kJ/verified reference), exercise3(date2019-12-31/12.5min/MISSING_WEIGHT/NULL energy), foreign owner food7→404 and verified catalogues/unconfiguredAI. Source and clone reports still matched the pre-dump snapshot after these reads. Only the clone application process was stopped; main application/database remain running.

Protected state file `state.json` records `APPLICATION_RESTORE_VERIFIED`, archive path/SHA256 and source/clone scope. Dump274480bytes, host permissions0600; directory0700; both backup/runtime artifacts confirmed Git-ignored. This is actual restore/read evidence, not merely file existence. It is a local synthetic recovery point, not a public/shared cutover or older-binary-on-V8 certification.
