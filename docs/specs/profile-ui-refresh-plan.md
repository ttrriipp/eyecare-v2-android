# Implementation Plan: Patient Profile UI Refresh

## Overview

Implement the confirmed direction in [`profile-ui-refresh-spec.md`](profile-ui-refresh-spec.md) as a sequence of small, testable presentation-layer changes. Profile becomes a warm patient account hub; Edit Profile becomes its visually cohesive form experience. Existing backend, repository, domain, and navigation contracts remain untouched, and the initials avatar remains the only identity graphic.

## Design Decision

### Chosen direction: personal care account

```text
Profile heading + supportive copy
  -> Patient identity surface
       -> Name, contact details, Edit profile
  -> Care & activity
       -> Messages + unread count
       -> Order History
       -> Prescriptions
       -> Feedback History
  -> Log out

Edit Profile
  -> Compact app bar
  -> Personal details surface
       -> Name
       -> Email
       -> Phone
  -> Cancel / Save changes
```

This direction retains the screen's useful destinations while replacing the current equal-weight settings list with a clear identity-to-activity hierarchy.

Alternatives considered and rejected:

- **Photo-led social profile:** requires image selection, storage, permissions, and backend support; explicitly excluded.
- **Dashboard card grid:** repeats the current hierarchy problem and gives every action equal visual weight on a narrow mobile screen.
- **Feature expansion:** address, password, account deletion, and new shortcuts would require contract or navigation decisions outside UI-only scope.
- **Decorative gradient hero:** adds visual noise and does not match the app's warm outlined-surface system.

## Architecture Decisions

- Keep `ProfileScreen` and `EditProfileScreen` as ViewModel-connected route composables.
- Extract stateless, internal content composables so representative states can be rendered without Hilt and tested with Compose UI tests.
- Retain the existing `User` model and `AuthRepository.updateUser(name, email, phone)` signature exactly.
- Keep dirty-form comparison in the presentation layer. Do not introduce a domain use case for UI-only form state.
- Preserve all route callbacks and lifecycle refresh behavior.
- Reuse `AppConfirmationDialog` for logout and dirty-form discard confirmation.
- Use existing `MaterialTheme` tokens, `outlineVariant` borders, and shape scale; do not change the global theme.

## Dependency Graph

```text
Observable UI contracts and test seam
             |
             v
  Main Profile hierarchy
             |
             v
 Edit Profile interaction states
             |
             v
 Accessibility + responsive review
             |
             v
      Full quality gates
```

The work is sequential because both screens share presentation conventions and the Edit screen depends on the test seam and interaction decisions established first.

## Task List

### Phase 1: UI Contract and Test Seam

#### Task 1: Establish stateless profile content and failing UI expectations

**Description:** Add focused Compose tests that describe the approved identity-first hierarchy, current navigation callbacks, optional phone handling, unread-message presentation, and the supported loading/error states. Introduce only the minimal stateless content seam needed to compile and render these states; verify the new behavioral assertions fail before implementing the redesign.

**Acceptance criteria:**

- [ ] Tests describe identity-first content, all four existing destinations, and a clearly labelled edit action.
- [ ] Tests cover phone present/absent and unread counts of zero and 10+.
- [ ] Tests prove every existing navigation callback remains wired.
- [ ] Loading content exposes an accessible loading description; errors retain a retry action.

**Verification:**

- [ ] Red: at least one new hierarchy/state assertion fails against the pre-redesign content.
- [ ] Android test sources compile after the minimal test seam is introduced.
- [ ] Existing profile unit tests remain unchanged and passing.

**Dependencies:** None.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Small - 2 files.

### Checkpoint: Profile contract

- [ ] The expected visible hierarchy and callbacks are executable specifications.
- [ ] No ViewModel, repository, navigation, or backend contract has changed.
- [ ] Tests are failing only for the intended not-yet-built presentation.

### Phase 2: Patient Account Hub

#### Task 2: Build the identity-first Profile experience

**Description:** Restructure the Profile success state into the approved account hub. Create a deliberate identity surface with initials, patient contact information, and a labelled edit action; follow it with a care/activity group whose rows include supportive labels and restrained icon treatments.

**Acceptance criteria:**

- [ ] Name is the strongest identity text, while email and optional phone remain readable secondary content.
- [ ] The avatar is initials-only, handles blank/whitespace names safely, and exposes no upload affordance.
- [ ] **Edit profile** is visible text with a 48dp-minimum target.
- [ ] Messages, Order History, Prescriptions, and Feedback History preserve their callbacks and have distinct supporting copy.
- [ ] The unread count appears near Messages, announces its meaning, and caps visually at `9+` without relying only on color.
- [ ] Surfaces use semantic tokens, subtle borders, and the existing shape hierarchy without new global theme values.

**Verification:**

- [ ] Green: the focused Profile Compose tests from Task 1 pass.
- [ ] Manually inspect long-name, missing-phone, zero-unread, and 10+-unread sample states at compact width.
- [ ] Compile the debug app after the slice.

**Dependencies:** Task 1.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Small - 2 files.

#### Task 3: Finish Profile loading, error, and logout presentation

**Description:** Replace the contextless loading spinner with a content-shaped, accessible loading state; preserve actionable load errors; and restyle logout as a separated, low-emphasis destructive action using the shared themed confirmation dialog.

**Acceptance criteria:**

- [ ] Loading resembles the final identity/activity layout and exposes `Loading profile` semantics.
- [ ] Load failure continues to show the repository message and retry callback.
- [ ] Logout is visually separate from care/activity navigation and remains clearly destructive.
- [ ] Logout confirmation uses `AppConfirmationDialog` with explicit **Log out** and **Stay signed in** actions.
- [ ] Confirming logout calls the existing ViewModel behavior exactly once; dismissing does not log out.

**Verification:**

- [ ] Compose tests cover loading semantics, retry, logout dismissal, and logout confirmation callbacks.
- [ ] Existing `ProfileViewModelTest` logout behavior still passes.
- [ ] Debug compilation succeeds after the slice.

**Dependencies:** Task 2.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Small - 2 files.

### Checkpoint: Main Profile complete

- [ ] Identity is the dominant content and activity navigation is easy to scan.
- [ ] All original destinations, refresh behavior, and logout behavior remain intact.
- [ ] No profile-photo or new account-feature affordance exists.
- [ ] Focused Profile UI tests pass.

### Phase 3: Cohesive Edit Profile

#### Task 4: Define Edit Profile behavior with failing tests

**Description:** Extend the test seam to specify the edit form's supported fields, dirty-state navigation, saving state, validation feedback, and generic save failure. Write the state/interaction expectations before changing production behavior.

**Acceptance criteria:**

- [ ] Tests cover name, email, and phone only; address and photo controls do not appear.
- [ ] Unchanged Back and Cancel exit immediately.
- [ ] Dirty app-bar Back, system Back, and Cancel request discard confirmation.
- [ ] Save submits directly, shows progress, and prevents duplicate actions while saving.
- [ ] Field validation and non-validation save failures are visible and accessible.

**Verification:**

- [ ] Red: dirty-navigation and/or save-error expectations fail before implementation.
- [ ] Existing save and editing unit tests remain green before production changes.
- [ ] Android test sources compile after adding the required stateless edit seam.

**Dependencies:** Task 3.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/profile/EditProfileScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/profile/ProfileViewModelTest.kt`

**Estimated scope:** Medium - 3 files.

#### Task 5: Implement the cohesive form and presentation state

**Description:** Restyle Edit Profile around a focused personal-details surface and implement the approved UI-only interaction improvements. Add only the presentation state necessary to show a generic save failure; leave repository calls and request fields unchanged.

**Acceptance criteria:**

- [ ] The header, form surface, typography, spacing, fields, and actions visually belong to the main Profile experience.
- [ ] Name uses a person keyboard/autofill intent, email uses email input behavior, and phone uses phone input behavior.
- [ ] Save is a direct primary action; the redundant save-confirmation dialog is removed.
- [ ] Dirty comparison uses the loaded name, email, and normalized phone values and drives Back/Cancel confirmation consistently.
- [ ] The discard prompt uses `AppConfirmationDialog`; no prompt appears when unchanged.
- [ ] Existing field errors remain attached to their fields, and a non-validation failure appears as concise inline feedback without clearing the draft.
- [ ] The repository update signature and submitted fields remain name, email, and nullable phone only.

**Verification:**

- [ ] Green: focused Edit Profile UI assertions and presentation-state unit tests pass.
- [ ] Test save success, validation failure, generic failure, dirty discard, and unchanged exit.
- [ ] Compile the debug app after the slice.

**Dependencies:** Task 4.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/profile/EditProfileScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/profile/ProfileViewModelTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Medium - 4 files.

### Checkpoint: Cohesive edit flow

- [ ] Profile and Edit Profile share one visual and interaction language.
- [ ] Back/Cancel behavior protects only real changes.
- [ ] Saving and errors remain understandable without backend changes.
- [ ] All focused profile unit and UI tests pass.

### Phase 4: Accessibility, Responsive Polish, and Verification

#### Task 6: Audit accessibility and compact-device behavior

**Description:** Review both screens as a complete flow at compact width and increased font scale. Fix only in-scope clipping, ordering, semantics, touch-target, focus, or contrast issues exposed by the review.

**Acceptance criteria:**

- [ ] Both screens remain usable at 320dp width and increased font scale without clipped controls or horizontal scrolling.
- [ ] TalkBack can identify the avatar initials, Edit profile, unread message count, navigation destinations, form fields, saving/error state, and destructive confirmations.
- [ ] Touch targets are at least 48dp, traversal order follows the visual hierarchy, and color is not the sole state indicator.
- [ ] Content remains clear above the overlaid bottom navigation on Profile and with the IME visible on Edit Profile.

**Verification:**

- [ ] Run the focused Compose tests on an emulator when available.
- [ ] Manually traverse interactive elements with keyboard/DPAD or accessibility focus when available.
- [ ] Inspect representative screenshots for default, compact, large-font, loading, error, dirty, and saving states.

**Dependencies:** Task 5.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/EditProfileScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/profile/ProfileScreenTest.kt`

**Estimated scope:** Medium - 3 files.

#### Task 7: Run final quality gates and scope review

**Description:** Run the project checks once after the final implementation change, review the diff against the approved spec, and report any emulator-limited checks accurately.

**Acceptance criteria:**

- [ ] Focused and full unit tests pass with no tests disabled.
- [ ] Formatting, lint, and required debug build pass.
- [ ] The final diff contains only the approved Profile/Edit Profile presentation, focused tests, spec, and plan.
- [ ] No backend, DTO, domain-user, repository, navigation, dependency, or global-theme change is present.

**Verification:**

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat ktlintCheck
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

If an emulator is available:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

If runtime instrumentation cannot run, compile the Android tests and report the remaining manual/runtime verification explicitly.

**Dependencies:** Task 6.

**Files likely touched:** None beyond earlier tasks unless a check exposes an in-scope defect.

**Estimated scope:** Extra small - verification only.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| The two context documents disagree on the brand primary color | Medium | Use existing Android `MaterialTheme` tokens and make no global palette decision in this screen-level refresh. |
| Adding address appears tempting because the backend supports it | Medium | Keep the current Android `User` and repository signatures unchanged; address remains an explicit non-goal. |
| UI tests are difficult through Hilt-connected route composables | Medium | Extract stateless content composables and test visible outcomes/callbacks directly. |
| Instrumentation cannot run without an emulator | Medium | Compile Android tests, use deterministic content states, and report runtime verification as pending rather than claiming it passed. |
| Long identity or validation text clips on compact devices | Medium | Avoid fixed content heights, use weighted/wrapping layouts carefully, and inspect 320dp plus increased font scale. |
| Dirty-state comparison produces false prompts for blank phone values | Low | Normalize blank phone to `null` consistently and unit test unchanged/changed cases. |
| Presentation polish expands into account-feature work | High | Enforce the explicit non-goals and pause for approval if a data, repository, route, dependency, or global-theme change becomes necessary. |

## Explicit Non-Goals

- Profile-photo upload, camera/gallery selection, avatar storage, or image permissions
- Address editing or any user DTO/domain/repository expansion
- Password management, account deletion, or authentication redesign
- New profile destinations, shortcuts, or navigation-graph changes
- Backend, database, API, or Laravel work
- Global palette, typography, shape, dark-mode, or design-system overhaul
- Refactoring unrelated screens or shared components

## Plan Approval Gate

Implementation begins only after this specification and plan receive explicit approval. Any discovery that requires a new dependency, backend/data contract, route, or global theme change pauses implementation for a new decision.
