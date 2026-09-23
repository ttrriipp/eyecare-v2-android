package com.eyecare.app.presentation.eyewear

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentProofStatus
import com.eyecare.app.domain.model.PaymentProofUpload
import com.eyecare.app.presentation.common.FeatureFlags
import com.eyecare.app.presentation.common.RefreshOnResumeEffect
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpticalOrderDetailScreen(
    onBack: () -> Unit,
    ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED,
    viewModel: OpticalOrderDetailViewModel = hiltViewModel(),
    onMessageClinic: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var ratingItemId by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    var selectedProof by remember { mutableStateOf<SelectedPaymentProof?>(null) }
    var pickerErrorMessage by remember { mutableStateOf<String?>(null) }
    val proofPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedProof?.file?.delete()
        selectedProof = null
        pickerErrorMessage = null
        if (uri != null) {
            val result = preparePaymentProof(context, uri)
            pickerErrorMessage = result.errorMessage
            selectedProof = result.proof
        }
    }

    DisposableEffect(selectedProof) {
        val proofFile = selectedProof?.file
        onDispose { proofFile?.delete() }
    }

    RefreshOnResumeEffect(
        onRefresh = {
            // Returning from the system photo picker must not reload the order and recreate
            // the rating dialog, which would discard its selected attachment and form state.
            if (ratingItemId == null) viewModel.onResume()
        },
    )

    val successState = uiState as? OpticalOrderDetailUiState.Success
    val ratingTargetItem = ratingItemId?.let { id -> successState?.order?.items?.firstOrNull { it.id == id } }
    if (ratingTargetItem != null) {
        key(ratingTargetItem.id) {
            val ratingViewModel = hiltViewModel<FrameRatingViewModel, FrameRatingViewModel.Factory> {
                it.create(ratingTargetItem.id)
            }
            val ratingState by ratingViewModel.uiState.collectAsStateWithLifecycle()
            var ratingFlowReady by remember { mutableStateOf(false) }
            LaunchedEffect(ratingTargetItem.id) {
                ratingViewModel.reset()
                ratingFlowReady = true
            }
            LaunchedEffect(ratingState, ratingFlowReady) {
                if (!ratingFlowReady) return@LaunchedEffect
                val success = ratingState as? FrameRatingUiState.Success ?: return@LaunchedEffect
                viewModel.updateItemRating(ratingTargetItem.id, success.result)
                ratingViewModel.reset()
                ratingItemId = null
                viewModel.refresh()
            }
            FrameRatingDialog(
                currentRating = ratingTargetItem.rating?.rating,
                currentComment = ratingTargetItem.rating?.comment,
                currentAttachmentUrl = ratingTargetItem.rating?.ownerAttachmentUrl,
                isSubmitting = ratingState is FrameRatingUiState.Submitting,
                errorMessage = (ratingState as? FrameRatingUiState.Error)?.message,
                onSubmit = ratingViewModel::submitRating,
                onDismiss = {
                    ratingViewModel.reset()
                    ratingItemId = null
                },
                onSubmitWithAttachment = ratingViewModel::submitRatingWithAttachment,
            )
        }
    }

    LaunchedEffect(uiState) {
        val order = (uiState as? OpticalOrderDetailUiState.Success)?.order ?: return@LaunchedEffect
        if (order.paymentProofStatus != PaymentProofStatus.NOT_SUBMITTED) {
            selectedProof?.file?.delete()
            selectedProof = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Order details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is OpticalOrderDetailUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is OpticalOrderDetailUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
            ) { ErrorContent(message = state.message, onRetry = viewModel::retry) }

            is OpticalOrderDetailUiState.Success -> OrderDetailContent(
                order = state.order,
                onRateItem = { itemId -> ratingItemId = itemId },
                ratingsEnabled = ratingsEnabled,
                uploadState = state.uploadState,
                selectedProof = selectedProof,
                pickerErrorMessage = pickerErrorMessage,
                onPickProof = {
                    pickerErrorMessage = null
                    proofPicker.launch(arrayOf("image/jpeg", "image/png"))
                },
                onPaymentProofSubmit = { paymentMethod, senderName, referenceNumber, proof ->
                    viewModel.uploadProof(
                        PaymentProofUpload(
                            imageFile = proof.file,
                            senderName = senderName,
                            referenceNumber = referenceNumber,
                            paymentMethod = paymentMethod,
                            mimeType = proof.mimeType,
                            width = proof.width,
                            height = proof.height,
                            deleteAfterUpload = true,
                        ),
                    )
                },
                onPaymentWindowExpired = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onClearPaymentProofError = viewModel::clearUploadState,
                onMessageClinic = onMessageClinic,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
internal fun OrderDetailContent(
    order: OpticalOrder,
    onRateItem: (Int) -> Unit,
    ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED,
    uploadState: ProofUploadState = ProofUploadState.Idle,
    selectedProof: SelectedPaymentProof? = null,
    pickerErrorMessage: String? = null,
    onPickProof: (() -> Unit)? = null,
    onPaymentProofSubmit: ((String, String, String, SelectedPaymentProof) -> Unit)? = null,
    onPaymentWindowExpired: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onClearPaymentProofError: () -> Unit = {},
    modifier: Modifier = Modifier,
    onMessageClinic: (String) -> Unit = {},
) {
    val isPendingPayment = order.status == OpticalOrderStatus.PENDING_PAYMENT
    var selectedPaymentMethod by remember(order.id, order.paymentInstructions?.method) {
        mutableStateOf(order.paymentInstructions?.method ?: "gcash")
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OrderStatusGuidance(order.status, onRefresh)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            orderCardTitle(order),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Shared with OpticalOrderListScreen.kt's OrderCard so the same order never
                        // renders in contradictory colors between the list and its own detail screen.
                        val statusColor = orderStatusColor(order.status)
                        Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.12f)) {
                            Text(
                                orderStatusLabel(order.status),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = orderStatusTextColor(order.status),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Only the reference number plus the current stage - the tracker
                            // below already shows the full prep/ready/released progression, so
                            // listing every past timestamp here would just repeat it as text.
                            DetailInfoRow("Reference", order.orderNumber)
                            val (stageLabel, stageValue) = orderDateLabelFull(order)
                            DetailInfoRow(stageLabel, stageValue)
                        }
                    }
                }
            }

            if (isPendingPayment) {
                PaymentSummaryCard(order)
                OrderPaymentProofContent(order, onMessageClinic)
                PendingPaymentContent(
                    order = order,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodSelected = { selectedPaymentMethod = it },
                    selectedProof = selectedProof,
                    pickerErrorMessage = pickerErrorMessage,
                    uploadState = uploadState,
                    onPickProof = onPickProof,
                    onPaymentProofSubmit = onPaymentProofSubmit,
                    onPaymentWindowExpired = onPaymentWindowExpired,
                    onRefresh = onRefresh,
                    onClearPaymentProofError = onClearPaymentProofError,
                )
            }

            // Tracker
            OrderTracker(order.status)

            // Order items
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    if (order.items.isEmpty()) {
                        Text(
                            "No items on this order",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OrderItemImage(
                                imagePath = item.imagePath,
                                description = item.description,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${item.quantity} x ${formatPeso(item.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                formatPeso(item.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (ratingsEnabled && item.isRateable) {
                            TextButton(onClick = { onRateItem(item.id) }) {
                                Text(if (item.rating != null) "Update rating" else "Rate this item")
                            }
                        }
                        if (ratingsEnabled && item.rating != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Rating: ${item.rating.rating}/5${item.rating.comment?.let { " - $it" } ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                item.rating.ownerAttachmentUrl?.takeIf(String::isNotBlank)?.let { imageUrl ->
                                    OwnerRatingPhoto(
                                        url = imageUrl,
                                        contentDescription = "Your rating photo",
                                        modifier = Modifier
                                            .size(112.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }

            if (!isPendingPayment) {
                PaymentSummaryCard(order)
                OrderPaymentProofContent(order, onMessageClinic)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentSummaryCard(order: OpticalOrder) {
    val summary = order.paymentSummary
    val balanceDue = summary?.balanceDue?.takeIf { it > BigDecimal.ZERO }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Payment summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (summary != null) {
                DetailInfoRow(
                    "Status",
                    paymentStatusLabel(summary.status),
                    valueColor = paymentStatusTextColor(summary.status),
                )
                DetailInfoRow("Total", formatPeso(summary.totalAmount))
                DetailInfoRow("Paid", formatPeso(summary.amountPaid))
                DetailInfoRow(
                    "Balance due",
                    formatPeso(summary.balanceDue),
                    valueColor = if (balanceDue != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        paymentStatusTextColor(summary.status)
                    },
                    valueWeight = if (balanceDue != null) FontWeight.Bold else FontWeight.SemiBold,
                )
                summary.paymentDueDate?.let { DetailInfoRow("Due date", it) }
                if (summary.isOverdue) {
                    Text("Overdue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(
                    "Payment info unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OrderPaymentProofContent(
    order: OpticalOrder,
    onMessageClinic: (String) -> Unit,
) {
    val proof = order.paymentProof
    val proofRejected = order.paymentProofStatus == PaymentProofStatus.REJECTED ||
        proof?.status == PaymentProofStatus.REJECTED
    val rejectionReason = order.paymentProofRejectionReason ?: proof?.rejectionReason
    val contactClinic: (() -> Unit)? = if (proofRejected) {
        { onMessageClinic(paymentProofContactDraft(order.orderNumber, rejectionReason)) }
    } else {
        null
    }

    if (proof != null) {
        ExistingProofCard(
            proof = proof.copy(
                status = if (proofRejected) PaymentProofStatus.REJECTED else proof.status,
                rejectionReason = rejectionReason,
            ),
            onContactClinic = contactClinic,
        )
    } else if (order.paymentProofStatus != PaymentProofStatus.NOT_SUBMITTED) {
        PaymentProofStatusCard(
            status = order.paymentProofStatus,
            rejectionReason = rejectionReason?.takeIf(String::isNotBlank),
            paymentMethod = order.paymentProofMethod,
            onContactClinic = contactClinic,
        )
    }
}

private fun paymentProofContactDraft(orderNumber: String, rejectionReason: String?): String {
    val reason = rejectionReason?.takeIf(String::isNotBlank)
        ?: "the clinic could not verify the proof"
    return "Hi, I need help with order $orderNumber. My payment proof was rejected: $reason. What should I do next?"
}

@Composable
private fun PendingPaymentContent(
    order: OpticalOrder,
    selectedPaymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    selectedProof: SelectedPaymentProof?,
    pickerErrorMessage: String?,
    uploadState: ProofUploadState,
    onPickProof: (() -> Unit)?,
    onPaymentProofSubmit: ((String, String, String, SelectedPaymentProof) -> Unit)?,
    onPaymentWindowExpired: () -> Unit,
    onRefresh: () -> Unit,
    onClearPaymentProofError: () -> Unit,
) {
    if (order.paymentProofStatus != PaymentProofStatus.NOT_SUBMITTED ||
        order.paymentProof?.status == PaymentProofStatus.REJECTED
    ) return

    val instructions = order.paymentInstructions
    if (instructions == null) {
        PaymentInstructionsUnavailableCard(onRefresh = onRefresh)
        return
    }

    val paymentDeadline = order.paymentExpiresAt ?: instructions.paymentExpiresAt
    var paymentWindowExpired by remember(paymentDeadline) {
        mutableStateOf(paymentSecondsRemaining(paymentDeadline)?.let { it <= 0L } ?: true)
    }
    if (paymentDeadline != null) {
        PaymentDeadlineCard(
            expiresAt = paymentDeadline,
            onExpired = {
                paymentWindowExpired = true
                onPaymentWindowExpired()
            },
        )
    }

    if (paymentWindowExpired) {
        if (paymentDeadline == null) {
            PaymentInstructionsUnavailableCard(onRefresh = onRefresh)
        }
        return
    }

    val effectivePaymentMethod = instructions.availableMethods
        .firstOrNull { it.method == selectedPaymentMethod }
        ?.method
        ?: instructions.method
    PaymentInstructionsCard(
        instructions = instructions,
        selectedMethod = effectivePaymentMethod,
        onMethodSelected = onPaymentMethodSelected,
    )

    if (onPickProof != null && onPaymentProofSubmit != null) {
        val selectedMethodLabel = instructions.availableMethods
            .firstOrNull { it.method == effectivePaymentMethod }
            ?.label
            ?: instructions.label
        PaymentProofForm(
            selectedProof = selectedProof,
            onPickProof = onPickProof,
            paymentMethod = effectivePaymentMethod,
            paymentMethodLabel = selectedMethodLabel,
            onSubmit = onPaymentProofSubmit,
            uploadState = uploadState,
            onClearError = onClearPaymentProofError,
            canSubmit = true,
            pickerErrorMessage = pickerErrorMessage,
        )
    }
}

@Composable
internal fun OrderItemImage(
    imagePath: String?,
    description: String,
) {
    val imageUrl = imagePath
        ?.takeIf(String::isNotBlank)
        ?.let(::buildImageUrl)
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val imageLoaded = imageState is AsyncImagePainter.State.Success
    val placeholderDescription = when {
        imageUrl == null -> "$description image unavailable"
        imageState is AsyncImagePainter.State.Error -> "$description image unavailable"
        else -> "Loading $description image"
    }

    Surface(
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        if (!imageLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = placeholderDescription },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FaceRetouchingNatural,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                        tint = EyecareColors.current.accentText,
                    )
                }
            }
        }
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$description image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(if (imageLoaded) 1f else 0f),
                contentScale = ContentScale.Fit,
                onState = { imageState = it },
            )
        }
    }
}

private data class OrderStatusGuidanceCopy(val title: String, val message: String, val icon: ImageVector)

@Composable
private fun OrderStatusGuidance(status: OpticalOrderStatus, onRefresh: () -> Unit) {
    val copy = when (status) {
        OpticalOrderStatus.PENDING_PAYMENT -> OrderStatusGuidanceCopy(
            title = "Awaiting payment",
            message = "Your order has been accepted. Please complete payment to proceed.",
            icon = Icons.Outlined.Info,
        )
        OpticalOrderStatus.PAYMENT_REVIEW -> OrderStatusGuidanceCopy(
            title = "Payment under review",
            message = "Your payment proof is being reviewed by the clinic.",
            icon = Icons.Outlined.Info,
        )
        OpticalOrderStatus.QUEUED -> OrderStatusGuidanceCopy(
            title = "Order confirmed",
            message = "Your order is waiting to be prepared.",
            icon = Icons.Outlined.Info,
        )
        OpticalOrderStatus.IN_PROGRESS -> OrderStatusGuidanceCopy(
            title = "Order processing",
            message = "Your order is being prepared.",
            icon = Icons.Outlined.Info,
        )
        OpticalOrderStatus.READY_FOR_DISPENSING -> OrderStatusGuidanceCopy(
            title = "Ready for pickup",
            message = "Your order is ready. Visit the clinic to pick it up.",
            icon = Icons.Outlined.CheckCircle,
        )
        OpticalOrderStatus.DISPENSED -> OrderStatusGuidanceCopy(
            title = "Order completed",
            message = "This order has been completed and picked up.",
            icon = Icons.Outlined.CheckCircle,
        )
        OpticalOrderStatus.CANCELLED -> OrderStatusGuidanceCopy(
            title = "Order cancelled",
            message = "This order is no longer active.",
            icon = Icons.Outlined.Info,
        )
        OpticalOrderStatus.UNKNOWN -> OrderStatusGuidanceCopy(
            title = "Status unavailable",
            message = "We couldn't load the latest order status.",
            icon = Icons.Outlined.Info,
        )
    }
    val isActive = status == OpticalOrderStatus.QUEUED ||
        status == OpticalOrderStatus.IN_PROGRESS ||
        status == OpticalOrderStatus.READY_FOR_DISPENSING

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = copy.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = EyecareColors.current.accentText,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = copy.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (status == OpticalOrderStatus.UNKNOWN) {
                    TextButton(onClick = onRefresh) {
                        Text("Refresh order")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderTracker(status: OpticalOrderStatus) {
    val tracker = computeOrderTracker(status)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Order progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tracker.steps.forEachIndexed { index, (step, completed) ->
                    val isActive = step == tracker.activeStep
                    val stepLabel = trackerStepLabel(step)
                    val accessibleStepState = trackerStepAccessibilityStateLabel(status, step, completed, tracker.activeStep)
                    val (fillColor, textColor, weight) = when {
                        isActive -> Triple(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                            FontWeight.Bold,
                        )
                        completed -> Triple(
                            EyecareColors.current.accentText.copy(alpha = 0.14f),
                            EyecareColors.current.accentText,
                            FontWeight.SemiBold,
                        )
                        else -> Triple(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            FontWeight.Normal,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clearAndSetSemantics {
                                contentDescription = "$stepLabel, $accessibleStepState"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape,
                            color = fillColor,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor,
                                    fontWeight = weight,
                                )
                            }
                        }
                    }
                    if (index < tracker.steps.lastIndex) {
                        Surface(
                            modifier = Modifier.width(24.dp).height(2.dp),
                            shape = RoundedCornerShape(50),
                            color = if (tracker.steps[index + 1].second) {
                                EyecareColors.current.accentText.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ) {}
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                tracker.steps.forEachIndexed { index, (step, _) ->
                    Column(
                        modifier = Modifier.weight(1f).clearAndSetSemantics {},
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = trackerStepLabel(step),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                    if (index < tracker.steps.lastIndex) {
                        Spacer(Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}

private fun trackerStepLabel(step: TrackerStep): String = when (step) {
    TrackerStep.CONFIRMED -> "Confirmed"
    TrackerStep.PROCESSING -> "Processing"
    TrackerStep.READY -> "Ready for pickup"
    TrackerStep.COMPLETED -> "Completed"
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueWeight: FontWeight = FontWeight.SemiBold,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = valueWeight)
    }
}
