package com.eyecare.app.presentation.common

import com.eyecare.app.domain.model.ProductReview

data class ProductReviewsUiState(
    val reviews: List<ProductReview> = emptyList(),
    val total: Int = 0,
    val currentPage: Int = 0,
    val lastPage: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasMorePages: Boolean
        get() = currentPage in 1 until lastPage
}
