# Implementation Plan: EyecareV2 Android App

## Overview

Build the customer-facing Android app for the Optical Clinic Journey capstone. The project is currently a blank Android Studio template (Hello World). This plan takes it from zero to a fully functional app covering: auth, appointments, product catalog with offline cache, AR frame try-on, order requests, billing, prescriptions, messaging, and feedback — all targeting a 10-minute defense demo.

## Architecture Decisions

- **MVVM + Clean Architecture** — `data/` → `domain/` → `presentation/` layers. DTOs map to domain models at the repository boundary.
- **Hilt** for DI — single Application-scoped component, feature modules use `@HiltViewModel`.
- **Retrofit + OkHttp + Kotlinx Serialization** — no Gson. Auth interceptor attaches Bearer token. 401 triggers global logout.
- **Room** for product catalog offline cache only. Sensitive data (prescriptions, tokens) never persisted in Room.
- **Navigation Compose** with type-safe routes. Two nested graphs: `AuthGraph` and `MainGraph`.
- **StateFlow** for UI state, `sealed interface` for state modeling. No LiveData.
- **Fixed clinical blue palette** — no dynamic color. Outfit + DM Sans typography.
- **MediaPipe Face Landmarker** over ARCore — works on any front camera, lighter weight, sufficient for 2D frame overlay demo.
- **Split floating bottom nav** — 4-tab white pill (Home, Catalog, Visits, Profile) + detached blue chat FAB.

## Dependency Graph

```
Version Catalog + Gradle Config
        │
        ├── Hilt DI Setup
        │       │
        │       ├── Retrofit + OkHttp + Kotlinx Serialization
        │       │       │
        │       │       ├── Token Storage (EncryptedSharedPreferences)
        │       │       │       │
        │       │       │       └── Auth Interceptor
        │       │       │               │
        │       │       │               └── All API Services
        │       │       │
        │       │       └── Room Database (products cache)
        │       │
        │       └── Navigation Compose + Auth Gate
        │               │
        │               ├── Auth Screens (Login/Register)
        │               │       │
        │               │       └── All Authenticated Features
        │               │
        │               └── Split Bottom Nav Shell
        │                       │
        │                       ├── Home Screen
        │                       ├── Appointments (list/detail/booking)
        │                       ├── Product Catalog (list/detail)
        │                       │       │
        │                       │       └── AR Try-On (CameraX + MediaPipe)
        │                       │
        │                       ├── Orders (request/list/detail)
        │                       │       │
        │                       │       └── Billing Detail
        │                       │
        │                       ├── Prescriptions
        │                       ├── Messaging/Chat
        │                       └── Feedback
        │
        └── Theme / Design System (parallel with DI)
```

## Task List

---

### Phase 1: Foundation

#### Task 1: Gradle Scaffolding & Version Catalog

**Description:** Configure Gradle KTS with all dependencies in version catalog, set correct package name, min SDK, build config fields, and ktlint.

**Acceptance criteria:**
- [x] Version catalog declares: Hilt, Retrofit, OkHttp, Kotlinx Serialization, Room, Coil, Navigation Compose, CameraX, MediaPipe, Security-Crypto, JUnit 5, MockK, Turbine
- [x] `app/build.gradle.kts` applies Hilt, KSP, Kotlinx Serialization, Room plugins
- [x] `namespace` and `applicationId` set to `com.eyecare.app`
- [x] `minSdk = 26`, `targetSdk = 35`
- [x] `buildConfigField` for `API_BASE_URL` with debug/release variants
- [x] ktlint plugin configured
- [x] `./gradlew assembleDebug` succeeds

**Verification:**
- [x] `./gradlew assembleDebug` completes without errors
- [x] `./gradlew ktlintCheck` passes

**Files likely touched:**
- `gradle/libs.versions.toml`
- `build.gradle.kts` (project-level)
- `app/build.gradle.kts`
- `settings.gradle.kts`

**Estimated scope:** M (4 files, complex configuration)

---

#### Task 2: Hilt DI & Application Class

**Description:** Set up Hilt with `@HiltAndroidApp` Application class, annotate MainActivity, create NetworkModule providing OkHttp, Retrofit, and Kotlinx Serialization converter.

**Acceptance criteria:**
- [x] `EyecareApp` annotated with `@HiltAndroidApp`
- [x] `MainActivity` annotated with `@AndroidEntryPoint`
- [x] `NetworkModule` provides: `OkHttpClient`, `Retrofit`, `Json` (kotlinx)
- [x] Retrofit base URL reads from `BuildConfig.API_BASE_URL`
- [x] App launches without crash

**Verification:**
- [x] App compiles and runs on emulator (still shows placeholder UI)
- [x] Hilt component generation succeeds (no missing bindings)

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/EyecareApp.kt`
- `app/src/main/java/com/eyecare/app/di/NetworkModule.kt`
- `app/src/main/java/com/eyecare/app/MainActivity.kt`
- `AndroidManifest.xml`

**Estimated scope:** S (4 files, straightforward)

---

#### Task 3: Design System — Theme, Colors, Typography

**Description:** Replace default purple theme with the fixed clinical blue palette, configure Outfit + DM Sans fonts, define component shapes and color tokens.

**Acceptance criteria:**
- [x] `Color.kt` defines all spec tokens (primary `#4A90E2`, surface, status colors, etc.)
- [x] `Type.kt` defines typography using Outfit (headings) and DM Sans (body)
- [x] `Theme.kt` uses fixed `lightColorScheme` (no dynamic color), disables dark theme
- [x] Font files added to `res/font/` (via `ui-text-google-fonts` downloadable fonts provider)
- [ ] Preview composable confirms visual match to prototype palette

**Verification:**
- [ ] Compose preview shows clinical blue primary, white surface, correct typography
- [x] No purple remnants in theme

**Dependencies:** Task 1

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/common/theme/Color.kt`
- `app/src/main/java/com/eyecare/app/presentation/common/theme/Type.kt`
- `app/src/main/java/com/eyecare/app/presentation/common/theme/Theme.kt`
- `app/src/main/java/com/eyecare/app/presentation/common/theme/Shape.kt`
- `app/src/main/res/font/` (Outfit + DM Sans font files)

**Estimated scope:** M (5+ files with font assets)

---

#### Task 4: Token Storage & Auth Interceptor

**Description:** Implement secure token persistence in EncryptedSharedPreferences and an OkHttp interceptor that attaches the Bearer token and handles 401 globally.

**Acceptance criteria:**
- [x] `TokenManager` stores/retrieves/clears token from EncryptedSharedPreferences
- [x] `AuthInterceptor` reads token from `TokenManager` and adds `Authorization: Bearer {token}` header
- [x] `AuthInterceptor` detects 401 responses and signals logout (via shared event/flow)
- [x] `TokenManager` is provided via Hilt (`@Singleton`)
- [x] Unit test: store → retrieve → clear token
- [x] Unit test: interceptor adds header when token exists, skips when absent

**Verification:**
- [x] Unit tests pass
- [x] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 2

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/local/TokenManager.kt`
- `app/src/main/java/com/eyecare/app/data/remote/interceptor/AuthInterceptor.kt`
- `app/src/main/java/com/eyecare/app/di/NetworkModule.kt` (wire interceptor)
- `app/src/test/java/com/eyecare/app/data/local/TokenManagerTest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/interceptor/AuthInterceptorTest.kt`

**Estimated scope:** M (5 files)

---

#### Task 5: Auth API & Repository (Login/Register/Logout)

**Description:** Define auth API service interface, DTOs, domain model, and repository implementation handling login, register, logout, and error mapping.

**Acceptance criteria:**
- [x] `AuthApiService` interface with `login`, `register`, `logout`, `getUser` suspend functions
- [x] Request/Response DTOs with Kotlinx Serialization annotations
- [x] `User` domain model (id, name, email, role)
- [x] `AuthRepository` interface in domain layer
- [x] `AuthRepositoryImpl` maps DTOs → domain, handles 422 (validation) and 429 (rate limit) errors
- [x] Unit test: repository maps success response correctly
- [x] Unit test: repository maps 422 error with field messages

**Verification:**
- [x] Unit tests pass
- [x] No Gson usage anywhere

**Dependencies:** Task 4

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/AuthApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/AuthDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/User.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AuthRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AuthRepositoryImplTest.kt`

**Estimated scope:** M (6 files)

---

#### Task 6: Login & Register Screens

**Description:** Build login and register UI with form validation, loading/error states, and navigation between them.

**Acceptance criteria:**
- [x] Login: email + password fields, "Login" button, "Create account" link
- [x] Register: name + email + phone (optional) + password + confirm fields, "Register" button
- [x] Client-side validation before API call (email format, password min 8 chars, passwords match, required fields)
- [x] Shows per-field validation errors from 422 API response
- [x] Shows rate-limit message on 429 with retry countdown
- [x] Loading state disables form and shows indicator
- [x] Successful auth stores token and signals navigation to main graph
- [x] `AuthViewModel` with sealed `AuthUiState`

**Verification:**
- [x] Unit test: ViewModel emits correct states for success/error/validation/rate-limit
- [x] Compose preview shows both screens
- [x] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 5, Task 3

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/auth/LoginScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/RegisterScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/AuthViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/AuthViewModelTest.kt`

**Estimated scope:** M (4 files)

---

#### Task 7: Navigation Shell — Nav Graph, Auth Gate & Split Bottom Nav

**Description:** Implement Jetpack Navigation Compose with AuthGraph/MainGraph, auth gate that redirects based on token presence, and the custom split floating bottom nav bar matching the prototype.

**Acceptance criteria:**
- [x] `Routes` object defines type-safe routes for all screens
- [x] `AuthGraph` contains Login and Register routes
- [x] `MainGraph` contains Home, Catalog, Appointments (Visits), and Profile tab roots
- [x] Auth gate: no token → show AuthGraph; has token → show MainGraph
- [x] `SplitBottomNavBar` composable: white floating pill (4 tabs) + detached blue chat FAB
- [x] Active tab highlighted with primary color (matches prototype)
- [x] 401 event from interceptor clears token and navigates to AuthGraph
- [x] Back navigation within tabs preserves state

**Verification:**
- [x] App launches → login screen (no token). After login → bottom nav visible
- [x] Tapping tabs switches content
- [x] Chat FAB present and tappable
- [ ] Compose navigation test for auth gate logic

**Dependencies:** Task 6, Task 3

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/SplitBottomNavBar.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/AuthGate.kt`
- `app/src/main/java/com/eyecare/app/MainActivity.kt` (update to host NavGraph)

**Estimated scope:** M (5 files)

---

### Checkpoint: Phase 1 Complete

- [x] `./gradlew assembleDebug` succeeds
- [x] `./gradlew testDebugUnitTest` passes
- [x] `./gradlew ktlintCheck` passes
- [x] App launches → Login screen → Register → Login → see bottom nav with 4 tabs + chat FAB
- [x] Logout returns to login. Token persists across app restart.
- [x] Clinical blue theme visible throughout

---

### Phase 2: Appointments

#### Task 8: Appointments API Service & Repository

**Description:** Define appointment API service, DTOs, domain models, and repository with list/detail/create operations.

**Acceptance criteria:**
- [x] `AppointmentApiService` with `getAppointments()`, `getAppointment(id)`, `createAppointment(body)` endpoints
- [x] DTOs with Kotlinx Serialization annotations
- [x] `Appointment` domain model (id, visitReason, status, scheduledAt, contactNotes, staffNotes)
- [x] `AppointmentRepository` interface + `AppointmentRepositoryImpl`
- [x] Error mapping for 422 validation errors
- [x] Unit test for DTO → domain mapping

**Verification:**
- [x] Unit tests pass
- [x] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 4 (interceptor), Task 2 (DI)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/AppointmentApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Appointment.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AppointmentRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentRepositoryImplTest.kt`

**Estimated scope:** M (6 files)

---

#### Task 9: Appointment List & Detail Screens

**Description:** Build appointment list with status chips sorted by date, and detail screen showing full appointment info.

**Acceptance criteria:**
- [x] List shows appointments with: visit reason, date/time, status chip (colored per spec)
- [x] Sorted by most recent scheduled date
- [x] Pull-to-refresh reloads from API
- [x] Tap navigates to detail screen
- [x] Detail shows all fields: visit reason, status, scheduled date/time, contact notes, staff notes
- [x] "Leave Feedback" button visible when status = `completed` (navigates to feedback in later phase)
- [x] Empty state when no appointments exist
- [x] Loading and error states

**Verification:**
- [x] Unit test: ViewModel state transitions (Loading → Success, Loading → Error)
- [ ] Compose preview for list and detail
- [x] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 8, Task 7 (navigation)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/AppointmentDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/AppointmentListViewModelTest.kt`

**Estimated scope:** M (5 files)

---

#### Task 10: Book Appointment Wizard

**Description:** Multi-step booking: Step 1 (visit reason cards) → Step 2 (date chips + time slots per prototype) → Step 3 (notes + confirm). Step indicator at top.

**Acceptance criteria:**
- [x] Step 1: Large tappable reason cards (eye_exam, follow_up, prescription_check) with icons
- [x] Step 2: Horizontal date chips (next 7 days) + time slot grid (matching prototype layout)
- [x] Step 3: Optional contact notes (max 1000 chars) + summary review + "Confirm Booking" button
- [x] Step indicator (progress bar matching prototype — blue fill)
- [x] Back button returns to previous step, preserving selections
- [x] Submit calls API → on success navigates to appointment list
- [x] Shows API validation errors on failure
- [x] All state held in single `BookAppointmentViewModel`

**Verification:**
- [x] Unit test: ViewModel step transitions and final submission state
- [ ] Compose preview for each step
- [x] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 8, Task 7

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/steps/VisitReasonStep.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/steps/DateTimeStep.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/booking/steps/ConfirmStep.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/booking/BookAppointmentViewModelTest.kt`

**Estimated scope:** M (6 files)

---

### Checkpoint: Phase 2 Complete

- [ ] Can book an appointment → appears in list as "pending"
- [ ] Appointment detail shows full info with correct status chip color
- [ ] Wizard navigation works forward/backward without losing state
- [ ] All tests pass

---

### Phase 3: Product Catalog

#### Task 11: Room Database & Product Cache

**Description:** Set up Room database with product/variant/image entities, DAOs, and repository that fetches from network then caches locally for offline access.

**Acceptance criteria:**
- [x] `EyecareDatabase` with `ProductEntity`, `ProductVariantEntity`, `ProductImageEntity`
- [x] `ProductDao` with insertAll, getAll, getById, clearAll operations
- [x] Entity ↔ Domain model mappers
- [x] `ProductApiService` with `getProducts()`, `getProduct(id)`
- [x] `ProductRepositoryImpl` strategy: fetch API → cache in Room → serve from Room; if offline, serve stale cache
- [x] Unit test: repository serves cached data when network fails
- [ ] Room DAO test (instrumented or Robolectric)

**Verification:**
- [x] Unit tests pass
- [x] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 2 (Hilt/Retrofit)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/local/EyecareDatabase.kt`
- `app/src/main/java/com/eyecare/app/data/local/dao/ProductDao.kt`
- `app/src/main/java/com/eyecare/app/data/local/entity/ProductEntities.kt`
- `app/src/main/java/com/eyecare/app/data/remote/api/ProductApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/ProductDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Product.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/ProductRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/ProductRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/DatabaseModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ProductRepositoryImplTest.kt`

**Estimated scope:** M (10 files but most are small entity/mapper definitions)

---

#### Task 12: Product List Screen

**Description:** 2-column grid with product images, search bar, category filter chips, and AR badge — matching the prototype.

**Acceptance criteria:**
- [ ] Search bar with filter icon at top (matching prototype)
- [ ] Horizontal category chips: All, Frames, Sunglasses, Contacts, Accessories
- [ ] 2-column LazyVerticalGrid of product cards
- [ ] Each card: product image (Coil), brand label, product name, price in blue
- [ ] AR badge (icon + "AR" text) on cards with AR-eligible variants
- [ ] Pull-to-refresh
- [ ] Tap card → navigate to product detail
- [ ] Empty state and loading shimmer

**Verification:**
- [ ] Unit test: ViewModel filter logic (category selection filters products)
- [ ] Compose preview with sample data
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 11, Task 7 (navigation)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/catalog/ProductListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/catalog/ProductListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/catalog/components/ProductCard.kt`
- `app/src/main/java/com/eyecare/app/presentation/catalog/components/CategoryFilterChips.kt`
- `app/src/test/java/com/eyecare/app/presentation/catalog/ProductListViewModelTest.kt`

**Estimated scope:** M (5 files)

---

#### Task 13: Product Detail Screen

**Description:** Product detail with image carousel, variant selector, pricing, dimensions, and action buttons (Try AR / Order).

**Acceptance criteria:**
- [ ] Horizontal image pager with page indicator dots
- [ ] Product name, brand, description, dimensions displayed
- [ ] Variant selector (chips or dropdown): shows name, price difference, SKU
- [ ] Selected variant updates displayed price
- [ ] "Try with AR" button — visible only when selected variant has `ar_eligible = true`
- [ ] "Order this frame" button → navigates to order request (Phase 5) with variant pre-selected
- [ ] Back navigation returns to list with scroll position preserved

**Verification:**
- [ ] Unit test: variant selection updates state correctly
- [ ] Compose preview with multi-variant product
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 11, Task 12 (navigation from list)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/catalog/ProductDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/catalog/ProductDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/catalog/components/ImageCarousel.kt`
- `app/src/main/java/com/eyecare/app/presentation/catalog/components/VariantSelector.kt`
- `app/src/test/java/com/eyecare/app/presentation/catalog/ProductDetailViewModelTest.kt`

**Estimated scope:** M (5 files)

---

### Checkpoint: Phase 3 Complete

- [ ] Products load in 2-column grid with images via Coil
- [ ] Category filter works
- [ ] Detail shows variants; AR button visible only for eligible variants
- [ ] Offline: kill network → products still show from Room cache
- [ ] All tests pass

---

### Phase 4: AR Try-On

#### Task 14: CameraX Preview + Permission Handling

**Description:** Set up CameraX front-facing camera preview inside a Compose `AndroidView`, with runtime permission request and denial handling.

**Acceptance criteria:**
- [ ] Camera permission requested at runtime with rationale dialog
- [ ] Permission denied → show explanation with "Open Settings" button
- [ ] Front-facing CameraX preview renders in full-screen `AndroidView`
- [ ] Preview lifecycle-aware (stops when screen exits)
- [ ] No crash on permission denial or camera unavailable

**Verification:**
- [ ] Manual test: grant permission → camera shows. Deny → rationale shown.
- [ ] Unit test: ViewModel permission state logic
- [ ] `./gradlew assembleDebug` succeeds

**Dependencies:** Task 1 (CameraX dependency), Task 7 (navigation)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/CameraPreviewView.kt`
- `AndroidManifest.xml` (camera permission)

**Estimated scope:** M (4 files)

---

#### Task 15: MediaPipe Face Landmarker Integration

**Description:** Integrate MediaPipe Face Landmarker to detect face landmarks in real-time from the CameraX feed.

**Acceptance criteria:**
- [ ] `FaceLandmarkerHelper` wraps MediaPipe setup (model download/bundling, configuration)
- [ ] Processes camera frames and returns face landmarks at 30+ FPS
- [ ] Exposes key landmarks: nose bridge (6, 168), left temple (234), right temple (454)
- [ ] Computes face width (temple-to-temple) and rotation angle
- [ ] No face → emits "no face detected" state
- [ ] Properly releases resources on lifecycle destroy

**Verification:**
- [ ] Manual test on device: face detected, landmarks logged
- [ ] FPS counter shown in debug builds
- [ ] `./gradlew assembleDebug` succeeds

**Dependencies:** Task 14

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/ar/FaceLandmarkerHelper.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArViewModel.kt` (update)
- `app/src/main/java/com/eyecare/app/presentation/ar/model/FaceFrame.kt` (data class for computed values)

**Estimated scope:** M (3 files, complex ML integration)

---

#### Task 16: Frame Overlay Renderer & AR UI

**Description:** Render frame PNG overlay positioned on detected face landmarks. Implement the minimal AR UI: variant chip selector, close button, "Order this frame" FAB.

**Acceptance criteria:**
- [ ] Frame PNG loaded from `ar_asset_reference` URL via Coil (cached)
- [ ] Frame bitmap drawn as overlay: centered on nose bridge, scaled to temple-to-temple width, tilted with face rotation
- [ ] Variant selector: horizontal chip row at bottom (translucent background)
- [ ] Switching variant immediately updates overlay frame asset
- [ ] Close button (top-left) returns to product detail
- [ ] "Order this frame" FAB (bottom-right) → navigates to order request with selected variant
- [ ] No-face state: shows guide message ("Position your face in the center")
- [ ] Full-bleed camera — no nav bar, no status bar content (immersive)

**Verification:**
- [ ] Manual test: frame tracks face movement, scales/tilts correctly
- [ ] Variant switch changes overlay
- [ ] 30+ FPS maintained
- [ ] `./gradlew assembleDebug` succeeds

**Dependencies:** Task 15, Task 11 (product variant data)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/ar/FrameOverlayRenderer.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArTryOnScreen.kt` (update with full UI)
- `app/src/main/java/com/eyecare/app/presentation/ar/components/VariantChipRow.kt`
- `app/src/main/java/com/eyecare/app/presentation/ar/ArViewModel.kt` (update)

**Estimated scope:** M (4 files, complex rendering)

---

### Checkpoint: Phase 4 Complete

- [ ] AR screen opens from product detail "Try AR" button
- [ ] Camera shows, face detected, frame overlay renders on face
- [ ] Frame tracks head movement (position, scale, tilt)
- [ ] Variant switching works
- [ ] "Order this frame" navigates correctly
- [ ] 30+ FPS on target device
- [ ] **RISK CHECK:** If AR performance is unacceptable, implement fallback static frame preview

---

### Phase 5: Orders & Billing

#### Task 17: Order API Service & Repository

**Description:** Define order API service, DTOs, domain models, and repository with create/list/detail operations.

**Acceptance criteria:**
- [ ] `OrderApiService` with `getOrders()`, `getOrder(id)`, `createOrder(body)` endpoints
- [ ] DTOs include nested order items with product/variant/lens info
- [ ] `Order` and `OrderItem` domain models
- [ ] `OrderRepository` interface + `OrderRepositoryImpl`
- [ ] Error mapping for validation errors
- [ ] Unit test for DTO → domain mapping

**Verification:**
- [ ] Unit tests pass

**Dependencies:** Task 2 (DI/Retrofit)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/OrderApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/OrderDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Order.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/OrderRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/OrderRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/OrderRepositoryImplTest.kt`

**Estimated scope:** M (6 files)

---

#### Task 18: Order Request Screen

**Description:** Order submission form showing selected variant, lens type picker, quantity, optional appointment link, and non-prescription toggle.

**Acceptance criteria:**
- [ ] Receives selected product variant via navigation args (from product detail or AR)
- [ ] Displays frame info: product name, variant name, image, price
- [ ] Lens type selector: single_vision, bifocal, progressive (radio buttons or dropdown)
- [ ] Quantity selector (1-4)
- [ ] Optional "Link appointment" dropdown (shows user's appointments)
- [ ] Non-prescription toggle
- [ ] "Submit Order" button → calls API → navigates to order list on success
- [ ] Shows validation errors from API

**Verification:**
- [ ] Unit test: ViewModel validates required fields, handles success/error
- [ ] Compose preview
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 17, Task 8 (appointments for linking), Task 11 (product data)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/orders/OrderRequestScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/orders/OrderRequestViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/orders/OrderRequestViewModelTest.kt`

**Estimated scope:** M (3 files)

---

#### Task 19: Order List & Detail Screens

**Description:** Order list with status chips and detail screen showing items, totals, status timeline, and billing link.

**Acceptance criteria:**
- [ ] List: order number, status chip (colored), total amount, date
- [ ] Pull-to-refresh
- [ ] Detail: all order items (product name, variant, lens type, unit price, quantity, subtotal)
- [ ] Order totals: subtotal, total
- [ ] Status timeline/stepper (visual progression through order statuses)
- [ ] "View Billing" button when order status ≥ confirmed
- [ ] "Leave Feedback" button when status = completed
- [ ] Loading/error/empty states

**Verification:**
- [ ] Unit test: ViewModel state transitions
- [ ] Compose preview for list and detail
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 17, Task 7 (navigation)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/orders/OrderListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/orders/OrderListViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/orders/OrderDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/orders/OrderDetailViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/orders/components/StatusTimeline.kt`
- `app/src/test/java/com/eyecare/app/presentation/orders/OrderListViewModelTest.kt`

**Estimated scope:** M (6 files)

---

#### Task 20: Billing Detail Screen

**Description:** Read-only billing detail showing totals, balance, payment history.

**Acceptance criteria:**
- [ ] Shows total amount, amount paid, balance due
- [ ] Billing status chip
- [ ] Payment list: amount, method, reference number, date, status per payment
- [ ] Read-only — no actions
- [ ] Loading/error states

**Verification:**
- [ ] Unit test: ViewModel maps billing data correctly
- [ ] Compose preview
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 19 (navigated from order detail)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/BillingApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/BillingDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Billing.kt`
- `app/src/main/java/com/eyecare/app/presentation/billing/BillingDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/billing/BillingDetailViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/billing/BillingDetailViewModelTest.kt`

**Estimated scope:** S (6 files but all simple/read-only)

---

### Checkpoint: Phase 5 Complete

- [ ] Submit order from product detail → appears in order list as "requested"
- [ ] Submit order from AR → same result
- [ ] Order detail shows items and status timeline
- [ ] Billing detail accessible from confirmed+ orders
- [ ] All tests pass

---

### Phase 6: Prescriptions, Messaging & Feedback

#### Task 21: Prescription List & Detail

**Description:** Read-only prescription history with list and structured OD/OS/PD detail view.

**Acceptance criteria:**
- [ ] `PrescriptionApiService` + DTOs + domain model + repository
- [ ] List: prescriptions sorted by date, shows prescribed date and linked appointment
- [ ] Detail: structured layout showing OD sphere/cylinder/axis/add, OS same, PD value
- [ ] Expiration date highlighted in red if expired
- [ ] Loading/error/empty states

**Verification:**
- [ ] Unit test: ViewModel and repository mapping
- [ ] Compose preview
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 2, Task 7

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/PrescriptionApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/PrescriptionDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Prescription.kt`
- `app/src/main/java/com/eyecare/app/data/repository/PrescriptionRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionListScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionDetailScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/prescriptions/PrescriptionViewModelTest.kt`

**Estimated scope:** M (8 files but all straightforward read-only)

---

#### Task 22: Chat Screen — Single Conversation

**Description:** Build the persistent single-conversation chat UI with message bubbles, text input, and send functionality.

**Acceptance criteria:**
- [ ] `ConversationApiService` + `MessageApiService` with list/create endpoints
- [ ] On open: fetches user's first conversation (or creates one on first send)
- [ ] Messages as bubbles: own messages right-aligned (blue), staff left-aligned (grey)
- [ ] Text input bar at bottom with send button
- [ ] Read status indicator on messages
- [ ] Auto-scrolls to latest message
- [ ] Bottom nav hides when chat is open (full-screen experience per spec)
- [ ] Loading/error states

**Verification:**
- [ ] Unit test: ViewModel handles conversation creation, message sending, state updates
- [ ] Compose preview for bubble layout
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 2, Task 7

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/ConversationApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/MessageDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Message.kt`
- `app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageBubble.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`

**Estimated scope:** M (8 files)

---

#### Task 23: Chat Attachments & Context Linking

**Description:** Add file attachment (image/document upload) and appointment/order context card linking to messages.

**Acceptance criteria:**
- [ ] "+" button opens bottom sheet with: Attach file, Link appointment, Link order
- [ ] File picker: accepts images (jpg/png/gif) and documents (pdf/doc/docx), max 10MB
- [ ] Shows attachment preview before sending
- [ ] Sent attachments display inline: filename, size, icon by type
- [ ] "Link appointment" picker → compact context card in message (type, date, status)
- [ ] "Link order" picker → compact context card in message (order #, status)
- [ ] Multipart form upload for file messages

**Verification:**
- [ ] Unit test: attachment validation (file type, size)
- [ ] Compose preview for context cards
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 22

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt` (update)
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/AttachmentSheet.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/ContextCard.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/AttachmentPreview.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt` (update)

**Estimated scope:** M (5 files)

---

#### Task 24: Feedback Submission & History

**Description:** Star rating + comment submission triggered from completed appointments/orders, plus feedback history list showing staff replies.

**Acceptance criteria:**
- [ ] `FeedbackApiService` with `submitFeedback(body)` endpoint
- [ ] `FeedbackScreen`: star rating picker (1-5 tappable stars), comment field (max 2000 chars)
- [ ] Receives `appointment_id` or `order_id` via navigation args
- [ ] Submit → success toast → navigate back
- [ ] `FeedbackHistoryScreen`: list of past feedback with rating, comment, staff reply (if present)
- [ ] "Leave Feedback" buttons on appointment/order detail only when completed + no existing feedback

**Verification:**
- [ ] Unit test: ViewModel validates rating required, handles submit success/error
- [ ] Compose preview for star rating component
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 9 (appointment detail), Task 19 (order detail)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/data/remote/api/FeedbackApiService.kt`
- `app/src/main/java/com/eyecare/app/data/remote/dto/FeedbackDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Feedback.kt`
- `app/src/main/java/com/eyecare/app/data/repository/FeedbackRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/feedback/FeedbackHistoryScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/feedback/FeedbackViewModelTest.kt`

**Estimated scope:** M (8 files)

---

### Checkpoint: Phase 6 Complete

- [ ] Prescription list and detail load correctly
- [ ] Can send a message and see it in chat thread
- [ ] File attachment uploads successfully
- [ ] Feedback submission works from completed appointment detail
- [ ] Feedback history shows past submissions with staff replies
- [ ] All tests pass

---

### Phase 7: Home Screen & Polish

#### Task 25: Home Screen

**Description:** Build the rich dashboard matching the prototype: greeting, vision status card, prescription warning, next appointment card, active order tracker, new arrivals carousel.

**Acceptance criteria:**
- [ ] Greeting: "Good morning/afternoon/evening, {name}" + notification bell icon (top-right)
- [ ] "Your Vision Health" section header
- [ ] Vision Status card: "Optimal" + "20/20" circular badge (or derived from latest prescription)
- [ ] Prescription Expiring warning card (conditional — red border, shows days until expiry, "Book Exam" button)
- [ ] Next Appointment card: dark blue background, status chip, visit reason, date, doctor name, "View all" link
- [ ] Active Order tracker card: progress bar visualization of order status
- [ ] New Arrivals: horizontal scrolling product carousel (Coil images)
- [ ] All cards are tappable → navigate to relevant detail screens
- [ ] Pull-to-refresh reloads all data
- [ ] Loading/empty states per section

**Verification:**
- [ ] Unit test: HomeViewModel aggregates data from multiple repos
- [ ] Compose preview matches prototype layout
- [ ] `./gradlew testDebugUnitTest` passes

**Dependencies:** Task 8, Task 11, Task 17, Task 21 (reads from all repos)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/HomeViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/components/VisionStatusCard.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/components/PrescriptionWarningCard.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/components/NextAppointmentCard.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/components/OrderTrackerCard.kt`
- `app/src/main/java/com/eyecare/app/presentation/home/components/NewArrivalsCarousel.kt`
- `app/src/test/java/com/eyecare/app/presentation/home/HomeViewModelTest.kt`

**Estimated scope:** M (8 files)

---

#### Task 26: Profile Screen & More Tab

**Description:** Profile screen with user info and logout, plus the "More" tab aggregating navigation to orders, prescriptions, billing, feedback history.

**Acceptance criteria:**
- [ ] Profile screen: user avatar placeholder, name, email, role
- [ ] Logout button → clears token → navigates to login
- [ ] More/Profile tab serves as hub: links to Order History, Prescriptions, Feedback History
- [ ] Each link navigates to the already-built screens

**Verification:**
- [ ] Logout clears session correctly
- [ ] All navigation links work

**Dependencies:** Task 7 (nav), Task 5 (auth repo for user data)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileViewModel.kt`

**Estimated scope:** S (2 files)

---

#### Task 27: Error Handling, Loading & Empty States (Global)

**Description:** Add consistent shared composables for loading, error (with retry), and empty states. Audit all screens for missing states.

**Acceptance criteria:**
- [ ] `LoadingContent` composable: centered circular indicator
- [ ] `ErrorContent` composable: error message + "Retry" button
- [ ] `EmptyContent` composable: illustration placeholder + message + optional action button
- [ ] All ViewModels emit proper Loading/Error/Empty states
- [ ] Snackbar host for transient errors (network timeout, 500)
- [ ] 401 globally shows "Session expired" snackbar before redirecting to login
- [ ] Pull-to-refresh on all list screens

**Verification:**
- [ ] Manual walkthrough: airplane mode → error states shown with retry
- [ ] Each list screen shows empty state when no data

**Dependencies:** All feature screens (Task 9-26)

**Files likely touched:**
- `app/src/main/java/com/eyecare/app/presentation/common/components/LoadingContent.kt`
- `app/src/main/java/com/eyecare/app/presentation/common/components/ErrorContent.kt`
- `app/src/main/java/com/eyecare/app/presentation/common/components/EmptyContent.kt`
- Various screen files (minor updates to wire shared components)

**Estimated scope:** S (3 new files + minor updates)

---

#### Task 28: Demo Hardening & End-to-End Verification

**Description:** Run the full capstone defense demo script end-to-end against the seeded backend. Fix all issues found.

**Acceptance criteria:**
- [ ] Login with seeded customer account → Home screen loads with data
- [ ] AR try-on launches in < 3 seconds, frame tracks face
- [ ] Order submitted from AR → visible in order list immediately
- [ ] Appointment booked → appears in list as pending
- [ ] After admin confirms (separate panel), refresh shows updated status + billing
- [ ] Message sent to staff → appears in chat
- [ ] Feedback submitted for completed appointment
- [ ] Full demo path completes in < 10 minutes
- [ ] No crashes or ANRs during the demo path
- [ ] `./gradlew testDebugUnitTest` passes (all unit tests green)
- [ ] `./gradlew ktlintCheck` passes

**Verification:**
- [ ] Full demo rehearsal completed successfully
- [ ] Test suite green
- [ ] APK size acceptable (< 50MB with ML model)

**Dependencies:** All previous tasks

**Files likely touched:**
- Bug fixes as discovered during testing

**Estimated scope:** M (variable based on findings)

---

### Final Checkpoint

- [ ] `./gradlew assembleDebug` succeeds
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew ktlintCheck` passes
- [ ] Full defense demo script completes without crashes in < 10 minutes
- [ ] AR try-on is the opening "wow" moment and works reliably on demo device
- [ ] All screens have loading, error, and empty states
- [ ] Token persists across restarts, 401 redirects to login

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| MediaPipe FPS too low on demo device | High | Test on actual device in Phase 4 (fail fast). Fallback: static frame preview overlay without tracking |
| AR asset format mismatch with backend | Med | Define PNG spec early (transparent background, standard dimensions). Test with actual seeded assets in Phase 4 |
| CameraX + Compose interop complexity | Med | Use proven `AndroidView` wrapper pattern. Prototype camera-only first (Task 14) before adding ML |
| Network unreliable during defense demo | Med | Room cache for products. Pre-load catalog. Login once before demo starts with pre-seeded account |
| Rate limiting blocks demo login | Low | Use pre-authenticated seeded account. Login once before demo begins |
| Large APK from MediaPipe model | Low | Bundle only face_landmarker model (~4MB). Monitor total APK size |
| Package rename breaks IDE caches | Low | Do rename in Task 1 before any real code exists. Clean + rebuild |

## Parallelization Opportunities

If two agents/sessions are available:

- **After Phase 1:** Task 8 (Appointments API) and Task 11 (Product Room/API) can be built in parallel — they share no code
- **After Phase 3:** Task 14-16 (AR) and Task 17-20 (Orders/Billing) can be built in parallel — AR only needs product data which already exists
- **Phase 6:** Tasks 21, 22, and 24 are all independent features and can be parallelized
- **Must be sequential:** Tasks within each numbered sequence (e.g., 14→15→16 for AR)

## Open Questions — Resolved

1. **AR asset reference format:** Recommend treating as a relative storage path. The app constructs the full URL as `{BASE_URL}/storage/{ar_asset_reference}`. If the backend returns a full URL in the future, detect and use directly (check if starts with `http`).

2. **Products behind auth:** Yes — the backend has products under `auth:sanctum` (backend Task 10). The app must be logged in before catalog browsing. No guest/auth split needed. Room cache serves offline after first authenticated fetch.

3. **Seeded demo data (from backend Tasks 27–28):** The backend seeds:
   - Demo accounts: admin, staff, customer with documented credentials
   - Catalog: products with variants, brands, categories, lens types, AR asset references
   - Workflow records: appointment (pending + confirmed), order request, billing, messages, feedback
   - Both prescription and non-prescription order paths
   - Visit reasons: `eye_exam`, `follow_up`, `prescription_check`
   - All fixed statuses for appointments, orders, billings, payments, SMS, inventory
