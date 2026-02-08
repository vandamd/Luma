package app.luma.ui

import android.view.View
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import app.luma.data.Prefs
import app.luma.helper.performHapticFeedback
import app.luma.style.SettingsTheme
import app.luma.style.isDarkTheme
import kotlin.math.abs

fun Fragment.composeView(
    onSwipeBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
): View {
    val prefs = Prefs.getInstance(requireContext())
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SettingsTheme(isDarkTheme(prefs)) {
                if (onSwipeBack != null) {
                    SwipeBackContainer(onSwipeBack) { content() }
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SwipeBackContainer(
    onSwipeBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val currentOnSwipeBack = rememberUpdatedState(onSwipeBack)
    val edgeThresholdPx = with(LocalDensity.current) { 30.dp.toPx() }
    val dragThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.position.x > edgeThresholdPx) return@awaitEachGesture

                        var totalX = 0f
                        var totalY = 0f
                        var committed = false
                        var triggered = false

                        while (!triggered) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break

                            totalX = change.position.x - down.position.x
                            totalY = change.position.y - down.position.y

                            if (!committed && abs(totalY) > abs(totalX) * 1.5f) break

                            if (!committed && abs(totalX) > abs(totalY)) {
                                committed = true
                            }

                            if (committed) change.consume()

                            if (committed && totalX > dragThresholdPx) {
                                triggered = true
                                performHapticFeedback(context)
                                currentOnSwipeBack.value()
                            }
                        }
                    }
                },
    ) {
        content()
    }
}

/**
 * Navigates back using the activity's back press dispatcher.
 */
fun Fragment.goBack() {
    requireActivity().onBackPressedDispatcher.onBackPressed()
}
