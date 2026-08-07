# Spec: Backend Alignment v15 — Visit Feedback, Frame Ratings, and Contract Correction

> **Status:** Phase 4 (IMPLEMENT) — complete.
>
> **Source of truth:** `docs/API_CONTRACT.md` and `docs/BACKEND_CONTEXT.md` as of
> the third 2026-08-07 update (drift audit closed; frame rating aggregates shipped;
> route lists corrected to 53).
>
> **Revision history.** Draft 1 was written against an open drift audit and
> proposed a client-side filter bridge plus a six-item blocked register — both
> deleted when the backend shipped the underlying work. Draft 2 treated frame
> rating aggregates as backend-blocked; they have since shipped, so they are now
> in scope. All five of draft 2's open questions are resolved.

---

## Resolved Decisions

Carried in from review so the plan phase doesn't reopen them:

| # | Decision | Rationale |
|---|---|---|
| 1 | **Visit-feedback entry point:** inline chip on the appointment list row where `is_rateable && rating == null`, plus the full surface on detail. Not a blocking modal. | A modal on app-open would convert better but reads as naggy for patients who open the app rarely. The chip is low-friction and easy to ignore, at the cost of conversion — cheap to iterate on later from the Android side without touching the backend. |
| 2 | **`revision_number` is shown**, rendered as an "Edited" indicator when `> 1`. | Only the rating's own author ever sees an individual rating — verified: visit ratings hang off the patient's own appointment, frame ratings off their own order item, and the new catalog fields are aggregates only. At that point it's an "edited" receipt with no privacy exposure. Your reading is right; adopting it. |
| 3 | **Route total is 53** (8 / 24 / 21). | Both documents' appendix lists were corrected upstream and now enumerate 21 active-link routes, matching the count. Verified by direct count. No upstream report needed. |
| 4 | **Frame rating aggregates are in scope.** | `GET /frames` and `GET /frames/{id}` now return `average_rating` / `rating_count`. This closes the write-only gap that made the frame-rating feature pointless. |
| 5 | **Canonical app name is "EyeCare"** (capital C). | Confirmed. Both docs use it; `CONTEXT.md` and the app-name string say "Eyecare" and are now the outliers. |
| 6 | **Frame rating aggregates are cached in Room** — two nullable columns on `FrameEntity`, database version 3 → 4, additive migration. | The cache is a pure offline fallback, so staleness is bounded and low-consequence. Not caching would make rating badges silently vanish offline — looking broken exactly when the user has no connectivity to explain it. |
| 7 | **Ship averages now; the backend hidden-ratings fix lands in the same window.** Not carried as an indefinite caveat. | The distortion is *directional*, not noise — hiding correlates with low stars, so excluding them pushes averages systematically upward. Moderation can only make a product look better. At single-clinic volume (single-digit ratings per product) one hidden 1-star swings a displayed average meaningfully. The fix is diagnosed and one line — drop the `is_hidden` filter from the eager-load in `FrameController`, keep it only where comment text renders. |
| 8 | **No dismissal persistence for the visit-feedback chip.** If it later proves naggy, the shape is a **session-scoped snooze** (reappears next launch), never a permanent dismiss. | `is_rateable` has **no expiry** — verified in `app/Http/Resources/AppointmentResource.php:22`. A permanent dismiss would let a patient silently opt out of ever being asked about that visit, working against the point of the feature. Building persistence for an unobserved problem is speculative complexity; "if it proves naggy" is a trigger to design against later, with real signal. |

---

## Assumptions I'm Making

1. **The drift audit is closed and everything in it shipped.** `?filter=` works on
   both list endpoints; optical-order items expose
   `product_variant_id`/`is_rateable`/`rating`; `payment_summary.is_overdue` is
   present; `payment_summary.status` returns the machine-readable enum; the frame
   rating endpoint returns a sanitized `FrameRatingResource`; ordering is
   `created_at DESC, id DESC`. Android's DTOs were **already written against this
   intended shape**, so most of it needs tests, not code.
2. **Visit feedback is live**, not speculative — ordinary feature work.
3. **The nullable `item_id` fix is still required.** `FrameRatingResource` returns
   `"item_id": null` and our DTO types it as non-nullable `Int`.
4. `ignoreUnknownKeys = true` is set in `NetworkModule.kt` (verified), so backend
   fields we don't declare cannot crash decoding.
5. **The frame rating aggregate has a known backend bug** (below). Android renders
   what the server sends; there is no client-side correction and none should be
   attempted.
6. The frame Room cache is a **pure offline fallback** — written only on an
   unfiltered page-1 success, read only when the network call throws (verified in
   `FrameRepositoryImpl`). Caching aggregates therefore risks little staleness.
7. No new dependency and no navigation-graph restructure is required. A **Room
   schema change is** required and approved (resolved decision 6).
8. Authentication, linking, AR, appointment requests, and messaging are untouched.

---

## Objective

Reconcile the Android client with the 2026-08-07 backend updates:

| Kind of change | Example | How we respond |
|---|---|---|
| **New capability** | `POST /appointments/{id}/rating`; `average_rating` / `rating_count` on frames | Build it |
| **Previously-unbuilt behavior now shipped** | `?filter=`, `is_rateable`, `is_overdue`, machine-readable payment status | Verify — the client already codes for it |
| **Live shape correction** | `item_id` now nullable; route count 51→53 | Fix the client |

**Who the user is:** a linked patient using the EyeCare Android app.

**What success looks like:**

- A patient whose appointment is `fulfilled` sees a light **Rate this visit** chip
  in their appointment list, and can leave or revise a 1–5 star rating with an
  optional comment from appointment detail.
- Frame browsing shows each product's average rating and rating count, so ratings
  collected from previous buyers finally reach the next shopper.
- The My Eyewear rating action, the "Overdue" badge, and the payment-status label
  are confirmed working now that the backend emits the fields they depend on —
  each covered by a test that would have caught the silent failure.
- Submitting a frame rating no longer crashes on the response.
- `ApprovedApiRoutes.kt` describes the real 53-route contract.
- `CONTEXT.md` stops asserting things that are no longer true.

---

## Tech Stack

Unchanged. Relevant here:

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.0 (AGP 9.2.1 built-in — no `kotlin.android` plugin) |
| UI | Jetpack Compose + Material 3 (BOM 2026.05.01) |
| DI | Hilt 2.59.2 |
| Network | Retrofit 2.11 + Kotlinx Serialization 1.8.1 (`ignoreUnknownKeys = true`) |
| Local | Room 2.7.1 — frame cache only, currently schema version 3 |
| Tests | JUnit 5 + MockK + Turbine + coroutines-test |

---

## Commands

```
./gradlew assembleDebug          # Build — required after every change
./gradlew testDebugUnitTest      # Unit tests
./gradlew lintDebug              # Lint
./gradlew ktlintFormat           # Format
./gradlew ktlintCheck            # Format check
```

---

## Project Structure

```
app/src/main/java/com/eyecare/app/
├── data/
│   ├── remote/
│   │   ├── api/AppointmentV1ApiService.kt    → add rateAppointment()
│   │   └── dto/
│   │       ├── AppointmentV1Dtos.kt          → is_rateable, rating, VisitRating DTO
│   │       ├── FrameDtos.kt                  → average_rating, rating_count
│   │       └── OpticalOrderDtos.kt           → itemId nullable + product_variant_id
│   ├── local/
│   │   ├── entity/FrameEntity.kt             → averageRating, ratingCount  ⚠ schema
│   │   └── EyecareDatabase.kt                → version 3 → 4 + migration   ⚠ schema
│   └── repository/
│       ├── AppointmentV1RepositoryImpl.kt    → map rating fields, rateAppointment()
│       ├── FrameRepositoryImpl.kt            → map + cache aggregates
│       └── OpticalOrderRepositoryImpl.kt     → nullable itemId passthrough
├── domain/
│   ├── model/
│   │   ├── AppointmentV1.kt                  → isRateable, rating
│   │   ├── Frame.kt                          → averageRating, ratingCount
│   │   └── OpticalOrder.kt                   → RatingResult.itemId nullable
│   └── repository/AppointmentV1Repository.kt → rateAppointment() signature
└── presentation/
    ├── appointments/
    │   ├── AppointmentListScreen.kt          → "Rate this visit" chip
    │   ├── AppointmentDetailScreen.kt        → feedback card (gated)
    │   ├── AppointmentDetailViewModel.kt     → submit/revise wiring
    │   └── components/VisitFeedbackDialog.kt → NEW
    ├── frames/
    │   ├── FrameListScreen.kt                → rating badge on card
    │   ├── FrameDetailScreen.kt              → rating summary
    │   └── components/RatingBadge.kt         → NEW, shared
    ├── eyewear/OpticalOrderDetailScreen.kt   → "Edited" indicator
    └── reservations/FrameReservationListScreen.kt → one-per-appointment copy

app/src/test/java/com/eyecare/app/
├── data/remote/ApprovedApiRoutes.kt          → 51 → 53, alias reclassified
├── data/remote/dto/OpticalOrderDtoTest.kt    → NEW (null item_id regression)
├── data/repository/FrameRepositoryTest.kt    → NEW/extend (aggregates + cache)
├── data/repository/OpticalOrderRepositoryTest.kt → NEW/extend (live fields)
└── presentation/appointments/VisitFeedbackTest.kt → NEW

app/src/main/res/values/strings.xml           → app name "Eyecare" → "EyeCare"
docs/ ../CONTEXT.md                           → route count, branding, dedupe
```

**Explicitly not touched:** `EstimateListViewModel.kt`,
`OpticalOrderListViewModel.kt`, and the once-proposed `EyewearFilterPolicy.kt` —
the backend honors `?filter=` and both ViewModels already send it.

---

## Code Style

Match the surrounding code. Representative:

```kotlin
// data/remote/dto/FrameDtos.kt — aggregates are nullable; a product with no
// ratings returns average_rating: null, which must not become 0.0.
@Serializable
data class FrameDto(
    // ...existing fields unchanged...
    @SerialName("average_rating") val averageRating: Double? = null,
    @SerialName("rating_count") val ratingCount: Int = 0,
)

// data/remote/dto/AppointmentV1Dtos.kt
@Serializable
data class VisitRatingDto(
    val rating: Int,
    val comment: String? = null,
    @SerialName("revision_number") val revisionNumber: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
```

Conventions carried from `CONTEXT.md`:

- DTOs live in `data/remote/dto/`, use `@Serializable` + `@SerialName`, never leak
  into `domain/` or `presentation/`.
- Every field the backend may omit gets a Kotlin default. **Never** a non-nullable
  type for a field the contract marks nullable — this spec exists partly because
  that rule was broken once.
- Domain models are plain data classes, no serialization annotations.
- Repositories return `Result<T>`; ViewModels fold into a `sealed interface` state
  exposed as `StateFlow`. No LiveData.
- Unknown enum values map to `UNKNOWN` rather than throwing.
- Confirmation/acknowledgement prompts use the shared `AppConfirmationDialog`.

---

## Scope

### A. Visit feedback (new capability)

`POST /appointments/{appointment}/rating`, plus `is_rateable` / `rating` on
`AppointmentResource`. Upsert: **201** creates, **200** revises.

Request: `{ "rating": 1-5 (required), "comment": string|null (max 1000) }`
Response: `{ "data": { id, rating, comment, revision_number, created_at } }`

- **List entry point:** where `isRateable && rating == null`, the appointment row
  shows a compact **Rate this visit** chip that opens the feedback dialog. It is
  presentation-only and disappears once a rating exists. **No persisted
  dismissal** in this iteration — the chip is already easy to ignore, and
  persisting per-appointment dismissal would mean storing new local state for a
  surface we expect to iterate on. If patients report it as nagging, revisit.
- **Detail surface:** when rateable and unrated, a **Rate your visit** action.
  When already rated, show the stars, the comment, and an **Update rating**
  action, plus an **Edited** indicator when `revisionNumber > 1`.
- Both entry points gate on `isRateable`. Never infer rateability client-side from
  `status == fulfilled` — the server also checks ownership and is authoritative.
- `VisitFeedbackDialog` is themed consistently with `AppConfirmationDialog`: 1–5
  star selection, optional comment capped at 1000 characters with a live counter.
- Client-side validation mirrors the server: rating in 1–5, comment ≤ 1000.
  Invalid input never reaches the network.
- On success the returned rating updates state directly, following the
  reschedule pattern — **no** re-`load()` round trip.
- Errors: `404` → "This appointment is no longer available."; `422` → "This visit
  can't be rated yet."; otherwise existing generic handling.
- Hidden comments return `comment: null` to non-authors and authors always see
  their own, so the client renders what it receives with no special-casing.

### B. Frame rating aggregates (new capability)

`GET /frames` and `GET /frames/{id}` now return `average_rating` (float, one
decimal, **null when unrated**) and `rating_count` (integer).

- `FrameDto` and `Frame` gain `averageRating: Double?` and `ratingCount: Int`.
  `averageRating` must stay **nullable** — an unrated product is not a 0.0-star
  product, and collapsing the two would libel every new frame in the catalog.
- Shared `RatingBadge` composable: renders a star glyph, the average to one
  decimal, and the count (e.g. `★ 4.5 (12)`). When `averageRating == null` it
  renders **nothing** — no "No ratings yet" placeholder in the list, where it
  would be visual noise across a mostly-unrated catalog. Frame detail may show a
  quiet "No ratings yet" line, since there is room for it.
- Frame list card and frame detail both use the badge. Accessibility label reads
  the full phrase ("rated 4.5 out of 5 from 12 ratings"), not the glyph.
- Room cache: see Open Questions §1 for the schema decision. The spec assumes
  aggregates are cached (nullable columns, version 3 → 4, additive migration).

**Known backend bug — fixed backend-side, never worked around here.** The
aggregate excludes hidden ratings from **both** the average and the count, rather
than suppressing only the comment, contradicting the documented moderation model
and the `ModerateFrameRating` docblock.

The distortion is **directional, not noise**. Hiding correlates with low stars
(abusive comments accompany bad ratings), so excluding them pushes averages
systematically *upward* — moderation can only ever make a product look better,
never worse. At this clinic's realistic volume — single-clinic catalog, likely
single-digit ratings per product — one hidden 1-star rating moves a displayed
average meaningfully, not marginally.

Per resolved decision 7 this is being fixed backend-side in the same window (drop
the `is_hidden` filter from `FrameController`'s eager-load; keep it only where
comment text is rendered — `docs/specs/mobile-visit-feedback-tasks.md` Task 0d).
Android's behavior is unchanged either way: **display what the server sends.** No
client-side correction, before or after the fix.

### C. Optical-order fields now live (verification, not construction)

Everything the first draft listed as blocked has shipped, and Android's DTOs
already decode it. **No production code change expected** — but these paths have
never executed against real data and nothing asserts them, which is exactly how
the original drift survived a release.

| Field | Surface it drives | Was silently dead because |
|---|---|---|
| `items[].is_rateable` | Rate/Update rating action | defaulted `false` |
| `items[].rating` | Existing-rating line | defaulted `null` |
| `items[].product_variant_id` | (decoded, unused) | defaulted `null` |
| `payment_summary.is_overdue` | "Overdue" label | defaulted `false` |
| `payment_summary.status` | Payment status label | label string → `UNKNOWN` |

Confirm `PaymentStatus.from()` maps all four machine-readable values and still
falls back to `UNKNOWN`. Do **not** add display-label parsing — accepting both
would quietly re-enable the bug being closed.

Add `revision_number` to `OpticalOrderDtos.RatingSummaryDto` and surface the same
**Edited** indicator on optical-order detail, per resolved decision 2.

### D. Live shape corrections

1. **`RatingResultDto.itemId` becomes nullable.** `FrameRatingResource` returns
   `"item_id": null`; our field is non-nullable `Int`, so kotlinx throws on
   **every** frame-rating submit. Propagate to `RatingResult.itemId`. Not
   displayed, so no UI change follows.
2. **`RatingResultDto` gains `product_variant_id`** (`Int?`, defaulted).
3. **Frame-rating request stays as-is.** `product_variant_id` is optional and
   server-derived — what the client already sends.
4. **Frame-rating error codes** are now bare `403` / `404` / `422`. The client
   never referenced the retired codes (verified), so documentation-only.

### E. Route governance

`app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`:

- Header comment and counts: **51 → 53** (8 public, 24 account-only, 21 active-link).
- Add `POST /api/v1/appointments/{appointment}/rating` to `activeLinkRoutes`.
- Move `POST /api/v1/job-order-items/{item}/rating` out of `rejectedRoutes` — the
  backend deliberately keeps it as a compatibility alias, so asserting its absence
  is wrong. It must **not** go into `activeLinkRoutes` either, since that set
  governs what this client may call and we use the canonical path. Add a third
  category, `legacyAliasRoutes`, documented as "exists server-side, must not be
  called by this client", and assert exactly that.
- Everything else in `rejectedRoutes` stays rejected.

### F. Frame reservation semantics

- `expires_at` is `null` until `prepared`. DTO is already `String?` and the list
  screen doesn't render it — **no code change required.** Verified; recorded so
  the plan phase doesn't re-investigate.
- An appointment gets **exactly one reservation, ever** (DB unique constraint), not
  merely one *active* one. `isReservationEligible` can't see prior reservations, so
  it will still offer reservation for an appointment whose reservation was
  cancelled or released; the server rejects it. Scope is limited to making that
  rejection readable — surface the server message instead of a generic failure,
  and adjust copy to say one reservation per appointment. **Filtering the eligible
  list by prior reservations is out of scope** — no reliable signal exists in the
  contract.

### G. Documentation and branding

`CONTEXT.md` has drifted on our own side:

- "34 approved patient-mobile routes" — stale; the governed count is 53.
- "Route Governance — 51 Routes" — update to 53 and describe the alias.
- Sections describing removed features (`/eyewear`, `/job-orders`,
  `/billing-records`, order requests, product catalog tabs, accessory ordering)
  coexist with the sections that replaced them, and the endpoint list under
  "Backend API" contradicts the one under "Mobile REST API".
- **`Architecture`, `Active Specs`, and `Boundaries` each appear twice**, with
  different content.
- **Branding:** canonical spelling is **"EyeCare"**. Update `CONTEXT.md` and the
  app-name string resource. This is the one deliberate user-visible string change
  in this spec, made only because the spelling was explicitly confirmed.

Add this spec to Active Specs.

---

## Testing Strategy

JUnit 5 + MockK + Turbine under `app/src/test/java/`, mirroring production
packages. Compose UI tests in `app/src/androidTest/`. Follow
`skills/test-driven-development` — failing test first for every behavioral change.

| Area | Level | Must cover |
|---|---|---|
| Visit feedback gating | ViewModel | `isRateable=false` → no chip, no detail action; `true` + `rating=null` → create; `true` + rating → revise |
| Visit feedback validation | ViewModel | Rating 0/6 and 1001-char comment rejected before network |
| Visit feedback success | ViewModel + Turbine | Returned rating updates state directly; `load()` **not** re-invoked |
| Visit feedback errors | ViewModel | 404 and 422 produce distinct messages |
| Edited indicator | Unit | `revisionNumber > 1` → shown; `1` or `null` → hidden |
| Appointment DTO | Serialization | Decodes with `is_rateable`/`rating` present **and** absent |
| Frame aggregates | Serialization + repo | `average_rating: null` stays null (never 0.0); populated values reach the domain model |
| Frame cache | Repository | Aggregates survive a cache write/read round trip; migration preserves existing rows |
| Rating badge | Unit/Compose | `null` average renders nothing in list; populated renders value + count + a11y phrase |
| Rating result DTO | Serialization | Decodes `"item_id": null` without throwing — regression test for the live crash |
| Optical order item | Serialization + repo | `is_rateable: true` and populated `rating` survive to the domain model |
| Payment summary | Serialization + repo | All four enum values map; unrecognized → `UNKNOWN`; `is_overdue: true` survives |
| Route governance | `ApiRouteAllowlistTest` | 53 total; appointment rating approved; `job-order-items` neither rejected nor callable |

`./gradlew testDebugUnitTest` and `./gradlew assembleDebug` must pass at each
checkpoint, not only at the end.

---

## Boundaries

**Always:**

- Run `./gradlew assembleDebug` after changes; `testDebugUnitTest` before calling a
  task done.
- Map DTOs to domain models at the repository boundary.
- Default every optional/absent backend field in the DTO.
- Keep `averageRating` nullable — unrated is not zero-rated.
- Gate visit feedback on `is_rateable`, never on `status == fulfilled`.
- Use `sealed interface` for UI state via `StateFlow`.
- Use `AppConfirmationDialog` styling for prompts.

**Ask first:**

- Adding any dependency.
- Changing navigation graph structure or adding a route.
- **The Room schema change** in Scope B (Open Questions §1).
- Any change to a file not listed in Project Structure.
- Any further user-visible string change beyond the confirmed "EyeCare" rename.

**Never:**

- Gson — Kotlinx Serialization only.
- Store tokens or health data in Room (frame catalog cache only).
- Apply `org.jetbrains.kotlin.android` (AGP 9 built-in).
- Add `android.disallowKotlinSourceSets=false`.
- Call `job-order-items/{id}/rating`.
- Re-add display-label parsing to `PaymentStatus.from()`.
- Client-side "correct" the frame rating aggregate for the hidden-ratings bug.
- Delete or weaken a failing test to make a task pass.

---

## Success Criteria

1. `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, and
   `./gradlew ktlintCheck` all pass.
2. `AppointmentDto` decodes payloads with and without `is_rateable`/`rating`.
3. A fulfilled, unrated appointment shows the **Rate this visit** chip in the list;
   a rated one does not.
4. From detail, submitting a rating updates state with the returned value and no
   second GET; a rated appointment offers **Update rating** and shows **Edited**
   once `revisionNumber > 1`.
5. With `is_rateable: false`, no feedback affordance is reachable and no request is
   issued to `/appointments/{id}/rating`.
6. Rating `0`, rating `6`, and a 1001-character comment are each rejected before
   the repository is called.
7. `average_rating: null` reaches the domain model as `null`, and the list card
   renders no badge for it.
8. A frame with `average_rating: 4.5, rating_count: 12` renders `★ 4.5 (12)` with
   the full accessibility phrase.
9. Existing cached frames survive the Room migration; aggregates round-trip
   through the cache.
10. `RatingResultDto` decodes `{"id":1,"item_id":null,...}` without throwing.
11. An optical-order item with `is_rateable: true` and a populated `rating` reaches
    the domain model intact and renders its rating action.
12. `payment_summary.status` of `partially_paid` maps to `PARTIALLY_PAID` and
    renders "Balance due"; `is_overdue: true` renders the Overdue label.
13. `ApiRouteAllowlistTest` asserts 53 routes and passes; no production Retrofit
    annotation references `job-order-items`.
14. `CONTEXT.md` contains exactly one `Architecture`, one `Active Specs`, and one
    `Boundaries` section; states 53 routes; uses "EyeCare"; and records the
    hidden-ratings aggregate bug.
15. No file outside § Project Structure is modified.

---

## Open Questions

1. **Does `is_rateable` stay `true` after a rating exists?** This decides whether
   the revise path is reachable at all.
   - `API_CONTRACT.md` §10 says `is_rateable` is true "only when `status =
     fulfilled` and the appointment belongs to the authenticated patient" — no
     mention of rated state, implying it stays true and revision works.
   - The §15 optical-order wording defines the analogous flag as "whether the
     patient may submit **or revise** a rating", which also implies it stays true.
   - But the review note describing `AppointmentResource.php:22` characterized it
     as true once "fulfilled **and unrated**".

   If the implementation includes an unrated condition, `is_rateable` flips to
   `false` the moment a rating is saved, the **Update rating** action never
   renders, and patients can never revise — even though the endpoint returns
   `200 OK` for revisions and the backend retains full revision history.

   **Verify before Stage 4.** If it does gate on unrated, either the resource
   drops that condition, or the client gates the revise action on
   `rating != null` instead of `isRateable`. The chip is unaffected either way,
   since it already requires `rating == null`.

---

## Verification Checklist (skill gate)

- [x] Spec covers all six core areas (Objective, Commands, Structure, Style,
      Testing, Boundaries)
- [x] Success criteria are specific and testable
- [x] Boundaries defined across Always / Ask first / Never
- [x] Saved to the repository at `docs/specs/backend-alignment-v15-spec.md`
- [x] **Human has reviewed and approved** — 2026-08-07. Proceed to Phase 2 (PLAN):
      `docs/specs/backend-alignment-v15-plan.md`
