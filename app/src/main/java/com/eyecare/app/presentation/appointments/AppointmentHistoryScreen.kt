package com.eyecare.app.presentation.appointments

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.presentation.appointments.components.VisitFeedbackDialog
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CLINIC_ZONE = ZoneId.of("Asia/Manila")
private val historyDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
private val historyTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentHistoryScreen(
    uiState: AppointmentHistoryUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onShowRating: (Int) -> Unit = {},
    onSubmitRating: (Int, String?) -> Unit = { _, _ -> },
    onDismissRating: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val content = uiState as? AppointmentHistoryUiState.Content
    val ratingAppointment = content?.ratingAppointmentId?.let { id ->
        content.appointments.find { it.id == id }
    }
    if (content != null && ratingAppointment != null) {
        VisitFeedbackDialog(
            onSubmit = onSubmitRating,
            onDismiss = onDismissRating,
            initialRating = ratingAppointment.visitRating?.rating ?: 0,
            initialComment = ratingAppointment.visitRating?.comment.orEmpty(),
            title = if (ratingAppointment.visitRating != null) "Update your rating" else "Rate your visit",
            isSubmitting = content.isSubmittingRating,
            errorMessage = content.ratingError,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointment History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        when (uiState) {
            is AppointmentHistoryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is AppointmentHistoryUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Unable to load history",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
            is AppointmentHistoryUiState.Empty -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No past appointments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Past visits, cancellations, and missed appointments will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRefresh) { Text("Refresh") }
                }
            }
            is AppointmentHistoryUiState.Content -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        uiState.refreshError?.let { error ->
                            HistoryErrorBanner(
                                message = error,
                                onRetry = onRefresh,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        HistoryList(
                            appointments = uiState.appointments,
                            isLoadingMore = uiState.isLoadingMore,
                            hasMorePages = uiState.hasMorePages,
                            loadMoreError = uiState.loadMoreError,
                            onLoadMore = onLoadMore,
                            onNavigateToDetail = onNavigateToDetail,
                            onShowRating = onShowRating,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    appointments: List<AppointmentV1>,
    isLoadingMore: Boolean,
    hasMorePages: Boolean,
    loadMoreError: String?,
    onLoadMore: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onShowRating: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(
        appointments.size,
        hasMorePages,
        isLoadingMore,
        loadMoreError,
    ) {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= appointments.size - 3 &&
                hasMorePages &&
                !isLoadingMore &&
                loadMoreError == null
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(appointments, key = { it.id }) { appointment ->
            HistoryRow(
                appointment = appointment,
                onClick = { onNavigateToDetail(appointment.id) },
                onRateClick = { onShowRating(appointment.id) },
            )
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        if (loadMoreError != null) {
            item {
                HistoryErrorBanner(
                    message = loadMoreError,
                    onRetry = onLoadMore,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HistoryErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun HistoryRow(
    appointment: AppointmentV1,
    onClick: () -> Unit,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = appointment.appointmentType,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                HistoryStatusChip(status = appointment.status)
            }
            Spacer(Modifier.height(8.dp))
            val parsed = remember(appointment.scheduledAt) { parseHistoryDateTime(appointment.scheduledAt) }
            Row {
                Text(
                    text = parsed?.date ?: appointment.scheduledAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = parsed?.time ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            appointment.reasonForVisit?.let { reason ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            when {
                appointment.isRateable && appointment.visitRating == null -> {
                    SuggestionChip(
                        onClick = onRateClick,
                        label = { Text("Rate this visit") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
                appointment.visitRating != null -> {
                    SuggestionChip(
                        onClick = onRateClick,
                        label = { Text("Update rating") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryStatusChip(
    status: AppointmentStatus,
    modifier: Modifier = Modifier,
) {
    val presentation = appointmentStatusPresentation(status)
    Surface(
        shape = RoundedCornerShape(50),
        color = presentation.fillColor.copy(alpha = 0.12f),
        modifier = modifier,
    ) {
        Text(
            text = presentation.label.uppercase(Locale.US),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = presentation.textColor,
        )
    }
}

private data class HistoryDateTime(val date: String, val time: String)

private fun parseHistoryDateTime(iso: String): HistoryDateTime? = runCatching {
    val zoned = OffsetDateTime.parse(iso).atZoneSameInstant(CLINIC_ZONE)
    HistoryDateTime(
        date = zoned.format(historyDateFormatter),
        time = zoned.format(historyTimeFormatter),
    )
}.getOrNull()
