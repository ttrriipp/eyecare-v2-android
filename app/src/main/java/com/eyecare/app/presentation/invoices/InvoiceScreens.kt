package com.eyecare.app.presentation.invoices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
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
import com.eyecare.app.domain.model.Invoice
import com.eyecare.app.domain.model.InvoiceStatus
import com.eyecare.app.presentation.common.components.ErrorContent

@Composable
fun InvoiceListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: InvoiceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.listState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Invoices") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            InvoiceListUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            InvoiceListUiState.Empty -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No invoices", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is InvoiceListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            is InvoiceListUiState.Success -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.invoices, key = { it.id }) { invoice ->
                    InvoiceCard(invoice = invoice, onClick = { onNavigateToDetail(invoice.id) })
                }
                if (state.hasMorePages) {
                    item {
                        LaunchedEffect(Unit) { viewModel.loadMore() }
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(invoice.invoiceNumber ?: "Invoice #${invoice.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(invoice.status)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                invoice.total?.let { Text("₱${String.format("%.2f", it)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                invoice.balanceDue?.let { if (it > 0) Text("Balance: ₱${String.format("%.2f", it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun InvoiceDetailScreen(
    invoiceId: Int,
    onBack: () -> Unit,
    viewModel: InvoiceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(invoiceId) { viewModel.loadDetail(invoiceId) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Invoice Detail") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            InvoiceDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is InvoiceDetailUiState.Error -> ErrorContent(message = state.message, onRetry = { viewModel.loadDetail(invoiceId) }, modifier = Modifier.padding(padding))
            is InvoiceDetailUiState.Success -> InvoiceDetailContent(invoice = state.invoice, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun InvoiceDetailContent(invoice: Invoice, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(invoice.invoiceNumber ?: "Invoice #${invoice.id}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            StatusChip(invoice.status)
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Financial Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                invoice.subtotal?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Subtotal"); Text("₱${String.format("%.2f", it)}") } }
                invoice.discountAmount?.let { if (it > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Discount", color = MaterialTheme.colorScheme.error); Text("-₱${String.format("%.2f", it)}", color = MaterialTheme.colorScheme.error) } }
                invoice.taxAmount?.let { if (it > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tax"); Text("₱${String.format("%.2f", it)}") } }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total", fontWeight = FontWeight.Bold); Text("₱${String.format("%.2f", invoice.total ?: 0.0)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Paid"); Text("₱${String.format("%.2f", invoice.amountPaid ?: 0.0)}") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Balance", fontWeight = FontWeight.Bold); Text("₱${String.format("%.2f", invoice.balanceDue ?: 0.0)}", fontWeight = FontWeight.Bold, color = if ((invoice.balanceDue ?: 0.0) > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary) }
            }
        }

        if (invoice.items.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    invoice.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(item.description); Text("Qty: ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text("₱${String.format("%.2f", item.amount)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (invoice.payments.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    invoice.payments.forEach { payment ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(payment.paymentMethod.uppercase()); payment.referenceNumber?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            Text("₱${String.format("%.2f", payment.amount)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: InvoiceStatus) {
    val (label, color) = when (status) {
        InvoiceStatus.DRAFT -> "Draft" to MaterialTheme.colorScheme.surfaceVariant
        InvoiceStatus.ISSUED -> "Issued" to MaterialTheme.colorScheme.primaryContainer
        InvoiceStatus.PARTIALLY_PAID -> "Partially Paid" to MaterialTheme.colorScheme.secondaryContainer
        InvoiceStatus.PAID -> "Paid" to MaterialTheme.colorScheme.tertiaryContainer
        InvoiceStatus.VOIDED -> "Voided" to MaterialTheme.colorScheme.errorContainer
    }
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}
