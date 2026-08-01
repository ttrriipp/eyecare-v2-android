# Backend Alignment V13 — Patient Account Access and Security

Status: Phases 1–3 approved — 2026-08-01; implementation awaiting authorization

## Objective

Replace the legacy one-request email registration and login experience with the
backend's patient-account access model:

- welcome screen with **Sign in** and **Create account**;
- registration through a verified email or phone contact;
- password login with OTP on new or untrusted installations;
- persistent device-labelled Sanctum sessions for normal app use;
- OTP-based password recovery that revokes other sessions;
- step-up verification for contact, password, and security changes;
- optional invite-code linking during registration; and
- invite-code entry for an authenticated account that is still unlinked.

Success means patients can establish and recover an account without clinic
staff operating the app for them, while Android keeps authentication secrets
out of Room, respects the account/clinical-patient separation, and never sends
an unlinked account into patient-linked screens.

This is deliberately an **auth-only phase**. It does not complete the wider
V13 API migration. Appointment requests, direct-booking retirement, intake
retirement, and the complete active-link route cutover require a later approved
specification before the updated backend and Android app can be released
together.

## Sources of Truth

1. `docs/API_CONTRACT.md`, backend state dated 2026-08-01, specifically:
   - Authentication;
   - policy metadata;
   - `PatientAccountResource` and `/me`;
   - sensitive-change step-up;
   - contact management;
   - patient linking and invitations; and
   - the standard error envelope.
2. `docs/BACKEND_CONTEXT.md`, specifically the account/contact, OTP,
   invitation, patient-link, and Sanctum token lifecycle rules.
3. Current Android V12 code at Git HEAD, which still uses legacy `/login` and
   `/register` requests and routes token presence directly to `MainGraph`.
4. This specification for Android flow order, presentation language, local
   secret handling, and auth-only scope.

The backend documents are user-owned inputs and Android work must not edit
them. Their explicit endpoint appendix currently lists 55 endpoints even
though its arithmetic says 54. This auth-only phase consumes only the routes
listed in this specification and does not attempt the global allowlist cutover.

## Confirmed Assumptions and Product Decisions

1. The app and backend have not been deployed, so Android will replace the
   legacy auth flow without compatibility adapters or legacy routes.
2. Registration is two-stage:
   - request and verify an OTP to obtain a short-lived registration token;
   - submit the registration token with identity, password, policy versions,
     optional invite code, and installation metadata.
3. Registration creates a patient-role account, not a clinical `Patient`.
4. The verified registration contact becomes the primary login contact. Email
   and phone are equally valid primary contacts.
5. The registration form uses structured name fields matching the contract:
   first name, optional middle name, and last name.
6. A password and password confirmation are part of the final registration
   form. Passwords must contain at least 12 characters; Android does not invent
   additional composition rules.
7. Policy metadata is loaded from `GET /auth/policies`. Android submits only
   the versions displayed to and accepted by the patient. Policy URLs are
   opened externally with a standard Android view intent; no browser dependency
   is required.
8. Both policy checkboxes must be independently selected before registration
   can be submitted. Preselected consent is forbidden.
9. An optional secondary contact is collected in the final form only when it
   is the other contact type: phone after email registration or email after
   phone registration.
10. The backend cannot register a secondary contact in `/auth/register`.
    Android therefore keeps it only in active flow memory, creates the account
    first, and then offers the authenticated contact-verification flow. Account
    creation remains successful if the patient skips or fails secondary-contact
    verification.
11. Because `/account/contacts/otp` requires step-up, verifying that optional
    secondary contact requires:
    - OTP to the current primary contact for step-up; then
    - OTP to the proposed secondary contact.
    The UI explains this before starting and provides **Skip for now**.
12. Invitation codes are opaque, case-preserving strings. Android trims outer
    whitespace but never parses, normalizes, or logs them.
13. A valid invitation code submitted during registration may return a linked
    account. Without a valid invitation, the account remains unlinked.
14. An authenticated unlinked or pending-review account can still enter an
    invitation code and complete the invitation OTP flow.
15. This phase displays current manual-link status but does not add submission
    of a new `/patient-link-requests` request. Manual link-request UX is
    deferred with the broader account-only experience.
16. Authenticated routing has three outcomes:
    - `linked` → existing `MainGraph`;
    - `unlinked` → limited account-access destination with invite entry;
    - `pending_review` → limited destination showing pending review while
      retaining invite entry.
17. Token presence alone is not proof of a valid session. Cold start uses a
    session-resolution screen and validates a stored token through `GET /me`
    before routing.
18. A stable random installation UUID is generated once per app installation.
    It is not an Android hardware identifier and requires no device permission.
19. The bearer token and installation UUID remain in the existing encrypted
    SharedPreferences facility. They are never stored in Room.
20. Passwords, OTPs, registration tokens, step-up tokens, invite codes, and raw
    proposed contacts are held only in the active ViewModel flow. Process death
    restarts the sensitive flow rather than persisting those values.
21. `device_name` is a non-secret patient-readable label derived from Android
    manufacturer/model information. `installation_id` is sent whenever the
    endpoint accepts it.
22. Normal app reopening does not call login. A valid stored token remains the
    session until logout, revocation, expiry, or a 401 response.
23. Explicit logout revokes the current token, clears it locally, preserves the
    installation UUID, and returns to Welcome.
24. A 401 clears the current token and returns to Welcome with neutral
    session-expired guidance. It does not clear the installation UUID.
25. Password recovery is presented as contact → OTP → new password. Because
    the backend verifies OTP and resets the password in one request, Android
    carries the six-digit code to the password screen in memory and submits
    code plus new password together. `INVALID_OTP` returns the patient to the
    OTP step without discarding the new-password fields unnecessarily.
26. Successful recovery saves the returned current-device token and routes by
    returned `link_status`. Other sessions are considered revoked by the
    backend.
27. A login contact field accepts either email or phone. Android performs only
    conservative format checks and leaves canonical normalization to the
    backend.
28. If login returns `step_up_required = false`, Android saves the returned
    token immediately. If true, Android navigates to login OTP verification.
29. Enumeration-safe login and recovery copy never confirms whether an account
    exists. Login-step copy says a code was sent **if the details match an
    account**.
30. OTP codes are always exactly six digits. Server expiry, attempt limits, and
    rate limits remain authoritative.
31. Resend starts a new challenge and replaces the active challenge ID. An old
    OTP is never submitted against a replacement challenge.
32. `PatientAccountResource` is mapped into distinct account and optional
    `linkedPatient` domain data. Clinical data inside `linked_patient` is
    read-only.
33. The existing Edit Profile experience is narrowed to account first and last
    name. Middle name, account date of birth, and linked clinical demographics
    are displayed but not editable because `/me` does not permit those writes.
34. Contact changes live in Account & Security rather than being sent through
    `PATCH /me`.
35. Step-up tokens are placed only in the `X-Step-Up-Token` header, are kept in
    memory, and are discarded after the single mutation attempt for which they
    were obtained.
36. Adding a contact, changing primary contact, removing a contact, and changing
    password each start a fresh step-up flow. Android does not reuse a token
    across mutations.
37. Setting a new primary contact and changing a password warn that other
    sessions may be revoked. The current successful session remains active
    unless the backend returns 401.
38. This auth-only phase does not expose clinical routes to unlinked accounts.
    It also does not migrate the rest of the app to the new 55-route contract.

Approval of this specification confirms these decisions.

## Backend Contract Consumed by Android

### Public endpoints

```text
POST /api/v1/auth/registration/otp
POST /api/v1/auth/registration/verify
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/login/verify
POST /api/v1/auth/password-recovery/otp
POST /api/v1/auth/password-recovery/verify
GET  /api/v1/auth/policies
```

### Authenticated endpoints

```text
POST   /api/v1/logout
POST   /api/v1/logout-all
GET    /api/v1/me
PATCH  /api/v1/me
POST   /api/v1/auth/step-up/otp
POST   /api/v1/auth/step-up/verify
POST   /api/v1/auth/password
GET    /api/v1/account/contacts
POST   /api/v1/account/contacts/otp
POST   /api/v1/account/contacts/verify
PATCH  /api/v1/account/contacts/{contact}/primary
DELETE /api/v1/account/contacts/{contact}
GET    /api/v1/account/link
POST   /api/v1/patient-invitations/acceptance/otp
POST   /api/v1/patient-invitations/accept
```

`POST /patient-link-requests` and
`GET /patient-link-requests/current` are not consumed in this phase. The
`pending_review` state is still decoded and displayed when returned by `/me`.

### Core domain outcomes

```text
RegistrationContactChallenge
    challengeId: String
    expiresAt: String

RegistrationProof
    token: String
    expiresAt: String
    contactType: ContactType

LoginOutcome
    OtpRequired(challengeId, expiresAt)
    Authenticated(session)

AuthenticatedSession
    token: String
    account: PatientAccount

PatientAccount
    id: Int
    name: String
    firstName: String?
    middleName: String?
    lastName: String?
    email: String?
    phone: String?
    role: String
    dateOfBirth: String?
    linkStatus: PatientLinkStatus
    privacyPolicyVersion: String?
    privacyAcceptedAt: String?
    linkedPatient: LinkedPatient?

LinkedPatient
    patientNumber: String
    fullName: String
    dateOfBirth: String?
    gender: String?
    occupation: String?
    address: String?
    phone: String?
    contactEmail: String?
```

Safe enum policy:

```text
ContactType
    EMAIL
    PHONE

PatientLinkStatus
    LINKED
    PENDING_REVIEW
    UNLINKED
    UNKNOWN
```

Unknown link status fails closed into the limited account-access destination;
it never grants access to `MainGraph`.

### Error handling

All new auth/account repositories decode the standard envelope:

```json
{
  "error": {
    "code": "MACHINE_READABLE_CODE",
    "message": "Patient-safe message",
    "details": {}
  }
}
```

Android uses machine codes for behavior and the patient-safe message for
fallback presentation. At minimum, focused behavior covers:

```text
INVALID_OTP
OTP_ATTEMPT_LIMIT_REACHED
OTP_RATE_LIMIT_REACHED
CONTACT_ALREADY_OWNED
INVITATION_INVALID
ACCOUNT_ALREADY_LINKED
PATIENT_ALREADY_LINKED
CONTACT_NOT_VERIFIED
LAST_CONTACT_REMAINING
ACTIVE_PATIENT_LINK_REQUIRED
```

Unknown codes remain safe generic failures. Error bodies, contacts, OTPs,
tokens, and credentials are never logged.

## Patient Experience

### Session resolution

App launch begins at a non-interactive session-resolution destination:

1. No token → Welcome.
2. Stored token → `GET /me`.
3. Success with `linked` → Main.
4. Success with `unlinked`, `pending_review`, or unknown → limited account
   access.
5. 401 → clear token and show Welcome.
6. Offline/transient failure → show retry and an explicit **Sign out** option;
   do not assume the session is invalid.

The existing chat/unread request must not run before session resolution and
must not run for an unlinked account.

### Welcome

Welcome contains two primary choices:

```text
Sign in
Create account
```

It contains no email-only assumptions and no direct API mutation.

### Sign in

Flow:

```text
Contact → Password → optional OTP → route by link status
```

- Contact accepts a verified email or phone.
- **Forgot password?** is available from the password step.
- Login sends `device_name` and the stable `installation_id`.
- Trusted response saves token and skips OTP.
- Untrusted response opens six-digit OTP verification.
- Back navigation preserves non-secret contact text but clears password when
  leaving the sign-in flow.
- Loading disables duplicate submissions.

### Create account

Flow:

```text
Choose Email or Phone
→ Enter contact
→ Verify six-digit OTP
→ Complete account details
→ optional secondary verification
→ route by link status
```

Final details contain:

- first name;
- optional middle name;
- last name;
- date of birth;
- password;
- password confirmation;
- optional secondary contact of the other type;
- optional invitation code;
- unchecked Terms acceptance;
- unchecked Privacy Policy acceptance.

Date of birth uses a date picker and must be before today. Android does not
impose an undocumented minimum age.

The Terms and Privacy labels contain separately tappable links. Registration
is disabled until metadata is loaded and both boxes are selected. A policy
load failure shows retry and never substitutes stale hard-coded versions.

Registration sends the verified registration token, server-provided policy
versions, optional invite code, and installation metadata. It never resends
the raw primary contact because the proof token owns that identity.

### Optional secondary contact

After account creation, a supplied secondary contact presents:

```text
Add another sign-in method?
Verify your current contact, then verify the new contact.
```

Actions:

```text
Verify now
Skip for now
```

**Verify now** runs sensitive-change step-up against the primary contact,
requests a new-contact OTP with the resulting header token, and verifies the
secondary contact. **Skip for now** discards the raw draft and routes normally.

Failure never rolls back the already-created account.

### Password recovery

Flow:

```text
Contact → OTP entry → New password + confirmation → route by link status
```

Recovery copy is enumeration-safe. The final request includes challenge ID,
OTP, password fields, `device_name`, and `installation_id`. Success explains
that other devices were signed out.

### Limited account access

Unlinked and pending-review accounts do not enter the existing patient-linked
main graph.

The limited destination shows:

- account name and primary contact;
- link status;
- invitation-code entry;
- Account & Security entry; and
- Sign out.

Pending review uses neutral copy such as **Clinic review pending**. It still
allows a valid invitation code because invitation acceptance can establish the
active link first.

Invitation flow:

```text
Enter invitation code
→ request invitation OTP
→ verify OTP
→ refresh /me
→ Main when linked
```

Invalid invitations retain the screen and show patient-safe guidance without
revealing another patient's existence.

### Account & Security

This destination is reachable from linked Profile and limited account access.
It contains:

- verified/pending masked contacts;
- primary contact indication;
- Add contact;
- Make primary for eligible verified secondary contacts;
- Remove for removable contacts;
- Change password;
- Sign out this device; and
- Sign out all devices.

Raw saved contacts are never displayed because the API intentionally returns
masked values.

Each sensitive mutation uses a fresh flow:

```text
Request step-up OTP to current primary
→ verify OTP
→ receive in-memory X-Step-Up-Token
→ perform exactly one mutation
→ discard token
```

Add contact then performs its separate OTP to the proposed contact. Setting a
primary contact is allowed only for verified contacts. Removing the last
verified contact is blocked by the server and explained without corrupting
local state.

Changing password requires current password, a new minimum-12-character
password, and confirmation after step-up. Success keeps the current token and
reports that other sessions were revoked.

### Profile integration

Profile displays:

- account name/contact from account fields;
- linked patient number and clinical demographics only from `linked_patient`;
- clinical demographics as read-only values; and
- Account & Security navigation.

Edit Profile sends only `first_name` and `last_name`. It does not send email,
phone, address, date of birth, occupation, gender, full name, or clinical email
through `PATCH /me`.

## Scope

### In Scope

- Replace legacy Login/Register routes, repository calls, states, and screens.
- Add Welcome and session-resolution destinations.
- Add registration contact choice, OTP, details, policy, and invitation flow.
- Add trusted/untrusted login outcomes.
- Add password recovery.
- Persist installation ID and returned Sanctum token securely.
- Decode `PatientAccountResource`, `linked_patient`, and link status.
- Add limited unlinked/pending destination with invite-code acceptance.
- Add contact management and step-up-protected security mutations.
- Add password change, current-device logout, and logout-all.
- Narrow Profile editing to allowed account fields.
- Prevent unlinked/unknown accounts from entering `MainGraph`.
- Adopt the new error envelope for auth/account repositories.
- Add focused unit and Compose/navigation tests.
- Update the auth/account portions of `CONTEXT.md` after implementation.

### Deferred / Out of Scope

- Appointment request list, detail, availability, creation, and cancellation.
- Migration away from direct `POST /appointments`.
- Removal of patient appointment-type selection.
- Confirmed-appointment contract changes.
- Patient-intake route and UI retirement.
- Appointment contact-note retirement.
- Full active-link audit across every clinical repository and screen.
- Global API route allowlist migration from V12 to the complete V13 list.
- New manual patient-link request submission UI.
- Backend code, migrations, jobs, configuration, or documentation edits.
- Admin/Filament account and link-review experiences.
- Biometric login, passkeys, social login, passwordless login, or SMS
  auto-reading.
- Push notifications or invitation deep links.
- Persisting auth workflow drafts across process death.
- Room schema or entity changes.
- New dependencies.

The deferred breaking API changes must receive a separate specification before
the app is considered compatible with the entire updated backend contract.

## Tech Stack

- Kotlin 2.3.0 with AGP 9.2.1 built-in Kotlin.
- Jetpack Compose and Material 3.
- Navigation Compose type-safe `@Serializable` routes.
- Hilt.
- Retrofit, OkHttp, and Kotlinx Serialization.
- EncryptedSharedPreferences backed by Android Keystore.
- `StateFlow` with sealed UI state.
- JUnit 5, MockK, Turbine, coroutines-test, MockWebServer, and Compose UI tests.

No dependency or build-plugin change is required.

## Commands

Run from the repository root in PowerShell:

```powershell
.\gradlew assembleDebug
.\gradlew testDebugUnitTest
.\gradlew assembleDebugAndroidTest
.\gradlew ktlintCheck
.\gradlew lintDebug
.\gradlew connectedDebugAndroidTest
```

`connectedDebugAndroidTest` is required only when an emulator or device is
available. Every implementation change must end with:

```powershell
.\gradlew assembleDebug
```

## Project Structure

Expected production areas:

```text
app/src/main/java/com/eyecare/app/data/local/
    TokenManager.kt                   secure token + installation metadata
app/src/main/java/com/eyecare/app/data/remote/api/
    AuthApiService.kt                 public/session auth endpoints
    AccountApiService.kt              contacts, link, invite, security
app/src/main/java/com/eyecare/app/data/remote/dto/
    AuthDtos.kt
    AccountDtos.kt
app/src/main/java/com/eyecare/app/data/repository/
    AuthRepositoryImpl.kt
    AccountRepositoryImpl.kt
app/src/main/java/com/eyecare/app/domain/model/
    PatientAccount.kt
    AccountSecurity.kt
app/src/main/java/com/eyecare/app/domain/repository/
    AuthRepository.kt
    AccountRepository.kt
app/src/main/java/com/eyecare/app/presentation/auth/
    welcome, login, registration, OTP, recovery, session resolution
app/src/main/java/com/eyecare/app/presentation/account/
    limited access, invitation, contacts, security
app/src/main/java/com/eyecare/app/presentation/profile/
    linked read-only profile integration
app/src/main/java/com/eyecare/app/presentation/navigation/
    auth/account/main graph routing
```

Expected focused tests mirror those areas under:

```text
app/src/test/java/com/eyecare/app/
app/src/androidTest/java/com/eyecare/app/presentation/auth/
app/src/androidTest/java/com/eyecare/app/presentation/account/
```

Exact file boundaries and whether shared OTP presentation becomes one
reusable component belong in Phase 2 architecture planning.

## Code Style

DTOs remain serialization-only. Polymorphic transport outcomes map to explicit
domain outcomes at the repository boundary:

```kotlin
@Serializable
data class LoginResponseDto(
    val data: LoginDataDto,
)

@Serializable
data class LoginDataDto(
    @SerialName("step_up_required") val stepUpRequired: Boolean,
    @SerialName("challenge_id") val challengeId: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val token: String? = null,
    val user: PatientAccountDto? = null,
)

private fun LoginDataDto.toDomain(): LoginOutcome =
    if (stepUpRequired) {
        LoginOutcome.OtpRequired(
            challengeId = requireNotNull(challengeId),
            expiresAt = requireNotNull(expiresAt),
        )
    } else {
        LoginOutcome.Authenticated(
            token = requireNotNull(token),
            account = requireNotNull(user).toDomain(),
        )
    }
```

Conventions:

- DTO field names mirror the API through `@SerialName`.
- DTOs never enter ViewModels or composables.
- Unknown link status maps to `UNKNOWN` and fails closed.
- ViewModel state is a sealed interface exposed as `StateFlow`.
- Credentials and OTPs are never included in state `toString`, logs, analytics,
  saved state, or exception messages.
- Repository functions describe outcomes rather than screens.
- UI labels say **Sign in**, **Create account**, **verification code**, and
  **Account & Security** rather than backend implementation terms.
- Contact and phone normalization remains backend-authoritative.

## Testing Strategy

### Contract and repository tests

- Encode every public auth request with exact field names.
- Decode registration challenges/proofs and policy metadata.
- Decode both login response variants and reject incomplete variants safely.
- Decode complete linked and unlinked `PatientAccountResource` payloads.
- Map unknown `link_status` to a fail-closed domain value.
- Verify token saving occurs only after authenticated success.
- Verify installation ID is stable and survives token clearing.
- Verify passwords, codes, invitation codes, and proof tokens are never written
  to local persistence.
- Verify `X-Step-Up-Token` is present only on protected mutations.
- Verify standard error-envelope code/message mapping.
- Verify logout clears token after a successful revocation and has a defined
  local fallback for network failure in Phase 2.

### ViewModel tests

- Welcome navigation has no side effects.
- Registration validates the chosen contact type.
- Resend replaces challenge ID.
- OTP local validation requires six digits.
- Registration details enforce names, past DOB, 12-character password,
  confirmation, and both policy checks.
- Policy load/retry and version submission are covered.
- Optional secondary contact never blocks account success.
- Trusted login skips OTP; untrusted login requires it.
- Duplicate submissions are guarded.
- Password recovery returns invalid OTP to the correct step.
- Auth success routes linked/unlinked/pending/unknown correctly.
- Session resolution distinguishes 401 from transient failure.
- Invitation acceptance refreshes account/link state.
- Every sensitive mutation obtains and discards a fresh step-up token.
- Contact errors retain the authoritative contact list.
- Password change success clears password fields.

### Compose and navigation tests

- Welcome exposes Sign in and Create account.
- Email and phone registration variants render correct keyboards/labels.
- OTP entry, expiry, resend, loading, attempt-limit, and rate-limit states.
- Final registration form contains every required and optional field.
- Terms and Privacy links are independently actionable.
- Password fields provide visibility controls without exposing values by
  default.
- Trusted sign-in reaches the correct graph without showing OTP.
- Recovery and optional-secondary flows follow their specified order.
- Unlinked, pending, unknown, and linked navigation boundaries.
- Invite-code acceptance from limited account access.
- Account & Security masks contacts and labels primary state.
- Clinical `linked_patient` values are read-only.
- System Back cannot accidentally submit or retain a completed secret step.
- Bottom navigation is absent throughout auth and limited-account flows.

### Regression and release-gate tests

- Existing linked users with a valid token still reach Main after `/me`.
- Current 401 event behavior clears only the token.
- No chat or linked-only request runs during session resolution or for an
  unlinked account.
- Legacy `/login` and `/register` are absent from auth Retrofit declarations.
- Auth/account focused unit tests, Android-test compilation, ktlint, lint, and
  debug assembly pass.
- Release remains blocked on a separate specification for the deferred V13
  appointment/intake/global-route changes.

## Boundaries

### Always

- Treat the explicit backend payloads as authoritative.
- Preserve the account/clinical-patient boundary.
- Keep linked clinical demographics read-only.
- Validate a stored session with `/me` before entering a graph.
- Fail closed for unknown link status.
- Use the stable random installation ID, never a hardware identifier.
- Keep bearer tokens only in encrypted preferences.
- Keep passwords, OTPs, invite codes, and proof tokens in memory only.
- Use step-up for every protected mutation.
- Submit current server-provided policy versions.
- Map DTOs to domain at repository boundaries.
- Use enumeration-safe copy.
- Run focused tests and `assembleDebug` after implementation changes.
- Preserve the user-owned backend documents and unrelated worktree changes.

### Ask First

- Change an endpoint or payload.
- Add a dependency or build plugin.
- Persist any new authentication or identity value.
- Change the server's password, OTP, expiry, attempt, or rate-limit rules.
- Allow an unlinked/unknown account into Main.
- Add manual patient-link request submission.
- Add biometrics, passkeys, social login, or auto-reading SMS.
- Expand this phase into appointment, intake, or full-route migration.

### Never

- Call legacy `/login` or `/register`.
- Store credentials, OTPs, invite codes, registration proofs, or step-up proofs
  in Room, SavedStateHandle, logs, analytics, or crash metadata.
- Log auth/account response bodies.
- Infer account existence from enumeration-safe responses.
- Treat token presence alone as a valid session.
- Treat an account record as a clinical Patient.
- Edit `linked_patient` fields through `/me`.
- Send contact changes through `PATCH /me`.
- Reuse a single-use step-up token.
- Trust unknown link status as linked.
- Claim that this auth-only phase completes the full V13 API alignment.

## Success Criteria

### Registration and policies

- [ ] Welcome provides Sign in and Create account.
- [ ] Patients can choose email or phone as their primary contact.
- [ ] Registration contact is OTP-verified before details submission.
- [ ] Account details use structured names, past DOB, and a minimum-12-character
  confirmed password.
- [ ] Current Terms and Privacy metadata comes from `/auth/policies`.
- [ ] Both policy checkboxes are required and never preselected.
- [ ] Optional invitation code is submitted unchanged apart from trimming.
- [ ] Optional secondary contact is verified only after account creation and
  never blocks account success.

### Login, recovery, and sessions

- [ ] Email and phone contacts can sign in.
- [ ] Trusted installations receive and save direct-token login responses.
- [ ] New/untrusted installations complete login OTP.
- [ ] Normal app reopening validates the stored token without asking for a
  password.
- [ ] Password recovery resets the password, saves the returned current-device
  token, and communicates revocation of other sessions.
- [ ] Logout and 401 clear the token but preserve installation identity.
- [ ] Transient bootstrap failures offer retry without silently deleting a
  potentially valid session.

### Linking and navigation

- [ ] Linked accounts reach Main.
- [ ] Unlinked, pending-review, and unknown accounts remain outside Main.
- [ ] Unlinked/pending accounts can enter and verify an invitation code.
- [ ] Successful invitation acceptance refreshes `/me` and reaches Main only
  when the backend reports `linked`.
- [ ] No chat or patient-linked request starts for an unlinked account.

### Security and profile

- [ ] Contacts are listed only in masked form.
- [ ] Adding, making primary, removing contacts, and changing password require
  fresh step-up verification.
- [ ] Protected mutations send proof in `X-Step-Up-Token`.
- [ ] Password change requires current password and confirmation and reports
  other-session revocation.
- [ ] Profile maps account fields separately from nullable `linked_patient`.
- [ ] Edit Profile sends only account first and last name.
- [ ] No auth secret is persisted outside encrypted token/installation storage.

### Quality and scope integrity

- [ ] New auth/account DTOs map to serialization-free domain models.
- [ ] Standard error codes drive OTP/contact/link behavior safely.
- [ ] Unknown status/error values fail safely.
- [ ] Legacy auth endpoints and email-only assumptions are removed.
- [ ] Focused/full unit tests, Android-test compilation, ktlint, lint, and debug
  assembly pass.
- [ ] Deferred appointment, intake, and full-route migration remains visibly
  documented and is not accidentally claimed complete.

## Open Questions

None blocking Phase 1. Approval confirms the assumptions above, including the
two-OTP optional-secondary flow, combined recovery verification/reset request,
invite-only unlinked experience for this phase, and explicit deferral of the
remaining V13 backend cutover.

## Phase Gate

This Phase 1 specification was approved by the project owner on 2026-08-01.

Create the Phase 2 technical implementation plan for separate approval. Do not
create Phase 3 tasks and do not modify Android production code until each
subsequent gate is separately approved.
