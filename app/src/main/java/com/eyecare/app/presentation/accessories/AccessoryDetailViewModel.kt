package com.eyecare.app.presentation.accessories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryVariant
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.repository.AccessoryRepository
import com.eyecare.app.presentation.common.ProductReviewsUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

sealed interface AccessoryDetailUiState {
    data object Loading : AccessoryDetailUiState
    data class Success(
        val accessory: Accessory,
        val selectedVariantId: Int,
        val reviews: ProductReviewsUiState = ProductReviewsUiState(isLoading = true),
    ) : AccessoryDetailUiState {
        val selectedVariant: AccessoryVariant?
            get() = accessory.variants.find { it.id == selectedVariantId }

        val displayPrice: BigDecimal
            get() = selectedVariant?.price ?: accessory.variants.firstOrNull()?.price ?: BigDecimal.ZERO

        val canAddToCart: Boolean
            get() = selectedVariant?.availability?.isOrderable == true
    }
    data class Error(
        val message: String,
        val isNotFound: Boolean = false,
    ) : AccessoryDetailUiState
}

@HiltViewModel
class AccessoryDetailViewModel @Inject constructor(
    private val repository: AccessoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val accessoryId: Int = savedStateHandle.get<Int>("accessoryId") ?: 0

    private val _uiState = MutableStateFlow<AccessoryDetailUiState>(AccessoryDetailUiState.Loading)
    val uiState: StateFlow<AccessoryDetailUiState> = _uiState.asStateFlow()
    private var reviewsJob: Job? = null

    init { load() }

    fun selectVariant(variantId: Int) {
        val current = _uiState.value
        if (current is AccessoryDetailUiState.Success) {
            _uiState.value = current.copy(selectedVariantId = variantId)
        }
    }

    fun retry() {
        load()
    }

    fun retryReviews() {
        val current = _uiState.value as? AccessoryDetailUiState.Success ?: return
        val page = (current.reviews.currentPage + 1).coerceAtLeast(1)
        loadReviews(page = page, append = page > 1)
    }

    fun loadMoreReviews() {
        val current = _uiState.value as? AccessoryDetailUiState.Success ?: return
        if (!current.reviews.hasMorePages) return
        loadReviews(page = current.reviews.currentPage + 1, append = true)
    }

    private fun load() {
        reviewsJob?.cancel()
        _uiState.value = AccessoryDetailUiState.Loading
        viewModelScope.launch {
            repository.getAccessory(accessoryId).fold(
                onSuccess = { accessory ->
                    val defaultVariant = accessory.variants.firstOrNull()?.id ?: 0
                    _uiState.value = AccessoryDetailUiState.Success(
                        accessory = accessory,
                        selectedVariantId = defaultVariant,
                        reviews = ProductReviewsUiState(isLoading = true),
                    )
                    loadReviews(page = 1, append = false)
                },
                onFailure = { error ->
                    val isNotFound = error is ApiDomainError && error.httpStatus == 404
                    _uiState.value = AccessoryDetailUiState.Error(
                        message = if (isNotFound) {
                            "This accessory could not be found."
                        } else {
                            "We couldn't load this accessory. Check your connection and try again."
                        },
                        isNotFound = isNotFound,
                    )
                },
            )
        }
    }

    private fun loadReviews(page: Int, append: Boolean) {
        val current = _uiState.value as? AccessoryDetailUiState.Success ?: return
        if (reviewsJob?.isActive == true || (append && !current.reviews.hasMorePages)) return

        _uiState.value = current.copy(
            reviews = current.reviews.copy(
                isLoading = !append,
                isLoadingMore = append,
                errorMessage = null,
            ),
        )
        reviewsJob = viewModelScope.launch {
            val result = repository.getAccessoryReviews(accessoryId, page = page, perPage = 15)
            if (!isActive) return@launch
            result.fold(
                onSuccess = { result ->
                    val latest = _uiState.value as? AccessoryDetailUiState.Success ?: return@launch
                    val previousReviews = latest.reviews
                    latest.copy(
                        reviews = previousReviews.copy(
                            reviews = if (append) {
                                previousReviews.reviews + result.data
                            } else {
                                result.data
                            },
                            total = result.total,
                            currentPage = result.currentPage,
                            lastPage = result.lastPage,
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = null,
                        ),
                    ).also { _uiState.value = it }
                },
                onFailure = {
                    val latest = _uiState.value as? AccessoryDetailUiState.Success ?: return@launch
                    _uiState.value = latest.copy(
                        reviews = latest.reviews.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = "Couldn't load reviews. Please try again.",
                        ),
                    )
                },
            )
        }
    }
}
