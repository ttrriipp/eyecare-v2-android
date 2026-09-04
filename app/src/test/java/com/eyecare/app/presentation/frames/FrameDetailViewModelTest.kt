package com.eyecare.app.presentation.frames

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.model.SavedFrame
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.domain.repository.SavedFrameRepository
import com.eyecare.app.presentation.common.components.SAVED_FRAME_DISCLAIMER
import io.mockk.coEvery
import io.mockk.coVerify
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
    private lateinit var savedFrameRepository: SavedFrameRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        savedFrameRepository = mockk()
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

        val viewModel = FrameDetailViewModel(repository, savedFrameRepository, frameId = 7, requestedVariantId = null)
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

        val viewModel = FrameDetailViewModel(repository, savedFrameRepository, frameId = 7, requestedVariantId = null)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as FrameDetailUiState.Success
        assertEquals(initial.id, state.frame.id)
        assertFalse(state.isRefreshing)
        assertEquals("Couldn't refresh frame. Please try again.", state.message)
    }

    @Test
    fun toggle_saved_updates_target_variant_without_reselecting_it_after_switch() = runTest(dispatcher) {
        val initial = frame()
        val saveResult = CompletableDeferred<Result<SavedFrame>>()
        coEvery { repository.getFrame(7) } returns Result.success(initial)
        coEvery { savedFrameRepository.save(71) } coAnswers { saveResult.await() }

        val viewModel = FrameDetailViewModel(repository, savedFrameRepository, frameId = 7, requestedVariantId = null)
        advanceUntilIdle()

        viewModel.toggleSaved()
        runCurrent()
        viewModel.selectVariant(initial.variants[1])

        saveResult.complete(Result.success(mockk()))
        advanceUntilIdle()

        val state = viewModel.uiState.value as FrameDetailUiState.Success
        assertEquals(72, state.selectedVariant.id)
        assertTrue(state.frame.variants.first { it.id == 71 }.isSaved)
        assertFalse(state.selectedVariant.isSaved)
    }

    @Test
    fun save_success_surfaces_the_saved_frame_notice_once() = runTest(dispatcher) {
        val initial = frame()
        coEvery { repository.getFrame(7) } returns Result.success(initial)
        coEvery { savedFrameRepository.save(71) } returns Result.success(mockk())

        val viewModel = FrameDetailViewModel(repository, savedFrameRepository, frameId = 7, requestedVariantId = null)
        advanceUntilIdle()
        assertEquals(null, (viewModel.uiState.value as FrameDetailUiState.Success).message)

        viewModel.toggleSaved()
        advanceUntilIdle()

        val state = viewModel.uiState.value as FrameDetailUiState.Success
        assertEquals(SAVED_FRAME_DISCLAIMER, state.message)

        viewModel.clearMessage()
        assertEquals(null, (viewModel.uiState.value as FrameDetailUiState.Success).message)
    }

    @Test
    fun save_network_failure_keeps_state_and_uses_generic_copy() = runTest(dispatcher) {
        val initial = frame()
        coEvery { repository.getFrame(7) } returns Result.success(initial)
        coEvery { savedFrameRepository.save(71) } returns Result.failure(IllegalStateException("offline"))

        val viewModel = FrameDetailViewModel(repository, savedFrameRepository, frameId = 7, requestedVariantId = null)
        advanceUntilIdle()

        viewModel.toggleSaved()
        advanceUntilIdle()

        val state = viewModel.uiState.value as FrameDetailUiState.Success
        assertFalse(state.selectedVariant.isSaved)
        assertEquals("Couldn't save this frame. Try again.", state.saveError)
    }

    @Test
    fun save_422_failure_explains_that_the_option_is_unavailable() = runTest(dispatcher) {
        val initial = frame()
        coEvery { repository.getFrame(7) } returns Result.success(initial)
        coEvery { savedFrameRepository.save(71) } returns Result.failure(
            ApiDomainError(422, "VARIANT_UNAVAILABLE", "Unavailable"),
        )

        val viewModel = FrameDetailViewModel(repository, savedFrameRepository, frameId = 7, requestedVariantId = null)
        advanceUntilIdle()

        viewModel.toggleSaved()
        advanceUntilIdle()

        val state = viewModel.uiState.value as FrameDetailUiState.Success
        assertEquals("This option can no longer be saved. Try refreshing.", state.saveError)
    }

    @Test
    fun refresh_does_not_clear_an_in_flight_save_or_allow_a_duplicate_tap() = runTest(dispatcher) {
        val initial = frame()
        val refreshResult = CompletableDeferred<Result<Frame>>()
        val saveResult = CompletableDeferred<Result<SavedFrame>>()
        var loadCount = 0
        coEvery { repository.getFrame(7) } coAnswers {
            if (loadCount++ == 0) Result.success(initial) else refreshResult.await()
        }
        coEvery { savedFrameRepository.save(71) } coAnswers { saveResult.await() }

        val viewModel = FrameDetailViewModel(repository, savedFrameRepository, frameId = 7, requestedVariantId = null)
        advanceUntilIdle()
        viewModel.toggleSaved()
        runCurrent()

        viewModel.refresh()
        runCurrent()
        refreshResult.complete(Result.success(initial))
        advanceUntilIdle()
        viewModel.toggleSaved()

        coVerify(exactly = 1) { savedFrameRepository.save(71) }
        saveResult.complete(Result.success(mockk()))
        advanceUntilIdle()
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
