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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Constants
import app.luma.data.Prefs
import app.luma.data.StatusBarSectionType
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SelectorButton
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ToggleTextButton

class StatusBarTimeFragment : Fragment() {
    private lateinit var prefs: Prefs
    private val formatState = mutableStateOf(Prefs.TimeFormat.TwentyFourHour)
    private val actionState = mutableStateOf(Constants.Action.ShowNotificationList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
    }

    override fun onResume() {
        super.onResume()
        formatState.value = prefs.timeFormat
        actionState.value = prefs.getSectionAction(StatusBarSectionType.TIME)
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
                title = stringResource(R.string.status_bar_time),
                onBack = ::goBack,
            )

            val enabledState = remember { mutableStateOf(prefs.timeEnabled) }
            val secondsState = remember { mutableStateOf(prefs.showSeconds) }
            val leadingZeroState = remember { mutableStateOf(prefs.leadingZero) }

            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(33.5.dp)) {
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_time_enabled),
                        checked = enabledState.value,
                        onCheckedChange = {
                            enabledState.value = it
                            prefs.timeEnabled = it
                        },
                        onClick = {
                            enabledState.value = !enabledState.value
                            prefs.timeEnabled = enabledState.value
                        },
                    )
                    SelectorButton(
                        label = stringResource(R.string.status_bar_time_format),
                        value =
                            when (formatState.value) {
                                Prefs.TimeFormat.Standard -> stringResource(R.string.status_bar_time_standard)
                                Prefs.TimeFormat.TwentyFourHour -> stringResource(R.string.status_bar_time_24h)
                            },
                        onClick = {
                            findNavController().navigate(R.id.action_statusBarTimeFragment_to_timeFormatFragment)
                        },
                    )
                    if (formatState.value == Prefs.TimeFormat.Standard) {
                        ToggleTextButton(
                            title = stringResource(R.string.status_bar_leading_zero),
                            checked = leadingZeroState.value,
                            onCheckedChange = {
                                leadingZeroState.value = it
                                prefs.leadingZero = it
                            },
                            onClick = {
                                leadingZeroState.value = !leadingZeroState.value
                                prefs.leadingZero = leadingZeroState.value
                            },
                        )
                    }
                    ToggleTextButton(
                        title = stringResource(R.string.status_bar_show_seconds),
                        checked = secondsState.value,
                        onCheckedChange = {
                            secondsState.value = it
                            prefs.showSeconds = it
                        },
                        onClick = {
                            secondsState.value = !secondsState.value
                            prefs.showSeconds = secondsState.value
                        },
                    )
                    SelectorButton(
                        label = stringResource(R.string.status_bar_on_press),
                        value = actionDisplayValue(actionState.value, StatusBarSectionType.TIME),
                        onClick = {
                            findNavController().navigate(
                                R.id.action_statusBarTimeFragment_to_gestureActionFragment,
                                bundleOf(GestureActionFragment.SECTION_TYPE to StatusBarSectionType.TIME.name),
                            )
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun actionDisplayValue(
        action: Constants.Action,
        section: StatusBarSectionType,
    ): String =
        when (action) {
            Constants.Action.OpenApp -> stringResource(R.string.action_open_app_name, prefs.getSectionApp(section).appLabel)
            Constants.Action.Disabled -> stringResource(R.string.action_disabled)
            else -> action.displayName()
        }
}
