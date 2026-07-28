# Backend Alignment V9 — Appointment Lifecycle Vocabulary

Status: Approved — Phases 1–2 complete (2026-07-27); Phase 3 tasks in review

## Objective

Align the completed Android V8 patient workflow with the appointment lifecycle
introduced by backend commit `579b964`.

The adjustment replaces the old appointment status vocabulary everywhere in
Android and applies the backend's new status-dependent permissions:

- `scheduled` appointments may be cancelled or rescheduled;
- `checked_in` appointments may be cancelled but not rescheduled;
- `fulfilled` appointments may receive patient feedback;
- `cancelled` and `no_show` remain terminal;
- unknown values are displayed safely and never enable a patient mutation.

Success means Android decodes, presents, filters, and acts on the new lifecycle
without retaining a legacy compatibility path.

## Source of Truth and Assumptions

1. `docs/API_CONTRACT.md` at backend commit `579b964` is authoritative for the
   mobile API.
2. `docs/BACKEND_CONTEXT.md` at the same revision provides supporting backend
   domain context.
3. The Android app is not deployed, so `pending`, `confirmed`, `arrived`, and
   `completed` do not require aliases or migration support.
4. Backend status `fulfilled` is shown to patients as **Completed**. Transport
   and domain values remain `fulfilled`/`FULFILLED`.
5. Android represents an unexpected backend status as `UNKNOWN`. It must not
   silently map to an actionable status.
6. A checked-in appointment remains an active appointment for Home and
   appointment-list purposes, but only cancellation is available.
7. The backend encounter rename from `waiting` to `planned` is out of Android
   scope because the approved mobile API exposes no encounter route or model.

Approval of this specification confirms these assumptions.

## Contract Delta

Backend contract changed from commit `ebd1e2e` to `579b964`.

| Previous value | New value | Android disposition |
|---|---|---|
| `pending` | `scheduled` | Remove old value; decode only `scheduled`. |
| `confirmed` | `scheduled` | Remove the separate confirmation state. |
| `arrived` | `checked_in` | Replace with checked-in state. |
| `completed` | `fulfilled` | Replace domain value; display as “Completed.” |
| `cancelled` | `cancelled` | Retain. |
| `no_show` | `no_show` | Retain. |

The table documents conceptual lifecycle replacement only. Android must not
accept the previous strings as runtime aliases.

### Capability matrix

| Status | Active/upcoming | Cancel | Reschedule | Feedback | Patient label |
|---|---:|---:|---:|---:|---|
| `scheduled` | Yes | Yes | Yes | No | Scheduled |
| `checked_in` | Yes | Yes | No | No | Checked in |
| `fulfilled` | No | No | No | Yes | Completed |
| `cancelled` | No | No | No | No | Cancelled |
| `no_show` | No | No | No | No | No show |
| unknown | No | No | No | No | Unknown |

Backend remains authoritative. Android capability checks prevent misleading UI
and accidental calls but do not replace server authorization or validation.

## Scope

### In Scope

- Replace `AppointmentStatus` values and raw-string mapping.
- Add fail-closed status capabilities for active, terminal, cancellation,
  rescheduling, and feedback behavior.
- Decode list, detail, create, cancel, and reschedule responses using the new
  vocabulary.
- Split appointment-detail cancellation and rescheduling visibility instead of
  treating both actions as one shared state.
- Guard cancellation and rescheduling in the ViewModel as well as the UI.
- Treat `fulfilled`, `cancelled`, and `no_show` as appointment history.
- Select only `scheduled` or `checked_in` appointments for the Home card.
- Present consistent patient-facing status labels in appointment list/detail
  and the messaging attachment picker.
- Update booking, repository, ViewModel, formatting/filter, and Home tests.
- Update `CONTEXT.md` after implementation.

### Out of Scope

- Backend code or database changes.
- Encounter status support.
- Retrofit routes, request fields, response shapes, pagination, or base URL.
- Authentication, intake, reservation, records, rating, invoice, or
  conversation behavior unrelated to appointment labels.
- Room schema or stored-data migration; appointments are network-only.
- Navigation structure or visual redesign.
- New dependencies or legacy-status adapters.
- Repairing unrelated feedback-history or duplicate-feedback concerns.

## Current Android Gap Analysis

| Current Android behavior | Required behavior |
|---|---|
| Enum uses `PENDING`, `CONFIRMED`, `ARRIVED`, `COMPLETED`. | Use `SCHEDULED`, `CHECKED_IN`, `FULFILLED`. |
| Unknown raw status falls back to `PENDING`. | Unknown status maps to non-actionable `UNKNOWN`. |
| Cancel and reschedule share pending/confirmed visibility. | Cancel scheduled/checked-in; reschedule scheduled only. |
| ViewModel mutation methods do not guard current status. | Invalid local status prevents the request. |
| Feedback is exposed for `COMPLETED`. | Feedback is exposed for `FULFILLED`. |
| Home selects pending/confirmed appointments. | Home selects scheduled/checked-in appointments. |
| List history treats `COMPLETED` as terminal. | List history treats `FULFILLED` as terminal. |
| UI badges and guidance use old labels. | UI uses Scheduled, Checked in, Completed. |
| Attachment picker derives labels from enum names. | Attachment picker uses the shared patient label. |
| Tests and fixtures return old status strings. | Contract fixtures use only new strings. |

## Tech Stack

No stack or dependency change:

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean (`data` → `domain` → `presentation`) |
| Dependency injection | Hilt 2.59.2 |
| Network | Retrofit 2.11 + Kotlinx Serialization 1.8.1 |
| State | `StateFlow` with sealed UI-state interfaces |
| Tests | JUnit 5, MockK, Turbine, coroutines-test |

## Commands

```powershell
.\gradlew testDebugUnitTest --tests "*Appointment*"
.\gradlew testDebugUnitTest --tests "*HomeViewModelTest"
.\gradlew ktlintCheck
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
```

## Project Structure

Expected implementation surface:

```text
app/src/main/java/com/eyecare/app/
├── domain/model/AppointmentV1.kt
├── data/repository/AppointmentV1RepositoryImpl.kt
└── presentation/
    ├── appointments/
    │   ├── AppointmentDetailViewModel.kt
    │   ├── AppointmentDetailScreen.kt
    │   └── AppointmentListScreen.kt
    ├── home/HomeViewModel.kt
    └── messaging/components/AttachmentSheet.kt

app/src/test/java/com/eyecare/app/
├── data/remote/dto/AppointmentV1DtosTest.kt
├── data/repository/AppointmentV1RepositoryImplTest.kt
└── presentation/
    ├── appointments/
    ├── home/HomeViewModelTest.kt
    └── appointments/booking/BookAppointmentViewModelTest.kt
```

Phase 2 may split or reduce this surface after dependency analysis. Production
DTO shape changes are not expected because the status is already transported as
a string and mapped at the repository boundary.

## Code Style

Status-dependent business rules belong on the domain status rather than being
repeated as ad hoc sets across screens:

```kotlin
enum class AppointmentStatus {
    SCHEDULED,
    CHECKED_IN,
    FULFILLED,
    CANCELLED,
    NO_SHOW,
    UNKNOWN;

    val canCancel: Boolean
        get() = this == SCHEDULED || this == CHECKED_IN

    val canReschedule: Boolean
        get() = this == SCHEDULED

    val canLeaveFeedback: Boolean
        get() = this == FULFILLED
}
```

Raw DTO strings map to the domain enum only at the repository boundary.
Patient-facing labels remain presentation concerns and must not be serialized
back to the server.

## Testing Strategy

### Domain and mapping tests

- Decode `scheduled`, `checked_in`, `fulfilled`, `cancelled`, and `no_show`.
- Map an unexpected value to `UNKNOWN`.
- Prove previous strings are not accepted as aliases.
- Verify capability properties for every enum value.

### Repository contract tests

- List/detail fixtures use the new values.
- Create response maps to `SCHEDULED`.
- Cancel response maps to `CANCELLED`.
- Reschedule response maps to `SCHEDULED`.
- Existing request bodies and error parsing remain unchanged.

### ViewModel tests

- Scheduled appointment can open/submit rescheduling and cancellation.
- Checked-in appointment can cancel but cannot open or submit rescheduling.
- Fulfilled, terminal, and unknown appointments cannot trigger mutation calls.
- Successful server responses still replace local appointment state.

### Presentation and filtering tests

- List/detail labels match the capability matrix.
- Upcoming contains future scheduled/checked-in appointments only.
- History contains fulfilled/cancelled/no-show and fail-closed unknown records.
- Home chooses the soonest scheduled/checked-in appointment.
- Feedback action appears only for fulfilled appointments.
- Attachment picker uses readable labels and never exposes `checked_in`.

### Verification gates

- Focused appointment and Home tests pass.
- Full unit suite passes.
- Ktlint and Android lint pass.
- Debug assembly succeeds.
- Manual smoke test covers scheduled, checked-in, fulfilled, cancelled,
  no-show, and an injected unknown-status fixture.

## Boundaries

### Always

- Use backend commit `579b964` as the behavior source.
- Keep DTO-to-domain mapping at the repository boundary.
- Keep server-side validation authoritative.
- Fail closed for unknown status values.
- Write failing tests before changing each behavior.
- Run focused tests and `.\gradlew assembleDebug` after each implementation
  slice.

### Ask First

- Add a dependency.
- Change a Retrofit route or payload.
- Change navigation structure.
- Expand scope to encounter workflows or backend implementation.
- Use a patient label other than “Completed” for `fulfilled`.

### Never

- Retain aliases for the retired status strings.
- Map an unknown status to an actionable state.
- Enable rescheduling for `checked_in`.
- Store appointment or clinical data in Room.
- Log appointment response bodies or patient notes.
- Change the backend documents supplied by the user as part of Android
  implementation.

## Success Criteria

- [ ] Android contains no `PENDING`, `CONFIRMED`, `ARRIVED`, or `COMPLETED`
  appointment enum value.
- [ ] No test fixture sends the retired appointment status strings.
- [ ] All five documented backend statuses decode correctly.
- [ ] Unknown status is visible but non-actionable.
- [ ] Scheduled appointments can cancel and reschedule.
- [ ] Checked-in appointments can cancel but cannot reschedule.
- [ ] Fulfilled appointments expose feedback and no appointment mutation.
- [ ] Cancelled and no-show appointments expose no mutation.
- [ ] Home and Upcoming use scheduled/checked-in only.
- [ ] History uses fulfilled/cancelled/no-show and fail-closed unknown records.
- [ ] Appointment list, detail, and attachment picker use consistent readable
  labels.
- [ ] Existing request bodies, route allowlist, and pagination remain unchanged.
- [ ] `CONTEXT.md` describes the new lifecycle.
- [ ] Focused and full verification gates pass.

## Open Questions

None. The source documents agree on the mobile appointment lifecycle. Human
review is required to approve the assumptions and patient-facing labels above.

## Phase Gate

Phases 1–2 were approved on 2026-07-27. The Phase 3 task list may be produced
for human review. Do not modify Android production code until the Phase 3 task
list is approved.
