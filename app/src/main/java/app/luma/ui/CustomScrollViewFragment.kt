package app.luma.ui

import SettingsTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import app.luma.data.Constants
import app.luma.data.Prefs
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SimpleTextButton

class CustomScrollViewFragment : Fragment() {
    
    private lateinit var prefs: Prefs
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefs = Prefs(requireContext())
        return ComposeView(requireContext()).apply {
            setContent {
                val isDark = when (prefs.appTheme) {
                    Constants.Theme.Light -> false
                    Constants.Theme.Dark -> true
                    Constants.Theme.System -> isSystemInDarkTheme()
                }
                
                SettingsTheme(isDark) {
                    CustomScrollViewDemo()
                }
            }
        }
    }
    
    @Composable
    fun CustomScrollViewDemo() {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsComposable.SettingsHeader(
                title = "Custom Scroll View Demo",
                onBack = { requireActivity().onBackPressed() }
            )

            ContentContainer {
                CustomScrollView {
                    for (action in Constants.Action.values()) {
                        val isSelected = prefs.doubleTapAction == action
                        val buttonText = when (action) {
                            Constants.Action.OpenApp -> "Open ${prefs.appDoubleTap.appLabel}"
                            else -> action.string()
                        }
                        SimpleTextButton(
                            title = buttonText,
                            underline = isSelected,
                            onClick = { }
                        )
                    }
                }
            }

        }
    }
}
