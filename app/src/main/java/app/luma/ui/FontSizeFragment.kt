package app.luma.ui

import SettingsTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import app.luma.data.Prefs
import app.luma.style.FontSizeOption
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton

class FontSizeFragment : Fragment() {

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
                FontSizeScreen()
            }
        }
        return compose
    }

    @Composable
    private fun FontSizeScreen() {
        val selected = remember { mutableStateOf(prefs.fontSizeOption) }
        Column {
            SettingsHeader(
                title = "Font Size",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )
            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    FontSizeOption.values().forEach { option ->
                        val isSelected = option == selected.value
                        SimpleTextButton(
                            title = option.title,
                            underline = isSelected
                        ) {
                            if (!isSelected) {
                                selected.value = option
                                prefs.fontSizeOption = option
                                requireActivity().recreate()
                            }
                        }
                    }
                }
            }
        }
    }
}
