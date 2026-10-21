# B04 — dated weight correction

Branch develop/yuankang-wei. BC-03. No new database migration; existing V2 date uniqueness/version/precision constraints remain authoritative. All changes tested only in owned UUID PostgreSQL16 databases. Peer review/UAT pending.

Files: profile/WeightController, WeightService, AddWeightRequest, WeightEntryResponse, WeightConflictException/Response, WeightEntryRepository; WeightServiceTest, WeightServiceIntegrationTest and two additional ProfileHttpIntegrationTest cases. OpenAPI adds WeightConflictResponse (optional owner-scoped currentItem) and generated TypeScript/Zod boundaries; generator schema count is now dynamic.

New dates require expectedVersion=null and return201. Existing same value without correction intent returns200 without changing version. Different values without version or stale explicit versions return409 plus currentItem. Explicit current version corrects the same row and increments version. Owner row lock serializes concurrent new-date creation; database unique constraint remains final protection. Existing incomplete users can maintain weight without fabricating completed setup. Profile legacy weight cache follows maximum measurement date; currentWeight/profileVersion reflect committed state. Backfill never overwrites the latest measurement. Range history requires from/to, validates 1–366 inclusive days and returns owner rows only. Exercise history is never recalculated as a side effect.

Test-first command (`-Dtest=WeightServiceIntegrationTest test`) failed compilation because WeightService did not exist (/tmp/fitnutri-b04-red.log). First implementation6 persistence tests passed (/tmp/fitnutri-b04-first.log). Final command from project root:

```
TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=test_only ./backend/mvnw -o -f backend/pom.xml verify
```

Actual final Surefire81 + Failsafe1 PASS, zero failures/errors/skips (/tmp/fitnutri-b04-final.log). New tests11: Mockito3 (invalid values before persistence, locked active/USER enforcement, stale equal-value correction); PostgreSQL6 (create/no-op/correct/stale, backfill/profile versions/history, invalid/missing targets, concurrent creation201/409 exactly one row, owner isolation, unchanged existing exercise estimated-calories snapshot); HTTP2 (201/200/409/currentItem/string ID, required fields and range rejection). Canonical exercise snapshot columns are D04/B10 work; this test proves the existing persisted legacy value is untouched, not that future schema is implemented.

`/tmp/fitnutri-validation-tools/bin/python scripts/validate_contract.py`: OpenAPI44 operations/57 schemas valid,558 examples accepted/11 negatives rejected. `python3 scripts/generate_api_types.py`: no drift. Frontend `npm test`:41 PASS; `npm run typecheck:test`, lint and build PASS. Existing bundle warning695.75KB remains visible. `git diff --check` PASS. No shared migration, remote operation, independent review or UAT. Next executable work: F03/F04/F06 or D03.
