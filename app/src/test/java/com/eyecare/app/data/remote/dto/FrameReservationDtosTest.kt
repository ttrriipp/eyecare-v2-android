package com.eyecare.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Decodes the literal §12 samples from `docs/API_CONTRACT.md` (2026-08-13).
 *
 * The contract replaced the six-state `status` string with a derived `is_held` boolean and
 * stopped sending `status` at all. Before this migration `ReservationDto.status` was a
 * non-null `String` with no default, so every live `GET /frame-reservations` response threw
 * `MissingFieldException` — these tests pin the payload shape that broke it.
 */
class FrameReservationDtosTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `decodes an unheld reservation with no status key`() {
        val payload = """
            {
              "id": 1,
              "appointment_id": 42,
              "is_held": false,
              "expires_at": "2026-07-30T18:00:00+08:00",
              "created_at": "2026-07-27T10:00:00+08:00",
              "appointment": {
                "id": 42,
                "appointment_number": "APT-2026-000042",
                "status": "scheduled",
                "scheduled_at": "2026-07-30T09:00:00+08:00",
                "duration_minutes": 30
              },
              "items": [
                {
                  "id": 1,
                  "product_variant_id": 42,
                  "variant": {
                    "id": 42,
                    "name": "Black / 52mm",
                    "sku": "RB-CR-BLK-52",
                    "price": "4500.00",
                    "compare_at_price": null,
                    "attributes": { "color": "black", "size": "52mm" },
                    "images": [],
                    "product": {
                      "id": 7,
                      "name": "Classic Rectangle",
                      "slug": "classic-rectangle",
                      "description": "Timeless frame design",
                      "product_type": "frame",
                      "brand": "Ray-Ban",
                      "category": "Full Rim"
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<FrameReservationDtos.ReservationDto>(payload)

        assertFalse(dto.isHeld)
        assertEquals("2026-07-30T18:00:00+08:00", dto.expiresAt)
        assertEquals(1, dto.items.size)
        assertEquals(42, dto.items.first().productVariantId)
    }

    @Test
    fun `decodes a held reservation`() {
        val payload = """
            {
              "id": 2,
              "appointment_id": 43,
              "is_held": true,
              "expires_at": "2026-07-31T18:00:00+08:00",
              "created_at": "2026-07-28T10:00:00+08:00",
              "items": []
            }
        """.trimIndent()

        val dto = json.decodeFromString<FrameReservationDtos.ReservationDto>(payload)

        assertTrue(dto.isHeld)
        assertEquals("2026-07-31T18:00:00+08:00", dto.expiresAt)
    }

    @Test
    fun `a missing is_held key fails closed to not-held`() {
        val payload = """
            {
              "id": 3,
              "appointment_id": 44,
              "created_at": "2026-07-28T10:00:00+08:00",
              "items": []
            }
        """.trimIndent()

        val dto = json.decodeFromString<FrameReservationDtos.ReservationDto>(payload)

        assertFalse(dto.isHeld)
    }

    @Test
    fun `an unknown accepted_at key is ignored rather than surfaced`() {
        val payload = """
            {
              "id": 4,
              "appointment_id": 45,
              "is_held": true,
              "accepted_at": "2026-07-29T08:00:00+08:00",
              "created_at": "2026-07-28T10:00:00+08:00",
              "items": []
            }
        """.trimIndent()

        val dto = json.decodeFromString<FrameReservationDtos.ReservationDto>(payload)

        assertTrue(dto.isHeld)
    }
}
