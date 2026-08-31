# Plan: Appointment Visit-Reason Presets

**Status:** Complete — 2026-08-31
**Date:** 2026-08-31
**Spec:** `docs/specs/appointment-visit-reason-presets-2026-08-31-spec.md`

> Phase: **Complete**. Verification passed: `ktlintFormat`, `testDebugUnitTest`, `lintDebug`,
> `assembleDebug`, and `assembleDebugAndroidTest`. Connected instrumentation was not run because
> `adb` is unavailable in this environment.

## Overview

Extend the existing appointment-type catalog through the data and domain layers, then add an
explicit visit-reason selection model to the current request wizard. The Reason step will render
server-managed single-select presets plus Android's **Other** choice, compose one final
`reason_for_visit` string, and preserve custom input across navigation and catalog reconciliation.

This is an Android-only contract-alignment change. The appointment-request endpoint, wizard order,
referral logic, identity logic, dependencies, and backend storage remain unchanged.

## Architecture Decisions

1. **Presets are part of `AppointmentType`.** Wire items decode beside the existing appointment-type
   DTO, map to a small domain model at the repository boundary, and remain associated with their
   owning type.
2. **Missing arrays degrade safely.** The DTO defaults `visit_reason_presets` to an empty list so an
   older or partially deployed response keeps the current text-only experience instead of failing
   catalog decoding.
3. **Choice is explicit presentation state.** The ViewModel distinguishes no choice, a preset ID,
   and **Other**. Compose never infers selection from displayed label text, and backend labels are
   not used as identity.
4. **One shared patient-text draft.** Preset details, **Other** text, and the no-preset free-text
   input use one patient-entered string. Switching choices preserves that string; the active choice
   determines how it contributes to the final value.
5. **Composition is pure and centralized.** One deterministic function resolves the selected preset
   inside the current appointment type, trims the input, creates `Preset label: details` when
   needed, and returns validation information for the 1000-character limit.
6. **The outbound contract stays free text.** Identity and preset-choice state end at the ViewModel.
   Repository creation continues receiving only the final `reasonForVisit` string, making it
   impossible for Retrofit to serialize a preset ID accidentally.
7. **Saved state contains primitives only.** Extend `RequestDraft` with a nullable selected preset ID
   and an **Other** flag while retaining its existing reason text. Old drafts with reason text but no
   new choice metadata restore as custom input.
8. **Catalog reconciliation is fail-safe.** A restored preset ID is accepted only if it exists on the
   selected appointment type. If it is missing, the already composed reason is preserved as custom
   **Other** text.
9. **Type changes cannot retain preset identity.** Before changing types, compose the current reason,
   clear the preset ID, and retain the resulting text as custom input. The new type never receives a
   stale preset association.
10. **Review remains string-based.** Review displays and submits the exact composed reason. Returning
    to Reason rehydrates choice and input from the request draft rather than parsing the displayed
    string.
11. **UI uses existing Material 3 primitives.** A wrapping layout of selectable chips and the current
    multiline field are sufficient; no design-system or dependency expansion is required.

## Components and Dependencies

```text
Updated backend contract
    │
    └── AppointmentTypeDto + VisitReasonPresetDto
            │
            └── Repository DTO-to-domain mapping
                    │
                    └── AppointmentType + VisitReasonPreset
                            │
                            ├── RequestDraft primitive choice state
                            │       │
                            │       └── RequestAppointmentViewModel
                            │               ├── selection/reconciliation
                            │               ├── pure composition/validation
                            │               └── final free-text submission
                            │
                            └── RequestReasonContent
                                    ├── preset and Other chips
                                    ├── conditional input copy/errors
                                    └── accessibility/layout behavior
                                            │
                                            └── Review regression proof
```

The required dependency chain is data contract → domain mapping → ViewModel workflow → Compose.
Presentation work must consume tested domain and state models rather than decode DTOs or duplicate
composition rules.

## Implementation Order

### Stage 1 — Extend the appointment-type catalog

1. Add failing Kotlinx Serialization tests for populated, empty, and missing preset arrays.
2. Add preset DTO/domain types, the appointment-type collection, and repository mapping.
3. Add or extend repository/catalog tests to prove IDs, labels, ownership, and server order survive
   the boundary unchanged.

At this checkpoint, Android can consume the updated catalog without changing the request UI.

### Stage 2 — Build reason selection and composition state

4. Add focused tests for preset-only, preset-plus-details, **Other**, no-preset fallback, whitespace,
   unresolved IDs, and the exact 1000-character boundary.
5. Introduce explicit choice state and the pure final-reason composer.
6. Extend `RequestDraft` and Reason-step state with primitive choice metadata and shared patient text,
   including compatibility behavior for drafts created before this feature.
7. Update ViewModel transitions for selection, input, validation, Back/forward navigation, changing
   appointment types, catalog refresh/restoration, Review, and submission.

At this checkpoint, all new behavior is proven without Compose, and the repository still receives
only a final reason string.

### Stage 3 — Deliver the Reason-step UI

8. Add failing Compose tests for server order, single-selection semantics, **Other**, text-only
   fallback, conditional field labels, inline errors, wrapping, and referral coexistence.
9. Render the **Common reasons** chip group and connect selection callbacks through
   `RequestAppointmentScreen`.
10. Adapt the multiline input and character feedback to the active choice and final composed length.
11. Extend Review tests to prove its displayed reason exactly matches the outbound value.

At this checkpoint, the complete patient flow is usable and accessible while retaining the existing
wizard structure.

### Stage 4 — Regression and completion

12. Run focused data, repository, ViewModel, and Compose tests; correct regressions without broad
    refactoring.
13. Run formatting, the full unit suite, lint, debug assembly, and Android-test compilation.
14. If a device or emulator is available, manually verify preset, **Other**, empty-list, referral,
    Back/forward, and type-change paths; otherwise record that connected tests were not run.
15. Update the spec, plan, and later task record to complete only after every applicable gate passes.

## Verification Checkpoints

### Checkpoint A — Catalog boundary (after Stage 1)

- Populated preset arrays decode successfully.
- Empty and missing arrays become `emptyList()` without losing the appointment type.
- Repository mapping preserves backend order and does not leak DTOs.
- Focused data/repository tests and `assembleDebug` pass.

### Checkpoint B — Workflow behavior (after Stage 2)

- All choice modes compose the specified final value.
- Validation uses the final composed length and preserves invalid input.
- Old/restored drafts and deactivated preset IDs degrade to custom input without data loss.
- Type changes clear preset identity while preserving a meaningful custom reason.
- Request creation receives no preset ID.
- Focused ViewModel tests and `assembleDebug` pass.

### Checkpoint C — Patient UI (after Stage 3)

- Presets appear in server order with Android's final **Other** chip.
- Exactly one choice is selected, and accessibility semantics expose that selection.
- Preset details are optional; **Other** text is required.
- Empty lists render the existing text-only field.
- Referral input, scrolling, narrow screens, and large fonts remain usable.
- Review shows the exact final reason.
- Compose tests compile and focused UI tests pass where executable.

### Checkpoint D — Complete (after Stage 4)

- No appointment-request contract or wizard-order regression exists.
- Formatting, unit tests, lint, `assembleDebug`, and `assembleDebugAndroidTest` pass.
- Connected-test availability and any manual verification are reported accurately.
- The final diff contains only this feature plus the user's pre-existing changes.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Missing new response key breaks the entire appointment-type catalog | High: patients cannot start a request | Default the DTO collection to empty and test a legacy-shaped response |
| Preset label is treated as stable identity | High: wrong choice after clinic edits or duplicate labels | Store and compare the backend integer ID only; use labels solely for display/composition |
| A deactivated preset disappears during restoration | High: patient-entered reason is lost | Preserve the prior composed reason as custom **Other** input |
| Switching appointment types carries an invalid preset | High: reason from one type is presented as belonging to another | Clear preset identity atomically and retain only custom text |
| Preset label plus details exceeds 1000 although details alone do not | Medium: avoidable backend 422 | Count and validate the exact composed value through one pure function |
| Multiple code paths compose different strings | High: Review differs from submission | Centralize composition and assert Review/submission equality |
| Back navigation only retains the final string | Medium: chip selection and editable details cannot be restored | Persist primitive choice/input state and never reverse-parse the final string |
| Long clinic-managed labels overflow chips | Medium: inaccessible or broken layout | Use wrapping layout, multi-line content, narrow-width and large-font Compose tests |
| **Other** is duplicated when no presets exist | Low: confusing extra choice | Keep the current direct text field for empty lists |
| Existing referral errors are displaced by new controls | Medium: patient cannot progress or locate error | Keep referral state independent and cover combined UI/state tests |

## Parallel vs Sequential Work

Must be sequential:

- DTO/domain definitions before repository mapping assertions.
- Repository/domain completion before ViewModel preset state.
- Composition and state transitions before Compose wiring.
- Full verification before completion status updates.

Safe to parallelize after Stage 2 is stable:

- Compose layout/accessibility tests and Review regression tests, provided their file ownership does
  not overlap.
- Documentation/status reconciliation and manual test-matrix preparation.

Most production changes converge on `RequestAppointmentViewModel.kt`, `RequestAppointmentScreen.kt`,
and `RequestReasonContent.kt`, so sequential implementation is preferred unless explicit file
ownership prevents conflicts.

## Scope Guardrails

- Do not change the backend API, database, admin preset management, or request payload shape.
- Do not submit preset IDs, add a preset endpoint, or cache clinic presets separately.
- Do not add multiple selection, search, favorites, recommendation ranking, or medical inference.
- Do not add a wizard step or redesign Type, Schedule, Identity, or Review beyond necessary wiring.
- Do not persist reason text in Room, analytics, logs, or crash metadata.
- Do not add dependencies, alter CI, or refactor unrelated appointment or AR code.
- Do not overwrite the user's backend-document or other existing working-tree changes.

## Open Questions

None. The approved specification fixes the choice, composition, fallback, and contract behavior.

## Plan Review Checklist

- [x] Major components and dependencies are identified.
- [x] Implementation order follows the data-to-presentation dependency chain.
- [x] Risks have concrete mitigations.
- [x] Sequential and parallel work boundaries are explicit.
- [x] Verification checkpoints separate contract, workflow, UI, and full regression gates.
- [x] Human has reviewed and approved this plan — 2026-08-31.
