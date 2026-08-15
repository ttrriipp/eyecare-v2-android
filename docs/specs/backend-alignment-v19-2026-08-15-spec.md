# Spec: Backend Alignment v19 — Messaging Hardening, Search, and Notifications

> Status: **Draft — awaiting human review**
> Phase: **Specify**
> Date: 2026-08-15
> Sources: `docs/API_CONTRACT.md` (2026-08-15, 61 routes),
> `docs/BACKEND_CONTEXT.md` (2026-08-15)
> Supersedes the Android messaging and route-governance assumptions recorded in
> `docs/specs/backend-alignment-v17-2026-08-11-spec.md`, `CONTEXT.md`, and the historical
> `docs/specs/backend-alignment-v2-spec.md`.

---

## Objective

Align the Android patient app with the backend's 2026-08-15 direct-messaging contract and ship the
new patient-facing message search and notification inbox.

The work serves every authenticated patient account, including accounts without an active Patient
link. A user must be able to:

1. Open the conversation at its newest message while retaining access to the complete older history.
2. Receive and clear an accurate unread-message badge.
3. Search the account's conversation with stable cursor pagination.
4. Open a paginated notification inbox, distinguish unread notifications, mark one or all as read,
   and follow supported mobile actions safely.
5. Recover from offline, validation, and rate-limit failures without losing a message draft or seeing
   raw server diagnostics.

This is a coordinated contract-and-client alignment. The Android implementation must not begin until
the API documentation includes the complete cursor, multipart-send, and mobile-notification schemas
defined below.

## Assumptions

These assumptions were surfaced before this specification was written and are part of the review
gate.

1. The behavior described in the 2026-08-15 backend documents is already deployed except for the
   additive stable mobile-notification fields defined by this spec.
2. Android will ship both message search and the notification inbox in v19; neither is a route-only
   or deferred feature.
3. The backend will add stable `kind` and `mobile_action` fields to notification resources. Existing
   `type`, `action_url`, `related_type`, and `related_id` fields may remain for compatibility, but
   Android never uses them for navigation.
4. `GET /conversation/messages` and `GET /conversation/messages/search` use the same opaque cursor
   shape: fixed page size 50, newest-first `(created_at DESC, id DESC)` ordering,
   `meta.next_cursor`, and `meta.has_more`.
5. The visible chat timeline is chronological (oldest at the top, newest at the bottom). Android may
   reverse a server page for presentation but must never derive or modify a cursor.
6. The conversation is account-owned and singular, so the `conversation` mobile action needs no ID.
7. Message unread state and notification unread state are separate server-owned concepts. Opening
   Chat marks received messages read; it does not mark unrelated notification rows read.
8. Notification list pagination remains page-based with `per_page=20`; message and search pagination
   remain cursor-based. Android will not force these APIs behind one pagination abstraction if that
   obscures their different semantics.
9. No new dependency, Room table, background worker, push provider, or navigation root is required.
10. Existing sender-type, attachment-download URL, access-level, and upload-capability mappings are
    directionally correct and will be retained with defensive unknown-value handling.

## Tech Stack

- Kotlin 2.3.0, JVM 11
- Android Gradle Plugin 9.2.1; compile SDK 35, minimum SDK 26
- Jetpack Compose + Material 3 using the Compose 2026.05.01 BOM
- Type-safe Navigation Compose routes with Kotlinx Serialization
- MVVM + Clean layers: data → domain → presentation
- Hilt 2.59.2 for dependency injection
- Retrofit 2.11.0 + OkHttp 4.12.0 + Kotlinx Serialization 1.8.1
- Coroutines, `StateFlow`, JUnit 5, MockK, Turbine, MockWebServer, and Compose UI tests

No dependency change is authorized by this specification.

## Commands

Run from the repository root:

```powershell
# Format
./gradlew ktlintFormat

# Focused unit tests during implementation
./gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest" --tests "*MessageDtosTest" --tests "*ChatRepository*" --tests "*ChatViewModelTest" --tests "*Notification*"

# Full unit suite
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug

# Required build after every repository change
./gradlew assembleDebug

# Instrumented Compose tests when an emulator/device is available
./gradlew connectedDebugAndroidTest
```

## Project Structure

Existing and expected integration points:

```text
docs/
├── API_CONTRACT.md                         → authoritative wire contract corrections
├── BACKEND_CONTEXT.md                      → backend behavior and 61-route inventory
└── specs/
    └── backend-alignment-v19-2026-08-15-spec.md

app/src/main/java/com/eyecare/app/
├── data/remote/api/
│   ├── ConversationApiService.kt           → cursor, search, and mark-read endpoints
│   └── NotificationApiService.kt           → list/count/mark-one/mark-all endpoints
├── data/remote/dto/
│   ├── MessageDtos.kt                      → cursor metadata and mark-read response
│   └── NotificationDtos.kt                 → page metadata and mobile action DTOs
├── data/repository/
│   ├── ChatRepositoryImpl.kt               → DTO mapping; no presentation ordering policy
│   └── NotificationRepositoryImpl.kt       → notification DTO-to-domain boundary
├── domain/model/
│   ├── Message.kt                          → message page and existing chat models
│   └── AppNotification.kt                  → stable kind/action domain models
├── domain/repository/
│   ├── ChatRepository.kt                   → paged list/search and mark-read contract
│   └── NotificationRepository.kt           → paged notifications and read mutations
├── presentation/messaging/
│   ├── ChatViewModel.kt                    → merge/dedupe/order/read/search/draft policy
│   ├── ChatScreen.kt                       → chronological timeline and search entry
│   └── MessageSearchContent.kt             → paginated search UI
├── presentation/notifications/
│   ├── NotificationListViewModel.kt        → list paging and read mutation state
│   └── NotificationListScreen.kt           → inbox, empty/error/loading states
└── presentation/navigation/
    ├── Routes.kt                           → typed Notifications sub-destination
    └── NavGraph.kt                         → account-only navigation and unread coordinator

app/src/test/java/com/eyecare/app/           → DTO, repository, ViewModel, and route tests
app/src/androidTest/java/com/eyecare/app/    → chat/search/notification Compose tests
```

Names may be refined during planning, but layer ownership may not be collapsed. Retrofit DTOs stay
in `data`, business-safe models stay in `domain`, and Compose state/presentation ordering stay in
`presentation`.

## Contract Corrections Required Before Android Implementation

### C1 — Complete cursor contract for message list and search

`docs/API_CONTRACT.md` must document the optional opaque `cursor` query parameter on both endpoints
and show the complete response shape:

```json
{
  "data": [],
  "meta": {
    "next_cursor": null,
    "has_more": false
  }
}
```

Rules:

- Page size is fixed at 50.
- Results are newest-first by `(created_at DESC, id DESC)`.
- `next_cursor` is opaque and nullable. Android stores and returns it unchanged.
- `has_more: false` is terminal even if `next_cursor` is unexpectedly non-null.
- Missing or malformed metadata is a protocol failure, not permission to fabricate a cursor.
- Search carries both `q` and the returned `cursor` on later pages.

### C2 — Restore the complete send-message contract

`POST /conversation/messages` must document both supported request encodings:

1. JSON text message: required `body`, maximum 5,000 characters.
2. Multipart attachment message: required `body`, optional singular `attachment`, allowed PDF/PNG/
   JPG/JPEG/DOC/DOCX, maximum 10 MB, and active Patient link required for an attachment.

Both forms return the created message wrapped as `{ "data": MessageResource }`. Legacy `contexts`
input is prohibited. The documented response includes `sender_type` and
`attachments[].download_url`.

### C3 — Stable mobile notification contract

The notification resource adds these fields without requiring removal of existing fields:

```json
{
  "id": "uuid",
  "kind": "new_message",
  "title": "New Message",
  "body": "Dr. Santos sent a message.",
  "mobile_action": {
    "type": "conversation"
  },
  "read_at": null,
  "created_at": "2026-08-15T10:00:00+08:00"
}
```

Contract rules:

- `kind` is a stable snake-case product enum. It never contains a PHP namespace or class name.
- `mobile_action` is nullable and discriminated by `type`.
- V19 supports `mobile_action.type = conversation` only.
- A `new_message` notification must return the `conversation` action.
- The action has no ID because the authenticated account owns one conversation.
- Future resource actions may add an integer `id`, for example
  `{ "type": "appointment", "id": 123 }`, without changing v19 behavior.
- Android treats unknown `kind` or action `type` as `UNKNOWN`; the row remains readable, but tapping
  it performs no navigation.
- Android does not parse, render as a link, or open `action_url`.
- `title` and `body` are patient-safe display strings. They contain no HTML and no staff-only data.
- Notification IDs remain strings/UUIDs end to end; they are never coerced to integers.

### C4 — Correct stale rating-history wording

The API contract must say that later rating POSTs update the current rating in place and return HTTP
200. It must not claim that the removed `frame_rating_revisions` or `visit_rating_revisions` history
is appended.

## Functional Requirements

### F1 — Route governance

`ApprovedApiRoutes` and its tests must describe exactly:

- 8 public routes
- 36 authenticated account-only routes
- 17 active-link routes
- 61 canonical patient-mobile routes total

The six account-only additions are:

```text
GET    /api/v1/conversation/messages/search
POST   /api/v1/conversation/messages/read
GET    /api/v1/notifications
GET    /api/v1/notifications/unread-count
PATCH  /api/v1/notifications/{notification}/read
PATCH  /api/v1/notifications/read-all
```

Explicit tests must verify these routes are in the account-only set. Attachment download remains
active-link protected.

### F2 — Message page model and repository boundary

The repository returns a domain page rather than a bare list:

```kotlin
data class MessagePage(
    val messages: List<Message>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

interface ChatRepository {
    suspend fun getMessages(cursor: String? = null): Result<MessagePage>
    suspend fun searchMessages(query: String, cursor: String? = null): Result<MessagePage>
    suspend fun markMessagesRead(): Result<Int>
}
```

The data layer maps DTOs to domain objects and preserves server order. Timeline ordering, deduping,
scroll behavior, and search presentation belong to the ViewModel/presentation layer.

### F3 — Initial conversation and chronological timeline

- Initial load requests the first message page with no cursor.
- The screen presents that newest-first server page chronologically, oldest at top and newest at
  bottom, then positions the list at the newest message without visible reverse-order flashing.
- Equal timestamps are ordered by ascending ID in the visible chronological timeline.
- Sending or polling merges by message ID; it never blindly appends to whichever order happens to be
  in memory.
- An existing loaded older page is retained when the first page is refreshed.
- Duplicate IDs across polling, send responses, and page boundaries render once.

### F4 — Loading older messages

- Reaching the top of Chat requests `next_cursor` only when `has_more` is true and no older-page
  request is active.
- Older messages are inserted above the current visible history without jumping the user's viewport.
- The cursor advances only after a successful response.
- An older-page failure leaves existing messages visible and exposes a retry affordance at the top.
- Empty successful pages terminate safely according to `has_more`; the client never loops on one
  cursor.

### F5 — Polling and incoming messages

- Lifecycle-aware polling may continue at the existing five-second interval while Chat is visible.
- Polling always refreshes the first page without a cursor and merges by stable ID.
- A fixed 50-item first page can change without changing its size; change detection must not rely on
  list size or `lastOrNull()` alone.
- Poll failures do not replace a usable conversation with a full-screen error.
- No background polling, WorkManager job, or FCM behavior is introduced.

### F6 — Read state and unread-message badge

- After the initial conversation and first message page load while Chat is visible, Android calls
  `POST /conversation/messages/read`.
- When polling adds one or more staff messages while Chat remains visible, Android calls mark-read
  again. The endpoint is idempotent.
- Own messages do not trigger a mark-read call by themselves.
- Mark-read failure does not block Chat. It is retried opportunistically on the next visible refresh,
  without a rapid retry loop.
- The Main graph owns a message-unread count sourced from `GET /conversation`.
- The Profile Messages badge shows this count, caps visible text at `9+`, and retains the full count
  in accessibility semantics.
- Successful mark-read updates the local message badge to zero and then refreshes server state.

### F7 — Draft ownership, send state, and throttling

- The message draft is ViewModel-owned so configuration changes and send failures do not erase it.
- A successful text send clears only the submitted draft and merges the returned message by ID.
- A failed send preserves the exact draft and pending attachment.
- Send is single-flight. UI controls are genuinely disabled while sending; color alone is not used as
  the disabled mechanism.
- HTTP 429 shows patient-safe copy such as "You're sending messages too quickly. Please wait and try
  again." If `Retry-After` is available, the UI may show a safe rounded wait duration.
- Raw backend exception text, validation internals, bearer data, or PHP class names are never shown.

### F8 — Conversation search

- Chat exposes an accessible Search action in its top app bar.
- Search is an explicit submit action, not a network request on every keystroke.
- The client trims the query, requires at least 3 visible characters because of the documented MySQL
  token limitation, and caps input at 500 characters.
- Results are newest-first, visually distinct from the chronological conversation timeline, and show
  sender, body, attachment metadata when present, and created time.
- Search results use their own cursor, loading, paging, empty, and inline retry states. They never
  overwrite the loaded conversation or its cursor.
- A new submitted query cancels or invalidates older in-flight results; stale responses cannot replace
  the latest query.
- Back exits search and restores the conversation, its loaded pages, scroll position, and draft.
- Search results are self-contained in v19; tapping a result is not required to locate it in the
  timeline because the API has no around-message cursor contract.

### F9 — Notification inbox entry and count

- Every authenticated account, linked or unlinked, can open Notifications.
- Home gains an accessible notification-bell action in the greeting header with an unread badge.
- The badge count comes only from `GET /notifications/unread-count`, caps visible text at `9+`, and
  exposes the full count to accessibility services.
- The count refreshes on Main-graph entry/app resume and after notification read mutations. No
  background refresh is required.
- Message unread and notification unread use separate state properties and cannot overwrite each
  other.

### F10 — Notification list

- Notifications is a typed Main-graph sub-destination, not a fifth bottom-navigation root.
- The screen requests page 1 with `per_page=20`, renders newest first, and loads later pages as the
  user approaches the end.
- Rows show title, body, relative or formatted time, and a non-color-only unread indicator.
- The screen defines initial loading, initial error with retry, empty, populated, load-more, and
  load-more error states.
- Duplicate UUIDs across refreshed pages render once.
- Pull-to-refresh replaces page state only after a successful page-1 response; a failed refresh keeps
  the existing list visible.

### F11 — Notification read mutations and mobile actions

- Tapping an unread row calls `PATCH /notifications/{notification}/read`, updates that row and count
  optimistically, and then performs a supported mobile action. A failed mutation reconciles with the
  unread-count endpoint and shows non-blocking patient-safe feedback.
- Tapping an already-read row does not issue a redundant mark-one request but may still perform its
  supported action.
- `mobile_action.type = conversation` navigates through the existing typed `Chat` route.
- Unknown or missing actions leave the user in Notifications; no arbitrary URI or backend URL opens.
- A **Mark all as read** action is available only when unread count is greater than zero. It calls
  `PATCH /notifications/read-all`, clears local notification unread state on success, and is
  single-flight.
- Opening Chat directly marks conversation messages read but does not mark notification rows read.
  Tapping a specific message notification marks that notification before navigating to Chat.

### F12 — Documentation alignment

The implementation phase updates:

- `docs/API_CONTRACT.md` with C1–C4.
- `CONTEXT.md` with the 61-route totals, paginated/searchable messaging, read behavior, notification
  inbox, and stable mobile action contract.
- `AGENTS.md` current-work pointer to the active v19 spec/plan as the gated workflow advances.
- Active-spec listings only when the corresponding spec/plan/tasks documents exist.

Historical completed specs remain unchanged except where a clearly marked stale cross-reference must
be corrected. The v2 spec is not reopened or treated as implementation truth.

## Navigation and State Boundaries

```text
Main graph unread coordinator
├── messageUnreadCount       ← GET /conversation
│   └── Profile → Messages badge
└── notificationUnreadCount  ← GET /notifications/unread-count
    └── Home → notification bell badge

Notifications row tap
├── PATCH /notifications/{id}/read (if unread)
└── mobile_action.type
    ├── conversation → typed Chat route
    └── unknown/null → remain in inbox

Visible Chat
├── first-page poll → merge by message ID
├── top reached → older-page cursor request
├── staff message observed → POST /conversation/messages/read
└── Search mode → independent query + cursor state
```

The unread coordinator may be a Main-graph-scoped ViewModel or an equivalently testable state owner.
It must not put Retrofit calls, cursor mutation, or notification-kind parsing directly in `NavGraph`.

## Code Style

Use Kotlinx Serialization DTOs, explicit repository mapping, safe unknown enum values, and sealed UI
state. A representative contract boundary:

```kotlin
@Serializable
data class MobileActionDto(
    val type: String,
    val id: Int? = null,
)

enum class MobileDestination {
    CONVERSATION,
    UNKNOWN;

    companion object {
        fun from(value: String?): MobileDestination = when (value) {
            "conversation" -> CONVERSATION
            else -> UNKNOWN
        }
    }
}

sealed interface NotificationListUiState {
    data object Loading : NotificationListUiState

    data class Success(
        val notifications: List<AppNotification>,
        val isLoadingMore: Boolean,
        val canLoadMore: Boolean,
        val mutationInFlight: Boolean,
        val inlineError: String?,
    ) : NotificationListUiState

    data class Error(val patientSafeMessage: String) : NotificationListUiState
}
```

Conventions:

- Kotlin properties use camelCase; wire fields use `@SerialName` snake_case.
- Domain names describe product meaning (`AppNotification`, `MobileDestination`), not Laravel
  implementation classes.
- Opaque cursors remain `String?` and are never decoded.
- DTO-to-domain mapping occurs once at the repository boundary.
- New presentation state uses `sealed interface` and `StateFlow`; no LiveData.
- Unknown external values fail closed without crashing or enabling navigation.
- User-visible errors are concise and patient-safe.

## Testing Strategy

### DTO and contract tests

- Decode message list/search responses with `next_cursor` and `has_more`.
- Reject or surface malformed required cursor metadata rather than silently treating it as terminal.
- Decode UUID notification IDs, `kind`, nullable `mobile_action`, page metadata, and unread count.
- Prove unknown notification kinds/actions map to safe `UNKNOWN` domain values.
- Prove legacy `type` and `action_url` fields are ignored for navigation.
- Prove `contexts` remains absent from serialized message requests and ignored on legacy responses.

### Retrofit and repository tests

- MockWebServer verifies `cursor` is omitted on page 1 and returned unchanged on later requests.
- Search later pages carry both `q` and `cursor`.
- Mark-read decodes the bare `{ "marked_count": n }` response.
- Notification mark-one uses the UUID path without coercion; mark-all uses the static route.
- All remote calls use `safeApiCall` and DTOs never escape the repository.

### ViewModel tests

- Initial newest-first page becomes one chronological timeline.
- Same-timestamp messages order deterministically by ID.
- Older pages merge without duplicates and cursor advances only on success.
- A 50-item polling refresh with unchanged size still detects a new message.
- Poll and send responses merge by ID and preserve already-loaded older history.
- Incoming staff messages trigger one idempotent mark-read attempt while visible; own messages do not.
- Send failure and HTTP 429 preserve draft/attachment and expose safe copy.
- Search latest-response-wins behavior, independent cursor state, empty/error/load-more states.
- Notification page dedupe, optimistic mark-one rollback/reconciliation, mark-all single flight, and
  action mapping.
- Message and notification unread counts remain independent.

### Compose tests

- Chat starts at the newest message while semantics/read order remain chronological.
- Loading older messages preserves the visible anchor and exposes top retry on failure.
- Search field validation, submitted search, empty/result/error states, and Back restoration.
- Notification bell badge visual cap and full accessibility count.
- Notification list loading/error/empty/populated states and non-color-only unread semantics.
- Unknown notification action is non-navigating; `conversation` invokes the Chat callback.
- Mark-all visibility and disabled/single-flight behavior.

### Route and regression tests

- `ApiRouteAllowlistTest` asserts 8 + 36 + 17 = 61 and all six v19 routes are account-only.
- Production Retrofit discovery contains no rejected legacy conversation routes.
- Existing auth, account-link, attachment gating, and profile tests remain green.
- `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug` pass before the
  implementation is considered complete.

No numeric coverage target is introduced; coverage is enforced through the behavior matrix above.

## Boundaries

### Always

- Correct and approve the wire contract before implementing its consumer.
- Treat every backend response as untrusted at the DTO boundary.
- Map DTOs to domain models at the repository boundary.
- Keep message and notification unread counts separate.
- Preserve message drafts and existing loaded data across recoverable failures.
- Use typed Navigation Compose routes for all mobile actions.
- Run `./gradlew assembleDebug` after every repository change and the full verification commands at
  the end of implementation.
- Keep API contract, route allowlist, `CONTEXT.md`, and tests synchronized in the same implementation
  task that changes the corresponding behavior.

### Ask first

- Adding any dependency.
- Changing backend behavior beyond C1–C4 or adding another mobile action type.
- Adding FCM, WorkManager, background polling, or another notification delivery channel.
- Adding Room/offline storage for messages or notifications.
- Changing the four bottom-navigation roots or moving Chat/Notifications outside the Main graph.
- Making search-result taps locate or scroll to a message without an API around-message contract.

### Never

- Use Gson; Kotlinx Serialization is mandatory.
- Store tokens, messages, notifications, attachments, or health data in Room.
- Apply `org.jetbrains.kotlin.android` under AGP 9.
- Decode, construct, or fabricate opaque cursors.
- Navigate from `action_url`, PHP notification class names, arbitrary URIs, or raw server strings.
- Show raw backend error messages, stack traces, validation internals, OTPs, or bearer tokens.
- Clear a draft or pending attachment on a failed send.
- Mark all notification rows read merely because Chat was opened.
- Infer sender ownership from list position; use authenticated account identity/sender type.

## Success Criteria

Each condition is independently testable.

1. `docs/API_CONTRACT.md` documents the cursor query/metadata, multipart send, stable notification
   fields, and in-place rating updates without the contradictions identified in C1–C4.
2. Route governance asserts exactly **8 public + 36 account-only + 17 active-link = 61 routes**.
3. All six v19 routes appear in the account-only route set; attachment download remains active-link.
4. Initial chat displays the newest 50 messages chronologically and lands at the newest message.
5. Loading an older page keeps the user's visible anchor stable, merges without duplicates, and
   advances only to the server-provided cursor.
6. Polling detects a changed fixed-size first page and retains previously loaded older messages.
7. Sending and polling merge messages by ID; equal timestamps use ID as deterministic tie-breaker.
8. Opening visible Chat and receiving a staff message mark received messages read idempotently.
9. The Profile Messages badge reflects `conversation.unread_count`, clears locally after successful
   mark-read, caps at `9+`, and exposes the full accessibility count.
10. Text and attachment drafts survive HTTP 422, 429, network, and server failures; a successful send
    clears only the submitted draft.
11. Search accepts 3–500 trimmed characters, supports cursor paging and latest-response-wins, and
    leaves conversation pages, scroll position, and draft unchanged when closed.
12. Home exposes a notification bell to linked and unlinked authenticated accounts, with a badge from
    `/notifications/unread-count` and full accessibility semantics.
13. Notifications supports page-based loading, refresh, empty/error/load-more states, mark-one, and
    mark-all without duplicate UUID rows.
14. A `conversation` mobile action navigates through typed `Chat`; unknown/null actions do not
    navigate, and `action_url` is never read for navigation.
15. Message unread and notification unread counts can differ without either overwriting the other.
16. DTO, repository, ViewModel, Compose, and route-governance tests cover the behavior matrix in the
    Testing Strategy.
17. `CONTEXT.md` records the 61-route contract and completed v19 behavior when implementation lands.
18. `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug` all pass.

## Out of Scope

- Push notifications, FCM token registration, notification channels, or system-tray delivery.
- Background synchronization, WorkManager, or polling while the app/Chat is not visible.
- Offline message or notification storage.
- Staff inbox archiving, restoration, clinic-wide read watermark, or Filament navigation badges.
- Conversation context cards or `contexts` input; that feature remains retired.
- Rich notification detail screens or actions other than `conversation`.
- Search-result-to-timeline positioning without a backend around-message endpoint.
- Changes to attachment formats, size limits, storage, or download authorization.
- Any rating UI/model changes beyond correcting stale API documentation.

## Resolved Decisions

1. **Search and notification inbox scope:** both ship in Android v19.
2. **Notification navigation contract:** use stable `kind` + typed `mobile_action`; Android ignores
   `action_url` and Laravel/PHP class names.
3. **Message presentation:** newest-first server pages are presented as one chronological timeline;
   older history loads upward.
4. **Notification entry point:** an account-only notification bell in the Home greeting header opens
   a typed Notifications sub-destination.
5. **Unread semantics:** message unread and notification unread are separate; only explicit
   notification interactions mark notification rows read.
6. **Search-result interaction:** results are self-contained in v19 and are not required to jump into
   the conversation timeline.

## Open Questions

None. After human approval, proceed to Phase 2 and create a separate technical implementation plan.

