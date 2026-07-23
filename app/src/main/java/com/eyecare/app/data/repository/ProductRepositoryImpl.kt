package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.ProductDao
import com.eyecare.app.data.local.entity.ProductEntity
import com.eyecare.app.data.remote.api.ProductApiService
import com.eyecare.app.data.remote.dto.ProductDtos
import com.eyecare.app.domain.model.Brand
import com.eyecare.app.domain.model.Category
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.model.forMobileCatalog
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

    override suspend fun getProducts(
        page: Int,
        search: String?,
        brandId: Int?,
        categoryId: Int?,
        sort: String?,
        inStock: Boolean?,
        minPrice: Double?,
        maxPrice: Double?,
    ): Result<List<Product>> {
        val hasFilters = search != null || brandId != null || categoryId != null || sort != null ||
            inStock != null || minPrice != null || maxPrice != null
        return try {
            val response = api.getProducts(
                page = page,
                search = search?.takeIf { it.isNotBlank() },
                brandId = brandId,
                categoryId = categoryId,
                sort = sort,
                inStock = inStock,
                minPrice = minPrice,
                maxPrice = maxPrice,
            )
            lastMeta = response.meta
            val visibleProducts = response.data.mapNotNull { it.toDomain().forMobileCatalog() }
            val visibleProductIds = visibleProducts.mapTo(mutableSetOf(), Product::id)
            // Only cache unfiltered page 1 results
            if (page == 1 && !hasFilters) {
                dao.clearAll()
                dao.insertAll(
                    response.data.filter { it.id in visibleProductIds }.map { it.toEntity() }
                )
            }
            Result.success(visibleProducts)
        } catch (e: Exception) {
            // Fallback to cache only for unfiltered page 1
            if (page == 1 && !hasFilters) {
                val cached = dao.getAll()
                if (cached.isNotEmpty()) {
                    Result.success(cached.mapNotNull { it.toDomain().forMobileCatalog() })
                }
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
        val product = api.getProduct(id).data.toDomain().forMobileCatalog()
            ?: error("Product is unavailable in the mobile catalog")
        Result.success(product)
    } catch (e: Exception) {
        val cached = dao.getById(id)?.toDomain()?.forMobileCatalog()
        if (cached != null) Result.success(cached)
        else Result.failure(e)
    }

    override suspend fun getBrands(): Result<List<Brand>> = runCatching {
        api.getBrands().data.map { Brand(it.id, it.name) }
    }

    override suspend fun getCategories(): Result<List<Category>> = runCatching {
        api.getCategories().data.map { Category(it.id, it.name) }
    }

    private fun ProductDtos.ProductDto.toEntity() = ProductEntity(
        id = id, name = name, slug = slug, description = description,
        productType = productType, brandName = brand, categoryName = category.orEmpty(),
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
        productType = productType, brand = brand, category = category.orEmpty(),
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
