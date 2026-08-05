package com.eyecare.app.presentation.appointments.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.ui.theme.EyecareColors

/**
 * A visual step indicator for multi-step wizards.
 *
 * Shows numbered circles connected by lines, with the current step highlighted
 * and completed steps showing a checkmark. Labels appear below each circle.
 *
 * @param currentStep Zero-indexed current step
 * @param steps List of step labels
 */
@Composable
fun WizardStepIndicator(
    currentStep: Int,
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, label ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep
            val isUpcoming = index > currentStep

            StepCircle(
                stepNumber = index + 1,
                label = label,
                isCompleted = isCompleted,
                isCurrent = isCurrent,
                isUpcoming = isUpcoming,
            )

            // Connector line between steps (not after the last step)
            if (index < steps.lastIndex) {
                StepConnector(
                    isCompleted = isCompleted,
                    modifier = Modifier.weight(1f).padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun StepCircle(
    stepNumber: Int,
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isUpcoming: Boolean,
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> EyecareColors.current.statusConfirmed
            isCurrent -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "step-bg",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.onTertiary
            isCurrent -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "step-content",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    text = "$stepNumber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun StepConnector(
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (isCompleted) EyecareColors.current.statusConfirmed
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(300),
        label = "connector-color",
    )

    Box(
        modifier = modifier
            .height(2.dp)
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .background(color),
    )
}
