package app.luma.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.PrefsToggleTextButton
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.SettingsItemSpacing

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

            ContentContainer {
                Column(verticalArrangement = Arrangement.spacedBy(SettingsItemSpacing)) {
                    PrefsToggleTextButton(
                        title = stringResource(R.string.status_bar_enabled),
                        initialValue = prefs.statusBarEnabled,
                        onValueChange = { prefs.statusBarEnabled = it },
                    )
                    SimpleTextButton(stringResource(R.string.status_bar_notification_indicator)) {
                        findNavController().navigate(R.id.action_statusBarFragment_to_statusBarNotificationIndicatorFragment)
                    }
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
