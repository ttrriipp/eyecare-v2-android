package com.eyecare.app.presentation.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.Feedback
import com.eyecare.app.ui.theme.StatusPending

@Composable
fun FeedbackHistoryScreen(
    viewModel: FeedbackViewModel = hiltViewModel<FeedbackViewModel, FeedbackViewModel.Factory> {
        it.create(null, null)
    },
) {
    val uiState by viewModel.history.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is FeedbackHistoryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is FeedbackHistoryUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No feedback submitted yet.", style = MaterialTheme.typography.bodyMedium)
        }
        is FeedbackHistoryUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        is FeedbackHistoryUiState.Success -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Feedback History", style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            items(state.items, key = { it.id }) { feedback ->
                FeedbackCard(feedback)
            }
        }
    }
}

@Composable
private fun FeedbackCard(feedback: Feedback) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Stars
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { i ->
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp),
                        tint = if (i < feedback.rating) StatusPending else MaterialTheme.colorScheme.outline)
                }
            }
            feedback.comment?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            // Staff reply
            if (!feedback.staffReply.isNullOrBlank()) {
                Surface(shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Staff Reply", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text(feedback.staffReply, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
