# Baseline test evidence — P01

Date: 2026-10-16 Australia/Sydney. Application baseline: 117348a01ded0a08ac82571fafb7b03103bfb50e. All runs use original source.

| Command / working directory | Exit | PASS / FAIL / ERROR / SKIP | Evidence |
|---|---:|---|---|
| `npm run build` / frontend | 0 | build passed; not test cases | TS + Vite 8.2.2, 1082 modules; existing chunk >500kB warning |
| `npm run lint` / frontend | 0 | lint passed; not test cases | no lint errors |
| `npm ls --depth=0` / frontend | 0 | dependency inventory passed | no invalid/missing declared dependency |
| `npm test` / external ../fitnutri-demo | 0 | 15 / 0 / 0 / 0 | original Node test runner, domain + MemoryStorage only |
| `./mvnw --version` / backend | 0 | runtime inspection | Maven 3.9.16 / Java 21.0.10 |
| Java baseline test below / backend | 1 | 0 / 0 / 1 / 0 | Surefire BackendApplicationTests.contextLoads; PG connection unavailable |

```sh
./mvnw -o \
  -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline \
  -Dspring.datasource.username=fitnutri_test \
  -Dspring.datasource.password=test_only test
```

Java compilation succeeded (61 main sources, 1 test). Context failed on unavailable isolated database; this is an environment failure, not a passing backend baseline. Existing contextLoads test has no business assertion and cannot establish feature correctness even when it passes. Raw local Surefire output is ignored under backend/target; do not commit stack traces or DB contents.

Clean-install reproducibility is tracked separately after the safe-copy npm ci attempt; an existing node_modules build does not prove clean installation. Real API, PostgreSQL transactions, E2E, real providers, independent peer review and UAT: NOT RUN/PENDING.

## Baseline reruns after isolated PostgreSQL startup

Same Java command, original application source, dedicated tmpfs PG16 at loopback55432: exit0,1 PASS/0 FAIL/0 ERROR/0 SKIP (4.951s Maven). Earlier failure remains above; no test removed or disabled. Raw log: /tmp/fitnutri-baseline-java.log (local diagnostic only).

Safe-copy `npm ci --offline --ignore-scripts --no-audit --no-fund` in /tmp/fitnutri-baseline-dybdlu5o: exit0,280 packages. Existing project lockfile unchanged. Lifecycle scripts intentionally not executed by this install; production build/lint already exercised from the project's installed dependency tree.
