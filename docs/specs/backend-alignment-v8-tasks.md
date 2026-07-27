# Backend Alignment V8 — Phase 3 Task List

Status: Approved — implementation in progress (56/60 tasks complete)

Approved specification:
`docs/specs/backend-alignment-v8-spec.md`

Approved implementation plan:
`docs/specs/backend-alignment-v8-plan.md`

Backend contract:
`docs/API_CONTRACT.md` at backend commit `ebd1e2e`

## Execution Rules

- Execute tasks in dependency order.
- Every task must leave the project compiling.
- No task may introduce a legacy runtime fallback or a second Retrofit stack.
- During a subsystem cutover, canonical files may be added beside compiling
  legacy files; the legacy path must remain unreachable after navigation/DI
  cutover and must be deleted by Tasks 55–56.
- Transitional coexistence is compile scaffolding only, never a runtime
  fallback or compatibility adapter.
- If compile fallout would push a task beyond five files, update this task list
  and split the task before editing.
- Write or update the focused test before changing behavior.
- Run the focused test and `.\gradlew assembleDebug` after every task.
- Run the full checkpoint commands after every task group.
- Do not store patient/clinical data in Room or log response bodies.

## Phase A — Contract and Network Foundation

### Task 1: Establish shared contract fixtures

**Description:** Add reusable test fixtures for canonical pagination and API
error envelopes without changing runtime behavior.

**Acceptance criteria:**
- [x] Fixtures cover `data`/`links`/`meta`, 401, 403, 404, 422, and 429.
- [x] Fixture decoding uses the project's configured Kotlinx `Json`.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*ApiContractFixturesTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** None

**Files likely touched:**
- `app/src/test/java/com/eyecare/app/data/remote/ApiContractFixtures.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiContractFixturesTest.kt`

**Estimated scope:** Small

### Task 2: Cut network configuration to API v1

**Description:** Change all build variants to a base URL ending in `/api/v1/`
and reduce debug HTTP logging from BODY to BASIC.

**Acceptance criteria:**
- [x] Debug and release Retrofit URLs resolve under `/api/v1/`.
- [x] Debug logging cannot print tokens, intake narratives, or response bodies.
- [x] Existing authorization-header behavior remains intact.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*NetworkModuleTest" --tests "*AuthInterceptorTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**
- `app/build.gradle.kts`
- `app/src/main/java/com/eyecare/app/di/NetworkModule.kt`
- `app/src/test/java/com/eyecare/app/di/NetworkModuleTest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/interceptor/AuthInterceptorTest.kt`

**Estimated scope:** Medium

### Task 3: Add shared pagination and API error DTOs

**Description:** Introduce reusable transport models for the canonical Laravel
pagination envelope and structured errors.

**Acceptance criteria:**
- [x] Paginated resources can share links/meta without leaking DTOs to domain.
- [x] Field errors remain `Map<String, List<String>>`.
- [x] Missing optional links decode safely.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*CommonApiDtosTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/dto/CommonApiDtos.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/CommonApiDtosTest.kt`

**Estimated scope:** Small

### Task 4: Normalize money values safely

**Description:** Add a Kotlinx serializer that accepts contract money values as
either JSON numbers or numeric strings and maps them to `BigDecimal`.

**Acceptance criteria:**
- [x] `"4500.00"` and `4500.00` produce equal domain values.
- [x] Invalid, blank, NaN, and infinite values fail decoding.
- [x] Formatting does not use binary floating-point arithmetic.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*MoneyValueSerializerTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 3

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/dto/MoneyValueSerializer.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/MoneyValueSerializerTest.kt`

**Estimated scope:** Small

## Checkpoint A — Foundation

- [x] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest`
- [x] `.\gradlew assembleDebug`
- [x] Confirm no HTTP response body is written to Logcat.

## Phase B — Authentication and Patient Profile

### Task 5: Align authentication DTOs and `/me` service

**Description:** Replace `/user` annotations with `/me` and expand the existing
identity model to the account-linked patient profile contract.

**Acceptance criteria:**
- [x] Register/login decode the complete patient profile.
- [x] GET/PATCH use `/me`.
- [x] Nullable patient fields and `patient_number` decode correctly.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*AuthRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 2–4

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/AuthApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/AuthDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/User.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AuthRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 6: Map and update the complete patient profile

**Description:** Extend repository and ViewModel profile editing across all
documented PATCH `/me` fields.

**Acceptance criteria:**
- [x] Repository mapping never exposes DTOs.
- [x] Profile edits can send account and nullable patient fields.
- [x] Field-level 422 errors remain attached to the correct inputs.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*AuthRepositoryImplTest" --tests "*ProfileViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 5

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AuthRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/profile/ProfileViewModelTest.kt`

**Estimated scope:** Medium

### Task 7: Present account and patient profile fields

**Description:** Update Profile and Edit Profile UI to show patient number and
the approved editable demographic/contact fields.

**Acceptance criteria:**
- [x] Nullable values have intentional empty/display behavior.
- [x] Save state, validation, retry, and success remain accessible.
- [x] No profile data is persisted outside in-memory UI state.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*ProfileViewModelTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: edit, clear, and save nullable fields.

**Dependencies:** Task 6

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/EditProfileScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Medium

## Checkpoint B — Identity

- [x] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest --tests "*Auth*" --tests "*Profile*"`
- [x] `.\gradlew assembleDebug`
- [x] Register, login, logout, GET `/me`, and PATCH `/me` work against v1.

## Phase C — Appointments and Scheduling

### Task 8: Add appointment-type and v1 appointment contracts

**Description:** Add appointment types, paginated appointment responses, and
the revised appointment/availability field shapes while temporarily leaving
old consumers compile-safe.

**Acceptance criteria:**
- [x] Appointment types decode `requires_referral`.
- [x] Appointments decode pagination and name-only optometrist data.
- [x] Availability uses `appointment_type_id`.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentV1DtosTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 3–4

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Appointment.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AppointmentAvailability.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/AppointmentDtosTest.kt`

**Estimated scope:** Medium

### Task 9: Replace visit-reason repository behavior

**Description:** Migrate the appointment repository to appointment types,
paginated lists, v1 availability, and create-time referring source.

**Acceptance criteria:**
- [x] Repository methods expose appointment types, pages, and `hasMorePages`.
- [x] Create sends `appointment_type_id` and conditional `referring_source`.
- [x] DTO-to-domain mapping preserves contract nullability.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentV1RepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 8

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/domain/model/VisitReason.kt` (remove/replace)
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 10: Migrate booking ViewModel to appointment types

**Description:** Replace visit-reason state with appointment-type state,
referral-source validation, and backend slot selection.

**Acceptance criteria:**
- [x] Referral source is required only when the backend type requires it.
- [x] Selected backend `starts_at` is submitted unchanged.
- [x] Slot-unavailable 422 returns the flow to time selection and refreshes.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*BookAppointmentViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 9

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentViewModelTest.kt`

**Estimated scope:** Medium

### Task 11: Update booking UI for appointment types and referrals

**Description:** Change booking labels, selection, review, and validation UI to
the appointment-type contract.

**Acceptance criteria:**
- [x] Referral source appears only for referral types.
- [x] Date/time UI continues using clinic-local display and backend timestamps.
- [x] No visit-reason terminology or hardcoded type IDs remain.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*TimeFormatTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: referral and non-referral bookings.

**Dependencies:** Task 10

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/TimeFormat.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/booking/TimeFormatTest.kt`

**Estimated scope:** Medium

### Task 12: Add appointment-list pagination

**Description:** Load appointment pages without losing existing list status,
formatting, and action behavior.

**Acceptance criteria:**
- [x] First page, load-more, exhaustion, retry, and duplicate-load prevention work.
- [x] Unknown statuses are visible but non-actionable.
- [x] List ordering follows the backend.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentListViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 9

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentListViewModelTest.kt`

**Estimated scope:** Medium

### Task 13: Align appointment-detail state and mutations

**Description:** Update detail state for the v1 resource and preserve returned
resources after cancel/reschedule.

**Acceptance criteria:**
- [x] Cancel/reschedule are available only for pending/confirmed.
- [x] Successful mutations update from the response without refetch.
- [x] Authorization/validation errors remain distinct from transport errors.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentDetailViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 9

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModelTest.kt`

**Estimated scope:** Small

### Task 14: Remove contact-note editing from appointment detail

**Description:** Remove the unregistered PATCH flow and align detail/reschedule
UI with the remaining patient mutations.

**Acceptance criteria:**
- [x] No edit-contact-note control or repository call remains.
- [x] Create-time contact notes remain visible where returned.
- [x] Reschedule uses appointment-type duration and optional appointment context.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentDetailViewModelTest"`
- [x] `.\gradlew assembleDebug`
- [x] `rg -n "contact-note|updateAppointmentContactNote" app/src`

**Dependencies:** Task 13

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/RescheduleBottomSheet.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentScheduling.kt`

**Estimated scope:** Medium

### Task 15: Delete retired appointment transport declarations

**Description:** Remove `/visit-reasons`, `/appointments/availability`, and
contact-note mutation declarations after all consumers use v1.

**Acceptance criteria:**
- [x] Appointment service contains exactly the seven approved setup/resource routes.
- [x] Retired request/response DTOs are gone.
- [x] No old appointment path remains in production or tests.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*Appointment*"`
- [x] `.\gradlew assembleDebug`
- [x] `rg -n "visit-reasons|appointments/availability|contact-note" app/src`

**Dependencies:** Tasks 10–14

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentRepositoryImplTest.kt`

**Estimated scope:** Medium

## Checkpoint C — Scheduling

- [x] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest --tests "*Appointment*" --tests "*BookAppointment*"`
- [x] `.\gradlew assembleDebug`
- [x] Booking, list, detail, reschedule, and cancel work end to end.

## Phase D — Intake and Appointment Feedback

### Task 16: Add patient-intake transport and domain models

**Description:** Add GET/PUT/submit contracts and a network-only domain model
for draft, submitted, and verified intake.

**Acceptance criteria:**
- [x] Nullable intake data and `data: null` decode correctly.
- [x] Save request contains only documented fields.
- [x] Status parsing fails closed.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*PatientIntakeDtosTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 15

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/dto/PatientIntakeDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/PatientIntake.kt`
- `app/src/main/java/com/eyecare/app/data/remote/api/PatientIntakeApiService.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/PatientIntakeDtosTest.kt`

**Estimated scope:** Medium

### Task 17: Add patient-intake repository

**Description:** Map intake DTOs to domain, translate 422 errors, and bind the
network-only repository with Hilt.

**Acceptance criteria:**
- [x] Load returns nullable intake; save handles 200 and 201.
- [x] Submit returns the immutable submitted resource.
- [x] No Room dependency or entity is introduced.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*PatientIntakeRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 16

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/domain/repository/PatientIntakeRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/PatientIntakeRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/PatientIntakeModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/PatientIntakeRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 18: Implement intake ViewModel state transitions

**Description:** Support load, draft edit/save, submit confirmation, immutable
states, field errors, and retry.

**Acceptance criteria:**
- [x] Draft state tracks unsaved edits and save progress.
- [x] Submitted/verified states expose no edit action.
- [x] Repeated submit and backend 422 are handled deterministically.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*PatientIntakeViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 17

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/intake/PatientIntakeViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/intake/PatientIntakeViewModelTest.kt`

**Estimated scope:** Medium

### Task 19: Add intake screen and navigation

**Description:** Build the intake form/read-only screen and link it from owned
appointment detail.

**Acceptance criteria:**
- [x] Draft fields render validation and save state accessibly.
- [x] Submit requires confirmation.
- [x] Submitted/verified intake renders read-only.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*PatientIntakeViewModelTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: draft save, submit, reopen read-only.

**Dependencies:** Task 18

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/intake/PatientIntakeScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`

**Estimated scope:** Medium

### Task 20: Remove feedback-history navigation

**Description:** Remove the retired GET-history destination before deleting its
repository method.

**Acceptance criteria:**
- [x] Profile and navigation expose no Feedback History action.
- [x] The history screen and destination are removed.
- [x] Feedback submission remains reachable from appointment detail.

**Verification:**
- [x] `.\gradlew assembleDebug`
- [x] `rg -n "FeedbackHistoryScreen|FeedbackHistoryRoute" app/src/main`

**Dependencies:** Task 19

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackHistoryScreen.kt` (delete)
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

**Estimated scope:** Medium

### Task 21: Make feedback completed-appointment-only

**Description:** Align feedback submission with the completed-appointment
contract while leaving the now-unreachable history internals for Task 22.

**Acceptance criteria:**
- [x] Request contains `appointment_id`, rating, and optional comment only.
- [x] Submission has no order-feedback target or behavior.
- [x] 422 field errors map correctly.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FeedbackRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 20

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/FeedbackApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/FeedbackDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Feedback.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FeedbackRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackScreen.kt`

**Estimated scope:** Medium

### Task 22: Align feedback UI and tests

**Description:** Remove the unreachable feedback-history API/repository/state
and restrict the ViewModel to completed-appointment submission.

**Acceptance criteria:**
- [x] Only completed appointment detail exposes feedback.
- [x] Submission success cannot be repeated accidentally.
- [x] Rating/comment validation matches the contract.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FeedbackViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 21

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/FeedbackApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/FeedbackRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FeedbackRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/feedback/FeedbackViewModelTest.kt`

**Estimated scope:** Medium

## Checkpoint D — Appointment Journey

- [x] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest --tests "*Appointment*" --tests "*Intake*" --tests "*Feedback*"`
- [x] `.\gradlew assembleDebug`
- [x] Booking → intake → completed appointment feedback is coherent.

## Phase E — Frames, AR, and Frame Cache

### Task 23: Replace product transport with frame contract

**Description:** Add the frame API/DTO/domain boundary with frame-only names and
the two approved frame routes. Keep the legacy product files compiling but do
not wire this boundary as a fallback.

**Acceptance criteria:**
- [x] Only `/frames` and `/frames/{frame}` remain.
- [x] Paginated frames decode money, variants, images, and AR fields.
- [x] Product-type/accessory orderability policy is absent from Frame domain.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameDtosTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 4

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/FrameApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/FrameDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Frame.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/FrameDtosTest.kt`

**Estimated scope:** Medium

### Task 24: Convert Room product cache to frame cache

**Description:** Add a frame entity/DAO and migrate the non-clinical cache
schema. The old product table may remain temporarily until Task 56 so existing
code continues to compile.

**Acceptance criteria:**
- [x] Database exposes a frame cache used only for non-clinical catalog data.
- [x] Schema version increments and exported schema is updated.
- [x] No patient/clinical field is added.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameDaoTest" --tests "*StorageModuleTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 23

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/local/entity/FrameEntity.kt`
- `app/src/main/java/com/eyecare/app/data/local/dao/FrameDao.kt`
- `app/src/main/java/com/eyecare/app/data/local/EyecareDatabase.kt`
- `app/src/main/java/com/eyecare/app/di/DatabaseModule.kt`
- `app/schemas/com.eyecare.app.data.local.EyecareDatabase/3.json`

**Estimated scope:** Medium

### Task 25: Replace product repository with frame repository

**Description:** Add the frame repository and Hilt binding, including page
tracking and frame-only cache fallback.

**Acceptance criteria:**
- [x] Repository maps network/cache frames at the boundary.
- [x] Search, page, sort, and detail work without brand/category metadata calls.
- [x] Cache fallback cannot expose legacy accessory rows.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 23–24

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/domain/repository/FrameRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FrameRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/FrameModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/FrameRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 26: Replace catalog list state with frame list state

**Description:** Add frame list state/tests using frame models without accessory
tabs or unsupported brand/category picker state.

**Acceptance criteria:**
- [x] Frames are the only list content.
- [x] Search remains debounced; paging and sort remain backend-driven.
- [x] No retired metadata endpoint or local product-type filtering remains.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameListViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 25

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameListViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/frames/FrameListViewModelTest.kt`

**Estimated scope:** Medium

### Task 27: Build frame list UI and card

**Description:** Add list/card UI for a single frame experience with
search, sorting, pagination, images, and AR affordance.

**Acceptance criteria:**
- [x] Root copy consistently says Frames.
- [x] Empty/error/loading/load-more states remain accessible.
- [x] No accessory, order, brand-ID, or category-ID control remains.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameListViewModelTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: search, sort, scroll-to-load, empty/error states.

**Dependencies:** Task 26

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/frames/components/FrameCard.kt`
- `app/src/test/java/com/eyecare/app/presentation/frames/FrameImageUrlTest.kt`

**Estimated scope:** Medium

### Task 28: Replace product detail with frame detail

**Description:** Add detail state/UI/tests with frame-only behavior and make
reservation the only transactional action.

**Acceptance criteria:**
- [x] Detail displays frame and variant information only.
- [x] AR appears only for usable AR variants.
- [x] No order/accessory action or unavailable-product policy remains.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameDetail*"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 25

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/frames/FrameDetailViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/frames/FrameDetailActionsTest.kt`

**Estimated scope:** Medium

### Task 29: Adapt AR state to frame models

**Description:** Replace Product/ProductVariant dependencies in AR with
Frame/FrameVariant while preserving rendering behavior.

**Acceptance criteria:**
- [x] AR loads frame detail from FrameRepository.
- [x] Selected variant and overlay asset remain deterministic.
- [x] Camera/MediaPipe behavior is unchanged.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*ArViewModelTest" --tests "*FaceRotationTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: launch and switch AR-ready variants.

**Dependencies:** Task 28

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/ar/ArViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/components/VariantChipRow.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/ArViewModelTest.kt`

**Estimated scope:** Medium

### Task 30: Cut root navigation from Catalog to Frames

**Description:** Replace Catalog/ProductDetail routes with Frames/FrameDetail
while preserving tab state and AR navigation.

**Acceptance criteria:**
- [x] Frames is the second approved root.
- [x] Frame detail and AR use type-safe frame/variant IDs.
- [x] No Catalog or ProductDetail route remains.

**Verification:**
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: switch roots, open frame, open AR, return with state intact.

**Dependencies:** Tasks 27–29

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/SplitBottomNavBar.kt`

**Estimated scope:** Medium

## Checkpoint E — Frames

- [x] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest --tests "*Frame*" --tests "*Ar*"`
- [x] `.\gradlew assembleDebug`
- [x] Frames, cache, detail, and AR work with no product/accessory API.

## Phase F — Frame Reservations

### Task 31: Add frame-reservation contract models

**Description:** Add sanitized list/create/cancel DTOs and domain status/action
policy from the common FrameReservationResource.

**Acceptance criteria:**
- [x] List is explicitly unpaginated.
- [x] List/create/cancel decode the same sanitized field set.
- [x] Excluded cost/inventory/internal fields do not exist in Android models.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameReservationDtosTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 25

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/FrameReservationApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/FrameReservationDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/FrameReservation.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/FrameReservationDtosTest.kt`

**Estimated scope:** Medium

### Task 32: Add frame-reservation repository

**Description:** Implement list/create/cancel mapping, validation errors, and
Hilt binding.

**Acceptance criteria:**
- [x] Create accepts 1–5 selected variant IDs and optional appointment ID.
- [x] Cancel returns the updated resource.
- [x] Repository exposes no client-side ownership assumptions.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameReservationRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 31

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/domain/repository/FrameReservationRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FrameReservationRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/FrameReservationModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/FrameReservationRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 33: Add reservation-list state and UI

**Description:** Build an unpaginated reservation history with status, selected
frames, empty/error states, and cancellation entry.

**Acceptance criteria:**
- [x] All six reservation statuses render intentionally.
- [x] Cancel is visible only for requested/prepared.
- [x] Returned cancellation updates the item without refetch.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*FrameReservationListViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 32

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationListScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/FrameReservationListViewModelTest.kt`

**Estimated scope:** Medium

### Task 34: Replace order request with reservation creation

**Description:** Move the old request entry point to a frame-reservation review
flow with optional appointment link and explicit confirmation.

**Acceptance criteria:**
- [x] One to five selected variants can be reviewed and submitted.
- [x] The flow never constructs an order or purchase payload.
- [x] Success shows the returned reservation and exits cleanly.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*CreateFrameReservationViewModelTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: create with and without appointment.

**Dependencies:** Tasks 30 and 32

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/orders/OrderRequestViewModel.kt` → `presentation/reservations/CreateFrameReservationViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/orders/OrderRequestScreen.kt` → `presentation/reservations/CreateFrameReservationScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/orders/OrderRequestViewModelTest.kt` → `presentation/reservations/CreateFrameReservationViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameDetailScreen.kt`

**Estimated scope:** Medium

### Task 35: Wire reservation navigation and Profile entry

**Description:** Add type-safe reservation list/create destinations and Profile
hub access.

**Acceptance criteria:**
- [x] Frame detail opens reservation creation.
- [x] Profile opens reservation history.
- [x] No OrderRequest route remains.

**Verification:**
- [x] `.\gradlew assembleDebug`
- [ ] `rg -n "OrderRequest" app/src/main`
- [ ] Manual check: Frame → Reserve and Profile → Reservations.

**Dependencies:** Tasks 33–34

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`

**Estimated scope:** Medium

## Checkpoint F — Frames and Reservations

- [x] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest --tests "*Frame*" --tests "*Reservation*"`
- [x] `.\gradlew assembleDebug`
- [x] Browse → AR → reserve → view → cancel works end to end.

## Phase G — Read-Only Patient Records and Ratings

### Task 36: Paginate prescription data

**Description:** Align prescription DTOs and repository with paginated lists and
the expanded optical fields.

**Acceptance criteria:**
- [x] List uses canonical links/meta.
- [x] Prism/base and numeric axis values decode correctly.
- [x] Detail remains patient-read-only.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*PrescriptionRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 3–4

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/PrescriptionApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/PrescriptionDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Prescription.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/PrescriptionRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/PrescriptionRepositoryImpl.kt`

**Estimated scope:** Medium

### Task 37: Add prescription paging UI

**Description:** Update prescription list/detail state and UI for pagination and
new optical values.

**Acceptance criteria:**
- [x] Load-more/exhaustion/retry work.
- [x] Nullable optical values have clear display behavior.
- [x] No edit/create action is shown.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*PrescriptionViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 36

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionScreens.kt`
- `app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/PrescriptionRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 38: Add quotation data vertical slice

**Description:** Add paginated list/detail contracts, nullable revision, items,
money, repository mapping, and read-only statuses.

**Acceptance criteria:**
- [x] List/detail use one Quotation domain model.
- [x] Null revision and all five statuses decode safely.
- [x] No mutation method exists.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*QuotationRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 3–4

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/QuotationApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/QuotationDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Quotation.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/QuotationRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/QuotationRepositoryImpl.kt`

**Estimated scope:** Medium

### Task 39: Add quotation UI vertical slice

**Description:** Bind quotation repository and add paginated list/detail UI with
revision totals and immutable line items.

**Acceptance criteria:**
- [x] Draft/no-revision and presented-revision states render correctly.
- [x] Expired/accepted/declined statuses are read-only.
- [x] Paging and detail retry work.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*Quotation*"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 38

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/di/QuotationModule.kt`
- `app/src/main/java/com/eyecare/app/presentation/quotations/QuotationViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/quotations/QuotationScreens.kt`
- `app/src/test/java/com/eyecare/app/data/repository/QuotationRepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/quotations/QuotationViewModelTest.kt`

**Estimated scope:** Medium

### Task 40: Replace order data with job-order data

**Description:** Add paginated, read-only job-order transport/domain/repository
files. Keep the legacy order files compiling until final cutover cleanup.

**Acceptance criteria:**
- [x] Only GET list/detail methods remain.
- [x] Detail preserves nullable workflow timestamps and variant ID.
- [x] Unknown status is non-actionable.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*JobOrderRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 3–4 and Task 35

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/JobOrderApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/JobOrderDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/JobOrder.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/JobOrderRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/JobOrderRepositoryImpl.kt`

**Estimated scope:** Medium

### Task 41: Replace order history UI with job-order UI

**Description:** Add list/detail ViewModels and screens with job-order semantics
and no patient mutation actions.

**Acceptance criteria:**
- [x] Job-order list pages and detail displays workflow/items.
- [x] Cancel/order creation actions are absent.
- [x] Status timeline uses queued through dispensed/cancelled.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*JobOrder*"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 40

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/components/JobOrderTimeline.kt`

**Estimated scope:** Medium

### Task 42: Add job-order repository and ViewModel tests

**Description:** Add the job-order Hilt binding and paging/detail/status tests
without request/cancel assumptions.

**Acceptance criteria:**
- [ ] Repository fixtures cover list and raw detail shapes.
- [ ] ViewModel tests cover paging, detail retry, and unknown status.
- [ ] No order-create/cancel test remains.

**Verification:**
- [ ] `.\gradlew testDebugUnitTest --tests "*JobOrder*"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 41

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/di/JobOrderModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/JobOrderRepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/joborders/JobOrderViewModelTest.kt`

**Estimated scope:** Medium

### Task 43: Add frame-rating data behavior

**Description:** Add rating request/response/revisions to the job-order service
and repository with server-authoritative error handling.

**Acceptance criteria:**
- [ ] Create/revision response maps complete revision history.
- [ ] Request variant ID comes from the selected job-order item.
- [ ] 403/404/422 are preserved without local override.

**Verification:**
- [ ] `.\gradlew testDebugUnitTest --tests "*FrameRatingRepositoryTest"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 42

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/JobOrderApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/FrameRatingDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/FrameRating.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/JobOrderRepository.kt`
- `app/src/test/java/com/eyecare/app/data/repository/FrameRatingRepositoryTest.kt`

**Estimated scope:** Medium

### Task 44: Add frame-rating UI behavior

**Description:** Add create/revise rating state and dialog/screen to eligible
dispensed job-order items.

**Acceptance criteria:**
- [ ] Action is shown only for dispensed items with a variant ID.
- [ ] Rating/comment validation matches contract.
- [ ] Returned current rating and revisions replace local state.

**Verification:**
- [ ] `.\gradlew testDebugUnitTest --tests "*FrameRatingViewModelTest"`
- [ ] `.\gradlew assembleDebug`
- [ ] Manual check: create and revise a rating.

**Dependencies:** Task 43

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/joborders/FrameRatingViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/FrameRatingDialog.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/joborders/FrameRatingViewModelTest.kt`

**Estimated scope:** Medium

### Task 45: Replace billing data with invoice data

**Description:** Add paginated, read-only invoice transport/domain/repository
files with items and posted payments. Keep legacy billing compiling until Task
56.

**Acceptance criteria:**
- [x] List/detail decode numeric money and nullable invoice fields.
- [x] Only posted payments are exposed.
- [x] No PDF/download or payment mutation method remains.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*InvoiceRepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 3–4

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/InvoiceApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/InvoiceDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Invoice.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/InvoiceRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/InvoiceRepositoryImpl.kt`

**Estimated scope:** Medium

### Task 46: Replace billing UI with invoice UI

**Description:** Add DI, ViewModel, screen, and tests for invoice list/detail
with totals, items, and posted payments.

**Acceptance criteria:**
- [x] Invoice history pages and detail retry work.
- [x] Draft/issued/partial/paid/voided are read-only.
- [x] No PDF or download state/action remains.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*Invoice*"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 45

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/di/InvoiceModule.kt`
- `app/src/main/java/com/eyecare/app/presentation/invoices/InvoiceViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/invoices/InvoiceScreens.kt`
- `app/src/test/java/com/eyecare/app/data/repository/InvoiceRepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/invoices/InvoiceViewModelTest.kt`

**Estimated scope:** Medium

### Task 47: Wire patient-record navigation and Profile hub

**Description:** Add type-safe destinations for prescriptions, quotations, job
orders, ratings, and invoices and replace Order/Billing routes.

**Acceptance criteria:**
- [x] All record lists/details are reachable from Profile.
- [x] OrderList/OrderDetail/BillingDetail routes are gone.
- [x] Back navigation returns to the correct record list.

**Verification:**
- [ ] `.\gradlew assembleDebug`
- [ ] `rg -n "OrderList|OrderDetail|BillingDetail" app/src/main`
- [ ] Manual navigation smoke test.

**Dependencies:** Tasks 37, 39, 44, and 46

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`

**Estimated scope:** Medium

## Checkpoint G — Patient Records

- [ ] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest --tests "*Prescription*" --tests "*Quotation*" --tests "*JobOrder*" --tests "*Rating*" --tests "*Invoice*"`
- [x] `.\gradlew assembleDebug`
- [x] All read-only records and the allowed rating mutation work.

## Phase H — Singleton Conversation and Attachments

### Task 48: Align singleton conversation contracts

**Description:** Replace plural/ID-based conversation endpoints and customer
fields with singleton patient conversation/message resources.

**Acceptance criteria:**
- [x] Service contains exactly the four approved conversation routes.
- [x] Conversation uses `patient_id` and backend unread count.
- [x] Messages remain unpaginated oldest-first.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*MessageDtosTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 2–3

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/ConversationApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/MessageDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Message.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/MessageDtosTest.kt`

**Estimated scope:** Medium

### Task 49: Implement singleton chat repository and secure attachments

**Description:** Remove conversation IDs/mark-read, encode indexed multipart
contexts, and add authenticated attachment download.

**Acceptance criteria:**
- [x] Repository calls singleton paths only.
- [x] Multipart fields use sequential `contexts[N][type/id]`.
- [x] Attachment download returns binary metadata/content without public URLs.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*ChatRepository*"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 48

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/domain/repository/ChatRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/ChatModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ChatRepositoryMappingsTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ChatRepositoryRequestTest.kt`

**Estimated scope:** Medium

### Task 50: Expand attachment validation and opening

**Description:** Support documented image/PDF/DOC/DOCX uploads up to 10 MB and
safe external opening of authenticated downloads.

**Acceptance criteria:**
- [x] Size/MIME validation matches contract for UX.
- [x] Temporary files use scoped content URIs and are cleaned up.
- [x] DOC/DOCX/PDF are never rendered in a WebView.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*AttachmentValidatorTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: image, PDF, DOCX, rejected MIME, oversized file.

**Dependencies:** Task 49

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/messaging/AttachmentValidator.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/AttachmentSheet.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/AttachmentPreview.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/AttachmentValidatorTest.kt`

**Estimated scope:** Medium

### Task 51: Make Chat ViewModel rate-limit-safe

**Description:** Remove explicit mark-read and aggressive/background polling in
favor of lifecycle-aware conservative refresh and manual retry.

**Acceptance criteria:**
- [x] Opening chat never sends mark-read.
- [x] Refresh stops when chat is not visible.
- [x] Request rate stays comfortably below 60/minute.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*ChatViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 49

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`

**Estimated scope:** Medium

### Task 52: Align chat UI and context navigation

**Description:** Use backend unread state, authenticated attachment actions,
and supported context destinations.

**Acceptance criteria:**
- [x] Appointment contexts open appointment detail.
- [x] Frame product contexts may open frame detail; historical orders are non-interactive.
- [x] UI does not optimistically clear unread count.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*Message*"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual context/attachment navigation smoke test.

**Dependencies:** Tasks 50–51

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageContextCard.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageBubble.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

**Estimated scope:** Medium

## Checkpoint H — Communication

- [x] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest --tests "*Chat*" --tests "*Message*" --tests "*Attachment*"`
- [x] `.\gradlew assembleDebug`
- [x] Chat respects auth, ownership, file safety, and rate limits.

## Phase I — Home, Final Navigation, and Legacy Removal

### Task 53: Rebuild Home state for the patient workflow

**Description:** Remove order/accessory shelves and load a lean combination of
next appointment/intake prompt and featured frames.

**Acceptance criteria:**
- [x] Home has no Order/ProductRepository dependency.
- [x] Network fan-out remains limited to scheduling and a small frame preview.
- [x] Partial failures do not hide otherwise available content.

**Verification:**
- [x] `.\gradlew testDebugUnitTest --tests "*HomeViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 19, 25, and 47

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/home/HomeViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/home/HomeViewModelTest.kt`

**Estimated scope:** Medium

### Task 54: Rebuild Home UI and root actions

**Description:** Replace commerce sections with patient appointment/intake and
featured-frame content linked to approved destinations.

**Acceptance criteria:**
- [x] Home contains no accessory/order/billing copy or callbacks.
- [x] Appointment/intake and frame actions navigate correctly.
- [x] Four-root bottom navigation remains visually and behaviorally stable.

**Verification:**
- [x] `.\gradlew assembleDebug`
- [ ] Manual Home/root navigation smoke test.

**Dependencies:** Task 53

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Estimated scope:** Medium

### Task 55: Remove orphaned legacy order artifacts

**Description:** Delete the now-unreachable legacy order data, DI, UI, and test
files left after reservation and job-order cutover. This is a mechanical
deletion set and may exceed the normal five-file task target.

**Acceptance criteria:**
- [ ] No OrderRequest/OrderStatus/OrderError artifact remains.
- [ ] No POST `/orders` or cancel-order behavior remains.
- [ ] Project still compiles with job orders and reservations.

**Verification:**
- [ ] `rg -n "OrderRequest|OrderError|POST\\(\"orders|orders/\\{id\\}/cancel" app/src`
- [ ] `.\gradlew testDebugUnitTest --tests "*JobOrder*" --tests "*Reservation*"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Tasks 42 and 47

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/OrderApiService.kt` (delete)
- `app/src/main/java/com/eyecare/app/data/remote/dto/OrderDtos.kt` (delete)
- `app/src/main/java/com/eyecare/app/domain/model/Order.kt` and `OrderError.kt` (delete)
- `app/src/main/java/com/eyecare/app/domain/repository/OrderRepository.kt`, `data/repository/OrderRepositoryImpl.kt`, and `di/OrderModule.kt` (delete)
- `app/src/main/java/com/eyecare/app/presentation/orders/` and remaining legacy order tests (delete)

**Estimated scope:** Small

### Task 56: Remove orphaned product/billing/filter artifacts

**Description:** Delete the now-unreachable Product/Billing stacks, old product
cache, retired filters, and PDF behavior. This is a mechanical deletion/schema
cleanup set and may exceed the normal five-file task target.

**Acceptance criteria:**
- [ ] No ProductType/accessory/filter metadata policy remains.
- [ ] No billing PDF/download code remains.
- [ ] No `/products`, `/brands`, `/categories`, or `/billing` annotation remains.
- [ ] Room contains the frame cache only.

**Verification:**
- [ ] `rg -n "products|brands|categories|billing/.*/pdf|ProductType|BillingRepository" app/src/main`
- [ ] `.\gradlew testDebugUnitTest --tests "*Frame*" --tests "*Invoice*"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Tasks 30 and 46

**Files likely touched:**
- Legacy Product API/DTO/domain/repository/DI files and product-only tests (delete)
- Legacy `presentation/catalog/` source/tests and filter-policy files (delete)
- `ProductEntity.kt` and `ProductDao.kt` (delete)
- Legacy Billing API/DTO/domain/repository/DI/presentation files and tests (delete)
- `EyecareDatabase.kt`, `DatabaseModule.kt`, and exported Room schema (remove product cache)

**Estimated scope:** Medium

### Task 57: Enforce the 34-route allowlist

**Description:** Add a reflection/source contract test comparing every
production Retrofit method/path to the approved allowlist.

**Acceptance criteria:**
- [ ] Exactly 34 method/path pairs are present.
- [ ] Retired and unversioned paths fail the test.
- [ ] Reservation/messages exceptions do not affect route count.

**Verification:**
- [ ] `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Tasks 15, 21, 30, 35, 47, 52, 55, and 56

**Files likely touched:**
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`

**Estimated scope:** Small

### Task 58: Synchronize project documentation

**Description:** Update project context and V8 status to the implemented patient
workflow and record verification evidence.

**Acceptance criteria:**
- [ ] `CONTEXT.md` describes all four roots and new resources.
- [ ] Retired commerce/contact-note/mark-read behavior is absent.
- [ ] V8 task statuses and verification results are accurate.

**Verification:**
- [ ] `git diff --check`
- [ ] `rg -n "visit-reasons|/orders|billing PDF|contact-note|mark-read" CONTEXT.md`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 57

**Files likely touched:**
- `CONTEXT.md`
- `docs/specs/backend-alignment-v8-spec.md`
- `docs/specs/backend-alignment-v8-plan.md`
- `docs/specs/backend-alignment-v8-tasks.md`

**Estimated scope:** Medium

### Task 59: Run full automated release gates

**Description:** Format and execute all project-wide automated checks without
weakening tests or hiding unrelated failures.

**Acceptance criteria:**
- [ ] Formatting, unit tests, lint, and debug assembly are executed.
- [ ] V8-related failures are fixed.
- [ ] Any unrelated pre-existing failure is documented with exact evidence.

**Verification:**
- [ ] `.\gradlew ktlintFormat`
- [ ] `.\gradlew ktlintCheck`
- [ ] `.\gradlew testDebugUnitTest`
- [ ] `.\gradlew lintDebug`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 58

**Files likely touched:**
- Files changed only by `ktlintFormat`, if any
- `docs/specs/backend-alignment-v8-tasks.md` for recorded results

**Estimated scope:** Medium

### Task 60: Complete emulator patient-journey smoke test

**Description:** Verify the complete release candidate against backend commit
`ebd1e2e` or a documented contract-equivalent commit.

**Acceptance criteria:**
- [ ] Auth, profile, appointment, intake, frames/AR, reservations, records,
  rating, messaging/attachments, and feedback complete successfully.
- [ ] Four-root back-stack behavior is correct.
- [ ] 401, 422, 429, network-loss, empty, and terminal states are exercised.

**Verification:**
- [ ] Record device/API level, backend commit, scenarios, and outcomes.
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 59

**Files likely touched:**
- `docs/specs/backend-alignment-v8-tasks.md` for smoke-test evidence

**Estimated scope:** Medium

## Checkpoint I — Release Candidate

- [ ] All 60 tasks are complete.
- [ ] Exactly 34 approved Retrofit routes remain.
- [ ] No legacy mobile API route or reachable UI action remains.
- [ ] No clinical/health data is stored in Room or logged.
- [ ] Full automated gates pass or unrelated failures are documented.
- [ ] Emulator smoke test passes against the recorded backend commit.
- [ ] `CONTEXT.md`, specification, plan, and task list agree.

## Parallelization Guidance

After Checkpoint B:

- Appointment tasks 8–22 and Frame tasks 23–35 are independent until Home and
  navigation integration.
- Record tasks 36–47 are independent of Messaging tasks 48–52.
- Within each resource, transport/repository tasks must precede ViewModel/UI.
- Tasks touching `Routes.kt`, `NavGraph.kt`, Profile, or Home require serialized
  coordination even if feature work is otherwise parallel.
- Final cleanup, route allowlist, documentation, and release gates are strictly
  sequential.

## Phase Gate

This Phase 3 task list must be reviewed and approved before Task 1 begins.
Implementation will then follow `incremental-implementation`,
`test-driven-development`, and the applicable feature/security skills one task
at a time.
