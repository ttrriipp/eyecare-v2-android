# Tasks: Face-Mesh Depth Occlusion v1

**Status:** Draft for approval

**Approved specification:** `docs/specs/face-mesh-depth-occlusion-spec.md`

**Approved plan:** `docs/specs/face-mesh-depth-occlusion-plan.md`

## Execution Rules

- Work on `codex/face-mesh-occlusion`; do not implement these tasks on `main`.
- Execute tasks in dependency order. Do not begin a task whose dependencies or
  checkpoint are incomplete.
- Use red-green-refactor for every new pure behavior: add the focused test,
  confirm it fails for the intended reason, implement the minimum behavior,
  then confirm it passes.
- Run the focused tests named by each task and `.\gradlew assembleDebug` after
  every implementation task.
- Commit every completed task independently with the listed commit message.
- Before every commit, inspect staged changes, run `git diff --staged --check`,
  and check the staged diff for secrets or facial-coordinate logging.
- Run the `code-review-and-quality` five-axis review at every named checkpoint.
  Fix all critical and required findings before proceeding.
- Do not repeat an unchanged successful command merely for reassurance.
- Do not add dependencies, ARCore, segmentation, backend changes, GLB changes,
  or patient-facing claims without revising the approved spec and plan.
- Stop at Task 7. The user must complete the POCO gate and report pass,
  conditional pass, or fail before Task 8 begins.
- Stop again at the final visual check in Task 9 before declaring completion.

Use Android Studio's JBR for Gradle commands:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Phase A: Validated Face-Surface Foundation

### Task 1: Capture a validated compact face mesh

**Description:** Add the immutable app-owned landmark representation, attach it
to `FaceFrame`, and copy all 478 landmarks at the MediaPipe callback boundary.
Remove the current per-frame landmark/FPS log because facial coordinates must
not be logged and logging invalidates performance measurements.

**TDD order:**

1. Add `FaceMeshLandmarksTest` for exact coordinate count, finite values,
   defensive copying, indexed access, and invalid input.
2. Run the focused test and confirm it fails because the model does not exist.
3. Implement the compact model and make the focused test pass.
4. Extend `FaceFrame` with an optional mesh default so existing non-mesh test
   fixtures and the 2D fallback remain source-compatible.
5. Copy validated landmarks in `FaceLandmarkerHelper`; emit `NoFace` for an
   invalid full mesh and remove per-frame coordinate/FPS logging.

**Acceptance criteria:**

- [ ] Exactly 478 `(x, y, z)` landmarks are stored as an app-owned defensive
  copy with no mutable buffer exposed.
- [ ] Missing, wrong-sized, or non-finite input is rejected before presentation
  state or rendering.
- [ ] A valid MediaPipe detection supplies both the existing pose inputs and a
  non-null face mesh; existing fallback fixtures can omit the mesh.
- [ ] No facial landmark coordinate or per-frame FPS log remains in the
  callback hot path.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*FaceMeshLandmarksTest"
.\gradlew testDebugUnitTest --tests "*ArViewModelTest" --tests "*ArTryOnContentStateTest"
.\gradlew assembleDebug
```

**Dependencies:** Approved tasks document.

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/ar/model/FaceMeshLandmarks.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/model/FaceFrame.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/FaceLandmarkerHelper.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/model/FaceMeshLandmarksTest.kt`

**Estimated scope:** Medium, 4 files.

**Commit:** `feat: capture validated face mesh landmarks`

---

### Task 2: Derive deterministic triangle topology

**Description:** Convert MediaPipe's pinned
`FACE_LANDMARKS_TESSELATION` directed connection set into a fixed triangle
index list once, without copying an opaque index table into the project or
rebuilding topology per frame. The pinned bytecode initializes 2,556 directed
connections, representing 852 closed three-edge cycles.

**TDD order:**

1. Add `FaceMeshTopologyTest` using small directed-edge graphs for closed
   `a -> b -> c -> a` triangles, shared edges, duplicate cyclic
   representations, shuffled input, incomplete cycles, invalid indices, and
   deterministic output.
2. Run it red because the topology builder is absent.
3. Implement the pure builder and MediaPipe adapter, then run it green.
4. Add a fail-closed MediaPipe adapter that requires exactly 852 unique
   triangles from the pinned official connection set with indices bounded to
   `0..477`. Keep framework initialization out of the local JVM test path; the
   runtime invariant is exercised at the POCO gate.

**Acceptance criteria:**

- [ ] Connection direction is preserved; only closed directed cycles become
  triangles, and duplicate cyclic representations cannot duplicate a face.
- [ ] Incomplete cycles and out-of-range indices cannot reach the renderer.
- [ ] Output order is deterministic regardless of connection-set iteration
  order.
- [ ] The official topology is derived lazily once, contains exactly 852
  triangles, and fails safely to no occluder if any pinned invariant changes.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*FaceMeshTopologyTest"
.\gradlew assembleDebug
```

**Dependencies:** Task 1.

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/ar/tracking/FaceMeshTopology.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/tracking/MediaPipeFaceMeshTopology.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/tracking/FaceMeshTopologyTest.kt`

**Estimated scope:** Medium, 3 files.

**Commit:** `feat: derive deterministic face mesh topology`

---

### Task 3: Map landmarks into an occluder projection

**Description:** Add a pure renderer-independent mapper for aspect-fill crop,
front-camera mirroring, bounded landmark depth, and occlusion bias. It outputs
validated viewport/depth vertices; SceneView ray conversion remains outside
this task.

**TDD order:**

1. Add `FaceOccluderMapperTest` for matching aspect ratios, horizontal and
   vertical aspect-fill crop, mirroring, portrait dimensions, representative
   nose-versus-cheek depth, bounded bias, invalid dimensions, and non-finite
   inputs.
2. Run it red because the mapper does not exist.
3. Implement the minimum pure mapping types and function, then run it green.
4. Refactor only if tests remain green and constants stay named and bounded.

**Acceptance criteria:**

- [ ] Image-to-viewport mapping preserves the center and applies aspect-fill
  crop deterministically for portrait output.
- [ ] Front-camera mirroring happens exactly once.
- [ ] Relative landmark `z` is normalized by detected face width and cannot
  produce unbounded renderer depth.
- [ ] Invalid image, viewport, face-width, or coordinate input returns no
  geometry instead of throwing or emitting invalid vertices.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*FaceOccluderMapperTest"
.\gradlew assembleDebug
```

**Dependencies:** Task 1.

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/ar/tracking/FaceOccluderMapper.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/tracking/FaceOccluderMapperTest.kt`

**Estimated scope:** Small, 2 files.

**Commit:** `feat: map face landmarks for depth occlusion`

---

## Checkpoint A: Foundation Quality Gate

After Tasks 1–3:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
```

- [ ] Run `code-review-and-quality` across commits for Tasks 1–3.
- [ ] Correctness: validation, topology, crop, mirroring, and depth bounds match
  the approved spec and tests cover failure paths.
- [ ] Readability: framework-independent models and mapping remain simple.
- [ ] Architecture: MediaPipe objects do not leak beyond the tracking boundary.
- [ ] Security/privacy: no coordinate persistence or logging exists.
- [ ] Performance: topology is not rebuilt per frame and landmark storage does
  not allocate hundreds of Kotlin point objects per result.
- [ ] Fix and commit all required findings before Phase B.

This checkpoint is automated and does not require user action.

## Phase B: Depth-Occlusion Technical Spike

### Task 4: Select depth occlusion or temple fallback explicitly

**Description:** Add a small pure policy that enables the depth mesh only for a
valid, current face/geometry pair and otherwise retains
`TempleVisibilityPolicy`. This prevents stale invisible geometry and makes the
fallback behavior testable before GPU integration.

**TDD order:**

1. Add `FaceOcclusionPolicyTest` for current valid geometry, missing mesh,
   mapping failure, negative/future timestamps, stale timestamps, face loss,
   and recovery.
2. Run it red because the policy does not exist.
3. Implement the minimum policy and run it green.

**Acceptance criteria:**

- [ ] Depth occlusion activates only for valid geometry within the documented
  freshness window.
- [ ] Missing, invalid, future, or stale data selects temple fallback.
- [ ] Face loss immediately makes the occluder ineligible; recovery is
  deterministic.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*FaceOcclusionPolicyTest"
.\gradlew assembleDebug
```

**Dependencies:** Tasks 1 and 3.

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/ar/rendering/FaceOcclusionPolicy.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/rendering/FaceOcclusionPolicyTest.kt`

**Estimated scope:** Small, 2 files.

**Commit:** `feat: gate face occlusion with safe fallback`

---

### Task 5: Build one reusable SceneView occluder node

**Description:** Wrap SceneView 4.18's occlusion material and dynamic geometry
in one lifecycle-owned component. Convert mapped viewport vertices through the
SceneView camera ray and update positions while retaining the node, topology,
vertex buffer, index buffer, and material instance.

This task is a framework/GPU adapter. Its pure inputs were tested in Tasks 2–4;
real rasterization is intentionally deferred to the POCO gate.

**Acceptance criteria:**

- [ ] One `GeometryNode` uses `createOcclusionInstance()` and the fixed triangle
  indices.
- [ ] Face results update existing vertex positions; they do not recreate the
  node, topology, material, vertex buffer, or index buffer.
- [ ] Invalid/inactive input hides the node without leaving stale depth.
- [ ] Node disposal destroys geometry and its owned material exactly once
  through SceneView's lifecycle.
- [ ] No landmark-coordinate logging or visible debug mesh ships in the normal
  path.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*FaceMeshTopologyTest" --tests "*FaceOccluderMapperTest" --tests "*FaceOcclusionPolicyTest"
.\gradlew assembleDebug
```

**Dependencies:** Tasks 2–4.

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/ar/rendering/FaceOccluderNode.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/rendering/FaceOccluderProjectionAdapter.kt`

**Estimated scope:** Small, 2 files.

**Commit:** `feat: add reusable depth-only face occluder`

---

### Task 6: Integrate occlusion into live try-on

**Description:** Pass the current `FaceFrame` into `FrameModelRenderer`, attach
the shared occluder alongside both bundled and downloaded model paths, and
switch whole-temple hiding off only while valid depth occlusion is active.

**Acceptance criteria:**

- [ ] The current face mesh reaches the renderer only while tracking.
- [ ] Bundled and downloaded GLBs use the same occluder component and policy.
- [ ] A valid occluder replaces whole-node temple hiding; inactive/failed
  occlusion immediately restores the existing policy.
- [ ] Asset loading, transparent camera composition, image fallback, variant
  identity, and reservation behavior remain unchanged.

**Verification:**

```powershell
.\gradlew testDebugUnitTest --tests "*ArViewModelTest" --tests "*ArTryOnContentStateTest" --tests "*TempleVisibilityPolicyTest" --tests "*FaceOcclusionPolicyTest"
.\gradlew assembleDebug
.\gradlew lintDebug
```

**Dependencies:** Tasks 4 and 5.

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/ar/rendering/FrameModelRenderer.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/ArTryOnContentStateTest.kt` only if its public behavior fixture must change

**Estimated scope:** Medium, 2–3 files.

**Commit:** `feat: integrate face depth occlusion into try-on`

---

## Checkpoint B: Technical Spike Quality Gate

After Tasks 4–6:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

- [ ] Run `code-review-and-quality` across Tasks 4–6.
- [ ] Correctness: valid/current mesh activation and temple fallback match the
  spec.
- [ ] Readability: renderer integration does not duplicate occluder logic
  between asset sources.
- [ ] Architecture: pure mapping/policy stays outside SceneView adapters.
- [ ] Security/privacy: no face data leaves memory or enters logs.
- [ ] Performance: one node/topology/material lifecycle is evident; no resource
  creation occurs per camera result.
- [ ] Produce `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Fix and commit all required findings before Task 7.

This checkpoint is automated and may continue without user action when clean.

### Task 7: Execute the mandatory POCO spike gate

**Description:** Install the reviewed APK and have the user validate actual
depth behavior. This task is a hard manual stop because JVM tests cannot prove
GPU occlusion, camera/renderer crop alignment, visual bias, or device FPS.

**Install:**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

**Manual script:**

1. Open the same round-frame 3D try-on used for prior calibration.
2. Hold front view for five seconds.
3. Turn slowly left to approximately 30 degrees, then 60 degrees.
4. Return to center and repeat to the right.
5. Repeat with normal movement, one fast turn, and moderate pitch.
6. Leave the camera frame and re-enter twice.
7. Run normal movement for 60 seconds with reduced logging and record median
   FPS or the project's accepted measurement evidence.
8. Confirm **Reserve this frame** and **View frame images** remain usable.

**Acceptance criteria:**

- [ ] Far/rearward frame geometry is covered by the nose/cheek/side-face in
  both yaw directions.
- [ ] Near/front geometry remains visible; there is no visible face mesh,
  large missing rim, or persistent depth hole.
- [ ] No black flash, renderer reset, crash, or stale occluder occurs during
  loss/reacquisition.
- [ ] Median rendered FPS is at least 24 over 60 seconds.
- [ ] Existing image and reservation actions still work.

**Gate result:**

- **Pass:** visual criteria and FPS floor pass; proceed to Task 8.
- **Conditional pass:** visual criteria pass but FPS misses the floor; Task 8
  may optimize only the measured bottleneck, then Task 7 is repeated.
- **Fail:** visual alignment/artifacts or stability fail; Task 8 follows the
  fail branch and preserves `TempleVisibilityPolicy`.

**Evidence file:**

- `docs/specs/face-mesh-depth-occlusion-evidence.md`

**Estimated scope:** Manual checkpoint plus one evidence file.

**Commit after the user reports the result:**
`docs: record face occlusion spike evidence`

**STOP:** Do not begin Task 8 until the user reports the result.

## Phase C: Conditional Hardening and Completion

### Task 8: Resolve only measured spike findings

**Description:** Follow the branch selected by Task 7. Do not perform speculative
optimization or broaden the design.

**Pass branch:**

- Harden repeated entry, background/foreground, disposal, and face-loss paths
  only where the gate or review found a concrete gap.
- Keep the narrowest depth multiplier and bias that passed both yaw directions.
- If no issue was found, mark the task not needed rather than manufacturing a
  code change.

**Conditional-pass branch:**

- Measure the vertex update/allocation path.
- Optimize only the proven bottleneck while retaining one node and fixed
  topology.
- Re-run focused tests, build, review, then repeat Task 7.

**Fail branch:**

- Remove or disable the incomplete depth-occlusion integration in an atomic,
  reviewable commit.
- Retain `TempleVisibilityPolicy` and ordinary 3D/image/reservation behavior.
- Retain foundation code only if it has no active hot-path cost; otherwise
  remove it as part of the bounded rollback.

**Acceptance criteria:**

- [ ] Every change maps to a recorded Task 7 finding.
- [ ] Every changed behavior starts with a failing regression test where it is
  testable on the JVM.
- [ ] No dependency, backend, CameraX ownership, GLB, ear/hair, or patient-facing
  scope is added.
- [ ] No more than five files are changed; otherwise revise and reapprove the
  task breakdown before continuing.

**Verification:** Use the focused test for each finding, then:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

**Dependencies:** Completed Task 7 result.

**Files:** Determined by evidence, maximum 5; record them in the evidence file
before editing.

**Estimated scope:** XS–Medium, 0–5 files.

**Commit:**

- Pass/conditional: `fix: harden measured face occlusion behavior`
- Fail: `revert: retain conservative temple occlusion fallback`

#### Checkpoint C: Hardening review

- [ ] Run `code-review-and-quality` on Task 8 or record that no code change was
  necessary.
- [ ] Fix all critical and required findings.
- [ ] If Task 7 must be repeated, stop for the user again.

### Task 9: Run final regression and reconcile evidence

**Description:** Run final automated checks, repeat the POCO motion script for
the accepted branch, and reconcile the extension and parent 3D checklists. Do
not mark ear/hair/full-head occlusion complete.

**Automated verification:**

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

**Manual verification:**

- Repeat front/30-degree/60-degree yaw in both directions.
- Repeat face loss/reacquisition and repeated screen entry.
- Confirm no black flash, crash, stale mask, or reservation/image regression.
- Record median FPS for the accepted 60-second run.
- Obtain the user's final visual approval.

**Documentation:**

- Mark completed tasks and checkpoints in this file.
- Finalize `docs/specs/face-mesh-depth-occlusion-evidence.md`.
- Update `docs/specs/3d-frame-try-on-tasks.md` with a link and accurate bounded
  outcome; do not rewrite unrelated completion evidence.

**Acceptance criteria:**

- [ ] All automated commands pass.
- [ ] The accepted gate branch is documented with device/build/evidence.
- [ ] Parent checklist reflects the extension without claiming excluded
  ear/hair/full-head behavior.
- [ ] Final five-axis code review has no critical or required findings.
- [ ] User confirms the final visual result.

**Dependencies:** Task 8 complete or explicitly not needed.

**Files:**

- `docs/specs/face-mesh-depth-occlusion-tasks.md`
- `docs/specs/face-mesh-depth-occlusion-evidence.md`
- `docs/specs/3d-frame-try-on-tasks.md`

**Estimated scope:** Medium, 3 documentation files plus verification only.

**Commit:** `docs: finalize face occlusion validation evidence`

## Final Checkpoint

- [ ] Spec, plan, and task statuses reflect approval/completion accurately.
- [ ] Every implemented task has one atomic commit and passing verification.
- [ ] All checkpoint reviews are recorded and required findings are fixed.
- [ ] No uncommitted changes remain.
- [ ] The branch is ready for user-authorized merge; do not merge implicitly.

## Tasks Approval Gate

Implementation must not begin until the user reviews and approves this task
breakdown. Approval authorizes Tasks 1–6 to proceed through automated
checkpoints without additional pauses, followed by the mandatory stop at Task
7. Tasks 8–9 remain conditional on the user's Task 7 result.
