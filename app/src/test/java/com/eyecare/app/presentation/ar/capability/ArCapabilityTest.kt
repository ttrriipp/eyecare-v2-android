package com.eyecare.app.presentation.ar.capability

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArCapabilityTest {

    @Test
    fun `reference device facts are supported`() {
        val result = ArCapability.evaluate(referenceFacts())

        assertTrue(result.isSupported)
        assertTrue(result.failures.isEmpty())
    }

    @Test
    fun `android 10 is the minimum supported API`() {
        assertTrue(ArCapability.evaluate(referenceFacts(apiLevel = 29)).isSupported)

        val result = ArCapability.evaluate(referenceFacts(apiLevel = 28))

        assertFalse(result.isSupported)
        assertEquals(listOf(ArCapabilityFailure.API_LEVEL), result.failures)
    }

    @Test
    fun `only 64 bit ARM is supported`() {
        assertTrue(
            ArCapability.evaluate(referenceFacts(supportedAbis = setOf("arm64-v8a"))).isSupported,
        )

        val result = ArCapability.evaluate(referenceFacts(supportedAbis = setOf("armeabi-v7a")))

        assertFalse(result.isSupported)
        assertEquals(listOf(ArCapabilityFailure.ARM64_ABI), result.failures)
    }

    @Test
    fun `GLES 3 point 0 is the minimum supported version`() {
        assertTrue(
            ArCapability.evaluate(
                referenceFacts(openGlEsVersion = OpenGlEsVersion(3, 0)),
            ).isSupported,
        )

        val result = ArCapability.evaluate(
            referenceFacts(openGlEsVersion = OpenGlEsVersion(2, 0)),
        )

        assertFalse(result.isSupported)
        assertEquals(listOf(ArCapabilityFailure.OPENGL_ES), result.failures)
    }

    @Test
    fun `at least four GiB total RAM is required`() {
        assertTrue(
            ArCapability.evaluate(
                referenceFacts(totalRamBytes = ArCapabilityRequirements.MIN_TOTAL_RAM_BYTES),
            ).isSupported,
        )

        val result = ArCapability.evaluate(
            referenceFacts(totalRamBytes = ArCapabilityRequirements.MIN_TOTAL_RAM_BYTES - 1),
        )

        assertFalse(result.isSupported)
        assertEquals(listOf(ArCapabilityFailure.MEMORY), result.failures)
    }

    @Test
    fun `a front camera is required`() {
        val result = ArCapability.evaluate(referenceFacts(hasFrontCamera = false))

        assertFalse(result.isSupported)
        assertEquals(listOf(ArCapabilityFailure.FRONT_CAMERA), result.failures)
    }

    @Test
    fun `storage headroom is inclusive at the declared minimum`() {
        assertTrue(
            ArCapability.evaluate(
                referenceFacts(
                    availableStorageBytes = ArCapabilityRequirements.MIN_AVAILABLE_STORAGE_BYTES,
                ),
            ).isSupported,
        )

        val result = ArCapability.evaluate(
            referenceFacts(
                availableStorageBytes = ArCapabilityRequirements.MIN_AVAILABLE_STORAGE_BYTES - 1,
            ),
        )

        assertFalse(result.isSupported)
        assertEquals(listOf(ArCapabilityFailure.STORAGE), result.failures)
    }

    @Test
    fun `simultaneous failures preserve stable reason order`() {
        val result = ArCapability.evaluate(
            ArDeviceFacts(
                apiLevel = 28,
                supportedAbis = setOf("armeabi-v7a"),
                openGlEsVersion = OpenGlEsVersion(2, 0),
                totalRamBytes = ArCapabilityRequirements.MIN_TOTAL_RAM_BYTES - 1,
                hasFrontCamera = false,
                availableStorageBytes = ArCapabilityRequirements.MIN_AVAILABLE_STORAGE_BYTES - 1,
            ),
        )

        assertFalse(result.isSupported)
        assertEquals(
            listOf(
                ArCapabilityFailure.API_LEVEL,
                ArCapabilityFailure.ARM64_ABI,
                ArCapabilityFailure.OPENGL_ES,
                ArCapabilityFailure.MEMORY,
                ArCapabilityFailure.FRONT_CAMERA,
                ArCapabilityFailure.STORAGE,
            ),
            result.failures,
        )
    }

    @Test
    fun `GLES version parser rejects malformed values`() {
        assertEquals(OpenGlEsVersion(3, 2), OpenGlEsVersion.parse("3.2"))
        assertEquals(OpenGlEsVersion(3, 0), OpenGlEsVersion.parse("3.0"))
        assertEquals(null, OpenGlEsVersion.parse("OpenGL ES 3.2"))
        assertEquals(null, OpenGlEsVersion.parse(""))
    }

    private fun referenceFacts(
        apiLevel: Int = 29,
        supportedAbis: Set<String> = setOf("arm64-v8a"),
        openGlEsVersion: OpenGlEsVersion? = OpenGlEsVersion(3, 2),
        totalRamBytes: Long = ArCapabilityRequirements.MIN_TOTAL_RAM_BYTES,
        hasFrontCamera: Boolean = true,
        availableStorageBytes: Long = ArCapabilityRequirements.MIN_AVAILABLE_STORAGE_BYTES,
    ): ArDeviceFacts = ArDeviceFacts(
        apiLevel = apiLevel,
        supportedAbis = supportedAbis,
        openGlEsVersion = openGlEsVersion,
        totalRamBytes = totalRamBytes,
        hasFrontCamera = hasFrontCamera,
        availableStorageBytes = availableStorageBytes,
    )
}
