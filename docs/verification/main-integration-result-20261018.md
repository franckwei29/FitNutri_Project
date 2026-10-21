# Main integration result — 2026-10-18

This branch integrates `origin/main` at `dd99dc5` into the reviewed handover snapshot, retaining the current V13 application contracts. This supersedes the earlier diagnostic review's statement that main is not integrated. The original develop checkout is not modified and no push is authorized.

## Adapted upstream changes

- #31: missing query parameters, missing upload parts and type mismatches now identify the offending field in the existing canonical error envelope. Rejected values and internal exception details remain hidden; request correlation, status codes and no-store handling are preserved.
- #32/#36: V1 and V2 food/exercise creation responses expose `Idempotent-Replay` consistently with their authoritative receipt body's `replayed` flag. CORS exposes this response header only through the existing allowed-origin configuration. No second idempotency mechanism or new database column is introduced. OpenAPI documents the additive response headers.
- #30/#35: the existing Q&A policy now blocks crisis expressions and additional eating-disorder terms from main, for both input and generated output. Crisis responses use fixed server text rather than model-written advice. Service tests prove no provider dispatch or private record-context construction on refusal. Existing eligibility/capability checks remain in force.
- #35/#38: the new assertions use existing test infrastructure, rather than adding incompatible H2 startup or disabling CSRF.

Upstream author: Haoran Liao. Commit references and original purposes are recorded in [the review](main-integration-review-20261018.md). These changes are adaptations, not verbatim cherry-picks.

## Explicit disposition of every upstream non-merge commit

| Commit | Disposition |
| --- | --- |
| `0282e2a` exercise voice | Preserve current SpeechController/EntryExtractionService and verified catalogue/date/weight behavior. Existing transcription, extraction, preview and browser tests cover this workflow. Do not add the incompatible old ExerciseCatalog calls. |
| `5145402` shared lookup | Preserve current reference/query/calculation services, immutable provenance and BigDecimal results. No parallel AFCD scanner or old DTO. |
| `d89bd06` food voice | Preserve current pending-candidate/review flow. Existing SpeechEvidence and extraction tests cover literal evidence and missing quantities. Model-estimated household grams are intentionally not adopted as authoritative input. |
| `6519d4d` Q&A | Adapt previously missing crisis and eating-disorder screens into current policy; retain `/qa/messages`, eligibility, no-store, private-context controls and feature gates. |
| `083dc93` errors | Adapt actionable field errors; current explicit English messages and canonical upload/protocol statuses remain. |
| `141cc77` idempotency | Keep stronger current receipt/hash/group/concurrency semantics and date policy; add replay response marker without accepting a parallel optional header-key identity. |
| `f044041` confidence removal | Already covered: current candidate/creation contracts omit or reject confidence. Keep richer candidate review fields. Legacy stored columns are not removed by this merge. |
| `4a927eb` references/portions | Already covered by immutable reference IDs, snapshots, exact portions and grouped food roots. Reject duplicate legacy mapped columns without migrations. |
| `dcc0835` test infrastructure | Retain isolated PostgreSQL and existing HTTP/controller/security tests; do not add H2/create-drop or tests without CSRF handling. |
| `e7fa718` API tests | Port missing policy/error/replay assertions. Preserve existing broader reference, summary, Q&A and speech suites; do not replace them with old endpoint assertions. |
| `eea84f4` CORS | Retain `APP_ALLOWED_ORIGINS`, credential/CSRF restrictions; expose the newly emitted replay marker and test origin rejection. |
| `9953e08` startup docs | Keep the verified current English fresh-start guide, explicit migration/import and private environment templates. Do not append incompatible auto-schema/auto-admin/H2 instructions. |
| `272df0f` voice tests | Existing tests retain fixed-clock dates, invalid/future/multiple-date rejection, audio validation, provider failure and no-save extraction. No fallback-to-today behavior regression. |
| `8e433f5` shared date helper | Current SpeechEvidence/RecordDatePolicy already share food/exercise date logic; retain them instead of introducing a second model-date authority. |

## Conflict resolution boundaries

All 14 text conflicts and automatically merged entity/test/README changes were explicitly reviewed. Current controllers, DTOs, repositories, migration policy and security configuration are the integration baseline. Upstream alternatives that duplicate already implemented requirements were resolved to current implementations; missing compatible behavior was then applied explicitly as above. This uses a normal two-parent merge, not an `ours` merge strategy.

No production database configuration, V1–V13 migration SQL, nutrition/MET values, frontend business flow, canonical submission identity or private environment files were changed. Added response headers are additive; existing receipt bodies remain authoritative.

## Validation

- Frontend: 113 Vitest files / 622 tests passed; legacy tests, boundary checks, proxy tests, production build, TypeScript and ESLint passed.
- Python data/preflight/trace suite: 45 tests passed.
- Backend final `verify`: 533 Surefire + 91 Failsafe tests passed; zero failures/errors/skips, independently counted from XML reports.
- Updated OpenAPI: validation and generated-type drift checks passed after adding the response headers.
- All production migration SQL, frontend business sources and application properties are unchanged relative to the local pre-merge snapshot.
- Browser suite: all 34 tests in 17 files passed in 9.1 minutes, against a separate newly initialized V13 database, imported catalogues, synthetic administrator and random loopback ports, with a real transport-fault proxy. No browser tests were skipped.
- Restart persistence: all 18 public-table content hashes unchanged after restarting the packaged backend with imports and administrator bootstrap disabled.
- Original develop HEAD (`a334fa9`) and its uncommitted status verified unchanged. No unresolved merge entries or whitespace errors remain.

The first backend run failed one new assertion because it searched the entire error body, including the existing request-path field. The assertion was corrected to inspect error message/field redaction without changing the production path contract; the complete backend suite then passed. No failing test was skipped or disabled.

Raw logs and browser artifacts remain ignored local files. No live paid AI-provider acceptance is implied by mocked-provider tests. All AI flags were disabled for the real-browser run; no user database or existing application service was selected.

## Delivery

The merge is committed locally on `review/main-integration-20261018` with main `dd99dc5` as the second parent. No push was performed. Only this run’s verified backend/frontend/proxy processes and uniquely named integration container are stopped after validation; test database contents are retained. Private evidence is under `.local/main-review-20261018/`, with browser JSON in `frontend/test-results/ux-v3/`. The earlier handover snapshot and diagnostic report remain in branch history.
