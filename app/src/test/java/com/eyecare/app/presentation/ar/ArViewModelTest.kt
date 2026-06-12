package com.eyecare.app.presentation.ar

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArViewModelTest {

    @Test
    fun `initial state is PermissionRequired`() {
        val vm = ArViewModel()
        assertInstanceOf(ArPermissionState.Required::class.java, vm.permissionState.value)
    }

    @Test
    fun `onPermissionGranted transitions to Granted`() {
        val vm = ArViewModel()
        vm.onPermissionResult(granted = true)
        assertInstanceOf(ArPermissionState.Granted::class.java, vm.permissionState.value)
    }

    @Test
    fun `onPermissionDenied transitions to Denied`() {
        val vm = ArViewModel()
        vm.onPermissionResult(granted = false)
        assertInstanceOf(ArPermissionState.Denied::class.java, vm.permissionState.value)
    }

    @Test
    fun `onPermissionDenied with shouldShowRationale=true sets rationale flag`() {
        val vm = ArViewModel()
        vm.onPermissionResult(granted = false, shouldShowRationale = true)
        val state = vm.permissionState.value as ArPermissionState.Denied
        assertTrue(state.shouldShowRationale)
    }

    @Test
    fun `onPermissionDenied with shouldShowRationale=false indicates permanent denial`() {
        val vm = ArViewModel()
        vm.onPermissionResult(granted = false, shouldShowRationale = false)
        val state = vm.permissionState.value as ArPermissionState.Denied
        assertFalse(state.shouldShowRationale)
    }
}
