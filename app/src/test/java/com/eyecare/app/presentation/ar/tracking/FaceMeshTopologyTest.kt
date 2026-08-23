package com.eyecare.app.presentation.ar.tracking

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FaceMeshTopologyTest {

    @Test
    fun `closed directed cycle becomes one triangle`() {
        val topology = FaceMeshTopology.fromConnections(
            connections = listOf(
                FaceMeshConnection(0, 1),
                FaceMeshConnection(1, 2),
                FaceMeshConnection(2, 0),
            ),
            landmarkCount = 3,
        )

        assertEquals(1, topology?.triangleCount)
        assertArrayEquals(intArrayOf(0, 1, 2), topology?.triangleIndices())
    }

    @Test
    fun `shared edges produce deterministic triangles`() {
        val connections = listOf(
            FaceMeshConnection(0, 1),
            FaceMeshConnection(1, 2),
            FaceMeshConnection(2, 0),
            FaceMeshConnection(1, 3),
            FaceMeshConnection(3, 0),
            FaceMeshConnection(0, 1),
        )

        val topology = FaceMeshTopology.fromConnections(connections.shuffled(), landmarkCount = 4)

        assertArrayEquals(intArrayOf(0, 1, 2, 0, 1, 3), topology?.triangleIndices())
    }

    @Test
    fun `cyclic representations do not duplicate a face`() {
        val topology = FaceMeshTopology.fromConnections(
            connections = listOf(
                FaceMeshConnection(0, 1),
                FaceMeshConnection(1, 2),
                FaceMeshConnection(2, 0),
                FaceMeshConnection(1, 2),
                FaceMeshConnection(2, 0),
                FaceMeshConnection(0, 1),
            ),
            landmarkCount = 3,
        )

        assertEquals(1, topology?.triangleCount)
    }

    @Test
    fun `opposite direction remains a distinct directed face`() {
        val topology = FaceMeshTopology.fromConnections(
            connections = listOf(
                FaceMeshConnection(0, 1),
                FaceMeshConnection(1, 2),
                FaceMeshConnection(2, 0),
                FaceMeshConnection(0, 2),
                FaceMeshConnection(2, 1),
                FaceMeshConnection(1, 0),
            ),
            landmarkCount = 3,
        )

        assertArrayEquals(intArrayOf(0, 1, 2, 0, 2, 1), topology?.triangleIndices())
    }

    @Test
    fun `incomplete cycles produce no topology`() {
        val topology = FaceMeshTopology.fromConnections(
            connections = listOf(
                FaceMeshConnection(0, 1),
                FaceMeshConnection(1, 2),
            ),
            landmarkCount = 3,
        )

        assertNull(topology)
    }

    @Test
    fun `invalid indices fail closed`() {
        val topology = FaceMeshTopology.fromConnections(
            connections = listOf(
                FaceMeshConnection(0, 1),
                FaceMeshConnection(1, 4),
                FaceMeshConnection(4, 0),
            ),
            landmarkCount = 4,
        )

        assertNull(topology)
    }

    @Test
    fun `expected triangle count is an invariant`() {
        val connections = listOf(
            FaceMeshConnection(0, 1),
            FaceMeshConnection(1, 2),
            FaceMeshConnection(2, 0),
        )

        assertNull(
            FaceMeshTopology.fromConnections(
                connections = connections,
                landmarkCount = 3,
                expectedTriangleCount = 2,
            )
        )
        assertEquals(
            1,
            FaceMeshTopology.fromConnections(
                connections = connections,
                landmarkCount = 3,
                expectedTriangleCount = 1,
            )?.triangleCount,
        )
    }
}
