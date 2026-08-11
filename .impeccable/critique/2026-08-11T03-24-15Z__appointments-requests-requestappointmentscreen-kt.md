---
target: book appointment flow screens
total_score: 19
max_score: 40
na_heuristics: 
p0_count: 3
p1_count: 2
timestamp: 2026-08-11T03-24-15Z
slug: appointments-requests-requestappointmentscreen-kt
---
Method: dual-agent (A: design review · B: deterministic evidence)

Target: the Request Appointment wizard — `RequestAppointmentScreen.kt`, `ProfileAndReasonContent.kt`, `RequestReviewContent.kt`, `RequestAppointmentViewModel.kt`, `WizardStepIndicator.kt`, `AppointmentActionButton.kt`. Mode: **Operate** (patient completing a task).

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Step indicator is genuinely good, but a disabled `Continue` never says why (`RequestAppointmentScreen.kt:348`, `:754`), validation failures below the fold produce no visible response, and no state change is announced to TalkBack. |
| 2 | Match System / Real World | 2 | Entry FAB says "Book appointment"; the product is a non-binding request. "The patient cancelled the request." is clinic-record third person shown to the patient. |
| 3 | User Control and Freedom | 1 | Schedule's back arrow exits the whole wizard; no `BackHandler` exists, so system Back destroys an 11-field form with no confirm; `ACTIVE_REQUEST_LIMIT_REACHED` is a hard dead end. |
| 4 | Consistency and Standards | 2 | Step-indicator inset is 16dp on step 1 and 24dp on steps 2–4; CTA is in-scroll on Type but pinned on Review; back semantics differ per step. |
| 5 | Error Prevention | 2 | Good past-date and duplicate-slot guards, race-safe availability. But tapping an alternative silently overwrites the preferred time with no undo (`ViewModel:219-227`). |
| 6 | Recognition Rather Than Recall | 3 | `SelectedPreferences` and the Review card are real strengths; undercut by no per-section "Edit" on Review. |
| 7 | Flexibility and Efficiency | 2 | No "next available," no signal of which days are open before the picker opens, no draft persistence across process death. |
| 8 | Aesthetic and Minimalist Design | 2 | Details dumps 11 fields at once; ~32 tap targets in one slot list; "30min slot" repeats on every row though duration is fixed by the type. |
| 9 | Error Recovery | 2 | Excellent recovery *routing* (`ViewModel:536-574`) undone by `errors = emptyMap()` on any keystroke (`:352`) and no scroll-to-error. |
| 10 | Help and Documentation | 1 | Nothing explains what a request means, how long review takes, or what "Requires referral" requires. |
| **Total** | | **19/40** | **Poor** — driven by three structural defects, not by polish. |

The engineering underneath is better than 19/40 suggests. The score reflects what a patient experiences, and three of the defects are severe enough to break the task.

## Design Specificity Verdict

**LLM assessment.** This is a generic 4-step form wizard wearing a clinic's colors. Strip the palette and it ships unchanged for a DMV or a car service. Nothing in the composition knows it is about eyes, about anxiety, or about a clinic that will say yes or no.

The product's one genuinely distinctive mechanic — a non-binding request carrying a *ranked* preferred time plus two alternatives — never became a design idea. It arrived as a data structure and got rendered as a text button repeated on every row of a long list.

There is exactly one authored component in the flow: `SelectedPreferences` (`RequestAppointmentScreen.kt:376-422`). It keeps the preferred time, the ordered alternatives, the n/2 counter, and the ordering rule visible while the patient scrolls. It is the correct structural answer to the working-memory cost the ranked model creates, and it should become the organizing idea of the Schedule step rather than a panel sitting above it.

**Deterministic scan.** `detect.mjs` returned `[]` with exit code 0 on both target directories — **and that result is meaningless.** `detector/node/file-system.mjs:26-30` defines `SCANNABLE_EXTENSIONS` as HTML/CSS/JS/TS/Vue/Svelte/Astro only. `.kt` is absent, so `walkDir` yielded zero files and the CLI reported zero findings with a success code. Exit 0 from this detector on a Kotlin path carries no signal. Every finding below comes from manual source scanning, and the 41 issues it surfaced are the measure of what the automated pass missed entirely.

Manual scan totals: **41 findings — 15 high, 16 medium, 13 low.** Two categories came back genuinely clean: no hard-coded colors anywhere in the flow (all access goes through `colorScheme` or `EyecareColors.current`), and no unstyled typography roles (`Type.kt:33-45` assigns 13 roles; `displayMedium`/`displaySmall` are the only unassigned ones and the flow never uses them).

**Visual overlays.** Not applicable — native Android target, no browser. No server started, no injection attempted.

## Overall Impression

The state machine is the best-engineered part of this flow and the interaction layer is the worst. `RequestAppointmentViewModel` does things most booking UIs get wrong: generation counters make stale availability responses unable to repaint the current day, and every backward transition round-trips the patient's drafts so backing up never costs typing.

Then the UI layer squanders it. The slot you just selected displays "Add as alternative," and tapping it does nothing. Back on step 2 throws away the wizard. System Back on the Details step destroys eleven hand-typed fields with no dialog. And the screen whose entire job is "confirm this is right before you commit" renders the time as `9:00 AM â€“ 9:30 AM`.

The single biggest opportunity: **the flow never tells the patient it is a request.** Fix the framing and the ranked-alternatives model stops being homework and becomes the reason to trust this app — you tell the clinic when you can come, and a human gets back to you.

## What's Working

**1. Submission-error recovery routing.** `ViewModel:536-574` routes `SLOT_UNAVAILABLE` back to Schedule with reason, referral, and identity preserved; referral validation failures back to Details *with the field error pre-attached*; type-unavailable back to Type with an explanatory notice. Combined with draft round-tripping in `backToSchedule` (`:301-318`), this is genuine care. Protect it in any redesign.

**2. Race-safe availability loading.** `availabilityGeneration` + `typeCatalogGeneration` + explicit job cancellation (`:127-129`, `:179-217`) plus the `appointmentTypeId` cross-check at `:194` mean a slow response for an abandoned date cannot repaint the current day's slots.

**3. `SelectedPreferences` and the Morning/Afternoon split.** The one authored component in the flow, plus honest chunking of the slot list (`RequestAppointmentScreen.kt:272-279`) and clear section labels on Details. Chunking is the one cognitive-load item that passes cleanly.

## Priority Issues

### [P0] The flow never says it's a request until the request is already sent

**What.** The entry FAB reads "Book appointment" (`AppointmentListScreen.kt:203`). Type says "Select appointment type" (`:716`). Schedule says "Choose your preferred time" (`:214`) — implies choice, not approval. Review says "Submit request" (`RequestReviewContent.kt:197`). The first and only statement that the clinic must approve and **no time is reserved** appears at `RequestAppointmentScreen.kt:604` — after submission. The right sentence already exists in the codebase at `AppointmentRequestPresentation.kt:53`: *"No time is reserved until confirmed."*

**Why it matters.** A patient with blurred vision taps "Book appointment," picks 9:00 AM Tuesday, sees a green checkmark, and believes they have an appointment Tuesday at 9. They may take time off work. They find out otherwise only if they open the request detail screen.

**Fix.** Rename the FAB to "Request appointment." Put a tonal banner (`primaryContainer`, Body role, `accentText` icon) as the first element of the Type step: *"The clinic reviews every request. Nothing is held until they confirm."* Repeat a one-line version in the Review bottom bar directly above the CTA and change the CTA to "Send request to clinic." Fix `AppointmentRequestPresentation.kt:68` from "The patient cancelled the request." to second person.

**Suggested command:** `/impeccable clarify`

### [P0] Back is broken three ways, and one of them destroys the form

**What.** Three separate defects:
- Schedule's back arrow calls `onBack` = `popBackStack()` (`RequestAppointmentScreen.kt:112`), exiting the entire wizard from step 2 of 4. The correct function `backToType()` (`ViewModel:286-299`) is **never called by any screen** — dead code. It is also broken if wired up: `:290-292` is an expression that can only return `emptyList()`, and `:296` restores `selectedType` only for `loadTypes()` at `:298` to overwrite it at `:137`.
- **No `BackHandler` anywhere in the appointments package.** On Details and Review the arrow steps back one stage while system Back pops the whole destination and kills the ViewModel — eleven fields including home address and date of birth, gone, no dialog. `EditProfileScreen.kt:82` already has the correct pattern.
- `SubmissionError` binds `onRetry` and `onBack` to the *same* call (`:141-143`). On `ACTIVE_REQUEST_LIMIT_REACHED`, `handleSubmissionError` copies the state onto itself (`ViewModel:557-560`), so the message swaps once and then both buttons are permanent no-ops. The only escape is system Back, which discards everything.

Related: `RequestAppointmentViewModel` takes no `SavedStateHandle` (`:120-122`), so process death restarts at step 1 with everything lost. No `rememberSaveable` in the package either — an open date picker vanishes on rotate.

**Why it matters.** DESIGN.md:234 and :291 both require system Back to be authoritative. Right now it is authoritative in the worst way: it is the one control guaranteed to destroy the patient's work.

**Fix.** One `BackHandler` in `RequestAppointmentScreen` dispatching on `step` exactly as the arrows do. Wire Schedule's arrow to `backToType()` and repair `:290-292` and the `loadTypes()` overwrite. Gate exit from Details and Review behind a "Discard this request?" confirmation. Give `SubmissionError` a real second action. Add `SavedStateHandle` persistence for the draft.

**Suggested command:** `/impeccable harden`

### [P0] The slot you just selected says "Add as alternative," and tapping it does nothing

**What.** `SlotCard`'s trailing `when` (`RequestAppointmentScreen.kt:539-547`) tests `canAddAlternative` before `isSelected`. That flag is true for the currently-selected preferred slot — it is never in `alternativeSlots`, so `alternativeOrder == null`. The selected slot therefore renders **"Add as alternative"** instead of its confirmation indicator, and tapping it is silently rejected by `ViewModel:232`.

Three more defects compound it:
- `Modifier.clickable` sits on the inner `Column` (`:513-516`), not the `Surface`, so the row's right portion and all 28dp of card padding are dead to selection. The clickable's composed height is text-only — roughly **34dp unselected**, below the 48dp floor, in exactly the state the patient taps.
- No `Role.RadioButton`, no `selected` state, no `stateDescription` anywhere in the flow. TalkBack announces an unlabeled button and never says which time is chosen. Same defect on the type cards (`:727`).
- Preferred, Alternative 1, and Alternative 2 render as identical cyan circles with the same icon (`:543-546`), distinguished only by `contentDescription`.

**Why it matters.** The thumb lands on the right half of a row and selects nothing. Then the slot they did select invites them to add it as an alternative to itself and ignores the tap. This is the "the app is broken" moment, at the step where they are trying to get care.

**Fix.** Reorder the `when` so `isSelected` and `alternativeOrder` match first. Make the whole `Surface` one `Modifier.selectable(selected = isSelected, role = Role.RadioButton)` target. Replace the per-row text button with a two-phase interaction: pick the preferred time, then the `SelectedPreferences` panel offers "Add backup times" and the list switches to multi-select. Give alternatives a numbered badge so the ranking is visible in the list, not only in the summary.

**Suggested command:** `/impeccable shape`

### [P1] Details is an 11-field wall under a keyboard, and Schedule can become uncompletable

**What.** Two layout defects, both verified against `MainActivity.kt:45` (`enableEdgeToEdge()`) and an `AndroidManifest.xml` with **no `windowSoftInputMode`** — under edge-to-edge the system does not auto-resize for the IME, so `imePadding()` is mandatory.

- **Details** (`ProfileAndReasonContent.kt:104-260`) renders ten `OutlinedTextField`s with **no `imePadding()` anywhere in the appointments package**. `AuthStepScaffold.kt:95` and `ChatScreen.kt:139` both do this correctly — this flow is the outlier, and DESIGN.md:232 names it explicitly. The `verticalScroll` present scrolls a viewport the keyboard is covering, not one it resized. The reason field is pinned to `height(120.dp)` (`:118`) and clips at increased font scale.
- **Schedule** (`RequestAppointmentScreen.kt:206-350`) has a `fillMaxSize()` outer Column with **no `verticalScroll`**. The Continue button at `:345` is declared after the `weight(1f)` child. At large font scale or on a short screen the fixed children consume the height, the weight child collapses, and the button is pushed off-screen with no way to scroll to it. **The step becomes uncompletable.**

Validation compounds it: `confirmDetails` (`ViewModel:356`) runs only on tap with no scroll-to-error and no focus request, so `Continue` appears to do nothing. Then `updateIdentity` sets `errors = emptyMap()` on every keystroke (`:352`), erasing errors for fields the patient never touched — they fix one field, watch every message vanish, believe they are done, and tap again.

Review has its own inset bug: `Box(...).padding(padding)` at `RequestReviewContent.kt:67` already applies the bottom system-bar inset, and the bottom Surface adds `.navigationBarsPadding()` at `:194` — counted twice — while the scroll content reserves only `bottom = 92.dp` (`:79`) against a bar that occupies ~124dp on a gesture-nav device.

**Why it matters.** This is where an unlinked patient — the newest and most anxious user in the system — either completes or abandons. The failure is invisible and the recovery is actively misleading.

**Fix.** Add `imePadding()` to Details and make Schedule's outer Column scrollable. Clear only the edited field's error. Add `bringIntoViewRequester` on the first invalid field. Replace `height(120.dp)` with `minLines = 3`. Remove the double inset on Review and compute the reserve from the actual bar height. Split identity into its own step — five honest steps beat one unreadable one.

**Suggested command:** `/impeccable adapt`

### [P1] Contrast violations in a shared component, and corrupted text on the confirmation screen

**What.**
- `RequestAppointmentScreen.kt:721` — `color = MaterialTheme.colorScheme.primary` on the "That appointment type is no longer available" notice. Lens Cyan `#29B6F6` on white ≈ **2.1:1**. DESIGN.md:342 forbids exactly this, and it is the most important message on the screen.
- `AppointmentActionButton.kt:75` — `ButtonDefaults.outlinedButtonColors()` resolves `contentColor` to `primary`, so cyan-on-white propagates to **every caller** without `primary` appearing anywhere. Same default hits eight `TextButton`s including "Add as alternative" (`:540`), "Remove" (`:447`), and the Gender selector, where the patient's own selected value renders at 2.1:1 (`ProfileAndReasonContent.kt:330`).
- Selection state is a 1.5dp `primary` border plus an 8%-alpha fill (`:498-504`, `:729-730`) — both under the 3:1 non-text floor. With the missing semantics above, selection is effectively invisible to a low-vision or TalkBack user, violating PRODUCT.md's "state communication that does not rely on color alone."
- `RequestReviewContent.kt:328` — the preferred-time string emits `â€“` instead of an en dash. Byte-verified: `C3 A2 E2 82 AC E2 80 9C`, a UTF-8 en dash decoded as CP1252 and re-encoded. Line 339 in the same file has the correct bytes, so this is corruption, not intent. It is the only occurrence in the entire codebase, and it renders on the Review screen's "Preferred time" row and every alternative row.

**Why it matters.** Garbled text on a health confirmation screen reads as *this app is broken, don't trust it with my eyes* — at the exact moment the patient is deciding whether to commit.

**Fix.** `:721` → `EyecareColors.current.accentText`. Give `AppointmentOutlinedButton` an explicit `contentColor = accentText`. Add a non-color selection cue (leading check glyph plus a text label) to both card types. Repair `:328`.

**Suggested command:** `/impeccable polish`

## Cognitive Load: 6 of 8 fail (high — critical)

| Item | Verdict | Evidence |
|---|---|---|
| Single focus | FAIL | Reason + referral + 9 identity fields under one `Continue`. |
| Chunking | **PASS** | Morning/Afternoon split; Contact/Personal/About you/Address labels. |
| Grouping | FAIL | The "Select date" `TextButton` (`ProfileAndReasonContent.kt:218`) floats between the DOB field and Gender — it visually binds to the wrong control. |
| Visual hierarchy | FAIL | Unselected type cards are white-on-canvas with no border and no elevation (`:726-731`) — options don't read as options. |
| One thing at a time | FAIL | Date + preferred time + two ranked alternatives at once, two competing targets per row. |
| ≤4 options per decision | FAIL | A 9–5 day at 30-min slots = 16 cards × 2 targets = ~32 targets in one scroll. |
| Working memory | FAIL | "Alternatives are submitted in the order you add them" must be held in the head; Review has no edit affordance. |
| Progressive disclosure | FAIL | Identity block appears wholesale; the 1000-char counter shows from character zero. |

## Emotional Journey

**Valley 1 — the date lottery.** `SelectableDates` only blocks the past (`:180-185`). A patient with a symptom picks Saturday → "The clinic is closed on this date," rendered with an **Inbox icon** (`ContentStates.kt:78` — a mail-tray metaphor for a closed clinic). Picks again → closed. No signal of which days are open before the dialog opens. The availability endpoint already returns `dayStatus`.

**Valley 2 — the wall.** Eleven fields, invisible validation failure, then self-erasing errors. The worst loop in the flow.

**Valley 3 — the dead end.** `ACTIVE_REQUEST_LIMIT_REACHED` strands a patient who just typed their address, DOB, and why their vision is blurred.

**Peak — absent, and currently a bug.** The intended peak is Review + Submit. That screen renders mojibake, has no reassurance line above the CTA, and then throws the whole card away for a bare spinner at the exact second of commitment — despite `AppointmentPrimaryButton` already supporting in-button `loading` (`AppointmentActionButton.kt:33`), which is what DESIGN.md:263 asks for.

**End — flat.** `RequestSuccessContent` is correct but thin: no expected turnaround, no restatement that no slot is held, one exit. Peak-end rule: the peak is a bug and the end is a shrug.

## Persona Red Flags

**Jordan (first-timer):** Taps a FAB labeled "Book appointment" and never learns the clinic must approve until after submitting. Taps the "Date of birth" field — `readOnly` with no click handler (`ProfileAndReasonContent.kt:209-217`) — nothing happens; the real trigger is a caption-looking `TextButton` below it. Picks a time, sees "Add as alternative" on that exact slot, taps it, nothing happens. Faces a greyed-out `Continue` with no explanation of what's missing.

**Sam (accessibility-dependent):** No `Role`, no `selected` semantics on any slot or type card — TalkBack never announces which time is chosen, and state is carried only by sub-3:1 color. No live-region announcement on any step or submission transition; `RequestSubmittingContent`'s spinner has no label. Preferred vs Alt 1 vs Alt 2 are visually identical. No `imePadding()` on Details. Fixed `height(120.dp)` and hard-coded `bottom = 92.dp` both break at large font scale.

**Casey (distracted, one-handed):** CTA is inside the scroll on Type but pinned on Review — has to hunt for Continue on one step and not the other. Every slot row has two hit targets with an invisible boundary; a thumb tap on the right half fires the wrong action. Tapping a slot already saved as an alternative silently promotes it to preferred and drops the previous one, with no confirm, no undo, no snackbar. A back-swipe mid-form discards everything.

**Riley (stress-tester):** Back semantics differ per step and the correct function is dead code with a no-op expression inside it. Hard dead end on `ACTIVE_REQUEST_LIMIT_REACHED` where both buttons call the same function. Errors self-destruct on one keystroke. Mojibake on every Review render. Change the date after selecting a slot and `SelectedPreferences` still asserts a preferred time the visible list shows no selection for — the summary and the list contradict each other. Process death loses all eleven fields.

## Minor Observations

- Step-indicator inset jumps 8dp between step 1 (16dp) and steps 2–4 (24dp) — a persistent element that visibly shifts on every transition.
- `wizardSteps` is declared once (`:77`) then re-typed inline three times; `SectionLabel` is duplicated verbatim in two files, both with an off-token `letterSpacing = 0.8.sp`.
- `"${duration}min slot"` repeats on all 16 rows though duration is fixed by the type chosen on the previous step.
- `ErrorContent` renders its message body in `colorScheme.error` (`ContentStates.kt:54`) — 4.13:1, under the 4.5:1 body floor, already flagged in DESIGN.md.
- Review nests a 16dp Surface in a 20dp Card — only 4dp tighter, weak against the Nested Radius Rule which pairs 16dp with 10–12dp.
- `isFrameReservationOrigin` is plumbed from `NavGraph.kt:326`, bound at `:330`, and never read — the flag is dead end-to-end and `RequestStep.Success.isFrameReservationOrigin` is permanently false.
- Off-scale spacing: `spacedBy(10.dp)` (two sites), `vertical = 14.dp`, `height(6.dp)`, `vertical = 5.dp`, and 1.5dp borders against DESIGN.md's one-dp convention.
- Unused imports in `RequestAppointmentScreen.kt`: `Card`, `CardDefaults`, `width`, `ZonedDateTime`, `AppointmentRequestGender`.

**Two DESIGN.md drifts, both doc-side:** line 264 ("Primary: Lens Cyan with Clear White content") is overruled by line 167 (`on-primary` = Charcoal, because white is 2.3:1) — the implementation follows 167 correctly, but the doc will mislead the next screen. And line 264's "primary-colored content" for outlined buttons directly contradicts line 342's ban; `AppointmentActionButton.kt:75` complies with one and violates the other. Separately, DESIGN.md claims twelve type roles; `Type.kt` styles thirteen (`bodySmall` is real and used 9 times in this flow).

**Not a defect:** `Theme.kt:117-120` pins `darkTheme = false` with a comment explaining dark theme is fully authored but intentionally unwired. DESIGN.md:197/341 assert it switches on `isSystemInDarkTheme()` and calls it first-class — the doc is ahead of the code by choice. Worth reconciling, but the flow's colors are all token-based and would survive the switch.

## Questions to Consider

1. **What if Schedule asked one thing — "when would you like to be seen?" — and treated alternatives as reassurance rather than a task?** After the preferred time is picked, a single card: "Add two backup times and the clinic can confirm faster." That turns a 32-target grid into one decision plus one optional decision, and finally makes the ranked model feel like a feature instead of homework.
2. **Why does the patient pick a date before knowing which dates are open?** The endpoint already returns `dayStatus`. Paint open days in the picker and open Schedule on the next available day, and Valley 1 disappears entirely.
3. **Should "Submitting" be a screen at all?** What if the Review card stayed, dimmed, with progress inside the button — and the success state transformed that same card into the pending request, so the peak and the end are the same object?
4. **If the request is non-binding, why does the flow look like a checkout?** A confirmation-shaped wizard sets a booking-shaped expectation no matter what the copy says. What would this look like as a *message to the clinic* — here's what I need, here's when I can come, here's what's wrong with my eye?
5. **Who is the Details step actually for?** Nine identity fields exist so staff can match a request to a record. That is the clinic's need, charged to an anxious patient at peak friction. Could reason-for-visit ship first, with identity collected after submission?
