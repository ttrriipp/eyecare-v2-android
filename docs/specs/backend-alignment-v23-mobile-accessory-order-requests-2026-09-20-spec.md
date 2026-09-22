# Spec: Backend Alignment v23 — Mobile Accessory Order Requests

Status: Approved — 2026-09-20; implementation awaits one backend response gate

## Objective

Build a linked-patient Android flow for browsing clinic accessories, collecting
multiple accessory variants in a client-side cart, submitting a non-binding
Order Request, paying an accepted request in full through GCash, uploading one
payment proof, and tracking the resulting Optical Order through clinic pickup.

An Order Request is not a completed purchase and does not reserve stock. Staff
remain authoritative for accepting or rejecting the request, confirming any
discount, verifying payment, and advancing fulfillment.

The feature is a commerce journey, not a clinical prescription journey. It
must not be placed inside prescriptions, send `placement=prescription`, infer
prescription compatibility, or classify accessories as prescription-required.

### User outcomes

1. A linked patient can browse and search active, orderable accessories without
   seeing exact stock, expiry, cost, or internal storage data.
2. The patient can choose variants, maintain quantities in a local cart, and
   submit 1–20 distinct variants with quantities of 1–5 each.
3. The patient understands that submission requests clinic review, does not
   reserve stock, and does not guarantee a requested discount.
4. The patient can inspect pending and historical requests, cancel a pending
   request, and open the resulting Optical Order after acceptance.
5. An accepted order shows the server-provided full-balance GCash instructions
   and deadline only while they are available.
6. The patient can upload one valid proof and understand that staff review is
   still required.
7. The patient can track payment and fulfillment through pickup and can rate an
   item only when the server returns `is_rateable: true`.

## Feasibility Conclusion

The Android feature is feasible with the existing Kotlin, Compose, Retrofit,
Kotlinx Serialization, Hilt, StateFlow, and MockWebServer stack. Existing
Optical Order listing, detail, pagination, rating, active-link navigation, image
URL handling, API error decoding, and multipart upload patterns provide useful
foundations. No new dependency, Room table, backend route, or worker is
expected.

The stable wire shapes are now defined in this specification. Implementation
must not begin until the remaining backend response gap under **Open Questions
and Contract Gates** is resolved and the two product decisions are approved.

## Sources of Truth

1. `docs/API_CONTRACT.md`, especially §13b Accessories and Order Requests and
   §14 Optical Orders.
2. `docs/BACKEND_CONTEXT.md`, especially the commerce data model, mobile route
   list, throttles, workflow errors, and payment-proof privacy boundary.
3. `CONTEXT.md`, `PRODUCT.md`, and `DESIGN.md` for Android architecture,
   product, and visual conventions.
4. The existing Android implementation under `data/`, `domain/`,
   `presentation/eyewear/`, `presentation/navigation/`, and
   `presentation/common/`.

If this specification conflicts with the two backend contract documents, the
backend documents win and this specification must be updated before code.

## Assumptions and Scope Decisions

1. Phase 1 produces this specification only. Plan, tasks, and implementation
   require human approval of the spec and resolution of the contract gates.
2. **Accessories** is an active-link-only destination opened from Home. It is
   not added to Prescriptions or to a prescription detail screen.
3. The Accessories catalog owns top-app-bar entry points to the cart and the
   patient's accessory request list. A successful submission opens the new
   request detail.
4. The existing **My Orders** and Optical Order detail surfaces remain the
   committed-order tracker. An accepted request links to its resulting Optical
   Order instead of introducing a second fulfillment tracker.
5. The cart exists only on the device for the current app session. A
   graph-scoped ViewModel/store may preserve it through ordinary navigation and
   configuration recreation, but it is not synced, stored in Room, or sent to
   the server before submission.
6. Product and variant prices shown in the cart are estimates from the latest
   catalog response. Android never sends a price, discount amount, subtotal, or
   total. The submitted request response is authoritative.
7. Removing an unavailable variant or changing a quantity requires explicit
   patient action. Android does not silently substitute variants or quantities
   after a server rejection.
8. GCash details are never hardcoded. Payment UI is rendered only from the
   nullable server `payment_instructions` object.
9. Staff request decisions, payment-proof review, inventory commitment,
   billing, notification delivery, and backend scheduling are outside Android
   scope.
10. The client does not poll continuously. Pull-to-refresh, refresh-on-resume,
    and post-mutation refresh reconcile state. A visible payment countdown may
    be derived from `payment_expires_at`, but reaching zero triggers an
    authoritative refresh rather than a local status mutation.
11. This phase adds no offline catalog cache. Cart state contains only public
    catalog identifiers, display snapshots, and quantities; it contains no
    token or health data.
12. Brand and Category controls are deferred for MVP because no mobile filter
    discovery endpoint exposes their IDs and labels. Android must not derive
    IDs from names returned by paginated products.
13. Extra Laravel paginator metadata is optional and ignored. Android maps only
    `links` and the documented `meta` fields with `ignoreUnknownKeys = true`.

## Authoritative API Surface

All routes use the configured `/api/v1/` Retrofit base URL, authenticated
patient account, and active Patient link.

| Capability | Method and route | Android behavior |
|---|---|---|
| Browse accessories | `GET /accessories` | Server-side query, sorting, filtering, and pagination |
| Accessory detail | `GET /accessories/{id}` | Refresh product/variant availability before adding |
| Request list | `GET /accessory-order-requests?filter=current|history` | Independent current/history pagination |
| Submit request | `POST /accessory-order-requests` | Send discount declaration and variant IDs/quantities only |
| Request detail | `GET /accessory-order-requests/{id}` | Owned request and optional accepted-order summary |
| Cancel request | `POST /accessory-order-requests/{id}/cancel` | Pending only; idempotent response |
| Order list | `GET /optical-orders?filter=current|history` | Extend existing implementation for new payment states |
| Order detail | `GET /optical-orders/{id}` | Payment, proof, fulfillment, and item-rating source of truth |
| Upload proof | `POST /optical-orders/{id}/payment-proof` | Multipart, one proof, no replacement |
| Rate item | `POST /optical-order-items/{id}/rating` | Existing upsert, gated by `is_rateable` |

Android must never send the retired `placement` query parameter.

## Stable Wire Contracts

### Accessory list and detail

The list response uses this stable shape:

```json
{
  "data": [
    {
      "id": 42,
      "name": "Daily Care Kit",
      "slug": "daily-care-kit",
      "description": "Everyday lens-care accessory.",
      "brand": "Acme",
      "category": "Lens Care",
      "images": ["catalog/daily-care-kit.jpg"],
      "average_rating": 4.5,
      "rating_count": 12,
      "variants": [
        {
          "id": 101,
          "name": "90 mL",
          "price": "350.00",
          "compare_at_price": null,
          "attributes": {
            "volume_ml": 90,
            "package_size": "90 mL"
          },
          "images": ["variants/daily-care-90ml.jpg"],
          "availability": "available"
        }
      ]
    }
  ],
  "links": {
    "first": "...",
    "last": "...",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "last_page": 1,
    "per_page": 15,
    "total": 1
  }
}
```

`GET /accessories/{id}` wraps the same product object in `data`.
`description`, `brand`, `category`, `compare_at_price`, and `average_rating`
may be null. `images` is always an array and uses `[]` when empty.
`attributes` is always a JSON object and uses `{}` when empty; its values may
be heterogeneous JSON primitives, so the DTO must not model it as
`Map<String, String>`. Prices are two-decimal strings. Availability is
`available`, `low_stock`, or `unavailable`.

Laravel may return additional paginator metadata. Those keys are additive and
ignorable; absence or presence must not affect DTO decoding.

### Order Request resources

List uses the standard paginated envelope. Create (`201`), detail (`200`), and
cancel (`200`) wrap the same request object in `data`:

```json
{
  "data": {
    "id": 10,
    "request_number": "ORQ-2026-000001",
    "status": "pending",
    "subtotal_amount": "700.00",
    "requested_discount_type": "none",
    "resolved_by": null,
    "resolved_at": null,
    "items": [
      {
        "id": 501,
        "product_variant_id": 101,
        "description": "Daily Care Kit — 90 mL",
        "quantity": 2,
        "unit_price": "350.00",
        "amount": "700.00",
        "item_kind": "accessory",
        "item_snapshot": {
          "product_name": "Daily Care Kit",
          "variant_name": "90 mL",
          "attributes": {
            "volume_ml": 90,
            "package_size": "90 mL"
          }
        }
      }
    ],
    "rejection_reason": null,
    "cancelled_at": null,
    "created_at": "2026-09-20T10:00:00+08:00",
    "order": null
  }
}
```

Accepted requests replace `order: null` with:

```json
{
  "id": 55,
  "order_number": "ORD-2026-000055",
  "status": "pending_payment",
  "discount_amount": "0.00",
  "total_amount": "700.00",
  "payment_expires_at": "2026-09-20T10:30:00+08:00"
}
```

`product_variant_id` and `item_snapshot` are required fields in the stable
mobile resource. The backend currently stores but does not return them; that
controller/resource change is the remaining implementation gate.

### Payment instructions and proof response

`payment_instructions` is either null or:

```json
{
  "method": "gcash",
  "clinic_account_name": "Padilla Optical Clinic",
  "clinic_account_number": "09XXXXXXXXX",
  "amount": "700.00",
  "order_reference": "ORD-2026-000055",
  "payment_expires_at": "2026-09-20T10:30:00+08:00"
}
```

`amount` is the exact current balance due, not the request subtotal.
`order_reference` is the Optical Order number. `payment_expires_at` may be null
at the schema level. The object is present only for `pending_payment` while the
GCash deployment configuration is complete.

Both the first proof upload (`201`) and idempotent retry (`200`) return:

```json
{
  "data": {
    "id": 900,
    "status": "pending",
    "sender_name": "Ana Reyes",
    "reference_number": "GCASH-12345",
    "created_at": "2026-09-20T10:12:00+08:00"
  }
}
```

Proof status is `pending`, `accepted`, or `rejected`. The response never embeds
the parent order or exposes file/review metadata. Android refreshes
`GET /optical-orders/{id}` after mapping the proof response.

Payment-proof throttling returns HTTP `429`, code
`PAYMENT_PROOF_RATE_LIMIT_REACHED`, a patient-safe message, and
`retry_after_seconds`; the standard `Retry-After` header is also present and is
the existing repository's retry-timing source. The limit is five attempts per
account per minute.

## Catalog Requirements

### Query state

The catalog supports:

- `search`, trimmed and limited to 100 characters;
- `sort`: `name`, `newest`, `rating`, or `most_rated`;
- `minimum_rating`: integer 1–5;
- `rated`: `all`, `rated`, or `unrated`;
- `page` >= 1; and
- `per_page` between 1 and 50.

Search is debounced and starts a new page-1 generation. Filter or sort changes
also reset pagination. An older response must not overwrite a newer query.
Append failure keeps loaded products and exposes an append retry. Refresh keeps
content visible where possible.

Brand and Category controls are not displayed in the MVP. A later phase may add
them only after a dedicated discovery contract such as
`GET /api/v1/accessories/filters` is implemented and documented.

### Product presentation

Catalog and detail surfaces may display only documented patient-safe fields:

- product name, description, brand/category labels, and images;
- average rating and rating count;
- active variants, variant name/attributes/images;
- decimal-string price and nullable compare-at price; and
- availability: `available`, `low_stock`, or `unavailable`.

An unavailable variant remains visible when returned but cannot be added to the
cart. The UI never displays or derives exact stock, lot quantities, expiry
dates, cost price, internal paths, or prescription compatibility. Relative
image paths use the existing `buildImageUrl` boundary.

Unknown availability values fail closed as non-orderable while remaining
displayable with neutral status copy.

## Cart and Request Submission

### Cart invariants

- Key entries by `product_variant_id`, so one variant appears at most once.
- Permit 1–20 distinct variants.
- Permit quantity 1–5 per variant.
- Adding an existing variant increments it only up to 5.
- Removing an item is explicit; decrementing from 1 requires removal.
- Disable submission while empty, invalid, or already submitting.
- Prevent duplicate submit calls with a single-flight mutation.
- Show a clearly labeled estimated subtotal from catalog snapshots for
  convenience only.
- Do not treat cart presence as stock reservation.

The cart should retain the minimum display snapshot needed for a stable cart
row: product and variant IDs, product/variant names, image path, latest price,
availability, and quantity. Submission maps this state to only:

```json
{
  "requested_discount_type": "none",
  "items": [
    {"product_variant_id": 42, "quantity": 2}
  ]
}
```

### Discount declaration

The review screen offers `none`, `senior_citizen`, and `pwd`. Copy must say the
selection is a request for staff review and is not guaranteed. Android does not
calculate a senior/PWD discount or ask for discount proof in this flow.

### Submission result

On `201`, clear the cart only after the response maps successfully, then open
the authoritative request detail. The success surface says the clinic still
needs to accept or reject the request and that stock was not reserved by
submission.

On failure, preserve the cart and discount selection. For
`ACCESSORY_NOT_ORDERABLE`, keep all rows, identify a field-specific item when
the Laravel error envelope supplies one, and ask the patient to review the
cart. For `ACTIVE_ORDER_REQUEST_EXISTS`, offer navigation to current requests.

## Order Request Experience

### Status model

The domain represents `pending`, `accepted`, `rejected`, `cancelled`, and an
unknown fallback. Available action policy is derived from the server status:

| Status | Primary presentation | Patient action |
|---|---|---|
| `pending` | Awaiting clinic review; non-binding | Cancel request |
| `accepted` | Request accepted; show resulting order summary | View Optical Order |
| `rejected` | Terminal; show patient-visible reason when present | None |
| `cancelled` | Terminal cancellation | None |
| unknown | Neutral unsupported state | Refresh only |

The list has Current and History filters backed by the corresponding server
filter. It does not merge or repartition results locally. Request detail shows
the immutable item snapshots and subtotal returned by the server, not live cart
or catalog values.

Cancellation uses a confirmation dialog, is single-flight, and replaces the
detail with the returned authoritative cancelled resource. A successful retry
of an already cancelled request remains a success. An
`ORDER_REQUEST_NOT_ACTIONABLE` error triggers detail refresh and removes the
cancel action when the refreshed state is terminal.

## Optical Order, Payment, and Pickup

### Status model

Extend `OpticalOrderStatus` with:

- `PENDING_PAYMENT` for an accepted request awaiting proof;
- `PAYMENT_REVIEW` after proof upload;
- the existing `QUEUED`, `IN_PROGRESS`, `READY_FOR_DISPENSING`, `DISPENSED`,
  and `CANCELLED`; and
- `UNKNOWN` as the fail-closed fallback.

Patient-facing progression is:

```text
Awaiting payment -> Payment under review -> Queued -> Preparing
-> Ready for pickup -> Picked up
```

Cancelled is terminal and is not rendered as a completed tracker step.

### Pending payment

When status is `pending_payment`, render payment UI only if
`payment_instructions` is non-null. The screen shows its method,
`clinic_account_name`, `clinic_account_number`, exact balance-due `amount`,
`order_reference`, and nullable deadline. It must not manufacture clinic
account details or infer a payable amount from a cart snapshot.

If instructions are null, show that payment instructions are currently
unavailable and allow refresh; do not show an upload form whose destination
details the patient cannot verify.

The deadline is an ISO 8601 instant. A countdown is display guidance only. At
deadline, disable a new upload attempt, refresh the order, and respect the
server result. `PAYMENT_WINDOW_EXPIRED` follows the same reconciliation path.

### Proof selection and upload

Use the Android system document/photo picker and accept only JPG/JPEG/PNG. The
client validates MIME type, readable size <= 10 MB, and image bounds <= 8,000 by
8,000 before upload, while treating backend validation as authoritative.

The multipart request contains exactly:

- `proof` as the selected image part;
- `sender_name` as trimmed text; and
- `reference_number` as trimmed text.

Both text fields are required and limited to 100 characters. Upload is
single-flight and exposes progress/loading without logging the URI or values.
The content URI permission is used only as needed for the request; no proof is
copied into Room or app-managed long-term storage.

Both `201` first submission and `200` idempotent retry are success. The returned
proof summary replaces local form state, the order is refreshed, and the UI
explains that payment is under staff review. Android never offers Replace,
Retry with another file, or Delete for an existing proof. A rejected proof is
shown with its patient-visible reason and no resubmission action in MVP.

### Payment review and fulfillment

`payment_review` never auto-expires on Android. Show staff-review guidance and
no countdown. `queued`, `in_progress`, and `ready_for_dispensing` reuse the
existing tracker with updated labels and status ordering. `dispensed` enables
rating only per item capability. `cancelled` shows terminal cancellation and
no payment or pickup action.

## Ratings

Catalog aggregates and filters are read-only indicators. A catalog or cart
item never receives a rating action.

Optical Order detail offers create/update rating only when the individual item
has `is_rateable: true`. The existing rating endpoint and dialog remain the
write path. The action does not infer eligibility solely from `dispensed`, and
it remains hidden or disabled when the backend capability is false.

## Error and Access Handling

All repository methods use the shared API error decoder so presentation can
handle HTTP status, stable code, message, and Laravel field errors without
parsing raw response text.

| Condition | Required Android response |
|---|---|
| `401` | Existing auth interceptor/session logout flow |
| `403` / `ACTIVE_PATIENT_LINK_REQUIRED` | Route to the existing link hub while preserving destination intent |
| `404` | Patient-safe not-found state; never reveal ownership |
| Laravel validation `422` | Field-level feedback when present; safe summary otherwise |
| `ACTIVE_ORDER_REQUEST_EXISTS` | Preserve cart and offer current request |
| `ACCESSORY_NOT_ORDERABLE` | Preserve cart and request item review |
| `ORDER_REQUEST_NOT_ACTIONABLE` | Refresh request and recompute actions |
| `PAYMENT_WINDOW_EXPIRED` | Disable upload and refresh order |
| `ORDER_NOT_AWAITING_PAYMENT` | Refresh order; remove stale upload UI |
| `PAYMENT_PROOF_RATE_LIMIT_REACHED` / `429` | Preserve form and use `Retry-After` for retry-later guidance |
| Unknown status/code | Fail closed; no mutation action based on guessed state |

Accessory routes and notification destinations must be added to the existing
active-link route policy and pending-intent restoration. The app does not call
any accessory endpoint for a known limited account.

## Navigation and Ownership

Proposed route ownership:

```text
Home
└── Accessories (active-link required)
    ├── AccessoryDetail(productId)
    ├── AccessoryCart
    └── AccessoryOrderRequests
        └── AccessoryOrderRequestDetail(requestId)
            └── OpticalOrderDetail(orderId)

Profile/Home existing entry
└── MyOrders
    └── OpticalOrderDetail(orderId)
        └── Item rating dialog
```

The catalog/cart/request surfaces live under a new
`presentation/accessories/` feature package. Committed fulfillment remains
under `presentation/eyewear/` until a broader naming migration is explicitly
approved. No accessory route is nested below `PrescriptionList` or
`PrescriptionDetail`.

## Domain and Interface Direction

Transport DTOs keep backend snake_case through `@SerialName`; domain models use
idiomatic Kotlin names and no serialization annotations. Decimal money maps to
`BigDecimal` through the existing `MoneyValueSerializer`. Timestamps stay as
contract strings at the domain boundary unless presentation needs an instant
for countdown/formatting, in which case parsing must retain the supplied
offset.

Expected intent-oriented repository operations include:

```kotlin
suspend fun getAccessories(query: AccessoryQuery): Result<PaginatedResult<Accessory>>
suspend fun getAccessory(id: Int): Result<Accessory>
suspend fun getOrderRequests(filter: OrderRequestFilter, page: Int): Result<PaginatedResult<AccessoryOrderRequest>>
suspend fun getOrderRequest(id: Int): Result<AccessoryOrderRequest>
suspend fun submitOrderRequest(command: SubmitAccessoryOrderRequest): Result<AccessoryOrderRequest>
suspend fun cancelOrderRequest(id: Int): Result<AccessoryOrderRequest>
suspend fun uploadPaymentProof(orderId: Int, proof: PaymentProofUpload): Result<PaymentProofSummary>
```

`AccessoryQuery`, `SubmitAccessoryOrderRequest`, and `PaymentProofUpload` are
domain/input contracts, not Retrofit DTOs. The repository boundary maps all
responses to domain models before presentation receives them.

## Tech Stack

- Kotlin 2.3.0 with AGP 9.2.1 built-in Kotlin support
- Jetpack Compose and Material 3
- Hilt
- Retrofit and OkHttp multipart
- Kotlinx Serialization
- StateFlow and Coroutines
- JUnit 5, MockK, Turbine, MockWebServer, and Compose UI tests

No new dependency is expected.

## Commands

Run from the repository root in PowerShell with Android Studio's JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

```powershell
# Focused tests during implementation
.\gradlew testDebugUnitTest --tests "*Accessory*" --tests "*OpticalOrder*" --tests "*PaymentProof*"

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

Expected implementation areas:

```text
app/src/main/java/com/eyecare/app/
  data/remote/api/                   accessory requests and proof upload
  data/remote/dto/                   exact wire DTOs after contract closure
  data/repository/                   DTO-to-domain mapping and safe API errors
  domain/model/                      accessory, cart, request, payment models
  domain/repository/                 intent-oriented repository interfaces
  di/                                Retrofit/repository bindings
  presentation/accessories/          catalog, detail, cart, request list/detail
  presentation/eyewear/              payment states, proof upload, tracker
  presentation/navigation/           routes, active-link intents and restoration
  presentation/home/                 Accessories entry point
  presentation/common/               only genuinely reusable focused controls

app/src/test/                         DTO, API, repository, policy, ViewModel tests
app/src/androidTest/                  critical Compose/accessibility tests
docs/specs/                           this spec and later approved plan/tasks
```

## Code Style

- Keep DTO, domain, cart, and UI state types separate.
- Use `@Serializable` and `@SerialName`; never Gson.
- Map DTOs to domain only at repository boundaries.
- Use sealed interfaces for screen state and enums with `UNKNOWN` for workflow
  values that may evolve.
- Model action availability as pure policy functions tested independently.
- Use `StateFlow` with lifecycle-aware collection; no LiveData.
- Keep request mutations single-flight and cancellation-cooperative.
- Keep composables focused; extract product rows, variant selectors, totals,
  status guidance, payment instructions, and proof form as distinct sections.
- Use semantic theme colors and text labels together; never rely on color alone.
- Never log proof URIs, sender/reference values, tokens, raw response bodies, or
  internal paths.

Example style:

```kotlin
enum class AccessoryAvailability {
    AVAILABLE,
    LOW_STOCK,
    UNAVAILABLE,
    UNKNOWN;

    val isOrderable: Boolean
        get() = this == AVAILABLE || this == LOW_STOCK
}
```

## Testing Strategy

### Contract and repository tests

- Decode complete and minimal accessory list/detail fixtures after exact JSON
  schemas are documented.
- Verify decimal strings preserve exact values and relative images map without
  exposing internal paths.
- Verify every MVP catalog query field and pagination value; assert that
  `brand`, `category`, and `placement` are never sent.
- Verify order-request list/detail/create/cancel envelopes, statuses, snapshots,
  optional accepted-order summary, and pagination.
- Verify submission sends only requested discount type, variant IDs, and
  quantities.
- Verify Optical Order DTOs decode both new payment statuses and all additive
  payment/proof fields without regressing existing orders.
- Verify multipart part names, MIME type, sender name, reference number, `201`
  first upload, and `200` idempotent retry.
- Verify stable workflow and Laravel validation errors survive as
  `ApiDomainError`.

### Domain and ViewModel tests

- Cart enforces distinct-item and quantity limits, deduplicates by variant ID,
  and preserves state on failed submission.
- Unavailable/unknown variants cannot be added or submitted locally.
- Search/filter generation suppresses stale responses; pagination, refresh,
  and append retry preserve correct content.
- Request actions follow status and reconcile from authoritative responses.
- Payment upload requires valid fields/file metadata, is single-flight, and
  handles success, idempotent retry, expiry, wrong state, rate limit, and
  rejected-proof no-retry behavior.
- Countdown derives from the offset timestamp and refreshes at expiry without
  locally inventing cancellation.
- Rating actions follow `is_rateable`, not inferred order status.
- Limited-account route restoration covers catalog, cart, requests, and order
  details without making protected API calls first.

### Compose and accessibility tests

- Catalog loading, empty, error, results, pagination, sort/rating filters, and
  orderability states are readable at large font scale.
- Cart limits, estimated-total disclaimer, discount declaration, submission
  disclaimer, and duplicate-tap prevention are visible and actionable.
- Request detail differentiates pending, accepted, rejected, cancelled, and
  unknown states without color-only meaning.
- Pending-payment UI appears only with server instructions and valid action
  capability; payment-review hides the countdown and upload form.
- Existing-proof and rejected-proof states never expose a replace action.
- Tracker order covers payment through pickup and remains understandable to
  TalkBack.
- Rating action appears only for `is_rateable: true` items.

### Regression gates

- Existing frame catalog/AR, Prescriptions, Saved Frames, Optical Order history,
  and frame-rating flows remain functional.
- Accessory navigation never appears inside prescription UI.
- Route allowlist contains only the documented accessory/order routes.
- Full unit tests, lint, instrumented-test compilation, and `assembleDebug` are
  run; unrelated pre-existing failures are reported, never removed.

## Boundaries

### Always

- Require an active Patient link before any accessory/order request call.
- Treat prices, request totals, discount decisions, order status, proof state,
  and rateability as server-authoritative.
- Keep API field names in snake_case at the transport boundary.
- Keep money exact and timestamps offset-aware.
- Validate cart and proof inputs locally while still handling backend `422`.
- Preserve user drafts on recoverable failure.
- Add focused tests with each behavior change.
- Run `assembleDebug` after implementation changes.

### Ask first

- Changing the proposed Home/catalog/request navigation placement.
- Adding disk persistence, Room, DataStore, offline catalog cache, background
  polling, WorkManager, or a new dependency.
- Changing existing My Orders naming or moving committed orders to a new
  package.
- Adding backend routes, fields, filter semantics, or mobile notification
  contracts.
- Supporting payment-proof resubmission or replacement.

### Never

- Add accessories to a prescription screen or send `placement=prescription`.
- Infer prescription compatibility or mark accessories prescription-required.
- Send client prices, totals, discount amounts, stock values, or proof review
  decisions.
- Show exact stock, expiry, cost, lots, internal notes, private proof paths, or
  storage metadata.
- Treat request submission or cart contents as a reservation or purchase.
- Hardcode GCash account details or show payment instructions after the backend
  removes them.
- Permit more than one proof or offer replacement/retry after an existing proof.
- Enable rating from catalog state or inferred eligibility.
- Store tokens, proof files, or health data in Room.
- Use Gson or apply `org.jetbrains.kotlin.android`.

## Success Criteria

1. Every feature route is active-link gated and no accessory endpoint is called
   for a known limited account.
2. The catalog sends only documented filters, never `placement`, paginates
   server results, and exposes no sensitive inventory fields.
3. The cart enforces 1–20 distinct variants and quantity 1–5 without server or
   Room persistence.
4. Submission sends only variant IDs/quantities and discount declaration,
   clearly states its non-binding nature, and preserves the cart on failure.
5. Current/history Order Request lists use server filters, and detail actions
   are correct for pending/accepted/rejected/cancelled/unknown states.
6. Accepted requests navigate to the existing Optical Order journey.
7. Optical Orders support `pending_payment` and `payment_review` alongside the
   existing fulfillment statuses.
8. GCash instructions appear only from non-null `payment_instructions`; a
   30-minute countdown never overrides server state locally.
9. Proof selection validates JPG/JPEG/PNG, 10 MB, and 8,000-pixel bounds; upload
   sends exactly the documented multipart fields.
10. First proof upload and idempotent retry both reconcile successfully, while
    no UI path replaces an existing or rejected proof.
11. Catalog ratings remain aggregate-only, and item rating is offered only when
    `is_rateable` is true.
12. Known 401/403/404/422/429 and workflow errors have tested recovery behavior.
13. Focused tests, full unit tests, lint, instrumented-test compilation, and
    `assembleDebug` pass or have unrelated pre-existing failures documented.

## Open Questions and Contract Gates

The stable accessory, request, payment-instruction, proof, and rate-limit wire
shapes are resolved above. Brand and Category controls are explicitly deferred,
and extra Laravel paginator keys are optional and ignored.

One backend response gate remains before Kotlin implementation:

1. **Expose stored request item fields:** Update the Order Request mobile
   resource/controller so every returned item includes `product_variant_id` and
   `item_snapshot` in the stable shape above. Create, list, detail, and cancel
   must all use that same resource.

## Approved Product Decisions

1. Use the proposed navigation: Home -> Accessories, catalog top-bar Cart and
   Requests, accepted request -> existing Optical Order detail.
2. Use a session-only cart with no disk persistence or cross-device sync.
