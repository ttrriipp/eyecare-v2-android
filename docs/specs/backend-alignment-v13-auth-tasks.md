# Tasks: Backend Alignment V13 — Patient Account Access and Security

Status: Phase 3 approved — 2026-08-01; implementation awaiting authorization

Approved inputs:

- `docs/specs/backend-alignment-v13-auth-spec.md`
- `docs/specs/backend-alignment-v13-auth-plan.md`

## Execution Rules

1. Execute tasks in dependency order unless a listed parallel opportunity is
   explicitly authorized.
2. Start each behavior change with a focused failing test where practical.
3. Keep every task within its listed files unless a discovered dependency is
   documented and approved before expanding scope.
4. Preserve the user-owned changes in `docs/API_CONTRACT.md` and
   `docs/BACKEND_CONTEXT.md`.
5. Do not implement deferred appointment, intake, or full-route V13 work.
6. Never persist or log passwords, OTPs, invitation codes, registration
   proofs, step-up proofs, or raw proposed contacts.
7. Run `./gradlew assembleDebug` after every implementation task, as required
   by the project instructions.
8. Stop at each checkpoint if its verification is not green; diagnose rather
   than carrying a known failure forward.

## Phase A — Contract and Security Foundation

### Task 1: Add patient-account domain types and fail-closed routing policy

**Description:** Introduce serialization-free account, linked-patient, link
status, contact, challenge, proof, policy, and authenticated-session domain
types. Keep the existing `User` temporarily so current Profile/Chat consumers
remain buildable until their scheduled migration.

**Acceptance criteria:**

- [ ] `PatientAccount` separates account fields from nullable `LinkedPatient`.
- [ ] Link status includes `UNKNOWN`, which resolves to limited access.
- [ ] Login and session outcomes cannot represent an ambiguous partial result.

**Verification:**

- [ ] Focused domain/routing tests pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** None.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/domain/model/PatientAccount.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AccountAccess.kt`
- `app/src/test/java/com/eyecare/app/domain/model/AccountAccessTest.kt`

**Estimated scope:** Medium — 3 files.

### Task 2: Add stable installation identity beside encrypted token storage

**Description:** Add a focused provider that generates and persists one random
installation UUID in encrypted preferences and derives a permission-free
device label. Token clearing must not clear installation identity.

**Acceptance criteria:**

- [ ] Repeated calls return the same installation UUID.
- [ ] Clearing the bearer token preserves installation UUID.
- [ ] No hardware identifier or new Android permission is used.

**Verification:**

- [ ] `DeviceIdentityProviderTest` and `TokenManagerTest` pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** None.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/local/DeviceIdentityProvider.kt`
- `app/src/main/java/com/eyecare/app/data/local/TokenManager.kt`
- `app/src/test/java/com/eyecare/app/data/local/DeviceIdentityProviderTest.kt`
- `app/src/test/java/com/eyecare/app/data/local/TokenManagerTest.kt`

**Estimated scope:** Medium — 4 files.

### Task 3: Decode the standard API error envelope

**Description:** Add one data-layer decoder for the new machine-code envelope
and map it to a serialization-free domain failure with safe fallbacks for
empty, malformed, or unknown error responses.

**Acceptance criteria:**

- [ ] Code, patient-safe message, status, and optional field details decode.
- [ ] Malformed/unknown bodies become a generic safe failure.
- [ ] Decoder and failure types contain no response-body logging.

**Verification:**

- [ ] Focused decoder tests cover known, unknown, empty, and malformed bodies.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 1.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/ApiErrorDto.kt`
- `app/src/main/java/com/eyecare/app/data/remote/ApiErrorDecoder.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AccountAccessError.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiErrorDecoderTest.kt`

**Estimated scope:** Medium — 4 files.

### Task 4: Replace legacy auth transport contracts

**Description:** Define Kotlinx Serialization DTOs and Retrofit declarations
for policies, staged registration, hybrid login, login verification, recovery,
logout, `/me`, and name-only profile updates. Remove legacy service declarations
for `/login` and `/register` in this slice.

**Acceptance criteria:**

- [ ] Every public/session request uses exact contract field names.
- [ ] Both login response variants decode without DTO leakage.
- [ ] `PatientAccountResource`, including `linked_patient`, decodes safely.

**Verification:**

- [ ] Auth DTO encode/decode tests pass for linked/unlinked and both login
  variants.
- [ ] Source search finds no legacy auth service annotation.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 1 and 3.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/AuthDtos.kt`
- `app/src/main/java/com/eyecare/app/data/remote/api/AuthApiService.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/AuthDtosTest.kt`

**Estimated scope:** Medium — 3 files.

### Task 5: Add authenticated account transport contracts

**Description:** Add DTOs and Retrofit declarations for contacts, step-up,
password change, link state, and invitation acceptance. Protected mutations
must declare `X-Step-Up-Token` as a header.

**Acceptance criteria:**

- [ ] Contact values decode only as backend-provided masked values.
- [ ] All protected mutation declarations accept the step-up header.
- [ ] Invite code remains an opaque body value and never a path/query value.

**Verification:**

- [ ] Account DTO/request/header contract tests pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 1 and 3.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/AccountDtos.kt`
- `app/src/main/java/com/eyecare/app/data/remote/api/AccountApiService.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/AccountDtosTest.kt`

**Estimated scope:** Medium — 3 files.

### Task 6: Implement the new Auth repository vertical

**Description:** Expand the domain interface and repository implementation for
policies, registration proof/completion, hybrid login, login OTP, recovery,
session resolution, name update, and logout. Keep a temporary internal legacy
profile adapter only if required to keep current consumers buildable; it must
not call a legacy backend route.

**Acceptance criteria:**

- [ ] DTOs map to domain outcomes only at the repository boundary.
- [ ] Tokens save only after a session-producing success.
- [ ] Every accepted endpoint receives device/installation metadata where
  documented.

**Verification:**

- [ ] MockWebServer tests cover requests, mappings, token writes, errors, and
  malformed login variants.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 1–4 and Task 2.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AuthRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AuthRepositoryImplTest.kt`

**Estimated scope:** Medium — 3 files.

### Task 7: Implement the Account repository vertical

**Description:** Add the authenticated account repository for contacts,
step-up, password changes, link state, and invitations. Convert explicit
step-up parameters into `X-Step-Up-Token` headers only inside the data layer.

**Acceptance criteria:**

- [ ] Every account DTO maps into a domain type before returning.
- [ ] Step-up headers appear on exactly the protected endpoints.
- [ ] Invitation acceptance and contact errors preserve machine codes.

**Verification:**

- [ ] MockWebServer tests cover all operations and header presence/absence.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 1–3 and Task 5.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/domain/repository/AccountRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AccountRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AccountRepositoryImplTest.kt`

**Estimated scope:** Medium — 3 files.

### Task 8: Wire Hilt and make 401 events bearer-aware

**Description:** Bind/provide the new repository and service verticals, then
restrict global session-expired events to 401 responses for requests that
actually carried the app bearer token.

**Acceptance criteria:**

- [ ] Auth and Account services/repositories resolve through Hilt.
- [ ] Public-request 401 does not emit global logout.
- [ ] Bearer-authenticated 401 emits exactly one logout event.

**Verification:**

- [ ] Hilt/network and interceptor focused tests pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 6 and 7.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/di/AuthModule.kt`
- `app/src/main/java/com/eyecare/app/data/remote/interceptor/AuthInterceptor.kt`
- `app/src/test/java/com/eyecare/app/data/remote/interceptor/AuthInterceptorTest.kt`
- `app/src/test/java/com/eyecare/app/di/NetworkModuleTest.kt`

**Estimated scope:** Medium — 4 files.

### Task 9: Refactor route governance for the auth-only transition

**Description:** Replace the misleading single 35-route assertion with exact
V13 auth/account routes plus an explicitly named set of deferred non-auth
migration debt. Do not claim removed appointment/intake routes remain approved.

**Acceptance criteria:**

- [ ] V13 auth/account declarations match the backend appendix exactly.
- [ ] Legacy auth routes are rejected.
- [ ] Deferred routes cannot grow without a test failure and explicit edit.

**Verification:**

- [ ] Route contract tests pass and report categories clearly on failure.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 4, 5, and 8.

**Files likely touched:**

- `app/src/test/java/com/eyecare/app/data/remote/ApprovedApiRoutes.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** Small — 2 files.

## Checkpoint A — Foundation

- [ ] `./gradlew testDebugUnitTest --tests "*Auth*" --tests "*Account*" --tests "*ApiRoute*" --tests "*TokenManager*" --tests "*DeviceIdentity*"` passes.
- [ ] `./gradlew assembleDebug` succeeds.
- [ ] No credential/proof value appears in persistent storage or logs.
- [ ] No Android production UI has been switched to the new flow prematurely.

## Phase B — Session Resolution and Navigation Shell

### Task 10: Implement session resolution and routing policy

**Description:** Add `SessionViewModel` and a pure fail-closed routing policy
for no token, linked, unlinked, pending, unknown, 401, and transient failure.

**Acceptance criteria:**

- [ ] Stored tokens are validated through `/me`.
- [ ] Only `LINKED` resolves to Main.
- [ ] A transient error preserves the token and exposes Retry/Sign out.

**Verification:**

- [ ] Session/routing ViewModel tests pass for the complete matrix.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 6 and 8.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/SessionViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/SessionRouting.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/SessionViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/SessionRoutingTest.kt`

**Estimated scope:** Medium — 4 files.

### Task 11: Add SessionGate, Welcome, and account-access graph routes

**Description:** Add type-safe routes and minimal screens for session checking,
Welcome, and limited account access graph entry. Make SessionGate the sole root
start destination.

**Acceptance criteria:**

- [ ] Welcome exposes Sign in and Create account without API side effects.
- [ ] SessionGate renders checking, retry, and sign-out states.
- [ ] Navigation has distinct Auth, AccountAccess, and Main graph roots.

**Verification:**

- [ ] Focused Compose/navigation tests pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 10.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/navigation/Routes.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/SessionGateScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/WelcomeScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/auth/WelcomeSessionGateTest.kt`

**Estimated scope:** Medium — 5 files.

### Task 12: Centralize session termination and gate linked-only startup work

**Description:** Route global 401 and explicit logout through one navigation
path, preserve installation identity, and prevent root conversation/unread work
from running until MainGraph is active.

**Acceptance criteria:**

- [ ] Session expiry clears token and removes protected graphs from history.
- [ ] Unlinked/session-checking states make no conversation request.
- [ ] Logout transport failure supports Retry and explicit local-only fallback.

**Verification:**

- [ ] Root navigation/session tests cover expiry, logout, and unlinked startup.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 11.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/MainActivity.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/test/java/com/eyecare/app/presentation/navigation/SessionNavigationTest.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatStartupBoundaryTest.kt`

**Estimated scope:** Medium — 4 files.

## Checkpoint B — Session Shell

- [ ] Cold start works for no token, linked, unlinked, pending, unknown,
  expired token, and offline/transient error.
- [ ] No linked-only request starts outside MainGraph.
- [ ] `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` pass.

## Phase C — Registration

### Task 13: Build shared stateless auth components

**Description:** Add reusable presentation primitives for auth layout, contact
method/field, password visibility, OTP entry/expiry/resend, and policy consent.
No component owns a repository or sensitive workflow state.

**Acceptance criteria:**

- [ ] OTP field accepts exactly six digits and exposes accessible semantics.
- [ ] Passwords are obscured by default with an explicit visibility action.
- [ ] Policy rows support independent checkbox and link actions.

**Verification:**

- [ ] Component Compose tests/previews compile.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Checkpoint B.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/components/AuthStepScaffold.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/components/ContactFields.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/components/SecretFields.kt`
- `app/src/main/java/com/eyecare/app/presentation/auth/components/PolicyConsent.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/auth/AuthComponentsTest.kt`

**Estimated scope:** Medium — 5 files.

### Task 14: Implement the registration state machine

**Description:** Add contact-method, contact OTP, registration proof, policy,
details, invitation, terminal session, and optional-secondary follow-up states.
Keep all proof/contact/credential values only in ViewModel memory.

**Acceptance criteria:**

- [ ] Email and phone variants reach registration completion.
- [ ] Details validation matches the approved specification.
- [ ] Optional secondary verification is skippable and cannot roll back the
  saved account session.

**Verification:**

- [ ] ViewModel tests cover resend replacement, policy failure/retry, invalid
  OTP, invite failure, linked/unlinked success, and secondary skip/success.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 6, 7, 10, and 13.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/RegistrationViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/RegistrationViewModelTest.kt`

**Estimated scope:** Medium — 2 files.

### Task 15: Replace the legacy registration screen

**Description:** Replace the flat Register UI with the approved multi-step
registration experience and wire its terminal session callback into root
fail-closed routing.

**Acceptance criteria:**

- [ ] Every required/optional field and policy link is present at the correct
  step.
- [ ] Back behavior cannot reuse a consumed registration proof.
- [ ] Completion routes solely from returned `linkStatus`.

**Verification:**

- [ ] Registration Compose/navigation tests pass for email, phone, policy,
  invite, and optional-secondary paths.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 14.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/RegisterScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/auth/RegistrationFlowTest.kt`

**Estimated scope:** Medium — 3 files.

## Checkpoint C — Registration

- [ ] Email and phone registration pass mocked end-to-end tests.
- [ ] Optional secondary contact cannot turn account success into failure.
- [ ] No secret/proof is stored or placed in a route.
- [ ] Focused tests and `./gradlew assembleDebug` pass.

## Phase D — Sign-in and Recovery

### Task 16: Implement the sign-in state machine

**Description:** Add contact, password, trusted-session, OTP-required, resend,
and terminal session states with enumeration-safe error copy.

**Acceptance criteria:**

- [ ] Trusted login skips OTP and returns the authenticated account.
- [ ] Untrusted login verifies OTP before returning the account.
- [ ] Password/contact secrets clear when the flow is abandoned.

**Verification:**

- [ ] ViewModel tests cover both login variants, invalid OTP, attempt/rate
  limits, resend, malformed response, and routing outcomes.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 6, 10, and 13.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/SignInViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/SignInViewModelTest.kt`

**Estimated scope:** Medium — 2 files.

### Task 17: Replace the legacy login screen

**Description:** Replace email-only Login with contact → password → optional
OTP UI, add Forgot password navigation, and integrate terminal session routing.

**Acceptance criteria:**

- [ ] Contact accepts email or phone without an email-only label.
- [ ] OTP appears only for `OtpRequired`.
- [ ] Trusted completion and verified completion use the same root callback.

**Verification:**

- [ ] Sign-in Compose/navigation tests pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 16.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/LoginScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/auth/SignInFlowTest.kt`

**Estimated scope:** Medium — 3 files.

### Task 18: Implement password-recovery state

**Description:** Add enumeration-safe recovery contact, OTP, new password,
combined verification/reset, and terminal session states. Carry the OTP only in
memory from its UI step to the reset request.

**Acceptance criteria:**

- [ ] Recovery sends device and installation metadata.
- [ ] Invalid OTP returns to OTP entry safely.
- [ ] Success saves the returned token and reports other-session revocation.

**Verification:**

- [ ] Recovery ViewModel tests cover known/unknown-safe responses, invalid OTP,
  rate limits, password validation, and linked/unlinked outcomes.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 6, 10, and 13.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/PasswordRecoveryViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/PasswordRecoveryViewModelTest.kt`

**Estimated scope:** Medium — 2 files.

### Task 19: Add the password-recovery screen

**Description:** Add contact → OTP → new password presentation, wire it from
Forgot password, and route terminal sessions through the shared callback.

**Acceptance criteria:**

- [ ] Recovery never confirms whether an account exists.
- [ ] New password remains obscured and confirms minimum length/match locally.
- [ ] Back/exit clears recovery secrets.

**Verification:**

- [ ] Recovery Compose/navigation tests pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 18.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/PasswordRecoveryScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/auth/PasswordRecoveryFlowTest.kt`

**Estimated scope:** Medium — 3 files.

## Checkpoint D — Public Access Flows

- [ ] Welcome, registration, sign-in, and recovery work end to end against
  mocked API responses.
- [ ] Trusted login never flashes OTP.
- [ ] Enumeration-safe copy is used throughout.
- [ ] Unit tests, Android-test compilation, and debug assembly pass.

## Phase E — Limited Account and Invitations

### Task 20: Implement limited-account and invitation state

**Description:** Represent unlinked, pending-review, and unknown account states,
then add invitation-code OTP, verification, `/me` refresh, and linked-only
terminal completion.

**Acceptance criteria:**

- [ ] No limited status emits Main navigation.
- [ ] Invitation code remains in memory and is trimmed only at submission.
- [ ] Acceptance reaches Main only after refreshed `/me` reports `LINKED`.

**Verification:**

- [ ] ViewModel tests cover unlinked/pending/unknown, invalid invitation,
  invalid OTP, idempotent linked success, and non-linked refresh.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 7 and 10.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/account/LimitedAccountViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/LimitedAccountViewModelTest.kt`

**Estimated scope:** Medium — 2 files.

### Task 21: Add limited-account and invite UI

**Description:** Build limited account overview, status copy, invite entry/OTP,
Account & Security navigation, and logout actions without bottom navigation.

**Acceptance criteria:**

- [ ] Unlinked/pending/unknown states use distinct safe copy.
- [ ] Invite errors reveal no patient identity.
- [ ] Back cannot enter MainGraph.

**Verification:**

- [ ] Limited-account Compose/navigation tests pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 20.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/account/LimitedAccountScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/account/LimitedAccountFlowTest.kt`

**Estimated scope:** Medium — 3 files.

## Checkpoint E — Link Boundary

- [ ] All non-linked statuses remain outside Main.
- [ ] Invitation acceptance refreshes authoritative account state.
- [ ] Account Security and logout are reachable without clinical navigation.
- [ ] Focused tests and debug assembly pass.

## Phase F — Account Security

### Task 22: Implement Account Security contact overview state

**Description:** Add contact loading/retry and masked primary/pending/verified
presentation state without yet enabling protected mutations.

**Acceptance criteria:**

- [ ] Only masked values reach UI state.
- [ ] Primary and verification eligibility are explicit.
- [ ] Loading/error/retry does not fabricate a contact list.

**Verification:**

- [ ] Contact overview ViewModel tests pass.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 7.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/AccountSecurityViewModelTest.kt`

**Estimated scope:** Medium — 2 files.

### Task 23: Add step-up and contact mutation states

**Description:** Extend Account Security with fresh step-up, add-contact OTP,
make-primary, and remove-contact flows. Clear proof after every protected
attempt and refresh contacts after success.

**Acceptance criteria:**

- [ ] Each protected mutation obtains a fresh step-up proof.
- [ ] Add contact performs a distinct OTP to the proposed contact.
- [ ] Last-contact and unverified-primary errors retain authoritative state.

**Verification:**

- [ ] Focused tests cover proof lifecycle, headers through repository calls,
  all contact actions, errors, and refresh.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 22.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/AccountSecurityViewModelTest.kt`

**Estimated scope:** Medium — 2 files.

### Task 24: Add password change and multi-device logout states

**Description:** Extend Account Security with step-up-protected password
change, logout-all, normal logout, retry, and local-only fallback behavior.

**Acceptance criteria:**

- [ ] Password change requires current/minimum-12/confirmation values.
- [ ] Success clears password/proof state and reports other-session revocation.
- [ ] Failed server logout never silently claims remote revocation.

**Verification:**

- [ ] Focused tests cover password validation/success/errors, logout-all, retry,
  and local-only fallback.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 23 and Task 6.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/account/AccountSecurityViewModelTest.kt`

**Estimated scope:** Medium — 2 files.

### Task 25: Add Account & Security UI and shared navigation

**Description:** Build the masked contact list and internal step-up/contact/
password states in one screen, reachable from both AccountAccessGraph and
linked Profile.

**Acceptance criteria:**

- [ ] Raw contacts and secret proofs never render.
- [ ] Protected actions clearly identify the current primary OTP destination.
- [ ] Completion/error states match ViewModel outcomes and prevent duplicates.

**Verification:**

- [ ] Account Security Compose/navigation tests cover all actions.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 24.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/account/AccountSecurityScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/account/components/ContactRow.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/account/AccountSecurityFlowTest.kt`

**Estimated scope:** Medium — 4 files.

## Checkpoint F — Account Security

- [ ] All protected mutations use fresh in-memory step-up proof.
- [ ] Contact values remain masked outside request-entry state.
- [ ] Password and logout behavior accurately represent revocation outcomes.
- [ ] Focused/full unit tests and debug assembly pass.

## Phase G — Profile Migration and Legacy Cleanup

### Task 26: Migrate Profile to PatientAccount and narrow editing

**Description:** Load the new account model, render nested linked clinical data
read-only, restrict editing to first/last account name, and expose Account &
Security navigation.

**Acceptance criteria:**

- [ ] `linkedPatient` data is never included in update requests.
- [ ] Email, phone, DOB, occupation, address, gender, and clinical email are
  absent from `PATCH /me`.
- [ ] Profile works for linked and null-linked account payloads.

**Verification:**

- [ ] Profile ViewModel/presentation tests cover exact request serialization
  and read-only clinical rendering.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Tasks 6 and 25.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileViewModel.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/com/eyecare/app/presentation/profile/EditProfileScreen.kt`
- `app/src/test/java/com/eyecare/app/presentation/profile/ProfileViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/presentation/navigation/NavGraph.kt`

**Estimated scope:** Medium — 5 files.

### Task 27: Migrate Chat identity lookup and remove legacy User adapters

**Description:** Switch the remaining AuthRepository consumer to
`PatientAccount`, then delete the old `User` model, temporary repository adapter,
and obsolete flat-profile mappings once source search proves no consumer.

**Acceptance criteria:**

- [ ] Chat still resolves the authenticated account ID.
- [ ] No production/test source imports or constructs legacy `User`.
- [ ] No flat clinical-field auth mapping remains.

**Verification:**

- [ ] Chat, Auth repository, and Profile focused tests pass.
- [ ] Source search for legacy `User` is empty outside historical docs.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 26.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/messaging/ChatViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/messaging/ChatViewModelTest.kt`
- `app/src/main/java/com/eyecare/app/domain/repository/AuthRepository.kt`
- `app/src/main/java/com/eyecare/app/data/repository/AuthRepositoryImpl.kt`
- `app/src/main/java/com/eyecare/app/domain/model/User.kt`

**Estimated scope:** Medium — 5 files.

### Task 28: Remove obsolete auth state/tests and run security source sweeps

**Description:** Delete the replaced flat `AuthViewModel` behavior and obsolete
tests, remove dead legacy screen callbacks/models, and add focused regression
checks for forbidden route/secret persistence patterns.

**Acceptance criteria:**

- [ ] No code calls or declares legacy auth endpoints.
- [ ] No old combined Auth UI state remains reachable.
- [ ] Automated/source checks cover forbidden persistence/log/route patterns.

**Verification:**

- [ ] Auth/account/session/profile focused suites pass.
- [ ] `./gradlew assembleDebugAndroidTest` succeeds.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Task 27 and all prior UI tasks.

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/auth/AuthViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/auth/AuthViewModelTest.kt`
- `app/src/test/java/com/eyecare/app/security/AuthSecretBoundaryTest.kt`
- `app/src/test/java/com/eyecare/app/data/remote/ApiRouteAllowlistTest.kt`

**Estimated scope:** Medium — 4 files.

## Checkpoint G — Feature Complete

- [ ] Registration, trusted/untrusted login, recovery, invitations, contacts,
  password change, session resolution, logout, and Profile pass regression.
- [ ] Legacy auth model/routes/state are absent.
- [ ] Deferred non-auth V13 routes remain explicitly marked, not hidden.
- [ ] Unit tests, Android-test compilation, ktlint, lint, and debug assembly
  pass.

## Phase H — Documentation and Final Verification

### Task 29: Update project context and close implementation records

**Description:** Update Android context and the spec/plan/task status lines to
describe the implemented auth-only scope, verification results, and remaining
V13 release blockers. Do not edit the backend documents.

**Acceptance criteria:**

- [ ] `CONTEXT.md` accurately describes the new auth/account architecture.
- [ ] Deferred appointment/intake/global-route work remains explicit.
- [ ] Spec, plan, and tasks record completion only after verification passes.

**Verification:**

- [ ] `git diff --check` passes for changed documentation.
- [ ] Documentation claims match actual commands/results.
- [ ] `./gradlew assembleDebug` succeeds.

**Dependencies:** Checkpoint G.

**Files likely touched:**

- `CONTEXT.md`
- `docs/specs/backend-alignment-v13-auth-spec.md`
- `docs/specs/backend-alignment-v13-auth-plan.md`
- `docs/specs/backend-alignment-v13-auth-tasks.md`

**Estimated scope:** Medium — 4 files.

### Task 30: Run the final auth-only quality gate

**Description:** Run the full approved verification matrix, resolve only
auth-scope failures, and record emulator-dependent checks accurately.

**Acceptance criteria:**

- [ ] All available automated checks pass.
- [ ] No auth/account contract, persistence, navigation, or security failure
  remains.
- [ ] Any unavailable connected-device test is reported, never claimed run.

**Verification:**

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew assembleDebug
.\gradlew connectedDebugAndroidTest
```

`connectedDebugAndroidTest` runs only when an emulator/device is available.

**Dependencies:** Task 29.

**Files likely touched:** None unless an auth-scope verification failure
requires a targeted correction approved by the task boundaries.

**Estimated scope:** Small — verification only.

## Parallelization Notes

After Checkpoint B and stable repository interfaces:

- Tasks 14, 16, and 18 may be implemented independently.
- Task 13 must finish before their corresponding screen tasks.
- Tasks 20 and 22 may proceed independently after AccountRepository is stable.

The following remain sequential:

- Tasks 1–9 foundation.
- Session shell before terminal navigation integration.
- Each ViewModel before its screen task.
- Account Security Tasks 22–25.
- Profile migration before legacy User removal.
- Final cleanup/documentation/quality gate.

Parallel execution is not authorized by this document alone; use it only if
the project owner explicitly requests agents or parallel work.

## Phase Gate

This Phase 3 task breakdown was approved by the project owner on 2026-08-01.

Do not begin Task 1 or modify Android production code until the project owner
explicitly says to implement.
