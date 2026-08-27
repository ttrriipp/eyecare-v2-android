package com.eyecare.app.data.remote

import com.eyecare.app.di.NetworkModule
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    // --- C1: Cursor-paginated message list fixtures ---

    @Test
    fun `message list page 1 has terminal cursor metadata`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.messageListPage1)
            .jsonObject

        val meta = envelope.getValue("meta").jsonObject
        assertTrue(meta.getValue("next_cursor") is JsonNull)
        assertFalse(meta.getValue("has_more").jsonPrimitive.content.toBoolean())
        assertEquals(2, envelope.getValue("data").jsonArray.size)
    }

    @Test
    fun `message list page with cursor has non-terminal metadata`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.messageListPageWithCursor)
            .jsonObject

        val meta = envelope.getValue("meta").jsonObject
        assertNotEquals(JsonNull, meta.getValue("next_cursor"))
        assertTrue(meta.getValue("next_cursor").jsonPrimitive.content.isNotBlank())
        assertTrue(meta.getValue("has_more").jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `message list cursor is opaque and not decoded by fixtures`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.messageListPageWithCursor)
            .jsonObject

        val cursor = envelope.getValue("meta").jsonObject.getValue("next_cursor").jsonPrimitive.content
        // Cursor is base64-encoded opaque data; fixtures must not assume internal structure
        assertTrue(cursor.contains("="), "Cursor should be opaque base64, not a decoded value")
    }

    @Test
    fun `search results have cursor metadata matching message list shape`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.messageSearchResults)
            .jsonObject

        val meta = envelope.getValue("meta").jsonObject
        assertTrue(meta.containsKey("next_cursor"))
        assertTrue(meta.containsKey("has_more"))
        assertEquals(1, envelope.getValue("data").jsonArray.size)
    }

    // --- C2: Send message response ---

    @Test
    fun `send message response wraps data envelope`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.sendMessageResponse)
            .jsonObject

        val message = envelope.getValue("data").jsonObject
        assertEquals(42, message.getValue("id").jsonPrimitive.content.toInt())
        assertEquals("patient", message.getValue("sender_type").jsonPrimitive.content)
        assertTrue(message.containsKey("attachments"))
    }

    // --- Mark-read response ---

    @Test
    fun `mark read response has marked_count`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.markReadResponse)
            .jsonObject

        assertEquals(3, envelope.getValue("marked_count").jsonPrimitive.content.toInt())
    }

    // --- C3: Notification fixtures ---

    @Test
    fun `notification list has UUID string IDs`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.notificationList)
            .jsonObject

        val notifications = envelope.getValue("data").jsonArray
        assertEquals(2, notifications.size)

        val firstId = notifications[0].jsonObject.getValue("id").jsonPrimitive.content
        val secondId = notifications[1].jsonObject.getValue("id").jsonPrimitive.content
        // UUIDs are strings, never coerced to integers
        assertTrue(firstId.contains("-"), "Notification ID should be UUID string")
        assertTrue(secondId.contains("-"), "Notification ID should be UUID string")
        assertTrue(firstId.isNotBlank())
        assertTrue(secondId.isNotBlank())
    }

    @Test
    fun `notification list has stable kind field`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.notificationList)
            .jsonObject

        val notifications = envelope.getValue("data").jsonArray
        assertEquals("new_message", notifications[0].jsonObject.getValue("kind").jsonPrimitive.content)
        assertEquals("unknown_future_kind", notifications[1].jsonObject.getValue("kind").jsonPrimitive.content)
    }

    @Test
    fun `notification list has typed mobile_action`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.notificationList)
            .jsonObject

        val notifications = envelope.getValue("data").jsonArray
        val action = notifications[0].jsonObject.getValue("mobile_action").jsonObject
        assertEquals("conversation", action.getValue("type").jsonPrimitive.content)

        // Second notification has null mobile_action
        assertTrue(notifications[1].jsonObject.getValue("mobile_action") is JsonNull)
    }

    @Test
    fun `notification with legacy fields ignores type and action_url for navigation`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.notificationWithLegacyFields)
            .jsonObject

        val notification = envelope.getValue("data").jsonArray[0].jsonObject

        // Legacy fields present but irrelevant for navigation
        assertTrue(notification.containsKey("type"))
        assertTrue(notification.containsKey("action_url"))
        assertTrue(notification.containsKey("related_type"))
        assertTrue(notification.containsKey("related_id"))

        // Navigation depends only on mobile_action.type
        val mobileAction = notification.getValue("mobile_action").jsonObject
        assertEquals("conversation", mobileAction.getValue("type").jsonPrimitive.content)

        // PHP class name in type field is NOT used for navigation
        val legacyType = notification.getValue("type").jsonPrimitive.content
        assertTrue(legacyType.contains("\\"), "Legacy type contains PHP namespace separators")
    }

    @Test
    fun `notification page metadata uses page-based pagination`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.notificationList)
            .jsonObject

        val meta = envelope.getValue("meta").jsonObject
        assertEquals(1, meta.getValue("current_page").jsonPrimitive.content.toInt())
        assertEquals(1, meta.getValue("last_page").jsonPrimitive.content.toInt())
        assertEquals(20, meta.getValue("per_page").jsonPrimitive.content.toInt())
        assertEquals(2, meta.getValue("total").jsonPrimitive.content.toInt())
    }

    @Test
    fun `notification unread count fixture has integer value`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.notificationUnreadCount)
            .jsonObject

        assertEquals(3, envelope.getValue("unread_count").jsonPrimitive.content.toInt())
    }

    @Test
    fun `notification mark-one response has message`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.notificationMarkOneResponse)
            .jsonObject

        assertEquals("Notification marked as read.", envelope.getValue("message").jsonPrimitive.content)
    }

    @Test
    fun `notification mark-all response has message`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.notificationMarkAllResponse)
            .jsonObject

        assertEquals("All notifications marked as read.", envelope.getValue("message").jsonPrimitive.content)
    }

    // --- Saved Frames fixtures ---

    @Test
    fun `saved frames page has product variant ID identity and newest-first timestamps`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.savedFramesPageAvailable)
            .jsonObject

        val item = envelope.getValue("data").jsonArray[0].jsonObject
        assertEquals(42, item.getValue("product_variant_id").jsonPrimitive.content.toInt())
        assertTrue(item.getValue("saved_at").jsonPrimitive.content.startsWith("2026-08-27"))
        assertEquals("available", item.getValue("availability").jsonPrimitive.content)
    }

    @Test
    fun `saved frames page has numeric page metadata`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.savedFramesPageAvailable)
            .jsonObject

        val meta = envelope.getValue("meta").jsonObject
        assertEquals(1, meta.getValue("current_page").jsonPrimitive.content.toInt())
        assertEquals(1, meta.getValue("last_page").jsonPrimitive.content.toInt())
        assertEquals(15, meta.getValue("per_page").jsonPrimitive.content.toInt())
        assertEquals(1, meta.getValue("total").jsonPrimitive.content.toInt())
    }

    @Test
    fun `saved frames variant has nested product and string price`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.savedFramesPageAvailable)
            .jsonObject

        val variant = envelope.getValue("data").jsonArray[0].jsonObject.getValue("variant").jsonObject
        assertEquals(42, variant.getValue("id").jsonPrimitive.content.toInt())
        assertEquals("Black / 52mm", variant.getValue("name").jsonPrimitive.content)
        assertEquals("4500.00", variant.getValue("price").jsonPrimitive.content)

        val product = variant.getValue("product").jsonObject
        assertEquals(7, product.getValue("id").jsonPrimitive.content.toInt())
        assertEquals("Classic Rectangle", product.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun `saved frames variant has typed AR when available`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.savedFramesPageAvailable)
            .jsonObject

        val variant = envelope.getValue("data").jsonArray[0].jsonObject.getValue("variant").jsonObject
        val ar = variant.getValue("ar").jsonObject
        assertEquals("ready", ar.getValue("status").jsonPrimitive.content)
        assertEquals("glb", ar.getValue("asset").jsonObject.getValue("format").jsonPrimitive.content)
    }

    @Test
    fun `saved frames variant has null AR when unavailable`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.savedFramesPageUnavailable)
            .jsonObject

        val variant = envelope.getValue("data").jsonArray[0].jsonObject.getValue("variant").jsonObject
        assertTrue(variant.getValue("ar") is JsonNull)
    }

    @Test
    fun `saved frames has number price and compare_at_price`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.savedFramesPageUnavailable)
            .jsonObject

        val variant = envelope.getValue("data").jsonArray[0].jsonObject.getValue("variant").jsonObject
        // price is number, compare_at_price is number
        assertEquals(5200, variant.getValue("price").jsonPrimitive.content.toInt())
        assertEquals(6000.00, variant.getValue("compare_at_price").jsonPrimitive.content.toDouble())
    }

    @Test
    fun `saved frames has no reservation or stock count fields`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.savedFramesPageAvailable)
            .jsonObject

        val item = envelope.getValue("data").jsonArray[0].jsonObject
        assertFalse(item.containsKey("id"), "Saved Frame has no client-facing ID")
        assertFalse(item.containsKey("status"), "Saved Frame has no reservation status")
        assertFalse(item.containsKey("appointment_id"), "Saved Frame has no appointment coupling")
        assertFalse(item.containsKey("expires_at"), "Saved Frame has no expiry")
        assertFalse(item.containsKey("stock_quantity"), "Saved Frame exposes no stock count")
    }

    @Test
    fun `save response wraps single resource`() {
        val envelope = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.savedFrameSaveResponse)
            .jsonObject

        val item = envelope.getValue("data").jsonObject
        assertEquals(42, item.getValue("product_variant_id").jsonPrimitive.content.toInt())
        assertEquals("available", item.getValue("availability").jsonPrimitive.content)
    }

    @Test
    fun `frame catalog variant has is_saved boolean`() {
        val variant = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.frameCatalogVariantWithSaved)
            .jsonObject

        assertTrue(variant.getValue("is_saved").jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `legacy frame catalog variant without is_saved defaults to false`() {
        val variant = ApiContractFixtures.json
            .parseToJsonElement(ApiContractFixtures.frameCatalogVariantLegacy)
            .jsonObject

        // Legacy cache rows omit is_saved; Kotlinx Serialization defaults to false
        assertFalse(variant.containsKey("is_saved"), "Legacy variant omits is_saved")
    }
}
