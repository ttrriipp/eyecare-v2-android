# Tasks: Backend Alignment v18 — Commerce Simplification & Simplified Frame Reservations

> Status: **Draft — awaiting review**
> Spec: `docs/specs/backend-alignment-v18-2026-08-13-spec.md`
> Plan: `docs/specs/backend-alignment-v18-2026-08-13-plan.md`
> Date: 2026-08-13

**19 tasks across 6 checkpoints.** Tasks are ordered by dependency. No task touches more than ~6 files.

**Build note:** this environment has no `JAVA_HOME` on the shell path. Every verify command must be
run from PowerShell with:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat <tasks> --console=plain
```
Referred to below as `<gradle>`.

---

## Stage 0 — Baseline

- [ ] **Task 0: Confirm the tree is green before any edit**
  - Acceptance: `assembleDebug` and `testDebugUnitTest` both succeed on the current working tree,
    with the pre-existing 36 uncommitted files in place.
  - Verify: `<gradle> assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL`.
  - Files: none (verification only).
  - **Blocking:** if red, stop and report. Do not layer this migration onto a broken baseline.

---

## Stage 1 — Quotation removal and the My Orders rename

- [ ] **Task 1: Delete the Quotation data + domain vertical**
  - Acceptance: `QuotationApiService.kt`, `QuotationDtos.kt`, `QuotationRepository.kt`,
    `QuotationRepositoryImpl.kt`, `Quotation.kt`, `QuotationModule.kt` are gone, along with
    `QuotationDtosTest.kt` and `QuotationRepositoryImplTest.kt`. Hilt no longer binds a
    `QuotationRepository`.
  - Verify: compile fails **only** in the Estimate presentation layer (expected — Task 2 clears it);
    no unresolved reference anywhere else.
  - Files: 6 deleted in `main`, 2 deleted in `test`.

- [ ] **Task 2: Delete the Estimate presentation layer**
  - Acceptance: `EstimateListScreen.kt`, `EstimateListViewModel.kt`, `EstimateDetailScreen.kt`,
    `EstimateDetailViewModel.kt`, `EstimateListViewModelTest.kt`, `EstimateDetailViewModelTest.kt`
    are gone. `EyewearPresentation.kt` no longer declares `estimateStatusLabel`,
    `estimateStatusColor`, `estimateCardTitle`, or `estimateDateLabel`, and
    `EyewearPresentationTest` no longer asserts them.
  - Verify: `<gradle> compileDebugKotlin` fails only in navigation and `MyEyewearScreen` (cleared by
    Tasks 3–4).
  - Files: 6 deleted, `EyewearPresentation.kt`, `EyewearPresentationTest.kt`.

- [ ] **Task 3: Sever the order↔quotation link**
  - Acceptance: `QuotationReferenceDto` and `OpticalOrderDtos.sourceQuotation` removed;
    `QuotationReference` and `OpticalOrder.sourceQuotation` removed; the mapping in
    `OpticalOrderRepositoryImpl` removed; the order-detail reference row and
    `OpticalOrderListScreen.onViewEstimate`/`onNavigateToEstimate` parameters removed.
    `OpticalOrderDtosTest` still decodes a payload that **contains** `source_quotation` and passes,
    proving `ignoreUnknownKeys` absorbs it.
  - Verify: `<gradle> testDebugUnitTest --tests "*OpticalOrder*"` green.
  - Files: `OpticalOrderDtos.kt`, `OpticalOrder.kt`, `OpticalOrderRepositoryImpl.kt`,
    `OpticalOrderDetailScreen.kt`, `OpticalOrderListScreen.kt`, `OpticalOrderDtosTest.kt`.

- [ ] **Task 4: Rename the destination to My Orders**
  - Acceptance: `MyEyewearScreen.kt` → `MyOrdersScreen.kt` with composable `MyOrdersScreen`; the
    Estimates section and its heading are gone, leaving the Orders list only; app-bar title reads
    "My Orders"; the `estimateViewModel` parameter and its `ON_RESUME` refresh are removed.
  - Verify: screen renders the orders list with no Estimates heading.
  - Files: `MyEyewearScreen.kt` (renamed).

- [ ] **Task 5: Update navigation for the removal and rename**
  - Acceptance: route `EstimateDetail` and its `composable<EstimateDetail>` block deleted; route
    `MyEyewear` → `MyOrders`; `PatientFeatureIntent.EstimateDetail` deleted and
    `PatientFeatureIntent.MyEyewear` → `MyOrders` across the intent↔route mapping and the
    protected-destination list; the `!route.contains("Quotation")` guard in `NavGraph` removed as
    dead.
  - Verify: `<gradle> assembleDebug` succeeds — first full green compile of the stage.
  - Files: `Routes.kt`, `NavGraph.kt`, `PatientFeatureIntent.kt`.

- [ ] **Task 6: Update the Profile entry point and its tests**
  - Acceptance: the **Care & activity** row reads "My Orders" and navigates to `MyOrders`;
    `ProfileScreenTest` asserts the new label; no `androidTest` file references "My Eyewear" or
    "Estimate".
  - Verify: `<gradle> assembleDebug` green **and**
    `grep -ril "quotation\|estimate\|my eyewear" app/src/main app/src/androidTest` returns nothing.
  - Files: `ProfileScreen.kt`, `ProfileScreenTest.kt`.

> ### ✅ Checkpoint 1
> `<gradle> assembleDebug testDebugUnitTest` green. No `Quotation`/`Estimate`/`MyEyewear` identifier
> or user-visible string survives in `main` or `androidTest`.

---

## Stage 2 — Reservation two-state migration

- [ ] **Task 7: Migrate the reservation domain model**
  - Acceptance: `ReservationStatus` deleted; `FrameReservation.status` replaced by
    `isHeld: Boolean`; `isCancellable` is `true` for every reservation (documented: DELETE succeeds
    for the owner in either state); `canAddItems` = `!isHeld && items.size < MAX_RESERVATION_ITEMS`
    and `canRemoveItems` = `!isHeld && items.isNotEmpty()` added; `MAX_RESERVATION_ITEMS = 5` moved
    into the domain from `CreateFrameReservationViewModel.maxReservationItems`.
  - Verify: compile fails only in reservation data/presentation (cleared by Tasks 8–11).
  - Files: `FrameReservation.kt`.

- [ ] **Task 8: Migrate the reservation DTO, API, and repository**
  - Acceptance: `ReservationDto.status: String` → `@SerialName("is_held") val isHeld: Boolean = false`;
    `cancelReservation` replaced by `@DELETE("frame-reservations/{id}") suspend fun
    deleteReservation(...): Response<Unit>`; `FrameReservationRepository.cancelReservation` returns
    `Result<Unit>`; mapping sets `isHeld`. A new DTO test decodes the contract's literal
    `is_held`/`expires_at` sample.
  - Verify: `<gradle> testDebugUnitTest --tests "*FrameReservation*"` green; a 204 with an empty body
    resolves as success (asserted with a MockK-stubbed `Response.success(null)`).
  - Files: `FrameReservationDtos.kt`, `FrameReservationApiService.kt`,
    `FrameReservationRepository.kt`, `FrameReservationRepositoryImpl.kt`, + DTO/repo tests.

- [ ] **Task 9: Rewrite reservation presentation copy**
  - Acceptance: `ReservationPresentation.kt` exposes two-state helpers keyed on `isHeld`, using the
    contract's copy verbatim — `false` → "Request sent — the clinic will set these aside before your
    visit."; `true` → "Set aside for your visit until {expires_at}." Chip labels are **Requested** /
    **Set aside**. All six-state labels, explanations, and colors are gone.
  - Verify: unit test asserts both strings exactly, including the em dash.
  - Files: `ReservationPresentation.kt`, + a new presentation test.

- [ ] **Task 10: Update the reservation list screen**
  - Acceptance: the terminal-status partition (`CONVERTED`/`RELEASED`/`CANCELLED`) is gone — every
    returned reservation is active; the status chip becomes a held/requested chip; the
    `PREPARED`-gated row detail keys off `isHeld`.
  - Verify: `<gradle> assembleDebug` green; list renders both states.
  - Files: `FrameReservationListScreen.kt`.

- [ ] **Task 11: Update the reservation detail screen and ViewModel for deletion**
  - Acceptance: the Requested→Prepared→Tried-on tracker is removed; the status pill becomes a held
    chip; `HoldNotice` renders only when `isHeld`; a terminal
    `ReservationDetailUiState.Deleted` is added and a successful cancel transitions to it; the screen
    consumes it with a `LaunchedEffect` that calls `onBack` exactly once; the cancelled-banner
    animation is removed.
  - Verify: ViewModel test — cancel success emits `Deleted`; recomposition does not re-emit. Compose
    test — confirming cancel invokes `onBack` once.
  - Files: `FrameReservationDetailScreen.kt`, `FrameReservationDetailViewModel.kt`,
    + `FrameReservationDetailViewModelTest`.

> ### ✅ Checkpoint 2
> `<gradle> assembleDebug testDebugUnitTest` green. `grep -r "ReservationStatus" app/src` returns
> nothing. Cancelling pops to the list.

---

## Stage 3 — Reservation item editing

- [ ] **Task 12: Add the item endpoints to API and repository**
  - Acceptance: `POST frame-reservations/{id}/items` and
    `DELETE frame-reservations/{id}/items/{itemId}` declared, the latter as
    `Response<ReservationResponse>` so 200-vs-204 is observable;
    `addItem(reservationId, variantId): Result<FrameReservation>` and
    `removeItem(reservationId, itemId): Result<FrameReservation?>` added, with `null` meaning the
    reservation was deleted (204); both map 422 into `FrameReservationError.ValidationError`.
  - Verify: repo tests cover add success, remove-with-items-remaining (200 → non-null),
    remove-last-item (204 → null), and a 422 producing `ValidationError`.
  - Files: `FrameReservationApiService.kt`, `FrameReservationRepository.kt`,
    `FrameReservationRepositoryImpl.kt`, + repo tests.

- [ ] **Task 13: Wire item mutation into the detail ViewModel**
  - Acceptance: `removeItem(itemId)` is single-flight with per-item pending state so a double tap
    cannot fire twice; a `null` result transitions to the terminal `Deleted` state from Task 11; a
    failure surfaces inline copy and leaves the reservation intact.
  - Verify: ViewModel tests — double-tap removal issues one call; last-item removal emits `Deleted`;
    failure preserves the reservation and shows an error.
  - Files: `FrameReservationDetailViewModel.kt`, + tests.

- [ ] **Task 14: Add the item affordances to the detail screen**
  - Acceptance: each frame card carries a remove affordance and the screen carries **+ Add frame**,
    both rendered only when `!isHeld`; Add is hidden at 5 items; Add navigates to the frames catalog;
    a held reservation shows neither and instead explains that the clinic has already set the frames
    aside.
  - Verify: Compose tests for the unheld state (both affordances present), the held state (neither
    present), and the 5-item state (Add hidden).
  - Files: `FrameReservationDetailScreen.kt`, `NavGraph.kt` (catalog callback), + Compose test.

- [ ] **Task 15: Replace cancel-then-recreate with a single add-item call**
  - Acceptance: `mergeIntoExisting` issues exactly one `addItem` call; the cancel-then-recreate path
    and its "we released your previous hold" apology copy are deleted; `mergeOutcome` keys off
    `isHeld` and item count, and `MergeOutcome.Blocked` is removed along with
    `activeReservationStatuses`; `CreateFrameReservationScreen`'s status-based cancellable check is
    updated.
  - Verify: ViewModel test asserts one `addItem` call **and** `verify(exactly = 0)` on the delete
    path — a regression to cancel-then-recreate fails the suite.
  - Files: `CreateFrameReservationViewModel.kt`, `CreateFrameReservationScreen.kt`,
    `CreateFrameReservationViewModelTest.kt`.

> ### ✅ Checkpoint 3
> `<gradle> assembleDebug testDebugUnitTest` green. Reserving into an existing unheld reservation
> issues one add-item call and zero deletes.

---

## Stage 4 — Payload alignment

- [ ] **Task 16: Remove `revision_number` end-to-end**
  - Acceptance: the field is gone from `AppointmentV1Dtos.VisitRatingDto`, both `OpticalOrderDtos`
    occurrences, `AppointmentV1.VisitRating`, both `OpticalOrder` rating models, both repository
    mappings, and `OpticalOrderDetailViewModel`; `EyewearPresentation.isEdited` is deleted along with
    its test; the "Edited" badge is removed from `AppointmentDetailScreen` and
    `OpticalOrderDetailScreen`; `OpticalOrderDtosTest` and `FrameRatingViewModelTest` are updated.
  - Verify: `grep -r "revision_number\|revisionNumber\|isEdited" app/src` returns nothing;
    `<gradle> testDebugUnitTest` green.
  - Files: ~11 across dto/domain/repository/presentation + 3 test files.

- [ ] **Task 17: Add `rejection_reason` to appointment requests**
  - Acceptance: nullable `@SerialName("rejection_reason")` on the request DTO;
    `AppointmentRequest.rejectionReason: String?`; repository mapping; a rejection row on
    `AppointmentRequestDetailScreen` shown only when status is `REJECTED` **and** the reason is
    non-blank, styled as a clinic notice rather than an error.
  - Verify: DTO test decodes both a rejected payload with a reason and a pending payload without the
    key; Compose test asserts the row appears for rejected-with-reason and is absent otherwise.
  - Files: `AppointmentRequestDtos.kt`, `AppointmentRequest.kt`,
    `AppointmentRequestRepositoryImpl.kt`, `AppointmentRequestDetailScreen.kt`, + 2 tests.

> ### ✅ Checkpoint 4
> `<gradle> assembleDebug testDebugUnitTest` green. No `revision_number` anywhere; rejected requests
> show their reason.

---

## Stage 5 — Frame-ratings feature flag

- [ ] **Task 18: Introduce the flag and gate both rating surfaces**
  - Acceptance: `presentation/common/FeatureFlags.kt` declares a documented
    `const val FRAME_RATINGS_ENABLED = false` noting that re-enablement is expected soon and that
    flipping the boolean is the entire restore procedure. Gated: the order-item rating action **and**
    its rating readout in `OpticalOrderDetailScreen`; `RatingBadge` in `FrameCard`;
    `RatingBadgeDetail` including its "No ratings yet" branch in `FrameDetailScreen`. DTOs,
    repositories, `FrameRatingViewModel`, `FrameRatingDialog`, and `FrameRatingViewModelTest` are
    untouched and still compile and pass.
  - Verify: `<gradle> testDebugUnitTest` green including `FrameRatingViewModelTest`; Compose tests
    assert no rating affordance on order detail, no ★ badge on a frame card with a non-null
    `averageRating`, and no "No ratings yet" on frame detail.
  - Files: `FeatureFlags.kt` (new), `OpticalOrderDetailScreen.kt`, `FrameCard.kt`,
    `FrameDetailScreen.kt`, + Compose tests.

> ### ✅ Checkpoint 5
> `<gradle> assembleDebug testDebugUnitTest` green. No rating surface is reachable; the rating code
> path is still tested.

---

## Stage 6 — Route governance and living documentation

- [ ] **Task 19: Close the contract at 54 routes and update CONTEXT.md**
  - Acceptance:
    - `ApprovedApiRoutes` — remove `GET /quotations`, `GET /quotations/{quotation}`,
      `POST /frame-reservations/{reservation}/cancel`; add `DELETE /frame-reservations/{reservation}`,
      `POST /frame-reservations/{reservation}/items`,
      `DELETE /frame-reservations/{reservation}/items/{item}`. Active-link stays **17**; total **54**.
    - `POST /optical-order-items/{item}/rating` **remains** active-link (approved route, UI gated by
      the Stage 5 flag).
    - `legacyAliasRoutes` emptied; `POST /job-order-items/{item}/rating` moved into `rejectedRoutes`
      together with the two quotation routes and the old reservation cancel route.
    - Header comments and counts updated from "V17 / 55" to "V18 / 54".
    - `CONTEXT.md` — rewrite §Frame Reservations for the two-state model and item editing; replace
      §My Eyewear with §My Orders; add the flagged frame-ratings note; **delete** the "Known backend
      bug" paragraph (fixed server-side 2026-08-13); update §Route Governance and §Backend API to 54;
      add the three v18 docs to §Active Specs.
  - Verify: `<gradle> testDebugUnitTest --tests "*ApiRouteAllowlistTest*"` green, with the discovery
    test failing if any production Retrofit annotation references a rejected route.
  - Files: `ApprovedApiRoutes.kt`, `ApiRouteAllowlistTest.kt`, `CONTEXT.md`.

> ### ✅ Checkpoint 6 (final)
> `<gradle> assembleDebug testDebugUnitTest ktlintCheck` all green, and every one of the spec's 13
> success criteria verified.

---

## Traceability

| Spec success criterion | Task(s) |
|---|---|
| 1 Build + tests pass | 0, all checkpoints |
| 2 Allowlist = 54, rejects deleted routes | 19 |
| 3 No quotation/estimate reference in `main` | 1, 2, 3, 6 |
| 4 `ReservationStatus` gone | 7 |
| 5 Contract copy rendered verbatim | 9 |
| 6 Merge = 1 add-item, 0 deletes | 15 |
| 7 Last-item removal pops to list | 12, 13 |
| 8 Cancel issues DELETE and pops | 8, 11 |
| 9 Rejection reason shown when rejected | 17 |
| 10 Rating surfaces hidden behind flag | 18 |
| 11 No "Edited" badge, no `revision_number` | 16 |
| 12 Destination reads "My Orders" | 4, 5, 6 |
| 13 `CONTEXT.md` current | 19 |
