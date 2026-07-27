# Spec: Backend Alignment V8 — Patient Workflow Migration

Status: Approved — Phase 3 implementation in progress (2026-07-27)

## Objective

Migrate the Eyecare Android app from the retired customer commerce API to the
backend's sole patient-mobile contract at `/api/v1`.

The primary user is a patient whose authenticated account is linked to an
independent clinical patient record. The app must support the patient-facing
journey documented in `docs/BACKEND_CONTEXT.md`:

- authenticate and maintain the patient's own profile;
- book, view, reschedule, and cancel appointments;
- complete and submit appointment intake;
- browse AR-capable frames and manage frame reservations;
- view prescriptions, quotations, job orders, and invoices;
- communicate with the clinic through the patient's singleton conversation;
- submit appointment feedback and eligible frame ratings.

The migration is successful when Android calls only the approved 34 patient
routes, models the new resources without leaking DTOs into presentation, and
contains no reachable order/billing/accessory-purchase flow that the backend no
longer permits.

## Source of Truth and Decisions

The Android contract is defined by:

1. `docs/BACKEND_CONTEXT.md`, dated 2026-07-27, for backend scope, workflow,
   roles, and the patient route allowlist; and
2. `docs/API_CONTRACT.md` for request/response details at backend commit
   `ebd1e2e` (2026-07-27).

The contract review resolves these earlier uncertainties:

- The allowlist contains 34 routes. `appointment-types` is already one of the
  34 routes in both documents.
- Booking uses `appointment_type_id`, never `visit_reason_id`.
- `/me` exposes account and patient fields; registration has no privacy
  acknowledgement fields.
- Intake is editable only while `draft`.
- Reservations contain 1–5 active frame variants and may optionally link an
  owned appointment; only `requested` and `prepared` may be cancelled.
- Conversation unread state is read-only on mobile. There is no mark-read
  mutation.
- Feedback targets completed appointments only.
- Accessories, orders, billing PDF, feedback history, and appointment
  contact-note editing are intentionally retired.
- Existing frame browsing and AR rendering should be adapted to `GET /frames`
  and `GET /frames/{frame}`.
- Embedded message contexts remain part of message payloads. Appointment
  contexts may stay actionable; retired order/product contexts must render as
  non-interactive historical context unless a supported destination exists.

Approved product decisions:

1. Android will implement all 34 patient routes for a fully functional app.
2. This is a single-release API cutover delivered as dependency-ordered,
   independently compiling vertical slices. The released app will not depend
   on both legacy and new contracts.
3. Legacy routes, models, repositories, screens, and tests are removed as their
   replacements land. No compatibility adapters or runtime fallback are
   maintained.
4. Room may continue to cache non-clinical frame catalog data, but tokens,
   intake data, prescriptions, and all other health or clinical data remain
   excluded from Room.
5. The four approved root destinations are Home, Frames, Appointments, and
   Profile.

## Scope

### In Scope

- Retrofit base URL and all service declarations for the `/api/v1` allowlist.
- DTO, domain model, repository, dependency-injection, ViewModel, and Compose
  changes required by the new patient resources.
- Type-safe navigation for frames, reservations, intake, quotations, job
  orders, and invoices.
- Removal or replacement of Android entry points that call retired routes.
- Authenticated retrieval of private conversation attachments.
- Pagination for endpoints explicitly documented as paginated. Conversation
  messages and frame reservations are explicitly unpaginated full lists.
- Contract, mapping, ViewModel, and relevant Compose tests.
- Updating `CONTEXT.md` after implementation to describe the final behavior.

### Out of Scope

- Filament, staff, admin, optometrist, payment-recording, dispensing, and other
  clinic-side workflows.
- Android creation of quotations, job orders, invoices, payments, checkout
  records, orders, billings, or purchases.
- Backend schema, route, policy, or response changes unless separately
  approved.
- Offline storage of clinical or health data.
- Keeping legacy API routes alive solely for backward compatibility.
- New dependencies, analytics, push notifications, or background sync unless
  separately specified.

## Approved Mobile Route Contract

Android must have no production Retrofit declaration or reachable request
outside this allowlist after cutover.

| Area | Routes | Android responsibility |
|---|---|---|
| Authentication | `POST /register`, `POST /login`, `POST /logout` | Create and end a patient session; store only the Sanctum token in encrypted preferences. |
| Own profile | `GET /me`, `PATCH /me` | Display account identity plus patient number, full name, birth date, occupation, address, gender, and contact email; update only documented optional fields. |
| Appointment setup | `GET /appointment-types`, `GET /appointment-availability` | Load backend-owned appointment types; request slots by `date` and `appointment_type_id`, with optional reschedule appointment/provider context. |
| Appointments | `GET /appointments`, `POST /appointments`, `GET /appointments/{appointment}`, `POST /appointments/{appointment}/reschedule`, `POST /appointments/{appointment}/cancel` | Paginated history, booking, detail, and permitted patient mutations. |
| Intake | `GET /appointments/{appointment}/intake`, `PUT /appointments/{appointment}/intake`, `POST /appointments/{appointment}/intake/submit` | Load/save a draft and perform the explicit irreversible submit transition. |
| Frames | `GET /frames`, `GET /frames/{frame}` | Browse frames and open AR only when the returned variant has a usable AR asset. |
| Reservations | `GET /frame-reservations`, `POST /frame-reservations`, `POST /frame-reservations/{reservation}/cancel` | Unpaginated reservation history, creation with 1–5 frame variants, optional appointment linking, and valid patient cancellation. GET uses a sanitized resource; mutation responses must be confirmed to use the same representation. |
| Prescriptions | `GET /prescriptions`, `GET /prescriptions/{prescription}` | Paginated, read-only clinical prescription history and detail. |
| Quotations | `GET /quotations`, `GET /quotations/{quotation}` | Paginated, read-only quotation history and immutable revision detail. |
| Job orders | `GET /job-orders`, `GET /job-orders/{jobOrder}` | Paginated, read-only fulfillment status and item detail. |
| Invoices | `GET /invoices`, `GET /invoices/{invoice}` | Paginated, read-only financial history, items, and posted/voided payment trail. |
| Conversation | `GET /conversation`, `GET /conversation/messages`, `POST /conversation/messages`, `GET /conversation/attachments/{attachment}` | Use the authenticated patient's singleton conversation; send text plus an optional attachment/context payload and securely open image, PDF, DOC, or DOCX attachments. |
| Feedback | `POST /feedback` | Submit one-to-five-star feedback with an optional comment for an owned completed appointment. |
| Frame ratings | `POST /job-order-items/{item}/rating` | Create or revise a rating after the backend verifies patient ownership, dispensed job-order status, item/variant consistency, and dispensing-event ownership. Client-side eligibility remains UX only. |

All paths above are relative to a Retrofit base URL ending in `/api/v1/`.

## Current Android Gap Analysis

| Current Android behavior | Required behavior | Disposition |
|---|---|---|
| Base URL defaults to `/api/`. | Base URL ends in `/api/v1/`. | Replace configuration defaults and document local/release override expectations. |
| Profile uses `GET/PATCH /user` and a customer-shaped `User`. | Profile uses `GET/PATCH /me` and returns account plus nullable patient-demographic fields. | Replace endpoint and split or expand the domain model without storing demographics locally. |
| Booking loads `/visit-reasons` and availability from `/appointments/availability`. | Booking loads `/appointment-types` and calls `/appointment-availability` with `date` and `appointment_type_id`. | Replace visit-reason state, add conditional `referring_source`, and preserve backend slot timestamps unchanged when submitting. |
| Appointment responses are non-paginated and support `/contact-note`. | Appointment lists are paginated; no contact-note route is approved. | Add pagination and remove the unsupported edit mutation while retaining only fields returned by the new resource. |
| No patient-intake feature exists. | Draft load/save plus explicit submission are supported. | Add a new intake vertical slice with submitted/verified states enforced by backend responses. |
| Catalog uses `/products`, `/brands`, and `/categories`; accessories are orderable. | Mobile catalog exposes `/frames` only; patients cannot create purchases. | Replace the catalog data source with frames, preserve AR, and remove accessory/order entry points. |
| Orders are created, listed, detailed, and cancelled. | Patients create frame reservations and read clinic-created job orders. | Remove order creation/history semantics; add reservation and read-only job-order features. |
| Billing detail and PDF use `/billing/{id}`. | Financial records are read-only invoices at `/invoices`. | Replace billing models/screens with invoice list/detail; remove PDF until a route is documented. |
| Prescription list assumes an unpaginated response. | Prescription list is paginated. | Preserve read-only UI while replacing response and paging behavior. |
| No quotations feature exists. | Quotations are patient-readable. | Add quotation list/detail with revision-aware display. |
| Conversation endpoints require a conversation ID and expose mark-read. | Conversation endpoints are singleton paths and no mark-read route is listed. | Remove route IDs and explicit mark-read calls; use backend-defined unread behavior. |
| Attachments accept images/PDF and are upload metadata only. | Uploads also accept DOC/DOCX up to 10 MB; private files are fetched through an authenticated endpoint. | Expand validation, add secure download/open behavior, and avoid constructing public storage URLs. |
| Feedback includes GET history and appointment/order targets. | Only completed appointments can be submitted to `POST /feedback`. | Remove history/order fields and show submission only from completed appointment detail. |
| No frame-rating flow exists. | The endpoint creates or revises one rating per patient/variant with server-enforced eligibility. | Add the action only for dispensed job-order detail items with a non-null matching frame variant. |
| Home displays accessory shelves and an active order. | Home must represent the patient workflow. | Replace legacy commerce cards with confirmed new resources without duplicating full list screens. |
| Profile links to Orders and Feedback History. | Profile must expose the new read-only records and reservations. | Replace hub destinations after information architecture is approved. |

## Contract Evidence Status

`docs/API_CONTRACT.md` supplies the required field names, validation rules,
envelopes, timestamps, status values, error shapes, rate limits, and retired
feature decisions for the API at backend commit `ebd1e2e`.

The updated contract now documents:

- unpaginated frame reservations and their complete nested variant/product;
- quotation, job-order, and invoice detail responses;
- indexed multipart message contexts;
- job-order detail item variant IDs; and
- complete frame-rating and revision responses.

Production DTOs must be backed by JSON fixtures copied from that contract.
Where a remaining example still uses `{ ... }` or `{ /* Resource */ }`, reuse a
fully expanded resource example only when the contract explicitly states it is
the same resource; otherwise obtain a canonical fixture.

No missing DTO field should be guessed from a database column or Filament
model.

## Security Review

Backend commit `ebd1e2e` resolves the original security and response findings:

1. `GET /frame-reservations` now uses `FrameReservationResource` and excludes
   cost price, inventory quantities/thresholds, soft-delete state, and other
   internal model fields.
2. `POST /job-order-items/{item}/rating` now enforces patient ownership,
   `dispensed` job-order status, request/item variant equality, and
   dispensing-event ownership server-side.
3. GET, create, and cancel reservation responses all use the same sanitized
   `FrameReservationResource`.
4. `BACKEND_CONTEXT.md` explicitly identifies frame reservations and
   conversation messages as the two unpaginated list exceptions.

Paginated endpoints will use the canonical Laravel `data`/`links`/`meta`
envelope shown by the appointment and frame examples. Contract fixtures must
verify this before repository implementation.

## Tech Stack

| Layer | Technology |
|---|---|
| Language/build | Kotlin 2.3.0, AGP 9.2.1 built-in Kotlin |
| UI | Jetpack Compose + Material 3 BOM 2026.05.01 |
| Architecture | MVVM + Clean (`data` → `domain` → `presentation`) |
| Dependency injection | Hilt 2.59.2 |
| Network | Retrofit 2.11, OkHttp 4.12, Kotlinx Serialization 1.8.1 |
| Local cache | Room 2.7.1, frame catalog only if retained |
| Navigation | Navigation Compose 2.9.0 type-safe `@Serializable` routes |
| Images/AR | Coil 3.1.0, CameraX 1.5.0, MediaPipe 0.10.35 |
| Tests | JUnit 5, MockK, Turbine, coroutines-test, MockWebServer |

No dependency change is currently required.

## Commands

Run from the repository root in PowerShell:

```powershell
.\gradlew ktlintFormat
.\gradlew ktlintCheck
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
```

Focused test commands will be defined in Phase 3 after the target files and
vertical slices are approved.

## Project Structure

Existing layers remain authoritative. New files should be grouped by patient
resource rather than placed into a generic API or screen package.

```text
app/src/main/java/com/eyecare/app/
├── data/
│   ├── remote/
│   │   ├── api/          Retrofit services by resource group
│   │   ├── dto/          Kotlinx-serializable transport models
│   │   └── interceptor/  Sanctum auth and centralized 401 handling
│   ├── local/            Frame-only Room cache, if retained
│   └── repository/       DTO-to-domain mapping and error translation
├── domain/
│   ├── model/            Transport-independent patient resource models
│   └── repository/       Stable feature contracts
├── presentation/
│   ├── appointments/     List, detail, booking, reschedule
│   ├── intake/           Appointment intake draft and submission
│   ├── frames/           Browse, detail, and AR entry
│   ├── reservations/     Frame reservation list/create/cancel
│   ├── prescriptions/    Read-only list/detail
│   ├── quotations/       Read-only list/detail
│   ├── joborders/        Read-only list/detail and rating entry
│   ├── invoices/         Read-only list/detail
│   ├── messaging/        Singleton conversation and attachments
│   ├── feedback/         Submission only
│   ├── profile/          Account-linked patient profile and hub
│   └── navigation/       Type-safe routes and graph wiring
└── di/                   Resource service/repository bindings

app/src/test/java/com/eyecare/app/
├── data/                 Contract decoding and repository mapping tests
├── domain/               Status and eligibility policy tests
└── presentation/         ViewModel state-transition tests

docs/specs/               Living specification, plan, and task status
```

Package names may reuse `catalog` for frames and `billing` for invoices only if
the names remain semantically accurate after the migration. New clinical data
must not be added to `data/local`.

## Code Style

API naming stays in DTOs; repositories produce domain models; UI observes a
sealed `StateFlow`.

```kotlin
@Serializable
data class FrameResponse(
    val data: FrameDto,
)

data class Frame(
    val id: Int,
    val name: String,
)

internal fun FrameDto.toDomain(): Frame =
    Frame(
        id = id,
        name = name,
    )

sealed interface FrameDetailUiState {
    data object Loading : FrameDetailUiState
    data class Success(val frame: Frame) : FrameDetailUiState
    data class Error(val message: String) : FrameDetailUiState
}
```

Conventions:

- Use `@Serializable` and `@SerialName`; never Gson.
- Keep API envelopes, pagination DTOs, and request DTOs in `data/remote/dto`.
- Map every DTO to a domain model at the repository boundary.
- Return `Result<T>` from repository operations and translate structured API
  failures before the ViewModel.
- Use `sealed interface` UI states exposed as read-only `StateFlow`.
- Use type-safe `@Serializable` navigation routes.
- Preserve backend enum strings with explicit fail-closed mapping; unknown
  statuses must not become actionable states.
- Use `java.time` and the documented clinic timezone for appointment display
  and submission.
- Prefer small resource-specific services and repositories over a single
  all-purpose patient API class.

## UX and Information Architecture Requirements

- Keep the four-root navigation model unless review approves a redesign.
  Proposed semantics are Home, Frames, Appointments, and Profile.
- Home prioritizes the next appointment, incomplete/submittable intake, active
  frame reservation, active quotation/job order, and unpaid invoice only when
  those summaries are available without excessive network fan-out.
- Frames remain browsable and AR-capable. The former accessory tab and all
  purchase/order actions disappear.
- A frame detail action creates a reservation, not an order. It must show the
  backend-confirmed reservation result and never imply payment or purchase.
- Profile acts as the hub for Messages, Reservations, Prescriptions,
  Quotations, Job Orders, and Invoices.
- Intake submission requires an explicit confirmation because submission is a
  state transition; draft save does not.
- Cancellation actions require confirmation and are shown only when server
  status makes them eligible.
- Read-only resources must not present disabled edit controls that imply a
  patient mutation exists.
- Loading, empty, retryable error, forbidden, validation, and terminal-state
  UI must be distinguishable for every feature.

The final labels and hierarchy remain subject to review after exact resource
payloads reveal which summaries are available.

## Testing Strategy

### Contract tests

- Use canonical backend fixtures with Kotlinx Serialization to cover every
  response DTO, request DTO, error envelope, and pagination envelope.
- Verify serialization names and omission/null behavior for every mutation.
- Add a route allowlist test that enumerates production service annotations and
  fails if a retired or non-`/api/v1` patient route is introduced.

### Repository tests

- Verify DTO-to-domain mapping, nullability, enum handling, pagination, and
  structured HTTP error translation.
- Verify patient-scoped resources cannot be cross-linked using stale IDs in
  repository or presentation state.
- Verify attachments are fetched through authenticated API calls and are not
  exposed as public storage URLs.

### ViewModel tests

- Cover loading, success, empty, validation, authorization, conflict/terminal,
  retry, and mutation-in-progress states.
- Cover status-driven action eligibility for appointments, intake,
  reservations, job-order ratings, and invoices.
- Use Turbine and `kotlinx-coroutines-test`; do not depend on wall-clock delays.

### UI and integration checks

- Add focused Compose tests for intake confirmation, reservation cancellation,
  read-only resource actions, and navigation visibility.
- Use MockWebServer for representative authenticated happy paths and errors.
- Manually verify booking/timezone behavior, AR launch, private image/PDF
  attachment opening, and back-stack behavior on an emulator.

### Required gates

- All focused and full unit tests pass.
- `ktlintCheck` passes.
- `lintDebug` passes, or pre-existing unrelated failures are documented with
  exact file/diagnostic evidence.
- `assembleDebug` passes after every implementation task and at final cutover.

## Boundaries

### Always

- Treat the exact 34 `/api/v1` routes as the patient-mobile allowlist.
- Derive JSON contracts from `API_CONTRACT.md` fixtures or backend
  resources/tests, not database prose.
- Map DTOs to domain models at repository boundaries.
- Scope navigation and actions to the authenticated patient's resources.
- Keep tokens in encrypted preferences and fetch private attachments with
  authentication.
- Implement pagination only where the contract documents it; keep conversation
  messages and frame reservations unpaginated.
- Fail closed for unknown statuses and unsupported product/resource types.
- Update the spec before implementing a discovered contract or scope change.
- Run `.\gradlew assembleDebug` after changes.

### Ask First

- Change the backend or request a new mobile endpoint.
- Add or upgrade a dependency.
- Change Room schema or retain a new offline cache.
- Change the four root navigation destinations.
- Preserve a legacy endpoint or feature during cutover.
- Infer a relationship or ID from display text.
- Store any additional personal data on device.

### Never

- Call legacy `/products`, `/orders`, `/billing`, `/visit-reasons`,
  `/appointments/availability`, `/user`, or plural `/conversations` routes
  after cutover.
- Let Android create job orders, invoices, payments, purchases, or billings.
- Store tokens, intake answers, prescriptions, encounter data, or other health
  data in Room.
- Expose private attachment URLs without authenticated retrieval.
- Treat client-side rating eligibility as authorization; the server remains
  authoritative even when Android hides an ineligible action.
- Add internal reservation fields such as cost price or stock thresholds to
  Android domain/presentation models.
- Guess required DTO fields, enum transitions, or authorization behavior.
- Use Gson, LiveData, string navigation routes, or
  `org.jetbrains.kotlin.android`.
- Remove failing tests merely to make the migration green.

## Success Criteria

- [ ] All assumptions and blocking questions in this specification are
  reviewed and resolved.
- [ ] The Retrofit base URL resolves to `/api/v1/` in debug and release
  configurations.
- [ ] Production code can invoke all and only the 34 approved patient routes.
- [ ] Every endpoint has canonical request, success, relevant pagination, and
  relevant error fixtures.
- [ ] Authentication and profile use `/me` and correctly represent the
  account-linked patient identity.
- [ ] Appointment booking uses backend appointment types and availability
  without locally invented IDs or slots.
- [ ] Appointment lists, frames, prescriptions, quotations, job orders, and
  invoices page correctly to exhaustion; reservations follow the clarified
  backend contract.
- [ ] Intake supports load, draft save, and confirmed submission while keeping
  all data network-only.
- [ ] Frame browsing and AR work through `/frames`; accessories and purchase
  actions are absent.
- [ ] Patients can create/cancel frame reservations only when allowed by
  backend state, and reservation responses expose no commercial/internal
  inventory fields.
- [ ] Prescriptions, quotations, job orders, and invoices are read-only;
  job-order-item rating follows the server-enforced eligibility contract.
- [ ] Messaging uses singleton conversation routes, and private attachments
  open through authenticated retrieval.
- [ ] No retired route is referenced by a Retrofit annotation, repository, or
  reachable UI action.
- [ ] Focused tests, the full unit suite, formatting check, lint, and debug
  assembly meet the required gates.
- [ ] `CONTEXT.md` and this living spec describe the implemented contract.

## Open Questions

None. Contract, scope, cutover strategy, security boundaries, local-storage
boundaries, and root navigation are approved.

## Phase Gate

Phases 1–2 were approved on 2026-07-27. The Phase 3 task list has been produced
for human review. Do not change production code until the Phase 3 task list is
approved.
