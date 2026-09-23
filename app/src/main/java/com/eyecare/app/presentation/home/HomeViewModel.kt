package com.eyecare.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryQuery
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.ClinicHoursDay
import com.eyecare.app.domain.model.CurrentAppointmentJourney
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.AccessoryRepository
import com.eyecare.app.domain.repository.ClinicRepository
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.domain.repository.PrescriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val nextAppointment: AppointmentV1?,
        val currentPrescription: Prescription?,
        val featuredFrames: List<Frame>,
        val featuredAccessories: List<Accessory> = emptyList(),
        val clinicHours: List<ClinicHoursDay> = emptyList(),
        val currentAppointmentJourney: CurrentAppointmentJourney? = null,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appointmentRequestRepository: AppointmentRequestRepository,
    private val frameRepository: FrameRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val clinicRepository: ClinicRepository,
    private val accessoryRepository: AccessoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh(hasActivePatientLink: Boolean = true) {
        load(hasActivePatientLink)
    }

    fun load(hasActivePatientLink: Boolean = true) {
        _uiState.value = HomeUiState.Loading
        if (!hasActivePatientLink) {
            viewModelScope.launch {
                val framesDeferred = async {
                    runCatching { frameRepository.getFrames(page = 1).getOrDefault(emptyList()) }
                }
                val clinicHoursDeferred = async {
                    runCatching { clinicRepository.getClinicHours().getOrDefault(emptyList()) }
                }
                val accessoriesDeferred = async { loadFeaturedAccessories() }
                val frames = framesDeferred.await().getOrDefault(emptyList())
                val clinicHours = clinicHoursDeferred.await().getOrDefault(emptyList())
                val accessories = accessoriesDeferred.await()
                _uiState.value = HomeUiState.Success(
                    nextAppointment = null,
                    currentAppointmentJourney = null,
                    currentPrescription = null,
                    featuredFrames = frames.take(HOME_SHELF_LIMIT),
                    featuredAccessories = accessories,
                    clinicHours = clinicHours,
                )
            }
            return
        }

        viewModelScope.launch {
            val journeyDeferred = async {
                runCatching { appointmentRequestRepository.getCurrentAppointmentJourney().getOrNull() }
            }
            val framesDeferred = async {
                runCatching { frameRepository.getFrames(page = 1).getOrDefault(emptyList()) }
            }
            val prescriptionsDeferred = async {
                runCatching { prescriptionRepository.getPrescriptions(page = 1).getOrNull()?.data ?: emptyList() }
            }
            val clinicHoursDeferred = async {
                runCatching { clinicRepository.getClinicHours().getOrDefault(emptyList()) }
            }
            val accessoriesDeferred = async { loadFeaturedAccessories() }

            val journeyResult = journeyDeferred.await()
            val frames = framesDeferred.await().getOrDefault(emptyList())
            val prescriptions = prescriptionsDeferred.await().getOrDefault(emptyList())
            val clinicHours = clinicHoursDeferred.await().getOrDefault(emptyList())
            val accessories = accessoriesDeferred.await()

            val journey = journeyResult.getOrNull()
            val nextAppointment = (journey as? CurrentAppointmentJourney.Appointment)?.appointment

            val currentPrescription = prescriptions
                .filter { it.isCurrent }
                .maxByOrNull { it.date }

            _uiState.value = HomeUiState.Success(
                nextAppointment = nextAppointment,
                currentAppointmentJourney = journey,
                currentPrescription = currentPrescription,
                featuredFrames = frames.take(HOME_SHELF_LIMIT),
                featuredAccessories = accessories,
                clinicHours = clinicHours,
            )
        }
    }

    private suspend fun loadFeaturedAccessories(): List<Accessory> = runCatching {
        accessoryRepository.getAccessories(
            AccessoryQuery(
                sort = "newest",
                page = 1,
                perPage = HOME_SHELF_LIMIT,
            ),
        ).getOrNull()?.data.orEmpty()
            .filter { accessory ->
                accessory.variants.any { variant -> variant.availability.isOrderable }
            }
            .map { accessory ->
                accessory.copy(
                    variants = accessory.variants.sortedByDescending { variant ->
                        variant.availability.isOrderable
                    },
                )
            }
            .take(HOME_SHELF_LIMIT)
    }.getOrDefault(emptyList())

    private companion object {
        const val HOME_SHELF_LIMIT = 4
    }
}
