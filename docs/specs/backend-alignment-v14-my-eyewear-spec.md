# Backend Alignment V14 — My Eyewear Estimates and Orders

Status: Phases 1–2 approved — 2026-08-03; Phase 3 tasks awaiting approval

## Objective

Replace the retired aggregate Eyewear API integration with one patient-facing
**My Eyewear** destination containing two independent experiences:

1. **Estimates** — read-only quotations that the clinic has presented to the
   patient.
2. **Orders** — confirmed physical eyewear that the clinic is preparing or has
   released to the patient.

The patient journey is:

```text
Estimate presented by clinic
        ↓ clinic confirms it and creates an order
Optical order prepared → Ready for pickup → Released to you
```

This is one navigation destination, not one combined data set. Estimates and
Orders retain independent filters, pagination, loading, empty, and error
states. Android must never join, deduplicate, or reclassify the two paginated
API responses.

The feature remains read-only except for the contract-authorized rating of an
eligible released product item. Patients cannot accept or decline an estimate,
place or modify an order, change fulfillment status, or change payments.

This specification supersedes the aggregate endpoint, aggregate list, opaque
Eyewear key, and combined detail assumptions in
`backend-alignment-v12-spec.md`. Compatible V12 decisions—patient-friendly
language, exact money, accessibility, read-only behavior, and conditional
financial presentation—remain applicable.

## Sources of Truth

1. `docs/API_CONTRACT.md`, especially Quotations, Optical Orders,
   Conversation, Coordinated Breaking Changes, Retired Features, and the
   complete 51-route appendix.
2. `docs/BACKEND_CONTEXT.md`, including the active-link route boundary and
   clinic-controlled quotation/order workflow.
3. Current Android code at Git HEAD plus the user-owned uncommitted backend
   documentation and auth UI changes present during specification.
4. `CONTEXT.md` for project architecture and implemented account-linking
   behavior. Where it conflicts with the updated API documents, the API
   documents are authoritative and `CONTEXT.md` must later be reconciled.
5. This approved specification for Android information architecture,
   terminology, presentation policy, and migration scope.

The backend documents are inputs to Android work and must not be edited as part
of this feature.

## Confirmed Product and Contract Decisions

1. The Profile destination is named **My Eyewear**.
2. The destination uses primary **Estimates** and **Orders** tabs. Each tab has
   its own **Current** and **History** filter.
3. The initial selection is **Estimates → Current**. Selection is UI state and
   is not persisted across process death.
4. Estimates call only `GET /quotations`; Orders call only
   `GET /optical-orders`.
5. Each tab owns independent results, page metadata, refresh/retry state, and
   pagination. Switching tabs does not transform or copy records between
   lists.
6. Current/History membership is entirely backend-owned:
   - Estimates Current: `presented`.
   - Estimates History: `accepted`, `declined`, `expired`.
   - Orders Current: `queued`, `in_progress`, `ready_for_dispensing`.
   - Orders History: `dispensed`, `cancelled`.
7. Draft quotations are never shown.
8. An accepted estimate remains in Estimate History. When its nullable
   `optical_order` reference exists, **View order** opens that Order detail.
9. An Order with a nullable `source_quotation` reference shows **View original
   estimate**. A direct Order simply omits this action.
10. These explicit cross-links do not constitute a client-side join. Android
    passes the referenced public ID directly to the corresponding detail
    route and fetches only that resource.
11. Android preserves backend list order and does not sort, merge, deduplicate,
    or decide that an Estimate should be hidden because an Order exists.
12. The app derives a concise card title only from items already present on
    that record, such as the first description plus “and 2 more.” This is
    presentation formatting, not cross-resource aggregation.
13. All money is decoded directly to `BigDecimal` with the existing
    `MoneyValueSerializer`; it never passes through `Double` or `Float`.
14. Unknown status or payment values map to explicit non-actionable `UNKNOWN`
    states with safe copy.
15. `items[].is_rateable` is server-authoritative. Android never infers rating
    eligibility from status or `product_variant_id`.
16. Rating is one POST upsert: first creation may return 201 and a later
    revision may return 200. Both are successful outcomes.
17. `payment_summary` is optional. When absent, no empty Payment Summary card
    is shown.
18. The backend does not expose payment line history in Optical Orders.
    Android shows only total, paid, balance, due date, overdue state, and
    machine-readable payment status supplied by `payment_summary`.
19. Message contexts use only `optical_order` or `quotation`. The old `order`
    context and old Job Order repository must not remain in the picker.
20. The existing one-file-per-message attachment limit remains backend-owned.
    This migration aligns response fields and context types but does not add a
    multi-attachment UI.
21. No notification API currently exists. There is no notification deep link
    to migrate in this phase.
22. The app has not been deployed, so final implementation may remove obsolete
    compatibility code after a reachability sweep rather than preserve legacy
    behavior.

Approval of this specification confirms these product decisions, including the
initial Estimates tab.

## Backend Contract Consumed by Android

### Estimates list and detail

```text
GET /api/v1/quotations
    filter=current|history
    page>=1
    per_page=1..50

GET /api/v1/quotations/{quotation}
```

The list and detail expose the same patient-safe Quotation shape:

```text
id
quotation_number
status
valid_until
subtotal
discount_amount
total
notes
created_at
presented_at
confirmed_at
optical_order? { id, order_number }
items[] {
    id
    item_type
    description
    quantity
    unit_price
    amount
}
```

Quotation status values are `presented`, `accepted`, `declined`, and `expired`.
`item_type` is `product` or `service`. Every monetary field is a two-decimal
string. A missing or non-owned detail returns 404.

### Orders list and detail

```text
GET /api/v1/optical-orders
    filter=current|history
    page>=1
    per_page=1..50

GET /api/v1/optical-orders/{id}
```

The list and detail expose the same patient-safe Optical Order shape:

```text
id
order_number
status
fulfillment_mode
total_amount
started_at?
ready_at?
dispensed_at?
cancelled_at?
created_at
source_quotation? { id, quotation_number }
items[] {
    id
    description
    quantity
    unit_price
    amount
    product_variant_id?
    is_rateable
    rating? { rating, comment?, created_at }
}
payment_summary? {
    status
    total_amount
    amount_paid
    balance_due
    payment_due_date?
    is_overdue
}
```

Order status values are `queued`, `in_progress`, `ready_for_dispensing`,
`dispensed`, and `cancelled`. Fulfillment mode is `immediate` or `prepared`.
Payment status is `unpaid`, `partially_paid`, `paid`, or `voided`. Order items
are product snapshots; service lines remain on the source Estimate only.

### Rating

```text
POST /api/v1/optical-order-items/{id}/rating
Content-Type: application/json

{
  "rating": 1..5,
  "comment": string|null // max 1000
}
```

The first rating returns 201; a revision returns 200. The response contains the
current rating and `revision_number`. A 404 is intentionally non-specific for
missing, non-owned, or non-rateable items. A 422 may expose
`ORDER_NOT_DISPENSED` or `VALIDATION_ERROR`.

### Messaging alignment

Conversation messages no longer require `conversation_id` in their response
DTO. They expose `sender_id`, `sender_type`, non-null `body`, zero or one
attachment, and optional contexts. Valid context types are only:

```text
optical_order:{public order ID}
quotation:{public quotation ID}
```

Context selection loads the corresponding APIs independently. A tapped
message context navigates to Order detail or Estimate detail by type and ID.
Unknown future context types remain visible as non-clickable generic context
labels rather than being misrouted.

### Retired Android calls

The final implementation must make no request to:

```text
GET  /api/v1/eyewear
GET  /api/v1/eyewear/{key}
GET  /api/v1/job-orders
GET  /api/v1/job-orders/{id}
GET  /api/v1/billing-records
GET  /api/v1/billing-records/{id}
POST /api/v1/job-order-items/{id}/rating
```

The route allowlist must match the authoritative 51-route appendix and reject
these retired routes.

## Patient Experience

### Navigation and top-level state

- Profile **Care & activity** contains one **My Eyewear** row.
- The destination is active-link protected by the existing limited-account
  navigation gate.
- The screen title is **My Eyewear**.
- Primary tabs are **Estimates** and **Orders**.
- Each selected primary tab shows **Current** and **History** controls.
- Each of the four combinations owns an independent empty/error/pagination
  state. A failure in one does not replace content in another.
- Refresh and retry apply only to the selected source/filter.

### Estimate cards

Each Estimate card shows:

- an item-derived description;
- patient label: **Awaiting confirmation**, **Confirmed**, **Declined**,
  **Expired**, or **Status unavailable**;
- estimate reference;
- presented date, falling back to created date without mislabeling it;
- validity date when present;
- total; and
- **View order** only when `optical_order` exists.

The card does not expose accept/decline controls. “Awaiting confirmation” means
the clinic has presented the Estimate and any agreement continues with clinic
personnel.

### Order cards

Each Order card shows:

- an item-derived eyewear description;
- patient label: **Preparing**, **In preparation**, **Ready for pickup**,
  **Released to you**, **Cancelled**, or **Status unavailable**;
- order reference;
- the most relevant actual timestamp without inventing an expected date;
- total amount;
- payment status as a separate label; and
- remaining balance when greater than zero and supplied by the backend.

Recommended status mapping:

| API value | Patient label |
|---|---|
| `queued` | Preparing |
| `in_progress` | In preparation |
| `ready_for_dispensing` | Ready for pickup |
| `dispensed` | Released to you |
| `cancelled` | Cancelled |
| unknown | Status unavailable |

Payment wording is separate from fulfillment wording:

| API value | Patient label |
|---|---|
| `unpaid` | Payment due |
| `partially_paid` | Balance due |
| `paid` | Paid |
| `voided` | Payment voided |
| unknown | Payment status unavailable |

An overdue indicator is driven only by `is_overdue`; it is not encoded into
the payment status enum.

### Estimate detail

Estimate detail shows:

- item-derived title and patient status;
- estimate reference and dates;
- product and service items with quantity, unit price, and amount;
- subtotal, discount, and total;
- notes when present; and
- **View order** when `optical_order` exists.

No preparation, dispensing, or payment card is synthesized from other APIs.

### Order detail

Order detail shows:

- item-derived title, patient status, and order reference;
- a fulfillment tracker: **Preparation → Ready → Released**;
- actual created, started, ready, released, or cancelled timestamps when
  supplied;
- **Eyewear details** with product quantities and prices;
- **Payment summary** only when supplied;
- **View original estimate** only when `source_quotation` exists; and
- rating/revision actions only on items where `is_rateable == true`.

Queued and in-progress both occupy the Preparation milestone. Cancelled and
unknown states use an exceptional status treatment and must not falsely mark
future milestones complete. Immediate fulfillment still follows the
server-provided timestamps and status.

The order screen does not fetch the source Estimate to populate its body. The
cross-link opens a separate Estimate detail request only after user action.

### Empty and error copy

Empty states must explain the selected collection without implying a technical
failure, for example:

- “No current estimates”
- “No estimate history yet”
- “No current eyewear orders”
- “No order history yet”

Errors are scoped to the selected collection and provide Retry. A load-more
failure retains already-loaded cards and offers a localized retry.

## Scope

### In Scope

- Replace the aggregate Eyewear DTO/API/repository/domain assumptions.
- Align the Quotation integration to the current flat contract, filters,
  pagination, links, and exact money fields.
- Introduce an Optical Order API, DTO, repository, domain model, and DI binding.
- Rework My Eyewear list and detail presentation into independent Estimate and
  Order surfaces.
- Preserve eligible product rating with the new route and upsert semantics.
- Migrate message models, context picker, context submission, and navigation
  from old Job Orders to Quotations and Optical Orders.
- Update type-safe navigation routes and Profile copy.
- Remove obsolete aggregate, Job Order, and Billing Record consumers after a
  source-reference audit.
- Align the exact route allowlist to 51 routes and explicitly reject retired
  routes.
- Update tests and `CONTEXT.md` after implementation.

### Out of Scope

- Backend changes or legacy backend compatibility.
- Estimate acceptance/decline in Android.
- Creating, editing, cancelling, readying, or dispensing an Optical Order.
- Creating or changing bills and payments.
- Showing payment line history that the API does not provide.
- Joining Estimate and Order pages into a lifecycle record.
- Notification endpoints or deep links that do not exist in the contract.
- Clinic operational screens or terminology changes in the backend.
- Changes to auth, registration, contact management, account linking,
  appointments, frames, reservations, or prescriptions except necessary
  regression fixes caused by shared navigation/network types.
- Persisting Estimates, Orders, or messages in Room.
- New libraries or build plugins.

## Tech Stack

- Kotlin 2.3.0 with AGP 9.2.1 built-in Kotlin.
- Jetpack Compose and Material 3.
- Hilt.
- Retrofit, OkHttp, and Kotlinx Serialization.
- `BigDecimal` with `MoneyValueSerializer`.
- Type-safe Navigation Compose `@Serializable` routes.
- Sealed UI state via `StateFlow`.
- JUnit 5, MockK, Turbine, coroutines-test, MockWebServer, and existing Compose
  test infrastructure.

No dependency or build-plugin change is required.

## Commands

Run from the repository root in PowerShell:

```powershell
.\gradlew ktlintFormat
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

Run focused unit tests during each increment. Every implementation change must
end with `.\gradlew assembleDebug` as required by `AGENTS.md`. Instrumented
tests may be compiled/run when the applicable task and environment support
them.

## Project Structure

Expected production areas after planning:

```text
app/src/main/java/com/eyecare/app/data/remote/api/
    QuotationApiService.kt
    OpticalOrderApiService.kt
    ConversationApiService.kt
app/src/main/java/com/eyecare/app/data/remote/dto/
    QuotationDtos.kt
    OpticalOrderDtos.kt
    MessageDtos.kt
app/src/main/java/com/eyecare/app/data/repository/
    QuotationRepositoryImpl.kt
    OpticalOrderRepositoryImpl.kt
    ConversationRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/
    Quotation.kt
    OpticalOrder.kt
    Message.kt
app/src/main/java/com/eyecare/app/domain/repository/
    QuotationRepository.kt
    OpticalOrderRepository.kt
app/src/main/java/com/eyecare/app/di/
    QuotationModule.kt
    OpticalOrderModule.kt
app/src/main/java/com/eyecare/app/presentation/eyewear/
    MyEyewearScreen.kt
    MyEyewearViewModel.kt
    EstimateDetailScreen.kt
    EstimateDetailViewModel.kt
    OpticalOrderDetailScreen.kt
    OpticalOrderDetailViewModel.kt
    FrameRatingDialog.kt
    FrameRatingViewModel.kt
    EyewearPresentation.kt
app/src/main/java/com/eyecare/app/presentation/messaging/
app/src/main/java/com/eyecare/app/presentation/navigation/
app/src/main/java/com/eyecare/app/presentation/profile/
```

Expected focused tests mirror the DTO, repository, ViewModel, presentation,
messaging, route-governance, and navigation areas above. Exact file additions,
renames, reuse, and deletions belong in Phase 2 planning after a complete
reference audit.

## Code Style

DTOs remain serialization-only, domain models remain plain Kotlin, and mapping
occurs at the repository boundary:

```kotlin
@Serializable
data class OpticalOrderItemDto(
    val id: Int,
    val description: String,
    val quantity: Int,
    @SerialName("unit_price")
    @Serializable(with = MoneyValueSerializer::class)
    val unitPrice: BigDecimal,
    @Serializable(with = MoneyValueSerializer::class)
    val amount: BigDecimal,
    @SerialName("product_variant_id") val productVariantId: Int? = null,
    @SerialName("is_rateable") val isRateable: Boolean,
    val rating: RatingSummaryDto? = null,
)

private fun OpticalOrderItemDto.toDomain() = OpticalOrderItem(
    id = id,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    amount = amount,
    productVariantId = productVariantId,
    isRateable = isRateable,
    rating = rating?.toDomain(),
)
```

Conventions:

- Public feature language is My Eyewear, Estimates, Orders, Preparation,
  Ready for pickup, Released to you, and Payment summary.
- Internal `JobOrder` terminology may appear only in historical deletion
  context, never as new patient-facing or domain naming.
- Transport names mirror the contract via `@SerialName`.
- Unknown enum values map to explicit `UNKNOWN`.
- UI state uses sealed interfaces and `StateFlow`, never LiveData.
- Each source/filter is keyed explicitly so a stale response cannot replace a
  newly selected view.
- Reusable label, tracker, date, card-title, and money logic is pure/testable.
- DTOs never escape the repository boundary.
- No patient, financial, message, token, or response-body data is logged.

## Testing Strategy

### Contract and repository tests

- Decode complete and nullable Quotation and Optical Order fixtures.
- Decode exact quoted money without precision loss.
- Map every documented quotation, order, payment, fulfillment, sender, and
  context value plus unknown values.
- Verify both list APIs send `filter`, `page`, and `per_page` correctly.
- Preserve backend order and pagination metadata.
- Verify detail IDs are passed unchanged and ownership-safe 404s surface as
  generic not-found errors.
- Verify rating request fields and both 201-create and 200-revision success.
- Verify `is_rateable` passes through unchanged.
- Verify Message decoding without `conversation_id`, with `sender_type`, and
  with zero or one attachment.

### ViewModel tests

- Estimates Current is the initial selection.
- All four source/filter combinations load independently.
- Switching source or filter never merges records or leaks pagination state.
- Stale responses cannot replace the active source/filter.
- Initial loading, content, empty, retryable error, append, append error, and
  duplicate-load guards are covered per collection.
- Estimate and Order detail load exactly one typed ID.
- Cross-link callbacks preserve resource type and ID.
- Rating success replaces the current item rating for either 201 or 200;
  validation errors remain actionable without discarding detail content.

### Presentation-policy tests

- All patient status and payment labels, including unknown values.
- Item-derived title for empty, one-item, and multi-item records.
- Presented-date fallback is labeled accurately.
- Exact peso formatting and remaining-balance visibility.
- Order tracker for every normal and exceptional state.
- Optional source Estimate, linked Order, Payment Summary, notes, due date, and
  rating controls appear only when allowed.
- Rating visibility follows `is_rateable` only.

### Messaging and navigation tests

- Profile exposes one **My Eyewear** row.
- My Eyewear opens Estimates and Orders without exposing old operational
  destinations.
- Estimate/Order details use typed integer IDs rather than aggregate keys.
- Linked Estimate ↔ Order actions open the correct detail type.
- Message context picker loads Quotations and Optical Orders independently.
- Context send payloads use `quotation` and `optical_order` only.
- Context cards navigate by type; unknown types are safe and non-clickable.
- Limited accounts remain blocked before any active-link request is made.

### Removal and regression tests

- The route allowlist matches exactly 51 authoritative routes.
- Explicit tests reject `/eyewear`, `/job-orders`, `/billing-records`, and the
  old rating route.
- A source sweep proves no Retrofit service, repository, ViewModel, screen,
  route, DI module, or message picker still calls retired endpoints.
- Full unit tests, formatting, lint, and debug assembly pass.
- Existing auth/account-linking and appointment behavior remain unchanged.

## Boundaries

### Always

- Keep Estimates and Orders as independent paginated resources.
- Use backend-owned filters, ordering, links, payment state, and rateability.
- Use typed public integer IDs unchanged for detail and context navigation.
- Use `BigDecimal` for all money.
- Map DTOs to domain at repository boundaries.
- Keep fulfillment and payment state visually and semantically separate.
- Render optional content only when the source resource supplies it.
- Keep the feature read-only except for eligible rating upserts.
- Use patient-friendly language and accessible semantics.
- Preserve unrelated user changes and backend documentation.
- Run focused tests and `.\gradlew assembleDebug` after implementation edits.

### Ask First

- Add, remove, or change a backend endpoint or payload.
- Add a dependency or build plugin.
- Change backend-defined Current/History membership or order.
- Introduce an Overview that fetches and presents both paginated lists at once.
- Add client-side joining, deduplication, or automatic lifecycle routing.
- Add an Estimate, Order, fulfillment, or payment mutation.
- Infer expected completion or payment history.
- Persist Estimates, Orders, messages, tokens, or health/financial data.
- Expand attachment behavior beyond the documented single optional file.

### Never

- Join or deduplicate Quotations and Optical Orders client-side.
- Call retired Eyewear, Job Order, Billing Record, or rating routes.
- Hide an accepted Estimate because a linked Order exists.
- Treat a Quotation ID as an Order ID or vice versa.
- Convert money through `Double` or `Float`.
- Infer rateability from order status or product variant.
- Render absent Payment Summary or source links as empty sections.
- Let patients accept/decline Estimates or mutate Orders/payments.
- Present expected completion when the backend supplies none.
- Use Gson, LiveData, or `org.jetbrains.kotlin.android`.
- Store tokens, clinical, financial, Estimate, Order, or message data in Room.
- Log sensitive request or response data.

## Success Criteria

### Contract and data

- [ ] Android calls only current Quotation, Optical Order, rating, and
  Conversation routes for this feature.
- [ ] Estimates and Orders have independent exact pagination/filter state.
- [ ] All money remains exact `BigDecimal`.
- [ ] Nullable fields and unknown enum values decode safely.
- [ ] Rating follows server `is_rateable` and accepts both upsert success codes.
- [ ] Message DTOs and contexts match the current contract.
- [ ] The allowlist matches exactly 51 routes and rejects all retired routes.

### My Eyewear list

- [ ] Profile and screen title use **My Eyewear**.
- [ ] Primary Estimates/Orders and secondary Current/History controls are
  accessible and independently stateful.
- [ ] Initial state is Estimates Current.
- [ ] Cards use patient-friendly status, exact totals, accurate dates, and
  separate payment state.
- [ ] No cards are joined, deduplicated, or locally reclassified.
- [ ] Empty, error, retry, refresh, and load-more behavior is isolated per
  collection.

### Details and cross-links

- [ ] Estimate detail shows only Estimate data and no mutation controls.
- [ ] Order detail shows fulfillment, product items, optional payment summary,
  and eligible rating.
- [ ] Order progress is Preparation → Ready → Released with safe cancelled and
  unknown handling.
- [ ] Accepted Estimates link to Orders only when `optical_order` exists.
- [ ] Orders link to original Estimates only when `source_quotation` exists.
- [ ] Cross-links fetch a typed detail after user action and never pre-join
  resources.
- [ ] No expected completion or unavailable payment history is invented.

### Retirement and quality

- [ ] No reachable code calls `/eyewear`, `/job-orders`, `/billing-records`, or
  `/job-order-items/{id}/rating`.
- [ ] Obsolete aggregate/Job Order/Billing presentation and data code is removed
  after reference verification.
- [ ] Messaging uses Quotation and Optical Order contexts end-to-end.
- [ ] `CONTEXT.md` documents the final My Eyewear behavior and 51-route state.
- [ ] Focused/full unit tests, formatting, lint, and debug assembly pass.

## Open Questions

None blocking. Approval confirms the recommended initial
**Estimates → Current** selection. Any later request for automatic tab choice,
a mixed Overview, or a combined lifecycle card would reopen the no-join
information architecture decision and require a new contract/design review.

## Phase Gate

This is the Phase 1 specification. Android production code must not change
until this specification is approved and Phase 2 produces an approved
implementation plan. After Phase 2 approval, Phase 3 will produce the
dependency-ordered task list and pause again before implementation.
