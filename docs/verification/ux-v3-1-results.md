# V3.1 actual execution results

## UX31-P00

Commit 97d3c48. Branch/source baseline: ux-v3-1-baseline.md. Four affected Vitest files /24 passed. Backend/DB/contract none. Peer/UAT PENDING.

## Legacy assertion changes

V3 stay-on-entry navigation and Continue recording are superseded by verified create → today Dashboard. Exact receipt, frozen retry, rejected/unknown, epoch, root identity, permission and persistence assertions remain. Registration timing is unchanged. Inline details are moved to the existing full detail view; assertions follow information there. Menu actions require opening More actions, with the same mutation assertions.

## UX31-P01 — docs(ux): map feedback requirements and regression gates

Actual files: docs/development.md, docs/verification/ux-v3-1-test-matrix.md.

Backend Change / Database Change / Contract: none. Tests: Document structure: 16 unique cards, 51 scenario rows; prior 24-test baseline retained, no business implementation in this package. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F00 — feat(feedback): scope verified save display lifecycle to owner

Actual files: frontend/src/features/feedback/saveFeedbackState.ts, frontend/src/features/feedback/SaveFeedbackContext.ts, frontend/src/app/DraftProvider.tsx, frontend/tests/unit/saveFeedbackState.test.ts.

Backend Change / Database Change / Contract: none. Tests: RED missing module confirmed; GREEN Vitest four files /23 passed (log /tmp/fitnutri-ux31-f00-green.log); exact receipt/epoch tests retained. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F01 — feat(feedback): show accessible timed floating save notices

Actual files: frontend/src/features/feedback/SaveFeedbackHost.tsx, frontend/src/styles.css, frontend/tests/component/SaveFeedbackHost.test.tsx.

Backend Change / Database Change / Contract: none. Tests: RED missing host confirmed; GREEN two Vitest files /9 passed, 0 failed/skipped; fake-clock destination/2200ms/hidden pause/reduced-motion/focus/remount assertions. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F02 — fix(entry): return verified creations to todays dashboard

Actual files: frontend/src/features/feedback/useSaveNavigation.ts, frontend/src/features/entry/ReviewPage.tsx, frontend/src/features/entry/food/FoodCreateReviewPage.tsx, frontend/src/features/entry/exercise/ExerciseCreateReviewPage.tsx, frontend/src/pages/EntryPage.tsx, frontend/tests/component/review-page.test.tsx.

Backend Change / Database Change / Contract: none. Tests: RED old stay-on-entry assertion:1 failed/1 passed; GREEN four Vitest files /17 passed, zero failed/skipped (f02-green log), frozen save and recovery retained. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F03 — feat(dashboard): retain saved context and recover failed reads

Actual files: frontend/src/features/feedback/SavedRecordContext.tsx, frontend/src/hooks/useSubmitDraft.ts, frontend/src/pages/DashboardPage.tsx, frontend/src/styles.css, frontend/tests/component/SavedRecordContext.test.tsx.

Backend Change / Database Change / Contract: none. Tests: RED missing context confirmed; GREEN four Vitest files /16 passed, zero failed/skipped; test typecheck exit0. F02 count corrected to actual17.. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F04 — feat(records): separate clear summaries from complete saved details

Actual files: frontend/src/features/history/EntryRootCard.tsx, frontend/src/features/history/EntryRootDetailsDialog.tsx, frontend/src/styles.css, frontend/tests/component/EntryRootPresentation.test.tsx, frontend/tests/component/GroupedHistory.test.tsx.

Backend Change / Database Change / Contract: none. Tests: RED three new presentation cases exposed old layout; GREEN two Vitest files /9 passed, zero failed/skipped after fixing test migration syntax and duplicate reference selector; underlying detail rendering retained. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F05 — feat(records): group protected mutations in accessible menus

Actual files: frontend/src/features/history/EntryRootActions.tsx, frontend/src/styles.css, frontend/tests/component/GroupedCorrection.test.tsx.

Backend Change / Database Change / Contract: none. Tests: RED10 failed before menu; GREEN GroupedCorrection10 passed, zero failed/skipped; existing correction/version/unknown/reuse/delete assertions retained plus Escape/focus; test typecheck exit0. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F06 — style(dashboard): space root panels and expose authoritative counts

Actual files: frontend/src/features/history/EntryRootCard.tsx, frontend/src/pages/DashboardPage.tsx, frontend/src/styles.css.

Backend Change / Database Change / Contract: none. Tests: GREEN four affected Vitest files /17 passed, zero failed/skipped; grouped count/typed identity/pagination contract regressions; visual viewport checks follow UX31-Q01. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F07 — style(history): organise dated groups and restore detail focus

Actual files: frontend/src/pages/HistoryPage.tsx, frontend/src/styles.css.

Backend Change / Database Change / Contract: none. Tests: GREEN four Vitest files /42 passed, zero failed/skipped; filter/date correction/reuse/delete/version/unknown retained. F06 count corrected to actual17.. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-F08 — feat(entry): compact recent choices and focus required inputs

Actual files: frontend/src/features/entry/food/FoodComposer.tsx, frontend/src/features/entry/food/RecentDishes.tsx, frontend/src/features/entry/food/SmartFoodSearch.tsx, frontend/src/features/entry/exercise/RecentQuickExercises.tsx, frontend/src/features/entry/exercise/ExerciseEntryEditorV2.tsx, frontend/src/features/entry/exercise/ActivityPeriodEditor.tsx, frontend/src/features/entry/exercise/QuickExerciseV2.tsx, frontend/src/styles.css, frontend/tests/component/FoodComposer.test.tsx, frontend/tests/component/QuickExerciseV2.test.tsx.

Backend Change / Database Change / Contract: none. Tests: RED2 failed/15 passed for missing required-input focus; GREEN four files /35 passed, zero failed/skipped; continuous add/recent explicit reuse/workout input regressions. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

### Q00 investigation — actual browser navigation timing

The first actual eight-case browser run loaded the pre-fix coordinator: the first three cases failed because navigation was consumed while the prior saving blocker still applied; real records were already committed. React external-store notification can precede the queued DraftProvider render. The coordinator now waits for the matching rendered lastSaved/idle state and defers navigation to a cancellable microtask after blocker effects. The newly guarded component test passed both before/after (it did not reproduce this real scheduling race); no false RED claim. The actual browser run subsequently passed today's floating notice/Back/reload, historical link, dropped overview reads and GET-only recovery after commit. All eight cases will be rerun on the final coordinator. Logs: /tmp/fitnutri-ux31-q00-browser.log and q00-race-green.log (3 files/10 passed).

Static gate: actual OpenAPI 64 operations/122 schemas,706 valid examples/11 invalid rejections; generated drift check, test typecheck and lint PASS, no skip. Log /tmp/fitnutri-ux31-static.log. Actual owned runtime JAR matches private manifest, PG successful schema max13, frontend auth/csrf200. Sandboxed localhost URLError was access restriction, not a stopped runtime; no restart performed.

## UX31-Q00 — test(feedback): verify real saves recovery and guarded navigation

Actual files: frontend/playwright.v3.config.ts, frontend/src/features/feedback/useSaveNavigation.ts, frontend/src/features/history/EntryRootCard.tsx, frontend/tests/component/EntryRootPresentation.test.tsx, frontend/tests/component/review-page.test.tsx, frontend/tests/e2e/dish-entry-v3.spec.ts, frontend/tests/e2e/exercise-entry-v3.spec.ts, frontend/tests/e2e/food-entry-v3.spec.ts, frontend/tests/e2e/grouped-summary.spec.ts, frontend/tests/e2e/save-unknown-v3.spec.ts, frontend/tests/e2e/ux-accessibility-v3.spec.ts, frontend/tests/e2e/v3-exercise-journeys.ts, frontend/tests/e2e/v3-journeys.ts, frontend/tests/e2e/workout-entry-v3.spec.ts, frontend/tests/e2e/save-feedback-v31.spec.ts.

Backend Change / Database Change / Contract: none. Tests: Actual installed Chrome real stack8/8 PASS1.8m (q00-browser-green), initial3failed/5passed navigation race closed; component4files/12passed and guarded-order4passed; static/build PASS with186modules954edges0violations. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-Q01 — fix(a11y): close visual reflow and record focus regressions

Actual files: frontend/src/features/feedback/SaveFeedbackRouteBoundary.tsx, frontend/src/features/feedback/SaveFeedbackHost.tsx, frontend/src/features/feedback/saveFeedbackState.ts, frontend/src/features/history/useDeletedRootFocus.ts, frontend/src/features/history/EntryRootDetailsDialog.tsx, frontend/src/pages/HistoryPage.tsx, frontend/src/pages/DashboardPage.tsx, frontend/src/layouts/AppLayout.tsx, frontend/src/styles.css, frontend/tests/component/SaveFeedbackHost.test.tsx, frontend/tests/component/GroupedCorrection.test.tsx, frontend/tests/e2e/record-presentation-v31.spec.ts, docs/verification/ux-v3-1-browser.md, docs/verification/ux-v3-1-visual-evidence.json, docs/verification/ux-v3-1-screenshots.

Backend Change / Database Change / Contract: none. Tests: Actual Chrome presentation1/1PASS35.4s; five widths and200%CSSzoom, keyboard/detail/delete focus; component3files25PASS, typecheckPASS;16synthetic screenshots preserved and key images visually inspected; earlier four actual failures documented. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

### Q02 full-gate failures and closure work

First complete frontend run:113files,619tests;603passed/16failed,0skips. Failures were five old save-route/media fixture files expecting stay-on-entry/Continue recording and one isolated Dashboard affordance render missing the now-required owned provider. Updated their stated navigation/timing requirements and supplied mocked unrelated Dashboard read APIs within component scope; kept real production routes, owned provider, receipt validation, frozen retries, known-conflict reconfirmation and IMAGE/VOICE provenance assertions. Six affected files now24PASS,0failed/skipped (/tmp/fitnutri-ux31-q02-legacy-assertions-green.log). First fixture migration had missing food-read stub and ambiguous status selector; corrected, not counted as PASS. Whole frontend gate is rerunning.

Initial data gate ran40tests with5sandbox Docker access errors; this was not a PASS. Full rerun with permitted access:40PASS,0fail/error/skip, /tmp/fitnutri-ux31-final-data-permitted.log. No business database migration.

Whole frontend rerun after fixture migration:113files/619Vitest PASS,0fail/skips. That sandboxed gate then failed all10fault-proxy cases at loopback listen EPERM; the gate is not reported as passed. Complete frontend gate is rerunning with permitted local socket access, no test removed or skipped. Log /tmp/fitnutri-ux31-final-frontend-green.log retains both the619PASS and listener failures.

Q02 additional preservation checks: successful navigation initially discarded the allowed default meal along with the empty continuation. Added a regression requiring the next explicitly started SPA food entry to retain only the confirmed default meal, with today/fresh IDs/blank amounts. RED2failed/7passed, then GREEN3files/13passed (/tmp/fitnutri-ux31-meal-context-green.log). The owned reducer keeps only that preference in memory; reload/owner/epoch changes still clear it. No API/schema change.

Q02 mobile overlay: a fixed88px header assumption could overlap wrapped mobile navigation. RED1failed/5passed (missing measured-header offset), then GREEN3files/9passed (/tmp/fitnutri-ux31-mobile-toast-green.log). The floating host measures actual header layout with ResizeObserver, without changing document flow or success timing. Real375px placement assertion added to today's actual save case. Final whole frontend gate will include this last source change.

Actual Vite Dashboard chunk abort after a committed save:1PASS11.2s (/tmp/fitnutri-ux31-chunk-fault.log). Real POST201, actual route error boundary, no premature toast, successful reload with committed record, one POST and unchanged SQL/proof. No successful API response fabricated.

Full32-case browser run discovered one additional obsolete expectation in the original .mjs shared helpers: M1 core confirmation still waited for Add food while the actual app was correctly at Dashboard. Updated only post-confirm/post-receipt destinations in core-journeys.mjs and browser-journeys.mjs; initial Add-page waits and SQL/snapshot/cancellation/security assertions remain. Full suite failure is retained and will be closed by execution, not ignored.

The first full browser process had discovered/cached old shared .mjs confirmations before their migration:4old-destination failures,11passes,1interrupted,16notrun, exit130 after an explicitly verified graceful SIGINT to that owned Playwright runner only. This is NOT a PASS. No app/PG service stopped. Raw initial report retained privately at /tmp/fitnutri-ux31-browser-first-results.json, log /tmp/fitnutri-ux31-final-browser.log. All32 cases are now executing from a fresh runner against updated helper assertions, with one worker and zero retries. No SQL/provenance/cancellation/security assertion was removed.

### Final complete gates on frozen business source

- Frontend: `FITNUTRI_PYTHON=/private/tmp/fitnutri-ux-v3-venv/bin/python sh scripts/check.sh --stage frontend` —113files/620Vitest PASS,0failed/skipped; Demo15 + fixture integrity1, boundaries11, forwarding-fault10, deployment/pipeline/artifact13 PASS; TS/build PASS188reachable modules/963edges/32manifest entries/34text artifacts/0violations. Log /tmp/fitnutri-ux31-final-frontend-current.log. This includes the final meal preference and measured mobile header change.
- Static: same interpreter, `sh scripts/check.sh --stage static` —64operations/122schemas/706valid examples/11invalid rejections; no generated drift, test TS/lint PASS. Latest log /tmp/fitnutri-ux31-final-static-closure.log after .mjs helper updates.
- Data: same interpreter, `sh scripts/check.sh --stage data` —40PASS,0fail/error/skip with permitted isolated Docker access; /tmp/fitnutri-ux31-final-data-permitted.log.
- Backend: `python3 /tmp/fitnutri-ux-backend-check.py /tmp/fitnutri-ux31-final-backend.log verify` —actual owned TEST_DB_* privately supplied, Maven offline cache;522Surefire/104files +91Failsafe/27files PASS,0failure/error/skip, BUILD SUCCESS. XML counts independently checked; final-java-summary.json contains only counters.
- Actual built gateway: `node /private/tmp/fitnutri-ux-v3-built-gateway-smoke.mjs` —4real HTTP checks PASS with currentdist+owned Spring; hashed JS/cache headers, real CSRF and protected401/no SPA fallback. Ephemeral listener closed,0business writes. /tmp/fitnutri-ux31-final-built-smoke.log.
- Complete browser: `python3 /private/tmp/fitnutri-ux-v3-browser-check.py /tmp/fitnutri-ux31-final-browser-green.log` —fresh actual installed Chrome155.0.8059.80, all32cases/16files PASS in8.5minutes, one worker,0fail/skip/retry/flaky. Original four obsolete-helper failures are closed in this new full run, not omitted. Public final-browser-summary.json retains sanitized case titles/status/durations and actual version.
- Trace: all51scenario rows and linked executed test-family files validated; mappings are not51invented independent runs. Live model/microphone and additional native browsers remain separately limited.

Backend Change:none. Database Change:none. OpenAPI:none. Branch develop/yuankang-wei. Peer/UAT PENDING. Next UX31-R00 operational handover, then R01 receipt.

## UX31-Q02 — test(ux): close final feedback and full-stack regression gates

Actual files: frontend/src/features/entry/draftReducer.ts, frontend/src/features/entry/draftTypes.ts, frontend/src/features/entry/submissionV2State.ts, frontend/src/features/feedback/SaveFeedbackHost.tsx, frontend/src/pages/AddRecordPage.tsx, frontend/src/styles.css, frontend/tests/component/ActionAffordance.test.tsx, frontend/tests/component/ExerciseReviewV2.test.tsx, frontend/tests/component/FoodSubmitRouteV2.test.tsx, frontend/tests/component/MediaEntryV3Compatibility.test.tsx, frontend/tests/component/SaveFeedbackHost.test.tsx, frontend/tests/component/entry-routes.test.tsx, frontend/tests/component/voice-entry-routes.test.tsx, frontend/tests/e2e/save-feedback-v31.spec.ts, frontend/tests/e2e/core-journeys.mjs, frontend/tests/e2e/browser-journeys.mjs, frontend/tests/unit/foodSubmissionV2State.test.ts, docs/verification/ux-v3-1-test-matrix.md, docs/verification/ux-v3-1-final-browser-summary.json, docs/verification/ux-v3-1-final-java-summary.json.

Backend Change / Database Change / Contract: none. Tests: Final whole frontend620/113files PASS plus15Demo/1integrity/11boundaries/10fault/13pipeline; Java522+PGHTTP91; data40; built4; actual Chrome32/16files8.5m,0fail/skip/retry; static and51trace linksPASS. Historical failures retained and closed.. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-R00 — docs(ux): publish current feedback operation and independent UAT

Actual files: README.md, docs/development.md, docs/verification/ux-v3-uat.md, docs/verification/ux-v3-1-uat.md, docs/implementation/FitNutri_UX_Feedback_and_Record_Presentation_Execution_Plan_v3_1.md, docs/verification/ux-v3-1-browser.md, docs/verification/ux-v3-1-visual-evidence.json, docs/verification/ux-v3-1-screenshots/v31-floating-save-mobile.png.

Backend Change / Database Change / Contract: none. Tests: Documentation local-link and17screenshot-hash checks PASS; complete actual browser32/frontend620/Java522+91/data40/built4 retained; human Peer/UAT remain PENDING; Safari/Edge and live provider limitations explicit. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.

## UX31-R01 — docs(ux): deliver verified v3.1 and local commit receipt

Actual files: README.md, docs/implementation/ux-v3-1-tasks.md, docs/verification/ux-v3-1-final.md.

Backend Change / Database Change / Contract: none. Tests: Final evidence audit PASS: frontend620, Java522+PGHTTP91, data40, real Chrome32/16files0retry, built4;16task identities/51trace links/17screenshot hashes, runtime3HTTP200/no armed fault, unchanged tested business code since3d1ce33. Peer/UAT pending.. Branch: develop/yuankang-wei. Commit: resolve by exact task marker in local git log. Developer: verified within stated scope. Peer: PENDING. UAT: PENDING. No remote action. Next: next ready card in approved queue.
