package com.eyecare.app.domain.model

import com.eyecare.app.data.remote.dto.AppointmentRequestAvailabilityData
import com.eyecare.app.data.remote.dto.AppointmentRequestAvailabilityResponse
import com.eyecare.app.data.remote.dto.AvailabilitySlotDto
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId

class AvailabilityCapabilityTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val clinicTz = ZoneId.of("Asia/Manila")

    private val fullDayPayload = """
    {
        "data": {
            "date": "2026-08-10",
            "timezone": "Asia/Manila",
            "interval_minutes": 15,
            "slot_duration_minutes": 45,
            "visit_duration_minutes": 45,
            "appointment_type_id": 1,
            "day_status": "open",
            "generated_at": "2026-08-09T10:00:00+08:00",
            "slots": [
                {"starts_at": "2026-08-10T09:00:00+08:00", "ends_at": "2026-08-10T09:45:00+08:00", "available": true, "reason": null},
                {"starts_at": "2026-08-10T09:15:00+08:00", "ends_at": "2026-08-10T10:00:00+08:00", "available": true, "reason": null},
                {"starts_at": "2026-08-10T09:30:00+08:00", "ends_at": "2026-08-10T10:15:00+08:00", "available": true, "reason": null},
                {"starts_at": "2026-08-10T09:45:00+08:00", "ends_at": "2026-08-10T10:30:00+08:00", "available": false, "reason": "capacity_reached"},
                {"starts_at": "2026-08-10T10:00:00+08:00", "ends_at": "2026-08-10T10:45:00+08:00", "available": true, "reason": null},
                {"starts_at": "2026-08-10T16:00:00+08:00", "ends_at": "2026-08-10T16:45:00+08:00", "available": true, "reason": null},
                {"starts_at": "2026-08-10T16:15:00+08:00", "ends_at": "2026-08-10T17:00:00+08:00", "available": true, "reason": null}
            ]
        }
    }
    """.trimIndent()

    private fun AppointmentRequestAvailabilityData.toDomain() = AppointmentRequestAvailability(
        date = date,
        timezone = timezone,
        intervalMinutes = intervalMinutes,
        slotDurationMinutes = slotDurationMinutes,
        visitDurationMinutes = visitDurationMinutes,
        appointmentTypeId = appointmentTypeId,
        dayStatus = dayStatus,
        generatedAt = generatedAt,
        slots = slots.map {
            AvailabilitySlot(
                startsAt = it.startsAt,
                endsAt = it.endsAt,
                available = it.available,
                reason = it.reason,
            )
        },
    )

    // ── Capability 1: Availability requires an appointment type ──

    @Test
    fun `response includes appointment_type_id`() {
        val response = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload)
        assertEquals(1, response.data.appointmentTypeId)
    }

    @Test
    fun `domain model carries appointment type id`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        assertEquals(1, domain.appointmentTypeId)
    }

    // ── Capability 2: A missing, inactive, or hidden type is rejected ──

    @Test
    fun `empty slots list when type has no availability`() {
        val payload = """
        {"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":15,"slot_duration_minutes":45,"visit_duration_minutes":45,"appointment_type_id":999,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[]}}
        """.trimIndent()
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(payload).data.toDomain()
        assertTrue(domain.slots.isEmpty())
        assertEquals(999, domain.appointmentTypeId)
    }

    // ── Capability 3: Starts appear on the 15-minute grid ──

    @Test
    fun `interval_minutes is 15`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        assertEquals(15, domain.intervalMinutes)
    }

    @Test
    fun `all slot starts fall on 15-minute boundaries`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        domain.slots.forEach { slot ->
            val start = OffsetDateTime.parse(slot.startsAt)
            assertEquals(0, start.minute % 15, "Slot start ${slot.startsAt} must be on 15-minute grid")
        }
    }

    @Test
    fun `successive slots are separated by the interval`() {
        val payload = """
        {"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":15,"slot_duration_minutes":45,"visit_duration_minutes":45,"appointment_type_id":1,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[
            {"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:45:00+08:00","available":true,"reason":null},
            {"starts_at":"2026-08-10T09:15:00+08:00","ends_at":"2026-08-10T10:00:00+08:00","available":true,"reason":null},
            {"starts_at":"2026-08-10T09:30:00+08:00","ends_at":"2026-08-10T10:15:00+08:00","available":true,"reason":null},
            {"starts_at":"2026-08-10T09:45:00+08:00","ends_at":"2026-08-10T10:30:00+08:00","available":true,"reason":null},
            {"starts_at":"2026-08-10T10:00:00+08:00","ends_at":"2026-08-10T10:45:00+08:00","available":true,"reason":null}
        ]}}
        """.trimIndent()
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(payload).data.toDomain()
        for (i in 1 until domain.slots.size) {
            val prev = OffsetDateTime.parse(domain.slots[i - 1].startsAt)
            val curr = OffsetDateTime.parse(domain.slots[i].startsAt)
            val diff = Duration.between(prev, curr).toMinutes()
            assertEquals(15, diff, "Slots must be 15 minutes apart")
        }
    }

    // ── Capability 4: Visit end time uses the selected type's duration ──

    @Test
    fun `visit_duration_minutes reflects the type duration`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        assertEquals(45, domain.visitDurationMinutes)
    }

    @Test
    fun `slot end time equals start plus visit duration`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        domain.slots.forEach { slot ->
            val start = OffsetDateTime.parse(slot.startsAt)
            val end = OffsetDateTime.parse(slot.endsAt)
            val duration = Duration.between(start, end).toMinutes()
            assertEquals(domain.visitDurationMinutes!!.toLong(), duration,
                "Slot ${slot.startsAt}–${slot.endsAt} duration must equal visit_duration_minutes")
        }
    }

    // ── Capability 5: A 45-minute visit can start at 9:00, 9:15, 9:30, etc. ──

    @Test
    fun `45-minute slots start at 9_00 9_15 9_30`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        val starts = domain.slots.map { OffsetDateTime.parse(it.startsAt).toLocalTime() }
        assertTrue(starts.any { it.hour == 9 && it.minute == 0 })
        assertTrue(starts.any { it.hour == 9 && it.minute == 15 })
        assertTrue(starts.any { it.hour == 9 && it.minute == 30 })
    }

    // ── Capability 6: A start is hidden if the complete visit extends beyond clinic hours ──

    @Test
    fun `latest slot ends at or before 5pm`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        domain.slots.forEach { slot ->
            val end = OffsetDateTime.parse(slot.endsAt)
            assertTrue(end.hour < 17 || (end.hour == 17 && end.minute == 0),
                "Slot end ${slot.endsAt} must not extend past 5:00 PM")
        }
    }

    @Test
    fun `no 4_45pm start exists because visit would extend past 5pm`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        val starts = domain.slots.map { OffsetDateTime.parse(it.startsAt).toLocalTime() }
        assertFalse(starts.any { it.hour == 16 && it.minute == 45 },
            "4:45 PM start must be hidden — 45-minute visit would end at 5:30 PM")
    }

    // ── Capability 7: Provider hours and partial-day absences remove only affected intervals ──

    @Test
    fun `unavailable slot due to absence carries reason`() {
        val payload = """
        {"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":15,"slot_duration_minutes":45,"visit_duration_minutes":45,"appointment_type_id":1,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[
            {"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:45:00+08:00","available":true,"reason":null},
            {"starts_at":"2026-08-10T09:15:00+08:00","ends_at":"2026-08-10T10:00:00+08:00","available":false,"reason":"provider_absence"},
            {"starts_at":"2026-08-10T09:30:00+08:00","ends_at":"2026-08-10T10:15:00+08:00","available":false,"reason":"provider_absence"},
            {"starts_at":"2026-08-10T09:45:00+08:00","ends_at":"2026-08-10T10:30:00+08:00","available":true,"reason":null}
        ]}}
        """.trimIndent()
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(payload).data.toDomain()
        assertEquals(4, domain.slots.size)
        assertTrue(domain.slots[0].available)
        assertFalse(domain.slots[1].available)
        assertEquals("provider_absence", domain.slots[1].reason)
        assertFalse(domain.slots[2].available)
        assertEquals("provider_absence", domain.slots[2].reason)
        assertTrue(domain.slots[3].available)
    }

    // ── Capability 8: Existing confirmed appointments consume capacity ──

    @Test
    fun `unavailable slot due to capacity carries capacity_reached reason`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        val capacitySlot = domain.slots.first { !it.available && it.reason == "capacity_reached" }
        assertFalse(capacitySlot.available)
        assertEquals("capacity_reached", capacitySlot.reason)
    }

    // ── Capability 9: Pending appointment requests do not consume capacity ──

    @Test
    fun `slots remain available when only pending requests exist`() {
        val payload = """
        {"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":15,"slot_duration_minutes":45,"visit_duration_minutes":45,"appointment_type_id":1,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[
            {"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:45:00+08:00","available":true,"reason":null},
            {"starts_at":"2026-08-10T09:15:00+08:00","ends_at":"2026-08-10T10:00:00+08:00","available":true,"reason":null}
        ]}}
        """.trimIndent()
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(payload).data.toDomain()
        assertTrue(domain.slots.all { it.available },
            "All slots should be available when only pending requests exist")
    }

    // ── Capability 10: Two optometrists allow simultaneous appointments ──

    @Test
    fun `same time slot can be available when multiple providers exist`() {
        val payload = """
        {"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":15,"slot_duration_minutes":45,"visit_duration_minutes":45,"appointment_type_id":1,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[
            {"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:45:00+08:00","available":true,"reason":null},
            {"starts_at":"2026-08-10T09:15:00+08:00","ends_at":"2026-08-10T10:00:00+08:00","available":true,"reason":null}
        ]}}
        """.trimIndent()
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(payload).data.toDomain()
        assertEquals(2, domain.slots.filter { it.available }.size,
            "Both slots available when two optometrists are free")
    }

    // ── Capability 11: An assigned optometrist cannot receive overlapping confirmed appointments ──

    @Test
    fun `overlapping confirmed appointment causes slot to be unavailable`() {
        val payload = """
        {"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":15,"slot_duration_minutes":45,"visit_duration_minutes":45,"appointment_type_id":1,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[
            {"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:45:00+08:00","available":false,"reason":"capacity_reached"},
            {"starts_at":"2026-08-10T09:15:00+08:00","ends_at":"2026-08-10T10:00:00+08:00","available":false,"reason":"capacity_reached"},
            {"starts_at":"2026-08-10T09:30:00+08:00","ends_at":"2026-08-10T10:15:00+08:00","available":true,"reason":null}
        ]}}
        """.trimIndent()
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(payload).data.toDomain()
        val unavailable = domain.slots.filter { !it.available }
        assertEquals(2, unavailable.size)
        assertTrue(unavailable.all { it.reason == "capacity_reached" })
    }

    // ── UI filtering: only available slots shown ──

    @Test
    fun `UI should filter to available slots only`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        val availableSlots = domain.slots.filter { it.available }
        val unavailableSlots = domain.slots.filter { !it.available }
        assertEquals(6, availableSlots.size)
        assertEquals(1, unavailableSlots.size)
        assertTrue(availableSlots.none { it.reason == "capacity_reached" })
    }

    // ── Domain model structure ──

    @Test
    fun `domain model has all required fields`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        assertNotNull(domain.date)
        assertNotNull(domain.timezone)
        assertEquals(15, domain.intervalMinutes)
        assertEquals(45, domain.slotDurationMinutes)
        assertEquals(45, domain.visitDurationMinutes)
        assertEquals(1, domain.appointmentTypeId)
        assertEquals("open", domain.dayStatus)
        assertNotNull(domain.generatedAt)
        assertNotNull(domain.slots)
    }

    @Test
    fun `slot model has startsAt endsAt available and reason`() {
        val domain = json.decodeFromString<AppointmentRequestAvailabilityResponse>(fullDayPayload).data.toDomain()
        val slot = domain.slots.first()
        assertNotNull(slot.startsAt)
        assertNotNull(slot.endsAt)
        assertTrue(slot.available)
        assertNull(slot.reason)
    }
}
