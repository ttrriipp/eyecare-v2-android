# Eyecare Android App — Project Context

## What This Is

Customer-facing Android app for Padilla Optical Clinic (POCMS). Consumes a Laravel 13 REST API. Lets patients browse AR-ready frames and accessories, order accessories, book appointments, view prescriptions/billings, and chat with clinic staff.

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
│   ├── auth/              # V13: SessionGate, Welcome, SignIn, Registration, Recovery, SessionViewModel
│   ├── account/           # V13: LimitedAccount, AccountSecurity, contacts, step-up, invitations
│   ├── home/              # Dashboard
│   ├── catalog/           # Product list + detail
│   ├── ar/                # AR try-on (CameraX + MediaPipe)
│   ├── appointments/      # List, detail, booking wizard
│   ├── orders/            # List, detail, order request
│   ├── prescriptions/     # List + detail
│   ├── billing/           # Billing detail
│   ├── messaging/         # Chat screen
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
- **Limited account access:** A validated account with `UNLINKED`, `PENDING_REVIEW`, or `UNKNOWN`
  link status enters `MainGraph` with the normal shell. Home and account/security remain available;
  active-link clinical destinations are gated at navigation and open `LimitedAccount` for invitation
  entry or a clinic-review link request. The link hub remembers the protected destination that opened
  it and returns there after a successful invitation link. Profile keeps a persistent link entry point.
- **Bottom-nav tab switches:** use `popUpTo<MainGraph> { saveState = true; inclusive = false }` + `launchSingleTop = true` + `restoreState = true` so switching tabs doesn't grow the back-stack or lose scroll position.
- **Wizard flows (e.g. booking):** on terminal success, pop the wizard's own route off the stack with `popUpTo(WizardRoute) { inclusive = true }` before navigating to the result screen.
- **Backend alias fields:** when the backend returns two field names for the same value (e.g. `lens_category_id` canonical + `lens_type_id` backward-compat alias), the DTO parses both but the domain model exposes only the canonical name. Repository mapping prefers canonical, falls back to alias: `lensCategoryId = lensCategoryId ?: lensTypeId`. Request bodies (e.g. `OrderItemRequest`) may keep using the alias name if the backend documents it as accepted — no need to migrate outbound fields the backend still supports.

## Current Auth and Account-Linking Behavior (Android)

This is the implemented Android behavior for the current auth cutover. The backend remains
authoritative for account ownership, OTP challenges, link status, invitation validity, and clinical
access. Endpoint payloads and machine-readable errors belong in `docs/API_CONTRACT.md` and
`docs/BACKEND_CONTEXT.md`; this section records the client decisions and navigation boundary.

### Entry flows

- **Sign in:** phone number + password first. Trusted installations may receive a session directly;
  otherwise the app shows a six-digit login OTP before entering the session gate.
- **Create account:** phone number -> phone OTP -> profile/password/policy form -> account session.
  An optional invitation code may link the account during registration; without one, the new account
  is unlinked and still enters the normal app shell.
- **Reset password:** phone number -> OTP -> new password. It uses the same fixed Philippine phone
  prefix as the other auth forms.
- **Sign out:** clears the local token and returns to `Welcome`, not directly to `Login`.

### Session and linking boundary

- `GET /me` is resolved before routing. `LINKED` enters `MainGraph`; `UNLINKED`, `PENDING_REVIEW`,
  and unknown link states enter `MainGraph` with limited access; a missing/invalid session enters
  `Welcome`.
- Limited users can use account-safe areas, the limited Home shell, Profile, Account & Security, and
  the normal shell. Clinical data is not loaded or shown until the backend reports an active link.
- Limited users may browse the nonclinical frame catalog (including frame details/AR browse) and submit
  appointment requests. Confirmed appointments, prescriptions, reservations, eyewear, and messaging
  remain active-link-only.
- The appointment-request wizard adds a requester-identity step after the visit reason for limited
  accounts. It pre-fills the verified account phone, optional email, and structured account identity
  when available. The requester can edit email, first/middle/last name, date of birth, gender,
  occupation, and home address; phone remains read-only because it must match the account's verified
  contact. The form validates the contract-required fields and shows the complete identity in Review
  before sending the encrypted identity snapshot. Linked accounts skip this step, omit client
  identity, and rely on the authoritative clinic Patient record.
- The Appointments tab combines confirmed appointments with the account's appointment requests for
  linked and unlinked users. Pending requests appear in Upcoming; rejected, cancelled, expired, and
  unresolved accepted requests appear in History. Accepted requests already represented by a confirmed
  appointment are not duplicated. Request cards open account-owned details, where pending requests can
  be cancelled. The current contract has no request-edit endpoint, so changing a request requires
  cancelling it and submitting a new request.
- Any patient-only destination requested by a limited user opens `LimitedAccount` as a link hub. The
  hub offers **Enter invitation code** and **Ask clinic to link me**. After successful invitation
  acceptance, the app returns to the original feature; backing out clears that pending destination.
- Profile provides a persistent **Enter Invitation Code** entry point, so an unlinked user does not
  need to trigger a blocked patient feature to link their account.

### OTP, phone, and error presentation

- Phone fields display a non-editable `+63` prefix while users enter local digits. Values are
  normalized to canonical E.164 at the ViewModel/repository boundary; the UI never prepends `63`
  repeatedly while editing or deleting.
- OTP fields accept exactly six digits. The shared OTP row shows a readable local expiry time,
  countdown, expired state, and a 30-second resend cooldown. Resending replaces the active challenge
  and clears the previous code.
- Known auth/linking codes are converted to recovery-specific messages, including duplicate phone,
  invalid/expired invitation, invalid OTP, too many attempts, OTP rate limits, already-linked
  conflicts, and pending clinic-link requests. Unknown failures retain a safe server message or use
  a generic fallback.

### Implementation rationale and scope

- The flow follows a progressive-trust boundary: verify phone ownership, collect credentials/profile,
  then verify clinic-link authorization before exposing clinical data.
- This is an Android-side implementation aligned to the existing API contract. No backend endpoint,
  payload, rate-limit rule, or legacy compatibility layer was added for this flow.
- Auth/linking UI uses the existing Material 3 theme and primary blue. Home, bottom navigation, and
  appointment surfaces remain the visual authority while the broader app design is still being
  completed. See `PRODUCT.md` and `DESIGN.md` for the current product/design direction.

### Verification

- Focused auth/linking unit tests cover route intent restoration, linking errors, OTP expiry
  presentation, and password-recovery normalization.
- `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` pass for the current implementation.

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
- Reschedule is available for `scheduled` appointments only. Cancel is available for `scheduled` and `checked_in`. Both the appointment list and detail screen use the canonical capability matrix.
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

- The catalog is split by a `SingleChoiceSegmentedButtonRow` into **Frames** and **Accessories**. Frames is the default tab.
- A centralized domain policy treats only `frame` and `accessory` as mobile catalog types. Unknown values, `contact_lens`, `lens`, and legacy `general` fail closed. Frames are visible only when they retain at least one AR-ready variant (`ar_eligible == true` plus a non-blank asset reference).
- Search remains server-backed and debounced by 300ms. Its placeholder and the empty-state copy follow the active catalog tab.
- The old horizontally scrolling category/brand chip row was replaced by a compact toolbar: **Filters** opens a `ModalBottomSheet`, while **Sort** remains a separate dropdown chip. The Filters label shows the number of active category/brand selections.
- The filter sheet follows a two-pane mobile layout. The left rail switches between **Category** and **Brand**; the right pane shows the corresponding backend-provided options in a two-column grid. Selected options use both a tinted surface and a check icon.
- Category and brand choices are draft state while the sheet is open. **Reset** clears both draft selections; **Apply** calls `ProductListViewModel.applyCatalogFilters(brandId, categoryId)` so both query parameters change together and trigger one page-1 reload. Dismissing the sheet does not apply draft changes.
- Category options remain tab-specific. Frame filters recognize category names containing `frame`, `eyeglass`, or `sunglass`; accessory filters conservatively recognize accessory, cleaning, case, solution, and eye-drop wording. Category identity still comes from the backend ID—the name matching only decides which options are relevant to each tab.
- Product response `category` is nullable. The network DTO accepts `null`, and repository mapping normalizes it to an empty domain/cache value so uncategorized products remain browseable without changing the Room schema.
- Switching catalog tabs preserves search, brand, and sort. An active category is cleared and products are reloaded because category choices are tab-specific.
- Brands and categories are retained in dedicated ViewModel fields so filter metadata is not lost when those requests finish before the initial product request.
- Tests in `ProductListViewModelTest` cover default tab grouping, accessory-only grouping, tab-specific categories, filter-metadata load ordering, atomic category+brand application, and traversal across mixed pages.
- Because the backend does not expose a `product_type` query parameter, tabs still filter mixed pages in memory. When a fetched page adds no product for the selected tab, the ViewModel advances until it finds a match or exhausts pagination while preserving relative server order.

## Order Requests — Product-Type Rules

`presentation/orders/OrderRequestScreen.kt` and `OrderRequestViewModel.kt`:

- Only accessories can enter or submit the mobile order-request flow. Frame, contact-lens, optical-lens, legacy-general, and unknown deep links are rejected with a non-retryable customer-readable message.
- Frames are browse-only: their detail screen can show AR and explains that customers should contact the clinic to order. Neither product detail nor AR try-on exposes a frame-order action; the order action appears only for accessories.
- Product detail uses a compact **Photo coming soon** treatment when both variant and product images are absent. A persistent details card shows availability, selected option, SKU, and category; backend-provided descriptions and variant attributes appear as About and Specifications sections. In-stock accessories use a filled primary order action.
- A successfully loaded product with no active variants uses a product-aware **Temporarily unavailable** detail state. It keeps the product image, brand, name, and description visible, hides price/order actions, and offers **Check again**; transport and API failures continue to use the generic error state.
- The customer order repository always serializes `is_non_prescription = true`; callers cannot override it.
- Customer accessory orders are independent retail requests: the order screen does not load or offer appointments, and the create-order payload omits `appointment_id`. Order responses still decode nullable `appointment_id` for historical and staff-created records.
- Create-order items contain only `product_variant_id` and `quantity`. Historical order responses continue decoding both lens-category aliases, but neither alias exists in the outbound item DTO.

## Home Dashboard — Clinic Products

`presentation/home/HomeScreen.kt` and `HomeViewModel.kt`:

- **From the clinic** and its supporting copy live inside one outlined card. That container holds equal-size horizontal product cards grouped into **Featured frames**, **Accessories**, and **Eye-care essentials** shelves.
- Featured frames use the centralized frame policy. Only accessories are eligible for the non-frame shelves; other and unknown types are excluded even if stale cache data contains them.
- Accessory grouping normalizes case, underscores, hyphens, and repeated whitespace, then recognizes accessory, cleaning-kit, case, and cases wording. Remaining accessory products become Eye-care essentials.
- Each Home shelf preserves source order and is capped at four products. The Home request currently reads only the first product page, so products outside that page are not candidates for a shelf.
- Home grouping behavior is covered by `HomeViewModelTest`, including disallowed product types and category-name normalization.

## Profile — Patient Account Hub

`presentation/profile/ProfileScreen.kt`, `EditProfileScreen.kt`, and `ProfileViewModel.kt`:

- The main Profile screen is account-first: the previous patient-details card and **Edit profile** button are removed. The Account section appears directly under the page title, with **Account & Security**, a linked-only read-only **Profile** destination for the clinic Patient record, and **Enter Invitation Code** for accounts without an active link.
- **Care & activity** keeps the existing Messages, Order History, Prescriptions, and Feedback History destinations. Navigation rows are label-only with restrained primary-tinted icon treatment; Messages retains the unread badge, caps its visible text at `9+`, and exposes the full count through accessibility semantics.
- **Log out** is a full-width filled error-colored button. It opens the shared themed `AppConfirmationDialog` with **Log out** / **Stay signed in** actions before invoking the existing logout behavior.
- Initial profile loading uses an accessible content-shaped placeholder; load errors retain the existing retry action. Profile data still refreshes on lifecycle resume so edits are reflected after returning from Edit Profile.
- Edit Profile remains limited to the current Android contract: name, email, and nullable phone. Save submits directly without a redundant confirmation dialog. Fields and navigation actions are disabled while saving, validation errors remain attached to their fields, and non-validation failures preserve the draft and show concise inline feedback.
- App-bar Back, system Back, and **Cancel** share dirty-state handling. Unchanged values exit immediately; changed values open the themed **Discard changes?** confirmation. Blank and whitespace-only phone drafts are normalized to the existing nullable-phone behavior when checking for changes.
- The refresh is presentation-only: no backend endpoint, DTO, domain `User`, repository signature, navigation route, dependency, or global theme token changed. Backend-supported address editing remains out of scope because the current Android user/update contract does not expose it.
- Stateless `ProfileContent`, `ProfileLoadingContent`, and `EditProfileContent` composables provide deterministic Compose UI-test seams. `ProfileViewModelTest` covers dirty comparison, initials fallback, and save-failure draft preservation; `ProfileScreenTest` covers the visible hierarchy, callbacks, optional phone, unread semantics, loading semantics, supported edit fields, direct save, and saving/error states.

## Messaging — Tappable Context Cards

`data/remote/dto/MessageDtos.kt`, `data/repository/ChatRepositoryImpl.kt`,
`domain/model/Message.kt`, `presentation/messaging/components/MessageBubble.kt`,
and `presentation/navigation/NavGraph.kt`:

- Inbound message `contexts` are preserved and mapped at the repository boundary to typed appointment, order, or unsupported domain variants. Missing contexts default to an empty list, so ordinary messages remain backward-compatible.
- Mapping accepts both request aliases (`appointment`, `order`) and the backend's current polymorphic response values (`App\Models\Appointment`, `App\Models\Order`).
- Appointment and order links render as distinct nested cards inside the existing message bubble. Each card has its own 56dp minimum tap target, icon, reference ID, directional affordance, and accessibility label; the surrounding bubble remains non-clickable.
- Cards navigate through the existing type-safe `AppointmentDetail` and `OrderDetail` routes. Unknown/product contexts remain non-interactive rather than creating misleading navigation.
- DTO and repository unit tests cover decoding, defaults, typed mapping, and unsupported values. `MessageBubbleTest` covers card rendering and callback IDs; it compiles in the Android test source set. Live device verification confirmed appointment 5 and order 1 cards open their corresponding detail screens.

## Backend API (base: `/api/v1`)

55 approved patient-mobile routes (8 public, 26 account-only, 21 active-link).
Source of truth: `docs/API_CONTRACT.md`.

**Auth:** V13 two-stage OTP registration, hybrid login (trusted skips OTP), password recovery, Sanctum bearer tokens. Stored via `TokenManager` (SharedPreferences). Installation identity via `DeviceIdentityProvider`. 401 → bearer-aware logout via `AuthEventBus`. Session resolution via `GET /me` before routing.

**Breaking changes:** `POST /register`, `POST /login`, `POST /appointments`, three intake routes, `/eyewear`, `/job-orders`, `/billing-records`, and `/job-order-items/{id}/rating` are retired or replaced. `GET /appointment-types` was restored as an account-only patient-visible catalog. See `docs/API_CONTRACT.md` §18–19.

## Appointment Requests — Variable-Duration Scheduling (v16)

`presentation/appointments/requests/RequestAppointmentViewModel.kt`, `RequestAppointmentScreen.kt`:

- **4-step wizard:** Type → Schedule → Details → Review. The patient selects an appointment type first, then date/time, then enters reason/referral/identity, then reviews and submits.
- **Appointment types:** loaded from `GET /appointment-types` on flow entry. Shows patient label, optional description, duration, and referral indicator. No hardcoded types or IDs. Failure shows retry, no fallback.
- **Availability:** `GET /appointment-request-availability` sends both `date` and `appointment_type_id`. Response includes `visit_duration_minutes`, `appointment_type_id`, and 15-minute cadence slots. Only server-returned `available = true` slots are selectable. No fabricated or placeholder slots. No hardcoded Sunday restriction (server-authoritative).
- **Time preferences:** one required primary slot plus up to two optional ordered alternatives. Alternatives are distinct from primary and each other, capped at two. Changing type clears all selections and availability.
- **Referral:** if type `requires_referral` is true, `referring_source` is required (1–255 chars). Non-referral types clear and send `null`.
- **Identity:** linked accounts omit identity; unlinked/pending-review accounts retain existing identity collection with verified-phone behavior.
- **Submission:** sends `appointment_type_id`, `scheduled_at` (primary), `alternative_scheduled_times` (ordered), `reason_for_visit`, conditional `referring_source`, and optional `identity`.
- **Non-binding semantics:** pending requests never reserve capacity. `time_preferences_are_reserved = false` is in the model. UI copy never claims a time is held, reserved, or released. `expires_at` is mapped but omitted from UI.
- **Request detail/list:** shows appointment type, duration, ordered alternatives, referral source when present. Legacy records without new fields render safely (nullable).
- **Error handling:** `SLOT_UNAVAILABLE` clears affected selections and refreshes availability. `ACTIVE_REQUEST_LIMIT_REACHED` preserves draft. Type catalog failure blocks at Type with retry.
- **Tests:** `RequestAppointmentViewModelTest` covers type loading/selection/retry, alternative max-two/uniqueness, referral validation, stale-response handling, and submission.

## Root Navigation

Four approved roots: **Home**, **Frames**, **Appointments**, **Profile**.

- Home: next appointment, current prescription summary, featured frames preview
- Frames: searchable/paged catalog, detail, AR, reservation entry
- Appointments: list, detail, booking, reschedule, cancel, intake
- Profile: hub for Messages, Prescriptions, Reservations, My Eyewear

## My Eyewear — Estimates and Orders

`presentation/eyewear/MyEyewearScreen.kt`, `EstimateListViewModel.kt`,
`OpticalOrderListViewModel.kt`, `EstimateDetailScreen.kt`,
`OpticalOrderDetailScreen.kt`, `FrameRatingViewModel.kt`:

- Profile **Care & activity** contains one **My Eyewear** row (active-link protected).
- The destination has primary **Estimates** and **Orders** tabs, each with
  **Current** and **History** filters. Initial selection is **Estimates → Current**.
- Estimates call `GET /quotations`; Orders call `GET /optical-orders`. Each tab
  owns independent results, pagination, loading, empty, and error states.
- Current/History membership is backend-owned. Draft quotations are never shown.
- Estimate cards show patient status (Awaiting confirmation, Confirmed, Declined,
  Expired), reference, dates, total, and **View order** when `optical_order` exists.
- Order cards show patient status (Preparing, In preparation, Ready for pickup,
  Released to you, Cancelled), reference, dates, total, payment status, and
  remaining balance when > 0.
- Estimate detail is read-only with items, subtotal, discount, total, notes, and
  optional Order cross-link.
- Order detail shows fulfillment tracker (Preparation → Ready → Released),
  timestamps, Eyewear details, optional Payment Summary, optional source Estimate
  link, and rating/revision on `is_rateable` items.
- Rating is one POST upsert (`POST /optical-order-items/{id}/rating`): 201 for
  first creation, 200 for revision. `is_rateable` is server-authoritative.
- Typed integer routes: `MyEyewear`, `EstimateDetail(quotationId)`,
  `OpticalOrderDetail(orderId)`. Cross-links pass ID directly.
- `PatientFeatureIntent` preserves typed Estimate/Order intents through the
  active-link gate.

## Messaging — Quotation and Optical Order Contexts

`data/remote/dto/MessageDtos.kt`, `data/repository/ChatRepositoryImpl.kt`,
`domain/model/Message.kt`, `presentation/messaging/ChatViewModel.kt`:

- Messages expose `sender_type` (patient/staff/unknown) and zero-or-one attachment
  with `download_url`. No `conversation_id` in response DTO.
- Valid context types: `quotation:{id}` and `optical_order:{id}`. Old `appointment`
  and `order` contexts are removed.
- Context picker loads Estimates and Optical Orders independently. Picker failures
  are scoped per source.
- Incoming context cards navigate to typed Estimate/Order detail by type and ID.
  Unknown types render as non-clickable references.
- Bubble ownership compares `message.senderId` to the authenticated account's own id
  (loaded via `GET /me` alongside the conversation), falling back to
  `sender_type == patient` only if that id is unavailable. This backend is known to
  leak raw Eloquent polymorphic class names for other polymorphic fields instead of
  its documented `patient`/`staff` aliases, and patient accounts are `App\Models\User`
  rows just like staff, so `sender_type` alone is not a reliable ownership signal.

## Route Governance — 55 Routes

`test/.../ApprovedApiRoutes.kt`, `test/.../ApiRouteAllowlistTest.kt`:

- 8 public, 26 account-only, 20 active-link (canonical) = 54 canonical callable.
- 1 legacy alias (`POST /job-order-items/{id}/rating`) — exists server-side for backward compatibility, not callable by this client.
- Total registered callable routes: 55 (54 canonical + 1 legacy alias).
- Account-only includes `GET /appointment-types` (restored patient-visible catalog) and `GET /appointment-optometrists` (governance-only, no Android consumer).
- Retired routes explicitly rejected: `/eyewear`, `/job-orders`, `/billing-records`,
  legacy `/login`, `/register`, appointment intake routes.
- Discovery test fails if any rejected route appears in production Retrofit annotations.

## Visit Feedback

`presentation/appointments/AppointmentDetailScreen.kt`,
`AppointmentDetailViewModel.kt`, `AppointmentListScreen.kt`,
`AppointmentListViewModel.kt`, `components/VisitFeedbackDialog.kt`:

- `POST /appointments/{id}/rating` — upsert: 201 creates, 200 revises.
- `AppointmentDto` decodes `is_rateable` (default false) and `rating` (VisitRatingDto?, default null).
- **List entry point:** compact **Rate this visit** chip on confirmed-appointment rows where `isRateable && visitRating == null`. No persisted dismissal.
- **Detail surface:** when rateable and unrated, **Rate your visit** action. When rated, stars + comment + **Update rating** + **Edited** when `revisionNumber > 1`.
- Gated on `isRateable` only — never inferred from `status == fulfilled`.
- Client-side validation: rating 1–5, comment ≤ 1000.
- On success, returned rating merges locally — no re-`load()` round trip.
- `VisitFeedbackDialog` themed to match `AppConfirmationDialog`.

## Frame Rating Aggregates

`data/remote/dto/FrameDtos.kt`, `domain/model/Frame.kt`,
`data/local/entity/FrameEntity.kt`, `data/local/EyecareDatabase.kt`,
`presentation/frames/components/RatingBadge.kt`:

- `GET /frames` and `GET /frames/{id}` return `average_rating` (Double?, null when unrated) and `rating_count` (Int).
- `averageRating` is nullable end-to-end — unrated is not 0.0.
- Room schema 3→4: explicit additive migration (`ALTER TABLE frames ADD COLUMN`).
  `fallbackToDestructiveMigration` removed.
- `RatingBadge` renders `★ 4.5 (12)` for populated values, nothing for null.
  Accessibility: "rated 4.5 out of 5 from 12 ratings".
- Frame detail shows "No ratings yet" when unrated.
- **Known backend bug:** aggregate excludes hidden ratings from both average and count (directional upward skew). Being fixed backend-side. Android displays what the server sends — no client-side correction.

## Branding

| Element | Value |
|---|---|
| Primary color | `#29B6F6` (logo cyan) |
| Text / on-surface | `#3D3535` (logo charcoal) |
| Background | `#F8F9FA` (warm off-white) |
| App name | EyeCare |
| Font | Instrument Sans (Google Fonts, downloaded at runtime) |

Color tokens live in `ui/theme/Color.kt` and are wired into `MaterialTheme.colorScheme` via `ui/theme/Theme.kt` (light scheme only — no dark theme defined yet). Cards use pure white (`CardSurface`) with a subtle 8%-black border (`CardBorder`, mapped to `outlineVariant`) so they float above the warm background.

## Active Specs

- `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-spec.md` — Complete: Variable-Duration Appointment Requests
- `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-plan.md` — Complete: implementation plan (5 stages)
- `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-tasks.md` — Complete: 12 tasks + 4 checkpoints
- `docs/specs/backend-alignment-v15-spec.md` — Complete: Visit Feedback, Frame Ratings, and Contract Correction
- `docs/specs/backend-alignment-v15-plan.md` — Complete: implementation plan (6 stages)
- `docs/specs/backend-alignment-v15-tasks.md` — Complete: 16 tasks + checkpoints
- `docs/specs/backend-alignment-v14-my-eyewear-spec.md` — Complete: My Eyewear Estimates and Orders
- `docs/specs/backend-alignment-v14-my-eyewear-plan.md` — Complete: implementation plan
- `docs/specs/backend-alignment-v14-my-eyewear-tasks.md` — Complete: 31 tasks + checkpoints
- `docs/specs/backend-alignment-v13-auth-spec.md` — Complete: patient account access and security
- `docs/specs/backend-alignment-v13-auth-plan.md` — Complete: implementation plan (7 stages)
- `docs/specs/backend-alignment-v13-auth-tasks.md` — Complete: 30 tasks + 7 checkpoints
- `docs/specs/backend-alignment-v8-spec.md` — Complete: patient workflow migration (60 tasks)
- `docs/specs/backend-alignment-v8-plan.md` — Complete: implementation plan
- `docs/specs/backend-alignment-v8-tasks.md` — Complete: all acceptance criteria met
- `docs/BACKEND_CONTEXT.md` — Full backend documentation (source of truth for API shapes)
- `docs/API_CONTRACT.md` — Authoritative mobile API contract (55 routes)

## Boundaries

- **Never** use Gson — only Kotlinx Serialization
- **Never** store tokens or health data in Room (only frame cache)
- **Never** apply `org.jetbrains.kotlin.android` plugin (AGP 9 built-in)
- **Never** add `android.disallowKotlinSourceSets=false`
- **Always** run `./gradlew assembleDebug` after changes
- **Always** map DTOs to domain models at repository boundary
- **Always** use `sealed interface` for UI state
- **Ask first** before adding new dependencies
- **Ask first** before changing navigation graph structure
