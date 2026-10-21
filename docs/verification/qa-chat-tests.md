# A09 stateless Q&A UI — developer evidence

Branch: `develop/yuankang-wei`. Backend/database change: none; canonical A08 endpoint reused. No migration.

The Demo chat layout/styles now render real `/api/qa/messages` results. There is no seeded greeting/answer or persisted conversation. Requests contain only the current question, an explicitly enabled nullable date range (at most31days), and the zone. Client response validation checks scope and the exact disclosed recorded range. Questions/answers remain component memory only; no profile, owner identity or conversation history is sent.

Availability and profile restriction are separate states with Profile/recording navigation. Pending questions have local keys; retries replace the matching failed turn and preserve its original request. A bounded `Retry-After` countdown blocks new/retry requests and never resends automatically. Responses render as plain React text. Provider refusal is distinct from transport failure and carries the server disclaimer/range disclosure.

Owner/epoch, current profile eligibility, request generation and AbortSignal checks suppress stale output. Account switch, session expiry, consent withdrawal and unmount abort requests and remove old content. Server403 forces a privacy reset even if cached eligibility looked valid. Stop waiting and Clear conversation invalidate late responses. Aborting a browser wait cannot undo an already dispatched provider request; these calls never save records.

## Tests and results

- `frontend/tests/component/chat-page.test.tsx`:10AAA tests for eligible/restricted/unconfigured states, single explicit send, preserved next draft, plain-text malicious markup, selected range/refusal, captured-question retry, consent withdrawal, account switch, server403 privacy reset, bounded429/manual retry, stop/clear and expired session.
- `frontend/tests/contract/qa-api.test.ts`:3tests for exact minimal request/signal/timeout, strict date/field validation and refusal/range/scope validation.
- `frontend/tests/contract/api-transport.test.ts`:20retained/new tests include actual Axios429 header parsing and no automatic replay.
- RED: missing client/retry metadata and then UI availability-transition harness failures were observed. Corrected tests wait for the current enabled DOM controls rather than detached pre-capability nodes. No business assertions were removed.
- Focused command: `npm --prefix frontend test -- tests/component/chat-page.test.tsx tests/contract/qa-api.test.ts tests/contract/api-transport.test.ts`:33PASS/0FAIL/0SKIP.

Full regression: `npm --prefix frontend test -- --maxWorkers=2`:63files/343PASS/0FAIL/0SKIP in44.80s. After the final timer disposal/composer-clear adjustment, chat10PASS reran. `typecheck:test`, `lint`(0warnings), `build` and enforced production boundary PASS:117modules/507edges/30manifest entries/32text artifacts/0violations. Controlled component/API tests are not actual browser, microphone, model accuracy or live-provider acceptance. A10 and Q08 remain separate. Peer review/UAT: PENDING.
