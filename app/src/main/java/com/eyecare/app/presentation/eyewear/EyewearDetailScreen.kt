package com.eyecare.app.presentation.eyewear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.EyewearDetail
import com.eyecare.app.domain.model.EyewearEstimate
import com.eyecare.app.domain.model.EyewearItem
import com.eyecare.app.domain.model.EyewearPreparation
import com.eyecare.app.domain.model.EyewearDispensing
import com.eyecare.app.domain.model.EyewearPaymentSummary
import com.eyecare.app.domain.model.EyewearProgress
import com.eyecare.app.presentation.common.components.ErrorContent
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EyewearDetailScreen(
    key: String,
    onBack: () -> Unit,
    onNavigateToJobOrder: (Int) -> Unit = {},
    viewModel: EyewearDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var ratingTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) } // jobOrderItemId, productVariantId

    LaunchedEffect(key) { viewModel.load(key) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Eyewear Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is EyewearDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is EyewearDetailUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding),
            )
            is EyewearDetailUiState.Success -> {
                EyewearDetailContent(
                    detail = state.detail,
                    onRateItem = { itemId, variantId -> ratingTarget = Pair(itemId, variantId) },
                    onNavigateToJobOrder = onNavigateToJobOrder,
                    modifier = Modifier.padding(padding),
                )

                ratingTarget?.let { (itemId, variantId) ->
                    FrameRatingDialog(
                        onSubmit = { rating, comment ->
                            ratingTarget = null
                            // Rating submission handled by FrameRatingViewModel
                        },
                        onDismiss = { ratingTarget = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun EyewearDetailContent(
    detail: EyewearDetail,
    onRateItem: (jobOrderItemId: Int, productVariantId: Int) -> Unit = { _, _ -> },
    onNavigateToJobOrder: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Text(detail.description ?: "Eyewear transaction", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        val (dateLabel, dateValue) = formatDateLabel(detail.consultationAt, detail.createdAt)
        Text("$dateLabel: $dateValue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Progress and payment chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressChip(detail.progress)
            detail.paymentStatus?.let { PaymentChip(it) }
        }

        // Financial summary
        Text("Total: ${formatPeso(detail.totalAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (shouldShowBalance(detail.paymentStatus, detail.balanceDue)) {
            Text("Balance: ${formatPeso(detail.balanceDue!!)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        // Progress tracker
        ProgressTracker(progress = detail.progress)

        // Conditional sections
        detail.estimate?.let { EstimateSection(it) }
        detail.preparation?.let {
            PreparationSection(
                preparation = it,
                isDispensed = detail.progress == EyewearProgress.DISPENSED,
                onRateItem = onRateItem,
            )
        }
        detail.dispensing?.let { DispensingSection(it) }
        detail.paymentSummary?.let { PaymentSummarySection(it) }
    }
}

@Composable
private fun ProgressTracker(progress: EyewearProgress) {
    val tracker = computeTracker(progress)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                tracker.steps.forEach { (step, completed) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(32.dp)
                                .background(
                                    if (completed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(50),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (completed) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            step.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            tracker.terminalMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EstimateSection(estimate: EyewearEstimate) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estimate", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            estimate.items.forEach { ItemRow(it) }
            estimate.subtotal?.let { SummaryRow("Subtotal", formatPeso(it)) }
            estimate.discountAmount?.let { if (it > BigDecimal.ZERO) SummaryRow("Discount", "-${formatPeso(it)}") }
            estimate.total?.let { SummaryRow("Total", formatPeso(it), bold = true) }
            estimate.validUntil?.let { Text("Valid until: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun PreparationSection(
    preparation: EyewearPreparation,
    isDispensed: Boolean = false,
    onRateItem: (jobOrderItemId: Int, productVariantId: Int) -> Unit = { _, _ -> },
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Preparation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            preparation.items.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.description, style = MaterialTheme.typography.bodyMedium)
                        Text("Qty: ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isDispensed && item.id != null && item.productVariantId != null) {
                        IconButton(onClick = { onRateItem(item.id, item.productVariantId) }) {
                            Icon(Icons.Outlined.Star, contentDescription = "Rate", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text(formatPeso(item.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            preparation.startedAt?.let { SummaryRow("Started", formatTimestamp(it)) }
            preparation.readyAt?.let { SummaryRow("Ready", formatTimestamp(it)) }
        }
    }
}

@Composable
private fun DispensingSection(dispensing: EyewearDispensing) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pickup & Release", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            dispensing.readyAt?.let { SummaryRow("Ready for pickup", formatTimestamp(it)) }
            dispensing.dispensedAt?.let { SummaryRow("Released to You", formatTimestamp(it)) }
        }
    }
}

@Composable
private fun PaymentSummarySection(summary: EyewearPaymentSummary) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Payment Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            summary.totalAmount?.let { SummaryRow("Total", formatPeso(it)) }
            summary.amountPaid?.let { SummaryRow("Paid", formatPeso(it)) }
            summary.balanceDue?.let { SummaryRow("Balance", formatPeso(it), bold = it > BigDecimal.ZERO) }
            if (summary.payments.isNotEmpty()) {
                HorizontalDivider()
                Text("Payments", style = MaterialTheme.typography.labelMedium)
                summary.payments.forEach { payment ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(humanizePaymentMethod(payment.paymentMethod), style = MaterialTheme.typography.bodySmall)
                            payment.referenceNumber?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            payment.recordedAt?.let { Text(formatTimestamp(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Text(formatPeso(payment.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: EyewearItem) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(item.description, style = MaterialTheme.typography.bodyMedium)
            Text("Qty: ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatPeso(item.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}
