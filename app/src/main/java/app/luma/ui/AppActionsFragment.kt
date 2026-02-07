package app.luma.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.AppModel
import app.luma.data.Constants
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.HomeLayout
import app.luma.data.Prefs
import app.luma.helper.openAppInfo
import app.luma.helper.uninstallApp
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton

class AppActionsFragment : Fragment() {
    private val appPackage: String by lazy { arguments?.getString("appPackage") ?: "" }
    private val appLabel: String by lazy { arguments?.getString("appLabel") ?: "" }
    private val appAlias: String by lazy { arguments?.getString("appAlias") ?: "" }
    private val appActivityName: String by lazy { arguments?.getString("appActivityName") ?: "" }
    private val homePosition: Int by lazy { arguments?.getInt("homePosition", -1) ?: -1 }
    private val isAppHidden: Boolean by lazy { arguments?.getBoolean("isHidden", false) ?: false }

    private val prefs: Prefs by lazy { Prefs.getInstance(requireContext()) }

    private val displayName: String
        get() = appAlias.ifEmpty { appLabel }

    private val isPinnedShortcut: Boolean
        get() = appPackage == Constants.PINNED_SHORTCUT_PACKAGE

    private val isHomeApp: Boolean
        get() = homePosition >= 0

    private val canMoveUp: Boolean by lazy {
        if (!isHomeApp) return@lazy false
        val pageStartIndex = (homePosition / HomeLayout.APPS_PER_PAGE) * HomeLayout.APPS_PER_PAGE
        homePosition > pageStartIndex
    }

    private val canMoveDown: Boolean by lazy {
        if (!isHomeApp) return@lazy false
        val page = homePosition / HomeLayout.APPS_PER_PAGE
        val pageStartIndex = page * HomeLayout.APPS_PER_PAGE
        val appsOnPage = prefs.getAppsPerPage(page + 1)
        homePosition < pageStartIndex + appsOnPage - 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { AppActionsScreen() }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeConfirmationResult()
    }

    private fun observeConfirmationResult() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<Boolean>("confirmed").observe(viewLifecycleOwner) { confirmed ->
            if (confirmed != true) return@observe

            savedStateHandle.remove<Boolean>("confirmed")
            val action = savedStateHandle.remove<String>("action")

            when (action) {
                "removeShortcut" -> {
                    val prefs = Prefs.getInstance(requireContext())
                    prefs.removePinnedShortcut(appActivityName)
                    prefs.unhideShortcut(appActivityName)

                    if (homePosition >= 0) {
                        val currentHomeApp = prefs.getHomeAppModel(homePosition)
                        if (currentHomeApp.appActivityName == appActivityName &&
                            currentHomeApp.appPackage == Constants.PINNED_SHORTCUT_PACKAGE
                        ) {
                            prefs.setHomeAppModel(
                                homePosition,
                                AppModel(
                                    appLabel = "",
                                    appPackage = "",
                                    appAlias = "",
                                    appActivityName = "",
                                    user = android.os.Process.myUserHandle(),
                                    key = null,
                                ),
                            )
                        }
                    }

                    findNavController().popBackStack(R.id.mainFragment, false)
                }

                "uninstallApp" -> {
                    uninstallApp(requireContext(), appPackage)
                    findNavController().popBackStack(R.id.mainFragment, false)
                }
            }
        }
    }

    @Composable
    private fun AppActionsScreen() {
        Column {
            SettingsHeader(
                title = displayName,
                onBack = ::goBack,
            )
            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(33.5.dp)) {
                    SimpleTextButton(stringResource(R.string.app_actions_rename)) {
                        findNavController().navigate(
                            R.id.renameFragment,
                            bundleOf(
                                "appPackage" to appPackage,
                                "appLabel" to appLabel,
                                "appAlias" to appAlias,
                                "homePosition" to homePosition,
                            ),
                        )
                    }
                    if (!isHomeApp) {
                        val buttonText = if (isAppHidden) R.string.app_actions_show else R.string.app_actions_hide
                        SimpleTextButton(stringResource(buttonText)) {
                            val prefs = Prefs.getInstance(requireContext())
                            if (isPinnedShortcut) {
                                if (isAppHidden) prefs.unhideShortcut(appActivityName) else prefs.hideShortcut(appActivityName)
                            } else {
                                val newSet = prefs.hiddenApps.toMutableSet()
                                if (isAppHidden) newSet.remove(appPackage) else newSet.add(appPackage)
                                prefs.hiddenApps = newSet
                            }
                            findNavController().popBackStack(R.id.mainFragment, false)
                        }
                    }
                    if (isHomeApp) {
                        SimpleTextButton(stringResource(R.string.app_actions_replace)) {
                            findNavController().navigate(
                                R.id.appListFragment,
                                bundleOf(
                                    "flag" to AppDrawerFlag.SetHomeApp.toString(),
                                    "n" to homePosition,
                                ),
                            )
                        }
                        if (canMoveUp) {
                            SimpleTextButton(stringResource(R.string.app_actions_move_up)) {
                                val above = prefs.getHomeAppModel(homePosition - 1)
                                val current = prefs.getHomeAppModel(homePosition)
                                prefs.setHomeAppModel(homePosition - 1, current)
                                prefs.setHomeAppModel(homePosition, above)
                                findNavController().popBackStack(R.id.mainFragment, false)
                            }
                        }
                        if (canMoveDown) {
                            SimpleTextButton(stringResource(R.string.app_actions_move_down)) {
                                val below = prefs.getHomeAppModel(homePosition + 1)
                                val current = prefs.getHomeAppModel(homePosition)
                                prefs.setHomeAppModel(homePosition + 1, current)
                                prefs.setHomeAppModel(homePosition, below)
                                findNavController().popBackStack(R.id.mainFragment, false)
                            }
                        }
                    }
                    if (isPinnedShortcut) {
                        SimpleTextButton(stringResource(R.string.app_actions_uninstall)) {
                            findNavController().navigate(
                                R.id.confirmFragment,
                                bundleOf(
                                    "title" to getString(R.string.app_actions_confirm_title),
                                    "message" to getString(R.string.app_actions_confirm_message, displayName),
                                    "confirmText" to getString(R.string.app_actions_uninstall),
                                    "action" to "removeShortcut",
                                ),
                            )
                        }
                    } else {
                        SimpleTextButton(stringResource(R.string.app_actions_app_info)) {
                            openAppInfo(
                                requireContext(),
                                android.os.Process.myUserHandle(),
                                appPackage,
                            )
                            findNavController().popBackStack(R.id.mainFragment, false)
                        }
                        SimpleTextButton(stringResource(R.string.app_actions_uninstall)) {
                            findNavController().navigate(
                                R.id.confirmFragment,
                                bundleOf(
                                    "title" to getString(R.string.app_actions_uninstall_title),
                                    "message" to getString(R.string.app_actions_confirm_message, displayName),
                                    "confirmText" to getString(R.string.app_actions_uninstall),
                                    "action" to "uninstallApp",
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
