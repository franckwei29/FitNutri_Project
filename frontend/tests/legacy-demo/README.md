# Unmodified upstream Demo regression fixtures — Q01

This directory is test-only. It preserves all original15 Node domain/service assertions and their required domain.ts/seed.ts/store.ts dependencies byte-for-byte from the Demo baseline verified against FitNutri_Demo_Source.zip in P00. source-manifest.json records5 file lengths/SHA256s; the pretest integrity guard verifies every one before execution.

Run from repository root:
- `npm --prefix frontend run test:legacy` (integrity1 PASS, then original15 PASS; Node24 strip-types)
- `npm --prefix frontend run typecheck:legacy` (strict TypeScript, upstream unused-symbol policy)

These are local simulated Domain/MemoryStorage/seed/password/parser/QA behavior tests. They do not call Spring Boot, PostgreSQL or an AI provider. Demo account strings and salted fixture hashes stay confined to this archive. Never import it into src, use it as production data, or cite a passing mock save as real persistence evidence. The production import/bundle gate rejects these modules.

The unchanged archive runs/types separately from current frontend tests and lint, retaining upstream code and assertions without adapting old explicit-any/unused-symbol style. Current src/tests remain under their strict existing checks. Main Vitest discovers only unit/component/contract; it does not accidentally run the global MemoryStorage/window overrides in the current UI test environment.

The v2 specification takes precedence over historical behavior. In particular, old editDraft may recompute an unchanged activity after a weight change; the canonical backend preserves no-op snapshots and recalculates only an explicit changed-input correction after review. Old keyword voice parsing/fixed QA answers remain historical tests, not M2 implementation. Cross-language vectors and actual service/API/PG evidence are documented in docs/verification/demo-regression-mapping.md at repository root.
