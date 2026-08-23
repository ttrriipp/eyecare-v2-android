# Spec: Face-Mesh Depth Occlusion v1

**Status:** Approved on 2026-08-23

**Parent feature:** `docs/specs/3d-frame-try-on-spec.md`

**Scope owner:** Android application; no backend change

## Objective

Improve the existing front-camera 3D eyewear preview so frame geometry that is
physically behind the detected face is hidden by the nose, cheeks, and side of
the face during moderate head turns. The result remains a visual preview, not a
clinical fit measurement.

This extension addresses the parent spec's explicit exclusion of perfect
temple occlusion with a bounded v1. It does not expand the claim to ears, hair,
or complete head reconstruction.

## Assumptions and Product Decisions

- The existing CameraX, MediaPipe Face Landmarker, and SceneView/Filament
  pipeline remains authoritative for camera ownership, face tracking, and 3D
  rendering.
- The POCO X8 Pro remains the primary capstone validation device.
- V1 covers depth occlusion by the facial surface: nose, cheeks, and side-face.
- Accurate ear, hair, and full-head occlusion remains excluded.
- The current conservative temple-visibility policy remains the fallback when
  a valid occluder is unavailable or the spike fails its gate.
- Face landmarks remain on-device and ephemeral. They are never persisted,
  uploaded, logged, or added to analytics.
- No backend endpoint, asset contract, GLB change, ARCore integration, semantic
  segmentation model, or new third-party dependency is required for v1.
- The first code increment is a technical spike and must stop at the POCO
  manual checkpoint before production hardening continues.

## Success Criteria

The spike passes only when all of the following are observed on the POCO X8
Pro with the currently published round-frame asset:

1. At approximately 30 to 60 degrees of left and right yaw, the far-side
   temple and any genuinely rearward frame geometry are hidden by the detected
   side-face instead of drawing over it.
2. The near rim, near temple, and front-facing frame geometry remain visible;
   the occluder does not erase the whole model or produce a visible face mesh.
3. The camera feed does not flash black when a face is acquired, lost, or
   reacquired.
4. Tracking loss removes or disables stale occlusion consistently with the
   existing model tracking-loss behavior.
5. A 60-second motion script sustains a median of at least 24 rendered frames
   per second with reduced per-frame logging, without a crash, renderer reset,
   or unbounded memory growth.
6. Disabling or failing occlusion restores the existing temple visibility
   behavior and does not block image preview or frame reservation.

Passing the spike authorizes the remaining v1 hardening tasks. Failing the
spike keeps the existing implementation and records the evidence; it does not
trigger ARCore or segmentation work automatically.

## Scope

### Included

- Preserve and validate the single detected face's 478 MediaPipe landmarks.
- Copy MediaPipe-owned output into an immutable presentation-safe model before
  leaving the result callback.
- Map landmark coordinates through the same rotation, crop, mirroring, and
  renderer-space conventions used by the visible frame.
- Build or update one facial-surface mesh using a fixed, reviewed MediaPipe
  face topology.
- Render the face mesh as an invisible depth-only occluder before the glasses.
- Apply a bounded, testable depth bias or face-region policy so front frame
  geometry is not incorrectly removed.
- Reuse GPU objects and vertex/index buffers rather than rebuilding renderer
  resources on every camera frame.
- Add deterministic unit tests for landmark validation, coordinate mapping,
  tracking loss, and fallback selection.
- Record POCO side-view screenshots/video notes and performance evidence at the
  manual gate.

### Excluded

- Ear, hair, neck, hand, or complete head occlusion.
- Semantic person, hair, or body segmentation.
- ARCore Depth or a replacement camera session.
- Model-specific mesh authoring changes or splitting frame GLBs into parts.
- Patient controls for occlusion calibration.
- Clinical face measurements or a claim of physical fit accuracy.
- Backend, database, admin, reservation, messaging, or notification changes.

## Functional Requirements

### Landmark capture

- Accept exactly one face, consistent with the existing tracker.
- Reject missing, incomplete, non-finite, or dimensionally invalid landmark
  frames without passing malformed geometry to the renderer.
- Keep the existing facial transformation matrix and pose pipeline unchanged.
- Avoid per-frame collection churn where practical; correctness takes priority
  during the spike, and allocation optimization follows measurement.

### Coordinate mapping

- Landmark mapping must explicitly account for rotated CameraX input, front
  camera mirroring, preview crop/aspect ratio, and SceneView camera projection.
- Mapping logic must be independent of Compose and GPU objects so it can be
  tested with deterministic landmark fixtures.
- The occluder and visible frame must use one timestamp-compatible face result;
  stale face geometry must not be paired with a newer pose indefinitely.

### Rendering

- The occluder writes depth but never writes visible color.
- Depth testing remains enabled for the visible frame.
- One occluder node is created per active renderer instance and updated in
  place; it is not recreated for each landmark result.
- Invalid or absent occluder geometry hides the occluder and enables the
  existing temple policy.
- Renderer disposal releases occluder geometry, material, vertex buffers, and
  index buffers exactly once.
- The implementation must preserve transparent CameraX/SceneView composition
  and existing asset-loading states.

### User experience and fallback

- No new patient-facing control or claim is added.
- The existing disclosure, **Visual preview only. Final fit is confirmed at the
  clinic.**, remains visible.
- Occlusion failure is silent when the ordinary 3D preview remains usable; the
  conservative temple policy provides the fallback.
- A renderer-level failure continues to use the existing image-preview
  fallback and never blocks reservation.

## Technical Stack

| Concern | Existing decision retained |
|---|---|
| Camera | CameraX 1.5.0 |
| Face surface | MediaPipe Tasks Vision 0.10.35 Face Landmarker |
| Renderer | SceneView 4.18.0 |
| GPU runtime | Google Filament through SceneView |
| UI/state | Compose + `StateFlow`; no LiveData |
| Tests | JUnit 5 and existing Android test stack |

MediaPipe documents that Face Landmarker returns a 478-landmark 3D face mesh
and optional facial transformation matrices. Filament documents independent
color-buffer and depth-buffer writes, allowing an invisible object to occlude
later geometry. V1 uses those capabilities instead of introducing ARCore.

## Commands

Run from the repository root in PowerShell with Android Studio's JBR available:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

Focused test commands will be named in the approved task plan once production
types are finalized.

## Project Structure

```text
app/src/main/java/com/eyecare/app/presentation/ar/model/
  Immutable landmark and occlusion inputs
app/src/main/java/com/eyecare/app/presentation/ar/tracking/
  Pure landmark validation and renderer-coordinate mapping
app/src/main/java/com/eyecare/app/presentation/ar/rendering/
  Depth material, reusable mesh/node lifecycle, and fallback policy
app/src/test/java/com/eyecare/app/presentation/ar/
  Pure mapper, validation, and policy tests
docs/specs/
  This spec, the approved task plan, and checkpoint evidence
```

## Code Style

Keep external MediaPipe types at the tracker boundary and expose validated,
immutable app models:

```kotlin
data class FaceMeshFrame(
    val landmarks: List<FaceMeshLandmark>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
)

fun mapFaceOccluder(
    frame: FaceMeshFrame,
    viewport: RenderViewport,
): FaceOccluderGeometry?
```

- DTO/framework types do not leak into renderer-independent mapping logic.
- Public behavior is expressed through state or return values, not log parsing.
- Hot-path resources have explicit ownership and disposal.
- Constants that affect visual calibration are named, bounded, and documented.

## Testing Strategy

### Unit tests

- Landmark model accepts one complete finite frame and rejects malformed input.
- Coordinate mapping covers portrait rotation, mirroring, aspect-ratio crop,
  representative depth ordering, and invalid viewport dimensions.
- Occlusion activation selects depth mesh only for a valid current face and
  selects the existing temple fallback otherwise.
- Depth-bias policy stays within documented limits.
- Tracking loss cannot leave a stale occluder active indefinitely.

GPU rasterization itself is not asserted by JVM tests. That behavior requires
the physical-device checkpoint.

### Build and static checks

- Run focused tests after every behavioral increment.
- Run `assembleDebug` after every implementation task, per project rules.
- Run the full unit suite and `lintDebug` before the final review.

### Manual POCO checkpoint

Test front, approximately 30-degree yaw, approximately 60-degree yaw, pitch,
normal movement, fast movement, face loss/reacquisition, and repeated screen
entry. Capture both left and right yaw. Record frame visibility defects, black
flashes, median FPS, crashes, and renderer resets.

The agent must stop at this checkpoint. Human confirmation is required before
the spike is declared successful and hardening tasks begin.

## Boundaries

### Always do

- Keep face processing ephemeral and on-device.
- Retain the existing non-3D and reservation fallbacks.
- Use one reusable occluder per renderer instance.
- Test pure mapping and fallback logic before renderer integration.
- Build after every implementation task and review every checkpoint change.

### Ask first

- Add any dependency or MediaPipe model.
- Introduce ARCore, segmentation, shaders outside the existing
  SceneView/Filament material path, or a second camera pipeline.
- Change the GLB asset contract or backend response.
- Expand the claim to ears, hair, clinical fit, or minimum-device support.

### Never do

- Upload, persist, or log facial landmark coordinates.
- Make occlusion availability a condition for reservation.
- Recreate GPU resources on every frame without measured justification.
- Claim accurate ear/hair coverage from the facial mesh.
- Skip the physical-device checkpoint after the technical spike.

## Risks and Gate Outcomes

| Risk | Impact | Required mitigation |
|---|---|---|
| Camera/renderer coordinate mismatch | Mesh occludes the wrong pixels | Pure mapper tests plus POCO overlay calibration |
| Depth fighting near rims and nose | Flicker or missing front geometry | Bounded depth bias and both-yaw manual evidence |
| Per-frame allocation/GPU updates | Lag or thermal throttling | Reuse buffers, remove frame logs, measure 60 seconds |
| Stale mesh after tracking loss | Face remains an invisible blocker | Timestamp validation and explicit hide/fallback state |
| SceneView lifecycle misuse | Black frames, leaks, or crashes | Single ownership, disposal tests where possible, repeated-entry device check |

Gate outcomes:

- **Pass:** all spike success criteria pass; proceed to hardening and final
  regression tasks.
- **Conditional pass:** occlusion works but misses the 24 FPS floor; optimize
  only measured hot paths and repeat the checkpoint.
- **Fail:** incorrect depth, unacceptable artifacts, or instability persists;
  remove/disable the spike and retain `TempleVisibilityPolicy`.

## Open Questions

No product decision is currently blocking the spike. The exact reviewed face
triangle topology representation and safe depth-bias range are technical spike
outputs and must be recorded in the subsequent task plan/evidence before the
gate is evaluated.

## Official Sources

- MediaPipe Face Landmarker for Android: 3D landmarks, 478-point face mesh, and
  facial transformation matrices:

  https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker/android
- Google Filament material rasterization controls: `colorWrite`, `depthWrite`,
  and depth testing:

  https://google.github.io/filament/main/materials.html#materialdefinitions-material-block-rasterization
- ARCore Depth capability and support constraints, considered but excluded from
  v1:

  https://developers.google.com/ar/develop/java/depth/developer-guide
