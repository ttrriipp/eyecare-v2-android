package com.eyecare.app.presentation.frames

import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.repository.FrameRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FrameDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FrameRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refresh_exposes_progress_and_preserves_selected_variant() = runTest(dispatcher) {
        val initial = frame()
        val refreshResult = CompletableDeferred<Result<Frame>>()
        var callCount = 0
        coEvery { repository.getFrame(7) } coAnswers {
            if (callCount++ == 0) Result.success(initial) else refreshResult.await()
        }

        val viewModel = FrameDetailViewModel(repository, frameId = 7)
        advanceUntilIdle()
        viewModel.selectVariant(initial.variants[1])

        viewModel.refresh()
        runCurrent()

        val refreshing = viewModel.uiState.value as FrameDetailUiState.Success
        assertTrue(refreshing.isRefreshing)
        assertEquals(initial.variants[1].id, refreshing.selectedVariant.id)

        refreshResult.complete(Result.success(initial))
        advanceUntilIdle()

        val settled = viewModel.uiState.value as FrameDetailUiState.Success
        assertFalse(settled.isRefreshing)
        assertEquals(initial.variants[1].id, settled.selectedVariant.id)
    }

    @Test
    fun refresh_failure_preserves_content_and_surfaces_recovery_message() = runTest(dispatcher) {
        val initial = frame()
        coEvery { repository.getFrame(7) } returnsMany listOf(
            Result.success(initial),
            Result.failure(IllegalStateException("offline")),
        )

        val viewModel = FrameDetailViewModel(repository, frameId = 7)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as FrameDetailUiState.Success
        assertEquals(initial.id, state.frame.id)
        assertFalse(state.isRefreshing)
        assertEquals("Couldn't refresh frame. Please try again.", state.message)
    }

    private fun frame() = Frame(
        id = 7,
        name = "Classic Rectangle",
        slug = "classic-rectangle",
        description = "Timeless frame design",
        brand = "Eyecare",
        category = "Full Rim",
        variants = listOf(
            variant(id = 71, name = "Black", arReady = true),
            variant(id = 72, name = "Tortoise", arReady = false),
        ),
        images = listOf("frames/classic-rectangle.jpg"),
    )

    private fun variant(id: Int, name: String, arReady: Boolean) = FrameVariant(
        id = id,
        name = name,
        sku = "SKU-$id",
        price = BigDecimal("4500.00"),
        compareAtPrice = null,
        attributes = null,
        arEligible = arReady,
        arAssetReference = if (arReady) "frame-$id.glb" else null,
        images = emptyList(),
    )
}
