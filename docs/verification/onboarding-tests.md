# F06 — real atomic three-step questionnaire

Branch develop/yuankang-wei. Frontend only; no backend/database change in this work package. Depends on tested B03/B04. Peer review/UAT pending.

Added OnboardingPage, shared ProfileSettingsForm/ProfileSections and profileForm boundary helpers; adapted the Demo three-step controls, labels, copy, stepper/back/goal choices/safety consent and classes. Inputs initialize once from a server snapshot and retain the baseline versions. Complete setup performs one canonical POST including initialWeight and explicit false consent; earlier steps write nothing. Field errors map back to their step. Unknown outcomes freeze further saves, persist a recovery flag in the current owner session and permit only GET/review; already-completed conflicts also use GET. A server-confirmed response is required before publishing completion. Malformed success responses are classified as unknown attempted writes, while invalid client payloads are rejected before dispatch. Response/raw health data is not logged or persisted locally.

NavigationSafetyContext combines form and entry draft protection into one router blocker. Pending saves block leaving; unknown form warnings describe settings rather than submission receipts. Logout warns about unsaved forms and cannot run during a save. A synchronous confirmed-save state commit clears this form's navigation block with completion; initial component tests caught the previous success-redirect race. Brand now has an explicit accessible name. Demo stylesheet base is retained with a small fieldset compatibility extension.

Energy goal input keeps canonical physical kJ while switching display unit; editing kcal uses decimal integer arithmetic and one HALF_UP rounding to kJ3. Unit tests cover untouched physical-value preservation, null clearing and invalid/exponent values. Settings PUT foundation is shared but the actual Profile page/weight controls are F14 next, not claimed delivered here.

Actual commands:

```
npm --prefix frontend test
npm --prefix frontend run typecheck:test
npm --prefix frontend run lint
npm --prefix frontend run build
```

Test-first unit run failed missing profileForm module (/tmp/fitnutri-f06-red.log); initial unit4 PASS. First combined run had3 component failures (successful redirect blocked by dirty form, and Brand accessible-name mismatch); fixed in implementation. Test TypeScript also rejected unsupported exact role options and an unused import; corrected tests without loosening checks. Final76 PASS across11 files, zero failures/skips (/tmp/fitnutri-f06-final-tests.log); typecheck/lint/build PASS (/tmp/fitnutri-f06-final-*.log). New16 tests: profile form unit4, component8, API contract3 and session unknown-recovery1. Tests assert back retention/no early write, false consent and one final POST, field errors/no false completion, timeout/read-only recovery, already-completed conflict, incomplete input/leave guard, incomplete GET keeping POST locked and repeated pending clicks creating one request. Current bundle warning remains visible (~730KB). git diff --check PASS.

Real browser smoke with synthetic account browser.smoke@example.test and dedicated fitnutri_integration_browser_20261016: first two UI steps left profile count0/weight count0 (read-only SQL reconciliation). Final UI step selected NO and kept QA consent unchecked. One submission succeeded; PostgreSQL read-back showed onboarding_completed=true, adult_confirmed=true, consent=false, goal=MAINTAIN, target8400.000kJ, profileVersion1 and exactly one weight70.25kg. Refresh stayed on /dashboard. Screenshot screenshots/onboarding-review.jpg shows actual step3 before final save. Dashboard itself remains an explicit placeholder pending F11/B13; this evidence proves the onboarding identity/transaction/refresh path, not complete M1 E2E or an implemented data dashboard. No production data, remote operation, peer review or UAT.
