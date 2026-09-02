package com.eyecare.app.presentation.ar.rendering

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TempleVisibilityPolicyTest {

    @Test
    fun `frontal fallback hides both temples`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.None, policy.update(0f))
        assertEquals(TempleVisibility.None, policy.update(20f))
        assertEquals(TempleVisibility.Both, policy.update(27f))
    }

    @Test
    fun `positive yaw hides the left temple after the hide threshold`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.Both, policy.update(31.9f))
        assertEquals(TempleVisibility.LeftOnly, policy.update(32f))
    }

    @Test
    fun `negative yaw hides the right temple after the hide threshold`() {
        val policy = TempleVisibilityPolicy()

        assertEquals(TempleVisibility.Both, policy.update(-31.9f))
        assertEquals(TempleVisibility.RightOnly, policy.update(-32f))
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
}
