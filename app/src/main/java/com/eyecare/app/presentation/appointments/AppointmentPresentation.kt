package com.eyecare.app.presentation.appointments

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.ui.theme.EyecareColors
import java.util.Locale

data class AppointmentStatusPresentation(
    val label: String,
    val fillColor: Color,
    val textColor: Color,
)

/**
 * The single source of truth for how a confirmed appointment's status renders — shared by
 * the appointment list card and the detail screen so status never silently degrades to a
 * differently-colored badge on one of the two surfaces that show it. Mirrors
 * requestStatusPresentation()'s fill/text split in AppointmentRequestPresentation.kt.
 *
 * CANCELLED and NO_SHOW deliberately share one color (both are terminal negative outcomes),
 * the same way AppointmentRequestStatusPill deliberately groups REJECTED/CANCELLED/EXPIRED —
 * a labeled, intentional pairing rather than the two independent-branches-that-happen-to-match
 * collision this replaces (FULFILLED and UNKNOWN previously shared onSurfaceVariant by accident).
 */
@Composable
fun appointmentStatusPresentation(status: AppointmentStatus): AppointmentStatusPresentation {
    val colors = EyecareColors.current
    return when (status) {
        AppointmentStatus.SCHEDULED -> AppointmentStatusPresentation(
            label = "Scheduled",
            fillColor = colors.statusPending,
            textColor = colors.statusPendingText,
        )
        AppointmentStatus.CHECKED_IN -> AppointmentStatusPresentation(
            label = "Checked in",
            fillColor = colors.statusInfo,
            textColor = colors.statusInfo,
        )
        AppointmentStatus.FULFILLED -> AppointmentStatusPresentation(
            label = "Completed",
            fillColor = colors.statusConfirmed,
            textColor = colors.statusConfirmedText,
        )
        AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW -> AppointmentStatusPresentation(
            label = if (status == AppointmentStatus.NO_SHOW) "No show" else "Cancelled",
            fillColor = colors.statusCancelled,
            textColor = colors.statusCancelledText,
        )
        AppointmentStatus.UNKNOWN -> AppointmentStatusPresentation(
            label = "Unknown",
            fillColor = MaterialTheme.colorScheme.onSurfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Borderless status pill used by both AppointmentListScreen's card and AppointmentDetailScreen's
 * header — previously two separately-maintained implementations (one bordered, one not; one
 * WCAG-failing, one not) that could and did drift apart.
 */
@Composable
fun AppointmentStatusPill(status: AppointmentStatus) {
    val presentation = appointmentStatusPresentation(status)
    Surface(
        shape = RoundedCornerShape(50),
        color = presentation.fillColor.copy(alpha = 0.12f),
    ) {
        Text(
            text = presentation.label.uppercase(Locale.US),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = presentation.textColor,
        )
    }
}
