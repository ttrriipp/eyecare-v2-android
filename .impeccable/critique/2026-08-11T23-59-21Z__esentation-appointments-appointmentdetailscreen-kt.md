---
target: appointment detail page
total_score: 23
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
timestamp: 2026-08-11T23-59-21Z
slug: esentation-appointments-appointmentdetailscreen-kt
---
## Design Health Score

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of system status | 2 | Loading and reschedule success are clear, but cancellation success is inferred and action errors can land below long content. |
| 2 | Match between system and real world | 3 | Date, time, optometrist, and status are readable; “Unknown” and enum-derived reservation labels are not patient language. |
| 3 | User control and freedom | 3 | Back, dismiss, cancel, and destructive confirmation exist; cancellation has no undo or explicit completion state. |
| 4 | Consistency and standards | 2 | Material structure is coherent, but detail status styling diverges from the documented bordered chip and raw primary cyan is used on light surfaces. |
| 5 | Error prevention | 2 | Rating validation and cancel confirmation help, but rescheduling offers locally generated slots that may not be available. |
| 6 | Recognition rather than recall | 3 | Core appointment facts and actions are visible; the cancel dialog omits the appointment identity and schedule. |
| 7 | Flexibility and efficiency | 2 | Bottom actions are efficient, but the custom time stepper is tap-heavy and does not present authoritative slots. |
| 8 | Aesthetic and minimalist design | 3 | Calm spacing and restrained surfaces work; status repeats and every reservation/frame item expands inline. |
| 9 | Error recognition and recovery | 2 | Retry and inline errors exist, but raw exception text can reach patients and action failures lack a nearby next step. |
| 10 | Help and documentation | 1 | Scheduled/checked-in guidance exists; terminal, unknown, and failed-action states lack contextual recovery/help. |
| **Total** |  | **23/40** | **Acceptable; significant improvements needed.** |

## Design Specificity Verdict

The page is partially authored for Eyecare, but remains category-interchangeable in its core appointment treatment. The reserved-frames section is a distinctive product connection, and the scheduled/checked-in guidance supports the “Clear Care Companion” tone ([AppointmentDetailScreen.kt:216](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:216), [AppointmentDetailScreen.kt:287](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:287)).

Without reservations, the page is a familiar stock detail pattern: top bar, status pill, date/time card, provider, and bottom actions. It misses the stronger visit-readiness story already supported by the model: duration and reason for visit are available but not surfaced ([AppointmentV1.kt:5](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/domain/model/AppointmentV1.kt:5)).

The deterministic detector returned 0 findings and no rule locations. That scan is clean, but it is not evidence that native Compose accessibility, state communication, or runtime scheduling behavior is correct. Browser visualization was not applicable to this Kotlin/Compose target, so no overlay is available.

## Overall Impression

A calm, trustworthy skeleton with a strong appointment summary and good thumb-zone actions. The biggest opportunity is to make the page behave like a visit-readiness companion rather than a generic booking record: show the facts patients need, offer only real reschedule choices, and make cancellation/error endings explicit and reassuring.

## What’s Working

- The opening hierarchy is strong: appointment identity, status, date, and time are grouped in a scannable card ([AppointmentDetailScreen.kt:218](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:218)).
- Reschedule is the clear primary action and cancellation is visually secondary/destructive, with both anchored in the thumb zone ([AppointmentDetailScreen.kt:323](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:323)).
- The page integrates appointments with eyewear reservations instead of treating frames as unrelated commerce content, and the state model covers loading, errors, mutation progress, success, and rating flows ([AppointmentDetailViewModel.kt:20](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt:20)).

## Priority Issues

### [P1] Rescheduling presents unverified availability

**Why it matters:** Patients can spend time selecting and confirming a slot the clinic never offered, then receive a late rejection. That erodes trust in the app’s most consequential action.

The sheet generates selectable dates and 15-minute clinic-hour times locally ([RescheduleBottomSheet.kt:229](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/RescheduleBottomSheet.kt:229), [RescheduleBottomSheet.kt:280](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/RescheduleBottomSheet.kt:280)).

**Fix:** After date selection, fetch appointment-specific availability and show only server-provided slots. Include loading, no-availability, retry, and stale-slot states; disable review until an authoritative slot is selected.

**Suggested command:** $impeccable harden

### [P1] Accessibility contracts are breached across linked states

**Why it matters:** TalkBack and low-vision users may not perceive rating, status, or time-picker meaning reliably.

- Rated stars use raw Lens Cyan on a white card, despite the design system documenting that pairing as insufficient contrast ([AppointmentDetailScreen.kt:438](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:438), [DESIGN.md:168](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/DESIGN.md:168)).
- Feedback stars are 44dp, below the project’s 48dp touch-target requirement ([VisitFeedbackDialog.kt:95](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/components/VisitFeedbackDialog.kt:95), [PRODUCT.md:83](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/PRODUCT.md:83)).
- Time-stepper controls announce only “Increase”/“Decrease,” not whether they change hours or minutes ([RescheduleBottomSheet.kt:438](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/RescheduleBottomSheet.kt:438)).
- Scheduled status uses amber as small text on a light tint ([AppointmentDetailScreen.kt:860](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:860)).
- The project’s dark theme is implemented but globally forced off ([Theme.kt:115](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/ui/theme/Theme.kt:115)).

**Fix:** Use accessible foreground tokens for text/icons, make rating targets at least 48dp, label each time control by unit, expose the combined selected time semantically, and restore system theme selection.

**Suggested command:** $impeccable audit

### [P1] Cancellation and action errors lack context

**Why it matters:** A distracted patient can cancel the wrong visit or interpret a failed action as no response.

The dialog says only “This can’t be undone” and labels the safe action “Keep” ([AppointmentDetailScreen.kt:103](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:103)). Cancellation errors are inserted below variable-length content instead of near the persistent action area ([AppointmentDetailScreen.kt:310](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:310)), while the ViewModel can pass exception messages directly to the UI ([AppointmentDetailViewModel.kt:69](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt:69)).

**Fix:** Restate appointment type/date/time in the confirmation, rename the safe action “Keep appointment,” clarify verified reservation consequences, show explicit “Cancelling…” progress, and announce a plain-language success/failure message beside the action area.

**Suggested command:** $impeccable harden

### [P2] Vertical priority favors expanded reservations over visit readiness

**Why it matters:** Patients may scan a long frame list before seeing the practical facts needed to prepare for the visit.

The model includes duration and reason for visit, but the hero renders only date, time, and optional optometrist ([AppointmentV1.kt:5](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/domain/model/AppointmentV1.kt:5), [AppointmentDetailScreen.kt:262](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:262)). Meanwhile every reservation and frame item is expanded before “View all reservations” ([AppointmentDetailScreen.kt:586](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:586)).

**Fix:** Add duration and a plain-language visit reason to the summary. Limit inline reservations to one or two compact previews plus a count; keep full detail behind the existing reservation destinations.

**Suggested command:** $impeccable distill

### [P2] Terminal statuses do not explain what happens next

**Why it matters:** Cancelled, no-show, and especially unknown-status patients receive a label without reassurance or a recovery path.

Guidance exists only for SCHEDULED and CHECKED_IN; other statuses return no guidance ([AppointmentDetailScreen.kt:496](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:496)). UNKNOWN is exposed literally to patients ([AppointmentDetailScreen.kt:866](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:866)).

**Fix:** Add patient-safe terminal guidance: cancellation confirmation, no-show recovery/contact-clinic action, and “Status unavailable” with retry for unknown data. Never expose “Unknown” as if it were meaningful clinic language.

**Suggested command:** $impeccable clarify

## Cognitive Load

Moderate: 3 of 8 checklist failures, becoming high with multiple reservations.

- **Chunking fails** when all reservations and all frame items expand inline.
- **Working memory fails** in the cancellation dialog because it omits appointment identity/date/time.
- **Progressive disclosure fails** because reservations, notes, feedback, and management actions share one long surface.

The reschedule time stepper also exposes seven controls at once (hour/minute adjustments, AM/PM, and review), and four or more reservations create more than four simultaneous navigation choices.

## Emotional Journey

- **Arrival:** Calm and reassuring; the guidance panel and date/time card are easy to scan.
- **Understanding:** Confidence dips because status guidance and the status badge repeat the same fact.
- **Action:** Bottom actions are reachable and differentiated, but cancellation becomes anxiety-inducing because the dialog warns of irreversibility without restating what is being cancelled.
- **Rescheduling:** The two-step sheet is understandable, but locally generated times create false confidence.
- **Ending:** Reschedule has a specific success dialog; cancellation only changes the page state and rating submission silently closes, so those endings feel administrative.

## Persona Red Flags

### Jordan — first-time mobile patient

- “Unknown” and mechanical reservation labels provide no plain-language meaning.
- “Keep” is ambiguous; “Keep appointment” is safer.
- The page does not explain whether cancelling affects reserved frames.
- Failed actions have no visible clinic-help path.

### Sam — accessibility-dependent patient

- Five 44dp star controls miss the 48dp target.
- Raw Lens Cyan is used for rating icons on a light surface.
- Identical “Increase”/“Decrease” labels do not distinguish hour from minute.
- Action errors are not explicitly announced near the action that failed.
- The system theme preference is ignored.

### Casey — distracted mobile patient

- The fixed bottom action area is a strong thumb-zone choice.
- The cancellation dialog forces memory because it omits appointment identity.
- A long reservation list can bury the visit summary and feedback.
- Repeated time-stepper taps may still end in a server rejection.
- Cancellation completion is easy to miss without explicit success feedback.

## Minor Observations

- Status is communicated twice in immediate succession: guidance first, then the hero badge ([AppointmentDetailScreen.kt:216](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:216), [AppointmentDetailScreen.kt:250](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:250)).
- The detail badge is a tinted Surface, not the bordered detail-chip treatment documented in the design system ([DESIGN.md:270](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/DESIGN.md:270)).
- clinicNote is hard-coded to null, leaving its rendered branch unreachable ([AppointmentDetailScreen.kt:201](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:201)).
- The screen preview uses the default Hilt ViewModel and does not represent success, terminal, long-content, or accessibility states ([AppointmentDetailScreen.kt:897](/C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt:897)).

## Questions to Consider

- Is this primarily an appointment record, a visit-readiness companion, or a management console?
- If reserved frames exist, what should cancellation do to them—and where should that consequence be stated?
- What would make the end of cancellation feel as specific and reassuring as reschedule success?
- Should a checked-in patient still be offered cancellation, or should that action route to clinic contact instead?
