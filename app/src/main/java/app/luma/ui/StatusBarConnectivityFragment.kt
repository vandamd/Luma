package app.luma.ui

import android.os.Bundle
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
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Constants
import app.luma.data.Prefs
import app.luma.data.StatusBarSectionType
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SelectorButton
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ToggleTextButton

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

            val cellularState = remember { mutableStateOf(prefs.cellularEnabled) }
            val wifiState = remember { mutableStateOf(prefs.wifiEnabled) }
            val bluetoothState = remember { mutableStateOf(prefs.bluetoothEnabled) }

            ContentContainer {
                Column(verticalArrangement = Arrangement.spacedBy(33.5.dp)) {
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_cellular),
                        checked = cellularState.value,
                        onCheckedChange = {
                            cellularState.value = it
                            prefs.cellularEnabled = it
                        },
                        onClick = {
                            cellularState.value = !cellularState.value
                            prefs.cellularEnabled = cellularState.value
                        },
                    )
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_wifi),
                        checked = wifiState.value,
                        onCheckedChange = {
                            wifiState.value = it
                            prefs.wifiEnabled = it
                        },
                        onClick = {
                            wifiState.value = !wifiState.value
                            prefs.wifiEnabled = wifiState.value
                        },
                    )
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_bluetooth),
                        checked = bluetoothState.value,
                        onCheckedChange = {
                            bluetoothState.value = it
                            prefs.bluetoothEnabled = it
                        },
                        onClick = {
                            bluetoothState.value = !bluetoothState.value
                            prefs.bluetoothEnabled = bluetoothState.value
                        },
                    )
                    SelectorButton(
                        label = stringResource(R.string.status_bar_on_press),
                        value = actionDisplayValue(actionState.value, StatusBarSectionType.CELLULAR),
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

    @Composable
    private fun actionDisplayValue(
        action: Constants.Action,
        section: StatusBarSectionType,
    ): String =
        when (action) {
            Constants.Action.OpenApp -> stringResource(R.string.action_open_app_name, prefs.getSectionApp(section).appLabel)
            Constants.Action.Disabled -> stringResource(R.string.action_disabled)
            else -> action.displayName()
        }
}
