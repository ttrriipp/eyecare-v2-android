package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.ui.theme.EyecareColors
import java.util.Locale

data class RequestStatusPresentation(
    val label: String,
    val description: String,
    val showCancel: Boolean = false,
    val showViewConfirmed: Boolean = false,
)

/**
 * The single source of truth for how a request status renders as a pill — shared by
 * the appointment list card and the request detail screen so status never silently
 * degrades to plain text on one of the two surfaces that show it.
 */
@Composable
fun AppointmentRequestStatusPill(status: AppointmentRequestStatus, label: String) {
    val colors = EyecareColors.current
    val color = when (status) {
        AppointmentRequestStatus.PENDING -> colors.statusPending
        AppointmentRequestStatus.ACCEPTED -> colors.statusConfirmed
        AppointmentRequestStatus.REJECTED,
        AppointmentRequestStatus.CANCELLED,
        AppointmentRequestStatus.EXPIRED -> colors.statusCancelled
        AppointmentRequestStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            text = label.uppercase(Locale.US),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

fun requestStatusPresentation(status: AppointmentRequestStatus): RequestStatusPresentation = when (status) {
    AppointmentRequestStatus.PENDING -> RequestStatusPresentation(
        label = "Awaiting clinic review",
        description = "The requested time is held while staff review it.",
        showCancel = true,
    )
    AppointmentRequestStatus.ACCEPTED -> RequestStatusPresentation(
        label = "Confirmed",
        description = "Staff accepted the request and created an appointment.",
        showViewConfirmed = true,
    )
    AppointmentRequestStatus.REJECTED -> RequestStatusPresentation(
        label = "Not approved",
        description = "The clinic could not approve this request.",
    )
    AppointmentRequestStatus.CANCELLED -> RequestStatusPresentation(
        label = "Cancelled",
        description = "The patient cancelled the request.",
    )
    AppointmentRequestStatus.EXPIRED -> RequestStatusPresentation(
        label = "Expired",
        description = "The request was not resolved before its hold expired.",
    )
    AppointmentRequestStatus.UNKNOWN -> RequestStatusPresentation(
        label = "Status unavailable",
        description = "The request status could not be determined.",
    )
}
