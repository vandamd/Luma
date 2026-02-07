package app.luma.ui

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import app.luma.R
import app.luma.helper.LumaNotificationListener
import app.luma.helper.performHapticFeedback
import app.luma.style.SettingsTheme
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.MessageText
import app.luma.ui.compose.SettingsComposable.SettingsHeader

private data class NotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String?,
)

class NotificationListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { NotificationListScreen() }

    @Suppress("DEPRECATION")
    private fun loadNotifications(): List<NotificationItem> {
        val pm = requireContext().packageManager
        return LumaNotificationListener
            .getActiveNotifications()
            .filter { !it.isOngoing }
            .map { sbn -> sbn.toNotificationItem(pm) }
            .sortedBy { it.title.lowercase() }
    }

    @Suppress("DEPRECATION")
    private fun StatusBarNotification.toNotificationItem(pm: android.content.pm.PackageManager): NotificationItem {
        val appLabel =
            try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                packageName
            }
        return NotificationItem(
            key = key,
            packageName = packageName,
            title = notification.extras.getString(Notification.EXTRA_TITLE) ?: appLabel,
            text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
    }

    @Composable
    private fun NotificationListScreen() {
        val context = LocalContext.current
        val items = loadNotifications()
        val notifications = remember { mutableStateListOf(*items.toTypedArray()) }

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                title = stringResource(R.string.notification_list_title),
                onBack = ::goBack,
            )
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    MessageText(stringResource(R.string.notification_list_empty))
                }
            } else {
                ContentContainer {
                    CustomScrollView(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        notifications.forEach { item ->
                            NotificationRow(
                                item = item,
                                onTap = {
                                    if (item.packageName.isNotEmpty()) {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                                        if (launchIntent != null) {
                                            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(launchIntent)
                                        }
                                    }
                                },
                                onDismiss = {
                                    if (item.key.isNotEmpty()) {
                                        LumaNotificationListener.dismissNotification(item.key)
                                    }
                                    notifications.remove(item)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NotificationRow(
        item: NotificationItem,
        onTap: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        val context = LocalContext.current

        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.close_24px),
                contentDescription = stringResource(R.string.content_desc_dismiss),
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable {
                            performHapticFeedback(context)
                            onDismiss()
                        },
                colorFilter = ColorFilter.tint(SettingsTheme.typography.title.color),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable {
                            performHapticFeedback(context)
                            onTap()
                        },
            ) {
                Text(
                    item.title,
                    style = SettingsTheme.typography.pageButton,
                    fontSize = 28.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.text.isNullOrBlank()) {
                    Text(
                        item.text,
                        style = SettingsTheme.typography.item,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
