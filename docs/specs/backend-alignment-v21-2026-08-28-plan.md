# Implementation Plan: Backend Alignment v21 — Self-Service Account Profile

**Date:** 2026-08-28  
**Status:** Approved 2026-08-28 — implementation/verification in progress  
**Specification:** `docs/specs/backend-alignment-v21-2026-08-28-spec.md`

## Overview

Extend the canonical **Account & Security** editor from first/last-name updates to an exact partial
`PATCH /me` implementation for first, middle, and last names plus account DOB. The plan reuses the
existing step-up OTP endpoints for DOB-bearing requests, preserves drafts across recoverable errors,
adopts the server's complete returned account, and removes the unreachable duplicate Edit Profile
destination after the canonical path is proven.

The implementation proceeds contract-first and risk-first: prove PATCH presence/null semantics and
the optional step-up header before changing presentation state, then add the protected DOB workflow,
then the UI, and only then remove the obsolete editor.

## Architecture Decisions

1. **One canonical editor:** Account & Security owns all self-service account identity editing.
   Profile remains the account hub and the linked clinic Patient profile remains read-only.
2. **Explicit PATCH presence:** A domain/data representation distinguishes an omitted field from an
   explicitly supplied nullable value. This is required for `middle_name: null` versus no
   `middle_name` key.
3. **Repository boundary:** `AuthRepository` exposes a profile-patch operation returning
   `PatientAccount`. `AuthRepositoryImpl` serializes only present fields and maps the complete
   response at the existing DTO-to-domain boundary.
4. **Optional proof header:** Retrofit accepts a nullable `X-Step-Up-Token`; name-only calls omit the
   header, while DOB-bearing calls can only reach the repository after step-up succeeds.
5. **Draft-carrying step-up action:** The existing Account Security OTP state gains a profile-update
   action that carries the normalized draft/patch in memory. OTP cancellation and failures restore
   that draft; single-use proofs are never persisted.
6. **Server-canonical success:** The returned account replaces the editor snapshot. A small session
   adoption hook reconciles the shared `SessionState` from the returned `link_status` without
   synthesizing clinical data.
7. **Two documented error families:** Existing `ApiErrorDecoder` remains the central decoder. The
   editor consumes Laravel field errors and machine-readable step-up errors without parsing raw JSON
   in the ViewModel.
8. **Legacy removal last:** The typed `EditProfile` route, screen, duplicate state, and old
   name-specific repository operation remain until the canonical flow and tests are green, then are
   removed together.

## Dependency Graph

```text
Approved spec
    |
    v
PATCH model + serializer + repository contract
    |
    v
Account Security draft/validation state
    |
    v
DOB step-up orchestration + session adoption
    |
    v
Compose editor/date picker/error UI
    |
    v
Legacy EditProfile removal
    |
    v
Living docs + full verification
```

## Implementation Phases

### Phase 1 — Contract and serialization foundation

- Add an explicit partial-profile change representation with omitted versus `Set(null)` semantics.
- Extend the Retrofit `PATCH me` method with an optional step-up header.
- Add `AuthRepository.updateAccountProfile(...)` while temporarily retaining the old name-only method
  so the project stays compilable before legacy removal.
- Serialize only present allowlisted keys using Kotlinx Serialization primitives; never introduce
  Gson or a second error parser.
- Add MockWebServer/unit coverage for exact request paths, bodies, null clearing, omitted DOB,
  present proof headers, response mapping, and both documented `422` envelopes.

#### Checkpoint A — Contract proof

- Focused DTO/repository/error tests pass.
- A name-only fixture contains no `date_of_birth` key or step-up header.
- A middle-name clear contains `"middle_name": null`.
- A DOB fixture contains exact `Y-m-d` and the provided `X-Step-Up-Token`.
- `assembleDebug` passes with existing UI behavior unchanged.

### Phase 2 — Account editor state and protected workflow

- Replace the Account Security name-only draft fields with one testable profile draft containing
  first, middle, last, DOB, field errors, dirty state, and save/step-up progress.
- Normalize and validate names and DOB in `Asia/Manila`; compute the smallest supported patch.
- Send name-only patches directly.
- Add `StepUpAction.UpdateProfile` so DOB-bearing patches request OTP, verify it, and execute with the
  single-use proof.
- Preserve the exact edit draft across OTP cancellation, invalid OTP, validation, network, and PATCH
  failures. Clear the proof after every attempt.
- Add a session-account adoption operation that maps the returned `link_status` to linked or limited
  session state and ignores no server-owned field.
- Add ViewModel/session tests before each behavior is implemented.

#### Checkpoint B — Workflow proof

- Account Security ViewModel tests prove dirty comparison, direct name save, DOB step-up, draft
  preservation, field mapping, single-flight behavior, and canonical success state.
- Session tests prove linked remains linked and pending/unlinked remain limited after adoption.
- Existing contact and password step-up tests remain green.
- `assembleDebug` passes.

### Phase 3 — Canonical Compose editor

- Expand Account Details edit mode to first, middle, last, and account DOB.
- Reuse the existing Material 3 date-picker pattern with today/future dates disabled in
  `Asia/Manila`; display a friendly date while callbacks retain exact `Y-m-d`.
- Attach backend/local errors to their matching controls and keep unknown/profile errors at form
  level.
- Disable controls while saving or verifying and expose accessible progress semantics.
- Add dirty-discard confirmation for app-bar/system Back and Cancel; clean exits remain immediate.
- Update OTP copy for a pending profile update without changing contact/password flows.
- Add deterministic Compose UI tests for fields, read-only boundaries, date selection, errors,
  progress, save, and discard behavior.

#### Checkpoint C — User-flow proof

- Focused Account Security unit and Compose test compilation passes.
- The accessible account editor covers all four allowed fields and no prohibited field.
- Name-only and DOB-bearing save paths are wired end to end through tested callbacks.
- `assembleDebugAndroidTest` and `assembleDebug` pass.

### Phase 4 — Remove the duplicate editor and old contract

- Prove no supported UI callback navigates to the typed `EditProfile` destination.
- Remove the `EditProfile` route/composable/import, legacy screen, and its obsolete Compose test.
- Remove editing-only state and functions from `ProfileViewModel` while retaining Profile load,
  linked-account adoption, refresh, and logout behavior.
- Remove the temporary old `updateAccountName` repository operation after all callers are gone.
- Update navigation/profile tests and ensure bottom-navigation visibility remains correct.

#### Checkpoint D — Single-editor proof

- Static search finds no `EditProfile`, `EditProfileScreen`, or `updateAccountName` production symbol.
- Profile hub, Account Security, session routing, and bottom-navigation tests pass.
- Full unit tests and `assembleDebug` pass.

### Phase 5 — Living documentation and final verification

- Update `CONTEXT.md` so the active Profile/Account Security description matches the shipped v21
  fields, step-up behavior, error handling, and clinical boundary.
- Update `AGENTS.md` current-work pointer to the approved v21 spec.
- Preserve the user's `docs/API_CONTRACT.md` and `docs/BACKEND_CONTEXT.md` edits byte-for-byte unless
  a newly discovered blocking contradiction requires explicit user approval.
- Create and maintain the approved v21 task record with evidence from actual commands.
- Run formatting, full unit tests, lint, debug build, and Android-test compilation; run connected
  instrumentation only if a device and `adb` are available.

#### Checkpoint E — Completion

- All specification success criteria have test or static evidence.
- `ktlintFormat`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and
  `assembleDebugAndroidTest` pass in that order after the last production edit.
- Sensitive-value/logging and prohibited-technology searches are clean.
- Final scoped diff review contains no unrelated app changes and preserves the user's backend docs.
- Spec, plan, and tasks statuses match the verified implementation state.

## Verification Commands

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew testDebugUnitTest --tests "*ApiErrorDecoderTest" --tests "*AuthRepositoryImplTest"
.\gradlew testDebugUnitTest --tests "*AccountSecurityViewModelTest" --tests "*SessionViewModelTest"
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintFormat
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
.\gradlew assembleDebugAndroidTest
```

Static verification will also confirm:

```powershell
rg -n "EditProfile|EditProfileScreen|updateAccountName" app/src/main app/src/test app/src/androidTest
rg -n -i "date.?of.?birth|step.?up|X-Step-Up-Token" app/src/main app/src/test app/src/androidTest
rg -n -i "Gson|LiveData|org.jetbrains.kotlin.android" app build.gradle.kts settings.gradle.kts
```

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Nullable middle name is omitted instead of cleared | High | Explicit presence model plus exact-body MockWebServer tests |
| DOB is sent without step-up or unchanged DOB triggers OTP | High | Compute patch before routing; test absent/present header and direct/protected paths |
| OTP/PATCH failure loses patient input | High | Carry draft in state/pending action; state-based failure tests |
| Single-use proof is accidentally reused | High | Keep proof local to one execution and request a new challenge for retry |
| Account DOB is confused with linked Patient DOB | High | Separate account draft only; invariant tests that `linkedPatient` is untouched |
| Backend field errors appear as generic failures | Medium | Map `ApiDomainError.fieldErrors` by wire key with form-level fallback |
| Session retains stale pending-review status | Medium | Adopt complete returned account through tested session mapping |
| Removing legacy editor breaks navigation | Medium | Remove last, after static/navigation tests prove it unreachable |
| Existing contact/password step-up regresses | Medium | Reuse shared primitives and run focused regression suites at Checkpoint B |
| User backend-doc changes are overwritten | High | Never patch those files; use scoped staging/diff review |

## Parallelization

No parallel code edits are recommended. Contract, Account Security state, screen, navigation, and
tests share central types and should be changed sequentially to keep every checkpoint compilable.
Independent review of the completed plan/tasks is safe, but implementation ownership remains serial.

## Open Questions

1. None. The canonical-editor decision was approved with the v21 specification on 2026-08-28.
