# Spec: Offline-First Mobile App

Status: Pending approval
Phase: Specify

## Objective

Make the Eyecare Android app as offline-capable as possible. Users should be able to browse their data (appointments, orders, prescriptions, billings, products) without internet. Write operations that require server validation will queue when offline and attempt to submit when connectivity returns — with clear feedback about what succeeded/failed.

## Strategy

**Three tiers:**
1. **Full offline** — cached data available, reads work seamlessly
2. **Queued writes** — simple writes (cancel, feedback, profile update) queued for sync
3. **Online-only** — complex writes that require server-side validation (book appointment, place order)

## Assumptions

1. Room is the local cache layer (already used for products)
2. We use a `NetworkMonitor` utility that exposes connectivity state as a Flow
3. Queued operations are stored in a Room table and retried on connectivity change
4. Cached data shows a "last synced" indicator, not presented as real-time
5. Prescriptions are encrypted on the server but returned decrypted via API — we cache the decrypted response (acceptable since the phone itself is the security boundary)
6. We do NOT cache chat messages locally (privacy concern + complexity of sync)

## Tier 1: Offline Read (Cache Everything)

### New Room Entities

| Entity | Caches | Source |
|--------|--------|--------|
| `ProductEntity` | Products + variants JSON | `GET /products` (already exists) |
| `AppointmentEntity` | Customer's appointments | `GET /appointments` |
| `OrderEntity` | Customer's orders + items JSON | `GET /orders` |
| `PrescriptionEntity` | Customer's prescriptions | `GET /prescriptions` |
| `BillingEntity` | Billing + items + payments JSON | `GET /billing/{id}` |
| `UserEntity` | Current user profile | `GET /user` |
| `VisitReasonEntity` | Visit reasons list | `GET /visit-reasons` |
| `BrandEntity` | Brands list | `GET /brands` |
| `CategoryEntity` | Categories list | `GET /categories` |

### Cache Pattern (per repository)

```kotlin
// Fetch-first, fallback to cache
override suspend fun getAppointments(): Result<List<Appointment>> {
    return try {
        val response = api.getAppointments()
        dao.replaceAll(response.data.map { it.toEntity() })
        Result.success(response.data.map { it.toDomain() })
    } catch (e: Exception) {
        val cached = dao.getAll()
        if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
        else Result.failure(e)
    }
}
```

### Connectivity Indicator

- A `NetworkMonitor` singleton observes `ConnectivityManager` and exposes `isOnline: StateFlow<Boolean>`
- When offline, show a subtle banner at the top: "You're offline — showing cached data"
- Disable write action buttons (book, order, send message) when offline
- Pull-to-refresh shows "No internet connection" toast when offline

## Tier 2: Queued Writes

Simple write operations that don't require complex server validation can be queued:

| Operation | Queue? | Reason |
|-----------|--------|--------|
| Cancel appointment | ✅ Queue | Simple POST, idempotent |
| Cancel order | ✅ Queue | Simple POST, idempotent |
| Update profile | ✅ Queue | Simple PATCH, last-write-wins |
| Submit feedback | ✅ Queue | Simple POST, idempotent by appointment/order |
| Mark messages read | ✅ Queue | Simple POST, idempotent |
| Book appointment | ❌ Online only | Requires conflict detection |
| Place order | ❌ Online only | Requires stock validation |
| Send message | ❌ Online only | Real-time expectation |

### Pending Operations Table

```
pending_operations (Room)
- id (auto)
- type (cancel_appointment | cancel_order | update_profile | submit_feedback | mark_read)
- payload (JSON — endpoint params)
- created_at
- status (pending | failed)
- error_message (nullable)
- retry_count
```

### Sync Behavior

- On connectivity restored → process all pending operations in FIFO order
- Max 3 retries per operation
- Show badge on Profile: "2 pending changes" when operations queued
- Failed operations show in a "Pending Sync" section with retry/discard options

## Tier 3: Online-Only (with clear feedback)

- Book appointment → button disabled when offline, tooltip "Requires internet"
- Place order → button disabled when offline
- Send message → input disabled, show "Reconnect to send"
- Download PDF → button disabled when offline

## Changes Required

### Phase 1: NetworkMonitor + Offline Banner
- Create `NetworkMonitor` using `ConnectivityManager.NetworkCallback`
- Inject as singleton
- Show offline banner in `NavGraph` (above content, below status bar)
- Expose `isOnline` to ViewModels that need it

### Phase 2: Room Entities + Cache Layer
- Add entities: Appointment, Order, Prescription, Billing, User, VisitReason, Brand, Category
- Add DAOs with `replaceAll()` and `getAll()` patterns
- Bump database version with destructive migration
- Update each repository to cache on success + fallback on failure

### Phase 3: Queued Writes
- Create `PendingOperation` entity + DAO
- Create `SyncManager` that observes connectivity and processes queue
- Update cancel/profile/feedback ViewModels to queue when offline
- Add "Pending Sync" indicator

### Phase 4: UI Polish
- Disable online-only buttons when offline
- Add "last synced" timestamp to data screens
- Pull-to-refresh offline feedback

## Success Criteria

- [ ] App shows cached products, appointments, orders, prescriptions when offline
- [ ] Offline banner visible when connectivity lost
- [ ] Cancel appointment/order queues when offline, syncs when online
- [ ] Profile update queues when offline
- [ ] Feedback submission queues when offline
- [ ] Book appointment / place order buttons disabled when offline
- [ ] Chat input disabled when offline
- [ ] "Pending Sync" indicator shows queued operations count
- [ ] Failed syncs show error + retry option
- [ ] App compiles with zero errors

## Boundaries

- **Always:** Show clear offline/online state to user. Never silently fail a write.
- **Ask first:** Before caching sensitive data (prescriptions). Before adding WorkManager dependency.
- **Never:** Cache chat messages. Present cached data as real-time. Allow users to think a queued write has succeeded when it hasn't.

## Open Questions

1. Should we use WorkManager for background sync (more reliable but adds dependency) or just observe connectivity in-process?
2. Should prescription data be encrypted in Room (defense-in-depth) or is device-level encryption sufficient?
