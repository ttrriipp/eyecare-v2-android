package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppNotificationTest {

    @Test
    fun `notification kind maps every documented clinic event`() {
        val expected = mapOf(
            "appointment_confirmed" to NotificationKind.APPOINTMENT_CONFIRMED,
            "appointment_request_declined" to NotificationKind.APPOINTMENT_REQUEST_DECLINED,
            "appointment_rescheduled" to NotificationKind.APPOINTMENT_RESCHEDULED,
            "appointment_cancelled" to NotificationKind.APPOINTMENT_CANCELLED,
            "prescription_available" to NotificationKind.PRESCRIPTION_AVAILABLE,
            "visit_completed" to NotificationKind.VISIT_COMPLETED,
            "optical_order_confirmed" to NotificationKind.OPTICAL_ORDER_CONFIRMED,
            "optical_order_ready" to NotificationKind.OPTICAL_ORDER_READY,
            "optical_order_cancelled" to NotificationKind.OPTICAL_ORDER_CANCELLED,
            "payment_recorded" to NotificationKind.PAYMENT_RECORDED,
            "payment_updated" to NotificationKind.PAYMENT_UPDATED,
            "optical_order_released" to NotificationKind.OPTICAL_ORDER_RELEASED,
            "new_message" to NotificationKind.NEW_MESSAGE,
        )

        expected.forEach { (raw, kind) ->
            assertEquals(kind, NotificationKind.from(raw), raw)
        }
    }

    @Test
    fun `unknown notification kind remains safe`() {
        assertEquals(NotificationKind.UNKNOWN, NotificationKind.from("future_event"))
        assertEquals(NotificationKind.UNKNOWN, NotificationKind.from(null))
    }

    @Test
    fun `mobile action maps documented destination types`() {
        assertEquals(MobileDestination.APPOINTMENT, MobileDestination.from("appointment"))
        assertEquals(MobileDestination.APPOINTMENT_REQUEST, MobileDestination.from("appointment_request"))
        assertEquals(MobileDestination.PRESCRIPTION, MobileDestination.from("prescription"))
        assertEquals(MobileDestination.OPTICAL_ORDER, MobileDestination.from("optical_order"))
        assertEquals(MobileDestination.CONVERSATION, MobileDestination.from("conversation"))
        assertEquals(MobileDestination.UNKNOWN, MobileDestination.from("future_destination"))
    }

    @Test
    fun `detail destinations require a positive ID while conversation does not`() {
        assertTrue(MobileDestination.CONVERSATION.isActionable(null))
        assertTrue(MobileDestination.APPOINTMENT.isActionable(123))
        assertFalse(MobileDestination.APPOINTMENT.isActionable(null))
        assertFalse(MobileDestination.APPOINTMENT.isActionable(0))
        assertFalse(MobileDestination.UNKNOWN.isActionable(123))
    }
}
