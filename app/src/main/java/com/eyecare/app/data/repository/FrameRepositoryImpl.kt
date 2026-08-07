package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.FrameDao
import com.eyecare.app.data.local.entity.FrameEntity
import com.eyecare.app.data.remote.api.FrameApiService
import com.eyecare.app.data.remote.dto.FrameDtos
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.repository.FrameRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class FrameRepositoryImpl @Inject constructor(
    private val api: FrameApiService,
    private val dao: FrameDao,
    private val json: Json,
) : FrameRepository {

    private var lastMeta: com.eyecare.app.data.remote.dto.PaginationMeta? = null

    override suspend fun getFrames(
        page: Int,
        search: String?,
        brandId: Int?,
        categoryId: Int?,
        sort: String?,
    ): Result<List<Frame>> {
        val hasFilters = search != null || brandId != null || categoryId != null || sort != null
        return try {
            val response = api.getFrames(
                page = page,
                search = search?.takeIf { it.isNotBlank() },
                brandId = brandId,
                categoryId = categoryId,
                sort = sort,
            )
            lastMeta = response.meta
            val frames = response.data.map { it.toDomain() }
            if (page == 1 && !hasFilters) {
                dao.clearAll()
                dao.insertAll(response.data.map { it.toEntity() })
            }
            Result.success(frames)
        } catch (e: Exception) {
            if (page == 1 && !hasFilters) {
                val cached = dao.getAll()
                if (cached.isNotEmpty()) {
                    Result.success(cached.map { it.toDomain() })
                } else {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun hasMorePages(page: Int): Boolean {
        val meta = lastMeta ?: return false
        return page < meta.lastPage
    }

    override suspend fun getFrame(id: Int): Result<Frame> = try {
        Result.success(api.getFrame(id).data.toDomain())
    } catch (e: Exception) {
        val cached = dao.getById(id)?.toDomain()
        if (cached != null) Result.success(cached)
        else Result.failure(e)
    }

    private fun FrameDtos.FrameDto.toEntity() = FrameEntity(
        id = id,
        name = name,
        slug = slug,
        description = description,
        brandName = brand,
        categoryName = category.orEmpty(),
        variantsJson = json.encodeToString(variants),
        imagesJson = json.encodeToString(images),
        averageRating = averageRating,
        ratingCount = ratingCount,
    )

    private fun FrameEntity.toDomain(): Frame {
        val variants = runCatching {
            json.decodeFromString<List<FrameDtos.FrameVariantDto>>(variantsJson)
        }.getOrElse { emptyList() }
        val images = runCatching {
            json.decodeFromString<List<String>>(imagesJson)
        }.getOrElse { emptyList() }
        return Frame(
            id = id,
            name = name,
            slug = slug,
            description = description,
            brand = brandName,
            category = categoryName,
            variants = variants.map { it.toDomain() },
            images = images,
            averageRating = averageRating,
            ratingCount = ratingCount,
        )
    }

    private fun FrameDtos.FrameDto.toDomain() = Frame(
        id = id,
        name = name,
        slug = slug,
        description = description,
        brand = brand,
        category = category.orEmpty(),
        variants = variants.map { it.toDomain() },
        images = images,
        averageRating = averageRating,
        ratingCount = ratingCount,
    )

    private fun FrameDtos.FrameVariantDto.toDomain() = FrameVariant(
        id = id,
        name = name,
        sku = sku,
        price = price,
        compareAtPrice = compareAtPrice,
        attributes = attributes?.toStringMap(),
        arEligible = arEligible,
        arAssetReference = arAssetReference,
        images = images,
    )

    private fun kotlinx.serialization.json.JsonElement.toStringMap(): Map<String, String>? =
        runCatching {
            (this as? JsonObject)?.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() }
        }.getOrNull()
}
