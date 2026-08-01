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
import com.eyecare.app.presentation.auth.PasswordRecoveryScreen
import com.eyecare.app.presentation.auth.RegisterScreen
import com.eyecare.app.presentation.auth.SessionGateScreen
import com.eyecare.app.presentation.auth.WelcomeScreen
import com.eyecare.app.presentation.account.LimitedAccountScreen
import com.eyecare.app.presentation.account.AccountSecurityScreen
import com.eyecare.app.presentation.ar.ArTryOnScreen
import com.eyecare.app.presentation.intake.PatientIntakeScreen
import com.eyecare.app.presentation.prescriptions.PrescriptionDetailScreen
import com.eyecare.app.presentation.prescriptions.PrescriptionListScreen
import com.eyecare.app.presentation.eyewear.EyewearListScreen
import com.eyecare.app.presentation.eyewear.EyewearDetailScreen
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
    val startDestination = SessionGate
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest: NavDestination? = backStackEntry?.destination

    // Unread message count — only loaded inside MainGraph
    var unreadCount by remember { mutableIntStateOf(0) }

    // Hide bottom nav on auth/limited screens and chat
    val showBottomNav = currentDest?.route?.let { route ->
        !route.contains("Login") && !route.contains("Register") &&
            !route.contains("SessionGate") && !route.contains("Welcome") &&
            !route.contains("CreateAccount") && !route.contains("RecoverPassword") &&
            !route.contains("LimitedAccount") && !route.contains("AccountSecurity") &&
            !route.contains("Chat") && !route.contains("AppointmentDetail") &&
            !route.contains("BookAppointment") && !route.contains("CreateFrameReservation") &&
            !route.contains("FrameReservation") && !route.contains("ArTryOn") && !route.contains("FrameDetail") &&
            !route.contains("Prescription") &&
            !route.contains("EditProfile") &&
            !route.contains("PatientIntake") && !route.contains("Quotation") &&
            !route.contains("JobOrder") && !route.contains("Eyewear")
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
                // SessionGate — root start destination
                composable<SessionGate> {
                    SessionGateScreen(
                        onNavigateToWelcome = {
                            navController.navigate(Welcome) {
                                popUpTo<SessionGate> { inclusive = true }
                            }
                        },
                        onNavigateToMain = {
                            navController.navigate(MainGraph) {
                                popUpTo<SessionGate> { inclusive = true }
                            }
                        },
                        onNavigateToLimited = {
                            navController.navigate(LimitedAccount) {
                                popUpTo<SessionGate> { inclusive = true }
                            }
                        },
                    )
                }

                // Welcome
                composable<Welcome> {
                    WelcomeScreen(
                        onSignIn = { navController.navigate(Login) },
                        onCreateAccount = { navController.navigate(CreateAccount) },
                    )
                }

                // Create account (V13 registration)
                composable<CreateAccount> {
                    RegisterScreen(
                        onNavigateToLogin = { navController.popBackStack() },
                        onRegisterSuccess = {
                            navController.navigate(SessionGate) {
                                popUpTo<Welcome> { inclusive = true }
                            }
                        },
                    )
                }

                // Password recovery
                composable<RecoverPassword> {
                    PasswordRecoveryScreen(
                        onRecoverySuccess = {
                            navController.navigate(SessionGate) {
                                popUpTo<Welcome> { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                // Account access graph (unlinked/pending)
                composable<LimitedAccount> {
                    LimitedAccountScreen(
                        account = com.eyecare.app.domain.model.PatientAccount(
                            id = 0, name = "", firstName = null, middleName = null, lastName = null,
                            email = null, phone = null, role = "patient", dateOfBirth = null,
                            linkStatus = com.eyecare.app.domain.model.PatientLinkStatus.UNLINKED,
                            privacyPolicyVersion = null, privacyAcceptedAt = null, linkedPatient = null,
                        ),
                        onAccountSecurity = { navController.navigate(AccountSecurity) },
                        onSignOut = {
                            navController.navigate(Welcome) {
                                popUpTo<LimitedAccount> { inclusive = true }
                            }
                        },
                        onNavigateToMain = {
                            navController.navigate(MainGraph) {
                                popUpTo<LimitedAccount> { inclusive = true }
                            }
                        },
                    )
                }

                // Account & Security (shared from limited and linked)
                composable<AccountSecurity> {
                    AccountSecurityScreen(
                        onSignedOut = {
                            navController.navigate(SessionGate) {
                                popUpTo<AccountSecurity> { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                // Auth graph
                navigation<AuthGraph>(startDestination = Login) {
                    composable<Login> {
                        LoginScreen(
                            onNavigateToRegister = { navController.navigate(CreateAccount) },
                            onLoginSuccess = {
                                navController.navigate(SessionGate) {
                                    popUpTo<Welcome> { inclusive = true }
                                }
                            },
                            onForgotPassword = { navController.navigate(RecoverPassword) },
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
                            onNavigateToPrescriptionDetail = { navController.navigate(PrescriptionDetail(it)) },
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
                            onSuccess = {
                                navController.navigate(FrameReservationList) {
                                    popUpTo<CreateFrameReservation> { inclusive = true }
                                }
                            },
                            onBookAppointment = { navController.navigate(BookAppointmentForReservation) },
                        )
                    }
                    composable<BookAppointmentForReservation> {
                        BookAppointmentScreen(
                            onBack = { navController.popBackStack() },
                            onBooked = { navController.popBackStack() },
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
                    composable<PrescriptionList> {
                        PrescriptionListScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDetail = { navController.navigate(PrescriptionDetail(it)) },
                        )
                    }
                    composable<PrescriptionDetail> { back ->
                        val route = back.toRoute<PrescriptionDetail>()
                        PrescriptionDetailScreen(
                            prescriptionId = route.prescriptionId,
                            onBack = { navController.popBackStack() },
                            onNavigateToPrevious = { previousId ->
                                navController.navigate(PrescriptionDetail(previousId))
                            },
                        )
                    }
                    composable<EyewearList> {
                        EyewearListScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDetail = { navController.navigate(EyewearDetail(it)) },
                        )
                    }
                    composable<EyewearDetail> { back ->
                        val route = back.toRoute<EyewearDetail>()
                        EyewearDetailScreen(
                            key = route.key,
                            onBack = { navController.popBackStack() },
                        )
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
                            onNavigateToIntake = { appointmentId -> navController.navigate(PatientIntake(appointmentId)) },
                            onNavigateToReservations = { navController.navigate(FrameReservationList) },
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
                            onNavigateToEyewear = { navController.navigate(EyewearList) },
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
