---
target: reservations list page
total_score: 23
max_score: 40
na_heuristics: 
p0_count: 1
p1_count: 2
timestamp: 2026-08-11T23-43-07Z
slug: ntation-reservations-framereservationlistscreen-kt
---
Method: dual-agent (A: general-purpose design-review sub-agent · B: general-purpose detector/evidence sub-agent)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Status pill is shown, but the one status fact that actually carries urgency — hold expiry — never surfaces on the card; refresh-on-resume also fires an extra, needless loading flash on first entry. |
| 2 | Match System / Real World | 3 | Peso formatting, "Reservation #", "View details" read naturally, but the friendlier `reservationStatusExplanation` copy that exists in code never reaches this screen. |
| 3 | User Control and Freedom | 2 | No filter/sort; there's no way to separate actionable holds from a growing pile of RELEASED/CANCELLED history. |
| 4 | Consistency and Standards | 2 | Loading state is a bare spinner instead of the skeleton pattern Home already established for the same moment; the card recipe (16dp, hairline border, no elevation) diverges from DESIGN.md's own documented 20dp/1dp-elevation "Appointment Card" spec despite this card being appointment-anchored. |
| 5 | Error Prevention | 3 | No destructive actions live on this screen, so there's little to prevent — fine by scope. |
| 6 | Recognition Rather Than Recall | 3 | Thumbnails plus labeled status pills aid scanning; undercut by the RELEASED/UNKNOWN color collision below. |
| 7 | Flexibility and Efficiency of Use | 1 | No search, sort, or filter of any kind; Appointments' Upcoming/History segmented control has no analog here. |
| 8 | Aesthetic and Minimalist Design | 3 | Clean and restrained at the code level (no hardcoded colors/sizes, disciplined token use), but an uncapped item list can make one card sprawl indefinitely. |
| 9 | Error Recovery | 3 | `ErrorContent` + retry works as a pattern, but the raw fallback `it.message ?: "Failed to load reservations"` can leak unfiltered exception text to the patient. |
| 10 | Help and Documentation | 1 | Status meaning is never explained in place; a patient has to open a reservation just to learn what "Requested" or "Tried on" means for them. |
| **Total** | | **23/40** | **Acceptable** |

This is an Operate-mode task screen, so heuristics 7 and 10 are scored, not waived — a status-tracking list is exactly where "what does this state mean and what should I do about it" help belongs.

## Design Specificity Verdict

**LLM assessment**: This is largely a generic "list of cards with a status pill" pattern wearing Eyecare's color tokens rather than a screen authored around what a patient actually needs from it. The loading state ignores the bespoke skeleton loader Home already established for this exact moment, defaulting to a bare `CircularProgressIndicator`. Where the screen does commit to something specific — the frame-thumbnail treatment shared with the detail screen, deliberate ellipsis handling on long names, the status-pill recipe — it earns its keep. But the single biggest miss is domain-specific, not stylistic: `reservation.expiresAt` — the one fact that actually matters to a patient checking on a held frame ("is my hold about to lapse?") — is read by the detail screen's `HoldNotice` component but never touched anywhere in this list screen. That gap is the difference between an authored Eyecare screen and a template that happens to render Eyecare data.

**Deterministic scan**: The bundled detector (`detect.mjs`) returned `[]` (exit 0) against the Kotlin source. This detector's ruleset targets HTML/CSS/JSX web markup and has no rules capable of parsing Compose syntax, so the empty result reflects "no applicable rules ran," not a verified-clean file — no design conclusion should be drawn from it alone. A supplementary mechanical grep scan (not aesthetic judgment, just objective counts) found genuinely good code hygiene: zero hardcoded hex colors, zero hardcoded `.sp` sizes, zero raw `.clickable` on non-semantic containers, and no sub-48dp touch targets — every interactive element is a proper `Card(onClick=...)` or `IconButton`. The one gap the scan surfaces: of 6 icon/image elements, 5 use `contentDescription = null`, and one of those is the actual frame photo (`AsyncImage`, line 291) rather than a purely decorative glyph — a real piece of content going unlabeled for screen-reader users, not just chrome being correctly silenced.

**Visual overlays**: Not applicable. This is a native Android Jetpack Compose screen, not a web/dev-server target, so no browser injection or live-server overlay was attempted — this was declared up front rather than silently skipped.

## Overall Impression

The screen is competently built and visually calm — it won't embarrass anyone — but it's solving "render a list of cards" rather than "help a patient track a frame that's being held for them." The biggest opportunity is small and specific: put hold-urgency on the card itself, since that's the actual reason this screen exists.

## What's Working

- **Frame thumbnail treatment** (`FrameThumbnail`, lines 279–304) reuses the same tonal `surfaceVariant` background and `FaceRetouchingNatural` fallback icon as the detail screen's frame imagery — a real, deliberate visual thread rather than two screens independently inventing their own placeholder.
- **Long-content handling is intentional, not accidental**: both the frame name and brand/variant line apply `maxLines = 1` + `TextOverflow.Ellipsis` (lines 225–226, 232–233), so long product names degrade gracefully instead of wrapping the card taller unpredictably.
- **Disciplined token usage at the code level**: every color and text style in this file routes through `MaterialTheme.colorScheme.*` / `EyecareColors.current.*` or `MaterialTheme.typography.*` — zero hardcoded hex or `.sp` literals — which is exactly the discipline DESIGN.md asks for and a lot of screens don't maintain this cleanly.

## Priority Issues

**[P0] Hold expiry is never shown on the list, only in the detail screen.**
Why it matters: the entire emotional stakes of this screen — "is my held frame about to be released?" — lives in `reservation.expiresAt` (confirmed in `FrameReservation.kt:17` and consumed by `HoldNotice` in `FrameReservationDetailScreen.kt:175-176,346,366`, which renders "Held until …" / "Hold ended …"). `ReservationCard` in the list screen never reads this field, so a patient scanning multiple reservations gets a status word but no urgency signal, and has to open each card individually to learn whether a hold is about to lapse.
Fix: surface a compact "Held until {date}" line (reuse the existing `formatReservationDateTime` helper) on the card when status is `PREPARED`, matching the detail screen's own language.
Suggested command: `/impeccable clarify` (or `/impeccable layout` if the card needs restructuring to fit it)

**[P1] Refresh-on-resume double-fires, causing a needless loading flash on first entry.**
Why it matters: the `DisposableEffect` at lines 71-77 calls `viewModel.refresh()` on every `ON_RESUME`, with no first-resume guard — and `init { load() }` in the ViewModel already loads on creation. The sibling `AppointmentListScreen.kt` solves this exact problem with a `hasResumedOnce` flag (confirmed at lines 107-117: refresh only fires when `hasResumedOnce.value` is already true). This screen doesn't carry that fix, so every fresh navigation into the list re-triggers a full loading state immediately after the initial load.
Fix: port the same `hasResumedOnce` guard from `AppointmentListScreen.kt`.
Suggested command: `/impeccable harden`

**[P1] Reservation items render with no cap, so a card with many frames sprawls indefinitely.**
Why it matters: `reservation.items.forEach { … }` (line 213) renders every item row unconditionally. A reservation with five or more frames produces a proportionally tall card, pushing the summary line and "View details" action far down the list and breaking the ≤4-items-per-group chunking guideline.
Fix: cap visible rows (e.g. 3) and add a "+N more" affordance, consistent with how the rest of the app handles long collections.
Suggested command: `/impeccable layout`

**[P2] RELEASED and UNKNOWN statuses are visually identical.**
Why it matters: `reservationStatusColor` (`ReservationPresentation.kt:48,50`) resolves both `RELEASED` and `UNKNOWN` to the same `MaterialTheme.colorScheme.onSurfaceVariant`, so two semantically different terminal states — "this hold correctly expired" vs. "we don't actually know the state" — read as the same neutral gray pill in a quick scan.
Fix: give `UNKNOWN` its own (likely warning-toned) color so it doesn't hide inside the "normal, resolved" visual language.
Suggested command: `/impeccable colorize`

**[P2] Missing/zero appointment data has no readable fallback.**
Why it matters: `appointment.scheduledAt.isNotBlank()` (line 203) silently drops the entire date/time row with no replacement copy when blank — contrast the established `AppointmentListScreen.kt:600` precedent, which falls back to "Time TBD" rather than omitting the row. Separately, `appointment.appointmentNumber ?: "Appointment #${appointment.id}"` (line 199) has no guard for `id == 0`, so a malformed/placeholder appointment can render the literal "Appointment #0" to a patient.
Fix: add a "Date TBD"-style fallback for blank schedules, and guard the id-based fallback the same way the appointment number is guarded.
Suggested command: `/impeccable harden`

## Persona Red Flags

**Sam (Accessibility-Dependent)**: The `Card` has no grouped/merged semantics, so TalkBack reads the reservation number, status pill, appointment row, and every single frame row and price sequentially as one long undifferentiated block — a 5-item reservation becomes a long linear read with no way to skip to "the part I care about." Separately, the actual frame photo (`AsyncImage`, line 291) uses `contentDescription = null`, which is correct for a decorative icon but wrong for a photo that's the actual subject of the row — a screen-reader user gets no indication a specific frame image exists there at all, only silence where sighted users see the product photo.

**Riley (Deliberate Stress Tester)**: Confirmed live, not hypothetical — a reservation with 5+ frame items renders every row with no cap (P1 above); a reservation with a blank `scheduledAt` silently loses its entire date/time row instead of showing a fallback (P2 above); an `appointment.id == 0` with no `appointmentNumber` renders the literal "Appointment #0"; and first navigation into the screen double-loads due to the missing resume guard (P1 above).

## Minor Observations

- Zero-price items render as "₱0.00" with no visual distinction from a genuine price — worth a quick check on whether that's ever a real backend state.
- The "N frames · total price" footer (lines 249-257) uses the same `bodySmall`/`onSurfaceVariant` weight as the item rows above it, so the card's most summary-relevant line has no more visual weight than the line items it's summarizing.
- Of 6 icon/image elements in the file, 5 use `contentDescription = null` (lines 133, 193, 268, 291, 298) — most are correctly-silenced decorative glyphs, but line 291 (the frame photo) is the one that should likely carry a real description (see Sam, above).

## Questions to Consider

- If a hold's expiry is the single most anxiety-relevant fact in this domain, why does the list card hide it behind a tap into the detail screen?
- Should RELEASED and CANCELLED reservations even share list real estate with PREPARED/REQUESTED ones, or does mixing "urgent, needs attention" and "archived, resolved" in one flat list work against the patient trying to scan it quickly?
- Is the 16dp bordered "Frame Card" recipe the right sibling to reach for here, or should an appointment-anchored, status-driven card follow DESIGN.md's own 20dp/1dp-elevation "Appointment Card" precedent instead, since this card is arguably closer to that family than to the Frames-grid family?
