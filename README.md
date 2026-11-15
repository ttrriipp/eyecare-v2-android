# EyeCare Android

EyeCare is the patient-facing Android app for Padilla Optical Clinic. It connects to the clinic's Laravel REST API so patients can browse eyewear, try selected frames on with the camera, request appointments, and access account and care information.

## Features

- Browse and search frames and accessories, view product details, and save frames.
- Try supported frames on with the camera using 3D assets, CameraX, and MediaPipe.
- Request and manage appointments, including current appointment details and appointment history.
- View prescriptions, billing records, optical orders, and clinic notifications.
- Send messages to clinic staff and submit accessory order requests.
- Register and sign in with phone verification, with account linking for access to patient features.

Some patient features depend on the account's link status with the clinic. The backend remains authoritative for account access and clinical data.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM + Clean Architecture |
| Dependency injection | Hilt |
| Networking | Retrofit, OkHttp, Kotlinx Serialization |
| Local storage | Room for catalog data; AndroidX Security for sensitive local credentials |
| Navigation | Navigation Compose with serializable, type-safe routes |
| Camera and 3D | CameraX, MediaPipe, SceneView / Filament |

The app uses Android SDK 35 and supports Android 8.0 (API 26) and later.

## Getting started

### Requirements

- Android Studio with Android SDK Platform 35 installed.
- JDK 17 or later (the Gradle wrapper is included). `gradle.properties` currently pins Gradle to a Windows Android Studio JDK path; on other systems, update or remove the `org.gradle.java.home` override.
- Access to a running EyeCare backend if you want to use API-backed features.

### Build the debug app

```bash
git clone https://github.com/ttrriipp/eyecare-v2-android.git
cd eyecare-v2-android
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug` from Command Prompt or PowerShell.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Configure the backend URL

The debug build defaults to `http://10.0.2.2/api/v1/`, which reaches the host machine from the Android emulator. To use another backend, add these properties to the root `local.properties` file (keep any existing `sdk.dir` entry):

```properties
api.base.url=http://10.0.2.2/api/v1/
api.catalog.image.base.url=
api.release.url=https://your-api-host.example/api/v1/
```

`api.base.url` configures debug builds and should end with `/`. Use a host reachable from the device when running on physical hardware. `api.catalog.image.base.url` is optional and is only needed when catalog images use a separate base URL. Set `api.release.url` to the production API before building a release; its default is a placeholder. `local.properties` is ignored by Git.

## Common commands

| Command | Description |
| --- | --- |
| `./gradlew assembleDebug` | Build the debug APK |
| `./gradlew testDebugUnitTest` | Run local unit tests |
| `./gradlew lintDebug` | Run Android lint |
| `./gradlew ktlintCheck` | Check Kotlin formatting |
| `./gradlew ktlintFormat` | Apply Kotlin formatting |

## Architecture

The app follows a `data → domain → presentation` structure under `app/src/main/java/com/eyecare/app/`:

- `data/` contains Retrofit services, serializable API DTOs, repository implementations, and local storage.
- `domain/` contains app models and repository interfaces.
- `presentation/` contains Compose screens, ViewModels, and navigation.
- `di/` contains Hilt modules; `ui/theme/` contains the Compose theme.

Repositories map API DTOs to domain models at the data boundary. Screens observe ViewModel state through `StateFlow`. Room is used for catalog caching; authentication credentials are managed separately.

## Project documentation

- [CONTEXT.md](CONTEXT.md) — detailed project context, conventions, and architecture notes.
- [docs/API_CONTRACT.md](docs/API_CONTRACT.md) — patient mobile API contract.
- [docs/BACKEND_CONTEXT.md](docs/BACKEND_CONTEXT.md) — backend behavior and integration context.
- [docs/specs/](docs/specs/) — feature specifications and implementation plans.
