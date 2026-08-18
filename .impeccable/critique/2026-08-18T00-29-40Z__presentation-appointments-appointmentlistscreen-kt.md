---
target: the appointments list screen
total_score: 22
max_score: 36
na_heuristics: 10
p0_count: 1
p1_count: 2
timestamp: 2026-08-18T00-29-40Z
slug: presentation-appointments-appointmentlistscreen-kt
---
## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Pull-to-refresh drives `uiState` to `Loading`, swapping the whole content tree for a bare spinner (`AppointmentListScreen.kt:132-134`) instead of an in-place refresh indicator. |
| 2 | Match System / Real World | 3 | Request-status copy is honest and reassuring ("No time is reserved until confirmed" — `AppointmentRequestPresentation.kt:64`); no complaints. |
| 3 | User Control and Freedom | 2 | Tab/date-filter selection lives in `remember` inside `AppointmentListContent` (`:646-648`) and is silently reset every time refresh forces the screen through `Loading`. |
| 4 | Consistency and Standards | 1 | The same `AppointmentStatus` enum renders via three unrelated, disagreeing color/text implementations across this file, the detail screen, and a dead orphaned composable. |
| 5 | Error Prevention | 3 | Rating validation and inline retry rows are solid; nothing destructive lives on this screen itself. |
| 6 | Recognition Rather Than Recall | 2 | Status meaning depends on color the user must relearn per screen — worsened by the contrast bug below. |
| 7 | Flexibility and Efficiency | 3 | Week-jump calendar, tab split, and FAB are good accelerators; undercut by pagination being wired for requests only. |
| 8 | Aesthetic and Minimalist Design | 3 | Card recipes are calm and on-brand; the header-visibility flicker (P2) is the main clutter/instability source. |
| 9 | Error Recovery | 3 | `RequestListErrorRow` and the confirmed-error fallback both surface inline retry reasonably well. |
| 10 | Help and Documentation | n/a | No in-app help layer expected on this Operate surface; not applicable rather than absent. |
| **Total** | | **22/36** | **Acceptable** |

## Design Specificity Verdict

**LLM assessment**: The screen is genuinely authored for Eyecare, not a template — the FAB's "Request appointment" copy (with an inline comment explaining the confirmed-vs-pending distinction, `AppointmentListScreen.kt:200`), the bespoke `WeeklyAppointmentCalendar` week-slider with per-day appointment-count dots (`:276-363`), and the shared `requestStatusPresentation()` copy bank all reflect real product thinking about a clinic-specific workflow. But that authored quality is uneven: the confirmed-appointment status pill reuses none of the accessible-text-token discipline the request pill sitting in the same `LazyColumn` already has, and there are three different, disagreeing implementations of "how does an `AppointmentStatus` render as a badge" across this file and `AppointmentDetailScreen.kt`. It reads like two different engineers built the two halves of this hub at two different times — specific in its content/copy layer, generic/inconsistent in its systemization layer.

**Deterministic scan**: `detect.mjs --json` returned `[]`/exit 0 on all four files scanned (`AppointmentListScreen.kt`, `AppointmentRequestPresentation.kt`, `AppointmentListViewModel.kt`, `AppointmentRequestListViewModel.kt`). This is "no signal," not "clean" — confirmed in an earlier session today that `.kt` isn't in the scanner's scannable-extensions list and its rules target CSS/JSX/HTML syntax. A manual mechanical pass substituted for it and independently confirmed — verified directly against the source — the same collision class already fixed once this session in a sibling screen: `appointmentStatusLabelAndColor` (`AppointmentListScreen.kt:568-578`) maps `NO_SHOW`/`CANCELLED` to the identical `statusCancelled` hex and `FULFILLED`/`UNKNOWN` to the identical `onSurfaceVariant` grey — two pairs of semantically distinct states rendering as visually identical pills. The manual pass also found zero hardcoded hex colors, zero missing `contentDescription`, one undersized touch target (a 38dp clickable calendar day, below the 48dp floor), two hand-picked font-size overrides, and confirmed dead code (`StatusChip` at `:546-565`, zero call sites anywhere in the codebase; `appointmentCountsByDate` at `:610-611`, zero call sites; `AppointmentRequestListViewModel.retryAppend()`, zero call sites).

**Visual overlays**: N/A — native Android target, no browser/dev-server to inject into. Both assessments substituted source-level evidence in its place.

## Overall Impression

This hub carries real, specific product thinking (the calendar, the FAB copy, the request-status copy bank) but the two concepts sharing the screen — confirmed appointments and pending requests — were clearly built at different levels of care. The request half has a genuinely well-built shared presentation layer (fill/text color split, reused by both list and detail). The confirmed-appointment half never got that treatment: it has its own separate, buggier status-color function, its own pagination that's wired in the ViewModel but never surfaced in the UI, and a section header whose visibility depends on the *other* list's contents rather than its own. The fixes are mostly "copy the pattern that already works four lines away," which makes this a high-leverage, low-risk pass.

## What's Working

- `requestStatusPresentation()` / `AppointmentRequestStatusPill` (`AppointmentRequestPresentation.kt`) is a genuinely well-built single source of truth — fill color and WCAG-safe text color are derived together, and it's reused by both the list card and the detail screen. This is the pattern the rest of the app should copy.
- `WeeklyAppointmentCalendar` (`:276-363`) is a bespoke, well-crafted interaction — animated week transitions, per-day appointment dots, clinic-hours awareness — well beyond generic list-template fare.
- Confirmed and request cards share one `LazyColumn` with consistent card styling; the FAB copy decision shows the team is actively thinking about the confirmed/pending conceptual split rather than defaulting to a generic "+".

## Priority Issues

**[P0] WCAG contrast failure on the confirmed-appointment status pill — the screen's most common status.**
`AppointmentStatusPill` (`AppointmentListScreen.kt:527-543`) and its backing `appointmentStatusLabelAndColor` (`:568-578`) use the same raw brand hue (`colors.statusPending`, `colors.statusCancelled`) as both the 12%-tint fill *and* the direct text color (`color = color` at line 540). `Theme.kt`'s own comments document this exact pairing measures 1.8-3.5:1, below WCAG AA — the identical bug class already found and fixed in the sibling orders screen this session, and its fix (`statusPendingText`/`statusConfirmedText`/`statusCancelledText`) already exists in `Theme.kt` and is already correctly used four lines away in this same file's sibling, `AppointmentRequestStatusPill`. **Fix**: give `appointmentStatusLabelAndColor` a fill/text pair the way the request pill already does. → `/impeccable colorize`

**[P1] Confirmed-appointment pagination is fully wired in the ViewModel but never surfaced in the UI.**
`AppointmentListUiState.Success` carries `isLoadingMore`/`hasMorePages`/`loadMoreError` and `AppointmentListViewModel.loadMore()` exists — confirmed by grep, zero references to `viewModel::loadMore`, `hasMorePages`, `isLoadingMore`, or `loadMoreError` (for the confirmed side) anywhere in `AppointmentListScreen.kt`. Only requests get a "Load more" row (`:787-806`). Any patient with more than one page of confirmed appointments silently never sees page 2+, with no signal more exist. **Fix**: add a load-more row for `visibleAppointments` analogous to the existing requests one. → `/impeccable harden`

**[P1] Pull-to-refresh unmounts the content tree and silently resets tab/date-filter state.**
`selectedTab`/`dateFilterEnabled`/`selectedDate` are `remember`ed inside `AppointmentListContent` (`:646-648`). `PullToRefreshBox`'s `onRefresh` calls `viewModel.refresh()`, which synchronously sets `uiState = Loading`; the `when` block swaps to a bare spinner, destroying and recreating `AppointmentListContent` — and `AppointmentRequestListViewModel.refresh()` does the identical thing to the requests half. A user on the History tab or with a date filter applied who pulls to refresh gets bounced back to Upcoming/unfiltered with no warning, exactly when this should feel seamless. **Fix**: hoist the filter state to `rememberSaveable` above the `when` branch, and/or stop routing pull-to-refresh through the full-screen `Loading` state so existing content stays mounted while refreshing. → `/impeccable harden`

**[P2] Three unsynchronized status-badge implementations for one `AppointmentStatus` enum.**
`AppointmentStatusPill` (list, borderless, buggy raw-hue text), `AppointmentDetailStatusBadge` (detail screen, bordered, ad hoc per-status text-color branching), and a dead `StatusChip` (`:546-565`, zero call sites, confusingly same-named as an unrelated `StatusChip(isHeld: Boolean)` in `FrameReservationListScreen.kt:326`) all disagree on how the same status should look. The same appointment, same status, looks different depending which of the two screens shows it — while the analogous request-status pattern correctly centralizes in one shared function used by both surfaces. **Fix**: extract one `appointmentStatusPresentation()` + shared pill composable mirroring `requestStatusPresentation`/`AppointmentRequestStatusPill`; delete the dead `StatusChip`. → `/impeccable colorize`

**[P2] "Confirmed appointments" section header is contingent on the *other* list's contents.**
Confirmed by direct read: the "Appointment requests" header renders whenever `visibleRequests.isNotEmpty()` (`:743-750`), but the "Confirmed appointments" header only renders inside a nested `if (visibleRequests.isNotEmpty())` (`:758-767`) — i.e. it depends on unrelated request-list state, not its own list's content. A patient with only confirmed appointments never sees a "Confirmed appointments" heading at all; the same patient, the moment they also have one pending request, suddenly does. The IA label a user learns to scan for is not a stable structural feature of the screen. **Fix**: render "Confirmed appointments" whenever `visibleAppointments.isNotEmpty()`, independent of the requests list. → `/impeccable layout`

## Persona Red Flags

**Alex (Power User)**: filters to History or a specific date, then pulls to refresh expecting an updated list in place — instead gets bounced to a full-screen spinner and lands back on Upcoming/unfiltered. Separately, if Alex has more than one page of confirmed appointments, they never learn page 2 exists — `loadMore` is simply never called from the screen for that half.

**Sam (Accessibility)**: the SCHEDULED/CANCELLED/NO_SHOW status pill text on confirmed-appointment cards fails WCAG AA contrast — this hits SCHEDULED, almost certainly the single most frequent status a patient sees. The weekly calendar's has-appointment dot (`:342-350`) also carries no `contentDescription`/semantic label — a TalkBack user gets the day letter and number but no signal that a date has an appointment.

**Riley (Stress Tester)**: REJECTED, CANCELLED, and EXPIRED request statuses all collapse to the identical red pill — three semantically distinct outcomes (clinic declined vs. patient cancelled vs. nobody responded) read as one uniform "alarm" color, distinguished only by label text. The merged per-day appointment-count map also fuses confirmed and request dates into one opaque count, with no way to tell from the calendar dot alone whether a day holds a confirmed visit, a pending request, or both.

## Minor Observations

- `StatusChip` (`:546-565`) is dead code — zero call sites anywhere in the app, name collides with an unrelated private `StatusChip` in `FrameReservationListScreen.kt`.
- `appointmentCountsByDate` (`:610-611`) is also dead — its logic is duplicated inline instead.
- The weekly calendar's day-of-week/day-number text uses ad hoc `.copy(fontSize=…)` overrides (`:330, 337`) without documented precedent for that specific override.
- Empty-state headings (`EmptyDayCard`, `EmptyAppointmentTab`) use `titleSmall` where DESIGN.md assigns `headlineSmall` as the role for empty-state intros.
- FULFILLED and UNKNOWN both resolve to the same grey in both the list pill and detail badge — a routine "Completed" and a concerning "Status unavailable" look identical by color (consistent app-wide, so lower priority, but worth a look).

## Questions to Consider

- If `requestStatusPresentation()`/`AppointmentRequestStatusPill` is unambiguously the better-built pattern in this file, why wasn't it used as the template when `AppointmentStatusPill` was written — was the request-status pill added later, or does confirmed-appointment status simply have no owner keeping it in sync?
- Given pagination already exists end-to-end in `AppointmentListViewModel` for confirmed appointments, was surfacing it in the UI deliberately deferred, or did it get lost when the requests half of this screen was built out?
- Is the "Confirmed appointments"/"Appointment requests" split meant to be a permanent IA decision — if so, should each section's heading be a stable, independent structural element rather than one contingent on the other's contents?
