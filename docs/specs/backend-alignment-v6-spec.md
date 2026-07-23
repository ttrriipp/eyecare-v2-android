# Spec: Backend Alignment V6 — Product Taxonomy and Reschedule Reasons

Status: Complete — 2026-07-23

## Objective

Align Android with the latest additive backend contract changes recorded in
`docs/BACKEND_CONTEXT.md`:

1. Preserve and display the customer-readable `last_reschedule_reason` returned
   with appointments after a staff reschedules a visit.
2. Treat `contact_lens` and `accessory` as distinct, directly orderable product
   types that never require optical-lens cutting or a lens-category selection.
3. Retain the existing frame order flow and compatibility with the backend's
   accepted `lens_type_id` request alias.

The customer should see why a clinic-initiated schedule change occurred, and
should be able to order non-frame retail products without being asked
frame-specific prescription questions.

## Assumptions

1. `docs/BACKEND_CONTEXT.md` is the API source of truth; its current uncommitted
   diff is the newest backend change set.
2. `last_reschedule_reason` is additive and nullable on every appointment
   resource response, including list, detail, contact-note, cancel, and
   reschedule mutation responses.
3. `contact_lens` and `accessory` products are inherently
   `is_non_prescription = true` in the Android order request because neither
   requires optical-lens cutting.
4. Only `frame` products may expose Android's existing optical lens-category
   choice.
5. The backend continues to accept `items[].lens_type_id` as an outbound alias;
   changing that request key is not required in this alignment.

## Tech Stack

Kotlin 2.3.0, Jetpack Compose Material 3, Hilt, Retrofit, Kotlinx
Serialization, StateFlow, JUnit 5, MockK, Turbine, and coroutines-test.

## Commands

```text
Build:  .\gradlew assembleDebug
Test:   .\gradlew testDebugUnitTest
Lint:   .\gradlew lintDebug
Format: .\gradlew ktlintFormat
Check:  .\gradlew ktlintCheck
```

## Project Structure

```text
app/src/main/java/com/eyecare/app/data/remote/dto/
  AppointmentDtos.kt                 API response field definitions
app/src/main/java/com/eyecare/app/data/repository/
  AppointmentRepositoryImpl.kt       DTO-to-domain mapping boundary
app/src/main/java/com/eyecare/app/domain/model/
  Appointment.kt                     Transport-independent appointment model
app/src/main/java/com/eyecare/app/presentation/appointments/
  AppointmentDetailScreen.kt         Customer-visible reschedule reason
app/src/main/java/com/eyecare/app/presentation/orders/
  OrderRequestViewModel.kt            Product-type order invariants
  OrderRequestScreen.kt               Type-appropriate order controls/copy
app/src/test/java/com/eyecare/app/
  data/repository/                    JSON contract and mapping tests
  presentation/orders/               Order behavior tests
docs/specs/                           Alignment specs
```

## API Contract and Android Mapping

| Backend contract | Android contract | Compatibility behavior |
|---|---|---|
| `last_reschedule_reason: string|null` | `AppointmentDto.lastRescheduleReason` → `Appointment.lastRescheduleReason` | Optional with a `null` default so older/partial responses remain readable |
| `product_type = frame` | Existing frame order flow | Customer may choose whether lens cutting is required; current lens-category request alias remains accepted |
| `product_type = contact_lens` | Direct non-frame order | Force `is_non_prescription = true`; send no lens-category ID |
| `product_type = accessory` | Direct non-frame order | Force `is_non_prescription = true`; send no lens-category ID |
| `product_type = lens` | Not mobile-orderable | Backend excludes/rejects it; Android must not add a direct order path |

API snake_case remains isolated to DTOs:

```kotlin
@Serializable
data class AppointmentDto(
    val id: Int,
    @SerialName("last_reschedule_reason")
    val lastRescheduleReason: String? = null,
)

data class Appointment(
    val id: Int,
    val lastRescheduleReason: String? = null,
)
```

Product-type checks are case-insensitive at the presentation boundary because
the existing catalog already tolerates case variants in cached/test data.
Unknown non-frame types are not silently granted frame-only lens controls.

## UX Behavior

### Staff reschedule reason

- When `lastRescheduleReason` is non-blank, appointment detail shows a distinct
  "Schedule changed by clinic" notice containing the backend text.
- The reason is not merged with `staff_notes`: the former explains the latest
  staff schedule change, while the latter remains the general read-only clinic
  note.
- Null or whitespace-only reasons render no empty container.
- A successful customer reschedule uses the returned appointment directly; if
  the backend clears the reason to `null`, the notice disappears immediately.

### Product ordering

- Frame orders retain the "No lens cutting required" choice and lens-category
  selector.
- Contact-lens and accessory orders do not show the frame-only toggle or
  lens-category selector.
- Contact-lens and accessory requests always send
  `is_non_prescription = true` and a null lens-category alias.
- Copy uses "Product" rather than "Frame" for shared product information.
- The lens selector is labelled "Lens Category", matching current backend
  terminology.

## Testing Strategy

- Repository contract tests deserialize `last_reschedule_reason` from both list
  and detail/mutation-compatible appointment shapes and verify domain mapping.
- Repository tests also verify the field remains optional for compatibility.
- ViewModel tests verify:
  - frames preserve the existing lens-cutting flow;
  - contact lenses initialize as non-prescription;
  - accessories initialize as non-prescription;
  - non-frame submissions cannot send a stale lens selection;
  - frame submissions preserve the accepted outbound lens alias.
- UI behavior remains state-driven; compilation and lint verify Compose
  integration. No new test dependency is introduced.

## Boundaries

### Always

- Map DTOs to domain models at the repository boundary.
- Use Kotlinx Serialization only.
- Treat the backend product type as the authority for order invariants.
- Preserve unknown response fields through `ignoreUnknownKeys`.
- Run `.\gradlew assembleDebug` after production changes.

### Ask First

- Add a mobile lens-categories endpoint or any dependency.
- Change navigation graph structure.
- Change the backend response/request contract.
- Infer a visit-reason ID from its display label.

### Never

- Allow `lens` products to be ordered directly.
- Show lens-cutting controls for `contact_lens` or `accessory`.
- Merge `last_reschedule_reason` into editable customer notes.
- Store appointment or prescription health data in Room.
- Apply `org.jetbrains.kotlin.android` or
  `android.disallowKotlinSourceSets=false`.

## Implementation Tasks

- [x] Task 1: Preserve the latest staff reschedule reason.
  - Acceptance: Every appointment mapping path retains nullable
    `last_reschedule_reason`, and older responses without the field still parse.
  - Verify:
    `.\gradlew testDebugUnitTest --tests "*AppointmentRepositoryImplTest*"`.
  - Files: `AppointmentDtos.kt`, `Appointment.kt`,
    `AppointmentRepositoryImpl.kt`, `AppointmentRepositoryImplTest.kt`.

- [x] Task 2: Display the reschedule reason on appointment detail.
  - Acceptance: A non-blank reason is visually distinct from clinic/customer
    notes; null/blank values render nothing; returned customer-reschedule data
    clears the notice without a refetch.
  - Verify: targeted appointment ViewModel tests and
    `.\gradlew assembleDebug`.
  - Files: `AppointmentDetailScreen.kt`,
    `AppointmentDetailViewModelTest.kt`.

- [x] Task 3: Enforce product-type order invariants in the ViewModel.
  - Acceptance: Contact-lens/accessory orders always submit as
    non-prescription with no lens category; frame behavior remains unchanged.
  - Verify:
    `.\gradlew testDebugUnitTest --tests "*OrderRequestViewModelTest*"`.
  - Files: `OrderRequestViewModel.kt`, `OrderRequestViewModelTest.kt`.

- [x] Task 4: Make order controls and copy product-type aware.
  - Acceptance: Frame-only controls are absent for contact lenses/accessories,
    shared copy says Product, and canonical customer-facing terminology says
    Lens Category.
  - Verify: `.\gradlew assembleDebug`.
  - Files: `OrderRequestScreen.kt`.

- [x] Task 5: Synchronize context and complete verification.
  - Acceptance: `CONTEXT.md` records the aligned behavior and this spec records
    actual verification outcomes.
  - Verify: `.\gradlew ktlintFormat`, `.\gradlew testDebugUnitTest`,
    `.\gradlew lintDebug`, and `.\gradlew assembleDebug`.
  - Files: `CONTEXT.md`, this spec.

## Success Criteria

- Appointment JSON containing `last_reschedule_reason` is preserved through the
  DTO-to-domain boundary.
- Customers can see a non-blank latest staff reschedule reason on appointment
  detail without confusing it with notes.
- Customer rescheduling clears the displayed staff reason when the returned
  response contains `null`.
- Contact-lens and accessory orders never require or transmit optical lens
  assignment.
- Frame order behavior remains compatible with existing Android behavior and
  the backend's accepted request alias.
- Catalog and Home grouping continue to recognize the four-value taxonomy.
- Targeted tests, the unit suite, lint, and the required debug build are run;
  any pre-existing unrelated failure is reported rather than hidden.

## Deferred / Blocked Contract Work

- Backend-powered reschedule availability remains blocked because appointment
  list/detail responses document `visit_reason` but not the stable
  `visit_reason_id` required by `GET /appointments/availability`. Android will
  not infer IDs from display text. This remains Task 3 in
  `backend-alignment-v5-spec.md`.
- The Android frame order flow uses a fixed local set of lens category IDs
  because no customer-facing lens-categories endpoint is documented. Replacing
  that set requires an explicit backend contract and is outside this additive
  update.

## Baseline Verification

Before implementation, the current Android state was verified on 2026-07-23:

- `.\gradlew testDebugUnitTest --tests "*AppointmentRepositoryImplTest*" --tests
  "*OrderRequestViewModelTest*"` — passed.
- `.\gradlew assembleDebug` — passed.
- The shell did not initially expose Java. Both successful commands used
  Android Studio's bundled JDK at
  `C:\Program Files\Android\Android Studio\jbr` through process-local
  `JAVA_HOME` and `Path` values; no machine or project configuration changed.

## Final Verification

Completed on 2026-07-23:

- `.\gradlew ktlintFormat` — passed.
- `.\gradlew ktlintCheck` — passed.
- `.\gradlew testDebugUnitTest` — passed, 157 tests.
- `.\gradlew assembleDebug` — passed.
- `.\gradlew lintDebug` — analysis completed with no V6-related error, but
  the task remains red on two pre-existing unrelated errors:
  - `MainActivity.kt`: ignored Material 3 Scaffold content padding.
  - `AndroidManifest.xml`: camera permission without an optional camera
    hardware feature declaration.
- The complete unit run initially exposed three stale test-infrastructure
  assumptions which were corrected without changing production behavior:
  `GET /orders` now has pagination metadata in its fixture,
  `OrderListViewModelTest` stubs the existing pagination query, and
  `ChatViewModelTest` stubs read receipts while terminating the intentional
  polling scope deterministically.

## Open Questions

- The Mobile REST API summary in `BACKEND_CONTEXT.md` still says
  "FRAME and GENERAL products" in one line even though the detailed taxonomy,
  response example, validation rules, and current diff all specify
  `frame`, `contact_lens`, and `accessory`. This spec follows the repeated,
  newer four-value contract.
- `BACKEND_CONTEXT.md` says to use `GET /visit-reasons` for brand/category IDs
  in one paragraph, but the documented and implemented Android endpoints are
  `GET /brands` and `GET /categories`. No Android change is required.
