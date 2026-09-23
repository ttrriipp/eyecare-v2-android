package com.eyecare.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryQuery
import com.eyecare.app.domain.model.AppointmentV1
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeSection {
    APPOINTMENT_JOURNEY,
    PRESCRIPTION,
    FRAMES,
    CLINIC_HOURS,
    ACCESSORIES,
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val nextAppointment: AppointmentV1?,
        val currentPrescription: Prescription?,
        val featuredFrames: List<Frame>,
        val featuredAccessories: List<Accessory> = emptyList(),
        val clinicHours: List<ClinicHoursDay> = emptyList(),
        val currentAppointmentJourney: CurrentAppointmentJourney? = null,
        val loadingSections: Set<HomeSection> = emptySet(),
        val isRefreshing: Boolean = false,
        val hasActivePatientLink: Boolean = true,
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
    private var loadJob: Job? = null
    private var loadGeneration = 0

    fun refresh(hasActivePatientLink: Boolean = true) {
        load(hasActivePatientLink)
    }

    fun load(hasActivePatientLink: Boolean = true) {
        loadJob?.cancel()
        val generation = ++loadGeneration
        val previous = (_uiState.value as? HomeUiState.Success)
            ?.takeIf { it.hasActivePatientLink == hasActivePatientLink }
        val sections = buildSet {
            if (hasActivePatientLink) {
                add(HomeSection.APPOINTMENT_JOURNEY)
                add(HomeSection.PRESCRIPTION)
            }
            add(HomeSection.FRAMES)
            add(HomeSection.CLINIC_HOURS)
            add(HomeSection.ACCESSORIES)
        }

        _uiState.value = HomeUiState.Success(
            nextAppointment = previous?.nextAppointment,
            currentPrescription = previous?.currentPrescription,
            featuredFrames = previous?.featuredFrames.orEmpty(),
            featuredAccessories = previous?.featuredAccessories.orEmpty(),
            clinicHours = previous?.clinicHours.orEmpty(),
            currentAppointmentJourney = previous?.currentAppointmentJourney,
            loadingSections = sections,
            isRefreshing = previous != null,
            hasActivePatientLink = hasActivePatientLink,
        )

        loadJob = viewModelScope.launch {
            coroutineScope {
                if (hasActivePatientLink) {
                    launch {
                        val journey = safely(previous?.currentAppointmentJourney) {
                            appointmentRequestRepository.getCurrentAppointmentJourney()
                                .getOrElse { previous?.currentAppointmentJourney }
                        }
                        updateSection(generation, HomeSection.APPOINTMENT_JOURNEY) {
                            copy(
                                currentAppointmentJourney = journey,
                                nextAppointment = (journey as? CurrentAppointmentJourney.Appointment)?.appointment,
                            )
                        }
                    }
                    launch {
                        val prescription = safely(previous?.currentPrescription) {
                            prescriptionRepository.getPrescriptions(page = 1).fold(
                                onSuccess = { result ->
                                    result.data.filter { it.isCurrent }.maxByOrNull { it.date }
                                },
                                onFailure = { previous?.currentPrescription },
                            )
                        }
                        updateSection(generation, HomeSection.PRESCRIPTION) {
                            copy(currentPrescription = prescription)
                        }
                    }
                }

                launch {
                    val framesFallback = previous?.featuredFrames.orEmpty()
                    val frames = safely(framesFallback) {
                        frameRepository.getFrames(page = 1, perPage = HOME_SHELF_LIMIT)
                            .getOrNull()
                            ?.take(HOME_SHELF_LIMIT)
                            ?: framesFallback
                    }
                    updateSection(generation, HomeSection.FRAMES) {
                        copy(featuredFrames = frames)
                    }
                }
                launch {
                    val hoursFallback = previous?.clinicHours.orEmpty()
                    val clinicHours = safely(hoursFallback) {
                        clinicRepository.getClinicHours().getOrNull() ?: hoursFallback
                    }
                    updateSection(generation, HomeSection.CLINIC_HOURS) {
                        copy(clinicHours = clinicHours)
                    }
                }
                launch {
                    val accessories = loadFeaturedAccessories(previous?.featuredAccessories.orEmpty())
                    updateSection(generation, HomeSection.ACCESSORIES) {
                        copy(featuredAccessories = accessories)
                    }
                }
            }
            _uiState.update { state ->
                if (generation == loadGeneration && state is HomeUiState.Success) {
                    state.copy(isRefreshing = false)
                } else {
                    state
                }
            }
        }
    }

    private inline fun updateSection(
        generation: Int,
        section: HomeSection,
        crossinline transform: HomeUiState.Success.() -> HomeUiState.Success,
    ) {
        _uiState.update { state ->
            if (generation == loadGeneration && state is HomeUiState.Success) {
                state.transform().copy(loadingSections = state.loadingSections - section)
            } else {
                state
            }
        }
    }

    private suspend fun <T> safely(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        fallback
    }

    private suspend fun loadFeaturedAccessories(fallback: List<Accessory>): List<Accessory> = safely(fallback) {
        val accessories = accessoryRepository.getAccessories(
            AccessoryQuery(
                sort = "newest",
                page = 1,
                perPage = HOME_SHELF_LIMIT,
            ),
        ).getOrNull()?.data ?: return@safely fallback

        accessories
            .filter { accessory -> accessory.variants.any { it.availability.isOrderable } }
            .map { accessory ->
                accessory.copy(
                    variants = accessory.variants.sortedByDescending { it.availability.isOrderable },
                )
            }
            .take(HOME_SHELF_LIMIT)
    }

    private companion object {
        const val HOME_SHELF_LIMIT = 4
    }
}
