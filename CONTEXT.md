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
  appointment requests. Confirmed appointments, prescriptions, and eyewear remain
  active-link-only. Messaging is account-only; attachment download is account-only.
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
- Invitation OTP requests and acceptance verification are single-flight on mobile; their controls
  disable while the request is in progress so rapid taps cannot create duplicate challenges or
  acceptance calls. A 429 is shown as a retry-later message rather than a session-expired error.
- After invitation acceptance, the app fetches the linked account once, hands that account directly
  to `SessionViewModel`, and navigates without issuing a second session-resolution `GET /me`.
- The session handoff cancels and generation-guards older `GET /me` work, so a pre-link response
  cannot replace the linked state with a limited state while the user enters an appointment request.
- The Profile screen also adopts the account from the session handoff immediately, so returning from
  invitation linking cannot render an empty or stale profile while its background `GET /me` refresh runs.
- If Profile's fresh `GET /me` discovers an active link while the navigation session still has a
  limited snapshot, it promotes that linked account into `SessionViewModel`; limited snapshots do
  not cancel the refresh that can discover the link.
- If the appointment API rejects a submitted identity with `IDENTITY_NOT_ALLOWED`, the request flow
  removes the forbidden identity from the saved review state and lets the linked request be retried.
- The appointment-request identity step and outbound identity payload use the account's link status,
  not only the `SessionState` variant, so a stale limited wrapper cannot add Details to a linked flow.

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
  presentation, single-flight invitation actions, 429 recovery copy, linked-session handoff, and
  password-recovery normalization.
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
- UI is a `ModalBottomSheet` showing date and time in **one continuous view** — a week strip above a morning/afternoon-grouped slot list — matching the schedule step of the appointment-request flow (`RequestScheduleContent.kt`) rather than the older tabbed calendar-then-list pattern. There is no separate step for a visit reason here, so nothing is lost by collapsing date and time onto one screen. It skips the partially expanded anchor so the strip, slot list, and confirmation controls open at a usable height on compact screens.
- **Week strip:** seven day cells with prev/next navigation, each showing a backend-resolved verdict (open / closed / fully booked / checking) before the day costs a tap, exactly like the request flow's `WeekStrip`/`DayCell`. `DayAvailability` and `availabilityWeekLength` now live in the shared `AppointmentScheduling.kt` (moved out of the `requests` package) so both scheduling surfaces consume the same primitives instead of duplicating the enum.
  - `AppointmentDetailViewModel.loadRescheduleWeekAvailability(weekStart)` fans out to `GET /appointment-availability` once per visible day (in parallel via `async`/`awaitAll`), the same one-date-per-call fan-out `RequestAppointmentViewModel.loadWeekAvailability` uses — there is no week-range endpoint. Selecting a day still fires the existing single-day `loadRescheduleAvailability(date)` call for the slot list; its response also patches that one day's verdict into `rescheduleDayAvailability` so the strip never flickers back to "checking" for the day already on screen.
  - Opening the sheet seeds `weekStart` at the later of the appointment's current week or today's week (`showRescheduleSheet()`), so it never opens on an all-past, entirely disabled week.
  - **No hardcoded Sunday rule.** Past dates are still excluded locally (never fetched or selectable), but a closed day — Sunday or otherwise — is now server-authoritative via each day's `dayStatus`, matching the appointment-request flow instead of a local weekday check.
- **Slot list:** available times for the selected day are grouped under **Morning**/**Afternoon** headers, styled as radio-led rows (time range + duration), reusing `formatTimeRange`/`formatSlotDuration`/`parseSlotTime` from `requests/RequestFormatting.kt` rather than re-deriving them.
- The draft starts at the appointment's current clinic-local date/time. Same-day past times advance to the next 15-minute slot; past and unchanged selections are blocked locally with concise guidance before any API request.
- Reschedule is available for `scheduled` appointments only. Cancel is available for `scheduled` and `checked_in`. Both the appointment list and detail screen use the canonical capability matrix.
- The list screen's "Reschedule" button navigates to `AppointmentDetailScreen` (not the booking wizard) — actual reschedule happens via the sheet on the detail screen.
- **Confirm-before-submit:** tapping "Review reschedule" in the sheet does not submit immediately — it opens the shared `AppConfirmationDialog` ("Confirm reschedule — Move this appointment to [date] at [time]?") with **Reschedule appointment** / **Keep current time** actions. Only the confirm action calls `onConfirm(scheduledAt)`. Uses local `formatRescheduleDate`/`formatRescheduleTime` helpers (operate on a raw `startsAt` instant, not a combined `scheduled_at` string — separate from `formatAppointmentDate`/`formatAppointmentTime` in `AppointmentListScreen.kt`).
- **On success:** `AppointmentDetailViewModel.rescheduleAppointment` uses the `Appointment` returned directly by the `POST /appointments/{id}/reschedule` response — it does **not** call `load()` to re-fetch. This avoids an extra network round trip and any risk of transiently showing stale data from a second GET. `showRescheduleSheet` is set to `false` and `showRescheduleSuccessDialog` to `true` in the same state update, which dismisses the bottom sheet and immediately shows a confirmation dialog ("Appointment Rescheduled — Your appointment is now set for [date] at [time]"), dismissed via `dismissRescheduleSuccessDialog()`.
- The detail screen's "Reschedule" button uses the same filled, theme-tinted `Button` style (primary color at 12% alpha background, primary content color, no elevation) as the list screen's card action buttons, rather than the plain `OutlinedButton` used elsewhere.
- **Scheduling timezone:** picker/slot values represent Philippine clinic-local time. Booking and rescheduling submit an ISO-8601 timestamp with the explicit `Asia/Manila` offset; response timestamps are converted by instant to `Asia/Manila` for display, filtering, and date grouping.
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

## Home Dashboard — Clinic Hours

`presentation/home/HomeScreen.kt` (`ClinicHoursCard`), `HomeViewModel.kt`, `data/repository/ClinicRepositoryImpl.kt`:

- Backend-driven, not hardcoded. `GET /clinic-hours` (account-only, no active link required) returns all seven `clinic_hours` rows in one call; `HomeViewModel.load()` fetches it in parallel with the other Home sources for both the linked and limited/unlinked branches, degrading to an empty list (card hidden) on failure like the other Home sources.
- `weekday` follows the backend's Carbon convention (`0 = Sunday` … `6 = Saturday`); Android converts `LocalDate.now().dayOfWeek` (ISO, Monday=1..Sunday=7) via `dayOfWeek.value % 7` to find today's row rather than trusting index order. `day_name` from the response is used directly for display so the conversion is never re-derived for copy.
- The schema has one continuous `open_time`–`close_time` range per weekday with no lunch-break field, and disabled days return both times as `null`. The card reflects this: a single formatted range (`9:00 AM – 5:00 PM`) or "Closed", never a fabricated morning/afternoon split.
- Collapsed state shows today's day name and hours inline as the card subtitle; expanding reveals the full seven-day list in the order the API returns it (Sunday-first), with today's row bolded.
- `GET /clinic-hours` is an account-only route in the 59-route API contract — see `ApprovedApiRoutes.kt`/`ApiRouteAllowlistTest.kt`.

## Saved Frames — Account-Owned Preferences (v20)

`presentation/frames/SavedFramesScreen.kt`, `SavedFramesViewModel.kt`,
`data/remote/dto/SavedFrameDtos.kt`, `data/repository/SavedFrameRepositoryImpl.kt`,
`domain/model/SavedFrame.kt`, `domain/repository/SavedFrameRepository.kt`:

- **Account-only access:** Any authenticated account (linked or unlinked) can save, list, and remove
  frame variant preferences. No active patient link is required. Saved Frames never holds inventory,
  never depends on an appointment, and never promises availability.
- **Three routes:** `GET /saved-frames` (page-paginated, newest first), `PUT /saved-frames/{productVariant}`
  (idempotent save, no body), `DELETE /saved-frames/{productVariant}` (idempotent remove, returns 204).
  All three are account-only in the 59-route contract.
- **Catalog `is_saved`:** Every authenticated frame-catalog variant includes an account-specific
  `isSaved` boolean. Mapped at the Frame repository boundary; never persisted in the shared Room
  catalog cache (forced `false` on cache write to prevent cross-account state leakage).
- **Saved Frames list:** Page 1 with `per_page=15`, newest first. Supports initial loading,
  initial error/retry, empty, populated, refreshing, loading-more, and load-more error states.
  Pull-to-refresh replaces the list only on success. Each row shows product/variant identity,
  safe price/image data, saved time, and a non-color-only **Unavailable** status when applicable.
  Row activation opens `FrameDetail(frameId, variantId)` with the saved variant selected.
  Remove is per-row single-flight with patient-safe retry feedback.
- **Preference disclaimer:** The screen visibly presents: **Saved frames are preferences only.
  Availability is not guaranteed until your purchase is confirmed.**
- **Frame Detail save toggle:** The bottom action for the selected variant becomes **Save frame**
  when unsaved and **Remove from saved** when saved. Available to linked and unlinked accounts.
  Mutation is single-flight; success updates only the matching variant's `isSaved` value.
  422 save failure explains the option can no longer be saved.
- **AR Try-On save toggle:** Provides save/remove actions for the selected variant. This is independent of
  appointment and patient-link state. AR asset loading, calibration, fallback, camera capability,
  and renderer behavior are unchanged.
- **Navigation:** Typed `FrameDetail(frameId, variantId?)` with optional variant ID for exact
  saved-variant navigation. Saved Frames is classified account-only and never opens the
  Limited Account link hub.
- **Route governance:** 8 public + 40 account-only + 11 active-link = 59 canonical routes.
  All five former Frame Reservation routes are rejected. Attachment download is account-only.

## Profile — Patient Account Hub (v21)

`presentation/profile/ProfileScreen.kt`, `presentation/account/AccountSecurityScreen.kt`,
`presentation/account/AccountSecurityViewModel.kt`, `presentation/account/AccountProfileEditor.kt`,
and `ProfileViewModel.kt`:

- The main Profile screen is account-first: the Account section appears directly under the page title, with **Account & Security**, a linked-only read-only **Profile** destination for the clinic Patient record, and **Enter Invitation Code** for accounts without an active link.
- **Care & activity** keeps the existing Messages, Order History, Prescriptions, and Feedback History destinations. Navigation rows are label-only with restrained primary-tinted icon treatment; Messages retains the unread badge, caps its visible text at `9+`, and exposes the full count through accessibility semantics.
- **Log out** is a full-width filled error-colored button. It opens the shared themed `AppConfirmationDialog` with **Log out** / **Stay signed in** actions before invoking the existing logout behavior.
- Initial profile loading uses an accessible content-shaped placeholder; load errors retain the existing retry action. Profile data still refreshes on lifecycle resume.
- **Account & Security** is the canonical account editor. It edits account first name, nullable middle name, last name, and date of birth. Contact changes use dedicated Contact Management endpoints; password changes use the protected step-up flow; linked Patient demographics remain read-only.
- Name-only changes save directly without step-up. A request containing a changed DOB triggers the existing same-account step-up OTP flow before the PATCH is sent. The step-up proof is single-use and memory-only.
- `PATCH /me` sends only changed fields using `AccountProfilePatch` with explicit presence semantics: `Unchanged` fields emit no JSON key; `Set(null)` emits explicit JSON `null` for middle-name clearing. The four backend-allowlisted fields (`first_name`, `middle_name`, `last_name`, `date_of_birth`) are the only serializable keys.
- Local validation enforces required/non-blank first and last names, 255-character name limits, exact `Y-m-d` date parsing, and DOB before today in `Asia/Manila`. Laravel field validation errors attach to matching controls; machine-readable step-up errors receive safe flow-level handling.
- Draft input survives OTP cancellation, OTP verification failure, validation failure, network failure, and protected PATCH failure. The single-use proof is cleared after every attempt.
- A successful response becomes the canonical account state. The returned `link_status` is adopted into the shared `SessionState` via `SessionViewModel.adoptAccount()`.
- App-bar Back, system Back, and **Cancel** share dirty-state handling. Unchanged values exit immediately; changed values open the themed **Discard changes?** confirmation.
- The legacy `EditProfile` destination, screen, and duplicate editing state were removed after the canonical Account & Security path was proven.
- `ProfileViewModel` retains load, linked-account adoption, refresh, and logout behavior but no editing state.
- `ProfileViewModelTest` covers linked-account adoption, stale-session cancellation, limited-session non-cancellation, and logout. `AccountSecurityViewModelTest` covers dirty comparison, direct name save, DOB step-up, draft preservation, field error mapping, and no-op exit. `AccountProfileEditorTest` covers normalization, validation, dirty detection, and patch computation. `AccountProfilePatchTest` covers JSON serialization semantics.

## Messaging — Account-Owned Conversation (v17, hardened v19)

`data/remote/dto/MessageDtos.kt`, `data/repository/ChatRepositoryImpl.kt`,
`domain/model/Message.kt`, `presentation/messaging/ChatViewModel.kt`,
`presentation/messaging/ChatScreen.kt`, `presentation/messaging/MessageTimeline.kt`,
`presentation/messaging/MessageSearchContent.kt`,
`presentation/messaging/components/MessageBubble.kt`:

- **Conversation access:** Chat and text messaging are account-only. Linked and unlinked accounts can read and send text messages. `GET /conversation`, `GET /conversation/messages`, `POST /conversation/messages`, `GET /conversation/messages/search`, `POST /conversation/messages/read`, and `GET /conversation/attachments/{attachment}` are account-only routes. Upload still requires the server-provided `can_upload_attachments` capability and linked-patient access.
- **Cursor pagination:** `GET /conversation/messages` and `GET /conversation/messages/search` use opaque cursor pagination (fixed page size 50, newest-first `(created_at DESC, id DESC)` ordering, `meta.next_cursor`, `meta.has_more`). Android stores and returns cursors unchanged; it never decodes, constructs, or fabricates them.
- **Chronological timeline:** Server pages are presented as one chronological timeline (oldest at top, newest at bottom). Messages are merged by stable integer ID using `MessageTimeline` with ascending-instant, ascending-ID tie-breaking.
- **Older history:** Reaching the top of Chat requests `next_cursor` only when `has_more` is true. Older messages insert above the visible history without jumping the viewport.
- **Polling:** Lifecycle-aware polling at 5-second interval refreshes page 1 (no cursor) and merges by ID. Changed fixed-size first page detected via ID set comparison, not size/lastOrNull.
- **Read state:** `POST /conversation/messages/read` is called after initial load and when new staff messages arrive while visible. Own messages don't trigger mark-read. Failure retries opportunistically on next visible refresh.
- **Draft safety:** Text draft is ViewModel-owned. Success clears only the submitted draft. HTTP 429/422/network failures preserve exact text and show patient-safe copy. Send is single-flight.
- **Search:** Cursor-paginated conversation search via `GET /conversation/messages/search?q=&cursor=`. Independent state from timeline (own results, cursor, generation). 3-500 trimmed characters, explicit submit. Back restores timeline, draft, and scroll.
- **Access levels:** Conversation response includes `access_level` (`linked_patient` or `general_inquiry`) and `capabilities` (`can_upload_attachments`). Missing or unknown values fail closed for upload/download while text remains account-available.
- **No structured contexts:** `contexts[]` is retired and never sent. Legacy server messages with `contexts` fields are safely ignored by `ignoreUnknownKeys`.
- **Attachment gating:** Upload controls appear only when `access_level == linked_patient` AND `capabilities.can_upload_attachments == true`. Image preview rendering only attempts the protected download route for `linked_patient` conversations. General-inquiry messages show safe metadata without fetching protected images.
- **Error handling:** All conversation operations use `safeApiCall` to preserve `ApiDomainError` details. Send failures preserve the draft text and show patient-safe copy. Single-flight sends with no automatic retry.
- **Route governance:** 8 public, 40 account-only, 11 canonical active-link routes. 59 canonical callable routes total. Conversation search/read-mark, attachment download, Saved Frames, and notification routes are account-only.

## Notification Inbox (v19)

`data/remote/dto/NotificationDtos.kt`, `data/repository/NotificationRepositoryImpl.kt`,
`domain/model/AppNotification.kt`, `presentation/notifications/NotificationListViewModel.kt`,
`presentation/notifications/NotificationListScreen.kt`,
`presentation/navigation/MainUnreadViewModel.kt`:

- **Access:** Every authenticated account (linked or unlinked) can open Notifications.
- **Notification resource:** Stable `kind` (snake-case product enum, e.g. `new_message`) and nullable `mobile_action` (discriminated by `type`; v19 supports `conversation` only). Legacy `type`, `action_url`, `related_type`, `related_id` are ignored for navigation.
- **Pagination:** Page-based (`per_page=20`), newest-first. Pull-to-refresh replaces page 1 only on success.
- **UUID identity:** Notification IDs are UUID strings end-to-end; never coerced to integers.
- **Read mutations:** `PATCH /notifications/{id}/read` marks one; `PATCH /notifications/read-all` marks all. Mark-one is optimistic per UUID with single-flight guard. Mark-all clears only after server success.
- **Mobile actions:** `conversation` navigates through typed `Chat` route. Unknown/null actions remain in inbox; `action_url` is never read for navigation.
- **Unread badge:** Home greeting header notification bell with `9+` cap. Count from `GET /notifications/unread-count`. Full count in accessibility semantics.
- **Unread coordinator:** `MainUnreadViewModel` owns independent `messageUnreadCount` and `notificationUnreadCount`. Chat mark-read zeros message count; notification mutations update notification count. Neither overwrites the other.

## Backend API (base: `/api/v1`)

59 approved patient-mobile routes (8 public, 40 account-only, 11 active-link).
Source of truth: `docs/API_CONTRACT.md`.

**Auth:** V13 two-stage OTP registration, hybrid login (trusted skips OTP), password recovery, Sanctum bearer tokens. Stored via `TokenManager` (SharedPreferences). Installation identity via `DeviceIdentityProvider`. 401 → bearer-aware logout via `AuthEventBus`. Session resolution via `GET /me` before routing.

**Breaking changes:** `POST /register`, `POST /login`, `POST /appointments`, three intake routes, `/eyewear`, `/job-orders`, `/billing-records`, and `/job-order-items/{id}/rating` are retired or replaced. `GET /appointment-types` was restored as an account-only patient-visible catalog. See `docs/API_CONTRACT.md` §18–19.

## Appointment Requests — Variable-Duration Scheduling (v16)

`presentation/appointments/requests/RequestAppointmentViewModel.kt`,
`RequestAppointmentScreen.kt`, `AppointmentRequestDetailViewModel.kt`, and
`AppointmentRequestListViewModel.kt`:

- **4-step wizard:** Type → Schedule → Details → Review. The patient selects an appointment type first, then date/time, then enters reason/referral/identity, then reviews and submits.
- **Appointment types:** loaded from `GET /appointment-types` on flow entry. Shows patient label, optional description, duration, and referral indicator. No hardcoded types or IDs. The catalog is vertically scrollable so all returned types and Continue remain reachable on small screens. Failure blocks the flow with retry and no fallback. If a submitted type becomes inactive or hidden, the flow returns to Type, refreshes the catalog, clears the invalid selection, and explains what happened.
- **Availability:** `GET /appointment-request-availability` sends both `date` and `appointment_type_id`. Response includes `visit_duration_minutes`, `appointment_type_id`, and 15-minute cadence slots. Only server-returned `available = true` slots are selectable. No fabricated or placeholder slots. No hardcoded Sunday restriction (server-authoritative). Each request renders loading, retryable error, closed/non-open day, and no-times states. Type-catalog and availability responses use latest-response-wins generations, so an older response cannot overwrite a newer selection, including when the same date is selected again.
- **Time preferences:** one required primary slot plus up to two optional ordered alternatives. Alternatives are distinct from primary and each other, capped at two. Changing type clears all selections and availability.
- **Referral:** if type `requires_referral` is true, `referring_source` is required (1–255 chars). Non-referral types clear and send `null`.
- **Identity:** linked accounts omit identity; unlinked/pending-review accounts retain existing identity collection with verified-phone behavior.
- **Submission:** sends `appointment_type_id`, `scheduled_at` (primary), `alternative_scheduled_times` (ordered), `reason_for_visit`, conditional `referring_source`, and optional `identity`.
- **Non-binding semantics:** pending requests never reserve capacity. `time_preferences_are_reserved = false` is in the model. UI copy never claims a time is held, reserved, or released. `expires_at` is mapped but omitted from UI.
- **Request detail/list:** shows appointment type, duration, ordered alternatives, referral source when present. Legacy records without new fields render safely (nullable). Request detail preserves linked-account context through asynchronous loading and cancellation refreshes; retry works after an initial load failure. Patient request access remains ownership-scoped; `REQUEST_NOT_OWNED` renders neutral not-found handling rather than another account’s data.
- **Error handling:** `SLOT_UNAVAILABLE` clears affected selections and refreshes availability. `ACTIVE_REQUEST_LIMIT_REACHED` preserves draft. Type-unavailable field/code errors return to Type and refresh the catalog. Referral validation errors return to Details with the entered draft and field-level feedback. Unknown request-flow failures use patient-safe retryable copy; raw server messages are not shown.
- **Staff boundary:** reviewer queue fields, reviewer preference selection, and the contact-note requirement when accepting outside submitted preferences are backend/staff-surface behavior. Patient mobile only submits ordered preferences and consumes the resulting request state.
- **Tests:** ViewModel tests cover type loading/selection/retry, catalog and availability latest-response-wins behavior, alternative max-two/uniqueness, referral and type validation recovery, safe error copy, request detail linking/retry, and submission. Compose tests cover ordered alternatives, availability states, review content, and scrollable type catalog behavior.

### Schedule step — backup times

`presentation/appointments/requests/RequestScheduleContent.kt`:

- The step runs in two phases (`SchedulePhase`): `PREFERRED` collects the one preferred time,
  `ALTERNATIVES` turns the same rows into checkboxes for up to two numbered backups. One meaning
  per row, so there is no second hidden action competing for the same thumb.
- **`ChosenTimesCard` is present in both phases** once a preferred time exists. It lists the
  preferred time and each ranked backup with its **day**, and carries per-backup removal.
  Two reasons it must be a card rather than list state: a backup routinely sits on a day the slot
  list is not showing, and the previous panel disappeared once both backups were chosen — which
  removed the only entry into the backup phase and left no way to undo a wrong pick.
- The card's add affordance adapts so its height stays roughly constant: a full-width outlined
  **Add backup times** while none are chosen (with the reason they help), a compact **Add another**
  in the header once one exists, and a count plus swap hint when both are taken.
- Rank is carried by a labelled pill (`RankPill`, shared with Review), not by color alone.
  Preferred uses `primaryContainer`, backups use `surfaceVariant`.
- Week-strip day cells mark days holding one of the patient's chosen times (`DayMarker`), so a
  backup in another week is visible from the strip. The marker also reaches TalkBack as
  "holds a time you chose" appended to the day's open/closed status.
- `slotClinicDate` in `RequestFormatting.kt` resolves a slot's clinic-local calendar day for that
  marking.

### Review step — layout

`presentation/appointments/requests/RequestReviewContent.kt`:

- `ReviewCard` uses the app's standard card chrome (white surface, 16dp radius, 1dp
  `outlineVariant` border, no elevation) instead of an elevated 20dp variant, and the inner
  ticket surface stays at 12dp so nested radii stay paired.
- The **preferred time** is the hero: the appointment card shows visit type, a divider, then day
  and time with the time at `titleMedium`, rather than three equal-weight metadata rows.
- Backups appear under **"If that time is taken"** with the same `RankPill` treatment the Schedule
  step uses, so ranking reads identically on both screens.
- `editLabel` is applied as the Edit button's `contentDescription`. On screen all three actions
  read "Edit"; the description is what makes them distinguishable to a screen reader.
- `ReviewRow` is a label/value pair row (label left, value right) instead of stacked label-above-
  value, which is what let the seven-field details card fit.
- Compose tests: `RequestAppointmentScheduleScreenTest` covers the chosen-times card across
  phases, cross-day backup removal when both are taken, the empty-backup rationale, and blocked
  Continue; `RequestReviewContentTest` covers the hero time, ranked backups, distinguishable edit
  actions, and labelled detail pairs.

## Root Navigation

Four approved roots: **Home**, **Frames**, **Appointments**, **Profile**.

- Home: next appointment, current prescription summary, featured frames preview
- Frames: searchable/paged catalog, detail, AR, Saved Frames
- Appointments: list, detail, booking, reschedule, cancel, intake
- Profile: hub for Messages, Prescriptions, Saved Frames, My Orders

## My Orders

`presentation/eyewear/MyEyewearScreen.kt`,
`OpticalOrderListViewModel.kt`, `OpticalOrderDetailScreen.kt`,
`FrameRatingViewModel.kt`:

- Profile **Care & activity** contains one **My Orders** row (active-link protected).
  The app bar also reads **My Orders**.
- Orders-only destination — no Estimates tab. Initial selection is **Current** filter.
- Orders call `GET /optical-orders` with independent results, pagination, loading,
  empty, and error states.
- Current/History membership is backend-owned.
- Order cards show patient status (Preparing, In preparation, Ready for pickup,
  Released to you, Cancelled), reference, dates, total, payment status, and
  remaining balance when > 0.
- Order detail shows fulfillment tracker (Preparation → Ready → Released),
  timestamps, Eyewear details, optional Payment Summary, and rating on
  `is_rateable` items.
- Rating is one POST upsert (`POST /optical-order-items/{id}/rating`): 201 for
  first creation, 200 for revision. `is_rateable` is server-authoritative.
  Frame ratings (RatingBadge on catalog cards and frame detail, rating action
  on order items) are hidden behind `FeatureFlags.FRAME_RATINGS_ENABLED = false`.
  Visit feedback (appointment ratings) stays live.
- Typed integer routes: `MyEyewear`, `OpticalOrderDetail(orderId)`. Cross-links
  pass ID directly.
- `PatientFeatureIntent` preserves typed Order intents through the active-link gate.

## Route Governance — 59 Routes

`test/.../ApprovedApiRoutes.kt`, `test/.../ApiRouteAllowlistTest.kt`:

- 8 public, 40 account-only, 11 active-link = 59 total.
- Account-only includes `GET /appointment-types`, `GET /appointment-optometrists`, `GET /clinic-hours`, Saved Frames (GET/PUT/DELETE), conversation read/list/send/search/read-mark, attachment download, and notification list/count/mark-one/mark-all.
- Attachment download (`GET /conversation/attachments/{id}`) is account-only; upload remains capability-gated.
- Retired routes explicitly rejected: `/eyewear`, `/job-orders`, `/billing-records`,
  legacy `/login`, `/register`, appointment intake routes, and all five `/frame-reservations` routes.
- Discovery test fails if any rejected route appears in production Retrofit annotations.

## Visit Feedback

`presentation/appointments/AppointmentDetailScreen.kt`,
`AppointmentDetailViewModel.kt`, `AppointmentListScreen.kt`,
`AppointmentListViewModel.kt`, `components/VisitFeedbackDialog.kt`:

- `POST /appointments/{id}/rating` — upsert: 201 creates, 200 revises.
- `AppointmentDto` decodes `is_rateable` (default false) and `rating` (VisitRatingDto?, default null).
- **List entry point:** compact **Rate this visit** chip on confirmed-appointment rows where `isRateable && visitRating == null`. No persisted dismissal.
- **Detail surface:** when rateable and unrated, **Rate your visit** action. When rated, stars + comment + **Update rating**.
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

- `docs/specs/backend-alignment-v20-2026-08-27-spec.md` — Complete: Saved Frames cutover and
  reservation retirement
- `docs/specs/backend-alignment-v20-2026-08-27-plan.md` — Complete: implementation plan
- `docs/specs/backend-alignment-v20-2026-08-27-tasks.md` — Complete: implementation tasks + checkpoints
- `docs/specs/backend-alignment-v19-2026-08-15-spec.md` — Complete: Messaging Hardening, Search, and Notifications
- `docs/specs/backend-alignment-v19-2026-08-15-plan.md` — Complete: implementation plan (7 phases)
- `docs/specs/backend-alignment-v19-2026-08-15-tasks.md` — Complete: 22 tasks + 6 checkpoints
- `docs/specs/backend-alignment-v18-2026-08-13-spec.md` — Complete: Frame Reservation Item Editing and My Orders
- `docs/specs/backend-alignment-v18-2026-08-13-plan.md` — Complete: implementation plan
- `docs/specs/backend-alignment-v18-2026-08-13-tasks.md` — Complete: tasks + checkpoints
- `docs/specs/backend-alignment-v17-2026-08-11-spec.md` — Complete: Account-Owned Conversation
- `docs/specs/backend-alignment-v17-2026-08-11-plan.md` — Complete: implementation plan (5 stages)
- `docs/specs/backend-alignment-v17-2026-08-11-tasks.md` — Complete: 12 tasks + 4 checkpoints
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


