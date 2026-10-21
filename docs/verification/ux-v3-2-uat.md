# FitNutri v3.2 manual acceptance

Approved scope: [v3.2 specification](../implementation/FitNutri_Centered_Feedback_and_Action_Style_Execution_Plan_v3_2.md). Developer verification, peer review and user acceptance are separate. All checks below await direct user acceptance.

## Runtime boundaries

The historical manual acceptance endpoint is http://localhost:15173 with Spring on 18080. Retain the acceptance database and accounts established for that environment; recheck health before handover. Automation uses separate ports 15183/18090/18092 and owned random test databases, without accessing manual acceptance data. If unavailable, verify process, artifact and database ownership before taking action; do not clear or restore data. Configuration and credentials remain in private .local files, outside Git. Refresh Vite pages to load the updated styles. New installations should follow docs/getting-started.md instead of assuming these services exist.

## User checklist

- [ ] Sign in with the original acceptance account; add food, review and confirm. Return to Dashboard with a centered green Food saved notice, large white single-line text, disappearing after two seconds.
- [ ] Save Quick/Workout entries: Exercise saved uses the same presentation, without duplicate submission.
- [ ] Date, Today and Refresh controls are compact; historical dates can return to today; refresh provides feedback and avoids duplicate activation.
- [ ] Saved date, View records and the dismiss control share the saved-information bar. Access remains after two seconds; dismissal does not delete records.
- [ ] A historical save opens the correct records for that date.
- [ ] A failed read after saving retains the fact that saving succeeded. Retry does not resubmit; the warning clears after recovery.
- [ ] History, Goals and Data table remain clear and usable; the data table can still expand.
- [ ] View, more actions, Edit, Log again and Delete have consistent styling on Dashboard/History. Cancelling deletion changes no data.
- [ ] More food/More exercise and detail Retry/Close/View date work; pagination failure retains loaded records.
- [ ] Mobile, desktop, keyboard and actual browser 200% zoom work; the three Add buttons retain their existing presentation.

Live AI/microphone services remain unconfigured or unaccepted. These style tests do not establish model acceptance. Record other-browser checks, peer review and this checklist according to actual results; do not sign on another person's behalf.
