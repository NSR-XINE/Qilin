package com.mydonghua.app.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collect

@Composable
fun ScrollPerfMonitor(listState: LazyListState, tag: String) {
    LaunchedEffect(listState, tag) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) {
                    var last = 0L
                    var maxGap = 0L
                    var frames = 0
                    val t0 = System.nanoTime()
                    while (listState.isScrollInProgress) {
                        val now = withFrameNanos { it }
                        if (last != 0L) maxGap = maxOf(maxGap, now - last)
                        frames++
                        last = now
                    }
                    android.util.Log.d(
                        "ScrollPerf",
                        "$tag scroll frames=$frames maxFrameGapMs=${maxGap / 1_000_000L} durationMs=${(System.nanoTime() - t0) / 1_000_000L}"
                    )
                }
            }
    }
}
