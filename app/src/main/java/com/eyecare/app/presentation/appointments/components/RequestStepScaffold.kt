package com.eyecare.app.presentation.appointments.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The single horizontal margin every step of the request wizard uses (DESIGN.md task-dense flows). */
val RequestStepMargin = 16.dp

/**
 * The frame every step of the request wizard sits in, so insets, the step indicator's position,
 * and the pinned action bar can only be right or wrong in one place.
 *
 * `imePadding` is applied to the whole scaffold rather than the scrolling content: the app runs
 * edge-to-edge, so the system does not resize the window for the keyboard and a form would
 * otherwise scroll inside a viewport the keyboard is covering. Putting the action in [bottomBar]
 * (instead of a `Box`-aligned surface over the content) is what keeps the navigation-bar inset
 * from being counted twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestStepScaffold(
    title: String,
    stepLabels: List<String>,
    currentStep: Int,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back",
                            )
                        }
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            WizardStepIndicator(
                currentStep = currentStep,
                steps = stepLabels,
                modifier = Modifier.padding(
                    horizontal = RequestStepMargin,
                    vertical = 16.dp,
                ),
            )
            // The content gets its own weighted slot. Without it a `fillMaxSize` child would be
            // measured against the scaffold's full height rather than what the indicator left,
            // and overflow the bottom of every step by the indicator's height.
            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                content()
            }
        }
    }
}
