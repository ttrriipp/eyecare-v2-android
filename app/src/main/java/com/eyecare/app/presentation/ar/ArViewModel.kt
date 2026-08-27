package com.eyecare.app.presentation.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ArAssetFailureReason
import com.eyecare.app.domain.model.ArAssetLoadResult
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.model.isTypedArReady
import com.eyecare.app.domain.repository.ArAssetRepository
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.domain.repository.SavedFrameRepository
import com.eyecare.app.presentation.ar.capability.ArCapability
import com.eyecare.app.presentation.ar.capability.ArCapabilityProvider
import com.eyecare.app.presentation.ar.model.ArAssetSource
import com.eyecare.app.presentation.ar.model.ArAssetState
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose
import com.eyecare.app.presentation.ar.model.FacePoseCalibration
import com.eyecare.app.presentation.ar.model.FrameModelScale
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

private const val AR_PREVIEW_UNAVAILABLE_MESSAGE =
    "This frame's 3D preview is not available. Try the image preview instead."

@HiltViewModel(assistedFactory = ArViewModel.Factory::class)
class ArViewModel @AssistedInject constructor(
    private val frameRepository: FrameRepository,
    private val arAssetRepository: ArAssetRepository,
    private val capabilityProvider: ArCapabilityProvider,
    private val savedFrameRepository: SavedFrameRepository,
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

    private val _assetSource = MutableStateFlow<ArAssetSource>(ArAssetSource.NotLoaded)
    val assetSource: StateFlow<ArAssetSource> = _assetSource.asStateFlow()

    private val poseStabilizer = PoseStabilizer()
    private var poseCalibration = FacePoseCalibration.ProvisionalRoundFrame
    private var loadJob: Job? = null
    private var assetLoadJob: Job? = null
    private var assetGeneration = 0
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
        loadAssetForVariant(selected)
    }

    fun toggleSaved() {
        val variant = selectedVariant ?: return
        val current = _uiState.value
        val isSaving = when (current) {
            is ArTryOnUiState.Loading -> current.isSaving
            is ArTryOnUiState.Searching -> current.isSaving
            is ArTryOnUiState.Tracking -> current.isSaving
            else -> return
        }
        if (isSaving) return

        _uiState.value = when (current) {
            is ArTryOnUiState.Loading -> current.copy(isSaving = true, saveError = null)
            is ArTryOnUiState.Searching -> current.copy(isSaving = true, saveError = null)
            is ArTryOnUiState.Tracking -> current.copy(isSaving = true, saveError = null)
            else -> return
        }

        viewModelScope.launch {
            val result = if (variant.isSaved) {
                savedFrameRepository.remove(variant.id)
            } else {
                savedFrameRepository.save(variant.id)
            }
            result.fold(
                onSuccess = {
                    val updatedVariant = variant.copy(isSaved = !variant.isSaved)
                    selectedVariant = updatedVariant
                    loadedVariants = loadedVariants.map {
                        if (it.id == variant.id) updatedVariant else it
                    }
                    _uiState.value = when (val latest = _uiState.value) {
                        is ArTryOnUiState.Loading -> latest.copy(
                            variants = loadedVariants,
                            selectedVariant = updatedVariant,
                            isSaving = false,
                        )
                        is ArTryOnUiState.Searching -> latest.copy(
                            variants = loadedVariants,
                            selectedVariant = updatedVariant,
                            isSaving = false,
                        )
                        is ArTryOnUiState.Tracking -> latest.copy(
                            variants = loadedVariants,
                            selectedVariant = updatedVariant,
                            isSaving = false,
                        )
                        else -> return@launch
                    }
                },
                onFailure = {
                    _uiState.value = when (val latest = _uiState.value) {
                        is ArTryOnUiState.Loading -> latest.copy(
                            isSaving = false,
                            saveError = "Couldn't update saved state. Try again.",
                        )
                        is ArTryOnUiState.Searching -> latest.copy(
                            isSaving = false,
                            saveError = "Couldn't update saved state. Try again.",
                        )
                        is ArTryOnUiState.Tracking -> latest.copy(
                            isSaving = false,
                            saveError = "Couldn't update saved state. Try again.",
                        )
                        else -> return@launch
                    }
                },
            )
        }
    }

    fun clearSaveError() {
        _uiState.value = when (val current = _uiState.value) {
            is ArTryOnUiState.Loading -> current.copy(saveError = null)
            is ArTryOnUiState.Searching -> current.copy(saveError = null)
            is ArTryOnUiState.Tracking -> current.copy(saveError = null)
            else -> return
        }
    }

    fun retry() {
        if (!sessionActive || _uiState.value !is ArTryOnUiState.Error) return

        clearTracking()
        assetState = ArAssetState.Checking
        _assetSource.value = ArAssetSource.NotLoaded
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
                    selectedVariant?.let { loadAssetForVariant(it) }
                },
                onFailure = {
                    _uiState.value = ArTryOnUiState.Error(
                        message = "We couldn't load this frame. Please try again.",
                    )
                },
            )
        }
    }

    private fun loadAssetForVariant(variant: FrameVariant) {
        assetLoadJob?.cancel()
        assetGeneration++
        val currentGeneration = assetGeneration
        val arAsset = variant.ar
        if (arAsset == null || !variant.isTypedArReady) {
            _assetSource.value = ArAssetSource.NotLoaded
            assetState = ArAssetState.Failed(AR_PREVIEW_UNAVAILABLE_MESSAGE)
            updateActiveState()
            return
        }

        _assetSource.value = ArAssetSource.Loading
        assetState = ArAssetState.Loading
        updateActiveState()
        assetLoadJob = viewModelScope.launch {
            val result = try {
                arAssetRepository.load(variant.id, arAsset)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ArAssetLoadResult.RecoverableFailure(ArAssetFailureReason.NETWORK)
            }
            if (!sessionActive || !isActive) return@launch
            if (currentGeneration != assetGeneration) return@launch

            when (result) {
                is ArAssetLoadResult.Ready -> {
                    val cal = arAsset.calibration
                    _assetSource.value = ArAssetSource.Ready(
                        filePath = result.localFilePath,
                        scale = FrameModelScale(
                            x = cal.scale.x.toFloat(),
                            y = cal.scale.y.toFloat(),
                            z = cal.scale.z.toFloat(),
                        ),
                    )
                    poseCalibration = FacePoseCalibration(
                        translationScale = 0.01f,
                        scaleMultiplier = 1f,
                        mirrorFrontCamera = true,
                        anchorX = cal.anchor.x.toFloat(),
                        anchorY = cal.anchor.y.toFloat(),
                        anchorZ = cal.anchor.z.toFloat(),
                        pitchOffsetDeg = cal.rotationDegrees.x.toFloat(),
                        yawOffsetDeg = cal.rotationDegrees.y.toFloat(),
                        rollOffsetDeg = cal.rotationDegrees.z.toFloat(),
                    )
                    assetState = ArAssetState.Ready
                    moveToFacePhase()
                }

                is ArAssetLoadResult.Unsupported -> {
                    _assetSource.value = ArAssetSource.NotLoaded
                    assetState = ArAssetState.Failed(
                        AR_PREVIEW_UNAVAILABLE_MESSAGE,
                    )
                    moveToFacePhase()
                }

                is ArAssetLoadResult.RecoverableFailure -> {
                    _assetSource.value = ArAssetSource.Failed(
                        message = "The 3D frame could not be loaded. Try the image preview instead.",
                    )
                    assetState = ArAssetState.Failed(
                        "The 3D frame could not be loaded. Try the image preview instead.",
                    )
                    moveToFacePhase()
                }
            }
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

    private fun updateActiveState() {
        _uiState.value = when (val current = _uiState.value) {
            is ArTryOnUiState.Loading -> current.copy(assetState = assetState)
            is ArTryOnUiState.Searching -> current.copy(assetState = assetState)
            is ArTryOnUiState.Tracking -> current.copy(assetState = assetState)
            else -> return
        }
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
        assetLoadJob?.cancel()
        clearTracking()
        super.onCleared()
    }
}
