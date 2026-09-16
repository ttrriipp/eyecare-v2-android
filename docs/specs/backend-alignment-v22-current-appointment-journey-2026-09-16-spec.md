# Spec: Backend Alignment v22 — Unified Current Appointment Journey

Status: Approved — 2026-09-16

## Objective

Replace the Android app's merged active appointment/request lists with one
patient-facing **My Appointment** screen backed by
`GET /api/v1/appointment-requests/current`.

The current endpoint is the sole source of truth for the account's actionable
booking journey. The screen renders one of three mutually exclusive states:

1. no active booking;
2. one pending new appointment request; or
3. one future confirmed appointment, optionally accompanied by its original
   accepted request and one pending reschedule request.

The primary user is an authenticated patient account. A patient should
understand the current booking state and available next action without opening
a list card. Completed and otherwise historical confirmed appointments move to
a separate History destination backed by
`GET /api/v1/appointments?filter=history`.

### User outcomes

1. A patient with no active journey immediately sees that state and can start
   an appointment request.
2. A patient with a pending new request immediately sees its status, requested
   time, alternatives, and available request actions.
3. A patient with a confirmed appointment immediately sees the confirmed
   details and management actions.
4. A pending reschedule never replaces or visually overrides the confirmed
   appointment. It appears beneath the appointment and is explicitly described
   as not yet confirmed.
5. Appointment history remains available as a separate, server-filtered list.

## Sources of Truth and Supersession

1. `docs/API_CONTRACT.md`, especially:
   - `GET /appointment-requests/current`;
   - appointment-request detail, update, cancellation, and creation;
   - `GET /appointments?filter=history`;
   - confirmed appointment cancellation and rating; and
   - account-only versus active-link route requirements.
2. `docs/BACKEND_CONTEXT.md`, especially the mobile route tiers,
   `ResolveCurrentBooking`, and appointment lifecycle rules.
3. The current Android implementation for appointment requests, confirmed
   appointments, navigation, session/link state, Home, and shared presentation
   components.
4. `PRODUCT.md` and `DESIGN.md` for Android product and visual constraints.

This specification supersedes the active UI architecture in
`docs/specs/backend-alignment-v13-appointment-cutover-spec.md` that requires
top-level Requests and Confirmed views and independent active-list pagination.
The V13 document remains historical and is not rewritten. Its request creation,
detail, cancellation, link-boundary, and server-authoritative availability
decisions remain valid unless this specification explicitly changes them.

## Assumptions and Scope Decisions

1. The bottom-navigation destination remains named **Appointments** to avoid a
   crowded navigation label; its screen title becomes **My Appointment**.
2. History is a distinct secondary destination opened from the My Appointment
   top app bar. It is not an Upcoming/History segment on the current screen.
3. The active screen never reconstructs current state by merging
   `GET /appointment-requests` and `GET /appointments`.
4. The existing appointment-request creation wizard remains the creation flow.
   Successful creation returns to My Appointment, where the new pending request
   is loaded from the current endpoint.
5. The existing request-detail destination remains available for the original
   request and pending request/reschedule drill-down, but all important current
   information is also visible inline on My Appointment.
6. The existing confirmed appointment detail destination remains available for
   history, notifications, and other contextual deep links. The active
   Appointments root no longer requires a card tap into that destination.
7. Request history is not added to the new Appointment History screen. The
   screen lists only the records returned by
   `GET /appointments?filter=history`. Existing request detail remains
   addressable when the app already has a request ID.
8. Home changes its active confirmed-appointment lookup to the current endpoint
   so it does not use an unfiltered appointment list as a second active source
   of truth. Adding a new pending-request Home card is outside this phase.
9. No backend, database, dependency, Room, worker, or notification-contract
   change is required.
10. Existing appointment/request actions and backend validation remain
    authoritative; this work changes their presentation and refresh ownership.

## Authoritative Contract Mapping

### Current journey

`GET /api/v1/appointment-requests/current` is account-only and may be called
before a Patient link is active.

| `data.kind` | Required payload | Android domain state |
|---|---|---|
| `none` | no additional object | `CurrentAppointmentJourney.None` |
| `pending_request` | `request` | `CurrentAppointmentJourney.PendingRequest` |
| `appointment` | `appointment`; nullable `original_request`; nullable `pending_reschedule` | `CurrentAppointmentJourney.Appointment` |

The DTO layer may represent the wire response with nullable variant fields, but
the repository boundary must validate the discriminator and produce a
serialization-free sealed domain model. Missing required variant data, an
unknown `kind`, or an internally inconsistent response fails closed as a load
error. It must never be interpreted as `none`.

The embedded request and appointment objects reuse the existing request and
confirmed-appointment field mappings. DTOs must not be exposed to presentation
code.

### History

Appointment history uses:

```text
GET /api/v1/appointments?filter=history&page={page}&per_page={perPage}
```

The filter is always sent by the history repository operation. Android does not
fetch an unfiltered list and partition it locally. Pagination, rating fields,
and historical appointment detail behavior remain supported.

### Access boundaries

| Capability | Access requirement |
|---|---|
| Read current journey | authenticated account |
| Create/read/update/cancel owned appointment request | authenticated account |
| Read appointment history | active Patient link |
| Read/cancel confirmed appointment | active Patient link |
| Load rebooking availability or submit linked rebooking | active Patient link |
| Read original owned request | authenticated account |

The current journey's content is rendered from the endpoint regardless of link
state. Link state controls only protected actions and destinations. If an
unlinked session receives `kind: "appointment"`, Android shows the appointment
but replaces protected controls with concise link-required guidance; it does
not hide or reinterpret the journey.

## UX and State Behavior

### Screen shell

My Appointment uses one vertically scrollable detail surface:

- a top app bar with **My Appointment** and a History action when history is
  available;
- a prominent, text-labeled status header;
- immediately visible date/time and visit or request identity;
- organized detail sections rather than a collection of tappable list cards;
- an action group appropriate to the current state; and
- pull-to-refresh plus refresh-on-resume behavior.

The screen preserves Android system Back, edge-to-edge insets, minimum 48dp
touch targets, increased-font scrolling, dark theme, and TalkBack reading order.

### Loading, refresh, and error states

- First load uses a stable content-shaped skeleton rather than rendering stale
  list controls.
- Pull-to-refresh and resume refresh keep already displayed content visible.
- A background refresh failure shows an inline retry message and retains the
  last successful current journey.
- A first-load failure shows a patient-safe error and Retry action.
- Unknown or malformed current-journey data exposes no mutation action.
- Mutations are single-flight and preserve any patient-entered cancellation or
  reschedule draft on recoverable failure.

### `none`

The empty state shows:

- **No active appointment**;
- concise guidance that the patient can request a visit;
- one primary **Request an appointment** action; and
- History as a secondary top-app-bar action only when the account has an active
  Patient link.

The empty state does not show request limits, empty list sections, date filters,
or inactive cards.

### `pending_request`

The pending request is shown as the page's main content. It includes:

- patient-facing pending status and request number;
- appointment type and duration when available;
- primary requested date/time;
- every `alternative_scheduled_times` value, labeled as alternatives;
- reason for visit and referral context when present;
- expiry or request timing guidance when useful; and
- explicit copy that submitted times are preferences awaiting clinic approval.

Available actions are:

- **Change requested times**, using the existing request schedule-update flow;
- **Cancel request**, using the required reason flow and same-day policy; and
- **View request details** only for secondary metadata not appropriate inline.

The screen does not offer another new request while this state is active.

### `appointment`

The confirmed appointment is always the primary object. The screen shows:

- confirmed status (`scheduled` or `checked_in`) in text and semantic color;
- appointment type, date, time, and duration;
- appointment number when available;
- reason for visit, assigned optometrist, referral source, and patient-visible
  contact notes when present; and
- cancellation or staff-reschedule guidance already supported by the existing
  appointment presentation when applicable.

For a linked scheduled appointment with no pending reschedule, the primary
management action is **Request a different time**. Eligible cancellation remains
available as a lower-emphasis destructive action. Checked-in appointments do
not offer rebooking. Same-day restrictions provide clinic-contact guidance and
remain server-authoritative.

The screen does not show the active appointment as a card that must be tapped to
reveal these details.

### Pending reschedule under an appointment

When `pending_reschedule` is non-null:

- the confirmed appointment and its current confirmed time remain at the top;
- the pending request appears below in a distinct tonal section;
- the section title is **Time-change request pending**;
- visible copy says **Not yet confirmed**;
- the requested time and all alternatives are shown;
- request status and request number are shown; and
- the action to submit another rebooking is removed.

The patient may open the request detail to change or cancel the pending request
when still eligible. Cancelling or changing the request does not change the
confirmed appointment.

### Original request

When `original_request` is non-null, the appointment screen provides a
low-emphasis **View original request** action. The original request is supporting
context, not a second current item. When it is null, no placeholder or disabled
action is shown.

### Appointment history

History is a separate paginated list with no Upcoming segment and no active
request cards. It supports:

- terminal and past appointments returned by `filter=history`;
- pull-to-refresh, pagination, empty, initial error, and append-retry states;
- existing historical detail navigation; and
- existing fulfilled-visit rating and rating-update behavior.

History is unavailable without an active Patient link. The My Appointment
screen remains usable because its current endpoint is account-only.

### Mutation and lifecycle reconciliation

After any successful current-journey mutation, Android refetches
`GET /appointment-requests/current` rather than manually reconciling separate
active lists:

| Event | Expected refreshed state |
|---|---|
| New request submitted | `pending_request` |
| Pending new request cancelled | `none` |
| Pending request schedule changed | same kind with updated times |
| Staff accepts new request | `appointment` |
| Rebooking submitted | `appointment` with `pending_reschedule` |
| Pending rebooking changed | `appointment` with updated `pending_reschedule` |
| Pending rebooking cancelled/rejected/expired | `appointment` without `pending_reschedule` |
| Active appointment cancelled, fulfilled, or no-show | `none`; record is available through History when applicable |

Returning from request detail, appointment action sheets, request creation, or a
protected deep link refreshes the current root. Refresh generation or job
cancellation prevents an older response from overwriting newer state.

### Home integration

Home uses the current-journey repository operation for its existing next-visit
ticket:

- `appointment` supplies the confirmed appointment;
- `none` and `pending_request` produce no confirmed next-visit ticket in this
  phase; and
- tapping the ticket opens My Appointment rather than requiring an active-list
  lookup.

Home does not call unfiltered `GET /appointments` to infer the active visit.

## Domain and Interface Design

The domain contract should follow this shape without transport annotations:

```kotlin
sealed interface CurrentAppointmentJourney {
    data object None : CurrentAppointmentJourney

    data class PendingRequest(
        val request: AppointmentRequest,
    ) : CurrentAppointmentJourney

    data class Appointment(
        val appointment: AppointmentV1,
        val originalRequest: AppointmentRequest?,
        val pendingReschedule: AppointmentRequest?,
    ) : CurrentAppointmentJourney
}
```

Repository interfaces expose intent, not Retrofit response details:

```kotlin
suspend fun getCurrentAppointmentJourney(): Result<CurrentAppointmentJourney>
suspend fun getAppointmentHistory(
    page: Int = 1,
    perPage: Int = 15,
): Result<PaginatedResult<AppointmentV1>>
```

`getAppointmentHistory` always applies the history filter. Existing generic or
legacy list methods may remain only while a verified caller still needs them;
the My Appointment and Home paths must not use them.

## Navigation and Component Ownership

Proposed destination ownership:

```text
Appointments (bottom-navigation root, account-only)
└── MyAppointmentScreen
    ├── RequestAppointment
    ├── AppointmentRequestDetail(requestId)
    └── AppointmentHistory (active-link required)
        └── AppointmentDetail(appointmentId)
```

The implementation should reuse focused content from the current appointment
and request detail screens instead of nesting whole screens or duplicating
action logic. Likely reusable pieces include:

- status header;
- appointment summary and detail sections;
- requested-time and alternative-time sections;
- pending-reschedule section;
- cancellation dialog/sheet;
- reschedule/request-schedule sheet; and
- rating dialog for history.

One ViewModel owns current-journey loading and refresh. History owns independent
pagination state. Dialog and draft state remain local to the relevant current
screen/ViewModel and are not stored in Room.

## Visual and Accessibility Requirements

- Follow `DESIGN.md` and Material 3 components.
- Use Deep Vision Navy for the confirmed-appointment summary anchor.
- Use the existing Amber Pending treatment for pending requests and time-change
  requests, always paired with status text.
- Use Warm Clinic Canvas, restrained white/tonal sections, hairline borders,
  and the existing spacing/type scales.
- Avoid a stack of equally weighted cards; hierarchy must make the confirmed
  appointment or pending new request unmistakably primary.
- Do not use Lens Cyan as text or icon color on a light surface; use the
  established accessible accent token.
- Maintain 48dp touch targets, meaningful content descriptions, logical
  TalkBack order, and layouts that scroll at large font sizes.
- Motion communicates only state changes and respects reduced-animation system
  settings.

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- StateFlow and lifecycle-aware state collection
- Retrofit
- Kotlinx Serialization
- Hilt
- Coroutines
- JUnit, MockK, Turbine, MockWebServer, and Compose UI testing

No new dependency is expected.

## Commands

Run from the repository root in PowerShell with Android Studio's JBR available:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

```powershell
# Focused unit tests during implementation
.\gradlew testDebugUnitTest --tests "*CurrentAppointment*" --tests "*AppointmentHistory*" --tests "*AppointmentRequest*" --tests "*AppointmentV1*"

# Compile instrumented Compose tests
.\gradlew compileDebugAndroidTestKotlin

# Required project gates
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug

# Formatting when needed
.\gradlew ktlintFormat
```

## Project Structure

Expected areas of change:

```text
app/src/main/java/com/eyecare/app/
  data/remote/api/                 current endpoint and history filter
  data/remote/dto/                 discriminated current-journey DTOs
  data/repository/                 DTO-to-domain validation and mapping
  domain/model/                    sealed current-journey model
  domain/repository/               current and history intent operations
  presentation/appointments/       My Appointment and History state/UI
  presentation/appointments/requests/ reusable request actions/details
  presentation/home/               current-journey next-visit source
  presentation/navigation/         History route and access policy

app/src/test/                       domain, DTO, repository, ViewModel tests
app/src/androidTest/                Compose state and accessibility tests
docs/specs/                         this spec and later approved plan/tasks
```

## Code Style

- Keep DTOs, domain models, and presentation state separate.
- Map DTOs to domain models only at repository boundaries.
- Model mutually exclusive server variants with a sealed interface.
- Use exhaustive `when` expressions for current-journey rendering.
- Keep composables focused and extract sections before a screen becomes a
  monolithic renderer.
- Use existing semantic theme tokens and typography roles rather than raw
  colors or ad hoc text sizes.
- Preserve coroutine cancellation when converting failures.
- Use patient-safe error copy and never render raw response bodies.

## Testing Strategy

### Contract and repository tests

- Decode all three current kinds.
- Decode appointment kind with and without original request and pending
  reschedule.
- Verify primary and alternative times survive DTO-to-domain mapping.
- Reject unknown kinds and missing required variant objects without mapping to
  `None`.
- Verify appointment history sends `filter=history`, page, and per-page values.
- Verify no current-journey operation calls either paginated active list.

### ViewModel tests

- Cover first load, retry, retained-content refresh failure, and stale-response
  suppression.
- Cover the three current states and nullable supporting objects.
- Cover mutation success followed by authoritative current refetch.
- Cover link-state action policy without using link state to infer journey kind.
- Cover independent history pagination, refresh, append retry, and rating
  updates.
- Cover Home's current-journey source selection.

### Compose UI tests

- `none` exposes the request CTA without empty list controls.
- `pending_request` shows primary and alternative times without requiring a
  card tap.
- `appointment` shows confirmed details immediately.
- A pending reschedule appears below the confirmed appointment and contains the
  phrases **Time-change request pending** and **Not yet confirmed**.
- The pending reschedule never replaces the confirmed date/time semantics.
- Original-request action is present only when data exists.
- Protected actions and History follow active-link policy.
- Status is understandable without color, touch targets are reachable, and
  large text does not clip critical actions.

### Regression gates

- Appointment request creation, update, cancellation, and rebooking remain
  functional.
- Historical appointment detail and visit rating remain functional.
- Limited accounts can load My Appointment without calling active-link-only
  endpoints.
- Route allowlist includes the current endpoint and continues rejecting retired
  direct-reschedule routes.
- Full unit tests, lint, and `assembleDebug` complete, with any unrelated
  pre-existing failures reported rather than silently removed.

## Boundaries

### Always

- Treat `/appointment-requests/current` as authoritative for active state.
- Treat `filter=history` as mandatory for the History list.
- Preserve account-only request access and active-link confirmed-action rules.
- Keep DTO-to-domain mapping at the repository boundary.
- Refresh current state after every successful current-journey mutation.
- Add focused tests before or with each behavior change.
- Run `assembleDebug` after production changes.

### Ask first

- Changing the backend response contract or route tiers.
- Adding request-history UI to Appointment History.
- Changing the bottom-navigation label or navigation-shell geometry.
- Adding a new dependency, persistence layer, cache, worker, or polling system.
- Removing a deep-link destination used outside the Appointments root.

### Never

- Infer current journey by merging paginated list responses.
- Show a pending reschedule as the confirmed appointment time.
- Call protected appointment/history endpoints for an inactive Patient link.
- Store appointment, request, health, or token data in Room.
- Use Gson or apply `org.jetbrains.kotlin.android`.
- Log appointment reasons, cancellation text, clinical data, tokens, or raw
  response bodies.
- Manufacture IDs, appointment states, or booking eligibility locally.

## Success Criteria

1. The Appointments root makes `GET /appointment-requests/current` its only
   active-journey read.
2. `none`, `pending_request`, and `appointment` each render a complete,
   test-covered full-detail state.
3. Important current information is visible without tapping a list card.
4. A confirmed appointment remains primary while a pending reschedule is shown
   below it as not confirmed, including every alternative time.
5. The original request is accessible when present and omitted cleanly when
   absent.
6. Appointment History always calls `GET /appointments?filter=history` and has
   no Upcoming mode or active request cards.
7. Unlinked accounts can load and manage eligible request states without a
   protected appointment-list call.
8. Protected confirmed actions and History remain active-link gated.
9. Home no longer calls an unfiltered appointment list to infer the next active
   visit.
10. Successful create, update, cancel, rebooking, and return-navigation paths
    reconcile through a current-journey refetch.
11. Existing history detail, rating, cancellation, request update, and
    rebooking behavior continues to pass focused regression tests.
12. Focused unit/Compose tests, full unit tests, lint, and `assembleDebug` are
    run and their outcomes are reported accurately.

## Approved Product Decisions

The approved implementation defaults are:

1. Keep the bottom-navigation label **Appointments** while using
   **My Appointment** as the screen title.
2. Open History as a separate destination from a top-app-bar action.
3. Keep request history out of Appointment History; expose only the original or
   pending request reachable from the current journey.
4. Change Home's data source but do not add a new pending-request Home card in
   this phase.
