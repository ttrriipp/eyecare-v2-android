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
- **Backend alias fields:** when the backend returns two field names for the same value (e.g. `lens_category_id` canonical + `lens_type_id` backward-compat alias), the DTO parses both but the domain model exposes only the canonical name. Repository mapping prefers canonical, falls back to alias: `lensCategoryId = lensCategoryId ?: lensTypeId`. Request bodies (e.g. `OrderItemRequest`) may keep using the alias name if the backend documents it as accepted — no need to migrate outbound fields the backend still supports.

## Booking Wizard — Date & Time Selection

`presentation/appointments/booking/BookAppointmentScreen.kt`, Step2/Step3:

- **Step2 (date):** Material3 `DatePicker` with a `SelectableDates` override that blocks past dates and Sundays (clinic closed). Selected date parsed/formatted via UTC epoch millis.
- **Step3 (time):** retains the themed digital HH:MM picker with arrow controls and AM/PM toggle, while `GET /appointments/availability` supplies the valid starts for the selected date and visit reason. The clock initializes to the first available start; Review is disabled when the displayed time is unavailable.
  - Selecting an available clock time submits the matching backend `starts_at` value unchanged. Loading, retry, closed-day, fully-booked, and unavailable-time states are handled inline. API response timestamps, including UTC (`Z`) values, are converted to `Asia/Manila` before display and date grouping.
  - Booking mutations remain authoritative. A 422 with `code = SLOT_UNAVAILABLE` returns the wizard to Step3, refreshes availability, and asks the customer to choose another time.
- **Step1 (visit reason):** compact navigation rows keep the reason name primary, show duration as a trailing clock value, and use a chevron to communicate immediate progression. The four-step indicator uses a primary active fill with a subdued surface track.
- Android system Back and the app-bar Back button share the same wizard behavior: Steps 2–4 return to the previous step, while Step 1 exits to the appointment list.
- All four steps use the same 16dp top and horizontal content grid. Past dates, Sundays, and dates without enough remaining clinic hours for the selected visit duration cannot advance. Backend capacity is loaded after date selection. Review shows clinic-local Date and Time as separate icon rows and uses a compact optional notes field.

## Appointment Reschedule — Bottom Sheet

`presentation/appointments/RescheduleBottomSheet.kt`, invoked from `AppointmentDetailScreen.kt`:

- Rescheduling an **existing** appointment calls `POST /appointments/{id}/reschedule` — it does NOT create a new appointment. Never route the "Reschedule" action through the booking wizard (`BookAppointmentScreen`).
- UI is a `ModalBottomSheet` with a `SecondaryTabRow` (Date / Time tabs) rather than a multi-step wizard — reschedule only needs date + time, no visit reason or notes re-entry. It skips the partially expanded anchor so the calendar and confirmation controls open at a usable height on compact screens. Date and time controls update the draft selection directly, with one contextual bottom action: **Continue to time** on Date and **Review reschedule** on Time.
- The draft starts at the appointment's current clinic-local date/time. Same-day past times advance to the next 15-minute slot; past and unchanged selections are blocked locally with concise guidance before any API request.
- Reimplements the same `SelectableDates` (no past dates, no Sundays) and digital HH:MM clinic-hours picker (`isValidClinicTime`, 9:00 AM–5:00 PM, 15-minute steps) as the booking wizard, but as private composables local to this file — intentionally not shared/extracted, since `BookAppointmentScreen`'s versions are `private`.
- Reschedule and Cancel are available for `PENDING` and `CONFIRMED` statuses. Both the appointment list and detail screen use this same status set.
- The list screen's "Reschedule" button navigates to `AppointmentDetailScreen` (not the booking wizard) — actual reschedule happens via the sheet on the detail screen.
- **Confirm-before-submit:** tapping "Confirm Reschedule" in the sheet does not submit immediately — it opens an `AlertDialog` ("Reschedule this appointment to [date] at [time]?") with **Reschedule** / **Keep Current Time** actions. Only the "Reschedule" action calls `onConfirm(scheduledAt)`. Uses local `formatPickedDate`/`formatPickedTime` helpers (operate on the raw picker strings, not on a combined `scheduled_at` string — separate from `formatAppointmentDate`/`formatAppointmentTime` in `AppointmentListScreen.kt`).
- **On success:** `AppointmentDetailViewModel.rescheduleAppointment` uses the `Appointment` returned directly by the `POST /appointments/{id}/reschedule` response — it does **not** call `load()` to re-fetch. This avoids an extra network round trip and any risk of transiently showing stale data from a second GET. `showRescheduleSheet` is set to `false` and `showRescheduleSuccessDialog` to `true` in the same state update, which dismisses the bottom sheet and immediately shows a confirmation `AlertDialog` ("Appointment Rescheduled — Your appointment is now set for [date] at [time]"), dismissed via `dismissRescheduleSuccessDialog()`.
- The detail screen's "Reschedule" button uses the same filled, theme-tinted `Button` style (primary color at 12% alpha background, primary content color, no elevation) as the list screen's card action buttons, rather than the plain `OutlinedButton` used elsewhere.
- **Scheduling timezone:** picker values represent Philippine clinic-local time. Booking and rescheduling submit an ISO-8601 timestamp with the explicit `Asia/Manila` offset; response timestamps are converted by instant to `Asia/Manila` for display, filtering, and date grouping.
- **Availability limitation:** rescheduling still uses the locally constrained picker and authoritative backend validation. The availability endpoint requires `visit_reason_id`, while appointment responses currently expose only the display label `visit_reason`; Android will not infer a stable ID by matching display text. Add `visit_reason_id` to appointment list/detail responses before wiring reschedule availability.
- **Staff reschedule reason:** appointment responses parse the nullable `last_reschedule_reason` into the domain model. A non-blank value appears on appointment detail in a distinct **Schedule changed by clinic** notice rather than being merged with customer or clinic notes. Customer rescheduling uses the returned appointment directly, so the notice disappears immediately when the backend clears the reason.
- **Customer note editing:** pending and confirmed appointments show the edit action directly beside **Your booking note**. There is no generic Notes heading; the customer and clinic labels provide the complete hierarchy, with the clinic note remaining clearly read-only. The inline editor accepts up to 1000 characters, sends trimmed text through `PATCH /appointments/{id}/contact-note`, and sends `null` when cleared. The returned appointment updates the screen directly; all later statuses hide the edit action.

## Themed Confirmation Dialogs

`presentation/common/components/AppConfirmationDialog.kt`:

- Shared composable used in place of the stock Material3 `AlertDialog` wherever the app needs a confirmation or acknowledgement prompt (reschedule confirm, reschedule success, cancel-appointment confirm in `AppointmentDetailScreen.kt`, reschedule confirm in `RescheduleBottomSheet.kt`). The stock `AlertDialog` doesn't match the app's visual language (pill buttons, tinted icon badges, `CardBorder`-outlined surfaces), so this wraps a raw `Dialog` with a rounded `Surface` (24dp), a circular icon badge tinted at 12% alpha, and pill-shaped (`RoundedCornerShape(50)`) action buttons.
- Takes `confirmLabel` only for a single acknowledgement, or both `confirmLabel` + `dismissLabel` for a yes/no confirmation. `isDestructive = true` swaps the confirm button to error-colored (used for the Cancel Appointment flow).
- **Button sizing:** the action-button `Row` uses `height(IntrinsicSize.Min)` with both buttons set to `fillMaxHeight()`, rather than a fixed height — this lets both buttons grow together if a longer label (e.g. "Keep Current Time") needs to wrap to two lines, instead of the text clipping/overflowing. Button text uses `labelMedium` (not the default larger button text style) with tighter `contentPadding` to give long labels more room to fit on one line where possible.

## Bottom Navigation Shape

`presentation/navigation/SplitBottomNavBar.kt`:

- The nav-tab container and the chat FAB share the same `RoundedCornerShape(16.dp)` squircle shape (previously the nav-tab container was a full pill at 40dp) so the two bottom-bar elements read as one consistent shape language rather than two different corner treatments sitting side by side.
- The selected-tab inner highlight is `RoundedCornerShape(12.dp)` (reduced from 32dp) to stay proportional inside the smaller-radius outer container.

## Product Catalog — Filters

`presentation/catalog/ProductListScreen.kt`, `ProductListViewModel.kt`, and
`presentation/catalog/components/CatalogFilterSheet.kt`:

- The catalog is split by a `SingleChoiceSegmentedButtonRow` into **Frames** (case-insensitive `product_type == "frame"`) and **Eye Products** (all other product types returned by the catalog endpoint). Frames is the default tab.
- Search remains server-backed and debounced by 300ms. Its placeholder and the empty-state copy follow the active catalog tab.
- The old horizontally scrolling category/brand chip row was replaced by a compact toolbar: **Filters** opens a `ModalBottomSheet`, while **Sort** remains a separate dropdown chip. The Filters label shows the number of active category/brand selections.
- The filter sheet follows a two-pane mobile layout. The left rail switches between **Category** and **Brand**; the right pane shows the corresponding backend-provided options in a two-column grid. Selected options use both a tinted surface and a check icon.
- Category and brand choices are draft state while the sheet is open. **Reset** clears both draft selections; **Apply** calls `ProductListViewModel.applyCatalogFilters(brandId, categoryId)` so both query parameters change together and trigger one page-1 reload. Dismissing the sheet does not apply draft changes.
- Category options remain tab-specific. Frame filters recognize category names containing `frame`, `eyeglass`, or `sunglass`; Eye Product filters recognize contact-lens or accessory category names. Category identity still comes from the backend ID—the name matching only decides which options are relevant to each tab.
- Product response `category` is nullable. The network DTO accepts `null`, and repository mapping normalizes it to an empty domain/cache value so uncategorized products remain browseable without changing the Room schema.
- Switching catalog tabs preserves search, brand, and sort. An active category is cleared and products are reloaded because category choices are tab-specific.
- Brands and categories are retained in dedicated ViewModel fields so filter metadata is not lost when those requests finish before the initial product request.
- Tests in `ProductListViewModelTest` cover default tab grouping, non-frame Eye Products, tab-specific categories, filter-metadata load ordering, and atomic category+brand application.

**Known pagination constraint:** the top-level Frames/Eye Products split is currently applied in memory after each mixed, paginated product response. If the loaded page contains no products for the selected tab, later matching pages may not be reachable from the current empty state. Prefer a backend `product_type` filter or deliberate page traversal before expanding this behavior.

## Order Requests — Product-Type Rules

`presentation/orders/OrderRequestScreen.kt` and `OrderRequestViewModel.kt`:

- Only `frame` products expose the **No lens cutting required** choice and optional **Lens Category** selector.
- `contact_lens`, `accessory`, and unknown non-frame product types are treated conservatively as directly orderable non-prescription products. They always submit `is_non_prescription = true` with no lens-category alias.
- Turning on **No lens cutting required** for a frame clears any prior lens-category selection so a hidden stale value cannot be submitted.
- The outbound request continues to use the backend-supported `lens_type_id` compatibility alias. The fixed local category set remains until the backend documents a customer-facing lens-categories endpoint.

## Home Dashboard — Clinic Products

`presentation/home/HomeScreen.kt` and `HomeViewModel.kt`:

- **From the clinic** and its supporting copy live inside one outlined card. That container holds equal-size horizontal product cards grouped into **Featured frames**, **Accessories**, and **Eye-care essentials** shelves.
- Featured frames use `product_type == "frame"`. Other retail product types—including `general`, `accessory`, `contact_lens`, and `lens`—are eligible for the non-frame shelves; `service` products are intentionally excluded.
- Accessory grouping normalizes case, underscores, hyphens, and repeated whitespace, then recognizes accessory, cleaning-kit, case, and cases wording. Remaining non-frame retail products become Eye-care essentials.
- Each Home shelf preserves source order and is capped at four products. The Home request currently reads only the first product page, so products outside that page are not candidates for a shelf.
- Home grouping behavior is covered by `HomeViewModelTest`, including alternate non-frame product-type values and category-name normalization.

## Profile — Patient Account Hub

`presentation/profile/ProfileScreen.kt`, `EditProfileScreen.kt`, and `ProfileViewModel.kt`:

- The main Profile screen uses an identity-first hierarchy: a warm outlined patient card with a name-derived initials avatar, name, email, optional phone, and a labelled **Edit profile** action. The avatar is display-only — there is no photo upload, camera/gallery picker, image permission, or storage flow.
- **Care & activity** keeps the existing Messages, Order History, Prescriptions, and Feedback History destinations. Each row uses a restrained primary-tinted icon treatment and supporting copy; Messages retains the unread badge, caps its visible text at `9+`, and exposes the full count through accessibility semantics.
- **Log out** is a full-width filled error-colored button. It opens the shared themed `AppConfirmationDialog` with **Log out** / **Stay signed in** actions before invoking the existing logout behavior.
- Initial profile loading uses an accessible content-shaped placeholder; load errors retain the existing retry action. Profile data still refreshes on lifecycle resume so edits are reflected after returning from Edit Profile.
- Edit Profile remains limited to the current Android contract: name, email, and nullable phone. Save submits directly without a redundant confirmation dialog. Fields and navigation actions are disabled while saving, validation errors remain attached to their fields, and non-validation failures preserve the draft and show concise inline feedback.
- App-bar Back, system Back, and **Cancel** share dirty-state handling. Unchanged values exit immediately; changed values open the themed **Discard changes?** confirmation. Blank and whitespace-only phone drafts are normalized to the existing nullable-phone behavior when checking for changes.
- The refresh is presentation-only: no backend endpoint, DTO, domain `User`, repository signature, navigation route, dependency, or global theme token changed. Backend-supported address editing remains out of scope because the current Android user/update contract does not expose it.
- Stateless `ProfileContent`, `ProfileLoadingContent`, and `EditProfileContent` composables provide deterministic Compose UI-test seams. `ProfileViewModelTest` covers dirty comparison, initials fallback, and save-failure draft preservation; `ProfileScreenTest` covers the visible hierarchy, callbacks, optional phone, unread semantics, loading semantics, supported edit fields, direct save, and saving/error states.

## Backend API (base: `/api`)

Key endpoints the app consumes:
```
POST   /login, /register, /logout
GET    /user                          → {data: {id, name, email, phone, role}}
PATCH  /user                          → update profile
GET    /appointments, /appointments/{id}
GET    /appointments/availability     → slot grid for date + visit_reason_id; optional appointment_id for reschedule
POST   /appointments                  → book (pending)
POST   /appointments/{id}/cancel
POST   /appointments/{id}/reschedule  → reschedule own appointment (pending/confirmed only)
PATCH  /appointments/{id}/contact-note → edit or clear own pending/confirmed appointment contact note
GET    /visit-reasons                 → [{id, name, duration_minutes}]
GET    /products, /products/{id}      → active frame/contact_lens/accessory catalog products, paginated (supports search/brand/category/min_price/max_price/in_stock/sort)
GET    /brands, /categories           → filter metadata used by the Catalog filter sheet
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

- `docs/specs/backend-alignment-v2-spec.md` — Complete: fixed initial API misalignments (11 tasks)
- `docs/specs/backend-alignment-v3-spec.md` — Complete: reschedule endpoint, or_number removal, lens_category_id fields, price filter params (6 tasks)
- `docs/specs/backend-alignment-v5-spec.md` — In progress: booking availability complete; reschedule availability awaits `visit_reason_id` in appointment responses
- `docs/specs/backend-alignment-v6-spec.md` — Complete: product taxonomy, order invariants, and customer-visible staff reschedule reasons
- `docs/specs/implementation-plan-v2.md` — Task breakdown for alignment v2
- `docs/specs/profile-ui-refresh-spec.md` — Complete: approved UI-only Profile and Edit Profile refresh
- `docs/specs/profile-ui-refresh-plan.md` — Complete: TDD task breakdown and verification plan for the profile refresh
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
