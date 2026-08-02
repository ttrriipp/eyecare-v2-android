package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentRequestDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: AppointmentRequestRepository
    private lateinit var vm: AppointmentRequestDetailViewModel

    private val pendingRequest = AppointmentRequest(
        id = 1, requestNumber = "APR-2026-000001", status = AppointmentRequestStatus.PENDING,
        patientId = null, scheduledAt = "2026-08-10T10:00:00+08:00", reasonForVisit = "Test",
        expiresAt = "2026-08-11T10:00:00+08:00", cancelledAt = null, createdAt = "2026-08-09T10:00:00+08:00",
        appointmentId = null,
    )

    private val cancelledRequest = pendingRequest.copy(status = AppointmentRequestStatus.CANCELLED, cancelledAt = "2026-08-09T11:00:00+08:00")

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        vm = AppointmentRequestDetailViewModel(repo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load success shows data`() {
        coEvery { repo.getRequest(1) } returns Result.success(pendingRequest)
        vm.load(1)
        val state = vm.state.value as RequestDetailState.Data
        assertEquals("APR-2026-000001", state.request.requestNumber)
        assertTrue(state.request.status.isCancellable)
    }

    @Test
    fun `load REQUEST_NOT_OWNED shows NotFound`() {
        coEvery { repo.getRequest(1) } returns Result.failure(ApiDomainError(404, "REQUEST_NOT_OWNED", "Not found"))
        vm.load(1)
        assertTrue(vm.state.value is RequestDetailState.NotFound)
    }

    @Test
    fun `cancel success updates state`() {
        coEvery { repo.getRequest(1) } returns Result.success(pendingRequest)
        coEvery { repo.cancelRequest(1) } returns Result.success(cancelledRequest)
        vm.load(1)
        vm.cancel()
        val state = vm.state.value as RequestDetailState.Data
        assertEquals(AppointmentRequestStatus.CANCELLED, state.request.status)
        assertFalse(state.request.status.isCancellable)
    }

    @Test
    fun `cancel REQUEST_NOT_CANCELLABLE refreshes`() {
        coEvery { repo.getRequest(1) } returns Result.success(pendingRequest)
        coEvery { repo.cancelRequest(1) } returns Result.failure(ApiDomainError(422, "REQUEST_NOT_CANCELLABLE", "Cannot cancel"))
        val refreshedRequest = pendingRequest.copy(status = AppointmentRequestStatus.ACCEPTED, appointmentId = 42)
        coEvery { repo.getRequest(1) } returns Result.success(refreshedRequest)
        vm.load(1)
        vm.cancel()
        val state = vm.state.value as RequestDetailState.Data
        assertEquals(AppointmentRequestStatus.ACCEPTED, state.request.status)
    }

    @Test
    fun `non-pending request has no cancel action`() {
        val acceptedRequest = pendingRequest.copy(status = AppointmentRequestStatus.ACCEPTED, appointmentId = 42)
        coEvery { repo.getRequest(1) } returns Result.success(acceptedRequest)
        vm.load(1)
        val state = vm.state.value as RequestDetailState.Data
        assertFalse(state.request.status.isCancellable)
    }
}
