package app.luma.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
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
        val versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: ""
        Column {
            SettingsHeader(
                title = stringResource(R.string.settings_title, versionName),
                onBack = ::goBack,
            )
            val invertState = remember { mutableStateOf(prefs.invertColours) }

            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // ToggleTextButton(
                    //     title = stringResource(R.string.settings_invert_colours),
                    //     checked = invertState.value,
                    //     onCheckedChange = {
                    //         invertState.value = it
                    //         prefs.invertColours = it
                    //         requireActivity().recreate()
                    //     },
                    //     onClick = {
                    //         invertState.value = !invertState.value
                    //         prefs.invertColours = invertState.value
                    //         requireActivity().recreate()
                    //     },
                    // )
                    SimpleTextButton(
                        stringResource(R.string.settings_pages),
                    ) { findNavController().navigate(R.id.action_settingsFragment_to_pagesFragment) }
                    SimpleTextButton(stringResource(R.string.settings_gestures)) {
                        findNavController().navigate(R.id.action_settingsFragment_to_gesturesFragment)
                    }
                    SimpleTextButton(
                        stringResource(R.string.settings_notifications),
                    ) { findNavController().navigate(R.id.action_settingsFragment_to_notificationsFragment) }
                    SimpleTextButton(stringResource(R.string.settings_hidden_apps)) { showHiddenApps() }
                    SimpleTextButton(stringResource(R.string.settings_default_launcher)) { openDefaultLauncherSettings() }
                }
            }
        }
    }

    private fun openDefaultLauncherSettings() {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
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
