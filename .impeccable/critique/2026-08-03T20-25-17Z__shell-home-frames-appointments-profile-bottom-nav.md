---
target: core app shell (Home, Frames, Appointments, Profile, bottom nav)
total_score: 23
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
timestamp: 2026-08-03T20-25-17Z
slug: shell-home-frames-appointments-profile-bottom-nav
---
Method: dual-agent (A: design-review general-purpose agent · B: deterministic-scan general-purpose agent)

## Design Health Score

| # | Heuristic | Score | Key Finding |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Appointment detail's "View Intake" button is wired to nothing (`onNavigateToIntake` defaults to `{}` in NavGraph) — tapping it gives zero feedback |
| 2 | Match Between System / Real World | 2 | Home's Clinic Hours card states a lunch-break schedule; the reschedule picker's bookable window is continuous 9am-5pm — the app disagrees with itself about when the clinic is open |
| 3 | User Control and Freedom | 3 | Unsaved-changes discard dialog + BackHandler on Edit Profile; cancel/reschedule flows stay reversible until confirmed |
| 4 | Consistency and Standards | 2 | Three different loading treatments across the four core screens: skeleton blocks, plain "Loading…" text, and a bare spinner — confirmed dark theme and typography are now fully wired and consistent (0 unstyled fallback roles, 0 hard-coded colors — both corroborated by the deterministic scan) |
| 5 | Error Prevention | 3 | Destructive actions (cancel, discard, logout) all route through the themed confirmation dialog |
| 6 | Recognition Rather Than Recall | 3 | Status is consistently color + text, confirmed by both assessments |
| 7 | Flexibility and Efficiency of Use | 2 | Sort/search/filter exist, but no quick-rebook or frame favoriting for a returning patient |
| 8 | Aesthetic and Minimalist Design | 2 | Home renders "Featured Frames" as a section header twice — once on the wrapping card, once inside the shelf composable itself |
| 9 | Error Recovery | 3 | Retry pattern applied uniformly with server-supplied messages |
| 10 | Help and Documentation | 1 | No contextual help/FAQ anywhere in the four core screens; only escape hatch is Messages, two taps deep in Profile |

**Total: 23/40 — Acceptable** (up from 20/40 on the prior run; still below Good)

## Trend

**23/40**, first improvement pass over the prior 20/40 baseline. Typography and dark-theme fixes fully verified as resolved by the deterministic scan; new issues surfaced are mostly ones the first pass's narrower scope didn't reach.

## Design Specificity Verdict

**LLM read**: better than the first pass, but still reads as a well-executed Material 3 app with one strong bespoke idea (the navy Visit Ticket) rather than a system that's Padilla-specific throughout — none of the four core screens render the clinic name or logo; identity comes from generic copy and the ₱ symbol alone.

**Deterministic scan**: confirms the typeset/colorize work actually landed — 0 hard-coded colors, 0 unstyled typography fallbacks (all ~110 checked call sites resolve to a styled role), dark theme fully wired end-to-end (`isSystemInDarkTheme()` reaching every screen via `EyecareTheme`), and `onPrimary` correctly used everywhere a primary fill needs content color. It also found what the first pass's narrower scope missed: one touch target still under 48dp (`RescheduleBottomSheet.kt`'s AM/PM toggle, 34dp), one dead composable (`StatusChip`, zero call sites anywhere), and two hand-rolled controls not documented in DESIGN.md.

## What's Working

- Dark theme and typography fixes hold up under independent re-verification — this is a real, confirmed improvement, not just a self-report.
- Confirmation-dialog discipline is consistent across every destructive action checked (cancel, discard, logout).
- Status communication remains color + text everywhere, corroborated again.

## Priority Issues

1. **[P1] Lens Cyan as text color on white surfaces fails WCAG AA — mathematically the same failure already fixed for white-on-cyan, just the colors swapped.** Frame prices (`FrameCard.kt`) and link text (`FrameDetailScreen.kt`) render Primary (#29B6F6) directly as text on white/light surfaces. Independently verified: this measures 2.30:1 — contrast is symmetric, so it's the identical ratio to the white-on-cyan bug already fixed, just inverted. This is a genuinely new finding the prior pass's narrower scope didn't reach, and it likely affects every screen using Lens Cyan as link/price/emphasis text on a light surface, not just these two. → `/impeccable colorize`
2. **[P1] Dead "View Intake" button on appointment detail.** `AppointmentDetailScreen.kt` always shows a "View Intake" `TextButton`, but `NavGraph.kt` never supplies `onNavigateToIntake` — it silently no-ops. Consistent with intake being retired per git history, but the UI was never cleaned up. Design-review finding, not deterministically re-verified — worth a quick manual check before removing. → `/impeccable harden`
3. **[P2] Duplicate "Featured Frames" header on Home.** The wrapping card and the inner shelf composable both render their own section header, stacked vertically in different typography roles. → `/impeccable polish`
4. **[P2] One touch target still below 48dp.** `RescheduleBottomSheet.kt`'s AM/PM toggle is 52×34dp — confirmed via static read. The prior adapt pass fixed that screen's up/down stepper arrows and this toggle's text color, but not this box's own size. → `/impeccable adapt`
5. **[P2] Clinic hours contradict each other.** Home's Clinic Hours card shows a lunch-break schedule; the reschedule picker validates a continuous 9am-5pm bookable window. Design-review finding, not deterministically re-verified. → `/impeccable harden`

## Persona Red Flags

- **Jordan (first-timer)**: no clinic name/logo anywhere on Home — nothing anchors the app as Padilla's specifically beyond generic copy. The dead "View Intake" button (if real) is exactly the kind of small trust leak that reads as unreliability in a health app.
- **Casey (thumb-zone)**: the appointments FAB (bottom-end) and the centered floating bottom nav create two independently-positioned tap zones to track one-handed.
- **Sam (accessibility-dependent)**: hit directly by the AM/PM toggle's 34dp height; separately, the frame variant picker's unbounded `FlowRow` of options gives TalkBack no count/grouping context.

## Minor Observations

- Dead code: `StatusChip` in `AppointmentListScreen.kt` has zero call sites anywhere in the repo — confirmed via full-repo grep, not a spot-check guess.
- `RescheduleBottomSheet.kt`'s AM/PM toggle and hour/minute stepper are hand-rolled rather than Material3 components, and neither is documented in DESIGN.md's Components section as a sanctioned pattern.
- Frame search field uses a 32dp full-pill shape and placeholder-only labeling, diverging from DESIGN.md's documented 4dp field spec and "persistent label" rule.
- Frame price formatting is unlocalized (`String.format("%.2f", …)`).
- `HomeViewModel` silently swallows all three parallel-fetch failures with no partial-failure messaging.

## Questions to Consider

- Lens Cyan text-on-white is likely used well beyond these two flagged files — is it worth a dedicated pass across the whole app rather than fixing it screen by screen?
- If a patient can't tell this app apart from a generic clinic template without reading the Visit Ticket, is one navy card doing all the branding work the rest of the system should share?
- Is the dead "View Intake" button an isolated miss, or a sign that navigation cleanup after feature retirement needs its own checklist?
