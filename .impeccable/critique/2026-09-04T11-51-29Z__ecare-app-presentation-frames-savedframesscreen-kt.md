---
target: saved frames list page
total_score: 22
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 4
timestamp: 2026-09-04T11-51-29Z
slug: ecare-app-presentation-frames-savedframesscreen-kt
---
## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2/4 | States exist, but the initial spinner is unlabeled, removal disappears silently, and refresh/load-more feedback is easy to miss. |
| 2 | Match System / Real World | 3/4 | “Saved frames,” price, and saved date are familiar; UNKNOWN is presented as if it means confirmed unavailable. |
| 3 | User Control and Freedom | 2/4 | Back and refresh work, but remove has no undo/confirmation and the empty path has no direct exit into frame browsing. |
| 4 | Consistency and Standards | 2/4 | The row diverges from the catalog card’s brand/product hierarchy, border treatment, image fallback, and AR affordance. |
| 5 | Error Prevention | 2/4 | Single-flight removal prevents duplicate requests, but destructive removal has no guardrail and availability semantics are collapsed. |
| 6 | Recognition Rather Than Recall | 3/4 | Core identity, price, saved time, and remove action are visible; whole-card navigation and the next empty-state action are implicit. |
| 7 | Flexibility and Efficiency | 2/4 | Pull-to-refresh and pagination exist, but there is no direct browse/AR path or efficient recovery for row-level failures. |
| 8 | Aesthetic and Minimalist Design | 3/4 | Calm spacing and restrained color work, but the metadata stack is dense and the delete affordance competes with the primary identity. |
| 9 | Help Users Recognize, Diagnose, and Recover from Errors | 2/4 | Initial retry is clear; refresh/remove failures say “Try again” while rendering only “Dismiss.” |
| 10 | Help and Documentation | 1/4 | The required preference-only/availability explanation is absent from this screen. |
| **Total** | | **22/40** | **Acceptable — significant improvements needed before users feel confident.** |

## Design Specificity Verdict

### LLM assessment

This is a product-aligned foundation, but it still reads like a generic wishlist. Eyecare’s cyan, warm-surface, rounded-card language is present, yet the screen does not express the product-specific distinction between a remembered eyewear preference and a reservation/purchase commitment. It also misses the opportunity to reconnect a saved choice to frame discovery or AR try-on.

### Deterministic scan

detect.mjs --json returned an empty array with exit code 0: 0 findings, 0 failures, and no rule locations. That detector is a generic source-text pass and is not Compose/runtime-aware, so the clean result does not validate layout, semantics, or interaction quality. No .impeccable/critique/ignore.md exists.

## Overall Impression

The screen is operationally disciplined: the state model, native top bar, pagination, and per-row loading behavior are sensible. The experience is not yet reassuring enough for a patient-facing shortlist because important semantics and next actions are missing. The single biggest opportunity is to make every state answer three questions at a glance: “What frame is this?”, “Can I still get it?”, and “What should I do next?”

## What’s Working

- SavedFramesUiState explicitly models initial loading/error, refresh, pagination, row removal, and inline failure states (SavedFramesViewModel.kt:14–28).
- The screen follows Android expectations with a Material TopAppBar, Back navigation, 48dp icon targets, and exact FrameDetail(frameId, variantId) routing (SavedFramesScreen.kt:73–84,288–303; NavGraph.kt:469–482).
- The restrained 16dp card language and accessible accentText usage fit the established Eyecare system (SavedFramesScreen.kt:221–227,272–277; DESIGN.md:273–303).

## Priority Issues

### [P1] Disclose preference-only semantics and preserve availability truth

Why it matters: The product contract requires the Android save surface to explain that saved frames are preferences and do not guarantee availability (CONTEXT.md:296–303; API_CONTRACT.md:1881–1889). SavedFrameDisclaimer exists but is not used by this screen (SavedFrameDisclaimer.kt:13–28). In addition, SavedFramesScreen.kt:283–285 shows the same badge for both UNAVAILABLE and UNKNOWN, although the domain distinguishes them (SavedFrame.kt:37–47). Red styling makes an unknown backend state feel like a confirmed stock failure.

Fix: Add the compact disclaimer as the first list item or directly beneath the app bar/empty-state copy. Render UNAVAILABLE as “Unavailable,” UNKNOWN as “Availability unknown” or “Check availability,” and avoid error-red treatment for uncertainty. Reconcile the current tests that assert the disclaimer does not exist with the current product/API contract.

Suggested command: $impeccable clarify

### [P1] The empty state is a dead end

Why it matters: EmptySavedFrames() tells users to browse frames but provides no action (SavedFramesScreen.kt:324–350). Saved Frames is a sub-destination with the bottom navigation hidden; the user must remember to press Back and then find Frames (NavGraph.kt:469–482).

Fix: Add an onNavigateToFrames callback and a primary “Browse frames” action. Keep the bookmark explanation, but turn the state into a clear handoff back to the catalog. Place the disclaimer beneath the action or in the same quiet tonal panel.

Suggested command: $impeccable onboard

### [P1] Error recovery is buried and mismatched

Why it matters: The ViewModel produces “Couldn’t refresh. Try again.” and “Couldn’t remove this frame. Try again.” (SavedFramesViewModel.kt:105–119), but the composable renders “Dismiss” whenever canRetryLoadMore is false (SavedFramesScreen.kt:183–208). Refresh/remove failures therefore offer no direct retry, and the error sits after the entire list.

Fix: Use operation-specific retry metadata. A refresh failure should keep the list visible with a retry Snackbar/action; a remove failure should keep the row and offer “Try again” for that exact variant. Keep “Retry” for load-more only, and announce successful removal with an Undo action if undo is added.

Suggested command: $impeccable harden

### [P1] The row hierarchy hides the actual frame identity

Why it matters: The product name is rendered as labelSmall, the variant name becomes the strongest identity, and the brand is omitted (SavedFramesScreen.kt:259–282). The established catalog card uses brand → product name → price hierarchy and stronger image handling (FrameCard.kt:113–151,190–225). A patient scanning saved items should recognize the frame before reading metadata.

Fix: Reorder to brand (quiet eyebrow) → product name (title) → variant/colour (secondary) → price (strong accent) → saved time/status (muted). Preserve the 64dp thumbnail, add an explicit loading/error placeholder like the catalog, and consider a small AR-ready affordance only when the saved variant actually has typed AR data.

Suggested command: $impeccable layout

### [P2] Removal lacks confirmation or undo

Why it matters: Tapping the red delete icon immediately removes the row (SavedFramesScreen.kt:288–303), while Frame Detail uses an explicit “Remove saved frame?” confirmation with “Keep saved” (FrameDetailScreen.kt:484–498). The same preference is therefore safer to remove from one surface than another, and a distracted user has no recovery path.

Fix: Prefer a Snackbar reading “Removed [frame] from saved” with an Undo action for this low-risk, reversible preference; otherwise reuse the existing confirmation dialog for consistency.

Suggested command: $impeccable harden

## Persona Red Flags

### Jordan — First-Timer

- The empty copy says “Browse frames” but does not expose a Browse frames button.
- “Unavailable” does not explain whether the item is sold out, temporarily unknown, or simply needs a detail refresh.
- A whole-card tap opens detail without a visible chevron or “View details” cue.

### Sam — Accessibility-Dependent User

- The initial CircularProgressIndicator has no explicit “Loading saved frames” announcement (SavedFramesScreen.kt:87–90).
- Successful removal is silent; the row vanishes without a status announcement.
- Inline errors appear after the list and can be easy to miss in linear traversal, even though the primary retry mismatch is visible to sighted users.

### Casey — Distracted Mobile User

- A slow first load presents only a blank surface and spinner, with no explanation of what is being loaded.
- An empty list requires Back plus another navigation step to continue shopping.
- Removing a favorite while interrupted cannot be undone.

## Minor Observations

- SavedFrameCard uses 1dp elevation without the outlineVariant border used by FrameCard, so row separation depends more on shadow than the established border-led system (SavedFramesScreen.kt:221–227; FrameCard.kt:64–71).
- AsyncImage has no explicit loading/error placeholder here; an image failure can leave a quiet 64dp tile, unlike the catalog’s “Photo unavailable” treatment (SavedFramesScreen.kt:234–257; FrameCard.kt:79–100,190–225).
- The Unavailable badge uses a compact 4dp rectangle rather than the system’s usual status-capsule language; this is a polish issue, not a blocker.

## Questions to Consider

1. Should Saved Frames remain a quiet shortlist, or become an active eyewear hub with “Browse frames” and “Try on” paths?
2. For removal, should the list use an Undo Snackbar, the same confirmation dialog as Frame Detail, or a lighter inline action?
3. Should the preference disclaimer be always visible on this list, directly accessible from an info affordance, or should the current contract/tests be revised?
