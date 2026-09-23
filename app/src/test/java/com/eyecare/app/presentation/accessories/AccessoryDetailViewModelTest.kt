package com.eyecare.app.presentation.accessories

import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryVariant
import com.eyecare.app.domain.model.ProductReview
import com.eyecare.app.domain.repository.AccessoryRepository
import com.eyecare.app.domain.repository.PaginatedResult
import androidx.lifecycle.SavedStateHandle
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AccessoryDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccessoryRepository

    private fun accessory(
        id: Int = 1,
        variants: List<AccessoryVariant> = listOf(variant(10)),
    ) = Accessory(
        id = id,
        name = "Test Accessory",
        slug = "test-accessory",
        description = "A test accessory",
        brand = "TestBrand",
        category = "TestCategory",
        images = listOf("test.jpg"),
        averageRating = 4.5,
        ratingCount = 10,
        variants = variants,
    )

    private fun variant(
        id: Int,
        availability: AccessoryAvailability = AccessoryAvailability.AVAILABLE,
        price: BigDecimal = BigDecimal("100.00"),
    ) = AccessoryVariant(
        id = id,
        name = "Variant $id",
        price = price,
        compareAtPrice = null,
        attributes = mapOf("size" to "Medium"),
        images = emptyList(),
        availability = availability,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.getAccessoryReviews(any(), any(), any()) } returns Result.success(
            PaginatedResult(emptyList(), currentPage = 1, lastPage = 1, total = 0),
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(accessoryId: Int = 1) = AccessoryDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("accessoryId" to accessoryId)),
    )

    @Test
    fun `initial load shows loading then success`() = runTest {
        coEvery { repository.getAccessory(1) } returns Result.success(accessory())
        val viewModel = createViewModel(1)

        assertTrue(viewModel.uiState.value is AccessoryDetailUiState.Loading)
        advanceUntilIdle()
        val state = viewModel.uiState.value as AccessoryDetailUiState.Success
        assertEquals(1, state.accessory.id)
        assertEquals(10, state.selectedVariantId)
    }

    @Test
    fun `initial load includes consented public reviews`() = runTest {
        coEvery { repository.getAccessory(1) } returns Result.success(accessory())
        coEvery { repository.getAccessoryReviews(1, 1, 15) } returns Result.success(
            PaginatedResult(
                data = listOf(ProductReview(5, "Works well.", "2026-09-20T10:00:00+08:00")),
                currentPage = 1,
                lastPage = 1,
                total = 1,
            ),
        )

        val viewModel = createViewModel(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryDetailUiState.Success
        assertEquals("Works well.", state.reviews.reviews.single().comment)
        assertEquals(1, state.reviews.total)
    }

    @Test
    fun `loading more appends the next review page`() = runTest {
        coEvery { repository.getAccessory(1) } returns Result.success(accessory())
        coEvery { repository.getAccessoryReviews(1, 1, 15) } returns Result.success(
            PaginatedResult(
                data = listOf(ProductReview(5, "First review.", "2026-09-20T10:00:00+08:00")),
                currentPage = 1,
                lastPage = 2,
                total = 2,
            ),
        )
        coEvery { repository.getAccessoryReviews(1, 2, 15) } returns Result.success(
            PaginatedResult(
                data = listOf(ProductReview(4, "Second review.", "2026-09-19T10:00:00+08:00")),
                currentPage = 2,
                lastPage = 2,
                total = 2,
            ),
        )

        val viewModel = createViewModel(1)
        advanceUntilIdle()
        viewModel.loadMoreReviews()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryDetailUiState.Success
        assertEquals(listOf("First review.", "Second review."), state.reviews.reviews.map { it.comment })
        assertEquals(2, state.reviews.currentPage)
    }

    @Test
    fun `initial load shows error on failure`() = runTest {
        coEvery { repository.getAccessory(1) } returns Result.failure(RuntimeException("fail"))
        val viewModel = createViewModel(1)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AccessoryDetailUiState.Error)
    }

    @Test
    fun `404 shows not found state`() = runTest {
        coEvery { repository.getAccessory(999) } returns Result.failure(
            com.eyecare.app.domain.model.ApiDomainError(404, "NOT_FOUND", "Not found"),
        )
        val viewModel = createViewModel(999)

        advanceUntilIdle()
        val state = viewModel.uiState.value as AccessoryDetailUiState.Error
        assertTrue(state.message.contains("not found", ignoreCase = true) || state.isNotFound)
    }

    @Test
    fun `selecting variant updates selectedVariantId`() = runTest {
        val variants = listOf(variant(10), variant(20, price = BigDecimal("200.00")))
        coEvery { repository.getAccessory(1) } returns Result.success(accessory(variants = variants))
        val viewModel = createViewModel(1)
        advanceUntilIdle()

        viewModel.selectVariant(20)
        val state = viewModel.uiState.value as AccessoryDetailUiState.Success
        assertEquals(20, state.selectedVariantId)
    }

    @Test
    fun `unavailable variant is not orderable`() = runTest {
        val variants = listOf(variant(10, availability = AccessoryAvailability.UNAVAILABLE))
        coEvery { repository.getAccessory(1) } returns Result.success(accessory(variants = variants))
        val viewModel = createViewModel(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryDetailUiState.Success
        assertTrue(!state.canAddToCart)
    }

    @Test
    fun `unknown availability is not orderable`() = runTest {
        val variants = listOf(variant(10, availability = AccessoryAvailability.UNKNOWN))
        coEvery { repository.getAccessory(1) } returns Result.success(accessory(variants = variants))
        val viewModel = createViewModel(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryDetailUiState.Success
        assertTrue(!state.canAddToCart)
    }

    @Test
    fun `available variant is orderable`() = runTest {
        val variants = listOf(variant(10, availability = AccessoryAvailability.AVAILABLE))
        coEvery { repository.getAccessory(1) } returns Result.success(accessory(variants = variants))
        val viewModel = createViewModel(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value as AccessoryDetailUiState.Success
        assertTrue(state.canAddToCart)
    }

    @Test
    fun `retry reloads after error`() = runTest {
        coEvery { repository.getAccessory(1) } returns Result.failure(RuntimeException("fail"))
        val viewModel = createViewModel(1)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AccessoryDetailUiState.Error)

        coEvery { repository.getAccessory(1) } returns Result.success(accessory())
        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AccessoryDetailUiState.Success)
    }

    @Test
    fun `selected variant determines price display`() = runTest {
        val variants = listOf(
            variant(10, price = BigDecimal("100.00")),
            variant(20, price = BigDecimal("250.00")),
        )
        coEvery { repository.getAccessory(1) } returns Result.success(accessory(variants = variants))
        val viewModel = createViewModel(1)
        advanceUntilIdle()

        viewModel.selectVariant(20)
        val state = viewModel.uiState.value as AccessoryDetailUiState.Success
        assertEquals(BigDecimal("250.00"), state.displayPrice)
    }
}
