package com.eyecare.app.presentation.eyewear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.eyecare.app.domain.model.PaymentInstructions
import com.eyecare.app.domain.model.PaymentProofSummary
import com.eyecare.app.domain.model.PaymentProofStatus
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

@Composable
fun PaymentInstructionsCard(
    instructions: PaymentInstructions,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

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

            // Method
            InfoRow(label = "Method", value = instructions.method.uppercase())

            // Account name
            InfoRow(label = "Account name", value = instructions.clinicAccountName)

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
                        instructions.clinicAccountNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(instructions.clinicAccountNumber))
                }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy account number")
                }
            }

            // Amount
            InfoRow(label = "Amount to pay", value = pesoFormat.format(instructions.amount))

            // Order reference
            InfoRow(label = "Order reference", value = instructions.orderReference)

            instructions.paymentExpiresAt?.let {
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
                "Refresh the order to try again. Do not upload payment proof until the clinic provides its GCash details.",
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
    modifier: Modifier = Modifier,
) {
    val (title, message, color) = when (status) {
        PaymentProofStatus.NOT_SUBMITTED -> Triple("Payment proof", "No payment proof has been submitted.", MaterialTheme.colorScheme.onSurfaceVariant)
        PaymentProofStatus.PENDING -> Triple("Payment proof under review", "The clinic is reviewing your GCash payment.", MaterialTheme.colorScheme.primary)
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
    onSubmit: (senderName: String, referenceNumber: String, proof: SelectedPaymentProof) -> Unit,
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
                "Take a screenshot of your GCash payment confirmation and enter the details below.",
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
                placeholder = { Text("Name on GCash account") },
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
                placeholder = { Text("GCash reference number") },
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
                            onSubmit(senderName.trim(), referenceNumber.trim(), proof)
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
