package com.eyecare.app.presentation.orders

import androidx.compose.foundation.BorderStroke
import com.eyecare.app.presentation.common.buildImageUrl
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderRequestScreen(
    productId: Int,
    variantId: Int,
    onBack: () -> Unit,
    onOrderSubmitted: (orderId: Int) -> Unit,
) {
    val viewModel = hiltViewModel<OrderRequestViewModel, OrderRequestViewModel.Factory> {
        it.create(productId, variantId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is OrderRequestUiState.Submitted) {
            onOrderSubmitted((uiState as OrderRequestUiState.Submitted).order.id)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Order Request") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        when (val state = uiState) {
            is OrderRequestUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is OrderRequestUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
            is OrderRequestUiState.Submitted -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator() // Brief flash before navigation
            }
            is OrderRequestUiState.Ready -> OrderRequestContent(state = state, viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderRequestContent(
    state: OrderRequestUiState.Ready,
    viewModel: OrderRequestViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Frame info card
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val imageRef = state.product.images.firstOrNull { it.isPrimary }?.path
                    ?: state.product.images.firstOrNull()?.path
                if (imageRef != null) {
                    val url = if (imageRef.startsWith("http")) imageRef
                    else buildImageUrl(imageRef)
                    AsyncImage(
                        model = url,
                        contentDescription = state.product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp).let {
                            it // clip handled by Card
                        },
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.product.brand.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.product.name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(state.selectedVariant.name, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₱${state.selectedVariant.price}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Non-prescription toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Non-Prescription", style = MaterialTheme.typography.titleMedium)
                Text("Frames only, no lenses", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = state.isNonPrescription,
                onCheckedChange = viewModel::toggleNonPrescription,
            )
        }

        // Lens type selector (hidden when non-prescription)
        if (!state.isNonPrescription) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = state.selectedLensType?.label ?: "Select lens type",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Lens Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    isError = state.error != null && state.selectedLensType == null,
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    LensType.entries.forEach { lens ->
                        DropdownMenuItem(
                            text = { Text(lens.label) },
                            onClick = { viewModel.selectLensType(lens); expanded = false },
                        )
                    }
                }
            }
        }

        // Quantity selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Quantity", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { viewModel.setQuantity(state.quantity - 1) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease",
                        modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Text(state.quantity.toString(), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(40.dp).padding(horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Surface(
                    onClick = { viewModel.setQuantity(state.quantity + 1) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase",
                        modifier = Modifier.padding(8.dp).size(20.dp))
                }
            }
        }

        // Link appointment dropdown (optional)
        if (state.appointments.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            val linked = state.appointments.firstOrNull { it.id == state.linkedAppointmentId }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = linked?.let { "${it.visitReason.replace("_", " ")} — ${it.scheduledAt.take(10)}" }
                        ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Link Appointment (optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("None") },
                        onClick = { viewModel.linkAppointment(null); expanded = false })
                    state.appointments.forEach { appt ->
                        DropdownMenuItem(
                            text = { Text("${appt.visitReason.replace("_", " ")} — ${appt.scheduledAt.take(10)}") },
                            onClick = { viewModel.linkAppointment(appt.id); expanded = false },
                        )
                    }
                }
            }
        }

        // Error message
        if (state.error != null) {
            Text(state.error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        // Submit button
        Button(
            onClick = viewModel::submit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp))
            else Text("Submit Order", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(96.dp))
    }
}




