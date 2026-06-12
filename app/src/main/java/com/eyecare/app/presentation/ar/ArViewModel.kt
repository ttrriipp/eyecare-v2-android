package com.eyecare.app.presentation.ar

import androidx.lifecycle.ViewModel
import com.eyecare.app.presentation.ar.model.ArFaceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface ArPermissionState {
    data object Required : ArPermissionState
    data object Granted : ArPermissionState
    data class Denied(val shouldShowRationale: Boolean) : ArPermissionState
}

@HiltViewModel
class ArViewModel @Inject constructor() : ViewModel() {

    private val _permissionState = MutableStateFlow<ArPermissionState>(ArPermissionState.Required)
    val permissionState: StateFlow<ArPermissionState> = _permissionState.asStateFlow()

    private val _faceState = MutableStateFlow<ArFaceState>(ArFaceState.Initialising)
    val faceState: StateFlow<ArFaceState> = _faceState.asStateFlow()

    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean = false) {
        _permissionState.value = if (granted) ArPermissionState.Granted
        else ArPermissionState.Denied(shouldShowRationale)
    }

    fun onFaceResult(state: ArFaceState) {
        _faceState.value = state
    }
}
