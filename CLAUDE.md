# EyecareV2 Android App

## Tech Stack
- AGP 9.2.1 (built-in Kotlin — no `kotlin.android` plugin)
- Kotlin 2.3.0, KSP 2.3.9
- Jetpack Compose + Material 3 (BOM 2026.05.01)
- Hilt 2.59.2 (DI)
- Retrofit 2.11 + OkHttp 4.12 + Kotlinx Serialization 1.8.1
- Room 2.7.1 (products cache only)
- Navigation Compose 2.9.0 (type-safe routes)
- Coil 3.1.0 (image loading)
- CameraX 1.5.0 + MediaPipe 0.10.35 (AR try-on)
- Security-Crypto 1.1.0-alpha06 (token storage)

## Commands
- Build: `./gradlew assembleDebug`
- Test: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Format: `./gradlew ktlintFormat`
- Format check: `./gradlew ktlintCheck`

## Architecture
MVVM + Clean Architecture: `data/` → `domain/` → `presentation/`
- DTOs live in `data/remote/dto/`, map to domain models at the repository boundary
- Domain models in `domain/model/`, repository interfaces in `domain/repository/`
- Repository implementations in `data/repository/`
- ViewModels in `presentation/<feature>/`, annotated `@HiltViewModel`
- UI state as `sealed interface` emitted via `StateFlow`
- No LiveData

## Package: `com.eyecare.app`

## Key Conventions
- All Hilt-injected ViewModels use `@HiltViewModel` + `@Inject constructor`
- Retrofit errors: catch `HttpException`, parse body for 422/429
- 401 responses signal global logout via a `SharedFlow` in `AuthEventBus`
- Room only for `ProductEntity` — never store tokens or prescriptions in Room
- `kotlinOptions {}` is gone in AGP 9 — use `kotlin { compilerOptions {} }`
- Tests use JUnit 5 (`@Test` from `org.junit.jupiter.api`), MockK, Turbine, coroutines-test

## Boundaries
- Never use Gson — only Kotlinx Serialization
- Never store sensitive data (tokens, prescriptions) in Room
- Never add `android.disallowKotlinSourceSets=false` to gradle.properties
- Never apply `org.jetbrains.kotlin.android` plugin (AGP 9 has built-in Kotlin)
- Always run `./gradlew assembleDebug` after Gradle changes

## Implementation Plan
See `docs/implementation-plan.md`. Tasks 2–28 are pending.
Task 1 (Gradle scaffolding) is complete and committed.
