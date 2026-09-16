package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AppointmentBookingBlockingReason
import com.eyecare.app.domain.model.AppointmentBookingEligibility
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.RequestStepMargin

@Composable
internal fun RequestLimitCheckingContent(onBack: () -> Unit) {
    RequestAccessScaffold(
        title = "Request an appointment",
        onBack = onBack,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Checking your appointment requests...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun RequestLimitReachedContent(
    activeRequestCount: Int,
    onViewRequests: () -> Unit,
    onBack: () -> Unit,
) {
    RequestAccessScaffold(
        title = "Request an appointment",
        onBack = onBack,
        bottomBar = {
            AppointmentPrimaryButton(
                text = "View my requests",
                onClick = onViewRequests,
            )
            AppointmentOutlinedButton(
                text = "Back",
                onClick = onBack,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RequestStepMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                text = "You have $activeRequestCount pending appointment requests",
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Please wait for the clinic to respond or cancel one before sending another.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun RequestBookingBlockedContent(
    eligibility: AppointmentBookingEligibility,
    onViewAppointments: () -> Unit,
    onBack: () -> Unit,
) {
    val title = when (eligibility.blockingReason) {
        AppointmentBookingBlockingReason.SCHEDULED_APPOINTMENT,
        AppointmentBookingBlockingReason.CHECKED_IN_APPOINTMENT,
        -> "Active appointment already exists"
        AppointmentBookingBlockingReason.ACTIVE_REQUEST -> "Active request already exists"
        AppointmentBookingBlockingReason.UNKNOWN, null -> "Another request can't be started yet"
    }
    val message = when (eligibility.blockingReason) {
        AppointmentBookingBlockingReason.SCHEDULED_APPOINTMENT ->
            if (eligibility.canRequestRebooking) {
                "You already have a scheduled appointment. You can reschedule it from its appointment details or cancel it before requesting another."
            } else {
                "You already have a scheduled appointment. You may reschedule or cancel it before requesting another."
            }
        AppointmentBookingBlockingReason.CHECKED_IN_APPOINTMENT ->
            "You already have a checked-in appointment. You may cancel it before requesting another."
        AppointmentBookingBlockingReason.ACTIVE_REQUEST ->
            "You already have an active appointment request. Wait for the clinic to respond or cancel it before sending another."
        AppointmentBookingBlockingReason.UNKNOWN, null ->
            "Your account already has an active booking. Review your appointments or requests before starting another."
    }

    RequestAccessScaffold(
        title = "Request an appointment",
        onBack = onBack,
        bottomBar = {
            AppointmentPrimaryButton(
                text = if (eligibility.blockingReason == AppointmentBookingBlockingReason.ACTIVE_REQUEST) {
                    "View my requests"
                } else {
                    "View my appointments"
                },
                onClick = onViewAppointments,
            )
            AppointmentOutlinedButton(
                text = "Back",
                onClick = onBack,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RequestStepMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestAccessScaffold(
    title: String,
    onBack: () -> Unit,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (bottomBar != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RequestStepMargin, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        bottomBar()
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            content = content,
        )
    }
}
