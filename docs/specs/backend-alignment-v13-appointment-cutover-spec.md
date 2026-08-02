# Backend Alignment V13 — Appointment Requests and Intake Retirement

Status: Phase 1 approved — 2026-08-02

## Objective

Complete the deferred non-authentication portion of the V13 Android cutover by
replacing direct mobile appointment creation with clinic-reviewed appointment
requests, retiring the removed patient-intake feature, and bringing confirmed
appointment rescheduling and non-auth API errors onto the authoritative mobile
contract.

The primary user is an authenticated patient account. Both linked and unlinked
accounts can request an appointment. Only an account with an active Patient link
can view or mutate confirmed appointments and access the other clinical
resources.

Success means:

- **Request appointment** uses only the five appointment-request endpoints;
- patients never select an internal appointment type;
- the Appointments destination clearly separates requests from confirmed visits;
- unlinked accounts can request and track visits without receiving access to
  clinical records;
- confirmed rescheduling uses server-generated capacity instead of local time
  guesses;
- no Android runtime path calls a removed appointment-type, direct-booking, or
  intake endpoint; and
- non-auth repositories understand the backend's machine-readable error
  envelope.

This specification does not change the backend, authentication UX, clinical
record schemas, frame-reservation rules, or the already implemented Eyewear
experience.

## Sources of Truth

1. `docs/API_CONTRACT.md`, backend state dated 2026-08-02, specifically:
   - Appointment Requests;
   - Appointment Availability;
   - Confirmed Appointments;
   - Error Responses;
   - Coordinated Breaking Changes; and
   - Retired Features.
2. `docs/BACKEND_CONTEXT.md`, especially the account-only versus active-link
   route boundary.
3. The approved `docs/specs/backend-alignment-v13-auth-*` documents for
   session resolution, Patient-link state, and account-only navigation.
4. Current Android code for appointment booking, confirmed appointments,
   patient intake, frame-reservation eligibility, navigation, and shared API
   error decoding.
5. This specification for Android presentation, route ownership, and migration
   behavior.

The backend documents are user-owned inputs and are not edited by this Android
cutover. They currently contain 55 endpoint sections but the appendix omits
`GET /appointment-request-availability` and reports 54. Android treats that
endpoint as authoritative because it is fully specified, appears in
`BACKEND_CONTEXT.md`, and is listed in the contract's New Routes table.

## Confirmed Product Decisions and Assumptions

1. The app and backend have not been deployed. Removed mobile routes are deleted
   without compatibility adapters, feature flags, or legacy fallbacks.
2. The work remains part of V13 and is named
   `backend-alignment-v13-appointment-cutover`.
3. Patient-facing **Book appointment** language becomes **Request appointment**.
4. The existing Appointments destination becomes one coherent area with two
   views: **Requests** and **Confirmed**.
5. A linked account opens the **Confirmed** view by default to preserve the
   established appointments experience. An unlinked or pending-review account
   opens **Requests** because confirmed visits require an active Patient link.
6. Requests show all server-returned statuses: `pending`, `accepted`,
   `rejected`, `cancelled`, and `expired`.
7. Confirmed visits continue to show `scheduled`, `checked_in`, `fulfilled`,
   `cancelled`, and `no_show` statuses.
8. Creating a request collects only:
   - server-returned appointment-request availability;
   - one selected `scheduled_at`; and
   - required free-text `reason_for_visit`, maximum 1000 characters.
9. Patients do not choose appointment type, optometrist, visit duration,
   referring source, or a separate contact note while requesting a visit.
10. Android does not locally infer whether a requested slot is available. It
    submits only a currently selected available slot returned by
    `GET /appointment-request-availability`.
11. A successful request is not presented as a confirmed booking. Confirmation
    copy explains that clinic review is required.
12. Unlinked and pending-review accounts can list, create, inspect, and cancel
    their own appointment requests.
13. Confirmed appointments and all other clinical-resource destinations remain
    protected by the active Patient-link boundary.
14. An accepted request may offer **View confirmed appointment** when its
    nested `appointment` contains an ID. Android otherwise shows accepted state
    without inventing a confirmed appointment identifier.
15. Only pending requests expose **Cancel request**. Cancellation relies on the
    backend as final authority and handles `REQUEST_NOT_CANCELLABLE` safely if
    state changed concurrently.
16. Android paginates request and confirmed lists independently using their
    respective response metadata.
17. Pending-request holds, the two-active-request limit, expiry, acceptance,
    rejection, and patient resolution are backend-owned. Android displays
    returned state and does not reproduce those rules locally.
18. `SLOT_UNAVAILABLE` returns the request flow to refreshed server availability
    while preserving the reason for visit.
19. `ACTIVE_REQUEST_LIMIT_REACHED` keeps the patient in the request flow and
    explains that an existing pending request must be resolved or cancelled.
20. The frame-reservation prerequisite continues to require an eligible
    confirmed appointment. A newly created appointment request does not satisfy
    that prerequisite.
21. When a patient enters Request appointment from frame reservation, success
    explains that the frame can be reserved after clinic confirmation. Android
    does not persist, automatically replay, or submit a pending frame reservation.
22. The three patient-intake endpoints, their repository/data/domain types,
    ViewModel, Compose screen, navigation route, and appointment-detail entry
    point are removed outright.
23. No intake data is migrated or retained locally because the app does not
    store it in Room and the backend feature is retired.
24. Confirmed appointment DTO/domain/UI support `reason_for_visit` as nullable
    patient-facing information. Existing `contact_notes` remains read-only when
    returned, but there is no patient edit endpoint.
25. Confirmed rescheduling loads
    `GET /appointment-availability?date=...&appointment_id=...` and shows only
    server-returned available slots.
26. Rescheduling does not send `appointment_type_id` or `optometrist_id`; the
    backend derives duration, type, and provider capacity from the appointment.
27. Local clinic-hour and duration calculations do not authorize a reschedule.
    Local date rules may prevent obviously invalid past dates, but server
    availability is authoritative.
28. A reschedule `SLOT_UNAVAILABLE` response refreshes availability for the
    selected date and requires a new available selection.
29. The shared `ApiErrorDecoder` becomes the common repository-boundary decoder
    for non-auth HTTP failures touched by this cutover. It retains compatibility
    with the older Laravel `message`/`errors` envelope only as a defensive parser,
    not as an approved route contract.
30. Patient-safe backend error messages may be displayed. Raw response bodies,
    exception dumps, request payloads, and clinical data are never logged.
31. `403 ACTIVE_PATIENT_LINK_REQUIRED` from a protected feature is treated as a
    stale session-link state: Android refreshes `/me`, routes to limited access
    when the link is no longer active, and does not log the user out.
32. A generic 403 without that machine-readable code remains a feature error;
    Android does not infer link loss from HTTP status alone.
33. A 401 continues to use the existing bearer-aware global logout behavior.
34. The full Android route allowlist is reconciled to the 55 actual contract
    endpoints. Removed appointment/intake routes cannot remain tolerated as
    discovered Retrofit calls.
35. Existing quotation, job-order, billing-record, Eyewear, prescription,
    frame, reservation, conversation, and rating behavior remains unchanged
    unless a focused contract test reveals a direct dependency on removed
    appointment or intake behavior.
36. No new library, database schema, Room entity, worker, service, or backend
    change is needed.

## Patient Experience

### Appointments destination

The Appointments destination contains two top-level views:

```text
Appointments
┌──────────────┬──────────────┐
│   Requests   │  Confirmed   │
└──────────────┴──────────────┘
```

For linked accounts:

- **Confirmed** is selected initially;
- both views are available;
- the primary action is **Request appointment**.

For unlinked or pending-review accounts:

- **Requests** is selected initially;
- request list, request detail, cancellation, and new-request flow work normally;
- **Confirmed** shows a link-required explanation and account-link action;
- no confirmed-appointment API call is made while the link is inactive.

Each request card shows:

- request number;
- requested date and time in clinic-local formatting;
- reason for visit;
- patient-friendly status;
- expiry information for pending requests when useful; and
- a clear indication when an accepted request has become a confirmed visit.

Status copy:

| API status | Patient-facing label | Meaning |
|---|---|---|
| `pending` | Awaiting clinic review | The requested time is held while staff review it. |
| `accepted` | Confirmed | Staff accepted the request and created an appointment. |
| `rejected` | Not approved | The clinic could not approve this request. |
| `cancelled` | Cancelled | The patient cancelled the request. |
| `expired` | Expired | The request was not resolved before its hold expired. |
| unknown | Status unavailable | Fail closed without treating it as pending or accepted. |

### Request appointment flow

```text
Choose date
    ↓
Load request availability
    ↓
Choose available server slot
    ↓
Enter reason for visit
    ↓
Review request
    ↓
Submit
    ↓
Request sent — awaiting clinic confirmation
```

Requirements:

- past dates cannot be selected;
- availability is loaded after a date is selected;
- unavailable slots remain visible only when useful for context and cannot be
  selected;
- changing the date clears the selected slot;
- reason for visit is required, trimmed, and limited to 1000 characters;
- review uses **Requested date**, **Requested time**, and **Reason for visit**;
- submit is single-flight and prevents duplicate taps;
- success navigates to the new request detail or Requests view;
- request creation never navigates directly to confirmed appointment detail.

### Frame-reservation handoff

When Request appointment was opened because no eligible confirmed appointment
exists for a frame reservation, request success additionally states:

> Your appointment request was sent. Once the clinic confirms it, return to the
> frame and reserve it for that visit.

No raw frame selection is persisted across process death, and no reservation
request is issued from an appointment-request success response.

### Request detail and cancellation

Request detail displays the same canonical request state as the list. A pending
request offers cancellation with confirmation. Successful cancellation updates
the detail immediately and refreshes the list when the patient returns.

Accepted requests show **View confirmed appointment** only when the response
contains `appointment.id` and the account has an active Patient link.

### Confirmed appointments

The existing confirmed list/detail experience remains, with these changes:

- `reason_for_visit` is decoded and shown when present;
- no intake action exists in any status;
- no contact-note edit action exists;
- Request appointment replaces Book appointment;
- confirmed rescheduling uses server availability rather than locally generated
  times.

### Confirmed rescheduling

```text
Choose date
    ↓
GET /appointment-availability?date=…&appointment_id=…
    ↓
Choose available server slot
    ↓
Review
    ↓
POST /appointments/{id}/reschedule
```

The sheet shows loading, empty-day, retry, unavailable-slot, and submission
states. It does not display a locally fabricated selectable time.

## Backend Contract Consumed by This Cutover

### Account-only appointment-request endpoints

```text
GET  /api/v1/appointment-request-availability?date={Y-m-d}
GET  /api/v1/appointment-requests?page={page}&per_page={perPage}
POST /api/v1/appointment-requests
GET  /api/v1/appointment-requests/{appointmentRequest}
POST /api/v1/appointment-requests/{appointmentRequest}/cancel
```

Create request body:

```json
{
  "scheduled_at": "2026-08-10T09:00:00+08:00",
  "reason_for_visit": "Blurred vision in my left eye"
}
```

The Android request model contains no appointment type, contact note,
referring source, optometrist, Patient ID, or duration field.

The request domain model represents:

```text
id
requestNumber
status
patientId?
scheduledAt
reasonForVisit
expiresAt?
cancelledAt?
createdAt
appointmentId?
```

Only the confirmed appointment ID is required from the nested accepted
appointment for navigation. Unknown extra appointment fields remain ignored at
the transport layer.

### Active-link confirmed-appointment endpoints

```text
GET  /api/v1/appointment-availability?date={Y-m-d}&appointment_id={id}
GET  /api/v1/appointments?page={page}&per_page={perPage}
GET  /api/v1/appointments/{appointment}
POST /api/v1/appointments/{appointment}/cancel
POST /api/v1/appointments/{appointment}/reschedule
```

Confirmed appointment responses additionally map nullable `reason_for_visit`.
The reschedule body remains:

```json
{
  "scheduled_at": "2026-08-10T09:00:00+08:00"
}
```

### Removed endpoints

Android must contain no Retrofit annotation or runtime consumer for:

```text
GET  /api/v1/appointment-types
POST /api/v1/appointments
GET  /api/v1/appointments/{appointment}/intake
PUT  /api/v1/appointments/{appointment}/intake
POST /api/v1/appointments/{appointment}/intake/submit
```

## Architecture and Project Structure

The existing MVVM + Clean layers remain:

```text
app/src/main/java/com/eyecare/app/
├── data/remote/api/          Retrofit service contracts
├── data/remote/dto/          Kotlinx Serialization wire models
├── data/repository/          DTO-to-domain mapping and API error conversion
├── domain/model/             Serialization-free request/appointment models
├── domain/repository/        Feature-facing repository contracts
└── presentation/
    ├── appointments/         Combined list, confirmed detail, rescheduling
    ├── appointments/requests Request flow and request detail
    ├── reservations/         Confirmed-appointment prerequisite handoff
    └── navigation/           Account-only versus active-link destinations

app/src/test/java/com/eyecare/app/
├── data/remote/              DTO, route, and error-envelope contract tests
├── data/repository/          MockWebServer repository tests
├── domain/model/             Status and access policy tests
└── presentation/             ViewModel and pure UI-policy tests

docs/specs/                   Specification, plan, and task breakdown
CONTEXT.md                    Updated after implementation to remove stale routes
```

Exact file creation, reuse, moves, and deletions are Phase 2 planning decisions.
No production package is renamed solely for aesthetics.

## Tech Stack

- Android Gradle Plugin 9.2.1, compile/target SDK 35, minimum SDK 26
- Kotlin 2.3.0 with AGP built-in Kotlin and Java/JVM 11
- Jetpack Compose using BOM 2026.05.01 and Material 3
- Navigation Compose 2.9.0 with type-safe `@Serializable` routes
- Hilt 2.59.2 and KSP 2.3.9
- Retrofit 2.11.0, OkHttp 4.12.0, and Kotlinx Serialization 1.8.1
- StateFlow-based MVVM with clean data/domain/presentation boundaries
- JUnit Jupiter 5.11.4, MockK 1.14.2, Turbine 1.2.0,
  kotlinx-coroutines-test 1.10.2, and MockWebServer 4.12.0

No dependency is added by this feature.

## Commands

Run from the repository root with Android Studio's JBR available as `JAVA_HOME`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

```powershell
# Build — mandatory after every production task
.\gradlew assembleDebug

# Complete JVM unit suite
.\gradlew testDebugUnitTest

# Focused appointment/request tests during development
.\gradlew testDebugUnitTest --tests "*Appointment*" --tests "*ApiRouteAllowlistTest" --tests "*ApiErrorDecoderTest"

# Android lint
.\gradlew lintDebug

# Format Kotlin sources
.\gradlew ktlintFormat
```

Formatting may be run after edits, but it cannot be used to rewrite or discard
unrelated user-owned changes.

## Code Style

Use Kotlinx Serialization DTOs at the transport boundary and map immediately to
serialization-free domain models in the repository:

```kotlin
@Serializable
data class AppointmentRequestDto(
    val id: Int,
    @SerialName("request_number") val requestNumber: String,
    val status: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("reason_for_visit") val reasonForVisit: String,
    val appointment: AppointmentReferenceDto? = null,
)

private fun AppointmentRequestDto.toDomain() = AppointmentRequest(
    id = id,
    requestNumber = requestNumber,
    status = AppointmentRequestStatus.fromRaw(status),
    scheduledAt = scheduledAt,
    reasonForVisit = reasonForVisit,
    appointmentId = appointment?.id,
)
```

Conventions:

- four-space indentation and trailing commas;
- `PascalCase` types, `camelCase` members, uppercase constants;
- `sealed interface` UI states exposed as read-only `StateFlow`;
- immutable data copied through ViewModels;
- explicit `@SerialName` for snake_case JSON;
- one repository boundary owns DTO-to-domain and HTTP-to-domain mapping;
- unknown backend status values map to `UNKNOWN` and fail closed;
- no Gson, LiveData, Android hardware identifiers, or transport DTOs in UI;
- patient-facing copy says request, confirmed visit, and reason for visit rather
  than exposing internal workflow terms.

## Error and Concurrency Semantics

| Condition | Required Android behavior |
|---|---|
| `SLOT_UNAVAILABLE` during request | Preserve reason, refresh request availability, clear selected slot. |
| `ACTIVE_REQUEST_LIMIT_REACHED` | Preserve form, explain limit, offer return to Requests. |
| `REQUEST_NOT_CANCELLABLE` | Refresh request detail; do not continue showing a cancel action blindly. |
| `REQUEST_NOT_OWNED` or 404 | Show neutral not-found copy without existence disclosure. |
| `REQUEST_TERMINAL` | Refresh request and display returned terminal state when obtainable. |
| `ACTIVE_PATIENT_LINK_REQUIRED` | Refresh `/me`; route protected destination to limited access if link inactive. |
| 401 with bearer token | Existing global logout/session-expired path. |
| 429 | Patient-safe retry-later copy; no automatic request loop. |
| malformed/unknown error | Generic retryable error; never surface raw JSON. |

Latest-response-wins rules apply to date changes and availability refreshes.
An older response cannot overwrite the state for a newer selected date.
Submission and cancellation are single-flight.

## Testing Strategy

### Unit and contract tests

1. DTO tests decode every request status, nullable fields, pagination, accepted
   appointment reference, request availability, and confirmed
   `reason_for_visit`.
2. Encoding tests prove create-request payload contains only `scheduled_at` and
   `reason_for_visit`.
3. Retrofit/route tests prove all five request routes and the corrected
   reschedule query shape, and prove removed routes are absent from production
   services.
4. Repository MockWebServer tests verify DTO-to-domain mapping, pagination,
   create/cancel bodies, and machine-readable error conversion.
5. ViewModel tests cover initial linked/unlinked view selection, loading,
   pagination, date races, stale slots, active-request limits, cancellation,
   accepted navigation, and frame-reservation-origin copy.
6. Confirmed reschedule tests prove only server slots can be selected and stale
   slots refresh.
7. Navigation/access tests prove request routes are account-only while
   confirmed/clinical routes require an active link.
8. Regression tests prove limited Home does not call protected APIs and 401
   handling remains bearer-aware.

### Static retirement checks

Tests or source scans fail when production code contains:

- `@GET("appointment-types")`;
- `@POST("appointments")` for creation;
- an intake Retrofit annotation;
- a `PatientIntake` navigation destination; or
- `appointment_type_id` in the request-creation body.

The route governance test must no longer classify an actively discovered
removed route as acceptable merely because it is listed as rejected.

### Build and manual verification

Every production task ends with `assembleDebug`. Checkpoints run the complete
unit suite. Final verification also runs lint and these manual matrices:

- linked versus unlinked versus pending-review Appointments destination;
- all request statuses;
- request creation success plus each machine-readable error;
- frame-reservation-origin request success;
- confirmed rescheduling loading/empty/stale/success states;
- no intake action for every confirmed appointment status; and
- link revoked while a protected feature is open.

No arbitrary coverage percentage is introduced. All changed domain,
repository, routing, and ViewModel branches require focused assertions.

## Boundaries

### Always do

- Preserve unrelated dirty-worktree changes.
- Start behavior changes with focused failing tests where practical.
- Map DTOs to domain models at the repository boundary.
- Use Kotlinx Serialization and the shared API error decoder.
- Treat request availability and status as server-authoritative.
- Keep unlinked request access separate from active-link clinical access.
- Run `assembleDebug` after every production task.
- Update `CONTEXT.md` and route governance after implementation.
- Keep specification, plan, tasks, and implementation synchronized.

### Ask first

- Any backend contract or route change.
- Any dependency, Gradle plugin, SDK, Room schema, or CI change.
- Persisting a request draft or frame-selection handoff across process death.
- Combining request and confirmed records into one backend-derived aggregate.
- Expanding work into unrelated resource redesign or visual rebranding.
- Keeping any retired intake/direct-booking compatibility code.

### Never do

- Call a retired appointment-type, direct-create, or intake endpoint.
- Allow appointment requests to expose clinical data to an unlinked account.
- Present a pending request as a confirmed appointment.
- Let patients select internal appointment type or provider identifiers.
- Generate selectable reschedule capacity solely on the device.
- Retry a non-idempotent request automatically after an ambiguous failure.
- Store request reasons, appointment data, credentials, or health data in Room.
- Log tokens, identifiers, reasons for visit, raw API bodies, or clinical data.
- Use Gson, LiveData, or `org.jetbrains.kotlin.android`.
- Delete failing tests to make the migration green.

## Success Criteria

- [ ] All five appointment-request endpoints have tested Android consumers.
- [ ] Linked and unlinked authenticated accounts can create and manage their
      own appointment requests.
- [ ] The Appointments destination clearly separates Requests and Confirmed.
- [ ] Unlinked accounts make no confirmed-appointment or clinical-resource API
      calls.
- [ ] New request payloads contain only `scheduled_at` and
      `reason_for_visit`.
- [ ] Appointment type, referring source, and booking contact-note selection are
      absent from the request UX.
- [ ] Request success is described as awaiting clinic confirmation.
- [ ] Pending requests can be cancelled and terminal requests cannot expose a
      stale cancel action.
- [ ] Accepted requests can open the confirmed appointment when an ID is
      returned and the account is linked.
- [ ] Frame-reservation-origin requests explain that reservation must wait for
      confirmation and do not auto-submit a reservation.
- [ ] Confirmed appointments decode and display nullable `reason_for_visit`.
- [ ] Confirmed rescheduling uses only server-returned availability with
      `appointment_id`.
- [ ] `SLOT_UNAVAILABLE` clears stale selection and refreshes availability.
- [ ] Patient intake code and navigation are absent from production sources.
- [ ] `GET /appointment-types` and direct `POST /appointments` creation are
      absent from production sources.
- [ ] Non-auth endpoints touched by the cutover map the standard error envelope
      into domain errors.
- [ ] `ACTIVE_PATIENT_LINK_REQUIRED` refreshes link state without logging out.
- [ ] Route governance matches the 55 actual endpoint sections and fails on any
      discovered removed route.
- [ ] Existing frames, reservations, prescriptions, Eyewear, billing,
      messaging, and rating tests remain green.
- [ ] Focused tests, the complete unit suite, `assembleDebug`, and `lintDebug`
      pass.
- [ ] `CONTEXT.md` no longer documents direct booking or patient intake as
      current behavior.

## Open Questions

None. The product and migration decisions required for Phase 2 are confirmed.
If implementation evidence contradicts the documented appointment reference or
error envelope, this specification must be amended and re-approved before code
continues.
