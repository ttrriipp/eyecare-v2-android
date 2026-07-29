# Backend Alignment V12 — Implementation Plan

Status: Approved — Phase 2 complete (2026-07-29); Phase 3 tasks awaiting approval

Specification: `docs/specs/backend-alignment-v12-spec.md`

## Goal

Implement the approved unified patient Eyewear journey against the backend's
read-only aggregate API, switch visible navigation only after the new
experience reaches parity, and then retire obsolete operational presentation
without changing the underlying Quotation, Job Order, Billing Record, or Frame
Rating API/data layers.

This plan authorizes no production changes. After approval, it must be
decomposed into a separate Phase 3 task list and approved again before
implementation begins.

## Planning Constraints

- Current Android V11 is the implementation baseline.
- The user-supplied 2026-07-29 `API_CONTRACT.md` and `BACKEND_CONTEXT.md` are
  read-only implementation inputs.
- The approved mobile API now contains exactly 35 routes.
- The six existing Quotation, Job Order, and Billing Record read routes stay in
  the backend contract.
- Android adds the two Eyewear routes and continues declaring the six existing
  Retrofit services, preserving route discovery equality at 35.
- The new list/detail never joins operational endpoints on the client.
- All aggregate money stays `BigDecimal`.
- Frame Rating remains the only mutation reachable from Eyewear and only for
  eligible dispensed items.
- No dependency, Room schema, backend, or build-plugin change is required.
- Current unrelated working-tree/user changes must be preserved.
- Every green implementation checkpoint ends with
  `.\gradlew assembleDebug`.

## Architecture

### Aggregate data vertical

Add an independent vertical:

```text
EyewearApiService
    -> EyewearDtos
    -> EyewearRepositoryImpl
    -> EyewearRepository
    -> EyewearSummary / EyewearDetail + optional section models
    -> EyewearListViewModel / EyewearDetailViewModel
    -> Eyewear list/detail screens
    -> EyewearList / EyewearDetail(key)
```

The repository exposes:

```kotlin
interface EyewearRepository {
    suspend fun getEyewear(
        filter: EyewearFilter,
        page: Int = 1,
    ): Result<PaginatedResult<EyewearSummary>>

    suspend fun getEyewearDetail(key: String): Result<EyewearDetail>
}
```

`EyewearFilter` is a closed domain enum that serializes to `current` or
`history` at the data boundary. ViewModels do not pass arbitrary filter
strings.

The API service owns only:

```text
GET eyewear?filter=&page=&per_page=
GET eyewear/{key}
```

The six operational service interfaces remain unchanged and therefore the
route allowlist/discovery test continues to model the complete 35-route
backend contract.

### Transport and domain

DTOs mirror the aggregate payload, including nullable/omitted fields:

```text
EyewearSummaryDto
EyewearDetailDto
EyewearEstimateDto
EyewearEstimateItemDto
EyewearPreparationDto
EyewearPreparationItemDto
EyewearDispensingDto
EyewearPaymentSummaryDto
EyewearPaymentDto
EyewearListResponse
EyewearResponse
```

All amounts are annotated with `MoneyValueSerializer`, including nullable
balance. Optional sections use nullable defaults so key omission is distinct
from an empty section.

Repository mapping creates serialization-free domain models and maps raw
status strings through explicit `UNKNOWN` values. It does not parse canonical
keys, sort results, construct sections, or infer linkage.

### List state

`EyewearListViewModel` owns:

```text
selectedFilter
records
initial loading/error/empty
current page / has more
append loading/failure
active request identity/job
```

Current is loaded initially. Changing filter:

1. cancels or invalidates the previous filter request;
2. selects the new filter;
3. resets pagination;
4. clears records from the old filter;
5. loads page 1.

A late response for an old filter cannot replace current state. Page append
preserves backend order and guards concurrent requests. An append failure
retains existing records and exposes retry without decrementing/advancing the
effective page incorrectly.

No client-side progress/payment filtering or sorting is permitted.

### Detail state

`EyewearDetailViewModel` loads exactly one opaque key and owns:

```text
Loading
Success(EyewearDetail)
Error(message)
```

It does not initialize or call the list. Retry reloads the same key. Canonical
`eyw_` and `jo_` alias values pass through unchanged.

Frame Rating state remains separate from detail loading so a rating submission
cannot replace or invalidate aggregate content.

### Presentation policy

Pure/internal functions own:

- progress labels and semantic colors;
- payment labels and semantic colors;
- Consultation versus Created date choice;
- exact peso formatting;
- balance visibility;
- normal/exceptional tracker milestone state;
- conditional section visibility;
- payment-method humanization;
- rating eligibility.

Unknown progress/payment values use neutral labels and enable no rating.
Presentation never directly branches on raw transport strings.

### Frame Rating migration

The existing `JobOrderRepository.submitRating` and rating endpoint remain
unchanged. The current Job Order presentation contains a rating dialog and an
assisted `FrameRatingViewModel`, but its current dialog callback dismisses
without invoking submission. V12 must preserve the intended capability, not
copy that broken wiring.

Implementation approach:

1. add focused `FrameRatingViewModel` tests for validation, request arguments,
   submitting, success, structured failure, and reset;
2. wire the rating dialog to the assisted ViewModel for the selected aggregate
   preparation item;
3. allow selection only when aggregate progress is `DISPENSED` and both item
   IDs exist;
4. keep the dialog open during submission and on error;
5. dismiss or show acknowledgement only after success;
6. move rating presentation from `presentation/joborders` to
   `presentation/eyewear` before deleting the old Job Order presentation.

The aggregate endpoint supplies Job Order item ID and product variant ID, so no
legacy Job Order detail request is needed.

### Navigation and context handling

Add:

```kotlin
@Serializable data object EyewearList
@Serializable data class EyewearDetail(val key: String)
```

When the combined screens are green:

- replace three Profile callbacks/rows with `onNavigateToEyewear`;
- navigate list cards with the canonical key;
- support genuine Job Order entry points as
  `EyewearDetail("jo_$jobOrderId")`;
- hide bottom navigation for both Eyewear destinations.

The current message mapping treats response type `order` as a supported Order
context, while `NavGraph` incorrectly sends that retired Order ID to
`JobOrderDetail`. V12 removes that misleading path:

- Appointment contexts stay supported;
- documented retired Order contexts map to non-interactive unsupported state,
  or otherwise render without a Job Order/Eyewear navigation callback;
- Product/unknown behavior remains safe;
- no `job_order` context is invented without a backend contract.

### Presentation retirement boundary

After Eyewear parity, route tests, Profile tests, message tests, rating tests,
and manual link checks are green:

- remove Quotation list/detail screens and combined ViewModel;
- remove Job Order list/detail screens and combined ViewModel;
- remove Billing Record list/detail screens and ViewModels;
- remove their presentation-only tests;
- remove six operational screen routes/composables and callbacks;
- retain Quotation, Job Order, Billing Record, and Frame Rating
  service/DTO/domain/repository/DI layers while their API routes remain in the
  35-route contract.

Frame Rating presentation has already moved to Eyewear at this point. The
underlying `JobOrderRepository` remains because it owns the rating mutation.

## Dependency Graph

```text
Updated backend aggregate contract (35 routes)
    |
    +--> Route allowlist target (35)
    |       |
    |       +--> EyewearApiService
    |
    +--> MoneyValueSerializer
    |       |
    |       +--> Eyewear DTOs --> repository mapping --> domain
    |                                              |
    |                                              +--> list ViewModel
    |                                              |       |
    |                                              |       +--> Current/History list
    |                                              |
    |                                              +--> detail ViewModel
    |                                                      |
    |                                                      +--> detail sections/tracker
    |
    +--> Existing JobOrderRepository.submitRating
    |       |
    |       +--> tested FrameRatingViewModel/dialog
    |               |
    |               +--> dispensed Eyewear preparation item
    |
    +--> Eyewear routes
            |
            +--> Profile single entry
            +--> canonical list keys
            +--> jo_ aliases for genuine Job Order entry points
            +--> message context correction
                    |
                    +--> old presentation link sweep
                            |
                            +--> operational screen retirement
```

## Implementation Order

### Stage 0 — Baseline and route-contract red proof

Purpose: establish a known green baseline and prove route discovery detects the
two missing aggregate endpoints.

Activities:

- record `git status` and preserve user-owned/backend changes;
- run the existing route test, relevant Quotation/Job Order/Billing Record,
  Frame Rating, Profile, and message tests;
- run the full unit suite and debug assembly;
- change the approved route set/count from 33 to 35 by adding the two Eyewear
  routes;
- add `{key}` path normalization;
- run the route test before adding `EyewearApiService`.

The intended red result is that the allowlist expects two Eyewear routes that
Retrofit discovery cannot yet find. Existing 33 routes remain approved.

Checkpoint commands:

```powershell
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew testDebugUnitTest --tests "*FrameRating*"
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
```

Exit criteria:

- unrelated baseline failures are recorded before implementation;
- the route test proves the exact 33-to-35 delta;
- backend docs and unrelated files are untouched.

### Stage 1 — Aggregate contract, domain, repository, and DI

Purpose: add a fully tested data vertical without changing visible navigation.

Test-first coverage:

- complete list/detail decoding;
- estimate-only, preparation-only, ready, complete, and voided-billing
  section combinations;
- omitted section keys;
- nullable consultation/payment/balance/timestamps/product variant IDs;
- quoted and numeric exact-money values;
- every progress/payment value and unknown fallback;
- pagination metadata and server order;
- query parameters for both filters/pages;
- canonical/alias detail path pass-through;
- repository boundary mapping.

Production work:

- add service, DTOs, domain, repository interface/implementation, and Hilt
  module;
- map all amounts to `BigDecimal`;
- keep `activityAt` without sorting locally;
- leave all legacy verticals unchanged.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*EyewearDtosTest"
.\gradlew testDebugUnitTest --tests "*EyewearRepositoryImplTest"
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebug
```

Exit criteria:

- route allowlist and discovery both report exactly 35;
- complete and partial resources map correctly;
- no aggregate money uses floating point;
- no client-side operational joining exists;
- debug assembly passes.

### Stage 2 — List/detail state and presentation policy

Purpose: establish testable state/policy before building dense Compose
presentation.

Test-first coverage:

- initial Current request;
- success, empty, failure, and retry;
- filter switch reset;
- stale old-filter response suppression;
- server-order append;
- duplicate append prevention;
- append failure/retry without data loss;
- detail isolation, opaque key forwarding, error, and same-key retry;
- every label/date/money/balance/tracker/section/rating policy.

Production work:

- add separate list/detail ViewModels;
- use request cancellation or monotonically increasing request identity so
  stale responses fail closed;
- add pure presentation helpers/policy;
- do not add navigation or remove old consumers yet.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*EyewearListViewModelTest"
.\gradlew testDebugUnitTest --tests "*EyewearDetailViewModelTest"
.\gradlew testDebugUnitTest --tests "*EyewearPresentationTest"
.\gradlew assembleDebug
```

Exit criteria:

- state behavior is deterministic under filter/request races;
- detail performs no list request;
- presentation policy covers unknown/terminal states;
- debug assembly passes.

### Stage 3 — Additive Eyewear list and detail UI

Purpose: build the complete new experience while existing screens remain
reachable for parity comparison.

List work:

- Eyewear app bar;
- Current/History segmented control;
- loading, empty, initial error/retry;
- cards with description, Consultation/Created date, progress, total,
  independent payment state, and applicable balance;
- pagination spinner and append retry;
- canonical-key callback.

Detail work:

- patient-friendly header;
- Estimate → Preparation → Ready → Released tracker;
- exceptional terminal states;
- conditional Estimate, Preparation, Pickup & Release, and Payment Summary
  cards;
- exact financial rows and posted payment history;
- safe missing/unknown values;
- no operational mutations.

Compose coverage:

- list filter and all primary states;
- card content/callback;
- full aggregate;
- every documented partial-section response;
- empty future sections absent;
- tracker milestone semantics;
- patient-friendly copy and absence of internal headings.

No Profile or old-route cutover occurs yet.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*Eyewear*"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Manual parity review at this stage compares the new combined page with all
three old detail screens for estimate items/prices/discount/total,
preparation items/status/timestamps, dispensing state, and payment summary.

Exit criteria:

- combined list/detail cover every approved section;
- conditional layout never shows empty operational cards;
- internal terms are absent from headings;
- Android test APK and debug APK assemble.

### Stage 4 — Make Frame Rating functional inside Eyewear

Purpose: preserve the dispensed-frame capability before retiring Job Order
presentation.

Test-first coverage:

- 1–5 rating validation;
- exact Job Order item/product variant arguments;
- trimmed/blank optional comment behavior;
- duplicate-submit guard;
- submitting, success, structured validation error, generic error, and reset;
- eligibility only for `DISPENSED` aggregate plus both required IDs;
- non-frame/null-variant and unknown progress have no action.

Production work:

- wire the existing assisted rating ViewModel to the selected Eyewear item;
- connect dialog submit to the repository call;
- retain dialog/draft on failure;
- prevent dismiss during submission;
- handle success deterministically;
- initially reuse rating presentation from its current package, then move it
  during Stage 6.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*FrameRating*"
.\gradlew testDebugUnitTest --tests "*Eyewear*"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Manual rating proof uses a dispensed frame item and confirms error/retry and
success.

Exit criteria:

- rating calls the existing endpoint with aggregate item identifiers;
- no non-dispensed action is exposed;
- broken dismiss-without-submit wiring is not propagated;
- focused tests and assemblies pass.

### Stage 5 — Navigation/Profile cutover and message correction

Purpose: make Eyewear the single visible patient journey only after parity and
rating are green.

Work:

- add `EyewearList` and `EyewearDetail(key)`;
- add NavGraph destinations and hide bottom navigation;
- replace Profile's three rows/callbacks with one Eyewear row;
- route list keys unchanged;
- support the `jo_` alias helper for genuine Job Order IDs;
- stop routing retired Order message contexts to Job Order detail;
- preserve Appointment context navigation and safe unsupported contexts.

Test-first coverage:

- Profile single-row label/supporting copy/callback;
- absence of three old Profile rows;
- route argument round-trip for opaque keys;
- canonical and `jo_` navigation;
- retired Order context is non-interactive/not misrouted;
- Appointment message context regression;
- bottom-nav visibility.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*Eyewear*"
.\gradlew testDebugUnitTest --tests "*MessageDtosTest" --tests "*ChatRepository*"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Manual navigation:

1. Profile → Eyewear → detail → Back;
2. switch Current/History without duplicate destinations;
3. open canonical key;
4. open a genuine Job Order alias when available;
5. verify an Appointment message card still works;
6. verify retired Order context does not open a Job Order/Eyewear record.

Exit criteria:

- Eyewear is the only visible Profile entry for the operational journey;
- message context behavior matches its actual contract;
- new destinations are stable and bottom-nav safe;
- old screens still exist only as temporary non-Profile routes pending Stage
  6.

### Stage 6 — Verified operational presentation retirement

Purpose: remove obsolete patient screens only after all new consumers are
proven.

Pre-deletion link audit:

```powershell
rg -n "QuotationList|QuotationDetail|JobOrderList|JobOrderDetail|BillingRecordList|BillingRecordDetail|onNavigateToQuotations|onNavigateToJobOrders|onNavigateToBillingRecords" app/src
rg -n "QuotationListScreen|QuotationDetailScreen|JobOrderListScreen|JobOrderDetailScreen|BillingRecordListScreen|BillingRecordDetailScreen" app/src
```

Expected remaining references before deletion must be limited to the old
presentation routes/screens/ViewModels/tests assigned to this stage.

Work:

- move `FrameRatingDialog` and `FrameRatingViewModel` into the Eyewear
  presentation package and update tests/imports;
- remove old list/detail composables and combined ViewModels;
- remove their type-safe routes and NavGraph composables/imports;
- remove obsolete presentation tests;
- leave old service/DTO/domain/repository/DI layers and their contract tests;
- rerun source sweeps.

The route allowlist remains 35 because all eight relevant read service
annotations still exist: six operational plus two aggregate.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*Eyewear*"
.\gradlew testDebugUnitTest --tests "*FrameRating*"
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Exit criteria:

- no old screen route/callback/composable/ViewModel remains;
- no patient-facing operational navigation returns;
- aggregate and rating flows remain green;
- operational data/API layers remain unchanged;
- route discovery stays exactly 35.

### Stage 7 — Documentation and complete verification

Purpose: synchronize current context and prove the integrated V12 result.

Documentation:

- update `CONTEXT.md` to 35 routes and the unified Eyewear journey;
- document the aggregate data source, filters, exact money, conditional
  sections, Profile entry, and rating;
- remove current-context references to separate operational screens;
- correct V11 status metadata to reflect the already-existing V11
  implementation commits if still stale;
- mark V12 phases complete only when their work is actually complete;
- do not edit backend documents.

Source sweeps:

```powershell
rg -n "QuotationList|QuotationDetail|JobOrderList|JobOrderDetail|BillingRecordList|BillingRecordDetail|onNavigateToQuotations|onNavigateToJobOrders|onNavigateToBillingRecords" app/src CONTEXT.md
rg -n -i "\"Quotations\"|\"Job Orders\"|\"Billing Records\"" app/src/main app/src/androidTest CONTEXT.md
rg -n "Double|Float" app/src/main/java/com/eyecare/app/data/remote/dto/EyewearDtos.kt app/src/main/java/com/eyecare/app/domain/model/Eyewear.kt app/src/main/java/com/eyecare/app/presentation/eyewear
```

Review data-layer class-name matches separately; operational DTO/repository
layers intentionally remain. The forbidden target is old presentation and
patient-facing navigation/copy.

Final automated verification:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew assembleDebug
git diff --check
```

If an emulator/device is available:

```powershell
.\gradlew connectedDebugAndroidTest
```

Manual smoke matrix:

- Current/History empty, one-page, multi-page, retry, append, and filter-race
  behavior;
- all seven progress states plus unknown;
- no payment, balance due, paid, and unknown payment states;
- Consultation and Created date paths;
- estimate-only, preparation-only, ready, complete, and voided-billing detail;
- exact totals/discount/payments/balance;
- normal/exceptional tracker states;
- dispensed rating success/error;
- Profile, Back, bottom navigation, Appointment message contexts, and retired
  Order contexts.

Exit criteria:

- full automated gates pass or environment-only limits are documented;
- route allowlist/discovery reports 35;
- forbidden presentation/source sweeps are clean;
- current context matches V12;
- backend docs remain untouched;
- debug APK assembles.

## Expected File Groups

### New aggregate production files

```text
app/src/main/java/com/eyecare/app/data/remote/api/EyewearApiService.kt
app/src/main/java/com/eyecare/app/data/remote/dto/EyewearDtos.kt
app/src/main/java/com/eyecare/app/data/repository/EyewearRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/Eyewear.kt
app/src/main/java/com/eyecare/app/domain/repository/EyewearRepository.kt
app/src/main/java/com/eyecare/app/di/EyewearModule.kt
app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearListViewModel.kt
app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearDetailViewModel.kt
app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearScreens.kt
```

Phase 3 may split the screen/policy implementation into additional small files
where that keeps individual tasks within the five-file target.

### Expected new tests

```text
app/src/test/java/com/eyecare/app/data/remote/dto/EyewearDtosTest.kt
app/src/test/java/com/eyecare/app/data/repository/EyewearRepositoryImplTest.kt
app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearListViewModelTest.kt
app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearDetailViewModelTest.kt
app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearPresentationTest.kt
app/src/test/java/com/eyecare/app/presentation/eyewear/FrameRatingViewModelTest.kt
app/src/androidTest/java/com/eyecare/app/presentation/eyewear/EyewearListScreenTest.kt
app/src/androidTest/java/com/eyecare/app/presentation/eyewear/EyewearDetailScreenTest.kt
```

### Shared integration files

```text
app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt
app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt
app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt
app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/Message.kt
app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageBubble.kt
app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt
app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt
app/src/test/java/com/eyecare/app/data/remote/dto/MessageDtosTest.kt
app/src/androidTest/java/com/eyecare/app/presentation/messaging/components/MessageBubbleTest.kt
app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt
CONTEXT.md
```

### Presentation files moved

```text
app/src/main/java/com/eyecare/app/presentation/joborders/FrameRatingDialog.kt
    -> app/src/main/java/com/eyecare/app/presentation/eyewear/FrameRatingDialog.kt
app/src/main/java/com/eyecare/app/presentation/joborders/FrameRatingViewModel.kt
    -> app/src/main/java/com/eyecare/app/presentation/eyewear/FrameRatingViewModel.kt
```

### Presentation files expected to be deleted after parity

```text
app/src/main/java/com/eyecare/app/presentation/quotations/QuotationScreens.kt
app/src/main/java/com/eyecare/app/presentation/quotations/QuotationViewModel.kt
app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderScreens.kt
app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderViewModel.kt
app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordScreens.kt
app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordListViewModel.kt
app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordDetailViewModel.kt
app/src/test/java/com/eyecare/app/presentation/joborders/JobOrderViewModelTest.kt
```

Phase 3 must audit for any additional direct presentation tests before
finalizing deletion tasks.

### Operational files intentionally retained

```text
QuotationApiService / QuotationDtos / Quotation domain/repository/DI/tests
JobOrderApiService / JobOrderDtos / JobOrder domain/repository/DI/tests
BillingRecordApiService / BillingRecordDtos / BillingRecord domain/repository/DI/tests
FrameRating DTO/domain/repository behavior
```

## Test-First and Green-Checkpoint Strategy

For each approved Phase 3 task:

1. add or update the smallest focused test;
2. run it and confirm the intended red reason;
3. implement the minimum coherent behavior;
4. run the focused test;
5. run adjacent regressions;
6. run `.\gradlew assembleDebug`;
7. inspect the diff and run `git diff --check`.

Stages 1–5 are additive and should end green individually. Stage 6 is the only
compiler-coupled retirement slice; rating moves, route deletion, screen
deletion, and integration cleanup must reach one green checkpoint before a
commit.

Do not introduce compatibility wrappers between old and new presentation.
Both presentations may coexist only while the aggregate is being verified.

## Suggested Commit Boundaries

Commit only after the associated checkpoint is green:

1. `feat(V12): add unified patient eyewear journey`
2. `refactor(V12): retire separate eyewear record screens`
3. `docs(V12): update Android context and verification status`

The first commit includes the 35-route allowlist after its red proof is
resolved. Backend documents must not be staged in Android commits.

## Parallel and Sequential Work

Implementation should be sequential in the existing branch:

- list/detail share aggregate DTO/domain/repository policy;
- both screens share progress/payment/date/tracker formatting;
- Profile, routes, NavGraph, and message correction converge in the same
  integration layer;
- rating presentation must move before Job Order presentation deletion;
- final source sweeps depend on the complete integrated tree.

The additive architecture reduces compiler coupling but does not justify
parallel worktrees because shared navigation and presentation-policy edits
would create avoidable conflicts. No subagent or worktree split is required.

## Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Android reconstructs the aggregate from legacy endpoints | Eyewear repository depends only on `EyewearApiService`; tests assert its paths/queries. |
| Exact money is lost | Apply `MoneyValueSerializer` to every aggregate amount and test quoted/numeric precision-sensitive fixtures. |
| Missing sections render empty cards | Model section objects as nullable with defaults and test every partial response. |
| Filter switch shows stale records | Cancel/invalidate old jobs and test out-of-order response completion. |
| Pagination reorders data | Append server pages without sorting and test order preservation. |
| Payment changes Current/History membership | Send backend filter only and never locally classify; presentation policy keeps payment separate. |
| Unknown status implies active/completed state | Explicit `UNKNOWN` labels and no rating/action. |
| Opaque key is parsed or rebuilt incorrectly | Treat list keys as raw strings; only documented `jo_` alias helper constructs genuine Job Order entry keys. |
| Created date is mislabeled Consultation | Central date policy returns both label and value and has nullable tests. |
| UI invents expected completion | No domain field or presentation calculation exists. |
| Tracker implies cancelled work will continue | Exceptional policy marks only evidence-backed milestones and shows a terminal outcome. |
| Internal clinic terms dominate the UI | Patient-facing headings/labels are centralized and instrumented tests reject old visible rows/headings. |
| Rating disappears with Job Order screen deletion | Make rating green in Eyewear first, move its files, then delete old presentation. |
| Existing broken rating callback is copied | Add ViewModel call-argument/state tests and a manual endpoint smoke test. |
| Retired Order IDs are treated as Job Order IDs | Map/render them as unsupported and add repository/UI regression coverage. |
| Old presentation is deleted before parity | Explicit Stage 3 parity and Stage 5 link gates precede Stage 6. |
| Route count falls after screen deletion | Retain all legacy Retrofit services; only presentation is retired, keeping discovery at 35. |
| Old data layers are removed accidentally | File-level retention list and route tests protect operational verticals. |
| User/backend changes are overwritten | Record status, scope edits explicitly, and exclude backend docs/unrelated files from staging. |
| V11 documentation contradicts implemented baseline | Correct status metadata in the final documentation stage using existing Git evidence. |

## Verification Matrix

| Area | Focused proof | Integrated proof |
|---|---|---|
| Route delta | `ApiRouteAllowlistTest` red then green | Exact 35-route final assertion |
| DTO and exact money | `EyewearDtosTest` | Full unit suite and precision smoke |
| Repository mapping | `EyewearRepositoryImplTest` | Complete/partial API smoke |
| Filter/pagination races | `EyewearListViewModelTest` | Current/History manual stress |
| Detail isolation/retry | `EyewearDetailViewModelTest` | Detail network/error smoke |
| Labels/date/tracker | `EyewearPresentationTest` | Compose semantics/manual matrix |
| Conditional sections | DTO/presentation tests | Full/partial detail Compose tests |
| Rating | Frame Rating ViewModel/repository tests | Dispensed rating manual smoke |
| Profile/navigation | Profile and route coverage | Back/bottom-nav smoke |
| Message correction | Chat mapping and MessageBubble tests | Appointment/retired Order smoke |
| Presentation retirement | `rg` source sweeps | Final diff review |
| Formatting/static quality | `ktlintCheck` | `lintDebug` |
| Integration | focused `assembleDebug` per stage | full tests, Android-test APK, debug APK |

## Open Questions

None. The approved V12 specification and updated backend contract resolve the
technical dependencies required for Phase 3 decomposition.

## Phase Gate

Phase 1 was approved by the project owner on 2026-07-29.

This Phase 2 plan was approved by the project owner on 2026-07-29. The Phase 3
task breakdown may be produced for human review.

Do not modify Android production code until the Phase 3 task breakdown is
approved and the project owner gives a separate instruction to proceed.
