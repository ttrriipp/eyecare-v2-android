---
target: RescheduleBottomSheet
total_score: 27
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 4
target_identity: "file:C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\appointments\\MyAppointmentScreen.kt"
target_fingerprint: "sha256:c5b3252357db5cbc7fb8986c3dd66107866934a30683d99a84dfbeae6c57b976"
target_path: "C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\appointments\\MyAppointmentScreen.kt"
timestamp: 2026-09-21T15-38-47Z
slug: ation-appointments-myappointmentscreen-kt-5dd32214
---
# RescheduleBottomSheet critique

## Design Health Score

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of system status | 3/4 | Date and availability state are visible, but the disabled CTA does not explain what is missing. |
| 2 | Match between system and real world | 4/4 | “Current appointment,” preferred time, clinic approval, and reason choices are natural. |
| 3 | User control and freedom | 2/4 | There is no explicit close/cancel action when the sheet opens. |
| 4 | Consistency and standards | 3/4 | Material controls are familiar, but the task order is less predictable than it should be. |
| 5 | Error prevention | 3/4 | Date limits, unavailable slots, required reasons, and confirmation are well guarded. |
| 6 | Recognition rather than recall | 3/4 | The current appointment stays visible, but available times are hidden below the opening viewport. |
| 7 | Flexibility and efficiency | 2/4 | Preset reasons help, but the user must perform a long vertical scan. |
| 8 | Aesthetic and minimalist design | 3/4 | Calm surfaces and grouping work; repeated reassurance and the pinned footer consume compact-screen space. |
| 9 | Error recovery | 2/4 | Retry states exist, but feedback is concentrated in the footer rather than beside the affected control. |
| 10 | Help and documentation | 2/4 | Inline guidance exists, but availability dots, alternatives, and the disabled state are under-explained. |
| **Total** |  | **27/40** | **Acceptable; significant improvements would make the flow more effortless.** |

## Design specificity

The sheet is moderately product-specific. The clinic-approval language, “current appointment stays confirmed” reassurance, preset reasons, and preferred/alternative-time model clearly belong to Eyecare. Visually, though, it remains a conventional Material booking sheet that could be transplanted into many appointment apps.

The deterministic detector found no issues (`[]`) for the target file, `RescheduleBottomSheet.kt`, and the appointments directory. That scan does not resolve native Compose behavior, so it missed the source-level state and touch-target risks below.

No browser overlay is available because this is a native Jetpack Compose surface. The supplied Android screenshot was manually inspected.

## Overall impression

The screen feels calm and trustworthy when it explains the existing appointment, but the first viewport asks the patient to choose a reason before showing the actual times they need to choose. The biggest opportunity is to make the decision sequence explicit: date → available time → optional alternatives → reason → review.

## What’s working

1. The current appointment card is a strong trust anchor. Patients can see the exact date/time and understand that rescheduling does not immediately cancel the existing visit.
2. Preferred versus alternative times is thoughtfully modeled. Alternatives are capped at two, ranked, and progressively revealed after a preferred time is selected.
3. The visual language is coherent with Eyecare: warm surfaces, restrained cyan selection, bordered cards, and plain clinic language.

## Priority issues

### [P1] Available times are below the fold

`RescheduleBottomSheet.kt` renders the reason picker before `RescheduleSlotSection`. In the screenshot, the opening viewport ends at the reason chips and disabled footer, before the actual time rows.

Fix: place the available slot list immediately after the selected-date caption. Move the reason step after a preferred time is chosen, or split the flow into explicit stages.

### [P1] A selected slot can become stale after changing dates

`selectedPrimarySlotStartsAt` and alternatives survive through `rememberSaveable`, while changing the week/date updates only `selectedDate`. A patient can select a Thursday slot, move to Friday, and still submit the old Thursday time.

Fix: clear the preferred and alternative selections whenever the selected date or week changes, or revalidate them against the newly loaded availability before enabling review.

### [P1] No explicit close or cancel affordance

The sheet relies on the drag handle, outside tap, or system Back. “Keep current time” appears only later in the confirmation dialog.

Fix: add a visible 48dp close action in the header or a labeled “Keep current time” action in the pinned footer.

### [P1] The disabled review action does not explain itself

“Review reschedule” is greyed out, but the patient is not told whether they are missing a time, a reason, or both. The missing time is also below the fold.

Fix: use dynamic supporting copy such as “Choose a time to continue” and then “Choose a reason to continue.” If the action is tapped prematurely, scroll/focus to the missing control instead of relying only on a disabled button.

### [P2] Compact controls and pinned-footer clearance are fragile

The reason chips use `heightIn(min = 44.dp)`, below the project’s 48dp touch-target guidance. Small green dots carry most of the visible availability cue, with no legend. The scroll area reserves a hard-coded 140dp/200dp while the footer height and error text are dynamic.

Fix: raise chips to at least 48dp, keep day targets visibly generous, pair dots with text/status cues, and calculate scroll padding from the actual footer height.

## Persona red flags

**Jordan — first-timer**

- Cannot tell why “Review reschedule” is disabled.
- Has no visible cancel action when reconsidering.
- May not understand what the green availability dots or “alternative times” mean.

**Sam — accessibility-dependent user**

- Reason chips may be under the 48dp touch-target floor.
- Availability is visually encoded mostly by tiny dots and muted cells.
- Footer-only validation gives no evidence of focus movement to the invalid or missing control.

**Casey — distracted mobile user**

- The pinned CTA is thumb-friendly, but the actual time choices require a long scroll.
- Seven compact date cells and four chips demand precise taps.
- Leaving the sheet can discard in-progress choices without an explicit draft/keep-current affordance.

## Minor observations

- The intro copy and current-time card repeat the clinic-approval reassurance.
- The selected date appears in both the day cell and the full caption; this helps accessibility but adds repetition.
- The 20dp content margin is roomier than the 16dp task-dense appointment guidance in `DESIGN.md`.
- Dates use `Locale.US`, which will need localization later.
- `skipPartiallyExpanded = true` plus `fillMaxHeight()` makes this function like a full-screen task page, but it has no task header or progress framing.

## Questions to consider

- Why ask for a reason before showing the available times?
- What would this feel like if the first viewport showed the current appointment, selected date, first available time, and one obvious next action?
- If the sheet is effectively full-screen, should it become a proper task page with a clear header and progress?
- Can the disabled footer explain its missing requirement in one sentence?
