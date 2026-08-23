package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceMeshLandmarks
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker

internal const val MEDIAPIPE_FACE_MESH_TRIANGLE_COUNT = 852

/**
 * Lazily derives the topology from the pinned MediaPipe connection set.
 *
 * The lazy value also caches a failed derivation as null, so a framework or
 * topology change cannot cause repeated work on camera frames.
 */
private val mediaPipeFaceMeshTopology: FaceMeshTopology? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    runCatching {
        FaceMeshTopology.fromConnections(
            connections = FaceLandmarker.FACE_LANDMARKS_TESSELATION.map { connection ->
                FaceMeshConnection(connection.start(), connection.end())
            },
            landmarkCount = FaceMeshLandmarks.LANDMARK_COUNT,
            expectedTriangleCount = MEDIAPIPE_FACE_MESH_TRIANGLE_COUNT,
        )
    }.getOrNull()
}

/** Returns the fixed topology once, or null when the pinned invariant fails. */
internal fun loadMediaPipeFaceMeshTopology(): FaceMeshTopology? = mediaPipeFaceMeshTopology
