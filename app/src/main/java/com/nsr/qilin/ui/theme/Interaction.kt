package com.nsr.qilin.ui.theme

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

object Haptics {
    private var vibrator: Vibrator? = null
    private var lastAt = 0L
    private var lastKey = ""

    var enabled: Boolean = true

    fun init(context: Context) {
        if (vibrator != null) return
        val app = context.applicationContext
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun play(key: String, debounceMs: Long, durationMs: Long, amplitude: Int) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val now = SystemClock.uptimeMillis()
        if (key == lastKey && now - lastAt < debounceMs) return
        lastAt = now
        lastKey = key
        val effect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            } else {
                v.vibrate(effect)
            }
        }
    }

    fun tick() = play("tick", 32L, 16L, 200)

    fun soft() = play("soft", 30L, 10L, 140)

    fun confirm() = play("confirm", 60L, 20L, 235)

    fun impact() = play("impact", 80L, 26L, 255)

    fun toggle(on: Boolean) = play("toggle", 80L, 18L, if (on) 235 else 165)

    fun reject() = play("reject", 120L, 28L, 240)

    fun success() = play("success", 160L, 22L, 230)
}

fun Modifier.hapticOnTap(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this
    pointerInput(Unit) {
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val downPosition = down.position
            var moved = false
            var released = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if ((change.position - downPosition).getDistance() > touchSlop) moved = true
                if (!change.pressed) {
                    released = true
                    break
                }
            }
            if (released && !moved) Haptics.tick()
        }
    }
}

object RevealMemory {
    private val seen = mutableSetOf<String>()

    fun claim(key: String): Boolean = synchronized(seen) { seen.add(key) }
}

@Composable
fun RevealOnEnter(
    index: Int = 0,
    modifier: Modifier = Modifier,
    distance: Dp = 26.dp,
    scaleFrom: Float = 0.965f,
    onceKey: String? = null,
    content: @Composable () -> Unit
) {
    val shouldAnimate = remember(onceKey) {
        onceKey == null || RevealMemory.claim(onceKey)
    }
    if (!shouldAnimate) {
        Box(modifier = modifier) { content() }
        return
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceIn(0, 8) * 55L)
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
        label = "revealOnEnter"
    )
    val distancePx = with(LocalDensity.current) { distance.toPx() }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * distancePx
            val s = scaleFrom + (1f - scaleFrom) * progress
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}

@Composable
fun rememberAnimatedCount(target: Int, durationMillis: Int = 900): Int {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animatable.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = ColorOSMotion.EmphasizedDecelerate
            )
        )
    }
    return animatable.value.roundToInt()
}
