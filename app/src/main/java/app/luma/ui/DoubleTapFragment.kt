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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Constants
import app.luma.data.Constants.Action
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp

class DoubleTapFragment : Fragment() {

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
                DoubleTapScreen()
            }
        }
        return compose
    }

    @Composable
    fun DoubleTapScreen() {
        Column {
            SettingsHeader(
                title = "Double tap",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )
            ContentContainer(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                for (action in Constants.Action.values()) {
                    val isSelected = prefs.doubleTapAction == action
                    val buttonText = when (action) {
                        Constants.Action.OpenApp -> "Open ${prefs.appDoubleTap.appLabel}"
                        else -> action.string()
                    }
                    SimpleTextButton(
                        title = buttonText,
                        underline = isSelected,
                        onClick = { handleActionSelection(action) }
                    )
                }
            }
        }
    }

    private fun handleActionSelection(action: Action) {
        prefs.doubleTapAction = action
        when(action) {
            Action.OpenApp -> {
                findNavController().navigate(
                    R.id.action_doubleTapFragment_to_appListFragment,
                    bundleOf("flag" to AppDrawerFlag.SetDoubleTap.toString())
                )
            }
            else -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
