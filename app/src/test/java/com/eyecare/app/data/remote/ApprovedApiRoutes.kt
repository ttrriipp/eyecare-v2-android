package com.eyecare.app.data.remote

/**
 * V13 auth-only route governance.
 *
 * Two explicit categories:
 * 1. V13 auth/account routes — new endpoints consumed by this phase
 * 2. Account-only feature routes — authenticated routes that do not require a patient link
 * 3. Deferred active-link routes — existing consumers awaiting coordinated V13 migration
 *
 * Legacy /login and /register are explicitly rejected.
 * The full 55-route V13 allowlist is represented here so Android consumers cannot drift from
 * the backend contract.
 */
internal object ApprovedApiRoutes {
    private const val BASE = "/api/v1"

    /** New V13 auth/account routes consumed by this phase. */
    val v13AuthRoutes: Set<String> = setOf(
        // Public auth
        "POST $BASE/auth/registration/otp",
        "POST $BASE/auth/registration/verify",
        "POST $BASE/auth/register",
        "POST $BASE/auth/login",
        "POST $BASE/auth/login/verify",
        "POST $BASE/auth/password-recovery/otp",
        "POST $BASE/auth/password-recovery/verify",
        "GET $BASE/auth/policies",
        // Authenticated account-only
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
    )

    /** Authenticated feature routes that are available to linked and unlinked accounts. */
    val accountOnlyRoutes: Set<String> = setOf(
        "GET $BASE/appointment-request-availability",
        "GET $BASE/appointment-requests",
        "POST $BASE/appointment-requests",
        "GET $BASE/appointment-requests/{appointmentRequest}",
        "POST $BASE/appointment-requests/{appointmentRequest}/cancel",
        "GET $BASE/frames",
        "GET $BASE/frames/{frame}",
    )

    /**
     * Deferred non-auth routes with existing Android consumers.
     * These remain as migration debt — NOT claimed as approved by the updated V13 backend.
     * Each entry documents why it's deferred.
     */
    val deferredRoutes: Set<String> = setOf(
        // Appointment availability — deferred (requires visit_reason_id in responses)
        "GET $BASE/appointment-availability",
        // Confirmed appointments — deferred active-link cutover
        "GET $BASE/appointments",
        "GET $BASE/appointments/{appointment}",
        "POST $BASE/appointments/{appointment}/cancel",
        "POST $BASE/appointments/{appointment}/reschedule",
        // Frame reservations — deferred active-link cutover
        "GET $BASE/frame-reservations",
        "POST $BASE/frame-reservations",
        "POST $BASE/frame-reservations/{reservation}/cancel",
        // Prescriptions — deferred active-link cutover
        "GET $BASE/prescriptions",
        "GET $BASE/prescriptions/{prescription}",
        // Quotations — deferred active-link cutover
        "GET $BASE/quotations",
        "GET $BASE/quotations/{quotation}",
        // Job orders — deferred active-link cutover
        "GET $BASE/job-orders",
        "GET $BASE/job-orders/{jobOrder}",
        // Billing records — deferred active-link cutover
        "GET $BASE/billing-records",
        "GET $BASE/billing-records/{billingRecord}",
        // Eyewear aggregate — deferred active-link cutover
        "GET $BASE/eyewear",
        "GET $BASE/eyewear/{key}",
        // Conversation — deferred active-link cutover
        "GET $BASE/conversation",
        "GET $BASE/conversation/messages",
        "POST $BASE/conversation/messages",
        "GET $BASE/conversation/attachments/{attachment}",
        // Frame ratings — deferred active-link cutover
        "POST $BASE/job-order-items/{item}/rating",
    )

    /** Routes explicitly rejected — legacy endpoints removed by V13. */
    val rejectedRoutes: Set<String> = setOf(
        "POST $BASE/register",
        "POST $BASE/login",
        "GET $BASE/appointment-types",
        "POST $BASE/appointments",
        "GET $BASE/appointments/{appointment}/intake",
        "PUT $BASE/appointments/{appointment}/intake",
        "POST $BASE/appointments/{appointment}/intake/submit",
    )

    val allApproved: Set<String> = v13AuthRoutes + accountOnlyRoutes + deferredRoutes
}
