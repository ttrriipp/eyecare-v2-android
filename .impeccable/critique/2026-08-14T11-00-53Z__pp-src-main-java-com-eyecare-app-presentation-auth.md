---
target: the auth screens
total_score: 18
max_score: 40
na_heuristics: 
p0_count: 2
p1_count: 2
timestamp: 2026-08-14T11-00-53Z
slug: pp-src-main-java-com-eyecare-app-presentation-auth
---
Method: dual-agent (A: design-review subagent · B: detector+evidence subagent)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2/4 | OTP countdown/cooldown is clear, but the Verify/Continue button's `loading` is wired to `isResending` on Login/Register/Recovery — tapping Resend visibly spins the wrong button. |
| 2 | Match System / Real World | 3/4 | +63 phone prefix and PH OTP conventions fit local context well; undercut by a raw fallback error string on the session gate ("Something went wrong"). |
| 3 | User Control and Freedom | 1/4 | System Back silently discards flow progress on every multi-step screen; `RegisterDetailsStep` has no in-app back control at all. |
| 4 | Consistency and Standards | 1/4 | Lens Cyan used as text color throughout despite DESIGN.md's explicit ban; 32dp auth margins vs. the documented 24dp; OTP field `enabled` state differs between Login and Register/Recovery. |
| 5 | Error Prevention | 2/4 | Good live enable/disable gating on buttons and DOB date-range limits; no proactive password-strength guidance beyond a post-submit length error. |
| 6 | Recognition Rather Than Recall | 2/4 | Phone number persists across steps, but returning from OTP to credentials on Login clears the typed password, forcing re-entry. |
| 7 | Flexibility and Efficiency | 2/4 | Trusted-device OTP skip is a real efficiency win; no autofill/OTP-paste affordance visible. |
| 8 | Aesthetic and Minimalist Design | 2/4 | Individual OTP/phone/password steps are clean and single-focus; the registration Details step is a dense 9-field/2-checkbox wall that breaks the pattern. |
| 9 | Error Recovery | 2/4 | Centralized error-copy mapping gives good patient-safe messages for known codes; the session gate bypasses it and can surface a raw exception message. |
| 10 | Help and Documentation | 1/4 | No "Need help?" / contact-clinic escape hatch anywhere in the auth flow. |
| **Total** | | **18/40** | **Poor** |

## Design Specificity Verdict

**LLM assessment**: This does not read as authored for Eyecare/Padilla Optical Clinic. Every step-heading section in the Login, Register, and Password Recovery screens carries the literal source comment `// Maya pattern: bold conversational heading, generous spacing` — the flow is explicitly borrowed from Maya, a Philippine fintech e-wallet, not composed for a "calm care companion" optical clinic. No auth screen ever names Padilla Optical Clinic in copy; the only clinic identity is a static logo image. The decorative top gradient bar claims (in its own code comment) to "mirror the WelcomeScreen hero gradient," but WelcomeScreen has no gradient at all — evidence this was carried over from a different template rather than deliberately designed for this product. This could be the login flow for any generic phone-OTP consumer app.

**Deterministic scan**: The bundled CLI detector (`detect.mjs`) returned exit code 0 and an empty `[]` — but this is **not** a clean bill of health. Its `SCANNABLE_EXTENSIONS` set covers `.html/.htm/.css/.scss/.sass/.less/.jsx/.tsx/.js/.ts/.vue/.svelte/.astro` only; it does not include `.kt`. Every file under the auth folder is Kotlin, so 0 files were scannable and the empty result is a no-op, not a passing audit. Assessment B substituted a manual, citation-backed pattern scan of every auth file against the app's own theme tokens (`ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `Shape.kt`). It found, among other things: three raw hex color literals hand-declared in `AuthStepScaffold.kt` (one duplicating the `primary` token as an independent literal, two matching no token anywhere else in the system); every `OutlinedTextField` across all three flows skips the app's own declared 12dp input-field shape token in favor of Material3's default; a 475dp hardcoded logo size and other dimensions with no relationship to the established 2/4/8/12/16/20/24/32dp spacing scale; zero uses of `Modifier.semantics{}`/`liveRegion`/`Role.*` anywhere in the folder, so dynamic error and loading text is never announced to TalkBack; and a real (not template-borrowed) strength — `PolicyConsent.kt` correctly isolates the tap target for its inline "Terms of Service"/"Privacy Policy" link rather than making the whole consent label clickable, matching DESIGN.md's specific rule on this exact pattern precisely.

**Visual overlays**: Not applicable. This is a native Android/Compose surface with no dev-server URL or browser-injectable page, so the browser-overlay step from the critique flow doesn't apply here. Live on-device capture of these specific screens was also deliberately skipped: reaching the pre-authentication Welcome/Login/Register screens on the connected physical device would require logging the user's real, currently-authenticated session out on their personal phone, which was not authorized for this evidence-gathering pass. Both assessments worked from source.

## Overall Impression

The individual interaction mechanics — OTP countdown/resend cooldown, IME-aware scrolling, live-gated buttons, the consent link's precise tap isolation — are handled with real care. But the flow as a whole is a reskinned template, not an Eyecare surface: the borrowed "Maya pattern" comment is the tell, and the symptoms follow from it — Lens Cyan used as text color everywhere DESIGN.md says not to, a decorative gradient bar with no relationship to the actual palette or dark theme, and a registration step that abandons the single-focus discipline the rest of the flow otherwise respects. The single biggest opportunity: stop treating the auth screens as scaffolding to fill in later and design them from DESIGN.md's own vocabulary — border-led cards, Deep Vision Navy for the one moment that deserves gravity, `accentText` for every cyan word — the way Home and the appointment surfaces already do.

## What's Working

- **`PolicyConsent.kt`'s link tap-target isolation.** It computes the exact glyph bounds of "Terms of Service"/"Privacy Policy" via `TextLayoutResult.getOffsetForPosition` rather than making the whole consent sentence one giant tap target — this is the one place in the auth flow that visibly followed a specific DESIGN.md rule (and the one place cyan text correctly uses `accentText`, not `primary`).
- **OTP expiry/resend handling (`OtpFields.kt`).** A genuinely well-built combined countdown + absolute-time display and an independent, real 30-second resend cooldown — the actual edge case (resend spam, "is my code still valid?") is handled properly, not hand-waved.
- **Keyboard-safe layout (`AuthStepScaffold.kt`).** IME padding plus vertical scroll is applied consistently on every step, matching DESIGN.md's requirement that keyboard-compressed forms scroll instead of clip.

## Priority Issues

**[P0] Lens Cyan is used as text/icon color on nearly every low-emphasis auth action, which DESIGN.md itself documents as a WCAG failure.**
Why it matters: "Forgot password?", "Create account", "Sign in", "Create one", "Sign out", and "Resend code" all fall back to Material3's default `TextButton`/`OutlinedButton` content color, which resolves to `MaterialTheme.colorScheme.primary` (`#29B6F6`). DESIGN.md states this exact hex measures 2.0-2.3:1 as text on any light surface — "below the 3:1 floor" — and explicitly names `accent-text` as the required substitute. This is the single most widespread defect in the flow: it touches nearly every secondary action across all three screens, in the first experience a new or anxious patient has with the product.
Fix: pass `colors = ButtonDefaults.textButtonColors(contentColor = EyecareColors.current.accentText)` (and the outlined-button equivalent) everywhere a low-emphasis auth action renders, starting with `AuthOutlinedButton` in `AuthStepScaffold.kt` and the bare `TextButton`s in `LoginScreen.kt`, `RegisterScreen.kt`, `SessionGateScreen.kt`, and `OtpFields.kt`'s "Resend code."
Suggested command: `/impeccable audit` (accessibility pass), then `/impeccable harden`.

**[P0] System Back silently discards verified progress; the registration Details step has no back control at all.**
Why it matters: Login, Register, and Recovery each run as internal ViewModel-driven steps inside a single nav destination. The visible TopAppBar arrow calls the ViewModel's own `back()`, but nothing intercepts hardware/gesture Back, so it pops the whole nav entry straight to Welcome — discarding a just-verified OTP challenge and every typed field with zero warning. `RegisterDetailsStep` is the worst case: its `AuthStepScaffold` call passes no `onBack` at all, so system Back is the *only* exit, and it deletes all nine fields silently. To a first-time patient this reads as the app crashing, not as an intentional exit.
Fix: add a `BackHandler` per step that calls the same `viewModel::back()` the visible arrow uses, and give the Details step at minimum a path back to the OTP step (or a discard-confirmation dialog) instead of zero back affordance.
Suggested command: `/impeccable harden`.

**[P1] The registration Details step bundles 9 fields and 2 consent checkboxes into one unbroken screen, breaking the flow's own single-focus pattern.**
Why it matters: Every other step in Login/Register/Recovery is single-purpose (one phone field, one OTP row, one password field). `RegisterDetailsStep` alone asks for first/middle/last name, date of birth, email, password, confirm password, an optional invitation code, and two legal consents in one scroll — 6 of 8 cognitive-load checklist items fail here specifically (single focus, chunking, one-thing-at-a-time, minimal choices, working-memory demand, progressive disclosure). The step's own copy ("A few details") undersells what's actually being asked.
Fix: split into 2-3 sub-steps (Identity → Security → Invitation & Consent), consistent with the one-decision-per-screen discipline the rest of the flow already establishes.
Suggested command: `/impeccable layout`, then `/impeccable onboard`.

**[P1] The OTP "Verify"/"Continue" button borrows the Resend action's loading flag — and password recovery's OTP step doesn't actually verify anything at that point.**
Why it matters: On Login, Register, and Recovery, the primary verify/continue button's `loading` state is wired to `state.isResending` rather than a submitting flag of its own — tapping "Resend code" visibly spins the *verify* button, which reads as the wrong thing happening. Compounding this, `PasswordRecoveryViewModel.verifyOtp()` performs no network call at all; the code is only actually checked later inside `resetPassword()`, so recovery's "Continue" button's loading indicator is never reflecting a real verification in flight. This is exactly the highest-anxiety wait-state in the whole flow (did my code work or not?), and the feedback is misleading in three different ways across three screens.
Fix: give each screen's verify/submit action its own `isVerifying`/`isSubmitting` state, decoupled from `isResending`; have recovery's OTP step either genuinely verify server-side or make clear in copy that verification happens at the password-reset step.
Suggested command: `/impeccable harden`, then `/impeccable clarify` for the recovery-step copy.

**[P2] The whole step scaffold is a literal borrowed template ("Maya pattern"), and its one visual flourish hardcodes non-theme colors that break dark mode.**
Why it matters: The in-code comments admitting this was copied from a fintech app are themselves evidence of the design-specificity problem above. Concretely, `AuthStepScaffold.kt` hand-declares three raw hex colors for a decorative gradient bar shown above every auth screen — bypassing `EyecareColors`/`MaterialTheme` entirely even though dark theme is a fully implemented, first-class scheme elsewhere in the app. One of the three literals duplicates the `primary` token instead of referencing it; the other two match nothing else in the palette. The bar's own justifying comment ("mirrors the WelcomeScreen hero gradient") is factually wrong — WelcomeScreen has no gradient — which is itself a tell that this wasn't a deliberate design decision.
Fix: remove the gradient bar, or rebuild it from real DESIGN.md tokens with a defined dark-mode counterpart; while in there, fix the 32dp auth margins back to the documented 24dp, and route every `OutlinedTextField` through the app's own declared 12dp input-field shape token instead of the Material default.
Suggested command: `/impeccable document` (align the code to DESIGN.md) or a fresh `/impeccable shape` pass on the auth flow specifically, then `/impeccable polish`.

## Persona Red Flags

**Jordan (First-Timer)**: Hits the 11-item wall of the Details step immediately after the reassuring, low-effort OTP step — the jump in complexity is jarring and the copy ("a few details") undersells it. If Jordan reconsiders mid-form and taps system Back, the entire registration — including the phone number just verified — vanishes with no warning, which is very likely to read as an app crash rather than an intentional exit.

**Sam (Accessibility-Dependent)**: Every low-emphasis action in the flow ("Forgot password?", "Create account", "Sign in", "Create one", "Sign out", "Resend code") renders in a cyan DESIGN.md itself documents as failing WCAG AA. Zero uses of `Modifier.semantics{}` or `liveRegion` exist anywhere in the auth folder, so dynamically-appearing error text and loading copy are never proactively announced. The Details step's five section labels ("Personal details," "Account security," etc.) are styled text only with no heading semantics, so a TalkBack user has no way to jump between the step's five sub-groups on the single longest, most field-dense screen in the flow. The date-of-birth field's "open picker" interaction is implemented as raw pointer-input hit-testing rather than a standard clickable modifier, so it carries no discoverable "double-tap to activate" semantics at all.

**Anxious-Patient (Casey-adjacent, clinic-specific)**: Nothing in the auth copy ever says "Padilla Optical Clinic" — a patient handing over a password and date of birth for the first time gets no textual confirmation this is the legitimate clinic app, at the exact moment trust matters most. There is no visible help/support link anywhere in the flow for a stuck OTP or a duplicate-phone conflict. And because registration state lives only in the ViewModel's in-memory state with no persistence, a distracted patient pulled away mid-Details-step (phone call, app backgrounded) risks losing an already-completed OTP verification.

## Minor Observations

- Returning from the Login OTP step back to credentials clears the typed password, forcing needless re-entry on a two-field form.
- `LoginOtpStep` omits `enabled = !state.isResending` on its OTP field while the equivalent Register/Recovery steps include it — inconsistent lockout behavior between otherwise-parallel screens.
- Three different loading-spinner sizes exist for the same "operation in progress" concept (20dp in Register's policy loader, default ~40dp on the session gate, 18dp inside the primary-button spinner) — none sharing a component.
- Required-field marking is inconsistent: Register uses a trailing `"*"` on several labels; Login's password field and every screen's phone field never do, despite phone being required everywhere.
- The registration Details step's date-of-birth picker uses a stock, unthemed `DatePickerDialog`, inconsistent with the app's established practice elsewhere (`AppConfirmationDialog`) of not trusting stock Material dialogs to carry the visual language unmodified.
- `AuthErrorMessages.kt` covers only five known codes; an invalid/expired invitation code during registration has no client-authored copy and falls through to whatever raw message the backend returns.
- The session gate's error mapping is separate, thinner logic than `authErrorMessages()`, so it's the one place in the whole auth flow where a raw exception string can reach the user.

## Questions to Consider

- What if the Details step were split by actual risk/reversibility — identity fields the clinic needs regardless, then security, then the genuinely optional add-ons (invitation code, final consent) — so the form never shows more than 3-4 fields at once?
- What if the clinic's name and a one-line trust statement appeared on Welcome and again at the moment DOB/password is requested, so a first-time patient always has textual grounding for what they're handing over and to whom?
- What if system Back always behaved exactly like the visible in-app back arrow, and the Details step got its own back control, so "I want to fix something" never means "start over from zero"?
- What if "Resend code" carried its own loading/disabled state instead of borrowing the verify button's, and a persistent "Trouble receiving a code? Contact the clinic" link sat at the bottom of every OTP step — turning the single most anxious wait-state in the flow into reassurance instead of silent uncertainty?
- What would this flow look like if it were built from DESIGN.md's own vocabulary from scratch — border-led cards, Deep Vision Navy reserved for the one moment that deserves gravity, `accentText` for every cyan word — instead of the borrowed fintech scaffold it currently is?
