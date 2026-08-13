package com.eyecare.app.presentation.frames

import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.repository.FrameRepository
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
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class FrameListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FrameRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        coEvery { repository.getFrames(1, null, null, null, "name") } returns
            Result.success(listOf(frame(1, "Acme", "Full Rim", arReady = true), frame(2, "Vista", "Round", arReady = false)))
        coEvery { repository.hasMorePages(1) } returns false
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `catalog filters narrow visible frames`() = runTest {
        val viewModel = FrameListViewModel(repository)

        viewModel.selectBrand("Acme")
        viewModel.selectCategory("Full Rim")
        viewModel.setArOnly(true)

        val state = viewModel.uiState.value as FrameListUiState.Success
        assertEquals(listOf(1), state.visibleFrames.map { it.id })
    }

    @Test
    fun `clearing catalog filters restores all loaded frames`() = runTest {
        val viewModel = FrameListViewModel(repository)
        viewModel.selectBrand("Acme")

        viewModel.clearCatalogFilters()

        val state = viewModel.uiState.value as FrameListUiState.Success
        assertEquals(listOf(1, 2), state.visibleFrames.map { it.id })
    }

    @Test
    fun `load more failure exposes a recovery message`() = runTest {
        coEvery { repository.hasMorePages(1) } returns true
        coEvery { repository.getFrames(2, null, null, null, "name") } returns
            Result.failure(IllegalStateException("offline"))

        val viewModel = FrameListViewModel(repository)
        viewModel.loadMore()

        val state = viewModel.uiState.value as FrameListUiState.Success
        assertEquals("Couldn't load more frames. Please try again.", state.message)
        assertTrue(!state.isLoadingMore)
    }

    private fun frame(
        id: Int,
        brand: String,
        category: String,
        arReady: Boolean,
    ) = Frame(
        id = id,
        name = "Frame $id",
        slug = "frame-$id",
        description = null,
        brand = brand,
        category = category,
        variants = listOf(
            FrameVariant(
                id = id,
                name = "Standard",
                sku = "SKU-$id",
                price = BigDecimal("4500.00"),
                compareAtPrice = null,
                attributes = null,
                arEligible = arReady,
                arAssetReference = if (arReady) "frame-$id.glb" else null,
                images = emptyList(),
            ),
        ),
        images = emptyList(),
    )
}
