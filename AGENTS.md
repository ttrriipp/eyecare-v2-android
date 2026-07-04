# EyecareV2 Android App

> Full project context: see `CONTEXT.md`

## Commands
- Build: `./gradlew assembleDebug`
- Test: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Format: `./gradlew ktlintFormat`

## Quick Reference
- Package: `com.eyecare.app`
- Architecture: MVVM + Clean (data → domain → presentation)
- DI: Hilt | Network: Retrofit + Kotlinx Serialization | UI: Compose + Material 3
- Navigation: type-safe routes (`@Serializable` objects)
- State: `sealed interface` via `StateFlow` (no LiveData)
- Backend docs: `docs/BACKEND_CONTEXT.md`
- Current work: `docs/specs/backend-alignment-v2-spec.md`

## Boundaries
- Never use Gson — only Kotlinx Serialization
- Never store tokens/health data in Room
- Never apply `org.jetbrains.kotlin.android` plugin (AGP 9 built-in)
- Always run `./gradlew assembleDebug` after changes
- Always map DTOs → domain models at repository boundary
