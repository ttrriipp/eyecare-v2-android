package com.eyecare.app.presentation.ar.rendering

import com.eyecare.app.presentation.ar.model.FaceMeshLandmarks
import com.eyecare.app.presentation.ar.tracking.FaceMeshTopology
import com.eyecare.app.presentation.ar.tracking.FaceOccluderGeometry
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.View
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.GeometryNode
import io.github.sceneview.safeDestroyGeometry

/**
 * One reusable invisible depth-only face surface.
 *
 * The node, material, vertex buffer, and index buffer are created once. Face
 * results replace only vertex positions; invalid input hides the node so no
 * stale depth remains in the frame.
 */
internal class FaceOccluderNode private constructor(
    engine: Engine,
    geometry: Geometry,
    materialInstance: MaterialInstance,
    private val triangleIndices: List<List<Int>>,
    private val projectionAdapter: FaceOccluderProjectionAdapter,
) : GeometryNode(
    engine = engine,
    geometry = geometry,
    materialInstance = materialInstance,
    destroyMaterialsOnDispose = true,
) {

    init {
        isVisible = false
    }

    /** Returns true when the node was updated and made depth-active. */
    fun update(
        geometry: FaceOccluderGeometry?,
        view: View?,
        referencePlaneZ: Float,
        worldDepthScale: Float = FaceOccluderProjectionAdapter.DEFAULT_WORLD_DEPTH_SCALE,
    ): Boolean {
        val vertices = geometry?.let {
            projectionAdapter.project(
                geometry = it,
                view = view,
                referencePlaneZ = referencePlaneZ,
                worldDepthScale = worldDepthScale,
            )
        }
        if (vertices == null || vertices.size != FaceMeshLandmarks.LANDMARK_COUNT) {
            isVisible = false
            return false
        }

        updateGeometry(vertices = vertices, indices = triangleIndices)
        isVisible = true
        return true
    }

    fun hide() {
        isVisible = false
    }

    companion object {
        /** Creates the node only when the fixed topology and GPU resources are valid. */
        fun create(
            engine: Engine,
            materialLoader: MaterialLoader,
            topology: FaceMeshTopology,
            projectionAdapter: FaceOccluderProjectionAdapter = FaceOccluderProjectionAdapter(),
        ): FaceOccluderNode? {
            val indices = topology.triangleIndices().toList()
            if (indices.isEmpty() || indices.size % 3 != 0) return null

            val placeholderVertices = List(FaceMeshLandmarks.LANDMARK_COUNT) {
                Geometry.Vertex(position = Position())
            }
            val geometry = try {
                Geometry.Builder()
                    .vertices(placeholderVertices)
                    .indices(indices)
                    .build(engine)
            } catch (_: Exception) {
                return null
            }
            val materialInstance = try {
                materialLoader.createOcclusionInstance()
            } catch (_: Exception) {
                engine.safeDestroyGeometry(geometry)
                return null
            }

            return FaceOccluderNode(
                engine = engine,
                geometry = geometry,
                materialInstance = materialInstance,
                triangleIndices = listOf(indices),
                projectionAdapter = projectionAdapter,
            )
        }
    }
}
