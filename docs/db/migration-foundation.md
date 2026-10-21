# D01 / Q00 — Controlled migration and isolated PG16 tests

2026-10-16 Australia/Sydney; branch develop/yuankang-wei. Boot4.1.1 effective BOM supplies Flyway12.4.0, Mockito5.23.0; no framework downgrade. Flyway PostgreSQL module installed explicitly. V1 derives from observed six-table PG16.15 schema. Corrected pg_dump enum-check cast re-parsing by expressing the same reviewed IN enums as original Hibernate DDL; strict metadata equality retained. No shared migration previously existed or was edited.

Normal legacy application configuration leaves Flyway disabled until D05 rollout; migration profile requires explicit DB_URL/DB_USERNAME/DB_PASSWORD and Hibernate validate. Automatic baseline=false and clean-disabled=true. No new account/record/reference schema upgrade applied yet. Default legacy Hibernate update is scheduled for replacement after target schema validation, not claimed removed already.

`LegacySchemaVerifier` checks exact columns/types/precision/nullability/defaults/identity/constraints/indexes against a reviewed JSON signature; data values excluded. `scripts/db/adopt-baseline.sh` standalone main avoids normal app/Hibernate writers and requires explicit identity, reviewed approval and verified-backup acknowledgements. It checks schema, account/date collisions and absence of previous Flyway history before explicit version1 baseline. Shared adoption additionally requires maintenance/write freeze, actual preflight and backup/restore evidence; no such shared operation occurred.

Real test CLI exercised against a new UUID synthetic legacy database: exit0, Flyway baseline1 created and app_user rows remained0. Source baseline/test volumes untouched; temporary UUID target removed. This is not a shared backup/restore rehearsal.

## Reproducible developer test command

```sh
TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline \
TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=test_only \
./backend/mvnw -o -f backend/pom.xml verify
```

TestDatabase refuses absent settings, non-local/non-test-name targets, query-bearing URLs and non-PG16 servers. Creates only UUID-named test databases; cleanup limited to the owned generated name. Context tests inherit a unique database via DynamicPropertySource, integration-test profile uses Flyway and Hibernate validate, never application-default DB. No H2 or disabled PG tests. Mockito explicit javaagent avoids sandbox attach limitations. Surefire fails on0 discovered tests; Failsafe executes *IT at verify.

Actual final verification: Surefire21 PASS/0 FAIL/0 ERROR/0 SKIP; Failsafe1 PASS/0 FAIL/0 ERROR/0 SKIP, Maven verify exit0 (7.031s). Context asserts PostgreSQL16 and V1 history. Four migration tests prove empty DB/idempotent second run, explicit adoption preserves synthetic email/hash, wrong schema no automatic baseline, and altered constraint/identity mismatch rejected. Failsafe smoke checks actual23505 uniqueness and transactional rollback to zero rows. Earlier19-test run found strict check-expression mismatch (1 error); fixed DDL to match real schema, not validation expectations. Multiple independent UUID runs reproduced results; no original failure hidden.

Peer review/UAT PENDING. Future V2–V7 and real-data migration remain TODO. Official reference provenance not established by old AFCD loader.
