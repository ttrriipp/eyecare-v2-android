# Implementation Plan: Backend Alignment V13 — Patient Account Access and Security

Status: Phases 2–3 approved — 2026-08-01; implementation awaiting authorization

Approved specification:
`docs/specs/backend-alignment-v13-auth-spec.md`

## Overview

Replace the current email-only, single-request auth vertical with the approved
two-stage registration, trusted-device login, recovery, account security, and
invite-linking experience without implementing the deferred appointment/intake
V13 cutover.

The work will use vertical slices and keep the project buildable between
checkpoints. Contract/domain foundations land first, followed by session
resolution, registration, login/recovery, limited account access, security,
and Profile integration. Production implementation remains blocked until a
separate Phase 3 task breakdown is approved.

## Current-State Findings

The implementation plan is based on these verified seams:

- `AuthApiService` still calls legacy `login` and `register`.
- `AuthDtos` assumes email-only login and a flat legacy `UserDto`.
- `AuthRepositoryImpl` saves tokens but cannot represent OTP-required login.
- `AuthViewModel` combines login and registration into one flat success/error
  state and injects `TokenManager` directly.
- `LoginScreen` and `RegisterScreen` keep form values in local `remember`
  state and have no multi-step orchestration.
- `NavGraph` chooses its start graph synchronously from token presence.
- `NavGraph` requests conversation/unread data before account link state is
  resolved.
- `AuthInterceptor` broadcasts logout for every 401, including a request that
  did not carry a bearer token.
- `TokenManager` stores only the bearer token in encrypted preferences.
- `User` is a flat model whose account and clinical-patient fields are mixed.
- Profile currently attempts to edit fields now forbidden by `PATCH /me`.
- `ApprovedApiRoutes` still represents the broader V12 35-route contract. The
  approved auth-only spec deliberately defers the global V13 allowlist cutover.

## Architecture Decisions

### 1. Separate public/session auth from authenticated account management

Use two API/repository verticals:

```text
AuthApiService / AuthRepository
    policies
    registration OTP/proof/completion
    login start/verification
    password recovery
    getMe/updateMe
    logout/logout-all

AccountApiService / AccountRepository
    contacts
    step-up OTP/proof
    password change
    account link status
    invitation OTP/acceptance
```

This keeps public credential exchange separate from mutations that require an
existing bearer token and sometimes an `X-Step-Up-Token` header.

### 2. Replace the mixed `User` model with `PatientAccount`

Introduce a domain model that separates account identity from the optional
read-only clinical link:

```text
PatientAccount
    account fields
    linkStatus
    linkedPatient: LinkedPatient?
```

Remove the old `User` model after Auth, Profile, Chat, and their tests migrate.
This is a clean cutover rather than a compatibility alias because the app is
not deployed and legacy maintenance is not required.

### 3. Keep DTO polymorphism at the repository boundary

Login's response is a transport object with nullable challenge/session fields.
`AuthRepositoryImpl` validates the documented shape and maps it into:

```text
LoginOutcome.OtpRequired
LoginOutcome.Authenticated
```

Malformed combinations fail as contract errors; ViewModels never inspect
nullable DTO fields.

The same rule applies to `PatientAccountDto.linkedPatient`, policy resources,
contacts, challenges, proofs, and invitation results.

### 4. Use one in-memory state machine per sensitive user journey

Do not place contacts, OTPs, passwords, invitation codes, registration tokens,
or step-up tokens in navigation arguments or `SavedStateHandle`.

Top-level screens own one ViewModel and switch internal steps:

```text
SignInViewModel
    Contact → Password → OTP → Complete

RegistrationViewModel
    Method → Contact → OTP → Details → OptionalSecondary → Complete

PasswordRecoveryViewModel
    Contact → OTP → NewPassword → Complete

LimitedAccountViewModel
    Overview → InvitationCode → InvitationOtp → Complete

AccountSecurityViewModel
    Overview → StepUpOtp → ProtectedAction → ContactOtp/Complete
```

Process death intentionally restarts the affected sensitive journey.

### 5. Share presentation primitives, not security orchestration

Create reusable stateless Compose primitives for:

- auth step scaffold/top bar;
- contact method selector;
- contact field;
- password field with visibility control;
- six-digit OTP field and expiry/resend presentation;
- policy consent rows; and
- masked contact rows.

Each ViewModel retains purpose-specific resend and verification logic. There is
no generic OTP repository or global OTP state.

### 6. Always start through session resolution

`EyecareNavGraph` starts at a `SessionGate` route instead of choosing from
token presence. `SessionViewModel` produces:

```text
Checking
Unauthenticated
Linked(account)
Limited(account)
TransientFailure
```

Routing policy is a pure function of `PatientLinkStatus`:

```text
LINKED                 → MainGraph
UNLINKED               → AccountAccessGraph
PENDING_REVIEW         → AccountAccessGraph
UNKNOWN                → AccountAccessGraph
```

The gate validates a stored token with `/me`. A transient failure stays on the
gate with Retry/Sign out; only 401 invalidates the local token.

### 7. Scope conversation work to linked MainGraph

Move the initial unread/conversation request out of root graph composition so
it is created only after `MainGraph` is active. This prevents account-only users
from calling an active-link endpoint during startup.

The complete active-link audit remains deferred, but this root-level request
must move because it is currently unconditional and directly conflicts with
auth routing.

### 8. Harden 401 broadcasting

`AuthInterceptor` emits `AuthEvent.Logout` only when the failed request
actually carried the app's bearer token. Public login, registration, recovery,
and policy responses cannot create a false global session-expired event.

The event remains a process-wide escape hatch. MainActivity/navigation clears
only the token, preserves installation identity, and returns to Welcome.

### 9. Keep installation identity separate from session identity

Retain `TokenManager` for the encrypted bearer token and add a focused
`DeviceIdentityProvider` backed by the same encrypted preferences:

```text
getOrCreateInstallationId(): String
deviceName(): String
```

The installation ID is a random UUID generated once. Clearing a session never
clears it. No Android hardware identifier is read.

### 10. Centralize the new error-envelope decoder

Add one data-layer decoder for:

```json
{"error":{"code":"...","message":"...","details":{}}}
```

Auth and Account repositories map decoded failures into a shared
serialization-free domain error carrying HTTP status, machine code,
patient-safe message, and field details where present.

Local validation remains ViewModel-owned. Machine codes drive OTP, contact,
invitation, link, and rate-limit transitions.

### 11. Keep token persistence inside repository success boundaries

Only repository operations returning an authenticated session save a bearer
token:

- registration completion;
- trusted-device login;
- login OTP verification; and
- password recovery verification/reset.

Challenge/proof calls never mutate persistent session state. ViewModels do not
write tokens directly.

### 12. Treat step-up proof as single-operation state

`AccountSecurityViewModel` holds a step-up token only long enough to issue one
protected request. It clears the token after success or failure. A retry of the
protected mutation begins a fresh step-up flow unless failure occurred before
the token reached the backend and Phase 3 proves safe reuse is contractually
valid; the default implementation is fresh verification.

### 13. Profile becomes an account/clinical read model

Profile maps:

```text
PatientAccount fields      → account identity and primary login contact
linkedPatient fields       → read-only clinic identity/demographics
```

Edit Profile contains only first and last name. Account contacts and password
move behind an **Account & Security** row shared with `AccountAccessGraph`.
Profile logout calls the repository and no longer clears the token separately
in both ViewModel and navigation callbacks.

### 14. Refactor route governance into an honest transitional model

The current allowlist requires every approved route to have a Retrofit consumer
and will fail as soon as legacy auth routes are replaced. Updating that single
set to the full V13 list would instead force the explicitly deferred
appointment/intake migration into auth work.

Refactor the route contract test into two explicit categories:

```text
V13 auth/account routes
    exact new subset consumed by this phase

Deferred non-auth routes
    existing Android consumers awaiting the coordinated V13 migration
```

Tests must assert:

- no legacy `/login` or `/register` declaration remains;
- every V13 auth/account service route exactly matches the backend appendix;
- no newly introduced route can enter the deferred set;
- every deferred route is named as migration debt rather than described as
  backend-approved; and
- the full 55-route allowlist remains a release blocker for the later cutover.

This keeps the suite green without falsely claiming removed appointment/intake
routes are approved by the updated backend.

## Navigation Design

### Route graph

```text
SessionGate
    ├── AuthGraph
    │   ├── Welcome
    │   ├── SignIn
    │   ├── CreateAccount
    │   └── RecoverPassword
    ├── AccountAccessGraph
    │   ├── LimitedAccount
    │   └── AccountSecurity
    └── MainGraph
        └── existing screens
            └── AccountSecurity (shared destination)
```

Sign-in, registration, recovery, invitation, and security substeps are internal
screen states, not navigation routes. This keeps secrets out of routes and
makes Back behavior deterministic.

### Terminal navigation

Every session-producing flow emits `PatientAccount` to one root callback:

```text
onSessionEstablished(account)
```

The root uses the fail-closed routing policy and clears the previous auth or
account-access graph with an inclusive `popUpTo`. Invitation acceptance reaches
Main only after a refreshed `/me` reports `LINKED`.

### Back behavior

- Welcome uses normal system exit behavior.
- Internal auth steps move to the prior step; leaving a flow clears secrets.
- Registration cannot navigate back from Details to a consumed OTP and submit
  it again; returning before account completion restarts contact verification.
- Limited account Back does not enter Main.
- Account Security exits to its linked Profile or limited-account caller.
- A completed logout clears navigation history and opens Welcome.

## Data and Interface Design

### Auth repository surface

The exact signatures belong in Phase 3, but the plan requires operations for:

```text
getPolicies
requestRegistrationOtp
verifyRegistrationOtp
register
beginLogin
verifyLogin
requestPasswordRecoveryOtp
recoverPassword
getMe
updateAccountName
logoutCurrent
logoutAll
```

All return `Result<DomainType>` using the established repository convention.

### Account repository surface

```text
getContacts
requestStepUp
verifyStepUp
requestContactOtp(stepUpToken, contact)
verifyContactOtp
makePrimary(stepUpToken, contactId)
removeContact(stepUpToken, contactId)
changePassword(stepUpToken, credentials)
getLinkState
requestInvitationOtp
acceptInvitation
```

Step-up proof is an explicit method parameter and becomes an HTTP header only
inside the data layer.

### Persistence

Encrypted preferences contain only:

```text
auth_token
installation_id
```

No Room schema change occurs. Device name is derived on demand and need not be
persisted.

## Implementation Sequence

This is component-level Phase 2 ordering, not the Phase 3 task breakdown.

### Stage A — Contract and security foundation

1. Add new auth/account DTOs and strict domain mappings.
2. Introduce `PatientAccount`, `LinkedPatient`, contact/link/session outcomes,
   and the shared API error model.
3. Add `DeviceIdentityProvider` and extend storage tests.
4. Split Auth/Account Retrofit services, repositories, and Hilt bindings.
5. Harden bearer-aware 401 broadcasting.
6. Refactor route-contract tests into exact V13 auth/account routes plus named
   deferred non-auth migration debt.

**Checkpoint A:** MockWebServer proves all auth/account payloads, response
variants, headers, error codes, and persistence boundaries; debug build passes.

### Stage B — Session resolution and navigation shell

1. Add SessionGate and `SessionViewModel`.
2. Add Welcome and AccountAccess graph roots.
3. Introduce fail-closed link-status routing.
4. Move root conversation/unread work behind MainGraph.
5. Centralize session-expired and explicit-logout navigation. A normal logout
   clears the token only after server revocation succeeds; a transport failure
   stays signed in with Retry and an explicit **Sign out locally anyway**
   fallback that explains server revocation could not be confirmed.

**Checkpoint B:** cold-start tests cover no token, linked, unlinked, pending,
unknown, 401, and transient failure without invoking linked-only work.

### Stage C — Registration vertical

1. Implement policy loading and contact-method selection.
2. Implement registration OTP request, verification proof, resend, and expiry
   state.
3. Implement details/consent/invitation submission.
4. Route the authenticated account by link status.
5. Add optional secondary-contact follow-up using the Account repository.

**Checkpoint C:** email and phone registration work end to end against mocked
contract responses; secondary verification is skippable and cannot roll back
account creation.

### Stage D — Sign-in and recovery verticals

1. Implement contact/password sign-in steps.
2. Handle trusted direct-session and untrusted OTP outcomes.
3. Implement recovery contact, OTP, new-password, and session result.
4. Apply enumeration-safe and machine-code-specific error presentation.

**Checkpoint D:** trusted, untrusted, invalid OTP, rate limit, recovery, and
other-session-revocation states pass focused tests and assemble cleanly.

### Stage E — Limited account and invitation vertical

1. Render unlinked/pending/unknown limited-account states.
2. Add invitation-code OTP and acceptance state machine.
3. Refresh `/me` after acceptance and enforce linked-only terminal routing.
4. Expose Account & Security and logout from the limited destination.

**Checkpoint E:** no limited state reaches Main without backend `LINKED`; invite
errors remain enumeration-safe.

### Stage F — Account security vertical

1. Add contact list and masked/primary presentation.
2. Implement fresh step-up orchestration.
3. Add and verify a secondary contact.
4. Make primary and remove contact through protected mutations.
5. Change password and add logout-all behavior.

**Checkpoint F:** every protected request carries one in-memory
`X-Step-Up-Token`; token and credential leak checks pass.

### Stage G — Profile and legacy auth cleanup

1. Migrate Profile and Chat consumers from `User` to `PatientAccount`.
2. Render `linkedPatient` as read-only clinical data.
3. Restrict Edit Profile to first/last name.
4. Link Account & Security from Profile.
5. Remove legacy AuthViewModel, screen behavior, DTOs, endpoint declarations,
   and obsolete tests after replacements are green.
6. Update auth/account project context and record deferred V13 blockers.

**Checkpoint G:** legacy auth routes are absent; linked Profile, limited
account, logout, full focused unit suite, Android-test compilation, ktlint,
lint, and debug assembly pass.

## Parallelization Opportunities

After Stage A contracts are stable, the following implementation work can be
developed independently with coordination at navigation callbacks:

- Registration presentation and ViewModel tests.
- Sign-in/recovery presentation and ViewModel tests.
- Account Security presentation and ViewModel tests.
- Stateless shared auth components and previews.

The following must remain sequential:

- Domain/DTO/repository contract before ViewModel implementation.
- SessionGate before terminal graph integration.
- Account repository step-up support before secondary contact or security UI.
- `PatientAccount` migration before old `User` deletion.
- Replacement flow verification before legacy auth removal.

No sub-agent or parallel branch is required by this plan; Phase 3 may identify
safe parallel sessions if the project owner requests them.

## Verification Strategy

### Focused commands during implementation

```powershell
.\gradlew testDebugUnitTest --tests "*Auth*" --tests "*Account*" --tests "*Session*" --tests "*Profile*"
.\gradlew assembleDebug
```

### Checkpoint commands

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew assembleDebug
```

When an emulator/device is available:

```powershell
.\gradlew connectedDebugAndroidTest
```

### Manual end-to-end matrix

```text
email registration without invite
phone registration with valid invite
registration with skipped/verified secondary contact
trusted login
untrusted login OTP
password recovery
unlinked invite acceptance
pending-review limited access
add/make-primary/remove contact
change password
logout current/logout all
cold start linked/unlinked/offline/expired token
```

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Sensitive values leak through state, logs, routes, or saved state | High | Single-screen in-memory state machines, BASIC-only HTTP logging, explicit leak tests/source sweeps |
| Public 401 triggers global logout | High | Emit logout only when the request carried the app bearer token |
| Token presence bypasses server validation | High | Mandatory SessionGate `/me` resolution |
| Unknown link status exposes clinical UI | High | Pure fail-closed routing policy with exhaustive tests |
| Optional secondary contact makes registration appear failed | Medium | Persist account session first; separate skippable follow-up state |
| One-time step-up proof is accidentally reused | High | Explicit method argument, in-memory ownership, clear after each protected attempt |
| Root chat call fails for unlinked account | High | Move unread/conversation initialization behind MainGraph |
| Profile update sends retired clinical/contact fields | High | New narrow request model and removal tests for forbidden serialized keys |
| Login polymorphic response silently decodes an invalid combination | Medium | Strict repository mapping with required-field contract failures |
| Process death loses a long registration flow | Medium | Intentional security tradeoff; restart with clear copy, never persist secrets |
| Auth-only work is mistaken for complete V13 compatibility | High | Keep deferred cutover in spec, plan, context, and release checklist |
| Backend appendix count remains inconsistent | Low | Consume explicit endpoint definitions; defer global count assertion |

## Deferred Work and Release Boundary

This plan does not implement:

- appointment requests;
- removal of direct booking or patient-selectable appointment types;
- intake retirement;
- appointment contact-note retirement;
- full active-link enforcement audit; or
- the global 55-route allowlist.

The auth phase may be built and reviewed independently, but it must not be
described as full V13 backend alignment or released against the breaking
backend without the deferred migration.

## Open Questions

None. The approved specification resolves product behavior. Phase 3 will
translate these stages into small tasks of approximately five files or fewer,
with explicit acceptance and verification for each.

## Phase Gate

This Phase 2 plan was approved by the project owner on 2026-08-01.

Create the Phase 3 task breakdown for separate approval. Do not modify Android
production code until Phase 3 is approved and the project owner explicitly
authorizes implementation.
