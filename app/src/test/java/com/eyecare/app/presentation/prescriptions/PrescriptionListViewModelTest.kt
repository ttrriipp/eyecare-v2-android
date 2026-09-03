package com.eyecare.app.presentation.prescriptions

import app.cash.turbine.test
import com.eyecare.app.domain.model.EyeMeasurement
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.PrescriptionMeasurementGroup
import com.eyecare.app.domain.model.PrescriptionMeasurements
import com.eyecare.app.domain.repository.PaginatedResult
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrescriptionListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: PrescriptionRepository = mockk()
    private lateinit var viewModel: PrescriptionListViewModel

    private fun createPrescription(id: Int, date: String = "2026-07-27") = Prescription(
        id = id,
        appointmentId = 1,
        previousPrescriptionId = null,
        isCurrent = true,
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
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load shows loading then success`() = runTest {
        val prescriptions = listOf(createPrescription(1))
        coEvery { repository.getPrescriptions(1) } returns Result.success(
            PaginatedResult(prescriptions, 1, 1, 1)
        )

        viewModel = PrescriptionListViewModel(repository)

        viewModel.uiState.test {
            assertEquals(PrescriptionListUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val success = awaitItem() as PrescriptionListUiState.Success
            assertEquals(1, success.prescriptions.size)
            assertFalse(success.hasMorePages)
            assertFalse(success.isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh keeps current prescriptions visible while loading`() = runTest {
        val initialPrescriptions = listOf(createPrescription(1))
        val refreshedPrescriptions = listOf(createPrescription(2))
        coEvery { repository.getPrescriptions(1) } returnsMany listOf(
            Result.success(PaginatedResult(initialPrescriptions, 1, 1, 1)),
            Result.success(PaginatedResult(refreshedPrescriptions, 1, 1, 1)),
        )

        viewModel = PrescriptionListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()

        val refreshing = viewModel.uiState.value as PrescriptionListUiState.Success
        assertEquals(initialPrescriptions, refreshing.prescriptions)
        assertTrue(refreshing.isRefreshing)

        dispatcher.scheduler.advanceUntilIdle()

        val refreshed = viewModel.uiState.value as PrescriptionListUiState.Success
        assertEquals(refreshedPrescriptions, refreshed.prescriptions)
        assertFalse(refreshed.isRefreshing)
    }

    @Test
    fun `empty list shows empty state`() = runTest {
        coEvery { repository.getPrescriptions(1) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )

        viewModel = PrescriptionListViewModel(repository)

        viewModel.uiState.test {
            assertEquals(PrescriptionListUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(PrescriptionListUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error shows error state`() = runTest {
        coEvery { repository.getPrescriptions(1) } returns Result.failure(RuntimeException("Network error"))

        viewModel = PrescriptionListViewModel(repository)

        viewModel.uiState.test {
            assertEquals(PrescriptionListUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val error = awaitItem() as PrescriptionListUiState.Error
            assertEquals("Network error", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `preserves server order without client sorting`() = runTest {
        val p1 = createPrescription(1, "2026-07-20")
        val p2 = createPrescription(2, "2026-07-27")
        coEvery { repository.getPrescriptions(1) } returns Result.success(
            PaginatedResult(listOf(p1, p2), 1, 1, 2)
        )

        viewModel = PrescriptionListViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            val success = awaitItem() as PrescriptionListUiState.Success
            // Server order preserved: p1 first, p2 second
            assertEquals(1, success.prescriptions[0].id)
            assertEquals(2, success.prescriptions[1].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMore appends and sets hasMorePages`() = runTest {
        val p1 = createPrescription(1)
        val p2 = createPrescription(2)
        coEvery { repository.getPrescriptions(1) } returns Result.success(
            PaginatedResult(listOf(p1), 1, 2, 2)
        )
        coEvery { repository.getPrescriptions(2) } returns Result.success(
            PaginatedResult(listOf(p2), 2, 2, 2)
        )

        viewModel = PrescriptionListViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            val initial = awaitItem() as PrescriptionListUiState.Success
            assertTrue(initial.hasMorePages)

            viewModel.loadMore()
            val loadingMore = awaitItem() as PrescriptionListUiState.Success
            assertTrue(loadingMore.isLoadingMore)

            dispatcher.scheduler.advanceUntilIdle()
            val final = awaitItem() as PrescriptionListUiState.Success
            assertEquals(2, final.prescriptions.size)
            assertFalse(final.hasMorePages)
            assertFalse(final.isLoadingMore)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMore guards duplicate calls`() = runTest {
        val p1 = createPrescription(1)
        coEvery { repository.getPrescriptions(1) } returns Result.success(
            PaginatedResult(listOf(p1), 1, 1, 1)
        )

        viewModel = PrescriptionListViewModel(repository)

        viewModel.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success

            // Should not trigger another load since hasMorePages is false
            viewModel.loadMore()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
