# ADR-04 — Versioned administrator account changes (BC-12 / B14)

Status: implemented; developer tests pass; independent review and UAT pending.

The existing controller wrote enabled/passwordHash directly and returned an unbounded list. The v2 execution specification requires a bounded identity-only list and explicit optimistic versions. Preventing self-deactivation and loss of the last enabled administrator is an engineering enhancement requested by v2 §13.10 and the concurrency addendum, not an original Proposal requirement.

Every status/password mutation enters one database transaction and locks all ADMIN rows with `SELECT … ORDER BY id FOR UPDATE` before reading the actor and locking the target. Roles cannot be edited through this API. This stable lock order serializes competing administrator changes; after waiting, the service rereads current enabled/role authority. It then verifies the target's expectedVersion. It rejects self-deactivation and deactivation when at most one locked administrator is enabled. A no-op with a current version does not increment it; a stale no-op still conflicts. Real concurrent mutual-deactivation and same-target tests verify authority rechecking and one-winner version behavior. Locking only the target or counting unlocked administrators would not provide these guarantees.

Search is server-side across email/displayName, case-insensitive with escaped SQL LIKE wildcards, stable id ordering, page/size bounds and totalMatches. Responses contain exactly id (string), email, displayName, role, enabled and version. No profile, health, record, hash or private timestamp fields are returned. All responses use no-store. Existing Spring Security ADMIN/CSRF checks remain in addition to fresh service authorization.

The retained reset-password route requires expectedVersion, validates the existing password policy plus BCrypt's 72 UTF-8 byte limit, hashes the password and increments the account version. It has no new frontend flow. Its pre-existing session policy is unchanged: disabled accounts are rejected on every request; password resets do not by themselves revoke other active sessions. This limitation is explicit and is not represented as global session revocation.

No migration, role reassignment, account deletion, record recalculation or data clearing is introduced. V2's existing app_user version/updated_at columns are prerequisites. Local developer tests use owned UUID PostgreSQL 16 databases only. Shared rollout, human review and UAT remain pending.
