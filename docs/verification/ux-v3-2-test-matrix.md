# v3.2 trace matrix

| T32-01 | U/C/E | Food and Exercise show their fixed single-line notices only after valid creation receipts; one notice per batch | VERIFIED (developer; actual evidence below)
| T32-02 | C/E | Draft additions, previews, refreshes and rejected/unknown outcomes never show false success; recovering verified receipts does not write twice | VERIFIED (developer; actual evidence below)
| T32-03 | C/E/V | Notice bounds are centered in the visible viewport before/after scrolling without displacing Dashboard cards | VERIFIED (developer; actual evidence below)
| T32-04 | C | Timing begins on first actual display; still visible at 1999ms, removed at 2000ms, with no additional exit duration | VERIFIED (developer; actual evidence below)
| T32-05 | U/C | Pause when hidden and resume elapsed timing; StrictMode/rerender/remount do not reset it; logout/account changes clear it | VERIFIED (developer; actual evidence below)
| T32-06 | C/E/V | Reduced motion disables animation; polite live region, no focus stealing, overlay or click interception; no replay after refresh | VERIFIED (developer; actual evidence below)
| T32-07 | C/E/V | Desktop Date/Today/Refresh share one group and row; mobile wraps normally without horizontal overflow | VERIFIED (developer; actual evidence below)
| T32-08 | C/E | Today does not repeat work on the current day; historical/invalid dates can return to today using the established business timezone | VERIFIED (developer; actual evidence below)
| T32-09 | C/E | Refresh covers existing summary/food/exercise/trend reads; prevent duplicate loading requests and allow retry after failure | VERIFIED (developer; actual evidence below)
| T32-10 | C/E | View records carries saved date, type and typed root IDs; backdated saves do not incorrectly target today | VERIFIED (developer; actual evidence below)
| T32-11 | C/E | Dismiss closes only the saved-information bar with no writes; ending the two-second toast does not remove the bar; warnings remain independent | VERIFIED (developer; actual evidence below)
| T32-12 | C/E | A failed read after saving retains the saved fact; Retry performs GET only, clears warnings on recovery and never creates a second database record | VERIFIED (developer; actual evidence below)
| T32-13 | C/E | History retains its original trend range; Goals targets the original Profile; Data table expands/collapses without changing data | VERIFIED (developer; actual evidence below)
| T32-14 | C/E | View/more actions have the same meaning on Dashboard and History; open the correct root details even when names repeat | VERIFIED (developer; actual evidence below)
| T32-15 | C/E | More-actions keyboard/Enter/Space/Escape behavior, menu state, focus restoration and contextual names are correct | VERIFIED (developer; actual evidence below)
| T32-16 | C/E | Edit retains versions/drafts/filters and preserves input on 409; Log again retains reuse safeguards and empty-quantity rules | VERIFIED (developer; actual evidence below)
| T32-17 | C/E | Delete retains danger styling, complete consequences and confirmation; cancelling writes nothing; whole-root deletion, unknown outcomes and focus behavior do not regress | VERIFIED (developer; actual evidence below)
| T32-18 | C/E | Pending/busy action states and draft protection remain effective; asynchronous errors remain visible after closing menus | VERIFIED (developer; actual evidence below)
| T32-19 | C/E | More food/exercise remain distinguishable; prevent duplicate loading, retain prior data on failure, retry readonly and avoid duplicate pagination | VERIFIED (developer; actual evidence below)
| T32-20 | C/E | Detail Retry/Close/View date perform only their respective actions, restore filters/focus and retain authorization/404 semantics | VERIFIED (developer; actual evidence below)
| T32-21 | C/E/V | The three Add buttons retain original text, icons, appearance and entry behavior; out-of-scope Auth/Profile/Review actions are unaffected | VERIFIED (developer; actual evidence below)
| T32-22 | C/E/V | Normal/hover/focus/disabled/loading/error states, tooltips and accessible names remain complete; icons do not replace essential text | VERIFIED (developer; actual evidence below)
| T32-23 | E/V | At 360/375/768/1280/1440px and actual 200% zoom, long names and pagination coexist without clipping; notices remain single-line | VERIFIED (developer; actual evidence below)
| T32-24 | E/K | Session/CSRF/cross-user boundaries remain intact; no persistent health payloads or business writes before confirmation | VERIFIED (developer; actual evidence below)
| T32-25 | E | Singles/dishes, Quick/Workout, historical saves and receipt recovery persist through real services; root/leaf counts and duration are conserved | VERIFIED (developer; actual evidence below)
| T32-26 | K/E | Retain core v3/v3.1 regression assertions; wording changes must not hide defects by deleting tests, skipping cases or weakening selectors | VERIFIED (developer; actual evidence below)

RF32-01/02→F00; 03/04→F01; 05/06/07→F02/F03/F04; 08/09→all and Q00/Q01/Q02. Shared IntakeChart affects History as well as Dashboard. No other scope added.

## Executed evidence mapping

T32-01/02/04/05/06: Host/store, submission route components and save-feedback/recovery Chrome cases. T32-03/07/22/23: action-style-v3 five-width and genuine browser zoom cases plus record-presentation-v31 keyboard/CSS200 case. T32-08/09/13/21: dashboard components, exact Add JSX comparison, actual core and presentation flows. T32-10/11/12: SavedRecordContext components plus real historical/read-failure/no-duplicate cases. T32-14/15/16/17/18: GroupedCorrection/history components and real record-management/presentation/unknown cases. T32-19/20: history pagination and detail components retaining read-failure/version/focus assertions. T32-24/25: real legacy-regression/grouped-summary/food/dish/exercise/workout cases and Java/PG suites. T32-26: 113 files/622 frontend cases plus static/build and full 34 real Chrome cases; prior failures preserved in results, none skipped. Developer verification does not sign Peer/UAT. See final report and browser/Java JSON summaries.
