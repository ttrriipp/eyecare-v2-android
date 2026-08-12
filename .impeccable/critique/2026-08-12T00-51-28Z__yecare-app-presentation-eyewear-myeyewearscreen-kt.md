---
target: the my eyewear page
total_score: 20
max_score: 40
na_heuristics: 
p0_count: 1
p1_count: 2
timestamp: 2026-08-12T00-51-28Z
slug: yecare-app-presentation-eyewear-myeyewearscreen-kt
---
Method: dual-agent (A: general-purpose design-review sub-agent · B: general-purpose detector/evidence sub-agent)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 1 | No badge on the Profile "My Eyewear" entry row for pending items, and pagination silently caps at page 1 with no visible "more available" signal. |
| 2 | Match System / Real World | 2 | Status labels like "Preparing" (QUEUED) vs. "In preparation" (IN_PROGRESS) are near-synonyms with no explanatory sentence, unlike the reservations flow's `reservationStatusExplanation`. |
| 3 | User Control and Freedom | 3 | Tabs, back navigation, and the cross-links between an order and its source estimate all work. |
| 4 | Consistency and Standards | 1 | A stock, unstyled `TabRow` at the hub level, plus a custom pill `OrderFilterRow`/`EstimateFilterRow` underneath — neither matches DESIGN.md's canonical segmented-tab recipe used elsewhere in the app (Appointments, and the just-reworked Reservations list). |
| 5 | Error Prevention | 2 | Per-tab retry on initial load works; nothing prevents showing stale status after returning from a detail screen where the user just took an action. |
| 6 | Recognition Rather Than Recall | 2 | The identical `Icons.Outlined.Receipt` glyph is reused at the Profile entry point and both tabs' empty states — zero glanceable differentiation between "estimates" and "orders." |
| 7 | Flexibility and Efficiency of Use | 2 | Only a Current/History filter; no further accelerators, consistent with the rest of the app's task screens. |
| 8 | Aesthetic and Minimalist Design | 3 | Individual cards are clean, but `OrderCard` stacks up to 8 same-weight rows with no sub-grouping. |
| 9 | Error Recovery | 3 | `ErrorContent`'s retry path is solid for a full-list failure; a load-more failure is a bare error string with no retry action, and load-more is unreachable regardless (see P0). |
| 10 | Help and Documentation | 1 | No copy anywhere on the hub explains what distinguishes an "estimate" from an "order," despite it being the core distinction the screen exists to make legible. |
| **Total** | | **20/40** | **Acceptable** |

## Design Specificity Verdict

**LLM assessment**: `MyEyewearScreen.kt` itself is a thin, generic tab shell — 93 lines, a stock `TabRow`, no copy explaining what each tab means, no summary state, no icon differentiation between the two tabs it hosts. It reads as "My Orders" from any e-commerce template, not a screen authored around Eyecare's real distinction: an estimate (a quotation the clinic prepared, pending your acceptance) versus an order (something you've already committed to, now being fulfilled and paid for). Compare this to `FrameReservationListScreen.kt` — recently reworked with hold-expiry banners, resume-aware refresh, and disciplined item capping — and the contrast is stark: this hub, which DESIGN.md explicitly names as an existing surface, received none of that authorship. The individual tab content (`EstimateListScreen.kt`, `OpticalOrderListScreen.kt`) is more developed, but the two files are close to 80% duplicated structure, which is very likely *why* the color-mapping drifted apart between them (see Priority Issues).

**Deterministic scan**: `detect.mjs` returned `[]`/exit 0 on the target file — no Kotlin rules exist, so that's "no signal," not "clean." Mechanical scan of `MyEyewearScreen.kt` itself found good hygiene where there's little to get wrong: zero hardcoded colors/sizes, the one icon present has a real `contentDescription`, and the only interactive elements (`IconButton`, `Tab`) are semantic components. But it's a 93-line shell with a single `contentDescription` in the whole file and zero `Modifier.semantics{}` blocks — there's simply very little accessibility surface here to audit, because there's very little screen here.

**Visual overlays**: N/A — native Android Compose target, no browser rendering available.

## Overall Impression

The hub shell itself is inoffensive but empty-handed — it does nothing to help a patient tell "my estimate" from "my order" apart, which is the one job a hub over two related-but-distinct concepts has. Underneath it, the two tabs' near-duplicated card code has drifted into a real correctness bug: two opposite-meaning order statuses now render as the same color. And a full pagination path — UI state, ViewModel logic, error handling — was built and then never wired to anything that triggers it.

## What's Working

- **Cross-linking between domains is structurally present**: `OrderCard`'s "View original estimate" button (`OpticalOrderListScreen.kt:226-230`) and the reverse link on the estimate side both acknowledge that these two concepts are related, not just visually similar.
- **Clean, consistent state modeling**: both `EstimateListViewModel` and `OpticalOrderListViewModel` use the same sealed `UiState` shape (Loading/Success/Empty/Error) and the same Current/History filter pattern — predictable to extend, easy to reason about.
- **`ErrorContent`'s retry path** for a failed initial load is solid and consistent with the rest of the app.

## Priority Issues

**[P0] Pagination is built but structurally unreachable — history lists silently cap at page one.**
Why it matters: confirmed both `EstimateListContent` and `OpticalOrderListContent` accept an `onLoadMore: () -> Unit` parameter wired to `vm::loadMore` in `MyEyewearScreen.kt:73,85`, and both ViewModels implement real `loadMore()`/`loadMoreInternal()` logic with `isLoadingMore`/`loadMoreError` state. But confirmed: neither list's `LazyColumn` has a scroll listener, and neither screen renders a "Load more" button anywhere — `onLoadMore` is never actually invoked from either file. A patient with a long estimate or order history has no way to see anything past the first page, with no visual indication more exists.
Fix: add a bottom-of-list trigger (scroll-position `derivedStateOf` check, or an explicit "Load more" button matching the pattern already used in `AppointmentListScreen.kt`'s request list).
Suggested command: `/impeccable harden`

**[P1] `READY_FOR_DISPENSING` and `DISPENSED` render as the identical color — the exact bug class just fixed in Reservations.**
Why it matters: confirmed at `OpticalOrderListScreen.kt:166-167` — both statuses resolve to `MaterialTheme.colorScheme.tertiary`. "Your glasses are ready to pick up" and "you already picked them up" are opposite-urgency states rendered identically. The same pattern repeats one level down: `PaymentStatus.VOIDED` and `PaymentStatus.UNKNOWN` (lines 206-207) both resolve to `onSurfaceVariant`. This is the same class of collision `ReservationPresentation.kt` explicitly fixed for `RELEASED`/`UNKNOWN` last session, with an inline comment distinguishing a resolved neutral state from one that needs attention — that fix wasn't carried over here.
Fix: give `READY_FOR_DISPENSING` its own color (e.g. `statusPending` amber, matching "action needed") distinct from `DISPENSED`'s neutral/confirmed tone; route payment `UNKNOWN` through an attention-worthy color distinct from the neutral `VOIDED`.
Suggested command: `/impeccable colorize`

**[P1] `refresh()` exists on both ViewModels but is never called — same dead-control pattern as the reservation detail screen's `dismissCancelError`.**
Why it matters: confirmed `OpticalOrderListViewModel.kt:53` (and the equivalent in `EstimateListViewModel.kt`) defines a `refresh()` function, but `MyEyewearScreen.kt` has no lifecycle observer, no pull-to-refresh, and no other caller for it anywhere in the eyewear package. A patient who accepts an estimate, then navigates back to this hub, sees stale status until they force-kill and reopen the tab (switching tabs re-triggers `hiltViewModel()`'s `init`, but returning to an already-composed tab does not).
Fix: wire the same resume-aware refresh pattern used in `FrameReservationListScreen.kt` (`DisposableEffect` + `ON_RESUME`, with a first-resume guard) to both tabs, or at minimum to whichever tab is currently selected.
Suggested command: `/impeccable harden`

**[P2] Zero iconographic differentiation between "estimates" and "orders."**
Why it matters: confirmed the identical `Icons.Outlined.Receipt` glyph is used at the Profile entry point (`ProfileScreen.kt:212`), the Estimates empty state (`EstimateListScreen.kt:61`), and the Orders empty state (`OpticalOrderListScreen.kt:63`) — three different places, one icon, no visual cue for which domain you're looking at.
Fix: give Estimates and Orders their own distinct icon (e.g. a quote/document icon vs. a receipt/package icon), and keep the Profile entry point's icon consistent with whichever concept is primary.
Suggested command: `/impeccable colorize`

**[P3] `OrderCard` stacks up to 8 same-weight rows with the most urgent line (balance due) buried mid-stack.**
Why it matters: title, status pill, reference, date, total, payment pill, and balance-due all render at similar visual weight with uniform 8dp spacing — on a card whose single most financially urgent fact (money owed) has no more prominence than the order reference number above it.
Fix: give balance-due its own visual treatment (bolder weight, or pinned near the status pill) rather than a same-style trailing line.
Suggested command: `/impeccable layout`

## Persona Red Flags

**Jordan (Confused First-Timer)**: nothing on this hub explains what an "estimate" is versus an "order" — a first-time patient has to infer the relationship from two tab labels alone. The near-synonymous "Preparing"/"In preparation" status labels compound this: neither conveys what happens next or how long to expect.

**Riley (Deliberate Stress Tester)**: confirmed live — a patient with more than one page of order/estimate history hits the invisible pagination ceiling with no indication more exists (P0); a patient who accepts an estimate, returns to this hub, and expects to see it reflected sees stale data because `refresh()` is unreachable (P1).

## Minor Observations

- `EstimateCard` and `OrderCard` are close to 80% structurally duplicated across their two files — very likely the direct cause of the color-mapping drift between them, and a candidate for a shared component the next time either changes.
- The bare "My Eyewear" title carries none of the companion warmth DESIGN.md establishes elsewhere (e.g. Home's greeting, the reservation flow's reassurance copy) — it reads as a generic section header rather than a continuation of the app's voice.

## Questions to Consider

- Why did Reservations get a full rework — hold-expiry surfaced, resume-aware refresh, disciplined status colors — while this hub, which DESIGN.md explicitly names as an existing surface, got none of that same attention? Is that a sequencing gap worth closing now that the pattern exists to copy from?
- Should the estimate → order relationship be the hub's primary information architecture (e.g., grouping an order under its source estimate) rather than two independent tabs connected only by a text button?
- Was the `READY_FOR_DISPENSING`/`DISPENSED` color collision ever deliberately reviewed, or is it an artifact of copy-pasting `EstimateCard`'s status-color pattern into `OrderCard` without re-checking for new collisions?
