package com.eyecare.app.presentation.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ArAssetFailureReason
import com.eyecare.app.domain.model.ArAssetLoadResult
import com.eyecare.app.domain.model.Frame
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
import com.eyecare.app.presentation.ar.model.ArTrackingQuality
import com.eyecare.app.presentation.ar.model.ArTryOnUiState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose
import com.eyecare.app.presentation.ar.model.FacePoseCalibration
import com.eyecare.app.presentation.ar.model.FrameModelScale
import com.eyecare.app.presentation.ar.tracking.FaceDistanceScaleTracker
import com.eyecare.app.presentation.ar.tracking.PoseStabilizer
import com.eyecare.app.presentation.ar.tracking.classifyFaceTrackingQuality
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
private const val QUALITY_LOSS_SAMPLE_COUNT = 3
private const val QUALITY_RECOVERY_SAMPLE_COUNT = 3

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
    private val faceDistanceScaleTracker = FaceDistanceScaleTracker()
    private var poseCalibration = FacePoseCalibration.ProvisionalRoundFrame
    private var loadJob: Job? = null
    private var assetLoadJob: Job? = null
    private var saveJob: Job? = null
    private var refreshJob: Job? = null
    private var assetGeneration = 0
    private var sessionActive = true
    private var capabilityPassed = false
    private var permissionGranted = false
    private var loadedVariants: List<FrameVariant> = emptyList()
    private var loadedFrameName: String? = null
    private var selectedVariant: FrameVariant? = null
    private var latestFace: FaceFrame? = null
    private var latestPose: FacePose? = null
    private var trackingQuality = ArTrackingQuality.Stabilizing
    private var consecutiveStableQualitySamples = 0
    private var consecutiveUnstableQualitySamples = 0
    private var assetState: ArAssetState = ArAssetState.Checking
    private var hasTrackedThisSession = false

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
                val previousFace = latestFace
                when {
                    previousFace == null || state.frame.timestampMs > previousFace.timestampMs -> {
                        latestFace = state.frame
                        val mappedPose = mapFacePose(
                            matrix = state.frame.transformationMatrix,
                            calibration = poseCalibration,
                        )
                        val distanceAdjustedPose = mappedPose?.let { pose ->
                            faceDistanceScaleTracker.update(
                                faceWidthNorm = state.frame.faceWidthNorm,
                                mappedPoseScale = pose.scale,
                                yawDeg = pose.yawDeg,
                                faceCenterX = state.frame.noseBridgeX,
                                faceCenterY = state.frame.noseBridgeY,
                                pitchDeg = pose.pitchDeg,
                                rollDeg = pose.rollDeg,
                            )?.let { scale -> pose.copy(scale = scale) }
                        }
                        val candidateQuality = classifyFaceTrackingQuality(
                            face = state.frame,
                            pose = distanceAdjustedPose,
                        )
                        val previousQuality = trackingQuality
                        val effectiveQuality = updateTrackingQuality(candidateQuality)
                        latestPose = when {
                            effectiveQuality == ArTrackingQuality.Stable &&
                                candidateQuality == ArTrackingQuality.Stable -> {
                                poseStabilizer.update(
                                    pose = distanceAdjustedPose,
                                    timestampMs = state.frame.timestampMs,
                                )
                            }
                            effectiveQuality == ArTrackingQuality.Stable -> {
                                // Keep the last good pose during the short grace window. This
                                // prevents a single malformed transform from blanking the model
                                // before hysteresis can decide whether tracking is really lost.
                                latestPose
                            }
                            else -> {
                                if (previousQuality == ArTrackingQuality.Stable) {
                                    poseStabilizer.reset()
                                }
                                null
                            }
                        }
                    }

                    state.frame.timestampMs == previousFace.timestampMs -> {
                        // The mask pairer republishes this face timestamp when the asynchronous
                        // segmentation callback arrives. Attach the richer frame, but do not
                        // feed a duplicate timestamp into PoseStabilizer (which would reset the
                        // pose and make the rendered frame blink).
                        latestFace = state.frame
                    }
                    // Older callbacks cannot move the renderer backwards.
                }
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
            is ArTryOnUiState.Loading -> current.copy(
                selectedVariant = selected,
                saveError = null,
                saveMessage = null,
            )
            is ArTryOnUiState.Searching -> current.copy(
                selectedVariant = selected,
                saveError = null,
                saveMessage = null,
            )
            is ArTryOnUiState.Tracking -> current.copy(
                selectedVariant = selected,
                saveError = null,
                saveMessage = null,
            )
            else -> return
        }
        loadAssetForVariant(selected)
    }

    fun toggleSaved() {
        val variant = selectedVariant ?: return
        val wasSaved = variant.isSaved
        val current = _uiState.value
        val isSaving = when (current) {
            is ArTryOnUiState.Loading -> current.isSaving
            is ArTryOnUiState.Searching -> current.isSaving
            is ArTryOnUiState.Tracking -> current.isSaving
            else -> return
        }
        if (isSaving || saveJob?.isActive == true) return

        _uiState.value = when (current) {
            is ArTryOnUiState.Loading -> current.copy(isSaving = true, saveError = null, saveMessage = null)
            is ArTryOnUiState.Searching -> current.copy(isSaving = true, saveError = null, saveMessage = null)
            is ArTryOnUiState.Tracking -> current.copy(isSaving = true, saveError = null, saveMessage = null)
        }

        saveJob = viewModelScope.launch {
            val result = if (wasSaved) {
                savedFrameRepository.remove(variant.id)
            } else {
                savedFrameRepository.save(variant.id)
            }
            result.fold(
                onSuccess = {
                    val updatedVariant = variant.copy(isSaved = !wasSaved)
                    val updatedVariants = loadedVariants.map {
                        if (it.id == variant.id) updatedVariant else it
                    }
                    loadedVariants = updatedVariants
                    val latest = _uiState.value
                    val latestSelectedId = when (latest) {
                        is ArTryOnUiState.Loading -> latest.selectedVariant?.id
                        is ArTryOnUiState.Searching -> latest.selectedVariant?.id
                        is ArTryOnUiState.Tracking -> latest.selectedVariant?.id
                        else -> null
                    }
                    val reconciledSelected = updatedVariants.firstOrNull { it.id == latestSelectedId }
                        ?: updatedVariants.firstOrNull { it.id == variant.id }
                    selectedVariant = reconciledSelected
                    val saveMessage = if (!wasSaved) {
                        "Saved as a preference. Availability is not guaranteed."
                    } else {
                        "Removed from saved frames."
                    }
                    _uiState.value = when (latest) {
                        is ArTryOnUiState.Loading -> latest.copy(
                            variants = loadedVariants,
                            selectedVariant = reconciledSelected,
                            isSaving = false,
                            saveError = null,
                            saveMessage = saveMessage,
                        )
                        is ArTryOnUiState.Searching -> latest.copy(
                            variants = loadedVariants,
                            selectedVariant = reconciledSelected,
                            isSaving = false,
                            saveError = null,
                            saveMessage = saveMessage,
                        )
                        is ArTryOnUiState.Tracking -> latest.copy(
                            variants = loadedVariants,
                            selectedVariant = reconciledSelected,
                            isSaving = false,
                            saveError = null,
                            saveMessage = saveMessage,
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

    fun clearSaveMessage() {
        _uiState.value = when (val current = _uiState.value) {
            is ArTryOnUiState.Loading -> current.copy(saveMessage = null)
            is ArTryOnUiState.Searching -> current.copy(saveMessage = null)
            is ArTryOnUiState.Tracking -> current.copy(saveMessage = null)
            else -> return
        }
    }

    /** Reconciles account-owned saved state without restarting the camera or active asset. */
    fun refreshSavedState() {
        if (!sessionActive || !capabilityPassed || loadedVariants.isEmpty()) return
        if (loadJob?.isActive == true || refreshJob?.isActive == true) return
        val canRefresh = when (val state = _uiState.value) {
            is ArTryOnUiState.Loading -> !state.isSaving
            is ArTryOnUiState.Searching -> !state.isSaving
            is ArTryOnUiState.Tracking -> !state.isSaving
            else -> false
        }
        if (!canRefresh) return

        refreshJob = viewModelScope.launch {
            val result = try {
                frameRepository.getFrame(frameId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure<Frame>(error)
            }
            if (!sessionActive || !isActive) return@launch
            result.onSuccess(::reconcileSavedState)
        }
    }

    private fun reconcileSavedState(frame: Frame) {
        val refreshedVariants = frame.variants
        if (refreshedVariants.isEmpty()) return

        loadedFrameName = frame.name
        val previousSelectedId = selectedVariant?.id
        loadedVariants = refreshedVariants
        val reconciledSelected = refreshedVariants.firstOrNull { it.id == previousSelectedId }
            ?: refreshedVariants.first()
        val selectionChanged = reconciledSelected.id != previousSelectedId
        selectedVariant = reconciledSelected

        _uiState.value = when (val current = _uiState.value) {
            is ArTryOnUiState.Loading -> current.copy(
                variants = refreshedVariants,
                selectedVariant = reconciledSelected,
            )
            is ArTryOnUiState.Searching -> current.copy(
                variants = refreshedVariants,
                selectedVariant = reconciledSelected,
            )
            is ArTryOnUiState.Tracking -> current.copy(
                variants = refreshedVariants,
                selectedVariant = reconciledSelected,
            )
            else -> return
        }

        if (selectionChanged) loadAssetForVariant(reconciledSelected)
    }

    fun retry() {
        if (!sessionActive || _uiState.value !is ArTryOnUiState.Error) return

        clearTracking()
        hasTrackedThisSession = false
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
        loadedFrameName = null
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
                    loadedFrameName = frame.name
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
                    // A remote asset can carry a different pose calibration. Face callbacks may
                    // have arrived while it was downloading, so do not reuse that pose for the
                    // newly calibrated model; the next trusted sample must establish its baseline.
                    faceDistanceScaleTracker.reset()
                    poseStabilizer.reset()
                    latestPose = null
                    resetTrackingQuality()
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

        val (isSaving, saveError, saveMessage) = when (val current = _uiState.value) {
            is ArTryOnUiState.Loading -> Triple(current.isSaving, current.saveError, current.saveMessage)
            is ArTryOnUiState.Searching -> Triple(current.isSaving, current.saveError, current.saveMessage)
            is ArTryOnUiState.Tracking -> Triple(current.isSaving, current.saveError, current.saveMessage)
            else -> Triple(false, null, null)
        }
        _uiState.value = latestFace?.let { face ->
            hasTrackedThisSession = true
            ArTryOnUiState.Tracking(
                variants = loadedVariants,
                selectedVariant = selectedVariant,
                face = face,
                pose = latestPose,
                assetState = assetState,
                trackingQuality = trackingQuality,
                isSaving = isSaving,
                saveError = saveError,
                saveMessage = saveMessage,
                frameName = loadedFrameName,
            )
        } ?: ArTryOnUiState.Searching(
            variants = loadedVariants,
            selectedVariant = selectedVariant,
            assetState = assetState,
            hasTrackedBefore = hasTrackedThisSession,
            isSaving = isSaving,
            saveError = saveError,
            saveMessage = saveMessage,
            frameName = loadedFrameName,
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
        frameName = loadedFrameName,
    )

    private fun clearTracking() {
        poseStabilizer.reset()
        faceDistanceScaleTracker.reset()
        latestFace = null
        latestPose = null
        resetTrackingQuality()
    }

    private fun updateTrackingQuality(candidate: ArTrackingQuality): ArTrackingQuality {
        if (candidate == ArTrackingQuality.Stable) {
            consecutiveUnstableQualitySamples = 0
            if (trackingQuality == ArTrackingQuality.Stable) {
                consecutiveStableQualitySamples = 0
            } else {
                consecutiveStableQualitySamples++
                // FaceDistanceScaleTracker already requires a consecutive trusted baseline before
                // it returns a pose, so the first stable sample can safely start the preview. A
                // later recovery still needs a short run to prevent reappearing-frame flicker.
                if (
                    trackingQuality == ArTrackingQuality.Stabilizing ||
                    consecutiveStableQualitySamples >= QUALITY_RECOVERY_SAMPLE_COUNT
                ) {
                    trackingQuality = ArTrackingQuality.Stable
                }
            }
            return trackingQuality
        }

        consecutiveStableQualitySamples = 0
        consecutiveUnstableQualitySamples++
        if (trackingQuality == ArTrackingQuality.Stable) {
            if (consecutiveUnstableQualitySamples >= QUALITY_LOSS_SAMPLE_COUNT) {
                trackingQuality = if (candidate == ArTrackingQuality.Stabilizing) {
                    ArTrackingQuality.Reacquiring
                } else {
                    candidate
                }
            }
        } else if (trackingQuality == ArTrackingQuality.Stabilizing) {
            // Keep the initial guide generic until a pose has been calibrated. A non-stabilizing
            // candidate here is still useful if a future tracker supplies one before calibration.
            if (candidate != ArTrackingQuality.Stabilizing) trackingQuality = candidate
        } else if (candidate != ArTrackingQuality.Stabilizing) {
            trackingQuality = candidate
        } else {
            trackingQuality = ArTrackingQuality.Reacquiring
        }
        return trackingQuality
    }

    private fun resetTrackingQuality() {
        trackingQuality = ArTrackingQuality.Stabilizing
        consecutiveStableQualitySamples = 0
        consecutiveUnstableQualitySamples = 0
    }

    override fun onCleared() {
        sessionActive = false
        loadJob?.cancel()
        assetLoadJob?.cancel()
        saveJob?.cancel()
        refreshJob?.cancel()
        clearTracking()
        super.onCleared()
    }
}
