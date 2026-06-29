# Implementation Plan: Backend Alignment v2

## Overview

Implement 11 tasks from `backend-alignment-v2-spec.md` to close all remaining gaps between the Android app and the backend API. Tasks are ordered by dependency graph with vertical slicing — each task delivers a complete working feature.

## Architecture Decisions

- **Cancel actions:** Follow existing mutation pattern (repository `Result<Unit>`, ViewModel state update, UI confirmation dialog + refresh). Same pattern as logout.
- **Unread badge:** Use a shared `ChatRepository` call at NavGraph level via a dedicated `UnreadViewModel` (singleton-scoped). The FAB receives unread count as parameter.
- **Profile edit:** In-place edit mode (toggle) rather than separate screen — keeps navigation simple.
- **Visit reasons:** Fetch once on booking screen init, cache in ViewModel state. No Room caching needed (small dataset, always fresh).
- **Billing rewrite:** Complete DTO replacement — the current model is fundamentally wrong (has `orderId` that doesn't exist in API response).

## Dependency Graph

```
Task 3 (User phone)  ←── Task 3 is prerequisite for profile edit showing phone
    │
    └── no other task depends on phone being added

Task 5 (Billing model)  ←── Task 6 depends on correct billing DTO
    │
    └── Task 6 (Order billing_id + navigation)

All other tasks are independent of each other.
```

## Task List

### Phase 1: Data Model Fixes (foundation — no UI changes yet)

#### Task 1: Add phone to User model + fix Billing DTO + fix Order DTO + remove staff_reply

**Description:** Fix all data layer misalignments in one pass. These are pure model/DTO changes with no UI impact — they set the foundation for subsequent UI tasks.

**Acceptance criteria:**
- [ ] `AuthDtos.UserDto` has `val phone: String? = null`
- [ ] Domain `User` has `val phone: String? = null`
- [ ] `AuthRepositoryImpl.toDomain()` maps phone
- [ ] `BillingDtos.BillingDto` matches API: `billingNumber`, `subtotal`, `discountAmount`, no `orderId`, has `items: List<BillingItemDto>`, `payments: List<PaymentDto>`
- [ ] `BillingItemDto` added: `id`, `type`, `description`, `quantity`, `unitPrice`, `amount`
- [ ] Domain `Billing` updated: `billingNumber`, `subtotal`, `discountAmount`, no `orderId`, has `items` list
- [ ] Domain `BillingItem` added
- [ ] `BillingRepositoryImpl` mapping updated
- [ ] `OrderDtos.OrderDto` has `@SerialName("billing_id") val billingId: Int? = null`
- [ ] Domain `Order` has `val billingId: Int? = null`
- [ ] `OrderRepositoryImpl.toDomain()` maps billingId
- [ ] `ProductDtos.VariantDto` has `@SerialName("in_stock") val inStock: Boolean = true`
- [ ] Domain `ProductVariant` has `val inStock: Boolean = true`
- [ ] `ProductRepositoryImpl` variant mapping includes inStock
- [ ] `FeedbackDtos.FeedbackDto` has no `staffReply`/`repliedAt` fields
- [ ] Domain `Feedback` has no `staffReply`/`repliedAt` fields
- [ ] `FeedbackRepositoryImpl.toDomain()` updated
- [ ] `MessageDtos.ConversationDto` has `@SerialName("unread_count") val unreadCount: Int = 0`
- [ ] Domain `Conversation` has `val unreadCount: Int = 0`
- [ ] `ChatRepositoryImpl.toDomain()` maps unreadCount

**Dependencies:** None

**Verification:** `./gradlew assembleDebug` — expect compile errors in UI layer (BillingDetailScreen references `orderId`, FeedbackHistoryScreen references `staffReply`). These are fixed in Phase 2.

**Files:**
- `data/remote/dto/AuthDtos.kt`
- `domain/model/User.kt`
- `data/repository/AuthRepositoryImpl.kt`
- `data/remote/dto/BillingDtos.kt`
- `domain/model/Billing.kt`
- `data/repository/BillingRepositoryImpl.kt`
- `data/remote/dto/OrderDtos.kt`
- `domain/model/Order.kt`
- `data/repository/OrderRepositoryImpl.kt`
- `data/remote/dto/ProductDtos.kt`
- `domain/model/Product.kt`
- `data/repository/ProductRepositoryImpl.kt`
- `data/remote/dto/FeedbackDtos.kt`
- `domain/model/Feedback.kt`
- `data/repository/FeedbackRepositoryImpl.kt`
- `data/remote/dto/MessageDtos.kt`
- `domain/model/Message.kt`
- `data/repository/ChatRepositoryImpl.kt`

**Scope:** L (many files but each change is 1-3 lines)

---

#### Task 2: Add cancel + profile update + mark-read + visit-reasons API endpoints

**Description:** Wire all new API service methods. No repository logic yet — just Retrofit interface declarations.

**Acceptance criteria:**
- [ ] `AppointmentApiService` has `@POST("appointments/{id}/cancel") suspend fun cancelAppointment(@Path("id") id: Int)`
- [ ] `OrderApiService` has `@POST("orders/{id}/cancel") suspend fun cancelOrder(@Path("id") id: Int)`
- [ ] `AuthApiService` has `@PATCH("user") suspend fun updateUser(@Body request: AuthDtos.UpdateUserRequest): AuthDtos.UserResponse`
- [ ] `AuthDtos.UpdateUserRequest` data class added: `name`, `email`, `phone`
- [ ] `ConversationApiService` has `@POST("conversations/{id}/messages/read") suspend fun markMessagesRead(@Path("id") id: Int)`
- [ ] `AppointmentApiService` (or new file) has `@GET("visit-reasons") suspend fun getVisitReasons(): AppointmentDtos.VisitReasonsResponse`
- [ ] `AppointmentDtos.VisitReasonDto` added: `id`, `name`, `duration_minutes`
- [ ] `AppointmentDtos.VisitReasonsResponse` added: `data: List<VisitReasonDto>`

**Dependencies:** None

**Verification:** `./gradlew assembleDebug` succeeds (unused methods are fine).

**Files:**
- `data/remote/api/AppointmentApiService.kt`
- `data/remote/api/OrderApiService.kt`
- `data/remote/api/AuthApiService.kt`
- `data/remote/api/ConversationApiService.kt`
- `data/remote/dto/AuthDtos.kt`
- `data/remote/dto/AppointmentDtos.kt`

**Scope:** S

---

#### Task 3: Add repository methods for cancel, profile update, mark-read, visit-reasons

**Description:** Implement repository logic for all new endpoints.

**Acceptance criteria:**
- [ ] `AppointmentRepository` has `suspend fun cancelAppointment(id: Int): Result<Unit>`
- [ ] `AppointmentRepositoryImpl.cancelAppointment()` calls API, returns Result
- [ ] `OrderRepository` has `suspend fun cancelOrder(id: Int): Result<Unit>`
- [ ] `OrderRepositoryImpl.cancelOrder()` calls API, returns Result
- [ ] `AuthRepository` has `suspend fun updateUser(name: String, email: String, phone: String?): Result<User>`
- [ ] `AuthRepositoryImpl.updateUser()` calls API, maps response, handles 422
- [ ] `ChatRepository` has `suspend fun markMessagesRead(conversationId: Int): Result<Unit>`
- [ ] `ChatRepositoryImpl.markMessagesRead()` calls API
- [ ] `AppointmentRepository` has `suspend fun getVisitReasons(): Result<List<VisitReason>>`
- [ ] Domain `VisitReason` data class added: `id: Int, name: String, durationMinutes: Int`
- [ ] `AppointmentRepositoryImpl.getVisitReasons()` calls API, maps to domain

**Dependencies:** Task 2 (API methods must exist)

**Verification:** `./gradlew assembleDebug` succeeds.

**Files:**
- `domain/repository/AppointmentRepository.kt`
- `data/repository/AppointmentRepositoryImpl.kt`
- `domain/repository/OrderRepository.kt`
- `data/repository/OrderRepositoryImpl.kt`
- `domain/repository/AuthRepository.kt`
- `data/repository/AuthRepositoryImpl.kt`
- `domain/repository/ChatRepository.kt`
- `data/repository/ChatRepositoryImpl.kt`
- `domain/model/Appointment.kt` (add VisitReason class)

**Scope:** M

---

### Checkpoint: Foundation Complete
- [ ] `./gradlew assembleDebug` succeeds (UI compile errors from Task 1 remain — fixed next)
- [ ] All DTOs match API response shapes
- [ ] All repository methods wired

---

### Phase 2: UI Fixes (resolve compile errors + add features)

#### Task 4: Fix BillingDetailScreen + FeedbackHistoryScreen compile errors

**Description:** Update UI screens to match the new data models from Task 1.

**Acceptance criteria:**
- [ ] `BillingDetailScreen` shows `billing.billingNumber` in header
- [ ] `BillingDetailScreen` shows line items list (type badge, description, qty × unit_price = amount)
- [ ] `BillingDetailScreen` summary shows subtotal, discount (if > 0), total
- [ ] `BillingDetailScreen` has no reference to `billing.orderId`
- [ ] `FeedbackHistoryScreen` has no reference to `staffReply` or `repliedAt`
- [ ] Both screens compile cleanly

**Dependencies:** Task 1

**Verification:** `./gradlew assembleDebug` succeeds with zero errors.

**Files:**
- `presentation/billing/BillingDetailScreen.kt`
- `presentation/feedback/FeedbackHistoryScreen.kt`

**Scope:** S

---

#### Task 5: Cancel Appointment UI

**Description:** Add cancel button and confirmation dialog to appointment detail screen.

**Acceptance criteria:**
- [ ] Cancel button visible when status is PENDING or CONFIRMED
- [ ] Cancel button hidden for RESCHEDULED, CANCELLED, COMPLETED
- [ ] Tapping cancel shows AlertDialog with "Cancel Appointment?" title and confirm/dismiss
- [ ] Confirming calls `cancelAppointment(id)` via ViewModel
- [ ] On success: appointment refreshes to CANCELLED state
- [ ] On failure: error snackbar/text shown
- [ ] Button shows loading state during API call

**Dependencies:** Task 3 (repository method)

**Verification:** `./gradlew assembleDebug` succeeds.

**Files:**
- `presentation/appointments/AppointmentDetailViewModel.kt`
- `presentation/appointments/AppointmentDetailScreen.kt`

**Scope:** S

---

#### Task 6: Cancel Order UI

**Description:** Add cancel button and confirmation dialog to order detail screen.

**Acceptance criteria:**
- [ ] Cancel button visible when status is REQUESTED only
- [ ] Cancel button hidden for all other statuses
- [ ] Tapping cancel shows AlertDialog with "Cancel Order?" title and confirm/dismiss
- [ ] Confirming calls `cancelOrder(id)` via ViewModel
- [ ] On success: order refreshes to CANCELLED state
- [ ] On failure: error text shown
- [ ] Button shows loading state during API call

**Dependencies:** Task 3 (repository method)

**Verification:** `./gradlew assembleDebug` succeeds.

**Files:**
- `presentation/orders/OrderDetailViewModel.kt`
- `presentation/orders/OrderDetailScreen.kt`

**Scope:** S

---

#### Task 7: Fix Order → Billing navigation

**Description:** Use `order.billingId` for billing navigation instead of `order.id`.

**Acceptance criteria:**
- [ ] "View Billing" button only shows when `order.billingId != null`
- [ ] `onViewBilling` callback passes `order.billingId!!` (not `order.id`)
- [ ] Button remains hidden for orders without billing (requested status)

**Dependencies:** Task 1 (billingId on Order model)

**Verification:** `./gradlew assembleDebug` succeeds.

**Files:**
- `presentation/orders/OrderDetailScreen.kt`

**Scope:** XS

---

### Checkpoint: Core Fixes Complete
- [ ] `./gradlew assembleDebug` succeeds with zero errors
- [ ] Cancel appointment/order flows work end-to-end
- [ ] Billing screen shows correct data
- [ ] Order → Billing navigation correct

---

### Phase 3: Profile Edit + Messaging

#### Task 8: Profile Edit Mode

**Description:** Convert profile screen from read-only to editable with save/cancel.

**Acceptance criteria:**
- [ ] Profile shows phone number (from User model)
- [ ] "Edit Profile" button toggles edit mode
- [ ] Edit mode shows TextFields for name, email, phone (pre-filled)
- [ ] "Save" calls `updateUser()` via ViewModel
- [ ] On success: exits edit mode, refreshes profile
- [ ] On 422 error: shows field validation errors
- [ ] "Cancel" discards changes and exits edit mode
- [ ] Loading state during save

**Dependencies:** Task 3 (repository method)

**Verification:** `./gradlew assembleDebug` succeeds.

**Files:**
- `presentation/profile/ProfileViewModel.kt`
- `presentation/profile/ProfileScreen.kt`

**Scope:** M

---

#### Task 9: Mark Messages Read + Unread Badge on FAB

**Description:** Call mark-read when chat opens, show unread count on FAB.

**Acceptance criteria:**
- [ ] `ChatViewModel` calls `markMessagesRead()` on initial load
- [ ] `ChatViewModel` calls `markMessagesRead()` after poll fetches new messages from other party
- [ ] `SplitBottomNavBar` accepts `unreadCount: Int` parameter
- [ ] FAB shows small red badge with count when `unreadCount > 0`
- [ ] Badge hidden when count is 0
- [ ] NavGraph provides unread count to SplitBottomNavBar (via a shared ViewModel or state hoist)
- [ ] Unread count refreshes periodically (piggyback on chat poll or separate lightweight poll)

**Dependencies:** Task 3 (repository methods), Task 1 (unreadCount on Conversation)

**Verification:** `./gradlew assembleDebug` succeeds.

**Files:**
- `presentation/messaging/ChatViewModel.kt`
- `presentation/navigation/SplitBottomNavBar.kt`
- `presentation/navigation/NavGraph.kt`
- New: `presentation/navigation/UnreadViewModel.kt` (or inline in NavGraph)

**Scope:** M

---

### Checkpoint: Profile + Messaging Complete
- [ ] `./gradlew assembleDebug` succeeds
- [ ] Profile edit saves to backend
- [ ] Unread badge visible on FAB
- [ ] Messages marked read on chat open

---

### Phase 4: Product + Booking Improvements

#### Task 10: Out-of-Stock Indicator on Product Variants

**Description:** Show out-of-stock state on product detail and disable ordering for OOS variants.

**Acceptance criteria:**
- [ ] `ProductDetailScreen` shows "Out of Stock" chip/badge on OOS variants
- [ ] "Order" button disabled when selected variant has `inStock == false`
- [ ] `OrderRequestScreen` shows disabled state if selected variant is OOS
- [ ] In-stock variants work as before (no regression)

**Dependencies:** Task 1 (inStock on ProductVariant)

**Verification:** `./gradlew assembleDebug` succeeds.

**Files:**
- `presentation/catalog/ProductDetailScreen.kt`
- `presentation/orders/OrderRequestScreen.kt`

**Scope:** S

---

#### Task 11: Fetch Visit Reasons from API + Dynamic Time Slots

**Description:** Replace hardcoded visit reasons with API data; generate time slots dynamically.

**Acceptance criteria:**
- [ ] `BookAppointmentViewModel` fetches visit reasons from `getVisitReasons()` on init
- [ ] Loading state shown while fetching
- [ ] Visit reason cards show names from API (not hardcoded strings)
- [ ] Correct `visitReasonId` sent in booking request
- [ ] Static `TIME_SLOTS` list replaced with generated slots: 30-min increments from 9:00 to 17:00
- [ ] Step 2 shows 17 slots (9:00, 9:30, 10:00, ..., 17:00)
- [ ] Hardcoded `VISIT_REASONS`, `REASON_IDS` removed

**Dependencies:** Task 3 (repository method for visit reasons)

**Verification:** `./gradlew assembleDebug` succeeds; no hardcoded reason IDs remain.

**Files:**
- `presentation/appointments/booking/BookAppointmentViewModel.kt`
- `presentation/appointments/booking/BookAppointmentScreen.kt`

**Scope:** M

---

#### Task 12: Fix Home Upcoming Appointments Filter

**Description:** Include today's appointments in the "next appointment" section.

**Acceptance criteria:**
- [ ] `HomeViewModel` filter uses `!isBefore(today)` instead of `isAfter(today)`
- [ ] Today's confirmed/pending appointments appear as "upcoming"
- [ ] Past appointments (yesterday and before) still excluded

**Dependencies:** None

**Verification:** `./gradlew assembleDebug` succeeds.

**Files:**
- `presentation/home/HomeViewModel.kt`

**Scope:** XS

---

### Checkpoint: All Tasks Complete
- [ ] `./gradlew assembleDebug` succeeds with zero errors
- [ ] All 15 success criteria from the spec are met
- [ ] No references to removed fields (grep: `staffReply`, `orderId` in Billing, hardcoded `REASON_IDS`)
- [ ] Full app functional

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Cancel endpoints return unexpected error format | Med | Wrap in generic `runCatching`, show error message directly |
| Billing API response shape differs from example | Med | `ignoreUnknownKeys = true` already set; nullable defaults protect against missing fields |
| Unread count poll adds network overhead | Low | Piggyback on existing chat poll (only active when NavGraph visible) |
| Visit reasons API not seeded in dev | Low | Show empty state with retry; fallback to error message |

## Open Questions

None — all resolved.
