# Eyecare 3D Frame Try-On

## Problem Statement

How might we help remote patients confidently shortlist and reserve real frames
for an upcoming appointment using a believable 3D preview, without claiming to
replace professional fitting or requiring a full-scale 3D asset operation?

## Recommended Direction

Build a **reservation-first, curated 3D try-on pilot** for three real frame
variants. A patient browses the existing catalog, previews a frame on their
face, and reserves the physical variant for an upcoming appointment. Final
sizing and optical suitability remain clinic responsibilities.

The capstone contribution is not AR in isolation. It is the complete connection
between remote visualization, real clinic inventory, an upcoming appointment,
and physical frame availability:

```text
Browse catalog
    -> Open frame
    -> Try on in 3D
    -> Select preferred variant
    -> Reserve for upcoming appointment
    -> Confirm physical fit at the clinic
```

The implementation should first prove one locally bundled and calibrated model.
Remote asset delivery, the reservation action, and two additional models follow
only after that vertical slice is stable.

## Key Assumptions to Validate

- [ ] **Patients recognize the virtual model as the physical product.** Test at
  least 15 participants with a blind match among three physical frames. At
  least 12 participants (80%) must select the correct counterpart, and the
  median shape-and-color similarity rating must be at least 4 out of 5.
- [ ] **Tracking remains believable during normal movement.** Test front-facing
  use, moderate head turns, tilt, and normal changes in camera distance on a
  budget device, a typical midrange device, and a recent high-end reference
  device. Median placement-stability rating must be at least 4 out of 5.
- [ ] **Three acceptable GLB assets can be produced without an in-house 3D
  specialist.** Time-box an AI-assisted multi-view trial for one physical frame
  before committing to the other two. Reject output with materially incorrect
  lens shape, bridge, color, or temple style.
- [ ] **Try-on improves reservation confidence.** In the user study, record
  willingness to reserve before and after try-on rather than relying only on a
  general satisfaction score.
- [ ] **Clinic staff can manage the asset workflow.** Ask a staff user to upload,
  calibrate, publish, replace, and disable a model using written instructions.
- [ ] **Three frames are operationally reasonable for an appointment.** Confirm
  the proposed limit with clinic staff before changing the existing five-item
  reservation contract.

## MVP Scope

### Frame selection

Select three visually distinct, scan-friendly physical frames:

1. A full-rim rectangular frame.
2. A full-rim round or oval frame.
3. A full-rim cat-eye or browline frame.

Avoid rimless, semi-rimless, transparent, highly reflective, and thin wire
frames in the pilot. The exact catalog variants remain open until suitable
physical inventory is identified.

### Android experience

- Preserve CameraX for the front-camera preview.
- Preserve MediaPipe Face Landmarker but use its facial transformation matrix
  for three-dimensional pose rather than deriving only a flat overlay from a
  small landmark subset.
- Replace bitmap-based frame drawing with a real-time 3D renderer that loads
  optimized GLB assets.
- Track translation, scale, pitch, yaw, and roll.
- Support three AR-enabled physical frame variants.
- Allow variant switching within the try-on experience.
- Provide explicit loading, download-failure, unsupported-device,
  camera-permission, and no-face states.
- Offer **Reserve this frame** from the try-on flow and use the existing
  appointment-linked frame reservation workflow.
- Display a clear **visual preview only; final fit is confirmed at the clinic**
  disclosure.
- Process the camera feed on-device; do not upload or retain frames, selfies, or
  video.
- Cache downloaded models and make ordinary catalog images the fallback when
  try-on is unavailable.

The application keeps its existing `minSdk` of 26. The supported floor for the
3D try-on feature is deliberately higher:

- Android 10 / API 29 or newer;
- 64-bit ARM device;
- at least 4 GB RAM;
- OpenGL ES 3.0;
- working front-facing camera; and
- connectivity for the initial model download.

Target a stable minimum of 24 frames per second, with 30 frames per second
preferred. Devices below the feature floor retain frame browsing and
reservation through catalog images.

### Asset production

Use AI as a production assistant, not as the authority for catalog identity:

1. Measure total frame width, lens width, lens height, bridge width, and temple
   length from the physical product.
2. Photograph the same frame from front, back, left, and right under diffuse
   lighting against a controlled background.
3. Generate a draft mesh with an offline multi-image-to-3D tool and export GLB.
4. Inspect the model against the physical frame.
5. Use a narrow Blender workflow to set the bridge-centered origin, apply
   real-world scale, remove obvious unwanted geometry, simplify the mesh,
   correct basic materials, and export the delivery GLB.
6. Upload the asset and calibrate it in the backend.
7. Publish it only after it passes the physical comparison and user-study
   threshold.

One team member owns this repeatable process even if AI generates the initial
mesh. The AI service is an offline authoring tool and is not a runtime mobile or
backend dependency.

Every delivery asset uses:

- GLB format;
- a bridge-centered origin;
- a documented, consistent forward/up axis convention;
- real-world scale;
- optimized geometry and textures; and
- a target download size of approximately 5 MB or less where practical.

### Backend and API

- Permit only authorized clinic staff to manage AR assets.
- Associate one published AR asset with a product variant.
- Store asset binaries in backend-managed file or object storage, not in the
  database and not through conversation attachments.
- Generate opaque storage names and retain the original name only as metadata.
- Validate file type and size before publication.
- Keep the previously published model active until a replacement validates.
- Represent asset processing with `processing`, `ready`, and `failed` states.
- Mark a variant AR-ready only when its model and required calibration are
  complete.
- Return an immutable asset URL, version, size, and calibration metadata.
- Keep ordinary product images separate from 3D assets.

Proposed patient API shape:

```json
{
  "ar_asset": {
    "url": "https://example.test/storage/ar/variants/uuid/model.glb",
    "format": "glb",
    "version": 1,
    "byte_size": 1842500,
    "checksum": "sha256-value",
    "status": "ready"
  },
  "calibration": {
    "frame_width_mm": 138,
    "lens_width_mm": 52,
    "bridge_width_mm": 18,
    "anchor_x": 0.0,
    "anchor_y": 0.03,
    "anchor_z": -0.01,
    "scale_multiplier": 1.0
  }
}
```

The current API example uses a USDZ asset reference while Android attempts to
load that reference as a bitmap. The implementation must replace this ambiguous
contract with an explicit GLB asset representation rather than silently changing
the meaning of `ar_asset_reference`.

### Reservation policy

The intended patient experience recommends a shortlist of no more than three
frames for one appointment. Three offers meaningful comparison without
unnecessarily increasing clinic preparation or held inventory.

The current backend and Android domain allow five items per reservation. The MVP
must not impose a contradictory client-only hard limit. Preserve five until
clinic staff confirm the operational change; if confirmed, update the server,
API contract, Android domain constant, validation, copy, and tests together.

## Delivery Order

1. Render one locally bundled GLB without face tracking.
2. Attach that model to MediaPipe pose and stabilize normal head movement.
3. Calibrate it against its physical counterpart.
4. Validate it on the three device classes.
5. Implement backend upload, publication, and remote Android loading.
6. Connect try-on to the existing appointment-linked reservation flow.
7. Produce and validate the remaining two assets.
8. Conduct the blind-match and reservation-confidence study.

## Success Criteria

- Three real catalog variants support a recognizable 3D preview.
- At least 80% of study participants correctly match each virtual model to its
  physical counterpart.
- Median visual similarity and placement stability are each at least 4 out of
  5.
- Tracking remains at or above 24 FPS on the declared minimum test device under
  the study conditions.
- A linked patient with an eligible upcoming appointment can complete a frame
  reservation from the try-on journey.
- Clinic staff can replace a model without modifying or rebuilding Android.
- Unsupported devices retain image-based browsing and reservation.
- The experience makes no guarantee of physical or clinical fit.

## Not Doing (and Why)

- **Full-catalog 3D coverage** - three polished examples prove the system more
  effectively than many unreliable assets.
- **Guaranteed sizing or fit** - monocular camera tracking and simple
  calibration are insufficient for a clinical fit guarantee.
- **Pupillary-distance or facial measurement** - this introduces accuracy and
  clinical-risk concerns unrelated to remote shortlisting.
- **Automatic 3D reconstruction in the product** - reflective, thin eyewear is
  difficult to reconstruct reliably, and cleanup still needs human validation.
- **Patient-uploaded models** - catalog assets must correspond to clinic-managed
  inventory.
- **Stored selfies or camera footage** - unnecessary for real-time processing
  and creates avoidable privacy risk.
- **USDZ and iOS support** - the current client is Android, so GLB is the pilot
  delivery format.
- **Perfect face, hair, ear, and temple occlusion** - valuable future polish but
  not required to validate the reservation journey.
- **Social sharing and beauty effects** - these do not support the core patient
  job.
- **A runtime AI-generation integration** - it adds cost, nondeterminism, and an
  external dependency without improving the validated patient flow.
- **A complex asset-processing platform** - premature for three curated models.

## Open Questions

- Which exact three physical catalog variants meet the pilot selection criteria?
- Can clinic staff operationally commit to preparing at most three frames, or
  should the established five-item limit remain?
- Which available physical phones will represent the minimum, typical, and
  reference device classes in testing?
- Who owns the asset-production workflow and its acceptance checklist?
- What project milestone is the cutoff for falling back to a polished 2.5D
  preview if the first GLB cannot meet the similarity or performance gates?

