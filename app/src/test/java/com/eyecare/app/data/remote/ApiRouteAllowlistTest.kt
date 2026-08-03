package com.eyecare.app.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ApiRouteAllowlistTest {

    @Test
    fun `v13 auth routes match expected count`() {
        assertEquals(25, ApprovedApiRoutes.v13AuthRoutes.size, "V13 auth/account routes")
    }

    @Test
    fun `deferred routes are explicitly named`() {
        assertTrue(ApprovedApiRoutes.deferredRoutes.isNotEmpty(), "Deferred routes must be listed")
    }

    @Test
    fun `updated account-only routes are explicitly approved`() {
        assertEquals(7, ApprovedApiRoutes.accountOnlyRoutes.size, "Account-only feature routes")
        assertTrue("GET /api/v1/appointment-request-availability" in ApprovedApiRoutes.accountOnlyRoutes)
        assertTrue("GET /api/v1/frames" in ApprovedApiRoutes.accountOnlyRoutes)
        assertTrue("GET /api/v1/frames/{frame}" in ApprovedApiRoutes.accountOnlyRoutes)
    }

    @Test
    fun `legacy auth endpoints are rejected`() {
        assertTrue("POST /api/v1/register" in ApprovedApiRoutes.rejectedRoutes)
        assertTrue("POST /api/v1/login" in ApprovedApiRoutes.rejectedRoutes)
    }

    @Test
    fun `no legacy auth endpoint appears in v13 auth routes`() {
        val legacyInAuth = ApprovedApiRoutes.v13AuthRoutes.filter {
            it.endsWith("/register") && !it.contains("auth/") || it.endsWith("/login") && !it.contains("auth/")
        }
        assertTrue(legacyInAuth.isEmpty(), "Legacy auth endpoints in V13 set: $legacyInAuth")
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

        // Discovered routes must be either approved/deferred or explicitly rejected
        val unaccounted = normalizedDiscovered.filter { it !in normalizedApproved && it !in normalizedRejected }
        assertTrue(
            unaccounted.isEmpty(),
            "Discovered routes not in any approved/deferred/rejected set: $unaccounted",
        )

        // Rejected routes must not appear in approved sets
        val rejectedInApproved = normalizedRejected.filter { it in normalizedApproved }
        assertTrue(
            rejectedInApproved.isEmpty(),
            "Rejected routes found in approved set: $rejectedInApproved",
        )

        // Don't require all deferred routes to have consumers — some may be added later
        val v13AuthNormalized = ApprovedApiRoutes.v13AuthRoutes.map { normalizeRouteVariables(it) }.toSet()
        val missingAuth = v13AuthNormalized.filter { it !in normalizedDiscovered }
        assertTrue(
            missingAuth.isEmpty(),
            "V13 auth routes not found in services: $missingAuth",
        )
    }

    private fun normalizePathVariables(path: String): String {
        return path
            .replace(Regex("""appointments/\{id\}"""), "appointments/{appointment}")
            .replace(Regex("""appointments/\{id\}/cancel"""), "appointments/{appointment}/cancel")
            .replace(Regex("""appointments/\{id\}/reschedule"""), "appointments/{appointment}/reschedule")
            .replace(Regex("""frames/\{id\}"""), "frames/{frame}")
            .replace(Regex("""frame-reservations/\{id\}"""), "frame-reservations/{reservation}")
            .replace(Regex("""frame-reservations/\{id\}/cancel"""), "frame-reservations/{reservation}/cancel")
            .replace(Regex("""prescriptions/\{id\}"""), "prescriptions/{prescription}")
            .replace(Regex("""quotations/\{id\}"""), "quotations/{quotation}")
            .replace(Regex("""job-orders/\{id\}"""), "job-orders/{jobOrder}")
            .replace(Regex("""billing-records/\{id\}"""), "billing-records/{billingRecord}")
            .replace(Regex("""eyewear/\{id\}"""), "eyewear/{key}")
            .replace(Regex("""conversation/attachments/\{id\}"""), "conversation/attachments/{attachment}")
            .replace(Regex("""job-order-items/\{id\}"""), "job-order-items/{item}")
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
            .replace("{jobOrder}", "{var}")
            .replace("{billingRecord}", "{var}")
            .replace("{key}", "{var}")
            .replace("{attachment}", "{var}")
            .replace("{item}", "{var}")
            .replace("{contact}", "{var}")
            .replace("{appointmentRequest}", "{var}")
    }
}
