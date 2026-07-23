# Android Backend Alignment v7: Mobile Catalog and Ordering

**Status:** Complete — approved and implemented July 23, 2026
**Source of truth:** `docs/BACKEND_CONTEXT.md`
**Scope:** Android catalog visibility, product detail actions, and customer order submission

## Objective

Align the Android app with the updated mobile product contract:

- `GET /products` exposes active accessories and browse-only frames with active AR-ready variants.
- `GET /products/{id}` exposes only accessories and AR-capable frames.
- Android customers may submit orders for accessories only.
- Every Android order request must send `is_non_prescription: true`.
- Android order requests must not send `lens_category_id` or `lens_type_id`.
- Contact lenses, optical lenses, legacy `general` products, non-AR frames, and inactive products must not be surfaced from stale local cache.

This contract does **not** change catalog sort values. The backend still documents `name` as the default and supports `newest`, `price_asc`, and `price_desc`.

## Current-State Findings

| Area | Current Android behavior | Contract mismatch |
|---|---|---|
| Product detail | Every in-stock product shows an order button labeled “Order this frame.” | Frames must be browse-only; accessory copy is incorrect. |
| Order request | Frames can choose lens cutting/category; contact lenses and accessories can submit as non-prescription. | Only accessories may enter or submit the order flow. |
| Request DTO | `OrderItemRequest` contains nullable `lens_type_id`. | Both lens field aliases are prohibited in customer requests. |
| Order repository | Caller supplies `isNonPrescription`, including `false`. | Mobile create-order requests must always send JSON boolean `true`. |
| Catalog tabs | “Eye Products” includes every non-frame type. | It must represent accessories only; contact lenses/lenses/general are hidden. |
| Home shelves | Any non-frame/non-service type is treated as retail. | Only frames and accessories may be surfaced. |
| Cache fallback | Cached products are returned without applying the new visibility rules. | Old general/contact-lens/lens/non-AR-frame rows can reappear offline or after a detail 404. |
| Pagination | Tabs filter a mixed server page in memory; an empty selected tab hides “Load More.” | Valid products on later pages can be unreachable. |

`ProductDto.category` is already nullable and maps to an empty domain display value, so no additional serialization fix is required for that field.

## Tech Stack

- Kotlin, coroutines, and `StateFlow`
- Jetpack Compose with Material 3
- MVVM + Clean Architecture
- Retrofit with Kotlinx Serialization
- Hilt
- Room product cache
- JUnit 5, MockK, Turbine, and MockWebServer

## Commands

```powershell
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Run focused tests during implementation, then all three quality gates before handoff.

## Project Structure

Expected implementation touchpoints:

```text
app/src/main/java/com/eyecare/app/
├── data/
│   ├── remote/dto/OrderDtos.kt
│   └── repository/
│       ├── OrderRepositoryImpl.kt
│       └── ProductRepositoryImpl.kt
├── domain/
│   ├── model/Product.kt
│   └── repository/OrderRepository.kt
└── presentation/
    ├── catalog/
    │   ├── ProductDetailScreen.kt
    │   ├── ProductListScreen.kt
    │   └── ProductListViewModel.kt
    ├── home/HomeViewModel.kt
    └── orders/
        ├── OrderRequestScreen.kt
        └── OrderRequestViewModel.kt

app/src/test/java/com/eyecare/app/
├── data/repository/
│   ├── OrderRepositoryImplTest.kt
│   └── ProductRepositoryImplTest.kt
└── presentation/
    ├── catalog/ProductListViewModelTest.kt
    ├── home/HomeViewModelTest.kt
    └── orders/OrderRequestViewModelTest.kt
```

`CONTEXT.md` and the relevant backend-alignment status entry should be updated after implementation and verification.

## Code Style and Contract Policy

Centralize product capability checks instead of repeating case-insensitive string comparisons in screens and view models.

Illustrative shape:

```kotlin
enum class ProductType {
    FRAME,
    ACCESSORY,
    CONTACT_LENS,
    LENS,
    UNKNOWN,
}

val Product.isMobileCatalogVisible: Boolean
    get() = type == ProductType.ACCESSORY ||
        (type == ProductType.FRAME && variants.any(ProductVariant::isArReady))

val Product.isMobileOrderable: Boolean
    get() = type == ProductType.ACCESSORY
```

Unknown or legacy values must fail closed: they are neither visible nor orderable. DTO and Room values must be mapped to domain types at the repository boundary.

If changing `Product.productType` to an enum would cause disproportionate churn, an internal normalized policy object is acceptable, provided all catalog, home, detail, cache, and order checks use that single policy.

## Testing Strategy

Use tests first for each behavior change.

### Unit and serialization coverage

- Product type mapping recognizes the four backend values case-insensitively and maps other values to unknown.
- AR-ready means both `ar_eligible == true` and a non-blank `ar_asset_reference`.
- Network and cache mappings expose:
  - accessories with their returned variants;
  - frames with AR-ready variants only.
- Network/cache policy rejects contact lenses, optical lenses, legacy general products, and non-AR frames.
- A detail API 404 cannot fall back to a cached product that is no longer mobile-visible.
- Product list tabs show frames or accessories only and preserve server order inside each tab.
- Empty selected tabs continue pagination until a matching item is found or the server is exhausted.
- Home shelves include only visible frames and accessories.
- Order route/view model rejects frames, contact lenses, lenses, legacy general, and unknown types.
- Accessory submission always serializes `is_non_prescription: true`.
- Serialized order items contain only `product_variant_id` and `quantity`; neither lens alias appears.
- Historical order response decoding keeps both lens aliases for old records.

### UI behavior coverage

Prefer testing extracted presentation policy where Compose UI tests are not already configured:

- Frame detail: AR action when eligible, no order action, browse-only explanation.
- Accessory detail: order action uses accessory-neutral copy and respects stock.
- Unknown/non-orderable detail: no order navigation.
- Order request: no lens-cutting switch or lens-category selector.

### Manual verification

1. Open a returned AR frame and verify AR remains available while ordering is absent.
2. Open an in-stock accessory and submit an order.
3. Inspect the request body and confirm `is_non_prescription` is `true` and no lens field is present.
4. Try a stale/deep-linked frame order route and verify submission is blocked with a customer-readable message.
5. Verify contact-lens and optical-lens products do not appear after an offline cache fallback.
6. Verify switching tabs can reach matching products located on later API pages.

## Boundaries

### In scope

- Mobile catalog type/variant visibility
- Cache sanitization for the updated visibility contract
- Frame browse-only and accessory orderable UI
- Accessory-only order request state and payload
- Mixed-page tab pagination behavior
- Tests and Android context documentation

### Out of scope

- Backend endpoint or database changes
- Staff/admin order rules
- Historical order response removal of lens aliases
- AR renderer changes
- Appointment `last_reschedule_reason` alignment, which is a separate backend change
- Changes to supported catalog sort values or their default
- A catalog visual redesign unrelated to the contract

## Implementation Plan

### Task 1 — Encode mobile product capabilities in the domain boundary

**Files**

- `domain/model/Product.kt`
- `data/repository/ProductRepositoryImpl.kt`
- `data/repository/ProductRepositoryImplTest.kt`

**Work**

- Introduce a normalized product type/capability policy.
- Map DTO and Room string values at the repository boundary.
- Define AR-ready as eligible plus a non-blank asset reference.
- Sanitize both live and cached products with the same policy.
- Trim frame variants to AR-ready variants; keep accessory variants as returned.
- Prevent hidden cached products from being returned by list or detail fallback.

**Acceptance criteria**

- Only accessories and AR-capable frames cross into mobile catalog presentation.
- Non-AR frame variants never appear in Android frame details.
- Unknown/legacy types fail closed.
- Existing nullable category responses still decode successfully.

**Verify**

```powershell
./gradlew testDebugUnitTest --tests "*ProductRepositoryImplTest"
```

### Task 2 — Align catalog tabs, labels, home shelves, and pagination

**Files**

- `presentation/catalog/ProductListViewModel.kt`
- `presentation/catalog/ProductListScreen.kt`
- `presentation/home/HomeViewModel.kt`
- their unit tests

**Work**

- Rename “Eye Products” to “Accessories.”
- Filter tabs by normalized product type, not “all non-frame.”
- Remove contact-lens category matching from the accessory filter heuristic.
- Preserve backend response order; do not locally sort products.
- When the selected tab has no matches but `meta.last_page` indicates more pages, load forward until a match is found or pagination is exhausted.
- Restrict home shelves to visible frames/accessories and remove legacy general/contact/lens expectations.

**Acceptance criteria**

- Catalog and home never show disallowed types, including from cache.
- Frame and accessory ordering within each view matches their relative server order.
- A valid later-page frame/accessory is reachable even when earlier mixed pages have no match for the selected tab.
- Existing sort controls continue sending documented backend values.

**Verify**

```powershell
./gradlew testDebugUnitTest --tests "*ProductListViewModelTest" --tests "*HomeViewModelTest"
```

### Task 3 — Make frame detail browse-only and accessory detail orderable

**Files**

- `presentation/catalog/ProductDetailScreen.kt`
- optionally a small presentation policy/component test file

**Work**

- Show AR only for an AR-ready frame variant.
- Never show order navigation for frames.
- Add concise browse-only guidance for frames, such as “Try frames virtually; contact the clinic to order.”
- Show the order button only for accessories.
- Replace “Order this frame” with “Order this item” or “Order accessory.”
- Keep out-of-stock behavior for accessories.

**Acceptance criteria**

- No frame UI action can navigate to `OrderRequest`.
- Accessories retain the order path.
- Copy accurately explains frame availability without implying mobile checkout.

**Verify**

- Presentation policy tests plus manual detail-screen checks.

### Task 4 — Make invalid customer order requests unrepresentable

**Files**

- `data/remote/dto/OrderDtos.kt`
- `domain/repository/OrderRepository.kt`
- `data/repository/OrderRepositoryImpl.kt`
- `presentation/orders/OrderRequestViewModel.kt`
- `presentation/orders/OrderRequestScreen.kt`
- order repository/view-model tests

**Work**

- Remove lens fields from the create-order item DTO only; retain response aliases.
- Remove the caller-controlled `isNonPrescription` argument from the customer create-order repository API and hardcode `true` in the request DTO.
- Remove lens type constants, selection state, toggle, validation, and UI from the mobile order request.
- On order-route load, require `productType == accessory`; otherwise emit a non-retryable unavailable/not-orderable state.
- Recheck orderability in `submit()` to protect against stale state or direct route invocation.

**Acceptance criteria**

- Android cannot construct a customer create-order payload with `false`.
- Android cannot serialize either lens alias in a create-order item.
- A frame/contact-lens/lens/general/unknown deep link cannot call `POST /orders`.
- Accessory quantity and optional appointment linking continue to work.

**Verify**

```powershell
./gradlew testDebugUnitTest --tests "*OrderRepositoryImplTest" --tests "*OrderRequestViewModelTest"
```

### Task 5 — Complete regression verification and documentation

**Files**

- `CONTEXT.md`
- this spec’s status/task checklist

**Work**

- Update Android context from the former frame/general ordering flow to accessory-only ordering with browse-only AR frames.
- Record any backend limitations and final verification results.
- Run the full unit, lint, and build gates.

**Acceptance criteria**

- Documentation describes the same contract as `BACKEND_CONTEXT.md`.
- All required gates pass, or unrelated pre-existing failures are documented with exact evidence.

**Verify**

```powershell
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Old Room rows expose removed types while offline. | Apply the same fail-closed visibility policy to cache mapping and detail fallback. |
| Direct/deep navigation bypasses a hidden button. | Validate product type when loading and immediately before submission. |
| Nullable lens request fields serialize as JSON `null` and trigger HTTP 422. | Remove them from the create-request DTO rather than relying on serializer settings. |
| Mixed-type pagination makes a tab look empty too early. | Continue fetching while the selected tab has no match and pages remain. |
| Category names do not reliably identify product type. | Keep category filtering conservative and track the API enhancement below. |
| Future backend product types become accidentally orderable. | Unknown values fail closed. |

## Open Questions

1. `GET /products` has no documented `product_type` filter, and `GET /categories` does not identify which product type owns a category. The Android implementation can safely filter returned products, but category-filter options remain name-based. Recommended backend follow-up: add `?product_type=frame|accessory` and/or return product-type metadata with categories.
2. Confirm the preferred browse-only frame copy. Proposed default: “Available for virtual try-on. Contact the clinic to order.”
3. Confirm whether linking an appointment to an accessory order remains desired. The updated contract still permits nullable `appointment_id`, so this plan preserves it.

None of these questions blocks the safe contract alignment. The recommended defaults are specified above.

## Success Criteria

- Android displays only backend-authorized mobile catalog products.
- Frames remain browsable and AR-capable but cannot enter the customer ordering flow.
- Only accessories can invoke `POST /orders`.
- Every create-order request contains `is_non_prescription: true` and no lens fields.
- Stale cache and deep links cannot bypass these rules.
- Catalog pagination does not strand valid products on later mixed pages.
- Focused tests, full unit tests, lint, and debug assembly pass.

## Review and Verification Result

Approved July 23, 2026 and implemented through isolated, verified commits.

- Focused repository, catalog, home, order, and presentation-policy tests pass.
- Full `testDebugUnitTest` passes.
- `assembleDebug` passes.
- `lintDebug` reaches the report gate but remains blocked by two unrelated pre-existing errors:
  - unused Material 3 `Scaffold` content padding in `MainActivity.kt`;
  - camera permission without the corresponding optional hardware feature in `AndroidManifest.xml`.
