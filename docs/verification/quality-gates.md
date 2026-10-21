# R00 executable local quality gates

Executed on2026-10-17, `develop/yuankang-wei`, Node24/Java21/PostgreSQL16.15. No school GitHub or remote runner was accessed; availability is UNVERIFIED under the explicit no-remote instruction. No speculative CI workflow/run is claimed.

`FITNUTRI_PYTHON=/private/tmp/fitnutri-validation-tools/bin/python MAVEN_ARGS=-o TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=<synthetic-local-value> TEST_DB_DOCKER_CONTAINER=fitnutri-integration-20261016 ./scripts/check.sh` exited0.

- Static:44operations/59schemas/564positive+11negative contract examples; generated59types/Zod no drift; TypeScript/lint0warnings PASS.
- Frontend:63files/343tests PASS; original Node15+byte-integrity1 PASS; legacy strict types PASS; production boundary11 and transport fault-proxy8 PASS; pipeline4 PASS. Actual production build/enforced gate117modules/507edges/30manifest entries/32text artifacts/0violations.
- Python normalization/preflight tests: 15PASS/0FAIL in1.992s.
- Backend:460Surefire+4Failsafe PASS,0fail/error/skip; Maven55.119s. Includes actual source-pinned catalogue and UUID-owned PostgreSQL migration/transaction/concurrency/backup-restore tests.
- Pipeline self-test RED4 before entrypoint existed; GREEN4. Deliberately failing static executable exits17 and prevents downstream commands. Unknown stage and missing DB configuration fail. No skip-tests/continue-on-error or live paid-provider calls.

Local full output retained in `/tmp/fitnutri-r00-full-check.log`. These automated gates do not replace Q05/Q06/Q08 actual-browser acceptance, A10 live-provider evidence, peer review or UAT. All remain separately tracked.
