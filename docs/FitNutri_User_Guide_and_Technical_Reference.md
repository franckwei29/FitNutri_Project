# FitNutri User Guide and Technical Reference

Audience: first-time users, testers, and software engineers/architects seeking implementation details.

Written: 2026-10-17 (Australia/Sydney).

Inspected baseline: `develop/yuankang-wei`, `c13c203`. The commit adding this document follows that baseline.

Project directory: `/Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6`.

This guide was based on the code, runbooks, and developer test records at that baseline. It supports use and handover and does not replace the authoritative execution specification. Page labels are mainly English and are reproduced for navigation.

> Historical baseline: the V8 schema, catalogue counts, local environment, and verification counts below describe the inspected baseline, not the latest handover. For a new installation, use the current [getting-started guide](getting-started.md).
>
> Status at writing: M1 manual-record functions were implemented with developer tests and real local integration. M2 image, speech, and AI Q&A frontend/backend flows were implemented, but live model calls, real microphones, and full browser acceptance were incomplete. Peer review, UAT, and public deployment were incomplete. These instructions do not claim acceptance passed or that historically started services remain running.

## Contents

- Part One: User Guide
  - 1. Purpose and permissions
  - 2. Open an existing environment
  - 3. Start from a terminal
  - 4. Complete ordinary-user workflow
  - 5. Images, speech, and AI Q&A
  - 6. Administrator operations
  - 7. Beginner checklist and troubleshooting
- Part Two: Technical Reference
  - 8. Architecture and technology
  - 9. Directories and module responsibilities
  - 10. Key request and data flows
  - 11. Database, calculations, and consistency
  - 12. Identity, security, and AI boundaries
  - 13. Testing, maintenance, and delivery status
  - 14. Glossary and further reading

---

# Part One: User Guide

## 1. Purpose and Permissions

FitNutri records food intake, exercise, and weight, and displays daily overviews, historical trends, and rule-based summaries of actual records. AI can generate input candidates for review and answer general health questions; saving always requires explicit user confirmation.

The software provides no medical diagnosis or prescription. Energy references are user-selected values; missing targets or weight are never silently replaced with defaults.

### 1.1 Understand Your Role

Testing permission does not grant access to every user's data or bypass confirmations. The actual application has no Demo identity switch, whole-database wipe, or one-click reset.

| Role | Allowed actions | How obtained | Does not allow |
|---|---|---|---|
| Ordinary user `USER` | Own profile, food, exercise, weight, history; AI when eligible and available | Self-registration or an authorized account supplied by a maintainer | Others' private records or administrator console |
| Administrator `ADMIN` | Minimum necessary account lookup, disable/restore accounts | Existing authorized account or controlled backend bootstrap | Private food/exercise/health information; not a superuser for private data |
| Developer/environment maintainer | Start/stop services, configure databases, isolated tests, AI setup | Authorization for the machine and environment | Tests against shared real data, publishing keys, direct DB edits masquerading as business verification |

Registration creates only `USER`. Test admin and ordinary-user behavior in separate browsers or normal/private windows to avoid overwriting session Cookies.

### 1.2 M1 and M2

| Delivery scope | Features | Supported conclusion at baseline |
|---|---|---|
| M1 CORE + SUPPORT | Authentication, onboarding, manual entry, review/save, overview, history, profile, weight, trends, rule summaries, account management | Implemented; developer tests and real local integration evidence; manual acceptance pending |
| M2 FULL | Food images, food/exercise speech, AI Q&A | Frontend/backend implemented; controlled-provider tests passed; live models/devices pending |

## 2. Open an Existing Environment

Confirm with the maintainer that services are running, then open:

| Address | Purpose |
|---|---|
| `http://localhost:8787` | Local built-frontend demonstration with same-origin backend gateway |
| `http://localhost:5173` | Vite development server for viewing code changes |

`localhost` means this computer. Sending this address to another computer does not connect it to yours. Port 8787 is a local demo, not a published website.

Keep the same hostname rather than alternating `localhost` and `127.0.0.1`. Check services before considering database changes if the page fails to open.

Register your own ordinary test account. Maintainers privately provide existing admin/test credentials; neither this guide nor the repository supplies a universal password. Use synthetic information, not real medical histories or other people's media.

## 3. Start from a Terminal

### 3.1 Prerequisites

| Dependency | Use |
|---|---|
| Node.js 24, npm | Frontend tooling and same-origin gateway |
| Java 21 | Spring Boot backend |
| Bundled Maven Wrapper | `backend/mvnw`; no need to guess Maven version |
| PostgreSQL 16 | Actual accounts and records |
| FFmpeg | Speech and related tests; manual flows do not invoke decoding |
| Database credentials | Obtain for your own authorized environment |

These historical quick steps assume an existing local database with V8 migration and reference imports complete. Startup validates the database; it does not automatically create tables, migrate, or import food.

The recorded synthetic environment used PostgreSQL port `55432`, database `fitnutri_integration_browser_20261016`. A maintainer must confirm it still exists. Optional repository Docker Compose defaults describe another environment.

For a new computer/database, follow the current [getting-started guide](getting-started.md). The baseline process used the [runbook](runbook.md) and [migration guide](db/migration-foundation.md) for initialization, preflight, backup, and imports. Do not bypass errors by changing `ddl-auto`, disabling migration validation, or arbitrary SQL.

### 3.2 Open the Project and Check the Branch

```sh
cd /Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6
git rev-parse --show-toplevel
git status --short --branch
git branch --show-current
```

This development round required `develop/yuankang-wei`. If different or unfamiliar modifications exist, ask the maintainer to inspect; do not clean/reset/overwrite. This guide requires no university GitHub access or `fetch`, `pull`, `clone`, or `push`.

### 3.3 Install Locked Frontend Dependencies

On first installation or lockfile change, from the project root:

```sh
npm --prefix frontend ci
```

This may access package registries; it does not replace source from GitHub. Do not repeat every startup if dependencies and lockfile are unchanged.

### 3.4 Terminal A: Backend

The following address is for the confirmed historical synthetic database. A maintainer must substitute another authorized database's configuration when appropriate.

```sh
export DB_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_browser_20261016
export DB_USERNAME=fitnutri_test
export APP_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8787
export APP_BOOTSTRAP_ADMIN_ENABLED=false
```

In macOS zsh, enter the password without putting it in command history:

```sh
read -rs 'DB_PASSWORD?Enter the database password supplied by the maintainer: '
export DB_PASSWORD
printf '\n'
./scripts/dev.sh backend
```

Keep the terminal running. This uses the `local` profile and Spring on `127.0.0.1:8080`. First Maven startup may resolve dependencies. Missing configuration, failed connections, incomplete migration history, or reference-source mismatches require fixing the corresponding environment; they are not successful startup.

### 3.5 Terminal B: Frontend

In a new terminal:

```sh
cd /Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6
./scripts/dev.sh frontend
```

Keep running and open `http://localhost:5173`.

### 3.6 Terminal C: Backend/Database Health

```sh
cd /Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6
./scripts/dev.sh health
```

Expected output includes `Backend/database health and CSRF: PASS`. This verifies health/CSRF endpoints only. Log in and check food/exercise catalogues; unavailable catalogues restrict entry actions.

### 3.7 Optional Built-Frontend Demonstration

With backend running, in another terminal:

```sh
cd /Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6
npm --prefix frontend run build
FITNUTRI_LOCAL_HTTP=1 ./scripts/dev.sh gateway
```

Open `http://localhost:8787`. The gateway serves `frontend/dist` and forwards `/api` to Spring. Rebuild after source changes to see them on 8787.

After verified packaging, a maintainer may use `./scripts/dev.sh backend-built` instead of Terminal A, first stopping their existing 8080 service. It uses a protected immutable JAR copy. Do not run a JAR in `backend/target` while repackaging over it.

### 3.8 Normal Shutdown

Press `Ctrl+C` in the frontend/gateway/backend terminals you started. Do not remove database containers/volumes to stop the app. The historical dedicated test PostgreSQL used tmpfs; stopping/restarting can lose data. A maintainer must first preserve and verify external backups. Do not run `docker compose down -v`.

## 4. Complete Ordinary-User Workflow

Recommended first walkthrough:

```text
Register → Log in → Three-step onboarding → Manual food → Review → Confirm save
         → Manual exercise → Review → Confirm save → Dashboard → History → Profile → Log out
```

### 4.1 Registration and Login

1. Open `/login` and switch to registration.
2. Enter your test email, display name, and password: at least 8 characters, letters and numbers, subject to backend length limits.
3. Return to login after registration. Registration does not automatically log you in.
4. Log in. Email is case-insensitive; do not register duplicates using case changes.
5. New ordinary users enter onboarding, configured users enter daily pages, administrators enter their console.

### 4.2 Three-Step Onboarding

| Step | Actions | Notes |
|---|---|---|
| Basic information | Name, age-18 confirmation, birth year, sex, height, initial weight/date | Follow validation; use coherent synthetic data |
| Goals and activity | Activity level, optional general goal, energy unit, optional daily intake reference | Goals/references may remain empty or be removed later |
| Health and Q&A | Safety answers and consent for general-health questions using confirmed records | No detailed medical history; do not falsify answers to enable AI |

Successful submission opens Dashboard. Backend saves onboarding completion and initial weight consistently. Food, exercise, history, and rule summaries remain usable without Q&A eligibility/consent.

### 4.3 Manual Food

1. Click **Add a record**.
2. Select **Food**, **Manual entry**, then **Continue**.
3. Search/select an actual reference item. Names are mainly English; try `rice` or `apple`, using actual results.
4. Enter grams, for example `100 g`, and the test meal/date. Grams are not serving counts.
5. Add multiple items if desired; check each name, grams, meal, date.
6. Enter **Review** and inspect candidates and energy.
7. Click **Confirm & save**, wait for explicit success, then check Dashboard/History.

Review is not saving. Returning, canceling, or discarding drafts must create no formal records. Drafts live in application memory; refresh/logout may lose unsubmitted content.

### 4.4 Manual Exercise

1. **Add a record → Exercise → Manual entry**.
2. Select an available activity and enter minutes/date.
3. Check energy estimate, enter Review, explicitly confirm.
4. Without historical weight for that date, energy may be unknown while minutes remain savable. Unknown is not zero.

At this baseline the catalogue contained 15 source-verified activities, not every possible exercise.

### 4.5 Dashboard

View selected-date food, exercise, energy, and rule summaries. Only confirmed records count; unsaved drafts do not. Missing goals, unrecorded dates, and unknown exercise energy differ from `0`.

Energy displays in `kJ` or `kcal`. Changing units changes presentation, not the number of stored records.

### 4.6 History and Trends

1. Open **History**.
2. Filter **Record type**, dates, or **Last 7 days / Last 30 days**.
3. Open details and check name, date, quantity, provenance, and energy.
4. Use **Summary view** for period/daily/weekly summaries and trends.
5. No matches should show an empty state, never fabricated data; no records need not mean zero intake.

### 4.7 Edit and Delete

Edit from details, review changes, confirm. After date changes, inspect the new date. Deletion requires confirmation and removes the entry from ordinary lists/statistics.

If another window changed the record, reload and reconsider instead of blindly overwriting. Soft Delete retains the original database row/audit trail; UI deletion is not a database wipe.

### 4.8 Profile, Goals, and Weight

Use **Profile** to change personal settings, units, general goals, optional energy reference, and Q&A consent. Weight has independent dated entries; add/correct same-day or historical data.

Weight changes must not rewrite previous exercise-energy snapshots. Withdrawing Q&A consent restricts AI questions while manual records remain available.

### 4.9 Logout and Uncertain Saves

Choose **Log out** from the upper-right account menu. For unsubmitted drafts, decide whether to remain and continue or discard. Logout invalidates the backend session.

If the network drops after confirmation and the outcome is unknown:

- Keep the page open; use its status/recovery action.
- Do not recreate the same record or repeatedly confirm.
- Wait for an explicit backend result before deciding next steps.
- After refresh/logout, first inspect History after logging back in. Give maintainers request ID/time, never passwords.

Canceling a request/wait does not mean the database transaction rolled back.

## 5. Images, Speech, and AI Q&A

### 5.1 Why Buttons May Be Disabled

Capabilities tells frontend what is available. Missing model keys/names, switches, catalogues, decoder, or eligibility can disable entry. Implemented pages may still be unavailable because runtime prerequisites are absent.

At the recorded delivery, live-model configuration was absent and paid calls numbered 0. The following describes use after configuration, not completed live-recognition acceptance.

### 5.2 Food Images

Choose **Food → Upload food image** and an authorized JPEG, PNG, or WebP: maximum 8 MiB, dimensions at most 4096 pixels per side.

Inspect candidates, complete/correct reference matching, grams, meal, and date, then Review and explicitly confirm. Recognition failure reports an error, not a fixed success. Guessed portions are not automatically saved as reliable facts.

### 5.3 Food/Exercise Speech

Choose record type and **Voice entry**:

1. Permit microphone access and record synthetic descriptions without private health information.
2. Stop and explicitly transcribe. WebM/MP4 support depends on browser; maximum decoded duration 60 seconds, upload 8 MiB.
3. Check/edit **Transcription**, then parse.
4. Check candidates and complete quantities, minutes, meal, or dates.
5. Review → Confirm & save.

Chinese-language test examples include “今天午餐吃了 100 克米饭” (100 grams of rice for lunch today) and “今天步行 20 分钟” (walked 20 minutes today). These are inputs, not model-accuracy guarantees. Use manual entry if microphone permission is denied or unsupported.

### 5.4 General Health Q&A

When current safety answers, explicit consent, and model readiness allow, open `/chat`. Ask a general question; select only necessary record dates, at most 31 days.

Only the current question and explicitly selected minimum facts are sent, not full profiles, raw safety answers, or complete chat histories. Follow waiting/retry/restriction messages. Q&A failures do not delete records; questions neither create food/exercise records nor persist chat history.

## 6. Administrator Operations

1. Log in with a privately supplied `ADMIN` account and open **Administrator Console**.
2. Find a specifically authorized synthetic account.
3. Check email/status, then disable and confirm.
4. In a separate user browser verify the old session is blocked and login rejected.
5. Restore, then verify login works and original records remain.

Self-actions, the last enabled admin, and stale versions are protected. Refresh/recheck conflicts; do not bypass via direct DB edits. Ordinary registration cannot create admins.

For initial bootstrap, maintainers privately set `APP_BOOTSTRAP_ADMIN_ENABLED=true`, `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD`, and `APP_BOOTSTRAP_ADMIN_DISPLAY_NAME`; disable bootstrap and restart after success. This does not promote existing ordinary users and is not for ordinary testers to enable.

## 7. Beginner Checklist and Troubleshooting

### 7.1 Walkthrough Checklist

Actual operators fill these boxes; they are not preclaimed passes. Writes use only your own synthetic account/environment.

| Check | Action | Expected |
|---|---|---|
| [ ] | Register then log in | No automatic login; appropriate onboarding/home destination |
| [ ] | Complete onboarding | Dashboard and saved profile |
| [ ] | Add two foods, cancel at review | Neither appears in History |
| [ ] | Re-add and confirm two foods | Correct date, two items, one batch |
| [ ] | Confirm exercise | Correct minutes/date; estimate when possible |
| [ ] | History filters/trends | Matching dates, type, units |
| [ ] | Correct record/date | Updated record on new date; statistics updated |
| [ ] | Delete own test record | Confirmation then updated lists/statistics |
| [ ] | Units, remove optional goal, historical weight | Consistent reread, no invented missing target |
| [ ] | Logout/login | Saved records persist; drafts are not formal records |
| [ ] | Authorized disable/restore | Session blocked; login restored; records preserved |
| [ ] | M2 after live configuration | Real editable candidates; no formal writes before confirmation |

### 7.2 Common Problems

| Symptom | First checks/actions |
|---|---|
| Site will not open | Service on 5173/8787; address belongs to this machine |
| Page opens but requests fail | Spring 8080, PostgreSQL, backend health; not just frontend |
| Missing `DB_*` | Privately set variables in the backend terminal |
| V8 validation failure | Maintainer checks target/history; do not disable validation or wipe |
| No catalogue/entry unavailable | Verified import and capabilities |
| Image/speech disabled | Displayed reason, backend models/switches/FFmpeg |
| Q&A unavailable | Current eligibility/consent and readiness |
| Repeated logout/403 | Same hostname, relogin; maintainer checks origins/Cookie/CSRF, not disable security |
| Unknown save | Recover original submission; no new ID duplicates |
| 409 version conflict | Reload and review latest before editing |
| Port occupied | Identify owner; do not kill another person's service |

---

# Part Two: Technical Reference

## 8. Architecture and Technology

Separate frontend/backend web application with a Modular Monolith: business modules inside one Spring app, not independently deployed microservices.

```text
Browser: React pages, forms, candidate drafts, review
  │ HTTP /api with Session Cookie and write-request CSRF Token
  ├─ Development: Vite :5173 ────────────┐
  └─ Local built demo: Node :8787 ──────┤
                                       ▼
                              Spring Boot :8080
                 Security → Controller → Service → Repository
                                       │               │
                                PostgreSQL       AI adapter
                    Accounts/profiles/records/catalogues │
                                                  External model
                                          (optional; backend-only keys)
```

| Layer | Technology | Responsibility |
|---|---|---|
| Frontend | React, TypeScript, Vite, MUI | Pages, types, build, UI components |
| Routing/forms | React Router, React Hook Form, Zod | Navigation, form state, input/response validation |
| Server data | TanStack Query, Axios | Requests, cache, refresh, errors, sessions |
| Charts | Recharts | Backend trend presentation |
| Backend | Java 21, Spring Boot 4.1.1 | API, rules, transactions, errors |
| Security | Spring Security Session, CSRF | Sessions, roles/ownership, forgery protection |
| Persistence | Spring Data JPA/Hibernate, PostgreSQL 16 | Mapping, queries, constraints, transactions, concurrency |
| Migration | Flyway V1–V8, explicit scripts | Safe incremental migration; automatic startup migration disabled |
| Media/models | FFmpeg, backend HTTP adapters | Bounded decoding, structured candidates, general Q&A |
| Built demo | Node.js HTTP(S) gateway | Static artifacts and same-origin API proxy, not business DB/model keys |

Backend is authoritative for calculations, authorization, persistence. Frontend checks improve experience but do not replace validation. Demo visuals/interactions were reused; mock Store, seed users, and fixed AI replies are not production business sources.

## 9. Directories and Module Responsibilities

### 9.1 Top-Level Layout

```text
frontend/                 React app, build configuration, frontend tests
backend/                  Spring Boot, migrations, Java tests
deploy/                   Built-frontend gateway, immutable JAR staging
scripts/                  Startup, quality, contracts, reference data, DB tools
docs/
  implementation/         Authoritative spec, tasks, reuse/change maps
  api/                    OpenAPI contracts and examples
  db/                     Preflight, migration, recovery
  verification/           Actual evidence and incomplete acceptance
.local/                   Local private artifacts, credentials, backups, etc.
```

### 9.2 Frontend

Paths relative to `frontend/src/`:

| Path | Responsibility |
|---|---|
| `app/router.tsx` | `/login`, `/onboarding`, `/dashboard`, `/add`, `/entry`, `/review`, `/history`, `/profile`, `/chat`, `/admin` routes |
| `app/SessionProvider.tsx` | Login/user/profile lifecycle |
| `app/DraftProvider.tsx` | Entry draft/submission state, not business database |
| `app/providers.tsx`, `app/theme.ts` | App providers/shared containers and MUI theme |
| `layouts/` | Shell, header, authentication/role/onboarding guards |
| `pages/` | Authentication, onboarding, overview, history, profile, entry choice, Q&A, admin assembly |
| `features/entry/` | Food/exercise editors, draft machines, shared/correction review, save recovery |
| `features/entry/media/` | Images, recorder lifecycle, transcription/parsing/candidate conversion |
| `features/profile/` | Onboarding, settings, dated weight forms |
| `features/history/` | URL filters, period summaries/trends |
| `hooks/` | Session, catalogue, preview, query, submission, navigation protections |
| `api/` | HTTP, CSRF, errors, generated types/response validation; `auth.ts`, `records.ts`, `speech.ts`, `qa.ts` |
| `components/` | Dialogs, dates, records, energy cards, provenance |
| `domain/` | Pure date/unit/label/input/display functions, not final DB calculations |

### 9.3 Backend

Under `backend/src/main/java/com/fitnutri/backend/`:

| Package | Responsibility |
|---|---|
| `security` | Session chain, CSRF, access rules |
| `user` | Accounts/authentication, normalized email, passwords, explicit admin bootstrap |
| `profile` | Onboarding, goals, units, eligibility inputs, dated weights |
| `admin` | Minimal DTOs, lookup, versioned disable/restore, admin protections |
| `foodreference` | Datasets, provenance verification, search/import, nutrition calculation |
| `activity` | Datasets, exercise types, MET, reference capabilities |
| `records` | Persistence, estimation, versioned corrections, soft deletion, ownership |
| `submission` | Batch UUID/digest, transactions, receipts, retry/recovery |
| `summary` | Daily/weekly/period trends, deterministic summaries |
| `capabilities` | Availability from config/data/adapters/user scope |
| `ai` | Model configuration, restricted HTTP, structured responses, exception mapping |
| `food` | Image validation/model/candidate matching; no direct record writes |
| `speech` | Decoding, transcription, parsing, incomplete candidates |
| `qa` | Current eligibility/consent, minimal context, Q&A policy |
| `common` | Current-user checks, exceptions, shared infrastructure |

Controllers accept requests/respond; Services apply rules, authorization, transactions; Repositories access databases; Entities map tables; DTOs limit exposed fields.

### 9.4 Main API Boundaries

| Path/prefix | Purpose |
|---|---|
| `/api/auth` | Registration/login/logout/session/CSRF |
| `/api/profile`, `/api/profile/weight` | Profile/onboarding/weight |
| `/api/food-reference` | Catalogue/details/calculation previews |
| `/api/records/food`, `/api/records/exercise` | Formal records, exercise catalogue/estimates, batch creation |
| Correction endpoints under `/api/records` | Versioned changes/deletion; exact paths/methods in contract |
| `/api/submissions/{submissionId}` | Original receipt/unknown-result recovery |
| `/api/summary` | Daily/weekly/trend/rule facts |
| `/api/admin/users` | Account management |
| `/api/capabilities` | Capabilities without paid model probing |
| `/api/food/recognize` | Image candidates |
| `/api/speech/transcribe`, `/api/speech/parse` | Transcription/parsing |
| `/api/qa/messages` | Current general-health question |

Use the [OpenAPI contract](api/openapi-mvp.yaml) for methods, fields, errors, examples, not this overview alone.

## 10. Key Request and Data Flows

### 10.1 Manual Food Submission

1. Frontend queries capabilities/catalogue; user chooses reference/grams.
2. Backend preview returns canonical calculation/reference identity; frontend forms editable Draft.
3. Explicit Review confirmation freezes batch and UUID.
4. Request passes Session, CSRF, role, ownership, data validation.
5. Backend recalculates/saves all records and receipt in one transaction; any invalid item rolls back all.
6. Frontend refreshes after explicit success; lost responses recover via original UUID receipt.

### 10.2 AI-Assisted Entry

```text
Image/audio → Explicit frontend request → Backend validation/metadata removal
            → External model → Strict structure → Reference matching/missing fields
            → Editable candidates → Shared Review → Same formal submission transaction
```

Model output never directly writes `food_record`/`exercise_record`. Unavailable AI does not become fixed simulated success and should not impair manual records.

### 10.3 General Health Q&A

Frontend sends current question and explicitly selected context. Backend checks account/eligibility/consent, constructs minimal facts, calls model, then rechecks eligibility before returning so withdrawn consent is respected. No automatic record save or complete conversation persistence.

## 11. Database, Calculations, and Consistency

### 11.1 Entities

| Table | Content/relationship |
|---|---|
| `app_user` | Identity, role, enabled state, version |
| `user_profile` | Profile, onboarding completion, optional goals, units, consent |
| `weight_entry` | Dated user weight with same-day correction consistency |
| `food_dataset` → `food_reference` | Source version and entries |
| `activity_dataset` → `exercise_type` | Source version, types, MET |
| `food_record` | Confirmed date/reference/grams/nutrition snapshot/version |
| `exercise_record` | Confirmed date/activity/minutes/weight/MET/energy snapshots/version |
| `submission_receipt` | Owner, batch ID, normalized digest, result receipt |

Users own profile, weights, food/exercise records, receipts. Datasets have reference entries. Records preserve source identity and calculation snapshots; current catalogue names cannot reconstruct history alone.

### 11.2 Calculation Rules

- Food: `per-100-g value × actual grams ÷ 100`, backend `BigDecimal` and consistent rounding.
- Store `kJ`; display conversion `1 kcal = 4.184 kJ`.
- Exercise estimates use verified MET, minutes, available historical weight for date, retaining input/result snapshots.
- Missing weight leaves energy `NULL`, never assumes 70 kg; minutes remain valid.
- Dates use `LocalDate`, default business zone `Australia/Sydney`; old timestamp migration requires explicit zone approval.
- Backend owns aggregation; missing dates, unknown energy, and genuine zeros differ. Frontend must not zero-fill to alter denominators.

### 11.3 Duplicates, Concurrency, History

Idempotency combines owner, UUID, normalized digest: replay creates no second batch; changed content cannot reuse ID. Durable receipts recover lost responses.

Versions detect stale changes: concurrent old-version requests conflict and require reread/review. Account changes also have versions/locks.

Preserve historical reference versions, nutrition/exercise snapshots, original receipts. Explicit corrections and soft deletion retain history; historical weight entry does not silently recalculate records.

### 11.4 Migrations and Reference Data

At this baseline the database was V8, using V1–V8 migrations. `spring.jpa.hibernate.ddl-auto=validate` only checks schema; Flyway auto-execution is disabled, and a read-only startup gate verifies reviewed history.

Existing DB migration requires preflight, identity/timezone confirmation, backup and restored-copy verification. Scripts require explicit approval variables, never automatically claim nonempty legacy databases. Preserve original V1 baseline; do not upgrade/wipe it as a practice database.

Verified sources included 3,741 AUSNUT foods and 15 selected official activities. Preserve legacy rows without claiming official verification. Source caches, hashes, normalized manifests are checked at import; user requests never fetch sources dynamically.

Restore backups into a new isolated database, compare data and verify real reads; never overwrite the live source. Do not assume old-app/V8 compatibility or provide destructive automatic downgrade.

## 12. Identity, Security, and AI Boundaries

### 12.1 Browser and Identity Security

- BCrypt hashes passwords; no plaintext persistence.
- Session Cookies associate server sessions, not trusted LocalStorage role flags.
- CSRF tokens protect writes and refresh with authentication/logout; frontend handles them, users need not paste them.
- Backend checks effective role/status/owner on every request; hidden buttons are not authorization.
- Cookies default to HttpOnly, SameSite=Lax, Secure; explicit local HTTP disables Secure only locally. Public hosting requires HTTPS.
- Private API responses are not cached; logs exclude raw health content, passwords, media, model keys.

### 12.2 AI Configuration

Set only in Spring backend environment, never `VITE_*`, frontend builds, browser storage, or Git:

| Variable | Meaning |
|---|---|
| `OPENAI_API_KEY` | Authorized provider key |
| `OPENAI_IMAGE_MODEL` | Available image model |
| `OPENAI_TRANSCRIPTION_MODEL` | Transcription model |
| `OPENAI_PARSING_MODEL` | Text parsing model |
| `OPENAI_QA_MODEL` | General-health model |
| `AI_IMAGE_ENABLED` | Explicit image enablement |
| `AI_TRANSCRIPTION_ENABLED`, `AI_PARSING_ENABLED` | Transcription/parsing switches; voice requires relevant prerequisites together |
| `AI_QA_ENABLED` | General Q&A switch |
| `FFMPEG_BIN` | Trusted absolute executable if absent from PATH |

Switches default false, models empty; do not guess account availability. Configured does not mean a real call succeeded. Calls may cost money; independently verify with authorized account/models/budget.

Images are validated and re-encoded to remove metadata; restricted FFmpeg converts audio to 16 kHz mono WAV. Raw media is processed in request memory, not persisted as a business archive. Providers receive processed media, requiring submission rights.

Strict output structure and reference matching preserve missing fields rather than inventing confidence/quantities. Q&A sends current question/minimal selected facts, not raw safety answers, credentials, or full profiles. Conservative policy still requires live-model/manual review.

## 13. Testing, Maintenance, and Delivery Status

### 13.1 Layers and Entry Points

| Layer | Tools/location | Focus |
|---|---|---|
| Java unit/API | JUnit 5, Mockito, compatible Spring tests, `backend/src/test/` | Normal/boundary/missing/error/permission/API cases |
| PostgreSQL integration | Maven verify, independent UUID databases | Persistence, uniqueness, transactions, concurrency, rollback, migration, recovery |
| Frontend unit/component/contract | Vitest, Testing Library, `frontend/tests/` | Forms, machines, interactions, response checks, recovery |
| API contract | `docs/api/`, `scripts/validate_contract.py`, generators | Protocol/TypeScript/Zod consistency, no generated drift |
| Build/deployment | `scripts/tests/`, production dependency checks | No mock business import/key leakage; gateway/immutable JAR behavior |
| Browser E2E | Actual browsers, `frontend/tests/e2e/` | Real frontend/Spring/PostgreSQL workflows |
| Live models/devices | Separate smoke/manual scenarios | Images, transcription, parsing, Q&A, microphone, browser permissions |

Frontend-only examples:

```sh
npm --prefix frontend test -- --maxWorkers=2
npm --prefix frontend run typecheck:test
npm --prefix frontend run lint
npm --prefix frontend run build
```

Complete entry: `./scripts/check.sh`. Requires dedicated Python environment, locked dependencies, reference sources, isolated PostgreSQL, and private `TEST_DB_ADMIN_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`, `TEST_DB_DOCKER_CONTAINER`. These differ from application DB settings; never target ordinary records with automation. See [development rules](development.md).

Browser automation is separate; follow [E2E instructions](../frontend/tests/e2e/README.md) for services, identities, fault proxy. Discovery is not successful execution. Default checks make no paid live-model calls.

### 13.2 Recorded Evidence and Outstanding Work

These are historical delivery results; the complete suite was not rerun when writing this guide.

| Scope | Recorded result | Limitation |
|---|---|---|
| Java | 467 Surefire + 4 Failsafe passed; 0 failures/errors/skips | Developer testing, not peer review |
| Frontend | 63 files, 343 tests; types/lint/build passed | Not every real browser |
| Advanced APIs | 27 real local HTTP/DB tests passed | Controlled upstream, not real-model quality |
| M1 browser integration | Listed Chrome/in-app scenarios passed, including concurrency/disable | Incomplete browser/device matrix |
| Backup/restore | New DB restored; actual login/read passed | No source overwrite/shared deployment |
| Playwright | 9 cases discovered and typechecked | Runner not executed; not 9 E2E passes |
| Performance | Some local warm-start samples recorded | Not public/cold-start/live-model latency compliance |
| Live models/microphone | Incomplete | Private configuration, authorized media, real devices needed |
| Browser matrix | Incomplete | Edge/Safari, versions/viewports/media pending |
| Peer review/UAT | Pending | Developer tests cannot substitute |
| Public HTTPS deployment | Not executed | Local 8787 is not production |

`IN_REVIEW` among 69 tasks means independent review pending, not unimplemented and not accepted. Controlled-model tests do not complete real M2 service validation.

### 13.3 Maintenance Sequence

1. Work on required personal branch; preserve existing uncommitted metadata; no remote operations.
2. Pair business changes with meaningful Arrange–Act–Assert tests.
3. Update/check OpenAPI, generated types, response validation, callers for API changes.
4. DB changes require preflight, backup, isolated recovery rehearsal, reviewed incremental migration.
5. Run relevant checks, retain actual results/limits/tasks, verify branch, make small local Conventional Commits.
6. Complete real-service/hardware/browser/peer/UAT checks separately; development completion is not acceptance.

## 14. Glossary and Further Reading

| Term | Explanation |
|---|---|
| Frontend / Backend | Browser UI / authorization, rules, calculations, data service |
| API | Agreed requests/responses between applications |
| SPA | Single-page application with frontend routing |
| Same-origin Gateway | Page/API accessed through one browser origin |
| Session / Cookie | Server session and browser-held request credential |
| CSRF | Cross-site request forgery; token prevents third-party writes using login state |
| Draft / Review / Confirm | Only successful final confirmation creates formal records |
| Transaction / Rollback | Whole batch succeeds or is undone |
| Idempotency / Receipt | Prevent duplicate saves and recover uncertain results |
| Snapshot / Provenance | Retain calculated results and reference identity at that time |
| Migration / Preflight | Versioned DB changes and prior safety checks |
| Unit / Integration / E2E Test | Local logic / collaborating parts / full workflow |
| Mock / Controlled Provider | Predictable test behavior, not real-model calls |
| Code Review / UAT | Independent review/user acceptance, separate from developer tests |

Recommended reading:

1. [Local runtime/deployment runbook](runbook.md): environment, startup, AI, recovery.
2. [Final developer report](verification/final-report.md): executed results and external/manual blockers.
3. [Task ledger](implementation/tasks.md): 69 cards and commit evidence.
4. [Source reuse map](implementation/source-map.md): Demo interactions to real implementation.
5. [Backend changes](implementation/backend-change-log.md): backend/database impact.
6. [API contract](api/openapi-mvp.yaml), [migration foundation](db/migration-foundation.md), [restore rehearsal](db/restore-rehearsal.md).
7. [Live-model smoke status](verification/provider-smoke.md), [browser matrix](verification/browser-matrix.md).
8. [Authoritative v2.0 specification](implementation/FitNutri_AI_Integration_Execution_Plan.md): requirements/tasks/tests/acceptance.
