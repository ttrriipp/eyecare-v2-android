package com.eyecare.app.data.remote.dto

import kotlinx.serialization.Serializable

object ProductFilterDtos {

    @Serializable
    data class BrandDto(val id: Int, val name: String)

    @Serializable
    data class CategoryDto(val id: Int, val name: String)

    @Serializable
    data class BrandListResponse(val data: List<BrandDto>)

    @Serializable
    data class CategoryListResponse(val data: List<CategoryDto>)
}
