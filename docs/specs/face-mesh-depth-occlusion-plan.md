# Implementation Plan: Face-Mesh Depth Occlusion v1

**Status:** Draft for approval

**Approved specification:** `docs/specs/face-mesh-depth-occlusion-spec.md`

## Overview

Extend the current CameraX + MediaPipe + SceneView try-on path with one
invisible, dynamic facial mesh that writes only to Filament's depth buffer.
The plan is risk-first: prove topology, projection alignment, and depth behavior
on the POCO X8 Pro before investing in lifecycle and performance hardening.

The existing glasses pose remains authoritative. Occlusion augments the
renderer; it does not replace face detection, pose stabilization, asset loading,
or reservation behavior.

## Architecture Decisions

### 1. Keep MediaPipe types at the tracking boundary

`FaceLandmarkerHelper` will copy the one detected face's 478 `(x, y, z)` values
into a compact validated app model. MediaPipe-owned lists and landmark objects
will not enter the view model or renderer.

The compact model will use a private copied float buffer with indexed access,
instead of allocating 478 Kotlin landmark objects for every result. Existing
bridge/temple fields and the facial transformation matrix remain available so
the current pose and 2D fallback behavior do not change.

### 2. Derive topology once from the pinned official constant

MediaPipe 0.10.35 exposes
`FaceLandmarker.FACE_LANDMARKS_TESSELATION` as landmark connections. A pure
topology builder will normalize the undirected edges, enumerate closed
three-edge cycles, remove duplicate triangles, validate indices against the
478-point model, and return a deterministic index order.

The topology is built once per process or renderer lifetime. It is not rebuilt
per camera frame. Because SceneView's occlusion material is double-sided,
triangle winding is not a visual dependency for the spike; deterministic
winding/order remains useful for tests and diagnostics.

This avoids copying a large undocumented triangle table into the repository
while keeping the runtime tied to the exact pinned MediaPipe package.

### 3. Split projection into testable stages

Mapping will be separated into:

1. normalized MediaPipe image coordinates to the shared Compose viewport,
   including portrait rotation, aspect-fill crop, and front-camera mirroring;
2. viewport position to a SceneView camera ray; and
3. ray placement at the visible frame's depth, with bounded relative depth
   from MediaPipe `z` and a tunable occlusion bias.

The first and third stages will be pure Kotlin. SceneView camera-ray access will
remain in the renderer adapter. The spike will not introduce a second camera
session or ARCore.

The CameraX preview and SceneView already occupy the same full-size Compose
container. If POCO evidence shows their crops differ despite the explicit
aspect-fill mapping, the spike fails pending an approved plan change to carry
CameraX output-transform metadata; it will not be hidden with arbitrary
per-device offsets.

### 4. Reuse SceneView's supported occlusion material and geometry lifecycle

SceneView 4.18.0 provides `MaterialLoader.createOcclusionInstance()`, whose
material is invisible, depth-writing, opaque-pass, and double-sided. It also
provides `GeometryNode.updateGeometry()` and destroys the geometry when the node
is disposed.

One `GeometryNode`, one material instance, one vertex buffer, and one index
buffer will be created for each active `ModelScene`. Landmark updates replace
vertex positions in the existing geometry. The index topology stays fixed.
Material ownership will be explicit through the node's
`destroyMaterialsOnDispose` behavior.

### 5. Make fallback selection explicit

The existing `TempleVisibilityPolicy` stays active until a current, valid
occluder is ready. When depth occlusion is active, whole-node temple hiding is
disabled so the depth mesh can provide partial coverage. Missing, malformed,
stale, or renderer-rejected geometry immediately returns to the existing
policy.

No new patient-facing error state is introduced. A total renderer failure still
uses the existing image preview.

## Dependency Graph

```text
Approved spec
    |
    +-- compact landmark model + validation
    |       |
    |       +-- MediaPipe callback copying
    |       |
    |       +-- pure viewport/depth mapping
    |
    +-- deterministic topology builder
            |
            +-- reusable SceneView occluder node
                    |
                    +-- renderer/screen integration
                            |
                            +-- POCO spike checkpoint
                                    |
                 +------------------+------------------+
                 | pass / conditional pass             | fail
                 v                                     v
          lifecycle/performance                 disable spike and
          hardening + regression                retain temple fallback
                 |
                 v
          final POCO regression and evidence
```

Landmark capture and topology can be developed independently after their app
contracts are defined. Projection must precede renderer integration. Renderer
integration and the physical checkpoint are sequential because visual evidence
determines whether hardening is justified.

## Implementation Phases

### Phase A: Validated face-surface foundation

Establish the renderer-independent contracts and prove them with unit tests:

- compact immutable landmark storage with exact count/finite-value validation;
- deterministic tesselation-to-triangle conversion;
- aspect-fill, mirroring, depth-scale, and bounded-bias mapping;
- explicit valid/current occluder versus temple-fallback decision;
- MediaPipe result copying into the extended `FaceFrame`.

Likely production areas:

- `presentation/ar/model/FaceFrame.kt`
- `presentation/ar/FaceLandmarkerHelper.kt`
- new files under `presentation/ar/tracking/`

Likely test areas:

- `presentation/ar/model/`
- `presentation/ar/tracking/`
- affected `ArViewModel` and active-content fixtures

#### Checkpoint A: Foundation review

- Focused unit tests pass.
- Full unit suite passes.
- `assembleDebug` succeeds.
- Code review finds no unresolved correctness, privacy, architecture, or
  hot-path allocation issue.
- No UI or renderer behavior has changed yet.

This checkpoint is automated; it does not require the user to stop and test.

### Phase B: Depth-occlusion technical spike

Create one reusable occluder and integrate it into both bundled and downloaded
model paths:

- create the SceneView occlusion material and initial geometry once;
- update vertices without replacing the node or fixed topology;
- place the mesh through the SceneView camera projection adapter;
- hide stale/invalid geometry and restore `TempleVisibilityPolicy` fallback;
- preserve transparent camera composition and model loading states;
- expose only diagnostic information needed for a bounded debug measurement,
  with no landmark coordinate logging.

Likely production areas:

- `presentation/ar/rendering/FrameModelRenderer.kt`
- new renderer node/adapter files under `presentation/ar/rendering/`
- `presentation/ar/ArTryOnScreen.kt`

Likely test areas:

- renderer-independent activation and depth-policy tests
- Compose/state regression tests where existing public state changes

#### Checkpoint B: Technical review

- Focused tests and full unit suite pass.
- `assembleDebug` and `lintDebug` succeed.
- Code review confirms single resource ownership, bounded inputs, correct
  fallback, and no dependency/backend expansion.
- A debug APK is produced for the POCO.

#### Checkpoint C: Mandatory POCO spike gate

Stop for human verification. Test both left and right yaw at front,
approximately 30 degrees, and approximately 60 degrees, plus face loss and
reacquisition. Confirm:

- far/rearward frame geometry is hidden by the side-face;
- near/front geometry remains visible;
- no visible occluder, black flash, or persistent stale mask appears;
- ordinary tracking and reservation remain usable;
- the 60-second session meets the median 24 FPS floor.

Record pass, conditional pass, or fail. No hardening phase begins without the
user's checkpoint result.

### Phase C: Pass-branch hardening

Only after Checkpoint C passes or conditionally passes:

- address measured allocation or update-time bottlenecks;
- harden repeated entry, background/foreground, disposal, and tracking loss;
- calibrate the narrowest depth-bias range that passes both yaw directions;
- remove temporary diagnostics and retain only bounded performance evidence;
- run full automated and POCO regressions;
- update the spec/task evidence and parent 3D checklist without claiming
  ear/hair coverage.

#### Checkpoint D: Final review

- All focused and full tests pass.
- `assembleDebug` and `lintDebug` succeed.
- Repeated screen entry produces no crash, black flash, or retained occluder.
- The POCO motion script meets all approved success criteria.
- Code review has no unresolved critical or required findings.
- Human confirms the final visual result.

### Fail branch

If the spike fails Checkpoint C:

- disable or remove incomplete renderer integration in a rollback-friendly
  commit;
- retain the validated foundation only if it has independent value and no hot
  path cost, otherwise revert it as well;
- keep `TempleVisibilityPolicy` as the production behavior;
- record the failure reason and evidence;
- require a new approved specification before considering CameraX transform
  metadata, segmentation, ARCore, or GLB authoring changes.

## Verification Strategy

Automated verification will follow red-green-refactor for every new pure
behavior. Framework/GPU behavior that cannot be proven on the JVM is kept
behind a small adapter and verified at the POCO checkpoint.

Every implementation increment will run its focused tests and
`.\gradlew assembleDebug`. The full unit suite and lint run at phase checkpoints,
not redundantly between unchanged commits.

The task document created after plan approval will name exact test classes,
commands, file limits, and commit boundaries.

## Estimated Change Shape

| Area | Estimate |
|---|---:|
| Implementation tasks after task breakdown | 7-9 |
| Files per task | 1-5 |
| Total affected/new production files | 7-11 |
| Total affected/new test files | 4-7 |
| Backend/API changes | 0 |
| Mandatory human checkpoints | 2: spike result and final visual result |

This remains approximately 25-35% of the original end-to-end 3D try-on spec.
The estimate assumes no CameraX transform-contract expansion is needed after
the POCO spike.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Edge-set triangle derivation includes invalid faces | Incorrect depth surface | Deterministic graph tests, bounds checks, and visible debug mesh only during local spike diagnostics |
| Preview/render crop mismatch | Occlusion shifted from the glasses | Test aspect-fill mapping first; fail the gate rather than add arbitrary offsets |
| Landmark `z` is not metric depth | Nose/cheek depth is exaggerated or flat | Normalize by detected face width and use a tightly bounded, documented multiplier |
| `Geometry.update` allocates buffers per result | FPS or thermal regression | Measure first; reuse node/topology, then optimize only the vertex upload if required |
| Duplicate bundled/downloaded renderer paths diverge | One asset source lacks occlusion | Use one shared occluder component in both branches and test fallback independent of source |
| Occluder remains after face loss | Invisible stale blocker | Timestamp freshness, explicit visibility false, and reacquisition tests |
| GPU ownership error | Leak, crash, or black frame | SceneView node lifecycle plus explicit material ownership; repeated-entry POCO check |

## Scope Discipline

Intentionally untouched:

- backend and API contracts;
- asset upload/publication workflow;
- GLB node structure and materials;
- reservation, appointment, messaging, and notification flows;
- ear/hair segmentation and ARCore;
- minimum-device compatibility claims beyond existing fallbacks.

## Open Questions

No product question blocks task breakdown. These implementation measurements
are intentionally resolved by the spike rather than guessed in the plan:

- the smallest acceptable relative landmark-depth multiplier;
- the smallest non-flickering occlusion bias;
- whether SceneView's current vertex update meets 24 FPS without a lower-level
  Filament buffer adapter;
- whether the explicit aspect-fill mapping aligns with the POCO preview crop.

Any answer that expands dependencies, camera ownership, scope, or patient-facing
claims requires a plan/spec revision and human approval.

## Source Basis

- MediaPipe Face Landmarker Android output and 478-landmark mesh:
  https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker/android
- MediaPipe Java `FACE_LANDMARKS_TESSELATION` API:
  https://developers.google.com/edge/api/mediapipe/java/com/google/mediapipe/tasks/vision/facelandmarker/FaceLandmarker
- Filament color/depth write and depth-test controls:
  https://google.github.io/filament/main/materials.html#materialdefinitions-material-block-rasterization
- SceneView 4.18.0 source used from the pinned Gradle artifact:
  `MaterialLoader.createOcclusionInstance`, `Geometry.update`, and
  `GeometryNode.destroy`.
