# Spec: Android Backend Alignment

Status: Approved — implementing
Phase: Tasks

## Objective

Align the Android app's data layer (DTOs, domain models, API services, Room cache) with the actual backend API responses. The app was built against the original MVP spec which had several assumptions that diverged from the final backend implementation. This spec corrects those mismatches so the app can successfully communicate with the backend without runtime deserialization errors or incorrect UI state.

## Discrepancies Summary

| # | Area | Current Android State | Actual Backend |
|---|------|----------------------|----------------|
| 1 | Auth response | Wraps in `{data: {token, user}}` | Returns `{token, user}` flat |
| 2 | Product — `price` field | Product has `price` | No price on product — only on variants |
| 3 | Product — `dimensions` | Product/variant has `dimensions` | Removed. Variants have `attributes` (JSON) |
| 4 | Product — `product_type` | Not in DTO | Backend returns `product_type` |
| 5 | Product — images | `ImageDto` with `id, path, is_primary, sort_order` | JSON array of path strings (no metadata) |
| 6 | Variant — missing fields | Only `id, name, sku, price, dimensions, ar_eligible, ar_asset_reference` | Also has `compare_at_price`, `attributes`, `images` |
| 7 | Products pagination | `ProductListResponse` = `{data: [...]}` | Paginated: `{data: [...], links: {...}, meta: {...}}` |
| 8 | Orders pagination | `OrderListResponse` = `{data: [...]}` | Paginated: `{data: [...], links: {...}, meta: {...}}` |
| 9 | Order statuses | `REQUESTED, UNDER_REVIEW, CONFIRMED, PREPARING, READY_FOR_PICKUP, COMPLETED, CANCELLED` | `requested, confirmed, processing, ready_for_pickup, completed, cancelled` — no `under_review`/`preparing` |
| 10 | Order item — `lens_type_id` | Non-nullable `Int` | Nullable — omit for non-lens items |
| 11 | Order item — `lens_type_name` | Non-nullable `String` | Nullable |
| 12 | Billing statuses | `DRAFT, ISSUED, PARTIALLY_PAID, PAID, VOIDED` | `issued, partially_paid, paid, voided` — no `draft` |
| 13 | Appointment — `assigned_staff` | Not in DTO | Backend returns `assigned_staff: {id, name}` |
| 14 | Conversation DTO | Has `staff_id, appointment_id, order_id, subject` | Only has `customer_id` |
| 15 | POST /conversations | Exists in API service | Not a real endpoint — conversations auto-exist per customer |
| 16 | Message sending with context | No context support in send request | Backend accepts `contexts[]` array in POST message body |
| 17 | Room `ProductEntity` | Has `price`, `dimensions` columns | Needs `product_type`, remove `price`/`dimensions` |

## Changes Required

### 1. Auth Response Structure

**File:** `AuthDtos.kt`

```
BEFORE: AuthResponse → data: AuthData { token, user }
AFTER:  AuthResponse { token, user }  (flat — no wrapper)
```

Remove `AuthResponse` wrapper. The login/register endpoints return `{token, user}` directly.

Note: `GET /user` still returns `{data: {id, name, email, role}}` — `UserResponse` stays as-is.

### 2. Product DTOs

**File:** `ProductDtos.kt`

- Remove `price` and `dimensions` from `ProductDto`
- Add `product_type: String` to `ProductDto`
- Change `images: List<ImageDto>` → `images: List<String>` (just paths)
- Remove `ImageDto` class entirely
- On `VariantDto`: remove `dimensions`, add `attributes: JsonElement?`, `compare_at_price: String?`, `images: List<String>`
- Update `ProductListResponse` to handle pagination (add `links`, `meta`)

### 3. Product Domain Model

**File:** `domain/model/Product.kt`

- Remove `price` and `dimensions` from `Product`
- Add `productType: String`
- Change `images: List<ProductImage>` → `images: List<String>`
- Remove `ProductImage` data class
- On `ProductVariant`: remove `dimensions`, add `attributes: Map<String, String>?`, `compareAtPrice: String?`, `images: List<String>`

### 4. Product Pagination

**Files:** `ProductDtos.kt`, `ProductApiService.kt`, `ProductRepositoryImpl.kt`, `ProductListViewModel.kt`

- Add `PaginatedProductResponse` with `data`, `links`, `meta`
- Add `page` query parameter to `getProducts()`
- Repository returns paginated results; ViewModel supports load-more

### 5. Order Statuses

**File:** `domain/model/Order.kt`

```
BEFORE: REQUESTED, UNDER_REVIEW, CONFIRMED, PREPARING, READY_FOR_PICKUP, COMPLETED, CANCELLED
AFTER:  REQUESTED, CONFIRMED, PROCESSING, READY_FOR_PICKUP, COMPLETED, CANCELLED
```

Remove `UNDER_REVIEW` and `PREPARING`. Add `PROCESSING`.

### 6. Order Item Nullability

**Files:** `OrderDtos.kt`, `domain/model/Order.kt`

- `lensTypeId: Int` → `lensTypeId: Int?`
- `lensTypeName: String` → `lensTypeName: String?`
- Same on `OrderItemRequest`: `lensTypeId: Int` → `lensTypeId: Int?`

### 7. Orders Pagination

**Files:** `OrderDtos.kt`, `OrderApiService.kt`, `OrderRepositoryImpl.kt`, `OrderListViewModel.kt`

- Add `PaginatedOrderResponse` with `data`, `links`, `meta`
- Add `page` query parameter to `getOrders()`
- ViewModel supports load-more

### 8. Billing Statuses

**File:** `domain/model/Billing.kt`

```
BEFORE: DRAFT, ISSUED, PARTIALLY_PAID, PAID, VOIDED
AFTER:  ISSUED, PARTIALLY_PAID, PAID, VOIDED
```

Remove `DRAFT`. Default/fallback in `from()` should be `ISSUED`.

### 9. Appointment — Assigned Staff

**Files:** `AppointmentDtos.kt`, `domain/model/Appointment.kt`

- Add `assigned_staff: AssignedStaffDto?` to `AppointmentDto`
- Add `AssignedStaffDto { id: Int, name: String }`
- Add `assignedStaff: AssignedStaff?` to domain `Appointment`
- Add `AssignedStaff(id: Int, name: String)` domain model

### 10. Conversation DTO Simplification

**File:** `MessageDtos.kt`

- Remove `staff_id`, `appointment_id`, `order_id`, `subject` from `ConversationDto`
- Add `customer_id: Int?`
- Conversation domain model: remove `appointmentId`, `orderId`, `subject`; keep `id`, `customerId`, `createdAt`

### 11. Remove POST /conversations

**Files:** `ConversationApiService.kt`, `ChatRepository`, `MessageDtos.kt`

- Remove `createConversation()` endpoint and `CreateConversationRequest`
- Conversations auto-exist per customer; `GET /conversations` returns the single one

### 12. Message Context Links

**Files:** `ConversationApiService.kt`, `MessageDtos.kt`, `ChatViewModel.kt`

- `POST /conversations/{id}/messages` body accepts optional `contexts[]` array
- Update `SendMessageRequest` to include `contexts: List<ContextLink>?`
- `ContextLink { type: String, id: Int }` — type is `appointment`, `order`, or `product`

### 13. Room ProductEntity Update

**File:** `data/local/entity/ProductEntity.kt`, `ProductDao.kt`, `EyecareDatabase.kt`

- Remove `price` and `dimensions` columns
- Add `productType` column
- Bump Room schema version (1 → 2) with destructive migration (cache-only data)

## Task Breakdown

### Task 1: Fix Auth Response Structure

**Description:** Remove the `data` wrapper from `AuthResponse`. Backend returns `{token, user}` flat.

**Acceptance criteria:**
- [x] `AuthResponse` deserializes `{"token": "...", "user": {...}}` directly (no nested `data` field)
- [x] `AuthRepositoryImpl.safeCall` reads `response.token` and `response.user` instead of `response.data.token`
- [x] `GET /user` still works via `UserResponse` with its `{data: ...}` wrapper (unchanged)
- [x] App compiles without errors

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `data/remote/dto/AuthDtos.kt` — remove `AuthResponse` wrapper, rename `AuthData` → `AuthResponse`
- `data/repository/AuthRepositoryImpl.kt` — update `response.data.token` → `response.token`, `response.data.user` → `response.user`

**Scope:** S

---

### Task 2: Fix Product DTOs and Domain Model

**Description:** Align product DTOs and domain models with the actual backend shape: no product-level price/dimensions, images are string arrays, variants have attributes/compare_at_price/images, products have product_type.

**Acceptance criteria:**
- [x] `ProductDto` has no `price` or `dimensions` fields
- [x] `ProductDto` has `product_type: String` field (with `@SerialName("product_type")`)
- [x] `ProductDto.images` is `List<String>` (not `List<ImageDto>`)
- [x] `ImageDto` class is removed entirely
- [x] `VariantDto` has no `dimensions` field
- [x] `VariantDto` has `attributes: JsonElement? = null`, `@SerialName("compare_at_price") compareAtPrice: String? = null`, `images: List<String> = emptyList()`
- [x] Domain `Product` has no `price`/`dimensions`, has `productType: String`, `images: List<String>`
- [x] Domain `ProductVariant` has no `dimensions`, has `attributes: Map<String, String>? = null`, `compareAtPrice: String? = null`, `images: List<String> = emptyList()`
- [x] `ProductImage` data class is removed
- [x] `ProductRepositoryImpl` mapping functions updated (no `toDisplayString`, no `ImageDto.toDomain`)

**Verify:** `./gradlew assembleDebug` succeeds (may have UI compile errors addressed in Task 11).

**Files:**
- `data/remote/dto/ProductDtos.kt`
- `domain/model/Product.kt`
- `data/repository/ProductRepositoryImpl.kt`

**Scope:** M

---

### Task 3: Add Product Pagination

**Description:** Backend returns paginated products with `data`, `links`, `meta`. Support `?page=N&per_page=N` query params and load-more in the ViewModel.

**Acceptance criteria:**
- [x] `ProductListResponse` replaced with `PaginatedProductResponse` containing `data: List<ProductDto>`, `meta: PaginationMeta`
- [x] `PaginationMeta` has `current_page`, `last_page`, `per_page`, `total`
- [x] `ProductApiService.getProducts()` accepts `@Query("page") page: Int = 1` and `@Query("per_page") perPage: Int = 15`
- [x] `ProductRepository.getProducts()` signature updated to accept `page: Int = 1`
- [x] `ProductRepositoryImpl` passes page param, still caches page 1 results in Room
- [x] `ProductListViewModel` supports `loadMore()` — appends next page results; tracks `hasMorePages`

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `data/remote/dto/ProductDtos.kt` — add `PaginatedProductResponse`, `PaginationMeta`
- `data/remote/api/ProductApiService.kt` — add page/per_page query params
- `domain/repository/ProductRepository.kt` — update signature
- `data/repository/ProductRepositoryImpl.kt` — pass page, cache logic
- `presentation/catalog/ProductListViewModel.kt` — add pagination state and `loadMore()`

**Scope:** M

---

### Task 4: Fix Order Statuses and Item Nullability

**Description:** Correct the `OrderStatus` enum to match backend values and make `lensTypeId`/`lensTypeName` nullable.

**Acceptance criteria:**
- [x] `OrderStatus` enum: remove `UNDER_REVIEW` and `PREPARING`, add `PROCESSING`
- [x] `OrderStatus.from()` maps `"processing" -> PROCESSING`; fallback remains `REQUESTED`
- [x] `OrderItemDto.lensTypeId` is `Int?` (nullable), `lensTypeName` is `String?`
- [x] Domain `OrderItem.lensTypeId` is `Int?`, `lensTypeName` is `String?`
- [x] `OrderItemRequest.lensTypeId` is `Int?` (nullable — omit for non-lens items)
- [x] `OrderRequestViewModel` handles null `lensTypeId` when lens not selected

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `domain/model/Order.kt` — fix enum, fix `OrderItem` nullability
- `data/remote/dto/OrderDtos.kt` — fix `OrderItemDto` and `OrderItemRequest` nullability
- `presentation/orders/OrderRequestViewModel.kt` — handle nullable lens type

**Scope:** S

---

### Task 5: Add Order Pagination

**Description:** Backend returns paginated orders with `data`, `links`, `meta`. Support `?page=N` and load-more.

**Acceptance criteria:**
- [x] `OrderListResponse` replaced with `PaginatedOrderResponse` containing `data: List<OrderDto>`, `meta: PaginationMeta`
- [x] `OrderApiService.getOrders()` accepts `@Query("page") page: Int = 1` and `@Query("per_page") perPage: Int = 15`
- [x] `OrderRepository.getOrders()` accepts `page: Int = 1`
- [x] `OrderRepositoryImpl` passes page param
- [x] `OrderListViewModel` supports `loadMore()` — appends next page; tracks `hasMorePages`

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `data/remote/dto/OrderDtos.kt` — add `PaginatedOrderResponse`, reuse `PaginationMeta` or add local
- `data/remote/api/OrderApiService.kt` — add query params
- `domain/repository/OrderRepository.kt` — update signature
- `data/repository/OrderRepositoryImpl.kt` — pass page
- `presentation/orders/OrderListViewModel.kt` — pagination state

**Scope:** M

---

### Task 6: Fix Billing Statuses

**Description:** Remove `DRAFT` from `BillingStatus` enum; change fallback to `ISSUED`.

**Acceptance criteria:**
- [x] `BillingStatus` enum: `ISSUED, PARTIALLY_PAID, PAID, VOIDED` (no `DRAFT`)
- [x] `BillingStatus.from()` fallback is `ISSUED` instead of `DRAFT`
- [x] No other code references `BillingStatus.DRAFT`

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `domain/model/Billing.kt`

**Scope:** S

---

### Task 7: Add Assigned Staff to Appointments

**Description:** Backend returns `assigned_staff: {id, name}` on appointments. Add to DTO and domain model.

**Acceptance criteria:**
- [x] `AppointmentDto` has `@SerialName("assigned_staff") assignedStaff: AssignedStaffDto? = null`
- [x] `AssignedStaffDto(val id: Int, val name: String)` added to `AppointmentDtos`
- [x] Domain `Appointment` has `assignedStaff: AssignedStaff? = null`
- [x] `AssignedStaff(val id: Int, val name: String)` domain model added
- [x] `AppointmentRepositoryImpl.toDomain()` maps the field
- [x] `AppointmentDetailScreen` displays staff name when present

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `data/remote/dto/AppointmentDtos.kt` — add `AssignedStaffDto`, add field to `AppointmentDto`
- `domain/model/Appointment.kt` — add `AssignedStaff`, add field to `Appointment`
- `data/repository/AppointmentRepositoryImpl.kt` — map field
- `presentation/appointments/AppointmentDetailScreen.kt` — show staff name

**Scope:** S

---

### Task 8: Fix Conversation Model and Remove POST /conversations

**Description:** Simplify `ConversationDto` (remove staff_id, appointment_id, order_id, subject). Remove `createConversation` endpoint. Chat always finds existing conversation via GET.

**Acceptance criteria:**
- [x] `ConversationDto` only has: `id`, `customer_id`, `created_at`
- [x] `CreateConversationRequest` class removed from `MessageDtos`
- [x] `ConversationResponse` wrapper removed (not used anymore)
- [x] `ConversationApiService.createConversation()` method removed
- [x] Domain `Conversation` has only: `id`, `customerId: Int?`, `createdAt`
- [x] `ChatRepositoryImpl.getOrCreateConversation()` → renamed to `getConversation()`: only calls GET, returns first result or error
- [x] `ChatRepository` interface updated accordingly
- [x] `ChatViewModel` handles case where no conversation exists (show "send first message" state or error)

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `data/remote/dto/MessageDtos.kt` — simplify `ConversationDto`, remove `CreateConversationRequest`, remove `ConversationResponse`
- `data/remote/api/ConversationApiService.kt` — remove `createConversation()`
- `domain/model/Message.kt` — simplify `Conversation`
- `domain/repository/ChatRepository.kt` — rename method
- `data/repository/ChatRepositoryImpl.kt` — simplify, remove create logic
- `presentation/messaging/ChatViewModel.kt` — update to use new method name

**Scope:** M

---

### Task 9: Add Message Context Links

**Description:** Backend accepts `contexts[]` array when sending messages. Update DTO and chat flow to support attaching appointment/order/product context.

**Acceptance criteria:**
- [x] `SendMessageRequest` has `contexts: List<ContextLinkDto>? = null`
- [x] `ContextLinkDto(val type: String, val id: Int)` added to `MessageDtos`
- [x] `ChatRepository.sendMessage()` accepts optional `contexts: List<ContextLinkDto>? = null`
- [x] `ChatRepositoryImpl` passes contexts in the request body
- [x] `ChatViewModel.sendContextMessage()` passes context links instead of embedding text
- [x] `ChatRepository.sendContextMessage()` removed or unified with `sendMessage()`

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `data/remote/dto/MessageDtos.kt` — add `ContextLinkDto`, update `SendMessageRequest`
- `domain/repository/ChatRepository.kt` — update `sendMessage` signature, remove `sendContextMessage`
- `data/repository/ChatRepositoryImpl.kt` — pass contexts, remove duplicate method
- `presentation/messaging/ChatViewModel.kt` — use updated sendMessage with contexts

**Scope:** S

---

### Task 10: Update Room Cache Schema

**Description:** Align `ProductEntity` with the new product shape. Remove price/dimensions, add productType. Bump schema version with destructive migration (cache-only data, safe to destroy).

**Acceptance criteria:**
- [x] `ProductEntity` has no `price` or `dimensions` columns
- [x] `ProductEntity` has `productType: String` column
- [x] `EyecareDatabase` version = 2
- [x] Destructive migration via `.fallbackToDestructiveMigration()` in `DatabaseModule`
- [x] `ProductRepositoryImpl.toEntity()` maps `productType`, removes price/dimensions
- [x] `ProductRepositoryImpl.toDomain()` from entity maps correctly to new domain model

**Verify:** `./gradlew assembleDebug` succeeds.

**Files:**
- `data/local/entity/ProductEntity.kt` — remove price/dimensions, add productType
- `data/local/EyecareDatabase.kt` — version = 2
- `di/DatabaseModule.kt` — add fallbackToDestructiveMigration
- `data/repository/ProductRepositoryImpl.kt` — update entity mapping

**Scope:** S

---

### Task 11: Update UI References

**Description:** Fix all presentation-layer compile errors from removed/changed model fields. This is the final cleanup task.

**Acceptance criteria:**
- [x] No references to `product.price` — price comes from first variant or variant selector
- [x] No references to `product.dimensions` or `variant.dimensions`
- [x] No references to `ProductImage` class — images are `List<String>` (direct URL paths)
- [x] No references to `OrderStatus.UNDER_REVIEW` or `OrderStatus.PREPARING`
- [x] No references to `BillingStatus.DRAFT`
- [x] `ProductListScreen`/`ProductCard` shows price from `product.variants.firstOrNull()?.price`
- [x] `ProductDetailScreen` uses `variant.attributes` for display instead of `dimensions`
- [x] Image loading uses string URLs directly (no `.path` accessor)
- [x] `StatusTimeline` in orders uses correct statuses: requested → confirmed → processing → ready_for_pickup → completed
- [x] `ArViewModel` loads frame from `variant.arAssetReference` (unchanged) but no longer references `dimensions`
- [x] All screens compile and render correctly

**Verify:** `./gradlew assembleDebug` succeeds with zero errors.

**Files:**
- `presentation/catalog/ProductListScreen.kt`
- `presentation/catalog/ProductDetailScreen.kt`
- `presentation/catalog/components/ProductCard.kt`
- `presentation/orders/OrderDetailScreen.kt`
- `presentation/orders/components/StatusTimeline.kt`
- `presentation/billing/BillingDetailScreen.kt`
- `presentation/home/HomeViewModel.kt`
- `presentation/ar/ArViewModel.kt`
- `presentation/common/ImageUrlHelper.kt`

**Scope:** M

---

## Implementation Order & Dependencies

```
Task 1 (Auth)          ─── independent, start first
Task 2 (Product DTOs)  ─── independent, can parallel with Task 1
Task 4 (Order Status)  ─── independent, can parallel with Task 1 & 2
Task 6 (Billing)       ─── independent, can parallel with all above
Task 7 (Appointments)  ─── independent, can parallel with all above
│
├─ Task 10 (Room)      ─── depends on Task 2 (needs new ProductEntity shape)
├─ Task 3 (Product Pagination) ─── depends on Task 2 (needs new DTO shape)
├─ Task 5 (Order Pagination)   ─── depends on Task 4 (needs new DTO shape)
│
├─ Task 8 (Conversations)  ─── independent
├─ Task 9 (Context Links)  ─── depends on Task 8 (needs cleaned up MessageDtos)
│
└─ Task 11 (UI References) ─── depends on ALL above (final sweep)
```

**Parallel tracks:**
- Track A: Tasks 1, 2, 10, 3 (Auth → Products → Room → Pagination)
- Track B: Tasks 4, 5 (Orders → Order Pagination)
- Track C: Tasks 6, 7 (Billing + Appointments — both trivial)
- Track D: Tasks 8, 9 (Conversations → Context Links)
- Final: Task 11 (after all tracks converge)

## Verification Checkpoints

1. **After Tasks 1–2:** Auth response deserializes; product DTOs compile with new shape.
2. **After Tasks 3–5:** Pagination wired for products and orders; can load multiple pages.
3. **After Tasks 6–9:** All statuses correct; conversations simplified; context links work.
4. **After Task 10:** Room schema bumped; entity matches domain model.
5. **After Task 11:** `./gradlew assembleDebug` succeeds with zero errors. Full app compiles.

## Success Criteria

- [x] App compiles with zero errors after all changes
- [x] Login/register successfully deserialize the flat `{token, user}` response
- [x] Product list loads with pagination (page 1 auto-loads, scroll loads more)
- [x] Product detail shows `product_type`, variant `attributes`, variant images
- [x] No reference to `ProductImage`, `ImageDto`, or product-level `price`/`dimensions`
- [x] Order list loads with pagination
- [x] Order statuses are `requested → confirmed → processing → ready_for_pickup → completed` (+ `cancelled`)
- [x] Billing status enum has no `DRAFT`
- [x] Appointments show `assigned_staff` name when present
- [x] Chat works without POST /conversations — finds existing conversation via GET
- [x] Messages can include `contexts[]` for linking appointments/orders/products
- [x] Room database migrates cleanly (version 2)

## Boundaries

- **Always:** Keep domain models clean of serialization annotations. Map at repository boundary.
- **Ask first:** If any UI layout changes are needed beyond removing/adding a field display.
- **Never:** Change the backend to match the old spec — we adapt the Android app to the backend.

## Open Questions

None — the backend context document is the source of truth. Proceeding with alignment.
