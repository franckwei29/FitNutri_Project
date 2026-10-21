# FitNutri: Demo → Production Project Integration Execution Specification

> English handover edition (2026-10-18): this historical specification retains its original baseline, task statuses and instructions as evidence. It does not grant new execution authority or certify the current implementation. Current submission prose is English; functional multilingual inputs are retained. For installation use [the fresh-start guide](../getting-started.md), and for this handover use [the validation report](../verification/team-handover-validation.md).

> **Audience: coding AI agents, code reviewers, frontend and backend developers, and QA.**
> **Document version: 2.0 (final single-file execution edition) | Updated: 2026-10-16 | Status: pending implementation on the user's local development branch; not yet executed.**
> **Final objective: retain the Demo's visual design and interactions while connecting it to the existing Java/Spring Boot/PostgreSQL project; stop presenting a browser-simulated database as a real backend.**
> This file covers development rules, evidence of the current state, target APIs, database migrations, per-file responsibilities, page state machines, task cards, commit boundaries, and acceptance gates. Code blocks are contracts, algorithm outlines, or commands to implement; they are not complete implementations already present in the project.

**First instruction to the implementing AI: verify locally and enter the specified branch `develop/yuankang-wei` (Branch Gate = PASS); then execute P00–P03. Thereafter, implement features and their automated tests in dependency order, actually run them, review your work, and commit locally. Do not overwrite `frontend/` by copying the Demo, begin with bulk Controller changes, or run database migrations first.**

**Single implementation specification: this file already includes the development branch restriction, course testing requirements, tests delivered alongside development, the original 69 task cards, all backend/database changes, and complete M1/M2 acceptance criteria. Implementation requires only this file, the actual project source, and the Demo source; no older prompt or additional execution Markdown is required. Course Lab9/L9 Testing materials are references for checking course practices and do not independently change this file's execution instructions.**

## Reading and execution guide

| Task | Read |
|---|---|
| Lock the personal development branch, begin development, and commit code | Sections 0.0, 0.3, 3, and 23 |
| Understand data flow and the target code structure | Sections 4–6 |
| Change APIs or the database | Sections 7–11; every change must reference a BC ID |
| Migrate pages and manage cross-page state | Sections 12–16 |
| Replace simulated image, speech, and Q&A functionality with real services | Section 17 |
| Find concrete tasks an AI can take | Section 18 |
| Deliver tests with features, CI, migration rehearsals, and final handover | Sections 0.7 and 18–23 |
| Check source evidence, terminology, and actual dependencies | Appendices A–D |

---

# 0. Rules to establish before development

## 0.0 Mandatory working branch: `develop/yuankang-wei` (before any write)

**This is the sole development branch explicitly specified by the user; its exact spelling is `develop/yuankang-wei`. It is neither a wildcard nor a naming suggestion.** This section overrides older generic examples such as a default feature branch or suggested integration branch. Integration implementation, tests, documentation, and local commits for this project all belong on this local branch by default. Do not substitute a new feature, integration, or test branch, and never modify `main`, `master`, shared `develop`, another member's branch, or a detached HEAD.

The IDE screenshot supplied by the user indicates the intended name; **it does not replace verification with Git commands in the actual working directory**. The implementing AI must begin with these read-only commands:

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

Workflow:

1. Verify that the current directory is the project worktree the user actually intends to modify, rather than a temporary directory containing only extracted ZIP files. If `git rev-parse` fails, stop project writes. Do not independently run `git init`, `git clone`, or retrieve the repository remotely. Ask the user for the existing local Git worktree path.
2. If the current branch **exactly equals** `develop/yuankang-wei`, inspect uncommitted changes, record existing user/team contributions, and identify files the AI can safely edit. Do not count pre-existing changes as this work's contribution.
3. If the current branch differs, `git switch develop/yuankang-wei` is allowed only when that branch **already exists locally** and switching cannot affect uncommitted content. Report conflicts, uncommitted changes, a missing branch, or a branch occupied by another worktree and wait for the user to resolve them. Never automatically stash, reset, clean, or create a branch.
4. After switching, rerun `git branch --show-current` and `git status --short --branch`. The branch must be exactly `develop/yuankang-wei`, HEAD must not be detached, and the Git root must match the user's working directory.
5. Once verified, record `BASE_COMMIT=$(git rev-parse HEAD)`, the original change list, and checksums of both ZIP files. If branch contents differ from the ZIPs, review the differences and protect the existing implementation; do not automatically reset the branch to a ZIP.
6. **Before Branch Gate passes, do not:** install or upgrade dependencies that write to the project, modify source/tests/configuration, run database migrations, create project documentation, or commit code. Read-only analysis may continue.
7. Recheck `git branch --show-current` **before writing files for each task and before every commit**. Stop writes immediately if it is not `develop/yuankang-wei`. Reverify when resuming an interrupted session. Multiple agents must not write concurrently to the same worktree; obtain fresh explicit user permission if parallel branches are needed.

**This work is based entirely on the user's local code. Do not access the university GitHub repository, run `git fetch`, `git pull`, or `git clone`, or infer a newer version from a remote repository.** Existing local history may be read to identify differences; **connecting to or verifying a remote server is unnecessary**. Without authorization, do not push, create/merge remote PRs, or change branch protection.

The initial acknowledgement must include:

```text
Project root: <actual canonical worktree path>
Required branch: develop/yuankang-wei
Current branch: <exact git branch --show-current result>
HEAD: <actual local commit SHA>
Pre-existing changes: <list / none>
ZIP comparison: <same / differences + details>
Branch Gate: PASS / BLOCKED
```

If BLOCKED, **do not continue modifying code**; explain only which condition the user must resolve. A selected branch in a screenshot is not evidence of PASS.

## 0.1 Permissions and scope

1. The **design scope** authorized by this plan covers frontend integration, necessary backend fixes, auditable database migrations, and tests within this repository. It is not unconditional authorization to operate production/shared databases, remote repository settings, or teammates' branches.
2. The coding AI must work on the verified local `develop/yuankang-wei` branch and may produce small, reviewable local commits per task. Remote pushes and PR creation/merging require explicit user authorization; otherwise deliver local commits and suggested commands only.
3. Do not commit directly to `main` or the team's protected integration branch. Do not run `git push --force`, `git reset --hard`, or `git clean -fd`, delete others' branches, or independently stash/overwrite uncommitted changes.
4. Do not wipe an existing shared database, delete its volumes, restore over it, or rebuild tables destructively. Do not solve migration problems with `docker compose down -v`.
5. Real business data must not enter seeds, test snapshots, model prompt logs, screenshot attachments, or public commits. Use explicitly synthetic development/test data.
6. Do not request or print API keys; use secret environment variables. Frontend `VITE_*` variables may contain public configuration only, never model keys, database passwords, or session credentials.
7. This document has not changed any original ZIP, GitHub file, branch, or database. Future implementers must not report “plan generated” as “project completed”.

## 0.2 Priority of requirements and facts

Resolve conflicts in this order:

1. New requirements or scope changes explicitly confirmed by the user during execution.
2. FR/NFR and acceptance conditions in the submitted Proposal [REQ].
3. Team-confirmed API contracts and this plan's target design.
4. Existing Demo user experience that does not violate the above.
5. The latest target-branch implementation: prefer reuse, but do not automatically treat known code deviations as requirement changes.

**Evidence labels:** `[SRC]` denotes actual source inspected for this plan; `[REQ]`, the uploaded Proposal; `[DESIGN]`, implementation decisions introduced by this plan; `[VERIFY]`, environments or runtime outcomes to verify during execution; and `[DOC]`, official technical documentation. New routes, classes, and tables are target designs, not features that may be described as already existing.

If the target branch already has an equivalent implementation, retain it and add contracts/tests rather than creating a second implementation merely to match an example class name. If a new dependency conflicts with course rules, record the conflict and pause that item; do not guess what the course permits.

## 0.3 Git branch and commit conventions

**Sole permitted development branch: `develop/yuankang-wei`.** This is the actual target explicitly supplied by the user; see the Branch Gate in 0.0. P00 records the exact branch, BASE_COMMIT, Demo checksums, and existing changes. A ZIP filename containing `main` neither requires development on main nor establishes the current Git branch.

Do not automatically create `feat/*`, `fix/*`, or `integration/*` branches. Make small commits for each logical work package on **the same verified branch**, retaining task and test evidence. Follow new instructions if the user later explicitly authorizes multiple development branches. Without authorization, do not perform remote Git operations; multiple agents may not operate the same worktree concurrently.

Use Conventional Commits [DOC-GIT]. This is this plan's development convention, not an assumed university requirement:

```text
feat(records): add atomic food batch confirmation [B09]
fix(auth): restrict public registration to user accounts [B01]
refactor(frontend): replace demo database context with query hooks [F03]
test(records): cover concurrent replay and rollback [Q02]
docs(db): document legacy backfill and restore procedure [D05]
```

Every commit body must include at least:

```text
Why: User behavior or defect cause; associated FR/NFR.
What: Main modules changed.
Backend-Change: BC-xx, or none.
Database-Change: Migration ID, or none.
Contract: Added/compatible/breaking changes and synchronized clients.
Tests: Commands actually run and their results; explicitly explain anything not run.
Limitations: Known unfinished items, or none.
```

Rules:

- Each commit addresses one explainable logical change. Avoid messages such as “update”, “final”, or “fix everything”.
- Separate pure formatting, component moves, and business logic changes where possible. Large moves may exceed normal diff sizes, but must not conceal authorization/calculation changes.
- Related DTOs, services, and tests should form buildable commits. Closely coupled database/Java field changes may share a PR, but provide a separate migration review view.
- Update `package-lock.json` only when dependencies actually change. Do not incidentally rewrite the whole lockfile because npm versions differ.
- Do not falsify contributions by changing author identity; retain the user's existing Git identity. Preserve attribution for the Demo and teammates' existing code.
- Before committing, run `git diff --check` and relevant tests, and inspect the staged diff. Work that fails to build may remain on a personal branch but cannot be marked Ready for review.
- A local commit is not a push; an opened PR is not a merged PR; passing tests is not acceptance PASS. Report status accurately.

## 0.4 PR and code review gates

Every PR must include task IDs, FR/NFR, BC IDs, before/after screenshots or workflow descriptions, API examples, database impact, test results, rollback instructions, known blockers, and Demo replacement points.

A backend reviewer must check backend changes; database migrations require a separate data-safety review. Decisions involving new reference sources, health-consent semantics, breaking API changes, or additional technical tables must be recorded in an ADR and confirmed by the project lead before integration-branch merge.

Never obtain review approval by deleting failing tests, changing assertions to `true`, swallowing exceptions, disabling strict typing or CSRF, allowing all routes with `permitAll()`, adding fake-data fallback, or substituting zero for unknown values.

## 0.5 Coding conventions

| Area | Convention |
|---|---|
| Language | Keep the user interface in English; documentation may be Chinese; use primarily English code identifiers and comments |
| TypeScript | Enable strict mode in new modules; use `import type`; do not introduce unjustified `any`, `@ts-ignore`, or non-null assertions |
| React | Function components; no write requests during render; no hooks inside loops/conditions; retain StrictMode |
| Forms | Retain working Demo `useState` logic; use reducers for complex submissions. RHF/Zod may be used in new modules, without rewriting all existing forms |
| API | Components do not call Axios directly; use domain API functions and query/mutation hooks |
| State | React Query owns server data; DraftProvider owns unsubmitted drafts; local state owns UI; do not copy the same form into three places |
| Java | Retain `com.fitnutri.backend`; use Jakarta APIs; thin Controllers, Services for transactions/rules, Repositories for queries |
| Construction | Constructor dependency injection; existing Lombok use is acceptable; do not expose entities directly through JSON serialization |
| Numbers | Server-side BigDecimal; one final rounding operation; frontend only formats authoritative server results |
| Time | LocalDate for business dates; Instant for creation/update timestamps; injectable Clock |
| Errors | Machine-readable codes and understandable messages; locatable field errors; no passwords, raw security answers, or raw model output in logs |
| Styling | Retain Demo theme/CSS; do not introduce a second UI framework |
| Files | Normally one primary responsibility per file; split large components at existing function boundaries, not mechanically by line count |
| Configuration | No major-version upgrades of existing dependencies; actual lockfiles/effective POM determine tool versions |

## 0.6 Prohibited integration shortcuts

- Rename Demo `store.ts` to `api.ts` while continuing to write localStorage.
- Add a backend “get the entire Database” endpoint to accommodate the Demo Context.
- Send successive food-record POSTs with `Promise.all` and claim an atomic transaction.
- Let the browser submit calories, MET, or body weight for unconditional backend trust.
- Silently display seed data after API failures, or return chicken, rice, and vegetables for every uploaded image.
- Remove correct Demo behavior such as cancellation, dates, versions, or unavailable estimates merely to fit existing APIs.
- Alter database contents to make assertions pass, or overwrite old reference rows and change historical meaning.
- Make Q&A consent a prerequisite for ordinary users to complete the questionnaire.

## 0.7 Deliver “feature + tests + self-review” as one work package (mandatory; based on course Lab9)

**The complete test plan already covered JUnit, MockMvc, real PostgreSQL integration tests, React tests, and E2E. The added mandatory development cadence requires every business task to create/update its automated tests, with the code author running and reviewing them first. Q00–Q08 in Section 18 provide shared infrastructure and systematic acceptance; they do not permit earlier business tasks to deliver features first and defer unit tests until the end.**

Pages 2–3 of `ELEC5619-Lab9.pdf` introduce JUnit 5, Mockito, the testing pyramid, shift-left testing, Arrange–Act–Assert, and writing tests early. Pages 6–8 of `L9 Testing (1).pptx` emphasize developer-written unit tests and TDD/continuous testing during development. `ELEC5619-Lab9.pdf` also discusses MockMvc, repository, integration, E2E, performance/security, and UAT testing. **“Coverage often exceeds 95%” is an instructional statement in the slides, not a separate mandatory grading threshold established by the supplied materials; Lab9 emphasizes meaningful assertions.** These teaching examples may use APIs from different Spring versions. **Implementation must follow the effective POM and currently compatible imports for this project's Spring Boot 4.1.1 / Java 21**, without downgrading frameworks or disabling security to copy course examples.

Every business work package other than documentation-only changes follows this cycle:

```text
Confirm Branch Gate = PASS / task FR, API, BC, and DoD
    ↓
List expected assertions for normal, boundary, failure, and security/authorization cases (where applicable)
    ↓
Prefer adding JUnit/Mockito or corresponding frontend unit tests first (initial failures may locate missing behavior)
    ↓
Implement business code and necessary DTOs/migrations/pages
    ↓
Run the smallest relevant tests and fix failures; add higher-level tests when necessary
    ↓
Run applicable builds/static checks and adjacent regression tests; verify actual execution counts
    ↓
Check impact on others' code, permissions, schemas, and historical data
    ↓
Reconfirm develop/yuankang-wei and review the staged diff
    ↓
Make a reviewable local commit combining business code and related tests on that branch
    ↓
Report developer self-test results (eligible for IN_REVIEW) and await independent peer review
```

**Do not deliver only `contextLoads()`, or rely solely on `assertNotNull`, `assertTrue(true)`, HTTP 200, or “no exception” without business assertions.** Derive expected values independently, never using the tested implementation itself. Mock dependencies, not the core method under test. Use clear naming and Arrange–Act–Assert; cover normal/error/boundary cases and observable side effects. Submit a reproducible regression test with each bug fix.

| Change type | Minimum accompanying tests | Subsequent verification they cannot replace |
|---|---|---|
| Java calculation/eligibility/date service | Fast isolated tests using compatible JUnit Jupiter + Mockito | Real database constraints/sessions |
| Controller / Auth | MockMvc or equivalent API tests covering parameters, responses, 401/403/409, and Spring Security | Real Cookie/CSRF behavior across requests |
| JPA / transactions / idempotency / migrations | Uniqueness, rollback, concurrency, and migration tests on isolated PostgreSQL 16 | Real browser flows |
| React pages/drafts/hooks | Frontend TS/JS unit/component/contract tests covering loading, out-of-order responses, cancellation, and refresh failures | Real Spring Boot + PG E2E |
| Images/speech/Q&A | Schema/error/authorization tests with a controllable fake provider | Authorized real-model smoke tests and manual acceptance |
| Documentation-only or minor styling | Explicit N/A rationale and relevant static/build/visual checks | Do not invent a meaningless JUnit test |

**Test partitioning:** run individual classes first for speed, then relevant module regressions. For final `./mvnw test` and `./mvnw verify`, inspect Maven Surefire/Failsafe configuration to verify discovery of `*Test`, `*IT`, or `*IntegrationTest`; zero executed tests is not PASS. Do not copy course `@MockBean`/older Security examples without checking compatibility; use options such as `@MockitoBean` as supported by Boot 4. H2 in the course materials is a teaching technique. This project's PostgreSQL-specific indexes, locks, transactions, and Flyway must be verified on real isolated PostgreSQL.

**Single reviewable commit principle:** normally every local business-change commit includes its most direct automated tests and necessary contracts/documentation. If migration/infrastructure constraints prevent a single commit, create clearly linked preparatory commits, but do not mark the business task `IN_REVIEW` / `DONE` until tests exist and actually pass. Record environmental blockers as `BLOCKED`; never fabricate success, delete assertions, or disable CSRF. Developer self-testing is distinct from Peer Review, UAT, and final course acceptance; record their actual statuses separately.

**Minimum per-task self-review record:** Task ID / FR / BC; Branch=`develop/yuankang-wei`; HEAD or `HEAD + uncommitted changes`; test files/methods; actual commands; executed PASS/FAIL/SKIP counts; isolated database status; reasons for unexecuted checks; actual commit if committed; and `Peer review: PENDING`. Course Sprint materials require traceable tasks, contributions, and acceptance. Actual commits and test evidence support this, but **do not claim the instructor separately mandates a fixed-format JUnit report**.

---

# 1. Input baseline, verification results, and limitations

## 1.1 Materials actually inspected for this plan

| Code | Material | Role |
|---|---|---|
| REPO | `Lab_practical_05_Group_6-main(2).zip` (same SHA-256 as `(1).zip`) | User's last-confirmed local main-project ZIP baseline, containing Java backend and frontend login scaffolding |
| DEMO | `FitNutri_Demo_Source.zip` | Standalone interactive frontend, 24 ZIP entries, with local simulated services and tests |
| REQ | `FitNutri_Project_Proposal_Group06.pdf`, 2026-09-13 | Product scope, FR01–FR23, NFR01–NFR06, and data design |
| COURSE | `ELEC5619-Lab9.pdf`, `L9 Testing (1).pptx`, etc. | Testing-method references (applicable requirements fully incorporated into Sections 0.7 and 19); execution does not depend on other planning Markdown |

```text
REPO SHA-256:
139764140d4db575631a7520e35cf214e8c1acd938d26afadd55c439dd9a295d

DEMO SHA-256:
a7a7449a1a2374a2edd0640db7f7eafe0e498e8de8263e5d6f0c243476524b3c
```

The supplied REPO ZIP contains no `.git`, but the user has specified **`develop/yuankang-wei`** as the actual local development branch. A ZIP cannot establish that the working directory is currently on that branch; verify the actual local Git worktree as required by Section 0.0. **This task uses the user's ZIP as its source-audit baseline and requires neither access to the university GitHub repository nor checking the latest remote branch.** The uploaded REPO `(1)` and `(2)` ZIPs have identical SHA-256 values. Only read-only analysis is allowed before Branch Gate passes; do not overwrite source.

DEMO `npm test` was actually rerun during this planning round: 15 tests passed, 0 failed, on Node 22.16.0 / npm 10.9.2. These are local domain/store tests, not frontend/backend E2E and not a substitute for the project's Node 24 build. This round did not run the Java application, database migrations, frontend production build, complete browser workflows, or real-model calls.

## 1.2 Current responsibilities of the two codebases [SRC]

**DEMO**: React pages → `store.ts` → local `Database` → localStorage; sessionStorage holds simulated login state. `domain.ts` calculates nutrition, estimates, history, and batch submissions; pages access complete arrays through `db.*`.

**REPO**: React login page → Axios Cookie requests → Spring Security session → Spring MVC Controller → Spring Data JPA → PostgreSQL. The backend already supports accounts, profiles, weight, food references, food/exercise records, summaries, and an image-recognition adapter.

**The central gap** is not incompatible UI frameworks: authoritative data, persistence, identity, transactions, and asynchronous state have not yet been connected.

## 1.3 Verified stack configuration; declared versions are not runtime verification

| Layer | Actual configuration |
|---|---|
| Backend | Spring Boot parent **4.1.1**; Java **21**; Maven Wrapper distribution **3.9.16** |
| HTTP | `spring-boot-starter-webmvc`, not WebFlux |
| Security | Spring Security + BCrypt + HttpSession; Basic/formLogin disabled; CSRF currently also disabled |
| Persistence | Spring Data JPA / Hibernate, PostgreSQL JDBC; currently `ddl-auto=update` |
| DB | Docker Compose service `db`, `postgres:16`, existing named volume |
| JSON | POM explicitly includes `com.fasterxml.jackson.core:jackson-databind`; legacy loader/vision code constructs Jackson 2 ObjectMapper instances |
| Frontend | React `^19.2.8`, TS `~6.0.2`, Vite `^8.2.2` |
| UI/routing | MUI `^9.3.1`, Emotion, React Router DOM `^7.18.2`, Recharts `^3.10.1` |
| Requests | Axios `^1.19.0` and React Query `^5.102.4` declared; DEMO business code does not use them |
| Forms | RHF `^7.86.0` and Zod `^4.4.3` declared; DEMO actually uses useState and handwritten validation |
| Node | Main repository `.nvmrc` specifies **24** |

DEMO and REPO dependency/devDependency declarations match individually; scripts, tsconfig, usage, and lockfiles still require independent comparison. Do not overwrite the main project's tsconfig directly: it uses project references, `verbatimModuleSyntax`, `erasableSyntaxOnly`, etc. Adapt Demo imports and types accordingly.

Check Boot 4 test modules, Jackson stack, and Flyway starter against that version. Do not copy package names from Boot 2/3 articles and then downgrade Boot to make examples work. Determine the explicitly declared Jackson 2 dependency's relationship to the MVC mapper through the actual dependency tree and startup tests, not dependency names alone.

---

# 2. Delivery scope: milestones without losing Demo features

## 2.1 M1 — CORE: real core MVP

Required: real registration/login/logout, three-step questionnaire, manual multi-food entry, manual exercise entry, shared review, confirmation/cancellation/failure recovery, server persistence, Daily Overview, and reading saved data after refresh or login again.

To avoid regressing existing Demo behavior, also deliver basic History, Profile, weight/units/goals, editing/deletion, and administrator account-status workflows. Their dependencies are separately identified in task cards; do not present all of them as original Proposal Sprint 2 scope.

M1 must not call simulated speech/image/Q&A services. Hide advanced entry points or clearly mark them unavailable; enable them only after real M2 capabilities are completed.

## 2.2 M2 — FULL: connect every user feature exposed by the Demo to real services

Includes M1 plus real image recognition → reference matching → shared review; real recording → transcription → editable text → food/exercise candidates → shared review; server-side eligibility, consent, and scope controls for general AI Q&A. Rule-based summaries and trends remain nongenerative and auditable.

**FULL means more than having pages.** If external services are unconfigured, return the actual reason for unavailability. Controlled stubs can test service boundaries, but final claims that real image/speech/Q&A functionality passed require corresponding minimal real-service acceptance evidence. Fixed Demo results cannot establish success.

Model-account permissions, keys, and usage budgets are environmental prerequisites, not resources the coding AI may guess or purchase independently. When unavailable, complete implementation and offline tests, record `BLOCKED_EXTERNAL_SERVICE`, and do not mark the whole project DONE.

## 2.3 Out of scope

No new microservices, Node/Express backend, GraphQL, Redis, vector database, persistent chat history, raw audio/image storage, native mobile app, dietary/training prescriptions, email recovery, or social login. Do not replace the existing Java/PostgreSQL architecture with a new system.

## 2.4 Main end-to-end acceptance flow

```text
Register USER → Login → Fetch me/profile → Three-step questionnaire → Empty Dashboard
 → Multiple foods (different meals) → Review/edit/cancel → Confirm → Real database → Dashboard
 → Exercise (date + type + minutes) → Review → Confirm → Dashboard
 → Backdate an entry by 30 days → History locates it automatically → Correct date/portion → Refetch all views
 → Change units/goals/weight in Profile → Existing record snapshots remain unchanged
 → Logout → Login → Data persists; another user cannot see the previous user's data
 → Admin disables/restores account → Login restrictions and original data restored
M2: Images/two speech paths reuse the same Draft/Review/Confirm; Q&A permissions are separate from ordinary recording
```

---

# 3. Startup preflight and execution ledger

## 3.1 Reading order before execution

First read root and relevant subdirectory `AGENTS.md`, `CONTRIBUTING.md`, README, package.json/lockfile, pom.xml, Compose, migration directories, existing security configuration, and existing OpenAPI. Record missing documents as “not found”; absence does not automatically authorize overwriting.

Extract ZIPs safely, rejecting path traversal and symbolic links. Do not overwrite the project with `.git`, node_modules, dist, target, or simulated runtime data. Keep the Demo read-only in `reference/fitnutri-demo/` in the working copy or outside the project; **it is not a production frontend build input**.

Example read-only preflight commands (execute 0.0 first and verify the branch before any installation or write):

```bash
pwd
git rev-parse --is-inside-work-tree
git rev-parse --show-toplevel
git status --short --branch
git branch --show-current
git branch --list
git worktree list
git rev-parse HEAD
git log -5 --oneline
node --version
npm --version
java -version
(cd backend && ./mvnw --version)
```

**Remote Git operations are prohibited for this work**, including fetch/pull/clone. Do not rely on university GitHub services to establish the baseline. After a safe switch, `git branch --show-current` must be exactly `develop/yuankang-wei`; do not continue editing source without local Git metadata. A configured remote URL is not a reason to access it.

## 3.2 Required baseline file

`docs/implementation/baseline.md` must include at least:

- Actual `BASE_BRANCH=develop/yuankang-wei`, BASE_COMMIT, working directory/worktree, Branch Gate, and user uncommitted-change list; no remote lookup is required.
- Both source checksums, actual tool versions, build/test baseline, and failure reasons.
- Individual comparisons for BC-01–BC-16: implemented/partial/missing/alternative in the target branch.
- API, schema, model-provider, and test differences between the user's local `develop/yuankang-wei` branch and the ZIP audit baseline; do not claim it is the latest remote version.
- Blockers involving dependencies, official reference data, course requirements, shared-database permissions, etc.
- Confirmed scope: M1/M2 execution order and gates requiring manual approval.

Do not inspect Java code alone: inspect the actual database schema or generate and compare it from this version on temporary PostgreSQL. Source annotations do not guarantee that an existing database matches them exactly.

## 3.3 Task status protocol

Create `docs/implementation/tasks.md`:

```text
ID | Status | Branch | Commit SHA | BC/migration | Paired test file/method | Actual PASS/FAIL/SKIP | Developer self-review | Peer Review | Unresolved issues | Next task
```

Verify `develop/yuankang-wei` when beginning and committing every code change. Do not conflate Developer tests and Peer Review.

Use only `TODO / IN_PROGRESS / BLOCKED / IN_REVIEW / DONE`. Advance one primary task at a time; record actual results before taking a task whose dependencies are satisfied. `DONE` requires commit, test, and acceptance evidence, not merely existing files.

Every handover includes changed files, contract changes, DB impact, test results, unverified scenarios, and the next safe step. Subsequent AI agents must not guess fields from conversation summaries.

## 3.4 Hard gates for external data and environment

If official AUSNUT files or specific 2024 activity-reference data files are unavailable, implement importers and synthetic fixture tests first. Acquiring/verifying official sources requires lawful access and provenance records. Do not rename Demo values or unverified repository AFCD data as AUSNUT.

If the latest branch is inaccessible, report review/contract differences but do not claim integration into the “latest branch”. If the database contains unknown production data or migration ambiguities, stop at preflight; do not choose to delete data on the user's behalf.

---

# 4. Target architecture and key design decisions

## 4.1 What to retain and replace

```text
Demo JSX / MUI theme / CSS / page flows
                ↓ Retain and split at component boundaries
React pages + DraftProvider + page state
                ↓
Query hooks / Mutation hooks
                ↓
api/*.ts (Axios; Cookie; CSRF; DTO validation; AbortSignal)
                ↓ HTTP /api
Spring MVC Controller (identity and input)
                ↓
Service (authorization, calculations, transactions, idempotency, versions, consistency)
                ↓
Spring Data JPA / limited parameterized SQL
                ↓
PostgreSQL 16 (constraints, record snapshots, versions, submission receipts)
```

Allocation of business authority:

| Responsibility | Layer |
|---|---|
| Unsubmitted text, selected references, confirmation checkboxes, dialogs | Frontend Draft / form state |
| Server data loading, caching, and query invalidation | React Query |
| User identity, roles, ownership, Q&A permission | Java server |
| Nutritional values, activity estimates, final saved calculation snapshots | Java server |
| Uniqueness, concurrency conflicts, atomic batch saves | Java transactions + PG constraints |
| Unit display and number formatting | Frontend; no recalculation of nutrition |
| Range summaries, missing-value policy, rule-based summaries | One Java SummaryService |
| Remote speech/image/Q&A model calls | Backend provider adapter; no frontend keys |

## 4.2 ADR-01: reuse the Demo without copying its simulated database

Retain theme definitions in `main.tsx`, `styles.css`, display components from `ui.tsx`, and valid interactions in account/entry/overview/extras. Retain the separate `/review` page: it is not a defect. Do not move Review back into entry pages merely to match an older plan.

Production builds must not import Demo `store.ts`, `seed.ts`, `Database`, local password-hash functions, or DemoTools. Legacy domain/store tests may run separately in `tests/legacy-demo/` as regression references; they must not enter production bundles through barrel imports.

**Do not expose `db: Database`.** Production user DTOs contain neither passwordHash nor lists of other users; administrator DTOs also exclude private profiles and records.

## 4.3 ADR-02: standardize DTOs, preserve necessary compatibility, avoid duplicate authority

Target HTTP contracts use camelCase; DB columns retain snake_case. New frontend domain objects also use camelCase. Mechanically replace Demo fields in one migration step to avoid maintaining a second complete snake_case database.

- Return all database IDs in new contracts as decimal strings, such as `"143"`; Java still uses Long. Frontend code treats IDs as opaque identifiers without arithmetic.
- `version` is a nonnegative integer; define units, dates, and enums in one contract file.
- Nutrition and portion JSON values must be finite numbers; precise calculations occur only on the BigDecimal server. Keep input as strings until submission validation; do not let `Number("")` become zero.
- Explicitly use `null` for required nullable fields, not `undefined` to clear values. Distinguish missing fields from explicit clearing.
- Responses may add fields; dangerous legacy write contracts must be disabled or delegate to the same safe Service. Compatibility must not retain an alternative path for forged nutrition values.
- This plan's API-level upgrade identifier is `mvp-1`, exposed through capabilities. Changing legacy contracts is a coordinated migration, not transparently compatible with every old client.

## 4.4 ADR-03: nine business tables plus one justified technical receipt table

The Proposal's nine business tables remain the target: accounts, profiles, weight, food datasets, food references, food records, activity datasets, exercise types, and exercise records.

**This plan additionally recommends the `submission_receipt` technical table [DESIGN], making the default target 10 application tables plus Flyway's own schema history.** This is a design addition to the Proposal and must be documented as such; do not claim the original Proposal included a tenth table.

Rationale: identifying multi-food batches, concurrent duplicates, confirmations after lost responses, and original submissions whose records were later edited/deleted is ambiguous when based only on current record contents. The receipt retains submission identity, a normalized request digest, and minimal response data so retries do not recreate records.

Scope control: receipts serve food/exercise creation only and contain no chats, questionnaire answers, or full database copies. Do not introduce Redis/message queues. Reuse an equivalent existing mechanism if it passes all idempotency tests and document the ADR-03 alternative rather than forcing another similar table.

## 4.5 ADR-04: upgrade APIs incrementally through one canonical save service

Retain existing paths and package structure; add business actions only for clear needs. Controllers must not duplicate calculation/confirmation logic. The original single-food POST may wrap one item in a batch and call the same Service, but must supply canonical fields such as referenceId, submission identity, and recordDate.

Legacy client requests supplying only foodName/calories must return `400 CONTRACT_UPGRADE_REQUIRED` when production functionality is enabled, rather than guessing a reference. Legacy read-only energy fields may temporarily remain deprecated aliases derived from kJ; they must no longer be authoritative for new data.


## 4.6 ADR-05: explicit states and failures instead of false success

Send each write request as one explicit action. For explicit rejection, show field issues; for an uncertain write outcome, enter `unknown`; for a successful write followed by a failed read, enter `saved-refresh-failed`. Do not collapse these states into one “Save failed” button.

Do not optimistically accumulate nutrition in the frontend; refetch confirmed data after receiving the server receipt. Keep caches in memory only and clear them on logout/session expiry. Drafts may persist within one page session for the same user; cross-device drafts are not promised.

---

# 5. Target file organization and per-file migration roadmap

These paths are defaults. If the latest branch already has a suitable structure, retain the team's structure and record equivalent paths in the baseline.

## 5.1 Target frontend directory

```text
frontend/
  src/
    main.tsx
    App.tsx
    app/
      providers.tsx
      theme.ts
      router.tsx
      queryClient.ts
      queryKeys.ts
      capabilities.ts
      SessionProvider.tsx
      DraftProvider.tsx
      sessionEpoch.ts
    api/
      client.ts
      csrf.ts
      errors.ts
      contracts.ts
      validators.ts
      auth.ts
      profile.ts
      weight.ts
      foodReference.ts
      exerciseTypes.ts
      records.ts
      submissions.ts
      summary.ts
      admin.ts
      recognition.ts
      speech.ts
      qa.ts
    hooks/
      useSession.ts
      useProfile.ts
      useFoodSearch.ts
      useFoodPreview.ts
      useExerciseEstimate.ts
      useRecordQueries.ts
      useSubmitDraft.ts
      useRecordMutations.ts
      useSummary.ts
      useUnsavedChanges.ts
    layouts/
      AppLayout.tsx
      Header.tsx
      ProtectedRoute.tsx
    pages/
      AuthPage.tsx
      OnboardingPage.tsx
      DashboardPage.tsx
      HistoryPage.tsx
      ProfilePage.tsx
      AdminPage.tsx
      ChatPage.tsx
    features/entry/
      AddRecordPage.tsx
      EntryPage.tsx
      ReviewPage.tsx
      FoodEditor.tsx
      FoodItemEditor.tsx
      FoodReferencePicker.tsx
      ExerciseEditor.tsx
      ReviewActions.tsx
      SubmissionRecovery.tsx
      draftTypes.ts
      draftReducer.ts
      draftValidation.ts
      submissionPayload.ts
      media/Recorder.ts
      media/VoiceEntryPanel.tsx
      media/ImageEntryPanel.tsx
    components/
      Brand.tsx
      Page.tsx
      ErrorBox.tsx
      AsyncPanel.tsx
      LoadingButton.tsx
      ConfirmDialog.tsx
      DateField.tsx
      SelectField.tsx
      EnergyValue.tsx
      SourceNote.tsx
      RecordList.tsx
      RecordDetailsDialog.tsx
      NutrientTiles.tsx
      RuleSummary.tsx
      RecordCharts.tsx
      UserNoticeProvider.tsx
    domain/
      types.ts
      labels.ts
      dates.ts
      units.ts
      inputRules.ts
    styles.css
  tests/
    unit/
    component/
    contract/
    e2e/
    legacy-demo/
  vite.config.ts
  playwright.config.ts
```

Do not introduce circular dependencies during migration: `api` does not import pages; `domain` does not import React/DB; `components` receive display data through props; query hooks do not own the entire Draft; DraftProvider does not own the entire server database.

## 5.2 Demo → target file mapping

| Demo source/symbol | Target | Retention and modification guidance |
|---|---|---|
| `main.tsx` `createTheme` | `app/theme.ts` | Move visual tokens unchanged; integrate with the existing QueryClientProvider without nesting two ThemeProviders |
| `styles.css` | `src/styles.css` | Retain main styles; check conflicts with repository index.css/App.css and remove Vite sample constraints |
| `App.tsx::Header` | `layouts/Header.tsx` | Retain layout; use session/profile queries; await server logout |
| `App.tsx::Protected` | `layouts/ProtectedRoute.tsx` | Add loading/error/expired states; do not classify request errors as incomplete questionnaires |
| `App.tsx::App` | `App.tsx + app/router.tsx` | Remove api.ready, whole-database subscriptions, demoEmail, and DemoTools; retain routing and notifications |
| `context.tsx::AppContextValue` | Three focused Session/Draft/Notice Providers | Remove Database; separate current user, drafts, and notifications |
| `account.tsx::Login` | `pages/AuthPage.tsx` | Retain login/register switching; remove automatic demo-account filling; connect auth hooks |
| `account.tsx::ProfilePage` | Onboarding/Profile + reusable ProfileForm | Reuse form presentation and initialize once; refetch must not overwrite an in-progress edit |
| `entry.tsx::AddRecord` | `features/entry/AddRecordPage.tsx` | Retain type selection and draft-overwrite confirmation; enable advanced methods by capability |
| `entry.tsx::FoodEditor` | FoodEditor/FoodItemEditor/Picker | Replace db.foods with remote search and selected-reference details; use independent queries per item |
| `entry.tsx::ExerciseEditor` | ExerciseEditor | Read real catalogue and estimates; remove local MET/weight lookups |
| `entry.tsx::Entry` | EntryPage + Voice/Image panels | Connect manual entry first; separate advanced-processing hooks; produce the same Draft |
| `entry.tsx::Review` | ReviewPage/useSubmitDraft | Retain UI; freeze submission snapshots; distinguish create/edit/replay/unknown |
| `overview.tsx::SourceNote` | SourceNote | Use real dataset data; do not hardcode Demo/AUSNUT names |
| `overview.tsx::Records` | RecordList/DetailsDialog | Backend responses include names/reference details; eliminate whole-table joins through db.foods.find |
| `overview.tsx::NutrientTiles` | NutrientTiles/EnergyValue | Presentation components; handle null/partial/0 separately |
| `overview.tsx::Summary` | RuleSummary | Read SummaryService facts; do not redefine averaging policy in the frontend |
| `overview.tsx::Dashboard` | DashboardPage | Read today's summary and both record lists; retain existing layout |
| `overview.tsx::History` | HistoryPage/RecordCharts | URL-driven ranges; locate entries automatically after saving; chart server daily series |
| `extras.tsx::Admin` | AdminPage | Backend authorization, minimal account DTOs, versions, and operation confirmation |
| `extras.tsx::Chat` | ChatPage | Retain conversation UI; check eligibility on the backend for every request; do not call local answerQuestion |
| `extras.tsx::DemoTools` | Tests/read-only reference, excluded from production builds | Do not migrate data-reset, simulated identity-switching, or simulated-failure controls |
| `domain.ts` types and label/date/unit utilities | `domain/*` | Remove password_hash/Database; extract pure display/input rules |
| `domain.ts::commitDraft/estimateExercise/aggregate` | Java Service + regression tests | Not authoritative in the production frontend; cases may become Java tests |
| All business write functions in `store.ts` | `api/* + hooks/*` | Replace rather than simulate HTTP; production import graphs must contain no reference |
| `seed.ts` | `tests/fixtures` or read-only reference | Do not migrate simulated users, passwords, or official-data placeholders into real runtime |
| `tests/domain.test.ts/service.test.ts` | `tests/legacy-demo` + new Java/API tests | Preserve original case semantics; distinguish simulated-service tests from real-service tests |

Inventory Brand/Page/ErrorBox/LoadingButton/ConfirmDialog/DateField/SelectField/focusError/messageOf in `ui.tsx`. Reuse the first three presentation components directly; delegate `messageOf` to the new API error parser; pass the shared today value into DateField; add pending and keyboard-focus constraints to ConfirmDialog.

## 5.3 Target backend directory additions

```text
backend/src/main/java/com/fitnutri/backend/
  common/
    ApiError.java                 # new
    FieldViolation.java           # new
    BusinessException.java        # new
    ApiExceptionHandler.java      # modified
    ClockConfig.java               # new
    RecordDatePolicy.java          # new
    CurrentUserService.java        # new
    ZoneResolver.java              # reuse/test
  security/
    SecurityConfig.java            # modified
    AppUserDetailsService.java     # modified
    CsrfController.java            # new: CSRF bootstrap
    SessionConfiguration.java      # new or integrated into SecurityConfig
    JsonAuthenticationEntryPoint.java # new
    JsonAccessDeniedHandler.java   # new
  user/
    AuthController.java            # modified
    AuthService.java               # new
    AdminBootstrapRunner.java      # new, explicit environment toggle
    User.java/UserRepository.java/UserResponse.java # modified
  profile/
    ProfileService.java            # new
    WeightService.java             # new
    CompleteOnboardingRequest.java # new
    ProfileSettingsRequest.java    # new, replaces ambiguous legacy update request
    [existing Controller/DTO/entity/repository] # modified
  foodreference/
    FoodDataset.java/FoodDatasetRepository.java # new
    FoodReferenceService.java/FoodCalculationService.java # new
    ReferenceImportService.java/ReferenceManifest.java # new
    [FoodReference/Repository/Loader/Controller/Response] # modified
  activity/
    ActivityDataset.java/ExerciseType.java # new
    ActivityDatasetRepository.java/ExerciseTypeRepository.java # new
    ActivityReferenceImportService.java # new
    ExerciseReferenceService.java # new
  records/
    RecordService.java/FoodRecordService.java/ExerciseRecordService.java # new
    ExerciseEstimateService.java   # new
    FoodBatchRequest.java/FoodBatchItemRequest.java # new
    UpdateFoodRecordRequest.java/UpdateExerciseRecordRequest.java # new
    [existing entity/repository/controller/response] # modified
  submission/
    SubmissionReceipt.java/SubmissionReceiptRepository.java # new
    SubmissionService.java/SubmissionController.java # new
    CanonicalSubmissionHasher.java/SubmissionReceiptResponse.java # new
  summary/
    SummaryService.java/RuleSummaryService.java # new
    SummaryDtos.java or separate DTO files # new
    [TodaySummaryController/DataSummaryController] # convert to thin entry points
  admin/
    AdminService.java/ChangeAccountStatusRequest.java # new
    [AdminController/AdminUserSummary] # modified
  capabilities/
    CapabilitiesController.java/CapabilitiesService.java/CapabilitiesResponse.java # new
  food/                         # retain original image package
    [FoodRecognitionController/OpenAiVisionService/DTO] # modified
  speech/                       # new in M2
    SpeechController.java/TranscriptionService.java/EntryExtractionService.java
  qa/                           # new in M2
    QaController.java/QaService.java/QaScopePolicy.java
    QaEligibilityService.java/QaContextBuilder.java/PermittedRecordContext.java
  ai/                           # shared by M2
    AiProviderConfig.java/AiTransport.java/ProviderResponseValidator.java
```

This is a responsibility inventory, not a request for empty files. Every new Service must have a real, independent rule/transaction responsibility. Use constructor injection consistently; invoke transactional methods from another Spring Bean so self-invocation does not bypass the proxy.

---

# 6. Shared frontend types, page states, and domain invariants

## 6.1 Core type outline [DESIGN]

```ts
export type Id = string; // Decimal database ID; do not parseInt for arithmetic
export type LocalDateString = string; // Must pass date validation; format YYYY-MM-DD
export type EnergyUnit = 'KJ' | 'KCAL';
export type EntrySource = 'MANUAL' | 'VOICE' | 'IMAGE';
export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';
export type SafetyAnswer = 'NO' | 'YES_OR_UNSURE' | 'PREFER_NOT_TO_ANSWER';
export type EstimateStatus =
  | 'AVAILABLE' | 'MISSING_WEIGHT' | 'NO_APPLICABLE_REFERENCE'
  | 'LEGACY_UNVERIFIED';

export interface CurrentUser {
  id: Id; email: string; displayName: string;
  role: 'USER' | 'ADMIN'; enabled: boolean; version: number;
}
export interface Nutrients {
  energyKj: number | null;
  proteinGrams: number | null;
  fatGrams: number | null;
  carbsGrams: number | null;
}
export interface ReferenceInfo {
  referenceId: Id; publicFoodKey: string; displayName: string;
  dataset: { id: Id; name: string; release: string; revision: string;
             sourceUrl: string | null; nutrientPolicy: string; verified: boolean };
  active: boolean;
}
export interface DraftFood {
  rowKey: string;               // React row identity, not server idempotency
  clientEntryId: string;        // Record-level UUID
  referenceId: Id | null;
  selectedReference: ReferenceInfo | null; // Selected-item display, not the entire catalogue
  quantityGramsInput: string;
  mealType: MealType | '';
  referenceConfirmed: boolean;
  gramsConfirmed: boolean;
}
export interface DraftBase {
  ownerId: Id;
  recordDate: LocalDateString;
  source: EntrySource;
  revision: number;             // Frontend draft revision, not DB version
  returnTo: string;             // Allowlisted internal paths only
}
export type Draft =
  | (DraftBase & { kind: 'food'; foods: DraftFood[]; edit?: EditTarget })
  | (DraftBase & { kind: 'exercise'; clientEntryId: string;
      exerciseTypeId: Id | null; durationMinutesInput: string; edit?: EditTarget });
export interface EditTarget {
  recordId: Id; expectedVersion: number; originalRecordDate: LocalDateString;
}
export type SubmissionState =
  | { status: 'idle' }
  | { status: 'saving'; submissionId: string; frozenPayload: unknown; ownerEpoch: number }
  | { status: 'unknown'; submissionId: string; frozenPayload: unknown; ownerEpoch: number }
  | { status: 'saved'; receipt: SubmissionReceipt; refreshFailed: boolean }
  | { status: 'rejected'; error: ApiError };
```

The `unknown` above only illustrates the state-machine boundary. In implementation, frozenPayload must be the discriminated union `CreateFoodBatchPayload | CreateExercisePayload`; do not retain `unknown` to evade typing. P02 OpenAPI and `api/contracts.ts` fix the complete DTOs.

## 6.2 Draft state machine and commands

`draftReducer.ts` must support at least `START_NEW`, `ADD_FOOD`, `REMOVE_FOOD`, `SELECT_REFERENCE`, `CHANGE_GRAMS`, `CHANGE_MEAL`, `CHANGE_DATE`, `SET_EXERCISE_TYPE`, `SET_DURATION`, `CONFIRM_REFERENCE`, `CONFIRM_GRAMS`, `LOAD_EDIT`, `BEGIN_SUBMIT`, `SUBMIT_REJECTED`, `SUBMIT_UNKNOWN`, `SUBMIT_CONFIRMED`, and `DISCARD`.

Maintain these invariants:

- Never reuse a draft across users; clear it immediately and notify the user on ownerId mismatch.
- Increment revision on ordinary edits; changing references clears referenceConfirmed; changing grams clears gramsConfirmed; recognition results are unconfirmed by default.
- Row identity, record idempotency ID, and batch submissionId are three different identities; array indexes cannot replace them all.
- Entering Review creates no database records; changing Review contents also does not write automatically.
- On Save, synchronously set a ref/reducer lock and freeze submission contents to prevent double-click reentry before React updates the button.
- Freeze all payload-changing controls during `saving` and `unknown`. Do not change contents and resend under a submissionId whose outcome is unknown.
- Editing is allowed after a 4xx validation error that explicitly confirms no transaction was committed; the next confirmation creates a new submissionId. Item clientEntryIds may remain because nothing was written; any existing ID requires explicit backend conflict handling.
- For unknown outcomes, retrieve the receipt or retry the original payload with the original submissionId before allowing a new identical record.
- After a successful save, “record another meal” creates a new submissionId and new item clientEntryIds, even for identical contents.

## 6.3 Frontend input conventions

- Dates: real calendar dates, never future dates; derive today in the application's resolved IANA timezone. Do not substitute `new Date().toISOString().slice(0,10)` for the user's local today.
- Food: `0 < grams <= 10000`, at most three decimal places, 1–50 items per batch. These are engineering safeguards, not health advice; if course limits differ, update contracts/tests first.
- Exercise: `0 < minutes <= 1440`, at most two decimal places. This plan supports the fractional minutes already allowed by the Demo; migrate backend Integer to BigDecimal.
- Height/weight: initially retain the backend's existing 50–260 cm / 20–400 kg input ranges, synchronized across frontend/backend. They are engineering ranges, not medical thresholds. Record a change before widening them if the Proposal requires it.
- Optional energy goal: empty string → null; otherwise positive and compatible with numeric(12,3). Do not derive it automatically or retain the unexplained original 800–6000 kcal restriction.
- Birth year: use explicit rules for record/current year; initial setup requires explicit adult confirmation. Year alone cannot establish an exact birthday; the UI must not claim exact age or clinical assessment.
- The server revalidates every input; disabling a button is not a security boundary.

## 6.4 Date and unit policy

Canonical storage: `recorded_date date`, `energy_kj numeric`, and `created_at/updated_at timestamptz`. Use `zone` only to determine today, reject future dates, and interpret legacy data. After creation, changing query timezone must not change a record's business date.

kcal = kJ / 4.184. Round calculation/storage only once to three decimal places. Display may round to integers as in the Demo, while tooltips/details expose precision. Frontend approximations from the four macronutrient values must not replace reference-dataset energy.

When migrating old kcal fields, convert units only; do not guess new AUSNUT matches from names and overwrite old snapshots. Preserve nulls and flags for missing legacy values; never fabricate zeros.

---

# 7. Backend change register: why each change is necessary

**Backend commits by the coding AI must cite the BC IDs below. Register any additional backend/database changes, their impact, and tests before implementation.** Do not hide unexplained server changes inside frontend PRs.

| ID | Current source fact | Reason | Files/data involved | Classification |
|---|---|---|---|---|
| BC-01 | First public registrant becomes ADMIN; email lookup uses the original string; custom login manually calls saveContext; CSRF disabled | FR08/NFR01, real Cookie sessions, logout, and request-forgery protection | AuthController, AuthService, User/UserRepository, AppUserDetailsService, SecurityConfig, CsrfController; email index/version | Required fix; CSRF is a security enhancement design |
| BC-02 | Profile PUT ignores null; no explicit adult confirmation/units; separate eligibility field; profile saves append weight | Persist Demo three-step questionnaire, goal clearing, units, name, and completion state consistently | ProfileController/Service/DTO, UserProfile, User; new onboarding operation | Required fix |
| BC-03 | Same-day weight always appends; no version | FR06, historical-date weight dependency, frontend conflict recovery | WeightController/Service/Repository/Entry; date uniqueness and version | Required fix |
| BC-04 | FoodReference PK is String publicFoodKey; AFCD-labelled data without source version | FR15 AUSNUT traceability, immutable reference versions, food FK | FoodReference, repository generics, Loader/Service/Controller; food_dataset, new id/constraints | Required fix; no guessed migration matches |
| BC-05 | ExerciseCatalog hardcodes a Map; no source/applicable-age entity | FR11 real explainable estimates; Demo MET values must not carry over | activity_dataset, exercise_type, importer; adapt legacy Catalogue | Required fix |
| BC-06 | BaseRecord uses Instant recordedAt; food/calorie uses Double; missing canonical date, kJ, version, deletedAt, client ID | Dates/units/snapshots/corrections/duplicate prevention | BaseRecord, FoodRecord, ExerciseRecord, read/write DTOs/queries; new columns and type migration | Required fix |
| BC-07 | No batch receipt or equivalent concurrent-retry mechanism | Reliable recovery after lost responses, multi-record batch identity, no resurrection when retrying deleted records | submission package and submission_receipt | New design; additional technical table requires ADR |
| BC-08 | FoodRecordController writes client-supplied nutrition directly; single-item POST | FR10/FR15/FR17; connecting pages must not preserve trust in incorrect data | FoodCalculationService, FoodRecordService, batch DTO/entry point | Required fix |
| BC-09 | Exercise uses current weight, falls back to 70 kg; Integer duration | FR11 historical dates, unavailable is not zero; Demo fractional minutes | ExerciseEstimateService, RecordService, snapshot fields, BigDecimal | Required fix; fractional precision standardized by this plan |
| BC-10 | Record lists load everything then filter; PUT reuses create DTO; physical deletion, no concurrency version | Real History editing, date location, authorization/version/retry, rereading legacy records | Both record repositories/controllers; detail GET, separate update DTO | Required fixes/necessary convenience endpoints |
| BC-11 | today/trends and daily/weekly disagree on averages/missing values; automatic calorie goal | FR18–22; Demo must not display contradictory numbers from different algorithms | SummaryService, RuleSummaryService, both summary controllers; stop automatic goals in production views | Required fix |
| BC-12 | Admin list/disable/restore lacks versions | Demo admin confirmation and stale-update feedback, FR09 | AdminService, status request, User.version | Supporting workflow fix |
| BC-13 | Errors mostly message-only; no field locations or feature-readiness state | Distinguish frontend field errors, unknown saves, unavailable external services | ApiError/handler/CurrentUser/Clock/capabilities | Shared engineering enhancement |
| BC-14 | ddl-auto=update; no formal migrations found | Existing DB changes must be reproducible, reviewable, and preserve old data | POM, application profiles, Flyway migrations, DB scripts, Compose | Required migration infrastructure |
| BC-15 | Image adapter returns only name/grams/confidence; hardcoded model | M2 real image candidates → reference review; errors/file validation/configurable model | food package, AiTransport, candidate-matching DTO | M2 |
| BC-16 | No speech API or real QA API | M2 two recording paths and restricted Q&A; cannot retain local simulation | speech/qa/ai packages and configuration; no audio/chat-storage tables | M2 |

**Table count:** current six entities → add three missing Proposal business tables → add this plan's one technical receipt table. Old fields may remain temporarily to preserve data; this does not establish permanent duplicate authority. Contract migrations removing columns require separate review and must not be performed incidentally during frontend integration.

---

# 8. Target API contracts (mvp-1)

## 8.1 General conventions

All paths retain `/api`. Production browsers use same-origin `/api`; Vite proxies development requests. Requests carry the existing Cookie session, with a CSRF header on writes. Derive identity from Authentication; clients must not specify the target user through ownerId/userId.

Date input uses `recordDate: YYYY-MM-DD`; query `from/to` are inclusive. Without zone, use server-configured Australia/Sydney; the frontend normally supplies the selected application timezone explicitly. No valid zone may change a saved recordDate.

Standard error example:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Check the highlighted fields.",
  "path": "/api/records/food/batch",
  "requestId": "server-generated-correlation-id",
  "fieldErrors": [
    {"field": "items[1].quantityGrams", "clientEntryId": "item-uuid", "message": "Enter positive grams."}
  ],
  "retryable": false
}
```

Errors must not expose raw user input, SQL, stacks, provider responses, or raw safety answers. Map `fieldErrors` through clientEntryId so removing a row cannot display the second item's error on the third item.

| HTTP | Example code | Frontend action |
|---|---|---|
| 400 | VALIDATION_ERROR / CONTRACT_UPGRADE_REQUIRED | Do not retry writes; retain draft, locate fields/show upgrade notice |
| 401 | UNAUTHENTICATED | Use shared session-expiry handling; avoid repeated notices outside login forms |
| 403 | FORBIDDEN / QA_RESTRICTED | Explain permissions; do not treat every 403 as expiry |
| 403 | CSRF_INVALID | Refresh token; retry once only if the write definitely never entered the application; otherwise require user confirmation |
| 404 | NOT_FOUND | Record no longer visible; do not disclose whether another user's record exists |
| 409 | VERSION_CONFLICT | Retain local edits, fetch latest, require another review |
| 409 | IDEMPOTENCY_CONFLICT | Do not automatically change ID; show conflict and inspect original submission |
| 409 | REFERENCE_INACTIVE / CALCULATION_CHANGED | Do not save; reselect reference or review new estimate |
| 422 | CATALOGUE_UNAVAILABLE / UNSUPPORTED_REFERENCE | Explain data-readiness issue; no Demo fallback |
| 429 | RATE_LIMITED | Respect Retry-After; no continuous automatic model retries |
| 502/504 | PROVIDER_BAD_RESPONSE / PROVIDER_TIMEOUT | Create no record; retain draft and offer manual entry |
| 503 | FEATURE_NOT_CONFIGURED | Feature genuinely unavailable; do not return false success |
| 500 | INTERNAL_ERROR | No sensitive details; receipt recovery determines write-outcome certainty |

Java handles Bean Validation 400 and business 409/422 consistently. Spring Security filter 401/403 responses do not automatically pass through ControllerAdvice; use JSON EntryPoint/AccessDeniedHandler. Axios must still tolerate empty/non-JSON bodies.

## 8.2 API inventory: retained, extended, and new

| Operation | Target path | Relationship to current source |
|---|---|---|
| Register/login/logout/me | POST `/auth/register`、`/auth/login`、`/auth/logout`；GET `/auth/me` | Retained; fix security and DTOs |
| CSRF | GET `/auth/csrf` | New small token-bootstrap endpoint |
| Capabilities | GET `/capabilities` | New; returns schemaVersion and actual configuration state, not an acceptance certificate |
| Current profile | GET `/profile` | Retained; add canonical fields, version, currentWeight |
| Initial setup | POST `/profile/onboarding` | New; atomically complete profile and initial weight |
| Update profile | PUT `/profile` | Retain path; mvp-1 complete-settings snapshot semantics, unambiguous null |
| Clear goal | DELETE `/profile/goal`、`/profile/energy-goal` | Retain compatibility; delegate to same version-checking Service |
| Weight | GET/POST `/profile/weight` | Retained; POST creates/corrects by date + version |
| Food search | GET `/food-reference/search?query=&page=&size=` | Retained; returns referenceId + source + per100g |
| Reference by immutable ID | GET `/food-reference/by-id/{id}` | New; avoids public-key ambiguity across versions |
| Portion preview by ID | GET `/food-reference/by-id/{id}/nutrition?grams=` | New; legacy key endpoint supports active references only |
| Exercise catalogue | GET `/records/exercise/types` | Retained; upgrade to structured type list |
| Exercise preview | GET `/records/exercise/estimate?exerciseTypeId=&durationMinutes=&recordDate=&zone=` | Retained; add inputs and status/snapshots |
| Create food batch | POST `/records/food/batch` | New core write endpoint |
| Create single food | POST `/records/food` | Compatible one-item batch; prohibit dangerous legacy writes |
| Create exercise | POST `/records/exercise` | Retained; add idempotency and canonical date |
| Submission receipt | GET `/submissions/{submissionId}` | New; recover unknown outcomes |
| Record lists | GET `/records/food`、`/records/exercise` | Retained; filter by owner + recordDate + nondeleted |
| Record details | GET `/records/food/{id}`、`/records/exercise/{id}` | New; for conflicts, date moves, and location after receipt |
| Correct/delete | PUT/DELETE both types `/records/.../{id}` | Retained; versions, soft deletion, server recalculation |
| Summaries | GET `/summary/today`、`/daily`、`/weekly`、`/trends` | Retained; unified Service and calculation policy |
| Administrator accounts | GET `/admin/users`；PATCH `/{id}/deactivate`、`/{id}/reactivate` | Retained; add version/query support; preserve private-data boundaries |
| Image | POST `/food/recognize` multipart `image` | Retained and enhanced; M2 only |
| Speech transcription | POST `/speech/transcribe` multipart `audio` | New in M2; shared by both speech paths |
| Structure transcript | POST `/speech/parse` JSON | New in M2; reparse editable text |
| General Q&A | POST `/qa/messages` | New in M2; restricted single-turn Q&A, no chat-history DB |

Compared with the older plan's emphasis on adding batch routes only, this plan additionally requires real unknown-outcome recovery, atomic complete-questionnaire saving, reference-version lookup, security bootstrap, and all advanced Demo flows. This justifies a few focused endpoints, not a separate backend for every page.

## 8.3 Accounts and CSRF

Registration retains `email/password/displayName`; the backend ignores/rejects role input and fixes the role to USER. Normalize email with trim + Locale.ROOT lowercase and enforce a database unique index. Synchronize valid password-length rules across frontend/backend; verify BCrypt's input limit against the actual library, without silent truncation. Never send registration passwords to analytics or logs.

Successful registration returns 201 and CurrentUser without passwordHash, but creates no login session. After successful login (200), restore CSRF first, then fetch me/profile; do not rely on Demo's synchronous currentProfile().

`GET /auth/csrf` returns:

```json
{"headerName":"X-CSRF-TOKEN","token":"framework-issued-token"}
```

Keep the token in memory only; it is not session identity. Return `Cache-Control: no-store`. Obtain a token before anonymous login/registration and fetch another after login/logout. Use existing Spring mechanisms, not a custom random-string security protocol. See Section 11.

## 8.4 Profile contract and consent semantics

GET `/profile` returns:

```text
profileVersion: number | null             # null when no profile exists; do not invent version 0
user: CurrentUser                        # Permitted fields for current user
birthYear: number | null
adultConfirmed: boolean
sex: MALE | FEMALE | OTHER | PREFER_NOT_TO_SAY | null
heightCm: number | null
activityLevel: SEDENTARY | LIGHT | MODERATE | HIGH | null
goalType: LOSE_FAT | GAIN_MUSCLE | MAINTAIN | NOT_SURE | null
dailyTargetKj: number | null
energyUnit: KJ | KCAL
healthSafetyAnswer: NO | YES_OR_UNSURE | PREFER_NOT_TO_ANSWER | null
qaConsentGranted: boolean
qaEligibility: {eligible:boolean, reason:string}
onboardingCompleted: boolean
currentWeight: {id, recordDate, weightKg, version} | null
healthSafetyUpdatedAt / qaConsentUpdatedAt / updatedAt: timestamp | null
```

Compute `qaEligibility` from current answers/consent; never accept client overrides or rely on the old `aiQaEligible` column. Migration merges legacy `YES` and `NOT_SURE` into `YES_OR_UNSURE`, and maps `PREFER_NOT_TO_SAY` to `PREFER_NOT_TO_ANSWER`. This merges semantics; it does not guess unknown choices as NO.

POST `/profile/onboarding`: complete profile settings + `expectedUserVersion` + `expectedProfileVersion` (null if absent) + `initialWeight:{recordDate,weightKg}` + `zone`. Explicit adultConfirmed=true is mandatory; Q&A consent=false is valid. In one transaction, lock the current user, check versions, create/update profile, write initial weight, update display name, and finally mark complete; any failure rolls back everything.

PUT `/profile`: complete editable-settings snapshot, including expectedUserVersion/expectedProfileVersion. `goalType:null`/`dailyTargetKj:null` genuinely clear values; omitted fields are contract errors rather than retaining ambiguous partial-update behavior. Ordinary settings PUT must not implicitly append weight; use a separate weight form. Retain the broad Demo UI but separate profile saving from adding/correcting weight, each with clear feedback.

Retain an equivalent atomic patch implementation with field-presence handling if already available in the latest branch. Prove that omission differs from clearing with null and that multi-field name/profile updates cannot partially succeed.

Repeated POST after setup completion must not overwrite blindly: return 409 `ONBOARDING_ALREADY_COMPLETED`; frontend GETs current settings for comparison. Recover unknown network outcomes read-only. Do not duplicate raw safety answers in submission_receipt.

## 8.5 Precise weight POST rules

```json
{"recordDate":"2026-10-01","weightKg":70,"expectedVersion":null,"zone":"Australia/Sydney"}
```

Use expectedVersion=null for a new date. If a record already exists on that date with the same value and no update intent, returning it with 200 is allowed; a different value requires 409 with the current item for user confirmation. Explicit corrections include the existing version; validate it, update that row, and increment version.

Locking the user row can serialize concurrent creation for a date not yet present for that user; database uniqueness on `(user_id,entry_date)` is the final safeguard. Compute currentWeight from the latest measurement date, not createdAt. Weight corrections do not proactively update old exercise records. Return weight and latest profileVersion/currentWeight so the frontend does not retain stale versions.

## 8.6 Food search and preview

Retain paginated search responses `{results,totalMatches,page,size,hasMore}`; do not change to an array without adapting the frontend. Each item contains immutable referenceId, publicFoodKey, name, source version, active, and per100g. Search returns only valid catalogue entries available for new selection; historical reference details may read inactive versions.

Nutrition preview takes referenceId + grams and returns reference + grams + portioned nutrients + `calculationFingerprint`. This is not an AI estimate; never convert missing nutrients to zero. Changing an item's reference or grams requires a new preview; do not confirm using the old preview before new data arrives.

If selected official core nutrient fields are missing, this plan disallows new selection as a complete core reference by default and explains `UNSUPPORTED_REFERENCE`. Preserve nulls and source notes for other sources/legacy records. If the team approves records with missing nutrients, extend acceptance criteria and partial statistics together; do not silently fill values.

## 8.7 Food batch requests and receipts

Synthetic contract example below; referenceIds are not actual official food IDs:

```json
{
  "submissionId":"0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd",
  "recordDate":"2026-10-16",
  "zone":"Australia/Sydney",
  "source":"MANUAL",
  "items":[
    {"clientEntryId":"3d849f70-7c4e-4fdf-abd0-a53c1c807dd9","referenceId":"101","quantityGrams":150,"mealType":"LUNCH"},
    {"clientEntryId":"1845f5ac-2e84-4357-99e4-037559e2880b","referenceId":"102","quantityGrams":200,"mealType":"SNACK"}
  ]
}
```

Requests contain no final nutrient values, user IDs, or arbitrary food names substituting for references. Calculate nutrition from immutable referenceId. Successful saving returns a minimal receipt:

```json
{
  "submissionId":"0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd",
  "kind":"FOOD_BATCH",
  "committedAt":"2026-10-16T08:00:00Z",
  "replayed":false,
  "records":[
    {"id":"501","clientEntryId":"3d849f70-7c4e-4fdf-abd0-a53c1c807dd9","recordDate":"2026-10-16","versionAtCreation":0},
    {"id":"502","clientEntryId":"1845f5ac-2e84-4357-99e4-037559e2880b","recordDate":"2026-10-16","versionAtCreation":0}
  ]
}
```

Initial creation returns 201; replay of the same submission returns 200. Receipts prove creation succeeded at that time, not current record contents. If records were subsequently edited/deleted, replay still returns the original creation identities without recreating or reverting them. The frontend reads current details/lists by IDs/dates; never overwrite the latest edited version with historical receipt versionAtCreation.

`GET /submissions/{submissionId}` is accessible only to its current user. 200 means committed; 404 means **no committed receipt is currently observed**, not proof that another request cannot commit later. Do not immediately generate a new submission ID after 404 and duplicate the entry.

## 8.8 Exercise preview and saving

The catalogue returns `[{id,activityKey,displayName,sourceActivityCode,sourceDescription,metValue,dataset,active}]`. The backend accepts valid preset IDs only; it must not guess catalogue entries from familiar names supplied by the frontend.

Preview responses include at least exerciseType, recordDate, durationMinutes, nullable estimatedKj, estimateStatus, unavailableReason, weightKgUsed, weightDateUsed, metUsed, formulaVersion, and calculationFingerprint.

Save POST includes `submissionId/clientEntryId/exerciseTypeId/durationMinutes/recordDate/zone/source/calculationFingerprint`. Recheck current weight/applicability on the server; if calculation inputs differ from the reviewed fingerprint, return 409 `CALCULATION_CHANGED` with the latest preview for reconfirmation rather than silently saving a different estimate. Valid activity may still be created without an estimate, with energy=null, not zero.

Unavailability precedence is fixed: inapplicable reference/user-age information → NO_APPLICABLE_REFERENCE; applicable but no weight on or before the date → MISSING_WEIGHT; otherwise AVAILABLE. Unrecoverable legacy calculation inputs → LEGACY_UNVERIFIED, preserving old energy while indicating incomplete provenance. Save complete calculation inputs for available results in the record.

## 8.9 Corrections, deletion, details, and versions

PUT food `{expectedVersion,referenceId,quantityGrams,mealType,recordDate,zone}`; PUT exercise `{expectedVersion,exerciseTypeId,durationMinutes,recordDate,zone,calculationFingerprint}`. Fingerprint is required when calculation inputs change; unchanged requests may omit it/use null (see no-op rules in 14.6). Do not accept userId. Retain original source; manual correction must not erase the original input method.

Call DELETE `/{id}?expectedVersion=N` after confirmation; the server soft-deletes. Repeated DELETE of the current user's already-deleted record may return 204 without resurrection; missing/other-user records uniformly return 404. Ordinary corrections/deletions require versions, returning 400 if absent. 409 responses must not expose others' data.

GET by-id returns the current nondeleted record. A just-deleted same-user record checked against a receipt may return 404; the frontend says “Previously saved, now deleted” and must not automatically restore it because a receipt exists.

Use `@Version` + request expectedVersion for corrections; never call `entity.setVersion(expectedVersion)`. Compare, modify, then flush inside the transaction to catch actual concurrent-update conflicts.

## 8.10 Summary, administrator, and capabilities APIs

`summary/daily?date=YYYY-MM-DD&zone=` and `summary/today?zone=` share one day aggregate; today only determines date. The original weekly parameter is `end`, not from/to. Retain `summary/weekly?end=YYYY-MM-DD&zone=` for the seven days ending at end compared with the preceding seven days, explicitly returning rangeStart/rangeEnd/dayCount. Arbitrary historical ranges use `summary/trends?from=...&to=...&zone=` to return Section 15.4 PeriodSummary, including daily series and totals/averages/facts; an arbitrary ten-day interval is not weekly. The original trends days shortcut may remain compatible, but mixing it with from/to must explicitly return 400.

Standard summaries contain record counts, known energy sum, missing count, hasData/partial, activityMinutes, nutrient totals, available-day average and denominator, and currentGoalComparison. See Section 10.

Administrator lists expose only identity/role/enabled/version, excluding profile/food/exercise/safety data. Status PATCH takes `{expectedVersion}`. If retaining reset-password routes, maintain security/version requirements, but do not add UI features beyond this scope.

capabilities returns `schemaVersion:'mvp-1'`, `available/reason` per capability, and catalogue ready/verified states. It reflects backend configuration/readiness, not proof of real-model invocation or acceptance. Frontend feature flags may disable further, but cannot override backend unavailability to claim availability.

---

# 9. Database upgrades: protecting existing data, tables, and migration details

## 9.1 Migration principles [BC-14]

Current `ddl-auto=update` does not explain data/constraint migration. Use Flyway versioned SQL, then `ddl-auto=validate` once the corresponding schema is ready. Do not let schema.sql, Hibernate update, and Flyway compete to alter tables. See [DOC-DB] for Boot initialization recommendations and the PostgreSQL Flyway module.

Plan to add Boot-managed `spring-boot-starter-flyway` and `org.flywaydb:flyway-database-postgresql`; check effective versions against Boot 4.1.1 dependency management rather than mixing incompatible versions. For CLI operations, configure dedicated maintenance scripts/an explicit Maven plugin; installing a starter does not provide every Maven goal.

Test both paths:

- **New database:** V1 creates the approved six-table baseline, then V2…V7 produce the target entity schema.
- **Existing database:** back up, export schema, match V1, check duplicates/anomalies, explicitly baseline to 1 through a dedicated maintenance operation, then apply V2…V7. Default `baseline-on-migrate` to false. Stop on schema mismatch; do not automatically baseline arbitrary databases.

This plan specifies logical migration contents, not one-click destructive SQL pretending to suit every existing database. Verify old column names/types/constraint names/FKs against the real schema. Never delete unknown references with `CASCADE`.

## 9.2 Migration sequence and gates

| Target file | Contents | Risks/prerequisites |
|---|---|---|
| `V1__legacy_schema_baseline.sql` | Reproduce approved existing six-table structure; create only on new databases | Do not guess all Hibernate DDL from this prose; generate and diff it |
| `V2__account_profile_weight_integrity.sql` | user/profile/weight versions, update timestamps, adult confirmation, units, goal kJ, consent timestamps, unique weight dates | Resolve case-insensitive email and same-day weight conflicts first |
| `V3__versioned_reference_catalogues.sql` | food_dataset/activity_dataset/exercise_type; food_reference surrogate id, dataset/version/active/kJ | Carefully verify replacement of public_food_key PK |
| `V4__record_dates_snapshots_and_receipts.sql` | recordDate, version, soft deletion, UUID, reference FKs, kJ/calculation snapshots, submission_receipt | Add nullable columns first; never disguise legacy data as new data |
| `V5__legacy_data_backfill.sql` | Approved date/unit migration, legacy flags; retain original input | Explicit legacy timezone; do not guess food or MET |
| `V6__verified_constraints_and_indexes.sql` | Validate FKs, conditional constraints, uniqueness, owner-date indexes | Execute after completing/reviewing backfill report |
| `V7__canonical_schema_validation.sql` | Final necessary NOT NULL, catalogue state, transitional constraints, entity validation | Do not incidentally delete old business columns; separate cleanup migration/PR |

Continue existing Flyway numbering if present; do not reuse V1. Never edit migrations already executed in shared environments; fix through new files. SQL must not embed current-date constants or import time-varying external data.

## 9.3 Target changes per table

### app_user [BC-01, BC-02, BC-12]

Retain existing PK, password_hash, email, display_name, role, enabled, and created_at. Add updated_at/version. Create a unique lower(email) index; application writes also trim/lowercase. **Do not merge/delete** existing users whose emails differ only by case; report conflicts for the user to decide.

Do not automatically demote all existing ADMIN users because the legacy first-user-ADMIN rule existed. New administrator bootstrap is a separate explicit operation; the project lead confirms existing administrator ownership.

### user_profile [BC-02]

Retain shared user_id PK/@MapsId. Add adult_confirmed, energy_unit, daily_target_kj, health_safety_updated_at, qa_consent_updated_at, updated_at, and version. Existing names such as height may remain; synchronize Java BigDecimal mappings with migrations.

The physical `health_safety_consent` column may remain and map to Java/DTO `qaConsentGranted`; do not duplicate consent merely to rename it. Stop using ai_qa_eligible for decisions; retain and deprecate it before eventual deletion. Keep energy_goal_kcal only for historical compatibility; daily_target_kj becomes authoritative.

**Do not set adult_confirmed=true for all existing users.** Without explicit confirmation evidence, set false and request confirmation on the next visit. A dedicated migration-status notice may handle valid legacy onboarding, without claiming prior consent.

Changing energyUnit alone must not alter food/exercise snapshots; changing goal alone must not calculate an energy target automatically.

### weight_entry [BC-03]

Retain physical `entry_date`, using recordDate in DTOs to avoid pointless renaming. Add updated_at/version; convert weight_kg to numeric(6,2) after validating old values. Add unique `(user_id,entry_date)` and index `(user_id,entry_date desc)`.

Duplicate dates: even identical values require backup/cleanup records. For different values, propose retention according to the existing “latest created_at for that day” semantics, but require manual confirmation rather than deleting directly. Preserve original candidate rows in protected backups, excluded from Git. Historical corrections do not add dates or recalculate confirmed activities.

### food_dataset / food_reference [BC-04]

food_dataset includes at least name/release/revision/source_url/content_sha256/nutrient_policy/imported_at, plus verified or equivalent state distinguishing real sources from unverified legacy data. Unique `(name,release,revision)`.

**Special handling of the original PK:** FoodReference currently uses String publicFoodKey as `@Id`, and its Repository uses String. Versioned references require a new Long id:

1. Add a surrogate id to the old table, generate unique values for existing rows, and verify counts.
2. Add dataset_id; assign old rows to an explicit legacy dataset, not official AUSNUT.
3. Verify every old FK before switching the PK to id; retain original public_food_key values.
4. Change Repository to JpaRepository<FoodReference,Long>; replace old key lookup with explicit findActiveByPublicFoodKey, not findById(key).
5. Add unique `(dataset_id,public_food_key)`. Populate survey_id only with real provenance; use appropriate nullable uniqueness. Check conflicts before adding a partial unique active-public-key index.
6. Add official_name/display_name/description, energy_kj_100g/protein_g_100g/fat_g_100g/carbs_g_100g, and is_active. Do not delete existing fiber/sugar/sodium fields without explanation.
7. New food-record FKs point to immutable id, not a public key whose active version changes.

The old AFCD file's SHA proves that attachment's contents, not official AUSNUT provenance. If original provenance is unknown, source_url may be empty and explicitly unverified. Do not fake provenance with an all-zero fingerprint.

### activity_dataset / exercise_type [BC-05]

activity_dataset contains source version, content fingerprint, and min_age/max_age; exercise_type contains dataset_id, activity_key, display_name, source_activity_code/source_description, met_value numeric(6,2), and is_active. Follow Proposal uniqueness for dataset/activity key and active key.

The original ExerciseCatalog Map may be exported as `LEGACY_CONFIG_UNVERIFIED` to explain old records, never labelled official 2024 entries. The new production catalogue uses approved representative entries, with one general option per common activity. Matching names do not imply matching MET values.

### food_record / exercise_record [BC-06, BC-08, BC-09, BC-10]

Shared: retain id/user_id/source/created_at; add recorded_date, updated_at, version, deleted_at, client_entry_id, and data_quality. Keep recorded_at to interpret old records; new queries no longer group by it.

food_record adds food_reference_id FK, energy_kj, canonical numeric macronutrients, and snapshot name (existing food_name is sufficient). quantity_grams is numeric(10,3). Original calories is read-only compatibility, never a second writable authority. Deleting/deactivating references must not cascade-delete records.

exercise_record adds exercise_type_id FK, duration_minutes numeric(8,2), weight_kg_used/weight_date_used/met_used/estimated_kj/estimate_status/formula_version. Legacy exercise_type string/intensity may remain temporarily; new forms do not require intensity. If retaining its NOT NULL constraint, write a server compatibility default without claiming it was user-selected.

Each table constrains `(user_id,client_entry_id)` without `WHERE deleted_at IS NULL`, so soft deletion does not release retry identity. Index `(user_id,recorded_date)` for nondeleted queries. Ownership FKs must not default to cascading deletion of user data.

`data_quality` is `CURRENT` / `LEGACY_UNVERIFIED`. CURRENT requires canonical reference, business date, valid input, and snapshot; LEGACY may lack referenceId or calculation evidence. Never fabricate referenceIds to satisfy NOT NULL on old rows.

### submission_receipt [BC-07]

```text
id bigint PK
user_id bigint NOT NULL FK app_user
submission_id uuid NOT NULL
operation varchar(20) NOT NULL         # FOOD_BATCH / EXERCISE_CREATE
request_sha256 char(64) NOT NULL
result_json jsonb NULL                  # Only record IDs/date/creation version, never safety answers
created_at timestamptz NOT NULL
committed_at timestamptz NULL
UNIQUE(user_id, submission_id)
```

Complete result_json/committed_at at the end of the same transaction. Intermediate receipts/records remain invisible to other transactions, and exceptions roll everything back; no visible permanently IN_PROGRESS row may remain. Tests must prove this.

Receipts are user-isolated; do not expose a list allowing administrators to inspect private records. MVP does not delete receipts, to retain retry identity. Any future retention period requires a nonreusable tombstone; simply deleting receipts must not let old requests resurrect records.

## 9.4 Legacy backfill rules: no guessing

| Legacy data | Permitted backfill | Prohibited backfill |
|---|---|---|
| calories value | Explicit kcal × 4.184 → kJ, preserving original value/migration record | Replace with new reference nutrition after matching food names |
| recordedAt | Convert date in confirmed legacy zone, preserving timestamp | Claim the converted date necessarily matches the user's original selection |
| foodName | Retain name; keep unproven referenceId null/legacy | Fuzzy-match the first same-name item and claim traceability |
| exercise estimate | Convert old value's units; status=LEGACY_UNVERIFIED | Infer weightUsed/MET/formula using current weight |
| Missing client ID | Generate unique UUID per old row to prevent new conflicts; mark migration-generated | Claim recovery of original client submission identity |
| Missing consent | false; missing safety answer null | Automatic NO/consent |
| Duplicate weight | Conflict report, backup, approved resolution | Silent deletion or averaging |

Incomplete legacy provenance does not prevent developing canonical new flows, but final reports must identify records remaining legacy. Do not hide data-quality problems from both UI and reports.

## 9.5 Migration rehearsal and rollback

Target maintenance scripts: `scripts/db/preflight.sh`, `backup.sh`, `adopt-baseline.sh`, `migrate.sh`, `verify.sh`, and `restore-to-new-db.sh`. Default to configuration-based checks only; database mutations require explicit database identity and authorization parameters. Verify backups by restoring into an independent new database and comparing row counts/constraints, not merely checking file existence.

Prefer compatible expand-schema plus application rollback. If data has been transformed, restore and verify in an independent database before the project lead decides whether to switch. Do not promise old applications can safely interpret every new-version write. Before release, record the supported rollback commit/schema range.

Migration checks include at least per-table old-database counts, normal/legacy counts, date distributions, total energy after unit conversion, orphan FKs, duplicate email/weight/client IDs, VALID constraints, all key indexes, entity validation, and repeated fresh-database startup without duplicate imports.

---

# 10. Java business-service implementation guidance

## 10.1 CurrentUser / Clock / dates

`CurrentUserService.requireActive(Authentication)` returns the current user entity or read-only identity; explicitly reject null authentication, missing users, and enabled=false. Controllers do not accept client userId. Use `findByIdAndUserIdAndDeletedAtIsNull` consistently for ordinary lookups; return 404 uniformly without first disclosing resource existence to others.

`ClockConfig` supplies a Clock Bean; RecordDatePolicy uses `LocalDate.now(clock.withZone(zone))`. Do not silently replace null input with server UTC today. New records require explicit recordDate, defaulted by the frontend. Reject from>to, excessive query ranges, and invalid zones. An engineering limit of 366 days/request may be documented in the contract, never silently truncate.

## 10.2 ProfileService and WeightService

Use a database lock on the current user row to serialize related profile/weight changes and record confirmation for that user; Java synchronized alone is insufficient. Consistent lock order: user → receipt (if any) → profile/weight → references/records sorted by ID, preventing inter-service deadlocks.

`completeOnboarding(userId, request)`: validate adult/year/basic fields/explicit answer and user/profile versions; save complete profile + initial weight; set onboardingComplete last. `updateSettings`: complete snapshot including null clearing, separate user/profile version checks, name/profile updates in one transaction. `WeightService.upsert`: look up by date, check version, add/update one row, and select currentWeight from the latest date. Ordinary profile updates do not incidentally write weight.

If legacy profile.weightKg remains temporarily as a cache, WeightService must maintain every update. Estimates must not use that cache as historical weight evidence. Removing it later is optional for this work.

ProfileService also supplies `requireCompletedAdultSetup(userId)` for new record creation/corrections and QA. Frontend redirects alone cannot enforce adult scope. Do not block profile reading/confirmation completion/weight maintenance through this check or remove access to existing legacy records. Previously completed users with adultConfirmed=false follow 13.2 confirmation recovery without writing initial weight again.

## 10.3 FoodCalculationService

Suggested signature:

```java
NutrientSnapshot calculate(FoodReference reference, BigDecimal grams);
```

Validate reference existence, available version, valid core fields, and grams precision. For each value, calculate `per100g * grams / 100`, then final `setScale(3, HALF_UP)`. Do not convert to double or round intermediate steps. Preview and saving call the same function.

If displaying batch totals, sum rounded record snapshots. Do not preview totals using unrounded values while saving rounded totals. Disable/delegate the original FoodReferenceController scale function rather than retaining a second two-decimal algorithm.

## 10.4 ExerciseEstimateService

Suggested signature:

```java
ExerciseEstimate estimate(Long userId, Long exerciseTypeId,
    LocalDate recordDate, BigDecimal minutes);
```

1. Load the activity reference/dataset; reject nonexistent types, but return unavailable for valid types with insufficient applicability.
2. Derive year-based age from recordDate's year and birthYear; check dataset range. Birth year does not establish exact birthday; document this as a year approximation, not a clinical age determination.
3. `findFirstByUserIdAndEntryDateLessThanEqualOrderByEntryDateDesc(...)` obtains the latest applicable weight; no 70 kg fallback.
4. Calculate `MET × weightKg × minutes × 4.184 / 60`, rounding once at the end to three decimal places.
5. Return nullable snapshot and status/reason; only AVAILABLE has complete evidence. Do not insert zero merely to make unavailable values look tidy.
6. Fingerprints bind only canonical calculation inputs/reference version/used weight and date, not display units or unrelated names. Recalculate and compare the reviewed fingerprint on save.

Synthetic formula test: MET=3.5, 70 kg, 10 min → 170.846666… kJ, stored as 170.847. This is a pure formula test, not a claim that official Walking necessarily uses 3.5; production entries follow approved sources.

Calculate only on new saves/explicit corrections. Profile/weight edits do not bulk-update existing exercise_record rows. Historical details read saved snapshots directly rather than “correcting” them against today's profile.

## 10.5 Idempotency and atomic batches: verify in this order [BC-07/08/09]

Use one database transaction, with no model calls inside it. For a low-concurrency course application, same-user row locking can simplify reliability; do not introduce distributed locks.

Pseudocode:

```text
confirm(user, canonical request):
  authenticate + basic shape validation
  compute canonical hash (excluding final nutrition and current time)
  begin transaction
    lock user row; recheck active and scope
    existing = receipt(user, submissionId)
    if existing:
       if operation/hash differs -> 409 IDEMPOTENCY_CONFLICT
       return stored receipt as replay (no recalculation or record changes)
    insert receipt using UNIQUE(user, submissionId)
    validate no duplicate item clientEntryIds within request
    reject item IDs already owned by another submission; do not silently skip
    load all immutable references, check availability and inputs
    calculate all snapshots; check exercise fingerprint if applicable
    save all rows; flush so constraints run inside transaction
    store minimal receipt result and committedAt
  commit
  return receipt only after successful commit
```

Canonical hashing: fixed field order, canonical dates/UUIDs/enums, BigDecimal without insignificant trailing zeros. Sort items by clientEntryId before hashing; explain that display order does not change business submission contents. Include operation/source/recordDate/actual inputs/reference IDs. Normalize zone and freeze it for the submission; it cannot change on retry. Create cross-language fixtures confirming the client's frozen payload; the server hash is authoritative.

Concurrency: user row locking orders same-user requests, but database uniqueness remains mandatory. If using `INSERT ... ON CONFLICT DO NOTHING`, subsequently read the committed matching receipt in the same transaction. Do not catch a uniqueness exception and continue using a rollback-only transaction. See [DOC-PG]; verify with two real concurrent connections, not single-threaded mocks.

Find receipts before revalidating reference-active/date-dependent business conditions for new inputs. Successful original requests must remain identifiable as retries after reference deactivation/profile changes, without creating again or arbitrarily changing success into failure. Always check identity/authorization first.

Every failure rolls back receipt and records. Do not swallow JSON construction, database flush, or receipt-update exceptions. Do not commit records before adding receipts, or commit pending receipts without recovery strategy.

## 10.6 Record correction and soft deletion

Separate create/update DTOs; do not reuse CreateFoodRecordRequest to conceal different required fields/version semantics. New requests cannot directly modify calories/confidence/metUsed/owner/source.

Read the owned record including version; compare expectedVersion; recalculate food from reference + grams and exercise from reviewed new inputs. Changing date updates only the record/snapshot, not manual additions/subtractions in daily-total tables, because summaries are derived queries.

Inactive references: changing only meal/display-unrelated fields may preserve the snapshot. Explicitly define whether immutable historical references can be used when changing quantity or reselecting references. Default policy permits old records to recalculate using their own original referenceId, but forbids inactive references for new records; replacement references must be active. Implement this exception explicitly in Service, not merely frontend visibility.

On editing 409, fetch the latest record and retain local drafts for comparison; never automatically replace expectedVersion and overwrite again. Deletion sets deletedAt=clock.instant and increments version; all queries exclude deleted rows. Preserve stable clientEntryId and receipt.

Lost write response: create retrieves the receipt; edit/delete reads the current record to compare state. If contents already match user intent, report the verified result. Otherwise, or if uncertain, require review rather than automatically overwriting another change.

## 10.7 SummaryService: one policy across all pages

TodaySummaryController and DataSummaryController must not implement separate business algorithms. Both delegate to one aggregation: filter by user + recordDate + nondeleted, group by date in the database, and assemble consistent DTOs in Service.

Each metric returns:

```text
knownSum: number | null
recordCount: integer
knownValueCount: integer
missingValueCount: integer
legacyValueCount: integer
hasData: boolean
partial: boolean
```

Rules: no records of that type → hasData=false, knownSum=null; records with entirely missing values → hasData=true, knownSum=null, missing>0; partially missing values → knownSum sums known values, partial=true; real zero-valued records → knownSum=0, knownValueCount>0. Do not discard state by applying `??0` to null.

Empty Dashboard days may show Demo-style “0 recorded”, with a clear “No records yet” notice and retained hasData state. Trend series use null for no-data days, not zero measurement points. Valid exercise minutes count even when energy is unavailable.

Averages: for each metric independently, divide by days with known values, return the denominator, and label it recorded-day average. Do not default to the full calendar span or infer unrecorded meals. Arbitrary selected ranges must agree with corresponding History lists.

Goal comparison uses only explicit user dailyTargetKj; null produces no comparison. Disable CalorieTargetCalculator automatic fallback in production summaries and the `target-intake+exercise` “remaining food today” display. Intake minus recorded exercise is not a complete daily energy deficit.

RuleSummaryService produces auditable facts: totals, recorded-day averages, coverage days, changes against the preceding equal-length range, current-goal comparison, and recorded weight changes. Report insufficient comparison data instead of replacing null with zero to invent trends. Rule summaries do not call LLMs and remain readable when Q&A is restricted.

## 10.8 ReferenceImportService

Import using an explicit manifest listing file SHA, dataset/release/revision, source URL, field mapping, core-nutrient policy, and license/provenance notes. Exact AUSNUT filenames/columns are absent from attached source. **Implementers must not guess mappings from memory:** inspect actual official files, then record exact header mappings in manifests/tests.

Import sequence: read/validate all required fields → deduplicate/range-check → verify SHA → validate sample temporarily → transactionally write new dataset/references → switch active → report completion. Reimporting an identical manifest adds no rows; identical version with different content hash is rejected and requires a new revision. Old versions are immutable. Do not skip all version updates whenever startup finds count>0, or log errors while pretending the catalogue is ready.

Activity imports likewise preserve source code/description, MET, and applicability. The manifest fixes selected general activities; Demo Badminton/legacy Map coefficients are not automatically official facts.

---

# 11. Security, JSON compatibility, and runtime configuration

## 11.1 Cookie sessions and login persistence [BC-01]

Retain Spring Security sessions, not JWT. The custom AuthController already manually calls saveContext; additionally inspect session-fixation protection and CSRF token rotation. Default design [DOC-SEC]:

- SecurityConfig and AuthController share one SecurityContextRepository Bean.
- After AuthenticationManager succeeds, explicitly invoke the configured SessionAuthenticationStrategy before creating/setting/saving SecurityContext.
- SessionAuthenticationStrategy combines framework session-ID rotation and CSRF authentication strategies. If the latest branch uses a framework authentication filter that already does this, do not invoke it twice.
- Calling only `SecurityContextHolder.setContext` without persistence is insufficient; configuring sessionManagement does not automatically handle everything in a handwritten Controller.
- Use JSON entry points; unauthenticated ordinary APIs return 401, not a 302 HTML page.
- Disabled accounts cannot log in. This plan additionally requires CurrentUserService to check enabled on existing-session requests and reject access, explicitly as a security enhancement rather than claiming the Proposal requires immediate eviction of every session.

Logout safely invalidates the existing session without unnecessarily creating one through `getSession()`. Clear Context, CSRF, and session Cookie. If logout fails over the network, clear sensitive frontend display without claiming server logout succeeded; prevent direct switching to another account until retry/session verification.

## 11.2 CSRF policy [BC-01]

Default to HttpSessionCsrfTokenRepository + Spring's existing CsrfTokenRequestHandler. Read framework-issued tokens through GET `/auth/csrf`, retain them in frontend memory, and use returned headerName. Masked/unmasked handling must match the version's request handler; do not combine Cookie/session examples into an incompatible configuration. [DOC-CSRF]

Anonymous register/login also require tokens; OPTIONS/GET must not perform business writes. Framework CSRF strategy clears old tokens after authentication, and the frontend fetches replacements; likewise after logout. Restrict allowed origins to actual development origins; credentialed CORS must not use `*`.

On explicit CSRF_INVALID with server confirmation that the security filter rejected before business processing, refresh the token and resend once. Do not automatically replay writes for other 403/500/timeouts. No global unlimited-retry interceptor.

## 11.3 JSON and Boot 4

Current vision/loader code constructs Jackson 2 ObjectMappers. Determine actual MVC serializer version from effective Boot dependencies and choose one consistent DTO serialization policy. Do not assume arbitrary `com.fasterxml.jackson.databind.ObjectMapper` injection or copy old Spring test annotation imports.

HTTP tests must verify Long → string IDs, BigDecimal → JSON numbers, LocalDate → `YYYY-MM-DD`, Instant → ISO8601, enums, null fields, field errors, and JSONB receipt roundtrips. CanonicalSubmissionHasher uses fixed explicit fields, independent of incidental global mapper field order.

## 11.4 Environment and local integration

Retain service roles: PostgreSQL `localhost:5432`, Java `localhost:8080`, Vite `localhost:5173`. Original Demo `127.0.0.1:5174` is read-only reference, not an interchangeable production origin.

Target Vite configuration outline (execute after implementation):

```ts
server: {
  host: 'localhost', port: 5173, strictPort: true,
  proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
}
```

Axios baseURL=`/api`; business functions use `/auth/me`, not another `/api/auth/me`, avoiding `/api/api`. Axios handles default JSON request headers. Do not manually set Content-Type for FormData; retain the browser's multipart boundary. [DOC-VITE]

Production Vite builds contain no development proxy. Default deployment uses a same-origin static frontend with reverse-proxied `/api` to Java, or team-approved Spring static hosting. Verify direct refresh of `/review` and `/history` does not 404. Unknown `/api` paths must not fall back to index.html.

Target environment variables: DB_URL/DB_USERNAME/DB_PASSWORD, APP_ALLOWED_ORIGINS, APP_DEFAULT_ZONE, SESSION_COOKIE_SECURE, OPENAI_API_KEY, AI_IMAGE_MODEL/AI_TRANSCRIPTION_MODEL/AI_TEXT_MODEL, AI_FEATURES_ENABLED. Verify model names against the actual account; hardcoded names are not proof of permanent availability.

Use an explicit local development profile. Production requires HTTPS, secure/httpOnly/sameSite Cookie configuration, and trusted proxy-header boundaries. Disable SESSION_COOKIE_SECURE only for HTTP localhost in the local profile, never in production for convenience.

## 11.5 Logging, external calls, and performance boundaries

Structured logs contain only requestId, operation, status, duration, and necessary anonymized diagnostics. Never log passwords, cookies, CSRF tokens, Authorization headers, raw health answers, entire audio/image base64, or sensitive raw model output.

Approximately three seconds for ordinary requests is the Proposal acceptance target; AI calls have separate timeouts. Suggested engineering defaults: ordinary client HTTP 10–15 seconds, provider connection 10 seconds, processing 45–60 seconds adjusted to actual capability, explicit file/recording limits. These are configurations to test, not measured performance.

Do not wait for models inside transactions. Model failures affect only the assisted input/Q&A operation, not manual recording/summaries. Do not probe real models expensively on every capabilities request.

---

# 12. Frontend infrastructure: HTTP, sessions, caching, and routing

This section replaces Demo's `api.ready → Database → emit` chain, not the framework. Acceptance requires the production dependency graph to exclude simulated accounts/data and local authoritative calculations, beyond simply opening pages.

## 12.1 `api/client.ts`: one real client

Responsibilities:

1. Reuse the main project's `axios.create`, baseURL `/api`, `withCredentials:true`. Read public configuration; no browser provider keys.
2. JSON/upload requests share authentication/error conventions, but uploads must not inherit manually imposed JSON Content-Type.
3. Every API function accepts `signal?: AbortSignal`; GET passes React Query's signal into Axios. Cancellation does not display a server-error notice. [DOC-QUERY]
4. Writes go through `csrf.ts::ensureCsrfToken()`; share the loading promise to avoid ten simultaneous initializations. Refresh/resend once only for explicit CSRF rejection.
5. Interceptors do not select navigation destinations; normalize errors and pass them to SessionProvider/callers. Avoid `client → router → hooks → client` cycles.
6. 401 emits session expiry only. Do not treat every 403 as logout: it may be ordinary-user admin access, restricted QA, or CSRF failure.
7. No automatic retry for arbitrary POSTs. GET transient errors may have bounded retries; never blindly retry 401/403/400/404.
8. Unexpected non-JSON responses become `UNEXPECTED_RESPONSE`; reverse-proxy index.html is not an authenticated-user object.

Suggested function boundaries:

```ts
// Signature outline; complete types follow the contract fixed in P02.
get<T>(path: string, options?: { params?: object; signal?: AbortSignal }): Promise<T>
post<T, B>(path: string, body: B, options?: RequestOptions): Promise<T>
normaliseApiError(error: unknown): ApiError
isAuthExpired(error: ApiError): boolean
isKnownRejectedWrite(error: ApiError): boolean
```

`isKnownRejectedWrite` cannot merely check `status < 500`: only explicit trusted-backend validation/authorization/conflict responses establish business rejection. Proxy disconnects, client timeouts, write cancellation, and unparseable responses may have uncertain outcomes.

## 12.2 `api/contracts.ts`, `validators.ts`, and adapter boundaries

Define request/response types per API. At entry, validate at least discriminators, string IDs, finite numbers, required-null semantics, and enums. Existing Zod may validate **API boundaries** without forcing all forms into Zod.

Requirements:

- `CurrentUser.id` and reference/record/weight IDs must be strings. Do not silently accept numeric IDs; investigate an old contract. Temporary legacy adapters must be separate functions with a removal task.
- DTOs expose no database object references/passwordHash. Unknown enums produce explicit version/data errors, not default NO or default meal values.
- `NaN/Infinity/undefined` cannot enter JSON/charts. `null` means missing; never convert it to zero with `Number(null)`.
- Use `Date` only for display/Clock adaptation; persisted record dates are plain strings. kJ↔kcal conversion lives only in `domain/units.ts`.
- Use explicit mappers for Demo snake_case fields: `recorded_date → recordDate`, `energy_unit → energyUnit`, `qa_consent_granted → qaConsentGranted`. Generic snake_case-to-camelCase conversion does not automatically align semantics.
- During transition, only `api/legacyAdapters.ts`, if needed, understands old `calories/recordedAt`. Remove it after canonical APIs are complete; it must not permanently hide server-version mismatches.

## 12.3 `SessionProvider`: identity is a server fact

States include at least `booting | authenticated | anonymous | expired | error | loggingOut`. Neither user nor profile may be restored solely from localStorage.

Startup sequence:

```text
Create one QueryClient
  → GET /auth/csrf (anonymous access allowed)
  → GET /auth/me
      401: anonymous; show AuthPage
      Network failure: error; show retry, never load Demo accounts
      200: authenticated
           ADMIN: management layout; do not force ordinary-user questionnaire
           USER: read profile and capabilities
                 Request failure: retryable error, not incomplete setup
                 Incomplete: /onboarding
                 Complete but adult confirmation missing: /onboarding?mode=reconfirm
                 Complete and adultConfirmed: original protected destination or /dashboard
```

CSRF bootstrap and me failures may retry independently. An unloaded token does not mean login failed. This plan defaults `/capabilities` to authenticated access, read only after authentication. If the latest branch exposes a minimal public schema version, separately test that configuration is not leaked.

Login mutation: validate → authenticate → increment session epoch → clear old-user cache/drafts → refetch CSRF → fetch me/profile → navigate. Registration success only displays success/login entry; it does not set authenticated state.

Logout mutation: lock button → explain unsaved/unknown-outcome risk → call real logout → increment epoch → cancel old requests and clear cache/drafts/chat/recording resources → anonymous → refresh CSRF. Network failure says server logout could not be confirmed; clear sensitive display and prevent direct entry to another account's data pages, allowing logout retry/session recheck.

## 12.4 `sessionEpoch.ts`: prevent old requests contaminating new accounts

Maintain a monotonically increasing memory epoch. Every mutation captures `{ownerId, epoch}` at start. Responses mismatching the current session cannot update current UI, emit success toasts, or populate current caches.

On logout/login switch:

1. Call `queryClient.cancelQueries()`; actual queryFns must consume signals.
2. Clear all user caches and mutation results, not just me while leaving history.
3. `DraftProvider.reset()`, clear ChatPage messages, stop Recorder tracks.
4. Cancel/clear old-owner references in callbacks. Even if an HTTP POST cannot be recalled, server identity follows that request's Cookie; the frontend cannot display its receipt to the new user.

Multiple tabs may use BroadcastChannel for a **private-data-free** `session-changed` notice, prompting recipients to revalidate me and clear old caches. This is an enhancement; server authorization cannot depend on it.

## 12.5 `queryKeys.ts` and cache invalidation matrix

Generate keys centrally, not independently per page. Examples:

```ts
session: ['session']
profile: ['user', userId, 'profile']
weights: ['user', userId, 'weights']
foodSearch: ['catalogue', catalogueRevision, 'food', query, page, size]
foodPreview: ['user', userId, 'food-preview', referenceId, canonicalGrams]
exerciseTypes: ['catalogue', catalogueRevision, 'exercise-types']
exerciseEstimate: ['user', userId, 'exercise-estimate', typeId, minutes,
                   recordDate, zone, profileVersion, currentWeightVersion]
foodRecords: ['user', userId, 'food-records', from, to, zone]
exerciseRecords: ['user', userId, 'exercise-records', from, to, zone]
summary: ['user', userId, 'summary', kind, from, to, zone]
record: ['user', userId, 'record', kind, recordId]
submission: ['user', userId, 'submission', submissionId]
```

Profile/currentWeight versions in estimate keys support frontend invalidation, not replacement of server date-based historical weight selection/fingerprints. Correcting earlier weight invalidates all current-user exercise estimates even when currentWeight is unchanged.

| Mutation | Minimum invalidation/update on success | Do not |
|---|---|---|
| Confirm new food/exercise | Corresponding records, all affected summary/history/trends; cache receipt if useful | Unconditionally add values to today's numbers |
| Correct date/content | Queries for old/new dates, record detail, related summaries | Refresh only the new date |
| Delete | Detail, records, summary; preserve creation receipt identity | Treat soft deletion as receipt deletion |
| Profile settings | profile, me displayName/version, goal summaries, estimates | Automatically alter saved records |
| Add/correct weight | weights, profile/currentWeight, all estimates, weight trends | Bulk-recalculate historical exercise |
| Logout/account switch | Clear all user data | Retain old-user data as placeholders |
| Admin status | Admin list/target detail | Read target user's private profile |

After saving, call `invalidateQueries`, awaiting displayed-query refetch where necessary. React Query mutation success and cache invalidation are separate outcomes. [DOC-INVALIDATE] A failed GET after successful writing is not write failure.

## 12.6 Routing integration and navigation blocking

Retain Demo routes. For reliable internal navigation blocking, default to replacing `BrowserRouter + Routes` with `createBrowserRouter + RouterProvider` from **the same React Router 7 package**. Reuse existing elements; introduce no framework server and do not replace React Query with Router actions. [DOC-ROUTER]

Rationale: `useBlocker` requires data-router context; do not insert examples from old versions/other modes into BrowserRouter. Create the router once at module level, not each render. Place Session/Draft/Notice providers appropriately above consumers; UnsavedChanges using router hooks must be inside RouterProvider. [DOC-BLOCK]

`ProtectedRoute` controls UX, not backend authorization. `returnTo` accepts only allowlisted internal paths, never external URLs or `//evil`. Direct `/review` without a draft explains the situation and returns to `/add`; do not invent an empty meal.

`useUnsavedChanges`:

- Confirm internal navigation away from dirty drafts/Profile forms; entering Review and returning to edit within the same flow is not discarding.
- Block ordinary departure during `saving`; `unknown` explicitly warns saving may have happened and offers recovery.
- Use beforeunload for hard refresh, subject to browser prompt rules; do not claim protection against shutdown/process termination.
- After explicit cancellation/confirmed saving, clear dirty/reset blocker before navigating to avoid another leave prompt.
- Blocking custom buttons alone does not establish browser Back/Forward coverage; Q05 requires actual tests.

---

# 13. Page-by-page implementation: inputs, outputs, events, and empty/error states

Retain Demo visuals and writing style, but every success message must reflect a real server result. Execute these specifications alongside Section 18 task cards.

## 13.1 AuthPage: source `account.tsx::Login`

**Retain:** login/register mode switching, password confirmation, field layout, success/error regions. **Remove:** `?demo=` account autofill, preset passwords, direct store-user lookup.

Events:

- `submitRegister`: trim email, validate displayName, require matching password/confirmation, omit confirmation/role from requests, call register; after 201 clear passwords, switch to login, retain email.
- `submitLogin`: obtain CSRF first, then login. Subsequent read failure displays authenticated/profile-recovery status or failure; do not resubmit passwords and create concurrent sessions.
- Authenticated visits to /login redirect by role/profile. Error state permits session checking rather than automatically assuming anonymous.
- Pending forms disable duplicate submissions, mode switching, and unsafe reentry involving password visibility. Display only public-safe server messages.
- Use consistent invalid-password notices. Account-disabled responses may explain the canonical code without unnecessarily revealing email existence.

Completion: real HTTP tests cover refresh, wrong password, case-insensitive duplicate email, requests after logout, and ordinary-user /admin access.

## 13.2 OnboardingPage: onboarding branch of `account.tsx::ProfilePage`

**Initialization:** after successful me for USER and first successful profile read, create the form snapshot. Initial date is today in the application zone; birthYear/units come from real profile or explicit defaults. Refetch must not repeatedly overwrite active input.

**Three steps:** About you → Goals & activity → Health & Q&A. Next validates only that step's required fields; final Save validates everything. Refusing Q&A is valid; adult confirmation and adult birth-year range are independently validated.

**Final save:** one POST/onboarding with both expectedVersions and initialWeight. Do not simulate a transaction through separate profile PUT, weight POST, and completion updates.

**Errors:**

- Locate field errors in their step and focus the first; preserve all other input.
- On 409, allow reading new profile/differences without overwriting entered data.
- ONBOARDING_ALREADY_COMPLETED first GETs and compares; uncertain network outcomes recover read-only, without guessed repeat writes.
- After rereading, navigate to /dashboard based on server completion. If reread fails, display completed/refresh-needed, not an invitation to create initial weight again.

**Difference from Profile:** onboarding submits initial weight once; later settings use an independent weight action.

**Legacy adult reconfirmation:** existing `onboardingCompleted=true`, `adultConfirmed=false` users must not resubmit initialWeight as new users. Preserve server records; frontend derives `needsAdultReconfirmation` and explains recovery at `/onboarding?mode=reconfirm`. Submit complete-settings PUT with existing versions, updating only the user's explicit confirmation and necessary fields they supply. Do not reset names/weight history or invent past consent timestamps. Until completion, the server disallows new ordinary records while preserving historical data. Only genuinely incomplete new users use POST/onboarding. This derived state needs no new table; P02 specifies it in profile/route contracts and tests.

## 13.3 AddRecordPage: source `entry.tsx::AddRecord`

Select food/exercise, then manual/voice/image. Images apply only to food. Invalid URL combinations show safe return without creating drafts. Confirm overwriting dirty drafts; “new” cannot bypass unknown-submission recovery.

Capabilities: M1 enables manual only; M2 enables individual methods after capabilities confirmation. Unconfigured models show Unavailable/manual alternatives, never successful simulated-recognition panels.

`START_NEW` centrally generates rowKey/clientEntryId and uses current userId/today with no default meal. Do not infer Breakfast/Dinner from clock time. Every input method produces the same canonical Draft.

## 13.4 EntryPage + FoodEditor: source `entry.tsx::FoodEditor`

**Per-row responsibility:** FoodItemEditor owns/reads its row reference, grams, meal, and confirmation state. Place hooks in separate row components, not directly inside variable-length parent array maps.

FoodReferencePicker:

- Debounce input approximately 250 ms (adjustable engineering choice); reset page when query changes.
- Call real search; show loading/error/no matches/load more. Empty input follows the contract's initial recommendations/empty guidance, never the Demo's 15 entries.
- Avoid Autocomplete local filtering after server search. Distinguish identical names from different sources by referenceId.
- Keep selections in row display state/by-id cache; a new results page lacking the item must not clear it.
- Old responses cannot overwrite newer queries; use both cancellation signals and query keys.
- Show actual dataset/release/revision. Explain verified=false/legacy explicitly, not merely an official-name icon.

Portions permit intermediate empty strings/decimal points without converting every keystroke to numbers. Normalize decimal input on blur/validation; reject negative/zero/excessive/overprecision values. Reference/grams changes revoke corresponding confirmation and invalidate preview.

Enable preview only for valid reference + grams; include both in query keys. Show matching current values/loading/failure. **Do not present old values as confirmed while new preview is absent.** Reference and grams require manual confirmation before Review. Meal is independently required but must not block side-effect-free nutrition preview.

Add/remove rows by rowKey, not index as persistent identity. Deleting one item must not rewrite others' clientEntryIds. Removing the last item may show an empty editor but cannot save an empty batch.

## 13.5 ExerciseEditor: source `entry.tsx::ExerciseEditor`

Read a structured server catalogue, not copied Demo eight-item/legacy 14-item maps. Fields are recordDate/typeId/duration, not required intensity/distance/pace.

When all three are valid, estimate; date changes also recalculate. AVAILABLE shows kJ, used weight/date, and formula. Unavailable shows reason and allows valid activity into Review. The frontend never calculates authoritative energy using currentWeight.

Bind preview fingerprint to current inputs; confirmation waits for it. Review cannot manually edit estimated energy. Profile/weight query changes invalidate estimates; the server still checks changed evidence on save.

Historical correction may compare old snapshots/new previews. Without changed calculation inputs, preserve snapshots under server rules; opening an editor does not change data.

## 13.6 ReviewPage: source `entry.tsx::Review`

Read DraftProvider, not Database. Display all selections, sources, dates, meals, portions, and currently valid previews. Editing may remain; any input change increments revision and revalidates/previews.

Return to edit retains the Draft; Cancel confirms then clears it; Confirm dispatches separate create/edit mutations rather than generic store.save. Disable the entire editing area while saving, not just buttons.

Edit mode permits one record only; editing a meal does not mean bulk-editing originally independent records. Title/button say Confirm changes. Separate POST/PUT; preserve original source and recordID.

Use Section 14 result handling, not another retry implementation. Success atomically clears Draft, updates notice, and navigates to the actual date. Pending old callbacks must not recreate cleared drafts.

## 13.7 DashboardPage: source `overview.tsx::Dashboard`

Read today's summary, food list, and exercise list in parallel. One `useToday(zone)` supplies date and updates at midnight/window refocus. Requests may fail independently, but failed lists must not appear as no records today.

Retain metrics, meal cards, exercise list, add buttons, and basic charts. Summary is authoritative for numbers; lists group for display. Temporary activity-minute sums require complete returned lists; incomplete pagination cannot establish totals.

Distinguish initial empty, food without exercise, unavailable exercise estimates, and partially unverified historical provenance. Missing goals show actual records only; do not present CalorieTargetCalculator output as a user goal or “X remaining to eat” as complete physiological energy balance.

Saved notices may include count/date. Failed summary refetch retains the saved fact and offers refresh. Label stale/failed-update data rather than presenting cached numbers as refreshed truth.

## 13.8 HistoryPage: source `overview.tsx::History`

URL is filter authority; suggested `/history?from=YYYY-MM-DD&to=YYYY-MM-DD&type=food|exercise|all&highlight=food:ID`. Exclude names, safety answers, and raw recognition text.

Validate dates/order on opening; default to recent seven days only for absent parameters. Invalid parameters receive guidance and an explicit default, not unbounded queries. Date/type changes update URL; Back/Forward restores filters instead of resetting today.

Group by recordDate; details use DTO saved snapshots and provenance. Zero completed results shows no matches; network errors show retry, distinctly.

Retain Demo period/daily/weekly summary switching: period calls trends(from,to) for matching-range facts; daily calls daily(date); weekly calls weekly(end) with an explicit seven-day range. `summary=period|daily|weekly&summaryDate=...` may preserve selection in URL across Back navigation. Do not send from/to to daily/weekly APIs that do not accept them.

**Locating saved past entries:** use actual recordDate from receipt/update to set a containing from/to range and highlight. Set type=all or matching type so filters cannot hide the new entry. Merely navigating to /history leaves 30-day-old records outside default seven days. If a highlighted record moved dates, GET by-id finds its current date and explains/adjusts; deleted records are explained, never resurrected.

## 13.9 ProfilePage: settings branch of `account.tsx::ProfilePage`

Separate `ProfileSettingsForm` and `WeightEntryForm` actions. Retain visual grouping if desired, but distinguish buttons/results so partial success is not called whole-page success.

ProfileSettingsForm stores baseline and user/profile versions on load. Refetch cannot overwrite dirty forms; changed server versions display a notice that submission will check updates. Save sends complete editable snapshot, genuinely clears nulls, and updates baseline/version on success. Cancel restores last successfully loaded values without API writes.

EnergyUnit changes only display/input interpretation. State the target's input unit; switching converts the same physical quantity, never relabels 2000 kcal as 2000 kJ. Server always stores dailyTargetKj. Removing the goal removes comparisons.

WeightEntryForm selects date/weight and checks same-day measurements. Existing rows show Correction and use their version; do not silently delete them. Success refreshes weight/profile/estimate queries without recalculating confirmed exercise.

Consent withdrawal immediately updates Q&A entry/chat restrictions after success. The server must recheck in-flight Q&A results; ordinary recording remains available.

## 13.10 AdminPage: source `extras.tsx::Admin`

Routes prevent ordinary USER entry; backend independently checks hasRole. Tables receive minimal user summaries, with no private-record viewer extension.

Send search parameters only when supported by backend. Filtering a limited page locally is not searching all accounts. Confirmation dialogs show target identity/current status/record-retention explanation; submit expectedVersion. Pending locks row and confirm button; 409 rereads without automatic re-disabling.

The Proposal does not define self-disable/last-active-administrator rules. This plan defaults to disallowing self-disable or disabling the last enabled ADMIN through this UI as an anti-lockout enhancement. Document/test under BC-12 rather than claiming it is an original requirement. Security-regress existing backend password reset without adding a new UI flow this round.

## 13.11 SourceNote, charts, Dialog, and helpers

- SourceNote renders only reference/dataset responses; absent sourceUrl does not justify a fake official link.
- RecordCharts leaves null gaps without connectNulls; nutrient gram proportions are not calorie proportions. Use unified server series.
- ConfirmDialog includes title, target, cancel/confirm, pending, double-click prevention, and keyboard focus return; deletion requires confirmation.
- AsyncPanel distinguishes initial loading/refetching/error/empty/partial; `data || []` must not hide errors.
- ErrorBox supports field/global errors and requestId for collaborative debugging, never backend stacks/SQL/raw provider output.
- EnergyValue standardizes units/precision/missing labels; inconsistent per-page toFixed must not create mismatches.

---

# 14. Submission, retry, editing, and leaving: one shared state machine

This is the highest-risk frontend integration logic. Every input method uses one implementation; do not copy five save implementations.

## 14.1 Distinguish three IDs and two versions

| Name | Lifetime | Purpose |
|---|---|---|
| rowKey | Editing one frontend row | React identity and field-error mapping |
| clientEntryId | One intentional record creation, including retries | Database uniqueness and item duplicate prevention |
| submissionId | One frozen complete confirmation and recovery | Batch identity and receipt lookup |
| Draft.revision | Increments on each edit | Detect stale preview/frozen payload |
| expectedVersion | Read from an existing server record | Concurrent edit/delete conflict detection |

Call `crypto.randomUUID()` only on explicit start/add/prepareSubmission events, never every render/retry.

## 14.2 Record creation algorithm

`useSubmitDraft.ts` coordinates; pure `submissionPayload.ts::prepareSubmission` normalizes without side effects.

```text
confirm(draft):
  synchronously check/set inFlightRef
  verify owner/session epoch, draft validity, matching preview/revision
  construct canonical payload once; generate submissionId; deep-freeze
  retain minimal recovery identity (optional sessionStorage, see 14.5)
  state = saving
  send real POST
    201/200 with valid receipt: state=saved; clear Draft; invalidate/navigate
    trusted explicit rejection: state=rejected; retain Draft; unfreeze for correction
    timeout/disconnection/unparseable result: state=unknown; retain frozen payload
  finally releases request lock only; never unconditionally clear Draft or mark success
```

Verify owner epoch when the Promise completes. Receipt submissionId/kind/item count/client IDs must match payload. Mismatch is protocol error requiring receipt investigation, not success.

## 14.3 Unknown-outcome recovery

`SubmissionRecovery.tsx` offers Check result, Retry same submission, and Leave safely with explicit risks.

- GET receipt 200 confirms creation. Clear draft and read current records, possibly edited/deleted by another tab; display current state.
- GET 404 means not yet confirmed; original request may still be processing. Wait or resend **original submissionId and payload**, never a new ID creating duplicates.
- Original POST retry returning 409 IDEMPOTENCY_CONFLICT indicates implementation error/changed data. Stop retries and retain requestId for investigation.
- Test slow original POST with receipt lookup returning 404 before commit; frontend cannot infer rollback.
- Only explicit backend business-validation rejection permits modifying the frozen payload; next confirmation uses a new submissionId. Network errors never promise saving was cancelled.

`AbortController.abort()` stops waiting/transmission, not server transactions. Cancelling recognition that creates no records is safe; cancelling an already-sent save cannot mean data definitely was not saved.

## 14.4 Separate save success from refresh

Confirmed write success cannot revert to rejected/unknown after GET failure. Clear Draft/submission lock, retain receipt, then refresh records/summary. Display `Saved. Some views could not refresh.` and retry GET.

Navigation: today's creation → Dashboard; past creation → dated History; edit → History containing new date and highlighted ID; batches may highlight first item and show count. Clear dirty state before navigation to avoid discard prompts after success.

## 14.5 Refresh, close, and minimal recovery identity

The Proposal does not require persistent cross-device Drafts. Do not store entire health/profile/food drafts in localStorage. Drafts default to memory.

An optional minimal sessionStorage journal may contain `{ownerId,submissionId,kind,startedAt}`, **never passwords, Cookies, tokens, safety answers, audio, images, or complete record payloads**. It is not a login credential. After refresh, verify me first and query receipts only for the matching owner.

Without frozenPayload after refresh, never automatically retry POST; inspect receipts/history. Temporary receipt 404 means unconfirmed, requiring further checking/manual comparison, not fabricated payload recovery. This does not guarantee cross-device draft recovery after crashes; acceptance evidence must state the boundary.

Delete journal after receipt reconciliation. On account switch clear display, but do not execute another user's journal. Warn about unresolved submissions before explicit logout. Server authorization always applies.

## 14.6 Editing/deleting differs from creation receipts

Receipts primarily serve creation. PUT uses expectedVersion; after a lost response, GET current record and compare with frozen update intent. Matching values establish current agreement, not proof this request caused it. Different version/content requires reload/manual confirmation, never unconditional replay overwriting changes.

After lost DELETE response, GET again: 404 establishes current invisibility, explained in context of this user's prior successful read; repeated same-owner DELETE may return 204. An unauthorized target's 404 does not prove the user deleted it.

Weight/settings/admin updates use the same version/reread principles. Do not force every write into food submission machinery or copy safety answers into receipts.

**No-change PUT:** compare food referenceId/grams and exercise typeId/minutes/recordDate. With no field changes, check version then return current record without snapshot recalculation/version increment. Food-only meal/date changes preserve nutrition; exercise without calculation-input changes preserves estimates. Exercise calculationFingerprint is required on creation/calculation-related changes; unchanged corrections may omit/use null, with server determination. Opening Review must not substitute current-weight calculations. Frontend disables Confirm changes when not dirty; backend independently enforces these rules.

## 14.7 Estimate changes during Review

Exercise CALCULATION_CHANGED means nothing saved. Retain input, show old/new evidence differences, refresh estimate, revoke corresponding confirmation, and submit a new submissionId after review. Do not reuse the old submissionId with a new fingerprint.

If food reference becomes inactive, show REFERENCE_INACTIVE and preserve other items while the user selects a valid reference. Immutable reference nutrition must not be overwritten behind the scenes; modifying same-version imported values is a reference implementation defect, not something frontend silently accommodates.

---

# 15. Cross-page data/state: call chains and API consistency

## 15.1 Identity, questionnaire, and empty Dashboard

| Step | Frontend event | Backend action | Next state |
|---|---|---|---|
| 1 | Submit Register | Create USER, unique database email, no session | Registration success and login |
| 2 | Login | Authenticate, protect session fixation, save Context | Reload CSRF/me |
| 3 | USER reads profile | Explicit incomplete representation if absent, not fabricated saved profile | Onboarding |
| 4 | Complete setup | user/profile/initialweight in one transaction | Read server completion/versions |
| 5 | Open Dashboard | Read confirmed daily records and unified summary | Genuine empty state if no records |
| 6 | Refresh | Revalidate session/profile | Correct destination without sessionStorage user ID |

Ordinary profile/record/summary/QA server endpoints uniformly require USER and current ownership; administration requires ADMIN. An active ADMIN returned by CurrentUserService does not imply permission to access others' private resources. If the course wants administrators to hold personal-record identities too, define that role policy explicitly; the implementing AI must not expand permissions independently.

## 15.2 Create, backdate, and correct two foods

```text
AddRecord → START_NEW(food)
  → FoodSearch(refId A/B) → Preview(A,150g)/Preview(B,200g)
  → Review(independent meal per item)
  → POST food/batch(same recordDate/submissionId, two clientEntryIds)
  → 201 receipt
  → invalidate records + summary
  → Today's Dashboard / past History(from=to=recordDate)
  → Refresh still reads the same two records
  → Correct A: GET A → LOAD_EDIT(version v)
       → Change grams/recordDate → Preview → PUT A(expectedVersion v)
       → A version v+1; B unchanged
       → Invalidate old/new date summaries and History together
```

Verify: editing one record does not POST the whole batch again; deleting B does not alter A's clientEntryId; parent meal settings do not overwrite two distinct item meals.

## 15.3 Exercise and weight dependencies

| Event | Recalculate/refetch | Must remain unchanged |
|---|---|---|
| Add earlier weight | Weight list; unsaved estimates for affected dates | All confirmed exercise snapshots |
| Correct current weight | currentWeight/profile; unsaved estimates | Old records' weightKgUsed/dateUsed |
| Correct exercise date | Its new estimate/weight selection; old/new date summaries | Other exercise records |
| Change energyUnit | Number formatting throughout UI | Stored kJ, snapshots, MET |
| Change display name/general goal | me/profile, goal comparison | Saved food/exercise results |
| Publish reference version | Catalogue list for new entries | Original reference versions/saved results |

Frontend weight changes must not iterate historical records and PUT them, violating snapshot requirements. Java services must disallow implicit bulk-recalculation endpoints.

## 15.4 Suggested stable SummaryResponse shape

P02 fixes this in OpenAPI rather than letting frontend/backend guess independently. This is a design outline, not an existing source capability:

```ts
interface MetricAggregate {
  knownSum: number | null;
  recordCount: number;
  knownValueCount: number;
  missingValueCount: number;
  legacyValueCount: number;
  hasData: boolean;
  partial: boolean;
}
interface DayAggregate {
  recordDate: LocalDateString;
  intake: MetricAggregate;
  protein: MetricAggregate;
  fat: MetricAggregate;
  carbs: MetricAggregate;
  expenditure: MetricAggregate;
  activityMinutes: number | null;
  foodCount: number;
  exerciseCount: number;
}
interface PeriodSummary {
  rangeStart: LocalDateString;
  rangeEnd: LocalDateString;
  calendarDayCount: number;
  days: DayAggregate[];
  totals: Omit<DayAggregate, 'recordDate'>;
  averages: Record<'intake'|'protein'|'fat'|'carbs'|'expenditure'|'activityMinutes',
                    {value: number | null; knownDayCount: number}>;
  goalComparison: {dailyTargetKj: number; comparedDays: number;
                   knownIntakeKj: number | null; targetForComparedDaysKj: number} | null;
  facts: Array<{code: string; text: string; values: Record<string, number | string | null>}>;
}
```

`totals.activityMinutes` is null without any exercise records, otherwise sums actual minutes. Intake/protein/etc. have independent known-day denominators; food count is not day count. `goalComparison` applies the current explicit goal to selected known-intake days. Label it comparison against the current reference, not the historical goal. Never fill unrecorded days with zero for comparison.

Daily/weekly/trends may tailor the same aggregation object, but specify public shapes clearly so one side does not expect `days` while the other returns only `points`. Daily/today may return a single DayAggregate and daily facts with unchanged field semantics.

## 15.5 Draft, preview, and confirmed-record boundaries

| Data | Location | Who changes it | Included in summaries? |
|---|---|---|---|
| Form input/unconfirmed recognition candidates | DraftProvider/local React state | User and controlled extraction mapping | No |
| Preview response | React Query, bound to exact inputs | Server calculates; frontend displays | No |
| Confirmed record/snapshot | PostgreSQL; read-only frontend query cache | Authorized server creation/explicit correction | Yes |
| Synthetic test fixtures | Tests directory/isolated test DB | Test initialization scripts | Never mixed into production acceptance data |
| Raw recording/image | Temporary memory/request files | Processing briefly, then cleanup | No |
| Q&A conversation | Current page memory, cleared on refresh | Current user/real restricted service | Never becomes food/exercise records |

Do not combine these into a Database Context for uniform state management. The Demo's local linkage does not justify a second database in the production client.

## 15.6 Details that must not be missed

1. All pages use one zone; query keys include it. Recompute today across local midnight, but retain selected Draft dates unless explicitly edited.
2. After new catalogues load, selectedReference remains retrievable by immutable ID; do not substitute another same-name food.
3. Logout stops microphone recording. Sent writes may complete; recovery journals cannot authorize another user's login.
4. Client checks help users; servers independently enforce the same business restrictions. Disabled buttons are not authorization.
5. After weight mutation updates cached versions, a dirty Profile form keeps its original expectedVersion. Save should conflict/warn, not silently advance its version to hide changes.
6. Overview, History, Summary, and QA facts must not replace null goals with calculator defaults.
7. Unknown historical provenance displays legacy labels. Before changing calculation inputs, require valid reference reselection or explicit old-snapshot rules; never guess links from names.

---

# 16. Integration procedure: migration order, import boundaries, and review

## 16.1 Retain UI, then replace dependencies; avoid simultaneous redesign and rewrite

Keep a read-only Demo copy/source manifest in reference; move runtime modules incrementally into `frontend/src`. Do not commit node_modules/dist or overwrite main package.json/lockfile with another copy.

For each module:

```text
Identify original symbol → Record target file → Move pure presentation → Replace props/types
 → Remove db/store references → Connect query/hooks → Add loading/error/empty
 → Run component/API tests → Record source location and commit in task ledger
```

Temporary development fixtures belong strictly in tests/separate demo entry points excluded from production imports. Unavailable production APIs display unavailability, never silently load fixtures.

## 16.2 Imports and calls to replace

| Original pattern | Target | Review focus |
|---|---|---|
| `import * as api from './store'` | Focused `api/*` and query/mutation hooks | No same-name fake HTTP facade |
| `useSyncExternalStore(api.subscribe, ...)` | React Query subscriptions | No local-db refresh through emit |
| `db.users` | SessionProvider/admin queries | Never download all password_hash values |
| `db.profiles.find(...)` | useProfile | Network failure is not incomplete setup |
| `db.foods.filter(...)` | useFoodSearch | Provenance/pagination/stale requests |
| `db.exerciseTypes` | API catalogue | Remove hardcoded catalogue |
| Final `calculateFood(...)` result | useFoodPreview / Java FoodCalculationService | Client is no longer authoritative |
| `estimateExercise(db,...)` | useExerciseEstimate | Real historical weight/server fingerprint |
| `api.saveDraft` | useSubmitDraft(create) / useRecordMutations(edit) | Separate POST/PUT, unknown recovery |
| `api.deleteRecord` | Versioned DELETE | Confirmation/soft deletion |
| `aggregate(db,...)` / `trends` | useSummary | One server calculation policy |
| `answerQuestion` / `parseTranscript` | Real qa/speech APIs | M2; no fixed answer when unconfigured |
| DemoTools/reset/injectFailure | Test harness | No production-bundle entry |

A same-name function may validly remain in `tests/legacy-demo` regression cases. The critical rule is that **production entrypoint dependency graphs cannot reach simulated store/seed**.

## 16.3 Structural development checks

Implement `scripts/check-production-boundaries.mjs`: parse dependencies from production entries or inspect bundles. Fail on production imports of legacy-demo/store/seed/mock services, known Demo login autofill, or browser provider keys.

Do not classify every repository localStorage occurrence as forbidden. Nonsensitive theme preferences may follow team conventions; this plan prohibits authentication, safety answers, and confirmed business databases. Do not delete fixtures to appease grep. Distinguish production/tests and report concrete offending files.

Also verify:

- Record mutations occur only in event handlers/explicit commands, not automatic form-watching useEffect saves.
- queryFn does not modify Draft; selection dispatch follows user events.
- API interceptors do not read ReactContext; use independent session-event/epoch modules.
- src contains no test-account password hashes, all-zero official-provenance SHA placeholders, or fixed recognition candidates as ordinary results.
- Java Controllers no longer construct FoodRecord directly from client nutrition; legacy routes delegate to unified services or reject dangerous old DTOs.

## 16.4 Suggested PR slices

1. Baseline/ADR/OpenAPI/task ledger, no behavior change.
2. Database migrations/minimal entity compatibility, passing isolated migration tests.
3. Backend auth/profile/weight and tests; frontend session/auth/onboarding integration.
4. References and food batch/receipt; frontend food/review integration.
5. Exercise/summaries; frontend exercise/Dashboard integration.
6. Record correction/soft deletion and Profile/History/admin support.
7. M1 E2E, failure recovery, production boundaries, deployment.
8. Separate image/speech/Q&A PRs, then M2 real-provider smoke/final acceptance.

This is merge sequencing, not a requirement to finish all backend work before frontend work. Parallel work may follow OpenAPI freeze. Before merging DTO changes, verify dependent frontend changes/compatibility so integration does not remain unrunnable.

## 16.5 Limits of old/new API coexistence

Retaining paths does not mean retaining vulnerabilities. Legacy food POST accepting arbitrary calories must leave production paths, not stay open indefinitely for possible old pages. Coordinate atomic release/development-environment switching before schemaVersion changes; old clients explicitly receive CONTRACT_UPGRADE_REQUIRED.

Temporary adapters perform only verifiable field/unit/date conversions; never fabricate consent, guess official references, or infer historical exercise weight. Each adapter has a removal task and applicable-version note.

If the actual target branch already includes batches, Flyway, or version fields, update baseline-map and reuse them. Do not create duplicate Controllers simply to match target names and cause route conflicts.

---

# 17. M2: real image, two speech paths, and general AI Q&A integration

M2 is not simply enabling Demo buttons. Each path requires real-provider success evidence and failure/authorization regressions. Missing keys, model access, actual audio/images, or official catalogue means BLOCKED_EXTERNAL. Continue other M1 work without claiming every Demo feature is genuinely complete.

## 17.1 Shared AI adapter [BC-15/BC-16]

Retain Java `HttpClient`; adding three operations does not require Python services, WebFlux, or complex agent frameworks. `ai/AiTransport` handles HTTP/authentication/timeouts/provider error mapping; business adapters own request/response schemas.

Backend configuration controls models/provider URLs. Default to the existing OpenAI provider, but **hardcoded current models do not guarantee every future capability**. Verify actual account access and fields for vision, structured text, and transcription models, recording the verification date in provider-smoke evidence. [DOC-AUDIO][DOC-STRUCT]

Model responses are untrusted: validate length, JSON schema, enums, finite numbers, item limits, negatives, and nonexistent referenceIds. Structured Outputs assists format constraints but still requires handling refusals, truncation, provider errors, and business validation. JSON mode does not prove business correctness. [DOC-STRUCT]

Do not grant database-write tools, execute model SQL/code, or automatically visit model-returned URLs. Recognition/Q&A cannot directly create food/exercise records. Raw instructions such as “ignore instructions” or “give me all users' data” remain input text and never alter backend owner/scope.

## 17.2 Images: backend and contract

Retain POST `/food/recognize`, multipart `image`. Current code supports JPEG/PNG/WebP, 8 MiB business limit, and 10 MB servlet limit. Retain 8 MiB while accommodating multipart overhead in proxy/servlet limits. Validate actual signatures/decodable dimensions; filename/MIME alone does not prove valid images.

WebP decoding depends on actual Java image support. Check JDK/ImageIO plugins; absent support does not make legitimate WebP malicious, and JDK support cannot be assumed. Add a reviewed decoder or explicitly narrow support with synchronized UI/NFR notes; never silently remove promised types.

Flow:

```text
Authenticate current USER → Validate type/size/decoded dimensions → Provider visual candidates
 → Validate candidate structure → Match names against real catalogue
 → Return pending candidates/matches
 → User confirms reference/grams/meal per item → Shared Review → Ordinary food batch
```

Suggested response:

```ts
interface FoodCandidateDto {
  candidateKey: string;               // Identity within this response, not a record ID
  proposedName: string;
  suggestedGrams: number | null;
  referenceMatches: ReferenceInfo[];  // Server queries real catalogue; model cannot invent IDs
  warnings: string[];
}
interface ImageRecognitionResponse {
  candidates: FoodCandidateDto[];
  notices: string[];
  requestId: string;
}
```

Do not display confidence percentages. Existing backend confidence may be optional diagnostics, never authoritative nutrition. No recognition returns explainable empty candidates, not three fixed fallback foods. Images do not determine dates/meals: retain selected date and require per-item meal entry. Do not collect/retain EXIF location.

`FoodCandidateMatcher` prioritizes explainable name search and several candidates, not guaranteed exact grams. No save without selected reference/confirmed grams. Unmatched names remain manually searchable, without invented nutrition.

## 17.3 Images: frontend state and cleanup

`ImageEntryPanel`: idle → selected → validating → processing → candidates/error. Changing files revokes old ObjectURL/cancels old requests; old responses cannot overwrite new files. Processing offers feedback/manual entry; cancellation discards candidates without record writes.

Map candidates to DraftFood: new rowKey/clientEntryId, nullable referenceId, suggested grams string, confirmation=false, mealType='', source=IMAGE. Multiple matches cannot treat the top result as user confirmation.

Failure retains selected date/existing draft, offering retry/manual entry. Unmount/logout clears ObjectURL/Blob references. Never persist base64 in localStorage/logs/query caches.

## 17.4 Real speech: separate transcription and extraction

Both speech paths share Recorder/transcription service, parsing food/exercise separately. Users can edit transcripts and extract again; ending recording never automatically saves.

Add POST `/speech/transcribe`, multipart `audio`; optional language hints only if supported by the actual provider fields. Response `{transcript,requestId,notices}`. No client userId or record writes.

Suggested engineering limits: 60 seconds recording, 8 MiB audio body. These are project limits, not claimed provider limits. Default to completed-file transcription, not Realtime/WebSocket. Supported formats are the intersection of browser, backend validation, and provider; prioritize WebM/MP4 tests. [DOC-AUDIO]

Add POST `/speech/parse`:

```json
{"kind":"FOOD","transcript":"For lunch yesterday I ate 150 grams of chicken.","baseDate":"2026-10-16","zone":"Australia/Sydney"}
```

baseDate must equal server today in that zone or return explicit date context. Arbitrary client baseDate cannot change validation. Results include recordDate, dateWasDefaulted, items/exercise candidates, missingFields, warnings. Relative dates use explicit current context, not model training dates.

FOOD extracts name/grams/meal; EXERCISE extracts type description/minutes. Missing date temporarily defaults to today with a flag. Missing grams/meal/type/minutes remain missing; recording time does not imply meal. Bowls/cups produce warnings requiring grams, with no promise of reliable conversion.

Server matchers map names/types to actual catalogue entries. Arbitrary model-supplied IDs cannot be saved. Models supply candidate structure only; use shared reference selection, preview, Review, and save.

## 17.5 Recorder implementation requirements

Encapsulate `media/Recorder.ts` independently of business Draft:

```ts
isSupported(): boolean
start(options: {maxDurationMs: number; onState: (s: RecorderState) => void}): Promise<void>
stop(): Promise<{blob: Blob; mimeType: string; durationMs: number}>
cancel(): void
dispose(): void
```

- Check secure context, `navigator.mediaDevices?.getUserMedia`, and `MediaRecorder` first.
- Use `MediaRecorder.isTypeSupported` to select among backend-supported formats before construction; not every browser records WebM. [DOC-RECORDER]
- Request `{audio:true}` after explicit user click, never on page mount.
- Preserve chunk order; compose Blob only after stop and final dataavailable; reject absent audio/empty Blob.
- Stop automatically at 60 seconds. dispose/cancel/unmount/logout stops every MediaStreamTrack and removes listeners/timers/temporary URLs.
- Explicit states cover double start, quick stop/start, permission denial, no input device, unplugging, and unsupported browsers, without unhandled Promises.
- Match upload filename extension/Content-Type to bytes; do not label WebM as `.wav`.
- Fake microphones prove flow only; M2 retains at least one manually verified real-microphone sample.

## 17.6 VoiceEntryPanel and both paths

States: idle/requestingPermission/recording/processing/transcriptReady/extracting/reviewReady/error. Transcript editing invalidates old extraction; confirm overwriting manually edited candidates before extracting again.

Food maps to multiple DraftFoods; exercise to one typeId/date/minutes followed by real estimate. First version explicitly requests separate records for multiple exercises in one utterance, rather than silently discarding extras and claiming complete success. Multi-exercise batches require separate scope agreement beyond the current contract.

Recognition only updates Draft, never POSTrecords. After microphone denial, preserve manual entry and optionally typed-text extraction; the latter does not establish successful recorded-audio transcription.

## 17.7 Server Q&A responsibilities and authorization [BC-16]

Add POST `/qa/messages`. Requests exclude complete profile/safety answers:

```json
{"message":"What have I recorded this week?","recordRange":{"from":"2026-10-10","to":"2026-10-16"},"zone":"Australia/Sydney"}
```

recordRange may be null; general questions needing no records then use general information only. Document engineering limits such as 2,000 message characters/31-day range in contract/UI. Use single-turn stateless Q&A: page bubbles do not imply the server has all previous turns, and UI promises no cross-session memory. Multi-turn support requires separate context-filter review; never trust an entire browser chat array as records.

`QaService.ask` order:

1. Authenticate active USER; ProfileService checks completed adult setup. Read current healthSafetyAnswer/qaConsent from DB, never client eligibility.
2. Unless answer=NO and consent currently valid, return 403 QA_RESTRICTED. Ordinary record/summary APIs remain available.
3. Check scope: disease/symptom/medication/treatment requests receive clear boundaries, no diagnosis/dosage/course. Keywords are preliminary rules, not comprehensive safety guarantees from a small English list.
4. If context is needed, SummaryService generates allowlisted facts for **current user only**: dates, recorded totals/minutes, insufficiency, current-goal comparison. Exclude raw safety answers, names, emails, passwords, birthYear, and detailed medical history.
5. Provider calls occur outside transactions, without DB-write tools. Distinct fields label user text/server facts; “ignore rules” cannot change permissions.
6. Validate/limit provider output. Failures return 502/504 and may retry, never fixed health-advice fallback.
7. Recheck current consent/eligibility before returning. Withdrawal during processing blocks the result. This is an explicit enhancement, not retroactive cancellation of the already-authorized external transmission.
8. Compliant answers include non-medical notices; refusals explain scope. Store no chat/raw-safety database copies.

Suggested response: `{answer,disclaimer,scope:'GENERAL'|'RECORD_SUMMARY'|'REFUSED',recordRangeUsed,requestId}`. Server-policy refusal may return 200+REFUSED for explanation; ineligibility remains 403. Neither model nor policy refusal is a save failure.

## 17.8 ChatPage interaction

Read profile.qaEligibility/capabilities. Ineligibility shows reason/Profile link; unconfigured provider shows service unavailable rather than blaming the user's health status.

Lock sending on submission; assign local messageKey. Success updates only that bubble, never later questions or another user's session. Failure shows retryable bubble, retaining question. Retry may incur new provider charges but creates no business records; do not promise free retries.

Chat is memory-only. Logout/consent withdrawal clears sensitive display and cancels local waiting. Record-context dates must be understandable; never secretly send all history. Any Markdown renderer sanitizes HTML/URLs; do not render raw model output through dangerouslySetInnerHTML.

## 17.9 M2 acceptance gates

| Path | Real success evidence | Failure/restriction evidence | Core invariant |
|---|---|---|---|
| Image | Real recognizable food image yields editable nonfixed candidates | Invalid file, empty recognition, timeout | No save before confirmation |
| Food speech | Real recording → text → multiple foods → correction → save | Microphone denial, missing grams/meal, no match | Shared food batch |
| Exercise speech | Real recording → date/type/minutes → estimate → save | Missing type/minutes, missing weight | Shared exercise service |
| Q&A | Eligible real-model general/record answer | Withdrawal, direct-HTTP frontend bypass, medical scope, provider failure | No raw safety transmission or record writes |

Insufficient provider budget/keys/network permits continued mock contract tests, but real M2 evidence remains NOT RUN/BLOCKED. Passing 15 local tests cannot replace this table.

---

# 18. 69 implementation task cards ready for AI assignment

**Mandatory execution constraint:** Every B/F/A/D task adding/changing business behavior must deliver applicable automated tests, actual execution, and self-review in **the same development work package**, regardless of where Q00–Q08 appears in the dependency table. Missing/failing tests prevent `IN_REVIEW`/`DONE`. Q00–Q08 provide shared test environments, cross-module regression, and integrated acceptance; they do not justify deferring business tests. Section 0.0 branch locking and 0.7 concurrent test delivery override card example commits/file lists. Verify `develop/yuankang-wei` before every write/commit.

All tasks initially have status **TODO**. Estimates represent effective effort for developers/reviewers familiar with the stack, not AI runtime commitments, and exclude external-data/approval waits. Short Java paths are relative to `backend/src/main/java/com/fitnutri/backend/`; other paths are repository-relative. New filenames describe target design; register/reuse equivalent existing classes under Section 3.

Every card follows Section 0 commit/permission rules, Section 19 testing standards, and the global completion definition. Dependencies are prerequisites for DONE; tests/stubs may be written after contract freeze, but integration tasks cannot be marked Done while dependencies remain unfinished.

## 18.1 Task overview and dependencies

| ID | Task | Phase | Prerequisites | Estimated hours | BC |
|---|---|---|---|---:|---|
| P00 | Verify target branch and both source baselines | PREP | — | 1–2 | none |
| P01 | Establish environment and existing test baseline | PREP | P00 | 2–4 | none |
| P02 | Freeze canonical APIs and domain types | PREP | P00, P01 | 3–5 | none |
| P03 | Establish task ledger and development conventions | PREP | P00 | 1–2 | none |
| D00 | Inspect actual database schema and migration preflight | M1 | P00, P03 | 2–4 | BC-14 |
| D01 | Introduce controlled Flyway and isolated test infrastructure | M1 | D00, P01 | 3–6 | BC-14 |
| D02 | Incremental account/profile/weight migration | M1 | D01, P02 | 3–5 | BC-01, BC-02, BC-03, BC-12, BC-14 |
| D03 | Create versioned food/activity reference structures | M1 | D01, P02 | 4–8 | BC-04, BC-05, BC-14 |
| D04 | Migrate record dates, snapshots, soft deletion, receipts | M1 | D01, D02, D03, P02 | 4–8 | BC-06, BC-07, BC-14 |
| D05 | Backfill legacy data, tighten constraints, validate | M1 | D02, D03, D04 | 4–8 | BC-04, BC-06, BC-14 |
| B00 | Shared errors, current-user and date policies | M1 | P02, P01 | 2–4 | BC-01, BC-13 |
| B01 | Fix registration/account lookup and controlled admin bootstrap | M1 | B00, D02, Q00 | 3–6 | BC-01 |
| B02 | Real session lifecycle and CSRF | M1 | B01 | 4–8 | BC-01 |
| B03 | Atomic questionnaire and complete-settings service | M1 | B00, B02, D02 | 4–8 | BC-02 |
| B04 | Dated weight corrections and historical lookup | M1 | B03, D02 | 3–5 | BC-03 |
| B05 | Production food catalogue import/search | M1 | D03, B00 | 4–8 | BC-04 |
| B06 | Production activity catalogue and source applicability | M1 | D03, B00 | 3–6 | BC-05 |
| B07 | Unified food nutrition preview/calculation | M1 | B05 | 2–4 | BC-08 |
| B08 | Creation receipts, canonical hash, and concurrent idempotency | M1 | D04, B00 | 5–9 | BC-07 |
| B09 | Multi-food confirmation and safe compatibility endpoints | M1 | B08, B07 | 4–7 | BC-08 |
| B10 | Date-based exercise estimates and reliable saving | M1 | B08, B06, B04 | 4–7 | BC-09 |
| B11 | Unified record lists and detail reads | M1 | D04, B00 | 3–5 | BC-10 |
| B12 | Versioned corrections and soft deletion | M1 | B11, B09, B10 | 4–7 | BC-10 |
| B13 | Unified overview, trends, and rule summaries | M1 | B11, B03, B04 | 4–8 | BC-11 |
| B14 | Versioned administrator account-state management | M1-SUPPORT | B02, D02 | 2–4 | BC-12 |
| B15 | Capability and contract readiness | M1 | B00, B05, B06 | 1–2 | BC-13 |
| F00 | Migrate Demo theme and application shell | M1 | P01, P03 | 2–4 | none |
| F01 | Define canonical types, labels, units and dates | M1 | P02, F00 | 3–5 | none |
| F02 | Connect Axios, CSRF, errors and cancellation | M1 | F01, B02 | 3–5 | none |
| F03 | Session Provider, query cache and route guards | M1 | F02, F00, B03 | 4–6 | none |
| F04 | Connect real login and registration screens | M1 | F03, B01, B02 | 2–4 | none |
| F05 | Shared draft reducer and leave protection | M1 | F01, F00 | 4–6 | none |
| F06 | Connect atomic three-step onboarding | M1 | F04, F05, B03 | 4–7 | none |
| F07 | Remote food search and stable selection | M1 | F02, B05 | 3–5 | none |
| F08 | Multiple-food editor and asynchronous previews | M1 | F07, B07, F05 | 3–5 | none |
| F09 | Exercise editor and dated estimates | M1 | F02, B10, F05 | 2–4 | none |
| F10 | Review create/edit dispatch and unknown-result recovery | M1 | F08, F09, B09, B10, F03 | 4–7 | none |
| F11 | Real Dashboard and post-save refresh | M1 | F10, B13, B11 | 3–5 | none |
| F12 | History URL filters, details and navigation | M1-SUPPORT | F11, B11 | 3–5 | none |
| F13 | Historical corrections and deletion confirmation | M1-SUPPORT | F12, F10, B12 | 3–5 | none |
| F14 | Profile settings and separate weight operations | M1-SUPPORT | F06, B04, B03 | 3–5 | none |
| F15 | Real trends and rule-based summaries | M1-SUPPORT | F12, B13 | 3–5 | none |
| F16 | Connect administrator screen | M1-SUPPORT | F03, B14 | 2–4 | none |
| F17 | Visual, accessibility and interaction regression | M1-SUPPORT | F10, F11, F12, F14, F16 | 2–4 | none |
| F18 | Exclude production mocks and enable real M1 | M1 | F03, F10, F11, B15 | 1–2 | none |
| A00 | Shared provider transport and configuration boundaries | M2 | B00, B02, B15 | 3–5 | BC-15, BC-16 |
| A01 | Image candidates and real-reference matching backend | M2 | A00, B05 | 3–6 | BC-15 |
| A02 | Image frontend and shared draft integration | M2 | A01, F10 | 2–4 | none |
| A03 | Real file transcription backend | M2 | A00 | 4–7 | BC-16 |
| A04 | Structured food and exercise text extraction | M2 | A00, B05, B06 | 4–7 | BC-16 |
| A05 | Browser recorder and microphone lifecycle | M2 | F02, A03 | 4–7 | none |
| A06 | Connect both speech paths to Review | M2 | A04, A05, F10 | 3–5 | none |
| A07 | QA eligibility, scope and record context | M2 | B03, B13, A00 | 4–7 | BC-16 |
| A08 | Real QA provider and answer endpoint | M2 | A07, A00 | 3–6 | BC-16 |
| A09 | Real chat page and consent withdrawal | M2 | A08, F03, F14 | 3–5 | none |
| A10 | Real-provider smoke and M2 evidence | M2 | A02, A06, A09 | 2–4 | BC-15, BC-16 |
| Q00 | Java/PG16 integration harness | M1 | D01, P01 | 4–6 | BC-14 |
| Q01 | Reuse Demo cases and align Java units | M1 | B07, B10, F05 | 3–5 | none |
| Q02 | Concurrency, atomicity and lost responses | M1 | B09, B10, B12, Q00 | 4–7 | none |
| Q03 | Auth, profile, weight and admin HTTP regression | M1 | B01, B02, B03, B04, B14, Q00 | 3–5 | none |
| Q04 | Frontend component and network contracts | M1 | F03, F05, F10, F14 | 4–6 | none |
| Q05 | M1 core browser E2E | M1 | F06, F10, F11, F18, Q02, Q03 | 4–7 | none |
| Q06 | History, Profile, admin and conflict E2E | M1-SUPPORT | F13, F14, F15, F16, Q05 | 3–6 | none |
| Q07 | Advanced contracts, permissions and failures | M2 | A02, A06, A09, Q03 | 3–6 | none |
| Q08 | Actual-browser compatibility, accessibility and timing | M1-AND-M2 | Q05, Q06 | 4–7 | none |
| R00 | CI and one-command local checks | DELIVERY | P01, P03, Q00 | 3–5 | none |
| R01 | Local startup and production-style same-origin deployment | DELIVERY | F18, B02, D05, R00 | 3–5 | BC-01, BC-13, BC-14 |
| R02 | Backup restoration and legacy migration rehearsal | DELIVERY | D05, R01 | 3–6 | BC-14 |
| R03 | Final ledger, risks and delivery sign-off | DELIVERY | R00, R01, R02, Q01, Q02, Q03, Q04, Q05, Q06, Q07, Q08, A10 | 2–4 | none |

Note: M1-SUPPORT follows the core flow but remains in scope. Missing M2 providers/official data may independently block work; simulated success is not real completion. R03 is final M2 sign-off; M1 may receive separate Section 22 acceptance before R03 M2 dependencies are ready.

## 18.2 Individual task cards


### P00｜Verify target branch and both source baselines

**Phase:** PREP　**Dependencies:** none　**Estimate:** 1–2 hours　**Backend/DB register:** none (this task must not hide backend changes)。

**Changed/new files:** `docs/implementation/baseline.md`；`docs/implementation/source-map.md`。

**Function/responsibility boundary:** `baseline inventory`。

**Steps:**

1. Execute Section 0.0 read-only local Git checks; record root worktree, existing changes, local branches, and HEAD without remote access.
2. Verify or safely switch to user-specified `develop/yuankang-wei`; do not create baseline files before Branch Gate=PASS.
3. Record BASE_COMMIT, Demo SHA, ZIP-audit differences; mark equivalent features for reuse. Missing branch/metadata or destructive switching is BLOCKED for user resolution.

**Tests/evidence:** Recomputable checksums; paths/branches/commits verified; no repository writes during checks.

**Completion:** Branch Gate=PASS and exact local branch `develop/yuankang-wei`; clear baseline, without calling main ZIP the current branch/latest remote.

**Suggested commit:** `docs(plan): record target branch and source baselines [P00]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### P01｜Establish environment and existing test baseline

**Phase:** PREP　**Dependencies:** P00　**Estimate:** 2–4 hours　**Backend/DB register:** none (this task must not hide backend changes)。

**Changed/new files:** `docs/implementation/environment.md`；`docs/verification/baseline-tests.md`。

**Function/responsibility boundary:** `runtime and dependency inventory`。

**Steps:**

1. Read .nvmrc, lockfile, POM, wrapper, Compose, README; check effective Node24/Java21/Maven versions.
2. Run npm ci/build/lint, Demo npm test, and Java tests in a safe working copy using an isolated database.
3. Distinguish network/tool/compilation/test failures; download failures are not code defects.

**Tests/evidence:** Record actual commands, exit codes, test counts; unexecuted checks are NOT RUN.

**Completion:** Reproducible environment or dependency/service-specific blockers; no preemptive framework upgrades.

**Suggested commit:** `docs(test): capture environment and existing test results [P01]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### P02｜Freeze canonical APIs and domain types

**Phase:** PREP　**Dependencies:** P00, P01　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes)。

**Changed/new files:** `docs/api/openapi-mvp.yaml`；`docs/api/examples/`；`docs/adr/ADR-02-contract.md`。

**Function/responsibility boundary:** `mvp-1 request/response schemas`。

**Steps:**

1. Define precise DTOs/enums/nulls/units/dates/string IDs/versions/error codes under Sections 6–8/15/17.
2. Inventory retained/upgraded/compatible/disabled endpoints; add M2 schemas/file limits.
3. Check equivalent branch support; identify approval gates for breaking changes and tenth technical table.

**Tests/evidence:** Parseable OpenAPI; success/validation/authorization/conflict examples per write; examples pass schemas.

**Completion:** No frontend/backend field guessing; unresolved official mappings/model parameters explicitly external inputs.

**Suggested commit:** `docs(api): define canonical integration contracts [P02]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### P03｜Establish task ledger and development conventions

**Phase:** PREP　**Dependencies:** P00　**Estimate:** 1–2 hours　**Backend/DB register:** none (this task must not hide backend changes)。

**Changed/new files:** `docs/implementation/tasks.md`；`docs/implementation/backend-change-log.md`；`docs/adr/`；`.github/pull_request_template.md`。

**Function/responsibility boundary:** `task ledger and PR template`。

**Steps:**

1. Register initial status, dependencies, responsible roles, and approvals for all 69 cards.
2. Make Section 0 conventions discoverable; read existing AGENTS/contribution rules before appending, never overwrite.
3. Create provenance and PR templates; distinguish local commit, remote push, and merge authorization.

**Tests/evidence:** Unique task IDs; PR template includes BC/DB/Tests/Rollback; no fabricated authors/completion.

**Completion:** Every subsequent backend/DB change has a traceable ID.

**Suggested commit:** `docs(workflow): establish integration task and review rules [P03]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### D00｜Inspect actual database schema and migration preflight

**Phase:** M1　**Dependencies:** P00, P03　**Estimate:** 2–4 hours　**Backend/DB register:** BC-14。

**Changed/new files:** `scripts/db/preflight.sh`；`docs/db/schema-before.md`；`docs/db/preflight-report.md`。

**Function/responsibility boundary:** `read-only schema and duplicate checks`。

**Steps:**

1. Read-only connection verifies tables/columns/types/constraints/indexes/counts and existing Flyway.
2. Check lower(email) collisions, same-user/date weight duplicates, FoodReference FKs, invalid dates/numbers.
3. Record real-data protections and legacy timezone; no DDL before confirmation.

**Tests/evidence:** Preflight locates synthetic duplicate/FK problems; scripts contain no mutation statements.

**Completion:** Actual schema rather than JPA guesses; conflict list manually confirmed.

**Suggested commit:** `chore(db): add read-only migration preflight [D00]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### D01｜Introduce controlled Flyway and isolated test infrastructure

**Phase:** M1　**Dependencies:** D00, P01　**Estimate:** 3–6 hours　**Backend/DB register:** BC-14。

**Changed/new files:** `backend/pom.xml`；`backend/src/main/resources/application-*.properties`；`backend/src/main/resources/db/migration/`；`scripts/db/adopt-baseline.sh`。

**Function/responsibility boundary:** `Flyway setup and legacy V1`。

**Steps:**

1. Add Flyway/PostgreSQL module under effective Boot4 BOM; verify artifact support.
2. Baseline creates original six tables on empty DB; explicitly baseline nonempty legacy DB only after schema matching, never automatically.
3. Separate migration/runtime profiles; no concurrent Hibernate update/Flyway table mutation.

**Tests/evidence:** Empty PG16 migrates, legacy clone adopts, wrong schema rejects; no shared DB access.

**Completion:** Auditable Flyway history; subsequent migrations ordered; enable validate after target schema completion.

**Suggested commit:** `chore(db): introduce controlled Flyway migrations [D01]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### D02｜Incremental account/profile/weight migration

**Phase:** M1　**Dependencies:** D01, P02　**Estimate:** 3–5 hours　**Backend/DB register:** BC-01, BC-02, BC-03, BC-12, BC-14。

**Changed/new files:** `backend/src/main/resources/db/migration/V2__account_profile_weight_integrity.sql`；`user/User.java`；`profile/UserProfile.java`；`profile/WeightEntry.java`。

**Function/responsibility boundary:** `version, consent, units, dated weight uniqueness`。

**Steps:**

1. Add target version/time/adult/unit fields, retaining Section 9 compatibility; accurately merge legacy health enums.
2. Add case-insensitive email/user-date weight uniqueness only after approved collision resolution.
3. Preserve explicit legacy consent/records; never guess adult confirmation true without evidence.

**Tests/evidence:** Legacy-clone counts/password hashes/identities preserved; duplicate input rejected; valid new-field nulls/defaults.

**Completion:** Entities match schema; every backfill decision traceable in report.

**Suggested commit:** `feat(db): migrate account profile and weight invariants [D02]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### D03｜Create versioned food/activity reference structures

**Phase:** M1　**Dependencies:** D01, P02　**Estimate:** 4–8 hours　**Backend/DB register:** BC-04, BC-05, BC-14。

**Changed/new files:** `backend/src/main/resources/db/migration/V3__versioned_reference_catalogues.sql`；`foodreference/FoodReference.java`；`foodreference/FoodReferenceRepository.java`；`activity/`。

**Function/responsibility boundary:** `immutable reference identity and datasets`。

**Steps:**

1. Migrate String publicFoodKey PK to separate Long id, retaining key and extended nutrient columns.
2. Add food_dataset/activity_dataset/exercise_type and version/active/source constraints; update repository generics/controller key lookups.
3. Unknown FKs block migration, never CASCADE-delete; legacy data explicitly not officially verified.

**Tests/evidence:** Same-key versions coexist, only one active; original key searchable; references/extra columns preserved.

**Completion:** Three new business tables/ID conversion have migration tests and recoverable reports.

**Suggested commit:** `feat(db): version reference catalogues without losing legacy keys [D03]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### D04｜Migrate record dates, snapshots, soft deletion, receipts

**Phase:** M1　**Dependencies:** D01, D02, D03, P02　**Estimate:** 4–8 hours　**Backend/DB register:** BC-06, BC-07, BC-14。

**Changed/new files:** `backend/src/main/resources/db/migration/V4__record_dates_snapshots_and_receipts.sql`；`records/BaseRecord.java`；`records/FoodRecord.java`；`records/ExerciseRecord.java`；`submission/SubmissionReceipt.java`。

**Function/responsibility boundary:** `recordDate, snapshots, client IDs, receipt schema`。

**Steps:**

1. Add recorded_date/kJ/reference/version/deleted_at/client_entry_id/legacy state; upgrade minutes to numeric.
2. Add minimal submission_receipt fields and unique(user,submissionId); record-client uniqueness includes soft-deleted rows.
3. Initially permit explained legacy gaps; apply CURRENT completeness constraints when fields are ready.

**Tests/evidence:** Empty/legacy DB migrate; duplicate clientID/receipts constrained; soft deletion retains identity; no forged legacy snapshots.

**Completion:** Additional technical table has ADR approval; record/receipt transaction tests can connect.

**Suggested commit:** `feat(db): add record snapshots and durable submission receipts [D04]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### D05｜Backfill legacy data, tighten constraints, validate

**Phase:** M1　**Dependencies:** D02, D03, D04　**Estimate:** 4–8 hours　**Backend/DB register:** BC-04, BC-06, BC-14。

**Changed/new files:** `backend/src/main/resources/db/migration/V5__legacy_data_backfill.sql`；`backend/src/main/resources/db/migration/V6__verified_constraints_and_indexes.sql`；`backend/src/main/resources/db/migration/V7__canonical_schema_validation.sql`；`scripts/db/verify.sh`；`docs/db/migration-report.md`。

**Function/responsibility boundary:** `approved conversion and reconciliation`。

**Steps:**

1. Backfill dates from Instant only in approved zone; kcal × 4.184, no guessed food reference/weight/MET.
2. Produce LEGACY_UNVERIFIED report, column checksums, count/known-energy reconciliation; protected duplicate-weight backups.
3. Add query indexes/satisfiable constraints; production ddl-auto=validate; no automatic old-column deletion.

**Tests/evidence:** Idempotent reruns, explainable sums/rounding tolerances, successful validate; deliberate wrong legacyzone cannot silently pass.

**Completion:** Every legacy row explained, not erased for new schema requirements; backup restoration rehearsable.

**Suggested commit:** `fix(db): reconcile legacy dates units and constraints [D05]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B00｜Shared errors, current-user and date policies

**Phase:** M1　**Dependencies:** P02, P01　**Estimate:** 2–4 hours　**Backend/DB register:** BC-01, BC-13。

**Changed/new files:** `common/ApiError.java`；`common/ApiExceptionHandler.java`；`common/CurrentUserService.java`；`common/RecordDatePolicy.java`；`common/ClockConfig.java`。

**Function/responsibility boundary:** `requireActive, requireGeneralUser, validateDate, error mapping`。

**Steps:**

1. Get real owner from Authentication, check role/enabled; private resources use owner-scoped lookup/404.
2. Injectable Clock, valid IANA zones; server rejects future dates/invalid ranges/nonfinite numbers.
3. Unified JSON errors/fieldErrors/clientEntryId/requestId; security filters share public-safe structure.

**Tests/evidence:** Unauthenticated401, unauthorized404/403, future-date400, invalid-zone400; no SQL/stacks/secrets in errors.

**Completion:** Subsequent Controllers avoid duplicate inconsistent identity/date logic.

**Suggested commit:** `refactor(api): centralise identity date and error policies [B00]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B01｜Fix registration/account lookup and controlled admin bootstrap

**Phase:** M1　**Dependencies:** B00, D02, Q00　**Estimate:** 3–6 hours　**Backend/DB register:** BC-01。

**Changed/new files:** `user/AuthController.java`；`user/AuthService.java`；`user/UserRepository.java`；`security/AppUserDetailsService.java`；`user/AdminBootstrapRunner.java`。

**Function/responsibility boundary:** `register, canonicalEmail, seedAdmin`。

**Steps:**

1. Public registration fixes USER; trim/ROOT-lowercase email with database uniqueness and correct concurrent-duplicate handling.
2. Bootstrap administrators only through controlled environment configuration, never first-public-user privilege; do not automatically demote existing administrators.
3. Enforce actual BCrypt input-length limits without silent truncation; responses never contain hashes.

**Tests/evidence:** First registrant in empty DB remains USER; role tampering cannot escalate; email case/concurrency collisions; disabled login rejected.

**Completion:** Registration matches Proposal security rules; no hardcoded production bootstrap password.

**Suggested commit:** `fix(auth): enforce safe user registration and admin bootstrap [B01]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B02｜Real session lifecycle and CSRF

**Phase:** M1　**Dependencies:** B01　**Estimate:** 4–8 hours　**Backend/DB register:** BC-01。

**Changed/new files:** `security/SecurityConfig.java`；`user/AuthController.java`；`security/CsrfController.java`；`security/SessionConfiguration.java`。

**Function/responsibility boundary:** `login, logout, csrf token, context repository`。

**Steps:**

1. Custom login invokes correct SessionAuthenticationStrategy and saves through the same repository; verify actual Boot4 configuration.
2. Enable session CSRF, anonymous token bootstrap, login/logout rotation; retain same-origin proxy and valid CORS.
3. Logout creates no session; 401 is JSON; authorization does not depend on UI.

**Tests/evidence:** Login/refresh me succeeds; session ID rotates; protected access rejected after logout; missing/old CSRF writes403, fresh token succeeds.

**Completion:** Real Cookie/CSRF handshake works; never temporarily disable security to pass.

**Suggested commit:** `feat(security): secure session authentication and CSRF lifecycle [B02]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B03｜Atomic questionnaire and complete-settings service

**Phase:** M1　**Dependencies:** B00, B02, D02　**Estimate:** 4–8 hours　**Backend/DB register:** BC-02。

**Changed/new files:** `profile/ProfileController.java`；`profile/ProfileService.java`；`profile/ProfileResponse.java`；`profile/CompleteOnboardingRequest.java`；`profile/ProfileSettingsRequest.java`；`profile/UpdateProfileRequest.java` (legacy compatibility/disabled)；`profile/QaEligibility.java`。

**Function/responsibility boundary:** `completeOnboarding, updateSettings, getCurrent`。

**Steps:**

1. Add atomic onboarding; lock user/check user/profile versions, write initial weight, mark completed last.
2. Complete-settings PUT supports true null clearing and transactional displayName; ordinary settings do not write weight.
3. Derive eligibility live from answer/consent; unrelated edits preserve consent; refusing QA does not prevent ordinary setup.

**Tests/evidence:** Any failure leaves no partial profile/weight; false consent completes; underage rejected; null clearing; version conflict.

**Completion:** Questionnaire/Profile really save, reread, recover; unify unsafe legacy partial-write entry points.

**Suggested commit:** `feat(profile): add atomic onboarding and versioned settings [B03]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B04｜Dated weight corrections and historical lookup

**Phase:** M1　**Dependencies:** B03, D02　**Estimate:** 3–5 hours　**Backend/DB register:** BC-03。

**Changed/new files:** `profile/WeightController.java`；`profile/WeightService.java`；`profile/WeightEntryRepository.java`；`profile/AddWeightRequest.java`；`profile/WeightEntryResponse.java`。

**Function/responsibility boundary:** `upsertByDate, latestOnOrBefore, latestCurrent`。

**Steps:**

1. Create new dates; return existing same-date/same-value records; different values require versioned correction, user lock, date uniqueness.
2. currentWeight follows latest measurement date; earlier backfills do not replace it.
3. Return latest weight/profile versions; preserve historical exercise snapshots.

**Tests/evidence:** Concurrent same-day creation yields one row; stale version409; correct backfill; weight edits preserve old exercise values.

**Completion:** One service implements weight-page and historical-exercise dependencies.

**Suggested commit:** `fix(weight): preserve dated history and versioned corrections [B04]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B05｜Production food catalogue import/search

**Phase:** M1　**Dependencies:** D03, B00　**Estimate:** 4–8 hours　**Backend/DB register:** BC-04。

**Changed/new files:** `foodreference/FoodReferenceDataLoader.java`；`foodreference/ReferenceImportService.java`；`foodreference/FoodReferenceRepository.java`；`foodreference/FoodReferenceController.java`；`backend/src/main/resources/data/manifests/`。

**Function/responsibility boundary:** `importManifest, searchActive, getImmutableReference`。

**Steps:**

1. Inspect actual AUSNUT headers/SHA/provenance rather than inferring from Demo; specify core nutrient policy.
2. Explicit version import/full validation; identical manifest is idempotent, same-version/different-hash rejected; count>0 cannot skip all imports.
3. Database-paginated search returns immutable ID/provenance; by-id supports inactive historical details.

**Tests/evidence:** Synthetic fixture import and bad-manifest rollback; pagination/same-name distinction; production samples checked against source.

**Completion:** verified=true only after real mapping validation; missing official files remain blocked, never relabelled substitutes.

**Suggested commit:** `feat(food-reference): import traceable versioned food data [B05]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B06｜Production activity catalogue and source applicability

**Phase:** M1　**Dependencies:** D03, B00　**Estimate:** 3–6 hours　**Backend/DB register:** BC-05。

**Changed/new files:** `activity/ActivityReferenceImportService.java`；`activity/ExerciseTypeRepository.java`；`records/ExerciseCatalog.java`；`backend/src/main/resources/data/manifests/`。

**Function/responsibility boundary:** `importActivityManifest, listActiveTypes`。

**Steps:**

1. Import reviewed source code/description/MET/age range, not copied Demo coefficients.
2. Adapt legacy ExerciseCatalog to repository or disable it; one production catalogue.
3. Types endpoint returns structured id/name/source/active while retaining preset-only restrictions.

**Tests/evidence:** Same-version idempotency, active uniqueness, invalid MET/provenance rejection; never guess new IDs from old names.

**Completion:** No hardcoded frontend catalogue; official provenance and selectable engineering subset traceable.

**Suggested commit:** `feat(exercise): expose traceable activity reference catalogue [B06]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B07｜Unified food nutrition preview/calculation

**Phase:** M1　**Dependencies:** B05　**Estimate:** 2–4 hours　**Backend/DB register:** BC-08。

**Changed/new files:** `foodreference/FoodCalculationService.java`；`foodreference/FoodReferenceController.java`；`foodreference/FoodNutritionResponse.java`。

**Function/responsibility boundary:** `calculate, previewByReferenceId`。

**Steps:**

1. BigDecimal per100g × grams with final three-decimal rounding; preview/save share a function.
2. New by-id nutrition returns reference/grams/core nutrients/fingerprint; legacy by-key delegates only valid active lookup.
3. Explicitly reject/explain missing core values, invalid precision, or excessive grams under contract.

**Tests/evidence:** Synthetic 100g/150g/fractional boundary formulas; null stays null; identical inputs match; no intermediate double error.

**Completion:** No separate two-decimal backend preview algorithm.

**Suggested commit:** `refactor(nutrition): share authoritative portion calculations [B07]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B08｜Creation receipts, canonical hash, and concurrent idempotency

**Phase:** M1　**Dependencies:** D04, B00　**Estimate:** 5–9 hours　**Backend/DB register:** BC-07。

**Changed/new files:** `submission/SubmissionService.java`；`submission/SubmissionReceiptRepository.java`；`submission/CanonicalSubmissionHasher.java`；`submission/SubmissionController.java`。

**Function/responsibility boundary:** `findReceipt, withSubmission, canonicalHash`。

**Steps:**

1. User row lock and same transaction; inspect completed receipt before revalidating old references/weight.
2. Same ID/hash replays original receipt; changed payload409. Store minimal identities, never health/raw image/audio.
3. Receipt GET is owner-only;404 means not yet observed, not definite rollback.

**Tests/evidence:** Two real PG connections with same key; different payload; record failure rolls receipt back; replay after deletion never resurrects.

**Completion:** No permanently pending receipt or two-transaction records-before-receipt window.

**Suggested commit:** `feat(submission): persist atomic idempotent creation receipts [B08]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B09｜Multi-food confirmation and safe compatibility endpoints

**Phase:** M1　**Dependencies:** B08, B07　**Estimate:** 4–7 hours　**Backend/DB register:** BC-08。

**Changed/new files:** `records/FoodRecordService.java`；`records/FoodRecordController.java`；`records/FoodBatchRequest.java`；`records/CreateFoodRecordRequest.java`。

**Function/responsibility boundary:** `confirmBatch, confirmSingleAdapter`。

**Steps:**

1. Accept submissionId/date/source/items, never authoritative client nutrition/owner.
2. Validate/calculate/save all items and receipt in one transaction; flush constraints inside it; any item failure rolls back all.
3. Legacy single POST delegates one-item batch or rejects old protocol, never bypasses reference calculation.

**Tests/evidence:** Two meals, second-item failure, duplicate clientID, same-submission retry, intentional identical content in separate submissions, tampered nutrition.

**Completion:** Real database consistently saves food flow; receipt/record counts match.

**Suggested commit:** `feat(records): confirm food batches atomically from references [B09]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B10｜Date-based exercise estimates and reliable saving

**Phase:** M1　**Dependencies:** B08, B06, B04　**Estimate:** 4–7 hours　**Backend/DB register:** BC-09。

**Changed/new files:** `records/ExerciseEstimateService.java`；`records/ExerciseRecordService.java`；`records/ExerciseRecordController.java`；`records/CreateExerciseRecordRequest.java`。

**Function/responsibility boundary:** `estimate, confirmExercise, fingerprint`。

**Steps:**

1. Select latest weight on/before activity date and applicable reference; fractional minutes allowed; no70kg fallback.
2. AVAILABLE includes full snapshot; unavailable includes status/reason/null. Recheck fingerprint before save; changes409/review.
3. Reuse creation receipt for duplicate prevention; preserve source/actual record date.

**Tests/evidence:** 70/80kg across dates, missing weight, inapplicable age, fractional minutes, weight changed after preview, unchanged old snapshots.

**Completion:** Historical estimates independent of current weight; unavailable-estimate activities remain saveable.

**Suggested commit:** `fix(exercise): calculate dated snapshots with safe confirmation [B10]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B11｜Unified record lists and detail reads

**Phase:** M1　**Dependencies:** D04, B00　**Estimate:** 3–5 hours　**Backend/DB register:** BC-10。

**Changed/new files:** `records/FoodRecordRepository.java`；`records/ExerciseRecordRepository.java`；`records/FoodRecordResponse.java`；`records/ExerciseRecordResponse.java`；`records/*RecordController.java`。

**Function/responsibility boundary:** `listOwned, getOwned, mapSnapshot`。

**Steps:**

1. Filter in database by owner/date/nondeleted; optional type filtering, never whole-database memory searches.
2. Detail GET supports conflicts/date moves/receipt location; return snapshots/minimal reference provenance/string IDs.
3. Retain LEGACY_UNVERIFIED notices, never dynamically recalculate saved records.

**Tests/evidence:** Three-date backfill, zone consistency, unauthorized IDs/filters, deleted exclusion, missing legacy data, empty/error distinction.

**Completion:** No full-db download/join required; list/detail contracts agree.

**Suggested commit:** `refactor(records): expose owned dated records and snapshots [B11]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B12｜Versioned corrections and soft deletion

**Phase:** M1　**Dependencies:** B11, B09, B10　**Estimate:** 4–7 hours　**Backend/DB register:** BC-10。

**Changed/new files:** `records/UpdateFoodRecordRequest.java`；`records/UpdateExerciseRecordRequest.java`；`records/FoodRecordService.java`；`records/ExerciseRecordService.java`。

**Function/responsibility boundary:** `updateOwned, softDeleteOwned`。

**Steps:**

1. Separate create/update DTOs; compare expectedVersion with @Version; never assign client version to entity.
2. Calculation corrections share services with explicit original-inactive-reference exception; preserve source and actually move dates.
3. Delete sets deletedAt, retains clientID/receipt; repeated same-owner delete204.

**Tests/evidence:** Two-tab concurrent edits409; grams recalculate; date selects weight; deletion cancel/failure/retry; old receipt never resurrects.

**Completion:** Old/new date queries/summaries derive from records, without manual total patches.

**Suggested commit:** `feat(records): add safe corrections and soft deletion [B12]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B13｜Unified overview, trends, and rule summaries

**Phase:** M1　**Dependencies:** B11, B03, B04　**Estimate:** 4–8 hours　**Backend/DB register:** BC-11。

**Changed/new files:** `summary/SummaryService.java`；`summary/RuleSummaryService.java`；`summary/TodaySummaryController.java`；`summary/DataSummaryController.java`；`summary/*Response.java`。

**Function/responsibility boundary:** `aggregateDay, aggregatePeriod, buildFacts`。

**Steps:**

1. All endpoints delegate one owner/date/softdelete aggregation, distinguishing null/0/partial/legacy.
2. Return independent knownDay average denominators; minutes include unavailable-energy records; reproducible daily/weekly comparisons.
3. Use explicit user goals only, no calculator fallback; intake minus exercise is not a complete deficit.

**Tests/evidence:** One day100/another null yields consistent mean/denominator; actual0 not missing; edits/deletion/date moves remain consistent; QA-restricted users can read.

**Completion:** Demo charts/cards share server facts, not competing algorithms.

**Suggested commit:** `fix(summary): unify recorded-data aggregation and comparisons [B13]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B14｜Versioned administrator account-state management

**Phase:** M1-SUPPORT　**Dependencies:** B02, D02　**Estimate:** 2–4 hours　**Backend/DB register:** BC-12。

**Changed/new files:** `admin/AdminController.java`；`admin/AdminService.java`；`admin/AdminUserSummary.java`；`admin/ChangeAccountStatusRequest.java`。

**Function/responsibility boundary:** `searchAccounts, changeStatus`。

**Steps:**

1. Minimal summary only identity/role/enabled/version; explicit pagination or bounded query.
2. Check versions on disable/restore; document anti-self-lockout/last-admin safeguards as new ADR rules.
3. Regress existing reset-password permissions without expanding private-data access.

**Tests/evidence:** USER403, stale-state409, cancel writes nothing, restoration preserves records, no private fields.

**Completion:** Every enabled Admin button has real authorization and clear outcomes.

**Suggested commit:** `fix(admin): version account status changes and restrict output [B14]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### B15｜Capability and contract readiness

**Phase:** M1　**Dependencies:** B00, B05, B06　**Estimate:** 1–2 hours　**Backend/DB register:** BC-13。

**Changed/new files:** `capabilities/CapabilitiesController.java`；`capabilities/CapabilitiesResponse.java`。

**Function/responsibility boundary:** `getCapabilities`。

**Steps:**

1. Return schemaVersion, catalogue ready/verified, each AI configured status and public reason.
2. No keys/internal addresses/sensitive configuration; no paid provider probing each request.
3. Record configuration and real smoke success separately; frontend may only disable further.

**Tests/evidence:** Unconfigured AI explicitly unavailable; failed import ready=false; authentication matches contract.

**Completion:** Production client distinguishes unsupported capability/network failure without launching simulated features.

**Suggested commit:** `feat(api): expose safe feature readiness information [B15]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F00｜Migrate Demo theme and application shell

**Phase:** M1　**Dependencies:** P01, P03　**Estimate:** 2–4 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/main.tsx`;`frontend/src/app/theme.ts`;`frontend/src/app/providers.tsx`;`frontend/src/styles.css`;`frontend/src/components/`.

**Function/responsibility boundary:** `theme, Brand, Page, common dialogs`.

**Steps:**

1. Move styles/presentation through the Demo source-map, retaining green/white layout; remove conflicting Vite default CSS.
2. Merge package/lockfiles rather than overwrite; use one ThemeProvider/QueryClient and retain StrictMode.
3. Exclude DemoTools/seed from production shell entry.

**Tests/evidence:** Build/lint baseline; visual login/empty-layout comparison; no duplicate Providers or mock accounts.

**Completion:** Main visual assets reused in a buildable production project.

**Suggested commit:** `refactor(frontend): reuse demo theme and presentation shell [F00]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F01｜Define canonical types, labels, units and dates

**Phase:** M1　**Dependencies:** P02, F00　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/api/contracts.ts`;`frontend/src/api/validators.ts`;`frontend/src/domain/types.ts`;`frontend/src/domain/labels.ts`;`frontend/src/domain/dates.ts`;`frontend/src/domain/units.ts`.

**Function/responsibility boundary:** `decodeDto, parseDate, formatEnergy`.

**Steps:**

1. Fix string ID/enum/null/BigDecimal JSON mapping from OpenAPI; remove Database/password_hash types.
2. Reuse valid Demo label/date display helpers; standardise today/zone and date-only strings.
3. Separate kJ/kcal input/display; isolate explicit legacy conversion adapters.

**Tests/evidence:** Reject malformed schema responses; null≠0; midnight/DST date-only handling; large-ID precision; unit round trips.

**Completion:** Pages no longer guess fields, units or null semantics independently.

**Suggested commit:** `refactor(frontend): define canonical API and display types [F01]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F02｜Connect Axios, CSRF, errors and cancellation

**Phase:** M1　**Dependencies:** F01, B02　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/api/client.ts`;`frontend/src/api/csrf.ts`;`frontend/src/api/errors.ts`;`frontend/vite.config.ts`.

**Function/responsibility boundary:** `ensureCsrfToken, normaliseApiError`.

**Steps:**

1. Reuse the client with same-origin /api proxy and withCredentials; business functions accept signals.
2. Keep CSRF tokens in memory with shared initialisation promise; resend at most once for explicit CSRF rejection.
3. Distinguish ordinary errors/timeouts/cancellation/HTML fallback; prohibit unbounded POST retries.

**Tests/evidence:** Anonymous-token login, expired-token recovery, FormData boundaries,401/403 distinction and signal cancellation.

**Completion:** Real requests carry cookies and errors are actionable by state machines.

**Suggested commit:** `feat(frontend): connect authenticated API transport and CSRF [F02]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F03｜Session Provider, query cache and route guards

**Phase:** M1　**Dependencies:** F02, F00, B03　**Estimate:** 4–6 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/app/SessionProvider.tsx`;`frontend/src/app/sessionEpoch.ts`;`frontend/src/app/queryClient.ts`;`frontend/src/app/queryKeys.ts`;`frontend/src/app/router.tsx`;`frontend/src/layouts/ProtectedRoute.tsx`.

**Function/responsibility boundary:** `bootstrapSession, expireSession, ProtectedRoute`.

**Steps:**

1. Replace whole-database subscription with real me/profile bootstrap; distinguish boot/error/anonymous.
2. Use the same Router7 data router, retaining paths/navigation blocking and USER/ADMIN/onboarding routing.
3. Owner-key caches and epoch reject late responses; logout clears caches/drafts/resources.

**Tests/evidence:** Refresh recovery; profile500 must not navigate to questionnaire; no cross-account late responses; no external returnTo redirects.

**Completion:** Page identity/permissions come from the server, not storage.

**Suggested commit:** `refactor(frontend): replace demo identity with session-aware routing [F03]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F04｜Connect real login and registration screens

**Phase:** M1　**Dependencies:** F03, B01, B02　**Estimate:** 2–4 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/pages/AuthPage.tsx`;`frontend/src/api/auth.ts`;`frontend/src/layouts/Header.tsx`.

**Function/responsibility boundary:** `submitLogin, submitRegister, logout`.

**Steps:**

1. Retain Demo forms/modes; remove demo query prefills/mock passwords.
2. Registration success is not login; after login restore CSRF/profile before navigation.
3. Header awaits real logout; network failure must not claim server logout.

**Tests/evidence:** Correct/incorrect passwords, duplicate emails, first USER signup, mode switching, protected requests fail after logout.

**Completion:** Demonstrable complete real-authentication happy path.

**Suggested commit:** `feat(frontend): wire demo auth screens to server sessions [F04]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F05｜Shared draft reducer and leave protection

**Phase:** M1　**Dependencies:** F01, F00　**Estimate:** 4–6 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/app/DraftProvider.tsx`;`frontend/src/features/entry/draftTypes.ts`;`frontend/src/features/entry/draftReducer.ts`;`frontend/src/features/entry/draftValidation.ts`;`frontend/src/hooks/useUnsavedChanges.ts`.

**Function/responsibility boundary:** `draft commands, dirty navigation, frozen submission`.

**Steps:**

1. Separate rowKey/clientEntryId/submissionId per Chapters6/14; edits revoke confirmation and increment revision.
2. Drafts remain memory-only without Database; account changes clear them; saving/unknown freezes edits.
3. Implement in-app blocking/beforeunload; retain drafts across in-flow Review navigation.

**Tests/evidence:** Stable row IDs, synchronous double-click lock, account switching, Back/Forward/refresh warning, unknown retains ID.

**Completion:** Five future entry paths share one Draft without duplicate save flows.

**Suggested commit:** `refactor(entry): centralise draft state and navigation safety [F05]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F06｜Connect atomic three-step onboarding

**Phase:** M1　**Dependencies:** F04, F05, B03　**Estimate:** 4–7 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/pages/OnboardingPage.tsx`;`frontend/src/features/profile/ProfileSettingsForm.tsx`;`frontend/src/api/profile.ts`.

**Function/responsibility boundary:** `initialiseOnce, submitOnboarding, mapFieldErrors`.

**Steps:**

1. Extract Demo onboarding, retaining three steps/back navigation; initialise only from the first server snapshot.
2. One complete POST includes initial weight/version; QA consent refusal still allows completion.
3. Map errors to steps; read-check unknown/already-completed conflicts rather than chaining three writes.

**Tests/evidence:** False consent, retained back-navigation input, failed completion, refresh after success reaches Dashboard, concurrent profile409.

**Completion:** Real new users reach an empty Dashboard.

**Suggested commit:** `feat(frontend): connect atomic onboarding workflow [F06]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F07｜Remote food search and stable selection

**Phase:** M1　**Dependencies:** F02, B05　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/features/entry/FoodReferencePicker.tsx`;`frontend/src/hooks/useFoodSearch.ts`;`frontend/src/api/foodReference.ts`.

**Function/responsibility boundary:** `searchActiveFood, selectReference`.

**Steps:**

1. Debounce/paginate with loading/error/no-match states, not filtering15 seeds.
2. Keep selectedReference independent of search pages; immutable IDs distinguish same names; stale queries cannot overwrite new terms.
3. Display real source/version and prevent Autocomplete from refiltering away server results.

**Tests/evidence:** Chicken→rice reversed responses, selection survives pagination, source labels, no seed fallback on outage.

**Completion:** Every row can stably select a real reference.

**Suggested commit:** `feat(entry): search and select real food references [F07]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F08｜Multiple-food editor and asynchronous previews

**Phase:** M1　**Dependencies:** F07, B07, F05　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/features/entry/FoodEditor.tsx`;`frontend/src/features/entry/FoodItemEditor.tsx`;`frontend/src/hooks/useFoodPreview.ts`.

**Function/responsibility boundary:** `editRow, previewPortion, validateReviewReadiness`.

**Steps:**

1. Retain Demo multirow appearance/independent meals; each row owns its Hook, never call Hooks in loops.
2. Edit grams as strings; bind preview to reference+grams and block stale confirmation.
3. Add/remove preserves other IDs/input; correctly reset reference/grams confirmations.

**Tests/evidence:** Two meals, zero/negative/decimal grams, middle-row removal, retained drafts on preview failure, reordered responses cannot save stale previews.

**Completion:** Review values correspond to every current row input.

**Suggested commit:** `feat(entry): wire multi-food editing to authoritative previews [F08]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F09｜Exercise editor and dated estimates

**Phase:** M1　**Dependencies:** F02, B10, F05　**Estimate:** 2–4 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/features/entry/ExerciseEditor.tsx`;`frontend/src/hooks/useExerciseEstimate.ts`;`frontend/src/api/exerciseTypes.ts`.

**Function/responsibility boundary:** `loadTypes, previewDatedExercise`.

**Steps:**

1. Select from real structured catalogue without Demo METs.
2. Refetch on date/type/minutes changes; support decimals, snapshots and unavailability reasons.
3. Do not require intensity; preview fingerprint matches current input.

**Tests/evidence:** Correct historical weight, Review despite missing weight, no old estimate after rapid edits.

**Completion:** Real exercise preview connects to save requests.

**Suggested commit:** `feat(entry): connect dated exercise estimates [F09]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F10｜Review create/edit dispatch and unknown-result recovery

**Phase:** M1　**Dependencies:** F08, F09, B09, B10, F03　**Estimate:** 4–7 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/features/entry/ReviewPage.tsx`;`frontend/src/hooks/useSubmitDraft.ts`;`frontend/src/features/entry/submissionPayload.ts`;`frontend/src/features/entry/SubmissionRecovery.tsx`;`frontend/src/api/submissions.ts`.

**Function/responsibility boundary:** `prepareSubmission, confirmDraft, recoverSubmission`.

**Steps:**

1. Freeze payload with synchronous re-entry guard; dispatch POST create/PUT edit, explicitly disabling unready edit APIs.
2. Clear draft/invalidate only after receipt validation; distinguish successful save from failed refresh.
3. For unknown, query receipt or retry original key/payload;404 does not create anew; minimal journal excludes health/full records.

**Tests/evidence:** Double clicks, second-item failure, lost post-commit response, receipt404 race, account-switch late result, cancel writes nothing.

**Completion:** Real food/exercise confirmation works without local store.

**Suggested commit:** `feat(review): implement reliable confirmation and receipt recovery [F10]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F11｜Real Dashboard and post-save refresh

**Phase:** M1　**Dependencies:** F10, B13, B11　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/pages/DashboardPage.tsx`;`frontend/src/hooks/useSummary.ts`;`frontend/src/hooks/useRecordQueries.ts`;`frontend/src/components/NutrientTiles.tsx`;`frontend/src/components/RecordList.tsx`.

**Function/responsibility boundary:** `loadToday, renderDayState, refreshAfterMutation`.

**Steps:**

1. Retain Demo layout; fetch same-date/zone summaries/records; group by meal, not save time.
2. Display null/0/partial/empty distinctly; minutes include unestimable activities.
3. Refetch after saves, never unconditionally increment today; update today at midnight/window restoration.

**Tests/evidence:** Empty day, two foods+exercise, past-date isolation, GET failure not empty, persistence after refresh.

**Completion:** Real M1 main flow with consistent totals/lists.

**Suggested commit:** `feat(dashboard): render confirmed server records and summaries [F11]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F12｜History URL filters, details and navigation

**Phase:** M1-SUPPORT　**Dependencies:** F11, B11　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/pages/HistoryPage.tsx`;`frontend/src/components/RecordDetailsDialog.tsx`;`frontend/src/api/records.ts`.

**Function/responsibility boundary:** `parseHistorySearch, navigateToSavedDate, loadDetails`.

**Steps:**

1. Drive range/type/highlight from URL; default to recent7 days only when absent; handle invalid values explicitly.
2. Read list/detail snapshots with explained sources, not whole database.
3. Backdated30-day saves navigate/highlight the correct range; locate moved records by ID and explain deletion.

**Tests/evidence:** Back/Forward,30-day history, owner filtering, empty versus network error, direct detail loads.

**Completion:** Users immediately find backdated saves without mistaking them for lost records.

**Suggested commit:** `feat(history): connect dated filtering and saved-record navigation [F12]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F13｜Historical corrections and deletion confirmation

**Phase:** M1-SUPPORT　**Dependencies:** F12, F10, B12　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/hooks/useRecordMutations.ts`;`frontend/src/components/RecordList.tsx`;`frontend/src/features/entry/ReviewPage.tsx`.

**Function/responsibility boundary:** `loadEditDraft, updateRecord, deleteRecord`.

**Steps:**

1. GET current version before LOAD_EDIT, preserving source; grams/date changes re-preview.
2. PUT expectedVersion;409 retains draft and compares latest state, never silently increments version/retries.
3. Confirm DELETE; cancellation makes no API call; invalidate old/new dates and details.

**Tests/evidence:** Two-tab conflicts, edit creates nothing new, date changes, repeated/cancelled deletion, readback after lost response.

**Completion:** Mutations affect original records and receipts never resurrect deleted entries.

**Suggested commit:** `feat(history): support versioned edits and confirmed deletion [F13]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F14｜Profile settings and separate weight operations

**Phase:** M1-SUPPORT　**Dependencies:** F06, B04, B03　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/pages/ProfilePage.tsx`;`frontend/src/features/profile/ProfileSettingsForm.tsx`;`frontend/src/features/profile/WeightEntryForm.tsx`;`frontend/src/api/weight.ts`.

**Function/responsibility boundary:** `saveSettings, removeGoal, correctWeight`.

**Steps:**

1. Retain Demo appearance with distinct settings/weight actions and complete null-clearing semantics.
2. Refetch cannot overwrite form baseline/version; unit target conversion preserves physical value.
3. Explicit versions for same-day weight corrections; refresh profile/estimates, not old records; withdrawn consent affects QA.

**Tests/evidence:** Removed goals remain null on readback, unit switches, no duplicate same-day weights, profile409, QA refusal leaves records usable.

**Completion:** Profile/units/weight/goals persist beyond browser state.

**Suggested commit:** `feat(profile): persist settings and dated weight corrections [F14]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F15｜Real trends and rule-based summaries

**Phase:** M1-SUPPORT　**Dependencies:** F12, B13　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/components/RecordCharts.tsx`;`frontend/src/components/RuleSummary.tsx`;`frontend/src/components/SourceNote.tsx`.

**Function/responsibility boundary:** `renderSeries, renderFacts, showCoverage`.

**Steps:**

1. Retain Recharts/Demo layout, sourcing server days/facts.
2. Break lines on missing days, expose known-day denominator/partial state and current-goal comparison rather than inferred past goals.
3. Source labels come from datasets, not hardcoded official names/demo values.

**Tests/evidence:** 100/null/real0 days, range changes, series/list consistency after edit/deletion.

**Completion:** Charts/tables/summaries share definitions and explain missing data.

**Suggested commit:** `feat(summary): render consistent trends and rule-based facts [F15]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F16｜Connect administrator screen

**Phase:** M1-SUPPORT　**Dependencies:** F03, B14　**Estimate:** 2–4 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/pages/AdminPage.tsx`;`frontend/src/api/admin.ts`;`frontend/src/components/ConfirmDialog.tsx`.

**Function/responsibility boundary:** `searchAccounts, confirmStatusChange`.

**Steps:**

1. Retain Demo list/filter/confirmation using minimal adminDTO only.
2. Lock pending actions; reread expectedVersion conflicts rather than automatically repeat.
3. Exclude local password hashes, mock identity switching and private-data links.

**Tests/evidence:** USER denial, deactivate/reactivate/cancel/conflicts, administrator safeguards.

**Completion:** Admin flow uses real APIs rather than browser state toggles.

**Suggested commit:** `feat(admin): integrate account status controls [F16]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F17｜Visual, accessibility and interaction regression

**Phase:** M1-SUPPORT　**Dependencies:** F10, F11, F12, F14, F16　**Estimate:** 2–4 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/styles.css`;`frontend/src/components/`;`docs/verification/ui-regression.md`.

**Function/responsibility boundary:** `focus, responsive states, busy behaviour`.

**Steps:**

1. Compare original Demo screenshots without unjustified redesign; check desktop/narrow layouts for clipping.
2. Verify error focus, step navigation, dialog focus, keyboard use, pending edit locks and live loading.
3. Check Back/Forward/refresh and recordDate boundaries, not only appearance.

**Tests/evidence:** Keyboard forms/dialogs, long names/empty lists, leaving during processing, real-browser records.

**Completion:** UI changes serve integration without regressing correct interactions.

**Suggested commit:** `fix(ui): preserve demo interactions across real loading states [F17]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### F18｜Exclude production mocks and enable real M1

**Phase:** M1　**Dependencies:** F03, F10, F11, B15　**Estimate:** 1–2 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `scripts/check-production-boundaries.mjs`;`frontend/src/App.tsx`;`frontend/src/app/capabilities.ts`;`frontend/src/main.tsx`.

**Function/responsibility boundary:** `assertProductionImports, enableRealCapabilities`.

**Steps:**

1. Exclude store/seed/DemoTools from production; retain legacy tests separately.
2. Capabilities disable unconnected M2; real-request failures never fall back.
3. Inspect production bundle for demo accounts/hashes/provider keys and record dependency-graph evidence.

**Tests/evidence:** Intentional mock import fails; normal build has no mock entry; backend disconnection shows errors.

**Completion:** M1 can honestly claim real integration without mixed mock success.

**Suggested commit:** `chore(frontend): exclude demo services from production runtime [F18]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A00｜Shared provider transport and configuration boundaries

**Phase:** M2　**Dependencies:** B00, B02, B15　**Estimate:** 3–5 hours　**Backend/DB register:** BC-15, BC-16.

**Changed/new files:** `ai/AiTransport.java`;`ai/AiProviderConfig.java`;`ai/AiExceptionMapper.java`;`backend/src/main/resources/application-*.properties`.

**Function/responsibility boundary:** `sendJson, sendMultipart, validateProviderConfiguration`.

**Steps:**

1. Reuse JavaHttpClient with explicit connection/processing timeouts, file limits and schema parameters; no new microservice.
2. Configure model/key through backend environment; unconfigured features return503; capabilities do not make paid probes.
3. Redact logs; no calls inside DB transactions or automatic database-writing tools.

**Tests/evidence:** Map provider401/429/5xx/timeouts consistently; main app works without key; no secrets in responses.

**Completion:** Three adapter types share testable transport with separately implemented business scopes.

**Suggested commit:** `refactor(ai): centralise provider transport and safe configuration [A00]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A01｜Image candidates and real-reference matching backend

**Phase:** M2　**Dependencies:** A00, B05　**Estimate:** 3–6 hours　**Backend/DB register:** BC-15.

**Changed/new files:** `food/FoodRecognitionController.java`;`food/OpenAiVisionService.java`;`food/FoodCandidateMatcher.java`;`food/FoodRecognitionResponse.java`.

**Function/responsibility boundary:** `recognize, validateImage, matchCandidates`.

**Steps:**

1. Retain image field/size limits, adding content/dimension validation and decoder-support checks.
2. Validate provider candidates against schemas/numeric limits, then match real catalogue references.
3. No UI confidence percentages or record writes; clearly distinguish empty results/failures.

**Tests/evidence:** Valid image, fake MIME, oversize, no food, bad JSON, unknown names, unavailable provider and zero record inserts.

**Completion:** Reviewable real image candidates, not three fixed results.

**Suggested commit:** `feat(image): match recognised foods to real references [A01]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A02｜Image frontend and shared draft integration

**Phase:** M2　**Dependencies:** A01, F10　**Estimate:** 2–4 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/features/entry/media/ImageEntryPanel.tsx`;`frontend/src/api/recognition.ts`;`frontend/src/features/entry/EntryPage.tsx`.

**Function/responsibility boundary:** `uploadImage, mapCandidatesToDraft, cleanupPreview`.

**Steps:**

1. Upload real FormData; cancel stale requests/ObjectURLs; old image responses cannot replace new choices.
2. Map to unconfirmed DraftFood, editable date, no guessed meal and independently editable items.
3. Retain drafts/manual fallback on errors; every final save uses Review.

**Tests/evidence:** Reordered file responses, cancellation writes nothing, removing one candidate preserves others, bad files and real batch save.

**Completion:** Images cannot bypass shared confirmation.

**Suggested commit:** `feat(image): connect recognition to shared food review [A02]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A03｜Real file transcription backend

**Phase:** M2　**Dependencies:** A00　**Estimate:** 4–7 hours　**Backend/DB register:** BC-16.

**Changed/new files:** `speech/SpeechController.java`;`speech/TranscriptionService.java`;`speech/TranscriptionResponse.java`.

**Function/responsibility boundary:** `transcribeAudio, validateAudio`.

**Steps:**

1. Implement multipart audio with format/size/empty/duration policy aligned across proxy/servlet.
2. Call the selected model real file-transcription contract; return editable transcript, not sample sentences.
3. Do not persist raw audio/write records; provider failures are retryable and isolated.

**Tests/evidence:** Actual WebM/MP4 support, empty/fake files, timeout, unauthorised calls, memory/temp-file cleanup.

**Completion:** Real transcription with explicit permission/cost boundaries.

**Suggested commit:** `feat(speech): add authenticated audio transcription [A03]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A04｜Structured food and exercise text extraction

**Phase:** M2　**Dependencies:** A00, B05, B06　**Estimate:** 4–7 hours　**Backend/DB register:** BC-16.

**Changed/new files:** `speech/EntryExtractionService.java`;`speech/ParseSpeechRequest.java`;`speech/ParsedEntryResponse.java`;`speech/CandidateMatcher.java`.

**Function/responsibility boundary:** `parseFood, parseExercise, resolveRelativeDate`.

**Steps:**

1. Fix schemas for kind/text/date context; independently validate extraction and never execute text instructions.
2. Mark missing dates as default today; preserve missing grams/meal/type/minutes; do not invent grams for cups/bowls.
3. Match real catalogue IDs; first-version multiple exercise requests require explicit splitting rather than silent omission.

**Tests/evidence:** Yesterday/explicit/future dates, missing meals, half-hour durations, unknown types, prompt injection and bad JSON.

**Completion:** Parsing creates pending candidates only, never records or invented nutrition.

**Suggested commit:** `feat(speech): extract validated food and activity drafts [A04]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A05｜Browser recorder and microphone lifecycle

**Phase:** M2　**Dependencies:** F02, A03　**Estimate:** 4–7 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/features/entry/media/Recorder.ts`;`frontend/src/api/speech.ts`.

**Function/responsibility boundary:** `start, stop, cancel, dispose`.

**Steps:**

1. Request microphone only after user action; choose MIME from isTypeSupported intersected with backend formats.
2. Await complete stop/dataavailable Blob; enforce time/size limits and stop all tracks/timers/listeners.
3. Handle denial/device loss/unsupported formats/rapid start-stop; never mislabel audio as WAV.

**Tests/evidence:** Automated fake-media flow plus actual-browser microphone checks; microphone indicator off after unmount.

**Completion:** Both speech paths share real recording with releasable resources.

**Suggested commit:** `feat(voice): implement reusable browser audio recording [A05]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A06｜Connect both speech paths to Review

**Phase:** M2　**Dependencies:** A04, A05, F10　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/features/entry/media/VoiceEntryPanel.tsx`;`frontend/src/features/entry/EntryPage.tsx`;`frontend/src/features/entry/draftReducer.ts`.

**Function/responsibility boundary:** `transcribeThenEdit, extractDraft, reparseWithConfirmation`.

**Steps:**

1. Recording→transcription→editable text→extraction→Draft, with separate loading/errors at every stage.
2. Text edits stale prior extraction; confirm replacing manually edited candidates; validate food/exercise mappings separately.
3. Share final preview/Review/save; microphone refusal offers manual entry without claiming speech passed.

**Tests/evidence:** Multiple food items missing grams, exercise missing minutes, reparse overwrite confirmation and saveable unestimable activity.

**Completion:** Real FR12/13 inputs obey manual-save rules.

**Suggested commit:** `feat(voice): integrate food and exercise transcription flows [A06]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A07｜QA eligibility, scope and record context

**Phase:** M2　**Dependencies:** B03, B13, A00　**Estimate:** 4–7 hours　**Backend/DB register:** BC-16.

**Changed/new files:** `qa/QaEligibilityService.java`;`qa/QaScopePolicy.java`;`qa/QaContextBuilder.java`;`qa/QaRequest.java`.

**Function/responsibility boundary:** `requireEligibility, assessScope, buildAllowedFacts`.

**Steps:**

1. Check current DB NO+consent each request; use only allowed owned summary facts.
2. Exclude raw safety answers/profile identity from models; define input/scope limits and disease/drug/treatment refusals.
3. Build context server-side rather than trust client claims; no database tools.

**Tests/evidence:** Direct HTTP bypass attempts, withdrawn consent, foreign userID injection, no raw safety transmission, record-range limits.

**Completion:** Independently testable QA permissions/data minimisation.

**Suggested commit:** `feat(qa): enforce consent scope and minimal record context [A07]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A08｜Real QA provider and answer endpoint

**Phase:** M2　**Dependencies:** A07, A00　**Estimate:** 3–6 hours　**Backend/DB register:** BC-16.

**Changed/new files:** `qa/QaController.java`;`qa/QaService.java`;`qa/OpenAiQaAdapter.java`;`qa/QaResponse.java`.

**Function/responsibility boundary:** `ask, validateResponse, recheckConsent`.

**Steps:**

1. Send only permitted messages/facts to real models; append nonmedical notice; single-turn without DB chat persistence.
2. Hold no transaction during external calls; recheck consent before delivery and suppress withdrawn-consent answers.
3. Distinguish scope refusal/provider errors; no fixed fallback advice on failure.

**Tests/evidence:** General/record questions, medical refusal, malicious instructions, mid-call consent withdrawal, timeout and zero record inserts.

**Completion:** Real QA answers/permissions, not fixed keyword replies.

**Suggested commit:** `feat(qa): add bounded real-model question answering [A08]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A09｜Real chat page and consent withdrawal

**Phase:** M2　**Dependencies:** A08, F03, F14　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/src/pages/ChatPage.tsx`;`frontend/src/api/qa.ts`;`frontend/src/app/capabilities.ts`.

**Function/responsibility boundary:** `sendQuestion, retryMessage, restrictChat`.

**Steps:**

1. Distinguish restricted eligibility from unconfigured service; provide clear Profile access without locking ordinary records.
2. Match responses by messageKey, show pending/error bubbles; single-turn context does not promise persistent conversational memory.
3. Logout/withdrawal clears sensitive display/local waiting state; render text safely.

**Tests/evidence:** Eligible/unavailable/refusal/retry, late account-switch responses, withdrawal; no fixed answers.

**Completion:** Chat UI correctly connects to the real QA contract.

**Suggested commit:** `feat(chat): connect consent-aware real question answering [A09]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### A10｜Real-provider smoke and M2 evidence

**Phase:** M2　**Dependencies:** A02, A06, A09　**Estimate:** 2–4 hours　**Backend/DB register:** BC-15, BC-16.

**Changed/new files:** `docs/verification/provider-smoke.md`;`docs/verification/m2-evidence.md`;`tests/fixtures/ai/`.

**Function/responsibility boundary:** `real service smoke and configuration evidence`.

**Steps:**

1. Use authorised keys and synthetic/owned inputs only; record model configuration, date, request IDs and redacted output.
2. Run actual image, microphone food/exercise and eligible QA flows; reconcile that DB writes occur only on Confirm.
3. Missing network/key means BLOCKED_EXTERNAL, never mock-backed real PASS.

**Tests/evidence:** All success/failure boundaries in17.9; no cost/data leakage.

**Completion:** Every advanced path has real evidence or explicit blocker, never blanket completion.

**Suggested commit:** `test(ai): record real multimodal and QA smoke evidence [A10]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q00｜Java/PG16 integration harness

**Phase:** M1　**Dependencies:** D01, P01　**Estimate:** 4–6 hours　**Backend/DB register:** BC-14.

**Changed/new files:** `backend/pom.xml`;`backend/src/test/java/com/fitnutri/backend/support/`;`backend/src/test/resources/`.

**Function/responsibility boundary:** `Postgres test lifecycle and fixtures`.

**Steps:**

1. Configure JUnit/Spring tests from actual Boot4 support using Testcontainers PG16 or equivalent isolated real PG.
2. Run Flyway automatically with synthetic users/catalogues; transactions/identity do not depend on shared data.
3. Report missing Docker explicitly; H2 cannot validate PG locks/partial indexes.

**Tests/evidence:** Reproducible independent runs; failure cleanup touches only test resources, not teammates volumes.

**Completion:** Backend tasks can validate behaviour on real PostgreSQL.

**Suggested commit:** `test(backend): establish isolated PostgreSQL integration harness [Q00]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q01｜Reuse Demo cases and align Java units

**Phase:** M1　**Dependencies:** B07, B10, F05　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/tests/legacy-demo/`;`frontend/tests/unit/`;`backend/src/test/java/com/fitnutri/backend/domain/`;`tests/fixtures/calculations.json`.

**Function/responsibility boundary:** `shared calculation and state fixtures`.

**Steps:**

1. Retain meaning/provenance of the original15 tests, explicitly mock-service rather than HTTP tests.
2. Port calculation/date/snapshot/null/eligibility cases to Java/frontend-input tests; synthetic METs are not official coefficients.
3. Share final-rounding and ID-lifecycle fixtures.

**Tests/evidence:** Original15 tests pass or documented reasons explain changes; matching Java results; no arbitrary assertion removal.

**Completion:** Demo assets become migration regression protection.

**Suggested commit:** `test(domain): preserve demo behaviours as integration fixtures [Q01]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q02｜Concurrency, atomicity and lost responses

**Phase:** M1　**Dependencies:** B09, B10, B12, Q00　**Estimate:** 4–7 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `backend/src/test/java/com/fitnutri/backend/records/`;`backend/src/test/java/com/fitnutri/backend/submission/`;`frontend/tests/e2e/submission-recovery.spec.ts`.

**Function/responsibility boundary:** `parallel writes, rollback, late commit`.

**Steps:**

1. Use two real connections for same/different keys; second-food failure rolls back everything; replay after soft deletion.
2. Lose responses after DB commit and verify receipt recovery; test receipt404 while original POST is uncommitted.
3. Explicitly reject version conflicts, same-ID/different payload and cross-batch item reuse.

**Tests/evidence:** Inspect actual DB counts/receipts/hashes/snapshots; recovery leaves one record, not inferred from DOM totals.

**Completion:** Reproducible automatic evidence for key reliability guarantees.

**Suggested commit:** `test(records): cover atomicity concurrency and uncertain outcomes [Q02]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q03｜Auth, profile, weight and admin HTTP regression

**Phase:** M1　**Dependencies:** B01, B02, B03, B04, B14, Q00　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `backend/src/test/java/com/fitnutri/backend/security/`;`backend/src/test/java/com/fitnutri/backend/profile/`;`backend/src/test/java/com/fitnutri/backend/admin/`.

**Function/responsibility boundary:** `real session request scenarios`.

**Steps:**

1. Use real Cookie/CSRF across requests, not mockAuthentication alone.
2. Cover first USER, duplicate email, role tampering, logout, disabled users, ownership/admin boundaries.
3. Cover atomic questionnaire failure, false consent, null goals and weight correction/concurrency/versions.

**Tests/evidence:** No sensitive-field leaks; FR/NFR-linked assertions; failures retain original data.

**Completion:** HTTP evidence for security/Profile services used by pages.

**Suggested commit:** `test(api): verify session profile weight and admin contracts [Q03]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q04｜Frontend component and network contracts

**Phase:** M1　**Dependencies:** F03, F05, F10, F14　**Estimate:** 4–6 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/tests/unit/`;`frontend/tests/component/`;`frontend/tests/contract/`;`frontend/package.json`.

**Function/responsibility boundary:** `async UI and DTO regression`.

**Steps:**

1. Retain Node tests; check compatibility before adding component runner/TestingLibrary; reuse existing tools.
2. Mock precise API contracts for reordered searches/previews,401/403, nulls, versions, epoch and field focus.
3. Fixtures prove UI only; names must not label mocks as real DB integration.

**Tests/evidence:** Malformed/HTML responses, large IDs, partial data, successful save/failed GET, dirty settings retained on refetch.

**Completion:** Automated evidence for critical asynchronous states.

**Suggested commit:** `test(frontend): cover async contracts forms and recovery states [Q04]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q05｜M1 core browser E2E

**Phase:** M1　**Dependencies:** F06, F10, F11, F18, Q02, Q03　**Estimate:** 4–7 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/playwright.config.ts`;`frontend/tests/e2e/core-flow.spec.ts`;`docs/verification/m1-evidence.md`.

**Function/responsibility boundary:** `register-to-dashboard real journey`.

**Steps:**

1. Run real Java+PG+frontend without fulfilling every business request through mocks.
2. New-user questionnaire→two foods→Review cancel/save→exercise→Dashboard→refresh/relogin.
3. Exercise past dates, absent goals, unestimable activity, duplicate clicks and service failures; check DB/HTTP each step.

**Tests/evidence:** Main/inverse M1 paths pass; actual browser/commit/schema recorded.

**Completion:** Acceptable real core MVP, excluding still-mocked M2.

**Suggested commit:** `test(e2e): verify real core MVP user journeys [Q05]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q06｜History, Profile, admin and conflict E2E

**Phase:** M1-SUPPORT　**Dependencies:** F13, F14, F15, F16, Q05　**Estimate:** 3–6 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `frontend/tests/e2e/record-management.spec.ts`;`frontend/tests/e2e/profile-admin.spec.ts`;`docs/verification/support-evidence.md`.

**Function/responsibility boundary:** `cross-page consistency and concurrency`.

**Steps:**

1. Correct grams/dates, cancel/confirm deletion, locate past dates, clear goals and correct dated weight.
2. Two browser contexts of one account create version conflicts; another account cannot see private data.
3. Verify admin deactivate/reactivate preserves records and statistics/charts agree.

**Tests/evidence:** Lists/charts/cards/summaries share facts; no silent conflicts; reproducible Back/Forward.

**Completion:** Support pages pass full workflows, not just page implementation.

**Suggested commit:** `test(e2e): verify record management profile and admin flows [Q06]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q07｜Advanced contracts, permissions and failures

**Phase:** M2　**Dependencies:** A02, A06, A09, Q03　**Estimate:** 3–6 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `backend/src/test/java/com/fitnutri/backend/ai/`;`frontend/tests/e2e/advanced-contract.spec.ts`;`docs/verification/ai-policy-tests.md`.

**Function/responsibility boundary:** `provider stubs, privacy, media states`.

**Steps:**

1. Controlled fake providers test HTTP/schema/errors only; cover bad JSON, refusal/timeouts and missing keys.
2. Verify image/speech processing does not write records, raw safety is not transmitted and direct QA unauthorised/withdrawn-consent access fails.
3. Cover denied microphone, unsupported formats, candidate edits/cancellation and unforgeable UI capabilities.

**Tests/evidence:** Record automatic contracts separately from A10 real smoke; missing either prevents M2 completion.

**Completion:** All AI paths preserve shared save/privacy boundaries.

**Suggested commit:** `test(ai): verify multimodal contracts privacy and failure handling [Q07]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### Q08｜Actual-browser compatibility, accessibility and timing

**Phase:** M1-AND-M2　**Dependencies:** Q05, Q06　**Estimate:** 4–7 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `docs/verification/browser-matrix.md`;`docs/verification/performance.md`;`docs/verification/accessibility.md`.

**Function/responsibility boundary:** `manual browser and timing checks`.

**Steps:**

1. Record actual desktop Chrome/Edge/Safari versions; Playwright WebKit is not Safari sign-off.
2. Run at least auth/questionnaire/manual/Review/history; append real microphone/image/QA after M2.
3. Measure ordinary operations against approximately3-second target with environment/sample/timings; AI must give immediate feedback and bounded timeouts.

**Tests/evidence:** Per-item PASS/FAIL/NOT RUN/environment, not verbal all-green conclusions.

**Completion:** Measured compatibility/performance; rerun M2 additions alongside A10.

**Suggested commit:** `test(verification): record browser compatibility and response timings [Q08]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### R00｜CI and one-command local checks

**Phase:** DELIVERY　**Dependencies:** P01, P03, Q00　**Estimate:** 3–5 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `scripts/check.sh`;`scripts/check-production-boundaries.mjs`;`.github/workflows/ci.yml`;`docs/development.md`.

**Function/responsibility boundary:** `lint build test pipeline`.

**Steps:**

1. Unify frontend/backend commands; configure enterprise CI against actual runners, not assumed Actions availability.
2. Stage JavaPG and frontend build/lint/unit/contracts/optionalE2E; failure blocks Ready.
3. No unlimited real-provider charges per PR; secret-free CI uses labelled mocks.

**Tests/evidence:** Matching local/CI commands; intentional failures exit nonzero; no continue-on-error masking required tests.

**Completion:** Attach CI-run evidence when available, otherwise runnable local checks/explicit limitations.

**Suggested commit:** `ci: add reproducible integration quality gates [R00]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### R01｜Local startup and production-style same-origin deployment

**Phase:** DELIVERY　**Dependencies:** F18, B02, D05, R00　**Estimate:** 3–5 hours　**Backend/DB register:** BC-01, BC-13, BC-14.

**Changed/new files:** `docker-compose.yml`;`deploy/`;`scripts/dev.sh`;`docs/runbook.md`;`frontend/vite.config.ts`;`backend/src/main/resources/application-*.properties`.

**Function/responsibility boundary:** `run services, reverse proxy, readiness`.

**Steps:**

1. Retain PG16/Java21/Vite development boundaries; production uses static frontend/API reverse proxy with SPA refresh but intact /api404.
2. Configure Cookie/CSRF/origins/body limits/model environment without secrets; explicit DB health/catalogue readiness.
3. Document npm ci/build, mvn verify, migration and startup order; never expose dev server publicly by default.

**Tests/evidence:** Direct /history and /review refresh, working session cookies, M1 without providers, JSON API errors instead of HTML.

**Completion:** Team reproducibly starts from clean environment without production reliance on Vite proxy.

**Suggested commit:** `chore(deploy): document and verify same-origin application startup [R01]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### R02｜Backup restoration and legacy migration rehearsal

**Phase:** DELIVERY　**Dependencies:** D05, R01　**Estimate:** 3–6 hours　**Backend/DB register:** BC-14.

**Changed/new files:** `scripts/db/backup.sh`;`scripts/db/restore-to-new-db.sh`;`docs/db/restore-rehearsal.md`.

**Function/responsibility boundary:** `backup, restore new target, reconcile`.

**Steps:**

1. Check target/permissions before backup; no committed dumps; restore only to new isolated DB.
2. Compare counts/known nutrients/dates/reference legacy states; explicitly stop writes or define snapshot strategy.
3. Test app/schema rollback compatibility, not automatic downgrade SQL dropping new fields.

**Tests/evidence:** Restored login/records readable; original DB/volumes unchanged; backup actually usable, not merely present.

**Completion:** Operational rollback and explainable legacy preservation.

**Suggested commit:** `test(db): rehearse backup restoration and legacy reconciliation [R02]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.


### R03｜Final ledger, risks and delivery sign-off

**Phase:** DELIVERY　**Dependencies:** R00, R01, R02, Q01, Q02, Q03, Q04, Q05, Q06, Q07, Q08, A10　**Estimate:** 2–4 hours　**Backend/DB register:** none (this task must not hide backend changes).

**Changed/new files:** `docs/implementation/tasks.md`;`docs/implementation/backend-change-log.md`;`docs/verification/final-report.md`;`README.md`.

**Function/responsibility boundary:** `M1/M2 sign-off and handover`.

**Steps:**

1. Record actual commits/tests/evidence per task and summarise API/DB rationale/compatibility impacts.
2. Separate M1/support/M2 status and itemise unrun/external blockers without masking them in percentages.
3. Remove temporary debug entry points/review pending files; deliver startup, demo-data provenance, limitations and recovery.

**Tests/evidence:** All required dependencies DONE with accessible evidence; explained git status; no fabricated PR/merge claims.

**Completion:** Agent can stop with human-reviewable delivery rather than a bare completion claim.

**Suggested commit:** `docs(delivery): record integration evidence and outstanding risks [R03]`. The body must include actual Tests/BC/DB/Limitations under Section 0.3.

---

# 19. Testing specification: from Demo tests to real-system acceptance

## 19.0 Course basis and when to write tests (not a separate grading promise)

ELEC5619-Lab9.pdf pages2–8 covers numerous fast JUnit/Mockito units, MockMvc service-layer tests, JPA/database tests, fewer cross-layer integration/E2E cases, performance/security smoke and User Story-based UAT, explicitly recommending shift-left, determinism and AAA. L9 Testing (1).pptx pages6–8 assigns unit tests to developers and introduces TDD before/during implementation; pages20–22 cover the pyramid/contracts. ELEC5619-Lab7.pdf pages1–2 emphasises feature demonstrations, task/contribution evidence and Q&A in Sprint assessment; Lab9 pages1–2 emphasises Sprint Backlog, individual contributions and retrospectives.

**Execution decision: develop tests alongside features, with author self-review before peer review; actual users/owners still conduct final UAT.** This is more explicit than adding a few JUnit files at the end, but does not claim a course-mandated standalone report format or fixed95% coverage. Follow0.7; Q00–Q08 do not permit delaying unit tests.

## 19.1 Test layers and what they prove

|Layer|Tools/environment|Evidence scope|Cannot replace|
|---|---|---|---|
|Original Demo domain/service|Node built-in test/MemoryStorage|Local rule/interaction regressions|Java sessions/SQL transactions/real providers|
|Java service units|Compatible JUnit, controlled Clock, synthetic references|Exact calculations/eligibility/date/state rules|Database indexes/concurrency|
|Java API+PG integration|Effective Spring Boot test stack, isolated PG16|Session/CSRF/ownership, migration, transactions, versions|Real-browser media/layout|
|Frontend unit/component|Original Node tests; compatible runner as needed|Reducers, DTOs, forms, loading/errors, reordered responses|Actual PG persistence|
|Browser E2E|Playwright + real Java/PG|User workflows and HTTP/DB interaction|All actual-Safari differences, real-provider success|
|Provider contract|Controlled fake-provider HTTP service|Schemas/errors/timeouts/privacy payloads|External-account availability/output quality|
|Real-provider smoke|Actual authorised provider|Connected image/audio/QA flows|Security coverage of every possible input|
|Manual browser/performance|Actual Chrome/Edge/Safari|Prototype experience, microphone, compatibility/timing|Screenshots cannot replace data assertions|

Testcontainers or manually isolated PG are acceptable, but transaction/unique/partial-index tests require PG16, not H2 compatibility mode. Verify Boot4 test packages/dependencies through the effective POM rather than copying imports and skipping compilation.

Playwright covers Chromium/Firefox/WebKit/browser channels, but its WebKit is not installed Safari; perform the Proposal's actual Safari acceptance separately on Mac.[DOC-PLAYWRIGHT]

## 19.2 Synthetic fixtures

Clearly distinguish fixtures from demos/official data. Suggested fixed fixtures:

- USER_A, USER_B, ADMIN_A and a disabled user with separate emails; generate test passwords rather than reuse real accounts.
- Two food references with distinct meals/known per100g nutrients; an inactive old version, missing-core-nutrition reference and unlinked legacy record.
- Synthetic MET=3.5 activity solely for formula testing, plus a valid but inapplicable age range.
- Three user weight dates: earlier70kg, later80kg and a value later than every activity; same-date versioned correction.
- Fixed Clock plus UTC/Australia/Sydney dates around midnight/DST.
- Provider mocks with valid/empty candidates, invalid JSON, negative grams, wrong IDs, excessive text, timeouts and refusals.

Fixtures may share canonical values, but do not present the Proposal prototype's838kJ as an inevitable real AUSNUT result without explanation. Official-source E2E samples are calculated/checked from actual imports.

## 19.3 Core acceptance cases

Use these IDs in test names, Jira/Issues and PR evidence. Record environment, BASE_COMMIT, schema version, inputs and assertions for each.

|ID|Precondition/action|Assertion|Main tasks|
|---|---|---|---|
|T-AUTH-01|First public signup in empty DB|USER role, no ADMIN promotion|B01/Q03|
|T-AUTH-02|Case-varied same-email concurrent signup|One account, explicit conflict|B01/Q03|
|T-AUTH-03|Tamper role/direct admin request|Denied; DB role unchanged|B01/B14/Q03|
|T-AUTH-04|Login then refresh me|Same session valid, profile-based routing|B02/F03/Q05|
|T-AUTH-05|Session/CSRF before/after login|Effective strategy; old token rejected/new accepted|B02/Q03|
|T-AUTH-06|GET/write records after logout|401, no new record|B02/Q03|
|T-AUTH-07|Switch A→B with late A response|B sees no A data/success notice|F03/Q04/Q05|
|T-PROFILE-01|Initial questionnaire without QA consent|Completes, food entry works, QA restricted|B03/F06/Q03|
|T-PROFILE-02|Initial-weight write fails|No partial user/profile/completed updates|B03/Q03|
|T-PROFILE-03|Lost save response/already completed|GET recovery, no duplicate weight|B03/F06/Q05|
|T-PROFILE-04|Null goal/unit switch|Actually cleared; same physical quantity, no record rewrite|B03/F14/Q06|
|T-PROFILE-05|Refetch/version change during dirty form|Input retained; explicit submission conflict|F14/Q04|
|T-WEIGHT-01|Backfill earlier weight|History retained, current does not regress|B04/Q03|
|T-WEIGHT-02|Same-day correction/concurrency|Date uniqueness/version checks, no duplicate append|B04/Q03|
|T-WEIGHT-03|Read old exercise after weight change|Snapshot unchanged|B04/B10/Q01|
|T-FOOD-01|Two meals/foods, Review then Cancel|No inserts; summary unchanged|F08/F10/Q05|
|T-FOOD-02|Confirm two real items|Both saved once, receipt/items match|B09/Q02/Q05|
|T-FOOD-03|Second item invalid/DB failure|Neither first item nor receipt remains|B09/Q02|
|T-FOOD-04|100→150g|Preview/save share formula/rounding|B07/B09/Q01|
|T-FOOD-05|Tamper calories/userId|No client authority/unauthorised access|B09/Q03|
|T-FOOD-06|Rapid query/grams edits, late old response|No overwritten selection/stale preview save|F07/F08/Q04|
|T-FOOD-07|Open/edit inactive-version record|Original source shown, no silent new-version recalculation|B11/B12/Q06|
|T-EX-01|Activity between two weight dates|Latest prior weight, not current|B10/Q01|
|T-EX-02|No weight on/before date|null/MISSING_WEIGHT; activity saveable|B10/F09/Q05|
|T-EX-03|Inapplicable reference|NO_APPLICABLE_REFERENCE, no guessed energy|B10/Q01|
|T-EX-04|Decimal minutes|Correct numeric precision, no Integer truncation|B10/F09/Q01|
|T-EX-05|Weight changed after preview|409 CALCULATION_CHANGED, no write, re-review|B10/F10/Q02|
|T-IDEM-01|Two connections, same submissionId/payload|One creation, matching201/200 receipts|B08/Q02|
|T-IDEM-02|Same submissionId/different payload|409, no extra write|B08/Q02|
|T-IDEM-03|Different submission reuses itemID|Reject rather than skip existing items|B08/B09/Q02|
|T-IDEM-04|Response lost after commit|GET receipt recovery or same-payload retry without duplicates|F10/Q02|
|T-IDEM-05|Receipt404 while original POST runs|No new ID, one eventual creation|F10/Q02|
|T-IDEM-06|Replay creation after correction/deletion|No resurrection/rollback; changes retained|B08/B12/Q02|
|T-EDIT-01|Change grams without client nutrition|Correct server recalculation|B12/Q06|
|T-EDIT-02|Change exercise date|Reselect dated weight, move old/new date statistics|B12/Q06|
|T-EDIT-03|Two tabs edit same version|One succeeds/one409, no silent overwrite|B12/Q02/Q06|
|T-DELETE-01|Cancel/confirm/repeat deletion|Cancel retains; confirm excludes; repeated owned delete204|B12/F13/Q06|
|T-HISTORY-01|Save30-day-old record|Locate that date and find after refresh|F12/Q06|
|T-HISTORY-02|URL changes/Back/Forward|Restore filters, not default seven days|F12/Q06|
|T-SUM-01|Three days100/null/0|Specified totals/knownDay denominator/gaps|B13/F15/Q01|
|T-SUM-02|Same-range today/daily/trends|Consistent facts for common dates|B13/Q06|
|T-SUM-03|No/removed goal|Actual values retained, comparison empty, no automatic goal|B13/F14/Q06|
|T-SUM-04|Exercise with unavailable energy|Minutes retained, no energy0 fill, explicit partial|B13/Q05|
|T-REF-01|Repeated manifest/different hash same version|Idempotent/rejected; no historical overwrite|B05/B06/Q01|
|T-REF-02|Missing catalogue/import failure|readiness=false, explicit UI unavailability|B15/F18/Q04|
|T-ADMIN-01|Direct USER admin request/minimal ADMIN list|Denied/no private data|B14/Q03|
|T-ADMIN-02|Deactivate/reactivate/version conflict|Correct status, personal records retained|B14/F16/Q06|
|T-STATE-01|Write succeeds/summary GET fails|Saved notice, GET-only retry|F10/F11/Q04|
|T-STATE-02|Refresh /review without draft|Return clearly to Add, no empty record|F05/Q05|
|T-STATE-03|Leave/edit during pending|Freeze or explicit warning; frozen payload unchanged|F05/F10/Q04|
|T-DB-01|Migrate/restore legacy clone|Auditable counts/energy/dates/legacy notes|D05/R02|
|T-DB-02|Schema does not match baseline|Explicit failure, no automatic adoption|D01/Q00|
|T-BOUNDARY-01|Real service disconnected|Error, no Demo fallback|F18/Q05|
|T-BOUNDARY-02|Production imports store/seed|Boundary script rejects build|F18/R00|

## 19.4 M2 additional tests

|ID|Scenario|Required assertion|
|---|---|---|
|T-IMAGE-01|Two different real food images|Actual provider calls, nonfixed candidates; no exact-output determinism requirement|
|T-IMAGE-02|Fake MIME/oversize/undecodable|Explicit rejection, zero record writes|
|T-IMAGE-03|Edit/remove candidate then save|Other items independent; nutrition from selected references|
|T-VOICE-01|Real food/exercise recording|Actual upload/transcription, not sample sentences|
|T-VOICE-02|Microphone denial/no device/unsupported format|Actionable feedback, manual entry, resource cleanup|
|T-VOICE-03|Edit transcription then reparse|New text used; confirm overwriting edited candidates|
|T-VOICE-04|Missing grams/meal/type/minutes|Required missing fields block save; no guessed meal/grams|
|T-VOICE-05|Relative/future dates/multiple activities|Explicit date context, reject future, no silent activity omission|
|T-QA-01|Eligible general question|Real provider answer with nonmedical notice|
|T-QA-02|Direct HTTP after consent withdrawal|403; ordinary summary remains readable|
|T-QA-03|Withdraw during processing|Suppress provider result; do not claim prior transmission recalled|
|T-QA-04|Medical/out-of-scope/injection text|Refusal/safe boundary; no other users' records|
|T-QA-05|Inspect provider request|No raw health answers/passwords/identity/full private database|
|T-AI-01|Unconfigured/429/refusal/timeout|Isolated error/retry; M1 manual entry works|
|T-AI-02|Invalid JSON/negative/nonexistent IDs|Schema/business rejection, no crash/direct save|
|T-AI-03|Account switch/media unmount|No old-response contamination; tracks stopped/URLs cleaned|

## 19.5 Suggested test layout

```text
backend/src/test/java/com/fitnutri/backend/
  support/PostgresIntegrationSupport.java
  migration/LegacyMigrationTest.java
  security/SessionCsrfIntegrationTest.java
  profile/OnboardingIntegrationTest.java
  profile/WeightHistoryIntegrationTest.java
  records/FoodCalculationTest.java
  records/ExerciseEstimateTest.java
  records/FoodBatchIntegrationTest.java
  records/RecordCorrectionIntegrationTest.java
  submission/ConcurrentSubmissionIntegrationTest.java
  summary/SummaryConsistencyIntegrationTest.java
  admin/AccountStatusIntegrationTest.java
  ai/ProviderContractTest.java
  qa/QaPrivacyIntegrationTest.java
frontend/tests/
  legacy-demo/              # Retain original tests/isolated mock fixtures, excluded from production
  unit/draftReducer.test.ts
  unit/units-and-dates.test.ts
  contract/api-decoders.test.ts
  component/auth-and-profile.test.tsx
  component/reference-search.test.tsx
  component/review-recovery.test.tsx
  e2e/core-flow.spec.ts
  e2e/submission-recovery.spec.ts
  e2e/record-management.spec.ts
  e2e/profile-admin.spec.ts
  e2e/advanced-contract.spec.ts
```

Validate Java test APIs/imports against actual Boot4. Class names above are targets, not existing-file claims. Never disable every integration test for missing dependencies and call the task complete.

## 19.6 Result format and completion

Each result includes taskId/testId, command, time, commit, environment, database isolation, pass/fail/skip counts, failure reasons and evidence paths. Synthetic screenshots exclude real accounts/sensitive answers.

**IN_REVIEW requires:** code/contracts agree, corresponding automatic tests actually pass, no unexplained destructive security/DB effects, updated BC/migration/source ledgers, and at least relevant real-interface verification. **DONE** additionally requires workflow acceptance at the appropriate gate, not merely function units.

Mock contract PASS and actual-provider NOT RUN may coexist. Report both rather than displaying only green results.

---

# 20. CI, startup, deployment and operational delivery

## 20.1 Proposed package scripts and check commands

Retain the Demo's Node tests under the meaningful name `test:legacy-demo`; validate tooling/lockfile compatibility when adding unit/component/E2E scripts. These are **target commands to provide after implementation**, not necessarily existing commands:

```text
frontend: npm ci
frontend: npm run lint
frontend: npm run build
frontend: npm run test:legacy-demo
frontend: npm run test:unit
frontend: npm run test:contract
frontend: npm run test:component
frontend: npm run test:e2e
repo:     node scripts/check-production-boundaries.mjs
backend:  ./mvnw verify
repo:     ./scripts/check.sh
```

This source/environment review actually ran only the packaged Demo npm test (15/15). Do not claim these future commands have run. Create/test missing scripts rather than documenting fictitious availability.

Use Boot-compatible BOM/version management for POM test/migration dependencies. Do not downgrade Boot4.1.1 to3.x for one compilation error. Record unavailable dependency-resolution networking as a blocker instead of inventing libraries.

## 20.2 CI stages

1. Static: diff/format checks, TypeScript/lint, API schemas and production dependency boundaries.
2. Frontend: existing legacy/domain tests, contract/component tests and production build.
3. Backend: compilation, unit tests, PG16 integration and Flyway empty/legacy migration checks.
4. E2E: start isolated DB/application, await readiness, exercise M1/support flows and preserve failure screenshots/traces.
5. M2 contracts: mock-provider checks; authorised manual workflow/local execution for real-provider smoke to avoid paid calls on every PR.
6. Delivery: real build artefacts, runbook and verification report only; exclude .env, dumps, original audio and real health data.

GitHub Enterprise Actions/runner support is unverified. R00 first checks available capabilities; if no runner exists, provide equivalent local scripts/manual review without fabricating remote CI success. Verify action versions/runner images during implementation and pin tested versions/commits; do not curl|bash unknown external scripts.

## 20.3 Environment variable inventory

|Configuration|Holder|Purpose|Default/security requirement|
|---|---|---|---|
|DB_URL/DB_USERNAME/DB_PASSWORD|Backend/migration environment|PostgreSQL connection|Placeholders only in .env.example; real passwords never in Git|
|APP_DEFAULT_ZONE|Backend/public capabilities|Default date context|Australia/Sydney; validate request-specific zones|
|APP_ALLOWED_ORIGINS|Backend|CORS allowlist|Local localhost:5173; explicit deployment domains|
|SESSION_COOKIE_SECURE|Backend|HTTPS Cookie|Production true; explicit local-http profile differences|
|VITE_API_BASE_URL|Public frontend|API path|Recommend /api; not secret|
|VITE_FEATURE_*|Public frontend|Further feature disabling|Cannot override server unavailability|
|OPENAI_API_KEY|Backend secret only|Provider authentication|No default key or log output|
|AI_IMAGE_MODEL/AI_TRANSCRIPTION_MODEL/AI_TEXT_MODEL|Backend|Verified model configuration|No hardcoded latest-model assumption|
|AI_FEATURES_ENABLED|Backend|Allow external processing|Consistent with capabilities; may be per feature|
|AI_TIMEOUT_* / MAX_*_BYTES|Backend|Resource/time limits|Synchronise proxy/servlet/business layers|
|TEST_DB_*|Tests only|Isolated test database|No fallback to main development/shared DB|

Current application.properties is not evidence of deployment security. If actual credentials are found, report file locations without reproducing them; the responsible owner decides rotation. Do not change shared accounts without authorisation.

## 20.4 Startup order and health checks

```text
Confirm environment and BASE_COMMIT
  → Point to isolated/authorised DB
  → Preflight + backup (existing database)
  → Apply approved Flyway migrations
  → Import reviewed reference manifest
  → Start Java; verify schema, catalogue readiness and auth/csrf
  → Start Vite localhost:5173, or deploy static dist + same-origin API proxy
  → Run real new-account core smoke
  → Confirm missing AI configuration affects only AI entry points, not M1
```

Readiness requires more than an open process port: at least database connectivity, successful migration and required catalogue state. Do not expose complete Actuator environment/secret endpoints; reuse health capabilities or add minimal internal readiness.

## 20.5 Production-style proxy checks

- Route /api/* to the backend, preserving JSON errors rather than index.html.
- Refreshing frontend routes such as /history and /profile returns the SPA entry; unknown static assets return404, not disguised HTML assets.
- Browser API is same-origin with correct Cookie path/domain/sameSite/secure; do not mix localhost and127.0.0.1.
- Multipart limits/timeouts accommodate images/audio; frontend explains gateway413/504.
- Cache static assets; prevent shared-cache leakage for authentication/profile/private-record/CSRF responses.
- Provider failures have requestId-correlated backend logs without raw audio/images/health answers.
- Do not use Vite dev server as a public production service; describe development/demo risks separately.

## 20.6 Handling failed migrations

Stop a failing migration first. Do not delete migration history or alter applied SQL checksums. Fix through a new migration, or correct an explicitly unapplied migration only in a personal test database, respecting actual Flyway state.

Rollback does not mean dropping every new column. Retain forward-compatible columns by default and verify old-app/new-schema compatibility. For irreversible semantic changes, restore into a **new isolated database** and switch manually, never overwrite an active shared database directly.

Recovery rehearsal records backup timestamp, schema version, redacted source/target databases, row counts/key sums, business smoke and evidence that the original database was untouched. An unrehearsed backup is not verified recoverable.

---

# 21. Effort, parallel work and critical path

## 21.1 Estimation basis

Summing69 task cards yields **effective human development/review effort**, not AI execution time, calendar commitments or completed hours. Scope includes necessary backend/database corrections, M1 support pages, M2 services and formal testing, exceeding the earlier frontend-only9–11-day preliminary plan.

Implementation-task tests are local developer checks; Q tasks provide independent cross-module regression/fault injection/acceptance infrastructure. Do not count the same hour twice. Update estimates for actual differences/reuse.

|Category|Tasks|Estimated hours|Main content|
|---|---:|---:|---|
|P: preflight/contracts/rules|4|7–13|Branch, environment, OpenAPI, ledger|
|D: database|6|20–39|Migrations, source versions, legacy data, receipts/constraints|
|B: core backend/support|16|52–98|Session, profile, records, calculation, summaries, admin|
|F: frontend integration|19|54–93|Reuse pages; replace data wiring/async/reliability|
|A: advanced features|11|35–63|Real images, transcription, extraction, QA, smoke|
|Q: testing/acceptance|9|32–55|PG concurrency, UI, E2E, multiple browsers|
|R: CI/deployment/recovery/delivery|4|11–20|Reproducible checks, startup, backup/restore, sign-off|
|**Total**|**69**|**211–381**|Entire M1+support+M2+delivery scope|

At8 effective hours/day this is approximately **26.4–47.6 person-days**, not consecutive calendar days or a reliable four-person schedule obtained by dividing by4. AI may reduce coding time, but conflicts/contracts/provenance/real environments/review remain.

**Frontend F work alone is54–93 hours**, not total engineering effort. P00 should reuse completed BC corrections and remove duplicate implementation tasks; extra learning/source preparation/environment maintenance increases estimates.

M2 A tasks plus dedicated Q07 total about38–69 hours; excluding them leaves M1/support/shared delivery about173–312 hours. This is a budget view; shared tasks still need post-M2 regression without recounting completed infrastructure.

## 21.2 Suggested roles (team confirmation required)

|Workstream|Suggested role|Delivery boundary|
|---|---|---|
|Requirements/contracts/acceptance coordination|User as architecture/product owner|Approve scope, ADR, BC and target branch|
|Backend/DB agent or developer|Backend owner|D/B, retaining package/database|
|Frontend agent or developer|Frontend owner|F, preserving Demo appearance with standard APIs|
|Advanced features|Respective feature owners|A, aligned with existing allocation without fabricated contributions|
|QA/review|Independent QA/second reviewer|Q/R; rerun rather than accepting verbal conclusions|

Proposal member assignments remain the historical baseline; do not rewrite actual contributions here. Map suggested roles to team-approved people/agents before execution.

## 21.3 Parallel and coordinated work

Parallelisable: after P02 contracts freeze, F00/F01/F05 alongside D/B foundations; food/exercise services after shared ID/date/receipt contracts; A01 images, A03 transcription and A07QA after M1 stability.

Not safely parallel without coordination: concurrent edits to package-lock/POM/SecurityConfig/OpenAPI/shared types; independently allocated migration numbers; recordDate versus recordedAt divergence without adapters.

Assign one integration owner per shared file; other features propose small PRs. Each AI worktree runs its own tests without overwriting one shared directory.

## 21.4 Critical path and stage gates

```text
P00/P01/P02/P03
  → D00/D01/D02/D03/D04 + Q00
  → B01/B02/B03/B04 + B05/B06/B07/B08
  → B09/B10/B11/B13 + F03/F06/F08/F09/F10/F11
  → Q02/Q03/Q05: M1 core gate
  → B12/B14 + F12–F17 + Q06: support gate
  → A00–A09 + Q07 + A10: real M2 gate
  → D05/R01/R02/Q08/R03: final delivery gate
```

This is not the only topological order: D05 can rehearse after entities/backfill stabilise, and UI can develop against contracts. Shared contracts precede implementation, database safeguards precede risky migrations, and real foundations precede advanced model calls.

Report which real flow can be demonstrated today, rather than component counts. For blocked authorisation/external-data tasks, record who must provide what while continuing independent work.

---

# 22. Stage completion criteria and sign-off

## 22.1 M1 CORE sign-off

All conditions are required to claim a genuinely connected core MVP:

- New USER registers/logs in/completes questionnaire through real sessions and can continue without QA consent.
- Two foods in different meals use real references/grams through Review; cancel writes nothing; batches save consistently with duplicate protection/failure recovery.
- Exercise uses real presets, dates/minutes and historical weight or explicit unavailability, never fallback70kg.
- Dashboard displays server-confirmed values on correct dates, surviving refresh/logout/login; past dates do not pollute today.
- Date, kJ, provenance, snapshots, role/owner and CSRF satisfy contracts.
- Real M1 E2E and key PG transaction/concurrency/receipt tests pass, with no production mock fallback.
- Isolated migration verification and existing-data protection/recovery plan exist; missing official provenance prevents claiming full FR15 acceptance.

Backend-contract pass without browser execution means “API ready; frontend not accepted”; clickable mock UI means “UI ready; real saving not accepted”.

## 22.2 M1 SUPPORT sign-off

History, Profile, trends/rule summaries and admin support real reads/updates. Date moves/edits/deletes refresh derived views; same-day weight correction, target clearing, units and version conflicts work. Although the Proposal deferred some features to later Sprints, the existing Demo already has their interactions, so retain and connect them rather than removing them.

## 22.3 M2 FULL sign-off

Provide real external-service success evidence for images, food speech, exercise speech and QA; pass failure/permission regressions; all candidates share Review; manual entry/statistics remain usable during provider outages.

Configured keys are not successful calls; mock contracts are not real-provider passes; returned LLM text is not proof of correct recording/eligibility/privacy. Supply separate evidence.

## 22.4 Final deliverables

|Deliverable|Project location|Verification|
|---|---|---|
|Buildable real frontend|frontend/|npm ci/build/lint/test|
|Standard services/tests|backend/|mvnw verify + PG integration|
|Migrations/source manifests|backend/resources + docs/db|Empty/legacy migration and restore reports|
|OpenAPI/examples|docs/api/|Schema validation matches HTTP examples|
|BC rationale/changes/impact|docs/implementation/backend-change-log.md|Traceable commits/PRs|
|Task ledger|docs/implementation/tasks.md|Actual state/dependencies/evidence for69 cards|
|ADR decisions|docs/adr/|Confirmed tables/contracts/security-role enhancements|
|Test/browser evidence|docs/verification/|Explicit environment/results/unverified items|
|Startup/deployment/recovery|README.md, docs/runbook.md|Reproducible in a new environment|
|Demo provenance|docs/implementation/source-map.md|Traceable original files/migrated symbols|

Avoid unexplained completion percentages. Report each item as UI complete / API integrated / verified / blocked.

---

# 23. Coding-agent execution and startup prompt

## 23.1 Recommended location

Place this document at:

```text
<repository>/docs/implementation/FitNutri_AI_Integration_Execution_Plan.md
```

Keep Demo sources in a read-only reference directory or accessible external path; ZIPs/the entire mock program need not become production commits. Use a real git checkout and never overwrite an existing working tree with a ZIP. Proposal may reside in docs/reference or an external read-only path.

## 23.2 Sole startup instruction (copyable; self-contained, no old prompts)

```text
You are the FitNutri implementation agent. You have the sole specification
  docs/implementation/FitNutri_AI_Integration_Execution_Plan.md
and the user's local project directory and FitNutri_Demo_Source.zip.
Course Lab9 material is a testing-method reference.
Do not locate, load or depend on old startup prompts or another execution Markdown.

FIRST PRIORITY: Before any write, read-only verify the local Git working tree
as specified in Section0.0. Work only on
  develop/yuankang-wei
If that local branch is absent, only a ZIP without .git exists, switching is unsafe,
or uncommitted files may conflict, stop writes and ask the user. Do not use main
or initialise Git yourself. Do not access university GitHub, fetch/pull/clone,
or replace supplied sources from a remote.

Only after Branch Gate=PASS execute P00–P03 and continue according to Chapter18.
Preserve Demo UI and independent Review, replacing mock store/localStorage
business data with real React + Axios/React Query → Java21/Spring Boot4/
Spring Security Session → PostgreSQL.
Register rationale, impacts, migrations and tests for every backend/DB change by BC ID.

SECOND MANDATORY GATE: Each business task includes appropriate JUnit5/Mockito,
MockMvc, isolated PG integration, frontend unit/component/contract or E2E tests.
Define verifiable assertions, implement, self-test, fix, review the diff and make
small local commits on develop/yuankang-wei. Q00–Q08 do not justify postponing
business tests. Never fabricate PASS. The instructor requires suitable methods
and explainable coverage, not an invented mandatory95% grading threshold.

Every self-review receipt includes task ID, changed files, BC/DB impacts,
test paths, actual commands/counts/pass/fail/skip, branch, HEAD/commit,
blockers and next task. Record Developer Tests separately from Peer Review;
AI cannot sign for others. No push/merge, force operations, database clearing,
overwriting existing work or treating demo seeds as real data.

First report working directory, real branch, BASE_COMMIT, existing changes,
ZIP differences and test baseline. With Branch Gate PASS, execute P00–P03
and subsequent eligible tasks. Do not repeatedly write plans instead of code;
stop affected tasks only for hard safety/data/permission blockers.
Provide separate real evidence for M1 CORE+SUPPORT and M2 FULL.
Missing model keys/providers must be reported honestly as BLOCKED_EXTERNAL.
```

## 23.3 Standard input for subsequent rounds

```text
Tasks authorised this round: F07, F08 (example).
Baseline: <verified commit>.
Completed dependencies: <actual task-ledger states>.
Writable files: <task-card files and necessary tests>.
Prohibited: remote push/merge, shared DB writes, unrelated dependency upgrades.
Deliver: code diff, local commit, actual test results, updated task/BC ledgers, blockers.
```

Do not make all69 changes in one unbounded round. Each round must be reviewable by another person/agent; prioritise separate reviews for migrations/security rules.

## 23.4 Mandatory execution receipt per round

```text
Task IDs:
Project root / Branch Gate: PASS / BLOCKED
Required/current branch: develop/yuankang-wei / <actual git output>
Baseline commit / HEAD:
Pre-existing changes protection:
Changed files:
Source reuse: original Demo symbol → new file
Backend changes: BC-xx / none
Database changes: migration ID / none
Contract changes:
Paired tests: test files and meaningful assertions (by feature)
Developer tests actually run: commands, exit codes, test counts, evidence paths
Real API/PG/E2E: PASS / FAIL / BLOCKED / NOT RUN
Peer review / UAT: PENDING unless independently confirmed
Not run / blockers:
Local commits (only on develop/yuankang-wei):
Remote state: not pushed unless explicitly authorised
Next eligible tasks:
```

When context is insufficient, read ledgers/dependencies rather than guessing fields from chat memory. Record discrepancies between plan/latest branch in ADR/change logs with rationale. Confirm safety/data/scope conflicts; equivalent naming adjustments may be recorded and continued.

## 23.5 Stop conditions and final round

Unknown database FKs, irreversible migrations, potential real-data loss or missing required authorisation stop the affected scope; automatic AI execution does not bypass these risks.

Run full checks/real smoke before final evidence updates. Unfinished M2 or actual Safari remains explicitly NOT RUN/BLOCKED, never vaguely “mostly working”.

The aim is not maximum code volume: every change needs clear user behaviour, API contracts, data guarantees, test evidence and reviewable commits.

---

# Appendix A. Auditable source evidence index

This appendix describes only the uploaded ZIP, locating code rather than proving equality with the latest remote branch. Line numbers/short hashes refer to the extracted attachment. If hashes differ, compare content before choosing reuse/revision. Short hashes are the first12 SHA-256 characters; formal baselines retain full values.

## A.1 Key Demo files

| Original file | Lines | First12 SHA-256 characters | Main migration target |
|---|---:|---|---|
| `package.json` | 41 | `de64938b3578` | Dependencies/scripts; do not overwrite the main lockfile |
| `src/App.tsx` | 282 | `501b88b1f73b` | Header/Protected/full-store subscription/routing/DemoTools |
| `src/main.tsx` | 47 | `bf402b309fe8` | MUI theme/BrowserRouter/StrictMode |
| `src/context.tsx` | 14 | `4bd20cf867dd` | Context containing Database, requiring separation |
| `src/account.tsx` | 737 | `ed2784fa535b` | Login/three-step questionnaire/Profile |
| `src/entry.tsx` | 992 | `50c50f551f19` | AddRecord/FoodEditor/ExerciseEditor/Entry/Review |
| `src/overview.tsx` | 852 | `8363d4864818` | Records/Dashboard/History/charts/summaries |
| `src/extras.tsx` | 522 | `2d96634e3cd1` | Admin/Chat/DemoTools |
| `src/domain.ts` | 574 | `5066cc75f7d1` | Types/formulas/dates/commitDraft/version rules |
| `src/store.ts` | 394 | `b2b838464aef` | localStorage service, mock login, QA/parsing |
| `src/seed.ts` | 255 | `0f81fd85625e` | 15 demo foods,8 activities and synthetic users |
| `src/ui.tsx` | 213 | `29a3757a2fd6` | Reusable UI/focus/error helpers |
| `src/styles.css` | 1805 | `4574c18fc2aa` | Main visual styles |
| `tests/domain.test.ts` | 229 | `32747b8e83e8` | Retain as mock-rule regression, not HTTP integration |
| `tests/service.test.ts` | 164 | `82a9a6f411b4` | Retain as mock-rule regression, not HTTP integration |

## A.2 Main-project runtime/frontend baseline

| Original file | Lines | First12 SHA-256 characters |
|---|---:|---|
| `README.md` | 66 | `0d85caf4a077` |
| `.nvmrc` | 1 | `68ca3fba3b7e` |
| `docker-compose.yml` | 15 | `84240f2b0af0` |
| `backend/pom.xml` | 130 | `81a79758a125` |
| `backend/.mvn/wrapper/maven-wrapper.properties` | 3 | `488e1b3f2e64` |
| `backend/src/main/resources/application.properties` | 10 | `07c91dbbf4b6` |
| `frontend/package.json` | 41 | `15d310eabaa3` |
| `frontend/src/App.tsx` | 7 | `ce1f74605832` |
| `frontend/src/main.tsx` | 20 | `f1af8019738e` |
| `frontend/src/api/client.ts` | 9 | `e52c12c514df` |
| `frontend/src/api/auth.ts` | 38 | `a116195c131d` |
| `frontend/src/pages/LoginPage.tsx` | 79 | `9c9f451ad05a` |
| `frontend/tsconfig.app.json` | 26 | `8e5d12ba330e` |
| `frontend/vite.config.ts` | 7 | `e1eefd0ed163` |

Do not reproduce application configuration here, avoiding potential credentials. Inspect only in authorised environments and redact logs/reports.

## A.3 Complete Java main-source inventory

Paths are relative to `backend/src/main/java/com/fitnutri/backend/`. This inventory prevents checking Controllers alone while missing Repositories, DTOs or standalone summary algorithms.

| Original file | Lines | First12 SHA-256 characters |
|---|---:|---|
| `BackendApplication.java` | 13 | `ee3aeafafdb1` |
| `admin/AdminController.java` | 56 | `9dae823f6d0b` |
| `admin/AdminUserSummary.java` | 25 | `3a2608ff9e9a` |
| `admin/ResetPasswordRequest.java` | 14 | `da81dfeadbde` |
| `common/ApiExceptionHandler.java` | 51 | `c1458c1293f7` |
| `common/ZoneResolver.java` | 25 | `ed094bae7ac2` |
| `food/FoodCandidate.java` | 4 | `6b9ec5ab3848` |
| `food/FoodRecognitionController.java` | 50 | `39283317e3e4` |
| `food/FoodRecognitionResponse.java` | 6 | `a1bc912e5d8d` |
| `food/OpenAiVisionService.java` | 124 | `bc7b4724162f` |
| `foodreference/FoodNutritionResponse.java` | 15 | `c9cae84fe740` |
| `foodreference/FoodReference.java` | 30 | `5195a7684d53` |
| `foodreference/FoodReferenceController.java` | 113 | `15b4eff08e33` |
| `foodreference/FoodReferenceDataLoader.java` | 38 | `4f135a43879f` |
| `foodreference/FoodReferenceRepository.java` | 6 | `74f2466bee4e` |
| `foodreference/FoodReferenceResponse.java` | 20 | `e5f7d0156e08` |
| `foodreference/FoodSearchResponse.java` | 12 | `c531452c7b28` |
| `profile/ActivityLevel.java` | 5 | `9e2236904dd6` |
| `profile/AddWeightRequest.java` | 17 | `af9e38c2ffa7` |
| `profile/Goal.java` | 5 | `7e4c70e8e4fa` |
| `profile/HealthSafetyAnswer.java` | 5 | `882dbe6a1465` |
| `profile/ProfileController.java` | 116 | `44caeef78b3d` |
| `profile/ProfileResponse.java` | 38 | `2290afbe98e8` |
| `profile/QaEligibility.java` | 33 | `d9407e1fdcb1` |
| `profile/Sex.java` | 5 | `4f94bfed9b44` |
| `profile/UpdateProfileRequest.java` | 28 | `cdacba935850` |
| `profile/UserProfile.java` | 55 | `ead940cf9487` |
| `profile/UserProfileRepository.java` | 6 | `05c5617d6933` |
| `profile/WeightController.java` | 73 | `f1fc498a2781` |
| `profile/WeightEntry.java` | 52 | `f64dae1f60e4` |
| `profile/WeightEntryRepository.java` | 11 | `09f8c8b460b4` |
| `profile/WeightEntryResponse.java` | 10 | `6e60f0ae5af5` |
| `records/BaseRecord.java` | 32 | `e6b987a8d3ad` |
| `records/CreateExerciseRecordRequest.java` | 26 | `299533a2868d` |
| `records/CreateFoodRecordRequest.java` | 21 | `88303bfb7624` |
| `records/ExerciseCatalog.java` | 55 | `ef0cff16e833` |
| `records/ExerciseRecord.java` | 27 | `34e74e6cbf49` |
| `records/ExerciseRecordController.java` | 165 | `0a71d010d74e` |
| `records/ExerciseRecordRepository.java` | 9 | `26da4f4599b7` |
| `records/ExerciseRecordResponse.java` | 20 | `ccedec225f57` |
| `records/FoodRecord.java` | 37 | `be0eb62dfcfb` |
| `records/FoodRecordController.java` | 134 | `98088509142e` |
| `records/FoodRecordRepository.java` | 9 | `6c5bf92e305e` |
| `records/FoodRecordResponse.java` | 25 | `268b74eb8e02` |
| `records/Intensity.java` | 5 | `df2897e960fd` |
| `records/MealType.java` | 5 | `e09fd91a81ad` |
| `records/RecordSource.java` | 5 | `35b618c6f5c8` |
| `security/AppUserDetailsService.java` | 32 | `a262350c1ddc` |
| `security/SecurityConfig.java` | 67 | `3529aa04f37d` |
| `summary/CalorieTargetCalculator.java` | 65 | `f0bf4ea801e7` |
| `summary/DataSummaryController.java` | 388 | `d9bb83515d67` |
| `summary/DaySummary.java` | 13 | `999d3fe10cd1` |
| `summary/TodaySummaryController.java` | 207 | `54b4b6c1324f` |
| `summary/TodaySummaryResponse.java` | 16 | `070372b3427a` |
| `summary/TrendsResponse.java` | 25 | `ed47864ffb69` |
| `user/AuthController.java` | 89 | `4baa2094be3a` |
| `user/LoginRequest.java` | 10 | `b639436d18c4` |
| `user/RegisterRequest.java` | 17 | `018b3b5414ca` |
| `user/User.java` | 43 | `b0a6a13a6495` |
| `user/UserRepository.java` | 10 | `86e0f5b592ae` |
| `user/UserResponse.java` | 8 | `870d7e22706a` |

## A.4 Original32 business HTTP mappings

These mappings describe the inspected sources, not implemented new APIs or passing runtime results. Chapter8 defines new contracts. Explicitly retain/adapt/restrict every original route without overlooking unsafe old write entry points.

| Method | Current path | Original Controller:line |
|---|---|---|
| GET | `/api/admin/users` | `admin/AdminController.java:23` |
| PATCH | `/api/admin/users/{id}/deactivate` | `admin/AdminController.java:30` |
| PATCH | `/api/admin/users/{id}/reactivate` | `admin/AdminController.java:37` |
| POST | `/api/admin/users/{id}/reset-password` | `admin/AdminController.java:44` |
| POST | `/api/food/recognize` | `food/FoodRecognitionController.java:27` |
| GET | `/api/food-reference/search` | `foodreference/FoodReferenceController.java:21` |
| GET | `/api/food-reference/{publicFoodKey}` | `foodreference/FoodReferenceController.java:59` |
| GET | `/api/food-reference/{publicFoodKey}/nutrition` | `foodreference/FoodReferenceController.java:66` |
| GET | `/api/profile` | `profile/ProfileController.java:25` |
| PUT | `/api/profile` | `profile/ProfileController.java:33` |
| DELETE | `/api/profile/goal` | `profile/ProfileController.java:70` |
| DELETE | `/api/profile/energy-goal` | `profile/ProfileController.java:81` |
| POST | `/api/profile/weight` | `profile/WeightController.java:32` |
| GET | `/api/profile/weight` | `profile/WeightController.java:61` |
| GET | `/api/records/exercise/types` | `records/ExerciseRecordController.java:41` |
| GET | `/api/records/exercise/estimate` | `records/ExerciseRecordController.java:47` |
| POST | `/api/records/exercise` | `records/ExerciseRecordController.java:59` |
| GET | `/api/records/exercise` | `records/ExerciseRecordController.java:73` |
| PUT | `/api/records/exercise/{id}` | `records/ExerciseRecordController.java:96` |
| DELETE | `/api/records/exercise/{id}` | `records/ExerciseRecordController.java:114` |
| POST | `/api/records/food` | `records/FoodRecordController.java:29` |
| GET | `/api/records/food` | `records/FoodRecordController.java:51` |
| PUT | `/api/records/food/{id}` | `records/FoodRecordController.java:76` |
| DELETE | `/api/records/food/{id}` | `records/FoodRecordController.java:116` |
| GET | `/api/summary/daily` | `summary/DataSummaryController.java:91` |
| GET | `/api/summary/weekly` | `summary/DataSummaryController.java:159` |
| GET | `/api/summary/today` | `summary/TodaySummaryController.java:43` |
| GET | `/api/summary/trends` | `summary/TodaySummaryController.java:77` |
| POST | `/api/auth/register` | `user/AuthController.java:31` |
| POST | `/api/auth/login` | `user/AuthController.java:49` |
| POST | `/api/auth/logout` | `user/AuthController.java:73` |
| GET | `/api/auth/me` | `user/AuthController.java:80` |

## A.5 Executed and unexecuted work in this review

This review ran `npm test` in an extracted Demo copy, executing the original `node --experimental-strip-types --test tests/*.test.ts`:15 tests,15 pass,0 fail,0 skipped, under Node22.16.0/npm10.9.2. Coverage is limited to its local domain/store.

This review did not integrate code into GitHub, modify the user database or run the target Java app/migrations/production build/browser E2E/real providers. All target tasks initially remain TODO. Original files were unchanged; only this Markdown plan was generated.

---

# Appendix B. Proposal → task → acceptance traceability

This matrix traces requirements, not completion. Original Proposal terms/scope take precedence; receipts, CSRF bootstrap and capabilities are justified implementation designs, not purported original Proposal items.

| Requirement | Implementation focus | Tasks | Representative acceptance |
|---|---|---|---|
| FR01 Login and Logout | Real sessions, role/questionnaire routing, logout invalidation | B01/B02/F03/F04 | T-AUTH-04/05/06 |
| FR02 Role-Based Access Control | Server roles/ownership; admin cannot view private records | B00/B02/B11/B14 | T-AUTH-03/07、T-ADMIN-01 |
| FR03 Initial Questionnaire | Three-step form, adulthood, atomic initial weight/completed | B03/F06 | T-PROFILE-01/02/03 |
| FR04 Health-Safety/Q&A Eligibility | Derived from current answer+consent; QA restrictions do not block entry | B03/A07/A08/F14/A09 | T-QA-02/03/05 |
| FR05 Profile and Display Settings | Versions, units, name/settings readback | B03/F14/F01 | T-PROFILE-04/05 |
| FR06 Body Data and Weight History | Same-date corrections, latest by measurement date, unchanged old snapshots | B04/F14 | T-WEIGHT-01/02/03 |
| FR07 General Goals and Energy References | Nullable/removable goals, no automatic prescription | B03/B13/F14/F15 | T-SUM-03 |
| FR08 Email-Based Self-Registration | USER-only, case-insensitive email uniqueness | B01/F04 | T-AUTH-01/02/03 |
| FR09 User Account Status Management | Deactivate/reactivate/confirm/version | B14/F16 | T-ADMIN-02 |
| FR10 Manual Food Intake Entry | Real references, multiple items, independent meals/dates | B05/B07/B09/F07/F08 | T-FOOD-01/02/04 |
| FR11 Manual Exercise Entry | type/date/minutes, historical weight, saveable unavailability | B06/B10/F09 | T-EX-01/02/03/04 |
| FR12 Voice-Based Food Intake Entry | Real recording/transcription, missing fields, shared Review | A03/A04/A05/A06 | T-VOICE-01/02/03/04 |
| FR13 Voice-Based Exercise Entry | Real recordings/presets/minutes, shared estimates | A03/A04/A05/A06/B10 | T-VOICE-01/04/05 |
| FR14 Image-Based Food Intake Entry | Real candidates/item corrections, no confidence percentages | A01/A02 | T-IMAGE-01/02/03 |
| FR15 Australian Food Reference Matching | AUSNUT provenance/version, immutable IDs, server calculation | D03/B05/B07/B09 | T-REF-01/02、T-FOOD-04/07 |
| FR16 Shared Review/Editing/Cancellation | Five paths share Draft/Review; cancellation writes nothing | F05/F08/F09/F10/A02/A06 | T-FOOD-01、T-STATE-03 |
| FR17 Confirmation and Consistent Saving | Batch transactions, UUID/receipts, rollback, backdating | B08/B09/B10/F10 | T-IDEM-01–06、T-FOOD-03 |
| FR18 Daily Overview | Today confirmed data, meals, minutes, absent goals, partial data | B13/F11 | T-SUM-01/04、Q05 |
| FR19 Viewing and Filtering History | owner/date/type, details, empty states | B11/F12 | T-HISTORY-01/02 |
| FR20 Historical Statistics and Trends | Consistent basis, missing-day nulls, units | B13/F15 | T-SUM-01/02 |
| FR21 Correcting and Deleting Records | Versions, recalculation, date moves, confirmation | B12/F13 | T-EDIT-01/02/03、T-DELETE-01 |
| FR22 Rule-Based Summaries | No LLM, actual facts, mean denominators, current goals | B13/F15 | T-SUM-01/02/03 |
| FR23 General Health AI Q&A | Per-request eligibility, nonmedical boundary, minimal context, failure isolation | A07/A08/A09/A10 | T-QA-01–05 |
| NFR01 Security | hash/session/CSRF/roles/owner, no private leakage | B00/B01/B02/B14/Q03 | T-AUTH、T-ADMIN |
| NFR02 Privacy | High-level answers, current consent, no raw answers sent to model | B03/A07/A08/Q07 | T-QA-02/03/05 |
| NFR03 Performance/Feedback | Measure ordinary operations around3 seconds; immediate AI feedback/timeouts | F02/F11/A00/Q08 | performance records、T-AI-01 |
| NFR04 Usability | Units/field errors/dates/shared flow/confirmation | F05–F17/Q04/Q08 | T-STATE、T-FOOD、T-DELETE |
| NFR05 Reliability/Consistency | Snapshots/transactions/retries/edits/deletes/missing versus0 | D04/B08–B13/Q02/Q06 | T-IDEM、T-EDIT、T-SUM |
| NFR06 Web Compatibility | Actual Chrome/Edge/Safari; manual fallback on microphone denial | Q08/A05/A06 | browser-matrix、T-VOICE-02 |

Requirement locations in the Proposal PDF: physical pages3–9 FR,10–11 NFR,12–16 data design,17–23 prototypes,24–26 plans/assignments. Physical numbering includes the cover and differs from printed footer numbering.

---

# Appendix C. Official technical references and boundaries

These references support implementation-method checks, not replacement of source code, Proposal or future runtime evidence. Consulted2026-10-16; recheck against locked branch versions when coding. Use official/browser API documentation rather than outdated third-party tutorials for Boot4/Router7 interfaces.

| Reference | Material | Use in this plan |
|---|---|---|
| DOC-SEC | Spring Security Servlet Session Management：`https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html` | Custom-authentication session strategies and explicit context persistence |
| DOC-CSRF | Spring Security Servlet CSRF：`https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html` | Session tokens, request handlers, login/logout token lifecycle |
| DOC-DB | Spring Boot Database Initialization：`https://docs.spring.io/spring-boot/how-to/data-initialization.html` | Flyway/initialisation responsibilities and PostgreSQL module; verify dependencies for Boot4 |
| DOC-PG | PostgreSQL16 INSERT：`https://www.postgresql.org/docs/16/sql-insert.html`；Transaction Isolation：`https://www.postgresql.org/docs/16/transaction-iso.html` | Unique conflicts, ON CONFLICT, real transaction tests |
| DOC-QUERY | TanStack Query v5 Query Cancellation：`https://tanstack.com/query/v5/docs/framework/react/guides/query-cancellation` | queryFn consumes AbortSignal |
| DOC-INVALIDATE | TanStack Query v5 Invalidations from Mutations：`https://tanstack.com/query/v5/docs/framework/react/guides/invalidations-from-mutations` | Invalidate/refetch related queries after saving |
| DOC-VITE | Vite Server Options：`https://vite.dev/config/server-options` | Development /api proxy, distinct from production proxy |
| DOC-ROUTER | React Router createBrowserRouter：`https://reactrouter.com/api/data-routers/createBrowserRouter` | Data router from the same Router package, created once |
| DOC-BLOCK | React Router useBlocker：`https://reactrouter.com/api/hooks/useBlocker`；Navigation Blocking：`https://reactrouter.com/how-to/navigation-blocking` | In-app navigation blocking; hard refresh needs additional handling |
| DOC-RECORDER | MDN MediaRecorder.isTypeSupported：`https://developer.mozilla.org/en-US/docs/Web/API/MediaRecorder/isTypeSupported_static` | Actual-browser recording MIME capability detection |
| DOC-AUDIO | OpenAI File Transcription：`https://developers.openai.com/api/docs/guides/speech-to-text`；Create Transcription：`https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create/` | Backend file transcription; verify formats/model fields against actual capability |
| DOC-STRUCT | OpenAI Structured Outputs：`https://developers.openai.com/api/docs/guides/structured-outputs`；Chat Completions Create：`https://developers.openai.com/api/reference/resources/chat/subresources/completions/methods/create/` | Schema versus JSON mode; still handle refusals/truncation |
| DOC-PLAYWRIGHT | Playwright Browsers：`https://playwright.dev/docs/browsers` | Automated browser coverage versus actual Safari acceptance |
| DOC-GIT | Conventional Commits 1.0：`https://www.conventionalcommits.org/en/v1.0.0/` | Plan commit-naming convention, not a claimed university mandate |

This plan does not recommend a particular latest model or guarantee permanent browser audio-format support; verify actual environments. Users still review external-model correctness; schemas do not replace food matching/nutrition provenance.

---

# Appendix D. Reusable review templates and additional boundaries

## D.1 Backend change register template

At least one entry per BC; split a BC across multiple PRs if needed.

```text
BC-ID:
Task IDs:
Related FR/NFR:
Observed old behaviour: Original file/symbol/line; describe only observed implementation.
Why frontend alone cannot fix it:
Chosen change:
Files/classes/methods:
API before → after:
Database before → after:
Existing data impact:
Backwards compatibility / deprecation:
Transaction and concurrency model:
Privacy/security considerations:
Migration ID / prerequisites / authorisation:
Tests actually run and evidence:
Rollback/recovery procedure:
Known limitations:
Reviewers / approvals:
Commit / PR:
```

Explain why frontend-only changes cannot solve the issue: React cannot guarantee PG batch rollback across HTTP writes; disabled buttons do not ensure server retry idempotency; browsers cannot fabricate user consent; date filters cannot repair database date attribution.

## D.2 Database migration review template

```text
Source database identity (redacted):
Schema/version before:
Application commit before:
Migration files and checksums:
Read-only preflight result:
Email/weight/ref conflicts and approved decisions:
Legacy timezone and uncertainty note:
Backup location (not committed) / restore verification:
Expected rows and preserved columns:
Expected numeric/unit conversions:
No inferred reference/weight/MET/consent fields:
New indexes/constraints and validation result:
Application validate / smoke tests:
Rollback-compatible application range:
Authorisation and reviewer:
```

## D.3 PR template

```markdown
## Purpose
Task IDs / FR / NFR:
What user workflow becomes possible:

## Source reuse
Demo symbol → target file:
Existing backend service reused:

## Backend / database changes
BC IDs:
Why each change is needed:
Migration / backfill / compatibility:

## Verification
Commands actually run:
Passed / failed / not run:
Evidence links:

## Risks and recovery
Unknown outcomes / concurrency / privacy:
Rollback or restore procedure:

## Review checklist
- [ ] No real-mode mock fallback or simulated credentials.
- [ ] No unintended changes to teammates' files or branches.
- [ ] API examples and DTOs match.
- [ ] Null, date, kJ and ID semantics are consistent.
- [ ] Required server ownership/version checks are present.
- [ ] Actual tests and limitations are recorded.
```

This is a proposed PR template, not completed checks. Never mark every checkbox[x] for appearance.

## D.4 Boundaries to settle before completing OpenAPI

1. Separate Profile currentWeight from profile/user versions; never use weight version as profile expectedVersion.
2. Previously completed users missing adultConfirmed use supplemental confirmation without recreating initialWeight; cover B03/F03/F06/Q03.
3. Food/exercise edits cannot change source/clientEntryId; moving a date does not create a new record.
4. Scalar IDs are strings; failed path-to-Long parsing returns400, not500. Do not break version types with global Long-as-string serialisation.
5. BigDecimal is a JSON number; preserve server precision while frontend represents ordinary displayable quantities. Compute precise internal hashes in Java without requiring browser hash reproduction.
6. False consent, null goal, empty arrays, missing fields,0 and null have distinct contract semantics; do not broadly apply `value || default`.
7. Reuse submissionId only for the same operation/canonical payload; consistently use IDEMPOTENCY_CONFLICT across modules.
8. Receipt200 proves successful creation, not current nondeletion; record GET404 must not trigger another POST.
9. Date ranges include both from/to; cross-page filtering must retain the day. Exceeding limits returns explicit errors rather than silent truncation.
10. Today and daily(date=today) represent identical facts; page failures are not no data/0.
11. New reference versions do not mutate old rows. New records require active references; old records may correct their own immutable inactive references under10.6.
12. Distinguish token/header/body errors; protected handlers check CurrentUser/roles; security-filter errors also return JSON.
13. Provider configuration ready, capability available and real-smoke PASS are distinct states, not one combined feature-complete claim.

## D.5 Additional concurrency/locking review

Same-owner writes requiring unified consistency follow one lock order. User-row locks simplify same-user concurrency without blocking different users; database uniqueness remains mandatory despite locks.

Protecting the last ADMIN requires more than locking one target user during concurrent deactivation. B14 must choose stable serial locking: lock the relevant ADMIN set by ascending ID inside the transaction and recheck remaining enabled administrators, or use a tested equivalent global-constraint lock. Two requests must not each count2 and both deactivate to0. No extra business table is required; document the lock scheme in ADR/tests.

@Version does not cover concurrent creation of an absent same-date weight row; user-row locks plus date uniqueness do. Never hold user locks during external calls. Separate QA eligibility check/call/recheck, with the final recheck deciding answer delivery under permission races.

Temporary null result_json/committed_at inside a receipt transaction does not permit committing an empty receipt. Services populate/flush before returning; injected integration failures prove full rollback. Startup verification may report anomalous incomplete committed receipts, never treat them as success.

## D.6 Additional reference-import/numeric review

Database/service validation covers at least positive quantity/minutes/weight, precision, nonfuture dates (current-date dependence in Services), source/meal enums, required CURRENT references/snapshots, uniqueness and owner FKs. Explicit legacy-state exceptions must not relax nonnull guarantees for all new records.

Source-file hashes fingerprint **file bytes**, not filenames; canonical submission hashes fingerprint **normalised requests**, not calculated nutrition. Keep these concepts distinct.

Do not bind network downloads directly to database transactions. Obtain official files in authorised environments, verify licensing/headers and hashes, then transactionally write new versions. Deactivate the old active version before activating the new one within a single transaction, preserving active indexes and rollback on errors. Never delete historical references.

## D.7 Explicit changes from earlier plans

| Earlier suggestion | Decision here | Reason |
|---|---|---|
| Build many pages from scratch | Reuse existing Demo components/theme/independent Review | The newly supplied Demo already has real local interactions |
| Use RHF/Zod for all forms | Keep working useState; reducers for complex state; Zod at API boundaries | Avoid valueless rewriting for tool uniformity |
| Review as entry-page second step | Keep independent /review with reliable routing/drafts | Reuse existing Demo routing |
| Focus on one new food-batch endpoint | Add onboarding, receipt recovery, immutable reference details, CSRF/capabilities and M2 APIs | Scope requires executable integration/recovery, not page-count-driven APIs |
| Nine tables per Proposal | Nine business tables + technical receipt table, requiring ADR approval | Persistent whole-submission identity/lost-response recovery, not purported original Proposal content |
| Preliminary frontend9–11days | Layered69-task estimates; separate frontend F from full engineering | Requested backend/database/all Demo features/acceptance prevent reporting UI effort alone |

---

**Execution principles: verify target branch/existing data first; reuse Demo without copying its mock database. Every backend/database change has a rationale, every user action has state, every commit has evidence.**


---

## Single-file v2.0 maintenance record (2026-10-16)

- **Retains all v1 architecture,69 task cards, BC-01–BC-16, migrations, M1/M2, tests and delivery criteria; this is not another patch document and requires no old execution prompt.**
- User confirmed local branch **`develop/yuankang-wei`**: added Section0.0 Branch Gate and updated0.1,0.3,3,18,23. ZIP is a source comparison, not permission to develop on main; no university GitHub connection.
- Added0.7/19.0 from `ELEC5619-Lab9.pdf` and `L9 Testing (1).pptx`, requiring **concurrent feature/test development, author self-review and same-work-package commits**, retaining Q00–Q08 for cross-layer safeguards.
- Aligns course JUnit5/Mockito, MockMvc, shift-left, AAA and UAT; adapt old APIs to Spring Boot4.1.1;95% is not presented as an explicit instructor grading threshold.
- Supply **this latest Markdown alone**, local working tree and Demo sources to execute23.2. This planning round did not alter project code, branches, databases or remotes; all tasks remain unstarted.
