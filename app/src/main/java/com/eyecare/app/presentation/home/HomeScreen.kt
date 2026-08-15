package com.eyecare.app.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.ClinicHoursDay
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.presentation.appointments.formatAppointmentDate
import com.eyecare.app.presentation.appointments.formatAppointmentTime
import com.eyecare.app.presentation.appointments.formatAppointmentTitle
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.frames.components.FrameCard
import com.eyecare.app.ui.theme.EyecareColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect

internal fun timeOfDayGreeting(time: LocalTime): String = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToBooking: () -> Unit = {},
    onNavigateToFrames: () -> Unit = {},
    onNavigateToFrameDetail: (Int) -> Unit = {},
    onNavigateToLinkAccount: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    hasActivePatientLink: Boolean = true,
    patientLinkStatus: PatientLinkStatus = PatientLinkStatus.UNLINKED,
    notificationUnreadCount: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasActivePatientLink) {
        viewModel.load(hasActivePatientLink)
    }

    PullToRefreshBox(
        isRefreshing = uiState is HomeUiState.Loading,
        onRefresh = { viewModel.refresh(hasActivePatientLink) },
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> HomeLoadingContent()

            is HomeUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = { viewModel.refresh(hasActivePatientLink) },
            )

            is HomeUiState.Success -> HomeContent(
                state = state,
                onNavigateToAppointments = onNavigateToAppointments,
                onNavigateToBooking = onNavigateToBooking,
                onNavigateToFrames = onNavigateToFrames,
                onNavigateToFrameDetail = onNavigateToFrameDetail,
                onNavigateToLinkAccount = onNavigateToLinkAccount,
                onNavigateToNotifications = onNavigateToNotifications,
                hasActivePatientLink = hasActivePatientLink,
                patientLinkStatus = patientLinkStatus,
                notificationUnreadCount = notificationUnreadCount,
            )
        }
    }
}

@Composable
fun HomeLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Loading home" }
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadingBlock(width = 96.dp, height = 14.dp)
            LoadingBlock(modifier = Modifier.fillMaxWidth(0.62f), height = 28.dp)
        }
        LoadingBlock(modifier = Modifier.fillMaxWidth(), height = 168.dp, cornerRadius = 16.dp)
        LoadingBlock(modifier = Modifier.fillMaxWidth(), height = 104.dp, cornerRadius = 16.dp)
        LoadingBlock(width = 132.dp, height = 20.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LoadingBlock(width = 148.dp, height = 196.dp, cornerRadius = 12.dp)
            LoadingBlock(width = 148.dp, height = 196.dp, cornerRadius = 12.dp)
        }
    }
}

@Composable
private fun LoadingBlock(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
    height: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun HomeContent(
    state: HomeUiState.Success,
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToBooking: () -> Unit = {},
    onNavigateToFrames: () -> Unit = {},
    onNavigateToFrameDetail: (Int) -> Unit = {},
    onNavigateToLinkAccount: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    hasActivePatientLink: Boolean = true,
    patientLinkStatus: PatientLinkStatus = PatientLinkStatus.UNLINKED,
    notificationUnreadCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = timeOfDayGreeting(LocalTime.now()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Here's what's ahead",
                    style = MaterialTheme.typography.displayLarge,
                )
            }
            NotificationBell(
                unreadCount = notificationUnreadCount,
                onClick = onNavigateToNotifications,
            )
        }

        if (hasActivePatientLink) {
            state.nextAppointment?.let { appointment ->
                VisitTicket(appointment = appointment, onClick = onNavigateToAppointments)
            } ?: BookingInvitation(onClick = onNavigateToBooking)
        } else {
            AccountLinkInvitation(linkStatus = patientLinkStatus, onClick = onNavigateToLinkAccount)
        }

        if (state.clinicHours.isNotEmpty()) {
            ClinicHoursCard(clinicHours = state.clinicHours)
        }

        if (state.featuredFrames.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Featured Frames",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Browse our AR-ready collection",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = onNavigateToFrames,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        ) {
                            Text("See all")
                        }
                    }
                    HomeFrameShelf(
                        frames = state.featuredFrames,
                        onFrameClick = onNavigateToFrameDetail,
                    )
                }
            }
        }
    }
}

/** Backend's Carbon weekday convention: 0 = Sunday ... 6 = Saturday. */
private fun LocalDate.toCarbonWeekday(): Int = dayOfWeek.value % 7

private val clinicTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

private fun formatClinicTime(hhmm: String): String = runCatching {
    LocalTime.parse(hhmm).format(clinicTimeFormatter)
}.getOrDefault(hhmm)

private fun ClinicHoursDay.rangeLabel(): String =
    if (enabled && openTime != null && closeTime != null) {
        "${formatClinicTime(openTime)} – ${formatClinicTime(closeTime)}"
    } else {
        "Closed"
    }

@Composable
private fun ClinicHoursCard(clinicHours: List<ClinicHoursDay>) {
    var expanded by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val todayHours = remember(clinicHours, today) {
        clinicHours.firstOrNull { it.weekday == today.toCarbonWeekday() }
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val todayDayName = todayHours?.dayName
        ?: today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, locale)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = EyecareColors.current.accentText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Clinic Hours",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = todayHours?.let { "$todayDayName · ${it.rangeLabel()}" } ?: todayDayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    clinicHours.forEach { day ->
                        val isToday = day.weekday == todayHours?.weekday
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = day.dayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isToday) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                text = day.rangeLabel(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitTicket(
    appointment: AppointmentV1,
    onClick: () -> Unit,
) {
    val formattedDate = formatAppointmentDate(appointment.scheduledAt)
    val dateParts = formattedDate.replace(",", "").split(" ")
    val month = dateParts.getOrNull(0)?.uppercase(Locale.US) ?: "VISIT"
    val day = dateParts.getOrNull(1) ?: "—"
    val status = appointment.status.name
        .replace("_", " ")
        .lowercase(Locale.US)
        .split(" ")
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EyecareColors.current.visitNavy),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(64.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = month,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = day,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "YOUR NEXT VISIT",
                    style = MaterialTheme.typography.labelMedium,
                    color = EyecareColors.current.onVisitNavy.copy(alpha = 0.72f),
                    letterSpacing = 0.8.sp,
                )
                Text(
                    text = formatAppointmentTitle(appointment.appointmentType),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.onVisitNavy,
                )
                Surface(
                    color = EyecareColors.current.onVisitNavy.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = EyecareColors.current.onVisitNavy,
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EyecareColors.current.onVisitNavy,
                )
                Text(
                    text = formatAppointmentTime(appointment.scheduledAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EyecareColors.current.onVisitNavy.copy(alpha = 0.82f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "View appointments",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingInvitation(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = EyecareColors.current.accentText,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "PLAN YOUR NEXT VISIT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = "Make time for clearer, more comfortable vision.",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Choose a visit reason and a clinic time that works for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onClick,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("Book an appointment")
            }
        }
    }
}

@Composable
private fun AccountLinkInvitation(
    linkStatus: PatientLinkStatus,
    onClick: () -> Unit,
) {
    val isPending = linkStatus == PatientLinkStatus.PENDING_REVIEW
    val tintColor = if (isPending) EyecareColors.current.statusPending else EyecareColors.current.accentText
    val eyebrow = if (isPending) "CLINIC REVIEW PENDING" else "ACCOUNT NOT LINKED"
    val headline = if (isPending) {
        "Your clinic link is under review"
    } else {
        "Connect your clinic record"
    }
    val body = if (isPending) {
        "We're reviewing your request. You can check its status or link with an invitation code instead."
    } else {
        "Link your account with an invitation code, or ask the clinic to review your request, to unlock appointments, prescriptions, and orders."
    }
    val buttonLabel = if (isPending) "View request status" else "Link your account"

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = tintColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPending) Icons.Outlined.Schedule else Icons.Outlined.Bookmark,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = tintColor,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.accentText,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = EyecareColors.current.accentText,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeFrameShelf(
    frames: List<Frame>,
    onFrameClick: (Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(frames, key = { it.id }) { frame ->
            FrameCard(
                frame = frame,
                onClick = { onFrameClick(frame.id) },
                modifier = Modifier.width(148.dp),
            )
        }
    }
}

@Composable
private fun NotificationBell(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = if (unreadCount > 0) {
                        "$unreadCount unread notifications"
                    } else {
                        "Notifications"
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (unreadCount > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .widthIn(min = 18.dp)
                    .height(18.dp)
                    .semantics {
                        contentDescription = "$unreadCount unread notifications"
                    },
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}



