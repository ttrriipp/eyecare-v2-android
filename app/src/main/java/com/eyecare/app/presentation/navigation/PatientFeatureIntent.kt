package com.eyecare.app.presentation.navigation

/**
 * A protected destination that a limited account asked to open before linking.
 *
 * This stays in the navigation layer so the account-linking flow does not need
 * to know about feature screens or hold clinical data itself.
 */
sealed interface PatientFeatureIntent {
    data object FramesTab : PatientFeatureIntent
    data object AppointmentsTab : PatientFeatureIntent
    data object RequestAppointment : PatientFeatureIntent
    data object FrameReservationList : PatientFeatureIntent
    data object PrescriptionList : PatientFeatureIntent
    data object EyewearList : PatientFeatureIntent
    data object Chat : PatientFeatureIntent
    data class AppointmentDetail(val appointmentId: Int) : PatientFeatureIntent
    data class FrameDetail(val frameId: Int) : PatientFeatureIntent
    data class CreateFrameReservation(val frameId: Int, val variantId: Int) : PatientFeatureIntent
    data class ArTryOn(val frameId: Int, val variantId: Int) : PatientFeatureIntent
    data class PrescriptionDetail(val prescriptionId: Int) : PatientFeatureIntent
    data class EyewearDetail(val key: String) : PatientFeatureIntent
}

fun PatientFeatureIntent.toRoute(): Any = when (this) {
    PatientFeatureIntent.FramesTab -> Frames
    PatientFeatureIntent.AppointmentsTab -> Appointments
    PatientFeatureIntent.RequestAppointment -> RequestAppointment()
    PatientFeatureIntent.FrameReservationList -> FrameReservationList
    PatientFeatureIntent.PrescriptionList -> PrescriptionList
    PatientFeatureIntent.EyewearList -> EyewearList
    PatientFeatureIntent.Chat -> Chat
    is PatientFeatureIntent.AppointmentDetail -> AppointmentDetail(appointmentId)
    is PatientFeatureIntent.FrameDetail -> FrameDetail(frameId)
    is PatientFeatureIntent.CreateFrameReservation -> CreateFrameReservation(frameId, variantId)
    is PatientFeatureIntent.ArTryOn -> ArTryOn(frameId, variantId)
    is PatientFeatureIntent.PrescriptionDetail -> PrescriptionDetail(prescriptionId)
    is PatientFeatureIntent.EyewearDetail -> EyewearDetail(key)
}

fun patientFeatureIntentFrom(route: Any): PatientFeatureIntent? = when (route) {
    Frames -> PatientFeatureIntent.FramesTab
    Appointments -> PatientFeatureIntent.AppointmentsTab
    is RequestAppointment -> PatientFeatureIntent.RequestAppointment
    FrameReservationList -> PatientFeatureIntent.FrameReservationList
    PrescriptionList -> PatientFeatureIntent.PrescriptionList
    EyewearList -> PatientFeatureIntent.EyewearList
    Chat -> PatientFeatureIntent.Chat
    is AppointmentDetail -> PatientFeatureIntent.AppointmentDetail(route.appointmentId)
    is FrameDetail -> PatientFeatureIntent.FrameDetail(route.frameId)
    is CreateFrameReservation -> PatientFeatureIntent.CreateFrameReservation(route.frameId, route.variantId)
    is ArTryOn -> PatientFeatureIntent.ArTryOn(route.frameId, route.variantId)
    is PrescriptionDetail -> PatientFeatureIntent.PrescriptionDetail(route.prescriptionId)
    is EyewearDetail -> PatientFeatureIntent.EyewearDetail(route.key)
    else -> null
}

val PatientFeatureIntent.label: String
    get() = when (this) {
        PatientFeatureIntent.FramesTab,
        is PatientFeatureIntent.FrameDetail,
        is PatientFeatureIntent.CreateFrameReservation,
        is PatientFeatureIntent.ArTryOn -> "frames"
        PatientFeatureIntent.AppointmentsTab,
        PatientFeatureIntent.RequestAppointment,
        is PatientFeatureIntent.AppointmentDetail -> "appointments"
        PatientFeatureIntent.FrameReservationList -> "reservations"
        PatientFeatureIntent.PrescriptionList,
        is PatientFeatureIntent.PrescriptionDetail -> "prescriptions"
        PatientFeatureIntent.EyewearList,
        is PatientFeatureIntent.EyewearDetail -> "eyewear"
        PatientFeatureIntent.Chat -> "messages"
    }
