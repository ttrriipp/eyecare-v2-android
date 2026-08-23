package com.eyecare.app.presentation.ar.rendering

import com.eyecare.app.presentation.ar.tracking.FaceOccluderGeometry
import com.google.android.filament.View
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.math.Position
import io.github.sceneview.utils.screenToRay
import kotlin.math.abs

/**
 * Converts mapper screen vertices into world positions using SceneView's
 * camera ray. The mapper remains independent from this Android/GPU boundary.
 */
internal class FaceOccluderProjectionAdapter(
    private val minimumRayDepth: Float = MINIMUM_RAY_DEPTH,
) {

    init {
        require(minimumRayDepth.isFinite() && minimumRayDepth > 0f) {
            "Minimum ray depth must be positive and finite"
        }
    }

    fun project(
        geometry: FaceOccluderGeometry,
        view: View?,
        referencePlaneZ: Float,
        worldDepthScale: Float = DEFAULT_WORLD_DEPTH_SCALE,
    ): List<Geometry.Vertex>? {
        if (
            view == null ||
            !referencePlaneZ.isFinite() ||
            !worldDepthScale.isFinite() ||
            worldDepthScale <= 0f ||
            view.viewport.width <= 0 ||
            view.viewport.height <= 0
        ) {
            return null
        }

        val vertices = ArrayList<Geometry.Vertex>(geometry.vertexCount)
        for (index in 0 until geometry.vertexCount) {
            val ray = runCatching {
                view.screenToRay(geometry.xPx(index), geometry.yPx(index))
            }.getOrNull() ?: return null
            val directionZ = ray.direction.z
            if (!directionZ.isFinite() || abs(directionZ) <= minimumRayDepth) return null

            val planeDistance = (referencePlaneZ - ray.origin.z) / directionZ
            val depthDistance = planeDistance + geometry.depthOffset(index) * worldDepthScale
            if (
                !planeDistance.isFinite() ||
                planeDistance <= minimumRayDepth ||
                !depthDistance.isFinite() ||
                depthDistance <= minimumRayDepth
            ) {
                return null
            }

            val pointX = ray.origin.x + ray.direction.x * depthDistance
            val pointY = ray.origin.y + ray.direction.y * depthDistance
            val pointZ = ray.origin.z + ray.direction.z * depthDistance
            if (!pointX.isFinite() || !pointY.isFinite() || !pointZ.isFinite()) return null
            vertices += Geometry.Vertex(
                position = Position(
                    x = pointX,
                    y = pointY,
                    z = pointZ,
                )
            )
        }
        return vertices
    }

    companion object {
        const val MINIMUM_RAY_DEPTH = 0.0001f
        internal const val DEFAULT_WORLD_DEPTH_SCALE = 0.06f
    }
}
