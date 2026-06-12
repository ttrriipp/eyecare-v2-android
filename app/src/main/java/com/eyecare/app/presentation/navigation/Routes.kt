package com.eyecare.app.presentation.navigation

import kotlinx.serialization.Serializable

// Auth graph
@Serializable object Login
@Serializable object Register

// Main graph — tab roots
@Serializable object Home
@Serializable object Catalog
@Serializable object Appointments
@Serializable object Profile

// Appointment sub-destinations
@Serializable data class AppointmentDetail(val appointmentId: Int)
@Serializable object BookAppointment

// Catalog sub-destinations
@Serializable data class ProductDetail(val productId: Int)
@Serializable data class ArTryOn(val productId: Int, val variantId: Int)

// Order destinations
@Serializable data class OrderRequest(val productId: Int, val variantId: Int)
@Serializable object OrderList
@Serializable data class OrderDetail(val orderId: Int)

// Billing
@Serializable data class BillingDetail(val billingId: Int)

// Prescriptions
@Serializable object PrescriptionList
@Serializable data class PrescriptionDetail(val prescriptionId: Int)

// Chat (FAB destination, not a tab)
@Serializable object Chat

// Graph tags
@Serializable object AuthGraph
@Serializable object MainGraph
