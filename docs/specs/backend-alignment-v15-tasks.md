# Implementation Plan: Backend Alignment v15

> **Status:** Phase 4 (IMPLEMENT) — complete. All 16 tasks implemented and verified.
>
> **Spec:** `docs/specs/backend-alignment-v15-spec.md` (approved 2026-08-07)
> **Plan:** `docs/specs/backend-alignment-v15-plan.md` (approved 2026-08-07)
>
> **Supersedes the plan document's stage numbering.** Rebuilt using
> `skills/planning-and-task-breakdown`, which changed two structural things:
> tasks are now **vertically sliced** (each delivers a working path, not a layer),
> and the **Room migration moved early** to fail fast rather than late to contain
> blast radius. 16 tasks, all S or M.

---

## Overview

Reconcile the Android client with the 2026-08-07 backend updates: build visit
feedback and frame rating aggregates (new capabilities), prove five optical-order
fields that shipped but were never exercised, and fix a live deserialization crash
plus stale route governance.

---

## Architecture Decisions

- **Vertical slices over layer-by-layer.** Each task carries a change from
  transport through to something observable, so every checkpoint has working
  functionality rather than half a stack.
- **Riskiest task first within its slice.** The Room 3→4 migration is Task 3, not
  Task 12. If the migration harness doesn't exist or the schema can't be evolved
  additively, that surfaces before any code depends on it.
- **The crash fix leads.** Task 1 is the smallest change in the plan and stops an
  exception on every frame-rating submit — nothing should queue ahead of it.
- **Route governance precedes the API annotation.** Adding `rateAppointment()`
  fails `ApiRouteAllowlistTest` until the route is approved, so Task 2 gates
  Phase 3.
- **Display what the server sends.** No client-side derivation, label parsing, or
  aggregate correction — the drift this spec closes was caused by the client
  quietly compensating for the server.

---

## Task List

### Phase 1: Foundation

---

## Task 1: Fix the live frame-rating deserialization crash

**Description:** `FrameRatingResource` returns `"item_id": null` but
`RatingResultDto.itemId` is a non-nullable `Int`, so kotlinx throws on every
rating submit. Make it nullable end to end and add the sanitized resource's
`product_variant_id`.

**Acceptance criteria:**

- [ ] A regression test decodes `{"id":1,"item_id":null,…}` without throwing —
      written first, red before the fix
- [ ] `RatingResultDto.itemId: Int?` and `RatingResult.itemId: Int?`, with
      `productVariantId: Int? = null` added to the DTO
- [ ] No UI reads `itemId`, so no presentation change follows

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*OpticalOrderDtoTest*"`
- [ ] `./gradlew assembleDebug`
- [ ] Manual: submit a frame rating on a dispensed order — succeeds

**Dependencies:** None

**Files likely touched:**

- `data/remote/dto/OpticalOrderDtos.kt`
- `domain/model/OpticalOrder.kt`
- `data/repository/OpticalOrderRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/OpticalOrderDtoTest.kt` (new)

**Estimated scope:** S

---

## Task 2: Route governance — 53 routes and the legacy alias

**Description:** The allowlist asserts 51 routes and rejects
`job-order-items/{id}/rating`, which the backend has deliberately retained as a
compatibility alias. Correct the counts, pre-approve the visit-rating route so
Phase 3 isn't blocked, and introduce a third category for server-side-only routes.

**Acceptance criteria:**

- [ ] Counts read 8 public / 24 account-only / 21 active-link = 53
- [ ] `POST /api/v1/appointments/{appointment}/rating` in `activeLinkRoutes`
- [ ] New `legacyAliasRoutes` holds the `job-order-items` path, removed from
      `rejectedRoutes` and absent from `allApproved`; the test asserts no
      production Retrofit annotation references it

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest*"`

**Dependencies:** None

**Files likely touched:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** S

---

### ✅ Checkpoint: Foundation (Tasks 1–2)

- [ ] All tests pass, build clean
- [ ] Frame-rating submit no longer throws
- [ ] Allowlist green at 53, with the visit-rating route pre-approved ahead of its
      annotation (intended)

---

### Phase 2: Frame rating aggregates — highest risk first

---

## Task 3: Room schema 3 → 4 with a preserving migration

**Description:** The riskiest change in the plan, taken first. Add nullable
aggregate columns to the frame cache and evolve the database additively. A wrong
migration silently wipes every user's offline catalog, so this ships with a test
before anything depends on it.

**Acceptance criteria:**

- [ ] `FrameEntity` gains `averageRating: Double?` and `ratingCount: Int = 0`
- [ ] `EyecareDatabase` version 3 → 4 with an explicit `Migration(3, 4)` running
      two `ALTER TABLE frames ADD COLUMN` statements
- [ ] `fallbackToDestructiveMigration()` is absent — confirm it was never present
      and is not introduced

**Verification:**

- [ ] Migration test: seed a v3 `frames` row, migrate, assert the row survives with
      `null` / `0` aggregates
- [ ] `./gradlew assembleDebug`
- [ ] Manual: launch over an existing install without clearing app data

**Dependencies:** None

**Files likely touched:**

- `data/local/entity/FrameEntity.kt`
- `data/local/EyecareDatabase.kt`
- `di/DatabaseModule.kt` (only if the migration registers there)
- `app/src/androidTest/java/com/eyecare/app/data/local/MigrationTest.kt` (new)

**Estimated scope:** M

> ⚠️ **Known unknown:** no migration-test harness has been confirmed to exist. If
> none does, setting one up is part of this task — the reason it runs first. It is
> also the only task requiring a device or emulator.

---

## Task 4: Frame aggregates reach the domain model, online and offline

**Description:** Carry `average_rating` / `rating_count` from the API response
through to the domain model and the cache, in both directions. Completes the data
path so the next task only has to render.

**Acceptance criteria:**

- [ ] `FrameDto` gains `averageRating: Double?` and `ratingCount: Int = 0`;
      `Frame` mirrors them — **not** `MoneyValueSerializer`, this is a plain float
- [ ] `average_rating: null` stays `null` at every layer and never becomes `0.0`
- [ ] Round trip network → cache → domain preserves both values

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*FrameDtoTest*"`
- [ ] `./gradlew testDebugUnitTest --tests "*FrameRepositoryTest*"`

**Dependencies:** Task 3

**Files likely touched:**

- `data/remote/dto/FrameDtos.kt`
- `domain/model/Frame.kt`
- `data/repository/FrameRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/FrameDtoTest.kt` (new)
- `app/src/test/java/com/eyecare/app/data/repository/FrameRepositoryTest.kt`

**Estimated scope:** M

---

## Task 5: Ratings become visible in the catalog

**Description:** A shared badge rendering the average and count, wired into the
frame list card and detail. Completes the vertical slice: a rating submitted by a
previous buyer now reaches the next shopper — the gap that made frame ratings
pointless.

**Acceptance criteria:**

- [ ] `RatingBadge` renders `★ 4.5 (12)` for populated values and **nothing** when
      `averageRating == null` — no placeholder in the list
- [ ] Accessibility label reads the full phrase ("rated 4.5 out of 5 from 12
      ratings"), not the glyph
- [ ] Frame detail additionally shows a quiet "No ratings yet" when unrated

**Verification:**

- [ ] `./gradlew testDebugUnitTest`
- [ ] Manual: rated and unrated frames in both list and detail
- [ ] Manual: TalkBack spot-check of one badge

**Dependencies:** Task 4

**Files likely touched:**

- `presentation/frames/components/RatingBadge.kt` (new)
- `presentation/frames/FrameListScreen.kt`
- `presentation/frames/FrameDetailScreen.kt`

**Estimated scope:** S

---

### ✅ Checkpoint: Frame ratings (Tasks 3–5)

- [ ] Migration preserves pre-existing cached rows
- [ ] `null` average never becomes `0.0` at any layer
- [ ] Catalog shows ratings online and offline; unrated frames show no badge
- [ ] Review with human before proceeding

---

### Phase 3: Visit feedback

---

## Task 6: Confirm `is_rateable` semantics after a rating exists

**Description:** Resolve spec Open Question 1 / plan R8 before building the UI
that depends on it. Both contract sections imply `is_rateable` stays `true` after
rating so revisions can be submitted; a review note described it as true when
"fulfilled **and unrated**". If the latter is right, the revise path is
unreachable no matter how the client is written.

**Acceptance criteria:**

- [ ] Definitive answer from `AppointmentResource.php` or a live response on a
      rated fulfilled appointment
- [ ] Finding recorded inline in this task
- [ ] If it flips to `false` once rated: revise gates on `rating != null` instead
      of `isRateable`, and the divergence is reported

**Verification:**

- [ ] Answer written down before Task 9 begins

**Dependencies:** None

**Files likely touched:** None (investigation)

**Estimated scope:** XS

---

## Task 7: Appointment rating data path

**Description:** Teach the appointment transport and domain layers about ratings —
the fields on the way in, the endpoint on the way out.

**Acceptance criteria:**

- [ ] `VisitRatingDto(rating, comment?, revisionNumber?, createdAt?)`;
      `AppointmentDto` gains `is_rateable` (default `false`) and `rating`
      (default `null`), decoding today's payload **and** a full one
- [ ] Domain `VisitRating`; `AppointmentV1.isRateable` + `.rating`, mapped at the
      repository boundary
- [ ] `rateAppointment(id, rating, comment): Result<VisitRating>` on the service
      and repository; 404 and 422 surface as distinguishable failures

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*AppointmentV1DtoTest*"`
- [ ] `./gradlew testDebugUnitTest --tests "*AppointmentV1RepositoryTest*"`
- [ ] `./gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest*"` still green

**Dependencies:** Task 2

**Files likely touched:**

- `data/remote/dto/AppointmentV1Dtos.kt`
- `data/remote/api/AppointmentV1ApiService.kt`
- `domain/model/AppointmentV1.kt`
- `domain/repository/AppointmentV1Repository.kt`
- `data/repository/AppointmentV1RepositoryImpl.kt`

**Estimated scope:** M

---

## Task 8: Feedback dialog component

**Description:** The stateless star-and-comment dialog both entry points share.
Built separately because it carries no business logic and is the only piece two
later tasks both depend on.

**Acceptance criteria:**

- [ ] 1–5 tappable stars; optional comment with a live `n/1000` counter
- [ ] Submit disabled until a star is chosen; submit and dismiss are callbacks
- [ ] Themed to match `AppConfirmationDialog` — rounded surface, pill buttons

**Verification:**

- [ ] `./gradlew assembleDebug`
- [ ] Manual: Compose preview at both empty and filled states

**Dependencies:** None

**Files likely touched:**

- `presentation/appointments/components/VisitFeedbackDialog.kt` (new)

**Estimated scope:** S

---

## Task 9: Patient can rate and revise a visit from appointment detail

**Description:** First working vertical path — a patient with a fulfilled
appointment leaves a rating and edits it later. Gating follows Task 6's finding.

**Acceptance criteria:**

- [ ] `submitRating` rejects rating `<1`/`>5` and comments `>1000` **before** the
      repository is called
- [ ] Success merges locally — `current.appointment.copy(rating = returned)` — and
      does **not** re-invoke `load()`; the endpoint returns a rating, not an
      appointment
- [ ] Unrated → **Rate your visit**; rated → stars, comment, **Update rating**, and
      **Edited** when `revisionNumber > 1`; nothing renders when not rateable
- [ ] 404 → "This appointment is no longer available."; 422 → "This visit can't be
      rated yet."

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*AppointmentDetailViewModelTest*"`
- [ ] Manual: rate a fulfilled appointment, reopen, revise it

**Dependencies:** Tasks 6, 7, 8

**Files likely touched:**

- `presentation/appointments/AppointmentDetailViewModel.kt`
- `presentation/appointments/AppointmentDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModelTest.kt`

**Estimated scope:** M

---

## Task 10: Rate-this-visit chip on the appointment list

**Description:** The low-friction prompt. A patient shouldn't have to open an
appointment to discover they can rate it, but the ask stays ignorable rather than
modal.

**Acceptance criteria:**

- [ ] Chip renders on confirmed-appointment rows only when
      `isRateable && rating == null`, opening the Task 8 dialog
- [ ] Absent on appointment-_request_ rows, rated appointments, and non-rateable
      ones; show/hide logic is a pure unit-tested function
- [ ] No persisted dismissal (spec decision 8)

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*AppointmentListViewModelTest*"`
- [ ] Manual: mixed list of requests, upcoming, and fulfilled appointments

**Dependencies:** Task 9

**Files likely touched:**

- `presentation/appointments/AppointmentListScreen.kt`
- `presentation/appointments/AppointmentListViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentListViewModelTest.kt`

**Estimated scope:** M

---

### ✅ Checkpoint: Visit feedback (Tasks 6–10)

- [ ] End-to-end: fulfilled appointment → chip → dialog → rating persists → revise
- [ ] With `is_rateable: false`, no affordance is reachable anywhere and no request
      is issued
- [ ] Review with human before proceeding

---

### Phase 4: Prove the previously-dead optical-order fields

> These fields shipped backend-side but have never executed against real data and
> nothing asserts them — the exact shape of the original drift, and why the My
> Eyewear rating action sat dead through a release. **If any task here fails, the
> backend has not shipped what the contract claims: stop and report, do not add a
> client-side fallback.**

---

## Task 11: Optical-order item fields verified live

**Description:** Assert that `is_rateable`, `rating`, and `product_variant_id`
survive to the domain model, so the rating action on order detail is provably
reachable.

**Acceptance criteria:**

- [ ] An item with `is_rateable: true`, a populated `rating`, and a non-null
      `product_variant_id` reaches `OpticalOrderItem` intact
- [ ] The same item with those fields absent still decodes to safe defaults

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*OpticalOrderDtoTest*"`
- [ ] Manual: a dispensed order shows its rating action

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/test/java/com/eyecare/app/data/remote/dto/OpticalOrderDtoTest.kt`

**Estimated scope:** S

---

## Task 12: Payment summary verified live

**Description:** The backend now sends machine-readable payment statuses instead
of display labels. Confirm the mapping works and that the old label form still
falls through to `UNKNOWN` rather than being quietly re-accepted.

**Acceptance criteria:**

- [ ] `unpaid`, `partially_paid`, `paid`, `voided` all map correctly
- [ ] `"Partially Paid"` and other unrecognized values → `UNKNOWN`; no
      display-label parsing is added
- [ ] `is_overdue: true` survives; a missing `payment_summary` decodes to `null`

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*OpticalOrderRepositoryTest*"`
- [ ] Manual: an order with a balance shows "Balance due", not "Payment status
      unavailable"

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/test/java/com/eyecare/app/data/repository/OpticalOrderRepositoryTest.kt` (new)

**Estimated scope:** S

---

## Task 13: Edited indicator on frame ratings

**Description:** Mirror the visit-rating treatment onto optical-order item
ratings, so a revised rating reads as a receipt rather than silently replacing
itself.

**Acceptance criteria:**

- [ ] `RatingSummaryDto` and domain `RatingSummary` carry `revisionNumber: Int?`;
      payloads without it still decode
- [ ] A pure helper returns `true` only when `revisionNumber > 1` (`null` and `1`
      → false), unit-tested
- [ ] Order detail renders **Edited**; the raw revision number is never shown

**Verification:**

- [ ] `./gradlew testDebugUnitTest --tests "*EyewearPresentation*"`
- [ ] Manual: revise a frame rating, confirm the marker appears

**Dependencies:** Task 11

**Files likely touched:**

- `data/remote/dto/OpticalOrderDtos.kt`
- `domain/model/OpticalOrder.kt`
- `data/repository/OpticalOrderRepositoryImpl.kt`
- `presentation/eyewear/EyewearPresentation.kt`
- `presentation/eyewear/OpticalOrderDetailScreen.kt`

**Estimated scope:** M

---

### ✅ Checkpoint: Optical orders (Tasks 11–13)

- [ ] All five spec §C fields have passing assertions
- [ ] My Eyewear rating action and Overdue badge demonstrably reachable

---

### Phase 5: Cleanup

---

## Task 14: Reservation rejection reads clearly

**Description:** An appointment now gets exactly one reservation _ever_, not one
active reservation. The client can't detect that in advance, so the server's
rejection needs to be legible instead of generic.

**Acceptance criteria:**

- [ ] A rejected `POST /frame-reservations` surfaces the server's message
- [ ] Copy states one reservation per appointment
- [ ] `isReservationEligible` is **unchanged** — filtering by prior reservations
      stays out of scope

**Verification:**

- [ ] `./gradlew testDebugUnitTest`
- [ ] Manual: reserve against an appointment whose reservation was already
      cancelled

**Dependencies:** None

**Files likely touched:**

- `presentation/reservations/CreateFrameReservationViewModel.kt`
- `presentation/reservations/CreateFrameReservationScreen.kt`
- `presentation/reservations/FrameReservationListScreen.kt`

**Estimated scope:** S

---

## Task 15: `CONTEXT.md` reconciliation

**Description:** Our own documentation has drifted — duplicated sections,
contradictory endpoint lists, and three different route counts. Reconcile it to
the finished state.

**Acceptance criteria:**

- [ ] Exactly one `Architecture`, one `Active Specs`, one `Boundaries` section
- [ ] Route count 53 (not 34, not 51); the two contradictory endpoint lists merged
- [ ] Retired-feature sections removed (`/eyewear`, `/job-orders`,
      `/billing-records`, order requests, accessory ordering, catalog tabs)
- [ ] Visit feedback, frame aggregates, the hidden-ratings caveat, and v15 recorded

**Verification:**

- [ ] `grep -c "^## Architecture" CONTEXT.md` = 1, same for the other two headings
- [ ] Manual read-through

**Dependencies:** Tasks 5, 10, 13

**Files likely touched:**

- `CONTEXT.md`

**Estimated scope:** S

---

## Task 16: Branding and final verification

**Description:** Adopt the confirmed "EyeCare" spelling and verify the whole
release against the spec's success criteria.

**Acceptance criteria:**

- [ ] App name string → "EyeCare"; `CONTEXT.md` branding table matches
- [ ] All 15 spec success criteria confirmed

**Verification:**

- [ ] `./gradlew assembleDebug && ./gradlew testDebugUnitTest && ./gradlew ktlintCheck`
- [ ] Manual: launcher label reads "EyeCare"

**Dependencies:** Task 15

**Files likely touched:**

- `app/src/main/res/values/strings.xml`
- `CONTEXT.md`

**Estimated scope:** XS

---

### ✅ Checkpoint: Complete

- [ ] All acceptance criteria met
- [ ] `assembleDebug`, `testDebugUnitTest`, `ktlintCheck` all pass
- [ ] No file outside spec § Project Structure modified
- [ ] Ready for review

---

## Risks and Mitigations

| Risk                                                                                         | Impact | Mitigation                                                                                                                          |
| -------------------------------------------------------------------------------------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| Room migration wipes the offline frame cache                                                 | High   | Task 3 runs first with an explicit additive migration and a seed-migrate-assert test; `fallbackToDestructiveMigration` stays absent |
| No migration-test harness exists                                                             | Medium | Folded into Task 3's scope; surfaces on day one rather than at the end                                                              |
| `is_rateable` flips to `false` once rated, making revision unreachable                       | Medium | Task 6 settles it before Task 9 builds on it; fallback is to gate revise on `rating != null`                                        |
| Rating endpoint returns a rating, not an appointment — copying the reschedule pattern breaks | Low    | Called out in Task 9's acceptance criteria: merge via `copy(rating = returned)`                                                     |
| `average_rating` collapsing to `0.0`, libelling every unrated frame                          | High   | Nullable end to end; explicit null-preservation assertion in Task 4                                                                 |
| Hidden-ratings aggregate skews averages upward                                               | Medium | Backend fix landing in the same window (spec decision 7); no client-side correction                                                 |
| Task 2 approves a route no annotation uses until Task 7                                      | None   | Intended — the allowlist governs what _may_ be called                                                                               |

---

## Parallelization Opportunities

- **Safe to parallelize:** Phase 2 (frame ratings) and Phase 3 (visit feedback)
  share no files after Task 2. Phase 4 is independent of both after Task 1.
  Task 14 touches one screen nothing else edits.
- **Must be sequential:** Task 3 before Task 4 (schema before mapping); Task 2
  before Task 7 (route approval before annotation); Task 6 before Task 9;
  Tasks 5/10/13 before Task 15 (docs describe the finished state).
- **Needs coordination:** none — no two phases share an API contract.

For a single implementer, run 1 → 16 in order.

---

## Open Questions

- **Task 3's migration harness** — does one exist? Unknown until the task starts.
  It is the only task requiring a device or emulator, and the likeliest to slip.
- **Task 6's finding** may change Task 9's gating. Both outcomes are handled; the
  answer just needs to arrive first.

---

## Verification (skill gate)

- [x] Every task has acceptance criteria
- [x] Every task has a verification step
- [x] Task dependencies are identified and ordered correctly
- [x] No task touches more than ~5 files
- [x] Checkpoints exist between major phases
- [x] **The human has reviewed and approved the plan** ← gate to Phase 4 (IMPLEMENT)
