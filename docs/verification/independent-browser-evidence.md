# Q06 actual independent-browser evidence / R01 immutable artifact repair

2026-10-17; `develop/yuankang-wei`. Real Chrome(extension browser1) and Codex IAB(browser3), separate cookie stores, actual built frontend at localhost8787 → Node gateway → Spring8080 → owned PG16 review database. This supersedes the earlier Q06 gap for independent cookies; it does not certify full Chrome/Edge/Safari compatibility/version matrix.

## Runtime failure retained and repaired

The first real Chrome PUT returned500; the UI froze its input/version and offered read-back/exact retry, without automatic repost. SQL showed no changed business/proof rows, original saved version1. Investigation showed the running JVM's JAR file descriptor now pointed to the small `.jar.original` inode after a concurrent Maven repackage of its mutable target path. The source package was not used for a second write. The completed failed request and zero active DB statements were verified before restarting.

`deploy/stage-backend.mjs` now stages verified SHA256-addressed independent JAR copies (runtime directory0700, file0400, no symlink/overwrite acceptance) under ignored `.local/runtime`. `scripts/dev.sh backend-built` starts that copy. Unit RED2 before helper, GREEN2: replacing build source changes the new artifact identity but cannot alter the original running bytes; dedup/hash/permissions and malformed/symlink paths checked. Pipeline includes these gates. After restart both old sessions expired; the unsubmitted IAB draft was explicitly cancelled/discarded. The same real correction then succeeded. Failure screenshot is retained, not omitted from evidence.

## T-EDIT-03

Both browsers independently logged in as retained synthetic USER4. Both reviewed food7/version1: Chrome260g, IAB255g. SQL before either confirmation equaled the initial snapshot (zero writes). Chrome explicitly confirmed →version2/260g/1713.4kJ. IAB confirmation →409; its original version remained displayed, Confirm disabled, Read latest exposed version2/260g. Full owner-scoped SQL snapshot equaled the expected winning change only: other records, weights, profile and immutable receipt hashes/results unchanged. IAB correction explicitly discarded. Chrome then reviewed/confirmed restoring250g/1647.5kJ →version3, using normal business APIs rather than direct SQL.

## T-ADMIN-02 / T-AUTH-06

Chrome logged out of USER and logged in to the already provisioned synthetic ADMIN2, while IAB retained its separate USER4 session. Admin search showed only target identity/role/access/version fields. Explicit disable →accountversion3. IAB reload →login, private history removed; login attempt while disabled returned Invalid email or password. All business values/receipts retained. Chrome explicit restore →accountversion4/enabledtrue. IAB login succeeded again and owned history was readable. No admin private-record endpoint/UI access was added.

Before/winner/disabled/after owner snapshots live only in ignored0600 `.local/e2e/q06-independent-*.json`. After-state equals before on all business values except food7/version3 from the two explicit corrections; account is restored enabledtrue/version4. No data/receipt was deleted, no new record/submission created.

Reusable real UI helpers executed through CUA use the same documented Playwright facade as the CI wrappers. The conflict helper accepts actual reviewed/latest versions rather than requiring a newly seeded version0. Types/lint0warnings PASS. CI9cases discovery/types only; no standalone Playwright browser runner was executed. Node artifact/pipeline/gateway13PASS. Full business regression:FE343,Java467+4PASS (Q07 full output).

Screenshots: independent-browser-version-conflict.png; independent-browser-session-revoked.png; independent-browser-account-restored.png; independent-browser-runtime-artifact-error.png. All use synthetic data. Browser version collection via chrome://version was blocked by the browser protocol policy (HTTP/HTTPS only); no workaround attempted. Actual Edge/Safari, full performance/compatibility and M2 media/live-provider acceptance remain Q08/A10. Peer review/UAT PENDING.
