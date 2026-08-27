package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.SavedFrameApiService
import com.eyecare.app.data.remote.dto.FrameDtos
import com.eyecare.app.data.remote.dto.SavedFrameDtos
import com.eyecare.app.domain.model.ArAsset
import com.eyecare.app.domain.model.ArAssetFile
import com.eyecare.app.domain.model.ArAssetFormat
import com.eyecare.app.domain.model.ArAssetStatus
import com.eyecare.app.domain.model.ArCalibration
import com.eyecare.app.domain.model.ArVector
import com.eyecare.app.domain.model.SavedFrame
import com.eyecare.app.domain.model.SavedFrameAvailability
import com.eyecare.app.domain.model.SavedFramePage
import com.eyecare.app.domain.model.SavedFrameProduct
import com.eyecare.app.domain.model.SavedFrameVariant
import com.eyecare.app.domain.repository.SavedFrameRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class SavedFrameRepositoryImpl @Inject constructor(
    private val api: SavedFrameApiService,
) : SavedFrameRepository {

    override suspend fun getSavedFrames(page: Int, perPage: Int): Result<SavedFramePage> = safeApiCall {
        val response = api.getSavedFrames(page, perPage)
        SavedFramePage(
            items = response.data.map { it.toDomain() },
            currentPage = response.meta.currentPage,
            lastPage = response.meta.lastPage,
        )
    }

    override suspend fun save(productVariantId: Int): Result<SavedFrame> = safeApiCall {
        api.saveFrame(productVariantId).data.toDomain()
    }

    override suspend fun remove(productVariantId: Int): Result<Unit> = safeApiCall {
        val response = api.removeFrame(productVariantId)
        if (!response.isSuccessful && response.code() != 204 && response.code() != 200) {
            throw retrofit2.HttpException(response)
        }
    }

    private fun SavedFrameDtos.SavedFrameResource.toDomain() = SavedFrame(
        productVariantId = productVariantId,
        savedAt = savedAt,
        availability = SavedFrameAvailability.from(availability),
        variant = variant.toDomain(),
    )

    private fun SavedFrameDtos.SavedFrameVariantDto.toDomain() = SavedFrameVariant(
        id = id,
        name = name,
        sku = sku,
        price = price,
        compareAtPrice = compareAtPrice,
        attributes = attributes?.toStringMap(),
        images = images,
        ar = ar?.toDomain(),
        product = product.toDomain(),
    )

    private fun SavedFrameDtos.SavedFrameProductDto.toDomain() = SavedFrameProduct(
        id = id,
        name = name,
        brand = brand,
        category = category,
    )

    private fun FrameDtos.ArAssetDto.toDomain() = ArAsset(
        status = when (status) {
            FrameDtos.ArAssetStatusDto.READY -> ArAssetStatus.READY
        },
        asset = ArAssetFile(
            url = asset.url,
            format = when (asset.format) {
                FrameDtos.ArAssetFormatDto.GLB -> ArAssetFormat.GLB
            },
            version = asset.version,
            byteSize = asset.byteSize,
            sha256 = asset.sha256,
        ),
        calibration = ArCalibration(
            frameWidthMm = calibration.frameWidthMm,
            outerFrameHeightMm = calibration.outerFrameHeightMm,
            lensWidthMm = calibration.lensWidthMm,
            lensHeightMm = calibration.lensHeightMm,
            bridgeWidthMm = calibration.bridgeWidthMm,
            templeLengthMm = calibration.templeLengthMm,
            scale = calibration.scale.toDomain(),
            anchor = calibration.anchor.toDomain(),
            rotationDegrees = calibration.rotationDegrees.toDomain(),
        ),
    )

    private fun FrameDtos.ArVectorDto.toDomain() = ArVector(x = x, y = y, z = z)

    private fun kotlinx.serialization.json.JsonElement.toStringMap(): Map<String, String>? =
        runCatching {
            (this as? JsonObject)?.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() }
        }.getOrNull()
}
