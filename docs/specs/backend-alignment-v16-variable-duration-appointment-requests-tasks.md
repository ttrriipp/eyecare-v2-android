# Implementation Plan: Backend Alignment v16 — Variable-Duration Appointment Requests

Status: Complete — 2026-08-10
Date: 2026-08-10
Spec: `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-spec.md`
Technical plan: `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-plan.md`

## Overview

This task breakdown delivers the approved variable-duration request contract in
12 dependency-ordered tasks. Each task is sized to five or fewer likely files,
has explicit acceptance criteria, and leaves the project buildable. Production
behavior changes follow test-driven development during Phase 4.

## Architecture Decisions

- Request DTO/domain ownership stays in the appointment-request feature.
- New response fields are nullable/defaulted for legacy records.
- New outbound requests strictly require a type and primary preference.
- Wizard state uses explicit primary and ordered alternative selections.
- Availability is keyed by appointment type plus date and never fabricated.
- Backend governance tracks 55 callable registered routes, while Android uses
  only 54 canonical routes.
- Provider selection, local persistence, backend work, and confirmed appointment
  redesign are out of scope.

## Task List

### Phase 1: Contract and governance foundation

## Task 1: Reconcile the authoritative appointment contract

**Description:** Update the two user-owned backend documents using the six
resolved decisions so implementation fixtures and source scans have one
unambiguous contract.

**Acceptance criteria:**

- [x] All appointment-request examples use the shared expanded resource and
      document nullable legacy fields.
- [x] Stale patient-type, pending-hold, `expires_at`, referral-limit, and route
      count statements are corrected consistently.
- [x] The route documentation reports 55 callable registered routes: 54
      canonical and one legacy alias.

**Verification:**

- [x] `rg -n "never select|pending request hold|24 account-only|53 routes|53 total" docs/API_CONTRACT.md docs/BACKEND_CONTEXT.md` returns no stale authoritative claim.
- [x] `git diff --check -- docs/API_CONTRACT.md docs/BACKEND_CONTEXT.md`

**Dependencies:** None

**Files likely touched:**

- `docs/API_CONTRACT.md`
- `docs/BACKEND_CONTEXT.md`

**Estimated scope:** Small (2 files)

---

## Task 2: Govern 55 registered routes and 54 canonical routes

**Description:** Restore appointment types to the account-only route set, add
the optometrist catalog, and separate registered-callable counts from routes
that production Android services may consume.

**Acceptance criteria:**

- [x] Governance contains 8 public, 26 account-only, 20 canonical active-link,
      and one callable legacy-alias route.
- [x] `GET /appointment-types` and `GET /appointment-optometrists` are approved
      account-only routes; appointment types are no longer rejected.
- [x] Retrofit discovery accepts only canonical routes and still fails if the
      legacy alias appears in production code.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [x] `./gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** Small (2 files)

### Checkpoint: Contract boundary

- [x] Backend docs have no stale decision conflicts.
- [x] Route-governance tests pass with 55 registered/54 canonical semantics.
- [x] Debug build succeeds.

### Phase 2: Transport and repository foundation

## Task 3: Define the expanded request contract and domain models

**Description:** Add patient-visible appointment types, type-specific
availability fields, expanded shared request-resource fields, and the complete
create body while retaining legacy response compatibility.

**Acceptance criteria:**

- [x] DTOs decode appointment types and all expanded request fields, using
      nullable/defaulted additions for legacy records.
- [x] Domain models expose type summary, alternatives, duration, referral,
      reservation semantics, and mapped-but-hidden `expiresAt` without
      serialization annotations.
- [x] Dead duplicate appointment-request/type wire models are removed from the
      confirmed-appointment DTO container.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*AppointmentRequestDtosTest" --tests "*AppointmentRequestStatusTest"`
- [x] `./gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentRequestDtos.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentV1Dtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AppointmentRequest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/AppointmentRequestDtosTest.kt`

**Estimated scope:** Medium (4 files)

---

## Task 4: Connect appointment types and type-specific requests

**Description:** Update Retrofit and repository contracts to load appointment
types, send type ID with availability, submit alternatives/referral/identity,
and map the shared expanded resource.

**Acceptance criteria:**

- [x] `getAppointmentTypes()` maps transport types to domain types at the
      repository boundary.
- [x] Availability always sends both `date` and `appointment_type_id`.
- [x] Create bodies contain only approved fields and shared expanded responses
      map consistently for list, detail, create, and cancel.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*AppointmentRequestApiServiceTest" --tests "*AppointmentRequestRepositoryImplTest"`
- [x] Inspect MockWebServer assertions for exact query/body field names.
- [x] `./gradlew assembleDebug`

**Dependencies:** Tasks 2 and 3

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentRequestApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentRequestRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/remote/api/AppointmentRequestApiServiceTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImplTest.kt`

**Estimated scope:** Medium (5 files)

### Checkpoint: Data path

- [x] Expanded and legacy DTO fixtures pass.
- [x] Retrofit queries and create bodies match the approved contract.
- [x] Repository mapping never leaks DTOs beyond the data layer.
- [x] Debug build succeeds.

### Phase 3: Request state machine

## Task 5: Add appointment-type selection state

**Description:** Introduce the Type wizard step with load, retry, selection, and
downstream invalidation behavior before scheduling can begin.

**Acceptance criteria:**

- [x] The initial state loads types and blocks progression until a type is
      selected.
- [x] Type failures expose retry without a hardcoded fallback.
- [x] Changing type clears primary/alternative slots and stale referral data.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest"`
- [x] `./gradlew assembleDebug`

**Dependencies:** Task 4

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModelTest.kt`

**Estimated scope:** Small (2 files)

---

## Task 6: Model primary and alternative time preferences

**Description:** Replace the single schedule selection with one required primary
and up to two ordered alternatives, all loaded for the selected type.

**Acceptance criteria:**

- [x] Availability state and jobs are keyed by both type ID and date; stale
      responses cannot overwrite current state.
- [x] Alternatives remain ordered, are distinct from the primary and each other,
      and cannot exceed two.
- [x] `SLOT_UNAVAILABLE` clears affected selections and refreshes current
      authoritative availability while preserving safe draft details.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest"`
- [x] Tests cover type/date races, add/remove/reorder boundaries, duplicates,
      max-two enforcement, and stale-slot recovery.
- [x] `./gradlew assembleDebug`

**Dependencies:** Task 5

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModelTest.kt`

**Estimated scope:** Small (2 files)

---

## Task 7: Validate referral details and submit the expanded request

**Description:** Extend Details and submission state with type-bound referral
validation while retaining current linked/unlinked identity behavior.

**Acceptance criteria:**

- [x] Referral types require a trimmed 1–255-character source; non-referral
      types clear and send `null`.
- [x] Linked accounts omit identity and unlinked accounts preserve existing
      verified-phone/profile validation.
- [x] Submission sends selected type, primary, ordered alternatives, reason,
      referral source, and permitted identity exactly once.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest" --tests "*AppointmentRequestIdentityTest"`
- [x] `./gradlew assembleDebug`

**Dependencies:** Task 6

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/ProfileAndReasonContent.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModelTest.kt`

**Estimated scope:** Medium (3 files)

### Checkpoint: State machine

- [x] All type, preference, referral, identity, and submission state tests pass.
- [x] No state transition can submit without type and primary time.
- [x] Debug build succeeds.

### Phase 4: Patient-facing wizard

## Task 8: Render the appointment-type step

**Description:** Add a patient-facing Type step showing the server label,
description, duration, and referral indicator, and wire it into Back/Continue
behavior and the four-step indicator.

**Acceptance criteria:**

- [x] Loading, retry, empty-catalog, selected, and unselected states render
      without a hardcoded type.
- [x] Continue requires a selection and advances to Schedule.
- [x] Back and step labels follow Type → Schedule → Details → Review.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest"`
- [x] `./gradlew assembleDebug`
- [x] Manual check: type cards display nullable descriptions and dynamic duration.

**Dependencies:** Task 7

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentTypeContent.kt` (new only if extraction keeps the screen focused)

**Estimated scope:** Small (1–2 files)

---

## Task 9: Render server-only primary and alternative scheduling

**Description:** Update Schedule to select a primary and optional alternatives
from server responses, reflect dynamic durations, and replace placeholder and
hardcoded closure behavior with real states.

**Acceptance criteria:**

- [x] No placeholder/generated slot or hardcoded Sunday restriction remains in
      the production request flow.
- [x] Primary and up to two alternatives can be added/removed with clear labels
      and duplicate prevention.
- [x] Loading, retry, closed-day, and no-times states cannot expose a selectable
      fake time.

**Verification:**

- [x] `rg -n "placeholderSlots|DayOfWeek.SUNDAY" app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentScreen.kt` finds no request-flow fallback/restriction.
- [x] `./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest"`
- [x] `./gradlew assembleDebug`

**Dependencies:** Task 8

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestScheduleContent.kt` (new only if cohesive extraction is needed)

**Estimated scope:** Small (1–2 files)

---

## Task 10: Complete Details, Review, and success presentation

**Description:** Surface conditional referral input and present the selected
type, duration, ordered preferences, reason, referral, and requester identity
before submission and after success.

**Acceptance criteria:**

- [x] Details shows referral source only when required and displays its 255
      character validation feedback.
- [x] Review shows all outbound patient-entered values in a stable hierarchy.
- [x] Success says the request awaits clinic review and never says a time is
      reserved or confirmed.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest"`
- [x] `./gradlew assembleDebug`
- [x] Manual check: linked/unlinked normal and referral requests match Review.

**Dependencies:** Task 9

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/ProfileAndReasonContent.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestReviewContent.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentScreen.kt`

**Estimated scope:** Medium (3 files)

### Checkpoint: End-to-end request flow

- [x] Type → Schedule → Details → Review works in both link states.
- [x] Normal/referral and zero/one/two alternative bodies are valid.
- [x] Empty and failed availability cannot be submitted.
- [x] Debug build succeeds.

### Phase 5: History, copy, and final reconciliation

## Task 11: Align request list, detail, and capacity copy

**Description:** Present expanded request information safely across history
surfaces and remove every stale hold/release/expiry promise.

**Acceptance criteria:**

- [x] Request cards/details show type and ordered preferences when available and
      remain safe for legacy records.
- [x] `expires_at` is not displayed; requested times are shown instead.
- [x] Pending, expired, cancellation, and accepted copy makes no capacity-hold
      claim.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*AppointmentRequestPresentationTest" --tests "*AppointmentRequestListPresentationTest" --tests "*AppointmentRequestDetailViewModelTest"`
- [x] `rg -n "held|hold expired|will be released|Expires" app/src/main/java/com/eyecare/app/presentation/appointments` returns no stale request copy.
- [x] `./gradlew assembleDebug`

**Dependencies:** Tasks 3 and 10

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestPresentation.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestPresentationTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentRequestListPresentationTest.kt`

**Estimated scope:** Medium (5 files)

---

## Task 12: Reconcile Android context and run final verification

**Description:** Update the living Android context and workflow artifact statuses
only after all production behavior and quality gates pass.

**Acceptance criteria:**

- [x] `CONTEXT.md` documents 55 registered routes, 54 canonical client routes,
      restored types, variable duration, alternatives, referral rules, and
      non-binding request semantics.
- [x] Spec, plan, and tasks accurately reflect the shipped implementation and
      completed verification; no checkbox is marked early.
- [x] No unrelated user-owned change is overwritten or reformatted.

**Verification:**

- [x] `./gradlew testDebugUnitTest`
- [x] `./gradlew assembleDebug`
- [x] `./gradlew lintDebug`
- [x] `./gradlew ktlintCheck`
- [x] `git diff --check`
- [x] Manual matrix: linked/unlinked × normal/referral × zero/two alternatives,
      plus empty day, type/date race, stale slot, legacy detail, and cancellation.

**Dependencies:** Task 11

**Files likely touched:**

- `CONTEXT.md`
- `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-spec.md`
- `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-plan.md`
- `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-tasks.md`

**Estimated scope:** Medium (4 files)

### Checkpoint: Complete

- [x] All v16 success criteria are met.
- [x] Focused and full tests pass.
- [x] Build, lint, formatting, and diff checks pass.
- [x] Manual behavior matrix passes.
- [x] Artifacts are ready for code review.

## Parallelization Opportunities

- Tasks 1 and 3 can be prepared independently only after the resolved decisions
  are treated as fixed, but Task 1 must land before contract assertions are
  considered authoritative.
- Task 8 and Task 11 can proceed in parallel after Tasks 3–7 because they use
  different primary presentation files.
- Tasks 9 and 10 should be sequential because both edit
  `RequestAppointmentScreen.kt`.
- Task 12 is always last.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Legacy resources omit expanded fields | High | Nullable response fields and legacy fixtures in Task 3 |
| Availability response race | High | Key state updates by type ID + date in Task 6 |
| Invalid duplicate alternatives | Medium | ViewModel uniqueness rules before UI in Task 6 |
| Referral/type stale state | High | Atomic dependent-state clearing in Tasks 5 and 7 |
| Fake availability survives migration | High | Static scan and empty/error behavior in Task 9 |
| Alias semantics distort route tests | Medium | Separate registered/canonical/legacy sets in Task 2 |
| Patient copy promises held capacity | Medium | Focused copy audit and tests in Task 11 |

## Open Questions

None. Phase 3 was approved on 2026-08-10. Phase 4 implementation is intentionally
paused until the user explicitly asks to start.

## Verification of Task-Breakdown Gate

- [x] Every task has explicit acceptance criteria.
- [x] Every task has executable verification or a precise manual check.
- [x] Dependencies are identified and ordered.
- [x] No task lists more than five likely files.
- [x] Checkpoints occur after each major phase.
- [x] Human has reviewed and approved the task breakdown — 2026-08-10.
