# UX-P01 actual regression baseline

2026-10-17; baseline ae264208; branch develop/yuankang-wei. No business source modified before these runs.

| Stage | Actual command / evidence | Result |
|---|---|---|
| Static | FITNUTRI_PYTHON=/private/tmp/fitnutri-ux-v3-venv/bin/python sh scripts/check.sh --stage static | PASS; /tmp/fitnutri-ux-v3-static-rerun.log |
| Frontend | sh scripts/check.sh --stage frontend (approved local socket access) | PASS; 63 Vitest files /343 tests; /tmp/fitnutri-ux-v3-frontend-rerun.log |
| Data | same private Python, sh scripts/check.sh --stage data | FAIL: 15 tests, 3 errors in legacy preflight fixtures; /tmp/fitnutri-ux-v3-data-rerun.log. Preserve for diagnosis; not a new business regression. |
| Backend | explicit TEST_DB_* injected privately from the dedicated container; MAVEN_ARGS=-o sh scripts/check.sh --stage backend | PASS; /tmp/fitnutri-ux-v3-backend.log |
| Browser / live AI | not run this round | NOT_RUN; existing verification is historical only |

Java XML totals [tests,failures,errors,skipped]: surefire: [467, 0, 0, 0]; failsafe: [4, 0, 0, 0].
Node24.14.0 / Java21.0.10 / existing PG16 at loopback55432 in fitnutri-integration-20261016. Every backend test owns a UUID DB; no user database migrated. Private synthetic credential not logged.

First attempts: system Python lacked jsonschema/openpyxl, corrected with repository requirements in a temporary venv; sandbox prevented Docker and loopback listen, rerun with approved permissions. These failures remain in /tmp/fitnutri-ux-v3-{static,data,frontend}.log. No skipped tests or weakened assertions.

Developer baseline recorded, with the data failure outstanding. Peer review PENDING; UAT PENDING. Independent frontend-only tasks may continue while the existing data fixture issue is diagnosed.
