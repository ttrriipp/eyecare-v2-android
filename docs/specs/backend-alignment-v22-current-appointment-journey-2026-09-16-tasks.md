# Backend Alignment V22 — Current Appointment Journey Tasks

Status: Draft for approval — 2026-09-16

Related documents:

- `docs/specs/backend-alignment-v22-current-appointment-journey-2026-09-16-spec.md`
- `docs/specs/backend-alignment-v22-current-appointment-journey-2026-09-16-plan.md`
- `docs/API_CONTRACT.md`
- `docs/BACKEND_CONTEXT.md`

## Execution rules

- Do not edit `docs/API_CONTRACT.md` or `docs/BACKEND_CONTEXT.md`; they are backend-owned inputs with existing user changes.
- Implement tasks in order unless a dependency explicitly permits parallel work.
- Add or update focused tests with each behavior change; do not defer all testing to the end.
- Keep DTO-to-domain conversion at repository boundaries and use Kotlinx Serialization only.
- Do not add Room persistence, a new backend endpoint, or a new dependency for this feature.
- Run `./gradlew assembleDebug` after every production-code task, as required by `AGENTS.md`.
- Stop at each checkpoint for review before beginning the next phase.
- Do not commit automatically. Commit only when the user explicitly requests it.

## Phase 0 — Establish the baseline

### Task 1 — Record the pre-change verification baseline

**Depends on:** None

**Likely files:** This task file only, if baseline notes are needed.

**Work:**

- Run the existing appointment, request, home, route-access, API-governance, and repository unit tests.
- Run a debug build.
- Record any pre-existing failures before production files are changed.

**Acceptance criteria:**

- Existing failures, if any, are distinguished from regressions introduced by V22.
- The project produces a debug build or the exact pre-existing blocker is documented.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*Appointment*" --tests "*HomeViewModelTest" --tests "*PatientRouteAccessTest" --tests "*ApiRouteAllowlistTest"
./gradlew assembleDebug
```

## Phase 1 — Contract and repository foundation

### Task 2 — Model and decode the current-journey union

**Depends on:** Task 1

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/domain/model/CurrentAppointmentJourney.kt` (new)
- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentRequestDtos.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/AppointmentRequestDtosTest.kt`

**Work:**

- Add the domain union for `none`, `pending_request`, and `appointment`.
- Add transport DTOs for `GET /appointment-requests/current`, including the appointment's original request and optional pending change request.
- Cover all three variants plus nullable/optional nested fields with decoding tests based on the contract examples.

**Acceptance criteria:**

- Every documented `data.kind` decodes into an explicit domain state.
- Unknown or malformed kinds fail predictably instead of silently becoming `none`.
- Pending-change data can coexist with a confirmed appointment.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentRequestDtosTest"
./gradlew assembleDebug
```

### Task 3 — Extract reusable appointment-request mapping

**Depends on:** Task 2

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRequestMappers.kt` (new)
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImplTest.kt`

**Work:**

- Move request DTO-to-domain conversion into an internal reusable mapper.
- Keep the existing request-list and request-detail behavior unchanged.
- Add mapper coverage for requested time, alternative times, status, cancellation metadata, and linked appointment identifiers.

**Acceptance criteria:**

- Existing request repository operations still return equivalent domain values.
- The current-journey repository can reuse the mapper without duplicating conversion logic.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentRequestRepositoryImplTest"
./gradlew assembleDebug
```

### Task 4 — Extract reusable confirmed-appointment mapping

**Depends on:** Task 2

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/data/repository/AppointmentV1Mappers.kt` (new)
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImplTest.kt`

**Work:**

- Move confirmed-appointment DTO-to-domain conversion into an internal reusable mapper.
- Preserve pagination, status, clinic, schedule, cancellation, and rating behavior.
- Make the mapper reusable by both current-journey and history operations.

**Acceptance criteria:**

- Existing appointment repository results remain equivalent.
- Current and history paths can share one appointment conversion implementation.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentV1RepositoryImplTest"
./gradlew assembleDebug
```

### Task 5 — Add the current-journey API operation and governance coverage

**Depends on:** Task 2

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentRequestApiService.kt`
- `app/src/test/java/com/eyecare/app/data/remote/api/AppointmentRequestApiServiceTest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/api/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/api/ApiRouteAllowlistTest.kt`

**Work:**

- Add `GET appointment-requests/current` to the Retrofit service.
- Verify the exact method and path with MockWebServer.
- Add the endpoint to the approved route inventory.

**Acceptance criteria:**

- The API method sends exactly one authenticated-style GET request to `/appointment-requests/current`.
- Route-governance tests recognize the endpoint without weakening unrelated checks.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentRequestApiServiceTest" --tests "*ApiRouteAllowlistTest"
./gradlew assembleDebug
```

### Task 6 — Expose the current journey through the request repository

**Depends on:** Tasks 3, 4, and 5

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentRequestRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImplTest.kt`

**Work:**

- Add `getCurrentAppointmentJourney()` to the existing request repository boundary.
- Map each transport variant into the domain union using the shared request and appointment mappers.
- Preserve API failure details through the repository's established error model.

**Acceptance criteria:**

- `none`, `pending_request`, and `appointment` map correctly.
- A confirmed appointment remains the primary result when a pending change request is included.
- Repository tests cover success and error propagation.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentRequestRepositoryImplTest"
./gradlew assembleDebug
```

### Task 7 — Add a history-only appointment repository operation

**Depends on:** Task 4

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentV1ApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentV1Repository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImplTest.kt`

**Work:**

- Add an intent-specific `getAppointmentHistory(page, perPage)` repository operation.
- Ensure its transport request always includes `filter=history`.
- Preserve pagination and appointment mapping.

**Acceptance criteria:**

- No history call can omit or substitute the required filter.
- Tests assert `filter=history`, page, and page-size query parameters.
- Existing repository behavior remains intact until legacy callers are retired.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentV1RepositoryImplTest"
./gradlew assembleDebug
```

### Checkpoint A — Contract readiness

- Review DTO names and nullability against the updated backend documents.
- Confirm mapper reuse and the repository boundary before adding UI state.
- Confirm focused tests and `assembleDebug` pass.

## Phase 2 — Read-only My Appointment experience

### Task 8 — Build the My Appointment load and refresh state machine

**Depends on:** Task 6 and Checkpoint A

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModelTest.kt` (new)

**Work:**

- Model initial loading, content, empty, and recoverable error states.
- Load only from `getCurrentAppointmentJourney()`.
- Add retry and explicit refresh without blanking valid content unnecessarily.

**Acceptance criteria:**

- Each union variant produces deterministic presentation state.
- Retry recovers from a failed first load.
- Refresh can preserve current content while a request is in flight.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*MyAppointmentViewModelTest"
./gradlew assembleDebug
```

### Task 9 — Extract full-detail request content for reuse

**Depends on:** Task 8

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/CurrentAppointmentRequestContent.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentRequestDetailScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/appointment/CurrentAppointmentRequestContentTest.kt` (new)

**Work:**

- Extract focused request-detail sections instead of embedding the existing screen.
- Present status, requested time, alternatives, notes, and linked details without a card tap.
- Keep the existing deep-linked request detail screen behavior intact.

**Acceptance criteria:**

- Pending-request information is visible directly in the current screen content.
- Alternatives are clearly distinguished from the requested time.
- Existing request-detail rendering continues to use equivalent formatting.

**Verify:**

```powershell
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

### Task 10 — Extract confirmed appointment and pending-change content

**Depends on:** Task 8

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/CurrentConfirmedAppointmentContent.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentDetailScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/appointment/CurrentConfirmedAppointmentContentTest.kt` (new)

**Work:**

- Extract focused confirmed-appointment sections for reuse.
- Keep confirmed appointment details visually primary.
- Render an optional pending time-change request beneath them with an explicit “not yet confirmed” label and alternatives.

**Acceptance criteria:**

- The confirmed schedule cannot be mistaken for a pending proposed schedule.
- Original-request access remains available from the confirmed state.
- Existing deep-linked appointment detail remains functional.

**Verify:**

```powershell
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

### Task 11 — Compose the read-only My Appointment screen

**Depends on:** Tasks 8, 9, and 10

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentScreen.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/appointment/MyAppointmentScreenTest.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentFormattingTest.kt`

**Work:**

- Add the “My Appointment” top bar, prominent status header, and organized full-detail sections.
- Render loading, error, `none`, `pending_request`, and `appointment` states.
- Show “Request an appointment” for `none` and a History action in the top app bar.

**Acceptance criteria:**

- Important active-journey details require no intermediate card tap.
- Empty and error states expose the correct primary action.
- Screen semantics identify status and the pending-change warning.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentFormattingTest"
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

### Checkpoint B — Read-only journey review

- Review all three journey variants on compact and large font settings.
- Confirm the confirmed appointment dominates when a pending change exists.
- Confirm the visual hierarchy matches the approved single-screen concept before actions are added.

## Phase 3 — Current-journey actions and reconciliation

### Task 12 — Centralize current-journey action eligibility

**Depends on:** Checkpoint B

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentCurrentActionPolicy.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentCurrentActionPolicyTest.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentDetailViewModelTest.kt`

**Work:**

- Consolidate status and timing rules for cancel and time-change actions.
- Reuse the policy from current and detail experiences.
- Preserve backend authorization as authoritative; client rules only guide affordances.

**Acceptance criteria:**

- Current and detail screens cannot drift on action availability.
- Boundary times and unsupported statuses are covered by unit tests.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentCurrentActionPolicyTest" --tests "*AppointmentDetailViewModelTest"
./gradlew assembleDebug
```

### Task 13 — Add pending-request actions to My Appointment

**Depends on:** Task 12

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointment/CurrentAppointmentRequestContent.kt`

**Work:**

- Wire allowed request cancellation/edit actions using existing repository operations.
- Add progress, confirmation, success, and recoverable failure states.
- Prevent duplicate submissions while a mutation is active.

**Acceptance criteria:**

- Only actions allowed by status are exposed.
- Mutation failures preserve visible request details and can be retried.
- Rapid repeated taps issue at most one mutation.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*MyAppointmentViewModelTest"
./gradlew assembleDebug
```

### Task 14 — Add confirmed-appointment cancellation

**Depends on:** Task 12

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointment/CurrentConfirmedAppointmentContent.kt`

**Work:**

- Wire confirmed cancellation through the existing appointment repository.
- Reuse established confirmation and reason capture where applicable.
- Keep content stable during submission and surface backend rejection clearly.

**Acceptance criteria:**

- Cancellation availability follows the shared action policy.
- Success and failure paths are covered without optimistic removal of the appointment.
- Duplicate submissions are blocked.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*MyAppointmentViewModelTest"
./gradlew assembleDebug
```

### Task 15 — Add appointment time-change creation

**Depends on:** Tasks 12 and 14

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointment/RescheduleBottomSheet.kt`

**Work:**

- Load available slots and submit a time-change request through existing operations.
- Keep the confirmed schedule visible while the change request is pending.
- Disable creation when the aggregate already contains a pending change.

**Acceptance criteria:**

- Slot loading, empty availability, submission, and backend errors are covered.
- A successful submission does not immediately replace the confirmed appointment time.
- The UI cannot start a second pending time change.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*MyAppointmentViewModelTest"
./gradlew assembleDebug
```

### Task 16 — Reconcile every mutation with the current endpoint

**Depends on:** Tasks 13, 14, and 15

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/MyAppointmentViewModelTest.kt`

**Work:**

- Refetch `GET /appointment-requests/current` after successful mutation calls.
- Serialize or ignore stale refresh responses so older data cannot overwrite newer state.
- Handle transitions among pending request, confirmed appointment, pending change, and none.

**Acceptance criteria:**

- The post-mutation UI is derived from the authoritative aggregate response.
- Mutation success plus refresh failure yields a recoverable state without false confirmation.
- Tests cover out-of-order refresh completion and all expected state transitions.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*MyAppointmentViewModelTest"
./gradlew assembleDebug
```

### Checkpoint C — Action safety review

- Exercise cancellation and time-change actions with slow and failing responses.
- Confirm the confirmed schedule remains primary while a change is pending.
- Confirm all successful mutations reconcile through the current endpoint.

## Phase 4 — Appointment History

### Task 17 — Build the history paging state machine

**Depends on:** Task 7 and Checkpoint C

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentHistoryViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentHistoryViewModelTest.kt` (new)

**Work:**

- Load only through `getAppointmentHistory`.
- Model initial load, empty, pagination, retry, and refresh independently from My Appointment.
- Prevent duplicate next-page requests.

**Acceptance criteria:**

- History never uses the current endpoint or an unfiltered appointment list.
- Initial and pagination failures have distinct retry paths.
- Refresh resets pagination deterministically.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentHistoryViewModelTest"
./gradlew assembleDebug
```

### Task 18 — Extract reusable history rows and rating content

**Depends on:** Task 17

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentHistoryContent.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentListScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/appointment/AppointmentHistoryContentTest.kt` (new)

**Work:**

- Extract the useful historical appointment card, rating, and status presentation from the legacy list.
- Exclude active request-list concepts and current/upcoming filters.
- Preserve navigation to the existing appointment detail route.

**Acceptance criteria:**

- Historical rows show date, clinic/service context, terminal status, and rating affordance when eligible.
- No request-history tab or active-list control appears.
- Row semantics and tap target behavior are covered.

**Verify:**

```powershell
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

### Task 19 — Compose the standalone Appointment History screen

**Depends on:** Tasks 17 and 18

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentHistoryScreen.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/appointment/AppointmentHistoryScreenTest.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentFormattingTest.kt`

**Work:**

- Add a dedicated top bar, list, empty state, pagination indicator, and error recovery.
- Keep rating submission behavior available for eligible completed appointments.
- Keep history state independent from the active journey.

**Acceptance criteria:**

- The screen can render and paginate without a My Appointment state object.
- Empty history is clearly distinct from “No active appointment.”
- Rating updates do not force a reload of the active journey.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentFormattingTest"
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

### Checkpoint D — History review

- Confirm every history network request includes `filter=history`.
- Confirm request history is absent by design.
- Review empty, long-list, paging-error, and rating states.

## Phase 5 — Navigation and workflow cutover

### Task 20 — Route Appointments to the new journey and add History

**Depends on:** Tasks 11 and 19 and Checkpoint D

**Likely files (5):**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientRouteAccess.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientFeatureIntent.kt`

**Work:**

- Keep the bottom-navigation label “Appointments” while its root renders My Appointment.
- Add a separate Appointment History destination opened from the top app bar.
- Apply account-only access to My Appointment and active-link access to History, preserving existing detail deep links.

**Acceptance criteria:**

- Bottom navigation has one active-journey destination, not Requests/Confirmed lists.
- History access and route guards match the approved plan.
- Existing appointment and request detail routes remain resolvable.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*PatientRouteAccessTest"
./gradlew assembleDebug
```

### Task 21 — Refresh My Appointment after request workflows

**Depends on:** Task 20

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/RequestAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/RequestAppointmentViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

**Work:**

- Replace list-based active-request preflight with the current aggregate.
- Return from successful request creation to My Appointment and trigger a refresh.
- Preserve form state and recoverable errors on failed submission.

**Acceptance criteria:**

- A new request appears on My Appointment without app restart.
- Preflight uses the single source of truth and blocks conflicting active journeys.
- Back navigation does not create duplicate My Appointment destinations.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest" --tests "*MyAppointmentViewModelTest"
./gradlew assembleDebug
```

### Task 22 — Replace detail-screen pending-change discovery

**Depends on:** Task 20

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentDetailViewModelTest.kt`

**Work:**

- Remove page-one request-list inference from appointment detail.
- Use the current aggregate to identify a pending change associated with the displayed appointment.
- Preserve deep-link behavior for historical appointments that are not current.

**Acceptance criteria:**

- Pending-change discovery no longer depends on request ordering or pagination.
- Historical detail still loads when the current aggregate points elsewhere or is empty.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*AppointmentDetailViewModelTest"
./gradlew assembleDebug
```

### Task 23 — Source the Home visit ticket from the current aggregate

**Depends on:** Task 20

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/home/HomeViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/home/HomeViewModelTest.kt`

**Work:**

- Replace unfiltered appointment-list inference with `getCurrentAppointmentJourney()`.
- Continue showing only the existing confirmed next-visit ticket.
- Do not add a pending-request card to Home.

**Acceptance criteria:**

- Home shows a ticket only for the aggregate's confirmed appointment.
- `none` and `pending_request` do not create a fake confirmed visit.
- Existing Home error isolation remains intact.

**Verify:**

```powershell
./gradlew testDebugUnitTest --tests "*HomeViewModelTest"
./gradlew assembleDebug
```

### Checkpoint E — End-to-end workflow review

- Walk through none → request → pending request → confirmed appointment → pending time change → history.
- Verify process-death/deep-link entry to appointment and request details.
- Verify account/link access transitions and back-stack behavior.

## Phase 6 — Retire legacy active-list architecture

### Task 24 — Remove obsolete coordinator and request-list state

**Depends on:** Tasks 20–23 and Checkpoint E

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentsCoordinator.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentsCoordinatorTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentRequestListViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentRequestListViewModelTest.kt`

**Work:**

- Confirm there are no remaining runtime references.
- Remove the Requests/Confirmed coordinator and request-list state machine.
- Keep request detail and request mutation capabilities that remain in use.

**Acceptance criteria:**

- No production code exposes the old Requests/Confirmed switcher.
- Request-detail and current-journey tests continue to pass.

**Verify:**

```powershell
rg -n "AppointmentsCoordinator|AppointmentRequestListViewModel" app/src
./gradlew testDebugUnitTest --tests "*AppointmentRequest*" --tests "*MyAppointmentViewModelTest"
./gradlew assembleDebug
```

### Task 25 — Remove the obsolete combined appointment list

**Depends on:** Task 24

**Likely files (5):**

- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointment/AppointmentListViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentListViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentRequestListPresentationTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointment/AppointmentFormattingTest.kt`

**Work:**

- Remove the legacy Upcoming/History and Requests/Confirmed list UI after extracted components are in use.
- Move any remaining formatting coverage to the new screen/content tests.
- Confirm no old filters or tab labels remain in runtime resources.

**Acceptance criteria:**

- Appointments no longer has an active appointment/request list implementation.
- History and reusable formatting/rating behavior survive independently.
- No dead navigation callbacks or presentation tests reference the removed UI.

**Verify:**

```powershell
rg -n "AppointmentListScreen|AppointmentListViewModel|Upcoming|Confirmed" app/src/main app/src/test app/src/androidTest
./gradlew testDebugUnitTest --tests "*Appointment*"
./gradlew assembleDebug
```

### Task 26 — Remove obsolete unfiltered active-list repository use

**Depends on:** Tasks 22, 23, and 25

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentV1Repository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentV1ApiService.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentV1RepositoryImplTest.kt`

**Work:**

- Search all callers of the generic unfiltered appointment-list operation.
- Remove or narrow it when no supported flow requires it.
- Retain detail, mutation, rating, availability, and history operations.

**Acceptance criteria:**

- No active-journey behavior is inferred from `GET /appointments` without `filter=history`.
- Supported appointment detail and mutation behavior remains covered.
- Dead service/repository methods are removed rather than left as alternate sources of truth.

**Verify:**

```powershell
rg -n "getAppointments\(" app/src
./gradlew testDebugUnitTest --tests "*AppointmentV1RepositoryImplTest" --tests "*HomeViewModelTest" --tests "*AppointmentDetailViewModelTest"
./gradlew assembleDebug
```

### Checkpoint F — Legacy-removal review

- Confirm no Requests/Confirmed, Upcoming/History, or page-one inference remains.
- Confirm direct detail routes still work.
- Review the deletion diff before final hardening.

## Phase 7 — Hardening and handoff

### Task 27 — Complete accessibility and resilient-layout coverage

**Depends on:** Checkpoint F

**Likely files (3):**

- `app/src/androidTest/java/com/eyecare/app/presentation/appointment/MyAppointmentScreenTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/appointment/AppointmentHistoryScreenTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/appointment/CurrentConfirmedAppointmentContentTest.kt`

**Work:**

- Cover large font, long clinic/service text, empty/error actions, status semantics, and pending-change wording.
- Verify touch targets and that critical actions are reachable without relying on color alone.
- Add regression cases for content that can scroll beyond one viewport.

**Acceptance criteria:**

- Critical details and actions remain readable and reachable at increased font scale.
- Status and pending-change meaning are exposed through text/semantics, not color only.
- Compact-device tests do not clip primary actions.

**Verify:**

```powershell
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

### Task 28 — Run final gates and close the V22 documents

**Depends on:** Task 27

**Likely files (4):**

- `CONTEXT.md`
- `docs/specs/backend-alignment-v22-current-appointment-journey-2026-09-16-spec.md`
- `docs/specs/backend-alignment-v22-current-appointment-journey-2026-09-16-plan.md`
- `docs/specs/backend-alignment-v22-current-appointment-journey-2026-09-16-tasks.md`

**Work:**

- Run the complete unit suite, lint, debug build, and connected tests when a device/emulator is available.
- Update project context and mark documents implemented only after all required gates pass.
- Record any intentionally deferred manual or device-only verification explicitly.

**Acceptance criteria:**

- Unit tests, lint, and debug build pass.
- Connected tests pass when infrastructure is available, or the unavailable infrastructure is documented without claiming a pass.
- Documentation reflects the shipped architecture and contains no stale active-list guidance.

**Verify:**

```powershell
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

### Checkpoint G — Implementation approval gate

- Review the complete diff against the approved specification and plan.
- Review test evidence and any documented environment limitations.
- Commit only after the user explicitly requests it.

## Definition of done

- Appointments opens one full-detail My Appointment journey sourced only from `GET /appointment-requests/current`.
- `none`, `pending_request`, and `appointment` states match the approved behavior.
- A pending time change is subordinate to the still-confirmed appointment and is clearly unconfirmed.
- History is a separate destination sourced only from `GET /appointments?filter=history`.
- Existing request and appointment detail deep links remain functional.
- Home no longer infers the next visit from an unfiltered appointment list.
- The legacy combined active-list architecture and its page-one inference are removed.
- Relevant tests, lint, and build gates pass, with device-only limitations documented accurately.
