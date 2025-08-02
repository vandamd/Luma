package app.luma.ui

import SettingsTheme
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.luma.BuildConfig
import app.luma.MainViewModel
import app.luma.R
import app.luma.data.Constants
import app.luma.data.Constants.Action
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Constants.Theme.*
import app.luma.data.Prefs
import app.luma.databinding.FragmentSettingsBinding
import app.luma.helper.*
import app.luma.listener.DeviceAdmin
import app.luma.ui.compose.SettingsComposable.SettingsArea
import app.luma.ui.compose.SettingsComposable.SettingsGestureItem
import app.luma.ui.compose.SettingsComposable.SettingsItem
import app.luma.ui.compose.SettingsComposable.SettingsNumberItem
import app.luma.ui.compose.SettingsComposable.SettingsToggle
import app.luma.ui.compose.SettingsComposable.SettingsTopView
import app.luma.ui.compose.SettingsComposable.SimpleTextButton

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val offset = 5

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
        val selected = remember { mutableStateOf("") }

        Column {
            SettingsTopView(
                stringResource(R.string.app_name) + " " + requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName,
                onClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
            ) {
                SimpleTextButton(stringResource(R.string.hidden_apps)) { showHiddenApps() }
            }
            SettingsArea(title = "Customise",
                selected = selected,
                items = arrayOf(
                    { open, onChange ->
                        SettingsItem(
                            title = stringResource(R.string.theme_mode),
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.appTheme) },
                            values = arrayOf(System, Light, Dark),
                            onSelect = { j -> setTheme(j) }
                        )
                    },
                    { _, onChange ->
                        SettingsToggle(
                            title = stringResource(R.string.auto_show_keyboard),
                            onChange = onChange,
                            state = remember { mutableStateOf(prefs.autoShowKeyboard) },
                        ) { toggleKeyboardText() }
                    },
                    { open, onChange ->
                        SettingsNumberItem(
                            title = stringResource(R.string.apps_on_home_screen),
                            open = open,
                            onChange = onChange,
                            currentSelection = remember { mutableStateOf(prefs.homeAppsNum) },
                            min = 0,
                            max = Constants.MAX_HOME_APPS,
                            onSelect = { j -> updateHomeAppsNum(j) }
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_left_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeLeftAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeLeft, j) },
                            appLabel = prefs.appSwipeLeft.appLabel.ifEmpty { "Camera" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_right_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeRightAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeRight, j) },
                            appLabel = prefs.appSwipeRight.appLabel.ifEmpty { "Phone" },
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_up_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeUpAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeUp, j) },
                            appLabel = prefs.appSwipeUp.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.swipe_down_app),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.swipeDownAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetSwipeDown, j) },
                            appLabel = prefs.appSwipeDown.appLabel,
                        )
                    },
                    { open, onChange ->
                        SettingsGestureItem(
                            title = stringResource(R.string.double_tap),
                            open = open,
                            onChange = onChange,
                            currentAction = prefs.doubleTapAction,
                            onSelect = { j -> updateGesture(AppDrawerFlag.SetDoubleTap, j) },
                            appLabel = prefs.appDoubleTap.appLabel
                        )
                    }
                )
            )

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
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
    }

    private fun updateHomeAppsNum(homeAppsNum: Int) {
        prefs.homeAppsNum = homeAppsNum
        viewModel.homeAppsCount.value = homeAppsNum
    }

    private fun toggleKeyboardText() {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
    }

    private fun toggleAutoOpenApp() {
        prefs.autoOpenApp = !prefs.autoOpenApp
    }

    private fun setTheme(appTheme: Constants.Theme) {
        // if (AppCompatDelegate.getDefaultNightMode() == appTheme) return // TODO find out what this did
        prefs.appTheme = appTheme
        requireActivity().recreate()
    }


    private fun setTextSize(size: Int) {
        prefs.textSize = size
    }

    private fun updateGesture(flag: AppDrawerFlag, action: Action) {
        when (flag) {
            AppDrawerFlag.SetSwipeLeft -> prefs.swipeLeftAction = action
            AppDrawerFlag.SetSwipeRight -> prefs.swipeRightAction = action
            AppDrawerFlag.SetSwipeUp -> prefs.swipeUpAction = action
            AppDrawerFlag.SetSwipeDown -> prefs.swipeDownAction = action
            AppDrawerFlag.SetClickClock -> prefs.clickClockAction = action
            AppDrawerFlag.SetClickDate -> prefs.clickDateAction = action
            AppDrawerFlag.SetDoubleTap -> prefs.doubleTapAction = action
            AppDrawerFlag.SetHomeApp,
                AppDrawerFlag.HiddenApps,
                AppDrawerFlag.LaunchApp -> {}
        }

        when(action) {
            Action.OpenApp -> {
                viewModel.getAppList()
                findNavController().navigate(
                    R.id.action_settingsFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString())
                )
            }
            else -> {}
        }
    }
}
