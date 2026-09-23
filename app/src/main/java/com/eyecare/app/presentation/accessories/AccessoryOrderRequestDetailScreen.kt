package com.eyecare.app.presentation.accessories

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.AcceptedOrderSummary
import com.eyecare.app.domain.model.DiscountProofStatus
import com.eyecare.app.domain.model.DiscountProofUpload
import com.eyecare.app.domain.model.DiscountType
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.eyewear.formatTimestamp
import com.eyecare.app.presentation.eyewear.orderStatusColor
import com.eyecare.app.presentation.eyewear.orderStatusLabel
import com.eyecare.app.presentation.eyewear.orderStatusTextColor
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
private const val OTHER_CANCELLATION_REASON = "Other"
private val ORDER_REQUEST_CANCELLATION_REASON_PRESETS = listOf(
    "Don't need" to "I no longer need these items.",
    "By mistake" to "I submitted this request by mistake.",
    "Change items" to "I need to change the requested items.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoryOrderRequestDetailScreen(
    uiState: RequestDetailUiState,
    showCancelDialog: Boolean,
    onShowCancelDialog: () -> Unit,
    onDismissCancelDialog: () -> Unit,
    onCancel: (String) -> Unit,
    onNavigateToOrder: (Int) -> Unit,
    onUploadDiscountProof: (DiscountProofUpload) -> Unit = {},
    onClearDiscountProofUploadState: () -> Unit = {},
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedProof by remember { mutableStateOf<SelectedDiscountProof?>(null) }
    var pickerErrorMessage by remember { mutableStateOf<String?>(null) }
    var cancelReason by remember { mutableStateOf("") }
    var selectedCancelReason by remember { mutableStateOf<String?>(null) }
    val proofPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedProof?.file?.delete()
        selectedProof = null
        pickerErrorMessage = null
        if (uri != null) {
            val result = prepareDiscountProof(context, uri)
            pickerErrorMessage = result.errorMessage
            selectedProof = result.proof
        }
    }

    DisposableEffect(selectedProof) {
        onDispose { selectedProof?.file?.delete() }
    }

    LaunchedEffect(showCancelDialog) {
        if (showCancelDialog) {
            cancelReason = ""
            selectedCancelReason = null
        }
    }

    val cancelReasonToSubmit = if (selectedCancelReason == OTHER_CANCELLATION_REASON) {
        cancelReason.trim()
    } else {
        selectedCancelReason.orEmpty().trim()
    }

    LaunchedEffect(uiState) {
        val state = uiState as? RequestDetailUiState.Success ?: return@LaunchedEffect
        if (state.request.discountProofStatus !in setOf(
                DiscountProofStatus.NOT_SUBMITTED,
                DiscountProofStatus.REJECTED,
            )
        ) {
            selectedProof?.file?.delete()
            selectedProof = null
            pickerErrorMessage = null
        }
    }

    if (showCancelDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.Cancel,
            title = "Cancel request?",
            message = "Choose a cancellation reason. This cannot be undone.",
            confirmLabel = "Cancel request",
            dismissLabel = "Keep request",
            isDestructive = true,
            confirmEnabled = cancelReasonToSubmit.isNotBlank() &&
                cancelReasonToSubmit.length <= MAX_CANCELLATION_REASON_LENGTH,
            onConfirm = {
                onCancel(cancelReasonToSubmit)
                onDismissCancelDialog()
            },
            onDismissRequest = onDismissCancelDialog,
            supportingContent = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Cancellation reason · required", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Choose a preset or select Other to write your own.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OrderRequestCancellationReasonPicker(
                        selectedReason = selectedCancelReason,
                        customReason = cancelReason,
                        onPresetSelected = { preset ->
                            selectedCancelReason = preset
                            cancelReason = ""
                        },
                        onOtherSelected = {
                            selectedCancelReason = OTHER_CANCELLATION_REASON
                            cancelReason = ""
                        },
                        onCustomReasonChanged = { value ->
                            if (value.length <= MAX_CANCELLATION_REASON_LENGTH) cancelReason = value
                        },
                    )
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        when (val state = uiState) {
            is RequestDetailUiState.Loading -> LoadingContent(
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            is RequestDetailUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = if (!state.isNotFound) onRetry else null,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            is RequestDetailUiState.Success -> {
                val request = state.request
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        state.refreshErrorMessage?.let { message ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }

                        RequestSummaryCard(request = request)

                        // Status guidance
                        RequestStatusGuidance(request = request)

                        request.order?.let { order ->
                            AcceptedOrderSummaryCard(order = order)
                        }

                        if (request.requestedDiscountType != DiscountType.NONE) {
                            DiscountProofSection(
                                request = request,
                                uploadState = state.uploadState,
                                selectedProof = selectedProof,
                                pickerErrorMessage = pickerErrorMessage,
                                onPickProof = {
                                    pickerErrorMessage = null
                                    proofPicker.launch(arrayOf("image/jpeg", "image/png"))
                                },
                                onSubmitProof = {
                                    selectedProof?.let { proof ->
                                        onUploadDiscountProof(
                                            DiscountProofUpload(
                                                imageFile = proof.file,
                                                mimeType = proof.mimeType,
                                                width = proof.width,
                                                height = proof.height,
                                                deleteAfterUpload = true,
                                            ),
                                        )
                                    }
                                },
                                onClearUploadState = onClearDiscountProofUploadState,
                            )
                        }

                        // Items
                        if (request.items.isNotEmpty()) {
                            Text(
                                "Items (${request.items.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            request.items.forEach { item ->
                                val productName = item.itemSnapshot.productName.ifBlank { item.description }
                                val variantName = item.itemSnapshot.variantName.trim()
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, EyecareColors.current.cardBorder),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        RequestItemImage(
                                            imagePath = item.itemSnapshot.images.firstOrNull(),
                                            productName = productName,
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = productName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (variantName.isNotBlank()) {
                                                Text(
                                                    text = variantName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Text(
                                                "× ${item.quantity}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "Line total",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                pesoFormat.format(item.amount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EyecareColors.current.accentText,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Actions
                        if (state.canCancel) {
                            OutlinedButton(
                                onClick = onShowCancelDialog,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = EyecareColors.current.statusCancelledText,
                                ),
                            ) {
                                Icon(Icons.Outlined.Cancel, contentDescription = null)
                                Text("Cancel request")
                            }
                        }

                        if (state.isCancelling) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                                Text("Cancelling...")
                            }
                        }

                        // Accepted order navigation
                        request.order?.let { order ->
                            Button(
                                onClick = { onNavigateToOrder(order.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp),
                            ) {
                                Icon(Icons.Outlined.ShoppingBag, contentDescription = null)
                                Text("View order and payment")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRequestCancellationReasonPicker(
    selectedReason: String?,
    customReason: String,
    onPresetSelected: (String) -> Unit,
    onOtherSelected: () -> Unit,
    onCustomReasonChanged: (String) -> Unit,
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        selectedLabelColor = EyecareColors.current.accentText,
    )

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { selectableGroup() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ORDER_REQUEST_CANCELLATION_REASON_PRESETS.forEach { (label, reason) ->
            FilterChip(
                selected = selectedReason == reason,
                onClick = { onPresetSelected(reason) },
                label = { Text(label) },
                modifier = Modifier.heightIn(min = 44.dp),
                colors = chipColors,
            )
        }
        FilterChip(
            selected = selectedReason == OTHER_CANCELLATION_REASON,
            onClick = onOtherSelected,
            label = { Text(OTHER_CANCELLATION_REASON) },
            modifier = Modifier.heightIn(min = 44.dp),
            colors = chipColors,
        )
    }

    if (selectedReason == OTHER_CANCELLATION_REASON) {
        OutlinedTextField(
            value = customReason,
            onValueChange = onCustomReasonChanged,
            label = { Text("Your reason") },
            placeholder = { Text("Tell the clinic why you're cancelling") },
            supportingText = {
                Text("Required · ${customReason.length}/$MAX_CANCELLATION_REASON_LENGTH")
            },
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RequestSummaryCard(request: AccessoryOrderRequest) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, EyecareColors.current.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    request.requestNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                RequestStatusBadge(status = request.status)
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Estimated subtotal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    pesoFormat.format(request.subtotalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = EyecareColors.current.accentText,
                )
            }

            RequestSummaryRow("Submitted", formatTimestamp(request.createdAt))
            if (request.requestedDiscountType != DiscountType.NONE) {
                RequestSummaryRow(
                    "Discount requested",
                    discountTypeLabel(request.requestedDiscountType),
                )
            }
            request.cancelledAt?.let {
                RequestSummaryRow("Cancelled", formatTimestamp(it))
                request.cancellationReason?.takeIf(String::isNotBlank)?.let { reason ->
                    RequestSummaryRow("Cancellation reason", reason)
                }
            } ?: request.resolvedAt?.let {
                RequestSummaryRow("Reviewed", formatTimestamp(it))
            }
        }
    }
}

@Composable
private fun RequestSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

private const val MAX_CANCELLATION_REASON_LENGTH = 1_000

@Composable
private fun AcceptedOrderSummaryCard(order: AcceptedOrderSummary) {
    val status = OpticalOrderStatus.from(order.status)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, EyecareColors.current.accentText.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Order created",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusPill(
                    label = orderStatusLabel(status),
                    fillColor = orderStatusColor(status),
                    textColor = orderStatusTextColor(status),
                )
            }
            RequestSummaryRow("Order", order.orderNumber)
            RequestSummaryRow("Order total", pesoFormat.format(order.totalAmount))
            if (order.discountAmount > BigDecimal.ZERO) {
                RequestSummaryRow("Discount applied", "−${pesoFormat.format(order.discountAmount)}")
            }
            order.paymentExpiresAt?.let {
                RequestSummaryRow("Payment deadline", formatTimestamp(it))
            }
        }
    }
}

@Composable
private fun DiscountProofSection(
    request: AccessoryOrderRequest,
    uploadState: DiscountProofUploadState,
    selectedProof: SelectedDiscountProof?,
    pickerErrorMessage: String?,
    onPickProof: () -> Unit,
    onSubmitProof: () -> Unit,
    onClearUploadState: () -> Unit,
) {
    val discountLabel = discountTypeLabel(request.requestedDiscountType)
    val canUpload = request.status == OrderRequestStatus.PENDING

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, EyecareColors.current.accentText.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Discount proof",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "$discountLabel discount requested. The clinic must accept your proof before the request can be accepted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (request.discountProofStatus) {
                DiscountProofStatus.NOT_SUBMITTED,
                DiscountProofStatus.REJECTED -> {
                    if (request.discountProofStatus == DiscountProofStatus.REJECTED) {
                        Text(
                            request.discountProofRejectionReason?.takeIf(String::isNotBlank)
                                ?: "The clinic could not verify the previous proof. Please upload a replacement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EyecareColors.current.statusCancelledText,
                        )
                    }

                    if (!canUpload) {
                        Text(
                            "This request is no longer accepting discount proof uploads.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (selectedProof != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    selectedProof.displayName,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                TextButton(onClick = onPickProof) { Text("Choose another") }
                            }
                        }
                        Button(
                            onClick = onSubmitProof,
                            enabled = uploadState !is DiscountProofUploadState.Uploading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                        ) {
                            if (uploadState is DiscountProofUploadState.Uploading) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Text("Uploading proof…")
                                }
                            } else {
                                Text(if (request.discountProofStatus == DiscountProofStatus.REJECTED) "Replace proof" else "Submit proof")
                            }
                        }
                    } else {
                        OutlinedButton(onClick = onPickProof, modifier = Modifier.fillMaxWidth()) {
                            Text(if (request.discountProofStatus == DiscountProofStatus.REJECTED) "Choose replacement proof" else "Choose proof")
                        }
                    }
                }
                DiscountProofStatus.PENDING -> {
                    Text(
                        "Proof submitted and awaiting clinic review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EyecareColors.current.accentText,
                    )
                }
                DiscountProofStatus.ACCEPTED -> {
                    Text(
                        "Proof accepted. The clinic can now review your request.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EyecareColors.current.statusConfirmedText,
                    )
                }
                DiscountProofStatus.NOT_REQUIRED,
                DiscountProofStatus.UNKNOWN -> {
                    Text(
                        "Discount proof status is unavailable. Refresh to try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            pickerErrorMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (uploadState is DiscountProofUploadState.Error) {
                TextButton(onClick = onClearUploadState) { Text("Dismiss") }
                Text(
                    uploadState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun discountTypeLabel(type: DiscountType): String = when (type) {
    DiscountType.SENIOR_CITIZEN -> "Senior citizen"
    DiscountType.PWD -> "PWD"
    DiscountType.NONE -> "No discount"
    DiscountType.UNKNOWN -> "Discount unavailable"
}

@Composable
private fun RequestItemImage(
    imagePath: String?,
    productName: String,
) {
    val imageUrl = imagePath?.takeIf(String::isNotBlank)?.let(::buildImageUrl)
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = Modifier.size(68.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            val imageLoaded = imageState is AsyncImagePainter.State.Success
            val imageUnavailable = imageUrl == null || imageState is AsyncImagePainter.State.Error
            if (imageUnavailable) {
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    contentDescription = "$productName image unavailable",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .alpha(if (imageLoaded) 1f else 0f),
                    onState = { imageState = it },
                )
            }
        }
    }
}

@Composable
private fun RequestStatusBadge(status: OrderRequestStatus) {
    val (label, fillColor, textColor) = when (status) {
        OrderRequestStatus.PENDING -> Triple(
            "Awaiting review",
            EyecareColors.current.statusPending,
            EyecareColors.current.statusPendingText,
        )
        OrderRequestStatus.ACCEPTED -> Triple(
            "Accepted",
            EyecareColors.current.statusConfirmed,
            EyecareColors.current.statusConfirmedText,
        )
        OrderRequestStatus.REJECTED -> Triple(
            "Declined",
            EyecareColors.current.statusCancelled,
            EyecareColors.current.statusCancelledText,
        )
        OrderRequestStatus.CANCELLED -> Triple(
            "Cancelled",
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrderRequestStatus.UNKNOWN -> Triple(
            "Status unavailable",
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    StatusPill(label = label, fillColor = fillColor, textColor = textColor)
}

@Composable
private fun StatusPill(label: String, fillColor: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = fillColor.copy(alpha = 0.12f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RequestStatusGuidance(request: AccessoryOrderRequest) {
    val (icon, title, message, containerColor, iconColor) = when (request.status) {
        OrderRequestStatus.PENDING -> GuidanceContent(
            icon = Icons.Outlined.Schedule,
            title = "Awaiting clinic review",
            message = "No payment due yet. No action needed yet. The clinic will accept or decline this request. Stock is not reserved.",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            iconColor = EyecareColors.current.accentText,
        )
        OrderRequestStatus.ACCEPTED -> GuidanceContent(
            icon = Icons.Outlined.CheckCircle,
            title = "Request accepted",
            message = "The clinic created an order. Review the payment and pickup details below.",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
            iconColor = EyecareColors.current.statusConfirmedText,
        )
        OrderRequestStatus.REJECTED -> GuidanceContent(
            icon = Icons.Outlined.Cancel,
            title = "Request declined",
            message = request.rejectionReason?.takeIf(String::isNotBlank)
                ?: "The clinic declined this request.",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            iconColor = EyecareColors.current.statusCancelledText,
        )
        OrderRequestStatus.CANCELLED -> GuidanceContent(
            icon = Icons.Outlined.Cancel,
            title = "Request cancelled",
            message = "This request is no longer active.",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrderRequestStatus.UNKNOWN -> GuidanceContent(
            icon = Icons.Outlined.Info,
            title = "Status unavailable",
            message = "We couldn't confirm the latest status. Pull down to check again.",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, EyecareColors.current.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private data class GuidanceContent(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String,
    val containerColor: androidx.compose.ui.graphics.Color,
    val iconColor: androidx.compose.ui.graphics.Color,
)
