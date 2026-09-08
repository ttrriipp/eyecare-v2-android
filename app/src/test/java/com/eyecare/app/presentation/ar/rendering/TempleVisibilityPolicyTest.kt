package com.eyecare.app.presentation.ar.rendering

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TempleVisibilityPolicyTest {

    @Test
    fun `frontal fallback hides both temples`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.None, policy.update(0f))
        assertEquals(TempleVisibility.None, policy.update(20f))
        assertEquals(TempleVisibility.Both, policy.update(23f))
    }

    @Test
    fun `positive yaw hides the left temple after the hide threshold`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.Both, policy.update(23.9f))
        assertEquals(TempleVisibility.LeftOnly, policy.update(24f))
    }

    @Test
    fun `positive yaw hides the far temple before the oblique view exposes it`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.LeftOnly, policy.update(24f))
    }

    @Test
    fun `negative yaw hides the right temple after the hide threshold`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.Both, policy.update(-23.9f))
        assertEquals(TempleVisibility.RightOnly, policy.update(-24f))
    }

    @Test
    fun `hidden temple stays hidden until yaw returns below show threshold`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.LeftOnly, policy.update(40f))
        assertEquals(TempleVisibility.LeftOnly, policy.update(25f))
        assertEquals(TempleVisibility.None, policy.update(20f))
    }

    @Test
    fun `missing or invalid yaw restores both temples`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.LeftOnly, policy.update(40f))
        assertEquals(TempleVisibility.Both, policy.update(null))
        assertEquals(TempleVisibility.RightOnly, policy.update(-40f))
        assertEquals(TempleVisibility.Both, policy.update(Float.NaN))
    }

    @Test
    fun `head mask keeps conservative far-temple fallback at oblique yaw`() {
        assertEquals(
            TempleVisibility.LeftOnly,
            TempleVisibility.LeftOnly.withHeadOcclusionSafety(),
        )
        assertEquals(
            TempleVisibility.RightOnly,
            TempleVisibility.RightOnly.withHeadOcclusionSafety(),
        )
    }

    @Test
    fun `head mask restores both temples only for frontal fallback`() {
        assertEquals(
            TempleVisibility.Both,
            TempleVisibility.None.withHeadOcclusionSafety(),
        )
        assertEquals(
            TempleVisibility.Both,
            TempleVisibility.Both.withHeadOcclusionSafety(),
        )
    }
}
