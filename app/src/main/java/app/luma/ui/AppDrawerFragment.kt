package app.luma.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.luma.MainViewModel
import app.luma.R
import app.luma.data.AppModel
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.databinding.FragmentAppDrawerBinding
import app.luma.style.SettingsTheme
import app.luma.style.isDarkTheme
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

        val viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val appAdapter =
            AppDrawerAdapter(
                requireContext(),
                AppDrawerConfig(
                    gravity = Gravity.CENTER,
                    clickListener = appClickListener(viewModel, flag, n),
                    appLongPressListener =
                        if (flag == AppDrawerFlag.LaunchApp ||
                            flag == AppDrawerFlag.HiddenApps
                        ) {
                            appLongPressListener()
                        } else {
                            null
                        },
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

    private fun appLongPressListener(): (AppModel) -> Unit =
        { appModel ->
            findNavController().navigate(
                R.id.appActionsFragment,
                bundleOf(
                    "appPackage" to appModel.appPackage,
                    "appLabel" to appModel.appLabel,
                    "appAlias" to appModel.appAlias,
                    "appActivityName" to appModel.appActivityName,
                    "isHidden" to (flag == AppDrawerFlag.HiddenApps),
                ),
            )
        }
}
