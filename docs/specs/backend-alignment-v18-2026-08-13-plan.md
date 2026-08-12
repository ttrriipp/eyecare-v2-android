# Plan: Backend Alignment v18 — Commerce Simplification & Simplified Frame Reservations

> Status: **Draft — awaiting review**
> Spec: `docs/specs/backend-alignment-v18-2026-08-13-spec.md`
> Date: 2026-08-13

---

## Shape of the work

Six stages. Stages 1–3 are sequential (3 builds on 2's model change). Stages 4 and 5 are independent
of 1–3 and of each other. Stage 6 closes the contract and must run last.

```
Stage 1  Quotation removal + My Orders rename          ─┐
Stage 2  Reservation two-state migration                │  independent of 4/5
Stage 3  Reservation item editing (new routes)          │  depends on 2
                                                        │
Stage 4  Payload alignment (revision_number, reason)   ─┤  independent
Stage 5  Frame-ratings feature flag                    ─┘  independent

Stage 6  Route governance + CONTEXT.md + full verify    ← last, depends on all
```

Each stage ends at a checkpoint: `./gradlew assembleDebug` **and** `./gradlew testDebugUnitTest` both
green. No stage is "done" on a compile alone.

---

## Stage 0 — Baseline (prerequisite, not a change)

The working tree carries substantial uncommitted work across the reservation and eyewear screens
(36 files, ~2600 insertions). Establish that the tree builds and tests green **before** touching
anything, so any later failure is attributable to this plan rather than pre-existing state.

- Run `./gradlew assembleDebug testDebugUnitTest`.
- If the baseline is already red, stop and report — do not layer a migration onto a broken tree.

---

## Stage 1 — Quotation removal and the My Orders rename

**Why first:** it is the largest deletion and touches navigation. Doing it first means every later
stage compiles against the final file set instead of against files scheduled for deletion.

**Components**

1. *Delete the vertical* — `QuotationApiService`, `QuotationDtos`, `QuotationRepository(+Impl)`,
   `Quotation` domain model, `QuotationModule`, `Estimate{List,Detail}Screen`,
   `Estimate{List,Detail}ViewModel`, and their four test files.
2. *Sever the order↔quotation link* — remove `QuotationReferenceDto`, `OpticalOrder.sourceQuotation`,
   `QuotationReference`, the mapping in `OpticalOrderRepositoryImpl`, the order-detail reference row,
   and `OpticalOrderListScreen.onViewEstimate`.
3. *Navigation* — delete the `EstimateDetail` route, its `composable<EstimateDetail>` block, and
   `PatientFeatureIntent.EstimateDetail` (including the intent↔route mapping and the gate's
   protected-destination list).
4. *Rename* — `MyEyewear` → `MyOrders` across route, intent, screen file, composable name, app-bar
   title, and the Profile **Care & activity** row label.
5. *Presentation cleanup* — drop `estimateStatusLabel`, `estimateStatusColor`, `estimateCardTitle`,
   `estimateDateLabel` from `EyewearPresentation.kt` and their assertions in
   `EyewearPresentationTest`.

**Order within the stage:** navigation last. Deleting screens first surfaces every reference through
compiler errors, which is a more reliable checklist than grepping.

**Risk — orphaned Compose/androidTest references.** `ProfileScreenTest` asserts the "My Eyewear" row
label. Mitigation: grep `MyEyewear|Estimate` across `androidTest` before declaring the stage done.

**Checkpoint 1:** builds and tests green; `grep -ri "quotation\|estimate" app/src/main` returns nothing.

---

## Stage 2 — Reservation two-state migration

**Components**

1. *Domain* — delete `ReservationStatus`; `FrameReservation.status: ReservationStatus` becomes
   `isHeld: Boolean`. `isCancellable` becomes a constant `true` (DELETE succeeds in either state);
   add `canAddItems`/`canRemoveItems` per spec D3.
2. *DTO* — `ReservationDto.status: String` → `@SerialName("is_held") isHeld: Boolean = false`.
3. *API* — replace `@POST("frame-reservations/{id}/cancel")` with
   `@DELETE("frame-reservations/{id}")` returning `Response<Unit>`.
4. *Repository* — `cancelReservation` returns `Result<Unit>`; mapping sets `isHeld`.
5. *Presentation* — `ReservationPresentation.kt` collapses six labels/explanations/colors to two,
   using the contract's verbatim copy.
6. *Screens* — `FrameReservationListScreen` loses its terminal-status partition and status chip;
   `FrameReservationDetailScreen` loses the tracker, the status pill becomes a held chip, the hold
   notice keys off `isHeld`, and successful cancellation **pops to the list** instead of rendering a
   cancelled banner (the record no longer exists).
7. *Create flow* — `activeReservationStatuses` and the `MergeOutcome.Blocked` branch disappear;
   `mergeOutcome` now keys off `isHeld` and item count.

**Risk — 204 with a body-typed Retrofit method.** `DELETE /frame-reservations/{id}` returns 204 with
no body. A `suspend fun` declared to return a DTO throws on an empty body. Mitigation: declare
`Response<Unit>` and treat any `isSuccessful` as success.

**Risk — the cancel-success UI contract changes.** Detail previously stayed on-screen showing a
cancelled reservation. Now the entity is gone, so the screen must pop. This needs a one-shot event,
not a state field, or the pop re-fires on recomposition. Mitigation: model it as a terminal
`ReservationDetailUiState.Deleted` that the screen consumes with a `LaunchedEffect` keyed on the
state instance.

**Checkpoint 2:** builds and tests green; `grep -r "ReservationStatus" app/src` returns nothing.

---

## Stage 3 — Reservation item editing

**Depends on Stage 2** (needs `isHeld` and the new repository shape).

**Components**

1. *API* — `POST frame-reservations/{id}/items` returning the reservation;
   `DELETE frame-reservations/{id}/items/{itemId}` returning `Response<ReservationResponse>` so the
   200-vs-204 split is observable.
2. *Repository* — `addItem(reservationId, variantId): Result<FrameReservation>` and
   `removeItem(reservationId, itemId): Result<FrameReservation?>`, where `null` means the last item
   was removed and the reservation is gone (spec D5). Both parse 422 into
   `FrameReservationError.ValidationError` the same way `createReservation` already does.
3. *Detail ViewModel* — single-flight `addItem`/`removeItem` with per-item pending state so two rapid
   taps cannot double-remove; `null` from `removeItem` transitions to the terminal `Deleted` state.
4. *Detail screen* — per-card remove affordance and a **+ Add frame** button, both shown only when
   `!isHeld`; Add is hidden at 5 items. Add navigates to the frames catalog (spec D4).
5. *Create flow* — `mergeIntoExisting` becomes one `addItem` call. **Delete the cancel-then-recreate
   path entirely**, including its apology copy for the "we released your hold but couldn't re-add" case,
   which can no longer happen.

**Risk — the merge rewrite is the highest-value change and the easiest to regress.** Mitigation: an
explicit `verify(exactly = 0)` on the delete path in the merge test, so a reintroduced
cancel-then-recreate fails the suite rather than passing silently.

**Risk — removing an item while the list screen holds stale data.** The list already refreshes on
`ON_RESUME`, so returning from detail re-reads. No extra work; noted so it is not re-solved.

**Checkpoint 3:** builds and tests green; merge issues one add-item call and zero deletes.

---

## Stage 4 — Payload alignment

Independent of 1–3; small and mechanical.

1. *Remove `revision_number`* from `AppointmentV1Dtos.VisitRatingDto`, both `OpticalOrderDtos`
   occurrences, `AppointmentV1.VisitRating`, both `OpticalOrder` rating models, the two repository
   mappings, `OpticalOrderDetailViewModel`, `EyewearPresentation.isEdited`, and the "Edited" badge in
   `AppointmentDetailScreen` and `OpticalOrderDetailScreen`. Update `OpticalOrderDtosTest`,
   `EyewearPresentationTest`, `FrameRatingViewModelTest`.
2. *Add `rejection_reason`* — DTO (nullable), `AppointmentRequest.rejectionReason`, repository
   mapping, and a rejection row on `AppointmentRequestDetailScreen` rendered only when the status is
   `REJECTED` and the reason is non-blank.

**Risk — `isEdited` deletion cascades into tests that assert it.** Mitigation: delete the assertion
block with the function; do not leave a test asserting a constant.

**Checkpoint 4:** builds and tests green; `grep -r "revision_number\|revisionNumber" app/src` returns
nothing.

---

## Stage 5 — Frame-ratings feature flag

Independent of 1–4.

1. Add `presentation/common/FeatureFlags.kt` with a single documented
   `const val FRAME_RATINGS_ENABLED = false`, stating that re-enablement is expected soon and that
   flipping the boolean is the whole restore procedure.
2. Gate the order-item rating action in `OpticalOrderDetailScreen` (the action *and* the existing
   rating readout — showing a rating the patient cannot revise is worse than showing none).
3. Gate `RatingBadge` in `FrameCard` and `RatingBadgeDetail` in `FrameDetailScreen`, including the
   "No ratings yet" branch.
4. Leave DTOs, repositories, `FrameRatingViewModel`, `FrameRatingDialog`, and their tests untouched
   and compiling.

**Risk — a flagged-off surface rots.** Reading `FeatureFlags.FRAME_RATINGS_ENABLED` *inside* the
composables would make the enabled path unrenderable in tests, so the code we intend to ship soon
would sit untested for the whole time it is hidden. Mitigation: the flag is a **composable parameter
defaulted to the global**, so production passes nothing and tests render both states:

```kotlin
fun FrameCard(
    frame: Frame,
    ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED,
)
```

`FrameRatingViewModelTest` also stays running — the flag hides UI, not logic.

**Checkpoint 5:** builds and tests green; no rating affordance reachable in the debug build.

---

## Stage 6 — Route governance and living documentation

**Must run last** — it asserts the end state of every prior stage.

1. `ApprovedApiRoutes`: remove `GET /quotations`, `GET /quotations/{quotation}`, and
   `POST /frame-reservations/{reservation}/cancel`; add `DELETE /frame-reservations/{reservation}`,
   `POST /frame-reservations/{reservation}/items`, and
   `DELETE /frame-reservations/{reservation}/items/{item}`. Active-link stays at **17**
   (−3 +3), total **54**.
2. Empty `legacyAliasRoutes` and move `POST /job-order-items/{item}/rating` into `rejectedRoutes`;
   add the two quotation routes and the old cancel route to `rejectedRoutes` so a regression is caught
   by the discovery test rather than by a 404 in production.
3. `POST /optical-order-items/{item}/rating` **stays** in the active-link set — it is an approved route
   that the UI simply does not reach while Stage 5's flag is off.
4. Update the doc header and counts in `ApiRouteAllowlistTest`/`ApprovedApiRoutes` from "V17 / 55" to
   "V18 / 54".
5. `CONTEXT.md`: rewrite §Frame Reservations, replace §My Eyewear with §My Orders, add the flagged
   frame-ratings note, delete the "Known backend bug" paragraph (fixed server-side 2026-08-13), update
   §Route Governance to 54, and add v18 to §Active Specs.

**Checkpoint 6 (final):** `assembleDebug`, `testDebugUnitTest`, and `ktlintCheck` all green; every
success criterion in the spec verified.

---

## Sequencing summary

| Stage | Depends on | Can run in parallel with | Rough size |
|---|---|---|---|
| 0 Baseline | — | — | verify only |
| 1 Quotation removal + rename | 0 | 4, 5 | large (deletions) |
| 2 Reservation two-state | 0 | 4, 5 | large |
| 3 Reservation item editing | 2 | 4, 5 | medium |
| 4 Payload alignment | 0 | 1, 2, 3, 5 | small |
| 5 Frame-ratings flag | 0 | 1, 2, 3, 4 | small |
| 6 Governance + docs | 1–5 | — | small |

Execution is serial (1 → 2 → 3 → 4 → 5 → 6); the parallel column records what *could* be split across
sessions if this work is divided.

---

## Consolidated risk register

| # | Risk | Severity | Mitigation |
|---|---|---|---|
| R1 | Pre-existing uncommitted work makes failures ambiguous | High | Stage 0 baseline before any edit |
| R2 | 204 responses crash body-typed Retrofit methods | High | `Response<Unit>` / `Response<T>` + `isSuccessful` |
| R3 | Merge rewrite silently regresses to cancel-then-recreate | High | `verify(exactly = 0)` on the delete path |
| R4 | Cancel-success pop re-fires on recomposition | Medium | Terminal `Deleted` state consumed by `LaunchedEffect` |
| R5 | Orphaned `MyEyewear`/`Estimate` strings in androidTest | Medium | Grep `androidTest` as part of Checkpoint 1 |
| R6 | Flagged-off rating code rots | Medium | Keep its unit tests running; assert absence in Compose |
| R7 | Allowlist and contract silently diverge | Medium | Stage 6 asserts the count explicitly, and runs last |
| R8 | Deleting `isEdited` leaves tests asserting a constant | Low | Delete assertions with the function |

---

## What this plan does not do

- No Room migration (rating columns stay — spec Assumption 7).
- No My Orders redesign beyond removing the Estimates section and renaming.
- No in-sheet frame picker (spec D4 routes through the catalog).
- No change to visit feedback beyond dropping `revision_number`.
- No client handling for staff-only backend changes (`voided` states, `inbox_archived_at`).
