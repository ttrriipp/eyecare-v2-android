package com.eyecare.app.presentation.appointments.requests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
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
 * rather than by guessing how many times to press Back. The non-binding nature of the request is
 * explained once in the notice so the pinned action area can stay focused on submission.
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
            AppointmentPrimaryButton(
                text = "Send request",
                onClick = onSubmit,
                loading = state.isSubmitting,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RequestStepMargin),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NonBindingNotice()
                ReviewCard(
                title = "Appointment",
                onEdit = { onEdit(RequestStepId.SCHEDULE) },
                editLabel = "Change appointment time",
                enabled = !state.isSubmitting,
            ) {
                // The time is the single fact the patient is here to confirm, so it gets the
                // largest role on the screen instead of being one of three equal-weight rows.
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
                            label = "Visit type",
                            value = "${state.selectedType.name} · " +
                                "${state.selectedType.durationMinutes} min",
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = EyecareColors.current.accentText,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = "Preferred time: " +
                                        "${formatRequestWeekday(state.date)}, " +
                                        "${formatRequestDate(state.date)}, " +
                                        formatTimeRange(
                                            state.primarySlot.startsAt,
                                            state.primarySlot.endsAt,
                                        )
                                },
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "Preferred time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${formatRequestWeekday(state.date)}, " +
                                        formatRequestDate(state.date),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = formatTimeRange(
                                        state.primarySlot.startsAt,
                                        state.primarySlot.endsAt,
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                if (state.alternativeSlots.isEmpty()) {
                    Text(
                        text = "No alternative times selected. The clinic will confirm your preferred time or contact you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Alternative times",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.alternativeSlots.forEachIndexed { index, slot ->
                        val rank = "Alternative ${index + 1}"
                        val value = formatSlotWithDay(slot)
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "$rank: $value"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RankPill(text = rank, isPreferred = false)
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
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
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.referringSource?.takeIf { it.isNotBlank() }?.let { referral ->
                    ReviewRow(label = "Referred by", value = referral)
                }
            }

            state.identity?.let { identity ->
                val hasSecondaryDetails = listOfNotNull(
                    identity.gender,
                    identity.occupation?.takeIf(String::isNotBlank),
                    identity.address?.takeIf(String::isNotBlank),
                ).isNotEmpty()
                val detailsExpanded = remember { mutableStateOf(false) }

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
                    identity.dateOfBirth?.let {
                        ReviewRow(label = "Date of birth", value = formatRequestDate(it))
                    }
                    identity.phone?.takeIf(String::isNotBlank)?.let {
                        ReviewRow(label = "Phone", value = it)
                    }
                    identity.email?.takeIf(String::isNotBlank)?.let {
                        ReviewRow(label = "Email", value = it)
                    }
                    if (hasSecondaryDetails) {
                        AnimatedVisibility(visible = detailsExpanded.value) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                identity.gender?.let {
                                    ReviewRow(label = "Gender", value = it.label)
                                }
                                identity.occupation?.takeIf(String::isNotBlank)?.let {
                                    ReviewRow(label = "Occupation", value = it)
                                }
                                identity.address?.takeIf(String::isNotBlank)?.let {
                                    ReviewRow(label = "Home address", value = it)
                                }
                            }
                        }
                        TextButton(
                            onClick = { detailsExpanded.value = !detailsExpanded.value },
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = if (detailsExpanded.value) {
                                    "Collapse additional details"
                                } else {
                                    "Expand additional details"
                                }
                            },
                        ) {
                            Text(
                                text = if (detailsExpanded.value) "Less details" else "More details",
                                color = EyecareColors.current.accentText,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            }
            if (state.isSubmitting) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Sending your request to the clinic\u2026",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
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

/**
 * The app's standard card chrome — white surface, 16dp radius, one-dp `outlineVariant` border —
 * so Review reads as the same material as every other card in the app rather than as an
 * elevated variant of it. The inner ticket surface stays at 12dp to keep the nested radii paired.
 */
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                // Every card's action reads as "Edit" on screen; `editLabel` is what makes the
                // three of them distinguishable to a screen reader.
                TextButton(
                    onClick = onEdit,
                    enabled = enabled,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = editLabel
                        role = Role.Button
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = EyecareColors.current.accentText,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Edit", color = EyecareColors.current.accentText)
                }
            }
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
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

/**
 * Label left, value right. Seven stacked label-above-value pairs made the details card taller
 * than the screen; paired columns halve that and let the eye run down one column of answers.
 */
@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "$label: $value"
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f),
        )
    }
}

/** Shared so the same promise appears on the review screen and the success screen. */
internal const val REQUEST_NON_BINDING_NOTE =
    "Request only — your appointment is not booked until the clinic confirms."

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
