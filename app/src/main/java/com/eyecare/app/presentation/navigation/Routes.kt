package com.eyecare.app.presentation.navigation

import kotlinx.serialization.Serializable

// Auth graph
@Serializable object Login
@Serializable object Register

// V13 Auth graph
@Serializable object SessionGate
@Serializable object Welcome
@Serializable object CreateAccount
@Serializable object RecoverPassword

// V13 Account access graph (unlinked/pending)
@Serializable object LimitedAccount
@Serializable object AccountSecurity

// Main graph — tab roots
@Serializable object Home
@Serializable object Frames
@Serializable object Appointments
@Serializable object Profile

// Appointment sub-destinations
@Serializable data class AppointmentDetail(val appointmentId: Int)
@Serializable data object RequestAppointment
@Serializable data class AppointmentRequestDetail(val requestId: Int)

// Frame sub-destinations
@Serializable data class ArTryOn(val frameId: Int, val variantId: Int)
@Serializable data class FrameDetail(val frameId: Int, val variantId: Int? = null)
@Serializable object SavedFrames

// Prescriptions
@Serializable object PrescriptionList
@Serializable data class PrescriptionDetail(val prescriptionId: Int)

// My Orders (V14)
@Serializable object MyOrders
@Serializable data class OpticalOrderDetail(val orderId: Int)

// Profile sub-destinations
@Serializable object EditProfile
@Serializable object PatientProfile

// Chat (FAB destination, not a tab)
@Serializable object Chat

// Notifications (sub-destination, not a tab)
@Serializable object Notifications

// Graph tags
@Serializable object AuthGraph
@Serializable object AccountAccessGraph
@Serializable object MainGraph
