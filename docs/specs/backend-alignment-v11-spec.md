# Backend Alignment V11 — Billing Records and Appointment-Linked Frame Reservations

Status: Approved — Android Phases 1–3 complete (2026-07-28); implementation paused by project owner

## Objective

Align the completed Android V10 application with the backend changes now
implemented and documented in the current 2026-07-28 patient API:

1. replace the retired Invoice model and `/invoices` routes with read-only
   internal Billing Records; and
2. require an eligible Appointment when a patient creates a Frame Reservation,
   while displaying the embedded Appointment context in reservation history.

The Android app has not been deployed. V11 performs a clean cutover with no
Invoice or nullable-reservation compatibility layer.

Success means patients can accurately review their internal balance and posted
payment history, open the authoritative Job Order for item details, and reserve
a frame only for a valid scheduled clinic visit.

## Sources of Truth

1. `docs/API_CONTRACT.md` — authoritative patient API at the current backend
   repository state dated 2026-07-28.
2. `docs/BACKEND_CONTEXT.md` — supporting schema, lifecycle, and workflow
   context.
3. Android V10 commits:
   - `c638e43` — versioned prescription migration;
   - `6cdf95b` — 33-route allowlist red proof;
   - `9a8a0be` — clinic-feedback retirement;
   - `f99cded` — V10 context and verification update.

The former standalone billing and reservation backend specification files were
removed because their implemented behavior is now incorporated into the two
authoritative living documents. Android must not restore or depend on those
deleted files.

## Confirmed Assumptions and Android Decisions

1. V10 is implemented and is the V11 client baseline.
2. No deployed Android version or persisted network record requires legacy
   Invoice compatibility.
3. The authoritative patient API contains exactly 33 routes. Two Invoice routes
   are replaced one-for-one by two Billing Record routes.
4. Billing Records are internal operational ledgers, not official invoices,
   receipts, tax documents, or BIR records.
5. Android uses **Billing Record** terminology in packages, symbols,
   navigation, screens, labels, tests, and documentation.
6. All Billing Record money is represented as `BigDecimal` and decoded with the
   existing `MoneyValueSerializer`, which safely accepts the documented JSON
   numbers and decimal strings.
7. Android never converts Billing Record money through `Double` or `Float`.
8. Billing Record list and detail responses use the same resource shape and
   include posted payments. The list uses the standard `{ data, links, meta }`
   envelope.
9. Billing Record item detail is not duplicated. The linked Job Order remains
   authoritative and is opened from Billing Record detail.
10. Unknown Billing Record, payment-status, or payment-method values display
    safely and never enable an action.
11. Billing Record list and detail use separate ViewModels so opening detail
    does not trigger an unnecessary list request.
12. Every new Frame Reservation requires exactly one eligible Appointment.
13. Android continues submitting the single frame variant selected from Frame
    detail as a one-item request. A multi-frame cart remains outside V11 even
    though the API accepts one to five distinct variants.
14. Android loads all Appointment pages needed to build the eligible selection
    set. No new eligible-appointments endpoint is required.
15. A patient-selectable Appointment must:
    - have status `scheduled`;
    - have a parseable schedule;
    - have an end instant (`scheduled_at + duration_minutes`) that has not
      passed; and
    - belong to the authenticated patient, as guaranteed and revalidated by the
      backend.
16. Invalid timestamps and unknown Appointment statuses fail closed.
17. The embedded reservation `appointment` object is the sole domain/display
    source for Appointment context. Android does not expose duplicate scalar
    Appointment state in its domain model.
18. Reservation creation failures use the documented standard `422` validation
    body. Android responds by field, not by parsing message text for an
    undocumented reason code.
19. If no eligible Appointment exists, the patient can book one through a
    dedicated type-safe booking destination that reuses the existing booking
    screen and returns to the in-progress reservation flow.
20. Booking a visit does not automatically submit the reservation. On return,
    Android refreshes eligible Appointments and requires the patient to select
    the new visit and confirm.
21. Successful reservation creation opens Reservation history so the patient
    immediately sees the linked Appointment and selected frame.

Approval of this specification confirms these decisions.

## Contract Delta

### Billing Records

Remove:

```text
GET /api/v1/invoices
GET /api/v1/invoices/{invoice}
```

Add:

```text
GET /api/v1/billing-records
GET /api/v1/billing-records/{billingRecord}
```

`GET /billing-records` is paginated with optional `per_page` and the standard:

```text
data
links
meta
```

The patient-safe Billing Record resource contains:

```text
id
billing_record_number
patient_id
job_order_id
encounter_id
status
total_amount
amount_paid
balance_due
recorded_by
recorded_at
created_at
updated_at
deleted_at
payments[]
    id
    billing_record_id
    amount
    payment_method
    reference_number
    recorded_by
    recorded_at
    notes
    status
    created_at
    updated_at
```

Android domain/presentation needs only:

```text
BillingRecord
    id
    billingRecordNumber
    jobOrderId
    status
    totalAmount
    amountPaid
    balanceDue
    recordedAt
    payments

BillingPayment
    id
    amount
    paymentMethod
    referenceNumber
    status
    recordedAt
```

The DTO may omit declarations for internal or unused fields because production
Kotlinx `Json` ignores unknown keys. Patient, encounter, recorder, notes,
deleted-state, and audit/timestamp fields that do not serve the mobile
experience must not leak into the domain.

Billing Record statuses:

```text
unpaid
partially_paid
paid
voided
```

Only posted payments are returned. Voided payments are excluded by the backend.

Removed Invoice concepts include:

- invoice number and official number;
- draft and issued states;
- sold-to identity and sale type;
- subtotal, discount, and tax breakdowns;
- copied Invoice items;
- issued timestamp;
- Invoice/BIR/receipt wording.

### Appointment-linked Frame Reservations

`POST /api/v1/frame-reservations` changes `appointment_id` from optional to
required:

```json
{
  "appointment_id": 42,
  "items": [
    {
      "product_variant_id": 18
    }
  ]
}
```

The API validates:

- patient ownership;
- `scheduled` Appointment status;
- Appointment end time not being in the past;
- one to five distinct active frame variants; and
- no other active reservation for the Appointment.

All documented reservation validation failures use `422` with the standard
field-error envelope. Unauthenticated requests use the existing `401` flow.

Reservation list, create, and cancel resources now include:

```text
appointment
    id
    appointment_number
    status
    scheduled_at
    duration_minutes
```

`appointment_id` remains present in transport JSON but is not duplicated in the
Android domain model.

## Scope

### In Scope — Billing Records

- Replace Invoice Retrofit services and routes with Billing Record services.
- Replace Invoice DTOs, domain models, repositories, DI, ViewModels, screens,
  routes, navigation, Profile callbacks/labels, and tests.
- Decode exact money with `BigDecimal`.
- Support paginated Billing Record list loading.
- Show list status, total, paid amount, balance, and recorded date when present.
- Show detail financial summary and posted payment history.
- Link Billing Record detail to the associated Job Order.
- Remove all copied-item, tax, discount, official-number, sold-to, issued, and
  Invoice UI.
- Fail safely for unknown read-only enum/string values.
- Keep the patient experience strictly read-only.

### In Scope — Frame Reservations

- Make `appointmentId` non-null in request DTO, repository, and ViewModel
  interfaces.
- Add embedded Appointment DTO/domain mapping.
- Inject `AppointmentV1Repository` into reservation creation.
- Load all Appointment pages and filter eligible visits locally.
- Add required Appointment selection UI with loading, retry, empty, and
  populated states.
- Reuse the existing booking screen through a reservation-return destination.
- Refresh eligible visits after returning from booking.
- Parse and expose standard `422` validation errors without relying on message
  text.
- Clear/reload an invalid Appointment selection when `appointment_id` has a
  field error.
- Keep the selected Appointment for item-only validation failures.
- Show embedded Appointment context in Reservation history.
- Keep existing reservation cancellation behavior.
- Navigate to Reservation history after successful creation.

### In Scope — Contract and Documentation

- Replace the two allowlisted Invoice routes with Billing Record routes while
  retaining exactly 33 routes.
- Update route-variable normalization from `{invoice}` to `{billingRecord}`.
- Remove stale Invoice wording from current Android source/tests/context.
- Update `CONTEXT.md` for Billing Records and appointment-linked reservations.
- Correct V10 spec/plan/task status metadata to reflect the completed V10
  commits.

### Out of Scope

- Backend code, migrations, seeders, tests, or documentation changes.
- Restoring the deleted backend billing/reservation specification files.
- Physical BIR Service Invoice numbers, documents, printing, or tax behavior.
- Patient Billing Record or payment creation/editing.
- Payment correction, reversal, voiding, refunds, credits, or overpayments.
- Billing Record item duplication.
- Adding Billing Record data to Home.
- A new eligible-appointments endpoint.
- A multi-frame cart or catalog redesign.
- Clinic-side walk-in reservation creation.
- Changes to frame allocation or Appointment-triggered cleanup.
- Changes to Appointment booking rules beyond the return destination.
- Room persistence or schema changes.
- New dependencies.
- Compatibility aliases for Invoice routes, symbols, or models.

## Current Android Gap Analysis

### Billing

| Current V10 Android behavior | Required V11 behavior |
|---|---|
| Calls `/invoices` routes. | Call `/billing-records` routes. |
| Uses `Invoice`, `InvoiceItem`, and `InvoicePayment`. | Use minimal `BillingRecord` and `BillingPayment`. |
| Money is nullable `Double`. | Required financial totals use exact `BigDecimal`. |
| Unknown status falls back to `DRAFT`. | Unknown maps to non-actionable `UNKNOWN`. |
| Shows Draft and Issued. | Show Unpaid, Partially paid, Paid, Voided, Unknown. |
| Shows official/sold-to/tax/discount/item concepts. | Remove all retired Invoice concepts. |
| One ViewModel initializes both list and detail. | Separate list/detail ViewModels. |
| Profile and navigation say Invoices. | Use Billing Records everywhere. |
| No Job Order link from financial detail. | Open the authoritative Job Order. |
| Allowlist contains Invoice routes. | Replace them with Billing Record routes; total remains 33. |

### Frame Reservations

| Current V10 Android behavior | Required V11 behavior |
|---|---|
| `appointmentId` is nullable throughout. | Appointment ID is required by construction. |
| Creation sends no Appointment. | Patient must select an eligible Appointment. |
| Creation copy says linkage is optional. | Explain that the frames are prepared for the selected visit. |
| No Appointment repository is used. | Load/filter all patient Appointment pages. |
| Generic create state has Idle/Submitting/Success/Error only. | Include Appointment-loading and field-validation state. |
| DTO/domain omit embedded Appointment. | Map nested Appointment context. |
| Reservation cards show only status/items. | Show Appointment number, schedule, duration, and status. |
| Repository forwards raw `HttpException`. | Map standard `422` fields to a domain validation error. |
| Creation success returns to Frame detail. | Open Reservation history. |

## Functional Requirements

### 1. Billing Record Domain and Transport

- Introduce:
  - `BillingRecord`;
  - `BillingPayment`;
  - `BillingRecordStatus`;
  - `BillingPaymentStatus`.
- Use `UNKNOWN` for unexpected statuses.
- Keep `paymentMethod` as a trimmed raw string because the authoritative mobile
  contract does not enumerate all serialized method values.
- Presentation formats known method strings such as `cash`, `gcash`,
  `bank_transfer`, `credit_card`, and `card`; unknown non-blank values are
  humanized safely, and blank values display **Unknown**.
- Use `MoneyValueSerializer` for `total_amount`, `amount_paid`, `balance_due`,
  and payment `amount`.
- Treat `billing_record_number`, `job_order_id`, and financial totals as
  required.
- Treat `recorded_at`, payment `reference_number`, and payment `recorded_at` as
  nullable.
- Preserve pagination using `PaginationMeta`.
- Map DTOs only in `BillingRecordRepositoryImpl`.
- Delete the old Invoice files rather than wrapping them.

Recommended domain shape:

```kotlin
data class BillingRecord(
    val id: Int,
    val billingRecordNumber: String,
    val jobOrderId: Int,
    val status: BillingRecordStatus,
    val totalAmount: BigDecimal,
    val amountPaid: BigDecimal,
    val balanceDue: BigDecimal,
    val recordedAt: String?,
    val payments: List<BillingPayment>,
)

data class BillingPayment(
    val id: Int,
    val amount: BigDecimal,
    val paymentMethod: String,
    val referenceNumber: String?,
    val status: BillingPaymentStatus,
    val recordedAt: String?,
)
```

### 2. Billing Record List

- Destination title and Profile label: **Billing Records**.
- Supporting copy: **View balances and payments**.
- Preserve server pagination order and append pages without re-sorting.
- Each card shows:
  - Billing Record number;
  - patient-friendly status;
  - total amount;
  - amount paid;
  - balance due; and
  - recorded date/time when present.
- Use Philippine peso formatting without conversion through floating point.
- Empty copy: **No billing records yet**.
- Loading, retry, pull-to-refresh, and load-more behavior follow existing
  paginated record screens.
- Opening a card navigates to `BillingRecordDetail(id)`.

### 3. Billing Record Detail

- Use a dedicated detail ViewModel that loads only the requested resource.
- Show:
  - Billing Record number and status;
  - Total amount;
  - Amount paid;
  - Balance due;
  - recorded date/time when present;
  - posted payment history.
- Each payment row shows:
  - amount;
  - payment method;
  - optional reference number; and
  - recorded date/time when present.
- If payments are empty, show **No payments recorded yet**.
- Provide **View job order**, navigating to `JobOrderDetail(jobOrderId)`.
- Never show line items, tax, discount, sold-to, official-number, or BIR copy.
- Voided and unknown records remain readable and expose no mutation.

### 4. Billing Navigation and Route Contract

- Replace:

```text
InvoiceList
InvoiceDetail(invoiceId)
```

with:

```text
BillingRecordList
BillingRecordDetail(billingRecordId)
```

- Hide bottom navigation on Billing Record sub-destinations just as it was
  hidden on Invoice destinations.
- Rename all Profile callback parameters from Invoice to Billing Record.
- Replace the two allowlisted routes without changing the total of 33.
- Normalize `billing-records/{id}` to
  `billing-records/{billingRecord}` in route tests.

### 5. Reservation Appointment Domain and Mapping

- Add a `ReservationAppointment` domain type:

```kotlin
data class ReservationAppointment(
    val id: Int,
    val appointmentNumber: String?,
    val status: AppointmentStatus,
    val scheduledAt: String,
    val durationMinutes: Int,
)
```

- `FrameReservation` contains `appointment: ReservationAppointment`.
- Remove nullable `appointmentId` from the domain.
- The transport DTO requires both `appointment_id` and `appointment`.
- Repository mapping uses the nested Appointment as the display source.
- Unknown embedded status maps through `AppointmentStatus.from` and remains
  non-actionable.
- List, create, and cancel responses use the same mapping.

### 6. Eligible Appointment Selection

- Create a pure, testable eligibility function accepting an Appointment and
  `now: Instant`.
- Eligible:
  - `status == SCHEDULED`;
  - `scheduledAt` parses as an offset timestamp;
  - `durationMinutes` is non-negative;
  - end instant is equal to or after `now`.
- Ineligible:
  - checked-in, fulfilled, cancelled, no-show, or unknown;
  - malformed schedule;
  - negative duration;
  - end instant before `now`.
- Sort eligible visits by scheduled instant ascending.
- Load page 1, then sequentially load through `lastPage`.
- Deduplicate by Appointment ID before filtering.
- A later-page failure produces a retryable Appointment-loading error rather
  than silently presenting an incomplete eligible set.
- Do not call Appointment detail for each candidate.

### 7. Reservation Creation State and Validation

Creation state must represent:

- loading Appointments;
- Appointment-load failure;
- ready with eligible Appointments and optional selection;
- no eligible Appointments;
- submitting;
- validation error while preserving the usable draft;
- success.

Rules:

- Confirm is disabled until an Appointment is selected.
- `submit` requires a non-null Appointment ID in its function signature.
- Ignore duplicate submit taps.
- On an `appointment_id` validation error:
  - show safe validation guidance;
  - clear the invalid selection;
  - reload eligible Appointments.
- On an `items` validation error:
  - keep the selected Appointment;
  - explain that the selected frame is no longer eligible;
  - offer Back to choose another frame.
- On an unclassified `422`:
  - keep the form open;
  - show the backend-safe validation summary without branching on message text.
- Authentication continues through the existing global `401` handling.

### 8. Reservation Creation UI and Booking Return

- Show the selected frame context already supplied by the route; no additional
  product request is added solely for Appointment selection.
- Explain that the clinic will prepare the frame for the selected visit.
- Render Appointment choices with:
  - appointment number when present;
  - appointment type;
  - date;
  - start time;
  - duration;
  - Scheduled status.
- Use accessible single-choice selection.
- No eligible state includes:
  - explanation that a scheduled visit is required;
  - **Book an appointment**;
  - Back.
- Add a dedicated `BookAppointmentForReservation` route that reuses
  `BookAppointmentScreen`.
- Successful booking from that route pops only the booking destination and
  returns to the existing reservation screen.
- On resume, reload eligible Appointments.
- Do not automatically select or submit the newly created Appointment.
- On reservation success, replace the creation destination with
  `FrameReservationList`.

### 9. Reservation History

- Display authoritative embedded Appointment context on every reservation:
  - number;
  - date/time;
  - duration;
  - patient-facing status.
- Continue showing reservation status and frame items.
- Continue allowing cancellation only for requested or prepared reservations.
- Map the cancel response back into the list, including its embedded
  Appointment.
- Preserve pull-to-refresh and empty/error behavior.
- Do not locally mutate reservations in response to Appointment cancellation or
  no-show; backend lifecycle handling remains authoritative.

## Tech Stack

No dependency or platform change:

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.0 |
| Build | AGP 9.2.1 built-in Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean (`data` → `domain` → `presentation`) |
| Dependency injection | Hilt 2.59.2 |
| Network | Retrofit 2.11 + OkHttp 4.12 + Kotlinx Serialization 1.8.1 |
| State | `StateFlow` with sealed UI-state interfaces |
| Money | `BigDecimal` + existing `MoneyValueSerializer` |
| Tests | JUnit 5, MockK, Turbine, coroutines-test, MockWebServer |

## Commands

```powershell
.\gradlew testDebugUnitTest --tests "*BillingRecord*"
.\gradlew testDebugUnitTest --tests "*FrameReservation*"
.\gradlew testDebugUnitTest --tests "*CreateFrameReservation*"
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
```

## Expected Project Structure

```text
app/src/main/java/com/eyecare/app/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── BillingRecordApiService.kt
│   │   │   └── FrameReservationApiService.kt
│   │   └── dto/
│   │       ├── BillingRecordDtos.kt
│   │       └── FrameReservationDtos.kt
│   └── repository/
│       ├── BillingRecordRepositoryImpl.kt
│       └── FrameReservationRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── BillingRecord.kt
│   │   └── FrameReservation.kt
│   └── repository/
│       ├── BillingRecordRepository.kt
│       └── FrameReservationRepository.kt
├── di/
│   ├── BillingRecordModule.kt
│   └── FrameReservationModule.kt
└── presentation/
    ├── billingrecords/
    │   ├── BillingRecordListViewModel.kt
    │   ├── BillingRecordDetailViewModel.kt
    │   └── BillingRecordScreens.kt
    ├── reservations/
    │   ├── CreateFrameReservationViewModel.kt
    │   ├── CreateFrameReservationScreen.kt
    │   ├── FrameReservationListViewModel.kt
    │   └── FrameReservationListScreen.kt
    ├── navigation/
    └── profile/
```

The corresponding Invoice files and `presentation/invoices/` package are
deleted during the clean cutover.

## Code Style

Exact money remains explicit:

```kotlin
@Serializable
data class BillingRecordDto(
    val id: Int,
    @SerialName("billing_record_number")
    val billingRecordNumber: String,
    @SerialName("total_amount")
    @Serializable(with = MoneyValueSerializer::class)
    val totalAmount: BigDecimal,
)
```

Reservation submission is non-null by construction:

```kotlin
suspend fun createReservation(
    variantIds: List<Int>,
    appointmentId: Int,
): Result<FrameReservation>
```

Eligibility is a pure policy:

```kotlin
internal fun isReservationEligible(
    appointment: AppointmentV1,
    now: Instant,
): Boolean
```

## Testing Strategy

Implementation follows test-driven slices after Phase 3 approval.

### Billing DTO and repository tests

- Decode documented list/detail resources.
- Decode standard pagination metadata.
- Decode money from documented JSON numbers and decimal strings.
- Preserve nullable `recorded_at` and payment references.
- Map all Billing Record statuses plus unknown.
- Map posted and unknown payment statuses.
- Preserve raw payment methods.
- Verify list/detail request paths.
- Verify DTOs do not require unused internal fields.

### Billing ViewModel tests

- List initial, empty, retry, pagination, and load-more failure behavior.
- Server-order preservation.
- Detail performs no list request.
- Detail retry reloads the active ID.
- Job Order ID is available for navigation.

### Billing presentation tests

- Peso formatting uses `BigDecimal`.
- Status and payment-method labels are safe.
- List/detail use Billing Record terminology only.
- Empty payment history appears.
- Job Order callback receives the correct ID.
- No retired Invoice concepts render.

### Reservation DTO and repository tests

- Decode embedded Appointment for list, create, and cancel responses.
- Map nested status through `AppointmentStatus`.
- Serialize required `appointment_id`.
- Prove null Appointment submission cannot compile through the repository API.
- Parse standard `422` field errors into a reservation validation error.
- Preserve item mapping and cancellation behavior.

### Reservation eligibility and ViewModel tests

- Scheduled future and in-progress appointments are eligible through their end
  instant.
- Exact end instant is eligible.
- Past-end, checked-in, fulfilled, cancelled, no-show, unknown, malformed, and
  negative-duration Appointments are excluded.
- All pages load and deduplicate.
- Later-page failure does not present a partial set.
- Submit requires selection and ignores duplicate taps.
- Appointment field errors clear/reload selection.
- Item field errors retain Appointment selection.
- Returning from booking refreshes candidates.

### Reservation presentation/navigation tests

- Loading, retry, no-eligible, ready, validation, submitting, and success
  states render.
- Booking route returns to the reservation draft.
- Creation success opens Reservation history.
- Reservation cards display embedded Appointment context.
- Cancellation action matrix remains unchanged.

### Route and removal tests

- Allowlist contains Billing Record routes and no Invoice routes.
- Retrofit route discovery contains exactly 33 routes.
- No Invoice symbol, package, route, or patient-facing text remains.
- Deferred Billing Record and Reservation backend spec files are not restored.

### Required verification

- Focused tests pass after each coherent slice.
- Full unit suite passes.
- Android test APK compiles.
- Ktlint and Android lint pass.
- Debug APK assembles.
- Manual smoke testing covers all four Billing Record statuses, empty and
  populated payment history, eligible/ineligible Appointment selection,
  booking return, reservation validation, success, history, and cancellation.

## Boundaries

### Always

- Follow the corrected current `API_CONTRACT.md`.
- Keep the route count at exactly 33.
- Use Billing Record terminology.
- Use Kotlinx Serialization, never Gson.
- Use `BigDecimal` for financial values.
- Map DTOs to domain models at repository boundaries.
- Keep Billing Records read-only.
- Require Appointment selection before reservation submission.
- Revalidate on the backend after local eligibility filtering.
- Fail closed for unknown statuses and malformed timestamps.
- Parse `422` fields structurally without branching on message text.
- Keep financial, Appointment, and patient response bodies out of logs.
- Run focused tests and `.\gradlew assembleDebug` after implementation
  changes.

### Ask First

- Add a dependency.
- Add an endpoint.
- Add a multi-frame cart.
- Change the existing booking domain rules.
- Automatically submit after booking.
- Persist Billing Records, Appointments, or reservations in Room.
- Add patient payment mutations.
- Retain an Invoice compatibility layer.
- Expand V11 into backend implementation.

### Never

- Call `/invoices` after cutover.
- Present a Billing Record as an official invoice, receipt, or BIR document.
- Use `Double` or `Float` for Billing Record money.
- Duplicate Job Order items into Billing Record UI/domain.
- Submit a null or omitted reservation `appointment_id`.
- Treat checked-in or past-end Appointments as patient-selectable.
- Parse validation message prose to infer conflict categories.
- Trust local eligibility instead of backend validation.
- Restore deleted backend future-spec files.
- Log tokens, clinical data, payment details, or patient API bodies.

## Success Criteria

### Billing Records

- [ ] Android calls only `/billing-records` list/detail routes.
- [ ] Billing list pagination uses documented `links`/`meta`.
- [ ] All financial values remain exact `BigDecimal`.
- [ ] Unpaid, partially paid, paid, voided, and unknown display safely.
- [ ] List/detail show totals, balances, and posted payments.
- [ ] Empty payment history has explicit copy.
- [ ] Detail opens the linked Job Order.
- [ ] No Invoice model, route, package, test, callback, or label remains.
- [ ] No official/BIR, copied-item, sold-to, tax, discount, or issued concept
  remains.

### Frame Reservations

- [ ] Repository and request DTO require a non-null Appointment ID.
- [ ] All Appointment pages are loaded before presenting eligible choices.
- [ ] Eligibility matches scheduled status and inclusive end-time rules.
- [ ] No eligible state can open booking and return to the reservation draft.
- [ ] Patient must select and confirm; booking never auto-submits.
- [ ] Standard `422` field errors produce actionable, safe UI recovery.
- [ ] List/create/cancel responses map embedded Appointment context.
- [ ] Reservation history shows Appointment number, schedule, duration, and
  status.
- [ ] Existing requested/prepared cancellation behavior remains.
- [ ] Creation success opens Reservation history.

### Contract and Quality

- [ ] Approved and discovered Retrofit route counts are exactly 33.
- [ ] Invoice routes are replaced one-for-one with Billing Record routes.
- [ ] V10 status metadata reflects completed implementation.
- [ ] `CONTEXT.md` describes current Billing Record and reservation behavior.
- [ ] Focused and full unit tests pass.
- [ ] Android test APK compiles.
- [ ] Ktlint and Android lint pass.
- [ ] Debug assembly succeeds.

## Open Questions

None. The corrected source documents now agree on route count, Billing Record
pagination, resource fields, reservation linkage, eligibility, and validation
semantics.

## Phase Gate

Phases 1–3 were approved by the project owner on 2026-07-28.

Implementation is explicitly paused by the project owner. Do not modify Android
production code until the project owner gives a separate instruction to
proceed.
