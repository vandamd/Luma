package app.luma.ui

import SettingsTheme
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import isDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.ToggleSelectorButton
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat

class NotificationsFragment : Fragment() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val compose = ComposeView(requireContext())
        compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        compose.setContent {
            SettingsTheme(isDarkTheme(prefs)) {
                NotificationsScreen()
            }
        }
        return compose
    }

    @Composable
    fun NotificationsScreen() {
        Column {
            SettingsHeader(
                title = "Notifications",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )

            ContentContainer(verticalArrangement = Arrangement.spacedBy(45.dp)) {
                SimpleTextButton("Grant Permissions") { 
                    openNotificationListenerSettings() 
                }
                
                val notificationIndicatorState = remember { mutableStateOf(prefs.showNotificationIndicator) }
                val hasNotificationPermission = NotificationManagerCompat.getEnabledListenerPackages(requireContext()).contains(requireContext().packageName)
                
                ToggleSelectorButton(
                    label = "Indicator (*)",
                    value = if (hasNotificationPermission) {
                        if (notificationIndicatorState.value) "visible next to apps" else "not visible"
                    } else {
                        "not visible (permission required)"
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
                    enabled = hasNotificationPermission
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
