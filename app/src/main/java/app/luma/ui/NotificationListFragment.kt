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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

    private fun testNotifications(): List<NotificationItem> =
        listOf(
            NotificationItem(
                "1",
                "",
                "John sent you a message on WhatsApp",
                "Hey, are you free for lunch today? I was thinking we could try that new Thai place on King Street",
            ),
            NotificationItem(
                "2",
                "",
                "Mom",
                "Don't forget to call grandma this weekend, she's been asking about you and wants to know how the new job is going",
            ),
            NotificationItem(
                "3",
                "",
                "Your Daily Mix is ready on Spotify",
                "Based on your recent listening: Radiohead, Thom Yorke, Everything Everything, and more",
            ),
            NotificationItem(
                "4",
                "",
                "Gmail: Meeting rescheduled",
                "Tomorrow's 2pm product review has been moved to 3pm in the large conference room on the 4th floor",
            ),
            NotificationItem(
                "5",
                "",
                "WhatsApp Group: Weekend Plans",
                "Sarah sent a photo and 3 messages in the group chat about the camping trip next Saturday",
            ),
            NotificationItem(
                "6",
                "",
                "Slack: #engineering",
                "Jake mentioned you in a thread about the new authentication service deployment timeline for Q2",
            ),
            NotificationItem(
                "7",
                "",
                "Calendar reminder",
                "Standup in 15 minutes — don't forget to prepare your update on the notification feature",
            ),
            NotificationItem("8", "", "Twitter", "5 new notifications from people you follow including @SwiftOnSecurity and @kelaborators"),
            NotificationItem(
                "9",
                "",
                "Commonwealth Bank",
                "Payment of $42.50 to Woolworths Metro Town Hall processed successfully from your everyday account",
            ),
            NotificationItem(
                "10",
                "",
                "Weather Alert for Sydney",
                "Severe thunderstorm warning: heavy rain and possible hail expected between 3pm and 6pm this afternoon",
            ),
            NotificationItem(
                "11",
                "",
                "Uber",
                "Your ride with Ahmed is arriving in 3 minutes — look for a white Toyota Camry with plate XYZ 123",
            ),
            NotificationItem(
                "12",
                "",
                "Netflix",
                "New episode of The Bear Season 4 is now streaming — continue watching where you left off",
            ),
            NotificationItem(
                "13",
                "",
                "Signal: Alex",
                "Can you pick up milk and bread on your way home? Also we're out of coffee and dishwasher tablets",
            ),
            NotificationItem(
                "14",
                "",
                "ABC News Australia",
                "Breaking: Major announcement from Apple — new product line revealed at surprise event in Cupertino",
            ),
            NotificationItem(
                "15",
                "",
                "Fitness: Daily Goal Progress",
                "You've hit 8,000 steps today! Only 2,000 more to reach your daily target of 10,000 steps",
            ),
        )

    @Composable
    private fun NotificationListScreen() {
        val context = LocalContext.current
        val real = loadNotifications()
        val items = real.ifEmpty { testNotifications() }
        val notifications = remember { mutableStateListOf(*items.toTypedArray()) }

        Column {
            SettingsHeader(
                title = stringResource(R.string.notification_list_title),
                onBack = ::goBack,
            )
            ContentContainer {
                if (notifications.isEmpty()) {
                    MessageText(stringResource(R.string.notification_list_empty))
                } else {
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
