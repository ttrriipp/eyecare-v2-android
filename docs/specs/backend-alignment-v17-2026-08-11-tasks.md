# Implementation Plan: Backend Alignment v17 — Account-Owned Conversation

Status: Complete — 2026-08-11
Date: 2026-08-11
Spec: `docs/specs/backend-alignment-v17-2026-08-11-spec.md`
Technical plan: `docs/specs/backend-alignment-v17-2026-08-11-plan.md`

## Overview

This breakdown delivers the approved v17 Android alignment in 12
dependency-ordered tasks. Each task is limited to five or fewer likely files,
has explicit acceptance criteria and verification, and must leave the project
buildable. During Phase 4, every behavior change begins with a failing focused
test or contract assertion before production code is changed.

Appointment-type selection, non-binding appointment requests, and the existing
linked/unlinked identity fix are regression boundaries, not redesign work.

## Architecture Decisions

- Conversation text belongs to the authenticated account; attachment authority
  comes from the freshly loaded conversation access level and capabilities.
- Missing or unknown access/capability values fail closed for upload and
  download while text remains account-available.
- Structured contexts are removed from creation, rendering, navigation,
  transport, domain, and repository layers in buildable increments.
- Conversation Retrofit failures cross the shared `safeApiCall` boundary and
  remain `ApiDomainError` values for patient-safe presentation.
- The route ledger distinguishes 54 canonical Android-callable routes from 55
  server-registered routes including the legacy alias.
- Invitation acceptance keeps the original token and refreshes `/me`; additive
  response token/user fields are ignored under the approved contract.
- No task edits the user-owned backend contract documents.

## Task List

### Phase 1: Contract and governance foundation

## Task 1: Reclassify the conversation route ledger

**Description:** Move conversation read, list, and send routes into the
account-only governance set while retaining attachment download in the
active-link set and preserving separate canonical/registered totals.

**Acceptance criteria:**

- [ ] Governance contains 8 public, 29 account-only, and 17 canonical
      active-link routes, plus one active-link legacy alias.
- [ ] The canonical Android-callable total is 54 and the registered backend
      total is 55.
- [ ] Retrofit source discovery accepts the three account-only conversation
      routes, retains protected attachment download, and still rejects the
      legacy alias in production annotations.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*ApiRouteAllowlistTest"`
- [ ] Build succeeds: `./gradlew assembleDebug`

**Dependencies:** None

**Files likely touched:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** Small (2 files)

---

## Task 2: Add conversation access and capability contracts

**Description:** Decode the documented `general_inquiry` and `linked_patient`
conversation variants, map them to serialization-free domain policy, and make
the JSON send request body-only. The temporary repository context argument may
remain during this task solely to keep existing callers compiling, but it must
be ignored by serialization and is removed after those callers are retired.

**Acceptance criteria:**

- [ ] Conversation DTO/domain models expose typed access level and both
      capability flags; missing or unknown values map to fail-closed policy.
- [ ] Upload permission is true only for a `linked_patient` conversation whose
      `can_upload_attachments` capability is true.
- [ ] Serializing `SendMessageRequest` can produce only `body`; no `contexts`
      key can appear in a JSON request.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*MessageDtosTest" --tests "*ChatRepositoryMappingsTest"`
- [ ] Fixtures cover linked, unlinked, missing, and unknown access/capability
      values.
- [ ] Build succeeds: `./gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/MessageDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Message.kt`
- `app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/MessageDtosTest.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ChatRepositoryMappingsTest.kt`

**Estimated scope:** Medium (5 files)

### Checkpoint: Route and capability contract

- [ ] Route governance reports 55 registered and 54 canonical routes.
- [ ] Conversation capability fixtures and fail-closed policy tests pass.
- [ ] JSON text requests cannot serialize structured contexts.
- [ ] Debug build succeeds.

### Phase 2: Capability-driven messaging slice

## Task 3: Remove context creation from the chat composer

**Description:** Rebuild chat state and the composer around text plus one
optional permitted attachment. Remove quotation/order repositories, picker
state, context send methods, and the multi-action attachment sheet; the one
remaining add action launches the system document picker directly.

**Acceptance criteria:**

- [ ] `ChatViewModel` has no quotation/order dependencies, pending-context
      state, context send method, or picker-data request.
- [ ] General-inquiry, unknown, and capability-denied conversations cannot set
      or send an attachment, even through direct ViewModel calls.
- [ ] Linked-patient conversations with upload capability can select one valid
      attachment; text messaging remains available in every success state.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*ChatViewModelTest" --tests "*AttachmentValidatorTest"`
- [ ] Source check: `rg -n "PendingContext|sendContextMessage|loadPickerData|QuotationRepository|OpticalOrderRepository" app/src/main/java/com/eyecare/app/presentation/messaging`
      returns no production messaging match.
- [ ] Build succeeds: `./gradlew assembleDebug`

**Dependencies:** Task 2

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/AttachmentSheet.kt` (remove)
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/ContextCard.kt` (remove)

**Estimated scope:** Medium (5 files)

---

## Task 4: Remove context rendering and gate protected downloads

**Description:** Simplify message bubbles to body and attachments only. Pass
conversation download permission into rendering so general-inquiry messages
never construct an active-link attachment request; convert the stale context
Compose test into current plain-message and attachment regressions.

**Acceptance criteria:**

- [ ] Message bubbles expose no context-card rendering or optical-resource
      callback.
- [ ] General-inquiry and unknown conversations render safe attachment metadata
      without initiating the protected image/download route.
- [ ] Linked-patient conversations may render permitted image attachments, and
      plain text/body ownership rendering remains unchanged.

**Verification:**

- [ ] Android test sources compile: `./gradlew compileDebugAndroidTestKotlin`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Manual check: an unlinked attachment message does not issue a request to
      `/conversation/attachments/{id}`.

**Dependencies:** Task 3

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageBubble.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/components/MessageContextCard.kt` (remove)
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/messaging/components/MessageBubbleTest.kt`

**Estimated scope:** Medium (4 files)

---

## Task 5: Make Chat an account-only destination

**Description:** Reclassify the existing Chat destination, navigate to it
directly from Profile, and remove obsolete estimate/order callback wiring while
retaining the limited-account backstop for actual clinical destinations.

**Acceptance criteria:**

- [ ] Linked, unlinked, and pending-review accounts can access `Chat` without a
      Limited Account redirect.
- [ ] Profile navigation opens Chat directly and the Chat destination has no
      estimate/order callback wiring.
- [ ] Unknown and clinical routes continue to fail closed as active-link
      destinations.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*PatientRouteAccessTest"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Manual check: unlinked Profile → Messages opens the text composer while
      Prescriptions still redirects to account linking.

**Dependencies:** Tasks 3 and 4

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientRouteAccess.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientRouteAccessTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`

**Estimated scope:** Medium (4 files)

---

## Task 6: Remove the obsolete protected Chat intent

**Description:** Delete Chat from the deferred active-link feature-intent model
now that navigation treats it as an account-safe destination, while preserving
argument mappings for all remaining protected intents.

**Acceptance criteria:**

- [ ] `PatientFeatureIntent` no longer contains, labels, or maps Chat.
- [ ] Remaining feature-intent route arguments round-trip unchanged.
- [ ] No production navigation call attempts to defer Chat through the account
      linking hub.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*PatientFeatureIntentTest" --tests "*PatientRouteAccessTest"`
- [ ] Source check: `rg -n "PatientFeatureIntent\.Chat|navigatePatientFeature\(Chat" app/src/main app/src/test`
      returns no match.
- [ ] Build succeeds: `./gradlew assembleDebug`

**Dependencies:** Task 5

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/PatientFeatureIntent.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/PatientFeatureIntentTest.kt`

**Estimated scope:** Small (2 files)

### Checkpoint: Account-owned conversation UI

- [ ] Unlinked accounts can open Chat and send text.
- [ ] Upload controls appear only for a capable linked-patient conversation.
- [ ] General-inquiry rendering does not call the protected attachment route.
- [ ] No context picker, preview, card, or optical navigation remains.
- [ ] Focused unit tests, Android test compilation, and debug build pass.

### Phase 3: Retired transport and resilient failures

## Task 7: Remove structured contexts from transport and domain

**Description:** Once all presentation callers are gone, delete the remaining
context DTO/domain types, message fields, repository argument, and conversion
logic so production Android cannot construct, map, or render `contexts[]`.

**Acceptance criteria:**

- [ ] `SendMessageRequest` and `ChatRepository.sendMessage` accept body only.
- [ ] `MessageDto` and `Message` contain no context collection, and extra legacy
      response `contexts` fields remain decodable through `ignoreUnknownKeys`.
- [ ] No context DTO, domain variant, mapper, parameter, or production symbol
      remains.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*MessageDtosTest" --tests "*ChatRepositoryMappingsTest" --tests "*ChatViewModelTest"`
- [ ] Source check: `rg -n "MessageContext|ContextLinkDto|contexts" app/src/main/java/com/eyecare/app/data/remote/dto/MessageDtos.kt app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt app/src/main/java/com/eyecare/app/domain app/src/main/java/com/eyecare/app/presentation/messaging`
      returns no match.
- [ ] Build succeeds: `./gradlew assembleDebug`

**Dependencies:** Tasks 3, 4, and 6

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/MessageDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/Message.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/ChatRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt`

**Estimated scope:** Medium (4 files)

---

## Task 8: Preserve conversation HTTP errors and exact multipart requests

**Description:** Move every conversation repository operation through the
shared HTTP error boundary, validate streamed download responses before reading
them, and prove exact JSON/multipart requests with MockWebServer-style tests.

**Acceptance criteria:**

- [ ] JSON text send and one-file multipart upload contain no context field or
      part and use the documented singular `attachment` field.
- [ ] HTTP failures, including 429, reach callers as `ApiDomainError` with
      status, code, message, and field errors preserved.
- [ ] A non-successful attachment download is decoded as an API failure rather
      than read as successful bytes; temporary upload files are still deleted.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*ChatRepositoryImplTest" --tests "*RepositoryApiCallTest"`
- [ ] Inspect recorded request assertions for exact content type, body, and part
      names.
- [ ] Build succeeds: `./gradlew assembleDebug`

**Dependencies:** Task 7

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/api/ConversationApiService.kt`
- `app/src/main/java/com/eyecare/app/data/repository/ChatRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/ChatRepositoryImplTest.kt` (new)
- `app/src/test/java/com/eyecare/app/data/repository/RepositoryApiCallTest.kt`

**Estimated scope:** Medium (4 files)

---

## Task 9: Make chat send failures patient-safe and recoverable

**Description:** Add explicit composer/send error state and safe error mapping.
Text remains until a confirmed success, failed attachments are restored, and
each deliberate send invokes the repository exactly once without an automatic
write retry.

**Acceptance criteria:**

- [ ] A successful text send clears the draft and appends the returned message;
      a failed send preserves the draft and shows safe actionable copy.
- [ ] A failed attachment upload restores the pending attachment and shows safe
      error copy; a capability change cannot replay it.
- [ ] HTTP 429 and stable rate-limit codes show retry-later copy, and rapid
      duplicate actions remain single-flight with one repository invocation.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*ChatViewModelTest" --tests "*ChatErrorMessagesTest"`
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Manual check: force a send failure and confirm the draft remains editable
      and is not resent automatically.

**Dependencies:** Task 8

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatErrorMessages.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatErrorMessagesTest.kt` (new)
- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatScreen.kt`

**Estimated scope:** Medium (5 files)

### Checkpoint: Transport and failure behavior

- [ ] No production context symbol or request field remains.
- [ ] Exact JSON/multipart contract tests pass.
- [ ] Conversation failures preserve `ApiDomainError` details.
- [ ] Failed sends preserve patient input and are never automatically retried.
- [ ] Focused tests and debug build pass.

### Phase 4: Cross-feature rate limits and regressions

## Task 10: Add stable rate-limit codes to auth and linking

**Description:** Add the documented invitation and general API rate-limit codes
to the central code registry and map them consistently in authentication and
account-linking flows without exposing backend text or adding retry loops.

**Acceptance criteria:**

- [ ] `INVITATION_RATE_LIMIT_REACHED` and `API_RATE_LIMIT_REACHED` are stable
      constants alongside the existing OTP rate-limit code.
- [ ] Auth and invitation/linking surfaces map all documented HTTP 429 variants
      to patient-safe retry-later copy.
- [ ] Invitation OTP/acceptance actions remain single-flight and are not
      automatically repeated after a rate-limit response.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*AuthErrorMessagesTest" --tests "*LimitedAccountViewModelTest"`
- [ ] Build succeeds: `./gradlew assembleDebug`

**Dependencies:** Task 9

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/domain/model/ApiDomainError.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/AuthErrorMessages.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/AuthErrorMessagesTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/account/LimitedAccountViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/LimitedAccountViewModelTest.kt`

**Estimated scope:** Medium (5 files)

---

## Task 11: Lock appointment and invitation compatibility regressions

**Description:** Extend patient-safe appointment-request rate-limit coverage and
prove invitation acceptance tolerates additive token/user fields while still
refreshing `/me` with the existing authenticated session. Do not change the
appointment wizard or replace the stored bearer token.

**Acceptance criteria:**

- [ ] Appointment-request HTTP 429 and `API_RATE_LIMIT_REACHED` map to safe
      retry-later copy while all existing identity/link errors remain specific.
- [ ] Invitation acceptance decodes a response containing additive `token` and
      `user` fields without requiring those fields in the Android DTO.
- [ ] A successful invitation acceptance calls `/me` once, promotes only a
      freshly linked account, and does not call acceptance more than once.

**Verification:**

- [ ] Tests first fail, then pass: `./gradlew testDebugUnitTest --tests "*AppointmentRequestErrorMessagesTest" --tests "*AccountDtosTest" --tests "*LimitedAccountViewModelTest"`
- [ ] Existing linked/unlinked request tests pass: `./gradlew testDebugUnitTest --tests "*AppointmentRequestIdentityTest" --tests "*RequestAppointmentViewModelTest"`
- [ ] Build succeeds: `./gradlew assembleDebug`

**Dependencies:** Task 10

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestErrorMessages.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/AppointmentRequestErrorMessagesTest.kt` (new)
- `app/src/test/java/com/eyecare/app/data/remote/dto/AccountDtosTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/LimitedAccountViewModelTest.kt`

**Estimated scope:** Medium (4 files)

### Checkpoint: Cross-feature contract regression

- [ ] Auth, invitation, messaging, and appointment 429 mappings are safe.
- [ ] No non-idempotent operation is automatically retried.
- [ ] Invitation acceptance retains the original-token + `/me` behavior.
- [ ] Appointment type, non-binding preferences, and identity tests remain green.
- [ ] Focused tests and debug build pass.

### Phase 5: Final verification and living context

## Task 12: Run the full matrix and reconcile Android context

**Description:** Run all automated and manual gates, audit the source tree for
retired behavior, and update Android-owned context/workflow status only after
the implementation is proven. Preserve every unrelated dirty worktree change.

**Acceptance criteria:**

- [ ] `CONTEXT.md` records account-owned text conversation, capability-gated
      attachments, retired contexts, stable rate limits, and 55/54 route totals.
- [ ] Spec, technical plan, and task status accurately reflect shipped behavior
      and actual verification; no checkbox is marked before its gate passes.
- [ ] Backend documents remain untouched by Android implementation and no
      unrelated user change is overwritten or reformatted.

**Verification:**

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew lintDebug`
- [ ] `./gradlew ktlintCheck`
- [ ] `./gradlew compileDebugAndroidTestKotlin`
- [ ] `git diff --check`
- [ ] Source audit: no production `MessageContext`, `ContextLinkDto`, context
      picker/card, or Chat protected-intent symbol remains.
- [ ] Manual matrix: linked/unlinked Chat access; allowed/denied upload;
      allowed/denied image download; text success/422/429; invitation acceptance;
      linked four-step request without identity; unlinked five-step request with
      identity; appointment type selection and no-time-held copy.

**Dependencies:** Task 11

**Files likely touched:**

- `CONTEXT.md`
- `docs/specs/backend-alignment-v17-2026-08-11-spec.md`
- `docs/specs/backend-alignment-v17-2026-08-11-plan.md`
- `docs/specs/backend-alignment-v17-2026-08-11-tasks.md`

**Estimated scope:** Medium (4 files)

### Checkpoint: Complete

- [ ] All approved v17 success criteria are met.
- [ ] Focused and full unit tests pass.
- [ ] Debug build, lint, formatting, Android test compilation, and diff checks
      pass.
- [ ] Manual linked/unlinked messaging and appointment regression matrix passes.
- [ ] Artifacts are ready for code review.

## Parallelization Opportunities

- Task 1 and the failing fixtures for Task 2 can be prepared independently
  because they do not share files, but both must pass before presentation work.
- Task 4 message rendering and Task 6 intent cleanup are independent only after
  Tasks 3 and 5 establish their respective contracts.
- Task 10 auth/account mapping and the additive invitation fixture in Task 11
  can be prepared separately with explicit ownership of
  `LimitedAccountViewModelTest.kt`.
- Tasks sharing `ChatViewModel.kt`, `ChatScreen.kt`, `Message.kt`,
  `ChatRepositoryImpl.kt`, or `NavGraph.kt` must be sequential.
- Task 12 is always last.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Removing context types before consumers | High | Tasks 3–6 retire creation/rendering/navigation before Task 7 deletes shared types |
| Missing capability enables protected actions | High | Fail-closed model fixtures in Task 2 and direct-call ViewModel tests in Task 3 |
| Unlinked rendering fetches protected images | High | Explicit download permission and manual network check in Task 4 |
| Route totals conflate alias and canonical routes | Medium | Separate assertions in Task 1 |
| Failed send loses patient text | Medium | ViewModel-owned recoverable draft and failure tests in Task 9 |
| Multipart path silently retains contexts | High | Exact recorded-request assertions in Task 8 and source audit in Task 12 |
| Stable 429 code is handled in one feature only | Medium | Cross-feature mapping Tasks 9–11 plus final focused suite |
| Invitation response causes token replacement | High | Additive-field fixture and existing-token `/me` regression in Task 11 |
| Shared dirty files are overwritten | Medium | Narrow patches and diff inspection before every task/checkpoint |

## Open Questions

None. Phase 4 implementation is intentionally paused until this Phase 3 task
breakdown is reviewed and explicitly approved.

## Verification of Task-Breakdown Gate

- [x] Every task has explicit acceptance criteria.
- [x] Every task has executable verification or a precise manual check.
- [x] Dependencies are identified and ordered.
- [x] No task lists more than five likely files.
- [x] Checkpoints occur after every two to three tasks.
- [x] High-risk contract and capability work occurs before presentation access.
- [ ] Human has reviewed and approved the Phase 3 task breakdown.
