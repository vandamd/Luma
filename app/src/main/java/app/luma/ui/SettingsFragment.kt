package app.luma.ui

import SettingsTheme
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
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
import app.luma.data.Constants
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Constants.Theme.*
import app.luma.data.Prefs
import app.luma.databinding.FragmentSettingsBinding
import app.luma.helper.*
import app.luma.listener.DeviceAdmin
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.ToggleTextButton

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.testView.setContent {

            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkTheme()
            }

            SettingsTheme(isDark) {
                Settings()
            }
        }
    }

    @Composable
    private fun Settings() {
        Column {
            SettingsHeader(
                title = "Luma Settings (" + requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName + ")",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )
            val isDark = when (prefs.appTheme) {
                Light -> false
                Dark -> true
                System -> isSystemInDarkTheme()
            }
            val themeState = remember { mutableStateOf(!isDark) }
            
            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(40.dp)) {
                    ToggleTextButton(
                        title = "Invert Colours",
                        checked = themeState.value,
                        onCheckedChange = {
                            themeState.value = it
                            setTheme(if (it) Light else Dark)
                        },
                        onClick = {
                            themeState.value = !themeState.value
                            setTheme(if (themeState.value) Light else Dark)
                        }
                    )
                    SimpleTextButton("Pages") { findNavController().navigate(R.id.action_settingsFragment_to_pagesFragment) }
                    SimpleTextButton("Gestures") { findNavController().navigate(R.id.action_settingsFragment_to_gesturesFragment) }
                    SimpleTextButton("Notifications") { findNavController().navigate(R.id.action_settingsFragment_to_notificationsFragment) }
                    SimpleTextButton("Hidden Apps") { showHiddenApps() }
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.isLumaDefault()

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showHiddenApps() {
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf("flag" to AppDrawerFlag.HiddenApps.toString())
        )
    }

    private fun checkAdminPermission() {
        deviceManager.isAdminActive(componentName)
    }

    private fun setTheme(appTheme: Constants.Theme) {
        prefs.appTheme = appTheme
        requireActivity().recreate()
    }
}
