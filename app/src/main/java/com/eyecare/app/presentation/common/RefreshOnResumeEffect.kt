package com.eyecare.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Skips the first resume event because each destination's ViewModel performs its initial load.
 * Subsequent resume events represent a return from another destination or the background.
 */
internal class ResumeRefreshObserver(
    private val onRefresh: () -> Unit,
    private val skipInitialResume: Boolean = true,
) : LifecycleEventObserver {
    private var hasObservedInitialResume = false

    fun markInitialResumeObserved() {
        hasObservedInitialResume = true
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event != Lifecycle.Event.ON_RESUME) return
        if (!skipInitialResume || hasObservedInitialResume) {
            onRefresh()
        } else {
            hasObservedInitialResume = true
        }
    }
}

@Composable
fun RefreshOnResumeEffect(
    onRefresh: () -> Unit,
    skipInitialResume: Boolean = true,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnRefresh = rememberUpdatedState(onRefresh)
    DisposableEffect(lifecycleOwner) {
        val observer = ResumeRefreshObserver(
            onRefresh = { currentOnRefresh.value() },
            skipInitialResume = skipInitialResume,
        )
        lifecycleOwner.lifecycle.addObserver(observer)
        // If the observer is installed after the destination is already RESUMED, the lifecycle
        // may not dispatch a catch-up ON_RESUME. Mark that state so the next ON_RESUME is treated
        // as a return. When a catch-up event is dispatched, it is skipped first and this remains
        // harmless.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            observer.markInitialResumeObserved()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
