package app.luma.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Constants
import app.luma.data.GestureType
import app.luma.data.Prefs
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SelectorButton
import app.luma.ui.compose.SettingsComposable.SettingsHeader

class GesturesFragment : Fragment() {
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { GesturesScreen() }

    @Composable
    fun GesturesScreen() {
        Column {
            SettingsHeader(
                title = "Gestures",
                onBack = ::goBack,
            )

            ContentContainer {
                CustomScrollView(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    SelectorButton(
                        label = "Swipe left",
                        value = getActionText(prefs.swipeLeftAction, prefs.appSwipeLeft.appLabel),
                        onClick = { navigateToGesture(GestureType.SWIPE_LEFT) },
                    )
                    SelectorButton(
                        label = "Swipe right",
                        value = getActionText(prefs.swipeRightAction, prefs.appSwipeRight.appLabel),
                        onClick = { navigateToGesture(GestureType.SWIPE_RIGHT) },
                    )
                    SelectorButton(
                        label = "Swipe down",
                        value = getActionText(prefs.swipeDownAction, prefs.appSwipeDown.appLabel),
                        onClick = { navigateToGesture(GestureType.SWIPE_DOWN) },
                    )
                    SelectorButton(
                        label = "Swipe up",
                        value = getActionText(prefs.swipeUpAction, prefs.appSwipeUp.appLabel),
                        onClick = { navigateToGesture(GestureType.SWIPE_UP) },
                    )
                    SelectorButton(
                        label = "Double tap",
                        value = getActionText(prefs.doubleTapAction, prefs.appDoubleTap.appLabel),
                        onClick = { navigateToGesture(GestureType.DOUBLE_TAP) },
                    )
                }
            }
        }
    }

    private fun navigateToGesture(gestureType: GestureType) {
        findNavController().navigate(
            R.id.action_gesturesFragment_to_gestureActionFragment,
            bundleOf(GestureActionFragment.GESTURE_TYPE to gestureType.name),
        )
    }

    @Composable
    private fun getActionText(
        action: Constants.Action,
        appLabel: String,
    ): String =
        when (action) {
            Constants.Action.OpenApp -> "Open $appLabel"
            Constants.Action.Disabled -> "Disabled"
            else -> action.displayName()
        }
}
