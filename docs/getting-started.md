# Run FitNutri from a fresh checkout

These instructions use your own PostgreSQL database. Existing personal review databases and ports mentioned in historical reports are not prerequisites. Run commands from the repository root. Use macOS/Linux or WSL2 with a POSIX shell; the shell scripts require that environment.

## Install prerequisites

Install JDK 21 (including javac), Node.js 24 with npm, Python 3, Docker with Compose, and PostgreSQL client tools (psql, pg_dump, pg_restore). Install FFmpeg for audio features/tests and Poppler (pdftotext) for reference-data preparation. Select JDK 21 in JAVA_HOME. Maven is supplied by backend/mvnw; dependencies require network access on first installation.

```sh
java -version
node --version
npm --prefix frontend ci
python3 -m venv .local/tools
.local/tools/bin/python -m pip install -r scripts/data-requirements.txt -r scripts/contract-requirements.txt
```

## Configure your database

Copy `.env.example` to `.env`. Replace CHANGE_ME with your own password. DB_URL must match DB_DATABASE and DB_PORT. If you change the port, change both values. Keep secrets out of Git.

```sh
cp .env.example .env
# Edit .env before continuing.
set -a
. ./.env
set +a
export REFERENCE_SOURCE_DIR="$PWD/.local/reference-sources"
```

Repeat the export step in every backend terminal. Compose reads .env automatically; Java and the existing launch scripts do not. Editing POSTGRES_PASSWORD does not change the password of a previously initialized Docker volume.

For a new installation using the repository's Compose database:

```sh
docker compose config --quiet
docker compose up -d db
```

If the container name or port already belongs to another installation, stop and choose a distinct Compose configuration/name and free port. Do not remove an existing volume. Alternatively, create your own PostgreSQL 16 database and role and set DB_URL/DB_USERNAME/DB_PASSWORD accordingly.

## Initialize an empty database

The backend requires Flyway V1 through V13 and normally validates schema without changing it. The following explicit initialization starts the application with migration enabled. Use it ONLY after confirming the selected database is newly created and empty. It does not baseline a nonempty database. Existing databases require the reviewed backup/preflight procedure in docs/db/migration-foundation.md.

```sh
./backend/mvnw -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local,migration
```

Wait for successful startup, then stop this initialization process with Ctrl-C. Future starts use only the local profile. Do not set legacy timezone approvals for a fresh empty database; legacy conversion is relevant only when migrating existing records. Do not modify migration SQL files or their checksums.

## Prepare and import reference catalogues

The bundled normalized JSON files are required but the importer also checks the pinned official source bytes. Download them into your own ignored cache:

```sh
.local/tools/bin/python scripts/data/fetch_ausnut_sources.py
.local/tools/bin/python scripts/data/fetch_compendium_sources.py
.local/tools/bin/python scripts/data/prepare_ausnut.py
.local/tools/bin/python scripts/data/prepare_compendium_ux.py
```

The Compendium age page is restored from the bundled historical evidence snapshot and checked against the unchanged original SHA256; it is not taken from the changing live page. The official PDF and AUSNUT workbooks are still downloaded and checked. If any source hash fails, do not bypass it or replace the manifest hash merely to proceed; obtain the reviewed pinned source or review a new dataset revision. Network availability remains necessary for those downloads.

Start once with all four imports enabled and explicitly choose the r2 activity manifest:

```sh
IMPORT_FOOD_CATALOGUE=true IMPORT_ACTIVITY_CATALOGUE=true \
IMPORT_FOOD_SEARCH_METADATA=true IMPORT_EXERCISE_OPTIONS=true \
ACTIVITY_MANIFEST=data/manifests/compendium-2024-r2.json \
./scripts/dev.sh backend
```

Wait for all imports and successful startup, then stop with Ctrl-C. The one-command flags above do not persist in the shell. Keep the four values false in .env for ordinary restarts.

## Start the application

In the configured backend terminal:

```sh
./scripts/dev.sh backend
```

In a second terminal at the repository root:

```sh
./scripts/dev.sh frontend
```

Open http://localhost:5173. The frontend proxies /api to the backend on 127.0.0.1:8080. Use localhost consistently in the browser. The local profile permits HTTP session cookies; production uses secure cookies. A different frontend origin requires updating APP_ALLOWED_ORIGINS, and a different backend port requires updating the proxy configuration.

Run `./scripts/dev.sh health`. Register your own user, log in, and complete onboarding. Check food search, exercise selection, saving, history and daily summaries. Restart both services and confirm records persist. No shared user or default administrator password is provided. Optional admin creation uses APP_BOOTSTRAP_ADMIN_ENABLED and the private email/password/display-name variables documented in docs/runbook.md; disable it after provisioning.

## Optional AI functionality

Manual records do not require an AI key. For image, transcription, parsing or Q&A, configure your own backend-only OPENAI_API_KEY, the relevant model names and corresponding AI_*_ENABLED flags. Transcription also requires FFmpeg. Disabled features should report unavailable; this is not a database failure. Never put API keys in VITE variables or frontend source.

## Tests

```sh
npm --prefix frontend run test:unit
npm --prefix frontend run test:component
npm --prefix frontend run test:contract
./backend/mvnw -f backend/pom.xml -Dtest=FoodCalculationServiceTest test
```

The full backend suite requires a SEPARATE PostgreSQL 16 test instance. Copy .env.test.example to a private .env.test, configure its container/credentials, and export it in the test terminal. Its base database name must start with fitnutri_integration_; its role needs CREATE DATABASE. Tests create their own UUID databases. Never point these variables at the application database. Full checks also need downloaded reference sources, Python dependencies and FFmpeg.

```sh
cp .env.test.example .env.test
# Edit .env.test: set a private TEST_DB_PASSWORD and choose unused container/port.
# Keep TEST_DB_ADMIN_URL consistent with TEST_DB_PORT.
docker compose --env-file .env.test -f docker-compose.test.yml config --quiet
docker compose --env-file .env.test -f docker-compose.test.yml up -d test-db
set -a
. ./.env.test
set +a
FITNUTRI_PYTHON="$PWD/.local/tools/bin/python" ./scripts/check.sh
```

The test Compose file uses a separate named volume from the application Compose file. Its PostgreSQL-created role has the database-creation privileges required by TestDatabase. Wait for the container health check to pass before running tests. Do not substitute another person's existing container or use `down -v`.

Browser tests have additional Playwright browser, dedicated application/database and synthetic administrator requirements; see frontend/tests/e2e/README.md and docs/verification/ux-v3-uat.md. Historical screenshots and reports are evidence from prior runs, not proof that your new installation passed.

## Troubleshooting

- Connection refused: check the selected database is running and DB_URL uses its host port.
- Authentication failed: check the role/password in that database; changing .env does not reset an existing volume password.
- Schema readiness error: complete V1–V13 initialization on the intended database.
- Empty/unavailable food or movement lists: finish all four catalogue imports, including r2 activities.
- Login does not persist: use the local profile and one consistent browser hostname/origin.
- Missing TEST_DB_*: configure the separate test database; ordinary application credentials do not substitute for it.
