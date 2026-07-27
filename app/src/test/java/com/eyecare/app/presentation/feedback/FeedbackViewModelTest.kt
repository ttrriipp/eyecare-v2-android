package com.eyecare.app.presentation.feedback

import app.cash.turbine.test
import com.eyecare.app.domain.model.Feedback
import com.eyecare.app.domain.repository.FeedbackRepository
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
class FeedbackViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FeedbackRepository

    private val fakeFeedback = Feedback(1, appointmentId = 1, rating = 5, comment = "Great!")

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `submit success emits Submitted state`() = runTest {
        coEvery { repo.submitFeedback(1, 5, "Great!") } returns Result.success(fakeFeedback)
        val vm = FeedbackViewModel(repo, appointmentId = 1)

        vm.uiState.test {
            assertEquals(FeedbackUiState.Idle, awaitItem())
            vm.submit(5, "Great!")
            assertEquals(FeedbackUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val submitted = awaitItem() as FeedbackUiState.Submitted
            assertEquals(5, submitted.feedback.rating)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit with zero rating shows validation error`() = runTest {
        val vm = FeedbackViewModel(repo, appointmentId = 1)

        vm.uiState.test {
            awaitItem()
            vm.submit(0, null)
            val error = awaitItem() as FeedbackUiState.ValidationError
            assertEquals("Please select a rating", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit error emits Error state`() = runTest {
        coEvery { repo.submitFeedback(1, 3, any()) } returns
            Result.failure(RuntimeException("Server error"))
        val vm = FeedbackViewModel(repo, appointmentId = 1)

        vm.uiState.test {
            awaitItem()
            vm.submit(3, null)
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            val error = awaitItem() as FeedbackUiState.Error
            assertEquals("Server error", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
