# Implementation Plan: Backend Alignment V14 — My Eyewear Estimates and Orders

Status: Phase 2 approved — 2026-08-03; Phase 3 tasks awaiting approval

Approved specification:
`docs/specs/backend-alignment-v14-my-eyewear-spec.md`

## Overview

Replace Android's retired aggregate Eyewear, Job Order, and Billing Record
integrations with two independent, contract-current verticals: Estimates backed
by Quotations and Orders backed by Optical Orders. Compose them beneath one
**My Eyewear** destination without combining their paginated data. Then migrate
rating and messaging contexts, switch typed navigation, remove obsolete code,
and make route governance enforce the authoritative 51-route contract.

The migration uses a short compile-time overlap: new contract models and
screens are built alongside the aggregate feature, navigation switches only
after both new details work, and retired layers are deleted only after static
reference checks. Each stage leaves the project buildable and has an explicit
verification checkpoint.

## Current-State Findings

The plan is based on these verified seams:

- Profile already exposes one **Eyewear** row and uses the existing active-link
  navigation gate. Only the patient-facing name and destination implementation
  need replacement.
- `EyewearApiService`, DTOs, repository, domain models, DI module, list/detail
  ViewModels, screens, and tests implement the now-removed `/eyewear` aggregate.
- Eyewear navigation uses one opaque string `key`; the new contract requires
  distinct integer Estimate and Order detail IDs.
- `PatientFeatureIntent` persists the old aggregate list/detail route shape and
  must preserve typed intent restoration across the limited-account link hub.
- `QuotationApiService` does not send `filter`; its DTO/domain model expects a
  nullable revision object and stores money as `Double`. The current contract
  is flat, includes item IDs/types and cross-links, and requires exact string
  money.
- There is no Optical Order service, DTO, domain model, repository, module, or
  focused presentation state. Existing `JobOrder*` files call retired routes
  and omit payment summaries, source links, explicit rateability, and current
  rating summaries.
- `FrameRatingViewModel` depends on `JobOrderRepository`, requires a client
  `productVariantId`, and posts to `/job-order-items/{id}/rating`. The aggregate
  detail currently contains a TODO instead of a completed submission path.
- `FrameRatingDtos` model the old full moderation/revision resource. The new
  endpoint accepts only item ID, rating, and comment and returns a compact
  current upsert result.
- Billing Record service/domain/repository/module/tests have no remaining
  presentation consumer, but their retired endpoints still compile and appear
  in route governance.
- `ChatViewModel` loads old Job Orders plus confirmed Appointments for its
  context picker and submits `order` or `appointment`. The contract permits
  only `quotation` and `optical_order`.
- `MessageDto` requires removed `conversation_id`, lacks `sender_type` and
  `attachments[].download_url`, while the domain repository interface leaks a
  transport `ContextLinkDto`.
- Message context rendering supports Appointment only and silently drops the
  old Order type. Navigation still supplies an appointment callback to Chat.
- `ApprovedApiRoutes` describes a 55-route transitional/deferred state and
  explicitly approves the now-retired Eyewear, Job Order, Billing Record, and
  old rating routes. Its discovery test tolerates production annotations that
  are in the rejected set.
- The authoritative documents now agree on exactly 51 routes: 8 public,
  24 account-only, and 19 active-link.
- The working tree contains user-owned auth UI, backend documentation,
  `.opencode`, and `.claude` changes. Implementation must patch around them and
  never reset, stage, or reformat unrelated files.

## Architecture Decisions

### 1. Build two independent feature verticals

Use separate contracts and repositories:

```text
QuotationApiService                  OpticalOrderApiService
        ↓                                      ↓
QuotationDtos                        OpticalOrderDtos + rating DTOs
        ↓                                      ↓
QuotationRepositoryImpl              OpticalOrderRepositoryImpl
        ↓                                      ↓
QuotationRepository                  OpticalOrderRepository
        ↓                                      ↓
Estimate list/detail                  Order list/detail + rating
```

Do not introduce a new aggregate repository or a union transaction model.
`Quotation` and `OpticalOrder` remain separate domain types. Their only
relationship is a nullable typed reference carrying the other resource's ID
and reference number.

The existing generic `PaginatedResult<T>` remains the pagination boundary.
Both repositories accept `filter` and `page`, use the contract's default
`per_page = 15`, preserve server ordering, and map DTOs before returning.

### 2. Replace Quotation models in place

The current Quotation vertical has no presentation consumer outside the future
My Eyewear and messaging migration, so update it directly rather than create a
second Estimate data layer.

The domain shape becomes flat and exact:

```text
Quotation
├── id / quotationNumber / status
├── validUntil / createdAt / presentedAt / confirmedAt
├── subtotal / discountAmount / total: BigDecimal
├── notes?
├── opticalOrder?: OpticalOrderReference
└── items: QuotationItem
    ├── id / itemType / description / quantity
    └── unitPrice / amount: BigDecimal
```

Remove `QuotationRevision` and all `Double` money. `QuotationStatus.UNKNOWN`
is a real fail-closed value; an unknown response must never become Draft or
Presented. Although drafts are hidden by the server, the decoder may retain a
non-display Draft value defensively only if it does not enter either filter.

### 3. Introduce Optical Order rather than renaming legacy Job Order types

Create a new `OpticalOrder` domain and API vertical matching the patient
contract. Do not adapt `JobOrder` in place: its internal fields, `Double`
amounts, legacy route, and staff-oriented naming make accidental compatibility
too easy.

```text
OpticalOrder
├── id / orderNumber
├── status / fulfillmentMode
├── totalAmount: BigDecimal
├── startedAt? / readyAt? / dispensedAt? / cancelledAt? / createdAt
├── sourceQuotation?: QuotationReference
├── items: OpticalOrderItem
│   ├── id / description / quantity
│   ├── unitPrice / amount: BigDecimal
│   ├── productVariantId?
│   ├── isRateable
│   └── rating?: RatingSummary
└── paymentSummary?: PaymentSummary
    ├── status / isOverdue
    ├── totalAmount / amountPaid / balanceDue: BigDecimal
    └── paymentDueDate?
```

Define `UNKNOWN` for order, fulfillment, and payment enums. Presentation maps
those values to safe labels and never uses ordinal comparisons.

### 4. Keep each list state machine independent and lazy

The My Eyewear route is a coordinator, not an aggregate ViewModel:

```text
MyEyewearScreen
├── local selected primary tab (Estimates initially)
├── EstimateListContent + EstimateListViewModel
│   └── Current/History pagination state
└── OpticalOrderListContent + OpticalOrderListViewModel
    └── Current/History pagination state
```

Instantiate the selected tab's ViewModel inside its Compose branch so initial
entry loads only Estimates Current. Navigation-scoped ViewModel retention keeps
each tab's state when the user switches back. Each ViewModel owns its filter,
page, stale-response sequence, refresh, and append error handling.

Do not put both lists in a single collection, compare their IDs, or trigger one
repository from the other's result. The coordinator owns only the primary tab.
Current is the initial filter within each list.

### 5. Use distinct typed detail routes

Replace aggregate routes with:

```text
MyEyewear
EstimateDetail(quotationId: Int)
OpticalOrderDetail(orderId: Int)
```

Update `PatientFeatureIntent` to preserve these types through the active-link
gate. Cross-links pass the nullable reference ID directly to the corresponding
route. They do not prefetch, parse reference numbers, or fall back from one
resource endpoint to another.

The navigation cutover is atomic: register both new details and messaging
callbacks before removing `EyewearDetail(key)`.

### 6. Treat presentation derivation as pure policy

Keep API/domain enums machine-oriented and place patient language in pure
presentation functions:

- Estimate and Order status labels;
- payment labels and overdue wording;
- first-item-plus-count card titles;
- accurate date labels and fallback order;
- balance visibility;
- Preparation → Ready → Released tracker state;
- optional-section and cross-link visibility.

Estimate and Order details render only their own response. Order detail never
loads a source Estimate to fill sections; Estimate detail never loads an Order
until the patient taps its cross-link.

### 7. Fold rating into the Optical Order vertical

`OpticalOrderApiService` owns:

```text
POST optical-order-items/{id}/rating
```

The repository method accepts only `itemId`, `rating`, and nullable `comment`.
It does not accept or transmit `productVariantId`; the server resolves that
from the item. Replace the old moderation-history DTO with a compact upsert
result and map API errors through the existing structured error decoder.

Order detail exposes the rating dialog only when the item's backend
`isRateable` is true. A successful 201 or 200 updates the item's current rating
in detail state without reloading unrelated lists. A failed validation retains
detail content and the entered rating/comment. The 1–5 and 1,000-character
limits are validated locally for immediate feedback but remain server-owned.

### 8. Make messaging use domain contexts only

Remove transport DTOs from `ChatRepository` signatures. Introduce domain
context references:

```text
MessageContext
├── Quotation(id)
├── OpticalOrder(id)
└── Unsupported(type, id)
```

The data repository alone maps these to/from `ContextLinkDto`. Remove
Appointment and old Job Order context choices because the current API accepts
neither. The picker offers **Link estimate** and **Link eyewear order**, backed
by independent first-page Quotation and Optical Order loads. Picker failures
are scoped per source and must not fail the conversation itself.

Update Message to remove `conversationId`, add an explicit sender type, and
retain attachment `downloadUrl`. Bubble ownership uses `sender_type == patient`
as the contract authority, with `UNKNOWN` failing to staff/neutral styling.
Unknown context types render a generic non-clickable reference rather than
disappearing or being routed incorrectly.

Keep the current single optional file workflow. The multipart field remains
`attachment`; adding a context to the same attachment message is not required
by this migration.

### 9. Enforce route governance only after consumers migrate

Rebuild `ApprovedApiRoutes` around the contract categories:

```text
publicRoutes       = 8
accountOnlyRoutes  = 24
activeLinkRoutes   = 19
allApproved        = 51
```

Move every coordinated breaking-change route to `rejectedRoutes`. Strengthen
the discovery test so a Retrofit annotation in `rejectedRoutes` fails even if
it is otherwise “accounted for.” Normalize new Optical Order and rating path
variables. Keep approved routes without Android consumers in the authoritative
set, while requiring every discovered production route to be approved and not
rejected.

### 10. Delete legacy layers after the cutover proves reachability

After My Eyewear, rating, messaging, navigation, and governance are green,
delete:

- aggregate Eyewear API/DTO/repository/domain/module and superseded list/detail
  state/screens/tests;
- Job Order API/DTO/repository/domain/module and old tests;
- Billing Record API/DTO/repository/domain/module and old tests;
- obsolete Frame Rating transport/domain pieces replaced by the Optical Order
  rating result; and
- old aggregate routes and `PatientFeatureIntent` variants.

Retain reusable Compose presentation only when its types and behavior match the
new spec; prefer narrow extraction over keeping a legacy feature layer alive.
A final `rg` sweep is a deletion gate, not merely documentation.

## Implementation Stages and Dependency Order

### Stage 0 — Baseline and contract characterization

Record the dirty-worktree boundary, run focused current tests/build, and add or
adjust contract fixtures that demonstrate the new Quotation, Optical Order,
Message, and route expectations before production migration. Route tests may
be intentionally red only inside the focused TDD step; the stage ends with the
existing project baseline understood and unrelated failures documented.

Depends on: approved specification.

Checkpoint:

- updated backend documents are treated as read-only inputs;
- affected source/reference inventory is captured;
- no user-owned auth or tooling files are modified;
- baseline `assembleDebug` result is known.

### Stage 1 — Complete the Estimate vertical

Replace Quotation wire/domain/repository contracts, add filter-aware exact
pagination, implement Estimate list/detail state and presentation, and cover
cross-link output without wiring global navigation yet. This stage creates a
complete testable Estimate slice and removes revision-based assumptions.

Depends on: Stage 0.

Checkpoint:

- Quotation fixtures decode exact money and all nullable links/dates;
- Estimate Current/History pagination and stale-response behavior pass;
- Estimate detail is read-only and emits only a typed Order navigation event;
- debug assembly passes with the old aggregate route still reachable.

### Stage 2 — Complete the Order and rating vertical

Add Optical Order wire/domain/repository/DI layers, implement Order list/detail
state and presentation, then migrate and fully wire rating to the new item
endpoint. Keep this slice independent of Estimates except for emitting a typed
source-Estimate navigation event.

Depends on: Stage 0; may begin after shared exact-money/pagination conventions
from early Stage 1 are stable.

Checkpoint:

- all order/payment/fulfillment states and nullable summaries decode safely;
- Current/History pagination remains independent;
- detail tracker and conditional content pass presentation tests;
- rating honors `isRateable`, sends no variant ID, and accepts 201/200;
- debug assembly passes.

### Stage 3 — Cut over My Eyewear navigation

Compose the two list slices beneath My Eyewear, add the typed detail routes and
cross-links, migrate `PatientFeatureIntent`, update Profile copy, and switch
`NavGraph` atomically. Preserve active-link gating and bottom-navigation
visibility policy.

Depends on: Stages 1 and 2.

Checkpoint:

- initial route is Estimates Current and does not call Orders until selected;
- returning to a visited tab retains its independent list/filter state;
- Estimate ↔ Order cross-links use the correct typed IDs;
- limited-account intent restoration reaches the requested typed destination;
- no visible aggregate screen or opaque string key remains;
- unit tests and debug assembly pass.

### Stage 4 — Migrate messaging contracts and contexts

Align Message DTO/domain mapping, remove DTO leakage from `ChatRepository`,
replace Appointment/old Job Order picker sources with Estimates/Optical Orders,
update context cards and navigation, and keep attachment download/upload
behavior contract-compatible.

Depends on: Stages 1–3 repositories and typed detail routes.

Checkpoint:

- messages decode without `conversation_id` and with sender/attachment fields;
- send payloads contain only `quotation` or `optical_order` contexts;
- picker sources fail and paginate independently without failing Chat;
- context taps open the correct typed detail;
- unknown contexts are visible but non-clickable;
- polling, text sending, one-file upload, and download regressions pass.

### Stage 5 — Retire old code and lock route governance

Remove aggregate Eyewear, Job Order, Billing Record, and superseded rating
layers/tests after verifying no references. Rebuild the allowlist at 51 routes,
make rejected production annotations fail, update `CONTEXT.md`, and perform the
full verification matrix.

Depends on: Stages 3 and 4.

Checkpoint:

- static searches find no retired Retrofit paths or legacy feature symbols;
- every discovered Retrofit route is approved and no rejected route is
  discovered;
- route counts are exactly 8 + 24 + 19 = 51;
- full unit tests, formatting, lint, and debug assembly pass;
- `CONTEXT.md` describes the implemented state rather than future intent.

## Dependency Graph

```text
approved V14 specification
        ↓
baseline + contract characterization
        ↓
Quotation/Estimate vertical ───────┐
        │                          │
        └──── shared conventions ──┼──► My Eyewear navigation cutover
                                   │               ↓
Optical Order + rating vertical ───┘       messaging migration
                                                   ↓
                                  legacy deletion + 51-route governance
                                                   ↓
                                      documentation + final verification
```

## Parallel and Sequential Work

Safe to parallelize after Stage 0 and shared model conventions are fixed:

- Estimate vertical and Optical Order vertical, provided they do not edit the
  same My Eyewear coordinator/navigation files;
- Estimate presentation tests and Order presentation tests;
- static legacy-reference inventory and message fixture preparation.

Must remain sequential:

- DTO/domain contracts before their repository/ViewModel/presentation layers;
- Optical Order repository before rating and message picker consumers;
- both detail routes before navigation cutover;
- typed My Eyewear routes before message context navigation;
- all consumer migration before deleting legacy services/modules;
- retired service deletion before exact route-governance enforcement turns
  green.

Needs coordination:

- `NavGraph.kt`, `Routes.kt`, `PatientFeatureIntent.kt`, and Profile copy;
- shared pagination/error helpers;
- Chat files that consume both new repositories;
- route-governance fixtures and production service paths.

Phase 3 will translate these stages into small file-owned tasks. No concurrent
work should modify the user-owned auth files currently dirty in the worktree.

## Migration and Deletion Strategy

There is no runtime data migration, feature flag, or legacy compatibility
layer. Source migration proceeds as follows:

1. characterize the new contract with focused tests;
2. build Quotation and Optical Order verticals alongside aggregate Eyewear;
3. wire new details, rating, coordinator, and typed navigation;
4. migrate Chat to the same two resource types;
5. prove no production consumer requires aggregate/Job Order/Billing code;
6. delete obsolete verticals and tests;
7. make 51-route governance exact and update project context.

Do not stage or commit the backend-document/auth/tooling changes from the dirty
working tree unless the user later explicitly includes them in the same commit.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| One-screen wording is mistaken for permission to join records | Contract violation and broken pagination | Separate repositories, list ViewModels, UI branches, and explicit cross-links; no union domain type. |
| Both tab ViewModels eagerly request data | Extra active-link requests and incorrect initial behavior | Instantiate selected branch lazily and test that initial entry calls only Quotations Current. |
| Filter switching leaks page/results across collections | Mixed history/current cards | Separate state machines with request sequence keys and focused race tests. |
| Replacing Quotation revision model breaks hidden consumers | Compile regressions | Complete symbol/reference sweep first; migrate Chat only after repository contract stabilizes. |
| Exact money regresses through old `Double` helpers | Incorrect displayed totals | Decode with `MoneyValueSerializer`, expose `BigDecimal` only, add precision fixtures and static searches. |
| Rating UI trusts status/product ID instead of `is_rateable` | Unauthorized action or misleading affordance | Pass through server boolean and make presentation tests assert it is the sole eligibility gate. |
| Rating 200 revision is treated as failure | Patient cannot revise feedback | Retrofit accepts both success codes; repository tests cover 201 and 200 and detail state replaces summary. |
| Source link triggers hidden client join/prefetch | Extra calls and lifecycle coupling | Callback-only typed navigation; repository call occurs only after route entry. |
| Message sender ownership changes bubble styling | Staff/patient messages reversed | Map explicit sender enum and test patient, staff, and unknown cases. |
| Removing Appointment contexts surprises existing Android UI | Lost unsupported capability | Contract is authoritative; remove picker/callback and cover only the two accepted context values. |
| Context picker first-page results omit older records | Patient cannot attach an old reference | Use contract pagination state in picker or an explicit load-more UI; Phase 3 tasks must choose a bounded UI without fetching all pages. |
| Unknown context is silently lost | Confusing historical message | Preserve Unsupported domain value and render neutral non-clickable reference. |
| Route test continues tolerating retired annotations | Removed backend route survives unnoticed | Assert discovered ∩ rejected is empty and verify exact category counts. |
| Bulk deletion removes a still-used shared rating/model helper | Build/runtime regression | Delete only after `rg` reachability sweep and focused/full tests; extract reusable UI first. |
| Dirty worktree changes are overwritten or accidentally staged | User work loss | Inspect status/diffs before edits, patch narrowly, never reset, and stage explicit paths only if later requested. |

## Verification Commands

Run from the repository root with Android Studio's JBR:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Focused contract/data verification:

```powershell
.\gradlew testDebugUnitTest --tests "*Quotation*" --tests "*OpticalOrder*" --tests "*MessageDtosTest" --tests "*ChatRepositoryMappingsTest" --tests "*ApiRouteAllowlistTest"
```

Focused presentation/state verification:

```powershell
.\gradlew testDebugUnitTest --tests "*Eyewear*" --tests "*Estimate*" --tests "*OpticalOrder*" --tests "*FrameRating*" --tests "*ChatViewModelTest" --tests "*PatientFeatureIntentTest"
```

Mandatory after every production increment:

```powershell
.\gradlew assembleDebug
```

Checkpoints and final verification:

```powershell
.\gradlew ktlintFormat
.\gradlew testDebugUnitTest
.\gradlew lintDebug
.\gradlew assembleDebug
```

Manual final matrix:

1. My Eyewear opens Estimates Current and makes no initial Order request.
2. Estimate Current/History each handle loading, content, empty, retry, refresh,
   append, and append failure.
3. Order Current/History handle the same states without changing Estimate state.
4. Estimate statuses cover awaiting confirmation, confirmed, declined, expired,
   and unknown.
5. Order statuses cover queued, preparation, ready, released, cancelled, and
   unknown; payment labels remain separate.
6. Accepted Estimate with Order link opens that Order; without link it shows no
   empty action.
7. Order with source Estimate opens it; a direct Order shows no empty action.
8. Optional notes, dates, payment summary, due date, overdue state, and balance
   appear only when supplied.
9. Rating is hidden when `is_rateable` is false; create and revision both update
   the detail when true; validation preserves input.
10. Message picker links only Estimates and Optical Orders and supports older
    pages through its chosen pagination UI.
11. Incoming patient/staff/unknown senders and valid/unknown contexts render
    safely; context taps navigate correctly.
12. Limited accounts opening any My Eyewear or message-linked detail are routed
    through the existing link hub without a protected API call.
13. No screen, retry, poll, or picker produces a request to a retired path.

## Documentation Deliverables

- approved Phase 1 status in the V14 specification;
- this implementation plan approved before Phase 3 task breakdown;
- `docs/specs/backend-alignment-v14-my-eyewear-tasks.md` created only in
  Phase 3;
- V12 left intact as historical context but explicitly superseded by V14;
- `CONTEXT.md` updated after the Android implementation reflects the new
  behavior;
- backend documents left unchanged.

## Exit Criteria for Phase 2

- major components and dependencies are explicit;
- the no-join architecture is enforced at data, state, and navigation layers;
- Estimate, Order, rating, messaging, and route changes have safe buildable
  sequencing;
- legacy deletion occurs only after consumer cutover and reference proof;
- risks have concrete mitigations;
- parallel and sequential boundaries are clear;
- verification checkpoints and commands are reviewable;
- no Android production implementation has begun.

## Phase Gate

This Phase 2 plan requires project-owner approval. After approval, Phase 3 may
produce the small, dependency-ordered implementation task list and must pause
again before production code changes.
