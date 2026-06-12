package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.ProductDao
import com.eyecare.app.data.local.entity.ProductEntity
import com.eyecare.app.data.remote.api.ProductApiService
import com.eyecare.app.data.remote.dto.ProductDtos
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductImage
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.repository.ProductRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val api: ProductApiService,
    private val dao: ProductDao,
) : ProductRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getProducts(): Result<List<Product>> {
        return try {
            val response = api.getProducts()
            val entities = response.data.map { it.toEntity() }
            dao.insertAll(entities)
            Result.success(dao.getAll().map { it.toDomain() })
        } catch (e: Exception) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
            else Result.failure(e)
        }
    }

    override suspend fun getProduct(id: Int): Result<Product> = runCatching {
        api.getProduct(id).data.toDomain()
    }

    private fun ProductDtos.ProductDto.toEntity() = ProductEntity(
        id = id, name = name, slug = slug, description = description,
        price = price, dimensions = dimensions.toDisplayString(),
        brandName = brand, categoryName = category,
        variantsJson = json.encodeToString(variants),
        imagesJson = json.encodeToString(images),
    )

    private fun ProductEntity.toDomain(): Product {
        val variants = runCatching {
            json.decodeFromString<List<ProductDtos.VariantDto>>(variantsJson)
        }.getOrElse { emptyList() }
        val images = runCatching {
            json.decodeFromString<List<ProductDtos.ImageDto>>(imagesJson)
        }.getOrElse { emptyList() }
        return Product(
            id = id, name = name, slug = slug, description = description,
            price = price, dimensions = dimensions,
            brand = brandName, category = categoryName,
            variants = variants.map { it.toDomain() },
            images = images.map { it.toDomain() },
        )
    }

    private fun ProductDtos.ProductDto.toDomain() = Product(
        id = id, name = name, slug = slug, description = description,
        price = price, dimensions = dimensions.toDisplayString(),
        brand = brand, category = category,
        variants = variants.map { it.toDomain() },
        images = images.map { it.toDomain() },
    )

    private fun ProductDtos.VariantDto.toDomain() = ProductVariant(
        id = id, name = name, sku = sku, price = price,
        dimensions = dimensions.toDisplayString(),
        arEligible = arEligible, arAssetReference = arAssetReference,
    )

    private fun ProductDtos.ImageDto.toDomain() = ProductImage(
        id = id, path = path, isPrimary = isPrimary, sortOrder = sortOrder,
    )

    /** Converts JsonElement? (object or primitive) to a human-readable string or null. */
    private fun JsonElement?.toDisplayString(): String? {
        this ?: return null
        return if (this is JsonPrimitive) content.takeIf { it.isNotBlank() }
        else runCatching {
            jsonObject.entries.joinToString(" · ") { (k, v) ->
                "${k.replaceFirstChar { it.uppercase() }}: ${(v as? JsonPrimitive)?.content ?: v}"
            }
        }.getOrNull()
    }
}
