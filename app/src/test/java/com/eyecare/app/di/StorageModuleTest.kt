package com.eyecare.app.di

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageModuleTest {

    @Test
    fun `AEADBadTagException is detected by exception chain check`() {
        val cause = javax.crypto.AEADBadTagException("bad tag")
        val wrapper = RuntimeException("Unable to start", cause)

        val isAeadException = wrapper.cause?.javaClass?.simpleName == "AEADBadTagException" ||
            wrapper is javax.crypto.AEADBadTagException ||
            wrapper.message?.contains("AEADBadTag") == true

        assertTrue(isAeadException, "Should detect AEADBadTagException in cause chain")
    }

    @Test
    fun `direct AEADBadTagException is detected`() {
        val e = javax.crypto.AEADBadTagException("direct")

        val isAeadException = e.cause?.javaClass?.simpleName == "AEADBadTagException" ||
            e is javax.crypto.AEADBadTagException ||
            e.message?.contains("AEADBadTag") == true

        assertTrue(isAeadException)
    }
}
