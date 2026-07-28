package com.eyecare.app.presentation.home

import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.domain.repository.PaginatedResult
import com.eyecare.app.domain.repository.PrescriptionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var appointmentRepo: AppointmentV1Repository
    private lateinit var frameRepo: FrameRepository
    private lateinit var prescriptionRepo: PrescriptionRepository

    private val futureAppt = AppointmentV1(1, "APT-001", "New Patient", 30, null, AppointmentStatus.SCHEDULED,
        "${LocalDate.now().plusDays(3)}T10:00:00+08:00", null, null, "mobile", null)
    private val pastAppt = AppointmentV1(2, "APT-002", "Follow-up", 15, null, AppointmentStatus.FULFILLED,
        "${LocalDate.now().minusDays(5)}T10:00:00+08:00", null, null, "mobile", null)
    private val expiredPrescription = Prescription(1, 1, null, null, null, null,
        null, null, null, null, null, null, null, null, null,
        prescribedAt = "${LocalDate.now().minusYears(1)}",
        expiresAt = "${LocalDate.now().minusDays(5)}", notes = null)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        appointmentRepo = mockk()
        frameRepo = mockk()
        prescriptionRepo = mockk()
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
        coEvery { frameRepo.getFrames(any()) } returns Result.success(emptyList())
        coEvery { prescriptionRepo.getPrescriptions(any()) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = HomeViewModel(appointmentRepo, frameRepo, prescriptionRepo)

    @Test
    fun `nextAppointment is the soonest future confirmed appointment`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(PaginatedResult(listOf(pastAppt, futureAppt), 1, 1, 2))
        val state = vm().uiState.value as HomeUiState.Success
        assertEquals(futureAppt, state.nextAppointment)
    }

    @Test
    fun `expiringPrescription is set when prescription expires within 30 days`() = runTest {
        coEvery { prescriptionRepo.getPrescriptions(any()) } returns Result.success(PaginatedResult(listOf(expiredPrescription), 1, 1, 1))
        val state = vm().uiState.value as HomeUiState.Success
        assertNotNull(state.expiringPrescription)
    }

    @Test
    fun `expiringPrescription is null when no expiry within 30 days`() = runTest {
        val healthyPrescription = expiredPrescription.copy(
            expiresAt = "${LocalDate.now().plusMonths(6)}"
        )
        coEvery { prescriptionRepo.getPrescriptions(any()) } returns Result.success(PaginatedResult(listOf(healthyPrescription), 1, 1, 1))
        val state = vm().uiState.value as HomeUiState.Success
        assertNull(state.expiringPrescription)
    }

    @Test
    fun `featuredFrames takes first 4 frames`() = runTest {
        val frames = (1..6).map { frame(it) }
        coEvery { frameRepo.getFrames(any()) } returns Result.success(frames)
        val state = vm().uiState.value as HomeUiState.Success
        assertEquals(4, state.featuredFrames.size)
        assertEquals(1, state.featuredFrames[0].id)
        assertEquals(4, state.featuredFrames[3].id)
    }

    @Test
    fun `partial failures do not hide available content`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.failure(RuntimeException("offline"))
        coEvery { frameRepo.getFrames(any()) } returns Result.success(listOf(frame(1)))
        coEvery { prescriptionRepo.getPrescriptions(any()) } returns Result.success(PaginatedResult(listOf(expiredPrescription), 1, 1, 1))
        val state = vm().uiState.value as HomeUiState.Success
        assertNull(state.nextAppointment)
        assertEquals(1, state.featuredFrames.size)
        assertNotNull(state.expiringPrescription)
    }

    private fun frame(id: Int) = Frame(
        id = id,
        name = "Frame $id",
        slug = "frame-$id",
        description = null,
        brand = "Test Brand",
        category = "Eyeglasses",
        variants = emptyList(),
        images = emptyList(),
    )
}
