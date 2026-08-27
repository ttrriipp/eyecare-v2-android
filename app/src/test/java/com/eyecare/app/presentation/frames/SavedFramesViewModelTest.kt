package com.eyecare.app.presentation.frames

import com.eyecare.app.domain.model.ArAsset
import com.eyecare.app.domain.model.ArAssetFile
import com.eyecare.app.domain.model.ArAssetFormat
import com.eyecare.app.domain.model.ArAssetStatus
import com.eyecare.app.domain.model.ArCalibration
import com.eyecare.app.domain.model.ArVector
import com.eyecare.app.domain.model.SavedFrame
import com.eyecare.app.domain.model.SavedFrameAvailability
import com.eyecare.app.domain.model.SavedFramePage
import com.eyecare.app.domain.model.SavedFrameProduct
import com.eyecare.app.domain.model.SavedFrameVariant
import com.eyecare.app.domain.repository.SavedFrameRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SavedFramesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: SavedFrameRepository
    private lateinit var viewModel: SavedFramesViewModel

    private fun savedFrame(
        variantId: Int,
        availability: SavedFrameAvailability = SavedFrameAvailability.AVAILABLE,
    ) = SavedFrame(
        productVariantId = variantId,
        savedAt = "2026-08-27T10:00:00+08:00",
        availability = availability,
        variant = SavedFrameVariant(
            id = variantId,
            name = "Variant $variantId",
            sku = "SKU-$variantId",
            price = BigDecimal("4500.00"),
            compareAtPrice = null,
            attributes = null,
            images = emptyList(),
            ar = null,
            product = SavedFrameProduct(id = 1, name = "Frame", brand = "Brand", category = "Cat"),
        ),
    )

    private fun savedFramePage(
        items: List<SavedFrame>,
        currentPage: Int = 1,
        lastPage: Int = 1,
    ) = SavedFramePage(items = items, currentPage = currentPage, lastPage = lastPage)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load shows success with items`() = runTest {
        val items = listOf(savedFrame(1), savedFrame(2))
        coEvery { repository.getSavedFrames(1) } returns Result.success(savedFramePage(items))

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SavedFramesUiState.Success
        assertEquals(2, state.items.size)
        assertEquals(1, state.currentPage)
        assertFalse(state.canLoadMore)
    }

    @Test
    fun `initial load shows empty success when no items`() = runTest {
        coEvery { repository.getSavedFrames(1) } returns Result.success(savedFramePage(emptyList()))

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SavedFramesUiState.Success
        assertEquals(0, state.items.size)
    }

    @Test
    fun `initial load shows error on failure`() = runTest {
        coEvery { repository.getSavedFrames(1) } returns Result.failure(RuntimeException("Network"))

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SavedFramesUiState.Error
        assertTrue(state.patientSafeMessage.contains("couldn't load"))
    }

    @Test
    fun `refresh replaces data on success`() = runTest {
        val initial = listOf(savedFrame(1))
        val refreshed = listOf(savedFrame(1), savedFrame(3))
        coEvery { repository.getSavedFrames(1) } returnsMany listOf(
            Result.success(savedFramePage(initial)),
            Result.success(savedFramePage(refreshed)),
        )

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SavedFramesUiState.Success
        assertEquals(2, state.items.size)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `refresh keeps data on failure`() = runTest {
        val initial = listOf(savedFrame(1))
        coEvery { repository.getSavedFrames(1) } returnsMany listOf(
            Result.success(savedFramePage(initial)),
            Result.failure(RuntimeException("Network")),
        )

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SavedFramesUiState.Success
        assertEquals(1, state.items.size)
        assertTrue(state.inlineError != null)
    }

    @Test
    fun `loadMore deduplicates by productVariantId`() = runTest {
        val page1 = savedFramePage(listOf(savedFrame(1), savedFrame(2)), currentPage = 1, lastPage = 2)
        val page2 = savedFramePage(listOf(savedFrame(2), savedFrame(3)), currentPage = 2, lastPage = 2)
        coEvery { repository.getSavedFrames(1) } returns Result.success(page1)
        coEvery { repository.getSavedFrames(2) } returns Result.success(page2)

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SavedFramesUiState.Success
        assertEquals(3, state.items.size)
        assertEquals(setOf(1, 2, 3), state.items.map { it.productVariantId }.toSet())
    }

    @Test
    fun `removeSavedFrame removes item on success`() = runTest {
        val items = listOf(savedFrame(1), savedFrame(2))
        coEvery { repository.getSavedFrames(1) } returns Result.success(savedFramePage(items))
        coEvery { repository.remove(1) } returns Result.success(Unit)

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.removeSavedFrame(1)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SavedFramesUiState.Success
        assertEquals(1, state.items.size)
        assertEquals(2, state.items[0].productVariantId)
        assertTrue(state.removingVariantIds.isEmpty())
    }

    @Test
    fun `removeSavedFrame keeps item on failure`() = runTest {
        val items = listOf(savedFrame(1), savedFrame(2))
        coEvery { repository.getSavedFrames(1) } returns Result.success(savedFramePage(items))
        coEvery { repository.remove(1) } returns Result.failure(RuntimeException("Network"))

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.removeSavedFrame(1)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SavedFramesUiState.Success
        assertEquals(2, state.items.size)
        assertTrue(state.inlineError != null)
    }

    @Test
    fun `removeSavedFrame ignores duplicate tap while in flight`() = runTest {
        val items = listOf(savedFrame(1))
        coEvery { repository.getSavedFrames(1) } returns Result.success(savedFramePage(items))
        coEvery { repository.remove(1) } returns Result.success(Unit)

        viewModel = SavedFramesViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.removeSavedFrame(1)
        viewModel.removeSavedFrame(1) // duplicate tap

        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.remove(1) }
    }
}
