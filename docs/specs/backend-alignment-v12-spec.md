# Backend Alignment V12 — Unified Patient Eyewear Journey

Status: Approved — Android Phases 1–2 complete (2026-07-29); Phase 3 tasks awaiting approval

## Objective

Replace the patient-facing separation between Quotations, Job Orders, and
Billing Records with one read-only **Eyewear** journey backed by the new
aggregate API:

```text
GET /api/v1/eyewear
GET /api/v1/eyewear/{key}
```

Patients should see one transaction card that follows their eyewear from
estimate through preparation, pickup readiness, release, and payment, while the
clinic continues managing the three operational records independently.

Success means the patient has one Profile destination, one Current/History
list, and one progressive detail page with only the sections that exist.
Patients cannot use this experience to accept an estimate, place or modify an
order, change preparation state, dispense eyewear, or change payments.

## Sources of Truth

1. `docs/API_CONTRACT.md` at the backend state dated 2026-07-29, including the
   Eyewear aggregate and 35-route appendix.
2. `docs/BACKEND_CONTEXT.md`, including persistent `eyw_{ULID}` keys,
   `jo_{job_order_id}` lookup aliases, and aggregate lifecycle rules.
3. Current Android V11 code at Git HEAD, which already contains separate
   Quotation, Job Order, and Billing Record experiences.
4. This approved specification for Android product language, navigation,
   presentation policy, and gradual retirement behavior.

The two backend documents are user-owned inputs. Android work must not edit
them. Minor documentation defects observed during specification do not block
Android design:

- the Billing Records section lost its `GET /billing-records` list subheading;
- `consultation_at` is intended to be nullable but that nullability is only
  implicit in the examples and prerequisite; and
- the backend context's top reconciliation timestamp/test count predates the
  Eyewear addition.

Android therefore decodes `consultation_at` defensively as nullable and relies
on `created_at` for the documented fallback display.

## Confirmed Assumptions and Product Decisions

1. V11 is implemented and is the V12 baseline.
2. The new backend aggregate, rather than Android-side joining, is the sole
   source for the unified Eyewear list and detail.
3. The patient-visible navigation entry is the existing Profile **Care &
   activity** area. Its Quotation, Job Order, and Billing Record rows are
   replaced by one **Eyewear** row.
4. Current and History membership is backend-owned:
   - Current: `estimate_available`, `in_preparation`, `ready_for_pickup`;
   - History: `dispensed`, `estimate_declined`, `estimate_expired`,
     `cancelled`.
5. Payment never changes Current/History membership. Dispensed eyewear with a
   balance remains in History.
6. The list preserves backend order (`activity_at DESC, key ASC`) and does not
   sort or reclassify transactions locally.
7. Draft quotations do not appear. Presented/accepted estimates without a Job
   Order appear as Estimate Available.
8. List and detail navigation use the backend's opaque canonical string key.
   Android never parses an `eyw_` key.
9. Actual Job Order links may open detail with the documented
   `jo_{job_order_id}` alias. Android does not first look up the canonical key.
10. The current API has no mobile notification endpoint. There is therefore no
    notification destination to migrate.
11. Existing message context type `order` represents the retired Order
    capability, not a documented Job Order context. Android must not
    reinterpret an Order ID as a Job Order/Eyewear ID. A future genuine Job
    Order context can use the `jo_` alias after its contract exists.
12. `consultation_at` is displayed as **Consultation** when present. Otherwise,
    `created_at` is displayed as **Created**; it is never mislabeled as a
    consultation.
13. The backend provides no expected-completion value. Android omits expected
    completion and never estimates it from `ready_at`, duration, or local time.
14. All aggregate money is `BigDecimal`, decoded with the existing
    `MoneyValueSerializer`. It never passes through `Double` or `Float`.
15. The backend's conditional section presence controls detail visibility.
    Missing `estimate`, `preparation`, `dispensing`, or `payment_summary`
    objects are omitted from the UI rather than rendered as empty cards.
16. Frame rating remains available for eligible dispensed frame items. It is
    feedback about a released product, not a mutation of estimate,
    preparation, dispensing, or payment state.
17. The new screens are developed alongside the old presentation temporarily.
    Visible navigation changes only after the combined flow reaches parity.
18. After navigation and link sweeps confirm no remaining consumer, obsolete
    Quotation/Job Order/Billing Record screens, ViewModels, routes, and
    presentation tests may be removed. Their backend tables and existing API
    endpoints remain unchanged.
19. Existing data/network/domain layers for the three operational records may
    remain while their six legacy read endpoints remain in the authoritative
    35-route contract. Retiring those endpoints is a separate backend change.
20. The unrelated, currently modified Frame Reservation DTO/repository files
    are outside V12 and must be preserved without alteration.

Approval of this specification confirms these decisions.

## Backend Contract Consumed by Android

### List

```text
GET /api/v1/eyewear
    filter=current|history
    page>=1
    per_page=1..50
```

The standard paginated response provides:

```text
key
description
consultation_at
created_at
progress
payment_status
total_amount
balance_due
activity_at
links
meta
```

Android uses:

```text
EyewearSummary
    key: String
    description: String
    consultationAt: String?
    createdAt: String
    progress: EyewearProgress
    paymentStatus: EyewearPaymentStatus?
    totalAmount: BigDecimal
    balanceDue: BigDecimal?
    activityAt: String
```

`activityAt` is retained for traceability/testing but server order remains
authoritative.

### Detail

```text
GET /api/v1/eyewear/{key}
```

The resource repeats the summary fields and conditionally contains:

```text
estimate
preparation
dispensing
payment_summary
```

Android models each section as nullable:

```text
EyewearDetail
    summary fields
    estimate: EyewearEstimate?
    preparation: EyewearPreparation?
    dispensing: EyewearDispensing?
    paymentSummary: EyewearPaymentSummary?
```

The DTO uses nullable defaults for optional section objects so omitted keys
decode safely. Section contents map to serialization-free domain types at the
repository boundary.

### Exact money

Every aggregate amount uses `BigDecimal`:

```text
summary.total_amount
summary.balance_due
estimate.subtotal
estimate.discount_amount
estimate.total
estimate.items[].unit_price
estimate.items[].amount
preparation.total_amount
preparation.items[].unit_price
preparation.items[].amount
payment_summary.total_amount
payment_summary.amount_paid
payment_summary.balance_due
payment_summary.payments[].amount
```

The backend currently emits quoted decimal strings. The existing serializer
also remains tolerant of JSON numeric values for contract robustness.

### Safe enums

```text
EyewearProgress
    ESTIMATE_AVAILABLE
    IN_PREPARATION
    READY_FOR_PICKUP
    DISPENSED
    ESTIMATE_DECLINED
    ESTIMATE_EXPIRED
    CANCELLED
    UNKNOWN

EyewearPaymentStatus
    BALANCE_DUE
    PAID
    UNKNOWN
```

Unknown progress never implies completion or enables rating. Unknown payment
state displays neutral fallback copy without displaying a fabricated balance.

Nested operational statuses may be retained as raw strings or mapped to
safe enums for presentation policy, but they must not leak internal wording
into headings.

## Patient Experience

### Profile navigation

Replace these visible rows:

```text
Quotations
Job Orders
Billing Records
```

with:

```text
Eyewear
Track estimates, preparation, pickup, and payments
```

The destination is:

```kotlin
@Serializable
data object EyewearList

@Serializable
data class EyewearDetail(val key: String)
```

The bottom navigation remains hidden on both destinations.

### Eyewear list

The screen title is **Eyewear**. A two-option segmented control provides:

```text
Current
History
```

Current is selected initially. Switching filters:

- requests page 1 with the selected backend filter;
- clears stale records from the previously selected filter;
- prevents a late response for an old filter from replacing the current one;
- preserves server ordering;
- supports independent empty/error copy;
- resets pagination state and guards duplicate load-more calls.

Each card displays:

- backend `description`;
- **Consultation** date when `consultation_at` is present, otherwise
  **Created** date from `created_at`;
- a patient-facing progress chip;
- total amount;
- a separate payment chip when `payment_status` exists;
- **Balance due** only when payment status is Balance Due and
  `balance_due` exists.

Progress labels:

| API value | Patient-facing label |
|---|---|
| `estimate_available` | Estimate Available |
| `in_preparation` | In Preparation |
| `ready_for_pickup` | Ready for Pickup |
| `dispensed` | Dispensed |
| `estimate_declined` | Estimate Declined |
| `estimate_expired` | Estimate Expired |
| `cancelled` | Cancelled |
| unknown | Status Unavailable |

Payment labels:

| API value | Patient-facing label |
|---|---|
| `balance_due` | Balance Due |
| `paid` | Paid |
| unknown | Payment Status Unavailable |
| null | no payment chip |

Cards use the canonical key returned in the list to open detail. Empty
descriptions fail safely to **Eyewear transaction**.

### Eyewear detail header

The top area displays:

- eyewear description;
- overall patient-facing progress;
- Consultation/Created date;
- separate payment status when present;
- total amount and applicable balance.

It does not prominently display Quotation, Job Order, or Billing Record names.
Operational reference numbers may appear only as secondary **Reference**
values inside an existing section if visual design needs them.

### Progress tracker

The standard tracker is:

```text
Estimate → Preparation → Ready → Released
```

Standard progress:

| Aggregate progress | Tracker state |
|---|---|
| Estimate Available | Estimate active; future steps inactive |
| In Preparation | Estimate complete; Preparation active |
| Ready for Pickup | Estimate/Preparation complete; Ready active |
| Dispensed | Estimate/Preparation/Ready complete; Released complete |

Detail uses **Released to You** for the final milestone/copy because it is
clearer than the internal dispensing action.

Exceptional History states:

- Estimate Declined or Expired shows Estimate as the reached stage and the
  terminal outcome clearly; later stages remain inactive.
- Cancelled shows a terminal Cancelled status and only evidence-backed earlier
  milestones. It never implies the transaction is continuing.
- Unknown status shows a neutral unavailable state and enables no action.

The tracker is progress context, not an interactive control.

### Estimate section

Show **Estimate** only when `estimate` exists.

Contents:

- **Eyewear Details** item rows;
- description and quantity;
- unit price and line amount;
- subtotal;
- discount when non-zero;
- total;
- valid-until date when present;
- patient-friendly estimate status when useful.

There is no accept, decline, edit, checkout, or order button.

### Preparation section

Show **Preparation** only when `preparation` exists.

Contents:

- current patient-friendly progress;
- **Eyewear Details** item rows;
- quantities and amounts;
- preparation start date when present;
- actual ready date when present.

The API supplies no expected-completion field. No expected-completion row,
estimate, countdown, or locally derived date is shown.

### Dispensing section

Show **Pickup & Release** only when `dispensing` exists.

Contents:

- **Ready for pickup** with actual ready date when available; or
- **Released to You** with actual release date when available.

Do not show an empty Pickup & Release section for estimate-only or
queued/in-progress records.

### Payment Summary section

Show **Payment Summary** only when `payment_summary` exists.

Contents:

- total;
- payments made;
- remaining balance;
- posted payment history with amount, safely humanized method, optional
  reference, and recorded date.

A voided Billing Record is omitted by the backend and therefore produces no
Payment Summary. Android does not infer or expose the voided record.

The section is read-only. It includes no pay, correct, void, refund, or edit
action and never calls a payment mutation.

### Frame rating

For a dispensed aggregate, preparation items with both a Job Order item ID and
`product_variant_id` retain the existing **Rate this frame** action.

The action:

- reuses the existing Frame Rating repository and endpoint;
- is unavailable for every non-dispensed or unknown progress;
- does not change aggregate progress or payment state;
- retains one-to-five-star validation and optional comment behavior;
- moves/reuses rating presentation under the Eyewear feature before obsolete
  Job Order presentation is removed.

## Navigation and Gradual Retirement

### Cutover order

1. Add the Eyewear transport/domain/repository vertical.
2. Build and verify list/detail screens alongside the existing screens.
3. Add `EyewearList` and `EyewearDetail`.
4. Replace the three Profile rows atomically with Eyewear.
5. Route any genuine Job Order link to
   `EyewearDetail("jo_$jobOrderId")`.
6. Verify all current navigation and message/deep-link consumers.
7. Remove obsolete Quotation, Job Order, and Billing Record presentation
   routes/screens/ViewModels only after the combined experience covers their
   patient-facing content and Frame Rating.

### Message contexts

The current API documents only Appointment, retired Order, and Product message
contexts. Therefore:

- Appointment contexts remain unchanged.
- Retired Order contexts must not be routed to Eyewear by treating an Order ID
  as a Job Order ID.
- Unknown/Product contexts keep their current safe behavior.
- If a future `job_order` context is documented, its integer ID can navigate
  through the `jo_` alias without changing the aggregate API.

### Final presentation state

After the verified cleanup:

- Profile contains one Eyewear row;
- only Eyewear list/detail routes are reachable for the combined journey;
- no obsolete screen callback or composable remains in `NavGraph`;
- underlying Quotation, Job Order, Billing Record, and Frame Rating data/API
  behavior remains unchanged;
- the two new routes coexist with the six old backend read routes, so the
  authoritative API and Retrofit allowlist total is 35.

## Scope

### In Scope

- Add the two Eyewear routes to the Android API allowlist; total becomes 35.
- Add Eyewear Retrofit service, DTOs, domain models, repository, DI, and
  mapping.
- Decode all money exactly.
- Add paginated Current/History list state.
- Add isolated detail state with conditional sections.
- Add patient-friendly progress, payment, date, money, and payment-method
  presentation policies.
- Add the four-step non-interactive tracker.
- Replace Profile's three operational rows with Eyewear.
- Preserve Frame Rating for eligible dispensed items.
- Correct the current erroneous assumption that a retired Order message
  context is a Job Order link.
- Remove obsolete presentation navigation/screens only after parity/link
  verification.
- Update current Android context and tests.

### Out of Scope

- Backend code, schema, migrations, tests, or documentation edits.
- Changes to the new aggregate contract.
- Retiring the six existing Quotation/Job Order/Billing Record backend routes.
- Changing clinic Filament navigation or operational record workflows.
- Creating or editing quotations, job orders, billing records, or payments.
- Direct order placement or checkout.
- Estimate acceptance/decline from Android.
- Expected-completion prediction.
- Room persistence or schema changes.
- Home dashboard Eyewear content.
- Search, sort, or filters beyond Current and History.
- New notification or message-context API behavior.
- A patient-facing complaint/remediation workflow.
- New dependencies.

## Tech Stack

- Kotlin 2.3.0 with AGP 9.2.1 built-in Kotlin.
- Jetpack Compose and Material 3.
- Hilt.
- Retrofit, OkHttp, and Kotlinx Serialization.
- `BigDecimal` with existing `MoneyValueSerializer`.
- Navigation Compose type-safe `@Serializable` routes.
- `StateFlow` sealed UI states.
- JUnit 5, MockK, Turbine, coroutines-test, MockWebServer, and Compose UI tests.

No dependency or build-plugin change is required.

## Commands

Run from the repository root in PowerShell:

```powershell
.\gradlew assembleDebug
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew connectedDebugAndroidTest
```

`connectedDebugAndroidTest` is required only when an emulator/device is
available. Every implementation change still ends with:

```powershell
.\gradlew assembleDebug
```

## Project Structure

Expected new production areas:

```text
app/src/main/java/com/eyecare/app/data/remote/api/EyewearApiService.kt
app/src/main/java/com/eyecare/app/data/remote/dto/EyewearDtos.kt
app/src/main/java/com/eyecare/app/data/repository/EyewearRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/Eyewear.kt
app/src/main/java/com/eyecare/app/domain/repository/EyewearRepository.kt
app/src/main/java/com/eyecare/app/di/EyewearModule.kt
app/src/main/java/com/eyecare/app/presentation/eyewear/
```

Expected focused tests:

```text
app/src/test/java/com/eyecare/app/data/remote/dto/EyewearDtosTest.kt
app/src/test/java/com/eyecare/app/data/repository/EyewearRepositoryImplTest.kt
app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearListViewModelTest.kt
app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearDetailViewModelTest.kt
app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearPresentationTest.kt
app/src/androidTest/java/com/eyecare/app/presentation/eyewear/
```

Shared integration areas:

```text
app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt
app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt
app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt
app/src/main/java/com/eyecare/app/presentation/messaging/
app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt
app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt
CONTEXT.md
```

Exact deletion/move files belong in Phase 2 planning and Phase 3 tasks after a
source-reference audit.

## Code Style

DTOs remain serialization-only, domain models remain plain, and mapping stays
at the repository boundary:

```kotlin
@Serializable
data class EyewearSummaryDto(
    val key: String,
    val description: String,
    @SerialName("consultation_at") val consultationAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    val progress: String,
    @SerialName("payment_status") val paymentStatus: String? = null,
    @SerialName("total_amount")
    @Serializable(with = MoneyValueSerializer::class)
    val totalAmount: BigDecimal,
    @SerialName("balance_due")
    @Serializable(with = MoneyValueSerializer::class)
    val balanceDue: BigDecimal? = null,
    @SerialName("activity_at") val activityAt: String,
)

private fun EyewearSummaryDto.toDomain() = EyewearSummary(
    key = key,
    description = description,
    consultationAt = consultationAt,
    createdAt = createdAt,
    progress = EyewearProgress.fromApi(progress),
    paymentStatus = paymentStatus?.let(EyewearPaymentStatus::fromApi),
    totalAmount = totalAmount,
    balanceDue = balanceDue,
    activityAt = activityAt,
)
```

Conventions:

- `Eyewear` is the public feature/package term.
- Transport field names mirror the contract through `@SerialName`.
- Unknown values map to explicit `UNKNOWN`, never the first active state.
- UI state uses sealed interfaces via `StateFlow`.
- List and detail use separate ViewModels.
- Formatting and milestone derivation are pure/testable where practical.
- No clinical or financial response body is logged.

## Testing Strategy

### Contract and repository tests

- Decode list pagination and complete detail.
- Decode every documented partial section combination.
- Prove omitted sections and nullable consultation/balance values are safe.
- Decode quoted and numeric money without precision loss.
- Map every progress/payment status plus unknown.
- Verify Current/History query values, page, and per-page behavior.
- Verify canonical and `jo_` detail keys are passed unchanged.
- Preserve backend order and pagination metadata.
- Verify DTOs never escape the repository boundary.

### ViewModel tests

- Current is the initial filter.
- Current/History switching restarts at page 1.
- Stale responses cannot replace a newly selected filter.
- Initial load, empty, error, retry, append, and append failure are covered.
- Duplicate load-more calls are guarded.
- Detail loads exactly one key and never triggers a list request.
- Detail retry uses the active key.
- Unknown values remain safe and non-actionable.

### Presentation-policy tests

- Every progress and payment label.
- Consultation versus Created date labeling.
- Exact peso formatting.
- Balance visibility.
- Conditional section visibility.
- Normal and exceptional tracker states.
- Payment-method humanization.
- Rating eligibility only for dispensed items with required IDs.

### Compose/navigation tests

- Profile exposes one Eyewear row and none of the three old rows.
- Current/History controls and list states.
- Card content and callbacks.
- Detail full/partial section layouts.
- Progress tracker semantics and terminal outcomes.
- Rating action eligibility.
- Eyewear routes hide bottom navigation.
- Canonical keys and `jo_` aliases reach detail.
- Retired Order message contexts do not open Eyewear.
- Android test APK compiles before old screens are deleted.

### Removal and regression tests

- Route allowlist red proof then green at exactly 35.
- Source sweep verifies no old Profile callbacks/routes/screens remain after
  cleanup.
- Frame Rating focused tests continue passing.
- Appointment/message context behavior remains unchanged.
- Full unit, Android-test compilation, ktlint, lint, and debug assembly pass.

## Boundaries

### Always

- Use the aggregate API instead of joining legacy lists on Android.
- Preserve backend order and filter semantics.
- Use opaque string keys unchanged.
- Use `BigDecimal` for every money value.
- Map DTOs to domain at the repository boundary.
- Keep detail sections conditional.
- Keep progress and payment visually/semantically separate.
- Preserve Frame Rating only under server-compatible dispensed eligibility.
- Keep Eyewear read-only apart from the existing rating capability.
- Use patient-friendly terminology.
- Run focused tests and `.\gradlew assembleDebug` after implementation changes.
- Preserve unrelated user changes and backend documents.

### Ask First

- Add or modify an endpoint.
- Add a dependency or build plugin.
- Change Current/History membership.
- Interpret a new message context as a Job Order.
- Add expected-completion logic.
- Add a patient mutation.
- Retire any existing backend route.
- Remove operational data/domain layers still required elsewhere.
- Persist Eyewear in Room.

### Never

- Join Quotations, Job Orders, and Billing Records client-side.
- Parse or construct canonical `eyw_` identifiers.
- Convert Eyewear money through `Double` or `Float`.
- Reclassify a transaction based on payment status.
- Render absent sections as empty operational cards.
- Call estimate acceptance, Job Order mutation, dispensing, or payment mutation.
- Infer expected completion.
- Present a Billing Record as an invoice, receipt, or BIR document.
- Treat a retired Order ID as a Job Order ID.
- Enable rating for non-dispensed or unknown progress.
- Log patient, financial, clinical, token, or response-body data.

## Success Criteria

### Contract and data

- [ ] Android approves and discovers exactly 35 API routes.
- [ ] Eyewear list/detail use only the aggregate endpoints.
- [ ] Canonical and `jo_` alias detail keys work without parsing.
- [ ] All money remains exact `BigDecimal`.
- [ ] Optional sections and nullable fields decode safely.
- [ ] Unknown statuses fail safely.

### Navigation and list

- [ ] Profile contains one Eyewear row instead of three operational rows.
- [ ] Current is the default filter.
- [ ] Both filters use backend pagination/order.
- [ ] Cards show description, Consultation/Created date, progress, total,
  separate payment status, and applicable balance.
- [ ] Empty/error/retry/load-more states work independently.

### Detail

- [ ] Header and tracker communicate overall progress without controls.
- [ ] Estimate, Preparation, Pickup & Release, and Payment Summary render only
  when their backend section exists.
- [ ] Estimate shows items, quantity, price, discount, and total.
- [ ] Preparation shows eyewear details and actual progress timestamps.
- [ ] No invented expected-completion value appears.
- [ ] Ready and released states use patient-friendly copy.
- [ ] Payment Summary shows total, paid, balance, and posted payments.
- [ ] No estimate/order/payment mutation is reachable.
- [ ] Eligible dispensed frame items retain rating.

### Retirement and quality

- [ ] Visible operational navigation is fully replaced.
- [ ] Genuine Job Order links use the aggregate alias.
- [ ] Retired Order contexts are not misrouted.
- [ ] Old presentation screens/routes are removed only after parity and link
  verification.
- [ ] Underlying backend operational data/routes remain unchanged.
- [ ] `CONTEXT.md` reflects Eyewear and 35 routes.
- [ ] Focused/full unit tests, Android-test compilation, ktlint, lint, and debug
  assembly pass.

## Open Questions

None. The updated backend documents and the confirmed assumptions above resolve
the aggregation, identity, filtering, payment, pagination, section-presence,
deep-link alias, terminology, and read-only behavior required for Phase 1.

## Phase Gate

Phases 1 and 2 were approved by the project owner on 2026-07-29. The Phase 3
task breakdown may be produced for human review.

Do not modify Android production code until the Phase 3 task breakdown is
approved and the project owner gives a separate instruction to proceed.
