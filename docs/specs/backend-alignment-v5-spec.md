# Spec: Backend Alignment V5 - Appointment Availability

## Objective
Replace Android's locally generated appointment times with the backend availability grid so customers choose a slot that is currently valid for the selected visit reason and date. The backend remains authoritative at mutation time, and stale selections recover by refreshing availability instead of leaving the user at an unexplained validation error.

## Tech Stack
Android Kotlin app using Jetpack Compose, Hilt, Retrofit, Kotlinx Serialization, StateFlow, and JUnit 5.

## Commands
Build: `.\gradlew assembleDebug`
Test: `.\gradlew testDebugUnitTest`
Lint: `.\gradlew lintDebug`
Format: `.\gradlew ktlintFormat`

## Project Structure
`app/src/main/java/com/eyecare/app/data/remote/` defines the Retrofit and serialized availability contract.
`app/src/main/java/com/eyecare/app/data/repository/` maps availability DTOs into domain models.
`app/src/main/java/com/eyecare/app/domain/` exposes availability without transport concerns.
`app/src/main/java/com/eyecare/app/presentation/appointments/` owns booking and reschedule availability state and UI.
`app/src/test/java/` contains repository contract and ViewModel behavior tests.

## Code Style
API snake_case remains isolated to DTOs and domain models use camelCase:

```kotlin
@Serializable
data class AvailabilitySlotDto(
    @SerialName("starts_at") val startsAt: String,
    val available: Boolean,
)

data class AppointmentSlot(
    val startsAt: String,
    val available: Boolean,
)
```

## API Contract
Android calls `GET /appointments/availability` with `date` and `visit_reason_id`. Rescheduling additionally sends the owned `appointment_id` so the current appointment does not conflict with itself.

The response includes the clinic date and timezone, interval and visit duration, `day_status`, generation timestamp, and every generated slot that fits before closing. Each slot has offset-bearing `starts_at` and `ends_at`, an availability flag, and an optional reason.

Booking and reschedule mutations remain authoritative. A 422 response with `code = SLOT_UNAVAILABLE` means the snapshot became stale; Android refreshes through the availability GET endpoint and asks the customer to choose again. Android does not depend on the optional availability context embedded in the error until its exact nested JSON shape is documented.

## UX Behavior
- Fetch slots when the customer selects a date and reason.
- Show a loading state in the time step without advancing automatically.
- Present slots in a stable grid using clinic-local formatted times.
- Show capacity-blocked slots disabled so customers can understand the day's schedule.
- Hide elapsed same-day slots because they are no longer actionable.
- Show a concise empty state for closed days or days with no available times, with a control to choose another date.
- Show an inline retry state when availability cannot be loaded.
- Only an available slot can advance to Review & Confirm.
- On `SLOT_UNAVAILABLE`, return to time selection, refresh the grid, and explain that the selected time was just taken.

## Testing Strategy
- Repository tests verify query parameters, Kotlinx deserialization, and DTO-to-domain mapping for open and closed days.
- ViewModel tests verify loading, success, retry, selection, empty availability, and stale-slot refresh behavior.
- Pure formatting is tested with explicit `+08:00` timestamps.
- Compose behavior is kept state-driven; build verification catches API and layout integration issues.

## Implementation Tasks
- [x] Task 1: Add the availability API and domain contract.
  - Acceptance: Repository callers can fetch a mapped availability grid for booking or rescheduling, including unavailable reasons and offset-bearing timestamps.
  - Verify: `.\gradlew testDebugUnitTest --tests "*AppointmentRepositoryImplTest*"`.
- [x] Task 2: Replace booking's manual time picker with backend slots.
  - Acceptance: Booking loads availability for the selected reason/date, only permits available selections, and represents loading, retry, and empty states.
  - Verify: `.\gradlew testDebugUnitTest --tests "*BookAppointmentViewModelTest*"` and `.\gradlew assembleDebug`.
- [ ] Task 3: Use backend slots for rescheduling.
  - Acceptance: Reschedule requests include `appointment_id`, current-appointment self-exclusion works, and stale selection errors refresh the grid.
  - Verify: targeted appointment detail ViewModel tests and `.\gradlew assembleDebug`.
- [ ] Task 4: Synchronize system context and perform final review.
  - Acceptance: `CONTEXT.md`, this spec, and backend context accurately describe shipped Android behavior.
  - Verify: `.\gradlew testDebugUnitTest`, `.\gradlew lintDebug`, and `.\gradlew assembleDebug`.

## Boundaries
- Always: Treat backend availability as a snapshot and mutations as authoritative.
- Always: Preserve explicit clinic offsets and map DTOs at the repository boundary.
- Always: Keep the existing booking wizard and appointment detail navigation structure.
- Ask first: Add dependencies, change backend contracts, or change navigation.
- Never: Infer availability from the appointment list or expose backend capacity details beyond documented slot states.
- Never: Deserialize undocumented stale-error context as a required contract.

## Success Criteria
- A customer cannot review or submit a locally generated time that the availability API marks unavailable.
- A 20-minute appointment correctly blocks every overlapping 15-minute candidate returned by the backend.
- Closed and fully booked dates have purposeful UI states.
- Rescheduling excludes the current appointment through `appointment_id`.
- A slot lost to a concurrent booking produces a refreshed choice flow rather than a dead-end error.
- Existing appointment booking, detail, cancel, and history behavior continues to build and pass tests.

## Open Questions
- The backend documents a safe `availability` context in stale 422 responses but not its exact nested schema. Android will perform a fresh GET after `SLOT_UNAVAILABLE`; the embedded context can be adopted later as an optimization.
