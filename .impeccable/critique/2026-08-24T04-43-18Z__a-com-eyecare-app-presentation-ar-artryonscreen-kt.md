---
target: the AR try-on screen
total_score: 22
max_score: 40
na_heuristics: 
p0_count: 2
p1_count: 3
timestamp: 2026-08-24T04-43-18Z
slug: a-com-eyecare-app-presentation-ar-artryonscreen-kt
---
Method: dual-agent (A: general-purpose design-review agent · B: general-purpose detector/evidence agent)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Reserve button vanishes entirely instead of disabling while the 3D asset loads (ArTryOnScreen.kt:226-238); the asset status banner's top offset jumps 64dp→112dp depending on face presence. |
| 2 | Match System / Real World | 3 | Clinic-appropriate disclosure copy, but implementation-facing strings leak through ("Preparing 3D frame…", ArStatusOverlay.kt:127). |
| 3 | User Control and Freedom | 3 | "View frame images" escape hatch exists in nearly every state, but the exit icon is a back-arrow glyph labeled "Close" (ArTryOnScreen.kt:127). |
| 4 | Consistency and Standards | 1 | Bare `Button`/`OutlinedButton`/`FilterChip` with no explicit size — confirmed by B: none of the six interactive controls in the AR files set `.size()`/`fillMaxWidth()`, bypassing the app's own `AppointmentPrimaryButton`/`AppointmentOutlinedButton` (52dp, full-width) used everywhere else. |
| 5 | Error Prevention | 3 | GLB validation and an `assetGeneration` counter guard against stale async loads (ArViewModel.kt:232-254; FrameModelRenderer.kt:439-464). |
| 6 | Recognition Rather Than Recall | 2 | No persistent face-alignment guide/reticle — only a text bubble ("Position your face in the center"). |
| 7 | Flexibility and Efficiency | 2 | Single fixed interaction path; no remembered last variant, no shortcuts. |
| 8 | Aesthetic and Minimalist Design | 1 | Bottom cluster stacks Reserve + Outlined button + variant chips with no `Arrangement.spacedBy`; confirmed by B: six different overlay alpha values (0.3/0.4/0.5/0.62/0.7/0.72) used for visually-similar scrim purposes across the same feature. |
| 9 | Error Recovery | 4 | Unsupported/Error/PermissionDenied states each carry a specific cause and a one-tap recovery action, and are test-verified (ArTryOnScreenTest.kt:24-65). |
| 10 | Help and Documentation | 1 | No contextual guidance beyond one line of permission rationale; a failed camera bind produces zero user-facing message (CameraPreviewView.kt:55-61). |
| **Total** | | **22/40** | **Acceptable — significant improvements needed before users are happy** |

## Design Specificity Verdict

**LLM assessment**: The state layer is genuinely built for a real optical AR pipeline — the sealed `ArTryOnUiState` hierarchy, GLB validation, and stale-write guarding show real domain investment. But the surface the patient actually sees is generic camera-filter chrome: black translucent pill banners, a plain text bubble instead of a face-alignment guide, a crude two-rounded-rects-and-a-bridge wireframe fallback, and unstyled default Material buttons. Strip the word "frame" out of the copy and this reads as a generic face-filter demo, not "an optical clinic fitting a physical product on your face" — nothing in the composition signals eyewear specifically the way the disclosure copy does at the text layer alone.

**Deterministic scan**: The bundled detector (`detect.mjs`) has **zero Kotlin/Compose support** — `SCANNABLE_EXTENSIONS` covers only HTML/CSS/JS/JSX/TSX/Vue/Svelte/Astro, so a directory scan of `presentation/ar/` silently walks past every `.kt` file, and even a forced single-file regex pass against `ArTryOnScreen.kt` matched nothing (exit 0, `[]`). This is a coverage gap, not a clean bill of health — treat the `[]` result as "not measured," not "no issues." In its place, a manual grep-based evidence sweep confirmed several of the LLM assessment's claims with exact citations:
- **Zero window-inset handling** anywhere in the 27-file AR tree (`grep -rn "WindowInsets\|safeDrawing\|systemBars\|statusBars\|navigationBars"` → no matches) — the close button and bottom action cluster rely entirely on fixed dp offsets inside a `fillMaxSize()` Box.
- **The contrast bug is real and precise**: `VariantChipRow.kt:40-49` sets unselected chips to `containerColor = Color.White.copy(alpha = 0.3f)` with `labelColor = Color.White` — white label text on a near-white translucent fill.
- **The icon/label mismatch is the only icon in the entire feature**: exactly one `Icon(...)` call exists in all 27 files, `Icons.AutoMirrored.Filled.ArrowBack` with `contentDescription = "Close"`.
- All 8 `ArTryOnUiState` variants are exhaustively handled between `ArStatusOverlay.toStatusCopy()` and `ActiveTryOnContentState.toActiveTryOnContentState()` — no state falls through unhandled, which is a real strength the LLM pass under-credited.
- Test coverage has real gaps: `PermissionDenied`, the top-level `ArTryOnScreen` composable itself, the close button, and `VariantChipRow` are never exercised by `ArTryOnScreenTest.kt`.

**Visual overlays**: Not applicable — this is a native Android Compose screen with no dev server or URL, so no browser injection/overlay pass was possible. This critique is source-grounded, not screenshot-grounded; treat any layout-appearance claim as an inference from code, not an observed render.

## Overall Impression

The AR try-on screen sits on a genuinely solid engineering foundation (exhaustive state modeling, graceful three-tier degradation from 3D model → 2D overlay → wireframe → catalog exit, stale-write protection) wrapped in visual chrome that was never brought up to the rest of the app's standard. It reads as though the state machine got a design pass and the pixels didn't: default-sized buttons instead of the app's own pill components, zero window-inset handling, a real contrast failure on unselected variant chips, and a disclosure banner whose visibility is coupled to noisy per-frame face detection. The single biggest opportunity is also the cheapest fix: swap the ad hoc `Button`/`OutlinedButton` pair for the app's existing `AppointmentPrimaryButton`/`AppointmentOutlinedButton` components and add window-insets padding — that alone resolves the two P0s and meaningfully improves three of the four worst-scoring heuristics.

## What's Working

- **Exhaustive state modeling**: all 8 `ArTryOnUiState` variants (CheckingCapability, PermissionRequired, PermissionDenied, Loading, Searching, Tracking, Unsupported, Error) resolve to explicit, distinct copy or rendering — nothing falls through silently, and the mapping is pinned by tests (ArTryOnScreenTest.kt:24-117).
- **Graceful degradation chain**: GLB 3D model → 2D image overlay (FrameOverlayRenderer) → hand-drawn wireframe fallback → "View frame images" catalog exit. The user is never fully blocked from completing the underlying task even when AR fails end-to-end.
- **Stale-write protection**: `assetGeneration` (ArViewModel.kt:65, 232-254) correctly prevents a slow async asset load from clobbering a newer variant selection when a patient taps through chips quickly.

## Priority Issues

**[P0] Missing window insets (edge-to-edge violation)**
- Why it matters: DESIGN.md's Android platform reference requires edge-to-edge content to respect window insets so nothing hides behind system bars or a display cutout. A grep across all 27 files in `presentation/ar/` found zero uses of `WindowInsets`/`safeDrawing`/`statusBars`/`navigationBars`. The close button (ArTryOnScreen.kt:120-128) and the bottom action cluster (lines 217-222) are positioned with fixed `.padding(8.dp)`/alignment only, inside a plain `fillMaxSize()` Box — on a gesture-nav device or a cutout phone, the close button or Reserve/variant row can sit under system chrome or become hard to reach.
- Fix: wrap the close `IconButton` in `Modifier.windowInsetsPadding(WindowInsets.statusBars)` and the bottom `Box` in `Modifier.windowInsetsPadding(WindowInsets.navigationBars)`.
- Suggested command: `/impeccable harden`

**[P0] Bottom action cluster breaks the app's own button system**
- Why it matters: DESIGN.md specifies primary actions as full-width, pill-shaped, 48-52dp, with "one obvious primary action per task step." The bottom `Column` (ArTryOnScreen.kt:224-259) has no `Arrangement.spacedBy`, and the `Button`/`OutlinedButton` at lines 230-250 have no `fillMaxWidth()`/explicit height — confirmed by evidence sweep: none of the six interactive controls in the AR files (Button, OutlinedButton, TextButton, FilterChip ×3 contexts) set an explicit size, all falling back to small Material defaults. This is also why Reserve and "View frame images" render at equal visual weight, and why the whole cluster's height jumps when Reserve appears/disappears. The rest of the app already solves this with `AppointmentPrimaryButton`/`AppointmentOutlinedButton` (AppointmentActionButton.kt:36-71).
- Fix: replace the raw `Button`/`OutlinedButton` calls with the existing `AppointmentPrimaryButton`/`AppointmentOutlinedButton` components and add `Arrangement.spacedBy(12.dp)` to the Column.
- Suggested command: `/impeccable layout`

**[P1] Unselected variant chips have a real contrast failure**
- Why it matters: `VariantChipRow.kt:39-44` sets unselected chips to `containerColor = Color.White.copy(alpha = 0.3f)` with `labelColor = Color.White` — white text on a near-white translucent fill. This is a legibility failure independent of what's behind the camera feed, and it disproportionately hits low-vision users (a hard contrast fail, not just "hard to read").
- Fix: switch the unselected chip container to a dark scrim (`Color.Black.copy(alpha = 0.4f)`), matching the pattern already used by `ArDisclosureBanner`/`ArAssetStatusBanner` elsewhere in the same file.
- Suggested command: `/impeccable audit`

**[P1] Camera bind failures are silently swallowed**
- Why it matters: `CameraPreviewView.kt:55-61` wraps `bindToLifecycle` in `runCatching { }.onFailure { Log.e(...) }` and never surfaces the failure to the ViewModel or UI. If the front camera is unavailable (in use elsewhere, hardware fault), the patient sees a frozen or black screen with AR chrome floating over nothing and zero explanation — a direct violation of "help users recognize, diagnose, and recover from errors."
- Fix: plumb a failure callback from `CameraPreviewView` up into `ArViewModel` so a bind failure transitions the screen into `ArTryOnUiState.Error` with real copy.
- Suggested command: `/impeccable harden`

**[P1] Disclosure banner and status banner reflow with tracking noise**
- Why it matters: `ArDisclosureBanner` only renders when `state.phase == Tracking && state.face != null` (ArTryOnScreen.kt:202), and `ArAssetStatusBanner`'s top padding hard-switches between 64dp and 112dp based on `state.face != null` (line 214). Face detection is inherently noisy frame-to-frame, so the legal/expectation-setting disclosure ("Visual preview only. Final fit is confirmed at the clinic.") can flicker on and off, and the asset banner visibly jumps position — this undermines both the compliance intent of the disclosure and the "calm, reassuring" design north star.
- Fix: keep the disclosure visible for the entire active session (Loading/Searching/Tracking), and give both banners a fixed vertical slot regardless of momentary face-detection state.
- Suggested command: `/impeccable polish`

## Persona Red Flags

**Jordan (First-Timer)**: The OS camera-permission dialog fires immediately on mount via `LaunchedEffect(uiState)` (ArTryOnScreen.kt:83-87) with no priming screen of Eyecare's own explaining why AR needs the camera before the system interrupt appears. If Jordan reflexively denies it, the very next thing they see is the `PermissionDenied` copy — there's no gentler on-ramp between "screen opens" and "OS asks for a sensitive permission."

**Sam (Accessibility-Dependent)**: None of `ArDisclosureBanner`, `ArAssetStatusBanner`, or the "Position your face in the center" text carry any live-region/semantics wiring, so a TalkBack user gets no announcement when the asset state flips from "Loading 3D frame…" to "3D preview unavailable" unless they happen to already be focused on that node. Separately, the white-label-on-white-30%-fill unselected chip (VariantChipRow.kt:39-44) is a hard contrast fail regardless of assistive technology — this is a low-vision blocker, not just an inconvenience.

**Casey (Distracted Mobile User)**: Reserve and "View frame images" sit with no gap between them at small default (~40dp) size rather than the app's normal full-width 52dp pills (ArTryOnScreen.kt:230-250). A one-handed, in-motion tap is meaningfully more likely to miss or hit the wrong button than it would against the rest of the app's action buttons — and because Reserve disappears entirely while the asset loads, Casey may tap where Reserve used to be and hit nothing, or hit "View frame images" by mistake right after it shifts up to fill the gap.

## Minor Observations

- Six different overlay alpha values (0.3, 0.4, 0.5, 0.62, 0.7, 0.72) are used for visually-similar dark/light scrim purposes across ArTryOnScreen.kt, ArStatusOverlay.kt, and FrameModelRenderer.kt — no single scrim token, just ad hoc literals per call site.
- Several dp values fall outside the app's documented 4/8/12/16/20/24/32dp spacing scale: `vertical = 10.dp` (×3, ArTryOnScreen.kt:195, ArStatusOverlay.kt:112/140), `vertical = 28.dp` (ArStatusOverlay.kt:64), `vertical = 14.dp` (FrameModelRenderer.kt:488), and the 64dp/112dp banner offsets already flagged above.
- The close icon (`Icons.AutoMirrored.Filled.ArrowBack`) is labeled `contentDescription = "Close"` — a back-arrow glyph performing a "close AR session" action; consider `Icons.Filled.Close` so the signifier matches the behavior.
- `FrameOverlayRenderer`'s no-bitmap wireframe fallback (two rounded rects plus a bridge line) reads as a dev placeholder if it's on screen for any real duration on a slow connection.
- Long variant names have no `maxLines`/ellipsis handling in `VariantChipRow.kt:37` and could produce irregularly-sized chips.
- Implementation-facing copy ("Preparing 3D frame…", "Loading 3D frame…") is exposed directly to patients rather than task-facing language.
- Test coverage gaps: `PermissionDenied`, the top-level `ArTryOnScreen` composable itself, the close button's click/label, and `VariantChipRow` are never exercised by `ArTryOnScreenTest.kt` — only sub-components are rendered directly in tests.

## Questions to Consider

- If the design system's rule for this screen category is "one obvious primary action," why does Reserve — the only action with real business consequence — carry the same visual weight as a secondary escape hatch?
- The disclosure banner exists for a reason (legal/expectation-setting) — should its visibility really be coupled to per-frame face-tracking noise, or does that turn a compliance requirement into a coin flip?
- What would this screen look like if a face-alignment guide and a branded loading treatment were treated as first-class requirements for "trying on eyewear," instead of a generic camera-filter text bubble and wireframe placeholder?
