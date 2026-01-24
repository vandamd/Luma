package app.luma.ui

import SettingsTheme
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
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import isDarkTheme

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
                    AppDrawerFlag.SetHomeApp -> "Select/Rename App"
                    AppDrawerFlag.HiddenApps -> "Hidden Apps"
                    else -> "App Drawer"
                }
            SettingsTheme(isDarkTheme(Prefs(requireContext()))) {
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

        when (flag) {
            AppDrawerFlag.SetHomeApp -> {
                // Remove legacy rename button, keep only the pseudo rename app in the list
                binding.drawerButton.isVisible = false
            }

            AppDrawerFlag.SetSwipeRight,
            AppDrawerFlag.SetSwipeLeft,
            AppDrawerFlag.SetSwipeDown,
            -> {
                binding.drawerButton.setOnClickListener {
                    findNavController().popBackStack()
                }
            }

            else -> {}
        }

        val viewModel =
            activity?.run {
                ViewModelProvider(this)[MainViewModel::class.java]
            } ?: throw Exception("Invalid Activity")

        val appAdapter =
            AppDrawerAdapter(
                flag,
                Gravity.CENTER,
                appClickListener(viewModel, flag, n),
                appInfoListener(),
                appShowHideListener(),
                appRenameListener(),
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
                        val prefs = Prefs(requireContext())
                        val hiddenApps = prefs.hiddenApps
                        appList.filter { app ->
                            !hiddenApps.contains(app.appPackage + "|" + app.user.toString())
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
            if (flag == AppDrawerFlag.SetHomeApp && appModel.appPackage == "__rename__") {
                // We need to pass the home app information to rename
                val homeAppModel = Prefs(requireContext()).getHomeAppModel(n)
                findNavController().navigate(
                    R.id.renameFragment,
                    bundleOf(
                        "appPackage" to homeAppModel.appPackage,
                        "appLabel" to homeAppModel.appLabel,
                        "homePosition" to n,
                    ),
                )
            } else {
                viewModel.selectedApp(appModel, flag, n)
                if (flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps) {
                    findNavController().popBackStack(R.id.mainFragment, false)
                } else {
                    findNavController().popBackStack()
                }
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
            val prefs = Prefs(requireContext())
            val newSet = mutableSetOf<String>()
            newSet.addAll(prefs.hiddenApps)

            if (flag == AppDrawerFlag.HiddenApps) {
                newSet.remove(appModel.appPackage) // for backward compatibility
                newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
            } else {
                newSet.add(appModel.appPackage + "|" + appModel.user.toString())
            }

            prefs.hiddenApps = newSet

            if (newSet.isEmpty()) findNavController().popBackStack()
        }

    private fun appRenameListener(): (appPackage: String, appAlias: String) -> Unit =
        { appPackage, appAlias ->
            val prefs = Prefs(requireContext())
            prefs.setAppAlias(appPackage, appAlias)
        }

    // Removed legacy renameListener function
}
