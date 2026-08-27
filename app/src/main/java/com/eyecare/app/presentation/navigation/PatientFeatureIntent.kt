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
    data object PrescriptionList : PatientFeatureIntent
    data object MyOrders : PatientFeatureIntent
    data class AppointmentDetail(val appointmentId: Int) : PatientFeatureIntent
    data class FrameDetail(val frameId: Int) : PatientFeatureIntent
    data class ArTryOn(val frameId: Int, val variantId: Int) : PatientFeatureIntent
    data class PrescriptionDetail(val prescriptionId: Int) : PatientFeatureIntent
    data class OpticalOrderDetail(val orderId: Int) : PatientFeatureIntent
}

fun PatientFeatureIntent.toRoute(): Any = when (this) {
    PatientFeatureIntent.FramesTab -> Frames
    PatientFeatureIntent.AppointmentsTab -> Appointments
    PatientFeatureIntent.RequestAppointment -> RequestAppointment
    PatientFeatureIntent.PrescriptionList -> PrescriptionList
    PatientFeatureIntent.MyOrders -> MyOrders
    is PatientFeatureIntent.AppointmentDetail -> AppointmentDetail(appointmentId)
    is PatientFeatureIntent.FrameDetail -> FrameDetail(frameId)
    is PatientFeatureIntent.ArTryOn -> ArTryOn(frameId, variantId)
    is PatientFeatureIntent.PrescriptionDetail -> PrescriptionDetail(prescriptionId)
    is PatientFeatureIntent.OpticalOrderDetail -> OpticalOrderDetail(orderId)
}

fun patientFeatureIntentFrom(route: Any): PatientFeatureIntent? = when (route) {
    Frames -> PatientFeatureIntent.FramesTab
    Appointments -> PatientFeatureIntent.AppointmentsTab
    is RequestAppointment -> PatientFeatureIntent.RequestAppointment
    PrescriptionList -> PatientFeatureIntent.PrescriptionList
    MyOrders -> PatientFeatureIntent.MyOrders
    is AppointmentDetail -> PatientFeatureIntent.AppointmentDetail(route.appointmentId)
    is FrameDetail -> PatientFeatureIntent.FrameDetail(route.frameId)
    is ArTryOn -> PatientFeatureIntent.ArTryOn(route.frameId, route.variantId)
    is PrescriptionDetail -> PatientFeatureIntent.PrescriptionDetail(route.prescriptionId)
    is OpticalOrderDetail -> PatientFeatureIntent.OpticalOrderDetail(route.orderId)
    else -> null
}

val PatientFeatureIntent.label: String
    get() = when (this) {
        PatientFeatureIntent.FramesTab,
        is PatientFeatureIntent.FrameDetail,
        is PatientFeatureIntent.ArTryOn -> "frames"
        PatientFeatureIntent.AppointmentsTab,
        PatientFeatureIntent.RequestAppointment,
        is PatientFeatureIntent.AppointmentDetail -> "appointments"
        PatientFeatureIntent.PrescriptionList,
        is PatientFeatureIntent.PrescriptionDetail -> "prescriptions"
        PatientFeatureIntent.MyOrders,
        is PatientFeatureIntent.OpticalOrderDetail -> "eyewear"
    }
