package app.luma.ui

import SettingsTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.luma.BuildConfig
import app.luma.MainViewModel
import app.luma.R
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.databinding.FragmentSettingsBinding
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.SettingsComposable.ToggleTextButton
import isDarkTheme

class SettingsFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        binding.testView.setContent {
            SettingsTheme(isDarkTheme(prefs)) {
                Settings()
            }
        }
    }

    @Composable
    private fun Settings() {
        Column {
            SettingsHeader(
                title =
                    "Luma Settings (" + requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName + ")",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showHiddenApps() {
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf("flag" to AppDrawerFlag.HiddenApps.toString()),
        )
    }
}
