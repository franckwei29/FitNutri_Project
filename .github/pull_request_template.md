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
