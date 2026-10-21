# P02 contract validation

2026-10-16 Australia/Sydney; develop/yuankang-wei.

Command: `/tmp/fitnutri-validation-tools/bin/python scripts/validate_contract.py`. OpenAPI3.1 validation PASS,44 unique operations,56 schemas;558 embedded/fixture examples valid,11 negative schema assertions rejected;0 failures/0 skips. A generated reset-password example initially failed its password pattern; fixed the synthetic example, retained rule and rejection tests. Validated requests include explicit null goals and false QA consent. Negative cases cover role/owner/nutrient injection, missing goal, empty batch, negative/zero/excess grams, numeric ID, excess duration and impossible date. Cross-field date, byte lengths, consent, ownership, idempotency and concurrency remain server assertions, not falsely proven by schema validation.

Contract is a target, not a declaration that44 running handlers exist. No database migrations applied. Peer review/UAT PENDING.
