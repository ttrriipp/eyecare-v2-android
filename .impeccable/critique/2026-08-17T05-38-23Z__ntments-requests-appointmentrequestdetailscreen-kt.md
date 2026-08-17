---
target: appointment request awaiting clinic review screen
total_score: 23
max_score: 32
na_heuristics: 7,10
p0_count: 1
p1_count: 2
timestamp: 2026-08-17T05-38-23Z
slug: ntments-requests-appointmentrequestdetailscreen-kt
---
Method: dual-agent (A: general-purpose design-review agent · B: general-purpose detector/evidence agent)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | No refresh affordance anywhere on the one screen whose entire purpose is checking for a status change — no pull-to-refresh, no reload icon in the `TopAppBar` (contrast the list screen, which has one). |
| 2 | Match System / Real World | 4 | "Your request is awaiting clinic review. No time is reserved until confirmed." is plain, honest, patient-facing language. |
| 3 | User Control and Freedom | 2 | Only escape valve for a PENDING request is destructive Cancel; no edit, no contact-clinic path. The confirm dialog also closes immediately on confirm without reflecting the async cancel's outcome — that only surfaces afterward via a separate error `Text`/button-loading state. |
| 4 | Consistency and Standards | 3 | Status pill/label genuinely shared between list and detail (`AppointmentRequestStatusPill`), but several small token deviations exist: an ad hoc 8%-alpha `primary` wash instead of the documented `primary-container` token, off-scale spacing (5dp pill padding, 10dp arrangement — neither is on DESIGN.md's 4/8/12/16/20/24/32 scale), an ad hoc `fontWeight` override on the Body role, and a structurally duplicate (not shared) pill composable for confirmed appointments. |
| 5 | Error Prevention | 4 | Cancel is gated by `AppConfirmationDialog`, matching the documented Confirmation Dialog spec exactly (20dp surface, hairline border, 44dp tinted icon badge, semantic error confirm color, safe "Keep"). |
| 6 | Recognition Rather Than Recall | 3 | All submitted data is re-shown; alternates are clearly labeled. |
| 7 | Flexibility and Efficiency | n/a | Single read-mostly detail screen; no power-user path is expected here. |
| 8 | Aesthetic and Minimalist Design | 2 | Hierarchy inversion: the `TopAppBar` hero slot is spent on the opaque `requestNumber`, rendered through Material3's implicit default title style rather than any of DESIGN.md's twelve named roles, while the patient-meaningful appointment type/reason renders as plain `bodyMedium` — the reverse of DESIGN.md's own prescribed pairing for a detail screen's hero heading. |
| 9 | Error Recovery | 3 | `cancelError` is shown as a bare, unstyled error-colored `Text` with no retry affordance beside it, unlike the styled rejection-reason box a few lines above it. |
| 10 | Help and Documentation | n/a | Not applicable to a single detail screen; but the *absence* of any "how long does review usually take" cue is a real emotional gap, tracked as a Priority Issue below rather than scored here. |
| **Total** | | **23/32** | **Good (72%)** |

## Design Specificity Verdict

**LLM assessment**: Generic. The screen is a template status-detail pattern — hero title, status pill, metadata block, one conditional action — that could ship unchanged in a package-tracking or expense-approval app. The one product-specific idea ("no time is reserved until confirmed," the clinic-as-authority, non-binding-request framing) lives entirely in one sentence of copy; nothing in the composition, color, or layout reinforces it. The screen also ignores a named product capability — direct clinic messaging — at exactly the moment a patient would most want it: while waiting on a decision.

**Deterministic scan**: `detect.mjs` ran cleanly (exit 0) against all three relevant files and returned `[]` — a real, completed run, not a skip. The detector's pattern set targets web markup (HTML/CSS/JS/JSX) and found nothing applicable to Kotlin/Compose, so this is a legitimate "ran, nothing to report" result rather than a crash or gap. The code-evidence pass in place of browser inspection is summarized below and folded into the heuristics table and issues above/below.

Key facts the evidence pass surfaced, beyond what shaped the score table:
- No raw/hardcoded hex color literals in either target file — all colors route through `MaterialTheme.colorScheme.*` or `EyecareColors.current.*`.
- No use of raw `primary`/Lens Cyan as `Text`/`Icon` color on a light surface was found (the one raw `.primary` reference is a `Surface` background fill, not text/icon color, so it doesn't trip DESIGN.md's specific WCAG-fail pattern).
- The status pill's text color (`AppointmentRequestPresentation.kt:45`) is the raw semantic hue (`statusPending`/`statusConfirmed`/`statusCancelled`) used directly as text on that same hue's 12%-alpha tint of itself. DESIGN.md's documented contrast analysis only covers white-text-on-solid-fill cases; this same-hue-on-own-wash pairing is unverified and plausibly under 3:1 for the amber pending state specifically.
- Both screens genuinely reuse the same composable and status text for PENDING with no visual divergence; the only difference is placement (list pairs the pill with the request number, detail moves the number into the `TopAppBar` and pairs the pill with the long-form description instead).

**Visual overlays**: Not applicable — this is native Jetpack Compose with no browser-renderable surface, so no injection/overlay pass was attempted (per the critique flow's own allowance for non-viewable targets).

## Overall Impression

The bones are sound and the system discipline mostly holds — real token reuse, a genuinely shared status pill, and a well-built confirmation dialog. But the screen fails at its one job during its highest-stakes moment: a patient checking in on an uncertain request gets no way to refresh, no way to reach the clinic, an inverted hierarchy that buries what they actually asked for behind an internal reference number, and — in the worst case — unscrollable content if their reason or alternate times run long. The single biggest opportunity is turning "Awaiting clinic review" from a passive status readout into an active waiting experience: refreshable, reassuring about timing, and one tap from a human.

## What's Working

- **The shared status pill is a real system, not a claim.** `AppointmentRequestStatusPill` is invoked identically from the list and detail screens with the same label, color, and shape logic — exactly the "single source of truth" DESIGN.md asks for, and it always pairs color with text, never color alone.
- **The cancel-confirmation flow is textbook.** `AppConfirmationDialog` matches the documented Confirmation Dialog spec precisely: 20dp surface, hairline border, 44dp tinted icon badge, semantic error color on the destructive action, and a clearly worded safe alternative ("Keep").
- **The copy is honest and calm.** "No time is reserved until confirmed" does real reassurance work without hedging or jargon — the writing is ahead of the visuals here.

## Priority Issues

**[P0] Main content column has no scroll wrapper.** The `Column` at `AppointmentRequestDetailScreen.kt:139-142` fills its `Scaffold` padding but is never wrapped in `verticalScroll`. A long reason for visit, several alternative times, and (for REJECTED) a rejection reason stacked together will clip against the viewport instead of scrolling — a direct violation of DESIGN.md's explicit Do: "scroll rather than clip when content plus keyboard exceeds the viewport."
**Why it matters**: this is exactly the kind of content a real patient produces (a written reason, 2-3 backup times); clipped content silently hides information the clinic will act on.
**Fix**: wrap the `Column` in `Modifier.verticalScroll(rememberScrollState())`, matching the pattern already used in `RequestReviewContent.kt` and `RequestSubmissionErrorContent`.
**Suggested command**: `/impeccable harden`

**[P1] No way to refresh a PENDING request's status.** The screen that exists specifically so a patient can check whether the clinic has decided yet has no pull-to-refresh and no reload control in its `TopAppBar` — the patient must navigate back out and back in to see a change.
**Why it matters**: this is the single moment the screen's visibility-of-status job matters most, and it currently fails it.
**Fix**: add pull-to-refresh (or a manual reload icon in the `TopAppBar`, mirroring the pattern already present on the list screen) that re-triggers `viewModel.load(requestId)`.
**Suggested command**: `/impeccable polish`

**[P1] Only action offered is destructive Cancel — no path to the clinic.** `AppointmentRequestDetailScreen.kt:258-267` surfaces exactly one action for a PENDING request, and it ends the request. The product's own positioning names "direct clinic communication" as a core capability, yet the screen where a patient is most likely to want to ask "did you get this?" offers no way to message the clinic.
**Why it matters**: an anxious patient's only outlet is to cancel and start over, which actively works against retention of a request the clinic may be about to accept.
**Fix**: add a secondary "Message the clinic" action (outlined, non-destructive) alongside Cancel, deep-linking into the existing messaging feature with this request as context.
**Suggested command**: `/impeccable onboard`

**[P2] Hierarchy inversion in the hero slot.** The `TopAppBar` title (`:134`) spends its hero position on the opaque `requestNumber`, rendered through Material3's implicit default style rather than any of DESIGN.md's twelve named typography roles, while the patient-meaningful appointment type and reason are pushed down into plain `bodyMedium` rows inside `DetailMetadataRow`/`DetailRow`. This is the reverse of DESIGN.md's own Title Large guidance for a detail screen's hero heading ("the appointment type... paired with a small label/reference above it").
**Why it matters**: a first-time patient can't tell what "Request REQ-00123" means, and the thing they actually care about (what kind of visit, for when) is visually subordinate to an internal reference number.
**Fix**: move the appointment type (or "Awaiting clinic review" itself) into the Title Large hero slot, and demote the request number to a small label/reference line above or below it, per the documented pattern; route the `TopAppBar` title through an explicit typographic role instead of the implicit default.
**Suggested command**: `/impeccable layout`

**[P2] Status pill text color is an unverified contrast risk.** `AppointmentRequestPresentation.kt:45` renders the pill's label in the raw semantic status hue (`statusPending` Amber `#F6AD55`, etc.) directly on that same hue's 12%-alpha tint of itself. DESIGN.md's existing "Known contrast gap" note only analyzes white text on a *solid* status fill — it does not cover this same-hue-on-own-wash case, and Amber-on-its-own-12%-wash is a plausible AA failure that hasn't been checked.
**Why it matters**: this is the single component that communicates request status everywhere in the app (list and detail); if it fails contrast, it fails on both surfaces at once.
**Fix**: run the actual contrast check for `statusPending`/`statusConfirmed`/`statusCancelled` text-on-12%-own-wash in both themes; if any fail, either darken the text-use variant (mirroring the existing `accent-text` split between fill-color and text-color tokens) or raise the tint opacity.
**Suggested command**: `/impeccable audit`

## Persona Red Flags

**Riley (Stress Tester)**: Long reason text or several alternate times will clip against the viewport rather than scroll (P0, confirmed by direct code read — no `verticalScroll` present). The cancel-error state (`:254-256`) renders as bare error-colored text with no retry button positioned next to it, unlike the styled rejection-reason box a few lines above using the same semantic color role.

**Jordan (First-Timer)**: The top bar reads "Request REQ-00123" with nothing explaining what that number is for. Nowhere on the screen is there any stated expected review turnaround ("clinics usually respond within..."), so a first-timer genuinely cannot tell whether "awaiting review" means minutes or days — and has no lower-anxiety way to find out than canceling or waiting blind.

**Sam (Accessibility-Dependent)**: The status pill's text-on-own-wash contrast is unverified and plausibly fails AA for the Amber pending state specifically (see P2 above) — this is the exact component a screen-reader/low-vision user relies on to know the request's state at a glance. No `semantics {}` blocks exist in either target file (confirmed via search), which is acceptable where default Compose semantics already suffice, but combined with the contrast risk on the one component carrying all the state information, this persona has the least redundancy to fall back on.

## Minor Observations

- Rejection-reason panel uses a 12dp corner radius (`:233`) nested inside the 20dp card, while the metadata panel above it uses 16dp — inconsistent inner-radius choice for two panels playing the same role.
- The date/time/type metadata `Surface` uses an ad hoc `MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)` fill rather than the documented `primary-container` token or the 14%/35% cyan-tint recipe used elsewhere (segmented tabs, chips).
- `DetailMetadataRow`'s value text (`:316`) overrides the Body role's documented Regular (400) weight to Medium via an ad hoc `fontWeight` param.
- Appointment-type formatting ("`${type.name} (${type.durationMinutes} min)`", `:190`) doesn't match the list card's "`·`"-separator convention for the same data.
- A structurally near-identical status pill (`AppointmentStatusPill` in `AppointmentListScreen.kt`, for confirmed appointments rather than requests) duplicates the same `Surface`/`RoundedCornerShape(50)`/12%-alpha/`labelSmall`-Bold recipe as a separate composable rather than sharing it.
- No "requested on" timestamp anywhere on the screen, so a patient can't gauge how long they've already been waiting.
- 5dp pill vertical padding and 10dp panel spacing are both off DESIGN.md's documented 4/8/12/16/20/24/32 spacing scale.

## Questions to Consider

- If "nothing is reserved yet" is the core idea this screen exists to communicate, why does the hero slot go to an internal reference number instead of anything that visually communicates "not locked in"?
- The product's positioning names direct clinic messaging as a core capability — why is Cancel the only action ever offered on the one screen where a patient is most likely to want to ask "did you get this"?
- What would this screen look like if "waiting" were designed as an active state (refreshable, time-boxed, reachable) rather than a static readout?
