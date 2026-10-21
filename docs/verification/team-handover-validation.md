# Team handover validation — 2026-10-18

Status: PASS for the defined handover scope; English documentation and trace audits complete. No commit, push or other remote Git operation was performed. Existing application databases and services were not selected or modified.

## Reproduction boundary

A temporary directory was populated from tracked working-tree source plus the proposed new handover files. No node_modules, Maven target, database dump, private environment file or reference-source cache was copied. Dependencies were installed again (`npm ci` and a separate Python venv using both requirements files). Machine-level dependency caches, installed JDK/Node/PostgreSQL tools and Chrome were available: this was a clean source/dependency-directory test, not a fresh operating system.

A new PostgreSQL 16.15 container, private generated credentials, an unused loopback port and a separately named `fitnutri_integration_handover` database were used. The automated backend suite created additional UUID test databases in this same owned instance. No original application database was queried or changed. The temporary frontend/backend used ports 15197/18197, avoiding the existing review services.

## Observed checks

| Check | Result |
| --- | --- |
| Ordinary frontend `npm ci` | Passed; 356 packages installed |
| Frontend Vitest | 113 files, 622 tests passed |
| Legacy fixture integrity and legacy tests | 1 integrity test and 15 legacy tests passed |
| Production boundary tests | 11 passed |
| Fault-proxy unit tests | 10 passed |
| Pipeline/deployment/backend-artifact tests | 13 passed after creating local Git metadata in the temporary source copy |
| TypeScript test and legacy checks, ESLint | Passed |
| OpenAPI validation and generated API type drift check | Passed using the documented Python environment |
| Production build/postbuild | Passed; 188 reachable modules, 962 import edges, 33 manifest entries, 35 production text artifacts, zero boundary violations |
| Python data/preflight/contract/trace tests | 45 passed, including 52 task cards, 129 scenarios and 14 backend-change trace checks |
| Maven `verify` | 522 Surefire tests and 91 Failsafe tests; zero failures, errors or skipped tests, counted from XML reports |
| Empty database initialization | V1–V13 applied successfully, Hibernate validation and startup passed |
| Reference-source provenance | All three AUSNUT sources, the official Compendium PDF and the pinned age evidence passed their original hashes |
| Reference preparation | AUSNUT and Compendium r2 preparation passed |
| Catalogue imports | All four import flags enabled for initial import; 3,741 food references, 40 stored activity references across revisions, 52 food search profiles, 38 exercise options and 36 movements observed |
| Real Chromium handover journeys against the restarted packaged backend | 10/10 passed: food, dish portion modes, Quick exercise, Workout, grouped summaries and responsive/zoom/keyboard presentation |
| Restart persistence | Packaged backend restarted using only `local`, all four import flags false; content hashes of all 18 public tables identical before/after restart |
| Existing-user restart check | Existing synthetic account logged in and retrieved nonempty saved food history after restart |
| Markdown file links | No missing local Markdown link targets found |
| Tracked private/generated paths | No tracked environment secrets, IDE settings, dependency/build directories or database dumps found by path audit |

## Portable test configuration

The old Python preflight test hardcoded a workstation Docker container name. It now requires explicit `TEST_DB_DOCKER_CONTAINER` and `TEST_DB_USERNAME`, rejects missing/shared targets, and uses only UUID-named disposable databases. Two guard tests cover this configuration boundary. The initial unconfigured attempt was denied by the sandbox before Docker access; the corrected suite passed all 45 tests on the dedicated handover container. No historical application data was accessed.

## Reference-data correction

The live Compendium age webpage no longer matches the immutable r1/r2 source hash. The fetcher now restores the original reviewed bytes from a bundled compressed evidence snapshot and verifies the unchanged original SHA256. The source URL, attribution and purpose are documented beside it. It is never served or executed as application HTML. The PDF and AUSNUT sources still download from the official sources and must pass their pinned hashes. No nutrition value, MET value, manifest hash or migration checksum was changed.

## Failures investigated during verification

- The first browser pass was 9/10: an English replacement for a long-name fixture exceeded the application's 100-character name limit. The fixture was shortened while retaining its long-layout purpose; the targeted test then passed. The business validation rule was unchanged.
- Pipeline tests initially failed because the exported temporary source lacked `.git`. Local Git metadata was initialized only in that temporary directory, without a remote, and all 13 pipeline/deployment/artifact checks passed.
- The first static attempt lacked `openapi-spec-validator`. Installing the documented requirements in the temporary venv resolved it; the full static stage passed.
- A manual restart API probe initially omitted required date-range parameters and correctly received HTTP 400. Repeating it with the saved record date returned HTTP 200 and nonempty history. The existing browser login/dashboard requests also succeeded.

## Limits and retained content

This handover does not certify live paid AI providers, public HTTPS deployment, peer review or human UAT. Optional AI still needs each operator's own credentials and enabled models. A future download can fail if an upstream source or dependency becomes unavailable; checksum failures remain intentional and must not be bypassed. Members must supply their own database password and follow the empty-database initialization/import steps.

Historical reports retain their original dates, counts, TODOs and failures. Chinese search aliases, speech/date/unit inputs, multilingual security/UTF-8 test cases and immutable legacy fixture content remain because they exercise application behavior. Exact historical source filenames may remain for provenance. These are distinct from instructional prose and test names, which have been translated.

The current files contain environment placeholders rather than shared credentials. This is not certification that the complete historical Git object database is secret-free: old development defaults were previously removed from current configuration, and history was not rewritten. Any formerly used credential should not be reused on an exposed/shared system.

## Final documentation and cleanup

All four large specifications and the user guide are now English. Cross-checks preserved task/requirement/test IDs, dependency edges, historical counts, source hashes and executable command blocks; code comments were translated without changing executable code. The UX trace still checks all 52 cards, 129 scenarios and 14 backend changes. Remaining Chinese is functional content or exact historical provenance, not untranslated instructional prose.

The verified temporary frontend/backend processes and only the newly created handover container were stopped after testing. Its database contents were retained; no original service was stopped. Private raw validation logs are retained under ignored `.local/handover-validation-20261018/`. The proposed 35-path change set is in [the submission manifest](team-handover-files.md); the old/new user-guide paths represent one rename. Nothing is staged, committed or pushed for this handover.
