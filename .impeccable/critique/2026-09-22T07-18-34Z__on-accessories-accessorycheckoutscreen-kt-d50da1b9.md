---
target: Review order request checkout screen
total_score: 26
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 3
target_identity: "file:C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\accessories\\AccessoryCheckoutScreen.kt"
target_fingerprint: "sha256:ebef039f9e9c85e1c0862426523b4961d5266a57f5855fbccd7ef760fc544158"
target_path: "C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\accessories\\AccessoryCheckoutScreen.kt"
timestamp: 2026-09-22T07-18-34Z
slug: on-accessories-accessorycheckoutscreen-kt-d50da1b9
---
Method: dual-agent (A: checkout_design_assessment · B: checkout_detector_assessment)

## Design Health Score

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of system status | 3/4 | Source has submitting/success/error states, but the idle screen does not explain response timing or what happens immediately after submission. |
| 2 | Match with real world | 3/4 | “Items,” PHP pricing, clinic review, and local discount categories feel appropriate; “estimated total” still needs a clearer plain-language explanation. |
| 3 | User control and freedom | 3/4 | Back navigation and reversible discount selection work, but there is no direct “Edit cart” action or clear cancel behavior during submission. |
| 4 | Consistency and standards | 2/4 | Native Material patterns are familiar, but repeated identical surfaces flatten hierarchy and the row plus nested radio are both clickable. |
| 5 | Error prevention | 2/4 | The warning helps, but the source has no explicit empty-cart state and does not clearly explain how an approved discount changes the displayed amount. |
| 6 | Recognition rather than recall | 3/4 | The item, amount, choices, and caveat are visible together; the item/variant text is dense and small. |
| 7 | Flexibility and efficiency | 2/4 | The flow is linear, with no quick edit path or persistent action for longer carts and larger text sizes. |
| 8 | Aesthetic and minimalist design | 3/4 | Clean and calm overall, but the red warning competes with the primary action and review copy repeats. |
| 9 | Error recovery | 3/4 | Retry and “View my requests” states exist in code; raw server messages could still be vague or technical. |
| 10 | Help and documentation | 2/4 | Inline guidance exists, but approval timing, eligibility verification, and the no-payment/no-reservation boundary need one concise explanation. |
| **Total** |  | **26/40** | **Acceptable — significant improvements needed before users are fully confident.** |

### Design specificity verdict

This feels moderately authored for Eyecare: Filipino peso, clinic review language, and Senior Citizen/PWD categories are product-specific. The structure is still close to a generic Material checkout. The largest missed opportunity is reassurance about what happens next—no payment yet, no stock hold, clinic review, and when the patient should expect an answer.

The deterministic Impeccable detector returned `[]`; it found no rule-based findings. Browser evidence was skipped because this is native Jetpack Compose Android code with a device screenshot, not a DOM/localhost surface; no overlay or live-server evidence was used.

### Overall impression

The sequence is understandable: review items → choose a discount → submit a request. The biggest opportunity is to make the review feel more trustworthy and less alarming: strengthen the item/total hierarchy, replace the error-looking disclaimer treatment, and make accessibility and long-cart behavior explicit.

### What’s working

- The primary task is clear and the sections are ordered logically.
- The selected discount is communicated by both radio state and a tinted row, rather than color alone.
- The disclaimer correctly says the request does not reserve stock or guarantee the discount.

### Priority issues

1. **[P1] The total and selected state are too pale**

   **Why it matters:** The amount is the key thing users verify before submitting, yet `colorScheme.primary` is light cyan on a light surface. The screenshot makes ₱350.00 less prominent than it should be.

   **Fix:** Use the project’s darker `accentText` color for the amount and selected radio treatment; reserve bright cyan for fills, borders, and the primary CTA.

   **Suggested command:** `$impeccable colorize`

2. **[P1] A normal policy caveat looks like an error**

   **Why it matters:** The pink/red panel visually competes with the CTA and can make users think the request is invalid.

   **Fix:** Use a neutral or amber informational surface with an info icon and concise copy such as: “Request only — stock and discounts are not confirmed. The clinic will confirm availability, eligibility, and final price.”

   **Suggested command:** `$impeccable clarify`

3. **[P1] Discount rows expose duplicate click targets**

   **Why it matters:** Each `Surface(onClick = …)` contains a clickable `RadioButton`. TalkBack can announce two actions for one choice, and touch events can feel inconsistent.

   **Fix:** Make the entire row the sole click target and set the nested radio’s `onClick` to `null` (or make only the radio interactive). Ensure the row exposes one label and selected state to accessibility services.

   **Suggested command:** `$impeccable harden`

4. **[P2] The action is not resilient to long carts or larger text**

   **Why it matters:** The CTA is inside the scroll column. It looks thumb-friendly for one item, but multiple variants or 1.3× text scale push the main action far below the fold.

   **Fix:** Use an inset-aware sticky bottom action area with the CTA and compact total; keep the content scrollable above it. Enforce the intended 48–52dp primary-button height.

   **Suggested command:** `$impeccable layout`

5. **[P2] Empty-cart and pricing/discount edge states are under-explained**

   **Why it matters:** An empty cart currently renders the review form with a disabled submit button. “Estimated total” also does not say whether it is before an approved discount, and the user cannot see whether discount selection is optional.

   **Fix:** Add an explicit empty state with “Back to catalog,” label the amount “Estimated total before approved discount,” mark the discount request “Optional,” and preserve a clear retry/edit path for server-side changes.

   **Suggested command:** `$impeccable shape`

### Persona red flags

**Casey — distracted mobile user**

- The CTA is in the thumb zone for one item but is not sticky for larger carts.
- The pale cyan amount and small caveat are easy to miss in glare or while interrupted.
- Return/recreation behavior for scroll position and selected discount is not obvious.

**Jordan — first-timer**

- “Order request,” “estimated total,” and “clinic review” do not explicitly say that no payment or stock hold occurs yet.
- The red caveat feels like an error rather than a normal rule.
- The PWD expansion is helpful; keep it.

**Riley — stress tester**

- Long product names, several items, or larger font sizes can push the CTA below the fold.
- `state.message` is rendered directly, so a backend message may be technical or non-actionable.
- Back remains available during submission; define whether leaving mid-request is safe.
- Success says “View my requests” while the callback is singular (`onViewRequest(requestId)`); align the label with the actual destination.
- Test TalkBack with the nested row/radio interaction.

### Minor observations

- Add an “Edit cart” affordance if Back is the only way to change items.
- The item card would scan better with a small product image, distinct product name/variant lines, and a clearer quantity.
- The repeated clinic-review copy can be reduced to one always-visible summary plus optional detail.
- Test long names, several variants, empty carts, server validation errors, and 1.3× font scale.
- The source relies on default `Button` sizing; confirm it matches the project’s primary-button token.

### Questions to consider

- Is discount selection optional, and should “No discount” be the default?
- Should the amount explicitly read “Estimated total before approved discount”?
- What response time should the clinic promise after submission?
- Should the review screen offer direct cart editing?
