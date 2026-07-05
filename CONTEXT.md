# Eyecare Android App — Project Context

## What This Is

Customer-facing Android app for Padilla Optical Clinic (POCMS). Consumes a Laravel 13 REST API. Lets patients browse frames, book appointments, place orders, view prescriptions/billings, and chat with clinic staff.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.0 (AGP 9.2.1 built-in — no `kotlin.android` plugin) |
| UI | Jetpack Compose + Material 3 (BOM 2026.05.01) |
| DI | Hilt 2.59.2 |
| Network | Retrofit 2.11 + OkHttp 4.12 + Kotlinx Serialization 1.8.1 |
| Local DB | Room 2.7.1 (products cache only) |
| Navigation | Navigation Compose 2.9.0 (type-safe routes via `@Serializable`) |
| Images | Coil 3.1.0 |
| Camera/AR | CameraX 1.5.0 + MediaPipe 0.10.35 |
| Tests | JUnit 5 + MockK + Turbine + coroutines-test |

## Commands

```
./gradlew assembleDebug          # Build
./gradlew testDebugUnitTest      # Unit tests
./gradlew lintDebug              # Lint
./gradlew ktlintFormat           # Format
./gradlew ktlintCheck            # Format check
```

## Architecture

MVVM + Clean Architecture: `data/` → `domain/` → `presentation/`

```
com.eyecare.app/
├── data/
│   ├── remote/
│   │   ├── api/           # Retrofit service interfaces
│   │   ├── dto/           # Serializable DTOs (map to domain at repo boundary)
│   │   └── interceptor/   # Auth interceptor + 401 event bus
│   ├── local/
│   │   ├── dao/           # Room DAOs (ProductDao only)
│   │   ├── entity/        # Room entities
│   │   ├── EyecareDatabase.kt
│   │   └── TokenManager.kt
│   └── repository/        # Repository implementations
├── domain/
│   ├── model/             # Domain data classes + enums
│   └── repository/        # Repository interfaces
├── presentation/
│   ├── auth/              # Login, Register
│   ├── home/              # Dashboard
│   ├── catalog/           # Product list + detail
│   ├── ar/                # AR try-on (CameraX + MediaPipe)
│   ├── appointments/      # List, detail, booking wizard
│   ├── orders/            # List, detail, order request
│   ├── prescriptions/     # List + detail
│   ├── billing/           # Billing detail
│   ├── messaging/         # Chat screen
│   ├── feedback/          # Submit + history
│   ├── profile/           # User profile
│   ├── navigation/        # NavGraph, Routes, BottomNavBar
│   └── common/            # Shared components + helpers
├── di/                    # Hilt modules (Network, Auth, Feature modules)
└── ui/theme/              # Color, Type, Shape, Theme
```

## Key Conventions

- **DTOs:** Live in `data/remote/dto/`, use `@Serializable` + `@SerialName`. Never leak into domain/presentation.
- **Domain models:** Plain Kotlin data classes. No serialization annotations.
- **Mapping:** Always at repository boundary (`dto.toDomain()` extension functions).
- **ViewModels:** `@HiltViewModel` + `@Inject constructor`. State as `sealed interface` via `StateFlow`.
- **Assisted inject:** Used for ViewModels needing runtime params (`@AssistedFactory`).
- **Error handling:** Repositories return `Result<T>`. ViewModels fold into UI state.
- **HTTP errors:** Catch `HttpException`, parse error body for 422/429. 401 triggers `AuthEventBus.Logout`.
- **Images:** `buildImageUrl(path)` prepends storage base URL. Prefer variant images → fallback to product images.
- **Pagination:** `PaginationMeta` (currentPage, lastPage). ViewModel tracks `hasMorePages` + `loadMore()`.
- **Navigation:** Type-safe routes via `@Serializable` objects/data classes. Auth/Main graph split.
- **Bottom-nav tab switches:** use `popUpTo<MainGraph> { saveState = true; inclusive = false }` + `launchSingleTop = true` + `restoreState = true` so switching tabs doesn't grow the back-stack or lose scroll position.
- **Wizard flows (e.g. booking):** on terminal success, pop the wizard's own route off the stack with `popUpTo(WizardRoute) { inclusive = true }` before navigating to the result screen.

## Booking Wizard — Date & Time Selection

`presentation/appointments/booking/BookAppointmentScreen.kt`, Step2/Step3:

- **Step2 (date):** Material3 `DatePicker` with a `SelectableDates` override that blocks past dates and Sundays (clinic closed). Selected date parsed/formatted via UTC epoch millis.
- **Step3 (time):** digital HH:MM picker — up/down arrows per segment, AM/PM tap-toggle on the right. No analog clock, no seconds.
  - Clinic hours: 9:00 AM – 6:30 PM, enforced by `isValidClinicTime(hour12, minute, isPm)`.
  - Hour steps wrap correctly across the AM/PM boundary (`11 AM → 12 PM`, `12 PM → 1 PM → … → 6 PM → 12 PM`); the PM cycle must include the `12 -> 1` case or the hour overflows to 13 and silently invalidates every minute value.
  - Minute steps are in **5-minute increments** (0, 5, 10, …, 55), wrapping and guarded by `isValidClinicTime` so the picker can't be pushed past 6:30 PM.

## Product Catalog — Filters

`presentation/catalog/ProductListScreen.kt`, `FilterRow`:

- All filter/sort controls (All, categories, Brand dropdown, Sort dropdown, Clear) live in a **single horizontally-scrollable `LazyRow`**, not a stacked multi-row layout.
- Brand and Sort are `FilterChip`s that open a `DropdownMenu` on tap.
- "Clear" only renders when a filter or non-default sort is active.

## Backend API (base: `/api`)

Key endpoints the app consumes:
```
POST   /login, /register, /logout
GET    /user                          → {data: {id, name, email, phone, role}}
PATCH  /user                          → update profile
GET    /appointments, /appointments/{id}
POST   /appointments                  → book (pending)
POST   /appointments/{id}/cancel
GET    /visit-reasons                 → [{id, name, duration_minutes}]
GET    /products, /products/{id}      → frame-only, paginated
GET    /orders, /orders/{id}          → paginated, includes billing_id
POST   /orders                        → submit (requested)
POST   /orders/{id}/cancel
GET    /billing/{id}                  → with items[] + payments[]
GET    /prescriptions, /prescriptions/{id}
GET    /conversations                 → includes unread_count
GET    /conversations/{id}/messages
POST   /conversations/{id}/messages
POST   /conversations/{id}/messages/read
POST   /feedback
GET    /feedback, /feedback/{id}
```

Auth: Sanctum token in `Authorization: Bearer {token}`. Stored via `TokenManager` (SharedPreferences). 401 → auto-logout via `AuthEventBus`.

## Branding

| Element | Value |
|---|---|
| Primary color | `#29B6F6` (logo cyan) |
| Text / on-surface | `#3D3535` (logo charcoal) |
| Background | `#F8F9FA` (warm off-white) |
| App name | Eyecare |
| Font | Instrument Sans (Google Fonts, downloaded at runtime) |

Color tokens live in `ui/theme/Color.kt` and are wired into `MaterialTheme.colorScheme` via `ui/theme/Theme.kt` (light scheme only — no dark theme defined yet). Cards use pure white (`CardSurface`) with a subtle 8%-black border (`CardBorder`, mapped to `outlineVariant`) so they float above the warm background.

## Active Specs

- `docs/specs/backend-alignment-v2-spec.md` — Current: fixing remaining API misalignments (11 tasks)
- `docs/specs/implementation-plan-v2.md` — Task breakdown for alignment v2
- `docs/BACKEND_CONTEXT.md` — Full backend documentation (source of truth for API shapes)

## Boundaries

- **Never** use Gson — only Kotlinx Serialization
- **Never** store tokens or health data in Room (only product cache)
- **Never** apply `org.jetbrains.kotlin.android` plugin (AGP 9 built-in)
- **Never** add `android.disallowKotlinSourceSets=false`
- **Always** run `./gradlew assembleDebug` after changes
- **Always** map DTOs to domain models at repository boundary
- **Always** use `sealed interface` for UI state
- **Ask first** before adding new dependencies
- **Ask first** before changing navigation graph structure
