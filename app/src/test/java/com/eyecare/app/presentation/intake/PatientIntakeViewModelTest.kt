package com.eyecare.app.presentation.intake

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.eyecare.app.data.repository.IntakeError
import com.eyecare.app.domain.model.IntakeStatus
import com.eyecare.app.domain.model.PatientIntake
import com.eyecare.app.domain.repository.PatientIntakeRepository
import com.eyecare.app.domain.repository.SaveIntakeRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatientIntakeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: PatientIntakeRepository

    private val draftIntake = PatientIntake(
        id = 1, patientId = 1, appointmentId = 5, status = IntakeStatus.DRAFT,
        appointmentType = "New Patient", fullName = "Ana Reyes", dateOfBirth = "1990-05-15",
        gender = "female", occupation = "Teacher", address = "123 Main St",
        phone = "09171234567", email = "ana@example.com", chiefComplaint = "Blurred vision",
        pastOcularHistory = null, pastSurgicalHistory = null, pastMedicalHistory = null,
        allergies = null, medications = null, submittedAt = null, verifiedAt = null,
    )

    private val submittedIntake = draftIntake.copy(
        status = IntakeStatus.SUBMITTED,
        submittedAt = "2026-07-27T10:00:00+08:00",
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun createVm(appointmentId: Int = 5): PatientIntakeViewModel {
        coEvery { repo.getIntake(appointmentId) } returns Result.success(draftIntake)
        return PatientIntakeViewModel(repo, SavedStateHandle(mapOf("appointmentId" to appointmentId)))
    }

    @Test
    fun `load populates draft from intake`() = runTest {
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PatientIntakeUiState.Success
        assertEquals("Ana Reyes", state.draft.fullName)
        assertEquals("Blurred vision", state.draft.chiefComplaint)
        assertEquals(IntakeStatus.DRAFT, state.intake?.status)
    }

    @Test
    fun `submitted intake has no edit action`() = runTest {
        coEvery { repo.getIntake(5) } returns Result.success(submittedIntake)
        val vm = PatientIntakeViewModel(repo, SavedStateHandle(mapOf("appointmentId" to 5)))
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PatientIntakeUiState.Success
        assertEquals(IntakeStatus.SUBMITTED, state.intake?.status)
    }

    @Test
    fun `saveDraft updates intake from response`() = runTest {
        val updated = draftIntake.copy(chiefComplaint = "Updated complaint")
        coEvery { repo.saveIntake(5, any()) } returns Result.success(updated)
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.updateDraft { it.copy(chiefComplaint = "Updated complaint") }
        vm.saveDraft()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PatientIntakeUiState.Success
        assertEquals("Updated complaint", state.intake?.chiefComplaint)
        assertFalse(state.isSaving)
    }

    @Test
    fun `saveDraft 422 maps field errors`() = runTest {
        coEvery { repo.saveIntake(5, any()) } returns Result.failure(
            IntakeError.ValidationError(mapOf("date_of_birth" to listOf("The date of birth must be a date before today."))),
        )
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.saveDraft()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PatientIntakeUiState.Success
        assertEquals("The date of birth must be a date before today.", state.fieldErrors["date_of_birth"]?.first())
    }

    @Test
    fun `submitIntake transitions to submitted`() = runTest {
        coEvery { repo.submitIntake(5) } returns Result.success(submittedIntake)
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.submitIntake()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PatientIntakeUiState.Success
        assertEquals(IntakeStatus.SUBMITTED, state.intake?.status)
        assertTrue(state.submitSuccess)
    }

    @Test
    fun `submitIntake 422 shows error`() = runTest {
        coEvery { repo.submitIntake(5) } returns Result.failure(
            IntakeError.ValidationError(mapOf("status" to listOf("Only draft intakes can be submitted."))),
        )
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.submitIntake()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PatientIntakeUiState.Success
        assertEquals("Only draft intakes can be submitted.", state.submitError)
    }

    @Test
    fun `no intake creates empty draft`() = runTest {
        coEvery { repo.getIntake(5) } returns Result.success(null)
        val vm = PatientIntakeViewModel(repo, SavedStateHandle(mapOf("appointmentId" to 5)))
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PatientIntakeUiState.Success
        assertEquals(null, state.intake)
        assertEquals("", state.draft.fullName)
    }
}
