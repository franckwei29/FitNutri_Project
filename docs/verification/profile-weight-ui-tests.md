# F14 — independent settings and weight actions

Branch develop/yuankang-wei. Frontend-only work; no backend/database schema change. Peer review and UAT pending.

Added ProfilePage, WeightEntryForm and typed weight API. Reused Demo settings/weight layout and existing shared settings form. Settings snapshot and expected versions remain fixed until explicit save/reset/reload; background weight/profile refresh never overwrites typed settings. Null goals/reference clear through ordinary PUT. Energy unit changes preserve physical kJ. Existing adult reconfirmation uses settings PUT, without another initial weight.

Weight controls are a separate form/action. History uses owner-scoped query keys, bounded date range, explicit loading/error/retry/empty states and AbortSignal. Choosing a saved measurement carries its version. An unversioned conflicting value opens review before a new versioned correction; invalid weight/date never dispatches. Server current-item conflicts are schema-validated and retained through sanitized ApiRequestError normalization only for /api/profile/weight. Unknown outcomes freeze writes and persist an owner-session recovery request, then read/reconcile by date without automatic POST retry. Confirmed save invalidates owner queries and refreshes profile; a local refresh failure still reports the verified save, clears the form and does not offer duplicate confirmation. Existing activity values are untouched. Aggregate navigation registry preserves protection for both forms; saving one cannot clear another dirty form's warning. Owner logout/expiry clears recovery state.

New14 tests: component9 (goal/null clearing, physical unit preservation, stable baseline across weight refresh + version409 read/reload, explicit conflict review, history correction/invalid input, unknown GET recovery, combined dirty-form navigation, history failure, post-ack local refresh failure); weight contract2; sanitized conflict/calendar unit2; session recovery1. Test-first weight API run failed missing module (/tmp/fitnutri-f14-red.log). First functional run86 PASS; types/lint caught an unused test import. Added tests reached89 PASS. A first attempt at the post-ack regression test failed because arrow methods are instance fields rather than prototype methods; fixed the test to capture/spy the actual session instance. No rules disabled.

Final actual commands:

```
npm --prefix frontend test
npm --prefix frontend run typecheck:test
npm --prefix frontend run lint
npm --prefix frontend run build
```

Final90 tests across14 files PASS, zero failures/skips (/tmp/fitnutri-f14-final-tests.log). Strict test/app types, lint and build PASS (/tmp/fitnutri-f14-final-*.log). Bundle warning746.41KB remains visible. git diff --check PASS.

Real browser -> Vite -> Spring -> dedicated synthetic PG16 verification: switched existing8400kJ target to KCAL; read-back remained8400.000kJ and weight rows1. Removed goal/reference and saved; refresh/read-back showed both SQL NULL. Entered a different same-day weight without version; server409 displayed current70.25kg vs proposed71.25kg and required explicit correction. The server committed that correction, but browser hot reload retained an old SessionController instance without its newly added method, exposing local post-ack handling. No duplicate POST was sent: cancelled old dialog, cleared the ephemeral form, reloaded and read back row71.25/version1/count1. Fixed post-ack handling and added the regression test. With a fresh module instance, selected that history row and explicitly corrected to71.50; real UI reported success, final read-back weight version2/count1/profile version5/cleared goals true. No existing data or user credentials were involved. Screenshots: weight-correction-review.jpg and profile-weight-saved.jpg. This is profile/weight smoke, not full M1 E2E. No shared DB changes, remote Git operations, peer review or UAT. Next: D03 catalogue/versioned reference foundation.
