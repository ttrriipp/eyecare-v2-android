package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.ProductDao
import com.eyecare.app.data.local.entity.ProductEntity
import com.eyecare.app.data.remote.api.ProductApiService
import com.eyecare.app.data.remote.dto.ProductDtos
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.repository.ProductRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val api: ProductApiService,
    private val dao: ProductDao,
    private val json: Json,
) : ProductRepository {

    private var lastMeta: ProductDtos.PaginationMeta? = null

    override suspend fun getProducts(page: Int): Result<List<Product>> {
        return try {
            val response = api.getProducts(page = page)
            lastMeta = response.meta
            if (page == 1) {
                dao.clearAll()
            }
            dao.insertAll(response.data.map { it.toEntity() })
            if (page == 1) {
                Result.success(dao.getAll().map { it.toDomain() })
            } else {
                Result.success(response.data.map { it.toDomain() })
            }
        } catch (e: Exception) {
            if (page == 1) {
                val cached = dao.getAll()
                if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
                else Result.failure(e)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun hasMorePages(page: Int): Boolean {
        val meta = lastMeta ?: return false
        return page < meta.lastPage
    }

    override suspend fun getProduct(id: Int): Result<Product> = try {
        Result.success(api.getProduct(id).data.toDomain())
    } catch (e: Exception) {
        val cached = dao.getById(id)
        if (cached != null) Result.success(cached.toDomain())
        else Result.failure(e)
    }

    private fun ProductDtos.ProductDto.toEntity() = ProductEntity(
        id = id, name = name, slug = slug, description = description,
        productType = productType, brandName = brand, categoryName = category,
        variantsJson = json.encodeToString(variants),
        imagesJson = json.encodeToString(images),
    )

    private fun ProductEntity.toDomain(): Product {
        val variants = runCatching {
            json.decodeFromString<List<ProductDtos.VariantDto>>(variantsJson)
        }.getOrElse { emptyList() }
        val images = runCatching {
            json.decodeFromString<List<String>>(imagesJson)
        }.getOrElse { emptyList() }
        return Product(
            id = id, name = name, slug = slug, description = description,
            productType = productType, brand = brandName, category = categoryName,
            variants = variants.map { it.toDomain() },
            images = images,
        )
    }

    private fun ProductDtos.ProductDto.toDomain() = Product(
        id = id, name = name, slug = slug, description = description,
        productType = productType, brand = brand, category = category,
        variants = variants.map { it.toDomain() },
        images = images,
    )

    private fun ProductDtos.VariantDto.toDomain() = ProductVariant(
        id = id, name = name, sku = sku, price = price,
        compareAtPrice = compareAtPrice,
        attributes = attributes?.toStringMap(),
        inStock = inStock,
        arEligible = arEligible, arAssetReference = arAssetReference,
        images = images,
    )

    private fun kotlinx.serialization.json.JsonElement.toStringMap(): Map<String, String>? =
        runCatching {
            (this as? JsonObject)?.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() }
        }.getOrNull()
}
