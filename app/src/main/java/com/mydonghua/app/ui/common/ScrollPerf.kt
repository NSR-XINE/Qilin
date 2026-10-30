package com.mydonghua.app.ui.common

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.mydonghua.app.BuildConfig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

object ScrollGate {
    @Volatile
    var scrolling = false
}

@Composable
fun ScrollPerfMonitor(state: ScrollableState, tag: String) {
    LaunchedEffect(state, tag) {
        snapshotFlow { state.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) {
                    ScrollGate.scrolling = true
                    try {
                        if (BuildConfig.DEBUG) {
                            var last = 0L
                            var maxGap = 0L
                            var frames = 0
                            val t0 = System.nanoTime()
                            while (state.isScrollInProgress) {
                                val now = withFrameNanos { it }
                                if (last != 0L) maxGap = maxOf(maxGap, now - last)
                                frames++
                                last = now
                            }
                            android.util.Log.d(
                                "ScrollPerf",
                                "$tag scroll frames=$frames maxFrameGapMs=${maxGap / 1_000_000L} durationMs=${(System.nanoTime() - t0) / 1_000_000L}"
                            )
                        } else {
                            snapshotFlow { state.isScrollInProgress }
                                .dropWhile { it }
                                .first()
                        }
                    } finally {
                        ScrollGate.scrolling = false
                    }
                }
            }
    }
}
