---
target: order request details page
total_score: 22
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 3
target_identity: "file:C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\accessories\\AccessoryOrderRequestDetailScreen.kt"
target_fingerprint: "sha256:3ce228d8b0a584589cedd536df6b764f5aae15515a65b72d53314b586435522c"
target_path: "C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\accessories\\AccessoryOrderRequestDetailScreen.kt"
timestamp: 2026-09-22T17-51-53Z
slug: ries-accessoryorderrequestdetailscreen-kt-c7bde7ec
---
## Design health score

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of system status | 3/4 | Pending/cancel/upload states exist, but submitted/resolved timestamps and refresh feedback are missing. |
| 2 | Match system / real world | 3/4 | Clinic language is clear, but the prominent amount is not labeled as an estimate/subtotal. |
| 3 | User control and freedom | 3/4 | Back and confirmed cancellation work; unknown/pending states lack a clear refresh or contact path. |
| 4 | Consistency and standards | 2/4 | Material structure is present, but the repeated tonal blocks flatten the hierarchy and drift from Eyecare’s border-led cards. |
| 5 | Error prevention | 2/4 | Cancellation is confirmed, but the unlabeled amount can imply a confirmed charge or final price. |
| 6 | Recognition rather than recall | 2/4 | Request ID, status, images, and items are visible; submitted date, discount declaration, and resolution details are hidden. |
| 7 | Flexibility and efficiency | 2/4 | The page is easy to scan, but status refresh and accepted-order context require another screen. |
| 8 | Aesthetic and minimalist design | 2/4 | Calm, but pale repeated surfaces and the large blank lower area make it feel unfinished. |
| 9 | Error recovery | 2/4 | Generic retry and upload errors exist; unknown status has no obvious recovery action. |
| 10 | Help and documentation | 1/4 | The inline explanation is useful, but there is no explicit “what happens next” guidance. |
| **Total** |  | **22/40** | **Acceptable; significant improvements are needed.** |

## Design-specificity verdict

Partially authored for Eyecare, approximately 5/10. The clinic-review language, Philippine peso formatting, accessory photos, and “Stock is not reserved” boundary are product-specific. The visual composition still reads like a generic e-commerce order-detail page and does not fully express Eyecare’s strongest traits: white border-led cards, clear status hierarchy, and calm but high-contrast care guidance.

The deterministic detector returned **0 findings**. Because this is a native Compose screen, browser visualization and live-server overlays were not applicable. The manual review still found contrast, semantic, and hierarchy issues the detector does not currently flag.

## Overall impression

The page is easy to understand at a glance: request number, status, amount, items, and cancellation are in a sensible order. The main opportunity is to make the page answer the patient’s real question: “What happens now, and is this amount final?” Right now the bright, unlabeled `₱1,300.00` competes with the pending status, while the only strong action is destructive cancellation.

## What’s working

- The message that clinic review is pending and stock is not reserved is honest and aligned with the request workflow.
- Item rows group product images, names, variants, quantities, and amounts in a compact, scannable layout.
- The implementation covers important states: loading, not-found/error, cancellation confirmation, discount-proof upload, upload progress, rejection reasons, and accepted-order handoff.

## Priority issues

### [P1] Label the amount and expose the request summary

The amount at `AccessoryOrderRequestDetailScreen.kt:186` is visually dominant but has no “Estimated subtotal” or “Request total” label. The model also contains submitted/resolved dates, discount declaration, and resolution details that are not shown.

Why it matters: patients may interpret the amount as a confirmed charge or final payable balance.

Fix: show a compact summary with “Estimated subtotal,” submitted date, requested discount, and resolution date/reason when available. For accepted requests, preview the resulting order status and payment deadline before the “View order” action.

Suggested commands: `$impeccable clarify`, `$impeccable layout`.

### [P1] Correct contrast and pending-state semantics

The amount uses `MaterialTheme.colorScheme.primary` as text, which is too light on the light surface. The pending badge also uses raw amber as text on a tinted chip. These are visible in the screenshot as faint cyan and amber text.

The pending guidance uses `CheckCircle` at `AccessoryOrderRequestDetailScreen.kt:527`, which implies completion even though the request is still awaiting review.

Fix: use `EyecareColors.current.accentText` for the amount, accessible status-text tokens for badges, and an info/schedule/hourglass icon for pending. Keep the explicit status label so color is never the only signal.

Suggested commands: `$impeccable audit`, `$impeccable colorize`.

### [P1] Restore a stronger Eyecare card hierarchy

The header, status guidance, and item rows all use `surfaceVariant` without borders at `AccessoryOrderRequestDetailScreen.kt:166`, `:539`, and `:235`. This creates a stack of similarly weighted gray blocks.

Fix: use white, hairline-bordered cards for the request summary and item rows; reserve the cyan-tinted surface for status guidance. Give the guidance a short title such as “Awaiting clinic review” followed by the explanation.

Suggested commands: `$impeccable layout`, `$impeccable typeset`.

### [P2] Give every workflow state a clear next step

Pending has only “Cancel request.” Unknown status has a message but no refresh action. Accepted requests jump to the resulting order without showing its status, total, or payment deadline first. Rejected/cancelled states provide limited closure.

Fix: add state-specific actions: “Refresh status” for unknown, a concise accepted-order summary plus “View order,” and a clear wait/contact explanation for pending. Keep cancellation visually secondary and near the bottom with proper safe-area spacing.

Suggested command: `$impeccable harden`.

### [P2] Protect compact-width and accessibility readability

Product names and variants are clamped at `AccessoryOrderRequestDetailScreen.kt:252`, visibly truncating the first item. The image announces the product name while the adjacent title announces it again. The upload spinner replaces the proof button label without an accessible progress description.

Fix: let detail rows expose the full snapshot name at larger font scales, or provide a “Show full name” affordance; make successful images decorative or differentiate their semantics; preserve an accessible “Uploading proof…” label/state.

Suggested commands: `$impeccable adapt`, `$impeccable audit`.

## Persona red flags

**Jordan — first-timer**

- `₱1,300.00` does not say whether it is an estimate, subtotal, or amount due.
- A check-circle beside “Awaiting review” sends contradictory status signals.
- There is no submitted date or explicit explanation of what Jordan should do next.
- “Cancel request” is the only prominent action and can feel like the default next step.

**Casey — distracted mobile user**

- The actionable content is at the bottom of a scrollable stack; discount proof or more items will push the next step farther away.
- There is no visible refresh/contact action when Casey returns after an interruption.
- The large blank lower area makes the screen feel incomplete instead of reinforcing the pending state.
- The full-width cancel control and 16dp margins are thumb-friendly positives.

**Riley — deliberate stress tester**

- Unknown status has no refresh/retry affordance.
- Long item names are permanently ellipsized with no way to inspect the complete snapshot.
- Accepted metadata exists in the model but is hidden.
- Rejection/cancellation outcomes lack structured resolution details and closure.

## Minor observations

- Label item amounts as “Line total” and show the request item count, e.g. “Items (2).”
- Use a loading placeholder distinct from the “image unavailable” state; currently the unavailable semantics can appear before image loading completes.
- Ensure status, guidance, and discount-proof secondary text use sufficient contrast.
- Give the cancellation button an explicit 48dp height and keep it clearly secondary to informational content.
- Add bottom safe-area padding so the final action remains comfortably above the gesture bar on compact devices.
- Discount-proof status text currently uses raw primary/tertiary colors on light surfaces and should use accessible text tokens.

## Questions to consider

- If stock is not reserved, why is the first dominant visual an unlabeled cyan total rather than the request state and next step?
- What should a patient do on a pending request besides cancel—wait, refresh, or contact the clinic—and where should that affordance live?
- When a request is accepted, why does the patient see only “View order” instead of the amount, status, and deadline needed to act?
- What would make this unmistakably an Eyecare clinic request rather than a generic commerce detail page?
