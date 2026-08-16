package com.eyecare.app.data.ar

import com.eyecare.app.di.ArAssetCacheDirectory
import com.eyecare.app.domain.model.ArAsset
import com.eyecare.app.domain.model.ArAssetFailureReason
import com.eyecare.app.domain.model.ArAssetLoadResult
import com.eyecare.app.domain.repository.ArAssetRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import javax.inject.Inject

private const val TEMP_FILE_SUFFIX = ".tmp"
private const val BUFFER_SIZE = 8 * 1024

/** Downloads only typed assets and exposes verified cache state through the domain repository. */
class RemoteArAssetRepository @Inject constructor(
    private val client: OkHttpClient,
    @param:ArAssetCacheDirectory private val cacheDirectory: File,
) : ArAssetRepository {

    override suspend fun load(variantId: Int, asset: ArAsset): ArAssetLoadResult =
        withContext(Dispatchers.IO) {
            try {
                loadInternal(variantId, asset)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.NETWORK)
            }
        }

    private fun loadInternal(variantId: Int, asset: ArAsset): ArAssetLoadResult {
        val descriptorDecision = ArAssetPolicy.validate(variantId, asset)
        val accepted = descriptorDecision as? ArAssetPolicyDecision.Accepted
            ?: return descriptorDecision.toLoadFailure()

        if (!ensureCacheDirectory()) {
            return ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.CACHE_CORRUPT)
        }

        val target = File(cacheDirectory, accepted.cacheKey.value)
            if (target.isFile) {
            val cachedDecision = validateFile(target, variantId, asset)
            if (cachedDecision is ArAssetPolicyDecision.Accepted) {
                return ArAssetLoadResult.Cached(
                    identity = cachedDecision.identity,
                    asset = asset,
                    localFilePath = target.absolutePath,
                )
            }
            if (target.exists() && !target.delete()) {
                return ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.CACHE_CORRUPT)
            }
        }

        return downloadAndPromote(
            variantId = variantId,
            asset = asset,
            target = target,
        )
    }

    private fun downloadAndPromote(
        variantId: Int,
        asset: ArAsset,
        target: File,
    ): ArAssetLoadResult {
        val request = Request.Builder()
            .url(asset.asset.url)
            .get()
            .build()

        val temporary = try {
            File.createTempFile("ar-asset-", TEMP_FILE_SUFFIX, cacheDirectory)
        } catch (_: IOException) {
            return ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.CACHE_CORRUPT)
        }

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.NETWORK)
                }
                val body = response.body
                    ?: return ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.NETWORK)
                if (body.contentLength() > ArAssetPolicy.MAX_ASSET_BYTES) {
                    return ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.SIZE_LIMIT_EXCEEDED)
                }

                val downloaded = writeBody(body, temporary)
                when (
                    val decision = ArAssetPolicy.validateDownloaded(
                        variantId = variantId,
                        asset = asset,
                        actualByteSize = downloaded.byteSize,
                        actualSha256 = downloaded.sha256,
                    )
                ) {
                    is ArAssetPolicyDecision.Rejected -> return decision.toLoadFailure()
                    is ArAssetPolicyDecision.Accepted -> {
                        if (!promoteAtomically(temporary, target)) {
                            return ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.CACHE_CORRUPT)
                        }
                        ArAssetLoadResult.Downloaded(
                            identity = decision.identity,
                            asset = asset,
                            localFilePath = target.absolutePath,
                        )
                    }
                }
            }
        } catch (_: AssetTooLargeException) {
            ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.SIZE_LIMIT_EXCEEDED)
        } catch (_: IOException) {
            ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.NETWORK)
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun validateFile(
        file: File,
        variantId: Int,
        asset: ArAsset,
    ): ArAssetPolicyDecision? {
        val byteSize = file.length()
        if (byteSize <= 0) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.NON_POSITIVE_ACTUAL_SIZE)
        }
        if (byteSize > ArAssetPolicy.MAX_ASSET_BYTES) {
            return ArAssetPolicyDecision.Rejected(ArAssetPolicyViolation.ACTUAL_SIZE_EXCEEDED)
        }
        return runCatching {
            ArAssetPolicy.validateDownloaded(
                variantId = variantId,
                asset = asset,
                actualByteSize = byteSize,
                actualSha256 = file.inputStream().use { input -> sha256(input) },
            )
        }.getOrNull()
    }

    private fun writeBody(
        body: okhttp3.ResponseBody,
        temporary: File,
    ): DownloadedMetadata {
        val digest = MessageDigest.getInstance("SHA-256")
        var byteSize = 0L
        val buffer = ByteArray(BUFFER_SIZE)
        body.byteStream().use { input ->
            FileOutputStream(temporary).use { output ->
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    byteSize += read
                    if (byteSize > ArAssetPolicy.MAX_ASSET_BYTES) {
                        throw AssetTooLargeException()
                    }
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
                output.fd.sync()
            }
        }
        return DownloadedMetadata(
            byteSize = byteSize,
            sha256 = digest.digest().toHexString(),
        )
    }

    private fun sha256(input: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().toHexString()
    }

    private fun ensureCacheDirectory(): Boolean {
        if (cacheDirectory.exists()) return cacheDirectory.isDirectory
        return cacheDirectory.mkdirs() || cacheDirectory.isDirectory
    }

    private fun promoteAtomically(temporary: File, target: File): Boolean = try {
        Files.move(
            temporary.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
        true
    } catch (_: AtomicMoveNotSupportedException) {
        false
    } catch (_: IOException) {
        false
    }

    private fun ArAssetPolicyDecision.toLoadFailure(): ArAssetLoadResult.RecoverableFailure =
        ArAssetLoadResult.RecoverableFailure(
            reason = when (this) {
                is ArAssetPolicyDecision.Accepted -> ArAssetFailureReason.INVALID_METADATA
                is ArAssetPolicyDecision.Rejected -> when (violation) {
                    ArAssetPolicyViolation.DECLARED_SIZE_EXCEEDED,
                    ArAssetPolicyViolation.ACTUAL_SIZE_EXCEEDED,
                    ArAssetPolicyViolation.NON_POSITIVE_ACTUAL_SIZE,
                    -> ArAssetFailureReason.SIZE_LIMIT_EXCEEDED

                    ArAssetPolicyViolation.CHECKSUM_MISMATCH,
                    ArAssetPolicyViolation.INVALID_ACTUAL_SHA256,
                    -> ArAssetFailureReason.CHECKSUM_MISMATCH

                    else -> ArAssetFailureReason.INVALID_METADATA
                }
            },
        )

    private data class DownloadedMetadata(
        val byteSize: Long,
        val sha256: String,
    )

    private class AssetTooLargeException : IOException()
}

private fun ByteArray.toHexString(): String = joinToString("") { byte -> "%02x".format(byte) }
