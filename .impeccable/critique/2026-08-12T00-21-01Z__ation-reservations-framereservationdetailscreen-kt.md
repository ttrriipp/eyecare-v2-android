---
target: the reservation detail page
total_score: 26
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
timestamp: 2026-08-12T00-21-01Z
slug: ation-reservations-framereservationdetailscreen-kt
---
Method: dual-agent (A: general-purpose design-review sub-agent · B: general-purpose detector/evidence sub-agent)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Loading/cancelling states are covered, but a successful cancel just silently re-renders the pill — no confirmation moment for a destructive action. |
| 2 | Match System / Real World | 4 | Copy consistently speaks clinic language ("held," "fitting," "estimate," "nothing is charged") rather than generic e-commerce terms. |
| 3 | User Control and Freedom | 2 | `dismissCancelError()` exists on the ViewModel but is never called from the screen — once a cancel fails, the red error text has no dismiss or clear affordance. |
| 4 | Consistency and Standards | 3 | `DetailCard`/`StatusPill` match the list screen's own recipe well; minor drift from other detail screens' elevation choice, within DESIGN.md's allowed range. |
| 5 | Error Prevention | 3 | Confirmation dialog gates the destructive cancel; button disables mid-request. |
| 6 | Recognition Rather Than Recall | 2 | The progress-tracker dots and the frame-photo pager dots both encode state by color/opacity alone, with no text or accessible label pairing. |
| 7 | Flexibility and Efficiency of Use | 2 | No progressive disclosure — long description/spec content renders in full with no collapse, on an already long scroll. |
| 8 | Aesthetic and Minimalist Design | 3 | Matches DESIGN.md's palette and shape language well; at risk of bloat once description/specs run long. |
| 9 | Error Recovery | 2 | The cancel-error message surfaces the raw `error.message` fallback text with no structured recovery path or dismiss control. |
| 10 | Help and Documentation | 2 | `UNKNOWN` status has a reasonable "ask the clinic" fallback, but there's no contact/support action anywhere else on the screen. |
| **Total** | | **26/40** | **Acceptable** |

## Design Specificity Verdict

**LLM assessment**: Mostly authored for Eyecare specifically — the hold-notice copy ("Frames go back to the display after this time"), the "nothing is charged" reassurance, and the status explanations in `ReservationPresentation.kt` clearly speak to a patient checking on a real clinic hold, not a generic order tracker. But `ReservedFrameCard` (brand eyebrow, strikethrough `compareAtPrice`, a "Specifications" key/value dump, "View frame details" CTA) is close to an unmodified e-commerce product-detail-page template, and it's the single largest visual element on the screen — the frame name renders at `headlineMedium`, larger than the reservation's own `titleLarge` "Reservation #" heading. The screen reads as two authors: a clinic-hold voice on top, a retail-PDP voice underneath.

**Deterministic scan**: `detect.mjs` returned `[]`/exit 0 — this detector targets HTML/CSS/JSX and has no Kotlin/Compose rules, so that's "nothing applicable ran," not "verified clean." A mechanical grep pass found strong discipline overall: only one raw color literal in the whole file (`Color.White.copy(alpha = 0.6f)` on the inactive pager dot, line 587), no hardcoded font sizes, and every interactive element uses a semantic component (`IconButton`, `Card(onClick=...)`, `Button`/`OutlinedButton`) — zero raw `.clickable`. Accessibility semantics are real but narrow: exactly one `Modifier.semantics {}` block exists in the whole file (`DetailFactRow`, line 684, pairing label+value into one node); it is not applied to the progress tracker, the pager dots, or the status pill, which rely entirely on default merging and per-icon `contentDescription` — 6 of 8 icons/images in the file use `contentDescription = null`.

**Visual overlays**: N/A — native Android Compose target, no browser rendering available; declared up front rather than skipped silently.

## Overall Impression

This screen gets the emotionally important parts right — hold-expiry framing, reassurance copy, a genuine confirmation dialog before a destructive action — but then hands the largest share of the screen's real estate to a retail product page bolted on underneath, and leaves two real interaction dead-ends (an unreachable error-dismiss function, and status information conveyed by color alone in two separate places).

## What's Working

- **`HoldNotice`** correctly differentiates active vs. expired hold language and uses the same "Held until {date}" phrasing the list screen now uses — a genuine, deliberate thread between the two screens rather than two authors inventing separate copy.
- **`DetailFactRow`'s merged semantics** (`Modifier.semantics { contentDescription = "$label: $value" }`, line 684) and **`FrameImages`' per-page description** (`"${item.frameName} image ${page + 1}"`, line 570) show real accessibility craft exactly where it was applied — the gap is coverage, not competence.
- **The "nothing is charged" line** in `ReservationValueCard` (line 605) pre-empts a specific, domain-real anxiety (am I being billed for holding a frame?) that a generic template would never think to address.

## Priority Issues

**[P1] `dismissCancelError()` is dead code — the cancel-error message has no way to clear.**
Why it matters: the ViewModel defines `dismissCancelError()` (`FrameReservationDetailViewModel.kt:48-51`), but nothing in the screen calls it — confirmed it's the only reference to that symbol in the whole codebase. Once a cancel attempt fails, the red error text (lines 199-205) is permanent on screen until some other state change happens to clear it; there's no dismiss or retry control next to it.
Fix: add a small dismiss (or "Try again") action beside the error text wired to `viewModel::dismissCancelError`.
Suggested command: `/impeccable clarify`

**[P1] Progress-tracker and photo-pager state is color/opacity-only, with no accessible label.**
Why it matters: the progress-tracker step dots (color-filled vs. 24%-alpha) and the frame-photo pager dots (8dp solid primary vs. 6dp white-60%) are the only cue for "which step is current" and "which photo is showing." Confirmed: the file's only `Modifier.semantics {}` block is on `DetailFactRow` (line 684) — neither the progress dots nor the pager dots carry any `stateDescription`/`contentDescription`, so a screen-reader user gets no equivalent of "Step 2 of 3, current" or "Photo 2 of 3."
Fix: add `Modifier.semantics { stateDescription = "Step 2 of 3" }`-style labeling to both indicator sets.
Suggested command: `/impeccable harden`

**[P2] The "Reserved frame(s)" heading renders even when there are zero items.**
Why it matters: `reservation.items.forEach { ... }` and the value card are both correctly conditioned on content, but the "Reserved frame"/"Reserved frames" section heading above them (lines 184-189) is not — an empty-item reservation would show a heading over nothing.
Fix: gate the heading behind `reservation.items.isNotEmpty()`, matching the guard already used on `ReservationValueCard`.
Suggested command: `/impeccable harden`

**[P2] Frame description and specifications render with no length limit.**
Why it matters: `frameDescription` and `attributes` (lines 495-520) render in full with no clamp or expand/collapse control. A long HTML description or a large attributes map lets one `ReservedFrameCard` dominate the entire scroll, burying the cancel action and pushing chunking well past the ≤4-groups-per-screen guideline on an already 7-card-deep screen.
Fix: clamp long description text to ~4 lines with a "Show more" toggle; consider capping displayed spec rows similarly.
Suggested command: `/impeccable layout`

**[P3] No closure moment after a successful cancel.**
Why it matters: a successful cancel (`FrameReservationDetailViewModel.kt:61`) just swaps in a new `Success` state with an updated pill — nothing marks the moment for the patient. For a destructive, hard-to-reverse action, that's a weak peak-end close.
Fix: a brief inline confirmation or snackbar acknowledging the cancellation.
Suggested command: `/impeccable delight`

## Persona Red Flags

**Sam (Accessibility-Dependent)**: The `HorizontalPager` in `FrameImages` gives each photo its own `contentDescription`, but nothing announces *position* — swiping through 3 frame photos has no "image 2 of 3" cue beyond the visual dot row, which itself has no semantics. Same gap on the progress tracker: "reached" state is conveyed purely by dot color/alpha, so a screen-reader user checking reservation progress gets no equivalent signal at all.

**Riley (Deliberate Stress Tester)**: Confirmed live — a `PREPARED` reservation with `expiresAt == null` shows **zero** hold-timing information anywhere on the screen (`reservation.expiresAt?.let { ... }` at line 175 just silently drops the whole `HoldNotice`), which is exactly the moment a patient most wants to know "how long do I have" and gets nothing. Also confirmed: the orphaned "Reserved frame(s)" heading on a zero-item reservation, and the dead `dismissCancelError` path on a failed-then-retried cancel.

## Minor Observations

- `ReservedFrameCard`'s frame name at `headlineMedium` outranks the reservation's own `titleLarge` "Reservation #" heading — a hierarchy inversion where the sub-item visually outweighs the thing it belongs to.
- The `compareAtPrice` strikethrough (lines 476-483) is an unmodified retail-discount pattern with no clinic framing (e.g., no "list price" label) — reads as pulled in from a shop template rather than considered for this context.
- The cancel-error fallback text (`FrameReservationDetailViewModel.kt:66`) surfaces raw `error.message` when present, which can leak unfiltered exception text to the patient — same pattern flagged on the list screen's ViewModel in the prior critique.

## Questions to Consider

- Is a full product-detail card (spec sheet, strikethrough retail pricing) the right amount of information here, or should this screen defer entirely to "View frame details" and stay focused on hold status and next steps?
- Should hold expiry be pinned near the top of the scroll (or made sticky) given it's the one time-sensitive fact on an otherwise long, static page?
- `dismissCancelError` exists in the ViewModel with no caller anywhere in the codebase — was a dismiss UI planned and then dropped, and is there other dead ViewModel surface worth auditing across the reservation flow?
