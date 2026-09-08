package com.eyecare.app.presentation.ar.rendering

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.View
import com.eyecare.app.presentation.ar.model.HeadOcclusionMask
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.GeometryNode
import io.github.sceneview.safeDestroyGeometry
import io.github.sceneview.utils.screenToRay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Reusable depth-only screen mesh for the side-head portion of a segmentation mask.
 *
 * The node writes depth but no colour, so the camera remains visible while rear
 * temple fragments fail the depth test. Geometry and material ownership stays
 * inside this adapter; each mask update replaces vertex/index data only.
 */
internal class HeadOcclusionNode private constructor(
    engine: Engine,
    geometry: Geometry,
    materialInstance: MaterialInstance,
    private val triangleIndices: List<Int>,
) : GeometryNode(
    engine = engine,
    geometry = geometry,
    materialInstance = materialInstance,
    destroyMaterialsOnDispose = true,
) {

    init {
        isVisible = false
    }

    /** Returns true when at least one valid side-mask cell is depth-active. */
    fun update(
        mask: HeadOcclusionMask?,
        view: View?,
        referencePlaneZ: Float,
    ): Boolean {
        if (
            mask == null ||
            view == null ||
            !referencePlaneZ.isFinite() ||
            view.viewport.width <= 0 ||
            view.viewport.height <= 0
        ) {
            isVisible = false
            return false
        }

        // Filament vertex/index buffer capacities are fixed at build time. Keep
        // one bounded 64x64-cell topology and collapse inactive cells to
        // degenerate triangles instead of resizing GPU buffers per result.
        // Keep one permanent degenerate sentinel quad in the buffer. Filament
        // rejects an empty AABB, including the valid-but-empty case where all
        // projected cells happen to collapse to the same point.
        val vertices = placeholderVertices().toMutableList()
        var projectedCellCount = 0
        val stepX = max(1, ceil(mask.imageWidth / MAX_GRID_SIZE.toFloat()).toInt())
        val stepY = max(1, ceil(mask.imageHeight / MAX_GRID_SIZE.toFloat()).toInt())
        val cellCountX = ceil(mask.imageWidth / stepX.toFloat()).toInt().coerceAtMost(MAX_GRID_SIZE)
        val cellCountY = ceil(mask.imageHeight / stepY.toFloat()).toInt().coerceAtMost(MAX_GRID_SIZE)
        for (cellY in 0 until cellCountY) {
            val y = cellY * stepY
            val endY = min(mask.imageHeight - 1, y + stepY - 1)
            for (cellX in 0 until cellCountX) {
                val x = cellX * stepX
                val endX = min(mask.imageWidth - 1, x + stepX - 1)
                if (hasActivePixel(mask, x, y, endX, endY)) {
                    val rect = sourceCellToViewport(mask, x, y, endX, endY, view)
                    if (rect != null) {
                        val startIndex = (cellY * MAX_GRID_SIZE + cellX) * VERTICES_PER_CELL
                        val corners = listOf(
                            ScreenPoint(rect.left, rect.top),
                            ScreenPoint(rect.right, rect.top),
                            ScreenPoint(rect.right, rect.bottom),
                            ScreenPoint(rect.left, rect.bottom),
                        ).map { point ->
                            project(point, view, referencePlaneZ)
                        }
                        if (corners.all { it != null }) {
                            corners.filterNotNull().forEachIndexed { index, position ->
                                vertices[startIndex + index] = Geometry.Vertex(position = position)
                            }
                            projectedCellCount++
                        }
                    }
                }
            }
        }

        if (projectedCellCount == 0) {
            isVisible = false
            return false
        }

        updateGeometry(
            vertices = vertices,
            indices = listOf(triangleIndices),
        )
        isVisible = true
        return true
    }

    fun hide() {
        isVisible = false
    }

    private fun hasActivePixel(
        mask: HeadOcclusionMask,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
    ): Boolean {
        val sampleStepX = max(1, (endX - startX + 1) / CELL_SAMPLE_GRID)
        val sampleStepY = max(1, (endY - startY + 1) / CELL_SAMPLE_GRID)
        var y = startY
        while (y <= endY) {
            var x = startX
            while (x <= endX) {
                if (mask.isActive(x, y)) return true
                x += sampleStepX
            }
            y += sampleStepY
        }
        return mask.isActive(endX, endY)
    }

    private fun sourceCellToViewport(
        mask: HeadOcclusionMask,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        view: View,
    ): ScreenRect? {
        val halfPixel = mask.fillScale * 0.5f
        val x0 = mask.viewportXForSourcePixel(startX)
        val x1 = mask.viewportXForSourcePixel(endX)
        val y0 = mask.viewportYForSourcePixel(startY)
        val y1 = mask.viewportYForSourcePixel(endY)
        val left = (min(x0, x1) - halfPixel).coerceIn(0f, view.viewport.width.toFloat())
        val right = (max(x0, x1) + halfPixel).coerceIn(0f, view.viewport.width.toFloat())
        val top = (min(y0, y1) - halfPixel).coerceIn(0f, view.viewport.height.toFloat())
        val bottom = (max(y0, y1) + halfPixel).coerceIn(0f, view.viewport.height.toFloat())
        return if (
            left.isFinite() && right.isFinite() && top.isFinite() && bottom.isFinite() &&
            right - left > MINIMUM_RECT_SIZE && bottom - top > MINIMUM_RECT_SIZE
        ) {
            ScreenRect(left, top, right, bottom)
        } else {
            null
        }
    }

    private fun project(
        point: ScreenPoint,
        view: View,
        referencePlaneZ: Float,
    ): Position? {
        val ray = runCatching {
            view.screenToRay(point.x, point.y)
        }.getOrNull() ?: return null
        val directionZ = ray.direction.z
        if (!directionZ.isFinite() || abs(directionZ) <= MINIMUM_RAY_DEPTH) return null

        val distance = (referencePlaneZ - ray.origin.z) / directionZ
        if (!distance.isFinite() || distance <= MINIMUM_RAY_DEPTH) return null

        val x = ray.origin.x + ray.direction.x * distance
        val y = ray.origin.y + ray.direction.y * distance
        val z = ray.origin.z + ray.direction.z * distance
        return if (x.isFinite() && y.isFinite() && z.isFinite()) {
            Position(x = x, y = y, z = z)
        } else {
            null
        }
    }

    private data class ScreenPoint(val x: Float, val y: Float)

    private data class ScreenRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    )

    companion object {
        private const val MAX_GRID_SIZE = 64
        private const val VERTICES_PER_CELL = 4
        private const val CELL_VERTEX_COUNT = MAX_GRID_SIZE * MAX_GRID_SIZE * VERTICES_PER_CELL
        private const val SENTINEL_VERTEX_OFFSET = CELL_VERTEX_COUNT
        private const val MAX_VERTEX_COUNT = CELL_VERTEX_COUNT + VERTICES_PER_CELL
        private const val CELL_SAMPLE_GRID = 4
        private const val MINIMUM_RECT_SIZE = 0.25f
        private const val MINIMUM_RAY_DEPTH = 0.0001f

        /**
         * Builds the fixed-capacity vertex list used both at creation and on
         * every update. The last four vertices form a tiny quad behind the AR
         * camera. It is never visible, but gives Filament a non-zero extent on
         * all three axes (a flat/collinear sentinel is still an empty AABB).
         */
        private fun placeholderVertices(): List<Geometry.Vertex> = buildList(MAX_VERTEX_COUNT) {
            repeat(CELL_VERTEX_COUNT) {
                add(Geometry.Vertex(position = Position()))
            }
            add(Geometry.Vertex(position = Position(x = 0f, y = 0f, z = SENTINEL_DEPTH)))
            add(Geometry.Vertex(position = Position(x = SENTINEL_EXTENT, y = 0f, z = SENTINEL_DEPTH)))
            add(Geometry.Vertex(position = Position(x = SENTINEL_EXTENT, y = SENTINEL_EXTENT, z = SENTINEL_DEPTH)))
            add(Geometry.Vertex(position = Position(x = 0f, y = SENTINEL_EXTENT, z = SENTINEL_DEPTH)))
        }

        private const val SENTINEL_EXTENT = 0.001f
        private const val SENTINEL_DEPTH = 1f

        private fun triangleIndices(): List<Int> = buildList(MAX_GRID_SIZE * MAX_GRID_SIZE * 6 + 6) {
            repeat(MAX_GRID_SIZE * MAX_GRID_SIZE) { cellIndex ->
                val startIndex = cellIndex * VERTICES_PER_CELL
                add(startIndex)
                add(startIndex + 1)
                add(startIndex + 2)
                add(startIndex)
                add(startIndex + 2)
                add(startIndex + 3)
            }
            add(SENTINEL_VERTEX_OFFSET)
            add(SENTINEL_VERTEX_OFFSET + 1)
            add(SENTINEL_VERTEX_OFFSET + 2)
            add(SENTINEL_VERTEX_OFFSET)
            add(SENTINEL_VERTEX_OFFSET + 2)
            add(SENTINEL_VERTEX_OFFSET + 3)
        }

        fun create(
            engine: Engine,
            materialLoader: MaterialLoader,
        ): HeadOcclusionNode? {
            val triangleIndices = triangleIndices()
            val placeholderVertices = placeholderVertices()
            val geometry = try {
                Geometry.Builder()
                    .vertices(placeholderVertices)
                    .indices(triangleIndices)
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
            return HeadOcclusionNode(
                engine = engine,
                geometry = geometry,
                materialInstance = materialInstance,
                triangleIndices = triangleIndices,
            )
        }
    }
}
