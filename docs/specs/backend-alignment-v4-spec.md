# Spec: Backend Alignment V4

## Objective
Align the Android app with the current backend contract in `docs/BACKEND_CONTEXT.md` after backend workflow changes. Success means the app no longer exposes impossible appointment states/actions, schedules only within backend-supported clinic hours, and parses the current billing detail shape.

## Tech Stack
Android Kotlin app using Jetpack Compose, Hilt, Retrofit, Kotlinx Serialization, Room for product cache, and JUnit 5 tests.

## Commands
Build: `./gradlew assembleDebug`
Test: `./gradlew testDebugUnitTest`
Lint: `./gradlew lintDebug`
Format: `./gradlew ktlintFormat`

## Project Structure
`app/src/main/java/com/eyecare/app/data/remote/dto/` holds API DTOs.
`app/src/main/java/com/eyecare/app/data/repository/` maps DTOs to domain models.
`app/src/main/java/com/eyecare/app/domain/model/` holds presentation-safe models.
`app/src/main/java/com/eyecare/app/presentation/appointments/` holds appointment UI and booking/reschedule controls.
`app/src/main/java/com/eyecare/app/presentation/billing/` holds billing detail UI.
`app/src/test/java/` holds contract and ViewModel tests.

## Code Style
DTO fields use `@SerialName` for snake_case API fields and map to camelCase domain fields at the repository boundary:

```kotlin
@SerialName("billing_number") val billingNumber: String
```

Domain models do not use serialization annotations.

## Testing Strategy
Update existing JUnit 5 tests that express the API contract. Prefer repository mapping tests for JSON shape changes and small ViewModel/unit tests for pure scheduling helpers.

## Implementation Tasks
- [x] Task 1: Align appointment lifecycle statuses and customer action eligibility.
  - Acceptance: Android maps all six backend statuses and only pending/confirmed appointments expose reschedule/cancel actions.
  - Verify: `./gradlew testDebugUnitTest --tests "*AppointmentRepositoryImplTest*"` and `./gradlew assembleDebug`.
- [x] Task 2: Align booking time selection with clinic hours and slot intervals.
  - Acceptance: Booking offers 09:00-17:00 in 15-minute increments.
  - Verify: `./gradlew testDebugUnitTest --tests "*BookAppointmentViewModelTest*"` and `./gradlew assembleDebug`.
- [ ] Task 3: Align reschedule time selection with clinic hours and slot intervals.
  - Acceptance: Rescheduling offers 09:00-17:00 in 15-minute increments.
  - Verify: source assertions for stale 5-minute/18:30 values and `./gradlew assembleDebug`.
- [ ] Task 4: Add optional billing notes to the DTO, domain model, repository mapping, and detail UI.
  - Acceptance: Billing responses with or without notes deserialize, map, and render correctly.
  - Verify: targeted billing tests and `./gradlew assembleDebug`.
- [ ] Task 5: Synchronize backend alignment documentation and run final verification.
  - Acceptance: This spec and `docs/BACKEND_CONTEXT.md` describe the implemented Android compatibility state.
  - Verify: final `./gradlew assembleDebug` and applicable targeted tests.

## Boundaries
- Always: Treat `docs/BACKEND_CONTEXT.md` as newer than completed historical specs.
- Always: Keep DTO-to-domain mapping at the repository boundary.
- Always: Run `./gradlew assembleDebug` after changes.
- Ask first: Navigation graph changes or new dependencies.
- Never: Reintroduce Gson, store health data in Room, or add the Kotlin Android plugin.

## Success Criteria
- Appointment statuses match backend lifecycle: `pending`, `confirmed`, `arrived`, `completed`, `no_show`, `cancelled`.
- `rescheduled` is not exposed as a domain lifecycle status or eligible action.
- Customer reschedule action is only offered for `pending` and `confirmed` appointments.
- Booking and reschedule time pickers use 09:00-17:00 clinic hours and 15-minute increments.
- Billing detail parses and displays optional `notes`.
- `./gradlew assembleDebug` succeeds.

## Open Questions
- `GET /appointments/availability` is listed with request parameters, but its response shape and reschedule exclusion contract are not documented. Android keeps client-side slot selection and relies on booking/reschedule 422 validation until that contract is documented; no guessed DTO is introduced.

## Verification Notes
- Task 1: `assembleDebug` passes. The targeted repository test cannot compile because the unrelated `ProductListViewModelTest` still passes `String` category names to an `Int?` API at lines 78, 97, and 99.
- Task 2: Source checks confirm no stale 18:30/5-minute booking values; `assembleDebug` passes. The targeted unit test remains blocked by the same unrelated catalog test compile errors.
