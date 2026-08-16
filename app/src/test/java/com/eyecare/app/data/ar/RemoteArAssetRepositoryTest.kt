package com.eyecare.app.data.ar

import com.eyecare.app.domain.model.ArAsset
import com.eyecare.app.domain.model.ArAssetFile
import com.eyecare.app.domain.model.ArAssetFormat
import com.eyecare.app.domain.model.ArAssetFailureReason
import com.eyecare.app.domain.model.ArAssetIdentity
import com.eyecare.app.domain.model.ArAssetLoadResult
import com.eyecare.app.domain.model.ArAssetStatus
import com.eyecare.app.domain.model.ArCalibration
import com.eyecare.app.domain.model.ArVector
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.security.MessageDigest

class RemoteArAssetRepositoryTest {

    @TempDir
    lateinit var tempDirectory: Path

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `downloads verified bytes and atomically promotes the cache entry`() = runTest {
        val payload = payload()
        val asset = asset(payload)
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(payload)))

        val result = repository().load(variantId = 42, asset = asset)

        assertInstanceOf(ArAssetLoadResult.Downloaded::class.java, result)
        assertEquals(1, server.requestCount)
        assertEquals(payload.toList(), cacheFile(asset).readBytes().toList())
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun `returns a valid cached version without making a network request`() = runTest {
        val payload = payload()
        val asset = asset(payload)
        cacheFile(asset).apply {
            parentFile?.mkdirs()
            writeBytes(payload)
        }

        val result = repository().load(variantId = 42, asset = asset)

        assertInstanceOf(ArAssetLoadResult.Cached::class.java, result)
        assertEquals(0, server.requestCount)
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun `evicts a corrupt cache entry before downloading a verified replacement`() = runTest {
        val payload = payload()
        val asset = asset(payload)
        val unrelatedFile = tempDirectory.resolve("keep-me.bin").toFile().apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        cacheFile(asset).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(9, 8, 7))
        }
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(payload)))

        val result = repository().load(variantId = 42, asset = asset)

        assertInstanceOf(ArAssetLoadResult.Downloaded::class.java, result)
        assertEquals(payload.toList(), cacheFile(asset).readBytes().toList())
        assertEquals(listOf<Byte>(1, 2, 3), unrelatedFile.readBytes().toList())
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun `checksum mismatch never promotes the temporary file`() = runTest {
        val payload = payload()
        val asset = asset(payload, sha256 = "b".repeat(64))
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(payload)))

        val result = repository().load(variantId = 42, asset = asset)

        val failure = assertInstanceOf(ArAssetLoadResult.RecoverableFailure::class.java, result)
        assertEquals(ArAssetFailureReason.CHECKSUM_MISMATCH, failure.reason)
        assertFalse(cacheFile(asset).exists())
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun `oversized response never promotes the temporary file`() = runTest {
        val payload = ByteArray((ArAssetPolicy.MAX_ASSET_BYTES + 1).toInt()) { 4 }
        val asset = asset(
            payload = payload,
            byteSize = ArAssetPolicy.MAX_ASSET_BYTES,
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(payload)))

        val result = repository().load(variantId = 42, asset = asset)

        val failure = assertInstanceOf(ArAssetLoadResult.RecoverableFailure::class.java, result)
        assertEquals(ArAssetFailureReason.SIZE_LIMIT_EXCEEDED, failure.reason)
        assertFalse(cacheFile(asset).exists())
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun `interrupted response leaves no partial cache entry`() = runTest {
        val payload = ByteArray(32 * 1024) { 6 }
        val asset = asset(payload)
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(payload))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )

        val result = repository().load(variantId = 42, asset = asset)

        val failure = assertInstanceOf(ArAssetLoadResult.RecoverableFailure::class.java, result)
        assertEquals(ArAssetFailureReason.NETWORK, failure.reason)
        assertFalse(cacheFile(asset).exists())
        assertTrue(tempFiles().isEmpty())
    }

    @Test
    fun `failed replacement keeps the last known-good version`() = runTest {
        val oldPayload = payload(2)
        val oldAsset = asset(oldPayload, version = 1)
        cacheFile(oldAsset).apply {
            parentFile?.mkdirs()
            writeBytes(oldPayload)
        }
        val replacementPayload = payload(3)
        val replacement = asset(replacementPayload, version = 2)
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(replacementPayload))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )

        val result = repository().load(variantId = 42, asset = replacement)

        assertInstanceOf(ArAssetLoadResult.RecoverableFailure::class.java, result)
        assertEquals(oldPayload.toList(), cacheFile(oldAsset).readBytes().toList())
        assertFalse(cacheFile(replacement).exists())
    }

    @Test
    fun `invalid metadata is rejected before any request`() = runTest {
        val asset = asset(payload(), url = "http://cdn.example.test/ar/model.glb")

        val result = repository().load(variantId = 42, asset = asset)

        val failure = assertInstanceOf(ArAssetLoadResult.RecoverableFailure::class.java, result)
        assertEquals(ArAssetFailureReason.INVALID_METADATA, failure.reason)
        assertEquals(0, server.requestCount)
    }

    private fun repository(): RemoteArAssetRepository = RemoteArAssetRepository(
        client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val rewrittenUrl = server.url(chain.request().url.encodedPath)
                chain.proceed(
                    chain.request().newBuilder()
                        .url(rewrittenUrl)
                        .build(),
                )
            }
            .build(),
        cacheDirectory = tempDirectory.toFile(),
    )

    private fun cacheFile(asset: ArAsset) = tempDirectory.resolve(
        ArAssetPolicy.cacheKey(
            ArAssetIdentity(
                variantId = 42,
                version = asset.asset.version,
                sha256 = asset.asset.sha256,
            ),
        ).value,
    ).toFile()

    private fun tempFiles() = tempDirectory.toFile().listFiles()
        .orEmpty()
        .filter { it.name.endsWith(".tmp") }

    private fun asset(
        payload: ByteArray,
        url: String = "https://cdn.example.test/ar/model.glb",
        version: Int = 2,
        byteSize: Long = payload.size.toLong(),
        sha256: String = sha256(payload),
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

    private fun payload(seed: Int = 1) = ByteArray(2048) { index -> (index + seed).toByte() }

    private fun sha256(bytes: ByteArray): String = MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }

}
