package app.luma.ui

import SettingsTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import isDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.CustomScrollView

class PageCountFragment : Fragment() {

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
            SettingsTheme(isDarkTheme(prefs)) {
                PageCountScreen()
            }
        }
        return compose
    }

    @Composable
    fun PageCountScreen() {
        Column {
            SettingsHeader(
                title = "Number of Pages",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )

            ContentContainer {
                CustomScrollView {
                    for (i in 1..5) {
                        val isSelected = prefs.homePages == i
                        SimpleTextButton(
                            title = "$i Page${if (i > 1) "s" else ""}",
                            underline = isSelected,
                            onClick = { updateHomePages(i) }
                        )
                    }
                }
            }
        }
    }

    private fun updateHomePages(homePages: Int) {
        prefs.homePages = homePages
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }
}
