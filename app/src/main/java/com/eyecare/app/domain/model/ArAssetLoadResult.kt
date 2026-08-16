package com.eyecare.app.domain.model

private const val SHA256_HEX_LENGTH = 64

/** Stable identity used to bind a verified model to one catalog variant. */
data class ArAssetIdentity(
    val variantId: Int,
    val version: Int,
    val sha256: String,
) {
    init {
        require(variantId > 0) { "AR asset variant ID must be positive" }
        require(version > 0) { "AR asset version must be positive" }
        require(
            sha256.length == SHA256_HEX_LENGTH &&
                sha256.all { it in '0'..'9' || it in 'a'..'f' },
        ) {
            "AR asset sha256 must be 64 lowercase hexadecimal characters"
        }
    }
}

/** Domain result for asset loading; no File, Path, HTTP, or renderer types cross this boundary. */
sealed interface ArAssetLoadResult {
    /** A verified asset that is ready for the renderer, identified independently of its origin. */
    sealed interface Ready : ArAssetLoadResult {
        val identity: ArAssetIdentity
        val asset: ArAsset
    }

    data class Cached(
        override val identity: ArAssetIdentity,
        override val asset: ArAsset,
    ) : Ready

    data class Downloaded(
        override val identity: ArAssetIdentity,
        override val asset: ArAsset,
    ) : Ready

    data class Unsupported(
        val reason: ArAssetUnsupportedReason,
    ) : ArAssetLoadResult

    data class RecoverableFailure(
        val reason: ArAssetFailureReason,
    ) : ArAssetLoadResult
}

enum class ArAssetUnsupportedReason {
    NO_READY_ASSET,
    UNSUPPORTED_FORMAT,
}

enum class ArAssetFailureReason {
    INVALID_METADATA,
    SIZE_LIMIT_EXCEEDED,
    CHECKSUM_MISMATCH,
    CACHE_CORRUPT,
    NETWORK,
    RENDERER,
}
