package com.eyecare.app.presentation.appointments.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.ui.theme.EyecareColors

/**
 * A visual step indicator for multi-step wizards.
 *
 * Steps share the available width rather than claiming a fixed column each, so the same
 * component reads correctly whether a flow has four steps or five. The whole row announces
 * itself to TalkBack as one phrase ("Step 3 of 5, Reason") instead of emitting a separate
 * node per circle.
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
    val currentLabel = steps.getOrNull(currentStep)
    val announcement = if (currentLabel != null) {
        "Step ${currentStep + 1} of ${steps.size}, $currentLabel"
    } else {
        "Step ${currentStep + 1} of ${steps.size}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = announcement },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, label ->
            StepCircle(
                stepNumber = index + 1,
                label = label,
                isCompleted = index < currentStep,
                isCurrent = index == currentStep,
                modifier = Modifier.weight(1f),
            )

            // Connector line between steps (not after the last step)
            if (index < steps.lastIndex) {
                StepConnector(
                    isCompleted = index < currentStep,
                    modifier = Modifier.weight(0.5f).padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun RowScope.StepCircle(
    stepNumber: Int,
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
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
                    contentDescription = null,
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
        Spacer(modifier = Modifier.height(8.dp))
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
