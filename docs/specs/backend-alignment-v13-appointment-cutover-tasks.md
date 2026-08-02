# Tasks: Backend Alignment V13 — Appointment Requests and Intake Retirement

Status: Phase 3 draft — awaiting approval

Approved inputs:

- `docs/specs/backend-alignment-v13-appointment-cutover-spec.md`
- `docs/specs/backend-alignment-v13-appointment-cutover-plan.md`

## Execution Rules

1. Execute tasks in dependency order unless a listed parallel opportunity is
   explicitly authorized.
2. Begin each behavior change with a focused failing test where practical, then
   implement the minimum behavior needed to pass it.
3. Keep each task within its declared files. If a discovered dependency would
   exceed five files, update this task document and obtain approval before
   expanding that task.
4. Inspect the dirty worktree before every task and preserve all unrelated
   user-owned changes, especially current auth, navigation, and UI work.
5. Never reset, checkout, overwrite, or reformat an unrelated modified file.
6. Use Kotlinx Serialization only and map DTOs to domain models at the repository
   boundary.
7. Do not store request reasons, appointment records, clinical data, tokens, or
   other sensitive flow values in Room.
8. Do not add a dependency, Gradle plugin, Room migration, backend change, or CI
   change without separate approval.
9. Run `assembleDebug` after every task that changes production code.
10. Do not leave a checkpoint with a known failing test or build. Diagnose before
    moving forward.
11. Update the specification first if implementation evidence invalidates an
    approved behavior or boundary.
12. Do not commit automatically. Commit only when explicitly requested.

Set the Android Studio JBR before Gradle commands:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Phase A — Appointment-Request Contract Foundation

### Task 1: Add request status and domain models

- [ ] Create `AppointmentRequestStatus` with `PENDING`, `ACCEPTED`, `REJECTED`,
      `CANCELLED`, `EXPIRED`, and fail-closed `UNKNOWN` mapping.
- [ ] Create serialization-free `AppointmentRequest`,
      `AppointmentRequestAvailability`, and shared slot domain types.
- [ ] Keep confirmed `AppointmentV1` separate from account-owned requests.

Acceptance:

- Every documented request status maps deterministically.
- An unknown status cannot be treated as pending, cancellable, or confirmed.
- The request model includes nullable patient, expiry, cancellation, and
  confirmed-appointment reference fields without transport annotations.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentRequestStatusTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/domain/model/AppointmentRequest.kt` (new)
- `app/src/test/java/com/eyecare/app/domain/model/AppointmentRequestStatusTest.kt` (new)

### Task 2: Add appointment-request DTO contract tests and wire models

- [ ] Write decoding fixtures for request availability, all request statuses,
      nullable fields, pagination, cancellation, and accepted appointment ID.
- [ ] Write an encoding assertion proving create payload contains only
      `scheduled_at` and `reason_for_visit`.
- [ ] Implement Kotlinx Serialization DTOs that satisfy those fixtures.

Acceptance:

- Accepted requests decode a minimal nested appointment reference by ID while
  ignoring additional fields.
- Unknown response fields do not break decoding.
- No patient ID, appointment type, contact note, provider, or duration is
  serialized in the create body.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentRequestDtosTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentRequestDtos.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/dto/AppointmentRequestDtosTest.kt` (new)

### Task 3: Declare the five appointment-request Retrofit routes

- [ ] Add a dedicated service with request availability, paginated list,
      create, detail, and cancel operations.
- [ ] Add focused reflection/source assertions for exact HTTP methods, paths,
      path variables, and query names.

Acceptance:

- The service declares exactly the five contract endpoints.
- Availability accepts only `date`.
- Create accepts the exact DTO from Task 2.
- List supports `page` and `per_page`.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentRequestApiServiceTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentRequestApiService.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/api/AppointmentRequestApiServiceTest.kt` (new)

### Task 4: Extract shared repository HTTP error conversion

- [ ] Add a small data-layer helper that maps `HttpException` through the
      existing `ApiErrorDecoder`.
- [ ] Preserve already-domain errors and non-HTTP failures.
- [ ] Test V13 `error`, legacy validation fallback, empty body, and malformed
      body behavior.

Acceptance:

- Repositories can reuse one function without consuming an error body twice.
- Machine-readable code, patient-safe message, status, and field details reach
  `ApiDomainError`.
- No response body is logged.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*RepositoryApiCallTest" --tests "*ApiErrorDecoderTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/repository/RepositoryApiCall.kt` (new)
- `app/src/test/java/com/eyecare/app/data/repository/RepositoryApiCallTest.kt` (new)

### Task 5: Implement the appointment-request repository boundary

- [ ] Add the domain repository interface.
- [ ] Implement all five operations with DTO-to-domain mapping and shared error
      conversion.
- [ ] Test request/response payloads, pagination metadata, status mapping,
      accepted appointment ID, and machine-readable failures with MockWebServer.

Acceptance:

- No DTO escapes the data layer.
- Server list ordering is preserved.
- Request creation sends the exact two-field body.
- All HTTP errors are returned as `ApiDomainError`.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentRequestRepositoryImplTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentRequestRepository.kt` (new)
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImpl.kt` (new)
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImplTest.kt` (new)

### Task 6: Bind the appointment-request vertical with Hilt

- [ ] Provide `AppointmentRequestApiService` from Retrofit.
- [ ] Bind `AppointmentRequestRepositoryImpl` to its domain interface.
- [ ] Add no new singleton state or dependency.

Acceptance:

- Hilt resolves the request repository from an Android ViewModel.
- Existing appointment bindings remain intact.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/di/AppointmentRequestModule.kt` (new)

### Task 7: Decode confirmed appointment reason for visit

- [ ] Add nullable `reason_for_visit` to confirmed appointment DTO and domain
      mapping.
- [ ] Extend DTO and repository fixtures for present and absent values.

Acceptance:

- Existing confirmed responses without the field remain valid.
- Present values reach `AppointmentV1.reasonForVisit` at the repository boundary.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentV1DtosTest" --tests "*AppointmentV1RepositoryImplTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentV1Dtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AppointmentV1.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/AppointmentV1DtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImplTest.kt`

### Checkpoint A: Contract foundation

- [ ] Tasks 1–7 are complete.
- [ ] New request DTO/service/repository focused tests pass.
- [ ] Confirmed appointment reason remains backward compatible.
- [ ] `assembleDebug` passes.
- [ ] No UI or navigation has switched to the new flow prematurely.

## Phase B — Server-Driven Request Creation

### Task 8: Define and test the request-creation state machine

- [ ] Add request steps, availability substate, draft fields, origin, success,
      and error policy to a new ViewModel.
- [ ] Start with tests for date changes, available-only selection, required
      reason, 1000-character limit, and single-flight submission.
- [ ] Use cancellable/latest-response-wins availability loading.

Acceptance:

- A stale date response cannot overwrite the active date.
- Changing date clears the selected time.
- Only a currently available server slot can advance.
- The ViewModel never asks for or stores appointment type, referral, contact
  note, provider, or frame identifiers.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModelTest.kt` (new)

### Task 9: Implement request submission error and success semantics

- [ ] Add tests and behavior for `SLOT_UNAVAILABLE`,
      `ACTIVE_REQUEST_LIMIT_REACHED`, 429, validation, ambiguous network failure,
      and success.
- [ ] Preserve reason/date on retryable failures.
- [ ] Add standard versus frame-reservation-origin success presentation state.

Acceptance:

- Stale slot clears and availability refreshes.
- Active-limit error offers a Requests exit without discarding the draft.
- POST is never automatically retried.
- Success holds an `AppointmentRequest`, not a confirmed appointment.
- Frame origin changes copy only and performs no reservation call.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModelTest.kt`

### Task 10: Build date and server-slot request steps

- [ ] Add the Request appointment scaffold, date selection, availability
      loading/error/empty states, and server slot list.
- [ ] Reuse formatting components only when they do not reproduce local
      capacity rules.
- [ ] Make unavailable slots non-selectable and label the screen as a request.

Acceptance:

- The screen contains no appointment-type or provider chooser.
- Past dates cannot advance.
- No selectable time is fabricated locally.
- Retry reloads the current date through the ViewModel.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentScreen.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAvailabilityContent.kt` (new)

### Task 11: Build reason, review, and confirmation steps

- [ ] Add the required reason field with counter/validation.
- [ ] Add review rows for requested date, requested time, and reason.
- [ ] Add pending-confirmation success and frame-origin guidance.
- [ ] Disable duplicate submission while loading.

Acceptance:

- Review and success never call the request confirmed or booked.
- The reason is not logged or persisted.
- Frame-origin success tells the patient to return after clinic confirmation.
- Navigation callbacks distinguish request detail and Requests list exits.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestReviewContent.kt` (new)

### Checkpoint B: Request creation

- [ ] Tasks 8–11 are complete.
- [ ] Request state-machine tests pass.
- [ ] Encoded create body remains exactly two fields.
- [ ] Frame-origin success performs no reservation operation.
- [ ] `assembleDebug` passes.

## Phase C — Request List, Detail, and Combined Appointments

### Task 12: Implement the paginated request-list ViewModel

- [ ] Add initial load, refresh, append, append retry, deduplication, and empty
      state.
- [ ] Test all pagination transitions and error recovery.
- [ ] Preserve backend ordering.

Acceptance:

- Initial and append failures have distinct states.
- Duplicate request IDs are not rendered twice.
- Refresh resets pagination without mixing previous pages.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentRequestListViewModelTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestListViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestListViewModelTest.kt` (new)

### Task 13: Build request list cards and status policy

- [ ] Add patient-facing labels for all five statuses plus unknown.
- [ ] Render request number, requested date/time, reason, expiry where useful,
      and accepted linkage indicator.
- [ ] Add loading, empty, initial error, pagination, and retry content.
- [ ] Test pure status-label and action policy functions.

Acceptance:

- Unknown is never shown as pending or confirmed.
- Only pending cards advertise cancellation availability.
- Reason text is bounded in cards without losing it in detail.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentRequestPresentationTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestListContent.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestPresentation.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestPresentationTest.kt` (new)

### Task 14: Implement request-detail load and cancellation state

- [ ] Add detail load, retry, single-flight cancellation, and refreshed terminal
      state.
- [ ] Handle `REQUEST_NOT_CANCELLABLE`, `REQUEST_TERMINAL`,
      `REQUEST_NOT_OWNED`, and neutral 404 behavior.
- [ ] Gate confirmed navigation by appointment ID and active link.

Acceptance:

- Cancel is exposed only for pending state.
- Concurrent terminal-state errors trigger refresh before actions reappear.
- Not-owned/not-found copy does not disclose another account's record.
- Accepted navigation fails closed without both prerequisites.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentRequestDetailViewModelTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestDetailViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestDetailViewModelTest.kt` (new)

### Task 15: Build request-detail UI

- [ ] Render canonical status, request metadata, full reason, expiry/cancelled
      timestamps, and accepted appointment action.
- [ ] Add cancellation confirmation and loading/error feedback.
- [ ] Use active-link input only for confirmed-navigation visibility.

Acceptance:

- Terminal requests have no cancel action.
- Accepted request action appears only when permitted by Task 14 policy.
- Cancellation result updates immediately.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestDetailScreen.kt` (new)

### Task 16: Refactor Appointments into Requests and Confirmed modes

- [ ] Add a coordinator surface with access-aware default selection.
- [ ] Preserve existing Upcoming/History controls inside Confirmed content.
- [ ] Embed request list content in Requests mode.
- [ ] Show link-required Confirmed content for limited sessions without loading
      the confirmed repository.
- [ ] Add pure coordinator-policy tests.

Acceptance:

- Linked defaults to Confirmed; limited/pending defaults to Requests.
- Both modes remain available to linked accounts.
- Limited Confirmed view makes zero confirmed API calls.
- Request pagination state is independent of confirmed pagination state.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentsCoordinatorTest" --tests "*AppointmentListViewModelTest" --tests "*AppointmentRequestListViewModelTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentsCoordinator.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentsCoordinatorTest.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentListViewModelTest.kt`

### Checkpoint C: Combined Appointments

- [ ] Tasks 12–16 are complete.
- [ ] Request list/detail/cancel tests pass.
- [ ] Linked and limited coordinator matrices pass.
- [ ] No limited session calls confirmed appointment APIs.
- [ ] `assembleDebug` passes.

## Phase D — Navigation Cutover and Direct-Booking Retirement

### Task 17: Define account-only and active-link navigation policy

- [ ] Add type-safe `RequestAppointment(origin)` and
      `AppointmentRequestDetail(requestId)` routes.
- [ ] Replace broad appointment feature classification with explicit route
      access policy.
- [ ] Test account-only requests versus active-link confirmed/clinical routes.

Acceptance:

- Request create/list/detail are reachable by every authenticated session.
- Confirmed appointment list/detail and other clinical features remain linked-only.
- Unknown protected route classification fails closed.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*PatientFeatureIntentTest" --tests "*PatientRouteAccessTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientFeatureIntent.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientRouteAccess.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt` (new)

### Task 18: Wire request routes into NavGraph and main entry points

- [ ] Register request creation and detail destinations.
- [ ] Pass link state into the Appointments coordinator and request detail.
- [ ] Replace Home and Appointments **Book** callbacks/copy with **Request**.
- [ ] Preserve pending protected destination behavior only for clinical routes.

Acceptance:

- Unlinked navigation reaches request creation without visiting LimitedAccount.
- Accepted detail can navigate to confirmed detail only when linked.
- Home/Appointments entry points use request terminology.
- No old booking destination is invoked by these callers.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*PatientRouteAccessTest" --tests "*AccountAccessTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListScreen.kt`

### Task 19: Replace the frame-reservation booking handoff

- [ ] Route no-eligible-appointment action to request creation with frame origin.
- [ ] Replace **Book appointment** copy with request-and-confirmation guidance.
- [ ] Remove refresh-after-booking behavior that assumes immediate confirmation.
- [ ] Test that request success cannot create or select a reservation appointment.

Acceptance:

- Request success does not auto-return a confirmed appointment.
- No frame or variant identifier enters the appointment-request payload/state.
- The patient is told to return after clinic confirmation.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*CreateFrameReservationViewModelTest" --tests "*FrameReservationEligibilityTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

### Task 20: Delete the legacy booking presentation vertical

- [ ] Remove old booking screen, ViewModel, and their obsolete tests after all
      callers use request routes.
- [ ] Retain only contract-compatible formatting helpers used elsewhere.
- [ ] Remove stale imports and route references in the same files.

Acceptance:

- No production symbol named `BookAppointmentScreen`,
  `BookAppointmentViewModel`, or `BookingResult` remains.
- Request creation remains buildable and navigable.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentScreen.kt` (delete)
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentViewModel.kt` (delete)
- `app/src/test/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentViewModelTest.kt` (delete)
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/TimeFormat.kt` (retain, move, or delete based on live consumers)

### Task 21: Remove appointment-type and direct-create domain APIs

- [ ] Remove `getAppointmentTypes` and `createAppointment` from the repository
      contract and implementation.
- [ ] Remove their Retrofit annotations and request DTOs.
- [ ] Delete the unused `AppointmentType` model.

Acceptance:

- Production code cannot call `GET appointment-types` or direct
  `POST appointments` creation.
- Confirmed list/detail/cancel/reschedule remain available.
- No compatibility overload retains the removed behavior.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentV1DtosTest" --tests "*AppointmentV1RepositoryImplTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentV1ApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentV1Dtos.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentV1Repository.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AppointmentType.kt` (delete)

### Task 22: Replace obsolete direct-booking data tests

- [ ] Remove encoding and repository tests for direct creation and appointment
      types.
- [ ] Retain and strengthen confirmed list/detail/cancel behavior.
- [ ] Add static assertions that removed annotations are absent.

Acceptance:

- Tests no longer normalize or excuse the removed routes.
- Confirmed appointment coverage stays intact.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentV1DtosTest" --tests "*AppointmentV1RepositoryImplTest" --tests "*ApiRouteAllowlistTest"
```

Files:

- `app/src/test/java/com/eyecare/app/data/remote/dto/AppointmentV1DtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

### Checkpoint D: Direct-booking retirement

- [ ] Tasks 17–22 are complete.
- [ ] Every request entry point uses request routes.
- [ ] Removed appointment-type/direct-create annotations are absent.
- [ ] Unlinked request navigation works while clinical routes remain protected.
- [ ] Focused tests and `assembleDebug` pass.

## Phase E — Server-Authoritative Confirmed Rescheduling

### Task 23: Correct confirmed availability contract and repository API

- [ ] Change confirmed availability query input to date plus appointment ID.
- [ ] Stop sending appointment type and optometrist query parameters.
- [ ] Keep derived response metadata nullable/response-only as appropriate.
- [ ] Convert confirmed appointment repository HTTP failures through the shared
      error helper.

Acceptance:

- Recorded request URL contains `date` and `appointment_id` only.
- Repository exposes `getAppointmentAvailability(date, appointmentId)`.
- `SLOT_UNAVAILABLE` reaches presentation as `ApiDomainError`.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentV1RepositoryImplTest" --tests "*AppointmentV1DtosTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentV1ApiService.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentV1Repository.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AppointmentAvailability.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImplTest.kt`

### Task 24: Add server-availability reschedule state to appointment detail

- [ ] Model selected date, availability loading/data/error, selected slot, and
      submission state in `AppointmentDetailViewModel`.
- [ ] Add latest-date-response protection.
- [ ] Test load, retry, available-only selection, empty day, stale slot refresh,
      and success.

Acceptance:

- Opening reschedule does not fabricate slots.
- Only a server-returned available slot can submit.
- `SLOT_UNAVAILABLE` clears selection and reloads current date.
- Successful response replaces the current appointment.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentDetailViewModelTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModelTest.kt`

### Task 25: Replace local reschedule time generation with server slots

- [ ] Render date selection, availability loading/error/closed/empty states,
      server slot selection, and review confirmation.
- [ ] Remove local clinic-time generation as a selectable source.
- [ ] Connect sheet callbacks to Task 24 state.

Acceptance:

- Every selectable time came from the active server response.
- Date change clears prior selection visually and in state.
- Retry and stale-slot feedback are patient-safe.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/RescheduleBottomSheet.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`

### Task 26: Present confirmed reason for visit

- [ ] Show nullable reason in confirmed appointment detail with patient-friendly
      hierarchy.
- [ ] Add it to list cards only when it materially helps identification and does
      not crowd existing status/date information.
- [ ] Keep `contact_notes` distinct and read-only.

Acceptance:

- Present reason is visible in detail; absent reason creates no empty section.
- Reason is never labeled staff note or intake.
- Existing contact-note presentation remains read-only.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListScreen.kt`

### Checkpoint E: Confirmed appointment alignment

- [ ] Tasks 23–26 are complete.
- [ ] Reschedule uses server availability and corrected query shape.
- [ ] Stale-slot behavior is tested.
- [ ] Confirmed reason is optional and correctly separated from contact notes.
- [ ] Appointment-focused tests and `assembleDebug` pass.

## Phase F — Patient Intake Retirement

### Task 27: Remove intake actions and navigation entry points

- [ ] Remove the appointment-detail intake callback and action for every status.
- [ ] Remove `PatientIntake` route and protected-intent mapping.
- [ ] Remove NavGraph destination/imports.
- [ ] Update appointment action-policy tests.

Acceptance:

- No confirmed appointment status exposes intake.
- Navigation cannot construct an intake destination.
- Other appointment actions retain their existing status rules.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentDetail*" --tests "*PatientFeatureIntentTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientFeatureIntent.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`

### Task 28: Delete intake presentation code and tests

- [ ] Delete intake screen and ViewModel after all callers are gone.
- [ ] Delete the obsolete ViewModel test.

Acceptance:

- No production presentation package references intake.
- Build remains green before lower layers are removed.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/intake/PatientIntakeScreen.kt` (delete)
- `app/src/main/java/com/eyecare/app/presentation/intake/PatientIntakeViewModel.kt` (delete)
- `app/src/test/java/com/eyecare/app/presentation/intake/PatientIntakeViewModelTest.kt` (delete)

### Task 29: Delete intake data and domain layers

- [ ] Delete intake Retrofit service, DTOs, data repository, domain repository,
      and domain model.

Acceptance:

- No production code can call or represent the retired intake API.
- No Room schema or migration changes occur.

Verify:

```powershell
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/remote/api/PatientIntakeApiService.kt` (delete)
- `app/src/main/java/com/eyecare/app/data/remote/dto/PatientIntakeDtos.kt` (delete)
- `app/src/main/java/com/eyecare/app/data/repository/PatientIntakeRepositoryImpl.kt` (delete)
- `app/src/main/java/com/eyecare/app/domain/repository/PatientIntakeRepository.kt` (delete)
- `app/src/main/java/com/eyecare/app/domain/model/PatientIntake.kt` (delete)

### Task 30: Delete intake Hilt wiring and obsolete data tests

- [ ] Delete the intake module and DTO/repository tests.
- [ ] Add or strengthen a static retired-route assertion in the route test.

Acceptance:

- Hilt contains no intake binding.
- No intake annotation or source reference remains.
- Route governance fails if an intake route is reintroduced.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/di/PatientIntakeModule.kt` (delete)
- `app/src/test/java/com/eyecare/app/data/remote/dto/PatientIntakeDtosTest.kt` (delete)
- `app/src/test/java/com/eyecare/app/data/repository/PatientIntakeRepositoryImplTest.kt` (delete)
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

### Checkpoint F: Intake removal

- [ ] Tasks 27–30 are complete.
- [ ] No production/test source except retirement assertions mentions intake.
- [ ] All three intake Retrofit annotations are absent.
- [ ] Appointment action regressions, route tests, and `assembleDebug` pass.

## Phase G — Error and Active-Link Hardening

### Task 31: Migrate reservation validation to the shared error decoder

- [ ] Replace direct `ApiErrorBody` parsing in frame-reservation creation.
- [ ] Preserve appointment/item field validation behavior using
      `ApiDomainError.fieldErrors`.
- [ ] Test the V13 error envelope and defensive legacy shape.

Acceptance:

- Existing reservation field feedback remains specific.
- Machine-readable codes and patient-safe messages are retained.
- Raw `HttpException` and raw bodies do not reach the ViewModel for handled
  HTTP failures.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*FrameReservationRepositoryImplTest" --tests "*CreateFrameReservationViewModelTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/repository/FrameReservationRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModel.kt`
- `app/src/test/java/com/eyecare/app/data/repository/FrameReservationRepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModelTest.kt`

### Task 32: Emit active-link-required network events safely

- [ ] Add a distinct `PatientLinkRefreshRequired` event.
- [ ] On bearer-backed 403, peek at a bounded response body and emit only when
      the decoded code is `ACTIVE_PATIENT_LINK_REQUIRED`.
- [ ] Preserve the original response for Retrofit/repository decoding.
- [ ] Test 401, matching 403, generic 403, public responses, and body preservation.

Acceptance:

- 401 behavior remains unchanged.
- Matching 403 never clears the token.
- Generic 403 never triggers link refresh.
- Response content is still readable by the downstream consumer.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AuthInterceptorTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/data/remote/interceptor/AuthEventBus.kt`
- `app/src/main/java/com/eyecare/app/data/remote/interceptor/AuthInterceptor.kt`
- `app/src/test/java/com/eyecare/app/data/remote/interceptor/AuthInterceptorTest.kt`

### Task 33: Refresh session link state without logging out

- [ ] Make `SessionViewModel` react to the new event with a single-flight `/me`
      refresh.
- [ ] Preserve bearer token on linked-to-limited transitions.
- [ ] Test repeated events, linked result, limited result, transient failure,
      and 401 during refresh.

Acceptance:

- Repeated events do not start overlapping `/me` requests.
- Inactive/unknown link resolves to `SessionState.Limited`.
- Network failure does not falsely log out or claim an active link.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*SessionViewModelTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/auth/SessionViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/SessionViewModelTest.kt`

### Task 34: Exit protected destinations after active-link loss

- [ ] Observe linked-to-limited transitions in navigation.
- [ ] If the current destination is active-link protected, navigate to limited
      access and remove the invalid protected destination from the top.
- [ ] Leave request, Home, Profile, Account Security, and link-management
      destinations in place.
- [ ] Add pure navigation decision tests.

Acceptance:

- Link loss on a clinical screen fails closed without logout.
- Link loss while viewing a request does not interrupt the account-only flow.
- Repeated state emissions do not loop or stack duplicate LimitedAccount screens.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*PatientRouteAccessTest" --tests "*SessionNavigationPolicyTest"
.\gradlew assembleDebug
```

Files:

- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientRouteAccess.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/SessionNavigationPolicy.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/navigation/SessionNavigationPolicyTest.kt` (new)

### Checkpoint G: Error and link-state behavior

- [ ] Tasks 31–34 are complete.
- [ ] Request, confirmed appointment, and reservation HTTP failures use shared
      domain errors.
- [ ] 401 logout and active-link 403 refresh behaviors remain distinct.
- [ ] Repeated events create no refresh or navigation loop.
- [ ] Complete unit suite and `assembleDebug` pass.

## Phase H — Exact Route Governance, Documentation, and Final Verification

### Task 35: Reconcile Android route governance to the V13 contract

- [ ] Replace auth-only/deferred tolerance with an explicit authoritative
      consumed-route inventory.
- [ ] Require every consumed route to exist and every discovered route to be
      consumed.
- [ ] Fail on every removed route.
- [ ] Include `GET appointment-request-availability` and document the known
      backend appendix/count discrepancy.

Acceptance:

- All five request routes are required and discovered.
- Appointment-type, direct-create, and intake routes are forbidden and absent.
- No discovered production Retrofit route is merely tolerated as rejected.
- The test reports actionable missing, extra, and removed sets.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
```

Files:

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

### Task 36: Update project context after implementation

- [ ] Replace direct-booking/current appointment-type documentation with
      appointment requests.
- [ ] Document Requests/Confirmed access behavior, server rescheduling, and
      frame-reservation handoff.
- [ ] Remove intake routes, packages, and feature claims.
- [ ] Record the new request vertical and error/link-state behavior.

Acceptance:

- `CONTEXT.md` describes the implemented app, not the retired V12 behavior.
- It does not claim backend changes or deployment.
- No stale intake or direct-create route remains in current-state sections.

Verify:

```powershell
rg -n "appointment-types|POST.*/appointments$|appointments/.*/intake|Patient intake|direct booking" CONTEXT.md
```

Files:

- `CONTEXT.md`

### Task 37: Run final automated verification and close the living documents

- [ ] Run focused request, appointment, reservation, access, route, and error
      tests.
- [ ] Run the complete JVM unit suite.
- [ ] Run debug assembly and Android lint.
- [ ] Run `git diff --check` on all touched files.
- [ ] Mark spec, plan, and tasks implementation status complete only after all
      required evidence is green.

Acceptance:

- All approved success criteria have test/build/manual evidence.
- No unrelated worktree change is staged, overwritten, or claimed.
- Any environment-only warning is distinguished from a product failure.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*AppointmentRequest*" --tests "*Appointment*" --tests "*FrameReservation*" --tests "*AccountAccess*" --tests "*PatientRouteAccess*" --tests "*ApiRouteAllowlistTest" --tests "*ApiErrorDecoderTest" --tests "*AuthInterceptorTest"
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
git diff --check
```

Files:

- `docs/specs/backend-alignment-v13-appointment-cutover-spec.md`
- `docs/specs/backend-alignment-v13-appointment-cutover-plan.md`
- `docs/specs/backend-alignment-v13-appointment-cutover-tasks.md`

### Checkpoint H: Ready for handoff

- [ ] Tasks 35–37 are complete.
- [ ] All final commands pass.
- [ ] Manual verification matrix from the approved plan is complete or any
      emulator/backend limitation is explicitly reported.
- [ ] No production source references retired booking or intake routes.
- [ ] Android consumes appointment requests for linked and limited accounts.
- [ ] The living specification documents match the implementation.

## Approved Parallel Opportunities

Only after their prerequisite checkpoint is green:

- Tasks 10 and 12 may run in parallel after Tasks 8–9.
- Tasks 13 and 14 may run in parallel after Task 12 and Task 5 respectively.
- Tasks 24 and 27 may run in parallel after Checkpoint D, provided their file
  ownership does not overlap `AppointmentDetailScreen.kt`; otherwise execute
  sequentially.
- Task 31 may run alongside Tasks 27–30 after Checkpoint E.
- Task 32 can begin after Task 4 but Tasks 33–34 remain sequential after it.

No parallel edit may target `NavGraph.kt`, `AppointmentDetailScreen.kt`,
`AppointmentV1ApiService.kt`, or the same test file concurrently.

## Phase 3 Approval Gate

Approval of this document authorizes only the listed implementation scope and
order. Phase 4 implementation begins only after explicit human approval. Any
material scope, route, persistence, dependency, or backend-contract change
returns to the appropriate earlier gate before code continues.

