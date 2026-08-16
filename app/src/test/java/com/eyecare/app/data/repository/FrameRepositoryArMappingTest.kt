package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.FrameDao
import com.eyecare.app.data.local.entity.FrameEntity
import com.eyecare.app.data.remote.api.FrameApiService
import com.eyecare.app.data.remote.dto.FrameDtos
import com.eyecare.app.data.remote.ApiContractFixtures
import com.eyecare.app.domain.model.ArAssetStatus
import com.eyecare.app.domain.model.ArAssetFormat
import com.eyecare.app.domain.model.isArReady
import com.eyecare.app.domain.model.isTypedArReady
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FrameRepositoryArMappingTest {

    private val api: FrameApiService = mockk()
    private val json: Json = ApiContractFixtures.json

    @Test
    fun `maps typed ar at the repository boundary`() = runTest {
        val dto = FrameDtos.FrameDto(
            id = 7,
            name = "Round frame",
            slug = "round-frame",
            description = null,
            brand = "Eyecare",
            category = "Full Rim",
            variants = listOf(readyVariant()),
            images = emptyList(),
        )
        coEvery { api.getFrame(7) } returns FrameDtos.FrameResponse(dto)

        val frame = FrameRepositoryImpl(api, EmptyFrameDao(), json).getFrame(7).getOrThrow()
        val variant = frame.variants.single()

        assertEquals(ArAssetStatus.READY, variant.ar?.status)
        assertEquals(ArAssetFormat.GLB, variant.ar?.asset?.format)
        assertEquals(2, variant.ar?.asset?.version)
        assertEquals(123.0, variant.ar?.calibration?.frameWidthMm)
        assertEquals("legacy-preview.jpg", variant.arAssetReference)
    }

    @Test
    fun `legacy cached frame without typed ar remains image-browsable`() = runTest {
        val cachedVariant = FrameDtos.FrameVariantDto(
            id = 8,
            name = "Legacy frame",
            sku = "LEGACY-8",
            price = BigDecimal("1200.00"),
            arEligible = true,
            arAssetReference = "legacy-preview.jpg",
        )
        val cachedDao = EmptyFrameDao(
            cached = FrameEntity(
                id = 8,
                name = "Legacy frame",
                slug = "legacy-frame",
                description = null,
                brandName = "Eyecare",
                categoryName = "Full Rim",
                variantsJson = json.encodeToString(listOf(cachedVariant)),
                imagesJson = "[]",
            ),
        )
        coEvery { api.getFrame(8) } throws IllegalStateException("offline")

        val frame = FrameRepositoryImpl(api, cachedDao, json).getFrame(8).getOrThrow()
        val variant = frame.variants.single()

        assertNull(variant.ar)
        assertEquals("legacy-preview.jpg", variant.arAssetReference)
        assertFalse(variant.isTypedArReady)
        assertTrue(variant.isArReady)
    }

    private fun readyVariant() = FrameDtos.FrameVariantDto(
        id = 42,
        name = "Matte Black / 50mm",
        sku = "ROUND-BLK-50",
        price = BigDecimal("4500.00"),
        arEligible = true,
        arAssetReference = "legacy-preview.jpg",
        ar = FrameDtos.ArAssetDto(
            status = FrameDtos.ArAssetStatusDto.READY,
            asset = FrameDtos.ArAssetFileDto(
                url = "https://cdn.example.test/ar/variants/42/v2/model.glb",
                format = FrameDtos.ArAssetFormatDto.GLB,
                version = 2,
                byteSize = 5256552,
                sha256 = "a".repeat(64),
            ),
            calibration = FrameDtos.ArCalibrationDto(
                frameWidthMm = 123.0,
                outerFrameHeightMm = 48.0,
                lensWidthMm = 50.0,
                lensHeightMm = 45.0,
                bridgeWidthMm = 20.0,
                templeLengthMm = 140.0,
                scale = FrameDtos.ArVectorDto(0.123, 0.144565, 0.123),
                anchor = FrameDtos.ArVectorDto(0.0, 0.0, 0.0),
                rotationDegrees = FrameDtos.ArVectorDto(0.0, 0.0, 0.0),
            ),
        ),
    )

    private class EmptyFrameDao(var cached: FrameEntity? = null) : FrameDao {
        override suspend fun insertAll(frames: List<FrameEntity>) {
            cached = frames.firstOrNull()
        }

        override suspend fun getAll(): List<FrameEntity> = cached?.let(::listOf).orEmpty()

        override suspend fun getById(id: Int): FrameEntity? = cached?.takeIf { it.id == id }

        override suspend fun clearAll() {
            cached = null
        }
    }
}
