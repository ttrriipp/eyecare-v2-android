# Plan: Backend Alignment v18 — Commerce Simplification & Simplified Frame Reservations

> Spec: `docs/specs/backend-alignment-v18-2026-08-13-spec.md`
> Tasks: `docs/specs/backend-alignment-v18-2026-08-13-tasks.md` — **authoritative** for task-level
> acceptance criteria, verification commands, and checkpoints.
> Baseline: `23035e8` on branch `backend-alignment-v18`, verified green.
> Date: 2026-08-13

This document holds the *why*: phase rationale, ordering, and risk. It deliberately does not repeat
the 21 tasks.

---

## Shape of the work

Six phases, **ordered by risk rather than by size**.

```
Phase 1  Reservation two-state model (strangler)       ← highest risk, goes first
Phase 2  Reservation DELETE + item editing              │  depends on 1
                                                        │
Phase 3  Quotation removal + My Orders rename          ─┤  independent of 1/2
Phase 4  Payload alignment (revision_number, reason)   ─┤  independent
Phase 5  Frame-ratings feature flag                    ─┘  independent

Phase 6  Route governance + CONTEXT.md + full verify     ← last, depends on all
```

Each phase ends green: `assembleDebug` **and** `testDebugUnitTest`. No phase is "done" on a compile
alone.

---

## Architecture Decisions

### A1 — Risk-first ordering, not size-first
Every genuine unknown lives in the reservation work: 204 response handling, the terminal navigation
state, and the merge rewrite. Quotation removal is the larger diff but is mechanical deletion that
cannot surprise us. The two halves touch disjoint files, so nothing is gained by clearing the big
diff first — and failing fast on the risky half is worth a great deal.

### A2 — Strangler migration for `is_held`
A single breaking type change would leave the tree uncompilable across several tasks. Instead
`isHeld` is added *alongside* `status` (Task 1), consumers migrate one at a time (Tasks 2–3), and
`ReservationStatus` is deleted only once nothing references it (Task 4). Every task ends green, so a
failure is always attributable to the task that caused it.

### A3 — Phase 1 repairs a live production bug
`ReservationDto.status` is a non-null `String` with no default, and the server has stopped sending
`status`. `GET /frame-reservations` therefore throws `MissingFieldException` today — the reservations
list is broken in production right now. Task 1 is a fix, not scaffolding, which is a second reason it
goes first.

### A4 — 204 is modelled, not caught
Both new delete routes can return an empty body. A Retrofit `suspend fun` declared to return a
non-null DTO throws on 204, and the failure only appears at runtime. Every such method is declared
`Response<Unit>` or `Response<T>` and read via `isSuccessful`. On
`DELETE .../items/{item}` the 200-vs-204 split is not an edge case to tolerate but the actual signal
that the last item was removed and the reservation deleted — hence `Result<FrameReservation?>`.

### A5 — Terminal state, not a boolean, for post-deletion navigation
Detail can no longer render a "cancelled" reservation because the record ceases to exist. It must pop
to the list. Modelled as a terminal `ReservationDetailUiState.Deleted` consumed once by a
`LaunchedEffect`; a boolean field would re-fire the pop on every recomposition.

### A6 — Feature flag as a composable parameter
`ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED` rather than a global read inside the
composable. Production passes nothing; tests render both states. Since re-enablement is expected
soon, the hidden path is code that ships shortly and must stay under test the whole time it is
withheld.

---

## Dependency Graph

```
ReservationDto/domain (isHeld)          Quotation vertical
      │                                       │
      ├── ReservationPresentation             ├── navigation (route + intent)
      │        ├── list screen                │        ├── Estimate screens/VMs
      │        └── detail screen              │        └── Quotation data layer
      │                 └── create flow       │
      │                                       └── OpticalOrder ↔ quotation link
      ├── DELETE cancel ──── terminal Deleted state
      │                              │
      └── item add/remove ───────────┴── detail affordances
                                              └── merge rewrite

revision_number removal ── independent
rejection_reason        ── independent
frame-ratings flag      ── independent (shares files with quotation removal)
                                              │
                        route governance + CONTEXT.md ← depends on everything
```

Implementation follows this bottom-up: model → repository → presentation → navigation.

---

## Phase rationale

| Phase | Tasks | Why it sits here |
|---|---|---|
| **0** Baseline | — | A green restore point before touching 36 files of pre-existing WIP. Done: `23035e8`. |
| **1** Reservation two-state | 1–4 | Highest risk *and* a live bug. Strangler order keeps every task green. |
| **2** Reservation DELETE + items | 5–9 | Needs Phase 1's model. Contains the merge rewrite — the highest-value correctness fix here. |
| **3** Quotation removal + rename | 10–13 | Pure deletion, no unknowns. Navigation first so deleting screens can't dangle a route. |
| **4** Payload alignment | 14–17 | Small and mechanical. Sequenced after 3 only to avoid re-editing `OpticalOrderDetailScreen.kt`. |
| **5** Frame-ratings flag | 18–19 | Product decision, not a backend one. Last code phase because it touches files 3 and 4 also edit. |
| **6** Governance + docs | 20–21 | Asserts the end state of every prior phase, so it must run last. |

---

## Risk Register

| # | Risk | Impact | Mitigation | Kind |
|---|---|---|---|---|
| R1 | 204 crashes body-typed Retrofit methods | High | `Response<Unit>`/`Response<T>` + `isSuccessful`; both paths proven by test (Tasks 5–6) | Prevent by construction |
| R2 | Merge regresses to cancel-then-recreate, destroying patient holds | High | `verify(exactly = 0)` on the delete path, with a comment marking it a permanent guard (Task 9) | Permanent test guard |
| R3 | Cancel-success pop re-fires on recomposition | Medium | Terminal `Deleted` state consumed by `LaunchedEffect` (Task 5) | Prevent by construction |
| R4 | Flagged-off rating code rots before re-enablement | Medium | Flag as composable parameter; both states rendered in tests (Tasks 18–19) | Prevent by construction |
| R5 | Breaking type change leaves the tree uncompilable mid-migration | Medium | Strangler ordering (A2) | Prevent by construction |
| R6 | Orphaned `MyEyewear`/`Estimate` strings in `androidTest` | Medium | Grep both `main` and `androidTest` at Checkpoints E and F | Verify once |
| R7 | Allowlist and contract silently diverge | Medium | Task 20 asserts the count explicitly and runs last; the discovery test fails on any reintroduced route | Verify once |
| R8 | Compose verification unavailable | Low | No Robolectric — these are `androidTest`. A device is attached; smoke-run `connectedDebugAndroidTest` before Task 8 depends on it | Verify once |

**On risk kinds.** Treating these uniformly is what makes risk registers useless. R1/R3/R4/R5 stop
existing the moment the signature is right and cost nothing thereafter. R2 is the only one worth a
permanent test — and that assertion guards against a future contributor, not against this migration.
R6/R7/R8 are one-time greps and smoke runs; do not over-engineer them.

---

## Verification checkpoints

Ten checkpoints, one every 2–3 tasks, detailed in the tasks document. Human review is requested at
Checkpoints **B** (Phase 1 complete), **D** (Phase 2 complete), **F** (Phase 3 complete), and **J**
(final).

**Build command** — this environment has no `JAVA_HOME` on the shell path, and a piped Bash
`./gradlew` reports false success:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest --console=plain
```

Instrumented Compose tests run via `connectedDebugAndroidTest` on the attached device; there is no
Robolectric, so they do not run under `testDebugUnitTest`.

---

## Parallelization

- **Safe to parallelize:** Phases 1–2 (reservations), Phase 3 (quotations), and Task 17
  (rejection reason) touch disjoint files and could run in separate sessions.
- **Must be sequential:** Tasks 1→4 (strangler order), 5→6→7→8 (dependency chain), Phase 6 last.
- **Needs coordination:** Phase 5 edits `OpticalOrderDetailScreen.kt`, which Task 13 and Task 15 also
  touch — keep them in one session or rebase carefully.

Execution here is serial: Task 1 → 21.

---

## What this plan does not do

- No Room migration (rating columns stay — spec Assumption 7).
- No My Orders redesign beyond removing the Estimates section and renaming.
- No in-sheet frame picker (spec D4 routes through the catalog).
- No change to visit feedback beyond dropping `revision_number`.
- No client handling for staff-only backend changes (`voided` states, `inbox_archived_at`).
