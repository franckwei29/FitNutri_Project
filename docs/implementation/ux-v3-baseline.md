# UX v3 actual baseline — UX-P00

2026-10-17. Branch Gate PASS. Root: `/Users/franckwei/Desktop/5619_Project/Lab_practical_05_Group_6`.
Required/current branch: `develop/yuankang-wei`; initial HEAD `ae264208ac597f1de81220b502a8213b574dadd4` equals the archived HEAD recorded in v3. One worktree, no tracked pre-existing diff. Preserve untracked `.local/`, `.idea/` and seven nested/root `.DS_Store` entries; never stage their contents. No ZIP is needed or loaded under the direct user instruction. No remote Git operation performed.

Existing React/MUI routes, memory DraftProvider, session/CSRF, generated API, Spring services, owner locks, receipts and V1–V8 migrations will be reused. No V9 exists at this gate. Existing model/hardware/human acceptance limitations remain separate from this round.

Active specification: [v3 FINAL](FitNutri_UX_Refinement_Execution_Plan_v3.md), copied byte-for-byte from the user-provided Downloads file. v2 remains historical; conflicts are superseded by v3.

| Actual source | SHA256 at gate |
|---|---|
| `frontend/src/pages/AuthPage.tsx` | `03bcc8977ae297d7a657c2a2be6063ff1b8b43a05e1ed431d8316af8628434ed` |
| `frontend/src/app/DraftProvider.tsx` | `73c4a133d138a54622a82f4089aa4f03f7b2e54dd2a71071acb332a16d44eac5` |
| `backend/src/main/java/com/fitnutri/backend/submission/SubmissionService.java` | `6127b86efd2b69b4fabe7cb460ba1abd46de601c59ed420485289570ee2d46ff` |
| `docs/api/openapi-mvp.yaml` | `3028f9c33847ce0c7556c8fd167bc6fd58290d7084095bdf4c1670be78be7e1d` |
| `scripts/check.sh` | `6b29667cb6a25abadac0fd368155d4287f9dce987d1c2b127d25eee19ff5336d` |

Developer check: actual Git commands including worktree list and is-inside-work-tree ran successfully. Peer review PENDING; UAT PENDING. No business tests claimed by this preparation task.
