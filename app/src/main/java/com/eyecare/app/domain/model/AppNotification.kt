package com.eyecare.app.domain.model

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val mobileAction: MobileDestination?,
    val mobileActionId: Int? = null,
    val readAt: String?,
    val createdAt: String,
)

enum class NotificationKind {
    APPOINTMENT_CONFIRMED,
    APPOINTMENT_REQUEST_DECLINED,
    APPOINTMENT_RESCHEDULED,
    APPOINTMENT_CANCELLED,
    PRESCRIPTION_AVAILABLE,
    VISIT_COMPLETED,
    OPTICAL_ORDER_CONFIRMED,
    OPTICAL_ORDER_READY,
    OPTICAL_ORDER_CANCELLED,
    PAYMENT_RECORDED,
    PAYMENT_UPDATED,
    OPTICAL_ORDER_RELEASED,
    NEW_MESSAGE,
    UNKNOWN;

    companion object {
        fun from(value: String?): NotificationKind = when (value) {
            "appointment_confirmed" -> APPOINTMENT_CONFIRMED
            "appointment_request_declined" -> APPOINTMENT_REQUEST_DECLINED
            "appointment_rescheduled" -> APPOINTMENT_RESCHEDULED
            "appointment_cancelled" -> APPOINTMENT_CANCELLED
            "prescription_available" -> PRESCRIPTION_AVAILABLE
            "visit_completed" -> VISIT_COMPLETED
            "optical_order_confirmed" -> OPTICAL_ORDER_CONFIRMED
            "optical_order_ready" -> OPTICAL_ORDER_READY
            "optical_order_cancelled" -> OPTICAL_ORDER_CANCELLED
            "payment_recorded" -> PAYMENT_RECORDED
            "payment_updated" -> PAYMENT_UPDATED
            "optical_order_released" -> OPTICAL_ORDER_RELEASED
            "new_message" -> NEW_MESSAGE
            else -> UNKNOWN
        }
    }
}

enum class MobileDestination {
    APPOINTMENT,
    APPOINTMENT_REQUEST,
    PRESCRIPTION,
    OPTICAL_ORDER,
    CONVERSATION,
    UNKNOWN;

    companion object {
        fun from(value: String?): MobileDestination = when (value) {
            "appointment" -> APPOINTMENT
            "appointment_request" -> APPOINTMENT_REQUEST
            "prescription" -> PRESCRIPTION
            "optical_order" -> OPTICAL_ORDER
            "conversation" -> CONVERSATION
            else -> UNKNOWN
        }
    }

    fun isActionable(id: Int?): Boolean = when (this) {
        APPOINTMENT,
        APPOINTMENT_REQUEST,
        PRESCRIPTION,
        OPTICAL_ORDER,
        -> id != null && id > 0
        CONVERSATION -> true
        UNKNOWN -> false
    }
}

data class NotificationPage(
    val notifications: List<AppNotification>,
    val currentPage: Int,
    val lastPage: Int,
    val total: Int,
)
