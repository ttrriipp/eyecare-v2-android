package com.eyecare.app.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Brand
import com.eyecare.app.domain.model.Category
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val apiValue: String, val label: String) {
    NAME("name", "Name"),
    NEWEST("newest", "Newest"),
    PRICE_ASC("price_asc", "Price ↑"),
    PRICE_DESC("price_desc", "Price ↓"),
}

data class ProductFilters(
    val search: String = "",
    val brandId: Int? = null,
    val categoryId: Int? = null,
    val sort: SortOption = SortOption.NAME,
    val inStockOnly: Boolean = false,
)

sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Success(
        val products: List<Product>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val filters: ProductFilters = ProductFilters(),
        val brands: List<Brand> = emptyList(),
        val categories: List<Category> = emptyList(),
    ) : ProductListUiState
    data class Error(val message: String) : ProductListUiState
}

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repository: ProductRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var currentProducts = mutableListOf<Product>()
    private var filters = ProductFilters()
    private var searchJob: Job? = null

    init {
        loadFilters()
        load()
    }

    fun refresh() {
        currentPage = 1
        currentProducts.clear()
        load()
    }

    fun search(query: String) {
        filters = filters.copy(search = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            resetAndLoad()
        }
    }

    fun selectBrand(brandId: Int?) {
        filters = filters.copy(brandId = brandId)
        resetAndLoad()
    }

    fun selectCategory(categoryId: Int?) {
        filters = filters.copy(categoryId = categoryId)
        resetAndLoad()
    }

    fun selectSort(sort: SortOption) {
        filters = filters.copy(sort = sort)
        resetAndLoad()
    }

    fun toggleInStock(enabled: Boolean) {
        filters = filters.copy(inStockOnly = enabled)
        resetAndLoad()
    }

    fun clearFilters() {
        filters = ProductFilters()
        resetAndLoad()
    }

    fun loadMore() {
        val current = _uiState.value as? ProductListUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMorePages) return
        _uiState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            currentPage++
            repository.getProducts(
                page = currentPage,
                search = filters.search.takeIf { it.isNotBlank() },
                brandId = filters.brandId,
                categoryId = filters.categoryId,
                sort = filters.sort.apiValue,
                inStock = if (filters.inStockOnly) true else null,
            ).fold(
                onSuccess = { newProducts ->
                    currentProducts.addAll(newProducts)
                    val hasMore = repository.hasMorePages(currentPage)
                    _uiState.value = current.copy(
                        products = currentProducts.toList(),
                        isLoadingMore = false,
                        hasMorePages = hasMore,
                    )
                },
                onFailure = {
                    currentPage--
                    _uiState.value = current.copy(isLoadingMore = false)
                },
            )
        }
    }

    private fun resetAndLoad() {
        currentPage = 1
        currentProducts.clear()
        load()
    }

    private fun load() {
        val current = _uiState.value
        // Keep filter state visible during reload
        if (current !is ProductListUiState.Success) {
            _uiState.value = ProductListUiState.Loading
        }
        viewModelScope.launch {
            repository.getProducts(
                page = 1,
                search = filters.search.takeIf { it.isNotBlank() },
                brandId = filters.brandId,
                categoryId = filters.categoryId,
                sort = filters.sort.apiValue,
                inStock = if (filters.inStockOnly) true else null,
            ).fold(
                onSuccess = { products ->
                    currentProducts.clear()
                    currentProducts.addAll(products)
                    val hasMore = repository.hasMorePages(1)
                    val prev = _uiState.value as? ProductListUiState.Success
                    _uiState.value = ProductListUiState.Success(
                        products = currentProducts.toList(),
                        hasMorePages = hasMore,
                        filters = filters,
                        brands = prev?.brands ?: emptyList(),
                        categories = prev?.categories ?: emptyList(),
                    )
                },
                onFailure = {
                    _uiState.value = ProductListUiState.Error(it.message ?: "Failed to load")
                },
            )
        }
    }

    private fun loadFilters() {
        viewModelScope.launch {
            val brands = repository.getBrands().getOrDefault(emptyList())
            val categories = repository.getCategories().getOrDefault(emptyList())
            val current = _uiState.value
            if (current is ProductListUiState.Success) {
                _uiState.value = current.copy(brands = brands, categories = categories)
            }
            // If still loading, they'll be picked up when load() completes
        }
    }
}
