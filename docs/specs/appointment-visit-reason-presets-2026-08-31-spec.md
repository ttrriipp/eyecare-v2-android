# Spec: Appointment Visit-Reason Presets

**Date:** 2026-08-31
**Status:** Complete — 2026-08-31 — implementation and verification passed
**Authoritative inputs:** `CONTEXT.md`, `docs/BACKEND_CONTEXT.md`,
`docs/API_CONTRACT.md`, the implemented Android source tree, and the approved product direction in
this task

## Objective

Make the existing **Reason for visit** step faster to complete by showing backend-managed common
reasons for the selected appointment type. Patients can choose one preset and optionally add more
specific details, or choose **Other** and provide their own description. The Android app still sends
one free-text `reason_for_visit` value and never sends or stores a preset ID on the backend.

### User outcomes

1. After choosing an appointment type, the patient sees that type's active visit-reason presets on
   the existing reason step.
2. A patient can select one preset and continue immediately, or add optional details that clarify
   their concern.
3. A patient can choose **Other** and enter an exact custom reason.
4. An appointment type with no presets keeps the current required free-text experience.
5. The completed reason shown on Review and submitted to the backend is understandable without the
   preset catalog being available later.
6. Back navigation, configuration changes, and correctable validation failures preserve the
   patient's work.

## Assumptions and Scope Decisions

1. `docs/API_CONTRACT.md` and `docs/BACKEND_CONTEXT.md` are the wire-contract source of truth.
2. Preset selection is optional and single-select. Presets are suggestions, not diagnoses and not a
   restriction on what the patient may write.
3. Android adds the **Other** choice locally when the selected appointment type has one or more
   presets. When the server returns an empty preset list, the existing text field already serves the
   same purpose, so a redundant **Other** chip is not shown.
4. A selected preset is a complete valid reason by itself. The accompanying detail field is
   optional.
5. The final reason is composed as follows after trimming whitespace:
   - preset without details: `Preset label`
   - preset with details: `Preset label: patient details`
   - **Other**: the patient's exact custom description, without an `Other:` prefix
   - no presets: the patient's exact custom description
6. The complete composed value, including the preset label and separator, must not exceed the
   backend's 1000-character limit.
7. The preset ID and choice are Android workflow state only. `POST /appointment-requests` remains
   unchanged and receives only the composed `reason_for_visit` string.
8. The current Type → Schedule → Reason → optional Identity → Review order remains unchanged. No
   new wizard step is introduced.
9. If a selected preset is unavailable after the appointment-type catalog is refreshed or restored,
   Android preserves the already composed reason as a custom **Other** value instead of silently
   deleting patient input.
10. Changing the appointment type clears the old preset association. Any existing reason text is
    preserved as custom input because it may still describe the patient's concern.
11. The server controls preset wording and ordering. Android does not hardcode clinic-specific
    reasons, sort them again, or infer medical meaning from them.
12. No new dependency, backend endpoint, analytics event, or persistent database storage is needed.
13. Existing unrelated working-tree changes, including the updated backend documents, belong to the
    user and must remain intact.

## Authoritative Contract Delta

### `GET /api/v1/appointment-types`

Every appointment type now includes `visit_reason_presets`, which may be empty:

```json
{
  "id": 1,
  "name": "First eye examination",
  "description": "For your first examination at the clinic.",
  "duration_minutes": 45,
  "requires_referral": false,
  "visit_reason_presets": [
    {
      "id": 21,
      "label": "Blurred or reduced vision"
    },
    {
      "id": 22,
      "label": "Eye pain or discomfort"
    }
  ]
}
```

- The array contains only active presets for that appointment type.
- Items are already ordered by backend `sort_order`; Android preserves that order.
- Each item exposes only an integer `id` and a trimmed, nonblank `label` of at most 255 characters.
- For rollout tolerance, a missing `visit_reason_presets` key decodes as an empty list and uses the
  text-only fallback.

### `POST /api/v1/appointment-requests`

The request contract does not change:

```json
{
  "appointment_type_id": 1,
  "scheduled_at": "2026-09-02T10:00:00+08:00",
  "alternative_scheduled_times": [],
  "reason_for_visit": "Blurred or reduced vision: mostly in my left eye for two weeks",
  "referring_source": null,
  "identity": null
}
```

- `reason_for_visit` remains required free text with a maximum length of 1000 characters.
- No `visit_reason_preset_id`, preset label field, or **Other** marker is submitted.
- Existing referral and unlinked-identity rules remain unchanged.

## UX and State Behavior

### Reason step with presets

- Keep the existing screen title, step indicator, explanation, referral section, Back action, and
  Continue action.
- Add a **Common reasons** group before the text field.
- Render server presets in their received order as wrapping, single-select Material 3 filter chips,
  followed by a locally supplied **Other** chip.
- Long labels wrap without horizontal scrolling or truncating essential text. Chips expose selected
  semantics and remain usable with large font sizes and screen readers.
- Before a choice is made, supporting copy asks the patient to select a common reason or **Other**.
- Selecting a preset shows an **Add details (optional)** multiline field. The field starts empty and
  does not duplicate the preset label.
- Selecting **Other** shows a required **Describe your reason for visit** multiline field.
- Changing between choices preserves typed details during the current reason-step session. This
  avoids accidental data loss; only the currently selected choice contributes to the final value.
- Tapping Continue with no choice, or with **Other** and a blank description, shows a patient-safe
  inline error beside the choice/input rather than advancing.
- A preset with no details is valid. A preset or **Other** choice never bypasses the existing referral
  validation when the appointment type requires a referral.

### Reason step without presets

- Do not render the **Common reasons** group or **Other** chip.
- Keep the current required **Reason for visit** multiline text field and validation behavior.
- This is also the safe fallback for a staged deployment where the new response key is absent.

### Composition and length handling

- Build the final reason through one pure function used by validation, Review, draft restoration,
  and submission.
- Normalize leading/trailing whitespace in the preset detail or custom description. Preserve normal
  internal punctuation and line content; do not rewrite the patient's words.
- For a preset plus details, add exactly `: ` between the backend label and the patient text.
- Character feedback reflects the length of the final composed value, not merely the detail field.
- Reject locally when the composed value exceeds 1000 characters, while preserving the complete
  draft for correction.
- Review displays exactly the composed value that will be submitted.

### Draft and catalog reconciliation

- Extend the existing `SavedStateHandle` request draft with primitive preset-choice state and detail
  text; do not place DTO or domain objects in saved state.
- Re-fetch appointment types on restoration as the app already does, then resolve a saved preset ID
  only inside the currently selected appointment type.
- If that ID no longer exists, convert the saved composed reason to custom **Other** input and keep it
  visible.
- Moving backward and forward in the wizard restores the active choice, the shared detail/custom
  text draft, and referral input.
- Server validation or network failures at submission leave the reason draft intact.

## Tech Stack

- Kotlin with AGP 9 built-in Kotlin support
- Jetpack Compose and Material 3
- MVVM + Clean Architecture (`data -> domain -> presentation`)
- Hilt dependency injection
- Retrofit + Kotlinx Serialization
- Coroutines, `StateFlow`, and `SavedStateHandle`; no LiveData
- JUnit 5, MockK, MockWebServer, and Compose UI tests

No new library is required. Compose Material 3 already provides the chip and text-field primitives.

## Commands

Run from the repository root with Android Studio's bundled JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew ktlintFormat
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
.\gradlew assembleDebugAndroidTest
```

Run `connectedDebugAndroidTest` only when an emulator/device and `adb` are available; otherwise report
that instrumented execution was not run.

## Project Structure

```text
app/src/main/java/com/eyecare/app/
  data/remote/dto/AppointmentRequestDtos.kt
      visit-reason preset response DTO and tolerant empty-list decoding
  data/repository/AppointmentRequestRepositoryImpl.kt
      appointment-type DTO-to-domain mapping
  domain/model/AppointmentType.kt
      visit-reason preset domain model and appointment-type collection
  presentation/appointments/requests/
    RequestDraft.kt
      primitive saved preset choice and patient-entered detail
    RequestAppointmentViewModel.kt
      selection, reconciliation, validation, and final-reason composition
    RequestAppointmentScreen.kt
      reason-step callbacks
    RequestReasonContent.kt
      preset chips, Other choice, conditional text field, and inline errors
    RequestReviewContent.kt
      unchanged final free-text presentation, covered by regression tests

app/src/test/java/com/eyecare/app/
  data/remote/dto/AppointmentRequestDtosTest.kt
  data/repository/AppointmentRequestRepositoryImplTest.kt
  domain/model/AppointmentTypeCatalogTest.kt
  presentation/appointments/requests/RequestAppointmentViewModelTest.kt

app/src/androidTest/java/com/eyecare/app/presentation/appointments/requests/
  RequestReasonContentTest.kt
  RequestReviewContentTest.kt

docs/specs/
  appointment-visit-reason-presets-2026-08-31-spec.md
```

## Code Style

Keep the API model explicit and keep workflow-only choice state out of the wire layer:

```kotlin
data class VisitReasonPreset(
    val id: Int,
    val label: String,
)

sealed interface VisitReasonChoice {
    data object None : VisitReasonChoice
    data class Preset(val presetId: Int) : VisitReasonChoice
    data object Other : VisitReasonChoice
}

fun composeReasonForVisit(
    choice: VisitReasonChoice,
    presets: List<VisitReasonPreset>,
    details: String,
): String = when (choice) {
    VisitReasonChoice.None -> details.trim()
    VisitReasonChoice.Other -> details.trim()
    is VisitReasonChoice.Preset -> {
        val label = presets.firstOrNull { it.id == choice.presetId }?.label.orEmpty()
        details.trim().takeIf(String::isNotEmpty)?.let { "$label: $it" } ?: label
    }
}
```

The production function must handle an unresolved preset through the reconciliation path rather than
submitting an empty label. DTO names use Kotlin camelCase and retain backend snake_case through
`@SerialName`. DTOs map to domain models at the repository boundary.

## Testing Strategy

Follow RED -> GREEN -> REFACTOR for each behavior slice.

### Unit tests

- DTO decoding covers populated, empty, and temporarily missing `visit_reason_presets` arrays.
- Repository mapping preserves preset IDs, labels, appointment ownership, and server ordering.
- Final-reason composition covers preset-only, preset plus details, **Other**, whitespace
  normalization, missing preset reconciliation, and the 1000-character boundary.
- ViewModel tests cover single selection, switching choices without losing typed input, inline
  validation, referral coexistence, Back/forward restoration, process-draft restoration, appointment
  type changes, catalog refresh with a deactivated preset, and the empty-list fallback.
- Submission tests prove that the request contains only the final `reason_for_visit` string and never
  a preset ID.
- Existing request submission and server-error mapping tests remain green.

### Compose UI tests

- Presets appear in backend order with a final **Other** choice.
- Only one choice exposes selected semantics at a time.
- Preset selection shows optional-details copy; **Other** shows required custom-description copy.
- Empty preset lists render the existing text-only field without a redundant **Other** chip.
- Inline errors attach to the choice or text field that needs correction.
- Long labels, large fonts, scrolling, and the referral field remain usable.
- Review renders the exact composed reason.

### Regression gates

- Appointment type, schedule, optional identity, review, and submission step order is unchanged.
- Appointment availability and request creation still use the selected appointment type ID.
- Referral requirements and unlinked identity requirements retain their current behavior.
- Full unit suite, lint, debug build, and Android-test compilation pass.

## Boundaries

### Always

- Treat the backend documents as the wire-contract source of truth.
- Use only presets belonging to the currently selected appointment type.
- Preserve server ordering and supply **Other** in Android.
- Keep custom free text available and preserve patient input across correctable transitions.
- Enforce the 1000-character limit on the exact final value sent to the backend.
- Map DTOs to domain models at the repository boundary.
- Keep reason text out of logs, analytics, Room, and crash metadata.
- Preserve unrelated working-tree and backend-document changes.
- Run focused tests while implementing and all required project gates before completion.

### Ask first

- Add or change a backend endpoint, request field, preset lifecycle, or admin behavior.
- Submit or persist a preset ID on the backend.
- Support multiple selected presets or change the final-reason composition rule.
- Change the appointment-request wizard order.
- Add a dependency or change build/CI configuration.
- Persist visit reasons anywhere beyond the request flow's existing `SavedStateHandle` behavior.

### Never

- Hardcode clinic-managed preset labels in the Android app.
- Treat a preset as a diagnosis or prevent the patient from using a custom reason.
- Send **Other**, a preset ID, or a separate detail field to `POST /appointment-requests`.
- Silently erase patient-entered details when a choice, type, or refreshed catalog changes.
- Reorder backend presets or combine presets from different appointment types.
- Log or analyze visit-reason text.
- Use Gson, LiveData, or the `org.jetbrains.kotlin.android` plugin.

## Success Criteria

1. Each appointment type domain model exposes its backend-provided visit-reason presets.
2. The Reason step shows presets only for the currently selected appointment type and preserves their
   server order.
3. Android adds one **Other** choice when presets exist.
4. Preset selection is single-select and a preset alone is a valid reason.
5. A selected preset accepts optional patient details; **Other** requires a custom description.
6. Empty or missing preset arrays retain the current required text-only experience.
7. The final reason follows the approved composition rules and never exceeds 1000 characters.
8. Review displays exactly the string later submitted as `reason_for_visit`.
9. `POST /appointment-requests` remains unchanged and contains no preset identifier.
10. Back/forward navigation and restoration preserve the active choice and patient-entered text.
11. A missing or deactivated saved preset degrades to custom input without losing the composed reason.
12. Changing appointment types cannot carry a stale preset association into the new type.
13. Referral and unlinked-identity behavior remains unchanged.
14. Preset controls are accessible, wrap at narrow widths, and remain usable at large font sizes.
15. Focused DTO, repository, ViewModel, submission, and Compose tests pass.
16. `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleDebugAndroidTest` pass after
    formatting.
17. No dependency, backend change, sensitive logging, Room storage, or unrelated app work is
    introduced.

## Open Questions

None. Human approval on 2026-08-31 confirmed both the final-text composition rule and the text-only
fallback when an appointment type has no presets.
