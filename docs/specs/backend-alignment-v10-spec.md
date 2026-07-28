# Backend Alignment V10 — Versioned Prescriptions and Feedback Retirement

Status: Approved — Android Phases 1–3 complete (2026-07-28); implementation not started

## Objective

Align the implemented Android V9 application with the changes already present
in the current 2026-07-28 patient API:

1. replace the legacy flat prescription model with the current versioned,
   nested prescription resource; and
2. remove the retired clinic-feedback feature and reduce the Android API
   allowlist from 34 to 33 routes.

Billing Record simplification and required Appointment linkage for Frame
Reservations are deliberately deferred. They will receive a separate Android
alignment specification after their final implemented API contract is
available.

The Android app has not been deployed. V10 therefore performs a clean cutover
without retaining compatibility for the retired prescription fields or
feedback route.

## Sources of Truth

1. `docs/API_CONTRACT.md` — authoritative patient API contract at the current
   backend repository state dated 2026-07-28.
2. `docs/BACKEND_CONTEXT.md` — supporting backend domain and workflow context.
3. Android commit `dfa9969` — completed V9 client baseline.

The approved future requirements in
`docs/billing-record-simplification-spec.md` and
`docs/frame-reservation-appointment-linkage-spec.md` are acknowledged but are
not implementation sources for V10.

## Confirmed Assumptions and Android Decisions

1. V9 is complete; V10 contains only changes beyond V9.
2. No production users or stored Android records require legacy compatibility.
3. `GET /prescriptions` returns only current leaf versions and remains
   paginated.
4. `GET /prescriptions/{id}` can return a current or historical superseded
   version belonging to the authenticated patient.
5. Android can follow `previous_prescription_id` backward from a current
   prescription to its earlier versions. It does not infer or request a next
   version.
6. All prescription measurement fields are nullable strings. Android preserves
   their text and does not perform clinical calculations or normalization.
7. The Home expiry warning is removed because the new resource has no
   `expires_at`.
8. Home instead shows a compact **Current prescription** summary when one is
   available, with navigation to its detail.
9. Clinic feedback is removed completely rather than hidden or redirected.
10. Frame ratings remain a separate supported feature and are unchanged.
11. The approved route allowlist contains exactly 33 routes.

Approval of this specification confirms these decisions.

## Contract Delta

### Versioned prescriptions

The previous prescription resource exposed flat measurements and validity
metadata:

```text
od_sphere
od_cylinder
od_axis
od_add
od_prism
od_base
os_sphere
os_cylinder
os_axis
os_add
os_prism
os_base
pd
prescribed_at
expires_at
notes
```

Those fields are replaced by:

```text
id
appointment_id
previous_prescription_id
is_current
date
measurements
    main
        od
            value
            sphere
            cylinder
        os
            value
            sphere
            cylinder
    add
        od
            value
            sphere
            cylinder
        os
            value
            sphere
            cylinder
remarks
```

`GET /prescriptions` returns paginated current versions only. Superseded
versions remain patient-accessible through `GET /prescriptions/{id}`.

### Feedback retirement

The following route has been removed:

```text
POST /api/v1/feedback
```

The API appendix now contains exactly 33 routes. The retirement applies to
clinic feedback only. It does not remove:

```text
POST /api/v1/job-order-items/{item}/rating
```

Complaints also remain a separate backend remediation workflow, but adding a
patient complaint interface is outside V10.

## Scope

### In Scope

- Replace prescription DTOs and domain models with the nested versioned
  structure.
- Map the new DTOs to domain models at the repository boundary.
- Update prescription list and detail state, formatting, and UI.
- Support backward navigation through the prescription version chain.
- Remove all expiry, validity, axis, prism, base, PD, and legacy-notes
  behavior.
- Replace the Home prescription-expiry warning with a current-prescription
  summary and detail link.
- Delete the clinic Feedback Retrofit service, DTO, domain model, repository,
  DI module, ViewModel, screen, navigation route, and tests.
- Remove the Appointment detail feedback action and obsolete feedback state.
- Remove `AppointmentStatus.canLeaveFeedback`.
- Preserve the existing **View Intake** action for fulfilled Appointments even
  though it currently shares a container with the feedback action.
- Update the approved API route list and route test from 34 to 33.
- Update affected tests and `CONTEXT.md`.

### Out of Scope

- Billing Record models, routes, screens, labels, or Invoice replacement.
- Required Appointment selection for Frame Reservations.
- Reservation DTO, domain, error, or UI changes.
- Backend implementation or documentation changes.
- Complaint submission or complaint management.
- Frame-rating behavior or UI changes.
- Prescription creation, editing, printing, or amendment.
- Clinical interpretation, validation, or calculation of measurements.
- Room persistence or schema changes.
- Navigation redesign unrelated to removing Feedback or opening a
  prescription.
- New dependencies.
- Any compatibility adapter for old prescription fields or `/feedback`.

## Current Android Gap Analysis

| Current V9 behavior | Required V10 behavior |
|---|---|
| Prescription DTO and domain model use flat OD/OS fields. | Use nested Main/ADD and OD/OS measurement types. |
| Prescription uses `prescribedAt`, `expiresAt`, and `notes`. | Use `date`, `isCurrent`, version linkage, and `remarks`. |
| List sorts by `prescribedAt`. | Use the new `date` field. |
| List/detail show validity and expiration information. | Remove all validity and expiration presentation. |
| Detail shows axis, prism, base, and PD. | Show only documented Main/ADD values, sphere, and cylinder. |
| Home searches for expired or soon-expiring prescriptions. | Home optionally shows the latest current prescription. |
| Appointment detail exposes Leave feedback for fulfilled visits. | Fulfilled visits have no clinic-feedback action. |
| Feedback has API, data, domain, DI, ViewModel, screen, and route layers. | Delete the feature end to end. |
| API allowlist contains `POST /feedback` and expects 34 routes. | Remove the route and require exactly 33. |

## Functional Requirements

### 1. Prescription Domain and Transport

- Introduce explicit nested DTOs for:
  - the full measurements object;
  - a Main or ADD measurement group; and
  - an individual eye measurement.
- Use `@SerialName` for snake-case fields.
- Model the same structure with serialization-free domain classes.
- Map DTOs to domain models only in `PrescriptionRepositoryImpl`.
- Preserve `appointmentId`, `previousPrescriptionId`, `isCurrent`, `date`, and
  `remarks`.
- Preserve nullable measurement strings exactly.
- Do not coerce missing values to zero or an empty clinical measurement.
- Retain the current repository paging interface unless Phase 2 finds a
  mechanical rename necessary.

Recommended domain shape:

```kotlin
data class Prescription(
    val id: Int,
    val appointmentId: Int,
    val previousPrescriptionId: Int?,
    val isCurrent: Boolean,
    val date: String,
    val measurements: PrescriptionMeasurements,
    val remarks: String?,
)

data class PrescriptionMeasurements(
    val main: PrescriptionMeasurementGroup,
    val add: PrescriptionMeasurementGroup,
)

data class PrescriptionMeasurementGroup(
    val od: EyeMeasurement,
    val os: EyeMeasurement,
)

data class EyeMeasurement(
    val value: String?,
    val sphere: String?,
    val cylinder: String?,
)
```

### 2. Prescription List

- Keep **Prescriptions** as the destination title.
- Display current versions returned by the paginated endpoint.
- Present the prescription date as the primary temporal context.
- Show a concise Main OD/OS summary without inventing labels or units not
  present in the contract.
- Use an em dash for a missing measurement value.
- Do not show Valid, Expiring, Expired, or expiry-date badges.
- Sort by `date` descending only if local sorting remains necessary; otherwise
  preserve authoritative server order.
- The empty state explains that finalized prescriptions will appear after an
  eye examination.
- Existing loading, retry, and pagination behavior remains.

### 3. Prescription Detail and Version History

- Display **Current prescription** when `isCurrent` is true.
- Display **Previous prescription** when `isCurrent` is false.
- Present Main and ADD groups separately.
- Within each group, compare OD and OS consistently.
- For each eye, render the documented `value`, `sphere`, and `cylinder` fields.
- Render `remarks` only when non-blank.
- Do not show removed axis, prism, base, PD, validity, expiry, or notes fields.
- Show **View previous version** when `previousPrescriptionId` is non-null.
- Reuse `PrescriptionDetail(previousPrescriptionId)` for historical
  navigation.
- Normal Back returns to the newer version, preserving the version chain in
  the navigation back stack.
- Do not attempt to derive or expose a next-version action.
- A historical record that is unavailable or outside patient scope uses the
  existing not-found/error behavior without exposing identifiers from another
  patient.

### 4. Home Prescription Summary

- Replace `expiringPrescription` with `currentPrescription` in Home state.
- Remove all `LocalDate` expiry-window filtering.
- From the first prescription page, select the latest record where
  `isCurrent == true`, using `date` as a deterministic fallback.
- Display a compact **Current prescription** card with its date.
- Provide **View details**, navigating to `PrescriptionDetail(id)`.
- Do not show renewal warnings because the backend no longer provides an
  expiry date.
- If the prescription request fails, omit this optional section while keeping
  independently loaded Home content usable.

### 5. Clinic Feedback Removal

- Delete:
  - `FeedbackApiService`;
  - `FeedbackDtos`;
  - `Feedback`;
  - `FeedbackRepository` and `FeedbackRepositoryImpl`;
  - `FeedbackModule`;
  - `FeedbackViewModel` and `FeedbackScreen`;
  - `FeedbackSubmit`;
  - `FeedbackViewModelTest`; and
  - feedback-only fixtures or imports.
- Remove `onLeaveFeedback` from Appointment detail and navigation.
- Remove `hasFeedback` from `AppointmentDetailUiState`.
- Remove feedback-derived height and action-layout branches from Appointment
  detail.
- Make bottom-action visibility independent of feedback so fulfilled
  Appointments retain their existing **View Intake** access.
- Remove `canLeaveFeedback` from `AppointmentStatus` and its unit tests.
- Keep the normal fulfilled Appointment history presentation.
- Do not replace the action with frame rating or complaints.

### 6. Route Allowlist

- Remove `POST /api/v1/feedback` from `ApprovedApiRoutes`.
- Change route-count assertions from 34 to 33.
- Keep `POST /api/v1/job-order-items/{item}/rating`.
- Require Retrofit annotation discovery and the approved allowlist to match
  exactly.

## Architecture

V10 retains the existing MVVM + Clean flow:

```text
Retrofit service
    -> Kotlinx Serialization DTO
    -> repository mapping
    -> domain model
    -> ViewModel StateFlow
    -> Compose UI
```

Responsibilities remain:

- data layer: transport decoding and DTO-to-domain mapping;
- domain layer: stable versioned prescription representation;
- presentation layer: labels, formatting, navigation, and UI state;
- backend: patient scoping and authoritative current-version selection.

No prescription or other clinical data is stored in Room.

## Expected Project Surface

```text
app/src/main/java/com/eyecare/app/
├── data/
│   ├── remote/
│   │   ├── api/PrescriptionApiService.kt
│   │   └── dto/PrescriptionDtos.kt
│   └── repository/PrescriptionRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── AppointmentV1.kt
│   │   └── Prescription.kt
│   └── repository/PrescriptionRepository.kt
├── presentation/
│   ├── appointments/
│   │   ├── AppointmentDetailViewModel.kt
│   │   └── AppointmentDetailScreen.kt
│   ├── home/
│   │   ├── HomeViewModel.kt
│   │   └── HomeScreen.kt
│   ├── navigation/
│   │   ├── Routes.kt
│   │   └── NavGraph.kt
│   └── prescriptions/
│       ├── PrescriptionViewModel.kt
│       └── PrescriptionScreens.kt
└── di/
    └── FeedbackModule.kt               # deleted

app/src/test/java/com/eyecare/app/
├── data/remote/
│   ├── ApprovedApiRoutes.kt
│   └── ApiRouteAllowlistTest.kt
├── domain/model/AppointmentStatusTest.kt
└── presentation/
    ├── feedback/FeedbackViewModelTest.kt  # deleted
    ├── home/HomeViewModelTest.kt
    └── prescriptions/PrescriptionViewModelTest.kt
```

Phase 2 must enumerate all Feedback files and references before deletion and
may refine the prescription test filenames after dependency analysis.

## Testing Strategy

Implementation follows test-driven slices after Phase 3 approval.

### DTO and repository tests

- Decode the complete documented prescription response.
- Decode every nullable measurement as null.
- Decode `previous_prescription_id` and `is_current`.
- Decode both current and historical detail resources.
- Map nested DTOs to domain models at the repository boundary.
- Retain list pagination metadata.
- Prove removed flat fields are not required by fixtures.

### ViewModel tests

- Load and paginate the current-version prescription list.
- Sort or preserve ordering according to the Phase 2 decision.
- Load a current detail.
- Load a historical detail.
- Expose previous-version navigation data.
- Home selects the latest current prescription.
- Home remains usable when prescription loading fails.
- No expiry-window logic remains.

### Presentation and navigation tests

- Render full, partial, and empty Main/ADD measurement groups.
- Use an em dash for missing values.
- Show current versus previous labels correctly.
- Show remarks only when non-blank.
- Show previous-version action only when an ID exists.
- Navigate through successive previous-version IDs with normal Back behavior.
- Appointment detail contains no clinic-feedback action.
- Navigation contains no Feedback destination.

### Removal and route tests

- No production or test source references the Feedback feature.
- No source contains `@POST("feedback")`.
- API allowlist contains exactly 33 routes.
- Retrofit route discovery matches the allowlist.
- Frame-rating route and tests remain intact.
- No prescription production code references the removed flat fields.

### Verification Commands

```powershell
.\gradlew testDebugUnitTest --tests "*Prescription*"
.\gradlew testDebugUnitTest --tests "*HomeViewModelTest"
.\gradlew testDebugUnitTest --tests "*AppointmentStatusTest"
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew ktlintCheck
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
```

Focused tests run after each slice. The full suite, lint, and debug assembly run
before V10 is complete. `assembleDebug` is mandatory after Android changes.

## Boundaries

### Always

- Follow the current `API_CONTRACT.md`.
- Use Kotlinx Serialization, never Gson.
- Map DTOs to domain models at the repository boundary.
- Use `StateFlow` and sealed UI state.
- Preserve nullable clinical values without interpretation.
- Fail safely when a historical record cannot be loaded.
- Keep tokens, prescription data, and response bodies out of logs.
- Write failing tests before each behavior change.
- Run focused tests and `.\gradlew assembleDebug` after implementation
  changes.

### Ask First

- Add a dependency.
- Add or change an endpoint beyond the documented prescription and feedback
  delta.
- Change navigation beyond Feedback removal and prescription-detail links.
- Persist prescriptions in Room.
- Expand V10 to Billing Records or appointment-linked reservations.
- Add complaint or frame-rating behavior.

### Never

- Retain compatibility aliases for removed prescription fields.
- Reconstruct removed measurements from unrelated fields.
- Treat a missing value as zero.
- Reintroduce clinic feedback under another route.
- Treat frame ratings as clinic feedback.
- Log patient clinical data.
- Modify the backend documents as part of Android implementation.

## Success Criteria

- [ ] Prescription list and detail decode the current nested resource.
- [ ] DTOs do not leak into domain or presentation code.
- [ ] Current and historical versions display accurately.
- [ ] Previous versions can be opened through `previous_prescription_id`.
- [ ] Missing measurements display safely without invented values.
- [ ] No expiry, validity, axis, prism, base, PD, or old-notes UI remains.
- [ ] Home shows a current-prescription summary instead of an expiry warning.
- [ ] No clinic Feedback API, data, domain, DI, presentation, navigation, or
  test code remains.
- [ ] Fulfilled Appointments remain visible without a feedback action.
- [ ] Fulfilled Appointments retain their existing View Intake action.
- [ ] Frame ratings remain unchanged.
- [ ] API allowlist and discovered Retrofit services contain exactly 33 routes.
- [ ] No legacy prescription or Feedback compatibility layer remains.
- [ ] `CONTEXT.md` describes the completed V10 behavior.
- [ ] Focused and full tests pass.
- [ ] Ktlint and Android lint pass.
- [ ] Debug assembly succeeds.

## Open Questions

None. Billing Record and Frame Reservation integration questions are deferred
with those features and do not block V10.

## Deferred Follow-Up

Create a separate Android alignment specification when the implemented backend
contract documents:

- `/billing-records` list/detail routes and exact response envelopes; and
- required `appointment_id`, embedded Appointment context, and stable errors
  for Frame Reservation creation.

No Billing Record or reservation-linkage production code is changed in V10.

## Phase Gate

Phases 1–3 were approved by the project owner on 2026-07-28.

Implementation is authorized by the approved task breakdown but has not
started. Do not begin it until the project owner gives a separate instruction
to proceed.
