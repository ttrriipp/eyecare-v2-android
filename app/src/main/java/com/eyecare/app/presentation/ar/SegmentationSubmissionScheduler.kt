package com.eyecare.app.presentation.ar

/**
 * Bounds how often the optional head segmenter receives a camera frame.
 * Face-landmark submissions remain full-rate so model pose stays responsive.
 */
internal class SegmentationSubmissionScheduler(
    private val minIntervalMs: Long = HeadSegmenterConfig.MIN_SUBMISSION_INTERVAL_MS,
) {

    init {
        require(minIntervalMs in 1L..MAX_INTERVAL_MS) {
            "Segmentation interval must be between 1 and $MAX_INTERVAL_MS ms"
        }
    }

    private var lastSubmissionTimestampMs = Long.MIN_VALUE

    fun shouldSubmit(timestampMs: Long): Boolean {
        if (timestampMs < 0L) return false

        val previousTimestampMs = lastSubmissionTimestampMs
        if (previousTimestampMs != Long.MIN_VALUE) {
            val elapsedMs = timestampMs - previousTimestampMs
            if (elapsedMs < minIntervalMs) return false
        }

        lastSubmissionTimestampMs = timestampMs
        return true
    }

    fun reset() {
        lastSubmissionTimestampMs = Long.MIN_VALUE
    }

    private companion object {
        const val MAX_INTERVAL_MS = 1_000L
    }
}
