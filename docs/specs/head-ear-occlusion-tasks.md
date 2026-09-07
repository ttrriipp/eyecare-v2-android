# Tasks: App-Side Head and Ear Occlusion

**Specification:** `docs/specs/head-ear-occlusion-spec.md`

**Plan:** `docs/specs/head-ear-occlusion-plan.md`

**Status:** Approved on 2026-09-06; Tasks 1–6 implemented, physical visual checkpoint pending

## Phase A — Camera and pure foundation

### Task 1: Add the reviewed SelfieSegmenter boundary

**Description:** Add the approved square SelfieSegmenter model asset and a
small helper that owns MediaPipe `ImageSegmenter` creation, live-stream
configuration, confidence-mask extraction, error handling, and disposal. Keep
the helper framework-facing; do not expose MediaPipe result objects to app
state.

**TDD/verification order:**

1. Add model/configuration tests for the asset name, live-stream mode, and
   confidence-mask requirement.
2. Add the reviewed model under `app/src/main/assets/` after verifying its
   model card/license and APK impact.
3. Implement helper creation and close behavior; run focused tests and a debug
   build.

**Acceptance criteria:**

- [x] The model asset is present, loadable, and its license/model card is
  recorded in the task evidence.
- [x] The helper uses `RunningMode.LIVE_STREAM` and confidence output.
- [x] Initialization failure reports a recoverable unavailable state and never
  blocks image preview or reservation.
- [x] The helper closes its ImageSegmenter exactly once.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*HeadSegmenter*"
.\gradlew assembleDebug
```

**Dependencies:** Approved spec and plan.

**Files likely touched:**

- `app/src/main/assets/<reviewed-selfie-segmenter-model>.tflite`
- `app/src/main/java/com/eyecare/app/presentation/ar/HeadSegmenterHelper.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/model/HeadSegmenterState.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/HeadSegmenterConfigTest.kt`

**Estimated scope:** Medium, 4 files including the model asset.

### Task 2: Publish one paired camera result

**Description:** Refactor the CameraX analyzer so one `ImageProxy` produces one
rotated `MPImage` and one monotonic timestamp shared by Face Landmarker and
Image Segmenter. Publish the face and optional mask together and close the
proxy/bitmap resources once.

**Acceptance criteria:**

- [x] No two analyzer helpers consume or close the same `ImageProxy`.
- [x] Face and mask results carry the same source timestamp and image
  dimensions.
- [x] Face loss clears the mask; a mask without a current face cannot reach
  the renderer.
- [x] Existing pose stabilization, permission, asset loading, and image
  fallback behavior remain unchanged.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*FaceLandmarker*" --tests "*ArViewModel*"
.\gradlew assembleDebug
```

**Dependencies:** Task 1.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/ar/CameraPreviewView.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/FaceLandmarkerHelper.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArFrameAnalyzer.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/model/FaceFrame.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/model/ArFaceState.kt` if split from `FaceFrame.kt`

**Estimated scope:** Medium, 4–5 files.

### Task 3: Map and gate the side-head mask

**Description:** Add immutable mask data plus pure coordinate mapping and
activation policy. Mapping must share the face path's rotation, mirroring, and
aspect-fill rules. The policy limits masking to side-head/ear pixels outside
the central face region and enforces confidence and 100 ms freshness limits.

**TDD order:**

1. Add red tests for dimensions, rotation, mirroring, crop, confidence,
   freshness, face loss, central-face exclusion, and fallback selection.
2. Implement the minimum pure mapper and policy.
3. Run focused tests, then the full unit suite.

**Acceptance criteria:**

- [x] Invalid or non-finite masks produce no active occlusion.
- [x] Mapping matches the existing face/model viewport conventions.
- [x] Central face/frame pixels are excluded from the side-head mask.
- [x] A mask older than 100 ms or below the reviewed confidence threshold
  selects `TempleVisibilityPolicy`.
- [x] The mask buffer is bounded and not copied once per render frame.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*HeadOcclusion*"
.\gradlew assembleDebug
```

**Dependencies:** Task 2.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/ar/model/HeadOcclusionMask.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/tracking/HeadOcclusionMapper.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/tracking/HeadOcclusionPolicy.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/tracking/HeadOcclusionMapperTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/tracking/HeadOcclusionPolicyTest.kt`

**Estimated scope:** Medium, 5 files.

### Checkpoint A: Foundation quality gate

After Tasks 1–3:

- [x] Focused tests pass.
- [x] `testDebugUnitTest` passes.
- [x] `assembleDebug` passes.
- [x] `lintDebug` passes.
- [x] No segmentation or mask values are logged or persisted.
- [x] Renderer work started only after the pure foundation gate; no visual
  success claim is made before the device checkpoint.

## Phase B — Visual proof and renderer spike

### Task 4: Add a debug-only mask overlay

**Description:** Render the mapped side-head mask as a translucent debug layer
over the camera preview, enabled only in debug builds or an internal flag. Use
it to verify portrait crop, front-camera mirroring, ear coverage, face-region
exclusion, and stale-mask clearing before changing the 3D renderer.

**Implementation status:** The debug-only overlay is wired and downsampled for
inspection. Alignment and stale-mask behavior still require the POCO X8 Pro
visual checkpoint below.

**Acceptance criteria:**

- [ ] The overlay is aligned with the user's visible head in front,
  three-quarter, and side views.
- [ ] The overlay does not cover the central face/frame region.
- [ ] The overlay disappears promptly on face loss or stale data.
- [ ] Release behavior has no debug overlay or segmentation data logging.

**Verification:**

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
```

Manual: POCO X8 Pro front, 30-degree, 60-degree, and side-profile views.

**Dependencies:** Checkpoint A.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/ar/components/HeadOcclusionDebugOverlay.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/components/HeadOcclusionDebugOverlayTest.kt` if a pure visibility contract is needed

**Estimated scope:** Small–Medium, 2–3 files.

### Task 5: Build the mask-aware renderer adapter

**Description:** Implement one reusable SceneView/Filament adapter that receives
the validated side-head mask and applies it as a depth-only or mask-aware
compositing operation. Keep material, texture, mesh, and buffer ownership
explicit and reuse them across mask updates.

**Implementation status:** A reusable fixed-capacity depth-only SceneView node
is integrated. Its occlusion effect and layer order remain unproven until the
POCO X8 Pro checkpoint.

**Acceptance criteria:**

- [ ] The adapter can be enabled, updated, hidden, and disposed without
  recreating GPU resources for each camera result.
- [ ] Mask pixels can occlude a rearward temple without drawing visible color.
- [ ] The adapter cannot cover the front frame/lenses outside the side region.
- [ ] Invalid/stale input disables the adapter immediately.
- [ ] No device-specific translation or scale offset is introduced to hide a
  projection mismatch.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*HeadOcclusion*" --tests "*FaceOccluder*"
.\gradlew assembleDebug
```

Manual: prove the layer order and mask effect on the POCO before integration.

**Dependencies:** Task 4.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/ar/rendering/HeadOcclusionRenderer.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/rendering/HeadOcclusionMaterial.kt` if required by the selected Filament path
- `app/src/test/java/com/eyecare/app/presentation/ar/rendering/HeadOcclusionRendererPolicyTest.kt`

**Estimated scope:** Medium, 3 files.

### Task 6: Integrate both model paths and preserve fallbacks

**Description:** Pass the validated mask through `ArTryOnScreen` and
`FrameModelRenderer`, attach the same renderer adapter to bundled and downloaded
GLBs, and make the existing temple policy authoritative whenever mask
occlusion is inactive.

**Implementation status:** Both bundled and downloaded paths share the node and
restore the yaw fallback whenever the mask gate is inactive. Device behavior is
still pending the physical checkpoint.

**Acceptance criteria:**

- [ ] Bundled and downloaded models use the same side-head mask behavior.
- [ ] Front face depth occlusion and partial temple masking can coexist.
- [ ] Mask loss immediately restores yaw-based temple visibility.
- [ ] Asset loading, variant changes, image preview, saving, and reservation
  remain unchanged.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*TempleVisibilityPolicy*" --tests "*FaceOcclusionPolicy*" --tests "*ArTryOn*"
.\gradlew assembleDebug
.\gradlew lintDebug
```

**Dependencies:** Task 5.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/rendering/FrameModelRenderer.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/model/ArTryOnUiState.kt` only if the mask needs a state carrier
- `app/src/test/java/com/eyecare/app/presentation/ar/ArTryOnContentStateTest.kt` if public fixtures change

**Estimated scope:** Medium, 3–4 files.

### Checkpoint B: Physical-device gate

Run the full automated suite and install the debug APK on the POCO X8 Pro.
Test front, both yaw directions, side profile, pitch, fast turns, hair and
lighting changes, face loss/reacquisition, repeated entry, and 60 seconds of
movement. Record FPS, thermal behavior, temple flashes, artifacts, crashes,
and renderer resets.

- [ ] Visual and performance criteria pass.
- [ ] No black camera flash or stale mask remains.
- [ ] User confirms the temple/ear result before hardening.

**STOP:** Do not begin Phase C without the user's checkpoint result.

## Phase C — Conditional hardening and evidence

### Task 7: Resolve measured gate findings only

**Description:** Follow the checkpoint branch. Optimize only a measured
bottleneck or fix a documented visual/lifecycle defect. If the gate fails,
disable the mask renderer and retain the existing face-mesh/yaw fallback.

**Acceptance criteria:**

- [ ] Every code change maps to a recorded device finding.
- [ ] Regression behavior has a unit test where JVM-testable.
- [ ] No new backend, GLB, ARCore, or patient-control scope is added.

**Verification:**

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

**Dependencies:** Checkpoint B.

**Files:** At most five files named in the evidence before editing.

**Estimated scope:** XS–Medium, conditional.

### Task 8: Record final evidence and reconcile specs

**Description:** Record model/version/license, APK/build, devices, mask and
FPS results, accepted limitations, and the user's final visual confirmation.
Update the parent 3D try-on documents without claiming perfect ear/hair
occlusion or clinical fit.

**Acceptance criteria:**

- [ ] Spec, plan, and task status reflect the actual gate outcome.
- [ ] Automated checks and the accepted manual result are recorded.
- [ ] Fallback behavior and known hair/lighting limitations are explicit.

**Verification:** Documentation review plus final automated commands.

**Dependencies:** Task 7 or an explicitly recorded failed/disabled branch.

**Files likely touched:**

- `docs/specs/head-ear-occlusion-tasks.md`
- `docs/specs/head-ear-occlusion-evidence.md`
- `docs/specs/3d-frame-try-on-tasks.md`

**Estimated scope:** Medium, documentation only.

## Approval gate

Implementation must not begin until this task breakdown is reviewed and
approved. Approval authorizes Tasks 1–6 through Checkpoint B. Tasks 7–8 remain
conditional on the physical-device result.
