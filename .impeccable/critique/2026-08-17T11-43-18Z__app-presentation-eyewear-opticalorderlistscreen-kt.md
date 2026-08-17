---
target: my orders list page
total_score: 26
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
timestamp: 2026-08-17T11-43-18Z
slug: app-presentation-eyewear-opticalorderlistscreen-kt
---
## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Refresh-on-resume wipes the whole list to a bare centered spinner (`OpticalOrderListScreen.kt:49-51`) instead of a non-destructive top indicator. |
| 2 | Match System / Real World | 4 | Status labels are humanized and patient-appropriate ("Ready for pickup," "Released to you"); no real issue found. |
| 3 | User Control and Freedom | 2 | No pull-to-refresh and no manual refresh affordance anywhere on `MyOrdersScreen.kt`; the only refresh path is backgrounding/foregrounding the app. |
| 4 | Consistency and Standards | 1 | The list's `orderStatusColor` (`EyewearPresentation.kt:50-56`) and the detail screen's independent, duplicated status-color switch (`OpticalOrderDetailScreen.kt:110-116`) disagree on the same order — confirmed by direct read. |
| 5 | Error Prevention | 3 | Read-only surface, low inherent risk; the load-more retry loop is really an error-prevention issue in disguise (see Priority Issues). |
| 6 | Recognition Rather Than Recall | 4 | Context-aware date label (`orderDateLabel`, `EyewearPresentation.kt:81-90`) and color+text pills reduce recall burden well. |
| 7 | Flexibility and Efficiency | 1 | After the Current/History split was removed, there is zero filter/sort/search — a patient with a long order history can't jump to "needs payment" or "ready now." |
| 8 | Aesthetic and Minimalist Design | 3 | `OrderCard` is reasonably lean (~5 chunks), no clutter — a real improvement over the historical "8 same-weight rows" complaint. |
| 9 | Error Recovery | 2 | `ErrorContent`'s full-list retry works; the load-more error path is bare red text with no retry button, and silently re-triggers instead of surfacing a controlled retry. |
| 10 | Help and Documentation | 3 | Nothing is needed on a read-only list this simple; nothing is offered; net neutral for this surface. |
| **Total** | | **26/40** | **Acceptable** |

## Design Specificity Verdict

**LLM assessment**: The screen reads as authored for Eyecare, not a generic order-list template — but only partially. Evidence of specificity: patient-warm status vocabulary rather than technical fulfillment states, PH-locale peso formatting (`formatPeso`, `EyewearPresentation.kt:19`), the border-led 16dp card recipe matching DESIGN.md's `card-standard`, and balance-due surfaced as the second visual beat on the card — a genuine product-specific priority ("do I owe money"). Working against specificity: the `Total` row (`OpticalOrderListScreen.kt:184-188`) gets zero brand treatment — plain `onSurface`, same weight class as the metadata rows above it — while DESIGN.md's own Frame Card spec prescribes Deep Lens Cyan (`accent-text`) for price display. A price with no accent color, on a card that's otherwise just icon + pills + rows, is the one place this screen slips into "any order list" territory.

**Deterministic scan**: `detect.mjs --json` returned `[]`/exit 0 on all four files in this surface (`OpticalOrderListScreen.kt`, `MyOrdersScreen.kt`, `EyewearPresentation.kt`, `OpticalOrderListViewModel.kt`). This is "no signal," not "clean" — the scanner's `SCANNABLE_EXTENSIONS` list has no `.kt` entry and its regex rules target CSS/JSX/HTML syntax (border-radius, Tailwind classes, hex/rgb() color functions), none of which match Kotlin/Compose syntax. When a single file is passed explicitly it still runs the regex engine unconditionally, so the empty result reflects rule-pattern mismatch with the language, not a real pass.

In place of the scanner, a manual mechanical evidence pass (Assessment B) confirmed: zero hardcoded hex colors in the four target files (all route through `MaterialTheme.colorScheme.*`/`EyecareColors.current.*`), zero missing `contentDescription`, zero undersized touch targets, zero raw font-size literals bypassing typography roles, zero TODO/dead code. It also found two *new*, previously-unflagged color collisions the detector's regex couldn't have caught: `orderStatusColor` (`EyewearPresentation.kt:54-55`) resolves `CANCELLED` and `UNKNOWN` to the identical hex in both themes (`StatusCancelled`/`ErrorDark`), and `paymentStatusColor` (`:69,71`) resolves `UNPAID`/`PARTIALLY_PAID` and `UNKNOWN` to the same identical pair — the same collision *class* a prior critique run caught and fixed for `READY_FOR_DISPENSING`/`DISPENSED`, now recurring one level down in two different enums.

**Visual overlays**: N/A — native Android Compose target, no browser/dev-server to inject into. Both assessments substituted source-level evidence passes instead, as required for non-viewable targets.

## Overall Impression

This surface has clearly been worked on since the last critique — the P0 (unreachable pagination), one P1 (dead `refresh()`), and the original `READY_FOR_DISPENSING`/`DISPENSED` color collision are all genuinely fixed, and the card's information hierarchy now correctly foregrounds balance-due. But the fixes were applied locally to the list screen without closing the loop with its sibling detail screen, and the underlying pattern that caused the original collision — hand-rolled `when` blocks mapping enums to raw theme colors, duplicated in two files — was never consolidated into one shared source of truth. That's why two *new* collisions appeared in the same function that fixed the old one, and why the detail screen now visually contradicts the list for the same order.

## What's Working

- **Balance-due placement is now correctly prioritized**: the status/payment pill pairing followed immediately by bold balance-due (`OpticalOrderListScreen.kt:137-169`) fixes the historical "buried mid-stack" complaint — it's the second thing the eye lands on, which is right for a bill-paying context.
- **Pagination is properly wired**: `derivedStateOf` + `LaunchedEffect` on scroll position (`OpticalOrderListScreen.kt:71-81`) closes the prior P0 where load-more was built but never triggered.
- **`refresh()` is genuinely reachable**: `MyOrdersScreen.kt:38-49` wires it to `ON_RESUME` with a correct first-resume guard against double-loading.

## Priority Issues

**[P1] Order status color is unreliable — three separate collisions/divergences, one already caught once before.**
Why it matters: (1) `orderStatusColor` (`EyewearPresentation.kt:54-55`) maps `CANCELLED` and `UNKNOWN` to the identical color in both light and dark theme; (2) `paymentStatusColor` (`:69,71`) maps `UNPAID`/`PARTIALLY_PAID` and `UNKNOWN` to the identical color; (3) confirmed by direct read, the detail screen's own inline `when` block (`OpticalOrderDetailScreen.kt:110-116`) maps `READY_FOR_DISPENSING` and `DISPENSED` to the *same* `colorScheme.tertiary` — the exact bug the list screen's version of this function was already fixed to avoid. A patient sees the same order rendered in contradictory colors one tap apart, and can't visually distinguish "we don't know this order's status" from "this order is cancelled" or "you still owe money." This is the identical collision class a prior critique run caught and fixed once already — it recurred because the fix lived in one file's `when` block instead of a single shared helper.
Fix: delete the detail screen's duplicated `when` block and call the list's `orderStatusColor`/`paymentStatusColor` from both places; give `UNKNOWN` its own distinct color in both functions rather than reusing an existing semantic hue.
Suggested command: `/impeccable colorize`

**[P1] Load-more failure can trigger a tight, unbounded retry loop.**
Why it matters: `LaunchedEffect(shouldLoadMore, state.hasMorePages, state.isLoadingMore)` (`OpticalOrderListScreen.kt:79-81`) re-evaluates on every `isLoadingMore` flip. `loadMoreInternal()` (`OpticalOrderListViewModel.kt:79-104`) sets `isLoadingMore = true` then `false` on failure, with `hasMorePages` unchanged — if the user is still scrolled near the bottom, `shouldLoadMore` is still true and the effect fires `onLoadMore()` again immediately. On a flaky connection near the end of a long list, this hammers the backend continuously with no cap or backoff, and the `loadMoreError` text (`:101-110`) flickers rather than settling into an actionable state.
Fix: gate retries with an explicit "already attempted at this scroll position" flag, or require a user-initiated retry action instead of re-deriving purely from scroll position + loading flag.
Suggested command: `/impeccable harden`

**[P2] Status and payment pill label text skips the app's own contrast-safe text tokens.**
Why it matters: confirmed `EyecareExtendedColors` already defines `statusPendingText`/`statusConfirmedText`/`statusCancelledText` (`Theme.kt:77-79,96-98,113-115`) specifically because the raw fill-strength hues fail WCAG AA as text on their own 12% tint (this is the exact pattern `AppointmentRequestStatusPill` already follows correctly). `orderStatusColor`/`paymentStatusColor` (`EyewearPresentation.kt:50-56,66-72`) never route through these tokens — they return the raw fill color and use it as both the tint source *and* the label text color, on the two most decision-relevant chips on the card.
Fix: use the `...Text` token variants for label color, keep the raw hue only for the tint fill.
Suggested command: `/impeccable colorize`

**[P2] No pull-to-refresh; the only refresh path is destructive and undiscoverable.**
Why it matters: `MyOrdersScreen.kt` has no `PullToRefreshBox`, unlike the sibling `AppointmentListScreen.kt:122-130`. The only refresh trigger is `ON_RESUME`, and `refresh()` sets state back to `Loading` (`OpticalOrderListViewModel.kt:57-59`), which swaps the entire list for a bare centered spinner (`OpticalOrderListScreen.kt:49-51`), discarding scroll position. Most patients won't know backgrounding the app refreshes the list, and there's no way to force a refresh while actively looking at it.
Fix: add `PullToRefreshBox` matching the appointments pattern; change `refresh()` to keep existing items visible with a top-level refreshing indicator instead of resetting to `Loading`.
Suggested command: `/impeccable harden`

**[P3] Order title has no line-clamp safeguard for long item names.**
Why it matters: the title `Text` in `OrderCard` (`OpticalOrderListScreen.kt:130-134`) has no `maxLines`/`overflow`, unlike `AppointmentCard`'s title in `AppointmentListScreen.kt`, which sets `maxLines = 2, overflow = TextOverflow.Ellipsis`. A long single-item frame/lens description will wrap uncontrolled, producing inconsistent card heights and breaking the established precedent.
Fix: mirror the `AppointmentCard` treatment.
Suggested command: `/impeccable polish`

## Persona Red Flags

**Sam (Accessibility-Dependent User)**: the status-pill and payment-pill text-contrast failure above sits on exactly the fields a low-vision user most needs to distinguish quickly. Separately, `loadMoreError` renders in raw `colorScheme.error` at `bodySmall` (`OpticalOrderListScreen.kt:104-108`) directly on white — DESIGN.md's own documented ~4.13:1 gap for this hue as inline text, which fails strict AA for normal-weight small text.

**Riley (Deliberate Stress Tester)**: the unbounded load-more retry loop (`OpticalOrderListViewModel.kt:79-104` + `OpticalOrderListScreen.kt:79-81`) is squarely Riley's scenario — flaky connectivity near the bottom of a long list turns into continuous, silent backend hammering with a flickering error message, no cap, no backoff.

**Casey (Distracted Mobile User)**: cancelled orders get no visual de-emphasis beyond an 11sp pill color — no dimming or strikethrough for `CANCELLED` — so a quick glance can't reliably separate "needs my attention" from "already closed out." Orders with `paymentSummary == null` show no payment info at all (the `order.paymentSummary?.let { ... }` guards at lines 148 and 162 simply render nothing), which reads to a distracted scanner as "nothing owed" rather than "unknown."

## Minor Observations

- `Total` (`OpticalOrderListScreen.kt:184-188`) gets no accent color despite DESIGN.md prescribing Deep Lens Cyan for price display on the Frame Card precedent — a missed brand touch on the second-most financially relevant number on the card.
- The outer `Column(modifier.fillMaxSize())` wrapping the `when` block (`OpticalOrderListScreen.kt:47`) is redundant since every branch already fills its own size.
- No date/status grouping for long order histories — a purely flat, unsectioned list once a patient accumulates enough orders.
- The empty state (`:52-64`) hand-rolls its own layout instead of reusing the shared `EmptyContent` composable that already exists in the codebase with consistent padding/action-label support.

## Questions to Consider

- Now that the Current/History filter is gone, is a flat, unfiltered, unsorted list genuinely the intended long-term IA for a patient who might accumulate years of eyewear orders — or was the filter dropped as scope-cut collateral when the old combined hub was split apart?
- Why does the detail screen re-derive status color instead of calling the same `orderStatusColor` helper the list already uses — intentional divergence, or drift from separate edits?
- Is silently omitting payment information for orders with a null `paymentSummary` an acceptable backend contract, or should the UI treat "payment info unavailable" as its own visible state, given this screen exists specifically to answer "do I owe money"?
