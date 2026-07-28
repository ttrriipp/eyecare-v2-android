# Backend Alignment V11 — Phase 3 Task List

Status: Approved by the project owner on 2026-07-28 — implementation explicitly paused

Approved specification:
`docs/specs/backend-alignment-v11-spec.md`

Approved implementation plan:
`docs/specs/backend-alignment-v11-plan.md`

Backend contract:
`docs/API_CONTRACT.md` at the current backend repository state dated 2026-07-28

## Execution Rules

- Execute tasks in dependency order.
- Write or update the focused test before changing corresponding production
  behavior.
- Confirm each red test or compiler failure is caused by the intended V11
  contract change.
- Tasks 1–6 form one compiler-coupled Billing Record cutover. Do not commit or
  add Invoice compatibility between them; their shared green gate is
  Checkpoint A.
- Task 1 intentionally makes the route allowlist test red before the service
  replacement in Task 3.
- Tasks 7–13 form one compiler-coupled reservation cutover. Do not commit or
  restore nullable Appointment linkage between them; their shared green gate
  is Checkpoint B.
- After every independently green task or shared checkpoint, run its focused
  tests and `.\gradlew assembleDebug`.
- Use Kotlinx Serialization only and keep `BigDecimal` exact.
- Map DTOs to domain models only at repository boundaries.
- Do not modify Room, add a dependency, add an endpoint, or change backend
  code.
- Do not restore the deleted standalone backend billing/reservation specs.
- Treat `docs/API_CONTRACT.md`, `docs/BACKEND_CONTEXT.md`, and their current
  deletion state as user-owned inputs. Do not stage them in Android
  implementation commits unless explicitly requested.
- Do not log tokens, health information, payment details, patient response
  bodies, or validation response bodies.

## Phase A — Billing Record Clean Cutover

### Task 1: Change the approved routes from Invoice to Billing Record

**Description:** Update the Android allowlist and path-variable normalization
before replacing the old Retrofit service. This is the intentional red proof
that route discovery detects both the retired Invoice endpoints and the
missing Billing Record endpoints.

**Acceptance criteria:**

- [x] `ApprovedApiRoutes` contains the two Billing Record routes.
- [x] Both Invoice routes are absent.
- [x] The approved route total remains exactly 33.
- [x] Route normalization maps `billing-records/{id}` to
  `billing-records/{billingRecord}`.
- [x] Invoice normalization is removed.
- [x] The focused test fails only because the old Invoice service is still
  discovered and the Billing Record service does not exist yet.

**Verification:**

- [ ] Intended RED:
  `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`

**Dependencies:** None

**Files modified:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** Small

### Task 2: Replace Invoice DTO and domain shapes

**Description:** Write Billing Record decoding tests, then replace the Invoice
DTO/domain files with the approved minimal Billing Record resource. This
intentionally breaks the old repository and presentation consumers until
Tasks 3–6 complete the cutover.

**Test-first steps:**

1. Add `BillingRecordDtosTest` using production-equivalent Kotlinx `Json`.
2. Cover numeric and decimal-string money, pagination, nullable timestamps,
   empty/populated payments, and raw unknown values.
3. Confirm the test fails because Billing Record DTOs do not exist.
4. Rename and rewrite the DTO/domain files without compatibility aliases.

**Acceptance criteria:**

- [x] Required totals decode directly as `BigDecimal` through
  `MoneyValueSerializer`.
- [x] No Billing Record money uses `Double` or `Float`.
- [x] List decodes the standard `data`, `links`, and `meta` envelope.
- [x] List and detail decode the same resource shape.
- [x] Payment amount, method, reference, status, and recorded time are
  preserved.
- [x] Domain contains only the approved minimal Billing Record and payment
  fields.
- [x] Billing and payment status enums include `UNKNOWN`.
- [x] Raw payment method remains a string.
- [x] Invoice item, official number, sold-to, tax, discount, issued, and draft
  concepts do not survive.

**Verification:**

- [ ] Add the intended RED test:
  `.\gradlew testDebugUnitTest --tests "*BillingRecordDtosTest"`
- [ ] If compilation stops in scheduled Tasks 3–6 consumers, record those
  failures and defer the first green execution to Checkpoint A.

**Dependencies:** Task 1

**Files renamed and rewritten:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/InvoiceDtos.kt`
  → `app/src/main/java/com/eyecare/app/data/remote/dto/BillingRecordDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Invoice.kt`
  → `app/src/main/java/com/eyecare/app/domain/model/BillingRecord.kt`

**File added:**

- `app/src/test/java/com/eyecare/app/data/remote/dto/BillingRecordDtosTest.kt`

**Estimated scope:** Medium

### Task 3: Replace Invoice service and repository

**Description:** Rewrite the Retrofit and repository vertical for the two
Billing Record endpoints. Map transport objects to the minimal domain and
preserve standard pagination metadata.

**Test-first steps:**

1. Rename and rewrite `InvoiceRepositoryImplTest` around MockWebServer fixtures
   for Billing Record list/detail.
2. Add failing assertions for endpoint paths, pagination, exact money, unknown
   statuses, and discarded internal fields.
3. Rename and implement the service, repository interface, and implementation.
4. Re-run the repository and route tests.

**Acceptance criteria:**

- [x] Service calls `GET billing-records` and
  `GET billing-records/{id}` only.
- [x] List forwards `page` and retains the documented `per_page` behavior.
- [x] Repository returns `PaginatedResult<BillingRecord>`.
- [x] `currentPage`, `lastPage`, and `total` map correctly.
- [x] Detail maps one Billing Record without loading the list.
- [x] Exact money and every approved domain field survive mapping.
- [x] Unknown status values map to `UNKNOWN`.
- [x] Internal patient/encounter/recorder/audit fields do not enter the domain.
- [x] Route allowlist and discovery both report exactly 33.

**Verification:**

- [ ] Add the intended RED repository coverage:
  `.\gradlew testDebugUnitTest --tests "*BillingRecordRepositoryImplTest"`
- [ ] If main-source compilation stops in Tasks 4–6 consumers, verify those are
  the only blockers and defer green execution to Checkpoint A.

**Dependencies:** Task 2

**Files renamed and rewritten:**

- `app/src/main/java/com/eyecare/app/data/remote/api/InvoiceApiService.kt`
  → `app/src/main/java/com/eyecare/app/data/remote/api/BillingRecordApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/InvoiceRepository.kt`
  → `app/src/main/java/com/eyecare/app/domain/repository/BillingRecordRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/InvoiceRepositoryImpl.kt`
  → `app/src/main/java/com/eyecare/app/data/repository/BillingRecordRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/InvoiceRepositoryImplTest.kt`
  → `app/src/test/java/com/eyecare/app/data/repository/BillingRecordRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 4: Split Billing Record list and detail state

**Description:** Replace the Invoice Hilt binding and combined ViewModel with
Billing Record DI plus dedicated list and detail ViewModels.

**Test-first steps:**

1. Add independent list and detail ViewModel tests.
2. Assert list loading, empty/error/retry, pagination, duplicate-load guards,
   and non-destructive load-more failure.
3. Assert detail performs no list request and retries the active ID.
4. Rename the DI module and combined ViewModel, then add the detail ViewModel.

**Acceptance criteria:**

- [x] Hilt binds `BillingRecordRepositoryImpl` to
  `BillingRecordRepository`.
- [x] List state owns initial load, refresh, append, and load-more flags.
- [x] Pages append in server order without client sorting.
- [x] Concurrent or duplicate `loadMore()` calls are ignored.
- [x] Load-more failure retains existing records and clears the loading flag.
- [x] Detail state owns one ID and never calls the list repository method.
- [x] Detail retry requests the same active ID.
- [x] User-visible repository failures use safe fallback text.
- [x] No combined Invoice/Billing ViewModel remains.

**Verification:**

- [ ] Add the intended RED list/detail coverage:
  `.\gradlew testDebugUnitTest --tests "*BillingRecordListViewModelTest" --tests "*BillingRecordDetailViewModelTest"`
- [ ] Remaining compiler failures are confined to screen/navigation consumers
  in Tasks 5–6; green execution occurs at Checkpoint A.

**Dependencies:** Task 3

**Files renamed and rewritten:**

- `app/src/main/java/com/eyecare/app/di/InvoiceModule.kt`
  → `app/src/main/java/com/eyecare/app/di/BillingRecordModule.kt`
- `app/src/main/java/com/eyecare/app/presentation/invoices/InvoiceViewModel.kt`
  → `app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordListViewModel.kt`

**Files added:**

- `app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/billingrecords/BillingRecordListViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/billingrecords/BillingRecordDetailViewModelTest.kt`

**Estimated scope:** Medium

### Task 5: Rewrite Billing Record screens and route types

**Description:** Replace the Invoice screens and routes with exact-money,
read-only Billing Record list/detail presentation and a Job Order action.

**Test-first steps:**

1. Add pure presentation tests for money, status, payment method, and nullable
   date formatting.
2. Rewrite the screens against the split ViewModels and minimal domain.
3. Rename the route types and their ID property.

**Acceptance criteria:**

- [x] List shows Billing Record number, safe status, total, paid amount,
  balance, and recorded date when present.
- [x] Detail shows the same financial summary and posted payment history.
- [x] Empty payments have explicit neutral copy.
- [x] Raw payment methods are safely humanized; unknown values do not crash.
- [x] Exact peso output is derived from `BigDecimal`, never floating point.
- [x] Detail exposes **View job order** with the record's `jobOrderId`.
- [x] No copied items, taxes, discounts, sold-to, official/BIR, issued, or
  receipt language appears.
- [x] Routes are `BillingRecordList` and
  `BillingRecordDetail(billingRecordId)`.
- [x] No `InvoiceScreens.kt` or Invoice presentation package remains.

**Verification:**

- [ ] Add the intended RED presentation coverage:
  `.\gradlew testDebugUnitTest --tests "*BillingRecordPresentationTest"`
- [ ] Remaining compiler failures are confined to Task 6 integration; green
  execution occurs at Checkpoint A.

**Dependencies:** Task 4

**File renamed and rewritten:**

- `app/src/main/java/com/eyecare/app/presentation/invoices/InvoiceScreens.kt`
  → `app/src/main/java/com/eyecare/app/presentation/billingrecords/BillingRecordScreens.kt`

**Files modified/added:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/test/java/com/eyecare/app/presentation/billingrecords/BillingRecordPresentationTest.kt`

**Estimated scope:** Large

### Task 6: Integrate Billing Records with navigation and Profile

**Description:** Complete the clean cutover by wiring Billing Record
destinations, Job Order navigation, bottom-bar behavior, and the Profile row.

**Test-first steps:**

1. Update Profile instrumented expectations and callback names first.
2. Confirm navigation/Profile compilation fails against old Invoice wiring.
3. Replace all Invoice imports, callbacks, destinations, and labels.

**Acceptance criteria:**

- [x] Profile displays **Billing Records**, never **Invoices**.
- [x] `onNavigateToBillingRecords` opens `BillingRecordList`.
- [x] List opens `BillingRecordDetail(id)`.
- [x] Detail opens `JobOrderDetail(jobOrderId)`.
- [x] Back behavior matches other read-only list/detail features.
- [x] Bottom navigation is hidden on Billing Record destinations.
- [x] Profile instrumented callback coverage compiles.
- [x] No Invoice production/test file, symbol, package, route, callback, or
  patient-facing text remains.

**Verification:**

- [ ] `.\gradlew testDebugUnitTest --tests "*BillingRecord*"`
- [ ] `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`
- [ ] Billing/Invoice source sweeps from Checkpoint A are clean.

**Dependencies:** Task 5

**Files modified:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Medium

## Checkpoint A — Billing Record Cutover Green

- [x] `.\gradlew testDebugUnitTest --tests "*BillingRecord*"`
- [x] `.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [x] `.\gradlew assembleDebugAndroidTest`
- [x] `.\gradlew assembleDebug`
- [x] Allowlisted and discovered Retrofit route counts are exactly 33.
- [x] Multi-page Billing Record list and detail retry work.
- [x] All four documented statuses plus unknown display safely.
- [x] Empty and populated payment history display correctly.
- [x] Job Order navigation works.
- [x] No Billing Record money passes through `Double` or `Float`.
- [x] No Invoice compatibility or retired concept remains.

Source sweeps:

```powershell
rg -n -i "Invoice|InvoiceList|InvoiceDetail|InvoiceApiService|InvoiceRepository|/invoices|@GET\\(\"invoices" app/src
rg -n "Double|Float" app/src/main/java/com/eyecare/app/data/remote/dto/BillingRecordDtos.kt app/src/main/java/com/eyecare/app/domain/model/BillingRecord.kt app/src/main/java/com/eyecare/app/presentation/billingrecords
```

Only after Checkpoint A is green may the first V11 implementation commit be
created:

```text
feat(V11): replace invoices with billing records
```

## Phase B — Appointment-Linked Frame Reservations

### Task 7: Replace nullable linkage with embedded Appointment data

**Description:** Write DTO tests, then make `appointment_id` required in the
create request and replace scalar nullable domain linkage with the embedded
Appointment returned by list, create, and cancel.

**Test-first steps:**

1. Add fixtures for list/create/cancel resource decoding.
2. Assert the create request serializes a required Appointment ID.
3. Cover embedded number, status, schedule, and duration plus unknown status.
4. Rewrite DTO and domain models.

**Acceptance criteria:**

- [x] `CreateReservationRequest.appointmentId` is non-null with no default.
- [x] Every reservation resource requires and decodes embedded `appointment`.
- [x] Transport may retain scalar `appointment_id` for contract decoding.
- [x] Domain exposes only `ReservationAppointment`, not scalar
  `appointmentId`.
- [x] Embedded Appointment status uses the safe Appointment status model.
- [x] Reservation item and exact-price mapping remain unchanged.
- [x] Unknown statuses fail closed.

**Verification:**

- [ ] Add the intended RED DTO coverage:
  `.\gradlew testDebugUnitTest --tests "*FrameReservationDtosTest"`
- [ ] If compilation stops in Tasks 8–13 consumers, confirm failures are
  limited to the scheduled cutover and defer green execution to Checkpoint B.

**Dependencies:** Checkpoint A

**Files modified/added:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/FrameReservationDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/FrameReservation.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/FrameReservationDtosTest.kt`

**Estimated scope:** Medium

### Task 8: Require Appointment IDs in the repository and map `422` fields

**Description:** Make the repository API non-null, map embedded Appointment
context, and decode documented validation responses into a domain error with
field errors.

**Test-first steps:**

1. Add MockWebServer repository tests for list, create, cancel, and failures.
2. Assert create serializes non-null `appointment_id` and one selected variant.
3. Assert list/create/cancel all map embedded Appointment data.
4. Assert `422` preserves field maps and malformed/non-422 failures fall back
   safely.
5. Implement the repository signature, mapping, `Json` injection, and error.

**Acceptance criteria:**

- [x] Repository create requires `appointmentId: Int` with no null/default.
- [x] Repository maps embedded Appointment for all three response paths.
- [x] Existing requested/prepared cancellation capability is unchanged.
- [x] A successful cancel replaces the resource with the returned state.
- [x] `HttpException(422)` is decoded through `ApiErrorBody`.
- [x] Field errors are exposed through
  `FrameReservationError.ValidationError`.
- [x] Repository does not parse error message prose.
- [x] Malformed validation JSON and non-422 failures remain safe.
- [x] No response body is logged.

**Verification:**

- [ ] Add the intended RED repository coverage:
  `.\gradlew testDebugUnitTest --tests "*FrameReservationRepositoryImplTest"`
- [ ] Remaining compiler failures are limited to Tasks 9–13; green execution
  occurs at Checkpoint B.

**Dependencies:** Task 7

**Files modified/added:**

- `app/src/main/java/com/eyecare/app/domain/repository/FrameReservationRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FrameReservationRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/domain/model/FrameReservationError.kt`
- `app/src/test/java/com/eyecare/app/data/repository/FrameReservationRepositoryImplTest.kt`

**Estimated scope:** Medium

### Task 9: Add pure Appointment eligibility policy

**Description:** Implement the approved eligibility rule as a deterministic,
side-effect-free policy operating on `AppointmentV1` and a supplied `Instant`.

**Test-first steps:**

1. Add boundary-focused eligibility tests.
2. Confirm failures because the policy does not exist.
3. Implement the smallest pure policy needed by reservation creation.

**Acceptance criteria:**

- [x] Scheduled future Appointment is eligible.
- [x] Scheduled Appointment whose end equals `now` is eligible.
- [x] Scheduled Appointment whose end is before `now` is ineligible.
- [x] Checked-in, fulfilled, cancelled, no-show, and unknown are ineligible.
- [x] Malformed or offset-less schedule values fail closed.
- [x] Invalid duration fails closed.
- [x] Offset-aware values are compared as instants, independent of display
  timezone.
- [x] Policy performs no I/O and is directly unit tested.

**Verification:**

- [ ] Add the intended RED eligibility coverage:
  `.\gradlew testDebugUnitTest --tests "*FrameReservationEligibilityTest"`
- [ ] If compilation is blocked by Tasks 10–13 consumers, confirm those are the
  only blockers and defer green execution to Checkpoint B.

**Dependencies:** Task 8

**Files added:**

- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationEligibility.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/FrameReservationEligibilityTest.kt`

**Estimated scope:** Small

### Task 10: Build all-page selection and field-aware creation state

**Description:** Rewrite the assisted creation ViewModel to load all
Appointment pages, present explicit selection state, submit only the selected
visit, and recover according to validation fields.

**Test-first steps:**

1. Add ViewModel tests for page traversal, eligibility, selection, submit
   guards, success, and error recovery.
2. Confirm the old ViewModel fails the new tests.
3. Inject `AppointmentV1Repository` and implement the state machine.

**Acceptance criteria:**

- [x] Pages load sequentially from page 1 through `lastPage`.
- [x] Appointments preserve server order and duplicate IDs are removed.
- [x] A later-page failure exposes a retryable load error, never a partial
  authoritative list.
- [x] Retry restarts from page 1.
- [x] Concurrent loads and duplicate submit taps are guarded.
- [x] Submit is impossible without an explicitly selected eligible
  Appointment.
- [x] Create sends exactly the assisted `variantId` and selected Appointment
  ID.
- [x] `appointment_id` field errors clear selection and reload appointments.
- [x] `items` and `items.*` errors retain selection.
- [x] Unknown validation fields and generic failures display safe text without
  leaking response content.
- [x] Booking return refresh can be requested without auto-selection or
  submission.

**Verification:**

- [ ] Add the intended RED state-machine coverage:
  `.\gradlew testDebugUnitTest --tests "*CreateFrameReservationViewModelTest"`
- [ ] Remaining compiler failures are confined to Tasks 11–13; green execution
  occurs at Checkpoint B.

**Dependencies:** Task 9

**Files modified/added:**

- `app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/CreateFrameReservationViewModelTest.kt`

**Estimated scope:** Large

### Task 11: Implement required Appointment selection UI

**Description:** Replace the optional-link confirmation screen with explicit
eligible-Appointment loading, selection, retry, booking, validation, and
confirmation states.

**Test-first steps:**

1. Extract stateless content inside the existing screen file where needed for
   Compose tests.
2. Add instrumented assertions for loading, retry, empty, populated,
   selection, validation, and submission callbacks.
3. Rewrite the screen against the Task 10 state machine.

**Acceptance criteria:**

- [x] Copy explains the frame will be prepared for the selected visit.
- [x] Loading and page-load failure have clear progress/retry UI.
- [x] Empty eligible state offers **Book appointment**.
- [x] Populated state shows number, schedule, duration, and safe status.
- [x] Exactly one Appointment can be selected.
- [x] Confirm is disabled until selection and while submitting.
- [x] Appointment field validation is shown near selection and stale selection
  is absent.
- [x] Item/generic validation remains actionable without clearing a valid
  selection.
- [x] Booking and success are callbacks; the screen does not implement
  navigation directly.
- [x] No optional-link copy or null submission remains.

**Verification:**

- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] If compilation is blocked, remaining failures are confined to Tasks
  12–13 and green execution occurs at Checkpoint B.

**Dependencies:** Task 10

**Files modified/added:**

- `app/src/main/java/com/eyecare/app/presentation/reservations/CreateFrameReservationScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/reservations/CreateFrameReservationScreenTest.kt`

**Estimated scope:** Large

### Task 12: Add the reservation booking-return destination

**Description:** Add the dedicated type-safe route, reuse the existing booking
screen, return to the live reservation draft, signal a refresh only after a
successful booking, and open history after reservation success.

**Acceptance criteria:**

- [x] `BookAppointmentForReservation` is a distinct type-safe destination.
- [x] It reuses `BookAppointmentScreen`.
- [x] Back without booking returns to the unchanged reservation draft.
- [x] Successful booking pops only the booking destination and signals the
  previous reservation entry to refresh.
- [x] Refresh does not select or submit automatically.
- [x] General `BookAppointment` still navigates to Appointments exactly as
  before.
- [x] Reservation success navigates to `FrameReservationList`.
- [x] The completed creation destination is removed from the back stack.
- [x] Bottom navigation remains hidden throughout both booking flows.
- [x] Backend route count is unaffected.

**Verification:**

- [ ] If focused or Android-test compilation is blocked, remaining failures are
  confined to Task 13 and green execution occurs at Checkpoint B.

**Dependencies:** Task 11

**Files modified:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

**Estimated scope:** Medium

### Task 13: Display Appointment context in Reservation history

**Description:** Cover list/cancel state and rewrite reservation cards to show
the authoritative embedded Appointment while preserving cancellation rules.

**Test-first steps:**

1. Add list ViewModel tests for load, empty, failure, retry, cancel replacement,
   and cancel failure.
2. Add Compose assertions for Appointment context and the cancellation matrix.
3. Implement state and card changes.

**Acceptance criteria:**

- [x] History cards show reservation ID and selected frame details correctly.
- [x] Cards show Appointment number, schedule, duration, and safe status.
- [x] Unknown or malformed display values fail safely.
- [x] Requested and prepared reservations expose cancellation.
- [x] Tried-on, converted, released, cancelled, and unknown do not.
- [x] Successful cancellation replaces the full returned resource, including
  embedded Appointment.
- [x] Cancellation failure does not discard the current list.
- [x] Empty, loading, refresh, and load-error behavior remain functional.

**Verification:**

- [ ] RED then implementation:
  `.\gradlew testDebugUnitTest --tests "*FrameReservationListViewModelTest"`
- [ ] `.\gradlew assembleDebugAndroidTest`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Task 12

**Files modified/added:**

- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/reservations/FrameReservationListScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/reservations/FrameReservationListViewModelTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/reservations/FrameReservationListScreenTest.kt`

**Estimated scope:** Medium

## Checkpoint B — Appointment-Linked Reservations Green

- [x] `.\gradlew testDebugUnitTest --tests "*FrameReservation*"`
- [x] `.\gradlew testDebugUnitTest --tests "*CreateFrameReservationViewModelTest"`
- [x] `.\gradlew assembleDebugAndroidTest`
- [x] `.\gradlew assembleDebug`
- [x] Create request cannot omit or null `appointment_id`.
- [x] All Appointment pages contribute to selection.
- [x] Eligibility boundary and fail-closed cases pass.
- [x] Booking returns to the existing draft without submitting.
- [x] Appointment and item field errors have distinct recovery behavior.
- [x] Success opens Reservation history.
- [x] History shows embedded Appointment context.
- [x] Cancellation capability and returned-resource replacement remain correct.

Source sweeps:

```powershell
rg -n "appointmentId: Int\\?|appointmentId: Int\\? = null|appointment_id.*null" app/src/main app/src/test app/src/androidTest
rg -n -i "optional appointment|appointment.*optional" app/src CONTEXT.md
```

Only after Checkpoint B is green may the second V11 implementation commit be
created:

```text
feat(V11): require appointment-linked frame reservations
```

## Phase C — Documentation and Release Verification

### Task 14: Synchronize project context and completed V10 metadata

**Description:** Update current Android documentation for V11 and correct V10
phase metadata from the already-completed V10 commit evidence.

**Acceptance criteria:**

- [x] `CONTEXT.md` documents Billing Records rather than Invoices.
- [x] Billing Records are described as read-only internal ledgers with exact
  money and Job Order linkage.
- [x] Reservation creation is documented as requiring an eligible Appointment.
- [x] Booking return, embedded history context, and the existing one-frame
  choice are documented.
- [x] Current route total remains 33.
- [x] V10 spec, plan, and tasks accurately say V10 implementation completed.
- [x] V10 completion metadata cites the existing implementation commits where
  useful.
- [x] User-owned backend docs and deleted standalone specs are not changed.

**Verification:**

- [ ] `git diff --check`
- [ ] `.\gradlew assembleDebug`

**Dependencies:** Checkpoints A–B

**Files modified:**

- `CONTEXT.md`
- `docs/specs/backend-alignment-v10-spec.md`
- `docs/specs/backend-alignment-v10-plan.md`
- `docs/specs/backend-alignment-v10-tasks.md`

**Estimated scope:** Medium

### Task 15: Run V11 source-contract sweeps and finalize phase metadata

**Description:** Prove the clean cutovers in current source/docs and update V11
documents only after implementation evidence exists.

**Acceptance criteria:**

- [x] No Invoice source, package, route, callback, test, or current-context
  wording remains.
- [x] No Billing Record money type uses `Double` or `Float`.
- [x] No nullable/default reservation Appointment linkage remains.
- [x] No optional-Appointment creation copy remains.
- [x] Deleted backend standalone specifications remain deleted.
- [x] Route allowlist/discovery still reports 33.
- [x] V11 spec, plan, and task statuses match actual completed checkpoints.

**Verification:**

```powershell
rg -n -i "Invoice|InvoiceList|InvoiceDetail|InvoiceApiService|InvoiceRepository|/invoices|@GET\\(\"invoices" app/src CONTEXT.md
rg -n "Double|Float" app/src/main/java/com/eyecare/app/data/remote/dto/BillingRecordDtos.kt app/src/main/java/com/eyecare/app/domain/model/BillingRecord.kt app/src/main/java/com/eyecare/app/presentation/billingrecords
rg -n "appointmentId: Int\\?|appointmentId: Int\\? = null|appointment_id.*null" app/src/main app/src/test app/src/androidTest
rg -n -i "optional appointment|appointment.*optional" app/src CONTEXT.md
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
git diff --check
.\gradlew assembleDebug
```

Expected `rg` result: no forbidden matches. Review legitimate historical
language in archived specifications separately; the sweep targets current
source and `CONTEXT.md`.

**Dependencies:** Task 14

**Files modified:**

- `docs/specs/backend-alignment-v11-spec.md`
- `docs/specs/backend-alignment-v11-plan.md`
- `docs/specs/backend-alignment-v11-tasks.md`

**Estimated scope:** Small

### Task 16: Run the complete V11 release gate

**Description:** Execute all automated verification, inspect the final diff,
perform the manual Billing Record/reservation smoke matrix, and record only
genuine environment limitations.

**Acceptance criteria:**

- [x] Full unit suite passes.
- [x] Android test APK compiles.
- [x] Ktlint passes.
- [x] Android lint passes.
- [x] Debug APK assembles.
- [x] Route allowlist and discovery both report 33.
- [ ] Manual Billing Record checks pass.
- [ ] Manual Appointment-linked reservation checks pass.
- [ ] General Appointment booking, Profile, bottom navigation, Job Order
  detail, and frame cancellation regressions pass.
- [x] Final diff contains no compatibility layer, backend implementation, Room
  change, or new dependency.
- [x] User-owned backend changes are not staged in Android commits.

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

If an emulator or device is available:

```powershell
.\gradlew connectedDebugAndroidTest
```

**Billing Record manual smoke checklist:**

- [ ] Empty, single-page, and multi-page list states.
- [ ] Initial failure, retry, load more, and load-more failure.
- [ ] Unpaid, partially paid, paid, voided, and unknown status.
- [ ] Exact totals, paid amount, and balance.
- [ ] Null and populated recorded dates.
- [ ] Empty and populated payment history.
- [ ] Unknown raw payment method.
- [ ] Detail retry and Job Order navigation.
- [ ] No official invoice, receipt, BIR, tax, discount, sold-to, issued, or
  copied-item presentation.

**Reservation manual smoke checklist:**

- [ ] Eligible choices load across multiple Appointment pages.
- [ ] Exact-end Appointment remains eligible.
- [ ] Past, non-scheduled, malformed, and unknown Appointments are excluded.
- [ ] Empty state can book and return to the same draft.
- [ ] Back from booking preserves the draft.
- [ ] Successful booking refreshes but never auto-selects or auto-submits.
- [ ] Confirmation is impossible without selection.
- [ ] Appointment field error clears/reloads selection.
- [ ] Item field error retains selection.
- [ ] Success opens history with Appointment number, schedule, duration, and
  status.
- [ ] Requested/prepared cancellation succeeds.
- [ ] Other statuses expose no cancellation action.

**Dependencies:** Task 15

**Files modified:** None unless verification exposes an in-scope defect. Any
fix requires a focused regression test and rerunning the affected checkpoint.

**Estimated scope:** Medium

## Checkpoint C — V11 Complete

- [x] Checkpoint A passed and its feature commit exists.
- [x] Checkpoint B passed and its feature commit exists.
- [x] Tasks 14–15 documentation and source proofs passed.
- [x] Task 16 automated and available manual verification passed.
- [x] `CONTEXT.md` matches implemented V11 behavior.
- [x] V10 and V11 metadata reflects actual completion.
- [x] Backend source documents were not altered by Android implementation.
- [x] Deleted standalone backend specs were not restored.

After Checkpoint C, create the documentation commit:

```text
docs(V11): update Android context and verification status
```

## Dependency Summary

```text
Task 1: Billing route red proof
    -> Task 2: Billing DTO/domain
        -> Task 3: Billing service/repository
            -> Task 4: Billing DI/list/detail state
                -> Task 5: Billing screens/routes
                    -> Task 6: Billing navigation/Profile
                        -> Checkpoint A
                            -> Task 7: reservation DTO/domain
                                -> Task 8: repository/error mapping
                                    -> Task 9: eligibility policy
                                        -> Task 10: creation state
                                            -> Task 11: creation UI
                                                -> Task 12: booking return
                                                    -> Task 13: history/cancel
                                                        -> Checkpoint B
                                                            -> Task 14: context/V10 metadata
                                                                -> Task 15: V11 sweeps/metadata
                                                                    -> Task 16: release gate
                                                                        -> Checkpoint C
```

## Task Sizing

| Task | Scope | Primary risk |
|---|---|---|
| 1 | Small | Route red proof detects the exact replacement |
| 2 | Medium | Exact money and minimal DTO/domain shape |
| 3 | Medium | Pagination and clean endpoint replacement |
| 4 | Medium | State split and redundant requests |
| 5 | Large | Exact-money presentation without retired concepts |
| 6 | Medium | Shared navigation/Profile integration |
| 7 | Medium | Required transport linkage and embedded domain data |
| 8 | Medium | Structured validation and response mapping |
| 9 | Small | Inclusive instant boundary and fail-closed parsing |
| 10 | Large | All-page loading and field-aware recovery |
| 11 | Large | Required selection UI state density |
| 12 | Medium | Back-stack preservation and no auto-submit |
| 13 | Medium | Embedded context and cancellation regression |
| 14 | Medium | Accurate current context and historical metadata |
| 15 | Small | Correctly scoped retirement proofs |
| 16 | Medium | Cross-feature release verification |

## Phase Gate

Phases 1–3 were approved by the project owner on 2026-07-28.

Implementation has not started and is explicitly paused by the project owner.
Do not modify Android production code until the project owner gives a separate
instruction to proceed.
