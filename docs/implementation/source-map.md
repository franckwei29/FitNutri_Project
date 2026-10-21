# Source provenance — P00

Demo reference remains outside production. Exact migrations are recorded here as work lands.

## 5.2 Demo-to-target file mapping

| Demo source/symbol | Target | Retention and adaptation guidance |
|---|---|---|
| `main.tsx` createTheme | `app/theme.ts` | Preserve visual tokens; integrate with the existing QueryClientProvider without nesting two ThemeProviders |
| `styles.css` | `src/styles.css` | Preserve the main styles; check index.css/App.css conflicts and remove Vite example constraints |
| `App.tsx::Header` | `layouts/Header.tsx` | Preserve layout; use session/profile queries; await server logout |
| `App.tsx::Protected` | `layouts/ProtectedRoute.tsx` | Add loading/error/expired states; a failed request must not imply incomplete onboarding |
| `App.tsx::App` | `App.tsx + app/router.tsx` | Remove api.ready, full-database subscriptions, demoEmail and DemoTools; preserve routing and notices |
| `context.tsx::AppContextValue` | Separate Session/Draft/Notice Providers | Remove Database; separate current user, drafts and notices |
| `account.tsx::Login` | `pages/AuthPage.tsx` | Preserve login/register mode switching; remove demo-account autofill; connect auth hooks |
| `account.tsx::ProfilePage` | Onboarding/Profile and reusable ProfileForm | Reuse form presentation; initialize once; refetch must not overwrite in-progress edits |
| `entry.tsx::AddRecord` | `features/entry/AddRecordPage.tsx` | Preserve type selection and draft-overwrite confirmation; enable advanced methods by capability |
| `entry.tsx::FoodEditor` | FoodEditor/FoodItemEditor/Picker | Replace db.foods with remote search and selected-reference details; query each item independently |
| `entry.tsx::ExerciseEditor` | ExerciseEditor | Read real catalogue and estimates; remove local MET/weight lookups |
| `entry.tsx::Entry` | EntryPage and Voice/Image panels | Connect manual input first; isolate advanced processing in hooks; produce the same Draft |
| `entry.tsx::Review` | ReviewPage/useSubmitDraft | Preserve UI; freeze submission snapshots; distinguish create/edit/replay/unknown |
| `overview.tsx::SourceNote` | SourceNote | Read the real dataset; do not hardcode Demo/AUSNUT names |
| `overview.tsx::Records` | RecordList/DetailsDialog | Backend responses include names and reference details; remove whole-table db.foods.find joins |
| `overview.tsx::NutrientTiles` | NutrientTiles/EnergyValue | Presentation components; distinguish null, partial and zero |
| `overview.tsx::Summary` | RuleSummary | Read SummaryService facts; do not redefine averaging rules in the frontend |
| `overview.tsx::Dashboard` | DashboardPage | Read today's summary and both record lists; preserve layout |
| `overview.tsx::History` | HistoryPage/RecordCharts | URL-driven range; locate saved records automatically; use server-provided daily chart series |
| `extras.tsx::Admin` | AdminPage | Backend authorization, minimal account DTOs, versions and explicit action confirmation |
| `extras.tsx::Chat` | ChatPage | Preserve conversation UI; check eligibility on every backend request; do not call local answerQuestion |
| `extras.tsx::DemoTools` | Test/readonly reference, excluded from production | Do not migrate reset-data tools, simulated identity switching or simulated fault controls |
| `domain.ts` types and labels/dates/units | `domain/*` | Remove password_hash/Database; extract pure presentation and input rules |
| `domain.ts::commitDraft/estimateExercise/aggregate` | Java services and regression tests | Not authoritative production frontend logic; cases may become Java tests |
| All `store.ts` business writes | `api/* + hooks/*` | Replace with real transport, not simulated HTTP; no production import references |
| `seed.ts` | `tests/fixtures` or readonly reference | Never migrate simulated users/passwords or official-data placeholders into real runtime |
| `tests/domain.test.ts/service.test.ts` | `tests/legacy-demo` and new Java/API tests | Preserve case semantics; distinguish simulated-service and real-service tests |

Inventory Brand/Page/ErrorBox/LoadingButton/ConfirmDialog/DateField/SelectField/focusError/messageOf in `ui.tsx`. Reuse the first three presentation components directly. Delegate messageOf to the new API error parser; pass the shared today value into DateField; add pending-state and keyboard-focus constraints to ConfirmDialog.


## Archive file fingerprints

| Source | SHA-256 |
|---|---|
| `fitnutri-demo/.gitignore` | `65c9cbd55e3d4c554a2baa19230e690e4818efde7b4c5f9708056c82299b3b03` |
| `fitnutri-demo/README.md` | `3e087cffa70d9fe423a84fa6b21174f407f44b7e2cd70f92bc6bd1c65bde2b17` |
| `fitnutri-demo/REQUIREMENTS.md` | `c1496f42a9eb056a9d5d17a650a359aa4f45f177795a92111547aca5dd92a75d` |
| `fitnutri-demo/START_HERE.zh-CN.md` | `d6ed1d265960903b41c690690e7c8ebd2ef84a23eb591d022b04badd9bc06279` |
| `fitnutri-demo/index.html` | `5fe385ea0f6b68c9a66a3a031a825f2e0cd74713e316a2d5196d9a16d7cbd014` |
| `fitnutri-demo/package-lock.json` | `fb835254fc04003e8359f7d4642a6a4573008285d40328f56a29cb7aed3e295b` |
| `fitnutri-demo/package.json` | `de64938b35787b99b23f284e9124dd116ceba310f0f195ef80d96f37cc9633ed` |
| `fitnutri-demo/public/favicon.svg` | `fd7721218d13bdc282c9200607a80dd6f4a4889d903f8cae50ac1e865b0b630a` |
| `fitnutri-demo/src/App.tsx` | `501b88b1f73b58e09093964c71626c1e383772cd84c71966449bccfbfbe16622` |
| `fitnutri-demo/src/account.tsx` | `ed2784fa535bb8685f9950f3c0ca0074a09b3874ae698661fff8c543527c2cca` |
| `fitnutri-demo/src/context.tsx` | `4bd20cf867dd8b3b25fd35a58ef9418423810318f399328fc97794774b26b4f0` |
| `fitnutri-demo/src/domain.ts` | `5066cc75f7d17affc5019d3967c693a4d3f2524502eaad1fcdcaedf5d6c35a1a` |
| `fitnutri-demo/src/entry.tsx` | `50c50f551f19b9b4e1317343753363035a5d224f8bfb2655e4811c692335d0e9` |
| `fitnutri-demo/src/extras.tsx` | `2d96634e3cd18cbed825f78d7c68d91e0bebb90134418a629fbed873e9ff8ad5` |
| `fitnutri-demo/src/main.tsx` | `bf402b309fe8df8a1620e844ade00f6444c1cf7ff96c5d9b287295a6e0cbb7fb` |
| `fitnutri-demo/src/overview.tsx` | `8363d4864818a15c214513d465d6153a0851fac4fe7fccaea07bb7e199229cbe` |
| `fitnutri-demo/src/seed.ts` | `0f81fd85625e16dceaf1044566b2a8c4f437a98022005885ce3efdaae144e9a6` |
| `fitnutri-demo/src/store.ts` | `b2b838464aefa477dee8d2dd0cd8bdb91fe8fbc745e30eab4a748bf449a4db92` |
| `fitnutri-demo/src/styles.css` | `4574c18fc2aa24645008b128811acbd6c5f5edf6af61d9fbefbb865114710279` |
| `fitnutri-demo/src/ui.tsx` | `29a3757a2fd60705f64052e965ba553aad1d088261d5e412754baeee7b8dda8f` |
| `fitnutri-demo/tests/domain.test.ts` | `32747b8e83e81bef3cf49177b01fa6bf73f95f3e83f7fa9a855d10f740c01953` |
| `fitnutri-demo/tests/service.test.ts` | `82a9a6f411b4862195d346b15884daac97a5bb50ffbfe109cf5513365b6c30cf` |
| `fitnutri-demo/tsconfig.json` | `d36178a2a9e2942bcaeba71ca8b29f57d4f7207c76e5c6b4c8d7d818a331a46f` |
| `fitnutri-demo/vite.config.ts` | `4c176a81060f72365d86a18a1f399fb37a00a80fb68dc8ffe38e60c5f2fa7cf3` |

## Implemented presentation migration [F00]

- Demo main.tsx createTheme → frontend/src/app/theme.ts (palette/typography/component tokens retained).
- Demo styles.css → frontend/src/styles.css (byte-for-byte original).
- Demo ui.tsx → frontend/src/components/presentation.tsx + formHelpers.ts (DateField today explicit, dialog unique IDs).
- Existing main.tsx providers → app/providers.tsx; AppLayout uses reused Brand/Page. Existing LoginPage remains until F04, so no false claim that account.tsx already integrated.

## Final actual source reuse and authority (R03)

The original mapping above is historical P00 guidance; the following implemented paths supersede its placeholders/pending wording. Demo visual tokens/styles, form sections, cards, navigation, record/trend presentation and chat layout were retained and adapted rather than replacing React/MUI.

| Demo symbol | Actual integrated files |
|---|---|
| main createTheme / styles | frontend/src/app/theme.ts; frontend/src/styles.css (F00 base plus subsequent responsive/accessibility changes) |
| App Header/Protected/context | frontend/src/layouts/Header.tsx/ProtectedRoute.tsx/AppLayout.tsx; app/SessionProvider.tsx/DraftProvider.tsx/router.tsx |
| account Login/Profile | pages/AuthPage.tsx/OnboardingPage.tsx/ProfilePage.tsx; features/profile/ProfileSections.tsx/ProfileSettingsForm.tsx/WeightEntryForm.tsx |
| entry Add/Entry/Food/Exercise/Review | pages/AddRecordPage.tsx/EntryPage.tsx; features/entry/FoodEditor.tsx/ExerciseEditor.tsx/ReviewPage.tsx/CorrectionReviewPage.tsx; media/ImageEntryPanel.tsx/VoiceEntryPanel.tsx/Recorder.ts |
| overview Dashboard/History/Records/Nutrients/SourceNote | pages/DashboardPage.tsx/HistoryPage.tsx; components/RecordList.tsx/NutrientTiles.tsx/SourceNote.tsx; features/history/HistorySummary.tsx/PeriodTrends.tsx |
| extras Admin/Chat | pages/AdminPage.tsx/ChatPage.tsx; real api/admin.ts/qa.ts, no local answerQuestion |
| ui presentation | components/presentation.tsx and form helpers; actual date/focus/dialog/keyboard handling |
| store/seed/domain authority | Replaced by real strict api/*/Spring services/PostgreSQL. Pure display/input helpers remain domain/*. Original Demo service/domain cases archived byte-identically under tests/legacy-demo with new cross-language rule/API tests |

No production Database store, seeded identity/business dataset or fixed successful AI answer is reachable. Current build-enforced AST/manifest/dist boundary:117modules/507edges/30manifest entries/32text artifacts/0violations. The archive fingerprint section remains an immutable provenance record; later implementation descriptions do not imply changing the external Demo ZIP/reference.
