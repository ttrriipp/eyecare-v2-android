---
target: the order detail page
total_score: 23
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
timestamp: 2026-08-18T16-31-16Z
slug: p-presentation-eyewear-opticalorderdetailscreen-kt
---
## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | `computeOrderTracker` computes `activeStep`, but `OrderTracker` never reads it — for `READY_FOR_DISPENSING`, Prep and Ready render with identical styling, so the tracker can't show "current" vs. "already done." |
| 2 | Match System / Real World | 3 | Plain-language labels are fine, but "Balance" (detail) vs. "Balance due" (list) is an unexplained terminology drift for the identical field. |
| 3 | User Control and Freedom | 2 | Zero actions on the screen; the one interactive control, "Rate this item," is wired to a no-op lambda. |
| 4 | Consistency and Standards | 1 | Structural outlier vs. the session's own gold-standard detail layout — no status-guidance banner, no bottom action bar, and payment-status color coding present on the list card is entirely absent here. |
| 5 | Error Prevention | 3 | The null-`paymentSummary` "Payment info unavailable" fallback correctly carries forward this session's earlier fix. |
| 6 | Recognition Rather Than Recall | 3 | Rows are clearly labeled throughout; no memory burden. |
| 7 | Flexibility and Efficiency | 1 | No shortcuts, no CTA, nothing actionable on the one screen that states what's owed. |
| 8 | Aesthetic and Minimalist Design | 2 | "Order is ready" is stated three times (status pill, tracker, "Ready" timestamp row) with no new information added each repetition. |
| 9 | Error Recovery | 3 | `ErrorContent`/retry present and functional. |
| 10 | Help and Documentation | 3 | n/a-adjacent but scored: a short, single-purpose status/ledger screen doesn't need in-context documentation the way a multi-step flow would. |
| **Total** | | **23/40** | **Acceptable** |

## Design Specificity Verdict

**LLM assessment**: Reads as a generic order-status template with clinic vocabulary pasted on top, not a screen authored for a patient checking on prescription eyewear. Nothing on the screen references anything eyewear-specific (frame, lens type, prescription, pickup location) beyond the item description string pulled from the order line; the tracker (Prep/Ready/Released) and info rows (Reference/Created/Started/Ready/Released/Cancelled) are the same shape you'd use for a food-delivery or repair-ticket order. Compare `AppointmentDetailScreen.kt`'s `ReservedFramesSection`, which shows frame images, brand, and variant name — that screen feels authored for this clinic's domain; this one doesn't.

**Deterministic scan**: `detect.mjs` returned `[]`/exit 0 on all three files — "no signal," `.kt` isn't scanned. A manual pass substituted for it and — verified directly against source myself — confirmed two of the most consequential findings: (1) `OrderTracker` (`OpticalOrderDetailScreen.kt:242`) destructures only `(step, completed)` from `tracker.steps`, never touching `TrackerState.activeStep` at all, so the active step gets no distinct treatment from a completed one; (2) `NavGraph.kt:449` wires `onRateItem = { /* Rating handled within screen via dialog */ }` — a no-op lambda with a false comment, since no `Dialog`, no rating state, and no reference to `FrameRatingDialog` exists anywhere in the screen file, and `FrameRatingDialog(` has zero call sites anywhere in the entire codebase (confirmed by direct grep) despite being a fully-built, declared composable. Also confirmed: order-status color correctly routes through the shared `orderStatusColor`/`orderStatusTextColor` helpers (with an explicit comment crediting the list-screen consistency fix), but payment-status color does not — `paymentStatusColor`/`paymentStatusTextColor` are never called anywhere in this file, so "Status" and "Balance" render through the generic, colorless `DetailInfoRow`, while the list card one screen over correctly colors the same fields. Zero hardcoded hex, zero missing `contentDescription`, zero sub-48dp touch targets, zero typography-role bypasses. The order title and item-description `Text` calls have no `maxLines`/`overflow`, unlike the list card's own title, which does.

**Visual overlays**: N/A — native Android target, no browser to inject into.

## Overall Impression

The screen inherited this session's order-status color fix correctly (with a comment explicitly crediting it) but not the payment-status color fix from the very same helper file — the two calls sit right next to each other in `EyewearPresentation.kt`, and only one made it into this screen. Combined with a progress tracker that computes the "current step" data it needs and then never displays it, and an interactive button that's been dead since navigation was wired, this reads less like a screen with deliberate design gaps and more like one that stopped a step short of the polish its siblings already received this session.

## What's Working

- Explicit "Payment info unavailable" fallback correctly carries forward the fix that prevents a null `paymentSummary` from silently reading as nothing owed.
- Genuinely shares `orderStatusColor`/`orderStatusTextColor` with the list screen, with a code comment explicitly documenting the intent to avoid contradictory colors between list and detail — real cross-screen discipline, confirmed by direct read.
- Clean sealed-interface Loading/Error/Success state machine with working retry, matching the rest of the app's pattern.

## Priority Issues

**[P1] Tracker can't distinguish "current step" from "already done."**
Confirmed by direct read: `computeOrderTracker` returns `activeStep` (`EyewearPresentation.kt:32-36`), but `OrderTracker` (`OpticalOrderDetailScreen.kt:242-263`) only destructures `(step, completed)` — `activeStep` is never referenced. For a `READY_FOR_DISPENSING` order, both "Prep" and "Ready" render with identical `accentText`/`SemiBold` styling. This undermines the screen's core job — "is my order ready right now" — since two of three steps look equally "done." **Fix**: thread `activeStep` through and give it a distinct treatment (filled vs. outlined pill, or a small "current" marker) separate from merely-completed steps.

**[P1] Payment status/balance has no color coding on the detail screen.**
Confirmed by direct read: `paymentStatusColor`/`paymentStatusTextColor` (`EyewearPresentation.kt`) are called by the list card but never imported or referenced anywhere in `OpticalOrderDetailScreen.kt`. "Status" and "Balance" render through the generic, colorless `DetailInfoRow`; only the separate `isOverdue` flag gets red text. The exact fact that's bold red on the list card ("Balance due") becomes flat, easy-to-skim-past text on the one screen a patient opens specifically to check it — a regression relative to the list screen's own fix earlier this session. **Fix**: route "Status"/"Balance" through `paymentStatusColor`/`paymentStatusTextColor`, or at minimum color `balanceDue > 0` red to match the list card's treatment.

**[P2] Dead "Rate this item" control.**
Confirmed by direct read of both files: `OpticalOrderDetailScreen.kt:171` wires the button to `onRateItem(item.id)`, and `NavGraph.kt:449` passes `onRateItem = { /* Rating handled within screen via dialog */ }` — an empty lambda. No dialog exists anywhere in the screen file, `FrameRatingDialog(` has zero call sites anywhere in the codebase despite being fully built, and `OpticalOrderDetailViewModel.updateItemRating` is never called. Currently invisible since `FeatureFlags.FRAME_RATINGS_ENABLED = false`, but it's a landmine the moment that flag flips — tapping the button will do nothing. **Fix**: wire `FrameRatingDialog` into this screen (mirroring `AppointmentDetailScreen`'s `VisitFeedbackDialog` pattern), or strip the misleading comment and gate the button out until it's actually wired.

**[P2] "Ready" is stated three times with no new information each time.**
For a `READY_FOR_DISPENSING` order, the status pill, the tracker, and the reference-and-dates card's "Ready" row all restate the same fact. This is the identical duplication pattern the list card was just distilled to remove one file over. **Fix**: collapse pill + tracker into one visual unit, or drop the standalone timestamp row for the state the tracker already communicates.

**[P3] Structural outlier vs. the session's gold-standard detail layout.**
No `Card`/elevation anywhere (all sections are bare bordered `Surface`s), no status-guidance banner explaining what the current status means or what to do next (contrast `AppointmentStatusGuidance`), and no bottom action bar — contrast the floating action bar on the appointment detail screen. A patient who just learned they owe money has no button on the screen to act on it. **Fix**: add a per-status guidance banner and evaluate a "Contact clinic about balance" affordance in a bottom bar to match the established shell.

## Persona Red Flags

**Jordan (First-Timer)**: opens an order in `READY_FOR_DISPENSING` state and sees the tracker with Prep and Ready both lit the same color — can't tell from the tracker alone whether the glasses are actually ready or still one step away; must re-read the separate pill text to be sure.

**Sam (Accessibility)**: the "Balance" row carries no color cue at all, so a low-vision patient scanning by color gets no urgency signal for money owed — worse than the list card one screen over, which does color it. TalkBack on the tracker also announces only "Prep"/"Ready"/"Released" with no completed/current state conveyed.

**Riley (Stress Tester)**: taps "Rate this item" (once ratings are enabled) and nothing happens — confirmed dead binding. Also opens a zero-item order and the entire "Eyewear details" card silently vanishes with no "No items" messaging, reading as a possibly-broken screen rather than a deliberate empty state — inconsistent with the payment section's own explicit "unavailable" treatment for its null case a few lines below.

## Minor Observations

- `PaymentStatus` and `BigDecimal` are imported but never referenced by name anywhere else in the file.
- "Balance" (detail) vs. "Balance due" (list) is an unnecessary label mismatch for the identical field across two screens in the same flow.
- The zero-item order edge case has no explicit fallback, inconsistent with the payment section's own explicit "unavailable" treatment for its null case.
- Order title and item-description `Text` calls have no `maxLines`/`overflow`, unlike the list card's own title.
- `NavGraph.kt:446` assigns `route` from `back.toRoute<OpticalOrderDetail>()` but never uses it in that composable block — likely harmless (the ViewModel probably reads the nav arg via `SavedStateHandle` directly) but worth a second look.

## Questions to Consider

- If a patient's whole reason for opening this screen is "do I owe money," why does that fact get less visual weight here than on the list card they just tapped through?
- `TrackerState.activeStep` exists in the data model — was surfacing it in the UI simply missed, or was "current vs. completed" never actually designed for this tracker?
- Every other detail screen in this app ends in a bottom action bar. Is the absence of any action here (pay, contact clinic, dispute) a deliberate product decision, or is this screen just unfinished?
