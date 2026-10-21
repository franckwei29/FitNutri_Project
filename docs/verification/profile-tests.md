# B03 — atomic onboarding and profile settings

Branch: develop/yuankang-wei. Developer verification only; peer review and UAT pending.

Changed ProfileController/Response; added CompleteOnboardingRequest, ProfileSettingsRequest and ProfileService; retired UpdateProfileRequest. UserRepository now exposes a pessimistic owner lock; WeightEntryRepository exposes latest measurement-date lookup. Added ProfileServiceTest and ProfileHttpIntegrationTest.

BC-02: POST /api/profile/onboarding atomically writes display name, full settings and initial dated weight, then marks completion. Failure of the late weight insert rolls back previously flushed user/profile writes. Completed repeated POST returns ONBOARDING_ALREADY_COMPLETED. PUT uses a complete snapshot with explicit nullable goals and two expected versions, and never appends weight. Missing properties and unknown legacy weight fields return 400. Compatibility goal DELETE routes delegate to the same lock/version policy. Server-only eligibility derives from current answer and consent. A false consent explicitly permits completion. Unchanged legacy consent/health answers retain unknown timestamps; adult reconfirmation uses PUT without new weight. requireCompletedAdultSetup is available for dependent record/QA services.

Database: existing V2 columns/constraints only; no migration or shared database change. Every test database was a new owned UUID PostgreSQL16 database, cleaned on JVM shutdown. Duplicate-weight rollback test inserts only a synthetic measurement in its owned database. No existing data is removed.

Actual commands (project root; synthetic test credentials via environment):

```
TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=test_only ./backend/mvnw -o -f backend/pom.xml -Dtest=ProfileHttpIntegrationTest test
TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=test_only ./backend/mvnw -o -f backend/pom.xml verify
```

Test-first run against old endpoint: 10 errors/0 passed because canonical profile fields were absent (log /tmp/fitnutri-b03-red.log). First implementation: 10 passed, 0 failure/error/skip (/tmp/fitnutri-b03-first.log). Added concurrency and isolated Mockito rules: final Surefire70 plus Failsafe1 passed, 0 failure/error/skip (/tmp/fitnutri-b03-final.log). B03 adds 16 meaningful tests: service5 and real HTTP/PostgreSQL11. Assertions cover missing profile/null version, false consent, duplicate onboarding, explicit null versus omission, invalid/underage/future input, both version conflicts, late database rollback, legacy adult reconfirmation, derived eligibility, versioned compatibility deletes and two simultaneous onboarding calls creating one initial weight. git diff --check passed.

Frontend settings adoption is F03/F06; current UI is not claimed complete. Old clients using partial PUT must adopt the canonical contract. Weight correction remains B04. No remote Git operation, review, merge or UAT performed. Next executable task: B04 and F03.
