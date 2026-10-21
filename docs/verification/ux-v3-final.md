# FitNutri V3 actual developer handover

Active specification: [V3](../implementation/FitNutri_UX_Refinement_Execution_Plan_v3.md). Work resumed from UX-D01 and continued through the remaining dependency-ready cards without batch approval pauses. Exact local commits, per-card files/assertions/commands/failures and limitations are in the [52-card ledger](../implementation/ux-v3-tasks.md), [actual results](ux-v3-results.md), [backend/database register](../implementation/ux-v3-backend-changes.md) and [129-scenario trace](ux-v3-test-matrix.md). Mapped scenarios are not129 independently executed variants.

Branch: **develop/yuankang-wei**. No fetch/pull/clone/push/PR/merge or replacement baseline. Pre-existing .DS_Store/.idea/.local items remain unstaged and outside delivery. Original V8 binary/database were not upgraded; review V3 at **http://localhost:15173**, with its separately owned Spring18080/PostgreSQL16/V13 runtime. The old8787built review app is a historical V8 target.

## Implementation status

| Area | Actual delivered behavior | Developer verification | Independent acceptance |
|---|---|---|---|
| Existing M1 accounts | Register/login/Session/CSRF, adult onboarding, Profile/units/goals/dated weights, logout/account isolation, admin disable/reactivate | Real UI/API/SQL and retained original regressions | Peer review/UAT PENDING |
| Food | Reviewed bilingual smart search, explicit preparation/approximation, selected references, local continuous Add, meals, authoritative inline preview, shared final Review | Component/contract/Java/real-stack browser | Peer review/UAT PENDING |
| Dishes | PERSONAL or explicit FULL_RECIPE fraction/percent/servings; exact rational consumption; one root with stored ingredient snapshots | Exact independent expected values, real PG constraints and browser SQL | Peer review/UAT PENDING |
| Exercise | Product options/movements, Quick sports/equipment, Gym timed periods with optional sets/reps/load; no duplicate movement energy/time | Null/zero/partial estimates, dated weight and unchanged-child assertions, real PG/browser | Peer review/UAT PENDING |
| History/statistics | Typed root pagination, context/highlights/details, whole-root versioned correction/deletion, independent Log again with blank amounts; conserved leaf totals and explicit root counts | Actual multiple dates/roots, portion/date changes, deletion and immutable proof | Peer review/UAT PENDING |
| Reliable confirmation | Owner/epoch-bound immutable body/UUID, one atomic create, original proof GET/replay, stale409 retained, current-date continuation and explicit backfill | Real commit-then-drop, blocked in-flight transaction/404, concurrency and hard reload | Peer review/UAT PENDING |
| UX/accessibility | Short notices, clear actions, compact entry, charts before lists, dated axes/units/tables, narrow consent and keyboard interactions | Actual Chrome375px/desktop and limited native Safari smoke | Edge NOT_RUN; full Safari/assistive/device variants and human UAT PENDING |
| M2 image/voice/Q&A | Frontend candidate/recorder/chat flows and real backend adapters remain implemented; food/voice exercise candidates converge on shared V2 Review; safety/privacy/availability fail closed | Controlled-provider tests and real unconfigured-service behavior | **BLOCKED_EXTERNAL** for live provider/model and human microphone acceptance |

Current source metadata/capabilities are recorded in [sanitized runtime inventory](ux-v3-runtime-inventory.json). Food/exercise directories are verified and ready; all four AI capabilities are configured=false/available=false in this owned runtime. That is a configuration boundary, not a claim that the corresponding frontend/backend code is absent. No canned answer is reported as a real model result.

## Final check closure

| Check | Actual command / evidence | Result |
|---|---|---|
| Contract/static | sh scripts/check.sh --stage static; /tmp/fitnutri-ux-final-static.log | PASS:64operations/122schemas,706valid/11invalid, no generated drift, test TS/lint |
| Complete frontend | sh scripts/check.sh --stage frontend; /tmp/fitnutri-ux-final-frontend-serial.log | PASS:109files600Vitest, originalDemo15+integrity, boundaries11, faultproxy10, pipeline/deployment/artifact13;0fail/skip |
| Production build | Same frontend stage | PASS:183reachable modules/951edges/33manifest entries/35text artifacts,0boundary violations |
| Data/preflight/trace | sh scripts/check.sh --stage data; /tmp/fitnutri-ux-final-data.log | PASS:40tests,0fail/skip; source bytes, readonly preflight and129scenario link/52card integrity |
| Complete current Java/PG/HTTP/restore | ./backend/mvnw -f backend/pom.xml verify with explicit isolated TEST_DB_*; /tmp/fitnutri-ux-final-backend-serial.log | PASS:522Surefire +91Failsafe,0fail/error/skip, BUILD SUCCESS in2m45s |
| Complete actual browser | npm --prefix frontend run test:e2e; explicit owned environment and installed browser | PASS:27/27 in14files,7.8minutes;0fail/skip/retry; /tmp/fitnutri-ux-final-browser.log |
| Built application HTTP smoke | Actual dist + ephemeral gateway + real Spring; /tmp/fitnutri-ux-final-built-smoke.log | PASS:4requests, real hashed JS/CSRF/unauthenticated401JSON; listener closed,no business writes |
| Running artifact parity | /tmp/fitnutri-ux-final-artifact-parity.log | PASS:all374business classes match current compiled classes, every class has current source; immutable SHA25641a0c23750b023c3b7c4152250c74bd2f12b5e8291e9023aa5d67b12a245a5ad |
| Actual readonly runtime preflight | scripts/preflight_ux_v3.py with owned UUID DB and pinned source dir; /tmp/fitnutri-ux-r00-runtime-preflight.log | PASS:zero issues,readonly transaction,migrationApplied=false |
| Old binary boundary | Actual immutable V8 on a fresh V13 restore clone; [migration evidence](ux-v3-migration.md) | PASS:explicit V8 rejection, no listening server, all public rows unchanged |

Full first-round failures remain recorded: /tmp/fitnutri-ux-final-frontend.log596PASS/4async failures under concurrent checks; original assertions/timeouts retained, affected22passed independently and the complete serial600passed. First full Java attempt /tmp/fitnutri-ux-final-backend.log522SurefirePASS/90of91FailsafePASS failed the restored executable's15second startup bound; it is not a full PASS. The complete serial rerun closed it with522+91PASS and BUILD SUCCESS, without changing the15second bound. Browser selector/readiness failures and their exact corrected checkpoints are retained in ux-v3-results.md; no blind retries/skip annotations or relaxed ownership/SQL constraints were used.

Actual Chrome ordinary-read sample:14–28ms across6sequential local reads, saved notice visibly removed in2813ms; [sample](ux-v3-operation-sample.json). These are bounded ordinary-operation observations, not a production load/SLO certification. Actual [desktop](ux-v3-screenshots/dashboard-accessibility.png), [mobile](ux-v3-screenshots/mobile-dashboard.png), [multiline consent](ux-v3-screenshots/mobile-consent.png) and [grouped conservation](ux-v3-screenshots/grouped-conservation.png) screenshots were inspected. Native Safari27.0.1smoke is distinct from Chrome155.0.8059.80automation; see [browser limits](ux-v3-browser.md).

## Accepted limits and handover

Hard reload deliberately forgets drafts and submission IDs. It never automatically resends. A server transaction may still commit; manually check History before recording again. Identical content with fresh root/submission IDs is a legal new occurrence, so this design cannot promise to identify every repeated user intent after reload.

Use the [operational handover and unchecked chapter22UAT steps](ux-v3-uat.md). Database rollout requires explicit target identity, readonly preflight, verified protected backup/new-database restore and matching V13 deployment; startup never adopts an old schema. Do not downgrade a binary ontoV13 or truncate/down-migrate data to force compatibility.

All implemented/developer-verified cards remain **IN_REVIEW**, with **peer review PENDING** and **UAT PENDING**. Negative main/no-Git branch fixtures were not performed against this project; the actual personal branch gate and explicit staging were audited. Edge is unavailable, the native Safari smoke does not certify the full suite, and live provider/human microphone acceptance remains BLOCKED_EXTERNAL. Final branch/commit/check receipts are appended only after actual completion.


## Final delivery receipt

All52V3cards are now developer **IN_REVIEW**, including the42remaining cards from D01; all peer/UAT columns remain PENDING. The42cards have43small local commits after the D00checkpoint because D01has a separate storage-regression follow-up. Find the exact final receipt SHA with `git log -1 --format=%H --fixed-strings --grep='[UX-R01]'`; the ledger resolves the other51task IDs to actual local SHAs. No remote operation was performed.

Complete final actual browser run:27/27PASS,14files,one worker,zero retries/failures/skips,7.8minutes in /tmp/fitnutri-ux-final-browser.log. [Sanitized per-case result/duration summary](ux-v3-final-browser-summary.json) is derived from Playwright's actual result JSON, with every result asserted passed/retry0. It covers core accounts, four food/portion modes, Quick/Gym, grouped conservation, V1 compatibility, other USER/ADMIN privacy, account/weight settings, conflict/deletion, real dropped acknowledgements/404/in-flight reload and keyboard/mobile/notice flows. Final whole Java rerun522+91PASS and full frontend600PASS close every earlier recorded failure; no failure was skipped or signed off as a waiver.

Production dist + actual Spring also passed4ephemeral gateway HTTP checks (real hashed module, CSRF JSON, unauthenticated401/no HTML fallback); no business record was written and its temporary listener was closed. Running owned backend matches all374current business classes and every class has a current source file. Current app remains available at localhost15173. Final git checks preserve the personal branch, generated files and source-only staging; pre-existing .DS_Store/.idea/.local files remain untouched/untracked. Negative unsafe-branch fixtures, Edge, full Safari/assistive/physical-device variants and live model/human microphone remain exactly as limited above; automated green does not replace human acceptance.
