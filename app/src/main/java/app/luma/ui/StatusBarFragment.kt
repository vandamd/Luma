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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.SettingsComposable.ToggleTextButton

class StatusBarFragment : Fragment() {
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
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
                title = stringResource(R.string.settings_status_bar),
                onBack = ::goBack,
            )

            val enabledState = remember { mutableStateOf(prefs.statusBarEnabled) }
            val notifIndicatorState = remember { mutableStateOf(prefs.showNotificationIndicator) }

            ContentContainer {
                Column(verticalArrangement = Arrangement.spacedBy(33.5.dp)) {
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_enabled),
                        checked = enabledState.value,
                        onCheckedChange = {
                            enabledState.value = it
                            prefs.statusBarEnabled = it
                        },
                        onClick = {
                            enabledState.value = !enabledState.value
                            prefs.statusBarEnabled = enabledState.value
                        },
                    )
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_notification_indicator),
                        checked = notifIndicatorState.value,
                        onCheckedChange = {
                            notifIndicatorState.value = it
                            prefs.showNotificationIndicator = it
                        },
                        onClick = {
                            notifIndicatorState.value = !notifIndicatorState.value
                            prefs.showNotificationIndicator = notifIndicatorState.value
                        },
                    )
                    SimpleTextButton(stringResource(R.string.status_bar_connectivity)) {
                        findNavController().navigate(R.id.action_statusBarFragment_to_statusBarConnectivityFragment)
                    }
                    SimpleTextButton(stringResource(R.string.status_bar_time)) {
                        findNavController().navigate(R.id.action_statusBarFragment_to_statusBarTimeFragment)
                    }
                    SimpleTextButton(stringResource(R.string.status_bar_battery)) {
                        findNavController().navigate(R.id.action_statusBarFragment_to_statusBarBatteryFragment)
                    }
                }
            }
        }
    }
}
