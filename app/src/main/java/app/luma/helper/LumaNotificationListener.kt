package app.luma.helper

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.lang.ref.WeakReference

class LumaNotificationListener : NotificationListenerService() {
    companion object {
        private var instance: WeakReference<LumaNotificationListener> = WeakReference(null)

        fun getActiveNotificationPackages(): Set<String> =
            instance
                .get()
                ?.activeNotifications
                ?.map { it.packageName }
                ?.toSet()
                ?: emptySet()
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
        // Notification posted - could trigger UI update here if needed
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Notification removed - could trigger UI update here if needed
    }
}
