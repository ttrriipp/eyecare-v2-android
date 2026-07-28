package com.eyecare.app.presentation.billingrecords

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.BillingRecord
import com.eyecare.app.domain.model.BillingRecordStatus
import com.eyecare.app.presentation.common.components.ErrorContent
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BillingRecordListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: BillingRecordListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Billing Records") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is BillingRecordListUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is BillingRecordListUiState.Empty -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Receipt, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No billing records yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is BillingRecordListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            is BillingRecordListUiState.Success -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.records, key = { it.id }) { record ->
                    BillingRecordCard(record = record, onClick = { onNavigateToDetail(record.id) })
                }
                if (state.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
                if (state.loadMoreError != null) {
                    item {
                        Text(
                            state.loadMoreError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingRecordCard(record: BillingRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(record.billingRecordNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(record.status)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: ${formatPeso(record.totalAmount)}", style = MaterialTheme.typography.bodyMedium)
                Text("Paid: ${formatPeso(record.amountPaid)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (record.balanceDue > BigDecimal.ZERO) {
                Text("Balance: ${formatPeso(record.balanceDue)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            record.recordedAt?.let {
                Text(it.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BillingRecordDetailScreen(
    billingRecordId: Int,
    onBack: () -> Unit,
    onNavigateToJobOrder: (Int) -> Unit = {},
    viewModel: BillingRecordDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(billingRecordId) { viewModel.load(billingRecordId) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Billing Record") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is BillingRecordDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is BillingRecordDetailUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::retry, modifier = Modifier.padding(padding))
            is BillingRecordDetailUiState.Success -> BillingRecordDetailContent(
                record = state.record,
                onNavigateToJobOrder = onNavigateToJobOrder,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun BillingRecordDetailContent(
    record: BillingRecord,
    onNavigateToJobOrder: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(record.billingRecordNumber, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            StatusChip(record.status)
        }

        // Financial summary
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Financial Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total"); Text(formatPeso(record.totalAmount), fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Paid"); Text(formatPeso(record.amountPaid))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Balance", fontWeight = FontWeight.Bold)
                    Text(formatPeso(record.balanceDue), fontWeight = FontWeight.Bold,
                        color = if (record.balanceDue > BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                }
                record.recordedAt?.let {
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Recorded"); Text(it.take(16))
                    }
                }
            }
        }

        // Payments
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                if (record.payments.isEmpty()) {
                    Text("No payments recorded yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    record.payments.forEach { payment ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(payment.paymentMethod.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                                payment.referenceNumber?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                payment.recordedAt?.let { Text(it.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Text(formatPeso(payment.amount), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Job Order link
        Button(
            onClick = { onNavigateToJobOrder(record.jobOrderId) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50),
        ) {
            Text("View job order")
        }
    }
}

@Composable
private fun StatusChip(status: BillingRecordStatus) {
    val (label, color) = when (status) {
        BillingRecordStatus.UNPAID -> "Unpaid" to MaterialTheme.colorScheme.errorContainer
        BillingRecordStatus.PARTIALLY_PAID -> "Partially paid" to MaterialTheme.colorScheme.secondaryContainer
        BillingRecordStatus.PAID -> "Paid" to MaterialTheme.colorScheme.tertiaryContainer
        BillingRecordStatus.VOIDED -> "Voided" to MaterialTheme.colorScheme.errorContainer
        BillingRecordStatus.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

private fun formatPeso(amount: BigDecimal): String = pesoFormat.format(amount)
