package app.luma.ui

import SettingsTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import isDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.Constants
import app.luma.data.Constants.Action
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SimpleTextButton
import app.luma.ui.compose.CustomScrollView

/**
 * Unified fragment for configuring gesture actions (swipe left/right/up/down, double tap).
 * Pass the gesture type via arguments using the GESTURE_TYPE key.
 */
class GestureActionFragment : Fragment() {

    enum class GestureType(
        val title: String,
        val appDrawerFlag: AppDrawerFlag,
        val navActionId: Int
    ) {
        SWIPE_LEFT("Swipe left", AppDrawerFlag.SetSwipeLeft, R.id.action_gestureActionFragment_to_appListFragment),
        SWIPE_RIGHT("Swipe right", AppDrawerFlag.SetSwipeRight, R.id.action_gestureActionFragment_to_appListFragment),
        SWIPE_UP("Swipe up", AppDrawerFlag.SetSwipeUp, R.id.action_gestureActionFragment_to_appListFragment),
        SWIPE_DOWN("Swipe down", AppDrawerFlag.SetSwipeDown, R.id.action_gestureActionFragment_to_appListFragment),
        DOUBLE_TAP("Double tap", AppDrawerFlag.SetDoubleTap, R.id.action_gestureActionFragment_to_appListFragment);
    }

    companion object {
        const val GESTURE_TYPE = "gesture_type"

        fun newInstance(gestureType: GestureType): GestureActionFragment {
            return GestureActionFragment().apply {
                arguments = bundleOf(GESTURE_TYPE to gestureType.name)
            }
        }
    }

    private lateinit var prefs: Prefs
    private lateinit var gestureType: GestureType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(requireContext())
        val typeName = arguments?.getString(GESTURE_TYPE) ?: GestureType.SWIPE_LEFT.name
        gestureType = GestureType.valueOf(typeName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val compose = ComposeView(requireContext())
        compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        compose.setContent {
            SettingsTheme(isDarkTheme(prefs)) {
                GestureScreen()
            }
        }
        return compose
    }

    @Composable
    fun GestureScreen() {
        Column {
            SettingsHeader(
                title = gestureType.title,
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }
            )

            ContentContainer {
                CustomScrollView {
                    for (action in Constants.Action.values()) {
                        val isSelected = getCurrentAction() == action
                        val buttonText = when (action) {
                            Constants.Action.OpenApp -> "Open ${getAppLabel()}"
                            else -> action.string()
                        }
                        SimpleTextButton(
                            title = buttonText,
                            underline = isSelected,
                            onClick = { handleActionSelection(action) }
                        )
                    }
                }
            }
        }
    }

    private fun getCurrentAction(): Action {
        return when (gestureType) {
            GestureType.SWIPE_LEFT -> prefs.swipeLeftAction
            GestureType.SWIPE_RIGHT -> prefs.swipeRightAction
            GestureType.SWIPE_UP -> prefs.swipeUpAction
            GestureType.SWIPE_DOWN -> prefs.swipeDownAction
            GestureType.DOUBLE_TAP -> prefs.doubleTapAction
        }
    }

    private fun setCurrentAction(action: Action) {
        when (gestureType) {
            GestureType.SWIPE_LEFT -> prefs.swipeLeftAction = action
            GestureType.SWIPE_RIGHT -> prefs.swipeRightAction = action
            GestureType.SWIPE_UP -> prefs.swipeUpAction = action
            GestureType.SWIPE_DOWN -> prefs.swipeDownAction = action
            GestureType.DOUBLE_TAP -> prefs.doubleTapAction = action
        }
    }

    private fun getAppLabel(): String {
        return when (gestureType) {
            GestureType.SWIPE_LEFT -> prefs.appSwipeLeft.appLabel
            GestureType.SWIPE_RIGHT -> prefs.appSwipeRight.appLabel
            GestureType.SWIPE_UP -> prefs.appSwipeUp.appLabel
            GestureType.SWIPE_DOWN -> prefs.appSwipeDown.appLabel
            GestureType.DOUBLE_TAP -> prefs.appDoubleTap.appLabel
        }
    }

    private fun handleActionSelection(action: Action) {
        setCurrentAction(action)
        when (action) {
            Action.OpenApp -> {
                findNavController().navigate(
                    gestureType.navActionId,
                    bundleOf("flag" to gestureType.appDrawerFlag.toString())
                )
            }
            else -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
