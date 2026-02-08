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
import app.luma.R
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ToggleTextButton

class StatusBarBatteryFragment : Fragment() {
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
                title = stringResource(R.string.status_bar_battery),
                onBack = ::goBack,
            )

            val enabledState = remember { mutableStateOf(prefs.batteryEnabled) }
            val percentageState = remember { mutableStateOf(prefs.batteryPercentage) }
            val iconState = remember { mutableStateOf(prefs.batteryIcon) }

            ContentContainer {
                Column(verticalArrangement = Arrangement.spacedBy(33.5.dp)) {
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_battery_enabled),
                        checked = enabledState.value,
                        onCheckedChange = {
                            enabledState.value = it
                            prefs.batteryEnabled = it
                        },
                        onClick = {
                            enabledState.value = !enabledState.value
                            prefs.batteryEnabled = enabledState.value
                        },
                    )
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_battery_percentage),
                        checked = percentageState.value,
                        onCheckedChange = {
                            percentageState.value = it
                            prefs.batteryPercentage = it
                        },
                        onClick = {
                            percentageState.value = !percentageState.value
                            prefs.batteryPercentage = percentageState.value
                        },
                    )
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_battery_icon),
                        checked = iconState.value,
                        onCheckedChange = {
                            iconState.value = it
                            prefs.batteryIcon = it
                        },
                        onClick = {
                            iconState.value = !iconState.value
                            prefs.batteryIcon = iconState.value
                        },
                    )
                }
            }
        }
    }
}
