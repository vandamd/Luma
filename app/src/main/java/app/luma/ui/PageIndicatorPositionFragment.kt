package app.luma.ui

import SettingsTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.CustomScrollView

class PageIndicatorPositionFragment : Fragment() {

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
                Screen()
            }
        }
        return compose
    }

    @Composable
    fun Screen() {
        Column {
            SettingsHeader(
                title = "Page Indicator Position",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )
            ContentContainer {
                CustomScrollView {
                    SimpleTextButton(
                        title = "Left",
                        underline = prefs.pageIndicatorPosition == Prefs.PageIndicatorPosition.Left,
                        onClick = {
                            prefs.pageIndicatorPosition = Prefs.PageIndicatorPosition.Left
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    )
                    SimpleTextButton(
                        title = "Right",
                        underline = prefs.pageIndicatorPosition == Prefs.PageIndicatorPosition.Right,
                        onClick = {
                            prefs.pageIndicatorPosition = Prefs.PageIndicatorPosition.Right
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    )
                }
            }
        }
    }
}
