---
target: appointment screen
total_score: 23
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 4
timestamp: 2026-09-17T09-40-42Z
slug: p-presentation-appointments-myappointmentscreen-kt
---
Method: dual-agent (A: Aquinas · B: Boyle)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|---|---:|---|
| 1 | Visibility of System Status | 2/4 | Loading and error states exist, but mutations mainly communicate progress by disabling controls. |
| 2 | Match System / Real World | 3/4 | Appointment language is natural; “primary” and “alternative” are not sufficiently explained. |
| 3 | User Control and Freedom | 3/4 | Dismiss, retry, cancel, and safe exits exist, but selections can become stale after date changes. |
| 4 | Consistency and Standards | 2/4 | Pending status uses confirmation green, and some appointment patterns diverge from the documented design system. |
| 5 | Error Prevention | 2/4 | Date and cancellation rules are guarded, but hidden stale slots can remain submittable. |
| 6 | Recognition Rather Than Recall | 2/4 | Main details are visible, but the reschedule sheet omits the current time and selected alternatives. |
| 7 | Flexibility and Efficiency of Use | 2/4 | Week navigation and alternatives help, but selection behavior is rigid and not sufficiently visible. |
| 8 | Aesthetic and Minimalist Design | 3/4 | The journey is focused and grouped, though the reschedule sheet exposes several decisions at once. |
| 9 | Help Users Recognize, Diagnose, and Recover from Errors | 3/4 | Retry and patient-safe messages exist, but mutation feedback is easy to miss. |
| 10 | Help and Documentation | 1/4 | Inline guidance exists, but there is no clear explanation of fallback times or a contextual contact path. |
| **Total** |  | **23/40** | **Acceptable; significant improvements are needed around rescheduling.** |

## Design Specificity Verdict

### LLM assessment

Moderately product-specific. The one-active-journey model, clinic approval language, pending/confirmed distinction, clinic-link gating, and local clinic-time formatting are clearly authored for Eyecare. The visual layer is less distinctive: the core appointment surface leans on generic Material defaults, History is icon-only, and several states use incorrect or unassigned Material tokens.

### Deterministic scan

The bundled detector returned zero findings for the Kotlin target: 0 primary, 0 advisory, no rules, and no locations. This is not evidence that the native UI is fully clean—the detector processes the explicit `.kt` file with generic text rules and has no Kotlin AST or Compose-native semantics analysis. No false positives were identified.

The evidence pass did confirm labeled History and week controls, date-cell date/status semantics, polite live regions for some updates, theme-token usage rather than raw hex colors, and no fixed-pixel or inline font-size declarations. It also identified native concerns that require manual review: placeholder-only loading has no explicit TalkBack announcement, slot rows combine a selectable parent with a read-only nested checkbox, variable slots are eagerly rendered inside a `verticalScroll`, the reschedule text field has no explicit `imePadding`, and selection labels use `primary` text where the design system specifies `accentText`.

Browser visualization and overlay injection were not applicable because this is a native Android Compose surface, not a browser-rendered page.

## Overall Impression

The screen has a strong information model and is meaningfully simpler than an appointment list, but the reschedule sheet is functionally more complex than its presentation suggests. The single biggest opportunity is to make the consequence of a selection unmistakable: keep the current appointment visible, let the user intentionally assign primary versus alternatives, and show the exact selected times before submission.

## What’s Working

- The `None` / `PendingRequest` / `Appointment` journey model is clear and matches the product’s “one active journey” idea.
- A pending time-change request remains attached to the confirmed appointment instead of becoming a second active appointment.
- The shared picker has good foundations: a pinned action, scrolling, availability loading/error states, retry, date semantics, and a confirmation step.

## Priority Issues

### [P1] Reschedule selections can become stale or silently change role

Changing the selected date or week leaves `selectedPrimarySlotStartsAt` and alternatives in place. A slot from the previous date can remain the submitted primary, while the first slot selected on the new date becomes an alternative. The old selection is not visible while browsing the new date.

Why it matters: users can submit a time they no longer see or misunderstand which time the clinic will treat as preferred.

Fix: either clear selections on date/week changes or keep selections explicitly keyed by date. Add a persistent “Selected times” summary showing the primary and alternatives, with remove/promote actions. Do not enable review if the primary is not represented by the current valid selection set.

Suggested command: `$impeccable clarify` followed by `$impeccable layout`.

### [P1] “Pending” is styled like a confirmed state

`PendingRequestContent` uses `MaterialTheme.colorScheme.tertiary`, which is the app’s confirmation green, while its copy says the request is awaiting clinic approval.

Why it matters: the most prominent state cue contradicts the actual workflow and can make patients believe the clinic has already confirmed the visit.

Fix: use the documented pending amber token and pair it with explicit copy such as “Awaiting clinic review.” Reserve confirmation green for the confirmed appointment and successful outcomes.

Suggested command: `$impeccable colorize`.

### [P1] The picker removes the context needed to make a safe decision

The sheet receives the current appointment time but does not display it. The confirmation dialog only reports the number of alternatives, not their exact dates and times.

Why it matters: rescheduling is a consequential action. Users must remember the existing appointment and cannot verify the complete request at the final decision point.

Fix: show a compact “Current appointment” reference at the top of the sheet and an exact selected-times summary. In the confirmation dialog, list the primary time and each alternative in clinic-local time.

Suggested command: `$impeccable clarify`.

### [P1] Compact-width and TalkBack semantics are fragile

Seven equal-width date cells can fall below the 48dp touch-target minimum on compact devices. Slot rows use a selectable parent with a read-only nested checkbox, which can produce ambiguous screen-reader traversal. Selection labels also use `primary` as text on a light surface instead of the contrast-safe `accentText` token.

Why it matters: Casey may mis-tap dates, and Sam may hear duplicate or incomplete controls without understanding the current selection role.

Fix: use a horizontally scrollable date selector or fewer visible dates, merge each slot row into one semantic control with an explicit state description, announce selection/availability changes, and use `EyecareColors.current.accentText` for light-surface labels.

Suggested command: `$impeccable audit`.

### [P2] Action feedback and exits need clearer wording

The main appointment actions mostly disable during mutation without an inline progress indicator. The History destination is icon-only, and the cancellation dialog’s safe action is labeled only “Keep.”

Why it matters: users may interpret a disabled control as a failed tap, miss the history affordance, or hesitate because “Keep” does not say what will be kept.

Fix: show progress inside the submitting action, label the history action “History” where space permits, and use “Keep appointment” / “Keep request.” Add a direct clinic-contact path when same-day cancellation is unavailable if messaging is part of the approved workflow.

Suggested command: `$impeccable clarify`.

## Persona Red Flags

### Jordan — confused first-timer

- “Primary” is determined by the first selected slot rather than an explicit choice; the user has to infer the rule from helper copy.
- The current appointment disappears from the reschedule context.
- History is exposed through an icon-only control, and “Keep” in the cancellation dialog is ambiguous.

### Sam — accessibility-dependent user

- Date cells may be narrower than 48dp.
- A selectable slot row plus nested read-only checkbox may result in redundant or unclear TalkBack focus.
- Placeholder loading and some mutation changes lack explicit status announcements.
- The distinction between pending and confirmed currently relies partly on a misleading color treatment.

### Casey — distracted mobile user

- The most important final action is pinned near the thumb zone, which is good, but the sheet asks for reason text before the schedule choices and can feel long when interrupted.
- Selections can persist invisibly across date changes, making it easy to submit an unintended combination after returning to the app.
- The durable pending section is helpful after submission, but success initially relies on transient snackbar feedback.

## Minor Observations

- The empty state’s `Arrangement.Center` is inside a width-only column, so it is likely top-biased rather than vertically centered.
- The appointment card uses a 12dp surface/elevation treatment while the documented appointment pattern calls for a larger, border-led appointment card.
- Appointment and request IDs are presented without always being labeled as “Appointment number” or “Request number.”
- `Icons.Outlined.Bedtime` is used for the Afternoon section, which is a slightly odd metaphor.
- The loading skeleton does not tell a screen-reader user that the appointment is loading.
- Eager `forEach` rendering is acceptable for ordinary clinic slot counts but is less resilient than a lazy list if availability grows.

## Questions to Consider

- Should the user explicitly choose “Primary time” first, then optionally add alternatives, instead of relying on first-tap behavior?
- Can the current confirmed appointment remain visible as a fixed reference throughout rescheduling?
- Should the empty state expose a labeled “View appointment history” action instead of relying on the top-bar icon?
- Can the confirmation surface show the exact request that will be sent, rather than only saying that alternatives are included?
