package com.eyecare.app.presentation.appointments.requests

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.RequestStepMargin
import com.eyecare.app.presentation.appointments.components.RequestStepScaffold
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun RequestAppointmentScreen(
    onBack: () -> Unit,
    onRequestCreated: (requestId: Int) -> Unit,
    onViewRequests: () -> Unit = onBack,
    requestIdentity: AppointmentRequestIdentity? = null,
    identityDetailsRequired: Boolean = false,
    isFrameReservationOrigin: Boolean = false,
    viewModel: RequestAppointmentViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsState()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    /**
     * One place decides what "back" means, so the system gesture and the top-bar arrow can never
     * disagree. Steps holding typed work ask before discarding it.
     */
    val goBack: () -> Unit = {
        when (val current = step) {
            is RequestStep.Type -> onBack()
            is RequestStep.Schedule ->
                if (current.phase == SchedulePhase.ALTERNATIVES) {
                    viewModel.finishAddingAlternatives()
                } else {
                    viewModel.backToType()
                }
            is RequestStep.Reason ->
                if (current.hasUnsavedInput) showDiscardDialog = true else viewModel.backToSchedule()
            is RequestStep.Identity ->
                if (current.hasUnsavedInput) showDiscardDialog = true else viewModel.backToReason()
            is RequestStep.Review -> if (!current.isSubmitting) viewModel.backFromReview()
            is RequestStep.SubmissionError -> viewModel.backFromSubmissionError()
            is RequestStep.Success -> onRequestCreated(current.request.id)
        }
    }

    // Submitting must not be interruptible by Back; everything else routes through goBack.
    val submitting = (step as? RequestStep.Review)?.isSubmitting == true
    BackHandler(enabled = !submitting) { goBack() }

    when (val s = step) {
        is RequestStep.Type -> TypeContent(
            state = s,
            identityRequired = identityDetailsRequired,
            onSelectType = viewModel::selectType,
            onConfirm = {
                viewModel.confirmType(
                    identityRequired = identityDetailsRequired,
                    initialIdentity = requestIdentity,
                )
            },
            onRetry = viewModel::retryTypes,
            onBack = onBack,
        )

        is RequestStep.Schedule -> ScheduleContent(
            state = s,
            onShowWeek = viewModel::showWeek,
            onDateSelected = viewModel::selectDate,
            onSelectSlot = viewModel::selectPrimarySlot,
            onStartAddingAlternatives = viewModel::startAddingAlternatives,
            onFinishAddingAlternatives = viewModel::finishAddingAlternatives,
            onToggleAlternative = viewModel::toggleAlternative,
            onRemoveAlternative = viewModel::removeAlternative,
            onRetry = viewModel::retryAvailability,
            onConfirm = viewModel::confirmSchedule,
            onBack = goBack,
        )

        is RequestStep.Reason -> RequestReasonContent(
            state = s,
            onReasonChange = viewModel::updateReason,
            onReferringSourceChange = viewModel::updateReferringSource,
            onConfirm = { viewModel.confirmReason(initialIdentity = requestIdentity) },
            onBack = goBack,
        )

        is RequestStep.Identity -> RequestIdentityContent(
            state = s,
            onEmailChange = { viewModel.updateIdentity(email = it) },
            onFirstNameChange = { viewModel.updateIdentity(firstName = it) },
            onMiddleNameChange = { viewModel.updateIdentity(middleName = it) },
            onLastNameChange = { viewModel.updateIdentity(lastName = it) },
            onDateOfBirthChange = { viewModel.updateIdentity(dateOfBirth = it) },
            onGenderChange = { viewModel.updateIdentity(gender = it) },
            onOccupationChange = { viewModel.updateIdentity(occupation = it) },
            onAddressChange = { viewModel.updateIdentity(address = it) },
            onFocusHandled = viewModel::consumeIdentityFocus,
            onConfirm = viewModel::confirmIdentity,
            onBack = goBack,
        )

        is RequestStep.Review -> RequestReviewContent(
            state = s,
            onEdit = viewModel::editFromReview,
            onSubmit = { viewModel.submit(isFrameReservationOrigin) },
            onBack = goBack,
        )

        is RequestStep.SubmissionError -> RequestSubmissionErrorContent(
            state = s,
            onRecover = viewModel::handleSubmissionError,
            onBackToReview = viewModel::backFromSubmissionError,
            onLeave = onViewRequests,
        )

        is RequestStep.Success -> RequestSuccessContent(
            state = s,
            onContinue = { onRequestCreated(s.request.id) },
        )
    }

    if (showDiscardDialog) {
        AppConfirmationDialog(
            icon = Icons.AutoMirrored.Outlined.ExitToApp,
            title = "Discard this request?",
            message = "You haven't sent this to the clinic yet. " +
                "Going back now will clear what you've entered on this step.",
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardDialog = false
                when (step) {
                    is RequestStep.Reason -> viewModel.backToSchedule()
                    is RequestStep.Identity -> viewModel.backToReason()
                    else -> Unit
                }
            },
            onDismissRequest = { showDiscardDialog = false },
            dismissLabel = "Keep editing",
            isDestructive = true,
        )
    }
}

// -------------------------------------------------------------------- type step

@Composable
private fun TypeContent(
    state: RequestStep.Type,
    identityRequired: Boolean,
    onSelectType: (AppointmentType) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    RequestStepScaffold(
        title = "Request an appointment",
        stepLabels = requestStepLabels(identityRequired),
        currentStep = requestStepIndex(RequestStepId.TYPE, identityRequired),
        onBack = onBack,
        bottomBar = {
            AppointmentPrimaryButton(
                text = "Continue",
                onClick = onConfirm,
                enabled = state.selectedType != null,
            )
            if (state.selectedType == null && state.types.isNotEmpty()) {
                Text(
                    text = "Choose a visit type to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) {
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.fillMaxSize())
            state.error != null -> ErrorContent(
                message = state.error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RequestStepMargin),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NonBindingNotice()

                state.notice?.let { notice ->
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodyMedium,
                        // `primary` fails contrast as text on a light surface; this is the
                        // deeper same-hue token DESIGN.md reserves for cyan text.
                        color = EyecareColors.current.accentText,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                Text(
                    text = "What do you need to be seen for?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )

                if (state.types.isEmpty()) {
                    Text(
                        text = "The clinic isn't taking requests for any visit type right now. " +
                            "Please try again later or message the clinic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.types.forEach { type ->
                    AppointmentTypeCard(
                        type = type,
                        isSelected = type.id == state.selectedType?.id,
                        onSelect = { onSelectType(type) },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AppointmentTypeCard(
    type: AppointmentType,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        // Every card carries a border, so an unselected option still reads as an option.
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = type.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${type.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                type.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (type.requiresReferral) {
                    Text(
                        text = "Needs a referral",
                        style = MaterialTheme.typography.labelMedium,
                        color = EyecareColors.current.accentText,
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------- success step

@Composable
private fun RequestSuccessContent(
    state: RequestStep.Success,
    onContinue: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RequestStepMargin, vertical = 12.dp),
                ) {
                    AppointmentPrimaryButton(text = "View my request", onClick = onContinue)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        EyecareColors.current.statusConfirmed.copy(alpha = 0.12f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.EventAvailable,
                    contentDescription = null,
                    tint = EyecareColors.current.statusConfirmed,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = "Request sent",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                text = "Request ${state.request.requestNumber} is with the clinic now.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            NonBindingNotice()
            Text(
                text = "We'll notify you as soon as they confirm.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
