# Spec: Backend Alignment v20 — Saved Frames Cutover

**Date:** 2026-08-27  
**Status:** Approved 2026-08-27 — Phase 3 task decomposition in progress  
**Authoritative inputs:** `CONTEXT.md`, `docs/BACKEND_CONTEXT.md` (2026-08-26),
`docs/API_CONTRACT.md` (2026-08-26), and the implemented Android source tree

## Objective

Align the Android app with the backend replacement of appointment-bound Frame Reservations by
account-owned Saved Frames.

After this cutover, any authenticated account can save a frame variant as a persistent preference,
review its saved variants, open the exact saved variant in the catalog, and remove it. Saving never
holds inventory, never depends on an appointment or active patient link, and never promises
availability. All Android reservation routes, models, screens, appointment coupling, navigation,
copy, and tests are retired in the same coordinated change.

The change also aligns route governance with the authoritative 59-route contract and moves
conversation attachment download to the account-only tier while retaining backend-capability gating
for attachment upload.

### User outcomes

1. A linked or unlinked authenticated account can save or remove the selected variant from Frame
   Detail and AR Try-On.
2. Profile exposes **Saved Frames** to every authenticated account.
3. Saved Frames lists the account's preferences newest first, supports server pagination, distinguishes
   available from unavailable variants without exposing stock numbers, and opens the exact saved
   variant in Frame Detail.
4. The app explains that saved frames are preferences only and never uses reservation, hold, set-aside,
   expiry, or appointment-selection language for this feature.
5. No production Retrofit service can call a retired Frame Reservation route.

## Assumptions

1. `docs/BACKEND_CONTEXT.md` and `docs/API_CONTRACT.md` are authoritative over the v19 Android spec
   when they describe the 2026-08-26 Saved Frames cutover.
2. `is_saved` is a required account-specific boolean on every variant returned by `GET /frames` and
   `GET /frames/{frame}`, even though the current `GET /frames` example omits the field.
3. Message search remains cursor-paginated and continues accepting the optional opaque `cursor`
   query. `docs/BACKEND_CONTEXT.md` and the search response text explicitly retain cursor pagination;
   the missing `cursor` row in the current API-contract query list is treated as documentation drift,
   not an Android breaking change.
4. The backend's Encounter → Consultation terminology change does not require Android work in v20:
   the Android app has no Encounter-labelled patient surface, and the backend document explicitly
   keeps wire names Encounter-based.
5. Existing Saved Frame backend behavior is complete. This work changes only the Android client and
   repository documentation; it does not add or alter backend endpoints.
6. A Saved Frames row opens Frame Detail with the saved ProductVariant preselected. Extending the
   typed `FrameDetail` route with an optional variant ID is acceptable and does not create a new API
   route.
7. Catalog cards do not gain an ambiguous one-tap save action because a product can contain multiple
   variants. Mutation occurs only where one variant is selected: Frame Detail and AR Try-On.

## Authoritative Contract Delta

### Added account-only routes

```text
GET    /api/v1/saved-frames
PUT    /api/v1/saved-frames/{productVariant}
DELETE /api/v1/saved-frames/{productVariant}
```

- `GET` is page-paginated, newest first, with `page` (minimum 1) and `per_page` (1–50, default 15).
- `PUT` has no body, is idempotent, returns 200 and one Saved Frame resource, and does not change
  `saved_at` on a repeated save.
- `DELETE` has no body, is idempotent, and returns 204 whether or not the preference exists.
- All three routes require authentication but not an active patient link.

### Added catalog field

Every authenticated frame-catalog variant includes:

```json
{
  "is_saved": true
}
```

The value is scoped to the authenticated account. It must reach the domain model as `isSaved` and
must not be persisted as account-neutral Room catalog state.

### Retired routes

```text
GET    /api/v1/frame-reservations
POST   /api/v1/frame-reservations
DELETE /api/v1/frame-reservations/{reservation}
POST   /api/v1/frame-reservations/{reservation}/items
DELETE /api/v1/frame-reservations/{reservation}/items/{item}
```

These routes move into Android's rejected-route set. Their Retrofit service, DTOs, repositories,
domain types, DI module, screens, and tests are removed rather than retained as compatibility code.

### Messaging route-tier correction

`GET /api/v1/conversation/attachments/{attachment}` is account-only. Existing presentation behavior
that permits downloads for `LINKED_PATIENT` and `GENERAL_INQUIRY` conversations remains. Upload still
requires the server-provided `can_upload_attachments` capability and linked-patient access.

### Route totals

```text
8 public + 40 account-only + 11 active-link = 59 canonical routes
```

## Functional Requirements

### F1 — Saved Frame data boundary

- Add Kotlinx Serialization DTOs for the Saved Frame resource, nested variant/product, page envelope,
  links/meta, and `availability`.
- Price values accept the existing string-or-number money contract.
- Published typed AR metadata uses the same validation and domain meaning as frame-catalog AR data.
- Map DTOs to new domain models only at the Saved Frame repository boundary.
- Model availability as `AVAILABLE`, `UNAVAILABLE`, or safe `UNKNOWN`; unknown external values render
  as unavailable and never crash or enable a stock promise.
- Expose a repository with page load, idempotent save, and idempotent remove operations.
- All calls use the existing safe API-call/error-decoding boundary and return `Result`.

Representative domain interface:

```kotlin
data class SavedFramePage(
    val items: List<SavedFrame>,
    val currentPage: Int,
    val lastPage: Int,
)

interface SavedFrameRepository {
    suspend fun getSavedFrames(page: Int = 1, perPage: Int = 15): Result<SavedFramePage>
    suspend fun save(productVariantId: Int): Result<SavedFrame>
    suspend fun remove(productVariantId: Int): Result<Unit>
}
```

### F2 — Catalog saved state and Room isolation

- Add `isSaved` to the frame variant DTO and domain model and map it at the Frame repository boundary.
- Existing serialized Room cache rows may omit the new field; they decode fail-closed as `false`.
- Before writing catalog variants into Room, force `isSaved = false`. The shared product cache must not
  expose one signed-in account's preference state to another account or to a later logged-out session.
- Online list/detail responses remain authoritative for saved state.
- Refreshing Frame Detail, Frames, or AR reconciles saved state from the backend.

### F3 — Saved Frames list

- Replace the Profile **Reservations** row with **Saved Frames** for linked and unlinked authenticated
  accounts.
- Saved Frames is a typed Main-graph sub-destination, not a fifth bottom-navigation root and not an
  active-link-protected feature intent.
- Load page 1 with `per_page=15`, render newest first, and load later pages without duplicate
  ProductVariant IDs.
- Define initial loading, initial error/retry, empty, populated, refreshing, loading-more, and
  load-more error states.
- Pull-to-refresh replaces the list only after a successful first-page response; failure keeps usable
  data visible.
- Each row shows product/variant identity, patient-safe price/image data, saved time, and a non-color-only
  **Unavailable** status when applicable. It never shows an inventory count or availability reason.
- Row activation opens `FrameDetail(frameId, variantId)` with the saved variant selected.
- Remove is per-row single-flight. Successful idempotent removal deletes the row locally; failure keeps
  it and shows patient-safe retry feedback.
- The screen visibly presents: **Saved frames are preferences only. Availability is not guaranteed
  until your purchase is confirmed.**

### F4 — Frame Detail save toggle

- The bottom action for the selected variant becomes **Save frame** when unsaved and **Remove from
  saved** (or an equivalently explicit saved-state action) when saved.
- The action is available to linked and unlinked authenticated accounts and has no appointment gate.
- Mutation is single-flight and visibly disabled while running.
- Successful save/remove updates the selected variant in the currently displayed Frame domain object
  without reloading the entire screen.
- Switching variants changes the action to that variant's own `isSaved` value.
- Failure preserves the prior state and shows patient-safe feedback. A 422 save failure explains that
  the option can no longer be saved and offers refresh/retry rather than exposing validation internals.
- The preference-only/availability disclaimer is visible or directly accessible beside the action.

### F5 — AR Try-On save toggle

- Replace **Reserve this frame** and its create-reservation navigation callback with a save/remove
  action for the currently selected variant.
- The AR ViewModel owns mutation state and uses SavedFrameRepository; the composable does not call
  Retrofit directly.
- Save/remove remains available when a valid selected variant is present; it is independent of
  appointment and patient-link state.
- Switching variants uses each variant's server `isSaved` value.
- Successful first save shows concise preference-only feedback; failure leaves the previous state and
  uses patient-safe copy.
- AR asset loading, calibration, fallback, camera capability, and renderer behavior do not change.

### F6 — Remove reservation coupling

- Remove `CreateFrameReservation`, `FrameReservationList`, and `FrameReservationDetail` routes and
  composables.
- Remove all Frame Reservation feature intents, access labels, and limited-account restoration paths.
- Remove the reservation-origin argument from `RequestAppointment`, RequestAppointmentScreen,
  RequestAppointmentViewModel success state, and submission.
- Appointment Detail no longer injects or loads a FrameReservationRepository and no longer renders a
  Reserved Frames section.
- Remove reservation-specific appointment eligibility, maximum-item, held/cancellable, total-value,
  expiry, and presentation logic.
- No active Android copy says reserve, reservation, held, set aside, or expiry for frame preferences.
  Historical docs/specs may retain clearly historical wording.

### F7 — Navigation and refresh behavior

- Extend typed `FrameDetail` with nullable `variantId`; direct catalog navigation may omit it.
- FrameDetailViewModel selects the requested variant when present, otherwise the first variant.
- Unknown/stale variant IDs safely fall back to the first available variant and do not crash.
- Saved Frames is classified account-only and never opens the Limited Account link hub.
- Returning to a retained catalog/detail/AR destination reconciles saved state through a lifecycle
  refresh or an equivalently testable server refresh path.

### F8 — Route governance and documentation

- Route allowlist asserts 8 public, 40 account-only, 11 active-link, and 59 total.
- Add all three Saved Frames routes to account-only.
- Move attachment download from active-link to account-only.
- Add all five former reservation routes to rejected routes and prove production Retrofit discovery
  contains none of them.
- Update `CONTEXT.md` to describe Saved Frames, account-only access, 59 routes, attachment download,
  and removal of appointment-coupled reservations.
- Correct the two source-document drifts during implementation without altering their intended backend
  behavior: add `is_saved` to the `GET /frames` example and restore optional `cursor` to the message
  search query list.
- Update `AGENTS.md` current-work pointer and `CONTEXT.md` active-spec listing as the gated workflow
  advances. Historical completed alignment specs remain unchanged.

## Tech Stack

| Concern | Technology |
|---|---|
| Language/build | Kotlin 2.3.0, AGP 9.2.1 built-in Kotlin support |
| UI | Jetpack Compose + Material 3 |
| State | ViewModel + sealed UI state + `StateFlow` |
| Navigation | Navigation Compose typed `@Serializable` routes |
| Network | Retrofit 2.11, OkHttp 4.12, Kotlinx Serialization 1.8.1 |
| DI | Hilt 2.59.2 |
| Local cache | Room 2.7.1, catalog only; saved ownership state is not persisted |
| Tests | JUnit 5, MockK, Turbine, coroutines-test, MockWebServer, Compose tests |

No new dependency is required.

## Commands

Run from the repository root:

```powershell
# Format
.\gradlew ktlintFormat

# Focused tests during implementation
.\gradlew testDebugUnitTest --tests "*SavedFrame*" --tests "*FrameDetailViewModelTest" --tests "*ArViewModelTest" --tests "*ApiRouteAllowlistTest"

# Full unit suite
.\gradlew testDebugUnitTest

# Lint
.\gradlew lintDebug

# Mandatory build after repository changes
.\gradlew assembleDebug

# Instrumented UI tests when an emulator/device is available
.\gradlew connectedDebugAndroidTest
```

On this Windows workstation, Gradle commands use Android Studio's bundled JBR when `JAVA_HOME` is not
already configured.

## Project Structure

```text
app/src/main/java/com/eyecare/app/
├── data/remote/api/
│   ├── FrameApiService.kt                 # Existing catalog + is_saved
│   └── SavedFrameApiService.kt            # GET/PUT/DELETE saved-frames
├── data/remote/dto/
│   ├── FrameDtos.kt                       # Existing catalog + is_saved
│   └── SavedFrameDtos.kt                  # Page/resource wire contract
├── data/repository/
│   ├── FrameRepositoryImpl.kt             # Catalog mapping + cache isolation
│   └── SavedFrameRepositoryImpl.kt        # DTO → domain boundary
├── domain/model/
│   ├── Frame.kt                           # Variant isSaved
│   └── SavedFrame.kt                      # Preference and availability models
├── domain/repository/
│   └── SavedFrameRepository.kt
├── di/
│   └── SavedFrameModule.kt
└── presentation/
    ├── frames/                             # Detail toggle + Saved Frames list/state
    ├── ar/                                 # Try-On toggle; renderer unchanged
    ├── appointments/                       # Reservation coupling removed
    ├── profile/                            # Saved Frames entry
    └── navigation/                         # Typed route/access cutover

app/src/test/java/com/eyecare/app/
├── data/remote/                            # Contract fixtures + 59-route governance
├── data/repository/                        # Saved mapping/mutation/cache tests
└── presentation/                           # Saved list/detail/AR/navigation tests

docs/specs/
└── backend-alignment-v20-2026-08-27-{spec,plan,tasks}.md
```

Former `FrameReservation*` production and test files are deleted in implementation. They are not
renamed into Saved Frame types because the ownership, lifecycle, identifiers, pagination, access tier,
and user promise are all different.

## Code Style

Use wire-specific DTOs, explicit repository mapping, safe unknown values, and immutable UI-state
updates. Representative mapping and state style:

```kotlin
@Serializable
data class SavedFrameDto(
    @SerialName("product_variant_id") val productVariantId: Int,
    @SerialName("saved_at") val savedAt: String,
    val availability: String,
    val variant: SavedFrameVariantDto,
)

enum class SavedFrameAvailability {
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN;

    companion object {
        fun from(value: String): SavedFrameAvailability = when (value) {
            "available" -> AVAILABLE
            "unavailable" -> UNAVAILABLE
            else -> UNKNOWN
        }
    }
}

sealed interface SavedFramesUiState {
    data object Loading : SavedFramesUiState

    data class Success(
        val items: List<SavedFrame>,
        val currentPage: Int,
        val canLoadMore: Boolean,
        val removingVariantIds: Set<Int> = emptySet(),
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val inlineError: String? = null,
    ) : SavedFramesUiState

    data class Error(val patientSafeMessage: String) : SavedFramesUiState
}
```

Conventions:

- Kotlin properties use camelCase; wire fields use `@SerialName` snake_case.
- DTOs never escape the data layer.
- ProductVariant ID is the preference identity; no client-generated Saved Frame ID exists.
- External enums fail closed.
- Mutation controls are disabled by real state, not color alone.
- User-visible errors never expose raw Laravel/PHP or validation internals.
- No Retrofit calls, pagination ownership, or mutation state live in composables or NavGraph.

## Testing Strategy

### Contract and DTO tests

- Decode a Saved Frames page with string/number prices, nullable comparison price, nested product,
  images, typed/null AR, page metadata, and both availability values.
- Decode unknown availability safely.
- Prove `is_saved` maps for list and detail frame responses; legacy cached variants without the field
  decode as false.
- Verify PUT has no body and DELETE handles 204.
- Add contract fixtures that contain the required `is_saved` field.

### Repository tests

- Map Saved Frame DTOs to domain at the boundary without leaking DTOs.
- Save returns the server resource; repeated save remains one preference and preserves server time.
- Remove treats successful 204 idempotently.
- Page metadata drives `canLoadMore`; duplicate ProductVariant IDs are removed by presentation state.
- Catalog cache writes strip account-specific `isSaved`; offline reads never claim another account's
  saved state.
- 422/network/server errors remain typed or patient-safe at presentation.

### ViewModel tests

- Saved list initial load, refresh retention, pagination, dedupe, load-more retry, empty state, and
  per-row single-flight removal.
- Frame Detail selects an optional route variant, safely falls back for an unknown variant, toggles
  only the selected variant, prevents duplicate taps, and preserves prior state on failure.
- AR toggles the selected variant without changing renderer/camera state and changes state correctly
  after a variant switch.
- Appointment Detail loads only appointment data and has no reservation repository dependency.
- Appointment Request success no longer carries reservation-origin state.

### Compose and navigation tests

- Profile shows **Saved Frames** for linked and unlinked accounts.
- Saved list covers loading/error/empty/populated/unavailable/removing states and disclaimer semantics.
- Saved row opens the exact variant; remove controls meet touch/disabled/accessibility behavior.
- Frame Detail and AR expose accurate Save/Remove labels and in-flight semantics.
- Limited accounts can access Saved Frames without the link hub.
- No active UI string uses reservation/hold/set-aside/expiry language for frame preferences.

### Route and regression tests

- Assert exactly 8 + 40 + 11 = 59.
- Assert Saved Frames and attachment download are account-only.
- Assert all former Frame Reservation routes are rejected and absent from production Retrofit.
- Existing frame catalog, typed AR, messaging upload capability, appointment, profile, auth/linking,
  and Room tests remain green.
- Full unit, lint, and debug build commands pass.

No new numeric coverage target is introduced; the behavior matrix above is the coverage requirement.

## Boundaries

### Always

- Treat Saved Frames as account-owned preferences, never reservations or inventory commitments.
- Keep ProductVariant ID as the route/mutation identity.
- Permit Saved Frames to linked and unlinked authenticated accounts.
- Map DTOs to domain at the repository boundary.
- Keep account-specific `isSaved` out of the shared Room cache.
- Preserve existing AR security/integrity validation and attachment-upload capability checks.
- Use typed Navigation Compose routes.
- Keep updated backend documents and unrelated user work intact.
- Run `assembleDebug` after implementation changes and full verification before completion.

### Ask first

- Adding a dependency or changing the Room schema.
- Persisting Saved Frames or account-specific saved state locally.
- Adding background sync, WorkManager, or push behavior.
- Adding save mutations directly to multi-variant catalog cards.
- Changing backend payloads, availability meanings, route tiers, or pagination.
- Keeping any legacy reservation route or compatibility UI.
- Expanding the Encounter → Consultation terminology change into Android UI without a concrete Android
  label or wire-contract requirement.

### Never

- Use Gson; Kotlinx Serialization is mandatory.
- Store tokens, health data, messages, attachments, or Saved Frame ownership in Room.
- Apply `org.jetbrains.kotlin.android` under AGP 9.
- Call a retired `/frame-reservations` route.
- Ask for or require an appointment to save a frame.
- Claim that a save reserves stock, guarantees availability, or expires.
- Expose stock quantity or the backend reason a saved item is unavailable.
- Show raw backend exception text, validation internals, bearer tokens, or PHP class names.
- Let a stale or unknown external enum enable an action or availability promise.

## Success Criteria

Each condition is independently testable.

1. Production code contains exactly the three Saved Frames endpoints and no Frame Reservation endpoint.
2. Route governance passes with 8 public + 40 account-only + 11 active-link = 59 routes.
3. Saved Frames and attachment download are account-only; attachment upload remains capability-gated.
4. Catalog list/detail DTOs and domain variants expose account-specific `isSaved`.
5. Room cache writes remove saved ownership state, and legacy cache rows decode with `isSaved=false`.
6. Linked and unlinked accounts can open Saved Frames from Profile without the link hub.
7. Saved Frames paginates newest first, deduplicates by ProductVariant ID, and preserves loaded data on
   refresh/load-more failure.
8. Available/unavailable state is patient-safe and never exposes stock counts or internal causes.
9. Saved list rows open the exact saved variant in Frame Detail.
10. Frame Detail and AR save/remove the selected variant with single-flight, failure-safe state.
11. The required preference-only disclaimer is visible or directly accessible on save surfaces.
12. All reservation models, repositories, DI, screens, routes, appointment coupling, origin flags,
    presentation code, and active tests are removed.
13. No active Android frame-preference copy uses reserved, held, set aside, expiry, or appointment
    selection terminology.
14. Unknown Saved Frame availability and stale variant route IDs fail safely without crashing.
15. `CONTEXT.md`, the active-work pointer, API examples/query clarification, and route documentation are
    synchronized with the completed implementation.
16. Focused tests, `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass.

## Out of Scope

- Backend migrations, the one-time reservation conversion command, or staff Preferred Frames UI.
- Inventory movements, stock holds, purchase guarantees, checkout, quotation, or order creation.
- Local/offline Saved Frame mutation queues or Room persistence.
- Push notifications or background Saved Frame synchronization.
- A fifth bottom-navigation tab.
- New AR models, renderer/calibration changes, or camera behavior.
- Android Encounter/Consultation UI that does not currently exist.
- Message-search behavior changes; v20 only corrects the omitted cursor documentation row.

## Open Questions

1. **Approval gate:** Confirm the assumptions and scope above, especially the Frame Detail + AR mutation
   surfaces and optional variant ID on the typed Frame Detail route. After approval, Phase 2 will create
   a separate implementation plan; no production code changes begin before then.
