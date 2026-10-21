# V3.1 baseline — UX31-P00

2026-10-18 Australia/Sydney. Branch Gate PASS on develop/yuankang-wei; base HEAD 139236d734434bef54289a27ef785e7b501f4f9c. No tracked/staged changes at entry. Existing untracked .DS_Store directories, .idea, .local and approved v3.1 specification retained. No AGENTS.md discovered in workspace parent search. Current v3 source reused; no ZIP extraction/reset or remote Git operation.

Actual baseline: `npm --prefix frontend test -- tests/unit/foodSubmissionV2State.test.ts tests/unit/exerciseSubmissionV2State.test.ts tests/component/GroupedHistory.test.tsx tests/component/draft-navigation.test.tsx` — 4 files / 24 tests PASS, 0 failures/skips. Log /tmp/fitnutri-ux31-baseline-tests.log.

Runtime manifest for previously owned V13 test stack exists privately. Docker inspection required sandbox escalation; dedicated fitnutri-integration-20261016 is running. Initial sandboxed localhost probes returned URLError (not evidence the services are stopped); runtime ownership/version/availability will be verified with permitted access before E2E. No services stopped or restarted. Separate original V8 runtime/database will not be touched.

Affected modules: CreateReview food/exercise navigation, lastSaved presentation lifecycle, AppLayout notification host, Dashboard refresh/context, EntryRootCard/details/actions, History grouped presentation, manual entry/recent controls. Existing receipt/owner/epoch/recovery/state-machine tests are retained. Public v3 schema/API/source remain unchanged.

Backend Change: none. Database Change: none. Developer baseline verified; Peer review PENDING; UAT PENDING.
