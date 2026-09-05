---
target: the appointments list page
total_score: 25
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 4
timestamp: 2026-09-04T16-43-38Z
slug: presentation-appointments-appointmentlistscreen-kt
---
# Impeccable critique - Appointments list

## Design Health Score

Mode: Operate. The page supports browsing, filtering, requesting, and maintaining clinic appointments.

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of System Status | 2/4 | Loading, refresh, pagination, and errors exist, but the initial confirmed-appointment spinner hides requests and background refresh failures are silent. |
| 2 | Match System / Real World | 3/4 | Upcoming, History, dates, times, and statuses are familiar; preferred request times can still look like booked visits. |
| 3 | User Control and Freedom | 3/4 | Tabs, calendar close, pull-to-refresh, retry, and detail exits exist; there is no clear date reset or refresh-failure recovery affordance. |
| 4 | Consistency and Standards | 3/4 | Material patterns and shared status treatment are strong, but 38dp date controls, custom tiny type, and Visits/Appointments naming drift from the system. |
| 5 | Error Prevention | 2/4 | Detail confirmations are good, but list semantics do not prevent confusing pending requests with confirmed visits, and date dots can describe hidden items. |
| 6 | Recognition Rather Than Recall | 3/4 | Status, title, date, and time are visible; users must infer card clickability, dot meaning, and the current week/month. |
| 7 | Flexibility and Efficiency | 3/4 | Saved state, date filtering, refresh, pagination, and rating support repeat use; two independent pagination paths add friction. |
| 8 | Aesthetic and Minimalist Design | 3/4 | Calm cards and restrained color fit Eyecare, but two entity types, filters, rating chips, and dual pagination make the page busy. |
| 9 | Error Recognition and Recovery | 2/4 | Retry exists and empty copy gives some direction, but refresh failures are silent and some appointment errors expose raw messages. |
| 10 | Help and Documentation | 1/4 | The list has no contextual explanation for the request lifecycle or how a patient should interpret the combined page. |
| **Total** |  | **25/40** | **Acceptable - significant improvements needed before users are happy** |

## Design Specificity Verdict

The screen feels authored for Eyecare in behavior, but only moderately distinctive in composition. It understands the product: confirmed appointments and patient-owned requests are separate, duplicate accepted requests are suppressed, Upcoming/History is persisted, clinic-local dates are formatted, and status pills are shared with detail surfaces. The visual structure remains category-familiar: title, segmented tabs, optional calendar strip, stacked cards, and a floating request action. Eyecare character comes from its warm canvas, cyan selection language, typography, and restrained cards rather than from an appointments-specific interaction model.

The deterministic detector found zero findings (`[]`, exit code 0) for `AppointmentListScreen.kt`; there are no rule names or emitted locations, and no false positives to dismiss. Its limited mechanical review still identified the 38dp clickable date cells and likely narrow-screen crowding in the fixed six-day strip. Browser visualization and overlays were not applicable: this is native Compose source with no exposed browser-renderable or native runtime surface.

## Overall Impression

This is a thoughtful appointments hub with stronger product logic than its first impression suggests. Confirmed visits feel reassuring, but pending requests and confirmed appointments share too much visual weight, while a second data source makes loading, refresh, and recovery states feel incomplete. The single biggest opportunity is to make the appointment lifecycle explicit: what is merely requested, what is confirmed, and what the patient can trust right now.

## What's Working

- The product-aware split between requests and confirmed appointments, duplicate suppression, Upcoming/History filtering, and clinic-local date/time formatting are strong information-architecture decisions.
- Status is communicated with text, not color alone, and the shared status-pill treatment keeps the list and detail surfaces coherent.
- The state scaffolding is thoughtful: retained content during refresh, independent request/appointment pagination flags, inline retries, limited-account gating, and a contextual Rate this visit action.

## Priority Issues

### [P1] The two async sources do not share a stable, recoverable page shell

**Why it matters:** The top-level `when (uiState)` can hide already-loaded requests behind a confirmed-appointment spinner. If both sources fail, the full-page retry only retries confirmed appointments. Returning from detail can also leave a rescheduled or cancelled confirmed appointment stale because resume refreshes only the request ViewModel (`AppointmentListScreen.kt:113-185`; `AppointmentListViewModel.kt:48-98`).

**Fix:** Render the title, tabs, request action, and both section containers immediately. Give each section its own loading, empty, error, and retry state; add a shared resume refresh for both datasets or merge mutation results into the retained list. Preserve visible data and show a small patient-safe refresh failure message instead of silently stopping the indicator.

**Suggested command:** `$impeccable harden`

### [P1] Failed appointment pagination can skip a page on retry

**Why it matters:** `loadMore()` increments `currentPage` before the request (`AppointmentListViewModel.kt:102-108`). If that request fails, the retry increments again and asks for the following page (`AppointmentListViewModel.kt:205-228`), silently omitting appointments from the patient’s history.

**Fix:** Compute the next page without mutating the committed page, advance `currentPage` only after success, and retry the same page after failure. Add a regression test for failure-then-retry behavior.

**Suggested command:** `$impeccable harden`

### [P1] Pending requests and confirmed visits share too much visual meaning

**Why it matters:** A patient can read “Preferred time” in an Upcoming request card as a reserved appointment. The backend’s important reassurance - no time is reserved until the clinic confirms - is available in request presentation logic but not repeated on the list (`AppointmentListScreen.kt:827-875`; `AppointmentRequestPresentation.kt:61-70`).

**Fix:** Make the lifecycle explicit in the list: use a clearly secondary “Awaiting clinic confirmation” treatment for pending requests, label real rows “Confirmed visit,” and add concise non-binding copy. For accepted requests with an appointment ID, provide a direct route to the confirmed visit.

**Suggested command:** `$impeccable clarify`

### [P1] The date filter is undersized, ambiguous, and semantically noisy

**Why it matters:** The clickable day surfaces are 38dp, below Eyecare’s 48dp target requirement. The strip has no visible week/month context, the 4dp dot has no legend or accessible announcement, and dots are calculated from all appointments and requests even when the selected tab/date filter hides some of them (`AppointmentListScreen.kt:303-367`, `618-656`). On a 360dp viewport, the center strip is also tighter than its six 38dp cells plus spacing allow.

**Fix:** Keep the clinic’s intentional Monday-Saturday range, but make the interactive hit areas at least 48dp and use responsive or scrollable geometry. Add a visible week/month range and Today/clear affordance, derive markers from the active visible dataset, and announce each day’s date, selected state, and count to TalkBack.

**Suggested command:** `$impeccable adapt`

### [P2] Naming and card affordances make the list harder to parse than necessary

**Why it matters:** Bottom navigation says “Visits,” the page says “Appointments,” and the underlying coordinator distinguishes “Requests” and “Confirmed,” while the user sees “Upcoming” and “History.” The whole card is clickable without a chevron or visible “View details” cue, and the empty state says “Book” while the actual action says “Request appointment” (`AppointmentListScreen.kt:218-220`, `441-442`, `612-675`).

**Fix:** Choose one naming model and apply it across navigation, headings, and empty states. Keep Upcoming/History if it remains primary, but make the request/confirmed section labels and lifecycle copy explicit. Add a trailing chevron or equivalent semantic “View details” affordance to each card.

**Suggested command:** `$impeccable clarify`

## Persona Red Flags

### Jordan - first-timer

- “Visits” versus “Appointments” and “Book” versus “Request appointment” force unnecessary interpretation.
- A pending request shows “Preferred time” without saying that the time is not reserved.
- Jordan must guess that the entire card opens details and that the calendar dot means an item exists.
- The date strip gives no week/month context.

### Sam - accessibility-dependent user

- Directly clickable 38dp day cells miss the project’s 48dp target requirement (`AppointmentListScreen.kt:332-338`).
- Day cells do not explicitly announce selected state, appointment count, or dot meaning; single-letter weekdays make Tuesday and Thursday indistinguishable.
- Hand-sized 9sp/13sp calendar typography is fragile at increased font scales.
- Loading and refresh transitions have no explicit state announcement. Status pills are a positive exception because they include text.

### Casey - distracted mobile user

- The fixed calendar geometry crowds a 320-360dp viewport, and pagination controls sit after potentially long combined lists.
- Pull-to-refresh is gesture-only; a slow connection starts with a bare full-screen spinner while the other source may already have usable content.
- A fixed `bottom = 116.dp` FAB offset is fragile across device/inset configurations.
- Persisted date/filter state can make a returning user think appointments disappeared when the selected day is simply no longer relevant.

## Minor Observations

- Requests and confirmed appointments are sorted within separate sections rather than one chronological timeline.
- Both “Load more requests” and “Load more appointments” can appear on one page.
- Raw timestamp fragments and `Locale.US` fallbacks can produce awkward dates when parsing fails.
- Legacy requests without an appointment type can render an empty summary line.
- After rating, the chip disappears without a visible “Rated” or “Updated” closure.
- The Monday-Saturday strip is intentional, but its schedule assumptions should stay aligned with the clinic-authoritative availability policy.

## Questions to Consider

- Which primary model should lead the page: **Upcoming/History**, **Confirmed visits/Requests**, or **one timeline grouped by lifecycle status**?
- Should pending requests become visually secondary with explicit “not reserved” copy, or should the page keep equal card weight and rely on the status pill?
- Which improvement matters most first: **state freshness and retry recovery**, **request-versus-confirmed clarity**, or **calendar accessibility/responsive behavior**?
