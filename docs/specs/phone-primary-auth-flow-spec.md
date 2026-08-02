# Spec: Phone-Primary Patient Authentication

## Objective

Align the pre-deployment Android app with the current patient authentication
contract. Public authentication uses a verified phone number as the only login
and registration identity:

- Sign in: phone + password → login OTP when the backend requires it → session.
- Create account: phone → registration OTP → account form → session.
- Password recovery remains phone → OTP → new password.

Because the app has not been deployed, obsolete email-based public-auth paths
and compatibility states are removed rather than retained.

The optional email accepted by `/auth/register` remains in the account form as
pending account contact data. It is not offered as a login or registration OTP
identity and is not followed by the obsolete post-registration secondary flow.

## Contract assumptions

1. The backend is authoritative for phone normalization, OTP expiry, rate
   limits, password rules, and duplicate-contact errors.
2. A trusted installation may receive a session directly from `/auth/login`;
   an untrusted installation enters `/auth/login/verify` for the login OTP.
3. `ContactType` remains in the domain because authenticated account-contact
   management still supports email and phone; public registration, login, and
   recovery use phone only.
4. The app is not required to support the removed legacy `/login` or
   `/register` endpoints.

## Commands

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

## Project Structure

- `app/src/main/java/com/eyecare/app/data/remote/dto/AuthDtos.kt` — public
  auth request/response contracts.
- `app/src/main/java/com/eyecare/app/domain/repository/AuthRepository.kt` —
  auth boundary.
- `app/src/main/java/com/eyecare/app/presentation/auth/` — auth state
  machines and Compose screens.
- `app/src/test/java/com/eyecare/app/presentation/auth/` — auth flow tests.
- `docs/specs/` — Android implementation specifications.

## Code Style

Use sealed `StateFlow` states and keep secrets only in the active ViewModel
flow. Public auth calls should expose phone-specific APIs at the repository
boundary instead of accepting a contact-type argument that can reintroduce
email:

```kotlin
suspend fun requestRegistrationOtp(phone: String): Result<OtpChallenge>
```

Account-contact management may continue to use `ContactType` because it is a
separate authenticated feature.

## Testing Strategy

- DTO tests verify the optional `email` request field and phone OTP payload.
- Repository tests verify phone-only registration OTP and login-verify device
  metadata.
- ViewModel tests verify sign-in, registration, and recovery reject/avoid
  email-based public-auth paths and preserve OTP transitions.
- Full `testDebugUnitTest` and `assembleDebug` are required before handoff.

## Boundaries

- Always: follow the updated backend documents, keep OTP/password/registration
  secrets in memory only, use machine-readable API errors, and test each flow.
- Ask first: backend changes, new dependencies, persistence changes, or
  changes to authenticated account-contact management.
- Never: retain legacy public auth endpoints, send email as a login identifier,
  log phone numbers/OTPs/passwords, or store auth secrets in Room.

## Success Criteria

- Registration opens directly to phone entry, requests a phone OTP, verifies
  it, then shows the account form.
- The final registration request sends the verified registration token and an
  optional email field, without a phone field or secondary-contact step.
- Login accepts phone and password only; untrusted login proceeds to OTP and
  trusted login may complete immediately according to the backend response.
- Password recovery copy and input are phone-only.
- No public-auth state, UI, DTO, or repository method permits email login or
  email registration OTP.
- Existing authenticated contact-management email support remains intact.

## Open Questions

None for this adjustment; the updated `BACKEND_CONTEXT.md` and
`API_CONTRACT.md` define the required behavior.
