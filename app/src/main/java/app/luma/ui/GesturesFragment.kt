package app.luma.ui

import SettingsTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Constants
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SelectorButton
import app.luma.ui.compose.CustomScrollView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp

class GesturesFragment : Fragment() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val compose = ComposeView(requireContext())
        compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        compose.setContent {
            val isDark = when (prefs.appTheme) {
                app.luma.data.Constants.Theme.Light -> false
                app.luma.data.Constants.Theme.Dark -> true
                app.luma.data.Constants.Theme.System -> isSystemInDarkTheme()
            }
            SettingsTheme(isDark) {
                GesturesScreen()
            }
        }
        return compose
    }

    @Composable
    fun GesturesScreen() {
        Column {
            SettingsHeader(
                title = "Gestures",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )

            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    SelectorButton(
                        label = "Swipe left",
                        value = getActionText(prefs.swipeLeftAction, prefs.appSwipeLeft.appLabel),
                        onClick = { findNavController().navigate(R.id.action_gesturesFragment_to_swipeLeftFragment) }
                    )
                    SelectorButton(
                        label = "Swipe right",
                        value = getActionText(prefs.swipeRightAction, prefs.appSwipeRight.appLabel),
                        onClick = { findNavController().navigate(R.id.action_gesturesFragment_to_swipeRightFragment) }
                    )
                    SelectorButton(
                        label = "Swipe down",
                        value = getActionText(prefs.swipeDownAction, prefs.appSwipeDown.appLabel),
                        onClick = { findNavController().navigate(R.id.action_gesturesFragment_to_swipeDownFragment) }
                    )
                    SelectorButton(
                        label = "Swipe up",
                        value = getActionText(prefs.swipeUpAction, prefs.appSwipeUp.appLabel),
                        onClick = { findNavController().navigate(R.id.action_gesturesFragment_to_swipeUpFragment) }
                    )
                    SelectorButton(
                        label = "Double tap",
                        value = getActionText(prefs.doubleTapAction, prefs.appDoubleTap.appLabel),
                        onClick = { findNavController().navigate(R.id.action_gesturesFragment_to_doubleTapFragment) }
                    )
                }
            }
        }
    }

    @Composable
    private fun getActionText(action: Constants.Action, appLabel: String): String {
        return when (action) {
            Constants.Action.OpenApp -> "Open $appLabel"
            Constants.Action.Disabled -> "Disabled"
            else -> action.string()
        }
    }
}
