package com.eyecare.app.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.eyecare.app.presentation.appointments.AppointmentDetailScreen
import com.eyecare.app.presentation.appointments.AppointmentListScreen
import com.eyecare.app.presentation.appointments.booking.BookAppointmentScreen
import com.eyecare.app.presentation.auth.LoginScreen
import com.eyecare.app.presentation.auth.RegisterScreen
import com.eyecare.app.presentation.ar.ArTryOnScreen
import com.eyecare.app.presentation.feedback.FeedbackHistoryScreen
import com.eyecare.app.presentation.feedback.FeedbackScreen
import com.eyecare.app.presentation.billing.BillingDetailScreen
import com.eyecare.app.presentation.orders.OrderDetailScreen
import com.eyecare.app.presentation.prescriptions.PrescriptionDetailScreen
import com.eyecare.app.presentation.prescriptions.PrescriptionListScreen
import com.eyecare.app.presentation.orders.OrderDetailViewModel
import com.eyecare.app.presentation.orders.OrderListScreen
import com.eyecare.app.presentation.orders.OrderRequestScreen
import com.eyecare.app.presentation.catalog.ProductDetailScreen
import com.eyecare.app.presentation.catalog.ProductDetailViewModel
import com.eyecare.app.presentation.catalog.ProductListScreen
import com.eyecare.app.presentation.home.HomeScreen
import com.eyecare.app.presentation.messaging.ChatScreen
import com.eyecare.app.presentation.profile.ProfileScreen

@Composable
fun EyecareNavGraph(
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = if (tokenManager.getToken() != null) MainGraph else AuthGraph
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest: NavDestination? = backStackEntry?.destination

    // Hide bottom nav on auth screens and chat
    val showBottomNav = currentDest?.route?.let { route ->
        !route.contains("Login") && !route.contains("Register") &&
            !route.contains("Chat") && !route.contains("AppointmentDetail") &&
            !route.contains("BookAppointment") && !route.contains("ProductDetail") &&
            !route.contains("ArTryOn") && !route.contains("OrderRequest") &&
            !route.contains("OrderDetail")
    } ?: false

    val currentRoute = if (showBottomNav) when {
        currentDest?.route?.contains("Home") == true -> Home
        currentDest?.route?.contains("Catalog") == true -> Catalog
        currentDest?.route?.contains("Appointments") == true -> Appointments
        currentDest?.route?.contains("Profile") == true -> Profile
        else -> Home
    } else null

    Box(modifier = Modifier.fillMaxSize()) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding),
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
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToBooking = { navController.navigate(BookAppointment) },
                            onNavigateToOrderDetail = { navController.navigate(OrderDetail(it)) },
                            onNavigateToCatalog = {
                                navController.navigate(Catalog) { launchSingleTop = true }
                            },
                            onNavigateToProductDetail = { navController.navigate(ProductDetail(it)) },
                        )
                    }
                    composable<Catalog> {
                        ProductListScreen(
                            onNavigateToDetail = { id -> navController.navigate(ProductDetail(id)) },
                        )
                    }
                    composable<ProductDetail> { backStackEntry ->
                        val route = backStackEntry.toRoute<ProductDetail>()
                        ProductDetailScreen(
                            productId = route.productId,
                            onBack = { navController.popBackStack() },
                            onNavigateToAr = { pId, vId -> navController.navigate(ArTryOn(pId, vId)) },
                            onNavigateToOrder = { pId, vId -> navController.navigate(OrderRequest(pId, vId)) },
                        )
                    }
                    composable<ArTryOn> { backStackEntry ->
                        val route = backStackEntry.toRoute<ArTryOn>()
                        ArTryOnScreen(
                            productId = route.productId,
                            initialVariantId = route.variantId,
                            onBack = { navController.popBackStack() },
                            onNavigateToOrder = { pId, vId -> navController.navigate(OrderRequest(pId, vId)) },
                        )
                    }
                    composable<OrderRequest> { backStackEntry ->
                        val route = backStackEntry.toRoute<OrderRequest>()
                        OrderRequestScreen(
                            productId = route.productId,
                            variantId = route.variantId,
                            onBack = { navController.popBackStack() },
                            onOrderSubmitted = { navController.navigate(OrderList) { popUpTo(OrderRequest(route.productId, route.variantId)) { inclusive = true } } },
                        )
                    }
                    composable<OrderList> {
                        OrderListScreen(
                            onNavigateToDetail = { navController.navigate(OrderDetail(it)) },
                        )
                    }
                    composable<OrderDetail> { back ->
                        val route = back.toRoute<OrderDetail>()
                        OrderDetailScreen(
                            orderId = route.orderId,
                            onBack = { navController.popBackStack() },
                            onViewBilling = { orderId -> navController.navigate(BillingDetail(orderId)) },
                            onLeaveFeedback = { id -> navController.navigate(FeedbackSubmit(orderId = id)) },
                        )
                    }
                    composable<FeedbackSubmit> { back ->
                        val route = back.toRoute<FeedbackSubmit>()
                        FeedbackScreen(
                            appointmentId = route.appointmentId.takeIf { it != -1 },
                            orderId = route.orderId.takeIf { it != -1 },
                            onBack = { navController.popBackStack() },
                            onSubmitted = { navController.popBackStack() },
                        )
                    }
                    composable<FeedbackHistory> { FeedbackHistoryScreen() }
                    composable<BillingDetail> { back ->
                        val route = back.toRoute<BillingDetail>()
                        BillingDetailScreen(
                            billingId = route.billingId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<PrescriptionList> {
                        PrescriptionListScreen(onNavigateToDetail = { navController.navigate(PrescriptionDetail(it)) })
                    }
                    composable<PrescriptionDetail> { back ->
                        val route = back.toRoute<PrescriptionDetail>()
                        PrescriptionDetailScreen(prescriptionId = route.prescriptionId, onBack = { navController.popBackStack() })
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
                            onNavigateToOrders = { navController.navigate(OrderList) },
                            onNavigateToPrescriptions = { navController.navigate(PrescriptionList) },
                            onNavigateToFeedbackHistory = { navController.navigate(FeedbackHistory) },
                        )
                    }
                    composable<Chat> {
                        ChatScreen(onBack = { navController.popBackStack() })
                    }
                }
            }

        // Floating navbar — overlaid on content, no background behind it
        if (showBottomNav && currentRoute != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                SplitBottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onChatClick = { navController.navigate(Chat) },
                )
            }
        }
    }
}
