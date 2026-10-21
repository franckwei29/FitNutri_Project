# FitNutri — Centered Success Feedback and Related Action Styling Execution Specification v3.2

> English handover edition (2026-10-18): this historical specification retains its original baseline, task statuses and instructions as evidence. It does not grant new execution authority or certify the current implementation. Current submission prose is English; functional multilingual inputs are retained. For installation use [the fresh-start guide](../getting-started.md), and for this handover use [the validation report](../verification/team-handover-validation.md).

> Date: 2026-10-18 (Australia/Sydney)
> Requirements: CONFIRMED — the user confirmed option A, success feedback, short labels, and the refinement scope.
> Development: APPROVED — on 2026-10-18 the user explicitly authorized continuous development, accompanying tests, and local commits; push is prohibited.
> Implementation was subsequently authorized. The preparation-stage boundary at the end is a historical record; see the [task ledger](ux-v3-2-tasks.md) for actual progress.

## 0. Purpose, Sources, and Specification Relationships

### 0.1 Objectives

1. After a Food / Exercise record is actually saved, show a green, large-text, single-line success notification at the exact screen center, disappearing after 2 seconds.
2. Use the selected Dashboard option A: a compact toolbar and compact saved-record information bar.
3. Standardize short labels, icons, grouping, states, and accessibility for explicitly listed related actions, including menus, pagination, and failure states not normally visible.
4. Keep `Add a record`, `Add food`, and `Add exercise` as they are.

Retain React, TypeScript, Vite, MUI, Spring Boot, Spring Security Session, and PostgreSQL. This round has a defined presentation and interaction scope; it does not reimplement existing full-stack business functionality.

### 0.2 Sources and Priority

- The user's two Dashboard screenshots show the current problems: right-positioned feedback, scattered actions, long labels, and insufficient icons.
- The user selected option A and confirmed centered green large text, one line, 2 seconds, and consistent Food / Exercise behavior.
- The user then confirmed short labels and related hidden entry points, explicitly retaining the three Add buttons.
- Conversation mockups were style-selection aids. Illustrated steps, water, ring metrics, and background blur are not new requirements and must not replace the Dashboard.
- Latest explicit user instructions > confirmed differences in this document > uncovered rules in the [existing v3.1 specification](FitNutri_UX_Feedback_and_Record_Presentation_Execution_Plan_v3_1.md) > retained v3 business, security, and data rules.
- Code quality, testing, branch, and local-commit requirements in the [development specification](../development.md) remain applicable. Do not reread or execute the old v2 plan or overwrite the working tree from a ZIP.

Pixel values, task divisions, and test IDs here are executable engineering defaults, not new business rules. If implementation reveals a conflict, follow the user-confirmed behavior and record the difference.

### 0.3 Document Preparation Baseline

| Item | Read-only inspection result |
|---|---|
| Project root | `/Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6` |
| Current / required branch | `develop/yuankang-wei` |
| HEAD | `de36589fcb3efe0bea275b7c76f221c41742ddad` |
| Tracked changes | None before preparation |
| Existing untracked files | `.DS_Store`, `.idea/`, `.local/`, and `.DS_Store` files under backend; preserve them |
| Branch Gate | PASS during preparation; recheck before development |
| Business tests | Not executed during document preparation, NOT_RUN |

This HEAD and historical passing counts describe the baseline only, not v3.2 test success. Preserve accounts and records in the existing manual acceptance environment. Subsequent automation must use a separately owned isolated test database, never rebuild or clear the manual acceptance database.

## 1. Frozen Requirements and Scope

| ID | Confirmed requirement | Acceptance focus |
|---|---|---|
| RF32-01 | Center Food / Exercise success feedback consistently | Center horizontally and vertically in the visible viewport, not the content container or top center |
| RF32-02 | Green, large white text, check icon, one line, 2 seconds | No subtitle, confirmation button, overlay, or page displacement |
| RF32-03 | Dashboard option A compact toolbar | Date / Today / Refresh grouped on one desktop row; natural narrow-screen wrapping |
| RF32-04 | Compact saved-record information bar | Saved date, View records, and close in one related area |
| RF32-05 | Confirmed short labels and icons | Follow section 4 exactly; do not expand to labels such as Refresh the overview |
| RF32-06 | Refine hidden states too | Complete menu, pagination, refresh failure, disabled, loading, and focus states |
| RF32-07 | Consistent related actions across locations | Dashboard, History, and the same record-details flow, limited to section 4's allowlist |
| RF32-08 | Preserve all three Add buttons | Do not proactively change labels, icons, appearance, position, or behavior |
| RF32-09 | Preserve business and data protections | Retain receipts, idempotency, read-only retries, authorization, dates, and draft protection |

### 1.1 Precise Overrides of v3.1

| Previous rule/implementation | This round's override |
|---|---|
| Success notification positioned right below navigation | Center in the viewport |
| Variable success message extensions such as date and count | New-save notifications fixed to Food saved / Exercise saved; dates remain in the information bar |
| About 2000ms visible plus a separate exit phase | Total visibility 2000ms; any exit animation is included, not added |
| Long labels such as Refresh overview / View saved records | Replace according to section 4 |
| Text trigger for more-actions menu | Ellipsis icon button with an explicit accessible name; retain menu text |
| v3.1 save navigation, owner/epoch, receipts, read failures, and business rules | Retain; do not rewrite for styling |

### 1.2 Explicit Scope Limits

Do not redesign Dashboard metric cards, chart content, or layout; add steps/water metrics; or change global navigation, authentication, questionnaire, entry, review/submit, Profile forms, or Admin actions. The Goals entry in the target card is included; internal Profile buttons are not. Do not change other success-notification durations or styles.

Do not upgrade frameworks/dependencies, add APIs or migrations, change nutrition/exercise calculations, delete historical data, or configure/falsely report live AI. Record issues outside this scope separately rather than folding them into this round.

## 2. Complete Success-Feedback Behavior

### 2.1 Trigger and Wording

- Display only after the existing save coordinator confirms a complete, valid new-save receipt; the destination remains today's Dashboard.
- Ordinary food, composed dishes, and mixed food batches all show `Food saved`; Quick / Workout show `Exercise saved`.
- Share appearance and lifecycle. Show once per verified save event, not once per leaf, ingredient, or exercise segment.
- Draft addition, AI candidates, successful preview, page load, list refresh, unverified receipts, failures, and unknown outcomes must not trigger success.
- An unknown outcome subsequently verified through receipt recovery may show once; do not regenerate a submission id or resubmit to obtain feedback.
- Existing-record edits retain correction and return-context rules; do not force the new-record Dashboard flow onto them.

### 2.2 Appearance and Position

- Solid theme success green, bold white text, and a white check icon. Colors must meet actual contrast requirements.
- Suggested desktop text: 24–28px; mobile: 22–24px. `Exercise saved` and its icon must fit completely on one line at 360px.
- Suggested radius: 14–18px with moderate shadow; content-sized width, at least 16px viewport side clearance. No ellipsis truncation.
- Center relative to the currently visible viewport, unaffected by scrolling, Dashboard width, navigation height, or ancestor transforms.
- Remain outside document flow: no card displacement, overlay, scroll lock, click interception, or keyboard-focus stealing. Menus and confirmation dialogs remain usable; do not cover essential dialog interaction layers.
- Use one polite live region and a decorative icon; green alone must not convey success.

### 2.3 Timing and Session

- Start accumulated visible time when Dashboard actually mounts and displays; remove at 2000ms. Lazy loading must not consume notification time.
- If fading, use approximately the final 150–200ms within the total 2000ms; reduced-motion removes fading and removes the notice at 2000ms.
- Pause visible-time accumulation while background-hidden; resume the remainder without restarting. Retain existing lifecycle protection.
- Rerenders, StrictMode, and remounts must not replay or extend it; leaving after display follows existing termination rules.
- Logout/account switches clear the previous user's notification; refresh and relogin never replay it. Do not persist notifications or business payloads in LocalStorage or similar storage.
- Fake-clock component tests assert still present at 1999ms and removed at 2000ms. Browser measurements start at actual display, allow reasonable scheduling variation, and record observations; do not conceal defects with relaxed timeouts.

## 3. Option A: Dashboard Header and Saved Information Bar

```text
[ Calendar  date ]  [ Calendar-return  Today ]  [ Refresh-icon  Refresh ]
[ Check  Saved for 18 Oct       List-icon  View records →     × ]
```

This illustrates grouping, not fixed character spacing. Common desktop widths should have a compact row without large gaps separating buttons; Refresh must not occupy an entire row. Narrow screens may wrap groups while preserving information/action relationships. Keep existing date rules and explicit field labels.

- Today: fixed label `Today`, return-to-today calendar icon, hover/focus explanation “Back to today”. Clearly show current/disabled state when already today without another refresh. Historical or invalid dates can return to a valid today.
- Refresh: refresh existing summary, records, and trends for the current date. Show loading feedback and prevent repeated triggers, then restore. Invalid dates must not request an invalid overview. Provide static feedback under reduced-motion.
- Saved bar: retain the actual saved date, distinguish it from the browsing date, and use the application date convention. Do not omit exact dates/years where ambiguous.
- View records: open History for the actual saved date, type, and correct typed root identities, not simply today or a context-free list.
- ×: close only the saved bar and its current viewing entry. Do not delete records, change lists, dismiss refresh-failure warnings, or trigger network writes.
- The centered notice disappears after 2 seconds; the information bar does not automatically disappear with it. Other lifecycle rules follow v3.1.
- Save succeeds but reads fail: retain the saved fact and a local warning with Retry. Retry reads only, never POST/PUT/DELETE. Clear the warning after recovery.
- Without a current save event, reserve no empty bar; dismissal naturally collapses the layout.

## 4. Action Inventory: The Only Standardization Scope

### 4.1 Confirmed Entry Points

| Action/location | Current label | Final visible content | Icon/state requirements |
|---|---|---|---|
| Dashboard return to today | Today | Today | Calendar return; clear current-day state |
| Dashboard refresh | Refresh overview | Refresh | Refresh icon; prevent repetition while loading |
| Current saved-record entry | View saved records | View records | List plus right arrow |
| Close saved bar | Dismiss | × icon | Accessible name Close saved notice; Tooltip |
| Read failure after save | Retry refresh | Retry | Retry icon; related error context |
| History below trends | Explore your history | History | History icon; retain date range and type |
| Expand trend data table | View intake data table | Data table | Table icon and expand/collapse arrow; retain table |
| Goal-card entry | Manage your goals | Goals | Goal icon; still open Profile |
| View root record | View entry | View | View icon; retain root-details link |
| Root more-actions entry | More actions ⋯ | ⋯ icon | Record-specific accessible name; keyboard menu control |
| Menu edit | Edit entry | Edit | Edit icon; retain version/context |
| Menu reuse | Log again | Log again | Repeat-entry icon; create reviewable draft only |
| Menu delete | Delete entry | Delete | Delete icon, danger color, existing confirmation |
| Food pagination | Load more food entries | More food | Load-down icon; disabled while loading |
| Exercise pagination | Load more exercise entries | More exercise | Load-down icon; disabled while loading |

Both pagination entries may coexist, so neither should become an indistinguishable More. Menu edit/reuse/delete retain icons and text rather than ambiguous icon-only actions.

### 4.2 Related States in the Same Record-Details Flow

Read-only inspection found additional `EntryRootDetailsDialog` actions not necessarily visible in Dashboard screenshots. Include them under the approved consistency scope:

| Condition | Current label | Recommended implementation | Meaning retained |
|---|---|---|---|
| Saved-record details read failure | Retry saved entry | Retry plus retry icon | Reread this record only |
| Close details | Close details | Close plus close icon | Close, restore filters and trigger focus |
| Record outside current date/type filter | View this entry’s current date | View date plus calendar icon | Navigate to actual record date; retain positioning and explanation |

These three items enumerate approved related scope, not all Retry/Close actions elsewhere. If other synonymous entries exist, list their locations and assessment in UX32-P01. Include only clearly equivalent record viewing/refresh/close actions; report different meanings or out-of-scope actions separately.

Retain meanings and necessary consequence explanations in delete/replace-draft dialogs, including Cancel, Replace draft, and Confirm deletion. Short labels must not conceal whole-record/all-component deletion. Do not affect unrelated flows through global dialog styles.

### 4.3 Global Consistency Boundary

Apply this specification to the same shared actions on Dashboard, History, and related details. Limit styles to their components/containers; no unscoped overrides of all Buttons, links, or Alerts.

Preserve `Add a record`, `Add food`, and `Add exercise` labels, icons, visual styles, layout, and behavior as regression protections. Profile forms, registration-success notices, and Entry/Review submission buttons are not targets.

## 5. Visual and Accessibility Rules

- Retain the light-green background, botanical-green accents, dark text, and rounded corners; introduce no theme or icon library. Prefer existing MUI icons.
- Suggested action height 40–44px, icons 18–20px, icon/text gap 6–8px, and group gap 8–12px. Touch targets at least 44×44px; enlarge hit area without enlarging icons if needed.
- View records is the bar's primary action; Refresh, Today, and History are secondary. Use danger styling for menu deletion only, distinct from green success.
- Provide normal, hover, focus-visible, disabled, loading, and error states. Do not rely solely on color or hide focus outlines.
- Icon buttons require clear accessible names and Tooltips. Record action names include context to distinguish repeated View/More buttons and include visible labels for screen readers/voice control.
- Data-table expansion retains keyboard-operable disclosure semantics and correct expanded state. Links remain links, not clickable divs.
- Menu Esc/close returns focus to the trigger; closing details restores entry focus. Preserve safe focus after deletion. Async errors must remain visible after the menu closes.
- Check 360, 375, 768, 1280, and 1440px plus desktop 200% browser zoom. Long record names and concurrent pagination must not overflow, overlap, or clip. Report CSS zoom separately from actual browser zoom.

## 6. Code Mapping and Architectural Constraints

The following entry points were inspected read-only; reconfirm before implementation. Add modules only to reduce real duplication, not to rebuild business state machines for label changes.

| File/area | Expected responsibility | Must preserve |
|---|---|---|
| `frontend/src/features/feedback/SaveFeedbackHost.tsx` | Centering, styling, timing, screen readers | Verified events and session boundaries |
| `frontend/src/features/feedback/saveFeedbackState.ts` | Short messages and total duration | owner/epoch, deduplication, remaining-time protection |
| `frontend/src/features/feedback/SavedRecordContext.tsx` | Compact bar, View records, ×, Retry | Saved date, typed roots, read-only retry |
| `frontend/src/pages/DashboardPage.tsx` | Toolbar, Today/Refresh states, Goals/History | Queries, dates, statistics, three Add buttons |
| `frontend/src/components/IntakeChart.tsx` | Data table entry | Trend and table data |
| `frontend/src/features/history/EntryRootActions.tsx` | View, more entry, menu icons/labels | edit/reuse/delete handlers, pending/permissions/version checks |
| `frontend/src/features/history/EntryRootPagination.tsx` | More food / More exercise and loading | Page merging, preserve loaded data on failure |
| `frontend/src/features/history/EntryRootDetailsDialog.tsx` | Related Retry/Close/View date | Position, filters, focus, account boundaries |
| `frontend/src/pages/HistoryPage.tsx` and shared record cards | Shared presentation and caller compatibility | Root identity, details, filtering |
| Existing theme/local styles | Scoped action styling | Global theme and out-of-scope buttons |

Backend Change: none. Database Change: none. Contract Change: none. Reuse existing DTOs, queries, link generators, and business handlers; add no parallel cache, write path, or calculation logic.

If a defect requires backend/database changes, first record its cause, impact, and independent tests. It requires a separately explicit scope and must not be hidden in styling work.

## 7. Development Management and Git Rules

### 7.1 Branch Gate

Before implementation, run in the actual project:

```bash
git rev-parse --show-toplevel
git status --short --branch
git branch --show-current
git branch --list
git rev-parse HEAD
```

Only `develop/yuankang-wei` is allowed. Switch only if that local branch exists and switching is safe for existing changes, then recheck. If Git/branch is missing or switching is risky, report BLOCKED_BRANCH and await the user; do not init, create a substitute branch, or clean the tree.

Recheck before each package write, after session resumption, and before commits. No development on main/master/shared develop/other members' branches. No fetch, pull, clone, push, PR creation/merge, force push, stash/reset/clean, or overwriting others' code. Preserve existing untracked files.

### 7.2 Code Standards

- Follow current lockfile/POM, strict TypeScript, and existing directory/component/hook/test structure. Do not hide errors with any, forced assertions, or relaxed validation.
- Retain React Router, TanStack Query, Zod, and MUI; do not hand-edit generated API types.
- Scope styles explicitly; no global `!important` or Add-button changes for matching screenshots.
- Do not duplicate business data/calculations or expose keys in frontend. Keep synthetic test data, runtime logs, backups, real accounts, and health information out of commits.
- Do not start parallel agents without user authorization or have multiple executors concurrently modify the same working tree.

### 7.3 Work-Package Closure

1. Read requirements, callers, and existing tests; establish acceptance conditions.
2. Prefer meaningful behavior tests; record old assertions legitimately changed by requirements.
3. Make the minimum necessary change, delivering behavior and tests together.
4. Run relevant tests, fix failures, and inspect shared components, permissions, interfaces, and data impacts.
5. Review diff/branch and explicitly stage package files; never broadly stage existing `.local/` content.
6. Local Conventional Commit: `type(scope): concrete change [UX32-ID]`.
7. Update ledger with files, commands, counts, environment, failures/fixes, commit, blockers, and next task.
8. Once authorized, continue by dependencies without waiting after each batch. Report real permission/environment blockers accurately.

Commit bodies require Why, What, Backend-Change, Database-Change, Contract, Tests, and Limitations. Record actual commands and passed/failed/skipped/not-run counts, not vague “tested”. Document preparation creates no business implementation commit; future UX32-P00 may explicitly include this specification in its documentation package.

## 8. Task Cards and Dependencies (12; Actual Status in Ledger)

| Task ID | Package | Dependency | Output and exit conditions |
|---|---|---|---|
| UX32-P00 | Branch Gate and baseline freeze | Separate user development authorization | Branch, HEAD, original diff, affected test baseline, environment ownership |
| UX32-P01 | Action inventory and test tracing | P00 | Complete RF32 → entry/state → test → task mapping; freeze allowlist/protections |
| UX32-F00 | Centered success feedback | P01 | Fixed Food/Exercise single line, centering, 2000ms, lifecycle, no layout shift tests pass |
| UX32-F01 | Dashboard option A | F00 | Grouped date/Today/Refresh, saved bar, read-only Retry, exceptional states pass |
| UX32-F02 | Other Dashboard entries | F01 | History/Goals/Data table short labels/icons; unchanged link/disclosure behavior |
| UX32-F03 | Shared record actions and menus | F02 | Consistent View/more/Edit/Log again/Delete; locks/focus/confirmation pass |
| UX32-F04 | Pagination and hidden details states | F03 | More food/exercise, details Retry/Close/View date states and behavior pass |
| UX32-Q00 | Real-stack regression | F04 | Real frontend/Spring/PG save, refresh failure, read-only retry, historical positioning, no duplicate writes |
| UX32-Q01 | Visual and accessibility | Q00 | Sizes/zoom/keyboard/menus/icons/2000ms screenshots and measurements; Add protections pass |
| UX32-Q02 | Complete automated regression | Q01 | Applicable static/frontend/data/backend/browser gates actually run; failure closure evidence |
| UX32-R00 | Runtime and manual acceptance guide | Q02 | UAT checklist, actual runtime entries, incomplete conditions, screenshot/result index |
| UX32-R01 | Final review and delivery | R00 | Requirement table, package commits, final branch/HEAD/diff; separate Peer/UAT states |

Test each F card during development; Q cards are combined regression, not a reason to delay unit tests. Backend/DB/Contract default to none for all tasks. Task counts are not completion evidence.

### 8.1 Deliverable Files

- P00: `docs/verification/ux-v3-2-baseline.md`.
- P01: `docs/implementation/ux-v3-2-tasks.md`, `docs/verification/ux-v3-2-test-matrix.md`, `docs/verification/ux-v3-2-results.md`.
- Q01: `docs/verification/ux-v3-2-screenshots/` and visual inspection records; synthetic accounts/data only.
- R00/R01: `docs/verification/ux-v3-2-uat.md`, `docs/verification/ux-v3-2-final.md`.
- Update active-spec pointer only when implementation formally begins. Preserve v3.1 historical reports/status and do not report historical test counts as current results.

## 9. Test Matrix (26 Acceptance Scenarios; Actual Results in Ledger)

U=unit, C=component, K=contract/boundary, E=real browser + Spring + isolated PG, V=visual/layout. A scenario may map to multiple tests; scenario count is not executed-test count.

| ID | Level | Required assertions |
|---|---|---|
| T32-01 | U/C/E | Fixed single-line Food/Exercise message only after valid new-save receipt; once per batch |
| T32-02 | C/E | No false success for draft/preview/refresh/rejected/unknown; verified recovery does not repeat writes |
| T32-03 | C/E/V | Notice center matches visible viewport before/after scrolling; no Dashboard-card displacement |
| T32-04 | C | Timing starts at actual display; present at 1999ms, removed at 2000ms; exit adds no duration |
| T32-05 | U/C | Hidden pauses, foreground resumes; no StrictMode/rerender/remount reset; logout/account switch clears |
| T32-06 | C/E/V | Reduced-motion has no animation; polite live region; no focus steal/overlay/click interception; no refresh replay |
| T32-07 | C/E/V | Desktop date/Today/Refresh grouped in one row; mobile wrapping without horizontal overflow |
| T32-08 | C/E | Today avoids repeated current-day actions; historical/invalid dates recover today with existing business timezone rules |
| T32-09 | C/E | Refresh reads existing summary/food/exercise/trends; prevents repeated loading triggers; retry after failure |
| T32-10 | C/E | View records carries saved date/type/typed root ids; historical entries never incorrectly target today |
| T32-11 | C/E | × closes bar only without writes; 2-second toast ending does not remove bar; warnings remain independent |
| T32-12 | C/E | Read failure retains saved fact; Retry uses GET only; recovery clears warning, no second DB creation |
| T32-13 | C/E | History retains trend range; Goals targets Profile; Data table toggles with unchanged data |
| T32-14 | C/E | View/more mean the same on Dashboard/History; correct root details even for duplicate names |
| T32-15 | C/E | More entry keyboard/Enter/Space/Esc, menu state, return focus, context names correct |
| T32-16 | C/E | Edit retains version/draft/filter; 409 preserves input; Log again retains reuse protections and empty-quantity rules |
| T32-17 | C/E | Delete retains danger styling/consequences/confirmation; cancel writes nothing; whole-root/unknown/focus behavior preserved |
| T32-18 | C/E | pending/busy and draft protections remain; async errors visible after menu closes |
| T32-19 | C/E | More food/exercise distinguishable together; no duplicate loading; failure preserves data; retry read-only, no duplicate pages |
| T32-20 | C/E | Details Retry/Close/View date perform only respective actions, restore filters/focus, retain permission/404 semantics |
| T32-21 | C/E/V | All three Add labels/icons/appearance/entry behavior preserved; Auth/Profile/Review unaffected |
| T32-22 | C/E/V | Normal/hover/focus/disabled/loading/error, Tooltips, names complete; icons do not replace essential text |
| T32-23 | E/V | 360/375/768/1280/1440px, real 200% zoom, long names, concurrent pagination without clipping; notice always one line |
| T32-24 | E/K | Session/CSRF/user boundaries, no persisted health payload, no business writes before confirmed save |
| T32-25 | E | Ordinary/composed food, Quick/Workout, historical saves, receipt recovery persist; root/leaf/duration conservation |
| T32-26 | K/E | Preserve v3/v3.1 core assertions; no deleted tests, skips, or loose selectors to conceal short-label failures |

### 9.1 Reuse and Extend Existing Tests

Confirmed entry points:

- `frontend/tests/component/SaveFeedbackHost.test.tsx`
- `frontend/tests/component/SavedRecordContext.test.tsx`
- `frontend/tests/component/EntryRootPresentation.test.tsx`
- `frontend/tests/component/GroupedHistory.test.tsx`
- `frontend/tests/component/presentation.test.tsx`
- `frontend/tests/component/record-presentation.test.tsx`
- `frontend/tests/e2e/record-presentation-v31.spec.ts`
- `frontend/tests/e2e/ux-accessibility-v3.spec.ts`

Add `DashboardActionsV32.test.tsx`, `RecordActionsV32.test.tsx`, and `action-style-v32.spec.ts` as needed; names are suggestions. Prefer accessible role/name and record context, not broad `.first()` or a single common word that may target the wrong button.

Update old top-right/2200ms/long-label assertions under approved requirements and record reasons. Retain permission/request-count/database/version/cancel/unknown assertions. Avoid CSS-mirroring unit tests; measure position/single-line/overflow in real browsers and open screenshots for inspection.

### 9.2 Commands and Quality Gates

Future execution entries; not run during preparation:

```bash
# Directly related examples; supplement with each package's actual files
npm --prefix frontend test -- tests/component/SaveFeedbackHost.test.tsx tests/component/SavedRecordContext.test.tsx

# Final complete sequential gates
sh scripts/check.sh --stage static
sh scripts/check.sh --stage frontend
sh scripts/check.sh --stage data
sh scripts/check.sh --stage backend
sh scripts/check.sh --stage browser
```

- AAA (Arrange–Act–Assert); cover normal, boundary, error, missing-data, and negative cases for key rules.
- Frontend: Unit, Component, Contract, production build/boundaries; real E2E must be React → Spring → PostgreSQL.
- Java/Controller/Database: retain JUnit 5/Mockito, MockMvc, and isolated PostgreSQL persistence/transaction/constraint/concurrency/rollback tests. Run required regression even without Java changes; do not add unrelated tests for counts.
- Privately configure actual backend `TEST_DB_ADMIN_URL/USERNAME/PASSWORD/DOCKER_CONTAINER`; browser follows existing README with independent database, real services, synthetic accounts. Never delete/clean the user's manual acceptance data.
- Verify actual `.local` ownership, databases, JARs, ports, dependencies. Historical PIDs, temporary scripts, and old database names are not current facts. Report BLOCKED_ENV if missing; never silently connect elsewhere.
- Component mocks may test controlled failures but are not real integration. Use existing controlled forwarding for real failures; never fabricate successful save responses.
- Record actual passed/failed/skipped/not-run. Zero tests, --list, skips, missing DB, and environment startup failure are not PASS. Preserve failure and rerun evidence without disabling permissions or relaxing business rules.
- Browser reports record actual name/version, OS, viewport, zoom, screenshot conclusions. Report Chrome automation, other-browser availability, and user UAT separately; WebKit is not Safari.

## 10. Data, Runtime, and Commit Safety

No schema changes this round; no restore, migration, or clearing of the manual acceptance database. Subsequent tests use explicitly owned isolated environments and preserve user-entered acceptance records.

Service health is not test evidence: page 200/backend UP proves only those probes. Reverify runtime entries before delivery. If restarting, operate only identified services/artifacts for this round, never unknown processes.

Unavailable model configuration remains BLOCKED_EXTERNAL. Styling completion cannot change M2 live-model, microphone, or manual acceptance status. Never commit provider keys, runtime passwords, real health data, or database dumps.

## 11. Package Records and Delivery Standards

Record per package:

```text
Task ID / Requirement IDs:
Actual files:
Backend Change / Database Change / Contract:
Tests / meaningful assertions:
Actual commands / environment:
Passed / failed / skipped / not run:
Failures found / fixes / rerun evidence:
Branch / local commit:
Developer verification:
Peer review: PENDING
UAT: PENDING
Remaining blockers:
Next executable task:
```

IN_REVIEW requires implemented in-scope requirements, direct tests and applicable complete-gate results, real-stack and visual checks, evidenced fixes, no unregistered API/DB changes, and complete package commits/runtime instructions.

ACCEPTED requires actual independent peer review and user UAT; the executor cannot sign for them. List blocked gates individually instead of declaring the whole round passed.

### 11.1 Manual Acceptance Checklist (All Await User Acceptance)

- [ ] Food / Exercise saves show centered green large single-line feedback, disappearing in 2 seconds without another click.
- [ ] Composed dishes/multi-segment exercise notify once; historical entries remain findable.
- [ ] Date, Today, Refresh are grouped with short labels and understandable icons, not one button per row.
- [ ] Compact saved bar; clear View records and × meanings; closing does not affect records.
- [ ] Post-save refresh failure is understandable; Retry does not save again.
- [ ] History, Goals, Data table positions/icons/text are clear and functionality correct.
- [ ] Saved-record View, more, Edit, Log again, Delete are understandable with protections intact.
- [ ] Hidden pagination and details failure/close/date positioning are orderly with loading feedback.
- [ ] Mobile, desktop, keyboard, and 200% zoom remain operable without clipping or stolen focus.
- [ ] Add a record, Add food, Add exercise remain unchanged.

### 11.2 Final Delivery Receipt

Report RF32-01–09 individually: actual files, Backend/DB/Contract status, commands/counts, real-stack and visual/browser evidence, final branch/HEAD/package commits, runtime URLs, remaining blockers. Separate Developer / Peer / UAT / Live AI statuses. No push or PR creation.

## 12. Instructions Available After Development Approval

> This section takes effect only after separate explicit user authorization.
>
> Execute the 12 cards in `FitNutri_Centered_Feedback_and_Action_Style_Execution_Plan_v3_2.md`. Run Branch Gate in the actual repository and work only on `develop/yuankang-wei`. Continue by dependencies from UX32-P00, writing/running tests, self-reviewing, making local Conventional Commits, and recording evidence per package. Implement option A, centered green single-line 2-second feedback, and allowlisted short labels/icons; preserve all three Add buttons. Preserve business/data/permission protections; do not restore/clear the acceptance database, operate remote Git, or redesign the entire site. Separately report developer tests, real integration, visuals, peer review, and UAT.

---

## Appendix: Document Preparation Boundary

This preparation round added only this MD. Git, related source/test entries, and development rules were inspected read-only. No business/test code, dependencies, database, runtime configuration, or active-spec pointer changed; no listed business tests, service start/stop, local commit, or remote action occurred. Requirements were confirmed, development still awaited separate authorization, all 12 cards were NOT_STARTED, and all 26 scenarios were NOT_RUN.
