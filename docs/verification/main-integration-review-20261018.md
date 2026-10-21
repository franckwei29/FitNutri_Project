# Remote main integration review — 2026-10-18

## Outcome

Do not merge remote main directly into the current development branch. The trial merge produced 14 conflicted files. A diagnostic choice of the local version for all 14 still produced four Java compilation errors in automatically added main code. Additional schema, API, security and documentation incompatibilities require an intentional integration design.

The restored local snapshot passed Java 21 / Maven `clean compile` after the diagnostic merge was aborted. The mixed version did not compile, so no runtime or full regression pass is claimed for it.

This is a review, not a completed integration. No database, application server or paid AI provider was started. No remote branch was pushed. The trial merge was aborted after recording the evidence.

## Exact comparison and isolation

- Original checkout: `Lab_practical_05_Group_6`, branch `develop/yuankang-wei`, HEAD `a334fa9`.
- Separate worktree: `../main-integration-review`, branch `review/main-integration-20261018`.
- Local review snapshot: `bfbcece`, containing the original HEAD plus the 35 proposed handover paths, including the deleted/renamed guide. The original checkout remains uncommitted and unchanged. The workstation-only untracked `scripts/test-local.py`, private environments, caches and databases were not copied.
- Remote main fetched from origin: `dd99dc5` (latest commit dated 2026-10-18 14:30:55 Australia/Sydney).
- Common ancestor: `117348a01ded0a08ac82571fafb7b03103bfb50e`.
- Main-only history: 26 commits, comprising 14 non-merge commits and 12 PR merge commits (#28–#39).
- Main changed 43 files: 2,911 insertions and 78 deletions. No frontend source or Flyway migration SQL changed in this range.
- Fetch updated the local remote-tracking reference `origin/main`; it did not advance the checked-out develop branch.

## What main added, and why

The purposes below come from commit messages and implementation comments, not assumptions about the author's private intentions.

| Area | Commits / PRs | Stated purpose | Relationship to the current development code |
| --- | --- | --- | --- |
| Exercise voice entry | `0282e2a`, #28 | Turn audio/transcript into a pending exercise entry and estimated energy | Current code already has `/api/speech/transcribe` and `/api/speech/parse`, catalogue-backed previews and dated-weight rules; main adds a separate `/api/voice/exercise` design. |
| Food voice entry and shared reference lookup | `5145402`, `d89bd06`, #29 | Share reference search/calculation; extract pending food entries, distinguish guessed portions and avoid saving before confirmation | Useful requirements, but main uses old AFCD/double/DTO structures; current code uses verified versioned references, BigDecimal, fingerprints and reviewed candidate selection. |
| General health Q&A | `6519d4d`, #30 | Enforce eligibility, refuse out-of-scope questions, avoid sending private profile data, and handle model failures | Main uses `/api/qa/ask`; the current UI uses `/api/qa/messages` through `QaService`, with a different request/response contract and capabilities. |
| Consistent errors | `083dc93`, #31 | Standardize invalid parameter/date/enum/upload responses and keep validation messages English | Port any genuinely missing cases into current `ApiError`/field-error/request-ID handling; do not replace the current error contract. |
| Duplicate-save prevention and dates | `141cc77`, #32 | Use an optional `Idempotency-Key` header, replay prior saves, and reject timestamps more than five minutes ahead | Current code already uses submission IDs, canonical request hashes, atomic receipts, concurrency controls and calendar-date rules. These are different mechanisms and should not run side by side. |
| Remove model confidence | `f044041`, #33 | Stop exposing unsupported confidence scores in image/food responses | The current `FoodCandidate` already has no confidence field. Preserve its richer review contract; do not replace it with main's two-field candidate. |
| Reference-linked confirmed portions | `4a927eb`, #34 | Store the selected reference/portion and compute nutrients server-side instead of trusting client numbers | Current code already uses immutable reference IDs, snapshots, exact portions and grouped records. Map any missing requirement into that model. |
| API and unit tests | `dcc0835`, `e7fa718`, #35 | Add HTTP-level tests using H2 and mocked providers, without PostgreSQL or real API keys | Reuse test scenarios, but adapt them to PostgreSQL fixtures, CSRF, current routes and current provider interfaces. |
| Configurable CORS | `eea84f4`, #36 | Allow alternate frontend origins and expose `Idempotent-Replay` | Origin configuration already exists as `APP_ALLOWED_ORIGINS`; main introduces a different property name. A replay header is useful only if the chosen response contract actually emits and consumes it. |
| Getting Started | `9953e08`, #37 | Explain setup, API keys, CORS and tests | Main's guide describes its older runtime. Keep the newly verified current handover guide. |
| More voice tests and shared date helper | `272df0f`, `8e433f5`, #38–#39 | Cover uploads/date behavior and remove duplicated date parsing between food/exercise endpoints | Reuse relevant scenarios with `SpeechEvidence`/`RecordDatePolicy`. Main's fallback-to-today policy differs from the current explicit review/rejection rules. |

## The 14 textual conflicts and recommended treatment

Paths below are under `backend/`.

| Conflicted file | Recommended integration approach |
| --- | --- |
| `src/main/java/com/fitnutri/backend/common/ApiExceptionHandler.java` | Retain current error envelope, field violations, request IDs and sanitized errors; add missing exception cases individually with contract tests. |
| `src/main/java/com/fitnutri/backend/food/FoodCandidate.java` | Retain current candidate key, proposed name, reference matches and warnings. It already excludes confidence. |
| `src/main/java/com/fitnutri/backend/food/OpenAiVisionService.java` | Retain current provider abstraction, feature controls and candidate validation; selectively port useful cases rather than the old response parser. |
| `src/main/java/com/fitnutri/backend/foodreference/FoodReferenceController.java` | Retain versioned references, immutable IDs, BigDecimal and preview/fingerprint contracts. Adapt shared lookup ideas through current services. |
| `src/main/java/com/fitnutri/backend/qa/QaController.java` | Keep `/api/qa/messages` and its service. If `/ask` compatibility is needed, design a documented adapter instead of replacing the controller. |
| `src/main/java/com/fitnutri/backend/records/CreateFoodRecordRequest.java` | Keep the current submission/reference/date contract; add genuinely missing portion metadata only after updating OpenAPI, persistence and UI together. |
| `src/main/java/com/fitnutri/backend/records/ExerciseRecordController.java` | Keep current correction/read boundaries; do not restore a second creation route or bypass grouped-record protection. |
| `src/main/java/com/fitnutri/backend/records/ExerciseRecordRepository.java` | Keep current owner, receipt and group query semantics. Do not add a parallel header-key lookup without an explicit compatibility design. |
| `src/main/java/com/fitnutri/backend/records/FoodRecordController.java` | Keep current service-based correction/deletion paths. Main's direct entity save/delete is not compatible with receipt and group invariants. |
| `src/main/java/com/fitnutri/backend/records/FoodRecordRepository.java` | Preserve existing reference/group/submission queries; adapt additional queries to the chosen single persistence model. |
| `src/main/java/com/fitnutri/backend/records/FoodRecordResponse.java` | Preserve the UI/OpenAPI response structure and exact values. Do not replace it with the old calories/portion DTO. |
| `src/main/java/com/fitnutri/backend/security/SecurityConfig.java` | Retain CSRF, active-account checks, USER/ADMIN separation and current origins property; selectively evaluate the exposed-header addition. |
| `src/main/resources/application.properties` | Keep explicit private database variables, `ddl-auto=validate`, explicit migrations and AI feature flags. Do not restore default development credentials or automatic schema updates. |
| `src/test/java/com/fitnutri/backend/BackendApplicationTests.java` | Keep the current isolated PostgreSQL/schema test foundation and port useful assertions. |

## Hidden conflicts that Git does not flag

### 1. New source does not compile against current types — observed

For diagnosis only, all 14 conflicted paths were temporarily resolved to the local snapshot, while retaining main's automatic additions. Java 21 / Maven `compile` failed with four errors:

- `FoodReferenceSearchService.java:48`: main constructs the old ten-argument `FoodNutritionResponse`; current code requires `(ReferenceInfo, BigDecimal, NutrientValues, String)`.
- `VoiceExerciseController.java:79`: current `ExerciseCatalog` has no `canonicalName(String)`.
- `VoiceExerciseController.java:96`: no `estimateCalories(String, Integer, double)`.
- `VoiceExerciseController.java:110`: no `names()`.

This proves that choosing “ours” for conflict markers is insufficient. It does not establish that every possible resolution fails; a deliberately adapted integration remains feasible.

### 2. Entity changes lack current migrations — static finding

Git automatically merged `idempotency_key` into `BaseRecord`, unique constraints into food/exercise entities, and `portion_quantity`, `portion_unit`, `reference_food_key`, `reference_food_name` into `FoodRecord`. No migration in the fetched change adds those columns to the current V13 schema. With current Hibernate validation, mapped missing columns would prevent startup. This was identified by source/schema comparison; no database was started to reproduce it.

Retain the existing receipt/reference model where it satisfies the requirement. If a new field is genuinely needed, use a new reviewed migration after V13, update schema readiness expectations and test fresh installation plus upgrade/restore in an independent database. Never edit applied V1–V13 checksums or switch to `ddl-auto=update` as a shortcut.

### 3. “Take theirs” would also be unsafe — static finding

Current `FoodCreationController` and `ExerciseCreationController` already own the base food/exercise POST routes. Main places those POST routes back into the record controllers. Retaining both designs would create duplicate mappings. Main's security file also disables CSRF, whereas the current frontend and tests require tokens. These are integration incompatibilities, not a claim that main was designed for the current architecture.

### 4. Automatically merged tests use incompatible infrastructure — static finding

Main adds test-scoped H2 and an `application-test.properties` profile with H2 and `create-drop`. Its HTTP helper does not acquire/send current CSRF tokens. Current migrations, PostgreSQL-specific constraints, readiness checks and mocked provider types differ. Do not import these tests unchanged or weaken current guards to make them green. Port their cases into existing isolated PostgreSQL tests and mock the current provider abstraction.

### 5. README would silently contradict the verified guide — observed text merge

Git appended a second Getting Started section without conflict. It says the backend creates tables/imports roughly 1,600 foods automatically, the first registered user becomes administrator, older Node versions are sufficient, and tests use H2. Current setup requires Node 24, explicit V1–V13 migration, reviewed reference imports, private configuration and explicit administrator provisioning. Keep the current guide as the source of truth and incorporate only compatible explanatory improvements.

### 6. Runtime contracts differ even where intent matches — static finding

- Main `/api/voice/*` is not wired to the current UI's `/api/speech/*` flow; adding endpoints alone does not upgrade the UI.
- Main date parsing can replace invalid/future spoken dates with today; current policy rejects/requires review and derives supported dates independently. Decide the product behavior explicitly rather than silently changing it.
- Main food lookup scans all references and labels values AFCD; current active/versioned AUSNUT provenance and exact calculation must remain consistent.
- Main's optional header-key replay does not replace current hash-based detection of reusing the same submission identity for different inputs, or atomic grouped receipts.

## Recommended order for a later real integration

1. Agree on the current OpenAPI routes, verified reference model, receipt semantics and date policy as the integration target. Inventory each main requirement as already covered, useful addition or incompatible alternative.
2. Bring over missing test scenarios first, adapted to current interfaces and independent PostgreSQL fixtures. Preserve authorship/PR references in the integration notes.
3. Apply small compatible improvements (for example missing error cases). Keep one origins setting and one authoritative setup guide.
4. Port any genuinely new voice/Q&A behavior through existing services and capability gates. Do not add a parallel provider stack simply to make old classes compile.
5. Introduce new database fields only if required, with a new migration and independent upgrade/restore validation. Keep existing private databases untouched.
6. In a review branch, finish the explicit merge resolution and run compile, static/contracts, full backend/frontend/data suites and real isolated browser journeys before considering develop integration. Never use a blanket `ours` merge strategy to mark unreviewed main work as integrated.

## Evidence and final state

Private local evidence is retained under `.local/main-review-20261018/`: exact conflict paths, raw conflict diff, automatic-merge diff, failed diagnostic compile log and restored-snapshot compile log. No merge remains in progress. The current development checkout's status was compared byte-for-byte with its pre-review status; original HEAD remains unchanged. The review branch contains the complete handover snapshot and this report, not a completed main merge.
