# Runtime inventory — P01

2026-10-16 Australia/Sydney. Branch develop/yuankang-wei; baseline 117348a.

| Runtime | Observed |
|---|---|
| Node | v24.14.0 (.nvmrc requires 24) |
| npm | 11.9.0 |
| Java | Homebrew OpenJDK 21.0.10 |
| Maven Wrapper | 3.9.16, wrapper distribution resolved locally |
| Spring Boot | 4.1.1, compile succeeded offline |
| Hibernate | 7.4.5.Final observed during baseline context startup |
| React / Vite / TS | installed 19.2.8 / 8.2.2 / 6.0.3 |
| Docker CLI | 29.7.2; daemon socket unavailable during baseline |
| PostgreSQL CLI | not on PATH |

`npm ls --depth=0` succeeded: installed versions satisfy the supplied lock/manifest. No dependency upgrades or lockfile changes. No configured DB_/PG/AI_/OPENAI_/SPRING_ environment names found; no values printed.

Baseline Java test command explicitly targets isolated loopback port 55432 and synthetic test credentials, never the application default database. Failed before a connection was established. PostgreSQL integration and actual legacy schema remain BLOCKED until an isolated PG16 can run. Docker Desktop start attempted; no existing volumes touched. Do not run default application tests against the unknown localhost:5432 database.

No official AUSNUT manifest/headers or audited 2024 activity data supplied. No real-provider calls attempted; missing authorised environment configuration is an external blocker, not a reason to introduce simulated runtime results.

## Resolved baseline environment blockers

Docker Desktop started successfully. Dedicated container `fitnutri-integration-20261016`, PostgreSQL16, loopback55432, synthetic database `fitnutri_integration_baseline`, tmpfs data, no existing volume mounted. Original Java test rerun: 1 pass/0 failures/0 errors/0 skips. This only proves context startup. Loader inserted1588 AFCD rows in the synthetic test database, not official AUSNUT verification. Existing services untouched.

Safe-copy npm ci --offline --ignore-scripts --no-audit --no-fund: exit0,280 packages installed in /tmp/fitnutri-baseline-dybdlu5o. Original lockfile unchanged. Full lifecycle install remains subject to its normal scripts if later needed; existing production build/lint passed.

OpenAPI verification uses /tmp/fitnutri-validation-tools virtualenv, dependencies pinned in scripts/contract-requirements.txt. Host Python3.9 emits a LibreSSL/urllib3 warning; validation is local and no URL is fetched.
