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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
