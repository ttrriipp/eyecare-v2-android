package com.eyecare.app.data.ar

import com.eyecare.app.domain.model.ArAsset
import com.eyecare.app.domain.model.ArAssetFailureReason
import com.eyecare.app.domain.model.ArAssetFile
import com.eyecare.app.domain.model.ArAssetFormat
import com.eyecare.app.domain.model.ArAssetIdentity
import com.eyecare.app.domain.model.ArAssetLoadResult
import com.eyecare.app.domain.model.ArAssetStatus
import com.eyecare.app.domain.model.ArAssetUnsupportedReason
import com.eyecare.app.domain.model.ArCalibration
import com.eyecare.app.domain.model.ArVector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArAssetPolicyTest {

    private val checksum = "a".repeat(64)

    @Test
    fun `accepts a valid ready asset and creates its deterministic cache key`() {
        val result = ArAssetPolicy.validate(variantId = 42, asset = asset())

        val accepted = assertInstanceOf(ArAssetPolicyDecision.Accepted::class.java, result)
        assertEquals(42, accepted.identity.variantId)
        assertEquals(2, accepted.identity.version)
        assertEquals(checksum, accepted.identity.sha256)
        assertEquals("variant-42-v2-sha256-$checksum.glb", accepted.cacheKey.value)
    }

    @Test
    fun `accepts the exact ten mib boundary`() {
        val result = ArAssetPolicy.validate(
            variantId = 42,
            asset = asset(byteSize = ArAssetPolicy.MAX_ASSET_BYTES),
        )

        assertInstanceOf(ArAssetPolicyDecision.Accepted::class.java, result)
    }

    @Test
    fun `cache key is deterministic and cannot contain path traversal`() {
        val first = ArAssetPolicy.cacheKey(
            ArAssetIdentity(variantId = 42, version = 2, sha256 = checksum),
        )
        val second = ArAssetPolicy.cacheKey(
            ArAssetIdentity(variantId = 42, version = 2, sha256 = checksum),
        )

        assertEquals(first, second)
        assertTrue(first.value.startsWith("variant-42-v2-sha256-"))
        assertTrue(first.value.endsWith(".glb"))
        assertFalse(first.value.contains('/'))
        assertFalse(first.value.contains('\\'))
        assertFalse(first.value.contains(".."))
    }

    @Test
    fun `rejects non-https urls`() {
        val result = ArAssetPolicy.validate(
            variantId = 42,
            asset = asset(url = "http://cdn.example.test/model.glb"),
        )

        assertRejected(result, ArAssetPolicyViolation.NON_HTTPS_URL)
    }

    @Test
    fun `rejects invalid variant ids and embedded url credentials`() {
        val invalidVariant = ArAssetPolicy.validate(variantId = 0, asset = asset())
        val embeddedCredentials = ArAssetPolicy.validate(
            variantId = 42,
            asset = asset(url = "https://user:pass@cdn.example.test/model.glb"),
        )

        assertRejected(invalidVariant, ArAssetPolicyViolation.INVALID_VARIANT_ID)
        assertRejected(embeddedCredentials, ArAssetPolicyViolation.NON_HTTPS_URL)
    }

    @Test
    fun `rejects invalid sha and non-positive version`() {
        val invalidSha = ArAssetPolicy.validate(
            variantId = 42,
            asset = asset(sha256 = "not-a-sha"),
        )
        val invalidVersion = ArAssetPolicy.validate(
            variantId = 42,
            asset = asset(version = 0),
        )

        assertRejected(invalidSha, ArAssetPolicyViolation.INVALID_SHA256)
        assertRejected(invalidVersion, ArAssetPolicyViolation.NON_POSITIVE_VERSION)
    }

    @Test
    fun `rejects declared and actual files over the ten mib ceiling`() {
        val declaredTooLarge = ArAssetPolicy.validate(
            variantId = 42,
            asset = asset(byteSize = ArAssetPolicy.MAX_ASSET_BYTES + 1),
        )
        val actualTooLarge = ArAssetPolicy.validateDownloaded(
            variantId = 42,
            asset = asset(),
            actualByteSize = ArAssetPolicy.MAX_ASSET_BYTES + 1,
            actualSha256 = checksum,
        )

        assertRejected(declaredTooLarge, ArAssetPolicyViolation.DECLARED_SIZE_EXCEEDED)
        assertRejected(actualTooLarge, ArAssetPolicyViolation.ACTUAL_SIZE_EXCEEDED)
    }

    @Test
    fun `rejects a downloaded file with a mismatched checksum`() {
        val result = ArAssetPolicy.validateDownloaded(
            variantId = 42,
            asset = asset(),
            actualByteSize = 5_256_552,
            actualSha256 = "b".repeat(64),
        )

        assertRejected(result, ArAssetPolicyViolation.CHECKSUM_MISMATCH)
    }

    @Test
    fun `rejects a downloaded file whose size differs from the declaration`() {
        val result = ArAssetPolicy.validateDownloaded(
            variantId = 42,
            asset = asset(),
            actualByteSize = 5_256_551,
            actualSha256 = checksum,
        )

        assertRejected(result, ArAssetPolicyViolation.ACTUAL_SIZE_MISMATCH)
    }

    @Test
    fun `load results distinguish cache, download, unsupported, and recoverable failure`() {
        val identity = ArAssetIdentity(variantId = 42, version = 2, sha256 = checksum)
        val cached = ArAssetLoadResult.Cached(identity = identity, asset = asset(), localFilePath = "/tmp/cached.glb")
        val downloaded = ArAssetLoadResult.Downloaded(identity = identity, asset = asset(), localFilePath = "/tmp/downloaded.glb")
        val unsupported = ArAssetLoadResult.Unsupported(ArAssetUnsupportedReason.NO_READY_ASSET)
        val recoverable = ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.CHECKSUM_MISMATCH)

        assertInstanceOf(ArAssetLoadResult.Cached::class.java, cached)
        assertInstanceOf(ArAssetLoadResult.Downloaded::class.java, downloaded)
        assertInstanceOf(ArAssetLoadResult.Unsupported::class.java, unsupported)
        assertInstanceOf(ArAssetLoadResult.RecoverableFailure::class.java, recoverable)
        assertFalse(cached::class == downloaded::class)
        assertEquals(ArAssetUnsupportedReason.NO_READY_ASSET, unsupported.reason)
        assertEquals(ArAssetFailureReason.CHECKSUM_MISMATCH, recoverable.reason)
    }

    private fun assertRejected(
        result: ArAssetPolicyDecision,
        violation: ArAssetPolicyViolation,
    ) {
        val rejected = assertInstanceOf(ArAssetPolicyDecision.Rejected::class.java, result)
        assertEquals(violation, rejected.violation)
    }

    private fun asset(
        url: String = "https://cdn.example.test/ar/variants/42/v2/model.glb",
        version: Int = 2,
        byteSize: Long = 5_256_552,
        sha256: String = checksum,
    ) = ArAsset(
        status = ArAssetStatus.READY,
        asset = ArAssetFile(
            url = url,
            format = ArAssetFormat.GLB,
            version = version,
            byteSize = byteSize,
            sha256 = sha256,
        ),
        calibration = ArCalibration(
            frameWidthMm = 123.0,
            outerFrameHeightMm = 48.0,
            lensWidthMm = 50.0,
            lensHeightMm = 45.0,
            bridgeWidthMm = 20.0,
            templeLengthMm = 140.0,
            scale = ArVector(0.123, 0.144565, 0.123),
            anchor = ArVector(0.0, 0.0, 0.0),
            rotationDegrees = ArVector(0.0, 0.0, 0.0),
        ),
    )
}
