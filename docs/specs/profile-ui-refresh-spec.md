# Spec: Patient Profile UI Refresh

## Status

Confirmed intent approved on 2026-07-20. This specification and its companion implementation plan are ready for human review; implementation remains gated on explicit plan approval.

## Objective

Redesign the Android **Profile** and **Edit Profile** screens as one cohesive, warm, patient-focused account experience. The existing screens are functional but visually flat and generic. The refresh should improve hierarchy, personality, clarity, accessibility, and state presentation without expanding the backend or account feature set.

The profile remains the patient's account hub for:

- Recognizing their signed-in identity and opening profile editing.
- Reaching Messages, Order History, Prescriptions, and Feedback History.
- Signing out safely.
- Editing the currently supported name, email, and phone fields.

## Confirmed Intent

- **Outcome:** A polished, warm, trustworthy profile experience rather than a generic settings list.
- **User:** An authenticated Eyecare patient.
- **Why now:** The current layout works but feels dull, flat, and visually disconnected.
- **Success:** Strong hierarchy, purposeful surfaces, clear actions, accessible interactions, and cohesive Profile/Edit Profile styling.
- **Binding constraint:** Presentation-layer work only; no backend, data-contract, repository, or navigation expansion.
- **Explicit exclusion:** No profile-photo upload or image-selection flow. The avatar remains initials-based.

## UX Direction

### Profile screen

1. Lead with a compact page heading and supportive account-oriented copy.
2. Present identity in a deliberate outlined hero surface:
   - Initials-based avatar derived from the patient's name.
   - Name as the strongest text.
   - Email and optional phone as secondary information.
   - A clearly labelled **Edit profile** action instead of relying on an icon alone.
3. Present Messages, Order History, Prescriptions, and Feedback History as a **Care & activity** group:
   - Each destination keeps its existing callback.
   - Tinted icon treatments and short supporting labels improve scanning.
   - The unread-message count remains visible and understandable without relying on color alone.
4. Treat **Log out** as a low-emphasis destructive account action, separated from primary activity.
5. Replace the stock logout dialog with the existing themed `AppConfirmationDialog`.

### Edit Profile screen

1. Match the Profile screen's surface, spacing, typography, and icon language.
2. Keep only the supported fields: name, email, and phone.
3. Group fields under a clear **Personal details** hierarchy with appropriate keyboard types, autofill semantics where supported, and validation messaging.
4. Save directly from the primary action rather than adding a second confirmation step.
5. Ask to discard only when values differ from the loaded user; unchanged Back/Cancel exits immediately.
6. Apply the same dirty-state behavior to the app-bar Back action, system Back, and Cancel action.
7. Keep saving progress and validation failures visible without shifting or clipping controls.

### Visual language

- Use `MaterialTheme` semantic colors and the existing Android cyan/charcoal/warm-surface palette.
- Use the established 12dp/16dp/24dp shape hierarchy and subtle `outlineVariant` borders.
- Prefer quiet white surfaces over heavy shadows, gradients, or a grid of equal-weight cards.
- Reserve strongest emphasis for identity and the primary edit/save actions.
- Do not add global theme tokens unless implementation proves an existing token is insufficient and the user approves the expansion.

## UI States

The plan must cover:

- Profile loading with an accessible, content-shaped placeholder.
- Profile load error with the existing retry behavior.
- Profile success with and without a phone number.
- Messages with zero, single-digit, and 10+ unread counts.
- Edit idle, dirty, saving, validation-error, and non-validation save-error states.
- Long names/emails, 320dp compact width, and increased font scale.

## Technical Context

- Kotlin 2.3.0 with AGP 9.2.1 built-in Kotlin support.
- Jetpack Compose and Material 3.
- MVVM + Clean Architecture (`data -> domain -> presentation`).
- Hilt-injected `ProfileViewModel` with sealed UI state through `StateFlow`.
- JUnit 5, MockK, Turbine/coroutines-test, and Compose instrumentation tests.

## Commands

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

## Relevant Project Structure

```text
app/src/main/java/com/eyecare/app/presentation/profile/
  ProfileScreen.kt            Profile route and account-hub UI
  EditProfileScreen.kt        Edit route and form UI
  ProfileViewModel.kt         Presentation state and save/logout behavior

app/src/test/java/com/eyecare/app/presentation/profile/
  ProfileViewModelTest.kt     Presentation-state unit tests

app/src/androidTest/java/com/eyecare/app/presentation/profile/
  ProfileScreenTest.kt        New stateless Compose UI coverage

app/src/main/java/com/eyecare/app/presentation/common/components/
  AppConfirmationDialog.kt    Existing themed confirmation dialog
```

## Code Style

Follow existing semantic tokens and outlined-surface treatment:

```kotlin
Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
) {
    // Focused, stateless content composed from small private sections.
}
```

- Keep ViewModel-connected route composables thin.
- Extract stateless content composables for deterministic previews and UI tests.
- Prefer composition over configurable all-purpose components.
- Use semantic colors, typography, and shapes rather than raw color or arbitrary spacing values.
- Keep interactive targets at least 48dp and provide visible labels or content descriptions.

## Testing Strategy

- Write failing Compose assertions before implementing each new UI contract.
- Test visible outcomes and callbacks, not internal composable structure.
- Extend `ProfileViewModelTest` only for presentation behavior introduced by the UI refresh, such as dirty/save error state.
- Keep existing repository and backend tests unchanged because their contracts are out of scope.
- Compile instrumentation tests even when no emulator is available; report runtime verification separately.
- Manually inspect representative compact-width and large-font states when an emulator or preview is available.

## Boundaries

### Always

- Preserve every existing profile destination and callback.
- Preserve lifecycle refresh, logout token clearing, and existing repository calls.
- Keep `ProfileUiState` a sealed interface exposed through `StateFlow`.
- Use the existing themed confirmation dialog for destructive/discard prompts.
- Run the required debug build after implementation changes.

### Ask first

- Add any dependency or global theme token.
- Change the navigation graph or destination ownership.
- Expand the editable user-field contract.

### Never

- Add profile-photo upload, camera/gallery selection, or image storage.
- Add or modify backend endpoints, DTO fields, domain fields, or repository signatures.
- Add address editing under this UI-only scope.
- Add password management, account deletion, or unrelated account features.
- Use Gson, store tokens/health data in Room, or apply `org.jetbrains.kotlin.android`.

## Success Criteria

- The Profile screen reads as a patient account hub, with identity first and care/activity navigation second.
- Profile and Edit Profile share one intentional visual language.
- All current navigation, refresh, editing, save, and logout capabilities still work.
- No photo-upload affordance or backend/data-contract change appears in the diff.
- Loading, error, empty/optional, saving, and validation states are intentional and accessible.
- The UI remains usable at 320dp width and with increased font scale.
- Focused unit/UI tests pass, formatting and lint pass, and `assembleDebug` succeeds.

## Open Questions

None blocking. The plan assumes the current Android semantic primary color remains authoritative for this screen; reconciling the separate backend/admin branding value is outside this refresh.
