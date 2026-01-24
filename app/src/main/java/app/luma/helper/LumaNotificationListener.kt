package app.luma.helper

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class LumaNotificationListener : NotificationListenerService() {
    companion object {
        private var instance: LumaNotificationListener? = null

        fun getActiveNotificationPackages(): Set<String> = instance?.activeNotifications?.map { it.packageName }?.toSet() ?: emptySet()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Notification posted - could trigger UI update here if needed
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Notification removed - could trigger UI update here if needed
    }
}
