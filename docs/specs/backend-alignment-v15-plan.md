# Plan: Backend Alignment v15

> **Status:** Phase 4 (IMPLEMENT) — complete.
>
> **Spec:** `docs/specs/backend-alignment-v15-spec.md` (approved 2026-08-07)

---

## 1. Components and Dependencies

Six workstreams. Only two hard ordering constraints exist; everything else is
sequenced for risk, not necessity.

```
W4 route governance (test-only)
  └─ MUST precede ──→ W2 visit feedback
                       (adding rateAppointment() to the Retrofit interface
                        fails ApiRouteAllowlistTest until the route is approved)

W3 contract corrections ──→ independent
W1 frame aggregates     ──→ independent  (Room migration = highest blast radius)
W5 reservation copy     ──→ independent
W6 docs + branding      ──→ independent  (do last; describes the finished state)
```

| ID | Workstream | Layers touched | Risk |
|---|---|---|---|
| W3 | Contract corrections (nullable `item_id`, `product_variant_id`, live-field verification, Edited indicator) | dto → domain → test | Low |
| W4 | Route governance (53 routes, `legacyAliasRoutes`) | test only | Low |
| W2 | Visit feedback | dto → domain → api → repo → vm → ui ×2 | Medium |
| W1 | Frame rating aggregates | dto → domain → **Room schema** → repo → ui | **High** (migration) |
| W5 | Reservation error surfacing + copy | ui | Low |
| W6 | `CONTEXT.md` + branding string | docs + res | Low |

---

## 2. Implementation Order

Ordered so that the live crash is fixed first, the enabling test change lands
before the code that depends on it, and the riskiest change is isolated in its own
stage with a clean checkpoint on either side.

### Stage 1 — Stop the bleeding + unblock W2

Fixes the shipped crash and pre-approves the route that Stage 3 will add.

1. `RatingResultDto.itemId` → `Int?`; add `@SerialName("product_variant_id") productVariantId: Int?`.
2. Propagate nullability to `RatingResult.itemId` in the domain model; update
   `OpticalOrderRepositoryImpl` passthrough.
3. Regression test: `RatingResultDto` decodes `{"id":1,"item_id":null,…}`.
4. `ApprovedApiRoutes.kt`: counts 51 → 53; add
   `POST /api/v1/appointments/{appointment}/rating` to `activeLinkRoutes`; add the
   `legacyAliasRoutes` category holding `job-order-items/{item}/rating` and remove
   it from `rejectedRoutes`; extend `ApiRouteAllowlistTest` to assert alias routes
   are neither rejected nor present in production annotations.

**Checkpoint 1:** `testDebugUnitTest` + `assembleDebug` pass. Frame-rating submit
no longer throws. Allowlist asserts 53 and stays green with the not-yet-added
appointment-rating route pre-approved.

### Stage 2 — Prove the previously-dead optical-order fields

No production change expected beyond the Edited indicator. This stage exists
because five fields have driven UI that could never fire, and nothing asserted
them — the exact shape of the original drift.

1. DTO + repository tests: `is_rateable: true`, populated `rating`,
   `product_variant_id`, `is_overdue: true`, and all four
   `payment_summary.status` enum values reaching the domain model intact;
   unrecognized status → `UNKNOWN`.
2. Add `revision_number` to `OpticalOrderDtos.RatingSummaryDto` and
   `RatingSummary`.
3. Render the **Edited** indicator on optical-order detail when
   `revisionNumber > 1`.

**Checkpoint 2:** every field in the spec's §C table has a passing assertion. If
any fails, the backend has not shipped what it claims — stop and report rather
than adding a client workaround.

### Stage 3 — Visit feedback, data layer

1. `VisitRatingDto`; add `is_rateable` (default `false`) and `rating` (default
   `null`) to `AppointmentDto`.
2. Domain: `VisitRating` model; `isRateable` + `rating` on `AppointmentV1`.
3. `AppointmentV1ApiService.rateAppointment(id, body)`.
4. `AppointmentV1Repository.rateAppointment(...): Result<VisitRating>` + impl.

**Checkpoint 3:** `AppointmentDto` decodes today's payload (fields absent) *and* a
full payload. Repository test covers success and the 404/422 error paths.

### Stage 4 — Visit feedback, presentation

1. `VisitFeedbackDialog` — 1–5 stars, optional comment with live counter, themed
   to match `AppConfirmationDialog`.
2. `AppointmentDetailViewModel`: `submitRating(rating, comment)` with client-side
   validation ahead of the network call.
3. `AppointmentDetailScreen`: rate/update action + rendered rating + Edited
   indicator, all gated on `isRateable`.
4. `AppointmentListScreen`: **Rate this visit** chip where
   `isRateable && rating == null`, on confirmed-appointment rows only.

**Checkpoint 4:** gating, validation, success-without-refetch, and distinct
404/422 messages all covered. With `is_rateable: false` no affordance is reachable
and no request is issued.

### Stage 5 — Frame rating aggregates (isolated: schema change)

1. `FrameDto`: `average_rating: Double?`, `rating_count: Int = 0`. **Do not** use
   `MoneyValueSerializer` — this is a plain float, not a monetary string.
2. `Frame` domain: `averageRating: Double?`, `ratingCount: Int`.
3. Room: two nullable columns on `FrameEntity`; `EyecareDatabase` version 3 → 4;
   explicit `Migration(3, 4)` with two `ALTER TABLE … ADD COLUMN` statements.
   **No `fallbackToDestructiveMigration()`** — that would wipe the offline cache.
4. `FrameRepositoryImpl`: map aggregates in `toDomain()`/`toEntity()` both ways.
5. `RatingBadge` composable; wire into the frame list card and frame detail.

**Checkpoint 5:** `average_rating: null` survives as `null` (never `0.0`);
migration test confirms pre-existing cached rows survive with `null` aggregates;
badge renders nothing for null and `★ 4.5 (12)` with the full accessibility phrase
for populated values.

### Stage 6 — Cleanup

1. Reservation: surface the server's rejection message instead of a generic
   failure; copy says one reservation per appointment.
2. `CONTEXT.md`: dedupe the doubled `Architecture` / `Active Specs` / `Boundaries`
   sections; route count → 53; remove the retired-feature sections; "EyeCare";
   record the hidden-ratings aggregate bug; add v15 to Active Specs.
3. `strings.xml`: app name → "EyeCare".

**Checkpoint 6:** full `assembleDebug` + `testDebugUnitTest` + `ktlintCheck`. All
15 success criteria in the spec verified.

---

## 3. Risk Register

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | **Room migration wipes the frame cache.** A missing or wrong `Migration(3,4)` falls back to destructive recreation. | Low | High | Explicit `ALTER TABLE ADD COLUMN` migration; assert `fallbackToDestructiveMigration` is absent; migration test seeding v3 rows and reading them back after upgrade. Isolated in its own stage. |
| R2 | ~~`is_rateable` may not be on the list payload.~~ **CLOSED 2026-08-07.** Confirmed present on `AppointmentResource` (`app/Http/Resources/AppointmentResource.php:22`), which `GET /appointments` uses. The list chip is buildable as specced. | — | — | No action. |
| R8 | **`is_rateable` may flip to `false` once rated,** making the revise path unreachable. Same source note describes it as true when "fulfilled and unrated", while both contract sections imply it stays true so revisions can be submitted. | Medium | Medium | Verify before Stage 4 (spec Open Question 1). If it does gate on unrated, gate the client's revise action on `rating != null` rather than `isRateable`. Chip unaffected — it already requires `rating == null`. |
| R3 | **The rating response is not an appointment.** Reschedule returns the full updated `Appointment`; `POST /appointments/{id}/rating` returns only the rating object. Blindly copying the reschedule pattern would fail to compile or, worse, swap the wrong object. | High | Low | The ViewModel merges locally: `current.appointment.copy(rating = returned)`. Still no refetch. Called out here because the spec says "follow the reschedule pattern" and the analogy is inexact. |
| R4 | **Stage 1's allowlist edit and Stage 3's API change land apart.** Between them, the allowlist approves a route no annotation uses. | High | None | Harmless and intentional — the allowlist governs what *may* be called. Named so a reviewer doesn't flag it as dead config. |
| R5 | **`average_rating` collapsing to `0.0`.** A non-null default or a lossy `Double` mapping would show every unrated frame as zero stars. | Medium | High | Nullable end to end; explicit test that `null` stays `null` through DTO → domain → entity → domain. |
| R6 | **Hidden-ratings aggregate skews averages upward.** Directional, not noise — hiding correlates with low stars, so moderation can only ever raise a product's displayed average. Material at single-digit ratings per product. | Medium | Medium | Being fixed backend-side in the same window (spec decision 7). Android displays what it receives regardless — no client-side correction, forbidden by the spec's Never list. If the fix slips past this work, `CONTEXT.md` carries the caveat instead. |
| R7 | **Compose UI tests for the badge/chip may need `androidTest`,** which is slower and not part of `testDebugUnitTest`. | Medium | Low | Keep badge/chip *logic* (when to show, what text) in pure functions unit-tested under `test/`; reserve `androidTest` for rendering only, mirroring `MessageBubbleTest`. |

---

## 4. Parallel vs Sequential

**Sequential (hard):** Stage 1 → Stage 3 (route approval before the annotation).

**Sequential (chosen):** Stage 1 first — it fixes a live crash and is the cheapest
change in the plan. Stage 5 late and alone — the schema change is the only item
that can damage existing installs, so it gets clean checkpoints on both sides.
Stage 6 last — documentation should describe the finished state.

**Parallelizable if more than one person picks this up:**

- **Stage 2** (optical-order verification) is fully independent of everything after
  Stage 1 — pure test work plus one small indicator.
- **Stage 5** (frame aggregates) shares no file with Stages 3–4. The only contact
  point is `CONTEXT.md` in Stage 6.
- **Stage 6.1** (reservation copy) touches one screen nothing else edits.

For a single implementer, run them in the stated order — the sequencing costs
nothing and keeps each checkpoint meaningful.

---

## 5. Verification Checkpoints

Each checkpoint must pass `./gradlew assembleDebug` and
`./gradlew testDebugUnitTest` before the next stage begins. Beyond that:

| CP | Gate |
|---|---|
| 1 | Null `item_id` decodes; allowlist asserts 53 and is green |
| 2 | All five previously-dead optical-order fields proven live end to end |
| 3 | `AppointmentDto` decodes both payload shapes; repo error paths covered |
| 4 | Feedback gating/validation/success/errors covered; nothing reachable when `is_rateable` is false |
| 5 | Migration preserves v3 rows; `null` average never becomes `0.0`; badge a11y phrase correct |
| 6 | `ktlintCheck` passes; all 15 spec success criteria verified |

**Stop-and-report conditions** — do not code around any of these:

- Checkpoint 2 fails → the backend has not shipped what the contract claims.
- The Room migration cannot preserve existing rows → stop; do not fall back to
  destructive migration.

**Verify-then-branch** (not a stop — resolve at the named stage):

- R8 / spec Open Question 1: if `is_rateable` gates on unrated, gate the revise
  action on `rating != null` instead. Report the divergence either way.

---

## 6. Review Checklist (skill gate)

- [x] Major components and dependencies identified
- [x] Implementation order determined, with rationale
- [x] Risks noted with mitigations
- [x] Parallel vs sequential work identified
- [x] Verification checkpoints defined between phases
- [x] **Human has reviewed and approved** ← gate to Phase 3 (TASKS)
