# Plan: Backend Alignment v16 — Variable-Duration Appointment Requests

Status: Complete — 2026-08-10
Date: 2026-08-10
Spec: `docs/specs/backend-alignment-v16-variable-duration-appointment-requests-spec.md`

## Overview

Migrate the existing account-only appointment-request feature from a single
provisional 30-minute preference to server-configured appointment types,
type-duration availability, one primary plus up to two alternative times, and
conditional referral details. The migration preserves the existing linked versus
unlinked identity boundary and confirmed-appointment flow while removing false
capacity-hold copy and all fabricated client availability.

No backend implementation, dependency, Room schema, navigation-graph structure,
or confirmed-appointment API shape changes are required.

## Architecture Decisions

1. **One canonical request data path.** Appointment-request wire models remain in
   `AppointmentRequestDtos.kt`; request domain types remain in
   `AppointmentRequest.kt`. The unused appointment-type/create-request types in
   `AppointmentV1Dtos.kt` are removed or reconciled so confirmed appointments do
   not own a second request contract.
2. **Legacy-tolerant responses, strict new requests.** Expanded response fields
   are nullable/defaulted because legacy records may lack them. New outbound
   request arguments require a selected type and primary time.
3. **Explicit preference semantics.** Wizard state stores one `primarySlot` and
   an ordered `alternativeSlots` list capped at two. It does not encode the
   primary implicitly as list index zero.
4. **Server-authoritative availability.** Availability calls are keyed by
   `(appointmentTypeId, date)`. Only the latest matching response may update
   state. The client blocks past dates but does not hardcode Sundays, clinic
   hours, cadence, duration, or fallback slots.
5. **Four-step patient flow.** The state machine becomes Type → Schedule →
   Details → Review. Alternative times live inside Schedule to avoid separate
   duplicated wizard steps.
6. **Provider catalog is governance-only.** The backend route is registered and
   callable but Android adds no provider picker or runtime consumer because the
   request body has no provider field and assignment remains clinic-controlled.
7. **Two route counts are tested.** Backend governance contains 55 callable
   registered routes (54 canonical + one legacy alias). Production Retrofit
   discovery must be a subset of the 54 canonical routes and must not call the
   alias.
8. **No hold semantics.** `expires_at` stays mapped but is omitted from UI.
   `time_preferences_are_reserved` is preserved in the model, while all patient
   copy follows the authoritative policy that requests never reserve capacity.
9. **Referral data is type-bound.** Referral source is trimmed and validated at
   1–255 characters only for `requires_referral = true`; switching to a
   non-referral type clears it and sends `null`.
10. **DTO-to-domain mapping remains the boundary.** Retrofit and Kotlinx
    Serialization types never leak into ViewModels or Compose.

## Components and Dependencies

```text
Resolved backend contract documentation
    │
    ├── Route governance (55 registered / 54 canonical)
    │
    └── Request DTOs + legacy-tolerant domain models
            │
            ├── Retrofit query/body changes
            └── Repository mapping and type catalog
                    │
                    ├── Type-selection state
                    │       │
                    │       └── Type-selection UI
                    │
                    ├── Preference-selection state
                    │       │
                    │       └── Schedule/alternative UI
                    │
                    └── Referral/submission state
                            │
                            ├── Details + Review UI
                            └── Expanded list/detail presentation
                                    │
                                    └── Context reconciliation + final verification
```

The high-risk dependency chain is transport/domain → repository → ViewModel
state → Compose. Presentation work must not begin against speculative wire
models.

## Implementation Order

### Stage 1 — Lock the contract and route boundary

1. Reconcile stale examples, retired-feature statements, capacity notes,
   referral limit, and 55-route totals in the two authoritative backend docs.
2. Update Android route governance to distinguish 55 callable registered routes
   from the 54 canonical routes available to new client code.

This stage fails fast on contract ambiguity and prevents later source-scan tests
from rejecting the newly restored type route.

### Stage 2 — Build the request contract foundation

3. Expand appointment-type, availability, create-request, and shared expanded
   resource DTO/domain types. Remove dead duplicate request wire models.
4. Add appointment-type retrieval, required type-specific availability query,
   expanded create arguments, and repository mappings with MockWebServer proof.

At this checkpoint, the data/domain layers can communicate correctly with the
new backend without any patient-visible UI change.

### Stage 3 — Rebuild the wizard state machine

5. Add type loading/selection/retry and invalidate downstream time selections on
   type change.
6. Add primary/alternative preference state, max-two and uniqueness rules,
   keyed latest-response-wins behavior, and stale-slot recovery.
7. Add referral validation, preserve linked/unlinked identity behavior, and send
   the complete request through the updated repository contract.

State transitions and submission arguments are proven before Compose wiring.

### Stage 4 — Deliver the patient flow

8. Add the Type step and update wizard navigation/indicators.
9. Update Schedule for server-only slots, alternatives, dynamic durations,
   closed/empty/error states, and removal of placeholder/Sunday assumptions.
10. Add referral input to Details and show type, duration, ordered preferences,
    referral, and identity in Review/success.

Each UI task consumes already-tested ViewModel state and keeps the app building.

### Stage 5 — Align request history and patient copy

11. Show expanded type/preference data in list/detail where useful, omit
    `expires_at`, and remove every claim that a pending request holds or releases
    capacity.
12. Reconcile `CONTEXT.md`, mark spec/plan/tasks complete only after verification,
    and run the complete quality gates.

## Verification Checkpoints

### Checkpoint A — Contract foundation (after Tasks 1–4)

- Backend docs express the resolved decisions without internal contradictions.
- Route tests report 55 registered callable and 54 canonical routes.
- DTO and repository tests pass with expanded and legacy fixtures.
- `assembleDebug` succeeds.

### Checkpoint B — State machine (after Tasks 5–7)

- Type/date races cannot overwrite newer state.
- Alternatives are ordered, distinct, and capped at two.
- Referral and identity validation produce the exact outbound request.
- Focused appointment-request tests and `assembleDebug` pass.

### Checkpoint C — End-to-end request flow (after Tasks 8–10)

- Type → Schedule → Details → Review works for linked and unlinked accounts.
- Normal and referral types produce valid bodies.
- Empty/error availability never exposes fabricated slots.
- Compose compiles and focused tests pass.

### Checkpoint D — Complete (after Tasks 11–12)

- Request list/detail/success/cancel copy contains no hold semantics.
- Legacy request records render safely.
- Full unit suite, build, lint, and formatting checks pass.
- Manual linked/unlinked normal/referral/stale-slot matrix passes.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Legacy requests omit new fields | High: deserialization or detail crash | Nullable/defaulted response DTOs plus explicit legacy fixtures |
| Type/date responses arrive out of order | High: patient submits a slot for the wrong type | Key jobs and state updates by both type ID and date; cancel older jobs |
| Alternatives duplicate the primary or each other | Medium: backend 422 at Review | Enforce timestamp uniqueness in ViewModel and test all add/remove paths |
| Type switches leave stale referral or slots | High: invalid request/body leakage | Clear dependent fields atomically on type change |
| Empty availability falls back to preview data | High: invalid or misleading booking attempt | Delete production placeholder generation and test empty/error states |
| Existing hardcoded Sunday rule diverges from configured hours | Medium: valid day blocked | Locally reject only past dates; defer closure to server `day_status`/slots |
| Route alias is incorrectly called or incorrectly treated as absent | Medium: governance drift | Separate registered, canonical, and legacy-alias sets in tests |
| `expires_at` is mistaken for a deadline | Medium: false patient promise | Map but omit from UI; show ordered preferences instead |
| Referral value exceeds backend limit | Low: avoidable 422 | Trim and validate 1–255 characters before Review |
| Large request screen becomes harder to maintain | Medium | Extract only cohesive Type/Schedule composables if needed; avoid broad redesign |

## Parallel vs Sequential Work

Must be sequential:

- Contract reconciliation before route/data assertions.
- DTO/domain definitions before repository implementation.
- Repository contract before ViewModel state.
- ViewModel transitions before their Compose consumers.
- Final context/status updates after verification.

Safe to parallelize after Stage 2 is stable:

- Type-step UI and expanded request-history presentation.
- Patient-copy audit and independent route-governance documentation.
- Review UI and Schedule UI only if their shared `RequestAppointmentScreen.kt`
  edits are coordinated or assigned sequentially.

Because most core tasks share the request ViewModel or screen, sequential
execution is preferred unless separate worktrees and explicit file ownership are
used.

## Scope Guardrails

- Do not add a preferred-optometrist picker.
- Do not alter confirmed appointment or reschedule payloads.
- Do not introduce local persistence for request drafts or appointment data.
- Do not redesign the broader Appointments destination or navigation graph.
- Do not expose optical-commerce internal fields.
- Do not repair the backend frame-rating aggregate in Android.
- Do not add dependencies, Room migrations, workers, or compatibility routes.

## Open Questions

None. Phase 2 and the Phase 3 task breakdown were approved on 2026-08-10.
Implementation is intentionally paused until the user explicitly asks to start.

## Plan Review Checklist

- [x] Major components and dependencies are identified.
- [x] High-risk foundations precede presentation work.
- [x] Risks have concrete mitigations.
- [x] Sequential and parallel boundaries are explicit.
- [x] Verification checkpoints occur every 2–3 implementation tasks.
- [x] Human has reviewed and approved the plan — 2026-08-10.
