---
target: My Appointment pending request detail
total_score: 27
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
p2_count: 3
p3_count: 0
target_identity: "file:C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\appointments\\MyAppointmentScreen.kt"
target_fingerprint: "sha256:c27d8aa16d45d024192fea17e79b3070cad2b1a5a42e4a6c7a257cef62d7db44"
target_path: "C:\\Users\\ironm\\AndroidStudioProjects\\EyecareV2\\app\\src\\main\\java\\com\\eyecare\\app\\presentation\\appointments\\MyAppointmentScreen.kt"
timestamp: 2026-09-20T11-21-41Z
slug: ation-appointments-myappointmentscreen-kt-5dd32214
---
Method: dual-agent (Assessment A visual/UX review; Assessment B source detector review)

# Impeccable critique: pending appointment request detail

Target: `app/src/main/java/com/eyecare/app/presentation/appointments/MyAppointmentScreen.kt`
Reference: supplied `My Appointment` screenshot showing an `AWAITING CLINIC REVIEW` request.

## Overall assessment

**27/40 — solid foundation with meaningful clarity and state-communication gaps.** The screen is calm and readable, and it exposes the request details and the two main actions. The main UX risk is that a specific date and time look booked even though the clinic has not approved them. The page also gives `Change requested time` and destructive `Cancel request` equal full-width emphasis, while offering no concrete next step or response expectation.

## Nielsen heuristic scores

| # | Heuristic | Score | Finding |
|---|---|---:|---|
| 1 | Visibility of system status | 3/4 | `Awaiting clinic review` is visible, but there is no last-updated signal, expected response guidance, or explicit statement that the time is not reserved. |
| 2 | Match between system and real world | 3/4 | Preferred time, date, duration, reason, and clinic review fit the patient’s mental model. The technical request number adds little patient-facing value. |
| 3 | User control and freedom | 3/4 | Change and cancel are available, and cancellation is confirmed with a reason flow. There is no undo or direct clinic-contact path. |
| 4 | Consistency and standards | 3/4 | Cards, buttons, typography, and bottom navigation fit the app. The standalone status treatment is less integrated than the richer request-detail treatment. |
| 5 | Error prevention | 3/4 | Labels and confirmation reduce mistakes, but the UI does not prevent a patient from interpreting the displayed time as confirmed. |
| 6 | Recognition rather than recall | 3/4 | Request number, type, date, time, duration, and reason are together. The outcome after clinic review still has to be inferred. |
| 7 | Flexibility and efficiency | 2/4 | The two common actions are exposed, but follow-up requires deeper navigation and there is no visible message-clinic action. |
| 8 | Aesthetic and minimalist design | 3/4 | The visual language is calm and appropriately sparse. The large blank lower area and detached status pill make the composition feel unfinished. |
| 9 | Error recovery | 2/4 | Cancellation is guarded, but failed changes or stale data have no visible retry or recovery path on this screen. |
| 10 | Help and documentation | 2/4 | The sentence about clinic approval helps, but does not explain whether the time is held, how review works, or what the patient should do next. |

## Design specificity

**Verdict: authored for Eyecare, but still partly generic and inconsistent with the newer request workflow.** Clinic review, preferred times, visit duration, and reason-for-visit content make the screen product-specific. `My Appointment` reads like a confirmed visit while the content is a pending request, which weakens the product’s request-state model. The pending status should use the same integrated treatment as the current appointment/request-detail surfaces.

## Cognitive-load assessment

What works:

- The current state is visible immediately.
- Request details are chunked into one card.
- There are only two task actions.
- Labeled bottom navigation keeps the broader app model recognizable.

Where the load increases:

- The status pill, card, and explanatory sentence repeat the pending state without answering the key question: **is 10:00 AM actually booked?**
- Two equal full-width outlined buttons make the constructive and destructive actions compete.
- There is no visible expectation for what happens after clinic review.
- The request ID is more prominent than useful patient guidance.
- If alternatives exist, the screen should make that relationship explicit rather than relying on the phrase “preferred times.”

## Emotional journey

The opening feels calm and organized. The uncertainty begins when the patient sees a precise date and time alongside a pending state: they can feel reassured that the visit exists, then disappointed if they discover the time was never reserved. Changing the request feels recoverable. Cancelling feels consequential because it is destructive and reason-gated, but there is no clinic-contact option for a patient who is unsure. The page ends in a waiting state without a strong reassurance such as “We’ll update this request here when the clinic responds.”

## What is working

- Clinic-specific wording grounds the workflow in a real care process.
- Essential appointment information is visible without another tap.
- The screen follows the established system: rounded cards, readable labels, cyan actions, and labeled navigation.
- Cancellation has a reason dialog, which supports accountability and reduces accidental cancellation.

## Priority issues

### [P1] The requested time can look booked

**Why it matters:** `Requested time` plus a prominent date/time can be read as a confirmed appointment even though the request is still under review.

**Fix:** Put explicit copy directly below the status or inside the card: **“Not confirmed yet. The clinic has not reserved this time.”** Keep the requested-time label, but make the booking state unmistakable. Use `$impeccable clarify` to refine the copy.

### [P1] Change and cancel have equal visual weight

**Why it matters:** The safe/default path is to wait or adjust the request. Giving cancellation the same full-width treatment makes it too easy to start the destructive flow and weakens action hierarchy.

**Fix:** Make `Change requested time` the filled primary button. Keep `Cancel request` outlined or text-level with destructive color, retaining the confirmation/reason dialog. Use `$impeccable distill`.

### [P1] The waiting state has no visible next-step support

**Why it matters:** A patient waiting for review needs reassurance and, when supported, a way to ask a question. The screen does not explain what the clinic will do next.

**Fix:** Add concise copy such as **“The clinic will review your preferred times and update this request here.”** Add `Message the clinic` only where the messaging capability exists; do not promise a response time unless the backend defines one. Use `$impeccable clarify`.

### [P2] The status is visually detached from the request card

**Why it matters:** The pill, card, and explanation are separate elements that the user must mentally connect.

**Fix:** Move the status into the card header beside the request number, or use one status banner containing both state and explanation. Reuse the same status component across request-detail and current-journey screens. Use `$impeccable layout`.

### [P2] The screen does not communicate freshness

**Why it matters:** Clinic review can happen outside the app, so the patient may wonder whether the screen is stale after returning later.

**Fix:** Keep automatic refresh on resume and show a subtle refreshing state when it occurs. If a timestamp is added, use low-emphasis text such as `Updated just now`; avoid permanent noise when pull-to-refresh already exists. Use `$impeccable harden`.

### [P2] Detail actions can fall below the fold

**Why it matters:** `JourneyContent` uses a vertical scroll and places both actions at the end of the pending content. Longer reason/referral/alternative-time content can push the actions below the first view.

**Fix:** Keep the primary action in a bottom action area that remains reachable, or use a sticky bottom action bar with safe-area padding. Keep cancellation visually secondary. Use `$impeccable layout`.

## Persona red flags

- **Jordan, first-time patient:** May read 10:00 AM as booked and has no direct answer to “What happens next?”
- **Sam, accessibility-dependent patient:** The all-caps small status pill relies on color and separation; the pending state should be announced as one semantic group. Keep the existing text labels and ensure status is exposed to TalkBack.
- **Casey, distracted mobile user:** Equal-weight actions make a destructive tap easier to begin, and there is no freshness cue after interruption or return.

## Minor observations

- `My Appointment` is singular and implies confirmation; `Appointment request` or `Request details` is more precise for this state.
- Keep the request number available for support, but reduce its visual prominence.
- The amber pill should meet contrast requirements in both themes and should not be the only state cue.
- The large lower blank area could be reduced by balancing the card and action area or using a small reassurance block.
- The top `History` action competes slightly with the persistent Appointments tab; keep both only if they lead to clearly different destinations.

## Assessment B: detector and runtime notes

The Impeccable detector completed successfully for the Kotlin target and returned `[]` (no rule findings). Treat that as no source-pattern findings, not a full visual or accessibility validation. Browser inspection was not applicable because this is native Compose; an emulator fallback was unavailable because `adb` was not on PATH. Static screenshot review was used instead.

## Run notes

- No files were edited.
- Impeccable reported `CONTEXT_STALE` for `.impeccable/design.json` being older than `DESIGN.md`; refresh it with the `document` command only if you want the sidecar regenerated.

## Provocative questions

- What if the first sentence answered **“Is this booked?”** before showing the requested time?
- Should the screen recommend waiting as the default action and make cancellation clearly secondary?
- What does a patient need to know if clinic review takes several hours or days?
- Could the pending state be a compact timeline: requested → under clinic review → confirmed?
- If the patient wants to ask a question, why should they leave this screen to discover messaging elsewhere?
