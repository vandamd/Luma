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
import androidx.core.os.bundleOf
import app.luma.R
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SelectorButton
import app.luma.ui.compose.CustomScrollView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp

class PagesFragment : Fragment() {

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
                PagesScreen()
            }
        }
        return compose
    }

    @Composable
    fun PagesScreen() {
        Column {
            SettingsHeader(
                title = "Pages",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )

            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    SelectorButton(
                        label = "Page Indicator Position",
                        value = prefs.pageIndicatorPosition.name,
                        onClick = { findNavController().navigate(R.id.action_pagesFragment_to_pageIndicatorPositionFragment) }
                    )
                    SelectorButton(
                        label = "Number of Pages",
                        value = "${prefs.homePages.coerceIn(1,3)} Pages",
                        onClick = { findNavController().navigate(R.id.action_pagesFragment_to_pageCountFragment) }
                    )
                    for (i in 1..prefs.homePages) {
                        val page = i
                        SelectorButton(
                            label = "Page $i, Number of Apps",
                            value = "${prefs.getAppsPerPage(i)} Apps",
                            onClick = { 
                                val bundle = bundleOf("pageNumber" to page)
                                findNavController().navigate(R.id.action_pagesFragment_to_appCountFragment, bundle)
                            }
                        )
                    }
                }
            }
        }
    }
}
