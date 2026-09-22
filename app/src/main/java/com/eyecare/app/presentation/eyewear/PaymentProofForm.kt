package com.eyecare.app.presentation.eyewear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.PaymentInstructions
import com.eyecare.app.domain.model.PaymentMethodInstructions
import com.eyecare.app.domain.model.PaymentProofSummary
import com.eyecare.app.domain.model.PaymentProofStatus
import com.eyecare.app.presentation.common.buildImageUrl
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

@Composable
fun PaymentInstructionsCard(
    instructions: PaymentInstructions,
    selectedMethod: String = instructions.method,
    onMethodSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val availableMethods = instructions.availableMethods.ifEmpty {
        listOf(
            PaymentMethodInstructions(
                method = instructions.method,
                label = instructions.label,
                clinicAccountName = instructions.clinicAccountName,
                clinicAccountNumber = instructions.clinicAccountNumber,
                bankName = instructions.bankName,
                amount = instructions.amount,
                orderReference = instructions.orderReference,
                paymentExpiresAt = instructions.paymentExpiresAt,
                qrImageUrl = instructions.qrImageUrl,
            ),
        )
    }
    val selected = availableMethods.firstOrNull { it.method == selectedMethod } ?: availableMethods.first()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Payment instructions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (availableMethods.size > 1) {
                Text(
                    "Payment method",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableMethods.forEach { method ->
                        FilterChip(
                            selected = method.method == selected.method,
                            onClick = { onMethodSelected(method.method) },
                            label = { Text(method.label) },
                        )
                    }
                }
            }

            InfoRow(label = "Method", value = selected.label)

            selected.bankName?.takeIf(String::isNotBlank)?.let {
                InfoRow(label = "Bank", value = it)
            }

            // Account name
            InfoRow(label = "Account name", value = selected.clinicAccountName)

            // Account number with copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Account number",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        selected.clinicAccountNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(selected.clinicAccountNumber))
                }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy account number")
                }
            }

            // Amount
            InfoRow(label = "Amount to pay", value = pesoFormat.format(selected.amount))

            // Order reference
            InfoRow(label = "Order reference", value = selected.orderReference)

            selected.qrImageUrl?.takeIf(String::isNotBlank)?.let { qrImageUrl ->
                Text(
                    "Scan to pay",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AsyncImage(
                    model = buildImageUrl(qrImageUrl),
                    contentDescription = "${selected.label} QR code",
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.CenterHorizontally),
                )
            }

            selected.paymentExpiresAt?.let {
                InfoRow(label = "Payment deadline", value = formatPaymentDeadline(it) ?: it)
            }
        }
    }
}

@Composable
fun PaymentInstructionsUnavailableCard(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Payment instructions are currently unavailable",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Refresh the order to try again. Do not upload payment proof until the clinic provides payment details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
fun PaymentDeadlineCard(
    expiresAt: String?,
    onExpired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember(expiresAt) { mutableStateOf(java.time.Instant.now()) }
    var expired by remember(expiresAt) {
        mutableStateOf(paymentSecondsRemaining(expiresAt, java.time.Instant.now()) == 0L)
    }
    var notified by remember(expiresAt) { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(expiresAt) {
        while (true) {
            now = java.time.Instant.now()
            val remaining = paymentSecondsRemaining(expiresAt, now)
            if (remaining == null || remaining <= 0L) {
                expired = remaining == 0L
                if (expired && !notified) {
                    notified = true
                    onExpired()
                }
                break
            }
            kotlinx.coroutines.delay(1_000)
        }
    }

    val remaining = paymentSecondsRemaining(expiresAt, now)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (expired) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (expired) "Payment window expired" else "Payment window",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            if (remaining == null) {
                Text(
                    "The clinic has not provided a payment deadline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (expired) {
                Text(
                    "Refresh the order to see the clinic's latest payment status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                Text(
                    "${formatPaymentCountdown(remaining)} remaining · Due ${formatPaymentDeadline(expiresAt) ?: expiresAt}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
fun PaymentProofStatusCard(
    status: PaymentProofStatus,
    rejectionReason: String?,
    paymentMethod: String? = null,
    modifier: Modifier = Modifier,
) {
    val (title, message, color) = when (status) {
        PaymentProofStatus.NOT_SUBMITTED -> Triple("Payment proof", "No payment proof has been submitted.", MaterialTheme.colorScheme.onSurfaceVariant)
        PaymentProofStatus.PENDING -> Triple(
            "Payment proof under review",
            paymentMethod?.let { "The clinic is reviewing your ${displayPaymentMethodLabel(it)} payment." }
                ?: "The clinic is reviewing your payment.",
            MaterialTheme.colorScheme.primary,
        )
        PaymentProofStatus.ACCEPTED -> Triple("Payment proof accepted", "Your payment has been verified by the clinic.", MaterialTheme.colorScheme.tertiary)
        PaymentProofStatus.REJECTED -> Triple("Payment proof rejected", rejectionReason ?: "The clinic could not verify this payment proof.", MaterialTheme.colorScheme.error)
        PaymentProofStatus.UNKNOWN -> Triple("Payment proof status unavailable", "Refresh the order to see the latest payment status.", MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ExistingProofCard(
    proof: PaymentProofSummary,
    modifier: Modifier = Modifier,
) {
    val statusLabel = when (proof.status) {
        PaymentProofStatus.NOT_SUBMITTED -> "Not submitted"
        PaymentProofStatus.PENDING -> "Under review"
        PaymentProofStatus.ACCEPTED -> "Accepted"
        PaymentProofStatus.REJECTED -> "Rejected"
        PaymentProofStatus.UNKNOWN -> "Status unavailable"
    }
    val statusColor = when (proof.status) {
        PaymentProofStatus.NOT_SUBMITTED -> MaterialTheme.colorScheme.onSurfaceVariant
        PaymentProofStatus.PENDING -> MaterialTheme.colorScheme.primary
        PaymentProofStatus.ACCEPTED -> MaterialTheme.colorScheme.tertiary
        PaymentProofStatus.REJECTED -> MaterialTheme.colorScheme.error
        PaymentProofStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Payment proof",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            InfoRow(label = "Sender", value = proof.senderName)
            proof.paymentMethod?.takeIf(String::isNotBlank)?.let {
                InfoRow(label = "Payment method", value = displayPaymentMethodLabel(it))
            }
            InfoRow(label = "Reference", value = proof.referenceNumber)

            if (proof.status == PaymentProofStatus.REJECTED && !proof.rejectionReason.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                ) {
                    Text(
                        "Reason: ${proof.rejectionReason}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            if (proof.status == PaymentProofStatus.PENDING) {
                Text(
                    "Your proof is being reviewed by the clinic. You will be notified once it is processed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PaymentProofForm(
    selectedProof: SelectedPaymentProof?,
    onPickProof: () -> Unit,
    paymentMethod: String = "gcash",
    paymentMethodLabel: String = displayPaymentMethodLabel(paymentMethod),
    onSubmit: (paymentMethod: String, senderName: String, referenceNumber: String, proof: SelectedPaymentProof) -> Unit,
    uploadState: ProofUploadState,
    onClearError: () -> Unit,
    canSubmit: Boolean = true,
    pickerErrorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var senderName by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var senderError by remember { mutableStateOf<String?>(null) }
    var refError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Upload payment proof",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                "Take a screenshot of your $paymentMethodLabel payment confirmation and enter the details below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = onPickProof,
                enabled = canSubmit && uploadState !is ProofUploadState.Uploading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null)
                Text(selectedProof?.displayName ?: "Choose proof image")
            }
            Text(
                selectedProof?.let {
                    "${it.mimeType} · ${"%.1f".format(it.file.length() / (1024f * 1024f))} MB"
                } ?: "JPG, JPEG, or PNG up to 10 MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            pickerErrorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = senderName,
                onValueChange = {
                    senderName = it
                    senderError = null
                    onClearError()
                },
                label = { Text("Sender name") },
                placeholder = { Text("Name on $paymentMethodLabel account") },
                isError = senderError != null,
                supportingText = senderError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = referenceNumber,
                onValueChange = {
                    referenceNumber = it
                    refError = null
                    onClearError()
                },
                label = { Text("Reference number") },
                placeholder = { Text("$paymentMethodLabel reference number") },
                isError = refError != null,
                supportingText = refError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            when (val state = uploadState) {
                is ProofUploadState.Error -> {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                is ProofUploadState.Uploading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        Text(
                            "Uploading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {}
            }

            Button(
                onClick = {
                    senderError = PaymentProofInspector.validateSenderName(senderName)
                    refError = PaymentProofInspector.validateReferenceNumber(referenceNumber)
                    if (senderError == null && refError == null) {
                        selectedProof?.let { proof ->
                            onSubmit(paymentMethod, senderName.trim(), referenceNumber.trim(), proof)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit && selectedProof != null && uploadState !is ProofUploadState.Uploading,
            ) {
                Icon(Icons.Outlined.Upload, contentDescription = null)
                Text("Submit proof")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun displayPaymentMethodLabel(method: String?): String = when (method?.lowercase()) {
    "bank_transfer" -> "Bank transfer"
    "gcash" -> "GCash"
    else -> "payment"
}
