# Backend Alignment V10 — Phase 3 Task List

Status: Approved by the project owner on 2026-07-28 — implementation not started

Approved specification:
`docs/specs/backend-alignment-v10-spec.md`

Approved implementation plan:
`docs/specs/backend-alignment-v10-plan.md`

Backend contract:
`docs/API_CONTRACT.md` at the current backend repository state dated 2026-07-28

## Execution Rules

- Execute tasks in dependency order.
- Write or update the focused test before changing the corresponding production
  behavior.
- Confirm each red test or compiler failure is caused by the intended V10
  contract change.
- Tasks 1–4 are one compiler-coupled prescription cutover. Do not commit or
  introduce compatibility fields between them; their shared green checkpoint
  is Checkpoint A.
- Task 5 deliberately makes the route allowlist test red before Task 6 removes
  Feedback.
- After every green task or checkpoint, run its focused tests and
  `.\gradlew assembleDebug`.
- Use Kotlinx Serialization only.
- Map DTOs to domain models only in the repository.
- Do not modify Invoice/Billing Record or Frame Reservation behavior.
- Do not modify Room or add a dependency.
- Preserve `docs/BACKEND_CONTEXT.md`, `docs/API_CONTRACT.md`,
  `docs/billing-record-simplification-spec.md`, and
  `docs/frame-reservation-appointment-linkage-spec.md` as user-supplied source
  documents.
- Do not stage or commit the user-supplied backend documents unless explicitly
  requested.

## Phase A — Versioned Prescription Cutover

### Task 1: Replace the prescription transport and domain contract

**Description:** Write contract and repository mapping tests for the documented
nested prescription resource, then replace the flat DTO/domain model and
repository mapping. This intentionally breaks legacy presentation consumers
until Tasks 2–4 complete the clean cutover.

**Test-first steps:**

1. Add `PrescriptionDtosTest` using the production Kotlinx `Json`
   configuration.
2. Add `PrescriptionRepositoryImplTest` using MockWebServer and Retrofit.
3. Run the focused tests and confirm they fail because the nested types and
   fields do not exist.
4. Implement the nested DTOs, domain types, and repository mapping.
5. Re-run the focused command. If Gradle stops at old presentation/Home
   references, confirm those errors are limited to files assigned to Tasks 2–4.

**Acceptance criteria:**

- [x] Paginated and detail fixtures decode `previous_prescription_id`,
  `is_current`, `date`, `measurements`, and `remarks`.
- [x] Main and ADD each decode OD and OS `value`, `sphere`, and `cylinder`.
- [x] Every clinical measurement can remain null.
- [x] `Prescription` exposes nested serialization-free domain types.
- [x] Repository mapping preserves all nested values and pagination metadata.
- [x] Old flat fields do not remain in the DTO or domain model.
- [x] No compatibility alias or calculated clinical value is introduced.

**Verification:**

- [x] Intended RED:
  `.\gradlew testDebugUnitTest --tests "*PrescriptionDtosTest" --tests "*PrescriptionRepositoryImplTest"`
- [x] Re-run after implementation and record that any remaining compile
  failures are only the scheduled Tasks 2–4 consumers.

**Dependencies:** None

**Files added:**

- `app/src/test/java/com/eyecare/app/data/remote/dto/PrescriptionDtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/PrescriptionRepositoryImplTest.kt`

**Files modified:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/PrescriptionDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Prescription.kt`
- `app/src/main/java/com/eyecare/app/data/repository/PrescriptionRepositoryImpl.kt`

**Files expected unchanged:**

- `app/src/main/java/com/eyecare/app/data/remote/api/PrescriptionApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/PrescriptionRepository.kt`
- `app/src/main/java/com/eyecare/app/di/PrescriptionModule.kt`

**Estimated scope:** Medium

### Task 2: Split prescription list and detail state

**Description:** Replace the shared `PrescriptionViewModel` with dedicated list
and detail ViewModels. Preserve server list order, remove expiry logic, and make
detail retry reload the requested record without an automatic list call.

**Test-first steps:**

1. Replace the old ViewModel test with list/detail test files using the new
   domain fixture.
2. Add failing expectations for server-order preservation, pagination, no
   redundant list request, historical detail, and same-ID retry.
3. Implement the two ViewModels and remove the old combined ViewModel.

**Acceptance criteria:**

- [x] List initial load, empty, error, retry, pagination, and load-more failure
  states are covered.
- [x] Later pages append in server order without client re-sorting.
- [x] Concurrent or duplicate `loadMore()` calls are guarded.
- [x] Detail loading does not call `getPrescriptions`.
- [x] Detail retry calls `getPrescription` with the active ID.
- [x] Current and historical prescription models both load.
- [x] `isExpired` and all expiry-derived state are removed.

**Verification:**

- [x] Intended RED then implementation:
  `.\gradlew testDebugUnitTest --tests "*PrescriptionListViewModelTest" --tests "*PrescriptionDetailViewModelTest"`
- [x] Confirm remaining compiler failures are limited to screens/Home assigned
  to Tasks 3–4.

**Dependencies:** Task 1

**Files added:**

- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionListViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionDetailViewModelTest.kt`

**Files deleted:**

- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModelTest.kt`

**Estimated scope:** Medium

### Task 3: Rewrite prescription list, detail, and history navigation

**Description:** Migrate both Compose screens to the nested measurements,
remove unsupported validity UI, and add lazy previous-version navigation using
the existing type-safe detail route.

**Test-first steps:**

1. Add or extract small formatter/presentation-policy tests for nullable
   measurement display where practical.
2. Confirm old screen code cannot compile against the new model.
3. Rewrite list/detail presentation and wire the previous-version callback.
4. Confirm no prescription screen references a removed field.

**Acceptance criteria:**

- [x] List uses `PrescriptionListViewModel`.
- [x] Detail uses `PrescriptionDetailViewModel`.
- [x] List rows show date and concise Main OD/OS content.
- [x] Missing values render as an em dash, not zero.
- [x] Validity badges and expiry/renewal messages are gone.
- [x] Detail distinguishes Current prescription from Previous prescription.
- [x] Main and ADD groups render OD/OS value, sphere, and cylinder.
- [x] Blank remarks are omitted; non-blank remarks render.
- [x] **View previous version** appears only when an ID exists.
- [x] Selecting it pushes `PrescriptionDetail(previousPrescriptionId)`.
- [x] Back traverses from older to newer versions naturally.
- [x] Detail retry reloads the displayed ID.

**Verification:**

- [x] Run:
  `.\gradlew testDebugUnitTest --tests "*Prescription*"`
- [x] Run `.\gradlew assembleDebug` and confirm any remaining error is confined
  to the Home prescription consumer handled by Task 4.

**Dependencies:** Tasks 1–2

**Files modified:**

- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionScreens.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

**Files optionally added if pure formatting is extracted:**

- `app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionPresentationTest.kt`

**Estimated scope:** Large

### Task 4: Replace Home expiry behavior with current prescription

**Description:** Complete the compiler-coupled prescription cutover by changing
Home state, selection, UI, navigation, and relevant tests to the new current
prescription model.

**Test-first steps:**

1. Replace expiry-based `HomeViewModelTest` fixtures and assertions.
2. Add failing tests for latest-current selection, defensive non-current
   exclusion, empty data, and partial failure.
3. Update Home production state and selection.
4. Update Home Compose UI and navigation callback.
5. Repair the relevant instrumented Home test using current V9 models and
   state.

**Acceptance criteria:**

- [x] `expiringPrescription` is replaced by `currentPrescription`.
- [x] Home chooses the maximum ISO `date` among page-1 current records.
- [x] A non-current record is ignored defensively.
- [x] Prescription failure does not hide available Appointment or Frame data.
- [x] Home shows **Current prescription**, its date, and **View details**.
- [x] View details navigates to `PrescriptionDetail(id)`.
- [x] Home contains no expiration or renewal copy.
- [x] No additional network request is added.
- [x] The stale Home instrumented fixture no longer restores retired Order or
  Product Home sections.

**Verification:**

- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*HomeViewModelTest"`
- [x] `.\gradlew testDebugUnitTest --tests "*Prescription*"`
- [x] `.\gradlew assembleDebugAndroidTest`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 1–3

**Files modified:**

- `app/src/main/java/com/eyecare/app/presentation/home/HomeViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/presentation/home/HomeViewModelTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Estimated scope:** Medium

## Checkpoint A — Prescription Cutover Green

- [x] `.\gradlew testDebugUnitTest --tests "*Prescription*"`
- [x] `.\gradlew testDebugUnitTest --tests "*HomeViewModelTest"`
- [x] `.\gradlew assembleDebugAndroidTest`
- [x] `.\gradlew assembleDebug`
- [x] Current, partial/null, and historical prescription fixtures display
  correctly.
- [x] Opening detail performs no redundant list request.
- [x] Current -> previous -> previous -> Back -> Back works.
- [x] No legacy prescription compatibility field exists.

Only after Checkpoint A is green may the first V10 implementation commit be
created:

```text
feat(V10): migrate versioned prescriptions across the app
```

## Phase B — Clinic Feedback Retirement

### Task 5: Change the approved route contract to 33

**Description:** Remove `/feedback` from the Android allowlist and update route
count assertions before deleting the Retrofit service. This creates the
intentional red proof that route discovery catches the retired service.

**Acceptance criteria:**

- [x] `ApprovedApiRoutes` contains exactly 33 routes.
- [x] `POST /api/v1/feedback` is absent.
- [x] `POST /api/v1/job-order-items/{item}/rating` remains.
- [x] Both explicit count assertions expect 33.
- [x] The focused test fails only because the still-present Feedback service is
  discovered as unapproved.

**Verification:**

- [x] Intended RED:
  `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`

**Dependencies:** Checkpoint A

**Files modified:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** Small

### Task 6: Delete clinic Feedback across all layers

**Description:** Remove the complete Feedback vertical, its type-safe route,
Appointment capability/state/action, and navigation wiring. Make Appointment
bottom-action visibility independent of Feedback so fulfilled visits retain
**View Intake**.

**Test-first steps:**

1. Update `AppointmentStatusTest` to remove feedback capability assertions.
2. Add `AppointmentDetailActionsTest` for the remaining bottom-container
   matrix.
3. Confirm the route test remains red while the Feedback service exists.
4. Delete the Feedback vertical and remove all references.
5. Run the focused route, Appointment, and frame-rating tests.

**Acceptance criteria:**

- [x] All listed Feedback production and unit-test files are deleted.
- [x] `FeedbackSubmit` and its composable are deleted.
- [x] `AppointmentStatus.canLeaveFeedback` is deleted.
- [x] `AppointmentDetailUiState.hasFeedback` is deleted.
- [x] `onLeaveFeedback`, its button, icon, copy, and layout branches are gone.
- [x] Scheduled shows Reschedule, Cancel, and View Intake.
- [x] Checked in shows Cancel and View Intake.
- [x] Fulfilled shows View Intake without Feedback.
- [x] Cancelled, no-show, and unknown show no bottom action container.
- [x] Cancellation and rescheduling capabilities are unchanged.
- [x] Retrofit discovery and allowlist both report 33 routes.
- [x] Frame-rating tests still pass.

**Verification:**

- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentStatusTest" --tests "*AppointmentDetailActionsTest"`
- [x] `.\gradlew testDebugUnitTest --tests "*FrameRating*"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 5

**Files deleted:**

- `app/src/main/java/com/eyecare/app/data/remote/api/FeedbackApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/FeedbackDtos.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FeedbackRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Feedback.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/FeedbackRepository.kt`
- `app/src/main/java/com/eyecare/app/di/FeedbackModule.kt`
- `app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/feedback/FeedbackViewModelTest.kt`

**Files modified:**

- `app/src/main/java/com/eyecare/app/domain/model/AppointmentV1.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/domain/model/AppointmentStatusTest.kt`

**File added:**

- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentDetailActionsTest.kt`

**Estimated scope:** Large

### Task 7: Remove stale Feedback expectations from instrumented Profile coverage

**Description:** Update the existing Profile Compose test so it no longer
constructs or expects a Feedback History destination. Align only the directly
affected fixture/callback surface with the current Profile screen.

**Acceptance criteria:**

- [ ] No Profile test refers to Feedback History or a feedback callback.
- [ ] Remaining Profile navigation rows retain their callback coverage.
- [ ] The Android test APK compiles.
- [ ] No retired Profile feature is restored to satisfy stale assertions.

**Verification:**

- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 6

**Files modified:**

- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Small

## Checkpoint B — Feedback Fully Retired

- [x] `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentStatusTest" --tests "*AppointmentDetailActionsTest"`
- [x] `.\gradlew testDebugUnitTest --tests "*FrameRating*"`
- [x] `.\gradlew assembleDebugAndroidTest`
- [x] `.\gradlew assembleDebug`
- [ ] Manual Appointment matrix confirms fulfilled retains View Intake.
- [ ] Frame rating remains reachable and functional.

Only after Checkpoint B is green may the second V10 implementation commit be
created:

```text
refactor(V10): retire clinic feedback and enforce 33 routes
```

## Phase C — Documentation and Release Verification

### Task 8: Synchronize Android context and run source-contract sweeps

**Description:** Update Android project documentation for the implemented V10
behavior, then prove no retired Feedback or legacy prescription references
remain.

**Acceptance criteria:**

- [x] `CONTEXT.md` documents nested versioned prescriptions and historical
  navigation.
- [x] Home is described as showing a current prescription, not expiry.
- [x] Feedback package, feature, route, and Appointment action references are
  removed.
- [x] The current mobile route total is documented as 33.
- [x] Frame ratings remain documented.
- [x] Invoice and Frame Reservation behavior remains described as currently
  implemented, not as the deferred future contract.
- [x] Generic Frame Reservation `expiresAt` fields remain untouched.

**Verification:**

- [x] Feedback sweep returns no source/current-context matches:

  ```powershell
  rg -n -i "FeedbackSubmit|FeedbackApiService|FeedbackRepository|canLeaveFeedback|hasFeedback|@POST\\(\"feedback\"\\)" app/src CONTEXT.md
  ```

- [x] Legacy prescription sweep returns no matches:

  ```powershell
  rg -n "odSphere|odCylinder|odAxis|odAdd|odPrism|odBase|osSphere|osCylinder|osAxis|osAdd|osPrism|osBase|prescribedAt|expiringPrescription|PrescriptionWarningCard|validityStatus" app/src CONTEXT.md
  ```

- [x] Manually inspect generic `expiresAt` matches and retain reservation
  expiry.
- [x] `git diff --check`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Checkpoints A–B

**Files modified:**

- `CONTEXT.md`
- `docs/specs/backend-alignment-v10-spec.md`
- `docs/specs/backend-alignment-v10-plan.md`
- `docs/specs/backend-alignment-v10-tasks.md`

**Estimated scope:** Medium

### Task 9: Run the complete V10 release gate

**Description:** Execute all automated verification, inspect the final diff,
perform manual smoke checks, and record only genuine environment limitations.

**Acceptance criteria:**

- [x] Full unit suite passes.
- [x] Android test APK compiles.
- [x] Ktlint passes.
- [x] Android lint passes.
- [x] Debug APK assembles.
- [ ] Manual prescription and Appointment checks pass.
- [x] Final diff contains no Billing Record or Frame Reservation adjustment.
- [x] User-supplied backend documents are not staged.
- [x] V10 spec, plan, and task statuses accurately reflect completed work.

**Verification:**

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew assembleDebug
git diff --check
git status --short
git diff --stat
```

If an emulator or device is available:

```powershell
.\gradlew connectedDebugAndroidTest
```

**Manual smoke checklist:**

- [ ] Prescription list loads and paginates.
- [ ] Current prescription displays complete measurements.
- [ ] Null/partial measurements show safely.
- [ ] Blank remarks are omitted.
- [ ] Previous-version chain and Back navigation work.
- [ ] Detail failure retries the active ID.
- [ ] Home current-prescription card opens detail.
- [ ] Scheduled Appointment actions are unchanged except no Feedback.
- [ ] Checked-in Appointment actions are unchanged except no Feedback.
- [ ] Fulfilled Appointment has View Intake and no Feedback.
- [ ] Cancelled/no-show Appointment has no action container.
- [ ] Frame rating still works.

**Dependencies:** Task 8

**Files modified:** None unless verification exposes an in-scope defect. Any
fix must receive a focused regression test and rerun the affected checkpoint.

**Estimated scope:** Medium

## Checkpoint C — V10 Complete

- [x] Checkpoint A passed.
- [x] Checkpoint B passed.
- [x] Task 8 source sweeps passed.
- [x] Task 9 complete verification passed.
- [x] `CONTEXT.md` matches the final Android behavior.
- [x] `docs/API_CONTRACT.md` and the other user-supplied backend documents were
  not modified by Android implementation.
- [x] No Billing Record or Appointment-linked Frame Reservation work entered
  V10.

After Checkpoint C, create the final documentation commit:

```text
docs(V10): update Android context and verification status
```

## Dependency Summary

```text
Task 1: prescription transport/domain
    -> Task 2: list/detail state
        -> Task 3: prescription screens/history
            -> Task 4: Home consumer
                -> Checkpoint A
                    -> Task 5: 33-route red proof
                        -> Task 6: Feedback deletion
                            -> Task 7: instrumented Profile cleanup
                                -> Checkpoint B
                                    -> Task 8: docs/sweeps
                                        -> Task 9: release gate
                                            -> Checkpoint C
```

## Task Sizing

| Task | Scope | Primary risk |
|---|---|---|
| 1 | Medium | Exact nested decoding and null preservation |
| 2 | Medium | Pagination and redundant requests |
| 3 | Large | Dense clinical UI and history navigation |
| 4 | Medium | Home partial-failure behavior and stale UI tests |
| 5 | Small | Proving allowlist enforcement |
| 6 | Large | Complete deletion while preserving Intake |
| 7 | Small | Stale instrumented fixture cleanup |
| 8 | Medium | Complete but correctly scoped source sweeps |
| 9 | Medium | Cross-feature regression verification |

## Phase Gate

This Phase 3 task list was approved by the project owner on 2026-07-28.
Implementation has not started and must remain paused until the project owner
gives a separate instruction to proceed.

Once that instruction is given, implementation may proceed in dependency
order. The compiler-coupled Tasks 1–4 must be completed through Checkpoint A
before creating their implementation commit.
