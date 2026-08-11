package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentRequestListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: AppointmentRequestRepository
    private lateinit var vm: AppointmentRequestListViewModel

    private fun fakeRequest(id: Int) = AppointmentRequest(
        id = id,
        requestNumber = "APR-2026-${id.toString().padStart(6, '0')}",
        status = AppointmentRequestStatus.PENDING,
        patientId = null,
        appointmentType = null,
        scheduledAt = "2026-08-10T10:00:00+08:00",
        alternativeScheduledTimes = emptyList(),
        provisionalDurationMinutes = null,
        reasonForVisit = "Test $id",
        referringSource = null,
        timePreferencesAreReserved = false,
        expiresAt = "2026-08-11T10:00:00+08:00",
        cancelledAt = null,
        createdAt = "2026-08-09T10:00:00+08:00",
        appointmentId = null,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load shows data`() {
        coEvery { repo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(listOf(fakeRequest(1)), 1, 1, 1)
        )
        vm = AppointmentRequestListViewModel(repo)
        val state = vm.state.value as RequestListState.Data
        assertEquals(1, state.requests.size)
    }

    @Test
    fun `initial load error shows error state`() {
        coEvery { repo.getRequests(1, 15) } returns Result.failure(Exception("Network"))
        vm = AppointmentRequestListViewModel(repo)
        val state = vm.state.value as RequestListState.Error
        assertEquals("We couldn't load your requests. Please try again.", state.message)
    }

    @Test
    fun `loadMore appends without duplicates`() {
        coEvery { repo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(listOf(fakeRequest(1)), 1, 2, 2)
        )
        coEvery { repo.getRequests(2, 15) } returns Result.success(
            PaginatedResult(listOf(fakeRequest(1), fakeRequest(2)), 2, 2, 2)
        )
        vm = AppointmentRequestListViewModel(repo)
        vm.loadMore()
        val state = vm.state.value as RequestListState.Data
        assertEquals(2, state.requests.size)
        assertEquals(1, state.requests[0].id)
        assertEquals(2, state.requests[1].id)
    }

    @Test
    fun `refresh resets pagination`() {
        coEvery { repo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(listOf(fakeRequest(1)), 1, 1, 1)
        )
        vm = AppointmentRequestListViewModel(repo)
        coEvery { repo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(listOf(fakeRequest(2)), 1, 1, 1)
        )
        vm.refresh()
        val state = vm.state.value as RequestListState.Data
        assertEquals(1, state.requests.size)
        assertEquals(2, state.requests[0].id)
    }

    @Test
    fun `append failure preserves existing data`() {
        coEvery { repo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(listOf(fakeRequest(1)), 1, 2, 2)
        )
        coEvery { repo.getRequests(2, 15) } returns Result.failure(Exception("Network"))
        vm = AppointmentRequestListViewModel(repo)
        vm.loadMore()
        val state = vm.state.value as RequestListState.Data
        assertEquals(1, state.requests.size)
        assertEquals("We couldn't load more requests. Please try again.", state.appendError)
    }
}
