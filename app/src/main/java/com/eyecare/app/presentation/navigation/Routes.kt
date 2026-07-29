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

// Eyewear aggregate
@Serializable object EyewearList
@Serializable data class EyewearDetail(val key: String)

// Profile sub-destinations
@Serializable object EditProfile

// Chat (FAB destination, not a tab)
@Serializable object Chat

// Graph tags
@Serializable object AuthGraph
@Serializable object MainGraph
