package com.eyecare.app.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.repository.ChatRepository
import com.eyecare.app.presentation.appointments.AppointmentDetailScreen
import com.eyecare.app.presentation.appointments.AppointmentListScreen
import com.eyecare.app.presentation.appointments.booking.BookAppointmentScreen
import com.eyecare.app.presentation.auth.LoginScreen
import com.eyecare.app.presentation.auth.RegisterScreen
import com.eyecare.app.presentation.ar.ArTryOnScreen
import com.eyecare.app.presentation.feedback.FeedbackScreen
import com.eyecare.app.presentation.intake.PatientIntakeScreen
import com.eyecare.app.presentation.prescriptions.PrescriptionDetailScreen
import com.eyecare.app.presentation.prescriptions.PrescriptionListScreen
import com.eyecare.app.presentation.quotations.QuotationListScreen
import com.eyecare.app.presentation.quotations.QuotationDetailScreen
import com.eyecare.app.presentation.joborders.JobOrderListScreen
import com.eyecare.app.presentation.joborders.JobOrderDetailScreen
import com.eyecare.app.presentation.invoices.InvoiceListScreen
import com.eyecare.app.presentation.invoices.InvoiceDetailScreen
import com.eyecare.app.presentation.frames.FrameDetailScreen
import com.eyecare.app.presentation.frames.FrameListScreen
import com.eyecare.app.presentation.reservations.CreateFrameReservationScreen
import com.eyecare.app.presentation.reservations.FrameReservationListScreen
import com.eyecare.app.presentation.home.HomeScreen
import com.eyecare.app.presentation.profile.EditProfileScreen
import com.eyecare.app.presentation.messaging.ChatScreen
import com.eyecare.app.presentation.profile.ProfileScreen

@Composable
fun EyecareNavGraph(
    tokenManager: TokenManager,
    chatRepository: ChatRepository,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = if (tokenManager.getToken() != null) MainGraph else AuthGraph
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest: NavDestination? = backStackEntry?.destination

    // Unread message count
    var unreadCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        chatRepository.getConversation().onSuccess { unreadCount = it.unreadCount }
    }
    // Refresh when navigating back from Chat
    LaunchedEffect(currentDest?.route) {
        if (currentDest?.route?.contains("Chat") == false) {
            chatRepository.getConversation().onSuccess { unreadCount = it.unreadCount }
        }
    }

    // Hide bottom nav on auth screens and chat
    val showBottomNav = currentDest?.route?.let { route ->
        !route.contains("Login") && !route.contains("Register") &&
            !route.contains("Chat") && !route.contains("AppointmentDetail") &&
            !route.contains("BookAppointment") &&
            !route.contains("ArTryOn") && !route.contains("FrameDetail") &&
            !route.contains("Prescription") && !route.contains("Feedback") &&
            !route.contains("EditProfile") &&
            !route.contains("PatientIntake") && !route.contains("Quotation") &&
            !route.contains("JobOrder") && !route.contains("Invoice")
    } ?: false

    val currentRoute = if (showBottomNav && currentDest != null) when {
        currentDest.route?.contains("Home") == true -> Home
        currentDest.route?.contains("Frames") == true -> Frames
        currentDest.route?.contains("Appointments") == true -> Appointments
        currentDest.route?.contains("Profile") == true -> Profile
        else -> Home
    } else null

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn() + slideInHorizontally { it / 6 } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it / 6 } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it / 6 } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 6 } },
        ) {
                // Auth graph
                navigation<AuthGraph>(startDestination = Login) {
                    composable<Login> {
                        LoginScreen(
                            onNavigateToRegister = { navController.navigate(Register) },
                            onLoginSuccess = {
                                navController.navigate(MainGraph) {
                                    popUpTo(AuthGraph) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable<Register> {
                        RegisterScreen(
                            onNavigateToLogin = { navController.popBackStack() },
                            onRegisterSuccess = {
                                navController.navigate(MainGraph) {
                                    popUpTo(AuthGraph) { inclusive = true }
                                }
                            },
                        )
                    }
                }

                // Main graph
                navigation<MainGraph>(startDestination = Home) {
                    composable<Home> {
                        HomeScreen(
                            onNavigateToAppointments = {
                                navController.navigate(Appointments) {
                                    popUpTo<MainGraph> {
                                        saveState = true
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToBooking = { navController.navigate(BookAppointment) },
                            onNavigateToFrames = {
                                navController.navigate(Frames) {
                                    popUpTo<MainGraph> {
                                        saveState = true
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToFrameDetail = { navController.navigate(FrameDetail(it)) },
                        )
                    }
                    composable<Frames> {
                        FrameListScreen(
                            onNavigateToDetail = { id -> navController.navigate(FrameDetail(id)) },
                        )
                    }
                    composable<FrameDetail> { backStackEntry ->
                        val route = backStackEntry.toRoute<FrameDetail>()
                        FrameDetailScreen(
                            frameId = route.frameId,
                            onBack = { navController.popBackStack() },
                            onNavigateToAr = { fId, vId -> navController.navigate(ArTryOn(fId, vId)) },
                            onNavigateToReserve = { fId, vId -> navController.navigate(CreateFrameReservation(fId, vId)) },
                        )
                    }
                    composable<CreateFrameReservation> { backStackEntry ->
                        val route = backStackEntry.toRoute<CreateFrameReservation>()
                        CreateFrameReservationScreen(
                            frameId = route.frameId,
                            variantId = route.variantId,
                            onBack = { navController.popBackStack() },
                            onSuccess = { navController.popBackStack() },
                        )
                    }
                    composable<FrameReservationList> {
                        FrameReservationListScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<ArTryOn> { backStackEntry ->
                        val route = backStackEntry.toRoute<ArTryOn>()
                        ArTryOnScreen(
                            frameId = route.frameId,
                            initialVariantId = route.variantId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<FeedbackSubmit> { back ->
                        val route = back.toRoute<FeedbackSubmit>()
                        FeedbackScreen(
                            appointmentId = route.appointmentId,
                            onBack = { navController.popBackStack() },
                            onSubmitted = { navController.popBackStack() },
                        )
                    }
                    composable<PrescriptionList> {
                        PrescriptionListScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDetail = { navController.navigate(PrescriptionDetail(it)) },
                        )
                    }
                    composable<PrescriptionDetail> { back ->
                        val route = back.toRoute<PrescriptionDetail>()
                        PrescriptionDetailScreen(prescriptionId = route.prescriptionId, onBack = { navController.popBackStack() })
                    }
                    composable<QuotationList> {
                        QuotationListScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDetail = { navController.navigate(QuotationDetail(it)) },
                        )
                    }
                    composable<QuotationDetail> { back ->
                        val route = back.toRoute<QuotationDetail>()
                        QuotationDetailScreen(quotationId = route.quotationId, onBack = { navController.popBackStack() })
                    }
                    composable<JobOrderList> {
                        JobOrderListScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDetail = { navController.navigate(JobOrderDetail(it)) },
                        )
                    }
                    composable<JobOrderDetail> { back ->
                        val route = back.toRoute<JobOrderDetail>()
                        JobOrderDetailScreen(jobOrderId = route.jobOrderId, onBack = { navController.popBackStack() })
                    }
                    composable<InvoiceList> {
                        InvoiceListScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDetail = { navController.navigate(InvoiceDetail(it)) },
                        )
                    }
                    composable<InvoiceDetail> { back ->
                        val route = back.toRoute<InvoiceDetail>()
                        InvoiceDetailScreen(invoiceId = route.invoiceId, onBack = { navController.popBackStack() })
                    }
                    composable<Appointments> {
                        AppointmentListScreen(
                            onNavigateToDetail = { id -> navController.navigate(AppointmentDetail(id)) },
                            onNavigateToBook = { navController.navigate(BookAppointment) },
                        )
                    }
                    composable<AppointmentDetail> {
                        AppointmentDetailScreen(
                            onBack = { navController.popBackStack() },
                            onLeaveFeedback = { id -> navController.navigate(FeedbackSubmit(appointmentId = id)) },
                            onNavigateToIntake = { appointmentId -> navController.navigate(PatientIntake(appointmentId)) },
                        )
                    }
                    composable<PatientIntake> { backStackEntry ->
                        val route = backStackEntry.toRoute<PatientIntake>()
                        PatientIntakeScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<BookAppointment> {
                        BookAppointmentScreen(
                            onBack = { navController.popBackStack() },
                            onBooked = {
                                navController.navigate(Appointments) {
                                    popUpTo(BookAppointment) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable<Profile> {
                        ProfileScreen(
                            onLogout = {
                                tokenManager.clearToken()
                                onLogout()
                                navController.navigate(AuthGraph) {
                                    popUpTo(MainGraph) { inclusive = true }
                                }
                            },
                            onNavigateToPrescriptions = { navController.navigate(PrescriptionList) },
                            onNavigateToReservations = { navController.navigate(FrameReservationList) },
                            onNavigateToQuotations = { navController.navigate(QuotationList) },
                            onNavigateToJobOrders = { navController.navigate(JobOrderList) },
                            onNavigateToInvoices = { navController.navigate(InvoiceList) },
                            onNavigateToEditProfile = { navController.navigate(EditProfile) },
                            onNavigateToMessages = { navController.navigate(Chat) },
                            unreadMessageCount = unreadCount,
                        )
                    }
                    composable<EditProfile> {
                        EditProfileScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<Chat> {
                        ChatScreen(
                            onBack = { navController.popBackStack() },
                            onAppointmentClick = { navController.navigate(AppointmentDetail(it)) },
                            onOrderClick = { navController.navigate(JobOrderDetail(it)) },
                        )
                    }
                }
            }

        // Floating navbar — overlaid on content, centered horizontally, no background behind it
        if (showBottomNav && currentRoute != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                SplitBottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo<MainGraph> {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}
