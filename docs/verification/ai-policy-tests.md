# Q07 advanced contracts, privacy and failure regression

2026-10-17; `develop/yuankang-wei`. Business code/backend/database change for Q07:none. Earlier paired A00–A09 implementations are retained; new real HTTP log-privacy regression and actual-stack browser wrapper/evidence consolidate their boundaries. No live provider or microphone success is implied.

## Executed global gate

`FITNUTRI_PYTHON=/private/tmp/fitnutri-validation-tools/bin/python MAVEN_ARGS=-o TEST_DB_ADMIN_URL=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline TEST_DB_USERNAME=fitnutri_test TEST_DB_PASSWORD=<private-synthetic-value> TEST_DB_DOCKER_CONTAINER=fitnutri-integration-20261016 ./scripts/check.sh` exited0; log `/tmp/fitnutri-q07-final-all.log`.

- Java467Surefire +4Failsafe PASS,0fail/error/skip; XML independently summed. Maven65s. Includes the newly expanded actual restore/current-JAR probes and V8 startup gate.
- Advanced actual Spring/HTTP/PG suites:ImageRecognition6 +Transcription5 +SpeechParsing6 +QaHttp9 +QaUnavailable1 =27PASS. Controlled provider uses actual local HTTP; application sessions, CSRF, transactions and DB are real. Canonical AI endpoints never directly create records.
- Frontend63files/343PASS; legacy Node15+byte-integrity1; Python15; boundary11/proxy8; static contracts44operations/59schemas/564positive+11negative/no drift; TS/lint0warnings/build; production117modules/507edges/30manifest entries/32text artifacts/0violations.
- At this full run, pipeline/gateway11PASS. Subsequent immutable-runtime staging adds2tests; combined artifact/pipeline/gateway13PASS separately. No Java/production frontend business change followed the global run.

## Requirement coverage and its limits

| Cases | Actual automated assertions | Evidence / remaining real acceptance |
|---|---|---|
| T-IMAGE-02/03 | Actual magic/decode/MIME/8MiB/4096bounds/metadata stripping; invalid provider JSON/IDs/negative/portion data rejected; candidates pending; edited/deleted rows independently reviewed; selected verified reference controls saved nutrition, replay no duplicate | ImageValidatorTest/OpenAiFoodRecognition tests/ImageRecognitionHttpIntegrationTest; image panel/API/draft/shared-route tests; image-recognition-tests.md/image-entry-tests.md. Two real different food images through live model T-IMAGE-01:NOT RUN(A10) |
| T-VOICE-02 | Fake MediaRecorder/getUserMedia: permission denial/no device/unsupported type/late permission/cancel/native ended/oversize/timeout; all tracks/listeners/timers released; no text-only simulated VOICE success | recorder16 and voice panel12. Actual OS permission/hardware/indicator/browser-format checks:NOT RUN(A05/A10/Q08) |
| T-VOICE-03/04/05 | Edited transcript sent exactly; replacement/revision guards; nullable grams/meal/type/minutes cannot bypass review; known literal date/quantity only, future/ambiguous/multiple exercise explicit rejection; actual selected directory IDs and confirm transaction | SpeechEvidence/EntryExtraction/Matcher tests +actual parsingHTTP6; voice API/draft/routes tests; speech-extraction-tests.md/voice-entry-tests.md |
| T-VOICE-01 | Actual synthetic WebM/MP4 decoding→metadata-free WAV→controlled provider multipart; no record write before confirmation; real shared VOICE food/exercise transaction exercised | Actual human microphone/live transcription for both food and exercise:NOT RUN(A10). Tone fixtures are never described as human speech |
| T-QA-02/03 | Current consent authoritative even stale cached eligibility; before configuration/dispatch/output; real committed withdrawal while provider latch pending suppresses all answer; ordinary summary remains readable | QaContextEligibilityIntegrationTest/QaHttpIntegrationTest; controlled-provider actualHTTP. Cannot undo an earlier legitimate transmission |
| T-QA-04/05 | Owner/range-minimal whitelist; other owner's10000kJ absent from own100kJ facts; raw profile/health answer/identity/password/weights absent on wire; strict request rejects injected eligibility/facts/model/owner; conservative medical/prescription/privacy/instruction refusal; no tools/history/chat persistence | qa-foundation-tests.md/qa-answer-tests.md. Conservative text rules are not exhaustive semantic safety or model accuracy certification |
| T-AI-01/02 | Missing configuration,429 boundedRetry-After/no automatic retry, provider refusal, malformed JSON/type/negative/invented IDs/timeout before headers or stalled body/oversized response/redirect all explicit; no fixed successful fallback | AiTransportTest22 and per-adapter/API suites; qa10component/API3/transport20 targeted33PASS in A09 |
| T-AI-03 | Owner/epoch/generation/abort guards; image URLs revoked; Recorder streams released; replacement/cancel/unmount/account/consent transitions ignore stale replies; frozen reviewed writes unchanged | image/voice/chat component and draft suites. Actual advanced live media success UI remains A10/Q08 |

New `QaHttpIntegrationTest.successfulAndMalformedQaResponsesNeverLogQuestionAnswerCredentialsOrProviderBody` attaches a real log appender during actual QA HTTP calls. Controlled successful answer is returned only to the eligible response; malformed provider result is502. Structured audit entries exist, but current-question/answer/provider-body/credential/email sentinel strings do not appear in formatted logs or throwable messages, and no record/proof counts change. This test is included in QaHttp9/global467; no logging was disabled to pass it.

## Actual browsers, without model configuration

`advanced-journeys.mjs.verifyUnavailableHelpers` executed through real IAB on the production-style origin: voice/image disabled from actual capabilities, manual Continue enabled; no-consent chat restricted with Profile/Continue recording links; records/receipts unchanged. Screenshot advanced-unconfigured-manual-available.png.

A newly registered real Chrome synthetic USER completed adult/NO/consenttrue setup; actual `/chat` displayed service-unconfigured state, disabled Question/Send and no fixed response. Actual Profile withdrawal then `/chat` changed to consent restriction, retaining ordinary recording links. Consent was explicitly enabled again for layout inspection; no QA request/provider call was made and no food/exercise/proof changed. Screenshots chrome-eligible-qa-unconfigured.png and Chrome core evidence under browser-matrix.md. These are actual unavailable/restriction checks, not live answer acceptance.

`advanced-contract.spec.ts` adds a reproducible real-stack wrapper with owner-scoped SQL before/after assertions and no route.fulfill. All9Playwright cases discovered/typechecked; standalone runner execution:NOT RUN. Actual CUA journeys above are recorded separately. Peer review/UAT PENDING; A10 and the desktop/hardware remainder of Q08 remain blocked independently.
