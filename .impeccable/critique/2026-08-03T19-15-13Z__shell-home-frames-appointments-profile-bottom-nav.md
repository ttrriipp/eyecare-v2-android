---
target: core app shell (Home, Frames, Appointments, Profile, bottom nav)
total_score: 20
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 4
timestamp: 2026-08-03T19-15-13Z
slug: shell-home-frames-appointments-profile-bottom-nav
---
Method: dual-agent (A: design-review general-purpose agent · B: deterministic-scan general-purpose agent)

## Design Health Score

| # | Heuristic | Score | Key Finding |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Good per-screen loading/error/refresh states, but button-level progress is inconsistent (Retry shows no spinner) |
| 2 | Match Between System / Real World | 2 | Home greeting hardcoded to "Good morning" regardless of actual time; Profile shows no patient name anywhere |
| 3 | User Control and Freedom | 3 | Reschedule/cancel both confirmed via dialog; no undo after cancel; unsaved-note discard has no confirmation |
| 4 | Consistency and Standards | 2 | 18 of 94 checked typography call sites fall back to unstyled Material (Roboto) instead of the app's Outfit/DM Sans roles — confirmed independently by both assessments |
| 5 | Error Prevention | 3 | Note length capped with counter, Save disabled until dirty; booking FAB can overlap the last list card on short screens |
| 6 | Recognition Rather Than Recall | 3 | Status consistently paired with text, never color alone, across every surface checked |
| 7 | Flexibility and Efficiency of Use | 1 | No shortcuts, no swipe actions, no persistent filter memory, no quick jump to "today" |
| 8 | Aesthetic and Minimalist Design | 2 | Restrained hairline-border cards read clean, but Home stacks 5 distinct content types in one undivided scroll; app has zero dark-theme support (confirmed: `Theme.kt` defines only `lightColorScheme`, no dark scheme, no dynamic color) |
| 9 | Help Recognize/Diagnose/Recover from Errors | 2 | Errors surface as plain red text mixing raw exception strings into patient-facing copy, no icon, no inline retry |
| 10 | Help and Documentation | 0 | No help affordance, tooltip, or contextual guidance anywhere in the reviewed surfaces |

**Total: 20/40 — Acceptable (significant improvements needed before users are happy)**

## Design Specificity Verdict

**LLM assessment**: This reads as a well-executed generic Material 3 app with a color skin, not a bespoke Padilla Optical Clinic experience. DESIGN.md itself predicts this exact failure: it explicitly calls unstyled Material fallback roles "an unfixed defect, not an alternate voice" — yet the appointment detail headline, frame card names, and status pills across the reviewed surfaces render in Roboto, not the brand's Outfit/DM Sans pairing. Combined with a Profile screen showing zero patient identity (no name, no avatar, no link-status summary) and a Home greeting that's simply wrong most of the day, the app currently feels templated rather than authored for this clinic's patients.

**Deterministic scan**: `detect.mjs` was attempted against the scoped source and returned exit 0 with an empty result — it's a web-markup detector and doesn't apply to Kotlin/Compose, confirmed inapplicable rather than forced. In its place, a grep/read static-analysis pass over the 13 scoped files found: **zero** hard-coded hex colors (the token system is followed cleanly), **zero** dark theme support (`Theme.kt:7-32` defines only a light scheme), **18 of 94** typography call sites using unstyled fallback roles (corroborating the LLM finding independently), **4** icon-only touch targets sized at 36dp against a 48dp platform minimum, and, after verifying every `contentDescription = null` hit in context, **zero** confirmed icon-only controls lacking an accessible label — the initial 27 hits all sit beside a visible text label or inside a composable whose semantics already merge one (a good example of a naive grep count needing verification before it's reported as a real issue).

**No visual overlays**: this is a native Android app with no dev server or browser target, so no browser-injected overlay was possible; all findings are static-source-level.

## Overall Impression

The bones are good — a genuinely restrained, hairline-bordered card language, consistent status-plus-text communication, and a distinctive Visit Ticket component that matches DESIGN.md's spec precisely. But the system that's supposed to make this feel like *Eyecare* rather than *any Material 3 app* isn't being enforced: typography silently degrades on primary content, the app ignores system dark mode entirely, and the two most "personal" moments in the app — the Home greeting and the Profile screen — currently say nothing true or specific about the patient looking at them.

## What's Working

- **Visit Ticket component** (`HomeScreen.kt`): the navy anchor, cyan date tile, and translucent status capsule match DESIGN.md's signature-component spec exactly — this is the app's strongest, most authored moment.
- **Status communication**: every status indicator checked (appointment status, AR badges, fulfillment states) pairs a semantic color with a text label — a real, consistently-applied accessibility strength, not just a lucky instance.
- **Token discipline**: zero hard-coded hex colors found anywhere in the scoped screens — all color goes through the theme system as designed.

## Priority Issues

**[P1] Brand typography silently degrades to Roboto on primary content**
Why it matters: DESIGN.md names exactly two typefaces (Outfit, DM Sans) as core to the "Clear Care Companion" identity, and explicitly flags this exact failure mode as an unfixed defect. It's shipping anyway — appointment titles, frame names, and status pills intermittently render in system Roboto instead of the brand faces, on the very surfaces DESIGN.md calls "visual authority." Confirmed independently by both the design review and the deterministic scan (18 of 94 checked call sites).
Fix: extend `EyecareTypography` in `Type.kt` to cover `titleLarge`, `titleSmall`, `labelSmall`, and `bodyLarge`, or stop referencing those roles from screen code.
Suggested command: `/impeccable typeset`

**[P1] Zero dark theme support**
Why it matters: `ui/theme/Theme.kt` defines only `lightColorScheme` — no `darkColorScheme`, no `isSystemInDarkTheme()`, no dynamic color. Android's own platform guidance treats dark theme as a first-class scheme, not an afterthought; a patient checking an appointment at night in a dark-mode OS gets a jarring full-brightness screen with no opt-out. Total absence, not partial gap.
Fix: define a `darkColorScheme` mapped to the existing semantic roles, wire `isSystemInDarkTheme()` into `EyecareTheme`, and verify Deep Vision Navy / status colors hold contrast in dark.
Suggested command: `/impeccable colorize` or `/impeccable harden`

**[P1] Home greeting is factually wrong most of the day**
Why it matters: `HomeScreen.kt` hardcodes "Good morning" regardless of actual time, despite `HomeViewModel` already importing `java.time.LocalDate`. A patient opening the app at 6pm is greeted "Good morning" — the first thing the "composed companion" says to them is false.
Fix: derive the greeting from local time-of-day.
Suggested command: `/impeccable clarify`

**[P1] Profile shows no patient identity**
Why it matters: `ProfileScreen.kt`'s header renders only the literal word "Profile" — no name, avatar, or link-status summary, despite `PatientAccount` data being available in scope. This is the single most personal screen in the app and it currently belongs to no one in particular.
Fix: surface the patient's name and link status in the Profile header.
Suggested command: `/impeccable clarify` or `/impeccable layout`

**[P2] Four icon-only touch targets below the 48dp minimum**
Why it matters: `RescheduleBottomSheet.kt:433,449` (time-adjust up/down), `AppointmentDetailScreen.kt:724` (edit-note), and `AppointmentListScreen.kt:356` (week navigation) are all explicitly sized to 36dp — verified via static read, not inference. Below Android's 48dp minimum, these are harder to hit precisely, especially for patients with reduced dexterity.
Fix: raise each `IconButton`'s `Modifier.size` to at least 48.dp.
Suggested command: `/impeccable adapt`

## Persona Red Flags

**Jordan (first-timer)**: Lands on Home to a time-of-day greeting that's already wrong (see P1 above) — the first impression is a mistake, not reassurance. On the appointments list, "Appointment requests" and "Confirmed appointments" render as visually near-identical cards (same corner radius, same status-pill pattern) with no copy distinguishing a pending request from an actual booking — Jordan can't tell if they're already booked.

**Sam (accessibility-dependent)**: The four undersized touch targets above are a direct hit for anyone with reduced dexterity using TalkBack or switch access. Separately (design-review finding, not deterministically re-verified): `SplitBottomNavBar.kt`'s tab item sets `contentDescription` on the icon to the tab label while the adjacent visible `Text` isn't excluded from the semantics tree — worth a live TalkBack check for double-announcement ("Home, Home") before ruling it out.

**Riley (stress tester)**: Rapidly toggling the appointment list's date filter mid-`AnimatedContent`-transition has no debounce; combined with pull-to-refresh triggering two separate view-model refresh calls without coordination, a fast double-pull could produce inconsistent list/tab state. Not deterministically re-verified — worth a manual repro before prioritizing.

## Minor Observations

- `FrameListScreen.kt` overrides the title to `FontWeight.Bold` where DESIGN.md's Display role specifies SemiBold — a small, real drift from the one documented type spec.
- Errors like "Confirmed appointments unavailable: $confirmedError" interpolate a raw caught-exception message directly into patient-facing copy.
- Frame sort control hides real choices behind a `FilterChip` + `DropdownMenu` rather than a visible control, inconsistent with the segmented-tab pattern DESIGN.md uses elsewhere.
- Frame price formatting uses `String.format("%.2f", …)` rather than a locale-aware currency formatter.
- Two apparently dead composables (`HomeProductShelf`, `HomeProductCard`) found in `HomeScreen.kt`, never called.

## Questions to Consider

- DESIGN.md was written to codify Home/Frames/Nav as "visual authority" — why do those very files violate its own typography contract? Was the design system ever enforced by review, or only documented after the fact?
- Why does the single most personal screen in the app show zero patient identity — deliberately deferred, or never designed?
- Given this app handles health data and money, why is there no in-app help surface anywhere in the primary destinations (heuristic 10 scored 0)?
