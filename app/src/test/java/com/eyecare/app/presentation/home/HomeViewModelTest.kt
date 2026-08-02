package com.eyecare.app.presentation.home

import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.EyeMeasurement
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.PrescriptionMeasurementGroup
import com.eyecare.app.domain.model.PrescriptionMeasurements
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.domain.repository.PaginatedResult
import com.eyecare.app.domain.repository.PrescriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
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

    private fun createPrescription(id: Int, isCurrent: Boolean, date: String) = Prescription(
        id = id,
        appointmentId = 1,
        previousPrescriptionId = null,
        isCurrent = isCurrent,
        date = date,
        measurements = PrescriptionMeasurements(
            main = PrescriptionMeasurementGroup(
                od = EyeMeasurement(null, "-2.00", "-0.50"),
                os = EyeMeasurement(null, "-1.75", "-0.25"),
            ),
            add = PrescriptionMeasurementGroup(
                od = EyeMeasurement(null, null, null),
                os = EyeMeasurement(null, null, null),
            ),
        ),
        remarks = null,
    )

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

    private fun vm() = HomeViewModel(appointmentRepo, frameRepo, prescriptionRepo).also { it.load() }

    @Test
    fun `nextAppointment is the soonest future scheduled appointment`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(PaginatedResult(listOf(pastAppt, futureAppt), 1, 1, 2))
        val state = vm().uiState.value as HomeUiState.Success
        assertEquals(futureAppt, state.nextAppointment)
    }

    @Test
    fun `currentPrescription is the latest current prescription`() = runTest {
        val current = createPrescription(1, true, "2026-07-27")
        val previous = createPrescription(2, false, "2026-06-15")
        coEvery { prescriptionRepo.getPrescriptions(any()) } returns Result.success(PaginatedResult(listOf(current, previous), 1, 1, 2))
        val state = vm().uiState.value as HomeUiState.Success
        assertNotNull(state.currentPrescription)
        assertEquals(1, state.currentPrescription?.id)
    }

    @Test
    fun `currentPrescription is null when no current prescription exists`() = runTest {
        val previous = createPrescription(1, false, "2026-06-15")
        coEvery { prescriptionRepo.getPrescriptions(any()) } returns Result.success(PaginatedResult(listOf(previous), 1, 1, 1))
        val state = vm().uiState.value as HomeUiState.Success
        assertNull(state.currentPrescription)
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
        val current = createPrescription(1, true, "2026-07-27")
        coEvery { prescriptionRepo.getPrescriptions(any()) } returns Result.success(PaginatedResult(listOf(current), 1, 1, 1))
        val state = vm().uiState.value as HomeUiState.Success
        assertNull(state.nextAppointment)
        assertEquals(1, state.featuredFrames.size)
        assertNotNull(state.currentPrescription)
    }

    @Test
    fun `limited load skips active-link repositories`() = runTest {
        val limitedVm = HomeViewModel(appointmentRepo, frameRepo, prescriptionRepo)

        limitedVm.load(hasActivePatientLink = false)

        val state = limitedVm.uiState.value as HomeUiState.Success
        assertNull(state.nextAppointment)
        assertNull(state.currentPrescription)
        assertEquals(emptyList<Frame>(), state.featuredFrames)
        coVerify(exactly = 0) { appointmentRepo.getAppointments(any()) }
        coVerify(exactly = 0) { frameRepo.getFrames(any()) }
        coVerify(exactly = 0) { prescriptionRepo.getPrescriptions(any()) }
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
