# Spec: Backend Alignment v18 — Commerce Simplification & Simplified Frame Reservations

> Status: **Draft — awaiting review**
> Date: 2026-08-13
> Sources: `docs/API_CONTRACT.md` (2026-08-13, 54 routes), `docs/BACKEND_CONTEXT.md` (2026-08-13)
> Supersedes the Android assumptions recorded in `CONTEXT.md` §Frame Reservations, §My Eyewear,
> §Frame Rating Aggregates, §Visit Feedback, and §Route Governance.

---

## Assumptions

These are the assumptions this spec is built on. Correct any that are wrong before implementation starts.

1. The backend changes described in the 2026-08-13 contract are **already deployed** — the app is
   currently broken against the live API (it calls two deleted quotation routes and a deleted
   reservation cancel route), so this is a repair, not a forward-looking migration.
2. There is **no transition window**. No client-side dual-read (old `status` + new `is_held`) or
   fallback to `POST .../cancel` is needed. Deleted routes are gone for every client.
3. `expires_at` on a reservation is now **always present** (derived server-side from clinic close on
   the appointment date) in both held and unheld states. The DTO stays nullable defensively, but the
   UI does not need a "no expiry known" state.
4. `source_quotation` still **exists on the wire** in the Optical Order response — only the
   `/quotations` endpoints were deleted. The client nonetheless stops consuming it (see D1);
   `ignoreUnknownKeys` absorbs the field harmlessly.
5. Hiding frame ratings is a **product decision, not a backend one**. The endpoints and aggregates
   still work; we are withholding the surface from patients pending clinic sign-off.
6. **Visit feedback (appointment ratings) stays live.** "Frame ratings" means frame-product ratings
   only — `POST /optical-order-items/{id}/rating` and the ★ aggregate on frames.
7. No new dependency, no navigation graph restructure, and no Room schema change is required. The
   `frames` table keeps its `averageRating`/`ratingCount` columns even while the badge is hidden
   (dropping them would require a migration for a temporary UI decision).

---

## Objective

Realign the Android client to the 2026-08-13 patient API, which removed patient quotations, collapsed
frame reservations to a two-state model with new item-editing routes, dropped rating revision history,
and added a rejection reason to appointment requests. Additionally, withhold the frame-ratings surface
from patients behind a feature flag.

**User:** an EyeCare patient using the Android app against the current production API.

**Success looks like:** every route the app calls exists on the server; reservations are editable from
the app instead of being silently cancelled and recreated; no screen references a quotation the API
can no longer return; and no patient sees a frame rating surface until the clinic signs off.

### Backend deltas driving this work

| # | Backend change | Client consequence |
|---|---|---|
| 1 | `GET /quotations`, `GET /quotations/{id}` deleted | Estimates feature is unreachable — must be removed |
| 2 | Reservation `status` enum → `is_held` boolean | `ReservationStatus` and all six-state presentation logic is invalid |
| 3 | `POST /frame-reservations/{id}/cancel` → `DELETE /frame-reservations/{id}` (204) | API + repository signature change |
| 4 | **New** `POST /frame-reservations/{id}/items`, `DELETE /frame-reservations/{id}/items/{item}` | Replaces the cancel-then-recreate merge hack; enables in-place editing |
| 5 | `revision_number` removed from every rating payload | "Edited" badge can never be true — remove field and badge |
| 6 | `rejection_reason` added to appointment requests | Rejected requests currently show no reason |
| 7 | Legacy alias `POST /job-order-items/{id}/rating` deleted; count 55 → 54 | Route governance must move it to rejected |
| 8 | Hidden-rating aggregate bug **fixed** | Remove the "Known backend bug" note from `CONTEXT.md` |

---

## Tech Stack

Unchanged. Kotlin 2.3.0 (AGP 9.2.1 built-in), Compose + Material 3 (BOM 2026.05.01), Hilt 2.59.2,
Retrofit 2.11 + OkHttp 4.12 + Kotlinx Serialization 1.8.1, Room 2.7.1, Navigation Compose 2.9.0,
JUnit 5 + MockK + Turbine + coroutines-test. **No dependency is added or removed.**

## Commands

```
./gradlew assembleDebug          # Build — must pass before any task is called done
./gradlew testDebugUnitTest      # Unit tests
./gradlew ktlintFormat           # Format
./gradlew ktlintCheck            # Format check
./gradlew lintDebug              # Lint
```

## Project Structure

Files this spec touches, by layer.

```
data/remote/api/
  FrameReservationApiService.kt      → cancel removed; delete + item routes added
  QuotationApiService.kt             → DELETED
data/remote/dto/
  FrameReservationDtos.kt            → status → isHeld; AddItemRequest added
  AppointmentRequestDtos.kt          → rejection_reason added
  AppointmentV1Dtos.kt               → revision_number removed
  OpticalOrderDtos.kt                → revision_number removed (x2)
  QuotationDtos.kt                   → DELETED
data/repository/
  FrameReservationRepositoryImpl.kt  → isHeld mapping, delete/add/remove item
  AppointmentRequestRepositoryImpl.kt→ rejectionReason mapping
  AppointmentV1RepositoryImpl.kt     → revisionNumber removed
  OpticalOrderRepositoryImpl.kt      → revisionNumber removed
  QuotationRepositoryImpl.kt         → DELETED
domain/model/
  FrameReservation.kt                → ReservationStatus deleted, isHeld added
  AppointmentRequest.kt              → rejectionReason added
  AppointmentV1.kt, OpticalOrder.kt  → revisionNumber removed
  Quotation.kt                       → DELETED
  OpticalOrder.kt                    → QuotationReference + sourceQuotation removed
domain/repository/
  FrameReservationRepository.kt      → signature changes
  QuotationRepository.kt             → DELETED
di/
  QuotationModule.kt                 → DELETED
presentation/common/
  FeatureFlags.kt                    → NEW (frame-ratings kill switch)
presentation/reservations/
  ReservationPresentation.kt         → two-state copy from the contract
  FrameReservationListScreen.kt      → status chip → held chip
  FrameReservationDetailScreen.kt    → add/remove items, delete-and-pop
  FrameReservationDetailViewModel.kt → item mutations, deletion state
  CreateFrameReservationViewModel.kt → merge via add-item, not cancel+recreate
  CreateFrameReservationScreen.kt    → cancellable check
presentation/eyewear/
  MyEyewearScreen.kt                 → RENAMED MyOrdersScreen.kt, Orders-only
  OpticalOrderDetailScreen.kt        → quotation ref removed; rating flagged
  OpticalOrderListScreen.kt          → estimate cross-link removed
  EyewearPresentation.kt             → estimate/isEdited helpers removed
  Estimate{List,Detail}{Screen,ViewModel}.kt → DELETED
presentation/profile/
  ProfileScreen.kt                   → "My Eyewear" row → "My Orders"
presentation/frames/
  FrameCard.kt, FrameDetailScreen.kt → RatingBadge flagged off
presentation/appointments/
  AppointmentDetailScreen.kt         → "Edited" badge removed
  requests/AppointmentRequestDetailScreen.kt → rejection reason shown
presentation/navigation/
  Routes.kt, NavGraph.kt, PatientFeatureIntent.kt → EstimateDetail removed
test/.../data/remote/
  ApprovedApiRoutes.kt               → 54-route contract
```

## Code Style

Existing conventions hold. The reservation model shows the shape this migration targets — a derived
boolean replacing an enum, with capability predicates as extension properties rather than scattered
`when` branches:

```kotlin
data class FrameReservation(
    val id: Int,
    val appointment: ReservationAppointment,
    /** Derived from `accepted_at`: false = request, true = frames pulled and held. */
    val isHeld: Boolean,
    val expiresAt: String?,
    val createdAt: String,
    val items: List<FrameReservationItem>,
)

/** DELETE succeeds for the owner in either state, so cancelling is always offered. */
val FrameReservation.isCancellable: Boolean
    get() = true

/** Contract: adding is rejected once the clinic has pulled the frames. */
val FrameReservation.canAddItems: Boolean
    get() = !isHeld && items.size < MAX_RESERVATION_ITEMS
```

Feature-flag gating reads as a plain conjunction at the call site, never as a wrapper composable:

```kotlin
if (FeatureFlags.FRAME_RATINGS_ENABLED && item.isRateable) {
    TextButton(onClick = { onRateItem(item.id) }) { Text("Rate this item") }
}
```

## Testing Strategy

- **Framework:** JUnit 5 + MockK + Turbine for ViewModels/repositories; Compose UI tests in
  `app/src/androidTest` for screen behavior.
- **Locations:** `app/src/test/java/com/eyecare/app/...` mirroring the production package.
- **Levels:**
  - *DTO tests* — decode a literal contract JSON sample for every changed payload
    (`is_held`, `rejection_reason`, rating payload without `revision_number`).
  - *Repository tests* — mapping and the new delete/add/remove item calls, including the
    204-last-item-removed branch.
  - *ViewModel tests* — reservation item mutation single-flight, delete-then-pop signalling,
    add-item merge replacing cancel+recreate, rejection-reason exposure.
  - *Route governance* — `ApiRouteAllowlistTest` must fail if any production Retrofit annotation
    references `/quotations`, `POST /frame-reservations/{id}/cancel`, or `/job-order-items/.../rating`.
- **Deleted tests:** `EstimateListViewModelTest`, `EstimateDetailViewModelTest`,
  `QuotationDtosTest`, `QuotationRepositoryImplTest` are removed with their subjects. Frame-rating
  tests (`FrameRatingViewModelTest`) are **kept** — the flag hides UI, it does not delete logic.
- **Coverage expectation:** every changed mapping and every new repository method has a test. No
  net loss of assertions outside the deliberately deleted quotation surface.

---

## Design Decisions

### D1 — Estimates: full removal, quotation surface gone entirely
`GET /quotations` is gone, so the Estimates tab cannot load. The entire Quotation vertical is deleted
(API service, DTOs, repository, domain model, DI module, both screens, both ViewModels, route, and
`PatientFeatureIntent.EstimateDetail`).

`OpticalOrder.sourceQuotation` is **also removed end-to-end** — DTO field, `QuotationReference` domain
class, repository mapping, the order-detail reference row, and the list's `onViewEstimate` cross-link.
A quotation number the patient can neither open nor act on is noise in an order screen; the destination
shows order information only. The server may keep sending `source_quotation` — `ignoreUnknownKeys`
discards it.

### D1a — The destination is renamed "My Orders"
With estimates gone the name "My Eyewear" no longer describes the screen. Renamed throughout: the
top-app-bar title, the Profile **Care & activity** row label, `MyEyewearScreen.kt` → `MyOrdersScreen.kt`,
the typed route `MyEyewear` → `MyOrders`, and `PatientFeatureIntent.MyEyewear` → `MyOrders`.

This is a route **rename**, not a graph restructure — no destination is added, removed, or reparented,
and the active-link gate behaves identically. It is called out here because route identity is normally
an "ask first" area.

### D2 — Reservations: two states, contract-supplied copy
`ReservationStatus` is deleted outright rather than reduced. The contract prescribes the patient copy
verbatim, and it is used as written:

| State | Copy |
|---|---|
| `is_held: false` | "Request sent — the clinic will set these aside before your visit." |
| `is_held: true` | "Set aside for your visit until {expires_at}." |

Chip labels are **Requested** / **Set aside**. There are no terminal states: a reservation either
exists or has been deleted, so the list's "terminal statuses" partition and the
Requested→Prepared→Tried-on tracker are removed, not remapped.

### D3 — Item editing is gated on `!isHeld` for both add and remove
The contract requires `is_held == false` for **add** and is silent for **remove** (backend
`RemoveFrameReservationItem` does restore stock when accepted, so the server would permit it).
We gate **both** on `!isHeld` anyway: once the clinic has physically pulled frames, changing the set
from the app without the clinic knowing is worse UX than telling the patient to ask at the visit. This
is deliberately more conservative than the server allows, and can be relaxed later without a server
change.

### D4 — "Add frame" routes through the existing catalog flow
Reservation detail's **+ Add frame** navigates to the frames catalog. The patient picks a frame and
uses the existing reserve action, which now folds into the existing reservation via
`POST /frame-reservations/{id}/items`. This reuses the whole merge path rather than building a second
frame picker inside a bottom sheet.

Consequently `CreateFrameReservationViewModel.mergeIntoExisting` stops being a cancel-then-recreate.
That hack could destroy a patient's hold if the recreate failed; it is replaced by one add-item call.
**This is the highest-value correctness fix in this spec.**

### D5 — Removing the last item is a deletion, not an empty reservation
`DELETE /frame-reservations/{id}/items/{item}` returns 204 when the last item goes, deleting the
reservation. The repository models this as `Result<FrameReservation?>` — `null` meaning "gone" — and
the detail screen pops to the list on `null`, the same terminal path as an explicit cancel.

### D6 — Rating revisions are removed, not defaulted
`revision_number` is gone from the API. Leaving the nullable field would leave `isEdited()` permanently
false — dead code masquerading as a feature. The field is removed from both DTOs and both domain
models, and the "Edited" badge is deleted from appointment detail and order detail.

### D7 — Frame ratings hidden behind one flag, code kept intact
`FeatureFlags.FRAME_RATINGS_ENABLED = false` gates two surfaces:
- the submit/revise rating action on My Eyewear order items (`FrameRatingDialog` entry point), and
- `RatingBadge` / `RatingBadgeDetail` on the frame catalog card and frame detail.

Visit feedback (appointment ratings) is **not** flagged and stays live. DTOs, repositories,
ViewModels, and their tests remain compiling and tested — flipping one boolean restores the feature.
Re-enablement is expected **soon**, which is exactly why this is a flag and not a deletion: the
surface must come back without archaeology through git history.
`POST /optical-order-items/{item}/rating` stays in the approved active-link route set: it is an
approved route that the UI simply does not reach while the flag is off. The Room columns
`averageRating`/`ratingCount` stay; dropping them would need a schema migration for a temporary
product decision.

---

## Boundaries

**Always**
- Run `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` after each task.
- Map DTOs → domain at the repository boundary; no DTO leaks into presentation.
- Keep UI state as `sealed interface` exposed via `StateFlow`.
- Use the contract's verbatim reservation copy (D2).
- Keep `ApprovedApiRoutes` and `CONTEXT.md` in step with every route change in the same task.

**Ask first**
- Any new dependency.
- Any navigation graph restructure beyond deleting the `EstimateDetail` route.
- Any Room schema change (explicitly out of scope — see Assumption 7).
- Relaxing D3 to allow item removal on a held reservation.

**Never**
- Use Gson.
- Store tokens or health data in Room.
- Apply the `org.jetbrains.kotlin.android` plugin.
- Call `/quotations`, `POST /frame-reservations/{id}/cancel`, or `/job-order-items/{id}/rating`.
- Coerce `average_rating: null` to `0.0` — unrated is not zero-star (still true behind the flag).
- Invent a `GET /frame-reservations/{id}`; detail continues resolving from the patient's own list.

---

## Success Criteria

Each is objectively checkable.

1. `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` both pass.
2. `ApiRouteAllowlistTest` asserts **8 public + 29 account-only + 17 active-link = 54** routes, and
   fails if a production Retrofit annotation references `/quotations`,
   `POST /frame-reservations/{id}/cancel`, or `POST /job-order-items/{id}/rating`.
3. **Zero** files under `app/src/main` reference `Quotation`, `quotation`, or `Estimate` — the grep
   returns nothing. No order screen renders a quotation number.
4. `ReservationStatus` does not exist anywhere in the codebase.
5. A reservation with `is_held: false` renders exactly: "Request sent — the clinic will set these
   aside before your visit." A reservation with `is_held: true` renders "Set aside for your visit
   until {formatted expires_at}." (Compose test.)
6. Reserving a frame for an appointment that already has an unheld reservation issues exactly one
   `POST /frame-reservations/{id}/items` and **zero** delete calls. (ViewModel test with MockK
   `verify(exactly = 0)` on the delete path.)
7. Removing the last item of a reservation (204) pops the detail screen to the list rather than
   rendering an empty reservation. (ViewModel test on the `null` terminal state.)
8. Cancelling a reservation issues `DELETE /frame-reservations/{id}` and pops to the list.
9. A rejected appointment request displays its `rejection_reason`; a non-rejected one displays no
   rejection row. (DTO test + Compose test.)
10. With `FRAME_RATINGS_ENABLED = false`: no rating action on order items, no ★ badge on frame cards,
    no "No ratings yet" on frame detail. Flipping the flag to `true` restores all three and the suite
    still compiles. (Compose tests for both flag states where practical.)
11. Appointment detail and order detail contain no "Edited" badge, and `revision_number` appears in no
    DTO.
12. The destination reads **My Orders** in its app bar and in the Profile row, and no user-visible
    string says "My Eyewear" or "Estimates".
13. `CONTEXT.md` reflects: 54 routes, two-state reservations, the My Orders destination, the flagged
    frame-ratings surface, and the removal of the "Known backend bug" note.

---

## Out of Scope

- Room schema migration to drop rating columns (Assumption 7).
- Any redesign of My Eyewear beyond removing the Estimates section.
- An in-sheet frame picker for reservation item adding (D4 routes through the catalog).
- Visit feedback / appointment rating changes beyond dropping `revision_number`.
- The `voided` encounter/prescription states, `inbox_archived_at`, and other staff-only backend
  changes — no patient route exposes them.

## Resolved Questions

All three open questions were resolved on 2026-08-13:

1. **Quotation reference on order detail** — *do not display quotation information at all.* The
   destination shows order details only; `sourceQuotation` is removed end-to-end (D1) rather than
   demoted to a plain-text row.
2. **Destination name** — *"My Orders."* Renamed in the app bar, the Profile row, the screen file,
   and the typed route (D1a).
3. **Frame-ratings re-enablement** — *near.* The feature flag stays; the surface is not deleted (D7).

## Open Questions

None. Ready for planning.
