package com.eyecare.app.presentation.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

// ── Clinic hours: 9:00 AM – 6:30 PM ──────────────────────────────────────────
private fun isValidClinicTime(hour12: Int, minute: Int, isPm: Boolean): Boolean {
    val hour24 = when {
        !isPm && hour12 == 12 -> 0
        isPm && hour12 != 12  -> hour12 + 12
        else                  -> hour12
    }
    return (hour24 * 60 + minute) in (9 * 60)..(18 * 60 + 30)
}

/**
 * Modal bottom sheet that lets the customer pick a new date + time for an existing
 * appointment and submit it via [onConfirm]. Mirrors the booking wizard's Step2/Step3
 * date+time pickers, condensed into a single sheet (no visit reason / notes re-entry).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleBottomSheet(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (scheduledAt: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var tabIndex by remember { mutableStateOf(0) } // 0 = date, 1 = time
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                "Reschedule Appointment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            SecondaryTabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("Date") },
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("Time") },
                )
            }

            Spacer(Modifier.height(16.dp))

            if (tabIndex == 0) {
                RescheduleDateStep(
                    selectedDate = selectedDate,
                    onSelectDate = {
                        selectedDate = it
                        tabIndex = 1
                    },
                )
            } else {
                RescheduleTimeStep(
                    onSelectTime = { selectedTime = it },
                )
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val date = selectedDate ?: return@Button
                    val time = selectedTime ?: return@Button
                    onConfirm("${date}T$time:00")
                },
                enabled = !isSubmitting && selectedDate != null && selectedTime != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Confirm Reschedule")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleDateStep(
    selectedDate: String?,
    onSelectDate: (String) -> Unit,
) {
    val today = remember { LocalDate.now() }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.let {
            runCatching { LocalDate.parse(it).toEpochDay() * 86400000L }.getOrNull()
        } ?: (today.toEpochDay() * 86400000L),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = java.time.Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                return !date.isBefore(today) && date.dayOfWeek != DayOfWeek.SUNDAY
            }
        },
    )

    Column {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.fillMaxWidth(),
            title = null,
            headline = null,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                navigationContentColor = MaterialTheme.colorScheme.onSurface,
                yearContentColor = MaterialTheme.colorScheme.onSurface,
                currentYearContentColor = MaterialTheme.colorScheme.primary,
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = Color.White,
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                selectedDayContentColor = Color.White,
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary,
                disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            ),
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val millis = datePickerState.selectedDateMillis ?: return@Button
                val date = java.time.Instant.ofEpochMilli(millis)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                onSelectDate(date.toString())
            },
            enabled = datePickerState.selectedDateMillis != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text("Next: Pick Time")
        }
    }
}

@Composable
private fun RescheduleTimeStep(
    onSelectTime: (String) -> Unit,
) {
    var isPm by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }

    fun nextHour() {
        if (!isPm && hour == 11) {
            isPm = true
            hour = 12
        } else if (isPm) {
            hour = when (hour) { 12 -> 1; 6 -> 12; else -> hour + 1 }
        } else {
            hour += 1
        }
        if (!isValidClinicTime(hour, minute, isPm)) minute = 0
    }
    fun prevHour() {
        if (isPm && hour == 12) {
            isPm = false
            hour = 11
        } else if (!isPm) {
            hour = when (hour) { 9 -> 11; else -> hour - 1 }
        } else {
            hour = when (hour) { 1 -> 12; else -> hour - 1 }
        }
        if (!isValidClinicTime(hour, minute, isPm)) minute = 0
    }
    fun nextMinute() {
        val n = if (minute >= 55) 0 else minute + 5
        if (isValidClinicTime(hour, n, isPm)) minute = n
    }
    fun prevMinute() {
        val p = if (minute <= 0) 55 else minute - 5
        if (isValidClinicTime(hour, p, isPm)) minute = p
    }
    fun switchPeriod(newIsPm: Boolean) {
        isPm = newIsPm
        hour = if (newIsPm) { if (hour in 1..6 || hour == 12) hour else 12 }
               else         { if (hour in 9..11) hour else 9 }
        if (!isValidClinicTime(hour, minute, isPm)) minute = 0
    }

    val hour24 = when {
        !isPm && hour == 12 -> 0
        isPm && hour != 12  -> hour + 12
        else                -> hour
    }
    val timeString = "%02d:%02d".format(hour24, minute)

    val primary      = MaterialTheme.colorScheme.primary
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val surfaceVar   = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Select Time", style = MaterialTheme.typography.titleMedium)
            Text(
                "9:00 AM – 6:30 PM",
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            RescheduleTimeSegment(
                value = "%02d".format(hour),
                onUp = ::nextHour,
                onDown = ::prevHour,
                primary = primary,
                onSurface = onSurface,
            )

            Text(
                ":",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = onSurface,
                modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 8.dp),
            )

            RescheduleTimeSegment(
                value = "%02d".format(minute),
                onUp = ::nextMinute,
                onDown = ::prevMinute,
                primary = primary,
                onSurface = onSurface,
            )

            Spacer(Modifier.width(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                listOf(false to "AM", true to "PM").forEach { (pm, label) ->
                    val selected = isPm == pm
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) primary else surfaceVar)
                            .clickable { switchPeriod(pm) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else onSurfaceVar,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onSelectTime(timeString) },
            enabled = isValidClinicTime(hour, minute, isPm),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text("Use This Time")
        }
    }
}

@Composable
private fun RescheduleTimeSegment(
    value: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    primary: Color,
    onSurface: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(onClick = onUp, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = onSurface,
        )
        IconButton(onClick = onDown, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = primary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
