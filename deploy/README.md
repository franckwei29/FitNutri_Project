# Static frontend and same-origin API gateway

Node24 built-in HTTP(S) serves only the built frontend and proxies `/api` to the loopback Spring service. It has no business store or provider credentials. The Vite development server is not used. Start backend separately after the reviewed schema/catalogue procedures in `docs/runbook.md`.

Build: `npm --prefix frontend ci && npm --prefix frontend run build` (requires the locked dependency registry or preinstalled dependencies). Start with a trusted certificate:

```sh
TLS_CERT_FILE=/private/operator/fullchain.pem TLS_KEY_FILE=/private/operator/key.pem \
BACKEND_ORIGIN=http://127.0.0.1:8080 HOST=127.0.0.1 PORT=8787 \
node deploy/server.mjs
```

Keep keys outside the repository with restricted filesystem permissions. Default startup requires HTTPS certificate/key paths, otherwise it fails. The backend should bind loopback, use `SESSION_COOKIE_SECURE=true`, and allow the exact browser origin in `APP_ALLOWED_ORIGINS`. No arbitrary forwarded headers are trusted. Do not open public interfaces/firewalls without a separate deployment decision. A production certificate, public DNS/firewall and remote hosting have not been provisioned or verified in this task.

For this owned **loopback-only HTTP demonstration**, explicitly use backend profile `local` and:

```sh
FITNUTRI_LOCAL_HTTP=1 BACKEND_ORIGIN=http://127.0.0.1:8080 \
HOST=127.0.0.1 PORT=8787 node deploy/server.mjs
```

Browse `http://localhost:8787` consistently and add this exact origin to `APP_ALLOWED_ORIGINS`. The local profile permits HTTP Session cookies; this is distinct from production HTTPS/Secure cookie acceptance. Local HTTP refuses a non-loopback bind. Never use it as a public service.

Known SPA paths (including `/history` and `/review`) return index with `no-store`; unknown assets/routes return404. Hashed build assets are immutable. `/api` never falls back to HTML; backend status, cookies, CSRF headers and response bodies pass through, with private API caching forced `no-store`. Client forwarding headers are stripped. Origin-form requests only: absolute/protocol-relative targets cannot redirect upstream traffic.

Gateway12MiB maximum aligns with Tomcat's bounded rejected-body drain; servlet10MB acceptance and decoded-media8MiB limits remain stricter. Gateway85s timeout exceeds server60s model processing plus10s decode and transport margin; client image/QA70s and transcription80s wait remain intentional. A stopped client wait may end before server processing. Gateway413/502/504 are safe canonical JSON with UUIDs and no upstream diagnostics; ordinary backend4xx remain JSON. No automatic retry or request-body logging.

Tests: `node --test scripts/tests/deployment-server.test.mjs`. Controlled upstream fixtures verify proxy mechanics; actual Spring/PostgreSQL/browser results are recorded separately in `docs/verification/same-origin-runtime.md`.

Keep the Spring JAR immutable while running: after verify, `node deploy/stage-backend.mjs` prints a verified SHA256-named private copy under ignored `.local/runtime`; run that path with Java and the intended private environment/profile. `scripts/dev.sh backend-built` does this for explicit local HTTP use. Rebuilding Maven target must not alter a running archive. Two executable artifact tests cover build-path mutation, content-addressed versions, permissions and malformed/symlink rejection.
