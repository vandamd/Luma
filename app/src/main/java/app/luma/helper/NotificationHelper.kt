package app.luma.helper

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    
    fun hasActiveNotifications(context: Context, packageName: String): Boolean {
        return try {
            if (!NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
                false
            } else {
                // This requires the notification listener service to be enabled
                // The actual implementation would need access to the StatusBarNotifications
                // which is only available in a NotificationListenerService
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun getPackagesWithNotifications(notifications: Array<StatusBarNotification>?): Set<String> {
        return notifications?.map { it.packageName }?.toSet() ?: emptySet()
    }
}