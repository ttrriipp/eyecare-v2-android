package com.eyecare.app.presentation.ar

import androidx.lifecycle.ViewModelStore
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.presentation.ar.capability.ArCapabilityProvider
import com.eyecare.app.presentation.ar.capability.ArCapabilityRequirements
import com.eyecare.app.presentation.ar.capability.ArDeviceFacts
import com.eyecare.app.presentation.ar.capability.OpenGlEsVersion
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import io.mockk.coEvery
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state checks capability before requesting permission`() {
        val viewModel = viewModel()

        assertInstanceOf(ArTryOnUiState.CheckingCapability::class.java, viewModel.uiState.value)
    }

    @Test
    fun `supported capability transitions to permission required`() {
        val viewModel = viewModel()
        drain()

        assertInstanceOf(ArTryOnUiState.PermissionRequired::class.java, viewModel.uiState.value)
    }

    @Test
    fun `unsupported capability blocks permission and late face results`() {
        val viewModel = viewModel(
            capabilityProvider = ArCapabilityProvider {
                supportedFacts().copy(apiLevel = 28)
            },
        )
        drain()

        val unsupported = assertInstanceOf(
            ArTryOnUiState.Unsupported::class.java,
            viewModel.uiState.value,
        )
        assertTrue(unsupported.failures.isNotEmpty())

        viewModel.onPermissionResult(granted = true)
        viewModel.onFaceResult(ArFaceState.Detected(frame(timestampMs = 1L)))
        viewModel.onAssetStateChanged(ArAssetState.Ready)

        assertInstanceOf(ArTryOnUiState.Unsupported::class.java, viewModel.uiState.value)
    }

    @Test
    fun `permission denial is represented in the unified state`() {
        val viewModel = viewModel()
        drain()

        viewModel.onPermissionResult(granted = false, shouldShowRationale = true)

        val denied = assertInstanceOf(
            ArTryOnUiState.PermissionDenied::class.java,
            viewModel.uiState.value,
        )
        assertTrue(denied.shouldShowRationale)
    }

    @Test
    fun `permission grant enters searching after variants load`() {
        val viewModel = viewModel()
        drain()

        viewModel.onPermissionResult(granted = true)

        val searching = assertInstanceOf(ArTryOnUiState.Searching::class.java, viewModel.uiState.value)
        assertEquals(11, searching.selectedVariant?.id)
        assertEquals(2, searching.variants.size)
    }

    @Test
    fun `permission grant exposes loading while variants are pending`() {
        val result = CompletableDeferred<Result<Frame>>()
        val viewModel = viewModel(pendingResult = result)
        drain()

        viewModel.onPermissionResult(granted = true)

        assertInstanceOf(ArTryOnUiState.Loading::class.java, viewModel.uiState.value)

        result.complete(Result.success(frame()))
        drain()

        assertInstanceOf(ArTryOnUiState.Searching::class.java, viewModel.uiState.value)
    }

    @Test
    fun `detected and lost faces transition between tracking and searching`() {
        val viewModel = viewModel()
        drain()
        viewModel.onPermissionResult(granted = true)

        viewModel.onFaceResult(ArFaceState.Detected(frame(timestampMs = 0L)))
        val tracking = assertInstanceOf(ArTryOnUiState.Tracking::class.java, viewModel.uiState.value)
        assertNotNull(tracking.pose)
        assertEquals(11, tracking.selectedVariant?.id)

        viewModel.onFaceResult(ArFaceState.NoFace)

        assertInstanceOf(ArTryOnUiState.Searching::class.java, viewModel.uiState.value)
    }

    @Test
    fun `invalid mapped transform keeps tracking but leaves pose unavailable for image fallback`() {
        val viewModel = viewModel()
        drain()
        viewModel.onPermissionResult(granted = true)
        val invalidMatrix = FaceTransformationMatrix.from(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 2f,
            ),
        )!!

        viewModel.onFaceResult(
            ArFaceState.Detected(frame(timestampMs = 0L, matrix = invalidMatrix)),
        )

        val tracking = assertInstanceOf(ArTryOnUiState.Tracking::class.java, viewModel.uiState.value)
        assertNull(tracking.pose)
    }

    @Test
    fun `asset state updates stay in the active state and preserve fallback semantics`() {
        val viewModel = viewModel()
        drain()
        viewModel.onPermissionResult(granted = true)

        viewModel.onAssetStateChanged(ArAssetState.Ready)
        val ready = assertInstanceOf(ArTryOnUiState.Searching::class.java, viewModel.uiState.value)
        assertInstanceOf(ArAssetState.Ready::class.java, ready.assetState)

        viewModel.onAssetStateChanged(ArAssetState.Failed("broken model"))
        val failed = assertInstanceOf(ArTryOnUiState.Searching::class.java, viewModel.uiState.value)
        assertEquals("broken model", (failed.assetState as ArAssetState.Failed).message)
    }

    @Test
    fun `variant selection updates only a loaded variant`() {
        val viewModel = viewModel()
        drain()
        viewModel.onPermissionResult(granted = true)
        val current = viewModel.uiState.value as ArTryOnUiState.Searching

        viewModel.selectVariant(current.variants[1])

        val selected = viewModel.uiState.value as ArTryOnUiState.Searching
        assertEquals(12, selected.selectedVariant?.id)

        viewModel.selectVariant(current.variants[1].copy(id = 999))

        assertEquals(12, (viewModel.uiState.value as ArTryOnUiState.Searching).selectedVariant?.id)
    }

    @Test
    fun `repository failure is recoverable and late face results cannot enter tracking`() {
        val viewModel = viewModel(
            repositoryResult = Result.failure<Frame>(IllegalStateException("offline")),
        )
        drain()

        val error = assertInstanceOf(ArTryOnUiState.Error::class.java, viewModel.uiState.value)
        assertEquals("We couldn't load this frame. Please try again.", error.message)

        viewModel.onPermissionResult(granted = true)
        viewModel.onFaceResult(ArFaceState.Detected(frame(timestampMs = 0L)))

        assertInstanceOf(ArTryOnUiState.Error::class.java, viewModel.uiState.value)
    }

    @Test
    fun `late permission result cannot replace a repository error`() {
        val viewModel = viewModel(
            repositoryResult = Result.failure<Frame>(IllegalStateException("offline")),
        )
        drain()

        viewModel.onPermissionResult(granted = false, shouldShowRationale = true)

        assertInstanceOf(ArTryOnUiState.Error::class.java, viewModel.uiState.value)
    }

    @Test
    fun `cleared viewmodel ignores late face results`() {
        val viewModel = viewModel()
        drain()
        viewModel.onPermissionResult(granted = true)

        ViewModelStore().apply {
            put("ar", viewModel)
            clear()
        }
        viewModel.onFaceResult(ArFaceState.Detected(frame(timestampMs = 0L)))

        assertInstanceOf(ArTryOnUiState.Searching::class.java, viewModel.uiState.value)
    }

    private fun viewModel(
        capabilityProvider: ArCapabilityProvider = ArCapabilityProvider { supportedFacts() },
        repositoryResult: Result<Frame> = Result.success(frame()),
        pendingResult: CompletableDeferred<Result<Frame>>? = null,
    ): ArViewModel {
        val repository = mockk<FrameRepository>()
        coEvery { repository.getFrame(1) } coAnswers {
            pendingResult?.await() ?: repositoryResult
        }
        return ArViewModel(repository, capabilityProvider, frameId = 1, initialVariantId = 11)
    }

    private fun drain() {
        dispatcher.scheduler.advanceUntilIdle()
    }

    private fun supportedFacts() = ArDeviceFacts(
        apiLevel = ArCapabilityRequirements.MIN_API_LEVEL,
        supportedAbis = setOf(ArCapabilityRequirements.REQUIRED_ABI),
        openGlEsVersion = OpenGlEsVersion(3, 0),
        totalRamBytes = ArCapabilityRequirements.MIN_TOTAL_RAM_BYTES,
        hasFrontCamera = true,
        availableStorageBytes = ArCapabilityRequirements.MIN_AVAILABLE_STORAGE_BYTES,
    )

    private fun frame() = Frame(
        id = 1,
        name = "Round frame",
        slug = "round-frame",
        description = null,
        brand = "Eyecare",
        category = "Full Rim",
        variants = listOf(
            variant(id = 11, name = "Matte Black"),
            variant(id = 12, name = "Tortoise"),
        ),
        images = emptyList(),
    )

    private fun variant(id: Int, name: String) = FrameVariant(
        id = id,
        name = name,
        sku = "SKU-$id",
        price = BigDecimal("4500.00"),
        compareAtPrice = null,
        attributes = null,
        arEligible = true,
        arAssetReference = "frame-$id.png",
        images = emptyList(),
    )

    private fun frame(
        timestampMs: Long,
        matrix: FaceTransformationMatrix = IDENTITY_MATRIX,
    ): FaceFrame = FaceFrame(
        noseBridgeX = 0.5f,
        noseBridgeY = 0.5f,
        leftTempleX = 0.3f,
        rightTempleX = 0.7f,
        faceWidthNorm = 0.4f,
        rotationDeg = 0f,
        imageWidth = 640,
        imageHeight = 480,
        transformationMatrix = matrix,
        timestampMs = timestampMs,
    )

    private companion object {
        val IDENTITY_MATRIX = FaceTransformationMatrix.from(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ),
        )!!
    }
}
