---
target: the visit feedback UI
total_score: 25
max_score: 36
na_heuristics: 10
p0_count: 0
p1_count: 2
timestamp: 2026-08-19T01-48-56Z
slug: ion-appointments-components-visitfeedbackdialog-kt
---
Method: dual-agent (A: general-purpose design-review sub-agent · B: general-purpose detector/evidence sub-agent)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Submit spinner is clear; the post-submit "thanks" banner renders at the top of a scroll the user is at the bottom of. |
| 2 | Match System / Real World | 3 | Plain and understandable; no personalization to the specific visit. |
| 3 | User Control and Freedom | 2 | A refresh mid-dialog silently discards the open dialog and any typed input. |
| 4 | Consistency and Standards | 1 | Diverges from the app's own `AppConfirmationDialog` pattern (no icon badge, `TextButton` instead of `OutlinedButton` despite a comment claiming it matches) and from the sibling `FrameRatingDialog` (different chrome, different star size). |
| 5 | Error Prevention | 4 | Submit disabled until 1-5 chosen; comment capped client- and server-side. |
| 6 | Recognition Rather Than Recall | 4 | Existing rating/comment always visible on the card before reopening. |
| 7 | Flexibility and Efficiency | 3 | Adequate for a simple form; the entry CTA's non-full-width sizing slightly hurts tap efficiency. |
| 8 | Aesthetic and Minimalist Design | 2 | Redundant CTA copy, missing icon badge, oversized unrefined stars. |
| 9 | Error Recovery | 3 | Error text shown, dialog stays open with input preserved; no specific recovery guidance beyond the message. |
| 10 | Help and Documentation | n/a | Not needed for a 2-field native dialog. |
| **Total** | | **25/36** | **Acceptable (69%)** |

## Design Specificity Verdict

**Generic star-rating boilerplate bolted onto an otherwise authored app.** The surrounding screen has a clear, considered dialog language — `AppConfirmationDialog`'s 20dp surface + hairline border + 44dp tinted icon badge + equal-width pill buttons, and a full modal "Appointment Rescheduled / Got it" celebration on reschedule success. `VisitFeedbackDialog` copies the surface/border/button chrome (its own inline comment at line 135 says "matching AppConfirmationDialog style") but drops the one element that makes that pattern feel warm and intentional: the icon badge. No icon, no visit-specific reference, no personalization — just "Rate your visit" and five bare stars. The entry-point CTA duplicates itself verbatim ("Rate your visit" heading directly above a "Rate your visit" button — verified at `AppointmentDetailScreen.kt:501-511`), the kind of unedited copy a generic star-widget ships with.

**Deterministic scan**: `detect.mjs` returned exit 0/`[]` for both files — no signal, not a clean pass (`.kt` is outside its scannable scope). No browser evidence applies (native screen).

## Overall Impression

The mechanics are solid — submission is robust (disabled controls while submitting, preserved input on failure, correct edit-existing-rating flow), and color discipline is genuinely correct throughout (every star tint uses `accentText`, never raw Lens Cyan as text). But the presentation was never given the same design pass as its neighbors: the entry point is buried at the bottom of a long scroll with no promotion, the post-submit acknowledgment is rendered at the opposite end of that same scroll from where the user is looking, and a verified, real bug means any refresh while the dialog is open — including the "Retry" button surfaced on this exact screen — silently closes it and discards whatever the patient typed.

## What's Working

- **Color discipline**: every rating-related tint routes through `EyecareColors.current.accentText`, never raw `primary` as text — verified at `VisitFeedbackDialog.kt:98` and `AppointmentDetailScreen.kt:475,499`.
- **Submission robustness**: Cancel/Submit/stars/field all disable while submitting, and a failed submit preserves the user's typed rating and comment rather than clearing the form — verified `VisitFeedbackDialog.kt:96,113,124-131,142`.
- **Recall-avoidance**: the card shows the existing star rating and comment, with proper merged accessibility semantics ("3 out of 5 stars"), before the user ever reopens the dialog — `AppointmentDetailScreen.kt:462-479`.

## Priority Issues

**[P1] Refreshing the screen while the dialog is open silently destroys it and discards the patient's input.** Verified directly: `load()` (`AppointmentDetailViewModel.kt:356-380`) unconditionally constructs a brand-new `Success` state with only `appointment`/`frameReservations` set, defaulting `showRatingDialog`, `isSubmittingRating`, and `ratingError` back to `false`/`null`. `refresh()` is literally `= load()` (line 84), and the "Retry" action inside `AppointmentStatusGuidance` — rendered on this exact screen — also calls it. If either fires while `VisitFeedbackDialog` is open, the dialog vanishes mid-interaction with zero warning, and any star/comment the patient had entered is gone. This is the same bug for the reschedule sheet's state too, not just rating.
Fix: preserve dialog-related fields across `load()` (carry them forward from the prior `Success` state instead of defaulting), or guard `load()`/`refresh()` from replacing state while a dialog is open.
Suggested command: `/impeccable harden`

**[P1] The prompt to give feedback is buried, and the thank-you after submitting is likely invisible.** Verified directly: `VisitFeedbackCard` sits as the last section in a long scrollable `Column` (`AppointmentDetailScreen.kt:343-349`, near line 513), with nothing in the top-of-screen "Visit completed" status banner hinting it exists. After a successful submit, `actionMessage = "Thanks for sharing your feedback."` (`AppointmentDetailViewModel.kt:337`) renders via `AppointmentActionMessage` as the very first element of that same `Column` (`AppointmentDetailScreen.kt:233`) — while the user's scroll position is still down near the card they just interacted with. The dialog closing in place doesn't move the scroll, so the acknowledgment appears off-screen above the fold with nothing to draw the eye to it. The one interaction on this screen explicitly about the patient's own voice gets the weakest and least visible payoff on the page.
Fix: show the thank-you as a snackbar (position-independent) instead of a scroll-anchored banner, and/or scroll-to-top on successful submit.
Suggested command: `/impeccable harden`

**[P2] Dialog language diverges from the app's own established pattern.** No icon badge (contrast `AppConfirmationDialog.kt:74-86`'s 44dp tinted circle + inner icon), and the Cancel button is a `TextButton` (`VisitFeedbackDialog.kt:140`) rather than the `OutlinedButton` `AppConfirmationDialog` actually uses (`AppConfirmationDialog.kt:114`) — despite the file's own comment at line 135 claiming the row matches that style. Unlike reschedule success, there's also no acknowledgment dialog after a successful rating submission.
Fix: add a small icon badge to the dialog header, switch Cancel to `OutlinedButton` to genuinely match, and route successful submission through a lightweight `AppConfirmationDialog` "Thanks for your feedback" acknowledgment (also fixes P1's visibility problem more thoroughly than a snackbar would).
Suggested command: `/impeccable layout`

**[P2] Entry-point button doesn't follow the app's own full-width-pill rule.** Verified directly: `AppointmentDetailScreen.kt:507-512`'s `Button` has no `.fillMaxWidth()`/explicit height, wrap-contenting at Material3's default ~40dp — every sibling primary action in this same file (the Cancel button, the dialog's own Submit/Cancel row) follows the "full-width pill, 48-52dp tall" rule; this is the one exception.
Fix: add `.fillMaxWidth().height(48.dp)`.
Suggested command: `/impeccable harden`

**[P3] Grammar and cross-dialog polish.** "Selected 3 star" is missing its plural (`VisitFeedbackDialog.kt:90`); the redundant "Rate your visit" heading sits directly above a "Rate your visit" button (`AppointmentDetailScreen.kt:501-511`); and `VisitFeedbackDialog` (48dp bare `Icon` on `.clickable`, themed `Surface`) and `FrameRatingDialog` (32dp `Icon` inside a 48dp `IconButton`, stock `AlertDialog`, no border/corner-radius override) are two separately-built star-rating dialogs with visibly different craft levels in the same app.
Fix: pluralize the label, differentiate the CTA copy from the heading, and extract one shared `StarRatingRow`/dialog shell both features use.
Suggested command: `/impeccable distill`

## Persona Red Flags

**Sam (accessibility)**: the interactive star picker gives TalkBack five ungrouped nodes ("Rate 1", "Rate 2", "Selected 3 star"...) with no combined "3 of 5" summary, while the *read-only* display one composable over uses proper merged semantics ("3 out of 5 stars") — the passive view is more accessible than the actual input surface it's describing. Compounded by the "Selected 3 star" grammar bug read aloud.

**Riley (stress-tester)**: tapping "Retry" on the status-guidance banner (or triggering any refresh) while the rating dialog is open silently discards it and any typed input — verified, no warning, just gone.

**Casey (mobile-distracted)**: the rating card is the last thing in a long scroll with nothing above hinting it exists; even a patient who does rate and submit gets their "thanks" acknowledgment rendered off their current scroll position, at the top of the screen they're currently at the bottom of.

## Minor Observations

- The comment field caps at `maxLines = 4` for up to 1000 characters (`VisitFeedbackDialog.kt:107-121`) with no visible affordance that more text exists below the fold if the reflection runs long.
- The disabled Submit button gives no inline explanation ("Select a rating to continue") for why it's unresponsive at 0 stars.
- Unused imports confirmed via grep: `ButtonDefaults` (`VisitFeedbackDialog.kt:21`), `Alignment` (`:35`).
- `VisitRating.createdAt` is defined on the domain model but never displayed anywhere in the presentation layer (confirmed by grep) — not necessarily wrong, but a "rated on [date]" line would be a small, easy addition given the field already exists.
- `VisitFeedbackCard` uses `16.dp` card corners (`AppointmentDetailScreen.kt:454`) while the dialog itself and `AppConfirmationDialog` both use `20.dp` — a minor radius mismatch between the entry point and the dialog it opens.

## Questions to Consider

- If reschedule success earns a full modal "Got it" celebration, why does sharing feedback about the actual clinical experience — arguably the more personal act — get only a banner that may never be seen?
- Why does this app have two separately-implemented star-rating dialogs (`VisitFeedbackDialog` and `FrameRatingDialog`) with different chrome, different star sizes, and different accessibility semantics, instead of one shared component both use?
- Is burying "Rate your visit" at the very bottom of the scroll a deliberate low-pressure choice, or just where it happened to land — and if intentional, shouldn't the top-of-screen "Visit completed" banner at least gesture at it?
