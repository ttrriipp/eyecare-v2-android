package com.eyecare.app.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ApiRouteAllowlistTest {

    @Test
    fun `exactly 33 approved routes exist`() {
        assertEquals(33, ApprovedApiRoutes.routes.size)
    }

    @Test
    fun `all Retrofit service annotations match the approved allowlist`() {
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
                    // Normalize path variables: {id} -> {variable}
                    val normalizedPath = normalizePathVariables(rawPath)
                    discoveredRoutes.add("$method /api/v1/$normalizedPath")
                }
            }
        }

        val approved = ApprovedApiRoutes.routes
        val normalizedApproved = approved.map { normalizeRouteVariables(it) }.toSet()
        val normalizedDiscovered = discoveredRoutes.map { normalizeRouteVariables(it) }.toSet()

        val notApproved = normalizedDiscovered.filter { it !in normalizedApproved }
        assertTrue(
            notApproved.isEmpty(),
            "Routes not in approved allowlist: $notApproved",
        )

        val missing = normalizedApproved.filter { it !in normalizedDiscovered }
        assertTrue(
            missing.isEmpty(),
            "Approved routes not found in services: $missing",
        )

        assertEquals(33, discoveredRoutes.size, "Expected exactly 33 routes, found ${discoveredRoutes.size}")
    }

    /**
     * Normalize path variables to a common form for comparison.
     * e.g., appointments/{id} -> appointments/{appointment}
     */
    private fun normalizePathVariables(path: String): String {
        // Map known path segments to their approved variable names
        return path
            .replace(Regex("""appointments/\{id\}"""), "appointments/{appointment}")
            .replace(Regex("""appointments/\{id\}/intake"""), "appointments/{appointment}/intake")
            .replace(Regex("""appointments/\{id\}/intake/submit"""), "appointments/{appointment}/intake/submit")
            .replace(Regex("""appointments/\{id\}/cancel"""), "appointments/{appointment}/cancel")
            .replace(Regex("""appointments/\{id\}/reschedule"""), "appointments/{appointment}/reschedule")
            .replace(Regex("""frames/\{id\}"""), "frames/{frame}")
            .replace(Regex("""frame-reservations/\{id\}"""), "frame-reservations/{reservation}")
            .replace(Regex("""frame-reservations/\{id\}/cancel"""), "frame-reservations/{reservation}/cancel")
            .replace(Regex("""prescriptions/\{id\}"""), "prescriptions/{prescription}")
            .replace(Regex("""quotations/\{id\}"""), "quotations/{quotation}")
            .replace(Regex("""job-orders/\{id\}"""), "job-orders/{jobOrder}")
            .replace(Regex("""invoices/\{id\}"""), "invoices/{invoice}")
            .replace(Regex("""conversation/attachments/\{id\}"""), "conversation/attachments/{attachment}")
            .replace(Regex("""job-order-items/\{id\}"""), "job-order-items/{item}")
    }

    /**
     * Normalize the approved route variables for comparison.
     */
    private fun normalizeRouteVariables(route: String): String {
        return route
            .replace("{appointment}", "{var}")
            .replace("{frame}", "{var}")
            .replace("{reservation}", "{var}")
            .replace("{prescription}", "{var}")
            .replace("{quotation}", "{var}")
            .replace("{jobOrder}", "{var}")
            .replace("{invoice}", "{var}")
            .replace("{attachment}", "{var}")
            .replace("{item}", "{var}")
    }
}
