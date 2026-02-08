package app.luma.ui

import android.os.Bundle
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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SelectorButton
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ToggleTextButton

class StatusBarNotificationIndicatorFragment : Fragment() {
    private lateinit var prefs: Prefs
    private val sectionState = mutableStateOf(Prefs.NotificationIndicatorSection.Time)
    private val alignmentState = mutableStateOf(Prefs.NotificationIndicatorAlignment.After)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
    }

    override fun onResume() {
        super.onResume()
        sectionState.value = prefs.notificationIndicatorSection
        alignmentState.value = prefs.notificationIndicatorAlignment
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
                title = stringResource(R.string.status_bar_notification_indicator),
                onBack = ::goBack,
            )

            val enabledState = remember { mutableStateOf(prefs.showStatusBarNotificationIndicator) }

            ContentContainer {
                Column(verticalArrangement = Arrangement.spacedBy(33.5.dp)) {
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_enabled),
                        checked = enabledState.value,
                        onCheckedChange = {
                            enabledState.value = it
                            prefs.showStatusBarNotificationIndicator = it
                        },
                        onClick = {
                            enabledState.value = !enabledState.value
                            prefs.showStatusBarNotificationIndicator = enabledState.value
                        },
                    )
                    SelectorButton(
                        label = stringResource(R.string.status_bar_notif_section),
                        value =
                            when (sectionState.value) {
                                Prefs.NotificationIndicatorSection.Cellular -> stringResource(R.string.status_bar_notif_section_cellular)
                                Prefs.NotificationIndicatorSection.Time -> stringResource(R.string.status_bar_notif_section_time)
                                Prefs.NotificationIndicatorSection.Battery -> stringResource(R.string.status_bar_notif_section_battery)
                            },
                        onClick = {
                            findNavController().navigate(
                                R.id.action_statusBarNotificationIndicatorFragment_to_notificationIndicatorSectionFragment,
                            )
                        },
                    )
                    SelectorButton(
                        label = stringResource(R.string.status_bar_notif_alignment),
                        value =
                            when (alignmentState.value) {
                                Prefs.NotificationIndicatorAlignment.Before -> stringResource(R.string.status_bar_notif_alignment_before)
                                Prefs.NotificationIndicatorAlignment.After -> stringResource(R.string.status_bar_notif_alignment_after)
                            },
                        onClick = {
                            findNavController().navigate(
                                R.id.action_statusBarNotificationIndicatorFragment_to_notificationIndicatorAlignmentFragment,
                            )
                        },
                    )
                }
            }
        }
    }
}
