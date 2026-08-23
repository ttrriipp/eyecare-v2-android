package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceMeshLandmarks

/** One directed edge from MediaPipe's face-mesh connection graph. */
internal data class FaceMeshConnection(
    val start: Int,
    val end: Int,
)

/**
 * Immutable triangle index buffer for one face mesh.
 *
 * The index order preserves the direction of the source graph. Callers get a
 * copy of the buffer so the topology can be safely shared by renderer nodes.
 */
internal class FaceMeshTopology private constructor(
    private val indices: IntArray,
) {

    val triangleCount: Int
        get() = indices.size / INDICES_PER_TRIANGLE

    fun triangleIndices(): IntArray = indices.copyOf()

    companion object {
        private const val INDICES_PER_TRIANGLE = 3

        /**
         * Derives faces from closed directed cycles `a -> b -> c -> a`.
         *
         * A null result is deliberately fail-closed: malformed graph data,
         * invalid indices, or a changed expected triangle count must never
         * reach a renderer as partial geometry.
         */
        fun fromConnections(
            connections: Iterable<FaceMeshConnection>,
            landmarkCount: Int = FaceMeshLandmarks.LANDMARK_COUNT,
            expectedTriangleCount: Int? = null,
        ): FaceMeshTopology? {
            if (landmarkCount <= 0 ||
                (expectedTriangleCount != null && expectedTriangleCount < 0)
            ) {
                return null
            }

            val outgoing = Array(landmarkCount) { mutableSetOf<Int>() }
            var connectionCount = 0
            for (connection in connections) {
                if (connection.start !in 0 until landmarkCount ||
                    connection.end !in 0 until landmarkCount ||
                    connection.start == connection.end
                ) {
                    return null
                }
                outgoing[connection.start].add(connection.end)
                connectionCount++
            }
            if (connectionCount == 0) return null

            val triangles = mutableSetOf<DirectedTriangle>()
            for (a in 0 until landmarkCount) {
                for (b in outgoing[a].sorted()) {
                    for (c in outgoing[b].sorted()) {
                        if (c != a && outgoing[c].contains(a)) {
                            triangles += canonicalRotation(DirectedTriangle(a, b, c))
                        }
                    }
                }
            }
            if (triangles.isEmpty()) return null

            val ordered = triangles.sortedWith(
                compareBy<DirectedTriangle> { it.first }
                    .thenBy { it.second }
                    .thenBy { it.third }
            )
            if (expectedTriangleCount != null && ordered.size != expectedTriangleCount) {
                return null
            }

            val flatIndices = IntArray(ordered.size * INDICES_PER_TRIANGLE)
            ordered.forEachIndexed { index, triangle ->
                val offset = index * INDICES_PER_TRIANGLE
                flatIndices[offset] = triangle.first
                flatIndices[offset + 1] = triangle.second
                flatIndices[offset + 2] = triangle.third
            }
            return FaceMeshTopology(flatIndices)
        }

        private fun canonicalRotation(triangle: DirectedTriangle): DirectedTriangle =
            listOf(
                triangle,
                DirectedTriangle(triangle.second, triangle.third, triangle.first),
                DirectedTriangle(triangle.third, triangle.first, triangle.second),
            ).minWithOrNull(
                compareBy<DirectedTriangle> { it.first }
                    .thenBy { it.second }
                    .thenBy { it.third }
            )!!

        private data class DirectedTriangle(
            val first: Int,
            val second: Int,
            val third: Int,
        )
    }
}
