---
target: home page
total_score: 25
max_score: 36
na_heuristics: 10
p0_count: 1
p1_count: 2
timestamp: 2026-08-13T09-07-14Z
slug: va-com-eyecare-app-presentation-home-homescreen-kt
---
Method: dual-agent (A: ab97c94d88632203e · B: acb5758400eede52d)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Loading skeleton and pull-to-refresh communicate state well; no distinct indicator differentiates "initial load" from "refreshing with stale content already shown." |
| 2 | Match System / Real World | 3 | Warm, plain-language copy throughout; the visit status pill only title-cases the first character ("Checked in", not "Checked In"). |
| 3 | User Control and Freedom | 3 | System Back untouched, pull-to-refresh present; no way to collapse Clinic Hours permanently or reorder Home content. |
| 4 | Consistency and Standards | 2 | "Featured Frames" heading renders twice in the same card (lines 218 and 510); frame name uses `bodySmall` instead of the documented Title Small role; `Color.White` literals bypass the `onVisitNavy` token. |
| 5 | Error Prevention | 3 | Home is read-only/idempotent, so little to prevent; the one destructive-adjacent moment (retry) is safe by construction. |
| 6 | Recognition Rather Than Recall | 3 | Cards are self-labeled and scannable; nothing requires memorized app state. |
| 7 | Flexibility and Efficiency | 2 | No shortcuts or quick actions for a returning patient; one CTA per card, nothing accelerates repeat use. |
| 8 | Aesthetic and Minimalist Design | 3 | Individually clean cards undercut by the duplicate heading and by a generic hours widget claiming the top slot over the signature Visit Ticket. |
| 9 | Error Recovery | 3 | `ErrorContent` gives message + Retry, but renders in full-saturation error red — a harsh first impression for a "calm, warm" brand on a low-stakes surface. |
| 10 | Help and Documentation | n/a | No help affordances on Home; reasonably out of scope for an Operate-mode overview screen with no complex actions. |
| **Total** | | **25/36** | **Acceptable, bordering Good** |

## Design Specificity Verdict

**LLM assessment:** Mixed, leaning generic. `VisitTicket` (`HomeScreen.kt:346-452`) is genuinely bespoke — navy card, cyan date tile, translucent status capsule — and matches DESIGN.md's own description of it as the screen's signature component almost exactly. But it doesn't get to lead: `ClinicHoursCard` (`HomeScreen.kt:239-343`) is a hardcoded business-hours accordion that any local-business app could ship unchanged, and it sits *above* the Visit Ticket in the vertical flow. `FrameSummaryCard` (`HomeScreen.kt:528-557`) strips the documented Frame Card spec down to image+name+brand, dropping price and the AR-ready badge that are core to the product's eyewear-discovery pitch. Crop out the Visit Ticket and this could be any local-business app's home tab.

**Deterministic scan:** `detect.mjs` returned `[]` (exit 0) on both the target file and the containing directory. This is **not a clean bill of health** — the detector's `SCANNABLE_EXTENSIONS` whitelist (`.html/.css/.jsx/.tsx/.js/.ts/.vue/.svelte/.astro`, etc.) does not include `.kt`, so it walked the directory, matched zero files, and returned empty by construction. Treat the detector as not having run at all for this target.

In its place, Assessment B's manual mechanical scan of the Kotlin source independently confirmed one of Assessment A's findings: 6 raw `Color.White` literals in `VisitTicket` (lines 402, 409, 412, 419, 425, 430) that bypass the `EyecareColors.current.onVisitNavy` token defined for exactly this content-on-navy role — agreement between the qualitative and mechanical passes strengthens this as a real (if currently invisible, since the token equals white today) drift risk. The mechanical scan also surfaced two things Assessment A didn't flag: an unused `LinearProgressIndicator` import (dead code, `HomeScreen.kt:40`) and zero use of `stringResource`/`R.string` anywhere in the file — 20+ literal user-facing strings with no i18n path, which is a structural gap worth naming even though localization wasn't in PRODUCT.md's stated scope. It also confirmed content-description hygiene is solid (every icon/image sets `contentDescription` deliberately, decorative vs. meaningful) and that state handling is a proper exhaustive `when` over a sealed `HomeUiState`, not an assume-data-present pattern — though no dedicated empty-state composable is used even though one (`EmptyContent`) exists in the shared components file.

**Visual overlays:** Not applicable — this is a native Android/Compose surface with no browser or running emulator available in this session, so no live-server injection or on-screen overlay was attempted. This critique is source-level only.

## Overall Impression

Home has one real piece of brand-authored design — the Visit Ticket — surrounded by generic scaffolding that doesn't yet earn the "Clear Care Companion" description DESIGN.md gives the app. The single biggest opportunity isn't a polish pass, it's a gap: unlinked patients, a population PRODUCT.md explicitly calls out as needing "a clear path to become linked," land on Home and get no linking prompt at all in the slot where linked patients see their Visit Ticket. Fixing that, reordering Clinic Hours below the visit content, and de-duplicating the Featured Frames heading would move this from "acceptable" to genuinely "good" without touching anything structurally large.

## What's Working

- **`VisitTicket`** (`HomeScreen.kt:346-452`) faithfully implements the DESIGN.md spec — navy anchor, cyan date tile, translucent status capsule — and is the strongest evidence Home is following an actual design system rather than reinventing itself.
- **`HOME_SHELF_LIMIT = 4`** (`HomeViewModel.kt:98`) is a concrete, deliberate application of the ≤4-items cognitive-load rule to the frame shelf — exactly the kind of restraint the cognitive-load checklist rewards.
- **`BookingInvitation` copy** (`HomeScreen.kt:479`, "Make time for clearer, more comfortable vision") turns a linked-but-nothing-scheduled state into reassurance instead of a blank void — good emotional-journey handling for the case it does cover.

## Priority Issues

**[P0] Unlinked patients get no linking CTA on Home**
- **Why it matters:** `HomeScreen.kt:199-203` wraps both `VisitTicket` and `BookingInvitation` in `if (hasActivePatientLink)`; when false, that slot renders nothing. This is the app's primary landing screen, and PRODUCT.md's second Product Principle explicitly requires "a clear path to become linked" for exactly this population. A newly registered, not-yet-linked patient — the exact persona the product is designed to onboard — opens the app and sees a greeting, an hours widget, and maybe frames, with zero indication their account isn't connected to the clinic yet.
- **Fix:** Render a Home-scoped linking invitation card (same visual family as `BookingInvitation`) in that slot when `!hasActivePatientLink`, pointing at the existing invite-code/request-review flow that today is reachable only from Profile.
- **Suggested command:** `/impeccable onboard` (first-run/activation gap for the unlinked-patient state)

**[P1] Duplicate "Featured Frames" heading**
- **Why it matters:** `HomeScreen.kt:216-227` (outer card header + subtitle) and `HomeScreen.kt:504-514` (`HomeFrameShelf`'s own header) both render the literal text "Featured Frames" inside the same card. It reads as two half-merged component patterns nobody reconciled, and breaks the chunk boundary a scanning user relies on.
- **Fix:** Keep the outer title+subtitle, delete the inner duplicate label, retain only "See all" from the inner header.
- **Suggested command:** `/impeccable layout`

**[P1] A generic utility card outranks the brand's signature component**
- **Why it matters:** `ClinicHoursCard` (`HomeScreen.kt:239-343`) — a hardcoded, non-brand-specific hours accordion — sits above `VisitTicket`/`BookingInvitation` in the vertical flow, meaning the least distinctive widget on the screen gets first position over the one component DESIGN.md calls "the signature high-priority component." It also delays the one thing Home is supposed to answer fastest ("what's my next visit") behind a static hours widget, hurting a quick-glance user (Casey).
- **Fix:** Move Clinic Hours below the visit/booking block (or relocate to Profile/a footer); source hours from the backend rather than hardcoded literals (lines 319, 334), consistent with the product principle that backend state stays authoritative.
- **Suggested command:** `/impeccable layout`

**[P2] Home's Frame Card ignores the documented Frame Card spec**
- **Why it matters:** `FrameSummaryCard` (`HomeScreen.kt:528-557`) drops price (`accent-text`) and the AR-ready badge, and hand-picks `bodySmall` + manual `FontWeight.SemiBold` (line 552) for the frame name where DESIGN.md assigns that exact field to the Title Small role — precisely the ad hoc sizing the Friendly Precision Rule forbids. It also renders `frame.brand` via `labelSmall` without the uppercase transform the Frame Card spec calls for. This weakens the AR/eyewear-discovery pitch right where Home is supposed to tease it.
- **Fix:** Reuse the canonical Frame Card component/styling instead of a bespoke stripped-down card.
- **Suggested command:** `/impeccable typeset`

**[P2] Raw `Color.White` bypasses the `onVisitNavy` token**
- **Why it matters:** `HomeScreen.kt:402, 409, 412, 419, 425, 430` hardcode `Color.White`/`Color.White.copy(alpha=...)` inside `VisitTicket`, even though `Theme.kt` defines `EyecareColors.current.onVisitNavy` for exactly this content-on-navy role — confirmed independently by both the qualitative review and the mechanical scan. Currently invisible (the token equals white in both schemes today) but untracked drift risk the moment the token changes.
- **Fix:** Replace all six literals with `EyecareColors.current.onVisitNavy`.
- **Suggested command:** `/impeccable polish`

**[P3] Status pill capitalization is inconsistent**
- **Why it matters:** `HomeScreen.kt:354-357` title-cases only the first character of the whole status string, so `"CHECKED_IN"` becomes `"Checked in"` rather than `"Checked In"` — a minor but visible copy-quality gap next to clean single-word statuses like "Confirmed."
- **Fix:** Title-case each word when formatting multi-word status values.
- **Suggested command:** `/impeccable clarify`

## Persona Red Flags

**Jordan (First-Timer):** Lands on Home immediately after registration; if still unlinked, the primary screen offers zero indication of what to do next (P0 above). This is precisely the "am I actually connected to my clinic?" anxiety point PRODUCT.md calls out by name, and Home says nothing about it.

**Sam (Accessibility-Dependent):** `ClinicHoursCard`'s chevron icon carries its own `contentDescription` ("Expand"/"Collapse", line 294) while sitting inside a separately `.clickable` parent `Card` (line 253) with no group-level semantics — TalkBack will likely expose the chevron as an extra, non-actionable stop distinct from the actual tappable region, confusing which element performs the action.

**Casey (Distracted Mobile):** Opens the app to check "what's next" and has to scroll past a duplicated "Featured Frames" label and a static hours widget before reaching (or, if unlinked, never reaching) next-visit information — adds friction to the single task Home is supposed to answer fastest.

## Minor Observations

- Unused `LinearProgressIndicator` import (`HomeScreen.kt:40`) — dead code suggesting a progress affordance was cut without cleanup.
- The Visit Ticket's day-of-month number uses the Display role, which DESIGN.md scopes to "compact screen-level statements" like the Home greeting — a plausible but undocumented extension into a 64dp date tile; worth folding into DESIGN.md explicitly if intentional.
- No `stringResource`/`R.string` usage anywhere in the file; 20+ literal user-facing strings have no i18n path. Not a stated product requirement today, but worth flagging before it becomes expensive to retrofit.
- `ClinicHoursCard`'s hours are hardcoded literals rather than backend-sourced, in an app whose own product principles treat the backend as authoritative for clinic operational facts.

## Questions to Consider

1. Does Home need a generic "Clinic Hours" card at all in its current top slot — what is it doing there that a link inside Profile, or a footer line, couldn't do without displacing the Visit Ticket?
2. If an unlinked patient opens the app right now, what actually tells them to go get linked? Is silence an acceptable answer on a healthcare app's first-run screen?
3. Is "Featured Frames" a finished component, or two half-merged headers nobody noticed render twice — and if the latter, what else on this screen was assembled the same way?
