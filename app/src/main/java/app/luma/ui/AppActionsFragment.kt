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
import app.luma.data.Constants
import app.luma.data.Constants.AppDrawerFlag
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

    private val displayName: String
        get() = appAlias.ifEmpty { appLabel }

    private val isPinnedShortcut: Boolean
        get() = appPackage == Constants.PINNED_SHORTCUT_PACKAGE

    private val isHomeApp: Boolean
        get() = homePosition >= 0

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

        // Listen for confirmation result
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("confirmed")
            ?.observe(viewLifecycleOwner) { confirmed ->
                if (confirmed == true) {
                    val action =
                        findNavController()
                            .currentBackStackEntry
                            ?.savedStateHandle
                            ?.get<String>("action")

                    when (action) {
                        "removeShortcut" -> {
                            val prefs = Prefs.getInstance(requireContext())
                            prefs.removePinnedShortcut(appActivityName)
                            prefs.unhideShortcut(appActivityName)
                            findNavController().popBackStack(R.id.mainFragment, false)
                        }

                        "uninstallApp" -> {
                            uninstallApp(requireContext(), appPackage)
                            findNavController().popBackStack(R.id.mainFragment, false)
                        }
                    }

                    // Clear the state
                    findNavController()
                        .currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Boolean>("confirmed")
                    findNavController()
                        .currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("action")
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
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(40.dp)) {
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
