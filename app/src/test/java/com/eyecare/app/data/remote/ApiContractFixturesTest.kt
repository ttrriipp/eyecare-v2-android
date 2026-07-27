package com.eyecare.app.data.remote

import com.eyecare.app.di.NetworkModule
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApiContractFixturesTest {

    @Test
    fun `paginated fixture contains canonical data links and meta`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.paginatedResponse)
            .jsonObject

        assertEquals(2, envelope.getValue("data").jsonArray.size)
        assertEquals(4, envelope.getValue("links").jsonObject.size)
        assertEquals(1, envelope.getValue("meta").jsonObject.getValue("current_page").jsonPrimitive.content.toInt())
        assertEquals(2, envelope.getValue("meta").jsonObject.getValue("last_page").jsonPrimitive.content.toInt())
        assertEquals(2, envelope.getValue("meta").jsonObject.getValue("total").jsonPrimitive.content.toInt())
    }

    @Test
    fun `error fixtures cover every approved shared status`() {
        assertEquals(setOf(401, 403, 404, 422, 429), ApiContractFixtures.errorResponses.keys)

        ApiContractFixtures.errorResponses.forEach { (_, fixture) ->
            val message = ApiContractFixtures.json
                .parseToJsonElement(fixture)
                .jsonObject
                .getValue("message")
                .jsonPrimitive
                .content

            assertTrue(message.isNotBlank())
        }
    }

    @Test
    fun `validation fixture preserves field error arrays`() {
        val errors = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.errorResponses.getValue(422))
            .jsonObject
            .getValue("errors")
            .jsonObject

        assertEquals(
            "The email has already been taken.",
            errors.getValue("email").jsonArray.single().jsonPrimitive.content,
        )
        assertEquals(
            "The scheduled at must be a date after now.",
            errors.getValue("scheduled_at").jsonArray.single().jsonPrimitive.content,
        )
    }

    @Test
    fun `fixtures use the production Kotlinx Json configuration`() {
        val productionConfiguration = NetworkModule.provideJson().configuration
        val fixtureConfiguration = ApiContractFixtures.json.configuration

        assertEquals(productionConfiguration.ignoreUnknownKeys, fixtureConfiguration.ignoreUnknownKeys)
        assertEquals(productionConfiguration.isLenient, fixtureConfiguration.isLenient)
        assertTrue(fixtureConfiguration.ignoreUnknownKeys)
        assertTrue(fixtureConfiguration.isLenient)
    }
}
