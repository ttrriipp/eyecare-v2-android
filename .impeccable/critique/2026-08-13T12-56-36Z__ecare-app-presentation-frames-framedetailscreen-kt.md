---
target: frame detail page
total_score: 25
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
timestamp: 2026-08-13T12-56-36Z
slug: ecare-app-presentation-frames-framedetailscreen-kt
---
⚠️ DEGRADED: single-context (spawn_agent unavailable in this session; Assessment A was completed inline and Assessment B ran the detector independently)

## Design Health Score

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of System Status | 2/4 | Initial loading is visible, but retry/refresh does not enter a visible in-flight state and image loading/failure is silent. |
| 2 | Match System / Real World | 3/4 | The photo, price, options, details, and reservation actions follow familiar product-detail conventions; SKU and AR behavior need more context. |
| 3 | User Control and Freedom | 3/4 | Back, retry, pager swiping, and variant selection exist; image recovery and clear selection feedback are incomplete. |
| 4 | Consistency and Standards | 3/4 | Border-led cards, cyan accents, and button hierarchy fit Eyecare, but repeated headline sizing and the footer layout drift from the documented system. |
| 5 | Error Prevention | 2/4 | Selecting a variant can silently change the available actions, while selection semantics, availability context, and media failure guards are missing. |
| 6 | Recognition Rather Than Recall | 3/4 | The selected product, price, variants, and actions are visible; AR eligibility and image position are not fully explained. |
| 7 | Flexibility and Efficiency | 2/4 | Swipeable media, direct try-on, and variant shortcuts help; there is no image zoom and many variants become a long visible decision row. |
| 8 | Aesthetic and Minimalist Design | 3/4 | The calm stacked-card composition is readable, but repeated icon tiles, oversized headings, and a heavy fixed footer add weight. |
| 9 | Help Users Recognize, Diagnose, and Recover from Errors | 2/4 | Whole-screen load errors can retry, but failed images and retry-in-progress states provide no actionable feedback. |
| 10 | Help and Documentation | 1/4 | There is no contextual explanation of AR try-on, variant differences, or what happens after Reserve. |
| **Total** |  | **25/40** | **Acceptable — a solid foundation with important confidence and state-communication work remaining.** |

## Design Specificity Verdict

### LLM assessment

The page is product-grounded: warm surfaces, border-led cards, cyan information cues, optical imagery, AR try-on, and frame reservations clearly belong to Eyecare. The sticky bottom actions also make the intended journey legible.

However, the structure still reads like a generic commerce detail template: blank top bar, hero carousel, product card, options card, details card, fixed buttons. The product’s differentiator—seeing a frame on the patient’s face—is named as “Try on,” but the page does not explain what AR-ready means or build confidence around that action.

### Deterministic scan

The bundled detector returned [] with zero findings for FrameDetailScreen.kt. That is a clean static result; it does not cover native runtime states, Compose semantics, failed image requests, pager state, or concurrent ViewModel loads.

Browser overlays were not applicable: this is a native Android Compose target, not a browser-renderable page. No live server, browser tab, overlay injection, or browser cleanup was used.

## Overall Impression

The detail screen is calm, scannable, and action-oriented. The strongest choice is the thumb-friendly footer that makes “Try on” primary and “Reserve” secondary for AR-ready variants.

The biggest opportunity is to turn it from a catalog record into a confidence-building decision surface: make the hero image reliable, keep the selected variant and its capabilities unmistakable, and communicate progress when the network is working.

## What’s Working

- The action hierarchy is correct: AR-ready variants lead with “Try on,” while “Reserve” remains available as the commitment step.
- The image, summary, options, and details are grouped into predictable 16dp border-led cards that match the established Eyecare visual language.
- The screen preserves a clear back path and provides a full-list retry state instead of leaving a failed fetch blank.

## Priority Issues

### [P1] The hero image has no loading or failure contract

**Why it matters:** The image is the primary decision aid, but AsyncImage only has a fallback when the image list is empty. A non-empty URL that is slow, malformed, or unavailable leaves a tonal rectangle with no explanation. That is especially damaging on a frame page, where shape and fit determine whether “Reserve” feels safe.

**Fix:** Track image loading state for each pager page. Show a stable “Loading photo…” treatment, a “Photo unavailable” fallback with the frame name, and a retry action or clear recovery path. Add an accessible position label such as “Image 1 of 3”; do not rely on the tiny decorative dots alone.

**Suggested command:** $impeccable harden

### [P1] Retry and refresh do not communicate in-flight work

**Why it matters:** FrameDetailViewModel.refresh() calls load(), but load() does not set a loading/refreshing state or cancel a previous request. After tapping Retry, the error screen can remain unchanged while the request runs; repeated taps can create overlapping loads whose completion order is not controlled.

**Fix:** Model isRefreshing alongside existing content, disable or replace Retry while active, cancel the previous Job, and ignore stale results after cancellation. Preserve the current detail content during a refresh when possible.

**Suggested command:** $impeccable harden

### [P1] Variant selection is visually selectable but not semantically explicit

**Why it matters:** The Surface(onClick = …) chips use color and border weight to indicate selection, but do not expose a selected/radio state to assistive technology. They are also only 40dp minimum height. More importantly, selecting a variant can change the hero images and change the footer from “Try on + Reserve” to only “Reserve” without an explicit explanation near the selected option.

**Fix:** Use selectable semantics with a radio-button role, a 48dp minimum touch target, and a clear selected label. Show an inline capability cue such as “AR-ready — try it on” or “Reserve in clinic.” Reset the pager to page 1 whenever the selected variant changes.

**Suggested command:** $impeccable clarify

### [P2] The fixed action footer needs stronger layout and capability context

**Why it matters:** The footer is the right location for mobile use, but its Row relies on weighted buttons without explicitly filling the available width. That can produce inconsistent sizing across Compose constraints. The action change based on variant eligibility is also easy to miss because the reason is not stated.

**Fix:** Give the action row fillMaxWidth(), keep both buttons at least 48dp high, and place a compact “AR-ready” or “Try this frame on first” cue above the actions when applicable. Keep the footer visually lighter than the content rather than using an 8dp shadow where the design system generally uses borders and restrained elevation.

**Suggested command:** $impeccable layout

### [P2] The page loses product context while scrolling

**Why it matters:** TopAppBar has an empty title. Once the user scrolls past the hero and summary, the only persistent navigation cue is Back, so a long specifications/description section can feel detached from the frame they are evaluating.

**Fix:** Add a compact title/brand to the app bar or use a collapsing title that appears after the hero leaves the viewport. Keep the action footer fixed, but let the current frame identity remain available.

**Suggested command:** $impeccable layout

### [P2] Section hierarchy is heavier than the information warrants

**Why it matters:** The frame name, “Options,” and “Details” all use headlineMedium, which makes every section compete with the product identity. The SKU, category, option, description, and arbitrary specifications then share one long card, increasing scan cost.

**Fix:** Use the documented detail hierarchy: one strong frame title, titleLarge/title for section labels, and body styles for values. Keep the primary facts near the hero, then progressively disclose technical specifications and long descriptions. Label crossed-out pricing as “Was” or show the savings explicitly.

**Suggested command:** $impeccable typeset

## Cognitive Load

Three of eight checklist items fail, giving the screen moderate cognitive load:

- **Visual hierarchy:** the product name, section headings, price, and sticky actions compete more than necessary.
- **Minimal choices:** all variants are exposed at once; a large variant set can exceed four simultaneous choices.
- **Progressive disclosure:** technical facts and descriptive content are presented in one expanding Details block instead of staged around the decision.

The primary decision itself is sound—choose a variant, then Try on or Reserve—but the selected variant’s capability should remain visible while making that decision.

## Emotional Journey

The entry is calm and reassuring when the hero image loads, with familiar product information and a clear next step. The main emotional valley is a blank or stalled hero image: users cannot judge the frame but are still presented with Reserve. The peak is the direct Try on action, which gives the product a reason to exist beyond a generic catalog. The end of the detail page is operationally clear, but the transition into reservation would feel more trustworthy if the selected variant and its eligibility were repeated in the action context.

## Persona Red Flags

### Jordan — confused first-timer

- “Try on” is clearer than “AR,” but there is no nearby explanation of what will happen after tapping it.
- Selecting a different option can remove Try on without saying why.
- SKU and technical specifications appear without explaining whether they matter to the patient.

### Sam — accessibility-dependent user

- Variant controls communicate selected state visually but not as selectable/radio semantics.
- The 40dp variant targets are below the project’s 48dp button convention.
- Pager dots are tiny visual indicators with no explicit image count or position announcement.
- Image failure is conveyed only by an empty tonal area.

### Casey — distracted mobile user

- The fixed footer is in the thumb zone, which is good, but the action row’s weighted layout should explicitly fill width.
- A slow image request leaves no reassuring progress state.
- The blank top-bar title makes it harder to regain context after an interruption or a long scroll.

## Minor Observations

- Currency formatting uses the device default locale here, while the catalog uses Locale.US; the two screens can format the same price differently.
- brand.uppercase() is locale-sensitive and can produce awkward output for some languages.
- The inactive pager dots use semi-transparent white, which may be weak against bright imagery and does not have a dark-surface treatment.
- The pager indicators are not clickable; a compact “1 / 3” label would be clearer and more accessible.
- The crossed-out comparison price has no “Was” label or savings amount.
- HtmlCompat strips the description into plain text; if formatting conveys meaningful structure, the result may become a dense paragraph.

## Questions to Consider

- Should the selected variant summary always show its capability—“AR-ready” or “Reserve in clinic”—so the footer never changes unexpectedly?
- Is the hero meant to sell the frame visually, or should it become a try-on preview entry point with stronger image controls?
- When a frame has more than four variants, should the options become a compact picker instead of a wall of chips?
