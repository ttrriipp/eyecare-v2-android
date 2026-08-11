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
        assertEquals(29, ApprovedApiRoutes.accountOnlyRoutes.size, "Account-only routes")
    }

    @Test
    fun `active-link routes match expected count`() {
        // 17 canonical active-link routes + 1 legacy alias = 18 in the contract's active-link section
        assertEquals(17, ApprovedApiRoutes.activeLinkRoutes.size, "Active-link routes (canonical)")
    }

    @Test
    fun `total approved routes is exactly 55`() {
        // 8 public + 29 account-only + 17 active-link = 54 canonical callable
        // + 1 legacy alias = 55 registered callable
        assertEquals(54, ApprovedApiRoutes.allApproved.size, "Total canonical callable routes")
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
    fun `legacy alias routes are not rejected and not callable`() {
        // The backend keeps job-order-items as a compatibility alias,
        // so it must not be in rejectedRoutes (that would assert absence from production).
        // But it must not be in allApproved either (we don't call it).
        assertTrue(
            "POST /api/v1/job-order-items/{item}/rating" in ApprovedApiRoutes.legacyAliasRoutes,
            "job-order-items rating should be in legacyAliasRoutes",
        )
        assertTrue(
            "POST /api/v1/job-order-items/{item}/rating" !in ApprovedApiRoutes.allApproved,
            "Legacy alias must not be in the approved callable set",
        )
        assertTrue(
            "POST /api/v1/job-order-items/{item}/rating" !in ApprovedApiRoutes.rejectedRoutes,
            "Legacy alias must not be in rejectedRoutes (backend keeps it intentionally)",
        )
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
        val legacyAlias = ApprovedApiRoutes.legacyAliasRoutes
        val normalizedApproved = allApproved.map { normalizeRouteVariables(it) }.toSet()
        val normalizedRejected = rejected.map { normalizeRouteVariables(it) }.toSet()
        val normalizedLegacyAlias = legacyAlias.map { normalizeRouteVariables(it) }.toSet()
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

        // Legacy alias routes must not appear in discovered production code
        val legacyAliasDiscovered = normalizedDiscovered.filter { it in normalizedLegacyAlias }
        assertTrue(
            legacyAliasDiscovered.isEmpty(),
            "Legacy alias routes found in production (use canonical path instead): $legacyAliasDiscovered",
        )
    }

    private fun normalizePathVariables(path: String): String {
        return path
            .replace(Regex("""appointments/\{id\}"""), "appointments/{appointment}")
            .replace(Regex("""appointments/\{id\}/cancel"""), "appointments/{appointment}/cancel")
            .replace(Regex("""appointments/\{id\}/reschedule"""), "appointments/{appointment}/reschedule")
            .replace(Regex("""appointments/\{id\}/rating"""), "appointments/{appointment}/rating")
            .replace(Regex("""frames/\{id\}"""), "frames/{frame}")
            .replace(Regex("""frame-reservations/\{id\}"""), "frame-reservations/{reservation}")
            .replace(Regex("""frame-reservations/\{id\}/cancel"""), "frame-reservations/{reservation}/cancel")
            .replace(Regex("""prescriptions/\{id\}"""), "prescriptions/{prescription}")
            .replace(Regex("""quotations/\{id\}"""), "quotations/{quotation}")
            .replace(Regex("""optical-orders/\{id\}"""), "optical-orders/{opticalOrder}")
            .replace(Regex("""conversation/attachments/\{id\}"""), "conversation/attachments/{attachment}")
            .replace(Regex("""optical-order-items/\{id\}"""), "optical-order-items/{item}")
            .replace(Regex("""account/contacts/\{id\}"""), "account/contacts/{contact}")
            .replace(Regex("""account/contacts/\{id\}/primary"""), "account/contacts/{contact}/primary")
            .replace(Regex("""appointment-requests/\{id\}"""), "appointment-requests/{appointmentRequest}")
            .replace(Regex("""appointment-requests/\{id\}/cancel"""), "appointment-requests/{appointmentRequest}/cancel")
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
    }
}
