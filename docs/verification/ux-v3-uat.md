> Historical V3 acceptance checkpoint. The active V3.1 UI/save navigation and current unchecked human UAT are in [ux-v3-1-uat](ux-v3-1-uat.md). V13 backend/import/migration safeguards below remain applicable; old stay-on-entry expectations are superseded by the approved v3.1 delta.

# V3 operational handover and independent UAT

Active specification: `../implementation/FitNutri_UX_Refinement_Execution_Plan_v3.md`. The v2 task ledger and final report are historical checkpoints. Current developer evidence is `ux-v3-results.md`, scenario evidence is `ux-v3-test-matrix.md`, and local commits are in `../implementation/ux-v3-tasks.md`. IN_REVIEW means implemented and developer verified; it is not a human approval.

## Actual isolated review environment

Open **http://localhost:15173** for V3. It runs actual React/TypeScript/Vite/MUI and forwards through the owned test transport on127.0.0.1:18082 to Spring on18080, backed by an independently created UUID PostgreSQL16 database. The prior V8 built app on8787 is a separate historical runtime; it has not been upgraded toV13. Do not use8787 to assess V3 or point its old binary at the new database. Public deployment was not requested.

Create an `example.test` synthetic USER through the real registration UI and complete onboarding. An existing synthetic ADMIN was explicitly provisioned for this isolated environment; its private credentials are never committed. Reviewers must obtain credentials through the local operator. No common/default admin password or automatic role promotion is provided. The current runtime has real reference catalogues and product options; live AI features are deliberately unconfigured. Disabled media/Q&A availability is truthful, not a model-quality acceptance result.

The matching backend requires approved V1–V13 history. An existing database must pass the read-only preflight and reviewed backup/restore procedure in `ux-v3-migration.md` before any explicitly approved migration. Startup never adopts an old database or auto-imports catalogues. Restore to a new database, then use its matching binary. An actual V8 binary on a disposableV13 clone failed its V8 readiness gate without changing any table.

## Reproducible local commands

From the project root, verify `git branch --show-current` is `develop/yuankang-wei`. Privately supply an explicitly selected **already migrated, owned V13** database in `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; do not copy another runtime's connection or print credentials. Package only after real tests using the commands below. The tested immutable backend launcher is:

```sh
SERVER_PORT=18080 APP_ALLOWED_ORIGINS=http://localhost:15173 \
  IMPORT_FOOD_CATALOGUE=false IMPORT_ACTIVITY_CATALOGUE=false \
  IMPORT_FOOD_SEARCH_METADATA=false IMPORT_EXERCISE_OPTIONS=false \
  APP_BOOTSTRAP_ADMIN_ENABLED=false sh scripts/dev.sh backend-built
```

Use free ports; do not stop an unidentified existing process. In a second terminal:

```sh
FITNUTRI_E2E_UPSTREAM=http://127.0.0.1:18080 FITNUTRI_E2E_FAULT_PORT=18082 \
  node frontend/tests/e2e/fault-proxy.mjs
```

In a third terminal, from `frontend/`:

```sh
FITNUTRI_E2E_FAULT_ORIGIN=http://127.0.0.1:18082 \
  npm run dev:e2e-fault -- --port 15173 --strictPort
```

This test-only proxy forwards actual API bytes and can discard one successful create acknowledgement. It is not production middleware. Run one browser worker/suite at a time because the one-shot fault control is shared by this owned test stack. Normal production frontend configuration still forwards to its configured same-origin backend; no provider fixture is inserted.

For a newly approved catalogue import, the pinned source bytes must first verify with the repository data preparation scripts. The actual owned runtime uses AUSNUT2023r1 and Compendium2024r2, plus reviewed food metadata/product-option/movement revisions. Select `ACTIVITY_MANIFEST=data/manifests/compendium-2024-r2.json` explicitly when importing exercise options; the old default r1 is not silently changed. Import flags default false and compare immutable source hashes. No import or migration should be enabled for an ordinary restart.

```sh
sh scripts/check.sh --stage static
sh scripts/check.sh --stage frontend
sh scripts/check.sh --stage data
# Supply explicit isolated TEST_DB_ADMIN_URL/USERNAME/PASSWORD/DOCKER_CONTAINER privately:
sh scripts/check.sh --stage backend
# Supply owned FITNUTRI_E2E_DB_NAME, TEST_DB_DOCKER_CONTAINER/USERNAME,
# synthetic FITNUTRI_E2E_ADMIN_EMAIL/PASSWORD, and optional installed browser path:
FITNUTRI_E2E_BASE_URL=http://localhost:15173 \
  npm --prefix frontend run test:e2e -- --config playwright.v3.config.ts
```

Set `FITNUTRI_PYTHON` to an existing environment with the repository requirements. Set `FITNUTRI_E2E_BROWSER_PATH` to an explicitly installed compatible browser executable if Playwright's managed browser is absent. Default checks never download browsers or invoke paid models. A zero-test/skip run is not PASS. Browser result JSON/screenshots are generated under `frontend/test-results/ux-v3`; a new run clears that directory, so preserve selected review evidence first.

## Checklist adapted from specification chapter22

Every checkbox below is **human UAT PENDING**. Developer evidence is linked separately, and no agent checks these boxes on behalf of a teammate or user.

- [ ] Register a synthetic USER; successful notice disappears within3seconds while errors remain; finish the three questionnaire steps on narrow and wide screens, including clickable multiline consent.
- [ ] Dashboard Add controls are clear and keyboard reachable; read-only metrics do not act as buttons. Graph follows metrics and precedes factual text/records, with units/axes and a data table.
- [ ] Choose an explicit date/default meal, find and select real food references, enter grams and Add two items locally. Existing meals survive a changed default; no save occurs yet. Unknown preparation is never assumed fat-free.
- [ ] Change grams/meal, remove/Undo, and resolve unadded composer input before Review. Reference replacement returns to its exact item. One final confirmation creates the reviewed batch; no per-field confirmation checkboxes remain.
- [ ] Successful confirmation returns to empty Add Food with a short notice and persistent View saved records. Past-date saves start a new current-date entry, with an explicit backfill shortcut. Recent reference reuse leaves grams blank until deliberate Use last.
- [ ] Create a named dish from chosen ingredients, distinguishing PERSONAL grams from FULL_RECIPE grams. Full recipe requires an explicit fraction/percentage/servings input;1/3 differs from33.3%. No invented recipe or nested dish reference.
- [ ] Save dish plus single food as two visible roots; expand original recipe input and consumed grams. Reload/login preserves saved groups. Correct/delete only that occurrence atomically; original creation proof and other occurrences remain unchanged.
- [ ] Log again creates fresh identities, blank grams/portion/time/details. Explicit Use last fills only the intended blank fields. Reusing an inactive reference requires a valid current selection.
- [ ] Quick exercise accepts sports such as Basketball without Gym. Select a real directory activity/movement and minutes; no intensity requirement. Optional equipment fields stay optional, zero remains zero, unavailable energy remains null.
- [ ] Gym records one50minute strength period with several movement details plus a separate20minute cardio period, total70minutes. Sets/reps/load never duplicate duration/energy. Show units/load basis and distinguish unavailable estimate from service failure.
- [ ] One Gym confirmation atomically saves one Workout root. New Gym duration is blank. Detail-only correction preserves stored energy/weight basis; changed date/time recalculates only affected periods. Whole-root deletion removes all active periods from summaries.
- [ ] Two sessions editing one version yield one success and one409. Rejected input/version remain until explicit latest-state replacement; edits must not erase the conflict or silently advance the version.
- [ ] Current-tab unknown save retains original body/UUID for receipt GET or exact retry. A404 does not prove rollback. Hard refresh forgets draft and submission ID, never resends automatically; manually inspect History before recording again.
- [ ] USER cannot manage accounts. Another USER and ADMIN cannot read/change private dishes/Workouts/proofs. Missing CSRF is refused; legacy child edits cannot bypass a grouped parent version.
- [ ] Existing V1 standalone creation/read/correction/replay, Profile units/goals/dated weights, logout/login, and administrator disable/reactivate remain real and preserve private records.
- [ ] With properly provisioned live services, test image candidates, real microphone transcription/extraction and Q&A scope/refusal. Every candidate still requires reference review/final confirm. **BLOCKED_EXTERNAL** until private provider/microphone conditions are available; controlled-provider tests do not check this box.
- [ ] Repeat ordinary flows on actual Chrome, Edge and Safari and record exact versions. Automated Chrome, native Safari availability and any unavailable browser are separate; Playwright WebKit is not Safari human UAT.

## Requirement provenance

The teacher-facing baseline retains the assigned stack, registration/session/role controls, nutrition/exercise records, PostgreSQL persistence and developer testing with meaningful Arrange–Act–Assert assertions. V3 user requirements add the eight UI refinements, reviewed search/reuse, exact recipe portions, Quick/Gym grouping, shared final review and deliberate memory-only refresh behavior. Port choices, disposable UUID test databases, immutable artifact staging, specific test fixtures and the local one-shot proxy are engineering defaults/evidence infrastructure, not new teacher requirements. Automated passes, peer review, UAT and live-provider acceptance are reported separately in the final handover.


## Observed active API and catalogue boundary

The executable contract is [openapi-mvp.yaml](../api/openapi-mvp.yaml); TypeScript/Zod are generated and checked without manual drift fixes. Actual current features use:

| Operation | Path |
|---|---|
| Reviewed food query | GET /api/food-reference/smart-search |
| Product activity/movement directory | GET /api/v2/exercise/options; GET /api/v2/exercise/movements |
| Food preview / final batch | POST /api/v2/food/preview; POST /api/v2/food/batches |
| Food root list / recent | GET /api/v2/food/entries; GET /api/v2/food/recent |
| Owned food read / correction / whole deletion | GET/PUT/DELETE /api/v2/food/entries/{single-or-dish}/{id}; POST its /preview |
| Exercise preview / final occurrence | POST /api/v2/exercise/preview; POST /api/v2/exercise/entries |
| Exercise root list / recent | GET /api/v2/exercise/entries; GET /api/v2/exercise/recent |
| Owned exercise read / correction / whole deletion | GET/PUT/DELETE /api/v2/exercise/entries/{quick-or-workout}/{id}; POST its /preview |
| Original minimal creation proof | GET /api/v2/submissions/{submissionId} |
| Runtime availability | GET /api/capabilities |

Existing V1 /api/records, /api/submissions, Session/CSRF, Profile/weight, summaries, media/Q&A and admin endpoints remain compatible under their existing role/owner rules. Group children refuse V1 independent writes. All mutations require current authority; readonly previews do not create a record. Admin account management never grants access to private USER roots.

Actual authenticated capability observation: schemaVersion=mvp-1, zone=Australia/Sydney, verified/ready food and exercise catalogues; image/transcription/speechParsing/qa all configured=false and available=false. Read-only runtime/source preflight returned PASS, zero issues and migrationApplied=false. Full public values are preserved in [sanitized runtime inventory](ux-v3-runtime-inventory.json). Active metadata is ux-food-r1 (52reviewed profiles,201aliases), ux-exercise-r1 (38options), ux-movements-r1 (36movements). Verified AUSNUT2023r1 contains3741references; Compendium2024ux-r2 contains26reviewed activity mappings. The latter is the curated verified mapping set, not a claim to publish every activity in the official Compendium;41total activity rows also include older retained-source rows. No official values were invented or replaced by UX labels.
