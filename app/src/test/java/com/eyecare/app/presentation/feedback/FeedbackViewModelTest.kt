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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FeedbackRepository

    private val fakeFeedback = Feedback(1, appointmentId = 1, orderId = null,
        rating = 5, comment = "Great!")

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        coEvery { repo.getFeedbackHistory() } returns Result.success(listOf(fakeFeedback))
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `submit without rating emits validation error`() = runTest {
        val vm = FeedbackViewModel(repo, appointmentId = 1, orderId = null)
        vm.submit(rating = 0, comment = "")
        assertInstanceOf(FeedbackUiState.ValidationError::class.java, vm.uiState.value)
    }

    @Test
    fun `submit with valid rating emits Loading then Submitted`() = runTest {
        coEvery { repo.submitFeedback(1, null, 5, "Great!") } returns Result.success(fakeFeedback)
        val vm = FeedbackViewModel(repo, appointmentId = 1, orderId = null)

        vm.uiState.test {
            awaitItem() // Idle
            vm.submit(rating = 5, comment = "Great!")
            val loading = awaitItem()
            assertInstanceOf(FeedbackUiState.Loading::class.java, loading)
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(FeedbackUiState.Submitted::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit failure emits Error`() = runTest {
        coEvery { repo.submitFeedback(any(), any(), any(), any()) } returns
            Result.failure(RuntimeException("server error"))
        val vm = FeedbackViewModel(repo, appointmentId = null, orderId = 1)

        vm.uiState.test {
            awaitItem()
            vm.submit(rating = 4, comment = null)
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(FeedbackUiState.Error::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `history loads list of feedback`() = runTest {
        coEvery { repo.getFeedbackHistory() } returns Result.success(listOf(fakeFeedback))
        val vm = FeedbackViewModel(repo, appointmentId = 1, orderId = null)
        dispatcher.scheduler.advanceUntilIdle()
        val history = vm.history.value as FeedbackHistoryUiState.Success
        assertEquals(1, history.items.size)
        assertEquals(5, history.items[0].rating)
    }
}
