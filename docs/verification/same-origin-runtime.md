# R01 production-style same-origin verification

2026-10-17, branch `develop/yuankang-wei`. Source schema V8; no migration/import/data cleanup. Source backend rebuilt into the executable JAR and restarted against the existing owned synthetic review database. Normal Vite5173 restored to its direct8080 proxy. Test-only faultproxy8082 stopped with no armed drop/pending submission. Current backend PID35833, gateway PID35655, normal Vite PID35652 at verification time; these are observations, not permanent service identities.

## Implemented and automated

- `deploy/server.mjs`: Node24 built-in static/API gateway, actual dist, no Vite runtime/business store/provider credentials. Default HTTPS requires certificate/key paths; explicit local HTTP must bind loopback. Backend target loopback only; origin-form request restriction prevents absolute/protocol-relative upstream overrides. Private API no-store/cookie/CSRF/status forwarding, known SPA paths, asset404, streaming12MiB bound,85s deadline, safe JSON errors.
- `scripts/dev.sh`: explicit backend/frontend/gateway/health entrypoints, no auto migration/import/install. DB-backed health and CSRF checks print no tokens.
- Application datasource credentials now explicit environment, without committed development defaults. Compose keeps its named volume and PG16 with loopback port/healthcheck and mandatory private password. `DB_PASSWORD=<local synthetic value> docker compose config --quiet`:PASS; Compose did not create/start/remove any container/volume.
- BC-13 protocol repair: actual missing API and unsupported method/media previously became500. RED2/14; GREEN14/14(MockMvc8 + actual HTTP/PG6), now canonical404/405/415, sanitized body and no-store. Framework Allow/accepted-media headers preserved. No security bypass or interface field/schema change.
- Full Maven verify:462Surefire+4Failsafe PASS/0fail/error/skip,55.395s. XML independently summed. Node pipeline4+gateway7 PASS. Gateway tests include actual trusted-CA HTTPS handshake with a temporary synthetic certificate, origin-target rejection, stalled504,413,outage502,SPA/asset404 and session/CSRF forwarding. No browser certificate warning was bypassed.

## Actual stack and browser

Built React at `http://localhost:8787` → Node gateway → current Spring127.0.0.1:8080 → existing PostgreSQL127.0.0.1:55432. Explicit local HTTP profile; no fault proxy and no Vite for this origin.

Actual IAB login with the retained synthetic core user succeeded. Direct `/history?from=2020-01-02&to=2020-01-02` load displayed saved rice250g/1647.5kJ and walking12.5min/213.558kJ, with the soft-deleted food absent. Direct `/review` and reload displayed the honest empty-review guidance. `/chat` displayed current Profile consent restriction and working Profile/recording links, no fixed AI answer. Screenshot: `screenshots/production-same-origin-history.png`.

Separate real HTTP same-origin smoke verified rotated Session ID after login, fresh CSRF after authentication, HttpOnly/Path=/SameSite=Lax cookie attributes, both actual catalogues ready/verified, all unconfigured AI unavailable while ordinary summaries remain200, API404 JSON(no HTML), canonical logout204 followed by401. First diagnostic incorrectly expected logout200; corrected to the existing documented204 without changing application behavior. Three daily-summary HTTP samples:16.14/16.67/14.90ms; these are API-only local measurements, not browser interaction/load timings or Q08 sign-off.

Selected10core/reference tables' row counts/checksums in both existing application DB and original baseline were identical before/after restart and read/login/logout smoke. Protected reports `.local/e2e/r01-{before,after}-source.json` are0600, ignored. Session data is in memory only. No existing user/record/profile/receipt rows changed by this verification.

## Limits

Local production-build/routing/session behavior: developer PASS. Public trusted HTTPS hosting/DNS/certificate/firewall deployment:NOT RUN, not provisioned. HTTPS server mechanics:actual automated test PASS; production Secure-cookie acceptance on a public browser origin:NOT RUN. Chrome/Edge/Safari compatibility, live model/microphone accuracy, independent peer review and UAT remain separately pending. No remote runner, push, PR or merge.

R01 immutable runtime follow-up: a later concurrent Maven repackage altered the running target JAR and produced a500 during independent browser testing. Zero writes verified; actual failure retained. New staged SHA256/0400 JAR avoids mutable build paths; actual same correction and independent-browser journeys passed after restart. Current JVM PID37622 at follow-up, private immutable artifact. See independent-browser-evidence.md.
