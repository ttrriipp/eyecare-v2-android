package com.eyecare.app.presentation.appointments

import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.PaginatedResult
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: AppointmentV1Repository

    private val firstAppointment = appointment(id = 1)
    private val secondAppointment = appointment(id = 2)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `refresh failure retains history and exposes a safe retry error`() = runTest {
        coEvery { repository.getAppointmentHistory(any(), any()) } returnsMany listOf(
            page(firstAppointment),
            Result.failure(RuntimeException("database details")),
        )
        val viewModel = AppointmentHistoryViewModel(repository)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentHistoryUiState.Content
        assertEquals(listOf(firstAppointment), state.appointments)
        assertFalse(state.isRefreshing)
        assertEquals("Something went wrong. Please try again.", state.refreshError)
    }

    @Test
    fun `refresh failure preserves the loaded page cursor`() = runTest {
        val thirdAppointment = appointment(id = 3)
        coEvery { repository.getAppointmentHistory(any(), any()) } returnsMany listOf(
            page(firstAppointment, lastPage = 3),
            page(secondAppointment, page = 2, lastPage = 3),
            Result.failure(RuntimeException("refresh failed")),
            page(thirdAppointment, page = 3, lastPage = 3),
        )
        val viewModel = AppointmentHistoryViewModel(repository)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getAppointmentHistory(3, 15) }
        val state = viewModel.uiState.value as AppointmentHistoryUiState.Content
        assertEquals(listOf(firstAppointment, secondAppointment, thirdAppointment), state.appointments)
    }

    @Test
    fun `load more exposes retry and deduplicates appointments`() = runTest {
        coEvery { repository.getAppointmentHistory(any(), any()) } returnsMany listOf(
            page(firstAppointment, lastPage = 2),
            Result.failure(RuntimeException("page failed")),
            page(firstAppointment, secondAppointment, page = 2, lastPage = 2),
        )
        val viewModel = AppointmentHistoryViewModel(repository)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val failed = viewModel.uiState.value as AppointmentHistoryUiState.Content
        assertNotNull(failed.loadMoreError)
        assertFalse(failed.isLoadingMore)

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val recovered = viewModel.uiState.value as AppointmentHistoryUiState.Content
        assertEquals(listOf(firstAppointment, secondAppointment), recovered.appointments)
        assertEquals(null, recovered.loadMoreError)
    }

    @Test
    fun `repeated load more taps make one request while loading`() = runTest {
        coEvery { repository.getAppointmentHistory(any(), any()) } returnsMany listOf(
            page(firstAppointment, lastPage = 2),
            page(secondAppointment, page = 2, lastPage = 2),
        )
        val viewModel = AppointmentHistoryViewModel(repository)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.loadMore()
        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getAppointmentHistory(2, 15) }
        assertEquals(2, (viewModel.uiState.value as AppointmentHistoryUiState.Content).appointments.size)
    }

    @Test
    fun `rating updates the history row without reloading history`() = runTest {
        val rateable = firstAppointment.copy(isRateable = true)
        val rating = VisitRating(
            rating = 5,
            comment = "Very helpful",
            createdAt = "2026-08-20T10:00:00+08:00",
        )
        coEvery { repository.getAppointmentHistory(any(), any()) } returns page(rateable)
        coEvery { repository.rateAppointment(1, 5, "Very helpful") } returns Result.success(rating)
        val viewModel = AppointmentHistoryViewModel(repository)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.showRatingDialog(1)
        viewModel.submitRating(5, "Very helpful")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentHistoryUiState.Content
        assertEquals(null, state.ratingAppointmentId)
        assertFalse(state.isSubmittingRating)
        assertEquals(rating, state.appointments.single().visitRating)
        coVerify(exactly = 1) { repository.getAppointmentHistory(1, 15) }
    }

    @Test
    fun `existing rating can be opened for an update`() = runTest {
        val existingRating = VisitRating(
            rating = 3,
            comment = "It was okay",
            createdAt = "2026-08-20T10:00:00+08:00",
        )
        val rateable = firstAppointment.copy(
            isRateable = true,
            visitRating = existingRating,
        )
        coEvery { repository.getAppointmentHistory(any(), any()) } returns page(rateable)

        val viewModel = AppointmentHistoryViewModel(repository)
        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.showRatingDialog(1)

        val state = viewModel.uiState.value as AppointmentHistoryUiState.Content
        assertEquals(1, state.ratingAppointmentId)
        assertFalse(state.isSubmittingRating)
    }

    @Test
    fun `rating failure keeps dialog open with safe error`() = runTest {
        val rateable = firstAppointment.copy(isRateable = true)
        coEvery { repository.getAppointmentHistory(any(), any()) } returns page(rateable)
        coEvery { repository.rateAppointment(1, 4, null) } returns
            Result.failure(RuntimeException("private backend details"))
        val viewModel = AppointmentHistoryViewModel(repository)

        viewModel.load()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.showRatingDialog(1)
        viewModel.submitRating(4, null)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentHistoryUiState.Content
        assertEquals(1, state.ratingAppointmentId)
        assertFalse(state.isSubmittingRating)
        assertEquals("We couldn't submit your rating. Try again.", state.ratingError)
    }

    private fun page(
        vararg appointments: AppointmentV1,
        page: Int = 1,
        lastPage: Int = 1,
    ): Result<PaginatedResult<AppointmentV1>> = Result.success(
        PaginatedResult(
            data = appointments.toList(),
            currentPage = page,
            lastPage = lastPage,
            total = appointments.size,
        ),
    )

    private companion object {
        fun appointment(id: Int) = AppointmentV1(
            id = id,
            appointmentNumber = "APT-$id",
            appointmentType = "Eye examination",
            durationMinutes = 30,
            referringSource = null,
            status = AppointmentStatus.FULFILLED,
            scheduledAt = "2026-08-${10 + id}T09:00:00+08:00",
            contactNotes = null,
            reasonForVisit = null,
            lastRescheduleReason = null,
            source = "mobile",
            assignedOptometrist = null,
        )
    }
}
