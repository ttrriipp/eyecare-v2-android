package com.eyecare.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.domain.repository.PrescriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val nextAppointment: AppointmentV1?,
        val expiringPrescription: Prescription?,
        val featuredFrames: List<Frame>,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appointmentRepository: AppointmentV1Repository,
    private val frameRepository: FrameRepository,
    private val prescriptionRepository: PrescriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            val appointmentsDeferred = async {
                runCatching { appointmentRepository.getAppointments(page = 1).getOrNull()?.data ?: emptyList() }
            }
            val framesDeferred = async {
                runCatching { frameRepository.getFrames(page = 1).getOrDefault(emptyList()) }
            }
            val prescriptionsDeferred = async {
                runCatching { prescriptionRepository.getPrescriptions(page = 1).getOrNull()?.data ?: emptyList() }
            }

            val appointments = appointmentsDeferred.await().getOrDefault(emptyList())
            val frames = framesDeferred.await().getOrDefault(emptyList())
            val prescriptions = prescriptionsDeferred.await().getOrDefault(emptyList())

            val today = LocalDate.now()

            val nextAppointment = appointments
                .filter {
                    it.status in setOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CHECKED_IN) &&
                        runCatching { !LocalDate.parse(it.scheduledAt.take(10)).isBefore(today) }.getOrElse { false }
                }
                .minByOrNull { it.scheduledAt }

            val expiringPrescription = prescriptions
                .filter { p ->
                    p.expiresAt != null && runCatching {
                        val exp = LocalDate.parse(p.expiresAt.take(10))
                        !exp.isBefore(today) && !exp.isAfter(today.plusDays(30))
                    }.getOrElse { false }
                }
                .minByOrNull { it.expiresAt ?: "" }
                ?: prescriptions.firstOrNull { p ->
                    p.expiresAt != null && runCatching {
                        LocalDate.parse(p.expiresAt.take(10)).isBefore(today)
                    }.getOrElse { false }
                }

            _uiState.value = HomeUiState.Success(
                nextAppointment = nextAppointment,
                expiringPrescription = expiringPrescription,
                featuredFrames = frames.take(HOME_SHELF_LIMIT),
            )
        }
    }

    private companion object {
        const val HOME_SHELF_LIMIT = 4
    }
}
