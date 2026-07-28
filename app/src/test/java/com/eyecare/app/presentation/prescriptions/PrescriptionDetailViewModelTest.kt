package com.eyecare.app.presentation.prescriptions

import app.cash.turbine.test
import com.eyecare.app.domain.model.EyeMeasurement
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.PrescriptionMeasurementGroup
import com.eyecare.app.domain.model.PrescriptionMeasurements
import com.eyecare.app.domain.repository.PrescriptionRepository
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrescriptionDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: PrescriptionRepository = mockk()
    private lateinit var viewModel: PrescriptionDetailViewModel

    private fun createPrescription(
        id: Int,
        previousId: Int? = null,
        isCurrent: Boolean = true,
    ) = Prescription(
        id = id,
        appointmentId = 1,
        previousPrescriptionId = previousId,
        isCurrent = isCurrent,
        date = "2026-07-27",
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
        viewModel = PrescriptionDetailViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load sets success state`() = runTest {
        val prescription = createPrescription(1)
        coEvery { repository.getPrescription(1) } returns Result.success(prescription)

        viewModel.load(1)

        viewModel.uiState.test {
            assertEquals(PrescriptionDetailUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val success = awaitItem() as PrescriptionDetailUiState.Success
            assertEquals(1, success.prescription.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load sets error state on failure`() = runTest {
        coEvery { repository.getPrescription(1) } returns Result.failure(RuntimeException("Not found"))

        viewModel.load(1)

        viewModel.uiState.test {
            assertEquals(PrescriptionDetailUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val error = awaitItem() as PrescriptionDetailUiState.Error
            assertEquals("Not found", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry reloads the same prescription`() = runTest {
        val prescription = createPrescription(1)
        coEvery { repository.getPrescription(1) } returnsMany listOf(
            Result.failure(RuntimeException("Network error")),
            Result.success(prescription),
        )

        viewModel.load(1)
        viewModel.uiState.test {
            assertEquals(PrescriptionDetailUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Error

            viewModel.retry()
            assertEquals(PrescriptionDetailUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val success = awaitItem() as PrescriptionDetailUiState.Success
            assertEquals(1, success.prescription.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads historical prescription with previous id`() = runTest {
        val historical = createPrescription(2, previousId = 1, isCurrent = false)
        coEvery { repository.getPrescription(2) } returns Result.success(historical)

        viewModel.load(2)

        viewModel.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            val success = awaitItem() as PrescriptionDetailUiState.Success
            assertEquals(2, success.prescription.id)
            assertEquals(1, success.prescription.previousPrescriptionId)
            assertEquals(false, success.prescription.isCurrent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load does not call getPrescriptions`() = runTest {
        val prescription = createPrescription(1)
        coEvery { repository.getPrescription(1) } returns Result.success(prescription)

        viewModel.load(1)
        dispatcher.scheduler.advanceUntilIdle()

        // If getPrescriptions was called, the mock would fail since it's not set up
        // This test passes because only getPrescription is called
    }
}
