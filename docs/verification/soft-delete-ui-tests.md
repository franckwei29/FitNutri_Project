# F13 confirmed soft-delete UI — task remains IN_PROGRESS

Developer self-check PASS for deletion; versioned edit/review UI remains in progress. Independent review/UAT PENDING. Backend/database schema unchanged. Only this session’s newly created synthetic browser record was soft-deleted; no supplied baseline/shared/original user data was changed or removed.

Saved-record details now offer a second explicit deletion confirmation. The confirmation captures the reviewed record/version, disables duplicate interaction while pending, and does not silently replace the expected version after conflict. Cancel makes no API call. A known conflict keeps the review visible and offers an explicit current-record reload without sending another DELETE. An uncertain outcome closes the detail overlay so the shared frozen recovery controls remain accessible. Scope/unmount guards prevent late completion from navigating another account/page.

New `tests/component/record-deletion-control.test.tsx` **6 cases** cover cancel/no DELETE, captured version and disabled pending controls, explicit conflict reload/no automatic retry, uncertain outcome with GET-only recovery, and actual active-query refresh behavior for expected typed404 versus genuine500. Existing History tests now use the same owned DraftProvider as the real app. Tests were added first; initial missing-component run failed.

Actual browser smoke found that an acknowledged deletion’s expected detail404 was being counted as a failed overview refresh. Added a real QueryObserver regression which reproduced the false warning, then corrected refresh handling: invalidate all relevant queries, including the deleted detail; only the deleted target’s typed `NOT_FOUND` is an expected terminal read. A genuine detail/server/network failure still sets saved-refresh-failed. No failed test/read or permission check was suppressed. Centralized by-ID query identity in queryKeys.

Actual commands: `npm --prefix frontend test` **199 passed / 0 failed**; `typecheck:test`, `lint`, `build`, `git diff --check` PASS. Targeted deletion/History regression passed. Existing production bundle-size warning remains; backend unchanged, no new Java run claimed.

Actual React → Session/CSRF → PostgreSQL smoke on the dedicated synthetic database:

- Opened this session’s chicken record (ID 2, 100 g, Dinner, 2026-10-16). Opened confirmation and canceled. Read-only DB check remained **2 physical foods / 2 active foods / 2 receipts / record version0**.
- Confirmed deletion through the real UI. Read-only verification: **2 physical foods / 1 active food / 1 activity / 2 receipts**. Deleted row remains at **version1**, nonnull deleted timestamp, with original **412.000 kJ** snapshot. No physical row or receipt was removed.
- Actual owner HTTP list excludes ID 2; by-ID GET returns typed404; remaining intake is **988.500 kJ**. The original FOOD_BATCH receipt still identifies both IDs 1 and 2 at creation version0. Reading that proof did not restore the deleted item. The original baseline database was not targeted.
- Retried only view reads after the refresh fix; the real UI shows deleted state and the corrected list/facts without a false refresh warning. No second DELETE was sent for this fix.

Evidence: `screenshots/soft-delete-confirmation.jpg`, `soft-delete-confirmed.jpg`. These are developer tests, not peer review/UAT or full F13 acceptance. Next is food/exercise correction Review, explicit latest-version comparison/reload, unchanged snapshot preservation and real browser PUT verification.
