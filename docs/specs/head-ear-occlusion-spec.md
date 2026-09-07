# Spec: App-Side Head and Ear Occlusion for 3D Try-On

**Status:** Approved on 2026-09-06

**Parent feature:** `docs/specs/3d-frame-try-on-spec.md`

**Related implementation:** `docs/specs/face-mesh-depth-occlusion-spec.md`

**Scope owner:** Android application; no backend or GLB contract change

## Objective

Improve the front-camera 3D eyewear preview so the temple arms disappear behind
the user's side head and ear at three-quarter and side angles. The front frame,
bridge, and lenses must remain visible. This is a visual preview improvement;
it is not a clinical fit measurement or a guarantee that the physical frame
will fit.

The current face-mesh depth occluder covers the facial surface but cannot cover
ears or the side/back of the head. This extension adds an app-side foreground
mask and a mask-aware render/composite path while retaining the existing yaw
temple policy as a fallback.

## Assumptions and product decisions

- Camera ownership remains CameraX `ImageAnalysis` with
  `STRATEGY_KEEP_ONLY_LATEST`.
- Face pose and facial-surface depth remain authoritative for the front of the
  frame; the new mask is limited to side-head/ear occlusion.
- The existing MediaPipe Tasks Vision dependency remains the runtime boundary.
  The implementation may add a reviewed MediaPipe Image Segmenter model asset,
  but must not add a second camera session.
- Segmentation data is copied into an immutable, short-lived app model and is
  never persisted, uploaded, logged, or sent to analytics.
- The existing `TempleVisibilityPolicy` remains active whenever the mask is
  missing, stale, invalid, or rejected by the renderer.
- No patient-facing calibration control is added in this slice.
- The work must preserve image preview, frame saving, reservation, and the
  existing **Visual preview only. Final fit is confirmed at the clinic.**
  disclosure.

## User-visible behavior

### Front view

- The complete front frame, bridge, lenses, and both temples remain visible.
- The head mask must not create a visible silhouette, halo, or dark cutout.

### Three-quarter and side view

- The temple arm that is behind the head/ear is hidden by the camera image.
- The near-side rim and the visible portion of the near temple remain visible.
- A short transition is smoothed without leaving a one-frame temple flash.

### Tracking loss or unsupported devices

- The mask is disabled immediately when its timestamp is stale or tracking is
  lost.
- The current yaw-based temple visibility and face-mesh occlusion continue to
  provide the fallback.
- Occlusion failure never blocks the 3D preview, image preview, or reservation.

## Proposed technical design

The existing composition is a CameraX `PreviewView` with a transparent
SceneView/Filament overlay. The implementation must add a mask-aware layer in
that composition rather than attempting to solve ear coverage in the GLB.

1. Extend the camera analysis boundary with a MediaPipe Image Segmenter result
   for the current frame. Keep `FaceLandmarkerHelper` and the segmenter adapter
   responsible for framework objects.
2. Convert the result into a compact `HeadOcclusionMask` containing image
   dimensions, timestamp, confidence/coverage metadata, and a reusable mask
   buffer. Apply the same rotation, front-camera mirroring, and aspect-fill
   mapping used by the face and model paths.
3. Derive a side-head/ear region from the mask plus the current face pose. Do
   not use the full person mask as an opaque layer: that would incorrectly hide
   the frame in front of the eyes and nose.
4. Feed the region to a renderer/compositor adapter. The adapter may use a
   segmentation-textured depth-only screen mesh or a Filament mask-aware
   material; the selected mechanism must be proven on the target device before
   hardening.
5. Gate the mask by freshness, pose availability, confidence, and renderer
   readiness. When any gate fails, restore the existing temple policy.

The exact MediaPipe model file and Filament masking mechanism are technical
spike outputs. They must not be hidden behind arbitrary per-device offsets.

## Success criteria

The feature is successful only when all of the following are true on the POCO
X8 Pro using a published 3D frame:

1. In front view, the frame and lenses remain visible with no visible mask
   artifact.
2. At approximately 30–60 degrees of left and right yaw, the rearward temple
   is covered by the side head/ear instead of drawing over it.
3. At a side profile, the temple endpoint does not remain visibly in front of
   the ear for a sustained frame, and reacquisition does not flash the stale
   mask.
4. Face loss, mask loss, or segmentation confidence loss returns to the
   existing face-mesh/yaw fallback without a crash or black camera flash.
5. The 60-second motion check maintains at least 24 rendered FPS, with no
   renderer reset, unbounded memory growth, or visible per-frame debug data.
6. Frame image preview, saving, reservation, variant selection, and the
   existing 3D fallback remain usable.

## Commands

Run from the repository root in PowerShell with Android Studio's JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

Focused tests will be added to the approved task plan after the mask model and
renderer adapter contracts are settled.

## Project structure

```text
app/src/main/java/com/eyecare/app/presentation/ar/
  CameraPreviewView.kt              CameraX composition and analyzer lifecycle
  FaceLandmarkerHelper.kt           MediaPipe face result boundary
  HeadSegmenterHelper.kt            MediaPipe segmentation result boundary
  model/HeadOcclusionMask.kt        Immutable mask input for app code
  tracking/HeadOcclusionMapper.kt   Pure rotation/crop/mirror mapping
  tracking/HeadOcclusionPolicy.kt   Freshness, confidence, and fallback gate
  rendering/HeadOcclusionRenderer.kt Mask-aware SceneView/Filament adapter
app/src/test/java/com/eyecare/app/presentation/ar/
  tracking/                         Pure mapping and policy tests
  rendering/                        Renderer policy/lifecycle tests where feasible
app/src/main/assets/
  <reviewed segmenter model>.task   On-device model, if approved
docs/specs/
  This spec, plan, tasks, and device evidence
```

## Code style

Keep MediaPipe types at the camera boundary and expose validated immutable
inputs to the renderer:

```kotlin
data class HeadOcclusionMask(
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
    val alpha: ByteArray,
    val confidence: Float,
)

fun selectHeadOcclusion(
    mask: HeadOcclusionMask?,
    face: FaceFrame?,
    nowTimestampMs: Long,
): HeadOcclusionMode
```

- Framework and GPU types stay in adapters.
- Coordinate transforms are pure and deterministic.
- Hot-path buffers and GPU resources are reused where practical.
- Constants for freshness, confidence, and side-region gating are named,
  bounded, and covered by tests.
- No landmark or segmentation values are logged.

## Testing strategy

### Unit tests

- Reject invalid dimensions, non-finite metadata, empty masks, and stale
  timestamps.
- Verify portrait rotation, front-camera mirroring, aspect-fill crop, and
  viewport mapping with deterministic fixtures.
- Verify that the side-head region can activate at three-quarter/side yaw but
  does not cover the central face/frame region in front view.
- Verify fallback to `TempleVisibilityPolicy` for missing mask, face loss,
  low confidence, stale data, and renderer failure.
- Verify mask buffers are not retained after the associated tracking frame is
  invalidated.

### Build and static checks

- Run focused tests after each behavioral increment.
- Run `testDebugUnitTest`, `assembleDebug`, and `lintDebug` before the device
  checkpoint.
- Use a manual device check for GPU compositing; JVM tests cannot prove actual
  camera/model layer ordering.

### Manual device checkpoint

Test front, 30-degree yaw, 60-degree yaw, side profile, pitch, quick turns,
hair/lighting changes, face loss/reacquisition, repeated screen entry, and a
60-second movement run. Record visible artifacts, temple flashes, median FPS,
crashes, and renderer resets.

## Boundaries

### Always do

- Keep all face and segmentation data on-device and ephemeral.
- Retain the existing yaw and face-mesh fallbacks.
- Reuse analysis and GPU resources; do not create a new camera session.
- Test mapping and gating before integrating the renderer.
- Preserve the existing image and reservation fallbacks.

### Ask first

- Select or replace the segmentation model asset if its license, size, or
  minimum-device requirements are unclear.
- Add ARCore depth, a second camera pipeline, or a new external renderer.
- Change the GLB asset contract or backend API.
- Make the feature a prerequisite for saving or reserving a frame.

### Never do

- Persist, upload, or log face/segmentation data.
- Apply the full person mask as an opaque overlay over the glasses.
- Claim perfect ear/hair occlusion or clinical fit accuracy.
- Remove the existing fallback because segmentation is unavailable.
- Skip the physical-device checkpoint.

## Approved decisions

1. Use the official MediaPipe **SelfieSegmenter** person/background model for
   the first spike, starting with the square model and a confidence mask. Keep
   the model file in `app/src/main/assets/` after its model card and license
   are reviewed. Do not start with the slower multi-class model; revisit it
   only if the person mask cannot provide acceptable ear edges.
2. Use the POCO X8 Pro as the initial device gate. Before production claims,
   repeat the gate on at least one lower-performance Android device.
3. Submit every latest CameraX frame to the MediaPipe `LIVE_STREAM` segmenter
   initially and rely on its asynchronous backpressure behavior. If measured
   FPS or thermal behavior is unacceptable, process every second frame and
   reuse a mask for at most 100 ms before falling back.
4. Require partial temple masking as the success behavior. Whole-temple
   hiding remains the fallback whenever the mask is uncertain, stale, or
   unavailable.

## Approval gate

The objective, side-only masking behavior, model direction, device gate,
cadence, and fallback decisions are approved. Create the implementation plan
and task breakdown next. Implementation remains incremental and must stop at
the physical-device checkpoint before any production-hardening claim.
