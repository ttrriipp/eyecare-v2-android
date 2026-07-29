# Backend Alignment V12 — Phase 3 Task List

Status: Draft — Phase 3 awaiting project-owner approval

Approved specification:
`docs/specs/backend-alignment-v12-spec.md`

Approved implementation plan:
`docs/specs/backend-alignment-v12-plan.md`

Backend contract:
`docs/API_CONTRACT.md` at the current backend repository state dated 2026-07-29

## Execution Rules

- Execute tasks in dependency order.
- Write or update the focused test before changing corresponding production
  behavior.
- Confirm every red test/compiler failure is caused by the intended V12 change.
- Task 1 deliberately leaves the route test red until Task 3 adds the Eyewear
  service.
- Tasks 2–11 are additive and must end green individually.
- Tasks 12–15 are one compiler-coupled presentation-retirement slice. Do not
  commit between them; their shared green gate is Checkpoint C.
- Do not add compatibility wrappers between old and new presentation.
- Use the aggregate endpoints only; never join operational lists on Android.
- Use Kotlinx Serialization and exact `BigDecimal`.
- Map DTOs to domain models only at repository boundaries.
- Preserve all six existing operational Retrofit services and their data,
  domain, repository, DI, and contract tests.
- Preserve the Frame Rating endpoint/repository.
- Do not modify Room, dependencies, build plugins, backend code, or backend
  documents.
- Preserve unrelated user/worktree changes.
- After every independently green task or checkpoint, run focused tests and
  `.\gradlew assembleDebug`.

## Phase A — Aggregate Contract and State

### Task 1: Change the approved route contract from 33 to 35

**Description:** Add the two documented Eyewear routes and `{key}`
normalization before adding the Retrofit service. This proves route discovery
detects the missing aggregate vertical.

**Acceptance criteria:**

- [ ] `GET /api/v1/eyewear` is approved.
- [ ] `GET /api/v1/eyewear/{key}` is approved.
- [ ] All existing 33 routes remain approved.
- [ ] Approved and expected discovered totals are 35.
- [ ] `eyewear/{id}` normalizes to `eyewear/{key}` if an implementation uses
  `{id}`.
- [ ] The focused test fails only because the new service is not yet
  discoverable.

**Verification:**

- [ ] Intended RED:
  `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`

**Dependencies:** None

**Files modified:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** Small

### Task 2: Add Eyewear DTO and domain contracts

**Description:** Decode the complete and partial aggregate payloads with exact
money, then map their shape into serialization-free models and safe enums.

**Test-first steps:**

1. Add DTO fixtures for list, complete detail, and every partial-section rule.
2. Cover quoted/numeric money, omitted sections, nullable values, all status
   values, and unknown strings.
3. Confirm tests fail because Eyewear types do not exist.
4. Add DTO and domain types without client-derived linkage or compatibility
   fields.

**Acceptance criteria:**

- [ ] List and detail summary fields decode.
- [ ] Estimate-only, preparation-only, ready, complete, and voided-billing
  payloads decode.
- [ ] Omitted sections become null; no empty section is fabricated.
- [ ] `consultation_at`, balance, payment state, timestamps, references, and
  product variant IDs decode safely when null.
- [ ] Every amount is `BigDecimal` with `MoneyValueSerializer`.
- [ ] No Eyewear amount uses `Double` or `Float`.
- [ ] Progress and payment enums include `UNKNOWN`.
- [ ] Canonical keys remain opaque strings.
- [ ] Domain models contain no serialization annotations.

**Verification:**

- [ ] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*EyewearDtosTest"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 1

**Files added:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/EyewearDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Eyewear.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/EyewearDtosTest.kt`

**Estimated scope:** Medium

### Task 3: Add Eyewear service, repository, mapping, and DI

**Description:** Add the aggregate-only data vertical, map pagination and
optional sections at the repository boundary, and resolve the 35-route red
proof.

**Test-first steps:**

1. Add MockWebServer tests for Current/History list and canonical/alias detail.
2. Assert paths, queries, server ordering, pagination, exact money, and domain
   mapping.
3. Add repository/service/DI production files.
4. Re-run repository and route tests.

**Acceptance criteria:**

- [ ] Service declares only the two Eyewear GET routes.
- [ ] List sends `current`/`history`, page, and documented per-page value.
- [ ] Repository returns `PaginatedResult<EyewearSummary>`.
- [ ] Pagination metadata is preserved.
- [ ] Backend record order is preserved without sorting.
- [ ] Canonical and `jo_` keys pass to the detail path unchanged.
- [ ] Complete and partial sections map at the repository boundary.
- [ ] Unknown statuses map safely.
- [ ] Repository never calls Quotation, Job Order, or Billing Record services.
- [ ] Hilt binds the new repository.
- [ ] Route allowlist and discovery both report 35.

**Verification:**

- [ ] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*EyewearRepositoryImplTest"`
- [ ] `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 2

**Files added:**

- `app/src/main/java/com/eyecare/app/data/remote/api/EyewearApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/EyewearRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/EyewearRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/EyewearModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/EyewearRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 4: Implement race-safe Current/History list state

**Description:** Add the list ViewModel with initial Current selection,
server-backed filter switching, pagination, and stale-response protection.

**Test-first steps:**

1. Add list ViewModel tests with controllable coroutine results.
2. Assert initial, empty, error/retry, switch, race, append, and append-failure
   behavior.
3. Implement the smallest state machine that satisfies them.

**Acceptance criteria:**

- [ ] Current is requested initially.
- [ ] Filter switch selects the new filter and requests page 1.
- [ ] Old-filter records are cleared.
- [ ] A late old-filter response cannot replace new-filter state.
- [ ] Server order is preserved across appended pages.
- [ ] Concurrent/duplicate append is guarded.
- [ ] Append failure retains existing records and exposes retry.
- [ ] Effective current page advances only after a successful response.
- [ ] Initial retry uses the selected filter.
- [ ] No local progress/payment filtering or sorting exists.

**Verification:**

- [ ] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*EyewearListViewModelTest"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 3

**Files added:**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearListViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearListViewModelTest.kt`

**Estimated scope:** Medium

### Task 5: Add isolated detail state and presentation policy

**Description:** Add a detail-only ViewModel and pure policy for
progress/payment/date/money/tracker/section/rating decisions.

**Test-first steps:**

1. Add detail ViewModel tests for canonical/alias keys, no list call, failure,
   and same-key retry.
2. Add policy tests for every documented/unknown state.
3. Implement the ViewModel and pure policy.

**Acceptance criteria:**

- [ ] Detail loads one opaque key and never calls list.
- [ ] Retry uses the active key unchanged.
- [ ] Canonical and `jo_` strings are not parsed.
- [ ] Every progress and payment label is covered.
- [ ] Unknown values are neutral and non-actionable.
- [ ] Consultation versus Created label/value is deterministic.
- [ ] Peso formatting remains exact.
- [ ] Balance visibility follows payment state plus value presence.
- [ ] Normal and terminal tracker states are covered.
- [ ] Rating is eligible only for dispensed items with both IDs.
- [ ] Missing sections remain absent.

**Verification:**

- [ ] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*EyewearDetailViewModelTest" --tests "*EyewearPresentationTest"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 4

**Files added:**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearPresentation.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearDetailViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/EyewearPresentationTest.kt`

**Estimated scope:** Medium

## Checkpoint A — Aggregate Data and State Green

- [ ] `.\gradlew testDebugUnitTest --tests "*Eyewear*"`
- [ ] `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [ ] `.\gradlew assembleDebug`
- [ ] All 35 routes are approved/discovered.
- [ ] Exact money and every partial response are covered.
- [ ] Filter races, pagination, and detail isolation are green.
- [ ] No legacy client-side join exists.

No commit is created yet; Checkpoint B completes the patient-visible feature.

## Phase B — Combined Experience and Navigation Cutover

### Task 6: Build the Current/History Eyewear list

**Description:** Add the patient-facing list with segmented filtering,
paginated cards, and filter-specific states.

**Test-first steps:**

1. Add a stateless/list-content Compose test seam.
2. Add assertions for Current/History controls and every primary state.
3. Implement the screen and card callbacks.

**Acceptance criteria:**

- [ ] Title is **Eyewear**.
- [ ] Current is initially selected.
- [ ] Current/History taps call the correct ViewModel action.
- [ ] Cards show description and Consultation/Created date.
- [ ] Progress and payment chips are separate.
- [ ] Total is exact and balance appears only when applicable.
- [ ] Canonical key is returned through the detail callback unchanged.
- [ ] Empty/error copy is filter-aware.
- [ ] Initial retry and append retry are distinct.
- [ ] Unknown/blank description fails safely.

**Verification:**

- [ ] `.\gradlew testDebugUnitTest --tests "*EyewearListViewModelTest" --tests "*EyewearPresentationTest"`
- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Checkpoint A

**Files added:**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearListScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/EyewearListScreenTest.kt`

**Estimated scope:** Large

### Task 7: Build conditional Eyewear detail and tracker

**Description:** Add the combined detail header, non-interactive tracker, and
conditional Estimate, Preparation, Pickup & Release, and Payment Summary
sections.

**Test-first steps:**

1. Add Compose fixtures for complete and every partial resource shape.
2. Assert absent sections do not render.
3. Add tracker/terminal-state and patient-language assertions.
4. Implement detail content without rating integration yet.

**Acceptance criteria:**

- [ ] Header shows description, progress, date, total, payment state, and
  applicable balance.
- [ ] Tracker is Estimate → Preparation → Ready → Released.
- [ ] Declined, expired, cancelled, and unknown do not imply future progress.
- [ ] Estimate renders items, quantity, price, non-zero discount, and total.
- [ ] Preparation renders Eyewear Details and actual timestamps.
- [ ] No expected-completion estimate appears.
- [ ] Pickup & Release renders only for ready/dispensing data.
- [ ] Dispensed detail says **Released to You**.
- [ ] Payment Summary renders total, paid, balance, and posted payments.
- [ ] Payment methods/references/dates fail safely.
- [ ] No operational mutation button exists.
- [ ] Internal operational terms are absent from headings.

**Verification:**

- [ ] `.\gradlew testDebugUnitTest --tests "*EyewearDetailViewModelTest" --tests "*EyewearPresentationTest"`
- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 6

**Files added:**

- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearDetailScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/EyewearDetailScreenTest.kt`

**Estimated scope:** Large

### Task 8: Make Frame Rating functional from Eyewear detail

**Description:** Test and repair the existing assisted rating state, then wire
eligible dispensed aggregate items to the dialog and real repository call.

**Test-first steps:**

1. Add Frame Rating ViewModel tests at its eventual Eyewear test location.
2. Prove the current Job Order dialog path dismisses without submission.
3. Add duplicate-submit, success, structured/generic failure, and reset tests.
4. Wire selected Eyewear item → assisted ViewModel → dialog → repository.

**Acceptance criteria:**

- [ ] Rating validates 1 through 5.
- [ ] Repository receives exact Job Order item and product variant IDs.
- [ ] Blank comments become null; non-blank comments are retained safely.
- [ ] Duplicate submit during submission is ignored.
- [ ] Dialog cannot dismiss while submitting.
- [ ] Error retains dialog and draft for retry.
- [ ] Success closes/acknowledges deterministically.
- [ ] Only dispensed items with both IDs show **Rate this frame**.
- [ ] Non-dispensed, unknown, or null-ID items expose no action.
- [ ] Aggregate content remains visible after rating error/success.

**Verification:**

- [ ] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*FrameRatingViewModelTest" --tests "*FrameRatingRepositoryTest"`
- [ ] `.\gradlew testDebugUnitTest --tests "*Eyewear*"`
- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 7

**Files modified/added:**

- `app/src/main/java/com/eyecare/app/presentation/joborders/FrameRatingViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/FrameRatingDialog.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/FrameRatingViewModelTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/eyewear/EyewearDetailScreenTest.kt`

**Estimated scope:** Large

### Task 9: Add Eyewear routes and replace Profile navigation

**Description:** Add list/detail destinations, wire opaque keys, hide bottom
navigation, and atomically replace the three Profile rows.

**Test-first steps:**

1. Update Profile Compose expectations to one Eyewear row.
2. Add route/key and bottom-navigation assertions where practical.
3. Add routes/NavGraph destinations and change Profile callbacks.

**Acceptance criteria:**

- [ ] Routes are `EyewearList` and `EyewearDetail(key: String)`.
- [ ] Profile shows one **Eyewear** row with approved supporting copy.
- [ ] Quotation, Job Order, and Billing Record rows are absent.
- [ ] Profile callback opens `EyewearList`.
- [ ] List callback opens detail with canonical key unchanged.
- [ ] A genuine Job Order helper forms `jo_{id}` without parsing canonical
  keys.
- [ ] Back behavior is natural.
- [ ] Bottom navigation is hidden on both destinations.
- [ ] Old destinations remain temporarily in NavGraph pending retirement.

**Verification:**

- [ ] `.\gradlew testDebugUnitTest --tests "*Eyewear*"`
- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 8

**Files modified:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Medium

### Task 10: Map retired Order message contexts as unsupported

**Description:** Correct repository mapping so a retired Order response can
never become a supported Job Order/Eyewear context. Preserve Appointment and
safe unsupported behavior. UI callback cleanup follows in Task 11.

**Test-first steps:**

1. Add repository-mapping tests for Appointment, Order, Product, and unknown
   response types.
2. Confirm the old mapping incorrectly creates `MessageContext.Order`.
3. Map Order responses to the existing unsupported domain form.

**Acceptance criteria:**

- [ ] Appointment contexts still map and render interactively.
- [ ] `order` / `App\Models\Order` does not become a Job Order or Eyewear
  context.
- [ ] Product and unknown contexts remain non-interactive/safe.
- [ ] No `jo_` key is constructed from a retired Order ID.
- [ ] Ordinary messages and attachments are unchanged.
- [ ] Actual API-mapped retired Order contexts cannot produce an interactive
  card.

**Verification:**

- [ ] RED then GREEN:
  `.\gradlew testDebugUnitTest --tests "*ChatRepositoryMappingTest" --tests "*MessageDtosTest"`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 9

**Files modified/added:**

- `app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ChatRepositoryMappingTest.kt`

**Estimated scope:** Small

### Task 11: Remove obsolete Order context UI and callback plumbing

**Description:** Remove the now-unreachable supported Order domain variant,
MessageBubble card/callback, Chat callback, and NavGraph misroute while
preserving Appointment-context navigation.

**Acceptance criteria:**

- [ ] `MessageContext.Order` is removed.
- [ ] MessageBubble no longer accepts or renders an Order destination.
- [ ] Chat screen no longer accepts or forwards `onOrderClick`.
- [ ] NavGraph no longer maps an Order ID to `JobOrderDetail`.
- [ ] Appointment context callback still opens Appointment detail.
- [ ] Unsupported/product contexts remain non-interactive.
- [ ] Chat messages, sending, attachments, and unread behavior are unchanged.

**Verification:**

- [ ] `.\gradlew testDebugUnitTest --tests "*ChatRepositoryMappingTest" --tests "*MessageDtosTest"`
- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 10

**Files modified:**

- `app/src/main/java/com/eyecare/app/domain/model/Message.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageBubble.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/messaging/components/MessageBubbleTest.kt`

**Estimated scope:** Medium

## Checkpoint B — Unified Eyewear Journey Green

- [ ] Checkpoint A remains green.
- [ ] `.\gradlew testDebugUnitTest --tests "*Eyewear*"`
- [ ] `.\gradlew testDebugUnitTest --tests "*FrameRating*"`
- [ ] `.\gradlew testDebugUnitTest --tests "*ChatRepositoryMappingTest" --tests "*MessageDtosTest"`
- [ ] `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`
- [ ] Full and all partial detail shapes render correctly.
- [ ] Current/History race and pagination behavior is correct.
- [ ] Rating submits successfully from a dispensed aggregate.
- [ ] Profile exposes only Eyewear.
- [ ] Appointment message contexts work and retired Order does not misroute.

Only after Checkpoint B is green may the first V12 implementation commit be
created:

```text
feat(V12): add unified patient eyewear journey
```

## Phase C — Verified Operational Presentation Retirement

Tasks 12–15 are compiler-coupled. Do not run a final green build or create a
commit until all four tasks complete.

### Task 12: Move Frame Rating presentation under Eyewear

**Description:** Move the now-functional dialog/ViewModel out of the obsolete
Job Order presentation package and update Eyewear/tests. Old Job Order
presentation may fail compilation until Task 14 deletes it.

**Acceptance criteria:**

- [ ] Rating dialog and ViewModel live under `presentation/eyewear`.
- [ ] Eyewear detail imports the new package.
- [ ] Rating tests target the final package.
- [ ] Repository/endpoint remain unchanged.
- [ ] Any compiler failure is limited to the old Job Order screen assigned to
  Task 14.

**Verification:**

- [ ] Record intended temporary compiler failure if old Job Order presentation
  still references the moved symbols.
- [ ] Green verification is deferred to Checkpoint C.

**Dependencies:** Checkpoint B

**Files moved/modified:**

- `app/src/main/java/com/eyecare/app/presentation/joborders/FrameRatingViewModel.kt`
  → `app/src/main/java/com/eyecare/app/presentation/eyewear/FrameRatingViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/FrameRatingDialog.kt`
  → `app/src/main/java/com/eyecare/app/presentation/eyewear/FrameRatingDialog.kt`
- `app/src/main/java/com/eyecare/app/presentation/eyewear/EyewearDetailScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/eyewear/FrameRatingViewModelTest.kt`

**Estimated scope:** Small

### Task 13: Delete Quotation and Billing Record presentation

**Description:** Remove the obsolete list/detail screens and ViewModels while
retaining their complete data/network/domain/repository/DI layers.

**Acceptance criteria:**

- [ ] Quotation screens and ViewModel are deleted.
- [ ] Billing Record screens and both ViewModels are deleted.
- [ ] No operational data-layer file is changed or removed.
- [ ] Compiler failures, if any, are limited to old NavGraph/routes assigned to
  Task 15.

**Verification:**

- [ ] Review deleted paths and retained data paths.
- [ ] Green verification is deferred to Checkpoint C.

**Dependencies:** Task 12

**Files deleted:**

- `app/src/main/java/com/eyecare/app/presentation/quotations/QuotationScreens.kt`
- `app/src/main/java/com/eyecare/app/presentation/quotations/QuotationViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordScreens.kt`
- `app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordDetailViewModel.kt`

**Estimated scope:** Small

### Task 14: Delete Job Order presentation and obsolete test

**Description:** Remove old Job Order list/detail state/screens after rating
has moved successfully.

**Acceptance criteria:**

- [ ] Job Order screens and combined ViewModel are deleted.
- [ ] Obsolete Job Order ViewModel test is deleted.
- [ ] Frame Rating presentation remains under Eyewear.
- [ ] `JobOrderRepository` and its rating behavior remain.
- [ ] Compiler failures, if any, are limited to old NavGraph/routes assigned to
  Task 15.

**Verification:**

- [ ] Review deleted paths and retained Frame Rating/data paths.
- [ ] Green verification is deferred to Checkpoint C.

**Dependencies:** Task 13

**Files deleted:**

- `app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderScreens.kt`
- `app/src/main/java/com/eyecare/app/presentation/joborders/JobOrderViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/joborders/JobOrderViewModelTest.kt`

**Estimated scope:** Small

### Task 15: Remove old routes/composables and prove retirement

**Description:** Remove six obsolete type-safe destinations and their NavGraph
imports/composables, then run the shared green checkpoint and source sweeps.

**Acceptance criteria:**

- [ ] `QuotationList` and `QuotationDetail` routes are absent.
- [ ] `JobOrderList` and `JobOrderDetail` routes are absent.
- [ ] `BillingRecordList` and `BillingRecordDetail` routes are absent.
- [ ] NavGraph contains no old screen import/composable.
- [ ] Profile contains no old callback.
- [ ] Chat contains no Job Order misroute.
- [ ] Eyewear and Frame Rating remain green.
- [ ] Legacy operational service/DTO/domain/repository/DI layers remain.
- [ ] All 35 Retrofit routes remain approved/discovered.

**Verification:**

```powershell
rg -n "QuotationList|QuotationDetail|JobOrderList|JobOrderDetail|BillingRecordList|BillingRecordDetail|onNavigateToQuotations|onNavigateToJobOrders|onNavigateToBillingRecords" app/src
rg -n "QuotationListScreen|QuotationDetailScreen|JobOrderListScreen|JobOrderDetailScreen|BillingRecordListScreen|BillingRecordDetailScreen" app/src
.\gradlew testDebugUnitTest --tests "*Eyewear*"
.\gradlew testDebugUnitTest --tests "*FrameRating*"
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Expected source-sweep result: no old presentation/navigation matches.

**Dependencies:** Task 14

**Files modified:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

**Estimated scope:** Medium

## Checkpoint C — Separate Operational Screens Retired

- [ ] Checkpoint B remains green.
- [ ] Task 15 source sweeps are clean.
- [ ] Eyewear list/detail and rating tests pass.
- [ ] Profile and message instrumented tests compile.
- [ ] No old presentation route/screen/ViewModel remains.
- [ ] All operational data/API layers remain.
- [ ] Route allowlist/discovery remains exactly 35.
- [ ] Android test APK and debug APK assemble.

Only after Checkpoint C is green may the second V12 commit be created:

```text
refactor(V12): retire separate eyewear record screens
```

## Phase D — Documentation and Release Verification

### Task 16: Synchronize Android context and V11 metadata

**Description:** Update current project context for V12 and correct stale V11
phase headers using the existing V11 implementation commit evidence.

**Acceptance criteria:**

- [ ] `CONTEXT.md` documents 35 routes.
- [ ] Profile and patient navigation describe one Eyewear destination.
- [ ] Aggregate filters, exact money, conditional sections, tracker, and
  rating are documented.
- [ ] Separate operational screens are no longer described as current UI.
- [ ] Retained operational API/data layers are distinguished from retired
  presentation.
- [ ] Retired Order contexts are not described as Job Order links.
- [ ] V11 spec/plan/tasks reflect their already-completed implementation.
- [ ] Backend documents are not modified.

**Verification:**

- [ ] `git diff --check`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Checkpoint C

**Files modified:**

- `CONTEXT.md`
- `docs/specs/backend-alignment-v11-spec.md`
- `docs/specs/backend-alignment-v11-plan.md`
- `docs/specs/backend-alignment-v11-tasks.md`

**Estimated scope:** Medium

### Task 17: Run V12 source-contract sweeps and finalize metadata

**Description:** Prove the final source boundaries and update V12 documents
only after implementation evidence exists.

**Acceptance criteria:**

- [ ] No old operational presentation symbol/copy remains.
- [ ] No Eyewear amount uses floating point.
- [ ] No Android-side operational aggregation exists.
- [ ] No retired Order ID is converted to `jo_`.
- [ ] All 35 routes remain green.
- [ ] V12 spec/plan/tasks reflect actual checkpoint completion.
- [ ] Backend docs remain untouched.

**Verification:**

```powershell
rg -n "QuotationList|QuotationDetail|JobOrderList|JobOrderDetail|BillingRecordList|BillingRecordDetail|onNavigateToQuotations|onNavigateToJobOrders|onNavigateToBillingRecords" app/src CONTEXT.md
rg -n -i "\"Quotations\"|\"Job Orders\"|\"Billing Records\"" app/src/main app/src/androidTest CONTEXT.md
rg -n "Double|Float" app/src/main/java/com/eyecare/app/data/remote/dto/EyewearDtos.kt app/src/main/java/com/eyecare/app/domain/model/Eyewear.kt app/src/main/java/com/eyecare/app/presentation/eyewear
rg -n "MessageContext\\.Order|onOrderClick" app/src
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
git diff --check
.\gradlew assembleDebug
```

Expected forbidden-source result: no matches. Operational data-layer class
names remain legitimate and are not targeted.

**Dependencies:** Task 16

**Files modified:**

- `docs/specs/backend-alignment-v12-spec.md`
- `docs/specs/backend-alignment-v12-plan.md`
- `docs/specs/backend-alignment-v12-tasks.md`

**Estimated scope:** Small

### Task 18: Run the complete V12 release gate

**Description:** Execute all automated gates, inspect the final diff, run the
manual Eyewear/rating/navigation matrix, and record only genuine environment
limitations.

**Acceptance criteria:**

- [ ] Full unit suite passes.
- [ ] Android test APK compiles.
- [ ] Ktlint passes.
- [ ] Android lint passes.
- [ ] Debug APK assembles.
- [ ] Route allowlist/discovery reports 35.
- [ ] Manual list/detail/filter/tracker/payment/rating checks pass.
- [ ] Profile, Back, bottom navigation, Appointment message contexts, and
  retired Order contexts pass.
- [ ] Final diff contains no backend, Room, dependency, or operational data
  change.
- [ ] User-owned backend files are not staged in Android commits.

**Verification:**

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew assembleDebug
git diff --check
git status --short
git diff --stat
```

If an emulator/device is available:

```powershell
.\gradlew connectedDebugAndroidTest
```

**Manual list checklist:**

- [ ] Current default, empty, retry, one page, and multiple pages.
- [ ] History empty, retry, one page, and multiple pages.
- [ ] Rapid Current/History switching never shows stale records.
- [ ] Every progress label plus unknown.
- [ ] No payment, Balance Due, Paid, and unknown payment.
- [ ] Consultation and Created date paths.
- [ ] Exact total and applicable balance.

**Manual detail checklist:**

- [ ] Estimate-only.
- [ ] Preparation-only.
- [ ] Ready with Pickup & Release.
- [ ] Complete linked with payments.
- [ ] Voided billing with no Payment Summary.
- [ ] Declined, expired, cancelled, and unknown tracker states.
- [ ] No empty section or invented expected completion.
- [ ] No estimate/order/payment mutation.
- [ ] Dispensed rating success and failure/retry.

**Manual integration checklist:**

- [ ] Profile has only Eyewear for the combined journey.
- [ ] Canonical detail and genuine `jo_` alias open correctly.
- [ ] Back and bottom navigation behave correctly.
- [ ] Appointment message context still opens Appointment detail.
- [ ] Retired Order context does not open Eyewear/Job Order.
- [ ] No separate operational screen is reachable.

**Dependencies:** Task 17

**Files modified:** None unless verification exposes an in-scope defect. Any
fix requires a focused regression test and rerunning the affected checkpoint.

**Estimated scope:** Medium

## Checkpoint D — V12 Complete

- [ ] Checkpoint A passed.
- [ ] Checkpoint B passed and its feature commit exists.
- [ ] Checkpoint C passed and its retirement commit exists.
- [ ] Tasks 16–17 documentation/source proofs passed.
- [ ] Task 18 automated and available manual verification passed.
- [ ] `CONTEXT.md` matches implemented V12.
- [ ] V11/V12 metadata reflects actual completion.
- [ ] Backend source documents were not altered by Android implementation.

After Checkpoint D, create the documentation commit:

```text
docs(V12): update Android context and verification status
```

## Dependency Summary

```text
Task 1: 35-route red proof
    -> Task 2: DTO/domain
        -> Task 3: service/repository/DI
            -> Task 4: list state
                -> Task 5: detail state/policy
                    -> Checkpoint A
                        -> Task 6: list UI
                            -> Task 7: detail/tracker UI
                                -> Task 8: functional rating
                                    -> Task 9: Profile/navigation cutover
                                        -> Task 10: message mapping/render correction
                                            -> Task 11: Chat callback cleanup
                                                -> Checkpoint B
                                                    -> Task 12: move rating presentation
                                                        -> Task 13: delete Quotation/Billing UI
                                                            -> Task 14: delete Job Order UI
                                                                -> Task 15: route/NavGraph retirement
                                                                    -> Checkpoint C
                                                                        -> Task 16: context/V11 metadata
                                                                            -> Task 17: V12 sweeps/metadata
                                                                                -> Task 18: release gate
                                                                                    -> Checkpoint D
```

## Task Sizing

| Task | Scope | Primary risk |
|---|---|---|
| 1 | Small | Exact route red proof |
| 2 | Medium | Exact money and optional-section decoding |
| 3 | Medium | Aggregate-only mapping and 35-route parity |
| 4 | Medium | Filter races and pagination |
| 5 | Medium | Detail isolation and presentation policy |
| 6 | Large | Dense paginated list states |
| 7 | Large | Conditional sections and tracker semantics |
| 8 | Large | Repairing and preserving rating |
| 9 | Medium | Atomic visible navigation cutover |
| 10 | Medium | Correct message context semantics |
| 11 | Small | Removing obsolete callback plumbing |
| 12 | Small | Rating package move before deletion |
| 13 | Small | Scoped Quotation/Billing presentation deletion |
| 14 | Small | Scoped Job Order presentation deletion |
| 15 | Medium | Compiler-coupled route cleanup and proof |
| 16 | Medium | Current context and historical metadata |
| 17 | Small | Correctly scoped source proofs |
| 18 | Medium | Cross-feature release verification |

## Phase Gate

Phases 1 and 2 were approved by the project owner on 2026-07-29.

This Phase 3 task list is awaiting explicit project-owner approval.
Implementation has not started. Do not modify Android production code until
this task list is approved and the project owner gives a separate instruction
to proceed.
