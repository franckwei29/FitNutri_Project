# FitNutri v2.0 implementation handover — developer verified, acceptance incomplete

2026-10-17 Australia/Sydney. This is an execution/evidence report, not a replacement development plan. The supplied v2.0 Markdown remains the sole specification; direct user branch/testing/safety/no-remote instructions take precedence. Historical plans/shared-chat instructions were not used as authority. All independently executable implementation/test/delivery work is committed on the required personal branch; final release is blocked by the explicit external/human items below.

## Branch Gate and baseline

```yaml
Project root: /Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6
Current branch: develop/yuankang-wei
Required branch: develop/yuankang-wei
BASE_COMMIT: 117348a01ded0a08ac82571fafb7b03103bfb50e
Implementation/test HEAD before final report: b2531a7cc42c1ab824b714e0a657681b948cb3bf
Report commit: resolve local subject [R03]; final HEAD is reported after committing
Pre-existing changes: 8 untracked metadata entries preserved, excluded from commits
ZIP baseline comparison: initial project97/97 and Demo24 paths/bytes PASS; subsequent differences are intentional local implementation
Branch Gate: PASS
```

`implementation/baseline.md` records original ZIP/spec hashes and actual Git gate. Final audit reverified97baseline files against the original ZIP using the declared checkout EOL filters (`backend/mvnw.cmd` is explicitlyCRLF), all24external Demo files byte-for-byte, and the unchanged sole-spec SHA256. Initial audit diagnostics from suffix ambiguity/normalized Git bytes were corrected in the comparison method, without modifying baseline/source files. No fetch/pull/clone/push/PR/merge, alternate branch, Git initialization, stash/reset/clean or remote baseline substitution. Existing main/backup branches were not used for development. The old tracking ref was not refreshed; ahead counts are not remote freshness evidence.

Pre-existing untracked metadata: root`.DS_Store`, `.idea/`; backend`.DS_Store`, `src/.DS_Store`, `src/main/.DS_Store`, `src/main/java/.DS_Store`, `src/main/java/com/.DS_Store`, `src/main/java/com/fitnutri/.DS_Store`. No implementation files are intentionally left uncommitted. Dependencies/dist/JARs/source caches/private credentials/dumps/test artifacts stay ignored. The task ledger has69unique IDs:3preparationDONE,62IN_REVIEW,4BLOCKED(A05/A10/Q08/R03); no TODO or unreported implementation bucket. Every row has a real local commit reference; R03 uses its subject selector to avoid a self-referential SHA.

## Milestones and acceptance

| Scope | Actual implementation / developer self-check | Real local integration | Peer review | UAT / release |
|---|---|---|---|---|
| M1 CORE | Real registration/login/session/CSRF; atomic adult questionnaire; multi-food/exercise manual entry; shared review/cancel/confirm; Dashboard; immutable snapshots/receipts/null energy | PASS cited real IAB + fresh Chrome journeys and SQL/audit checks; lost-response/late-commit recovery and atomicity/concurrency tested | PENDING | PENDING; desktop matrix remainder prevents final compatibility sign-off |
| M1 SUPPORT | History URL/date filters/details; versioned corrections/soft delete; Profile/null goals/energy units/dated weight corrections; trends/rule facts; minimal admin versioned access control | PASS cited actual support journeys, independent Chrome/IAB version409, admin revocation/failed login/restore/re-login with data retained | PENDING | PENDING; not a human acceptance declaration |
| M2 FULL | Actual image adapter/file/candidates; real Recorder/audio decode/transcription/edit/parse; voice-food/voice-exercise shared review; stateless consent-safe real QA adapter/UI | Controlled provider +actual Spring/PG/canonical Confirm PASS; actual unavailable/restriction UI PASS. Live provider/human microphone NOT RUN | PENDING | BLOCKED_EXTERNAL_SERVICE/HARDWARE; **M2 not complete/accepted** |
| Delivery | Unified fail-fast checks; explicit local/HTTPS-capable same-origin build gateway; immutable executable startup; protected backup/new-DB restore and real restored login/read | Local PASS. Public HTTPS hosting/Secure-cookie browser acceptance NOT RUN, no public deployment performed | PENDING | R03 final sign-off BLOCKED until missing dependencies/independent acceptance |

M1 can be reviewed independently of M2 under v2.0 §22. Its developer implementation is ready for review; it is not labelled fully accepted because browser/human gates remain.

## Actual test results and reproduction

| Check | Actual result / command |
|---|---|
| Full unified gate | `FITNUTRI_PYTHON=<private-venv>/bin/python MAVEN_ARGS=-o TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=<private-synthetic-value> TEST_DB_DOCKER_CONTAINER=fitnutri-integration-20261016 ./scripts/check.sh`:exit0; log `/tmp/fitnutri-q07-final-all.log` |
| Java |467Surefire+4Failsafe PASS/0fail/error/skip; actual PG16 transactions/uniqueness/concurrency/rollback/migrations/official source import/dump-restore. XML independently summed;65s Maven |
| Advanced actual HTTP/PG |Image6+Transcription5+Parsing6+QA9+unconfiguredQA1=27PASS; actual local controlled-provider HTTP is separate from live smoke |
| Clean frontend install | `npm --prefix frontend ci --offline`:356packages installed,0reported vulnerabilities; no browser binary installation |
| Fresh frontend tests | `npm --prefix frontend test -- --maxWorkers=2`:63files/343PASS/0FAIL/0SKIP,41.78s; `/tmp/fitnutri-q08-fresh-frontend.log` |
| Frontend TS/lint/build | `typecheck:test`, `lint`(0warnings), `build` all exit0 after clean install; build automatically enforces production boundary |
| Production boundary |117reachable source modules/507edges/30manifest entries/32text artifacts/0violations; AST plus actual built output, not filename-only exclusion |
| Original Demo regression |Original Node15PASS +byte-integrity1PASS +legacy strict TS. This is archived simulation regression, not M2 live service evidence |
| Other tooling |Python15PASS; production-boundary11PASS; transport fault-proxy8PASS; final pipeline4/gateway7/immutable-artifact2=13PASS |
| API contract |44operations/59schemas;564positive examples +11negative cases;59generated TS/Zod no drift; no skipped validation |
| Browser |Actual CUA IAB and desktop Chrome scopes in browser-matrix.md, including separate cookie stores. Standalone Playwright9cases discovery/types only, runner NOT RUN |
| Response-loss/late commit |Actual frontend/Spring/PG: one original POST/two rows/one proof; lost ack recovered; receipt404 while original transaction waits does not generate another batch; submission-network-recovery.md |
| Current source recovery |Actual protected host dump restored to new DB; current executable clone real CSRF/login/date/energy/null/provenance/foreign-owner404 reads PASS; source/clone reports unchanged |
| Timing |Actual warm Chrome navigation→saved metric visible534/443/544ms incl automation round trips; local scenario only, performance.md |

No test failure was concealed with empty assertions, skipped tests, disabled CSRF/permissions or changed business requirements. Significant RED/fix evidence remains in paired reports: date/input automation mismatch, async node readiness, unknown-result guards, HTTP500 protocol mapping, incomplete-schema startup, and mutable running-JAR packaging failure. Appropriate checks were rerun after each fix. The final Java business code is unchanged since the global pass; later runtime helper changes have their own13passing Node tests. Fresh FE dependencies/tests/build were independently reverified.

## Changed files, source reuse and backend/database effects

`implementation/changed-files.txt` inventories all baseline-to-delivery paths, including this report package. `implementation/tasks.md` maps69cards to actual commits/test evidence; `backend-change-log.md` covers BC-01–BC-16 and follow-up reasons/contracts/recovery. Use `git show --stat <SHA>` for each exact review package. No peer/team approval is inferred from an AI engineering role.

Demo theme/styles/presentation/navigation/form/editor/record/trend/admin/chat layout were adapted into real React/MUI components. Final actual symbol/path map is in `implementation/source-map.md`. Production business authority moved from Demo Store/LocalStorage/seed/fixed answer to strict API/session/Java/PostgreSQL. Original Demo files/tests remain traceable; the source archive was not copied over the repository.

- BC-01/02/03/12: canonical registration/password/session/CSRF/current active roles; versioned atomic profile and independent dated weights; ordered administrator state locks/minimal DTOs/self-last-admin guards.
- BC-04/05: immutable versioned reference provenance and pinned real official inputs, with3741verified AUSNUT foods and15selected official activities. Legacy references retained and not relabelled verified.
- BC-06/07/08/09/10: LocalDate/canonicalkJ decimal snapshots, durable owner/UUID/hash receipts, all-or-none creation/replay, real directory calculations, dated weight/MET/unavailableNULL semantics, owner-scoped corrections/soft deletion/immutable original proofs.
- BC-11/13: SQL owner/date/nondeleted factual summaries and known-day averages/gaps/current explicit targets; canonical safe errors/capabilities/request UUID/minimal audit; no raw private request/provider bodies in logs. Framework404/405/415 preserved, private API no-store.
- BC-14: V1–V8 opt-in/incremental migrations, preflight/identity/timezone/verified-backup gates, no auto-baseline/clean/DDL writes; read-only successful reviewed history startup gate. A V4 expanded schema was actually shown insufficient and now fails before readiness.
- BC-15/16: server-only configured transport/media limits/deadlines/strict outputs; metadata-free image/audio handling; literal pending extraction/real matching; fresh QA consent/owner/range whitelist/no tools/history; no automatic record/chat writes or fake successful AI fallback.

Schema files V1–V8 are preserved with raw legacy columns/PHOTO/calorie/timestamp inputs; no destructive downgrade or new late migration. Original provided V1 baseline remains unadopted/untouched; review DB was upgraded only after its earlier separate protected clone rehearsal. No shared/school database rollout occurred. Older application binary on V8 is not certified; supported recovery is a new restored compatible DB/application pair, not down-SQL deleting columns.

## Runnable local delivery and recovery

Main actual review origin: **http://localhost:8787** (production dist/API same-origin gateway, no Vite). Normal dev origin http://localhost:5173 is restored with direct8080 proxy. Spring binds127.0.0.1:8080; PG16 loopback55432; owned review DB`fitnutri_integration_browser_20261016`. Bootstrap/import/model flags off. All demo identities/data are synthetic, no seeded production login UI. Private synthetic credentials remain ignored `.local/e2e`; do not copy them or real secrets into Git/chat.

Read `../runbook.md` and `../../deploy/README.md`: explicit DB environment, reviewed migration/import sequence, Node24/Java21/PG16/FFmpeg, health/catalogue checks, local and trusted-HTTPS configuration. After verify, `./scripts/dev.sh backend-built` starts a hash-addressed protected immutable JAR; `frontend` starts Vite; explicit `FITNUTRI_LOCAL_HTTP=1 ... gateway` runs the local built-frontend proxy. No automatic schema adoption, package install, official data fetch or paid model probe at startup. Default non-local cookies remain Secure; public HTTPS certificate/hosting/firewall were not provisioned.

Final synthetic snapshot backup: ignored private `.local/db-backups/final-v8-8d4ce37e91504264901911dfcffbab25/state.json` =APPLICATION_RESTORE_VERIFIED. Dump0600/directory0700, actual archive SHA recorded. New restored DB`fitnutri_it_8d4ce37e91504264901911dfcffbab25` retained; its temporary8083 process stopped. Snapshot totals preserve10physical foods/3activities/8receipts/6users/5profiles/6weights including soft-deleted rows. Source-before/source-after/restored/post-login reports equal. Main DB/process left running; old faultproxy8082 stopped. Dedicated PG uses tmpfs, so retain the verified host dump before stopping/removing its container; do not run down-v or overwrite source.

Actual final dashboard screenshot: `screenshots/final-local-dashboard.png`. Current retained USER4 record quantities/energy restored through normal reviewed corrections(food7/version3), account enabled/version4 after independent admin tests; original receipts unchanged. New Chrome core USER6 retained2foods/1missing-energy activity/2receipts. The earlier date-input diagnostic USER5 remains with no records/proofs; no test data was deleted to make results look clean.

## Remaining blockers and ownership of acceptance

| Task/scope | Remaining condition | Current truth |
|---|---|---|
| A05 | Actual microphone/hardware/OS permission/indicator/browser codec lifecycle | Code/fake-media lifecycle PASS; actual hardware NOT RUN |
| A10 | Private authorised API key/account-available models/permitted budget, two owned food images and real human food/exercise speech; live QA | Presence-only key/models absent, live requests0; provider-smoke.md records per-path BLOCKED_EXTERNAL_SERVICE |
| Q08 | Actual versioned Chrome/Edge/Safari matrix; correct device viewport verification; advanced live media cases | Chrome real cited flows PASS, version page blocked by HTTP/HTTPS-only tool policy;375override stayed1710 and was reset; Edge not connected; native Safari timed out. No workaround/version/browser pass fabricated |
| R03 release | Missing advanced/browser gates plus independent review/UAT | Handover concrete and committed; final sign-off BLOCKED, not DONE |
| Shared/public rollout | Target-specific preflight/timezone/backup/certificate/origin/owner approval | NOT RUN; no shared/public deployment or remote publication |

All69rows explicitly retain Peer review:PENDING and UAT:PENDING. Developer self-test does not stand in for group Code Review or user acceptance. Conservative QA scope/output rules still require semantic/safety/accuracy review; controlled tests cannot prove an arbitrary live model's behavior. R03 may be resumed when the listed external/human prerequisites exist. There is no unstarted independent business work package hidden behind a new plan or approval request.
