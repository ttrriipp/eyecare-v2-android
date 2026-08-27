package com.eyecare.app.presentation.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RefreshOnResumeEffectTest {

    @Test
    fun `first resume is covered by initial load and later resume refreshes`() {
        var refreshCount = 0
        val observer = ResumeRefreshObserver(onRefresh = { refreshCount++ })
        val owner = mockk<LifecycleOwner>()

        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        observer.onStateChanged(owner, Lifecycle.Event.ON_PAUSE)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)

        assertEquals(1, refreshCount)
    }

    @Test
    fun `can delegate the initial resume to a viewmodel with its own guard`() {
        var refreshCount = 0
        val observer = ResumeRefreshObserver(
            onRefresh = { refreshCount++ },
            skipInitialResume = false,
        )
        val owner = mockk<LifecycleOwner>()

        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        observer.onStateChanged(owner, Lifecycle.Event.ON_PAUSE)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)

        assertEquals(2, refreshCount)
    }
}
