# Implementation Plan: Backend Alignment v22 — Unified Current Appointment Journey

Status: Approved — 2026-09-16

## References

- Approved specification:
  `docs/specs/backend-alignment-v22-current-appointment-journey-2026-09-16-spec.md`
- Authoritative transport contract: `docs/API_CONTRACT.md`
- Authoritative backend context: `docs/BACKEND_CONTEXT.md`
- Historical appointment cutover:
  `docs/specs/backend-alignment-v13-appointment-cutover-*`

## Overview

Implement the approved single-current-journey experience without breaking the
existing request creation, cancellation, rebooking, historical detail, or visit
rating flows.

The implementation proceeds additively at first: establish and test the new
current aggregate and server-filtered history operations, build their
presentation paths, then switch navigation and Home to those paths. The old
merged active-list implementation is removed only after route and caller scans
prove it is no longer needed.

No backend or database work is included. The user's current edits to
`docs/API_CONTRACT.md` and `docs/BACKEND_CONTEXT.md` remain untouched.

## Architecture Decisions

### 1. Keep the current endpoint at the appointment-request boundary

Add `getCurrentAppointmentJourney()` to `AppointmentRequestRepository` and
`AppointmentRequestApiService`. The endpoint is account-only and already lives
under the appointment-request route family. A separate repository and Hilt
binding would add indirection without creating a distinct backend boundary.

The returned domain model is the approved sealed
`CurrentAppointmentJourney`. Home and presentation code consume the domain
aggregate, never its wire discriminator or nullable DTO fields.

### 2. Share DTO-to-domain mappers before mapping the aggregate

The current response embeds both `AppointmentRequestDto` and
`AppointmentV1Dtos.AppointmentDto`. Their current mapping functions are private
inside separate repository implementations.

Extract package-internal mapping functions into focused data-layer mapper files
before implementing aggregate mapping. Existing repositories and the new
current operation then use one mapping definition. This avoids subtly different
status, rating, cancellation, or appointment-reference behavior.

### 3. Give history an intent-specific repository operation

Add `getAppointmentHistory(page, perPage)` to `AppointmentV1Repository` and a
required `filter=history` query in the API call. Do not let presentation code
pass arbitrary filter strings.

The old unfiltered `getAppointments()` operation remains temporarily during the
migration. Remove it only after My Appointment, Home, and tests no longer call
it and a repository-wide reference scan is clean.

### 4. Separate current and historical presentation state

Create independent state holders:

- `MyAppointmentViewModel` owns current-journey loading, retained-content
  refresh, action progress/errors, and authoritative refetch after mutations.
- `AppointmentHistoryViewModel` owns only history pagination, refresh, rating,
  and append retry.

The two ViewModels do not share list state or try to reconcile each other's
responses. Returning from History or a detail route refreshes only the state
that can have changed.

### 5. Reuse focused components, not whole screens

Extract reusable presentation pieces from the current appointment and request
detail implementations:

- appointment status/header and detail sections;
- requested-time and alternative-time sections;
- pending-reschedule section;
- cancellation reason dialog/content;
- schedule/rebooking sheet content;
- appointment history row/card; and
- visit feedback dialog integration.

`MyAppointmentScreen` composes these pieces directly. It does not embed
`AppointmentDetailScreen`, `AppointmentRequestDetailScreen`, or their route-
scoped ViewModels.

Action orchestration needed by both the current root and retained detail routes
must be moved into shared policy/helper code or expressed through focused
repository calls. Do not copy large blocks of availability, same-day, or error-
mapping logic into a second independent implementation.

### 6. Reconcile mutations by refetching the aggregate

After successful create, request update/cancel, appointment cancel, or rebooking
submission, trigger a new current-journey request. Local state may show action
progress and transient success feedback, but it does not manufacture the next
journey variant.

Use request generation or explicit job cancellation so an earlier response
cannot overwrite a later mutation result. Preserve existing content during
background refresh failure.

### 7. Keep route access aligned with backend tiers

- `Appointments` / My Appointment remains account-only.
- `AppointmentRequestDetail` remains account-only.
- New `AppointmentHistory` and existing `AppointmentDetail` remain
  active-link-required.
- Confirmed appointment management controls are hidden or replaced with link
  guidance when the current screen is rendered without an active link.

Link state controls actions and navigation only. It never determines the
current journey kind.

### 8. Preserve deep links while changing the root

Keep `AppointmentDetail(appointmentId)` for History, notifications, and other
existing contextual links. Keep `AppointmentRequestDetail(requestId)` for
original and pending request drill-down.

The Appointments bottom-navigation root changes to `MyAppointmentScreen` and no
longer navigates through an active list card. Existing detail return paths
signal the root to refresh.

### 9. Move Home to the same active source without redesigning Home

Inject the current-journey repository into `HomeViewModel`. Populate the
existing next-visit ticket only from `CurrentAppointmentJourney.Appointment`.
`None` and `PendingRequest` keep the current no-ticket behavior. No new Home
component is introduced in this phase.

## Dependency Graph

```text
current/history contract tests
        │
        ├── shared DTO mappers
        │       │
        │       └── current aggregate repository operation
        │                       │
        │                       ├── MyAppointmentViewModel
        │                       │       │
        │                       │       ├── My Appointment content
        │                       │       └── current action reconciliation
        │                       │
        │                       └── Home current source
        │
        └── history repository operation
                                │
                                └── AppointmentHistoryViewModel/content

My Appointment + History + action behavior
        │
        └── navigation/access cutover
                │
                └── legacy active-list retirement
                        │
                        └── full verification and living docs
```

## Implementation Phases

### Phase 0 — Establish the regression baseline

Before production edits:

- run the existing focused appointment, request, Home, navigation, DTO, and
  repository tests;
- run `assembleDebug`;
- record pre-existing failures separately from V22 regressions; and
- confirm the working tree contains only the user's backend-doc edits plus the
  approved V22 documentation.

This baseline prevents old date-sensitive or unrelated failures from being
misattributed to the new flow.

#### Checkpoint 0 — Baseline recorded

- Focused appointment-related tests have a recorded result.
- `assembleDebug` has a recorded result.
- No user-owned backend documentation was modified by the baseline work.

### Phase 1 — Contract, sealed domain, and repository foundation

Implement tests first for:

- current response decoding for `none`, `pending_request`, and `appointment`;
- appointment responses with null/present original requests and pending
  reschedules;
- malformed/unknown variant rejection;
- current Retrofit path and absence of active-link assumptions;
- shared request and appointment DTO mapping parity; and
- the exact `filter=history`, page, and per-page history query.

Then add:

- current journey DTO/response types;
- `CurrentAppointmentJourney` domain variants;
- shared package-internal request and appointment mapper functions;
- `AppointmentRequestApiService.getCurrentAppointmentJourney()`;
- `AppointmentRequestRepository.getCurrentAppointmentJourney()`;
- `AppointmentV1Repository.getAppointmentHistory()`; and
- history filter support in `AppointmentV1ApiService`.

Keep the legacy list methods during this phase so the app remains buildable.

#### Checkpoint A — Contract proof

- Every documented current variant maps deterministically.
- Unknown or incomplete variants fail and never become `None`.
- Existing appointment/request repository tests still pass after mapper
  extraction.
- History requests prove `filter=history` at the HTTP boundary.
- `assembleDebug` passes.

### Phase 2 — Read-only My Appointment vertical

Build a read-only current-journey path before adding mutation controls:

- `MyAppointmentUiState` with first load, retained content refresh, and
  first-load/background error distinctions;
- `MyAppointmentViewModel` with generation-safe load, retry, pull-to-refresh,
  and resume refresh;
- `MyAppointmentScreen` shell with title and conditional History action;
- full-detail `none`, pending new request, and confirmed appointment content;
- original-request navigation callback; and
- pending-reschedule content beneath the confirmed appointment with explicit
  **Not yet confirmed** language and all alternative times.

Keep this screen unregistered or behind the not-yet-selected root until its
state and Compose tests pass. Reuse extracted formatting and status components
instead of importing list-level behavior.

#### Checkpoint B — Current-state rendering proof

- Unit tests cover load/refresh/race/error behavior.
- Compose tests cover all three variants and nullable supporting objects.
- The pending reschedule cannot replace the confirmed time in presentation
  tests.
- The screen renders for linked and unlinked sessions without protected list
  calls.
- `compileDebugAndroidTestKotlin` and `assembleDebug` pass.

### Phase 3 — Current journey actions and authoritative reconciliation

Add the state-specific actions to `MyAppointmentViewModel` and screen content:

- pending new request schedule update;
- pending new request cancellation with reason and same-day guidance;
- scheduled appointment rebooking availability and linked request submission;
- confirmed appointment cancellation;
- pending-reschedule navigation/update/cancellation handoff; and
- protected-action link guidance.

Extract or share the existing scheduling, cancellation, availability, and
patient-safe error policies before wiring duplicate callers. Preserve action
drafts across recoverable errors, cancel stale availability jobs, and keep POST
submission explicit rather than automatically retrying ambiguous failures.

Every successful mutation ends by reloading the current aggregate. Tests should
assert the follow-up current call and resulting server-provided variant rather
than a locally constructed replacement.

#### Checkpoint C — Active journey behavior proof

- Each eligible action has unit coverage for progress, success, validation,
  server failure, and stale response behavior.
- A pending reschedule blocks a second rebooking action.
- Request cancellation never changes the confirmed appointment locally.
- Link loss or protected-route failure uses existing session reconciliation.
- Existing request-detail and appointment-detail focused tests still pass.
- `assembleDebug` passes.

### Phase 4 — Server-filtered Appointment History vertical

Build History independently from the current screen:

- a type-safe `AppointmentHistory` route;
- `AppointmentHistoryViewModel` using only `getAppointmentHistory()`;
- paginated history content with refresh, append retry, and empty/error states;
- historical appointment detail navigation; and
- existing fulfilled-visit rating and rating-update behavior.

Extract the reusable historical row/card and feedback-dialog integration from
the old combined list. Do not carry over Upcoming tabs, date-strip filtering,
active request cards, booking-limit notices, or request pagination.

#### Checkpoint D — History proof

- Repository tests prove every history page includes `filter=history`.
- ViewModel tests cover refresh, pagination, duplicate handling, append retry,
  and rating updates.
- Compose tests show only historical appointment content.
- Unlinked access is blocked before any history API call.
- Existing historical detail and rating tests pass.
- `assembleDebug` passes.

### Phase 5 — Navigation, Home, and workflow cutover

Switch live callers only after Checkpoints B–D pass:

- register My Appointment as the `Appointments` root;
- add History navigation and access classification;
- preserve request/appointment detail deep links;
- replace the old dual-ViewModel Appointments wiring in `NavGraph`;
- route request-creation success back to My Appointment and refresh it;
- refresh My Appointment when returning from request or appointment detail;
- update Home to load the current aggregate for the next-visit ticket;
- update request-creation preflight to current-journey semantics instead of
  paginated request-list inference; and
- update active appointment detail's pending-reschedule lookup to the current
  aggregate when that route still needs it.

Update route-allowlist and route-access tests in the same phase. Do not alter
the bottom-navigation label or shell geometry.

#### Checkpoint E — Live-flow proof

- Appointments root makes one current-journey read and no active list reads.
- Home no longer calls unfiltered `GET /appointments`.
- Standard request creation, original-request detail, pending-reschedule detail,
  History, notifications, and Back navigation reach the expected destinations.
- Limited sessions load My Appointment without protected calls.
- Linked sessions can open History and manage eligible appointments.
- Navigation, Home, request, and route-allowlist focused tests pass.
- `assembleDebug` passes.

### Phase 6 — Retire obsolete active-list orchestration

Run reference scans before deleting or simplifying anything. Expected obsolete
areas include:

- the combined `AppointmentListScreen` active coordinator;
- `AppointmentsCoordinator` and its link-based Requests/Confirmed selection;
- `AppointmentRequestListViewModel` if no non-root request-history caller
  remains;
- Upcoming/history client partition helpers and date-filter UI;
- active-list reconciliation delays and accepted-request deduplication logic;
- unfiltered `AppointmentV1Repository.getAppointments()` once all callers are
  gone; and
- tests that assert the superseded Requests/Confirmed/Upcoming architecture.

Retain request list transport/repository support if a verified caller or
contract-governance test still requires it. Do not remove detail routes or
shared components merely because the old root is gone.

#### Checkpoint F — Legacy absence proof

- `rg` finds no production caller that infers current state from paginated
  appointment/request lists.
- No active Appointments root tab, date-strip, or request card remains.
- All retained list methods have a named caller and purpose.
- Removed classes have no Hilt, preview, navigation, or test references.
- Focused appointment tests and `assembleDebug` pass.

### Phase 7 — Hardening, visual verification, and living documentation

- verify TalkBack labels/order, 48dp touch targets, large-font scrolling,
  light/dark themes, pull-to-refresh, and system Back;
- verify the three current variants and appointment-with-pending-reschedule on a
  compact Android viewport;
- verify same-day, offline, first-load failure, retained-content refresh
  failure, stale slot, link loss, and empty history states;
- run focused tests, all unit tests, Android-test compilation, lint, and build;
- record any unrelated pre-existing failures without weakening tests; and
- update `CONTEXT.md` and V22 spec/plan/task status to match the shipped Android
  architecture.

Do not edit the user-owned backend contract/context unless the implementation
discovers an actual contract contradiction that requires user direction.

#### Checkpoint G — Completion

- Every approved V22 success criterion has evidence.
- Full verification outcomes are reported accurately.
- No secrets, raw health data, or request/cancellation text are logged or
  persisted.
- Working-tree review confirms only intended app, test, and V22 documentation
  changes.

## Migration and Deletion Strategy

1. Add current/history contracts and tests while old lists still compile.
2. Add My Appointment and History as independent, non-root surfaces.
3. Add current actions and prove mutation refetch behavior.
4. Cut `NavGraph` and Home to the new sources in one coordinated phase.
5. Scan all references and remove only now-orphaned active-list code.
6. Run complete regression and visual checks.

There is no runtime compatibility fallback that reconstructs current state from
old endpoints. If the shipped backend lacks the documented current route, the
screen shows its normal load error; it does not silently revive the superseded
merge behavior.

## Parallelization

After Checkpoint A:

- read-only My Appointment presentation and History presentation can be worked
  independently if they do not edit shared navigation or mapper files;
- Compose fixtures/tests can be developed alongside pure ViewModel tests once
  the domain variants are fixed; and
- component extraction can proceed alongside History repository work if file
  ownership is separated.

Must remain sequential:

- mapper extraction before aggregate repository mapping;
- current repository mapping before current ViewModel implementation;
- action-policy extraction before My Appointment action integration;
- My Appointment and History verification before navigation cutover; and
- navigation cutover before legacy deletion.

For a single implementation thread, follow the numbered phases rather than
parallelizing edits across the same large appointment files.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Nullable discriminated response is accepted in an impossible shape | Incorrect empty state or crash | Repository validates required fields by `kind`; malformed fixtures must fail closed |
| Request and appointment mapping diverge between endpoints | Missing status, rating, cancellation, or linkage data | Extract one shared mapper for each DTO before aggregate work |
| Existing action logic is copied into a second ViewModel | Behavior drift and doubled maintenance | Extract shared policy/helpers and reusable sheets before wiring current actions |
| Old refresh finishes after a mutation refetch | UI reverts to stale journey | Cancel jobs or compare monotonically increasing request generations |
| History accidentally includes active records | Duplicate active appointment in two surfaces | Intent-specific repository method always supplies `filter=history`; verify with MockWebServer |
| Link loss occurs while a protected action is open | Repeated failures or leaked protected navigation | Reuse global active-link error reconciliation and fail closed |
| Notification/deep-link routes break during root replacement | Users cannot open referenced records | Preserve detail routes and add navigation regression tests before deleting old root code |
| Large existing list/detail files encourage monolithic replacement | Difficult review and regressions | Extract focused components and keep new route containers thin |
| Date-sensitive fixtures fail as calendar time advances | False regression signal | Replace hard-coded future assumptions with clock-relative fixtures in touched tests |
| Full unit suite contains unrelated failures | V22 signal becomes ambiguous | Record baseline, run focused gates per phase, and report unrelated failures separately |

## Verification Commands

Run in PowerShell from the repository root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

```powershell
# Contract and repository foundation
.\gradlew testDebugUnitTest --tests "*CurrentAppointment*" --tests "*AppointmentRequestDtosTest" --tests "*AppointmentRequestApiServiceTest" --tests "*AppointmentRequestRepositoryImplTest" --tests "*AppointmentV1RepositoryImplTest"

# Presentation and navigation checkpoints
.\gradlew testDebugUnitTest --tests "*MyAppointment*" --tests "*AppointmentHistory*" --tests "*AppointmentDetail*" --tests "*AppointmentRequestDetail*" --tests "*RequestAppointment*" --tests "*HomeViewModelTest" --tests "*PatientRouteAccessTest" --tests "*ApiRouteAllowlistTest"

# Instrumented test compilation
.\gradlew compileDebugAndroidTestKotlin

# Mandatory final gates
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug

# Diff hygiene
git diff --check
```

## Plan Exit Criteria

The plan is ready to become an executable task list when:

1. the architecture decisions and phase order are approved;
2. My Appointment action ownership is accepted as a shared-policy/component
   extraction rather than whole-screen embedding;
3. additive build-first migration and later legacy deletion are accepted; and
4. no new backend or product decision is required.
