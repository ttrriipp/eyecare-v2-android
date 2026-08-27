# Plan: Backend Alignment v20 — Saved Frames Cutover

> Status: **Approved 2026-08-27**  
> Phase: **Plan**  
> Spec: `docs/specs/backend-alignment-v20-2026-08-27-spec.md` — approved 2026-08-27  
> Tasks: `docs/specs/backend-alignment-v20-2026-08-27-tasks.md` — Phase 3 draft  
> Baseline: `assembleDebug` green after the v20 spec was added  
> Date: 2026-08-27

This document defines architecture, dependency order, risks, parallelization boundaries, and
verification checkpoints. It deliberately does not contain the task-level checklist; that belongs in
the Phase 3 tasks document after this plan is approved.

---

## Overview

V20 is a coordinated replacement, not a rename:

1. Add the account-owned Saved Frame protocol and domain boundary.
2. Carry account-specific `is_saved` through live frame responses without persisting ownership state
   in the shared Room catalog cache.
3. Add an account-only Saved Frames list and save/remove actions to Frame Detail and AR Try-On.
4. Remove every appointment-bound Frame Reservation consumer and route.
5. Align navigation and API governance with 59 routes and account-only attachment download.
6. Synchronize living documentation and prove the rest of the app still builds and tests cleanly.

Implementation is ordered to establish the new data path before retiring the old one. Saved Frame
protocol tests and repository behavior land first, then the new UI vertical slices. Only after the new
entry points work does the coordinated cutover delete reservation code and switch route governance.
This keeps each verification checkpoint diagnosable and avoids a long period where neither feature
compiles.

Every repository-changing implementation phase ends with the relevant focused tests and
`assembleDebug`. Full unit, lint, and build verification closes the work.

## Existing Baseline and Constraints

- `docs/BACKEND_CONTEXT.md` and `docs/API_CONTRACT.md` contain user-owned uncommitted backend updates.
  Only the two approved v20 drift corrections and explicitly synchronized route/context sections may
  be patched; unrelated edits must remain byte-for-byte intact.
- The current app has a complete reservation vertical slice: five Retrofit routes, DTO/domain/repository
  layers, DI, three screens, appointment-detail coupling, appointment-request origin state, navigation,
  and tests. All of it becomes invalid when the backend removes the routes.
- Frame catalog variants are cached as serialized DTO JSON in Room. Adding `is_saved` naively would
  persist one account's preference into an account-neutral cache.
- Frame Detail and AR already load a full Frame response and select a variant. They are the correct
  mutation surfaces because the ProductVariant ID is unambiguous there.
- The current `FrameDetail` typed route carries only a product/frame ID. Saved list navigation needs an
  optional ProductVariant ID so the exact preference opens selected.
- Frame list/detail use `FrameRepository`; Saved Frame mutations need a separate repository because
  their response, pagination, ownership, and lifecycle differ from catalog data.
- Attachment UI already allows account-owned download for linked and general-inquiry conversations
  and capability-gates upload. The remaining v20 change is route classification/governance and living
  documentation, not a messaging UI rewrite.
- No dependency, Room schema, WorkManager, push, backend, or bottom-navigation change is permitted.
- Existing unrelated `.impeccable` output and user document edits remain untouched.

## Shape of the Work

Six ordered implementation phases:

```text
Phase 0  Contract fixtures + failing governance tests
   │
   ▼
Phase 1  Saved Frame protocol/domain/repository + cache isolation
   │
   ├─────────────────────────────┐
   ▼                             ▼
Phase 2  Saved list + Profile    Phase 3  Detail + AR save/remove
   │                             │
   └──────────────┬──────────────┘
                  ▼
Phase 4  Coordinated reservation removal + navigation/route cutover
                  │
                  ▼
Phase 5  Lifecycle hardening + docs + full verification
```

Default execution is Phase 0 → 1 → 2 → 3 → 4 → 5. Once Phase 1 is green, Phases 2 and 3 are logically
independent except for `FrameDetail` route/state ownership. If work is split, Phase 2 owns the route
shape and Saved Frames screen while Phase 3 consumes that settled route and owns the mutation
surfaces. Phase 4 exclusively owns shared navigation, appointment cleanup, allowlist totals, and
reservation deletion.

## Architecture Decisions

### A1 — Treat Saved Frames as a new vertical slice

The reservation types cannot be renamed safely. Their identity is reservation/item ID; Saved Frames
are keyed by ProductVariant ID. Reservations are unpaginated, patient-linked, appointment-bound,
stateful holds; Saved Frames are page-paginated, account-owned, persistent preferences with an
availability projection.

Create `SavedFrameApiService`, `SavedFrameDtos`, `SavedFrameRepositoryImpl`, domain types, repository
interface, DI module, and Saved Frames presentation state. Reusing small pure helpers such as the
money serializer, AR value objects, image URL handling, or page metadata is allowed. Reusing
reservation models or repositories is not.

### A2 — ProductVariant ID is the only mutation identity

The backend deliberately exposes no Saved Frame ID. All list deduplication, per-row in-flight state,
PUT/DELETE paths, and variant UI updates use `productVariantId`.

The list domain item also carries its parent Frame/Product ID so it can navigate to
`FrameDetail(frameId, variantId)`. The client never invents a Saved Frame identifier or uses list
position as identity.

### A3 — Keep catalog and preference repositories separate

`FrameRepository` remains responsible for searchable/paged catalog reads and Room fallback.
`SavedFrameRepository` owns page reads and idempotent save/remove calls. This prevents Saved Frame
pagination and mutation state from leaking into catalog paging and avoids expanding the existing
Frame repository into an unrelated account-preference service.

Frame Detail and AR may inject both repositories: FrameRepository supplies the selected product and
server `isSaved`; SavedFrameRepository mutates the preference.

### A4 — Account-specific cache state fails closed

`FrameVariantDto.isSaved` defaults to false only so pre-v20 serialized cache JSON remains readable.
Network fixtures and tests always include the required field. Before encoding variants into
`FrameEntity.variantsJson`, the repository copies them with `isSaved=false`.

This provides three properties:

1. Old cache rows do not break decoding after upgrade.
2. New cache rows cannot expose Account A's saved state to Account B.
3. Offline catalog fallback may under-report a save but can never falsely claim ownership.

No database migration or separate Saved Frame table is introduced.

### A5 — Live screen state is authoritative after mutation

PUT returns the Saved Frame resource; DELETE returns no content. ViewModels update the selected
variant or list row only after successful responses. They do not optimistically flip before the
network succeeds, which avoids rollback ambiguity and is acceptable for a small account preference.

Each ProductVariant mutation is single-flight. Repeated taps while the same mutation is active are
ignored and the actual control is disabled. Failures retain the prior state. A 422 save is translated
to patient-safe unavailable copy; raw server validation text is not displayed.

### A6 — Reconcile independently retained destinations on resume

Frame list, Frame Detail, AR, and Saved Frames use separate ViewModels and may remain on the navigation
back stack. A mutation in one does not share an in-memory DTO with another. Each retained surface
therefore refreshes through an explicit lifecycle/resume hook or its existing refresh entry point.

Do not add a process-global mutable singleton solely to synchronize bookmark icons. Server refresh is
the source of truth and avoids cross-account state retention.

### A7 — Exact saved-variant navigation is optional and fail-safe

Change the typed route to:

```kotlin
@Serializable
data class FrameDetail(
    val frameId: Int,
    val variantId: Int? = null,
)
```

Direct catalog/home navigation omits `variantId`. Saved list navigation supplies it. Frame Detail
selects a matching variant, then safely falls back to the first available option when the ID is null,
stale, inactive, or absent. No new backend call or detail endpoint is created.

### A8 — AR owns its save mutation without disturbing rendering state

`ArViewModel` already owns loaded variants and selected-variant changes. Inject
SavedFrameRepository there and add narrowly scoped mutation fields/effects to active UI state. A
successful toggle replaces only the selected variant's `isSaved` value in the loaded variant list.

Camera permission, face tracking, model download, calibration, renderer state, fallback imagery, and
capability checks remain untouched. The old navigation callback to CreateFrameReservation is deleted;
the save action stays inside the AR destination.

### A9 — Page state stays in SavedFramesViewModel

Saved Frames uses numeric Laravel pagination. The repository returns a `SavedFramePage`; the ViewModel
owns current page, load-more eligibility, ID deduplication, refreshing, row removals, and inline errors.
The repository does not retain `lastMeta` globally and the composable does not manage page counters.

Refresh commits page 1 only on success. Load-more advances only on success. Removal deletes the item
from the current view but does not fabricate new pagination totals; the next refresh reconciles totals.

### A10 — Reservation retirement is one coordinated compile checkpoint

Reservation cleanup crosses production and test layers. Phase 4 switches navigation/profile/AR/detail
entry points first within the same patch, removes appointment/request dependencies, then deletes the
reservation feature files and updates route rejection/governance. The phase is not considered complete
until `rg` finds no active source references and the app compiles.

Historical specs and backend migration history remain. Active Android code and current context must
not retain compatibility shims for dead endpoints.

### A11 — Route governance follows the backend tier, not old UI assumptions

The allowlist becomes:

- public: 8;
- account-only: previous 36 + 3 Saved Frames + attachment download = 40;
- active-link: previous 17 - 5 reservations - attachment download = 11.

All five reservation routes enter `rejectedRoutes`. Retrofit discovery normalization adds
`saved-frames/{productVariant}` and removes reservation-specific normalization. Upload remains the
existing `POST /conversation/messages` route; runtime `can_upload_attachments` continues to decide
whether the multipart control is exposed.

### A12 — Documentation corrections are minimal and evidence-driven

Implementation changes only approved current documentation:

- add `is_saved` to the `GET /frames` response example because the same contract calls it required;
- restore optional `cursor` in the message-search query list because both backend context and the
  endpoint prose specify cursor pagination;
- rewrite the current `CONTEXT.md` reservation section into Saved Frames behavior;
- update access statements, route totals, feature summary, active specs, and AGENTS current work.

Do not rewrite unrelated backend history, AR workflow notes, or completed historical specs.

## Component and Dependency Map

```text
Frame catalog API
└── FrameVariantDto.isSaved
    ├── FrameRepository mapping ──→ FrameVariant.isSaved
    └── Room encoder ─────────────→ copy(isSaved = false)

SavedFrameApiService
└── SavedFrameDtos + page metadata
    └── SavedFrameRepositoryImpl
        └── SavedFrameRepository
            ├── SavedFramesViewModel ──→ SavedFramesScreen ──→ Profile/NavGraph
            ├── FrameDetailViewModel ──→ FrameDetailScreen
            └── ArViewModel ───────────→ ArTryOnScreen

Typed FrameDetail(frameId, variantId?)
├── catalog/home callers omit variantId
└── Saved Frames supplies exact productVariantId

Reservation retirement
├── appointment detail dependency removed
├── appointment request origin removed
├── routes/intents/screens removed
├── API/DTO/repository/DI/domain removed
└── allowlist rejects all five old routes
```

### Dependency order

1. Contract fixtures and failing tests define wire shape and route totals.
2. DTO/domain/repository/DI establish a compile-safe Saved Frame seam.
3. Frame catalog `isSaved` and cache isolation support all mutation surfaces.
4. Saved list and exact-variant route provide the replacement destination.
5. Detail/AR mutations replace reservation entry points.
6. Navigation and appointment/request cutover make reservation code unreachable.
7. Reservation files are deleted and route governance flips to 59.
8. Lifecycle reconciliation and docs close the work.

## Phase Rationale and Deliverables

### Phase 0 — Contract fixtures and red tests

Purpose: make the backend delta executable before production signatures change.

Deliverables:

- Saved Frame JSON fixtures for page/list, save response, availability, `is_saved`, price formats,
  and typed/null AR.
- Red DTO/service/repository expectations for the new routes and mapping.
- Red route-governance expectations for 8/40/11/59, Saved Frames account access, attachment movement,
  and rejected reservations.
- Exact documentation drift locations recorded; no broad source-doc rewrite.
- Baseline relevant tests captured so pre-existing failures are distinguishable.

Checkpoint: new tests fail for the expected missing implementation, while unrelated baseline tests
remain green. Run `assembleDebug` after committing repository document/test changes.

### Phase 1 — Protocol, domain, repository, DI, and cache isolation

Purpose: create the complete non-UI Saved Frame boundary.

Deliverables:

- `SavedFrameApiService` with GET/PUT/DELETE signatures and no mutation bodies.
- Saved Frame DTOs, domain models, availability mapping, repository interface/implementation, and Hilt
  module.
- `FrameVariantDto`/`FrameVariant.isSaved` mapping.
- Legacy-cache decode fallback and cache-write stripping of account-specific saved state.
- Focused DTO, MockWebServer/service, repository, and cache-isolation tests.

Checkpoint: all new data-layer tests pass; existing frame/AR repository tests pass; `assembleDebug`
passes before presentation work begins.

### Phase 2 — Saved Frames list and exact-variant navigation

Purpose: deliver the account-only replacement destination independently of mutation entry points.

Deliverables:

- SavedFramesViewModel state for load, refresh, pagination, dedupe, empty/error, load-more error, and
  per-row removal.
- SavedFramesScreen with preference disclaimer, availability semantics, accessible remove state, and
  exact-variant row navigation.
- Optional `variantId` in typed FrameDetail route and selection fallback tests.
- Profile callback/label switched to Saved Frames for linked and unlinked accounts.
- Account-only route classification and navigation tests for the new destination.

Checkpoint: Saved Frames presentation/navigation tests pass; limited-account access does not open the
link hub; existing Profile and Frame Detail tests pass; `assembleDebug` passes.

### Phase 3 — Frame Detail and AR save/remove actions

Purpose: replace both existing Reserve actions with direct account preference mutations.

Deliverables:

- FrameDetailViewModel SavedFrameRepository dependency and selected-variant mutation state.
- Frame Detail Save/Remove UI, disabled/single-flight behavior, disclaimer, and safe errors.
- ArViewModel mutation state and selected-variant list update.
- AR Try-On Save/Remove control and feedback; create-reservation callback removed.
- ViewModel and Compose/pure UI tests for variant switching, success, failure, 422, and duplicate taps.
- Regression tests proving AR loading/render state is unchanged by preference mutation.

Checkpoint: detail/AR focused tests and the existing AR suite pass; no Reserve action remains on those
surfaces; `assembleDebug` passes.

### Phase 4 — Coordinated reservation and route cutover

Purpose: remove dead behavior only after its replacement is operational.

Deliverables:

- Delete reservation routes, intents, navigation destinations, screens, ViewModels, presentation
  helpers, domain/repository/data/DI files, and their dedicated tests.
- Remove appointment-detail reservation injection/load/rendering and update constructor/UI tests.
- Remove appointment-request reservation-origin route/state/submission plumbing and update tests.
- Switch NavGraph/Profile/AR/Frame Detail wiring entirely to Saved Frames.
- Route allowlist updated to 8/40/11/59; attachment download moved to account-only; five reservation
  routes rejected.
- Static searches prove no production Retrofit annotation or active UI copy references the old
  reservation feature.

Checkpoint: route-governance, appointment, request, navigation, Profile, frame, AR, and messaging
capability tests pass; `assembleDebug` passes with all reservation files gone.

### Phase 5 — Lifecycle reconciliation, living docs, and final verification

Purpose: harden cross-screen truth and finish the living contract.

Deliverables:

- Resume/refresh behavior reconciles retained Frames, Frame Detail, AR, and Saved Frames state without
  a global account-preference singleton.
- Exact API-contract corrections for `is_saved` example and search cursor row.
- `CONTEXT.md` updated for Saved Frames, account access, attachment download, route totals, and removed
  appointment coupling.
- `AGENTS.md` current-work pointer and active spec/plan/tasks listings updated only when documents exist.
- Formatting, full unit suite, lint, and debug build executed; instrumented tests run when a device is
  available or explicitly reported as not run.

Checkpoint: every spec success criterion is evidenced and the working tree contains no unintended
changes.

## File Ownership and Hotspots

| Area | Primary files | Risk |
|---|---|---|
| Wire/domain foundation | `FrameDtos.kt`, new `SavedFrameDtos.kt`, new API/repository/domain/DI files | Nested resource drift, money/AR reuse |
| Cache isolation | `FrameRepositoryImpl.kt`, frame repository tests | Cross-account saved-state leak |
| Exact-variant route | `Routes.kt`, `FrameDetailViewModel.kt`, `NavGraph.kt` | Constructor/route churn across callers |
| Saved list | new presentation files, `ProfileScreen.kt` | Pagination and limited-account access |
| Detail mutation | `FrameDetailViewModel.kt`, `FrameDetailScreen.kt` | Variant-local state and lifecycle refresh |
| AR mutation | `ArViewModel.kt`, `ArTryOnScreen.kt`, AR state models | Accidental renderer/camera regression |
| Reservation removal | reservation package and data/domain/DI files | Broad compile/test fan-out |
| Appointment cleanup | `AppointmentDetail*`, `RequestAppointment*` | Constructor and state-test churn |
| Route governance | `ApprovedApiRoutes.kt`, `ApiRouteAllowlistTest.kt` | Tier/count mismatch |
| Living docs | `CONTEXT.md`, `API_CONTRACT.md`, `AGENTS.md` | Overwriting user-owned edits |

`NavGraph.kt`, `Routes.kt`, `FrameDetailViewModel.kt`, and `FrameRepositoryImpl.kt` are shared hotspots.
Only one phase should own each at a time. The future tasks document must split work so no task changes
more than roughly five files unless a coordinated deletion/cutover cannot compile otherwise.

## Risk Register

| Risk | Impact | Mitigation |
|---|---|---|
| `is_saved` persisted in Room | Another account sees stale ownership state | Strip on encode; legacy/default-false and cross-account cache tests |
| Required field absent in live backend response | UI falsely reports unsaved | Contract fixtures include it; source example corrected; refresh remains authoritative |
| Saved response nested shape diverges from catalog DTO | Decode failure | Dedicated DTOs; representative string/number price and AR fixtures |
| Saved list opens the wrong variant | Confusing remove/save state | Optional route variant ID; exact-selection and stale-ID fallback tests |
| Rapid PUT/DELETE taps race | Incorrect final state or duplicate work | Per-variant single-flight and disabled controls |
| Back-stack screen shows stale bookmark | Contradictory UI after mutation | Lifecycle/resume reconciliation; no global mutable singleton |
| Unknown availability appears available | False stock promise | UNKNOWN renders unavailable and blocks optimistic wording |
| Reservation deletion breaks appointment/request constructors | Compile failure | One coordinated Phase 4 checkpoint with focused constructor tests |
| Old endpoint survives in Retrofit | Runtime 404/contract violation | Rejected-route discovery test plus static search |
| Attachment upload accidentally opens to unlinked accounts | Privacy/authorization regression | Preserve server capability + linked access test while only moving download tier |
| AR mutation destabilizes renderer state | Camera/try-on regression | Narrow state updates and full existing AR suite at Phase 3 |
| Source docs are overwritten broadly | User work loss | Patch exact approved lines; inspect scoped diffs before verification |
| Gradle/network bootstrap fails in sandbox | Verification blocked | Use installed Android Studio JBR and approved Gradle network escalation when required |

## Verification Checkpoints

### Checkpoint A — Contract foundation

- Saved Frame fixtures cover list/save/availability/AR/price shapes.
- Service signatures prove PUT/DELETE have no body.
- Governance tests express 59-route target and fail only because implementation is not yet present.
- Existing unrelated unit tests remain green.

### Checkpoint B — Data boundary

- DTO/repository/DI compile and focused tests pass.
- `isSaved` reaches live domain variants.
- Cache encoding always writes false and old cache JSON remains readable.
- No Room schema change exists.

### Checkpoint C — Saved destination

- All list states and numeric pagination paths are tested.
- Unlinked account access is account-only.
- Preference disclaimer and unavailable semantics are accessible.
- Exact saved variant opens selected with safe stale-ID fallback.

### Checkpoint D — Mutation surfaces

- Frame Detail and AR toggle only the selected ProductVariant.
- Duplicate taps are ignored; controls are disabled in flight.
- Failures preserve state and translate 422 safely.
- Existing AR rendering/camera tests remain green.

### Checkpoint E — Cutover

- No active reservation route, type, screen, state, or wording remains.
- Appointment Detail and Appointment Request no longer depend on reservation behavior.
- Route totals are 8/40/11/59 and attachment download is account-only.
- Production Retrofit discovery contains zero rejected routes.

### Checkpoint F — Final

- Lifecycle reconciliation tests pass.
- Current docs and active pointers match implementation.
- `ktlintFormat`, full unit tests, lint, and debug build pass.
- Scoped `git diff --check` passes and unrelated user changes are preserved.

## Parallelization Opportunities

Parallel work is safe only after Phase 1 defines stable domain/repository interfaces:

- Saved list state/UI can proceed independently from AR mutation logic.
- DTO/repository tests can be extended independently from Compose tests once signatures settle.
- Documentation reconciliation can be drafted while final tests run, but must be applied only after
  implementation behavior is verified.

The following are sequential:

- DTO shape before repository mapping.
- Repository/domain before any ViewModel.
- Optional Frame Detail route shape before Saved list navigation and detail selection tests.
- New save/remove surfaces before deleting reservation navigation.
- Appointment/request cleanup and reservation file deletion within one Phase 4 ownership window.
- Route total flip after production Retrofit reservation services are removed.

This plan identifies parallel boundaries for scheduling; it does not authorize sub-agent delegation.

## Verification Commands

Use Android Studio's bundled JBR on this workstation:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew ktlintFormat
.\gradlew testDebugUnitTest --tests "*SavedFrame*" --tests "*FrameDetailViewModelTest" --tests "*ArViewModelTest" --tests "*ApiRouteAllowlistTest"
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
.\gradlew connectedDebugAndroidTest
```

Also run scoped static checks:

```powershell
rg -n -i "frame-reservations|FrameReservation|Reserve this frame|isFrameReservationOrigin" app/src/main app/src/test app/src/androidTest
rg -n "saved-frames|is_saved" app/src/main app/src/test CONTEXT.md docs/API_CONTRACT.md
git diff --check
```

The first search may match historical comments only if they are intentionally retained outside active
code; active production/test hits must be zero after Phase 4. `connectedDebugAndroidTest` requires an
available emulator/device and is reported honestly when unavailable.

## What This Plan Does Not Do

- It does not retain or emulate reservation history on Android.
- It does not migrate backend reservation data or release inventory.
- It does not persist Saved Frames locally.
- It does not add catalog-card save mutations, push, background sync, or a bottom tab.
- It does not change AR models, calibration, rendering, or camera capabilities.
- It does not expand Encounter → Consultation terminology into nonexistent Android surfaces.
- It does not change message search behavior beyond correcting the missing cursor documentation row.
- It does not commit, branch, or modify unrelated user changes.

## Open Questions

1. **Approval gate:** Approve this dependency order and architecture so Phase 3 can create the discrete
   implementation task checklist. No production implementation starts before both plan and task
   checklist are reviewed.
