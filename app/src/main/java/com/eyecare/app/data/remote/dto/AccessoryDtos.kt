package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import java.math.BigDecimal

object AccessoryDtos {

    @Serializable
    data class AccessoryVariantDto(
        val id: Int,
        val name: String,
        @Serializable(with = MoneyValueSerializer::class)
        val price: BigDecimal,
        @SerialName("compare_at_price")
        @Serializable(with = MoneyValueSerializer::class)
        val compareAtPrice: BigDecimal? = null,
        @Serializable(with = EmptyArrayAsJsonObjectSerializer::class)
        val attributes: JsonObject = JsonObject(emptyMap()),
        val images: List<String> = emptyList(),
        val availability: String = "unavailable",
    )

    @Serializable
    data class AccessoryDto(
        val id: Int,
        val name: String,
        val slug: String,
        val description: String? = null,
        val brand: String? = null,
        val category: String? = null,
        val images: List<String> = emptyList(),
        @SerialName("average_rating")
        val averageRating: Double? = null,
        @SerialName("rating_count")
        val ratingCount: Int = 0,
        val variants: List<AccessoryVariantDto> = emptyList(),
    )

    @Serializable
    data class AccessoryListResponse(
        val data: List<AccessoryDto>,
        val links: JsonElement? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class AccessoryResponse(val data: AccessoryDto)
}

private object EmptyArrayAsJsonObjectSerializer :
    JsonTransformingSerializer<JsonObject>(JsonObject.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonArray && element.isEmpty()) JsonObject(emptyMap()) else element
}
