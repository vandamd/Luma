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
                    GestureButton("Swipe left", GestureType.SWIPE_LEFT)
                    GestureButton("Swipe right", GestureType.SWIPE_RIGHT)
                    GestureButton("Swipe down", GestureType.SWIPE_DOWN)
                    GestureButton("Swipe up", GestureType.SWIPE_UP)
                    GestureButton("Double tap", GestureType.DOUBLE_TAP)
                }
            }
        }
    }

    @Composable
    private fun GestureButton(
        label: String,
        type: GestureType,
    ) {
        val action = prefs.getGestureAction(type)
        val value =
            when (action) {
                Constants.Action.OpenApp -> "Open ${prefs.getGestureApp(type).appLabel}"
                Constants.Action.Disabled -> "Disabled"
                else -> action.displayName()
            }
        SelectorButton(
            label = label,
            value = value,
            onClick = {
                findNavController().navigate(
                    R.id.action_gesturesFragment_to_gestureActionFragment,
                    bundleOf(GestureActionFragment.GESTURE_TYPE to type.name),
                )
            },
        )
    }
}
