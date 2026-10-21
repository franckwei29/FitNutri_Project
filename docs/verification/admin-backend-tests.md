# B14 — Administrator service developer verification

Branch: develop/yuankang-wei. BC-12; no database migration; existing V2 account version prerequisite. Engineering self/last-admin protection is documented in ADR-04. No remote operations.

Paired automated files:
- AdminServiceTest: 8 JUnit 5/Mockito cases for lock order, stale/no-op behavior, protected administrator states, wrong role/missing account, missing/negative versions, bounded search and UTF-8-safe password reset.
- AdminControllerTest: 3 MockMvc cases cover missing/null/string/fractional/negative versions, unknown authority/private fields, password validation, exact six-field DTO and no-store.
- AdminHttpIntegrationTest: 8 real HTTP/PostgreSQL cases cover server search/pagination/literal wildcard escaping, USER403 and ADMIN private-resource403, live-session deactivation, restoration preserving dated weight, self protection/missing identity, actual new-password login/stale reset, CSRF, mutual-admin concurrency and same-version concurrency.

Arrange–Act–Assert assertions check actual persisted versions/state/weight and HTTP status/body, not contextLoads. No mocked PostgreSQL, skips or shared database writes. Cancellation is a UI acceptance case for F16; B14 exposes no write before an HTTP mutation.

Commands actually executed:
`./backend/mvnw -o -f backend/pom.xml -Dtest=AdminServiceTest,AdminControllerTest test` — 11 PASS, 0 fail/error/skip.
`TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=<isolated-test-password> TEST_DB_DOCKER_CONTAINER=fitnutri-integration-20261016 ./backend/mvnw -o -f backend/pom.xml -Dtest=AdminServiceTest,AdminControllerTest,AdminHttpIntegrationTest test` — 19 PASS, 0 fail/error/skip. Logs /tmp/fitnutri-b14-unit.log and /tmp/fitnutri-b14-focused.log.

Full regression: same explicit isolated PostgreSQL environment with `./backend/mvnw -o -f backend/pom.xml verify` — 319 Surefire + 4 Failsafe PASS, 0 failures/errors/skips; BUILD SUCCESS. Log /tmp/fitnutri-b14-full.log. OpenAPI validation: 44 operations / 58 schemas / 562 valid / 11 negative examples PASS; generated TypeScript/Zod58 no drift. Independent group code review, shared deployment and UAT remain PENDING. Password reset does not globally revoke existing enabled sessions (existing policy retained); no password-reset UI is added.
