# Backend Alignment V10 — Phase 2 Implementation Plan

Status: Approved — Phases 2–3 complete (2026-07-28); implementation not started

Specification:
`docs/specs/backend-alignment-v10-spec.md`

Baseline: Android V9 completed at commit `dfa9969`

## Overview

V10 is a clean Android cutover for two changes already present in the current
patient API:

1. versioned prescriptions with nested Main/ADD and OD/OS measurements; and
2. retirement of clinic feedback and its `/feedback` route.

Billing Records and required Appointment linkage for Frame Reservations are not
part of this plan.

Implementation is divided into four test-first product stages followed by final
documentation and verification. Stages 1–3 are one compiler-coupled
prescription slice because replacing the domain model intentionally breaks all
of its consumers; they are executed in order but reach their shared green build
checkpoint after Stage 3. Stage 4 and Stage 5 each finish green independently.

## Architecture Decisions

### 1. Use nested domain measurements

The API structure is preserved through explicit DTO and domain types:

```text
Prescription
└── PrescriptionMeasurements
    ├── main: PrescriptionMeasurementGroup
    │   ├── od: EyeMeasurement
    │   └── os: EyeMeasurement
    └── add: PrescriptionMeasurementGroup
        ├── od: EyeMeasurement
        └── os: EyeMeasurement
```

`EyeMeasurement` contains nullable `value`, `sphere`, and `cylinder` strings.
The repository performs the complete DTO-to-domain conversion. Compose never
receives transport DTOs.

No compatibility properties are added for the old flat fields. Compile
failures after changing the domain model are used to enumerate every required
consumer migration.

### 2. Preserve server ordering for the paginated list

`GET /prescriptions` is authoritative for the current-version list. The list
ViewModel preserves response order and appends later pages in response order.
It does not re-sort the accumulated list on every page.

This avoids cross-page reordering and assumes no undocumented client ordering
responsibility. Home may independently select the maximum ISO `date` from page
1 because it needs a single deterministic current summary.

### 3. Split list and detail ViewModels

The current `PrescriptionViewModel` initializes a list request even when used
on the detail destination. Its detail retry also calls `refresh()`, which
refreshes the list rather than the failed detail.

Replace it with:

- `PrescriptionListViewModel` — initial list, refresh, and pagination;
- `PrescriptionDetailViewModel` — load and retry one prescription ID.

Benefits:

- opening a prescription performs one detail request rather than an
  unnecessary list request plus detail request;
- detail retry is unambiguous;
- historical-version navigation creates a detail destination with isolated
  state;
- list and detail tests have smaller responsibilities.

The detail screen continues receiving `prescriptionId` from the type-safe
route. `LaunchedEffect(prescriptionId)` calls the detail ViewModel, and retry
calls the same load method with that ID.

### 4. Navigate through history lazily

The list exposes only current versions. A current detail can navigate to
`previousPrescriptionId`; each tap pushes another `PrescriptionDetail` route.

Android does not prefetch the version chain. This keeps network use
proportional to what the patient opens and preserves ordinary Back navigation
from older to newer versions.

### 5. Home remains partially fault tolerant

Home keeps its three independent concurrent requests:

- Appointments;
- Frames; and
- page 1 of Prescriptions.

`expiringPrescription` becomes `currentPrescription`. A prescription failure
produces no prescription card but does not hide available Appointment or Frame
content.

Home adds an `onNavigateToPrescriptionDetail: (Int) -> Unit` callback. The main
navigation graph maps it to the existing `PrescriptionDetail` destination, so
no new route type is required.

### 6. Delete Feedback vertically

Feedback is removed through every architecture layer in the same stage:

```text
Retrofit route
    -> DTO
    -> repository implementation/interface
    -> domain model
    -> Hilt module
    -> ViewModel/screen
    -> route/navigation callback
    -> Appointment capability/action
    -> tests and documentation
```

There is no stub, hidden destination, deprecated wrapper, or replacement
action.

### 7. Preserve Intake while removing Feedback

Appointment detail currently uses feedback eligibility to decide whether to
show the bottom action container. That container also holds **View Intake**.

After Feedback removal, bottom-action visibility must preserve the current
non-feedback behavior:

```text
scheduled   -> Reschedule, Cancel appointment, View Intake
checked_in  -> Cancel appointment, View Intake
fulfilled   -> View Intake
cancelled   -> no bottom action container
no_show     -> no bottom action container
unknown     -> no bottom action container
```

Use a presentation-level `showBottomActions` decision based on the remaining
actions. Do not add a new backend mutation or reintroduce feedback as a reason
to render the container.

### 8. Treat source sweeps as contract assertions

After migration:

- no Feedback symbol, route, package, callback, or documentation entry remains;
- no prescription consumer refers to removed flat clinical fields;
- unrelated `expiresAt` fields belonging to Frame Reservations are retained;
- the frame-rating route remains present; and
- Retrofit discovery equals the 33-route allowlist.

The sweeps complement compile/tests; they do not replace behavioral tests.

## Component Dependencies

```text
Prescription contract fixture
    -> Prescription DTOs
        -> Prescription domain model
            -> PrescriptionRepositoryImpl mapping
                ├── PrescriptionListViewModel
                │   └── PrescriptionListScreen
                ├── PrescriptionDetailViewModel
                │   └── PrescriptionDetailScreen
                │       └── previous-version navigation
                └── HomeViewModel
                    └── HomeScreen
                        └── Home -> PrescriptionDetail navigation

33-route allowlist
    -> delete Feedback Retrofit service
        -> delete Feedback data/domain/DI
            -> delete Feedback presentation
                -> remove Feedback route
                    -> remove Appointment feedback action/capability
                        -> preserve View Intake action container
```

The new prescription domain and mapping must land before either prescription
presentation or Home can compile. Feedback deletion is structurally independent
of prescription transport but touches `NavGraph.kt` alongside Home navigation;
implement it after prescription navigation to minimize overlapping edits.

## Implementation Order

### Stage 0 — Baseline and contract evidence

Purpose: prove failures introduced later belong to V10 and protect the user's
existing documentation changes.

Actions:

1. Record `git status --short` and preserve the user's modified/untracked
   backend documents.
2. Confirm V9 commit `dfa9969` remains in history.
3. Re-read the prescription and retired-feature sections of
   `docs/API_CONTRACT.md`.
4. Run the existing debug build and unit suite:

   ```powershell
   .\gradlew testDebugUnitTest
   .\gradlew assembleDebug
   ```

5. Attempt `.\gradlew assembleDebugAndroidTest` to identify whether the existing
   stale Home/Profile instrumented tests already fail before V10.
6. Record baseline failures without broadening V10 into unrelated repair work.
   Tests directly affected by prescriptions or Feedback are repaired in this
   plan.

Exit criteria:

- baseline results are known;
- no production file has changed;
- user-owned backend-document edits remain untouched.

### Stage 1 — Prescription contract, domain, and repository

Purpose: establish a proven domain boundary for the new API shape before
changing presentation.

#### 1A. Write failing DTO tests

Add:

```text
app/src/test/java/com/eyecare/app/data/remote/dto/PrescriptionDtosTest.kt
```

Cover:

- documented paginated current-version response;
- documented detail response;
- `previous_prescription_id`;
- `is_current`;
- Main and ADD groups;
- OD and OS `value`, `sphere`, and `cylinder`;
- every measurement being null;
- required integer `appointment_id`, matching the approved V10 domain decision;
- nullable/blank `remarks`;
- `links` and `meta`.

Use the production Kotlinx `Json` configuration through the existing contract
fixture convention.

The test must initially fail because the current DTO expects flat fields.

#### 1B. Write failing repository mapping tests

Add:

```text
app/src/test/java/com/eyecare/app/data/repository/PrescriptionRepositoryImplTest.kt
```

Use the existing MockWebServer + Retrofit pattern. Cover:

- list request path and `page` query;
- pagination mapping;
- nested Main/ADD mapping;
- detail mapping for a current record;
- detail mapping for a superseded record;
- null preservation.

#### 1C. Replace production contract types

Modify:

```text
app/src/main/java/com/eyecare/app/data/remote/dto/PrescriptionDtos.kt
app/src/main/java/com/eyecare/app/domain/model/Prescription.kt
app/src/main/java/com/eyecare/app/data/repository/PrescriptionRepositoryImpl.kt
```

Keep unchanged unless compilation proves otherwise:

```text
app/src/main/java/com/eyecare/app/data/remote/api/PrescriptionApiService.kt
app/src/main/java/com/eyecare/app/domain/repository/PrescriptionRepository.kt
app/src/main/java/com/eyecare/app/di/PrescriptionModule.kt
```

Implementation rules:

- use `@Serializable` and `@SerialName`;
- define distinct DTO and domain nested types;
- map each nested object explicitly;
- do not add defaults that manufacture missing required wrapper objects;
- keep individual clinical values nullable;
- remove every old flat field rather than deprecating it.

#### 1D. Confirm the intended red state

Run the focused tests after writing them:

```powershell
.\gradlew testDebugUnitTest --tests "*PrescriptionDtosTest"
.\gradlew testDebugUnitTest --tests "*PrescriptionRepositoryImplTest"
```

Expected intermediate state:

- tests or main compilation fail for the intended missing nested contract and
  legacy consumer references;
- there are no unrelated new failures.

Stage exit criteria:

- the new contract/domain/repository implementation is present;
- the targeted tests express the correct mapping;
- compiler failures identify only the presentation/Home consumers scheduled
  for Stages 2–3;
- no compatibility fields are introduced to make the intermediate tree green.

### Stage 2 — Prescription list, detail, and history

Purpose: migrate the patient-facing prescription workflow and remove redundant
detail network work.

#### 2A. Split ViewModel tests

Replace or split:

```text
app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModelTest.kt
```

Target files:

```text
PrescriptionListViewModelTest.kt
PrescriptionDetailViewModelTest.kt
```

List tests:

- initial success and empty states;
- initial error and retry;
- server order preservation;
- page append order;
- `hasMorePages`;
- duplicate load-more prevention;
- load-more failure restores `isLoadingMore = false` without discarding data.

Detail tests:

- starts without triggering a list request;
- loads a current version;
- loads a historical version;
- exposes previous ID through the domain result;
- retries the same ID after failure;
- reports a safe fallback error.

Delete expiry and `isExpired` tests.

#### 2B. Split production ViewModels

Replace:

```text
app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModel.kt
```

with:

```text
app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionListViewModel.kt
app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionDetailViewModel.kt
```

The list ViewModel owns pagination only. The detail ViewModel owns one
`PrescriptionDetailUiState` and `load(id)`.

Do not sort accumulated pages locally. Do not retain `isExpired`.

#### 2C. Rewrite prescription screens

Modify:

```text
app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionScreens.kt
```

List:

- wire `PrescriptionListViewModel`;
- preserve loading, pull-to-refresh, error, empty, and load-more states;
- replace validity badges with date and Main OD/OS summaries;
- use a shared missing-value formatter returning an em dash.

Detail:

- wire `PrescriptionDetailViewModel`;
- add `onNavigateToPrevious: (Int) -> Unit`;
- load and retry the requested ID;
- show current/previous version label;
- render Main and ADD sections;
- render OD and OS values, sphere, and cylinder;
- conditionally render remarks;
- remove validity colors, warnings, and all legacy measurements;
- show **View previous version** only when linkage exists.

Prefer small private composables such as:

```text
PrescriptionMeasurementSection
EyeMeasurementCard
MeasurementValue
```

Do not introduce a generalized clinical-component framework.

#### 2D. Wire version navigation

Modify:

```text
app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt
```

For every `PrescriptionDetail` destination:

```text
onNavigateToPrevious(id) -> navigate(PrescriptionDetail(id))
```

The existing route type in `Routes.kt` remains unchanged.

#### 2E. Confirm the remaining Home-only red state

Run:

```powershell
.\gradlew testDebugUnitTest --tests "*Prescription*"
.\gradlew assembleDebug
```

At this compiler-coupled checkpoint, any remaining failure must be limited to
Home's references to the removed prescription fields. Prescription-specific
tests should otherwise be ready to pass once Stage 3 completes the last
consumer migration.

Stage exit criteria:

- list and detail use only the new model;
- detail opens without an automatic list request;
- version history works lazily;
- removed clinical/validity UI is absent;
- remaining compile failures are limited to the Home consumer handled in
  Stage 3.

### Stage 3 — Home current-prescription summary

Purpose: replace unsupported expiry behavior without adding network requests.

#### 3A. Update Home ViewModel tests first

Modify:

```text
app/src/test/java/com/eyecare/app/presentation/home/HomeViewModelTest.kt
```

Replace expiry fixtures/tests with:

- latest current prescription is selected by ISO `date`;
- a non-current record is ignored defensively;
- empty prescriptions produce `currentPrescription = null`;
- prescription failure leaves Appointment and Frame content available;
- the existing Appointment and Frame behavior remains unchanged.

#### 3B. Update Home state and selection

Modify:

```text
app/src/main/java/com/eyecare/app/presentation/home/HomeViewModel.kt
```

Changes:

- rename `expiringPrescription` to `currentPrescription`;
- remove prescription expiry parsing and the associated `LocalDate` logic;
- retain date handling needed for Appointments;
- select `prescriptions.filter(Prescription::isCurrent).maxByOrNull { it.date }`;
- keep the current independent request/failure behavior.

#### 3C. Replace the Home card

Modify:

```text
app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt
```

Changes:

- add `onNavigateToPrescriptionDetail`;
- replace `PrescriptionWarningCard` with `CurrentPrescriptionCard`;
- show the prescription date and a **View details** action;
- remove renewal/expiry language and the Book exam action from this card;
- retain the current Home spacing and visual language.

#### 3D. Wire Home navigation

Modify:

```text
app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt
```

Map the new Home callback to:

```text
navController.navigate(PrescriptionDetail(prescriptionId))
```

#### 3E. Repair relevant instrumented Home coverage

Modify:

```text
app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt
```

The existing file still contains pre-V8 models and state fields. Replace
relevant fixtures with current `AppointmentV1`, `Frame`, and `HomeUiState`.
Cover:

- current-prescription card visibility;
- absence when null;
- View details callback receives the prescription ID;
- no expiry/renewal copy;
- existing Appointment and Frame callbacks.

Do not restore retired Order or Product Home sections merely to preserve stale
tests.

#### 3F. Verify Home

Run:

```powershell
.\gradlew testDebugUnitTest --tests "*HomeViewModelTest"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Run the prescription manual checks now that the full slice compiles:

- current prescription with all values;
- partial/null measurements;
- ADD group with no values;
- non-blank and blank remarks;
- current -> previous -> previous -> Back -> Back;
- detail error and retry;
- multi-page list.

Stage exit criteria:

- Home contains no prescription expiry logic;
- current prescription opens its detail;
- partial failures remain isolated;
- relevant Home instrumented tests compile;
- the complete Stages 1–3 prescription slice reaches its first green focused
  tests and debug assembly.

### Stage 4 — Remove clinic Feedback and enforce 33 routes

Purpose: delete the retired capability across every layer without regressing
Appointment Intake or frame ratings.

#### 4A. Make the allowlist test fail first

Modify:

```text
app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt
app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt
```

Changes:

- remove `POST /api/v1/feedback`;
- update comments and assertions from 34 to 33.

Run the route test before deleting the service. It should fail because Retrofit
discovery still finds the retired route, proving the test detects the mismatch.

Keep:

```text
POST /api/v1/job-order-items/{item}/rating
```

#### 4B. Delete the Feedback vertical

Delete:

```text
app/src/main/java/com/eyecare/app/data/remote/api/FeedbackApiService.kt
app/src/main/java/com/eyecare/app/data/remote/dto/FeedbackDtos.kt
app/src/main/java/com/eyecare/app/data/repository/FeedbackRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/Feedback.kt
app/src/main/java/com/eyecare/app/domain/repository/FeedbackRepository.kt
app/src/main/java/com/eyecare/app/di/FeedbackModule.kt
app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackScreen.kt
app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackViewModel.kt
app/src/test/java/com/eyecare/app/presentation/feedback/FeedbackViewModelTest.kt
```

Remove the empty Feedback directories if they contain no other files.

#### 4C. Remove Appointment feedback behavior

Modify:

```text
app/src/main/java/com/eyecare/app/domain/model/AppointmentV1.kt
app/src/test/java/com/eyecare/app/domain/model/AppointmentStatusTest.kt
app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt
app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt
```

Changes:

- delete `canLeaveFeedback`;
- delete its capability assertions;
- delete `hasFeedback`;
- remove `onLeaveFeedback`;
- remove feedback-specific icon, label, layout, and bottom-padding branches;
- preserve **View Intake** for scheduled, checked-in, and fulfilled states;
- update the Appointment detail preview/call sites.

Do not change cancellation or rescheduling rules.

#### 4D. Remove navigation

Modify:

```text
app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt
app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt
```

Changes:

- delete `FeedbackSubmit`;
- delete the Feedback screen import and composable;
- remove the Appointment feedback callback;
- remove the `route.contains("Feedback")` bottom-navigation exception.

#### 4E. Repair relevant Profile instrumented coverage

Modify:

```text
app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt
```

Remove stale Feedback History callback, expected destination, and label
assertions. Align remaining test construction with the current Profile API only
as necessary for compilation.

#### 4F. Verify retirement

Run:

```powershell
.\gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"
.\gradlew testDebugUnitTest --tests "*AppointmentStatusTest"
.\gradlew testDebugUnitTest --tests "*FrameRating*"
.\gradlew assembleDebugAndroidTest
.\gradlew assembleDebug
```

Run source sweeps:

```powershell
rg -n -i "FeedbackSubmit|FeedbackApiService|FeedbackRepository|canLeaveFeedback|hasFeedback|@POST\\(\"feedback\"\\)" app/src
rg -n "POST .*/feedback" app/src
```

Expected result: no clinic-feedback matches. Human-readable historical commit
messages are irrelevant; source and current documentation are the target.

Stage exit criteria:

- route test reports exactly 33;
- no Feedback vertical remains;
- frame rating remains covered;
- fulfilled Appointment retains View Intake;
- Appointment cancellation/rescheduling behavior is unchanged.

### Stage 5 — Documentation and complete verification

Purpose: synchronize project context and prove the finished V10 baseline.

#### 5A. Update project context

Modify:

```text
CONTEXT.md
docs/specs/backend-alignment-v10-spec.md
docs/specs/backend-alignment-v10-plan.md
```

`CONTEXT.md` updates:

- remove Feedback package, route, and feature references;
- document the 33-route contract;
- document versioned prescription fields and history;
- replace the Home expiring-prescription description with current
  prescription;
- state that frame ratings remain;
- leave Invoice and current Frame Reservation behavior unchanged because those
  future changes are deferred.

Mark the V10 spec/plan phases complete only when their corresponding work is
actually complete.

Do not modify the user-supplied backend documents.

#### 5B. Run complete source sweeps

Feedback:

```powershell
rg -n -i "FeedbackSubmit|FeedbackApiService|FeedbackRepository|canLeaveFeedback|hasFeedback|@POST\\(\"feedback\"\\)" app/src CONTEXT.md
```

Legacy prescription names:

```powershell
rg -n "odSphere|odCylinder|odAxis|odAdd|odPrism|odBase|osSphere|osCylinder|osAxis|osAdd|osPrism|osBase|prescribedAt|expiringPrescription|PrescriptionWarningCard|validityStatus" app/src CONTEXT.md
```

Review any `expiresAt` match manually because Frame Reservations legitimately
retain that field.

#### 5C. Run final verification

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew assembleDebug
```

If an emulator/device is available:

```powershell
.\gradlew connectedDebugAndroidTest
```

Manual smoke test:

1. open the Prescriptions list;
2. verify a current version summary;
3. open a partial/null measurement record;
4. traverse at least two previous versions and return with Back;
5. retry a failed detail request;
6. open a prescription from Home;
7. open scheduled, checked-in, fulfilled, cancelled, and no-show Appointment
   details;
8. verify no clinic-feedback action exists;
9. verify View Intake remains on fulfilled;
10. verify a dispensed frame rating remains reachable and functional.

Stage exit criteria:

- all automated gates pass or any environment-only limitation is documented;
- no forbidden source matches remain;
- `CONTEXT.md` matches implemented V10;
- `git diff --check` passes;
- debug APK assembles.

## Expected File Groups

### New files

```text
app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionListViewModel.kt
app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionDetailViewModel.kt
app/src/test/java/com/eyecare/app/data/remote/dto/PrescriptionDtosTest.kt
app/src/test/java/com/eyecare/app/data/repository/PrescriptionRepositoryImplTest.kt
app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionListViewModelTest.kt
app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionDetailViewModelTest.kt
```

### Deleted files

```text
app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModel.kt
app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModelTest.kt
app/src/main/java/com/eyecare/app/data/remote/api/FeedbackApiService.kt
app/src/main/java/com/eyecare/app/data/remote/dto/FeedbackDtos.kt
app/src/main/java/com/eyecare/app/data/repository/FeedbackRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/Feedback.kt
app/src/main/java/com/eyecare/app/domain/repository/FeedbackRepository.kt
app/src/main/java/com/eyecare/app/di/FeedbackModule.kt
app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackScreen.kt
app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackViewModel.kt
app/src/test/java/com/eyecare/app/presentation/feedback/FeedbackViewModelTest.kt
```

### Modified production files

```text
app/src/main/java/com/eyecare/app/data/remote/dto/PrescriptionDtos.kt
app/src/main/java/com/eyecare/app/data/repository/PrescriptionRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/Prescription.kt
app/src/main/java/com/eyecare/app/domain/model/AppointmentV1.kt
app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionScreens.kt
app/src/main/java/com/eyecare/app/presentation/home/HomeViewModel.kt
app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt
app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt
app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt
app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt
app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt
```

### Modified test and documentation files

```text
app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt
app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt
app/src/test/java/com/eyecare/app/domain/model/AppointmentStatusTest.kt
app/src/test/java/com/eyecare/app/presentation/home/HomeViewModelTest.kt
app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt
app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt
CONTEXT.md
docs/specs/backend-alignment-v10-spec.md
docs/specs/backend-alignment-v10-plan.md
```

## Test-First Sequence

Within each production slice:

1. add or change the smallest relevant test;
2. run it and confirm failure for the intended missing behavior;
3. implement the minimum coherent production change;
4. run the focused test;
5. run adjacent regressions;
6. run `.\gradlew assembleDebug`;
7. inspect the diff before continuing.

Deletion-specific proof:

- change the allowlist to 33 first;
- confirm the route test detects the still-present Feedback service;
- delete the service and remaining vertical;
- confirm compilation and route discovery pass.

## Suggested Commit Boundaries

Commit only after the corresponding focused tests and debug assembly pass:

1. `feat(V10): migrate versioned prescriptions across the app`
2. `refactor(V10): retire clinic feedback and enforce 33 routes`
3. `docs(V10): update Android context and verification status`

Do not include the user's modified backend documents in Android implementation
commits unless the user explicitly asks to commit them.

## Parallel and Sequential Work

The work is best executed sequentially in one branch because:

- changing `Prescription` intentionally breaks every consumer;
- prescription Home navigation and Feedback removal both modify `NavGraph.kt`;
- the source sweeps and final route count depend on the fully integrated tree.

Logical independence still informs review:

- Stage 1 is transport/domain;
- Stages 2–3 are presentation consumers;
- Stage 4 is feature retirement;
- Stage 5 is documentation and release verification.

No subagent or worktree split is required by this plan.

## Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Old and new prescription fields are accidentally mixed | Replace the domain type once, allow compiler failures to expose consumers, and run legacy-name sweeps. |
| Pagination order changes after local sorting | Preserve server order and append pages without sorting. |
| Detail screen makes an unnecessary list request | Split list and detail ViewModels. |
| Detail Retry refreshes the wrong resource | Detail ViewModel reloads the active ID directly. |
| Empty measurement strings create misleading cards | Preserve values, render missing content as an em dash, and cover partial/null fixtures. |
| Version navigation loops because of bad backend linkage | Android follows only the supplied previous ID one step at a time; backend owns chain integrity. |
| Feedback deletion hides View Intake for fulfilled visits | Make bottom-container visibility explicit and manually test every Appointment status. |
| Generic `expiresAt` sweep removes reservation expiry | Limit migration to Prescription symbols and manually inspect generic matches. |
| Frame ratings are confused with retired clinic feedback | Keep the rating route in the allowlist and run focused rating tests. |
| Stale instrumented tests mask the new behavior | Repair only Home/Profile tests directly affected by V10 and compile the Android test APK. |
| User-supplied backend docs are committed accidentally | Stage Android files explicitly and inspect staged paths before any future commit. |

## Verification Matrix

| Area | Focused proof | Final proof |
|---|---|---|
| Prescription DTO | `PrescriptionDtosTest` | Full unit suite |
| Repository mapping | `PrescriptionRepositoryImplTest` | Full unit suite |
| List state/pagination | `PrescriptionListViewModelTest` | Full unit suite |
| Detail/history/retry | `PrescriptionDetailViewModelTest` | Manual chain smoke test |
| Home selection | `HomeViewModelTest` | Home instrumented test/APK |
| Feedback route removal | `ApiRouteAllowlistTest` | Route/source sweep |
| Appointment capabilities | `AppointmentStatusTest` | Status smoke matrix |
| Frame-rating preservation | `*FrameRating*` tests | Manual rating smoke test |
| Formatting/style | `ktlintCheck` | `lintDebug` |
| Integration | focused `assembleDebug` | final `assembleDebug` |

## Open Questions

None. The approved V10 specification resolves all product and contract
decisions needed for this plan.

## Phase Gate

This Phase 2 implementation plan and its Phase 3 task breakdown were approved
by the project owner on 2026-07-28.

Implementation has not started. Do not begin it until the project owner gives
a separate instruction to proceed.
