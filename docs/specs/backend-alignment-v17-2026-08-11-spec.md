# Spec: Backend Alignment v17 — 2026-08-11 Contract Reconciliation

Status: Complete — 2026-08-11
Date: 2026-08-11

## Objective

Align the Android client with the reconciled 2026-08-11 patient API contract.
The revised working-tree documents now consistently retain patient-visible
appointment types, non-binding appointment requests, and the 55-route ledger.
They also establish a new account-owned conversation model: linked and unlinked
accounts may read and send text, structured contexts are retired, and
attachments remain protected by the active patient-link boundary.

This specification is intentionally limited to the Specify phase. No
production code, route allowlist, or existing spec is changed by this draft.

## Sources and precedence

The following are the inputs for this change:

1. `docs/API_CONTRACT.md` working-tree update dated 2026-08-11.
2. `docs/BACKEND_CONTEXT.md` working-tree update dated 2026-08-11.
3. Current Android production code and tests.
4. `CONTEXT.md` and the completed v16 appointment-request specification.

The two backend documents are user-owned, uncommitted inputs. The completed v16
specification remains the Android appointment-request baseline. The resolved
2026-08-11 contract takes precedence for conversation access, capabilities,
rate-limit errors, and route governance.

## Current Android impact

The audit found these concrete client dependencies:

| Area | Current Android behavior | Contract-sensitive impact |
|---|---|---|
| Appointment requests | Calls `GET /appointment-types`, requires `appointment_type_id`, and renders a Type step. | Already aligned. Keep the v16 flow, conditional referral input, alternatives, and linked/unlinked identity behavior. |
| Pending capacity | Presents pending requests as non-binding and says no time is held until confirmation. | Already aligned with the request resource and `time_preferences_are_reserved = false`. Availability remains server-authoritative regardless of the blocks it considers. |
| Conversation access | Classifies Chat as active-link-only. | Move conversation read/send to account-only so unlinked accounts can contact the clinic. Attachment download remains active-link-only. |
| Conversation payload | Sends optional `contexts` and multipart file messages; renders context cards and attachment controls. | Remove context creation, transport, mapping, cards, and navigation. Keep linked-only attachment upload, but gate it with the server capability. |
| Conversation resource | Maps only `id`, `patient_id`, `unread_count`, and `created_at`. | The new response adds `access_level` and capability flags that are needed to gate attachment actions. Unknown fields currently do not break decoding, but they are not consumed. |
| Invitation acceptance | Uses the original bearer token, accepts the status, then refreshes `/me` to promote the session. | The additive `token` and `user` response fields are safely ignored today because the contract says the original token remains valid. A new token should not replace the stored token unless that behavior is explicitly required. |
| Rate limits | Preserves machine-readable body codes, but most features fall back to generic 429 copy and do not expose `Retry-After`. | New stable codes require a deliberate minimum UX and retry policy; automatic retry remains prohibited. |

Internal optical catalog, dispensing, lens-option, and Filament changes do not
affect Android unless a patient API route or response shape is explicitly
added.

## Resolved contract decisions

### 1. Appointment-type selection remains patient-visible

Keep `GET /appointment-types`, the Type wizard step, required
`appointment_type_id` availability/request fields, type-derived duration, and
conditional referral source. No appointment-request redesign belongs in v17.

### 2. Conversation contract

Conversation ownership and text messaging are account-only. `GET
/conversation`, `GET /conversation/messages`, and text `POST
/conversation/messages` are available to linked and unlinked accounts.

`contexts[]` is retired and must never be sent. Existing context picker UI,
outbound context requests, inbound context mapping, context cards, and context
navigation are removed. Old server messages without a `contexts` field remain
decodable.

Attachment upload remains available only when
`capabilities.can_upload_attachments` is true. Attachment download remains an
active-link route. The stale context lines in the retained multipart example do
not override the explicit prohibition on `contexts`.

### 3. Appointment requests remain non-binding

Keep `time_preferences_are_reserved = false` and the existing no-time-held
copy. Android does not infer capacity from pending requests. Both request and
confirmed-appointment availability render only server-returned slots, so any
legacy or internal schedule blocks require no client-side model.

### 4. Route ledger and rate-limit policy

Use 55 registered routes: 8 public, 29 account-only, and 18 active-link. The
legacy job-order-item rating alias remains registered but cannot be called by
production Android. Conversation read/list/send belong to the account-only
tier; attachment download remains active-link.

Preserve `OTP_RATE_LIMIT_REACHED`, add
`INVITATION_RATE_LIMIT_REACHED` and `API_RATE_LIMIT_REACHED`, and show
patient-safe retry-later copy for HTTP 429. Do not automatically retry
non-idempotent requests. Displaying a `Retry-After` countdown is not required
for this alignment.

## Proposed Android scope

Phase 2 should plan these bounded changes:

- **Appointment branch:** retain the v16 request flow. Preserve the linked
  identity/session fixes already present in the worktree; do not redesign the
  Type or Schedule steps.
- **Conversation branch:** classify Chat and conversation read/list/send as
  account-only, consume conversation capabilities, remove context creation and
  navigation, send text without `contexts`, and expose attachment actions only
  when the server allows them. Attachment download remains link-protected.
- **Error branch:** add stable rate-limit constants and feature-specific
  patient-safe messages; preserve `ApiDomainError` codes and prohibit automatic
  retry loops.
- **Invitation branch:** retain the existing original-token + `/me` refresh
  flow unless backend behavior changes again; add response/flow regression
  coverage only if the fresh token or returned user must be consumed.
- **Governance branch:** reconcile `ApprovedApiRoutes`, route-access policy, and
  tests with the 55-route ledger and split conversation access.

## Commands

Run from the repository root with Android Studio's JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
.\gradlew ktlintCheck
```

## Project structure and code style

Likely implementation boundaries are:

- `app/src/main/java/com/eyecare/app/data/remote/api/` for route and payload
  changes;
- `app/src/main/java/com/eyecare/app/data/remote/dto/` for Kotlinx
  Serialization wire models;
- `app/src/main/java/com/eyecare/app/data/repository/` for DTO-to-domain and
  HTTP-error mapping;
- `app/src/main/java/com/eyecare/app/domain/` for serialization-free contracts
  and access/capability policy;
- `app/src/main/java/com/eyecare/app/presentation/` for navigation, request,
  messaging, and error UX; and
- `app/src/test/java/com/eyecare/app/` for route, DTO, repository, policy, and
  ViewModel regression coverage.

Preserve MVVM + Clean boundaries, StateFlow state, type-safe navigation,
Kotlinx Serialization, DTO-to-domain mapping at the repository boundary, and
the existing no-Gson/no-Room-health-data constraints.

## Testing strategy

Phase 2 must plan focused failing tests for:

- the unchanged appointment request payload/query and non-binding wording;
- linked and unlinked request identity behavior;
- Chat access for linked and unlinked sessions;
- conversation capability decoding and attachment gating;
- absence of retired context fields, context UI, and context navigation;
- linked-only multipart attachment upload without any context fields;
- stable 429 code mapping and no automatic retry;
- invitation acceptance followed by `/me` returning `link_status: linked`; and
- the canonical route count and Retrofit annotation allowlist.

The full unit suite, `assembleDebug`, `lintDebug`, and `ktlintCheck` are required
after implementation. No test should be weakened or deleted to accommodate a
contract mismatch.

## Boundaries

- Do not edit or silently reconcile the user-owned backend documents as part
  of Android implementation.
- Do not remove appointment-type selection or change non-binding request copy.
- Do not send or render structured message contexts.
- Do not expose attachment controls when the conversation capability denies
  uploads or the session lacks an active patient link.
- Do not automatically retry `POST /appointment-requests`, invitation
  acceptance, or message sends after an ambiguous failure.
- Do not expose raw error JSON, tokens, health data, or attachment contents in
  logs.
- Do not add dependencies, storage, or backend routes without a separate
  approved decision.

## Success criteria

- The Android API surface and route governance match the 55-route contract.
- Appointment type selection and non-binding request behavior remain unchanged.
- Linked/unlinked access and appointment identity behavior remain consistent
  with `/me` link status.
- Linked and unlinked accounts can read and send conversation text.
- Conversation UI cannot send or render retired structured contexts.
- Attachment upload is offered only when the conversation capability and active
  link allow it; attachment download remains link-protected.
- Rate-limit responses remain patient-safe and do not trigger retry loops.
- Focused tests, the full unit suite, debug build, lint, and formatting checks
  pass.

## Open questions

None for Android Phase 2. The backend documents should still remove their stale
multipart `contexts` example and move the three conversation read/send entries
under the account-only route heading, but the repeated access, capability, and
55-route decisions are sufficient to plan the client alignment.
