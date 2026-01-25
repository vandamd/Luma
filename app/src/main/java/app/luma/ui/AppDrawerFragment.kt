package app.luma.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.luma.MainViewModel
import app.luma.R
import app.luma.data.AppModel
import app.luma.data.Constants
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.databinding.FragmentAppDrawerBinding
import app.luma.helper.openAppInfo
import app.luma.style.SettingsTheme
import app.luma.style.isDarkTheme
import app.luma.ui.AppDrawerConfig
import app.luma.ui.compose.SettingsComposable.SettingsHeader

class AppDrawerFragment : Fragment() {
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    private lateinit var flag: AppDrawerFlag
    private var n: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)

        val flagString = arguments?.getString("flag", AppDrawerFlag.LaunchApp.toString()) ?: AppDrawerFlag.LaunchApp.toString()
        flag = AppDrawerFlag.valueOf(flagString)
        n = arguments?.getInt("n", 0) ?: 0

        val header: ComposeView? = binding.headerCompose
        header?.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        header?.setContent {
            val headerTitle =
                when (flag) {
                    AppDrawerFlag.SetHomeApp -> requireContext().getString(R.string.app_drawer_select_rename)
                    AppDrawerFlag.HiddenApps -> requireContext().getString(R.string.app_drawer_hidden_apps)
                    else -> requireContext().getString(R.string.app_drawer_title)
                }
            SettingsTheme(isDarkTheme(Prefs.getInstance(requireContext()))) {
                SettingsHeader(title = headerTitle, onBack = { findNavController().popBackStack() })
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("RtlHardcoded")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // flag, n are now determined in onCreateView

        // No special setup needed for different flags anymore
        // The drawerButton was removed as it was always hidden

        val viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val appAdapter =
            AppDrawerAdapter(
                requireContext(),
                AppDrawerConfig(
                    flag = flag,
                    gravity = Gravity.CENTER,
                    clickListener = appClickListener(viewModel, flag, n),
                    appInfoListener = appInfoListener(),
                    appHideListener = appShowHideListener(),
                    appDeleteShortcutListener = appDeleteShortcutListener(),
                    appRenameListener = appRenameListener(),
                    appLongPressListener = if (flag == AppDrawerFlag.LaunchApp) appLongPressListener() else null,
                ),
            )

        initViewModel(flag, viewModel, appAdapter)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = appAdapter
    }

    private fun initViewModel(
        flag: AppDrawerFlag,
        viewModel: MainViewModel,
        appAdapter: AppDrawerAdapter,
    ) {
        viewModel.hiddenApps.observe(viewLifecycleOwner) {
            if (flag != AppDrawerFlag.HiddenApps) return@observe
            it?.let { appList ->
                binding.listEmptyHint.visibility = if (appList.isEmpty()) View.VISIBLE else View.GONE
                populateAppList(appList, appAdapter)
            }
        }

        viewModel.appList.observe(viewLifecycleOwner) {
            if (flag == AppDrawerFlag.HiddenApps) return@observe
            it?.let { appList ->
                val filteredList =
                    if (flag == AppDrawerFlag.SetHomeApp) {
                        appList
                    } else {
                        val prefs = Prefs.getInstance(requireContext())
                        val hiddenApps = prefs.hiddenApps
                        appList.filter { app ->
                            !hiddenApps.contains(app.appPackage)
                        }
                    }

                binding.listEmptyHint.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
                populateAppList(filteredList, appAdapter)
            }
        }
    }

    private fun populateAppList(
        apps: List<AppModel>,
        appAdapter: AppDrawerAdapter,
    ) {
        // layout animation removed
        appAdapter.setAppList(apps.toMutableList())
    }

    private fun appClickListener(
        viewModel: MainViewModel,
        flag: AppDrawerFlag,
        n: Int = 0,
    ): (appModel: AppModel) -> Unit =
        { appModel ->
            viewModel.selectedApp(appModel, flag, n)
            if (flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps || flag == AppDrawerFlag.SetHomeApp) {
                findNavController().popBackStack(R.id.mainFragment, false)
            } else {
                findNavController().popBackStack()
            }
        }

    private fun appInfoListener(): (appModel: AppModel) -> Unit =
        { appModel ->
            openAppInfo(
                requireContext(),
                appModel.user,
                appModel.appPackage,
            )
            findNavController().popBackStack(R.id.mainFragment, false)
        }

    private fun appShowHideListener(): (flag: AppDrawerFlag, appModel: AppModel) -> Unit =
        { flag, appModel ->
            val prefs = Prefs.getInstance(requireContext())

            if (appModel.appPackage == Constants.PINNED_SHORTCUT_PACKAGE) {
                val shortcutId = appModel.appActivityName
                if (flag == AppDrawerFlag.HiddenApps) {
                    prefs.unhideShortcut(shortcutId)
                } else {
                    prefs.hideShortcut(shortcutId)
                }
            } else {
                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)

                if (flag == AppDrawerFlag.HiddenApps) {
                    newSet.remove(appModel.appPackage)
                } else {
                    newSet.add(appModel.appPackage)
                }

                prefs.hiddenApps = newSet
            }

            if (flag == AppDrawerFlag.HiddenApps) {
                val hasHiddenApps = prefs.hiddenApps.isNotEmpty()
                val hasHiddenShortcuts = prefs.hiddenShortcutIds.isNotEmpty()
                if (!hasHiddenApps && !hasHiddenShortcuts) {
                    findNavController().popBackStack()
                }
            }
        }

    private fun appDeleteShortcutListener(): (appModel: AppModel) -> Unit =
        { appModel ->
            val prefs = Prefs.getInstance(requireContext())
            val shortcutId = appModel.appActivityName
            prefs.removePinnedShortcut(shortcutId)
            prefs.unhideShortcut(shortcutId)
        }

    private fun appRenameListener(): (appPackage: String, appAlias: String) -> Unit =
        { appPackage, appAlias ->
            val prefs = Prefs.getInstance(requireContext())
            prefs.setAppAlias(appPackage, appAlias)
        }

    private fun appLongPressListener(): (AppModel) -> Unit =
        { appModel ->
            findNavController().navigate(
                R.id.appActionsFragment,
                bundleOf(
                    "appPackage" to appModel.appPackage,
                    "appLabel" to appModel.appLabel,
                    "appAlias" to appModel.appAlias,
                    "appActivityName" to appModel.appActivityName,
                ),
            )
        }
}
