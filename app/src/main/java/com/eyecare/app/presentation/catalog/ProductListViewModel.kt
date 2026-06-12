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
    data class Success(val products: List<Product>) : ProductListUiState
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

    init { load() }

    fun refresh() { load() }

    fun selectCategory(category: String) {
        selectedCategory = category
        applyFilters()
    }

    fun search(query: String) {
        searchQuery = query
        applyFilters()
    }

    private fun load() {
        _uiState.value = ProductListUiState.Loading
        viewModelScope.launch {
            repository.getProducts().fold(
                onSuccess = { list ->
                    allProducts = list
                    applyFilters()
                },
                onFailure = {
                    _uiState.value = ProductListUiState.Error(it.message ?: "Failed to load")
                },
            )
        }
    }

    private fun applyFilters() {
        val filtered = allProducts
            .filter { selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true) }
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.brand.contains(searchQuery, ignoreCase = true) }
        _uiState.value = ProductListUiState.Success(filtered)
    }
}
