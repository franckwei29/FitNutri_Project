# Verified integration baseline — P00

Verified 2026-10-16 Australia/Sydney. Branch Gate: PASS.

- Working tree: `/Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6`
- BASE_BRANCH: `develop/yuankang-wei`
- BASE_COMMIT: `117348a01ded0a08ac82571fafb7b03103bfb50e`
- Local branches: target, main, backup/yuankang-before-main-20261016-1.
- One worktree; no checkout/reset/stash/remote operation performed.
- Local target reports ahead 54 relative to its existing tracking ref; no remote freshness claim.
- All 97 tracked project files match the supplied project ZIP byte-for-byte; no tracked extras/missing files.
- Existing metadata remains untouched and excluded from integration commits:

```text
?? .DS_Store
?? .idea/workspace.xml
?? backend/.DS_Store
?? backend/src/.DS_Store
?? backend/src/main/.DS_Store
?? backend/src/main/java/.DS_Store
?? backend/src/main/java/com/.DS_Store
?? backend/src/main/java/com/fitnutri/.DS_Store
```

| Input | SHA-256 |
|---|---|
| /Users/franckwei/Downloads/FitNutri_AI_Integration_Execution_Plan.md | `2c4028c43e52fc25e7dc8409cbd515f4f6448009d8b7c87016372f895fe6c388` |
| /Users/franckwei/Downloads/Lab_practical_05_Group_6-main.zip | `139764140d4db575631a7520e35cf214e8c1acd938d26afadd55c439dd9a295d` |
| /Users/franckwei/Downloads/FitNutri_Demo_Source.zip | `a7a7449a1a2374a2edd0640db7f7eafe0e498e8de8263e5d6f0c243476524b3c` |

Demo archive: 24 files, all paths checked for traversal and symlinks. Existing external reference `../fitnutri-demo` matches all ZIP files. It is outside production inputs. No ZIP copied over project files.

## Observed backend differences against v2.0

| BC | Baseline status / evidence |
|---|---|
| BC-01 | Missing: first registration becomes ADMIN; exact email matching; CSRF disabled; hand-built context repository. |
| BC-02 | Partial: existing profile/onboarding flag, no atomic onboarding/adult/unit/version; ambiguous null updates. |
| BC-03 | Missing: append-only unversioned dated weights. |
| BC-04 | Missing: String food key PK, AFCD JSON without dataset provenance. |
| BC-05 | Missing: static ExerciseCatalog Map. |
| BC-06 | Missing: Instant dates, Double nutrients, no snapshots/version/soft deletion/client identity. |
| BC-07 | Missing: no durable creation receipts. |
| BC-08 | Missing: single food POST accepts client nutrients. |
| BC-09 | Partial: exercise estimate exists; current weight/70 kg fallback; integer duration. |
| BC-10 | Partial: list/update/delete exist; no scoped date persistence/version/soft deletion/detail contract. |
| BC-11 | Partial: two summary controllers with separate calculations and automatic targets. |
| BC-12 | Partial: admin status API exists; no version or last-admin concurrency protection. |
| BC-13 | Missing: message-based errors; no structured readiness/Clock/current-user policy. |
| BC-14 | Missing: Hibernate update; no Flyway migrations. |
| BC-15 | Partial: image adapter exists; needs validation/configuration/matching. |
| BC-16 | Missing: no transcription, extraction or Q&A HTTP API. |

## Boundaries and blockers

M1 CORE + SUPPORT then M2 FULL as specified. Official AUSNUT headers/manifest and audited 2024 activities are absent; never relabel AFCD or Demo values. No AI environment configuration detected (names checked only, values not printed). Shared database identity/schema/legacy timezone remain unverified; no shared DDL is authorised or performed. ADRs/new technical receipt table are authorised for local implementation by the explicit v2.0 execution request; independent review/approval before integration merge remains PENDING. No remote push/PR/merge authorised.

Root/parent and project subdirectories contain no applicable AGENTS.md or CONTRIBUTING.md; no existing OpenAPI or migration directory discovered. Read the original project README and runtime manifests. Test/runtime evidence is in environment.md and ../verification/baseline-tests.md. No historical execution plans loaded.
