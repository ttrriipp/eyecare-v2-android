package com.eyecare.app.presentation.accessories

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.AccessoryCart
import com.eyecare.app.domain.model.DiscountProofUpload
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoryCheckoutScreen(
    cart: AccessoryCart,
    selectedDiscount: String,
    checkoutState: CheckoutUiState,
    onDiscountSelect: (String) -> Unit,
    onSubmit: (DiscountProofUpload?) -> Unit,
    onRetryDiscountProofUpload: (DiscountProofUpload) -> Unit = {},
    onViewRequest: (Int) -> Unit,
    onViewRequests: () -> Unit,
    onBack: () -> Unit,
    onBrowseAccessories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedProof by remember { mutableStateOf<SelectedDiscountProof?>(null) }
    var proofPickerError by remember { mutableStateOf<String?>(null) }
    val proofPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedProof?.file?.delete()
        selectedProof = null
        proofPickerError = null
        if (uri != null) {
            val result = prepareDiscountProof(context, uri)
            selectedProof = result.proof
            proofPickerError = result.errorMessage
        }
    }

    DisposableEffect(selectedProof) {
        val proofFile = selectedProof?.file
        onDispose { proofFile?.delete() }
    }

    LaunchedEffect(selectedDiscount) {
        if (selectedDiscount.equals("none", ignoreCase = true)) {
            selectedProof?.file?.delete()
            selectedProof = null
            proofPickerError = null
        }
    }

    LaunchedEffect(checkoutState) {
        if (checkoutState is CheckoutUiState.Success) {
            selectedProof?.file?.delete()
            selectedProof = null
            proofPickerError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review order request") },
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
        when (val state = checkoutState) {
            is CheckoutUiState.Submitting -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Submitting your request...")
                }
            }
            is CheckoutUiState.UploadingDiscountProof -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Request created. Sending discount proof...")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Keep this screen open while your proof uploads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is CheckoutUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Request submitted!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.proofSubmitted) {
                            "Your request and discount proof were submitted. The clinic will review both; no payment or stock hold occurs until the clinic confirms availability."
                        } else {
                            "The clinic will review your request. No payment or stock hold occurs until the clinic confirms availability."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { onViewRequest(state.requestId) }) {
                        Text("View request details")
                    }
                }
            }
            is CheckoutUiState.ProofUploadError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Your request was created",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    if (selectedProof != null) {
                        Button(
                            onClick = {
                                selectedProof?.let { proof ->
                                    onRetryDiscountProofUpload(proof.toDiscountProofUpload())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Retry proof upload")
                        }
                    }
                    TextButton(onClick = { onViewRequest(state.requestId) }) {
                        Text("Open request details")
                    }
                }
            }
            is CheckoutUiState.Error,
            is CheckoutUiState.Idle -> {
                val errorState = state as? CheckoutUiState.Error
                if (cart.isEmpty) {
                    EmptyContent(
                        message = "Your cart is empty. Browse accessories to add items before reviewing a request.",
                        actionLabel = "Back to catalog",
                        onAction = onBrowseAccessories,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Items (${cart.itemCount})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                TextButton(onClick = onBack) { Text("Edit cart") }
                            }

                            cart.items.forEach { item ->
                                val imageUrl = item.imagePath
                                    ?.takeIf(String::isNotBlank)
                                    ?.let(::buildImageUrl)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ShoppingBag,
                                                contentDescription = null,
                                                tint = EyecareColors.current.accentText,
                                                modifier = Modifier.size(28.dp),
                                            )
                                            if (imageUrl != null) {
                                                AsyncImage(
                                                    model = imageUrl,
                                                    contentDescription = "${item.productName} product image",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                item.productName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                item.variantName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "Qty ${item.quantity}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = EyecareColors.current.accentText,
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                pesoFormat.format(item.unitPrice * BigDecimal(item.quantity)),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = EyecareColors.current.accentText,
                                            )
                                            Text(
                                                "${pesoFormat.format(item.unitPrice)} each",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    EyecareColors.current.accentText.copy(alpha = 0.24f),
                                ),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Text(
                                            "Estimated total before approved discount",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            pesoFormat.format(cart.estimatedTotal),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = EyecareColors.current.accentText,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "This estimate does not include an approved discount.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Text(
                                "Discount request (optional)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "The clinic will review your eligibility and may approve or decline the request.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            listOf(
                                "none" to "No discount",
                                "senior_citizen" to "Senior Citizen",
                                "pwd" to "PWD (Person with Disability)",
                            ).forEach { (value, label) ->
                                val selected = selectedDiscount == value
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 64.dp)
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = {
                                                if (selectedDiscount != value) {
                                                    selectedProof?.file?.delete()
                                                    selectedProof = null
                                                    proofPickerError = null
                                                }
                                                onDiscountSelect(value)
                                            },
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) EyecareColors.current.accentText
                                        else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = EyecareColors.current.accentText,
                                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
                                        )
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            if (!selectedDiscount.equals("none", ignoreCase = true)) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        EyecareColors.current.accentText.copy(alpha = 0.24f),
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            "Discount proof",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            "Choose a JPG or PNG image that shows your discount eligibility. Maximum size: 10 MB.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        selectedProof?.let { proof ->
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    AsyncImage(
                                                        model = proof.file,
                                                        contentDescription = "Preview of ${proof.displayName}",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                }
                                                Text(
                                                    proof.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                proofPickerError = null
                                                proofPicker.launch(arrayOf("image/jpeg", "image/png"))
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp),
                                        ) {
                                            Text(
                                                if (selectedProof == null) {
                                                    "Choose proof image"
                                                } else {
                                                    "Choose a different image"
                                                },
                                            )
                                        }
                                        proofPickerError?.let { message ->
                                            Text(
                                                message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                                if (selectedProof == null && proofPickerError == null) {
                                    Text(
                                        "A proof image is required to request this discount.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(
                                    1.dp,
                                    EyecareColors.current.accentText.copy(alpha = 0.12f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = EyecareColors.current.accentText,
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "Before you submit",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            "Submitting sends a request for clinic review. No payment or stock hold occurs yet. The clinic will confirm availability, eligibility, and final price.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                errorState?.let { error ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                error.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                            if (error.isConflict) {
                                                TextButton(
                                                    onClick = onViewRequests,
                                                    modifier = Modifier.align(Alignment.Start),
                                                ) {
                                                    Text("View my requests")
                                                }
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Estimated total",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        pesoFormat.format(cart.estimatedTotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EyecareColors.current.accentText,
                                    )
                                }
                                Button(
                                    onClick = {
                                        onSubmit(
                                            selectedProof
                                                ?.takeIf { !selectedDiscount.equals("none", ignoreCase = true) }
                                                ?.toDiscountProofUpload(),
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp),
                                    enabled = cart.items.isNotEmpty() &&
                                        errorState?.isConflict != true &&
                                        (selectedDiscount.equals("none", ignoreCase = true) || selectedProof != null),
                                ) {
                                    Text(
                                        when {
                                            errorState != null -> "Try again"
                                            selectedDiscount.equals("none", ignoreCase = true) -> "Submit order request"
                                            else -> "Submit request and proof"
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun SelectedDiscountProof.toDiscountProofUpload() = DiscountProofUpload(
    imageFile = file,
    mimeType = mimeType,
    width = width,
    height = height,
    deleteAfterUpload = true,
)
