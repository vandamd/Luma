package app.luma.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Constants
import app.luma.data.Prefs
import app.luma.data.StatusBarSectionType
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.PrefsToggleTextButton
import app.luma.ui.compose.SettingsComposable.SelectorButton
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsItemSpacing

class StatusBarConnectivityFragment : Fragment() {
    private lateinit var prefs: Prefs
    private val actionState = mutableStateOf(Constants.Action.Disabled)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
    }

    override fun onResume() {
        super.onResume()
        actionState.value = prefs.getSectionAction(StatusBarSectionType.CELLULAR)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView(onSwipeBack = ::goBack) { Screen() }

    @Composable
    fun Screen() {
        Column {
            SettingsHeader(
                title = stringResource(R.string.status_bar_connectivity),
                onBack = ::goBack,
            )

            ContentContainer {
                Column(verticalArrangement = Arrangement.spacedBy(SettingsItemSpacing)) {
                    PrefsToggleTextButton(
                        title = stringResource(R.string.status_bar_cellular),
                        initialValue = prefs.cellularEnabled,
                        onValueChange = { prefs.cellularEnabled = it },
                    )
                    PrefsToggleTextButton(
                        title = stringResource(R.string.status_bar_wifi),
                        initialValue = prefs.wifiEnabled,
                        onValueChange = { prefs.wifiEnabled = it },
                    )
                    PrefsToggleTextButton(
                        title = stringResource(R.string.status_bar_bluetooth),
                        initialValue = prefs.bluetoothEnabled,
                        onValueChange = { prefs.bluetoothEnabled = it },
                    )
                    SelectorButton(
                        label = stringResource(R.string.status_bar_on_press),
                        value = actionDisplayValue(actionState.value, prefs, StatusBarSectionType.CELLULAR),
                        onClick = {
                            findNavController().navigate(
                                R.id.action_statusBarConnectivityFragment_to_gestureActionFragment,
                                bundleOf(GestureActionFragment.SECTION_TYPE to StatusBarSectionType.CELLULAR.name),
                            )
                        },
                    )
                }
            }
        }
    }
}
