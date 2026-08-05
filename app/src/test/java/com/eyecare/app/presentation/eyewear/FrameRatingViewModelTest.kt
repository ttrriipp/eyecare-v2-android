package com.eyecare.app.presentation.eyewear

import app.cash.turbine.test
import com.eyecare.app.domain.model.RatingResult
import com.eyecare.app.domain.repository.OpticalOrderRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FrameRatingViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository: OpticalOrderRepository = mockk()

    @BeforeEach
    fun setup() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `submitRating sends item ID only`() = runTest {
        coEvery { repository.rateItem(10, 5, "Great!") } returns Result.success(
            RatingResult(id = 1, itemId = 10, rating = 5, comment = "Great!", revisionNumber = 1)
        )
        val vm = FrameRatingViewModel(repository, orderItemId = 10)
        vm.submitRating(5, "Great!")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as FrameRatingUiState.Success
        assertEquals(5, state.result.rating)
    }

    @Test
    fun `submitRating validates range`() = runTest {
        val vm = FrameRatingViewModel(repository, orderItemId = 10)
        vm.submitRating(0, null)
        assertTrue(vm.uiState.value is FrameRatingUiState.Error)
    }

    @Test
    fun `submitRating validates max comment length`() = runTest {
        val vm = FrameRatingViewModel(repository, orderItemId = 10)
        vm.submitRating(5, "a".repeat(1001))
        assertTrue(vm.uiState.value is FrameRatingUiState.Error)
    }

    @Test
    fun `submitRating handles failure`() = runTest {
        coEvery { repository.rateItem(10, 3, null) } returns Result.failure(RuntimeException("ORDER_NOT_DISPENSED"))
        val vm = FrameRatingViewModel(repository, orderItemId = 10)
        vm.submitRating(3, null)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is FrameRatingUiState.Error)
    }

    @Test
    fun `reset returns to idle`() = runTest {
        coEvery { repository.rateItem(10, 5, null) } returns Result.success(
            RatingResult(id = 2, itemId = 10, rating = 5, comment = null, revisionNumber = 1)
        )
        val vm = FrameRatingViewModel(repository, orderItemId = 10)
        vm.submitRating(5, null)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is FrameRatingUiState.Success)

        vm.reset()
        assertTrue(vm.uiState.value is FrameRatingUiState.Idle)
    }
}
