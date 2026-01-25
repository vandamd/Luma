package app.luma.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import app.luma.R
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.SettingsComposable.ToggleSelectorButton

class NotificationsFragment : Fragment() {
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { NotificationsScreen() }

    @Composable
    fun NotificationsScreen() {
        Column {
            SettingsHeader(
                title = stringResource(R.string.settings_notifications),
                onBack = ::goBack,
            )

            ContentContainer(verticalArrangement = Arrangement.spacedBy(45.dp)) {
                SimpleTextButton(stringResource(R.string.notifications_grant_permissions)) {
                    openNotificationListenerSettings()
                }

                val notificationIndicatorState = remember { mutableStateOf(prefs.showNotificationIndicator) }
                val hasNotificationPermission =
                    NotificationManagerCompat
                        .getEnabledListenerPackages(
                            requireContext(),
                        ).contains(requireContext().packageName)

                ToggleSelectorButton(
                    label = stringResource(R.string.notifications_indicator),
                    value =
                        if (hasNotificationPermission) {
                            if (notificationIndicatorState.value) {
                                stringResource(R.string.notifications_visible_next_to_apps)
                            } else {
                                stringResource(R.string.notifications_not_visible)
                            }
                        } else {
                            stringResource(R.string.notifications_not_visible_permission_required)
                        },
                    checked = hasNotificationPermission && notificationIndicatorState.value,
                    onCheckedChange = {
                        if (hasNotificationPermission) {
                            notificationIndicatorState.value = it
                            prefs.showNotificationIndicator = it
                        }
                    },
                    onClick = {
                        if (hasNotificationPermission) {
                            notificationIndicatorState.value = !notificationIndicatorState.value
                            prefs.showNotificationIndicator = notificationIndicatorState.value
                        }
                    },
                    enabled = hasNotificationPermission,
                )
            }
        }
    }

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings if notification listener settings not available
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }
}
