# Plan: Backend Alignment v17 — Account-Owned Conversation

Status: Complete — 2026-08-11
Date: 2026-08-11
Spec: `docs/specs/backend-alignment-v17-2026-08-11-spec.md`

## Overview

Align the Android messaging surface and API governance with the approved v17
contract while preserving the completed v16 appointment-request behavior and
the linked-account identity/session fix already present in the worktree.

The implementation will make conversation text available to every authenticated
account, remove the retired structured-context feature end to end, and derive
attachment behavior from the conversation resource returned by the server.
Uploads require both a `linked_patient` access level and
`can_upload_attachments = true`; attachment downloads remain active-link-only.
Rate-limit failures will keep their machine-readable codes and produce safe,
feature-specific retry-later copy without automatically repeating a send.

No backend document, appointment flow, dependency, persistence schema, or
backend route is changed by this plan.

## Architecture Decisions

1. **The conversation resource is the messaging authority.** Extend the wire
   and domain conversation models with a typed access level and capabilities.
   The UI must not infer attachment permission from `SessionState`,
   `patient_id`, or a locally cached profile. Unknown/missing values fail closed.
2. **Text and attachment permissions are separate.** Chat itself and
   conversation read/list/text-send routes are account-only. Upload controls are
   exposed only for `linked_patient` conversations whose capability explicitly
   permits upload. Image/download requests are attempted only for
   `linked_patient` conversations.
3. **Structured contexts are removed at every layer.** Delete the context DTO,
   domain type, repository argument/mapping, picker state, picker repositories,
   context composables, and context navigation callbacks. A message payload has
   only `body`; old response fields are ignored by Kotlinx Serialization rather
   than mapped or rendered.
4. **The remaining attachment action is direct.** Remove the multi-action
   attachment sheet. When upload is allowed, the add button launches the system
   document picker directly; when it is denied, no upload affordance is shown.
5. **Repository failures use the shared error boundary.** Conversation calls
   use `safeApiCall` so JSON error codes survive as `ApiDomainError`. Explicitly
   validate non-successful streamed download responses before reading a body.
6. **Send failures remain recoverable and non-retrying.** Chat state exposes a
   patient-safe send error, keeps unsent text or restores the pending attachment,
   and clears the error on the next deliberate patient action. Message sends and
   invitation acceptance are never automatically retried.
7. **Route governance represents both server and client totals.** The ledger
   contains 8 public, 29 account-only, and 17 canonical active-link routes; the
   one server-side legacy alias produces 18 registered active-link routes and 55
   registered routes overall. Android may call 54 canonical routes and never the
   alias.
8. **Invitation handling remains token-stable.** The existing acceptance flow
   continues to use the original token and refresh `/me`. A regression fixture
   proves additive response `token`/`user` fields are safely ignored; no token
   replacement path is introduced.
9. **Appointment behavior is a regression boundary.** Type selection,
   type-derived duration, required `appointment_type_id`, non-binding preference
   copy, and linked/unlinked identity rules stay unchanged.

## Components and File Boundaries

| Component | Primary files | Planned responsibility |
|---|---|---|
| Conversation wire contract | `data/remote/dto/MessageDtos.kt`, `data/remote/api/ConversationApiService.kt` | Decode `access_level` and capabilities, make text requests body-only, retain one-file multipart upload and protected download endpoint |
| Conversation domain/repository | `domain/model/Message.kt`, `domain/repository/ChatRepository.kt`, `data/repository/ChatRepositoryImpl.kt` | Add typed access/capabilities, remove all context types and mappings, map DTOs at the repository boundary, preserve structured API failures |
| Messaging state | `presentation/messaging/ChatViewModel.kt` | Remove quotation/order dependencies and picker state, gate uploads from conversation policy, surface safe send errors, preserve failed drafts/attachments |
| Messaging UI | `presentation/messaging/ChatScreen.kt`, `presentation/messaging/components/MessageBubble.kt`, `AttachmentPreview.kt` | Allow account-wide text chat, launch attachment picker only when permitted, avoid protected downloads for general inquiry conversations, render no context cards |
| Retired UI | `AttachmentSheet.kt`, `ContextCard.kt`, `MessageContextCard.kt` | Remove obsolete context-link and multi-action picker components after their consumers are gone |
| Navigation policy | `presentation/navigation/PatientRouteAccess.kt`, `PatientFeatureIntent.kt`, `NavGraph.kt` | Classify Chat as account-only, navigate directly from Profile, remove context-detail callbacks, retain active-link redirects for clinical destinations |
| Error policy | `domain/model/ApiDomainError.kt`, messaging/auth/account/request error mappers | Add stable invitation/general API rate-limit constants and map all HTTP 429 variants to patient-safe copy without retrying writes |
| Route governance | `test/.../data/remote/ApprovedApiRoutes.kt`, `ApiRouteAllowlistTest.kt` | Move conversation read/list/send to account-only, keep attachment download active-link, verify 55 registered/54 canonical totals and Retrofit coverage |
| Contract regressions | Existing messaging, navigation, DTO, repository, auth/account, appointment-request, and invitation tests | Replace retired context assertions with body-only/capability/access tests and preserve the approved appointment/session behavior |
| Project context | `CONTEXT.md`, v17 spec/plan/tasks | Record the shipped Android contract only after all quality gates pass |

## Components and Dependencies

```text
Approved v17 contract
    |
    +-- Route ledger and navigation access policy
    |
    +-- Conversation DTOs and body-only request
            |
            +-- Typed domain access/capabilities
            |       |
            |       +-- Repository mapping and ApiDomainError preservation
            |               |
            |               +-- Capability-driven ChatViewModel state
            |                       |
            |                       +-- Chat composer and message rendering
            |                               |
            |                               +-- Retired context file removal
            |
            +-- Legacy-response and multipart request regressions
    |
    +-- Stable rate-limit codes and invitation compatibility fixture
            |
            +-- Full appointment/session regression and final context update
```

The high-risk chain is DTO/domain → repository → ViewModel → Compose. UI removal
must follow the tested body-only repository contract, and obsolete component
files must remain until all callers have been removed in the same compiling
increment.

## Implementation Order

### Stage 1 — Lock route and transport contracts

1. Update route-governance fixtures and route-access tests for 29 account-only
   routes, 17 canonical active-link routes, one active-link legacy alias, and
   account-only Chat navigation.
2. Add failing DTO/repository contract tests for general-inquiry and
   linked-patient conversations, missing/unknown capability values, messages
   without contexts, body-only text sends, context-free multipart upload, and
   decoded HTTP 429 errors.
3. Implement the conversation DTO/domain/repository foundation and stable
   rate-limit constants required by those tests.

At this checkpoint, transport and access policy match the approved contract
without requiring the Compose layer to guess permissions.

### Stage 2 — Rebuild messaging state around capabilities

4. Remove context-link and catalog-picker state from `ChatViewModel`, including
   its quotation and optical-order repository dependencies.
5. Gate attachment selection/send through the conversation access level and
   upload capability; ensure denied calls are no-ops with testable state and
   never reach the repository.
6. Add explicit load/send/upload error state and patient-safe 429 mapping.
   Preserve unsent content on failure and make each send single-flight with no
   automatic write retry.

At this checkpoint, linked and unlinked conversation behavior is proven in unit
tests before presentation wiring changes.

### Stage 3 — Deliver the account-owned conversation UI

7. Update `ChatScreen` to keep text messaging available for all authenticated
   accounts, show the add button only when upload is allowed, and launch the
   document picker directly.
8. Update `MessageBubble` to remove context callbacks/cards and prevent
   general-inquiry conversations from issuing protected image/download requests;
   retain safe attachment metadata where useful.
9. Remove the now-unreferenced attachment sheet and context-card components,
   then convert the stale Compose test to cover plain body, attachment, and
   download-gating rendering.

The UI consumes only already-tested state. It cannot construct `contexts[]` or
navigate from a message to an active-link optical resource.

### Stage 4 — Complete navigation, errors, and compatibility coverage

10. Wire Profile directly to the account-only Chat route, remove Chat from the
    protected feature-intent mechanism, and remove estimate/order callbacks from
    the Chat destination while retaining the limited-account backstop for all
    true active-link routes.
11. Apply the new stable rate-limit codes to authentication, invitation/linking,
    messaging, and appointment-request error mappers; add the additive invitation
    response fixture while preserving the original-token + `/me` refresh flow.
12. Run the appointment identity/session/request regression matrix and audit the
    source tree to prove no production context DTOs, mappings, picker actions,
    cards, or callbacks remain.

### Stage 5 — Verify and document

13. Run focused unit tests after each affected slice, then the full unit suite,
    debug build, lint, formatting check, and whitespace validation.
14. Manually verify linked and unlinked conversation behavior and update
    `CONTEXT.md` plus v17 status documents only after every required gate passes.

## Verification Checkpoints

### Checkpoint A — Contract foundation (after Stage 1)

- Route tests report 8 public, 29 account-only, 17 canonical active-link, 54
  canonical Android-callable, and 55 registered routes including the alias.
- `GET /conversation`, `GET /conversation/messages`, and text `POST
  /conversation/messages` are in the account-only set; attachment download is
  active-link-only.
- DTO fixtures decode both documented access levels and fail closed for missing
  or unknown capability data.
- Recorded text and multipart requests contain no `contexts` key or part.
- Focused DTO/repository/navigation tests and `assembleDebug` pass.

### Checkpoint B — Messaging state (after Stage 2)

- Linked-patient conversations with upload capability can select and send one
  validated attachment.
- General-inquiry, unknown, and capability-denied conversations cannot invoke
  upload even if a caller attempts it programmatically.
- Linked and unlinked accounts can send trimmed, nonblank text.
- HTTP 429 and stable rate-limit codes show retry-later copy, preserve pending
  content, and invoke each non-idempotent repository method exactly once.
- Focused ViewModel/error tests and `assembleDebug` pass.

### Checkpoint C — Presentation and navigation (after Stages 3–4)

- An unlinked account opens Chat without a Limited Account redirect and sees a
  text composer with no upload control.
- A linked conversation sees the upload control only when the server capability
  permits it.
- General-inquiry message rendering does not fetch the protected attachment
  route; linked-patient rendering can display allowed attachments.
- No context picker, context preview, context card, or optical-detail callback
  remains in the messaging/navigation source.
- Compose compilation, focused UI/navigation tests, and `assembleDebug` pass.

### Checkpoint D — Complete (after Stage 5)

- Appointment type selection, non-binding request copy, linked identity
  omission, and unlinked identity inclusion remain green.
- Invitation acceptance tolerates additive response fields and still refreshes
  `/me` with the original token.
- Full `testDebugUnitTest`, `assembleDebug`, `lintDebug`, and `ktlintCheck` pass.
- `git diff --check` passes and only approved v17 files plus pre-existing
  worktree changes are present.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Missing or unknown capability values accidentally enable upload | High: protected operation exposed | Nullable/default-false DTOs, typed unknown access level, and fail-closed domain policy tests |
| Unlinked Chat still invokes quotation/order APIs | High: active-link failures and data-boundary leak | Remove picker repositories and all context-loading methods from the ViewModel constructor and tests |
| Retired contexts remain in one outbound path | High: backend 422 | Body-only request type, multipart request inspection, and source audit for context symbols/parts |
| Existing messages contain legacy `contexts` fields | Medium: decoding failure | Rely on project-wide `ignoreUnknownKeys`; fixture proves legacy extra fields are ignored while body/attachments map |
| Image rendering automatically calls a protected download route for unlinked accounts | High: repeated 403s and misleading UI | Pass conversation download permission into message rendering and never build a protected image request when access is not `linked_patient` |
| Capability and cached session disagree | High: repeat of the linked-state mismatch | Treat the freshly fetched conversation resource as messaging authority; do not gate with the session wrapper |
| Text is cleared before a failed send completes | Medium: patient loses their message | Keep composer content until success or restore it through explicit ViewModel/UI state; test failure recovery |
| Multipart errors remain raw exceptions | Medium: generic or unsafe copy | Route all Retrofit calls through `safeApiCall` and explicitly reject non-2xx streamed responses |
| Route counts confuse canonical routes with the legacy alias | Medium: governance drift | Assert canonical and registered totals separately and prohibit the alias in Retrofit annotations |
| Broad context removal collides with unrelated dirty files | Medium: user changes overwritten | Use narrow patches, inspect each overlapping diff, and never reset or reformat unrelated files |
| Backend docs retain stale context examples | Medium: feature accidentally reintroduced | Follow the approved v17 precedence decision and keep backend documents read-only in Android implementation |

## Parallel vs Sequential Work

Must be sequential:

- DTO/domain shape before repository implementation.
- Repository capability/error behavior before ViewModel state.
- ViewModel state before Compose consumers.
- Consumer removal before deleting retired context component files.
- Final context/status updates after full verification.

Safe to parallelize after Stage 1 contracts are stable:

- Route-governance assertions and invitation additive-field fixture.
- Stable error-code constants/mappers and conversation DTO fixtures, provided
  ownership of shared error files is explicit.
- Navigation policy and message-bubble UI tests, once the domain model is fixed.

`ChatViewModel.kt`, `ChatScreen.kt`, `Message.kt`, and `NavGraph.kt` are shared,
high-conflict files and should have a single owner or be changed sequentially.

## Scope Guardrails

- Do not alter appointment Type, Schedule, Details, Review, request payload, or
  non-binding availability semantics.
- Do not edit `docs/API_CONTRACT.md` or `docs/BACKEND_CONTEXT.md`.
- Do not retain a hidden or dormant structured-context creation path.
- Do not expose upload or download operations from missing, unknown, or denied
  conversation permissions.
- Do not replace the stored bearer token from invitation-acceptance response
  fields under this contract.
- Do not add message persistence, background workers, WebSockets, pagination,
  multi-file upload, or new dependencies.
- Do not redesign the broader navigation graph; only reclassify the existing
  Chat destination and remove retired callbacks.
- Do not automatically retry message sends, appointment requests, invitation
  acceptance, or other non-idempotent writes.
- Do not log raw API bodies, tokens, OTP values, health data, or attachment
  contents.

## Open Questions

None. The approved specification resolves the stale backend examples by giving
precedence to the explicit account-owned conversation and prohibited-context
statements.

## Plan Review Checklist

- [x] Major components and file boundaries are identified.
- [x] High-risk foundations precede presentation work.
- [x] Access, capability, and error decisions fail closed.
- [x] Risks have concrete mitigations.
- [x] Sequential and parallel boundaries are explicit.
- [x] Verification checkpoints occur every two to three implementation slices.
- [x] Human has reviewed and approved this Phase 2 plan — 2026-08-11.
