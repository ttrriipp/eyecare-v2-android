package com.eyecare.app.presentation.feedback

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.ui.theme.StatusPending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    appointmentId: Int?,
    orderId: Int?,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val viewModel = hiltViewModel<FeedbackViewModel, FeedbackViewModel.Factory> {
        it.create(appointmentId, orderId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is FeedbackUiState.Submitted) {
            onSubmitted()
        }
    }

    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    val isLoading = uiState is FeedbackUiState.Loading

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Leave Feedback") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        when (uiState) {
            is FeedbackUiState.Submitted -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("How was your experience?", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold)

                // Star rating row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "$star stars",
                                tint = if (star <= rating) StatusPending else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }

                // Validation error
                if (uiState is FeedbackUiState.ValidationError) {
                    Text((uiState as FeedbackUiState.ValidationError).message,
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (uiState is FeedbackUiState.Error) {
                    Text((uiState as FeedbackUiState.Error).message,
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 2000) comment = it },
                    label = { Text("Comment (optional)") },
                    placeholder = { Text("Tell us about your experience…") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxLines = 6,
                    enabled = !isLoading,
                )

                Text("${comment.length}/2000", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End))

                Button(
                    onClick = { viewModel.submit(rating, comment.takeIf { it.isNotBlank() }) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp))
                    else Text("Submit Feedback", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(96.dp))
            }
        }
    }
}
