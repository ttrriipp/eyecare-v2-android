package com.eyecare.app.presentation.reservations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.presentation.common.components.ErrorContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFrameReservationScreen(
    frameId: Int,
    variantId: Int,
    onBack: () -> Unit,
    onSuccess: (reservationId: Int) -> Unit,
) {
    val viewModel = hiltViewModel<CreateFrameReservationViewModel, CreateFrameReservationViewModel.Factory> {
        it.create(frameId, variantId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is CreateReservationUiState.Success -> {
            onSuccess(state.reservation.id)
            return
        }
        else -> {}
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Reserve Frame", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            when (val state = uiState) {
                is CreateReservationUiState.Idle -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Confirm your frame reservation",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The clinic will prepare your selected frame for try-on. You can optionally link this to an existing appointment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.submit() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Confirm reservation")
                        }
                    }
                }
                is CreateReservationUiState.Submitting -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CreateReservationUiState.Error -> {
                    ErrorContent(message = state.message, onRetry = { viewModel.submit() })
                }
                is CreateReservationUiState.Success -> { /* handled above */ }
            }
        }
    }
}
