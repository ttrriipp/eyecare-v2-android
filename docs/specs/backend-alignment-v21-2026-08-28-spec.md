# Spec: Backend Alignment v21 — Self-Service Account Profile

**Date:** 2026-08-28  
**Status:** Approved 2026-08-28 — implementation complete, verification passed  
**Authoritative inputs:** `CONTEXT.md`, `docs/BACKEND_CONTEXT.md` (2026-08-28),
`docs/API_CONTRACT.md` (2026-08-28), and the implemented Android source tree

## Objective

Align the Android account editor with the backend's shipped self-service profile boundary. An
authenticated patient account must be able to update its account first, middle, and last names and
its account date of birth. A DOB-bearing update must reuse the existing same-account step-up OTP
flow. Account edits must never imply or perform a change to the linked clinic Patient record.

### User outcomes

1. A linked, pending-review, or unlinked authenticated account can edit its account first name,
   nullable middle name, last name, and date of birth from **Account & Security**.
2. A name-only change saves directly. A request containing a changed DOB asks the patient to verify
   the existing step-up OTP before the update is sent.
3. Blank middle-name input clears the account middle name; first and last names remain required.
4. DOB uses a Material date picker, serializes as exact `Y-m-d`, and cannot be today or in the future
   in the clinic's `Asia/Manila` date boundary.
5. Field validation appears beside the matching control, while step-up and non-validation failures
   preserve the complete draft for correction or retry.
6. Email and phone continue through verified Contact Management, password continues through its
   protected flow, and linked Patient demographics remain visibly read-only.
7. A successful update renders the canonical account returned by `PATCH /me`, including any changed
   `link_status`; the client never mutates a linked Patient locally.

## Assumptions and Scope Decisions

1. `docs/API_CONTRACT.md` and `docs/BACKEND_CONTEXT.md` are authoritative over the v17 profile text
   in `CONTEXT.md`, which still describes the previous name-only Android contract.
2. `date_of_birth` always means `PatientAccount.dateOfBirth` from the account portion of `/me`, never
   `linked_patient.date_of_birth`.
3. **Account & Security** is the canonical account editor. The current Profile hub already navigates
   there, while the typed `EditProfile` destination has no live source navigation. The unreachable
   legacy destination, screen, and duplicate editing state will be removed after navigation tests
   prove that no supported flow depends on them.
4. The Android app reuses `POST /auth/step-up/otp` and `POST /auth/step-up/verify`; no new
   authentication flow, credential type, dependency, or backend endpoint is introduced.
5. DOB cannot be cleared because the PATCH contract accepts an exact date string, not `null`.
6. Android applies only the backend's `before today` DOB rule. It does not invent a minimum age or a
   maximum age.
7. A changed DOB and changed names are submitted atomically in one PATCH after step-up verification.
8. Step-up proofs are single-use and memory-only. A failed protected PATCH preserves the draft but a
   later retry requests a fresh proof.
9. The working-tree changes already present in `docs/API_CONTRACT.md` and
   `docs/BACKEND_CONTEXT.md` belong to the user and must remain intact.

## Authoritative Contract Delta

### `PATCH /api/v1/me`

Allowed partial fields:

```json
{
  "first_name": "Ana",
  "middle_name": null,
  "last_name": "Santos",
  "date_of_birth": "1990-05-15"
}
```

- At least one allowed field is required.
- Only changed fields are sent.
- Omitted fields remain unchanged.
- Explicit `middle_name: null` clears the middle name; omission leaves it unchanged.
- Any payload containing `date_of_birth` includes `X-Step-Up-Token`.
- The response uses the complete `GET /me` `PatientAccountResource` shape.
- Unsupported, mixed, or invalid payloads fail atomically with `422`.
- A successful update never writes the `patients` row.

### Error envelopes

Field validation uses Laravel's validation envelope:

```json
{
  "message": "The given data was invalid.",
  "errors": {
    "date_of_birth": ["The date of birth field must be a date before today."]
  }
}
```

The editor maps `first_name`, `middle_name`, `last_name`, `date_of_birth`, and `profile` into field or
form errors. Unknown keys fall back to the form-level patient-safe message.

Step-up middleware uses the machine-readable envelope with `STEP_UP_REQUIRED` or
`INVALID_STEP_UP_TOKEN`. The client must support both envelope families through the existing
`ApiErrorDecoder`/`ApiDomainError` boundary.

## UX and State Behavior

### Account details overview

- Continue to show account names, verified contact summaries, account DOB, role, and link status.
- The linked clinic Patient profile remains a separate read-only destination.
- Edit mode clearly states that names and account DOB are editable, while contact and clinical
  details use separate workflows.

### Editing

- Controls: first name, middle name, last name, and a non-freeform DOB date-picker field.
- First and last names are trimmed and required; maximum length is 255.
- Middle name is trimmed, has maximum length 255, and blank input normalizes to an explicit clear.
- The date picker disallows today and future dates using `Asia/Manila`.
- Save is disabled while saving, during OTP verification, and when the normalized draft is unchanged.
- Cancel returns to the account snapshot without mutation. Leaving a dirty draft uses the existing
  confirmation-dialog style before discarding it.

### Step-up flow

1. The ViewModel computes a normalized PATCH from the current account and draft.
2. If DOB is omitted, it sends the PATCH directly.
3. If DOB is present, it requests a step-up OTP and carries the in-memory draft/patch into the
   existing OTP state.
4. Successful OTP verification immediately executes that pending PATCH with the proof token.
5. OTP cancellation or failure returns to/preserves the editor draft rather than discarding it.
6. A protected PATCH failure clears the single-use proof, preserves the draft, and displays mapped
   field/form feedback. Retrying a still-dirty DOB starts a new step-up challenge.

### Success and link-state reconciliation

- The returned `PatientAccount` replaces the displayed account snapshot.
- Edit mode closes and transient draft, OTP, token, and error state are cleared.
- The returned `link_status` is trusted. If a pending request was expired and the response becomes
  `unlinked`, subsequent account/session refresh uses that server state and may require a new link
  request.
- No local field is copied into `linkedPatient`.

## Tech Stack

- Kotlin on Android with AGP 9 built-in Kotlin support
- Jetpack Compose and Material 3
- MVVM + Clean Architecture (`data -> domain -> presentation`)
- Hilt dependency injection
- Retrofit + Kotlinx Serialization
- Coroutines and `StateFlow`; no LiveData
- JUnit 5, MockK, MockWebServer, and Compose UI tests

No new library is required. The date picker and step-up primitives already exist in the project.

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
  data/remote/api/AuthApiService.kt           PATCH /me request/header boundary
  data/remote/dto/AuthDtos.kt                 account request/response wire models
  data/remote/ApiErrorDecoder.kt              both documented error envelopes
  data/repository/AuthRepositoryImpl.kt       DTO-to-domain and error boundary
  domain/model/                               explicit partial-profile change model if needed
  domain/repository/AuthRepository.kt         account profile update interface
  presentation/account/
    AccountSecurityViewModel.kt               draft, validation, step-up, and save state
    AccountSecurityScreen.kt                  canonical editor and DOB picker
  presentation/profile/                       Profile hub; legacy editor removal
  presentation/navigation/                    typed legacy destination removal

app/src/test/java/com/eyecare/app/
  data/                                       serialization/repository/error tests
  presentation/account/                       ViewModel state and workflow tests

app/src/androidTest/java/com/eyecare/app/
  presentation/account/                       deterministic editor UI tests

docs/specs/                                   v21 spec, plan, and task record
```

## Code Style

Use explicit sealed state and `StateFlow`, map DTOs at the repository boundary, and represent PATCH
presence separately from a nullable value so omitted and explicit-null middle names cannot be
confused:

```kotlin
sealed interface ProfileFieldChange<out T> {
    data object Unchanged : ProfileFieldChange<Nothing>
    data class Set<T>(val value: T) : ProfileFieldChange<T>
}

data class AccountProfilePatch(
    val firstName: ProfileFieldChange<String> = ProfileFieldChange.Unchanged,
    val middleName: ProfileFieldChange<String?> = ProfileFieldChange.Unchanged,
    val lastName: ProfileFieldChange<String> = ProfileFieldChange.Unchanged,
    val dateOfBirth: ProfileFieldChange<String> = ProfileFieldChange.Unchanged,
)
```

The data layer serializes only `Set` fields. `Set(null)` emits JSON `null`; `Unchanged` emits no key.
Names use Kotlin camelCase and wire fields retain backend snake_case through serialization mapping.

## Testing Strategy

Follow RED -> GREEN -> REFACTOR for every behavior slice.

### Unit tests

- Partial PATCH encoding: omitted fields, explicit-null middle name, and changed DOB.
- Retrofit request: correct PATCH path, exact body, and absent/present `X-Step-Up-Token`.
- Repository: complete account response mapping and both `422` envelope families.
- ViewModel: draft initialization, normalization, dirty detection, field validation, direct name save,
  DOB step-up requirement, OTP success, OTP/PATCH failure preservation, single-flight behavior, and
  canonical response/link-state adoption.
- Navigation: no supported source flow references the retired `EditProfile` destination.

### Compose UI tests

- Overview renders account and read-only contact/clinical boundaries.
- Edit mode renders first, middle, last, and DOB controls.
- DOB picker rejects today/future selection and emits exact `Y-m-d` through callbacks.
- Field errors attach to the correct controls.
- Saving/step-up disables mutation controls and exposes progress semantics.
- Dirty cancellation confirms before discard; clean cancellation exits immediately.

### Regression gates

- Full unit suite, lint, debug build, and Android-test compilation pass.
- Existing contact-management, password step-up, session routing, Profile hub, and linked Patient
  profile tests remain green.

## Boundaries

### Always

- Treat backend docs as the wire-contract source of truth.
- Send only supported changed fields and require step-up when DOB is present.
- Preserve drafts across patient-correctable failures.
- Map DTOs to `PatientAccount` at the repository boundary.
- Keep DOB and step-up proof memory-only and out of logs, analytics, Room, and saved-state bundles.
- Preserve the user's existing uncommitted backend-document changes.
- Run focused tests after each increment and all required gates before completion.

### Ask first

- Add a dependency or change build/CI configuration.
- Add or alter an authentication endpoint, proof lifecycle, or backend behavior.
- Persist DOB, OTP challenges, codes, or step-up tokens locally.
- Make any linked Patient field editable or synchronize account identity into clinical data.
- Reintroduce a second account editor instead of the proposed canonical Account & Security flow.

### Never

- Send email, phone, password, consent, link state, or clinic-owned fields through `PATCH /me`.
- Update or locally synthesize `linkedPatient` from account edits.
- Log request bodies, DOB, OTP codes, or step-up tokens.
- Use Gson, LiveData, or the `org.jetbrains.kotlin.android` plugin.
- Store tokens or health data in Room.
- Replace or commit unrelated user changes as though they were part of this feature.

## Success Criteria

1. Account & Security edits first, middle, and last names plus account DOB.
2. Name-only PATCH requests omit DOB and require no step-up header.
3. DOB-bearing PATCH requests do not execute until step-up succeeds and include the proof header.
4. PATCH serialization distinguishes omitted middle name from explicit `middle_name: null`.
5. Only normalized changed fields are sent; unchanged drafts cannot submit.
6. Local validation enforces required/non-blank first and last names, 255-character name limits, exact
   date parsing, and DOB before today in `Asia/Manila`.
7. Laravel validation errors attach to matching fields; machine-readable step-up errors receive safe
   flow-level handling.
8. Draft input survives OTP, validation, network, and protected-PATCH failures.
9. A successful response becomes the canonical account state and reflects server link-status changes.
10. Linked Patient demographics remain read-only and unchanged in every client state transition.
11. Contact and password flows retain their dedicated endpoints and current behavior.
12. The unreachable duplicate Edit Profile implementation is removed without breaking supported
    navigation or bottom-navigation behavior.
13. No new dependency, persistence, analytics, or sensitive logging is introduced.
14. Focused data, repository, ViewModel, navigation, and Compose tests pass.
15. `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleDebugAndroidTest` pass after
    formatting.
16. The final diff preserves the user's backend-document changes and contains no unrelated app work.

## Open Questions

1. **Resolved 2026-08-28:** The user approved Account & Security as the single canonical editor and
   removal of the unreachable legacy `EditProfile` destination/screen.
