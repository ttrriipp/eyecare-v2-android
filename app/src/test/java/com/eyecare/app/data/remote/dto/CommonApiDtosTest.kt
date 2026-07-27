package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.ApiContractFixtures
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CommonApiDtosTest {

    private val json = ApiContractFixtures.json

    @Test
    fun `pagination links decode all fields`() {
        val linksJson = json.parseToJsonElement(
            """
            {
              "first": "https://example.test/api/v1/resources?page=1",
              "last": "https://example.test/api/v1/resources?page=2",
              "prev": null,
              "next": "https://example.test/api/v1/resources?page=2"
            }
            """.trimIndent(),
        ).jsonObject

        val links = json.decodeFromString<PaginationLinks>(linksJson.toString())

        assertEquals("https://example.test/api/v1/resources?page=1", links.first)
        assertEquals("https://example.test/api/v1/resources?page=2", links.last)
        assertNull(links.prev)
        assertEquals("https://example.test/api/v1/resources?page=2", links.next)
    }

    @Test
    fun `pagination links decode with all nulls`() {
        val links = json.decodeFromString<PaginationLinks>(
            """
            {
              "first": null,
              "last": null,
              "prev": null,
              "next": null
            }
            """.trimIndent(),
        )

        assertNull(links.first)
        assertNull(links.last)
        assertNull(links.prev)
        assertNull(links.next)
    }

    @Test
    fun `pagination meta decodes all fields`() {
        val meta = json.decodeFromString<PaginationMeta>(
            """
            {
              "current_page": 1,
              "last_page": 2,
              "per_page": 15,
              "total": 30
            }
            """.trimIndent(),
        )

        assertEquals(1, meta.currentPage)
        assertEquals(2, meta.lastPage)
        assertEquals(15, meta.perPage)
        assertEquals(30, meta.total)
    }

    @Test
    fun `api error body decodes message only`() {
        val error = json.decodeFromString<ApiErrorBody>(
            """
            {
              "message": "Unauthenticated."
            }
            """.trimIndent(),
        )

        assertEquals("Unauthenticated.", error.message)
        assertNull(error.errors)
    }

    @Test
    fun `api error body decodes message with field errors`() {
        val error = json.decodeFromString<ApiErrorBody>(
            ApiContractFixtures.errorResponses.getValue(422),
        )

        assertEquals("The given data was invalid.", error.message)
        assertEquals(
            "The email has already been taken.",
            error.errors?.getValue("email")?.single(),
        )
        assertEquals(
            "The scheduled at must be a date after now.",
            error.errors?.getValue("scheduled_at")?.single(),
        )
    }

    @Test
    fun `paginated fixture decodes through shared DTOs`() {
        val envelope = json.parseToJsonElement(ApiContractFixtures.paginatedResponse).jsonObject

        val links = json.decodeFromString<PaginationLinks>(envelope.getValue("links").toString())
        val meta = json.decodeFromString<PaginationMeta>(envelope.getValue("meta").toString())

        assertEquals("https://example.test/api/v1/resources?page=1", links.first)
        assertEquals(1, meta.currentPage)
        assertEquals(2, meta.total)
    }
}
