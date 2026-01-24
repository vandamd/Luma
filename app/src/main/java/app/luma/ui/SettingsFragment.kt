package app.luma.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.luma.MainViewModel
import app.luma.R
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.SettingsComposable.ToggleTextButton

class SettingsFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { Settings() }

    @Composable
    private fun Settings() {
        Column {
            SettingsHeader(
                title =
                    "Luma Settings (" + requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName + ")",
                onBack = ::goBack,
            )
            val invertState = remember { mutableStateOf(prefs.invertColours) }

            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(40.dp)) {
                    ToggleTextButton(
                        title = "Invert Colours",
                        checked = invertState.value,
                        onCheckedChange = {
                            invertState.value = it
                            prefs.invertColours = it
                            requireActivity().recreate()
                        },
                        onClick = {
                            invertState.value = !invertState.value
                            prefs.invertColours = invertState.value
                            requireActivity().recreate()
                        },
                    )
                    SimpleTextButton("Pages") { findNavController().navigate(R.id.action_settingsFragment_to_pagesFragment) }
                    SimpleTextButton("Gestures") { findNavController().navigate(R.id.action_settingsFragment_to_gesturesFragment) }
                    SimpleTextButton(
                        "Notifications",
                    ) { findNavController().navigate(R.id.action_settingsFragment_to_notificationsFragment) }
                    SimpleTextButton("Hidden Apps") { showHiddenApps() }
                }
            }
        }
    }

    private fun showHiddenApps() {
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.appListFragment,
            bundleOf("flag" to AppDrawerFlag.HiddenApps.toString()),
        )
    }
}
