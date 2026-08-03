package com.eyecare.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.LinkedPatient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    patient: LinkedPatient?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient profile") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        if (patient == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Patient profile unavailable",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = patient.fullName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            PatientDetailsCard(patient)
        }
    }
}

@Composable
private fun PatientDetailsCard(patient: LinkedPatient) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            PatientDetailRow("Patient number", patient.patientNumber)
            patient.dateOfBirth?.takeIf(String::isNotBlank)?.let {
                PatientDetailDivider()
                PatientDetailRow("Date of birth", it)
            }
            patient.gender?.takeIf(String::isNotBlank)?.let {
                PatientDetailDivider()
                PatientDetailRow("Gender", it)
            }
            patient.occupation?.takeIf(String::isNotBlank)?.let {
                PatientDetailDivider()
                PatientDetailRow("Occupation", it)
            }
            patient.address?.takeIf(String::isNotBlank)?.let {
                PatientDetailDivider()
                PatientDetailRow("Address", it)
            }
            patient.phone?.takeIf(String::isNotBlank)?.let {
                PatientDetailDivider()
                PatientDetailRow("Phone", it)
            }
            patient.contactEmail?.takeIf(String::isNotBlank)?.let {
                PatientDetailDivider()
                PatientDetailRow("Email", it)
            }
        }
    }
}

@Composable
private fun PatientDetailRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PatientDetailDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
