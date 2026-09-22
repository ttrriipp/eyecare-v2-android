# Implementation Plan: Backend Alignment v23 — Mobile Accessory Order Requests

Status: Approved — 2026-09-21

## References

- Approved specification:
  `docs/specs/backend-alignment-v23-mobile-accessory-order-requests-2026-09-20-spec.md`
- Authoritative transport contract: `docs/API_CONTRACT.md`
- Authoritative backend context: `docs/BACKEND_CONTEXT.md`
- Existing Optical Order implementation:
  `data/remote/api/OpticalOrderApiService.kt`,
  `data/remote/dto/OpticalOrderDtos.kt`,
  `data/repository/OpticalOrderRepositoryImpl.kt`, and
  `presentation/eyewear/`

## Overview

Implement the approved linked-patient accessory commerce journey as additive
vertical slices. Start with the catalog, then add the session cart and request
submission, request tracking/cancellation, and finally GCash proof upload and
the extended Optical Order tracker.

Prescriptions remain untouched. Existing frame browsing, Saved Frames,
appointment flows, My Orders, and item rating continue to work throughout the
migration. No new dependency, Room table, DataStore entry, background worker,
or backend route is introduced by Android.

Production implementation is blocked until the backend's shared Order Request
mobile resource returns `product_variant_id` and `item_snapshot` for create,
list, detail, and cancel responses. Phase 0 verifies this prerequisite before
any Android production source is changed.

## Architecture Decisions

### 1. Keep catalog and request boundaries separate

Create two focused verticals:

- `AccessoryRepository` owns `GET /accessories` and
  `GET /accessories/{id}`.
- `AccessoryOrderRequestRepository` owns request list, create, detail, and
  cancellation.

The split matches backend resource ownership and prevents catalog pagination
state from becoming coupled to a patient's commerce history. Each repository
maps DTOs to serialization-free domain models at its boundary and uses
`safeApiCall` for structured errors.

### 2. Model the stable wire shapes exactly

Use dedicated DTO groups for Accessories and Order Requests. Money uses the
existing `MoneyValueSerializer` and maps to `BigDecimal`. Product attributes
decode as `JsonObject`, not `Map<String, String>`, because values may include
numbers, strings, and other JSON primitives. Repository mapping converts them
to a serialization-free display model rather than exposing `JsonElement` to
presentation.

Accessory paginator responses map only the documented `links`/`meta` fields;
the configured `ignoreUnknownKeys = true` safely ignores additive Laravel
metadata.

Unknown availability, request status, discount type, order status, proof
status, or attribute value shapes fail closed for mutation capability while
remaining renderable with neutral copy.

### 3. Express catalog queries as a typed value

`AccessoryQuery` owns search, sort, minimum rating, rated state, page, and
per-page. Retrofit receives those values from the repository. The MVP does not
include Brand or Category fields in presentation state and never sends
`brand`, `category`, or `placement`.

The catalog ViewModel uses a monotonically increasing generation (or cancels
the prior load) so stale search/filter responses cannot replace newer results.
It owns first load, retained-content refresh, append loading, and append retry.

### 4. Scope the cart to the authenticated Main graph

Use one `AccessoryCartViewModel` scoped to the `MainGraph` navigation back-stack
entry and share it across catalog, detail, cart, and review destinations. This
keeps cart state through configuration changes and ordinary navigation without
disk persistence. Popping the authenticated graph on logout destroys the cart
and prevents cross-account leakage.

The cart is keyed by variant ID and stores only patient-safe display snapshots
plus quantity. It is cleared only after a successfully mapped Order Request or
when the patient explicitly clears it. Process death may lose the cart; no
SavedStateHandle serialization, Room, DataStore, or server draft is added.

### 5. Separate cart editing from submission orchestration

The shared cart ViewModel owns deterministic cart operations and estimated
subtotal. A route-scoped `AccessoryCheckoutViewModel` owns discount selection,
single-flight request submission, structured errors, and the authoritative
success resource.

Checkout constructs a command containing only `requested_discount_type`,
variant IDs, and quantities. It never copies display prices or totals into the
request body. Recoverable failure preserves both cart and discount selection.

### 6. Give Order Requests an independent current/history state machine

`AccessoryOrderRequestListViewModel` owns server-filtered Current/History
pagination. It never merges requests with Optical Orders or repartitions
records locally. `AccessoryOrderRequestDetailViewModel` owns detail refresh and
pending-request cancellation.

Accepted request detail emits a typed `OpticalOrderDetail(orderId)` navigation
event. It does not fetch or embed the full order itself.

### 7. Extend rather than duplicate Optical Orders

Add `PENDING_PAYMENT` and `PAYMENT_REVIEW` to the existing order domain and map
the additive payment/proof fields into:

- `paymentExpiresAt`;
- `PaymentProofStatus`;
- patient-visible rejection reason; and
- nullable `PaymentInstructions`.

Add proof upload to `OpticalOrderApiService` and
`OpticalOrderRepository`. The first `201` and idempotent `200` map to the same
`PaymentProofSummary`. After either response, detail always refetches the
Optical Order because the proof response does not embed it.

Do not create a second order screen or repository for accepted accessory
requests.

### 8. Keep legacy and accessory trackers semantically correct

The existing eyewear tracker remains the default for orders that never entered
the accessory payment-proof workflow. An order uses the extended payment-to-
pickup tracker only when its status is `pending_payment`/`payment_review` or its
proof status shows participation in that workflow.

This avoids showing invented GCash steps on existing clinic-created eyewear
orders while allowing accepted accessory requests to progress through payment,
review, queue, preparation, ready, and pickup.

### 9. Validate proof metadata before the repository upload

Use `ActivityResultContracts.OpenDocument` with JPG/JPEG/PNG MIME types. A
focused inspector reads `ContentResolver` metadata and image bounds without
fully decoding the bitmap, validating readable content, <= 10 MB, and <= 8,000
x 8,000 dimensions.

The repository follows the existing temporary-cache multipart pattern, uses
the part names `proof`, `sender_name`, and `reference_number`, and deletes the
temporary file in `finally`. It never logs or persists the URI, sender name,
reference, or proof bytes.

### 10. Make all accessory destinations active-link protected

Add type-safe routes for catalog, detail, cart, requests, and request detail.
Each maps to `PatientFeatureIntent` so a limited account is sent to the existing
link hub before any protected API call and returns to the intended destination
after linking.

Home adds the approved Accessories entry point. Prescriptions receive no route,
copy, component, query, or model change. The route allowlist adds exactly the
seven new active-link annotations: six accessory/request routes plus payment
proof upload.

## Dependency Graph

```text
backend exposes product_variant_id + item_snapshot
                         │
                         ▼
               baseline + contract fixtures
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
       accessory catalog      Optical Order additive
       domain/repository      payment/proof contract
              │                     │
              ▼                     ▼
       catalog + detail       proof validation/upload
              │                     │
              ▼                     ▼
      MainGraph-scoped cart    payment/order detail UI
              │                     │
              ▼                     │
       checkout submission           │
              │                     │
              ▼                     │
     request list/detail/cancel ─────┘
              │
              ▼
       navigation/Home/access/route governance
              │
              ▼
       full regression + living documentation
```

## Implementation Phases

### Phase 0 — Verify backend gate and record baseline

Before Android production edits:

- verify a current create/list/detail/cancel response from the backend resource
  includes `product_variant_id` and `item_snapshot` on every item;
- reconcile the final response examples into `docs/API_CONTRACT.md` if the
  backend repository has not already done so;
- run focused Optical Order, route-access, navigation, Home, and API-governance
  tests;
- run `assembleDebug`; and
- record unrelated failures and the existing dirty-worktree boundary.

If the two item fields are still absent, stop after the baseline. Android must
not make them nullable to hide backend drift.

#### Checkpoint 0 — Prerequisite proven

- All four Order Request operations use the same stable resource.
- Stored variant/snapshot fields are present in actual response JSON.
- Existing focused tests and build results are recorded.
- User-owned appointment and backend-document changes remain intact.

### Phase 1 — Complete the read-only accessory catalog vertical

Write failing tests first for the exact list/detail fixtures, heterogeneous
attributes, money precision, availability values, pagination, ignored extra
metadata, query encoding, and absence of prohibited parameters.

Then add:

- accessory DTOs, domain models, mapper, API service, repositories, and Hilt
  binding;
- typed query/filter policy;
- catalog and detail ViewModels;
- catalog search, sort, rating/rated filters, pagination, retained refresh,
  empty/error states, product cards, and availability-aware variant detail;
- type-safe catalog/detail routes and active-link intent restoration; and
- the linked-only Home entry point.

Cart affordances may be visually present but disabled or kept behind callbacks
until Phase 2 owns cart behavior.

#### Checkpoint A — Catalog proof

- List and detail decode the stable schema without DTO leakage.
- Search/filter races, refresh, pagination, and append retry pass unit tests.
- Unavailable/unknown variants expose no add action.
- Brand, Category, and `placement` are absent from requests and UI.
- A limited account reaches the link hub before a catalog call.
- Focused tests, Android-test compilation, and `assembleDebug` pass.

### Phase 2 — Add the session cart and checkout submission vertical

Implement the MainGraph-scoped cart with pure, test-first operations for add,
increment, decrement, remove, clear, deduplication, distinct-item limit,
quantity limit, orderability, and estimated subtotal.

Build cart and review UI with:

- stable item snapshots and availability guidance;
- 1–20 distinct and quantity 1–5 enforcement;
- estimated-total and no-reservation disclaimers;
- `none`, `senior_citizen`, and `pwd` declaration copy; and
- single-flight submission through the request repository.

On success, clear the cart and replace checkout with the returned request
detail route. On `ACCESSORY_NOT_ORDERABLE` or validation error, retain the
draft. On `ACTIVE_ORDER_REQUEST_EXISTS`, retain the cart and offer navigation
to Current Requests.

#### Checkpoint B — Request submission proof

- Cart policy tests cover every lower/upper bound and duplicate variant.
- MockWebServer proves the request body contains no prices or totals.
- Failed submission preserves cart and discount state.
- Successful mapping clears the cart exactly once and opens request detail.
- Rapid taps produce one POST.
- Focused tests and `assembleDebug` pass.

### Phase 3 — Complete request list, detail, and cancellation

Add independent Current/History list state with deterministic pagination,
refresh, stale-response suppression, empty/error states, and append retry.

Build request detail from immutable snapshots and status policy. Pending detail
offers idempotent cancellation behind confirmation. Accepted detail shows the
server order summary and opens `OpticalOrderDetail`. Rejected/cancelled states
show patient-visible terminal guidance and no mutation action. Unknown status
fails closed.

Refresh detail after `ORDER_REQUEST_NOT_ACTIONABLE`. Preserve ownership-safe
404 presentation and do not reveal whether another account owns an ID.

#### Checkpoint C — Request lifecycle proof

- Current and History always send their server filter and keep independent
  state.
- Every status maps to tested labels, colors, and action capability.
- Cancellation is single-flight and uses the returned authoritative resource.
- Accepted request navigation passes only the typed order ID.
- Request snapshots render without consulting live catalog/cart state.
- Focused tests, Android-test compilation, and `assembleDebug` pass.

### Phase 4 — Extend the Optical Order contract and payment state policy

Characterize current Optical Order behavior first, then add tests and mapping
for:

- `pending_payment` and `payment_review`;
- nullable `payment_expires_at`;
- proof status and rejection reason;
- nullable exact-shape GCash instructions; and
- proof result `pending`, `accepted`, `rejected`, and unknown states.

Update list/detail presentation policies and cards so all statuses are
patient-readable. Keep existing queued-through-dispensed behavior and rating
mapping intact. Add pure deadline and tracker policy tests before changing the
Compose screen.

#### Checkpoint D — Additive order compatibility proof

- Old order fixtures still decode and render safely.
- Every new field/status decodes with exact money and offset timestamps.
- Accessory and legacy tracker selection is deterministic and tested.
- A missing instruction object never produces hardcoded GCash UI.
- Existing Optical Order list/detail/rating tests remain green.
- `assembleDebug` passes.

### Phase 5 — Add payment-proof selection, upload, and reconciliation

Implement proof inspection and form validation, then add multipart upload to
the existing Optical Order repository. Extend detail state to retain loaded
content while proof selection, validation, upload, refresh, or a recoverable
error occurs.

Pending-payment UI shows only server instructions, exact balance due, order
reference, nullable deadline, sender name, reference number, and proof picker.
At local deadline it disables a new upload and refreshes the order. It never
locally sets cancellation.

After `201` or `200`, map the proof summary, clear local selected-file/form
state, and fetch the order. `PAYMENT_WINDOW_EXPIRED` and
`ORDER_NOT_AWAITING_PAYMENT` also trigger refresh. A `429` retains form state
and uses `Retry-After`. Existing/rejected proofs expose no replace, delete, or
resubmit action.

#### Checkpoint E — Payment proof proof

- Inspector tests cover MIME type, read failure, size, width, and height.
- Multipart tests prove exact part names/content and temporary-file cleanup.
- Both success codes trigger one authoritative order refresh.
- Expiry, wrong-state, validation, 404, and rate-limit paths retain safe state.
- No proof path, URI, bytes, sender, or reference is logged or persisted.
- Focused tests, Android-test compilation, and `assembleDebug` pass.

### Phase 6 — Integrate fulfillment tracker, rating, and live navigation

Finish the accepted-request-to-pickup journey:

- show payment review without countdown or upload controls;
- show queued, preparation, ready, dispensed, and cancelled guidance;
- use the extended tracker only for accessory payment-proof orders;
- keep legacy Optical Order tracker behavior for other orders;
- retain `is_rateable` as the sole item-rating capability; and
- refresh order detail on resume so staff decisions and fulfillment changes
  become visible without polling.

Complete live navigation callbacks for catalog Cart/Requests actions, checkout
success, request-to-order link, My Orders, Back behavior, and restored
post-link intents. Update route allowlist normalization and access tests for all
new annotations and route classes.

#### Checkpoint F — End-to-end journey proof

- Catalog -> cart -> submit -> pending request is navigable.
- Accepted request -> pending payment -> proof review -> pickup tracking is
  represented without client-invented transitions.
- Legacy eyewear orders do not show accessory-only payment steps.
- Rating appears only for `is_rateable: true` items after authoritative refresh.
- Limited-account restoration reaches every requested accessory destination.
- Navigation, route-governance, Home, order, and rating tests pass.
- `assembleDebug` passes.

### Phase 7 — Hardening, visual verification, and living documentation

- verify TalkBack order/labels, 48dp targets, large-font scrolling, light/dark
  themes, compact viewport layout, keyboard/IME behavior, and system Back;
- verify loading, empty, offline, retry, background-refresh failure, unknown
  status, expired payment, rejected proof, and unavailable variant states;
- verify no accessory content or action appears in Prescriptions;
- run static searches for prohibited parameters and sensitive logging;
- run focused tests, full unit tests, Android-test compilation, lint, and build;
- update `CONTEXT.md` and V23 documentation to the implemented state; and
- report unrelated pre-existing failures rather than weakening tests.

#### Checkpoint G — Completion

- Every approved V23 success criterion has evidence.
- No exact stock, internal path, private proof data, or prescription inference
  is exposed.
- Full verification outcomes are reported accurately.
- Working-tree review contains only intended app, test, and V23 documentation
  changes alongside preserved user-owned edits.

## Migration and Compatibility Strategy

1. Prove the backend response gate and current regression baseline.
2. Add catalog and request code alongside existing frame/eyewear features.
3. Add cart and checkout without any server-side draft compatibility layer.
4. Extend Optical Orders additively; do not rename or duplicate the existing
   vertical.
5. Wire Home and navigation only after each destination passes focused tests.
6. Harden route governance and docs after the full flow is reachable.

No runtime fallback sends legacy `/orders`, `placement`, client prices, or a
second proof. If a required stable response field is absent, the repository
fails the response rather than manufacturing data.

## Parallel and Sequential Work

Safe to parallelize after Checkpoint 0 when file ownership is coordinated:

- catalog presentation and Optical Order additive contract tests;
- pure cart policy and pure payment deadline/tracker policy;
- request list presentation and proof file-inspector tests after their domain
  contracts are fixed.

Must remain sequential:

- backend resource correction before Android request DTO implementation;
- catalog domain before cart display snapshots;
- cart before checkout submission;
- request repository before request list/detail;
- Optical Order additive mapping before proof upload/detail UI;
- individual destination verification before live navigation cutover; and
- navigation/access cutover before final route-governance proof.

For a single implementation thread, follow the numbered phases. Phase 3 task
breakdown will assign narrow file ownership and keep each task near five files.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Backend still omits stored request item fields | Runtime decode failure or guessed UI | Hard Phase 0 gate; require the shared resource fix before Android source work |
| Heterogeneous attributes are modeled as strings | Decode crash or information loss | DTO uses `JsonObject`; mapper converts supported values without transport leakage |
| Cart survives logout into another account | Cross-account stale commerce draft | Scope cart to `MainGraph`; logout pops and destroys the graph ViewModel |
| Cart limit logic differs between screens | Invalid checkout or confusing quantities | One pure cart policy/store owns all mutations; UI emits intents only |
| Search response races overwrite current filters | Wrong products under selected controls | Cancel jobs or compare query generations before applying results |
| Server price/availability changes after carting | Misleading totals or failed request | Label estimate, send no money, preserve cart, surface `ACCESSORY_NOT_ORDERABLE` |
| Accepted request is treated as completed purchase | Incorrect patient expectation | Status copy and success dialog repeat review/non-reservation boundary |
| New tracker changes legacy eyewear meaning | Existing order regression | Select accessory tracker from payment/proof participation; retain legacy policy otherwise |
| Countdown invents expiry before server scheduler | Wrong cancellation/payment state | Countdown disables upload then refreshes; server status remains authoritative |
| Proof validation decodes a large bitmap | Memory pressure or crash | Use metadata and `inJustDecodeBounds`; enforce bytes/dimensions before upload |
| Temporary proof file remains after failure | Private data residue | Create only for upload and delete in `finally`; add cleanup tests |
| Idempotent proof retry is treated as conflict | Patient sees false failure or retries again | Treat both `200` and `201` as success and refetch order |
| Active-link guard runs after a protected API call | 403 churn or protected-data boundary violation | Register every typed route/intent and test redirect before ViewModel creation/load |
| Route allowlist misses new annotations | Governance test failure or undocumented API use | Add all seven active-link routes and normalize their path variables in the same phase |
| Large Compose files become monolithic | Difficult review and regression risk | Extract focused product, cart, status, instructions, and proof-form sections |
| Dirty worktree changes are overwritten | User work loss | Patch narrowly, never reset, inspect status/diffs at each checkpoint |

## Verification Commands

Run from the repository root with Android Studio's JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Focused contract/data verification:

```powershell
.\gradlew testDebugUnitTest --tests "*Accessory*" --tests "*OpticalOrder*" --tests "*PaymentProof*" --tests "*ApiRouteAllowlist*"
```

Focused state/navigation verification:

```powershell
.\gradlew testDebugUnitTest --tests "*Accessory*ViewModel*" --tests "*OpticalOrder*ViewModel*" --tests "*PatientFeatureIntent*" --tests "*PatientRouteAccess*" --tests "*HomeScreen*"
```

Mandatory after every production increment:

```powershell
.\gradlew assembleDebug
```

Checkpoint and final verification:

```powershell
.\gradlew testDebugUnitTest
.\gradlew compileDebugAndroidTestKotlin
.\gradlew lintDebug
.\gradlew assembleDebug
```

Formatting when required:

```powershell
.\gradlew ktlintFormat
```

## Manual Final Matrix

1. A linked patient opens Accessories from Home; a limited patient reaches the
   link hub first and returns to Accessories after linking.
2. Catalog search, sort, rating filters, pagination, refresh, and retry work;
   Brand/Category controls are absent.
3. Product/detail images and attributes render safely; unavailable variants
   cannot enter the cart.
4. Cart enforces 20 distinct variants and quantity 5, survives feature
   navigation, and is gone after logout.
5. Review clearly labels the estimated total, non-reservation, and requested
   discount; request JSON contains no money.
6. Submission failure retains the cart; success clears it and opens the server
   request detail.
7. Current/History Requests paginate independently; only pending shows Cancel;
   accepted opens the typed Optical Order.
8. Pending payment shows only returned GCash details and exact balance due;
   absent instructions show no upload form.
9. Invalid proof type/size/dimensions and missing sender/reference are rejected
   locally without a network call.
10. First proof upload and idempotent retry both lead to payment review after an
    order refresh; `429` retains the form and shows retry timing.
11. Existing/rejected proof never offers replace or resubmit.
12. Payment review, queued, preparing, ready, dispensed, and cancelled states
    refresh correctly without polling or client-invented transitions.
13. Accessory orders use the extended tracker; legacy eyewear orders keep their
    current tracker.
14. Rating appears only when `is_rateable` is true and continues to update the
    existing item rating.
15. Prescription list/detail contain no accessory entry, compatibility copy,
    cart action, or accessory API request.

## Documentation Deliverables

- V23 specification marked approved with the confirmed product decisions;
- this Phase 2 plan approved before Phase 3 task breakdown;
- `docs/specs/backend-alignment-v23-mobile-accessory-order-requests-2026-09-20-tasks.md`
  created only after plan approval;
- `CONTEXT.md` updated after the Android implementation ships; and
- backend contract/context edited only for verified contract corrections, not
  speculative Android behavior.

## Exit Criteria for Phase 2

- major components and dependencies are explicit;
- backend and Android ownership boundaries are clear;
- catalog, cart, requests, payment proof, fulfillment, and rating have safe,
  buildable ordering;
- existing Optical Orders and Prescriptions have explicit regression guards;
- risks have concrete mitigations;
- parallel and sequential boundaries are clear;
- checkpoints and verification commands are reviewable; and
- no Android production implementation has begun.

## Phase Gate

This Phase 2 plan requires project-owner approval. After approval, Phase 3 may
produce the small, dependency-ordered task list and must pause again before
production code changes.
