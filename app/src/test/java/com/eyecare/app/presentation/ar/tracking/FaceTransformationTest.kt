package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaceTransformationTest {

    @Test
    fun extractsOneFiniteSixteenValueMatrix() {
        val raw = identityMatrix()

        val result = extractSingleFaceTransformationMatrix(listOf(raw))

        assertEquals(FaceTransformationMatrix.MATRIX_ELEMENT_COUNT, result?.values?.size)
        assertEquals(raw.toList(), result?.values)
    }

    @Test
    fun copiesTheSourceArraySoCallbackStateIsImmutable() {
        val raw = identityMatrix()
        val result = extractSingleFaceTransformationMatrix(listOf(raw))
        raw[0] = 42f

        assertEquals(1f, result?.get(0))
    }

    @Test
    fun rejectsMissingOrAmbiguousMatrices() {
        assertNull(extractSingleFaceTransformationMatrix(emptyList()))
        assertNull(extractSingleFaceTransformationMatrix(listOf(identityMatrix(), identityMatrix())))
    }

    @Test
    fun rejectsMalformedOrNonFiniteMatrices() {
        assertNull(extractSingleFaceTransformationMatrix(listOf(FloatArray(15))))

        val nonFinite = identityMatrix().also { it[5] = Float.NaN }
        assertNull(extractSingleFaceTransformationMatrix(listOf(nonFinite)))

        val infinite = identityMatrix().also { it[10] = Float.POSITIVE_INFINITY }
        assertNull(extractSingleFaceTransformationMatrix(listOf(infinite)))
    }

    @Test
    fun validMatrixValuesRemainFinite() {
        val result = extractSingleFaceTransformationMatrix(listOf(identityMatrix()))

        assertTrue(result!!.values.all(Float::isFinite))
    }

    private fun identityMatrix(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    )
}
