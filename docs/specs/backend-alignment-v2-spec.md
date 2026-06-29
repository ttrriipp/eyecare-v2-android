# Spec: Android Backend Alignment v2

Status: In progress
Phase: Implement

## Objective

Fix remaining misalignments between the Android app and the backend API that were not covered by the first backend-alignment-spec. These gaps are functional — missing endpoints, missing features, incorrect data models, and hardcoded values that should be API-driven.

## Assumptions

1. The backend API is the source of truth (per BACKEND_CONTEXT.md)
2. All endpoints documented are fully implemented and stable
3. We do NOT need to add new backend endpoints — only consume existing ones
4. `GET /visit-reasons` is a public endpoint (confirmed in API routes)
5. `GET /lens-types` is NOT a public endpoint — lens types stay hardcoded in the app
6. Order response includes `billing_id` (nullable)
7. `GET /user` and login/register return `phone` field
8. `GET /billing/{id}` returns `billing_number`, `items[]`, `discount_amount`, `subtotal`, and payments with `reference_number`/`paid_at`

## Discrepancies Summary

| # | Area | Current Android State | Actual Backend |
|---|------|----------------------|----------------|
| 1 | Cancel appointment | No endpoint or UI | `POST /appointments/{id}/cancel` (pending/confirmed only) |
| 2 | Cancel order | No endpoint or UI | `POST /orders/{id}/cancel` (requested only) |
| 3 | Profile update | Read-only profile screen | `PATCH /user` accepts name, email, phone |
| 4 | Mark messages read | No endpoint | `POST /conversations/{id}/messages/read` |
| 5 | Conversation unread_count | `ConversationDto` missing field | `GET /conversations` returns `unread_count` |
| 6 | Billing: billing_number | Not in DTO/domain | Backend returns `billing_number` |
| 7 | Billing: subtotal + discount | Not in DTO/domain | Backend returns `subtotal`, `discount_amount` |
| 8 | Billing: line items | Not in DTO/domain | Backend returns `items[]` array |
| 9 | Billing: order_id | Non-nullable `Int` in DTO | Not in billing API response at all |
| 10 | Feedback: staff_reply | Fields exist in DTO + domain + UI | Intentionally removed from backend |
| 11 | Product variant: in_stock | Not in DTO/domain | Backend returns `in_stock: Boolean` |
| 12 | User: phone | Missing from `UserDto` and domain `User` | Backend returns phone in all user responses |
| 13 | Order: billing_id | Not in DTO/domain | Backend returns `billing_id: Int?` |
| 14 | Order → Billing navigation | Passes `order.id` as billingId (WRONG) | Should use `order.billing_id` |
| 15 | Visit reasons | Hardcoded list + IDs | `GET /visit-reasons` returns `{data: [{id, name, duration_minutes}]}` |
| 16 | Time slots | Hardcoded static list | Should generate from clinic hours (9-17, 30-min) |
| 17 | Home: today excluded | `isAfter(today)` filters out today | Should include today's appointments |

## Changes Required

### Phase 1: Critical Missing Endpoints

#### 1.1 Cancel Appointment
- Add `POST /appointments/{id}/cancel` to `AppointmentApiService`
- Add `cancelAppointment(id)` to repository
- Add cancel button to `AppointmentDetailScreen` (pending/confirmed only)
- Confirmation dialog before cancel

#### 1.2 Cancel Order
- Add `POST /orders/{id}/cancel` to `OrderApiService`
- Add `cancelOrder(id)` to repository
- Add cancel button to `OrderDetailScreen` (requested only)
- Confirmation dialog before cancel

#### 1.3 Profile Update
- Add `phone: String?` to `UserDto` and domain `User`
- Add `PATCH /user` to `AuthApiService`
- Add `updateUser(name, email, phone)` to repository
- Convert `ProfileScreen` to support edit mode with save/cancel

### Phase 2: Messaging

#### 2.1 Mark Messages as Read
- Add `POST /conversations/{id}/messages/read` to `ConversationApiService`
- Call on chat open and after receiving new messages from other party

#### 2.2 Unread Count
- Add `unread_count: Int` to `ConversationDto` and domain `Conversation`
- Show badge on Chat FAB

### Phase 3: Billing Model Fix

#### 3.1 Rewrite BillingDto
Replace current `BillingDto` to match actual API response:
```kotlin
data class BillingDto(
    val id: Int,
    @SerialName("billing_number") val billingNumber: String,
    val status: String,
    val subtotal: String,
    @SerialName("discount_amount") val discountAmount: String,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("amount_paid") val amountPaid: String,
    @SerialName("balance_due") val balanceDue: String,
    @SerialName("issued_at") val issuedAt: String?,
    @SerialName("created_at") val createdAt: String,
    val items: List<BillingItemDto> = emptyList(),
    val payments: List<PaymentDto> = emptyList(),
)
```
- Remove `order_id` (not in response)
- Add `billing_number`, `subtotal`, `discount_amount`
- Add `BillingItemDto` with: `id`, `type`, `description`, `quantity`, `unit_price`, `amount`
- PaymentDto already correct (keeps `reference_number`, `paid_at`)

#### 3.2 Fix Order → Billing Navigation
- Add `billing_id: Int?` to `OrderDto` and domain `Order`
- NavGraph: use `order.billingId` instead of `order.id` for billing navigation
- Hide "View Billing" button when `billingId` is null

### Phase 4: Data Model Cleanup

#### 4.1 Remove staff_reply from Feedback
- Remove `staff_reply` and `replied_at` from DTO, domain, UI

#### 4.2 Add in_stock to Variants
- Add `in_stock: Boolean` to `VariantDto` and domain `ProductVariant`
- Show out-of-stock indicator; disable ordering for OOS variants

#### 4.3 Add phone to User
- Add `phone: String?` to `UserDto` and domain `User`
- Display in profile; include in login/register response parsing

### Phase 5: API-Driven Visit Reasons

#### 5.1 Fetch Visit Reasons
- Add `GET /visit-reasons` to an API service
- Replace hardcoded `VISIT_REASONS` and `REASON_IDS` in booking flow
- Load on booking screen init; show name from API

#### 5.2 Generate Time Slots
- Replace static `TIME_SLOTS` with generated 30-min slots (9:00–17:00)
- Server handles conflict detection — client shows all possible slots

### Phase 6: Minor Fix

#### 6.1 Include Today in Home Upcoming
- Change `isAfter(today)` to `!isBefore(today)` in `HomeViewModel`

## Task Breakdown

### Task 1: Cancel Appointment
- **Files:** `AppointmentApiService.kt`, `AppointmentRepository.kt`, `AppointmentRepositoryImpl.kt`, `AppointmentDetailScreen.kt`, `AppointmentDetailViewModel.kt`
- **Acceptance:** Cancel button on pending/confirmed appointments; confirmation dialog; refreshes to cancelled
- **Verify:** `./gradlew assembleDebug`

### Task 2: Cancel Order
- **Files:** `OrderApiService.kt`, `OrderRepository.kt`, `OrderRepositoryImpl.kt`, `OrderDetailScreen.kt`, `OrderDetailViewModel.kt`
- **Acceptance:** Cancel button on requested orders; confirmation dialog; refreshes to cancelled
- **Verify:** `./gradlew assembleDebug`

### Task 3: Add phone to User + Profile Update
- **Files:** `AuthDtos.kt`, `User.kt`, `AuthApiService.kt`, `AuthRepository.kt`, `AuthRepositoryImpl.kt`, `ProfileScreen.kt`, `ProfileViewModel.kt`
- **Acceptance:** Phone shown in profile; user can edit name/email/phone; 422 validation errors displayed
- **Verify:** `./gradlew assembleDebug`

### Task 4: Mark Messages Read + Unread Badge
- **Files:** `ConversationApiService.kt`, `MessageDtos.kt`, `ChatRepository.kt`, `ChatRepositoryImpl.kt`, `ChatViewModel.kt`, `Message.kt` (Conversation), `SplitBottomNavBar.kt`, `NavGraph.kt`
- **Acceptance:** Messages marked read on chat open; unread badge on FAB; badge clears after viewing
- **Verify:** `./gradlew assembleDebug`

### Task 5: Rewrite Billing Model + UI
- **Files:** `BillingDtos.kt`, `Billing.kt`, `BillingRepositoryImpl.kt`, `BillingDetailScreen.kt`
- **Acceptance:** Shows billing number, line items table, subtotal/discount/total summary, correct payment info; no order_id field
- **Verify:** `./gradlew assembleDebug`

### Task 6: Fix Order → Billing Navigation
- **Files:** `OrderDtos.kt`, `Order.kt`, `OrderRepositoryImpl.kt`, `OrderDetailScreen.kt`
- **Acceptance:** `billing_id` parsed from order; "View Billing" uses billingId; hidden when null
- **Verify:** `./gradlew assembleDebug`

### Task 7: Remove staff_reply from Feedback
- **Files:** `FeedbackDtos.kt`, `Feedback.kt`, `FeedbackRepositoryImpl.kt`, `FeedbackHistoryScreen.kt`
- **Acceptance:** No references to staff_reply/replied_at anywhere
- **Verify:** `./gradlew assembleDebug`; grep confirms zero matches

### Task 8: Add in_stock to Product Variants
- **Files:** `ProductDtos.kt`, `Product.kt`, `ProductRepositoryImpl.kt`, `ProductDetailScreen.kt`, `OrderRequestScreen.kt`
- **Acceptance:** OOS variants show indicator; Order button disabled for OOS
- **Verify:** `./gradlew assembleDebug`

### Task 9: Fetch Visit Reasons from API
- **Files:** `AppointmentApiService.kt` (or new service), `AppointmentDtos.kt`, `BookAppointmentViewModel.kt`, `BookAppointmentScreen.kt`
- **Acceptance:** Visit reasons loaded from `GET /visit-reasons`; names and IDs from API used in booking
- **Verify:** `./gradlew assembleDebug`

### Task 10: Generate Time Slots Dynamically
- **Files:** `BookAppointmentScreen.kt`
- **Acceptance:** Time slots generated as 30-min increments from 9:00-17:00; no hardcoded list
- **Verify:** `./gradlew assembleDebug`

### Task 11: Fix Home Upcoming Appointments Filter
- **Files:** `HomeViewModel.kt`
- **Acceptance:** Today's confirmed/pending appointments appear as upcoming
- **Verify:** `./gradlew assembleDebug`

## Implementation Order

```
Phase 1 (no dependencies between tasks):
  Task 1: Cancel Appointment
  Task 2: Cancel Order
  Task 3: Profile Update + Phone
  Task 11: Fix Home filter (trivial)

Phase 2:
  Task 4: Mark Read + Unread Badge

Phase 3 (billing - sequential):
  Task 5: Rewrite Billing Model
  Task 6: Fix Order → Billing Nav (needs billing_id on Order from Task 6 only)

Phase 4 (cleanup - independent):
  Task 7: Remove staff_reply
  Task 8: Add in_stock

Phase 5 (booking improvements):
  Task 9: Fetch Visit Reasons
  Task 10: Generate Time Slots
```

All tasks unblocked. No backend changes needed.

## Success Criteria

- [ ] Cancel appointment works (pending/confirmed only)
- [ ] Cancel order works (requested only)
- [ ] Profile edit saves name/email/phone via PATCH /user
- [ ] Phone displayed in profile
- [ ] Messages marked as read when chat opens
- [ ] Unread badge shown on Chat FAB
- [ ] Billing shows billing_number, line items, discount/subtotal/total
- [ ] Order → Billing uses billing_id (not order.id)
- [ ] "View Billing" hidden when order has no billing
- [ ] No staff_reply references in code
- [ ] Product variants show in_stock status; OOS disables ordering
- [ ] Visit reasons fetched from API
- [ ] Time slots generated dynamically (30-min from 9:00-17:00)
- [ ] Today's appointments show as upcoming
- [ ] App compiles with zero errors

## Boundaries

- **Always:** Match domain models to backend response. Serialization stays in data layer.
- **Ask first:** Any changes that would require backend modifications.
- **Never:** Break existing working flows. Skip confirmation dialogs on destructive actions.

## Open Questions

None — all resolved by updated BACKEND_CONTEXT.md.
