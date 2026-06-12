package com.eyecare.app.presentation.appointments.booking

import app.cash.turbine.test
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.repository.AppointmentRepository
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookAppointmentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: AppointmentRepository
    private lateinit var vm: BookAppointmentViewModel

    private val fakeAppt = Appointment(99, "eye_exam", AppointmentStatus.PENDING, "2026-10-24T09:00:00Z", null, null)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        vm = BookAppointmentViewModel(repo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial step is 1`() = runTest {
        assertEquals(1, vm.uiState.value.step)
    }

    @Test
    fun `selectReason advances to step 2`() = runTest {
        vm.selectReason("eye_exam")
        assertEquals(2, vm.uiState.value.step)
        assertEquals("eye_exam", vm.uiState.value.selectedReason)
    }

    @Test
    fun `selectDateTime advances to step 3`() = runTest {
        vm.selectReason("eye_exam")
        vm.selectDateTime("2026-10-24T09:00:00Z")
        assertEquals(3, vm.uiState.value.step)
        assertEquals("2026-10-24T09:00:00Z", vm.uiState.value.selectedDateTime)
    }

    @Test
    fun `goBack from step 2 returns to step 1`() = runTest {
        vm.selectReason("eye_exam")
        vm.goBack()
        assertEquals(1, vm.uiState.value.step)
    }

    @Test
    fun `goBack from step 3 returns to step 2 preserving selections`() = runTest {
        vm.selectReason("eye_exam")
        vm.selectDateTime("2026-10-24T09:00:00Z")
        vm.goBack()
        assertEquals(2, vm.uiState.value.step)
        assertEquals("eye_exam", vm.uiState.value.selectedReason) // preserved
    }

    @Test
    fun `submit success emits Submitted state`() = runTest {
        coEvery { repo.createAppointment(any(), any(), anyNullable()) } returns Result.success(fakeAppt)
        vm.selectReason("eye_exam")
        vm.selectDateTime("2026-10-24T09:00:00Z")

        vm.uiState.test {
            awaitItem() // current state (step 3)
            vm.submit("Call ahead please")
            val loading = awaitItem()
            assertEquals(true, loading.isLoading)
            dispatcher.scheduler.advanceUntilIdle()
            val submitted = awaitItem()
            assertInstanceOf(BookingResult.Success::class.java, submitted.result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit error emits Error result`() = runTest {
        coEvery { repo.createAppointment(any(), any(), anyNullable()) } returns
            Result.failure(RuntimeException("Server error"))
        vm.selectReason("eye_exam")
        vm.selectDateTime("2026-10-24T09:00:00Z")

        vm.uiState.test {
            awaitItem()
            vm.submit(null)
            awaitItem() // loading
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            assertInstanceOf(BookingResult.Error::class.java, state.result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `notes are optional — null notes submitted correctly`() = runTest {
        coEvery { repo.createAppointment(any(), any(), null) } returns Result.success(fakeAppt)
        vm.selectReason("follow_up")
        vm.selectDateTime("2026-10-24T09:00:00Z")

        vm.uiState.test {
            awaitItem()
            vm.submit(null)
            awaitItem() // loading
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(BookingResult.Success::class.java, awaitItem().result)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
