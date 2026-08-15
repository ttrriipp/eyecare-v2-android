package com.eyecare.app.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ApiRouteAllowlistTest {

    @Test
    fun `public routes match expected count`() {
        assertEquals(8, ApprovedApiRoutes.publicRoutes.size, "Public auth routes")
    }

    @Test
    fun `account-only routes match expected count`() {
        assertEquals(36, ApprovedApiRoutes.accountOnlyRoutes.size, "Account-only routes")
    }

    @Test
    fun `active-link routes match expected count`() {
        assertEquals(17, ApprovedApiRoutes.activeLinkRoutes.size, "Active-link routes")
    }

    @Test
    fun `total approved routes is exactly 61`() {
        // 8 public + 36 account-only + 17 active-link = 61 canonical callable
        assertEquals(61, ApprovedApiRoutes.allApproved.size, "Total canonical callable routes")
    }

    @Test
    fun `retired routes are rejected`() {
        assertTrue("GET /api/v1/eyewear" in ApprovedApiRoutes.rejectedRoutes)
        assertTrue("GET /api/v1/eyewear/{key}" in ApprovedApiRoutes.rejectedRoutes)
        assertTrue("GET /api/v1/job-orders" in ApprovedApiRoutes.rejectedRoutes)
        assertTrue("GET /api/v1/job-orders/{jobOrder}" in ApprovedApiRoutes.rejectedRoutes)
        assertTrue("GET /api/v1/billing-records" in ApprovedApiRoutes.rejectedRoutes)
        assertTrue("GET /api/v1/billing-records/{billingRecord}" in ApprovedApiRoutes.rejectedRoutes)
    }

    @Test
    fun `legacy alias is now rejected`() {
        // V18: The legacy alias POST /job-order-items/{id}/rating was deleted from the server.
        assertTrue(
            "POST /api/v1/job-order-items/{item}/rating" in ApprovedApiRoutes.rejectedRoutes,
            "Legacy alias must be in rejectedRoutes",
        )
    }

    @Test
    fun `quotation routes are rejected`() {
        assertTrue("GET /api/v1/quotations" in ApprovedApiRoutes.rejectedRoutes)
        assertTrue("GET /api/v1/quotations/{quotation}" in ApprovedApiRoutes.rejectedRoutes)
    }

    @Test
    fun `appointment types is approved and no longer rejected`() {
        assertTrue(
            "GET /api/v1/appointment-types" in ApprovedApiRoutes.accountOnlyRoutes,
            "appointment-types must be an approved account-only route",
        )
        assertTrue(
            "GET /api/v1/appointment-types" !in ApprovedApiRoutes.rejectedRoutes,
            "appointment-types must not be in rejected routes",
        )
    }

    @Test
    fun `appointment optometrists is approved account-only`() {
        assertTrue(
            "GET /api/v1/appointment-optometrists" in ApprovedApiRoutes.accountOnlyRoutes,
            "appointment-optometrists must be an approved account-only route",
        )
    }

    @Test
    fun `clinic hours is approved account-only`() {
        assertTrue(
            "GET /api/v1/clinic-hours" in ApprovedApiRoutes.accountOnlyRoutes,
            "clinic-hours must be an approved account-only route",
        )
    }

    @Test
    fun `conversation read and send are account-only`() {
        assertTrue(
            "GET /api/v1/conversation" in ApprovedApiRoutes.accountOnlyRoutes,
            "conversation read must be account-only",
        )
        assertTrue(
            "GET /api/v1/conversation/messages" in ApprovedApiRoutes.accountOnlyRoutes,
            "conversation message list must be account-only",
        )
        assertTrue(
            "POST /api/v1/conversation/messages" in ApprovedApiRoutes.accountOnlyRoutes,
            "conversation send must be account-only",
        )
    }

    @Test
    fun `conversation attachment download remains active-link`() {
        assertTrue(
            "GET /api/v1/conversation/attachments/{attachment}" in ApprovedApiRoutes.activeLinkRoutes,
            "attachment download must remain active-link protected",
        )
    }

    @Test
    fun `conversation search is account-only`() {
        assertTrue(
            "GET /api/v1/conversation/messages/search" in ApprovedApiRoutes.accountOnlyRoutes,
            "conversation search must be account-only",
        )
    }

    @Test
    fun `conversation mark-read is account-only`() {
        assertTrue(
            "POST /api/v1/conversation/messages/read" in ApprovedApiRoutes.accountOnlyRoutes,
            "conversation mark-read must be account-only",
        )
    }

    @Test
    fun `notification list is account-only`() {
        assertTrue(
            "GET /api/v1/notifications" in ApprovedApiRoutes.accountOnlyRoutes,
            "notification list must be account-only",
        )
    }

    @Test
    fun `notification unread count is account-only`() {
        assertTrue(
            "GET /api/v1/notifications/unread-count" in ApprovedApiRoutes.accountOnlyRoutes,
            "notification unread count must be account-only",
        )
    }

    @Test
    fun `notification mark-one is account-only`() {
        assertTrue(
            "PATCH /api/v1/notifications/{notification}/read" in ApprovedApiRoutes.accountOnlyRoutes,
            "notification mark-one must be account-only",
        )
    }

    @Test
    fun `notification mark-all is account-only`() {
        assertTrue(
            "PATCH /api/v1/notifications/read-all" in ApprovedApiRoutes.accountOnlyRoutes,
            "notification mark-all must be account-only",
        )
    }

    @Test
    fun `all Retrofit service annotations are accounted for`() {
        val serviceDir = File("src/main/java/com/eyecare/app/data/remote/api")
        assertTrue(serviceDir.exists(), "API service directory not found")

        val discoveredRoutes = mutableSetOf<String>()

        serviceDir.listFiles()?.filter { it.extension == "kt" }?.forEach { file ->
            val content = file.readText()
            val httpMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")

            httpMethods.forEach { method ->
                val pattern = Regex("""@$method\("([^"]+)"\)""")
                pattern.findAll(content).forEach { match ->
                    val rawPath = match.groupValues[1]
                    val normalizedPath = normalizePathVariables(rawPath)
                    discoveredRoutes.add("$method /api/v1/$normalizedPath")
                }
            }
        }

        val allApproved = ApprovedApiRoutes.allApproved
        val rejected = ApprovedApiRoutes.rejectedRoutes
        val normalizedApproved = allApproved.map { normalizeRouteVariables(it) }.toSet()
        val normalizedRejected = rejected.map { normalizeRouteVariables(it) }.toSet()
        val normalizedDiscovered = discoveredRoutes.map { normalizeRouteVariables(it) }.toSet()

        // Every discovered route must be approved (not rejected)
        val unaccounted = normalizedDiscovered.filter { it !in normalizedApproved }
        assertTrue(
            unaccounted.isEmpty(),
            "Discovered routes not in approved set: $unaccounted",
        )

        // Rejected routes must not appear in discovered production code
        val rejectedDiscovered = normalizedDiscovered.filter { it in normalizedRejected }
        assertTrue(
            rejectedDiscovered.isEmpty(),
            "Rejected routes found in production: $rejectedDiscovered",
        )
    }

    private fun normalizePathVariables(path: String): String {
        return path
            .replace(Regex("""appointments/\{id\}"""), "appointments/{appointment}")
            .replace(Regex("""appointments/\{id\}/cancel"""), "appointments/{appointment}/cancel")
            .replace(Regex("""appointments/\{id\}/reschedule"""), "appointments/{appointment}/reschedule")
            .replace(Regex("""appointments/\{id\}/rating"""), "appointments/{appointment}/rating")
            .replace(Regex("""frames/\{id\}"""), "frames/{frame}")
            .replace(Regex("""frame-reservations/\{id\}/cancel"""), "frame-reservations/{reservation}/cancel")
            .replace(Regex("""frame-reservations/\{id\}/items/\{itemId\}"""), "frame-reservations/{reservation}/items/{item}")
            .replace(Regex("""frame-reservations/\{id\}"""), "frame-reservations/{reservation}")
            .replace(Regex("""prescriptions/\{id\}"""), "prescriptions/{prescription}")
            .replace(Regex("""quotations/\{id\}"""), "quotations/{quotation}")
            .replace(Regex("""optical-orders/\{id\}"""), "optical-orders/{opticalOrder}")
            .replace(Regex("""conversation/attachments/\{id\}"""), "conversation/attachments/{attachment}")
            .replace(Regex("""optical-order-items/\{id\}"""), "optical-order-items/{item}")
            .replace(Regex("""account/contacts/\{id\}"""), "account/contacts/{contact}")
            .replace(Regex("""account/contacts/\{id\}/primary"""), "account/contacts/{contact}/primary")
            .replace(Regex("""appointment-requests/\{id\}"""), "appointment-requests/{appointmentRequest}")
            .replace(Regex("""appointment-requests/\{id\}/cancel"""), "appointment-requests/{appointmentRequest}/cancel")
            .replace(Regex("""notifications/\{id\}"""), "notifications/{notification}")
            .replace(Regex("""notifications/\{id\}/read"""), "notifications/{notification}/read")
    }

    private fun normalizeRouteVariables(route: String): String {
        return route
            .replace("{appointment}", "{var}")
            .replace("{frame}", "{var}")
            .replace("{reservation}", "{var}")
            .replace("{prescription}", "{var}")
            .replace("{quotation}", "{var}")
            .replace("{opticalOrder}", "{var}")
            .replace("{attachment}", "{var}")
            .replace("{item}", "{var}")
            .replace("{contact}", "{var}")
            .replace("{appointmentRequest}", "{var}")
            .replace("{notification}", "{var}")
    }
}
