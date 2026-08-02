# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Eyecare is for Padilla Optical Clinic patients, including patients whose mobile account is already linked to a clinic record and newly registered patients who still need to be linked. Clinic staff are an important counterpart in the workflow, but they use the separate clinic administration system rather than this Android app.

## Product Purpose

Eyecare is the clinic's patient-facing Android companion. It gives patients one place to manage appointments, access eligible clinical records, communicate with the clinic, and discover eyewear with augmented-reality frame try-on.

Success means patients can complete routine clinic interactions from their phone while clinical identity, sensitive records, and staff-controlled decisions remain correctly governed by the clinic system.

## Positioning

Eyecare connects a patient's mobile account to Padilla Optical Clinic's real clinical record and workflow. Its distinguishing mechanism is the combination of clinic-linked care access, direct clinic communication, and AR-assisted eyewear discovery within one patient account.

An account may exist before a clinical record link is established. Unlinked patients retain safe app access and a persistent path to enter an invitation code or request clinic review, while patient-specific clinical features remain gated until the link is active.

## Operating Context

Patients use the app before and between clinic visits to:

- register and sign in through a phone-primary OTP flow;
- link their account to a clinic patient record;
- review upcoming and historical clinic activity;
- request, review, reschedule, or cancel eligible appointments;
- view prescriptions and other eligible eyewear or billing information;
- browse AR-ready frames, try them on with the front-facing camera, and manage eligible frame reservations;
- exchange messages and contextual appointment or order information with clinic staff;
- maintain their supported account and contact details.

The Laravel API and clinic administration system remain authoritative for clinical identity, permissions, availability, workflow status, inventory, and health information.

## Capabilities and Constraints

- The product is a native Android application built with Kotlin and Jetpack Compose.
- Patient authentication is phone-primary: registration verifies the phone before collecting account details, while login verifies the phone and password and requests OTP step-up when required.
- A mobile account and a clinic patient record are separate identities until explicitly linked through an invitation or clinic-reviewed request.
- Unlinked accounts may use the normal app shell and account-safe features, but patient-specific clinical destinations require an active clinic link.
- The main patient navigation roots are Home, Frames, Visits, and Profile.
- Camera access is used for AR frame try-on. AR eligibility and asset references come from clinic-managed product data.
- Tokens and health data must not be stored in Room; local database storage is limited to non-sensitive catalog caching.
- Network DTOs are mapped to domain models at the repository boundary, and the mobile API contract is authoritative for transport behavior.
- The app does not create or infer clinical records, clinical matches, prescriptions, appointment availability, or staff decisions locally.

## Brand Commitments

- Product name: Eyecare.
- Clinic name: Padilla Optical Clinic.
- The existing Android brand identity is authoritative for the mobile app.
- Canonical Android primary blue: `#29B6F6`.
- Existing clinic and app logos must be treated as factual brand assets rather than recreated or replaced without approval.

## Evidence on Hand

- Product and implementation context: `CONTEXT.md`.
- Authoritative backend behavior: `docs/BACKEND_CONTEXT.md`.
- Authoritative mobile transport contract: `docs/API_CONTRACT.md`.
- Current Android implementation: `app/src/main/java/com/eyecare/app/`.
- Current Android theme and canonical primary blue: `app/src/main/java/com/eyecare/app/ui/theme/`.
- Existing launcher and application assets: `app/src/main/res/`.
- Feature decisions and acceptance criteria: `docs/specs/`.

No testimonials, patient outcome claims, usage benchmarks, awards, or third-party endorsements are currently established in the repository and must not be fabricated.

## Product Principles

1. Keep account identity and clinical identity separate until the clinic link is explicitly verified.
2. Give unlinked patients useful, safe access and a clear path to become linked without exposing patient-specific records early.
3. Treat backend clinical, scheduling, inventory, and workflow state as authoritative.
4. Make routine patient tasks understandable and recoverable, especially authentication, linking, appointments, and messaging.
5. Protect sensitive health and account data throughout storage, navigation, and error handling.

## Accessibility & Inclusion

Existing Android requirements call for meaningful TalkBack labels, touch targets of at least 48dp, state communication that does not rely on color alone, logical focus order, and layouts that remain usable on compact screens and at increased font sizes. No formal accessibility compliance target has been confirmed.
