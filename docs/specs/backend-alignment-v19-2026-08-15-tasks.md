# Tasks: Backend Alignment v19 — Messaging Hardening, Search, and Notifications

> Status: **Approved — implementation complete**
> Phase: **Tasks**
> Spec: `docs/specs/backend-alignment-v19-2026-08-15-spec.md` — approved 2026-08-15
> Plan: `docs/specs/backend-alignment-v19-2026-08-15-plan.md` — approved 2026-08-15
> Implementation: not authorized until this document is approved
> Date: 2026-08-15

This is the authoritative Phase 3 execution checklist. Tasks are dependency ordered, sized to five
files or fewer, and designed to leave the repository compiling at every step. Phase 4 implementation
must execute tasks one at a time using `incremental-implementation`, `test-driven-development`, and
`context-engineering` as required by the approved spec workflow.

---

## Execution Rules

1. Start each behavior task with the named failing test or fixture assertion before production code.
2. Do not start a task until all declared dependencies and the preceding checkpoint are green.
3. Run `assembleDebug` after every repository change, including documentation-only tasks.
4. Keep every task scoped to the listed files unless the living spec is updated and reviewed first.
5. Preserve pre-existing user changes in `docs/API_CONTRACT.md`, `docs/BACKEND_CONTEXT.md`, assets,
   and idea documents; use scoped patches and inspect the diff before each checkpoint.
6. Do not mark a checkbox complete from compilation alone. Acceptance criteria and verification must
   both pass.
7. Stop at the human-review checkpoints identified below instead of silently crossing a gate.

## Command Alias

`<gradle>` in this document means:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./gradlew
```

Every task's verification includes `<gradle> assembleDebug`, even when the targeted test command is
listed separately.

## Task Index

| Phase | Tasks | Outcome |
|---|---:|---|
| 0 — Contract gate | 1–2 | Complete authoritative schemas and frozen fixtures |
| 1 — Protocol foundations | 3–6 | 61 routes; message/search/read data boundary |
| 2 — Chat core | 7–11 | Correct timeline, paging, draft safety, and read state |
| 3 — Search | 12–13 | Independent paginated message search |
| 4 — Notifications | 14–17 | Notification data boundary, inbox, and mutations |
| 5 — Main integration | 18–20 | Separate unread counts and account-only navigation |
| 6 — Closeout | 21–22 | Living docs and full verification |

---

## Phase 0 — Contract Gate and Baseline

### Task 1: Complete the authoritative v19 contract

**Description:** Correct only the approved messaging, notifications, and rating-history sections so
Android has one complete wire contract before any consumer signature changes. Preserve the existing
uncommitted backend-document edits.

**Acceptance criteria:**

- [x] Message list/search document optional opaque `cursor`, fixed size 50, required
  `meta.next_cursor`/`meta.has_more`, and newest-first `(created_at, id)` ordering.
- [x] Send-message documents JSON and multipart requests plus the wrapped response; notifications
  document stable `kind` and nullable typed `mobile_action`, with `new_message → conversation`.
- [x] Rating wording says later POSTs update in place, both route inventories remain identical at 61,
  and `BACKEND_CONTEXT.md` records stable mobile notification fields without losing prior edits.

**Verification:**

- [x] Mechanical route-list comparison reports 61 unique routes in each document and no differences.
- [x] `rg -n "append a .*revision|moderation-history revision" docs/API_CONTRACT.md` returns no stale
  rating-history claim.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** None

**Files likely touched:**

- `docs/API_CONTRACT.md`
- `docs/BACKEND_CONTEXT.md`

**Estimated scope:** S (2 files)

### Task 2: Freeze representative v19 contract fixtures

**Description:** Add raw representative fixtures for every new or changed envelope before DTO work.
These tests protect the contract gate independently of production serializers.

**Acceptance criteria:**

- [x] Fixtures cover message list/search cursor metadata, mark-read, JSON/multipart send response,
  notification list/count, UUID mark-one, mark-all, and stable mobile action fields.
- [x] Fixture assertions prove cursor opacity/nullability, notification UUID/string identity, and the
  absence of a mobile navigation dependency on `action_url` or PHP `type`.
- [x] Fixtures match the corrected contract exactly and contain no retired `contexts` request input.

**Verification:**

- [x] `<gradle> testDebugUnitTest --tests "*ApiContractFixturesTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/test/java/com/eyecare/app/data/remote/ApiContractFixtures.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiContractFixturesTest.kt`

**Estimated scope:** S (2 files)

> ### Checkpoint A1 — Contract gate (after Tasks 1–2)
>
> - [x] Corrected docs and fixtures describe one schema.
> - [x] Stable `kind` and `mobile_action.type=conversation` are confirmed; no `action_url` fallback.
> - [x] Focused fixture tests and `assembleDebug` pass.
> - [x] Diff review confirms unrelated user-owned documentation changes are preserved.

---

## Phase 1 — Protocol Foundations and Route Governance

### Task 3: Move route governance to the 61-route contract

**Description:** Establish the new route baseline early so every later Retrofit declaration is
checked against the approved access tier.

**Acceptance criteria:**

- [x] `ApprovedApiRoutes` contains exactly 8 public, 36 account-only, and 17 active-link routes.
- [x] Search, message read, and all four notification routes are explicitly account-only; attachment
  download remains active-link.
- [x] Tests still reject all retired routes and assert each v19 addition independently.

**Verification:**

- [x] `<gradle> testDebugUnitTest --tests "*ApiRouteAllowlistTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** S (2 files)

### Task 4: Introduce cursor-aware message DTO and domain page models

**Description:** Replace the bare message-list envelope with required cursor metadata and add the
domain `MessagePage` boundary without changing presentation behavior yet.

**Acceptance criteria:**

- [x] Message list/search decode required `next_cursor` and `has_more`; mark-read decodes bare
  `marked_count`; missing required cursor metadata fails decoding.
- [x] Domain `MessagePage` carries `messages`, opaque nullable `nextCursor`, and `hasMore` without DTO
  leakage or cursor decoding.
- [x] Existing sender/access/attachment mappings remain defensive, and legacy response `contexts`
  remains safely ignored.

**Verification:**

- [x] DTO tests cover non-terminal, terminal, and missing-metadata responses from Task 2 fixtures.
- [x] `<gradle> testDebugUnitTest --tests "*MessageDtosTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 2

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/MessageDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Message.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/MessageDtosTest.kt`

**Estimated scope:** M (3 files)

### Task 5: Declare cursor, search, and read Retrofit calls

**Description:** Update the Conversation API surface and prove exact query/path behavior before the
repository consumes it.

**Acceptance criteria:**

- [x] `getMessages(cursor)` omits a null cursor and sends a non-null cursor unchanged.
- [x] `searchMessages(q, cursor)` carries both trimmed query and opaque cursor; `markMessagesRead()`
  uses `POST conversation/messages/read`.
- [x] Existing JSON/multipart send and protected attachment download signatures remain valid.

**Verification:**

- [x] MockWebServer test asserts page-1 omission, later cursor query, search `q+cursor`, and read path/
  method.
- [x] `<gradle> testDebugUnitTest --tests "*ConversationApiServiceTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Tasks 3–4

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/api/ConversationApiService.kt`
- `app/src/test/java/com/eyecare/app/data/remote/api/ConversationApiServiceTest.kt`

**Estimated scope:** S (2 files)

> ### Checkpoint A2 — Message protocol (after Tasks 3–5)
>
> - [x] 61-route governance and all Retrofit path/query assertions pass.
> - [x] Required cursor metadata fails closed; opaque cursors round-trip unchanged.
> - [x] `<gradle> testDebugUnitTest` and `<gradle> assembleDebug` pass.

### Task 6: Expose paged list, search, and mark-read through ChatRepository

**Description:** Complete the data-to-domain boundary and remove the old bare-list repository
contract. Presentation ordering remains out of the repository.

**Acceptance criteria:**

- [x] `ChatRepository` exposes `getMessages(cursor)`, `searchMessages(query, cursor)`, and
  `markMessagesRead(): Result<Int>` with `MessagePage` results.
- [x] Implementation maps every message and cursor field at the repository boundary and uses
  `safeApiCall` for all three operations.
- [x] Repository tests cover terminal/non-terminal pages, search forwarding, read count, malformed
  envelope failure, and existing safe unknown mappings.

**Verification:**

- [x] `<gradle> testDebugUnitTest --tests "*ChatRepository*"` passes.
- [x] `<gradle> assembleDebug` passes with temporary caller adaptations kept explicit and local.

**Dependencies:** Task 5

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/domain/repository/ChatRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ChatRepositoryImplTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ChatRepositoryMappingsTest.kt`

**Estimated scope:** M (4 files)

> ### Checkpoint A — Protocol foundation complete (after Task 6)
>
> - [x] All changed endpoints are callable through domain-safe repositories.
> - [x] No DTO escapes the data layer and no cursor is decoded/fabricated.
> - [x] Focused tests, full unit suite, and `assembleDebug` pass.
> - [x] **Human/backend review if stable notification fields still cannot be observed.**

---

## Phase 2 — Chat Timeline, Pagination, Read State, and Draft Safety

### Task 7: Build the deterministic message timeline reducer

**Description:** Introduce one pure, independently tested merge/order component used by initial load,
older pages, polling, and successful sends.

**Acceptance criteria:**

- [x] Messages upsert by stable ID and derive chronological order by parsed instant then ascending ID.
- [x] Overlapping pages and repeated poll/send responses render one row per ID while retaining older
  loaded history.
- [x] Malformed required timestamps fail predictably instead of silently creating unstable order.

**Verification:**

- [x] Reducer tests cover newest-first input, page overlap, replacement by ID, equal timestamps,
  equivalent instants with different offsets, and malformed timestamps.
- [x] `<gradle> testDebugUnitTest --tests "*MessageTimelineTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 6

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/MessageTimeline.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/MessageTimelineTest.kt`

**Estimated scope:** S (2 files)

### Task 8: Refactor ChatViewModel for initial, older-page, and polling state

**Description:** Replace bare-list assumptions with the timeline reducer and guarded cursor state,
leaving draft/read behavior for later focused tasks.

**Acceptance criteria:**

- [x] Initial load derives a chronological list and stores next cursor/terminal state; older-page
  requests are single-flight and advance only on success.
- [x] Polling refreshes page 1, merges by ID, retains older pages, and detects a changed fixed-size
  50-row page without size/`lastOrNull` heuristics.
- [x] Older-page/poll failures preserve usable messages and expose inline retry state without
  replacing the screen with a full error.

**Verification:**

- [x] ViewModel tests cover initial order, cursor success/failure/retry, concurrent guard, fixed-size
  polling, dedupe, and retained older history.
- [x] `<gradle> testDebugUnitTest --tests "*ChatViewModelTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 7

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`

**Estimated scope:** S (2 files)

### Task 9: Render chronological Chat and preserve the viewport on prepend

**Description:** Extract stateless Chat content, request older pages at the top, and anchor the user's
visible message key/offset when older messages are inserted.

**Acceptance criteria:**

- [x] Chat renders normal chronological layout, starts at the newest message, and uses stable message
  ID keys without reverse layout.
- [x] Reaching the top calls load-older only when allowed; top loading/error/retry states do not hide
  existing messages.
- [x] Prepending older rows restores the prior first-visible message key and offset without a jump.

**Verification:**

- [x] Compose tests cover initial newest position, chronological semantics, one load trigger, top
  retry, and anchor preservation after prepend.
- [x] `<gradle> connectedDebugAndroidTest --tests "*ChatScreenTest"` passes when a device is available.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 8

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatContent.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/messaging/ChatScreenTest.kt`

**Estimated scope:** M (3 files)

> ### Checkpoint B1 — Timeline and paging (after Tasks 7–9)
>
> - [x] Reducer, ViewModel paging/polling, and Compose timeline tests pass.
> - [x] Older-page failure is inline; fixed-size poll changes are detected.
> - [x] Full unit suite and `assembleDebug` pass.

### Task 10: Move draft ownership and safe send state into ChatViewModel

**Description:** Eliminate Compose-local draft loss and make sending genuinely single-flight with
patient-safe failure copy.

**Acceptance criteria:**

- [x] Text draft, pending attachment, validation, and submitted snapshot are ViewModel-owned; actual
  controls are disabled while sending.
- [x] Success clears only the submitted draft and merges the response by ID; all failures preserve
  exact text/attachment.
- [x] HTTP 429, 422, network, and unknown failures map to explicit patient-safe copy; raw exception/
  backend text never renders.

**Verification:**

- [x] ViewModel tests cover configuration-safe draft, double-tap single flight, success merge/clear,
  and preserved text/attachment for every failure class.
- [x] Compose test proves input reflects ViewModel state and remains after failure.
- [x] `<gradle> testDebugUnitTest --tests "*ChatViewModelTest"` and `<gradle> assembleDebug` pass.

**Dependencies:** Task 9

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatContent.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/messaging/ChatScreenTest.kt`

**Estimated scope:** M (4 files)

### Task 11: Add visibility-aware message mark-read and success effects

**Description:** Call the idempotent read endpoint at the approved lifecycle points and expose an
explicit success callback for later unread-count integration.

**Acceptance criteria:**

- [x] First successful visible load marks received messages read; newly merged staff messages while
  visible trigger another call; own-only changes do not.
- [x] One read request is active at a time; failure keeps Chat usable and retries only on the next
  eligible visible refresh.
- [x] Success emits one `MessagesMarkedRead` effect, which ChatScreen forwards through
  `onMessagesMarkedRead` without direct global state access.

**Verification:**

- [x] ViewModel tests cover initial/staff/own message triggers, idempotent single flight, hidden screen,
  failure retry timing, and one-shot effect behavior.
- [x] `<gradle> testDebugUnitTest --tests "*ChatViewModelTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 10

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`

**Estimated scope:** M (3 files)

> ### Checkpoint B — Chat core complete (after Tasks 10–11)
>
> - [x] Timeline, paging, polling, draft/send, and read-state tests pass together.
> - [x] Chat remains usable under poll/page/read/send failures and never displays raw errors.
> - [x] Full unit suite and `assembleDebug` pass.
> - [x] **Review Chat behavior before adding search.**

---

## Phase 3 — Conversation Search

### Task 12: Add independent search state and cursor behavior

**Description:** Extend ChatViewModel with a separate search generation/cursor/result stream that
cannot mutate the conversation timeline.

**Acceptance criteria:**

- [x] Search draft is trimmed, explicit-submit only, accepts 3–500 visible characters, and invalid
  input produces local feedback without a request.
- [x] Submitted query owns independent results/cursor/loading/errors; a new generation rejects stale
  prior responses and later pages carry the same query plus opaque cursor.
- [x] Closing search restores timeline pages and draft unchanged and clears no message state.

**Verification:**

- [x] Tests cover validation boundaries, no request while typing, latest-response-wins, page retry,
  query+cursor forwarding, dedupe, and close restoration.
- [x] `<gradle> testDebugUnitTest --tests "*ChatViewModelTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 11

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`

**Estimated scope:** S (2 files)

### Task 13: Build the paginated message-search UI

**Description:** Add the Chat app-bar search entry and stateless newest-first result content without
promising unsupported jump-to-context behavior.

**Acceptance criteria:**

- [x] Accessible Search action opens a field with local validation, explicit submit, and Back that
  restores the timeline/viewport/draft.
- [x] Results show sender, body, attachment metadata, and time with loading/empty/error/load-more
  states; result rows do not claim to jump into timeline context.
- [x] Scrolling near the end requests one later page at a time and inline retry preserves results.

**Verification:**

- [x] Compose tests cover 2/3/500/501-character behavior, submit, result content, states, paging guard,
  and Back restoration callbacks.
- [x] `<gradle> connectedDebugAndroidTest --tests "*MessageSearch*"` passes when available.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 12

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/MessageSearchContent.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/messaging/ChatScreenTest.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/messaging/MessageSearchContentTest.kt`

**Estimated scope:** M (4 files)

> ### Checkpoint C — Search complete (after Tasks 12–13)
>
> - [x] Search generation/cursor/state and Compose tests pass.
> - [x] Timeline pages, viewport, and message draft survive entering/exiting search.
> - [x] Existing Chat core tests, full unit suite, and `assembleDebug` remain green.

---

## Phase 4 — Notification Vertical Slice

### Task 14: Add notification DTO and domain models

**Description:** Define the stable notification boundary with UUID identity and fail-closed product
enums before any endpoint or UI consumes it.

**Acceptance criteria:**

- [x] DTOs decode list links/meta, unread count, UUID ID, stable `kind`, nullable `mobile_action`,
  read/created timestamps, and legacy fields only as ignored compatibility data.
- [x] Domain models preserve UUID strings and map only `new_message`/`conversation`; every unknown or
  missing kind/action becomes `UNKNOWN` without navigation authority.
- [x] Tests prove `action_url`, PHP class names, and `related_type/id` cannot create a mobile
  destination.

**Verification:**

- [x] `<gradle> testDebugUnitTest --tests "*NotificationDtosTest"` passes against Task 2 fixtures.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Tasks 2–3

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/NotificationDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AppNotification.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/NotificationDtosTest.kt`

**Estimated scope:** M (3 files)

### Task 15: Implement notification API, repository, and Hilt boundary

**Description:** Deliver all four notification routes through one domain-safe repository, proving
static/dynamic paths and page behavior with MockWebServer.

**Acceptance criteria:**

- [x] Repository exposes page list, unread count, UUID mark-one, and mark-all; page list uses
  `per_page=20` and maps DTOs at the boundary.
- [x] All calls use `safeApiCall`; mark-one keeps UUID as String and mark-all resolves the static
  `notifications/read-all` path rather than the dynamic route.
- [x] Hilt provides the API and binds the repository without changing NetworkModule or dependencies.

**Verification:**

- [x] Repository/MockWebServer tests cover first/later pages, count, UUID path, mark-all path, unknown
  mapping, 403/422/429/network errors, and malformed payloads.
- [x] `<gradle> testDebugUnitTest --tests "*NotificationRepositoryImplTest"` passes.
- [x] `<gradle> assembleDebug` passes with a resolved Hilt graph.

**Dependencies:** Task 14

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/api/NotificationApiService.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/NotificationRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/NotificationRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/di/NotificationModule.kt`
- `app/src/test/java/com/eyecare/app/data/repository/NotificationRepositoryImplTest.kt`

**Estimated scope:** M (5 files)

> ### Checkpoint D1 — Notification protocol (after Tasks 14–15)
>
> - [x] Stable action mapping and all four route/path tests pass.
> - [x] Unknown actions remain readable but non-navigating.
> - [x] Full unit suite and `assembleDebug` pass.

### Task 16: Implement notification list and mutation state

**Description:** Build the Notification ViewModel with retained page data, UUID dedupe, optimistic
mark-one reconciliation, success-only mark-all, and typed one-shot navigation effects.

**Acceptance criteria:**

- [x] Page 1/later page/refresh retain successful data correctly, dedupe UUIDs, and expose independent
  initial/load-more errors with single-flight guards.
- [x] Unread row tap decrements optimistically once per UUID, emits at most one known action, and
  reconciles/restores on failure; read rows do not re-mutate.
- [x] Mark-all is visible-state driven, single-flight, clears only after success, and all errors are
  patient-safe.

**Verification:**

- [x] ViewModel tests cover paging, refresh retention, dedupe, per-ID double tap, optimistic success/
  failure, mark-all, unknown action, and one-shot effects.
- [x] `<gradle> testDebugUnitTest --tests "*NotificationListViewModelTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 15

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/notifications/NotificationListViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/notifications/NotificationListViewModelTest.kt`

**Estimated scope:** S (2 files)

### Task 17: Build the stateless notification inbox UI

**Description:** Render the approved inbox states and expose callbacks for navigation/global unread
coordination without direct NavController or global ViewModel access.

**Acceptance criteria:**

- [x] Screen/content render initial loading/error/retry, empty, populated, pull-refresh, load-more, and
  inline load-more error while keeping successful rows.
- [x] Rows show title/body/time and a non-color-only unread semantic; Mark all is available only when
  unread exists and is actually disabled while mutating.
- [x] Screen forwards typed action and unread-count-change callbacks; unknown/null actions remain in
  the inbox.

**Verification:**

- [x] Compose tests cover every state, UUID keys, unread semantics, mark-one/read-row behavior,
  mark-all visibility/disabled state, known action callback, and unknown no-navigation.
- [x] `<gradle> connectedDebugAndroidTest --tests "*NotificationListScreenTest"` passes when available.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 16

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/notifications/NotificationListScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/notifications/NotificationListScreenTest.kt`

**Estimated scope:** S (2 files)

> ### Checkpoint D — Notification slice complete (after Tasks 16–17)
>
> - [x] Notification repository, ViewModel, and Compose state/mutation/action tests pass.
> - [x] No backend URL, PHP class, or arbitrary URI can cause navigation.
> - [x] Full unit suite and `assembleDebug` pass.
> - [x] **Review notification copy and interactions before Main integration.**

---

## Phase 5 — Shared Unread State and Navigation Integration

### Task 18: Introduce the Main-graph unread coordinator

**Description:** Replace ad hoc remembered count state with a testable owner for two independent
server-backed counters and mutation reconciliation.

**Acceptance criteria:**

- [x] State contains separate message/notification counts and loading/error freshness; one failed
  refresh preserves the other's last known value.
- [x] Main entry/resume refreshes both; successful Chat read zeros message count then reconciles;
  notification mutation callbacks update only notification count then reconcile.
- [x] Counts clamp safely at zero, refreshes are single-flight per source, and errors never expose raw
  backend text.

**Verification:**

- [x] Tests cover differing counts, partial refresh failure, local-zero/decrement, reconciliation,
  repeated resume, and no cross-counter overwrite.
- [x] `<gradle> testDebugUnitTest --tests "*MainUnreadViewModelTest"` passes.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Tasks 11 and 15

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/MainUnreadViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/MainUnreadViewModelTest.kt`

**Estimated scope:** S (2 files)

### Task 19: Add the Home notification entry and accessible badge

**Description:** Add the account-only notification bell to the greeting header as a stateless Home
callback/count surface ready for NavGraph integration.

**Acceptance criteria:**

- [x] Greeting header lays out title and bell without clipping on compact width or large font scale.
- [x] Positive count shows visible `9+` cap and full "N unread notifications" semantics; zero shows
  no badge while the bell remains available.
- [x] Bell invokes `onNavigateToNotifications` for linked and limited account content alike.

**Verification:**

- [x] Compose tests cover zero/1/9/10+ counts, full semantics, click callback, compact width, and large
  font scale.
- [x] `<gradle> connectedDebugAndroidTest --tests "*HomeScreenTest"` passes when available.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 18

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/home/HomeScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/home/HomeScreenTest.kt`

**Estimated scope:** S (2 files)

### Task 20: Wire account-only Notifications, typed actions, and both badges

**Description:** Integrate already-tested slices at the reserved Main-graph seam, remove direct
repository injection from the Activity/NavGraph, and prove limited-account access.

**Acceptance criteria:**

- [x] Typed `Notifications` is account-only/account-safe, excluded from bottom navigation, reachable
  from Home for linked/limited accounts, and renders NotificationListScreen.
- [x] MainUnread state drives Home notification and Profile message badges; Chat/notification success
  callbacks update only their counter; `conversation` action navigates through typed `Chat`.
- [x] `MainActivity`/`EyecareNavGraph` no longer inject/pass unused `ChatRepository`; unknown actions
  cannot navigate and active-link feature gates remain unchanged.

**Verification:**

- [x] Access-policy tests cover Notifications/Chat as account-only and representative clinical routes
  as active-link; existing redirect tests remain green.
- [x] Navigation/manual smoke: limited account opens inbox and Chat; known notification opens Chat;
  unknown stays; badges can display different counts.
- [x] `<gradle> testDebugUnitTest --tests "*PatientRouteAccessTest" --tests "*MainUnreadViewModelTest"`
  and `<gradle> assembleDebug` pass.

**Dependencies:** Tasks 17–19

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientRouteAccess.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/MainActivity.kt`

**Estimated scope:** M (5 files)

> ### Checkpoint E — Main integration complete (after Tasks 18–20)
>
> - [x] Linked and limited accounts reach both account-only features correctly.
> - [x] Message and notification counters differ/update independently in tests and smoke checks.
> - [x] Badge caps/accessibility and typed action safety pass.
> - [x] Full unit suite and `assembleDebug` pass.
> - [x] **Review integrated navigation and unread behavior before closeout.**

---

## Phase 6 — Hardening, Living Documentation, and Final Verification

### Task 21: Reconcile living docs and workflow status

**Description:** Record landed v19 behavior only after the feature paths are green, and close the
spec/plan/tasks lifecycle without rewriting historical completed specs.

**Acceptance criteria:**

- [x] `CONTEXT.md` records cursor messaging/search/read behavior, notification inbox/action safety,
  independent unread counts, and 8 + 36 + 17 = 61 routes.
- [x] `AGENTS.md` points current work to v19; Active Specs lists v19 spec/plan/tasks with accurate
  statuses; spec and plan record their approvals.
- [x] Tasks document records actual completed checkboxes/checkpoints only, and no historical v2/v17
  document is reopened as current truth.

**Verification:**

- [x] `rg -n "Route Governance — 55|Authoritative mobile API contract \(55 routes\)" CONTEXT.md`
  returns no stale current count.
- [x] Manual cross-read finds no contradiction among `CONTEXT.md`, `API_CONTRACT.md`, and landed code.
- [x] `<gradle> assembleDebug` passes.

**Dependencies:** Task 20

**Files likely touched:**

- `CONTEXT.md`
- `AGENTS.md`
- `docs/specs/backend-alignment-v19-2026-08-15-spec.md`
- `docs/specs/backend-alignment-v19-2026-08-15-plan.md`
- `docs/specs/backend-alignment-v19-2026-08-15-tasks.md`

**Estimated scope:** M (5 files)

### Task 22: Run the final security, regression, and build gate

**Description:** Verify every approved success criterion and audit the final diff without adding new
behavior. Any failure reopens the owning task rather than being patched opportunistically here.

**Acceptance criteria:**

- [x] All 18 spec success criteria map to a passing automated test, command, or recorded manual check.
- [x] Greps prove production navigation does not consume `action_url`, PHP notification classes, or
  arbitrary URIs, and patient UI does not render raw backend diagnostics.
- [x] Final status/diff review contains no accidental modifications to user-owned assets/docs and no
  task exceeded its approved scope without a reviewed spec update.

**Verification:**

- [x] `<gradle> ktlintFormat` then `git diff --check` passes.
- [x] `<gradle> testDebugUnitTest`, `<gradle> lintDebug`, and `<gradle> assembleDebug` pass.
- [x] `<gradle> connectedDebugAndroidTest` passes when a device/emulator is available; otherwise the
  unavailable runtime is recorded and instrumented test sources compile.

**Dependencies:** Task 21

**Files likely touched:** None expected; failures reopen their owning task.

**Estimated scope:** XS (verification only)

> ### Checkpoint F — Final
>
> - [x] All 22 tasks and all earlier checkpoints are complete.
> - [x] Full format/unit/lint/build/instrumented verification is green or explicitly runtime-blocked.
> - [x] 61-route governance and living documents match production code.
> - [x] All v19 success criteria are traceable and satisfied.
> - [x] **Ready for final human review.**

---

## Dependency Summary

```text
1 → 2
1 → 3
2 → 4 → 5 → 6
6 → 7 → 8 → 9 → 10 → 11 → 12 → 13
2 + 3 → 14 → 15 → 16 → 17
11 + 15 → 18 → 19
17 + 18 + 19 → 20 → 21 → 22
```

Safe optional concurrency after Task 6:

- Tasks 7–11 (Chat core) and Tasks 14–17 (Notifications) touch independent production files.
- Tasks 12–13 must follow Chat core.
- Task 18 waits for Chat read success and Notification repository semantics.
- Tasks 20–22 are sequential integration/closeout and must run last.

Execution remains serial unless explicitly coordinated; this document does not authorize agent
delegation or parallel edits.

## Risk-to-Task Traceability

| Plan risk | Prevented or verified by |
|---|---|
| R1 stable action not deployed | Tasks 1–2, Checkpoint A1 |
| R2 mixed/duplicate order | Tasks 4, 6–8 |
| R3 viewport jump | Task 9 |
| R4 fixed-size polling miss | Tasks 7–8 |
| R5 cursor/stale response errors | Tasks 8, 12 |
| R6 draft loss/raw errors | Task 10 |
| R7 mark-read storms | Task 11 |
| R8 optimistic double decrement | Tasks 16, 18 |
| R9 counter cross-overwrite | Tasks 18, 20 |
| R10 limited-account redirect | Task 20 |
| R11 static/dynamic route confusion | Task 15 |
| R12 unavailable instrumentation | Tasks 9, 13, 17, 19, 22 |
| R13 overwrite user work | Tasks 1, 21–22 and every checkpoint diff audit |

## Open Questions

None. After human approval, Phase 4 implementation begins with Task 1 and stops at each declared
review checkpoint.


