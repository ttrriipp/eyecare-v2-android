---
target: my orders page list
total_score: 24
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
timestamp: 2026-09-04T12-46-04Z
slug: eyecare-app-presentation-eyewear-myordersscreen-kt
---
## My Orders — design critique

### Design Specificity Verdict

The screen is moderately product-specific visually, but category-interchangeable in information architecture.

It follows Eyecare's calm "Clear Care Companion" direction: warm surfaces, white 16dp cards, subtle borders, cyan accents, and patient-friendly status labels. But the list itself lacks optical/clinic-specific context, omits the documented Current/History model, and reduces each order to a generic item title, amount, and reference. The same structure could be reused for pharmacy or generic retail orders without much change.

Deterministic scan: `MyOrdersScreen.kt` and `OpticalOrderListScreen.kt` both returned exit code 0 with raw JSON `[]`. The detector found 0 issues, 0 rules, and no false positives. This is useful confirmation that the source does not contain the detector's generic anti-patterns; it does not replace the product-specific UX findings below.

Browser visualization and overlay injection were not applicable: these are native Kotlin/Jetpack Compose sources, not browser-renderable HTML/URLs, and no native app surface was exposed by the available UI inventory.

### Design Health Score

| # | Heuristic | Score | Key Issue |
|---|---|---:|---|
| 1 | Visibility of System Status | 2/4 | Loading and append states exist, but failed refreshes are silent. |
| 2 | Match System / Real World | 3/4 | Status language is natural; `Ref` and generic order titles are less clear. |
| 3 | User Control and Freedom | 3/4 | Back, pull-to-refresh, retry, and detail navigation are available. |
| 4 | Consistency and Standards | 3/4 | Material and shared Eyecare patterns are used, but Current/History is missing. |
| 5 | Error Prevention | 3/4 | Read-only flow is safe, but pagination can skip a page after failure. |
| 6 | Recognition Rather Than Recall | 2/4 | Payment state and stable order identity are weak or absent on cards. |
| 7 | Flexibility and Efficiency of Use | 2/4 | Pagination exists, but there is no Current/History filtering. |
| 8 | Aesthetic and Minimalist Design | 3/4 | Calm and uncluttered, though overly sparse and under-informative. |
| 9 | Help Users Recognize, Diagnose, and Recover from Errors | 2/4 | Retry exists, but refresh failure is invisible and append retry is unsafe. |
| 10 | Help and Documentation | 1/4 | No contextual explanation of order states, payment labels, or empty results. |
| **Total** |  | **24/40** | **Acceptable; significant improvement needed** |

### Overall Impression

The happy path is restrained and readable: a patient can open an order, see its fulfillment status, and notice a balance amount. The main opportunity is to make the list answer the three questions patients actually have at a glance: “Which orders are active?”, “What exactly is this order?”, and “Do I owe anything?” Right now they must scan a mixed list, interpret a generic item title, and open detail to understand payment state.

### Cognitive Load and Emotional Journey

Three checklist failures create moderate cognitive load:

- Visual hierarchy: the item description, status, amount, and merged date/reference line compete; the order number is not the stable visual anchor.
- Working memory: a page can contain up to 15 orders, with no Current/History grouping to reduce comparison work.
- Progressive disclosure: payment status is hidden while the money line changes between “Balance due” and “Total,” forcing interpretation before opening detail.

The entry point and Back affordance establish context well. The emotional valley is the bare spinner, generic “No eyewear orders yet” state, and silent background-refresh failure. The status pill and amount are the peak of the list, but the more reassuring order tracker lives one screen deeper. Returning from detail normally refreshes while preserving content; a failed refresh leaves the patient unsure whether the visible status is current.

### What's Working

- The incumbent visual language is strong: 16dp white cards, subtle borders, restrained cyan, and semantic status pills feel cohesive with Eyecare.
- The happy-path state structure is present: first load, empty, error, pull-to-refresh, append loading, and append error are all represented.
- Navigation is predictable and typed: My Orders is a clearly bounded sub-surface with a labeled Back action and order-detail destination.

### Priority Issues

#### [P1] Current/History filtering is missing

**Why it matters:** The product context describes Current and History order views, with Current as the initial selection. The implementation always requests `filter = null`, so active and completed orders are mixed into one scan.

**Fix:** Add a two-option Material segmented control near the title, default to Current, and keep page/loading/error state independent per filter. Pass the selected filter through initial load, refresh, retry, and pagination.

**Suggested command:** `$impeccable shape`

#### [P1] Payment status is not visible on order cards

**Why it matters:** Cards show only “Balance due” or “Total.” Patients cannot confidently distinguish paid, unpaid, voided, or unavailable payment data without opening every order.

**Fix:** Show an explicit payment label alongside the amount — “Paid,” “Payment due,” “Balance due,” “Payment voided,” or “Payment status unavailable.” Keep balance due visually prominent and add due/overdue context when available.

**Suggested command:** `$impeccable clarify`

#### [P1] Refresh and pagination recovery are not trustworthy

**Why it matters:** `loadMore()` increments the page before the request. If it fails, tapping Retry increments again and can skip a page. A failed background refresh keeps stale content but gives no visible indication, and initial failures pass a raw throwable message to the UI.

**Fix:** Track the requested next page separately and advance only after success; retry the exact failed page. Add a patient-safe refresh-error banner or Snackbar stating that the last loaded orders remain visible.

**Suggested command:** `$impeccable harden`

#### [P2] Order identity and open affordance are too weak

**Why it matters:** The headline is derived from the first item description, while date and reference are merged into a de-emphasized line. Similar orders can look identical, and a fully clickable card has no visible “View details” cue.

**Fix:** Make `Order #…` the stable headline, keep the item description secondary, separate date and reference metadata, and add a visible chevron or “View order” affordance with a concise TalkBack summary.

**Suggested command:** `$impeccable layout`

#### [P2] Empty and error states are generic dead ends

**Why it matters:** “No eyewear orders yet” does not distinguish an empty Current view from an empty History view or explain when orders appear. Shared loading/error states are also visually and semantically generic.

**Fix:** Use state-specific copy such as “No current orders” and “No order history,” explain that clinic-confirmed orders appear here, and provide a relevant next action such as “View frames” where appropriate. Add explicit loading/error semantics for TalkBack.

**Suggested command:** `$impeccable harden`

### Persona Red Flags

- **Casey, distracted mobile user:** Pull-to-refresh is gesture-only, there is no Current/History shortcut, and missing payment status forces a detail-screen trip during an interrupted one-handed scan.
- **Riley, deliberate stress tester:** A failed load-more request followed by Retry can skip a page. Invalid timestamps can fall back to a raw ISO-like substring.
- **Sam, accessibility-dependent user:** Loading, empty, and error states have no explicit state semantics; Retry is generic, and important date/reference metadata is small. Status meaning should never rely on color alone.

### Minor Observations

- The list reserves 96dp of bottom padding even though My Orders is excluded from bottom navigation, likely leaving an unnecessarily large blank tail.
- Timestamp formatting preserves the source offset rather than normalizing to clinic/user locale and exposes a raw fallback on parse failure.
- There are no order-specific Compose tests for card hierarchy, empty/error states, filtering, accessibility semantics, or retry behavior.
- `CONTEXT.md` still references an older `MyEyewearScreen` route name while the implementation uses `MyOrdersScreen`.

### Questions to Consider

- What if the first decision were simply “Current or History,” before asking patients to scan orders?
- Can “Order #…” become the visual anchor instead of an item description?
- Does a patient ever need to infer payment state from the absence of “Paid”?
- What should the empty state help the patient do next: browse frames, contact the clinic, or simply understand why the list is empty?
