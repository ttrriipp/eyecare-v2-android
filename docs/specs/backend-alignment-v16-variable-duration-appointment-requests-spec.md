# Spec: Backend Alignment v16 — Variable-Duration Appointment Requests

Status: Complete — 2026-08-10
Date: 2026-08-10

## Objective

Align the Android appointment-request flow with the backend's 2026-08-09
variable-duration scheduling contract. An authenticated linked or unlinked
patient account must select a patient-visible appointment type before requesting
time preferences. Availability, duration, referral requirements, and final
capacity remain server-authoritative.

Success means Android can consume the restored appointment-type catalog, request
type-specific availability, submit the required `appointment_type_id`, support
up to two optional alternative times, collect a referral source only when the
selected type requires it, and present pending requests without claiming that a
time is reserved. Existing confirmed-appointment, rescheduling, identity,
account-link, and clinical-data boundaries remain intact.

The optical-commerce and dispensing additions dated 2026-08-10 are out of
Android scope because the authoritative contract explicitly keeps their internal
specifications, measurements, lots, supplier references, approval metadata, and
balance-override data out of patient resources and introduces no patient API
shape change for those features.

## Sources of Truth and Precedence

1. `docs/API_CONTRACT.md`, working-tree version dated 2026-08-10.
2. `docs/BACKEND_CONTEXT.md`, working-tree version dated 2026-08-10.
3. `CONTEXT.md` for the current Android architecture and implemented behavior.
4. Current Android production code and tests.
5. This specification, once reviewed and approved, for Android product and
   migration decisions.

The two backend documents are user-owned, uncommitted inputs. The human owner
resolved their stale contradictions on 2026-08-10; the decisions below take
precedence until the source documents are reconciled.

## Resolved Decisions

1. List, detail, create, and cancel use the same expanded appointment-request
   resource. List differs only by pagination. New fields are nullable on Android
   for legacy records.
2. Patients select a patient-visible appointment type. They do not select a
   preferred optometrist.
3. Pending requests never reserve or consume capacity in request, confirmed
   appointment, or reschedule availability. Only actual appointments consume
   capacity.
4. The backend registers 55 callable routes: 8 public, 26 account-only, and 21
   active-link. Of these, 54 are canonical and one is a callable legacy alias.
   Android must use only canonical routes.
5. `expires_at` remains in the data model but is omitted from Android UI. It is
   the latest submitted preference time, not a hold expiry or review deadline.
6. `referring_source` is trimmed, required and nonblank for referral types, and
   limited to 255 characters. It is optional otherwise.

## Contract Delta Since the Completed v15 Alignment

| Area | Previous Android baseline | New backend contract | Android impact |
|---|---|---|---|
| Route governance | 53 contract routes: 8 public, 24 account-only, 20 canonical active-link, 1 legacy alias | 55 callable routes: 8 public, 26 account-only, 20 canonical active-link, 1 callable legacy alias | Approve all 55 registered routes while allowing production Android consumers to use only the 54 canonical routes |
| Appointment types | `GET /appointment-types` retired and explicitly rejected | Restored as an account-only patient-visible catalog | Add a real consumer and remove the route from the rejected set |
| Optometrist catalog | No patient-safe catalog | `GET /appointment-optometrists` added | Classify as approved account-only; do not expose provider selection because create-request accepts no provider and backend policy keeps assignment clinic-controlled |
| Request availability query | `date` only | `date` plus required `appointment_type_id` | Type selection must precede availability loading |
| Request availability response | 30-minute provisional slot | 15-minute cadence and type duration; adds `visit_duration_minutes` and `appointment_type_id` | Decode and preserve both cadence and selected-type duration; render only returned slots |
| Create request body | `scheduled_at`, `reason_for_visit`, optional unlinked identity | Adds required `appointment_type_id`, optional max-two `alternative_scheduled_times`, and conditional `referring_source` | Expand DTO, repository contract, ViewModel state, validation, review, and tests |
| Request resource | Primary time and reason | Adds appointment-type summary, alternatives, provisional duration, referring source, and `time_preferences_are_reserved = false` | Expand nullable response/domain fields and request detail/list presentation |
| Pending semantics | Time described as held until expiry | Pending requests never reserve capacity; `expires_at` is the latest submitted preference | Remove all “held/released/hold expired” copy and stop labeling `expires_at` as a reservation expiry |
| Capacity | Pending requests consumed capacity | Pending requests are non-binding | No client-side capacity inference; submission remains subject to authoritative revalidation |
| Frame aggregates | Nullable average already implemented in v15 | Contract adds clarification only | No production change; retain nullable end-to-end behavior and existing tests |

## Current Android Gaps

The current app cannot submit a valid request against the new contract:

- `AppointmentRequestApiService.getAvailability()` sends only `date`.
- `CreateAppointmentRequest` omits required `appointment_type_id`.
- No production Retrofit service consumes `GET /appointment-types`.
- Route governance still rejects `GET /appointment-types`, counts 24
  account-only routes, and reports the old 53-route contract.
- The request DTO/domain model does not carry appointment type, alternative
  times, provisional duration, referral source, or reservation semantics.
- The three-step wizard starts with date/time and has no type or referral input.
- Request detail and cancellation copy claim the selected time is held and will
  be released.
- `RequestAppointmentScreen` fabricates placeholder 30-minute slots whenever
  the server returns no available slots. Those placeholders are selectable in a
  production path and must be removed; an empty server response must remain an
  empty state.
- `CONTEXT.md` still says appointment types are retired and records 53 routes.
- `AppointmentV1Dtos` contains unused appointment-type and create-request wire
  types separate from the active appointment-request data path; planning must
  choose one canonical owner and remove or reconcile dead duplicates.

## Product Behavior

### Request flow

The recommended flow is:

```text
Choose appointment type
    ↓
Choose primary date and server-returned time
    ↓
Optionally add up to two distinct alternative times
    ↓
Enter reason, conditional referral source, and unlinked identity when required
    ↓
Review all preferences
    ↓
Submit one appointment request for clinic review
```

The wizard may implement this as four visible steps — Type, Schedule, Details,
Review — while keeping alternative preferences inside Schedule. Exact Compose
component boundaries are a Phase 2 planning decision.

### Appointment-type selection

- Load `GET /appointment-types` when entering the flow.
- Show the patient label, optional description, duration, and a clear referral
  indicator when applicable.
- Do not hardcode the six seeded types or IDs; the catalog is configurable.
- Do not select an optometrist. `GET /appointment-optometrists` has no matching
  create-request field, and `BACKEND_CONTEXT.md` says provider assignment is
  clinic-controlled.
- Type-load failures show retry and cannot fall through to a fabricated default.
- Changing type clears all selected time preferences and reloads availability
  only after the patient selects a date.

### Availability and time preferences

- Call `GET /appointment-request-availability` with the selected date and
  appointment type ID.
- Preserve `interval_minutes`, `visit_duration_minutes`,
  `slot_duration_minutes`, and response `appointment_type_id` at the data/domain
  boundary. A response for a different type cannot overwrite current state.
- Select only `available = true` slots returned by the latest response.
- Never generate, infer, or substitute selectable slots on-device.
- Locally block past dates only. Clinic closures, hours, overrides, cadence, and
  duration fit are server-owned; Android must not treat Sunday as permanently
  closed when the schedule is configurable.
- A primary time is required. Alternatives are optional, ordered, distinct, and
  capped at two. The same timestamp cannot appear twice.
- Each alternative must come from availability fetched for the same selected
  type, though it may use another date.
- Show a real loading, retry, closed-day, and no-times state for each active
  availability request.

### Details and validation

- `reason_for_visit` remains required, trimmed, and capped at 1000 characters.
- If `requires_referral` is true, `referring_source` is trimmed and must contain
  1–255 characters before Review. If false, Android sends `null` and does not
  retain a stale value from a previously selected referral type.
- Linked accounts omit `identity`; unlinked/pending-review accounts retain the
  existing identity collection and verified-phone behavior.
- Android does not send patient ID, provider ID, duration, capacity flags, or
  verification metadata.

### Review, success, list, and detail

- Review shows appointment type, duration, primary preference, alternatives in
  submitted order, reason, referral source when applicable, and requester
  identity when supplied.
- Success describes a request awaiting clinic review, never a confirmed booking
  or reserved time.
- List/detail decode the expanded resource defensively because legacy rows may
  lack the new type and duration fields.
- Detail shows all returned time preferences and the selected appointment type
  when present.
- `time_preferences_are_reserved = false` is represented in the transport and
  domain model. UI copy must not contradict it.
- Pending cancellation copy says the request will be cancelled; it must not say
  a time will be released.
- `expires_at` remains mapped but is omitted from request UI. Android shows the
  ordered requested times instead and never labels the field “Expires.”
- Accepted requests continue to cross-link to the confirmed appointment only
  when `appointment.id` exists and the account has an active patient link.

### Error and concurrency behavior

| Condition | Required Android behavior |
|---|---|
| Type catalog fails | Keep the flow blocked at Type with retry; no defaults |
| Availability fails | Preserve type/date and show retry; no placeholder slots |
| `SLOT_UNAVAILABLE` on submit | Preserve type/details, clear affected selections, and refresh authoritative availability |
| Type becomes inactive or hidden | Return to Type, refresh catalog, and explain that the selection is no longer available |
| Referral validation fails | Return to Details with field-level feedback |
| `ACTIVE_REQUEST_LIMIT_REACHED` | Preserve the draft and explain the pending-request limit |
| 401 | Use the existing bearer-aware session-expired path |
| Unknown 4xx/5xx | Preserve safe draft state and show retryable patient-safe copy |

Type and date requests use latest-response-wins semantics. Submission remains
single-flight and is never automatically retried after an ambiguous network
failure.

## Backend Contract Consumed

```text
GET  /api/v1/appointment-types
GET  /api/v1/appointment-request-availability?date={Y-m-d}&appointment_type_id={id}
GET  /api/v1/appointment-requests?page={page}&per_page={perPage}
POST /api/v1/appointment-requests
GET  /api/v1/appointment-requests/{appointmentRequest}
POST /api/v1/appointment-requests/{appointmentRequest}/cancel
```

Create request body:

```json
{
  "appointment_type_id": 1,
  "scheduled_at": "2026-08-18T09:15:00+08:00",
  "alternative_scheduled_times": [
    "2026-08-18T10:30:00+08:00",
    "2026-08-19T09:00:00+08:00"
  ],
  "reason_for_visit": "Blurred vision in left eye",
  "referring_source": null,
  "identity": null
}
```

`GET /appointment-optometrists` is included in route governance but has no
runtime consumer in this scope. Adding a provider picker would require a new
backend request field and an explicit product decision.

Confirmed appointment routes and rescheduling request/response shapes remain
unchanged by this migration.

## Tech Stack

- Kotlin 2.3.0 with AGP 9.2.1 built-in Kotlin; Java/JVM 11
- Jetpack Compose and Material 3
- Hilt 2.59.2
- Retrofit 2.11, OkHttp 4.12, Kotlinx Serialization 1.8.1
- StateFlow MVVM with Clean data → domain → presentation boundaries
- JUnit 5, MockK, coroutines-test, Turbine, and MockWebServer
- Room remains frame-cache-only; this feature adds no local database storage

No dependency, Gradle plugin, SDK, Room schema, worker, or backend change is
required by the approved scope.

## Commands

Run from the repository root with Android Studio's JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Focused contract and request-flow tests
.\gradlew testDebugUnitTest --tests "*AppointmentRequest*" --tests "*ApiRouteAllowlistTest"

# Complete unit suite
.\gradlew testDebugUnitTest

# Mandatory build after every production task
.\gradlew assembleDebug

# Final static checks
.\gradlew lintDebug
.\gradlew ktlintCheck
```

## Project Structure

```text
app/src/main/java/com/eyecare/app/
├── data/remote/api/AppointmentRequestApiService.kt     Retrofit routes/queries
├── data/remote/dto/AppointmentRequestDtos.kt           Request wire models
├── data/repository/AppointmentRequestRepositoryImpl.kt Mapping and API calls
├── domain/model/AppointmentRequest.kt                  Domain models/policies
├── domain/repository/AppointmentRequestRepository.kt   Feature contract
└── presentation/appointments/requests/                 Wizard, list, detail

app/src/test/java/com/eyecare/app/
├── data/remote/                                        DTO/route contract tests
├── data/repository/                                    MockWebServer tests
└── presentation/appointments/requests/                 ViewModel/policy tests

docs/specs/                                             Spec, later plan/tasks
CONTEXT.md                                              Reconciled after implementation
```

Phase 2 will list exact files per task and keep each task near the five-file
limit. No production package will be renamed solely for aesthetics.

## Code Style

Transport fields stay in DTOs and map to serialization-free domain models at
the repository boundary:

```kotlin
@Serializable
data class AppointmentTypeDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("requires_referral") val requiresReferral: Boolean = false,
)

private fun AppointmentTypeDto.toDomain() = AppointmentType(
    id = id,
    name = name,
    description = description,
    durationMinutes = durationMinutes,
    requiresReferral = requiresReferral,
)
```

Conventions:

- four-space indentation and trailing commas;
- `PascalCase` types and `camelCase` members;
- explicit `@SerialName` for snake_case wire fields;
- `sealed interface` UI state exposed as read-only `StateFlow`;
- immutable state transitions with latest-response identity checks;
- unknown response fields ignored by the configured JSON parser;
- nullable/defaulted response additions for legacy resource compatibility;
- no transport DTO in domain or presentation code;
- no Gson, LiveData, or `org.jetbrains.kotlin.android` plugin.

## Testing Strategy

### DTO and route contract tests

- Decode appointment types with nullable descriptions and referral flags.
- Decode type-specific availability at 15-minute cadence with a longer visit
  duration and required response type ID.
- Encode required type ID, primary time, zero/one/two alternatives, conditional
  referral source, and optional identity with exact field names.
- Decode expanded request resources, including absent fields on legacy rows.
- Reconcile governance to 8 public, 26 account-only, 20 canonical active-link,
  and one callable legacy alias: 54 canonical routes and 55 callable registered
  routes. Production Android services must not call the alias.
- Prove `appointment-types` is approved and no longer rejected.

### Repository tests

- Assert availability sends both `date` and `appointment_type_id`.
- Assert type DTOs map at the repository boundary.
- Assert create-request bodies never include provider ID, duration, patient ID,
  or other server-owned fields.
- Assert expanded response fields map without collapsing absent legacy data.
- Preserve machine-readable API errors through `safeApiCall`.

### ViewModel and presentation tests

- Type loading, retry, selection, and type-change draft invalidation.
- Availability latest-response-wins across type/date changes.
- Empty or failed availability never creates selectable fallback slots.
- Primary/alternative uniqueness and max-two rules.
- Conditional referral validation and stale referral clearing.
- Linked versus unlinked identity behavior remains unchanged.
- Review and submission preserve ordered preferences.
- Slot-unavailable recovery preserves safe details and refreshes availability.
- Pending/list/detail/cancel copy contains no reservation or hold claim.

### Verification checkpoints

- Every production task runs its focused tests and `assembleDebug`.
- Every 2–3 tasks run the complete unit suite.
- Final verification runs unit tests, build, lint, formatting check, and a manual
  linked/unlinked matrix for normal, referral, empty-day, stale-slot, and
  cancellation flows.

No arbitrary coverage percentage is introduced. Every changed mapping,
validation branch, and state transition requires a focused assertion.

## Boundaries

### Always do

- Preserve user-owned changes in `docs/API_CONTRACT.md` and
  `docs/BACKEND_CONTEXT.md`.
- Treat appointment type, duration, cadence, availability, and final acceptance
  as server-authoritative.
- Map DTOs to domain models at the repository boundary.
- Use Kotlinx Serialization and the shared API error handling path.
- Keep request routes account-only and confirmed/clinical routes active-link-only.
- Start behavior changes with focused failing tests where practical.
- Run `assembleDebug` after every production change.
- Reconcile `CONTEXT.md` and route governance after implementation.
- Update this living spec before implementing any changed decision.

### Ask first

- Any backend contract, endpoint, or request-field change.
- Any patient-selectable optometrist behavior.
- Omitting alternative-time UX despite the backend feature supporting it.
- Persisting request drafts or health/contact data locally.
- Adding dependencies, changing Gradle/SDK/Room/CI, or restructuring navigation.
- Expanding work into confirmed-appointment redesign or optical-commerce UI.

### Never do

- Hardcode appointment type IDs, durations, clinic hours, or selectable slots.
- Generate placeholder availability in a production path.
- Claim a pending request reserves capacity.
- Send provider ID, patient ID, duration, or verification metadata in a request.
- Expose active-link clinical data to an unlinked account.
- Automatically retry non-idempotent request submission after an ambiguous failure.
- Store request reasons, appointment data, tokens, or health data in Room.
- Log tokens, contact values, request reasons, raw API bodies, or clinical data.
- Use Gson, LiveData, or the `org.jetbrains.kotlin.android` plugin.
- Delete or weaken failing tests to make the migration pass.

## Success Criteria

- [x] A patient-visible appointment-type catalog loads for linked and unlinked
      authenticated accounts without hardcoded types.
- [x] A type must be selected before availability can load.
- [x] Availability sends required `appointment_type_id` and renders only the
      latest server-returned slots.
- [x] No production path fabricates selectable time slots.
- [x] Request creation sends required type ID, primary time, ordered distinct
      alternatives (maximum two), reason, conditional referral source, and
      identity only when allowed.
- [x] Referral types cannot advance without a nonblank referral source;
      non-referral types send no stale referral value.
- [x] Expanded request responses map type, alternatives, duration, referral,
      and non-reservation semantics without breaking legacy rows.
- [x] Review and detail show the selected type and all returned preferences.
- [x] Pending, expired, cancellation, and success copy never claims a time is
      held, reserved, or released.
- [x] `GET /appointment-optometrists` is governed as account-only but no patient
      provider picker is introduced.
- [x] Route governance reports 54 canonical routes and 55 callable registered
      routes, including the intentionally unused legacy alias.
- [x] Existing identity, linking, confirmed appointments, rescheduling, visit
      ratings, frames, reservations, prescriptions, eyewear, and messaging tests
      remain green.
- [x] Focused tests, complete unit tests, `assembleDebug`, `lintDebug`, and
      `ktlintCheck` pass.
- [x] `CONTEXT.md` records 55 routes, restored appointment types, variable
      duration, alternatives, referral behavior, and non-binding requests.

## Open Questions

None. Phase 1 was approved on 2026-08-10. Any implementation evidence that
contradicts the resolved decisions must update this living spec and return to
human review before implementation continues.
