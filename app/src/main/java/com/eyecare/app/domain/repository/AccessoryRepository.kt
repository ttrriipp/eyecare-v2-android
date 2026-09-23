package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryQuery
import com.eyecare.app.domain.model.ProductReview

interface AccessoryRepository {
    suspend fun getAccessories(query: AccessoryQuery = AccessoryQuery()): Result<PaginatedResult<Accessory>>
    suspend fun getAccessory(id: Int): Result<Accessory>
    suspend fun getAccessoryReviews(
        id: Int,
        page: Int = 1,
        perPage: Int = 15,
    ): Result<PaginatedResult<ProductReview>>
}
