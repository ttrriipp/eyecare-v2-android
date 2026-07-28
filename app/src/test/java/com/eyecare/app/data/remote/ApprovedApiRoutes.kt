package com.eyecare.app.data.remote

/**
 * The 33 approved patient-mobile routes at /api/v1/.
 * Source: docs/API_CONTRACT.md Appendix.
 */
internal object ApprovedApiRoutes {
    private const val BASE = "/api/v1"

    val routes: Set<String> = setOf(
        // Auth
        "POST $BASE/register",
        "POST $BASE/login",
        "POST $BASE/logout",
        "GET $BASE/me",
        "PATCH $BASE/me",
        // Appointment setup
        "GET $BASE/appointment-types",
        "GET $BASE/appointment-availability",
        // Appointments
        "GET $BASE/appointments",
        "POST $BASE/appointments",
        "GET $BASE/appointments/{appointment}",
        "POST $BASE/appointments/{appointment}/cancel",
        "POST $BASE/appointments/{appointment}/reschedule",
        // Intake
        "GET $BASE/appointments/{appointment}/intake",
        "PUT $BASE/appointments/{appointment}/intake",
        "POST $BASE/appointments/{appointment}/intake/submit",
        // Frames
        "GET $BASE/frames",
        "GET $BASE/frames/{frame}",
        // Frame reservations
        "GET $BASE/frame-reservations",
        "POST $BASE/frame-reservations",
        "POST $BASE/frame-reservations/{reservation}/cancel",
        // Prescriptions
        "GET $BASE/prescriptions",
        "GET $BASE/prescriptions/{prescription}",
        // Quotations
        "GET $BASE/quotations",
        "GET $BASE/quotations/{quotation}",
        // Job orders
        "GET $BASE/job-orders",
        "GET $BASE/job-orders/{jobOrder}",
        // Invoices
        "GET $BASE/invoices",
        "GET $BASE/invoices/{invoice}",
        // Conversation
        "GET $BASE/conversation",
        "GET $BASE/conversation/messages",
        "POST $BASE/conversation/messages",
        "GET $BASE/conversation/attachments/{attachment}",
        // Frame ratings
        "POST $BASE/job-order-items/{item}/rating",
    )
}
