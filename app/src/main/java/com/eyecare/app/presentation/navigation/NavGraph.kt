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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.MobileDestination
import com.eyecare.app.domain.model.SessionState
import com.eyecare.app.domain.model.requiresAppointmentRequestIdentity
import com.eyecare.app.domain.model.toAppointmentRequestIdentityOrNull
import com.eyecare.app.domain.model.canAccessPatientFeatures
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.presentation.appointments.AppointmentDetailScreen
import com.eyecare.app.presentation.appointments.AppointmentListScreen
import com.eyecare.app.presentation.appointments.requests.AppointmentRequestDetailScreen
import com.eyecare.app.presentation.appointments.requests.RequestAppointmentScreen
import com.eyecare.app.presentation.auth.LoginScreen
import com.eyecare.app.presentation.auth.PasswordRecoveryScreen
import com.eyecare.app.presentation.auth.RegisterScreen
import com.eyecare.app.presentation.auth.SessionGateScreen
import com.eyecare.app.presentation.auth.SessionViewModel
import com.eyecare.app.presentation.auth.WelcomeScreen
import com.eyecare.app.presentation.account.LimitedAccountScreen
import com.eyecare.app.presentation.account.AccountSecurityScreen
import com.eyecare.app.presentation.ar.ArTryOnScreen
import com.eyecare.app.presentation.prescriptions.PrescriptionDetailScreen
import com.eyecare.app.presentation.prescriptions.PrescriptionListScreen
import com.eyecare.app.presentation.eyewear.MyOrdersScreen
import com.eyecare.app.presentation.eyewear.OpticalOrderDetailScreen
import com.eyecare.app.presentation.frames.FrameDetailScreen
import com.eyecare.app.presentation.frames.FrameListScreen
import com.eyecare.app.presentation.frames.SavedFramesScreen
import com.eyecare.app.presentation.frames.SavedFramesViewModel
import com.eyecare.app.presentation.home.HomeScreen
import com.eyecare.app.presentation.profile.PatientProfileScreen
import com.eyecare.app.presentation.messaging.ChatScreen
import com.eyecare.app.presentation.notifications.NotificationListScreen
import com.eyecare.app.presentation.notifications.NotificationListViewModel
import com.eyecare.app.presentation.notifications.NotificationListUiState
import com.eyecare.app.presentation.notifications.NotificationEffect
import com.eyecare.app.presentation.profile.ProfileScreen

internal fun shouldShowBottomNav(route: String): Boolean =
    !route.contains("Login") && !route.contains("Register") &&
        !route.contains("SessionGate") && !route.contains("Welcome") &&
        !route.contains("CreateAccount") && !route.contains("RecoverPassword") &&
        !route.contains("LimitedAccount") && !route.contains("AccountSecurity") &&
        !route.contains("Chat") && !route.contains("Notifications") && !route.contains("AppointmentDetail") &&
        !route.contains("AppointmentRequest") &&
        !route.contains("BookAppointment") &&
        !route.contains("ArTryOn") && !route.contains("FrameDetail") &&
        !route.contains("Prescription") &&
        !route.contains("PatientProfile") &&
        !route.contains("PatientIntake") && !route.contains("Quotation") &&
        !route.contains("OpticalOrderDetail") &&
        !route.contains("JobOrder") && !route.contains("MyOrders")

@Composable
fun EyecareNavGraph(
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = SessionGate
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val mainUnreadViewModel: MainUnreadViewModel = hiltViewModel()
    val sessionState by sessionViewModel.state.collectAsState()
    val mainUnreadState by mainUnreadViewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest: NavDestination? = backStackEntry?.destination

    LaunchedEffect(currentDest?.route, sessionState) {
        val route = currentDest?.route
        if (!shouldRedirectToLimitedAccount(route, sessionState)) return@LaunchedEffect
        if (navController.currentDestination?.route != route) return@LaunchedEffect

        // This is a backstop for restored/direct navigation paths. Normal feature clicks already
        // set pendingPatientFeature before opening the hub, while this path removes the protected
        // destination as soon as a limited session is observed.
        navController.popBackStack()
        navController.navigate(LimitedAccount) {
            launchSingleTop = true
        }
    }

    // Refresh unread counts when entering MainGraph
    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.Linked || sessionState is SessionState.Limited) {
            mainUnreadViewModel.refresh()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionState) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                (sessionState is SessionState.Linked || sessionState is SessionState.Limited)
            ) {
                mainUnreadViewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var pendingPatientFeature by remember { mutableStateOf<PatientFeatureIntent?>(null) }

    // Hide bottom nav on auth/limited screens and detail/sub-destination screens.
    val showBottomNav = currentDest?.route?.let(::shouldShowBottomNav) ?: false

    val currentRoute = if (showBottomNav) when {
        currentDest.route?.contains("Home") == true -> Home
        currentDest.route?.contains("Frames") == true -> Frames
        currentDest.route?.contains("Appointments") == true -> Appointments
        currentDest.route?.contains("Profile") == true -> Profile
        else -> Home
    } else null

    fun canNavigateTo(route: Any): Boolean = when (val state = sessionState) {
        is SessionState.Linked -> true
        is SessionState.Limited -> canAccessRoute(route, state.account.linkStatus)
        else -> false
    }

    fun navigatePatientFeature(
        route: Any,
        navOptions: NavOptionsBuilder.() -> Unit = {},
    ) {
        if (canNavigateTo(route)) {
            navController.navigate(route, navOptions)
        } else {
            pendingPatientFeature = patientFeatureIntentFrom(route)
            navController.navigate(LimitedAccount) {
                launchSingleTop = true
            }
        }
    }

    fun navigateMainTab(route: Any) {
        if (route == Home || route == Profile || canNavigateTo(route)) {
            navController.navigate(route) {
                popUpTo<MainGraph> {
                    saveState = true
                    inclusive = false
                }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            pendingPatientFeature = patientFeatureIntentFrom(route)
            navController.navigate(LimitedAccount)
        }
    }

    fun openAccountLink() {
        pendingPatientFeature = null
        navController.navigate(LimitedAccount)
    }

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
                        viewModel = sessionViewModel,
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
                            sessionViewModel.resolveSession()
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
                            sessionViewModel.resolveSession()
                            navController.navigate(SessionGate) {
                                popUpTo<Welcome> { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                // Account access graph (unlinked/pending)
                composable<LimitedAccount> {
                    val account = when (val state = sessionState) {
                        is SessionState.Linked -> state.account
                        is SessionState.Limited -> state.account
                        else -> null
                    }
                    if (account != null) {
                        LimitedAccountScreen(
                            account = account,
                            requestedFeatureLabel = pendingPatientFeature?.label,
                            onBack = {
                                pendingPatientFeature = null
                                navController.popBackStack()
                            },
                            onLinkComplete = { linkedAccount ->
                                val destination = pendingPatientFeature
                                pendingPatientFeature = null
                                sessionViewModel.setLinkedAccount(linkedAccount)
                                navController.popBackStack()
                                destination?.let { navController.navigate(it.toRoute()) }
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(SessionGate) {
                                popUpTo<LimitedAccount> { inclusive = true }
                            }
                        }
                    }
                }

                // Account & Security (shared from limited and linked)
                composable<AccountSecurity> {
                    AccountSecurityScreen(
                        onSignedOut = {
                            sessionViewModel.signOut()
                            navController.navigate(Welcome) {
                                popUpTo<MainGraph> { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onAccountUpdated = { account -> sessionViewModel.adoptAccount(account) },
                    )
                }

                // Auth graph
                navigation<AuthGraph>(startDestination = Login) {
                    composable<Login> {
                        LoginScreen(
                            onNavigateToRegister = { navController.navigate(CreateAccount) },
                            onLoginSuccess = {
                                sessionViewModel.resolveSession()
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
                        val homeLinkStatus = when (val state = sessionState) {
                            is SessionState.Linked -> state.account.linkStatus
                            is SessionState.Limited -> state.account.linkStatus
                            else -> PatientLinkStatus.UNKNOWN
                        }
                        HomeScreen(
                            onNavigateToAppointments = {
                                navigateMainTab(Appointments)
                            },
                            onNavigateToBooking = { navigatePatientFeature(RequestAppointment) },
                            onNavigateToFrames = {
                                navigateMainTab(Frames)
                            },
                            onNavigateToFrameDetail = { navigatePatientFeature(FrameDetail(it)) },
                            onNavigateToLinkAccount = ::openAccountLink,
                            onNavigateToNotifications = { navController.navigate(Notifications) },
                            hasActivePatientLink = canAccessPatientFeatures(sessionState),
                            patientLinkStatus = homeLinkStatus,
                            notificationUnreadCount = mainUnreadState.notificationUnreadCount,
                        )
                    }
                    composable<Frames> {
                        FrameListScreen(
                            onNavigateToDetail = { id -> navigatePatientFeature(FrameDetail(id)) },
                            onNavigateToTryOn = { frameId, variantId ->
                                navigatePatientFeature(ArTryOn(frameId, variantId))
                            },
                        )
                    }
                    composable<FrameDetail> { backStackEntry ->
                        val route = backStackEntry.toRoute<FrameDetail>()
                        FrameDetailScreen(
                            frameId = route.frameId,
                            variantId = route.variantId,
                            onBack = { navController.popBackStack() },
                            onNavigateToAr = { fId, vId -> navigatePatientFeature(ArTryOn(fId, vId)) },
                        )
                    }
                    composable<RequestAppointment> {
                        val requestAccount = when (val state = sessionState) {
                            is SessionState.Linked -> state.account
                            is SessionState.Limited -> state.account
                            else -> null
                        }
                        val requestIdentity = requestAccount?.toAppointmentRequestIdentityOrNull()
                        val identityDetailsRequired = requestAccount
                            ?.requiresAppointmentRequestIdentity() == true
                        RequestAppointmentScreen(
                            onBack = { navController.popBackStack() },
                            onRequestCreated = { requestId ->
                                navController.navigate(AppointmentRequestDetail(requestId)) {
                                    popUpTo<RequestAppointment> { inclusive = true }
                                }
                            },
                            onViewRequests = { navController.popBackStack() },
                            requestIdentity = requestIdentity,
                            identityDetailsRequired = identityDetailsRequired,
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
                            onNavigateToDetail = { navigatePatientFeature(PrescriptionDetail(it)) },
                        )
                    }
                    composable<PrescriptionDetail> { back ->
                        val route = back.toRoute<PrescriptionDetail>()
                        PrescriptionDetailScreen(
                            prescriptionId = route.prescriptionId,
                            onBack = { navController.popBackStack() },
                            onNavigateToPrevious = { previousId ->
                                navigatePatientFeature(PrescriptionDetail(previousId))
                            },
                        )
                    }
                    composable<MyOrders> {
                        MyOrdersScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToOrder = { id -> navigatePatientFeature(OpticalOrderDetail(id)) },
                        )
                    }
                    composable<OpticalOrderDetail> {
                        OpticalOrderDetailScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToMessages = { navigatePatientFeature(Chat) },
                        )
                    }
                    composable<Appointments> {
                        AppointmentListScreen(
                            onNavigateToDetail = { id -> navigatePatientFeature(AppointmentDetail(id)) },
                            onNavigateToRequest = { navigatePatientFeature(RequestAppointment) },
                            onNavigateToRequestDetail = { id ->
                                navigatePatientFeature(AppointmentRequestDetail(id))
                            },
                            hasActivePatientLink = canAccessPatientFeatures(sessionState),
                        )
                    }
                    composable<AppointmentDetail> {
                        AppointmentDetailScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<AppointmentRequestDetail> { backStackEntry ->
                        val route = backStackEntry.toRoute<AppointmentRequestDetail>()
                        AppointmentRequestDetailScreen(
                            requestId = route.requestId,
                            isLinked = sessionState is SessionState.Linked,
                            onBack = { navController.popBackStack() },
                            onViewConfirmedAppointment = { id ->
                                navigatePatientFeature(AppointmentDetail(id))
                            },
                            onNavigateToMessages = { navigatePatientFeature(Chat) },
                        )
                    }
                    composable<Profile> {
                        val account = when (val state = sessionState) {
                            is SessionState.Linked -> state.account
                            is SessionState.Limited -> state.account
                            else -> null
                        }
                        ProfileScreen(
                            account = account,
                            onLinkedAccountResolved = sessionViewModel::setLinkedAccount,
                            onLogout = {
                                tokenManager.clearToken()
                                sessionViewModel.signOut()
                                onLogout()
                                navController.navigate(Welcome) {
                                    popUpTo(MainGraph) { inclusive = true }
                                }
                            },
                            onNavigateToPrescriptions = { navigatePatientFeature(PrescriptionList) },
                            onNavigateToSavedFrames = { navigatePatientFeature(SavedFrames) },
                            onNavigateToEyewear = { navigatePatientFeature(MyOrders) },
                            onNavigateToMessages = { navigatePatientFeature(Chat) },
                            onNavigateToPatientProfile = { navigatePatientFeature(PatientProfile) },
                            onNavigateToAccountSecurity = { navController.navigate(AccountSecurity) },
                            onNavigateToInviteCode = ::openAccountLink,
                            unreadMessageCount = mainUnreadState.messageUnreadCount,
                        )
                    }
                    composable<SavedFrames> {
                        val savedFramesViewModel: SavedFramesViewModel = hiltViewModel()
                        val savedFramesState by savedFramesViewModel.uiState.collectAsStateWithLifecycle()
                        SavedFramesScreen(
                            uiState = savedFramesState,
                            onBack = { navController.popBackStack() },
                            onRefresh = savedFramesViewModel::refresh,
                            onLoadMore = savedFramesViewModel::loadMore,
                            onRemoveFrame = savedFramesViewModel::removeSavedFrame,
                            onOpenFrame = { frameId, variantId ->
                                navigatePatientFeature(FrameDetail(frameId, variantId))
                            },
                            onClearError = savedFramesViewModel::clearInlineError,
                        )
                    }
                    composable<PatientProfile> {
                        val patient = (sessionState as? SessionState.Linked)?.account?.linkedPatient
                        PatientProfileScreen(
                            patient = patient,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<Chat> {
                        ChatScreen(
                            onBack = { navController.popBackStack() },
                            onMessagesMarkedRead = { mainUnreadViewModel.onMessagesMarkedRead() },
                        )
                    }
                    composable<Notifications> {
                        val notificationViewModel: NotificationListViewModel = hiltViewModel()
                        val notificationUiState by notificationViewModel.uiState.collectAsState()

                        LaunchedEffect(Unit) {
                            notificationViewModel.effects.collect { effect ->
                                when (effect) {
                                    is NotificationEffect.Navigate -> {
                                        when (effect.destination) {
                                            MobileDestination.CONVERSATION -> navController.navigate(Chat)
                                            MobileDestination.UNKNOWN -> { /* no-op */ }
                                        }
                                    }
                                    NotificationEffect.NotificationRead -> mainUnreadViewModel.onNotificationRead()
                                    NotificationEffect.AllNotificationsRead -> mainUnreadViewModel.onAllNotificationsRead()
                                    is NotificationEffect.UnreadCountReconciled -> {
                                        mainUnreadViewModel.reconcileNotificationUnreadCount(effect.count)
                                    }
                                }
                            }
                        }

                        NotificationListScreen(
                            uiState = notificationUiState,
                            unreadCount = mainUnreadState.notificationUnreadCount,
                            onBack = { navController.popBackStack() },
                            onNotificationTap = { notificationViewModel.onNotificationTap(it) },
                            onMarkAllRead = { notificationViewModel.markAllRead() },
                            onLoadMore = { notificationViewModel.loadMore() },
                            onRefresh = { notificationViewModel.refresh() },
                            onRetry = { notificationViewModel.loadInitial() },
                            onDismissMessage = { notificationViewModel.clearInfoMessage() },
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
                        navigateMainTab(route)
                    },
                )
            }
        }
    }
}
