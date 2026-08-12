---
target: RequestReviewContent.kt
total_score: 33
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
p2_count: 2
p3_count: 1
timestamp: 2026-08-11T23-51-11Z
slug: tion-appointments-requests-requestreviewcontent-kt
---
Method: dual-agent (A: general · B: general)

# Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | No intermediate feedback during submission beyond button spinner |
| 2 | Match System / Real World | 4 | Plain-language, clinic-domain copy throughout |
| 3 | User Control and Freedom | 3 | Edit buttons per card, but no undo/confirmation before "Send" |
| 4 | Consistency and Standards | 4 | Cards, typography, color tokens all match DESIGN.md |
| 5 | Error Prevention | 3 | Non-binding notice mitigates, but empty identity rows render blank |
| 6 | Recognition Rather Than Recall | 4 | All info visible and grouped by domain |
| 7 | Flexibility and Efficiency of Use | 2 | No collapse, no shortcuts; equal density for first-timers and repeat users |
| 8 | Aesthetic and Minimalist Design | 3 | Good restraint overall; identity card competes with appointment card |
| 9 | Error Recovery | 4 | Excellent: preserves data, distinguishes retry paths, calm copy |
| 10 | Help and Documentation | 3 | Non-binding notice is contextual help; missing "what happens next" |
| **Total** | | **33/40** | **Good** |

# Design Specificity Verdict

**Grounded.** This is not a category-interchangeable review screen. The non-binding notice restated above the CTA, the backup-slot rank pills, the primaryContainer ticket surface elevating the one fact the patient cares about most — these are product-specific choices for *this patient, this moment*. The comment at line 95–96 ("The time is the single fact the patient is here to confirm, so it gets the largest role") is evidence of a designer thinking about the specific user, not filling a template.

One gap: the NonBindingNotice composable (line 432) exists with an Info icon and primaryContainer background but is never rendered in the review content itself. The notice only appears as bodySmall text in the bottom bar — too late to frame the entire review with the right emotional lens.

**Deterministic scan:** CLI detector returned 0 findings on both RequestReviewContent.kt and RequestStepScaffold.kt. Manual audit found 3 low-severity spacing issues and one informational accessibility note.

# Overall Impression

A strong review screen with careful accessibility and product-specific hierarchy. The appointment ticket surface is the right design decision. The identity card is the weak link — it dumps 7 flat rows that deflate the emotional peak. The non-binding notice is the most important emotional intervention in the wizard and it's buried below the fold.

# What's Working

1. **The appointment ticket surface is excellent.** Elevating date/time into a tinted container with typographic hierarchy (labelSmall → bodyMedium → titleMedium SemiBold) is exactly the kind of product-specific hierarchy that separates good review screens from great ones. The nested 12dp radius inside the 16dp card is correctly stepped.

2. **Accessibility is not an afterthought.** mergeDescendants with explicit contentDescription on every semantic row, clearAndSetSemantics on the Edit buttons to distinguish three identical-looking actions, liveRegion = Assertive on the error message. This is careful, intentional work.

3. **The error state is genuinely patient-preserving.** "Nothing has been sent yet, and your answers are saved" addresses the exact fear a patient has when something fails. The canRetry branching prevents retry loops.

# Priority Issues

## [P1] Non-binding notice is buried below the fold

**What:** The reassurance text ("The clinic reviews every request. No time is held until they confirm it.") appears only as bodySmall in the bottom bar (line 69). The NonBindingNotice composable with its Info icon and primaryContainer background exists in the file but is unused.

**Why:** This is the highest-stakes moment in the wizard. The patient is about to send personal health information. The non-binding framing is the single most important emotional intervention — it tells the patient "this is low-commitment." Burying it in small text below the CTA means many patients will never read it. The peak-end rule says the emotional frame around the final action disproportionately shapes memory.

**Fix:** Render NonBindingNotice as the first element inside the scrollable content, above the appointment card. Keep the bottom bar version as reinforcement.

**Suggested command:** /impeccable layout

## [P1] Identity card has no empty-state handling

**What:** Lines 226–233 render ReviewRow for every field regardless of value. If identity.phone is null, the patient sees "Phone" with an empty right column. If identity.gender is null, .label.orEmpty() shows blank.

**Why:** Empty rows look like the patient forgot to fill something or the app lost data. On a review screen where the patient's job is to verify, ambiguous empty states erode trust.

**Fix:** Skip rows with null/blank values (as already done for email on line 228), or show a muted "Not provided" label.

**Suggested command:** /impeccable harden

## [P2] Identity card competes with appointment card

**What:** The identity card renders 7 rows at the same visual weight as the appointment card's 2 data points. Both cards use identical ReviewCard chrome.

**Why:** Equal visual treatment flattens hierarchy and forces the patient to scan 7 rows of personal data before reaching the CTA. This is the cognitive-load valley — the screen shifts from "your appointment" to "your paperwork."

**Fix:** Consider making the identity card collapsible (expanded for first-time users, collapsed for returning users). At minimum, visually de-emphasize low-information rows or group them into an expandable "Additional details" section.

**Suggested command:** /impeccable distill

## [P2] No submission confirmation or intermediate state

**What:** Tapping "Send request to clinic" transitions directly to success or error. The only feedback during submission is the CTA's own CircularProgressIndicator.

**Why:** For "sending health information to a clinic," the patient needs stronger feedback. The spinner-in-button pattern is appropriate for low-stakes actions, not a full appointment submission. A frozen screen during submission is anxiety-inducing.

**Fix:** Add a full-screen semi-transparent overlay with centered progress indicator and calm message ("Sending your request to the clinic…") during isSubmitting.

**Suggested command:** /impeccable polish

## [P3] "No backup times" message is ambiguous

**What:** Line 161: "No backup times — the clinic will work with your preferred time." The phrase "work with" is vague — it could mean "try our best" or "guarantee."

**Why:** Ambiguity on a review screen is anxiety-inducing. The patient doesn't know if their preferred time is locked or tentative.

**Fix:** Either remove the message entirely (absence is self-evident) or sharpen: "No backup times selected. The clinic will confirm your preferred time or contact you."

**Suggested command:** /impeccable clarify

# Persona Red Flags

## Jordan (First-Timer)
- **Identity card overwhelm:** 7 rows of personal data with no explanation of why occupation or home address is relevant to an eye appointment. No contextual help on why this data is collected.
- **No "what happens next" info:** After tapping "Send," Jordan has no expectation of response time or notification method.

## Sam (Accessibility)
- **Card header grouping:** The three cards' titles ("Appointment", "Reason for visit", "Your details") are not grouped with their edit buttons in the accessibility tree. A screen reader user navigating by headings hears the title as a separate focus stop from the edit action. Consider wrapping the header row in mergeDescendants.
- **Divider contrast:** The 8% alpha divider at line 113 may not meet 3:1 non-text contrast for meaningful visual separators.

## Casey (Distracted Mobile)
- **Bottom bar compression:** On small screens, the non-binding notice text in the bottom bar may be compressed or partially obscured by system navigation. Casey could tap "Send" without reading the key reassurance.
- **No section jump links:** If interrupted mid-review and returning, Casey must scroll from the top to re-find their place. No visual indicator of reviewed cards.

# Minor Observations

- **Asymmetric card padding** (line 328): start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp creates visual left-heavy weight. Inconsistent with the design system's standard 16dp padding.
- **NonBindingNotice is defined but unused** (line 432). Dead code that represents a *better* design variant is a red flag.
- **Divider uses raw alpha** (line 113): onSurface.copy(alpha = 0.08f) instead of DESIGN.md's outlineVariant or card-border tokens. May not match warm gray tone in dark mode.
- **No preview composable.** The file has no @Preview function, which slows design iteration and increases visual regression risk.
- **Off-rhythm spacing:** Spacer(6.dp) at line 357 should be 8.dp; Arrangement.spacedBy(10.dp) at line 104 should be 12.dp.

# Questions to Consider

1. **Why is the identity card always expanded?** If the patient confirmed their details in a previous step, what are they verifying here? Could it default to collapsed with "Your details confirmed," expanding only on tap?
2. **Is "Send request to clinic" the right CTA label?** "Send" implies immediate transmission, but the notice says "the clinic reviews every request." Would "Submit request" or "Request this appointment" better match the semantics?
3. **What happens if the patient scrolls past the appointment card?** The non-binding notice is in the bottom bar, not the content. If the identity card pushes the appointment card above the fold, the patient's last visual memory before the CTA is their own data — not the appointment. Is that the right emotional sequence?
4. **Why are backup slots shown on the review at all?** The patient already selected them on the schedule step. Is the purpose to confirm, or to provide "last chance to change"? The edit button already serves the latter.
5. **What's the dark-mode story?** The primaryContainer ticket surface uses Material's container color (#123846 in dark mode). Has this specific surface been tested for contrast, or is it an assumption that the theme handles it correctly?
