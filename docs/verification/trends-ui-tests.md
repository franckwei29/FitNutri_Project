# F15 — Canonical trends and recorded facts developer verification

Branch develop/yuankang-wei. Frontend-only package; no backend/database/schema change or new endpoint. Existing B13 summary and B04 bounded weight APIs remain authoritative. Independent review and UAT PENDING.

Changes: reuse Demo overview.tsx Recharts styling and linear, nonanimated/disconnected-null lines for intake, saved exercise estimates and measured weight. Each point projects the server's date/value with display-only kJ/kcal conversion. Weight has no carry-forward. Accessible expandable tables show exact recorded dates, unavailable estimates, missing dates and activity duration. Known totals, per-metric averages and known-day denominators come directly from the server. Partial/missing/legacy coverage stays visible; a current explicit target is labeled as a current reference, not a historical snapshot or an energy balance. Server fact text is displayed without client recalculation, generated advice or Q&A consent gating. Weekly shows both explicit equal-length ranges. Per-record source/calculation details remain in the saved-record details, not a fabricated aggregate citation.

Added paired tests: trend-series unit2, chart/statistics/facts component5, summary-date integrity contract1, bounded/unique weight-date contract1, range/query integration4 (13 new cases). Existing history tests now explicitly mock their bounded weight read. Separate component layout mocks do not claim browser layout verification. Assertions include100/null/true0, display conversion without changing canonical numbers, server50kJ/2-known-day average, partial/legacy counts, all-missing exercise values, no weight interpolation, linear connectNulls=false and visible single-point fill, server facts without consent, range changes, independent failed weight read, and list/graph/average/facts refresh after owner invalidation for a confirmed date move.

Actual commands:
- Focused unit/component/contract:15 PASS,0failed after adjusting a display-conversion assertion to appropriate floating-point tolerance; canonical server numbers remain unchanged.
- History/range integration:9 PASS,0failed (4 new+5 retained).
- Full `npm --prefix frontend test`:244 PASS in49 files,0failed; final rerun after visual correction:244 PASS. Log /tmp/fitnutri-f15-visual-fixed-full.log.
- `run typecheck:test`, `run lint`, `run build`:PASS. Logs /tmp/fitnutri-f15-visual-fixed-{types,lint,build}.log.
- OpenAPI44operations/58schemas/562valid+11negative/0failure/0skip PASS; generated58 no drift. Logs /tmp/fitnutri-f15-{contract,generated}.log.
- Build1276.21kB/383.77gzip still warns about bundle size; not suppressed. F17 performance work follows. Java code unchanged; no new Java rerun claimed (latest B14 full319+4 PASS).

Real browser evidence, existing protected local PG database:
- Logged out synthetic ADMIN; signed back in as restored synthetic USER. Dashboard works; user account remains enabled/version2 after F16.
- History selected2026-10-15..17: real list contains200g rice315kcal and12.5min walking/MISSING_WEIGHT energyUnavailable. Totals and averages315kcal/1known day of3; exercise0known days/1unavailable record; no target. Three-day data table shows16/17 Not recorded, not zero. Weight71.5kg only on measured2026-10-16, no carry-forward to15/17.
- Weekly end17 shows current11..17 vs previous04..10, previous empty/unavailable and insufficient-comparison facts rather than invented changes.
- Visual review initially caught inherited Demo white fill plus zero-width dot stroke making isolated points invisible. Corrected explicit palette fill, added paired assertions, reran244 tests and inspected updated screenshot. Browser SVG DOM independently reports both recorded points fill#58773e/r4.
- Screenshots history-canonical-trends.jpg (expanded tables, visually inspected after fix) and history-weekly-canonical-comparison.jpg (updated after fix). No data writes during trend checks. Existing source weight/record snapshots remain intact.

F15 developer check PASS; human review/UAT/shared deployment and final Q05/Q06 remain pending. F17/F18 and milestone regressions follow; M2 provider smoke remains unconfigured/not run.
