# ADR-02 — Canonical mvp-1 HTTP contract (P02)

Status: accepted for local implementation under explicit v2.0 request; team integration approval and peer review PENDING. openapi-mvp.yaml is JSON syntax (YAML1.2 subset), avoiding a second parser in runtime code. Contract changes intentionally coordinate both clients and server. No API implemented simply by defining schemas.

IDs decimal strings; version numbers integers; numeric snapshots kJ and nullable missing values; inclusive business-date ranges max366 days. Profile full snapshots require explicit null goals; onboarding atomic, QA consent optional, legacy adults reconfirm without initialWeight. /today and /daily return DaySummary; /weekly returns current/previous PeriodSummary; /trends PeriodSummary. No implicit energy goal.

All 32 original mapped operations are retained/upgraded in this contract. Legacy single food write accepts canonical one-item payload or CONTRACT_UPGRADE_REQUIRED, never arbitrary calories. Legacy by-key reference resolves active entries; immutable by-id reads historical versions. Goal deletion remains version-checked. Existing reset-password is version-checked, no new UI. Every private path requires active USER ownership; ADMIN accesses minimum account management only. Unknown response fields may be added by a coordinated contract revision; requests reject undeclared writable identity/nutrients.

Created rows have item UUIDs and submission receipts. A 404 receipt is only not observed yet. Replay before changed-reference revalidation; no resurrection. Edits/deletes use expectedVersion, source/client identity retained. Unchanged updates retain snapshot/version. Known rejection, unknown outcome and saved-but-refresh-failed stay separate.

Image8MiB; audio8MiB/60seconds, frontend/server/provider format intersection verified before advertising. QA2000 characters /31days, output10000, raw safety/identity excluded; no persistent media/chat. Missing official manifest mappings and provider models are external inputs, not invented defaults.

New receipt technical table is the specified ADR-03 increment: nine business tables plus one narrowly scoped idempotency table, no health answers or media. Local implementation authorised by user; shared baseline adoption, legacy timezone/conflict decisions and merge review remain gated by explicit evidence/approval.
