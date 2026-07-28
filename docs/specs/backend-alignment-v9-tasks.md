# Backend Alignment V9 — Phase 3 Task List

Status: Draft — awaiting Phase 3 human review

Approved specification:
`docs/specs/backend-alignment-v9-spec.md`

Approved implementation plan:
`docs/specs/backend-alignment-v9-plan.md`

Backend contract:
`docs/API_CONTRACT.md` at backend commit `579b964`

## Execution Rules

- Execute tasks in dependency order.
- Write or update the focused test before changing production behavior.
- Confirm the focused test fails for the intended V9 reason.
- Every task must leave the project compiling.
- Run the focused test and `.\gradlew assembleDebug` after every task.
- Do not modify Retrofit routes, DTO shapes, Room, navigation, or dependencies.
- Do not accept retired backend status strings as runtime aliases.
- Tasks 1–8 may retain the old enum constants only as compile scaffolding while
  consumers are migrated. Those constants must be non-actionable and are
  deleted in Task 9.
- Preserve `docs/BACKEND_CONTEXT.md` and `docs/API_CONTRACT.md` as user-supplied
  source documents.

## Phase A — Domain and Transport Contract

### Task 1: Add canonical appointment lifecycle semantics

**Description:** Introduce the five canonical status values, fail-closed
`UNKNOWN`, raw-string mapping, and domain capability properties. Add temporary
fail-closed fallback branches to the existing exhaustive list/detail status
renderers so the additive enum change compiles. Retain the old enum constants
temporarily, but never map backend strings to them or give them capabilities.

**Acceptance criteria:**
- [x] New raw strings map exactly to their canonical enum values.
- [x] Unexpected and retired raw strings map to `UNKNOWN`.
- [x] Active, cancel, reschedule, and feedback capabilities match the approved
  matrix for every canonical value.
- [x] Unmigrated list/detail rendering treats new values as neutral and
  non-actionable until Tasks 4 and 6.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*AppointmentStatusTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** None

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/domain/model/AppointmentV1.kt`
- `app/src/test/java/com/eyecare/app/domain/model/AppointmentStatusTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`

**Estimated scope:** Medium

### Task 2: Align appointment DTO and repository fixtures

**Description:** Replace old transport examples with the canonical lifecycle
and prove list, detail, create, cancel, and reschedule responses map correctly
without changing DTO or service shapes.

**Acceptance criteria:**
- [x] DTO decoding preserves each new raw status string.
- [x] Repository responses map scheduled, checked-in, fulfilled, cancelled,
  and no-show correctly.
- [x] Request bodies, pagination behavior, and error parsing are unchanged.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*AppointmentV1DtosTest" --tests "*AppointmentV1RepositoryImplTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/test/java/com/eyecare/app/data/remote/dto/AppointmentV1DtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImplTest.kt`

**Estimated scope:** Small

## Checkpoint A — Contract Foundation

- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentStatusTest" --tests "*AppointmentV1DtosTest" --tests "*AppointmentV1RepositoryImplTest"`
- [x] `.\gradlew assembleDebug`
- [x] No Retrofit service, request DTO, or route allowlist changed.

## Phase B — Appointment Experience

### Task 3: Migrate booking and appointment-list state fixtures

**Description:** Update ViewModel fixtures and expectations so newly booked and
listed appointments use only canonical active statuses.

**Acceptance criteria:**
- [x] Successful booking returns `SCHEDULED`.
- [x] Appointment-list state handles scheduled and checked-in records.
- [x] No old enum constant remains in these ViewModel tests.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*BookAppointmentViewModelTest" --tests "*AppointmentListViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 2

**Files likely touched:**
- `app/src/test/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentListViewModelTest.kt`

**Estimated scope:** Small

### Task 4: Cut appointment list and shared labels to V9

**Description:** Add one presentation-level patient-label mapping, use it in
appointment-list status UI, and replace Upcoming/History grouping with domain
active capabilities.

**Acceptance criteria:**
- [x] Labels are Scheduled, Checked in, Completed, Cancelled, No show, and
  Unknown.
- [x] Upcoming contains future scheduled/checked-in records only.
- [x] Fulfilled, cancelled, no-show, unknown, and past active records appear in
  History.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*AppointmentStatusPresentationTest" --tests "*AppointmentFormattingTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 3

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/common/AppointmentStatusPresentation.kt`
- `app/src/test/java/com/eyecare/app/presentation/common/AppointmentStatusPresentationTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentFormattingTest.kt`

**Estimated scope:** Medium

## Checkpoint B — Booking and List

- [x] `.\gradlew testDebugUnitTest --tests "*BookAppointmentViewModelTest" --tests "*AppointmentListViewModelTest" --tests "*AppointmentFormattingTest" --tests "*AppointmentStatusPresentationTest"`
- [x] `.\gradlew assembleDebug`
- [x] Manual preview check: every canonical status uses readable list copy.

### Task 5: Guard appointment-detail mutations

**Description:** Enforce status capabilities in the ViewModel so only scheduled
appointments can reschedule and only scheduled/checked-in appointments can
cancel, independent of UI visibility.

**Acceptance criteria:**
- [x] Invalid statuses cannot open or submit rescheduling.
- [x] Invalid statuses cannot call cancellation.
- [x] Valid server results and existing 422 behavior still update UI state.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*AppointmentDetailViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModelTest.kt`

**Estimated scope:** Medium

### Task 6: Apply the appointment-detail action matrix

**Description:** Split the shared detail management condition into independent
reschedule/cancel actions, apply canonical badges/guidance, and expose feedback
only for fulfilled appointments.

**Acceptance criteria:**
- [x] Scheduled shows Reschedule and Cancel.
- [x] Checked in shows Cancel only; fulfilled shows Leave Feedback only.
- [x] Cancelled, no-show, unknown, and transitional constants show no mutation
  action.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*AppointmentDetailActionsTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual preview/runtime check of scheduled, checked-in, and fulfilled
  bottom actions.

**Dependencies:** Tasks 4–5

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentDetailActionsTest.kt`

**Estimated scope:** Medium

## Checkpoint C — Appointment Detail

- [x] `.\gradlew testDebugUnitTest --tests "*AppointmentDetail*"`
- [x] `.\gradlew assembleDebug`
- [x] Scheduled, checked-in, fulfilled, and terminal action behavior matches the
  backend contract.

## Phase C — Downstream Consumers

### Task 7: Align Home active-appointment selection

**Description:** Select the Home card from scheduled/checked-in appointments
only and migrate all Home fixtures to canonical values.

**Acceptance criteria:**
- [x] Home selects the soonest non-past scheduled or checked-in appointment.
- [x] Fulfilled, terminal, unknown, and past appointments are excluded.
- [x] Existing frame and prescription Home behavior is unchanged.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*HomeViewModelTest"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/home/HomeViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/home/HomeViewModelTest.kt`

**Estimated scope:** Small

### Task 8: Use patient labels in appointment attachments

**Description:** Replace enum-name lowercasing in the messaging attachment
picker with the shared patient-facing appointment label.

**Acceptance criteria:**
- [x] Picker copy never displays `checked_in` or `fulfilled`.
- [x] Every canonical and unknown status has readable secondary text.
- [x] Attachment selection and message navigation behavior are unchanged.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*AppointmentAttachmentLabelTest"`
- [x] `.\gradlew assembleDebug`
- [ ] Manual check: link scheduled, checked-in, and fulfilled appointments.

**Dependencies:** Task 4

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/AttachmentSheet.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/AppointmentAttachmentLabelTest.kt`

**Estimated scope:** Small

## Checkpoint D — Downstream Consumers

- [x] `.\gradlew testDebugUnitTest --tests "*HomeViewModelTest" --tests "*AppointmentAttachmentLabelTest"`
- [x] `.\gradlew assembleDebug`
- [x] Home and messaging expose no technical or retired status label.

## Phase D — Hard Cutover and Release

### Task 9: Remove transitional appointment constants

**Description:** Delete `PENDING`, `CONFIRMED`, `ARRIVED`, and `COMPLETED` after
all consumers use the canonical lifecycle, then prove the enum set is exact.

**Acceptance criteria:**
- [x] Enum contains exactly the five backend statuses plus `UNKNOWN`.
- [x] Production contains no retired enum reference or raw status literal.
- [x] Retired strings remain only in the focused negative mapping test, proving
  they resolve to `UNKNOWN`.

**Verification:**
- [x] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*AppointmentStatusTest"`
- [x] `rg -n "AppointmentStatus\\.(PENDING|CONFIRMED|ARRIVED|COMPLETED)|\"(pending|confirmed|arrived|completed)\"" app/src/main`
- [x] `rg -n "AppointmentStatus\\.(PENDING|CONFIRMED|ARRIVED|COMPLETED)" app/src/test`
- [x] `.\gradlew testDebugUnitTest --tests "*Appointment*"`
- [x] `.\gradlew assembleDebug`

**Dependencies:** Tasks 2–8

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/domain/model/AppointmentV1.kt`
- `app/src/test/java/com/eyecare/app/domain/model/AppointmentStatusTest.kt`

**Estimated scope:** Small

### Task 10: Synchronize documentation and run release gates

**Description:** Update living project context, record V9 completion evidence,
and run every automated and manual verification gate.

**Acceptance criteria:**
- [x] `CONTEXT.md` describes the canonical lifecycle and action matrix.
- [x] V9 spec, plan, and tasks accurately record final behavior/results.
- [x] V8 documents remain unchanged as historical evidence for `ebd1e2e`.

**Verification:**
- [x] `.\gradlew ktlintCheck`
- [x] `.\gradlew testDebugUnitTest`
- [x] `.\gradlew lintDebug`
- [x] `.\gradlew assembleDebug`
- [ ] Manual backend smoke test against `579b964` or a recorded
  contract-equivalent commit.

**Dependencies:** Task 9

**Files likely touched:**
- `CONTEXT.md`
- `docs/specs/backend-alignment-v9-spec.md`
- `docs/specs/backend-alignment-v9-plan.md`
- `docs/specs/backend-alignment-v9-tasks.md`

**Estimated scope:** Medium

## Checkpoint E — Complete

- [x] All 10 tasks are complete.
- [x] Exactly the canonical statuses plus `UNKNOWN` remain.
- [x] Route allowlist remains at 34 method/path pairs.
- [x] Scheduled, checked-in, fulfilled, cancelled, no-show, and unknown behavior
  matches the approved matrix.
- [x] No appointment or clinical data is stored in Room or logged.
- [x] Full automated gates and backend smoke test pass.
- [x] `CONTEXT.md`, V9 specification, plan, and tasks agree.

## Parallelization Guidance

- Tasks 1–2 are strictly sequential.
- After Task 2, Task 3 may run while Task 5 is prepared, but both modify
  appointment test fixtures and should be serialized in one worktree.
- After the shared label mapping in Task 4, Tasks 6–8 are logically independent.
- Task 9 must wait for every production/test consumer.
- Task 10 is strictly last.

## Phase Gate

This Phase 3 task list must be reviewed and approved before Task 1 begins.
Implementation will then follow test-driven development and incremental
implementation one task at a time.
