package com.eyecare.app.data.remote

/**
 * V16 route governance — 55-route contract.
 *
 * Categories:
 * 1. Public auth routes (8) — no authentication required
 * 2. Account-only routes (26) — authenticated, no patient link required
 * 3. Active-link routes (20) — require active patient link (canonical)
 * 4. Legacy alias routes (1) — exists server-side, must not be called by this client
 *
 * Total registered callable routes: 8 + 26 + 20 + 1 = 55.
 * Canonical routes (production Android may call): 8 + 26 + 20 = 54.
 * The legacy alias exists for backward compatibility only.
 *
 * Retired routes (eyewear, job-orders, billing-records) are explicitly rejected.
 */
internal object ApprovedApiRoutes {
    private const val BASE = "/api/v1"

    /** Public auth routes — no authentication required. (8) */
    val publicRoutes: Set<String> = setOf(
        "POST $BASE/auth/registration/otp",
        "POST $BASE/auth/registration/verify",
        "POST $BASE/auth/register",
        "POST $BASE/auth/login",
        "POST $BASE/auth/login/verify",
        "POST $BASE/auth/password-recovery/otp",
        "POST $BASE/auth/password-recovery/verify",
        "GET $BASE/auth/policies",
    )

    /** Account-only routes — authenticated, no patient link required. (26) */
    val accountOnlyRoutes: Set<String> = setOf(
        "POST $BASE/logout",
        "POST $BASE/logout-all",
        "GET $BASE/me",
        "PATCH $BASE/me",
        "POST $BASE/auth/step-up/otp",
        "POST $BASE/auth/step-up/verify",
        "POST $BASE/auth/password",
        "GET $BASE/account/contacts",
        "POST $BASE/account/contacts/otp",
        "POST $BASE/account/contacts/verify",
        "PATCH $BASE/account/contacts/{contact}/primary",
        "DELETE $BASE/account/contacts/{contact}",
        "GET $BASE/account/link",
        "POST $BASE/patient-link-requests",
        "GET $BASE/patient-link-requests/current",
        "POST $BASE/patient-invitations/acceptance/otp",
        "POST $BASE/patient-invitations/accept",
        "GET $BASE/appointment-types",
        "GET $BASE/appointment-optometrists",
        "GET $BASE/appointment-request-availability",
        "GET $BASE/appointment-requests",
        "POST $BASE/appointment-requests",
        "GET $BASE/appointment-requests/{appointmentRequest}",
        "POST $BASE/appointment-requests/{appointmentRequest}/cancel",
        "GET $BASE/frames",
        "GET $BASE/frames/{frame}",
    )

    /** Active-link routes — require active patient link. (20 canonical) */
    val activeLinkRoutes: Set<String> = setOf(
        "GET $BASE/appointment-availability",
        "GET $BASE/appointments",
        "GET $BASE/appointments/{appointment}",
        "POST $BASE/appointments/{appointment}/cancel",
        "POST $BASE/appointments/{appointment}/reschedule",
        "POST $BASE/appointments/{appointment}/rating",
        "GET $BASE/frame-reservations",
        "POST $BASE/frame-reservations",
        "POST $BASE/frame-reservations/{reservation}/cancel",
        "GET $BASE/prescriptions",
        "GET $BASE/prescriptions/{prescription}",
        "GET $BASE/quotations",
        "GET $BASE/quotations/{quotation}",
        "GET $BASE/optical-orders",
        "GET $BASE/optical-orders/{opticalOrder}",
        "GET $BASE/conversation",
        "GET $BASE/conversation/messages",
        "POST $BASE/conversation/messages",
        "GET $BASE/conversation/attachments/{attachment}",
        "POST $BASE/optical-order-items/{item}/rating",
    )

    /** Retired routes — must not appear in any production Retrofit annotation. */
    val rejectedRoutes: Set<String> = setOf(
        // Legacy auth
        "POST $BASE/register",
        "POST $BASE/login",
        // Aggregate eyewear (V12)
        "GET $BASE/eyewear",
        "GET $BASE/eyewear/{key}",
        // Job orders (replaced by optical-orders)
        "GET $BASE/job-orders",
        "GET $BASE/job-orders/{jobOrder}",
        // Billing records (retired)
        "GET $BASE/billing-records",
        "GET $BASE/billing-records/{billingRecord}",
        // Appointment intake (retired)
        "GET $BASE/appointments/{appointment}/intake",
        "PUT $BASE/appointments/{appointment}/intake",
        "POST $BASE/appointments/{appointment}/intake/submit",
    )

    /**
     * Legacy alias routes — exist server-side for backward compatibility,
     * but must not be called by this client. We use the canonical path instead.
     * Asserted: no production Retrofit annotation references these.
     */
    val legacyAliasRoutes: Set<String> = setOf(
        "POST $BASE/job-order-items/{item}/rating",
    )

    val allApproved: Set<String> = publicRoutes + accountOnlyRoutes + activeLinkRoutes
}
