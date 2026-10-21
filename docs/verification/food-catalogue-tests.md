# B05 — verified food catalogue and database search

Branch: develop/yuankang-wei. Developer self-check PASS; independent review/UAT PENDING. No shared DB changes or remote Git operations.

## Source decision and reproducibility

Actual FSANZ [AUSNUT data files](https://www.foodstandards.gov.au/science-data/food-nutrient-databases/ausnut/data-files), not legacy AFCD labels, provide 3,741 foods. Raw workbooks remain in ignored `.local/reference-sources/`; normalized factual names/nutrients and the complete SHA/header/unit manifest are tracked in `backend/src/main/resources/data/`. The manifest links each exact official URL and SHA256. It also records FSANZ attribution, the [copyright policy](https://www.foodstandards.gov.au/legal-policies/copyright), and no endorsement. No sampling/description prose, logos or third-party imagery is copied.

Three actual workbooks were inspected with openpyxl3.1.5. `Food details` row3 headers / row5 data join `Food nutrient profiles` row3 headers / row4 data on Public food key, with Survey ID/name checked. `TAB A - Nutrients - Foods ` confirms per100g units. Policy chooses **Energy with dietary fibre (kJ)**, **Protein (g)**, **Total fat (g)** and **Available carbohydrate, without sugar alcohols (g)**; all must be present, finite, nonnegative and within canonical precision. Null never becomes zero. Eight source names have trailing spaces: officialName preserves them exactly; displayName trims only surrounding whitespace. Values are not recalculated.

Reproduce using `python -m pip install -r scripts/data-requirements.txt`, `python scripts/data/fetch_ausnut_sources.py` (pinned HTTPS/SHA; no overwrite of unexpected cache), and `python scripts/data/prepare_ausnut.py` (check only; explicit --write regenerates reviewed artifacts). Source hashes are in `data/manifests/ausnut-2023-r1.json`. Python6 tests: headers, zero, missing/invalid/negative nutrients, mismatched join, duplicate keys and source-byte tampering.

## Runtime behavior and tests

`ReferenceManifestReader` validates all manifest/data/source bytes before any transaction. `ReferenceImportService` uses a transaction-scoped advisory lock and ascending active-reference locks, immutable version checks, complete insert and atomic active flip. Any failure rolls back both catalogue and activation. Same manifest returns replay and never reactivates an old version; changed same-version content returns409. Nothing is enabled from count alone. `FoodReferenceDataLoader` runs only with explicit IMPORT_FOOD_CATALOGUE=true and REFERENCE_SOURCE_DIR, fails closed on absent/invalid source files. Default startup does not import.

Search performs bounded database paging (size1..100), stable ordering, AND word matching with escaped LIKE metacharacters, verified/active/core-complete filtering. Empty configured search is empty; an unconfigured catalogue is503 CATALOGUE_UNAVAILABLE. Canonical IDs are strings. By-id can read immutable inactive history with exact null/zero distinctions. Old public-key paths delegate active lookup. Nutrition paths remain explicitly gated until B07; no old two-place/double preview authority is retained.

New Java tests: ReferenceImportServiceTest5, FoodReferenceServiceTest5, FoodCatalogueIntegrationTest6, FoodImportConditionTest2, one actual authenticated no-catalogue HTTP case. OfficialFoodCatalogueIT imports all3741 from actual pinned raw workbooks in a new UUID database; verifies repeat import, pagination/JSON shape and independent official rice/chicken/banana samples. Rice F007661:659kJ/3.1protein/.2fat/34.7carbs per100g; chicken F002594:412/22.5/.8/0; banana F000262:399kJ.

Actual final command: `TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=test_only TEST_DB_DOCKER_CONTAINER=fitnutri-integration-20261016 ./backend/mvnw -o -f backend/pom.xml verify`. Log `/tmp/fitnutri-b05-final.log`: Surefire138 + Failsafe3 PASS, zero failures/errors/skips. Python unittest6 PASS. Contract validation and generated types checked separately.

Earlier failures were corrected: test Java compound-var syntax; a Mockito test accidentally re-stubbed its own manager; full official import rejected eight official trailing-space names. The latter was resolved by preserving the raw source name and explicitly separating display normalization, not modifying official nutrient facts. Full real import rerun passed.

Backend change BC-04. Database: existing V3 schema only; no new migration, no existing browser/shared catalogue import. The live browser DB is still V2 until separately backed up, rehearsed and upgraded. B07 will implement shared BigDecimal preview/save calculations. Independent review, source-policy approval for shared merge and UAT remain pending.
