# Implementation Plan: Backend Alignment V13 — Appointment Requests and Intake Retirement

Status: Phase 2 approved — 2026-08-02

Approved specification:
`docs/specs/backend-alignment-v13-appointment-cutover-spec.md`

## Overview

Replace Android's removed appointment-type/direct-booking vertical with a
separate appointment-request vertical, then integrate requests beside confirmed
appointments without weakening the active Patient-link boundary. Retire intake
only after all navigation and appointment-detail dependencies are removed.
Finally, convert confirmed rescheduling to server availability and reconcile
shared error/session behavior and route governance.

The implementation is organized as buildable vertical slices. New request data
and domain foundations land before UI replacement; the old booking and intake
code remains compilable only until their replacements and callers are ready.
Each production slice ends with `assembleDebug`, and each checkpoint runs the
focused tests needed to prevent an invalid intermediate contract from moving
forward.

## Current-State Findings

The plan is based on these verified seams:

- `AppointmentV1ApiService` still declares retired `GET appointment-types` and
  direct `POST appointments` routes.
- `AppointmentV1Dtos` and `AppointmentV1Repository` still model patient-selected
  appointment types, contact notes, referring source, and direct creation.
- `BookAppointmentViewModel` loads appointment types first, uses confirmed
  appointment availability for creation, and returns an `AppointmentV1` as if
  booking were immediately confirmed.
- `BookAppointmentScreen` is a large four-step wizard whose type/referral and
  local review sections cannot be retained unchanged.
- no production API service, DTO, repository, domain model, ViewModel, or screen
  consumes any of the five appointment-request endpoints.
- `AppointmentListScreen` currently owns Upcoming/History filtering for confirmed
  visits only and its ViewModel loads immediately during construction.
- `NavGraph` treats `BookAppointment` and `BookAppointmentForReservation` as
  active-link clinical destinations, so unlinked accounts cannot reach a flow
  the backend explicitly permits.
- `PatientFeatureIntent` mixes account-only appointment requesting with
  active-link clinical destinations.
- `PatientIntakeApiService`, DTOs, repository, Hilt module, domain model,
  ViewModel, screen, tests, route, and appointment-detail action remain active
  even though all three backend routes are retired.
- `AppointmentDto` does not decode `reason_for_visit`.
- `RescheduleBottomSheet` fabricates clinic dates/times locally and never loads
  backend availability.
- `AppointmentV1ApiService.getAppointmentAvailability` still sends required
  `appointment_type_id` and optional `optometrist_id`; the V13 reschedule request
  requires only date plus `appointment_id`.
- `ApiErrorDecoder` already understands both the V13 `error` envelope and the
  older Laravel validation shape, but most non-auth repositories do not use it.
- `AuthInterceptor` centrally handles bearer-backed 401 responses but has no
  `ACTIVE_PATIENT_LINK_REQUIRED` event.
- current navigation guards already prevent normal limited-account access to
  most clinical screens, and Home avoids protected API calls when the link is
  inactive.
- route governance lists the new request endpoints as deferred and removed
  endpoints as rejected, but its test deliberately tolerates discovered rejected
  routes. That allows removed production Retrofit calls to remain green.
- the working tree contains user-owned auth/navigation/UI changes. Every task
  must patch around those changes rather than resetting or replacing them.

## Architecture Decisions

### 1. Keep appointment requests separate from confirmed appointments

Create a dedicated vertical:

```text
AppointmentRequestApiService
    ↓ wire DTOs
AppointmentRequestRepositoryImpl
    ↓ domain mapping/errors
AppointmentRequestRepository
    ↓
request list/detail/create ViewModels and Compose content
```

Do not add request operations to `AppointmentV1Repository`. An appointment
request is an account-owned workflow record, while `AppointmentV1` is a
clinical record behind the active-link boundary. Separate repositories make it
harder to accidentally expose confirmed data to a limited session and allow
independent pagination.

`AppointmentRequestApiService` owns exactly:

```text
GET  appointment-request-availability
GET  appointment-requests
POST appointment-requests
GET  appointment-requests/{id}
POST appointment-requests/{id}/cancel
```

The existing `AppointmentV1ApiService` is narrowed to confirmed list/detail,
confirmed availability, cancellation, and rescheduling.

### 2. Use distinct availability domain types

Request availability and confirmed reschedule availability share slot fields
but have different server semantics and metadata:

```text
AvailabilitySlot
├── startsAt
├── endsAt
├── available
└── reason?

AppointmentRequestAvailability
├── date / timezone
├── intervalMinutes
├── slotDurationMinutes
├── dayStatus / generatedAt
└── slots

AppointmentAvailability
├── date / timezone
├── intervalMinutes
├── appointmentTypeId?       response-only
├── visitDurationMinutes
├── appointmentId?
├── dayStatus / generatedAt
└── slots
```

Transport DTOs may share a slot DTO. Request DTOs must not contain a patient
selectable appointment-type field. Confirmed availability may decode derived
type/duration metadata but does not submit it as query input.

### 3. Model request status explicitly and fail closed

Create `AppointmentRequestStatus` with the five contract states plus `UNKNOWN`.
The domain model contains an optional confirmed appointment ID extracted from a
minimal nested appointment reference DTO. UI policy derives labels, cancel
eligibility, and confirmed navigation from domain status rather than raw strings.

The repository maps paginated results through the existing generic
`PaginatedResult<T>`. It never returns DTOs to presentation.

### 4. Make Appointments a coordinator surface

Retain `Appointments` as the bottom-navigation route. Refactor the screen into
a coordinator with **Requests** and **Confirmed** modes:

```text
AppointmentsScreen(hasActivePatientLink)
├── Requests content
│   └── AppointmentRequestListViewModel
└── Confirmed content
    ├── linked: existing confirmed list ViewModel/content
    └── limited: link-required content, no confirmed ViewModel/API call
```

Avoid one oversized ViewModel holding two pagination machines. Request and
confirmed list state stay independent. The coordinator owns only selected mode
and access-aware default selection.

The confirmed ViewModel must not be constructed or explicitly loaded for a
limited session. If refactoring requires a shared construction path, remove its
eager `init` load and make access-aware loading explicit.

Linked default: Confirmed. Limited/pending default: Requests. A later link-state
change recomputes access without discarding already loaded account-owned request
state.

### 5. Replace booking routes rather than aliasing them

Because the product is undeployed and legacy compatibility is not required,
replace:

```text
BookAppointment
BookAppointmentForReservation
```

with type-safe request routes:

```text
RequestAppointment(origin = STANDARD)
RequestAppointment(origin = FRAME_RESERVATION)
AppointmentRequestDetail(requestId)
```

The origin is presentation context only. It controls success copy and does not
carry frame or variant identifiers, persist a reservation draft, or alter the
request payload.

`PatientFeatureIntent` is split conceptually:

- account-only intent: request list/create/detail;
- active-link intent: confirmed appointment list/detail and all other clinical
  resources.

Navigation helpers must use an explicit route access policy instead of treating
every appointment-related route as a patient feature.

### 6. Build request creation as a server-driven state machine

Replace the booking ViewModel with a request ViewModel whose state owns:

```text
origin
step: date | time | reason | review
selectedDate?
availability state
selectedStartsAt?
reasonForVisit
field/generic errors
submission state
success request?
```

Use a cancellable availability job or a monotonically increasing selection key
so an older date response cannot overwrite a newer selection. Date changes
clear the slot. Only a slot marked available in the current response can
advance.

Request submission trims the reason, enforces the client-side 1000-character
limit, and sends only `scheduled_at` and `reason_for_visit`. Client validation is
for feedback; backend validation remains authoritative.

Machine-readable outcomes:

- `SLOT_UNAVAILABLE`: preserve reason/date, clear slot, return to time selection,
  and reload availability;
- `ACTIVE_REQUEST_LIMIT_REACHED`: preserve draft and offer Requests navigation;
- 429: retain draft and show retry-later copy;
- unknown error: retain draft and allow explicit retry;
- success: expose the created request and never manufacture an `AppointmentV1`.

### 7. Add request list and detail as independent states

The request list ViewModel owns pagination, refresh, append retry, and
deduplication by request ID. Server order is retained unless the contract
requires client sorting; Android does not reinterpret terminal state ordering.

Request detail owns load and cancel state. Cancel is single-flight and visible
only for `PENDING`. On `REQUEST_NOT_CANCELLABLE` or `REQUEST_TERMINAL`, it reloads
detail before rendering the action again. On 404/`REQUEST_NOT_OWNED`, it shows
neutral unavailable copy.

An accepted request can navigate to confirmed detail only when:

- `appointmentId` is non-null; and
- the current session has an active Patient link.

### 8. Convert confirmed rescheduling to repository-backed availability

Move availability orchestration into `AppointmentDetailViewModel` or a focused
reschedule state holder owned by it. The screen becomes a renderer of:

```text
selectedDate
availability loading/error/data
selected server slot
submission state/error
```

The ViewModel calls:

```text
getAppointmentAvailability(date, appointmentId)
```

and the repository sends only `date` and `appointment_id`. Remove local clinic
time generation as a selectable source. Pure formatting/date helpers may remain.

On reschedule `SLOT_UNAVAILABLE`, clear the slot and refresh the current date.
On success, replace the displayed appointment with the response and close the
sheet.

### 9. Retire intake after callers are removed

Deletion order:

1. remove intake action/callback from confirmed appointment detail;
2. remove intake route and `PatientFeatureIntent` mapping;
3. remove NavGraph destination/imports;
4. delete presentation ViewModel/screen;
5. delete domain repository/model;
6. delete data repository/DTO/service;
7. delete Hilt module;
8. delete obsolete intake tests and replace them with static retirement and
   appointment-detail policy assertions.

This order keeps the project buildable and makes dangling references obvious.

### 10. Standardize cutover errors at repository boundaries

Introduce or reuse a small repository helper that converts `HttpException`
through `ApiErrorDecoder` into `ApiDomainError`. Use it for:

- all new appointment-request operations;
- confirmed appointment list/detail/availability/cancel/reschedule; and
- frame-reservation creation where the appointment-request handoff still
  depends on actionable validation fields.

Remove direct `ApiErrorBody` decoding from those repositories. Intake decoding
disappears with intake. Job-order/rating migration is not required by the
appointment cutover unless route verification proves it blocks the standardized
contract; it should be scheduled separately rather than silently expanding this
plan.

Field errors continue to support both `error.details` and defensive legacy
`errors` parsing through the shared decoder.

### 11. Handle active-link loss as a global session event

Extend the existing bearer-aware network event path:

```text
HTTP 403 + code ACTIVE_PATIENT_LINK_REQUIRED
    ↓
AuthInterceptor peeks at (does not consume) the response body
    ↓
AuthEvent.PatientLinkRefreshRequired
    ↓
SessionViewModel resolves /me
    ↓
NavGraph observes Limited and exits protected destination
```

Do not clear the bearer token. Do not trigger on a generic 403. Preserve the
response body for Retrofit/repository error handling. Coalesce repeated events
through the existing shared event mechanism or an explicit single-flight
session refresh.

The navigation response is fail closed: when state becomes Limited while the
current destination is active-link protected, navigate to `LimitedAccount` and
clear the now-invalid protected destination from the top of the stack. Requests,
Profile, Home, Account Security, and link management remain available.

### 12. Make route governance enforce absence

Replace the auth-only/deferred tolerance model with three exact invariants:

1. every discovered Retrofit route is in the authoritative Android-consumed
   set;
2. every route Android claims to consume has a discovered Retrofit annotation;
3. no removed route is discovered in production services.

The authoritative set contains the 55 endpoint sections, including
`GET /appointment-request-availability`, even while the backend appendix count
is stale. If Android intentionally does not consume a valid backend endpoint,
route governance should represent that explicitly without calling it consumed.

The immediate cutover must at minimum eliminate all seven removed-route
discoveries and require all five request routes.

## Implementation Order

### Stage 1 — Contract tests and request foundations

1. Tighten route-governance expectations so current removed routes fail.
2. Add request DTO fixtures and encoding/decoding tests.
3. Add request status, availability, request, repository interface, service,
   repository implementation, and Hilt binding.
4. Add confirmed `reason_for_visit` and correct confirmed availability query
   signatures.

Why first: later UI work must compile against the final domain and transport
contract, and the failing route test prevents accidental fallback to removed
routes.

Checkpoint A:

- request and confirmed DTO/repository tests pass;
- new five request annotations are discovered;
- removed-route assertions are intentionally red only until Stage 4 deletion;
- `assembleDebug` passes where the checkpoint permits transitional legacy code.

### Stage 2 — Request creation vertical

1. Replace booking state with the server-driven request state machine.
2. Replace type/referral/contact-note UI with date, server slot, reason, review,
   and request-confirmation steps.
3. Add error behavior and date-response race protection.
4. Add standard versus frame-reservation-origin success copy.

Checkpoint B:

- request payload contract is exact;
- no UI path asks for appointment type;
- stale-slot and active-limit tests pass;
- frame-origin behavior never creates a reservation;
- `assembleDebug` passes.

### Stage 3 — Requests list/detail and combined destination

1. Add paginated request list and detail/cancel state holders.
2. Add request cards, status copy, empty/error/load-more states, and detail UI.
3. Refactor Appointments into Requests/Confirmed coordinator content.
4. Make linked/limited defaults and API loading access-aware.
5. Add accepted-to-confirmed navigation under active-link conditions.

Checkpoint C:

- linked and limited destination matrices pass;
- limited accounts make no confirmed API calls;
- list pagination and cancellation tests pass;
- accepted navigation fails closed without an ID or active link;
- `assembleDebug` passes.

### Stage 4 — Navigation replacement and direct-booking retirement

1. Replace booking routes with request routes and origin.
2. Update Home, Appointments, frame-reservation, and pending-intent callers.
3. Remove appointment-type/direct-create methods, DTOs, repository operations,
   models, and UI remnants.
4. Make route-governance removed-route checks green.

Checkpoint D:

- no production annotation calls appointment types or direct creation;
- unlinked request navigation succeeds;
- clinical navigation remains link-protected;
- route tests and `assembleDebug` pass.

### Stage 5 — Server-authoritative confirmed rescheduling

1. Add reschedule availability state and repository calls.
2. Convert the bottom sheet to server slots and remove local selectable-time
   generation.
3. Handle empty days, retries, date races, and stale-slot refresh.
4. Surface confirmed `reason_for_visit` in list/detail where appropriate.

Checkpoint E:

- only server-returned available slots can be submitted;
- query contains date and appointment ID, not type/provider inputs;
- `SLOT_UNAVAILABLE` refresh behavior is tested;
- confirmed appointment regressions and `assembleDebug` pass.

### Stage 6 — Intake retirement

Remove intake callers and layers in dependency order, then delete obsolete tests
and update appointment-detail policy coverage.

Checkpoint F:

- no production or navigation reference to Patient Intake remains;
- no intake Hilt binding or Retrofit annotation remains;
- confirmed appointment status/action tests pass;
- route tests and `assembleDebug` pass.

### Stage 7 — Error and link-state hardening

1. Apply shared error conversion to request, confirmed appointment, and
   reservation operations in scope.
2. Add the active-link-required network event without consuming response bodies.
3. Add single-flight `/me` refresh and protected-destination exit.
4. Verify generic 403 and 401 semantics remain distinct.

Checkpoint G:

- error-envelope tests pass for V13 and defensive legacy validation shapes;
- repeated link-required responses do not create navigation loops;
- 401 logs out; link-required 403 keeps the account authenticated;
- complete unit suite and `assembleDebug` pass.

### Stage 8 — Documentation and final verification

1. Reconcile the full Android route inventory.
2. Update `CONTEXT.md` to appointment requests, combined Appointments, server
   rescheduling, and intake retirement.
3. Run formatting only on touched Kotlin sources as needed.
4. Run focused tests, complete unit tests, build, lint, and manual matrices.

Checkpoint H:

- all specification success criteria are evidenced;
- `testDebugUnitTest`, `assembleDebug`, and `lintDebug` pass;
- no stale direct-booking/intake documentation remains;
- no unrelated worktree changes are staged or overwritten.

## Dependency Graph

```text
request contract/domain/repository
        ↓
request creation ──────────────┐
        ↓                      │
request list/detail            │
        ↓                      │
combined Appointments          │
        ↓                      │
route/caller replacement ◄─────┘
        ↓
delete direct booking/types

confirmed availability contract
        ↓
server-backed reschedule

remove intake callers
        ↓
delete intake layers

shared error decoder ──► request/appointment/reservation repositories
        ↓
active-link network event ──► session refresh ──► navigation exit

all streams
        ↓
route governance + documentation + final verification
```

## Parallel and Sequential Work

Potentially parallel after Stage 1 foundations are stable:

- request creation UI and request list/detail UI;
- confirmed reschedule conversion and request list/detail;
- intake caller inventory and static retirement-test preparation;
- shared error helper tests and visual request-card work.

Must remain sequential:

- DTO/domain contracts before repositories and ViewModels;
- request creation replacement before deleting direct-booking models;
- combined destination access policy before allowing unlinked navigation;
- removing intake callers before deleting intake layers;
- active-link event decoding before session/navigation reaction;
- all production migration before exact route-governance enforcement is green.

Parallel work must not edit the same navigation, appointment service, or shared
DTO file concurrently. The Phase 3 task breakdown will assign file ownership in
small slices.

## Migration and Deletion Strategy

There is no runtime data migration and no compatibility layer. Source migration
uses a short-lived compile-time overlap:

1. add new request types/services alongside old booking code;
2. move every UI/navigation caller to request APIs;
3. prove new flows and access boundaries;
4. delete direct-create/type code;
5. remove intake callers and delete intake vertical;
6. make static and route tests prove the old routes cannot return.

No deprecation annotations are needed because no released client must remain
source-compatible.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Accepted request nested appointment shape is broader than documented | Decode failure or missing navigation | Use minimal nullable reference DTO with required ID only and ignore unknown fields. |
| Combined screen constructs confirmed ViewModel for limited session | Unauthorized API call and noisy 403 | Branch before ViewModel construction or remove eager load and explicitly gate it; assert repository is never called. |
| Old availability response overwrites newer date | Patient submits unintended time | Cancel prior job and compare active date/request key before state mutation. |
| Slot changes between availability and submit | False success expectation | Handle `SLOT_UNAVAILABLE`, clear selection, refresh, preserve reason. |
| Ambiguous create failure causes duplicate request | Duplicate pending requests | Never auto-retry POST; explicit patient retry and backend active-limit/idempotency behavior remain authoritative. |
| Frame-reservation origin implies reservation is held | Misleading patient journey | Origin-specific success copy; carry no frame IDs and submit no reservation. |
| Removing intake breaks appointment detail action layout | Build or visual regression | Remove callbacks first, retain focused status/action tests, then delete layers. |
| 403 body inspection consumes Retrofit response | Repository loses error payload | Use OkHttp `peekBody` with a small bound; return the original response unchanged. |
| Repeated protected requests create refresh/navigation storms | Loop or flicker | Single-flight session refresh and idempotent navigation exit. |
| Global error migration expands indefinitely | Delayed appointment delivery | Limit repository conversion to request, confirmed appointment, and reservation seams named in the spec. |
| Route appendix says 54 while endpoint sections say 55 | Incorrect governance count | Use explicit endpoint set and document the known backend-doc arithmetic issue. |
| Dirty auth/navigation work overlaps cutover | User changes overwritten | Inspect diffs before every patch; make narrow edits and never reset unrelated files. |
| Large existing booking screen encourages risky rewrite | UI regressions | Replace behavior in vertical slices with ViewModel tests and preserve reusable date/format components only when contract-compatible. |

## Verification Commands

Run from the repository root after setting Android Studio's JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

```powershell
# Focused during contract/foundation work
.\gradlew testDebugUnitTest --tests "*AppointmentRequest*" --tests "*AppointmentV1DtosTest" --tests "*AppointmentV1RepositoryImplTest" --tests "*ApiRouteAllowlistTest" --tests "*ApiErrorDecoderTest"

# Focused during UI/state work
.\gradlew testDebugUnitTest --tests "*Appointment*" --tests "*AccountAccessTest" --tests "*FrameReservation*"

# Mandatory after every production task
.\gradlew assembleDebug

# Checkpoints and final
.\gradlew testDebugUnitTest
.\gradlew lintDebug
```

Manual final matrix:

1. linked account: Confirmed default, Requests available, request accepted link;
2. unlinked account: Requests default, create/list/cancel work, Confirmed locked;
3. pending-review account: same request access without protected calls;
4. request statuses: pending, accepted, rejected, cancelled, expired, unknown;
5. create errors: stale slot, active limit, 429, validation, offline ambiguity;
6. frame-reservation origin: correct post-request guidance and no reservation;
7. reschedule: loading, closed/empty day, stale slot, retry, success;
8. confirmed statuses: no intake action anywhere;
9. active link revoked mid-protected-screen: remains signed in and exits safely;
10. generic 403 versus bearer 401 remain distinct.

## Documentation Deliverables

- approved specification status updated through each gate;
- this implementation plan approved before task breakdown;
- `docs/specs/backend-alignment-v13-appointment-cutover-tasks.md` created in
  Phase 3;
- `CONTEXT.md` updated only after implementation reflects the new reality;
- backend documents left unchanged, with their known route-count inconsistency
  recorded in Android planning artifacts.

## Exit Criteria for Phase 2

- major components and boundaries are explicit;
- implementation order preserves buildability;
- request versus confirmed access is unambiguous;
- direct booking and intake have a safe deletion sequence;
- error and active-link behavior have a bounded architecture;
- risks have concrete mitigations;
- checkpoints and verification commands are reviewable;
- no production implementation has begun.
