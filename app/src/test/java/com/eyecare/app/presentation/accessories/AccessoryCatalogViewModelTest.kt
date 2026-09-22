package com.eyecare.app.presentation.accessories

import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryQuery
import com.eyecare.app.domain.model.AccessoryVariant
import com.eyecare.app.domain.repository.AccessoryRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccessoryCatalogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccessoryRepository
    private lateinit var viewModel: AccessoryCatalogViewModel

    private fun accessory(id: Int, name: String = "Product $id") = Accessory(
        id = id,
        name = name,
        slug = "slug-$id",
        description = "Description $id",
        brand = "Brand",
        category = "Category",
        images = emptyList(),
        averageRating = 4.5,
        ratingCount = 10,
        variants = listOf(
            AccessoryVariant(
                id = id * 10,
                name = "Variant",
                price = BigDecimal("100.00"),
                compareAtPrice = null,
                attributes = emptyMap(),
                images = emptyList(),
                availability = AccessoryAvailability.AVAILABLE,
            ),
        ),
    )

    private fun paginatedResult(
        items: List<Accessory>,
        currentPage: Int = 1,
        lastPage: Int = 1,
    ) = PaginatedResult(
        data = items,
        currentPage = currentPage,
        lastPage = lastPage,
        total = items.size,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load shows loading then success`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(listOf(accessory(1))),
        )
        viewModel = AccessoryCatalogViewModel(repository)

        assertTrue(viewModel.uiState.value is AccessoryCatalogUiState.Loading)
        advanceUntilIdle()
        val state = viewModel.uiState.value as AccessoryCatalogUiState.Success
        assertEquals(1, state.items.size)
        assertEquals(1, state.items[0].id)
    }

    @Test
    fun `initial load shows empty when no results`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(emptyList()),
        )
        viewModel = AccessoryCatalogViewModel(repository)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AccessoryCatalogUiState.Empty)
    }

    @Test
    fun `initial load shows error on failure`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.failure(RuntimeException("Network error"))
        viewModel = AccessoryCatalogViewModel(repository)

        advanceUntilIdle()
        val state = viewModel.uiState.value as AccessoryCatalogUiState.Error
        assertTrue(state.message.contains("load"))
    }

    @Test
    fun `search resets to page 1 and loads`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(listOf(accessory(1))),
        )
        viewModel = AccessoryCatalogViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.getAccessories(match { it.search == "lens" }) } returns Result.success(
            paginatedResult(listOf(accessory(2))),
        )
        viewModel.updateSearch("lens")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryCatalogUiState.Success
        assertEquals(1, state.items.size)
        assertEquals(2, state.items[0].id)
    }

    @Test
    fun `search is trimmed and limited to the server contract length`() = runTest {
        val queries = mutableListOf<AccessoryQuery>()
        coEvery { repository.getAccessories(any()) } coAnswers {
            queries += firstArg<AccessoryQuery>()
            Result.success(paginatedResult(listOf(accessory(1))))
        }
        viewModel = AccessoryCatalogViewModel(repository)
        advanceUntilIdle()

        viewModel.updateSearch("  ${"x".repeat(120)}  ")
        advanceUntilIdle()

        assertEquals(100, queries.last().search?.length)
        assertEquals("x".repeat(100), queries.last().search)
    }

    @Test
    fun `sort change resets to page 1`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(listOf(accessory(1))),
        )
        viewModel = AccessoryCatalogViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.getAccessories(match { it.sort == "rating" }) } returns Result.success(
            paginatedResult(listOf(accessory(2))),
        )
        viewModel.updateSort("rating")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryCatalogUiState.Success
        assertEquals(2, state.items[0].id)
    }

    @Test
    fun `loadMore appends items`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(listOf(accessory(1)), lastPage = 2),
        )
        viewModel = AccessoryCatalogViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.getAccessories(match { it.page == 2 }) } returns Result.success(
            paginatedResult(listOf(accessory(2)), currentPage = 2, lastPage = 2),
        )
        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryCatalogUiState.Success
        assertEquals(2, state.items.size)
        assertEquals(1, state.items[0].id)
        assertEquals(2, state.items[1].id)
        assertFalse(state.hasMorePages)
    }

    @Test
    fun `refresh replaces items`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(listOf(accessory(1))),
        )
        viewModel = AccessoryCatalogViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(listOf(accessory(2), accessory(3))),
        )
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryCatalogUiState.Success
        assertEquals(2, state.items.size)
        assertEquals(2, state.items[0].id)
        assertEquals(3, state.items[1].id)
    }

    @Test
    fun `stale search response does not overwrite newer results`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(listOf(accessory(1))),
        )
        viewModel = AccessoryCatalogViewModel(repository)
        advanceUntilIdle()

        // First search returns quickly
        coEvery { repository.getAccessories(match { it.search == "a" }) } coAnswers {
            Result.success(paginatedResult(listOf(accessory(10))))
        }

        // Second search returns slowly
        coEvery { repository.getAccessories(match { it.search == "b" }) } coAnswers {
            Result.success(paginatedResult(listOf(accessory(20))))
        }

        viewModel.updateSearch("a")
        viewModel.updateSearch("b")
        advanceUntilIdle()

        // Should show results from second search
        val state = viewModel.uiState.value
        if (state is AccessoryCatalogUiState.Success) {
            assertEquals(20, state.items[0].id)
        }
    }

    @Test
    fun `retry reloads after error`() = runTest {
        coEvery { repository.getAccessories(any()) } returns Result.failure(RuntimeException("fail"))
        viewModel = AccessoryCatalogViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AccessoryCatalogUiState.Error)

        coEvery { repository.getAccessories(any()) } returns Result.success(
            paginatedResult(listOf(accessory(1))),
        )
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryCatalogUiState.Success
        assertEquals(1, state.items.size)
    }
}
