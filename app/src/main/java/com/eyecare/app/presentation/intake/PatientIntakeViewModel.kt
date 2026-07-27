package com.eyecare.app.presentation.intake

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.IntakeStatus
import com.eyecare.app.domain.model.PatientIntake
import com.eyecare.app.domain.repository.PatientIntakeRepository
import com.eyecare.app.domain.repository.SaveIntakeRequest
import com.eyecare.app.data.repository.IntakeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PatientIntakeUiState {
    data object Loading : PatientIntakeUiState
    data class Success(
        val intake: PatientIntake?,
        val draft: IntakeDraft = IntakeDraft(),
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val fieldErrors: Map<String, List<String>> = emptyMap(),
        val isSubmitting: Boolean = false,
        val submitError: String? = null,
        val submitSuccess: Boolean = false,
    ) : PatientIntakeUiState
    data class Error(val message: String) : PatientIntakeUiState
}

data class IntakeDraft(
    val fullName: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val occupation: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val chiefComplaint: String = "",
    val pastOcularHistory: String = "",
    val pastSurgicalHistory: String = "",
    val pastMedicalHistory: String = "",
    val allergies: String = "",
    val medications: String = "",
)

@HiltViewModel
class PatientIntakeViewModel @Inject constructor(
    private val repository: PatientIntakeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val appointmentId: Int = checkNotNull(savedStateHandle["appointmentId"])

    private val _uiState = MutableStateFlow<PatientIntakeUiState>(PatientIntakeUiState.Loading)
    val uiState: StateFlow<PatientIntakeUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    fun updateDraft(update: (IntakeDraft) -> IntakeDraft) {
        val current = _uiState.value
        if (current !is PatientIntakeUiState.Success) return
        if (current.intake?.status != IntakeStatus.DRAFT && current.intake != null) return
        _uiState.value = current.copy(draft = update(current.draft), fieldErrors = emptyMap(), saveError = null)
    }

    fun saveDraft() {
        val current = _uiState.value
        if (current !is PatientIntakeUiState.Success) return
        if (current.intake?.status != IntakeStatus.DRAFT && current.intake != null) return
        _uiState.value = current.copy(isSaving = true, saveError = null, fieldErrors = emptyMap())
        viewModelScope.launch {
            repository.saveIntake(appointmentId, current.draft.toRequest()).fold(
                onSuccess = { intake ->
                    _uiState.value = current.copy(
                        intake = intake,
                        isSaving = false,
                        saveError = null,
                    )
                },
                onFailure = { error ->
                    if (error is IntakeError.ValidationError) {
                        _uiState.value = current.copy(
                            isSaving = false,
                            fieldErrors = error.fieldErrors,
                        )
                    } else {
                        _uiState.value = current.copy(
                            isSaving = false,
                            saveError = error.message ?: "Failed to save",
                        )
                    }
                },
            )
        }
    }

    fun submitIntake() {
        val current = _uiState.value
        if (current !is PatientIntakeUiState.Success) return
        if (current.intake?.status != IntakeStatus.DRAFT) return
        _uiState.value = current.copy(isSubmitting = true, submitError = null)
        viewModelScope.launch {
            repository.submitIntake(appointmentId).fold(
                onSuccess = { intake ->
                    _uiState.value = current.copy(
                        intake = intake,
                        isSubmitting = false,
                        submitSuccess = true,
                    )
                },
                onFailure = { error ->
                    if (error is IntakeError.ValidationError) {
                        _uiState.value = current.copy(
                            isSubmitting = false,
                            submitError = error.fieldErrors.values.flatten().firstOrNull() ?: "Submit failed",
                        )
                    } else {
                        _uiState.value = current.copy(
                            isSubmitting = false,
                            submitError = error.message ?: "Failed to submit",
                        )
                    }
                },
            )
        }
    }

    private fun load() {
        _uiState.value = PatientIntakeUiState.Loading
        viewModelScope.launch {
            repository.getIntake(appointmentId).fold(
                onSuccess = { intake ->
                    _uiState.value = PatientIntakeUiState.Success(
                        intake = intake,
                        draft = intake?.toDraft() ?: IntakeDraft(),
                    )
                },
                onFailure = {
                    _uiState.value = PatientIntakeUiState.Error(it.message ?: "Failed to load intake")
                },
            )
        }
    }

    private fun PatientIntake.toDraft() = IntakeDraft(
        fullName = fullName ?: "",
        dateOfBirth = dateOfBirth ?: "",
        gender = gender ?: "",
        occupation = occupation ?: "",
        address = address ?: "",
        phone = phone ?: "",
        email = email ?: "",
        chiefComplaint = chiefComplaint ?: "",
        pastOcularHistory = pastOcularHistory ?: "",
        pastSurgicalHistory = pastSurgicalHistory ?: "",
        pastMedicalHistory = pastMedicalHistory ?: "",
        allergies = allergies ?: "",
        medications = medications ?: "",
    )

    private fun IntakeDraft.toRequest() = SaveIntakeRequest(
        fullName = fullName.ifBlank { null },
        dateOfBirth = dateOfBirth.ifBlank { null },
        gender = gender.ifBlank { null },
        occupation = occupation.ifBlank { null },
        address = address.ifBlank { null },
        phone = phone.ifBlank { null },
        email = email.ifBlank { null },
        chiefComplaint = chiefComplaint.ifBlank { null },
        pastOcularHistory = pastOcularHistory.ifBlank { null },
        pastSurgicalHistory = pastSurgicalHistory.ifBlank { null },
        pastMedicalHistory = pastMedicalHistory.ifBlank { null },
        allergies = allergies.ifBlank { null },
        medications = medications.ifBlank { null },
    )
}
