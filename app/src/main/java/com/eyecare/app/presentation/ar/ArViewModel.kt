package com.eyecare.app.presentation.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.presentation.ar.model.ArFaceState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ArPermissionState {
    data object Required : ArPermissionState
    data object Granted : ArPermissionState
    data class Denied(val shouldShowRationale: Boolean) : ArPermissionState
}

@HiltViewModel(assistedFactory = ArViewModel.Factory::class)
class ArViewModel @AssistedInject constructor(
    private val frameRepository: FrameRepository,
    @Assisted("frameId") val frameId: Int,
    @Assisted("variantId") val initialVariantId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("frameId") frameId: Int,
            @Assisted("variantId") initialVariantId: Int,
        ): ArViewModel
    }

    private val _permissionState = MutableStateFlow<ArPermissionState>(ArPermissionState.Required)
    val permissionState: StateFlow<ArPermissionState> = _permissionState.asStateFlow()

    private val _faceState = MutableStateFlow<ArFaceState>(ArFaceState.Initialising)
    val faceState: StateFlow<ArFaceState> = _faceState.asStateFlow()

    private val _variants = MutableStateFlow<List<FrameVariant>>(emptyList())
    val variants: StateFlow<List<FrameVariant>> = _variants.asStateFlow()

    private val _selectedVariant = MutableStateFlow<FrameVariant?>(null)
    val selectedVariant: StateFlow<FrameVariant?> = _selectedVariant.asStateFlow()

    init { loadVariants() }

    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean = false) {
        _permissionState.value = if (granted) ArPermissionState.Granted
        else ArPermissionState.Denied(shouldShowRationale)
    }

    fun onFaceResult(state: ArFaceState) {
        _faceState.value = state
    }

    fun selectVariant(variant: FrameVariant) {
        _selectedVariant.value = variant
    }

    private fun loadVariants() {
        viewModelScope.launch {
            frameRepository.getFrame(frameId).onSuccess { frame ->
                _variants.value = frame.variants
                _selectedVariant.value = frame.variants.firstOrNull { it.id == initialVariantId }
                    ?: frame.variants.firstOrNull()
            }
        }
    }
}
