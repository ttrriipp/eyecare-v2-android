---
target: Accessory cart screen
total_score: 20
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 4
target_identity: "file:C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\accessories\\AccessoryCartScreen.kt"
target_fingerprint: "sha256:7330c29a7f4a06d9d730beb74a915c48ddacc38081fc57f68102ba6ede6e941d"
target_path: "C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\accessories\\AccessoryCartScreen.kt"
timestamp: 2026-09-22T16-25-02Z
slug: tation-accessories-accessorycartscreen-kt-8794f229
---
# Accessory Cart Screen critique

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|---:|---|
| 1 | Visibility of System Status | 2/4 | Quantity and total are visible, but edits and clear have no confirmation or undo. |
| 2 | Match System / Real World | 3/4 | Familiar cart controls and PHP pricing; “Review order request” needs clearer checkout context. |
| 3 | User Control and Freedom | 2/4 | Back, remove, and clear exist, but destructive actions cannot be undone. |
| 4 | Consistency and Standards | 2/4 | Material structure is present, but controls, radius, color, and inset choices drift from Eyecare guidance. |
| 5 | Error Prevention | 1/4 | Clear/remove are immediate, quantity limits are silent, and availability is not surfaced. |
| 6 | Recognition Rather Than Recall | 3/4 | Product, variant, quantity, and unit price are visible; line totals and item count are missing. |
| 7 | Flexibility and Efficiency | 2/4 | Steppers are quick, but there is no direct quantity entry or bulk management. |
| 8 | Aesthetic and Minimalist Design | 3/4 | Calm structure, but the sparse image/error treatment and missing commerce context weaken the finish. |
| 9 | Error Recovery | 1/4 | No cart-level error, image failure, snackbar, or undo path is visible. |
| 10 | Help and Documentation | 1/4 | The estimate disclaimer helps, but request semantics and limits are not explained contextually. |
| **Total** | | **20/40** | **Acceptable, but significant improvements are needed before release.** |

## Design Specificity Verdict

Moderately product-specific in language but visually category-interchangeable. Philippine peso formatting and the “clinic review” estimate caveat connect the screen to Eyecare’s accessory-request workflow. The composition itself—generic app bar, bordered rows, stepper, delete icon, and CTA—could ship unchanged in almost any retail cart. The strongest opportunity is to frame this as a clinic-reviewed request basket rather than a conventional checkout cart.

The deterministic detector returned zero findings (`[]`, exit code 0), with no false positives. That scan does not understand Compose semantics, so the source review still identifies contrast, touch-target, destructive-action, and recovery issues.

## Overall Impression

The flow is structurally understandable: items stay above a separated summary and the screen correctly says the amount is only an estimate. The highest-impact gap is trust during editing: the cart can change or disappear silently, while the user cannot verify line totals or current availability before reviewing the request.

## What’s Working

1. The scrollable list and separated summary/action area create a clear top-to-bottom task flow.
2. Each row groups product, variant, quantity, and unit price in one place.
3. “Final total will be confirmed by the clinic after review” correctly communicates that submitting is not immediate payment or a stock reservation.

## Priority Issues

### [P1] Empty cart is a dead end

`EmptyContent` receives only a message even though it says “Browse accessories.” Users must infer that Back returns to the catalog. Add an explicit `onBrowseAccessories` callback and a visible 48–52dp browse action.

### [P1] Destructive edits are irreversible and silent

Clear and Remove act immediately, and decrementing quantity 1 removes the item. Keep minus at one, make Remove an explicit decision, add a Snackbar Undo for item removal, and confirm Clear. Announce quantity changes to TalkBack.

### [P1] Pricing and availability do not support confident review

Rows show only unit price, while the total includes quantity. `AccessoryCartItem.availability` is never rendered. Show `quantity × unit price`, a line subtotal, a text availability badge, and a clear stale/unavailable state before review.

### [P1] Quantity controls are too small and too close

The explicit 32dp IconButtons conflict with the project’s 48dp touch-target requirement, and the 4dp gap increases mis-taps. Use a single bordered 48dp stepper with at least 8dp spacing; keep Remove separated.

### [P2] Cart styling and contrast drift from Eyecare

Prices use `MaterialTheme.colorScheme.primary` as light-surface text, although the design system requires `EyecareColors.current.accentText`. Add navigation-bar insets, make the CTA explicitly 52dp and pill-shaped, and align cards to the established 16dp border-led treatment.

## Persona Red Flags

**Casey — distracted mobile user:** the primary action is well placed at the bottom, but 32dp controls are easy to miss, edits have no confirmation, image loading has no fallback, and the bottom surface does not explicitly protect navigation-bar insets.

**Riley — deliberate stress tester:** stale availability and prices are not surfaced, max quantity is silently enforced, failed images can appear blank, and Clear/Remove cannot be recovered.

**Jordan — confused first-timer:** the empty-state copy promises browsing without offering the action, “Review order request” does not clearly say whether money is charged, and no item count or line subtotal makes verification harder.

## Minor Observations

- Add an item count to the title or summary (`Cart · 3 items`).
- Use product-specific labels such as “Decrease quantity for Lacryl Hydrate Eye Drops.”
- Prefer `ContentScale.Fit` for packaging photography and show “Photo unavailable” on image errors.
- Surface “Up to 5 per variant” near the stepper.
- Consider letting a row open the accessory detail page.

## Questions to Consider

- Is this truly a retail cart, or should it be framed as a clinic-reviewed request basket?
- Should quantity one ever be removed by tapping “−,” or should Remove always be explicit?
- What would make a patient certain that the amount is only an estimate and no payment or stock hold occurs yet?
