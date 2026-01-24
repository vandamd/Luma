package app.luma.ui

import SettingsTheme
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import app.luma.data.Prefs
import isDarkTheme

/**
 * Creates a ComposeView with proper lifecycle-aware composition strategy and themed content.
 */
fun Fragment.composeView(content: @Composable () -> Unit): View {
    val prefs = Prefs(requireContext())
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SettingsTheme(isDarkTheme(prefs)) {
                content()
            }
        }
    }
}

/**
 * Navigates back using the activity's back press dispatcher.
 */
fun Fragment.goBack() {
    requireActivity().onBackPressedDispatcher.onBackPressed()
}
