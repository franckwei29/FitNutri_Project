# UX v3 task ledger

Actual execution only. TODO is the normalized NOT_STARTED state; historical v2 ledger remains intact. Independent tasks may proceed with recorded baseline data failures; affected database gates cannot pass until fixed.

| Task | Scope | Dependencies | Developer state | Commit / evidence | Peer review | UAT |
|---|---|---|---|---|---|---|
| UX-P00 | Freeze actual working tree, branch and source differences | Pass the branch gate first | IN_REVIEW | 0f2ec18 / ux-v3-baseline.md | PENDING | PENDING |
| UX-P01 | Obtain a comparable current regression baseline | UX-P00 | IN_REVIEW | 2bb00bb / ../verification/ux-v3-baseline-results.md (initial data3 errors resolved by UX-Q00) | PENDING | PENDING |
| UX-P02 | Freeze V2 contracts, models, precision and change register | UX-P01 | IN_REVIEW | 082dce2; / ../verification/ux-v3-results.md | PENDING | PENDING |
| UX-P03 | Create task ledger, source mapping and execution gates | UX-P02 | IN_REVIEW | 2493b1c; trace2PASS | PENDING | PENDING |
| UX-Q00 | Add synthetic fixtures and isolated test scaffolding early | UX-P03 | IN_REVIEW | c3900e8, e780127; e780127 + Java unit1/PG IT1/frontend2/data22 PASS | PENDING | PENDING |
| UX-D00 | Create auditable search metadata schema | UX-Q00 | IN_REVIEW | d07ce05; real V8→V9 metadata + restore/startup + full backend PASS | PENDING | PENDING |
| UX-D01 | Create dish roots and high-precision leaf records | UX-D00 | IN_REVIEW | 8cd42d9, 5be75d4; DishSchema3 + JPA/restore compatibility PASS | PENDING | PENDING |
| UX-D02 | Validate versioned submission receipts in the database | UX-D01 | IN_REVIEW | 19144b7; V1 compatibility32 + typed receipt PG3 PASS | PENDING | PENDING |
| UX-D03 | Create exercise options, Workout and movement data structures | UX-D02 | IN_REVIEW | 0fecd08; schema4/JPA + exercise/restore30+5 PASS | PENDING | PENDING |
| UX-D04 | Group constraints, preflight and safe migration tools | UX-D03 | IN_REVIEW | a910bf5; full serial Java469+22 / data28 PASS | PENDING | PENDING |
| UX-B00 | Import search metadata and Chinese/English aliases | UX-D00 | IN_REVIEW | 5fd04f6; source validator5/Java8/official PG import1 PASS | PENDING | PENDING |
| UX-B01 | Implement smart search, dynamic distinctions and complete search | UX-B00 | IN_REVIEW | 7280b3c; query/API + official PG pagination4 PASS | PENDING | PENDING |
| UX-B02 | Read recent foods/dishes and reuse context | UX-D01 | IN_REVIEW | e15521f; recent/API + PG4 PASS | PENDING | PENDING |
| UX-B03 | Implement rational portions and precision-preserving food calculations | UX-D01 | IN_REVIEW | c154adf; exact rational/rounding + adjacent18 unit PASS | PENDING | PENDING |
| UX-B04 | Implement V2 single-item/dish previews | UX-B03, UX-B01 | IN_REVIEW | 76b761f; strict API17 + PG preview/recent7 PASS | PENDING | PENDING |
| UX-B05 | Implement V2 canonical hashes, receipts and replay | UX-D02, UX-Q00 | IN_REVIEW | 367a753; V1/V2 hash/proof27 + PG9 PASS | PENDING | PENDING |
| UX-B06 | Implement atomic SINGLE and DISH creation | UX-B04, UX-B05, UX-D04 | IN_REVIEW | 02426ef; full Java501+43 / frontend371 / data33 PASS | PENDING | PENDING |
| UX-B07 | Correct and delete historical dishes as complete groups | UX-B06 | IN_REVIEW | 2f4cd4a; aggregate/legacy32 + PG17 / client7 PASS | PENDING | PENDING |
| UX-B08 | Query food roots and complete details | UX-B07, UX-B02 | IN_REVIEW | eb98094; reads/API18 + PG/HTTP20 / contract10 PASS | PENDING | PENDING |
| UX-B09 | Organize exercise options, movements and source mappings | UX-D03 | IN_REVIEW | cef8986; reviewed38/36; Java12+6/data39/static PASS | PENDING | PENDING |
| UX-B10 | Expose category, search and movement-query APIs | UX-B09 | IN_REVIEW | d86c465; API/session28 + PG6 / client28/static PASS | PENDING | PENDING |
| UX-B11 | Implement unified Quick/Workout period previews | UX-B10, UX-B05 | IN_REVIEW | 30ecd41; 21 unit/API + 8 PG; 22 client; static PASS | PENDING | PENDING |
| UX-B12 | Implement atomic Workout and Quick V2 saves | UX-B11, UX-D04 | IN_REVIEW | fe30d99; 20 unit/API/old creation + 9 PG; client12; static PASS | PENDING | PENDING |
| UX-B13 | Correct/delete historical Workouts and prevent legacy API bypass | UX-B12 | IN_REVIEW | 0056e83; 39 unit/API/old correction/session + 17 PG; client11; static PASS | PENDING | PENDING |
| UX-B14 | List exercise roots, details, recent items and reuse contexts | UX-B13 | IN_REVIEW | e4771bd; 17 unit/API + 19 PG/real HTTP; client11; static PASS | PENDING | PENDING |
| UX-B15 | Unify statistics, counts, detail reads and Q&A context | UX-B08, UX-B14 | IN_REVIEW | 0f6c805; full Java522 + PG87; frontend392; data39; static PASS | PENDING | PENDING |
| UX-F00 | Dismiss registration-success feedback within three seconds | UX-P03 | IN_REVIEW | a968ea6; AuthSuccessNotice3 + session-auth10 PASS | PENDING | PENDING |
| UX-F01 | Align onboarding and provide explicit clickable actions | UX-P03 | IN_REVIEW | 5b57334; ActionAffordance + onboarding/dashboard regression PASS | PENDING | PENDING |
| UX-F02 | Reorder Dashboard/History charts and add axes | UX-F01 | IN_REVIEW | 6be2df0, 7147c27; targeted21 PASS; browser visual PENDING | PENDING | PENDING |
| UX-F03 | Generate V2 types and strict API clients | UX-P02 | IN_REVIEW | 9680fe0; v2 contracts/clients17PASS; live endpoints pending | PENDING | PENDING |
| UX-F04 | Refactor drafts into single/dish/workout union types | UX-F03, UX-Q00 | IN_REVIEW | 6fe9a9a; full frontend78files405 + static/build PASS | PENDING | PENDING |
| UX-F05 | Smart search, more results and recent-item UI | UX-F04, UX-B01, UX-B02 | IN_REVIEW | 283a468; 25 component/contract/state + static/build PASS | PENDING | PENDING |
| UX-F06 | Continuous additions, inline edits and draft Undo | UX-F05 | IN_REVIEW | 0a9dc9c; 83files432 + legacy15; network8/pipeline13/static/build PASS | PENDING | PENDING |
| UX-F07 | Dish editing, portion modes and ingredient-based entry | UX-F06, UX-B04 | IN_REVIEW | 00fba6e; paired43 + adjacent48; static/build PASS | PENDING | PENDING |
| UX-F08 | Simplify shared Review and support inline adjustments | UX-F07 | IN_REVIEW | c4894fe; connected via F09; full frontend91files472/static/build PASS | PENDING | PENDING |
| UX-F09 | Real food saves, staying on the entry page and date rule C | UX-F08, UX-B06 | IN_REVIEW | 454029a; exact V2 proof/recovery/date C; full frontend91files472/static/build PASS | PENDING | PENDING |
| UX-F10 | Quick exercise categories, recent items and equipment input | UX-F04, UX-B10, UX-B11 | IN_REVIEW | d4baba2; F12 connected; full frontend100files517/static/build PASS | PENDING | PENDING |
| UX-F11 | Multi-period Gym, strength movements and optional sets/repetitions/load | UX-F10 | IN_REVIEW | 59e5845; F12 connected; full frontend100files517/static/build PASS | PENDING | PENDING |
| UX-F12 | Exercise Review and staying on the entry page after real saves | UX-F11, UX-B12, UX-F09 | IN_REVIEW | ea4cf12; exact proof/recovery/date C; full frontend100files517/static/build PASS | PENDING | PENDING |
| UX-F13 | Dashboard/History root cards, details and counts | UX-F02, UX-B08, UX-B14, UX-B15 | IN_REVIEW | 92fa7be; frontend101files530 + Java522/PG16/static/build PASS | PENDING | PENDING |
| UX-F14 | Grouped historical correction/deletion and Log again | UX-F13, UX-B07, UX-B13 | IN_REVIEW | 95676a2; frontend105files564 + paired56/PG19/static/build PASS | PENDING | PENDING |
| UX-F15 | Precise navigation warnings and no restoration after refresh | UX-F09, UX-F12, UX-F14 | IN_REVIEW | a4f1cf1; frontend106files580 + static/build PASS; browser reload Q04 pending | PENDING | PENDING |
| UX-F16 | Migrate image/voice candidates and regress existing advanced features | UX-F04, UX-F08, UX-F12 | IN_REVIEW | 8f4c851; frontend107files586, Java522/PG88, static/data PASS; live provider BLOCKED_EXTERNAL | PENDING | PENDING |
| UX-Q01 | Real API/browser acceptance for complete food/dish flows | UX-F09, UX-F13, UX-F14, UX-F15 | IN_REVIEW | 5783259; actual Chrome/React/Java/PG5of5, unit29, static PASS; single provenance gap fixed | PENDING | PENDING |
| UX-Q02 | Quick/Gym actual periods, details and history acceptance | UX-F12, UX-F13, UX-F14, UX-F15 | IN_REVIEW | c37c686; actual Chrome/Java/PG3of3, server24, frontend22/static PASS; read-origin and spurious child rewrite fixed | PENDING | PENDING |
| UX-Q03 | Conservation of summary totals and multi-root/multi-day regression | UX-B15, UX-F13, UX-Q01, UX-Q02 | IN_REVIEW | 125cc61; actual Chrome/Java/PG1 + frontend38 PASS | PENDING | PENDING |
| UX-Q04 | Concurrency, lost responses and recovery boundaries without refresh | UX-B06, UX-B12, UX-F15 | IN_REVIEW | f265cdf; real browser4/PG16/frontend34/proxy10 PASS | PENDING | PENDING |
| UX-Q05 | Permissions, legacy API and advanced-service compatibility regression | UX-F16, UX-B07, UX-B13 | IN_REVIEW | 45a581f; actual browser11,PG28,paired27+adjacent22 PASS | PENDING | PENDING |
| UX-Q06 | Isolated database migration and backup/restore rehearsal | UX-D04, UX-B15 | IN_REVIEW | db77b7d; migration/restore16,preflight6,oldV8-on-V13 PASS | PENDING | PENDING |
| UX-Q07 | Accessibility, visual and basic performance regression | UX-F02, UX-Q01, UX-Q02 | IN_REVIEW | 5ccd312; actual Chrome3/Safari smoke; Edge NOT_RUN | PENDING | PENDING |
| UX-R00 | Update operating instructions, contracts and user acceptance scripts | UX-Q03, UX-Q04, UX-Q05, UX-Q06, UX-Q07 | IN_REVIEW | 10eae29; trace3,origin4,static/data40/frontend600 PASS; UAT unchecked | PENDING | PENDING |
| UX-R01 | Final self-review, branch commits and delivery receipt | UX-R00 | IN_REVIEW | git subject [UX-R01]; frontend600/Java522+PG91/browser27/data40 PASS; all human sign-offs pending | PENDING | PENDING |
