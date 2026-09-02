---
target: the AR screen (ArTryOnScreen.kt)
total_score: 21
max_score: 40
na_heuristics: 
p0_count: 1
p1_count: 2
timestamp: 2026-08-31T05-26-10Z
slug: a-com-eyecare-app-presentation-ar-artryonscreen-kt
---
Method: dual-agent (Assessment A: design review · Assessment B: detector + evidence)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | The permission-rationale card is functionally invisible — a `LaunchedEffect(uiState)` fires the OS camera dialog the instant `PermissionRequired` is reached, before the user can read it (`ArTryOnScreen.kt:91-95`). |
| 2 | Match System / Real World | 3 | Camera + live-overlay metaphor reads clearly; guidance copy ("Position your face in the center") is generic AR boilerplate rather than eyewear-fitting language. |
| 3 | User Control and Freedom | 3 | Back and the "View frame images" escape hatch work everywhere, but save/remove snackbars have no undo action despite `SnackbarHostState` already being wired (`ArViewModel.kt:207-211`). |
| 4 | Consistency and Standards | 1 | `VariantChipRow` and all overlay chrome hand-roll black/white styling instead of the app's documented cyan-selected-chip and theme-token conventions (`VariantChipRow.kt:39-50`). |
| 5 | Error Prevention | 1 | No rationale is actually shown before the OS camera dialog fires — the highest-trust-cost ask in the flow gets zero priming. |
| 6 | Recognition Rather Than Recall | 3 | Status banners keep current state visible (asset loading, disclosure, no-face) rather than requiring memory. |
| 7 | Flexibility and Efficiency | 2 | No accelerators for repeat use — no quick-cycle between variants, no remembered last variant, no shortcut back into the camera after granting permission. |
| 8 | Aesthetic and Minimalist Design | 2 | The bottom scrim stacks Save + disclaimer + Outlined button + full chip row in one dense zone, while two more banners compete at the top of the same live video (`ArTryOnScreen.kt:232-303`). |
| 9 | Error Recovery | 3 | `PermissionDenied`/`Unsupported`/`Error` states all have distinct, actionable copy and a consistent fallback to image preview. |
| 10 | Help and Documentation | 1 | No in-context explanation of how AR try-on works or what happens to the camera feed / face data — no privacy affordance anywhere, despite this being a live front-camera session. |
| **Total** | | **21/40** | **Acceptable — significant improvements needed** |

Both heuristics 7 and 10 genuinely apply to this Operate-mode camera screen and were scored, not waived.

## Design Specificity Verdict

**Generic AR camera screen wearing thin optical-retail branding.**

**LLM assessment:** The screen's architecture (state machine, capability gating, CameraX + face landmarker) is solid, but almost nothing about its presentation is authored for Eyecare or eyewear shopping specifically. All camera-surface chrome is hardcoded `Color.Black`/`Color.White` with manual alpha rather than `MaterialTheme.colorScheme` or DESIGN.md tokens. `VariantChipRow` invents its own black/white selected-chip recipe instead of the "~14% cyan fill / Deep-Lens-Cyan content / ~35%-alpha border" recipe every other segmented control in the app uses — a user arriving from the Frames grid or Appointments tabs won't recognize this as the same product. Copy is functional but interchangeable with any AR camera app ("Position your face in the center," "Camera access needed"); the one genuinely product-specific line is the disclosure banner, "Visual preview only. Final fit is confirmed at the clinic" (`ArStatusOverlay.kt:26`) — the strongest piece of retail-specific authorship on the screen. `SavedFrameDisclaimer` is dropped unmodified from a white-card context into the black camera scrim (`ArTryOnScreen.kt:278-280`), a visible seam showing component reuse without redesign for this surface.

**Deterministic scan:** `detect.mjs --json` against the AR directory returned exit 0 with an empty finding set — but this is **not a clean bill of health**. The detector's scannable-extension list (`.html/.css/.jsx/.tsx/.js/.ts/...`) does not include `.kt`; it examined zero of the 28 Kotlin files in scope and produced no signal either way. Assessment B supplemented this with a manual grep that independently corroborates the LLM's consistency finding: `ArTryOnScreen.kt:148,150,215,224,251,285,289` and `ArStatusOverlay.kt:108,114,136,142` all use raw `Color.Black`/`Color.White` instead of theme roles, and `VariantChipRow.kt:16,41` imports the raw hex constant `CharcoalDark` directly rather than resolving `MaterialTheme.colorScheme.onPrimary/onSurface` — defeating dark/light theme adaptability outright. On the positive side, the same grep found zero hardcoded `.sp` sizes (typography roles are used correctly throughout), the one `Icon` in scope has a proper `contentDescription`, and no touch target is explicitly shrunk below the Material 48dp default. It also surfaced a finding neither review set out to look for: **every user-facing string in `ArTryOnScreen.kt` and `ArStatusOverlay.kt` is a raw literal** — none route through `stringResource(...)`, a systemic, file-wide pattern rather than a one-off.

No live browser/dev-server target exists for this native Compose screen, so browser visualization was correctly skipped; no user-visible overlay is available for this run.

## Overall Impression

The AR try-on screen is functionally the most technically ambitious screen in the app — real-time face tracking, CameraX lifecycle management, and asset-loading state are all handled with real engineering care — but its visual and copy layer was never brought into the Eyecare design system. It looks and reads like a reusable "AR camera" module dropped into the shell rather than a screen the Clear Care Companion identity was extended to cover. The single biggest opportunity is also the single biggest risk: this is the one screen in the app that asks for the user's live camera feed and face, and it's the screen with the weakest trust-building (permission rationale is unreachable), the weakest state differentiation (searching vs. lost-tracking vs. low-light all collapse to one message), and zero privacy framing — exactly the moments where "calm, warm, clear" should matter most and currently doesn't show up at all.

## What's Working

1. **Consistent fallback discipline.** Every non-happy-path state (`PermissionDenied`, `Unsupported`, `Error`) offers the same safety net, "View frame images" (`ArStatusOverlay.kt:94-96,156,165,179,187`), so a user is never stranded — matching PRODUCT.md's "understandable and recoverable" principle.
2. **Correct Android camera-lifecycle behavior.** `CameraPreviewView` binds/unbinds through `DisposableEffect(lifecycleOwner)` (`CameraPreviewView.kt:31-69`) and the screen never intercepts system Back — a real Android-native win most custom camera UIs get wrong.
3. **The one line of copy that is genuinely Eyecare's own.** The AR disclosure banner ("Visual preview only. Final fit is confirmed at the clinic," `ArStatusOverlay.kt:26`) does real trust-calibration work appropriate to a clinic-linked, health-adjacent product.

## Priority Issues

**[P0] Permission rationale is written but never actually seen**
- **Why it matters:** `LaunchedEffect(uiState)` (`ArTryOnScreen.kt:91-95`) fires the OS camera-permission dialog the instant state becomes `PermissionRequired`, before the rationale card in `ArStatusOverlay.kt:154-157` gets a render frame the user can read. This is the highest-trust-cost ask in the entire app — live camera + face — landing with zero in-app framing, which measurably increases reflexive denial, and a denial frequently forces the harder Settings-recovery path on repeat visits.
- **Fix:** Render `PermissionRequired`'s copy as a real, must-tap-to-continue screen with its own "Allow camera access" button that calls `launcher.launch(...)`, instead of auto-launching from a side-effect.
- **Suggested command:** `/impeccable harden`

**[P1] No window-insets handling anywhere on the screen**
- **Why it matters:** `ArTryOnScreen.kt` has zero `WindowInsets`/`statusBarsPadding`/`navigationBarsPadding` usage; the close button uses flat `.padding(8.dp)` from `Alignment.TopStart` (`ArTryOnScreen.kt:143-151`) and the bottom scrim uses flat `.padding(bottom = 24.dp)` (`ArTryOnScreen.kt:252`). This directly violates DESIGN.md's "preserve... Android system insets" and the platform rule that content must never hide behind system bars — on real devices the close button and Save/chip row can render under the status bar or be clipped by the nav bar.
- **Fix:** Apply `.statusBarsPadding()` to the close button and `.navigationBarsPadding()` to the bottom scrim (or wrap the outer `Box` in `Modifier.windowInsetsPadding`).
- **Suggested command:** `/impeccable adapt`

**[P1] The screen abandons the app's theme system for hand-rolled black/white chrome**
- **Why it matters:** Confirmed independently by both assessments. `VariantChipRow.kt:39-50` invents its own selected-chip colors instead of the documented cyan-fill/Deep-Lens-Cyan/cyan-border recipe every other filter/segmented control uses; `VariantChipRow.kt:16,41` imports the raw hex `CharcoalDark` directly rather than `MaterialTheme.colorScheme`, breaking dark-theme adaptability; and `SavedFrameDisclaimer` (tuned for a white card, `primaryContainer.copy(alpha=0.5f)` fill + `onSurfaceVariant` text) is placed unmodified into a 40%-alpha black camera scrim (`ArTryOnScreen.kt:278-280`), producing an unpredictable, likely low-contrast result over a live video feed at the exact moment a disclaimer needs to be legible. This is the screen that will least survive a side-by-side comparison with the Frames grid it's launched from.
- **Fix:** Give the AR screen its own deliberate dark-scrim-appropriate token set (white text/cyan accents on the existing black-scrim family used by the other banners) and route `VariantChipRow` through the app's standard selected-chip recipe instead of custom `Color.White`/`Color.Black` values.
- **Suggested command:** `/impeccable polish`

**[P2] No visual/copy distinction between "never tracked," "lost tracking," and "low light"**
- **Why it matters:** `ArFaceState.NoFace` and `Initialising` both collapse into the same `Searching` state and the identical "Position your face in the center" copy (`ArViewModel.kt:107-128`); there's no state for low light at all. A user who was tracked and then loses tracking (head turn, bad lighting) gets the same generic prompt as someone who never started — the app can't help them diagnose what actually happened, which erodes trust in the tracking quality itself.
- **Fix:** Add a distinct "we lost you, hold still" state separate from cold-start search, and surface luminance-aware guidance ("try a brighter, more even light") when landmarker confidence degrades rather than drops to zero.
- **Suggested command:** `/impeccable clarify`

**[P2] Two independently-offset status banners will collide at larger text scales**
- **Why it matters:** `ArDisclosureBanner` (`top = 64.dp`) and `ArAssetStatusBanner` (`top = 64.dp` or `112.dp`) are two separately-positioned, unbounded-height texts stacked by hardcoded pixel offsets rather than a shared `Column` (`ArTryOnScreen.kt:232-245`). DESIGN.md explicitly commits to layouts that remain readable at increased font scales rather than clipping; at larger scales the disclosure banner will wrap and grow past the fixed gap, visually colliding with the asset-status banner — exactly the messages a user relies on to know whether tracking or loading is working.
- **Fix:** Stack both banners in one `Column` with `Arrangement.spacedBy`, anchored from a single top offset, instead of two absolute positions.
- **Suggested command:** `/impeccable adapt`

## Persona Red Flags

**Jordan (First-Timer):** Hits the P0 issue head-on — no priming screen, straight to the raw OS permission dialog, and the one piece of rationale copy written for exactly this moment (`ArStatusOverlay.kt:156`) is functionally dead code on a first run. Jordan also gets no explanation anywhere in the flow of what happens to the camera feed or face data — no privacy affordance exists at all (heuristic #10).

**Sam (Accessibility-Dependent):** Every dynamic status text — loading, "position your face," disclosure and asset banners — is plain `Text` with no live-region/semantics grouping; the only `contentDescription` in the entire AR package belongs to the close button ("Close," `ArTryOnScreen.kt:150`). A TalkBack user gets no announcement when tracking starts, stops, or an asset falls back from 3D to 2D — the most important state changes on the screen are invisible to them.

**Riley (Stress-Tester):** Rapid variant-chip switching is reasonably guarded at the ViewModel level (`assetLoadJob?.cancel()`, `ArViewModel.kt:412-413`), but it collides visually with the overlapping-banner issue above — quick switching while `Searching`/`Loading` banners are both potentially on screen will produce flicker/overlap that a deliberate stress test surfaces immediately.

## Minor Observations

- Every user-facing string in `ArTryOnScreen.kt` and `ArStatusOverlay.kt` is a raw Kotlin literal, not routed through `stringResource(...)` — a systemic, file-wide pattern (confirmed by manual grep across both files) rather than a single miss.
- The close button is a hand-drawn circular `IconButton` over a custom `Color.Black.copy(alpha=0.3f)` chip rather than a Material `TopAppBar` slot — defensible for an immersive camera UI, but worth naming as intentional rather than incidental.
- `FrameOverlayRenderer`'s wireframe placeholder uses a hardcoded `Color(0xFF29B6F6)` self-documented in a comment as "matches app primary" (`FrameOverlayRenderer.kt:67`) — if Lens Cyan is ever retuned in the theme, this hex silently drifts out of sync since it isn't sourced from the token.
- Save/remove snackbars ("Saved as a preference..." / "Removed from saved frames.", `ArViewModel.kt:207-211`) have no undo action despite `SnackbarHostState` already being wired for one — a low-cost affordance being left unused.

## Questions to Consider

- If the OS permission dialog fires before the user ever sees the in-app rationale card, what was that rationale copy actually written for — and is it worth confirming whether it's currently unreachable on every first run, or only in some code path?
- This screen puts a live front-facing camera feed of the patient's own face on screen with zero in-context privacy or data-handling explanation, for a clinic-linked health-adjacent app whose own product principles call for protecting sensitive data "throughout storage, navigation, and error handling" — was that omission a deliberate scope call, or does it need to be closed before this ships more broadly?
- Every other screen in the app converges on the same cyan-selected-chip, border-led-card, theme-token vocabulary DESIGN.md documents — why does this screen alone reach for hand-rolled black/white chips and raw color literals, and would that choice survive being placed directly next to the Frames grid it launches from?
