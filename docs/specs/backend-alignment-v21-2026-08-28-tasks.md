# Task Breakdown: Backend Alignment v21 — Self-Service Account Profile

**Date:** 2026-08-28  
**Status:** Approved 2026-08-28 — implementation/verification in progress  
**Specification:** `docs/specs/backend-alignment-v21-2026-08-28-spec.md`  
**Plan:** `docs/specs/backend-alignment-v21-2026-08-28-plan.md`

## Execution Rules

- Execute tasks in dependency order and keep the project compilable at every checkpoint.
- For behavioral work, write or extend the focused test first, observe RED, implement the minimum
  GREEN change, then refactor while tests remain green.
- Run each task's focused verification after its last code edit. Do not rerun an unchanged command
  merely for reassurance.
- Commit each verified increment atomically with a descriptive conventional message.
- Stage only files owned by the increment. Never stage or rewrite the user's uncommitted
  `docs/API_CONTRACT.md` or `docs/BACKEND_CONTEXT.md` changes.
- If a task discovers a contract or scope change, update the approved spec first and return to human
  review before continuing.

Before Gradle commands in a new shell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Phase 0 — Approved workflow record

### Task 1: Record the approved v21 workflow documents

**Description:** Close the spec/plan/task gates in version control before production work begins,
without capturing the user's backend-document changes.

**Acceptance criteria:**

- [ ] Spec, plan, and task statuses say approved/implementation pending after task approval.
- [ ] The canonical Account & Security decision and all boundaries remain unchanged.
- [ ] Only the three v21 workflow files are staged and committed.

**Verification:**

- [ ] `git diff --check -- docs/specs/backend-alignment-v21-2026-08-28-spec.md docs/specs/backend-alignment-v21-2026-08-28-plan.md docs/specs/backend-alignment-v21-2026-08-28-tasks.md` passes.
- [ ] `git diff --cached --name-only` lists only the three v21 workflow files.
- [ ] Commit succeeds with a scoped documentation message.

**Dependencies:** Human approval of this task breakdown  
**Files:** Three v21 workflow Markdown files  
**Estimated scope:** M (3 files)

## Phase 1 — Partial PATCH contract

### Task 2: Define explicit partial-profile serialization

**Description:** Introduce the smallest model/mapper that can distinguish an omitted field from an
explicit nullable middle-name clear, and prove its exact Kotlinx JSON output before network wiring.

**Acceptance criteria:**

- [ ] `Unchanged` fields emit no JSON key and `Set(null)` emits an explicit JSON null.
- [ ] Only the four backend-allowlisted snake_case fields can be serialized.
- [ ] DOB remains an ordinary exact string at this layer; presentation validation stays elsewhere.

**Verification:**

- [ ] New focused serialization tests are observed failing before implementation and then pass via
  `.\gradlew testDebugUnitTest --tests "*AccountProfilePatchTest"`.
- [ ] `.\gradlew assembleDebug` passes.

**Dependencies:** Task 1  
**Files:**

- `app/src/main/java/com/eyecare/app/domain/model/AccountProfilePatch.kt` (new)
- `app/src/main/java/com/eyecare/app/data/remote/dto/AuthDtos.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/AccountProfilePatchTest.kt` (new)

**Estimated scope:** M (3 files)

### Task 3: Wire profile PATCH through Retrofit and the repository

**Description:** Add the general account-profile operation while temporarily retaining the old
name-only method for the still-compiling legacy editor.

**Acceptance criteria:**

- [ ] Retrofit calls `PATCH /me`, accepts an optional `X-Step-Up-Token`, and returns `MeResponse`.
- [ ] Repository requests contain only present patch fields and map the complete response to
  `PatientAccount` at the repository boundary.
- [ ] MockWebServer proves absent/present proof headers, explicit middle-name null, DOB body, and
  Laravel versus machine-readable `422` decoding.

**Verification:**

- [ ] Focused repository/error tests are observed RED before wiring and then pass via
  `.\gradlew testDebugUnitTest --tests "*AuthRepositoryImplTest" --tests "*ApiErrorDecoderTest"`.
- [ ] `.\gradlew assembleDebug` passes with existing presentation behavior unchanged.

**Dependencies:** Task 2  
**Files:**

- `app/src/main/java/com/eyecare/app/data/remote/api/AuthApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AuthRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AuthRepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiErrorDecoderTest.kt`

**Estimated scope:** M (5 files)

> ### Checkpoint A — Contract proof (after Tasks 1–3)
>
> - [ ] Approved workflow documents are committed without backend docs.
> - [ ] Exact PATCH presence/null semantics and optional proof header have automated evidence.
> - [ ] Both documented `422` envelopes decode through the existing boundary.
> - [ ] Focused tests and `assembleDebug` pass.

## Phase 2 — Editor state and protected save workflow

### Task 4: Model and validate the complete account-profile draft

**Description:** Expand Account Security editing state to all four fields and extract deterministic
normalization, validation, dirty comparison, and minimal-patch calculation.

**Acceptance criteria:**

- [ ] Editing initializes first, middle, last, and account DOB from `PatientAccount` only.
- [ ] First/last are required, all names cap at 255, blank middle becomes explicit null, and DOB must
  parse exactly before today in `Asia/Manila`.
- [ ] Normalized unchanged input creates no patch; changed input creates only the required fields.

**Verification:**

- [ ] Pure/editor ViewModel tests are observed RED before implementation and then pass via
  `.\gradlew testDebugUnitTest --tests "*AccountProfileEditorTest" --tests "*AccountSecurityViewModelTest"`.
- [ ] `.\gradlew assembleDebug` passes.

**Dependencies:** Checkpoint A  
**Files:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountProfileEditor.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/AccountProfileEditorTest.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/account/AccountSecurityViewModelTest.kt`

**Estimated scope:** M (4 files)

### Task 5: Implement the direct name-only save path

**Description:** Route a valid patch without DOB directly through the new repository operation and
map validation/non-validation failures back to the preserved editor draft.

**Acceptance criteria:**

- [ ] Name/middle-only patches save without requesting step-up or sending its header.
- [ ] Save is single-flight, normalized no-ops cannot submit, and success adopts the returned account.
- [ ] Laravel errors map by field; unknown/profile/network failures use safe form-level feedback and
  preserve every draft value.

**Verification:**

- [ ] ViewModel tests are observed RED before implementation and then pass via
  `.\gradlew testDebugUnitTest --tests "*AccountSecurityViewModelTest"`.
- [ ] Existing Account Security tests remain green and `.\gradlew assembleDebug` passes.

**Dependencies:** Task 4  
**Files:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/AccountSecurityViewModelTest.kt`

**Estimated scope:** S (2 files)

### Task 6: Add DOB step-up orchestration with draft recovery

**Description:** Reuse the existing step-up challenge/verification flow for DOB-bearing patches and
guarantee that a single-use proof and all failure paths behave safely.

**Acceptance criteria:**

- [ ] A changed DOB requests step-up and does not call PATCH before OTP verification succeeds.
- [ ] Successful verification sends the pending atomic patch once with the proof token.
- [ ] OTP cancellation/failure and protected PATCH failure preserve the draft, clear proof state, and
  require a new challenge on retry.

**Verification:**

- [ ] DOB workflow tests are observed RED before implementation and then pass via
  `.\gradlew testDebugUnitTest --tests "*AccountSecurityViewModelTest"`.
- [ ] Existing contact/password step-up ViewModel tests remain green.
- [ ] `.\gradlew assembleDebug` passes.

**Dependencies:** Task 5  
**Files:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/AccountSecurityViewModelTest.kt`

**Estimated scope:** S (2 files)

> ### Checkpoint B — Workflow proof (after Tasks 4–6)
>
> - [ ] Minimal patches, local validation, direct saves, and protected saves have state-based tests.
> - [ ] Drafts survive all patient-correctable failures and proofs remain memory-only/single-use.
> - [ ] Existing contact and password workflows remain green.
> - [ ] Focused tests and `assembleDebug` pass.

## Phase 3 — Session and Compose integration

### Task 7: Reconcile returned accounts with shared session state

**Description:** Adopt the server's complete account after load/save so the shared session reflects
the returned link status without locally modifying clinical fields.

**Acceptance criteria:**

- [ ] Session adoption maps linked accounts to `SessionState.Linked` and all other statuses to
  `SessionState.Limited`.
- [ ] Account Security reports loaded/updated canonical accounts to the session owner without a
  second editor or a synthetic Patient mutation.
- [ ] Existing generation/cancellation protections still prevent stale session results from winning.

**Verification:**

- [ ] Session tests are observed RED before implementation and then pass via
  `.\gradlew testDebugUnitTest --tests "*SessionViewModelTest" --tests "*SessionRoutingTest"`.
- [ ] `.\gradlew assembleDebug` passes.

**Dependencies:** Checkpoint B  
**Files:**

- `app/src/main/java/com/eyecare/app/presentation/auth/SessionViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/SessionViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/account/AccountSecurityScreenTest.kt`

**Estimated scope:** M (5 files)

### Task 8: Render all editable fields and the account DOB picker

**Description:** Expand the canonical Account Details editor and reuse the established Material 3
date-picker pattern while keeping contact and clinical values read-only.

**Acceptance criteria:**

- [ ] Edit mode renders first, middle, last, and a non-freeform account DOB control.
- [ ] Date selection excludes today/future in `Asia/Manila`, displays friendly copy, and sends exact
  `Y-m-d` through the ViewModel callback.
- [ ] Email, phone, role, link status, and linked Patient values have no ordinary edit controls.

**Verification:**

- [ ] Compose tests are written first and compiled via
  `.\gradlew assembleDebugAndroidTest` after implementation.
- [ ] Focused Account Security unit tests and `.\gradlew assembleDebug` pass.

**Dependencies:** Task 7  
**Files:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/account/AccountSecurityScreenTest.kt`

**Estimated scope:** S (2 files)

### Task 9: Complete editor errors, progress, and discard behavior

**Description:** Finish the canonical UX around the proven save flow without changing its contract.

**Acceptance criteria:**

- [ ] Local/backend field errors render beside the matching field; profile/unknown errors render at
  form level with patient-safe copy.
- [ ] Saving and step-up verification disable duplicate actions and expose accessible progress.
- [ ] Dirty Back/Cancel asks before discard, clean exit is immediate, and OTP cancellation returns to
  the preserved draft.

**Verification:**

- [ ] ViewModel and Compose behavior tests are written/updated before implementation.
- [ ] `.\gradlew testDebugUnitTest --tests "*AccountSecurityViewModelTest"` passes.
- [ ] `.\gradlew assembleDebugAndroidTest` and `.\gradlew assembleDebug` pass.

**Dependencies:** Task 8  
**Files:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityViewModel.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/account/AccountSecurityScreenTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/AccountSecurityViewModelTest.kt`

**Estimated scope:** M (4 files)

> ### Checkpoint C — User-flow proof (after Tasks 7–9)
>
> - [ ] Canonical account/session state follows the server response.
> - [ ] Account & Security visibly edits exactly four allowed fields.
> - [ ] Date, validation, progress, step-up, and discard interactions have deterministic coverage.
> - [ ] Unit tests, Android-test compilation, and `assembleDebug` pass.

## Phase 4 — Single-editor cutover

### Task 10: Remove the unreachable Edit Profile destination

**Description:** Delete the duplicate screen only after the canonical path is green, and update
navigation/UI tests to prove supported flows still use Account & Security.

**Acceptance criteria:**

- [ ] `EditProfile` typed route, NavGraph composable/import, and `EditProfileScreen.kt` are absent.
- [ ] Profile hub still opens Account & Security and linked-only Patient Profile correctly.
- [ ] Bottom-navigation visibility and Profile Compose tests remain correct without legacy fixtures.

**Verification:**

- [ ] `rg -n "EditProfile|EditProfileScreen" app/src/main app/src/test app/src/androidTest` returns no
  active symbol.
- [ ] `.\gradlew testDebugUnitTest --tests "*BottomNavVisibilityTest"` passes.
- [ ] `.\gradlew assembleDebugAndroidTest` and `.\gradlew assembleDebug` pass.

**Dependencies:** Checkpoint C  
**Files:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/EditProfileScreen.kt` (delete)
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/BottomNavVisibilityTest.kt`

**Estimated scope:** M (5 files)

### Task 11: Remove duplicate ProfileViewModel editing and old repository method

**Description:** Simplify the retained Profile hub state and delete the temporary name-only contract
after its final caller is gone.

**Acceptance criteria:**

- [ ] `ProfileViewModel` retains load, linked-account adoption, refresh, logout, and related tests but
  no editing draft/save state.
- [ ] `AuthRepository.updateAccountName` and its implementation are absent; all account edits use the
  partial-profile operation.
- [ ] Static search finds no production `updateAccountName`, editing-only Profile state, or legacy
  route symbol.

**Verification:**

- [ ] `.\gradlew testDebugUnitTest --tests "*ProfileViewModelTest" --tests "*AuthRepositoryImplTest"` passes.
- [ ] Full `.\gradlew testDebugUnitTest` and `.\gradlew assembleDebug` pass.

**Dependencies:** Task 10  
**Files:**

- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/profile/ProfileViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AuthRepositoryImpl.kt`

**Estimated scope:** M (4 files)

> ### Checkpoint D — Single-editor proof (after Tasks 10–11)
>
> - [ ] Account & Security is the only supported account editor.
> - [ ] Profile hub and linked Patient Profile remain intact.
> - [ ] No legacy editor route/screen/state/repository method remains.
> - [ ] Full unit tests, Android-test compilation, and `assembleDebug` pass.

## Phase 5 — Living documentation and final evidence

### Task 12: Reconcile current Android documentation

**Description:** Update living project guidance only after behavior is green, while preserving the
backend team's uncommitted source documents.

**Acceptance criteria:**

- [ ] `CONTEXT.md` describes the four editable account fields, DOB step-up, two error envelopes,
  single Account & Security editor, and read-only linked Patient boundary.
- [ ] `AGENTS.md` current-work pointer targets the v21 spec.
- [ ] Spec, plan, and tasks say implementation/verification in progress; no completion claim is made
  before the final gate.

**Verification:**

- [ ] `git diff --check -- CONTEXT.md AGENTS.md docs/specs/backend-alignment-v21-2026-08-28-spec.md docs/specs/backend-alignment-v21-2026-08-28-plan.md docs/specs/backend-alignment-v21-2026-08-28-tasks.md` passes.
- [ ] Scoped search confirms current wording and no stale name-only profile statement in
  `CONTEXT.md`.
- [ ] `.\gradlew assembleDebug` passes.

**Dependencies:** Checkpoint D  
**Files:**

- `CONTEXT.md`
- `AGENTS.md`
- Three v21 workflow Markdown files

**Estimated scope:** M (5 files)

### Task 13: Run the final gate and close v21

**Description:** Execute the full required verification suite, reopen the owning task for any failure,
and mark the workflow complete only after all evidence passes.

**Acceptance criteria:**

- [ ] All 16 specification success criteria have automated or static evidence.
- [ ] No DOB, OTP code, step-up token, request body, or linked clinical field is persisted/logged by
  the feature; no Gson, LiveData, dependency, or Kotlin Android plugin change was introduced.
- [ ] Final scoped diff/commit review preserves the user's backend-document changes and contains no
  unrelated app work.

**Verification:**

- [ ] `.\gradlew ktlintFormat` completes.
- [ ] `.\gradlew testDebugUnitTest` passes.
- [ ] `.\gradlew lintDebug` passes.
- [ ] `.\gradlew assembleDebug` passes.
- [ ] `.\gradlew assembleDebugAndroidTest` passes.
- [ ] `.\gradlew connectedDebugAndroidTest` runs when a device/`adb` is available or is explicitly
  recorded as not run.
- [ ] `git diff --check` passes and final statuses/evidence are accurate.

**Dependencies:** Task 12  
**Files:** Three v21 workflow Markdown files; any production/test failure reopens its owning task  
**Estimated scope:** M (verification plus 3 evidence files)

> ### Checkpoint E — Completion (after Task 13)
>
> - [ ] All v21 success criteria and task acceptance boxes have evidence.
> - [ ] Formatting, unit, lint, debug build, and Android-test compilation pass.
> - [ ] Instrumented execution availability is recorded honestly.
> - [ ] Documentation and workflow statuses match the verified implementation.
> - [ ] User backend documents remain preserved and unstaged by v21 commits.

## Dependency Summary

```text
1 -> 2 -> 3 -> Checkpoint A
Checkpoint A -> 4 -> 5 -> 6 -> Checkpoint B
Checkpoint B -> 7 -> 8 -> 9 -> Checkpoint C
Checkpoint C -> 10 -> 11 -> Checkpoint D
Checkpoint D -> 12 -> 13 -> Checkpoint E
```

All production work is intentionally sequential because the slices share the PATCH contract,
Account Security state, and navigation. No parallel implementation ownership is planned.

## Open Questions

1. None. Spec and technical plan decisions were approved on 2026-08-28; this task record awaits its
   implementation authorization.
