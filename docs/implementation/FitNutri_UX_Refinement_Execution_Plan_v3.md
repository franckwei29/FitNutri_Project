# FitNutri: Food, Exercise and Frontend Experience Refinement Execution Plan

> English handover edition (2026-10-18): this historical specification retains its original baseline, task statuses and instructions as evidence. It does not grant new execution authority or certify the current implementation. Current submission prose is English; functional multilingual inputs are retained. For installation use [the fresh-start guide](../getting-started.md), and for this handover use [the validation report](../verification/team-handover-validation.md).

**Version: 3.0 FINAL｜Requirements frozen: 2026-10-17｜Code baseline: the supplied `Lab_practical_05_Group_6.zip`｜Execution status: this round of refinement has not yet started.**

**The only local Git branch authorised for modification is `develop/yuankang-wei`.**

This document is the **complete, single-file execution specification** for this round, rather than a prompt that patches an earlier plan. The coding AI needs this document and the user's actual project working tree; it does not need an earlier startup prompt or another implementation plan. This document incorporates the eight categories of self-test findings, the food interactions confirmed in the conversation, composed dishes and portion sharing, Add Exercise choices E1-C/E2-B/E3-B/E4-B/E5-A/E6-A/E7-A/E8-A, and the existing branch, code, database, testing and commit rules.

**This does not restart the Demo integration. The current ZIP already contains a real backend, sessions, transactions, idempotency, history corrections and extensive tests. Reuse existing work before extending it incrementally. Never overwrite the current project with an old `main*.zip` or Demo.**

## Read before execution

1. Complete the Branch Gate in Chapter 1 first. Until it passes, perform read-only analysis only.
2. Execute UX-P00–UX-P03 to establish the local code baseline, existing regressions, interfaces and migration approach.
3. Implement according to the task dependencies in Chapter 20. Write tests, run them, self-review and create a local commit for each business change.
4. Label every backend/database change with a UX-BC identifier and explain its rationale, compatibility and verification.
5. Frontend confirmation state and drafts must not survive refresh. Retain existing backend submission receipts; do not remove idempotency support.
6. Deliver an item only after its actual implementation, applicable tests and integration checks pass. Report code completion, self-testing, peer review and UAT separately.

## Navigation

|Chapters|Contents|
|---|---|
|0–3|Baseline, rules, requirements freeze and conflict resolution|
|4–8|Complete user journeys, pages, search, recent items and composed dishes|
|9–11|Exercise, draft state machine, calculation and precision|
|12–16|Backend changes, database, interfaces, services, correction and recovery|
|17–19|Statistics/AI integration, experience quality, migration and rollout|
|20–23|Task cards, test matrix, acceptance delivery and AI startup instructions|
|Appendices|Source-code evidence, existing commands, course/official references and requirements traceability|

---

# 0. Input baseline and trust boundaries

## 0.1 Sole source-code baseline for this round

|Item|Finding from this inspection|
|---|---|
|Latest code ZIP|`Lab_practical_05_Group_6.zip`, not the earlier `Lab_practical_05_Group_6-main*.zip`|
|ZIP SHA-256|`a5f9a7dfd038a545f22d0d5225673970a309620502639fe8bdf6c107b5a7498d`|
|Archived HEAD|`.git/HEAD` points to `refs/heads/develop/yuankang-wei`|
|Archived branch commit|`ae264208ac597f1de81220b502a8213b574dadd4`; this represents only the local metadata at packaging time|
|Backend main sources|171 Java files; layered services already exist, replacing the original Controller prototype|
|Frontend main sources|118 TS/TSX files; routing, real APIs, drafts, history and advanced input already exist|
|Backend test directory|97 Java files, including test classes and support classes; this is not a claim that 97 test cases passed|
|Food catalogue|3,741 entries in the `foods` array of the packaged `ausnut-2023-foods-r1.json`|
|Exercise catalogue|15 entries in the `activities` array of the packaged `compendium-2024-activities-r1.json`|
|Existing migrations|V1–V8; use the next number only after checking again during execution|
|Issue list|`自测问题记录_1017.md` (original user self-test findings for this round)|

The archive contains node_modules, target, `.local`, `.git` and macOS metadata. **These do not constitute an environment that necessarily runs on another operating system, nor do they imply that data or configuration inside `.local` may be committed.** This review read source/configuration/catalogue files as needed; it did not run the user's database, call a real model or switch the user's working branch.

The self-test Markdown's screenshot links point to the user's local Typora directory. No actual images were embedded with that Markdown. References to the “red area” were located using the user's text and current implementations such as `FoodReferencePicker`/`SourceNote`. Do not claim to have inspected pixels from screenshots that were not provided.

## 0.2 Existing capabilities: preserve rather than rebuild

- `FoodRecordService.confirmBatch()` already uses an outer `SubmissionService` transaction, a user lock, real references and server-side nutrition calculation.
- `ExerciseEstimateService` already finds the latest weight on or before the activity date and includes an unestimable state and calculation fingerprint.
- `SubmissionService`, `CanonicalSubmissionHasher` and `submission_receipt` already support duplicate requests, original receipts and database protection.
- `RecordMutationGuard` and record-modification services already enforce versions, soft deletion and ownership checks.
- React already has `DraftProvider`, request freezing, owner/epoch isolation against account mixing, `UnsavedChangesGuard`, and save/correction recovery.
- `SummaryRepository` already uses PostgreSQL to group and sum confirmed leaf records while preserving missing-value semantics.
- Images, speech and Q&A already have real adapters, permissions and automated tests. The new UI must not regress them to fixed mock responses.
- `scripts/check.sh`, an OpenAPI→TS/Zod generator, isolated PostgreSQL tests and browser cases already exist.

These findings come from **static code inspection for this round**. Historical PASS entries in the repository's verification documents are packaged evidence. The executing AI must rerun affected regressions and must not describe those entries as tests executed in this round.

## 0.3 Specification priority and scope

Priority: choices explicitly confirmed by the user in this round > implementation contracts in this document > compatibility with current code > historical design suggestions.

New requirements explicitly supersede these parts of the earlier plan: per-item reference/grams checkboxes; automatic navigation to Dashboard after saving; browser-persisted drafts/pending submission identities; single-exercise-only entry; and flattening composed dishes after saving. Preserve all other identity, permission, provenance, date, reliability and AI boundaries not explicitly changed.

This document separately labels **user decisions** and **engineering defaults**. Engineering defaults are implementation choices frozen here to remove coding ambiguity, not claims that the user individually selected every technical parameter. Stop to ask only for hard blockers such as a database conflict that cannot be resolved without loss, missing authorisation or invalid source evidence. Do not repeatedly ask A/B questions about an already-confirmed experience.

---

# 1. Mandatory rules before development

## 1.1 Branch Gate

Before any project write, dependency installation, test-file creation or database migration, execute these commands in the actual working directory:

```bash
pwd
git rev-parse --is-inside-work-tree
git rev-parse --show-toplevel
git status --short --branch
git branch --show-current
git branch --list
git worktree list
git rev-parse HEAD
```

Only `develop/yuankang-wei` is permitted. Archived screenshots/metadata are not runtime evidence. If another branch is active, run `git switch develop/yuankang-wei` only when **the target already exists, the local working tree is clean, and switching cannot affect existing content**, then verify again. If the branch does not exist, metadata is missing, switching is risky or another worktree occupies the target, mark `BLOCKED_BRANCH`; do not independently init/clone/create/stash/reset/clean.

Do not develop on main, master, shared develop, another person's branch or detached HEAD. Do not create a substitute feature/integration branch. Different agents must not write the same working tree simultaneously. Recheck the branch before starting writes in each work package, before every commit and when resuming an interrupted session.

For this round, **do not access the university GitHub, fetch/pull/clone/push, or create/merge a remote PR**. Downloading permitted dependencies or official reference sources is distinct from remote Git operations. Use the existing source-locking scripts and do not touch unknown shared systems.

Startup acknowledgement:

```text
Project root:
Required branch: develop/yuankang-wei
Current branch:
HEAD / BASE_COMMIT:
Pre-existing changes:
Latest ZIP comparison:
Branch Gate: PASS / BLOCKED_BRANCH
```

Protect existing user diffs. If the designated branch contains code beyond the ZIP, compare it first and reuse equivalent implementations; never reset it to the ZIP. Do not count the user's earlier changes as the AI's contributions in this round.

## 1.2 Technology stack and code conventions

Follow the existing lockfiles/effective POM: Java 21, Spring Boot 4.1.1, Spring Security Session/CSRF, Spring Data JPA, Flyway and PostgreSQL 16; React 19, TypeScript 6, Vite 8, MUI 9/Emotion, React Router 7, Axios, TanStack Query 5, Recharts 3 and the existing Vitest/Playwright versions.

These versions are declared in the ZIP and have not been reinstalled and verified during this review. Do not upgrade across major versions, switch to JWT, add a Node backend or replace the database/UI framework. Reuse existing hand-written React forms; do not convert everything to RHF merely for stylistic consistency.

- Use strict TypeScript types. Do not bypass unknown responses with `any`; decode requests/responses with Zod.
- `contracts.ts` and `schemas.ts` are generated: edit `docs/api/openapi-mvp.yaml` first, then run the generator. Never hand-edit generated output.
- That OpenAPI file currently contains JSON text, read with `json.loads()` by the scripts. Do not convert it to indented YAML merely because its extension is yaml.
- The current generator supports `anyOf`/`const`, but not `oneOf`/discriminator without extension. Use `anyOf` with mutually exclusive `kind` constants, or add generator tests before extending support.
- Preserve the Java package root `com.fitnutri.backend`: thin Controllers, Services orchestrating transactions, Repositories executing queries, and constructor injection.
- Use server-side BigDecimal, LocalDate for business dates, Instant for audit times and injectable Clock. Do not mix kJ/kcal in storage.
- Continue using concise AU English for UI strings. Chinese is for search aliases and user-defined dish names; do not add a site-wide bilingual toggle.
- Do not commit secrets, real accounts, health answers, media or database dumps. Do not echo private `.env` or `.local` contents.

## 1.3 Tests and self-review alongside every business task

Continue following the instructor's Lab9 principles of JUnit/Mockito, MockMvc, AAA, shift-left and layered testing: **write tests alongside features; the author self-reviews before handing work to another reviewer.** Do not use the final QA stage as a reason to postpone unit tests.

For each task: expected assertions → test/regression cases → implementation → relevant test execution → fixes → diff/permission/migration review → branch recheck → local commit.

Use project-compatible JUnit Jupiter/Mockito for pure Java calculations, MockMvc for Controllers, isolated PG16 for transactions/locks/unique constraints/migrations, Vitest/Testing Library/contract tests for the frontend, and real frontend+Java+PG for E2E. Do not downgrade Boot to copy old course annotations; retain working repository imports such as `tools.jackson`.

Require business assertions, not merely `contextLoads()`, notNull or HTTP 200. Mock dependencies, not the core logic being tested. Derive expected values independently. Zero tests, unexecuted/skipped tests, missing databases and failed dependency downloads must never be reported as PASS. The course slides' mention of 95% is not an explicit grading threshold in this material. Do not inflate coverage by deleting tests or excluding code.

## 1.4 Git / commits / documentation

Format: `type(scope): concrete change [UX-ID]`. Normally, include the business change and its directly corresponding tests in one reviewable commit. Preparatory migrations/contracts may be committed separately, but a business task without tests cannot be marked complete.

```text
feat(food): add reviewed ingredient dish confirmation [UX-B06]
fix(auth-ui): dismiss registration success within three seconds [UX-F00]
test(workout): prove strength durations are counted once [UX-Q03]

Why:
What:
Backend-Change: UX-BCxx / none
Database-Change: migration ID / none
Contract:
Tests: actual commands, counts, result, environment
Limitations:
```

Run `git diff --check` before committing, explicitly stage task files and inspect the staged diff; do not blindly run `git add .`. Do not falsify authorship or automatically push/merge. Preserve history in `docs/implementation/tasks.md` and add a UX task section. Append UX-BC entries to `docs/implementation/backend-change-log.md`; do not overwrite BC-01–16.

Update the current-specification pointer in `docs/development.md` to this document. Keep the old v2.0 file as history and mark conflicting rules as superseded by this document for this round; execution does not require loading that old file. Maintain one active specification rather than duplicate rules.

## 1.5 Data safety and completion states

Verify migrations only in explicitly isolated test databases. Do not use existing development data as disposable test data. Never clear user volumes with `down -v`, drop shared databases, modify applied V1–V8 migrations or bypass validation with Flyway clean/repair. Migrations in higher environments require separate explicit authorisation.

Use these states consistently: `TODO / IN_PROGRESS / BLOCKED_BRANCH / BLOCKED_DATA / BLOCKED_ENV / IMPLEMENTED_NOT_VERIFIED / IN_REVIEW / ACCEPTED`. Record developer tests, real integration, peer review and UAT separately. AI may report actual self-tests but must not sign on behalf of team members or the user.

---

# 2. Requirements freeze register

## 2.1 Self-test findings U01–U08 (from the original Markdown)

|ID|Final requirement|Current source entry point|
|---|---|---|
|U01|The green registration-success notice must finish fading and collapsing within ≤3 seconds of appearing, without leaving blank space|`pages/AuthPage.tsx`|
|U02|Align the Allow checkbox with the first line of adjacent text; wrap neatly and allow clicking the text to toggle it|`features/profile/ProfileSections.tsx`|
|U03|Clearly style primary clickable actions on the home page and distinguish secondary actions; read-only metrics must not resemble buttons; retain the existing green style|`DashboardPage.tsx`, `App.css`, `theme.ts`|
|U04|Hide F identifiers/data versions/match scores in food search; names and key raw/cooked/preparation distinctions must be understandable|`FoodReferencePicker.tsx`, food-search backend|
|U05|Remove per-item Confirm reference/grams checkboxes; retain the final unified Review and save|`FoodItemEditor.tsx`, `draftValidation.ts`, `draftReducer.ts`|
|U06|After selection, remove the duplicate Selected and source-explanation areas; retain only a compact name, necessary distinctions, inputs and nutrition|Call sites of `FoodReferencePicker.tsx` and `SourceNote.tsx`|
|U07|Cover common exercise equipment/ball sports; hide MET, codes, versions and long explanations; emphasise activity, minutes and estimated expenditure|`ExerciseEditor.tsx`, Review, details and catalogues|
|U08|Place Dashboard charts below metrics; place History charts below filters/statistics and before records; enlarge charts and add dates, vertical axes and units|`IntakeChart.tsx`, `PeriodTrends.tsx`, both pages|

The original self-test notes repeat number 5. This document normalises numbering by requirement meaning without omitting items. Users can still verify provenance by explicitly expanding `Data source` details; do not expose it repeatedly in the main flow.

## 2.2 Food F01–F24 (confirmed by the user)

|ID|Frozen behaviour|
|---|---|
|F01|Option A: match first and ask only when needed; keep the complete valid AUSNUT catalogue accessible|
|F02|Ask only about key attributes that remain unclear and that users can identify; do not repeat conditions already entered|
|F03|If preparation is unknown, offer approximate suggestions explaining their differences; require explicit selection and never preselect|
|F04|Match a specific real reference before calculating by grams; aliases/common categories are not new nutrition sources|
|F05|Use grams for portions; do not add automatic bowl/cup/item conversion|
|F06|Use `Add to meal` one item at a time; add only to an in-memory draft without a write API call; keep search available|
|F07|The user selects a default meal once, reused subsequently; each top-level item can override it; do not infer meals from the clock|
|F08|Edit grams and meal inline in My meal; replacing a reference returns to search and clearly identifies the target|
|F09|Draft removal takes effect immediately with Undo; historical deletion requires separate confirmation|
|F10|Edit grams/meal inline in Review; return to search only for reference replacement; provide only one final primary save button|
|F11|After successful saving return to the Add food editor, not Dashboard or a success page; clear the saved batch draft|
|F12|Fade the brief success notice within ≤3 seconds; provide a separate, continuing View saved records entry point|
|F13|After saving today, retain date/default meal; after backdating, return to today with one-click continuation of the previous historical date|
|F14|Recent items reuse the same specific reference with grams blank; Use last X g requires an explicit click|
|F15|Recent items come from this user's confirmed, undeleted records; date/meal use the current entry context|
|F16|When no whole-dish match exists, offer Record by ingredients; users choose real ingredients and grams; do not invent recipes|
|F17|Keep a composed dish as one dish in My meal, Review and History, expandable to its ingredients|
|F18|Default composed dishes to personal-ingredient mode; allow a whole recipe plus the fraction eaten this time; see register D|
|F19|History corrections affect only that historical record, not other same-name dishes or dates|
|F20|Warn before leaving the entry flow; Add food↔Review and reference replacement belong to the same flow and do not clear it|
|F21|Do not recover drafts after hard refresh/close; do not store drafts in localStorage/sessionStorage/IndexedDB|
|F22|After accidental refresh during saving, do not retain/recover submission state/identifiers; users check History themselves|
|F23|While the page remains mounted, freeze the original request and allow safe status checks/retries; retain backend idempotency and minimal receipts|
|F24|Retain existing manual, speech and image entry; align new interactions with their drafts/Review; never automatically save recognised results|

Earlier values such as “first 5”, “20 per additional page” and “at most 5 recent items” were suggested parameters. This version identifies them as engineering defaults, not individually voted user decisions.

## 2.3 Composed dishes D01–D14

|ID|Frozen behaviour|
|---|---|
|D01|Users name composed dishes; do not present them as official single foods. Label them My dish regardless of who cooked them; do not force Homemade|
|D02|Personal mode records grams of each ingredient personally consumed; the fraction is always 1|
|D03|Whole-recipe mode records grams added to the whole recipe/in the appropriate ingredient state, then the fraction eaten this time|
|D04|Offer fraction shortcuts ¼, ⅓, ½, all, and custom percentage; expose serving-count entry as an expandable option|
|D05|Convert total servings / servings eaten into a fraction; do not multiply by a percentage again|
|D06|Do not represent total raw-ingredient grams as finished cooked-dish weight; do not add portion sharing by finished weight|
|D07|Briefly explain estimation limitations such as uneven distribution and cooking losses; do not claim precise individual measurement|
|D08|Use one meal and date per dish; ingredients cannot have separate meals; review dishes and ordinary foods in the same batch|
|D09|Each ingredient has its own reference and grams; calculate only this intake; parent totals are for display and are not recorded again|
|D10|Log again copies into a new draft without changing the original; personal-mode grams are blank and Use last amounts explicitly restores them|
|D11|Whole-recipe Log again initially shows the previous recipe; copy grams only after Use last recipe; leave the current fraction/servings blank|
|D12|History can change this intake's fraction, whole recipe, ingredients and grams; save a snapshot for this occurrence without changing other records|
|D13|Require confirmation before deleting a historical dish; soft-delete the parent and all ingredient leaves in one transaction|
|D14|Keep old flat records independent; do not infer dishes by meal/name/original batch; do not add a standalone My Recipes system|

## 2.4 Exercise E1–E8: final choices, do not ask again

|Choice|Frozen meaning|
|---|---|
|E1-C|Two entry points: Quick exercise for a single activity and Gym workout for multiple segments|
|E2-B|Strength movements/equipment may optionally include weight, sets and repetitions; omission must not prevent recording|
|E3-B|Cardio duration is required; speed, incline, distance and equipment level are optional as appropriate to the equipment|
|E4-B|Enter total duration once for a strength segment; movements are details and do not each add duration; cardio uses separate segments|
|E5-A|Do not add intensity fields/questions; use approved references for general estimates and do not require users to assess MET|
|E6-A|Recent/Log again reuses a specific option or workout structure; duration is blank and Use last duration explicitly restores it|
|E7-A|Save after unified Review and return to Add Exercise to continue or view records; do not automatically navigate to Dashboard|
|E8-A|Show a Workout as one History card expandable into segments/movements; allow whole/item corrections and Log again, affecting only that occurrence|

The user also explicitly requires searchable common cardio equipment, strength machines and ball sports. Merely adding one generic Strength Training option is insufficient. Equipment names and energy-expenditure references are different concepts; see Chapter 9.

## 2.5 Frozen engineering defaults (not new product commitments)

- Search debounce: 250ms; at least 2 English characters, or 1 valid Chinese Han character; maximum 80 characters. Default 5 candidates; expanded results 20/page.
- Recent ordinary foods: 5 distinct referenceIds; recent composed dishes/Workouts: 5 historical snapshot entries each. Do not merge different recipes merely because their names match.
- Each food batch: at most 20 top-level items and 50 ingredient leaves total; each dish: at most 20 ingredients. Retain the existing API's 50-leaf safety limit.
- Each Workout: at most 12 non-overlapping segments, total minutes ≤1440; each strength segment: at most 30 movements; total movements ≤100.
- User-entered ingredient grams: >0 and ≤10000g, at most 3 decimal places, retaining existing rules. Derived personal grams may require greater precision; do not truncate early.
- Minutes: >0 and ≤1440, at most 2 decimals; total servings: integer 1–1000; servings eaten: >0 and ≤total servings, at most 3 decimals; percentage: >0 and ≤100, at most 3 decimals.
- Show success notices for about 2500ms, followed by 300ms fading/collapse, total ≤3000ms. Errors do not disappear automatically. Undo is available for 8 seconds, with a recovery entry retained in the input area; close the undo window before Review.
- Names: 1–100 characters. New custom text is limited to dish/workout names; users must not invent official references or calories.
- Exercise dates reuse Food's rule C; retain the default mode only in current-tab memory, not across refreshes; Quick is the default for a fresh page.
- Only one active food or exercise draft at a time; switching types requires explicit abandonment rather than maintaining multiple hidden drafts.

---

# 3. Logical conflicts and resolution rules for this version

**E1–E8 contain no irreconcilable conflicts; apply the following model/state rules to remove implementation ambiguity.**

|Risk|Frozen resolution|
|---|---|
|Detailed strength movements, but E4 permits only one duration|One Strength segment produces one energy leaf; movements create no additional exercise_record or accumulated duration|
|Optional speed/incline while E5 avoids intensity questions|Store parameters as training details only; do not use them to select another MET, interpolate or change formulas this round. Change the reference only when the explicit activity option changes|
|A machine has no dedicated official value|Still allow recording. Estimate only through approved general/specific mappings; otherwise return NO_APPLICABLE_REFERENCE, never invented values or 0|
|Quick and Workout coexist|Share ActivityPeriodEditor for single and multiple segments; Quick need not pass through Workout; keep their persisted identities distinct|
|Whole-recipe grams versus grams personally eaten|Make inputMode explicit; retain the original recipe, precise fraction and personal leaf snapshot; do not overwrite whole-recipe grams with allocated amounts|
|Draft recovery removed but duplicate protection still required|Browser state is memory-only; server receipts and uniqueness remain permanent. Refresh creates a new user intent/ID and duplicate intent cannot reliably be detected; explicitly accept this boundary|
|Confirm checkboxes removed but valid input still required|Remove duplicate confirmation gates from UI/reducer; valid specific reference, grams/duration, current preview and final save remain required|
|No intensity requirement, but reference describes specific intensity/speed|Do not present it as user-measured. Choose evidence-backed general mappings labelled as general estimates; never silently apply unsupported extra conditions|
|Dashboard's previous count===list.length after grouping|Old count measures leaves; add user-entry counts and compare matching units in the frontend, avoiding false inconsistencies/double counting|
|Source disabled when correcting a record|The original immutable reference may be reused for that correction; new entries/Log again require current availability. Never update history to follow a new catalogue|
|Log again with unknown save outcome|Confirm replacement of an unsaved draft; saving/unknown cannot create another record unless the user explicitly abandons session recovery and checks History themselves|
|Unbounded “all common equipment” scope|Chapter 9 lists mandatory options for this version; report missing energy mappings separately from missing catalogue entries, without hiding omissions behind generic training|

**Explicitly accepted risk:** The user chose not to retain submission identity across refreshes. Even if the server committed the first request, manual re-entry after refresh is a new intent/ID and cannot safely be recognised as a duplicate. Do not restore sessionStorage drafts to address this risk, or reject every legitimate second record merely because it has the same date/content.

---

# 4. Final user journeys and navigation contract

## 4.1 Ordinary food

```text
Dashboard/Add → Add food
  → Browse recent items or search an everyday name
  → Select a specific reference (choose attributes or explicitly accept an approximation if needed)
  → Enter grams; reuse the user's chosen default meal
  → Show current server nutrition preview
  → Add to meal (memory only)
  → Clear/refocus search and continue adding
  → Edit grams/meal, remove/Undo inline in My meal
  → Review (grouped by meal and top-level entries)
  → Correct inline if needed; one Confirm & save
  → Atomic server confirmation and receipt
  → Return to Add food with a new empty draft; success notice and View saved records
```

If the selected food is still being edited and has not been added, show `Add or discard the food you are editing first.` beside Review. Do not automatically add an incomplete item or silently discard input on entering Review. A search term alone, without selection/quantity, is not a business draft; actual entered content receives leave protection.

## 4.2 Recent ordinary food

Clicking a recent item explicitly selects that specific reference; do not ask again about known preparation. Grams remain blank; only clicking `Use last 180 g` fills the quantity. Date/meal come from the current context, not the old record. Request a fresh server preview rather than reusing historical nutrition totals.

## 4.3 Composed dishes

No suitable whole-dish result → Record by ingredients → enter dish name (optionally prefilled from the search term and editable) → default personal mode → select real ingredients and enter grams individually → add to the dish → Done/Add dish to meal.

Selecting Full recipe changes the grams label to `Used in the full recipe (g)` and adds `How much did you eat?`. Leave fraction/servings blank; adding or saving requires a fraction. Ingredients still use references for their raw/cooked state; do not invent cooking conversions.

Once added to My meal, a dish defaults to a collapsed summary of name, ingredient count, current portion and nutrition; expand to edit ingredients. Dishes and ordinary foods share one Review and batch confirmation.

## 4.4 Quick exercise

```text
Add Exercise → Quick exercise (new-session default)
  → Select a real option from categories/search/recent items
  → Date and duration
  → Optionally expand equipment-specific training details (all may be blank)
  → General expenditure estimate or brief reason estimation is unavailable
  → Review exercise (editable date, duration and details)
  → Confirm & save
  → Remain in Add Exercise's Quick editor, clear duration/selection, show View saved records
```

Quick can independently record strength equipment: the whole Quick entry is one strength period, with equipment as one movement detail. Do not force users into Gym. Ordinary Quick entries with an activity name do not require movement details.

## 4.5 Gym workout

```text
Add Exercise → Gym workout
  → Date and optional workout name (default “Gym workout”)
  → Add strength period: enter total minutes for this strength period once
      → Add movements such as bench press/chest press/chest fly; weight/sets/reps optional
  → Add activity period: 20min incline walking, optional speed/incline
  → Total duration = sum of distinct periods; do not sum movement energy again
  → Review workout: one card with expandable/editable periods and movements
  → One confirmation, one transaction, parent group + period snapshots + movement details
  → Return to empty Add Exercise Gym editor; continue or view History
```

A Workout need not contain strength and may contain only multiple cardio activities. Strength without movement details is valid; empty movement data does not invalidate a period. At least one valid period is required; a name or empty card alone is insufficient.

## 4.6 History corrections and Log again

- `Edit`: load the saved snapshot and expectedVersion; use Correction Review to update the original root. Return to the original History range and highlight it; the new-entry “remain in Add” rule does not apply.
- `Log again`: read the old snapshot only to create a new in-memory draft; generate new root/clientEntry/segment UUIDs. Use the current entry date context, otherwise today; require meal selection if not already chosen.
- Personal dish: copy ingredient references, leave amounts blank; Use last amounts can fill them.
- Whole-recipe dish: show the previous recipe read-only; Use last recipe copies grams; the current fraction/servings always remain blank.
- Quick: copy type, leave duration blank; Use last duration can fill it.
- Workout: copy periods/movement names, leaving all period durations blank; per-period Use last duration or collective Use last durations requires explicit clicks. Also leave weights/sets/reps/equipment parameters blank and offer Use last details; never silently imply identical training this time.
- Do not add a favourites/template library, mutable recipes shared across occurrences, training schedules or automatically recommended prescriptions.

## 4.7 Navigation: reuse routes and distinguish pages from API versions

Retain `/add`, `/entry`, `/review`, `/history`. `/add?kind=food|exercise` remains entry-method selection; the actual editor is `/entry?kind=food|exercise`, titled Add food/Add exercise. kind is only an initial routing hint; do not trust the URL for identity/existing drafts.

|Action|Destination|Draft rule|
|---|---|---|
|Add food↔Review|`/entry`↔`/review`|Same flowId, retain everything, no leave dialog|
|Change reference|`/entry?...`, addressed by in-memory selectionTarget|Return to originating row/Review after selection; do not create an extra item|
|New confirmation succeeds|`/entry?kind=...`, replace current Review history entry|Create empty draft, retain new-entry defaults, do not remain on savedReview|
|View saved records|`/history?from=D&to=D&type=...&highlight=kind:id`|Show the specific date only; confirm leaving any new draft first|
|Historical correction succeeds|Original History URL|Retain filters, highlight root and refetch|
|Switch food/exercise|`/add`/new kind|Nonempty draft requires Discard confirmation; pending first warns of unknown outcome|
|Hard refresh|Reload application|No draft/submission recovery; server Cookie may restore session|

Do not merely navigate to `/add` after Review saving: that returns users to manual/speech/image selection, contrary to continued entry. Default to the empty manual editor after success; if the previous method was image/speech, retain method-switch access without automatically starting microphone/image requests.

## 4.8 Date context after successful creation

Capture `savedRecordDate` and the user's `defaultMealType` on confirmation. Compare against the current business day from `useBusinessToday`, not the old date at entry.

- savedDate==today: the new empty FoodDraft retains today/defaultMeal.
- savedDate<today: the new empty draft uses today; retain defaultMeal with a prominent header, plus an in-memory `Continue recording for <savedDate>` shortcut.
- The shortcut restores only date/original default meal, not saved foods or submission. With new input, confirm context replacement rather than silently changing the batch date.
- Exercise follows the same rule without meal; retain Quick/Gym mode, not duration.
- Across midnight, do not shift an existing draft's date automatically; offer Today. Defaults after the next successful batch use the actual current day.
- lastSaved retains only committed identity/date/type in memory, never across refreshes, and does not contribute to statistics.

---

# 5. Page and component behaviour

## 5.1 Add food layout

Use a main workspace plus visible My meal on desktop and vertical stacking on narrow screens. Retain existing theme/CSS; do not add a second visual system.

```text
Add food                         Record date [D]   Default meal [Lunch]
Search food [                       ]
Recently used / Suggested matches / View all matching foods

Selected compact editor: name · key preparation label
Grams [ ]   Meal [current]       Nutrition preview
[Change food]                    [Add to meal]

My meal (2 items)   [single row] [dish summary + expand]
                            [Review food entries (2)]
```

Search results have only two levels: understandable name and essential distinctions. Do not show `Fxxxxx`, reference IDs, release/revision or model confidence in result rows, selected cards or main Review content. Remove duplicate `Selected: ...` and default `SourceNote` blocks.

Show readable **reviewed distinctions** such as raw/cooked and added oil for ordinary foods and ingredients; do not hide meaningful information. Fall back to the original name where no reviewed short name exists, wrapping readably; the browser must not arbitrarily truncate it into a different food.

## 5.2 Selection, quantity and preview

Collapse candidates after selection, retaining Change food. Default grams to blank and focus that field; previous quantity is a secondary action. Enter operates only when a candidate/button is explicitly focused; entering grams inside a <form> must not submit the whole batch.

The preview key includes owner+epoch+immutable referenceId+normalised grams+catalogueRevision. New input immediately invalidates the previous preview; show Loading/Updating instead of leaving Add enabled. Responses must match the request signature; late responses cannot overwrite the current item. Use `AbortSignal` to reduce wasted requests but still check signatures.

Server errors remain visible/retryable, not “no nutrition” or 0. Meal changes require no nutrition recalculation; grams/reference changes do. Allow empty strings and intermediate decimal input rather than forcing each keystroke into numeric 0.

## 5.3 Inline My meal editing

- One row per top-level item. Ordinary foods allow direct grams/meal editing; one draft is authoritative rather than separate List/Review copies.
- Mark that row's preview stale immediately after input changes; update it and totals after server confirmation without clearing other rows.
- `Done` closes editing only, without database writes. `Review` requires valid added items and current previews.
- Change food retains selectionTarget `{flowId,rootClientId,ingredientClientId?,returnView,restoreFocus}`; preserve quantity/meal and replace only upon selecting a new reference. Cancel search restores original reference/input without partial replacement.
- Preserve the number when changing raw/cooked state, show the new state and require a fresh preview; do not automatically convert cooked/raw weights.
- Allow duplicate references as separate draft items with distinct clientEntryIds; do not merge by name/grams.
- Remove stores an undo buffer with parent relationships, original index and UUID. Undo restores identity rather than generating IDs; undoing whole-dish removal restores the full subtree.
- Removing the last item may leave My meal empty and Review disabled. Empty dishes cannot save; add ingredients or remove them.

## 5.4 Review page

Show record date and top-level count at the top, grouping by nonempty meal categories. Each group contains ordinary rows or dish cards. Expanded dishes show original recipe, current fraction and personal grams; do not label whole-recipe and personal nutrition ambiguously as Total.

Reuse Add components/reducer actions for inline editing and remove `referenceConfirmed/gramsConfirmed` checkboxes. The final button is `Confirm & save`, disabled for missing required input, query errors/stale previews, unresolved candidates, pending requests or invalid dates. Removing checkbox JSX alone while retaining its validation gate is insufficient.

Date changes require validation but no ordinary-food recalculation; exercise date changes require a fresh dated estimate. Use stable clientId element keys so moving meals preserves other inputs.

If preview/source changes during confirmation, return item-specific errors and retain the entire batch. Show relevant before/after values and require another primary confirmation click; do not restore per-row safety-declaration checkboxes.

## 5.5 Save feedback

Success status/alert fades and collapses within ≤3 seconds. Text reflects top-level counts: `2 food entries saved` or `Workout saved`, not four dishes for four ingredients. Retain `Just saved · date · View saved records` until another success or leaving; actions must not depend on a brief Toast.

Clear the batch only after server transaction confirmation and a matching receipt. Return immediately to the actual editor without waiting for every GET. A background-refresh failure shows `Saved. Some views could not refresh.` with Retry views, never another POST.

While saving/unknown, freeze nested inputs, mode selection, Undo and additional items. Only explicit rejection such as 400/422 unfreezes correction; 5xx/timeouts/undecodable write responses remain unknown. See Chapter 16.

## 5.6 Specific implementation for U01/U02/U03

- Add `useTransientNotice` (or an equivalent existing Hook) and `TransientSuccessAlert`; reset timers by noticeId and clear on unmount. Start a 300ms fade at 2500ms, then unmount/Collapse; fake-timer tests verify total ≤3000ms. With reduced motion, omit animation but remove on schedule. Login errors must not auto-dismiss.
- Use `alignItems:flex-start` for `ProfileSections` `FormControlLabel`, retaining the checkbox's full click target and consistent label line-height. Do not align via absolute positioning/negative margins. Cover single/multiple lines and enlarged fonts.
- Dashboard's primary Add button is contained green; secondary History/Refresh use outlined/text styles with underline/focus. Data cards without onClick must not have pointer/hover button cues. Give record-detail access explicit View/chevron affordances and keyboard accessibility.

---

# 6. Smart food matching: full-catalogue coverage without false precision

## 6.1 Data layers

Keep real `food_reference` and `food_dataset` content/versions unchanged. Add product search metadata: short names, aliases, concepts, reviewed facets and display priority. **These are UX mappings, not new nutrition sources; never alter official per100g values.**

Separate complete-catalogue access from curated features: every active verified reference remains searchable by its complete original text; frequent groups receive reviewed aliases/attribute guidance. Uncurated entries still display actual official names and remain searchable. Do not promise immediate Chinese aliases for every original entry.

Frequent groups requiring review this version: rice, pasta/noodles, eggs, common chicken cuts, common beef/pork cuts, fish, prawns, milk/alternatives, yoghurt, cheese, oats/cereals, bread, common fruit/vegetables, tofu/legumes, potatoes, cooking oils, black/milk coffee. Create mappings only for combinations actually present in the catalogue.

## 6.2 Ranking process (deterministic, without an LLM as authority)

1. Normalise using Unicode NFKC, trim, English lowercase and whitespace/punctuation standardisation, maximum 80 characters.
2. Match English words/phrases rather than arbitrary within-word substrings; `rice` must not match `licorice`. Enable partial prefixes only for the currently unfinished final word, avoiding catalogue-wide fuzzy noise.
3. Match Chinese using reviewed aliases/longest dictionary matches. For unknown Chinese, explicitly suggest English/related names rather than claiming complete translation.
4. Extract explicit conditions **supported by reviewed mappings**; a cooked historical preference must not outrank an explicit raw request. Version multilingual aliases/conditions together.
5. Retrieve candidates through original-text term vectors/phrases and alias targets; exclude disabled/incomplete/unverified-source entries.
6. Rank by: no explicit-condition conflict → exact name/alias match → whole-word/attribute match → current user's recent use at equal relevance → product-reviewed common priority → stable name/immutable-ID ordering.
7. “Common” means product pinning/user history, not purported global popularity statistics. Recency cannot override explicit contrary conditions.
8. Suggest 5; `View all matching foods` exposes all real results for the same query, 20/page. Do not use an unrelated ranking or duplicate already displayed items.
9. Return matchKind EXACT/RELATED/APPROXIMATE, readable distinguishers and necessary questions, not percentage scores for the main UI.

Use bound SQL parameters; FTS may use PostgreSQL's `simple` dictionary and fixed term-vector indexes. Never concatenate user input. No external search cluster/vector database is required. See Chapter 13 for index creation.

## 6.3 When to ask follow-up questions

Calculate facets only from **actual current candidates**, not an imagined Cartesian product of attributes. Do not ask when there is only one candidate value or the input already specifies it. Show a small choice area when the distinction is useful and user-identifiable.

Priority: weighing state (raw/cooked) → food/cut → cooking method → added fat → dairy fat/type. Individual groups may override this order. Ask only the most useful current question, narrowing after answers rather than imposing a six-page questionnaire.

For `fried egg`, do not ask fried/boiled again; offer added-oil/no-extra-oil choices only where the catalogue distinguishes them. `Not sure` enters **explained approximate recommendations**, not false for unknown values. A result may read `Approximate reference · fried with added oil`; selection requires an explicit click.

Every recommendation must map to a real leaf referenceId. Without reasonable candidates, return an empty approximation area rather than forcing recommendations. Keep Record by ingredients as an explicit fallback. Matching must not choose supposedly “better” foods based on lower calories/higher protein.

## 6.4 Display and provenance

Result cards default to shortName + preparation labels, with no duplicate Selected area. Put officialName/publicFoodKey/dataset release revision in expandable Data source details for verification, not beneath every input.

`APPROXIMATE` describes the match relationship, not fictitious official data. `My dish` is user-composed, not an officially sourced whole dish. Store `selectionKind` (exact/related/approximate/user_selected) and search-metadata version for explanation, not complete search conversations/unauthorised external-model inputs.

## 6.5 Auditable data artefacts to generate

`backend/src/main/resources/data/ux/food-search-r1.json`: bind each entry to `(dataset release/revision, publicFoodKey)` and include:

```json
{
  "publicFoodKey": "<real source identifier, not a demo value>",
  "shortNameEn": "<short name preserving food meaning>",
  "shortNameZh": null,
  "conceptKey": "<reviewed everyday food group>",
  "aliases": [{"locale": "en", "value": "<everyday name>"}],
  "facets": {"state": "COOKED"},
  "commonRank": 20,
  "reviewStatus": "REVIEWED",
  "evidence": {"field": "officialName", "text": "<original-text evidence>"}
}
```

Missing facets are UNKNOWN, not false. Tools may propose splits, but verify each against original text afterwards; comma splitting cannot declare everything reviewed automatically. Validation detects nonexistent keys, conflicting short names, wrong raw/cooked mappings, unreviewed production entries and aliases pointing to unrelated concepts. Chinese names must be traceable; do not use unverified nutritional reinterpretations.

---

# 7. Recent items, reuse and context

## 7.1 Recent ordinary foods

Use only this user's confirmed, undeleted records. Sort by **actual creation/confirmation time**, not recordDate: backdating last month's food today counts as recent use. Correction updated_at does not move it to the top, avoiding preference changes from date-only edits. Deduplicate by immutable referenceId and offer grams from the most recent occurrence.

Dish ingredient leaves must not populate ordinary recents automatically, otherwise oil/seasonings from one dish fill the list. This version derives ordinary recents only from top-level SINGLE and separate dish recents from DISH. This is an engineering default; users can view full history without implicitly inventing additional preferences.

Hide the section when no recents exist. Local Retry on failure must not block normal search. Never read another account's data. A disabled reference may show unavailable/Change reference, but must not silently switch versions or change historical values.

## 7.2 Single-item reuse

Clicking Recent selects the original reference with `quantityGramsInput=''`; keep `lastAmountHint` for Use last X g. Clicking fills it and requests a fresh preview. Generate new clientEntryId only when actually adding/creating a draft, never copy the old ID or regenerate during renders/retries.

Recent-query cache keys include owner/epoch. Logout/account changes clear recents, previous grams and lastSaved, not only the main page.

## 7.3 Dish/Workout Log again

Read the original record's current snapshot, not a separate shared mutable template. Do not merge by dish name without a reliable recipe identity. Initially show historical record entries distinguished by date, at most 5; no MyRecipes favourites management.

`originRecord` stores only source kind/id for explanation/permission checks; new records have independent input snapshots/IDs. The server must not silently read missing grams/duration/fraction from origin; origin does not authorise copying everything.

If current fields were edited, Use last recipe/amounts/details fills only blanks and changes its label to Fill empty from last. Confirm the overwrite scope once if replacing values; never silently overwrite new user input. If an old reference cannot be newly used, mark only that item for reselection and preserve the others.

---

# 8. Dish portions, editing and storage semantics

## 8.1 Two modes do not create two sets of database nutrition

PERSONAL: ingredient.inputGrams is grams eaten this time; canonical factor=1/1.

FULL_RECIPE: ingredient.inputGrams is whole-recipe quantity; the current portion comes from fraction/percent/servings, applying one fraction to all ingredients. Preserve original input and derived personal grams, clearly distinguished in details.

Mode switches must not infer new values. Retain input and show `These amounts will now describe <my portion/full recipe>` with Cancel. Change labels only after explicit application; old fractions must not continue multiplying invisibly. Switching to FULL_RECIPE requires fraction reselection. Switching to PERSONAL requires explicit acknowledgement that retained grams represent personal intake, never silently treating a whole recipe as personal consumption.

## 8.2 Fraction precision

`1/3` is exact; do not convert it to 33.3% before calculation. The API preserves input form and rational value:

```text
FRACTION { numerator: 1, denominator: 3 }
PERCENT  { percent: 40 }        → canonical 2/5
SERVINGS { total: 4, eaten: 1 }  → canonical 1/4
```

FRACTION numerator/denominator are integers 1–1000000 with numerator≤denominator, then reduced. Percent is ≤100 with at most 3 decimals; total is integer 1–1000; eaten is >0 and ≤total, at most 3 decimals. These limits keep the smallest valid input positive in 12-decimal consumedGrams storage. Missing portions never default to all. Switching a shortcut fraction to percentage display may show approximate text, but must not change 1/3 to 33.333% without editing; retain original inputKind until a new input is explicitly submitted.

Do not add a fourth mode such as “cooked recipe weighs 800g; I ate 200g” this round. Raw ingredient totals are not finished weight. Actual meat/oil distribution may be uneven; this feature only allocates according to the user's fraction.

## 8.3 Calculation and UI

Dish summaries show name, ingredientCount, `My portion` or `½ of recipe`, and this intake's nutrition. Expanded details distinguish `Recipe amount (g)` and `Estimated consumed amount (g)`. PERSONAL shows personal grams only.

In whole-recipe mode edit ingredient.inputGrams, not unrestricted personal grams per ingredient that undermine the shared fraction. To adjust actual individual ingredient intake, explicitly switch to PERSONAL and enter eaten amounts. Different allocation fractions per ingredient in one dish are outside scope.

Changing an ingredient makes only that leaf/dish/batch totals stale, not other dishes. Changing fraction recalculates every dish leaf; changing dish name/meal does not recalculate nutrition. Dish names are not database search terms; renaming does not rematch ingredients.

## 8.4 History semantics

Each DISH has its own version. Update dish treats the complete current snapshot as one mutation unit; ingredient additions/removals, reference changes and fraction changes share a transaction. Removing an ingredient affects only this occurrence's leaf; zero ingredients cannot confirm—delete the dish instead.

Same-name dishes share no state. Log again creates a new header and all new leaf IDs; original data/creation receipt remain unchanged. Deleting the parent soft-deletes all active leaves while retaining source/client identities. Draft Undo does not restore deleted History; historical undelete is outside scope.

Old data without dish_id can only display as SINGLE; existing batch ID/date do not establish dish membership.

---

# 9. Add Exercise: catalogues, forms, periods and expenditure

## 9.1 Separate product options from expenditure references

Add product `ExerciseOption` (selectable activities/equipment/modes) and `MovementCatalogue` (strength movement/equipment details); retain versioned `ExerciseType` as the official expenditure reference.

An ExerciseOption may point to an approved ExerciseType or explicitly have no applicable mapping. Movement is not a new MET table. Never invent official machine-specific METs for chest press/leg extension, or rename an existing speed-specific Running reference as a precise generic running measurement.

Four UI categories: Cardio / Strength / Sports / Outdoor & Other. Support English/Chinese everyday aliases. Distinguish ambiguous equipment/movements with clear subtitles, especially `Rowing machine (cardio)` versus `Seated cable row (strength)`.

## 9.1.1 Search strength equipment directly in Quick without requiring Workout

Return clearly labelled `ACTIVITY_OPTION` and `STRENGTH_MOVEMENT` results. Combine planned options/movements queries in the frontend; Quick must not expose only generic strength training while hiding all specific equipment in Gym.

Selecting a MOVEMENT such as seated chest press creates a STRENGTH period using its reviewed `default_period_option_id` and adds the movement. Only period minutes are required; sets/reps/load remain optional. If only general resistance-training expenditure applies, label it as a general estimate. Even without a mapping, use a reviewed UNAVAILABLE STRENGTH option and allow recording.

For one Quick movement, use its snapshot name (e.g. Chest press) as the record name; for multiple movements show expandable Strength training. Calculate expenditure once from the period's option, not extra duration per movement. Adding a movement to an existing Gym strength period does not add another period.

Add `default_period_option_id` to MovementCatalogue (FK to a REVIEWED STRENGTH option). This defines a controlled default period category for independent Quick entry, not a movement MET. Backend reads/writes validate option applicability and must not trust arbitrary client substitution of higher-MET activities. Same-name movements in different exercise modes are independent records; names do not imply overlapping time.

## 9.2 Coverage required this version

This is the **selectable product catalogue** derived from user requirements, not a claim of distinct official METs for every row. Every option requires a reviewed mapping or explicit NO_APPLICABLE_REFERENCE and remains recordable.

|Category|Required discoverable options|
|---|---|
|Treadmill|Level/brisk walking, running, incline walking; separate modes, not one Treadmill option|
|Other cardio|Stationary rowing, elliptical, revolving stair climber, stepper, stationary/upright bike, recumbent bike, spin bike, ski machine/SkiErg, vertical climber|
|Ball sports|Basketball, football, badminton, tennis, table tennis, volleyball|
|Outdoor/other|Walking, jogging, running, hiking, cycling, swimming, yoga, dancing; retain existing choices including HIIT|
|Chest|Bench press, incline bench press, seated chest press, pec deck fly, cable fly|
|Back|Lat pulldown, seated row, assisted pull-up, barbell row, one-arm dumbbell row, back extension|
|Shoulders|Shoulder press machine, dumbbell shoulder press, lateral raise, reverse fly|
|Legs/glutes|Squat, Smith squat, hack squat, leg press, leg extension, seated/lying leg curl, hip thrust, hip abduction/adduction, calf raise, deadlift|
|Arms|Biceps curl, triceps extension, cable pushdown, assisted dip|
|Core|Abdominal crunch machine, torso rotation, leg raise, plank|
|Equipment aliases|Dumbbell/barbell/kettlebell, Smith machine, cable crossover, pec deck etc. for search/filtering; cable crossover alone is not one uniquely defined movement|

Current r1 already includes badminton, elliptical and rowing; do not repeat an earlier claim that badminton is absent. UX-P00 checks the actual current catalogue item by item and reuses references without duplicate import.

## 9.3 Reference mapping and uncertainty

Add `exercise-options-r1.json` and `movement-catalogue-r1.json` with product keys, short names, aliases, categories, allowed detail fields, mapping sources and evidence.

Mapping classes: `SPECIFIC_REFERENCE` (actual corresponding entry), `GENERAL_ACTIVITY_ESTIMATE` (justified general estimate for a similar activity), `UNAVAILABLE`. Do not map mismatched categories/text merely because METs are close, average two METs or invent interpolation from machine levels.

This round's web inspection only confirms official sports/conditioning entries and the prohibition on modifying METs or combining different levels. **Final import must still use repository source locks, checksums, reviewed manifests and tests.** Online HTML cannot replace pinned original bytes.

Do not add required intensity. If a direct equipment mapping is absent but a general same-category mapping is defensible, label General estimate; for example, do not assume incline walking falls in a specific speed/incline range. Otherwise permit saving with unavailable expenditure. Equipment parameters remain details without automatic MET changes, keeping E3-B compatible with E5-A.

## 9.4 Shared Quick/Gym period model

`ActivityPeriod` contains clientEntryId, optionId, periodKind(ACTIVITY/STRENGTH), durationMinutes, optional equipmentDetails and optional movements.

Quick has one period; Gym has 1–12. Each period produces one canonical `exercise_record`; MovementDetail produces no energy record. The Workout parent groups records and does not store another summable duration/energy total.

Strength duration is actual elapsed time for that strength period, including normal between-set rests if applicable. Do not also include separate cardio or long non-training breaks. Show one brief `Include this strength period only; log cardio separately.` hint, not repeated safety declarations. The app does not collect start/end times and cannot guarantee arbitrary overlap detection; do not advertise automatic detection.

Example: one 50-minute strength period with 3 movements plus 20-minute incline walking totals 70, not 3×50+20. Movement load/sets/reps are not MET-formula inputs; load kilograms are not body-weight kilograms.

## 9.5 Optional training details

**All fields may be omitted. Empty means null, not 0, and must not block recording name/duration alone.** Validate supplied values; invalid values block confirmation with feedback rather than being silently discarded.

|Field|UI/storage|Engineering validation default|
|---|---|---|
|Speed|Speed(km/h), `speedKmh`|>0 and ≤100, ≤2 decimals|
|Incline|Incline(%), `inclinePercent`|−20–40, ≤2 decimals; only treadmill-supporting options|
|Distance|Treadmill may show km, rowing m; canonical `distanceMetres`|>0 and ≤1,000,000m; convert once, ≤2 decimals|
|Machine level|Machine level, `machineLevel`|Integer 0–100; not a MET band or cross-brand comparison|
|Sets|Sets, `setCount`|Optional; otherwise integer 1–100|
|Repetitions|Reps per set, `repetitions`|Optional; otherwise integer 1–1000|
|Load|Load/Assistance kg, `loadKg`|Optional; otherwise 0–1000, ≤2 decimals|

Record one concise sets×reps×load summary per movement. Do not add varying per-set weights, training-volume charts, 1RM predictions or automatic plans. A future per-set table requires separate scope.

Movement catalogue `loadBasis` defines TOTAL_BARBELL (including bar), PER_DUMBBELL (one dumbbell), MACHINE_DISPLAY (machine scale), ADDED_LOAD (extra bodyweight-exercise load), or ASSISTANCE (assistance weight). Labels must be clear; ASSISTANCE is not added resistance. Do not calculate energy or compare cross-machine volume from these values. Planks do not require reps; name alone is allowed.

The selected equipment defines allowed fields. On switching, do not apply old treadmill incline to rowing: confirm discarding incompatible details, let Cancel restore the original type, and retain only compatible fields/minutes after confirmation.

## 9.6 Preview and changes

Names/movements/load/reps/sets/equipment details do not alter estimates; type/period minutes/date do. If profile/weight changes after preview invalidate the fingerprint, return CALCULATION_CHANGED with affected periods/new values for reconfirmation. Never save the old preview or expose MET/debug fields.

Every period needs valid input and a current available-or-explicitly-unavailable preview before Review/save. A failed preview request differs from confirmed unestimability: failure cannot masquerade as Unavailable success, whereas explicit unestimability permits saving.

## 9.7 Gym history, corrections, deletion and reuse

Show one Workout History card with date/title/total minutes/general estimated expenditure or partial unavailability. Expand periods, then strength movements. Quick remains a single entry.

Edits affect only the current workout version. Changing period minutes/type/date recalculates that period; movement-parameter-only edits retain its energy snapshot. Changing Workout date checks every period against the new date. Removing a movement does not change strength minutes; removing a period subtracts its duration.

Deleting Workout requires confirmation and transactionally soft-deletes all periods; movements never contribute separately to statistics. Old single-exercise APIs must not mutate Workout children; use the parent API/version to preserve group consistency.

Log again leaves duration blank and offers Use last duration(s), without creating confirmed records. Hide empty recent shortcuts. Use distinct wording/submission paths for creating new versus correcting old records.

---

# 10. Frontend state model and code changes

## 10.1 Keep the Provider, replace domain types, avoid duplicate equivalent state

Retain `DraftProvider` owner-remount, synchronous ref snapshots and reducer ownership checks. Extend `draftTypes.ts`, replacing food's flat `foods` with top-level `entries`; do not maintain independently mutable foods/entries as two sources of truth. Flatten through selectors where compatibility requires it.

The following defines **responsibilities of proposed types**; final DTOs come from OpenAPI with strict TS implementation, not a complete project ready to paste:

```ts
type FoodEntryDraft = SingleFoodDraft | DishDraft;
type PortionInput =
  | { kind: 'FRACTION'; numerator: string; denominator: string }
  | { kind: 'PERCENT'; percentInput: string }
  | { kind: 'SERVINGS'; totalInput: string; eatenInput: string };

type FoodDraftV3 = DraftBase & {
  kind: 'food'; entries: FoodEntryDraft[];
  defaultMealType: MealType | '';
  composer: FoodComposer | null; // Editing input not yet added
  selectionTarget: SelectionTarget | null;
  undo: UndoEntry | null;
};
// SINGLE: clientEntryId, referenceId, selectedReference, gramsInput,
// mealType, selectionKind, lastAmountHint?, origin?
// DISH: clientEntryId, name, mealType, inputMode,
// ingredients[{clientEntryId, referenceId, selectedReference, inputGrams}],
// portion|null, lastRecipeHint?, origin?

type ExerciseDraftV3 = DraftBase & {
  kind: 'exercise'; mode: 'QUICK' | 'WORKOUT';
  rootClientEntryId: string; titleInput: string;
  periods: ActivityPeriodDraft[];
};
// QUICK: periods.length===1; root and period share one business-record identity.
// WORKOUT: 1..12 periods; root has an independent identity.
// period: clientEntryId, periodKind, optionId, minutesInput,
// detailsInputs, movements[], lastDurationHint?
// movement: rowKey, catalogueId, optional setCountInput/repsInput/loadInput
```

Represent intermediate input as strings; parsing produces finite decimal numbers for submission. Do not store unsubmitted fields in URLs/Query caches, or duplicate already-added grams in useState. Expanded rows, cursors and popovers are local UI state, excluded from canonical requests.

## 10.2 Actions and side effects

Retain START_NEW/CHANGE_DATE/BEGIN_SUBMIT/UNKNOWN/REJECTED/CONFIRMED intent and add/rename:

|Action / helper|Responsibility|
|---|---|
|SET_DEFAULT_MEAL|Affect subsequent items only, not existing entries|
|START_COMPOSER / UPDATE_COMPOSER / CANCEL_COMPOSER|Manage the unadded item; Cancel preserves added entries|
|ADD_ENTRY|Add valid composer as SINGLE/DISH with stable UUID, clear composer and refocus search|
|PATCH_SINGLE / PATCH_INGREDIENT|Change grams etc., increment revision and invalidate relevant previews|
|SET_ENTRY_MEAL|Change one top-level entry; DISH ingredients inherit parent meal|
|BEGIN_REFERENCE_REPLACEMENT / APPLY_REFERENCE / CANCEL_REFERENCE_REPLACEMENT|Track target/return position; replace only after explicit selection, not as a new entry|
|REMOVE_ENTRY / REMOVE_INGREDIENT / UNDO_REMOVE|Remove/restore complete subtree with original UUID/index|
|CHANGE_DISH_MODE / SET_PORTION|Explicit mutually exclusive mode/portion changes; clear old previews|
|COPY_RECORD_TO_DRAFT|Log again creates new IDs/blank portions without changing history or creating receipts|
|USE_LAST_AMOUNTS / USE_LAST_RECIPE / USE_LAST_DURATION / USE_LAST_DETAILS|Fill only after user clicks; never silently overwrite new values|
|ADD_PERIOD / PATCH_PERIOD / REMOVE_PERIOD / ADD_MOVEMENT / PATCH_MOVEMENT|Edit Workout structure; movements do not change period time|
|CONTINUE_AFTER_SAVE|New empty draft + date rule C + in-memory lastSaved; never trigger with an unmatched receipt|

Reducers must be pure. Create UUIDs in user creation/addition commands; retries reuse frozen IDs. Perform HTTP, focus, scrolling and toast timers in Hooks/component effects, not reducers.

Remove old `referenceConfirmed`/`gramsConfirmed` fields/actions/validation gates. `selectedReference` must reflect a real current choice; unselected AI candidates have referenceId=null. Removing checkboxes must not auto-select. Replacement tests retain the business assertion that unselected candidates cannot save.

## 10.3 Draft revisions and preview keys

Increment revision for every canonical-content change, including name/meal/order/movement details; only calculation-related changes require fresh nutrient/energy previews. Distinguish:

- `requestSignature`: full prepared submission + revision, for freezing/preserving changes.
- `calculationSignature`: actual calculation inputs, determining recalculation.
- `selectionTarget`/expanded UI state do not change business revision.

Food calculation signatures include referenceId, inputGrams, canonical fraction and policy. Exercise includes option/reference-mapping versions, date, minutes and fingerprint associations. Display units do not alter the original kJ signature. Owner/epoch reject late responses after account switching.

## 10.4 New submission state

```text
EDITING -> PREVIEWING -> READY
READY --Confirm--> SAVING(frozen deep payload, submissionId, epoch)
SAVING --verified committed receipt--> SAVED -> NEW_EMPTY_EDITOR
SAVING --definite rejection--> REJECTED -> EDITING
SAVING --timeout/5xx/malformed response--> UNKNOWN
UNKNOWN --check/retry same envelope--> SAVED / UNKNOWN / REJECTED
UNKNOWN --explicit leave--> discard in-memory recovery, History self-check
hard reload at any step --> no draft/recovery reconstruction
```

Extend `freezePayload` to **full recursive freezing**, not merely first-level items. Freeze dish ingredients, portion, period details, movements and expected previews.

Stop identifying food/exercise through `'items' in payload`; both V2 domains may contain arrays. Use explicit `{apiVersion:2, domain:'FOOD'|'EXERCISE', payload}` discrimination. Update `api/submissions.ts`, `useSubmitDraft` and receipt validation.

## 10.5 Component file arrangement

Reuse existing files and add under `features/food` and `features/workout`, without rebuilding another App:

```text
features/food/
  FoodSearchPanel.tsx           Search, suggestions, facets, full expansion
  RecentFoods.tsx               Exact reference and last-grams hint
  FoodComposer.tsx              Compact selection/grams/meal/preview/Add
  MealDraftList.tsx             Mixed SINGLE/DISH, inline edits, Undo
  SingleFoodRow.tsx             Shared Add/Review row
  DishEditor.tsx                Name, mode, ingredients, portion
  IngredientRow.tsx             Real reference and entered grams
  PortionInput.tsx              Exclusive fraction/percent/servings
  DishCard.tsx                  Shared My meal/Review/History display
  FoodEntryDetails.tsx          User-expanded provenance
  foodDraftSelectors.ts         flatten/canonical/reuse/validity
features/workout/
  ExerciseOptionPicker.tsx      Categories, aliases, relevant search, recents
  ActivityPeriodEditor.tsx      Duration/option/optional equipment details
  EquipmentDetails.tsx          Whitelisted parameter form
  MovementPicker.tsx            Strength movement search
  MovementDetailsRow.tsx        Optional sets/reps/load
  QuickExerciseEditor.tsx       One period
  WorkoutEditor.tsx             Multiple-period structure
  WorkoutCard.tsx               Shared History/Review grouping
  WorkoutReview.tsx             Edits/total minutes/known expenditure
  workoutSelectors.ts           signatures/canonical/reuse
features/entry/
  draftTypes.ts / draftReducer.ts / draftValidation.ts (extend)
  ReviewPage.tsx / CorrectionReviewPage.tsx (unified dispatch)
  submissionPayload.ts / previewIdentity.ts (new schema support)
```

Make existing `FoodEditor`/`ExerciseEditor` lightweight entry points/adapters. Remove duplicated logic after moving responsibilities; do not retain conflicting validators. Adapt `ReviewFoodRow` to `SingleFoodRow`/`DishCard`; similar names do not justify a third form copy.

---

# 11. Calculation, precision and snapshots (server authority)

## 11.1 Food and dishes

Continue using `FoodCalculationService.calculate()` for ordinary food. Add `DishCalculationService` with `PortionNormalizer` and an explicit scaled entry point in the original calculator:

```text
ingredient consumed grams = entered grams × numerator / denominator
ingredient nutrient = per100g × entered grams × numerator / (100 × denominator)
DISH nutrient = sum(final rounded stored ingredient nutrients)
BATCH nutrient = sum(SINGLE snapshots + DISH leaf snapshots), each leaf once
```

Use BigDecimal and reduced positive rational fractions. **Never round whole-recipe nutrients before applying the fraction, truncate ⅓ to 33.3%, or recalculate from rounded displayed grams.** Round each leaf's energy/macronutrients HALF_UP to 3 decimals, then aggregate those same snapshots in every view. User-friendly display rounding must not become calculation input.

Existing `food_record.quantity_grams` is numeric(10,3). **Expand additively to numeric(24,12)** for fractional portions, updating JPA precision/scale; ordinary user input remains ≤3 decimals. Preserve `ingredient_input_grams` (original recipe ≤3 decimals) plus header ratio; quantity_grams stores personal consumed amounts to 12 decimals. Recalculate from original recipe/ratio, not rounded consumedGrams. Show legal amounts below 0.001g as `<0.001 g`, not exact zero.

Widening preserves old values but affects schema validation/response precision and needs migration/compatibility tests. Do not relax V1 single-input limits to accept 12-decimal manually entered values.

## 11.2 Independently verifiable examples (synthetic fixtures, not real foods)

Assume reference A=1000kJ/100g and B=400kJ/100g; whole recipe A=300g, B=150g.

- Whole-recipe energy=3000+600=3600kJ.
- 1/3 portion: personal A=100g, B=50g; leaf values 1000.000 and 200.000; dish=1200.000kJ.
- 33.3%: personal A=99.9g, B=49.95g; total=1198.800kJ. This **differs** from 1/3; tests must distinguish them.
- Add SINGLE C snapshot=500kJ: total=1700.000kJ for ⅓, not 3600+1200+500.
- Recipe inputs of 0.001g with portion=0.001% must retain positive consumed grams rather than rounding to zero at old scale3 and failing a check.

## 11.3 Exercise expenditure

With an applicable reference, retain the existing formula and dated weight:

```text
period kJ = MET × bodyWeightKg × durationMinutes × 4.184 / 60
```

Calculate per period only; Workout roots have no second energy value. Movement loadKg, sets/reps, machine levels and recorded speed/incline are not formula inputs this round. This does not measure individual physiological expenditure or produce weight-loss prescriptions/complete calorie balance.

Aggregate total minutes across valid periods; known energy=sum(AVAILABLE values). Partial unavailability shows the known total plus `Some activities have no estimate`; wholly unavailable shows Unavailable, not 0. Align known/missing semantics with existing MetricAggregate.

Synthetic test: P1=3.5MET, 70kg, 50min →854.233kJ; P2=5MET, 70kg, 20min →488.133kJ; total minutes=70, known energy=1342.366kJ. Adding three movements under P1 must not triple 854.233. Here 5MET is only a fixture, not an actual incline-machine coefficient.

## 11.4 Preview fingerprints and historical corrections

FoodV2 fingerprints cover leaf references+entered grams+canonical ratio+policy. Source references are immutable; labels/display units/meals are excluded from nutrition fingerprints. Workout fingerprints cover ordered period IDs and period calculationFingerprints; details/title are excluded from energy fingerprints but included in full submission hashes.

For new saves, lock owner and references/mappings, recalculate and compare fingerprints to prevent stale values after weight/reference-condition changes. Return a current displayable preview on change, not internal SQL/MET debugging.

For corrections, preserve snapshots for unchanged references/calculation inputs; recalculate changed grams/ratios/references. Recalculate Workout periods only for date/type/minutes changes. Name, meal, movement load or descriptive-detail changes must not rewrite historical expenditure using today's changed body weight.

---

# 12. Backend change register (UX-BC, rationale per change)

These are **targets for this round**, not claims of existing implementation. Retain old BC identifiers as history without renumbering.

|ID|Why backend changes are required|Existing entry point|Target and verification|
|---|---|---|---|
|UX-BC01|Substring matching/alphabetical sorting cannot implement Smart matching|FoodReferenceService/Repository|UX metadata, smart search, facets/aliases; full-catalogue reachability and no false matches|
|UX-BC02|Recents require owned confirmed data without downloading all history|RecordReadService/Repositories|Food/dish/exercise recents and read-only reuse details; owner isolation and inactive rules|
|UX-BC03|Original recipes/portions are not ordinary FoodRecords|FoodRecord/new FoodDishRecord|Parent, recipe quantities, ratio, grouping and precision; no inferred historical groups|
|UX-BC04|⅓ portions cannot rely on frontend-rounded nutrition submissions|FoodCalculationService|Scaled calculator/dish preview; server authority and independent numeric tests|
|UX-BC05|Flat V1 receipts cannot express parent/leaf identities|SubmissionService/Hasher/Receipt|V2 operations, minimal typed roots/leaves proof, canonical hash; retain V1 read/write validation|
|UX-BC06|Mixed ordinary/dish batches must save together|FoodRecordService/new FoodEntryService|V2 create/update/delete roots, not HTTP loops; atomicity/concurrency/rollback|
|UX-BC07|Equipment options differ from official expenditure references and coverage is limited|ExerciseCatalog/Activity import|ExerciseOption+MovementCatalogue, source mappings, optional parameter definitions|
|UX-BC08|Gym periods/movements must persist without double counting|ExerciseRecord/new WorkoutRecord|Parent, period links, movement/equipment details; no-applicable-reference constraints|
|UX-BC09|Quick options/Workout periods need unified preview|ExerciseEstimateService|Reuse dated estimates per period, explicit missing mapping and combined fingerprint|
|UX-BC10|Workout creation/correction/deletion requires parent transactions/version|ExerciseRecordService/new WorkoutService|V2 creation/root mutations; prevent old child endpoints bypassing parent versions|
|UX-BC11|Parent card counts differ from leaf counts; avoid duplicate statistics|SummaryRepository/Service/RuleSummaryService|Retain leaf sums; root counts, grouped reads and typed highlights|
|UX-BC12|Image/speech use old flat drafts/candidate UI|FoodCandidateMatcher/Speech adapters as needed|Keep catalogue candidates and adapt singles into unified drafts; no automatic dish/recipe creation|
|UX-BC13|New identities/fields/schema require strict compatibility/security|OpenAPI/generator/ApiError/guards|Strict V2 input, typed IDs, owner/version, minimal receipts, no secret leaks|
|UX-BC14|Applied Flyway history prevents editing V1–V8|migration/preflight/Schema verifier|Append-only migrations, backup/restore, source-manifest versions, explicit rollback strategy|

Final logs for every entry include Why / old vs new / files / API impact / DB migration / tests / rollback / evidence. Frontend-only U01/U02 and button/chart layout need no backend change; record none.

---

# 13. Database design: incremental extensions and one statistical ledger

## 13.1 Selected approach (no competing alternatives for implementers)

**Do not introduce a universal JSON record table mixing food and exercise.** Each domain has aggregate roots while actual intake/activity periods remain in existing food_record/exercise_record leaf ledgers, reusing current aggregation.

Add seven business/UX tables: `food_search_profile`, `food_search_alias`, `food_dish_record`, `exercise_option`, `movement_catalogue`, `workout_record`, `workout_movement_log`. Do not add recipe templates, training plans, per-set weight history or persistent draft tables.

Most additional fields belong to food_record/exercise_record; submission_receipt only expands operation types and V2 minimal-proof validation. UX-P02 rechecks actual DDL names against the current schema to avoid duplicating equivalent existing tables.

## 13.2 food_search_profile / food_search_alias

- profile: id, reference_id FK, metadata_revision, is_active, review_status, short_name_en, nullable short_name_zh/concept_key, facets JSONB, common_rank, evidence JSONB (official-field evidence only, not user queries).
- Unique `(reference_id,metadata_revision)`; partial uniqueness for `reference_id WHERE is_active AND review_status='REVIEWED'`. Source content stays unchanged; search mappings may add revisions.
- alias: id, profile_id or concept_key (exactly one target), locale, normalised_text, metadata_revision, review_status. One phrase may target multiple related candidates; do not impose incorrect global uniqueness requiring one word to mean one food.
- Profile/alias indexes/FTS only accelerate display; verified foods without profiles remain available through original-text fallback.
- Metadata import checks existing source keys, non-conflicting facets and targets within the same reviewed catalogue. Disable erroneous UX mappings rather than deleting food_reference.

## 13.3 food_dish_record

|Field|Requirement|
|---|---|
|id|bigint identity PK; decimal string in frontend|
|user_id|app_user FK; owner constraints on all reads/writes|
|client_entry_id|UUID unique per owner, not released by soft deletion|
|name|1–100 characters; user dish name, not an official name|
|recorded_date / meal_type|Uniform date/meal for this group|
|input_mode|PERSONAL / FULL_RECIPE|
|portion_input_kind|PERSONAL / FRACTION / PERCENT / SERVINGS|
|portion_numerator / portion_denominator|Positive bigint, n≤d, stored reduced; PERSONAL requires 1/1|
|portion_input_json|Restricted input echo: PERSONAL empty object only; otherwise only numerator/denominator, percent, or total/eaten; no arbitrary payload|
|source|Retain MANUAL/VOICE/IMAGE; dishes in this round are normally MANUAL; never infer source from a user name|
|origin_dish_id|Nullable self-FK, only owned existing records may be reuse origins; no dynamic calculation lookup|
|created_at/updated_at/version/deleted_at|Instant/optimistic locking/soft deletion; preserve history|

Do not duplicate summable nutrition/grams in the parent; derive totals from active leaf snapshots. Dishes cannot nest: ingredients reference food_reference, not another dish.

## 13.4 food_record extensions

- `dish_id nullable` FK to food_dish_record; null means an existing independent item.
- `dish_position nullable`, unique among active group positions (or service-wide ordering with deferred constraints); removing/readding must not reuse another entry's clientEntryId.
- `ingredient_input_grams nullable numeric(12,3)`: original recipe/personal entered grams.
- `nutrition_policy varchar(48)`: new groups use `RECIPE_PORTION_RATIONAL_V1`; existing singles retain their policy or explicit `REFERENCE_GRAMS_V1`.
- Optional explanatory `selection_kind`/`search_metadata_revision`; unknown defaults to USER_SELECTED without fabricated approximation state.
- Expand `quantity_grams` to numeric(24,12) for actual allocated intake; retain scale3 nutrient snapshots.

Constraints: grouped leaf owner/date/meal match parent; entered g>0 and ≤10000; consumed>0; no active children under a deleted parent; parent membership cannot cross owners. Prefer **deferred constraint triggers** checked at commit, allowing parent-before-child batch writes/soft deletion. Triggers validate only, without generating nutrition or changing old dates implicitly.

Old SINGLE retains dish_id=null, without invented ingredient_input_grams/parents. Old flat reads may expose leaves for compatibility, but PUT/DELETE on dish_id!=null returns `GROUP_EDIT_REQUIRED`.

## 13.5 exercise_option / movement_catalogue

ExerciseOption: id, option_key, revision, active, category, period_kind, display_name, aliases JSONB, allowed_details JSONB, nullable reference_id FK exercise_type, mapping_kind, mapping_evidence, review_status. At most one active version per option_key; old records retain option_id and confirmed mappings.

MovementCatalogue: id, movement_key, revision, active, display_name, aliases, body_region, equipment_type, load_basis, allowed_fields, review_status, default_period_option_id (controlled STRENGTH-option FK). It has no MET and must not masquerade as an activity_dataset entry. Import reviewed r1 display data; arbitrary external input cannot create official movements.

GENERAL_ACTIVITY_ESTIMATE requires mapping_evidence explaining applicability. UNAVAILABLE options remain selectable/saveable; catalogue capability checks must not disable all of them merely because some lack METs.

## 13.6 workout_record / exercise_record / workout_movement_log

Workout parent: id, user_id, client_entry_id, title, recorded_date, source, nullable origin_workout_id, created_at, updated_at, version, deleted_at. No additional summable parent energy.

Add to exercise_record:

- `workout_id nullable`: null=Quick/existing single; nonnull=group period.
- `period_position nullable`, `period_kind` ACTIVITY/STRENGTH; retain LEGACY/null view compatibility without inferring Workouts from names.
- `exercise_option_id nullable`, `option_name_snapshot` preserving the specific equipment name.
- Strictly whitelisted `equipment_details JSONB`; use `{}` or null consistently when absent.
- Retain existing exercise_type_id/MET/weight/date/energy/formula/status as actual calculation snapshots.

MovementLog: id, exercise_record_id FK, movement_catalogue_id FK, position, name_snapshot, load_basis_snapshot, nullable set_count/repetitions/load_kg; **no duration_minutes or estimated_kj**. Inherit ownership/deletion visibility from the period; do not expose independent cross-owner endpoints.

Existing `exercise_record_current_complete` requires exercise_type_id for CURRENT rows. Extend only the legitimate new branch: absent type requires a reviewed active option, status=NO_APPLICABLE_REFERENCE and NULL energy/MET/weight calculation snapshots. Existing completeness rules remain for rows with types. Never remove the entire check or relabel everything LEGACY to bypass it.

Movements/parameters may be absent; both DB/service reject invalid supplied values. Equipment JSON must be an object, rejecting extra keys, numeric strings and non-finite numbers. Services also validate option.allowed_details. Soft-delete periods transactionally with the parent; movements become invisible through parent/period while audit data remains.

## 13.7 submission_receipt: preserve existing safeguards

Retain V1 operations FOOD_BATCH / EXERCISE_CREATE. Add `FOOD_BATCH_V2` and `EXERCISE_ENTRY_V2` (both fit current varchar20), with distinct hash-version prefixes. **Do not change old canonical hashing and cause retry conflicts for existing receipts.**

Current receipts allow only `records[{id,clientEntryId,recordDate,versionAtCreation}]`, at most 50. V2 needs typed roots/leaves. Add operation-specific minimal-JSON validation and adjust CHECK; V1 retains existing rules, V2 permits only Chapter 14's limited keys. Preserve post-submission immutability/deletion triggers and deferred triggers preventing incomplete receipts from committing.

Store minimal identity proof, not dish names, recipes, nutrition, movement weights, audio, prompts, safety answers or entire requests. Input hashes verify consistency; read content from current owned record details.

## 13.8 Transaction locks and root versions

All writes retain lock order: owner → parent if any → sorted reference/option shared locks → sorted leaves. No model/HTTP calls inside locked transactions.

Group updates/deletes use parent expectedVersion only. The same transactional service controls leaf changes; old child-write endpoints reject grouped children. On conflict retain the draft and return 409, never automatically overwrite with a new version.

One create request executes one outer TransactionTemplate. Existing `SubmissionService` rejects joining an already-open outer transaction, so calling creation services **must not also use @Transactional**. Corrections/deletions manage their own @Transactional scope without nested creation-receipt calls.

---

# 14. API contracts: add V2 without breaking original receipts

## 14.1 Naming and old interfaces

Document version 3.0 and API v2 are distinct. Backend paths use `/api/v2`; frontend Axios already has baseURL=`/api`, so apiGet/Post arguments use `/v2/...`, never `/api/api/v2`.

Retain V1 semantics/tests for `/api/records/food/batch`, `/api/records/exercise`, `/api/submissions/{id}` and related paths. The new frontend uses V2 aggregate-root reads/writes; old Correction reads of SINGLE can still adapt. V1 cannot bypass group parent versions. Synchronise capabilities, OpenAPI, strict schemas and fixtures when limits/enums change.

## 14.2 Endpoint matrix

|Method / path|Responsibility|
|---|---|
|GET `/api/food-reference/smart-search`|query/page/size/mode/facets; shortName/candidates/optional questions; retain old search|
|GET `/api/v2/food/recent`|Owned recent SINGLE/DISH, controlled pagination/limits|
|POST `/api/v2/food/preview`|Authoritative leaves/root totals/fingerprints for singles/dishes; no writes|
|POST `/api/v2/food/batches`|Create whole SINGLE/DISH batch with V2 receipt; 201 new/200 replay|
|GET `/api/v2/food/entries`|from/to/meal/page/size; mixed independent singles/dish roots without duplicate internal leaves|
|GET/PUT/DELETE `/api/v2/food/entries/{kind}/{id}`|kind=single/dish; owned details/root correction/soft deletion|
|POST `/api/v2/food/entries/{kind}/{id}/preview`|Correction preview allows this record's original inactive reference, unlike new reuse|
|GET `/api/v2/exercise/options`|Category/search/pagination; product options/mapping availability|
|GET `/api/v2/exercise/movements`|Body region/equipment aliases/search; no movement MET|
|GET `/api/v2/exercise/recent`|Quick/Workout reuse and previous-detail hints|
|POST `/api/v2/exercise/preview`|Estimate each Quick/Workout period and overall fingerprint|
|POST `/api/v2/exercise/entries`|Create one Quick or Workout root atomically|
|GET `/api/v2/exercise/entries`|Date/option/category pagination returning roots|
|GET/PUT/DELETE `/api/v2/exercise/entries/{kind}/{id}`|kind=quick/workout; owned whole-root reads/updates/deletion|
|POST `/api/v2/exercise/entries/{kind}/{id}/preview`|Owned historical-correction preview|
|GET `/api/v2/submissions/{submissionId}`|V2 typed minimal receipt for current owner only|

Recent/reuse GET details create a **new frontend draft**; no automatic Log again write API, server drafts, recovery identifiers or cross-device sync API is needed.

Retain RecordReadService's safe date ranges; verify existing limits and document them in OpenAPI. Default page size 20, maximum 100, never download all history at once. Paginate roots/independent entries rather than splitting dish children across pages.

## 14.3 Food request structure

Numeric IDs/UUIDs below are schema examples only; runtime uses queried IDs and fresh UUIDs. Do not import these demo references.

```json
{
  "submissionId":"11111111-1111-4111-8111-111111111111",
  "recordDate":"2026-10-17",
  "zone":"Australia/Sydney",
  "source":"MANUAL",
  "previewFingerprint":"<current server preview 64-character hash>",
  "entries":[
    {
      "kind":"SINGLE",
      "clientEntryId":"22222222-2222-4222-8222-222222222222",
      "referenceId":"101",
      "quantityGrams":150,
      "mealType":"LUNCH",
      "selectionKind":"USER_SELECTED"
    },
    {
      "kind":"DISH",
      "clientEntryId":"33333333-3333-4333-8333-333333333333",
      "name":"My mixed dish",
      "mealType":"LUNCH",
      "inputMode":"FULL_RECIPE",
      "portion":{"kind":"FRACTION","numerator":1,"denominator":3},
      "ingredients":[
        {"clientEntryId":"44444444-4444-4444-8444-444444444444","referenceId":"102","inputGrams":300,"selectionKind":"USER_SELECTED"},
        {"clientEntryId":"55555555-5555-4555-8555-555555555555","referenceId":"103","inputGrams":150,"selectionKind":"APPROXIMATE"}
      ]
    }
  ]
}
```

DISH creation through Log again may include `originDishId` (decimal string); WORKOUT may include `originWorkoutId`. First verify the source is an existing owned record, rejecting another user's source. It records lineage only and never dynamically overrides submitted recipes/periods. Include it in payload hashes but not nutrition/energy fingerprints; historical PUT cannot change origin. Omit for ordinary manual creation; store NULL.

PERSONAL explicitly uses `{"kind":"PERSONAL"}` and stores 1/1; reject extra percent etc. SINGLE rejects dish fields; DISH rejects quantityGrams/client nutrients. Input clientEntryIds are stable/unique. Preserve root/ingredient array order canonically, without re-sorting user order.

Food preview uses the same structure without submissionId/previewFingerprint (optional draftRevision echo), returning entries with leaf inputGrams/consumedGrams/nutrients, totals, fingerprint, calculationPolicy and inputSignature. Individual ingredient previews may reuse old by-id/nutrition; whole dishes/batches use server composition rather than frontend-rounded sums.

## 14.4 Exercise request structure

```json
{
  "submissionId":"66666666-6666-4666-8666-666666666666",
  "recordDate":"2026-10-17",
  "zone":"Australia/Sydney",
  "source":"MANUAL",
  "previewFingerprint":"<current overall server preview hash>",
  "entry":{
    "kind":"WORKOUT",
    "clientEntryId":"77777777-7777-4777-8777-777777777777",
    "title":"Chest and cardio",
    "periods":[
      {
        "clientEntryId":"88888888-8888-4888-8888-888888888888",
        "periodKind":"STRENGTH",
        "optionId":"201",
        "durationMinutes":50,
        "calculationFingerprint":"<this period dated estimate fingerprint>",
        "equipmentDetails":{},
        "movements":[{"catalogueId":"301","setCount":3,"repetitions":8,"loadKg":40}]
      },
      {
        "clientEntryId":"99999999-9999-4999-8999-999999999999",
        "periodKind":"ACTIVITY",
        "optionId":"202",
        "durationMinutes":20,
        "calculationFingerprint":"<this period dated estimate or unavailable fingerprint>",
        "equipmentDetails":{"speedKmh":5,"inclinePercent":8},
        "movements":[]
      }
    ]
  }
}
```

QUICK uses `kind:QUICK, clientEntryId, period`; period/root are the same exercise_record and must share clientEntryId rather than create two records. WORKOUT root UUID differs from every period UUID. Only STRENGTH may have nonempty movements; equipment fields follow option whitelists.

Represent omitted optional parameters consistently as null/omitted per schema, normalising equivalent empties canonically. Missing duration cannot be inferred from history/sets/reps. Reject client-authoritative `met`, `calories`, `weightKgUsed` etc.

## 14.5 V2 proof and identity validation

```json
{
  "schemaVersion":2,
  "submissionId":"<original request ID>",
  "kind":"FOOD_BATCH_V2",
  "committedAt":"<server time>",
  "replayed":false,
  "roots":[{"kind":"DISH","id":"71","clientEntryId":"<group UUID>","recordDate":"2026-10-17","versionAtCreation":0}],
  "leaves":[{"kind":"FOOD_COMPONENT","id":"810","clientEntryId":"<ingredient UUID>","rootClientEntryId":"<group UUID>","recordDate":"2026-10-17","versionAtCreation":0}]
}
```

Root kinds=SINGLE/DISH/QUICK/WORKOUT; leaf kinds=FOOD_COMPONENT/ACTIVITY_PERIOD. SINGLE/QUICK root and unique leaf may share UUID/id: **one logical entity, not duplicate input**. DISH/WORKOUT roots differ from children. Different physical tables may both have id=1; uniqueness uses `(kind,id)`, not a global numeric-ID set.

Validate matching submissionId/operation, exact root UUID set, exact leaf set/root membership, no extra/missing leaves, matching dates, new version=0, unique same-level UUIDs and compatible kinds/root types. Both server/frontend validate; HTTP201 alone never clears drafts.

V2 DB result_json stores only schemaVersion/roots/leaves. Envelope time/operation come from receipt columns; replayed is dynamic. Limits match requests (food 50 leaves/20 roots; exercise 12 periods/1 root), without arbitrary additionalProperties. V1 records shape/hash remain unchanged.

## 14.6 Corrections and errors

PUT includes expectedVersion, recordDate, zone, complete root and a new previewFingerprint when calculation changes. It cannot change root kind, owner, creation time or root clientEntryId. New children use new UUIDs; unchanged children retain IDs/UUIDs with server membership checks.

DELETE takes expectedVersion query and returns 204; repeated deletion of the same owned root may idempotently return 204 without exposing other users' records. Retain ApiError: 401/403/404; 409 VERSION_CONFLICT/GROUP_EDIT_REQUIRED/IDEMPOTENCY_CONFLICT/CALCULATION_CHANGED/REFERENCE_INACTIVE; 400 VALIDATION_ERROR; 422 UNSUPPORTED_REFERENCE; 503 CATALOGUE_UNAVAILABLE.

fieldErrors locate leaves by clientEntryId and stable field paths such as `entries.<rootUuid>.ingredients.<leafUuid>.inputGrams`, not array indices that may reorder. Proof-endpoint 404 means currently invisible, not definitely unsaved.

---

# 15. Java implementation steps and method responsibilities

## 15.1 New services and actual reuse points

|File (under com.fitnutri.backend)|Methods/responsibilities|
|---|---|
|foodreference/FoodSmartSearchService.java|`search(query,facets,page,size,owner?)`: aliases+FTS+reviewed facets+stable ranking|
|foodreference/FoodSearchMetadataImporter.java|Validate UX mappings/source keys/review status; import new revisions without changing nutrition|
|records/food/RecipePortionCalculator.java|`normalisePortion()`/`calculateDish()`: rational fractions/final rounding|
|records/food/FoodEntryPreviewService.java|New/owned-correction previews, leaf aggregation/fingerprints, no record writes|
|records/food/FoodEntryService.java|`confirmBatchV2()`: no outer @Transactional; SubmissionService owns the outermost transaction|
|records/food/FoodEntryMutationService.java|`updateOwnedRoot()`/`deleteOwnedRoot()`: parent versions/atomic subtrees|
|records/food/FoodEntryReadService.java|`listRoots()`/`getOwnedRoot()`/`recent()`: avoid flattening before incorrect pagination|
|activity/ExerciseOptionService.java|Options/movements, allowlist validation, immutable mapping locks|
|records/workout/WorkoutPreviewService.java|Existing dated estimates per period or explicit missing mapping; overall fingerprint|
|records/workout/WorkoutService.java|Atomic Quick/Workout, periods/movements creation|
|records/workout/WorkoutMutationService.java|Parent optimistic lock, affected-period recalculation, group soft deletion|
|records/workout/WorkoutReadService.java|Quick/Workout root reads/recents and group-detail boundaries|
|submission/CanonicalSubmissionV2Hasher.java|Canonical request hash from supplied input only, no live-data lookup|
|submission/SubmissionService.java|Add `withSubmissionV2()`/V2 reads; share owner transactions while preserving V1|
|submission/SubmissionV2ProofValidator.java|Complete typed roots/leaves identity validation|
|summary/SummaryRepository.java and related files|Continue leaf sums; add userEntryCount; read after transactional correction|

These are proposed paths. Reuse equivalent existing modules instead of duplicating them. Controllers/DTOs remain thin wrappers; task cards identify file groups.

## 15.2 Complete food-batch creation sequence

1. Strict DTO Bean Validation checks shape, IDs, limits, gram format, mutually exclusive portion input and string length; reject unknown fields.
2. CanonicalSubmissionV2Hasher normalises schema version, operation, external owner namespace, root/leaf UUIDs, source, date/zone, references, input grams, ratio, name/meal, selectionKind and order; exclude returned nutrition/system time.
3. Enter withSubmissionV2 under one TransactionTemplate; lock current user and check USER/enabled.
4. **Check existing receipt first**: same operation/hash returns original proof; different hash returns 409. Retries must not re-evaluate today's profile/inactive references before accepting an already committed request.
5. Validate unique logical root/leaf UUIDs unused by this owner in any previous food/exercise/dish/workout, including soft-deleted records.
6. requireCompletedAdultSetup; validate recordDate/zone; load all references with shared locks sorted by ID; validate provenance/active state.
7. Calculate every SINGLE/DISH leaf from server references and compare preview fingerprints; return item-specific errors with no partial writes.
8. Initialise/persist all parents; bind leaves to parent, common date/meal, raw grams+ratio policy and personal nutrition snapshots; SINGLE parent=null.
9. Batch-save/flush leaves, construct minimal typed proof, validate counts/membership/versions/UUIDs, populate receipt result_json and commit.
10. Build HTTP201 only after TransactionTemplate returns. Any exception rolls back parents/leaves/temporary receipt; never return a partial-success list.

Do not commit the first dish before step 7, call old HTTP endpoints in foreach, or use separate REQUIRES_NEW transactions for roots/ingredients.

## 15.3 Workout creation sequence

Use the same receipt/owner framework; canonical hashes include option IDs, mapping revision identities, period minutes, optional details, movement selections and all input values.

Within locks, obtain dated estimates per period through existing ExerciseEstimateService so profile/dated-weight checks remain intact. For absent mappings, use an explicit no-reference preview/validation path returning complete unavailability; do not pass null and pretend the existing service supports it.

All periods use consistent dated weight in one transaction snapshot. Each new period creates one exercise_record; movements only enter the detail table. QUICK root is its period; WORKOUT has a separate parent. Total minutes are validated/returned, never inserted as another whole-workout exercise_record.

Reject mismatches between option/permitted details/strength kind. Client equipment names do not determine official types. New records require active loaded mappings; historical correction exceptions are strictly established through owner/original record.

## 15.4 Updates and deletions

Correction services use @Transactional: lock owner→owned root→expectedVersion→current subtree. Every supplied existing child must belong to the root; new children must be conflict-free. Soft-delete omitted children without releasing UUIDs, insert new children normally and preserve unchanged snapshots.

Update parent/changed leaves and flush once; increment parent version exactly once. Movement-detail-only edits must also change the parent version to prevent silent concurrent overwrites. A true no-op returns current DTO without increasing version.

Add group-child guards after ownership verification in V1 FoodRecordService.updateOwned/softDeleteOwned and equivalent ExerciseRecordService methods, returning 409 GROUP_EDIT_REQUIRED. Every backend write path enforces this; disabling buttons is insufficient.

Correction previews may reuse the original immutable inactive source; replacements require active references. Date/portion changes do not propagate to other history. Original creation proof remains unchanged because it proves creation, not current state.

## 15.5 Recent and grouped-list queries

Filter SQL by owner/nondeleted. Paginate a union of root identities first, then batch-load children/references in one/two queries, avoiding N+1/join fanout. Sort recordDate desc, createdAt desc, then stable kind/id; recents use createdAt desc and deduplicate specific single references. Unauthorised group metadata/details return 404 without leaking existence.

Retain old RecordReadService flat queries for compatibility/internal leaf statistics. V2 lists use roots as authority; never infer whole-period statistics from one loaded page.

---

# 16. Saving, retrying, leaving and refresh: respect final choices

## 16.1 Save state differs from read state

If server commit succeeds but Dashboard GET fails, show Saved + Retry views and allow a new draft; do not POST again. For unknown writes, freeze original payload/ID in memory; do not modify input while keeping the submissionId. Check/retry the original request first.

After definite uncommitted rejection, changed input gets a new submissionId; root/leaf client IDs may remain because no successful record exists. Do not automatically change IDs after an idempotency conflict; check the original outcome first to avoid duplicate writes.

Mismatched proof, missing leaves or unexpected HTML200 means unknown, retaining draft. Receipt404 may mean still running, not permission to unfreeze a new request. User refusal of browser recovery does not authorise deleting server receipts.

## 16.2 No pending content persisted across refresh

Never persist unconfirmed foods, recipes, exercise drafts, frozen payloads, pending submissionIds or mutation intents in localStorage, sessionStorage, IndexedDB, URL queries, window.name, ServiceWorker/Background Sync.

After hard refresh, only the real session restores login; show an empty editor/entry point and let users check History. **Do not secretly retain even a minimal submission identifier for reliability**; the user explicitly rejected that exception.

Valid saved-record History URLs may contain root kind/id/date; these are not pending recovery state. Do not indiscriminately delete session cookies or existing nonbusiness UI preferences.

## 16.3 Warnings and SPA navigation

- No warning within one Entry↔Review↔Change reference flow.
- Leaving a business draft for Dashboard/History or switching type prompts Keep editing/Discard & leave; confirmation clears draft/undo.
- During saving, in-app buttons indicate waiting; do not claim browser closure can be prevented.
- For unknown state show `The save may already have completed. Leaving won't cancel it. Check History before recording it again.`; confirmed departure clears memory without recovery cards.
- Use beforeunload as best-effort refresh/close warning; browsers control native wording. Lack of a browser warning does not permit hidden draft persistence.
- Do not POST on unload, create records through beacons or retry in the background.

## 16.4 Unknown correction/deletion outcomes

Before refresh, freeze original root/id/version and canonical intent in memory. For unknown PUT, GET the owned root: increased version with matching content confirms correction; changed version with different content requires 409/reload. An unchanged GET version does not prove the PUT cannot finish later. For unknown DELETE, GET404 may show unavailable, not claim certainty that this specific deletion caused it.

After refresh, do not restore mutation intent; users check History. Group/single operations share this intent state, and correction retry must not become a new create.

# 17. Integration with Dashboard, History, media entry and permissions

## 17.1 One nutrition and duration ledger

Statistics currently use valid food_record/exercise_record leaves; retain that basis:

```text
Single food → 1 food_record
Dish        → 1 food_dish_record (group) + N food_record (actual intake snapshots)
Quick       → 1 exercise_record
Workout     → 1 workout_record (group) + N exercise_record (non-overlapping periods)
Movements   → N workout_movement_log (details only, no extra minutes/energy)
```

SummaryRepository sums intake, exercise estimates and minutes from leaves once; never UNION parent totals into SUM. Parents hold no independent intake/expenditure ledger. Dish portion/recipe or Workout-period corrections update leaves/root version transactionally; normal summaries see committed data before or after the transaction.

**Keep counting levels explicit without changing existing meanings:**

|Field|Meaning|Example|
|---|---|---|
|foodCount (existing, retain)/foodComponentCount|Food leaf count|Four-ingredient dish + rice =5|
|foodEntryCount (new)|User-facing root count|The example above =2|
|exerciseCount (existing, retain)/activityPeriodCount|Activity-period count|Strength50min + cardio20min =2|
|exerciseEntryCount (new)|Quick/Workout root count|One Workout above =1|
|movementCount (optional detail)|Strength-movement count|3 movements do not change 70 minutes|

Preserve existing summary-client compatibility when extending SummaryModels/DTO/generated contracts. Each V2 root explicitly supplies componentCount/periodCount; do not infer counts from lazily loaded children. Dashboard's summary-count/list-length check must compare matching units: root lists against foodEntryCount/exerciseEntryCount, not five ingredients against two cards.

Separate HTTP responses do not inherently share one database snapshot. Invalidate/refetch relevant queries after mutation. Temporary inter-request version differences show syncing/retry, not save failure or manually adjusted numbers. An overview response may add generatedAt if needed, but timestamps alone do not prove cross-request consistency.

## 17.2 Dashboard: readable charts before text

Target DOM order: Header → date/primary Add → metric cards → **full-width trend chart** → rule-based text summary → food/exercise root lists. Large screens may place the lists side by side; do not squeeze the main trend beneath a text summary in a narrow sidebar.

Refactor components/IntakeChart.tsx, currently handwritten SVG without numeric Y ticks. Share Recharts-based components/TimeSeriesChart.tsx between Dashboard/History wrappers; do not assume existing charts already use one implementation.

- Default height: desktop360px, small screens260px; parent min-width:0 and responsive width. These are engineering defaults.
- Adapt X ticks to range while keeping endpoints identifiable; short ranges show each day.
- Show numeric Y ticks and explicit Energy (kJ), Energy (kcal) or Minutes labels/legends; never mix dimensions on an unlabelled shared axis.
- Tooltips show full date/value/unit; provide an expandable equivalent data table for keyboard/non-mouse users.
- Keep null gaps with connectNulls=false. Distinguish real0 from no record/unestimable data; all-null displays a brief empty state, not a fabricated zero line.
- Food energy and recorded exercise energy may be separate clearly named series, never a complete daily calorie balance.
- Retain theme colours, using legends/line styles rather than random rainbow colours or colour-only meaning.
- Share unit formatters across ticks/tooltips/metrics; do not mix rounded kcal and kJ.

## 17.3 History: filters/trends before records

DOM order: date/type filters → key statistics → **full trend chart** → roots grouped by date/meal → pagination. Reuse HistorySummary, PeriodTrends and historySearch.ts without replacing all existing filter-URL state.

Paginate complete V2 roots; dishes/workouts cannot split across pages. Root cards:

- SINGLE: short name, grams, meal, portion nutrition; View/Edit/Delete/Log again.
- DISH: user name, personal mode/current fraction, ingredient count, personal intake totals; expansion distinguishes **whole-recipe input from actual intake**, never placing recipe grams in an eaten column.
- QUICK: chosen activity, minutes, expenditure/unavailable; optional equipment details collapsed.
- WORKOUT: title, total minutes, available expenditure/incompleteness, period count; expand periods/strength movements without separate movement-energy bars.

View saved records uses receipt kind/id/date; backdated saves set a range containing that date. For multiple roots across meals, group by actual meal and highlight every saved root. Toast dismissal must not remove View records access; saved context only needs app memory, not refresh recovery.

If filtering Workouts by category/option, return the complete workout containing a match and label it accordingly; card totals remain whole-workout totals. If statistics cover the date range, explicitly label all records in selected dates so users do not infer subactivity filtering. In version one only date and FOOD/EXERCISE affect summaries; advanced catalogue filters apply to option search, not ambiguous historical subactivity statistics.

## 17.4 Preserve existing image/speech paths

Adapt imageDraft.ts, voiceDraft.ts and schemas to new draft types:

1. Convert image/speech candidates to SINGLE by default. Without a verifiable model-provided recipe, never automatically generate DISH, ingredient amounts or fractions.
2. Users may explicitly organise selected entries into dishes through personal/whole-recipe mode and portion steps; version one does not require automatic AI grouping.
3. After removing Confirm checkboxes, valid selected references still require user selection/explicit acceptance. Model names/confidence are not valid referenceIds. Even a unique reliable candidate follows confirmed product rules and never auto-saves.
4. Explicit spoken grams may prefill editable candidates. The blank-grams rule applies to historical reuse, not amounts spoken for this entry. Unknown portions require grams; do not add bowl/cup conversion this round.
5. Meal comes from the user's explicit default or spoken content, not recording time; resolve conflicts inline.
6. Spoken exercise initially becomes QUICK. Without explicit multiple non-overlapping periods, do not invent a Workout or double-count one sentence. Users may switch to Gym and organise periods.
7. Share nutrition/exercise previews, confirmation, idempotency, ownership and failure rules; do not revert to mock stores for the new UI.
8. Retain all existing recording-cancel, image-type/size, provider-timeout, microphone-denial and manual-fallback tests.

## 17.5 Q&A, administrator and profile regression boundaries

QaContextBuilder continues using authorised server summaries of confirmed records. New root counts may enter permitted summaries when needed, but do not send full recipes, per-set loads, raw health answers or database IDs by default. Do not send dish totals and ingredient totals separately for model summation.

Do not change login eligibility, administrator roles, questionnaire health questions or target formulas, or add exercise prescriptions. Administrators still manage account access only, without access to others' Workout details. Owner checks cover every new root/child/recent/preview/reference replacement/Log again read; all writes retain CSRF. Profile/weight changes never rewrite saved historical calculation snapshots.

# 18. Interaction quality, errors, performance and accessibility

## 18.1 Explicit states and feedback semantics

|Situation|User sees|Code requirement|
|---|---|---|
|No reference-search results|`No matching foods. Try another name or record by ingredients.`|200 empty collection, distinct from503|
|Approximate matches only|Short name, key distinction, `Approximate reference`|Explicit click, selectionKind recorded; no invented nutrition|
|Reference service unavailable|`Couldn't load food references. Try again.`|Retain input/drafts, no seed fallback|
|Missing grams|`Enter grams`|No zero-nutrition display; Add disabled with visible reason|
|Stale/calculating preview|Loading; old values removed or explicitly stale|Cannot confirm old fingerprint|
|Recordable but unestimable exercise|`Energy estimate unavailable` and brief reason|Count duration; null is not0|
|Definite save failure|Specific editable-field/retry feedback|Retain draft; modify payload only after definite rejection|
|Unknown save outcome|`Check save status` / `Retry safely`|Retain same ID/frozen payload in memory|
|Saved but refresh failed|`Saved. Couldn't refresh the view.`|Retry GET only, never create POST|
|Eating same recipe again|Normal new draft/new ID|No content deduplication of legitimate intake|

## 18.2 Temporary draft boundaries

Never persist business drafts, submission IDs, pending records, mutation intents or recovery markers in localStorage/sessionStorage/IndexedDB/cacheStorage/URL/window.name/service workers. Keep server session cookies/CSRF; do not remove authentication accidentally. Any retained mock pages are isolated fixtures, excluded from production bundles.

Undo affects current drafts only, default8 seconds, not deleted database records. Multiple removals may use a bounded in-memory stack of the last5; Undo targets the explicitly indicated item, never reviving committed/discarded drafts. Clear on save/exit/account switch.

## 18.3 Field validation and focus

- Frontend/API share contract limits. Empty string means unfilled, not0; parse through one DTO adapter, never silently accepting parseFloat('12abc')=12.
- Reject NaN/Infinity/scientific-notation bypasses of precision/SQL limits; schema and Bean Validation agree on cases.
- Single grams retain positive, ≤10000g, ≤3-decimal limits; dish ingredient inputs match. Do not truncate derived fractional grams using input precision.
- Root/dish names are trimmed1–100 characters; arrays follow Chapter14. Long names/JSON must not overflow cards.
- Sets/reps/load are optional and Review remains available without them. Preserve partial valid input without requiring all three. Load0 has movement-dependent meaning and differs from missing.
- Errors identify stable root/ingredient/period UUIDs, expand the relevant card and focus the field.
- Changing default meal affects future additions only, not existing rows/dishes; explicitly changing a dish meal updates the whole dish.
- Enter in quantity fields must not Confirm on the server. Search Enter may select an explicitly highlighted candidate, never default to the first on blur.

## 18.4 Accessibility and visual acceptance

Retain green theme, white cards, fonts and radii. Primary actions are contained, secondary outlined, destructive actions clearly confirmed. Noninteractive metrics lack pointer/fake-hover cues. Icon buttons have names, whole-row detail access supports keyboards, and buttons are not nested.

Align checkboxes/multiline labels to the first line; labels remain clickable and screen-reader associated. Success uses polite status/live announcements; errors do not auto-dismiss after3 seconds. The registration/save success budget includes collapse/fade: recommend2500ms visible + ≤300ms exit. Reduced motion removes/reduces animation without extending delay.

Restore sensible focus/scroll when returning from replacement search or Review. Expanded dishes/Workouts have accurate aria-expanded; closing dialogs restores trigger focus. Never hide the sole saved-record access inside a disappearing toast.

## 18.5 Performance and security defaults

- Search debounce250ms, English minimum2 characters/Chinese minimum1 valid Han character; default5 results/more20. Use cancellation plus response-key checks; cancelling does not guarantee server cancellation.
- Rank relevance before common/recency. Cache keys include query/facets/page/datasetRevision/metadataRevision; owner-specific recents never share public keys.
- Public reference caching is allowed; private data uses existing owner/session epoch. Logout/401 clears private caches and ignores late former-owner responses.
- Do not scan/return all3741 entries per keystroke. Use server pagination, parameters and bounded patterns; full-catalogue reachability is not full download.
- Batch root/child history reads to avoid Hibernate N+1; expand large movement arrays only for details. Record SQL counts/key plans in review; performance claims require measurement.
- Recheck permissions server-side rather than relying on hidden buttons. Retain lock order; concurrent catalogue import/disable versus save requires database tests.
- Keep service domains/API credentials out of client bundles/documents. Mark external outages BLOCKED_EXTERNAL; classroom tests must not automatically incur paid calls.

# 19. Migration, data preparation, staged rollout and rollback

## 19.1 Migration numbering and sequence

The ZIP contains V1–V8; inspect again with find during execution. If the actual personal branch already has V9, advance these numbers consistently in tasks/change logs. **Never modify applied migration content/checksums.** The following describes new SQL responsibilities, not unconditional permission to run against the user's original database.

|Suggested migration|Content|Protection|
|---|---|---|
|V9 `food_search_metadata`|food_search_profile/alias, review states/indexes|Preserve original references/nutrition; unreviewed metadata is not recommended|
|V10 `food_dish_records`|Dish roots, food_record links/order/input grams, precision widening|Old dish=NULL and quantities unchanged|
|V11 `submission_receipt_v2`|V2 operations/minimal-proof validation, retained immutable/complete-commit triggers|Old V1 hashes/proofs remain valid; only new grouped submissions use V2|
|V12 `exercise_options_workouts`|Options/movements/workout roots/movement logs, exercise links, explicit NO_APPLICABLE exception|No historical remapping/grouping or cleared estimates|
|V13 `group_consistency_indexes`|Deferred owner/date/meal/lifecycle triggers, indexes/additional checks|Transactional rollback on failure, no automatic repair of old anomalies|

Update ORM nullability/precision/scale/enums and SQL CHECK together, then run Hibernate validate. Widening food quantity10,3→24,12 preserves precision but **does not guarantee old binaries validate the new schema**. Rehearse old-app compatibility before rollback. If incompatible, disable new entry and fix forward, or restore backups under an explicit recovery plan; do not truncate new data through direct down-migration.

## 19.2 Data preflight script

Add scripts/preflight_ux_v3.py, read-only by default, producing machine-readable JSON and redacted explanations. Environment variables specify isolated/target connections; do not read/distribute secrets from the ZIP.

Check schema version; original counts/sums/nulls/deleted rows/receipt counts/hashes; owner/date/version validity; stable-client-ID conflicts; existing dataset hash/source verification; strings/numbers outside new constraints; maximum widened-quantity range; duplicate index names; equivalent existing table/column changes.

Do not automatically delete duplicates. For substantive conflicts, report traceable row IDs/reasons and stop that migration pending resolution; never log complete private meals/health answers.

## 19.3 Catalogue/search metadata import

Add schema-validated data/ux/food-search-metadata-v1.json, exercise-options-v1.json, movement-catalogue-v1.json and review manifests. Final locations may follow existing resources/data conventions, frozen in P02; do not maintain divergent copies.

Import must support dry-run reporting of totals, available/incomplete/unreviewed entries, orphan references, duplicate aliases, conflicting/unsupported official-name attributes, unjustified METs and option-type mismatches. Aliases/equipment names are product metadata, not original Compendium entries. Seed only when that version does not exist; reruns are idempotent.

Retain scripts/import_* and source locks. New official activity references require **new revisions or immutable rows**, preserving old references; never edit r1 MET/descriptions into another activity. Display options may cover required common items, but each mapping must be SPECIFIC_REFERENCE/GENERAL_ACTIVITY_ESTIMATE/UNAVAILABLE with review rationale. Unsupported items remain recordable with unavailable estimates; do not disable recording or invent METs.

No applicable reference differs from a service failure. Only reviewed mapping_kind=UNAVAILABLE permits saved null; outages return503 and preserve drafts. Do not disguise faults by converting every error to UNAVAILABLE.

## 19.4 Release switches and old-interface compatibility

Reuse capabilities, adding read-only server flags such as entryV2Available, foodSmartSearchAvailable and exerciseOptionsAvailable. New UI uses V2 only when schema/resources are ready. Partial upgrades show explicit unavailability/existing independent entry, never new group payloads through old POST.

Keep switches off until migrations/tests pass; production-default changes belong to the final release commit. Do not hardcode true in JS for demonstrations or return Demo options when catalogues are absent.

Retain V1 create, flat reads, old corrections and proof hashes for existing advanced paths/clients. V1 grouped-child writes return GROUP_EDIT_REQUIRED, preserving parent versions. Existing authentication/session/CSRF URLs remain unchanged.

## 19.5 Rollback rehearsal and deployment evidence

On isolated PG16: capture old baseline → backup → apply V9+ → assert old statistics/records/receipts unchanged → create/correct/delete DISH/WORKOUT → concurrency/fault tests → restore backup into another isolated database → verify schema/original snapshots.

Report backup source/target, restore verification, app compatibility, feature flags, presence of new data and recommended rollback. Do not use docker compose down -v, flyway clean or automatic shared-volume deletion. Real-user migration/restoration requires separate explicit authorisation; this plan never permits destroying existing data.

Correct alias/copy mistakes through new metadata revisions, not historical snapshot changes. Nutrition fixes affect future records or explicit user corrections; bulk historical rewrites require separate impact analysis/authorisation and are not automatic this round.

# 20. Executable code task queue

Every card below is a **task to be executed**. Its direct tests must be written alongside the business code; UX-Q01–Q07 provide additional cross-module verification and are not a reason to defer business unit tests. Paths are relative to the repository root. “New” means the path did not exist in this ZIP; reuse equivalent implementations already on the actual branch and record that reuse.


## 20.1 Effort, dependencies and release order

This round contains **52 task cards**, with an engineering planning estimate of **156–289 person-hours**, including direct unit tests and integration work. This is not an estimate of AI execution time or a guaranteed completion date. Source preparation, differences on the current branch, database migration and real-browser fixes may increase the effort; code-generation speed has not been converted into a guaranteed schedule.

Suggested phases:

|Phase|Deliverable|Main tasks|
|---|---|---|
|A|Branch, current regression baseline, contracts and scaffolding|P00–P03、Q00；the low-risk UX fixes in F00–F02 may be completed first|
|B|Metadata/schema/calculations/receipts and V2 APIs|D00–D04、B00–B15；F03 generates contracts|
|C|Food search, dishes, Review and continuous entry|F04–F09|
|D|Quick/Gym, periods and optional details|F10–F12|
|E|Root history, correction, reuse, recovery without refresh and media compatibility|F13–F16|
|F|Cross-layer regression, migration, browser checks and delivery evidence|Q01–Q07、R00–R01|

The dependency table expresses logical ordering; it does not authorize multiple agents to edit the same working tree concurrently. Optional parallel analysis must be read-only; actual writes remain serial tasks with small commits unless the user explicitly authorizes otherwise.

**Prefer source reuse:** The seven new table categories and V2 aggregate contracts are the selected implementation for dishes/Workouts and compatibility with old receipts, not a rebuild of the entire system. Reuse equivalent implementations already on the current branch according to this document’s acceptance contract; do not create duplicate tables or parallel statistics systems.


## 20.2 Task summary

|Task|Deliverable|Dependencies|Estimated person-hours|
|---|---|---|---|
|UX-P00|Freeze the actual working tree, branch and source differences|Branch Gate|1–2|
|UX-P01|Establish a comparable current regression baseline|UX-P00|2–4|
|UX-P02|Freeze V2 contracts, models, precision and change registration|UX-P01|3–5|
|UX-P03|Create the task ledger, source mapping and execution gates|UX-P02|1–2|
|UX-Q00|Prepare synthetic fixtures and isolated test scaffolding early|UX-P03|3–5|
|UX-D00|Create reviewable search metadata structures|UX-Q00|2–4|
|UX-D01|Create dish roots and high-precision leaf structures|UX-D00|3–6|
|UX-D02|Version submission-receipt database validation|UX-D01|3–5|
|UX-D03|Create equipment-option, Workout and movement structures|UX-D02|4–7|
|UX-D04|Group constraints, preflight and safe migration tools|UX-D03|3–6|
|UX-B00|Implement search metadata and Chinese/English alias imports|UX-D00|3–6|
|UX-B01|Implement smart search, dynamic disambiguation and full search|UX-B00|4–7|
|UX-B02|Implement recent food/dish reads and reuse context|UX-D01|2–4|
|UX-B03|Implement rational portions and precision-preserving food calculations|UX-D01|3–6|
|UX-B04|Implement V2 previews for singles and dishes|UX-B03, UX-B01|3–5|
|UX-B05|Implement V2 canonical hashes, receipts and replay services|UX-D02, UX-Q00|4–7|
|UX-B06|Implement atomic SINGLE + DISH creation|UX-B04, UX-B05, UX-D04|4–7|
|UX-B07|Implement whole-dish history correction and deletion|UX-B06|4–7|
|UX-B08|Implement Food root queries and complete details|UX-B07, UX-B02|3–5|
|UX-B09|Prepare exercise options, movements and source-mapping catalogues|UX-D03|4–8|
|UX-B10|Expose category, search and movement-query APIs|UX-B09|2–4|
|UX-B11|Implement unified Quick/Workout period previews|UX-B10, UX-B05|4–7|
|UX-B12|Implement atomic Workout and Quick V2 saves|UX-B11, UX-D04|4–7|
|UX-B13|Implement Workout history correction/deletion and legacy-API bypass protection|UX-B12|4–7|
|UX-B14|Implement exercise root lists, details, recent entries and reuse|UX-B13|3–5|
|UX-B15|Unify statistics, counts, detail reads and Q&A context|UX-B08, UX-B14|3–6|
|UX-F00|Dismiss registration success feedback within three seconds|UX-P03|1–2|
|UX-F01|Align the questionnaire and make clickable actions explicit|UX-P03|2–3|
|UX-F02|Rearrange Dashboard/History charts and add axes|UX-F01|3–5|
|UX-F03|Generate V2 types and wrap a strict API client|UX-P02|3–5|
|UX-F04|Refactor drafts into a single/dish/workout union|UX-F03, UX-Q00|4–7|
|UX-F05|Smart search, more results and recent-use UI|UX-F04, UX-B01, UX-B02|3–6|
|UX-F06|Continuous additions, inline edits and draft Undo|UX-F05|4–6|
|UX-F07|Dish editing, portion modes and ingredient-based entry|UX-F06, UX-B04|4–8|
|UX-F08|Simplify shared Review and support inline adjustments|UX-F07|3–6|
|UX-F09|Real food saves, remaining on the page after success and date rule C|UX-F08, UX-B06|4–7|
|UX-F10|Quick exercise categories, recent entries and equipment input|UX-F04, UX-B10, UX-B11|4–6|
|UX-F11|Multi-period Gym, strength movements and optional sets/reps/load|UX-F10|4–8|
|UX-F12|Exercise Review and remaining on the page after a successful real save|UX-F11, UX-B12, UX-F09|3–6|
|UX-F13|Dashboard/History root cards, details and counts|UX-F02, UX-B08, UX-B14, UX-B15|4–7|
|UX-F14|Group history correction, whole-group deletion and Log again|UX-F13, UX-B07, UX-B13|4–8|
|UX-F15|Precisely implement leave warnings and no restoration after refresh|UX-F09, UX-F12, UX-F14|2–4|
|UX-F16|Migrate image/speech candidates and regress existing advanced features|UX-F04, UX-F08, UX-F12|3–6|
|UX-Q01|Complete real-API and browser acceptance for food/dishes|UX-F09, UX-F13, UX-F14, UX-F15|3–6|
|UX-Q02|Quick/Gym acceptance for real periods, details and history|UX-F12, UX-F13, UX-F14, UX-F15|3–6|
|UX-Q03|Statistics conservation and multi-root/multi-day regression|UX-B15, UX-F13, UX-Q01, UX-Q02|2–4|
|UX-Q04|Concurrency, lost responses and recovery-without-refresh boundaries|UX-B06, UX-B12, UX-F15|3–6|
|UX-Q05|Permissions, legacy APIs and advanced-service compatibility regression|UX-F16, UX-B07, UX-B13|3–5|
|UX-Q06|Isolated database migration and backup/restore rehearsal|UX-D04, UX-B15|3–6|
|UX-Q07|Accessibility, visual and basic-performance regression|UX-F02, UX-Q01, UX-Q02|2–5|
|UX-R00|Update runtime instructions, contracts and user-acceptance scripts|UX-Q03, UX-Q04, UX-Q05, UX-Q06, UX-Q07|2–4|
|UX-R01|Final self-review, branch commits and handover receipt|UX-R00|1–3|


### UX-P00｜Freeze the actual working tree, branch and source differences

**Dependencies:** Pass the Branch Gate first
**Estimate:** 1–2 person-hours; status TODO
**Requirements:** All
**Backend/database changes:** None

**Files:**
- `docs/implementation/ux-v3-baseline.md` — Planned new path; reuse an equivalent implementation if present

**Key functions/responsibilities:** `Read-only Branch Gate; record ZIP_SHA, ARCHIVED_HEAD, actual HEAD and pre-existing diff`

**Steps:**
1. Check the actual working tree under Chapter 1 and verify develop/yuankang-wei exactly; do not write the baseline file until the gate passes.
2. Compare the key classes, DTOs, migrations and scripts in this ZIP; do not treat bundled dependencies/test caches as an installed environment.
3. Record differences from the archive and existing functionality. Map later tasks to equivalent actual implementations without overwriting others’ work.

**Tests alongside this task:**
- Save read-only Git output and key source SHAs; verify that no remote commands ran and that no unauthorized action preceded project-diff writes.

**Completion criteria:** Branch Gate PASS; source differences and ownership of existing changes recorded; tasks initially NOT_STARTED.

**Suggested commit:** `docs(ux-p): ux-p00 implementation and verification [UX-P00]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-P01｜Establish a comparable current regression baseline

**Dependencies:** UX-P00
**Estimate:** 2–4 person-hours; status TODO
**Requirements:** Regression
**Backend/database changes:** None

**Files:**
- `docs/verification/ux-v3-baseline-results.md` — Planned new path; reuse an equivalent implementation if present
- `scripts/check.sh` — Existing; modify incrementally/reuse
- `frontend/package.json` — Existing; modify incrementally/reuse
- `backend/pom.xml` — Existing; modify incrementally/reuse

**Key functions/responsibilities:** `Check existing test-discovery rules, Node/Java/PG environments, source verification and browser configuration`

**Steps:**
1. Run the available baseline layer by layer with current scripts; record actual counts and blockers.
2. Do not first modify business code to fix historical failures unrelated to this round; distinguish existing baseline failures from newly introduced regressions.
3. Recognize that 97 Java files is only a file count; obtain actual Surefire/Failsafe counts and record that external models were not called.

**Tests alongside this task:**
- Existing Java/React/schema/boundary tests; mark layers with unavailable dependencies or PostgreSQL BLOCKED, not PASS.

**Completion criteria:** Explain tested and untested scope for each layer, enabling later before/after comparison.

**Suggested commit:** `docs(ux-p): ux-p01 implementation and verification [UX-P01]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-P02｜Freeze V2 contracts, models, precision and change registration

**Dependencies:** UX-P01
**Estimate:** 3–5 person-hours; status TODO
**Requirements:** All
**Backend/database changes:** UX-BC01–14

**Files:**
- `docs/api/openapi-mvp.yaml` — Existing; modify incrementally/reuse
- `docs/implementation/ux-v3-backend-changes.md` — Planned new path; reuse an equivalent implementation if present
- `docs/architecture/ux-v3-record-roots.md` — Planned new path; reuse an equivalent implementation if present

**Key functions/responsibilities:** `FoodEntryInputV2 / ExerciseEntryInputV2 / PortionInput / ReceiptV2; root/leaf relationship ADR`

**Steps:**
1. Document one selected solution under Chapters 13–14; do not leave downstream implementers to guess among three mutually exclusive table designs.
2. Define const+anyOf schemas, string IDs, null policy for optional fields, limits, errors and legacy compatibility.
3. Register the rationale, affected services, migrations, tests and recovery plan for all 14 BCs; declare new engineering defaults.

**Tests alongside this task:**
- Run the OpenAPI validator and trial generation; cross-review tables/DTOs/rules without claiming unimplemented endpoints are callable.

**Completion criteria:** Contracts cover every user choice without ambiguity in amount/nutrition/duration semantics.

**Suggested commit:** `docs(ux-p): ux-p02 implementation and verification [UX-P02]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-P03｜Create the task ledger, source mapping and execution gates

**Dependencies:** UX-P02
**Estimate:** 1–2 person-hours; status TODO
**Requirements:** Development standards
**Backend/database changes:** None

**Files:**
- `docs/implementation/ux-v3-tasks.md` — Planned new path; reuse an equivalent implementation if present
- `docs/verification/ux-v3-test-matrix.md` — Planned new path; reuse an equivalent implementation if present
- `docs/verification/ux-v3-results.md` — Planned new path; reuse an equivalent implementation if present

**Key functions/responsibilities:** `Task → requirement → BC → code → test → commit traceability; independent status for each stage`

**Steps:**
1. Mark every task in this chapter NOT_STARTED; do not copy DONE from old documents.
2. Verify the personal branch before starting each code change/commit; order planned tasks for safe serial execution in one working tree.
3. Agree on actual test-log paths, sensitive-data redaction and handling of external-service blockers.

**Tests alongside this task:**
- Verify that the ledger includes every task ID, all 14 BCs and all test IDs in this document, with no dependency cycles.

**Completion criteria:** AI can select tasks by dependencies; code self-review/peer review/UAT fields remain distinct.

**Suggested commit:** `docs(ux-p): ux-p03 implementation and verification [UX-P03]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-Q00｜Prepare synthetic fixtures and isolated test scaffolding early

**Dependencies:** UX-P03
**Estimate:** 3–5 person-hours; status TODO
**Requirements:** Calculations/transactions/permissions
**Backend/database changes:** UX-BC04–11

**Files:**
- `backend/src/test/java/com/fitnutri/backend/support/UxV3Fixtures.java` — Planned new path; reuse an equivalent implementation if present
- `frontend/tests/fixtures/entryV2.ts` — Planned new path; reuse an equivalent implementation if present
- `scripts/check.sh` — Existing; modify incrementally/reuse

**Key functions/responsibilities:** `syntheticFoodRefs(); usersAndDatedWeights(); draftV2Fixture(); receiptV2Fixture()`

**Steps:**
1. Extend existing PG16 test infrastructure instead of introducing another database or test framework.
2. Create Chapter 11 fixtures with a fixed Clock, rational portions, 70 kg exercise inputs and a second-user unauthorized-access scenario.
3. Define unit/slice/IT/E2E discovery rules; default CI must not skip new tests or provider-double tests.

**Tests alongside this task:**
- FixtureSmokeTest: fixed inputs and references are consistent; Node/Java produce the same contract example; no real provider calls.

**Completion criteria:** Business tasks can write runnable deterministic tests from the outset.

**Suggested commit:** `test(ux-q): ux-q00 implementation and verification [UX-Q00]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-D00｜Create reviewable search metadata structures

**Dependencies:** UX-Q00
**Estimate:** 2–4 person-hours; status TODO
**Requirements:** F01–F06
**Backend/database changes:** UX-BC01

**Files:**
- `backend/src/main/resources/db/migration/V9__food_search_metadata.sql` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/foodreference/search/FoodSearchProfile.java` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/foodreference/search/FoodSearchAlias.java` — Planned new path; reuse an equivalent implementation if present

**Key functions/responsibilities:** `food_search_profile / food_search_alias; active reviewed revision constraints`

**Steps:**
1. Confirm the next migration number at execution time; create profiles and concept aliases pointing to real immutable references.
2. Define review status, a unique active revision, metadata provenance and indexes; do not modify original AUSNUT rows.
3. An ambiguous alias may return multiple references; do not force one Chinese term to bind uniquely to one nutrition record.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/foodreference/search/SearchMetadataSchemaIT.java: no orphan references/no unreviewed recommendations/no duplicates on repeat import.

**Completion criteria:** All original foods remain searchable; metadata tables support independent incremental maintenance.

**Suggested commit:** `feat(ux-d): ux-d00 implementation and verification [UX-D00]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-D01｜Create dish roots and high-precision leaf structures

**Dependencies:** UX-D00
**Estimate:** 3–6 person-hours; status TODO
**Requirements:** D01–D14
**Backend/database changes:** UX-BC04

**Files:**
- `backend/src/main/resources/db/migration/V10__food_dish_records.sql` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodDishRecord.java` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/records/FoodRecord.java` — Existing; modify incrementally/reuse

**Key functions/responsibilities:** `food_dish_record; dish_id, input_grams, position, quantity_grams(24,12)`

**Steps:**
1. Create user-owned historical occurrence roots, not global recipe templates; store rational portions and display inputs.
2. Extend existing food_record without a duplicate intake-energy store for statistics; preserve NULL dish_id on old rows.
3. Apply Chapter 13 quantity precision, constraints, update timestamps and version; adjust schema-validation expectations.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/food/DishSchemaIT.java: old values unchanged, 1/3-portion precision, cross-owner child rejection and independent historical occurrences.

**Completion criteria:** Dishes remain persistently grouped without changing old independent records; all extensions have migration tests.

**Suggested commit:** `feat(ux-d): ux-d01 implementation and verification [UX-D01]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-D02｜Version submission-receipt database validation

**Dependencies:** UX-D01
**Estimate:** 3–5 person-hours; status TODO
**Requirements:** F18/reliable saves
**Backend/database changes:** UX-BC06

**Files:**
- `backend/src/main/resources/db/migration/V11__submission_receipt_v2.sql` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/submission/SubmissionOperation.java` — Existing; modify incrementally/reuse

**Key functions/responsibilities:** `FOOD_BATCH_V2 / EXERCISE_ENTRY_V2; strict v1/v2 proof SQL validators`

**Steps:**
1. Extend receipts in the separate next V11 migration; do not revise UX-D01’s V10. If a number is already occupied at execution time, shift the sequence consistently.
2. Preserve existing triggers forbidding modification/deletion of committed receipts and requiring completeness before transaction commit.
3. V2 constrains typed roots/leaves, property allowlists, counts and group relationships; do not store recipe/private workout details.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/submission/ReceiptSchemaV2IT.java: V1 remains valid; excessive counts/extra fields/missing children rejected; committed receipts immutable.

**Completion criteria:** New group receipts can be stored with complete compatibility for original proof and replay semantics.

**Suggested commit:** `feat(ux-d): ux-d02 implementation and verification [UX-D02]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-D03｜Create equipment-option, Workout and movement structures

**Dependencies:** UX-D02
**Estimate:** 4–7 person-hours; status TODO
**Requirements:** E1–E8
**Backend/database changes:** UX-BC08–10

**Files:**
- `backend/src/main/resources/db/migration/V12__exercise_options_workouts.sql` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/activity/catalogue/ExerciseOption.java` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/activity/catalogue/MovementCatalogue.java` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/records/workout/WorkoutRecord.java` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/records/workout/WorkoutMovementLog.java` — Planned new path; reuse an equivalent implementation if present
- `backend/src/main/java/com/fitnutri/backend/records/ExerciseRecord.java` — Existing; modify incrementally/reuse

**Key functions/responsibilities:** `Separate product options from energy references; root/period/movement relationships`

**Steps:**
1. Create option/movement catalogues with immutable versions and reviewed mappings; movements have no MET.
2. Keep exercise_record as the sole minutes/energy ledger; extend it with workout relationships, option snapshots and controlled equipment parameters.
3. Allow NO_APPLICABLE_REFERENCE only for V2 options with a reviewed UNAVAILABLE mapping; do not globally remove the old valid-reference rule.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/workout/WorkoutSchemaIT.java: movement tables have no duration; invalid group/source/status combinations rejected; old exercises not rewritten.

**Completion criteria:** Strength movements support detailed records without accidentally contributing to duration/energy totals.

**Suggested commit:** `feat(ux-d): ux-d03 implementation and verification [UX-D03]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-D04｜Group constraints, preflight and safe migration tools

**Dependencies:** UX-D03
**Estimate:** 3–6 person-hours; status TODO
**Requirements:** Compatibility/safety
**Backend/database changes:** UX-BC04/06/09/14

**Files:**
- `backend/src/main/resources/db/migration/V13__group_consistency_indexes.sql` — Planned new path; reuse an equivalent implementation if present
- `scripts/preflight_ux_v3.py` — Planned new path; reuse an equivalent implementation if present
- `scripts/tests/test_preflight_ux_v3.py` — Planned new path; reuse an equivalent implementation if present

**Key functions/responsibilities:** `validate group owner/date/meal/lifecycle; dry-run report; parent/leaf indexes`

**Steps:**
1. Add deferrable cross-table consistency constraints, allowing root updates before child updates in the same transaction but requiring consistency at commit.
2. Read-only preflight reports old-data counts and aggregate/proof checks; no automatic duplicate deletion.
3. Record migration numbers, backups and old-binary validation compatibility boundaries; compare equivalent existing database structures first.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/GroupConstraintsIT.java: any cross-owner/date/meal/orphan-child update rejected; preflight unit tests.

**Completion criteria:** Invalid data blocks migration/commit instead of being silently lost; the rollback path can be explained.

**Suggested commit:** `feat(ux-d): ux-d04 implementation and verification [UX-D04]`; replace the generic phrase in the actual commit title with a concrete English action. The body must include Why/BC/DB/Tests/Limitations; never mark unexecuted tests PASS.


### UX-B00｜Import Search Metadata and Chinese/English Aliases

**Dependencies:** UX-D00
**Estimate:** 3–6person-hours; status TODO
**Requirements:** F01–F06
**Backend/Database changes:** UX-BC01

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/foodreference/search/FoodSearchMetadataImporter.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/resources/data/ux/food-search-metadata-v1.json` — Proposed new path; reuse equivalent implementation if found
- `scripts/validate_ux_catalogues.py` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `validateMetadata(); importRevision(); deriveReviewedFacetsOnly()`

**Execution steps:**
1. Link every item to a real reference; cover chapter6 frequent groups first, keeping official search for complex unannotated entries.
2. Maintain displayName/aliases/concept/facets/common flags; do not activate guessed attributes without human/evidence review.
3. Reviewable CSV/JSON diff: original name→short name→key conditions→evidence; no nutrition copied into metadata.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/foodreference/search/FoodMetadataImporterTest.java: negated added-fat conditions, repeat imports, ambiguous terms, orphan IDs.

**Completion conditions:** Every simplified candidate traces to one reference; unknown never defaults to no oil.

**Suggested commit:** `feat(ux-b): ux-b00 implementation and verification [UX-B00]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B01｜Smart Retrieval, Dynamic Distinctions, and Complete Search

**Dependencies:** UX-B00
**Estimate:** 4–7person-hours; status TODO
**Requirements:** F01–F06
**Backend/Database changes:** UX-BC02

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/foodreference/search/SmartFoodSearchService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/foodreference/search/SmartFoodSearchController.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/foodreference/FoodReferenceService.java` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `search(query,facets,mode,page,size); rankExactThenFacetsThenCommon(); suggestDisambiguation()`

**Execution steps:**
1. Preserve old search; new endpoint returns candidate kind/short name/necessary distinctions/current page/total matches.
2. Word boundaries and phrases first; rice must not match licorice substrings; Chinese uses reviewed aliases, not assumed full translation.
3. Ask only remaining ambiguity supported by data; explicit attributes outrank recent/common; all fallback official results reachable.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/foodreference/search/SmartFoodSearchTest.java: exact/ambiguous/empty/oil conditions/relevant-recent ranking/stable pagination; Controller slices.

**Completion conditions:** No fabricated recommendations or hidden rare valid references; deterministic testable ranking.

**Suggested commit:** `feat(ux-b): ux-b01 implementation and verification [UX-B01]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B02｜Recent Food/Dish Reads and Reuse Context

**Dependencies:** UX-D01
**Estimate:** 2–4person-hours; status TODO
**Requirements:** F07/D10–D12
**Backend/Database changes:** UX-BC03/07

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodRecentService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodRecentController.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `recentSinglesAndDishes(owner,limit); lastSuccessfulUsage(); detailForReuse()`

**Execution steps:**
1. Current user's confirmed undeleted records only, ordered by createdAt not backdated date; deduplicate single references.
2. Separate dish roots; ingredients do not become ordinary recent meals; same-name dishes do not merge into templates.
3. Return previous grams/recipe as hints and active state; no new/changed records or other users' content.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/food/FoodRecentIT.java: backdate ordering, deletion filtering, independent same-name dishes, cross-user, inactive references.

**Completion conditions:** Frontend reuses exact source but previous portions require explicit action.

**Suggested commit:** `feat(ux-b): ux-b02 implementation and verification [UX-B02]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B03｜Rational Portions and Precision-Preserving Calculation

**Dependencies:** UX-D01
**Estimate:** 3–6person-hours; status TODO
**Requirements:** D04–D08
**Backend/Database changes:** UX-BC05

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/food/PortionNormalizer.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/food/DishCalculationService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/foodreference/FoodCalculationService.java` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `normalisePortion(); calculateScaledReference(); sumRoundedLeaves()`

**Execution steps:**
1. Servings/fractions retain integer rationals; percentages convert exactly at specified precision, never1/3→33.3%.
2. Compute high-precision nutrition directly from inputGrams/rational; final leaves3 decimals HALF_UP; consumedGrams12 decimals, never reverse-calculate nutrition from it.
3. Reuse reference checks/legacycalculator; historicalinactive only for current linked reference, never new-reuse active bypass.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/food/PortionNormalizerTest.java
- backend/src/test/java/com/fitnutri/backend/records/food/DishCalculationTest.java: chapter11 1700kJ example, zero/negative/excess/duplicate portion fields.

**Completion conditions:** Independently recomputable personal intake, no full-recipe/personal confusion.

**Suggested commit:** `feat(ux-b): ux-b03 implementation and verification [UX-B03]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B04｜Single/Dish V2 Preview

**Dependencies:** UX-B03, UX-B01
**Estimate:** 3–5person-hours; status TODO
**Requirements:** F14/D08
**Backend/Database changes:** UX-BC05

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodEntryPreviewService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodEntryV2Controller.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `previewNewBatch(); previewOwnedRoot(); fingerprintCalculationInputs()`

**Execution steps:**
1. New batch preview checks root/leaf limits/date/reference, returns input/consumed snapshots and totals.
2. Historical preview first checks owner/version; original inactive references follow explicit rules, replacements must be active.
3. Fingerprint calculation-affecting reference revision/quantity/portion; client hash cannot replace server result; no record writes.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/food/FoodEntryPreviewIT.java: zero writes, portion invalidation, other-user IDs rejected.

**Completion conditions:** Review totals share calculation source with future save; errors locate leaf UUIDs.

**Suggested commit:** `feat(ux-b): ux-b04 implementation and verification [UX-B04]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B05｜V2 Canonical Hash, Receipts, and Replay

**Dependencies:** UX-D02, UX-Q00
**Estimate:** 4–7person-hours; status TODO
**Requirements:** Reliable saving
**Backend/Database changes:** UX-BC06

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/submission/CanonicalEntryHasherV2.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/submission/SubmissionService.java` — Existing; incrementally modify/reuse
- `backend/src/main/java/com/fitnutri/backend/submission/SubmissionReceiptV2Response.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/submission/SubmissionV2Controller.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `hashFoodV2(); hashExerciseV2(); withSubmissionV2(); validateTypedProof()`

**Execution steps:**
1. Add typed strategy inside existing TransactionTemplate/ownerlock/immutablereceipt, no parallel idempotency table.
2. Preserve V1 byte behavior/test vectors; fixed V2 canonical prefix/strict fields; same ID/different content409.
3. Check receipts before current active/date/weight so valid old requests replay accurately; different ID is legitimate new record.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/submission/CanonicalEntryHasherV2Test.java
- backend/src/test/java/com/fitnutri/backend/submission/SubmissionV2IT.java: UUID hierarchy, same numeric IDs across tables, original hash unchanged, one replayed creation.

**Completion conditions:** Clearing drafts requires complete typedproof; unproven saves never claim success.

**Suggested commit:** `feat(ux-b): ux-b05 implementation and verification [UX-B05]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B06｜Atomic SINGLE+DISH Creation

**Dependencies:** UX-B04, UX-B05, UX-D04
**Estimate:** 4–7person-hours; status TODO
**Requirements:** F18/D09
**Backend/Database changes:** UX-BC04/05/06

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodEntryV2Service.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodDishRepository.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/FoodRecordService.java` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `createBatchV2(); persistDishRootAndLeaves(); buildProofFromFlush()`

**Execution steps:**
1. Chapter15 order: canonical→ownerlock→receipt→whole-group validation→reference locks→calculation→roots/leaves→proof.
2. Reuse food_record leaves, no two-level double counting; ordinary items/multiple dishes succeed or fail together.
3. Register root/leaf UUID relationships against duplicates; constraint failures roll back batch; outer @Transactional must not violate SubmissionService assumptions.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/food/FoodEntryBatchV2IT.java: later ingredient failure leaves no earlier residue, concurrent duplicates, reject client nutrition.

**Completion conditions:** Real PG proves group atomicity/idempotency and complete V2 receipts.

**Suggested commit:** `feat(ux-b): ux-b06 implementation and verification [UX-B06]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B07｜Whole-Dish Historical Correction and Deletion

**Dependencies:** UX-B06
**Estimate:** 4–7person-hours; status TODO
**Requirements:** D12–D14
**Backend/Database changes:** UX-BC07

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodEntryMutationService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/RecordCorrectionController.java` — Existing; incrementally modify/reuse
- `backend/src/main/java/com/fitnutri/backend/records/RecordMutationGuard.java` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `updateOwnedDish(); softDeleteOwnedDish(); requireUngroupedForLegacyMutation()`

**Execution steps:**
1. Parent version is group token; validate child ownership/UUID; new child new ID, removed child soft-deleted, one root transaction.
2. Propagate date/meal through group; recipe/ingredient/portion changes this root only; metadata-only corrections preserve snapshots.
3. V1 grouped-child writes409 GROUP_EDIT_REQUIRED; GET cannot leak parent owner; failure preserves original group.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/food/DishMutationIT.java: concurrent users, partial rollback, inactive original refs, child authorization, original receipt replay.

**Completion conditions:** All confirmed fields correctable for one occurrence only; no bypass of root version.

**Suggested commit:** `feat(ux-b): ux-b07 implementation and verification [UX-B07]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B08｜Food Root Queries and Complete Details

**Dependencies:** UX-B07, UX-B02
**Estimate:** 3–5person-hours; status TODO
**Requirements:** F20/D02
**Backend/Database changes:** UX-BC07

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodEntryReadService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/food/FoodEntryReadRepository.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `listRoots(); getOwnedRoot(); batchLoadChildren(); orderStable()`

**Execution steps:**
1. UNION standalone foods/dish parents, paginate record date/stable ID; complete roots, no duplicate top-level children.
2. Sum actual leaf snapshots; retain legacy dataQuality/missing semantics, never infer dishes.
3. Batch children to avoid N+1; details controlled by owned root; old list semantics retained.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/food/FoodEntryReadIT.java:2roots5leaves, no split-group pagination, meal filter, deleted hidden, legacy retained.

**Completion conditions:** Dashboard/History/Log again read consistent complete root details.

**Suggested commit:** `feat(ux-b): ux-b08 implementation and verification [UX-B08]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B09｜Exercise Options, Movements, and Source Mappings

**Dependencies:** UX-D03
**Estimate:** 4–8person-hours; status TODO
**Requirements:** E1/E2/E3/E5
**Backend/Database changes:** UX-BC08

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/activity/catalogue/ExerciseOptionImporter.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/resources/data/ux/exercise-options-v1.json` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/resources/data/ux/movement-catalogue-v1.json` — Proposed new path; reuse equivalent implementation if found
- `scripts/validate_ux_catalogues.py` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `validateOptionMapping(); importImmutableCatalogueVersion(); coverageReport()`

**Execution steps:**
1. Cover chapter9 common equipment/sports with searchable aliases; no separate MET for each strength movement.
2. Verify official activity codes/full conditions, review reasons for GENERAL_ACTIVITY_ESTIMATE/UNAVAILABLE; rower<100W is not precise for every rower.
3. Original r1 immutable; new references require official-source validation/new revision; no MET in frontend/user input.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/activity/catalogue/ExerciseOptionImporterTest.java: invalid mapping, duplicate IDs, ambiguous aliases, movements without MET, coverage list.

**Completion conditions:** Needed recordable activities findable; insufficient evidence honestly means no estimate, not deleted catalogue entries.

**Suggested commit:** `feat(ux-b): ux-b09 implementation and verification [UX-B09]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B10｜Category, Search, and Movement APIs

**Dependencies:** UX-B09
**Estimate:** 2–4person-hours; status TODO
**Requirements:** E1/E2/E3
**Backend/Database changes:** UX-BC08

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/activity/catalogue/ExerciseOptionService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/activity/catalogue/ExerciseOptionController.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/activity/catalogue/MovementCatalogueService.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `listOptions(category,query); listMovements(bodyPart,equipment,query); defaultPeriodOptionId(); allowedDetailFields()`

**Execution steps:**
1. Cardio/Strength/Sports/Outdoor&Other and equipment/body-part filters; server supplies detail allowlist.
2. Distinguish STRENGTH period/ACTIVITY options; seated strength row is not aerobic rower.
3. Pagination limits, active controls,503 data failures; no fabricated calories for unmapped options.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/activity/catalogue/ExerciseOptionControllerTest.java: categories/aliases/empty/incompatible body fields.

**Completion conditions:** No frontend hardcoding of all movements/technical parameters.

**Suggested commit:** `feat(ux-b): ux-b10 implementation and verification [UX-B10]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B11｜Unified Quick/Workout Period Preview

**Dependencies:** UX-B10, UX-B05
**Estimate:** 4–7person-hours; status TODO
**Requirements:** E2–E5
**Backend/Database changes:** UX-BC09

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/workout/WorkoutEstimateService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/workout/EquipmentDetailsValidator.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/ExerciseEstimateService.java` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `estimatePeriod(); validateOptionalDetails(); sumAvailablePeriods(); computeFingerprint()`

**Execution steps:**
1. Estimate each nonoverlapping period with date-specific weight; strength duration counted once; sets/reps never converted to minutes.
2. Equipment parameters detail-only, no MET/formula effect; content hashing reflects changes without invented accuracy.
3. UNAVAILABLE mappings/inapplicability return explicit status/null; service failures are not UNAVAILABLE; full minutes retained.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/workout/WorkoutEstimateTest.java
- backend/src/test/java/com/fitnutri/backend/records/workout/WorkoutPreviewIT.java:70min/1342.366kJ fixture, partial unavailable, parameters never alter MET.

**Completion conditions:** Single/group logic consistent; fingerprint detects date/weight changes.

**Suggested commit:** `feat(ux-b): ux-b11 implementation and verification [UX-B11]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B12｜Atomic Workout and Quick V2 Saving

**Dependencies:** UX-B11, UX-D04
**Estimate:** 4–7person-hours; status TODO
**Requirements:** E1/E4/E7
**Backend/Database changes:** UX-BC09/10/06

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/workout/ExerciseEntryV2Controller.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/workout/ExerciseEntryV2Service.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/workout/WorkoutRepository.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `createQuick(); createWorkout(); persistPeriodsAndMovementDetails(); typedReceipt()`

**Execution steps:**
1. QUICK root equals one leaf; WORKOUT parent has periods; movement has no independent ledger/submission leaf.
2. Validate options/movement ownership/parameters/total duration≤1440; reject client calories/MET.
3. Save roots/periods/movements/proof in one SubmissionServiceV2 transaction; any missing item rolls back.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/workout/ExerciseEntryV2IT.java: movement failure rolls back group, unavailable mapping permits null, concurrent same ID one group.

**Completion conditions:** Real PG save/readback matches preview; no repeated strength timing.

**Suggested commit:** `feat(ux-b): ux-b12 implementation and verification [UX-B12]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B13｜Workout Corrections, Deletion, and Legacy Bypass Prevention

**Dependencies:** UX-B12
**Estimate:** 4–7person-hours; status TODO
**Requirements:** E8
**Backend/Database changes:** UX-BC10

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/workout/WorkoutMutationService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/ExerciseRecordService.java` — Existing; incrementally modify/reuse
- `backend/src/main/java/com/fitnutri/backend/records/RecordCorrectionController.java` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `updateOwnedWorkout(); softDeleteOwnedWorkout(); requireStandalonePeriod()`

**Execution steps:**
1. Parent version; movement/equipment-only edits preserve energy; type/date/minutes recalculate affected periods.
2. Period additions/removals atomic; original inactiveoption may remain historical, additions/Log again must be active.
3. V1 period PUT/DELETE cannot bypass root; confirmed whole deletion excludes all periods.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/workout/WorkoutMutationIT.java: load-only no recalculation, date uses historical weight, stale409, legacy standalone correction.

**Completion conditions:** Predictable History correction affects only this workout.

**Suggested commit:** `feat(ux-b): ux-b13 implementation and verification [UX-B13]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B14｜Exercise Root Lists, Details, Recent, and Reuse

**Dependencies:** UX-B13
**Estimate:** 3–5person-hours; status TODO
**Requirements:** E6/E8
**Backend/Database changes:** UX-BC10

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/records/workout/ExerciseEntryReadService.java` — Proposed new path; reuse equivalent implementation if found
- `backend/src/main/java/com/fitnutri/backend/records/workout/ExerciseRecentService.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `listRoots(); loadPeriodDetails(); recentEntries(); reuseSuggestion()`

**Execution steps:**
1. QUICK/WORKOUT share root-paginated API; details include periods/movements.
2. Recent owner/createdAt, exclude deleted; Log again GET only, never inherit historical date/currentmeal.
3. Previous duration/optional details offered for explicit Use last, no automatic creation/history mutation.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/records/workout/ExerciseEntryReadIT.java: root/period counts, deletion filter, inactive reuse notice, cross-user.

**Completion conditions:** Frontend can inspect/reuse complete workout.

**Suggested commit:** `feat(ux-b): ux-b14 implementation and verification [UX-B14]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-B15｜Consistent Aggregates, Counts, Details, and Q&A Context

**Dependencies:** UX-B08, UX-B14
**Estimate:** 3–6person-hours; status TODO
**Requirements:** U07/U08/aggregate consistency
**Backend/Database changes:** UX-BC11

**File locations:**
- `backend/src/main/java/com/fitnutri/backend/summary/SummaryRepository.java` — Existing; incrementally modify/reuse
- `backend/src/main/java/com/fitnutri/backend/summary/SummaryModels.java` — Existing; incrementally modify/reuse
- `backend/src/main/java/com/fitnutri/backend/summary/SummaryService.java` — Existing; incrementally modify/reuse
- `backend/src/main/java/com/fitnutri/backend/qa/QaContextBuilder.java` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `rootCounts(); existingLeafAggregates(); preserveMissingSemantics()`

**Execution steps:**
1. Totals still sum undeleted leaves; add rootcount without changing existing count; one dish/workout=one user card.
2. Preserve profile/goal/date/unit rules; daily/weekly summaries share aggregation.
3. Q&A sends allowed aggregates only, no parent-plus-child double counting or extra private inputs.

**Tests alongside this task:**
- backend/src/test/java/com/fitnutri/backend/summary/GroupedSummaryConsistencyIT.java:5leaves2foodroots,2periods1workout70min, all views consistent after portion/delete/backdate changes.

**Completion conditions:** Independently verify counts/energy/minutes from actual DB.

**Suggested commit:** `feat(ux-b): ux-b15 implementation and verification [UX-B15]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F00｜Registration Success Disappears Within Three Seconds

**Dependencies:** UX-P03
**Estimate:** 1–2person-hours; status TODO
**Requirements:** U01
**Backend/Database changes:** None

**File locations:**
- `frontend/src/pages/AuthPage.tsx` — Existing; incrementally modify/reuse
- `frontend/tests/component/AuthSuccessNotice.test.tsx` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `registrationNotice state/effect；2500ms visible＋≤300ms exit`

**Execution steps:**
1. Only success auto-dismisses; form errors remain. No repeat registration or automatic auth changes.
2. Clear timeout/route-unmount effects correctly; reduced-motion adds no delay.
3. Repeated success restarts timer; old notice cannot obscure login errors.

**Tests alongside this task:**
- Vitest fake timers: faded/no space by2.8s, errors remain, no updates after unmount.

**Completion conditions:** U01 fully resolved, theme/login behavior preserved.

**Suggested commit:** `feat(ux-f): ux-f00 implementation and verification [UX-F00]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F01｜Onboarding Alignment and Clear Clickable Actions

**Dependencies:** UX-P03
**Estimate:** 2–3person-hours; status TODO
**Requirements:** U02/U03
**Backend/Database changes:** None

**File locations:**
- `frontend/src/features/profile/ProfileSections.tsx` — Existing; incrementally modify/reuse
- `frontend/src/pages/DashboardPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/components/MetricCard.tsx` — Existing; incrementally modify/reuse
- `frontend/src/app/theme.ts` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `consentLabel alignment；primary/secondary action styling；read-only card semantics`

**Execution steps:**
1. Checkbox aligns first line, clickable label, original consent meaning.
2. Clear AddFood/AddExercise primary buttons, readable names/arrows for details; remove misleading pointer on noninteractive metrics.
3. Scoped existing-theme variants, no global button CSS breaking Admin/Chat.

**Tests alongside this task:**
- frontend/tests/component/ActionAffordance.test.tsx: keyboard/aria, multiline onboarding labels, reduced-motion visual checks.

**Completion conditions:** Users distinguish actions/values without a new theme.

**Suggested commit:** `feat(ux-f): ux-f01 implementation and verification [UX-F01]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F02｜Dashboard/History Chart Placement and Axes

**Dependencies:** UX-F01
**Estimate:** 3–5person-hours; status TODO
**Requirements:** U07/U08
**Backend/Database changes:** UX-BC12

**File locations:**
- `frontend/src/components/IntakeChart.tsx` — Existing; incrementally modify/reuse
- `frontend/src/components/TimeSeriesChart.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/pages/DashboardPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/pages/HistoryPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/history/PeriodTrends.tsx` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `TimeSeriesChart(props); axis formatters；graph-first DOM structure`

**Execution steps:**
1. Chapter17 placement after metrics/filter-statistics; explicit desktop/mobile dimensions.
2. Replace old no-YAxis SVG using existing Recharts; explicit dates/ticks/units/legend/null gaps.
3. Same-data table alternative; placement never changes API date/energy semantics.

**Tests alongside this task:**
- frontend/tests/component/TrendChart.test.tsx: kJ/kcal/null/real zero/empty; E2E screenshots/DOM order.

**Completion conditions:** Main trends readable on both first screens, not merely moving tiny old SVG upward.

**Suggested commit:** `feat(ux-f): ux-f02 implementation and verification [UX-F02]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F03｜Generate V2 Types and Strict API Clients

**Dependencies:** UX-P02
**Estimate:** 3–5person-hours; status TODO
**Requirements:** Contracts
**Backend/Database changes:** UX-BC13

**File locations:**
- `scripts/generate_api_types.py` — Existing; incrementally modify/reuse
- `docs/api/openapi-mvp.yaml` — Existing; incrementally modify/reuse
- `frontend/src/api/contracts.ts` — Existing; incrementally modify/reuse
- `frontend/src/api/schemas.ts` — Existing; incrementally modify/reuse
- `frontend/src/api/foodEntriesV2.ts` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/api/exerciseEntriesV2.ts` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/api/submissionsV2.ts` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `decodeFoodEntryV2(); decodeExerciseEntryV2(); confirmBatchV2(); readReceiptV2()`

**Execution steps:**
1. Generate types/Zod from JSON OpenAPI, no handwritten any; const+anyOf mutually exclusive kinds or first extend generator.
2. Reuse api/client session/CSRF/owner-error handling; /v2 under base/api, avoid doubled/api.
3. Retain V1 decode/hash tests; typed V2 root/leaf proof validation and fieldpath error mapping.

**Tests alongside this task:**
- frontend/tests/contract/entryV2Contract.test.ts: missing/extra fields, fake IDs, same numeric ID across kinds,500/unknown.

**Completion conditions:** UI consumes decoded data only, no as any pretend consistency.

**Suggested commit:** `feat(ux-f): ux-f03 implementation and verification [UX-F03]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F04｜Unified Single/Dish/Workout Draft Structure

**Dependencies:** UX-F03, UX-Q00
**Estimate:** 4–7person-hours; status TODO
**Requirements:** F08–F24/D/E
**Backend/Database changes:** UX-BC13

**File locations:**
- `frontend/src/features/entry/draftTypes.ts` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/draftReducer.ts` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/draftValidation.ts` — Existing; incrementally modify/reuse
- `frontend/src/app/DraftProvider.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/submissionPayload.ts` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `FoodRootDraft/ExerciseEntryDraft; entryKind discriminator; revision; deepFreezeSubmission()`

**Execution steps:**
1. Preserve owner binding/synchronous draftref; replace foods[] as sole source, adapt existing media paths.
2. Group/period/movement actions with stable UUIDs; checkbox booleans no longer manual-add prerequisites.
3. Deep-copy/freeze children; saving/unknown immutable; memory-only drafts, no automatic storage effects.

**Tests alongside this task:**
- frontend/tests/unit/draftV3Reducer.test.ts: copy/undo/portion/reference change/old response/independent reuseIDs/deep freeze.

**Completion conditions:** One domain state, not old/new Providers duplicating selection.

**Suggested commit:** `feat(ux-f): ux-f04 implementation and verification [UX-F04]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F05｜Smart Search, More Results, and Recent UI

**Dependencies:** UX-F04, UX-B01, UX-B02
**Estimate:** 3–6person-hours; status TODO
**Requirements:** F01–F07/U04
**Backend/Database changes:** UX-BC02/03

**File locations:**
- `frontend/src/features/entry/FoodReferencePicker.tsx` — Existing; incrementally modify/reuse
- `frontend/src/hooks/useFoodSearch.ts` — Existing; incrementally modify/reuse
- `frontend/src/hooks/useRecentFoods.ts` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/food/SmartFoodSearch.tsx` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `suggestion/group facets；expandAll；explicit select；UseLastGrams`

**Execution steps:**
1. Real shortName/key distinctions, default5/more20; complete results not limited to5.
2. Recent reuses exact reference with blank grams/explicit fill; no default first/approximate candidate selection.
3. Remove Fcode/source blocks but retain unobtrusive Food details; input/cancel/old response restore selectionTarget.

**Tests alongside this task:**
- frontend/tests/component/SmartFoodSearch.test.tsx: rice ranking/aliases/unselected approximations/unknown attributes/pagination/race/lastgrams.

**Completion conditions:** No database-code expertise required; complete valid official records accessible.

**Suggested commit:** `feat(ux-f): ux-f05 implementation and verification [UX-F05]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F06｜Continuous Add, Inline Edit, and Draft Undo

**Dependencies:** UX-F05
**Estimate:** 4–6person-hours; status TODO
**Requirements:** F08–F15/U05/U06
**Backend/Database changes:** UX-BC13

**File locations:**
- `frontend/src/features/entry/FoodEditor.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/FoodItemEditor.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/food/FoodComposer.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/food/MyMealList.tsx` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `commitComposerToDraft(); inlinePatch(); replaceReferenceTarget(); undoDraftRemoval()`

**Execution steps:**
1. One food with g/defaultmeal at a time; Add stores memory then restores search, no POST.
2. Inline g/meal updates that item/preview only; reference search replacement cancel preserves original.
3. Review cannot silently ignore unfinished composer: explicit Add or Discard before continuing, no extra Confirm boxes.

**Tests alongside this task:**
- frontend/tests/component/FoodComposer.test.tsx: three continuous adds/defaultmeal preserves old items/stale preview/delete-Undo.

**Completion conditions:** Continuous low-repetition flow with no hidden unsaved item.

**Suggested commit:** `feat(ux-f): ux-f06 implementation and verification [UX-F06]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F07｜Dish Editor, Portion Modes, and Ingredient Entry

**Dependencies:** UX-F06, UX-B04
**Estimate:** 4–8person-hours; status TODO
**Requirements:** D01–D11
**Backend/Database changes:** UX-BC04/05

**File locations:**
- `frontend/src/features/entry/food/DishEditor.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/food/DishIngredientRow.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/food/PortionEditor.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/food/DishSummaryCard.tsx` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `RecordByIngredients；switchInputMode；normalisePortionInput；applyRecipeSuggestion`

**Execution steps:**
1. User-named DISH, same SmartSearch/gram inputs; no nested dishes or automatic model-recipe decomposition.
2. PERSONAL default, full recipe needs explicit portion; fraction/custompercent/advancedservings retain precision.
3. Mode switch explains semantics without guessed conversion; explicit Use last recipe, reselect portion; one meal per dish.

**Tests alongside this task:**
- frontend/tests/component/DishEditor.test.tsx:1/3 not33.3, personal/full switches/cancel/missing portion/explicit recipe reuse.

**Completion conditions:** MyMeal/Review preserve recipe-versus-current-intake distinction; source never claims official whole dish.

**Suggested commit:** `feat(ux-f): ux-f07 implementation and verification [UX-F07]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F08｜Simplified Shared Review with Inline Adjustment

**Dependencies:** UX-F07
**Estimate:** 3–6person-hours; status TODO
**Requirements:** F16/D02
**Backend/Database changes:** UX-BC13

**File locations:**
- `frontend/src/features/entry/ReviewPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/ReviewFoodRow.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/food/ReviewFoodRoots.tsx` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `groupByMeal(); RootReviewRow；inlineedit；returnToReferenceSearch`

**Execution steps:**
1. Group roots by nonempty meals; expandable dish as one item, date top/totals bottom.
2. Shared MyMeal inline editor, quantity/portion changes refresh preview; no repeat questionnaire.
3. Final Confirm&save only; technical code/Fscore/policy in details; reference replacement returns original Review root.

**Tests alongside this task:**
- frontend/tests/component/FoodReviewV2.test.tsx:2roots not5items, edits stay, back to draft, future-date block.

**Completion conditions:** Review checks/corrects rather than repeats input or exposes debugging.

**Suggested commit:** `feat(ux-f): ux-f08 implementation and verification [UX-F08]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F09｜Real Food Save, Stay-on-Page, and Date Rule C

**Dependencies:** UX-F08, UX-B06
**Estimate:** 4–7person-hours; status TODO
**Requirements:** F18–F24
**Backend/Database changes:** UX-BC06/13

**File locations:**
- `frontend/src/hooks/useSubmitDraft.ts` — Existing; incrementally modify/reuse
- `frontend/src/api/submissionsV2.ts` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/SubmissionRecovery.tsx` — Existing; incrementally modify/reuse
- `frontend/src/pages/EntryPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/app/queryKeys.ts` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `freezeV2(); validateProof(); finishCreation(); nextEntryContext(); invalidateAffectedDates()`

**Execution steps:**
1. Retain single-click lock/owner epoch; explicit kind selects V2 domain, not guessed items field.
2. Proven success clears submitted draft, actual AddFood editor not success/chooser page; three-second notice+persistent Viewrecords.
3. Today retains date/defaultmeal; past resets today retaining explicit meal with continuepast; unknown same ID until refresh/explicit discard.

**Tests alongside this task:**
- frontend/tests/component/FoodSubmitV2.test.tsx: missing-leaf proof retains draft/replay/one CSRF retry/GET failure no POST/actual editor return.

**Completion conditions:** One save, clear feedback, immediate continued entry, no silently inherited past date.

**Suggested commit:** `feat(ux-f): ux-f09 implementation and verification [UX-F09]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F10｜Quick Categories, Recent, and Equipment Inputs

**Dependencies:** UX-F04, UX-B10, UX-B11
**Estimate:** 4–6person-hours; status TODO
**Requirements:** E1/E3/E5/E6
**Backend/Database changes:** UX-BC08/09

**File locations:**
- `frontend/src/features/entry/ExerciseEditor.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/exercise/ExerciseOptionPicker.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/exercise/ActivityPeriodEditor.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/exercise/EquipmentDetailsEditor.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/hooks/useExerciseEstimate.ts` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `selectQuickOption(); startQuickFromMovement(); optionalFieldWhitelist；UseLastDuration；datedPreview`

**Execution steps:**
1. New page defaults Quick, Gym switch; combined kind-labelled options/movements search; strength equipment creates one STRENGTH period/preset movement, no hardcoded15-item list.
2. Required duration, applicable optional equipment parameters; no required intensity, unavailable estimate still permits Review.
3. Recent reuses option not duration; explicit Use last duration. Option changes prompt/cancel discarding incompatible details, no hidden leftovers.

**Tests alongside this task:**
- frontend/tests/component/QuickExerciseV2.test.tsx: distinct basketball/incline/rowing, optional parameters, no-MET recording, blank duration.

**Completion conditions:** Ordinary exercise never requires complex Workout.

**Suggested commit:** `feat(ux-f): ux-f10 implementation and verification [UX-F10]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F11｜Multi-Period Gym, Strength Movements, Optional Sets/Load

**Dependencies:** UX-F10
**Estimate:** 4–8person-hours; status TODO
**Requirements:** E1/E2/E4
**Backend/Database changes:** UX-BC09/10

**File locations:**
- `frontend/src/features/entry/exercise/WorkoutEditor.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/exercise/StrengthPeriodEditor.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/exercise/MovementPicker.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/exercise/MovementDetailsRow.tsx` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `appendPeriod(); appendMovement(); sumMinutes(); optionalSetsRepsLoad()`

**Execution steps:**
1. One strength period and multiple independent aerobic periods, no required complex plan/movement timer.
2. Enter strength time once; selectable movement names, validate provided details; clear load-unit basis, never automatic×2.
3. Period/movement add-delete memory Undo, total-time calculation/explicit preview; legitimate duplicate movement names allowed.

**Tests alongside this task:**
- frontend/tests/component/WorkoutEditor.test.tsx: three movements still50+20=70, blank optional fields Review, invalid filled values blocked, target-only deletion.

**Completion conditions:** Detailed training without corrupting time/energy aggregates.

**Suggested commit:** `feat(ux-f): ux-f11 implementation and verification [UX-F11]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F12｜Exercise Review and Real Save Staying on Entry

**Dependencies:** UX-F11, UX-B12, UX-F09
**Estimate:** 3–6person-hours; status TODO
**Requirements:** E7
**Backend/Database changes:** UX-BC09/13

**File locations:**
- `frontend/src/features/entry/exercise/ReviewExerciseRoot.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/features/entry/ReviewPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/hooks/useSubmitDraft.ts` — Existing; incrementally modify/reuse
- `frontend/src/pages/EntryPage.tsx` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `reviewQuickOrWorkout(); submitExerciseV2(); preserveEntryModeAfterSuccess()`

**Execution steps:**
1. Shared confirmation shell, activity/total minutes/estimate completeness; expandable periods/movements, not lengthy MET formulas.
2. Date/duration/type changes repreview; movement/equipment details change content without choosing new MET.
3. Successful save stays actual AddExercise editor with Quick/Gym memory mode; new duration blank, past date follows C.

**Tests alongside this task:**
- frontend/tests/component/ExerciseReviewV2.test.tsx: one Confirm, partial unavailable savable, complete proof, double clicks.

**Completion conditions:** Consistent Food/exercise success, no automatic Dashboard navigation/duplicate save.

**Suggested commit:** `feat(ux-f): ux-f12 implementation and verification [UX-F12]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F13｜Dashboard/History Root Cards, Details, Counts

**Dependencies:** UX-F02, UX-B08, UX-B14, UX-B15
**Estimate:** 4–7person-hours; status TODO
**Requirements:** D02/E8/U07/U08
**Backend/Database changes:** UX-BC07/10/11

**File locations:**
- `frontend/src/pages/DashboardPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/pages/HistoryPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/components/RecordList.tsx` — Existing; incrementally modify/reuse
- `frontend/src/components/RecordDetailsDialog.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/history/EntryRootCard.tsx` — Proposed new path; reuse equivalent implementation if found
- `frontend/src/hooks/useRecordQueries.ts` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `renderEntryRoot(); typedHighlightKey(); rootCountConsistency；expandDetails`

**Execution steps:**
1. Root pagination for single/dish/quick/workout, no duplicate top-level children; compare like-for-like counts.
2. Details retain user name/recipe/current portion/periods/movements, collapsible technical sources.
3. Viewrecords targets past date by kind+id; distinguish empty range/read errors.

**Tests alongside this task:**
- frontend/tests/component/GroupedHistory.test.tsx:2roots5leaves, units, typed-ID collisions, historical dates, legacy flat records.

**Completion conditions:** Users identify dish/workout; statistics match database.

**Suggested commit:** `feat(ux-f): ux-f13 implementation and verification [UX-F13]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F14｜Grouped Correction, Whole Deletion, and Log Again

**Dependencies:** UX-F13, UX-B07, UX-B13
**Estimate:** 4–8person-hours; status TODO
**Requirements:** D10–D14/E6/E8
**Backend/Database changes:** UX-BC07/10

**File locations:**
- `frontend/src/features/entry/CorrectionReviewPage.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/recordMutationState.ts` — Existing; incrementally modify/reuse
- `frontend/src/hooks/useRecordMutations.ts` — Existing; incrementally modify/reuse
- `frontend/src/components/RecordEditControl.tsx` — Existing; incrementally modify/reuse
- `frontend/src/components/RecordDeletionControl.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/history/reuseEntry.ts` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `draftFromOwnedRoot(); correctionV2Intent；explicitUseLast；applyRootMutation`

**Execution steps:**
1. Edit retains root id/version/snapshot; Logagain entirely new root/leaf UUIDs/current date; different CTA/API.
2. Personal g blank/explicit Use-last; full recipe explicit reuse and blank portion; Workout time blank, optional details explicit reuse.
3. Whole-root delete confirmation/transaction;409 preserves input/no overwrite; unknown GET comparison never becomes create.

**Tests alongside this task:**
- frontend/tests/component/GroupedCorrection.test.tsx: occurrence independence/partial recipe edit/full delete/stale409/cancel/new Logagain IDs.

**Completion conditions:** Correction/new creation distinct; other same-name dishes/workouts untouched.

**Suggested commit:** `feat(ux-f): ux-f14 implementation and verification [UX-F14]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F15｜Precise Leave Warnings and No Refresh Recovery

**Dependencies:** UX-F09, UX-F12, UX-F14
**Estimate:** 2–4person-hours; status TODO
**Requirements:** F22/F24
**Backend/Database changes:** UX-BC13

**File locations:**
- `frontend/src/components/UnsavedChangesGuard.tsx` — Existing; incrementally modify/reuse
- `frontend/src/hooks/useUnsavedChanges.ts` — Existing; incrementally modify/reuse
- `frontend/src/app/NavigationSafetyContext.ts` — Existing; incrementally modify/reuse
- `frontend/src/app/DraftProvider.tsx` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `allowInternalFlowNavigation(); discardAndLeave(); bestEffortBeforeUnload()`

**Execution steps:**
1. Entry↔Review↔targetsearch no warning; leaving for Dashboard/History with draft offers Keep/Discard.
2. Unknown leaving explains possible server save; confirmed discard clears memory; hard refresh leaves no recovery marker/ID or beacon write.
3. Account switch clears draft/undo/private cache; new user never receives old owner response.

**Tests alongside this task:**
- frontend/tests/component/NoDraftPersistence.test.tsx
- frontend/tests/e2e/refresh-manual-history.spec.ts: spy all storage, refresh no recovery, server proof retained.

**Completion conditions:** Respect user's draft A/refresh-exception B choices, no covert allegedly safer recovery.

**Suggested commit:** `feat(ux-f): ux-f15 implementation and verification [UX-F15]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-F16｜Adapt Image/Speech Candidates and Regress Advanced Features

**Dependencies:** UX-F04, UX-F08, UX-F12
**Estimate:** 3–6person-hours; status TODO
**Requirements:** Media compatibility
**Backend/Database changes:** UX-BC13

**File locations:**
- `frontend/src/features/entry/media/imageDraft.ts` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/media/voiceDraft.ts` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/media/ImageEntryPanel.tsx` — Existing; incrementally modify/reuse
- `frontend/src/features/entry/media/VoiceEntryFlow.tsx` — Existing; incrementally modify/reuse
- `backend/src/main/java/com/fitnutri/backend/speech/CandidateMatcher.java` — Existing; incrementally modify/reuse

**Key functions/responsibilities:** `toSingleFoodCandidates(); toQuickExerciseCandidate(); requireExplicitReferenceSelection()`

**Execution steps:**
1. Adapt new union; provider fields cannot auto-become official references or whole-dish recipes.
2. Explicit current speech grams retained; historical blank-reuse rule cannot erase current input; complete unknown meal/g.
3. Preserve microphone denial/timeouts/manual fallback/scope refusals; new search does not rewrite providers.

**Tests alongside this task:**
- frontend/tests/component/MediaEntryV3Compatibility.test.tsx; regress all existing speech/image/QA API security tests.

**Completion conditions:** New manual flow preserves previously usable real AI flow.

**Suggested commit:** `feat(ux-f): ux-f16 implementation and verification [UX-F16]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-Q01｜Complete Food/Dish Real API and Browser Acceptance

**Dependencies:** UX-F09, UX-F13, UX-F14, UX-F15
**Estimate:** 3–6person-hours; status TODO
**Requirements:** All Food
**Backend/Database changes:** UX-BC04–07/13

**File locations:**
- `frontend/tests/e2e/food-entry-v3.spec.ts` — Proposed new path; reuse equivalent implementation if found
- `frontend/tests/e2e/dish-entry-v3.spec.ts` — Proposed new path; reuse equivalent implementation if found
- `backend/src/test/java/com/fitnutri/backend/records/food/FoodJourneyV2IT.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `newUserMealJourney(); fullRecipeAndReuseJourney(); correctOnlyOneOccurrence()`

**Execution steps:**
1. Use real frontend+Java+PG for ordinary items+dish; verify no Review writes, then roots/leaves/statistics after confirmation.
2. Verify personal/full-recipe fraction/percent/servings and explicit Logagain reuse; continue Add after save.
3. Record screenshots/requests/row counts against TUX scenarios; no real external-model keys.

**Tests alongside this task:**
- Corresponding chapter21 FOOD/DISH/SEARCH cases; synthetic removable test data only.

**Completion conditions:** Database and pages both pass, not merely clickable mock UI.

**Suggested commit:** `test(ux-q): ux-q01 implementation and verification [UX-Q01]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-Q02｜Quick/Gym Real Periods, Details, and History Acceptance

**Dependencies:** UX-F12, UX-F13, UX-F14, UX-F15
**Estimate:** 3–6person-hours; status TODO
**Requirements:** E1–E8
**Backend/Database changes:** UX-BC08–10/13

**File locations:**
- `frontend/tests/e2e/exercise-entry-v3.spec.ts` — Proposed new path; reuse equivalent implementation if found
- `frontend/tests/e2e/workout-entry-v3.spec.ts` — Proposed new path; reuse equivalent implementation if found
- `backend/src/test/java/com/fitnutri/backend/records/workout/WorkoutJourneyIT.java` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `quickJourney(); strengthAndCardio(); optionalDetailsAndReuse()`

**Execution steps:**
1. Quick sports/equipment records without Gym; Gym strength50+aerobic20 correct.
2. Optional load/sets/reps/equipment may be blank, valid values persist/read back, invalid values block; parameters do not alter MET.
3. Fully verify datebodyweight, partial unavailable estimates, root edit/delete/reuse.

**Tests alongside this task:**
- Chapter21 EXERCISE/WORKOUT cases; verify movement table has no duration/energy.

**Completion conditions:** Actual user flows prove all E choices compatible.

**Suggested commit:** `test(ux-q): ux-q02 implementation and verification [UX-Q02]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-Q03｜Aggregate Conservation and Multiple-Root/Day Regression

**Dependencies:** UX-B15, UX-F13, UX-Q01, UX-Q02
**Estimate:** 2–4person-hours; status TODO
**Requirements:** Deduplication/consistency
**Backend/Database changes:** UX-BC11

**File locations:**
- `backend/src/test/java/com/fitnutri/backend/summary/GroupedConservationIT.java` — Proposed new path; reuse equivalent implementation if found
- `frontend/tests/e2e/grouped-summary.spec.ts` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `ledgerConservation(); parentExcluded(); oldAndNewDatesReconciled()`

**Execution steps:**
1. Compute leafsum with independent SQL/fixed fixtures; compare Dashboard/History/trends/rules.
2. Retain consistent results across portion/date/delete/goal/unit changes.
3. Assert rootcount and leafcount separately; difference is not an error.

**Tests alongside this task:**
- All TUX calculation/chart assertions; UI approximation must not erase1/3 versus33.3%.

**Completion conditions:** Never double-count parents/movement duration/old-date records.

**Suggested commit:** `test(ux-q): ux-q03 implementation and verification [UX-Q03]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-Q04｜Concurrency, Lost Responses, and No Cross-Refresh Recovery

**Dependencies:** UX-B06, UX-B12, UX-F15
**Estimate:** 3–6person-hours; status TODO
**Requirements:** Reliable saving
**Backend/Database changes:** UX-BC06/13

**File locations:**
- `backend/src/test/java/com/fitnutri/backend/submission/V2ConcurrencyIT.java` — Proposed new path; reuse equivalent implementation if found
- `frontend/tests/e2e/save-unknown-v3.spec.ts` — Proposed new path; reuse equivalent implementation if found
- `frontend/tests/e2e/refresh-manual-history.spec.ts` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `duplicateConfirm(); loseResponseAfterCommit(); manualHistoryAfterReload()`

**Execution steps:**
1. Existing fault proxy drops post-commit response; verify same in-memory identity retry, not merely pre-submit failure.
2. No draft/ID recovery after hard refresh; manual History only, no automatic new POST.
3. Same content/new UUID is valid; same UUID/different payload rejected; record user-accepted risk.

**Tests alongside this task:**
- Concurrent transactions for two users, malicious cross-type IDs, late owner/epoch responses, no beacon/storage checks.

**Completion conditions:** Idempotency and explicit no-recovery choice both hold with explainable boundaries.

**Suggested commit:** `test(ux-q): ux-q04 implementation and verification [UX-Q04]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-Q05｜Authorization, Legacy API, Advanced-Service Compatibility

**Dependencies:** UX-F16, UX-B07, UX-B13
**Estimate:** 3–5person-hours; status TODO
**Requirements:** Regression/privacy
**Backend/Database changes:** UX-BC07/10/13

**File locations:**
- `backend/src/test/java/com/fitnutri/backend/security/GroupedOwnershipIT.java` — Proposed new path; reuse equivalent implementation if found
- `frontend/tests/e2e/legacy-regression-v3.spec.ts` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `directApiOwnerEnforcement(); legacyChildWriteBlocked(); providerIsolation()`

**Execution steps:**
1. ADMIN cannot read private groups; USER cannot manage accounts; all new details/preview/recent/mutations check owner.
2. Preserve legacy single create/read/correction, V1proof, CSRF/session, profile/QA refusals.
3. Missing AI key means blockedexternal; mock results never claim real-provider acceptance.

**Tests alongside this task:**
- TUX security/legacy/media and adjacent tests; verify old tests not removed/weakened.

**Completion conditions:** New capabilities do not weaken security or confirmed historical data.

**Suggested commit:** `test(ux-q): ux-q05 implementation and verification [UX-Q05]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-Q06｜Isolated Migration and Backup/Restore Rehearsal

**Dependencies:** UX-D04, UX-B15
**Estimate:** 3–6person-hours; status TODO
**Requirements:** Data protection
**Backend/Database changes:** UX-BC14

**File locations:**
- `backend/src/test/java/com/fitnutri/backend/migration/UxV3MigrationIT.java` — Proposed new path; reuse equivalent implementation if found
- `scripts/tests/test_preflight_ux_v3.py` — Proposed new path; reuse equivalent implementation if found
- `docs/verification/ux-v3-migration.md` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `baseline→migrate→validate→restore；originalProofReplay；precisionDiff()`

**Execution steps:**
1. Legacy fixtures include soft deletion/LEGACY/inactive/historical receipts; compare values/hashes before/after migration.
2. Verify group CHECK triggers, widened types, nullable exceptions, insertion order on real PG16.
3. Rehearse oldbinary schema boundaries; no lossless down-migration promise.

**Tests alongside this task:**
- Reproducible backup/restore, failed transaction, index/constraint, unchanged SQL aggregate logs.

**Completion conditions:** Explain safe release/forward-fix/recovery paths without shared DB changes.

**Suggested commit:** `test(ux-q): ux-q06 implementation and verification [UX-Q06]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-Q07｜Accessibility, Visual, Basic Performance Regression

**Dependencies:** UX-F02, UX-Q01, UX-Q02
**Estimate:** 2–5person-hours; status TODO
**Requirements:** U01–U08/quality
**Backend/Database changes:** UX-BC12

**File locations:**
- `frontend/tests/e2e/ux-accessibility-v3.spec.ts` — Proposed new path; reuse equivalent implementation if found
- `docs/verification/ux-v3-browser.md` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `keyboardFlows(); multiLineConsent(); chartAxes(); timedNotice()`

**Execution steps:**
1. Record actual Chrome/Edge/Safari versions separately; PlaywrightWebKit is not manual Safari acceptance.
2. Check button hierarchy/icon labels/focus/three-second notice/graph DOM/mobile width/data-table alternative.
3. Measure main ordinary operations, record device/samples/network; no unauthorized high-load testing.

**Tests alongside this task:**
- Before/after evidence for eight original issues; if Typora originals absent, capture current pages, no invented historical comparison.

**Completion conditions:** Functionality and usability reviewable, not just unit counts.

**Suggested commit:** `test(ux-q): ux-q07 implementation and verification [UX-Q07]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-R00｜Update Runtime, Contracts, and UAT Scripts

**Dependencies:** UX-Q03, UX-Q04, UX-Q05, UX-Q06, UX-Q07
**Estimate:** 2–4person-hours; status TODO
**Requirements:** All
**Backend/Database changes:** UX-BC01–14

**File locations:**
- `README.md` — Existing; incrementally modify/reuse
- `docs/development.md` — Existing; incrementally modify/reuse
- `docs/verification/ux-v3-uat.md` — Proposed new path; reuse equivalent implementation if found
- `docs/implementation/ux-v3-backend-changes.md` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `single active spec；startup instructions；M1-existing regression＋UX release evidence`

**Execution steps:**
1. Document actually enabled endpoints/capabilities/metadatarevision and controlled new migrations.
2. Turn chapter22 into checkable UAT/environment-account preparation; distinguish instructor requirements/engineering defaults.
3. Mark old plans historical; no choice between competing MDs; this round is active specification.

**Tests alongside this task:**
- Check links/commands/generated contracts; tasks/BC/tests/commits traceable.

**Completion conditions:** Docs reflect implementation; missing items/external blockers not hidden by aggregate completion.

**Suggested commit:** `docs(ux-r): ux-r00 implementation and verification [UX-R00]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


### UX-R01｜Final Review, Branch Commits, and Delivery Receipt

**Dependencies:** UX-R00
**Estimate:** 1–3person-hours; status TODO
**Requirements:** Delivery
**Backend/Database changes:** All

**File locations:**
- `docs/verification/ux-v3-final.md` — Proposed new path; reuse equivalent implementation if found
- `docs/implementation/ux-v3-tasks.md` — Proposed new path; reuse equivalent implementation if found

**Key functions/responsibilities:** `finalBranchGate(); changedFilesAndTests(); deliveryStatusByLayer()`

**Execution steps:**
1. Rerun applicable complete checks; generated files clean, no accidentally committed secrets/seeds/user changes.
2. Verify all local commits remain on develop/yuankang-wei, no push/merge; list actual SHA/task.
3. Separate IMPLEMENTED/TESTED/IN_REVIEW/UAT_PENDING; explain accepted no-refresh-recovery risk.

**Tests alongside this task:**
- Actual final commands/exit codes/counts/evidence; unrun layers NOT_RUN/BLOCKED.

**Completion conditions:** Deliver runnable changes and reviewable evidence; no proxy signoff or unverified-environment passes.

**Suggested commit:** `docs(ux-r): ux-r01 implementation and verification [UX-R01]`; replace generic wording with concrete English behavior; body requires Why/BC/DB/Tests/Limitations; never mark unrun tests PASS.


# 21. Acceptance Test Matrix and Examples

This version defines **129 numbered scenarios**, a checklist to implement/reuse, not an executed passing count. Multiple unit/IT/E2E methods may prove a scenario; not every boundary needs the slowest browser test. Begin tests with each implementation task, not deferred to Q.

|Layer|Execution|Evidence boundary|
|---|---|---|
|Pure logic|JUnit Jupiter/Mockito, Vitest|Calculation/validation/state; not PG transactions|
|Web/API slice|Existing MockMvc stack|Status/validation/JSON/security; not cross-layer integration|
|Real data integration|Isolated PG16+Spring; `*IT` through Failsafe|Atomicity/constraints/locks/idempotency/migrations|
|Component/contract|Existing Vitest/Testing Library/Zod|UI/parameters/out-of-order/preview/schema|
|E2E|Existing Playwright+real frontend/Java/PG|Complete journeys; no unauthorized paid calls|
|Manual UAT|Actual user/owner follows chapter22|User acceptance; AI cannot sign for them|


## 21.1 Development Gates

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-001|Correct personal branch: existing Git tree on develop/yuankang-wei|Record actual HEAD/old diff before starting; no remote access required.|UX-P00；read-only/process|
|TUX-002|main or no Git metadata: uncommitted main or source-only directory|Stop writes; no reset/stash/init/substitute branch.|UX-P00；read-only/process|
|TUX-003|Existing user changes on personal branch|Record scope, clarify conflicts first, exclude others' changes from staging/commits.|UX-P00；read-only/process|


## 21.2 Original UI Issues

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-004|Registration success fades: real success, fake clock|Fade and layout space end within 3000ms; login inputs unaffected.|UX-F00；component/browser|
|TUX-005|Registration/login errors have no timer|Remain until correction/user action, no success timer.|UX-F00；component/browser|
|TUX-006|Unmount/retrigger success notice|No setState after unmount; old timer cannot clear new notice.|UX-F00；component/browser|


## 21.3 Pages and Charts

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-007|Narrow onboarding wraps Allow text|Checkbox aligns with first line; clickable readable associated label.|UX-Q07；component/E2E|
|TUX-008|Dashboard Add buttons versus read-only metrics|Clear main actions, keyboard reachable; values do not impersonate buttons.|UX-Q07；component/E2E|
|TUX-009|Dashboard with multiple days|Chart DOM after metrics, before text/records; adequate width, Y ticks/units.|UX-Q07；component/E2E|
|TUX-010|History with many roots|filters/stats/graph/list order; no need to scroll all records before chart.|UX-Q07；component/E2E|
|TUX-011|One null day, real zero day, valued day|Retain graph gaps; tooltip/table distinguish null/0; no invented continuity.|UX-Q07；component/E2E|
|TUX-012|Switch kJ/kcal|Axes/tooltips/metrics convert consistently; stored values unchanged.|UX-Q07；component/E2E|
|TUX-013|Keyboard selection/edit/confirmation|Predictable focus, chart data table, icon labels, not color only.|UX-Q07；component/E2E|


## 21.4 Smart Search

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-014|query=rice with rice/licorice fixtures|licorice not ranked as a rice word match.|UX-B01；JUnit/MockMvc/component|
|TUX-015|query=cooked brown rice|Cooked brown rice before common white rice; recent cannot override explicit attributes.|UX-B01；JUnit/MockMvc/component|
|TUX-016|Explicit boiled egg query|Do not ask cooking method again; user still selects real reference.|UX-B01；JUnit/MockMvc/component|
|TUX-017|fried egg with added/no-added-fat alternatives|Ask remaining meaningful distinction only, not comma-count questions.|UX-B01；JUnit/MockMvc/component|
|TUX-018|User does not know preparation|Never default no-fat; offer explained approximate choice.|UX-B01；JUnit/MockMvc/component|
|TUX-019|First approximate candidate highlighted|No referenceId/authoritative nutrition without user selection.|UX-B01；JUnit/MockMvc/component|
|TUX-020|Uncommon entry without metadata|Official terms reach complete paginated search; noncommon entries not hidden.|UX-B01；JUnit/MockMvc/component|
|TUX-021|Reviewed Chinese alias 米饭 versus unreviewed alias|Reviewed alias matches; unreviewed is not confirmed fact.|UX-B01；JUnit/MockMvc/component|
|TUX-022|200 empty results versus 503|Empty offers Record by ingredients; failure retries preserving input, no seeds.|UX-B01；JUnit/MockMvc/component|
|TUX-023|More than 20 real matches|Top-five suggestions/full-20 pagination neither omit nor duplicate; stable order.|UX-B01；JUnit/MockMvc/component|
|TUX-024|chicken then rice; old response arrives late|Current query only; never auto-select old results.|UX-B01；JUnit/MockMvc/component|
|TUX-025|Selected rice150g, search brown rice, cancel replacement|Original reference/g/meal and other entries retained.|UX-B01；JUnit/MockMvc/component|


## 21.5 Recent and Reuse

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-026|Other user has food, current user only draft|Exclude others and unsaved drafts from recent.|UX-B02；PG/component|
|TUX-027|Today records food dated one month ago|Recent use ordered by operation, not old recordDate.|UX-B02；PG/component|
|TUX-028|Repeated same reference|Latest successful use and corresponding previous grams shown.|UX-B02；PG/component|
|TUX-029|Select recent180g|Reuse reference with blank g; only Use last 180g fills, editable afterwards.|UX-B02；PG/component|
|TUX-030|Previous Breakfast last week, current today Lunch|Reuse retains today Lunch.|UX-B02；PG/component|
|TUX-031|Historical reference inactive|History visible; new entry requires active replacement, never silent substitution.|UX-B02；PG/component|
|TUX-032|Saved one dish/four ingredients|Recent shows dish, not four ingredients as ordinary recent meals.|UX-B02；PG/component|


## 21.6 Continuous Entry and Review

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-033|Add rice/g/Lunch, then chicken|Added list retained, search resets, continue with no POST.|UX-F06；component/E2E|
|TUX-034|Two Lunch items then default Dinner|Old items remain Lunch; new Dinner; individual override available.|UX-F06；component/E2E|
|TUX-035|Delete second item then Undo|Same UUID/original position, others unchanged; no draft resurrection after save.|UX-F06；component/E2E|
|TUX-036|Change150g to200g inline|Only that preview recalculates; stale results not submittable.|UX-F06；component/E2E|
|TUX-037|Valid manual reference/g/meal without two confirmation boxes|Add/Review allowed; missing reference still blocked.|UX-F06；component/E2E|
|TUX-038|Filled composer not added; click Review|Explicit Add or Discard, no silent loss/automatic write.|UX-F06；component/E2E|
|TUX-039|Change grams/meal on Review|Stay and update; reference replacement searches then returns to same item.|UX-F06；component/E2E|
|TUX-040|Change global date to past/future|Past shared by batch; future rejected frontend/backend; no meal inferred from time.|UX-F06；component/E2E|
|TUX-041|Empty food batch or unfinished quantity|Cannot confirm empty; error locates incomplete root.|UX-F06；component/E2E|


## 21.7 Dishes and Portions

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-042|Create user-named spicy hotpot through ingredients|User grouping, not official whole-dish claim or guessed ingredients.|UX-B03；JUnit/PG/component|
|TUX-043|PERSONAL default A100g B50g|factor1/1, personal grams, no old ratio applied.|UX-B03；JUnit/PG/component|
|TUX-044|Valid full recipe without consumed portion|May view full pot, cannot confirm intake; no default all.|UX-B03；JUnit/PG/component|
|TUX-045|Synthetic3600kJ full recipe fraction1/3|1200.000kJ, original kind FRACTION retained.|UX-B03；JUnit/PG/component|
|TUX-046|Same recipe33.3%|1198.800kJ, not mistaken for1/3.|UX-B03；JUnit/PG/component|
|TUX-047|Four servings, one consumed|canonical1/4, SERVINGS input retained for display.|UX-B03；JUnit/PG/component|
|TUX-048|Zero/negative/>100%/eaten>total|Specific field rejected, no nutrition/history writes.|UX-B03；JUnit/PG/component|
|TUX-049|0.001g×0.001%|Positive consumedGrams retained, not old scale3 zero; UI not misleading0g.|UX-B03；JUnit/PG/component|
|TUX-050|Fraction recurring decimal|Original input/rational calculation; round final leaves then sum.|UX-B03；JUnit/PG/component|
|TUX-051|One-third dish1200 plus rice500|Two user items, leaf total1700; do not add full recipe3600.|UX-B03；JUnit/PG/component|
|TUX-052|Whole dish Lunch→Dinner|All children follow root; no child remains Lunch.|UX-B03；JUnit/PG/component|
|TUX-053|Cancel PERSONAL→full mode switch|Original mode/values/ratio unchanged.|UX-B03；JUnit/PG/component|
|TUX-054|Apply PERSONAL→full switch|Values retained with explicit meaning; portion reselected, no inferred whole weight.|UX-B03；JUnit/PG/component|
|TUX-055|Ingredient supplies dishId|Schema/service reject; only real foodreference allowed.|UX-B03；JUnit/PG/component|
|TUX-056|Same reference, different UUID rows|Both actual portions counted; no name-based deletion.|UX-B03；JUnit/PG/component|


## 21.8 Dish History and Reuse

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-057|Save dish plus rice then relogin|Two History roots, complete ingredients/input/personal portions expandable.|UX-B07；PG/component/E2E|
|TUX-058|Two same-name dishes, change second1/2→1/3|Only second root/leaves change; first snapshot unchanged.|UX-B07；PG/component/E2E|
|TUX-059|Recipe/reference correction with valid expectedVersion|Affected leaves recalculate consistently; any error rolls back whole correction.|UX-B07；PG/component/E2E|
|TUX-060|Name/meal-only correction after reference/weight update|No unnecessary snapshot rewrite; update relevant display/statistical grouping only.|UX-B07；PG/component/E2E|
|TUX-061|Remove leaf/add reference|Old leaf soft-deleted, new UUID, one root transaction.|UX-B07；PG/component/E2E|
|TUX-062|Confirm Delete dish|Root/all leaves excluded from statistics; cancel no changes.|UX-B07；PG/component/E2E|
|TUX-063|Two clients same version|One succeeds, other409 retains edits; no auto-overwrite with latest version.|UX-B07；PG/component/E2E|
|TUX-064|Direct V1PUT/DELETE dish child foodId|GROUP_EDIT_REQUIRED; no nutrition change without parent version.|UX-B07；PG/component/E2E|
|TUX-065|PERSONAL Log again with100g ingredients|New UUID, blank g, explicit Use last amounts; history unchanged.|UX-B07；PG/component/E2E|
|TUX-066|Full recipe Log again with old50%|Display recipe; Use last recipe explicitly fills; current portion blank.|UX-B07；PG/component/E2E|
|TUX-067|Legacy flat records same day/meal|Remain independent; no inferred dishes from name/date.|UX-B07；PG/component/E2E|


## 21.9 Quick and Catalogues

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-068|Quick basketball60min|No required Workout/equipment/intensity; Review/save allowed.|UX-B10；JUnit/MockMvc/component|
|TUX-069|Search chapter9 common equipment aliases|Find incline walking, stair climber, seated strength row, aerobic rowing separately.|UX-B10；JUnit/MockMvc/component|
|TUX-070|Reviewed mapping_kind=UNAVAILABLE|Save duration, unavailable estimate, no invented MET/zero.|UX-B10；JUnit/MockMvc/component|
|TUX-071|Reference service query failure|503 preserves input, not legitimate UNAVAILABLE.|UX-B10；JUnit/MockMvc/component|
|TUX-072|Common equipment GENERAL reference|Keep machine name, short general-estimate explanation, backend mapping evidence.|UX-B10；JUnit/MockMvc/component|
|TUX-073|Quick Chest press|One STRENGTH period/one movement, machine name, no forced Gym or duplicate time.|UX-B10；JUnit/MockMvc/component|
|TUX-074|Activity selected without minutes|Review blocked; no automatic usual duration.|UX-B10；JUnit/MockMvc/component|
|TUX-075|Any Quick/Gym path|No required intensity or substitution from profileActivityLevel.|UX-B10；JUnit/MockMvc/component|
|TUX-076|Select recent30min|Duration blank; explicit Use last duration only; current date retained.|UX-B10；JUnit/MockMvc/component|


## 21.10 Optional Details and Strength Timing

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-077|Named strength movement with empty sets/reps/load|Valid save/readback empty, not0.|UX-B11；JUnit/PG/component|
|TUX-078|Only sets3 or load40|Retain valid field, do not require all three.|UX-B11；JUnit/PG/component|
|TUX-079|Negative sets/fractional reps/excess load|Specific field errors; optional does not mean unrestricted.|UX-B11；JUnit/PG/component|
|TUX-080|Treadmill duration only|Savable; speed/incline/distance optional.|UX-B11；JUnit/PG/component|
|TUX-081|speed5km/h and incline8%|Readback as detail; no automatic MET/estimate change.|UX-B11；JUnit/PG/component|
|TUX-082|Unsupported equipmentKey for stair climber|Reject unknown/inapplicable fields; equipment change prompts clearing incompatible values.|UX-B11；JUnit/PG/component|
|TUX-083|Dumbbell per-hand versus machine displayed-stack|Catalogue labels, no automatic×2 or invented training volume.|UX-B11；JUnit/PG/component|
|TUX-084|Strength50min/three movements plus aerobic20min|Total70min, no extra50 per movement; fixture energy1342.366.|UX-B11；JUnit/PG/component|
|TUX-085|Fourth movement within same50min|Minutes/energy unchanged; details change.|UX-B11；JUnit/PG/component|
|TUX-086|AVAILABLE plus NO_APPLICABLE; or all unavailable|All durations retained; known subtotal marked incomplete; all unavailable shown Unavailable.|UX-B11；JUnit/PG/component|
|TUX-087|Historical70kg versus today75kg|Backdated new record uses nearest preceding70kg.|UX-B11；JUnit/PG/component|
|TUX-088|Period sum exceeds1440|Frontend/backend block, no silent UI truncation.|UX-B11；JUnit/PG/component|
|TUX-089|Extra met/calories in request|Strict schema rejects; server authority cannot be replaced.|UX-B11；JUnit/PG/component|


## 21.11 Workout Lifecycle

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-090|Root/two periods/three movements; movement insertion fails|No root/period residue, receipt not committed.|UX-B13；PG/E2E|
|TUX-091|Save then refresh/relogin|One Workout card expands periods/movements; card count does not change minutes.|UX-B13；PG/E2E|
|TUX-092|Only load/sets/speed correction after weight update|Energy snapshots retained; rootversion increments.|UX-B13；PG/E2E|
|TUX-093|Period duration/root date correction|Recalculate affected inputs; move statistics old→new date.|UX-B13；PG/E2E|
|TUX-094|Soft-delete Workout|All periods excluded, movements invisible, original receipt retained.|UX-B13；PG/E2E|
|TUX-095|Old exercisePUT edits grouped period|GROUP_EDIT_REQUIRED, no partial bypass.|UX-B13；PG/E2E|
|TUX-096|Log again old50+20min/movement loads|New root/period IDs, structure reused, duration blank; explicit Use last details.|UX-B13；PG/E2E|
|TUX-097|Modify reused movement/minutes|Old Workout structure/period snapshots unchanged.|UX-B13；PG/E2E|


## 21.12 Saving, Navigation, and No Cross-Refresh Recovery

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-098|Successful food201 complete proof stays AddFood|Clear submitted draft, usable search/Viewrecords, no separate success page.|UX-Q04；PG/E2E|
|TUX-099|Successful Workout201 complete proof stays AddExercise|Empty Gym draft, no copied duration, continue/view.|UX-Q04；PG/E2E|
|TUX-100|Save todayLunch|New todayLunch, not clock-derived Dinner.|UX-Q04；PG/E2E|
|TUX-101|Save pastLunch|New todayLunch; explicit Continue backfill restores past date, no silent reuse.|UX-Q04；PG/E2E|
|TUX-102|Successful POST/proof then overview GET fails|Saved/read-failure notice, no second POST or submitted-draft restoration.|UX-Q04；PG/E2E|
|TUX-103|Double click/concurrent same ID|One create/one replay, identical typedproof, no duplicate leaves.|UX-Q04；PG/E2E|
|TUX-104|Same ID changed portion/optional equipment value|IDEMPOTENCY_CONFLICT; original receipt unchanged.|UX-Q04；PG/E2E|
|TUX-105|Same content/new ID for another actual meal|Valid creation, no content-dedup rejecting legitimate second meal.|UX-Q04；PG/E2E|
|TUX-106|PG committed, proxy drops response|In-memory unknown, same-ID recovery/replay, no new ID.|UX-Q04；PG/E2E|
|TUX-107|201 proof missing root/leaf or owner/epoch mismatch|Do not clear draft/show success; explain recovery state.|UX-Q04；PG/E2E|
|TUX-108|Unknown receipt404 while POST in flight|Do not infer unsaved or auto-create new ID.|UX-Q04；PG/E2E|
|TUX-109|Entry/Review/search internal navigation|No leave warning; draft/selectionTarget retained.|UX-Q04；PG/E2E|
|TUX-110|Leave draft for Dashboard, confirm Discard|Memory cleared, Add no restore; Keep editing unchanged.|UX-Q04；PG/E2E|
|TUX-111|Hard refresh unsubmitted complex dish|No business storage writes/restore; native warning best effort.|UX-Q04；PG/E2E|
|TUX-112|Hard refresh while saving, server may commit|No persisted ID/recovery card/auto-POST; manual History check.|UX-Q04；PG/E2E|
|TUX-113|A response after logout and B login|No A draft/record/notice in B; epoch discards response.|UX-Q04；PG/E2E|


## 21.13 Contracts, Database, Security

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-114|dishId1 and singleId1 in different tables|Distinguish kind+id, not duplicate roots.|UX-Q05；PG/contract|
|TUX-115|Typedproof extra/missing leaf or wrong rootUUID|SQL/service/frontend reject, no false whole-batch acceptance.|UX-Q05；PG/contract|
|TUX-116|Direct UPDATE/DELETE committed receipt|Trigger rejects; original V1hash/JSON remain verifiable.|UX-Q05；PG/contract|
|TUX-117|Direct inconsistent parent/child owner/date/meal transaction|Deferred check rejects whole transaction at commit.|UX-Q05；PG/contract|
|TUX-118|B reads A root/details/preview/recent or writes without CSRF|Reject with existing authorization, not hidden-button-only security.|UX-Q05；PG/contract|
|TUX-119|ADMIN queries private dish/Workout|Admin role does not grant private group access.|UX-Q05；PG/contract|
|TUX-120|Wrong kind/extra nutrition/fake numeric string DTO|Client schema/backend reject, no as any bypass.|UX-Q05；PG/contract|
|TUX-121|OpenAPI changed without generation|Existing check fails; never hand-patch generated files.|UX-Q05；PG/contract|
|TUX-122|Legacy migration fixture soft-deleted/LEGACY/inactive/old receipt|Visible records/nutrition/minutes/hash unchanged; nullable group fields, no inferred legacy groups.|UX-Q05；PG/contract|
|TUX-123|Isolated PG upgrade then restored copy|Verify real values; report oldbinary incompatibility, no blind down-migration.|UX-Q05；PG/contract|


## 21.14 Media and Other Regression

|ID|Scenario/setup|Required result|Primary task/layer|
|---|---|---|---|
|TUX-124|Image model name/g but uncertain reference|No invented recipe; user selects valid reference then shared Review.|UX-F16；component/API/E2E|
|TUX-125|Speech explicitly says150g now|Editable prefill allowed; recent-blank rule cannot erase current input.|UX-F16；component/API/E2E|
|TUX-126|Microphone denied or controlled provider timeout|Clear feedback, manual entry available, no confirmed-record side effects.|UX-F16；component/API/E2E|
|TUX-127|Speech only running30min|QUICK conversion, no invented periods/sets/recipe.|UX-F16；component/API/E2E|
|TUX-128|Q&A no consent/out-of-scope/new-group aggregates|Retain authorization/refusals, no double aggregate/raw health answers sent.|UX-F16；component/API/E2E|
|TUX-129|Registration/onboarding/admin disable-restore/profile units|No regression to Demo; styles preserve role flows.|UX-F16；component/API/E2E|


## 21.A Code Examples: Assertion Strength, Not Existing Implementation

These examples illustrate tests for proposed interfaces. Adapt actual signatures/fixtures during implementation; copying them does not mean they ran.

```java
@Test
void oneThirdAndThirtyThreePointThreePercentRemainDifferent() {
    // Arrange: fixed synthetic official-reference substitutes in isolated test data.
    var recipe = fixtures.recipeWithEnergy3600Kj();
    // Act: use source grams and exact ratio; do not derive expected from the same method.
    var oneThird = calculator.calculate(recipe, Portion.fraction(1, 3));
    var percent = calculator.calculate(recipe, Portion.percent(new BigDecimal("33.3")));
    // Assert: independently computed values; BigDecimal numerical comparison.
    assertEquals(0, new BigDecimal("1200.000").compareTo(oneThird.energyKj()));
    assertEquals(0, new BigDecimal("1198.800").compareTo(percent.energyKj()));
}
```

```text
Workout integration test sequence:
1. Create synthetic USER with dated weight70kg, references3.5MET and5MET.
2. Preview50min Strength(three named movements) +20min Activity.
3. POST V2 using the exact preview and new submission/root/period UUIDs.
4. Assert roots=1, period leaves=2, movement rows=3, totalMinutes=70.
5. Assert stored period energies854.233 and488.133, summary1342.366.
6. Change only a movement's load, while user's newest body weight changes.
7. Assert rootversion increments, stored period energy values remain unchanged.
8. Retry original create using original submissionID; assert original proof returned,
   not a second workout and not current mutable-history data pretending to be original proof.
```

Component tests verify events/request counts, not only snapshots: no authoritative save before approximate selection; grams empty before Use last grams; fake timers for three-second feedback; no second POST after saved/GET failure; deep ingredient changes never mutate frozen payload.

## 21.B Actual Commands and Discovery Rules

**Run Branch Gate before commands writing files/databases.** These come from current scripts; verify dependencies/environment first. No remote Git required:

```bash
# Contract edit -> generated TS/Zod; normal check without --write must detect drift.
python3 scripts/generate_api_types.py --write
python3 scripts/validate_contract.py
python3 scripts/generate_api_types.py

# Current scripts/check.sh uses --stage, not a positional stage argument.
sh scripts/check.sh --stage static
sh scripts/check.sh --stage frontend
sh scripts/check.sh --stage data

# Only with explicit, isolated TEST_DB_* configured privately:
sh scripts/check.sh --stage backend

# Only after owned isolated app+DB+browser setup:
sh scripts/check.sh --stage browser
```

`--stage all`/default excludes browsers; all success is not browser PASS. Backend requires `TEST_DB_ADMIN_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD/TEST_DB_DOCKER_CONTAINER`; browser additionally needs `FITNUTRI_E2E_DB_NAME` and synthetic admin credentials as read by scripts. Never put real passwords in docs.

Targeted task tests are allowed; run adjacent regression before review:

```bash
# Future tests named here are created by the paired tasks, not present today.
./backend/mvnw -f backend/pom.xml -Dtest=PortionNormalizerTest,DishCalculationTest test
./backend/mvnw -f backend/pom.xml -Dit.test=FoodEntryBatchV2IT verify
npm --prefix frontend test -- tests/component/FoodComposer.test.tsx
npm --prefix frontend run typecheck:test
npm --prefix frontend run lint
npm --prefix frontend run build
```

Surefire runs `*Test`; Failsafe binds integration-test/verify. Confirm actual `*IT` reports. Targeted IT commands may also run units, not necessarily one layer. Zero execution, skips, or discovery errors are not PASS even with successful commands.

Record actual JUnit XML, Vitest output, browser reports, SQL assertion locations in verification docs. HTML/screenshots are runtime evidence; do not commit huge node_modules/target/DB dumps. Continue available layers when browser/network/PG is absent; inability to run is not a source defect, and static checks are not integration.


# 22. Final User Flows, Acceptance, and Deliverables

## 22.1 Ordinary Food Demonstration

1. Register a real synthetic USER; success fades within three seconds. Log in/onboard; Allow checkbox aligns to first line.
2. Dashboard Add is clearly clickable; trends with numeric axes appear below metrics.
3. Add food, today/Lunch, everyday search terms, real specific reference. Never automatically select first result or interpret unknown as no oil.
4. Enter g, inspect latest server preview, Add to meal; search is usable again. Add second item with inherited Lunch, independently editable.
5. Inline grams; delete/Undo affect draft only; Review has no per-item Confirm boxes.
6. Review permits grams/meal changes; reference replacement searches then returns to original item; one final Confirm & save.
7. Complete server proof clears draft and leaves searchable Add food. Short toast disappears, View saved records remains.
8. Today/Lunch persists after today's save; past saves reset to today with explicit continue-old-date entry. View records locates past date despite default seven-day filter.
9. Recent reuse keeps exact reference, blank g until explicit Use last grams or manual entry.

## 22.2 Composed Dish Demonstration

1. No suitable whole dish: Record by ingredients, user dish name, no invented standard recipe.
2. Default PERSONAL with actual ingredient grams; or explicitly Full recipe and whole amounts plus fraction1/3/percentage/servings.
3. MyMeal shows dish and rice as separate items; expansion distinguishes whole input/current portion. Totals count personal intake only.
4. Review/save together; durable History dish grouping, confirmed server records persist after refresh.
5. Correct one historical dish portion/recipe/ingredients; only that occurrence changes, not same-name dishes or original proof.
6. Log again creates draft, shows old recipe, explicit Use last recipe, new portion selected again; personal old grams also require explicit reuse.
7. Confirm whole-dish deletion; all children leave statistics together, no residue.

## 22.3 Quick Exercise Demonstration

1. Add Exercise defaults Quick; search sports/aerobic equipment/common strength movements.
2. Date/minutes required; applicable equipment/sets/load optional, no required intensity.
3. No applicable reference explicitly means estimate unavailable but valid Review allowed; reference service failure differs.
4. Confirm actual save, remain Add Exercise to continue/View records; no automatic old duration.
5. Recent selects same activity; explicit Use last duration.

## 22.4 Gym Workout Demonstration

1. Switch Gym, name workout (neutral default permitted), add50-minute Strength period.
2. Select bench press/chest press machine/cable fly; load/sets/reps optional, no50 minutes per movement.
3. Add separate20-minute aerobic period, incline walking/rowing etc.; applicable equipment details do not promise more accurate estimates.
4. Total70 minutes, estimates from two nonoverlapping periods; movements are details only.
5. Review and atomic save; return to Gym editor with fresh duration, one expandable History Workout.
6. Load correction retains historical energy; duration/date corrections recalculate affected periods under rules.
7. Log again creates blank-duration draft with explicit Use last duration/details; edits/deletion affect this occurrence only.

## 22.5 User-Accepted Exceptional Boundaries

Ordinary drafts disappear on refresh. Forced refresh during save also retains no submission ID; inspect History manually. Native leave warnings are browser-limited and not guaranteed. **Server receipts remain idempotent, but without the original client ID a manual reentry is a valid new request; perfect intent-duplicate detection cannot be promised.** Do not secretly use sessionStorage for reliability or prohibit legitimate same-day/same-content second entries.

While the page remains open, saving/unknown freezes payload and retries safely with the same submission ID; in-flight same-ID content cannot change. Post-save read failures retry GET only, never POST.

## 22.6 Definition of Done

Per task: requirement/BC mapping, code and paired tests, applicable tests discovered/run, schema/generated synchronization, no unexplained failures, correct branch, no data/permission/legacy regression, traceable commits/evidence.

Release acceptance: original U01–U08 verified; real food/dish/quick/workout journeys; precision/atomicity/owner/version/legacy/receipt isolated-PG tests; visual/browser results. Advanced functions lacking live-model prerequisites reported separately, without blocking independently testable manual paths.

AI may report implementation/self-test/review-pending. Peer Review/UAT pass only with actual peer/user confirmation. Compilation, Demo success, or provider mocks cannot substitute.

## 22.7 Delivery Records and Source Scope

Update:

```text
docs/implementation/ux-v3-baseline.md
docs/implementation/ux-v3-tasks.md
docs/implementation/ux-v3-backend-changes.md
docs/verification/ux-v3-test-matrix.md
docs/verification/ux-v3-results.md
docs/verification/ux-v3-migration.md
docs/verification/ux-v3-browser.md
docs/verification/ux-v3-uat.md
docs/verification/ux-v3-final.md
```

Reuse/link equivalent logs to avoid conflicting duplicates. These are implementation evidence, not another plan the user must find. **This file is the sole implementation specification.**

Final report: actual branch/HEAD, local commits, frontend/backend files, actual14 BC changes, migration/resource versions, commands/counts, blockers, four reproducible journeys. Runtime keys via environment, no README passwords; synthetic development accounts follow private-env rules.

# 23. Sole Ready-to-Use Coding-AI Start Prompt

Place this file at `docs/implementation/FitNutri_UX_Refinement_Execution_Plan_v3.md` or the user's explicit path. **Do not use old prompts to override confirmed choices.** Copy into a coding session:

```text
You own the next food/exercise/frontend UX round in existing FitNutri.
The sole execution specification is FitNutri_UX_Refinement_Execution_Plan_v3.md (3.0 FINAL).
Read fully: requirements, baseline, APIs, DB, cards, tests, Git are included.
No second execution MD or new plan from scratch is needed.

Baseline is actual local working tree corresponding to latest Lab_practical_05_Group_6.zip,
not early main*.zip or standalone Demo. Reuse sessions, CSRF, transactions,
idempotency, historical-weight estimates, editing, providers, tests; do not rebuild.

Before writes run Branch Gate. Only develop/yuankang-wei is allowed.
Check Git root/branch/HEAD/uncommitted changes/ZIP differences.
If unsafe to verify/switch, report; no main/others' branches,
init/stash/reset/clean/substitute branch or overwriting existing work.
No university GitHub access, fetch/pull/clone, unauthorized push/merge.

Frozen requirements must not change:
- Food match first, ask only as needed; users select approximations, unknown preparation never guessed.
- Continuous Add to meal, inherited meal default; inline g/meal in MyMeal and Review.
- Remove per-item Confirm boxes, retain specific valid references/current preview/final Confirm&save.
- Persistent dish grouping, default personal ingredients/optional full recipe, exact rational portions/servings.
- Explicit Use last recipe/grams; reselect current portion; corrections affect one occurrence.
- Food/Exercise success stays actual creation editor, clears saved draft, continue/View records.
- Preserve today/meal; historical success restores today with explicit previous-date continuation.
- Draft/submission IDs memory-only, no refresh recovery or secret sessionStorage/URL persistence.
  Keep server idempotency and current-page safe unknown retries; manual History after refresh.
- Exercise E1-C/E2-B/E3-B/E4-B/E5-A/E6-A/E7-A/E8-A:
  Quick+Gym, multiple activities; strength time counted once; optional sets/reps/load;
  optional equipment details do not change MET; no required intensity;
  explicit recent duration reuse, persistent Workout grouping/correction/delete/reuse.

Execute chapter20 dependencies from UX-P00. Pair business tasks with automated tests:
assertions, implementation, execution, self-review, small commits on required branch.
Existing JUnit/Mockito/MockMvc; isolated PG16 for transactions/constraints;
existing Vitest/contracts/Playwright. Do not postpone unit tests to Q.
Never hand-edit generated contracts.ts/schemas.ts; change OpenAPI and generate.
No applied-migration edits, DB wipes, deleting failures, disabling authorization for PASS.
Register each UX-BC cause/difference/compatibility/migration/actual result.

First report actual root/required-current branch/HEAD/existing changes/ZIP differences,
Branch Gate and regression baseline. After PASS code directly; do not re-ask confirmed choices.
Per task report files/tests/commands/pass-fail-skip counts/BC-DB impact/commit/blockers.
Separate Developer Tests, Peer Review, UAT. Claim completion only when verified; no proxy signoff.

Missing mappings never justify invented MET/nutrition. Mark external blockers and continue
safe independent tasks. Stop/report affected writes for non-lossless DB/permission conflicts.
Deliver real usable changes and evidence through all four journeys.
Start Branch Gate and UX-P00–UX-P03, then continue dependencies.
```

---

# Appendix A. Requirements-to-Code/Test Index

|Requirement|Chapters|Direct tasks|Combined verification|
|---|---|---|---|
|Registration notice, Allow alignment, clear buttons|5.6,18|F00/F01|Q07|
|Dashboard/History trend priority/axes|17,18|F02/F13/B15|Q03/Q07|
|Smart matching/hidden technical detail/full catalogue|6|D00/B00/B01/F05|Q01/Q07|
|Recent/explicit quantities|7|B02/F05/F14|Q01|
|Continuous add/meal defaults/inline Review|4–5,10|F04/F06/F08|Q01|
|Dish/personal/full/portions|8,11,13–15|D01/B03/B04/B06/F07|Q01/Q03/Q06|
|Dish correction/Log again/delete|7–8,14–16|B07/B08/F13/F14|Q01/Q04/Q05|
|E1 Quick+Gym|9–10|B09–B14/F10–F12|Q02|
|E2 optional strength details|9.5|D03/B10–B13/F11|Q02/Q03|
|E3 optional equipment/E5 no intensity|9.3–9.6|B09/B11/F10|Q02|
|E4 count time once|9,11,17|D03/B11/B15/F11|Q02/Q03|
|E6 recent/E8 grouped history|7,9,17|B14/F13/F14|Q02|
|E7 stay after save/Food date C|4.8,16|F09/F12|Q01/Q02/Q04|
|User rejects cross-refresh recovery|16,18.2|F15|Q04|
|Backend idempotency/precision/legacy|11–16,19|D01–D04/B03–B07/B12–B13|Q04/Q05/Q06|
|Advanced entry/permissions/admin retained|17.4–17.5|F16/B15|Q05|

UX- prefixes omitted for width only; formal task IDs include UX-. Every task has direct tests; combined verification does not defer testing.

# Appendix B. Current Source Evidence (Not Proof Proposed Classes Already Exist)

These files existed in the latest ZIP; line numbers refer to that snapshot. Only selected symbols are located. Proposed new classes are not misrepresented as existing. Hashes help compare differences, never force-overwrite newer personal-branch files.

|File|Key original lines|First12 SHA-256 characters|
|---|---|---|
|`frontend/src/pages/AuthPage.tsx`|notice: 12,45,49; Account created: 49|`03bcc8977ae2`|
|`frontend/src/features/profile/ProfileSections.tsx`|Allow FitNutri: 33; FormControlLabel: 2,16,33|`6f7c1943250c`|
|`frontend/src/pages/DashboardPage.tsx`|IntakeChart: 13,27; MetricCard: 11,26|`8c76659dd2b5`|
|`frontend/src/pages/HistoryPage.tsx`|HistorySummary: 11,26; RecordList: 5,24|`bf3a1b23ebd2`|
|`frontend/src/components/IntakeChart.tsx`|<svg: 8|`9f8730b0793f`|
|`frontend/src/features/entry/FoodReferencePicker.tsx`|publicFoodKey: 21; Selected: 27|`6ac00f582a21`|
|`frontend/src/features/entry/FoodItemEditor.tsx`|referenceConfirmed: 31; gramsConfirmed: 32|`29fe0b58fb62`|
|`frontend/src/features/entry/draftTypes.ts`|FoodDraft: 12,16; Submission: 1,18,19|`bc933e5c57b9`|
|`frontend/src/features/entry/draftReducer.ts`|matchesDraft: 20,91; confirmed: 8,24,44|`cb4e087e2898`|
|`frontend/src/hooks/useSubmitDraft.ts`|receipt: 3,6,7; invalidateQueries: 15,28|`1784aa759fac`|
|`frontend/src/app/DraftProvider.tsx`|useReducer: 1,9; useRef: 1,10|`73c4a133d138`|
|`frontend/src/components/UnsavedChangesGuard.tsx`|unknown: 10,13,16|`2daa69492726`|
|`backend/src/main/java/com/fitnutri/backend/foodreference/FoodReferenceService.java`|searchActive: 16; displayName: 26,29|`6abba7ab0857`|
|`backend/src/main/java/com/fitnutri/backend/foodreference/FoodCalculationService.java`|calculateHistorical: 17,24; HALF_UP: 32|`65c484719c03`|
|`backend/src/main/java/com/fitnutri/backend/records/FoodRecordService.java`|confirmBatch: 21,22; withSubmission: 25|`978ee6a6eb57`|
|`backend/src/main/java/com/fitnutri/backend/records/ExerciseEstimateService.java`|weight: 18,19,29|`7f8636c4f7c1`|
|`backend/src/main/java/com/fitnutri/backend/submission/SubmissionService.java`|TransactionTemplate: 21,22; withSubmission: 25|`6127b86efd2b`|
|`backend/src/main/java/com/fitnutri/backend/submission/CanonicalSubmissionHasher.java`|food: 18,20,21; exercise: 28,29,30|`97ecee6b37af`|
|`backend/src/main/java/com/fitnutri/backend/summary/SummaryRepository.java`|SUM: 1,3,11; food_record: 19|`86a7881f385b`|
|`backend/src/main/java/com/fitnutri/backend/qa/QaContextBuilder.java`|summary: 4,5,10; profile: 10|`2eff4b35ff5c`|
|`scripts/generate_api_types.py`|anyOf: 15,27; --write: 3,57,59|`be900adbad9c`|
|`scripts/check.sh`|--stage: 8; backend_checks: 32,50,51|`6b29667cb6a2`|
|`frontend/package.json`|test:e2e: 24; test:legacy: 19,21|`b76e99ffc6c4`|
|`backend/pom.xml`|4.1.1: 8; maven-failsafe-plugin: 112|`0cab6fd72cbb`|


## B.1 Static Inspection Performed

Read original self-test Markdown and safely inspected the latest ZIP's source, dependencies, real services, drafts, SQL migrations, and resources. Resource counts use the `foods` array **3741** and `activities` array **15**. Old Demo/main-backend issues were not assumed to apply to this implementation.

**No application build, JUnit/frontend/E2E tests, user database startup/migration, user Git-tree operation, or real-provider call was performed in that preparation round.** Test tables are implementation acceptance requirements. Old verification successes are historical evidence only.

Document-generation checks cover structure, dependencies, source paths, and arithmetic examples, not passing project tests. User project files remained unchanged during that preparation.

# Appendix C. Sources, Course Methods, and Engineering Judgment

**S1 Source baseline:** latest `Lab_practical_05_Group_6.zip`; SHA/archive branch in chapter0, locations in Appendix B. Original nutrition/activity resources, import scripts, source-lock underpin validation; similar display names do not establish official equivalence.

**S2 Original user issues:** self-test issue record dated1017: success feedback, onboarding Allow alignment, clickable entries, Food IDs/Confirm/redundant sources, Exercise catalogue/technical noise, both trend layouts. Referenced local Typora screenshots were not supplied; no pixel conclusions about unseen images.

**S3 Final requirements:** all sequential Food/dish/recent/Review/save/date/draft decisions and E1-C/E2-B/E3-B/E4-B/E5-A/E6-A/E7-A/E8-A. Frozen in chapter2. Unselected technical parameters are engineering defaults, not instructor grading rules.

**S4 Existing development rules:** old `FitNutri_AI_Integration_Execution_Plan.md` and runbook in latest ZIP were consulted for branch/test/code requirements. Applicable rules are included in chapter1; executors need not assemble old plans. No prior course requirements are deleted and UX scope does not become a complete backend rewrite.

**S5 Instructor testing materials:** `ELEC5619-Lab9.pdf` pp2–3 JUnit5/Mockito, Shift-left, AAA, meaningful assertions; pp3–5 MockMvc/JPA/cross-layer integration; p7 UAT; `L9 Testing (1).pptx` pp6–8 developer unit tests/TDD. Adapt actual Boot4 APIs, do not force downgrade. These materials do not establish95% as a mandatory grading threshold. Respect Lab6/Lab7 layering, authorization, task/contribution/review methods; classroom JWT exercises do not mandate changing authentication here.

**S6 Official boundary references (accessed2026-10-17):**

- MDN `beforeunload`: https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event . Browser restrictions prevent guaranteed custom warnings on every refresh/close; they do not authorize retaining drafts the user rejected.
- 2024 Adult Compendium overview: https://pacompendium.com/ . Cite actual sources; never invent or mix/alter MET.
- Conditioning Exercise: https://pacompendium.com/conditioning-exercise/ . Strength/equipment description boundaries; detailed movements do not imply machine-specific values.
- Walking: https://pacompendium.com/walking/ , Running: https://pacompendium.com/running/ , Bicycling: https://pacompendium.com/bicycling/ , Sports: https://pacompendium.com/sports/ . Verify complete descriptions/conditions for each mapping using the project's fixed version, not arbitrary search-snippet MET.

These support browser/activity-reference boundaries, not replacement of confirmed UI requirements or technology. Product categories, structures, precision, endpoints, and task divisions are architectural choices, not uniquely mandated by websites/instructors.

# Appendix D. Explicit Exclusions

No training prescriptions, dietary recommendations, medical judgments, AI recipes, manual machine-calorie overrides, user-invented official nutrition, barcode/nutrition-label OCR, automatic bowl/cup conversion, cross-device recipe library, per-set rest timers,1RM/volume analysis, precise raw/cooked yield models, refresh draft recovery, or server personal-draft table.

Do not delete valid existing functionality to reduce scope. Preserve image/speech/Q&A/admin/profile/statistics through regression. Optional exercise details aid recording, not guaranteed energy accuracy. Do not hide unverified external services, browsers, peer review, or UAT behind “all real functions complete”.

**Delivery direction: preserve the real system and incrementally implement confirmed interactions; simpler ordinary entry with detailed dishes/whole workouts when needed; rigorous sources, counting, transactions, authorization, and historical snapshots.**
