---
target: the AR screen
total_score: 25
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 4
timestamp: 2026-09-05T12-47-57Z
slug: a-com-eyecare-app-presentation-ar-artryonscreen-kt
---
Method: dual-agent (A: 01a07190-bdfb-75c0-932f-f291ef26821a · B: 01a07190-beff-73c0-afe7-ec63d3b05aed)

## Design Health Score

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of System Status | 3/4 | Capability, permission, loading, searching, tracking, saving, and error states exist, but there is no clear ready state or tracking-quality feedback. |
| 2 | Match System / Real World | 3/4 | “Position your face” and “View frame images” are natural; unsupported-device copy can expose jargon such as “OpenGL ES.” |
| 3 | User Control and Freedom | 3/4 | Back, retry, Settings, image fallback, and variant switching exist, but Settings recovery can strand the user. |
| 4 | Consistency and Standards | 2/4 | The back arrow is announced as “Close,” save copy differs from Frame Detail, and AR removal skips the confirmation used elsewhere. |
| 5 | Error Prevention | 3/4 | Permission gating, typed assets, stale-load protection, and single-flight saving are strong; non-AR variants lack an explicit images-only state. |
| 6 | Recognition Rather Than Recall | 3/4 | Variant names and actions are visible, but the frame name, selected-object identity, and face target are not. |
| 7 | Flexibility and Efficiency | 2/4 | Variant chips help, but there is no inline asset retry, direct selected-frame image route, or clear recovery after permission changes in Settings. |
| 8 | Aesthetic and Minimalist Design | 2/4 | The camera remains dominant, but the lower tray and stacked banners create noise and make the screen feel generic. |
| 9 | Error Recovery | 2/4 | Safe copy and fallbacks exist, but active asset failures have no retry and permanent/transient failures look identical. |
| 10 | Help and Documentation | 2/4 | Permission guidance is good; there is no contextual help for distance, lighting, tilt, or what to do when tracking fails. |
| **Total** |  | **25/40** | **Acceptable, but significant UX improvements are needed before release.** |

## Design Specificity Verdict

**Moderately specific in product behavior, visually category-interchangeable.** The privacy promise, clinic-fit disclaimer, preference-only save semantics, and typed AR asset flow are distinctly Eyecare. The active surface, however, is mostly a generic full-screen camera, black scrim, white buttons, and variant chips. It lacks frame identity, a branded guidance system, and a visual signature beyond cyan selection.

The deterministic detector returned zero findings with exit code 0 and output `[]`. That result has no meaningful signal here: the bundled detector is markup/DOM-oriented and does not scan Kotlin/Jetpack Compose. The independent native inspection reached the installed app's AR fallback state, showing the live camera, “3D preview unavailable,” save/catalog actions, and a variant chip. Source-to-device parity and successful 3D rendering were not verified.

## Overall Impression

The AR screen has a thoughtful state model and unusually good privacy language, but the camera is treated as the product while the product being tried on disappears. The strongest opportunity is to turn it into a guided, identity-rich try-on surface: keep the face/camera experience calm, make the selected frame unmistakable, and expose recovery actions exactly where failure occurs.

## What's Working

- The permission rationale directly answers the highest-trust question: “Nothing is recorded, stored, or sent to the clinic” in [`ArStatusOverlay.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/components/ArStatusOverlay.kt:179).
- The persistent tracking disclaimer, “Visual preview only. Final fit is confirmed at the clinic,” correctly prevents an AR preview from being mistaken for a clinical fit decision.
- Capability, permission, loading, searching, tracking, asset failure, and save feedback are modeled explicitly in [`ArTryOnUiState.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/model/ArTryOnUiState.kt:17). The cyan selected-chip treatment also follows the established Eyecare theme.

## Cognitive Load

- **Pass:** The camera is the single dominant surface; the user’s attention is not split by a conventional app shell.
- **Fail — chunking:** Save/remove, disclaimer, image fallback, and variant selection are stacked in one bottom tray without a clear grouping label.
- **Pass:** Top banners and bottom controls are at least grouped with scrims.
- **Fail — visual hierarchy:** There is no frame title or selected-variant summary, so the user can lose track of what they are looking at.
- **Fail — one thing at a time:** Saving and switching remain available while the model is loading or the face is still being searched.
- **Fail — minimal choices:** With three variants, the active tray can expose Save/Remove + View frame images + three variant choices: five visible actions.
- **Pass:** The selected chip supports working memory.
- **Fail — progressive disclosure:** `VariantChipRow` renders all variants in a horizontal row; larger variant sets are not collapsed or grouped.

Five checklist failures make the active state cognitively heavier than it needs to be.

Decision points exceeding four visible options:

- The active tray exposes Save/Remove, View frame images, and up to three variants at once.
- If the backend returns more than four variants, the AR row continues exposing them horizontally instead of using the picker pattern already used on Frame Detail.

## Emotional Journey

- **Opening:** “Preparing 3D try-on” is reassuring, but the screen immediately loses the frame name and variant context. The user may not know which object is loading.
- **Permission:** This is the strongest moment. The privacy rationale is specific and calm. Returning from Settings after granting permission can leave the user on the same blocked state, which turns reassurance into frustration.
- **Face searching:** “Position your face in the center” and “Lost you for a moment” are human and non-blaming. They are too generic for a first-use camera interaction: there is no move-closer, move-back, tilt, orientation, or lighting guidance.
- **Tracking:** The clinic-fit disclaimer is excellent boundary-setting. It appears only after a face is tracked, so there is no persistent privacy/status signal during the earlier active-camera search.
- **Asset loading/failure:** “Loading 3D frame…” and “Loading this frame’s preview…” can duplicate one another. A failed or non-AR variant is presented similarly to a searching state and offers no inline retry; the user is told to view images instead but is not clearly told whether the failure is permanent or transient.
- **Save:** The spinner, snackbar, and preference disclaimer are clear. Success copy does not name the frame/variant, and AR removal is immediate while Frame Detail confirms removal.
- **Exit:** The top-left control exits cleanly, but its “Close” accessibility label describes a different action from the visible back arrow. The fallback destination also depends on entry point.

## Priority Issues

### [P1] “View frame images” does not reliably open the selected frame

**Why it matters:** `onOpenCatalog` defaults to `onBack` in [`ArTryOnScreen.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt:69). The Frames grid can enter AR directly from a card in [`FrameListScreen.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/frames/FrameListScreen.kt:193), and [`NavGraph.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt:379) does not provide a selected-frame fallback route. A user who chooses the fallback may land on the catalog list rather than the exact frame they were trying on.

**Fix:** Preserve the frame and selected variant in the fallback action and route directly to Frame Detail, or make the callback origin-aware. Keep the selected variant selected when the image surface opens.

**Suggested command:** `$impeccable harden`

### [P1] Face guidance is too vague for a first-use AR interaction

**Why it matters:** The active state only offers “Position your face in the center” and “Lost you for a moment” in [`ArTryOnScreen.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt:207). The face state model only distinguishes `Detected`, `NoFace`, and `Initialising` in [`FaceFrame.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/model/FaceFrame.kt:69). A patient has no way to know whether to move, change lighting, straighten their face, or simply wait.

**Fix:** Add a high-contrast face guide and dynamic, actionable states such as “Move closer,” “Move back,” “Face the camera,” and “Find brighter light.” Provide the same state through accessible announcements, not color or the guide alone.

**Suggested command:** `$impeccable onboard`

### [P1] Active asset failure is a one-way, ambiguous failure

**Why it matters:** The failure copy in [`ArStatusOverlay.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/components/ArStatusOverlay.kt:144) says “3D preview unavailable. View frame images instead,” while the active content has no retry action in [`ArTryOnScreen.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt:244). Permanent “no AR asset” and transient download/renderer failure therefore look the same. A selected variant without a usable model can still leave the user seeing face-position guidance.

**Fix:** Separate “Images only” from “3D failed to load,” add `Try again` for transient failures, and suppress face-position guidance when the selected variant cannot render. Keep the image fallback as a secondary action.

**Suggested command:** `$impeccable harden`

### [P1] Permission recovery can strand the user

**Why it matters:** Permission is cached once with `remember` at [`ArTryOnScreen.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt:96), while the resume effect refreshes saved state only at line 108. After the user taps “Open settings” in [`ArStatusOverlay.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/components/ArStatusOverlay.kt:188) and grants camera access, the screen may still show the blocked state until recreation.

**Fix:** Re-check camera permission on resume, reconcile it with the ViewModel, and show a clear “Continue to try on” transition into loading/searching. Test both a first denial and a Settings grant.

**Suggested command:** `$impeccable harden`

### [P2] The active composition lacks identity and over-stacks secondary actions

**Why it matters:** The bottom scrim combines Save/Remove, a long disclaimer, View frame images, and the variant row in [`ArTryOnScreen.kt`](C:/Users/ironm/AndroidStudioProjects/EyecareV2/app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt:250). There is no frame title or selected-variant summary, unlike the standard Frame Detail top bar. The camera gets all the visual authority while the object being compared becomes anonymous.

**Fix:** Add a compact frame/variant header or a bottom-sheet handle that makes identity persistent. Make variant comparison the primary control, and move the disclaimer/fallback into a calmer secondary grouping. Standardize “Save frame” / “Remove from saved” copy and either confirm removal here or make the immediate action consistent everywhere. Disable or de-emphasize actions that do not make sense while loading.

**Suggested command:** `$impeccable distill` + `$impeccable layout`

## Persona Red Flags

- **First-timer:** After permission, there is no face outline and no distance/lighting guidance. If the selected variant has no usable AR asset, the user can still receive “Position your face in the center,” which sends them toward an impossible task.
- **Privacy-sensitive patient:** The strongest privacy explanation appears before permission and the clinical disclaimer appears only after tracking. During active searching there is no persistent “camera is on; processing stays on this device” signal.
- **Accessibility/low-vision patient:** White controls, chips, and text are composited over a live feed with translucent black/white layers, so contrast varies with the camera scene. The source shows no explicit state-announcement strategy for searching, tracking loss, or asset failure; important changes may be missed by TalkBack users. The arrow’s “Close” label also conflicts with its back behavior.

## Minor Observations

- `ArTryOnScreen` labels the back arrow “Close” even though it navigates back; use “Back” for the visible and spoken action.
- AR says “Save this frame,” while Frame Detail says “Save frame.”
- AR removal is immediate, while Frame Detail uses a “Remove saved frame?” confirmation.
- `CameraPreviewView` captures `onFaceResult` inside `DisposableEffect(lifecycleOwner)`; a changed callback without a lifecycle-owner change can leave the analyzer holding an older callback.
- The `SnackbarHost` uses a fixed `112.dp` bottom offset while the bottom control stack has dynamic height; overlap at larger font scales is not ruled out.
- Existing Compose tests cover overlay components, but not integrated `ArTryOnScreen`, direct fallback routing, large text, dark theme, successful 3D tracking, or the Settings-return path.
- Native inspection reached the installed build’s asset-failure fallback; runtime evidence is still needed for GLB placement, occlusion, camera crop, and contrast over real faces.

## Questions to Consider

- Should “View frame images” always open the exact selected frame and variant, regardless of entry point?
- What if the first active state showed the frame name plus one clear instruction—“Center your face inside the guide”—and delayed secondary actions until the model was ready?
- Should privacy status remain visible throughout Searching, rather than only after tracking begins?
- Is saving intentionally allowed before a face/model is ready? If yes, should the action say “Save this option” so its independence from live try-on is explicit?
