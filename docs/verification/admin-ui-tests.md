# F16 — Real administrator page developer verification

Branch develop/yuankang-wei; B14 backend commit17e46aa. This package changes frontend only and uses existing v2 admin contracts. No database migration, remote Git action, password reset UI or private-record browsing.

Demo extras.tsx Admin page/table/stats/toolbar/confirmation and existing CSS are reused. Data comes from server search with page/size/totalMatches; status filtering and active/inactive counts explicitly apply only to the current page. Confirmation captures identity/state/version; pending locks controls, known 409 re-reads but does not repeat PATCH, and unknown outcomes retain the exact original version with explicit GET observation/retry. Observed matching state does not claim which request wrote it. Success is retained when refreshing fails. Owner/epoch guards discard late results. The existing shared navigation/logout guard includes this page's pending change. Recovery is memory-only; leaving/logout warnings require rereading the account, not assuming cancellation.

Tests added with implementation:
- tests/contract/admin-api.test.ts: 5 cases for bounded search, exact metadata, duplicate/private-field rejection, strict captured-version PATCH acknowledgment, known ACCOUNT_PROTECTED rejection and exact-identity recovery across pages.
- tests/component/admin.test.tsx: 11 cases for USER route denial, honest page scope/self protection, cancellation/no write, single versioned confirmation, pending locks, conflict reread/no automatic repeat, uncertain observation/exact retry, protected rejection, read failure/reactivation, acknowledged-success retention and owner/epoch change.
- Existing session-auth test now expects the implemented page title rather than its removed scaffold; role isolation and no ADMIN profile fetch assertions are retained. It mocks the directory API instead of making real HTTP in a component test.

Actual commands/results: focused API/component16 PASS; full `npm --prefix frontend test`231 PASS/0failed; `run typecheck:test`, `run lint`, `run build` PASS. Logs /tmp/fitnutri-f16-focused-fixed.log, /tmp/fitnutri-f16-full-fixed.log and /tmp/fitnutri-f16-final-{types,lint,build}.log. Initial full regression failed only on the old scaffold title assertion; updated test verifies the real page. Initial strict-response case exposed extra-field stripping and was corrected by rejecting unknown admin response fields. Production bundle913.73kB/278.46gzip warning remains, not suppressed; F17 performance work pending.

Real browser → Vite5173 → SpringBoot8080 → PostgreSQL16 verification:
- Restarted only the dedicated local backend. First launch with DB_URL alone safely failed to connect to default5432; corrected launch explicitly uses SPRING_DATASOURCE_URL/USERNAME/PASSWORD for fitnutri_integration_browser_20261016. No schema migration ran.
- Explicit controlled bootstrap creates NEW synthetic.admin@example.test ADMIN; no existing USER is promoted. Server loopback only, bootstrap default in code remains false. The running developer process has the explicit local flag; production is unchanged.
- Signed in through real UI; account list shows the two synthetic identities, six fields and protected self-deactivation.
- Cancelled deactivation and independently read PostgreSQL: USER remained enabled/version0.
- Confirmed deactivation: UI shows disabled/version1 and success. Confirmed reactivation with capturedversion1: UI enabled/version2 and success.
- SQL after restore: food2 physical rows (id1 date2026-10-15/v2/1318.000kJ/nondeleted; id2 date2026-10-16/v1/412.000kJ/deleted), exercise1 (date2026-10-15/v1/MISSING_WEIGHT/energyNULL), receipts2, weight1 (2026-10-16/71.50kg/v2). All these values/counts match the prior correction package; only intended USER account version/state and newly bootstrapped synthetic ADMIN change. An initial read query used record_date instead of the actual recorded_date column and failed read-only; corrected query above succeeded.
- Screenshots admin-deactivation-confirmation.jpg, admin-deactivated.jpg and admin-reactivated.jpg; final screenshot visually inspected.

Developer verification PASS; independent group review/shared rollout/UAT PENDING. M1 final regression/Q05/Q06 and F15/F17/F18 remain; this does not mark the full milestone complete.

Commit-gate correction: after commit2b23578, the additional Axios error-normalization assertion was found to use a non-UUID requestId. The strict decoder correctly treated that malformed envelope as unknown. The final contract/route PASS statement in that commit message was premature: the command output contained one failure and was not properly gated before committing. The fixture now uses a valid synthetic UUID; no production decoder is relaxed. Full frontend rerun /tmp/fitnutri-f16-verified-full.log:231 PASS/0failed; typecheck:test and lint PASS in /tmp/fitnutri-f16-verified-{types,lint}.log. This follow-up preserves the audit history instead of rewriting it.
