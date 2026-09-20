package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AccessoryApiService
import com.eyecare.app.data.remote.api.getAccessories
import com.eyecare.app.data.remote.dto.AccessoryDtos
import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryQuery
import com.eyecare.app.domain.model.AccessoryVariant
import com.eyecare.app.domain.repository.AccessoryRepository
import com.eyecare.app.domain.repository.PaginatedResult
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class AccessoryRepositoryImpl @Inject constructor(
    private val api: AccessoryApiService,
) : AccessoryRepository {

    override suspend fun getAccessories(query: AccessoryQuery): Result<PaginatedResult<Accessory>> = safeApiCall {
        val response = api.getAccessories(query)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getAccessory(id: Int): Result<Accessory> = safeApiCall {
        api.getAccessory(id).data.toDomain()
    }

    private fun AccessoryDtos.AccessoryDto.toDomain() = Accessory(
        id = id,
        name = name,
        slug = slug,
        description = description,
        brand = brand,
        category = category,
        images = images,
        averageRating = averageRating,
        ratingCount = ratingCount,
        variants = variants.map { it.toDomain() },
    )

    private fun AccessoryDtos.AccessoryVariantDto.toDomain() = AccessoryVariant(
        id = id,
        name = name,
        price = price,
        compareAtPrice = compareAtPrice,
        attributes = attributes.entries.associate { (key, value) ->
            key to when (value) {
                is JsonPrimitive -> value.content
                else -> value.toString()
            }
        },
        images = images,
        availability = AccessoryAvailability.from(availability),
    )
}