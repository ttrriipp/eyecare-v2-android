# Tasks: Backend Alignment V14 — My Eyewear Estimates and Orders

Status: Phase 3 draft — awaiting approval

Approved inputs:

- `docs/specs/backend-alignment-v14-my-eyewear-spec.md`
- `docs/specs/backend-alignment-v14-my-eyewear-plan.md`

## Execution Rules

1. Execute tasks in dependency order unless a listed parallel opportunity is
   explicitly authorized.
2. Begin each behavior change with a focused failing test where practical,
   then implement the minimum behavior needed to pass it.
3. Keep each task within its declared files. If implementation discovers a
   dependency that would exceed five files, update this document and obtain
   approval before expanding that task.
4. Inspect `git status --short` and relevant diffs before every task. Preserve
   the user-owned auth UI, backend-document, `.opencode`, and `.claude` changes
   currently present in the working tree.
5. Never reset, checkout, overwrite, delete, stage, or reformat unrelated
   changes.
6. Use Kotlinx Serialization only. DTOs remain in the data layer and map to
   serialization-free domain models at the repository boundary.
7. Use `BigDecimal` and `MoneyValueSerializer` for every Estimate, Order, and
   Payment Summary amount. Never use `Double` or `Float` for money.
8. Keep Estimates and Orders independently paginated. Never join, merge,
   deduplicate, or reclassify records across their APIs.
9. Treat backend filter membership, ordering, cross-link IDs,
   `is_rateable`, payment status, and overdue state as authoritative.
10. Do not add a dependency, Gradle plugin, Room migration, backend change,
    patient mutation, or compatibility layer without separate approval.
11. Do not store Estimates, Orders, messages, financial/clinical data, or
    tokens in Room.
12. Run `assembleDebug` after every task that changes production code.
13. Do not leave a checkpoint with a known failing test or build. Diagnose and
    resolve the failure before advancing.
14. Update the approved specification before implementing any discovered
    contract or product decision that conflicts with it.
15. Do not commit automatically. Commit only when explicitly requested.

Set Android Studio's JBR before Gradle commands:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Phase A — Baseline and Estimate Vertical

### Task 1: Establish the protected baseline

- [ ] Record the current dirty paths and confirm the V14 documentation is the
      only authorized work from the specification phases.
- [ ] Run the current affected unit tests and debug assembly before production
      edits.
- [ ] Record any pre-existing failure without modifying unrelated code.

Acceptance:

- The implementation starts from a known build/test state.
- User-owned paths are explicitly separated from V14 paths.
- No production or test file changes in this task.

Verify:

```powershell
git status --short
.\gradlew testDebugUnitTest --tests "*Quotation*" --tests "*Eyewear*" --tests "*JobOrder*" --tests "*FrameRating*" --tests "*Chat*" --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebug
```

Dependencies: approved Phase 3 task list.

Files: none.

### Task 2: Replace the Quotation wire and domain models

- [ ] Write Kotlinx fixtures for the flat Quotation response, exact money,
      product/service item types, pagination, nullable dates/notes, and nullable
      `optical_order`.
- [ ] Replace the revision-based DTO/domain shape with the current flat
      contract.
- [ ] Add fail-closed status/item-type handling and remove all Quotation money
      `Double` values.

Acceptance:

- Every documented field decodes and money retains decimal precision.
- Unknown status/item values are non-actionable and never become Presented.
- `QuotationRevision` no longer exists in the domain contract.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*QuotationDtosTest"
.\gradlew assembleDebug
```

Dependencies: Task 1.

Files:

- `app/src/main/java/com/eyecare/app/data/remote/dto/QuotationDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Quotation.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/QuotationDtosTest.kt` (new)

### Task 3: Align the Quotation API and repository boundary

- [ ] Add `filter`, `page`, and `per_page` to the list service/repository
      contract.
- [ ] Map every flat DTO field, typed cross-link, enum, exact amount, and
      pagination value at the repository boundary.
- [ ] Verify Current/History query values, server order preservation, detail
      IDs, null links, and errors with focused tests.

Acceptance:

- Presentation receives domain Quotations only.
- List calls preserve backend pagination and ordering.
- Detail passes the typed integer ID unchanged.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*QuotationRepositoryImplTest"
.\gradlew assembleDebug
```

Dependencies: Task 2.

Files:

- `app/src/main/java/com/eyecare/app/data/remote/api/QuotationApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/QuotationRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/QuotationRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/QuotationRepositoryImplTest.kt`

### Checkpoint A1 — Estimate contract

- [ ] Quotation DTO and repository tests pass.
- [ ] `rg` finds no `Double` in Quotation DTO/domain/repository files.
- [ ] Debug assembly passes.
- [ ] No aggregate Eyewear behavior has been removed yet.

### Task 4: Add independent Estimate list state

- [ ] Add an Estimate list ViewModel with Current as its default filter.
- [ ] Cover initial load, empty, error/retry, refresh, append, append failure,
      duplicate-load guard, filter reset, and stale-response suppression.
- [ ] Keep all state independent from Optical Orders.

Acceptance:

- Switching Current/History restarts only Estimate pagination at page 1.
- Previously loaded cards are retained on append failure.
- No Optical Order repository or aggregate type is referenced.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*EstimateListViewModelTest"
.\gradlew assembleDebug
```

Dependencies: Task 3.

Files:

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EstimateListViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EstimateListViewModelTest.kt` (new)

### Task 5: Build the Estimate list presentation

- [ ] Add Estimate list content with Current/History controls and all list
      states.
- [ ] Implement pure Estimate status, title, date, and exact-money formatting.
- [ ] Show **View order** only for accepted Estimates with an actual
      `optical_order` reference.

Acceptance:

- Cards use patient-facing terminology and accurate date fallback labels.
- Empty/error/load-more copy is Estimate-specific.
- The UI emits typed Estimate and Order IDs but performs no repository call.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*EyewearPresentationTest" --tests "*EstimateList*"
.\gradlew assembleDebug
```

Dependencies: Task 4.

Files:

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EstimateListScreen.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearPresentation.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearPresentationTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/EstimateListScreenTest.kt` (new, if Android-test infrastructure is available)

### Task 6: Build the read-only Estimate detail

- [ ] Add typed detail loading/retry state for one Quotation ID.
- [ ] Render items, subtotal, discount, total, accurate dates, optional notes,
      and optional Order cross-link.
- [ ] Prove no accept/decline or order-creation control exists.

Acceptance:

- Detail calls only `GET /quotations/{quotation}`.
- Missing notes/link produce no empty section/action.
- Cross-link emits only the referenced Optical Order ID after a patient tap.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*EstimateDetailViewModelTest"
.\gradlew assembleDebug
```

Dependencies: Tasks 3 and 5.

Files:

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EstimateDetailViewModel.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EstimateDetailScreen.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EstimateDetailViewModelTest.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/EstimateDetailScreenTest.kt` (new, if Android-test infrastructure is available)

### Checkpoint A2 — Complete Estimate slice

- [ ] Estimate Current/History and detail tests pass.
- [ ] Estimate screens are read-only and cross-link by typed ID only.
- [ ] Exact money and unknown status policy tests pass.
- [ ] Debug assembly passes.

## Phase B — Optical Order and Rating Vertical

### Task 7: Add Optical Order wire and domain models

- [ ] Write fixtures for complete/nullable list and detail responses, exact
      money, source Estimate, product items, rating summaries, Payment Summary,
      fulfillment mode, and every enum value.
- [ ] Add serialization-only DTOs and plain domain models.
- [ ] Map unknown Order, fulfillment, and payment values to explicit UNKNOWN.

Acceptance:

- All documented nullable combinations decode safely.
- Payment and fulfillment states remain separate.
- Item `is_rateable` and rating summary are represented without inference.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderDtosTest"
.\gradlew assembleDebug
```

Dependencies: Task 1. May run in parallel with Tasks 2–6 after shared money
conventions are agreed.

Files:

- `app/src/main/java/com/eyecare/app/data/remote/dto/OpticalOrderDtos.kt` (new)
- `app/src/main/java/com/eyecare/app/domain/model/OpticalOrder.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/dto/OpticalOrderDtosTest.kt` (new)

### Task 8: Add the Optical Order API and repository boundary

- [ ] Declare only the list/detail Optical Order GET routes in this task.
- [ ] Add the domain repository and DTO-to-domain implementation.
- [ ] Test filters, pagination, typed detail IDs, exact mapping, ordering, null
      summaries/links, and error propagation.

Acceptance:

- No Job Order or aggregate API/type is used by the new repository.
- Both list filters send exact contract query values.
- No DTO escapes the repository boundary.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderRepositoryImplTest"
.\gradlew assembleDebug
```

Dependencies: Task 7.

Files:

- `app/src/main/java/com/eyecare/app/data/remote/api/OpticalOrderApiService.kt` (new)
- `app/src/main/java/com/eyecare/app/domain/repository/OpticalOrderRepository.kt` (new)
- `app/src/main/java/com/eyecare/app/data/repository/OpticalOrderRepositoryImpl.kt` (new)
- `app/src/test/java/com/eyecare/app/data/repository/OpticalOrderRepositoryImplTest.kt` (new)

### Checkpoint B1 — Order contract

- [ ] Optical Order DTO and repository tests pass.
- [ ] Money is exact and status mappings fail closed.
- [ ] No retired Retrofit route was introduced.
- [ ] Debug assembly passes.

### Task 9: Bind Optical Orders and add independent list state

- [ ] Bind the new API and repository through Hilt.
- [ ] Add an Order list ViewModel with Current as default.
- [ ] Cover the same independent pagination/race/error matrix as Estimates.

Acceptance:

- Hilt resolves the Optical Order repository.
- Order filter/page state never reads or mutates Estimate state.
- Initial construction requests only Orders Current for this ViewModel.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderListViewModelTest"
.\gradlew assembleDebug
```

Dependencies: Task 8.

Files:

- `app/src/main/java/com/eyecare/app/di/OpticalOrderModule.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderListViewModel.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/eyewear/OpticalOrderListViewModelTest.kt` (new)

### Task 10: Build the Optical Order list presentation

- [ ] Add Order list content with Current/History controls and all list states.
- [ ] Add pure Order/fulfillment/payment/title/date/balance formatting policy.
- [ ] Keep payment status visually and semantically separate from progress.

Acceptance:

- Status labels match the approved patient language.
- Remaining balance appears only when supplied and greater than zero.
- Overdue comes only from `is_overdue` and never changes filter membership.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*EyewearPresentationTest" --tests "*OpticalOrderList*"
.\gradlew assembleDebug
```

Dependencies: Task 9.

Files:

- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderListScreen.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearPresentation.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearPresentationTest.kt`

### Task 11: Build the read-only Optical Order detail

- [ ] Add typed detail loading/retry state for one Order ID.
- [ ] Render the fulfillment tracker, actual timestamps, Eyewear Details,
      optional Payment Summary, and optional source Estimate link.
- [ ] Cover queued/in-progress/ready/released/cancelled/unknown tracker states.

Acceptance:

- Order detail calls only `GET /optical-orders/{id}`.
- No expected completion, payment history, or missing section is invented.
- Source Estimate navigation occurs only after a tap on an existing link.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderDetailViewModelTest" --tests "*EyewearPresentationTest"
.\gradlew assembleDebug
```

Dependencies: Tasks 8 and 10.

Files:

- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailViewModel.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreen.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailViewModelTest.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreenTest.kt` (new, if Android-test infrastructure is available)

### Checkpoint B2 — Read-only Order slice

- [ ] Order Current/History and detail tests pass.
- [ ] Tracker, Payment Summary, and cross-link policies pass.
- [ ] No rating mutation is exposed yet.
- [ ] Debug assembly passes.

### Task 12: Implement Optical Order rating data behavior

- [ ] Extend Optical Order DTO/API/repository contracts with the new POST upsert
      request/response.
- [ ] Send only item ID in the route plus rating/comment body; never send a
      variant or dispensing-event ID.
- [ ] Test 201 creation, 200 revision, 404 privacy behavior, and both documented
      422 cases.

Acceptance:

- Both successful status codes return the current mapped rating result.
- Structured errors preserve safe code/message/field details.
- Rating does not modify Order, fulfillment, or payment state.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*OpticalOrderRepositoryImplTest"
.\gradlew assembleDebug
```

Dependencies: Task 8.

Files:

- `app/src/main/java/com/eyecare/app/data/remote/dto/OpticalOrderDtos.kt`
- `app/src/main/java/com/eyecare/app/data/remote/api/OpticalOrderApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/OpticalOrderRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/OpticalOrderRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/OpticalOrderRepositoryImplTest.kt`

### Task 13: Wire rating into Order detail

- [ ] Rework the rating ViewModel to use `OpticalOrderRepository` and item ID
      only.
- [ ] Enforce 1–5 and 1,000-character local limits while preserving server
      validation authority.
- [ ] Show rating/revision only when backend `is_rateable` is true and update
      the item summary after success.

Acceptance:

- Product variant and Order status are not used as eligibility gates.
- A validation failure preserves detail content and entered input.
- A 201 or 200 success updates the visible current rating without reloading
  unrelated lists.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*FrameRatingViewModelTest" --tests "*OpticalOrderDetailViewModelTest"
.\gradlew assembleDebug
```

Dependencies: Tasks 11 and 12.

Files:

- `app/src/main/java/com/eyecare/app/presentation/eyewear/FrameRatingViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/FrameRatingDialog.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/OpticalOrderDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/FrameRatingViewModelTest.kt` (new)

### Checkpoint B3 — Order rating

- [ ] Rating create/revision/error tests pass.
- [ ] `is_rateable` is the sole UI eligibility source.
- [ ] No request body contains `product_variant_id` or
      `dispensing_event_id`.
- [ ] Debug assembly passes.

## Phase C — My Eyewear and Typed Navigation

### Task 14: Compose the lazy My Eyewear destination

- [ ] Add a coordinator screen titled **My Eyewear** with Estimates and Orders
      primary tabs.
- [ ] Start at Estimates and instantiate only the selected branch's ViewModel.
- [ ] Retain each visited tab's independent filter/list state while the route
      remains alive.

Acceptance:

- Initial entry requests only Estimates Current.
- Selecting Orders does not merge, copy, or clear Estimate results.
- Returning to Estimates restores its prior in-route state.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*MyEyewear*"
.\gradlew assembleDebug
```

Dependencies: Tasks 6, 9, 10, and 11.

Files:

- `app/src/main/java/com/eyecare/app/presentation/eyewear/MyEyewearScreen.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/eyewear/MyEyewearCoordinatorTest.kt` (new)

### Task 15: Add typed My Eyewear routes and access intents

- [ ] Add `MyEyewear`, `EstimateDetail(Int)`, and
      `OpticalOrderDetail(Int)` routes alongside the old aggregate routes.
- [ ] Add typed `PatientFeatureIntent` conversion/restoration and access keys.
- [ ] Prove limited-account intent round trips preserve resource type and ID.

Acceptance:

- Estimate IDs cannot be reconstructed as Order IDs or opaque strings.
- All three routes remain active-link protected.
- Existing aggregate routes compile temporarily until the atomic cutover.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*PatientFeatureIntentTest"
.\gradlew assembleDebug
```

Dependencies: Task 14.

Files:

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientFeatureIntent.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`

### Task 16: Cut navigation and Profile over atomically

- [ ] Register My Eyewear and both detail destinations in `NavGraph` with
      bidirectional cross-links.
- [ ] Change Profile copy/callback to **My Eyewear** and remove visible
      aggregate navigation.
- [ ] Preserve active-link gating, pending-destination restoration, and
      bottom-navigation visibility policy.

Acceptance:

- Every visible entry reaches the new coordinator or typed detail.
- Limited accounts reach the link hub before any protected repository call.
- No visible route navigates to `EyewearDetail(key)`.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*PatientFeatureIntentTest" --tests "*Profile*" --tests "*Eyewear*"
.\gradlew assembleDebug
```

Dependencies: Task 15.

Files:

- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt` (new or existing)
- `app/src/androidTest/java/com/eyecare/app/presentation/navigation/MyEyewearNavigationTest.kt` (new, if Android-test infrastructure is available)

### Checkpoint C — My Eyewear cutover

- [ ] One My Eyewear entry exposes separate Estimates and Orders.
- [ ] Initial/lazy and independent-state assertions pass.
- [ ] Typed cross-links and limited-account restoration pass.
- [ ] No visible aggregate route remains.
- [ ] Debug assembly passes.

## Phase D — Messaging Contract and Context Migration

### Task 17: Align Message wire and domain models

- [ ] Update decoding fixtures to remove required `conversation_id` and add
      `sender_type` plus attachment `download_url`.
- [ ] Add patient/staff/unknown sender mapping and Quotation/Optical
      Order/Unsupported context domain values.
- [ ] Preserve non-null body and zero-or-one attachment behavior.

Acceptance:

- Current contract messages decode without legacy fields.
- Unknown sender/context values fail safely.
- Appointment and old Order are not valid actionable context variants.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*MessageDtosTest"
.\gradlew assembleDebug
```

Dependencies: Task 1. May run in parallel with Phases A/B before repository
consumers change.

Files:

- `app/src/main/java/com/eyecare/app/data/remote/dto/MessageDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Message.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/MessageDtosTest.kt`

### Task 18: Remove DTO leakage from ChatRepository

- [ ] Change send-message context parameters to domain references.
- [ ] Map domain contexts to DTOs only inside `ChatRepositoryImpl`.
- [ ] Map sender, attachment URL, valid contexts, and Unsupported context from
      response DTOs.

Acceptance:

- The domain repository imports no data-layer DTO.
- Outbound types are exactly `quotation` and `optical_order`.
- Mapping tests cover patient/staff/unknown and supported/unknown contexts.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*ChatRepositoryMappingsTest"
.\gradlew assembleDebug
```

Dependencies: Task 17.

Files:

- `app/src/main/java/com/eyecare/app/domain/repository/ChatRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ChatRepositoryMappingsTest.kt`

### Checkpoint D1 — Message contract

- [ ] Current response fixtures decode.
- [ ] Domain repository has no DTO imports.
- [ ] Only contract-valid contexts serialize.
- [ ] Existing text/file send and attachment download mapping still pass.

### Task 19: Replace Chat picker state with Estimates and Orders

- [ ] Remove Appointment and Job Order picker dependencies/state.
- [ ] Load Estimates and Optical Orders independently, including page/load-more
      state so older records remain reachable.
- [ ] Scope picker failures to their source without failing Chat or the other
      picker source.

Acceptance:

- Chat injects Quotation and Optical Order repositories only for context data.
- Estimate/Order picker pages never merge or share pagination.
- Context messages use patient-friendly body text and domain references.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*ChatViewModelTest"
.\gradlew assembleDebug
```

Dependencies: Tasks 3, 8, and 18.

Files:

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`

### Task 20: Rework the context picker UI

- [ ] Replace **Link appointment** and old **Link order** choices with
      **Link estimate** and **Link eyewear order**.
- [ ] Render independent loading, empty, error/retry, and load-more states.
- [ ] Update pending-context preview copy and callbacks to typed domain values.

Acceptance:

- The picker exposes only backend-valid context types.
- Older pages are reachable without bulk-fetching all records.
- A failure in one picker section does not remove the other.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*ChatViewModelTest"
.\gradlew assembleDebug
```

Dependencies: Task 19.

Files:

- `app/src/main/java/com/eyecare/app/presentation/messaging/components/AttachmentSheet.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/ContextCard.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/messaging/AttachmentSheetTest.kt` (new, if Android-test infrastructure is available)

### Task 21: Wire incoming message contexts to typed details

- [ ] Render Quotation and Optical Order context cards with patient-friendly
      titles and typed callbacks.
- [ ] Render Unsupported context as a neutral non-clickable reference.
- [ ] Replace Appointment callback plumbing through Chat and `NavGraph` with
      Estimate/Order detail callbacks.

Acceptance:

- A valid context tap reaches the matching typed detail route.
- Unknown types remain visible and cannot be misrouted.
- Bubble ownership uses `sender_type`, not a local user-ID comparison.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*Chat*" --tests "*PatientFeatureIntentTest"
.\gradlew assembleDebug
```

Dependencies: Tasks 16, 18, and 20.

Files:

- `app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageContextCard.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageBubble.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/MessageContextPresentationTest.kt` (new)

### Checkpoint D2 — Messaging cutover

- [ ] Chat creates and renders only Quotation/Optical Order contexts.
- [ ] Both picker sources paginate and fail independently.
- [ ] Incoming valid/unknown contexts and sender types render safely.
- [ ] Polling, text sending, single-file upload, and download regressions pass.
- [ ] Debug assembly passes.

## Phase E — Legacy Retirement and Contract Governance

### Task 22: Remove aggregate Eyewear data-layer files

- [ ] Prove no production consumer imports the aggregate service, DTO, or
      repository implementation.
- [ ] Delete aggregate data files and their focused data tests.
- [ ] Confirm no `/eyewear` Retrofit annotation remains.

Acceptance:

- Aggregate data symbols and endpoints are absent.
- New Estimate/Order repository tests remain green.
- Debug assembly passes after deletion.

Verify:

```powershell
if (rg -n 'EyewearApiService|EyewearDtos|EyewearRepositoryImpl|@GET\("eyewear' app/src) { throw 'Aggregate data references remain' }
.\gradlew testDebugUnitTest --tests "*Quotation*" --tests "*OpticalOrder*"
.\gradlew assembleDebug
```

Dependencies: Tasks 16 and 21.

Files removed:

- `app/src/main/java/com/eyecare/app/data/remote/api/EyewearApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/EyewearDtos.kt`
- `app/src/main/java/com/eyecare/app/data/repository/EyewearRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/EyewearDtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/EyewearRepositoryImplTest.kt`

### Task 23: Remove aggregate Eyewear presentation files

- [ ] Prove `NavGraph` and all callbacks use My Eyewear and typed details.
- [ ] Delete superseded aggregate list/detail screens, ViewModels, and the old
      aggregate list ViewModel test.
- [ ] Retain the repurposed pure `EyewearPresentation` policy used by new
      Estimate/Order screens.

Acceptance:

- No opaque aggregate key or aggregate UI state remains.
- My Eyewear and both typed details compile and pass focused tests.
- Debug assembly passes.

Verify:

```powershell
if (rg -n 'EyewearListUiState|EyewearDetailUiState|EyewearDetail\(val key' app/src) { throw 'Aggregate presentation references remain' }
.\gradlew testDebugUnitTest --tests "*Eyewear*" --tests "*Estimate*" --tests "*OpticalOrder*"
.\gradlew assembleDebug
```

Dependencies: Task 22.

Files removed:

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearListViewModelTest.kt`

### Task 24: Remove aggregate Eyewear domain and DI files

- [ ] Delete aggregate domain models, repository interface, and Hilt module.
- [ ] Prove no `EyewearRepository` or aggregate domain import remains.
- [ ] Keep the public package/destination term My Eyewear.

Acceptance:

- Hilt graph contains only Quotation and Optical Order feature bindings.
- No aggregate domain type is reachable.
- Debug assembly passes.

Verify:

```powershell
if (rg -n 'domain\.model\.Eyewear|EyewearRepository|EyewearModule' app/src) { throw 'Aggregate domain references remain' }
.\gradlew assembleDebug
```

Dependencies: Task 23.

Files removed:

- `app/src/main/java/com/eyecare/app/domain/model/Eyewear.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/EyewearRepository.kt`
- `app/src/main/java/com/eyecare/app/di/EyewearModule.kt`

### Checkpoint E1 — Aggregate retired

- [ ] No aggregate endpoint, key, data, domain, UI state, or Hilt binding
      remains.
- [ ] New My Eyewear focused tests pass.
- [ ] Debug assembly passes.

### Task 25: Remove legacy Job Order data files

- [ ] Prove My Eyewear, rating, and Chat consume Optical Orders only.
- [ ] Delete the old Job Order service, DTO, implementation, and repository
      tests, including obsolete rating repository tests.
- [ ] Confirm no `/job-orders` or `/job-order-items` annotation remains.

Acceptance:

- No production call targets a Job Order route.
- Optical Order and rating tests remain green.
- Debug assembly passes.

Verify:

```powershell
if (rg -n 'job-orders|job-order-items|JobOrderApiService|JobOrderDtos|JobOrderRepositoryImpl' app/src) { throw 'Job Order data references remain' }
.\gradlew testDebugUnitTest --tests "*OpticalOrder*" --tests "*FrameRatingViewModelTest"
.\gradlew assembleDebug
```

Dependencies: Tasks 13 and 21.

Files removed:

- `app/src/main/java/com/eyecare/app/data/remote/api/JobOrderApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/JobOrderDtos.kt`
- `app/src/main/java/com/eyecare/app/data/repository/JobOrderRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/JobOrderRepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/FrameRatingRepositoryTest.kt`

### Task 26: Remove legacy Job Order domain and DI files

- [ ] Delete old Job Order models, repository interface, and Hilt module.
- [ ] Prove patient-facing code uses Optical Order terminology/types.
- [ ] Preserve clinic-internal terminology only in historical documentation.

Acceptance:

- Hilt resolves no Job Order binding.
- `JobOrder` symbols are absent from Android production/test sources.
- Debug assembly passes.

Verify:

```powershell
if (rg -n 'JobOrder' app/src) { throw 'Job Order symbols remain' }
.\gradlew assembleDebug
```

Dependencies: Task 25.

Files removed:

- `app/src/main/java/com/eyecare/app/domain/model/JobOrder.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/JobOrderRepository.kt`
- `app/src/main/java/com/eyecare/app/di/JobOrderModule.kt`

### Task 27: Remove retired Billing Record data files

- [ ] Reconfirm Billing Record has no presentation or Payment Summary consumer.
- [ ] Delete the retired service, DTO, implementation, and focused data tests.
- [ ] Confirm no `/billing-records` Retrofit annotation remains.

Acceptance:

- Order Payment Summary remains sourced only from Optical Orders.
- No Billing Record data symbol or route remains.
- Debug assembly passes.

Verify:

```powershell
if (rg -n 'billing-records|BillingRecordApiService|BillingRecordDtos|BillingRecordRepositoryImpl' app/src) { throw 'Billing Record data references remain' }
.\gradlew testDebugUnitTest --tests "*OpticalOrder*"
.\gradlew assembleDebug
```

Dependencies: Task 11.

Files removed:

- `app/src/main/java/com/eyecare/app/data/remote/api/BillingRecordApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/BillingRecordDtos.kt`
- `app/src/main/java/com/eyecare/app/data/repository/BillingRecordRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/BillingRecordDtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/BillingRecordRepositoryImplTest.kt`

### Task 28: Remove retired Billing Record domain and DI files

- [ ] Delete Billing Record domain models, repository interface, and Hilt
      module.
- [ ] Prove no patient-facing code imports Billing Record.
- [ ] Preserve Payment Summary as an Optical Order submodel only.

Acceptance:

- Hilt resolves no Billing Record binding.
- Billing Record symbols are absent from Android sources.
- Debug assembly passes.

Verify:

```powershell
if (rg -n 'BillingRecord' app/src) { throw 'Billing Record symbols remain' }
.\gradlew assembleDebug
```

Dependencies: Task 27.

Files removed:

- `app/src/main/java/com/eyecare/app/domain/model/BillingRecord.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/BillingRecordRepository.kt`
- `app/src/main/java/com/eyecare/app/di/BillingRecordModule.kt`

### Task 29: Remove superseded rating transport/domain files

- [ ] Prove the new Optical Order DTO/domain owns all rating summaries/results.
- [ ] Delete the old moderation-history rating DTO and domain model.
- [ ] Confirm no patient path expects full revision history.

Acceptance:

- Rating create/revision still works through the new compact response.
- No old variant/dispensing-event request field remains.
- Debug assembly passes.

Verify:

```powershell
if (rg -n 'FrameRatingDtos|FrameRatingRevision|dispensingEventId' app/src) { throw 'Superseded rating contract remains' }
.\gradlew testDebugUnitTest --tests "*OpticalOrder*" --tests "*FrameRatingViewModelTest"
.\gradlew assembleDebug
```

Dependencies: Tasks 13 and 25.

Files removed:

- `app/src/main/java/com/eyecare/app/data/remote/dto/FrameRatingDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/FrameRating.kt`

### Checkpoint E2 — Operational legacy retired

- [ ] No aggregate Eyewear, Job Order, Billing Record, or old rating symbols or
      Retrofit routes remain.
- [ ] Estimate, Order, rating, and Chat focused tests pass.
- [ ] Hilt graph and debug assembly pass.

### Task 30: Enforce the exact 51-route contract

- [ ] Rebuild route sets as 8 public, 24 account-only, and 19 active-link;
      move every coordinated removed route to rejected.
- [ ] Make discovery fail whenever a rejected production Retrofit annotation
      exists and normalize new Optical Order/rating variables.

Acceptance:

- `allApproved.size == 51` and category counts match the backend documents.
- Every discovered production route is approved and none is rejected.

Verify:

```powershell
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebug
```

Dependencies: Tasks 22–29.

Files:

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

### Task 31: Reconcile documentation and complete final verification

- [ ] Update `CONTEXT.md` and all V14 status/decision records to the implemented
      reality.
- [ ] Run formatting, full unit tests, lint, debug assembly, and the manual
      13-case matrix from the approved Phase 2 plan.
- [ ] Record final verification results without staging or committing unrelated
      user-owned changes.

Acceptance:

- Project context documents My Eyewear, messaging contexts, rating, 51-route
  governance, and legacy retirement accurately.
- Spec, plan, tasks, implementation, and authoritative backend documents agree.
- All automated and manual completion gates are satisfied or an exact blocker
  is reported before claiming completion.

Verify:

```powershell
.\gradlew ktlintFormat
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
```

Manual verification: complete the 13-case matrix in the approved Phase 2 plan.

Dependencies: Task 30.

Files:

- `CONTEXT.md`
- `docs/specs/backend-alignment-v14-my-eyewear-spec.md`
- `docs/specs/backend-alignment-v14-my-eyewear-plan.md`
- `docs/specs/backend-alignment-v14-my-eyewear-tasks.md`

## Final Checkpoint — Ready for review

- [ ] Every task acceptance criterion is satisfied.
- [ ] The spec, plan, tasks, API contract, implementation, route governance,
      and `CONTEXT.md` agree.
- [ ] No client-side Estimate/Order join or compatibility layer exists.
- [ ] No patient mutation exists except server-authorized rating upsert.
- [ ] No retired endpoint or legacy feature symbol remains.
- [ ] Full unit tests, ktlint formatting, lint, and debug assembly pass.
- [ ] Manual linked/limited, list/detail, cross-link, rating, and messaging
      matrix passes.
- [ ] Unrelated user-owned working-tree changes remain intact and unstaged.

## Parallelization Map

Safe only with explicit authorization and disjoint file ownership:

- Tasks 2–6 (Estimate) may run beside Tasks 7–13 (Optical Order) after Task 1.
- Task 17 may run beside the Estimate/Order presentation work.
- Presentation tests for Estimates and Orders may run in parallel if
  `EyewearPresentation.kt` ownership is assigned to only one worker.
- Billing reachability preparation may run while messaging is implemented, but
  deletion waits for its declared dependency.

Must remain sequential:

- Tasks 2 → 3 → 4 → 5/6.
- Tasks 7 → 8 → 9/10/11 → 12 → 13.
- Tasks 14 → 15 → 16.
- Tasks 17 → 18 → 19 → 20 → 21.
- Consumer cutovers before Tasks 22–29 deletion.
- All deletions before Task 30 exact route governance.
- Task 30 route governance before Task 31 documentation/final verification.

## Phase Gate

This Phase 3 task breakdown requires project-owner approval. After approval,
Phase 4 may implement the tasks incrementally with test-driven development and
must report checkpoint results. No Android production implementation is
authorized by approval of the specification or plan alone.
