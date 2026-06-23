package com.eyecare.app.presentation.prescriptions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.ui.theme.StatusCancelled
import com.eyecare.app.ui.theme.StatusConfirmed
import com.eyecare.app.ui.theme.StatusPending
import java.time.LocalDate

private enum class ValidityStatus { VALID, EXPIRING_SOON, EXPIRED, UNKNOWN }

private fun validityStatus(expiresAt: String?): ValidityStatus {
    if (expiresAt.isNullOrBlank()) return ValidityStatus.UNKNOWN
    val exp = runCatching { LocalDate.parse(expiresAt.take(10)) }.getOrNull() ?: return ValidityStatus.UNKNOWN
    val today = LocalDate.now()
    return when {
        exp.isBefore(today) -> ValidityStatus.EXPIRED
        exp.isBefore(today.plusDays(30)) -> ValidityStatus.EXPIRING_SOON
        else -> ValidityStatus.VALID
    }
}

// ─── List Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: PrescriptionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.listState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Prescriptions") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.RemoveRedEye, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No prescriptions on record",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is PrescriptionListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
                is PrescriptionListUiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.prescriptions, key = { it.id }) { prescription ->
                        PrescriptionCard(
                            prescription = prescription,
                            onClick = { onNavigateToDetail(prescription.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrescriptionCard(prescription: Prescription, onClick: () -> Unit) {
    val validity = validityStatus(prescription.expiresAt)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row: date + validity badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Prescribed ${prescription.prescribedAt.take(10)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    prescription.expiresAt?.take(10)?.let {
                        Text(
                            "Expires $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ValidityBadge(validity)
            }

            // OD / OS sphere preview
            val odText = prescription.odSphere?.let { "OD $it" }
            val osText = prescription.osSphere?.let { "OS $it" }
            if (odText != null || osText != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    odText?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    osText?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidityBadge(status: ValidityStatus) {
    val (label, color) = when (status) {
        ValidityStatus.VALID -> "Valid" to StatusConfirmed
        ValidityStatus.EXPIRING_SOON -> "Expiring Soon" to StatusPending
        ValidityStatus.EXPIRED -> "Expired" to StatusCancelled
        ValidityStatus.UNKNOWN -> return
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color.copy(alpha = 0.12f)),
        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = color),
    )
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
            windowInsets = WindowInsets(0),
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
            is PrescriptionDetailUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
            is PrescriptionDetailUiState.Success -> {
                val p = state.prescription
                val validity = validityStatus(p.expiresAt)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ── Status card ───────────────────────────────────────
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Prescribed ${p.prescribedAt.take(10)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                p.expiresAt?.take(10)?.let {
                                    Text("Expires $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            ValidityBadge(validity)
                        }
                    }

                    // ── Expiry warning ────────────────────────────────────
                    if (validity == ValidityStatus.EXPIRED || validity == ValidityStatus.EXPIRING_SOON) {
                        val (msg, color) = if (validity == ValidityStatus.EXPIRED)
                            "This prescription has expired. Book an eye exam to get an updated one." to StatusCancelled
                        else
                            "This prescription expires within 30 days." to StatusPending
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Warning, contentDescription = null,
                                tint = color, modifier = Modifier.size(18.dp))
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }

                    // ── OD / OS grid ──────────────────────────────────────
                    EyeCard(
                        label = "OD — Right Eye",
                        sphere = p.odSphere, cylinder = p.odCylinder,
                        axis = p.odAxis, add = p.odAdd,
                    )
                    EyeCard(
                        label = "OS — Left Eye",
                        sphere = p.osSphere, cylinder = p.osCylinder,
                        axis = p.osAxis, add = p.osAdd,
                    )

                    // ── PD ────────────────────────────────────────────────
                    if (!p.pd.isNullOrBlank()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("PD", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Pupillary Distance", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(p.pd, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // ── Notes ─────────────────────────────────────────────
                    if (!p.notes.isNullOrBlank()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Notes", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(p.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EyeCard(label: String, sphere: String?, cylinder: String?, axis: String?, add: String?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("SPH" to sphere, "CYL" to cylinder, "AXIS" to axis, "ADD" to add).forEach { (key, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                value ?: "—",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (value != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(key, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
