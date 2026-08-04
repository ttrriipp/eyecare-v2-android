---
target: the appointment flow
total_score: 22
max_score: 40
na_heuristics: 
p0_count: 2
p1_count: 2
timestamp: 2026-08-04T02-24-35Z
slug: are-app-presentation-appointments-appointment-flow
---
Method: dual-agent (A: design review · B: detector + evidence)

Deterministic scan caveat: detect.mjs's scannable-extension list does not include .kt; it ran clean (exit 0, []) because it silently scanned zero files, not because the code passed inspection. No live visual capture was available: the only connected device was a real, lock-screen-protected physical phone.

Correction to the assessment brief: the "unstyled Roboto fallback" typography premise (real in the auth screens previously) does not hold here — Type.kt now explicitly styles all 12 Material typography roles app-wide. Discarded as a false premise, not a finding.

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Request-success fires a navigation side-effect directly in the composable body instead of LaunchedEffect — a jump-cut, not a transition |
| 2 | Match System / Real World | 3 | Identity step's occupation/home-address fields feel like a government form, not a booking |
| 3 | User Control and Freedom | 2 | Review step has no per-field edit — back-stepping only walks Review→Identity→Reason sequentially |
| 4 | Consistency and Standards | 1 | Status pill exists on the request list card, becomes plain text on the request detail screen; cancel has confirmation for appointments but not for pending requests; requests/ buttons skip the 52dp pill convention |
| 5 | Error Prevention | 2 | Picking an invalid date in RequestDateContent silently closes the dialog with no selection and no message |
| 6 | Recognition Rather Than Recall | 2 | No step/progress indicator across the 5-6 screen request wizard |
| 7 | Flexibility and Efficiency | 2 | No returning-user shortcuts; reschedule's scroll-wheel time picker takes dozens of taps to move hours |
| 8 | Aesthetic and Minimalist Design | 2 | 9 ungrouped fields in one continuous scroll in the identity step, no section headers |
| 9 | Error Recovery | 2 | "Active request limit reached" error has no link to the existing request from that screen |
| 10 | Help and Documentation | 3 | Domain mostly self-explains, but nothing explains why a booking form needs occupation/home address |
| **Total** | | **22/40** | **Acceptable** |

## Design Specificity Verdict

LLM assessment: Split-personality flow. AppointmentListScreen, AppointmentDetailScreen, RescheduleBottomSheet, and AppConfirmationDialog are genuinely authored for "Clear Care Companion." The entire requests/ package — the primary task path — is undifferentiated stock Material3: no EyecareColors imports anywhere, no card system, no status pill on the request-detail screen despite the same status rendering as a colored pill one screen earlier.

Deterministic scan: void for this Kotlin codebase (detector is scope-blind to .kt files).

Independent factual scan: three touch targets below Android's 48dp minimum (38dp date-circle selector, 34dp-tall AM/PM toggle, 40dp "View all reservations" override); a recurring off-scale secondary rhythm (18dp icons, 9-10dp spacedBy) that reads as intentional but undocumented; one decorative-ish contentDescription duplicating adjacent visible text (AppointmentDetailScreen.kt:534). No verticalScroll+weight conflicts; no raw `primary` used as text/icon color.

## Overall Impression

Confirmed-appointment surfaces (list, detail, reschedule) are well-crafted and on-brand. The booking flow that gets patients into the system is not — stock Material with real accessibility gaps, and its one emotional payoff moment (submitting a request) is a silent redirect while the secondary reschedule action gets full celebratory treatment.

## What's Working

1. AppConfirmationDialog — a well-made shared primitive, exactly the kind of considered detail the system calls for.
2. Server-authoritative action gating — AppointmentDetailScreen reads canCancel/canReschedule from the domain model rather than inferring locally.
3. AppointmentStatusGuidance/StaffRescheduleNotice proactively explain why a schedule changed rather than just showing a diff.

## Priority Issues

[P0] requests/ package ignores the design system entirely — primary task path reads as a cheaper, different product mid-flow. Fix: retrofit onto the card/pill/status-pill/accentText vocabulary already proven elsewhere. → /impeccable adapt

[P0] Systemic touch-target compliance gap — request-wizard buttons default to ~40dp with no explicit height; concrete violations also found elsewhere (38dp date circle, 34dp AM/PM toggle, 40dp override). Fix: shared 52dp primary-action convention; raise the three named targets to ≥48dp. → /impeccable harden

[P1] Identity step dumps 9 ungrouped fields with no chunking — violates ≤4-items-per-group rule at the moment trust matters most. Fix: split into Contact/Personal/Address groups; replace the literal "▾" glyph with a real Icon. → /impeccable layout

[P1] No reassurance moment at the flow's actual peak-end — request success is a silent, unguarded navigation while reschedule gets a full celebration. Fix: wrap in LaunchedEffect(step); add a themed acknowledgment. → /impeccable delight

[P2] Cancelling a pending request has no confirmation, cancelling a confirmed appointment does. Fix: reuse AppConfirmationDialog(isDestructive = true, ...). → /impeccable adapt

## Persona Red Flags

Jordan (First-Timer): identity step's unexplained occupation/home-address fields, no step-progress indicator, unfinished-looking "▾" glyph — real abandonment risk.

Sam (Accessibility-Dependent): reschedule time-picker steppers have no live-region announcing current time; "▾" glyph lacks proper semantics; ungrouped identity step has no anchors at 200% zoom.

Riley (Stress Tester): date-boundary picks silently no-op; unguarded navigation side-effect risks double-fire on rotation.

## Minor Observations

- AppointmentsCoordinator.kt is dead code (only its own test references it).
- "Asia/Manila" hardcoded independently in four files, duplicating AppointmentScheduling.kt's CLINIC_TIME_ZONE constant.
- Dark theme fully implemented but hardcoded off (val darkTheme = false) app-wide — unverified in this flow.
- ACTIVE_REQUEST_LIMIT_REACHED error has no link to the existing request — a dead end.

## Questions to Consider

- Was the request flow reviewed by the same person as the rest of the appointment surfaces, or bolted on separately?
- Is occupation/home address genuinely required to submit a request, or is the form asking for everything the clinic might ever want?
- Why does withdrawing a pending request skip the confirmation dialog proven out twice elsewhere?
