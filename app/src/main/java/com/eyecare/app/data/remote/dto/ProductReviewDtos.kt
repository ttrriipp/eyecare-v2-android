package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ProductReviewDtos {

    @Serializable
    data class ProductReviewDto(
        val rating: Int,
        val comment: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("attachment_url") val attachmentUrl: String? = null,
    )

    @Serializable
    data class ProductReviewListResponse(
        val data: List<ProductReviewDto>,
        val links: PaginationLinks? = null,
        val meta: PaginationMeta? = null,
    )
}
