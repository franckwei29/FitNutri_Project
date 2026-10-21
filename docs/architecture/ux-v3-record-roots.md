# V3 selected root/ledger contract — UX-P02

Active requirements: ../implementation/FitNutri_UX_Refinement_Execution_Plan_v3.md §§11–16. OpenAPI is the executable target contract, not a claim endpoints are deployed.

SINGLE => one food_record; DISH => food_dish_record plus ingredient food_record leaves. QUICK => one exercise_record (root UUID equals leaf UUID); WORKOUT => workout_record plus period exercise_record leaves. Movement rows carry no duration or energy. Aggregates sum only active leaves; separate root counts never replace existing count meanings. Never infer historical groups from date/name/batch.

Use seven new tables and append-only V9 search metadata, V10 dish/quantity numeric(24,12), V11 typed receipts, V12 options/workout/movements, V13 deferred owner/date/meal/lifecycle constraints. V1–V8 unchanged. No migrations applied to user data. Only isolated rehearsal first. No precision-reducing down migration: forward fix, or restore a separately verified compatible DB/application pair with explicit approval.

IDs remain decimal strings; strict mutually exclusive kind+anyOf requests. Input grams >0 <=10000, scale3; consumed grams scale12. Fraction bounds1..1000000, reduced rational; percent <=100 scale3; servings total integer1..1000 and eaten <=total scale3. Service additionally enforces cross-field ratio/mode/UUID identity, 50 total food leaves,1440 total minutes,100 total movements, allowed option fields and owned origins. Schema alone is not those business checks.

Each ingredient nutrient is calculated directly from source per100g, original grams and rational portion, then HALF_UP scale3. Sum rounded leaves once. Preserve 1/3 vs33.3%. Period formula reuses dated weight and immutable reference; optional load/sets/speed never change MET. Explicit reviewed UNAVAILABLE differs from catalogue503.

V1 request/hash/proof semantics stay unchanged. V2 uses independent operation/hash prefix and strict typed roots/leaves; minimal proof contains no recipe/training/health payload. Check owner lock and existing receipt before current active/date/profile revalidation on replay. Parent version protects grouped mutations; legacy child writes must return GROUP_EDIT_REQUIRED. Deep freeze pending inputs in memory; no draft/pending identity persistence after reload. All new writes require current role/owner and CSRF. Capabilities default off until migration/resource/service checks pass.

Contract output snapshot input is immutable DTO data; grouped identity is kind+id, not numeric ID alone. Historical corrections preserve unchanged calculation snapshots and original creation receipts. Origin IDs record provenance only, never silently copy inputs server-side.

Developer contract checks recorded in ux-v3-results.md. Peer review PENDING; UAT PENDING.
