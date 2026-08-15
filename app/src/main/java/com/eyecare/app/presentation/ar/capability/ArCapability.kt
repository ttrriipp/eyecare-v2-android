package com.eyecare.app.presentation.ar.capability

/** The device facts needed before initializing the 3D renderer. */
data class ArDeviceFacts(
    val apiLevel: Int,
    val supportedAbis: Set<String>,
    val openGlEsVersion: OpenGlEsVersion?,
    val totalRamBytes: Long,
    val hasFrontCamera: Boolean,
    val availableStorageBytes: Long,
)

fun interface ArCapabilityProvider {
    fun readFacts(): ArDeviceFacts
}

data class OpenGlEsVersion(
    val major: Int,
    val minor: Int,
) {
    init {
        require(major >= 0) { "OpenGL ES major version must not be negative" }
        require(minor >= 0) { "OpenGL ES minor version must not be negative" }
    }

    fun isAtLeast(required: OpenGlEsVersion): Boolean =
        major > required.major || (major == required.major && minor >= required.minor)

    companion object {
        private val VERSION_PATTERN = Regex("^(\\d+)\\.(\\d+)$")

        fun parse(raw: String?): OpenGlEsVersion? {
            val match = raw?.trim()?.let(VERSION_PATTERN::matchEntire) ?: return null
            return OpenGlEsVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
            )
        }
    }
}

/** Requirements are intentionally explicit so the fallback decision is auditable and testable. */
object ArCapabilityRequirements {
    const val MIN_API_LEVEL = 29
    const val REQUIRED_ABI = "arm64-v8a"
    const val MIN_TOTAL_RAM_BYTES = 4_294_967_296L // 4 GiB
    const val MIN_AVAILABLE_STORAGE_BYTES = 134_217_728L // 128 MiB headroom

    val MIN_OPEN_GL_ES_VERSION = OpenGlEsVersion(major = 3, minor = 0)
}

/** Stable failure identifiers and copy for an unsupported-device surface. */
enum class ArCapabilityFailure(
    val code: String,
    val message: String,
) {
    API_LEVEL(
        code = "api_level",
        message = "3D try-on requires Android 10 or newer.",
    ),
    ARM64_ABI(
        code = "arm64_abi",
        message = "3D try-on requires a 64-bit ARM device.",
    ),
    OPENGL_ES(
        code = "opengl_es",
        message = "This device does not support the required OpenGL ES version.",
    ),
    MEMORY(
        code = "memory",
        message = "This device does not have enough memory for 3D try-on.",
    ),
    FRONT_CAMERA(
        code = "front_camera",
        message = "A working front camera is required for 3D try-on.",
    ),
    STORAGE(
        code = "storage",
        message = "There is not enough storage available for the 3D frame.",
    ),
}

data class ArCapabilityResult(
    val failures: List<ArCapabilityFailure>,
) {
    val isSupported: Boolean get() = failures.isEmpty()
    val primaryFailure: ArCapabilityFailure? get() = failures.firstOrNull()
    val primaryMessage: String? get() = primaryFailure?.message
}

/** Pure capability evaluation; no Android services or renderer types cross this boundary. */
object ArCapability {

    fun evaluate(facts: ArDeviceFacts): ArCapabilityResult {
        val failures = buildList {
            if (facts.apiLevel < ArCapabilityRequirements.MIN_API_LEVEL) {
                add(ArCapabilityFailure.API_LEVEL)
            }
            if (ArCapabilityRequirements.REQUIRED_ABI !in facts.supportedAbis) {
                add(ArCapabilityFailure.ARM64_ABI)
            }
            if (facts.openGlEsVersion?.isAtLeast(ArCapabilityRequirements.MIN_OPEN_GL_ES_VERSION) != true) {
                add(ArCapabilityFailure.OPENGL_ES)
            }
            if (facts.totalRamBytes < ArCapabilityRequirements.MIN_TOTAL_RAM_BYTES) {
                add(ArCapabilityFailure.MEMORY)
            }
            if (!facts.hasFrontCamera) {
                add(ArCapabilityFailure.FRONT_CAMERA)
            }
            if (facts.availableStorageBytes < ArCapabilityRequirements.MIN_AVAILABLE_STORAGE_BYTES) {
                add(ArCapabilityFailure.STORAGE)
            }
        }
        return ArCapabilityResult(failures = failures)
    }
}
