# Q04 — Async frontend, strict contracts and form regression

Branch develop/yuankang-wei. Reuses paired tests written during every UI package; mocks are explicitly frontend boundary fixtures, never claimed as real database/provider evidence. The original Demo Node15 tests and byte-integrity assertion remain unchanged in their isolated archive (Q01 mapping explains v2 snapshot differences).

| Critical behavior / case | Actual automated evidence |
|---|---|
| T-AUTH-07,401/403, role/epoch | session.test.ts, session-auth.test.tsx, api-transport.test.ts: late old-user profile/record completion cannot replace new identity or refill old cache; expiry clears private cache; failed logout hides private UI and blocks account switching;401 vs403 and CSRF preparation/retry bounds distinguished. |
| T-FOOD-06 | food-reference-picker.test.tsx: reverse-order results ignored even when abort is ignored, owner/epoch/catalogue-scoped cache, page-stable selection, explicit failure/no fallback. food-editor.test.tsx: stale portions hidden after grams change, stable row/client identity, confirmations revoked and invalid/failing previews cannot review. |
| T-PROFILE-03/04/05, T-WEIGHT-02 | onboarding.test.tsx and profile-settings-weight.test.tsx: exact complete snapshot, false consent/null goals, no pre-confirmation write, unknown GET-only recovery, no duplicate initial weight, repeated-click lock, dirty fields not overwritten by refreshed weight/settings, conflict requires explicit current-version review, same physical energy on unit change, versioned dated weight. |
| Server field focus | Strengthened onboarding server-error test to require Year of birth focus after returning from final step. RED:1failed/7passed proved focus was missing. Added focusError only when mapped server field errors exist, after selecting the affected step; GREEN: onboarding8 + settings/weight9 =17passed. Typed weight/display name remain intact, no extra write. This is the only business UI change in Q04; no backend/schema/contract change. |
| T-IDEM-04/05, T-STATE-01/02/03 | submission-recovery, review-page, draft-navigation, record-mutation-recovery, record-mutation-boundary, correction-flow and reducers: frozen IDs/body, missing proof not rollback, exact retry, proof mismatch rejected, pending sync lock, owner epoch, empty Review guidance, successful ack followed by failed refresh stays saved and retries only reads. Actual network/database/browser proof is Q02, not these mocks. |
| Null, partial, HTML and opaque IDs | api-decoders/summary-api/record-api/record-mutations/submission-api: numeric large ID rejected/string9007199254740993 retained; required null vs omission/zero/nonfinite values; no raw passwordHash/unknown writable nutrients/role forwarded; HTML/malformed reads cannot look saved or empty; complete consecutive date series/selected range required. |
| Capability and owner UI | capabilities-api, entry-routes, exercise-editor, admin tests: missing verified catalogue/error disables entry, unconfigured AI remains unavailable, estimates can be explicitly missing, minimum admin DTO/versioned status, strict identity and pending recovery. No provider call is mocked as a real M2 pass. |
| F17 keyboard/dialog behavior | ui-interactions tests cover safe Cancel focus, nested modal Escape/restore, pending lock, main skip focus without router history mutation, live loading and safe route failure. Actual responsive/keyboard evidence is separately recorded in ui-regression.md. |

Final commands/results, actually executed2026-10-17:
- `npm --prefix frontend test`:257 PASS/51files/0fail (19.75s), /tmp/fitnutri-q04-full.log.
- `npm --prefix frontend test -- tests/component/onboarding.test.tsx tests/component/profile-settings-weight.test.tsx`:17 PASS/0fail after the failing focus assertion was fixed; /tmp/fitnutri-q04-field-focus-{red,green}.log retains both results.
- `npm --prefix frontend run typecheck:test` and lint PASS; /tmp/fitnutri-q04-{types,lint}.log.
- `npm --prefix frontend run test:legacy`: byte-integrity1 + original Node15 PASS/0fail/0skip; strict `typecheck:legacy` PASS; /tmp/fitnutri-q04-legacy*.log.
- `npm --prefix frontend run build`: TypeScript/build and enforced production gate PASS,105source modules/444edges/27manifest entries/29text artifacts/0violations; /tmp/fitnutri-q04-build.log.

Peer review and UAT remain PENDING. Q05/Q06 execute actual cross-page core/support journeys; Q07/Q08 and real M2 provider smoke remain distinct requirements.
