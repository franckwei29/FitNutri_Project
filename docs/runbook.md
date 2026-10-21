# FitNutri local and production-style runtime

Historical V2/V8 runtime instructions. For a fresh current V3/schemaV13 installation, start with [the team setup guide](getting-started.md). For historical review operations, use [the active operational handover](verification/ux-v3-uat.md) and [migration evidence](verification/ux-v3-migration.md). This file does not certify upgrading the old8787 review runtime.

Historical schema V8. Preserve React/TypeScript/Vite/MUI, Java21/Spring Boot4.1.1/Spring Security Session and PostgreSQL16. Task/evidence status is in `implementation/tasks.md`; developer checks do not imply peer review/UAT or completed M2 live-provider acceptance.

## Prerequisites and checks

Use Node24, Java21, Maven wrapper and PG16. Actual validation environment: Node24.14, Java21.0.10, PostgreSQL16.15, FFmpeg6.1.1. Install only explicit locked dependencies:

```sh
npm --prefix frontend ci
python3 -m venv /private/operator/fitnutri-tools
/private/operator/fitnutri-tools/bin/pip install -r scripts/contract-requirements.txt -r scripts/data-requirements.txt
```

Choose an operator-owned venv path. FFmpeg must be installed locally for speech/developer audio tests; set a trusted absolute `FFMPEG_BIN` if absent from PATH. No decoder/model is downloaded at application startup. See `development.md` for full `scripts/check.sh` gates and explicit isolated `TEST_DB_*` configuration. Paid provider calls are never part of default checks.

All work/commits for this execution belong to `develop/yuankang-wei`. No remote Git operations were performed. Preserve existing `.idea`/`.DS_Store` files. Never point automated tests at shared/application data or run database cleanup.

## Explicit database selection and migration

Backend now requires private `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (standard `SPRING_DATASOURCE_*` overrides also supported). Committed development-password defaults were removed from application/Compose configuration. No default startup migration or directory seeding occurs; Hibernate validates only. A read-only startup gate additionally requires successful reviewed V1–V8 migration history; older/unknown future schemas fail rather than being silently adopted.

For a **new owned local PG16 instance**, privately set `DB_PASSWORD`, optionally `DB_DATABASE`/`DB_USERNAME`/`DB_PORT`, inspect `docker compose config --quiet`, then explicitly start `docker compose up -d db` if that is the chosen instance. Compose binds127.0.0.1 and retains its existing named volume. Do not use `down -v`, recreate another person's database, or assume a volume is empty. The task's dedicated existing container/port55432 remains separate from this optional Compose definition.

Before any existing database migration: read-only `scripts/db/preflight.sh`, inspect actual schema/duplicates/timezone, create and restore-verify a protected backup, then explicitly approve reviewed adoption/migration. `scripts/db/adopt-baseline.sh` requires the V1 signature; it never infers that a nonempty schema can be baselined. `scripts/db/migrate.sh` requires `FITNUTRI_DATABASE_IDENTITY`, `FITNUTRI_MIGRATION_APPROVED=1` and `FITNUTRI_BACKUP_VERIFIED=1` in addition to explicit DB credentials. `verify.sh` is read-only. Full commands/gates are in `db/migration-foundation.md` and `db/restore-rehearsal.md`.

Legacy timestamp conversion requires reviewed `LEGACY_RECORD_ZONE=Australia/Sydney` (or another supported IANA zone) **and** `LEGACY_RECORD_ZONE_APPROVED=true`; unknown history intentionally blocks migration. Do not infer shared approval from this local example. Do not edit applied Flyway checksums or use automatic downgrade/drop SQL.

The original `fitnutri_integration_baseline` remains V1 without Flyway adoption; never upgrade it. Current owned synthetic review database `fitnutri_integration_browser_20261016` was separately backed up/restored/verified and is already V8. Its existing users, records and receipts are retained. Dedicated test PG uses tmpfs; removing/restarting that container loses its synthetic state, so retain required backups separately before such an action.

## Verified reference data

Pinned actual source bytes live only under ignored `.local/reference-sources/`. Follow `development.md`: fetch official AUSNUT/Compendium files explicitly, verify hashes and prepare normalized manifests; do not invent missing values. This implementation uses verified AUSNUT3741 foods and15 selected official activity types. Existing legacy reference rows remain traceable.

After reviewed migration, explicit one-time backend environment `IMPORT_FOOD_CATALOGUE=true IMPORT_ACTIVITY_CATALOGUE=true REFERENCE_SOURCE_DIR=<absolute-reviewed-cache-path>` imports only reviewed manifests with actual source hashes. Missing/mismatched provenance fails startup; ordinary startup leaves imports false. Disable those flags again after the verified import. Read authenticated `/api/capabilities` and confirm both `catalogues.*.ready/verified` before recording. A port or process alone is not directory readiness.

## Local development

In a private shell select the owned local database:

```sh
export DB_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_browser_20261016
export DB_USERNAME=fitnutri_test
# Set DB_PASSWORD privately; do not paste secrets into chat or tracked files.
export APP_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8787
export APP_BOOTSTRAP_ADMIN_ENABLED=false
./scripts/dev.sh backend
```

For a packaged local service, run full verify first, then use `./scripts/dev.sh backend-built`: it stages a SHA256-named immutable copy under ignored `.local/runtime` before starting Java. The directory is0700; JARs are0400. Never run Java directly from Maven’s mutable target JAR while rebuilding. Development `backend` remains the Maven source-run option and should be restarted after source compilation.

In another terminal run `./scripts/dev.sh frontend`, then `./scripts/dev.sh health`. Local profile binds Spring127.0.0.1:8080 and explicitly permits HTTP Session cookies. Vite binds localhost5173 with strict port and direct `/api`→8080 proxy. Use one browser hostname consistently. No fault-injection proxy is needed for normal development.

The local health command checks DB-backed health and CSRF without printing tokens. Register does not log in. Fetch fresh `/api/auth/csrf` before register/login and after login/logout; mutations send `X-CSRF-TOKEN`. Roles and enabled state are checked on the server. A USER completes adult onboarding before records; ADMIN has no private-profile/record access.

Administrator provisioning is explicit backend-only `APP_BOOTSTRAP_ADMIN_ENABLED=true` plus privately configured email/password/display-name; it never promotes an existing ordinary user. Disable bootstrap after successful provisioning. No default administrator password/account is supplied. Existing review synthetic admin is retained; credentials are local private test artifacts.

## Production build with same-origin API

`npm --prefix frontend run build` enforces the production dependency/secret/mock boundary. `deploy/server.mjs` serves the actual dist and proxies API to loopback Spring, without Vite. See `../deploy/README.md` for trusted TLS certificate/key configuration; HTTPS is required by default, and backend `SESSION_COOKIE_SECURE=true` remains the non-local default. Set exact public HTTPS origin in `APP_ALLOWED_ORIGINS`, keep Spring loopback and provider keys in the backend environment only. No public deployment, certificate issuance or firewall change is authorised/performed here.

The owned loopback demonstration uses `FITNUTRI_LOCAL_HTTP=1 ./scripts/dev.sh gateway` and `http://localhost:8787`, with the explicit local backend profile. This validates production build/routing/proxy mechanics; it does not certify a public HTTPS deployment. Local HTTP refuses a public bind. Cookie domain remains host-only, path`/`, HttpOnly, SameSite=Lax; HTTPS must retain Secure. API/CSRF/private responses are no-store, static hashed assets immutable. SPA refresh/history/review work; missing API/assets never fall through to HTML.

## Optional real AI configuration

Privately configure `OPENAI_API_KEY` and the explicitly account-available `OPENAI_IMAGE_MODEL`, `OPENAI_TRANSCRIPTION_MODEL`, `OPENAI_PARSING_MODEL`, `OPENAI_QA_MODEL`. Enable only selected `AI_IMAGE_ENABLED`, `AI_TRANSCRIPTION_ENABLED`, `AI_PARSING_ENABLED`, `AI_QA_ENABLED`; all defaultfalse/models blank. No model choice is guessed. Credentials belong only to Spring; never `VITE_*`, browser storage, request logs or Git. Optional `OPENAI_BASE_URL` is HTTPS without credentials/query/fragment; no redirect credential forwarding. Readiness is configuration/adapter/data/user scope and a local decoder probe, never a paid smoke.

Image: actual JPEG/PNG/WebP, ≤8MiB/4096pixels per side, metadata stripped in memory. Audio: browser-supported WebM/MP4 intersection, ≤8MiB and60decoded seconds; fresh16kHz monoWAV sent only on explicit Transcribe. FFmpeg pipe-only decoding is bounded10s/32MiB allocation/2parallel decoders. Missing key/model/decoder leaves relevant features unavailable while M1 remains usable. Servlet acceptance10MB, gateway/rejected-body drain12MiB. Model processing deadline60s; gateway85s; no automatic provider retries.

Recognition/extraction only produces editable candidates. Every food/exercise path selects actual references and confirmed quantities/date, then enters the same Review and explicit Confirm transaction. Q&A sends only the current question and explicitly selected ≤31day minimal recorded facts. Consent/eligibility is rechecked before dispatch and output; no chat persistence, diagnosis or prescription. Withdrawing consent does not disable ordinary records/factual summaries.

Actual provider smoke requires authorised configuration, owned/synthetic inputs and separate A10 evidence; controlled provider/codec tests are not model accuracy or real microphone acceptance. Pending configuration/hardware/browser/peer/UAT are recorded individually rather than represented as successful M2.

## Failure, recovery and current services

Preserve a frozen unknown submission UUID and query its original receipt; do not edit/repost with a new UUID while outcome is unresolved. A404 receipt while the original transaction is pending is not proof it will never commit. Cancel/abort is not database rollback. Account conflicts require reload/review rather than overwriting newer versions.

For application rollback, first prove old-code/new-schema compatibility in a fresh clone; no V8→V1 automatic downgrade is supported. Otherwise restore the protected backup into a **new** isolated database, compare counts/checksums/energy/date/legacy states, verify login/record reads and switch only after human deployment approval. Never overwrite the running source database. `scripts/db/backup.sh` refuses existing files and uses0600; restore refuses existing destinations and requires explicit approval. A dump file alone is not restore evidence.

Current local service commands/evidence and exact verification limitations are in `verification/same-origin-runtime.md`, `verification/local-runtime-v8.md`, `verification/m1-evidence.md` and `verification/support-evidence.md`. Remote CI, public HTTPS hosting, peer Code Review and UAT remain distinct and pending.
