package com.eyecare.app.presentation.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.presentation.ar.capability.ArCapability
import com.eyecare.app.presentation.ar.capability.ArCapabilityProvider
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose
import com.eyecare.app.presentation.ar.model.FacePoseCalibration
import com.eyecare.app.presentation.ar.tracking.PoseStabilizer
import com.eyecare.app.presentation.ar.tracking.mapFacePose
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ArViewModel.Factory::class)
class ArViewModel @AssistedInject constructor(
    private val frameRepository: FrameRepository,
    private val capabilityProvider: ArCapabilityProvider,
    @Assisted("frameId") private val frameId: Int,
    @Assisted("variantId") private val initialVariantId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("frameId") frameId: Int,
            @Assisted("variantId") initialVariantId: Int,
        ): ArViewModel
    }

    private val _uiState = MutableStateFlow<ArTryOnUiState>(ArTryOnUiState.CheckingCapability)
    val uiState: StateFlow<ArTryOnUiState> = _uiState.asStateFlow()

    private val poseStabilizer = PoseStabilizer()
    private val poseCalibration = FacePoseCalibration.ProvisionalRoundFrame
    private var loadJob: Job? = null
    private var sessionActive = true
    private var capabilityPassed = false
    private var permissionGranted = false
    private var loadedVariants: List<FrameVariant> = emptyList()
    private var selectedVariant: FrameVariant? = null
    private var latestFace: FaceFrame? = null
    private var latestPose: FacePose? = null
    private var assetState: ArAssetState = ArAssetState.Checking

    init {
        checkCapability()
    }

    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean = false) {
        if (!sessionActive || !capabilityPassed) return
        if (_uiState.value !is ArTryOnUiState.PermissionRequired &&
            _uiState.value !is ArTryOnUiState.PermissionDenied
        ) {
            return
        }

        if (!granted) {
            permissionGranted = false
            clearTracking()
            _uiState.value = ArTryOnUiState.PermissionDenied(shouldShowRationale)
            return
        }

        permissionGranted = true
        if (loadedVariants.isEmpty()) {
            _uiState.value = loadingState()
        } else {
            moveToFacePhase()
        }
    }

    fun onFaceResult(state: ArFaceState) {
        if (!canAcceptFaceEvents()) return

        when (state) {
            is ArFaceState.Detected -> {
                latestFace = state.frame
                latestPose = poseStabilizer.update(
                    pose = mapFacePose(
                        matrix = state.frame.transformationMatrix,
                        calibration = poseCalibration,
                    ),
                    timestampMs = state.frame.timestampMs,
                )
            }

            ArFaceState.NoFace,
            ArFaceState.Initialising,
            -> clearTracking()
        }

        if (loadedVariants.isNotEmpty()) moveToFacePhase()
    }

    fun onAssetStateChanged(state: ArAssetState) {
        if (!sessionActive) return
        assetState = state
        _uiState.value = when (val current = _uiState.value) {
            is ArTryOnUiState.Loading -> current.copy(assetState = state)
            is ArTryOnUiState.Searching -> current.copy(assetState = state)
            is ArTryOnUiState.Tracking -> current.copy(assetState = state)
            else -> return
        }
    }

    fun selectVariant(variant: FrameVariant) {
        if (!sessionActive) return
        val selected = loadedVariants.firstOrNull { it.id == variant.id } ?: return
        selectedVariant = selected
        _uiState.value = when (val current = _uiState.value) {
            is ArTryOnUiState.Loading -> current.copy(selectedVariant = selected)
            is ArTryOnUiState.Searching -> current.copy(selectedVariant = selected)
            is ArTryOnUiState.Tracking -> current.copy(selectedVariant = selected)
            else -> return
        }
    }

    fun retry() {
        if (!sessionActive || _uiState.value !is ArTryOnUiState.Error) return

        clearTracking()
        assetState = ArAssetState.Checking
        if (!capabilityPassed) {
            checkCapability()
            return
        }

        if (permissionGranted) {
            _uiState.value = loadingState()
        } else {
            _uiState.value = ArTryOnUiState.PermissionRequired
        }
        loadVariants()
    }

    private fun checkCapability() {
        viewModelScope.launch {
            val decision = runCatching {
                ArCapability.evaluate(capabilityProvider.readFacts())
            }.getOrElse {
                if (sessionActive) {
                    _uiState.value = ArTryOnUiState.Error(
                        message = "We couldn't check 3D support. Please try again.",
                    )
                }
                return@launch
            }
            if (!sessionActive) return@launch

            if (!decision.isSupported) {
                _uiState.value = ArTryOnUiState.Unsupported(decision.failures)
                return@launch
            }

            capabilityPassed = true
            _uiState.value = ArTryOnUiState.PermissionRequired
            loadVariants()
        }
    }

    private fun loadVariants() {
        loadJob?.cancel()
        loadedVariants = emptyList()
        selectedVariant = null
        loadJob = viewModelScope.launch {
            val result = try {
                frameRepository.getFrame(frameId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
            if (!sessionActive || !isActive) return@launch

            result.fold(
                onSuccess = { frame ->
                    if (frame.variants.isEmpty()) {
                        _uiState.value = ArTryOnUiState.Error(
                            message = "This frame has no available options.",
                        )
                        return@fold
                    }
                    loadedVariants = frame.variants
                    selectedVariant = frame.variants.firstOrNull { it.id == initialVariantId }
                        ?: frame.variants.first()
                    if (permissionGranted) moveToFacePhase()
                },
                onFailure = {
                    _uiState.value = ArTryOnUiState.Error(
                        message = "We couldn't load this frame. Please try again.",
                    )
                },
            )
        }
    }

    private fun canAcceptFaceEvents(): Boolean {
        if (!sessionActive || !permissionGranted) return false
        return when (_uiState.value) {
            is ArTryOnUiState.Loading,
            is ArTryOnUiState.Searching,
            is ArTryOnUiState.Tracking,
            -> true

            else -> false
        }
    }

    private fun moveToFacePhase() {
        if (!permissionGranted || loadedVariants.isEmpty() || !sessionActive) return

        _uiState.value = latestFace?.let { face ->
            ArTryOnUiState.Tracking(
                variants = loadedVariants,
                selectedVariant = selectedVariant,
                face = face,
                pose = latestPose,
                assetState = assetState,
            )
        } ?: ArTryOnUiState.Searching(
            variants = loadedVariants,
            selectedVariant = selectedVariant,
            assetState = assetState,
        )
    }

    private fun loadingState(): ArTryOnUiState.Loading = ArTryOnUiState.Loading(
        variants = loadedVariants,
        selectedVariant = selectedVariant,
        assetState = assetState,
    )

    private fun clearTracking() {
        poseStabilizer.reset()
        latestFace = null
        latestPose = null
    }

    override fun onCleared() {
        sessionActive = false
        loadJob?.cancel()
        clearTracking()
        super.onCleared()
    }
}
