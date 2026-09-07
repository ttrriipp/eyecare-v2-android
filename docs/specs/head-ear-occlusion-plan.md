# Implementation Plan: App-Side Head and Ear Occlusion

**Approved specification:** `docs/specs/head-ear-occlusion-spec.md`

**Status:** Approved on 2026-09-06; implementation in progress

## Overview

Add a MediaPipe SelfieSegmenter confidence mask to the existing CameraX face
tracking pipeline, validate its projection with a debug overlay, and then use a
side-head-only mask in the transparent SceneView/Filament composition. The
existing face-mesh occluder and yaw-based temple policy remain the source of
truth for the front of the frame and all fallbacks.

The plan is deliberately risk-first. The mask must be proven visually before
any renderer shader or compositing change is treated as production behavior.

## Architecture decisions

1. **One analyzer owns each camera frame.** `CameraPreviewView` will not call
   two helpers that each consume and close the same `ImageProxy`. A shared
   analyzer will create one rotated `MPImage`, submit it to the face landmarker
   and Image Segmenter with one timestamp, then publish one immutable result.
2. **The mask travels with the face frame.** `FaceFrame` will carry an optional
   `HeadOcclusionMask` so pose, crop, and mask timestamps cannot drift through
   separate UI state channels. The mask is cleared with face tracking loss.
3. **Person mask first, face mesh as the keep-visible boundary.** The first
   model is SelfieSegmenter. A pure policy limits the mask to side-head/ear
   pixels outside the facial region; the full person mask is never drawn as an
   opaque overlay over the glasses.
4. **Confidence output and bounded reuse.** The renderer receives a compact
   confidence buffer. It may reuse the most recent valid mask for at most
   100 ms; stale data selects the existing fallback.
5. **Renderer mechanism is a spike output.** The integration will use the
   smallest reusable Filament path that can sample the mask (for example a
   depth-only screen mesh or mask-aware material). The choice is not hidden by
   arbitrary offsets and must be validated on the POCO before hardening.
6. **No backend or asset contract change.** The GLB remains unchanged. Both
   bundled and downloaded models use the same mask/fallback policy.

## Dependency graph

```text
SelfieSegmenter model asset + helper
             |
Shared CameraX analyzer + immutable FaceFrame mask
             |
Pure mask mapping and activation policy
             |
Debug mask overlay and POCO visual calibration
             |
Filament mask-aware renderer adapter
             |
Bundled/downloaded integration + fallback/performance gate
             |
Conditional hardening + final evidence
```

## Implementation phases

### Phase A — camera and pure foundation

1. Add the reviewed SelfieSegmenter model asset and segmenter boundary.
2. Refactor camera analysis to publish one face/mask result per timestamp.
3. Add immutable mask data, coordinate mapping, side-region policy, and tests.

Checkpoint A: focused tests, full unit suite, `assembleDebug`, and
`lintDebug` pass. No production renderer behavior changes yet.

### Phase B — visual proof and renderer spike

4. Add a debug-only mask overlay and validate crop, mirroring, ear coverage,
   confidence threshold, and stale-mask behavior on the POCO X8 Pro.
5. Implement one reusable mask-aware SceneView/Filament adapter and prove the
   temple is hidden only in the side-head region.
6. Integrate the adapter into both model-loading paths and restore the yaw
   fallback immediately when mask/renderer state is invalid.

Checkpoint B: automated checks plus the mandatory physical-device gate. Stop
for the user's visual/FPS result before hardening.

### Phase C — conditional hardening

7. Resolve only measured gate findings: lifecycle, mask reuse, performance,
   thermal behavior, and repeated screen entry.
8. Record device evidence and reconcile the parent 3D try-on documentation.

## Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Segmenter and face landmarker consume different frames | Temple pops or trails | One analyzer, one timestamp, immutable paired result |
| Camera rotation/mirror/crop mismatch | Mask hides the wrong pixels | Pure mapper fixtures plus debug overlay before renderer work |
| Full person mask hides the front frame | Obvious broken try-on | Side-region policy and face-mesh keep-visible boundary |
| Segmenter is too slow beside face tracking | Low FPS/thermal throttling | SelfieSegmenter first; latest-frame async; measured every-other-frame fallback |
| Hair/lighting creates mask holes | Temple flash at the ear | Confidence threshold, short bounded reuse, yaw fallback |
| Filament layer order is wrong | Mask has no effect or hides camera | Device spike with no arbitrary offsets; retain fallback |
| Model asset license/size is unsuitable | Distribution or startup risk | Review model card/license and APK impact before implementation |

## Verification commands

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

## Manual checkpoint

On the POCO X8 Pro, test front view, 30-degree and 60-degree yaw in both
directions, side profile, pitch, fast turns, hair/lighting changes, face loss
and reacquisition, repeated screen entry, and a 60-second motion run. Record
mask alignment, temple flashes, front-frame visibility, median FPS, crashes,
renderer resets, and thermal behavior.

The feature is not declared complete until the user confirms the visual result.

## Review gate

The task breakdown in `head-ear-occlusion-tasks.md` must be reviewed before
implementation begins. Tasks 1–6 are sequential and may proceed through
automated checkpoints after approval. Tasks 7–8 remain conditional on the
physical-device result.
