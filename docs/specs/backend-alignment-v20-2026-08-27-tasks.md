# Tasks: Backend Alignment v20 — Saved Frames Cutover

> Status: **Phase 4 complete, Phase 5 Tasks 27–28 complete — Phases 0–3 pending**  
> Phase: **Implementation in progress**  
> Spec: `docs/specs/backend-alignment-v20-2026-08-27-spec.md` — approved 2026-08-27  
> Plan: `docs/specs/backend-alignment-v20-2026-08-27-plan.md` — approved 2026-08-27  
> Implementation: Phase 4 (Tasks 16–26) and Phase 5 Tasks 27–28 shipped; Phases 0–3 (Tasks 1–15) and Phase 5 Tasks 29–30 pending  
> Date: 2026-08-27

This is the authoritative Phase 3 execution checklist. Tasks are dependency ordered, normally touch
five files or fewer, and are designed to keep the repository compiling at every completed task.
Phase 4 implementation executes one task at a time using `incremental-implementation`,
`test-driven-development`, and `context-engineering` as required by the approved spec workflow.

---

## Execution Rules

1. Start every behavioral task with the listed failing test or fixture assertion before production
   code.
2. Do not start a task until its dependencies and the preceding checkpoint are green.
3. Run `assembleDebug` after every repository change, including test-only, deletion-only, and
   documentation-only tasks.
4. Keep work inside the listed files unless an unavoidable compile dependency is recorded in this
   tasks document before the edit.
5. Preserve user-owned changes in `docs/API_CONTRACT.md`, `docs/BACKEND_CONTEXT.md`, `.impeccable/`,
   models, and unrelated features. Inspect scoped diffs at every checkpoint.
6. Do not mark a checkbox complete from compilation alone. All acceptance and verification items for
   the task must pass.
7. Never retain a temporary compatibility call to `/frame-reservations`; the replacement must be
   complete before the old route is deleted.
8. Delete only the exact obsolete files listed in Tasks 20–25. Resolve and verify their absolute paths
   before applying deletion patches.
9. Stop at any contract ambiguity, new dependency, Room schema need, or backend behavior change and
   update the living spec before continuing.

## Command Alias

`<gradle>` means the following PowerShell setup and Gradle wrapper invocation:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew
```

Every task's verification includes `<gradle> assembleDebug`, even when only the focused command is
written first.

## Task Index

| Phase | Tasks | Outcome |
|---|---:|---|
| 0 — Contract gate | 1–2 | Corrected source examples and frozen v20 fixtures |
| 1 — Data foundation | 3–6 | Saved protocol/domain/repository plus safe catalog cache |
| 2 — Saved destination | 7–11 | Exact variant route, paged list, Profile entry, account-only access |
| 3 — Mutation surfaces | 12–15 | Frame Detail and AR save/remove behavior |
| 4 — Coordinated cutover | 16–26 | Appointment decoupling, reservation deletion, 59-route governance |
| 5 — Closeout | 27–30 | Lifecycle reconciliation, living docs, workflow status, full verification |

---

## Phase 0 — Contract Gate and Baseline

### Task 1: Correct the two approved API-contract drifts

**Description:** Patch only the current Frame and message-search examples so the authoritative API
document matches its own required-field and cursor-pagination prose.

**Acceptance criteria:**

- [ ] `GET /frames` example variants include required boolean `is_saved`.
- [ ] Message-search query parameters include optional opaque `cursor`, omitted on the first page and
  returned unchanged on subsequent requests.
- [ ] No unrelated user-authored backend update is changed.

**Verification:**

- [ ] `rg -n -C 4 'is_saved|conversation/messages/search|cursor' docs/API_CONTRACT.md` shows one
  internally consistent contract.
- [ ] `git diff --check -- docs/API_CONTRACT.md` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Approved v20 spec and plan

**Files:**

- `docs/API_CONTRACT.md`

**Estimated scope:** S (1 file)

### Task 2: Freeze representative Saved Frame fixtures

**Description:** Add raw contract fixtures and structural assertions before production DTOs exist.

**Acceptance criteria:**

- [ ] Fixtures cover a paginated Saved Frames response, save response, available/unavailable values,
  nested product/variant, string and number prices, typed/null AR, and required catalog `is_saved`.
- [ ] Assertions prove ProductVariant ID identity, newest-first timestamps, numeric page metadata, and
  the absence of reservation/appointment/stock-count fields.
- [ ] Existing v19 messaging and notification fixtures remain unchanged except the approved catalog
  fixture addition.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*ApiContractFixturesTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 1

**Files:**

- `app/src/test/java/com/eyecare/app/data/remote/ApiContractFixtures.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiContractFixturesTest.kt`

**Estimated scope:** S (2 files)

> ### Checkpoint A — Contract gate (after Tasks 1–2)
>
> - [ ] API prose, examples, and fixtures describe one v20 schema.
> - [ ] The new fixture assertions fail only where production support is intentionally still absent.
> - [ ] Scoped diff review proves the user's backend updates are preserved.
> - [ ] `assembleDebug` passes.

---

## Phase 1 — Saved Frame Data Foundation

### Task 3: Add catalog `isSaved` and isolate it from Room

**Description:** Carry live account ownership through Frame mapping while forcing the shared cached
variant JSON to false. Begin with tests for live mapping, old-cache decoding, and cache-write stripping.

**Acceptance criteria:**

- [ ] `FrameVariantDto` decodes `is_saved`; missing legacy cached values fail closed to false.
- [ ] `FrameVariant` exposes `isSaved`, and live list/detail mapping preserves it.
- [ ] `FrameRepositoryImpl` writes cached variant copies with `isSaved=false`.
- [ ] A cache round-trip can never reproduce a true saved state for another account.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*FrameDtosTest" --tests "*FrameRepositoryArMappingTest"`
  passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 2

**Files:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/FrameDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Frame.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FrameRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/FrameDtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/FrameRepositoryArMappingTest.kt`

**Estimated scope:** M (5 files)

### Task 4: Define Saved Frame DTO and domain models

**Description:** Implement the wire and domain shapes independently of Retrofit and repositories.

**Acceptance criteria:**

- [ ] DTOs decode page/resource envelopes, nested product/variant, money, images, and typed/null AR.
- [ ] Domain models expose ProductVariant ID, saved timestamp, parent Frame ID, display data, and safe
  `AVAILABLE`/`UNAVAILABLE`/`UNKNOWN` availability.
- [ ] Unknown availability maps fail closed and no DTO type leaks into domain.
- [ ] DTO tests consume the fixtures from Task 2.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*SavedFrameDtosTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 3

**Files:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/SavedFrameDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/SavedFrame.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/SavedFrameDtosTest.kt`

**Estimated scope:** M (3 files)

### Task 5: Declare and test the Saved Frame Retrofit service

**Description:** Add the three account-only calls with exact paths, query bounds, and bodyless
mutations.

**Acceptance criteria:**

- [ ] GET sends `page` and `per_page` and decodes the page envelope.
- [ ] PUT targets ProductVariant ID, sends no request body, and decodes the wrapped resource.
- [ ] DELETE targets ProductVariant ID, sends no body, and accepts 204.
- [ ] Service tests inspect HTTP method, path, query, and zero-length mutation bodies.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*SavedFrameApiServiceTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 4

**Files:**

- `app/src/main/java/com/eyecare/app/data/remote/api/SavedFrameApiService.kt`
- `app/src/test/java/com/eyecare/app/data/remote/api/SavedFrameApiServiceTest.kt`

**Estimated scope:** S (2 files)

### Task 6: Implement the Saved Frame repository and Hilt boundary

**Description:** Map DTOs once at the repository boundary, expose numeric page state, and use the
existing safe API call/error decoder.

**Acceptance criteria:**

- [ ] Repository interface exposes page load, idempotent save, and idempotent remove.
- [ ] Implementation maps availability, product/variant data, money/images/AR, and page metadata.
- [ ] Save returns the server resource; remove treats successful 204 as Unit.
- [ ] 422/network/server failures remain typed through `Result` and raw validation text is not exposed
  by repository-created strings.
- [ ] Hilt provides and binds the API/repository without adding a dependency.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*SavedFrameRepositoryImplTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 5

**Files:**

- `app/src/main/java/com/eyecare/app/domain/repository/SavedFrameRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/SavedFrameRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/SavedFrameModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/SavedFrameRepositoryImplTest.kt`

**Estimated scope:** M (4 files)

> ### Checkpoint B — Data boundary (after Tasks 3–6)
>
> - [ ] All Saved Frame DTO/service/repository tests pass.
> - [ ] Live catalog mapping preserves `isSaved`; Room always stores false.
> - [ ] No DTO crosses the repository boundary and no Room schema changed.
> - [ ] Existing Frame and typed-AR repository tests remain green.
> - [ ] `assembleDebug` passes.

---

## Phase 2 — Saved Frames Destination

### Task 7: Add exact-variant Frame Detail routing

**Description:** Extend the existing typed route with an optional ProductVariant ID and make initial
selection deterministic before the Saved Frames list depends on it.

**Acceptance criteria:**

- [ ] `FrameDetail(frameId, variantId=null)` preserves all current callers.
- [ ] FrameDetailViewModel selects a matching requested variant when supplied.
- [ ] Null or stale variant IDs safely select the first available variant.
- [ ] Refresh retains the current valid selection and never crashes on an empty variant list.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*FrameDetailViewModelTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Checkpoint B

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/frames/FrameDetailViewModelTest.kt`

**Estimated scope:** M (4 files)

### Task 8: Implement SavedFramesViewModel page and removal state

**Description:** Build the presentation state reducer before Compose UI.

**Acceptance criteria:**

- [ ] State covers initial load/error, empty/success, refresh retention, numeric load-more, inline
  load-more error, and per-variant removal.
- [ ] Items deduplicate by ProductVariant ID and page advances only on success.
- [ ] Refresh replaces data only on success; failed refresh keeps usable rows.
- [ ] Remove is single-flight per variant, removes only after success, and retains the row on failure.
- [ ] Errors are concise and patient-safe, including a safe unavailable result for 422 save-state
  conflicts returned during list reconciliation.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*SavedFramesViewModelTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 6

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/frames/SavedFramesViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/frames/SavedFramesViewModelTest.kt`

**Estimated scope:** M (2 files)

### Task 9: Build the stateless Saved Frames screen

**Description:** Render ViewModel state with accessible preference and availability semantics.

**Acceptance criteria:**

- [ ] Screen renders loading, retry, empty, populated, refreshing, loading-more, and inline-error states.
- [ ] The required preference-only availability disclaimer is visible.
- [ ] Rows show product/variant, safe price/image, saved time, and non-color-only unavailable state.
- [ ] Row click returns `(frameId, productVariantId)`; remove has a 48dp target and true disabled state.
- [ ] No stock count, unavailable reason, reservation, hold, expiry, or appointment copy appears.

**Verification:**

- [ ] `<gradle> assembleDebugAndroidTest` compiles `SavedFramesScreenTest`.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Tasks 7–8

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/frames/SavedFramesScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/frames/SavedFramesScreenTest.kt`

**Estimated scope:** M (2 files)

### Task 10: Replace the Profile reservation entry and wire Saved Frames

**Description:** Connect the new destination without yet deleting the old unreachable reservation
files.

**Acceptance criteria:**

- [ ] Profile shows **Saved Frames** for linked and limited accounts.
- [ ] The callback opens the typed Saved Frames destination in MainGraph.
- [ ] A saved row opens `FrameDetail(frameId, variantId)`.
- [ ] Existing Messages, Prescriptions, My Orders, and account-link behavior remains intact.

**Verification:**

- [ ] `<gradle> assembleDebugAndroidTest` compiles Profile UI tests with the new label/callback.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 9

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** M (3 files)

### Task 11: Classify Saved Frames as account-only

**Description:** Prove limited/unlinked accounts never enter the link hub for Saved Frames.

**Acceptance criteria:**

- [ ] Saved Frames route is `AccountOnly`.
- [ ] Linked and unlinked/pending/unknown authenticated account statuses can access it.
- [ ] Confirmed appointments, prescriptions, and orders remain active-link protected.
- [ ] Unknown routes still fail closed.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*PatientRouteAccessTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 10

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientRouteAccess.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt`

**Estimated scope:** S (2 files)

> ### Checkpoint C — Saved destination (after Tasks 7–11)
>
> - [ ] Saved Frames list/pagination/removal state tests pass.
> - [ ] Profile exposes Saved Frames to both linked and limited accounts.
> - [ ] Exact saved variant navigation and stale-ID fallback are proven.
> - [ ] Disclaimer, unavailable, loading, error, and disabled semantics compile in UI tests.
> - [ ] `assembleDebug` passes.

---

## Phase 3 — Frame Detail and AR Mutation Surfaces

### Task 12: Add test-first Frame Detail mutation state

**Description:** Inject SavedFrameRepository and implement selected-variant single-flight save/remove
behavior in the ViewModel before changing buttons.

**Acceptance criteria:**

- [ ] Save/remove targets only the selected ProductVariant ID.
- [ ] Duplicate taps while a mutation is active issue one call.
- [ ] Success updates only the matching variant's `isSaved` value in current Frame state.
- [ ] Failure preserves prior state; 422 uses patient-safe unavailable copy.
- [ ] Switching variants displays independent saved and in-flight state.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*FrameDetailViewModelTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Checkpoint C

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/frames/FrameDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/frames/FrameDetailViewModelTest.kt`

**Estimated scope:** M (2 files)

### Task 13: Replace Frame Detail Reserve UI with Save/Remove

**Description:** Bind the mutation state and remove create-reservation navigation from Frame Detail.

**Acceptance criteria:**

- [ ] Selected unsaved variants show **Save frame**; saved variants show **Remove from saved**.
- [ ] Control is actually disabled and announces progress while in flight.
- [ ] Preference-only disclaimer is visible/directly accessible and success/failure feedback is safe.
- [ ] Try-On remains available only for typed-ready AR and is otherwise unchanged.
- [ ] NavGraph no longer passes a reserve callback to Frame Detail.

**Verification:**

- [ ] `<gradle> assembleDebugAndroidTest` compiles Frame Detail saved-state UI coverage.
- [ ] `<gradle> testDebugUnitTest --tests "*FrameDetailViewModelTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 12

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/frames/FrameDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/frames/FrameDetailSavedStateScreenTest.kt`

**Estimated scope:** M (3 files)

### Task 14: Add test-first AR selected-variant mutation state

**Description:** Add SavedFrameRepository behavior to ArViewModel without changing renderer/camera
logic.

**Acceptance criteria:**

- [ ] The selected AR variant can save/remove by ProductVariant ID.
- [ ] Mutation is single-flight and updates only the matching loaded variant.
- [ ] Variant switching reflects each variant's `isSaved` value.
- [ ] Failure retains asset, face, pose, renderer, and saved state and emits safe feedback.
- [ ] Existing AR capability, model-load, retry, and lifecycle tests remain green.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*ArViewModelTest"` passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 12

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/ar/ArViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/model/ArTryOnUiState.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/ArViewModelTest.kt`

**Estimated scope:** M (3 files)

### Task 15: Replace AR Reserve UI with Save/Remove

**Description:** Keep preference mutation inside AR and delete its create-reservation navigation
callback.

**Acceptance criteria:**

- [ ] AR shows Save/Remove for the selected variant with true disabled/in-flight semantics.
- [ ] First save produces concise preference-only feedback; errors preserve the active try-on.
- [ ] NavGraph no longer navigates from AR to CreateFrameReservation.
- [ ] Catalog-return behavior and renderer overlay remain unchanged.
- [ ] No AR UI copy says reserve, held, set aside, expiry, or appointment.

**Verification:**

- [ ] `<gradle> testDebugUnitTest --tests "*ArTryOnContentStateTest" --tests "*ArViewModelTest"`
  passes.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 14

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/presentation/ar/ArTryOnContentStateTest.kt`

**Estimated scope:** M (3 files)

> ### Checkpoint D — Mutation surfaces (after Tasks 12–15)
>
> - [ ] Frame Detail and AR mutate only the selected variant and prevent duplicate calls.
> - [ ] Failure/422 tests preserve state and expose safe copy.
> - [ ] No Reserve action remains on Frame Detail or AR.
> - [ ] The complete existing AR unit suite remains green.
> - [ ] `assembleDebug` passes.

---

## Phase 4 — Coordinated Reservation and Route Cutover

### Task 16: Remove reservation loading from Appointment Detail ✅

**Description:** Decouple confirmed appointment display before deleting the reservation repository.

**Acceptance criteria:**

- [x] AppointmentDetailViewModel injects only AppointmentV1Repository for its data load.
- [x] UI state no longer contains `frameReservations` and the Reserved Frames section is removed.
- [x] Appointment refresh/reschedule/cancel/rating behavior remains unchanged.
- [x] NavGraph no longer passes reservation callbacks to Appointment Detail.

**Verification:**

- [x] `<gradle> testDebugUnitTest --tests "*AppointmentDetailViewModelTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Checkpoint D

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModelTest.kt`

**Estimated scope:** M (4 files)

### Task 17: Remove the appointment-request reservation origin ✅

**Description:** Delete the obsolete route flag and success-state plumbing created solely to return
from reservation booking.

**Acceptance criteria:**

- [x] `RequestAppointment` has no reservation-origin property.
- [x] RequestAppointmentScreen and ViewModel submit/create ordinary request success state only.
- [x] NavGraph creates RequestAppointment with no reservation-specific argument.
- [x] Request creation, identity gating, alternatives, and terminal navigation remain unchanged.

**Verification:**

- [x] `<gradle> testDebugUnitTest --tests "*RequestAppointmentViewModelTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 16

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModelTest.kt`

**Estimated scope:** M (5 files)

### Task 18: Remove reservation feature intents and access rules ✅

**Description:** Delete limited-account restoration concepts that no longer have a valid feature.

**Acceptance criteria:**

- [x] PatientFeatureIntent has no reservation list/detail/create variants or `reservations` label.
- [x] PatientRouteAccess has no active-link FrameReservation classification.
- [x] Frame catalog, Saved Frames, request, Chat, and Notifications remain account-only.
- [x] Protected clinical routes still fail closed and intent restoration tests remain exhaustive.

**Verification:**

- [x] `<gradle> testDebugUnitTest --tests "*PatientFeatureIntentTest" --tests "*PatientRouteAccessTest"`
  passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 17

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientFeatureIntent.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientRouteAccess.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt`

**Estimated scope:** M (4 files)

### Task 19: Remove reservation destinations from typed navigation ✅

**Description:** Make all reservation presentation files unreachable before deleting them.

**Acceptance criteria:**

- [x] Routes contains no CreateFrameReservation, FrameReservationList, or FrameReservationDetail.
- [x] NavGraph contains no reservation composable/import/callback/navigation block.
- [x] Saved Frames, Frame Detail, AR, appointment requests, and Profile routes still compile.
- [x] Bottom-navigation roots remain Home, Frames, Appointments, and Profile.

**Verification:**

- [x] `rg -n 'CreateFrameReservation|FrameReservationList|FrameReservationDetail' app/src/main/java/com/eyecare/app/presentation/navigation` returns no hits.
- [x] `<gradle> testDebugUnitTest --tests "*BottomNavVisibilityTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 18

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

**Estimated scope:** S (2 files)

### Task 20: Delete the obsolete create-reservation and eligibility slice ✅

**Description:** Remove the now-unreachable appointment-selection/create flow and its tests.

**Acceptance criteria:**

- [x] CreateFrameReservation screen/ViewModel and reservation eligibility helper are deleted.
- [x] Their focused tests are deleted; no other test imports them.
- [x] No active appointment-request path depends on reservation eligibility.

**Verification:**

- [x] All five exact paths are confirmed absent.
- [x] `rg -n 'mergeOutcome|MAX_RESERVATION_ITEMS|isReservationEligible' app/src/main app/src/test` returns no active hits outside files scheduled for later deletion.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 19

**Files deleted:**

- `app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationEligibility.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/FrameReservationEligibilityTest.kt`

**Estimated scope:** M (5 deletions)

### Task 21: Delete the obsolete reservation list and instrumented detail test ✅

**Description:** Remove the unreachable list destination and the instrumented test whose target is
scheduled for deletion next.

**Acceptance criteria:**

- [x] Reservation list screen/ViewModel are deleted.
- [x] Old instrumented detail-screen test is deleted before its production target disappears.
- [x] No active source imports the list ViewModel or screen.

**Verification:**

- [x] All three exact paths are confirmed absent.
- [x] `<gradle> assembleDebugAndroidTest` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 20

**Files deleted:**

- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationListViewModel.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/reservations/FrameReservationDetailScreenTest.kt`

**Estimated scope:** S (3 deletions)

### Task 22: Delete reservation detail and presentation behavior ✅

**Description:** Remove the remaining reservation UI/state and its unit tests.

**Acceptance criteria:**

- [x] Detail screen/ViewModel and ReservationPresentation helper are deleted.
- [x] Their unit tests are deleted.
- [x] `presentation/reservations` contains no Kotlin file.

**Verification:**

- [x] `rg --files app/src | rg 'presentation[\\/]reservations'` returns no files.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 21

**Files deleted:**

- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/ReservationPresentation.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/FrameReservationDetailViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/ReservationPresentationTest.kt`

**Estimated scope:** M (5 deletions)

### Task 23: Delete reservation data-layer tests ✅

**Description:** Remove tests for the dead DTO/repository immediately before deleting their production
targets.

**Acceptance criteria:**

- [x] Reservation DTO and repository implementation tests are deleted.
- [x] No remaining test instantiates FrameReservationRepositoryImpl or decodes FrameReservationDtos.

**Verification:**

- [x] Both exact paths are confirmed absent.
- [x] `<gradle> testDebugUnitTest` passes before production data deletion.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 22

**Files deleted:**

- `app/src/test/java/com/eyecare/app/data/remote/dto/FrameReservationDtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/FrameReservationRepositoryImplTest.kt`

**Estimated scope:** S (2 deletions)

### Task 24: Delete the reservation remote/repository/DI layer ✅

**Description:** Remove all production declarations capable of calling the retired endpoints.

**Acceptance criteria:**

- [x] FrameReservation Retrofit service, DTOs, repository implementation, and Hilt module are deleted.
- [x] Production Retrofit discovery has no `frame-reservations` annotation.
- [x] No DI binding or implementation import remains.

**Verification:**

- [x] All four exact paths are confirmed absent.
- [x] `rg -n 'frame-reservations|FrameReservationApiService|FrameReservationRepositoryImpl' app/src/main` returns no hits.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 23

**Files deleted:**

- `app/src/main/java/com/eyecare/app/data/remote/api/FrameReservationApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/FrameReservationDtos.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FrameReservationRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/FrameReservationModule.kt`

**Estimated scope:** S (4 deletions)

### Task 25: Delete reservation domain declarations ✅

**Description:** Remove the final unused reservation model, error, and repository interface.

**Acceptance criteria:**

- [x] FrameReservation, FrameReservationError, and FrameReservationRepository are deleted.
- [x] No active production or test source references any FrameReservation symbol.
- [x] Frame and SavedFrame domain models remain independent and compile.

**Verification:**

- [x] `rg -n 'FrameReservation|ReservationAppointment|MAX_RESERVATION_ITEMS' app/src/main app/src/test app/src/androidTest` returns no hits.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 24

**Files deleted:**

- `app/src/main/java/com/eyecare/app/domain/model/FrameReservation.kt`
- `app/src/main/java/com/eyecare/app/domain/model/FrameReservationError.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/FrameReservationRepository.kt`

**Estimated scope:** S (3 deletions)

### Task 26: Move route governance to the 59-route contract ✅

**Description:** Flip the allowlist only after production reservation Retrofit declarations are gone.

**Acceptance criteria:**

- [x] Counts are exactly 8 public, 40 account-only, 11 active-link, and 59 total.
- [x] All three Saved Frame routes and attachment download are account-only.
- [x] All five reservation routes are rejected.
- [x] Production Retrofit discovery contains every approved route and no rejected route.
- [x] Attachment upload runtime capability tests remain green.

**Verification:**

- [x] `<gradle> testDebugUnitTest --tests "*ApiRouteAllowlistTest" --tests "*MessageBubbleAttachmentTest"`
  passes.
- [x] Static route search returns Saved Frames and no Frame Reservations in production API services.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 25

**Files:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** M (2 files)

> ### Checkpoint E — Coordinated cutover (after Tasks 16–26)
>
> - [x] Appointment and request tests prove all reservation coupling is gone.
> - [x] No reservation presentation, data, domain, DI, route, or active test file remains.
> - [x] Production Retrofit discovery rejects every former reservation path.
> - [x] Route governance passes at 8/40/11/59 and attachment download is account-only.
> - [x] Saved Frames, frame, AR, Profile, navigation, and messaging capability tests remain green.
> - [x] `assembleDebug` passes.

---

## Phase 5 — Lifecycle, Documentation, and Final Verification

### Task 27: Reconcile retained saved state on resume ✅

**Description:** Refresh only the screens that can become stale beneath another mutation destination,
using one small lifecycle-aware helper or equivalent testable hooks.

**Acceptance criteria:**

- [x] Returning from Frame Detail refreshes Frames; returning from AR refreshes Frame Detail; returning
  from detail refreshes Saved Frames.
- [x] AR foreground resume can reconcile externally changed variant state without creating duplicate
  loads or disturbing active camera/render state.
- [x] Lifecycle observers are removed on dispose and do not trigger while unauthenticated/off-screen.
- [x] No process-global saved-state singleton or Room persistence is introduced.

**Verification:**

- [x] Existing FrameListViewModel, FrameDetailViewModel, ArViewModel, and SavedFramesViewModel refresh
  tests pass after the lifecycle wiring.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Checkpoint E

**Files:**

- `app/src/main/java/com/eyecare/app/presentation/common/RefreshOnResumeEffect.kt`
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/frames/FrameDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/frames/SavedFramesScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt`

**Estimated scope:** M (5 files)

### Task 28: Reconcile current living documentation ✅

**Description:** Update only current client truth and workflow pointers after behavior is verified.

**Acceptance criteria:**

- [x] `CONTEXT.md` describes account-only Saved Frames, preference semantics, list/detail/AR behavior,
  59 routes, account-owned attachment download, and no appointment-bound reservation feature.
- [x] Active feature summaries and route governance use Saved Frames terminology.
- [x] `AGENTS.md` current-work pointer targets v20.
- [x] API contract retains the Task 1 corrections and all user backend updates.

**Verification:**

- [x] Scoped searches find current Saved Frames/59-route wording and no active reservation section in
  `CONTEXT.md`.
- [x] `git diff --check -- CONTEXT.md docs/API_CONTRACT.md AGENTS.md` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 27

**Files:**

- `CONTEXT.md`
- `docs/API_CONTRACT.md`
- `AGENTS.md`

**Estimated scope:** M (3 files)

### Task 29: Prepare the evidence-backed workflow record

**Description:** Record completion evidence for Tasks 1–28 while leaving final status explicitly
pending until the full Task 30 gate passes.

**Acceptance criteria:**

- [ ] Completed task/acceptance/checkpoint boxes through Task 28 reflect commands that actually passed.
- [ ] Spec, plan, and tasks remain marked implementation/verification in progress.
- [ ] Open Questions record any unresolved implementation blocker before the final gate.
- [ ] No historical v19 or older spec is rewritten.

**Verification:**

- [ ] `git diff --check -- docs/specs/backend-alignment-v20-2026-08-27-*.md` passes.
- [ ] Status/checklist text matches the final verification record exactly.
- [ ] `<gradle> assembleDebug` passes.

**Dependencies:** Task 28; final completion wording waits for Task 30

**Files:**

- `docs/specs/backend-alignment-v20-2026-08-27-spec.md`
- `docs/specs/backend-alignment-v20-2026-08-27-plan.md`
- `docs/specs/backend-alignment-v20-2026-08-27-tasks.md`

**Estimated scope:** S (3 files)

### Task 30: Run the final gate and close the workflow

**Description:** Execute the full project commands, evidence every v20 success criterion, and only then
mark the v20 workflow complete. Fixes are not silently folded into this task; any failure reopens the
owning task and its focused test.

**Acceptance criteria:**

- [ ] No Gson, token/health/Saved Frame Room storage, new dependency, backend behavior, or Kotlin Android
  plugin was introduced.
- [ ] No active source/test route or UI copy uses the retired reservation feature.
- [ ] Saved state is account-only, cache-safe, exact-variant, single-flight, and patient-safe.
- [ ] Unrelated working-tree changes remain intact.
- [ ] After every command passes, spec/plan/tasks statuses and the remaining final checkboxes are marked
  complete with the actual verification result.

**Verification:**

- [ ] `<gradle> ktlintFormat` completes.
- [ ] `<gradle> testDebugUnitTest` passes.
- [ ] `<gradle> lintDebug` passes.
- [ ] `<gradle> assembleDebug` passes after formatting and tests.
- [ ] `<gradle> assembleDebugAndroidTest` passes; `connectedDebugAndroidTest` is run when a device is
  available or explicitly reported as not run.
- [ ] `rg -n -i 'frame-reservations|FrameReservation|Reserve this frame|isFrameReservationOrigin' app/src/main app/src/test app/src/androidTest` returns no active hits.
- [ ] `rg -n 'saved-frames|is_saved' app/src/main app/src/test CONTEXT.md docs/API_CONTRACT.md` shows the
  intended route/field/docs coverage.
- [ ] `git diff --check` passes and final scoped diff review finds no accidental user-file overwrite.

**Dependencies:** Tasks 1–29

**Files:**

- `docs/specs/backend-alignment-v20-2026-08-27-spec.md`
- `docs/specs/backend-alignment-v20-2026-08-27-plan.md`
- `docs/specs/backend-alignment-v20-2026-08-27-tasks.md`

**Estimated scope:** M (verification)

> ### Checkpoint F — Final gate (after Task 30)
>
> - [ ] All 16 spec success criteria have evidence.
> - [ ] Unit, lint, debug build, and Android-test compilation pass.
> - [ ] Instrumented execution result is recorded honestly.
> - [ ] Documentation and workflow status match shipped behavior.
> - [ ] Unrelated user changes are preserved.

## Dependency Summary

```text
1 → 2 → 3 → 4 → 5 → 6
                    ├→ 7 → 9 → 10 → 11
                    └→ 8 ────────┘
11 → 12 → 13
      └→ 14 → 15
15 → 16 → 17 → 18 → 19 → 20 → 21 → 22 → 23 → 24 → 25 → 26
26 → 27 → 28 → 29 → 30
```

Tasks 7 and 8 may be prepared independently after Task 6. Tasks 12 and 14 may be prepared
independently after Checkpoint C, but their NavGraph/UI wiring in Tasks 13 and 15 remains sequential.
No task may cross a checkpoint until all earlier acceptance and build checks are green.

## Risk-to-Task Traceability

| Risk | Owning tasks |
|---|---|
| Account-specific saved state leaks through Room | 3, 30 |
| Saved response or money/AR shape drifts | 2, 4–6 |
| Bodyless idempotent routes are declared incorrectly | 5–6 |
| Saved list paging/dedupe/removal corrupts state | 8–9 |
| Saved row opens wrong variant | 7, 9–10 |
| Limited account is incorrectly link-gated | 10–11, 26 |
| Rapid mutations race or failure flips state | 12–15 |
| AR renderer/camera regresses | 14–15, 30 |
| Appointment/request still depends on reservations | 16–17 |
| Dead endpoint remains callable | 18–26, 30 |
| Attachment upload opens to unlinked accounts | 26, 30 |
| Retained screens display stale saved state | 27 |
| User backend-document edits are overwritten | 1, 28–30 |

## Open Questions

1. **Approval gate:** Approve this task ordering and file ownership so Phase 4 implementation can
   begin. Once approved, execute Task 1 first and do not batch unchecked tasks together.
