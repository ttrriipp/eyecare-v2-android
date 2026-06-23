package com.eyecare.app.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Success(
        val products: List<Product>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
    ) : ProductListUiState
    data class Error(val message: String) : ProductListUiState
}

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repository: ProductRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private var allProducts: List<Product> = emptyList()
    private var selectedCategory: String = "All"
    private var searchQuery: String = ""
    private var currentPage = 1

    init { load() }

    fun refresh() {
        currentPage = 1
        allProducts = emptyList()
        load()
    }

    fun loadMore() {
        val current = _uiState.value as? ProductListUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMorePages) return
        _uiState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            currentPage++
            repository.getProducts(page = currentPage).fold(
                onSuccess = { newProducts ->
                    allProducts = allProducts + newProducts
                    val hasMore = repository.hasMorePages(currentPage)
                    _uiState.value = ProductListUiState.Success(
                        products = applyFilters(allProducts),
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

    fun selectCategory(category: String) {
        selectedCategory = category
        publishFiltered()
    }

    fun search(query: String) {
        searchQuery = query
        publishFiltered()
    }

    private fun load() {
        _uiState.value = ProductListUiState.Loading
        viewModelScope.launch {
            repository.getProducts(page = 1).fold(
                onSuccess = { list ->
                    allProducts = list
                    val hasMore = repository.hasMorePages(1)
                    _uiState.value = ProductListUiState.Success(
                        products = applyFilters(allProducts),
                        hasMorePages = hasMore,
                    )
                },
                onFailure = {
                    _uiState.value = ProductListUiState.Error(it.message ?: "Failed to load")
                },
            )
        }
    }

    private fun publishFiltered() {
        val current = _uiState.value as? ProductListUiState.Success ?: return
        _uiState.value = current.copy(products = applyFilters(allProducts))
    }

    private fun applyFilters(list: List<Product>) = list
        .filter { selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true) }
        .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.brand.contains(searchQuery, ignoreCase = true) }
}
