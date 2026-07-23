package com.eyecare.app.presentation.appointments

import androidx.lifecycle.SavedStateHandle
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.repository.AppointmentRepository
import com.eyecare.app.domain.repository.FeedbackRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class AppointmentDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var appointments: AppointmentRepository
    private lateinit var feedback: FeedbackRepository
    private lateinit var viewModel: AppointmentDetailViewModel

    private val appointment = Appointment(
        id = 4,
        visitReason = "Follow-up",
        status = AppointmentStatus.CONFIRMED,
        scheduledAt = "2026-07-14T09:00:00+08:00",
        contactNotes = "Original note",
        staffNotes = null,
        lastRescheduleReason = "Doctor availability changed",
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        appointments = mockk()
        feedback = mockk()
        coEvery { appointments.getAppointment(4) } returns Result.success(appointment)
        coEvery { feedback.getFeedbackHistory() } returns Result.success(emptyList())
        viewModel = AppointmentDetailViewModel(
            repository = appointments,
            feedbackRepository = feedback,
            savedStateHandle = SavedStateHandle(mapOf("appointmentId" to 4)),
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `start editing contact note opens editor`() = runTest {
        viewModel.startEditingContactNote()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertTrue(state.isEditingContactNote)
        assertEquals(null, state.contactNoteError)
    }

    @Test
    fun `save contact note trims value and uses returned appointment`() = runTest {
        val updated = appointment.copy(contactNotes = "Updated note")
        coEvery { appointments.updateAppointmentContactNote(4, "Updated note") } returns
            Result.success(updated)
        viewModel.startEditingContactNote()

        viewModel.saveContactNote("  Updated note  ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { appointments.updateAppointmentContactNote(4, "Updated note") }
        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertEquals("Updated note", state.appointment.contactNotes)
        assertFalse(state.isEditingContactNote)
        assertFalse(state.isSavingContactNote)
    }

    @Test
    fun `save blank contact note clears value`() = runTest {
        val updated = appointment.copy(contactNotes = null)
        coEvery { appointments.updateAppointmentContactNote(4, null) } returns Result.success(updated)
        viewModel.startEditingContactNote()

        viewModel.saveContactNote("   ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { appointments.updateAppointmentContactNote(4, null) }
        assertEquals(
            null,
            (viewModel.uiState.value as AppointmentDetailUiState.Success).appointment.contactNotes,
        )
    }

    @Test
    fun `failed contact note save keeps editor open with error`() = runTest {
        coEvery { appointments.updateAppointmentContactNote(4, any()) } returns
            Result.failure(RuntimeException("Unable to save note"))
        viewModel.startEditingContactNote()

        viewModel.saveContactNote("Updated note")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertTrue(state.isEditingContactNote)
        assertFalse(state.isSavingContactNote)
        assertEquals("Unable to save note", state.contactNoteError)
    }

    @Test
    fun `customer reschedule uses returned appointment and clears staff reason without refetch`() = runTest {
        val updated = appointment.copy(
            scheduledAt = "2026-07-15T10:00:00+08:00",
            status = AppointmentStatus.PENDING,
            lastRescheduleReason = null,
        )
        coEvery {
            appointments.rescheduleAppointment(4, "2026-07-15T10:00:00+08:00")
        } returns Result.success(updated)

        viewModel.rescheduleAppointment("2026-07-15T10:00:00+08:00")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertEquals(null, state.appointment.lastRescheduleReason)
        assertEquals("2026-07-15T10:00:00+08:00", state.appointment.scheduledAt)
        assertTrue(state.showRescheduleSuccessDialog)
        coVerify(exactly = 1) { appointments.getAppointment(4) }
    }
}
