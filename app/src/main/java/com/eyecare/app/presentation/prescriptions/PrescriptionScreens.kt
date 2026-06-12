package com.eyecare.app.presentation.prescriptions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.ui.theme.StatusCancelled

// ─── List Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionListScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: PrescriptionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.listState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = uiState is PrescriptionListUiState.Loading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val state = uiState) {
            is PrescriptionListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is PrescriptionListUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No prescriptions yet.", style = MaterialTheme.typography.bodyMedium)
            }
            is PrescriptionListUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::refresh) { Text("Retry") }
                }
            }
            is PrescriptionListUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Prescriptions", style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                items(state.prescriptions, key = { it.id }) { prescription ->
                    PrescriptionCard(
                        prescription = prescription,
                        isExpired = viewModel.isExpired(prescription.expiresAt),
                        onClick = { onNavigateToDetail(prescription.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrescriptionCard(prescription: Prescription, isExpired: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) StatusCancelled.copy(alpha = 0.05f) else Color.White,
        ),
        border = if (isExpired) androidx.compose.foundation.BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.4f)) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Rx — ${prescription.prescribedAt.take(10)}", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                if (isExpired) Text("Expired", style = MaterialTheme.typography.labelMedium,
                    color = StatusCancelled)
            }
            if (!prescription.expiresAt.isNullOrBlank()) {
                Text("Expires: ${prescription.expiresAt.take(10)}", style = MaterialTheme.typography.bodySmall,
                    color = if (isExpired) StatusCancelled else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            prescription.appointmentId?.let {
                Text("Appointment #$it", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── Detail Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionDetailScreen(
    prescriptionId: Int,
    onBack: () -> Unit,
    viewModel: PrescriptionViewModel = hiltViewModel(),
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(prescriptionId) { viewModel.loadDetail(prescriptionId) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Prescription") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        when (val state = detailState) {
            is PrescriptionDetailUiState.Idle,
            is PrescriptionDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is PrescriptionDetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is PrescriptionDetailUiState.Success -> {
                val p = state.prescription
                val isExpired = viewModel.isExpired(p.expiresAt)
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Dates + expiry warning
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Prescribed: ${p.prescribedAt.take(10)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!p.expiresAt.isNullOrBlank()) {
                        Text(
                            "Expires: ${p.expiresAt.take(10)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isExpired) StatusCancelled else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isExpired) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // OD / OS table
                    EyeSection(
                        label = "OD (Right Eye)",
                        sphere = p.odSphere, cylinder = p.odCylinder,
                        axis = p.odAxis, add = p.odAdd,
                    )
                    EyeSection(
                        label = "OS (Left Eye)",
                        sphere = p.osSphere, cylinder = p.osCylinder,
                        axis = p.osAxis, add = p.osAdd,
                    )

                    // PD
                    if (!p.pd.isNullOrBlank()) {
                        LabelValueRow("PD (Pupillary Distance)", p.pd)
                    }

                    // Notes
                    if (!p.notes.isNullOrBlank()) {
                        Column {
                            Text("Notes", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(p.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(96.dp))
                }
            }
        }
    }
}

@Composable
private fun EyeSection(label: String, sphere: String?, cylinder: String?, axis: String?, add: String?) {
    Card(shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("SPH" to sphere, "CYL" to cylinder, "AXIS" to axis, "ADD" to add)
                    .filter { it.second != null }
                    .forEach { (k, v) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(k, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(v ?: "—", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
            }
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
