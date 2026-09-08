package com.eyecare.app.presentation.ar

/** Constants for the on-device person segmentation spike. */
internal object HeadSegmenterConfig {
    const val MODEL_ASSET = "selfie_segmenter.tflite"
    const val USE_CONFIDENCE_MASK = true
    const val PERSON_CATEGORY_INDEX = 1

    /** Keep the mask proof layer opt-in; it is not part of the normal AR preview. */
    const val SHOW_DEBUG_OVERLAY = false
}
