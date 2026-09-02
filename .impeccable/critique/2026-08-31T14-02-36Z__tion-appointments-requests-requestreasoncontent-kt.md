---
target: the reason-for-visit step in request appointment
total_score: 27
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
timestamp: 2026-08-31T14-02-36Z
slug: tion-appointments-requests-requestreasoncontent-kt
---
Method: dual-agent (Assessment A: design review · Assessment B: detector + evidence)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Validation errors render with no live-region semantics, unlike the sibling Type step's notice — a TalkBack user gets no automatic announcement when Continue fails. |
| 2 | Match System / Real World | 3 | Preset vocabulary and error copy are plain and clinically apt, but degrade to generic phrasing whenever an appointment type has no presets. |
| 3 | User Control and Freedom | 3 | Back preserves drafts, but a single chip tap already sets `hasUnsavedInput = true`, triggering a "Discard this request?" dialog for a trivially reversible action. |
| 4 | Consistency and Standards | 2 | The two `FilterChip`s pass no `colors`/`border` override — every other themed chip in the app (catalog filters, AR variant chips) explicitly applies the brand recipe; these render in stock, unbranded Material tones. |
| 5 | Error Prevention | 2 | No live length-capping or color change on the character counter — it will read "5000 of 1000 characters" in neutral gray with zero visual alarm; the limit is enforced only at submit time. |
| 6 | Recognition Rather Than Recall | 3 | Field label/placeholder adapt to context well, but the composed final string ("Preset: details") is never previewed on this step, only at Review. |
| 7 | Flexibility and Efficiency | 2 | No accelerator for a returning/frequent patient — identical friction every visit. |
| 8 | Aesthetic and Minimalist Design | 3 | Genuinely lean layout, undercut by the unbranded chips making the step look visually flatter than its neighbors. |
| 9 | Error Recovery | 3 | Error copy is calm and actionable, but nothing auto-scrolls or focuses to an off-screen error, unlike the Identity step's explicit focus mechanism. |
| 10 | Help and Documentation | 3 | No inline reassurance beyond one explanatory line; applicable and modestly served. |
| **Total** | | **27/40** | **Acceptable — significant improvements needed** |

## Design Specificity Verdict

**Contingently specific, not authored specific.** All the eye-care vocabulary on this step comes from backend data (preset labels like "Blurred or reduced vision," "Eye pain or discomfort") and two hand-written placeholder strings. Strip the presets away and the step's own authored copy — "What would you like to be seen for?", "A short description helps the clinic prepare for your visit and decide how much time you need." — reads as a generic "describe your issue" intake form for any clinic. The code proves this itself: in the zero-preset branch, every trace of optical framing disappears except one placeholder string. Nothing about the layout, iconography, or structure signals "eye clinic" specifically.

**Deterministic scan:** `detect.mjs` returned exit 0 / empty findings against the requests directory — again not a clean bill of health, since `.kt` isn't in the detector's scannable-extension list and 0 of the 14 files in scope were actually examined. The manual grep that substituted for it found genuinely clean results on several axes: zero raw hex colors or `Color.White`/`Color.Black` anywhere in the directory, all typography routed through `MaterialTheme.typography` roles, and both `FilterChip`s correctly carry `heightIn(min = 48.dp)` for touch targets. It also independently deepened the P2 counter finding: `updateReason` in the ViewModel never clamps the typed string — `maxLines = 6` is a rendering constraint only, so a patient can type or paste arbitrarily far past the 1000-character limit with no cap and no color change, discovering the problem only when Continue bounces them back with a generic error. And it surfaced something neither review specifically went looking for: **zero strings anywhere in the entire appointments package are routed through `stringResource(...)`** — a codebase-wide pattern, not specific to this step. On the positive side, IME handling is not missing: `RequestStepScaffold` applies `imePadding()` at the scaffold level (with a documented rationale for why it's there rather than on the content), so the bottom-anchored primary button and the growing text field both shift correctly when the keyboard opens.

The detector's regex approach also has a structural blind spot worth naming: it can catch a *wrong* color literal but not an *absent* one — the unbranded `FilterChip`s (missing `colors`/`border` entirely) wouldn't trigger a pattern-match rule even if `.kt` were supported, since nothing is technically "wrong," just undecorated. That gap is exactly why Assessment A's read against DESIGN.md and codebase precedent caught it and a regex scan structurally couldn't.

No live browser/dev-server target exists for this native Compose screen, so browser visualization was correctly skipped.

## Overall Impression

This step is the calmest, best-engineered part of the request-appointment flow — progressive disclosure, draft preservation across every path, and plain non-technical error copy all show real care. But it's the one moment in the wizard where a patient states *why they need care*, and it gets shipped with the same stock Material chip styling every other selectable surface in the app deliberately overrides, plus a character counter that can't tell a patient they've badly overshot the limit until they've already tried to submit. The biggest opportunity is closing that gap between how carefully the step's logic was built and how little of the app's actual visual identity or protective feedback made it into the finished screen.

## What's Working

1. **Progressive disclosure of the free-text field** — hiding it until a preset or "Other" is chosen keeps the initial screen genuinely light, a real cognitive-load win.
2. **Draft preservation across every path** — reason and referral drafts survive back-navigation, appointment-type switching, and error states, concretely honoring PRODUCT.md's "recoverable" principle.
3. **Plain, non-technical error copy** — patient-facing validation messages are specific and calm, matching DESIGN.md's "clear" pillar.

## Priority Issues

**[P1] The two most-tapped controls on the step ship with stock, unbranded Material styling**
- **Why it matters:** Both `FilterChip` calls pass no `colors`/`border` parameters, while every other themed chip in the app (frame-catalog filters, the AR variant chips) explicitly applies the app's Lens Cyan selected-chip recipe. DESIGN.md itself warns that Material's default secondary/secondaryContainer roles are unbranded — meaning the selection state on the single most-interacted control of this step likely renders in the wrong color family entirely, confirmed by grep: no `FilterChipDefaults` override exists anywhere in this file.
- **Fix:** Apply the same `filterChipColors`/`filterChipBorder` recipe already used in `FrameCatalogControls.kt` and `VariantChipRow.kt`.
- **Suggested command:** `/impeccable polish`

**[P1] The character counter — and the field itself — never signal an over-limit state until submit**
- **Why it matters:** The counter renders unconditionally in neutral `onSurfaceVariant`, with no branch to error color even far past the 1000-character limit, and confirmed by code reading: `updateReason` never clamps the typed string, so a pasted or typed entry can run 5x over the cap with zero visual alarm. A patient discovers the problem only after tapping Continue and being bounced back.
- **Fix:** Switch the counter (and the field's `isError`) to error styling live once length exceeds `maxReasonForVisitLength`, rather than deferring all feedback to submit time.
- **Suggested command:** `/impeccable harden`

**[P2] No focus or scroll-to-error, unlike the sibling Identity step**
- **Why it matters:** Reason and referral errors are set in state but nothing scrolls or focuses to them, in contrast to Identity's explicit `focusField`/`onFocusHandled` mechanism. On this scrollable step, with the referral block sometimes off-screen, a patient can tap Continue, see nothing visibly change, and not understand why the flow didn't advance.
- **Fix:** Reuse the same focus-request mechanism the Identity step already has, or at minimum auto-scroll to the first error.
- **Suggested command:** `/impeccable clarify`

**[P2] A single chip tap triggers the same "Discard this request?" dialog as substantial typed work**
- **Why it matters:** `hasUnsavedInput` for the Reason step flips true the instant a reason choice is made, so tapping one preset chip and pressing Back immediately surfaces a destructive-sounding confirmation dialog — disproportionate friction for a near-zero-cost, fully reversible action, at a step where the patient may already feel some friction describing a symptom.
- **Fix:** Gate the discard dialog on actual typed content (`reason.isNotBlank() || referringSource.isNotBlank()`) rather than choice selection alone.
- **Suggested command:** `/impeccable clarify`

**[P3] No accessibility grouping semantics on the chip set**
- **Why it matters:** The preset `FilterChip` row has no `selectableGroup()`/radio-role wiring, and section labels like "Common reasons" aren't marked as headings — a TalkBack user gets each chip announced individually with no signal they're mutually exclusive, and can't navigate by heading to the referral block.
- **Fix:** Wrap the row in `Modifier.semantics { selectableGroup() }` and mark section headers with `Modifier.semantics { heading() }`.
- **Suggested command:** `/impeccable audit`

## Persona Red Flags

**Sam (Accessibility-Dependent):** Hit by two issues at once — no live-region announcement when submit-time validation fails, and no group semantics on the chip set. A TalkBack user who fails validation may get no reliable signal of what happened or where to go next.

**Riley (Stress-Tester):** Directly exposed by the counter/length-cap gap — pasting an oversized reason produces a neutral-toned "N of 1000 characters" readout that reads as informational rather than blocking, and the field never marks itself as an error state until Continue is pressed.

**Casey (Distracted Mobile User):** Tapping a preset chip and then bailing out (interrupted mid-task) triggers a "Discard this request?" dialog for what was a one-tap, fully reversible action — disproportionate friction for someone who wasn't deeply invested yet.

## Minor Observations

- Zero strings across the entire appointments package — not just this step — route through `stringResource(...)`; a codebase-wide pattern rather than something specific to Reason.
- The step's own docstring claims it's "short enough that a patient... can finish it in a single thought," yet the 1000-character ceiling permits a genuinely long essay — the code's self-description and its actual limit are in tension.
- The referral field uses `Words` capitalization appropriate to its "Dr. Santos, Manila General" example, but no explicit `imeAction`/`Done` wiring was found to confirm smooth field-to-field keyboard flow.
- IME handling is correctly centralized in `RequestStepScaffold`'s `imePadding()` with a documented rationale — worth calling out as something already done right, not a gap.

## Questions to Consider

- If every other selectable surface in this app gets deliberate brand theming, why do the two most-tapped controls on this specific step ship with stock Material defaults — an oversight, or a deliberate exception that isn't documented anywhere?
- The step goes out of its way to hide the text field until a choice is made, then lets a patient type five times past the limit with zero visual alarm — why does progressive disclosure get design attention here but length-limit feedback doesn't?
- Given the product principle of making routine patient tasks "understandable and recoverable," why does this specific step interrupt with a destructive-sounding discard dialog for a single chip tap, when comparably lightweight, reversible interactions elsewhere in the wizard don't carry that friction?
