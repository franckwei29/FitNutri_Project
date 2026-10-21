# Real local-stack browser regression

These tests use the actual React application, Spring session/CSRF endpoints and explicitly selected owned PostgreSQL database. They never fulfill business requests with invented responses. The fault proxy changes transport delivery only. Do not point this harness at a shared or production service.

Start the already migrated/verified isolated backend on loopback8080 (bootstrap disabled), then run in separate terminals:

```sh
node frontend/tests/e2e/fault-proxy.mjs
npm --prefix frontend run dev:e2e-fault
```

The separate test Vite configuration preserves localhost5173 and proxies `/api` through loopback8082. Normal `npm run dev` remains direct to8080. Stop an existing Vite process before starting this variant. There must be no unresolved submission when changing test services.

Explicit database controls are mandatory; there is no default database:

```sh
FITNUTRI_E2E_DB_NAME=fitnutri_integration_browser_20261016 \
TEST_DB_DOCKER_CONTAINER=fitnutri-integration-20261016 \
TEST_DB_USERNAME=fitnutri_test \
npm --prefix frontend run test:e2e
```

Use `npm --prefix frontend run test:e2e -- --list` for discovery. Browser binaries must already be installed for a CI Playwright execution; this change does not install/download them automatically. During Codex development the same `browser-journeys.mjs` steps run through the documented IAB facade; CI test discovery/typechecking is reported separately from actual browser observations. No external Playwright browser driver was launched during the recorded IAB verification.

Each case generates a new synthetic `e2e-…@example.test` account. The SELECT-only verifier restricts its queries to that identity, explicit test role/container/database and schemaV8+. `acquireSyntheticOwnerLock` additionally holds only that generated owner's row; its `finally` release commits the lock transaction without changing any row. It enables the actual POST timeout/receipt404/late-commit race without changing application code. Never reuse it for another person's account. Test data is retained for audit; there is no delete/reset/cleanup SQL.

Fault controls only arm one successful food/exercise create acknowledgment drop. The proxy forwards real cookies and CSRF but retains no credentials, cookie values, health bodies or raw request/error/provider bodies. Its bounded status contains UUIDs, body SHA256, resulting record IDs, actual HTTP status and delivery metadata only. Known4xx responses are delivered normally. Client disconnect does not cancel an already dispatched backend transaction. Control/status bind only loopback; this test-only server must not be deployed.

Credentials, local case snapshots and reports belong under ignored `.local/e2e`, `frontend/test-results` and `frontend/playwright-report`. Tracing/video are off. Failure screenshots can still contain synthetic record information; inspect before sharing. Peer review/UAT are separate and pending.

The separate administrator-context case additionally requires `FITNUTRI_E2E_ADMIN_EMAIL` and `FITNUTRI_E2E_ADMIN_PASSWORD` for an already provisioned **synthetic** administrator (`synthetic…@example.test`) in this same owned database. Supply them through private environment configuration; never commit them. The test cannot bootstrap/promote an administrator or skip itself when credentials are missing. Independent-context tests close their own browser contexts but retain synthetic records for audit. Actual IAB double-tab verification is explicitly distinguished from execution of those independent-context wrappers.
