package com.eyecare.app.presentation.navigation

import kotlinx.serialization.Serializable

// Auth graph
@Serializable object Login
@Serializable object Register

// Main graph — tab roots
@Serializable object Home
@Serializable object Frames
@Serializable object Appointments
@Serializable object Profile

// Appointment sub-destinations
@Serializable data class AppointmentDetail(val appointmentId: Int)
@Serializable object BookAppointment
@Serializable data class PatientIntake(val appointmentId: Int)

// Frame sub-destinations
@Serializable data class ArTryOn(val frameId: Int, val variantId: Int)
@Serializable data class FrameDetail(val frameId: Int)
@Serializable data class CreateFrameReservation(val frameId: Int, val variantId: Int)

// Reservation destinations
@Serializable object FrameReservationList
@Serializable object BookAppointmentForReservation

// Prescriptions
@Serializable object PrescriptionList
@Serializable data class PrescriptionDetail(val prescriptionId: Int)

// Quotations
@Serializable object QuotationList
@Serializable data class QuotationDetail(val quotationId: Int)

// Job Orders
@Serializable object JobOrderList
@Serializable data class JobOrderDetail(val jobOrderId: Int)

// Billing Records
@Serializable object BillingRecordList
@Serializable data class BillingRecordDetail(val billingRecordId: Int)

// Profile sub-destinations
@Serializable object EditProfile

// Chat (FAB destination, not a tab)
@Serializable object Chat

// Graph tags
@Serializable object AuthGraph
@Serializable object MainGraph
