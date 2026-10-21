# V3.1 operations and independent UAT

Active UI execution delta: [approved v3.1](../implementation/FitNutri_UX_Feedback_and_Record_Presentation_Execution_Plan_v3_1.md). Retained domain/security/data baseline: [v3](../implementation/FitNutri_UX_Refinement_Execution_Plan_v3.md). Current task ledger: [16 cards](../implementation/ux-v3-1-tasks.md). Actual failures, fixes and command results: [execution evidence](ux-v3-1-results.md). Developer verification is separate from peer review and human UAT.

## Run the current system

Open **http://localhost:15173**. This is the actual React/Vite application, forwarded by the owned test-only18082 transport to the matching immutable Spring application on18080 and its isolated PostgreSQL16/schemaV13 database. The existing private runtime manifest identifies its owner, immutable artifact and synthetic administrator. Do not print or commit that manifest or credentials. Create an example.test synthetic USER through registration and complete onboarding; no default admin password is supplied.

This iteration changes frontend behaviour and its tests. Backend Change:none; Database Change:none; OpenAPI Change:none. No schema migration, reference import or backend restart is required for these changes. The original V8 runtime/database remains separate and was not upgraded or used as a test target. Do not assess current V3.1 through the old8787 built app.

If a previously owned local service has stopped, first verify its process/artifact/database ownership and free ports. Use the retained [V13 operation and migration instructions](ux-v3-uat.md) for the matching backend, with imports/bootstrap off and privately supplied owned DB credentials. No startup should auto-adopt or migrate an unknown database. The same test transport and frontend can be started from the project root and frontend directory respectively:

```sh
FITNUTRI_E2E_UPSTREAM=http://127.0.0.1:18080 FITNUTRI_E2E_FAULT_PORT=18082 \
  node frontend/tests/e2e/fault-proxy.mjs
# In another terminal, from frontend/:
FITNUTRI_E2E_FAULT_ORIGIN=http://127.0.0.1:18082 \
  npm run dev:e2e-fault -- --port 15173 --strictPort
```

Do not stop an unidentified process. This proxy only forwards actual API bytes and can drop an acknowledgement for explicitly armed tests; it is not production middleware or simulated business data. Run one browser worker/suite at a time against its shared one-shot fault controller.

## The new interaction contract

- Add food, a mixed single/dish batch, Quick exercise or Gym workout → Review → explicit final confirmation → validated complete receipt → **today Dashboard**. Preview, media recognition and local Add are not saved records.
- A green floating notice appears after Dashboard actually mounts, stays about2seconds and fades for200ms. It sits below the actual navigation height, leaves document flow and focus alone, and respects reduced motion. Hidden tabs pause visible time. Navigating away ends the shown notice; returning/reloading does not replay it.
- A separate compact `View saved records` link remains available for the current session and shows the actual saved date. It is independently dismissible and is not the disappearing toast. It is not restored from browser storage.
- Historical dates remain historical. Dashboard returns to the current business day in Australia/Sydney; today's totals do not acquire yesterday's records. Follow the dated link to the actual saved roots in History.
- The next explicitly started manual food entry uses today and fresh identities/blank quantities, retaining only the confirmed default meal within the same owned SPA session. Exercise retains the chosen Quick/Gym mode. Reload/sign-out/account-epoch changes clear local entry/feedback preferences.
- A verified save remains saved if overview reads fail. A persistent warning offers `Retry refresh`, which performs reads only. Do not record the same occurrence again because a refreshed view failed.
- A lost/mismatched acknowledgement remains unknown on Review. Check the original receipt or retry the exact frozen submission; a temporary404 does not prove failure. Leaving explicitly discards local recovery, not the server transaction. After a hard refresh use History to check the actual committed state.
- A destination module error uses the existing route failure page. Reload the application and inspect confirmed records; there is no automatic repeat POST or recovery from browser storage.

## Record presentation and controls

Dashboard has distinct Food log and Movement panels with authoritative root counts. Shared summaries emphasise name, consumed portion/time and energy; food macros are secondary. A dish/workout stays one root, independent of its ingredient/period count. Missing energy is unavailable rather than zero; partial estimates stay labelled.

Use `View entry` or the record name to inspect full saved details. Recipe amounts/consumed portions, periods, training loads/sets, original references and saved version information remain accessible there. Closing details returns focus to the saved entry action; deletion moves focus to a surviving group/date heading or main content.

`More actions` contains Edit entry, Log again and Delete entry. Editing retains the compatible History range/type context; it is not treated as a new create-to-Dashboard flow. Log again requires current references and fresh review, with quantities/times blank until explicitly entered or reused. Delete requires whole-root confirmation. Busy/unknown/stale-version/foreign-owner restrictions remain.

Adding pages group date/meal and compact recent choices, with clear Quick/Gym selection. A recent choice checks the current reference/option and moves focus to the next required amount/time without silently accepting the previous amount. Mode/input replacement retains explicit discard protection.

## Developer verification commands

From the actual repository on **develop/yuankang-wei**:

```sh
git branch --show-current
git status --short --branch
sh scripts/check.sh --stage static
sh scripts/check.sh --stage frontend
sh scripts/check.sh --stage data
# Privately configure explicit owned TEST_DB_* values first:
sh scripts/check.sh --stage backend
# Privately configure the owned E2E stack and synthetic admin first:
sh scripts/check.sh --stage browser
```

Set FITNUTRI_PYTHON to an existing valid environment if needed. TEST_DB_ADMIN_URL/USERNAME/PASSWORD/DOCKER_CONTAINER and FITNUTRI_E2E_DB_NAME/ADMIN_EMAIL/ADMIN_PASSWORD must identify the owned test environment. Existing installed Chrome may be selected with FITNUTRI_E2E_BROWSER_PATH. Default Playwright configuration includes the original regression suite and both *-v31.spec.ts files. Discovery/listing is not execution. A sandbox socket/Docker denial must be rerun with permitted access; failed/zero/skipped tests cannot be called PASS. No default command calls paid models or downloads a browser.

Prefer scoped paired tests during implementation, then serial whole gates. Do not run full Maven, browser and frontend workloads concurrently. Preserve screenshots/JSON before a new browser run clears frontend/test-results/ux-v3.

## Independent human UAT — all pending

Record reviewer, date/time, browser/version, viewport, expected/actual result and issue link for each check. Automation and developer screenshot inspection do not tick these boxes.

- [ ] Ordinary food: explicit save returns to today, overview/list update, floating notice clears without moving layout or covering navigation.
- [ ] Mixed dish/single batch: one navigation/notice, correct root/leaf counts, original saved quantities visible in details.
- [ ] Quick and multi-period Workout: correct duration/energy/unavailable/partial state, no duplicate timed movements.
- [ ] Historical entry: today unchanged, dated History link locates all returned roots after the toast disappears.
- [ ] Read failure and unknown save are distinguishable; a refresh does not create records and unknown uses the original proof.
- [ ] Dashboard/History cards have readable hierarchy and padding at mobile/tablet/desktop widths and with long Chinese/English names.
- [ ] More menu works with mouse/keyboard; Edit/Log again/Delete maintain original protection and clear error feedback.
- [ ] Full details and data sources remain available; close/delete focus and History filters remain usable.
- [ ] Date/meal/recent choices and Quick/Gym controls are compact, labelled and do not silently discard input or reuse amounts.
- [ ] Real browser-toolbar200% zoom, intended assistive technology and required additional browsers are manually checked.

## Explicit limits

Current automated visual coverage uses actual installed Chrome at360/375/768/1280/1440px and200% document CSS zoom. CSS zoom is not a claim of browser-toolbar or OS magnification testing. Safari's prior V3 native smoke is historical; this round's Safari is NOT_RUN. Edge is not installed/NOT_RUN. Developer images are synthetic, not user health screenshots.

The owned runtime's image/voice/Q&A live services remain unconfigured. Adapter/controlled-provider and disabled-capability regressions do not prove real model quality or human microphone acceptance. Those remain BLOCKED_EXTERNAL/PENDING. No paid provider call or key search was performed.

Peer review:PENDING. Human UAT:PENDING. The local implementation is IN_REVIEW after actual developer gates; ACCEPTED requires independent sign-off. No fetch/pull/clone/push/PR/merge or public deployment is part of this iteration.
