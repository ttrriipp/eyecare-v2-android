---
target: eyewear order details page
total_score: 26
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 2
target_identity: "file:C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\eyewear\\OpticalOrderDetailScreen.kt"
target_fingerprint: "sha256:2f5af234aac566ec056b95f55adc7ca9fc8dd53aefceeb3b7750f5944a2afe3d"
target_path: "C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\eyewear\\OpticalOrderDetailScreen.kt"
timestamp: 2026-09-23T01-41-22Z
slug: ation-eyewear-opticalorderdetailscreen-kt-2b106692
---
## Eyewear order details — design critique

### Design health

| # | Heuristic | Score | Key issue |
|---|---|---:|---|
| 1 | Visibility of system status | 3/4 | Status and deadline are visible, but “Pull down to refresh” doesn’t match the available refresh controls. |
| 2 | Match with the real world | 4/4 | Patient-friendly order and payment language translates workflow states clearly. |
| 3 | User control and freedom | 2/4 | Back navigation exists, but a rejected one-shot proof has no clear recovery path. |
| 4 | Consistency and standards | 3/4 | Material patterns are familiar; some payment text uses bright primary cyan where the app’s design system calls for a darker text color. |
| 5 | Error prevention | 2/4 | The form validates fields and file constraints, but doesn’t preview the proof before its one-shot submission. |
| 6 | Recognition rather than recall | 3/4 | Payment details are shown inline and the account number is copyable; the order reference isn’t. |
| 7 | Flexibility and efficiency | 2/4 | Payment is a long, linear flow with few mobile shortcuts. |
| 8 | Aesthetic and minimalist design | 3/4 | Calm, restrained cards, but the status is repeated and the timed task comes after several secondary sections. |
| 9 | Error recognition and recovery | 2/4 | Upload feedback is visible, but rejection doesn’t explain what the patient can do next. |
| 10 | Help and documentation | 2/4 | Payment guidance is inline, but there’s no obvious support path when a proof is rejected. |
| **Total** |  | **26/40** | **Acceptable: strengthen the high-stakes payment path and recovery cues.** |

### Design specificity

The screen feels grounded in Eyecare’s clinic workflow: it has clinic payment instructions, peso amounts, order stages, and pickup context. Its visual language is calm and consistent with the app, but the stacked banner/card/tracker arrangement is still familiar commerce UI; the product’s specificity comes more from its workflow than its composition.

The automated scan found **0 issues** for the target file (`[]`), so there were no detector findings or false positives to merge. This was a source-based review: there’s no target screenshot fixture, and the connected phone was showing another app, so the rendered fold and contrast weren’t visually verified. Browser overlays don’t apply to this native Compose screen.

### Overall impression

The page explains order progress and payment clearly once the patient finds the relevant details. The biggest opportunity is to make the time-sensitive payment action the first thing a patient sees in the pending-payment state—and protect that one-shot proof submission with a preview and an actionable rejection outcome.

### What’s working

- Status copy translates backend stages into patient language, such as “Payment under review” and “Ready for pickup.”
- Payment methods, account details, amount, QR option, and deadline are shown in context; the account number also has a copy action.
- The screen uses familiar Material controls and restrained surfaces rather than introducing a competing visual style.

### Priority issues

1. **[P1] Make proof submission safer and explain rejection recovery.** The selected file is represented by its filename, with no image preview or final review, in `PaymentProofForm.kt:421`. The API contract says rejected proofs cannot be resubmitted in the MVP; rejection cancels the order (`docs/API_CONTRACT.md:2592`). Add a preview and a confirmation step showing the proof, payment method, sender, reference, and amount. If rejected, tell the patient the permitted next step—such as contacting the clinic or starting a new request—instead of leaving them at the rejection reason. Suggested command: `$impeccable harden`.

2. **[P1] Move the timed payment task up.** The page orders the status card, progress tracker, eyewear details, and payment summary before the proof form (`OpticalOrderDetailScreen.kt:223`). But the contract gives accepted orders a 30-minute payment deadline. In the pending-payment state, put amount due, deadline, instructions, and proof upload immediately after the order summary; move the tracker and item recap below or collapse them. Suggested command: `$impeccable layout`.

3. **[P2] Reduce repeated status and correct the refresh cue.** The guidance banner, status pill, and tracker can all communicate the same state. Also, “Pull down to refresh” is shown for unknown status, while this content uses a regular vertical scroll; refresh is offered elsewhere as a button. Keep one prominent current-state message and either implement pull-to-refresh or change the copy to match the actual control. Suggested command: `$impeccable clarify`.

4. **[P2] Make payment status and progress readable without color alone.** Payment headings use the bright primary color on lightly tinted surfaces, contrary to the Eyecare design token guidance. The tracker labels its stages, but does not announce which are completed/current/upcoming as tracker states. Use the app’s text-safe accent token and expose each step’s state semantically. Suggested command: `$impeccable audit`.

### Persona red flags

- **Casey, distracted mobile user:** Must scroll through several sections before reaching the timed payment and upload task. The order reference is visible but has no quick-copy action.
- **Jordan, first-timer:** Sees a rejection reason but no clear instruction about whether to contact the clinic or create a new order. A filename alone also makes it harder to verify the selected proof.
- **Sam, accessibility-dependent user:** The tracker’s current/completed states rely on visual styling; a screen reader may hear stage names without knowing which state applies.

### Minor observations

- Add a copy action for the order reference, matching the account-number affordance.
- Sender and reference fields use ordinary remembered state; consider preserving them if the screen is recreated during an interruption.

### Questions to consider

- Since proof rejection cancels the order and can’t be resubmitted, what exact next step should the patient see?
- Should pending payment put the deadline and proof form before the full tracker and item recap?
- What would make the tracker’s current state clear without relying on color?
