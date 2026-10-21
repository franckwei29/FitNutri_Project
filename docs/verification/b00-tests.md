# B00 developer test evidence

Branch develop/yuankang-wei. Baseline117348a; implementation commit identified by [B00].

Tests first: RecordDatePolicyTest, CurrentUserServiceTest, ApiExceptionHandlerTest written before implementation; initial compile failed because new classes did not yet exist (/tmp/fitnutri-b00-red.log). After implementation, sandbox run: 16 tests,11 PASS/5 ERROR (Mockito inline agent attachment unavailable). No assertions disabled or weakened. Re-ran with execution permission and dedicated PG16: 17 PASS/0 FAIL/0 ERROR/0 SKIP, including original context. Maven4.241s, /tmp/fitnutri-b00-all.log.

```sh
cd backend
./mvnw -o -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:55432/fitnutri_integration_baseline -Dspring.datasource.username=fitnutri_test -Dspring.datasource.password=test_only test
```

Assertions: fixed Clock across Sydney DST/local midnight and UTC; missing/default/invalid zones; dates never shifted; missing/future dates; inclusive1/366-day limits and reversed/oversize ranges. Server database identity wins over stale/spoofed authorities; null/anonymous/unauthenticated/missing/disabled accounts401; active ADMIN lacks ordinary USER record scope403. Standalone MockMvc verifies response code/path/UUID/fields/retryability,400 malformed JSON/ID, field identity without rejected values, and no SQL/secrets/stack exposure for unexpected/legacy failures. No database changes. Security-filter JSON errors belong to B02 and are not claimed covered by standalone MockMvc. Existing controllers adopt shared identity incrementally in dependent tasks. Peer review/UAT PENDING.
