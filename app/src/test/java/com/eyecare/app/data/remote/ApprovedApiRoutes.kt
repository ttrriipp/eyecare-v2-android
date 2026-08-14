package com.eyecare.app.data.remote

/**
 * V18 route governance — 55-route contract.
 *
 * Categories:
 * 1. Public auth routes (8) — no authentication required
 * 2. Account-only routes (30) — authenticated, no patient link required
 * 3. Active-link routes (17) — require active patient link
 *
 * Total canonical callable routes: 8 + 30 + 17 = 55.
 *
 * Conversation read/list/send are account-only; attachment download remains active-link.
 * Retired routes (eyewear, job-orders, billing-records, quotations, legacy aliases) are rejected.
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

    /** Account-only routes — authenticated, no patient link required. (30) */
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
        "GET $BASE/clinic-hours",
        "GET $BASE/appointment-request-availability",
        "GET $BASE/appointment-requests",
        "POST $BASE/appointment-requests",
        "GET $BASE/appointment-requests/{appointmentRequest}",
        "POST $BASE/appointment-requests/{appointmentRequest}/cancel",
        "GET $BASE/frames",
        "GET $BASE/frames/{frame}",
        // Conversation — account-owned text messaging
        "GET $BASE/conversation",
        "GET $BASE/conversation/messages",
        "POST $BASE/conversation/messages",
    )

    /** Active-link routes — require active patient link. (17) */
    val activeLinkRoutes: Set<String> = setOf(
        "GET $BASE/appointment-availability",
        "GET $BASE/appointments",
        "GET $BASE/appointments/{appointment}",
        "POST $BASE/appointments/{appointment}/cancel",
        "POST $BASE/appointments/{appointment}/reschedule",
        "POST $BASE/appointments/{appointment}/rating",
        "GET $BASE/frame-reservations",
        "POST $BASE/frame-reservations",
        "DELETE $BASE/frame-reservations/{reservation}",
        "POST $BASE/frame-reservations/{reservation}/items",
        "DELETE $BASE/frame-reservations/{reservation}/items/{item}",
        "GET $BASE/prescriptions",
        "GET $BASE/prescriptions/{prescription}",
        "GET $BASE/optical-orders",
        "GET $BASE/optical-orders/{opticalOrder}",
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
        // Reservation cancel (replaced by DELETE /frame-reservations/{id})
        "POST $BASE/frame-reservations/{reservation}/cancel",
        // Quotations (deleted from server)
        "GET $BASE/quotations",
        "GET $BASE/quotations/{quotation}",
        // Legacy alias for backward compatibility — use canonical path
        "POST $BASE/job-order-items/{item}/rating",
    )

    val allApproved: Set<String> = publicRoutes + accountOnlyRoutes + activeLinkRoutes
}
