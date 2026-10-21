# UX v3 backend change register — target, not completed

Source of truth: v3 §§12–19. Existing BC history remains intact.

| ID | Why / existing entry / selected target | State |
|---|---|---|
| UX-BC01 | Existing substring/alphabetical food search cannot provide smart matching / FoodReferenceService/Repository / Add UX search metadata, smart search and facet/alias results; retain full catalogue access without fabricated matches | TODO |
| UX-BC02 | Recent items must come from this user's confirmed records; the frontend should not download the full catalogue / RecordReadService/Repositories / Add readonly food/dish/exercise recent and Log again details; enforce owner isolation and inactive-reference rules | TODO |
| UX-BC03 | An original dish recipe and shared portion cannot be represented as one ordinary FoodRecord / FoodRecord/new FoodDishRecord / Add parent, recipe quantities, ratios, grouping and precision; never infer groups for legacy data | TODO |
| UX-BC04 | A one-third portion cannot be implemented by submitting frontend-rounded nutrients / FoodCalculationService / Add scaled calculations and dish previews; retain server authority and independent numeric tests | TODO |
| UX-BC05 | Flat V1 batch receipts cannot represent parent groups and child identities / SubmissionService/Hasher/Receipt / Add V2 operations, minimal typed root/leaf proofs and canonical hashes; retain V1 receipt read/write verification | TODO |
| UX-BC06 | A batch of singles and dishes must save together / FoodRecordService/new FoodEntryService / V2 create/update/delete aggregate roots; no HTTP write loop; ensure atomicity, concurrency handling and rollback | TODO |
| UX-BC07 | Product equipment options differ from the official energy catalogue and current coverage is limited / ExerciseCatalog/Activity import / Add ExerciseOption and MovementCatalogue with source mappings and optional parameter definitions | TODO |
| UX-BC08 | Gym periods and movements must persist without double counting / ExerciseRecord/new WorkoutRecord / Add parent groups, period links, movement details and equipment parameters; extend constraints for absent applicable references | TODO |
| UX-BC09 | Quick options and multi-period Workouts need unified previews / ExerciseEstimateService / Reuse dated estimates by period, explicitly support unmapped options and combined fingerprints | TODO |
| UX-BC10 | Workout creation/correction/deletion needs parent transactions and versions / ExerciseRecordService/new WorkoutService / V2 creation and root mutation; prevent legacy child mutations bypassing parent versions | TODO |
| UX-BC11 | New parent-card counts differ from legacy leaf counts; summaries must not duplicate values / SummaryRepository/Service/RuleSummaryService / Retain leaf sums; add root counts, grouped reads and typed highlights | TODO |
| UX-BC12 | Image/voice currently use flat drafts and candidate UI / FoodCandidateMatcher/Speech adapters as needed / Candidates may use existing catalogues and enter the shared draft through the new single-item adapter; no automatic dish grouping or recipe calculations | TODO |
| UX-BC13 | New identities, fields and schemas require strict compatibility and security / OpenAPI/generator/ApiError/guards / Strict V2 DTO inputs, typed IDs, owner/version checks, minimal receipt boundaries and no secret disclosure | TODO |
| UX-BC14 | Flyway has actual existing history; V1–V8 cannot be edited / migration/preflight/Schema verifier / Append migrations only, verify backup/restore, version source manifests and define rollback strategy | TODO |

All14: retain V1 interfaces and original receipts, owner/CSRF, source provenance and original snapshots. Migrations append V9–V13 only, isolated preflight/backup/restore first. Rollback is forward fix or explicitly verified compatible backup restore; never clean/down-truncate. Source paths, concrete tests, migration evidence and commit references are appended with each paired implementation task; contract-only work is not endpoint completion.

## UX-BC01 / UX-BC14 — UX-D00 implemented database foundation
Why: reviewed short names/facets/aliases require separate versioned metadata without changing official nutrients. Files: V9__food_search_metadata.sql, FoodSearchProfile.java, FoodSearchAlias.java, SchemaReadinessGate.java, SchemaReadinessCustomizer.java; paired SearchMetadataSchemaIT and readiness/migration/restore regression. API impact:none yet; smart-search importer/controller remain TODO. V9 applied only to newly created isolated test databases; old references/records not rewritten. New source requires V1–V9 before Hibernate validation; migration remains opt-in. Refer to ../verification/ux-v3-migration.md for actual preservation/restore tests and forward-fix/new-DB restore recovery. BC01 is PARTIAL (schema done, matching service pending); BC14 PARTIAL (V9 verified, V10+ and rollout pending).

### UX-BC03/04/14 — UX-D01
Why: old flat scale3 ledger cannot retain recipes and precise personal portions. Files:V10__food_dish_records.sql, FoodDishRecord.java, FoodRecord.java, approved-history gate and paired DishSchemaIT/compatibility tests. API:V1 input bounds unchanged; V2 services still pending. DB:V10 only isolated tests; old quantities preserved, dish links NULL. Recovery:forward migration or explicitly verified new-DB backup restore, never scale-reducing down SQL. Evidence:ux-v3-results.md UX-D01.

### UX-BC05/13/14 — UX-D02
Files:V11__submission_receipt_v2.sql,SubmissionOperation.java,approved schema version,ReceiptSchemaV2IT. Why:V1 flat proof cannot express typed roots/leaves. Old operations/hash/JSON retained; new operations separate strict minimal proof. DB only isolatedV11; immutable/deferred triggers retained. Recovery:forward fix or verified new-DB compatible restore, no down migration. Tests32+3PASS; V2 submission service pendingB05.

### UX-BC07/08/14 — UX-D03
Files:V12__exercise_options_workouts.sql,ExerciseOption/MovementCatalogue,WorkoutRecord/WorkoutMovementLog,ExerciseRecord mappings,WorkoutSchemaIT. Why:product machines/actions differ from official MET references and movement count must not multiply duration. API:none activated; catalogue importB09 and grouped servicesB11–14 pending. DB:new owned versioned option/parent/detail structures; old rows unchanged; explicit null-estimate branch retains original guards. Tested30+5PASS including actual restored executable. Recovery:forward fix/new-DB compatible restore; no down SQL.

### UX-BC03/08/13/14 — UX-D04
Files:V13__group_consistency_indexes.sql,preflight_ux_v3.py,GroupConstraintsIT,test_preflight_ux_v3.py,canonical quantity DTO/paired read test and approved version13. Why:aggregate consistency cannot rely on separate child requests or UI checks; upgrades must detect conflicts first. API:unchanged V1 numerical values with consistent scale12; V2 writes not activated. DB:deferred final-state checks/identity guards and indexes, migration conflict rollback verified; all application databases unchanged. Recovery:forward fix or compatible protected backup restored to new DB, no destructive down SQL. Full Java469+22/data28PASS.

### UX-BC01 — UX-B00
Why:UX aliases/short names must bind exact immutable references without changing nutrition. Files:food-search-metadata-v1.json,FoodSearchMetadataReader/Importer/DataLoader,validate_ux_catalogues.py and paired tests. API:none yet; default startup flagfalse. DB:no new migration, explicit atomic V9-metadata import into isolated test catalogue only; all reference checksums unchanged. Idempotent exact-revision replay, conflict refusal and read-only dry-run verified. Rollback:deactivate via reviewed later metadata revision; never change official source/history.

### UX-BC01 — UX-B01
SmartFoodQuery/SearchService/Controller + bulk immutable reference fetch. Why: support reviewed aliases/explicit facets, relevant owned recency and full server pagination without substring false matches. API: additive authenticated GET /api/food-reference/smart-search, private no-store, bounded query/facets/pages; old endpoint unchanged. DB: read-only V9 metadata/V13 ledger; no migration. Actual official3741 catalogue tests and API/error/unit regression PASS; frontend integration remainsF05. Task card annotationBC02 differs from §12 register; actual scope isBC01 here.

### UX-BC02/13 — UX-B02
FoodRecentService/Controller, FoodSnapshotReader and explicit /api/v2/** USER gate. Why: recent is a bounded owned historical hint, not a client-side catalogue scan or mutable recipe template. API:additive GET /api/v2/food/recent, current owner only, no-store; reusable detail reader will be exposed byB08. DB:read-only, no migrations/snapshot rewrites; inactive refs remain identities and need current-reference availability resolution. Actual PG4 + API/read/auth tests PASS. Frontend active-reference resolution and explicit last-amount/recipe actions followF04/F06.

### UX-BC04 — UX-B03
PortionNormalizer, DishCalculationService, validation-only FoodCalculationService entrypoints and paired rational/calculation tests. Why: fractions must not become rounded percentages and consumed display grams cannot be nutrient calculation inputs. API:internal calculation only; V1 methods/formula/fingerprint unchanged. DB:none. Unit18PASS incl existing canonical calculator/API; actual new create persistence followsB06.

### UX-BC04/13 — UX-B04
FoodEntryInputs/PreviewService/V2Controller and CanonicalV2Json; nullable historical aggregation retained. Why: Review and later save require the same authoritative mixed calculation and owned-original exception. API:additive POST /api/v2/food/preview and /entries/{kind}/{id}/preview; strict input, current owner/version, private no-store. DB:read-only, no migration/writes. Tests17+7PASS. Full submission hashing/proofs followB05; actual atomic create followsB06.

### UX-BC05/13 — UX-B05
CanonicalEntryHasherV2, SubmissionReceiptV2Response, SubmissionService V2 strategy/lookup, SubmissionV2Controller and strict food/exercise input validators/DTOs. Why: grouped roots require complete typed proof and replay without reevaluating current reference/profile/date dependencies. API:additive current-owner GET /api/v2/submissions/{id}; old V1 receipt/hash behavior retained. DB:uses original submission_receipt + V11 typed validator; read-only collision lookup spans V13 roots/leaves, no new table/migration. Tests27+9PASS; actual food/workout business creates followB06/B12.

### UX-BC03/04/05/06/13 — UX-B06
FoodEntryV2Service/DishRepository + authoritative conflict response/controller, strict generated contract/client error adaptation and paired synthetic business/HTTP/client tests. Why: mixed confirmation must persist actual consumed leaves atomically and return committed proof; stale authority must request renewed review. API:additive POST /api/v2/food/batches201new/200replay;409 may carry FoodCalculationChangedV2.currentPreview. Contract grows107schemas,64operations unchanged; generated types updated by script. DB:uses existing V10–V13, no migration; old food/source snapshots untouched. Full Java501+43/frontend371/Python33/staticPASS. Parent correction/deletion and legacy child write guard followB07 immediately; UI entry integration stillF04+.

### UX-BC06/13 — UX-B07
FoodEntryMutationService/strict UpdateRequest, root PUT/DELETE, shared SubmissionService unused-ID check and FoodRecordService V1 group guard. Why: food child edits must not bypass the owned parent's optimistic version; failures must leave the whole occurrence unchanged. API:additive root mutation; V1 grouped child update/delete409 GROUP_EDIT_REQUIRED; known-rejection client decode added. DB:existing V10–V13 only, parameterized atomic updates/soft deletes, exact one root version, no migration/hard deletion/receipt rewrite. Actual tests32+17 and client7/staticPASS. CardBC07 annotation differs from master register; this actual scope is BC06/13.

### UX-BC02/06/13 — UX-B08
FoodEntryReadRepository/Service/Controller, bulk FoodSnapshotReader and reused recent reads. Why: mixed roots need bounded pagination without child duplication; unknown legacy inputs cannot fit a verified-current read DTO. API:additive owned range/detail GET; mandatory dataQuality/displayName and strict current/legacy FoodRootV2 read union, generated clients112schemas updated. Existing V1 reads unchanged; legacy read schema is not a create input. DB:read-only batch queries, no migration/retroactive grouping/recalculation. Tests18+20 incl actual cookie/CSRF HTTP journey; frontend contract10/static/examples705PASS. Actual frontend pages and exercise root reads follow later cards.

### UX-BC07/13/14 — UX-B09
ExerciseOptionCatalogueReader/Importer/DataLoader,38/36reviewed product JSONs, additive26-row official r2/source preparer, expanded offline validator and shared source locks. Why: physical devices/movements cannot reuse conditional activity coefficients indiscriminately or each acquire a fictitious MET. API:none yet; query endpointsB10. DB:data import only on explicit flags, existing V12 structures, immutable old r1 preserved; missing source fails closed, no default seeding. Default activity manifest remainsr1; explicit reviewed r2 selection supported. Tests12+6/data39/staticPASS incl actual source import/late rollback/preflight. No user DB migrated/imported.

### UX-BC07/13 — UX-B10
ExerciseOptionService/MovementCatalogueService/Controller, shared bounded SQL query/page DTO; existing V2 frontend clients now carry supported filters. Why: users need reviewed searchable devices/movements with server whitelists and honest source/unavailable reasons. API:additive owned-session catalogue GETs, explicit option referenceId/mappingReason, strict filters, no-store and503fault vs200no-match. Transaction/account-check resource failures now safe503 without authority bypass/session destruction; failed writes remain unknown client outcomes. DB:read-only existing metadata, no migration. Tests28+6/client28/staticPASS.

### UX-BC09/13 — UX-B11
WorkoutEstimateService/EquipmentDetailsValidator, strict preview-only ExerciseEntryInputs, catalogue repositories and ExerciseEntryV2Controller; generated preview reason and frontend response invariants. Why: Quick/Workout need identical dated authority and truthful partial unavailable totals without per-movement estimates. API:additive POST /api/v2/exercise/preview, private no-store, explicit reason/null status. DB:read-only existing V12/V13; no migration or record write. Tests21+8/client22/staticPASS; confirmed atomic saving followsB12.

### UX-BC05/09/10/13 — UX-B12
ExerciseEntryV2Service/WorkoutRepository/WorkoutMovementRepository, create controller and ExerciseCalculationChangedV2 boundary; client errors/generated schema updated. Why: group/detail/proof must commit atomically and source changes require renewed review. API:additive POST /api/v2/exercise/entries201/200 and typed409. DB:existingV12/V13 only, no migration, no user data changes. Sorted metadata/source shared locks avoid nullable-join lock failures; current owner submission transaction controls all persistence. Tests20+9/client12/staticPASS. Old API calculator/creation regression retained; parent mutations followB13.

### UX-BC09/10/13 — UX-B13
WorkoutSnapshotReader/WorkoutCorrectionPreviewService/WorkoutMutationService, strict correction/update DTO, scoped estimate/optional movement validation, V1 ExerciseRecordService group guard and V2 controller/client checks. Why: historical detail edits must preserve dated snapshots and cannot bypass root optimistic version. API:additive owned correction preview/PUT/DELETE; old grouped child PUT/DELETE409GROUP_EDIT_REQUIRED. DB:existingV12/V13 parameterized transactions/soft deletion only, no migration/user data change or receipt rewriting. Tests39+17/client11/staticPASS; no-op and concurrent/late-failure rollback tested. Exposed mixed/current/legacy reads followB14.

### UX-BC02/10/13 — UX-B14
ExerciseEntryReadService/RecentService/ReadController, reusable snapshot assembler, complete mutation response, generated mixed current/unmapped read union and client invariants. Why: one training occurrence is one history card, while legacy unknown authority and stored names/optional details must survive. API:additive V2 owned range/detail/recent GET, explicit periodCount/dataQuality/displayName/periodDetails/selectableForNew; create contract unchanged. DB:read-only bounded owner queries, no migration/rewrite/user data changes. Tests17+19/client11/staticPASS incl real cookie/CSRF grouped journey and serialization discriminator repair. Frontend Log again selection/fill behavior followsF04/F10/F14.

### UX-BC11/13 — UX-B15
SummaryRepository/Models/Service and QaContextBuilder add root counts without redefining old leaf counts or sums. FoodSnapshotReader exposes componentCount; generated contracts/examples/clients validate count and identity. Why: grouped cards cannot compare their length to component/period totals, and Q&A must not double aggregate parent/children. API:additive foodEntryCount/exerciseEntryCount and componentCount; existing V1 count semantics preserved. DB:read-only distinct typed root counts in the same leaf query/snapshot, no migration, data rewrite or user DB changes. Full Java522+PG87/frontend392/data39/staticPASS; protected backup/restore/current source verified. Actual frontend state/UI integration followsF04 onward.

### UX-BC13 — UX-F04 frontend state boundary
DraftTypes/Reducer/Validation/Selectors plus grouped reducer/portion and reuse helpers; existing entry/media/correction consumers and tests migrated to derived single/reference adapters. Why: shared ownership/recovery must survive grouping without duplicate mutable domain state or accidental V1 flattening. Backend/API/DB:none changed; generated121schemas unchanged. Full frontend405/static/buildPASS; actual V2 business services remain previously verified, new review/coordinator wiring followsF08/F09/F12.

### UX-BC01/02/13 — UX-F05 frontend smart/recent boundary
SmartFoodSearch/FoodReferencePicker, actual search/current-reference adapters, owned hooks and selection metadata. Why: explicit source-backed choices and current availability must replace database-code browsing/automatic guessing. API/Java/DB:no server changes; consumes existing real B01/B02 endpoints with strict owner/page/ID checks. Tests25/static/buildPASS; frontend live browser integration pendingQ01. Obsolete per-field confirmation UI/fields removed by the immediately following F06 shared composer work.


### UX-BC13 — UX-F06 composer and review input boundary
Only frontend input state/validation changed. Pending composer and replacement targets cannot be omitted from a frozen request. Selected real references, positive quantities, explicit meal and final Review replace obsolete boolean confirmation gates for every food source. Existing owner/epoch/unknown-state restrictions and strict transport adapters remain. No Java/API/DB migration or user data changes; grouped V2 Review/save wiring follows F08/F09, advanced media UI convergence F16. Actual test evidence is in ux-v3-results.md; peer/UAT pending.


### UX-BC04/05/13 — UX-F07 dish input and preview boundary
Reuses existing strict food V2 preview and owned recent/by-id reads; no new backend route or schema. Explicit conversion preserves original fraction/percent/serving kinds and per-ingredient input grams; Full recipe cannot disguise an absent portion as PERSONAL. Client additionally binds ordered preview identities/ref IDs/input grams and aborts stale reads. Nutrition comes from read-only server preview, never a client formula or an old recipe total. Reuse preserves current date/meal and requires active reference selection; stale revision cannot replace input. No DB migration or existing data rewrite. Existing B03/B04 rational precision/real-PG tests stand; actual new UI/contract tests in ux-v3-results.md. Root batch Review/save follows F08/F09.


### UX-BC13 — UX-F08 root Review boundary
Reuses strict V2 food batch preview, no route/schema or Java changes. Current previews now reject null totals and bind complete ordered input; historical unknown snapshots remain legal reads. Shared root editor consumes batch-authoritative values instead of independently stale leaf values. Full route/confirm/frozen submission wiring followsF09; not claimed connected yet. No DB migration/user data mutation. Component23 and adjacent44 actual tests/static/build recorded in ux-v3-results.md; browser/peer/UAT pending.


### UX-BC05/06/13 — UX-F09 V2 owned submission and proof boundary
Actual manual food route now submits the explicit FOOD V2 envelope to existing transactional batches. Pure input/preview and typed receipt validators bind complete owned input and proof; unknown outcomes retain immutable original identity. Prior V1 compatibility flow remains discriminated separately. Success creates only a new empty local composer, not another server record; refresh retries are reads. Typed lastSaved metadata/previous-date context is memory-only. No Java/API/generated schema or database migration change, no user data rewrite. Existing B05/B06 real PostgreSQL atomicity/receipt tests remain the backend evidence; actual new state/coordinator/router/transport tests are recorded separately in ux-v3-results.md. Browser/peer/UAT pending.


### UX-BC08/09/13 — UX-F10 exercise UI boundaries
Existing reviewed option/movement directories, owned recent roots and dated V2 preview are reused. No new server endpoint, source mapping, formula or migration. Frontend preserves exact option IDs, creates one timed strength period for a selected movement, applies optional-field whitelist and distinguishes explicit unavailable preview from failed service. Cancellation support preserves existing transport-call compatibility; page/ID response checks tightened. New request mapping strips UI catalogue objects/hints and sends no client authority fields. Quick/Gym route and submission connection followsF11/F12; actual paired test evidence in ux-v3-results.md.


### UX-BC09/10/13 — UX-F11 period and movement UI
Existing real V2 period preview and reviewed movement directories reused; no endpoint/schema, source mapping, formula or database change. One strength period carries one duration; optional movement details do not create additional energy/time rows. Strict frontend mapping preserves empty-vs-zero and load-basis labels, and uses the specified optional workout-title fallback. Owner/epoch/period/option query scopes and existing atomic owned actions remain. Actual34paired/22adjacent/static/build evidence in ux-v3-results.md. Combined Quick/Gym production Review/save is connected inF12; browser/peer/UAT pending.


### UX-BC09/10/13 — UX-F12 connected exercise confirmation
Existing V2 dated preview/create/receipt endpoints reused through the single owned submission coordinator. Complete current input and per-period fingerprints are frozen and typed creation proof gates clearing; retries preserve the original envelope. Explicit V1 reference-based compatibility retained. No backend/API/generated contract/database change or user V8 runtime action. Controlled router/transport/state tests are recorded separately from pending actual browser/Java integration, peer review and UAT.


### UX-BC07/10/11 — UX-F13 saved root presentation and component labels
Dashboard/History switch to existing V2 root pagination; root entry counts are compared to root totals, while the existing leaf ledger still owns nutrition/minutes. FoodSnapshotReader adds original saved componentDetails from already-batched food_record rows; no per-card reference reads or current-name substitution. Optional componentDetails schema keeps older client fixtures/read contracts compatible; generated SDK validates order/identity/reference linkage. No endpoint, DB schema, migration, calculation, ownership, CSRF or catalogue-content protection changes. Actual test results in ux-v3-results.md; user V8 runtime not modified.


### UX-BC07/10/13 — UX-F14 whole-root commands and scoped archived metadata
Existing owned correction-preview/PUT/DELETE/recent/read endpoints are connected to actual UI. The single recordMutation state gains an explicit V2 root variant while V1 is narrowed separately. Exact read-back proof/typed input/version, shared write exclusion, owner epoch and lost-response behavior are preserved.
ExerciseEntryReadService includes actual original option and movement DTOs from its existing joins; separate selectableForNew flags prohibit silently reusing inactive sources. No added SQL per root, schema migration or current source update. Original metadata remains private to an authorized root read. OpenAPI makes the nested fields optional for reading earlier responses and allows unconfirmed correction-preview periods, matching the already supported server behavior. An older strict V2 bundle will reject newly added response fields and must be upgraded with the backend; this is not a promise of arbitrary mixed-version V2 deployment. Original V1 routes/proofs remain intact. No fake fingerprint, MET or active catalogue object. Actual19PG/HTTP,56paired and static results in ux-v3-results.md; whole-browser evidence remains separate.


### UX-BC13 — UX-F15 memory lifecycle
No backend/API/schema/database changes. Existing transactional server outcomes and proof endpoints remain authoritative. Explicit unknown leave clears local frozen intent only; no request cancels/reverses the server transaction. Native reload restores real auth only and performs no background save or draft-ID recovery. Empty-context cleanup preserves confirmed root correction notices. Actual paired/adjacent/static evidence is in ux-v3-results.md; real browser reload/receipt verification followsQ04.


### UX-BC13 — UX-F16 advanced source convergence
No server endpoint, Java, model provider, schema or DB migration change. CandidateMatcher already returns actual matched references as suggestions without recording or selecting them. Frontend adapters continue preserving this distinction; voice exercise no longer treats reference-type IDs as product-option IDs. Accepted candidates require explicit current selection and shared V2 preview/confirm. Existing speech/image validation, candidate safety, QA scope/consent/aggregate privacy and Session/CSRF tests are retained in the full522+88 backend pass. No provider key fabricated; live model verification remains separately blocked when unconfigured.


### UX-BC07/13 — UX-Q01 actual single correction provenance
Actual browser verification found the SINGLE frontend root-to-draft adapter omitted saved selectionKind and searchMetadataRevision, unlike its dish ingredient path. Preserve the stored fields in that bounded adapter; PUT and returned-state comparison now agree without relaxing strict proof. No backend/API/generated contract or schema migration change. The server's existing explicit default and provenance persistence remain. The failed synthetic occurrence really committed and is retained for audit; strict client detection correctly froze it rather than claiming success or creating another record. New unit29 and actual-browser evidence in ux-v3-results.md.


### UX-BC10/13 — UX-Q02 optional Workout origin read shape
Actual real-stack Gym read failed the existing strict OpenAPI schema because Java's stored Entry DTO serializer emitted originWorkoutId:null. ExerciseEntryReadService now removes only the absent optional origin from its public bounded input map. Existing real origin IDs remain. Private canonical hashing/DTO serializers and all creation/mutation calculations are unchanged; no null-to-fake-ID conversion or relaxed frontend schema. Added actual PG and cookie/CSRF HTTP assertions plus strict frontend contract coverage; evidence in ux-v3-results.md. No API endpoint, schema version, migration, catalogue rewrite, authorization or original V8 runtime change. The owned V13 backend alone will be restaged after verification.

Q02 also corrects WorkoutMutationService's JDBC value mapping for weight_date_used: bind a SQL Date (or null), matching the stored JDBC type, so changing only a parent title does not falsely rewrite an available child period. Recorded_date already used SQL Date. Actual full-row/date/version/movement/receipt RED then24affected PG/HTTP tests PASS prove unchanged snapshots are untouched while genuine changed-date/minutes cases still recalculate. No weakened root versions/constraints, historical recalculation, migration or canonical hash change. New package is isolated from Maven's mutable build path. Full final regression followsR01 after all work packages.


### UX-Q03 — grouped conservation, frontend pagination/context only
No Java/API/schema/migration change. Independent read-only synthetic-owner SQL checks compare actual stored active leaves with server summaries after whole-root correction/date movement/deletion; immutable creation proof retained. Frontend now merges overlapping root pages by typed identity/highest version and preserves a compatible History filter after correction. No fabricated totals or client nutrition/MET calculation.


### UX-BC06/13 — UX-Q04 safe intent/reload verification
No Java production or database migration. A new real-PG test proves identical business content with fresh submission/root UUIDs creates another legitimate occurrence; equivalent existing concurrent same-ID/different-content/rollback/replay cases are reused (16PASS). Frontend same-root stale409/404 cannot be erased by editing fields; explicit latest replacement/discard still clears it. Test-only loopback proxy extends V2 forwarding/drop and bounded identity metadata without business response fabrication. Actual reload/GET/replay evidence in ux-v3-results.md.


### UX-BC07/10/13 — UX-Q05 compatibility and privacy closure
No Java production/API/schema migration. Existing USER-only role rule returns ADMIN403before private handlers; another USER receives404, never a private root/proof. Missing CSRF403, legacy grouped child409, V1 creation/replay/correction and no-consent QA403 remain real. New API/browser assertions use actual role semantics and unchanged ownership/SQL constraints. Frontend preservation wrapper retains true no-op identity for ignored other-owner/epoch actions; it does not change authentication, payloads or version authority. Relevant actual browser11/PG28 and paired27/adjacent22 evidence are in ux-v3-results.md; full first-round failures are explicitly retained for serialR01closure.
