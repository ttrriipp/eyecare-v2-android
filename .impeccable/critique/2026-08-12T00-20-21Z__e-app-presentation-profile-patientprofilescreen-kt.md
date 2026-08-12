---
target: PatientProfileScreen.kt
total_score: 12
max_score: 40
na_heuristics: 
p0_count: 2
p1_count: 2
p2_count: 3
timestamp: 2026-08-12T00-20-21Z
slug: e-app-presentation-profile-patientprofilescreen-kt
---
Method: dual-agent (A: general · B: general)

# Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 1 | No loading state; null patient shows dead-end label with no retry or explanation |
| 2 | Match System / Real World | 2 | Field labels are clinic-system language ("Patient number"), not patient-facing |
| 3 | User Control and Freedom | 1 | Text-only back button violates Android ArrowBack convention; null state has no recovery |
| 4 | Consistency and Standards | 1 | Abandons Identity Header pattern from parent; card lacks border-led treatment |
| 5 | Error Prevention | 1 | No guard against navigating before data is ready; user can land on dead screen |
| 6 | Recognition Rather Than Recall | 2 | Field labels visible but no visual grouping for identity/contact/demographics |
| 7 | Flexibility and Efficiency of Use | 1 | Phone/email are static text with no tap-to-call or tap-to-email |
| 8 | Aesthetic and Minimalist Design | 2 | Minimal by absence rather than intention; no rhythm, no visual anchor |
| 9 | Error Recovery | 0 | Null state shows centered text with no icon, retry, guidance, or empathy |
| 10 | Help and Documentation | 1 | No contextual help; no indication of data freshness or editability |
| **Total** | | **12/40** | **Poor** |

# Design Specificity Verdict

**FAIL — Category-interchangeable.** This screen reads as a generic "view contact details" template. It has no visual DNA from the Eyecare design system. The parent ProfileScreen.kt establishes an Identity Header with avatar, status dot, and branded typography; this sub-screen discards all of it and falls back to a bare headlineLarge name string and a plain Scaffold with a TopAppBar. It violates the DESIGN.md rule explicitly: *"Never show a bare page title ("Profile") when real account identity is available."*

The card lacks the system's border-led treatment (no BorderStroke), the TextButton back affordance is a text-only ghost without the system's icon-chevron or pill-button language, and zero use of EyecareColors means the screen doesn't participate in the brand palette at all.

**Deterministic scan:** CLI detector returned 0 findings. Manual audit found: zero accessibility semantics, 1 off-rhythm spacing value (14dp), and complete absence of DESIGN.md Identity Header compliance.

# Overall Impression

This is a ship-blocker. The screen exists as a functional placeholder that was never designed — it has correct typography roles and null-safe engineering, but it completely fails to participate in the Eyecare design system. The biggest opportunity is also the simplest fix: extract and reuse the Identity Header from the parent ProfileScreen, add the border-led card, and the screen instantly transforms from "generic scaffold" to "branded care companion."

# What's Working

1. **Null-safe field handling.** The ?.takeIf(String::isNotBlank) pattern on every optional field (lines 83-106) prevents empty rows. This is good invisible engineering.

2. **Typography role selection is correct.** headlineLarge for the name, labelMedium/odyLarge for detail rows align with DESIGN.md. Font weights are appropriate.

3. **Scroll behavior.** erticalScroll(rememberScrollState()) ensures the screen handles long content gracefully.

# Priority Issues

## [P0] Identity Header Missing — Design System Violation

**What:** The screen shows a bare Text(patient.fullName) instead of the Identity Header pattern (56dp avatar with initials, Display-role name with ellipsis, status dot + label).

**Why:** DESIGN.md is explicit: *"Never show a bare page title when real account identity is available."* The parent ProfileScreen.kt:290-350 implements this correctly. This sub-screen discards it. For a patient viewing their linked clinic record, the identity header is the primary trust anchor — it confirms "this is your record."

**Fix:** Extract ProfileHeader (or a shared IdentityHeader composable) and use it here. At minimum: avatar circle with initials from ullName, name with TextOverflow.Ellipsis, and a subtitle like "Your clinic record."

**Suggested command:** /impeccable layout

## [P0] Card Lacks Border-Led Treatment

**What:** PatientDetailsCard uses CardDefaults.cardColors(containerColor = surface) but no order = BorderStroke(1.dp, outlineVariant) and no explicit shape.

**Why:** Every card in ProfileScreen.kt uses the border-led pattern. DESIGN.md: *"White cards and soft tonal panels over a warm off-white canvas; fine borders establish most separation."* The card here floats invisibly against the canvas.

**Fix:** Add order = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) and shape = RoundedCornerShape(16.dp).

**Suggested command:** /impeccable polish

## [P1] Null State Is a Dead End

**What:** When patient == null, the screen shows a centered 	itleLarge text with no icon, no retry, no guidance.

**Why:** The parent screen's error state provides ErrorContent with retry. This screen gives the user nothing. Nielsen #9 scores 0/4.

**Fix:** Use ErrorContent with a message like "We couldn't load this patient's profile." Add a retry callback or "Go back" action.

**Suggested command:** /impeccable harden

## [P1] Zero Accessibility Semantics

**What:** No semantics/contentDescription anywhere in the file. TalkBack users get raw text with no structure.

**Why:** Screen reader can't distinguish labels from values, can't group related fields, and the empty state isn't announced as a status.

**Fix:** Add semantics { mergeDescendants = true } with composed contentDescription for each detail row. Add liveRegion for the empty state.

**Suggested command:** /impeccable audit

## [P2] Back Button Violates Android Conventions

**What:** Navigation icon is TextButton(onClick = onBack) { Text("Back") } — text-only, no icon.

**Why:** Android M3 TopAppBar expects IconButton with ArrowBack. Text-only back is iOS convention. Breaks platform consistency.

**Fix:** Replace with IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back") }.

**Suggested command:** /impeccable polish

## [P2] No Tap Actions on Contact Fields

**What:** Phone and email are static text with no click handlers.

**Why:** Most likely action on seeing a phone number is to call it. The companion app should help patients act, not just observe.

**Fix:** Wrap phone in clickable ACTION_DIAL, email in ACTION_SENDTO. Use ccentText for value color to signal interactivity.

**Suggested command:** /impeccable polish

## [P2] Off-Rhythm Spacing

**What:** Detail row padding uses 14dp (line 116) instead of 12dp or 16dp.

**Fix:** Change to 16.dp to match DESIGN.md's card-standard padding.

**Suggested command:** /impeccable layout

# Persona Red Flags

**Jordan (First-Timer):** Lands expecting "my clinic record." Sees bare name and flat list. No avatar, no status confirmation, no "this is your linked record" reassurance. Wonders: *Is this actually my data?* If patient is null, hits dead-end with no recovery.

**Sam (Accessibility):** Zero semantics on any composable. Screen reader reads raw text with no structure, no grouping, no role hints. The TextButton back has no minimum touch target guarantee beyond M3 default.

**Casey (Distracted Mobile):** Text-only back button easy to miss at a glance. No lifecycle observer — if interrupted and returns, data is whatever was passed at navigation time. Long values (address, email) wrap within 14dp padding creating dense blocks.

# Minor Observations

- Card internal padding is horizontal-only (16dp) — vertical padding comes from rows' own 14dp. First label and last value touch card bounds.
- No section heading to anchor the card's purpose. Parent uses "Account", "Care & activity" headers.
- TopAppBar title "Patient profile" uses default Material typography — may not use Outfit as DESIGN.md requires.
- LinkedPatient model doesn't carry link status, so the Identity Header can't show the status dot without additional data.

# Questions to Consider

1. **What is this screen *for*?** Is the patient verifying their data? Calling the clinic? Reporting an error? If purely informational, should it be inline on the parent Profile instead of a separate screen?
2. **Why is the Identity Header abandoned?** The parent builds a beautiful avatar + name + status composition. This screen throws it away. Is there a design reason, or was it just not implemented?
3. **Why no provenance?** Health-adjacent data without a "last updated" timestamp erodes trust. A patient might wonder: *Does the clinic have my old address?*
4. **Would this pass a design critique?** It wouldn't survive the first question: "Where's the avatar?"
