package app.luma.helper

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

class LumaNotificationListener : NotificationListenerService() {
    companion object {
        private var instance: WeakReference<LumaNotificationListener> = WeakReference(null)

        private val _changeVersion = MutableStateFlow(0)
        val changeVersion: StateFlow<Int> = _changeVersion.asStateFlow()

        @Suppress("DEPRECATION")
        private fun StatusBarNotification.isLightOsKeepAlive(): Boolean {
            val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            if (!text.isNullOrBlank()) return false
            val title = notification.extras.getString(Notification.EXTRA_TITLE)
            if (title == "LightOS") return true
            if (title == null) {
                val pm = instance.get()?.packageManager ?: return false
                val appLabel =
                    try {
                        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                    } catch (_: Exception) {
                        null
                    }
                return appLabel == "LightOS"
            }
            return false
        }

        fun getActiveNotificationPackages(): Set<String> =
            instance
                .get()
                ?.activeNotifications
                ?.filterNot { it.isLightOsKeepAlive() }
                ?.map { it.packageName }
                ?.toSet()
                ?: emptySet()

        fun getActiveNotifications(): List<StatusBarNotification> =
            instance
                .get()
                ?.activeNotifications
                ?.filterNot { it.isLightOsKeepAlive() }
                ?.toList()
                ?: emptyList()

        fun dismissNotification(key: String) {
            instance.get()?.cancelNotification(key)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = WeakReference(null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        _changeVersion.value++
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        _changeVersion.value++
    }
}
