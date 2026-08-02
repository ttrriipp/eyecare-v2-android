# Limited Account Main Shell

Status: Implemented — 2026-08-01

## Decision

An authenticated account without an active clinic link is allowed into the
normal `MainGraph`, but it does not receive patient-record access. This keeps
the account usable while preserving the backend's active-link boundary.

## Access behavior

- `LINKED` accounts can open patient features.
- `UNLINKED`, `PENDING_REVIEW`, and `UNKNOWN` accounts can use Home, Profile,
  Account & Security, and sign-out.
- Visits, booking, frame catalog/reservations, prescriptions, eyewear,
  patient intake, and clinic messaging route to the link center instead of
  opening their active-link screens.
- Home skips active-link repository calls for limited sessions and shows a
  link explanation.
- Profile shows a persistent clinic-link card, so invitation entry is
  available without first attempting a restricted feature.

## Link options

The link center supports both backend-approved account-only paths:

1. Invitation code → invitation OTP → account refresh.
2. `POST /patient-link-requests` → clinic review, with the current request
   loaded from `GET /patient-link-requests/current`.

Invitation codes remain in the active ViewModel flow only. API error messages
are decoded through the existing structured error decoder and shown inline;
codes and tokens are never logged or persisted.

## Consequences

The session state remains `Limited` until `/me` confirms an active link. The
navigation shell therefore needs both the resolved session state and a link
center route; a link request alone does not grant clinical access.
