# Spec: Android Backend Alignment

Status: Draft — awaiting review
Phase: Specify

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

**Acceptance:** Login/register deserialize `{token, user}` correctly. `GET /user` still works with `{data: ...}` wrapper.

**Files:** `AuthDtos.kt`, `AuthRepositoryImpl.kt`

### Task 2: Fix Product DTOs and Domain Model

**Acceptance:** `ProductDto` has `product_type`, no `price`/`dimensions`. Images are `List<String>`. `VariantDto` has `attributes`, `compare_at_price`, `images`. Domain models match.

**Files:** `ProductDtos.kt`, `domain/model/Product.kt`, `ProductRepositoryImpl.kt`

### Task 3: Add Product Pagination

**Acceptance:** `GET /products` supports `?page=N` parameter. Response deserializes `links`/`meta`. ViewModel supports loading more pages.

**Files:** `ProductDtos.kt`, `ProductApiService.kt`, `ProductRepositoryImpl.kt`, `ProductListViewModel.kt`

### Task 4: Fix Order Statuses and Item Nullability

**Acceptance:** `OrderStatus` enum has `PROCESSING` (not `PREPARING`/`UNDER_REVIEW`). `lensTypeId` and `lensTypeName` are nullable throughout.

**Files:** `domain/model/Order.kt`, `OrderDtos.kt`, `OrderRequestViewModel.kt`

### Task 5: Add Order Pagination

**Acceptance:** `GET /orders` supports `?page=N`. Response deserializes pagination metadata. ViewModel supports load-more.

**Files:** `OrderDtos.kt`, `OrderApiService.kt`, `OrderRepositoryImpl.kt`, `OrderListViewModel.kt`

### Task 6: Fix Billing Statuses

**Acceptance:** `BillingStatus` enum has no `DRAFT`. Fallback is `ISSUED`.

**Files:** `domain/model/Billing.kt`

### Task 7: Add Assigned Staff to Appointments

**Acceptance:** `AppointmentDto` deserializes `assigned_staff` object. Domain model exposes `assignedStaff`. UI shows staff name when present.

**Files:** `AppointmentDtos.kt`, `domain/model/Appointment.kt`, `AppointmentRepositoryImpl.kt`, `AppointmentDetailScreen.kt`

### Task 8: Fix Conversation Model and Remove POST /conversations

**Acceptance:** `ConversationDto` only has `id, customer_id, created_at`. No `createConversation` endpoint. Chat flow uses `GET /conversations` to find existing conversation.

**Files:** `MessageDtos.kt`, `ConversationApiService.kt`, `ChatRepositoryImpl.kt`, `domain/model/Message.kt`

### Task 9: Add Message Context Links

**Acceptance:** `SendMessageRequest` supports `contexts[]` array. Chat UI can attach appointment/order/product context to messages.

**Files:** `MessageDtos.kt`, `ConversationApiService.kt`, `ChatViewModel.kt`

### Task 10: Update Room Cache Schema

**Acceptance:** `ProductEntity` has `productType`, no `price`/`dimensions`. Room version bumped. App migrates without crash.

**Files:** `ProductEntity.kt`, `EyecareDatabase.kt`, `ProductDao.kt`, `ProductRepositoryImpl.kt`

### Task 11: Update UI References

**Acceptance:** Any screens referencing removed fields (`product.price`, `product.dimensions`, `ProductImage.isPrimary`, order status `UNDER_REVIEW`/`PREPARING`, billing `DRAFT`) compile and work correctly with updated models.

**Files:** `ProductListScreen.kt`, `ProductDetailScreen.kt`, `OrderDetailScreen.kt`, `BillingDetailScreen.kt`, `HomeViewModel.kt`, `ArViewModel.kt`

## Success Criteria

- [ ] App compiles with zero errors after all changes
- [ ] Login/register successfully deserialize the flat `{token, user}` response
- [ ] Product list loads with pagination (page 1 auto-loads, scroll loads more)
- [ ] Product detail shows `product_type`, variant `attributes`, variant images
- [ ] No reference to `ProductImage`, `ImageDto`, or product-level `price`/`dimensions`
- [ ] Order list loads with pagination
- [ ] Order statuses are `requested → confirmed → processing → ready_for_pickup → completed` (+ `cancelled`)
- [ ] Billing status enum has no `DRAFT`
- [ ] Appointments show `assigned_staff` name when present
- [ ] Chat works without POST /conversations — finds existing conversation via GET
- [ ] Messages can include `contexts[]` for linking appointments/orders/products
- [ ] Room database migrates cleanly (version 2)

## Boundaries

- **Always:** Keep domain models clean of serialization annotations. Map at repository boundary.
- **Ask first:** If any UI layout changes are needed beyond removing/adding a field display.
- **Never:** Change the backend to match the old spec — we adapt the Android app to the backend.

## Open Questions

None — the backend context document is the source of truth. Proceeding with alignment.
