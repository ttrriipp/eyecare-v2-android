# Plan: Backend Alignment v19 — Messaging Hardening, Search, and Notifications

> Status: **Approved — implementation complete**
> Phase: **Plan**
> Spec: `docs/specs/backend-alignment-v19-2026-08-15-spec.md` — approved 2026-08-15
> Tasks: not created; Phase 3 remains gated on approval of this plan
> Baseline: working tree on 2026-08-15; `assembleDebug` verified green after the spec was added
> Date: 2026-08-15

This document defines architecture, dependency order, risk controls, parallelization boundaries, and
verification checkpoints. It deliberately does not contain task-level checklists; those belong in the
Phase 3 tasks document after this plan is approved.

---

## Overview

V19 is four coordinated product changes sharing one account-owned communication boundary:

1. Repair Chat for newest-first cursor pagination while preserving a chronological UI and older
   history.
2. Make message read state, unread badges, and draft-safe sending truthful.
3. Add cursor-paginated conversation search without disturbing the loaded timeline.
4. Add a page-paginated notification inbox with stable, typed mobile actions and its own unread count.

The implementation is ordered by protocol risk, not UI size. Wire-contract corrections and decoding
tests come first. Chat pagination/read behavior follows because it repairs an existing live surface.
Search builds on the settled chat state model. Notifications are then delivered as an independent
vertical slice. Shared unread/navigation integration happens only after both feature slices can report
successful mutations. Governance and living documentation close the work.

Every implementation phase must finish with a green unit suite and debug build. Production code does
not start until the Phase 0 contract gate passes.

## Existing Baseline and Constraints

- The repository currently compiles and the focused pre-v19 messaging/route tests pass, but they
  assert the stale 55-route, bare-list behavior.
- `docs/API_CONTRACT.md` and `docs/BACKEND_CONTEXT.md` already contain user-owned uncommitted changes.
  Implementation must patch only the approved v19 sections and preserve unrelated edits.
- `GET /conversation/messages` is currently decoded as a bare `List<MessageDto>`; cursor metadata is
  discarded and the first 50 server rows are rendered newest-first as though they were oldest-first.
- `ChatViewModel` appends successful sends, compares polling by size/last element, and does not own the
  text draft.
- `NavGraph` holds a local unread-message integer initialized to zero; the injected
  `ChatRepository` parameter is otherwise unused.
- Chat is correctly classified account-only. Notifications must be added to the same account-only
  navigation boundary so limited/unlinked accounts are not redirected to account linking.
- No notification data/domain/presentation modules exist. Existing Hilt feature modules provide the
  pattern for the new vertical slice.
- No Room, FCM, WorkManager, or dependency change is permitted.

## Shape of the Work

Seven ordered phases:

```text
Phase 0  Contract gate + baseline fixtures                  ← fail fast on backend ambiguity
   │
Phase 1  Protocol foundations + 61-route governance        ← DTO/API/repository/DI seams
   │
   ├───────────────┐
   ▼               ▼
Phase 2  Chat core timeline/read/draft     Phase 4  Notification vertical slice
   │               │                               │
   ▼               │                               │
Phase 3  Conversation search                       │
   └───────────────┴───────────────────────────────┘
                           │
                           ▼
Phase 5  Main unread coordinator + navigation integration
                           │
                           ▼
Phase 6  Hardening, living docs, and full verification
```

Execution is serial by default: Phase 0 → 1 → 2 → 3 → 4 → 5 → 6. Once Phase 1 is green, the
notification slice can be developed independently of Phases 2–3 if work is split across sessions;
shared files are reserved for Phase 5.

## Architecture Decisions

### A1 — Contract gate before consumer code

The backend docs currently omit required message cursor metadata/query details, omit the complete
multipart-send contract, expose an admin URL/PHP class as notification navigation data, and retain
stale rating-history wording. Phase 0 corrects the documentation and captures representative JSON
fixtures before Android signatures change.

The notification path is gated specifically on `kind` and `mobile_action` being documented and
available. Android DTO fields remain nullable defensively, but implementation does not substitute
`action_url` or Laravel `type` if the stable fields are absent.

### A2 — Keep cursor and page pagination separate

Messages/search use an opaque cursor; notifications use numeric pages plus `PaginationMeta`. These
protocols have different correctness rules and failure modes. Do not introduce a generic pagination
framework merely to share names.

Use a dedicated required `CursorMetaDto(nextCursor, hasMore)` and `MessagePage` for messages/search.
Reuse the existing page metadata shapes for notifications where compatible. Shared helpers may cover
small pure operations such as ID deduplication, but cursor advancement and page advancement remain in
their owning ViewModels.

### A3 — Message identity map plus deterministic derived order

The ViewModel owns messages by stable integer ID and derives the visible chronological list with a
single comparator:

1. Parse the required ISO-8601 `created_at` to an instant.
2. Sort ascending by instant.
3. Break ties by ascending message ID.

Polling, send responses, and older pages all enter through the same upsert function. This prevents
duplicate rows, fixed-size polling misses, mixed append directions, and loss of already-loaded older
history. Malformed required timestamps are treated as protocol/data failures at the boundary rather
than silently producing unstable order.

### A4 — One cursor state per stream

The conversation timeline and each submitted search query own independent cursor state. Each state
records:

- next opaque cursor;
- terminal `hasMore` flag;
- cursor currently in flight;
- generation/query identity for stale-response rejection;
- inline page error without discarding successful data.

The client never advances before success, retries one cursor concurrently, decodes the cursor, or
loops after `has_more=false`. A new search increments its generation and invalidates earlier results.

### A5 — Presentation owns order and viewport anchoring

Repositories preserve server order and map DTOs; they do not reverse pages for Compose. Chat
presentation exposes the chronological derived list. Before inserting older rows, the screen records
the first visible message key and offset, then restores that anchor after the keyed list changes.

This is preferred over relying on item-count arithmetic because polling or deduplication can change
the number inserted. `LazyColumn` uses stable message-ID keys and normal layout direction; reverse
layout is not introduced.

### A6 — Draft and single-flight send are ViewModel state

`inputText` moves out of `remember` and into `ChatUiState.Success`. `onDraftChanged`, pending
attachment, validation, and the submitted draft snapshot live under one ViewModel-owned state.

A send disables the actual clickable controls, captures the submitted draft, and clears only after
the response succeeds. Failure restores/preserves the exact text and attachment and maps 429/422/
network failures to patient-safe copy. No raw `Throwable.message` reaches Compose.

### A7 — Read state emits explicit success, not shared repository side effects

`ChatViewModel` calls the idempotent mark-read endpoint after first visible load and after merging new
staff messages while visible. It emits a one-shot `MessagesMarkedRead` effect only on success.

`ChatScreen` forwards that effect through an explicit callback to the Main-graph unread coordinator.
The repository remains stateless, and `NavGraph` does not call Retrofit directly. Failed mark-read is
remembered for an opportunistic retry on the next visible refresh, never a tight loop.

### A8 — Main-graph unread coordinator owns two independent counters

Replace `NavGraph`'s remembered integer with a Hilt ViewModel scoped to Main-graph composition. Its
state contains independent `messageUnreadCount` and `notificationUnreadCount` values and independent
refresh/error flags.

It refreshes both on authenticated Main entry/resume. A failure in one source preserves the last
known value of the other. Chat/Notification success callbacks apply immediate local updates and then
schedule server reconciliation. This removes the unused `ChatRepository` injection from
`MainActivity`/`EyecareNavGraph`.

### A9 — Notification actions are a closed domain whitelist

DTO mapping converts stable wire values to domain enums:

```text
kind = new_message              → NotificationKind.NEW_MESSAGE
mobile_action.type=conversation → MobileDestination.CONVERSATION
anything else                   → UNKNOWN
```

Only `CONVERSATION` produces a typed `Chat` navigation effect. `action_url`, PHP class names,
arbitrary URIs, and `related_type` are never navigation inputs. Unknown rows remain visible/readable
and safe to mark read.

### A10 — Notification row taps navigate immediately, mutation reconciles asynchronously

On an unread notification tap, the Notification ViewModel marks the row/count optimistically and
emits its known navigation action once. The mark-one request runs single-flight per notification. A
failure restores/reconciles unread state and produces non-blocking safe feedback; it does not strand
the user in the inbox.

Already-read rows navigate without a redundant request. Mark-all is different: it clears local state
only after server success, matching the approved spec. UUIDs remain strings in paths, models, keys,
and tests.

### A11 — Search is a sub-state, not a second conversation

Chat owns a separate search state containing draft query, submitted query, results, cursor, and
errors. Explicit submit starts a generation; typing alone performs no network call. Search results
remain newest-first and self-contained. Exiting search discards or parks only search state and
restores the timeline, its cursors, draft, and scroll position.

No attempt is made to jump from a result into an unloaded timeline because the backend has no
around-message cursor contract.

## Component and Dependency Map

| Component | Responsibility | Depends on | Primary consumers |
|---|---|---|---|
| Contract fixtures/docs | Complete and freeze wire shapes | Approved v19 spec/backend confirmation | DTO/API tests |
| `CursorMetaDto` / message response DTOs | Decode required cursor envelope | Contract gate | Chat repository |
| Notification DTOs | Decode UUID, stable kind/action, page meta | Contract gate | Notification repository |
| Conversation API/repository | list/search/read/send mapping | Message DTOs, Retrofit | Chat ViewModel |
| Notification API/repository | list/count/mark mutations | Notification DTOs, Retrofit | Notification/Main unread VMs |
| Chat timeline reducer/state | ID merge, ordering, cursors, polling, read, draft | Chat repository | Chat screen/search |
| Search presentation | query generation, cursor paging, result states | Settled Chat state | Chat screen |
| Notification list state | page loading, dedupe, optimistic mutations/actions | Notification repository | Notification screen |
| Main unread coordinator | two counts, refresh, mutation reconciliation | Both repositories/effects | Home/Profile/NavGraph |
| Navigation/access policy | account-only Notifications and typed Chat action | Stable domain action | Main graph |
| Route governance/docs | Assert and record final contract | All prior phases | CI/future agents |

### Dependency graph

```text
API contract corrections + verified fixtures
├── Cursor message/search DTOs
│   └── Conversation API + ChatRepository
│       ├── Chat timeline/read/draft
│       │   └── Search presentation
│       └── message unread source
│
├── Notification DTOs
│   └── Notification API + repository + Hilt
│       ├── Notification list/mutations/actions
│       └── notification unread source
│
└── 61-route allowlist baseline

Chat read success + notification mutation success
└── Main unread coordinator
    ├── Profile message badge
    ├── Home notification bell/badge
    └── typed account-only navigation

All feature paths
└── final contract/context/governance verification
```

## Phase Rationale and Deliverables

### Phase 0 — Contract gate and baseline

Purpose: fail before client code if the authoritative contract is still ambiguous or the stable
notification fields are unavailable.

Deliverables:

- Patch only approved C1–C4 sections in `docs/API_CONTRACT.md`.
- Record representative message list, search, send, notification list/count, mark-one, and mark-all
  fixtures in tests or a contract-fixture source.
- Confirm notification `kind=new_message` and `mobile_action.type=conversation`; legacy fields are
  ignored by client design.
- Capture the pre-implementation focused-test and build baseline without modifying user-owned assets.

Exit: documentation and fixtures describe one predictable contract; no open backend question remains.

### Phase 1 — Protocol foundations and route governance

Purpose: establish compiling, tested data/domain seams before presentation changes.

Deliverables:

- Cursor message/search envelopes, mark-read response, notification models/actions, and page response
  DTOs.
- Updated Conversation API/repository signatures and new Notification API/repository/Hilt module.
- DTO-to-domain mapping with required cursor metadata, UUID preservation, and safe unknown enums.
- Six new account-only routes and exact 8 + 36 + 17 = 61 governance assertions.
- MockWebServer/DTO/repository tests proving query/path/response behavior.

Exit: all six endpoints are callable through domain-safe repositories, no DTO reaches presentation,
and existing production callers compile through deliberate temporary adapters only if a later phase
still needs them.

### Phase 2 — Chat timeline, pagination, read state, and draft safety

Purpose: repair the existing Chat vertical path before adding more behavior to it.

Deliverables:

- Message identity map/reducer, chronological derived list, timeline cursor state, polling merge, and
  older-page retry.
- Stable-key viewport anchoring when older messages arrive.
- ViewModel-owned draft/attachment state, actual send disabling, safe 429/error copy, and merge-on-send.
- Visibility-aware idempotent mark-read and one-shot success effect.
- Unit and Compose tests for fixed-size polling, dedupe, ordering, anchoring, read attempts, and draft
  preservation.

Exit: Chat is correct and usable without search; first/older pages and send/poll/read paths coexist.

### Phase 3 — Conversation search

Purpose: add search only after Chat's state boundaries and cursor primitives are stable.

Deliverables:

- Search mode/action in the Chat app bar.
- Explicit 3–500-character submit flow with independent generation/cursor/result state.
- Newest-first paged result content with loading/empty/inline-error/load-more behavior.
- Back restoration of timeline pages, viewport, and draft.
- Latest-response-wins and later-page query/cursor tests.

Exit: search cannot mutate or replace conversation timeline state and handles stale requests safely.

### Phase 4 — Notification vertical slice

Purpose: deliver the inbox end to end while avoiding shared navigation files until its domain behavior
is proven.

Deliverables:

- Notification list ViewModel with page loading, refresh retention, UUID dedupe, mark-one optimistic
  reconciliation, mark-all success semantics, and typed navigation effects.
- Stateless Notification list content with loading/error/empty/populated/load-more states,
  non-color-only unread semantics, safe unknown actions, and accessible controls.
- Repository/ViewModel/Compose tests covering UUID paths, pagination, mutations, and actions.

Exit: the notification screen works through callbacks in isolation; it does not yet own global badge
state or direct NavController access.

### Phase 5 — Shared unread state and navigation integration

Purpose: connect completed slices at one controlled Main-graph seam.

Deliverables:

- Main unread coordinator with independently refreshed/preserved counts.
- Home greeting-header notification bell and accessible capped badge.
- Profile Messages badge driven by real conversation unread state.
- Typed `Notifications` route, account-only access classification, bottom-nav visibility behavior, and
  `conversation` action → typed `Chat` wiring.
- Success callbacks from Chat/Notifications to immediate local count updates plus reconciliation.
- Removal of direct `ChatRepository` injection from `MainActivity` and `EyecareNavGraph`.
- Navigation/access-policy and two-count independence tests.

Exit: linked and unlinked accounts can open both features, badges are truthful and independent, and
unknown notification actions cannot navigate.

### Phase 6 — Hardening, living documentation, and final verification

Purpose: assert the final state rather than leaving route/docs cleanup scattered through feature work.

Deliverables:

- Patient-safe error audit and greps proving `action_url`/PHP classes/raw errors are not navigation or
  display inputs.
- `CONTEXT.md` updated to v19 behavior and 61 routes.
- `AGENTS.md` and Active Specs pointers updated to the approved spec/plan/tasks documents.
- Focused, full unit, lint, debug build, and available instrumented Compose verification.
- Final diff audit preserving unrelated user changes.

Exit: every spec success criterion is traceable to a test, command, or explicit manual verification.

## File Ownership and Hotspots

| Hotspot | Planned ownership/order | Reason |
|---|---|---|
| `MessageDtos.kt`, `ConversationApiService.kt`, `ChatRepository*` | Phase 1 only, then test fixes | Shared foundation for Chat and search |
| `ChatViewModel.kt` | Phase 2, then additive search state in Phase 3 | Highest merge/conflict risk |
| `ChatScreen.kt` | Phase 2 timeline/draft, then Phase 3 search | Preserve a green UI between slices |
| `NavGraph.kt`, `Routes.kt`, `PatientRouteAccess.kt` | Reserved for Phase 5 | Centralize account-only integration |
| `HomeScreen.kt`, `ProfileScreen.kt` | Phase 5 only | Badge integration after coordinator exists |
| `ApiRouteAllowlistTest.kt`, `ApprovedApiRoutes.kt` | Phase 1; final audit Phase 6 | Contract baseline early, verify final late |
| `docs/API_CONTRACT.md` | Phase 0 scoped patch only | Contains pre-existing user edits |
| `CONTEXT.md`, `AGENTS.md` | Phase 6 | Describe landed behavior, not planned behavior |

The Phase 3 tasks document must keep each implementation task at roughly five files or fewer. Any
task spanning one hotspot and an independent feature slice must be split.

## Risk Register

| # | Risk | Impact | Mitigation | Verification point |
|---|---|---|---|---|
| R1 | Stable `kind`/`mobile_action` is documented but not deployed | High | Phase 0 gate with verified response/fixture; never fall back to `action_url` | Checkpoint A |
| R2 | Reversing pages produces mixed order or duplicate messages | High | One ID-indexed merge path and instant+ID comparator | Checkpoint B |
| R3 | Prepending older rows jumps the viewport | High | Stable keys plus captured key/offset restoration | Checkpoint B Compose test |
| R4 | Fixed 50-row poll hides a new message because size is unchanged | High | Compare/merge IDs, never size/last-only heuristics | Checkpoint B unit test |
| R5 | Cursor repeats, advances on failure, or stale search wins | High | In-flight cursor guard, terminal flag, query generation | Checkpoints B/C |
| R6 | Send failure loses patient text or exposes raw server details | High | ViewModel draft ownership and explicit safe error mapper | Checkpoint B |
| R7 | Mark-read races with polling and causes request storms | Medium | Visibility gate, staff-message detection, pending-retry flag, idempotent call | Checkpoint B |
| R8 | Optimistic notification reads double-decrement count | Medium | Per-ID single flight, clamped derived count, server reconciliation | Checkpoint D |
| R9 | Message and notification counts overwrite each other | Medium | Separate typed state fields and independent refresh results | Checkpoint E |
| R10 | Limited accounts are redirected away from Notifications | Medium | Account-safe destination/classifier tests before NavGraph wiring | Checkpoint E |
| R11 | Dynamic `{notification}` route is confused with static `read-all` | Medium | Explicit Retrofit annotation tests for both paths | Checkpoint A |
| R12 | Compose instrumentation is unavailable | Low | Pure reducer/ViewModel tests plus stateless content seams; run device tests when available | Every checkpoint |
| R13 | Existing user-owned doc/asset changes are overwritten | High | Scoped patches, status/diff audit before every doc phase | Checkpoints A/F |

## Verification Checkpoints

Detailed commands and per-task acceptance criteria will live in the Phase 3 tasks document. The plan
requires these gates:

### Checkpoint A — Contract and protocol foundation

- Complete contract shapes and fixtures agree.
- DTO/API/repository/route tests pass.
- Exactly 61 approved routes with all six v19 routes account-only.
- `assembleDebug` and focused unit tests pass.
- Human/backend confirmation if live stable notification fields cannot be observed locally.

### Checkpoint B — Chat core

- Timeline order, same-time tie-breaking, dedupe, fixed-size polling, older paging, viewport anchor,
  draft preservation, and mark-read tests pass.
- Chat remains usable when poll/page/read requests fail.
- Focused tests, full unit suite, and debug build pass.
- Human review of Chat behavior before search is layered on.

### Checkpoint C — Search

- Query validation, latest-response-wins, independent cursor, load-more retry, and Back restoration
  pass.
- Existing Chat core tests remain unchanged and green.
- Full unit suite and debug build pass.

### Checkpoint D — Notification slice

- Page/refresh retention, UUID dedupe, mark-one, mark-all, unknown action, and navigation-effect tests
  pass.
- Stateless Compose content passes accessibility/state tests.
- Full unit suite and debug build pass.
- Human review of notification copy and row interaction before global integration.

### Checkpoint E — Main integration

- Linked and limited accounts reach Chat and Notifications correctly.
- Message/notification counters differ and update independently in tests.
- Home/Profile badge caps and full accessibility counts pass.
- `action_url` and unknown actions cannot navigate.
- Full unit suite, navigation tests, and debug build pass.

### Checkpoint F — Final

- All 18 spec success criteria are accounted for.
- `ktlintFormat`, full unit suite, `lintDebug`, and `assembleDebug` pass.
- `connectedDebugAndroidTest` passes when a device/emulator is available; otherwise the limitation is
  recorded with compiled instrumented test sources.
- Route counts and living docs match the landed implementation.
- Final diff contains no accidental changes to existing user work.
- Human approval before Phase 4 implementation is declared complete.

## Parallelization Opportunities

No parallel execution is required. If work is divided across sessions after Phase 1:

- **Safe to parallelize:**
  - Phase 2 Chat core and Phase 4 Notification slice after both repository contracts are green.
  - DTO/fixture tests for Notifications alongside pure Chat reducer tests, provided production files
    have clear ownership.
- **Must be sequential:**
  - Phase 0 → Phase 1.
  - Chat Phase 2 → Search Phase 3.
  - Both feature slices → Main integration Phase 5.
  - All implementation → final docs/governance Phase 6.
- **Needs coordination:**
  - `ChatViewModel.kt` and `ChatScreen.kt` are owned serially by Phases 2–3.
  - `NavGraph.kt`, `Routes.kt`, and access policy are reserved for Phase 5.
  - `docs/API_CONTRACT.md` has pre-existing edits and receives one scoped owner in Phase 0.

## Verification Commands

The implementation environment uses Android Studio's bundled JBR when `JAVA_HOME` is absent:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

./gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest" --tests "*MessageDtosTest" --tests "*ChatRepository*" --tests "*ChatViewModelTest" --tests "*Notification*"
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

Run `./gradlew assembleDebug` after each repository change, not only at final verification.

## What This Plan Does Not Do

- It does not create Phase 3 task checklists or authorize implementation.
- It does not alter backend code; Phase 0 records the coordinated contract prerequisite and stops if
  the stable notification fields are unavailable.
- It does not add push delivery, background sync, offline storage, Room migrations, or dependencies.
- It does not redesign the four root tabs or turn Notifications into a fifth tab.
- It does not use search results to jump into unloaded timeline context.
- It does not couple mobile navigation to admin URLs, PHP classes, or arbitrary URI handling.
- It does not change staff inbox/read/archive behavior.

## Open Questions

None. After human approval, proceed to Phase 3 and generate a separate, dependency-ordered tasks
document with tasks sized to five files or fewer.

