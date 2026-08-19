---
target: the reservation detail page
total_score: 20
max_score: 32
na_heuristics: 7,10
p0_count: 0
p1_count: 3
timestamp: 2026-08-18T17-01-25Z
slug: ation-reservations-framereservationdetailscreen-kt
---
Method: dual-agent (A: general-purpose design-review sub-agent · B: general-purpose detector/evidence sub-agent)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Loading/deleting/removing states shown inline with spinners; solid. |
| 2 | Match System / Real World | 3 | Plain-language copy is good ("Set aside," "Requested"), undercut by leading with a raw database ID. |
| 3 | User Control and Freedom | 3 | Cancel/remove flows gated by confirm dialogs with clear dismiss paths. |
| 4 | Consistency and Standards | 1 | Diverges from the card radius, metadata-surface, elevation, and bottom-bar conventions established by three sibling detail screens; reintroduces a text-contrast bug already fixed elsewhere in this codebase. |
| 5 | Error Prevention | 3 | Destructive actions sit behind `AppConfirmationDialog`; singular/plural copy is handled. |
| 6 | Recognition Rather Than Recall | 2 | Leads with `"Reservation #47"` instead of frame identity; user must scroll to recognize what's actually reserved. |
| 7 | Flexibility and Efficiency | n/a | Single-path native Operate screen; no power-user acceleration expected. |
| 8 | Aesthetic and Minimalist Design | 3 | Uncluttered and appropriately terse. |
| 9 | Error Recovery | 2 | Errors render as a plain error-colored row + generic "Dismiss," no retry affordance for the specific failed action. |
| 10 | Help and Documentation | n/a | Not applicable to this Operate-mode screen. |
| **Total** | | **20/32** | **Acceptable (62.5%)** |

## Design Specificity Verdict

**Generic/category-interchangeable, not authored for Eyecare.** The screen uses the *palette* (`statusConfirmed`/`statusPending`/`accentText`) but skips the *system*: every structural convention the team converged on across three sibling detail screens this session (status guidance banner outside the card, 20dp hero card, `primary@8%` metadata surface, fixed bottom action bar, shared `AppointmentPrimaryButton`/`AppointmentOutlinedButton`) is absent here. It reads as competent default Compose — `TopAppBar` + `Column.verticalScroll` + stacked `Card`s + inline `OutlinedButton`s — not as a screen from the same design system as its siblings.

**Deterministic scan**: `detect.mjs` returned exit 0 / `[]` on the target file — this is **not a clean pass**. `.kt` files sit outside the detector's `SCANNABLE_EXTENSIONS`, and its rules target CSS/JSX/HTML, so this is a scan-unavailable result, not verified-clean signal. No browser evidence applies either — this is a native Android screen with no renderable web view.

Assessment B's manual code inventory did catch two concrete facts the design read alone wouldn't surface as cleanly: an unused `HorizontalDivider` import (line 33) and a completely dead `DetailFactRow` composable (lines 599–616, zero call sites anywhere in the codebase) — both folded into Minor Observations below.

## Overall Impression

Functionally solid — every ViewModel state is handled, destructive actions are confirmed, copy is considerate about singular/plural framing — but visibly the least-finished of the four detail screens in this codebase, and it contains one real, verified bug: the "your frames are being held" banner is hardcoded to render in confirmation-green regardless of whether the reservation is actually held. The single biggest opportunity is bringing this screen into line with the "gold standard" detail-screen pattern already proven three times over (Appointment Detail, Appointment Request Detail, Optical Order Detail) — which would fix the color-contrast bugs, the structural drift, and the weak hero hierarchy in one consistent pass rather than three separate patches.

## What's Working

- **Singular/plural microcopy discipline**: `"this frame"` vs. `"these frames"` (line 162), `"Reserved frame"` vs. `"Reserved frames"` (line 214), and the "removing the last item also cancels the reservation" warning (line 180) anticipate a real edge case rather than surprising the user.
- **Clean domain boundary**: `isCancellable`/`canAddItems`/`canRemoveItems` are read straight from `FrameReservation` extension properties (`FrameReservation.kt:43-52`) rather than re-derived in the UI layer — correct data→domain→presentation discipline.
- **Considerate empty-image fallback**: `FrameThumbnail` (lines 500-526) shows a themed icon in `accentText` rather than a broken-image box when a frame has no photo yet.

## Priority Issues

**[P1] StatusPill and HoldNotice use raw status hues as text/icon color, failing WCAG AA.**
Why it matters: `StatusPill` (lines 528-541) sets `color = if (isHeld) statusConfirmed else statusPending` and uses that *same* raw fill hue as the pill's **text** color (line 537); `HoldNotice`'s icon tint (line 354) does the same. `Theme.kt` documents that these raw hues measure 1.8-3.5:1 as text on their own 12%-tint background — below WCAG AA — which is exactly why this codebase already built `statusConfirmedText`/`statusPendingText` tokens, and why the sibling `OpticalOrderDetailScreen.kt` routes its pill label through a dedicated `orderStatusTextColor()` helper instead of reusing the fill color. `ReservationPresentation.kt` has no equivalent text-color helper at all — verified by reading the full file (only `reservationChipLabel`/`reservationExplanation`/formatters exist).
Fix: add `reservationStatusTextColor(isHeld)` mirroring `orderStatusTextColor`, and route the pill label and `HoldNotice` icon/text tint through it instead of the raw fill color.
Suggested command: `/impeccable harden` (or fold into a direct fix pass)

**[P1] HoldNotice renders a "confirmed"-green banner even for reservations that are only requested, not held.**
Why it matters: `HoldNotice` (lines 339-366) hardcodes `color = EyecareColors.current.statusConfirmed` for both its background and icon tint regardless of the `isHeld` parameter — only the *text* varies, via `reservationExplanation(isHeld, expiresAt)`. Verified directly: when `isHeld = false`, `reservationExplanation` (`ReservationPresentation.kt:20-21`) returns `"Request sent — the clinic will set these aside before your visit."` — so a merely-*requested* reservation shows a green, confirmation-colored box on the same screen as an amber `StatusPill` reading "Requested" one card above it. That's a same-screen color-semantics contradiction: green is reserved in DESIGN.md for explicitly successful/confirmed states, and here it fires for the not-yet-confirmed case. This is the single highest-stakes moment on the screen (does the clinic actually have my frames set aside?) and it gives a falsely reassuring signal.
Fix: gate `HoldNotice`'s color on `isHeld` the same way `StatusPill` already does — `statusConfirmed` only when held, `statusPending` otherwise (through the new text-safe token from the fix above).
Suggested command: `/impeccable harden`

**[P1] "Add frame" button renders its label/icon in raw Lens Cyan, failing AA, by bypassing the shared component built to prevent exactly this.**
Why it matters: the "Add frame" `OutlinedButton` (lines 232-240) has no `colors =` override, so its content color defaults to `MaterialTheme.colorScheme.primary` — raw Lens Cyan, which fails WCAG AA as text/icon color on a light surface (documented explicitly in DESIGN.md's Color section and in this codebase's own `AppointmentActionButton.kt` comment, which built `AppointmentOutlinedButton` specifically to avoid this failure via `accentText`). This screen doesn't use that shared component at all, so it reintroduces the bug the rest of the app already fixed.
Fix: replace the bare `OutlinedButton` with `AppointmentOutlinedButton` (also gains the established 52dp height, loading-state handling, and 48dp touch target for free — note the current button also lacks the `defaultMinSize(minHeight = 48.dp)` that the Cancel button has at line 285).
Suggested command: `/impeccable harden`

**[P2] Structural divergence from the converged "gold standard" detail-screen pattern.**
Why it matters: compared line-for-line against `OpticalOrderDetailScreen.kt` (the most recently built sibling): no status-guidance banner sits above/outside the hero card (that role is filled only by the conditional, two-state `HoldNotice`, gated on `expiresAt != null`); the hero card uses 16dp corners (`DetailCard`, line 567) instead of the established 20dp for detail-screen hero cards; there is no `primary@8%`-tinted metadata `Surface` — metadata is loose `Text` in the header `Column`; no `Card` sets an explicit `elevation` anywhere in the file (confirmed via grep); and actions ("Add frame," "Cancel reservation") sit inline in the scrolling `Column` instead of a pinned bottom action bar. Individually minor, together the screen reads like a different app section than its three siblings — this is also *why* the color-token gap above happened: the screen re-derives its own `StatusPill`/`IconBadge`/`DetailCard` primitives instead of reusing the ones already refined three times over elsewhere.
Fix: restructure per the established template — guidance banner outside the card (covering all reservation states, not just held/not-held), metadata in a tinted `Surface` inside a 20dp hero card, actions in a fixed bottom bar.
Suggested command: `/impeccable layout`

**[P3] Hero heading is an opaque database ID, not a human-legible summary.**
Why it matters: `ReservationSummaryCard` (lines 307-337) makes `"Reservation #${reservation.id}"` the titleLarge, bold hero — the single most visually dominant text on the screen — while what the patient actually reserved (the frame name(s)) doesn't appear until the "Reserved frame(s)" section further down. This inverts the sibling pattern (`AppointmentRequestDetailScreen.kt:212-221`) where a small reference label sits *above* a meaningful heading, not in its place.
Fix: promote the frame name (or "Frame reservation" + item count) to the hero position; demote "Reservation #{id}" to the small label above it, matching the sibling screens' hero convention.
Suggested command: `/impeccable clarify`

## Persona Red Flags

**Sam (accessibility-dependent)**: The `StatusPill` label (line 537) and `HoldNotice` icon (line 354) both fail WCAG AA contrast per this codebase's own documented measurements. A low-vision sighted user relying on the color/text pairing to tell "Set aside" from "Requested" gets pill text that's measurably harder to read than on the fixed sibling screens — a concrete, testable failure, not a stylistic nitpick.

**Casey (distracted mobile user)**: Glancing at the screen while multitasking, Casey reads color before text. The green `HoldNotice` banner can appear on a still-pending reservation whose own status pill above it is amber (`isHeld = false` still renders `statusConfirmed` green) — Casey walks away believing the frames are physically held when the clinic has only received the request. This is exactly the false-reassurance failure a distracted skim is most vulnerable to.

**Jordan (first-timer)**: Lands on a card whose largest, boldest text is `"Reservation #47"` before any mention of what frames were reserved or what "held" vs. "requested" even means — the plain-language explanation (`reservationExplanation`, rendered at lines 331-335) is present but demoted to small gray body text below the ID, so Jordan has to hunt for the sentence that actually explains the screen.

## Minor Observations

- `DetailFactRow` (lines 599-616) is fully defined but has zero call sites anywhere in the codebase — dead code, most likely an abandoned attempt at the metadata-row pattern that never got wired into the missing `primary@8%` surface noted in P2.
- `import androidx.compose.material3.HorizontalDivider` (line 33) is unused in the file body.
- `ReservationAppointment.status` (`FrameReservation.kt:8`) and six `FrameReservationItem` fields (`productVariantId`, `variantName`, `variantSku`, `frameCategory`, `frameDescription`, `attributes`) are never referenced by this screen — not necessarily wrong (may be intentionally out of scope for this view), but worth a quick check that nothing patient-relevant (e.g. surfacing appointment `status` next to "For your visit") was meant to be shown and got dropped.
- `totalValue` (`FrameReservation.kt:58-59`) is used by the sibling `FrameReservationListScreen.kt` but never surfaced on this detail screen — a patient with multiple items has no on-screen running total.
- `DetailCard`'s internal padding defaults to 16dp (line 564) where DESIGN.md calls for 18-20dp on "patient-facing summaries."
- The two inline error rows (lines 249-263, 265-279) are hand-built `Row` + `TextButton("Dismiss")` rather than a shared error-banner component — inconsistent with `RequestActionError`-style components used elsewhere in the app.
- The 0-item-reservation edge case has no fallback message (the "Reserved frame(s)" section is gated by `items.isNotEmpty()` at line 212 with nothing rendered if empty) — contrast `OpticalOrderDetailScreen.kt`'s explicit "No items on this order."
- The terminal "Deleted" state navigates back silently (`LaunchedEffect`, lines 86-88) with no confirmation toast/snackbar acknowledging the cancellation succeeded.

## Questions to Consider

- If `expires_at` is populated independent of `is_held` (confirmed in `docs/API_CONTRACT.md`), should "hold notice" really be one component with a color bug papering over two genuinely different states — or does a pending-request countdown deserve visually distinct treatment from a confirmed-hold countdown?
- Why does this screen re-derive its own `DetailCard`, `StatusPill`, and `IconBadge` instead of reusing the primitives already refined three times over in Appointment Detail, Appointment Request Detail, and Optical Order Detail — is there a shared `presentation/common/components` extraction that would make this exact contrast bug structurally impossible to reintroduce next time?
- If the hero of a reservation is arguably "the frames," not "the ticket number," would a compact frame-photo strip in the hero card itself read stronger than an order-number lead-in?
