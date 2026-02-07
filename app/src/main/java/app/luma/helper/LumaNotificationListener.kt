package app.luma.helper

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

        fun getActiveNotificationPackages(): Set<String> =
            instance
                .get()
                ?.activeNotifications
                ?.map { it.packageName }
                ?.toSet()
                ?: emptySet()

        fun getActiveNotifications(): List<StatusBarNotification> =
            instance
                .get()
                ?.activeNotifications
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
