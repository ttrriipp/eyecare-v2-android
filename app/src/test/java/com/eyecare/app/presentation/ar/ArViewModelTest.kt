package com.eyecare.app.presentation.ar

import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
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

    @BeforeEach fun setup() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun vm(): ArViewModel {
        val repo = mockk<FrameRepository>(relaxed = true)
        return ArViewModel(repo, frameId = 1, initialVariantId = 1)
    }

    @Test
    fun `initial state is PermissionRequired`() {
        assertInstanceOf(ArPermissionState.Required::class.java, vm().permissionState.value)
    }

    @Test
    fun `onPermissionGranted transitions to Granted`() {
        val vm = vm()
        vm.onPermissionResult(granted = true)
        assertInstanceOf(ArPermissionState.Granted::class.java, vm.permissionState.value)
    }

    @Test
    fun `onPermissionDenied transitions to Denied`() {
        val vm = vm()
        vm.onPermissionResult(granted = false)
        assertInstanceOf(ArPermissionState.Denied::class.java, vm.permissionState.value)
    }

    @Test
    fun `onPermissionDenied with shouldShowRationale=true sets rationale flag`() {
        val vm = vm()
        vm.onPermissionResult(granted = false, shouldShowRationale = true)
        val state = vm.permissionState.value as ArPermissionState.Denied
        assertTrue(state.shouldShowRationale)
    }

    @Test
    fun `onPermissionDenied with shouldShowRationale=false indicates permanent denial`() {
        val vm = vm()
        vm.onPermissionResult(granted = false, shouldShowRationale = false)
        val state = vm.permissionState.value as ArPermissionState.Denied
        assertFalse(state.shouldShowRationale)
    }

    @Test
    fun `detected face exposes a mapped pose and no face clears it`() {
        val vm = vm()

        vm.onFaceResult(ArFaceState.Detected(frame(timestampMs = 0L)))

        val pose = vm.facePose.value
        assertNotNull(pose)
        assertEquals(1f, pose!!.scale)

        vm.onFaceResult(ArFaceState.NoFace)

        assertNull(vm.facePose.value)
    }

    @Test
    fun `invalid mapped transform leaves pose unavailable for the fallback`() {
        val vm = vm()
        val invalidMatrix = FaceTransformationMatrix.from(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 2f,
            )
        )!!

        vm.onFaceResult(ArFaceState.Detected(frame(timestampMs = 0L, matrix = invalidMatrix)))

        assertNull(vm.facePose.value)
    }

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
            )
        )!!
    }
}
