package com.eyecare.app.data.ar

import com.eyecare.app.domain.model.ArAsset
import com.eyecare.app.domain.model.ArAssetIdentity
import java.net.URI

private const val SHA256_HEX_LENGTH = 64
private val SHA256_PATTERN = Regex("[0-9a-f]{$SHA256_HEX_LENGTH}")
private val CACHE_KEY_PATTERN = Regex(
    "variant-[1-9][0-9]*-v[1-9][0-9]*-sha256-[0-9a-f]{$SHA256_HEX_LENGTH}\\.glb",
)

/** A safe, immutable filename for the atomically promoted cache entry. */
@JvmInline
value class ArAssetCacheKey(val value: String) {
    init {
        require(CACHE_KEY_PATTERN.matches(value)) {
            "AR asset cache key contains an invalid path component"
        }
    }
}

sealed interface ArAssetPolicyDecision {
    data class Accepted(
        val identity: ArAssetIdentity,
        val cacheKey: ArAssetCacheKey,
    ) : ArAssetPolicyDecision

    data class Rejected(
        val violation: ArAssetPolicyViolation,
    ) : ArAssetPolicyDecision
}

enum class ArAssetPolicyViolation {
    INVALID_VARIANT_ID,
    NON_HTTPS_URL,
    NON_POSITIVE_VERSION,
    INVALID_SHA256,
    NON_POSITIVE_DECLARED_SIZE,
    DECLARED_SIZE_EXCEEDED,
    NON_POSITIVE_ACTUAL_SIZE,
    ACTUAL_SIZE_EXCEEDED,
    ACTUAL_SIZE_MISMATCH,
    INVALID_ACTUAL_SHA256,
    CHECKSUM_MISMATCH,
}

/** Pure download-boundary checks shared by the eventual network/cache implementation. */
object ArAssetPolicy {
    const val MAX_ASSET_BYTES: Long = 10L * 1024L * 1024L

    fun validate(variantId: Int, asset: ArAsset): ArAssetPolicyDecision {
        if (variantId <= 0) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.INVALID_VARIANT_ID)
        }
        if (!isTrustedHttpsUrl(asset.asset.url)) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.NON_HTTPS_URL)
        }
        if (asset.asset.version <= 0) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.NON_POSITIVE_VERSION)
        }
        if (!SHA256_PATTERN.matches(asset.asset.sha256)) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.INVALID_SHA256)
        }
        if (asset.asset.byteSize <= 0) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.NON_POSITIVE_DECLARED_SIZE)
        }
        if (asset.asset.byteSize > MAX_ASSET_BYTES) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.DECLARED_SIZE_EXCEEDED)
        }

        val identity = ArAssetIdentity(
            variantId = variantId,
            version = asset.asset.version,
            sha256 = asset.asset.sha256,
        )
        return ArAssetPolicyDecision.Accepted(
            identity = identity,
            cacheKey = cacheKey(identity),
        )
    }

    fun validateDownloaded(
        variantId: Int,
        asset: ArAsset,
        actualByteSize: Long,
        actualSha256: String,
    ): ArAssetPolicyDecision {
        val descriptorDecision = validate(variantId, asset)
        if (descriptorDecision is ArAssetPolicyDecision.Rejected) {
            return descriptorDecision
        }

        if (actualByteSize <= 0) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.NON_POSITIVE_ACTUAL_SIZE)
        }
        if (actualByteSize > MAX_ASSET_BYTES) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.ACTUAL_SIZE_EXCEEDED)
        }
        if (actualByteSize != asset.asset.byteSize) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.ACTUAL_SIZE_MISMATCH)
        }
        if (!SHA256_PATTERN.matches(actualSha256)) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.INVALID_ACTUAL_SHA256)
        }
        if (actualSha256 != asset.asset.sha256) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.CHECKSUM_MISMATCH)
        }

        return descriptorDecision
    }

    fun cacheKey(identity: ArAssetIdentity): ArAssetCacheKey = ArAssetCacheKey(
        "variant-${identity.variantId}-v${identity.version}-sha256-${identity.sha256}.glb",
    )

    private fun isTrustedHttpsUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull()
        return uri != null &&
            uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }
}
