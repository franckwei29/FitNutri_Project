# FitNutri — Save Feedback, Return Navigation, and Record Presentation Execution Specification v3.1

> English handover edition (2026-10-18): this historical specification retains its original baseline, task statuses and instructions as evidence. It does not grant new execution authority or certify the current implementation. Current submission prose is English; functional multilingual inputs are retained. For installation use [the fresh-start guide](../getting-started.md), and for this handover use [the validation report](../verification/team-handover-validation.md).

> Date: 2026-10-18 (Australia/Sydney)
> Status: APPROVED / implementation authorized by the user on 2026-10-18
> Authorization: continuous development, accompanying tests, self-review, and local commits approved; push prohibited.
> Start condition: explicit development authorization after user review. Tasks, commands, and prompts in this document do not themselves constitute current execution authorization.

## 0. Purpose, Sources, and Priority

### 0.1 Objectives

Complete the new-record experience: **Entry → Review → Confirm actual save → Dashboard → Green floating success feedback → Correct daily data**. Also refine Dashboard/History record cards, entry-page headers, and recent-item areas.

Retain React / TypeScript / Vite / MUI and Spring Boot / Spring Security Session / PostgreSQL. Reuse existing domain models, receipts, roots, sources, and test infrastructure. This is bounded interaction/presentation work, not reimplementation of completed v3 full-stack functions.

### 0.2 Inputs and Authority

| Source | Purpose and authority |
|---|---|
| User confirmation during preparation | Agreed direction; specification first, no development yet |
| Subsequent authorization on 2026-10-18 | Continuous v3.1 implementation, tests, local commits; no push |
| Original interaction-refinement source document dated 1018 and its four screenshots | Evidence of save destinations, floating feedback, cramped/obscured cards and weak hierarchy; not independent code-execution authorization |
| `FitNutri_UX_Refinement_Execution_Plan_v3.md` | Existing business/architecture/authorization/data/testing/development baseline |
| Actual working tree | Implementation facts; never overwrite from an old ZIP or plan |
| This document | Listed v3.1 behavior changes, boundaries, tasks, acceptance |

Priority: subsequent explicit user instructions > approved differences here > uncovered v3 rules > historical materials. This incremental specification does not require rereading/executing the v2 historical plan. Unaffected functions continue under v3.

### 0.3 Read-Only Baseline

- Project root: `/Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6`
- Required/current branch: `develop/yuankang-wei`
- Inspected HEAD: `139236d734434bef54289a27ef785e7b501f4f9c`
- Tracked diff: none at inspection; preserve untracked `.DS_Store`, `.idea/`, `.local/`, and backend `.DS_Store` files.
- Read-only check passed; rerun Branch Gate before implementation, not a permanent authorization.
- This records static code/screenshot facts; no application, business tests, or human acceptance were rerun.
- Historical v3 test counts cannot serve as v3.1 PASS evidence.

## 1. Frozen Changes and Scope

### 1.1 Confirmed Product Direction

| ID | Requirement |
|---|---|
| RF-01 | Return to Dashboard after successful new food, Quick exercise, Gym workout saves |
| RF-02 | Green nonblocking floating feedback without displacing content |
| RF-03 | Fade after approximately 2 seconds of actual display; once per save |
| RF-04 | Express save success in concise AU English, never Loading successful |
| RF-05 | Historical entries return to today's Dashboard with explicit saved date and History access |
| RF-06 | Dashboard/History records have hierarchy, spacing, clear actions, no clipped text |
| RF-07 | Prominent details action; edit/reuse/delete in a text-labelled more menu |
| RF-08 | Sources/calculation basis/save facts accessible in details, not repeated on every summary |
| RF-09 | Compact entry header/recent items; clear Quick/Gym selection |
| RF-10 | Editing retains original viewing/filter context rather than new-record return-to-today |

Dimensions, timing tolerance, component divisions, and test IDs below are engineering defaults, not instructor requirements or new backend rules; implement them after approval.

### 1.2 Precise v3 Overrides

| v3 location | v3.1 handling |
|---|---|
| §4.8, §5.5, UX-F09, UX-F12 stay-on-entry/continue-after-save | Successful creations go to `/dashboard` without historical `date` |
| Entry `Just saved` / `View saved records` area | Remove from new-save flow; destination has light feedback and persistent current-save entry |
| Continue recording on previous date | No longer post-save default; new entry defaults today, historical date selectable |
| §17 Dashboard/History root cards | Retain reads/statistics/identity/details; rearrange presentation/actions |
| §18 save feedback/accessibility | New-save timing/floating rules here; registration/profile notices not automatically two seconds |
| §21 old navigation/wording assertions | Mark requirement changes and replace; preserve idempotency/unknown/auth/no-persistence assertions |
| Other business/algorithm/DB/AI/authorization/receipt rules | Unchanged |

### 1.3 Explicit Exclusions

No new nutrition algorithms, model services, site-wide languages, DB tables, backend services, or framework upgrades. No historical snapshot changes/recalculation or changed completion status for unconfigured AI. Not a site-wide redesign; authentication/onboarding/Profile/Admin/Q&A still require regression.

## 2. Development, Git, and Data Rules

### 2.1 Branch Gate

Check at start, resumption, before each package write and commit. Initially run in the actual project:

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

Only `develop/yuankang-wei`. Switch only when the target exists and switching safely preserves changes, then recheck. Missing Git/branch, unsafe switching, or another worktree owning it means `BLOCKED_BRANCH`: report and await user. Never init, substitute a branch, or stash/reset/clean to bypass.

No main/master/shared develop/others' branches/detached HEAD development. No fetch/pull/clone/push or PR creation/merge. Preserve untracked files and others' code. Do not proactively spawn agents or have multiple executors write concurrently.

### 2.2 Code Standards

Current lockfile/POM defines versions; no major dependency upgrades. Strict TS; no any/casts hiding errors. Retain Zod, TanStack Query, React Router. Theme-consistent scoped styles, not global `!important`. No runtime API/service state/database passwords embedded in frontend.

Do not hand-edit generated types. Necessary contract changes start with OpenAPI then generation/validation; expected change is none. Necessary Java fixes preserve thin Controllers, Service transactions, constructor injection, BigDecimal, LocalDate, Instant, package structure.

### 2.3 Package Closure

1. Read actual code/acceptance and affected old tests.
2. Write/update key assertions first; justify obsolete assertions from requirements.
3. Implement minimum necessary changes.
4. Run/fix direct tests immediately, not deferred to final QA.
5. Check permissions/navigation/old interfaces/data/other callers.
6. `git diff --check`, branch check, explicit staging, staged-diff review.
7. Commit behavior and tests together as a reviewable package; update ledger.
8. Continue by dependencies without waiting each batch; classify real blockers.

Conventional Commits: `type(scope): concrete change [UX31-ID]`. Body: Why, What, Backend-Change, Database-Change, Contract, Tests (actual commands/counts/results/environment), Limitations. Example: `fix(entry): return confirmed creations to dashboard [UX31-F02]`.

### 2.4 Safety and Completion

Do not read/print key files or commit real health data/media/accounts/dumps/private manifests. Use explicitly owned isolated PG and synthetic accounts only. No wiping databases/volumes, Flyway clean/repair bypasses, applied-migration rewrites, or disabling authorization/CSRF for tests.

Statuses: TODO / IN_PROGRESS / BLOCKED_BRANCH / BLOCKED_ENV / BLOCKED_EXTERNAL / IMPLEMENTED_NOT_VERIFIED / IN_REVIEW / ACCEPTED. Author self-tests justify at most IN_REVIEW; separate Peer review/UAT, never sign for users/team members.

## 3. Navigation and Save Contract

### 3.1 Creation Flow

`Add → Entry → Review → Confirm → verified receipt → /dashboard`.

Emit success only after receipt validation, matching owner/epoch, and state-machine acceptance. Clicking, sending HTTP, a bare 2xx, candidate generation, or draft addition is not success.

Applies to ordinary food, mixed SINGLE/DISH batches, composed dishes, Quick, Workout, and image/speech candidates adapted into the same review path. Recognition alone never navigates/saves. A mixed batch produces one submission/navigation/notice, not one per root.

Use replace to avoid returning to a submitted Review; do not claim it erases all browser history. Back to earlier Entry must be safe without submitted content. Completed saves must not trigger false unsaved-draft guards; new drafts and unknown outcomes retain protection.

### 3.2 Dates and New Drafts

- Destination fixed `/dashboard`, remove old `?date=`. Determine today using existing business timezone, not UTC string slicing.
- Today: update summary/list/trends through actual queries.
- Historical: retain actual recordDate, never copy to today or increase today's total. Show explicit date and History link with correct date/typed roots.
- Around midnight, saved date follows confirmed payload; destination today follows business clock.
- Retain empty-draft/new-client-ID validation and permitted meal defaults. Never carry old quantities/ingredients/durations/receipts into next submission.
- Do not directly delete lastSaved or bypass reducer for navigation; preserve refresh-retry/receipt facts.

### 3.3 Edit, Delete, Reuse

Edits return to compatible History filters/date/highlight; dates outside range use existing correctedRootLocation semantics. Other entry points retain existing details-return behavior. Do not force all corrections to Dashboard.

Delete retains whole-root confirmation, versions, unknown handling; menu only relocates entry. Log again verifies current references, creates an unsubmitted reviewable draft, and returns to Dashboard only after final new-save success.

### 3.4 Exceptional States

| State | Destination | Feedback/recovery |
|---|---|---|
| saving | Stay Review | Busy, prevent duplicates, retain leave guard |
| Explicit rejected (validation/calculation changes) | Stay Review | Preserve input, actionable correction, review updated preview |
| unknown / receipt mismatch | Stay Review | Original submissionId/frozen payload, original verification/safe retry |
| Receipt temporarily 404 | Stay Review | Do not infer failure or create new ID |
| recover then verified | Dashboard once | Same success-event path |
| Reads fail after verified | Still Dashboard | Separate save success/read failure, persistent warning/read-only retry |
| Success arrives after logout/account switch | No navigation/old-user message | Discard old owner/epoch effects |
| Destination code load fails | Existing route error boundary | No repeated POST; retain in-memory success for session retry; after hard refresh inspect History |

## 4. Success Notice and Destination Feedback

### 4.1 Style and Wording

Use MUI Snackbar/Alert or equivalent existing-theme component in one application-level host. Desktop: right below navigation; narrow screen: centered in top safe area, at least 16px sides. No modal overlay, document-flow change, or stolen focus.

Defaults: `Food saved`, `3 food entries saved`, `Exercise saved`, `Workout saved`. Historical date adds `Saved for 17 Oct 2026`. Use actual roots, not ingredient leaves, and existing date formatters, not screenshot dates.

### 4.2 Timing and Deduplication

- Start on first actual Dashboard display: 2000ms foreground visibility plus default 200ms fade; reduced motion removes fade.
- Not from API return; do not wait for every Dashboard query.
- Delay initial display while backgrounded; pause remaining visibility if hidden after display. Ordinary rerenders do not reset.
- StrictMode, duplicate effects/proof recovery, route remounts cannot repeat. Identify by owner + epoch + submissionId, optionally existing noticeId.
- Keep display/consumption in session memory, never localStorage/sessionStorage/IndexedDB/window.name/persisted query cache.
- Assert visible before 2000ms, removed after default 2200ms; record real-browser scheduling tolerances. Fake clocks for precision, not arbitrary timeout expansion.
- Toast cannot contain the only important action. Layer correctly with other operations; never cover confirmation dialogs.

### 4.3 Persistent Current-Save Entry

Dashboard has a compact nonfloating context bar with saved date and `View saved records`, dismissible or replaced by next save. Session-only, not restored after refresh/logout. No continuing `Just saved` on Add.

Use a defined container/spacing with no layout shift when toast disappears. This is not a second auto-disappearing banner. Historical saves need this link so today's list does not suggest data loss.

Refresh failures have separate persistent warning and `Retry refresh`, outside the two-second timer. Rereading cannot create or alter receipts. Clear warning on recovery; loading is not failure.

### 4.4 Focus and Screen Readers

Follow route focus handling, moving to Dashboard heading if needed; never focus toast. Use role=status / aria-live=polite / aria-atomic and avoid duplicate live announcements. Buttons have readable names; color is not sole success signal.

## 5. Record Cards and Information Architecture

### 5.1 Shared Summary Cards

Name/energy first, portion/duration second, meal/source supplementary. Retain actual names and units. Long names may wrap, never lose essential content; if locally truncated, details must show complete name.

Food: name, consumed portion/mode, energy, protein/carbs/fat; dishes show ingredient count without expanding leaves. Exercise: name, total duration, known estimate, segment count; partial explicitly indicates some segments lack estimates; null never becomes 0. Distinguish expenditure from intake.

Details retain ingredients, portions, segments, sets/load, sources, calculation basis, saved date/version. Friendly `Record information` / `Data sources` headings may replace technical headings without deleting audit facts.

### 5.2 Dashboard

Retain statistics/trends/rules and server-authoritative aggregates. Refine Food log / Movement containers/cards. Wide screens two columns, narrow one; independent natural heights without fixed-height gaps/clipping.

Consistent title/card inset, no overlapping outer rounded borders/inner borders. Clear separation. Retain pagination/totalMatches; loaded page is not total records. Clearly show load-more availability.

### 5.3 History

Retain date/meal/exercise groups, filters, trends, pagination, typed highlights/deep links. Compact readable rows/cards with max width, not four metrics spread pointlessly across wide containers.

Existing details routes/dialogs remain shareable; close restores list/focus. Share cards with explicit density variants; do not duplicate calculations or record models.

### 5.4 More Actions

Prominent `View details` or equivalent; `More actions` contains Edit entry, Log again, Delete entry. Accessible trigger name includes record name.

Retain EntryRootActions loading locks, owner/epoch, draft replacement confirmation, pending-operation locks, delete confirmation, errors. Errors stay visible after menu unmount. Escape restores focus; dialogs own focus while open and return appropriately. Deleted trigger: focus next record in group or group heading.

### 5.5 Visual Defaults

- Existing colors/fonts; ordinary-text contrast at least 4.5:1; no faint essential text.
- Desktop padding 20–24px, narrow 16px; card gaps 12–16px.
- Names approximately 18–20px semibold, primary values 22–28px, body 14–16px; small text readable.
- Touch targets preferably 44×44px; min-width:0, sensible wrap/grid, no fixed body height.
- Scoped `.saved-entry-root`/dedicated classes, not whole-site `.card`/`.actions` changes.
- Correct menu/Tooltip portals/z-index; parent overflow:hidden must not clip focus rings.
- Test 360/375/768/1280/1440px, 200% zoom, long English/Chinese names, empty/many records, unknown energy.

## 6. Entry Header and Recent Items

Date/meal reasonably side-by-side wide, stacked narrow. Preserve labels/errors/keyboard access; placeholders are not labels. Reduce right-alignment whitespace without squeezing essential inputs.

Recent items become compact clickable lists, name first/date secondary, with local layout rather than inherited right-aligned `.actions`. No large empty container when absent; clear loading/error/retry.

Reuse preserves v3: validate current references, never automatically confirm old portion/duration, focus next required input. Quick/Gym has clear selected tabs/segments; visual switching cannot bypass discard/conversion rules. Dish, ingredient entry, My meal, Review remain visible.

## 7. Code Mapping and Constraints

### 7.1 Existing Files (Static Inspection)

| File relative to root | Current responsibility | Expected change |
|---|---|---|
| `frontend/src/features/entry/food/FoodCreateReviewPage.tsx` | lastSaved navigates to `/entry?kind=food` | Remove old navigation, unified verified-creation event |
| `frontend/src/features/entry/exercise/ExerciseCreateReviewPage.tsx` | Exercise success navigation | Same for Quick/Workout |
| `frontend/src/hooks/useSubmitDraft.ts` | Receipt/reducer/invalidate/recover/retry | Reuse success checks; separate refresh/save outcomes, no duplicate verify/POST |
| `frontend/src/features/entry/submissionV2State.ts` | Frozen envelope/confirm/empty draft/lastSaved | Only necessary display lifecycle fields; retain receipt/identity constraints |
| `frontend/src/features/entry/draftTypes.ts`, `frontend/src/app/DraftProvider.tsx` | Types/session memory | Minimal extension; old owner/epoch actions remain no-op |
| `frontend/src/layouts/AppLayout.tsx` | Navigation guard/Outlet/mutation boundary | Single host respecting session/provider boundaries |
| `frontend/src/pages/DashboardPage.tsx` | Dates/summary/list/refresh | Current-save link/read recovery/cards; preserve today |
| `frontend/src/pages/HistoryPage.tsx` | Filters/root lists/details | Shared cards, deep-link/filter regression |
| `frontend/src/features/history/EntryRootCard.tsx` | Root summary/inline details | Hierarchy/scoped styles/complete detail migration |
| `frontend/src/features/history/EntryRootActions.tsx` | Read/edit/reuse/delete | Menu using original handlers |
| `frontend/src/features/history/EntryRootDetailsDialog.tsx` | Complete root details | All content removed from summary remains accessible |
| `frontend/src/features/entry/food/SavedFoodNotice.tsx`, `exercise/SavedExerciseNotice.tsx` | Stay-on-page/old-date continuation | Remove old new-save display calls, migrate necessary logic; check all references before deletion |
| `frontend/src/components/TransientSuccessAlert.tsx` | Existing 2500ms+300ms | Do not change globally; configure new-save toast separately, preserve registration contracts |
| `frontend/src/features/entry/exercise/RecentQuickExercises.tsx` and food recent/entry components | Recent selection/entry | Scoped compact style with selection/async validation |
| `frontend/src/styles.css` | `.card` lacks basic padding, overflow:hidden; `.actions` right-aligned | Dedicated fixes without other-module effects |
| `frontend/src/app/router.tsx` | Existing `/dashboard` routes | Normally no new routes; verify lazy loading/error boundaries |

### 7.2 Proposed Modules (Not Yet Existing at Planning)

- `frontend/src/features/feedback/saveFeedbackState.ts`: pure lifecycle/identity/deduplication.
- `frontend/src/features/feedback/SaveFeedbackHost.tsx`: single host/timing/visibility/accessibility.
- `frontend/src/features/feedback/SavedRecordContext.tsx`: current-save entry/read warning.
- `frontend/src/features/feedback/saveFeedbackLocation.ts`: date/typed-root History links, preferably existing historySearch utilities.

Files may merge to suit structure, but never create competing save states. lastSaved remains verified fact; feedback stores display state/identifiers only, not a full duplicate health payload.

### 7.3 Lifecycle

1. `acceptV2` still runs verifyReceiptV2/reducer checks first.
2. Project one event from accepted lastSaved with owner/epoch/submissionId/recordDate/typed roots, never unvalidated API responses.
3. One coordinator decides replace navigation, not competing page effects.
4. Dashboard host starts visibility timing; rerender does not renew deadline.
5. Existing owner query keys handle invalidate/refetch. Newly mounted Dashboard failures must be captured, not only pre-navigation invalidate results.
6. Read failure cannot revert submission to unknown/rejected; retry reads only.
7. Account changes clear feedback; duplicate receipts create no message; refresh restores no local business state.

Details may vary, but test lazy loading, StrictMode, late responses, remounts, Dashboard reentry. Do not remove every UnsavedChangesGuard for automatic navigation.

### 7.4 Backend / Database / Contract

Expected Backend Change = none; Database Change = none; OpenAPI Change = none. Existing backend supplies aggregates; no frontend local sums pretending refresh succeeded.

Necessary backend defects require reproduction/minimal diff/interface impact/tests in the change register. Authorized business fixes may proceed; new data semantics/product scope require confirmation. Any DB change requires separate incremental migration, read-only preflight, isolated PG, rollback/recovery evidence. Never alter existing V1–V13 for UI work.

## 8. Task Queue and Dependencies

**16 cards**, initially TODO; creating this document executes none. Sequence below is serial; independent dependencies may reorder, not concurrently write the same tree.

| Task | Package | Dependency | Deliverable focus |
|---|---|---|---|
| UX31-P00 | Branch Gate/baseline | User authorization | Actual branch/diff/runtime ownership/old tests |
| UX31-P01 | Requirements/regression ledger | P00 | Map old stay-on-page assertions to new requirements |
| UX31-F00 | Verified-save event lifecycle | P01 | owner/epoch/dedup/display/empty draft |
| UX31-F01 | Floating host | F00 | Two seconds/fade/once/screen readers/background |
| UX31-F02 | Unified return | F00,F01 | Food/dish/Quick/Workout to today's Dashboard |
| UX31-F03 | Destination refresh/history entry | F02 | Real lists/historical links/read-only retries |
| UX31-F04 | Root summary/complete details | P01 | Name/metrics/portion hierarchy, no lost details |
| UX31-F05 | More menu | F04 | Edit/reuse/delete/focus/locks |
| UX31-F06 | Dashboard records | F03,F04,F05 | Two/one columns, spacing/count/pagination |
| UX31-F07 | History records | F04,F05 | Groups/filters/highlight/detail navigation |
| UX31-F08 | Entry/recent items | F02 | Compact header/selection/input protection |
| UX31-Q00 | Real save/exception regression | F03,F08 | Real DB/lost responses/recovery/no duplicate writes |
| UX31-Q01 | Visual/keyboard/responsive | F06,F07,F08 | Inspected screenshots/zoom/long names/screen-reader semantics |
| UX31-Q02 | Complete regression/compatibility | Q00,Q01 | Frontend/backend/PG/contracts/existing E2E |
| UX31-R00 | Runtime/UAT scripts | Q02 | New-flow docs/evidence/manual pending items |
| UX31-R01 | Final review/receipt | R00 | Branch/commit/cleanliness/status matrix |

### 8.1 Card Execution Definitions

#### UX31-P00 — Freeze Baseline
- Read this specification, retained v3 rules, actual AGENTS if present, Git/environment.
- Deliver `docs/verification/ux-v3-1-baseline.md`: actual intended files, tracked/untracked state, ports, isolated DB ownership.
- Verify read-only Gate and comparable affected-test baseline; record failures. Old ZIP is provenance only, never a reset; no repeated extraction required.
- Backend/DB: none. Exit: correct branch, clear environment boundaries, no concealed baseline failures.

#### UX31-P01 — Requirements and Test Traceability
- Deliver `docs/implementation/ux-v3-1-tasks.md`, `docs/verification/ux-v3-1-test-matrix.md`, `docs/verification/ux-v3-1-results.md`.
- Map RF-01–10 → files → T31 tests → tasks; explain old navigation/date continuation/duration assertion changes.
- Update active spec only after implementation begins; preserve historical v3 completion, neither reopen nor claim recompletion.
- Backend/DB: none. Exit: complete dependencies, unrun tests NOT_RUN.

#### UX31-F00 — Save Event State
- Scope: §7.1 hook/reducer/provider and minimal §7.2 feedback.
- Tests first: T31-01–06,14,15; real state machine, no-op references/identity, duplicate proofs yield no duplicate events.
- Backend/DB: none. Exit: original submission/receipt validation unchanged; no persisted health content in display state.

#### UX31-F01 — Notification
- Scope: host/AppLayout/local styles, no global registration changes.
- Tests first: T31-07–13; fake-clock normal/reduced/background/remount tests; assert placement, not text alone.
- Backend/DB: none. Exit: one live region, no stolen focus, roughly two-second fade-out.

#### UX31-F02 — Return Navigation
- Scope: both CreateReviewPages, shared Review, coordinator, necessary guards.
- Tests first: T31-16–21,27,28, including batches and unknown recovery.
- Backend/DB: none. Exit: confirmed save only replaces to today; Back causes no writes/false guard.

#### UX31-F03 — Refresh and Date Context
- Scope: Dashboard/SavedRecordContext/queries/links/old SavedNotice calls.
- Tests first: T31-22–26,29; read failures before and after navigation.
- Backend/DB: none. Exit: historical data never added to today, persistent viewing link, GET-only retry.

#### UX31-F04 — Cards and Details
- Scope: EntryRootCard/EntryRootDetailsDialog/local styles/pure display components.
- Tests first: T31-30–35; compare all existing detail fields, preserve units/partial/null.
- Backend/DB: none. Exit: clear summaries, complete details, no duplicated calculation.

#### UX31-F05 — Menus
- Scope: EntryRootActions/pure menu presentation, existing handlers.
- Tests first: T31-36–40; clicks/keyboard, rejected/conflict/unknown/focus return.
- Backend/DB: none. Exit: errors survive close, locks cannot bypass, whole-root deletion unchanged.

#### UX31-F06 — Dashboard Integration
- Scope: record area/shared List/Pagination/local CSS.
- Tests first: T31-30,34,41,42; independent totals/root counts.
- Backend/DB: none. Exit: correct columns, no duplicate pages, inset headings/text.

#### UX31-F07 — History Integration
- Scope: History/shared cards/deep links/highlights.
- Tests first: T31-35,37,41–43; date-changing corrections, deletion, close restores filters.
- Backend/DB: none. Exit: correct root identity/hierarchy/scroll/navigation.

#### UX31-F08 — Compact Entry
- Scope: actual food/exercise headers/recent items/mode switches.
- Tests first: T31-44–47; prevent invalid reuse during async validation, retain draft switch protections.
- Backend/DB: none. Exit: no empty blocks, keyboard usable, no silently reused quantity/duration.

#### UX31-Q00 — Real Business Flow
- Scope: existing E2E plus proposed `frontend/tests/e2e/save-feedback-v31.spec.ts`.
- Run T31-16–29,48–50 with real frontend/Spring/isolated PG and actual forwarding fault proxy; directly check DB root/leaf/receipt conservation.
- Backend/DB business changes: none; synthetic test writes allowed. Exit: no mocked success posing as integration or silent duplicate POST.

#### UX31-Q01 — Visual and Accessibility
- Scope: proposed `record-presentation-v31.spec.ts`, `record-accessibility-v31.spec.ts`, `docs/verification/ux-v3-1-screenshots/`.
- Run T31-07–13,30–47,51, at least actual Chrome; report other browsers by actual availability. Open/inspect screenshots, not capture-only approval.
- Backend/DB: none. Exit: sizes/zoom unobstructed, clear focus, no invented browser passes.

#### UX31-Q02 — Full Regression
- Run applicable §10 gates; preserve core historical coverage such as 14 files/27 scenarios, actual counts from discovery.
- Update obsolete E2E selectors/destinations for approved changes without deleting business assertions; rerun failing gates after fixes.
- Backend/DB: none except registered necessary fixes. Exit: traceable commands/environment/pass/fail/skip counts.

#### UX31-R00 — Usage and Acceptance
- Deliver `docs/verification/ux-v3-1-uat.md`; update affected README/development/runtime instructions.
- Cover creation return/historical entry/menus/details/recovery, actual AI live/Safari/Edge/Peer/UAT statuses.
- Backend/DB: none. Exit: no misleading stay-after-save guidance; unapproved/unexecuted items remain incomplete.

#### UX31-R01 — Delivery
- Deliver `docs/verification/ux-v3-1-final.md`, ledger SHA/test summary/screenshots/limits.
- Check branch/HEAD/tracked diff/explicit files/no sensitive content/no remote actions.
- Backend/DB: final actual register. Exit: reviewable developer evidence, no signing Peer/UAT.

## 9. Automated Matrix (51 Scenarios, Not 51 Executed Tests)

U=Vitest unit, C=Testing Library component, K=contract/boundary, E=real browser+Spring+PG, V=visual. Add tests during development; Q cards cover cross-module/full regression.

| ID | Level | Required behavior |
|---|---|---|
| T31-01 | U/C | No success event/navigation before verifyReceipt passes |
| T31-02 | U/C | Repeated receipt confirmation yields one consumable event |
| T31-03 | U/C | Old owner/epoch late response leaves new session untouched; ignored action no-op |
| T31-04 | U | SINGLE/DISH message count matches roots, not leaves |
| T31-05 | U/C | Empty draft has allowed defaults/new IDs/no business input |
| T31-06 | U/K/E | No browser persistence/window.name/persisted cache writes |
| T31-07 | C | 2000ms starts only on first Dashboard display |
| T31-08 | C | Visible before 2000ms, removed after 2200ms, no layout shift |
| T31-09 | C | Reduced motion has no fade; readable/announced message |
| T31-10 | C | Rerender/StrictMode/remount do not replay/extend |
| T31-11 | C | Initial background does not lose notice; later hiding pauses remainder |
| T31-12 | C/E | Single polite live region, no focus steal |
| T31-13 | C/E/V | Mobile safe area/navigation/menu/dialog layering correct |
| T31-14 | C | Logout clears context/pending events, no account leakage |
| T31-15 | C/E | Refresh/relogin never replay toast |
| T31-16 | C/E | Confirmed ordinary food to `/dashboard`, persisted once |
| T31-17 | C/E | Mixed SINGLE/DISH atomic save, one navigation/notice |
| T31-18 | C/E | Quick returns, correct duration/estimate |
| T31-19 | C/E | Multi-segment Workout returns, no duration duplication, correct partial |
| T31-20 | C/E | My meal addition/AI candidate generation no success navigation |
| T31-21 | C/E | Rejected preserves inputs; unknown preserves frozen payload/id, no false success |
| T31-22 | U/C/E | Historical save returns today without changing today's totals, increases historical day |
| T31-23 | U/C | Business midnight: saved date and destination today independently correct |
| T31-24 | C/E | History link has correct date/type/all typed roots |
| T31-25 | C/E | Post-save read failure remains saved, no repeated POST; warning outlives toast |
| T31-26 | C/E | Read-only retry restores data/warning state |
| T31-27 | C/E | Lost response recovered verified: one navigation/DB write |
| T31-28 | C/E | Back never resaves/restores submitted input/false draft prompt |
| T31-29 | C | Lazy Dashboard slow/failure does not consume unseen notice or repeat POST |
| T31-30 | C/V | Food name/portion/energy/macros complete with hierarchy |
| T31-31 | U/C | Existing kJ/kcal display conversion only; backend value unchanged |
| T31-32 | C/V | Long English/Chinese names, huge numbers, missing fields no overflow/overlap |
| T31-33 | C | null energy not 0; partial/legacy/source still inspectable |
| T31-34 | C/E | Dish/Workout one root; roots distinguished from leaves/segments |
| T31-35 | C/E | Details retain ingredients/segments/training/sources/version/save facts |
| T31-36 | C/E | Keyboard menu open/close, record-specific names, focus return |
| T31-37 | C/E | Edit preserves filters; changed dates retain compatible context |
| T31-38 | C/E | Log again creates review draft only; invalid references blocked |
| T31-39 | C/E | Confirm whole-root deletion; cancel no writes; unknown not false success |
| T31-40 | C/E | busy/pending locks cannot bypass; errors visible after close |
| T31-41 | C/E | No duplicate merged pages; highest version retained; truthful totalMatches |
| T31-42 | E | Lists/summary/trends match independent SQL aggregates |
| T31-43 | C/E | History links/typed highlight/detail close/filter restoration correct |
| T31-44 | C/V | Narrow date/meal layout no overflow; labels/helpers/errors visible |
| T31-45 | C/E | Recent reuse validates references, never adopts old quantity/duration |
| T31-46 | C/E | Quick/Gym selected semantics and actual draft discard protection |
| T31-47 | C/V | No large empty recent block; accessible loading/error/retry |
| T31-48 | E | Unknown 404 proof retains ID; same-body retries show one submission/multiple requests |
| T31-49 | E/K | Session/CSRF/USER-ADMIN/cross-user read-write boundaries pass |
| T31-50 | C/E | Media review, V1 records, Profile/Auth/Admin/Q&A retain regression behavior |
| T31-51 | E/V | 360/375/768/1280/1440px, 200% zoom, keyboard/focus without clipping |

Suggested tests: `saveFeedbackState.test.ts`, `SaveFeedbackHost.test.tsx`, `CreateSaveNavigation.test.tsx`, `SavedRecordContext.test.tsx`, `EntryRootPresentation.test.tsx`, `EntryRootMenu.test.tsx`, placed under existing tests/unit or tests/component conventions.

Inspect/update actual foodSubmissionV2State / exerciseSubmissionV2State, GroupedHistory, review-page, draft-navigation, food-entry-v3, exercise-entry-v3, save-unknown-v3, refresh-manual-history, grouped-summary, ux-accessibility-v3, submission-recovery tests. Search all `Just saved`, `View saved records`, `Continue recording`, `/entry?kind=`, and timing assertions; not one happy path only.

## 10. Test Standards and Execution

### 10.1 Inherited Standards

Arrange–Act–Assert, tests alongside development, negative cases for key transitions. Java uses JUnit 5/Mockito or compatible Jupiter; APIs MockMvc; persistence/transactions/uniqueness/concurrency/migrations isolated PostgreSQL. Frontend unit/component/contract/real E2E layers.

Without Java/DB changes, do not duplicate unrelated tests for counts, but run existing backend/PG regression. Backend fixes require corresponding Java/API/PG assertions in the card. Styling uses meaningful behavior and real screenshots/layout, not implementation-mirroring unit tests.

Zero discovered tests, skips, --list only, missing DB, environment download failure, and mocked success are not PASS. Never delete failures, globally relax timeouts, disable auth, or alter requirements to turn green.

### 10.2 Repository Commands

Run from root after configuring authorized interpreters/dependencies/isolated environment. Not executed in document preparation.

```bash
# Specific package tests (both files proposed for this round)
npm --prefix frontend test -- tests/unit/saveFeedbackState.test.ts
npm --prefix frontend test -- tests/component/SaveFeedbackHost.test.tsx

# Complete gates; record separate sequential logs
sh scripts/check.sh --stage static
sh scripts/check.sh --stage frontend
sh scripts/check.sh --stage data
sh scripts/check.sh --stage backend
sh scripts/check.sh --stage browser
```

`FITNUTRI_PYTHON` may target an existing valid venv. Backend requires private TEST_DB_ADMIN_URL, TEST_DB_USERNAME, TEST_DB_PASSWORD, TEST_DB_DOCKER_CONTAINER. Browser requires a real independent stack and synthetic admin/E2E configuration from current runtime docs, no passwords in public commands. Default all excludes browser; execute browser separately.

Temporary `/tmp` wrappers, historical PIDs/ports/JARs are not permanent configuration. Check ownership/version; restart only owned services for this round, preserving original V8 data/runtime. Avoid concurrent heavy Java/browser/frontend suites; small packages then sequential full checks.

Real E2E is React→Spring→PG, never route.fulfill save-success fabrication. Fault proxy forwards actual requests/responses then drops/delays; combine SQL, request counts, same-id/body fingerprints.

### 10.3 Visual/Browser Reporting

Record real browser/version, OS, viewport, zoom, before/after screenshots, inspection conclusions. Reproduce before in same environment, not unrelated device screenshots as strict pixel baselines. Missing browser = NOT_RUN; WebKit is not native Safari. Separate automated overflow and manual screenshot review; counts do not prove visual quality.

## 11. Package Records, Acceptance, Delivery

Record in results per package:

```text
Task ID / requirement IDs:
Actual files:
Backend Change / Database Change / Contract:
Tests and meaningful assertions:
Commands / environment:
Passed / failed / skipped / not run:
Failures found and resolved:
Branch / local commit:
Developer verification:
Peer review: PENDING
UAT: PENDING
Remaining blockers:
Next executable task:
```

### 11.1 Manual UAT (Initially Unsigned)

- [ ] Ordinary food confirmed then today's Dashboard; timely notice without layout shift.
- [ ] Dish/mixed batch navigates/notifies once; accurate summary/details.
- [ ] Quick/multi-segment Workout return correctly; clear duration/estimates/missing values.
- [ ] Yesterday's entry leaves today's totals unchanged; saved-context link opens yesterday.
- [ ] Induced read failure retains saved fact; retry creates no duplicate.
- [ ] Dashboard/History cards have no cramped edges/overlap/long-name overflow; names/values easy to find.
- [ ] Edit/reuse/delete menu understandable; canceled delete has no effect.
- [ ] Complete composition/training/provenance in details; History filters retained.
- [ ] Main flows work on mobile/desktop/200% zoom/keyboard.
- [ ] Recent/mode controls compact without accidental input loss.

### 11.2 Definition of Done

IN_REVIEW: implemented actual scope, direct/card and applicable full gates pass, real frontend/backend/PG flow passes, screenshots inspected, evidenced failure closure, clear branches/commits, no unregistered API/DB changes, updated docs.

ACCEPTED: actual separate Peer review and user UAT. UI regression cannot complete live M2 acceptance. Record missing external configuration precisely and continue independent work, never fabricated AI results.

### 11.3 Final Receipt

Separately report RF-01–10, unit/component/contract, Java/PG, real E2E, visual/browser, Peer, UAT, external blockers. Include final local HEAD, package commits, actual counts, unresolved items, runtime entries. No push/PR.

## 12. Ready-to-Use Prompt After Approval

> Effective only after explicit user development approval.
>
> Implement the 16 cards in `FitNutri_UX_Feedback_and_Record_Presentation_Execution_Plan_v3_1.md`. Run Branch Gate in actual repo, work only on `develop/yuankang-wei`, preserve changes/v3 protections. Continue dependencies from UX31-P00, testing/running/self-reviewing/local Conventional Commits/evidence each package. No remote Git, replacing baseline, clearing data, skipping failures, or false model completion. Only listed v3 interaction differences change. New saves return to today's Dashboard after verified receipts; unknowns retain safe recovery. Complete independent work and report developer/Peer/UAT/external states separately.

---

## Appendix: Historical Draft-Preparation Boundary

That round added only this specification, reading requirements/four screenshots and inspecting Git/frontend/test entries/v3 rules read-only. No business/test code, DB, dependencies, active pointer changes; no business tests, service starts/stops, or commits. All cards unstarted, new tests/UAT unexecuted.

This appendix describes initial preparation authorization/status only. The user subsequently approved implementation; see [current task ledger](ux-v3-1-tasks.md) and [actual evidence](../verification/ux-v3-1-results.md). Preserving history does not require approval again.
