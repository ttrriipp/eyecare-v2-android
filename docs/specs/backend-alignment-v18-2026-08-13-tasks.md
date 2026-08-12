# Implementation Plan: Backend Alignment v18

> Spec: `docs/specs/backend-alignment-v18-2026-08-13-spec.md`
> Plan: `docs/specs/backend-alignment-v18-2026-08-13-plan.md`
> Baseline commit: `23035e8` on branch `backend-alignment-v18` — verified green.
> Date: 2026-08-13

## Overview

Realign the Android client to the 2026-08-13 patient API: remove the deleted quotation vertical,
migrate frame reservations to the two-state `is_held` model with its new item-editing routes, drop
rating revision history, surface appointment-request rejection reasons, and hide the frame-ratings
surface behind a feature flag. 21 tasks, 6 phases, 10 checkpoints.

**Build command** (this environment has no `JAVA_HOME` on the shell path — a piped Bash `./gradlew`
reports false success):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat <tasks> --console=plain
```

Referred to below as `<gradle>`. Instrumented Compose tests need the attached device via
`<gradle> connectedDebugAndroidTest`; there is no Robolectric, so they do **not** run under
`testDebugUnitTest`.

---

## Architecture Decisions

- **Risk-first ordering.** Reservations come before the quotation deletion even though deletion is
  larger. The reservation work carries every genuine unknown (204 handling, terminal navigation
  state, the merge rewrite); quotation removal is mechanical deletion that cannot surprise us. Fail
  fast on the part that can fail.
- **Strangler migration for `is_held`.** Rather than one breaking type change, `isHeld` is added
  alongside `status`, consumers migrate one at a time, and `ReservationStatus` is deleted last. Every
  task ends green — no task leaves the tree uncompilable.
- **Task 1 is a live-API bug fix, not just preparation.** `ReservationDto.status` is a non-null
  `String` with no default. The server no longer sends `status`, so `GET /frame-reservations`
  currently throws `MissingFieldException` and the reservations list is broken in production. That is
  why it is task number one.
- **Feature flag as a composable parameter**, defaulted to the global const, so both flag states stay
  renderable in tests and the soon-to-return code does not rot.
- **204 is modelled, not caught.** `Response<Unit>` / `Response<T>` with `isSuccessful`, and on the
  item-removal route the 200-vs-204 split is the signal that the reservation was deleted.

## Dependency Graph

```
ReservationDto/domain (isHeld)          Quotation vertical
      │                                       │
      ├── ReservationPresentation             ├── navigation (route + intent)
      │        │                              │        │
      │        ├── list screen                │        ├── Estimate screens/VMs
      │        └── detail screen              │        │
      │                 │                     │        └── Quotation data layer
      │                 └── create flow       │
      │                                       └── OpticalOrder ↔ quotation link
      ├── DELETE cancel ──── terminal Deleted state
      │                              │
      └── item add/remove ───────────┴── detail affordances
                                              │
                                              └── merge rewrite

revision_number removal ── independent
rejection_reason        ── independent
frame-ratings flag      ── independent (touches files also touched by quotation removal)
                                              │
                        route governance + CONTEXT.md ← depends on everything
```

---

## Task List

### Phase 1: Reservation state model

#### Task 1: Parse `is_held` and stop requiring `status`

**Description:** Add the new two-state field to the DTO and domain model alongside the existing
`status`, and make `status` tolerant of absence so live responses parse at all. This restores a
currently-broken production call before any presentation work begins.

**Acceptance criteria:**
- [ ] `ReservationDto` declares `@SerialName("is_held") val isHeld: Boolean = false` and `status` becomes `String? = null`
- [ ] `FrameReservation` carries `isHeld: Boolean`, mapped at the repository boundary; `status` remains temporarily
- [ ] A DTO test decodes the contract's literal §12 sample (no `status` key, `is_held` present, non-null `expires_at`)

**Verification:**
- [ ] `<gradle> testDebugUnitTest --tests "*FrameReservation*"` passes
- [ ] `<gradle> assembleDebug` succeeds
- [ ] Decoding a payload without `status` no longer throws `MissingFieldException`

**Dependencies:** None
**Files likely touched:** `FrameReservationDtos.kt`, `FrameReservation.kt`, `FrameReservationRepositoryImpl.kt`, `FrameReservationDtosTest.kt`
**Estimated scope:** S

#### Task 2: Two-state presentation copy and list screen

**Description:** Collapse the six-state presentation helpers to two states keyed on `isHeld`, using
the contract's patient copy verbatim, and move the reservations list onto them.

**Acceptance criteria:**
- [ ] `isHeld = false` renders exactly "Request sent — the clinic will set these aside before your visit."
- [ ] `isHeld = true` renders exactly "Set aside for your visit until {expires_at}."
- [ ] Chips read **Requested** / **Set aside**; the list's terminal-status partition (`CONVERTED`/`RELEASED`/`CANCELLED`) is gone since every returned reservation is active

**Verification:**
- [ ] Unit test asserts both strings character-exact, including the em dash
- [ ] `<gradle> testDebugUnitTest` passes

**Dependencies:** Task 1
**Files likely touched:** `ReservationPresentation.kt`, `FrameReservationListScreen.kt`, `ReservationPresentationTest.kt`
**Estimated scope:** S

> ### ✅ Checkpoint A (after Tasks 1–2)
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] Reservation list renders both states with contract copy

#### Task 3: Move detail screen and create flow onto `isHeld`

**Description:** Migrate the remaining two `status` consumers. The detail screen drops the
Requested→Prepared→Tried-on tracker (there are no longer three states to track) and keys its hold
notice off `isHeld`; the create flow's merge eligibility stops consulting the status enum.

**Acceptance criteria:**
- [ ] Detail's tracker is removed, the status pill becomes a held/requested chip, and `HoldNotice` renders only when `isHeld`
- [ ] `mergeOutcome` keys off `isHeld` and item count; `MergeOutcome.Blocked` and `activeReservationStatuses` are gone
- [ ] `MAX_RESERVATION_ITEMS = 5` lives in the domain, not in the ViewModel

**Verification:**
- [ ] `<gradle> testDebugUnitTest --tests "*CreateFrameReservation*"` passes
- [ ] `<gradle> assembleDebug` succeeds

**Dependencies:** Task 1
**Files likely touched:** `FrameReservationDetailScreen.kt`, `CreateFrameReservationViewModel.kt`, `CreateFrameReservationScreen.kt`, `FrameReservation.kt`
**Estimated scope:** M

#### Task 4: Delete `ReservationStatus`

**Description:** With no consumers left, remove the enum and the transitional `status` field. This is
the task that makes the strangler migration complete rather than permanent.

**Acceptance criteria:**
- [ ] `ReservationStatus` no longer exists in the codebase
- [ ] `FrameReservation.status` and `ReservationDto.status` are removed
- [ ] `isCancellable` is documented as always `true` (DELETE succeeds for the owner in either state); `canAddItems` / `canRemoveItems` are added per spec D3

**Verification:**
- [ ] `grep -r "ReservationStatus" app/src` returns nothing
- [ ] `<gradle> assembleDebug testDebugUnitTest` green

**Dependencies:** Tasks 2, 3
**Files likely touched:** `FrameReservation.kt`, `FrameReservationDtos.kt`, `FrameReservationRepositoryImpl.kt`
**Estimated scope:** XS

> ### ✅ Checkpoint B (after Tasks 3–4) — Phase 1 complete
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] No `ReservationStatus` reference anywhere
> - [ ] **Review with human before proceeding**

---

### Phase 2: Reservation deletion and item editing

#### Task 5: Replace cancel with DELETE and a terminal deleted state

**Description:** Swap `POST .../cancel` for `DELETE /frame-reservations/{id}`. Because the record no
longer exists after cancelling, detail can no longer render a "cancelled" reservation — it must pop
to the list, driven by a terminal state consumed exactly once.

**Acceptance criteria:**
- [ ] API declares `@DELETE("frame-reservations/{id}") suspend fun deleteReservation(...): Response<Unit>`; the cancel route is gone
- [ ] `FrameReservationRepository.cancelReservation` returns `Result<Unit>`
- [ ] A terminal `ReservationDetailUiState.Deleted` is added; the screen consumes it in a `LaunchedEffect` that calls `onBack` once, and the cancelled-banner animation is removed

**Verification:**
- [ ] ViewModel test: cancel success emits `Deleted`; recomposition does not re-emit
- [ ] Repository test: a 204 with an empty body resolves as success (not an exception)
- [ ] `<gradle> testDebugUnitTest --tests "*FrameReservation*"` passes

**Dependencies:** Task 4
**Files likely touched:** `FrameReservationApiService.kt`, `FrameReservationRepository.kt`, `FrameReservationRepositoryImpl.kt`, `FrameReservationDetailViewModel.kt`, `FrameReservationDetailScreen.kt`
**Estimated scope:** M

#### Task 6: Add the item add/remove endpoints to API and repository

**Description:** Wire the two new routes. The removal route's 200-vs-204 split is the signal that the
last item was removed and the reservation deleted, so it must be observable — hence `Response<T>`.

**Acceptance criteria:**
- [ ] `POST frame-reservations/{id}/items` and `DELETE frame-reservations/{id}/items/{itemId}` declared, the latter returning `Response<ReservationResponse>`
- [ ] `addItem(reservationId, variantId): Result<FrameReservation>` and `removeItem(reservationId, itemId): Result<FrameReservation?>`, where `null` means the reservation was deleted
- [ ] Both map 422 into `FrameReservationError.ValidationError`, matching `createReservation`

**Verification:**
- [ ] Repository tests cover: add success; remove with items remaining (200 → non-null); remove last item (204 → null); 422 → `ValidationError`
- [ ] `<gradle> testDebugUnitTest --tests "*FrameReservationRepository*"` passes

**Dependencies:** Task 5
**Files likely touched:** `FrameReservationApiService.kt`, `FrameReservationRepository.kt`, `FrameReservationRepositoryImpl.kt`, `FrameReservationRepositoryImplTest.kt`
**Estimated scope:** M

> ### ✅ Checkpoint C (after Tasks 5–6)
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] Both 204 paths proven by test, not assumption

#### Task 7: Item mutation in the detail ViewModel

**Description:** Expose add/remove on the detail ViewModel with single-flight protection, routing the
last-item case into the terminal state from Task 5.

**Acceptance criteria:**
- [ ] `removeItem(itemId)` carries per-item pending state so a double tap cannot fire twice
- [ ] A `null` result transitions to `Deleted`
- [ ] A failure surfaces inline copy and leaves the reservation intact

**Verification:**
- [ ] ViewModel tests: double-tap issues one call; last-item removal emits `Deleted`; failure preserves the reservation
- [ ] `<gradle> testDebugUnitTest --tests "*FrameReservationDetailViewModel*"` passes

**Dependencies:** Task 6
**Files likely touched:** `FrameReservationDetailViewModel.kt`, `FrameReservationDetailViewModelTest.kt`
**Estimated scope:** S

#### Task 8: Item affordances on the detail screen

**Description:** Add per-card remove and a **+ Add frame** action, both gated on `!isHeld` per spec
D3. Add navigates to the frames catalog rather than opening a second in-sheet picker (spec D4).

**Acceptance criteria:**
- [ ] Unheld reservation shows remove on each card and **+ Add frame**; Add is hidden at 5 items
- [ ] Held reservation shows neither, and explains the clinic has already set the frames aside
- [ ] **+ Add frame** navigates to the frames catalog

**Verification:**
- [ ] Compose tests for unheld (both present), held (neither present), 5-item (Add hidden)
- [ ] `<gradle> connectedDebugAndroidTest --tests "*FrameReservationDetail*"` passes

**Dependencies:** Task 7
**Files likely touched:** `FrameReservationDetailScreen.kt`, `NavGraph.kt`, `FrameReservationDetailScreenTest.kt`
**Estimated scope:** M

#### Task 9: Replace cancel-then-recreate with one add-item call

**Description:** The highest-value correctness fix in this migration. Reserving a frame for an
appointment that already has a reservation currently cancels it and recreates it with the combined
items — if the recreate fails, the patient's hold is already destroyed, which the existing code
comments acknowledge. One `addItem` call replaces the whole dance.

**Acceptance criteria:**
- [ ] `mergeIntoExisting` issues exactly one `addItem` call
- [ ] The cancel-then-recreate path and its "we released your previous hold" apology copy are deleted
- [ ] The test carries a comment explaining that the zero-delete assertion is a permanent guard, not a snapshot of current behavior

**Verification:**
- [ ] ViewModel test asserts one `addItem` **and** `verify(exactly = 0)` on the delete path
- [ ] `<gradle> testDebugUnitTest --tests "*CreateFrameReservationViewModel*"` passes

**Dependencies:** Task 6
**Files likely touched:** `CreateFrameReservationViewModel.kt`, `CreateFrameReservationScreen.kt`, `CreateFrameReservationViewModelTest.kt`
**Estimated scope:** M

> ### ✅ Checkpoint D (after Tasks 7–9) — Phase 2 complete
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] Merge issues one add-item call and zero deletes
> - [ ] End-to-end: reserve → add frame → remove frame → cancel works on device
> - [ ] **Review with human before proceeding**

---

### Phase 3: Quotation removal and the My Orders rename

#### Task 10: Navigation removal and rename

**Description:** Remove the `EstimateDetail` destination and rename the eyewear destination to My
Orders. Navigation goes first so that deleting the screens afterwards cannot leave dangling route
references.

**Acceptance criteria:**
- [ ] Route `EstimateDetail`, its `composable` block, and `PatientFeatureIntent.EstimateDetail` are gone, including the protected-destination list entry
- [ ] `MyEyewear` → `MyOrders` across route, intent, screen file (`MyOrdersScreen.kt`), composable name, and app-bar title; the Estimates section and its `estimateViewModel` are removed from the screen
- [ ] The Profile **Care & activity** row reads "My Orders" and its test asserts the new label

**Verification:**
- [ ] `<gradle> assembleDebug` succeeds
- [ ] `grep -ri "my eyewear" app/src` returns nothing

**Dependencies:** None
**Files likely touched:** `Routes.kt`, `NavGraph.kt`, `PatientFeatureIntent.kt`, `MyEyewearScreen.kt` → `MyOrdersScreen.kt`, `ProfileScreen.kt`, `ProfileScreenTest.kt`
**Estimated scope:** M

#### Task 11: Delete the Estimate presentation layer

**Description:** With nothing routing to them, delete the Estimate screens and ViewModels and the
estimate-specific presentation helpers.

**Acceptance criteria:**
- [ ] `Estimate{List,Detail}Screen.kt`, `Estimate{List,Detail}ViewModel.kt` and their two test files are deleted
- [ ] `estimateStatusLabel`, `estimateStatusColor`, `estimateCardTitle`, `estimateDateLabel` are removed from `EyewearPresentation.kt`, along with their assertions

**Verification:**
- [ ] `<gradle> assembleDebug testDebugUnitTest` green

**Dependencies:** Task 10
**Files likely touched:** 6 deletions, `EyewearPresentation.kt`, `EyewearPresentationTest.kt`
**Estimated scope:** M *(mostly deletions)*

> ### ✅ Checkpoint E (after Tasks 10–11)
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] Destination reads "My Orders" and lists orders only

#### Task 12: Delete the Quotation data and domain vertical

**Description:** Remove the API service, DTOs, repository, domain model, and Hilt module for a
resource the server no longer exposes.

**Acceptance criteria:**
- [ ] `QuotationApiService.kt`, `QuotationDtos.kt`, `QuotationRepository.kt`, `QuotationRepositoryImpl.kt`, `Quotation.kt`, `QuotationModule.kt` deleted
- [ ] `QuotationDtosTest.kt`, `QuotationRepositoryImplTest.kt` deleted
- [ ] Hilt no longer binds a `QuotationRepository`

**Verification:**
- [ ] `<gradle> assembleDebug testDebugUnitTest` green (Hilt graph resolves)

**Dependencies:** Task 11
**Files likely touched:** 8 deletions
**Estimated scope:** M *(all deletions)*

#### Task 13: Sever the order ↔ quotation link

**Description:** Per spec D1, an order screen shows order information only. Remove the quotation
reference end-to-end rather than demoting it to a non-tappable row.

**Acceptance criteria:**
- [ ] `QuotationReferenceDto`, `OpticalOrderDtos.sourceQuotation`, `QuotationReference`, `OpticalOrder.sourceQuotation`, and the repository mapping are removed
- [ ] The order-detail reference row and `OpticalOrderListScreen`'s estimate cross-link are removed
- [ ] `OpticalOrderDtosTest` still decodes a payload that *contains* `source_quotation` and passes, proving `ignoreUnknownKeys` absorbs it

**Verification:**
- [ ] `<gradle> testDebugUnitTest --tests "*OpticalOrder*"` passes
- [ ] `grep -ri "quotation\|estimate" app/src/main app/src/androidTest` returns nothing

**Dependencies:** Task 12
**Files likely touched:** `OpticalOrderDtos.kt`, `OpticalOrder.kt`, `OpticalOrderRepositoryImpl.kt`, `OpticalOrderDetailScreen.kt`, `OpticalOrderListScreen.kt`, `OpticalOrderDtosTest.kt`
**Estimated scope:** M

> ### ✅ Checkpoint F (after Tasks 12–13) — Phase 3 complete
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] Zero quotation/estimate references in `main` or `androidTest`
> - [ ] **Review with human before proceeding**

---

### Phase 4: Payload alignment

#### Task 14: Remove `revision_number` from the optical-order data layer

**Description:** The field is gone from the API. Strip it from the order DTOs, domain models, and
repository mapping.

**Acceptance criteria:**
- [ ] Both `OpticalOrderDtos` occurrences removed
- [ ] `RatingSummary.revisionNumber` and `RatingResult.revisionNumber` removed
- [ ] `OpticalOrderRepositoryImpl` mappings updated

**Verification:**
- [ ] `<gradle> testDebugUnitTest --tests "*OpticalOrderDtos*"` passes with `revision_number` removed from fixtures

**Dependencies:** Task 13
**Files likely touched:** `OpticalOrderDtos.kt`, `OpticalOrder.kt`, `OpticalOrderRepositoryImpl.kt`, `OpticalOrderDtosTest.kt`
**Estimated scope:** S

#### Task 15: Remove the "Edited" badge from order presentation

**Description:** With no revision number, `isEdited()` can only ever return false — dead code
pretending to be a feature. Delete it and its badge.

**Acceptance criteria:**
- [ ] `EyewearPresentation.isEdited` deleted along with its test assertions
- [ ] The "Edited" badge is removed from `OpticalOrderDetailScreen`
- [ ] `OpticalOrderDetailViewModel` no longer passes a revision number

**Verification:**
- [ ] `<gradle> testDebugUnitTest` green
- [ ] `grep -r "isEdited" app/src` returns nothing

**Dependencies:** Task 14
**Files likely touched:** `EyewearPresentation.kt`, `EyewearPresentationTest.kt`, `OpticalOrderDetailScreen.kt`, `OpticalOrderDetailViewModel.kt`
**Estimated scope:** S

#### Task 16: Remove `revision_number` from the appointment side

**Description:** Same removal for visit ratings, including the "Edited" badge on appointment detail.

**Acceptance criteria:**
- [ ] `AppointmentV1Dtos.VisitRatingDto.revisionNumber` and `AppointmentV1.VisitRating.revisionNumber` removed
- [ ] `AppointmentV1RepositoryImpl` mapping updated
- [ ] The "Edited" badge is removed from `AppointmentDetailScreen`

**Verification:**
- [ ] `grep -r "revision_number\|revisionNumber" app/src` returns nothing
- [ ] `<gradle> assembleDebug testDebugUnitTest` green

**Dependencies:** Task 15
**Files likely touched:** `AppointmentV1Dtos.kt`, `AppointmentV1.kt`, `AppointmentV1RepositoryImpl.kt`, `AppointmentDetailScreen.kt`
**Estimated scope:** S

> ### ✅ Checkpoint G (after Tasks 14–16)
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] No `revision_number` and no "Edited" badge anywhere

#### Task 17: Surface appointment-request rejection reasons

**Description:** The API now returns why staff rejected a request. Show it, so a rejected request
stops being a dead end.

**Acceptance criteria:**
- [ ] Nullable `@SerialName("rejection_reason")` on the request DTO; `AppointmentRequest.rejectionReason: String?`; repository mapping
- [ ] A rejection row renders only when status is `REJECTED` **and** the reason is non-blank, styled as a clinic notice rather than an error
- [ ] Legacy records without the key still decode

**Verification:**
- [ ] DTO test decodes a rejected payload with a reason and a pending payload without the key
- [ ] Compose test: row present for rejected-with-reason, absent otherwise
- [ ] `<gradle> testDebugUnitTest --tests "*AppointmentRequest*"` passes

**Dependencies:** None
**Files likely touched:** `AppointmentRequestDtos.kt`, `AppointmentRequest.kt`, `AppointmentRequestRepositoryImpl.kt`, `AppointmentRequestDetailScreen.kt`, + 2 tests
**Estimated scope:** M

> ### ✅ Checkpoint H (after Task 17) — Phase 4 complete
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] Rejected requests display their reason

---

### Phase 5: Frame-ratings feature flag

#### Task 18: Introduce the flag and gate the order-item rating surface

**Description:** Add the kill switch and apply it to My Orders. The flag is a composable parameter
defaulted to the global const so both states stay testable — re-enablement is expected soon and the
hidden code must not rot.

**Acceptance criteria:**
- [ ] `presentation/common/FeatureFlags.kt` declares a documented `const val FRAME_RATINGS_ENABLED = false`, stating that flipping the boolean is the entire restore procedure
- [ ] The rating action **and** its rating readout are gated in `OpticalOrderDetailScreen` (showing a rating the patient cannot revise is worse than showing none)
- [ ] Gating is a parameter (`ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED`), not a direct global read

**Verification:**
- [ ] Compose test renders both `ratingsEnabled = false` (absent) and `true` (present)
- [ ] `<gradle> testDebugUnitTest --tests "*FrameRating*"` still passes — the flag hides UI, not logic

**Dependencies:** Task 15
**Files likely touched:** `FeatureFlags.kt` *(new)*, `OpticalOrderDetailScreen.kt`, `OpticalOrderDetailScreenTest.kt`
**Estimated scope:** S

#### Task 19: Gate the frame rating badges

**Description:** Hide the ★ aggregate on the catalog card and frame detail, including the "No ratings
yet" branch.

**Acceptance criteria:**
- [ ] `RatingBadge` in `FrameCard` and `RatingBadgeDetail` in `FrameDetailScreen` are gated by the same parameter pattern
- [ ] The "No ratings yet" detail branch is also hidden
- [ ] `RatingBadge` itself is unchanged — gating lives at the call site

**Verification:**
- [ ] Compose test: no ★ badge on a frame card whose `averageRating` is non-null; no "No ratings yet" on detail
- [ ] `<gradle> connectedDebugAndroidTest --tests "*Frame*"` passes

**Dependencies:** Task 18
**Files likely touched:** `FrameCard.kt`, `FrameDetailScreen.kt`, + Compose test
**Estimated scope:** S

> ### ✅ Checkpoint I (after Tasks 18–19) — Phase 5 complete
> - [ ] `<gradle> assembleDebug testDebugUnitTest` green
> - [ ] No rating surface reachable in the debug build; rating logic still under test

---

### Phase 6: Route governance and living documentation

#### Task 20: Close the contract at 54 routes

**Description:** Bring the allowlist to the 2026-08-13 contract and make every deleted route a
regression tripwire.

**Acceptance criteria:**
- [ ] Removed: `GET /quotations`, `GET /quotations/{quotation}`, `POST /frame-reservations/{reservation}/cancel`. Added: `DELETE /frame-reservations/{reservation}`, `POST /frame-reservations/{reservation}/items`, `DELETE /frame-reservations/{reservation}/items/{item}`. Active-link stays **17**; total **54**
- [ ] `legacyAliasRoutes` emptied; `POST /job-order-items/{item}/rating` moved to `rejectedRoutes` alongside the two quotation routes and the old cancel route
- [ ] `POST /optical-order-items/{item}/rating` **stays** active-link — an approved route whose UI is merely flag-gated
- [ ] Header comments updated from "V17 / 55" to "V18 / 54"

**Verification:**
- [ ] `<gradle> testDebugUnitTest --tests "*ApiRouteAllowlistTest*"` passes
- [ ] The discovery test fails if a rejected route is reintroduced into a production Retrofit annotation

**Dependencies:** Tasks 4, 6, 13
**Files likely touched:** `ApprovedApiRoutes.kt`, `ApiRouteAllowlistTest.kt`
**Estimated scope:** S

#### Task 21: Update CONTEXT.md

**Description:** Keep the living document honest — it is the first thing read in a new session.

**Acceptance criteria:**
- [ ] §Frame Reservations rewritten for the two-state model, DELETE, and item editing
- [ ] §My Eyewear replaced by §My Orders; the flagged frame-ratings note added
- [ ] The "Known backend bug" paragraph **deleted** (fixed server-side 2026-08-13)
- [ ] §Route Governance and §Backend API updated to 54; the three v18 docs added to §Active Specs

**Verification:**
- [ ] Manual read-through: no statement in `CONTEXT.md` contradicts `docs/API_CONTRACT.md`
- [ ] `grep -c "55" CONTEXT.md` shows no stale route counts

**Dependencies:** Task 20
**Files likely touched:** `CONTEXT.md`
**Estimated scope:** XS

> ### ✅ Checkpoint J (final)
> - [ ] `<gradle> assembleDebug testDebugUnitTest ktlintCheck` all green
> - [ ] `<gradle> connectedDebugAndroidTest` green
> - [ ] All 13 spec success criteria verified
> - [ ] **Ready for review**

---

## Risks and Mitigations

| Risk | Impact | Mitigation | Kind |
|---|---|---|---|
| 204 responses crash body-typed Retrofit methods | High | `Response<Unit>` / `Response<T>` + `isSuccessful`; both paths proven by test in Tasks 5–6 | Prevent by construction |
| Merge regresses to cancel-then-recreate, destroying holds | High | `verify(exactly = 0)` on the delete path with an explanatory comment (Task 9) | Permanent test guard |
| Cancel-success pop re-fires on recomposition | Medium | Terminal `Deleted` state consumed by `LaunchedEffect` (Task 5) | Prevent by construction |
| Flagged-off rating code rots before re-enablement | Medium | Flag as composable parameter; both states tested (Tasks 18–19) | Prevent by construction |
| Breaking type change leaves the tree uncompilable mid-migration | Medium | Strangler: add `isHeld`, migrate, then delete (Tasks 1–4) | Prevent by construction |
| Orphaned `MyEyewear`/`Estimate` strings in androidTest | Medium | Grep both `main` and `androidTest` at Checkpoints E and F | Verify once |
| Allowlist and contract silently diverge | Medium | Task 20 asserts the count explicitly and runs last | Verify once |
| Compose verification unavailable | Low | Device is attached; `connectedDebugAndroidTest` smoke-run before Task 8 relies on it | Verify once |

## Parallelization

- **Safe to parallelize:** Phase 1–2 (reservations), Phase 3 (quotations), and Task 17
  (rejection reason) touch disjoint files and could run in separate sessions.
- **Must be sequential:** Tasks 1→4 (strangler order), 5→6→7→8 (dependency chain), and Phase 6 last.
- **Needs coordination:** Phase 5 touches `OpticalOrderDetailScreen.kt`, which Phase 3 Task 13 and
  Phase 4 Task 15 also edit — keep them in one session or rebase carefully.

Execution here is serial: 1 → 21.

## Open Questions

None. All three spec questions were resolved on 2026-08-13.
