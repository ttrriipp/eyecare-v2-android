# Implementation Plan: Care-First Home Screen Redesign

## Overview

Implement the approved Home-screen direction from [`home-screen-redesign-spec.md`](home-screen-redesign-spec.md) as a small series of testable Compose changes. The next appointment—or booking action when absent—will become the first dominant content block. Existing prescription and order updates remain available, followed by restrained clinic-curated shelves for frames, accessories, and eye-care essentials.

This plan does not add routes, dependencies, backend fields, purchasing behavior, or changes outside Home.

## Design Decision

### Chosen direction: care itinerary

The screen opens with a compact greeting and a navy **visit ticket**. Its cyan date marker is the one expressive visual element; care updates and product shelves use quiet white surfaces and the existing typography.

```text
Header
  └── Next visit ticket / Book appointment invitation
        └── Care updates (only when relevant)
              ├── Prescription reminder
              └── Active order
                    └── From the clinic
                          ├── Featured frames
                          ├── Accessories
                          └── Eye-care essentials
```

Alternatives considered and rejected:

- **Mixed dashboard grid:** gives appointments, products, orders, and prescriptions equal visual weight, which repeats the current hierarchy problem.
- **Product-led hero:** makes Home read like retail or e-commerce and conflicts with the confirmed care-first intent.
- **Large generic health score:** the current “Vision Status / 20/20” card is not grounded in authoritative patient data and distracts from actionable information.

Self-critique against generic UI defaults:

- The plan uses the clinic's existing cyan, navy, charcoal, and warm surface system rather than introducing a trendy generic palette.
- It avoids a stock card grid; horizontal shelves encode optional groups while the vertical axis preserves care priority.
- It avoids excessive rounding and shadows by using the established 12/16/24dp shape hierarchy and subtle borders.
- It spends visual emphasis once, on the appointment ticket, instead of decorating every section.

## Architecture Decisions

- Keep `HomeScreen` as the ViewModel-connected route and extract a stateless `HomeContent` composable for previews and UI testing.
- Group products at the Home state boundary, not inside leaf product cards. `HomeUiState.Success` will expose `featuredFrames`, `accessories`, and `eyeCareEssentials`.
- Use `productType` for the stable top-level split and normalized `category` text for the backend's general-product categories. Keep the matcher conservative and unit tested.
- Reuse `formatAppointmentDate` and `formatAppointmentTime` so Home displays clinic-local dates consistently with appointment screens.
- Reuse all existing navigation callbacks. The appointment ticket opens the appointment destination; the empty ticket opens booking; product cards open existing details; **See all** opens Catalog.
- Preserve the existing sealed UI state, repository calls, pull-to-refresh behavior, prescription warning, and active-order tracker.
- Add no global theme tokens unless implementation proves an existing semantic token cannot express the approved design; such a change would require approval first.

## Dependency Graph

```text
Product grouping contract + unit tests
              │
              ▼
Updated Home success state
              │
              ├──────────────┐
              ▼              ▼
Appointment-first shell   Product shelves
              │              │
              └──────┬───────┘
                     ▼
          Care updates + UI states
                     │
                     ▼
       Accessibility and full verification
```

Implementation is sequential because the UI consumes the revised state contract. UI-test work can be written alongside each UI slice but must not get ahead of that slice's behavior.

## Task List

### Phase 1: State Contract

#### Task 1: Curate Home product groups at the state boundary

**Description:** Replace the generic `newArrivals` output with three capped Home groups derived from actual product fields. Write the grouping expectations first, confirm they fail, then implement the smallest grouping logic that makes them pass.

**Acceptance criteria:**

- [ ] `frame` products populate `featuredFrames` in source order.
- [ ] `general` products in accessory-like categories populate `accessories` using case-insensitive matching for accessory, case, and cleaning-kit categories.
- [ ] Other `general` products populate `eyeCareEssentials`; unsupported types do not appear in a Home shelf.
- [ ] Each group is capped to a small Home preview and empty groups remain empty.

**Verification:**

- [ ] Red: targeted grouping tests fail before production logic changes.
- [ ] Green: `HomeViewModelTest` passes after the state implementation.
- [ ] Existing Home appointment, prescription, and active-order tests still pass.

**Dependencies:** None.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/home/HomeViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/home/HomeViewModelTest.kt`

**Estimated scope:** Small — 2 files.

### Checkpoint: State contract

- [ ] Home unit tests pass.
- [ ] The project compiles against the revised `HomeUiState.Success` contract.
- [ ] No DTO, repository, or navigation contract changed.

### Phase 2: Appointment-First Experience

#### Task 2: Build the stateless Home shell and visit ticket

**Description:** Restructure Home around a stateless content composable and replace the greeting/unsupported vision-status lead with the approved appointment-first hierarchy. The visit ticket will use existing appointment formatting and navigation.

**Acceptance criteria:**

- [ ] A compact header is followed immediately by the dominant appointment ticket.
- [ ] An upcoming appointment shows visit reason, textual status, clinic-local date, clinic-local time, and an appointment action.
- [ ] No appointment shows a prominent booking invitation and **Book an appointment** action in the same location.
- [ ] The ticket remains readable with larger font scale and uses accessible touch targets and semantics.

**Verification:**

- [ ] Add Compose UI assertions for the upcoming-appointment and no-appointment states before finalizing the implementation, where the existing instrumentation harness supports them.
- [ ] Compile the debug Android tests.
- [ ] Manually inspect the stateless preview/sample states at compact width.

**Dependencies:** Task 1.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Estimated scope:** Small — 2 files.

#### Task 3: Restore care updates beneath the appointment

**Description:** Preserve prescription-expiry and active-order functionality while making both visually subordinate to the appointment. Keep their current actions and state rules intact.

**Acceptance criteria:**

- [ ] Expiring-prescription guidance appears only when supplied and still opens booking.
- [ ] Active-order progress appears only when supplied and still opens order details.
- [ ] Both sections use restrained existing surfaces, spacing, and typography and do not visually compete with the visit ticket.

**Verification:**

- [ ] Existing Home ViewModel tests pass.
- [ ] Compose state assertions cover presence and absence of optional care updates where practical.
- [ ] Debug compilation succeeds after the slice.

**Dependencies:** Task 2.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Estimated scope:** Small — 2 files.

### Checkpoint: Care-first hierarchy

- [ ] Appointment or booking action is the first dominant content.
- [ ] Optional care updates retain their behavior.
- [ ] Home builds and the focused tests pass.

### Phase 3: Curated Clinic Products

#### Task 4: Add the three editorial product shelves

**Description:** Replace the boxed “New Arrivals” carousel with reusable, quiet product shelves for featured frames, accessories, and eye-care essentials. Product cards support discovery without sales-heavy cues.

**Acceptance criteria:**

- [ ] Each non-empty state group renders under its exact section heading; empty groups are omitted.
- [ ] Cards show a product image, a concise brand/category label, and product name without cart, discount, checkout, or reservation UI.
- [ ] Product cards open the existing product-detail callback and **See all** opens the existing Catalog callback.
- [ ] Lazy rows use stable keys, meaningful image descriptions, readable compact-device widths, and 48dp-minimum interactive targets.

**Verification:**

- [ ] Compose assertions verify shelf visibility, empty-shelf omission, and click callbacks.
- [ ] Missing product images retain a deliberate non-blank fallback surface.
- [ ] Debug compilation succeeds after the slice.

**Dependencies:** Tasks 1 and 3.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Estimated scope:** Small — 2 files.

### Phase 4: States and Polish

#### Task 5: Finish loading, refresh, error, and responsive behavior

**Description:** Ensure the redesigned hierarchy has intentional initial loading, refresh, error, and empty-product behavior and remains clear above the floating bottom navigation.

**Acceptance criteria:**

- [ ] Initial loading uses a content-shaped accessible loading state rather than a contextless blank screen.
- [ ] Pull-to-refresh and retry still call the existing refresh behavior.
- [ ] Error content remains actionable; an empty catalog does not leave empty headings or large gaps.
- [ ] Content remains usable at 320dp width, with larger text, and above the overlaid bottom navigation.

**Verification:**

- [ ] Compose assertions cover loading and error semantics where practical.
- [ ] Review upcoming appointment, no appointment, no products, missing image, and all-sections-populated sample states.
- [ ] Run focused unit and Android-test compilation checks.

**Dependencies:** Task 4.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Estimated scope:** Small — 2 files.

### Checkpoint: Complete UI

- [ ] All approved Home states are implemented.
- [ ] The screen follows existing palette, type, shape, spacing, and navigation conventions.
- [ ] Accessibility labels and touch targets are present.
- [ ] No e-commerce or out-of-scope flow was added.

### Phase 5: Verification and Review

#### Task 6: Run quality gates and review scope

**Description:** Run the project's required checks once after the final code change, review the diff for scope and accessibility, and report any environment-limited checks accurately.

**Acceptance criteria:**

- [ ] Home unit tests pass with no skipped tests.
- [ ] Kotlin formatting and Android lint pass.
- [ ] The required debug build succeeds.
- [ ] Final diff contains only the approved spec, plan, Home UI/state, and focused tests.

**Verification:**

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat ktlintCheck
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

If an emulator is available, also run the focused Home instrumentation test. If it is unavailable, compile the Android tests and explicitly report that runtime UI verification remains pending.

**Dependencies:** Task 5.

**Files likely touched:** None beyond earlier tasks unless a check exposes an in-scope defect.

**Estimated scope:** Extra small — verification only.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Clinic category names differ from expected accessory wording | Medium | Normalize case/spacing, keep a conservative tested keyword set, and route unmatched `general` products to eye-care essentials. |
| Instrumented UI tests cannot run without an emulator | Medium | Keep presentation stateless, compile Android tests, use previews/sample states, and report runtime limitation explicitly. |
| Runtime Google Fonts are unavailable in preview/test environments | Low | Compose falls back without changing layout semantics; do not add a font dependency. |
| Long product or appointment text clips on compact devices | Medium | Avoid fixed text heights, cap product-name lines deliberately, use weighted layout carefully, and inspect at 320dp with larger font scale. |
| Product imagery creates a storefront feel | Medium | Keep images modest, omit price/discount/cart cues, and make the appointment ticket the only high-contrast block. |
| Home refresh temporarily replaces content with loading state | Low | Preserve current state contract in this scope; provide a shaped loading state and avoid expanding into repository/state architecture changes. |

## Explicit Non-Goals

- Product-detail redesign
- Product reservation or inquiry flow
- Cart, checkout, payment, or inventory promises
- Backend or database changes
- Navigation graph changes
- Global theme redesign or dark mode
- Profile-derived personalized greeting
- Refactoring unrelated appointment, order, prescription, or catalog screens

## Plan Approval Gate

Implementation begins only after this plan is explicitly approved. Any discovery that requires a new dependency, route, backend field, or global theme change pauses implementation for a new decision.
