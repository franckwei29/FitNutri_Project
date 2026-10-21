# FitNutri v3.1 — implementation and verification handover

Date: 2026-10-18 (Australia/Sydney). **All 16 task cards have implementation and developer verification completed, with status IN_REVIEW; peer review and user UAT are PENDING.** This report is not human acceptance.

The actual code baseline was `139236d734434bef54289a27ef785e7b501f4f9c`. The final business-code and paired-test commit was `3d1ce33` (UX31-Q02); operation/UAT documentation was committed as `b7f6878` (UX31-R00). No business source, backend, contract or database changes followed within this delivery.

This receipt accompanies UX31-R01. Its own SHA is not embedded in its contents, avoiding a circular reference. Resolve it with:

```sh
git log -1 --format=%H --fixed-strings --grep='[UX31-R01]'
```

## Requirements checklist

| ID | Actual delivery | Developer status |
|---|---|---|
| RF-01 | Singles, dishes/mixed batches, Quick and Workout return once to today's Dashboard after complete receipt verification; creation and correction follow separate flows | Implemented and tested |
| RF-02 | Green nonblocking floating notice does not affect document flow or steal focus; positioned using actual navigation height and layered below menus/confirmation dialogs | Implemented and tested |
| RF-03 | Display for two seconds after actual destination mounting, then fade over 200ms; pause in the background, respect reduced motion and do not replay after leaving | Implemented and tested |
| RF-04 | AU English Food saved / Exercise saved / Workout saved wording; one batch notice based on root counts | Implemented and tested |
| RF-05 | Backdated saves still return to today without changing today's totals; date and typed-root History access remain independent of the brief notice | Implemented and tested |
| RF-06 | Dashboard/History root cards emphasize name, portion/duration and energy, with secondary macronutrients; preserve spacing, wrapping, grouping and pagination | Implemented and tested |
| RF-07 | More actions contains edit, log again and delete; preserve locks, version/draft confirmations and errors; support keyboard menus and focus return | Implemented and tested |
| RF-08 | Details retain complete saved ingredients/consumed portions, exercise periods/training information, references and saved versions; no existing information removed | Implemented and tested |
| RF-09 | Reorder date/meal controls, compact recent items, clearly select Quick/Gym; focus required amount/time after selection without accepting previous amounts automatically | Implemented and tested |
| RF-10 | History correction preserves compatible date/type/filter/location context; date changes, whole-root deletion and Log again follow existing rules | Implemented and tested |

Additional business safeguards remain: review all candidates; no success on unknown outcomes or mismatched receipts; safe retry with the same frozen payload/ID; cross-account/epoch response isolation; separate save success from read failure; read retries never POST. The next manual in-app entry preserves the confirmed default meal/exercise mode, uses today, leaves quantities empty and creates fresh identities. Preferences and feedback remain in the current owner's session memory only.

Backend Change: **none**. Database Change: **none**. OpenAPI Change: **none**. This delivery did not change Java, migrations, official data, authorization, nutrition/exercise calculations or backend snapshot semantics.

## Final observed verification

| Gate | Final result | Evidence |
|---|---|---|
| Frontend Unit / Component / Contract | **113 files, 620 PASS, zero failures/skips** | `sh scripts/check.sh --stage frontend`; `/tmp/fitnutri-ux31-final-frontend-current.log` |
| Original Demo and frontend foundation | Demo15, fixture integrity1, boundary11, real-forwarding faults10, deployment/artifact/pipeline13 all passed | Same complete frontend gate; historical results were not counted as current execution |
| TypeScript / lint / contract | 64 operations, 122 schemas, 706 valid examples, 11 invalid examples rejected; no generated drift; TS/lint passed | `/tmp/fitnutri-ux31-final-static-closure.log` |
| Java Surefire | **104 report files, 522 PASS, zero failures/errors/skips** | Actual Maven verify, independently counted XML |
| Isolated PG / HTTP Failsafe | **27 report files, 91 PASS, zero failures/errors/skips** | Same complete verify; `/tmp/fitnutri-ux31-final-backend.log`, BUILD SUCCESS |
| Data/preflight/source tools | **40 PASS** | `/tmp/fitnutri-ux31-final-data-permitted.log` |
| Complete real-browser suite | **32/32 PASS, 16 files, zero failures/skips/retries/flaky cases, 8.5 minutes** | Actual Chrome155.0.8059.80; `/tmp/fitnutri-ux31-final-browser-green.log` |
| Production build/boundary | **188 reachable modules, 963 dependency edges, 32 manifest entries, 34 text artifacts, zero violations** | Complete build and production boundary gate |
| Built-gateway smoke | **4 PASS**: real HTML/hashed JS/cache, CSRF JSON, protected401 without SPA fallback | `/tmp/fitnutri-ux31-final-built-smoke.log`; temporary listener stopped, zero business writes |
| Traceability/documentation | 16 cards, 51 scenario mappings, all test links and 17 screenshot hashes checked | 51 is the mapping count, not 51 independent executions |

The 522 Surefire cases include existing real HTTP/PG tests and are not all pure unit tests. The backend wrapper privately supplied the owned TEST_DB_* values and ran Maven verify. Complete commands, failures and fixes are retained in execution evidence. Tests used existing tool versions/caches; no dependency upgrades or paid model calls were made.

The actual chain was React/Vite15173 → real forwarder18082 → matching immutable Spring18080 → owned UUID PostgreSQL16/V13. No business responses were fabricated with route.fulfill. Read/module failures were simulated through real network interruption; writes, receipts, roles and summaries were verified against actual services and SQL.

The final 32 cases include registration/onboarding/profile/weight/admin/two-session concurrency/legacy V1 receipts/permissions, plus mobile feedback, historical dates, read failures, module-load failures, lost-receipt/404 recovery, visual/keyboard behavior and the complete Workout lifecycle. Coverage was not limited to new happy paths.

## Failure closure

[Per-package execution evidence](ux-v3-1-results.md) retains actual issues: feedback preceding route-guard updates; date-action overflow at360px; nonwrapping navigation at200% CSS zoom; focus after closing details/deleting records; lost default meal after return; and the assumed fixed mobile navigation height. Each received corresponding code/test corrections and actual revalidation.

Legacy component/MJS helper assertions expecting stay-on-entry after save were updated for the approved requirement. SQL, receipt, authorization, unknown-outcome and candidate-review assertions remained. The first full browser run loaded stale helpers and had four failures before explicit interruption (11 passed, one interrupted,16 not run); it was never counted as passing. A fresh complete32-case run closed those issues with zero failures. Sandbox Docker/listener access failures are also retained and were followed by permitted full reruns, without skipped cases or relaxed timeouts.

## Human acceptance and external scope

- Peer review: **PENDING**; user UAT: **PENDING**, as shown by the unchecked checklist.
- Visual evidence is developer screenshot inspection at actual Chrome360/375/768/1280/1440px and200% document CSS zoom, not browser-toolbar/OS zoom or complete assistive-technology certification.
- Safari in this delivery: **NOT_RUN**; Edge: not installed/**NOT_RUN**. Prior V3 native Safari smoke remains historical evidence.
- M2 image/voice/Q&A adapters and controlled tests remain. The owned runtime has no configured real model service. Model quality and human microphone acceptance remain **BLOCKED_EXTERNAL / PENDING**; this UI regression does not complete them.
- The17 archived images contain synthetic test-user data only. Screenshot inspection does not substitute for user UAT.

## Git, data and runtime receipt

Required/current branch: `develop/yuankang-wei`. Each of the16 cards has a local Conventional Commit in the ledger. Existing untracked .DS_Store/.idea/.local contents were preserved. No fetch/pull/clone/push, PR or merge occurred; no database was cleared, code baseline replaced, original V8 database migrated or original application service stopped. Only this delivery's owned Playwright runner was gracefully interrupted to reload stale assertions, then rerun completely.

Readonly pre-delivery checks found frontend15173, forwarded real `/api/auth/csrf` and Spring `/actuator/health` returning200, with health UP and no armed fault remaining in the forwarder. Business source had not changed since UX31-Q02.

- [Open the historical running system](http://localhost:15173)
- [16 cards and local commits](../implementation/ux-v3-1-tasks.md)
- [Complete operations and independent UAT](ux-v3-1-uat.md)
- [Actual test trace for51 scenarios](ux-v3-1-test-matrix.md)
- [32 real-browser results](ux-v3-1-final-browser-summary.json) · [Actual Java/PG counts](ux-v3-1-final-java-summary.json)
- [Visual scope and limitations](ux-v3-1-browser.md) · [Dimensions/hashes of17 screenshots](ux-v3-1-visual-evidence.json)
- [Desktop card](ux-v3-1-screenshots/v31-food-card-1440.png) · [Mobile card](ux-v3-1-screenshots/v31-food-card-360.png) · [Mobile floating-save notice](ux-v3-1-screenshots/v31-floating-save-mobile.png)
