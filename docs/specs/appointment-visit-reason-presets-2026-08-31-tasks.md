# Tasks: Appointment Visit-Reason Presets

**Status:** Complete — 2026-08-31 — all implementation tasks and applicable gates passed
**Date:** 2026-08-31
**Spec:** `docs/specs/appointment-visit-reason-presets-2026-08-31-spec.md`
**Technical plan:** `docs/specs/appointment-visit-reason-presets-2026-08-31-plan.md`
**Implementation:** Tasks 1–5 shipped in atomic commits; Task 6 closed with the verification record below.
**Verification record:** `ktlintFormat`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and
`assembleDebugAndroidTest` passed. Connected instrumentation was not run because `adb` is not
available on PATH in this environment. The manual device matrix was not executed for the same reason;
unit and Compose-test coverage remains in the repository.

## Overview

This breakdown delivers the approved visit-reason preset behavior in six dependency-ordered tasks.
Each implementation task is limited to five or fewer likely files, has testable acceptance criteria,
and leaves the Android project buildable. Production behavior was delivered with RED → GREEN →
REFACTOR during Phase 4.

Before running Gradle commands in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Architecture Decisions

- Presets belong to their `AppointmentType` and cross the DTO-to-domain boundary once.
- A missing preset array is treated as empty for safe staged rollout.
- Presentation state distinguishes no choice, preset ID, and **Other** without using labels as IDs.
- One shared input holds optional preset details or custom reason text.
- One pure function composes and validates the final `reason_for_visit` string.
- Only primitive choice metadata enters `RequestDraft`; no DTO/domain object is saved.
- Review and request creation receive only the final string, never a preset ID.
- Missing presets and changed appointment types preserve patient text as custom input.

## Task List

### Phase 1: Catalog foundation

## Task 1: Decode preset-bearing appointment types

**Description:** Extend the appointment-type transport and domain models with visit-reason presets,
starting with decoding and domain-model tests. Missing and empty arrays must preserve the current
text-only behavior.

**Acceptance criteria:**

- [x] Kotlinx Serialization decodes preset `id` and `label` from `visit_reason_presets`.
- [x] Populated arrays preserve server order; empty and missing arrays become `emptyList()`.
- [x] `AppointmentType` exposes domain presets without serialization annotations or data-layer types.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*AppointmentRequestDtosTest" --tests "*AppointmentTypeCatalogTest"`
- [x] `./gradlew assembleDebug`

**Dependencies:** None

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/remote/dto/AppointmentRequestDtos.kt`
- `app/src/main/java/com/eyecare/app/domain/model/AppointmentType.kt`
- `app/src/test/java/com/eyecare/app/data/remote/dto/AppointmentRequestDtosTest.kt`
- `app/src/test/java/com/eyecare/app/domain/model/AppointmentTypeCatalogTest.kt`

**Estimated scope:** Medium (4 files)

---

## Task 2: Map presets through the repository boundary

**Description:** Map each appointment type's preset collection from DTOs to domain models and prove
the existing create-request body remains free of preset fields.

**Acceptance criteria:**

- [x] Repository mapping preserves every preset ID, label, owning type, and backend order.
- [x] An appointment type with no presets maps to an empty domain collection.
- [x] Exact request-body tests prove creation still sends only `reason_for_visit` and contains no
      preset identifier or separate detail field.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*AppointmentRequestRepositoryImplTest"`
- [x] Inspect MockWebServer assertions for `reason_for_visit` and absence of `preset` keys.
- [x] `./gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImpl.kt`
- `app/src/test/java/com/eyecare/app/data/repository/AppointmentRequestRepositoryImplTest.kt`

**Estimated scope:** Small (2 files)

### Checkpoint: Catalog boundary

- [x] Updated and legacy-shaped appointment-type responses decode.
- [x] DTOs map to domain models only at the repository boundary.
- [x] Backend ordering is unchanged.
- [x] Request creation has no preset field.
- [x] Debug build succeeds.

### Phase 2: Reason workflow

## Task 3: Define visit-reason choice and composition

**Description:** Introduce a presentation-level choice model and a pure composer that produces the
exact final free-text reason used by validation, Review, restoration, and submission.

**Acceptance criteria:**

- [x] Choice state explicitly represents none, one preset ID, or **Other**.
- [x] Composition returns the preset label, `Preset label: details`, or exact custom text according
      to the approved rules and trims only surrounding whitespace.
- [x] Tests cover empty input, unresolved IDs, duplicate labels, and final lengths of 1000 and 1001
      characters without parsing labels as identity.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*VisitReasonSelectionTest"`
- [x] `./gradlew assembleDebug`

**Dependencies:** Task 1

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/VisitReasonSelection.kt` (new)
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/VisitReasonSelectionTest.kt` (new)

**Estimated scope:** Small (2 files)

---

## Task 4: Integrate choices with request state and restoration

**Description:** Extend the request draft and ViewModel so selection, shared text, validation,
navigation, restoration, type changes, and submission all use the centralized composer without
losing patient input.

**Acceptance criteria:**

- [x] Preset selection, **Other**, and no-preset input validate correctly and produce the exact Review
      and repository reason string.
- [x] Back/forward and process-draft restoration preserve choice, text, and referral input; an old
      draft with text but no new metadata restores as custom input.
- [x] A missing preset or appointment-type change clears stale preset identity and preserves a
      meaningful custom reason; invalid and failed submissions keep the complete draft.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest" --tests "*VisitReasonSelectionTest"`
- [x] Tests cover preset-only, details, **Other**, empty arrays, referral coexistence, Back/forward,
      restored/deactivated presets, type changes, 1000-character validation, and single submission.
- [x] `./gradlew assembleDebug`

**Dependencies:** Tasks 2 and 3

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestDraft.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModel.kt`
- `app/src/test/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentViewModelTest.kt`

**Estimated scope:** Medium (3 files)

### Checkpoint: Workflow behavior

- [x] Every choice mode produces the approved final string.
- [x] Validation measures the exact final value and preserves invalid input.
- [x] Restoration and type/catalog changes cannot lose patient text or retain a stale preset ID.
- [x] Referral and identity behavior remains unchanged.
- [x] Focused unit tests and the debug build succeed.

### Phase 3: Patient-facing Reason step

## Task 5: Render and verify visit-reason choices

**Description:** Add the **Common reasons** chip group to the existing Reason step, wire callbacks to
the tested ViewModel state, adapt the multiline field and length feedback, and prove Review displays
the final outbound text.

**Acceptance criteria:**

- [x] Presets render in server order as wrapping, single-select chips followed by **Other**; selected
      semantics are exposed for accessibility.
- [x] Presets show optional-details input, **Other** shows required custom input, and empty arrays
      retain the current direct text field with correctly attached inline errors.
- [x] Narrow layouts, large text, scrolling, referral input, character feedback, and Review remain
      usable and show the exact composed reason.

**Verification:**

- [x] `./gradlew testDebugUnitTest --tests "*RequestAppointmentViewModelTest"`
- [x] `./gradlew assembleDebugAndroidTest`
- [x] `./gradlew assembleDebug`
- [x] Connected instrumentation availability checked; `connectedDebugAndroidTest` was not run because
      `adb` is unavailable on PATH.
- [x] Manual device matrix recorded as not run because no emulator/device or `adb` is available;
      preset-only, preset plus details, **Other**, no presets, referral, Back/forward,
      long label, and large-font paths.

**Dependencies:** Task 4

**Files likely touched:**

- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestReasonContent.kt`
- `app/src/main/java/com/eyecare/app/presentation/appointments/requests/RequestAppointmentScreen.kt`
- `app/src/androidTest/java/com/eyecare/app/presentation/appointments/requests/RequestReasonContentTest.kt` (new)
- `app/src/androidTest/java/com/eyecare/app/presentation/appointments/requests/RequestReviewContentTest.kt`

**Estimated scope:** Medium (4 files)

### Checkpoint: End-to-end patient flow

- [x] Type → Schedule → Reason → optional Identity → Review order is unchanged.
- [x] All preset, **Other**, empty-list, and referral combinations render and validate correctly.
- [x] Review and submission use the same final reason.
- [x] Compose tests compile; execution is deferred because no device/emulator is available.
- [x] Debug build succeeds.

### Phase 4: Final verification and reconciliation

## Task 6: Run complete gates and close the workflow artifacts

**Description:** Run the full quality gates, verify the final diff preserves unrelated user work,
record the delivered behavior in the living context, and mark workflow artifacts complete only after
all applicable checks pass.

**Acceptance criteria:**

- [x] `CONTEXT.md` accurately describes backend-managed type-specific presets, Android's **Other**
      option, final free-text submission, and empty-list fallback.
- [x] Spec, plan, and tasks statuses reflect actual implementation and verification; no checkbox is
      marked complete before its evidence passes.
- [x] The final diff contains no backend implementation, dependency, persistence, sensitive logging,
      or unrelated formatting/work.

**Verification:**

- [x] `./gradlew ktlintFormat`
- [x] `./gradlew testDebugUnitTest`
- [x] `./gradlew lintDebug`
- [x] `./gradlew assembleDebug`
- [x] `./gradlew assembleDebugAndroidTest`
- [x] `git diff --check`
- [x] Report `connectedDebugAndroidTest` as passed only if a device/emulator was available and the
      command actually ran.
- [x] Manual matrix recorded as not run (no device/emulator or `adb` available): preset-only; preset
      plus details; **Other**; no presets; referral; linked and unlinked requests; Back/forward; type
      change; restored/deactivated preset; 1000-character edge.

**Dependencies:** Task 5

**Files likely touched:**

- `CONTEXT.md`
- `docs/specs/appointment-visit-reason-presets-2026-08-31-spec.md`
- `docs/specs/appointment-visit-reason-presets-2026-08-31-plan.md`
- `docs/specs/appointment-visit-reason-presets-2026-08-31-tasks.md`

**Estimated scope:** Medium (4 files)

### Checkpoint: Complete

- [x] Every approved success criterion is implemented.
- [x] Focused and full unit tests pass.
- [x] Formatting, lint, debug build, and Android-test compilation pass.
- [x] Device/emulator test availability is reported accurately.
- [x] Workflow artifacts and context match the delivered source tree.
- [x] The feature is ready for code review.

## Parallelization Opportunities

- After Task 1, Task 2 repository mapping and Task 3 pure composition can be implemented in parallel
  because their production and test files do not overlap.
- Task 4 must wait for both Tasks 2 and 3.
- Task 5 is sequential after Task 4 because it consumes the final ViewModel state and shares request
  presentation files.
- Task 6 is always last.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Missing response key breaks type loading | High | Empty-list DTO default and legacy fixture in Task 1 |
| Label is mistaken for stable identity | High | ID-based selection and duplicate-label test in Task 3 |
| Review differs from submitted reason | High | Shared pure composer and integrated ViewModel assertions in Task 4 |
| Preset disappears during restoration | High | Convert prior composed value to custom input in Task 4 |
| Label plus details exceeds 1000 | Medium | Validate composed length at 1000/1001 boundaries in Tasks 3–4 |
| Long labels or controls harm accessibility | Medium | Wrapping/semantics/large-font coverage in Task 5 |
| New UI disrupts referral behavior | Medium | Combined state and Compose coverage in Tasks 4–5 |

## Open Questions

None. The task breakdown was approved by the human on 2026-08-31 and all implementation phases are
complete.

## Verification of Task-Breakdown Gate

- [x] Every task has explicit acceptance criteria.
- [x] Every task has executable verification or a precise manual check.
- [x] Dependencies are identified and ordered.
- [x] No task lists more than five likely files.
- [x] Checkpoints occur after each major phase.
- [x] Human has reviewed and approved the task breakdown — 2026-08-31.
