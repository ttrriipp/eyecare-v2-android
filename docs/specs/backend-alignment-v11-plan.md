# Backend Alignment V11 — Implementation Plan

Status: Approved — Phases 2–3 complete (2026-07-28); implementation paused by project owner

Specification: `docs/specs/backend-alignment-v11-spec.md`

## Goal

Implement the approved V11 clean cutover in two coherent Android slices:

1. replace the complete Invoice vertical with read-only Billing Records; and
2. make every new Frame Reservation select an eligible Appointment while
   retaining the Appointment context returned by the backend.

Approval of this plan authorized only the separate Phase 3 task breakdown. It
does not authorize production-code changes; the task list must be approved and
the project owner must give a separate instruction before implementation
starts.

## Planning Constraints

- The current V10 Android tree is the implementation baseline.
- `docs/API_CONTRACT.md` is the authoritative endpoint and payload contract.
- `docs/BACKEND_CONTEXT.md` supplies lifecycle and workflow context.
- The Android app is not deployed, so no Invoice compatibility path is kept.
- The route allowlist and discovered Retrofit routes must both remain exactly
  33 after replacing the two Invoice routes one-for-one.
- The user-deleted standalone backend billing and reservation specifications
  remain deleted.
- No dependency, Room schema, backend, or API changes are part of V11.
- Kotlinx Serialization, Hilt, MVVM/Clean boundaries, `StateFlow`, type-safe
  navigation, and repository-boundary DTO mapping remain mandatory.
- Every implementation checkpoint ends with `.\gradlew assembleDebug`.

## Architecture

### Billing Record vertical

The Invoice vertical is replaced, not adapted:

```text
BillingRecordApiService
    -> BillingRecordDtos
    -> BillingRecordRepositoryImpl
    -> BillingRecordRepository
    -> BillingRecord / BillingPayment
    -> BillingRecordListViewModel / BillingRecordDetailViewModel
    -> BillingRecord list/detail screens
    -> BillingRecordList / BillingRecordDetail routes
```

The resource shape is the same for list and detail. The list repository method
maps the standard pagination metadata to the existing `PaginatedResult`;
detail maps one resource. Both mappings discard patient, encounter, recorder,
notes, deletion, and unused audit fields.

The domain is intentionally minimal:

```kotlin
data class BillingRecord(
    val id: Int,
    val billingRecordNumber: String,
    val jobOrderId: Int,
    val status: BillingRecordStatus,
    val totalAmount: BigDecimal,
    val amountPaid: BigDecimal,
    val balanceDue: BigDecimal,
    val recordedAt: String?,
    val payments: List<BillingPayment>,
)

data class BillingPayment(
    val id: Int,
    val amount: BigDecimal,
    val paymentMethod: String,
    val referenceNumber: String?,
    val status: BillingPaymentStatus,
    val recordedAt: String?,
)
```

`BillingRecordStatus` and `BillingPaymentStatus` include `UNKNOWN`. Raw payment
method strings stay in the domain because the contract does not define a
closed method vocabulary; presentation uses a safe humanizer rather than an
enum fallback.

Every money DTO uses the existing `MoneyValueSerializer` and maps directly to
`BigDecimal`. Presentation formatting operates on `BigDecimal` and never
passes through `Double` or `Float`.

List and detail have separate Hilt ViewModels. List owns pagination, refresh,
and load-more state. Detail owns exactly one record ID, retry, and Job Order
navigation data. Opening detail therefore cannot trigger a list request.

### Appointment-linked Frame Reservation vertical

Reservation transport and domain become appointment-linked by construction:

```text
FrameReservationApiService
    -> FrameReservationDtos (required request appointment_id + embedded appointment)
    -> FrameReservationRepositoryImpl (mapping + structured 422 parsing)
    -> FrameReservationRepository (non-null appointmentId)
    -> FrameReservation (embedded ReservationAppointment only)
```

The domain removes nullable scalar `appointmentId` and contains:

```kotlin
data class ReservationAppointment(
    val id: Int,
    val appointmentNumber: String,
    val status: AppointmentStatus,
    val scheduledAt: String,
    val durationMinutes: Int,
)
```

The embedded object is used by create, list, and cancel responses and is the
only domain/display source for linkage. Unknown status maps safely to the
existing Appointment `UNKNOWN` value.

Reservation `422` handling follows the existing repository pattern used by
Appointments and Patient Intake:

```text
HttpException(422)
    -> decode ApiErrorBody with injected Json
    -> FrameReservationError.ValidationError(fieldErrors)
    -> ViewModel branches on error fields
```

The ViewModel never parses `message` prose. An `appointment_id` field error
clears the selection and reloads eligible Appointments. An `items` or
`items.*` error retains the selected Appointment. Malformed error JSON and
non-422 failures fall back to a safe generic error state.

### Appointment eligibility

`CreateFrameReservationViewModel` receives `AppointmentV1Repository` in
addition to `FrameReservationRepository`. It loads pages sequentially from
page 1 through `lastPage`, preserves backend order, and deduplicates by
Appointment ID defensively.

Eligibility is implemented as a pure, unit-tested function with an injected
or passed `Instant`:

```text
status == SCHEDULED
and scheduled_at parses as an offset instant
and scheduled instant + duration_minutes >= now
```

The exact end instant remains eligible. Invalid timestamps, negative duration,
unknown statuses, and already-ended visits fail closed. If any required page
fails, the whole selection load fails rather than presenting an incomplete set
as authoritative.

Selection state is separate from submission state so a reload, retry, or
validation recovery cannot accidentally submit a reservation.

### Navigation and return flow

Add one type-safe route:

```text
BookAppointmentForReservation
```

It reuses `BookAppointmentScreen`, but its `onBooked` behavior pops only the
booking destination and reveals the existing reservation creation entry. The
creation screen refreshes eligible Appointments after the booking route
returns. It does not auto-select or auto-submit the new visit.

The existing general `BookAppointment` behavior remains unchanged. Reservation
creation success navigates to `FrameReservationList` and removes the completed
creation destination from the back stack. Reservation history displays
embedded Appointment number, schedule, duration, and status.

The new booking destination changes only Compose navigation, not the backend
route count.

## Dependency Graph

```text
Corrected backend documents + approved V11 spec
    |
    +--> Route allowlist target (33)
    |       |
    |       +--> Billing Retrofit route replacement
    |
    +--> MoneyValueSerializer
    |       |
    |       +--> Billing DTOs --> Billing repository --> Billing domain
    |                                      |
    |                                      +--> list ViewModel --> list screen
    |                                      +--> detail ViewModel --> detail screen
    |                                                                  |
    |                                                                  +--> JobOrderDetail
    |
    +--> AppointmentV1Repository + AppointmentV1 model
    |       |
    |       +--> page loader --> eligibility filter --> selection state
    |                                                  |
    |                                                  +--> creation submission
    |
    +--> ApiErrorBody + Json
    |       |
    |       +--> reservation 422 mapping --> field-aware recovery
    |
    +--> Embedded reservation appointment DTO
            |
            +--> reservation domain --> create/list/cancel consumers
                                      |
                                      +--> history UI
                                      +--> booking-return navigation
```

## Implementation Order

### Stage 0 — Baseline and contract evidence

Purpose: establish that V10 is green and that the revised backend documents
agree with the approved V11 spec before changing production code.

Activities:

- record `git status` and preserve the user's modified/deleted backend docs;
- run the current route allowlist test, focused Invoice/reservation tests that
  exist, the full unit suite, and debug assembly;
- confirm the API appendix, context, and allowlist baseline counts;
- add target route assertions for `/billing-records` and remove target Invoice
  assertions, then run the route test while the old service still exists.

The deliberately changed allowlist should fail only because Retrofit discovery
still exposes the two Invoice routes and not the Billing Record routes. This is
the route-cutover red proof.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
```

Exit criteria:

- the pre-change tree is understood and any unrelated baseline failure is
  documented before implementation;
- the route test proves it detects the pending one-for-one replacement;
- no user-owned backend document is edited or restored.

### Stage 1 — Billing Record clean cutover

Purpose: replace the compiler-coupled Invoice transport, domain, presentation,
and navigation vertical in one green slice.

Test-first coverage:

- DTO decoding for numeric and decimal-string money, nullable timestamps,
  empty/populated payments, pagination, and unknown strings;
- repository list/detail mapping, exact `BigDecimal` preservation, pagination,
  minimal domain projection, and `UNKNOWN` mapping;
- list ViewModel initial/empty/error/retry, append, duplicate-load prevention,
  and load-more failure behavior;
- detail ViewModel no list request, success/error/retry for the active ID;
- presentation formatting for exact pesos, statuses, raw payment methods,
  nullable dates, and empty payment history;
- route allowlist and Profile destination/label coverage.

Production cutover:

- add Billing Record service, DTOs, domain, repository, DI, list/detail
  ViewModels, screens, and routes;
- link detail to `JobOrderDetail(jobOrderId)`;
- update Profile callback and label to **Billing Records**;
- update `NavGraph` imports, destinations, and bottom-bar visibility rules;
- delete all Invoice production and test files after consumers are migrated;
- remove all retired Invoice fields and wording rather than translating them.

The list/detail UI retains the current app's Material 3 visual language. It
shows only the financial summary, recorded date when present, posted payments,
and Job Order action approved by the spec.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*BillingRecord*"
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Source proof:

```powershell
rg -n -i "Invoice|/invoices|@GET\\(\"invoices" app/src CONTEXT.md
rg -n "Double|Float" app/src/main/java/com/eyecare/app/data/remote/dto/BillingRecordDtos.kt app/src/main/java/com/eyecare/app/domain/model/BillingRecord.kt app/src/main/java/com/eyecare/app/presentation/billingrecords
```

Exit criteria:

- exactly 33 routes are approved and discovered;
- only Billing Record endpoints are called;
- Billing Record list/detail are read-only and exact-money safe;
- detail does not load the list;
- Job Order navigation works;
- no current Android Invoice symbol, path, callback, test, or text remains;
- focused tests, Android-test compilation, and debug assembly pass.

### Stage 2 — Reservation contract, domain, and validation mapping

Purpose: make reservation linkage non-null at every boundary and preserve the
embedded Appointment returned by list, create, and cancel.

Test-first coverage:

- DTO decoding requires `appointment_id` in create requests and decodes the
  embedded Appointment in every reservation resource;
- repository create always serializes a non-null Appointment ID;
- list/create/cancel map the embedded object and never expose duplicate scalar
  domain state;
- unknown embedded Appointment and Reservation statuses fail safely;
- structured `422` field maps are preserved;
- malformed validation bodies and non-422 errors use safe fallbacks;
- existing cancellation capability for requested/prepared and denial for all
  other states remains unchanged.

Production cutover:

- add embedded Appointment transport/domain models;
- make request DTO and repository create signature non-null;
- remove scalar `appointmentId` from `FrameReservation`;
- inject the configured Kotlinx `Json` into the repository;
- add a reservation-specific domain error carrying field errors;
- update current reservation fixtures and consumers for the required embedded
  object;
- retain the one selected variant as the one request item.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*FrameReservationDtosTest"
.\gradlew testDebugUnitTest --tests "*FrameReservationRepositoryImplTest"
.\gradlew testDebugUnitTest --tests "*FrameReservation*"
.\gradlew assembleDebug
```

Exit criteria:

- null or omitted `appointment_id` is impossible through the Android
  repository API;
- create/list/cancel all retain embedded Appointment context;
- validation reasons are derived only from error fields;
- existing cancellation behavior is unchanged;
- focused tests and debug assembly pass.

### Stage 3 — Eligible Appointment selection and recovery

Purpose: require an authoritative local selection before calling the backend
while continuing to rely on backend revalidation.

Test-first coverage:

- page 1 through `lastPage` is loaded in order;
- page results are accumulated without local reordering and duplicate IDs are
  removed;
- a later-page failure produces a load error, not a partial eligible list;
- scheduled future and exact-end Appointments are eligible;
- past-end, checked-in, fulfilled, cancelled, no-show, unknown, malformed-time,
  and invalid-duration Appointments are ineligible;
- retry reloads from page 1;
- duplicate submit taps result in one create call;
- submit is blocked without a selected eligible Appointment;
- appointment field errors clear selection and reload;
- item field errors retain selection;
- generic failures do not leak backend response bodies.

Production work:

- split selection loading, selection, submission, and success/error state so
  each can recover independently;
- add the all-page loader and pure eligibility function;
- inject `AppointmentV1Repository`;
- require explicit Appointment selection and confirmation;
- show loading, retry, empty, populated, submitting, and validation states;
- expose a booking action when no eligible visit exists;
- refresh on returning from booking without auto-selecting or auto-submitting.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*FrameReservationEligibilityTest"
.\gradlew testDebugUnitTest --tests "*CreateFrameReservationViewModelTest"
.\gradlew testDebugUnitTest --tests "*FrameReservation*"
.\gradlew assembleDebug
```

Exit criteria:

- the selection set reflects all Appointment pages and the inclusive end-time
  rule;
- incomplete or malformed data fails closed;
- no create request can be sent without a valid explicit selection;
- field-specific recovery matches the approved spec;
- focused tests and debug assembly pass.

### Stage 4 — Reservation booking return, history, and integrated UI

Purpose: complete the patient flow and surface the backend's embedded
Appointment context.

Production work:

- add `BookAppointmentForReservation`;
- reuse the existing booking screen with reservation-specific back-stack
  behavior;
- preserve the general booking route unchanged;
- update creation copy and UI so Appointment linkage is required;
- navigate successful creation to Reservation history and remove the completed
  draft destination;
- render Appointment number, schedule, duration, and safe status on history
  cards;
- preserve cancellation actions and update a cancelled card from the returned
  embedded Appointment resource.

Presentation/instrumented coverage:

- required selection and confirmation behavior;
- loading, retry, empty, and populated appointment states;
- booking action and return to the same reservation draft;
- no auto-submit after booking;
- success destination and back-stack behavior;
- reservation history appointment context;
- requested/prepared cancellation and non-cancellable states.

Checkpoint:

```powershell
.\gradlew testDebugUnitTest --tests "*FrameReservation*"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Manual checkpoint:

1. open one frame variant and start reservation;
2. verify every eligible visit across multiple pages can be selected;
3. verify an exact-end visit remains eligible;
4. verify an empty state can book and return to the same draft;
5. verify returning never auto-submits;
6. submit and land on Reservation history;
7. verify Appointment context on the created card;
8. cancel requested and prepared reservations;
9. verify non-cancellable statuses have no cancel action;
10. trigger appointment and item validation failures and verify their distinct
    recovery behavior.

Exit criteria:

- the booking return loop is complete and type-safe;
- success opens history with Appointment context visible;
- cancellation behavior is preserved;
- Android test APK and debug APK assemble.

### Stage 5 — Documentation and complete verification

Purpose: synchronize current project context, remove stale metadata, and prove
the integrated V11 result.

Documentation:

- update `CONTEXT.md` from Invoices/optional linkage to Billing Records and
  required Appointment linkage;
- mark V10 spec, plan, and task status metadata complete using the existing V10
  commit evidence;
- mark the V11 spec, plan, and tasks complete only after their actual phases
  finish;
- do not edit or restore the user-owned backend source documents.

Source sweeps:

```powershell
rg -n -i "Invoice|InvoiceList|InvoiceDetail|InvoiceApiService|InvoiceRepository|/invoices|@GET\\(\"invoices" app/src CONTEXT.md
rg -n "appointmentId: Int\\?|appointmentId: Int\\? = null|appointment_id.*null" app/src/main app/src/test app/src/androidTest
rg -n -i "optional appointment|appointment.*optional" app/src CONTEXT.md
rg -n "Double|Float" app/src/main/java/com/eyecare/app/data/remote/dto/BillingRecordDtos.kt app/src/main/java/com/eyecare/app/domain/model/BillingRecord.kt app/src/main/java/com/eyecare/app/presentation/billingrecords
```

Final automated verification:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew assembleDebug
```

If an emulator or device is available:

```powershell
.\gradlew connectedDebugAndroidTest
```

Final manual smoke matrix:

- Billing Record list: empty, one page, multiple pages, retry, and load-more
  failure;
- Billing Record detail: unpaid, partially paid, paid, voided, unknown, empty
  payments, populated payments, nullable dates, raw unknown payment method,
  and Job Order navigation;
- reservation: eligible and ineligible status/time combinations, all-page
  loading, booking return, field validation, success, history, and
  cancellation;
- bottom navigation, Profile navigation, Back behavior, and general
  Appointment booking regression.

Exit criteria:

- full automated gates pass or an environment-only limitation is explicitly
  documented;
- route allowlist and discovery both report 33;
- all forbidden source sweeps are clean;
- current docs match the implemented Android behavior;
- `git diff --check` passes;
- debug APK assembles.

## Expected File Groups

### Billing Record additions/replacements

```text
app/src/main/java/com/eyecare/app/data/remote/api/BillingRecordApiService.kt
app/src/main/java/com/eyecare/app/data/remote/dto/BillingRecordDtos.kt
app/src/main/java/com/eyecare/app/data/repository/BillingRecordRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/BillingRecord.kt
app/src/main/java/com/eyecare/app/domain/repository/BillingRecordRepository.kt
app/src/main/java/com/eyecare/app/di/BillingRecordModule.kt
app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordListViewModel.kt
app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordDetailViewModel.kt
app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordScreens.kt
```

### Invoice deletions

```text
app/src/main/java/com/eyecare/app/data/remote/api/InvoiceApiService.kt
app/src/main/java/com/eyecare/app/data/remote/dto/InvoiceDtos.kt
app/src/main/java/com/eyecare/app/data/repository/InvoiceRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/Invoice.kt
app/src/main/java/com/eyecare/app/domain/repository/InvoiceRepository.kt
app/src/main/java/com/eyecare/app/di/InvoiceModule.kt
app/src/main/java/com/eyecare/app/presentation/invoices/InvoiceViewModel.kt
app/src/main/java/com/eyecare/app/presentation/invoices/InvoiceScreens.kt
app/src/test/java/com/eyecare/app/data/repository/InvoiceRepositoryImplTest.kt
```

### Reservation modifications

```text
app/src/main/java/com/eyecare/app/data/remote/dto/FrameReservationDtos.kt
app/src/main/java/com/eyecare/app/data/repository/FrameReservationRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/FrameReservation.kt
app/src/main/java/com/eyecare/app/domain/repository/FrameReservationRepository.kt
app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModel.kt
app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationScreen.kt
app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationListViewModel.kt
app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationListScreen.kt
```

### Shared integration modifications

```text
app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt
app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt
app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt
app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt
app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt
app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt
CONTEXT.md
docs/specs/backend-alignment-v10-spec.md
docs/specs/backend-alignment-v10-plan.md
docs/specs/backend-alignment-v10-tasks.md
```

### Expected new unit coverage

```text
app/src/test/java/com/eyecare/app/data/remote/dto/BillingRecordDtosTest.kt
app/src/test/java/com/eyecare/app/data/repository/BillingRecordRepositoryImplTest.kt
app/src/test/java/com/eyecare/app/presentation/billingrecords/BillingRecordListViewModelTest.kt
app/src/test/java/com/eyecare/app/presentation/billingrecords/BillingRecordDetailViewModelTest.kt
app/src/test/java/com/eyecare/app/presentation/billingrecords/BillingRecordPresentationTest.kt
app/src/test/java/com/eyecare/app/data/remote/dto/FrameReservationDtosTest.kt
app/src/test/java/com/eyecare/app/data/repository/FrameReservationRepositoryImplTest.kt
app/src/test/java/com/eyecare/app/presentation/reservations/FrameReservationEligibilityTest.kt
app/src/test/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModelTest.kt
app/src/test/java/com/eyecare/app/presentation/reservations/FrameReservationListViewModelTest.kt
```

Phase 3 will confirm exact test file grouping and keep each discrete task near
the skill's five-file target. Names may be consolidated only where doing so
improves focused ownership without weakening coverage.

## Test-First and Green-Checkpoint Strategy

Within each approved Phase 3 task:

1. add or change the smallest relevant test;
2. run it and confirm failure for the intended missing behavior;
3. implement the minimum coherent production change;
4. run the focused test;
5. run adjacent regression tests;
6. run `.\gradlew assembleDebug`;
7. inspect the diff and run `git diff --check`.

Compiler-coupled migrations may remain red only inside their active stage.
They must not be committed or handed off until the entire stage reaches its
green checkpoint. No temporary nullable field, Invoice alias, duplicate
Appointment state, or legacy route may be added merely to make an intermediate
compile pass.

## Suggested Commit Boundaries

Commit only after the corresponding stage's focused tests and debug assembly
pass:

1. `feat(V11): replace invoices with billing records`
2. `feat(V11): require appointment-linked frame reservations`
3. `docs(V11): update Android context and verification status`

The route allowlist red proof belongs in the first feature commit after it is
green. The user's modified/deleted backend documents must not be staged in
these Android commits unless the user explicitly asks for them to be included.

## Parallel and Sequential Work

Implementation should be sequential in the existing branch:

- the Billing Record domain replacement breaks every Invoice consumer until
  list/detail/navigation/Profile are migrated;
- reservation DTO/domain nullability and embedded Appointment changes break
  repository, ViewModel, screen, and fixtures together;
- both slices modify `NavGraph.kt`, routes, Profile/integration tests, and
  project context;
- final route and source proofs depend on the fully integrated tree.

The Billing and reservation designs are logically independent for review, but
parallel editing would create avoidable shared-file conflicts and weaken the
green-checkpoint discipline. No subagent or separate worktree is required.

## Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Financial precision is lost in transport or formatting | Annotate every money DTO with `MoneyValueSerializer`, retain `BigDecimal` through domain/UI, and test number/string payloads plus precision-sensitive values. |
| Billing Record is accidentally presented as an official document | Use Billing Record language everywhere and source-sweep Invoice, official, receipt, BIR, tax, sold-to, and issued concepts in the migrated feature. |
| Old and new billing models coexist | Perform a clean vertical replacement, delete Invoice files after consumer migration, and run symbol/path/route sweeps. |
| Detail triggers an unnecessary list call | Separate list/detail ViewModels and assert repository call counts. |
| Pagination duplicates or reorders records | Preserve backend page order, guard concurrent load-more, and test append/failure behavior. |
| Unknown statuses enable an action | Map to `UNKNOWN`; Billing Records remain read-only and reservation cancellation remains allowlisted by known statuses only. |
| Unknown payment methods display poorly | Keep the raw value and apply a deterministic safe humanizer with unit tests. |
| Required Appointment is still omitted through a default argument | Remove nullable/default parameters from DTO, repository, and ViewModel APIs and test serialized create requests. |
| Local eligibility differs from backend time logic | Compare parsed instants, include the exact end instant, fail closed, and still rely on backend revalidation. |
| Only the first Appointment page is shown | Traverse through `lastPage` and test multi-page accumulation. |
| A later-page failure silently exposes a partial list | Fail the complete selection load and offer retry from page 1. |
| Booking return loses the reservation draft or submits unexpectedly | Use a dedicated route that pops back to the existing creation entry; keep selection/submit explicit and test the back stack. |
| Lifecycle refresh produces duplicate requests | Make return refresh idempotent and guard concurrent appointment loads/submits. |
| Validation behavior depends on unstable prose | Decode `ApiErrorBody.errors` and branch only on `appointment_id`, `items`, and `items.*` keys. |
| Invalid appointment error leaves a stale selection | Clear selection and reload on the appointment field; retain it for item-only errors. |
| Embedded and scalar Appointment data diverge | Drop scalar linkage from the domain and map/display only the embedded object. |
| Reservation cancellation regresses | Preserve and unit-test the existing requested/prepared capability matrix and map the returned resource. |
| General appointment booking behavior changes | Add a separate reservation-return destination and keep the existing route callback unchanged. |
| User-deleted backend specs are restored or backend docs are overwritten | Treat backend documents as read-only inputs, inspect staged paths, and explicitly exclude them from Android commits. |
| Stale V10 status metadata obscures the baseline | Update metadata from the recorded V10 commit evidence during the documentation stage. |

## Verification Matrix

| Area | Focused proof | Integrated proof |
|---|---|---|
| Billing routes/count | `ApiRouteAllowlistTest` red then green | Full unit suite and 33-route assertion |
| Billing DTO/money | `BillingRecordDtosTest` | Full unit suite and precision smoke data |
| Billing mapping | `BillingRecordRepositoryImplTest` | List/detail manual smoke |
| Billing pagination | `BillingRecordListViewModelTest` | Multi-page and load-more smoke |
| Billing detail isolation | `BillingRecordDetailViewModelTest` | Network/call-count test and manual retry |
| Billing presentation | formatter/status tests | Four-status, unknown, empty/populated payment smoke |
| Reservation transport/mapping | DTO and repository tests | Create/list/cancel smoke |
| Reservation 422 behavior | repository and create-ViewModel tests | Appointment/item validation smoke |
| Appointment eligibility | pure eligibility tests | Multi-page/time-boundary smoke |
| Booking return | ViewModel/navigation or instrumented coverage | Manual back-stack smoke |
| Reservation history/cancel | list ViewModel and presentation coverage | History and cancellation smoke |
| Source retirement | `rg` sweeps | Review staged diff |
| Formatting/static quality | `ktlintCheck` | `lintDebug` |
| Integration | focused `assembleDebug` after every stage | full tests, Android-test APK, final debug APK |

## Open Questions

None. The approved V11 specification resolves the product, contract,
navigation, time-boundary, error-recovery, money, and compatibility decisions
needed to create the Phase 3 task breakdown.

## Phase Gate

Phase 1 was approved by the project owner on 2026-07-28.

This Phase 2 plan and the subsequent Phase 3 task breakdown were approved by
the project owner on 2026-07-28.

Implementation is explicitly paused by the project owner. Do not modify Android
production code until the project owner gives a separate instruction to
proceed.
