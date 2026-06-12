package com.eyecare.app.presentation.ar

import com.eyecare.app.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun vm(): ArViewModel {
        val repo = mockk<ProductRepository>(relaxed = true)
        return ArViewModel(repo, productId = 1, initialVariantId = 1)
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
}
