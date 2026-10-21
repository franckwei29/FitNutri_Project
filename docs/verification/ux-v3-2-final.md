# FitNutri v3.2 delivery — IN_REVIEW

## Implemented scope

| Requirement | Delivered behavior |
|---|---|
| RF32-01/02 | Shared Food saved / Exercise saved portal at viewport center, green/white bold single line, total 2000ms; hidden-time/owner/dedup protection retained |
| RF32-03/04 | Scheme A compact date/Today/Refresh toolbar and dated saved-context strip with View records and close; retry remains read-only |
| RF32-05/06/07 | Short labelled icons for History/Goals/Data table; shared View/more/Edit/Log again/Delete; More food/exercise and detail Retry/Close/View date |
| RF32-08 | Add a record/Add food/Add exercise unchanged |
| RF32-09 | Same backend/contracts/schema, creation proofs, CSRF/session, draft and version guards |

## Developer evidence

- Frontend: 113 files / 622 tests passed; legacy 15 + integrity 1, boundaries 11, fault transport 10, pipeline/deployment/artifact 13; build passed. `/tmp/ux32-frontend.log`.
- Static: OpenAPI 64 operations/122 schemas, 706 accepted and 11 invalid examples rejected; generated contracts/types/lint passed. `/tmp/ux32-static-final.log`.
- Data: 40 tests passed, 0 failures/skips. `/tmp/ux32-data.log`.
- Java: 522 Surefire and 91 Failsafe tests passed, zero failures/errors/skips. [XML summary](ux-v3-2-java-summary.json), `/tmp/ux32-backend.log`.
- Browser: final complete current suite 34/34 passed, 0 failed/skipped/flaky/retries; includes genuine browser 200 percent zoom. [Sanitized current run](ux-v3-2-browser-summary.json), /tmp/ux32-browser-full.log. Earlier scoped runs are additional evidence, not added to the final 34 count.
- Visual: 360 and 1440 screenshots opened and checked; all five widths measured for centered bounding boxes and overflow. [Evidence](ux-v3-2-visual-evidence.json).

## Failures and fixes

Initial Host RED: 4 failed/2 passed, then 6 passed. Invalid MUI icon export corrected. F02 history had 3 dependent-label failures pending F04 implementation; F03 initially 11 render failures due unavailable DeleteOutline export, fixed to installed DeleteOutlined; final scoped 25 passed. Old full Vitest run 2 failed/620 passed from obsolete multi-food/Workout messages; amended labels while preserving receipt/SQL assertions, current full 622 passed. Backend first run 6 failures/1 error caused by absent FFmpeg; installed isolated real FFmpeg 7.1 and full rerun 522+91 passed. Python requirements absent initially; isolated requirements installed, static/data rerun passed. Automatic approval timed out on an old-process check; retry confirmed old process already ended. Browser setup failures were preserved and rerun: missing synthetic admin prefix was corrected only on the newly provisioned test administrator email (role/password unchanged); genuine zoom initially inherited an incompatible deviceScaleFactor fixture, explicitly unset and then passed. First full browser run had wrong proxy environment: core request-count assertion failed after successful business assertions. Stopped the exact runner, retained failure JSON privately, set explicit isolated 18092 proxy and restarted full suite. UAT proxy nextDrop was null. None of these failing/interrupted runs counted as PASS.

## Runtime and acceptance

UAT target http://localhost:15173 (Spring 18080). Existing UAT database and accounts retained. Automated checks exclusively use independent 15183/18090/18092 and isolated test databases; no migration/restore/cleanup of UAT data. Runtime health is rechecked before final handover; final public checks recorded in results. [User checklist](ux-v3-2-uat.md).

Peer review PENDING. User UAT PENDING. Live AI/microphone/external models are not accepted by this presentation release. No remote Git operations. Branch develop/yuankang-wei. Exact task commits are listed in the task ledger; the final R01 commit identifies this receipt without a circular embedded hash.

## Final audit

All 12 tasks implemented and developer verified (IN_REVIEW). Final branch develop/yuankang-wei. Public runtime checks: frontend15173 HTTP200; backend18080 UP; proxied CSRF HTTP200; UAT and test proxy nextDrop null. Browser opened at http://localhost:15173. Add food/Add exercise original JSX exact comparison passed; header/layout source unchanged. No changes under backend/API/data/deploy. Existing untracked metadata and private runtimes retained; only current explicitly reviewed files committed. No push/PR/remote Git. [Task commits](../implementation/ux-v3-2-tasks.md).
