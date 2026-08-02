package com.eyecare.app.presentation.appointments.booking

import app.cash.turbine.test
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentSlot
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.PaginatedResult
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookAppointmentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: AppointmentV1Repository
    private lateinit var vm: BookAppointmentViewModel

    private val fakeAppt = AppointmentV1(
        id = 99,
        appointmentNumber = "APT-099",
        appointmentType = "New Patient",
        durationMinutes = 30,
        referringSource = null,
        status = AppointmentStatus.SCHEDULED,
        scheduledAt = "2026-10-24T09:00:00+08:00",
        contactNotes = null,
        reasonForVisit = null,
        lastRescheduleReason = null,
        source = "mobile",
        assignedOptometrist = null,
    )
    private val fakeTypes = listOf(
        AppointmentType(1, "New Patient", 30, false),
        AppointmentType(2, "Follow-up", 15, false),
    )
    private val fakeAvailability = AppointmentAvailability(
        date = "2026-10-24",
        timezone = "Asia/Manila",
        intervalMinutes = 15,
        visitReasonId = 1,
        visitDurationMinutes = 30,
        optometristId = null,
        appointmentId = null,
        dayStatus = "open",
        generatedAt = "2026-10-24T08:00:00+08:00",
        slots = listOf(
            AppointmentSlot(
                startsAt = "2026-10-24T09:00:00+08:00",
                endsAt = "2026-10-24T09:30:00+08:00",
                available = true,
                reason = null,
            ),
            AppointmentSlot(
                startsAt = "2026-10-24T09:15:00+08:00",
                endsAt = "2026-10-24T09:45:00+08:00",
                available = false,
                reason = "capacity_reached",
            ),
        ),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        coEvery { repo.getAppointmentTypes() } returns Result.success(fakeTypes)
        coEvery { repo.getAppointmentAvailability(any(), any(), any(), any()) } returns
            Result.success(fakeAvailability)
        coEvery { repo.getAppointments(any()) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0),
        )
        vm = BookAppointmentViewModel(repo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial step is 1`() = runTest {
        assertEquals(1, vm.uiState.value.step)
    }

    @Test
    fun `selectType advances to step 2`() = runTest {
        vm.selectType(fakeTypes[0])
        assertEquals(2, vm.uiState.value.step)
        assertEquals("New Patient", vm.uiState.value.selectedTypeName)
    }

    @Test
    fun `selectDate advances to step 3`() = runTest {
        vm.selectType(fakeTypes[0])
        vm.selectDate("2026-10-24")
        assertEquals(3, vm.uiState.value.step)
        assertEquals("2026-10-24", vm.uiState.value.selectedDate)
    }

    @Test
    fun `selectDate loads backend availability`() = runTest {
        vm.selectType(fakeTypes[0])

        vm.uiState.test {
            awaitItem()
            vm.selectDate("2026-10-24")
            val loading = awaitItem()
            assertEquals(true, loading.availabilityLoading)
            dispatcher.scheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assertEquals(fakeAvailability, loaded.availability)
            assertEquals(false, loaded.availabilityLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectTime advances to step 4`() = runTest {
        vm.selectType(fakeTypes[0])
        vm.selectDate("2026-10-24")
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectTime("2026-10-24T09:00:00+08:00")
        assertEquals(4, vm.uiState.value.step)
        assertEquals("2026-10-24T09:00:00+08:00", vm.uiState.value.selectedDateTime)
    }

    @Test
    fun `selectTime ignores unavailable backend slot`() = runTest {
        vm.selectType(fakeTypes[0])
        vm.selectDate("2026-10-24")
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectTime("2026-10-24T09:15:00+08:00")

        assertEquals(3, vm.uiState.value.step)
        assertEquals(null, vm.uiState.value.selectedDateTime)
    }

    @Test
    fun `goBack from step 2 returns to step 1`() = runTest {
        vm.selectType(fakeTypes[0])
        vm.goBack()
        assertEquals(1, vm.uiState.value.step)
    }

    @Test
    fun `goBack from step 4 returns to step 3 preserving selections`() = runTest {
        vm.selectType(fakeTypes[0])
        vm.selectDate("2026-10-24")
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectTime("2026-10-24T09:00:00+08:00")
        vm.goBack()
        assertEquals(3, vm.uiState.value.step)
        assertEquals("New Patient", vm.uiState.value.selectedTypeName)
    }

    @Test
    fun `submit success emits Submitted state`() = runTest {
        coEvery { repo.createAppointment(any(), any(), any(), any()) } returns Result.success(fakeAppt)
        vm.selectType(fakeTypes[0])
        vm.selectDate("2026-10-24")
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectTime("2026-10-24T09:00:00+08:00")

        vm.uiState.test {
            awaitItem() // current state (step 4)
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
        coEvery { repo.createAppointment(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("Server error"))
        vm.selectType(fakeTypes[0])
        vm.selectDate("2026-10-24")
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectTime("2026-10-24T09:00:00+08:00")

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
        coEvery { repo.createAppointment(any(), any(), null, any()) } returns Result.success(fakeAppt)
        vm.selectType(fakeTypes[1])
        vm.selectDate("2026-10-24")
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectTime("2026-10-24T09:00:00+08:00")

        vm.uiState.test {
            awaitItem()
            vm.submit(null)
            awaitItem() // loading
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(BookingResult.Success::class.java, awaitItem().result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stale slot returns to time selection and refreshes availability`() = runTest {
        coEvery { repo.createAppointment(any(), any(), any(), any()) } returns Result.failure(
            AppointmentError.ValidationError(
                fieldErrors = mapOf("scheduled_at" to listOf("This time slot is not available.")),
                code = "SLOT_UNAVAILABLE",
            ),
        )
        vm.selectType(fakeTypes[0])
        vm.selectDate("2026-10-24")
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectTime("2026-10-24T09:00:00+08:00")

        vm.submit(null)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(3, state.step)
        assertEquals(null, state.selectedDateTime)
        assertEquals("That time was just taken. Choose another available time.", state.availabilityNotice)
        assertEquals(fakeAvailability, state.availability)
    }
}
