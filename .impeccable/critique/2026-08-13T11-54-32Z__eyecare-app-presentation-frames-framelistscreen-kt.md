---
target: frames catalog page
total_score: 24
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
timestamp: 2026-08-13T11-54-32Z
slug: eyecare-app-presentation-frames-framelistscreen-kt
---
## Design Health Score

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of System Status | 2/4 | Initial load and load-more states exist, but refresh/search/sort can keep showing Success with no visible progress; load-more failures are silent. |
| 2 | Match System / Real World | 3/4 | “Frames,” search, price, and sort are familiar; “AR” is unexplained product jargon. |
| 3 | User Control and Freedom | 3/4 | Search can be cleared and errors retried, but empty results have no reset action and filtering state is not fully recoverable. |
| 4 | Consistency and Standards | 3/4 | Material cards, segmented controls, and theme tokens are coherent; rating color and search shape drift from the documented system. |
| 5 | Error Prevention | 2/4 | Media failures, stale search responses, and unavailable prices are not proactively explained or guarded. |
| 6 | Recognition Rather Than Recall | 3/4 | Brand, name, rating, price, and AR cue are visible; users must remember what AR means and what it enables. |
| 7 | Flexibility and Efficiency of Use | 2/4 | Search, sort, pull-to-refresh, and pagination exist, but there is no AR/category/brand filter or shortcut to the app’s signature action. |
| 8 | Aesthetic and Minimalist Design | 3/4 | The two-up grid is clean and scannable, though ratings and passive badges add density without improving the next action. |
| 9 | Error Recovery | 2/4 | Full-list errors offer Retry, but no-result, image-error, and load-more failure states offer little guidance. |
| 10 | Help and Documentation | 1/4 | There is no contextual explanation for AR, sorting, image states, or what to do when no frames match. |
| **Total** |  | **24/40** | **Acceptable — a solid foundation with significant browse-state and differentiation work needed.** |

## Design Specificity Verdict

The catalog is product-grounded but not yet product-distinctive. The Eyecare identity is visible in the Outfit/DM Sans type system, Lens Cyan accents, warm surfaces, border-led cards, two-column frame grid, and AR badge. The underlying browse composition—search, two sort choices, and generic product cards—could still belong to almost any optical or retail app.

The largest missed opportunity is that AR is Eyecare’s differentiating mechanism, but the catalog only exposes it as a small “AR” badge. It does not explain the benefit, offer an AR-ready filter, or make “Try on” feel like a next action.

The deterministic detector returned zero findings for this native Kotlin/Compose target. That is a clean automated result, not evidence that the Android runtime states are complete. Its manual watchpoints agree with the source review: the empty state lacks recovery, refresh/search/sort feedback can disappear, and the rating star uses raw primary on a light surface despite the theme’s contrast guidance.

## Overall Impression

The page feels calm, orderly, and easy to scan, but it currently behaves more like a static inventory grid than an eyewear discovery experience. The single biggest opportunity is to turn each catalog visit into a confident next step—especially “Try on”—while making loading, image, empty, and failure states feel intentional.

## What’s Working

- The two-up grid, 12dp gutters, 16dp card corners, restrained elevation, and compact metadata create a strong mobile browsing rhythm. See [FrameListScreen.kt](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/FrameListScreen.kt:108) and [FrameCard.kt](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/components/FrameCard.kt:51).
- Search, sorting, clear-search, pull-to-refresh, pagination, and retry are discoverable without flooding the screen with choices.
- The AR badge and accessible accentText treatment give the grid a product-specific cue, even though that cue needs to become more actionable.

## Priority Issues

### [P1] The catalog hides the product’s signature action

**Why it matters:** “AR” is an unexplained abbreviation and a passive badge. A patient can browse several cards without understanding which frames can be tried on or how to start. That weakens the reason to use Eyecare instead of a generic catalog.

**Fix:** Use plain language such as “Try on” or “AR-ready,” make the badge’s semantics explicit, and add a compact AR-ready filter or a clear try-on affordance in the detail transition. Keep Reserve as the detail-level commitment; the catalog should make discovery and try-on obvious.

**Suggested command:** $impeccable clarify followed by $impeccable shape

### [P1] Product imagery has no intentional loading or failure state

**Why it matters:** The image is the primary decision aid, yet AsyncImage has no placeholder or error content and uses ContentScale.Crop. Eyewear can be cropped or appear as an unexplained blank tonal square while loading or after a failed request.

**Fix:** Use ContentScale.Fit for frame photography, preserve the stable square, show the established eyewear icon/photo fallback while loading and on failure, and keep the frame name available in the visual fallback. Match the detail screen’s “Photo coming soon” treatment.

**Evidence:** [FrameCard.kt](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/components/FrameCard.kt:63).

**Suggested command:** $impeccable harden

### [P1] Search, sort, and refresh can look broken while work is happening

**Why it matters:** The ViewModel keeps the UI in Success while resetAndLoad() starts another coroutine, so the user can see old cards with no progress cue. Rapid searches can also leave overlapping loads that race to update the grid. A user may think their query or sort did not apply.

**Fix:** Model refreshing/filtering as explicit state alongside the current results, show a small inline progress indicator while preserving the grid, cancel the actual in-flight request rather than only the debounce delay, and surface a Snackbar for load-more failures.

**Evidence:** [FrameListViewModel.kt](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/FrameListViewModel.kt:52) and [FrameListScreen.kt](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/FrameListScreen.kt:96).

**Suggested command:** $impeccable harden

### [P2] “No frames found” is a dead end

**Why it matters:** The same terse message can represent an empty inventory or a search that is too narrow. It gives the user no explanation, filter context, or recovery path.

**Fix:** Distinguish the empty catalog from no search matches. For a query, say “No frames match ‘…’” and provide “Clear search”; use the shared empty-state component and icon treatment. If sort/filter controls expand later, add “Reset filters.”

**Evidence:** [FrameListScreen.kt](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/FrameListScreen.kt:119).

**Suggested command:** $impeccable clarify

### [P2] The catalog has a documented contrast drift

**Why it matters:** The rating star uses MaterialTheme.colorScheme.primary on a light card, while the design system explicitly reserves primary for fills and requires accentText for light-surface icons/text. The star may be difficult to perceive for low-vision users and makes the grid feel less finished.

**Fix:** Tint the star with EyecareColors.current.accentText, then verify the dark theme and all rating states.

**Evidence:** [RatingBadge.kt](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/components/RatingBadge.kt:49) and [DESIGN.md](C:/Users/ironm/AndroidStudioProjects/EyecareV2/DESIGN.md:145).

**Suggested command:** $impeccable audit

## Cognitive Load

Two of eight checklist items fail:

- **Visual hierarchy:** the product cards are clear, but the most differentiated action—AR try-on—is reduced to a low-explanation badge.
- **Progressive disclosure:** cards show several metadata layers while still withholding the meaning and consequence of AR.

There are no decision points above four visible options: search is one input and sorting exposes only Name/Newest. Overall cognitive load is moderate but not overwhelming; the issue is prioritization, not raw density.

## Emotional Journey

The entry is calm and reassuring: the title, search field, and evenly paced cards make browsing feel safe. The emotional valley comes when images are blank, stale results remain during a query, or a search returns only “No frames found.” The peak is also underdeveloped: the catalog should create anticipation around seeing a frame on the patient’s face, but the current AR badge does not carry that promise. The detail screen is more expressive, so the catalog currently asks users to invest a tap before revealing the product’s most distinctive value.

## Persona Red Flags

**Jordan — confused first-timer**

- The first action is visible, but “AR” is not explained and has no nearby “try it on” language.
- A no-result search leaves Jordan with “No frames found” and no recovery instruction.
- There is no contextual help for the difference between browsing, AR try-on, and reserving.

**Casey — distracted mobile user**

- The two-up grid is thumb-friendly, but important feedback is not: a refresh or search can remain visually unchanged while the network request runs.
- Slow or interrupted image loading produces blank product areas with no reassuring placeholder.
- The primary differentiator sits at the top-right of the image, away from the most obvious card action, and is not phrased as a tap-worthy action.

**Riley — deliberate stress tester**

- Rapid searches can create overlapping loads because the debounce job does not own the network job it starts; stale results may replace newer results.
- Load-more failure resets silently, so Riley cannot tell whether there are no more frames or the request failed.
- Long or unavailable catalog data is not clearly represented: the card uses only the first variant price and an empty string when no price exists.

## Minor Observations

- FrameCard displays the first variant’s price without a “from” label or indication that other options may differ ([FrameCard.kt](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/components/FrameCard.kt:49)).
- “Loading more…” is text-only; a small progress indicator would communicate an active pagination request faster.
- The search field is a 32dp-radius pill while the documented field shape is compact; if the pill is intentional for search, formalize it as a search-specific component rather than letting it look like a one-off.
- The grid has no visible brand/category/AR filters, so discovery depends heavily on typing and two sort choices.

## Questions to Consider

- If AR is the reason this catalog belongs in Eyecare, why is its only list-level cue a two-letter badge?
- Should the catalog optimize for a single next action—“Try on”—or remain a neutral inventory browser?
- Would a patient trust a blank gray square as a product photo, or should every media state visibly explain itself?
