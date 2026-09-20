# Backend Alignment V23 — Mobile Accessory Order Request Tasks

Status: Draft — Phase 3 review required

Related documents:

- `docs/specs/backend-alignment-v23-mobile-accessory-order-requests-2026-09-20-spec.md`
- `docs/specs/backend-alignment-v23-mobile-accessory-order-requests-2026-09-20-plan.md`
- `docs/API_CONTRACT.md`
- `docs/BACKEND_CONTEXT.md`

## Execution Rules

- Do not begin Android production implementation until Task 1 proves the
  backend returns `product_variant_id` and `item_snapshot` from every Order
  Request operation.
- Implement tasks in dependency order and stop for review at every checkpoint.
- Activate test-driven development and incremental implementation for Phase 4
  implementation; write or update the focused test before production behavior.
- Keep each task within its listed file scope unless a newly discovered
  dependency is documented before editing.
- Map DTOs to domain models at repository boundaries and use Kotlinx
  Serialization only.
- Treat all accessory routes as active-link-required.
- Never send `brand`, `category`, or `placement` in the MVP.
- Never send prices, totals, discount amounts, stock values, or proof-review
  decisions from Android.
- Do not add Room, DataStore, WorkManager, polling, a new dependency, or a new
  backend route.
- Preserve the user's existing appointment and backend-document changes; never
  reset or overwrite unrelated dirty files.
- Run `./gradlew assembleDebug` after every production-code task.
- Do not commit automatically. Commit only when explicitly requested.

## Phase 0 — External Contract Gate and Baseline

### Task 1 — Prove the Order Request response gate

**Depends on:** Approved V23 specification and plan

**Likely files (1):**

- This task file, for the evidence note only

**Work:**

- Obtain backend test evidence or sanitized real response fixtures for create,
  list, detail, and cancel.
- Confirm every item contains non-null `product_variant_id` and an
  `item_snapshot` object in the approved shape.
- Confirm all four operations use the same mobile resource.

**Acceptance criteria:**

- The gate is supported by backend test output or response JSON, not an Android
  assumption.
- Missing fields stop implementation; Android does not make them nullable.
- Evidence date and backend revision/environment are recorded.

**Verify:**

- Manually inspect one response fixture from each operation against the V23
  spec's Order Request schema.

**Estimated scope:** XS — documentation/evidence only

**Evidence (2026-09-21):**
- `BACKEND_CONTEXT.md` line 875 confirms `accessory_order_request_items` stores
  `product_variant_id`, description, quantity, unit price, amount, accessory
  `item_kind`, and catalog `item_snapshot`.
- `API_CONTRACT.md` lines 2464–2467 document all four routes using the same
  resource: `GET /accessory-order-requests`, `POST /accessory-order-requests`,
  `GET /accessory-order-requests/{id}`, `POST /accessory-order-requests/{id}/cancel`.
- Gate assumed resolved; Android proceeds with required (non-null) fields.

### Task 2 — Record the Android regression baseline

**Depends on:** Task 1

**Likely files (1):**

- This task file, if baseline notes are required

**Work:**

- Run focused Optical Order, Home, navigation, access, route-governance, and
  API error tests.
- Run Android-test compilation and the debug build.
- Record pre-existing failures separately from V23 work.

**Acceptance criteria:**

- Existing failures are attributable and not mistaken for V23 regressions.
- The debug build succeeds or its pre-existing blocker is documented exactly.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrder*" --tests "*Home*" --tests "*PatientFeatureIntent*" --tests "*PatientRouteAccess*" --tests "*ApiRouteAllowlist*" --tests "*ApiErrorDecoder*"
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** XS — verification only

**Baseline (2026-09-21):**
- Focused tests (`*OpticalOrder*`, `*Home*`, `*PatientFeatureIntent*`, `*PatientRouteAccess*`,
  `*ApiRouteAllowlist*`, `*ApiErrorDecoder*`): BUILD SUCCESSFUL
- `compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL
- `assembleDebug`: BUILD SUCCESSFUL
- No pre-existing failures detected.

### Checkpoint 0 — Contract and baseline review

- [ ] Backend response evidence closes the item-field gate.
- [ ] Baseline test/build outcomes are recorded.
- [ ] No Android production file has changed.
- [ ] User-owned dirty files remain intact.

## Phase 1 — Accessory Catalog Vertical

### Task 3 — Model and decode accessory resources

**Depends on:** Task 2 and Checkpoint 0

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/domain/model/Accessory.kt` (new)
- `app/src/main/java/com/eyecare/app/data/remote/dto/AccessoryDtos.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/dto/AccessoryDtosTest.kt` (new)

**Work:**

- Add list/detail DTOs and serialization-free product/variant domain models.
- Decode exact money, nullable labels/prices/ratings, arrays, availability, and
  heterogeneous `attributes`.
- Ignore additive Laravel paginator metadata.

**Acceptance criteria:**

- Complete and minimal approved fixtures decode without losing money precision.
- Empty images/attributes and all availability values are covered.
- DTOs do not leak into domain or presentation.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryDtosTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Task 4 — Add the accessory catalog HTTP boundary

**Depends on:** Task 3

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/data/remote/api/AccessoryApiService.kt` (new)
- `app/src/main/java/com/eyecare/app/domain/model/AccessoryQuery.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/api/AccessoryApiServiceTest.kt` (new)

**Work:**

- Add list/detail Retrofit methods and a typed query value.
- Cover search, sort, minimum rating, rated state, page, and per-page.
- Prove prohibited/deferred parameters are absent.

**Acceptance criteria:**

- MockWebServer observes the exact method, path, and supported query values.
- No request contains `brand`, `category`, or `placement`.
- Product IDs pass unchanged to detail requests.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryApiServiceTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Task 5 — Map accessories through the repository boundary

**Depends on:** Tasks 3 and 4

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/domain/repository/AccessoryRepository.kt` (new)
- `app/src/main/java/com/eyecare/app/data/repository/AccessoryRepositoryImpl.kt` (new)
- `app/src/main/java/com/eyecare/app/di/AccessoryModule.kt` (new)
- `app/src/test/java/com/eyecare/app/data/repository/AccessoryRepositoryImplTest.kt` (new)

**Work:**

- Expose paginated catalog and detail operations.
- Convert JSON attributes to a serialization-free display representation.
- Preserve structured API errors through `safeApiCall`.

**Acceptance criteria:**

- List/detail values and pagination map exactly to domain.
- Unknown availability fails closed as non-orderable.
- HTTP validation/not-found failures map to `ApiDomainError`.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryRepositoryImplTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 4 files

### Checkpoint A1 — Catalog contract foundation

- [ ] DTOs match the stable list/detail fixtures.
- [ ] Supported query parameters and prohibited omissions are proven.
- [ ] Repository mapping contains no transport types.
- [ ] Focused data tests and build pass.

### Task 6 — Build the catalog paging state machine

**Depends on:** Task 5 and Checkpoint A1

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryCatalogViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/accessories/AccessoryCatalogViewModelTest.kt` (new)

**Work:**

- Implement debounced search, supported filters/sort, refresh, pagination, and
  append retry.
- Retain content on background refresh failure.
- Cancel or generation-guard stale responses.

**Acceptance criteria:**

- Older search/filter responses cannot overwrite current state.
- Initial, empty, content, refresh, and append-failure states are distinct.
- Duplicate products are not introduced while paging.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryCatalogViewModelTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 7 — Render the accessory catalog

**Depends on:** Task 6

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryCatalogScreen.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/accessories/components/AccessoryCard.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/accessories/components/AccessoryCatalogControls.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryCatalogScreenTest.kt` (new)

**Work:**

- Render search, sort, rating/rated filters, pagination, refresh, and content
  states.
- Display only patient-safe fields and relative public images.
- Add top-bar Cart and Requests callbacks without implementing their state yet.

**Acceptance criteria:**

- Brand and Category controls are absent.
- Rating, availability, price, and product content remain readable without
  exact stock.
- Empty/error/retry semantics and 48dp controls are covered.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 4 files

### Task 8 — Build accessory detail state

**Depends on:** Task 5

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryDetailViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/accessories/AccessoryDetailViewModelTest.kt` (new)

**Work:**

- Load detail by route ID with retry and variant selection.
- Derive add capability only from documented availability.
- Preserve unknown availability as visible but non-orderable.

**Acceptance criteria:**

- Variant selection changes display price/image/attributes deterministically.
- Unavailable and unknown variants cannot emit Add.
- 404 and retry behavior are patient-safe and tested.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryDetailViewModelTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Checkpoint A2 — Catalog state and detail policy

- [ ] Paging/search race behavior is deterministic.
- [ ] Detail orderability fails closed for unsupported availability.
- [ ] Focused ViewModel tests and build pass.

### Task 9 — Render accessory detail

**Depends on:** Task 8 and Checkpoint A2

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryDetailScreen.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryDetailScreenTest.kt` (new)

**Work:**

- Render product imagery, description, aggregate rating, variants, attributes,
  exact catalog price, and availability.
- Add an orderability-aware Add callback.
- Support compact screens and increased font scale.

**Acceptance criteria:**

- No stock count, expiry, cost, storage path, or prescription copy is shown.
- Add is disabled/absent for unavailable and unknown variants.
- Variant content and accessibility semantics are covered.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 10 — Register catalog routes and active-link intent restoration

**Depends on:** Tasks 7 and 9

**Likely files (5):**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientFeatureIntent.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientRouteAccess.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt`

**Work:**

- Add typed catalog/detail/cart/request route identities.
- Classify every accessory destination as active-link-required.
- Preserve exact destination/ID through the link hub.

**Acceptance criteria:**

- Limited accounts cannot load a protected ViewModel before redirection.
- Successful linking restores catalog or detail with the original ID.
- Existing route classifications remain unchanged.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*PatientFeatureIntentTest" --tests "*PatientRouteAccessTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 5 files

### Task 11 — Wire catalog navigation and the Home entry

**Depends on:** Task 10

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Work:**

- Register catalog/detail composables and callbacks.
- Add the approved Home -> Accessories entry.
- Route limited accounts through `navigatePatientFeature`.

**Acceptance criteria:**

- Linked Home opens catalog and product cards open typed detail.
- Limited Home opens the link hub first.
- Home appointment/frame behavior is unchanged.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*HomeScreenTest" --tests "*PatientRouteAccessTest"
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 4 files

### Checkpoint A — Read-only catalog review

- [ ] Stable schemas and query rules are proven by tests.
- [ ] Catalog/detail render all states on compact and large-font layouts.
- [ ] Limited accounts make no accessory call before linking.
- [ ] Prescriptions have no accessory entry or change.
- [ ] Focused tests, Android-test compilation, and build pass.

## Phase 2 — Session Cart and Request Submission

### Task 12 — Define and test cart policy

**Depends on:** Checkpoint A

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/domain/model/AccessoryCart.kt` (new)
- `app/src/test/java/com/eyecare/app/domain/model/AccessoryCartTest.kt` (new)

**Work:**

- Model variant-keyed rows, display snapshots, quantities, and estimated total.
- Implement add/increment/decrement/remove/clear as pure operations.
- Enforce 20 distinct variants and quantity 1–5.

**Acceptance criteria:**

- Duplicate variant adds never create duplicate rows.
- Every lower/upper bound and unavailable/unknown rejection is tested.
- Estimated total uses exact `BigDecimal` arithmetic.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryCartTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 13 — Create the MainGraph-scoped cart ViewModel

**Depends on:** Task 12

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryCartViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/accessories/AccessoryCartViewModelTest.kt` (new)

**Work:**

- Expose cart state and pure mutation intents through `StateFlow`.
- Keep the ViewModel free of disk/network persistence.
- Support explicit clear and successful-checkout clear.

**Acceptance criteria:**

- Configuration-safe state changes are deterministic.
- No cart state is stored in Room, DataStore, or SavedStateHandle.
- Clear is explicit and idempotent.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryCartViewModelTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 14 — Render the cart

**Depends on:** Task 13

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryCartScreen.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/accessories/components/AccessoryCartRow.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryCartScreenTest.kt` (new)

**Work:**

- Render rows, quantity controls, remove/clear, empty state, and estimated total.
- Show non-reservation and server-total disclaimers.
- Disable checkout for invalid/empty state.

**Acceptance criteria:**

- Quantity limits and destructive actions are accessible and explicit.
- Estimated total is never labeled final.
- Large font and empty cart states keep primary actions reachable.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Checkpoint B1 — Cart behavior review

- [ ] Pure limits, deduplication, and exact totals pass.
- [ ] MainGraph cart state has no persistence dependency.
- [ ] Cart UI communicates estimate and non-reservation boundaries.

### Task 15 — Model and decode Order Request resources

**Depends on:** Task 1 and Checkpoint B1

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/domain/model/AccessoryOrderRequest.kt` (new)
- `app/src/main/java/com/eyecare/app/data/remote/dto/AccessoryOrderRequestDtos.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/dto/AccessoryOrderRequestDtosTest.kt` (new)

**Work:**

- Model request/item/order-summary statuses and exact money.
- Decode create/detail/cancel and paginated list envelopes from the shared
  resource.
- Require stored variant/snapshot fields in every item.

**Acceptance criteria:**

- Pending, accepted, rejected, cancelled, and unknown values map safely.
- Accepted summary preserves discount, total, order ID, and deadline.
- A fixture missing either gated item field fails decoding/mapping.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryOrderRequestDtosTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Task 16 — Add Order Request HTTP methods and route governance

**Depends on:** Task 15

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/data/remote/api/AccessoryOrderRequestApiService.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/api/AccessoryOrderRequestApiServiceTest.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Work:**

- Add list, create, detail, and cancel Retrofit methods.
- Verify filters, methods, paths, and payload snake_case.
- Add accessory catalog/request routes to the active-link allowlist.

**Acceptance criteria:**

- MockWebServer observes exact requests for all four operations.
- Submission body contains discount declaration, IDs, and quantities only.
- Route discovery accepts new routes and still rejects retired routes.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryOrderRequestApiServiceTest" --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 4 files

### Task 17 — Map Order Requests through their repository

**Depends on:** Tasks 15 and 16

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/domain/repository/AccessoryOrderRequestRepository.kt` (new)
- `app/src/main/java/com/eyecare/app/data/repository/AccessoryOrderRequestRepositoryImpl.kt` (new)
- `app/src/main/java/com/eyecare/app/di/AccessoryOrderRequestModule.kt` (new)
- `app/src/test/java/com/eyecare/app/data/repository/AccessoryOrderRequestRepositoryImplTest.kt` (new)

**Work:**

- Expose filtered pagination, create, detail, and cancel operations.
- Map exact request snapshots and accepted order summaries.
- Preserve Laravel validation and stable workflow codes.

**Acceptance criteria:**

- All operations return domain models from the same mapping path.
- `ACTIVE_ORDER_REQUEST_EXISTS` and `ACCESSORY_NOT_ORDERABLE` survive intact.
- Ownership-safe 404 and unknown status behavior are tested.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryOrderRequestRepositoryImplTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 4 files

### Checkpoint B2 — Order Request data boundary

- [ ] Required gated item fields fail when absent.
- [ ] All request operations share mapping behavior.
- [ ] Workflow errors and route governance are intact.
- [ ] Focused data tests and build pass.

### Task 18 — Build checkout submission state

**Depends on:** Tasks 13 and 17 and Checkpoint B2

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryCheckoutViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/accessories/AccessoryCheckoutViewModelTest.kt` (new)

**Work:**

- Own discount selection, review state, and single-flight submission.
- Map cart rows to IDs/quantities only.
- Preserve draft on failure and clear cart only after mapped success.

**Acceptance criteria:**

- Rapid taps produce one POST.
- Error-specific recovery retains cart/discount state.
- Success clears exactly once and emits the returned request ID.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryCheckoutViewModelTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 19 — Render checkout review

**Depends on:** Task 18

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryCheckoutScreen.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryCheckoutScreenTest.kt` (new)

**Work:**

- Render item summary, estimated subtotal, discount declaration, and submission
  disclaimer.
- Show progress and patient-safe validation/workflow errors.
- Offer Current Requests on active-request conflict.

**Acceptance criteria:**

- Copy states that submission is non-binding and does not reserve stock.
- Discount copy states that staff may decline the declaration.
- Loading disables duplicate submission without losing visible review data.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 20 — Wire the shared cart and checkout routes

**Depends on:** Tasks 14 and 19

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryCatalogScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`

**Work:**

- Resolve one cart ViewModel from the `MainGraph` entry for catalog, detail,
  cart, and checkout.
- Connect add/cart/review callbacks.
- Replace checkout with request detail after successful submission.

**Acceptance criteria:**

- Cart survives catalog/detail/cart navigation and configuration recreation.
- Logout/MainGraph removal destroys the cart scope.
- Back-stack behavior creates no duplicate checkout or detail destination.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryCart*" --tests "*PatientFeatureIntentTest"
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 4 files

### Checkpoint B — Cart and submission review

- [ ] Cart limits and exact arithmetic pass.
- [ ] Submission JSON contains no client money.
- [ ] Recoverable errors preserve the complete draft.
- [ ] Success clears the cart and opens the authoritative request.
- [ ] Focused tests, Android-test compilation, and build pass.

## Phase 3 — Request Tracking and Cancellation

### Task 21 — Build the request list paging state

**Depends on:** Task 17 and Checkpoint B

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestListViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestListViewModelTest.kt` (new)

**Work:**

- Add independent Current/History selection, pagination, refresh, append retry,
  and stale-response suppression.
- Preserve server ordering and deduplicate by request ID.

**Acceptance criteria:**

- Every load sends the selected server filter.
- Filter switching cannot leak pages/results.
- Refresh and append failures preserve usable content appropriately.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryOrderRequestListViewModelTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 22 — Render Current and History requests

**Depends on:** Task 21

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestListScreen.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/accessories/components/AccessoryOrderRequestCard.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestListScreenTest.kt` (new)

**Work:**

- Render segmented Current/History state and paginated request cards.
- Show request number, status, subtotal, timing, and concise next-step guidance.
- Cover loading, empty, retry, refresh, and append error.

**Acceptance criteria:**

- Request cards cannot be mistaken for completed purchases.
- Status remains understandable without color.
- Empty Current and empty History have distinct guidance.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Checkpoint C1 — Request list review

- [ ] Current/History use independent server-filtered paging state.
- [ ] Cards preserve the non-purchase meaning of requests.
- [ ] Loading, empty, retry, and append states are covered.

### Task 23 — Build request detail and cancellation state

**Depends on:** Task 17 and Checkpoint C1

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestDetailViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestDetailViewModelTest.kt` (new)

**Work:**

- Load/refresh owned detail and derive actions from server status.
- Add confirmation-backed, single-flight pending cancellation.
- Refresh on `ORDER_REQUEST_NOT_ACTIONABLE`.

**Acceptance criteria:**

- Only pending exposes cancel.
- Cancellation uses the returned resource and handles idempotent success.
- Accepted emits only the typed order ID; unknown states fail closed.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*AccessoryOrderRequestDetailViewModelTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 24 — Render request detail

**Depends on:** Task 23

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestDetailScreen.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestDetailScreenTest.kt` (new)

**Work:**

- Render immutable items/snapshots, subtotal, declaration, resolution fields,
  terminal reasons, and optional accepted order summary.
- Add pending cancel confirmation and accepted View Order action.

**Acceptance criteria:**

- Detail never reads live cart/catalog to reconstruct snapshots.
- Pending/accepted/rejected/cancelled/unknown actions match policy.
- Ownership-safe not-found and large-content scrolling are covered.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 25 — Wire request destinations and order handoff

**Depends on:** Tasks 22 and 24

**Likely files (4):**

- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientFeatureIntent.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt`

**Work:**

- Connect catalog Requests, conflict recovery, list cards, and request detail.
- Navigate accepted request to existing `OpticalOrderDetail(orderId)`.
- Preserve request IDs through limited-account link restoration.

**Acceptance criteria:**

- Every request destination is active-link-gated before loading.
- Accepted handoff makes no prefetch or ID inference.
- Back navigation returns to the correct request filter/list state.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*PatientFeatureIntentTest" --tests "*PatientRouteAccessTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 4 files

### Checkpoint C — Request lifecycle review

- [ ] Current/History filters and pagination are server-backed.
- [ ] Request snapshots remain immutable and independent of the catalog.
- [ ] Pending cancellation and accepted order handoff behave correctly.
- [ ] Unknown/404/422 states expose no unsafe action.
- [ ] Focused tests, Android-test compilation, and build pass.

## Phase 4 — Additive Optical Order Payment Contract

### Task 26 — Extend Optical Order DTOs and domain models

**Depends on:** Checkpoint C

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/data/remote/dto/OpticalOrderDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/OpticalOrder.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/OpticalOrderDtosTest.kt`

**Work:**

- Add pending-payment/payment-review statuses, deadline, proof status/reason,
  GCash instructions, and proof result DTO/domain values.
- Keep fields additive/default-safe for existing fixtures.

**Acceptance criteria:**

- Exact instruction/proof schemas and money decode correctly.
- New and legacy statuses map with an unknown fallback.
- Existing order/rating fixtures remain green.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderDtosTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Task 27 — Map additive payment fields through the repository

**Depends on:** Task 26

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/data/repository/OpticalOrderRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/OpticalOrderRepositoryImplTest.kt`

**Work:**

- Map every new order/proof/instruction value at the repository boundary.
- Preserve existing pagination, payment summary, images, and ratings.

**Acceptance criteria:**

- Both new statuses and every proof status map safely.
- Nullable instruction/deadline/reason values remain nullable.
- Existing repository tests show no behavior regression.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderRepositoryImplTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Checkpoint D1 — Optical Order mapping compatibility

- [ ] Old and new DTO fixtures decode.
- [ ] Repository mapping preserves existing images, payments, and ratings.
- [ ] New nullable fields remain additive.

### Task 28 — Define payment and tracker presentation policy

**Depends on:** Task 27 and Checkpoint D1

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearPresentation.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearPresentationTest.kt`

**Work:**

- Add labels/colors/action capabilities for new statuses/proof states.
- Define conditional legacy versus accessory tracker selection.
- Add offset-aware deadline/countdown policy without local cancellation.

**Acceptance criteria:**

- Legacy orders retain the four-stage tracker.
- Accessory payment participants use payment-through-pickup progression.
- Unknown and expired-display states fail closed and refresh rather than mutate.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*EyewearPresentationTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 29 — Render new statuses in the order list

**Depends on:** Task 28

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderListScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/OpticalOrderListScreenTest.kt`

**Work:**

- Add pending-payment and payment-review card presentation.
- Preserve Current/History pagination and existing fulfillment cards.

**Acceptance criteria:**

- New statuses are readable and remain in Current.
- Existing status cards and filters do not regress.
- Status meaning never relies on color alone.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Checkpoint D — Additive order compatibility review

- [ ] Old and new order fixtures decode and map.
- [ ] Tracker selection does not alter legacy order meaning.
- [ ] Missing instructions never produce hardcoded GCash details.
- [ ] Existing order list/detail/rating tests remain green.
- [ ] Build passes.

## Phase 5 — Payment Proof Upload

### Task 30 — Inspect and validate selected proof images

**Depends on:** Checkpoint D

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/PaymentProofInspector.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/eyewear/PaymentProofInspectorTest.kt` (new)

**Work:**

- Inspect MIME, readable bytes, size, and image bounds via `ContentResolver`.
- Avoid full bitmap decode and reject invalid files before network submission.

**Acceptance criteria:**

- JPG/JPEG/PNG, 5 MB, and 8,000-pixel boundaries are tested exactly.
- Unreadable/malformed images fail with patient-safe errors.
- Validation logs no URI or file metadata.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*PaymentProofInspectorTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 31 — Add the payment-proof multipart operation

**Depends on:** Tasks 26 and 30

**Likely files (5):**

- `app/src/main/java/com/eyecare/app/data/remote/api/OpticalOrderApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/OpticalOrderRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/OpticalOrderRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/OpticalOrderRepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`

**Work:**

- Add multipart proof upload with exact part names.
- Use a temporary cache file and delete it in `finally`.
- Accept both `201` and idempotent `200` proof summaries and govern the route.

**Acceptance criteria:**

- MockWebServer observes `proof`, `sender_name`, and `reference_number` only.
- Both success codes map identically.
- Temp cleanup occurs after success and failure; stable errors survive intact.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderRepositoryImplTest" --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 5 files

### Checkpoint E1 — Proof validation and transport

- [ ] Local file limits and malformed-input behavior pass.
- [ ] Multipart names, success codes, and cleanup are proven.
- [ ] Proof response mapping exposes no private storage metadata.

### Task 32 — Add proof-upload state to order detail

**Depends on:** Task 31 and Checkpoint E1

**Likely files (2):**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailViewModelTest.kt`

**Work:**

- Retain loaded order while selecting, validating, uploading, and refreshing.
- Validate sender/reference length and single-flight upload.
- Refresh after `200`/`201`, expiry, or wrong-state response.

**Acceptance criteria:**

- Success always refetches the order rather than inventing payment review.
- `429` retains form state and exposes Retry-After guidance.
- Existing/rejected proof and non-pending orders cannot submit.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderDetailViewModelTest"
.\gradlew assembleDebug
```

**Estimated scope:** S — 2 files

### Task 33 — Render GCash instructions and proof form

**Depends on:** Task 32

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/PaymentProofForm.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreenTest.kt`

**Work:**

- Render only returned GCash details, exact balance, order reference, and
  nullable deadline.
- Launch the system document picker and connect the validated proof form.
- Render payment-review/existing/rejected-proof states without replacement.

**Acceptance criteria:**

- Null instructions show guidance and no upload form.
- Existing or rejected proof offers no replace/delete/resubmit action.
- Large text, IME, loading, validation, and TalkBack states are covered.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Task 34 — Reconcile deadline and resume refresh

**Depends on:** Task 33

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailViewModelTest.kt`

**Work:**

- Refresh detail on resume and when the visible payment deadline elapses.
- Prevent overlapping refresh/upload jobs and stale response rollback.
- Remove countdown in payment review.

**Acceptance criteria:**

- Deadline produces refresh, never a locally manufactured cancellation.
- Returning after staff review updates proof/order state.
- Payment review has no expiry countdown or upload affordance.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderDetailViewModelTest"
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Checkpoint E — Payment proof review

- [ ] Local file and form validation cover all boundaries.
- [ ] Multipart names/content and temporary cleanup are proven.
- [ ] `200`, `201`, `422`, `429`, and refresh paths are tested.
- [ ] No replace/resubmit path exists after a proof.
- [ ] Focused tests, Android-test compilation, and build pass.

## Phase 6 — Fulfillment, Rating, and Navigation Completion

### Task 35 — Complete the accessory fulfillment tracker

**Depends on:** Checkpoint E

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearPresentation.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreenTest.kt`

**Work:**

- Render payment, review, queue, preparation, ready, pickup, and cancellation
  guidance according to pure policy.
- Keep the legacy tracker for non-accessory payment participants.
- Preserve `is_rateable` as the sole rating capability.

**Acceptance criteria:**

- No client state transition is inferred from time or previous screen state.
- Legacy orders remain visually/semantically unchanged.
- Rating is absent when `is_rateable` is false in every status.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*EyewearPresentationTest" --tests "*FrameRatingViewModelTest"
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Task 36 — Finish route governance and protected navigation coverage

**Depends on:** Tasks 25, 31, and 35

**Likely files (5):**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/BottomNavVisibilityTest.kt`

**Work:**

- Normalize accessory/request/proof path variables and prove all seven new
  annotations are approved active-link routes.
- Cover limited-account restoration for every typed destination.
- Confirm subdestinations hide bottom navigation consistently.

**Acceptance criteria:**

- Discovered production routes contain no rejected/unapproved route.
- Every accessory destination fails closed without an active link.
- Existing route and bottom-navigation tests remain green.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest" --tests "*PatientFeatureIntentTest" --tests "*PatientRouteAccessTest" --tests "*BottomNavVisibilityTest"
.\gradlew assembleDebug
```

**Estimated scope:** M — 5 files

### Task 37 — Verify the complete navigation journey

**Depends on:** Tasks 35 and 36

**Likely files (3):**

- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Work:**

- Verify Home -> catalog -> detail -> cart -> checkout -> request -> order.
- Verify Back, post-link restoration, My Orders entry, and refresh-on-return.
- Remove any temporary/incomplete navigation callback.

**Acceptance criteria:**

- Each hop uses a typed route ID and makes no prefetch join.
- Back navigation does not duplicate roots or lose the MainGraph cart early.
- Limited-to-linked restoration reaches the originally requested destination.

**Verify:**

```powershell
.\gradlew testDebugUnitTest --tests "*PatientFeatureIntentTest" --tests "*PatientRouteAccessTest"
.\gradlew compileDebugAndroidTestKotlin
.\gradlew assembleDebug
```

**Estimated scope:** M — 3 files

### Checkpoint F — End-to-end behavior review

- [ ] The complete patient journey is navigable with authoritative refreshes.
- [ ] Legacy eyewear and rating behavior remains intact.
- [ ] Active-link restoration works for every new destination.
- [ ] Route governance includes exactly the intended endpoints.
- [ ] Focused tests, Android-test compilation, and build pass.

## Phase 7 — Hardening and Handoff

### Task 38 — Complete accessibility and resilient-state coverage

**Depends on:** Checkpoint F

**Likely files (4):**

- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryCatalogScreenTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryCartScreenTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/accessories/AccessoryOrderRequestDetailScreenTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreenTest.kt`

**Work:**

- Cover large text, TalkBack labels/order, long values, compact viewports,
  keyboard behavior, loading/error/unknown states, and 48dp targets.
- Verify status and warnings do not depend on color.

**Acceptance criteria:**

- Critical content/actions remain reachable with large fonts and IME.
- Non-binding request, discount, expiry, and proof-review messages have
  accessible text semantics.
- No critical state clips on compact test dimensions.

**Verify:**

```powershell
.\gradlew compileDebugAndroidTestKotlin
.\gradlew connectedDebugAndroidTest
.\gradlew assembleDebug
```

**Estimated scope:** M — 4 files

### Task 39 — Run final gates and close V23 documentation

**Depends on:** Task 38

**Likely files (4):**

- `CONTEXT.md`
- `docs/specs/backend-alignment-v23-mobile-accessory-order-requests-2026-09-20-spec.md`
- `docs/specs/backend-alignment-v23-mobile-accessory-order-requests-2026-09-20-plan.md`
- `docs/specs/backend-alignment-v23-mobile-accessory-order-requests-2026-09-20-tasks.md`

**Work:**

- Run formatting/checks, full unit tests, Android-test compilation, lint, and
  debug build; run connected tests when infrastructure is available.
- Search for prohibited params, prescription coupling, sensitive logging, and
  unintended persistence.
- Update living docs only after verification matches the shipped behavior.

**Acceptance criteria:**

- Unit tests, lint, and debug build pass.
- Device-only limitations are documented without claiming an unrun pass.
- Static searches find no prohibited behavior or sensitive proof logging.

**Verify:**

```powershell
rg -n "placement|prescription.*accessor|accessor.*prescription" app/src/main
rg -n "Log\..*(sender|reference|proof|uri)|println\(.*(sender|reference|proof|uri)" app/src/main
.\gradlew ktlintCheck
.\gradlew testDebugUnitTest
.\gradlew compileDebugAndroidTestKotlin
.\gradlew lintDebug
.\gradlew assembleDebug
.\gradlew connectedDebugAndroidTest
```

**Estimated scope:** M — 4 documentation files plus verification

### Checkpoint G — Implementation approval gate

- [ ] Review the complete diff against the approved spec and plan.
- [ ] Review test evidence and environment limitations.
- [ ] Confirm user-owned changes remain preserved.
- [ ] Commit only after an explicit user request.

## Definition of Done

- A linked patient can browse active accessories, manage a local cart, and
  submit a non-binding multi-item Order Request.
- Catalog requests omit Brand/Category/placement and expose no sensitive stock
  or internal data.
- Cart and checkout enforce all limits, send no client money, and preserve
  drafts on recoverable failure.
- Current/History request tracking, pending cancellation, and accepted order
  handoff use server-authoritative resources.
- Accepted accessory orders support exact GCash instructions, one proof,
  payment review, fulfillment tracking, and clinic pickup.
- Legacy Optical Orders keep their existing tracker while accessory payment
  participants use the extended tracker.
- Rating remains available only when an item has `is_rateable: true`.
- Every new route is active-link-gated and restored through the link hub.
- Prescriptions remain clinically separate and unchanged by accessory ordering.
- Focused/full tests, Android-test compilation, lint, and build gates pass, with
  unavailable device-only verification documented accurately.
