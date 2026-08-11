package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.RequestStepMargin
import com.eyecare.app.presentation.appointments.components.RequestStepScaffold
import com.eyecare.app.ui.theme.EyecareColors

/**
 * The last screen before the request leaves the phone. Every section can be corrected in place
 * rather than by guessing how many times to press Back, and the non-binding nature of the
 * request is restated directly above the action instead of only after it is sent.
 */
@Composable
fun RequestReviewContent(
    state: RequestStep.Review,
    onEdit: (RequestStepId) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    RequestStepScaffold(
        title = "Review your request",
        stepLabels = requestStepLabels(state.identityRequired),
        currentStep = requestStepIndex(RequestStepId.REVIEW, state.identityRequired),
        onBack = if (state.isSubmitting) null else onBack,
        bottomBar = {
            Text(
                text = REQUEST_NON_BINDING_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            AppointmentPrimaryButton(
                text = "Send request to clinic",
                onClick = onSubmit,
                loading = state.isSubmitting,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RequestStepMargin),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReviewCard(
                title = "Appointment",
                onEdit = { onEdit(RequestStepId.SCHEDULE) },
                editLabel = "Change appointment time",
                enabled = !state.isSubmitting,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ReviewMetadataRow(
                            icon = Icons.Outlined.EventAvailable,
                            label = "Type",
                            value = "${state.selectedType.name} · " +
                                "${state.selectedType.durationMinutes} min",
                        )
                        ReviewMetadataRow(
                            icon = Icons.Outlined.CalendarMonth,
                            label = "Preferred day",
                            value = "${formatRequestWeekday(state.date)}, " +
                                formatRequestDate(state.date),
                        )
                        ReviewMetadataRow(
                            icon = Icons.Outlined.AccessTime,
                            label = "Preferred time",
                            value = formatTimeRange(
                                state.primarySlot.startsAt,
                                state.primarySlot.endsAt,
                            ),
                        )
                    }
                }
                if (state.alternativeSlots.isEmpty()) {
                    Text(
                        text = "No backup times. The clinic will work with your preferred time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Backup times",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        state.alternativeSlots.forEachIndexed { index, slot ->
                            ReviewRow(
                                label = "Backup ${index + 1}",
                                value = formatSlotWithDay(slot),
                            )
                        }
                    }
                }
            }

            ReviewCard(
                title = "Reason for visit",
                onEdit = { onEdit(RequestStepId.REASON) },
                editLabel = "Change reason for visit",
                enabled = !state.isSubmitting,
            ) {
                Text(
                    text = state.reason,
                    style = MaterialTheme.typography.bodyLarge,
                )
                state.referringSource?.takeIf { it.isNotBlank() }?.let { referral ->
                    ReviewRow(label = "Referred by", value = referral)
                }
            }

            state.identity?.let { identity ->
                ReviewCard(
                    title = "Your details",
                    onEdit = { onEdit(RequestStepId.IDENTITY) },
                    editLabel = "Change your details",
                    enabled = !state.isSubmitting,
                ) {
                    ReviewRow(
                        label = "Name",
                        value = listOfNotNull(
                            identity.firstName,
                            identity.middleName,
                            identity.lastName,
                        ).joinToString(" "),
                    )
                    ReviewRow(
                        label = "Date of birth",
                        value = identity.dateOfBirth?.let { formatRequestDate(it) }.orEmpty(),
                    )
                    ReviewRow(label = "Phone", value = identity.phone.orEmpty())
                    identity.email?.takeIf(String::isNotBlank)?.let {
                        ReviewRow(label = "Email", value = it)
                    }
                    ReviewRow(label = "Gender", value = identity.gender?.label.orEmpty())
                    ReviewRow(label = "Occupation", value = identity.occupation.orEmpty())
                    ReviewRow(label = "Home address", value = identity.address.orEmpty())
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Shown when the request could not be sent. The action offered depends on whether anything in
 * this flow can actually change the outcome — retrying a request limit the patient cannot clear
 * from here would only loop.
 */
@Composable
fun RequestSubmissionErrorContent(
    state: RequestStep.SubmissionError,
    onRecover: () -> Unit,
    onBackToReview: () -> Unit,
    onLeave: () -> Unit,
) {
    RequestStepScaffold(
        title = "Request not sent",
        stepLabels = requestStepLabels(state.identityRequired),
        currentStep = requestStepIndex(RequestStepId.REVIEW, state.identityRequired),
        onBack = onBackToReview,
        bottomBar = {
            if (state.canRetry) {
                AppointmentPrimaryButton(text = "Try again", onClick = onRecover)
                AppointmentOutlinedButton(text = "Back to review", onClick = onBackToReview)
            } else {
                AppointmentPrimaryButton(text = "View my requests", onClick = onLeave)
                AppointmentOutlinedButton(text = "Back to review", onClick = onBackToReview)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RequestStepMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = "We couldn't send your request",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            Text(
                text = "Nothing has been sent yet, and your answers are saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReviewCard(
    title: String,
    onEdit: () -> Unit,
    editLabel: String,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onEdit, enabled = enabled) {
                    Text(
                        text = "Edit",
                        color = EyecareColors.current.accentText,
                        modifier = Modifier.semantics { },
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ReviewMetadataRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EyecareColors.current.accentText,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Shared so the same promise appears on the review screen and the success screen. */
internal const val REQUEST_NON_BINDING_NOTE =
    "The clinic reviews every request. No time is held until they confirm it."

@Composable
internal fun NonBindingNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = EyecareColors.current.accentText,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = REQUEST_NON_BINDING_NOTE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
