# Implementation Plan: Reservation-First 3D Frame Try-On

Status: Approved by stakeholder on 2026-08-15
Phase: Implement
Approved specification: `docs/specs/3d-frame-try-on-spec.md`
Created: 2026-08-15

## Overview

Implement the approved feature as vertical slices that fail fast on the
highest-risk dependency: rendering and tracking the existing measured
round-frame GLB over CameraX on the POCO X8 Pro. Backend asset delivery,
reservation-policy changes, and the second and third models begin only after
that local feasibility slice passes.

No additional Meshy or Blender work is required before the first slice. Manual
calibration happens after the GLB is visible over the live camera because its
axes, anchor, scale, and perceived physical similarity cannot be finalized in
isolation.

## Current Baseline

- CameraX already provides a mirrored front-camera preview.
- MediaPipe analyzes one face, but the helper derives only normalized 2D
  bridge/temple coordinates and roll.
- `FrameOverlayRenderer.kt` uses Coil and Canvas to draw a flat bitmap.
- `ArTryOnScreen.kt` treats `ar_asset_reference` as an image URL, while the
  API example supplies a `.usdz` reference.
- `round_frame_textured.glb` is bundled, structurally valid, measured, and
  ready for the renderer spike.
- `MAX_RESERVATION_ITEMS` is five in Android and the documented backend
  contract.
- Backend code and the authenticated staff/admin UI are outside this workspace.

## Architecture Decisions

### Camera composition

Keep CameraX as the visible preview and place a transparent SceneView scene
above it. Use plain `SceneView`, not `ARSceneView`: MediaPipe provides face
pose, while ARCore would introduce an unrelated camera owner and lifecycle.

```text
Compose controls, guidance, disclosure, and reservation action
    -> transparent SceneView / Filament model layer
        -> existing CameraX PreviewView
```

### Renderer dependency

Start with `io.github.sceneview:sceneview:4.18.0`, as approved. Maven Central
confirms that release imports Compose BOM `2026.05.01`, matching the project.
The live SceneView site currently documents newer APIs, so compile a minimal
model scene against the selected artifact before using its APIs elsewhere.
Changing the version requires a spec decision; do not add `arsceneview` or
ARCore as a workaround.

### Tracking boundary

`FaceLandmarkerHelper` owns MediaPipe configuration and lifecycle but not
renderer state. It emits an immutable pose value containing the facial
transformation matrix and only the landmarks needed for scale and guidance.

A pure `FacePoseMapper` converts MediaPipe coordinates into renderer position,
rotation, and scale. A separate `PoseStabilizer` smooths that pose. Neither
depends on Compose, CameraX, MediaPipe result objects, nor SceneView so the math
can be unit tested deterministically.

### Asset identity and calibration

The first slice uses an explicit bundled descriptor for the measured round
frame and does not depend on the ambiguous legacy reference.

After the gate, add typed `ArAsset` and `ArCalibration` DTO/domain models.
Every loaded model stays bound to one variant ID, version, and checksum.
Switching variants changes identity first, then shows loading until that exact
model is ready; a stale model must never be reservable as the new variant.

### Remote loading and cache

The feasibility loader resolves a trusted path under `src/main/assets`. The
later remote loader:

1. receives only the typed, ready GLB contract;
2. checks HTTPS and the declared 10 MiB ceiling;
3. downloads to a temporary app-cache file;
4. verifies actual size and SHA-256;
5. atomically promotes the valid file; and
6. exposes only the verified local asset to the renderer.

Model binaries, tokens, and face data do not enter Room.

### UI and reservation boundaries

Consolidate the current permission, face, variant, and asset flows into one
immutable `ArTryOnUiState`. The composable owns permission launchers and
lifecycle-bound views; the view model coordinates product and reservation
intent.

Add `onReserve(frameId, variantId)` to the try-on navigation boundary and route
to the existing `CreateFrameReservation` flow. Do not duplicate appointment
or inventory rules in AR code.

Change the reservation maximum from five to three only when backend validation
and Android ship together. Android continues reading and showing legacy
four/five-item reservations and allows removal while preventing additions.

## Dependency Graph

```text
approved spec
    -> renderer dependency/build proof
        -> static bundled GLB
            -> MediaPipe transformation matrix
                -> pose mapping + smoothing
                    -> live one-frame slice
                        -> POCO feasibility gate
                            | PASS
                            |   -> typed AR API contract
                            |       -> secure remote cache
                            |           -> multi-variant switching
                            |               -> reservation journey
                            |                   -> final study
                            |
                            ` FAIL
                                -> polished 2D/2.5D fallback
                                    -> reservation journey
                                        -> fallback study/report

backend after PASS:
typed contract -> upload validation/storage -> immutable publication
                                      `------> Android remote cache

reservation policy:
backend max 3 <---- coordinated release ----> Android max 3
```

## Implementation Order

### Stage 1: Renderer dependency proof

Add the pinned SceneView catalog alias and application dependency. Create the
smallest isolated Compose renderer that loads
`models/round_frame_textured.glb`, supplies a camera and light, and exposes a
useful loading/error signal. Do not replace current AR behavior yet.

Checkpoint:

- dependency resolution and `assembleDebug` succeed;
- the existing unit suite still passes; and
- the bundled GLB appears on an isolated device surface without CameraX.

Stop and revise the dependency decision if it cannot compile with AGP 9.2.1,
Kotlin 2.3.0, and the current Compose BOM.

### Stage 2: Deterministic pose foundation

Enable MediaPipe facial transformation matrices and introduce an immutable raw
pose model. Extract coordinate conversion and calibration into pure functions.
Add bounded smoothing with reset after no-face or a large timestamp gap.

Checkpoint:

- fixtures prove neutral pose, translation, yaw, pitch, roll, mirroring,
  calibration scale, non-finite rejection, and stabilizer reset;
- the MediaPipe helper closes deterministically; and
- no renderer type crosses into tracking models or tests.

### Stage 3: One-frame live vertical slice

Composite the transparent renderer above `CameraPreviewView`. Drive one
remembered model instance from stabilized pose. Keep the round measurements and
provisional scale in an explicit feasibility descriptor. Remove per-frame debug
logging from the measured path.

This is the first manual step:

- install on the POCO X8 Pro;
- adjust axis rotation and bridge anchor until the front view is centered;
- compare width, height, and silhouette with the physical frame; and
- record final calibration rather than hiding corrections in UI code.

Checkpoint:

- camera and model mirror consistently;
- the model stays attached through the specified motion script;
- repeated entry and background/foreground do not duplicate resources; and
- renderer failure preserves image browsing.

### Stage 4: Capability, states, and fallback

Introduce unified UI state and capability evaluation. Add distinct permission,
unsupported, loading, searching, tracking, and recoverable-error surfaces. Add
the non-clinical-fit disclosure and image fallback. Keep this stage limited to
the bundled asset.

Checkpoint:

- state and capability unit tests pass;
- Compose tests cover disclosure and recovery;
- unsupported devices retain catalog and reservation access; and
- unit tests, lint, and debug build pass.

### Stage 5: POCO X8 Pro feasibility gate

Run the approved 60-second script with the calibrated model. Record build type,
GLB checksum/version, load time, median FPS, crashes, renderer resets, and
qualitative attachment observations.

The five-focused-day or 20%-of-remaining-time cutoff applies across Stages 1–5.
Review the evidence and follow the already-approved branch: remote 3D on pass,
or stop 3D expansion and execute the fallback on fail.

### Stage 6A: Typed remote contract (PASS)

Coordinate the additive `ar` response with the backend. Add Kotlinx
Serialization DTOs, domain models, validation/mapping, cache compatibility, and
MockWebServer tests. Android ignores the legacy reference for GLB loading.

Checkpoint:

- ready and null contract examples decode and map at the repository boundary;
- `ar = null` preserves image behavior;
- legacy fields remain harmless during compatibility; and
- backend-shaped MockWebServer responses pass.

### Stage 7A: Secure remote loading (PASS)

Implement bounded download, checksum verification, atomic cache, and
last-known-good behavior behind a domain-facing repository. Do not expose
OkHttp or file details to the UI.

Checkpoint:

- tests cover oversize, checksum mismatch, interruption, retry, atomic replace,
  and corrupted-cache eviction;
- a remote asset loads from network and then cache; and
- no partial or unverified file reaches Filament.

### Stage 8A: Backend upload/publication (external PASS stage)

In the backend repository, implement staff authorization, quarantine, GLB
validation, metadata, review, immutable publication, replacement, disablement,
auditing, and last-known-good retention.

This needs the storage/CDN decision and named Asset Steward/reviewer. It may run
in parallel with Stage 7A only after the request/response contract is frozen.

Checkpoint:

- backend security and contract tests pass;
- patients see only ready assets;
- failed replacement preserves the previous version; and
- an authorized non-developer can follow the publication checklist.

### Stage 9A: Multi-variant reservation journey (PASS)

Select and approve the other two physical variants, enable identity-safe model
switching, and add **Reserve this frame** to tracking and fallback states.

Coordinate the maximum of three across backend validation, Android domain
rules, copy, and tests. Preserve legacy reservation display/removal.

Checkpoint:

- displayed model, label, and reserved variant ID always agree;
- switching never creates a stale-model reservation window;
- server `422` remains authoritative; and
- full reservation regression tests and Android quality commands pass.

### Stage 6B: Polished fallback (FAIL)

Stop adding 3D/remote-model infrastructure. Improve the existing CameraX and
MediaPipe 2D/2.5D overlay within the remaining time and connect the same
reservation action. Document the measured failure and do not call the fallback
3D.

### Stage 10: Capstone validation

Run all automated checks, the available device matrix, blind physical-frame
matching, stability ratings, and before/after reservation-confidence study.
Archive only non-face results allowed by project privacy rules.

Checkpoint:

- evidence is tabulated against every approved success criterion;
- unsupported and failure paths are demonstrated;
- unverified compatibility claims stay labeled; and
- the report states that final fitting happens at the clinic.

## Verification Cadence

Every increment ends with:

```powershell
.\gradlew assembleDebug
```

Every stage checkpoint runs focused tests followed by:

```powershell
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
```

Use `ktlintFormat` only intentionally and review its diff so unrelated user
changes are not folded into the feature.

## Expected File Areas

These are boundaries, not permission to edit every file at once. The Tasks
phase will split them into changes of about five files or fewer.

| Area | Likely files/directories |
|---|---|
| Dependency proof | `gradle/libs.versions.toml`, `app/build.gradle.kts` |
| Renderer | `presentation/ar/rendering/`, `ArTryOnScreen.kt` |
| Tracking | `FaceLandmarkerHelper.kt`, `presentation/ar/tracking/`, `model/` |
| State/capability | `ArViewModel.kt`, `presentation/ar/capability/` |
| Typed contract | `FrameDtos.kt`, `Frame.kt`, `FrameRepositoryImpl.kt` |
| Remote cache | new data/domain asset repository files and DI binding |
| Reservation action | `ArTryOnScreen.kt`, `NavGraph.kt`, existing flow |
| Reservation limit | `FrameReservation.kt`, reservation view model/UI/tests |
| Verification | corresponding `src/test` and `src/androidTest` packages |
| Contracts | `docs/API_CONTRACT.md` and backend project documentation |

## Parallelization

Before the gate, renderer and pose work stay sequential because each validates
the next dependency and shares the AR surface.

After a pass and frozen contract, these can proceed independently:

- backend upload/storage and Android remote cache;
- production of the second and third assets; and
- backend and Android reservation-limit compatibility tests.

Integration, device testing, and the user study remain sequential.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| SceneView version/API mismatch | High | Compile the 4.18.0 spike first; require approval for a version change. |
| Coordinate mismatch | High | Pure matrix fixtures, explicit axes, mirror tests, recorded calibration. |
| Wrong AI proportions/origin | High | Measurements, visible calibration, independent physical review. |
| Slow/heavy model | High | One instance, 5 MiB target, reduced logs, early gate, planned fallback. |
| Native resource leak | High | Lifecycle ownership and repeated-entry device tests. |
| Wrong variant reserved | High | Bind state to variant/version/checksum; disable reserve while loading. |
| Backend unavailable here | Medium | Local slice stays demonstrable; remote work is an external stage. |
| Only a powerful phone exists | Medium | Treat POCO as reference, preserve fallback, avoid minimum claims. |
| Malicious/malformed GLB | High | Quarantine and bounds checks; Android size/checksum verification. |
| Capstone scope expands | High | Enforce the Stage 5 cutoff and stop expansion on failure. |

## Human Participation Schedule

No manual action is needed before the Tasks phase or dependency proof.

1. **Stage 3:** use the POCO and physical round frame for visual calibration.
2. **Stage 5:** review the recorded pass/fail evidence.
3. **Before Stage 8A:** name the steward/reviewer and choose backend storage.
4. **Before Stage 9A:** identify the other two catalog variants.
5. **Stage 10:** recruit participants with the physical products available.

## Plan Approval Gate

After stakeholder approval, create a separate Tasks document. Each task must fit
one focused session, touch about five files or fewer, define acceptance criteria
and verification, and leave the project building. Do not implement application
code until the Tasks document is also approved.

## Sources

- Approved spec: `docs/specs/3d-frame-try-on-spec.md`
- SceneView nodes/lifecycle: <https://sceneview.github.io/docs/nodes/>
- SceneView 4.18.0 release:
  <https://github.com/sceneview/sceneview/releases/tag/v4.18.0>
- SceneView 4.18.0 Maven metadata:
  <https://central.sonatype.com/artifact/io.github.sceneview/sceneview/4.18.0>
- MediaPipe Face Landmarker:
  <https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker/android>
- MediaPipe option builder:
  <https://developers.google.com/edge/api/mediapipe/java/com/google/mediapipe/tasks/vision/facelandmarker/FaceLandmarker.FaceLandmarkerOptions.Builder>
